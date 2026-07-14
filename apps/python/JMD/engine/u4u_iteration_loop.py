"""
U4U Iteration Loop Module
=========================
Iteratively balances utility demands by accounting for Utility-for-Utility (U4U)
consumption cascades.

The loop:
1. Dispatch power and steam assets to meet current demands
   (respecting priority, availability, spinning margin, capacity constraints)
2. Calculate U4U consumption from dispatch results (per-asset ODS norms)
3. For non-dispatchable utilities: generation = demand, calculate their U4U
4. Update demands with U4U consumption
5. Repeat until convergence (0.01% tolerance on demand change for ALL utilities)

All norms are sourced dynamically from the ODS file via ODSNormsReader.
No hardcoded norm values.
"""

import logging
from typing import TYPE_CHECKING, Dict, Optional, Set

if TYPE_CHECKING:
    from engine.ods_norms_reader import ODSNormsReader

from engine.norms_reader_factory import get_norms_reader
from engine.dispatch_engine import dispatch_power, dispatch_steam
from database.queries import fetch_process_demands_raw, fetch_fixed_consumption_raw

logger = logging.getLogger(__name__)

CONVERGENCE_TOLERANCE = 0.0001  # 0.01% as a fraction
MAX_ITERATIONS = 50

DEFAULT_ALLOWED_ACCOUNTS: Set[str] = {"Utilities", "Raw Material"}

_EXCLUDED_NON_DISPATCHABLE = {"Power_Dis"}

# Constants for reverse MMBTU norm calculation (same as NMD)
_KCAL_TO_BTU = 3.96567
_BTU_TO_MMBTU = 1_000_000
_FREE_STEAM_ENERGY_KCAL_KG = 760.87  # (810 - 110) / 0.92
_BTU_LB_TO_MMBTU_MT = 0.00396567  # HRSG: BTU/lb → MMBTU/MT


def _interpolate_hrsg_heat_rate(hrsg_name: str, steam_flow_tph: float, lookup_df) -> float:
    """Interpolate HRSG heat rate (BTU/lb) from CPP_HRSGHeatRate lookup by steam flow (TPH).

    Returns 0.0 if no data is found or steam_flow_tph <= 0.
    """
    if lookup_df is None or lookup_df.empty or steam_flow_tph <= 0:
        return 0.0

    normalized_name = _normalize_for_match(hrsg_name)
    lookup_norm = lookup_df["HRSGName"].apply(_normalize_for_match)
    hrsg_df = lookup_df[lookup_norm == normalized_name]

    if hrsg_df.empty:
        for idx, row in lookup_df.iterrows():
            db_norm = _normalize_for_match(str(row["HRSGName"]))
            if normalized_name in db_norm or db_norm in normalized_name:
                hrsg_df = lookup_df[lookup_df["HRSGName"] == row["HRSGName"]]
                break

    if hrsg_df.empty:
        return 0.0

    hrsg_df = hrsg_df.sort_values("LoadTPH").reset_index(drop=True)
    loads = hrsg_df["LoadTPH"].astype(float).values
    heat_rates = hrsg_df["HeatRateBTUlb"].astype(float).values

    if steam_flow_tph <= loads[0]:
        return float(heat_rates[0])
    if steam_flow_tph >= loads[-1]:
        return float(heat_rates[-1])

    for i in range(len(loads) - 1):
        if loads[i] <= steam_flow_tph <= loads[i + 1]:
            if loads[i] == loads[i + 1]:
                return float(heat_rates[i])
            frac = (steam_flow_tph - loads[i]) / (loads[i + 1] - loads[i])
            return float(heat_rates[i] + frac * (heat_rates[i + 1] - heat_rates[i]))

    return float(heat_rates[-1])


def _normalize_for_match(name: str) -> str:
    """Case-fold and strip non-alphanumeric chars for fuzzy name matching."""
    return "".join(ch.lower() for ch in name if ch.isalnum())


def _build_db_to_ods_mapping(ods_material_names: set, db_utility_names: set) -> Dict[str, str]:
    """
    Build a mapping from DB utility names to ODS material names dynamically.

    Strategy (in priority order):
      1. Exact match (same string)
      2. Case-insensitive match
      3. Normalized alphanumeric match (strips spaces, underscores, casing)

    Any DB utility name that cannot be matched to an ODS material is returned
    with itself as the key — it will appear as an UNMAPPED utility in logs.

    This eliminates all hardcoded name translation dicts.  When a new utility
    is added to the DB it is automatically matched to the ODS if the name is
    similar enough, or logged as unmapped if genuinely new.
    """
    ods_lower = {m.lower(): m for m in ods_material_names}
    ods_norm  = {_normalize_for_match(m): m for m in ods_material_names}

    mapping: Dict[str, str] = {}
    for db_name in db_utility_names:
        if db_name in ods_material_names:
            mapping[db_name] = db_name
        elif db_name.lower() in ods_lower:
            mapping[db_name] = ods_lower[db_name.lower()]
        elif _normalize_for_match(db_name) in ods_norm:
            mapping[db_name] = ods_norm[_normalize_for_match(db_name)]
        else:
            mapping[db_name] = db_name  # pass-through; logged as unmapped later
    return mapping

_MONTH_NAMES = {
    1: "January", 2: "February", 3: "March", 4: "April",
    5: "May", 6: "June", 7: "July", 8: "August",
    9: "September", 10: "October", 11: "November", 12: "December",
}


def _month_name(month: int) -> str:
    return _MONTH_NAMES.get(month, str(month))


class U4UIterationLoop:
    """
    Iterative U4U balancing loop.

    Each iteration:
    1. Dispatch power assets (priority, spinning margin, capacity enforced)
    2. Dispatch steam assets (priority, spinning margin, capacity enforced)
    3. Calculate U4U consumption from all generation (per-asset ODS norms)
    4. Update demands with U4U consumption
    5. Check convergence (0.01% tolerance on demand change for ALL utilities)
    """

    def __init__(
        self,
        plant_id: str,
        month: int,
        year: int,
        initial_demands: dict,
        ods_reader: Optional["ODSNormsReader"] = None,
        allowed_accounts: Optional[Set[str]] = None,
        convergence_tolerance: float = CONVERGENCE_TOLERANCE,
        max_iterations: int = MAX_ITERATIONS,
        external_import_mwh: float = 0.0,
        gt_heat_rate_df = None,
        hrsg_heat_rate_df = None,
    ):
        self.plant_id = plant_id
        self.month = month
        self.year = year
        self.initial_demands = initial_demands
        self.gt_heat_rate_df = gt_heat_rate_df
        self.hrsg_heat_rate_df = hrsg_heat_rate_df
        # Use factory to get norms reader (ODS or DB based on feature flag)
        self.ods_reader = ods_reader or get_norms_reader(plant_id, month, year)
        self.allowed_accounts = allowed_accounts or DEFAULT_ALLOWED_ACCOUNTS
        self.convergence_tolerance = convergence_tolerance
        self.max_iterations = max_iterations
        self.external_import_mwh = max(0.0, float(external_import_mwh))

        self.consumption_norms: dict = {}
        self.all_consumption_norms: dict = {}
        self._bpc_gen_quantities: dict = {}
        self._bpc_quantities: dict = {}
        self._all_producers: set = set()
        self._initial_power_mwh: float = 0.0
        self._initial_steam_mt: Dict[str, float] = {}
        self._raw_process: dict = {}
        self._raw_fixed: dict = {}
        self._power_ods_material: str = ""

        self.iteration_history: list = []
        self.final_power_result: Optional[dict] = None
        self.final_steam_result: Optional[dict] = None
        self.final_u4u_demands: dict = {}
        self.final_total_demands: dict = {}
        self.final_detail_records: list = []
        self.converged: bool = False
        self.iterations_used: int = 0

    def run(self) -> dict:
        """Run the U4U iteration loop until convergence or max iterations."""
        self.consumption_norms = self.ods_reader.get_consumption_norms()
        self.all_consumption_norms = self.ods_reader.get_all_consumption_norms()
        if not self.consumption_norms:
            logger.warning("  [U4U LOOP] No ODS consumption norms available")
            return self._empty_result()

        self._bpc_gen_quantities = self.ods_reader.get_bpc_generation_quantities()
        self._bpc_quantities = self.ods_reader.get_bpc_quantities()

        self._all_producers = set(self.consumption_norms.keys())
        initial_utility_demands = self._build_initial_demands()  # sets _raw_process/_raw_fixed
        self._precompute_initial_values()                         # reads _raw_process/_raw_fixed

        logger.info("")
        logger.info("  %s", "=" * 78)
        logger.info("  U4U ITERATION LOOP  (%s %d)", _month_name(self.month), self.year)
        logger.info("  %s", "=" * 78)
        logger.info("  Tolerance: %.4f%%  |  Max iterations: %d",
                     self.convergence_tolerance * 100, self.max_iterations)
        logger.info("  Allowed accounts: %s", ", ".join(sorted(self.allowed_accounts)))
        logger.info("  ODS producers: %d", len(self._all_producers))
        logger.info("")

        u4u_demands = {m: 0.0 for m in self._all_producers}
        prev_total_demands = {m: 0.0 for m in self._all_producers}

        for iteration in range(1, self.max_iterations + 1):
            total_demands = {
                m: initial_utility_demands.get(m, 0.0) + u4u_demands.get(m, 0.0)
                for m in self._all_producers
            }

            dispatch_demands = self._build_dispatch_demands(total_demands)

            power_result = dispatch_power(
                self.plant_id, self.month, self.year,
                demands=dispatch_demands, ods_reader=self.ods_reader,
                gt_heat_rate_df=self.gt_heat_rate_df,
            )

            steam_result = dispatch_steam(
                self.plant_id, self.month, self.year,
                power_result=power_result,
                demands=dispatch_demands, ods_reader=self.ods_reader,
            )

            new_u4u_demands, detail_records = self._calculate_all_u4u(
                power_result, steam_result, total_demands
            )

            self._log_iteration(
                iteration, total_demands, prev_total_demands,
                new_u4u_demands, u4u_demands, detail_records,
                power_result, steam_result,
            )

            converged = self._check_convergence(new_u4u_demands, u4u_demands)
            u4u_demands = new_u4u_demands
            prev_total_demands = dict(total_demands)

            self.iteration_history.append({
                "iteration": iteration,
                "total_demands": dict(total_demands),
                "u4u_demands": dict(u4u_demands),
                "power_generation_mwh": power_result.get("total_generation_mwh", 0.0),
                "steam_generation_mt": steam_result.get("total_generation_mt", 0.0),
                "converged": converged,
            })

            self.iterations_used = iteration
            self.final_power_result = power_result
            self.final_steam_result = steam_result
            self.final_u4u_demands = dict(u4u_demands)
            self.final_total_demands = dict(total_demands)
            self.final_detail_records = detail_records

            if converged:
                self.converged = True
                logger.info("")
                logger.info("  [U4U LOOP] ✓ CONVERGED in %d iterations", iteration)
                break

        if not self.converged:
            logger.warning("")
            logger.warning("  [U4U LOOP] ⚠ Did NOT converge after %d iterations", self.max_iterations)

        self._log_final_summary()

        return {
            "converged": self.converged,
            "iterations_used": self.iterations_used,
            "final_power_result": self.final_power_result,
            "final_steam_result": self.final_steam_result,
            "final_u4u_demands": self.final_u4u_demands,
            "final_total_demands": self.final_total_demands,
            "final_detail_records": self.final_detail_records,
            "final_dynamic_table": getattr(self, 'final_dynamic_table', []),
            "final_bpc_gen_quantities": self._bpc_gen_quantities,
            "final_bpc_quantities": self._bpc_quantities,
            "iteration_history": self.iteration_history,
        }

    # ------------------------------------------------------------------
    # Demand construction
    # ------------------------------------------------------------------

    def _precompute_initial_values(self):
        """Cache initial power and steam demand totals for dispatch construction.

        Uses the raw DB demands (self._raw_process / self._raw_fixed) and the
        dynamic ODS mapping built in _build_initial_demands() so that no utility
        names are hardcoded here.
        """
        # Power: find the ODS material whose normalized name matches 'powerdis'
        power_ods = None
        for mat in self._all_producers:
            if _normalize_for_match(mat) in ("powerdis", "power"):
                power_ods = mat
                break
        self._power_ods_material = power_ods  # e.g. "Power_Dis"

        raw_power_process = float(self._raw_process.get(power_ods, 0.0)) if power_ods else 0.0
        raw_power_fixed   = float(self._raw_fixed.get(power_ods, 0.0))   if power_ods else 0.0
        self._initial_power_mwh = raw_power_process + raw_power_fixed

        # Steam: find all steam _Dis ODS materials and sum process+fixed
        self._initial_steam_mt = {}   # {ods_material_name: initial_mt}
        for mat in self._all_producers:
            if mat.endswith("_Dis") and "Steam" in mat:
                proc  = float(self._raw_process.get(mat, 0.0))
                fixed = float(self._raw_fixed.get(mat, 0.0))
                self._initial_steam_mt[mat] = proc + fixed

    def _build_initial_demands(self) -> dict:
        """Build initial demand map keyed by ODS material name.

        Fetches raw process and fixed demands directly from the DB (no
        predefined mapping dict), then matches each DB utility name to an ODS
        material name using _build_db_to_ods_mapping().  New utilities added
        to the DB are automatically picked up — no code change required.
        """
        # Fetch raw demands (DB utility name → value)
        self._raw_process = fetch_process_demands_raw(self.plant_id, self.month, self.year)
        self._raw_fixed   = fetch_fixed_consumption_raw(self.plant_id, self.month, self.year)

        # Process demands for power are stored in KWH in the DB; convert to MWh.
        # Detect power utility by normalized name (handles Power, Power_Dis, etc.)
        for db_name in list(self._raw_process):
            if _normalize_for_match(db_name) in ("powerdis", "power"):
                self._raw_process[db_name] = self._raw_process[db_name] / 1000.0

        all_db_names = set(self._raw_process) | set(self._raw_fixed)
        db_to_ods = _build_db_to_ods_mapping(self._all_producers, all_db_names)

        # Log mapping so any unmatched utilities are visible
        unmapped = [db for db, ods in db_to_ods.items() if ods not in self._all_producers]
        if unmapped:
            logger.warning(
                "  [U4U LOOP] %d DB utilities not matched to any ODS producer: %s",
                len(unmapped), unmapped,
            )

        demands: dict = {}
        for db_name, value in self._raw_process.items():
            ods_mat = db_to_ods.get(db_name, db_name)
            demands[ods_mat] = demands.get(ods_mat, 0.0) + float(value)
        for db_name, value in self._raw_fixed.items():
            ods_mat = db_to_ods.get(db_name, db_name)
            demands[ods_mat] = demands.get(ods_mat, 0.0) + float(value)

        for producer in self._all_producers:
            if producer not in demands:
                demands[producer] = 0.0

        return demands

    def _build_dispatch_demands(self, total_demands: dict) -> dict:
        """Construct demands dict in the format expected by dispatch functions.

        Builds the merged demands dict that dispatch_power / dispatch_steam
        consume.  All keys are derived from the raw DB fetch and ODS material
        names — no hardcoded utility names.
        """
        # Start from raw process + fixed values (already in MWh / MT)
        dispatch_demands: dict = {}
        for db_name, val in self._raw_process.items():
            dispatch_demands[db_name] = dispatch_demands.get(db_name, 0.0) + float(val)
        for db_name, val in self._raw_fixed.items():
            dispatch_demands[db_name] = dispatch_demands.get(db_name, 0.0) + float(val)

        # Add U4U increment for power and store explicit split for dispatch logging
        if self._power_ods_material:
            power_u4u_increment = (
                total_demands.get(self._power_ods_material, 0.0)
                - self._initial_power_mwh
            )
            key = self._power_ods_material
            dispatch_demands[key] = dispatch_demands.get(key, 0.0) + power_u4u_increment

            # Store split: process stays as-is; fixed absorbs U4U so dispatch sees full demand.
            # Subtract external import so GTs only generate what isn't already covered externally.
            dispatch_demands["_power_process_mwh"] = float(
                self._raw_process.get(self._power_ods_material, 0.0)
            )
            dispatch_demands["_power_fixed_mwh"] = (
                float(self._raw_fixed.get(self._power_ods_material, 0.0))
                + power_u4u_increment
                - self.external_import_mwh
            )

        # Add U4U increment for each steam grade
        for ods_material, initial_mt in self._initial_steam_mt.items():
            steam_u4u_increment = (
                total_demands.get(ods_material, 0.0) - initial_mt
            )
            dispatch_demands[ods_material] = (
                dispatch_demands.get(ods_material, 0.0) + steam_u4u_increment
            )

        return dispatch_demands

    # ------------------------------------------------------------------
    # U4U calculation
    # ------------------------------------------------------------------

    def _calculate_all_u4u(
        self, power_result: dict, steam_result: dict, total_demands: dict
    ) -> tuple:
        """Calculate U4U consumption from all generation sources.

        Returns (u4u_dict, detail_records) where detail_records is a list of
        dicts with keys: producer, producer_utility, producer_uom, account,
        material, material_uom, norm, quantity, generation.
        """
        u4u: dict = {}
        details: list = []

        sub_u4u, sub_details = self._calculate_u4u_from_power(power_result)
        for material, amount in sub_u4u.items():
            u4u[material] = u4u.get(material, 0.0) + amount
        details.extend(sub_details)

        sub_u4u, sub_details = self._calculate_u4u_from_steam(steam_result)
        for material, amount in sub_u4u.items():
            u4u[material] = u4u.get(material, 0.0) + amount
        details.extend(sub_details)

        sub_u4u, sub_details = self._calculate_u4u_from_non_dispatchable(total_demands)
        for material, amount in sub_u4u.items():
            u4u[material] = u4u.get(material, 0.0) + amount
        details.extend(sub_details)

        sub_u4u, sub_details = self._calculate_steam_cascade_u4u(steam_result, total_demands)
        for material, amount in sub_u4u.items():
            u4u[material] = u4u.get(material, 0.0) + amount
        details.extend(sub_details)

        return u4u, details

    def _calculate_u4u_from_power(self, power_result: dict) -> tuple:
        """Calculate U4U from power dispatch using per-asset ODS norms.

        Returns (u4u_dict, detail_records).
        """
        u4u: dict = {}
        details: list = []
        # Dynamically find the power producer: the one whose production UOM is KWH
        power_producer_name = None
        powergen = None
        for pname, pinfo in self.consumption_norms.items():
            if pinfo.get("producer_uom", "").upper() == "KWH":
                power_producer_name = pname
                powergen = pinfo
                break
        if not powergen:
            return u4u, details

        consumptions = powergen.get("consumptions", [])
        producer_uom = powergen.get("producer_uom", "KWH")

        for asset in power_result.get("assets", []):
            asset_name = asset.get("asset_name", "")
            dispatched_mwh = asset.get("dispatched_mwh", 0.0)
            if dispatched_mwh <= 0:
                continue

            gen_kwh = dispatched_mwh * 1000  # MWh → KWH (ODS producer UOM)

            for c in consumptions:
                if c.get("source_plant", "") != asset_name:
                    continue
                if c["account"] not in self.allowed_accounts:
                    continue

                norm = c["norm"]
                if norm == 0:
                    continue

                material = c["material"]
                material_uom = c.get("material_uom", "")
                account = c["account"]

                quantity = gen_kwh * norm
                u4u_amount = quantity
                if material == "Power_Dis":
                    u4u_amount = u4u_amount / 1000.0  # KWH → MWh

                if material in self._all_producers:
                    u4u[material] = u4u.get(material, 0.0) + u4u_amount

                details.append({
                    "producer": asset_name,
                    "producer_utility": power_producer_name,
                    "producer_uom": producer_uom,
                    "generation": gen_kwh,
                    "account": account,
                    "material": material,
                    "material_uom": material_uom,
                    "norm": norm,
                    "quantity": quantity,
                    "norms_header_id": c.get("norms_header_id"),
                    "norms_month_detail_id": c.get("norms_month_detail_id"),
                })

        return u4u, details

    def _calculate_u4u_from_steam(self, steam_result: dict) -> tuple:
        """Calculate U4U from steam dispatch using per-asset ODS norms.

        Returns (u4u_dict, detail_records).
        """
        u4u: dict = {}
        details: list = []

        for asset in steam_result.get("assets", []):
            asset_name = asset.get("asset_name", "")
            dispatched_mt = asset.get("dispatched_mt", 0.0)
            free_steam_mt = asset.get("free_steam_mt", 0.0)
            # Use total output (free steam + supplementary) as generation basis so
            # that BFW, fuel and power norms are applied to the full HRSG output.
            # For AuxBoilers free_steam_mt=0 so this equals dispatched_mt.
            total_output_mt = asset.get("total_output_mt", dispatched_mt + free_steam_mt)
            if total_output_mt <= 0:
                continue

            producer_norms = self._find_steam_producer_norms(asset_name)
            if not producer_norms:
                logger.warning("  [U4U LOOP] No ODS norms for steam asset: %s", asset_name)
                continue

            producer_uom = producer_norms.get("producer_uom", "MT")

            # Reverse-calculate HRSG heat rate for fuel norm override
            hrsg_hr_btu_lb = 0.0
            if asset.get("asset_type", "").upper() == "HRSG" and self.hrsg_heat_rate_df is not None:
                op_hours = asset.get("op_hours", 0)
                steam_flow_tph = total_output_mt / op_hours if op_hours > 0 else 0.0
                hrsg_hr_btu_lb = _interpolate_hrsg_heat_rate(asset_name, steam_flow_tph, self.hrsg_heat_rate_df)

            for c in producer_norms.get("consumptions", []):
                if c["account"] not in self.allowed_accounts:
                    continue

                norm = c["norm"]
                if norm == 0:
                    continue

                material = c["material"]
                material_uom = c.get("material_uom", "")
                account = c["account"]

                # Reverse-calculate MMBTU norm for Raw Material (fuel) on HRSG
                # assets using heat rate from CPP_HRSGHeatRate table
                if account == "Raw Material" and hrsg_hr_btu_lb > 0:
                    reverse_norm = hrsg_hr_btu_lb * _BTU_LB_TO_MMBTU_MT
                    if reverse_norm > 0:
                        norm = reverse_norm

                quantity = total_output_mt * norm
                u4u_amount = quantity

                # Negative norms are byproduct credits (e.g. LP steam from HRSG).
                # Include them in detail_records for display in the U4U table but
                # do NOT add them to u4u[] — they are a supply, not a consumption.
                if norm < 0:
                    details.append({
                        "producer": asset_name,
                        "producer_utility": asset_name,
                        "producer_uom": producer_uom,
                        "generation": total_output_mt,
                        "account": account,
                        "material": material,
                        "material_uom": material_uom,
                        "norm": norm,
                        "quantity": quantity,
                        "norms_header_id": c.get("norms_header_id"),
                        "norms_month_detail_id": c.get("norms_month_detail_id"),
                    })
                    continue

                if material == "Power_Dis":
                    u4u_amount = u4u_amount / 1000.0  # KWH → MWh

                if material in self._all_producers:
                    u4u[material] = u4u.get(material, 0.0) + u4u_amount

                details.append({
                    "producer": asset_name,
                    "producer_utility": asset_name,
                    "producer_uom": producer_uom,
                    "generation": total_output_mt,
                    "account": account,
                    "material": material,
                    "material_uom": material_uom,
                    "norm": norm,
                    "quantity": quantity,
                    "norms_header_id": c.get("norms_header_id"),
                    "norms_month_detail_id": c.get("norms_month_detail_id"),
                })

        return u4u, details

    def _calculate_u4u_from_non_dispatchable(self, total_demands: dict) -> tuple:
        """Calculate U4U from non-dispatchable utilities (generation = demand).

        Returns (u4u_dict, detail_records).
        """
        u4u: dict = {}
        details: list = []

        for producer_name, producer_info in self.consumption_norms.items():
            if self._is_dispatchable(producer_name):
                continue
            if producer_name in _EXCLUDED_NON_DISPATCHABLE:
                continue
            # PRDS producers are handled by _calculate_steam_cascade_u4u
            if "PRDS" in producer_name.upper():
                continue

            generation = total_demands.get(producer_name, 0.0)
            if generation <= 0:
                continue

            producer_uom = producer_info.get("producer_uom", "")

            for c in producer_info.get("consumptions", []):
                if c["account"] not in self.allowed_accounts:
                    continue

                norm = c["norm"]
                if norm == 0:
                    continue

                material = c["material"]
                material_uom = c.get("material_uom", "")
                account = c["account"]

                quantity = generation * norm
                u4u_amount = quantity
                if material == "Power_Dis":
                    u4u_amount = u4u_amount / 1000.0  # KWH → MWh

                if material in self._all_producers:
                    u4u[material] = u4u.get(material, 0.0) + u4u_amount

                details.append({
                    "producer": producer_name,
                    "producer_utility": producer_name,
                    "producer_uom": producer_uom,
                    "generation": generation,
                    "account": account,
                    "material": material,
                    "material_uom": material_uom,
                    "norm": norm,
                    "quantity": quantity,
                    "norms_header_id": c.get("norms_header_id"),
                    "norms_month_detail_id": c.get("norms_month_detail_id"),
                })

        return u4u, details

    # ------------------------------------------------------------------
    # Helpers
    # ------------------------------------------------------------------

    def _calculate_steam_cascade_u4u(self, steam_result: dict, total_demands: dict) -> tuple:
        """Calculate steam grade U4U via bottom-up PRDS cascade.

        Walks LP → MP → HP → SHP, each step:
          1. Takes total demand for the lower grade from total_demands (which
             already contains process + fixed + all external U4U sources such
             as BFW → LP, PROCESS FEED WATER → LP, NITROGEN → MP, etc.).
          2. Subtracts HRSG LP byproduct supply (LP step only).
          3. Applies the PRDS norm to determine how much of the higher grade
             is consumed to satisfy the net demand.
          4. Adds that consumed quantity as U4U demand on the higher grade for
             the next iteration.

        Returns (u4u_dict, detail_records).
        """
        u4u: dict = {}
        details: list = []

        letdown_norms = self.ods_reader.get_steam_letdown_norms()
        cascade = letdown_norms.get("_cascade", [])
        if not cascade:
            return u4u, details

        byproduct_norms = self.ods_reader.get_hrsg_byproduct_norms()

        # LP byproduct from HRSGs based on this iteration's total HRSG output
        lp_byproduct_mt = 0.0
        for asset in steam_result.get("assets", []):
            if asset.get("asset_type", "") == "HRSG":
                total_out = asset.get("total_output_mt", 0.0)
                aname_upper = asset.get("asset_name", "").upper()
                norm_val = byproduct_norms.get(aname_upper)
                if norm_val is None:
                    norm_val = next(
                        (v for k, v in byproduct_norms.items()
                         if k in aname_upper or aname_upper in k),
                        0.0,
                    )
                # Byproduct norms are stored as negative credits; take abs value
                lp_byproduct_mt += total_out * abs(norm_val or 0.0)

        # Build ODS-name → total demand map so cascade steps for intermediate
        # grades (HP Steam_Dis) that are not in _all_producers can still be
        # looked up.  total_demands is keyed by ODS material names; for grades
        # not present (e.g. HP Steam_Dis not in ODS producers) start from 0.
        all_db_names = set(self._raw_process) | set(self._raw_fixed)
        db_to_ods = _build_db_to_ods_mapping(self._all_producers, all_db_names)
        ods_process: dict = {}
        ods_fixed: dict = {}
        for db_name, val in self._raw_process.items():
            ods_mat = db_to_ods.get(db_name, db_name)
            ods_process[ods_mat] = ods_process.get(ods_mat, 0.0) + float(val)
        for db_name, val in self._raw_fixed.items():
            ods_mat = db_to_ods.get(db_name, db_name)
            ods_fixed[ods_mat] = ods_fixed.get(ods_mat, 0.0) + float(val)

        # Walk the cascade bottom-up: cascade is ordered lowest→highest pressure
        # Each step: produces lower grade by consuming higher grade via PRDS
        # step = {prds, produces, consumes, norm}
        # norm: MT of `consumes` needed to produce 1 MT of `produces`
        #
        # gross_demand for each grade = total_demands (proc+fixed+all external
        # U4U) PLUS any cascade-internal uplift accumulated from lower grades.
        # This ensures BFW→LP and NITROGEN→MP contributions are included.
        cascade_uplift: dict = {}  # grade_dis -> MT added by this cascade only

        logger.debug("  [CASCADE] LP byproduct from HRSGs: %.2f MT", lp_byproduct_mt)
        for step in cascade:
            prds_name    = step["prds"]
            produces_dis = step["produces"]   # e.g. "LP Steam_Dis"
            consumes_dis = step["consumes"]   # e.g. "MP Steam_Dis"
            norm         = step["norm"]       # e.g. 0.945 MT MP per MT LP

            # For grades in total_demands (LP, MP, SHP): use that value directly.
            # It already contains proc+fixed+all external U4U (BFW→LP etc.) from
            # the previous iteration — do NOT add cascade_uplift on top or we
            # double-count the LP→MP contribution.
            # For intermediate grades absent from total_demands (HP Steam_Dis):
            # fall back to proc+fixed from DB plus cascade_uplift propagated
            # from the MP step of THIS cascade run.
            if produces_dis in total_demands:
                gross_demand = max(0.0, float(total_demands[produces_dis]))
            else:
                proc   = float(ods_process.get(produces_dis, 0.0))
                fixed  = float(ods_fixed.get(produces_dis, 0.0))
                uplift = cascade_uplift.get(produces_dis, 0.0)
                gross_demand = max(0.0, proc + fixed) + uplift

            # For LP grade: subtract HRSG byproduct supply
            if "LP" in produces_dis.upper():
                net_prds_demand = max(0.0, gross_demand - lp_byproduct_mt)
            else:
                net_prds_demand = max(0.0, gross_demand)

            logger.debug(
                "  [CASCADE] %-25s produces=%-20s gross=%10.2f "
                "byproduct=%10.2f net=%10.2f -> %-20s consumed=%10.2f",
                prds_name, produces_dis, gross_demand,
                lp_byproduct_mt if "LP" in produces_dis.upper() else 0.0,
                net_prds_demand, consumes_dis, net_prds_demand * norm,
            )

            # MT of higher grade consumed by PRDS to meet this net demand
            higher_grade_consumed = net_prds_demand * norm

            if higher_grade_consumed > 0:
                # consumes_dis may be an intermediate grade not in _all_producers
                # (e.g. HP Steam_Dis); still track it in cascade_uplift so the
                # next cascade step picks it up correctly.
                cascade_uplift[consumes_dis] = (
                    cascade_uplift.get(consumes_dis, 0.0) + higher_grade_consumed
                )
                if consumes_dis in self._all_producers:
                    u4u[consumes_dis] = u4u.get(consumes_dis, 0.0) + higher_grade_consumed

            details.append({
                "producer": prds_name,
                "producer_utility": prds_name,
                "producer_uom": "MT",
                "generation": net_prds_demand,
                "account": "Utilities",
                "material": consumes_dis,
                "material_uom": "MT",
                "norm": norm,
                "quantity": higher_grade_consumed,
                "norms_header_id": None,  # PRDS norms don't have individual IDs in current schema
                "norms_month_detail_id": None,
            })

        return u4u, details

    def _is_dispatchable(self, producer_name: str) -> bool:
        """Check if a producer is dispatchable (power or steam assets).

        A producer is dispatchable if it has raw material (fuel) consumption in
        the ODS — meaning it is a real generator (GT, HRSG, Aux Boiler) that is
        dispatched by dispatch_power / dispatch_steam rather than computed from
        its demand.  PRDS and other pass-through utilities have no raw material
        rows and are therefore non-dispatchable.
        """
        info = self.consumption_norms.get(producer_name)
        if info is None:
            return False
        return any(c["account"] == "Raw Material" for c in info.get("consumptions", []))

    def _lookup_bpc_gen_qty(self, producer_name: str) -> float:
        """Look up BPC generation quantity for a producer from ODS data."""
        if not self._bpc_gen_quantities:
            return 0.0
        if producer_name in self._bpc_gen_quantities:
            return self._bpc_gen_quantities[producer_name]
        for key, val in self._bpc_gen_quantities.items():
            if key.upper() == producer_name.upper():
                return val
        return 0.0

    def _lookup_bpc_qty(self, producer_name: str, material: str) -> float:
        """Look up BPC quantity for a producer+material from ODS data."""
        if not self._bpc_quantities:
            return 0.0
        bpc_map = None
        if producer_name in self._bpc_quantities:
            bpc_map = self._bpc_quantities[producer_name]
        else:
            for key, val in self._bpc_quantities.items():
                if key.upper() == producer_name.upper():
                    bpc_map = val
                    break
        if not bpc_map:
            return 0.0
        if material in bpc_map:
            return bpc_map[material]
        for k, v in bpc_map.items():
            if k.upper() == material.upper():
                return v
        return 0.0

    @staticmethod
    def _pct_diff(our_val: float, bpc_val: float) -> float:
        """Calculate percentage difference: (our - bpc) / bpc * 100."""
        if bpc_val == 0:
            return 0.0
        return (our_val - bpc_val) / bpc_val * 100.0

    def _find_steam_producer_norms(self, asset_name: str) -> Optional[dict]:
        """Find ODS consumption norms for a steam asset by name."""
        if asset_name in self.consumption_norms:
            return self.consumption_norms[asset_name]

        for producer, info in self.consumption_norms.items():
            if producer.upper() == asset_name.upper():
                return info

        for producer, info in self.consumption_norms.items():
            if producer.upper() in asset_name.upper() or asset_name.upper() in producer.upper():
                return info

        return None

    def _check_convergence(self, new_u4u: dict, old_u4u: dict) -> bool:
        """Check if all U4U demands have converged within tolerance."""
        all_materials = set(new_u4u.keys()) | set(old_u4u.keys())

        for material in all_materials:
            new_val = abs(new_u4u.get(material, 0.0))
            old_val = abs(old_u4u.get(material, 0.0))

            if new_val < 1e-10 and old_val < 1e-10:
                continue
            if new_val < 1e-10:
                return False

            change = abs(new_val - old_val) / new_val
            if change > self.convergence_tolerance:
                return False

        return True

    # ------------------------------------------------------------------
    # Logging
    # ------------------------------------------------------------------

    def _log_iteration(
        self, iteration: int, total_demands: dict, prev_total_demands: dict,
        new_u4u_demands: dict, old_u4u_demands: dict, detail_records: list,
        power_result: dict, steam_result: dict,
    ):
        """Log detailed iteration info: per-producer demands and U4U breakdown."""
        power_gen = power_result.get("total_generation_mwh", 0.0)
        steam_gen = steam_result.get("total_generation_mt", 0.0)
        power_demand = total_demands.get("Power_Dis", 0.0)

        logger.info("")
        logger.info("  [U4U LOOP] %s", "=" * 78)
        logger.info("  [U4U LOOP]  ITERATION %d", iteration)
        logger.info("  [U4U LOOP] %s", "=" * 78)
        logger.info("  [U4U LOOP]  Power:  demand=%14.2f MWh   gen=%14.2f MWh",
                     power_demand, power_gen)
        logger.info("  [U4U LOOP]  Steam:  gen=%14.2f MT", steam_gen)
        logger.info("")

        # Per-producer demand table
        logger.info("  [U4U LOOP]  %-30s  %12s  %12s  %12s  %10s",
                     "Utility (ODS Material)", "Old Demand", "New Demand", "U4U Demand", "Change %")
        logger.info("  [U4U LOOP]  %s  %s  %s  %s  %s",
                     "-" * 30, "-" * 12, "-" * 12, "-" * 12, "-" * 10)

        for material in sorted(self._all_producers):
            old_val = prev_total_demands.get(material, 0.0)
            new_val = total_demands.get(material, 0.0)
            u4u_val = new_u4u_demands.get(material, 0.0)

            if abs(new_val) < 0.01 and abs(old_val) < 0.01 and abs(u4u_val) < 0.01:
                continue

            if abs(new_val) > 1e-10:
                change_pct = abs(new_val - old_val) / abs(new_val) * 100
            elif abs(old_val) > 1e-10:
                change_pct = 100.0
            else:
                change_pct = 0.0

            logger.info("  [U4U LOOP]  %-30s  %12.2f  %12.2f  %12.2f  %9.4f%%",
                         material, old_val, new_val, u4u_val, change_pct)

        logger.info("")

        # Per-producer U4U consumption breakdown
        logger.info("  [U4U LOOP]  U4U CONSUMPTION BREAKDOWN (per producer per material):")
        logger.info("  [U4U LOOP]  %-25s  %-25s  %8s  %12s",
                     "Producer", "Material Consumed", "Norm", "Quantity")
        logger.info("  [U4U LOOP]  %s  %s  %s  %s",
                     "-" * 25, "-" * 25, "-" * 8, "-" * 12)

        for rec in detail_records:
            logger.info("  [U4U LOOP]  %-25s  %-25s  %8.6f  %12.2f",
                         rec["producer"][:25], rec["material"][:25],
                         rec["norm"], rec["quantity"])

        logger.info("")

        # Convergence status per material
        not_converged = []
        for material in sorted(self._all_producers):
            new_u4u = abs(new_u4u_demands.get(material, 0.0))
            old_u4u = abs(old_u4u_demands.get(material, 0.0))

            if new_u4u < 1e-10 and old_u4u < 1e-10:
                continue
            if new_u4u < 1e-10:
                not_converged.append(material)
                continue

            change = abs(new_u4u - old_u4u) / new_u4u
            if change > self.convergence_tolerance:
                not_converged.append(material)

        if not_converged:
            logger.info("  [U4U LOOP]  Not yet converged: %d materials",
                         len(not_converged))
            for m in not_converged:
                new_u4u = abs(new_u4u_demands.get(m, 0.0))
                old_u4u = abs(old_u4u_demands.get(m, 0.0))
                if new_u4u > 1e-10:
                    chg = abs(new_u4u - old_u4u) / new_u4u * 100
                    logger.info("  [U4U LOOP]    %-30s  change=%.4f%%", m, chg)
        else:
            logger.info("  [U4U LOOP]  All materials converged within tolerance")

        logger.info("")

    def _log_excess_availability(self):
        """Log excess power and steam available for export after meeting demands."""
        logger.info("  %s", "=" * 78)
        logger.info("  EXCESS POWER & STEAM AVAILABILITY (for export)")
        logger.info("  %s", "=" * 78)

        # --- Power ---
        pr = self.final_power_result or {}
        p_demand = pr.get("demand_mwh", 0.0)
        p_gen = pr.get("total_generation_mwh", 0.0)
        p_surplus = pr.get("surplus_mwh", 0.0)
        p_deficit = pr.get("deficit_mwh", 0.0)
        p_import = pr.get("total_aux_power_mwh", 0.0)

        logger.info("")
        logger.info("  POWER BALANCE")
        logger.info("  %-30s  %14.2f  MWh", "Total Demand (incl U4U)", p_demand)
        logger.info("  %-30s  %14.2f  MWh", "GT/STG Generation", p_gen)
        if self.external_import_mwh > 0:
            logger.info("  %-30s  %14.2f  MWh", "External Import Power", self.external_import_mwh)
            logger.info("  %-30s  %14.2f  MWh", "Total Supply (Gen+Import)",
                        p_gen + self.external_import_mwh)
        logger.info("  %-30s  %14.2f  MWh", "Auxiliary Consumption", p_import)

        if p_surplus > 0.01:
            logger.info("")
            logger.info("  ╔════════════════════════════════════════════════════════════╗")
            logger.info("  ║  ★ EXCESS POWER AVAILABLE FOR EXPORT: %14.2f MWh      ║", p_surplus)
            logger.info("  ║    (%.2f MW for 720 hrs @ 100%% utilization factor)        ║", p_surplus / 720.0)
            logger.info("  ╚════════════════════════════════════════════════════════════╝")
        elif p_deficit > 0.01:
            logger.info("")
            logger.info("  ⚠ POWER DEFICIT: %.2f MWh (generation insufficient)", p_deficit)
        else:
            logger.info("  ✓ Power demand met exactly — no surplus or deficit")

        # Per-asset power breakdown for surplus context
        p_assets = pr.get("assets", [])
        if p_assets:
            logger.info("")
            logger.info("  Power Asset Dispatch Summary:")
            logger.info("  %-25s  %10s  %10s  %10s  %10s",
                         "Asset", "Gross MWh", "Aux MWh", "Net MWh", "Status")
            logger.info("  %s  %s  %s  %s  %s", "-" * 25, "-" * 10, "-" * 10, "-" * 10, "-" * 10)
            for a in p_assets:
                name = str(a.get("asset_name", ""))[:25]
                gross = a.get("dispatched_mwh", 0.0)
                aux = a.get("aux_power", 0.0)
                net = a.get("net_mwh", gross - aux)
                max_mwh = a.get("max_mwh", 0.0)
                if gross >= max_mwh - 0.01:
                    status = "AT MAX"
                elif gross <= a.get("min_mwh", 0.0) + 0.01:
                    status = "AT MIN"
                else:
                    status = "PARTIAL"
                logger.info("  %-25s  %10.2f  %10.2f  %10.2f  %10s", name, gross, aux, net, status)

            total_max = sum(a.get("max_mwh", 0.0) for a in p_assets)
            total_min = sum(a.get("min_mwh", 0.0) for a in p_assets)
            headroom = total_max - p_gen
            logger.info("")
            logger.info("  %-25s  %10.2f  MWh", "Total Plant Capacity (max)", total_max)
            logger.info("  %-25s  %10.2f  MWh", "Total Plant Min", total_min)
            logger.info("  %-25s  %10.2f  MWh", "Current Generation", p_gen)
            if headroom > 0.01:
                logger.info("  %-25s  %10.2f  MWh  ★ ADDITIONAL EXPORT POTENTIAL", "Headroom (max - gen)", headroom)

        # --- Steam ---
        sr = self.final_steam_result or {}
        s_demand = (sr.get("demand_detail") or {}).get("shp_net", 0.0)
        s_gen = sr.get("total_generation_mt", 0.0)
        s_free = sr.get("total_free_steam_mt", 0.0)
        s_supp = sr.get("total_supplementary_generation_mt", 0.0)
        s_surplus = sr.get("surplus_mt", 0.0)
        s_deficit = sr.get("deficit_mt", 0.0)

        logger.info("")
        logger.info("  STEAM BALANCE (SHP)")
        logger.info("  %-30s  %14.2f  MT", "Total SHP Demand (incl U4U)", s_demand)
        logger.info("  %-30s  %14.2f  MT", "Free Steam (from GT exhaust)", s_free)
        logger.info("  %-30s  %14.2f  MT", "Supplementary Firing", s_supp)
        logger.info("  %-30s  %14.2f  MT", "Total Steam Generation", s_gen)

        if s_surplus > 0.01:
            logger.info("")
            logger.info("  ╔════════════════════════════════════════════════════════════╗")
            logger.info("  ║  ★ EXCESS STEAM AVAILABLE FOR EXPORT: %14.2f MT       ║", s_surplus)
            logger.info("  ║    (%.2f TPH for 720 hrs @ 100%% utilization factor)        ║", s_surplus / 720.0)
            logger.info("  ╚════════════════════════════════════════════════════════════╝")
        elif s_deficit > 0.01:
            logger.info("")
            logger.info("  ⚠ STEAM DEFICIT: %.2f MT (generation insufficient)", s_deficit)
        else:
            logger.info("  ✓ Steam demand met exactly — no surplus or deficit")

        # Per-asset steam breakdown for surplus context
        s_assets = sr.get("assets", [])
        if s_assets:
            logger.info("")
            logger.info("  Steam Asset Dispatch Summary:")
            logger.info("  %-25s  %12s  %12s  %12s  %10s",
                         "Asset", "Dispatched MT", "Max MT", "Min MT", "Status")
            logger.info("  %s  %s  %s  %s  %s", "-" * 25, "-" * 12, "-" * 12, "-" * 12, "-" * 10)
            for a in s_assets:
                name = str(a.get("asset_name", ""))[:25]
                disp = a.get("dispatched_mt", 0.0)
                max_mt = a.get("max_mt", a.get("max_tph", 0.0))
                min_mt = a.get("min_mt", a.get("min_tph", 0.0))
                if max_mt > 0 and disp >= max_mt - 0.01:
                    status = "AT MAX"
                elif disp <= min_mt + 0.01:
                    status = "AT MIN"
                else:
                    status = "PARTIAL"
                logger.info("  %-25s  %12.2f  %12.2f  %12.2f  %10s", name, disp, max_mt, min_mt, status)

            s_total_max = sum(a.get("max_mt", a.get("max_tph", 0.0)) for a in s_assets)
            s_total_min = sum(a.get("min_mt", a.get("min_tph", 0.0)) for a in s_assets)
            s_headroom = s_total_max - s_supp
            logger.info("")
            logger.info("  %-25s  %12.2f  MT", "Total Steam Capacity (max)", s_total_max)
            logger.info("  %-25s  %12.2f  MT", "Total Steam Min", s_total_min)
            logger.info("  %-25s  %12.2f  MT", "Current Supp. Generation", s_supp)
            if s_headroom > 0.01:
                logger.info("  %-25s  %12.2f  MT  ★ ADDITIONAL EXPORT POTENTIAL", "Headroom (max - supp)", s_headroom)

        logger.info("")
        logger.info("  %s", "=" * 78)
        logger.info("")

    def _log_demand_generation_balance(self):
        """Log demand vs generation balance table for all utilities."""
        # Dynamically identify intermediate PRDS (letdown station) producers.
        # A PRDS producer is one whose consumptions consist only of:
        #   - a steam "_Dis" material (the higher-grade steam it letdowns from)
        #   - optionally Boiler Feed Water (desuperheating)
        # and has NO raw-material rows.
        # These are skipped because their "generation" is already captured inside
        # the steam _Dis net demand values from the dispatch engine.
        steam_distribution = self.ods_reader.get_steam_distribution()
        steam_dis_grades = set(steam_distribution.keys())  # e.g. {"SHP Steam_Dis", ...}

        _skip_rows: set = set()
        for producer, info in self.consumption_norms.items():
            consumptions = info.get("consumptions", [])
            has_raw_material = any(c["account"] == "Raw Material" for c in consumptions)
            if has_raw_material:
                continue
            util_materials = {
                c["material"] for c in consumptions if c["account"] == "Utilities"
            }
            # PRDS: only consumes steam _Dis grades and/or BFW
            non_prds_inputs = util_materials - steam_dis_grades - {"Boiler Feed Water"}
            if not non_prds_inputs and any(
                m in steam_dis_grades for m in util_materials
            ):
                _skip_rows.add(producer)

        # power _Dis material name — derived dynamically
        power_dis = self._power_ods_material or "Power_Dis"

        # Build process/fixed demand per ODS material from raw DB demands
        all_db_names = set(self._raw_process) | set(self._raw_fixed)
        db_to_ods = _build_db_to_ods_mapping(self._all_producers, all_db_names)
        process_demands: dict = {}
        fixed_demands: dict = {}
        for db_name, val in self._raw_process.items():
            ods_mat = db_to_ods.get(db_name, db_name)
            process_demands[ods_mat] = process_demands.get(ods_mat, 0.0) + float(val)
        for db_name, val in self._raw_fixed.items():
            ods_mat = db_to_ods.get(db_name, db_name)
            fixed_demands[ods_mat] = fixed_demands.get(ods_mat, 0.0) + float(val)

        # Build generation per ODS material
        generation = {}
        steam_net_demand = {}  # override total demand for steam grades with dispatch net
        pr = self.final_power_result or {}
        sr = self.final_steam_result or {}

        # Power generation: GT/STG dispatch + external import
        generation[power_dis] = pr.get("total_generation_mwh", 0.0) + self.external_import_mwh

        # Steam: use demand_detail net values so that PRDS letdown consumption
        # is included in Total Demand, and Balance correctly shows 0 (met) or
        # positive (real export surplus for the highest grade).
        # The dispatch engine stores net demand keyed by grade prefix (shp/hp/mp/lp).
        dd = sr.get("demand_detail") or {}
        lp_byproduct = dd.get("lp_byproduct", 0.0)
        lp_net = dd.get("lp_net", 0.0)
        lp_total_gross = abs(lp_byproduct) + max(0.0, lp_net)

        # Map each steam _Dis grade to its net demand and generation dynamically.
        # Grade prefix is derived from the _Dis name (e.g. "LP Steam_Dis" → "lp").
        for dis_grade in steam_dis_grades:
            prefix = dis_grade.replace(" Steam_Dis", "").replace("_Dis", "").lower()
            net_key = f"{prefix}_net"
            if prefix == "lp":
                gen_val = lp_total_gross
                demand_val = lp_total_gross
            elif prefix == "shp":
                gen_val = sr.get("total_generation_mt", 0.0)
                demand_val = max(0.0, dd.get(net_key, 0.0))
            else:
                net_val = max(0.0, dd.get(net_key, 0.0))
                gen_val = net_val
                demand_val = net_val
            generation[dis_grade] = gen_val
            steam_net_demand[dis_grade] = demand_val

        # Non-dispatchable: generation = total demand
        for material in self._all_producers:
            if material not in generation and material not in _skip_rows:
                generation[material] = self.final_total_demands.get(material, 0.0)

        # Collect all utilities (union of demand and generation keys), skip PRDS
        all_utilities = sorted(
            (set(process_demands.keys()) | set(fixed_demands.keys()) |
             set(self.final_u4u_demands.keys()) | set(self.final_total_demands.keys()) |
             set(generation.keys()))
            - _skip_rows
        )

        # Build letdown per steam _Dis grade from dispatch demand_detail
        dd = (self.final_steam_result or {}).get("demand_detail") or {}
        steam_letdown: dict = {}
        for dis_grade in steam_dis_grades:
            prefix = dis_grade.replace(" Steam_Dis", "").replace("_Dis", "").lower()
            steam_letdown[dis_grade] = dd.get(f"{prefix}_letdown", 0.0)

        logger.info("  %s", "=" * 78)
        logger.info("  DEMAND vs GENERATION BALANCE TABLE")
        logger.info("  %s", "=" * 78)
        logger.info("  %-25s  %12s  %12s  %12s  %12s  %14s  %14s  %14s",
                     "Utility", "Process", "Fixed", "U4U", "Letdown", "Total Demand", "Generation", "Balance")
        logger.info("  %s  %s  %s  %s  %s  %s  %s  %s",
                     "-" * 25, "-" * 12, "-" * 12, "-" * 12, "-" * 12, "-" * 14, "-" * 14, "-" * 14)

        for util in all_utilities:
            proc = process_demands.get(util, 0.0)
            fix = fixed_demands.get(util, 0.0)
            u4u = self.final_u4u_demands.get(util, 0.0)
            letdown = steam_letdown.get(util, 0.0)
            # For steam _Dis rows use the dispatch net (includes PRDS letdown)
            if util in steam_net_demand:
                total = steam_net_demand[util]
            else:
                total = self.final_total_demands.get(util, proc + fix + u4u)
            gen = generation.get(util, 0.0)
            balance = gen - total

            if abs(total) < 0.01 and abs(gen) < 0.01 and abs(u4u) < 0.01:
                continue

            logger.info("  %-25s  %12.2f  %12.2f  %12.2f  %12.2f  %14.2f  %14.2f  %14.2f",
                         util[:25], proc, fix, u4u, letdown, total, gen, balance)

        logger.info("  %s", "=" * 78)
        logger.info("")

    def _build_dynamic_u4u_table(self) -> list:
        """Build a dynamic U4U consumption table from all ODS producers.

        Unlike ``final_detail_records`` (which only contains producers that
        actually generated something), this table includes every producer utility
        discovered in the ODS — including those with zero generation.  For each
        producer it lists every consumption material from the ODS (Utilities,
        Raw Material, Cat Chem, byproduct credits, etc.) with the computed
        quantity = generation * norm.  New producers or materials added to the
        ODS are automatically picked up without code changes.

        Returns a list of detail records in the same shape as
        ``final_detail_records``.
        """
        records: list = []
        if not self.all_consumption_norms:
            return records

        # Identify the power producer (UOM = KWH) and its ODS sub-assets
        power_producer_name = None
        power_sub_assets: set = set()
        for pname, pinfo in self.all_consumption_norms.items():
            if pinfo.get("producer_uom", "").upper() == "KWH":
                power_producer_name = pname
                for c in pinfo.get("consumptions", []):
                    sp = c.get("source_plant", "").strip()
                    if sp:
                        power_sub_assets.add(sp)
                break

        # Map power/steam asset names to their final generation
        power_asset_gens: dict = {}
        power_asset_heat: dict = {}  # asset_name → (heat_rate, free_steam_factor)
        for asset in (self.final_power_result or {}).get("assets", []):
            name = asset.get("asset_name", "")
            power_asset_gens[name] = asset.get("dispatched_mwh", 0.0) * 1000.0
            hr = asset.get("heat_rate", 0.0)
            fsf = asset.get("free_steam_factor", 0.0)
            if hr > 0:
                power_asset_heat[name] = (hr, fsf)

        steam_asset_gens: dict = {}
        hrsg_asset_heat: dict = {}  # asset_name → heat_rate_btu_lb
        for asset in (self.final_steam_result or {}).get("assets", []):
            aname = asset.get("asset_name", "")
            total_mt = asset.get(
                "total_output_mt",
                asset.get("dispatched_mt", 0.0) + asset.get("free_steam_mt", 0.0),
            )
            steam_asset_gens[aname] = total_mt

            # Build HRSG heat rate lookup for reverse norm calculation
            if asset.get("asset_type", "").upper() == "HRSG" and self.hrsg_heat_rate_df is not None:
                op_hours = asset.get("op_hours", 0)
                steam_flow_tph = total_mt / op_hours if op_hours > 0 else 0.0
                hr_btu_lb = _interpolate_hrsg_heat_rate(aname, steam_flow_tph, self.hrsg_heat_rate_df)
                if hr_btu_lb > 0:
                    hrsg_asset_heat[aname] = hr_btu_lb

        # PRDS generation from the cascade details already calculated
        prds_generation: dict = {}
        for rec in self.final_detail_records:
            if "PRDS" in rec.get("producer", "").upper():
                prds_generation[rec["producer"]] = rec.get("generation", 0.0)

        def _match_steam_asset(producer_name: str) -> tuple:
            """Return (asset_name, generation) for a steam producer, or ('', 0)."""
            if not steam_asset_gens:
                return "", 0.0
            # Exact match
            if producer_name in steam_asset_gens:
                return producer_name, steam_asset_gens[producer_name]
            # Case-insensitive / substring match
            pname_upper = producer_name.upper()
            for aname, gen in steam_asset_gens.items():
                if aname.upper() == pname_upper or aname.upper() in pname_upper or pname_upper in aname.upper():
                    return aname, gen
            return "", 0.0

        for producer_name, producer_info in self.all_consumption_norms.items():
            producer_uom = producer_info.get("producer_uom", "")
            consumptions = producer_info.get("consumptions", [])
            if not consumptions:
                continue

            gen_entries: list = []

            if producer_name == power_producer_name and power_producer_name:
                # One row per ODS sub-asset (GT1, GT2, STG, ...)
                for asset_name in sorted(power_sub_assets):
                    gen_entries.append({
                        "producer": asset_name,
                        "producer_utility": power_producer_name,
                        "generation": power_asset_gens.get(asset_name, 0.0),
                    })
                if not power_sub_assets:
                    total_gen = sum(power_asset_gens.values())
                    gen_entries.append({
                        "producer": producer_name,
                        "producer_utility": producer_name,
                        "generation": total_gen,
                    })
            elif self._is_dispatchable(producer_name):
                # Steam asset producer (HRSG, Aux Boiler, ...)
                asset_name, gen = _match_steam_asset(producer_name)
                gen_entries.append({
                    "producer": asset_name if asset_name else producer_name,
                    "producer_utility": producer_name,
                    "generation": gen,
                })
            elif "PRDS" in producer_name.upper():
                # PRDS / letdown station
                gen_entries.append({
                    "producer": producer_name,
                    "producer_utility": producer_name,
                    "generation": prds_generation.get(producer_name, 0.0),
                })
            else:
                # Non-dispatchable utility plant: generation = total demand
                # Convert MWh → KWh when producer UOM is KWH (e.g. Power_Dis)
                raw_gen = self.final_total_demands.get(producer_name, 0.0)
                if producer_uom.upper() == "KWH":
                    raw_gen = raw_gen * 1000.0
                gen_entries.append({
                    "producer": producer_name,
                    "producer_utility": producer_name,
                    "generation": raw_gen,
                })

            # Fallback: should always have at least one generation entry
            if not gen_entries:
                gen_entries.append({
                    "producer": producer_name,
                    "producer_utility": producer_name,
                    "generation": 0.0,
                })

            for gen_entry in gen_entries:
                gen = gen_entry["generation"]
                for c in consumptions:
                    # For the power producer, only show consumption entries
                    # belonging to the current sub-asset
                    if producer_name == power_producer_name:
                        source_plant = c.get("source_plant", "").strip()
                        if source_plant and source_plant != gen_entry["producer"]:
                            continue

                    material = c["material"]
                    material_uom = c.get("material_uom", "")
                    norm = c["norm"]

                    # Reverse-calculate MMBTU norm for Raw Material (fuel) entries
                    # on GT power assets using heat_rate and free_steam_factor
                    if c["account"] == "Raw Material" and gen_entry["producer"] in power_asset_heat:
                        hr, fsf = power_asset_heat[gen_entry["producer"]]
                        reverse_norm = (
                            _KCAL_TO_BTU * (hr - fsf * _FREE_STEAM_ENERGY_KCAL_KG)
                            / _BTU_TO_MMBTU
                        )
                        if reverse_norm > 0:
                            norm = reverse_norm

                    # Reverse-calculate MMBTU norm for Raw Material (fuel) entries
                    # on HRSG steam assets using heat rate from CPP_HRSGHeatRate
                    elif c["account"] == "Raw Material" and gen_entry["producer"] in hrsg_asset_heat:
                        hr_btu_lb = hrsg_asset_heat[gen_entry["producer"]]
                        reverse_norm = hr_btu_lb * _BTU_LB_TO_MMBTU_MT
                        if reverse_norm > 0:
                            norm = reverse_norm

                    quantity = gen * norm

                    records.append({
                        "producer": gen_entry["producer"],
                        "producer_utility": gen_entry["producer_utility"],
                        "producer_uom": producer_uom,
                        "generation": gen,
                        "account": c["account"],
                        "material": material,
                        "material_uom": material_uom,
                        "norm": norm,
                        "quantity": quantity,
                        "bpc_quantity": c.get("quantity", 0.0),
                    })

        return records

    def _log_final_summary(self):
        """Log final summary with demand table and detailed U4U consumption table."""
        logger.info("")
        logger.info("  %s", "=" * 78)
        logger.info("  U4U ITERATION LOOP — FINAL SUMMARY")
        logger.info("  %s", "=" * 78)
        logger.info("  Converged: %s  |  Iterations: %d",
                     "✓ YES" if self.converged else "✗ NO", self.iterations_used)
        logger.info("")

        # Part 0: Excess Power & Steam availability for export
        self._log_excess_availability()

        # Part 0b: Demand vs Generation balance table
        self._log_demand_generation_balance()

        # Part 1: Demand summary table
        logger.info("  %-30s  %14s  %14s", "Utility (ODS Material)", "U4U Demand", "Total Demand")
        logger.info("  %s  %s  %s", "-" * 30, "-" * 14, "-" * 14)

        for material in sorted(self.final_u4u_demands.keys()):
            u4u_val = self.final_u4u_demands.get(material, 0.0)
            total_val = self.final_total_demands.get(material, 0.0)
            if abs(total_val) > 0.01 or abs(u4u_val) > 0.01:
                logger.info("  %-30s  %14.2f  %14.2f", material, u4u_val, total_val)

        logger.info("")

        # Build dynamic U4U consumption table (all ODS producers, including zero gen)
        dynamic_table = self._build_dynamic_u4u_table()
        self.final_dynamic_table = dynamic_table

        # Part 2: Detailed U4U consumption table
        logger.info("  U4U CONSUMPTION TABLE (final iteration)")
        logger.info("  %-25s  %-25s  %-6s  %14s  %14s  %10s  %-12s  %-25s  %-6s  %12s  %14s  %14s  %10s",
                     "Utility Plant", "Utility", "UOM", "Gen Qty", "BPC Gen Qty", "Gen Diff %",
                     "Account", "Material", "UOM", "Norm", "Quantity", "BPC Quantity", "Qty Diff %")
        logger.info("  %s  %s  %s  %s  %s  %s  %s  %s  %s  %s  %s  %s  %s",
                     "-" * 25, "-" * 25, "-" * 6, "-" * 14, "-" * 14, "-" * 10,
                     "-" * 12, "-" * 25, "-" * 6, "-" * 12, "-" * 14, "-" * 14, "-" * 10)

        for rec in dynamic_table:
            bpc_gen = self._lookup_bpc_gen_qty(rec["producer"])
            gen_diff_pct = self._pct_diff(rec["generation"], bpc_gen)
            bpc_qty = rec.get("bpc_quantity")
            if bpc_qty is None:
                bpc_qty = self._lookup_bpc_qty(rec["producer"], rec["material"])
            qty_diff_pct = self._pct_diff(rec["quantity"], bpc_qty)
            logger.info("  %-25s  %-25s  %-6s  %14.2f  %14.2f  %9.2f%%  %-12s  %-25s  %-6s  %12.6f  %14.2f  %14.2f  %9.2f%%",
                         rec["producer"][:25],
                         rec["producer_utility"][:25],
                         rec["producer_uom"][:6],
                         rec["generation"],
                         bpc_gen,
                         gen_diff_pct,
                         rec["account"][:12],
                         rec["material"][:25],
                         rec["material_uom"][:6],
                         rec["norm"],
                         rec["quantity"],
                         bpc_qty,
                         qty_diff_pct)

        logger.info("")

        # Part 3: Totals by material
        material_totals: dict = {}
        for rec in dynamic_table:
            mat = rec["material"]
            material_totals[mat] = material_totals.get(mat, 0.0) + rec["quantity"]

        logger.info("  TOTAL U4U CONSUMPTION BY MATERIAL:")
        logger.info("  %-30s  %14s  %-6s", "Material", "Total Qty", "UOM")
        logger.info("  %s  %s  %s", "-" * 30, "-" * 14, "-" * 6)

        # Get UOM for each material from detail records
        material_uoms = {}
        for rec in dynamic_table:
            if rec["material"] not in material_uoms:
                material_uoms[rec["material"]] = rec["material_uom"]

        for material in sorted(material_totals.keys()):
            total = material_totals[material]
            uom = material_uoms.get(material, "")
            if abs(total) > 0.01:
                logger.info("  %-30s  %14.2f  %-6s", material, total, uom)

        logger.info("")

        # Part 4: Per-utility summary comparison table
        logger.info("  GENERATION UTILITY COMPARISON SUMMARY")
        logger.info("  %-25s  %-6s  %14s  %14s  %10s  %8s",
                     "Utility Plant", "UOM", "Gen Qty", "BPC Gen Qty", "Gen Diff %", "# Mat")
        logger.info("  %s  %s  %s  %s  %s  %s",
                     "-" * 25, "-" * 6, "-" * 14, "-" * 14, "-" * 10, "-" * 8)

        # Aggregate per producer
        producer_summary: dict = {}
        for rec in dynamic_table:
            p = rec["producer"]
            if p not in producer_summary:
                bpc_gen = self._lookup_bpc_gen_qty(p)
                producer_summary[p] = {
                    "utility": rec["producer_utility"],
                    "uom": rec["producer_uom"],
                    "generation": rec["generation"],
                    "bpc_gen": bpc_gen,
                    "material_count": 0,
                }
            producer_summary[p]["material_count"] += 1

        for p in sorted(producer_summary.keys()):
            s = producer_summary[p]
            gen_diff = self._pct_diff(s["generation"], s["bpc_gen"])
            logger.info("  %-25s  %-6s  %14.2f  %14.2f  %9.2f%%  %8d",
                         p[:25], s["uom"][:6], s["generation"], s["bpc_gen"],
                         gen_diff, s["material_count"])

        logger.info("  %s", "=" * 78)
        logger.info("")

        # Part 5: Detailed asset dispatch summary (power + steam)
        self._log_asset_dispatch_summary()

    def _log_asset_dispatch_summary(self):
        """Log a detailed end-of-run dispatch summary for all power and steam assets."""
        pr = self.final_power_result or {}
        sr = self.final_steam_result or {}

        logger.info("  %s", "=" * 78)
        logger.info("  FINAL ASSET DISPATCH SUMMARY")
        logger.info("  %s", "=" * 78)

        # --- Power assets ---
        power_assets = pr.get("assets", [])
        logger.info("  POWER ASSETS")
        logger.info("  %-25s  %8s  %8s  %8s  %10s  %12s  %12s  %12s  %9s",
                     "Asset", "Pri", "Man", "Hours", "Load MW",
                     "Gross MWh", "Aux MWh", "Free Stm MT", "Load %")
        logger.info("  %s  %s  %s  %s  %s  %s  %s  %s  %s",
                     "-" * 25, "-" * 8, "-" * 8, "-" * 8, "-" * 10,
                     "-" * 12, "-" * 12, "-" * 12, "-" * 9)
        total_gross = 0.0
        total_aux   = 0.0
        total_fs    = 0.0
        for a in power_assets:
            gross    = a.get("dispatched_mwh", 0.0)
            aux      = a.get("aux_power", 0.0)
            fs       = a.get("free_steam_mt", 0.0)
            hours    = a.get("op_hours", 0.0)
            load_mw  = a.get("dispatched_mw", 0.0)
            load_pct = a.get("load_percent", 0.0)
            man_flag = "Y" if a.get("mandatory", 0) == 1 else "-"
            total_gross += gross
            total_aux   += aux
            total_fs    += fs
            logger.info("  %-25s  %8d  %8s  %8.2f  %10.2f  %12.2f  %12.2f  %12.2f  %9.1f%%",
                         a.get("asset_name", "")[:25], a.get("priority", 0), man_flag,
                         hours, load_mw, gross, aux, fs, load_pct)
        import_mwh = pr.get("total_import_mwh", pr.get("import_mwh", 0.0))
        logger.info("  %-25s  %8s  %8s  %8s  %10s  %12.2f  %12.2f  %12.2f  %9s",
                     "TOTAL", "", "", "", "", total_gross, total_aux, total_fs, "")
        if import_mwh > 0:
            logger.info("  %-25s  %8s  %8s  %8s  %10s  %12.2f  %12s  %12s  %9s",
                         "  + Import Power", "", "", "", "", import_mwh, "", "", "")
        logger.info("")

        # --- Steam assets ---
        steam_assets = sr.get("assets", [])
        free_steam_total = sr.get("total_free_steam_mt", 0.0)
        supp_total       = sr.get("total_supplementary_generation_mt", 0.0)
        gen_total        = sr.get("total_generation_mt", 0.0)
        dd = sr.get("demand_detail") or {}
        top_grade = dd.get("_top_grade", "shp")
        net_demand = dd.get(f"{top_grade}_net", 0.0)
        logger.info("  STEAM ASSETS  (SHP)")
        logger.info("  %-25s  %8s  %8s  %8s  %10s  %10s  %12s  %12s  %12s  %10s",
                     "Asset", "Pri", "Man", "Hours", "Min TPH",
                     "Max TPH", "Supp. MT", "Free Stm MT", "Total MT", "Load %")
        logger.info("  %s  %s  %s  %s  %s  %s  %s  %s  %s  %s",
                     "-" * 25, "-" * 8, "-" * 8, "-" * 8, "-" * 10,
                     "-" * 10, "-" * 12, "-" * 12, "-" * 12, "-" * 10)
        for a in steam_assets:
            man_flag = "Y" if a.get("mandatory", 0) == 1 else "-"
            orig_min = a.get("orig_min_tph", a.get("min_tph", 0.0))
            orig_max = a.get("orig_max_tph", a.get("max_tph", 0.0))
            fs_mt    = a.get("free_steam_mt", 0.0)
            supp_mt  = a.get("dispatched_mt", 0.0)
            tot_mt   = a.get("total_output_mt", supp_mt + fs_mt)
            logger.info("  %-25s  %8d  %8s  %8.2f  %10.2f  %10.2f  %12.2f  %12.2f  %12.2f  %9.1f%%",
                         a.get("asset_name", "")[:25], a.get("priority", 0), man_flag,
                         a.get("op_hours", 0.0), orig_min, orig_max,
                         supp_mt, fs_mt, tot_mt, a.get("load_percent", 0.0))
        logger.info("  %-25s  %8s  %8s  %8s  %10s  %10s  %12.2f  %12.2f  %12.2f  %10s",
                     "TOTAL", "", "", "", "", "", supp_total, free_steam_total, gen_total, "")
        logger.info("  Net SHP Demand: %.2f MT  |  Generation: %.2f MT  |  Balance: %.2f MT",
                     net_demand, gen_total, gen_total - net_demand)
        logger.info("  %s", "=" * 78)
        logger.info("")

    def _empty_result(self) -> dict:
        """Return empty result when ODS norms are not available."""
        return {
            "converged": False,
            "iterations_used": 0,
            "final_power_result": None,
            "final_steam_result": None,
            "final_u4u_demands": {},
            "final_total_demands": {},
            "final_detail_records": [],
            "iteration_history": [],
            "message": "ODS consumption norms not available",
        }
