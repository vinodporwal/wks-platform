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

import os
import logging
from collections import defaultdict

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
from engine.ods_norms_reader import ODSNormsReader

logger = logging.getLogger(__name__)

# Free steam factor: GT gross kWh × factor = free steam MT
# Hardcoded for now; will be replaced by per-load lookup from CPP_GTHeatRate table
DEFAULT_FREE_STEAM_FACTOR = 1.97

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


def _get_steam_demands(plant_id: str, month: int, year: int, demands: dict = None) -> dict:
    """
    Build steam demand dict from pre-fetched demands or DB fallback.

    Args:
        plant_id:  CPP plant UUID
        month:     1-12
        year:      calendar year
        demands:   pre-fetched merged demands dict from calculator. If provided,
                   skips DB round-trip for process + fixed consumption.
    """
    if demands is not None:
        return {
            "lp_process": float(demands.get("lp_process", 0.0)),
            "lp_fixed": float(demands.get("lp_fixed", 0.0)),
            "mp_process": float(demands.get("mp_process", 0.0)),
            "mp_fixed": float(demands.get("mp_fixed", 0.0)),
            "hp_process": float(demands.get("hp_process", 0.0)),
            "hp_fixed": float(demands.get("hp_fixed", 0.0)),
            "shp_process": float(demands.get("shp_process", 0.0)),
            "shp_fixed": float(demands.get("shp_fixed", 0.0)),
        }

    process = fetch_process_demands(plant_id, month, year)
    fixed = fetch_fixed_consumption(plant_id, month, year)
    return {
        "lp_process": float(process.get("lp_process", 0.0)),
        "lp_fixed": float(fixed.get("lp_fixed", 0.0)),
        "mp_process": float(process.get("mp_process", 0.0)),
        "mp_fixed": float(fixed.get("mp_fixed", 0.0)),
        "hp_process": float(process.get("hp_process", 0.0)),
        "hp_fixed": float(fixed.get("hp_fixed", 0.0)),
        "shp_process": float(process.get("shp_process", 0.0)),
        "shp_fixed": float(fixed.get("shp_fixed", 0.0)),
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

        result.append({
            "asset_id": aid,
            "asset_name": aname,
            "asset_type": atype,
            "op_hours": op_hours,
            "min_mw": min_mw,
            "max_mw": max_mw,
            "priority": priority,
            "mandatory": mandatory,
            "min_mwh": min_mw * op_hours,
            "max_mwh": max_mw * op_hours,
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
        process_kwh = float(demands.get("power_process", 0.0))
        fixed_mwh = float(demands.get("power_fixed", 0.0))
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


def _dispatch_all_min_first(
    assets: list,
    demand_mwh: float,
    plant_id: str,
    month: int,
    year: int,
    ods_reader: ODSNormsReader = None,
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

    # Fetch POWERGEN norms from ODS reader (or fallback to cached reader)
    if ods_reader is not None:
        excel_norms = ods_reader.get_powergen_norms()
    else:
        excel_norms = ODSNormsReader.get_reader(plant_id, month, year).get_powergen_norms()

    # Calculate derived fields
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
        # Formula: gross_mwh × free_steam_factor
        is_gt = "GT" in d["asset_type"].upper()
        if is_gt and d["dispatched_mwh"] > 0:
            d["free_steam_factor"] = DEFAULT_FREE_STEAM_FACTOR
            d["free_steam_mt"] = round(d["dispatched_mwh"] * DEFAULT_FREE_STEAM_FACTOR, 2)
        else:
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
            # Fallback values
            is_stg = "STG" in d["asset_name"].upper() or "STEAM" in d["asset_name"].upper()
            norm_val = 0.00705 if is_stg else 0.00135
            
        d["aux_power_norm"] = norm_val
        d["aux_power"] = round(d["dispatched_mwh"] * norm_val, 2)

    return dispatch


def _dispatch_one_by_one(
    assets: list,
    demand_mwh: float,
    plant_id: str,
    month: int,
    year: int,
    ods_reader: ODSNormsReader = None,
) -> list:
    """
    Dispatch algorithm: bring assets online one-by-one by priority.
    Only activate the next asset if the current one at MAX still can't meet demand.

    (PLACEHOLDER — to be implemented once confirmed by senior management)
    """
    # For now, fall back to all_min_first
    logger.warning("  [DISPATCH] one_by_one mode not yet implemented — falling back to all_min_first")
    return _dispatch_all_min_first(assets, demand_mwh, plant_id, month, year, ods_reader=ods_reader)


# ---------------------------------------------------------------------------
# Logging helpers
# ---------------------------------------------------------------------------

def _log_dispatch_result(demand: dict, dispatch: list, month: int, year: int):
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
    logger.info("  %-25s  %8s  %14s  %14s  %8s  %12s  %14s  %14s  %14s",
                "Asset", "Priority", "Dispatched MW", "Dispatched MWh", "Load %", "Avg Load MW", "Free Steam MT", "Aux Norm", "Aux Power MWh")
    logger.info("  %s  %s  %s  %s  %s  %s  %s  %s  %s",
                "-" * 25, "-" * 8, "-" * 14, "-" * 14, "-" * 8, "-" * 12, "-" * 14, "-" * 14, "-" * 14)
    for d in dispatch:
        logger.info("  %-25s  %8d  %14.2f  %14.2f  %7.1f%%  %12.2f  %14.2f  %14.6f  %14.2f",
                     d["asset_name"], d["priority"],
                     d["dispatched_mw"], d["dispatched_mwh"], d["load_percent"],
                     d.get("avg_load_mw", 0), d.get("free_steam_mt", 0),
                     d.get("aux_power_norm", 0.0), d.get("aux_power", 0.0))
    logger.info("  %-25s  %8s  %14s  %14.2f  %8s  %12s  %14.2f  %14s  %14.2f",
                "TOTAL", "", "", total_gen, "", "", total_free_steam, "", total_aux_power)
    logger.info("  %s", sep)


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

def dispatch_power(
    plant_id: str,
    month: int,
    year: int,
    dispatch_mode: str = "all_min_first",
    demands: dict = None,
    ods_reader: ODSNormsReader = None,
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
    # Ensure we have an ODS reader (cached if not provided)
    if ods_reader is None:
        ods_reader = ODSNormsReader.get_reader(plant_id, month, year)

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

    # 3. Run dispatch
    if dispatch_mode == "one_by_one":
        dispatch = _dispatch_one_by_one(assets, total_demand, plant_id, month, year, ods_reader=ods_reader)
    else:
        dispatch = _dispatch_all_min_first(assets, total_demand, plant_id, month, year, ods_reader=ods_reader)

    # 4. Compute totals
    total_gen = sum(d["dispatched_mwh"] for d in dispatch)
    surplus = round(max(0.0, total_gen - total_demand), 2)
    deficit = round(max(0.0, total_demand - total_gen), 2)

    # 5. Log the result
    _log_dispatch_result(demand, dispatch, month, year)

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
        "message": (
            f"SURPLUS: {surplus:.2f} MWh over-generation" if surplus > 0
            else f"DEFICIT: {deficit:.2f} MWh under-generation" if deficit > 0
            else "Demand met"
        ),
    }


def _log_steam_dispatch_result(demand_details: dict, dispatch: list, month: int, year: int, free_steam: float):
    """Print the full steam dispatch result to the log in a structured table format."""
    total_demand = demand_details["shp_net"]
    total_gen = sum(d["dispatched_mt"] for d in dispatch) + free_steam
    surplus = max(0.0, total_gen - total_demand)
    deficit = max(0.0, total_demand - total_gen)

    sep = "=" * 78
    logger.info("  %s", sep)
    logger.info("  STEAM DISPATCH  (%s %d)", _MONTH_NAMES.get(month, ""), year)
    logger.info("  %s", sep)
    
    # Raw & Net Demands table
    logger.info("  Grade      Process/Fixed MT  Letdown MT  Byproduct MT     Net Demand MT")
    logger.info("  ---------  ----------------  ----------  ------------  ----------------")
    for grade in ["LP", "MP", "HP", "SHP"]:
        g_lower = grade.lower()
        process_fixed = demand_details[f"{g_lower}_process"] + demand_details[f"{g_lower}_fixed"]
        letdown = demand_details.get(f"{g_lower}_letdown", 0.0)
        byprod = demand_details.get(f"{g_lower}_byproduct", 0.0)
        net = demand_details[f"{g_lower}_net"]
        logger.info("  %-9s  %16.2f  %10.2f  %12.2f  %16.2f",
                    grade, process_fixed, letdown, byprod, net)
    logger.info("")
    logger.info("  Total Free Steam available: %.2f MT", free_steam)
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
    logger.info("  %s", sep)


def dispatch_steam(
    plant_id: str,
    month: int,
    year: int,
    power_result: dict,
    dispatch_mode: str = "all_min_first",
    demands: dict = None,
    ods_reader: ODSNormsReader = None,
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
    # Ensure we have an ODS reader (cached if not provided)
    if ods_reader is None:
        ods_reader = ODSNormsReader.get_reader(plant_id, month, year)

    # 1. Fetch demands (use pre-fetched demands if available)
    raw_demands = _get_steam_demands(plant_id, month, year, demands=demands)
    
    # 2. Fetch norms from unified ODS reader
    letdown_norms = ods_reader.get_steam_letdown_norms()
    lp_to_mp_norm = letdown_norms.get("LP_to_MP", 0.945)
    mp_to_hp_norm = letdown_norms.get("MP_to_HP", 0.900)
    hp_to_shp_norm = letdown_norms.get("HP_to_SHP", 0.936)
    
    byproduct_norms = ods_reader.get_hrsg_byproduct_norms()
    
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
        if stype == "SHP Steam_Dis" and atype in ["HRSG", "AUXBOILER"]:
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
            
            dispatch_assets.append({
                "asset_id": asset_id,
                "asset_name": aname,
                "asset_type": atype,
                "op_hours": op_hours,
                "min_tph": min_tph,
                "max_tph": max_tph,
                "priority": priority,
                "mandatory": mandatory,
                "dispatched_tph": 0.0,
                "dispatched_mt": 0.0,
            })
            
    # 4. Convergence loop (5 iterations)
    byproduct_lp_steam = 0.0
    demand_details = {}
    free_steam = float(power_result.get("total_free_steam_mt", 0.0))
    
    for iteration in range(5):
        # LP demand sequential letdown
        # LP byproduct reduces the LP net demand (norm is negative, so byproduct_lp_steam is negative)
        lp_net = raw_demands["lp_process"] + raw_demands["lp_fixed"] + byproduct_lp_steam
        
        lp_letdown = max(0.0, lp_net) * lp_to_mp_norm
        mp_net = raw_demands["mp_process"] + raw_demands["mp_fixed"] + lp_letdown
        
        mp_letdown = max(0.0, mp_net) * mp_to_hp_norm
        hp_net = raw_demands["hp_process"] + raw_demands["hp_fixed"] + mp_letdown
        
        hp_letdown = max(0.0, hp_net) * hp_to_shp_norm
        shp_net = raw_demands["shp_process"] + raw_demands["shp_fixed"] + hp_letdown
        
        demand_details = {
            **raw_demands,
            "lp_byproduct": byproduct_lp_steam,
            "lp_letdown": lp_letdown,
            "mp_letdown": mp_letdown,
            "hp_letdown": hp_letdown,
            "lp_net": round(lp_net, 2),
            "mp_net": round(mp_net, 2),
            "hp_net": round(hp_net, 2),
            "shp_net": round(shp_net, 2),
        }
        
        # Remaining SHP demand to be met by steam assets
        net_shp_to_dispatch = max(0.0, shp_net - free_steam)
        
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
                        
        # Recalculate byproduct LP steam
        byproduct_lp_steam = 0.0
        for a in dispatch_assets:
            norm_val = byproduct_norms.get(a["asset_name"].upper())
            if norm_val is None:
                norm_val = next(
                    (v for k, v in byproduct_norms.items() if k in a["asset_name"].upper() or a["asset_name"].upper() in k),
                    None
                )
            if norm_val is None:
                # Default fallback for HRSGs
                norm_val = -0.15 if "HRSG" in a["asset_name"].upper() else 0.0
            byproduct_lp_steam += a["dispatched_mt"] * norm_val
            
    # Calculate load percentages and round fields
    for a in dispatch_assets:
        a["load_percent"] = round(
            (a["dispatched_tph"] / a["max_tph"] * 100) if a["max_tph"] > 0 else 0.0, 1
        )
        a["dispatched_tph"] = round(a["dispatched_tph"], 4)
        a["dispatched_mt"] = round(a["dispatched_mt"], 2)
        a["min_tph"] = round(a["min_tph"], 2)
        a["max_tph"] = round(a["max_tph"], 2)
        
    # Log results
    _log_steam_dispatch_result(demand_details, dispatch_assets, month, year, free_steam)
    
    total_supp_gen = round(sum(a["dispatched_mt"] for a in dispatch_assets), 2)
    total_steam_gen = round(total_supp_gen + free_steam, 2)
    surplus_mt = round(max(0.0, total_steam_gen - shp_net), 2)
    deficit_mt = round(max(0.0, shp_net - total_steam_gen), 2)
    
    return {
        "demand_detail": demand_details,
        "total_free_steam_mt": free_steam,
        "total_supplementary_generation_mt": total_supp_gen,
        "total_generation_mt": total_steam_gen,
        "surplus_mt": surplus_mt,
        "deficit_mt": deficit_mt,
        "assets": dispatch_assets,
        "message": (
            f"SURPLUS: {surplus_mt:.2f} MT over-generation" if surplus_mt > 0
            else f"DEFICIT: {deficit_mt:.2f} MT under-generation" if deficit_mt > 0
            else "Demand met"
        ),
    }


def _find_linked_gt_asset(hrsg_name: str, power_assets: list) -> dict:
    import re
    m_hrsg = re.search(r'HRSG\s*(\d+)', hrsg_name, re.IGNORECASE)
    if not m_hrsg:
        return None
    hrsg_num = m_hrsg.group(1)
    
    for gt in power_assets:
        gt_name = gt["asset_name"]
        m_gt = re.search(r'GT[G]?\s*[-_]?\s*(\d+)', gt_name, re.IGNORECASE)
        if m_gt and m_gt.group(1) == hrsg_num:
            return gt
    return None

