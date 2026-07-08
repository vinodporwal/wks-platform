"""
NMD Power & Steam Dispatch Engine
==================================
Priority-based dispatch for NMD CPP plant.

Dispatch Rules (same as JMD):
  1. All available assets start at MIN load simultaneously.
  2. If total MIN >= demand → surplus logged, dispatch stays at MIN.
  3. If total MIN < demand  → ramp up by priority (1 = highest first).
  4. Equal-priority assets share additional load equally (equal MW).
  5. If all at MAX and still deficit → deficit logged.

Key NMD differences from JMD:
  - Asset data from AssetAvailability + OperationalHours (one row per month)
  - GT heat rate curves from CPP_GTHeatRate table (DB, not hardcoded)
  - Steam assets from SteamGenerationAssets (inline capacity + GT linkage)
  - Norms from NMDNormsReader (DB-based, not ODS)
"""

from __future__ import annotations

import logging
from collections import defaultdict
from typing import Dict, List, Optional

from database.queries import (
    fetch_asset_availability_with_hours,
    fetch_steam_generation_assets,
    fetch_gt_heat_rate_curves,
    fetch_process_demands,
    fetch_fixed_consumption,
    fetch_import_power,
)
from engine.norms_reader import NMDNormsReader

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Per-month DB query cache — eliminates redundant DB round-trips across U4U
# iterations (asset table, import power, GT curves, steam assets are invariant
# for a given month/year).  Call clear_db_cache() between months.
# ---------------------------------------------------------------------------
_DB_CACHE: Dict[tuple, object] = {}


def clear_db_cache() -> None:
    """Clear the dispatch-engine DB cache.  Call between months."""
    _DB_CACHE.clear()


DEFAULT_FREE_STEAM_FACTOR = 1.97
STEAM_TO_POWER_MT_PER_MWH = 3.56  # SHP steam (MT) consumed per MWh by STG
EXCESS_STEAM_THRESHOLD_MT = 1.0

_MONTH_NAMES = {
    1: "January", 2: "February", 3: "March", 4: "April",
    5: "May", 6: "June", 7: "July", 8: "August",
    9: "September", 10: "October", 11: "November", 12: "December",
}


def _fy_year(month: int, year: int) -> int:
    return year if month >= 4 else year - 1


def _fy_string(month: int, year: int) -> str:
    fy_start = _fy_year(month, year)
    return f"{fy_start}-{str(fy_start + 1)[-2:]}"


# ---------------------------------------------------------------------------
# GT asset name → canonical key mapping
# ---------------------------------------------------------------------------

def _canonical_gt_name(asset_name: str) -> Optional[str]:
    """Normalize GT asset names to canonical keys: GT-1, GT-2, GT-3."""
    n = str(asset_name or "").upper().replace(" ", "").replace("-", "")
    if "GT1" in n or "PLANT1" in n or "PLANT-1" in n:
        return "GT-1"
    if "GT2" in n or "PLANT2" in n or "PLANT-2" in n:
        return "GT-2"
    if "GT3" in n or "PLANT3" in n or "PLANT-3" in n:
        return "GT-3"
    return None


def _resolve_linked_gt(hrsg_name: str, power_assets: list) -> Optional[dict]:
    """
    Find the linked GT asset for an HRSG by name pattern.
    HRSG1 → GT1, HRSG2 → GT2, HRSG3 → GT3.
    """
    n = str(hrsg_name or "").upper().replace(" ", "").replace("-", "")
    gt_num = None
    if "HRSG1" in n:
        gt_num = 1
    elif "HRSG2" in n:
        gt_num = 2
    elif "HRSG3" in n:
        gt_num = 3
    if gt_num is None:
        return None
    for a in power_assets:
        canonical = _canonical_gt_name(a.get("asset_name", ""))
        if canonical == f"GT-{gt_num}":
            return a
    return None


# ---------------------------------------------------------------------------
# Heat rate / free steam lookup helpers
# ---------------------------------------------------------------------------

def _interpolate(load: float, points: list, value_key: str) -> float:
    """Linear interpolation over sorted list of dicts by 'gt_load' key."""
    if not points:
        return 0.0
    sorted_pts = sorted(points, key=lambda p: p["gt_load"])
    if load <= sorted_pts[0]["gt_load"]:
        return sorted_pts[0][value_key]
    if load >= sorted_pts[-1]["gt_load"]:
        return sorted_pts[-1][value_key]
    for i in range(1, len(sorted_pts)):
        lo = sorted_pts[i - 1]
        hi = sorted_pts[i]
        if lo["gt_load"] <= load <= hi["gt_load"]:
            if hi["gt_load"] == lo["gt_load"]:
                return lo[value_key]
            frac = (load - lo["gt_load"]) / (hi["gt_load"] - lo["gt_load"])
            return lo[value_key] + (hi[value_key] - lo[value_key]) * frac
    return sorted_pts[-1][value_key]


def _get_heat_rate(load_mw: float, curve: list) -> float:
    if not curve:
        return 0.0
    return round(_interpolate(load_mw, curve, "heat_rate"), 2)


def _get_free_steam_factor(load_mw: float, curve: list) -> float:
    if not curve:
        return DEFAULT_FREE_STEAM_FACTOR
    return round(_interpolate(load_mw, curve, "free_steam_factor"), 6)


# ---------------------------------------------------------------------------
# Power dispatch
# ---------------------------------------------------------------------------

def _build_power_asset_table(month: int, year: int) -> list:
    """
    Build unified power asset table from AssetAvailability + OperationalHours.

    Returns list of dicts per available asset:
        asset_id, asset_name, asset_type, op_hours, min_mw, max_mw,
        fixed_max_mw, priority, mandatory, min_mwh, max_mwh
    """
    cache_key = ("power_assets", month, year)
    if cache_key in _DB_CACHE:
        return _DB_CACHE[cache_key]

    assets_raw = fetch_asset_availability_with_hours(month, year)
    result = []
    for a in assets_raw:
        op_hours = a.get("operational_hours", 0.0)
        if op_hours is None or float(op_hours) <= 0:
            continue

        op_hours = float(op_hours)
        min_mw = float(a.get("min_operating_capacity") or 0.0)
        max_mw = float(a.get("max_operating_capacity") or 0.0)
        fixed_max_mw = float(a.get("fixed_max") or max_mw)
        priority = a.get("priority")
        if priority is None:
            priority = 999
        priority = int(priority)

        result.append({
            "asset_id":    a["asset_id"],
            "asset_name":  a["asset_name"],
            "asset_type":  a.get("asset_type", ""),
            "op_hours":    op_hours,
            "min_mw":      min_mw,
            "max_mw":      max_mw,
            "fixed_max_mw": fixed_max_mw,
            "priority":    priority,
            "mandatory":   0,  # NMD AssetAvailability has no Man_Load column
            "min_mwh":     min_mw * op_hours,
            "max_mwh":     max_mw * op_hours,
        })
    _DB_CACHE[cache_key] = result
    return result


def _get_power_demand(plant_id: str, month: int, year: int, demands: dict = None) -> dict:
    """
    Compute total power demand (process + fixed) in MWh.

    Process demand from DB is in kWh → convert to MWh.
    Fixed demand is already in MWh (converted in fetch_fixed_consumption).
    """
    if demands is not None:
        if "_power_process_mwh" in demands:
            process_mwh = float(demands["_power_process_mwh"])
            fixed_mwh = float(demands.get("_power_fixed_mwh", 0.0))
        elif "Power_Dis" in demands:
            total_mwh = float(demands["Power_Dis"])
            process_mwh = total_mwh
            fixed_mwh = 0.0
        else:
            process_kwh = float(demands.get("power_process", 0.0))
            fixed_mwh = float(demands.get("power_fixed", 0.0))
            process_mwh = process_kwh / 1000.0
    else:
        process = fetch_process_demands(plant_id, month, year)
        fixed = fetch_fixed_consumption(plant_id, month, year)
        process_kwh = float(process.get("power_process", 0.0))
        fixed_mwh = float(fixed.get("power_fixed", 0.0))
        process_mwh = process_kwh / 1000.0

    return {
        "process_mwh": round(process_mwh, 2),
        "fixed_mwh": round(fixed_mwh, 2),
        "total_mwh": round(process_mwh + fixed_mwh, 2),
    }


def _is_stg_asset(asset: dict) -> bool:
    """Check if asset is a Steam Turbine Generator."""
    name = str(asset.get("asset_name", "")).upper()
    atype = str(asset.get("asset_type", "")).upper()
    return "STG" in name or "STEAM TURBINE" in name or "STG" in atype


def _dispatch_all_min_first(
    assets: list,
    demand_mwh: float,
    norms_reader: NMDNormsReader,
    gt_curves: dict,
    stg_min_override_mwh: float = None,
    gt_reduction_mwh: float = 0.0,
) -> list:
    """
    Dispatch algorithm (all_min_first):
      1. All assets start at MIN load.
      2. If total MIN < demand → ramp up by priority.
      3. Equal-priority assets share additional load equally.
      4. If all at MAX and still deficit → deficit logged.
    """
    # Count non-STG assets for splitting GT reduction
    gt_count = sum(1 for a in assets if not _is_stg_asset(a))
    gt_reduction_per_asset = gt_reduction_mwh / gt_count if gt_count > 0 else 0.0

    dispatch = []
    for a in assets:
        d = {
            **a,
            "dispatched_mw": 0.0,
            "dispatched_mwh": 0.0,
        }
        # Apply GT reduction: reduce max capacity for non-STG assets (split across all GTs)
        if gt_reduction_per_asset > 0 and not _is_stg_asset(a):
            reduce_mw = gt_reduction_per_asset / a["op_hours"] if a["op_hours"] > 0 else 0.0
            d["max_mw"] = max(d["min_mw"], d["max_mw"] - reduce_mw)
            d["max_mwh"] = d["max_mw"] * d["op_hours"]
        dispatch.append(d)

    # Step 1: All assets at MIN (STG may have override)
    total_gen = 0.0
    for d in dispatch:
        if stg_min_override_mwh is not None and _is_stg_asset(d):
            override_mw = stg_min_override_mwh / d["op_hours"] if d["op_hours"] > 0 else 0.0
            d["dispatched_mw"] = min(override_mw, d["max_mw"])
            d["dispatched_mwh"] = d["dispatched_mw"] * d["op_hours"]
        else:
            d["dispatched_mw"] = d["min_mw"]
            d["dispatched_mwh"] = d["min_mwh"]
        total_gen += d["dispatched_mwh"]

    remaining = max(0.0, demand_mwh - total_gen)

    # Step 2: Ramp up by priority
    if remaining > 0:
        priority_groups = defaultdict(list)
        for i, d in enumerate(dispatch):
            if d["dispatched_mwh"] > 0 or d["mandatory"] == 1:
                priority_groups[d["priority"]].append(i)

        for pri in sorted(priority_groups.keys()):
            if remaining <= 0:
                break

            group_indices = priority_groups[pri]
            group_headroom = []
            for idx in group_indices:
                d = dispatch[idx]
                headroom_mw = d["max_mw"] - d["dispatched_mw"]
                if headroom_mw > 0.001:
                    group_headroom.append((idx, headroom_mw, d["op_hours"]))

            if not group_headroom:
                continue

            total_group_headroom_mwh = sum(h_mw * hrs for _, h_mw, hrs in group_headroom)
            allocation_mwh = min(remaining, total_group_headroom_mwh)

            if len(group_headroom) == 1:
                idx, headroom_mw, hrs = group_headroom[0]
                add_mw = min(allocation_mwh / hrs, headroom_mw)
                dispatch[idx]["dispatched_mw"] += add_mw
                dispatch[idx]["dispatched_mwh"] = dispatch[idx]["dispatched_mw"] * hrs
                remaining -= add_mw * hrs
            else:
                uncapped = [(idx, h_mw, hrs) for idx, h_mw, hrs in group_headroom]
                remaining_to_allocate = allocation_mwh

                while uncapped and remaining_to_allocate > 0.001:
                    total_hrs = sum(hrs for _, _, hrs in uncapped)
                    if total_hrs <= 0:
                        break
                    target_add_mw = remaining_to_allocate / total_hrs

                    newly_capped = []
                    for idx, headroom_mw, hrs in uncapped:
                        if target_add_mw >= headroom_mw:
                            dispatch[idx]["dispatched_mw"] += headroom_mw
                            dispatch[idx]["dispatched_mwh"] = dispatch[idx]["dispatched_mw"] * hrs
                            remaining_to_allocate -= headroom_mw * hrs
                            newly_capped.append(idx)
                        else:
                            dispatch[idx]["dispatched_mw"] += target_add_mw
                            dispatch[idx]["dispatched_mwh"] = dispatch[idx]["dispatched_mw"] * hrs
                            remaining_to_allocate -= target_add_mw * hrs

                    if newly_capped:
                        uncapped = [(i, h, hrs) for i, h, hrs in uncapped if i not in newly_capped]
                    else:
                        break

                remaining -= (allocation_mwh - remaining_to_allocate)

    # Calculate derived fields
    powergen_norms = norms_reader.get_powergen_norms()

    for d in dispatch:
        d["load_percent"] = round(
            (d["dispatched_mw"] / d["max_mw"] * 100) if d["max_mw"] > 0 else 0.0, 1
        )
        d["dispatched_mw"] = round(d["dispatched_mw"], 4)
        d["dispatched_mwh"] = round(d["dispatched_mwh"], 2)
        d["avg_load_mw"] = round(
            d["dispatched_mwh"] / d["op_hours"] if d["op_hours"] > 0 else 0.0, 2
        )

        # Free Steam MT — only for GT assets
        # Use both asset_type and name-based detection (NMD assets may have empty asset_type)
        is_gt = "GT" in d["asset_type"].upper() or _canonical_gt_name(d["asset_name"]) is not None
        if is_gt and d["dispatched_mwh"] > 0:
            canonical = _canonical_gt_name(d["asset_name"])
            curve = gt_curves.get(canonical) if canonical else None
            if curve:
                d["heat_rate"] = _get_heat_rate(d["avg_load_mw"], curve)
                d["free_steam_factor"] = _get_free_steam_factor(d["avg_load_mw"], curve)
                d["free_steam_mt"] = round(d["dispatched_mwh"] * d["free_steam_factor"], 2)
            else:
                d["heat_rate"] = 0.0
                d["free_steam_factor"] = DEFAULT_FREE_STEAM_FACTOR
                d["free_steam_mt"] = round(d["dispatched_mwh"] * DEFAULT_FREE_STEAM_FACTOR, 2)
                logger.warning(
                    "  [DISPATCH] No heat rate curve for '%s'; using default free steam factor %.2f",
                    d["asset_name"], DEFAULT_FREE_STEAM_FACTOR,
                )
        else:
            d["heat_rate"] = 0.0
            d["free_steam_factor"] = 0.0
            d["free_steam_mt"] = 0.0

        # Aux power norm from norms reader
        norm_val = powergen_norms.get(d["asset_name"].upper())
        if norm_val is None:
            norm_val = next(
                (v for k, v in powergen_norms.items()
                 if k in d["asset_name"].upper() or d["asset_name"].upper() in k),
                None,
            )
        if norm_val is None:
            logger.warning("  [DISPATCH] No aux power norm for '%s'; using 0.0", d["asset_name"])
            norm_val = 0.0

        d["aux_power_norm"] = norm_val
        d["aux_power"] = round(d["dispatched_mwh"] * norm_val, 2)

    return dispatch


def _log_power_dispatch(demand: dict, dispatch: list, month: int, year: int):
    """Log power dispatch result in structured table format."""
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

    logger.info("  %-25s  %8s  %8s  %8s  %8s  %14s  %14s  %8s  %12s  %12s",
                "Asset", "Priority", "Min MW", "Max MW", "Hours", "Dispatched MW", "Dispatched MWh", "Load %", "Free Steam MT", "Aux Power MWh")
    logger.info("  %s  %s  %s  %s  %s  %s  %s  %s  %s  %s",
                "-" * 25, "-" * 8, "-" * 8, "-" * 8, "-" * 8, "-" * 14, "-" * 14, "-" * 8, "-" * 12, "-" * 12)
    for d in dispatch:
        logger.info("  %-25s  %8d  %8.2f  %8.2f  %8.2f  %14.2f  %14.2f  %7.1f%%  %12.2f  %12.2f",
                     d["asset_name"], d["priority"],
                     d["min_mw"], d["max_mw"], d["op_hours"],
                     d["dispatched_mw"], d["dispatched_mwh"], d["load_percent"],
                     d.get("free_steam_mt", 0), d.get("aux_power", 0))

    total_free_steam = sum(d.get("free_steam_mt", 0) for d in dispatch)
    total_aux = sum(d.get("aux_power", 0) for d in dispatch)
    logger.info("  %-25s  %8s  %8s  %8s  %8s  %14s  %14.2f  %8s  %12.2f  %12.2f",
                "TOTAL", "", "", "", "", "", total_gen, "", total_free_steam, total_aux)

    if surplus > 0:
        logger.info("  SURPLUS: %.2f MWh over-generation", surplus)
    elif deficit > 0:
        logger.info("  DEFICIT: %.2f MWh under-generation", deficit)
    else:
        logger.info("  Demand met")
    logger.info("  %s", sep)


def dispatch_power(
    plant_id: str,
    month: int,
    year: int,
    dispatch_mode: str = "all_min_first",
    demands: dict = None,
    norms_reader: NMDNormsReader = None,
    stg_min_override_mwh: float = None,
    gt_reduction_mwh: float = 0.0,
) -> dict:
    """
    Dispatch power generation assets for NMD plant/month.

    NMD-specific dispatch rules:
      1. STG dispatched first at min load (or override if excess SHP).
      2. GTs cover remaining power demand by priority.
      3. If GTs maxed out, STG ramps up to meet deficit.
      4. Excess SHP steam → STG increases to consume it, GTs reduced.

    Args:
        plant_id:               NMD plant UUID
        month:                  1-12
        year:                   calendar year
        dispatch_mode:          "all_min_first" (default)
        demands:                pre-fetched merged demands dict
        norms_reader:           pre-loaded NMDNormsReader
        stg_min_override_mwh:   force STG to at least this MWh (excess SHP)
        gt_reduction_mwh:       reduce GT max generation by this MWh

    Returns:
        {
            "demand_mwh":           float,
            "demand_detail":        dict,
            "total_generation_mwh": float,
            "surplus_mwh":          float,
            "deficit_mwh":          float,
            "dispatch_mode":        str,
            "assets":               list,
            "import_power":         dict,
        }
    """
    if norms_reader is None:
        norms_reader = NMDNormsReader.get_reader(month, year)

    # 1. Build asset table
    assets = _build_power_asset_table(month, year)
    if not assets:
        logger.warning("  [DISPATCH] No available power assets for %d/%d", month, year)
        return {
            "demand_mwh": 0.0,
            "demand_detail": {"process_mwh": 0.0, "fixed_mwh": 0.0, "total_mwh": 0.0},
            "total_generation_mwh": 0.0,
            "surplus_mwh": 0.0,
            "deficit_mwh": 0.0,
            "dispatch_mode": dispatch_mode,
            "assets": [],
            "import_power": fetch_import_power(plant_id, month, year),
            "message": "No available power assets",
        }

    # 2. Fetch power demand
    demand = _get_power_demand(plant_id, month, year, demands=demands)
    total_demand = demand["total_mwh"]

    # 3. Fetch import power FIRST — import is utilized before GT/STG
    imp_key = ("import_power", plant_id, month, year)
    if imp_key in _DB_CACHE:
        import_power = _DB_CACHE[imp_key]
    else:
        import_power = fetch_import_power(plant_id, month, year)
        _DB_CACHE[imp_key] = import_power
    import_mwh = float(import_power.get("total_mwh", 0.0)) if import_power.get("success") else 0.0

    # Net demand after import power
    net_demand = max(0.0, total_demand - import_mwh)
    logger.info("  [DISPATCH] Total demand: %.2f MWh, Import: %.2f MWh, Net for GT/STG: %.2f MWh",
                total_demand, import_mwh, net_demand)

    # 4. Fetch GT heat rate curves from DB
    fy = _fy_string(month, year)
    gt_key = ("gt_curves", fy)
    if gt_key in _DB_CACHE:
        gt_curves = _DB_CACHE[gt_key]
    else:
        gt_curves = fetch_gt_heat_rate_curves(fy)
        _DB_CACHE[gt_key] = gt_curves

    # 5. Run dispatch — GT/STG only cover net demand (after import)
    dispatch = _dispatch_all_min_first(
        assets, net_demand, norms_reader, gt_curves,
        stg_min_override_mwh=stg_min_override_mwh,
        gt_reduction_mwh=gt_reduction_mwh,
    )

    # 6. Compute totals
    total_gen = sum(d["dispatched_mwh"] for d in dispatch)
    total_supply = total_gen + import_mwh
    surplus = round(max(0.0, total_supply - total_demand), 2)
    deficit = round(max(0.0, total_demand - total_supply), 2)

    # 7. Log
    _log_power_dispatch(demand, dispatch, month, year)

    total_free_steam = round(sum(d.get("free_steam_mt", 0) for d in dispatch), 2)
    total_aux = round(sum(d.get("aux_power", 0.0) for d in dispatch), 2)

    return {
        "demand_mwh": total_demand,
        "demand_detail": demand,
        "total_generation_mwh": round(total_gen, 2),
        "total_free_steam_mt": total_free_steam,
        "total_aux_power_mwh": total_aux,
        "surplus_mwh": surplus,
        "deficit_mwh": deficit,
        "dispatch_mode": dispatch_mode,
        "assets": dispatch,
        "import_power": import_power,
        "message": (
            f"SURPLUS: {surplus:.2f} MWh" if surplus > 0
            else f"DEFICIT: {deficit:.2f} MWh" if deficit > 0
            else "Demand met"
        ),
    }


# ---------------------------------------------------------------------------
# Steam dispatch
# ---------------------------------------------------------------------------

def _build_steam_asset_table(power_result: dict) -> list:
    """
    Build steam asset table from SteamGenerationAssets + GT-HRSG interlinking.

    NMD uses SteamGenerationAssets with inline MinCapacityMT/MaxCapacityMT.
    HRSGs inherit operational hours and priority from their linked GT.
    """
    steam_key = ("steam_assets",)
    if steam_key in _DB_CACHE:
        steam_assets = _DB_CACHE[steam_key]
    else:
        steam_assets = fetch_steam_generation_assets()
        _DB_CACHE[steam_key] = steam_assets
    power_assets = power_result.get("assets", [])

    dispatch_assets = []
    for asset in steam_assets:
        aname = asset.get("asset_name", "")
        atype = asset.get("asset_type", "")
        stype = asset.get("steam_type", "")

        # Only dispatch HRSGs producing SHP steam
        if atype != "HRSG" or stype != "SHP":
            continue

        # GT-HRSG interlinking
        linked_gt = _resolve_linked_gt(aname, power_assets)
        if linked_gt:
            op_hours = float(linked_gt.get("op_hours", 0.0))
            priority = int(linked_gt.get("priority", 999))
            if linked_gt.get("dispatched_mwh", 0.0) <= 0.0:
                op_hours = 0.0
            free_steam_mt = float(linked_gt.get("free_steam_mt", 0.0))
        else:
            op_hours = 0.0
            priority = 999
            free_steam_mt = 0.0

        min_tph = float(asset.get("min_capacity_mt", 0.0))
        max_tph = float(asset.get("max_capacity_mt", 0.0))

        dispatch_assets.append({
            "asset_id":       asset["asset_id"],
            "asset_name":     aname,
            "asset_type":     atype,
            "op_hours":       op_hours,
            "min_tph":        min_tph,
            "max_tph":        max_tph,
            "orig_min_tph":   min_tph,
            "orig_max_tph":   max_tph,
            "priority":       priority,
            "mandatory":      0,
            "free_steam_mt":  free_steam_mt,
            "linked_gt":      linked_gt.get("asset_name") if linked_gt else None,
            "dispatched_tph": 0.0,
            "dispatched_mt":  0.0,
            "total_output_mt": 0.0,
        })

    return dispatch_assets


def _dispatch_steam_assets(
    dispatch_assets: list,
    net_shp_to_dispatch: float,
) -> None:
    """
    Dispatch steam assets (HRSGs) to meet net SHP demand after free steam.

    Same priority-based algorithm as power dispatch:
      1. All at MIN, 2. Ramp up by priority, 3. Equal-priority share equally.
    """
    # Reset
    for a in dispatch_assets:
        a["dispatched_tph"] = 0.0
        a["dispatched_mt"] = 0.0

    if net_shp_to_dispatch <= 0:
        return

    # Step 1: All available at MIN
    total_min_mt = 0.0
    for a in dispatch_assets:
        if a["op_hours"] > 0:
            a["dispatched_tph"] = a["min_tph"]
            a["dispatched_mt"] = a["min_tph"] * a["op_hours"]
            total_min_mt += a["dispatched_mt"]

    remaining = max(0.0, net_shp_to_dispatch - total_min_mt)

    # Step 2: Ramp up by priority
    if remaining > 0:
        priority_groups = defaultdict(list)
        for idx, a in enumerate(dispatch_assets):
            if a["op_hours"] > 0 and a["dispatched_mt"] > 0:
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
                add_tph = min(allocation_mt / hrs, headroom_tph)
                dispatch_assets[idx]["dispatched_tph"] += add_tph
                dispatch_assets[idx]["dispatched_mt"] = dispatch_assets[idx]["dispatched_tph"] * hrs
                remaining -= add_tph * hrs
            else:
                uncapped = [(idx, h_tph, hrs) for idx, h_tph, hrs in group_headroom]
                remaining_to_allocate = allocation_mt

                while uncapped and remaining_to_allocate > 0.001:
                    total_hrs = sum(hrs for _, _, hrs in uncapped)
                    if total_hrs <= 0:
                        break
                    target_add_tph = remaining_to_allocate / total_hrs

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


def _log_steam_dispatch(demand_details: dict, dispatch: list, month: int, year: int, free_steam: float):
    """Log steam dispatch result."""
    top_grade = demand_details.get("_top_grade", "shp")
    cascade_grades = demand_details.get("_cascade_grades", ["lp", "mp", "hp", "shp"])
    total_demand = demand_details.get(f"{top_grade}_net", 0.0)
    total_gen = sum(d["dispatched_mt"] for d in dispatch) + free_steam
    surplus = max(0.0, total_gen - total_demand)
    deficit = max(0.0, total_demand - total_gen)

    sep = "=" * 78
    logger.info("  %s", sep)
    logger.info("  STEAM DISPATCH  (%s %d)", _MONTH_NAMES.get(month, ""), year)
    logger.info("  %s", sep)

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
    logger.info("")

    logger.info("  FINAL SUPPLEMENTARY FIRING DISPATCH")
    logger.info("  %-25s  %8s  %10s  %10s  %8s  %14s  %14s  %8s",
                "Asset", "Priority", "Min TPH", "Max TPH", "Hours", "Dispatched TPH", "Dispatched MT", "Load %")
    logger.info("  %s  %s  %s  %s  %s  %s  %s  %s",
                "-" * 25, "-" * 8, "-" * 10, "-" * 10, "-" * 8, "-" * 14, "-" * 14, "-" * 8)
    for d in dispatch:
        logger.info("  %-25s  %8d  %10.2f  %10.2f  %8.2f  %14.2f  %14.2f  %7.1f%%",
                     d["asset_name"], d["priority"],
                     d["min_tph"], d["max_tph"], d["op_hours"],
                     d["dispatched_tph"], d["dispatched_mt"], d.get("load_percent", 0))

    supp_gen = sum(d["dispatched_mt"] for d in dispatch)
    total_steam = sum(d.get("total_output_mt", 0) for d in dispatch)
    logger.info("  %-25s  %8s  %10s  %10s  %8s  %14s  %14.2f  %8s",
                "TOTAL SUPPLEMENTARY", "", "", "", "", "", supp_gen, "")
    logger.info("  %-25s  %8s  %10s  %10s  %8s  %14s  %14.2f  %8s",
                "TOTAL STEAM GENERATED", "", "", "", "", "", total_steam, "")

    if surplus > 0:
        logger.info("  SURPLUS: %.2f MT", surplus)
    elif deficit > 0:
        logger.info("  DEFICIT: %.2f MT", deficit)
    else:
        logger.info("  Demand met")
    logger.info("  %s", sep)


def dispatch_steam(
    plant_id: str,
    month: int,
    year: int,
    power_result: dict,
    demands: dict = None,
    norms_reader: NMDNormsReader = None,
    stg_extraction: dict = None,
) -> dict:
    """
    Dispatch steam generation assets (HRSGs) to meet SHP demand.

    Args:
        plant_id:       NMD plant UUID
        month:          1-12
        year:           calendar year
        power_result:   result dict from dispatch_power() (for GT-HRSG interlinking)
        demands:        pre-fetched merged demands dict
        norms_reader:   pre-loaded NMDNormsReader
        stg_extraction: dict with LP/MP extraction from STG (supply side)
                       {"LP Steam_Dis": <MT>, "MP Steam_Dis": <MT>}

    Returns:
        {
            "demand_detail":              dict,
            "total_free_steam_mt":        float,
            "total_supplementary_generation_mt": float,
            "total_generation_mt":        float,
            "surplus_mt":                 float,
            "deficit_mt":                 float,
            "assets":                     list,
        }
    """
    if norms_reader is None:
        norms_reader = NMDNormsReader.get_reader(month, year)

    # 1. Fetch letdown norms and derive grade cascade
    letdown_norms = norms_reader.get_steam_letdown_norms()
    cascade = []
    if letdown_norms:
        # Build cascade from letdown norms (highest → lowest pressure)
        # Physical steam flow: SHP → HP → MP → LP
        # Expected keys: SHP_to_HP, HP_to_MP, MP_to_LP
        if "SHP_to_HP" in letdown_norms:
            cascade.append({"produces": "HP Steam_Dis", "consumes": "SHP Steam_Dis", "norm": letdown_norms["SHP_to_HP"]})
        if "HP_to_MP" in letdown_norms:
            cascade.append({"produces": "MP Steam_Dis", "consumes": "HP Steam_Dis", "norm": letdown_norms["HP_to_MP"]})
        if "MP_to_LP" in letdown_norms:
            cascade.append({"produces": "LP Steam_Dis", "consumes": "MP Steam_Dis", "norm": letdown_norms["MP_to_LP"]})

    if cascade:
        grade_prefixes = [
            step["produces"].replace(" Steam_Dis", "").replace("_Dis", "").lower()
            for step in cascade
        ]
        # Ensure all grades are included in correct order (highest to lowest pressure)
        # Physical order: SHP → HP → MP → LP
        all_grades = ["shp", "hp", "mp", "lp"]
        for grade in all_grades:
            if grade not in grade_prefixes:
                grade_prefixes.append(grade)
        # Sort by pressure (highest to lowest)
        grade_prefixes = sorted(grade_prefixes, key=lambda x: all_grades.index(x) if x in all_grades else 999)
        top_grade = "shp"
    else:
        grade_prefixes = ["lp", "mp", "hp", "shp"]
        top_grade = "shp"

    # 2. Fetch steam demands
    if demands is not None:
        raw_demands = {}
        for g in grade_prefixes:
            ods_key = f"{g.upper()} Steam_Dis"
            for kind in ("process", "fixed"):
                prefix_key = f"{g}_{kind}"
                if kind == "process":
                    val = float(demands.get(ods_key, demands.get(prefix_key, 0.0)))
                else:
                    val = float(demands.get(prefix_key, 0.0))
                raw_demands[prefix_key] = val
    else:
        process = fetch_process_demands(plant_id, month, year)
        fixed = fetch_fixed_consumption(plant_id, month, year)
        raw_demands = {
            **{f"{g}_process": float(process.get(f"{g}_process", 0.0)) for g in grade_prefixes},
            **{f"{g}_fixed": float(fixed.get(f"{g}_fixed", 0.0)) for g in grade_prefixes},
        }

    byproduct_norms = norms_reader.get_hrsg_byproduct_norms()

    # 3. Build steam asset table
    dispatch_assets = _build_steam_asset_table(power_result)

    # 4. Convert HRSG total-output min/max into supplementary-firing min/max
    for a in dispatch_assets:
        if a["op_hours"] > 0:
            fs_tph = a["free_steam_mt"] / a["op_hours"] if a["op_hours"] > 0 else 0.0
            a["min_tph"] = max(0.0, a["min_tph"] - fs_tph)
            a["max_tph"] = max(0.0, a["max_tph"] - fs_tph)
            a["free_steam_tph"] = fs_tph

    # 5. Convergence loop (5 iterations for byproduct recalculation)
    lowest_grade = grade_prefixes[0] if grade_prefixes else "lp"
    byproduct_low_steam = 0.0
    free_steam = float(power_result.get("total_free_steam_mt", 0.0))
    demand_details = {}

    # Initialize STG extraction supply
    stg_lp_supply = float(stg_extraction.get("LP Steam_Dis", 0.0)) if stg_extraction else 0.0
    stg_mp_supply = float(stg_extraction.get("MP Steam_Dis", 0.0)) if stg_extraction else 0.0
    
    # Initialize HRSG LP byproduct supply (negative norm means it's generation)
    hrsg_lp_byproduct_supply = 0.0

    for iteration in range(5):
        net_by_grade = {}
        letdown_by_grade = {}
        prev_letdown = 0.0

        for i, g in enumerate(grade_prefixes):
            # Calculate net demand for this grade
            # Start with process + fixed demand
            net = raw_demands.get(f"{g}_process", 0.0) + raw_demands.get(f"{g}_fixed", 0.0)
            
            # Add byproduct for lowest grade (LP)
            if i == 0:
                net += byproduct_low_steam
            
            # Add letdown from previous grade (cascading)
            
            # Subtract HRSG LP byproduct supply (HRSG produces LP as byproduct)
            if g == "lp":
                net -= hrsg_lp_byproduct_supply
            
            # Add letdown from previous grade (cascading)
            else:
                net += prev_letdown
            
            # Subtract STG extraction supply (STG produces LP/MP steam)
            if g == "lp":
                net -= stg_lp_supply
            elif g == "mp":
                net -= stg_mp_supply
            
            net_by_grade[g] = net

            if i < len(cascade):
                norm = cascade[i]["norm"]
                letdown = max(0.0, net) * norm
            else:
                letdown = 0.0
            letdown_by_grade[g] = letdown
            prev_letdown = letdown

        demand_details = {
            **raw_demands,
            f"{lowest_grade}_byproduct": byproduct_low_steam,
            **{f"{g}_letdown": letdown_by_grade[g] for g in grade_prefixes},
            **{f"{g}_net": round(net_by_grade[g], 2) for g in grade_prefixes},
            "_cascade_grades": grade_prefixes,
            "_top_grade": top_grade,
        }

        net_top = net_by_grade.get(top_grade, 0.0)
        net_shp_to_dispatch = max(0.0, net_top - free_steam)

        _dispatch_steam_assets(dispatch_assets, net_shp_to_dispatch)

        # Total HRSG output = free steam + supplementary
        for a in dispatch_assets:
            if a["asset_type"] == "HRSG":
                a["total_output_mt"] = a["dispatched_mt"] + a["free_steam_mt"]
            else:
                a["total_output_mt"] = a["dispatched_mt"]

        # Recalculate byproduct for lowest grade
        hrsg_lp_byproduct_supply = 0.0
        for a in dispatch_assets:
            norm_val = byproduct_norms.get(a["asset_name"].upper())
            if norm_val is None:
                norm_val = next(
                    (v for k, v in byproduct_norms.items()
                     if k in a["asset_name"].upper() or a["asset_name"].upper() in k),
                    None,
                )
            if norm_val is None:
                norm_val = 0.0
            hrsg_lp_byproduct_supply += a["total_output_mt"] * norm_val
        
        # Keep byproduct_low_steam for backward compatibility in demand_details
        byproduct_low_steam = hrsg_lp_byproduct_supply

    # Final output computation
    for a in dispatch_assets:
        if a["asset_type"] == "HRSG":
            a["total_output_mt"] = a["dispatched_mt"] + a["free_steam_mt"]
        else:
            a["total_output_mt"] = a["dispatched_mt"]

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

    _log_steam_dispatch(demand_details, dispatch_assets, month, year, free_steam)

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
        "assets": dispatch_assets,
        "message": (
            f"SURPLUS: {surplus_mt:.2f} MT" if surplus_mt > 0
            else f"DEFICIT: {deficit_mt:.2f} MT" if deficit_mt > 0
            else "Demand met"
        ),
    }
