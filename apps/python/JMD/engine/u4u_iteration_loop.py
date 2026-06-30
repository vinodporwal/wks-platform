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
from typing import Dict, Optional, Set

from engine.ods_norms_reader import ODSNormsReader
from engine.dispatch_engine import dispatch_power, dispatch_steam

logger = logging.getLogger(__name__)

CONVERGENCE_TOLERANCE = 0.0001  # 0.01% as a fraction
MAX_ITERATIONS = 50

DEFAULT_ALLOWED_ACCOUNTS: Set[str] = {"Utilities", "Raw Material"}

_DISPATCHABLE_POWER_PRODUCER = "POWERGEN"
_DISPATCHABLE_STEAM_PREFIXES = ("AUXBOIL", "HRSG")
_EXCLUDED_NON_DISPATCHABLE = {"Power_Dis"}

_DEMAND_PREFIX_TO_ODS_MATERIAL: Dict[str, str] = {
    "power": "Power_Dis",
    "shp": "SHP Steam_Dis",
    "hp": "HP Steam_Dis",
    "mp": "MP Steam_Dis",
    "lp": "LP Steam_Dis",
    "air": "COMPRESSED AIR",
    "nitrogen_asu": "NITROGEN_ASU",
    "dm": "D M Water",
    "cooling_water": "Cooling Water",
    "cw1": "Cooling Water",
    "cw2": "Cooling Water",
    "utility_water": "Utility Water",
    "oxygen": "Oxygen",
    "effluent": "Effluent",
    "ret_steam_condensate": "Condensate",
    "raw_water": "Raw Water",
}

_ODS_MATERIAL_TO_DEMAND_PREFIX: Dict[str, str] = {}
for _prefix, _material in _DEMAND_PREFIX_TO_ODS_MATERIAL.items():
    if _material not in _ODS_MATERIAL_TO_DEMAND_PREFIX:
        _ODS_MATERIAL_TO_DEMAND_PREFIX[_material] = _prefix

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
        ods_reader: Optional[ODSNormsReader] = None,
        allowed_accounts: Optional[Set[str]] = None,
        convergence_tolerance: float = CONVERGENCE_TOLERANCE,
        max_iterations: int = MAX_ITERATIONS,
    ):
        self.plant_id = plant_id
        self.month = month
        self.year = year
        self.initial_demands = initial_demands
        self.ods_reader = ods_reader or ODSNormsReader.get_reader(plant_id, month, year)
        self.allowed_accounts = allowed_accounts or DEFAULT_ALLOWED_ACCOUNTS
        self.convergence_tolerance = convergence_tolerance
        self.max_iterations = max_iterations

        self.consumption_norms: dict = {}
        self._bpc_gen_quantities: dict = {}
        self._bpc_quantities: dict = {}
        self._all_producers: set = set()
        self._initial_power_mwh: float = 0.0
        self._initial_steam_mt: Dict[str, float] = {}

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
        if not self.consumption_norms:
            logger.warning("  [U4U LOOP] No ODS consumption norms available")
            return self._empty_result()

        self._bpc_gen_quantities = self.ods_reader.get_bpc_generation_quantities()
        self._bpc_quantities = self.ods_reader.get_bpc_quantities()

        self._all_producers = set(self.consumption_norms.keys())
        self._precompute_initial_values()
        initial_utility_demands = self._build_initial_demands()

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
            "final_bpc_gen_quantities": self._bpc_gen_quantities,
            "final_bpc_quantities": self._bpc_quantities,
            "iteration_history": self.iteration_history,
        }

    # ------------------------------------------------------------------
    # Demand construction
    # ------------------------------------------------------------------

    def _precompute_initial_values(self):
        """Cache initial power and steam demand totals for dispatch construction."""
        power_process_kwh = float(self.initial_demands.get("power_process", 0.0))
        power_fixed_mwh = float(self.initial_demands.get("power_fixed", 0.0))
        self._initial_power_mwh = power_process_kwh / 1000.0 + power_fixed_mwh

        self._initial_steam_mt = {}
        for grade in ("shp", "hp", "mp", "lp"):
            process = float(self.initial_demands.get(f"{grade}_process", 0.0))
            fixed = float(self.initial_demands.get(f"{grade}_fixed", 0.0))
            self._initial_steam_mt[grade] = process + fixed

    def _build_initial_demands(self) -> dict:
        """Build initial demand map keyed by ODS material name."""
        demands = {}
        for prefix, ods_material in _DEMAND_PREFIX_TO_ODS_MATERIAL.items():
            process_val = float(self.initial_demands.get(f"{prefix}_process", 0.0))
            fixed_val = float(self.initial_demands.get(f"{prefix}_fixed", 0.0))
            if prefix == "power":
                process_val = process_val / 1000.0
            total = process_val + fixed_val
            demands[ods_material] = demands.get(ods_material, 0.0) + total

        for producer in self._all_producers:
            if producer not in demands:
                demands[producer] = 0.0

        return demands

    def _build_dispatch_demands(self, total_demands: dict) -> dict:
        """Construct demands dict in the format expected by dispatch functions."""
        dispatch_demands = dict(self.initial_demands)

        power_u4u = total_demands.get("Power_Dis", 0.0) - self._initial_power_mwh
        dispatch_demands["power_fixed"] = (
            float(self.initial_demands.get("power_fixed", 0.0)) + power_u4u
        )

        steam_ods_map = {"shp": "SHP Steam_Dis", "hp": "HP Steam_Dis",
                         "mp": "MP Steam_Dis", "lp": "LP Steam_Dis"}
        for grade, ods_material in steam_ods_map.items():
            steam_u4u = total_demands.get(ods_material, 0.0) - self._initial_steam_mt.get(grade, 0.0)
            dispatch_demands[f"{grade}_fixed"] = (
                float(self.initial_demands.get(f"{grade}_fixed", 0.0)) + steam_u4u
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

        return u4u, details

    def _calculate_u4u_from_power(self, power_result: dict) -> tuple:
        """Calculate U4U from power dispatch using per-asset ODS norms.

        Returns (u4u_dict, detail_records).
        """
        u4u: dict = {}
        details: list = []
        powergen = self.consumption_norms.get(_DISPATCHABLE_POWER_PRODUCER)
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
                    "producer_utility": _DISPATCHABLE_POWER_PRODUCER,
                    "producer_uom": producer_uom,
                    "generation": gen_kwh,
                    "account": account,
                    "material": material,
                    "material_uom": material_uom,
                    "norm": norm,
                    "quantity": quantity,
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
            if dispatched_mt <= 0:
                continue

            producer_norms = self._find_steam_producer_norms(asset_name)
            if not producer_norms:
                logger.warning("  [U4U LOOP] No ODS norms for steam asset: %s", asset_name)
                continue

            producer_uom = producer_norms.get("producer_uom", "MT")

            for c in producer_norms.get("consumptions", []):
                if c["account"] not in self.allowed_accounts:
                    continue

                norm = c["norm"]
                if norm == 0:
                    continue

                # Skip byproducts from steam assets — handled by steam dispatch internally
                if norm < 0:
                    continue

                material = c["material"]
                material_uom = c.get("material_uom", "")
                account = c["account"]

                quantity = dispatched_mt * norm
                u4u_amount = quantity
                if material == "Power_Dis":
                    u4u_amount = u4u_amount / 1000.0  # KWH → MWh

                if material in self._all_producers:
                    u4u[material] = u4u.get(material, 0.0) + u4u_amount

                details.append({
                    "producer": asset_name,
                    "producer_utility": asset_name,
                    "producer_uom": producer_uom,
                    "generation": dispatched_mt,
                    "account": account,
                    "material": material,
                    "material_uom": material_uom,
                    "norm": norm,
                    "quantity": quantity,
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
                })

        return u4u, details

    # ------------------------------------------------------------------
    # Helpers
    # ------------------------------------------------------------------

    def _is_dispatchable(self, producer_name: str) -> bool:
        """Check if a producer is dispatchable (power or steam assets)."""
        if producer_name == _DISPATCHABLE_POWER_PRODUCER:
            return True
        return any(producer_name.startswith(p) for p in _DISPATCHABLE_STEAM_PREFIXES)

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
        logger.info("  %-30s  %14.2f  MWh", "Total Generation", p_gen)
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

        # Part 1: Demand summary table
        logger.info("  %-30s  %14s  %14s", "Utility (ODS Material)", "U4U Demand", "Total Demand")
        logger.info("  %s  %s  %s", "-" * 30, "-" * 14, "-" * 14)

        for material in sorted(self.final_u4u_demands.keys()):
            u4u_val = self.final_u4u_demands.get(material, 0.0)
            total_val = self.final_total_demands.get(material, 0.0)
            if abs(total_val) > 0.01 or abs(u4u_val) > 0.01:
                logger.info("  %-30s  %14.2f  %14.2f", material, u4u_val, total_val)

        logger.info("")

        # Part 2: Detailed U4U consumption table
        logger.info("  U4U CONSUMPTION TABLE (final iteration)")
        logger.info("  %-25s  %-25s  %-6s  %14s  %14s  %10s  %-12s  %-25s  %-6s  %12s  %14s  %14s  %10s",
                     "Utility Plant", "Utility", "UOM", "Gen Qty", "BPC Gen Qty", "Gen Diff %",
                     "Account", "Material", "UOM", "Norm", "Quantity", "BPC Quantity", "Qty Diff %")
        logger.info("  %s  %s  %s  %s  %s  %s  %s  %s  %s  %s  %s  %s  %s",
                     "-" * 25, "-" * 25, "-" * 6, "-" * 14, "-" * 14, "-" * 10,
                     "-" * 12, "-" * 25, "-" * 6, "-" * 12, "-" * 14, "-" * 14, "-" * 10)

        for rec in self.final_detail_records:
            bpc_gen = self._lookup_bpc_gen_qty(rec["producer"])
            gen_diff_pct = self._pct_diff(rec["generation"], bpc_gen)
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
        for rec in self.final_detail_records:
            mat = rec["material"]
            material_totals[mat] = material_totals.get(mat, 0.0) + rec["quantity"]

        logger.info("  TOTAL U4U CONSUMPTION BY MATERIAL:")
        logger.info("  %-30s  %14s  %-6s", "Material", "Total Qty", "UOM")
        logger.info("  %s  %s  %s", "-" * 30, "-" * 14, "-" * 6)

        # Get UOM for each material from detail records
        material_uoms = {}
        for rec in self.final_detail_records:
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
        logger.info("  %-25s  %-6s  %14s  %14s  %10s  %8s  %14s",
                     "Utility Plant", "UOM", "Gen Qty", "BPC Gen Qty", "Gen Diff %", "# Mat", "Total U4U Qty")
        logger.info("  %s  %s  %s  %s  %s  %s  %s",
                     "-" * 25, "-" * 6, "-" * 14, "-" * 14, "-" * 10, "-" * 8, "-" * 14)

        # Aggregate per producer
        producer_summary: dict = {}
        for rec in self.final_detail_records:
            p = rec["producer"]
            if p not in producer_summary:
                bpc_gen = self._lookup_bpc_gen_qty(p)
                producer_summary[p] = {
                    "utility": rec["producer_utility"],
                    "uom": rec["producer_uom"],
                    "generation": rec["generation"],
                    "bpc_gen": bpc_gen,
                    "material_count": 0,
                    "total_u4u_qty": 0.0,
                }
            producer_summary[p]["material_count"] += 1
            producer_summary[p]["total_u4u_qty"] += rec["quantity"]

        for p in sorted(producer_summary.keys()):
            s = producer_summary[p]
            gen_diff = self._pct_diff(s["generation"], s["bpc_gen"])
            logger.info("  %-25s  %-6s  %14.2f  %14.2f  %9.2f%%  %8d  %14.2f",
                         p[:25], s["uom"][:6], s["generation"], s["bpc_gen"],
                         gen_diff, s["material_count"], s["total_u4u_qty"])

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
