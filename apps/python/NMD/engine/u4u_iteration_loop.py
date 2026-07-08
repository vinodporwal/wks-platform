"""
NMD U4U Iteration Loop — Iterative Utility-for-Utility Balancing
=================================================================

Dynamic, data-driven iterative balancing for the NMD CPP plant.

Algorithm:
  1. Start with initial demands (process + fixed) for ALL utilities.
  2. Dispatch power (STG-first at min, then GTs, then STG ramp-up).
  3. Dispatch steam (LP → MP → HP → SHP cascade with HRSGs).
  4. Calculate U4U consumption: for each generation utility, multiply
     its output quantity by the per-unit consumption norms.
  5. Update demands: initial + U4U.
  6. Handle excess SHP steam → push to STG, reduce GTs.
  7. Repeat until convergence or max iterations.

Design principles:
  - NO hardcoded utility names. All utilities discovered dynamically
    from NormsHeader / CPPNorms / CalculatedProcessDemand tables.
  - New utilities automatically picked up from demand or norms tables.
  - Utilities with demand but no norms → pass-through (demand = supply).
  - Future-proof: adding a new utility to NormsHeader is sufficient.

Convergence:
  - U4U tolerance: 0.0001 (0.01%) relative change between iterations.
  - Power delta: 0.02 MWh absolute change.
  - Max iterations: 50 (with stall detection).
"""

from __future__ import annotations

import logging
from typing import Dict, List, Optional, Set, Tuple

from database.queries import fetch_consolidated_demand_with_uom
from engine.dispatch_engine import (
    dispatch_power,
    dispatch_steam,
    clear_db_cache,
    STEAM_TO_POWER_MT_PER_MWH,
    EXCESS_STEAM_THRESHOLD_MT,
)
from engine.norms_reader import NMDNormsReader

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Convergence parameters (same as JMD)
# ---------------------------------------------------------------------------
CONVERGENCE_TOLERANCE = 0.01     # 1% relative change in U4U demands (increased from 0.01% for chemical utility volatility)
POWER_DELTA_TOLERANCE = 0.02     # MWh absolute change in power demand
MAX_ITERATIONS = 50              # Max iterations
STALL_LIMIT = 5                  # consecutive iterations with small power delta to force convergence
STALL_POWER_THRESHOLD = 500.0    # MWh - if power delta < this for STALL_LIMIT iterations, force convergence

# ---------------------------------------------------------------------------
# Excess-steam proportional controller (ported from PPPython-script's
# HRSG_MIN_EXCESS controller in iteration_service.py). Instead of hard
# resetting STG to MIN whenever the computed excess drops to zero (which
# causes GTs to ramp up -> more free steam -> excess reappears -> STG snaps
# back up -> oscillation), the controller computes a target STG load from
# the *current* STG output + steam-to-power conversion, clamps it to STG's
# actual min/max, and only changes the override when the required
# adjustment is meaningful. Otherwise it HOLDS the existing override.
# ---------------------------------------------------------------------------
EXCESS_STEAM_ACTION_THRESHOLD_MT = 1.0            # MT — minimum meaningful STG adjustment to act on
EXCESS_STEAM_PRACTICAL_TOLERANCE_MT = 0.1         # MT — treat excess below this as balanced
POWER_INITIAL_CONVERGENCE_AUX_ERROR_MWH = 10.0    # MWh — aux-power error below which the excess-steam controller may engage

# ---------------------------------------------------------------------------
# GT Natural Gas MMBTU Calculation Constants (from PPPython-script)
# ---------------------------------------------------------------------------
# Formula: NET GT MMBTU = GROSS MMBTU - FREE STEAM MMBTU
# GROSS MMBTU = KWH × Heat Rate × 3.96567 / 1,000,000
# FREE STEAM MMBTU = KWH × FreeSteamFactor × 760.87 × 3.96567 / 1,000,000
GT_NG_KCAL_TO_BTU = 3.96567
GT_NG_BTU_TO_MMBTU = 1_000_000
GT_NG_FREE_STEAM_ENERGY_KCAL_KG = 760.87  # (810 - 110) / 0.92 KCAL/kg


def calculate_gt_ng_mmbtu(
    kwh: float,
    heat_rate: float,
    free_steam_factor: float,
) -> float:
    """
    Calculate GT Natural Gas MMBTU using heat rate lookup.

    Formula:
        NET GT MMBTU = GROSS MMBTU - FREE STEAM MMBTU
        GROSS MMBTU = KWH × Heat Rate × 3.96567 / 1,000,000
        FREE STEAM MMBTU = KWH × FreeSteamFactor × 760.87 × 3.96567 / 1,000,000

    Args:
        kwh: GT generation in KWH
        heat_rate: Heat rate from HeatRateLookup table (KCAL/KWH)
        free_steam_factor: Free steam factor from HeatRateLookup table

    Returns:
        Net GT MMBTU (final value)
    """
    if kwh <= 0 or heat_rate is None or free_steam_factor is None:
        return 0.0

    # Gross MMBTU = KWH × Heat Rate × 3.96567 / 1,000,000
    gross_mmbtu = kwh * heat_rate * GT_NG_KCAL_TO_BTU / GT_NG_BTU_TO_MMBTU

    # Free Steam MMBTU = KWH × FreeSteamFactor × 760.87 × 3.96567 / 1,000,000
    free_steam_mmbtu = kwh * free_steam_factor * GT_NG_FREE_STEAM_ENERGY_KCAL_KG * GT_NG_KCAL_TO_BTU / GT_NG_BTU_TO_MMBTU

    # Net MMBTU = Gross - Free Steam
    net_mmbtu = gross_mmbtu - free_steam_mmbtu

    return net_mmbtu


def calculate_hrsg_ng_mmbtu(
    total_steam_mt: float,
    ng_norm_mmbtu_mt: float = 2.954424,
) -> float:
    """
    Calculate HRSG Natural Gas MMBTU for total steam generation.

    Uses total steam generation (supplementary + free steam) to match BPC methodology.
    Note: Free steam technically comes from GT exhaust heat and doesn't consume
    additional NG, but BPC calculates NG based on total steam generation.

    Formula:
        NG MMBTU = Total Steam MT × NG Norm (MMBTU/MT)

    Args:
        total_steam_mt: Total steam generation in MT (supplementary + free steam)
        ng_norm_mmbtu_mt: NG norm in MMBTU/MT (default: 2.954424 from BPC.ods)

    Returns:
        NG consumption in MMBTU
    """
    if total_steam_mt <= 0:
        return 0.0
    return total_steam_mt * ng_norm_mmbtu_mt


_MONTH_NAMES = {
    1: "January", 2: "February", 3: "March", 4: "April",
    5: "May", 6: "June", 7: "July", 8: "August",
    9: "September", 10: "October", 11: "November", 12: "December",
}


# ---------------------------------------------------------------------------
# Utility name normalization
# ---------------------------------------------------------------------------
def _norm_name(name: str) -> str:
    """Normalize utility name for matching (lowercase, alphanumeric only)."""
    return "".join(ch.lower() for ch in str(name) if ch.isalnum())


def _names_match(a: str, b: str) -> bool:
    """Check if two names refer to the same plant/asset (fuzzy match)."""
    na = _norm_name(a)
    nb = _norm_name(b)
    if not na or not nb:
        return False
    if na == nb:
        return True
    # Handle cases like "NMD - Power Plant 2" vs "NMD-Power Plant-2"
    return na in nb or nb in na


# ---------------------------------------------------------------------------
# Steam grade mapping
# ---------------------------------------------------------------------------
_STEAM_GRADES = ["lp", "mp", "hp", "shp"]
_STEAM_MATERIAL_TO_GRADE = {
    "LP Steam_Dis": "lp",
    "MP Steam_Dis": "mp",
    "HP Steam_Dis": "hp",
    "SHP Steam_Dis": "shp",
}


def _is_steam_material(material: str) -> Optional[str]:
    """Return steam grade if material is a steam distribution utility, else None."""
    return _STEAM_MATERIAL_TO_GRADE.get(material)


# ---------------------------------------------------------------------------
# Main iteration loop
# ---------------------------------------------------------------------------

class NMDU4UIterationLoop:
    """
    Iterative U4U balancing loop for NMD plant.

    Usage::

        loop = NMDU4UIterationLoop(plant_id, month, year, norms_reader=reader)
        result = loop.run()
    """

    def __init__(
        self,
        plant_id: str,
        month: int,
        year: int,
        norms_reader: Optional[NMDNormsReader] = None,
        convergence_tolerance: float = CONVERGENCE_TOLERANCE,
        power_delta_tolerance: float = POWER_DELTA_TOLERANCE,
        max_iterations: int = MAX_ITERATIONS,
    ):
        self.plant_id = plant_id
        self.month = month
        self.year = year
        self.norms_reader = norms_reader or NMDNormsReader.get_reader(month, year)
        self.convergence_tolerance = convergence_tolerance
        self.power_delta_tolerance = power_delta_tolerance
        self.max_iterations = max_iterations

        # Demand data (built in run())
        self._initial_demands: Dict[str, float] = {}   # utility_name → total demand
        self._uom_map: Dict[str, str] = {}              # utility_name → UOM
        self._all_utilities: Set[str] = set()           # all known utilities
        self._generation_utilities: Set[str] = set()    # utilities that produce something
        self._pass_through_utilities: Set[str] = set()  # demand but no norms

        # U4U norms matrix: {utility: {material: norm}}
        self._u4u_matrix: Dict[str, Dict[str, float]] = {}

        # Results
        self.iteration_history: List[dict] = []
        self.final_power_result: Optional[dict] = None
        self.final_steam_result: Optional[dict] = None
        self.final_u4u_demands: Dict[str, float] = {}
        self.final_total_demands: Dict[str, float] = {}
        self.final_detail_records: List[dict] = []
        self.converged: bool = False
        self.iterations_used: int = 0
        self._power_initially_converged: bool = False   # excess-steam controller only engages once power has roughly stabilized
        self._excess_steam_controller_mode: Optional[str] = None  # None or "HRSG_MIN_EXCESS"

    # ------------------------------------------------------------------
    # Public entry point
    # ------------------------------------------------------------------

    def run(self) -> dict:
        """Run the U4U iteration loop until convergence or max iterations."""

        # 0. Clear dispatch-engine DB cache (fresh data for this month)
        clear_db_cache()

        # 1. Load norms and build U4U matrix
        self._u4u_matrix = self.norms_reader.get_consumption_norms_flat()
        self._generation_utilities = self.norms_reader.get_generation_utilities()

        # 2. Build initial demands from process + fixed (dynamic)
        self._build_initial_demands()

        # 3. Identify pass-through utilities (demand but no norms)
        self._identify_pass_throughs()

        # 4. Log header
        self._log_header()

        # 5. Iteration loop
        u4u_demands: Dict[str, float] = {u: 0.0 for u in self._all_utilities}
        prev_u4u: Dict[str, float] = {}
        prev_power_demand: float = 0.0
        stg_min_override_mwh: Optional[float] = None
        gt_reduction_mwh: float = 0.0
        stall_counter = 0
        power_result = {}
        steam_result = {}
        detail_records: List[dict] = []

        for iteration in range(1, self.max_iterations + 1):
            self.iterations_used = iteration

            # Build total demands = initial + U4U
            total_demands = {
                u: self._initial_demands.get(u, 0.0) + u4u_demands.get(u, 0.0)
                for u in self._all_utilities
            }

            # Build dispatch demands dict
            dispatch_demands = self._build_dispatch_demands(total_demands)

            # Dispatch power
            power_result = dispatch_power(
                self.plant_id, self.month, self.year,
                demands=dispatch_demands,
                norms_reader=self.norms_reader,
                stg_min_override_mwh=stg_min_override_mwh,
                gt_reduction_mwh=gt_reduction_mwh,
            )

            # Calculate STG extraction based on curve
            stg_extraction = self._calculate_stg_extraction(power_result)

            # Dispatch steam
            steam_result = dispatch_steam(
                self.plant_id, self.month, self.year,
                power_result=power_result,
                demands=dispatch_demands,
                norms_reader=self.norms_reader,
                stg_extraction=stg_extraction,
            )

            # Calculate U4U consumption
            new_u4u, detail_records = self._calculate_all_u4u(
                power_result, steam_result, total_demands,
            )

            # Aux power (U4U Power_Dis) change vs previous iteration — used to
            # decide when power has stabilized enough to engage the excess-
            # steam controller (mirrors PPPython's aux_power_error check).
            aux_power_error_mwh = abs(
                new_u4u.get("Power_Dis", 0.0) - u4u_demands.get("Power_Dis", 0.0)
            ) / 1000.0

            # Handle excess SHP steam
            excess_steam_mt, stg_min_override_mwh, gt_reduction_mwh = \
                self._handle_excess_steam(
                    steam_result, power_result, stg_extraction,
                    aux_power_error_mwh, stg_min_override_mwh,
                )

            # Check convergence
            converged, power_delta = self._check_convergence(
                new_u4u, u4u_demands, total_demands, prev_power_demand,
            )

            # Log iteration
            self._log_iteration(
                iteration, total_demands, prev_u4u if prev_u4u else {},
                new_u4u, u4u_demands, detail_records,
                power_result, steam_result, excess_steam_mt,
                stg_min_override_mwh, gt_reduction_mwh,
                power_delta, converged,
            )

            # Store history
            self.iteration_history.append({
                "iteration": iteration,
                "total_demands": dict(total_demands),
                "u4u_demands": dict(new_u4u),
                "power_generation_mwh": power_result.get("total_generation_mwh", 0.0),
                "steam_generation_mt": steam_result.get("total_generation_mt", 0.0),
                "power_delta": power_delta,
                "converged": converged,
            })

            prev_u4u = dict(u4u_demands)
            u4u_demands = dict(new_u4u)
            prev_power_demand = total_demands.get("Power_Dis", 0.0)

            self.final_power_result = power_result
            self.final_steam_result = steam_result
            self.final_u4u_demands = dict(u4u_demands)
            self.final_total_demands = dict(total_demands)
            self.final_detail_records = detail_records

            if converged:
                self.converged = True
                logger.info("")
                logger.info("  [U4U LOOP] === CONVERGED in %d iterations ===",
                            iteration)
                break

            # Stall detection - force convergence if power delta is small for multiple iterations
            if power_delta < STALL_POWER_THRESHOLD:
                stall_counter += 1
            else:
                stall_counter = 0

            if stall_counter >= STALL_LIMIT:
                logger.info("")
                logger.info("  [U4U LOOP] Power demand stabilized (delta < %.0f MWh for %d iterations) at iteration %d — "
                            "forcing convergence", STALL_POWER_THRESHOLD, STALL_LIMIT, iteration)
                self.converged = True
                break

        if not self.converged:
            logger.warning("")
            logger.warning("  [U4U LOOP] *** Did NOT converge after %d iterations ***",
                           self.max_iterations)

        # Log final summary
        self._log_final_summary()

        return {
            "converged": self.converged,
            "iterations_used": self.iterations_used,
            "power_result": self.final_power_result,
            "steam_result": self.final_steam_result,
            "u4u_demands": self.final_u4u_demands,
            "total_demands": self.final_total_demands,
            "detail_records": self.final_detail_records,
            "iteration_history": self.iteration_history,
            "pass_through_utilities": list(self._pass_through_utilities),
        }

    # ------------------------------------------------------------------
    # Initial demands
    # ------------------------------------------------------------------

    def _build_initial_demands(self):
        """Build initial demands from process + fixed (dynamic, no hardcoding)."""
        rows = fetch_consolidated_demand_with_uom(self.plant_id, self.month, self.year)

        for row in rows:
            util = row["utility_name"]
            uom = row.get("uom", "")
            total = float(row.get("total_value", 0.0))

            self._initial_demands[util] = total
            self._uom_map[util] = uom
            self._all_utilities.add(util)

        # Also add generation utilities from norms (may not have demand)
        for util in self._generation_utilities:
            self._all_utilities.add(util)
            if util not in self._initial_demands:
                self._initial_demands[util] = 0.0

        logger.info("  [U4U LOOP] Initial demands: %d utilities (%d from demand, "
                    "%d from norms)",
                    len(self._all_utilities),
                    len(self._initial_demands),
                    len(self._generation_utilities))

    def _identify_pass_throughs(self):
        """Identify utilities with demand but no generation norms (pass-through)."""
        self._pass_through_utilities = set()
        for util in self._all_utilities:
            if util not in self._generation_utilities and self._initial_demands.get(util, 0.0) > 0:
                self._pass_through_utilities.add(util)

        if self._pass_through_utilities:
            logger.info("  [U4U LOOP] Pass-through utilities (demand but no norms): %s",
                        ", ".join(sorted(self._pass_through_utilities)))

    # ------------------------------------------------------------------
    # Dispatch demands builder
    # ------------------------------------------------------------------

    def _calculate_stg_extraction(self, power_result: dict) -> dict:
        """
        Calculate STG extraction (LP/MP steam) based on STG load from curve lookup.

        Args:
            power_result: Result dict from dispatch_power() containing dispatched assets

        Returns:
            Dict with STG extraction quantities:
            {
                "LP Steam_Dis": <extraction MT>,
                "MP Steam_Dis": <extraction MT>
            }
        """
        extraction = {}

        # Find STG asset in power_result
        for asset in power_result.get("assets", []):
            asset_name = str(asset.get("asset_name", "")).upper()
            if "STG" in asset_name:
                # Get STG load (dispatched MW)
                stg_load_mw = float(asset.get("dispatched_mw", 0.0))
                stg_op_hours = float(asset.get("op_hours", 0.0))

                if stg_load_mw > 0 and stg_op_hours > 0:
                    # Get extraction rates from curve lookup
                    extraction_data = self.norms_reader.get_stg_extraction_for_load(stg_load_mw)
                    if extraction_data:
                        # Calculate extraction quantities
                        lp_extraction_mt = extraction_data.get("sl_ext_flow_tph", 0.0) * stg_op_hours
                        mp_extraction_mt = extraction_data.get("sm_bleed_flow_tph", 0.0) * stg_op_hours

                        extraction["LP Steam_Dis"] = lp_extraction_mt
                        extraction["MP Steam_Dis"] = mp_extraction_mt
                        # Dynamic SHP-steam-to-power conversion rate (MT/MWh) at this
                        # load point, from the STG extraction curve — used by the
                        # excess-steam controller instead of a fixed constant.
                        extraction["sp_steam_power"] = extraction_data.get("sp_steam_power", 0.0)

                        logger.info(
                            "  [STG EXTRACTION] STG Load: %.2f MW, LP Extraction: %.2f MT, MP Extraction: %.2f MT",
                            stg_load_mw, lp_extraction_mt, mp_extraction_mt
                        )
                break

        return extraction

    def _build_dispatch_demands(self, total_demands: dict) -> dict:
        """Build demands dict for dispatch_power / dispatch_steam.

        Maps utility names to the format expected by the dispatch engine.
        Power demand is converted to MWh (process + fixed + U4U).
        Steam demands are in MT.
        """
        demands = {}

        # Power demand
        power_total_kwh = total_demands.get("Power_Dis", 0.0)
        # Process power is in KWH from DB, but fixed may be in MWh.
        # fetch_consolidated_demand_with_uom returns total in native UOM (KWH for power).
        # For dispatch, we need MWh.
        power_process_mwh = self._initial_demands.get("Power_Dis", 0.0) / 1000.0
        power_u4u_kwh = total_demands.get("Power_Dis", 0.0) - self._initial_demands.get("Power_Dis", 0.0)
        power_u4u_mwh = power_u4u_kwh / 1000.0
        power_fixed_mwh = 0.0  # Fixed power is already included in initial demand

        demands["_power_process_mwh"] = round(power_process_mwh + power_u4u_mwh, 2)
        demands["_power_fixed_mwh"] = round(power_fixed_mwh, 2)
        demands["power_process"] = power_total_kwh
        demands["power_fixed"] = power_fixed_mwh

        # Steam demands (process + fixed + U4U) per grade
        for grade in _STEAM_GRADES:
            material = f"{grade.upper()} Steam_Dis"
            base = self._initial_demands.get(material, 0.0)
            u4u = total_demands.get(material, 0.0) - base
            demands[f"{grade}_process"] = base + u4u
            demands[f"{grade}_fixed"] = 0.0

        # Store all utility demands for reference
        for util, val in total_demands.items():
            if util not in demands:
                demands[util] = val

        return demands

    # ------------------------------------------------------------------
    # U4U calculation
    # ------------------------------------------------------------------

    def _calculate_all_u4u(
        self,
        power_result: dict,
        steam_result: dict,
        total_demands: dict,
    ) -> Tuple[Dict[str, float], List[dict]]:
        """
        Calculate U4U consumption from all generation utilities.

        Returns (u4u_dict, detail_records) where:
        - u4u_dict: {material_name: total_u4u_demand}
        - detail_records: list of dicts with per-producer breakdown
        """
        u4u: Dict[str, float] = {}
        details: List[dict] = []

        # Get consumption norms matrix
        consumption_norms = self.norms_reader.get_consumption_norms()

        # Power generation quantity (KWH for norm calculation)
        power_gen_mwh = float(power_result.get("total_generation_mwh", 0.0))
        power_gen_kwh = power_gen_mwh * 1000.0

        # Import power — added to Power_Dis total generation
        import_power = power_result.get("import_power", {})
        import_mwh = float(import_power.get("total_mwh", 0.0)) if isinstance(import_power, dict) and import_power.get("success") else 0.0
        import_kwh = import_mwh * 1000.0

        # Total Power_Dis generation = GT/STG generation + import power
        power_dis_gen_kwh = power_gen_kwh + import_kwh

        # Steam generation quantities per HRSG asset
        steam_gen_mt: Dict[str, float] = {}
        for asset in steam_result.get("assets", []):
            asset_name = asset.get("asset_name", "")
            total_output = float(asset.get("total_output_mt", 0.0))
            if total_output > 0:
                steam_gen_mt[asset_name] = total_output

        # Calculate U4U for each generation utility
        for matrix_key, info in consumption_norms.items():
            consumptions = info.get("consumptions", [])
            plant_name = info.get("plant_name", "")
            utility = info.get("utility", matrix_key)
            util_upper = utility.upper()

            # Determine generation quantity for this utility
            gen_qty = self._get_generation_quantity(
                utility, plant_name, power_gen_kwh, power_gen_mwh,
                steam_gen_mt, total_demands, power_result,
                power_dis_gen_kwh,
            )

            if gen_qty <= 0:
                # For POWERGEN, still create records with 0 generation
                # so unavailable assets appear in the comparison table
                if util_upper != "POWERGEN":
                    continue
                # Create 0-quantity records for this POWERGEN asset
                for c in consumptions:
                    # Include ALL accounts (not just Utilities/Catalyst & Chemical/Raw Material)
                    material = c.get("material", "")
                    material_uom = c.get("material_uom", "")
                    issuing_plant = c.get("issuing_plant", "")
                    details.append({
                        "producer": plant_name or utility,
                        "producer_utility": utility,
                        "generation": 0.0,
                        "material": material,
                        "material_uom": material_uom,
                        "norm": c.get("norm", 0.0),
                        "quantity": 0.0,
                        "issuing_plant": issuing_plant,
                        "is_credit": False,
                        "norms_header_id": c.get("norms_header_id"),
                        "norms_month_detail_id": c.get("norms_month_detail_id"),
                    })
                continue

            # Multiply generation by each consumption norm
            for c in consumptions:
                # Include ALL accounts (not just Utilities/Catalyst & Chemical/Raw Material)
                norm = c["norm"]
                if norm == 0:
                    continue

                material = c["material"]
                material_uom = c.get("material_uom", "")
                issuing_plant = c.get("issuing_plant", "")

                # For NATURAL GAS, use reverse calculation from heat rate lookup if available
                if material.upper() == "NATURAL GAS" and util_upper == "POWERGEN":
                    # Try to get heat rate data from power_result for this plant
                    heat_rate = None
                    free_steam_factor = None
                    
                    # Find the GT asset for this plant in power_result
                    for asset in power_result.get("assets", []):
                        asset_name = asset.get("asset_name", "")
                        
                        # Match plant name (asset_name contains plant name, e.g., "NMD-Power Plant-2")
                        # consumption matrix has plant_name like "NMD - Power Plant 2"
                        # GT assets are identified by having heat_rate field (not by "GT" in name)
                        if _names_match(asset_name, plant_name):
                            heat_rate = asset.get("heat_rate")
                            free_steam_factor = asset.get("free_steam_factor")
                            if heat_rate is not None:
                                logger.info("  [REVERSE NG] Found GT asset: %s, plant: %s, heat_rate: %s, free_steam: %s",
                                            asset_name, plant_name, heat_rate, free_steam_factor)
                                break
                    
                    # Use reverse calculation if heat rate data is available
                    if heat_rate and free_steam_factor and gen_qty > 0:
                        consumed = calculate_gt_ng_mmbtu(gen_qty, heat_rate, free_steam_factor)
                        logger.info("  [REVERSE NG] %s: gen_qty=%.0f KWH, heat_rate=%.2f, free_steam=%.4f, consumed=%.2f MMBTU (REVERSE)",
                                    plant_name, gen_qty, heat_rate, free_steam_factor, consumed)
                    else:
                        # Fall back to norm-based calculation
                        consumed = gen_qty * norm
                        logger.info("  [REVERSE NG] %s: gen_qty=%.0f KWH, norm=%.6f, consumed=%.2f MMBTU (NORM-BASED, heat_rate=%s, free_steam=%s)",
                                    plant_name, gen_qty, norm, consumed, heat_rate, free_steam_factor)
                elif material.upper() == "NATURAL GAS" and "HRSG" in util_upper and "SHP STEAM" in util_upper:
                    # HRSG NG reverse calculation using total steam (supplementary + free steam)
                    # Matches BPC methodology: NG based on total steam generation
                    total_steam_mt = 0.0
                    
                    # Find total steam generation for this HRSG from steam_result
                    for asset in steam_result.get("assets", []):
                        asset_name = asset.get("asset_name", "")
                        if _names_match(asset_name, utility):
                            # Use total steam (dispatched_mt + free_steam_mt) to match BPC
                            supp_steam = asset.get("dispatched_mt", 0.0)
                            free_steam = asset.get("free_steam_mt", 0.0)
                            total_steam_mt = supp_steam + free_steam
                            logger.info("  [REVERSE NG HRSG] Found HRSG: %s, supp_steam=%.2f MT, free_steam=%.2f MT, total_steam=%.2f MT",
                                        asset_name, supp_steam, free_steam, total_steam_mt)
                            break
                    
                    # Use reverse calculation if total steam data is available
                    if total_steam_mt > 0:
                        consumed = calculate_hrsg_ng_mmbtu(total_steam_mt, norm)
                        logger.info("  [REVERSE NG HRSG] %s: total_steam=%.2f MT, norm=%.6f, consumed=%.2f MMBTU (REVERSE, BPC method)",
                                    utility, total_steam_mt, norm, consumed)
                    else:
                        # Fall back to norm-based calculation on total generation
                        consumed = gen_qty * norm
                        logger.info("  [REVERSE NG HRSG] %s: gen_qty=%.2f MT, norm=%.6f, consumed=%.2f MMBTU (NORM-BASED, no steam data)",
                                    utility, gen_qty, norm, consumed)
                else:
                    # Standard norm-based calculation for other materials
                    consumed = gen_qty * norm

                # Import power is a known input, not norm-derived.
                # Use actual import value instead of norm × generation.
                if "MEL" in material.upper() and import_kwh > 0:
                    consumed = import_kwh

                # Negative norms are byproduct credits (e.g., LP steam from HRSG)
                # Include in details but don't add to U4U demand
                if norm < 0:
                    details.append({
                        "producer": plant_name or utility,
                        "producer_utility": utility,
                        "generation": gen_qty,
                        "material": material,
                        "material_uom": material_uom,
                        "norm": norm,
                        "quantity": consumed,
                        "issuing_plant": issuing_plant,
                        "is_credit": True,
                        "norms_header_id": c.get("norms_header_id"),
                        "norms_month_detail_id": c.get("norms_month_detail_id"),
                    })
                    continue

                # U4U demand is accumulated in same unit as initial demands
                u4u_amount = consumed

                u4u[material] = u4u.get(material, 0.0) + u4u_amount

                details.append({
                    "producer": plant_name or utility,
                    "producer_utility": utility,
                    "generation": gen_qty,
                    "material": material,
                    "material_uom": material_uom,
                    "norm": norm,
                    "quantity": consumed,
                    "issuing_plant": issuing_plant,
                    "is_credit": False,
                    "norms_header_id": c.get("norms_header_id"),
                    "norms_month_detail_id": c.get("norms_month_detail_id"),
                })

        return u4u, details

    def _get_generation_quantity(
        self,
        utility: str,
        plant_name: str,
        power_gen_kwh: float,
        power_gen_mwh: float,
        steam_gen_mt: dict,
        total_demands: dict,
        power_result: dict,
        power_dis_gen_kwh: float = 0.0,
    ) -> float:
        """
        Determine the generation quantity for a utility in its native UOM.

        - POWERGEN: per-asset from power dispatch (KWH)
        - Power_Dis: GT/STG generation + import power (KWH)
        - HRSG*_SHP STEAM: from steam dispatch (MT)
        - Other generation utilities: from total demand (native UOM)
        """
        util_upper = utility.upper()
        util_norm = _norm_name(utility)

        # POWERGEN — per-asset generation in KWH (norms are per KWH)
        if util_upper == "POWERGEN":
            # Look up per-asset dispatched MWh from power_result
            for asset in power_result.get("assets", []):
                asset_name = str(asset.get("asset_name", ""))
                if _names_match(plant_name, asset_name):
                    return float(asset.get("dispatched_mwh", 0.0)) * 1000.0
            # Asset not found (unavailable) — no generation
            return 0.0

        # HRSG steam assets — generation in MT
        if "HRSG" in util_upper and "SHP" in util_upper:
            # Match by asset name in steam dispatch
            for asset_name, mt in steam_gen_mt.items():
                if _norm_name(utility) in _norm_name(asset_name) or \
                   _norm_name(asset_name) in _norm_name(utility):
                    return mt
            # Fallback: if no dispatch match, use demand
            return total_demands.get(utility, 0.0)

        # STG steam extractions — these come from STG operation
        if "STG" in util_upper and ("LP" in util_upper or "MP" in util_upper):
            # STG extraction quantities are determined by steam cascade
            # Use total demand as generation quantity
            return total_demands.get(utility, 0.0)

        # PRDS (letdown stations) — determined by steam cascade
        if "PRDS" in util_upper:
            return total_demands.get(utility, 0.0)

        # Power from MEL (imported power) — from power_result
        if "MEL" in util_upper:
            import_power = power_result.get("import_power", {})
            import_mwh = float(import_power.get("total_mwh", 0.0)) if isinstance(import_power, dict) and import_power.get("success") else 0.0
            return import_mwh * 1000.0

        # Distribution utilities (Power_Dis, SHP Steam_Dis, etc.)
        # Power_Dis generation = GT/STG generation + import power
        # Other distribution utilities: total demand they need to supply
        if util_upper == "POWER_DIS":
            return power_dis_gen_kwh
        if utility in total_demands:
            return total_demands[utility]

        # Fallback: BPC generation quantity from norms
        bpc_gen = self.norms_reader.get_bpc_generation_quantities()
        return bpc_gen.get(utility, 0.0)

    # ------------------------------------------------------------------
    # Excess SHP steam handling
    # ------------------------------------------------------------------

    def _handle_excess_steam(
        self,
        steam_result: dict,
        power_result: dict,
        stg_extraction: dict,
        aux_power_error_mwh: float,
        current_stg_min_override_mwh: Optional[float],
    ) -> Tuple[float, Optional[float], float]:
        """
        Handle excess SHP steam using a proportional controller (ported from
        PPPython-script's HRSG_MIN_EXCESS controller).

        Instead of hard-resetting STG to MIN whenever excess drops to zero
        (which causes oscillation), the controller computes a target STG load
        from the current STG output + steam-to-power conversion, clamps it to
        STG's actual min/max, and only changes the override when the required
        adjustment is meaningful. Otherwise it HOLDS the existing override.

        Returns:
            (excess_steam_mt, stg_min_override_mwh, gt_reduction_mwh)
        """
        excess_mt = float(steam_result.get("surplus_mt", 0.0))

        # Check if power has initially converged (aux error is small enough)
        if aux_power_error_mwh < POWER_INITIAL_CONVERGENCE_AUX_ERROR_MWH:
            self._power_initially_converged = True

        # Find current STG generation and min/max limits
        stg_gross_mwh = 0.0
        stg_db_min_mwh = 0.0
        stg_db_max_mwh = 0.0
        for asset in power_result.get("assets", []):
            name = str(asset.get("asset_name", "")).upper()
            if "STG" in name or "STEAM TURBINE" in name:
                stg_gross_mwh = float(asset.get("dispatched_mwh", 0.0))
                min_mw = float(asset.get("min_mw", 0.0))
                max_mw = float(asset.get("max_mw", 0.0))
                op_hours = float(asset.get("op_hours", 0.0))
                stg_db_min_mwh = min_mw * op_hours
                stg_db_max_mwh = max_mw * op_hours
                break

        # Engage the proportional controller only after power has roughly stabilized
        if self._power_initially_converged and (
            excess_mt > EXCESS_STEAM_PRACTICAL_TOLERANCE_MT
            or self._excess_steam_controller_mode == "HRSG_MIN_EXCESS"
        ):
            # Use dynamic conversion factor if available from STG extraction curve
            conversion_rate = stg_extraction.get("sp_steam_power", STEAM_TO_POWER_MT_PER_MWH) if stg_extraction else STEAM_TO_POWER_MT_PER_MWH
            if conversion_rate <= 0:
                conversion_rate = STEAM_TO_POWER_MT_PER_MWH

            desired_stg_adj_mwh = excess_mt / conversion_rate
            target_stg_mwh = stg_gross_mwh + desired_stg_adj_mwh
            # Clamp to STG's actual min/max limits
            target_stg_mwh = max(stg_db_min_mwh, min(target_stg_mwh, stg_db_max_mwh))

            actual_stg_adj = target_stg_mwh - stg_gross_mwh
            action_threshold_mwh = EXCESS_STEAM_ACTION_THRESHOLD_MT / conversion_rate

            if abs(actual_stg_adj) > action_threshold_mwh or abs(excess_mt) > EXCESS_STEAM_PRACTICAL_TOLERANCE_MT:
                logger.info("  [U4U LOOP] ⚡ EXCESS STEAM BALANCING (PROPORTIONAL CONTROLLER)")
                logger.info("    Excess Steam: %.2f MT | Desired STG Adj: %.2f MWh | Target STG: %.2f MWh | Current STG: %.2f MWh",
                            excess_mt, desired_stg_adj_mwh, target_stg_mwh, stg_gross_mwh)
                logger.info("    ACTION: Adjusting STG override to %.2f MWh", target_stg_mwh)

                self._excess_steam_controller_mode = "HRSG_MIN_EXCESS"
                # GT reduction is handled naturally by power dispatch when STG increases
                return excess_mt, target_stg_mwh, 0.0
            else:
                # Excess is small — HOLD the existing override to prevent oscillation
                logger.info("  [U4U LOOP] Excess steam (%.2f MT) is small, holding STG override at %.2f MWh",
                            excess_mt, current_stg_min_override_mwh or 0.0)
                self._excess_steam_controller_mode = "HRSG_MIN_EXCESS"
                return excess_mt, current_stg_min_override_mwh, 0.0

        elif not self._power_initially_converged:
            # Power hasn't stabilized yet — don't engage excess steam controller
            self._excess_steam_controller_mode = None
            return 0.0, None, 0.0
        else:
            # Power converged but no excess steam — reset controller
            self._excess_steam_controller_mode = None
            return 0.0, None, 0.0

    # ------------------------------------------------------------------
    # Convergence check
    # ------------------------------------------------------------------

    def _check_convergence(
        self,
        new_u4u: dict,
        old_u4u: dict,
        total_demands: dict,
        prev_power_demand: float,
    ) -> Tuple[bool, float]:
        """
        Check if all U4U demands have converged within tolerance.

        Returns (converged, power_delta).
        """
        # Power delta
        current_power = total_demands.get("Power_Dis", 0.0)
        power_delta = abs(current_power - prev_power_demand)

        # U4U convergence: relative change per material
        # Use higher tolerance for small-magnitude utilities to prevent oscillation
        all_materials = set(new_u4u.keys()) | set(old_u4u.keys())
        for material in all_materials:
            # Skip chemical utilities from convergence check due to circular dependencies
            material_upper = material.upper()
            if ("CHEM" in material_upper or "CAUSTIC" in material_upper or 
                "SODA" in material_upper or "ALUM" in material_upper or
                "SULPHITE" in material_upper or "SULPHURIC" in material_upper or
                "MORPHO" in material_upper or "CYCLO" in material_upper):
                continue
            
            new_val = abs(new_u4u.get(material, 0.0))
            old_val = abs(old_u4u.get(material, 0.0))

            if new_val < 1e-10 and old_val < 1e-10:
                continue
            if new_val < 1e-10:
                return False, power_delta

            change = abs(new_val - old_val) / new_val
            abs_change = abs(new_val - old_val)

            # For small magnitudes, use 0.5 absolute change threshold OR 0.1% relative tolerance
            if new_val < 100000:  # Small magnitude threshold
                tolerance = max(self.convergence_tolerance * 10, 0.5 / new_val if new_val > 0 else 0.001)
            else:
                tolerance = self.convergence_tolerance

            if change > tolerance and abs_change > 0.5:  # Also require absolute change > 0.5
                return False, power_delta

        # Also check power delta
        if power_delta > self.power_delta_tolerance:
            return False, power_delta

        return True, power_delta

    # ------------------------------------------------------------------
    # Logging
    # ------------------------------------------------------------------

    def _log_header(self):
        """Log iteration loop header."""
        sep = "=" * 90
        logger.info("")
        logger.info("  %s", sep)
        logger.info("  U4U ITERATION LOOP  (%s %d)",
                    _MONTH_NAMES.get(self.month, self.month), self.year)
        logger.info("  %s", sep)
        logger.info("  Tolerance: %.4f%% (U4U)  |  %.2f MWh (power)  |  Max iterations: %d",
                    self.convergence_tolerance * 100, self.power_delta_tolerance,
                    self.max_iterations)
        logger.info("  Total utilities: %d  |  Generation utilities: %d  |  "
                    "Pass-through: %d",
                    len(self._all_utilities),
                    len(self._generation_utilities),
                    len(self._pass_through_utilities))
        if self._pass_through_utilities:
            logger.info("  Pass-through (no norms): %s",
                        ", ".join(sorted(self._pass_through_utilities)))
        logger.info("")

    def _log_iteration(
        self,
        iteration: int,
        total_demands: dict,
        prev_demands: dict,
        new_u4u: dict,
        old_u4u: dict,
        detail_records: list,
        power_result: dict,
        steam_result: dict,
        excess_steam_mt: float,
        stg_override: Optional[float],
        gt_reduction: float,
        power_delta: float,
        converged: bool,
    ):
        """Log detailed iteration info in table format."""
        sep = "-" * 90
        logger.info("")
        logger.info("  %s", sep)
        logger.info("  ITERATION %d", iteration)
        logger.info("  %s", sep)

        # Power summary
        power_demand = total_demands.get("Power_Dis", 0.0)
        power_gen = power_result.get("total_generation_mwh", 0.0)
        power_surplus = power_result.get("surplus_mwh", 0.0)
        power_deficit = power_result.get("deficit_mwh", 0.0)
        logger.info("  Power:  Demand = %14.2f KWH  |  Gen = %14.2f MWh  |  "
                    "Surplus = %8.2f  |  Deficit = %8.2f",
                    power_demand, power_gen, power_surplus, power_deficit)

        # Steam summary
        steam_gen = steam_result.get("total_generation_mt", 0.0)
        steam_surplus = steam_result.get("surplus_mt", 0.0)
        steam_deficit = steam_result.get("deficit_mt", 0.0)
        logger.info("  Steam:  Gen = %14.2f MT  |  Surplus = %8.2f  |  Deficit = %8.2f",
                    steam_gen, steam_surplus, steam_deficit)

        if excess_steam_mt > 0:
            logger.info("  Excess SHP: %10.2f MT → STG override = %.2f MWh, "
                        "GT reduction = %.2f MWh",
                        excess_steam_mt, stg_override or 0.0, gt_reduction)

        logger.info("  Power delta: %.4f MWh  |  Converged: %s",
                    power_delta, "YES" if converged else "NO")

        # Per-utility demand table
        logger.info("")
        logger.info("  %-35s  %14s  %14s  %14s  %10s",
                    "Utility", "Total Demand", "U4U Demand", "Initial", "Change %")
        logger.info("  %s  %s  %s  %s  %s",
                    "-" * 35, "-" * 14, "-" * 14, "-" * 14, "-" * 10)

        for util in sorted(self._all_utilities):
            new_val = total_demands.get(util, 0.0)
            old_val = prev_demands.get(util, 0.0)
            u4u_val = new_u4u.get(util, 0.0)
            initial = self._initial_demands.get(util, 0.0)

            if abs(new_val) < 0.01 and abs(old_val) < 0.01 and abs(u4u_val) < 0.01:
                continue

            if abs(new_val) > 1e-10:
                change_pct = abs(new_val - old_val) / abs(new_val) * 100
            elif abs(old_val) > 1e-10:
                change_pct = 100.0
            else:
                change_pct = 0.0

            pt_flag = " (PT)" if util in self._pass_through_utilities else ""
            logger.info("  %-35s  %14.2f  %14.2f  %14.2f  %9.4f%%%s",
                        util[:35], new_val, u4u_val, initial, change_pct, pt_flag)

        # U4U consumption breakdown
        if detail_records:
            logger.info("")
            logger.info("  U4U CONSUMPTION BREAKDOWN (per producer per material):")
            logger.info("  %-30s  %-30s  %10s  %14s  %s",
                        "Producer", "Material Consumed", "Norm", "Quantity", "Credit")
            logger.info("  %s  %s  %s  %s  %s",
                        "-" * 30, "-" * 30, "-" * 10, "-" * 14, "-" * 6)

            for rec in sorted(detail_records,
                              key=lambda r: (r["producer_utility"], r["material"])):
                credit = "YES" if rec.get("is_credit") else ""
                logger.info("  %-30s  %-30s  %10.6f  %14.2f  %s",
                            rec["producer_utility"][:30],
                            rec["material"][:30],
                            rec["norm"], rec["quantity"], credit)

        # Convergence status per material
        logger.info("")
        not_converged = []
        for material in sorted(set(new_u4u.keys()) | set(old_u4u.keys())):
            new_v = abs(new_u4u.get(material, 0.0))
            old_v = abs(old_u4u.get(material, 0.0))

            if new_v < 1e-10 and old_v < 1e-10:
                continue
            if new_v < 1e-10:
                not_converged.append((material, 100.0))
                continue

            change = abs(new_v - old_v) / new_v
            if change > self.convergence_tolerance:
                not_converged.append((material, change * 100))

        if not_converged:
            logger.info("  Not yet converged: %d materials", len(not_converged))
            for m, chg in not_converged:
                logger.info("    %-30s  change = %.4f%%", m, chg)
        else:
            logger.info("  All U4U materials converged within tolerance")

        logger.info("  %s", sep)

    def _log_final_summary(self):
        """Log the final balance summary in table format."""
        sep = "=" * 90
        logger.info("")
        logger.info("  %s", sep)
        logger.info("  FINAL BALANCE  (after %d iterations, %s)",
                    self.iterations_used,
                    "CONVERGED" if self.converged else "MAX ITERATIONS")
        logger.info("  %s", sep)

        # Power balance
        pr = self.final_power_result or {}
        sr = self.final_steam_result or {}
        demands = self.final_total_demands
        u4u = self.final_u4u_demands

        power_initial = self._initial_demands.get("Power_Dis", 0.0)
        power_u4u = u4u.get("Power_Dis", 0.0)
        power_total = demands.get("Power_Dis", 0.0)
        power_gen = pr.get("total_generation_mwh", 0.0)
        import_mwh = pr.get("import_power", {}).get("total_mwh", 0.0) if isinstance(pr.get("import_power"), dict) else 0.0
        total_supply = power_gen + import_mwh

        logger.info("")
        logger.info("  POWER BALANCE")
        logger.info("  %-35s  %14.2f KWH", "Process + Fixed Demand", power_initial)
        logger.info("  %-35s  %14.2f KWH", "U4U Demand", power_u4u)
        logger.info("  %-35s  %14.2f KWH", "TOTAL DEMAND", power_total)
        logger.info("  %-35s  %14.2f MWh", "GT/STG Generation", power_gen)
        logger.info("  %-35s  %14.2f MWh", "Import Power", import_mwh)
        logger.info("  %-35s  %14.2f MWh", "TOTAL SUPPLY", total_supply)
        logger.info("  %-35s  %14.2f MWh", "BALANCE",
                    total_supply - power_total / 1000.0)

        # Steam balance
        steam_detail = sr.get("demand_detail", {})
        cascade_grades = steam_detail.get("_cascade_grades", _STEAM_GRADES)
        logger.info("")
        logger.info("  STEAM BALANCE")
        logger.info("  %-12s  %14s  %14s  %14s  %14s",
                    "Grade", "Demand (MT)", "Gen (MT)", "Surplus (MT)", "Deficit (MT)")
        logger.info("  %s  %s  %s  %s  %s",
                    "-" * 12, "-" * 14, "-" * 14, "-" * 14, "-" * 14)
        for g in cascade_grades:
            net = steam_detail.get(f"{g}_net", 0.0)
            gen = sr.get("total_generation_mt", 0.0) if g == "shp" else 0.0
            surplus = max(0.0, gen - net) if g == "shp" else 0.0
            deficit = max(0.0, net - gen) if g == "shp" else 0.0
            logger.info("  %-12s  %14.2f  %14.2f  %14.2f  %14.2f",
                        g.upper(), net, gen, surplus, deficit)

        # U4U consumption summary table
        logger.info("")
        logger.info("  U4U CONSUMPTION SUMMARY")
        logger.info("  %-35s  %14s  %14s  %14s",
                    "Utility", "Initial Demand", "U4U Demand", "Total Demand")
        logger.info("  %s  %s  %s  %s",
                    "-" * 35, "-" * 14, "-" * 14, "-" * 14)
        for util in sorted(self._all_utilities):
            initial = self._initial_demands.get(util, 0.0)
            u4u_val = u4u.get(util, 0.0)
            total = demands.get(util, 0.0)
            if abs(total) < 0.01 and abs(u4u_val) < 0.01:
                continue
            pt = " (PT)" if util in self._pass_through_utilities else ""
            logger.info("  %-35s  %14.2f  %14.2f  %14.2f%s",
                        util[:35], initial, u4u_val, total, pt)

        # Pass-through utilities
        if self._pass_through_utilities:
            logger.info("")
            logger.info("  PASS-THROUGH UTILITIES (demand but no norms configuration):")
            for util in sorted(self._pass_through_utilities):
                demand = demands.get(util, 0.0)
                logger.info("    %-35s  demand = %.2f  (assumed external supply)",
                            util, demand)

        logger.info("  %s", sep)


# ---------------------------------------------------------------------------
# Convenience function
# ---------------------------------------------------------------------------

def run_u4u_iteration(
    plant_id: str,
    month: int,
    year: int,
    norms_reader: NMDNormsReader = None,
) -> dict:
    """
    Run the U4U iteration loop for NMD plant/month.

    Args:
        plant_id:     NMD plant UUID
        month:        1-12
        year:         calendar year
        norms_reader: pre-loaded NMDNormsReader

    Returns:
        {
            "converged":          bool,
            "iterations_used":    int,
            "power_result":       dict,
            "steam_result":       dict,
            "u4u_demands":        dict,
            "total_demands":      dict,
            "detail_records":     list,
            "iteration_history":  list,
            "pass_through_utilities": list,
        }
    """
    loop = NMDU4UIterationLoop(
        plant_id=plant_id,
        month=month,
        year=year,
        norms_reader=norms_reader,
    )
    return loop.run()
