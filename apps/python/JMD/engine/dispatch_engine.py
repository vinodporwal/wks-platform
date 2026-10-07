"""
JMD Power Dispatch Engine — New Standalone Module
==================================================
Clean, priority-based power dispatch for all JMD CPP plants.

Dispatch Rules:
  1. All available assets start at MIN load simultaneously.
  2. If total MIN >= demand → surplus logged, dispatch stays at MIN.
  3. If total MIN < demand  → ramp up by priority (1 = highest first).
  4. Equal-priority assets share additional load equally (equal MW).
  5. If all at MAX and still deficit → deficit logged.

Usage:
    from engine.dispatch_engine import dispatch_power
    result = dispatch_power(plant_id, month, year)

Designed to be reusable across all 5 CPP plants — parameterised by plant_id.
"""

import csv
import os
import re
import logging
from collections import defaultdict
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from engine.ods_norms_reader import ODSNormsReader

from database.queries import (
    fetch_process_demands,
    fetch_fixed_consumption,
    fetch_plant_power_info,
    fetch_power_asset_capacity_all_months,
    fetch_asset_priority,
    fetch_steam_generation_assets,
    fetch_steam_asset_operational_hours,
    fetch_steam_asset_priority,
    fetch_steam_asset_capacity_all_months,
)
from engine.norms_reader_factory import get_norms_reader
from engine.balancing_integration_v3 import _apply_power_spinning_margin_v2
from engine.dta_stg_calc import (
    calculate_dta_stg_extraction,
    get_default_dta_stg_calc,
    log_stg_extraction,
    select_dta_stg_case,
)
from engine.dta_stg_config import get_dta_stg_config
from engine.sez_stg_calc import calculate_sez_stg_extraction

logger = logging.getLogger(__name__)

# Free steam factor fallback for GT assets without a per-load lookup.
DEFAULT_FREE_STEAM_FACTOR = 1.97

# ---------------------------------------------------------------------------
# GT per-load heat rate & free steam factor lookup
# Can be populated from CPP_GTHeatRate table or use hardcoded fallback
# Format: (GTLOAD_MW, HEAT_RATE_Kcal_kWh, FREESTM_FACTOR)
# ---------------------------------------------------------------------------
_GT_LOAD_LOOKUP = {
    "C2GT1_FRAME9": [
        (50.00, 3413.07, 1.92), (51.00, 3393.07, 1.91), (52.00, 3373.07, 1.91),
        (53.00, 3353.07, 1.90), (54.00, 3333.07, 1.90), (55.00, 3314.07, 1.89),
        (56.00, 3295.07, 1.89), (57.00, 3276.07, 1.88), (58.00, 3257.07, 1.88),
        (59.00, 3239.07, 1.87), (60.00, 3221.07, 1.87), (61.00, 3203.07, 1.86),
        (62.00, 3185.07, 1.86), (63.00, 3168.07, 1.85), (64.00, 3151.07, 1.85),
        (65.00, 3134.07, 1.84), (66.00, 3117.07, 1.84), (67.00, 3101.07, 1.83),
        (68.00, 3085.07, 1.83), (69.00, 3069.07, 1.82), (70.00, 3053.07, 1.82),
        (71.00, 3038.07, 1.81), (72.00, 3023.07, 1.81), (73.00, 3008.07, 1.80),
        (74.00, 2993.07, 1.80), (75.00, 2979.07, 1.79), (76.00, 2966.07, 1.79),
        (77.00, 2954.07, 1.78), (78.00, 2942.07, 1.78), (79.00, 2930.07, 1.77),
        (80.00, 2918.07, 1.77), (81.00, 2908.07, 1.76), (82.00, 2898.07, 1.76),
        (83.00, 2888.07, 1.75), (84.00, 2878.07, 1.75), (85.00, 2868.07, 1.74),
        (86.00, 2858.07, 1.73), (87.00, 2848.07, 1.72), (88.00, 2837.07, 1.71),
        (89.00, 2826.07, 1.70), (90.00, 2815.16, 1.70), (91.00, 2804.25, 1.70),
        (92.00, 2793.25, 1.69), (93.00, 2782.25, 1.68), (94.00, 2771.25, 1.67),
        (95.00, 2760.25, 1.66), (96.00, 2749.34, 1.65), (97.00, 2738.42, 1.64),
        (98.00, 2726.42, 1.63), (99.00, 2714.42, 1.62), (100.00, 2702.52, 1.61),
        (101.00, 2690.62, 1.60), (102.00, 2678.71, 1.60), (103.00, 2666.81, 1.59),
        (104.00, 2654.90, 1.58), (105.00, 2643.00, 1.57), (106.00, 2638.00, 1.56),
        (107.00, 2633.00, 1.55), (108.00, 2629.00, 1.54), (109.00, 2625.00, 1.53),
        (110.00, 2621.00, 1.52), (111.00, 2618.00, 1.51), (112.00, 2615.00, 1.50),
        (113.00, 2612.00, 1.49), (114.00, 2609.00, 1.48),
    ],
    "C2GT2_FRAME9": [
        (50.00, 3430.34, 1.92), (51.00, 3410.50, 1.91), (52.00, 3390.66, 1.91),
        (53.00, 3370.82, 1.90), (54.00, 3350.98, 1.90), (55.00, 3332.14, 1.89),
        (56.00, 3313.29, 1.89), (57.00, 3294.44, 1.88), (58.00, 3275.59, 1.88),
        (59.00, 3257.74, 1.87), (60.00, 3239.88, 1.87), (61.00, 3222.02, 1.86),
        (62.00, 3204.17, 1.86), (63.00, 3187.30, 1.85), (64.00, 3170.44, 1.85),
        (65.00, 3153.58, 1.84), (66.00, 3136.71, 1.84), (67.00, 3120.84, 1.83),
        (68.00, 3104.97, 1.83), (69.00, 3089.10, 1.82), (70.00, 3073.22, 1.82),
        (71.00, 3058.34, 1.81), (72.00, 3043.46, 1.81), (73.00, 3028.58, 1.80),
        (74.00, 3013.70, 1.80), (75.00, 2999.82, 1.79), (76.00, 2985.93, 1.79),
        (77.00, 2972.04, 1.78), (78.00, 2958.15, 1.78), (79.00, 2945.26, 1.77),
        (80.00, 2932.36, 1.77), (81.00, 2920.46, 1.76), (82.00, 2908.55, 1.76),
        (83.00, 2896.65, 1.75), (84.00, 2884.74, 1.75), (85.00, 2872.84, 1.74),
        (86.00, 2860.94, 1.73), (87.00, 2849.03, 1.72), (88.00, 2838.12, 1.71),
        (89.00, 2827.21, 1.70), (90.00, 2816.30, 1.70), (91.00, 2805.38, 1.70),
        (92.00, 2793.48, 1.69), (93.00, 2781.58, 1.68), (94.00, 2769.67, 1.67),
        (95.00, 2757.77, 1.66), (96.00, 2746.86, 1.65), (97.00, 2735.94, 1.64),
        (98.00, 2725.03, 1.63), (99.00, 2714.12, 1.62), (100.00, 2702.22, 1.61),
        (101.00, 2690.31, 1.60), (102.00, 2678.41, 1.60), (103.00, 2666.50, 1.59),
        (104.00, 2654.60, 1.58), (105.00, 2642.70, 1.57), (106.00, 2637.74, 1.56),
        (107.00, 2632.78, 1.55), (108.00, 2628.81, 1.54), (109.00, 2624.84, 1.53),
        (110.00, 2620.87, 1.52), (111.00, 2617.90, 1.51), (112.00, 2614.92, 1.50),
        (113.00, 2611.94, 1.49), (114.00, 2608.97, 1.48),
    ],
}


def build_gt_heat_rate_lookup(gt_heat_rate_df):
    """
    Build GT heat rate lookup dictionary from database DataFrame.
    
    Args:
        gt_heat_rate_df: DataFrame with columns AssetId, GTName, LoadMW, HeatRateKCALKWH, FreeSteamFactor
    
    Returns:
        Dictionary mapping AssetId to list of (LoadMW, HeatRateKCALKWH, FreeSteamFactor) tuples
    """
    if gt_heat_rate_df is None or gt_heat_rate_df.empty:
        logger.warning("  [GT HR] No database data provided, using hardcoded fallback")
        return _GT_LOAD_LOOKUP
    
    lookup = {}
    for asset_id in gt_heat_rate_df['AssetId'].unique():
        gt_data = gt_heat_rate_df[gt_heat_rate_df['AssetId'] == asset_id]
        # Sort by LoadMW to ensure proper interpolation
        gt_data = gt_data.sort_values('LoadMW')
        # Convert to list of tuples
        lookup[asset_id] = [
            (row['LoadMW'], row['HeatRateKCALKWH'], row['FreeSteamFactor'])
            for _, row in gt_data.iterrows()
        ]
    
    logger.info("  [GT HR] Built lookup from database for %d GT assets", len(lookup))
    return lookup


# Map model asset names to the equipment types in the BPC lookup table.
# Extend this as more plant assets are validated.
# Used only as fallback when database lookup fails.
_ASSET_TO_EQUIPMENT_TYPE = {
    "JMD - C2-GTG 1": "C2GT1_FRAME9",
    "JMD - C2-GTG 2": "C2GT2_FRAME9",
}


def _lookup_gt_load_factor(load_mw: float, table: list) -> float:
    """
    Linearly interpolate the free-steam factor for a given GT load (MW).
    table: list of (load_mw, heat_rate, free_steam_factor) sorted by load_mw.
    """
    if not table:
        return DEFAULT_FREE_STEAM_FACTOR
    if load_mw <= table[0][0]:
        return table[0][2]
    if load_mw >= table[-1][0]:
        return table[-1][2]
    for i in range(1, len(table)):
        lo, _, fo = table[i - 1]
        hi, _, fh = table[i]
        if lo <= load_mw <= hi:
            if hi == lo:
                return fo
            return round(fo + (fh - fo) * (load_mw - lo) / (hi - lo), 6)
    return table[-1][2]


def _lookup_gt_heat_rate(load_mw: float, table: list) -> float:
    """
    Linearly interpolate the heat rate (Kcal/kWh) for a given GT load (MW).
    """
    if not table:
        return 0.0
    if load_mw <= table[0][0]:
        return table[0][1]
    if load_mw >= table[-1][0]:
        return table[-1][1]
    for i in range(1, len(table)):
        lo, ho, _ = table[i - 1]
        hi, hh, _ = table[i]
        if lo <= load_mw <= hi:
            if hi == lo:
                return ho
            return round(ho + (hh - ho) * (load_mw - lo) / (hi - lo), 2)
    return table[-1][1]


def _get_asset_equipment_type(asset_name: str) -> str:
    """Return the equipment type key for a model asset name, if known."""
    return _ASSET_TO_EQUIPMENT_TYPE.get(asset_name.strip(), "")


# DTA STG extraction curve — averaged from the four cases in the site Excel.
# Loaded on first use so it does not affect other plants.
_DTA_PLANT_ID = "A4AF8441-73AD-4F9F-BCF4-6734E8202F7A"
_C2_PLANT_ID = "BA558F95-8A3F-4769-9C78-FF7B6C639DDF"
_SEZ_PLANT_ID = "2DFEE33F-4CFD-4887-B9DD-53388AA95271"
_SEZ_PCG_PLANT_ID = "D2C7FBAD-7E00-4642-B3B2-5A768FAC8D45"
_DTA_PCG_PLANT_ID = "F6D82E68-C3B6-494F-9905-48F19DC611E3"

# Plants whose PRDS cascade is branching (one grade lets down to multiple
# lower grades — e.g. SEZ-PCG SHP→{HP,IHP}, DTA-PCG SHP→{HP,IHP,IP}).
# These use the topological children_map calculation instead of the
# linear chain.  Linear-cascade plants (DTA-CPP, C2-CPP, SEZ-CPP) are
# unaffected.
_BRANCHING_CASCADE_PLANTS = {_SEZ_PCG_PLANT_ID, _DTA_PCG_PLANT_ID}

# Plants where every grade's PRDS letdown already reaches the parent grade's
# demand through the U4U path (_calculate_steam_cascade_u4u adds the letdown
# onto the parent _Dis producer's demand).  For these plants the linear
# cascade must NOT add prev_letdown on top of the raw demand, or the same
# letdown flow is counted twice.
_U4U_LETDOWN_FEED_PLANTS = {_DTA_PLANT_ID, _C2_PLANT_ID}
_DTA_STG_CSV = os.path.join(os.path.dirname(__file__), "..", "data", "dta_stg_extraction.csv")
_DTA_STG_LOOKUP: list = []


def _load_dta_stg_lookup() -> list:
    """Load the DTA STG extraction curve from the CSV and sort by LoadMW."""
    global _DTA_STG_LOOKUP
    if _DTA_STG_LOOKUP:
        return _DTA_STG_LOOKUP
    try:
        with open(_DTA_STG_CSV, newline="") as f:
            reader = csv.DictReader(f)
            _DTA_STG_LOOKUP = sorted(
                [{k: float(v) for k, v in row.items() if v != ""} for row in reader],
                key=lambda r: r["LoadMW"],
            )
    except (FileNotFoundError, ValueError, KeyError):
        _DTA_STG_LOOKUP = []
    return _DTA_STG_LOOKUP


def _stg_num_from_name(asset_name: str) -> int:
    """Extract STG number from asset names like 'JMD - SGT Plant 5'."""
    m = re.search(r"(?:STG|SGT)[^0-9]*(\d+)", asset_name, re.IGNORECASE)
    return int(m.group(1)) if m else 0


def _get_dta_stg_extraction_requirement(plant_id: str, month: int, year: int) -> dict:
    """
    Required HP/MP extraction per DTA STG for the month, from the ODS
    (BPC) material quantities.

    The ODS carries the planned/actual extraction as material flows into the
    steam headers:  "HP Steam_Dis" consumes "STG5_HP STEAM" (97,200 MT →
    135 TPH for April), "MP Steam_Dis" consumes "STG5_MP STEAM", etc.

    Returns {stg_num: {"hp_mt": float, "mp_mt": float}}.  Empty dict when the
    ODS baseline is unavailable or no STG materials exist — callers then fall
    back to the fixed max-extraction operating point.
    """
    from engine.ods_norms_reader import ODSNormsReader

    try:
        reader = ODSNormsReader.get_reader(plant_id, month, year)
        bpc = reader.get_bpc_quantities() if reader and reader.is_available else {}
    except Exception as e:
        logger.warning("  [DTA STG] Could not load ODS extraction requirements: %s", e)
        return {}

    req: dict = {}
    for dis_key in ("HP Steam_Dis", "HP Steam Dis"):
        for material, qty in (bpc.get(dis_key) or {}).items():
            m = re.match(r"STG(\d+)_HP STEAM", str(material).strip(), re.IGNORECASE)
            if m:
                req.setdefault(int(m.group(1)), {"hp_mt": 0.0, "mp_mt": 0.0})
                req[int(m.group(1))]["hp_mt"] = float(qty or 0.0)
    for dis_key in ("MP Steam_Dis", "MP Steam Dis"):
        for material, qty in (bpc.get(dis_key) or {}).items():
            m = re.match(r"STG(\d+)_MP STEAM", str(material).strip(), re.IGNORECASE)
            if m:
                req.setdefault(int(m.group(1)), {"hp_mt": 0.0, "mp_mt": 0.0})
                req[int(m.group(1))]["mp_mt"] = float(qty or 0.0)
    return req


def _interpolate_dta_stg(load_mw: float) -> dict:
    """Interpolate the DTA STG extraction curve for a given per-unit load."""
    table = _load_dta_stg_lookup()
    if not table or load_mw <= 0:
        return {
            "shp_inlet_tph": 0.0, "hp_extraction_tph": 0.0,
            "mp_extraction_tph": 0.0, "lp_extraction_tph": 0.0,
            "condensate_tph": 0.0, "free_steam_factor": 0.0,
        }
    min_load = table[0]["LoadMW"]
    max_load = table[-1]["LoadMW"]
    if load_mw <= min_load:
        row = table[0]
    elif load_mw >= max_load:
        row = table[-1]
    else:
        lower = next(r for r in table if r["LoadMW"] <= load_mw)
        upper = next(r for r in table if r["LoadMW"] >= load_mw)
        if upper == lower:
            row = lower
        else:
            f = (load_mw - lower["LoadMW"]) / (upper["LoadMW"] - lower["LoadMW"])
            def _val(key):
                return lower[key] + f * (upper[key] - lower[key])
            row = {k: _val(k) for k in lower}
    return {
        "shp_inlet_tph": row.get("SHPInletTPH", 0.0),
        "hp_extraction_tph": row.get("HPExtractionTPH", 0.0),
        "mp_extraction_tph": row.get("MPExtractionTPH", 0.0),
        "lp_extraction_tph": row.get("LPExtractionTPH", 0.0),
        "condensate_tph": row.get("CondensateM3hr", 0.0),
        "free_steam_factor": row.get("FreeSteamFactor", 0.0),
    }


def _stg_extraction_totals_from_power_result(power_result: dict) -> dict:
    """Sum STG extraction across all dispatched STG assets."""
    totals = {
        "shp_consumption_mt": 0.0,
        "hp_consumption_mt": 0.0,
        "hp_for_power_mt": 0.0,
        "hp_for_mp_consumption_mt": 0.0,
        "hp_extraction_mt": 0.0,
        "mp_extraction_mt": 0.0,
        "lp_extraction_mt": 0.0,
    }
    for d in (power_result or {}).get("assets", []):
        if "STG" in d.get("asset_type", "").upper():
            totals["shp_consumption_mt"] += d.get("stg_shp_consumption_mt", 0.0)
            totals["hp_consumption_mt"] += d.get("stg_hp_consumption_mt", 0.0)
            totals["hp_for_power_mt"] += d.get("stg_hp_for_power_mt", 0.0)
            totals["hp_for_mp_consumption_mt"] += d.get("stg_hp_for_mp_consumption_mt", 0.0)
            totals["hp_extraction_mt"] += d.get("stg_hp_extraction_mt", 0.0)
            totals["mp_extraction_mt"] += d.get("stg_mp_extraction_mt", 0.0)
            totals["lp_extraction_mt"] += d.get("stg_lp_extraction_mt", 0.0)
    return totals


# ---------------------------------------------------------------------------
# Steam Spinning Margin (TPH) — per plant
# Hardcoded for now; will be replaced by DB table lookup per plant.
# After dispatch, total headroom (orig_max_tph - dispatched_tph) across all
# running assets must be >= this value.
# ---------------------------------------------------------------------------
STEAM_SPINNING_MARGIN_TPH = {
    "F6D82E68-C3B6-494F-9905-48F19DC611E3": 0.0,  # DTA-PCG-CPP
    "2DFEE33F-4CFD-4887-B9DD-53388AA95271": 0.0,  # SEZ-CPP
    "D2C7FBAD-7E00-4642-B3B2-5A768FAC8D45": 0.0,  # SEZ-PCG-CPP
    "A4AF8441-73AD-4F9F-BCF4-6734E8202F7A": 0.0,  # DTA-CPP
    "BA558F95-8A3F-4769-9C78-FF7B6C639DDF": 250.0,  # C2-CPP
}

# ---------------------------------------------------------------------------
# Power Spinning Margin — per plant enablement
# True = reserve capacity equal to most efficient asset's fixed_max
# False = no spinning margin (default)
# ---------------------------------------------------------------------------
POWER_SPINNING_MARGIN_ENABLED = {
    "F6D82E68-C3B6-494F-9905-48F19DC611E3": False,  # DTA-PCG-CPP
    "2DFEE33F-4CFD-4887-B9DD-53388AA95271": False,  # SEZ-CPP
    "D2C7FBAD-7E00-4642-B3B2-5A768FAC8D45": False,  # SEZ-PCG-CPP
    "A4AF8441-73AD-4F9F-BCF4-6734E8202F7A": False,  # DTA-CPP
    "BA558F95-8A3F-4769-9C78-FF7B6C639DDF": False,   # C2-CPP
}

_MONTH_NAMES = {
    1: "January", 2: "February", 3: "March", 4: "April",
    5: "May",     6: "June",     7: "July",   8: "August",
    9: "September", 10: "October", 11: "November", 12: "December",
}


def _fy_year(month: int, year: int) -> int:
    """Return the FY start year (e.g., April 2026 → 2026, Jan 2027 → 2026)."""
    return year if month >= 4 else year - 1


def _month_key(month: int) -> str:
    """Return 3-letter month abbreviation for DB column lookup."""
    return _MONTH_NAMES[month][:3]


def _get_steam_demands(plant_id: str, month: int, year: int, demands: dict = None,
                       grade_prefixes: list = None) -> dict:
    """
    Build steam demand dict from pre-fetched demands or DB fallback.

    Args:
        plant_id:       CPP plant UUID
        month:          1-12
        year:           calendar year
        demands:        pre-fetched merged demands dict from calculator.
        grade_prefixes: list of lowercase steam grade prefixes in cascade order
                        (e.g. ['lp', 'mp', 'hp', 'shp']).  Derived from the ODS
                        letdown cascade so no grades are hardcoded here.
    """
    if grade_prefixes is None:
        grade_prefixes = ["lp", "mp", "hp", "shp"]  # safe fallback

    if demands is not None:
        result = {}
        for g in grade_prefixes:
            # Derive the ODS _Dis material name from the grade prefix
            # e.g. "lp" → "LP Steam_Dis",  "shp" → "SHP Steam_Dis"
            ods_key = f"{g.upper()} Steam_Dis"
            # Some plants use " Dis" (space) instead of "_Dis" (underscore)
            # e.g. "IHP Steam Dis", "LLP Steam Dis"
            ods_key_alt = f"{g.upper()} Steam Dis"
            for kind in ("process", "fixed"):
                prefix_key = f"{g}_{kind}"
                # Prefer ODS material key (new format); fall back to prefix key (old)
                if kind == "process":
                    val = float(demands.get(ods_key, demands.get(ods_key_alt, demands.get(prefix_key, 0.0))))
                else:
                    val = float(demands.get(prefix_key, 0.0))
                result[prefix_key] = val
        return result

    process = fetch_process_demands(plant_id, month, year)
    fixed = fetch_fixed_consumption(plant_id, month, year)
    return {
        **{f"{g}_process": float(process.get(f"{g}_process", 0.0)) for g in grade_prefixes},
        **{f"{g}_fixed":   float(fixed.get(f"{g}_fixed", 0.0))   for g in grade_prefixes},
    }


# ---------------------------------------------------------------------------
# Core dispatch algorithm
# ---------------------------------------------------------------------------

def _build_asset_table(plant_id: str, month: int, year: int) -> list:
    """
    Build a unified asset table by joining power info, capacity, and priority.

    Returns list of dicts, one per available asset:
        {
            "asset_id", "asset_name", "asset_type",
            "op_hours", "min_mw", "max_mw", "priority",
            "min_mwh", "max_mwh",
        }
    """
    fy_start = _fy_year(month, year)
    mk = _month_key(month)

    # Fetch raw data from DB
    power_info = fetch_plant_power_info(plant_id, month, year)
    assets = power_info.get("assets", [])
    hours_list = power_info.get("hours", [])
    cap_list = fetch_power_asset_capacity_all_months(plant_id, fy_start)
    pri_list = fetch_asset_priority(plant_id, month, year)

    # Index by asset_id for fast lookup
    hours_by_id = {h["asset_id"]: h for h in hours_list}
    caps_by_id = {c["asset_id"]: c for c in cap_list}
    pri_by_id = {p["asset_id"]: p for p in pri_list}

    # DTA STG: operating case is selected per unit from the month's required
    # HP/MP extraction (ODS BPC quantities).  A unit with no extraction
    # requirement falls back to the legacy max-extraction point.
    dta_stg_req = {}
    dta_stg_mw = None
    if plant_id == _DTA_PLANT_ID:
        dta_stg_req = _get_dta_stg_extraction_requirement(plant_id, month, year)
        dta_stg_mw = get_default_dta_stg_calc()["mw"]

    result = []
    for asset in assets:
        aid = asset["asset_id"]
        aname = asset.get("asset_name", "")
        atype = asset.get("asset_type", "")

        # Operational hours
        hour_row = hours_by_id.get(aid, {})
        op_hours = hour_row.get("operational_hours")
        if op_hours is None or float(op_hours) <= 0:
            continue  # Asset not available this month

        op_hours = float(op_hours)

        # Capacity (min/max MW)
        cap_row = caps_by_id.get(aid, {})
        min_mw = cap_row.get(f"{mk}_Min")
        if min_mw is None:
            min_mw = cap_row.get("fixed_min")
        if min_mw is None:
            min_mw = 0.0
        min_mw = float(min_mw)

        max_mw = cap_row.get(f"{mk}_Max")
        if max_mw is None:
            max_mw = cap_row.get("fixed_max")
        if max_mw is None:
            max_mw = 0.0
        max_mw = float(max_mw)

        # Mandatory load flag (1 = must dispatch at MIN, 0 = optional)
        mandatory = int(cap_row.get(f"{mk}_Man_Load", 0) or 0)

        # Priority
        pri_row = pri_by_id.get(aid, {})
        priority = pri_row.get("priority")
        if priority is None:
            priority = 999  # unset priority → lowest
        priority = int(priority)

        fixed_max_mw = float(cap_row.get("fixed_max", 0) or 0)

        # DTA STG: an extraction STG's MW output is determined by its steam
        # duty, not the DB nameplate — select the AOP operating case that
        # covers the required HP/MP extraction and pin the unit at the case
        # MW (it is a steam-led machine: delivering the required extraction
        # produces the case's stated power).
        dta_stg_case = None
        dta_stg_req_hp_tph = 0.0
        dta_stg_req_mp_tph = 0.0
        if plant_id == _DTA_PLANT_ID and atype == "STG":
            stg_num = _stg_num_from_name(aname)
            req = dta_stg_req.get(stg_num)
            if req and (req["hp_mt"] > 0 or req["mp_mt"] > 0):
                dta_stg_req_hp_tph = req["hp_mt"] / op_hours
                dta_stg_req_mp_tph = req["mp_mt"] / op_hours
                dta_stg_case = select_dta_stg_case(dta_stg_req_hp_tph, dta_stg_req_mp_tph)
            if dta_stg_case:
                if abs(max_mw - dta_stg_case["mw"]) / max(max_mw, 1e-9) > 0.05:
                    logger.warning(
                        "  [DTA STG] %s: DB max %.2f MW differs from selected Case %s "
                        "output %.2f MW (req HP %.1f / MP %.1f TPH) — verify DB capacity",
                        aname, max_mw, dta_stg_case["case_id"], dta_stg_case["mw"],
                        dta_stg_req_hp_tph, dta_stg_req_mp_tph,
                    )
                min_mw = dta_stg_case["mw"]
                max_mw = dta_stg_case["mw"]
                mandatory = 1
            elif dta_stg_mw is not None:
                max_mw = dta_stg_mw

        max_mwh = max_mw * op_hours

        result.append({
            "asset_id": aid,
            "asset_name": aname,
            "asset_type": atype,
            "plant_code": asset.get("plant_code", ""),
            "op_hours": op_hours,
            "min_mw": min_mw,
            "max_mw": max_mw,
            "fixed_max_mw": fixed_max_mw,
            "priority": priority,
            "mandatory": mandatory,
            "min_mwh": min_mw * op_hours,
            "max_mwh": max_mwh,
            "dta_stg_case": dta_stg_case,
            "dta_stg_req_hp_tph": dta_stg_req_hp_tph,
            "dta_stg_req_mp_tph": dta_stg_req_mp_tph,
        })

    return result


def _get_power_demand(plant_id: str, month: int, year: int, demands: dict = None) -> dict:
    """
    Fetch and compute total power demand (process + fixed) in MWh.

    Args:
        plant_id:  CPP plant UUID
        month:     1-12
        year:      calendar year
        demands:   pre-fetched merged demands dict from calculator. If provided,
                   skips DB round-trip for process + fixed consumption.

    Returns:
        {
            "process_mwh": float,  # process demand (kWh ÷ 1000)
            "fixed_mwh": float,    # fixed demand (already MWh)
            "total_mwh": float,    # sum
        }
    """
    if demands is not None:
        if "_power_process_mwh" in demands:
            # Preferred: explicit split stored by _build_dispatch_demands (already MWh)
            process_mwh = float(demands["_power_process_mwh"])
            fixed_mwh   = float(demands.get("_power_fixed_mwh", 0.0))
        elif "Power_Dis" in demands:
            # ODS-keyed combined value (process+fixed merged) — split as best-effort
            total_mwh = float(demands["Power_Dis"])
            fixed_mwh = 0.0
            process_mwh = total_mwh
        else:
            # Legacy prefix-keyed format
            process_kwh = float(demands.get("power_process", 0.0))
            fixed_mwh   = float(demands.get("power_fixed", 0.0))
            process_mwh = process_kwh / 1000.0
    else:
        process = fetch_process_demands(plant_id, month, year)
        fixed = fetch_fixed_consumption(plant_id, month, year)
        process_kwh = float(process.get("power_process", 0.0))
        fixed_mwh   = float(fixed.get("power_fixed", 0.0))
        process_mwh = process_kwh / 1000.0

    return {
        "process_mwh": round(process_mwh, 2),
        "fixed_mwh": round(fixed_mwh, 2),
        "total_mwh": round(process_mwh + fixed_mwh, 2),
    }


def _min_first_allocation(dispatch: list, demand_mwh: float) -> float:
    """Min-load-first + priority ramp allocation shared by per-CPP dispatch
    and the JMD-wide power pool.

    Dispatch algorithm (Option A with mandatory load):
      1. Mandatory assets (Man_Load=1) start at MIN load.
      2. If mandatory MIN < demand → bring in optional assets at MIN.
      3. If all MIN < demand → ramp up by priority (1 = highest first).
      4. Equal-priority assets share additional load equally (equal MW).
      5. If all at MAX and still deficit → caller reports the deficit.

    Mutates dispatched_mw/dispatched_mwh on each asset dict in `dispatch`.
    Returns total generation dispatched (MWh).
    """
    # Step 1: Mandatory assets at MIN
    total_gen = 0.0
    for d in dispatch:
        if d["mandatory"] == 1:
            d["dispatched_mw"] = d["min_mw"]
            d["dispatched_mwh"] = d["min_mwh"]
            total_gen += d["dispatched_mwh"]

    remaining = max(0.0, demand_mwh - total_gen)

    # Step 2: If mandatory MIN not enough, bring in optional assets at MIN
    if remaining > 0:
        for d in dispatch:
            if d["mandatory"] == 0:
                d["dispatched_mw"] = d["min_mw"]
                d["dispatched_mwh"] = d["min_mwh"]
                total_gen += d["dispatched_mwh"]
        remaining = max(0.0, demand_mwh - total_gen)

    # Step 3: Ramp up by priority across all running assets
    if remaining > 0:
        priority_groups = defaultdict(list)
        for i, d in enumerate(dispatch):
            if d["dispatched_mwh"] > 0 or d["mandatory"] == 1:
                priority_groups[d["priority"]].append(i)

        # Ramp up in priority order (ascending = highest priority first)
        for pri in sorted(priority_groups.keys()):
            if remaining <= 0:
                break

            group_indices = priority_groups[pri]

            # Calculate headroom per asset (how much MW each can still add)
            group_headroom = []
            for idx in group_indices:
                d = dispatch[idx]
                headroom_mw = d["max_mw"] - d["dispatched_mw"]
                if headroom_mw > 0.001:
                    group_headroom.append((idx, headroom_mw, d["op_hours"]))

            if not group_headroom:
                continue

            # Total MWh headroom in this group
            total_group_headroom_mwh = sum(h_mw * hrs for _, h_mw, hrs in group_headroom)
            allocation_mwh = min(remaining, total_group_headroom_mwh)

            if len(group_headroom) == 1:
                # Single asset — give it everything
                idx, headroom_mw, hrs = group_headroom[0]
                add_mw = allocation_mwh / hrs
                add_mw = min(add_mw, headroom_mw)
                dispatch[idx]["dispatched_mw"] += add_mw
                dispatch[idx]["dispatched_mwh"] = dispatch[idx]["dispatched_mw"] * hrs
                remaining -= add_mw * hrs
            else:
                # Multiple assets with same priority → equal MW dispatch
                # Iteratively allocate: target same MW, cap those that hit max
                uncapped = [(idx, h_mw, hrs) for idx, h_mw, hrs in group_headroom]
                remaining_to_allocate = allocation_mwh

                while uncapped and remaining_to_allocate > 0.001:
                    # Target equal MW across all uncapped assets
                    total_hrs_uncapped = sum(hrs for _, _, hrs in uncapped)
                    if total_hrs_uncapped <= 0:
                        break
                    target_additional_mw = remaining_to_allocate / total_hrs_uncapped

                    newly_capped = []
                    for idx, headroom_mw, hrs in uncapped:
                        if target_additional_mw >= headroom_mw:
                            # This asset hits its max
                            dispatch[idx]["dispatched_mw"] += headroom_mw
                            dispatch[idx]["dispatched_mwh"] = dispatch[idx]["dispatched_mw"] * hrs
                            remaining_to_allocate -= headroom_mw * hrs
                            newly_capped.append(idx)
                        else:
                            # This asset can take the target MW
                            dispatch[idx]["dispatched_mw"] += target_additional_mw
                            dispatch[idx]["dispatched_mwh"] = dispatch[idx]["dispatched_mw"] * hrs
                            remaining_to_allocate -= target_additional_mw * hrs

                    if newly_capped:
                        uncapped = [(i, h, hrs) for i, h, hrs in uncapped if i not in newly_capped]
                    else:
                        break  # All allocated

                remaining -= (allocation_mwh - remaining_to_allocate)

    # Recompute from dispatched_mwh — step 3 ramps assets without updating
    # the running total, so the caller must see the real dispatched sum.
    return sum(d["dispatched_mwh"] for d in dispatch)


def _enrich_power_dispatch(dispatch: list, excel_norms: dict, gt_lookup: dict) -> None:
    """Populate derived per-asset fields after min-first allocation.

    Computes load_percent, avg_load_mw, free steam (GT assets), heat rate,
    and auxiliary power consumption using the owning plant's POWERGEN norms
    and GT heat-rate lookup.  Shared by per-CPP dispatch and the JMD pool —
    the caller must pass the norms/lookup of the plant that owns the assets.
    """
    for d in dispatch:
        d["load_percent"] = round(
            (d["dispatched_mw"] / d["max_mw"] * 100) if d["max_mw"] > 0 else 0.0, 1
        )
        d["dispatched_mw"] = round(d["dispatched_mw"], 4)
        d["dispatched_mwh"] = round(d["dispatched_mwh"], 2)

        # Avg Load MW = dispatched MWh / operational hours
        d["avg_load_mw"] = round(
            d["dispatched_mwh"] / d["op_hours"] if d["op_hours"] > 0 else 0.0, 2
        )

        # Free Steam MT — only for GT assets
        # Use per-load factor from database lookup when available; otherwise fallback.
        is_gt = "GT" in d["asset_type"].upper()
        if is_gt and d["dispatched_mwh"] > 0:
            # Try database lookup by asset_id first
            table = None
            if gt_lookup:
                table = gt_lookup.get(d["asset_id"])

            # Fallback to hardcoded lookup if database lookup fails
            if not table:
                equip_type = _get_asset_equipment_type(d["asset_name"])
                table = _GT_LOAD_LOOKUP.get(equip_type)

            if table:
                d["heat_rate"] = _lookup_gt_heat_rate(d["avg_load_mw"], table)
                d["free_steam_factor"] = _lookup_gt_load_factor(d["avg_load_mw"], table)
                d["free_steam_mt"] = round(d["dispatched_mwh"] * d["free_steam_factor"], 2)
            else:
                d["heat_rate"] = 0.0
                d["free_steam_factor"] = DEFAULT_FREE_STEAM_FACTOR
                d["free_steam_mt"] = round(d["dispatched_mwh"] * DEFAULT_FREE_STEAM_FACTOR, 2)
                if equip_type:
                    logger.warning("  [DISPATCH] No free-steam lookup table for equipment type '%s' on asset '%s'; using default %.2f",
                                   equip_type, d["asset_name"], DEFAULT_FREE_STEAM_FACTOR)
        else:
            d["heat_rate"] = 0.0
            d["free_steam_factor"] = 0.0
            d["free_steam_mt"] = 0.0

        # Aux power norm and aux power consumed
        norm_val = excel_norms.get(d["asset_name"].upper())
        if norm_val is None:
            norm_val = next(
                (v for k, v in excel_norms.items() if k in d["asset_name"].upper() or d["asset_name"].upper() in k),
                None
            )
        if norm_val is None:
            logger.warning("  [DISPATCH] No aux power norm in ODS for asset '%s'; using 0.0", d["asset_name"])
            norm_val = 0.0

        d["aux_power_norm"] = norm_val
        d["aux_power"] = round(d["dispatched_mwh"] * norm_val, 2)


def _dispatch_all_min_first(
    assets: list,
    demand_mwh: float,
    plant_id: str,
    month: int,
    year: int,
    ods_reader = None,
    gt_lookup: dict = None,
) -> list:
    """
    Dispatch algorithm (Option A with mandatory load):
      1. Mandatory assets (Man_Load=1) start at MIN load.
      2. If mandatory MIN < demand → bring in optional assets at MIN.
      3. If all MIN < demand → ramp up by priority (1 = highest first).
      4. Equal-priority assets share additional load equally (equal MW).
      5. If all at MAX and still deficit → deficit logged.

    Args:
        assets: list of asset dicts from _build_asset_table
        demand_mwh: total power demand in MWh
        plant_id: CPP plant UUID
        month: selected month
        year: selected year
        ods_reader: pre-loaded ODSNormsReader for norms (avoids re-reading ODS)
        gt_lookup: GT heat rate lookup dict from database or fallback

    Returns:
        list of asset dicts with added keys:
            "dispatched_mw", "dispatched_mwh", "load_percent"
    """
    # Work on copies — start everyone at 0
    dispatch = []
    for a in assets:
        dispatch.append({
            **a,
            "dispatched_mw": 0.0,
            "dispatched_mwh": 0.0,
        })

    _min_first_allocation(dispatch, demand_mwh)

    # Fetch POWERGEN norms from norms reader (or fallback to factory)
    if ods_reader is not None:
        excel_norms = ods_reader.get_powergen_norms()
    else:
        excel_norms = get_norms_reader(plant_id, month, year).get_powergen_norms()

    # Calculate derived fields
    _enrich_power_dispatch(dispatch, excel_norms, gt_lookup)

    return dispatch


def _dispatch_one_by_one(
    assets: list,
    demand_mwh: float,
    plant_id: str,
    month: int,
    year: int,
    ods_reader = None,
    gt_lookup: dict = None,
) -> list:
    """
    Dispatch algorithm: bring assets online one-by-one by priority.
    Only activate the next asset if the current one at MAX still can't meet demand.

    (PLACEHOLDER — to be implemented once confirmed by senior management)
    """
    # For now, fall back to all_min_first
    logger.warning("  [DISPATCH] one_by_one mode not yet implemented — falling back to all_min_first")
    return _dispatch_all_min_first(assets, demand_mwh, plant_id, month, year, ods_reader=ods_reader, gt_lookup=gt_lookup)


# ---------------------------------------------------------------------------
# Logging helpers
# ---------------------------------------------------------------------------

def _log_dispatch_result(demand: dict, dispatch: list, month: int, year: int, spinning_margin: float = 0.0):
    """Print the full dispatch result to the log in a structured table format."""
    total_demand = demand["total_mwh"]
    total_gen = sum(d["dispatched_mwh"] for d in dispatch)
    surplus = max(0.0, total_gen - total_demand)
    deficit = max(0.0, total_demand - total_gen)

    sep = "=" * 78
    logger.info("  %s", sep)
    logger.info("  POWER DISPATCH  (%s %d)", _MONTH_NAMES.get(month, ""), year)
    logger.info("  %s", sep)
    logger.info("  Demand:  Process = %.2f MWh  |  Fixed = %.2f MWh  |  Total = %.2f MWh",
                demand["process_mwh"], demand["fixed_mwh"], total_demand)
    logger.info("")

    # Step 1: MIN load table
    logger.info("  Step 1: Mandatory assets at MIN load, then optional if needed")
    logger.info("  %-25s  %8s  %4s  %8s  %8s  %8s  %12s",
                "Asset", "Priority", "Man", "Min MW", "Max MW", "Hours", "Min MWh")
    logger.info("  %s  %s  %s  %s  %s  %s  %s",
                "-" * 25, "-" * 8, "-" * 4, "-" * 8, "-" * 8, "-" * 8, "-" * 12)
    total_min_mwh = 0.0
    for d in dispatch:
        total_min_mwh += d["min_mwh"]
        man_flag = "Y" if d.get("mandatory", 0) == 1 else "-"
        logger.info("  %-25s  %8d  %4s  %8.2f  %8.2f  %8.2f  %12.2f",
                     d["asset_name"], d["priority"], man_flag,
                     d["min_mw"], d["max_mw"], d["op_hours"], d["min_mwh"])
    logger.info("  %-25s  %8s  %4s  %8s  %8s  %8s  %12.2f", "TOTAL MIN GENERATION", "", "", "", "", "", total_min_mwh)
    logger.info("")

    # Surplus / Deficit warning
    if surplus > 0:
        logger.info("  ⚠ SURPLUS: MIN generation (%.2f MWh) EXCEEDS demand (%.2f MWh)", total_gen, total_demand)
        logger.info("    Surplus = %.2f MWh  (will be handled via export power in future)", surplus)
        logger.info("")
    elif deficit > 0:
        logger.info("  ⚠ DEFICIT: MAX generation (%.2f MWh) BELOW demand (%.2f MWh)", total_gen, total_demand)
        logger.info("    Deficit = %.2f MWh", deficit)
        logger.info("")
    else:
        logger.info("  ✓ Demand met exactly or within tolerance")
        logger.info("")

    # Step 2: Final dispatch table
    total_free_steam = sum(d.get("free_steam_mt", 0) for d in dispatch)
    total_aux_power = sum(d.get("aux_power", 0) for d in dispatch)
    logger.info("  FINAL DISPATCH")
    logger.info("  %-25s  %8s  %14s  %14s  %8s  %12s  %12s  %14s  %14s  %14s",
                "Asset", "Priority", "Dispatched MW", "Dispatched MWh", "Load %", "Avg Load MW", "FreeStm Factor", "Free Steam MT", "Heat Rate", "Aux Power MWh")
    logger.info("  %s  %s  %s  %s  %s  %s  %s  %s  %s  %s",
                "-" * 25, "-" * 8, "-" * 14, "-" * 14, "-" * 8, "-" * 12, "-" * 12, "-" * 14, "-" * 14, "-" * 14)
    for d in dispatch:
        logger.info("  %-25s  %8d  %14.2f  %14.2f  %7.1f%%  %12.2f  %12.4f  %14.2f  %14.2f  %14.2f",
                     d["asset_name"], d["priority"],
                     d["dispatched_mw"], d["dispatched_mwh"], d["load_percent"],
                     d.get("avg_load_mw", 0),
                     d.get("free_steam_factor", 0.0),
                     d.get("free_steam_mt", 0),
                     d.get("heat_rate", 0.0),
                     d.get("aux_power", 0.0))
    logger.info("  %-25s  %8s  %14s  %14.2f  %8s  %12s  %12s  %14.2f  %14s  %14.2f",
                "TOTAL", "", "", total_gen, "", "", "", total_free_steam, "", total_aux_power)
    # Spinning margin post-dispatch verification
    if spinning_margin > 0:
        working = [d for d in dispatch if d["op_hours"] > 0]
        total_headroom = sum(d.get("orig_max_mw", d["max_mw"]) - d["dispatched_mw"] for d in working)
        if total_headroom >= spinning_margin:
            logger.info("")
            logger.info("  ✓ POWER SPINNING MARGIN OK: %.2f MW reserved (required: %.2f MW)", total_headroom, spinning_margin)
        else:
            logger.warning("")
            logger.warning("  " + "!" * 76)
            logger.warning("  !  *** POWER SPINNING MARGIN VIOLATION ***")
            logger.warning("  !  Required: %.2f MW  |  Available headroom: %.2f MW  |  Shortfall: %.2f MW", spinning_margin, total_headroom, spinning_margin - total_headroom)
            logger.warning("  !  Assets are dispatched beyond effective max — margin NOT maintained!")
            logger.warning("  " + "!" * 76)
    logger.info("  %s", sep)


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

def _apply_stg_derived_fields(plant_id: str, dispatch: list) -> dict:
    """Populate per-STG extraction/consumption fields and return totals.

    DTA uses the AOP case selected per unit from its required HP/MP
    extraction (units without a requirement keep the max-extraction point);
    SEZ uses the per-load curve (load_mw → tph).  Other plants are untouched.

    Mutates per-asset keys on `dispatch` (stg_* fields) and returns:
        {
            "shp_consumption_mt", "hp_consumption_mt", "hp_for_power_mt",
            "hp_for_mp_consumption_mt", "hp_extraction_mt",
            "mp_extraction_mt", "lp_extraction_mt", "condensate_mt",
        }
    """
    totals = {
        "shp_consumption_mt": 0.0,
        "hp_consumption_mt": 0.0,
        "hp_for_power_mt": 0.0,
        "hp_for_mp_consumption_mt": 0.0,
        "hp_extraction_mt": 0.0,
        "mp_extraction_mt": 0.0,
        "lp_extraction_mt": 0.0,
        "condensate_mt": 0.0,
    }

    # DTA STG extraction: each running STG books the month's required HP/MP
    # extraction (ODS BPC quantities) under the AOP operating case selected in
    # _build_asset_table.  Units without an extraction requirement keep the
    # legacy max-extraction operating point.
    if plant_id == _DTA_PLANT_ID:
        dta_stg_calc = get_default_dta_stg_calc()
        for d in dispatch:
            if "STG" not in d.get("asset_type", "").upper():
                continue
            hours = d.get("op_hours", 0.0)
            mwh = d.get("dispatched_mwh", 0.0)
            if hours <= 0 or mwh <= 0:
                d["stg_shp_consumption_mt"] = 0.0
                d["stg_hp_extraction_mt"] = 0.0
                d["stg_mp_extraction_mt"] = 0.0
                d["stg_lp_extraction_mt"] = 0.0
                d["stg_condensate_mt"] = 0.0
                continue
            case = d.get("dta_stg_case")
            if case:
                hp_tph = min(d.get("dta_stg_req_hp_tph", 0.0), case["hp_extraction_tph"])
                mp_tph = min(d.get("dta_stg_req_mp_tph", 0.0), case["mp_extraction_tph"])
                cond_tph = case["condensate_tph"]
                hp_f = case["hp_to_hhp_factor"]
                mp_f = case["mp_to_hhp_factor"]
                ext = {
                    "shp_inlet_tph": hp_tph + mp_tph + cond_tph,
                    "hp_extraction_tph": hp_tph,
                    "mp_extraction_tph": mp_tph,
                    "condensate_tph": cond_tph,
                    "hp_hhp_equivalent_tph": hp_tph * hp_f,
                    "mp_hhp_equivalent_tph": mp_tph * mp_f,
                    "net_hhp_tph": case["net_hhp_tph"],
                    "ssc_kg_kwh": case["ssc_kg_kwh"],
                    "mw": case["mw"],
                    "heat_rate_kcal_kwh": case["heat_rate_kcal_kwh"],
                    "case_id": case["case_id"],
                }
                d["stg_case_id"] = case["case_id"]
            else:
                ext = dta_stg_calc
            stg_num = _stg_num_from_name(d["asset_name"])
            d["stg_shp_inlet_tph"] = round(ext["shp_inlet_tph"], 4)
            d["stg_hp_extraction_tph"] = round(ext["hp_extraction_tph"], 4)
            d["stg_mp_extraction_tph"] = round(ext["mp_extraction_tph"], 4)
            d["stg_lp_extraction_tph"] = round(ext.get("lp_extraction_tph", 0.0), 4)
            d["stg_condensate_tph"] = round(ext["condensate_tph"], 4)
            d["stg_shp_consumption_mt"] = round(ext["shp_inlet_tph"] * hours, 2)
            d["stg_hp_extraction_mt"] = round(ext["hp_extraction_tph"] * hours, 2)
            d["stg_mp_extraction_mt"] = round(ext["mp_extraction_tph"] * hours, 2)
            d["stg_lp_extraction_mt"] = round(ext.get("lp_extraction_tph", 0.0) * hours, 2)
            d["stg_condensate_mt"] = round(ext["condensate_tph"] * hours, 2)
            d["stg_hp_material"] = f"STG{stg_num}_HP STEAM" if stg_num else ""
            d["stg_mp_material"] = f"STG{stg_num}_MP STEAM" if stg_num else ""
            d["stg_heat_rate_kcal_kwh"] = round(ext.get("heat_rate_kcal_kwh", 0.0), 4)
            totals["shp_consumption_mt"] += d["stg_shp_consumption_mt"]
            totals["hp_extraction_mt"] += d["stg_hp_extraction_mt"]
            totals["mp_extraction_mt"] += d["stg_mp_extraction_mt"]
            totals["lp_extraction_mt"] += d["stg_lp_extraction_mt"]
            totals["condensate_mt"] += d["stg_condensate_mt"]
            if ext.get("case_id"):
                logger.info(
                    "  DTA STG %s → Case %s (required HP %.1f / MP %.1f TPH)",
                    d["asset_name"], ext["case_id"],
                    d.get("dta_stg_req_hp_tph", 0.0), d.get("dta_stg_req_mp_tph", 0.0),
                )
            log_stg_extraction(d["asset_name"], ext)

    if plant_id == _SEZ_PLANT_ID:
        for d in dispatch:
            if "STG" not in d.get("asset_type", "").upper():
                continue
            hours = float(d.get("op_hours", 0.0))
            mwh = float(d.get("dispatched_mwh", 0.0))
            load_mw = mwh / hours if hours > 0 else 0.0
            ext = calculate_sez_stg_extraction(load_mw)
            stg_num = _stg_num_from_name(d["asset_name"])
            d["stg_hp_inlet_tph"] = round(ext["hp_inlet_tph"], 4)
            d["stg_hp_for_power_tph"] = round(ext["hp_for_power_tph"], 4)
            d["stg_hp_for_mp_consumption_tph"] = round(ext["hp_for_mp_extraction_tph"], 4)
            d["stg_mp_extraction_tph"] = round(ext["mp_extraction_tph"], 4)
            d["stg_ssc_kg_kwh"] = round(ext["ssc_kg_kwh"], 4)
            d["stg_heat_rate_kcal_kwh"] = round(ext["fy2025_26_heat_rate_kcal_kwh"], 4)
            d["stg_hp_consumption_mt"] = round(ext["hp_inlet_tph"] * hours, 2)
            d["stg_hp_for_power_mt"] = round(ext["hp_for_power_tph"] * hours, 2)
            d["stg_hp_for_mp_consumption_mt"] = round(ext["hp_for_mp_extraction_tph"] * hours, 2)
            d["stg_mp_extraction_mt"] = round(ext["mp_extraction_tph"] * hours, 2)
            d["stg_mp_material"] = f"STG{stg_num}_MP STEAM" if stg_num else ""
            totals["mp_extraction_mt"] += d["stg_mp_extraction_mt"]
            logger.info(
                "  SEZ STG calculation: %s load=%.4f MW, HP inlet=%.2f TPH, "
                "HP power=%.2f TPH, HP for MP=%.2f TPH, MP extraction=%.2f TPH, "
                "SSC=%.4f kg/kWh, heat rate=%.2f kcal/kWh",
                d["asset_name"], load_mw, ext["hp_inlet_tph"],
                ext["hp_for_power_tph"], ext["hp_for_mp_extraction_tph"],
                ext["mp_extraction_tph"], ext["ssc_kg_kwh"],
                ext["fy2025_26_heat_rate_kcal_kwh"],
            )

    totals["hp_consumption_mt"] = sum(
        float(d.get("stg_hp_consumption_mt", 0.0)) for d in dispatch
    )
    totals["hp_for_power_mt"] = sum(
        float(d.get("stg_hp_for_power_mt", 0.0)) for d in dispatch
    )
    totals["hp_for_mp_consumption_mt"] = sum(
        float(d.get("stg_hp_for_mp_consumption_mt", 0.0)) for d in dispatch
    )

    return totals


def dispatch_power(
    plant_id: str,
    month: int,
    year: int,
    dispatch_mode: str = "all_min_first",
    demands: dict = None,
    ods_reader = None,
    gt_heat_rate_df = None,
) -> dict:
    """
    Dispatch power generation assets for a JMD CPP plant/month.

    Args:
        plant_id:       CPP plant UUID
        month:          1-12
        year:           calendar year
        dispatch_mode:  "all_min_first" (default) or "one_by_one" (future)
        demands:        pre-fetched merged demands dict from calculator. If provided,
                        skips DB round-trip for process + fixed consumption.
        ods_reader:     pre-loaded ODSNormsReader for norms. If None, a cached
                        reader is used automatically.
        gt_heat_rate_df: DataFrame with GT heat rate data from CPP_GTHeatRate table.
                        If None, uses hardcoded fallback lookup.

    Returns:
        {
            "demand_mwh":           float,
            "demand_detail":        dict  (process_mwh, fixed_mwh, total_mwh),
            "total_generation_mwh": float,
            "surplus_mwh":          float,
            "deficit_mwh":          float,
            "dispatch_mode":        str,
            "assets":               list  (per-asset dispatch detail),
        }
    """
    # Ensure we have a norms reader (cached if not provided)
    if ods_reader is None:
        ods_reader = get_norms_reader(plant_id, month, year)

    # 1. Build asset table (available assets with capacity + priority)
    assets = _build_asset_table(plant_id, month, year)
    if not assets:
        logger.warning("  [DISPATCH] No available power assets for plant %s in %d/%d", plant_id, month, year)
        return {
            "demand_mwh": 0.0,
            "demand_detail": {"process_mwh": 0.0, "fixed_mwh": 0.0, "total_mwh": 0.0},
            "total_generation_mwh": 0.0,
            "surplus_mwh": 0.0,
            "deficit_mwh": 0.0,
            "dispatch_mode": dispatch_mode,
            "assets": [],
            "message": "No available power assets",
        }

    # 2. Fetch power demand (use pre-fetched demands if available)
    demand = _get_power_demand(plant_id, month, year, demands=demands)
    total_demand = demand["total_mwh"]

    # 2b. Build GT heat rate lookup from database or use fallback
    gt_lookup = build_gt_heat_rate_lookup(gt_heat_rate_df)

    # 2c. Apply spinning margin — reduces effective_max_mw per asset
    power_spinning_margin = _apply_power_spinning_margin(assets, plant_id)

    # 3. Run dispatch
    if dispatch_mode == "one_by_one":
        dispatch = _dispatch_one_by_one(assets, total_demand, plant_id, month, year, ods_reader=ods_reader, gt_lookup=gt_lookup)
    else:
        dispatch = _dispatch_all_min_first(assets, total_demand, plant_id, month, year, ods_reader=ods_reader, gt_lookup=gt_lookup)

    # 4. Compute totals
    total_gen = sum(d["dispatched_mwh"] for d in dispatch)
    surplus = round(max(0.0, total_gen - total_demand), 2)
    deficit = round(max(0.0, total_demand - total_gen), 2)

    # 5. Log the result
    _log_dispatch_result(demand, dispatch, month, year, power_spinning_margin)

    total_free_steam = round(sum(d.get("free_steam_mt", 0) for d in dispatch), 2)
    total_aux = round(sum(d.get("aux_power", 0.0) for d in dispatch), 2)

    # 4b. STG extraction/consumption derived fields (DTA + SEZ)
    stg_totals = _apply_stg_derived_fields(plant_id, dispatch)

    return {
        "demand_mwh": total_demand,
        "demand_detail": demand,
        "total_generation_mwh": round(total_gen, 2),
        "total_free_steam_mt": total_free_steam,
        "total_aux_power_mwh": total_aux,
        "total_stg_shp_consumption_mt": round(stg_totals["shp_consumption_mt"], 2),
        "total_stg_hp_consumption_mt": round(stg_totals["hp_consumption_mt"], 2),
        "total_stg_hp_for_power_mt": round(stg_totals["hp_for_power_mt"], 2),
        "total_stg_hp_for_mp_consumption_mt": round(stg_totals["hp_for_mp_consumption_mt"], 2),
        "total_stg_hp_extraction_mt": round(stg_totals["hp_extraction_mt"], 2),
        "total_stg_mp_extraction_mt": round(stg_totals["mp_extraction_mt"], 2),
        "total_stg_condensate_mt": round(stg_totals["condensate_mt"], 2),
        "spinning_margin_mw": power_spinning_margin,
        "surplus_mwh": surplus,
        "deficit_mwh": deficit,
        "dispatch_mode": dispatch_mode,
        "assets": dispatch,
        "message": (
            f"SURPLUS: {surplus:.2f} MWh over-generation" if surplus > 0
            else f"DEFICIT: {deficit:.2f} MWh under-generation" if deficit > 0
            else "Demand met"
        ),
    }


def _match_pool_transfers(plug_by_plant: dict, tolerance_mwh: float = 0.01) -> list:
    """Decompose signed per-CPP plug positions into a deterministic transfer matrix.

    Positive plug = import requirement; negative plug = export position.
    This is a reporting decomposition of the closed electrical pool — it does
    not assert dedicated physical lines between each pair.
    """
    exporters = [[pid, -value] for pid, value in plug_by_plant.items() if value < -tolerance_mwh]
    importers = [[pid, value] for pid, value in plug_by_plant.items() if value > tolerance_mwh]
    transfers = []

    for exporter in exporters:
        for importer in importers:
            quantity = min(exporter[1], importer[1])
            if quantity <= tolerance_mwh:
                continue
            transfers.append({
                "sender_plant_id": exporter[0],
                "receiver_plant_id": importer[0],
                "quantity_mwh": round(quantity, 2),
            })
            exporter[1] -= quantity
            importer[1] -= quantity
            if exporter[1] <= tolerance_mwh:
                break

    return transfers


def _log_pool_dispatch(pool: dict, results: dict, plant_ids: list, month: int, year: int) -> None:
    """Compact per-iteration log of the pooled JMD power dispatch."""
    sep = "=" * 78
    logger.info("  %s", sep)
    logger.info("  JMD POWER POOL DISPATCH  (%s %d)", _MONTH_NAMES.get(month, ""), year)
    logger.info("  %s", sep)
    logger.info("  Pool demand: %.2f MWh   Pool generation: %.2f MWh",
                pool["total_demand_mwh"], pool["total_generation_mwh"])
    if pool["surplus_mwh"] > 0:
        logger.info("  ⚠ POOL SURPLUS: %.2f MWh (minimum generation above demand)",
                    pool["surplus_mwh"])
    elif pool["deficit_mwh"] > 0:
        logger.info("  ⚠ POOL DEFICIT: %.2f MWh (max generation below demand)",
                    pool["deficit_mwh"])

    logger.info("  %-13s  %14s  %14s  %14s", "CPP", "Demand MWh", "Gen MWh", "Plug MWh")
    logger.info("  %s  %s  %s  %s", "-" * 13, "-" * 14, "-" * 14, "-" * 14)
    for pid in plant_ids:
        r = results.get(pid, {})
        logger.info("  %-13s  %14.2f  %14.2f  %14.2f",
                    str(pid)[:13],
                    r.get("demand_mwh", 0.0),
                    r.get("total_generation_mwh", 0.0),
                    r.get("plug_mwh", 0.0))
    logger.info("  %-13s  %14.2f", "CLOSURE", pool["closure_residual_mwh"])

    if pool["transfers"]:
        logger.info("  Pool transfer decomposition:")
        for t in pool["transfers"]:
            logger.info("    %s -> %s: %.2f MWh",
                        t["sender_plant_id"], t["receiver_plant_id"], t["quantity_mwh"])
    logger.info("  %s", sep)


def dispatch_power_pool(
    plant_ids: list,
    month: int,
    year: int,
    demands_by_plant: dict = None,
    ods_readers: dict = None,
    gt_heat_rate_dfs: dict = None,
) -> dict:
    """Dispatch all JMD CPP power assets as one pool by priority.

    Builds each CPP's asset table, merges them into a single pool, runs the
    shared min-first + priority-ramp allocation against the combined demand,
    then splits the result back per CPP — enriching each asset slice with the
    owning plant's POWERGEN norms, GT heat-rate lookup, and STG curves so the
    per-CPP result dicts are identical in shape to dispatch_power().

    Args:
        plant_ids:        CPP plant UUIDs to include in the pool
        month:            1-12
        year:             calendar year
        demands_by_plant: {plant_id: dispatch_demands dict} — each dict carries
                          "_power_process_mwh" / "_power_fixed_mwh" already net
                          of that plant's external import (CTU) and U4U
                          increment.  Falls back to raw DB demand if missing.
        ods_readers:      {plant_id: norms reader} for per-plant enrichment
        gt_heat_rate_dfs: {plant_id: GT heat rate DataFrame}

    Returns:
        {
            "plants": {plant_id: power_result dict (same keys as dispatch_power)
                       plus "plant_id" and "plug_mwh"},
            "pool": {
                "total_demand_mwh", "total_generation_mwh",
                "surplus_mwh", "deficit_mwh",
                "plug_by_plant_mwh", "closure_residual_mwh",
                "transfers", "demand_by_plant",
            },
        }
    """
    demands_by_plant = demands_by_plant or {}
    ods_readers = ods_readers or {}
    gt_heat_rate_dfs = gt_heat_rate_dfs or {}

    # 1. Build the combined asset table — tag each asset with its owning CPP.
    combined = []
    for pid in plant_ids:
        assets = _build_asset_table(pid, month, year)
        _apply_power_spinning_margin(assets, pid)
        for a in assets:
            combined.append({
                **a,
                "plant_id": pid,
                "dispatched_mw": 0.0,
                "dispatched_mwh": 0.0,
            })

    # 2. Per-CPP demands (process + fixed + U4U increment − external import).
    demand_by_plant = {
        pid: _get_power_demand(pid, month, year, demands=demands_by_plant.get(pid))
        for pid in plant_ids
    }
    total_demand = sum(d["total_mwh"] for d in demand_by_plant.values())

    # 3. Single min-first + priority-ramp allocation across the whole pool.
    total_gen = _min_first_allocation(combined, total_demand)

    # 4. Split back per CPP and enrich with each plant's own norms/curves.
    results = {}
    for pid in plant_ids:
        plant_dispatch = [d for d in combined if d["plant_id"] == pid]
        reader = ods_readers.get(pid)
        if reader is None:
            reader = get_norms_reader(pid, month, year)
        excel_norms = reader.get_powergen_norms()
        gt_lookup = build_gt_heat_rate_lookup(gt_heat_rate_dfs.get(pid))
        _enrich_power_dispatch(plant_dispatch, excel_norms, gt_lookup)
        stg_totals = _apply_stg_derived_fields(pid, plant_dispatch)

        gen = round(sum(d["dispatched_mwh"] for d in plant_dispatch), 2)
        demand = demand_by_plant[pid]
        demand_mwh = demand["total_mwh"]
        surplus = round(max(0.0, gen - demand_mwh), 2)
        deficit = round(max(0.0, demand_mwh - gen), 2)
        total_free_steam = round(sum(d.get("free_steam_mt", 0.0) for d in plant_dispatch), 2)
        total_aux = round(sum(d.get("aux_power", 0.0) for d in plant_dispatch), 2)

        results[pid] = {
            "plant_id": pid,
            "demand_mwh": demand_mwh,
            "demand_detail": demand,
            "total_generation_mwh": gen,
            "total_free_steam_mt": total_free_steam,
            "total_aux_power_mwh": total_aux,
            "total_stg_shp_consumption_mt": round(stg_totals["shp_consumption_mt"], 2),
            "total_stg_hp_consumption_mt": round(stg_totals["hp_consumption_mt"], 2),
            "total_stg_hp_for_power_mt": round(stg_totals["hp_for_power_mt"], 2),
            "total_stg_hp_for_mp_consumption_mt": round(stg_totals["hp_for_mp_consumption_mt"], 2),
            "total_stg_hp_extraction_mt": round(stg_totals["hp_extraction_mt"], 2),
            "total_stg_mp_extraction_mt": round(stg_totals["mp_extraction_mt"], 2),
            "total_stg_condensate_mt": round(stg_totals["condensate_mt"], 2),
            "spinning_margin_mw": 0.0,
            "surplus_mwh": surplus,
            "deficit_mwh": deficit,
            "plug_mwh": round(demand_mwh - gen, 2),
            "dispatch_mode": "jmd_pool",
            "assets": plant_dispatch,
            "message": (
                f"EXPORT: {surplus:.2f} MWh to JMD pool" if surplus > 0
                else f"IMPORT: {deficit:.2f} MWh from JMD pool" if deficit > 0
                else "Balanced"
            ),
        }

    plug_by_plant = {pid: results[pid]["plug_mwh"] for pid in plant_ids}
    pool = {
        "total_demand_mwh": round(total_demand, 2),
        "total_generation_mwh": round(total_gen, 2),
        "surplus_mwh": round(max(0.0, total_gen - total_demand), 2),
        "deficit_mwh": round(max(0.0, total_demand - total_gen), 2),
        "plug_by_plant_mwh": plug_by_plant,
        "closure_residual_mwh": round(sum(plug_by_plant.values()), 2),
        "transfers": _match_pool_transfers(plug_by_plant),
        "demand_by_plant": demand_by_plant,
    }
    _log_pool_dispatch(pool, results, plant_ids, month, year)

    return {"plants": results, "pool": pool}


def _log_steam_dispatch_result(demand_details: dict, dispatch: list, month: int, year: int, free_steam: float, spinning_margin: float = 0.0):
    """Print the full steam dispatch result to the log in a structured table format."""
    # Derive grade list dynamically from keys present in demand_details
    top_grade = demand_details.get("_top_grade", "shp")
    cascade_grades = demand_details.get("_cascade_grades", ["lp", "mp", "hp", "shp"])
    total_demand = demand_details[f"{top_grade}_net"]
    total_gen = sum(d["dispatched_mt"] for d in dispatch) + free_steam
    surplus = max(0.0, total_gen - total_demand)
    deficit = max(0.0, total_demand - total_gen)

    sep = "=" * 78
    logger.info("  %s", sep)
    logger.info("  STEAM DISPATCH  (%s %d)", _MONTH_NAMES.get(month, ""), year)
    logger.info("  %s", sep)

    # Raw & Net Demands table — grades derived from cascade
    logger.info("  Grade      Process/Fixed MT  Letdown MT  Byproduct MT     Net Demand MT")
    logger.info("  ---------  ----------------  ----------  ------------  ----------------")
    for g_lower in cascade_grades:
        grade = g_lower.upper()
        process_fixed = demand_details.get(f"{g_lower}_process", 0.0) + demand_details.get(f"{g_lower}_fixed", 0.0)
        letdown = demand_details.get(f"{g_lower}_letdown", 0.0)
        byprod = demand_details.get(f"{g_lower}_byproduct", 0.0)
        net = demand_details.get(f"{g_lower}_net", 0.0)
        logger.info("  %-9s  %16.2f  %10.2f  %12.2f  %16.2f",
                    grade, process_fixed, letdown, byprod, net)
    logger.info("")
    logger.info("  Total Free Steam available: %.2f MT", free_steam)

    # PRDS letdown stations (capacity-bounded — DTA only)
    prds_assets = demand_details.get("_prds_assets") or []
    if prds_assets:
        logger.info("")
        logger.info("  PRDS LETDOWN STATIONS")
        logger.info("  %-25s  %-14s  %-14s  %8s  %12s  %12s  %12s  %12s  %8s",
                    "Asset", "Produces", "Consumes", "Hours", "Min MT", "Max MT", "Output MT", "Feed MT", "Load %")
        for p in prds_assets:
            load_pct = (p["dispatched_mt"] / p["max_mt"] * 100.0) if p["max_mt"] > 0 else 0.0
            logger.info("  %-25s  %-14s  %-14s  %8.2f  %12.2f  %12.2f  %12.2f  %12.2f  %7.1f%%",
                        p["asset_name"], p["produces"], p["consumes"], p["op_hours"],
                        p["min_mt"], p["max_mt"], p["dispatched_mt"],
                        p["dispatched_mt"] * p["norm"], load_pct)
            if p["unmet_mt"] > 0.0:
                logger.warning("    ⚠ CAPACITY LIMITED: %.2f MT of %s demand unmet (reported as deficit, not routed)",
                               p["unmet_mt"], p["produces"])
            if p["surplus_mt"] > 0.0:
                logger.info("    Note: min-load forced %.2f MT surplus %s", p["surplus_mt"], p["produces"])
    logger.info("")

    # Supplementary Firing Dispatch
    logger.info("  FINAL SUPPLEMENTARY FIRING DISPATCH")
    logger.info("  %-25s  %8s  %4s  %10s  %10s  %8s  %14s  %14s  %8s",
                "Asset", "Priority", "Man", "Min TPH", "Max TPH", "Hours", "Dispatched TPH", "Dispatched MT", "Load %")
    logger.info("  %s  %s  %s  %s  %s  %s  %s  %s  %s",
                "-" * 25, "-" * 8, "-" * 4, "-" * 10, "-" * 10, "-" * 8, "-" * 14, "-" * 14, "-" * 8)
    for d in dispatch:
        man_flag = "Y" if d.get("mandatory", 0) == 1 else "-"
        logger.info("  %-25s  %8d  %4s  %10.2f  %10.2f  %8.2f  %14.2f  %14.2f  %7.1f%%",
                     d["asset_name"], d["priority"], man_flag,
                     d["min_tph"], d["max_tph"], d["op_hours"],
                     d["dispatched_tph"], d["dispatched_mt"], d["load_percent"])

    supp_gen = sum(d["dispatched_mt"] for d in dispatch)
    logger.info("  %-25s  %8s  %10s  %10s  %8s  %14s  %14.2f  %8s",
                "TOTAL SUPPLEMENTARY", "", "", "", "", "", supp_gen, "")
    logger.info("  %-25s  %8s  %10s  %10s  %8s  %14s  %14.2f  %8s",
                "TOTAL STEAM GENERATED", "", "", "", "", "", total_gen, "")
                
    if surplus > 0:
        logger.info("  ✓ Steam demand fully met with surplus of %.2f MT", surplus)
    elif deficit > 0:
        logger.info("  ⚠ DEFICIT: Steam generation deficit of %.2f MT", deficit)
    else:
        logger.info("  ✓ Steam demand met exactly")

    # Spinning margin post-dispatch verification
    if spinning_margin > 0:
        working = [d for d in dispatch if d["op_hours"] > 0]
        total_headroom = sum(d.get("orig_max_tph", d["max_tph"]) - d["dispatched_tph"] for d in working)
        if total_headroom >= spinning_margin:
            logger.info("")
            logger.info("  ✓ STEAM SPINNING MARGIN OK: %.2f TPH reserved (required: %.2f TPH)", total_headroom, spinning_margin)
        else:
            logger.warning("")
            logger.warning("  " + "!" * 76)
            logger.warning("  !  *** STEAM SPINNING MARGIN VIOLATION ***")
            logger.warning("  !  Required: %.2f TPH  |  Available headroom: %.2f TPH  |  Shortfall: %.2f TPH", spinning_margin, total_headroom, spinning_margin - total_headroom)
            logger.warning("  !  Assets are dispatched beyond effective max — margin NOT maintained!")
            logger.warning("  " + "!" * 76)
    logger.info("  %s", sep)


def _bounded_prds_output(net_mt: float, prds: dict) -> tuple:
    """Clamp PRDS output to [min_mt, max_mt] for a given net grade demand.

    Returns (prds_out_mt, unmet_mt, surplus_mt):
      - prds_out_mt: actual lower-grade output from this PRDS
      - unmet_mt:    demand above max capacity (deficit — reported, not routed)
      - surplus_mt:  forced output above demand when min load exceeds net
    """
    max_mt = prds.get("max_mt", 0.0)
    min_mt = prds.get("min_mt", 0.0)
    if prds.get("op_hours", 0.0) <= 0 or max_mt <= 0:
        return 0.0, max(0.0, net_mt), 0.0
    out = min(max(0.0, net_mt, min_mt), max_mt)
    unmet = max(0.0, net_mt - out)
    surplus = max(0.0, out - max(0.0, net_mt))
    return out, unmet, surplus


def dispatch_steam(
    plant_id: str,
    month: int,
    year: int,
    power_result: dict,
    dispatch_mode: str = "all_min_first",
    demands: dict = None,
    ods_reader = None,
) -> dict:
    """
    Dispatch steam generation assets (HRSGs + Aux Boilers) to meet SHP demand.

    Args:
        plant_id:       CPP plant UUID
        month:          1-12
        year:           calendar year
        power_result:   result dict from dispatch_power() (for GT-HRSG interlinking)
        dispatch_mode:  "all_min_first" (default) or "one_by_one" (future)
        demands:        pre-fetched merged demands dict from calculator. If provided,
                        skips DB round-trip for process + fixed consumption.
        ods_reader:     pre-loaded ODSNormsReader for norms. If None, a cached
                        reader is used automatically.
    """
    import re
    # Ensure we have a norms reader (cached if not provided)
    if ods_reader is None:
        ods_reader = get_norms_reader(plant_id, month, year)

    # 1. Fetch norms from ODS — letdown cascade drives the grade list
    letdown_norms = ods_reader.get_steam_letdown_norms()
    cascade = letdown_norms.get("_cascade", [])  # ordered lowest→highest pressure
    # grade_prefixes: ['lp', 'mp', 'hp', 'shp'] derived from cascade + top grade
    if cascade:
        grade_prefixes = [
            step["produces"].replace(" Steam_Dis", "").replace("_Dis", "").lower()
            for step in cascade
        ]
        top_grade_dis = cascade[-1]["consumes"]  # highest grade consumed by top PRDS
        top_grade = top_grade_dis.replace(" Steam_Dis", "").replace("_Dis", "").lower()
        if top_grade not in grade_prefixes:
            grade_prefixes.append(top_grade)
    else:
        grade_prefixes = ["lp", "mp", "hp", "shp"]
        top_grade = "shp"

    # 2. Fetch demands using the dynamic grade list
    raw_demands = _get_steam_demands(plant_id, month, year, demands=demands,
                                      grade_prefixes=grade_prefixes)
    
    # Byproduct norms grouped by steam grade ({'lp': {util: norm}, 'mp': ...}).
    # Negative norms = co-product supply credited against that grade's demand.
    byproduct_by_grade_norms = (
        ods_reader.get_hrsg_byproduct_norms_by_grade()
        if hasattr(ods_reader, "get_hrsg_byproduct_norms_by_grade")
        else {"lp": ods_reader.get_hrsg_byproduct_norms()}
    )
    
    # 3. Load DB assets, operational hours, priority, capacity
    steam_assets = fetch_steam_generation_assets(plant_id)
    steam_hours = fetch_steam_asset_operational_hours(plant_id, month, year)
    steam_pri = fetch_steam_asset_priority(plant_id, month, year)
    fy_start = _fy_year(month, year)
    steam_caps = fetch_steam_asset_capacity_all_months(plant_id, fy_start)
    
    # Index for fast lookup
    hours_by_id = {h["asset_id"]: h for h in steam_hours}
    pri_by_id = {p["asset_id"]: p for p in steam_pri}
    caps_by_id = {c["asset_id"]: c for c in steam_caps}
    
    # Filter for dispatchable steam assets (type HRSG or AUXBOILER, steam_type SHP Steam_Dis)
    dispatch_assets = []
    for asset in steam_assets:
        atype = asset.get("asset_type", "")
        stype = asset.get("steam_type", "")
        aname = asset.get("asset_name", "")
        
        # We only dispatch HRSG and AUXBOILER producing SHP steam
        top_dis = cascade[-1]["consumes"] if cascade else "SHP Steam_Dis"
        if stype == top_dis and atype in ["HRSG", "AUXBOILER"]:
            asset_id = asset["asset_id"]
            
            # Determine hours and priority
            is_hrsg = "HRSG" in atype.upper() or "HRSG" in aname.upper()
            
            op_hours = 0.0
            priority = 999
            
            if is_hrsg:
                # GT-HRSG interlinking
                linked_gt = _find_linked_gt_asset(aname, power_result.get("assets", []))
                if linked_gt:
                    # Inherit GT hours and priority
                    op_hours = float(linked_gt.get("op_hours", 0.0))
                    priority = int(linked_gt.get("priority", 999))
                    # Only available if GT was running and generating power
                    if linked_gt.get("dispatched_mwh", 0.0) <= 0.0:
                        op_hours = 0.0
                else:
                    # Fallback to DB operational hours if GT not found or linked
                    op_hours = float(hours_by_id.get(asset_id, {}).get("operational_hours") or 0.0)
                    priority = int(pri_by_id.get(asset_id, {}).get("priority") or 999)
            else:
                # Aux boiler
                op_hours = float(hours_by_id.get(asset_id, {}).get("operational_hours") or 0.0)
                priority = int(pri_by_id.get(asset_id, {}).get("priority") or 999)
                
            # Get capacity
            mk = _month_key(month)
            cap_row = caps_by_id.get(asset_id, {})
            min_tph = cap_row.get(f"{mk}_Min")
            if min_tph is None:
                min_tph = cap_row.get("fixed_min")
            if min_tph is None:
                min_tph = 0.0
            min_tph = float(min_tph)
            
            max_tph = cap_row.get(f"{mk}_Max")
            if max_tph is None:
                max_tph = cap_row.get("fixed_max")
            if max_tph is None:
                max_tph = 0.0
            max_tph = float(max_tph)

            # Mandatory load flag (1 = must dispatch at MIN, 0 = optional)
            mandatory = int(cap_row.get(f"{mk}_Man_Load", 0) or 0)

            # For HRSGs linked to GTs, the GT exhaust provides free steam that is
            # part of the HRSG total output. We track it separately so that the
            # HRSG min/max capacity is interpreted as TOTAL output capacity, and
            # the dispatchable supplementary range is total capacity minus free steam.
            free_steam_mt = 0.0
            linked_gt = None
            if is_hrsg:
                linked_gt = _find_linked_gt_asset(aname, power_result.get("assets", []))
                if linked_gt:
                    free_steam_mt = float(linked_gt.get("free_steam_mt", 0.0))

            dispatch_assets.append({
                "asset_id": asset_id,
                "asset_name": aname,
                "asset_type": atype,
                "op_hours": op_hours,
                "min_tph": min_tph,
                "max_tph": max_tph,
                "priority": priority,
                "mandatory": mandatory,
                "free_steam_mt": free_steam_mt,
                "linked_gt": linked_gt.get("asset_name") if linked_gt else None,
                "dispatched_tph": 0.0,
                "dispatched_mt": 0.0,
                "total_output_mt": 0.0,
            })

    # 3a. PRDS assets (DTA only) — bounded letdown stations.  They are not
    # dispatched like HRSGs; their output is the net residual of the grade
    # they produce, clamped to [min_tph, max_tph] x operational hours.  The
    # parent-grade letdown they consume is bounded accordingly.
    prds_by_grade = {}
    if plant_id == _DTA_PLANT_ID:
        mk = _month_key(month)
        for asset in steam_assets:
            if str(asset.get("asset_type", "")).upper() != "PRDS":
                continue
            produced_dis = asset.get("steam_type", "")
            step = next(
                (s for s in cascade if s.get("produces") == produced_dis),
                None,
            )
            if step is None:
                continue
            cap_row = caps_by_id.get(asset["asset_id"], {})
            min_tph = cap_row.get(f"{mk}_Min")
            if min_tph is None:
                min_tph = cap_row.get("fixed_min")
            max_tph = cap_row.get(f"{mk}_Max")
            if max_tph is None:
                max_tph = cap_row.get("fixed_max")
            op_hours = float(
                hours_by_id.get(asset["asset_id"], {}).get("operational_hours") or 0.0
            )
            grade = produced_dis.replace(" Steam_Dis", "").replace("_Dis", "").lower()
            prds_by_grade[grade] = {
                "asset_id": asset["asset_id"],
                "asset_name": asset.get("asset_name", ""),
                "produces": produced_dis,
                "consumes": step["consumes"],
                "norm": step["norm"],
                "op_hours": op_hours,
                "min_mt": float(min_tph or 0.0) * op_hours,
                "max_mt": float(max_tph or 0.0) * op_hours,
                "dispatched_mt": 0.0,
                "unmet_mt": 0.0,
                "surplus_mt": 0.0,
            }

    # 3b. Apply steam spinning margin — reduces effective_max_tph per asset
    steam_spinning_margin = _apply_spinning_margin(dispatch_assets, plant_id)

    # 3c. Convert HRSG total-output min/max into supplementary-firing min/max.
    # Free steam from the linked GT already covers part of the HRSG total output,
    # so only the remaining capacity is available for supplementary firing.
    for a in dispatch_assets:
        if a["asset_type"] == "HRSG" and a["op_hours"] > 0:
            fs_tph = a["free_steam_mt"] / a["op_hours"] if a["op_hours"] > 0 else 0.0
            a["orig_min_tph"] = a["min_tph"]
            a["orig_max_tph"] = a["max_tph"]
            # Supplementary firing range: total capacity minus free steam already present
            a["min_tph"] = max(0.0, a["min_tph"] - fs_tph)
            a["max_tph"] = max(0.0, a["max_tph"] - fs_tph)
            a["free_steam_tph"] = fs_tph

    # 4. Convergence loop (5 iterations)
    # byproduct applies per grade (lp/mp) — a co-product reduces that grade's
    # net demand.  For plants with only LP byproducts this is identical to the
    # previous behaviour since their lowest grade is 'lp'.
    lowest_grade = grade_prefixes[0] if grade_prefixes else "lp"
    byproduct_by_grade: dict = {}  # {grade: total byproduct MT (negative)}
    prds_out_by_grade: dict = {}   # {grade: bounded PRDS output MT}
    prds_unmet_by_grade: dict = {} # {grade: demand above PRDS capacity MT}
    demand_details = {}
    free_steam = float(power_result.get("total_free_steam_mt", 0.0))
    
    stg_totals = (
        _stg_extraction_totals_from_power_result(power_result)
        if plant_id in (_DTA_PLANT_ID, _SEZ_PLANT_ID)
        else {}
    )

    # SEZ-PCG / DTA-PCG use a branching cascade (SHP→HP, SHP→IHP, HP→MP,
    # SHP→IP for DTA-PCG, LP→LLP).  Build a parent→children map for the
    # branching calculation.
    is_branching = plant_id in _BRANCHING_CASCADE_PLANTS
    if is_branching and cascade:
        # Map: parent_grade → list of (child_grade, norm, prds_name)
        children_map = {}
        for step in cascade:
            parent = step["consumes"].replace(" Steam_Dis", "").replace("_Dis", "").lower()
            child = step["produces"].replace(" Steam_Dis", "").replace("_Dis", "").lower()
            children_map.setdefault(parent, []).append((child, step["norm"], step["prds"]))
        # All grades that appear as a "produces" in the cascade
        child_grades = set()
        for steps in children_map.values():
            for cg, _, _ in steps:
                child_grades.add(cg)
        # Top grade = the grade that is consumed but never produced
        top_grade_pcg = None
        for parent in children_map:
            if parent not in child_grades:
                top_grade_pcg = parent
                break
        if top_grade_pcg is None:
            top_grade_pcg = top_grade  # fallback
    else:
        children_map = None

    for iteration in range(5):
        # Dynamic cascade: walk from lowest grade upward, each grade letdowns into the next
        net_by_grade = {}
        letdown_by_grade = {}

        if is_branching and children_map:
            # ── SEZ-PCG branching cascade ──────────────────────────────────
            # Process grades bottom-up: for each grade, net demand =
            #   process + fixed + sum(letdowns from all children).
            # If net > 0, letdown from parent = net * norm.
            # If net <= 0, no letdown (surplus).
            #
            # We need to process in the right order: children before parents.
            # Build a topological order starting from leaf grades (no children).
            all_grades = set(grade_prefixes)
            # Leaf grades = grades that are not a parent of any other grade
            leaf_grades = all_grades - set(children_map.keys())
            # Topological order: process leaves first, then their parents, etc.
            topo_order = []
            remaining = set(all_grades)
            processed = set()
            # Start with leaves
            for g in grade_prefixes:
                if g in leaf_grades:
                    topo_order.append(g)
                    processed.add(g)
                    remaining.discard(g)
            # Now process remaining grades in order (children before parents)
            while remaining:
                added = False
                for g in grade_prefixes:
                    if g in remaining:
                        # Check if all children of g are already processed
                        kids = children_map.get(g, [])
                        if all(cg in processed for cg, _, _ in kids):
                            topo_order.append(g)
                            processed.add(g)
                            remaining.discard(g)
                            added = True
                if not added:
                    # Circular dependency or missing grade; add remaining as-is
                    for g in grade_prefixes:
                        if g in remaining:
                            topo_order.append(g)
                            processed.add(g)
                            remaining.discard(g)

            for g in topo_order:
                base = raw_demands.get(f"{g}_process", 0.0) + raw_demands.get(f"{g}_fixed", 0.0)
                # Co-product supply credits this grade's demand (negative value)
                base += byproduct_by_grade.get(g, 0.0)
                # Add letdowns from all children (already calculated in topo order)
                child_letdown_sum = 0.0
                for cg, norm, prds_name in children_map.get(g, []):
                    child_letdown_sum += letdown_by_grade.get(cg, 0.0)
                net = base + child_letdown_sum
                net_by_grade[g] = net

                # Calculate letdown IMMEDIATELY so parent grades can use it
                if g == top_grade_pcg:
                    letdown_by_grade[g] = 0.0
                else:
                    # Find which PRDS produces this grade
                    norm = None
                    for parent, steps in children_map.items():
                        for cg, n, pn in steps:
                            if cg == g:
                                norm = n
                                break
                    if norm is not None:
                        letdown_by_grade[g] = max(0.0, net) * norm
                    else:
                        letdown_by_grade[g] = 0.0

        else:
            # ── Linear cascade (original logic for other plants) ───────────
            prev_letdown = 0.0
            for i, g in enumerate(grade_prefixes):
                if g == top_grade or plant_id in _U4U_LETDOWN_FEED_PLANTS:
                    # Top-grade: no higher grade feeds it within the cascade.
                    # U4U-letdown-feed plants (DTA, C2): every grade's PRDS
                    # feed reaches it through the U4U path
                    # (_calculate_steam_cascade_u4u adds the letdown onto the
                    # parent _Dis producer's demand), so prev_letdown must
                    # not be added here or the same flow is counted twice.
                    net = raw_demands.get(f"{g}_process", 0.0) + raw_demands.get(f"{g}_fixed", 0.0)
                else:
                    net = raw_demands.get(f"{g}_process", 0.0) + raw_demands.get(f"{g}_fixed", 0.0) + prev_letdown
                # Co-product supply credits this grade's demand (negative value).
                # For existing plants only 'lp' is populated — same as the
                # previous lowest-grade behaviour.
                net += byproduct_by_grade.get(g, 0.0)
                net_by_grade[g] = net
                # Letdown: this grade's net demand drives consumption from the grade above
                if i < len(cascade):
                    norm = cascade[i]["norm"]
                    prds = prds_by_grade.get(g)
                    if prds is not None:
                        out, unmet, surplus = _bounded_prds_output(net, prds)
                        prds["dispatched_mt"] = out
                        prds["unmet_mt"] = unmet
                        prds["surplus_mt"] = surplus
                        prds_out_by_grade[g] = out
                        prds_unmet_by_grade[g] = unmet
                        letdown = out * norm
                    else:
                        letdown = max(0.0, net) * norm
                else:
                    letdown = 0.0
                letdown_by_grade[g] = letdown
                prev_letdown = letdown

        # Adjust the cascade for DTA STG extraction (Option A)
        if stg_totals:
            mp_ext = stg_totals.get("mp_extraction_mt", 0.0)
            hp_ext = stg_totals.get("hp_extraction_mt", 0.0)

            if "mp" in net_by_grade and mp_ext:
                net_by_grade["mp"] = max(0.0, net_by_grade["mp"] - mp_ext)
            if plant_id == _DTA_PLANT_ID and "hp" in net_by_grade and hp_ext:
                net_by_grade["hp"] = max(0.0, net_by_grade["hp"] - hp_ext)

            # Recalculate letdown for adjusted grades (cascade unchanged)
            for i, g in enumerate(grade_prefixes):
                if i < len(cascade):
                    norm = cascade[i]["norm"]
                    prds = prds_by_grade.get(g)
                    if prds is not None:
                        out, unmet, surplus = _bounded_prds_output(net_by_grade[g], prds)
                        prds["dispatched_mt"] = out
                        prds["unmet_mt"] = unmet
                        prds["surplus_mt"] = surplus
                        prds_out_by_grade[g] = out
                        prds_unmet_by_grade[g] = unmet
                        letdown_by_grade[g] = out * norm
                    else:
                        letdown_by_grade[g] = max(0.0, net_by_grade[g]) * norm
                else:
                    letdown_by_grade[g] = 0.0

        demand_details = {
            **raw_demands,
            **{f"{g}_byproduct": byproduct_by_grade.get(g, 0.0) for g in grade_prefixes},
            f"{lowest_grade}_byproduct": byproduct_by_grade.get(lowest_grade, 0.0),
            **{f"{g}_letdown": letdown_by_grade[g] for g in grade_prefixes},
            **{f"{g}_net": round(net_by_grade[g], 2) for g in grade_prefixes},
            **{f"{g}_prds_out": prds_out_by_grade[g] for g in grade_prefixes if g in prds_out_by_grade},
            **{f"{g}_prds_unmet": prds_unmet_by_grade[g] for g in grade_prefixes if g in prds_unmet_by_grade},
            "_cascade_grades": grade_prefixes,
            "_top_grade": top_grade,
            "stg_shp_consumption_mt": stg_totals.get("shp_consumption_mt", 0.0),
            "stg_hp_extraction_mt": stg_totals.get("hp_extraction_mt", 0.0),
            "stg_mp_extraction_mt": stg_totals.get("mp_extraction_mt", 0.0),
        }

        # Remaining top-grade demand to be met by supplementary firing.
        # Free steam from GTs is already available supply — only the portion
        # NOT covered by free steam needs supplementary firing from assets.
        net_top = net_by_grade.get(top_grade, 0.0)
        net_shp_to_dispatch = max(0.0, net_top - free_steam)
        
        # Reset dispatched values for this iteration
        for asset in dispatch_assets:
            asset["dispatched_tph"] = 0.0
            asset["dispatched_mt"] = 0.0
            
        if net_shp_to_dispatch > 0:
            # Step A: Mandatory assets at MIN load first
            total_min_mt = 0.0
            for a in dispatch_assets:
                if a["op_hours"] > 0 and a["mandatory"] == 1:
                    a["dispatched_tph"] = a["min_tph"]
                    a["dispatched_mt"] = a["min_tph"] * a["op_hours"]
                    total_min_mt += a["dispatched_mt"]

            if total_min_mt >= net_shp_to_dispatch:
                # Mandatory MIN meets or exceeds demand. Keep at MIN load
                pass
            else:
                # Step B: Bring in optional assets at MIN
                for a in dispatch_assets:
                    if a["op_hours"] > 0 and a["mandatory"] == 0:
                        a["dispatched_tph"] = a["min_tph"]
                        a["dispatched_mt"] = a["min_tph"] * a["op_hours"]
                        total_min_mt += a["dispatched_mt"]

                if total_min_mt >= net_shp_to_dispatch:
                    # All MIN meets or exceeds demand. Keep at MIN load
                    pass
                else:
                    # Step C: Ramp up by priority across all running assets
                    remaining = net_shp_to_dispatch - total_min_mt
                    
                    priority_groups = defaultdict(list)
                    for idx, a in enumerate(dispatch_assets):
                        if a["op_hours"] > 0 and (a["dispatched_mt"] > 0 or a["mandatory"] == 1):
                            priority_groups[a["priority"]].append(idx)
                        
                    for pri in sorted(priority_groups.keys()):
                        if remaining <= 0:
                            break
                        group_indices = priority_groups[pri]
                        
                        group_headroom = []
                        for idx in group_indices:
                            a = dispatch_assets[idx]
                            headroom_tph = a["max_tph"] - a["dispatched_tph"]
                            if headroom_tph > 0.001:
                                group_headroom.append((idx, headroom_tph, a["op_hours"]))
                            
                        if not group_headroom:
                            continue
                            
                        total_group_headroom_mt = sum(h_tph * hrs for _, h_tph, hrs in group_headroom)
                        allocation_mt = min(remaining, total_group_headroom_mt)
                        
                        if len(group_headroom) == 1:
                            idx, headroom_tph, hrs = group_headroom[0]
                            add_tph = allocation_mt / hrs
                            add_tph = min(add_tph, headroom_tph)
                            dispatch_assets[idx]["dispatched_tph"] += add_tph
                            dispatch_assets[idx]["dispatched_mt"] = dispatch_assets[idx]["dispatched_tph"] * hrs
                            remaining -= add_tph * hrs
                        else:
                            uncapped = [(idx, headroom_tph, hrs) for idx, headroom_tph, hrs in group_headroom]
                            remaining_to_allocate = allocation_mt
                            
                            while uncapped and remaining_to_allocate > 0.001:
                                total_hrs_uncapped = sum(hrs for _, _, hrs in uncapped)
                                if total_hrs_uncapped <= 0:
                                    break
                                target_add_tph = remaining_to_allocate / total_hrs_uncapped
                                
                                newly_capped = []
                                for idx, headroom_tph, hrs in uncapped:
                                    if target_add_tph >= headroom_tph:
                                        dispatch_assets[idx]["dispatched_tph"] += headroom_tph
                                        dispatch_assets[idx]["dispatched_mt"] = dispatch_assets[idx]["dispatched_tph"] * hrs
                                        remaining_to_allocate -= headroom_tph * hrs
                                        newly_capped.append(idx)
                                    else:
                                        dispatch_assets[idx]["dispatched_tph"] += target_add_tph
                                        dispatch_assets[idx]["dispatched_mt"] = dispatch_assets[idx]["dispatched_tph"] * hrs
                                        remaining_to_allocate -= target_add_tph * hrs
                                        
                                if newly_capped:
                                    uncapped = [(i, h, hrs) for i, h, hrs in uncapped if i not in newly_capped]
                                else:
                                    break
                                    
                            remaining -= (allocation_mt - remaining_to_allocate)
                        
        # Total HRSG output = free steam + supplementary firing. Add free steam back
        # to the HRSG output so that byproduct and downstream U4U are based on total output.
        for a in dispatch_assets:
            if a["asset_type"] == "HRSG":
                a["total_output_mt"] = a["dispatched_mt"] + a["free_steam_mt"]
            else:
                a["total_output_mt"] = a["dispatched_mt"]

        # Recalculate byproduct steam per grade (based on total SHP output).
        # Co-products (negative norms) are supply credited against that grade's
        # demand — lp byproduct → lp demand, mp byproduct → mp demand, etc.
        byproduct_by_grade = {}
        for a in dispatch_assets:
            for grade, grade_norms in byproduct_by_grade_norms.items():
                norm_val = grade_norms.get(a["asset_name"].upper())
                if norm_val is None:
                    norm_val = next(
                        (v for k, v in grade_norms.items() if k in a["asset_name"].upper() or a["asset_name"].upper() in k),
                        None
                    )
                if norm_val is None:
                    logger.debug("  [DISPATCH] No %s byproduct norm in ODS for asset '%s'; using 0.0", grade, a["asset_name"])
                    continue
                byproduct_by_grade[grade] = byproduct_by_grade.get(grade, 0.0) + a["total_output_mt"] * norm_val
            
    # Final total-output computation for returned assets
    for a in dispatch_assets:
        if a["asset_type"] == "HRSG":
            a["total_output_mt"] = a["dispatched_mt"] + a["free_steam_mt"]
        else:
            a["total_output_mt"] = a["dispatched_mt"]

    # Calculate load percentages and round fields (load % is vs total output capacity)
    for a in dispatch_assets:
        orig_max = a.get("orig_max_tph", a["max_tph"])
        total_output_tph = a["total_output_mt"] / a["op_hours"] if a["op_hours"] > 0 else 0.0
        a["load_percent"] = round(
            (total_output_tph / orig_max * 100) if orig_max > 0 else 0.0, 1
        )
        a["dispatched_tph"] = round(a["dispatched_tph"], 4)
        a["dispatched_mt"] = round(a["dispatched_mt"], 2)
        a["total_output_mt"] = round(a["total_output_mt"], 2)
        a["min_tph"] = round(a["min_tph"], 2)
        a["max_tph"] = round(a["max_tph"], 2)

    # PRDS audit rows (DTA) — exposed through demand_details for the U4U
    # cascade and the dispatch log.
    prds_assets = sorted(prds_by_grade.values(), key=lambda p: p["produces"])
    demand_details["_prds_assets"] = prds_assets
    prds_unmet_mt = round(sum(p["unmet_mt"] for p in prds_assets), 2)

    # Log results
    _log_steam_dispatch_result(demand_details, dispatch_assets, month, year, free_steam, steam_spinning_margin)

    total_supp_gen = round(sum(a["dispatched_mt"] for a in dispatch_assets), 2)
    total_steam_gen = round(sum(a["total_output_mt"] for a in dispatch_assets), 2)
    net_top = demand_details.get(f"{top_grade}_net", 0.0)
    surplus_mt = round(max(0.0, total_steam_gen - net_top), 2)
    deficit_mt = round(max(0.0, net_top - total_steam_gen), 2)

    return {
        "demand_detail": demand_details,
        "total_free_steam_mt": free_steam,
        "total_supplementary_generation_mt": total_supp_gen,
        "total_generation_mt": total_steam_gen,
        "surplus_mt": surplus_mt,
        "deficit_mt": deficit_mt,
        "prds_unmet_mt": prds_unmet_mt,
        "prds_assets": prds_assets,
        "assets": dispatch_assets,
        "message": (
            f"SURPLUS: {surplus_mt:.2f} MT over-generation" if surplus_mt > 0
            else f"DEFICIT: {deficit_mt:.2f} MT under-generation" if deficit_mt > 0
            else "Demand met"
        ) + (
            f" | PRDS deficit: {prds_unmet_mt:.2f} MT unmet lower-grade demand"
            if prds_unmet_mt > 0 else ""
        ),
    }


def _apply_spinning_margin(dispatch_assets: list, plant_id: str) -> float:
    """
    Compute effective_max_tph for each working asset by proportionally
    reducing max_tph to reserve spinning margin headroom.

    Returns the configured margin value (0.0 if none).

    Edge cases handled:
    - effective_max < min_tph → cap at min_tph, redistribute excess to others
    - total max <= margin → warn, no reduction applied
    - no working assets → warn
    """
    margin = STEAM_SPINNING_MARGIN_TPH.get(plant_id, 0.0)

    for a in dispatch_assets:
        a["orig_max_tph"] = a["max_tph"]
        a["effective_max_tph"] = a["max_tph"]

    if margin <= 0:
        return 0.0

    working = [a for a in dispatch_assets if a["op_hours"] > 0]
    if not working:
        logger.warning("")
        logger.warning("!" * 78)
        logger.warning("!  SPINNING MARGIN: No working assets — cannot reserve %.2f TPH margin", margin)
        logger.warning("!" * 78)
        logger.warning("")
        return margin

    total_max = sum(a["max_tph"] for a in working)
    if total_max <= margin:
        logger.warning("")
        logger.warning("!" * 78)
        logger.warning("!  SPINNING MARGIN: Total max capacity (%.2f TPH) <= margin (%.2f TPH) — cannot reserve", total_max, margin)
        logger.warning("!" * 78)
        logger.warning("")
        return margin

    # Iterative proportional reduction with min_tph floor
    remaining_margin = margin
    remaining = working[:]

    while remaining_margin > 0.01 and remaining:
        sub_total = sum(a["max_tph"] for a in remaining)
        if sub_total <= 0:
            break

        newly_capped = []
        for a in remaining:
            share = remaining_margin * a["max_tph"] / sub_total
            eff = a["max_tph"] - share
            if eff < a["min_tph"]:
                a["effective_max_tph"] = a["min_tph"]
                newly_capped.append(a)
            else:
                a["effective_max_tph"] = eff

        if newly_capped:
            absorbed = sum(a["max_tph"] - a["min_tph"] for a in newly_capped)
            remaining_margin -= absorbed
            remaining = [a for a in remaining if a not in newly_capped]
            for a in remaining:
                a["effective_max_tph"] = a["max_tph"]  # reset for next iteration
        else:
            remaining_margin = 0

    # Log the margin configuration
    logger.info("")
    logger.info("  SPINNING MARGIN CONFIG")
    logger.info("  Required margin: %.2f TPH", margin)
    logger.info("  %-25s  %10s  %10s  %10s", "Asset", "Max TPH", "Eff Max", "Reserve")
    logger.info("  %s  %s  %s  %s", "-" * 25, "-" * 10, "-" * 10, "-" * 10)
    total_reserved = 0.0
    for a in working:
        reserve = a["orig_max_tph"] - a["effective_max_tph"]
        total_reserved += reserve
        logger.info("  %-25s  %10.2f  %10.2f  %10.2f",
                     a["asset_name"], a["orig_max_tph"], a["effective_max_tph"], reserve)
    logger.info("  %-25s  %10s  %10s  %10.2f", "TOTAL RESERVED", "", "", total_reserved)

    if remaining_margin > 0.01:
        logger.warning("")
        logger.warning("!" * 78)
        logger.warning("!  SPINNING MARGIN SHORTFALL: Cannot reserve full %.2f TPH — shortfall of %.2f TPH", margin, remaining_margin)
        logger.warning("!" * 78)
        logger.warning("")
    else:
        logger.info("  → Margin of %.2f TPH successfully reserved", margin)
    logger.info("")

    return margin


def _apply_power_spinning_margin(dispatch_assets: list, plant_id: str) -> float:
    """
    Delegate to the centralised power spinning margin calculation in
    balancing_integration_v3._apply_power_spinning_margin_v2().

    Returns the margin value (0.0 if disabled or no margin needed).
    """
    return _apply_power_spinning_margin_v2(dispatch_assets, plant_id)


def _find_linked_gt_asset(hrsg_name: str, power_assets: list) -> dict:
    import re
    m_hrsg = re.search(r'HRSG\s*(\d+)', hrsg_name, re.IGNORECASE)
    if not m_hrsg:
        return None
    hrsg_num = m_hrsg.group(1)
    
    for gt in power_assets:
        gt_name = gt["asset_name"]
        m_gt = re.search(r'GT(?:G|\s+Power\s+Plant)?\s*[-_]?\s*(\d+)', gt_name, re.IGNORECASE)
        if m_gt and m_gt.group(1) == hrsg_num:
            return gt
    return None

