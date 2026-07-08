"""
NMD Dynamic Budget Output Generator

Generates budget format comparison tables dynamically from the NMDNormsReader
consumption matrix and NMDIterationLoop results — no hardcoded utility names
or norm multipliers.

The generator:
1. Iterates over all generation utilities discovered by NMDNormsReader
2. For each utility, iterates over all its consumption materials
3. Calculates model quantities (generation × norm)
4. Compares against BPC reference quantities
5. Formats output lines in the same format as nmd_budget_comparison_service.py

Key differences from the existing hardcoded approach:
- Utility/material lines are generated dynamically from the norms matrix
- Reverse-calculated norms (GT NG, HRSG NG, STG SHP) are applied via overrides
- Distribution ratios (Power_Dis, SHP/HP/MP/LP Steam_Dis) are applied via overrides
- BPC reference data is looked up by matching plant/utility/material names
"""

from __future__ import annotations

import os
import logging
from typing import Dict, List, Optional, Tuple

from services.nmd_norms_reader import NMDNormsReader
from services.nmd_budget_comparison_service import _fetch_cpp_norm, _preload_cpp_norms

logger = logging.getLogger(__name__)

MONTH_NAMES = {
    1: "January", 2: "February", 3: "March", 4: "April",
    5: "May", 6: "June", 7: "July", 8: "August",
    9: "September", 10: "October", 11: "November", 12: "December",
}


def _month_name(month: int) -> str:
    return MONTH_NAMES.get(month, str(month))


def _calc_pct(calc: float, ref: float) -> str:
    calc = float(calc or 0)
    ref = float(ref or 0)
    if ref != 0:
        return f"{((calc - ref) / ref) * 100:>+8.2f}%"
    if calc == 0:
        return f"{'0.00%':>9}"
    return f"{'N/A':>9}"


def _fmt_qty(value: float) -> str:
    value = float(value or 0)
    return f"{value:>18,.2f}"


def _fmt_norm(value: float, norm_fmt: str) -> str:
    value = float(value or 0)
    abs_value = abs(value)
    if 0 < abs_value < 0.0001:
        return f"{value:>10.7f}"
    if 0 < abs_value < 0.001:
        return f"{value:>10.6f}"
    if 0 < abs_value < 0.01:
        return f"{value:>10.4f}"
    return f"{format(value, norm_fmt):>10}"


# Material name display mapping: DB full name → shortened display name
_MATERIAL_NAME_MAP = {
    "CHEM ALUM.SULFATE, AL2(SO4)3,18H2O": "CHEM ALUM.SULFATE",
    "CHEM  SODIUM SULPHITE;PN:MIS 19OX": "CHEM SODIUM SULPHITE",
    "SODIUM CHLORIDE IS 797 GRADE1": "SODIUM CHLORIDE",
    "UREA,NITROGEN CONTENT 46%": "UREA",
    "CAUSTIC SODA LYE – GRADE 1": "CAUSTIC SODA LYE",
    "FURNACE OIL ( MEDIUM VISCOSITY GRADE )": "FURNACE OIL",
    "HYDRO CHLORIC ACID (30%) -VIRGIN": "HYDRO CHLORIC ACID",
}

# Utility display name mapping: DB utility name → output display name
_UTILITY_NAME_MAP = {
    "STG1_LP STEAM": "LP Extraction (STG)",
    "STG1_MP STEAM": "MP Extraction (STG)",
}


def _shorten_material(name: str) -> str:
    """Shorten DB material name to match existing budget comparison output."""
    if name in _MATERIAL_NAME_MAP:
        return _MATERIAL_NAME_MAP[name]
    # Generic truncation: cut at comma, semicolon, or opening parenthesis
    for sep in [",", ";", "("]:
        idx = name.find(sep)
        if idx > 0:
            return name[:idx].strip()
    return name


def _line(
    plant: str,
    utility: str,
    material: str,
    uom: str,
    model_qty: float,
    ref_qty: float,
    norm: float,
    calc_value: float,
    ref_value: float,
    norm_fmt: str,
) -> str:
    model_qty = float(model_qty or 0)
    ref_qty = float(ref_qty or 0)
    norm = float(norm or 0)
    calc_value = float(calc_value or 0)
    ref_value = float(ref_value or 0)
    diff = calc_value - ref_value
    return (
        f"{plant:<25} {utility:<20} {material:<25} {uom:<8} "
        f"{_fmt_qty(model_qty)} {ref_qty:>18,.2f} {_fmt_norm(norm, norm_fmt)} "
        f"{calc_value:>20,.2f} {ref_value:>20,.2f} {diff:>18,.2f} {_calc_pct(calc_value, ref_value)}"
    )


class NMDBudgetGenerator:
    """
    Dynamic budget format comparison generator.

    Usage::

        reader = NMDNormsReader.get_reader(month, year)
        generator = NMDBudgetGenerator(reader)
        text, cpp_totals, bpc_totals = generator.generate(
            month, year, financial_year, calculation_result, bpc_csv_path
        )
    """

    def __init__(self, norms_reader: NMDNormsReader):
        self.reader = norms_reader
        self.consumption_norms = norms_reader.get_consumption_norms()
        self._generation_utilities = norms_reader.get_generation_utilities()
        self._bpc_book = None

    # ------------------------------------------------------------------
    # Main generation method
    # ------------------------------------------------------------------

    def generate(
        self,
        month: int,
        year: int,
        financial_year: int,
        calculation_result: dict,
        bpc_csv_path: Optional[str] = None,
    ) -> Tuple[str, dict, dict]:
        """
        Generate the budget comparison text.

        Args:
            month, year: Financial period.
            financial_year: Starting year of the financial year.
            calculation_result: Result from NMDIterationLoop.run() or
                the existing usd_iterate() function.
            bpc_csv_path: Path to BPC reference file (ODS or CSV).

        Returns:
            (comparison_text, cpp_totals, bpc_totals)
        """
        month_nm = _month_name(month)

        # Preload CPPNorms cache for norm lookups
        _preload_cpp_norms(month, year)

        # Load BPC reference book
        if bpc_csv_path is None:
            bpc_csv_path = os.path.join(
                os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                "BPC.ods",
            )
        self._bpc_book = self._load_bpc_book(bpc_csv_path)

        # Extract model values from calculation result
        model_values = self._extract_model_values(calculation_result)

        # Build generation quantity map (plant → utility → model_qty)
        gen_map = self._build_generation_map(model_values, calculation_result)

        # Build norm override map (for reverse-calculated norms)
        norm_overrides = self._build_norm_overrides(model_values, calculation_result, month, year)

        # Build case-insensitive lookup for norm overrides (fallback for DB name variations)
        norm_overrides_ci = {
            (k[0].lower(), k[1].lower(), k[2].lower()): v
            for k, v in norm_overrides.items()
        }

        # Build distribution ratio overrides
        ratio_overrides = self._build_ratio_overrides(model_values, calculation_result)

        # Generate comparison lines
        lines: List[str] = []
        cpp_totals: Dict[str, float] = {}
        bpc_totals: Dict[str, float] = {}

        lines.append("=" * 220)
        lines.append(
            f"NMD BUDGET FORMAT - DYNAMIC COMPARISON ({month_nm} {year}) | "
            f"FY {financial_year}-{str(financial_year + 1)[-2:]}"
        )
        lines.append("=" * 220)
        lines.append(
            f"{'Generating Plant':<25} {'Utility':<20} {'Material':<25} {'UOM':<8} "
            f"{'QTY (Model)':>18} {'QTY (Ref)':>18} {'Norms':>10} "
            f"{'Calculated (Model)':>20} {'Reference (BPC)':>20} {'Difference':>18} {'% Diff':>10}"
        )
        lines.append("-" * 220)

        # Group consumption norms by plant name
        plant_groups = self._group_by_plant()

        # Track which utilities we've already output to avoid duplicates
        seen_utility_plant: set = set()

        for plant_name in sorted(plant_groups.keys()):
            utilities = plant_groups[plant_name]

            for utility_name in sorted(utilities.keys()):
                # Skip distribution utilities (they use ratios, not generation × norm)
                if utility_name in ("Power_Dis", "SHP Steam_Dis", "HP Steam_Dis",
                                    "MP Steam_Dis", "LP Steam_Dis"):
                    continue

                # Skip if already seen (same plant+utility)
                key = (plant_name, utility_name)
                if key in seen_utility_plant:
                    continue
                seen_utility_plant.add(key)

                # Skip utilities not in the generation map (materials misclassified
                # as utilities in DB Raw Material rows)
                if key not in gen_map:
                    continue

                # Get generation quantity for this plant+utility
                model_gen_qty = gen_map.get(key, 0.0)

                # Get BPC reference quantity
                ref_qty = self._get_bpc_ref_qty(month_nm, plant_name, utility_name)

                # Get all consumptions for this utility (already deduplicated)
                consumptions = utilities[utility_name]
                if not consumptions:
                    continue

                # Determine UOM for this utility
                uom = self._infer_uom(utility_name)

                first_line = True
                prev_account = None
                for c in sorted(consumptions, key=lambda x: (x.get("account", ""), x.get("material", ""))):
                    material = c.get("material", "")
                    if not material or material == "nan":
                        continue
                    account = c.get("account", "")
                    norm = c.get("norm", 0.0)

                    # Shorten material name for display
                    material_display = _shorten_material(material)

                    # Apply norm overrides (reverse-calculated norms)
                    override_key = (plant_name, utility_name, material)
                    if override_key in norm_overrides:
                        norm = norm_overrides[override_key]
                    else:
                        # Case-insensitive fallback for DB name variations
                        ci_key = (plant_name.lower(), utility_name.lower(), material.lower())
                        if ci_key in norm_overrides_ci:
                            norm = norm_overrides_ci[ci_key]

                    # Apply ratio overrides for distribution utilities
                    if material in ratio_overrides:
                        ratio = ratio_overrides[material]
                        # For distribution rows, the "generation" is the total demand
                        # and the norm is the ratio
                        calc_value = model_gen_qty * ratio
                        ref_value = ref_qty * ratio if ref_qty > 0 else 0.0
                    else:
                        calc_value = model_gen_qty * norm
                        # Nitrogen Gas is a credit (negative calc value)
                        if material_display.upper() == "NITROGEN GAS" or material_display.upper() == "NITROGEN":
                            calc_value = -calc_value
                        ref_value = self._get_bpc_ref_value(
                            month_nm, plant_name, utility_name, material, norm, ref_qty
                        )

                    # Determine norm format
                    norm_fmt = self._infer_norm_fmt(material, norm)

                    # Determine material UOM
                    material_uom = self._infer_material_uom(material)

                    # Output line
                    plant_display = plant_name if first_line else ""
                    if first_line:
                        utility_display = _UTILITY_NAME_MAP.get(utility_name, utility_name)
                    elif account != prev_account:
                        utility_display = account
                    else:
                        utility_display = ""

                    lines.append(_line(
                        plant_display, utility_display, material_display,
                        material_uom, model_gen_qty, ref_qty,
                        norm, calc_value, ref_value, norm_fmt,
                    ))

                    prev_account = account

                    # Accumulate totals
                    cpp_key = f"{plant_name}::{utility_name}::{material}"
                    cpp_totals[cpp_key] = cpp_totals.get(cpp_key, 0.0) + calc_value
                    bpc_totals[cpp_key] = bpc_totals.get(cpp_key, 0.0) + ref_value

                    first_line = False

                if not first_line:
                    lines.append("")

        # Add distribution section
        lines.extend(self._generate_distribution_section(
            month_nm, model_values, calculation_result, ratio_overrides,
        ))

        # Add summary
        lines.extend(self._generate_summary(cpp_totals, bpc_totals))

        return "\n".join(lines), cpp_totals, bpc_totals

    # ------------------------------------------------------------------
    # Model value extraction
    # ------------------------------------------------------------------

    def _extract_model_values(self, calc_result: dict) -> dict:
        """Extract model-calculated values from the iteration result."""
        usd_result = calc_result.get("usd_result", {}) or calc_result
        power_result = usd_result.get("power_result", {}) or {}
        dispatch = usd_result.get("final_dispatch", []) or []
        steam_balance = usd_result.get("final_steam_balance", {}) or calc_result.get("steam_result", {}) or {}
        stg_extraction = usd_result.get("stg_extraction", {}) or calc_result.get("stg_extraction", {}) or {}
        utility_consumption = calc_result.get("utility_consumption", {}) or {}
        hrsg_dispatch = usd_result.get("hrsg_dispatch", {}) or {}

        # Extract GT/STG generation
        gt1_gross = gt2_gross = gt3_gross = stg_gross = 0.0
        gt1_net = gt2_net = gt3_net = stg_net = 0.0
        stg_hours = 0.0

        for asset in dispatch:
            name = str(asset.get("AssetName", "")).upper()
            gross = asset.get("GrossMWh", 0) or 0
            net = asset.get("NetMWh", 0) or 0
            if "PLANT-1" in name or "GT1" in name:
                gt1_gross = gross
                gt1_net = net
            elif "PLANT-2" in name or "GT2" in name:
                gt2_gross = gross
                gt2_net = net
            elif "PLANT-3" in name or "GT3" in name:
                gt3_gross = gross
                gt3_net = net
            elif "STG" in name or "STEAM TURBINE" in name:
                stg_gross = gross
                stg_net = net
                stg_hours = asset.get("Hours", 0) or 0

        # Extract steam balance values
        lp_bal = steam_balance.get("lp_balance", {}) or {}
        mp_bal = steam_balance.get("mp_balance", {}) or {}
        hp_bal = steam_balance.get("hp_balance", {}) or {}

        # Extract HRSG generation
        hrsg1_shp = 0.0
        hrsg2_shp = 0.0
        hrsg3_shp = 0.0
        if hrsg_dispatch:
            hrsg1_shp = hrsg_dispatch.get("hrsg1_dispatched_mt", 0) or 0
            hrsg2_shp = hrsg_dispatch.get("hrsg2_dispatched_mt", 0) or 0
            hrsg3_shp = hrsg_dispatch.get("hrsg3_dispatched_mt", 0) or 0
        # Fallback to utility_consumption
        if hrsg1_shp == 0:
            hrsg1_shp = utility_consumption.get("shp_from_hrsg1", 0) or 0
        if hrsg2_shp == 0:
            hrsg2_shp = utility_consumption.get("shp_from_hrsg2", 0) or 0
        if hrsg3_shp == 0:
            hrsg3_shp = utility_consumption.get("shp_from_hrsg3", 0) or 0

        # Extract natural gas norms
        ng = utility_consumption.get("natural_gas", {}) or {}

        # Extract utility consumption totals
        bfw = utility_consumption.get("bfw", {}) or {}
        dm = utility_consumption.get("dm_water", {}) or {}
        cw = utility_consumption.get("cooling_water", {}) or {}
        air = utility_consumption.get("compressed_air", {}) or {}

        # Import power — match existing service: extract from utility_consumption
        import_power_mwh = utility_consumption.get("import_power_mwh", 0) or 0
        total_demand_mwh = utility_consumption.get("total_demand_mwh", 0) or 0

        return {
            "gt1_gross_mwh": gt1_gross,
            "gt2_gross_mwh": gt2_gross,
            "gt3_gross_mwh": gt3_gross,
            "stg_gross_mwh": stg_gross,
            "gt1_net_mwh": gt1_net,
            "gt2_net_mwh": gt2_net,
            "gt3_net_mwh": gt3_net,
            "stg_net_mwh": stg_net,
            "stg_hours": stg_hours,
            "gt1_kwh": gt1_gross * 1000,
            "gt2_kwh": gt2_gross * 1000,
            "gt3_kwh": gt3_gross * 1000,
            "stg_kwh": stg_gross * 1000,
            "gt1_net_kwh": gt1_net * 1000,
            "gt2_net_kwh": gt2_net * 1000,
            "gt3_net_kwh": gt3_net * 1000,
            "stg_net_kwh": stg_net * 1000,
            "hrsg1_shp": hrsg1_shp,
            "hrsg2_shp": hrsg2_shp,
            "hrsg3_shp": hrsg3_shp,
            "hp_prds": hp_bal.get("hp_total", 0) or hp_bal.get("hp_from_prds", 0) or 0,
            "lp_prds": lp_bal.get("lp_from_prds", 0) or 0,
            "mp_prds": mp_bal.get("mp_from_prds", 0) or 0,
            "stg_lp": lp_bal.get("lp_from_stg_available", lp_bal.get("lp_from_stg", 0)) or 0,
            "stg_mp": mp_bal.get("mp_from_stg_available", mp_bal.get("mp_from_stg", 0)) or 0,
            "lp_total": lp_bal.get("lp_total", 0) or 0,
            "mp_total": mp_bal.get("mp_total", 0) or 0,
            "hp_total": hp_bal.get("hp_total", 0) or 0,
            "bfw_total": bfw.get("total_m3", 0) or 0,
            "dm_total": dm.get("total_m3", 0) or 0,
            "cw1_total": cw.get("cw1_total_km3", 0) or 0,
            "cw2_total": cw.get("cw2_total_km3", 0) or 0,
            "air_total": air.get("total_nm3", 0) or 0,
            "oxygen_mt": utility_consumption.get("oxygen_mt", 0) or 0,
            "effluent_m3": utility_consumption.get("effluent_m3", 0) or 0,
            "import_power_mwh": import_power_mwh,
            "total_demand_mwh": total_demand_mwh,
            "total_demand_kwh": total_demand_mwh * 1000,
            "import_power_kwh": import_power_mwh * 1000,
            "ng": ng,
            "stg_extraction": stg_extraction,
            "sp_steam_power": stg_extraction.get("sp_steam_power", 0) or 0,
            "stg_shp_inlet_mt": stg_extraction.get("stg_shp_inlet_mt", 0) or 0,
            "stg_condensate_m3": stg_extraction.get("stg_condensate_m3", 0) or 0,
        }

    # ------------------------------------------------------------------
    # Generation map builder
    # ------------------------------------------------------------------

    def _build_generation_map(self, mv: dict, calc_result: dict) -> dict:
        """
        Build (plant, utility) → generation quantity map.

        Maps model-calculated generation quantities to the plant/utility
        pairs discovered by the norms reader.
        """
        gen_map: Dict[Tuple[str, str], float] = {}

        # Power plants (GT1, GT2, GT3, STG) — QTY in KWH
        gen_map[("NMD - Power Plant 1", "POWERGEN")] = mv["gt1_kwh"]
        gen_map[("NMD - Power Plant 1", "Utilities")] = mv["gt1_kwh"]
        gen_map[("NMD - Power Plant 2", "POWERGEN")] = mv["gt2_kwh"]
        gen_map[("NMD - Power Plant 2", "Utilities")] = mv["gt2_kwh"]
        gen_map[("NMD - Power Plant 3", "POWERGEN")] = mv["gt3_kwh"]
        gen_map[("NMD - Power Plant 3", "Utilities")] = mv["gt3_kwh"]
        gen_map[("NMD - STG Power Plant", "POWERGEN")] = mv["stg_kwh"]

        # HRSG steam generation — QTY in MT
        gen_map[("NMD - Utility Plant", "HRSG1_SHP STEAM")] = mv["hrsg1_shp"]
        gen_map[("NMD - Utility Plant", "HRSG2_SHP STEAM")] = mv["hrsg2_shp"]
        gen_map[("NMD - Utility Plant", "HRSG3_SHP STEAM")] = mv["hrsg3_shp"]

        # STG extraction — QTY in MT (display names: LP/MP Extraction (STG))
        gen_map[("NMD - Utility Plant", "STG1_LP STEAM")] = mv["stg_lp"]
        gen_map[("NMD - Utility Plant", "STG1_MP STEAM")] = mv["stg_mp"]

        # PRDS steam — QTY in MT
        gen_map[("NMD - Utility Plant", "HP Steam PRDS")] = mv["hp_prds"]
        gen_map[("NMD - Utility Plant", "MP Steam PRDS SHP")] = mv["mp_prds"]
        gen_map[("NMD - Utility Plant", "LP Steam PRDS")] = mv["lp_prds"]

        # Utility plants — QTY in their respective UOM
        gen_map[("NMD - Utility Plant", "Boiler Feed Water")] = mv["bfw_total"]
        gen_map[("NMD - Utility Plant", "D M Water")] = mv["dm_total"]
        gen_map[("NMD - Utility Plant", "Cooling Water 1")] = mv["cw1_total"]
        gen_map[("NMD - Utility Plant", "Cooling Water 2")] = mv["cw2_total"]
        gen_map[("NMD - Utility Plant", "COMPRESSED AIR")] = mv["air_total"]
        gen_map[("NMD - Utility Plant", "Oxygen")] = mv["oxygen_mt"]
        gen_map[("NMD - Utility Plant", "Effluent Treated")] = mv["effluent_m3"]

        # Distribution totals
        gen_map[("NMD - Utility/Power Dist", "Power_Dis")] = mv["total_demand_kwh"]
        gen_map[("NMD - Utility/Power Dist", "SHP Steam_Dis")] = (
            mv["hrsg1_shp"] + mv["hrsg2_shp"] + mv["hrsg3_shp"]
        )
        gen_map[("NMD - Utility/Power Dist", "LP Steam_Dis")] = mv["lp_total"]
        gen_map[("NMD - Utility/Power Dist", "MP Steam_Dis")] = mv["mp_total"]
        gen_map[("NMD - Utility/Power Dist", "HP Steam_Dis")] = mv["hp_total"]

        return gen_map

    # ------------------------------------------------------------------
    # Norm override builder (reverse-calculated norms)
    # ------------------------------------------------------------------

    def _build_norm_overrides(self, mv: dict, calc_result: dict, month: int = 0, year: int = 0) -> dict:
        """
        Build norm overrides for reverse-calculated norms.

        These norms are NOT read directly from the DB but are computed
        during the iteration loop:
        - GT NG norms (from heat rate lookup with free steam deduction)
        - HRSG NG norms (from heat rate lookup)
        - STG SHP norm (from STG extraction lookup)
        - STG condensate norm (from STG extraction lookup)
        - Power distribution ratios (Import/PP1/PP2/PP3/STG)
        - SHP steam distribution ratios (HRSG1/2/3)
        - LP/MP steam distribution ratios (STG/PRDS)
        """
        overrides: Dict[Tuple[str, str, str], float] = {}
        ng = mv["ng"]
        stg_ext = mv["stg_extraction"]

        # GT NG norms (MMBTU/KWH)
        overrides[("NMD - Power Plant 1", "POWERGEN", "NATURAL GAS")] = ng.get("gt1_ng_norm", 0.0095)
        overrides[("NMD - Power Plant 2", "POWERGEN", "NATURAL GAS")] = ng.get("gt2_ng_norm", 0.0101)
        overrides[("NMD - Power Plant 3", "POWERGEN", "NATURAL GAS")] = ng.get("gt3_ng_norm", 0.0095)

        # HRSG NG norms (MMBTU/MT)
        overrides[("NMD - Utility Plant", "HRSG1_SHP STEAM", "NATURAL GAS")] = ng.get("hrsg1_ng_norm", 2.8064)
        overrides[("NMD - Utility Plant", "HRSG2_SHP STEAM", "NATURAL GAS")] = ng.get("hrsg2_ng_norm", 2.8064)
        overrides[("NMD - Utility Plant", "HRSG3_SHP STEAM", "NATURAL GAS")] = ng.get("hrsg3_ng_norm", 2.8168)

        # STG SHP norm (MT/KWH) — fetch from CPPNorms
        stg_shp_norm = _fetch_cpp_norm(month, year, 'NMD - STG Power Plant', 'POWERGEN', 'SHP Steam_Dis', 0.0036)
        overrides[("NMD - STG Power Plant", "POWERGEN", "SHP Steam_Dis")] = stg_shp_norm

        # STG condensate norm (M3/KWH) — fetch from CPPNorms, force negative
        stg_condensate_norm = _fetch_cpp_norm(month, year, 'NMD - STG Power Plant', 'POWERGEN', 'Ret steam condensate', -0.0029)
        if stg_condensate_norm > 0:
            stg_condensate_norm = -stg_condensate_norm
        overrides[("NMD - STG Power Plant", "POWERGEN", "Ret steam condensate")] = stg_condensate_norm

        # HRSG BFW norm — fetch from CPPNorms (HRSG1 for all HRSGs)
        hrsg_bfw_norm = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'HRSG1_SHP STEAM', 'Boiler Feed Water', 1.1614)
        for u in ("HRSG1_SHP STEAM", "HRSG2_SHP STEAM", "HRSG3_SHP STEAM"):
            overrides[("NMD - Utility Plant", u, "Boiler Feed Water")] = hrsg_bfw_norm

        # HRSG LP Steam_Dis norm — fetch from CPPNorms (HRSG1 for all HRSGs)
        hrsg_lp_norm = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'HRSG1_SHP STEAM', 'LP Steam_Dis', 0.0121)
        for u in ("HRSG1_SHP STEAM", "HRSG2_SHP STEAM", "HRSG3_SHP STEAM"):
            overrides[("NMD - Utility Plant", u, "LP Steam_Dis")] = hrsg_lp_norm

        # FURNACE OIL norm — fetch from CPPNorms (HRSG2 for all HRSGs that have it)
        furnace_oil_norm = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'HRSG2_SHP STEAM', 'FURNACE OIL', 0.0000983)
        for u in ("HRSG2_SHP STEAM", "HRSG3_SHP STEAM"):
            overrides[("NMD - Utility Plant", u, "FURNACE OIL ( MEDIUM VISCOSITY GRADE )")] = furnace_oil_norm

        # UREA norm — fetch from CPPNorms for Effluent Treated
        urea_norm = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'Effluent Treated', 'UREA', 0.00075)
        overrides[("NMD - Utility Plant", "Effluent Treated", "UREA,NITROGEN CONTENT 46%")] = urea_norm

        # STG extraction SHP norms = 0.0 (Option C: physical flow norms = 0)
        # STG Power Generation accounts for ALL inlet steam.
        # Extraction rows are informational and consume 0 additional SHP.
        overrides[("NMD - Utility Plant", "STG1_LP STEAM", "SHP Steam_Dis")] = 0.0
        overrides[("NMD - Utility Plant", "STG1_MP STEAM", "SHP Steam_Dis")] = 0.0

        # Power distribution norms (ratios)
        total_demand_kwh = mv["total_demand_kwh"] if mv["total_demand_kwh"] > 0 else 1
        overrides[("NMD - Utility/Power Dist", "Power_Dis", "Power from MEL")] = (
            mv["import_power_kwh"] / total_demand_kwh
        )
        overrides[("NMD - Utility/Power Dist", "Power_Dis", "POWERGEN (PP1)")] = (
            mv["gt1_net_kwh"] / total_demand_kwh if mv["gt1_net_kwh"] > 0 else 0
        )
        overrides[("NMD - Utility/Power Dist", "Power_Dis", "POWERGEN (PP2)")] = (
            mv["gt2_net_kwh"] / total_demand_kwh if mv["gt2_net_kwh"] > 0 else 0
        )
        overrides[("NMD - Utility/Power Dist", "Power_Dis", "POWERGEN (PP3)")] = (
            mv["gt3_net_kwh"] / total_demand_kwh if mv["gt3_net_kwh"] > 0 else 0
        )
        overrides[("NMD - Utility/Power Dist", "Power_Dis", "POWERGEN (STG)")] = (
            mv["stg_net_kwh"] / total_demand_kwh if mv["stg_net_kwh"] > 0 else 0
        )

        # SHP steam distribution ratios
        total_shp = mv["hrsg1_shp"] + mv["hrsg2_shp"] + mv["hrsg3_shp"]
        if total_shp > 0:
            overrides[("NMD - Utility/Power Dist", "SHP Steam_Dis", "HRSG1_SHP STEAM")] = mv["hrsg1_shp"] / total_shp
            overrides[("NMD - Utility/Power Dist", "SHP Steam_Dis", "HRSG2_SHP STEAM")] = mv["hrsg2_shp"] / total_shp
            overrides[("NMD - Utility/Power Dist", "SHP Steam_Dis", "HRSG3_SHP STEAM")] = mv["hrsg3_shp"] / total_shp

        # LP steam distribution ratios
        lp_stg_ratio = stg_ext.get("lp_stg_ratio", 0.6134) or 0.6134
        lp_prds_ratio = 1.0 - lp_stg_ratio
        overrides[("NMD - Utility/Power Dist", "LP Steam_Dis", "STG1_LP STEAM")] = lp_stg_ratio
        overrides[("NMD - Utility/Power Dist", "LP Steam_Dis", "LP Steam PRDS")] = lp_prds_ratio

        # MP steam distribution ratios
        mp_stg_ratio = stg_ext.get("mp_stg_ratio", 0.2908) or 0.2908
        mp_prds_ratio = 1.0 - mp_stg_ratio
        overrides[("NMD - Utility/Power Dist", "MP Steam_Dis", "STG1_MP STEAM")] = mp_stg_ratio
        overrides[("NMD - Utility/Power Dist", "MP Steam_Dis", "MP Steam PRDS SHP")] = mp_prds_ratio

        # HP steam distribution (100% from PRDS)
        overrides[("NMD - Utility/Power Dist", "HP Steam_Dis", "HP Steam PRDS")] = 1.0

        return overrides

    # ------------------------------------------------------------------
    # Ratio overrides for distribution utilities
    # ------------------------------------------------------------------

    def _build_ratio_overrides(self, mv: dict, calc_result: dict) -> dict:
        """
        Build ratio overrides for distribution utility materials.

        For distribution rows (Power_Dis, SHP/HP/MP/LP Steam_Dis), the
        "norm" is actually a distribution ratio, not a consumption norm.
        The generation quantity is the total demand, and the calculated
        value = total_demand × ratio.
        """
        return {}  # Handled via norm_overrides — distribution rows use
        # the same formula (gen_qty × norm) but norm = ratio

    # ------------------------------------------------------------------
    # BPC reference book
    # ------------------------------------------------------------------

    def _load_bpc_book(self, bpc_csv_path: str):
        """Load BPC reference book from ODS or CSV file."""
        try:
            from services.nmd_budget_comparison_service import BPCReferenceBook
            return BPCReferenceBook(bpc_csv_path)
        except Exception as e:
            logger.warning("  [NMD Budget] Could not load BPC reference book: %s", e)
            return None

    def _get_bpc_ref_qty(self, month_name: str, plant: str, utility: str) -> float:
        """Get BPC reference generation quantity for a plant/utility."""
        if not self._bpc_book:
            return 0.0
        try:
            return self._bpc_book.infer_section_ref_qty(
                month_name, plant, utility, account_filter="Utilities"
            )
        except Exception:
            return 0.0

    def _get_bpc_ref_value(
        self, month_name: str, plant: str, utility: str,
        material: str, norm: float, ref_qty: float,
    ) -> float:
        """Get BPC reference value (quantity) for a specific material."""
        if not self._bpc_book:
            return 0.0
        try:
            return self._bpc_book.calculate_bpc_ref_qty(
                month_name, utility, material, norm, ref_qty,
                generating_plant=plant,
            )
        except Exception:
            return 0.0

    # ------------------------------------------------------------------
    # Grouping helpers
    # ------------------------------------------------------------------

    def _group_by_plant(self) -> Dict[str, Dict[str, List[dict]]]:
        """
        Group consumption norms by plant name -> utility name -> consumptions.

        Uses the per-consumption ``plant_name`` field (not the top-level
        one) so that multi-plant utilities like POWERGEN are correctly
        separated into Power Plant 1, 2, 3, STG.

        Entries with empty material names are skipped.
        Duplicate (plant, utility, material, account) entries are merged.
        """
        groups: Dict[str, Dict[str, List[dict]]] = {}
        seen: set = set()

        for utility_name, info in self.consumption_norms.items():
            for c in info.get("consumptions", []):
                material = (c.get("material") or "").strip()
                if not material or material == "nan" or material == "No Material":
                    continue

                plant = (c.get("plant_name") or info.get("plant_name") or "").strip()
                account = c.get("account", "")

                dedup_key = (plant, utility_name, material, account)
                if dedup_key in seen:
                    continue
                seen.add(dedup_key)

                if plant not in groups:
                    groups[plant] = {}
                if utility_name not in groups[plant]:
                    groups[plant][utility_name] = []
                groups[plant][utility_name].append(c)

        return groups

    # ------------------------------------------------------------------
    # UOM inference
    # ------------------------------------------------------------------

    def _infer_uom(self, utility_name: str) -> str:
        """Infer the producer UOM from utility name."""
        name_upper = utility_name.upper()
        if "POWERGEN" in name_upper:
            return "KWH"
        if "STEAM" in name_upper or "PRDS" in name_upper:
            return "MT"
        if "BOILER FEED" in name_upper:
            return "M3"
        if "D M WATER" in name_upper or "DM WATER" in name_upper:
            return "M3"
        if "COOLING WATER" in name_upper:
            return "KM3"
        if "COMPRESSED AIR" in name_upper or "AIR" in name_upper:
            return "NM3"
        if "OXYGEN" in name_upper:
            return "MT"
        if "EFFLUENT" in name_upper:
            return "M3"
        return ""

    def _infer_material_uom(self, material: str) -> str:
        """Infer the material UOM from material name."""
        mat_upper = material.upper()
        if "NATURAL GAS" in mat_upper:
            return "MMBTU"
        if "CONDENSATE" in mat_upper:
            return "M3"
        if "POWER" in mat_upper or mat_upper == "POWER_DIS":
            return "KWH"
        if "STEAM" in mat_upper:
            return "MT"
        if "BOILER FEED" in mat_upper or "BFW" in mat_upper:
            return "M3"
        if "D M WATER" in mat_upper or "DM WATER" in mat_upper:
            return "M3"
        if "COOLING WATER" in mat_upper:
            return "KM3"
        if "COMPRESSED AIR" in mat_upper or "AIR" in mat_upper:
            return "NM3"
        if "OXYGEN" in mat_upper:
            return "MT"
        if "EFFLUENT" in mat_upper:
            return "M3"
        if "WATER" in mat_upper:
            return "M3"
        if "SULPHURIC" in mat_upper or "ACID" in mat_upper:
            return "MT"
        if "CAUSTIC" in mat_upper:
            return "MT"
        if "UREA" in mat_upper:
            return "KG"
        if "MORPHOLENE" in mat_upper:
            return "MT"
        if "SODIUM CHLORIDE" in mat_upper:
            return "MT"
        if "CHEM" in mat_upper:
            return "KG"
        if "FURNACE OIL" in mat_upper:
            return "MMBTU"
        if "SODIUM" in mat_upper:
            return "KG"
        if "POLY" in mat_upper:
            return "KG"
        if "KEM" in mat_upper or "WATREAT" in mat_upper:
            return "KG"
        if "NITROGEN" in mat_upper:
            return "NM3"
        return ""

    def _infer_norm_fmt(self, material: str, norm: float) -> str:
        """Infer the norm format string based on material and norm value."""
        mat_upper = material.upper()
        abs_norm = abs(norm)

        if "NATURAL GAS" in mat_upper:
            return ".4f"
        if 0 < abs_norm < 0.001:
            return ".7f"
        return ".4f"

    # ------------------------------------------------------------------
    # Distribution section
    # ------------------------------------------------------------------

    def _generate_distribution_section(
        self, month_name: str, mv: dict, calc_result: dict,
        ratio_overrides: dict,
    ) -> List[str]:
        """Generate the distribution section (Power_Dis, Steam_Dis).

        Matches the existing service output format:
        Order: HP, LP, MP, Power, SHP
        Uses actual generation values as calc_value (not total * ratio).
        Uses sum of source ref quantities as ref_qty for all lines in a group.
        """
        lines: List[str] = []
        book = self._bpc_book

        def _get_ref(plant: str, utility: str, material: str) -> float:
            if not book:
                return 0.0
            return book.get_quantity(
                month_name, generating_plant=plant, utility=utility, material=material
            ) or 0.0

        def _infer_ref_qty(plant: str, utility: str, **kwargs) -> float:
            if not book:
                return 0.0
            return book.infer_section_ref_qty(month_name, plant, utility, **kwargs) or 0.0

        # --- HP steam distribution (100% from PRDS) ---
        hp_total = mv["hp_total"]
        hp_ref = _get_ref("NMD - Utility/Power Dist", "HP Steam_Dis", "HP Steam PRDS")
        lines.append("")
        lines.append(_line(
            "NMD - Utility/Power Dist", "HP Steam_Dis",
            "HP Steam PRDS", "MT",
            hp_total, hp_ref,
            1.0, hp_total, hp_ref, ".4f",
        ))

        # --- LP steam distribution ---
        lp_prds = mv["lp_prds"]
        stg_lp = mv["stg_lp"]
        lp_total = lp_prds + stg_lp
        lp_prds_ref = _get_ref("NMD - Utility/Power Dist", "LP Steam_Dis", "LP Steam PRDS")
        lp_stg_ref = _get_ref("NMD - Utility/Power Dist", "LP Steam_Dis", "STG1_LP STEAM")
        lp_dis_ref = lp_prds_ref + lp_stg_ref
        lines.append("")
        lines.append(_line(
            "NMD - Utility/Power Dist", "LP Steam_Dis",
            "LP Steam PRDS", "MT",
            lp_total, lp_dis_ref,
            (lp_prds / lp_total) if lp_total else 0, lp_prds, lp_prds_ref, ".4f",
        ))
        lines.append(_line(
            "", "",
            "STG1_LP STEAM", "MT",
            lp_total, lp_dis_ref,
            (stg_lp / lp_total) if lp_total else 0, stg_lp, lp_stg_ref, ".4f",
        ))

        # --- MP steam distribution ---
        mp_prds = mv["mp_prds"]
        stg_mp = mv["stg_mp"]
        mp_total = mp_prds + stg_mp
        mp_prds_ref = _get_ref("NMD - Utility/Power Dist", "MP Steam_Dis", "MP Steam PRDS SHP")
        mp_stg_ref = _get_ref("NMD - Utility/Power Dist", "MP Steam_Dis", "STG1_MP STEAM")
        mp_dis_ref = mp_prds_ref + mp_stg_ref
        lines.append("")
        lines.append(_line(
            "NMD - Utility/Power Dist", "MP Steam_Dis",
            "MP Steam PRDS SHP", "MT",
            mp_total, mp_dis_ref,
            (mp_prds / mp_total) if mp_total else 0, mp_prds, mp_prds_ref, ".4f",
        ))
        lines.append(_line(
            "", "",
            "STG1_MP STEAM", "MT",
            mp_total, mp_dis_ref,
            (stg_mp / mp_total) if mp_total else 0, stg_mp, mp_stg_ref, ".4f",
        ))

        # --- Power distribution ---
        total_demand_kwh = mv["total_demand_kwh"]
        import_power_kwh = mv["import_power_kwh"]
        gt1_ratio = (mv["gt1_net_kwh"] / total_demand_kwh) if total_demand_kwh else 0
        gt2_ratio = (mv["gt2_net_kwh"] / total_demand_kwh) if total_demand_kwh else 0
        gt3_ratio = (mv["gt3_net_kwh"] / total_demand_kwh) if total_demand_kwh else 0
        stg_ratio = (mv["stg_net_kwh"] / total_demand_kwh) if total_demand_kwh else 0
        import_ratio = (import_power_kwh / total_demand_kwh) if total_demand_kwh else 0

        # Ref quantities for power sources
        pp1_ref = _infer_ref_qty("NMD - Power Plant 1", "POWERGEN")
        pp2_ref = _infer_ref_qty("NMD - Power Plant 2", "POWERGEN")
        pp3_ref = _infer_ref_qty("NMD - Power Plant 3", "POWERGEN")
        stg_ref = _infer_ref_qty("NMD - STG Power Plant", "POWERGEN", use_abs_quantity=True)
        mel_ref = _get_ref("NMD - Utility/Power Dist", "Power_Dis", "Power from MEL")
        power_dis_ref = mel_ref + pp1_ref + pp2_ref + pp3_ref + stg_ref

        lines.append("")
        if import_power_kwh > 0:
            lines.append(_line(
                "NMD - Utility/Power Dist", "Power_Dis",
                "Power from MEL", "KWH",
                total_demand_kwh, power_dis_ref,
                import_ratio, import_power_kwh, mel_ref, ".4f",
            ))
            power_plant = ""
            power_utility = ""
        else:
            power_plant = "NMD - Utility/Power Dist"
            power_utility = "Power_Dis"
        lines.append(_line(
            power_plant, power_utility,
            "POWERGEN (PP1)", "KWH",
            total_demand_kwh, power_dis_ref,
            gt1_ratio, mv["gt1_net_kwh"], pp1_ref, ".4f",
        ))
        lines.append(_line(
            "", "",
            "POWERGEN (PP2)", "KWH",
            total_demand_kwh, power_dis_ref,
            gt2_ratio, mv["gt2_net_kwh"], pp2_ref, ".4f",
        ))
        lines.append(_line(
            "", "",
            "POWERGEN (PP3)", "KWH",
            total_demand_kwh, power_dis_ref,
            gt3_ratio, mv["gt3_net_kwh"], pp3_ref, ".4f",
        ))
        lines.append(_line(
            "", "",
            "POWERGEN (STG)", "KWH",
            total_demand_kwh, power_dis_ref,
            stg_ratio, mv["stg_net_kwh"], stg_ref, ".4f",
        ))

        # --- SHP steam distribution ---
        total_shp = mv["hrsg1_shp"] + mv["hrsg2_shp"] + mv["hrsg3_shp"]
        hrsg1_ref = _get_ref("NMD - Utility/Power Dist", "SHP Steam_Dis", "HRSG1_SHP STEAM")
        hrsg2_ref = _get_ref("NMD - Utility/Power Dist", "SHP Steam_Dis", "HRSG2_SHP STEAM")
        hrsg3_ref = _get_ref("NMD - Utility/Power Dist", "SHP Steam_Dis", "HRSG3_SHP STEAM")
        shp_dis_ref = hrsg1_ref + hrsg2_ref + hrsg3_ref
        lines.append("")
        lines.append(_line(
            "NMD - Utility/Power Dist", "SHP Steam_Dis",
            "HRSG1_SHP STEAM", "MT",
            total_shp, shp_dis_ref,
            (mv["hrsg1_shp"] / total_shp) if total_shp else 0, mv["hrsg1_shp"], hrsg1_ref, ".4f",
        ))
        lines.append(_line(
            "", "",
            "HRSG2_SHP STEAM", "MT",
            total_shp, shp_dis_ref,
            (mv["hrsg2_shp"] / total_shp) if total_shp else 0, mv["hrsg2_shp"], hrsg2_ref, ".4f",
        ))
        lines.append(_line(
            "", "",
            "HRSG3_SHP STEAM", "MT",
            total_shp, shp_dis_ref,
            (mv["hrsg3_shp"] / total_shp) if total_shp else 0, mv["hrsg3_shp"], hrsg3_ref, ".4f",
        ))

        return lines

    # ------------------------------------------------------------------
    # Summary
    # ------------------------------------------------------------------

    def _generate_summary(self, cpp_totals: dict, bpc_totals: dict) -> List[str]:
        """Generate summary section."""
        lines: List[str] = []
        lines.append("")
        lines.append("=" * 220)
        lines.append("SUMMARY")
        lines.append("=" * 220)

        total_cpp = sum(cpp_totals.values())
        total_bpc = sum(bpc_totals.values())
        diff = total_cpp - total_bpc

        lines.append(f"  Total Calculated (Model): {total_cpp:>20,.2f}")
        lines.append(f"  Total Reference (BPC):    {total_bpc:>20,.2f}")
        lines.append(f"  Difference:               {diff:>20,.2f}")
        lines.append(f"  % Difference:             {_calc_pct(total_cpp, total_bpc)}")
        lines.append("=" * 220)

        return lines


# ---------------------------------------------------------------------------
# Convenience function
# ---------------------------------------------------------------------------

def generate_nmd_budget_comparison(
    month: int,
    year: int,
    financial_year: int,
    calculation_result: dict,
    bpc_csv_path: Optional[str] = None,
) -> Tuple[str, dict, dict]:
    """
    Generate NMD budget comparison text dynamically.

    Args:
        month, year: Financial period.
        financial_year: Starting year of the financial year.
        calculation_result: Result from NMDIterationLoop.run() or usd_iterate().
        bpc_csv_path: Path to BPC reference file.

    Returns:
        (comparison_text, cpp_totals, bpc_totals)
    """
    reader = NMDNormsReader.get_reader(month, year)
    generator = NMDBudgetGenerator(reader)
    return generator.generate(
        month, year, financial_year, calculation_result, bpc_csv_path
    )
