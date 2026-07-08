"""
NMD Iteration Loop — Dynamic, data-driven power-steam balancing.

This module replaces the hardcoded ``usd_iterate`` function in
``iteration_service.py`` with a fully dynamic iteration loop that:

1. Reads all generation utilities and U4U consumption norms from the
   database via ``NMDNormsReader``.
2. Dispatches power using the existing ``distribute_by_priority``.
3. Calculates steam balance using existing ``steam_service`` functions.
4. Calculates U4U consumption generically from the norms matrix —
   no hardcoded utility names or norm multipliers.
5. Handles reverse-calculated norms (GT NG, HRSG NG) during iteration.
6. Checks convergence across all U4U demands.

The existing ``iteration_service.py`` is NOT modified — this is a
standalone module that can be used alongside or as a replacement.
"""

from __future__ import annotations

import logging
from typing import Dict, List, Optional, Set, Tuple

from services.nmd_norms_reader import NMDNormsReader
from services.power_service import distribute_by_priority
from services.steam_service import (
    calculate_steam_balance,
    calculate_lp_balance,
    calculate_mp_balance,
    calculate_lp_balance_stg_based,
    calculate_mp_balance_stg_based,
    get_hrsg_availability_from_dispatch,
    calculate_shp_generation_capacity,
    check_shp_balance,
    calculate_hrsg_min_load_and_excess_steam,
    dispatch_hrsg_load,
    STEAM_TO_POWER_MT_PER_MWH,
)
from services.demand_service import (
    calculate_u4u_power,
    calculate_u4u_bfw,
    calculate_u4u_dm,
    calculate_u4u_cw2,
    calculate_u4u_air,
    calculate_u4u_lp_steam,
    calculate_u4u_mp_steam,
)
from services.process_demand_service import get_process_demand_for_month
from services.norm_lookup_service import get_month_norm
from database.connection import get_connection
from database.power_asset_queries import (
    fetch_stg_extraction_lookup,
    get_stg_extraction_for_load,
    get_stg_operating_hours,
    fetch_hrsg_heat_rate_lookup,
    calculate_hrsg_ng_from_heat_rate,
)
from services.iteration_service import (
    calculate_stg_shp_demand,
    _fetch_lp_norm,
    _fetch_cpp_norm,
    _fetch_mp_tsc_qty,
)

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------
CONVERGENCE_TOLERANCE = 0.0001  # 0.01% relative change
MAX_ITERATIONS = 50
POWER_AUX_TOLERANCE_MWH = 0.02  # MWh absolute tolerance for power aux convergence
LP_U4U_TOLERANCE_MT = 0.1  # MT tolerance for LP U4U convergence
MP_U4U_TOLERANCE_MT = 0.1  # MT tolerance for MP U4U convergence
EXCESS_STEAM_TOLERANCE_MT = 0.1  # MT tolerance for excess steam
STALL_ITERATION_LIMIT = 3
STALL_EXCESS_STEAM_DELTA_MT = 1.1
STALL_POWER_AUX_DELTA_MWH = 0.02
STALL_STG_DELTA_MWH = 0.02

# Materials that are generation utilities (appear as producers)
DISPATCHABLE_UTILITY = "POWERGEN"

# Demand prefix to norms material name mapping
_DEMAND_TO_NORMS_MATERIAL = {
    "power": "Power_Dis",
    "shp": "SHP Steam_Dis",
    "hp": "HP Steam_Dis",
    "mp": "MP Steam_Dis",
    "lp": "LP Steam_Dis",
}


def _month_name(month: int) -> str:
    names = ["", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
             "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
    return names[month] if 1 <= month <= 12 else str(month)


class NMDIterationLoop:
    """
    Dynamic NMD iteration loop for power-steam balancing.

    Each iteration:
    1. Dispatch power assets (priority, capacity, STG limits enforced)
    2. Calculate steam balance (LP→MP→HP→SHP cascade with STG extraction)
    3. Dispatch HRSG supplementary firing based on SHP deficit
    4. Calculate U4U consumption from all generation (dynamic norms matrix)
    5. Update demands with U4U consumption
    6. Check convergence across all utility demands

    The loop dynamically discovers all generation utilities and their U4U
    relationships from the database — no hardcoded utility names.
    """

    def __init__(
        self,
        month: int,
        year: int,
        cpp_plant_id: str,
        initial_demands: dict,
        norms_reader: Optional[NMDNormsReader] = None,
        convergence_tolerance: float = CONVERGENCE_TOLERANCE,
        max_iterations: int = MAX_ITERATIONS,
        export_available: bool = False,
        hrsg_full_load: bool = False,
    ):
        """
        Args:
            month, year: Financial period.
            cpp_plant_id: CPP Plant UUID for power dispatch.
            initial_demands: Dict with keys like:
                power_process, power_fixed (KWH for process, MWh for fixed)
                lp_process, lp_fixed, mp_process, mp_fixed,
                hp_process, hp_fixed, shp_process, shp_fixed (MT)
                dm_process, dm_fixed (M3)
                cw1_process, cw2_process (KM3)
                air_process, air_fixed (NM3)
                oxygen_process (MT), effluent_process (M3)
                bfw_ufu (M3)
            norms_reader: Optional pre-built NMDNormsReader.
            convergence_tolerance: Relative tolerance for U4U convergence.
            max_iterations: Maximum iteration count.
            export_available: Whether power export is available.
            hrsg_full_load: If True, load HRSG without subtracting free steam.
        """
        self.month = month
        self.year = year
        self.cpp_plant_id = cpp_plant_id
        self.initial_demands = initial_demands
        self.norms_reader = norms_reader or NMDNormsReader.get_reader(month, year)
        self.convergence_tolerance = convergence_tolerance
        self.max_iterations = max_iterations
        self.export_available = export_available
        self.hrsg_full_load = hrsg_full_load

        # Consumption norms matrix from reader
        self.consumption_norms: Dict[str, Dict] = {}
        self._all_producers: Set[str] = set()
        self._bpc_gen_quantities: Dict[str, float] = {}
        self._bpc_quantities: Dict[str, float] = {}

        # Precomputed initial values
        self._initial_power_mwh: float = 0.0
        self._initial_steam_mt: Dict[str, float] = {}

        # STG extraction lookup
        self._stg_extraction_lookup_df = None
        self._stg_op_hours: float = 720.0
        self._stg_shp_norm: float = 0.0
        self._use_stg_load_based: bool = False

        # HRSG heat rate lookup
        self._hrsg_heat_rate_lookup_df = None
        self._use_hrsg_heat_rate_lookup: bool = False

        # Results
        self.iteration_history: List[dict] = []
        self.converged: bool = False
        self.iterations_used: int = 0
        self.final_power_result: Optional[dict] = None
        self.final_dispatch: Optional[list] = None
        self.final_steam_balance: Optional[dict] = None
        self.final_u4u_demands: Dict[str, float] = {}
        self.final_total_demands: Dict[str, float] = {}
        self.final_detail_records: List[dict] = []
        self.final_stg_extraction: Optional[dict] = None
        self.final_hrsg_dispatch: Optional[dict] = None
        self.final_hrsg_ng_calculation: Optional[dict] = None
        self.final_lp_balance: Optional[dict] = None
        self.final_mp_balance: Optional[dict] = None
        self.final_shp_balance: Optional[dict] = None
        self.final_shp_capacity: Optional[dict] = None
        self.final_hrsg_availability: Optional[dict] = None
        self.final_u4u_power: Optional[dict] = None
        self.final_u4u_lp_steam: Optional[dict] = None
        self.final_u4u_mp_steam: Optional[dict] = None
        self.stg_reduction_mwh: float = 0.0
        self.import_compensation_mwh: float = 0.0

    # ------------------------------------------------------------------
    # Main entry point
    # ------------------------------------------------------------------

    def run(self) -> dict:
        """Run the NMD iteration loop until convergence or max iterations."""
        # Load norms and lookup tables
        self._load_data()

        if not self.consumption_norms:
            logger.warning("  [NMD LOOP] No consumption norms available")
            return self._empty_result()

        # Precompute initial demand values
        self._precompute_initial_values()
        initial_utility_demands = self._build_initial_demands()

        print("")
        print("  " + "=" * 78)
        print("  NMD ITERATION LOOP  (%s %d)", _month_name(self.month), self.year)
        print("  " + "=" * 78)
        print("  Tolerance: %.4f%%  |  Max iterations: %d",
              self.convergence_tolerance * 100, self.max_iterations)
        print("  Generation utilities: %d", len(self._all_producers))
        print("")

        u4u_demands = {m: 0.0 for m in self._all_producers}
        prev_total_demands = {m: 0.0 for m in self._all_producers}

        # STG reduction / excess steam tracking
        stg_reduction_mwh = 0.0
        import_compensation_mwh = 0.0
        stg_original_max_mwh = None
        stg_steam_limit_mwh = None
        stg_min_override_mwh = None
        gt_reduction_for_balance_mwh = 0.0
        excess_steam_balancing_active = False
        power_initially_converged = False
        excess_steam_controller_mode = None
        repeated_stall_count = 0
        previous_excess_steam_mt = None
        previous_stg_override_mwh = None
        previous_gt_reduction_mwh = None
        previous_aux_power_error_mwh = None
        previous_utility_aux_mwh = 0.0
        previous_shp_deficit = None
        previous_lp_u4u_mt = 0.0
        previous_mp_u4u_mt = 0.0
        _lp_u4u_from_prev_iter = 0.0
        _mp_u4u_from_prev_iter = 0.0

        for iteration in range(1, self.max_iterations + 1):
            # Build total demands = initial + U4U
            total_demands = {
                m: initial_utility_demands.get(m, 0.0) + u4u_demands.get(m, 0.0)
                for m in self._all_producers
            }

            # --- STEP 1: Power Dispatch ---
            power_aux_mwh = u4u_demands.get("Power_Dis", 0.0)

            stg_limit_mwh = None
            if stg_reduction_mwh > 0 and stg_original_max_mwh is not None:
                stg_limit_mwh = max(0, stg_original_max_mwh - stg_reduction_mwh)
            if stg_steam_limit_mwh is not None:
                stg_limit_mwh = min(stg_limit_mwh, stg_steam_limit_mwh) if stg_limit_mwh is not None else stg_steam_limit_mwh

            power_result = distribute_by_priority(
                self.month, self.year, self.cpp_plant_id,
                additional_demand_mwh=power_aux_mwh,
                stg_max_mwh=stg_limit_mwh,
                stg_min_override_mwh=stg_min_override_mwh,
                gt_reduction_mwh=gt_reduction_for_balance_mwh,
                stg_extraction_lookup_df=self._stg_extraction_lookup_df,
            )

            if power_result.get("insufficientCapacity") or power_result.get("insufficientCapacityAfterImport"):
                return self._power_failure_result(power_result, self.iteration_history)

            current_dispatch = power_result["dispatchPlan"]

            # Extract GT/STG details
            stg_gross_mwh, stg_aux_mwh, stg_net_mwh, stg_shp_required, gt_details, stg_load_mw = (
                self._extract_dispatch_details(current_dispatch)
            )
            gt_gross_mwh = sum(gt["gross_mwh"] for gt in gt_details)

            if stg_original_max_mwh is None and stg_gross_mwh > 0:
                for asset in current_dispatch:
                    if "STG" in str(asset.get("AssetName", "")).upper():
                        stg_original_max_mwh = asset.get("CapacityMW", 25) * asset.get("Hours", 720)
                        break

            # --- STEP 2: STG Extraction (load-based) ---
            stg_extraction = self._calculate_stg_extraction(stg_load_mw, stg_gross_mwh)

            # --- STEP 3: Steam Balance ---
            lp_u4u_mt = u4u_demands.get("LP Steam_Dis", 0.0)
            mp_u4u_mt = u4u_demands.get("MP Steam_Dis", 0.0)

            steam_balance = calculate_steam_balance(
                lp_process=float(self.initial_demands.get("lp_process", 0.0)),
                lp_fixed=float(self.initial_demands.get("lp_fixed", 0.0)),
                mp_process=float(self.initial_demands.get("mp_process", 0.0)),
                mp_fixed=float(self.initial_demands.get("mp_fixed", 0.0)),
                hp_process=float(self.initial_demands.get("hp_process", 0.0)),
                hp_fixed=float(self.initial_demands.get("hp_fixed", 0.0)),
                shp_process=float(self.initial_demands.get("shp_process", 0.0)),
                shp_fixed=float(self.initial_demands.get("shp_fixed", 0.0)),
                bfw_ufu=float(self.initial_demands.get("bfw_ufu", 0.0)),
                stg_shp_power=stg_shp_required,
                lp_ufu_mt=lp_u4u_mt if lp_u4u_mt > 0 else None,
                mp_ufu_mt=mp_u4u_mt if mp_u4u_mt > 0 else None,
                stg_lp_extraction_tph=stg_extraction.get("lp_extraction_tph", 0.0),
                stg_mp_extraction_tph=stg_extraction.get("mp_extraction_tph", 0.0),
                stg_eq_svh_lp_tph=stg_extraction.get("eq_svh_lp_tph", 0.0),
                stg_eq_svh_mp_tph=stg_extraction.get("eq_svh_mp_tph", 0.0),
                stg_operating_hours=stg_extraction.get("stg_operating_hours", 0.0),
            )

            # --- STEP 4: HRSG Availability & SHP Capacity ---
            hrsg_availability = get_hrsg_availability_from_dispatch(current_dispatch)
            shp_capacity = calculate_shp_generation_capacity(hrsg_availability, self.hrsg_full_load)

            # --- STEP 5: HRSG Dispatch (supplementary firing) ---
            total_shp_demand = steam_balance["summary"]["total_shp_demand"]
            total_free_steam = shp_capacity.get("total_free_steam_mt", 0.0)
            net_shp_demand = max(0.0, total_shp_demand - total_free_steam)

            shp_balance = check_shp_balance(net_shp_demand, shp_capacity)
            can_meet_shp = shp_balance.get("can_meet", False)
            shp_deficit = shp_balance.get("deficit_mt", 0.0)

            hrsg_dispatch = dispatch_hrsg_load(
                current_dispatch, net_shp_demand, shp_capacity
            ) if can_meet_shp else None

            # --- STEP 6: HRSG NG Reverse Calculation ---
            hrsg_ng_calculation = self._calculate_hrsg_ng(hrsg_dispatch)

            # --- STEP 7: Calculate U4U Consumption ---
            # Use the existing demand_service functions for detailed U4U calcs
            # These functions already fetch norms from DB dynamically
            utility_consumption = self._calculate_u4u_consumption(
                current_dispatch, steam_balance, stg_extraction, hrsg_dispatch
            )

            new_u4u_demands = utility_consumption["u4u_demands"]
            detail_records = utility_consumption["detail_records"]

            # --- STEP 8: Convergence Check ---
            current_utility_aux_mwh = new_u4u_demands.get("Power_Dis", 0.0)
            current_lp_u4u_mt = new_u4u_demands.get("LP Steam_Dis", 0.0)
            current_mp_u4u_mt = new_u4u_demands.get("MP Steam_Dis", 0.0)

            aux_power_error = abs(current_utility_aux_mwh - previous_utility_aux_mwh)
            lp_u4u_error = abs(current_lp_u4u_mt - _lp_u4u_from_prev_iter)
            mp_u4u_error = abs(current_mp_u4u_mt - _mp_u4u_from_prev_iter)

            excess_steam_mt = 0.0
            if hrsg_dispatch:
                excess_steam_mt = hrsg_dispatch.get("excess_steam_mt", 0.0)

            power_converged = aux_power_error <= POWER_AUX_TOLERANCE_MWH
            shp_converged = can_meet_shp
            lp_u4u_converged = lp_u4u_error <= LP_U4U_TOLERANCE_MT
            mp_u4u_converged = mp_u4u_error <= MP_U4U_TOLERANCE_MT
            practical_excess_converged = excess_steam_mt <= EXCESS_STEAM_TOLERANCE_MT

            if aux_power_error < 10.0:
                power_initially_converged = True

            # --- Log iteration ---
            self._log_iteration(
                iteration, total_demands, prev_total_demands,
                new_u4u_demands, u4u_demands, detail_records,
                power_result, stg_gross_mwh, gt_gross_mwh,
                current_utility_aux_mwh, previous_utility_aux_mwh,
                aux_power_error, shp_deficit, excess_steam_mt,
                lp_u4u_error, mp_u4u_error,
                _lp_u4u_from_prev_iter, _mp_u4u_from_prev_iter,
            )

            # --- Record iteration ---
            iteration_record = {
                "iteration": iteration,
                "total_demand_mwh": round(total_demands.get("Power_Dis", 0.0), 2),
                "total_gross_mwh": round(power_result.get("totalGrossGeneration", 0), 2),
                "total_net_mwh": round(power_result.get("totalNetGeneration", 0), 2),
                "stg_gross_mwh": round(stg_gross_mwh, 2),
                "gt_gross_mwh": round(gt_gross_mwh, 2),
                "stg_shp_required_mt": round(stg_shp_required, 2),
                "shp_deficit_mt": round(shp_deficit, 2),
                "excess_steam_mt": round(excess_steam_mt, 2),
                "aux_power_error_mwh": round(aux_power_error, 6),
                "lp_u4u_error_mt": round(lp_u4u_error, 2),
                "mp_u4u_error_mt": round(mp_u4u_error, 2),
                "stg_reduction_mwh": round(stg_reduction_mwh, 2),
                "import_compensation_mwh": round(import_compensation_mwh, 2),
                "status": "PENDING",
            }

            # --- Excess steam balancing (proportional controller) ---
            stg_increased = False
            true_excess_steam_mt = 0.0
            if hrsg_dispatch:
                total_supp_min = hrsg_dispatch.get("min_supply_mt", 0.0)
                true_excess_steam_mt = total_supp_min - net_shp_demand

            if power_initially_converged and (true_excess_steam_mt > EXCESS_STEAM_TOLERANCE_MT or excess_steam_controller_mode == "HRSG_MIN_EXCESS"):
                conversion_rate = stg_extraction.get("sp_steam_power", STEAM_TO_POWER_MT_PER_MWH) if stg_extraction else STEAM_TO_POWER_MT_PER_MWH
                if conversion_rate <= 0:
                    conversion_rate = STEAM_TO_POWER_MT_PER_MWH

                desired_stg_adj_mwh = true_excess_steam_mt / conversion_rate
                stg_db_min_mwh = 0.0
                stg_db_max_mwh = 0.0
                for asset in current_dispatch:
                    if "STG" in str(asset.get("AssetName", "")).upper():
                        stg_db_min_mwh = asset.get("MinMW", 5.0) * asset.get("Hours", 720)
                        stg_db_max_mwh = asset.get("CapacityMW", 25) * asset.get("Hours", 720)
                        break

                target_stg_mwh = stg_gross_mwh + desired_stg_adj_mwh
                target_stg_mwh = max(stg_db_min_mwh, min(target_stg_mwh, stg_db_max_mwh))
                actual_stg_adj = target_stg_mwh - stg_gross_mwh

                if abs(actual_stg_adj) > 0.5 or abs(true_excess_steam_mt) > EXCESS_STEAM_TOLERANCE_MT:
                    stg_min_override_mwh = target_stg_mwh
                    gt_reduction_for_balance_mwh = 0.0
                    excess_steam_balancing_active = True
                    excess_steam_controller_mode = "HRSG_MIN_EXCESS"
                    if actual_stg_adj > 0:
                        stg_increased = True
                    iteration_record["action"] = f"EXCESS_STEAM_BALANCE_STG{actual_stg_adj:+.2f}"
                    iteration_record["status"] = "EXCESS_STEAM_BALANCING"
                else:
                    excess_steam_balancing_active = True
                    excess_steam_controller_mode = "HRSG_MIN_EXCESS"
                    iteration_record["action"] = f"HOLD_EXCESS_STEAM_OVERRIDE"
                    iteration_record["status"] = "EXCESS_STEAM_HOLD"
            elif not power_initially_converged:
                excess_steam_balancing_active = False
                excess_steam_controller_mode = None
                stg_min_override_mwh = None
                gt_reduction_for_balance_mwh = 0.0
            else:
                excess_steam_balancing_active = False
                excess_steam_controller_mode = None
                stg_min_override_mwh = None
                gt_reduction_for_balance_mwh = 0.0

            # --- SHP excess recovery ---
            if can_meet_shp and shp_deficit < 0 and stg_reduction_mwh > 0 and excess_steam_controller_mode is None:
                excess_shp = abs(shp_deficit)
                potential_stg_recovery = excess_shp / self._stg_shp_norm / 1000 if self._stg_shp_norm > 0 else 0
                damped_recovery = potential_stg_recovery * 0.5
                actual_recovery = min(damped_recovery, stg_reduction_mwh)
                if actual_recovery > 0.1:
                    stg_reduction_mwh -= actual_recovery
                    import_compensation_mwh = stg_reduction_mwh
                    stg_increased = True
                    iteration_record["action"] = f"INCREASE_STG_{actual_recovery:.2f}_MWH"
                    iteration_record["status"] = "SHP_EXCESS_RECOVERY"

            # --- Stall detection ---
            if previous_excess_steam_mt is not None:
                same_excess = abs(excess_steam_mt - previous_excess_steam_mt) <= STALL_EXCESS_STEAM_DELTA_MT
                same_aux = previous_aux_power_error_mwh is not None and abs(aux_power_error - previous_aux_power_error_mwh) <= STALL_POWER_AUX_DELTA_MWH
                same_stg = abs((stg_min_override_mwh or 0) - (previous_stg_override_mwh or 0)) <= STALL_STG_DELTA_MWH
                same_gt = abs(gt_reduction_for_balance_mwh - (previous_gt_reduction_mwh or 0)) <= STALL_STG_DELTA_MWH
                if same_excess and same_aux and same_stg and same_gt:
                    repeated_stall_count += 1
                else:
                    repeated_stall_count = 0
            else:
                repeated_stall_count = 0

            if repeated_stall_count >= STALL_ITERATION_LIMIT - 1 and power_converged and lp_u4u_converged and mp_u4u_converged:
                iteration_record["status"] = "PRACTICAL_CONVERGENCE"
                iteration_record["action"] = f"STALL_STOP"
                self.iteration_history.append(iteration_record)
                self._store_final_results(
                    current_dispatch, power_result, steam_balance, shp_capacity,
                    hrsg_availability, stg_extraction, hrsg_dispatch, hrsg_ng_calculation,
                    new_u4u_demands, total_demands, detail_records,
                    utility_consumption,
                )
                self.converged = practical_excess_converged or can_meet_shp
                break

            # --- Check full convergence ---
            if power_converged and shp_converged and lp_u4u_converged and mp_u4u_converged and practical_excess_converged and not stg_increased:
                iteration_record["status"] = "CONVERGED"
                iteration_record["action"] = "CONVERGED"
                self.iteration_history.append(iteration_record)
                self._store_final_results(
                    current_dispatch, power_result, steam_balance, shp_capacity,
                    hrsg_availability, stg_extraction, hrsg_dispatch, hrsg_ng_calculation,
                    new_u4u_demands, total_demands, detail_records,
                    utility_consumption,
                )
                self.converged = True
                break

            # --- SHP deficit: reduce STG ---
            if not can_meet_shp and shp_deficit > 0:
                if stg_gross_mwh <= 0:
                    iteration_record["status"] = "FAILED"
                    iteration_record["action"] = "SHP_IMPOSSIBLE"
                    iteration_record["failure_reason"] = "SHP_DEFICIT_UNSOLVABLE"
                    self.iteration_history.append(iteration_record)
                    self._store_final_results(
                        current_dispatch, power_result, steam_balance, shp_capacity,
                        hrsg_availability, stg_extraction, hrsg_dispatch, hrsg_ng_calculation,
                        new_u4u_demands, total_demands, detail_records,
                        utility_consumption,
                    )
                    break

                stg_reduction_for_shp = shp_deficit / self._stg_shp_norm / 1000 if self._stg_shp_norm > 0 else 0
                stg_reduction_mwh += stg_reduction_for_shp
                if stg_original_max_mwh is not None:
                    stg_reduction_mwh = min(stg_reduction_mwh, stg_original_max_mwh)
                import_compensation_mwh = stg_reduction_mwh
                iteration_record["action"] = f"REDUCE_STG_{stg_reduction_for_shp:.2f}_MWH"
                iteration_record["status"] = "SHP_DEFICIT"
            elif not power_converged:
                iteration_record["status"] = "POWER_ITERATING"
            elif not lp_u4u_converged:
                iteration_record["status"] = "LP_U4U_ITERATING"
            elif not mp_u4u_converged:
                iteration_record["status"] = "MP_U4U_ITERATING"

            self.iteration_history.append(iteration_record)

            # --- Store final results (updated each iteration) ---
            self._store_final_results(
                current_dispatch, power_result, steam_balance, shp_capacity,
                hrsg_availability, stg_extraction, hrsg_dispatch, hrsg_ng_calculation,
                new_u4u_demands, total_demands, detail_records,
                utility_consumption,
            )

            # --- Update for next iteration ---
            _lp_u4u_from_prev_iter = current_lp_u4u_mt
            _mp_u4u_from_prev_iter = current_mp_u4u_mt
            previous_utility_aux_mwh = current_utility_aux_mwh
            previous_shp_deficit = shp_deficit
            previous_excess_steam_mt = excess_steam_mt
            previous_stg_override_mwh = stg_min_override_mwh
            previous_gt_reduction_mwh = gt_reduction_for_balance_mwh
            previous_aux_power_error_mwh = aux_power_error
            prev_total_demands = dict(total_demands)
            u4u_demands = new_u4u_demands

            # Steam-based STG limit for next iteration
            base_shp_demand = (
                float(self.initial_demands.get("shp_process", 0.0))
                + float(self.initial_demands.get("shp_fixed", 0.0))
                + steam_balance.get("lp_balance", {}).get("shp_for_stg_lp", 0.0)
                + steam_balance.get("mp_balance", {}).get("shp_for_stg_mp", 0.0)
                + steam_balance.get("hp_balance", {}).get("shp_for_hp_prds", 0.0)
                + steam_balance.get("mp_balance", {}).get("shp_for_prds_mp", 0.0)
            )
            available_shp_for_stg = shp_capacity.get("max_shp_capacity_mt", 0.0) - base_shp_demand
            if available_shp_for_stg > 0 and self._stg_shp_norm > 0:
                stg_steam_limit_mwh = available_shp_for_stg / self._stg_shp_norm / 1000
            else:
                stg_steam_limit_mwh = 0.0

            self.iterations_used = iteration

        if not self.converged:
            logger.warning("  [NMD LOOP] Did NOT converge after %d iterations", self.max_iterations)

        self._log_final_summary()
        return self._build_result()

    # ------------------------------------------------------------------
    # Data loading
    # ------------------------------------------------------------------

    def _load_data(self):
        """Load norms, STG extraction lookup, and HRSG heat rate lookup."""
        # Load consumption norms from reader
        self.consumption_norms = self.norms_reader.get_consumption_norms()
        self._all_producers = self.norms_reader.get_generation_utilities()
        self._bpc_gen_quantities = self.norms_reader.get_bpc_generation_quantities()
        self._bpc_quantities = self.norms_reader.get_bpc_quantities()
        self._stg_shp_norm = self.norms_reader.get_stg_steam_norm()

        # Load STG extraction lookup
        self._stg_extraction_lookup_df = fetch_stg_extraction_lookup()
        self._stg_op_hours = get_stg_operating_hours(self.month, self.year)

        if self._stg_extraction_lookup_df is not None and not self._stg_extraction_lookup_df.empty:
            self._use_stg_load_based = True
        else:
            self._use_stg_load_based = False

        # Load HRSG heat rate lookup
        self._hrsg_heat_rate_lookup_df = fetch_hrsg_heat_rate_lookup(month=self.month, year=self.year)
        self._use_hrsg_heat_rate_lookup = self._hrsg_heat_rate_lookup_df is not None and not self._hrsg_heat_rate_lookup_df.empty

    # ------------------------------------------------------------------
    # Demand construction
    # ------------------------------------------------------------------

    def _precompute_initial_values(self):
        """Cache initial power and steam demand totals."""
        power_process_kwh = float(self.initial_demands.get("power_process", 0.0))
        power_fixed_mwh = float(self.initial_demands.get("power_fixed", 0.0))
        self._initial_power_mwh = power_process_kwh / 1000.0 + power_fixed_mwh

        self._initial_steam_mt = {}
        for grade in ("shp", "hp", "mp", "lp"):
            process = float(self.initial_demands.get(f"{grade}_process", 0.0))
            fixed = float(self.initial_demands.get(f"{grade}_fixed", 0.0))
            self._initial_steam_mt[grade] = process + fixed

    def _build_initial_demands(self) -> dict:
        """Build initial demand map keyed by norms material name."""
        demands = {}
        for prefix, ods_material in _DEMAND_TO_NORMS_MATERIAL.items():
            process_val = float(self.initial_demands.get(f"{prefix}_process", 0.0))
            fixed_val = float(self.initial_demands.get(f"{prefix}_fixed", 0.0))
            if prefix == "power":
                process_val = process_val / 1000.0  # KWH → MWh
            total = process_val + fixed_val
            demands[ods_material] = demands.get(ods_material, 0.0) + total

        for producer in self._all_producers:
            if producer not in demands:
                demands[producer] = 0.0

        return demands

    # ------------------------------------------------------------------
    # Dispatch detail extraction
    # ------------------------------------------------------------------

    def _extract_dispatch_details(self, current_dispatch: list) -> tuple:
        """Extract STG and GT details from power dispatch results."""
        stg_gross_mwh = 0.0
        stg_aux_mwh = 0.0
        stg_net_mwh = 0.0
        stg_shp_required = 0.0
        stg_load_mw = 0.0
        gt_details = []

        for asset in current_dispatch:
            asset_name = asset.get("AssetName", "Unknown")
            asset_upper = asset_name.upper()
            gross = asset.get("GrossMWh", 0)
            aux = asset.get("AuxMWh", 0)
            net = asset.get("NetMWh", 0)
            hours = asset.get("Hours", 0)

            if "STG" in asset_upper or "STEAM TURBINE" in asset_upper:
                stg_gross_mwh = gross
                stg_aux_mwh = aux
                stg_net_mwh = net
                stg_load_mw = asset.get("LoadMW", 0)

                if self._use_stg_load_based and stg_load_mw > 0:
                    ext_data = get_stg_extraction_for_load(stg_load_mw, self._stg_extraction_lookup_df)
                    shp_inlet_tph = ext_data.get("shp_inlet_tph", 0.0)
                    if shp_inlet_tph > 0:
                        stg_shp_required = shp_inlet_tph * hours
                    else:
                        stg_shp_required = stg_gross_mwh * 1000 * self._stg_shp_norm
                else:
                    stg_shp_required = stg_gross_mwh * 1000 * self._stg_shp_norm

            elif "GT" in asset_upper or "POWER PLANT" in asset_upper:
                gt_details.append({
                    "name": asset_name,
                    "gross_mwh": gross,
                    "aux_mwh": aux,
                    "net_mwh": net,
                    "load_mw": asset.get("LoadMW", 0),
                    "free_steam": asset.get("FreeSteam", 0),
                    "hours": hours,
                })

        return stg_gross_mwh, stg_aux_mwh, stg_net_mwh, stg_shp_required, gt_details, stg_load_mw

    # ------------------------------------------------------------------
    # STG extraction calculation
    # ------------------------------------------------------------------

    def _calculate_stg_extraction(self, stg_load_mw: float, stg_gross_mwh: float) -> dict:
        """Calculate STG extraction based on load or legacy fallback."""
        from services.iteration_service import calculate_stg_extraction_requirements

        # Get initial LP/MP totals from demands
        lp_process = float(self.initial_demands.get("lp_process", 0.0))
        lp_fixed = float(self.initial_demands.get("lp_fixed", 0.0))
        mp_process = float(self.initial_demands.get("mp_process", 0.0))
        mp_fixed = float(self.initial_demands.get("mp_fixed", 0.0))

        lp_balance = calculate_lp_balance(lp_process, lp_fixed, float(self.initial_demands.get("bfw_ufu", 0.0)))
        mp_balance = calculate_mp_balance(mp_process, mp_fixed, lp_balance["mp_for_prds_lp"])
        lp_total = lp_balance["lp_total"]
        mp_total = mp_balance["mp_total"]

        if self._use_stg_load_based and stg_load_mw > 0:
            ext_data = get_stg_extraction_for_load(stg_load_mw, self._stg_extraction_lookup_df)
            # Build full extraction result from lookup data
            lp_extraction_tph = ext_data.get("lp_extraction_tph", 0.0)
            mp_extraction_tph = ext_data.get("mp_extraction_tph", 0.0)
            shp_inlet_tph = ext_data.get("shp_inlet_tph", 0.0)
            condensing_load_m3hr = ext_data.get("condensing_load_m3hr", 0.0)
            actual_load_mw = ext_data.get("load_mw", stg_load_mw)

            stg_op_hours = self._stg_op_hours

            lp_from_stg_available = lp_extraction_tph * stg_op_hours
            lp_from_stg = min(lp_from_stg_available, lp_total)
            lp_from_prds = max(0, lp_total - lp_from_stg)

            mp_from_stg_available = mp_extraction_tph * stg_op_hours
            mp_from_stg = min(mp_from_stg_available, mp_total)
            mp_from_prds = max(0, mp_total - mp_from_stg)

            stg_shp_inlet_mt = shp_inlet_tph * stg_op_hours
            stg_gross_kwh = actual_load_mw * stg_op_hours * 1000
            stg_shp_norm = stg_shp_inlet_mt / stg_gross_kwh if stg_gross_kwh > 0 else 0.0
            sp_steam_power = stg_shp_inlet_mt / stg_gross_mwh if stg_gross_mwh > 0 else STEAM_TO_POWER_MT_PER_MWH

            return {
                "lp_from_stg": round(lp_from_stg, 2),
                "lp_from_prds": round(lp_from_prds, 2),
                "mp_from_stg": round(mp_from_stg, 2),
                "mp_from_prds": round(mp_from_prds, 2),
                "lp_extraction_tph": round(lp_extraction_tph, 2),
                "mp_extraction_tph": round(mp_extraction_tph, 2),
                "shp_inlet_tph": round(shp_inlet_tph, 2),
                "stg_shp_inlet_mt": round(stg_shp_inlet_mt, 2),
                "stg_shp_norm": round(stg_shp_norm, 7),
                "stg_load_mw": round(actual_load_mw, 2),
                "stg_operating_hours": round(stg_op_hours, 2),
                "sp_steam_power": round(sp_steam_power, 6),
                "condensing_load_m3hr": round(condensing_load_m3hr, 2),
                "stg_gross_kwh": round(stg_gross_kwh, 2),
                "mode": "stg_load_based",
            }
        else:
            return calculate_stg_extraction_requirements(lp_total, mp_total)

    # ------------------------------------------------------------------
    # HRSG NG reverse calculation
    # ------------------------------------------------------------------

    def _calculate_hrsg_ng(self, hrsg_dispatch: Optional[dict]) -> Optional[dict]:
        """Calculate HRSG Natural Gas from heat rate lookup."""
        if not hrsg_dispatch or not self._use_hrsg_heat_rate_lookup:
            return None

        return calculate_hrsg_ng_from_heat_rate(
            hrsg_dispatch, self._hrsg_heat_rate_lookup_df
        )

    # ------------------------------------------------------------------
    # U4U consumption calculation
    # ------------------------------------------------------------------

    def _calculate_u4u_consumption(
        self,
        current_dispatch: list,
        steam_balance: dict,
        stg_extraction: dict,
        hrsg_dispatch: Optional[dict],
    ) -> dict:
        """
        Calculate U4U consumption using existing demand_service functions.

        These functions already fetch norms dynamically from the DB.
        Returns dict with 'u4u_demands' (keyed by norms material name)
        and 'detail_records'.
        """
        # Extract generation values needed by demand_service functions
        stg_gross_mwh = 0.0
        gt1_gross_mwh = 0.0
        gt2_gross_mwh = 0.0
        gt3_gross_mwh = 0.0
        gt_idx = 0

        for asset in current_dispatch:
            name = str(asset.get("AssetName", "")).upper()
            gross = asset.get("GrossMWh", 0)
            if "STG" in name:
                stg_gross_mwh = gross
            elif "GT" in name or "POWER PLANT" in name:
                gt_idx += 1
                if gt_idx == 1:
                    gt1_gross_mwh = gross
                elif gt_idx == 2:
                    gt2_gross_mwh = gross
                elif gt_idx == 3:
                    gt3_gross_mwh = gross

        # Get steam balance outputs
        shp_from_hrsg_mt = 0.0
        if hrsg_dispatch:
            shp_from_hrsg_mt = hrsg_dispatch.get("total_shp_supply_mt", 0.0)

        hp_from_prds_mt = steam_balance.get("hp_balance", {}).get("hp_from_prds", 0.0)
        mp_from_prds_mt = steam_balance.get("mp_balance", {}).get("mp_from_prds", 0.0)
        lp_from_prds_mt = steam_balance.get("lp_balance", {}).get("lp_from_prds", 0.0)
        lp_from_stg_mt = steam_balance.get("lp_balance", {}).get("lp_from_stg", 0.0)
        mp_from_stg_mt = steam_balance.get("mp_balance", {}).get("mp_from_stg", 0.0)

        # Get process demands for non-steam utilities
        dm_process_m3 = float(self.initial_demands.get("dm_process", 54779.0))
        dm_fixed_m3 = float(self.initial_demands.get("dm_fixed", 0.0))
        cw1_process_km3 = float(self.initial_demands.get("cw1_process", 15194.0))
        cw2_process_km3 = float(self.initial_demands.get("cw2_process", 9016.0))
        air_process_nm3 = float(self.initial_demands.get("air_process", 6095102.0))
        air_fixed_nm3 = float(self.initial_demands.get("air_fixed", 0.0))
        oxygen_mt = float(self.initial_demands.get("oxygen_process", 5786.0))
        effluent_m3 = float(self.initial_demands.get("effluent_process", 243000.0))

        # Per-HRSG SHP quantities (needed for BFW and LP U4U)
        shp_hrsg1 = 0.0
        shp_hrsg2 = 0.0
        shp_hrsg3 = 0.0
        if hrsg_dispatch:
            shp_hrsg1 = hrsg_dispatch.get('hrsg1_dispatched_mt', 0.0) or 0.0
            shp_hrsg2 = hrsg_dispatch.get('hrsg2_dispatched_mt', 0.0) or 0.0
            shp_hrsg3 = hrsg_dispatch.get('hrsg3_dispatched_mt', 0.0) or 0.0

        # Calculate U4U using existing demand_service functions
        # (these already fetch norms from DB dynamically)
        u4u_bfw = calculate_u4u_bfw(
            month=self.month, year=self.year,
            shp_from_hrsg_mt=shp_from_hrsg_mt,
            hp_from_prds_mt=hp_from_prds_mt,
            mp_from_prds_mt=mp_from_prds_mt,
            lp_from_prds_mt=lp_from_prds_mt,
            shp_from_hrsg1_mt=shp_hrsg1,
            shp_from_hrsg2_mt=shp_hrsg2,
            shp_from_hrsg3_mt=shp_hrsg3,
        )
        total_bfw_m3 = u4u_bfw.get("total_m3", 0.0)

        u4u_dm = calculate_u4u_dm(
            month=self.month, year=self.year, bfw_total_m3=total_bfw_m3
        )
        total_dm_m3 = dm_process_m3 + dm_fixed_m3 + u4u_dm.get("total_m3", 0.0)

        total_cw1_km3 = cw1_process_km3

        stg_shp_mt = calculate_stg_shp_demand(
            stg_gross_mwh, stg_extraction.get("sp_steam_power", 0.0),
            month=self.month, year=self.year
        )

        u4u_cw2 = calculate_u4u_cw2(
            month=self.month, year=self.year,
            stg_gross_mwh=stg_gross_mwh,
            gt1_gross_mwh=gt1_gross_mwh,
            gt2_gross_mwh=gt2_gross_mwh,
            gt3_gross_mwh=gt3_gross_mwh,
            shp_from_stg_mt=stg_shp_mt,
        )
        total_cw2_km3 = cw2_process_km3 + u4u_cw2.get("total_km3", 0.0)

        u4u_air = calculate_u4u_air(
            month=self.month, year=self.year,
            gt1_gross_mwh=gt1_gross_mwh,
            gt2_gross_mwh=gt2_gross_mwh,
            gt3_gross_mwh=gt3_gross_mwh,
            stg_gross_mwh=stg_gross_mwh,
            shp_from_hrsg_mt=shp_from_hrsg_mt,
            cw1_total_km3=total_cw1_km3,
            cw2_total_km3=total_cw2_km3,
            dm_total_m3=total_dm_m3,
            bfw_total_m3=total_bfw_m3,
        )
        total_air_nm3 = air_process_nm3 + air_fixed_nm3 + u4u_air.get("total_nm3", 0.0)

        u4u_power = calculate_u4u_power(
            month=self.month, year=self.year,
            gt1_gross_mwh=gt1_gross_mwh,
            gt2_gross_mwh=gt2_gross_mwh,
            gt3_gross_mwh=gt3_gross_mwh,
            stg_gross_mwh=stg_gross_mwh,
            bfw_total_m3=total_bfw_m3,
            dm_total_m3=total_dm_m3,
            cw1_total_km3=total_cw1_km3,
            cw2_total_km3=total_cw2_km3,
            air_total_nm3=total_air_nm3,
            oxygen_total_mt=oxygen_mt,
            effluent_total_m3=effluent_m3,
        )

        # LP U4U: fetch norms from DB (same pattern as iteration_service.py)
        _lp_u4u_norm_bfw = _fetch_lp_norm(self.month, self.year, 'Boiler Feed Water', 'LP Steam_Dis')
        _lp_u4u_norm_hrsg1 = _fetch_lp_norm(self.month, self.year, 'HRSG1_SHP STEAM', 'LP Steam_Dis')
        _lp_u4u_norm_hrsg2 = _fetch_lp_norm(self.month, self.year, 'HRSG2_SHP STEAM', 'LP Steam_Dis')
        _lp_u4u_norm_hrsg3 = _fetch_lp_norm(self.month, self.year, 'HRSG3_SHP STEAM', 'LP Steam_Dis')

        u4u_lp_steam = calculate_u4u_lp_steam(
            bfw_total_m3=total_bfw_m3,
            shp_from_hrsg1_mt=shp_hrsg1,
            shp_from_hrsg2_mt=shp_hrsg2,
            shp_from_hrsg3_mt=shp_hrsg3,
            norm_bfw=_lp_u4u_norm_bfw,
            norm_hrsg1=_lp_u4u_norm_hrsg1,
            norm_hrsg2=_lp_u4u_norm_hrsg2,
            norm_hrsg3=_lp_u4u_norm_hrsg3,
        )

        # MP U4U: Treated Spent Caustic norm and qty from DB
        _mp_u4u_norm_tsc = _fetch_cpp_norm(self.month, self.year, 'Treated Spent Caustic', 'MP Steam_Dis')
        _mp_u4u_tsc_qty_kl = _fetch_mp_tsc_qty(self.month, self.year)

        u4u_mp_steam = calculate_u4u_mp_steam(
            tsc_qty_kl=_mp_u4u_tsc_qty_kl,
            norm_tsc=_mp_u4u_norm_tsc,
        )

        # Build U4U demands dict keyed by norms material name
        u4u_demands = {}
        utility_power = u4u_power.get("utility_power", {})
        u4u_demands["Power_Dis"] = utility_power.get("total_mwh", 0.0)
        u4u_demands["LP Steam_Dis"] = u4u_lp_steam.get("total_mt", 0.0)
        u4u_demands["MP Steam_Dis"] = u4u_mp_steam.get("total_mt", 0.0)

        # Build detail records for logging
        detail_records = [
            {"producer": "BFW Plant", "material": "Power_Dis", "quantity": utility_power.get("bfw_kwh", 0) / 1000, "norm": 0, "unit": "MWh"},
            {"producer": "DM Water Plant", "material": "Power_Dis", "quantity": utility_power.get("dm_kwh", 0) / 1000, "norm": 0, "unit": "MWh"},
            {"producer": "CW1 Plant", "material": "Power_Dis", "quantity": utility_power.get("cw1_kwh", 0) / 1000, "norm": 0, "unit": "MWh"},
            {"producer": "CW2 Plant", "material": "Power_Dis", "quantity": utility_power.get("cw2_kwh", 0) / 1000, "norm": 0, "unit": "MWh"},
            {"producer": "Air Plant", "material": "Power_Dis", "quantity": utility_power.get("air_kwh", 0) / 1000, "norm": 0, "unit": "MWh"},
        ]

        return {
            "u4u_demands": u4u_demands,
            "detail_records": detail_records,
            "u4u_bfw": u4u_bfw,
            "u4u_dm": u4u_dm,
            "u4u_cw2": u4u_cw2,
            "u4u_air": u4u_air,
            "u4u_power": u4u_power,
            "u4u_lp_steam": u4u_lp_steam,
            "u4u_mp_steam": u4u_mp_steam,
        }

    # ------------------------------------------------------------------
    # Result construction
    # ------------------------------------------------------------------

    def _store_final_results(
        self, current_dispatch, power_result, steam_balance, shp_capacity,
        hrsg_availability, stg_extraction, hrsg_dispatch, hrsg_ng_calculation,
        u4u_demands, total_demands, detail_records, utility_consumption,
    ):
        """Store final iteration results."""
        self.final_dispatch = current_dispatch
        self.final_power_result = power_result
        self.final_steam_balance = steam_balance
        self.final_shp_capacity = shp_capacity
        self.final_hrsg_availability = hrsg_availability
        self.final_stg_extraction = stg_extraction
        self.final_hrsg_dispatch = hrsg_dispatch
        self.final_hrsg_ng_calculation = hrsg_ng_calculation
        self.final_u4u_demands = dict(u4u_demands)
        self.final_total_demands = dict(total_demands)
        self.final_detail_records = detail_records
        self.final_u4u_power = utility_consumption.get("u4u_power")
        self.final_u4u_lp_steam = utility_consumption.get("u4u_lp_steam")
        self.final_u4u_mp_steam = utility_consumption.get("u4u_mp_steam")

        # LP/MP balance from steam_balance
        self.final_lp_balance = steam_balance.get("lp_balance")
        self.final_mp_balance = steam_balance.get("mp_balance")

        # SHP balance
        if shp_capacity:
            total_shp_demand = steam_balance["summary"]["total_shp_demand"]
            total_free_steam = shp_capacity.get("total_free_steam_mt", 0.0)
            net_shp_demand = max(0.0, total_shp_demand - total_free_steam)
            self.final_shp_balance = check_shp_balance(net_shp_demand, shp_capacity)
            if hrsg_dispatch:
                self.final_shp_balance.update({
                    "actual_shp_supply_mt": round(hrsg_dispatch.get("total_shp_supply_mt", 0.0), 2),
                    "actual_supplementary_firing_mt": round(hrsg_dispatch.get("total_dispatched_supp_mt", 0.0), 2),
                    "excess_steam_mt": round(hrsg_dispatch.get("excess_steam_mt", 0.0), 2),
                })

    def _build_result(self) -> dict:
        """Build the final result dict."""
        final_excess_power = self.final_power_result.get("excessPowerForExport", 0) if self.final_power_result else 0.0

        return {
            "success": self.converged,
            "converged": self.converged,
            "iterations_used": len(self.iteration_history),
            "error_type": None if self.converged else "NMD_NOT_CONVERGED",
            "message": "NMD iteration converged" if self.converged else "NMD iteration did not converge",

            # STG Extraction
            "stg_extraction": self.final_stg_extraction,

            # Power results
            "power_result": self.final_power_result,
            "final_dispatch": self.final_dispatch,

            # Steam results
            "final_steam_balance": self.final_steam_balance,
            "final_hrsg_availability": self.final_hrsg_availability,
            "final_shp_capacity": self.final_shp_capacity,
            "final_shp_balance": self.final_shp_balance,

            # LP/MP balance
            "final_lp_balance": self.final_lp_balance,
            "final_mp_balance": self.final_mp_balance,

            # HRSG dispatch
            "hrsg_dispatch": self.final_hrsg_dispatch,

            # HRSG NG reverse calculation
            "hrsg_ng_calculation": self.final_hrsg_ng_calculation,

            # STG reduction
            "stg_reduction_mwh": round(self.stg_reduction_mwh, 2),
            "import_compensation_mwh": round(self.import_compensation_mwh, 2),

            # Export power
            "excess_power_for_export_mwh": round(final_excess_power, 2),
            "export_available": self.export_available,

            # U4U results
            "final_u4u_power": self.final_u4u_power,
            "final_u4u_lp_steam": self.final_u4u_lp_steam,
            "final_u4u_mp_steam": self.final_u4u_mp_steam,
            "final_u4u_demands": self.final_u4u_demands,
            "final_total_demands": self.final_total_demands,

            # Iteration history
            "iteration_history": self.iteration_history,
        }

    def _empty_result(self) -> dict:
        """Return empty result when no norms available."""
        return {
            "success": False,
            "converged": False,
            "iterations_used": 0,
            "error_type": "NO_NORMS",
            "message": "No consumption norms available for this month/year",
            "iteration_history": [],
        }

    def _power_failure_result(self, power_result: dict, iteration_history: list) -> dict:
        """Return failure result for power dispatch failure."""
        return {
            "success": False,
            "converged": False,
            "error_type": "POWER_INSUFFICIENT",
            "message": power_result.get("message", "Power capacity insufficient"),
            "power_result": power_result,
            "iteration_history": iteration_history,
        }

    # ------------------------------------------------------------------
    # Logging
    # ------------------------------------------------------------------

    def _log_iteration(
        self, iteration, total_demands, prev_total_demands,
        new_u4u_demands, old_u4u_demands, detail_records,
        power_result, stg_gross_mwh, gt_gross_mwh,
        current_aux_mwh, previous_aux_mwh,
        aux_power_error, shp_deficit, excess_steam_mt,
        lp_u4u_error, mp_u4u_error,
        prev_lp_u4u, prev_mp_u4u,
    ):
        """Log iteration details."""
        total_gen = power_result.get("totalGrossGeneration", 0)
        total_net = power_result.get("totalNetGeneration", 0)
        power_demand = total_demands.get("Power_Dis", 0.0)

        print(f"\n  {'='*96}")
        print(f"  === ITERATION {iteration} ===")
        print(f"  {'='*96}")
        print(f"  Power:  demand={power_demand:>12.2f} MWh   gen={total_gen:>12.2f} MWh   net={total_net:>12.2f} MWh")
        print(f"  STG:    {stg_gross_mwh:>12.2f} MWh   GT total: {gt_gross_mwh:>12.2f} MWh")
        print(f"  Aux:    current={current_aux_mwh:>10.4f} MWh   prev={previous_aux_mwh:>10.4f} MWh   error={aux_power_error:.6f}")
        print(f"  SHP:    deficit={shp_deficit:>10.2f} MT   excess={excess_steam_mt:>10.2f} MT")
        print(f"  U4U:    LP error={lp_u4u_error:.2f} MT   MP error={mp_u4u_error:.2f} MT")

    def _log_final_summary(self):
        """Log final summary after iteration loop completes."""
        print(f"\n  {'='*78}")
        print(f"  NMD ITERATION LOOP - FINAL SUMMARY")
        print(f"  {'='*78}")
        print(f"  Converged:        {'YES' if self.converged else 'NO'}")
        print(f"  Iterations Used:  {len(self.iteration_history)}")
        if self.final_u4u_demands:
            print(f"  Final U4U Demands:")
            for material, value in sorted(self.final_u4u_demands.items()):
                if abs(value) > 0.01:
                    print(f"    {material:<30s}  {value:>14.2f}")
        print(f"  {'='*78}")
