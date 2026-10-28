"""
Fetch plant info, assets, and operational hours for one or more JMD plants.

Usage (from apps/python/JMD/):
    python3 test_plant_fetch.py [<plant_id> ...] [month] [year]

Examples:
    python3 test_plant_fetch.py F6D82E68-C3B6-494F-9905-48F19DC611E3
    python3 test_plant_fetch.py F6D82E68-C3B6-494F-9905-48F19DC611E3 4 2025
    python3 test_plant_fetch.py F6D82E68-C3B6-494F-9905-48F19DC611E3 2DFEE33F-4CFD-4887-B9DD-53388AA95271 4 2025

Available plant IDs:
    F6D82E68-C3B6-494F-9905-48F19DC611E3  DTA-PCG-CPP
    2DFEE33F-4CFD-4887-B9DD-53388AA95271  SEZ-CPP
    D2C7FBAD-7E00-4642-B3B2-5A768FAC8D45  SEZ-PCG-CPP
    A4AF8441-73AD-4F9F-BCF4-6734E8202F7A  DTA-CPP
    BA558F95-8A3F-4769-9C78-FF7B6C639DDF  C2-CPP
"""

import sys
import os
import logging

sys.path.insert(0, os.path.dirname(__file__))

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s  %(levelname)s  %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger(__name__)

from database.queries import (
    fetch_plant_info,
    fetch_plant_assets,
    fetch_asset_operational_hours,
    fetch_asset_operational_hours_all_months,
    fetch_asset_priority,
    fetch_asset_priority_all_months,
    fetch_steam_asset_priority,
    fetch_steam_asset_priority_all_months,
    fetch_steam_generation_assets,
    fetch_plant_power_info,
    fetch_power_asset_capacity_all_months,
    fetch_process_demands,
    fetch_process_demand_master,
    fetch_fixed_consumption,
)
from engine.calculator import run_full_year, run_month

# ─── CLI args ─────────────────────────────────────────────────────────────────
# Usage: test_plant_fetch.py [plant_id ...] [month] [year]
# Plant IDs are UUID-shaped (contain '-'); month/year are plain integers.
MONTH        = 4
YEAR         = 2025
MONTH_GIVEN  = False

_args = sys.argv[1:]
_explicit_plants: list[str] = []

# Peel trailing integer args (year then month, rightmost first)
if _args and _args[-1].isdigit() and len(_args[-1]) == 4:
    YEAR  = int(_args.pop())
if _args and _args[-1].isdigit():
    MONTH = int(_args.pop())
    MONTH_GIVEN = True

_explicit_plants = _args  # whatever remains are plant IDs
# ──────────────────────────────────────────────────────────────────────────────

from plant_mapper import PLANT_REGISTRY

PLANT_IDS = _explicit_plants if _explicit_plants else list(PLANT_REGISTRY.keys())
# Keep PLANT_ID for backward-compat (truthy only when exactly one plant given)
PLANT_ID = _explicit_plants[0] if len(_explicit_plants) == 1 else None


def section(title: str):
    logger.info("")
    logger.info("=" * 60)
    logger.info("  %s", title)
    logger.info("=" * 60)


def test_plant_info(plant_id: str):
    section("Plant Info  (Plants table)")
    info = fetch_plant_info(plant_id)
    if info:
        logger.info("  plant_id:                  %s", info['plant_id'])
        logger.info("  name:                      %s", info['name'])
        logger.info("  display_name:              %s", info['display_name'])
        logger.info("  plant_code:                %s", info['plant_code'])
        logger.info("  is_active:                 %s", info['is_active'])
        logger.info("  business_category:         %s", info['business_category'])
        logger.info("  business_category_display: %s", info['business_category_display'])
    else:
        logger.info("  NOT FOUND in Plants table")


def test_plant_assets_list(plant_id: str):
    section("Plant Assets  (PowerGenerationAssets)")
    assets = fetch_plant_assets(plant_id)
    if not assets:
        logger.info("  No assets found")
        return
    logger.info("  %d asset(s):", len(assets))
    logger.info("  %-25s  %-12s  %-25s  %-38s  %s",
                "Asset", "Type", "DisplayName", "UtilityGen FK", "UtilityDist FK")
    logger.info("  %s  %s  %s  %s  %s", "-"*25, "-"*12, "-"*25, "-"*38, "-"*38)
    for a in assets:
        logger.info("  %-25s  %-12s  %-25s  %-38s  %s",
                    a['asset_name'], a['asset_type'] or '-',
                    a['display_name'] or '-',
                    a['utility_generation_fk'] or '-',
                    a['utility_distributed_fk'] or '-')


def test_asset_ops_hours(plant_id: str):
    section(f"Asset Operational Hours  (CPPAssetOperationalHours)  {MONTH}/{YEAR}")
    rows = fetch_asset_operational_hours(plant_id, MONTH, YEAR)
    if not rows:
        logger.info("  No rows found")
        return
    logger.info("  %d row(s)  [aop_year=%s]", len(rows), rows[0]['aop_year'])
    logger.info("  %-25s  %-12s  %-20s  %-20s  %8s",
                "Asset", "Type", "Utility Dist", "Utility Gen", "Hours")
    logger.info("  %s  %s  %s  %s  %s", "-"*25, "-"*12, "-"*20, "-"*20, "-"*8)
    for r in rows:
        hrs = f"{r['operational_hours']:.2f}" if r['operational_hours'] is not None else "N/A"
        logger.info("  %-25s  %-12s  %-20s  %-20s  %8s",
                    r['asset_name'], r['asset_type'] or '-',
                    r['utility_distributed'] or '-',
                    r['utility_generated'] or '-', hrs)


def test_asset_priority(plant_id: str):
    section(f"Asset Priority  (CPPPowerAssetPriority)  {MONTH}/{YEAR}")
    rows = fetch_asset_priority(plant_id, MONTH, YEAR)
    if not rows:
        logger.info("  No rows found")
        return
    logger.info("  %d asset(s)  [aop_year=%s]", len(rows), rows[0]['aop_year'])
    logger.info("  %-25s  %-12s  %8s", "Asset", "Type", "Priority")
    logger.info("  %s  %s  %s", "-"*25, "-"*12, "-"*8)
    for r in rows:
        pri = str(r['priority']) if r['priority'] is not None else "N/A"
        logger.info("  %-25s  %-12s  %8s", r['asset_name'], r['asset_type'] or '-', pri)


def test_steam_asset_priority(plant_id: str):
    section(f"Steam Asset Priority  (CPPSteamAssetsPriority)  {MONTH}/{YEAR}")
    rows = fetch_steam_asset_priority(plant_id, MONTH, YEAR)
    if not rows:
        logger.info("  No rows found")
        return
    logger.info("  %d asset(s)  [aop_year=%s]", len(rows), rows[0]['aop_year'])
    logger.info("  %-25s  %-12s  %-12s  %8s", "Asset", "Type", "SteamType", "Priority")
    logger.info("  %s  %s  %s  %s", "-"*25, "-"*12, "-"*12, "-"*8)
    for r in rows:
        pri = str(r['priority']) if r['priority'] is not None else "N/A"
        logger.info("  %-25s  %-12s  %-12s  %8s",
                    r['asset_name'], r['asset_type'] or '-',
                    r['steam_type'] or '-', pri)


def test_steam_generation_assets(plant_id: str):
    section("Steam Generation Assets  (CPPSteamGenerationAsset)")
    rows = fetch_steam_generation_assets(plant_id)
    if not rows:
        logger.info("  No rows found")
        return
    logger.info("  %d asset(s):", len(rows))
    logger.info("  %-25s  %-12s  %-12s  %-8s  %-8s  %-25s",
                "Asset", "Type", "SteamType", "Visible", "Editable", "DisplayName")
    logger.info("  %s  %s  %s  %s  %s  %s",
                "-"*25, "-"*12, "-"*12, "-"*8, "-"*8, "-"*25)
    for r in rows:
        logger.info("  %-25s  %-12s  %-12s  %-8s  %-8s  %-25s",
                    r['asset_name'], r['asset_type'] or '-',
                    r['steam_type'] or '-',
                    str(r['is_visible']), str(r['is_editable']),
                    r['display_name'] or '-')


def test_plant_power_info(plant_id: str):
    section(f"Plant Power Info  (combined)  {MONTH}/{YEAR}")
    data = fetch_plant_power_info(plant_id, MONTH, YEAR)

    p = data.get("plant", {})
    s = data.get("summary", {})

    logger.info("  Plant:  %s  (%s)", p.get('name'), p.get('display_name'))
    logger.info("  Code:   %s   Active: %s", p.get('plant_code'), p.get('is_active'))
    logger.info("  AOP Year:               %s", s.get('aop_year'))
    logger.info("  Total Assets:           %s", s.get('total_assets'))
    logger.info("  Assets with Hours:      %s", s.get('assets_with_hours'))
    logger.info("  Total Operational Hrs:  %s", s.get('total_operational_hours'))

    hours = data.get("hours", [])
    if hours:
        logger.info("  %-25s  %-12s  %8s  %s", "Asset", "Type", "Hrs", "Utility")
        logger.info("  %s  %s  %s  %s", "-"*25, "-"*12, "-"*8, "-"*7)
        for h in hours:
            hrs = f"{h['operational_hours']:.2f}" if h['operational_hours'] is not None else "  N/A  "
            util = h.get('utility_generated') or h.get('utility_distributed') or "-"
            logger.info("  %-25s  %-12s  %8s  %s",
                        h['asset_name'], h['asset_type'] or '-', hrs, util)


_ALL_MONTHS = ["Apr", "May", "Jun", "Jul", "Aug", "Sep",
               "Oct", "Nov", "Dec", "Jan", "Feb", "Mar"]


def test_asset_ops_hours_all_months(plant_id: str):
    fy_year = YEAR if MONTH >= 4 else YEAR - 1
    fy = f"{fy_year}-{str(fy_year + 1)[-2:]}"
    section(f"Asset Operational Hours — All Months  (FY {fy})")
    rows = fetch_asset_operational_hours_all_months(plant_id, fy_year)
    if not rows:
        logger.info("  No rows found")
        return
    logger.info("  %d asset(s)  [aop_year=%s]", len(rows), rows[0]['aop_year'])
    header = f"  {'Asset':<25}  {'Type':<12}" + "".join(f"  {m:>5}" for m in _ALL_MONTHS) + "  Total"
    logger.info(header)
    logger.info("  %s  %s%s  %s", "-"*25, "-"*12, "  -----" * 12, "-------")
    for r in rows:
        vals = "".join(
            f"  {r[m]:>5.0f}" if r[m] is not None else "    N/A"
            for m in _ALL_MONTHS
        )
        logger.info("  %-25s  %-12s%s  %7.0f",
                    r['asset_name'], r['asset_type'] or '-', vals, r['total_hours'])


def test_asset_priority_all_months(plant_id: str):
    fy_year = YEAR if MONTH >= 4 else YEAR - 1
    fy = f"{fy_year}-{str(fy_year + 1)[-2:]}"
    section(f"Power Asset Priority — All Months  (FY {fy})")
    rows = fetch_asset_priority_all_months(plant_id, fy_year)
    if not rows:
        logger.info("  No rows found")
        return
    logger.info("  %d asset(s)  [aop_year=%s]", len(rows), rows[0]['aop_year'])
    header = f"  {'Asset':<25}  {'Type':<12}" + "".join(f"  {m:>4}" for m in _ALL_MONTHS)
    logger.info(header)
    logger.info("  %s  %s%s", "-"*25, "-"*12, "  ----" * 12)
    for r in rows:
        vals = "".join(
            f"  {r[m]:>4}" if r[m] is not None else "   N/A"
            for m in _ALL_MONTHS
        )
        logger.info("  %-25s  %-12s%s", r['asset_name'], r['asset_type'] or '-', vals)


def test_steam_asset_priority_all_months(plant_id: str):
    fy_year = YEAR if MONTH >= 4 else YEAR - 1
    fy = f"{fy_year}-{str(fy_year + 1)[-2:]}"
    section(f"Steam Asset Priority — All Months  (FY {fy})")
    rows = fetch_steam_asset_priority_all_months(plant_id, fy_year)
    if not rows:
        logger.info("  No rows found")
        return
    logger.info("  %d asset(s)  [aop_year=%s]", len(rows), rows[0]['aop_year'])
    header = f"  {'Asset':<25}  {'Type':<12}  {'SteamType':<12}" + "".join(f"  {m:>4}" for m in _ALL_MONTHS)
    logger.info(header)
    logger.info("  %s  %s  %s%s", "-"*25, "-"*12, "-"*12, "  ----" * 12)
    for r in rows:
        vals = "".join(
            f"  {r[m]:>4}" if r[m] is not None else "   N/A"
            for m in _ALL_MONTHS
        )
        logger.info("  %-25s  %-12s  %-12s%s",
                    r['asset_name'], r['asset_type'] or '-',
                    r['steam_type'] or '-', vals)


def test_process_demand_master(plant_id: str):
    section("Process Demand Master  (ProcessDemandMaster)")
    rows = fetch_process_demand_master(plant_id)
    if not rows:
        logger.info("  No active rows found for this plant")
        return
    logger.info("  %d active row(s):", len(rows))
    logger.info("  %-30s  %-20s  %-10s  %s",
                "Process Plant", "CPP Utility", "UOM", "Order")
    logger.info("  %s  %s  %s  %s", "-"*30, "-"*20, "-"*10, "-"*5)
    for r in rows:
        logger.info("  %-30s  %-20s  %-10s  %s",
                    r["process_plant"], r["cpp_utility"],
                    r["uom"], r["display_order"])


def test_demand_segregation(plant_id: str):
    section(f"Demand Segregation  (Process + Fixed)  {MONTH}/{YEAR}")

    process = fetch_process_demands(plant_id, MONTH, YEAR)
    fixed   = fetch_fixed_consumption(plant_id, MONTH, YEAR)
    merged  = {**process, **fixed}

    # Demand metadata for display
    _META = {
        "power_process":     ("Power (Process)",      "KWH"),
        "power_fixed":       ("Power (Fixed)",         "MWh"),
        "lp_process":        ("LP Steam (Process)",    "MT"),
        "lp_fixed":          ("LP Steam (Fixed)",      "MT"),
        "mp_process":        ("MP Steam (Process)",    "MT"),
        "mp_fixed":          ("MP Steam (Fixed)",      "MT"),
        "hp_process":        ("HP Steam (Process)",    "MT"),
        "hp_fixed":          ("HP Steam (Fixed)",      "MT"),
        "shp_process":       ("SHP Steam (Process)",   "MT"),
        "shp_fixed":         ("SHP Steam (Fixed)",     "MT"),
        "air_process":       ("Compressed Air",        "NM3"),
        "nitrogen_asu_process": ("Nitrogen (ASU)",     "NM3"),
        "dm_process":        ("DM Water",              "M3"),
        "cw1_process":       ("Cooling Water 1",       "KM3"),
        "cw2_process":       ("Cooling Water 2",       "KM3"),
        "cooling_water_process": ("Cooling Water",     "KM3"),
        "raw_water_process": ("Raw Water",             "M3"),
        "utility_water_process": ("Utility Water",     "M3"),
        "ret_steam_condensate_process": ("Ret Steam Condensate", "M3"),
        "oxygen_process":    ("Oxygen",                "MT"),
        "effluent_process":  ("Effluent",              "M3"),
    }

    # Aggregate totals per utility
    _ROLLUP = [
        ("Power",   ["power_process", "power_fixed"]),
        ("LP Steam",["lp_process",    "lp_fixed"]),
        ("MP Steam",["mp_process",    "mp_fixed"]),
        ("HP Steam",["hp_process",    "hp_fixed"]),
        ("SHP Steam",["shp_process",  "shp_fixed"]),
        ("Air",     ["air_process"]),
        ("Nitrogen ASU", ["nitrogen_asu_process"]),
        ("DM Water",["dm_process"]),
        ("CW1",     ["cw1_process"]),
        ("CW2",     ["cw2_process"]),
        ("Cooling Water", ["cooling_water_process"]),
        ("Raw Water",["raw_water_process"]),
        ("Utility Water", ["utility_water_process"]),
        ("Ret Steam Condensate", ["ret_steam_condensate_process"]),
        ("Oxygen",  ["oxygen_process"]),
        ("Effluent",["effluent_process"]),
    ]

    logger.info("  %-28s  %12s  %8s", "Demand Component", "Value", "Unit")
    logger.info("  %s  %s  %s", "-"*28, "-"*12, "-"*8)
    for key, (label, unit) in _META.items():
        val = merged.get(key, 0.0)
        marker = " *" if val > 0 else ""
        logger.info("  %-28s  %12.2f  %8s%s", label, val, unit, marker)

    logger.info("")
    logger.info("  AGGREGATE TOTALS")
    logger.info("  %-28s  %12s  %8s", "Utility", "Total", "Unit")
    logger.info("  %s  %s  %s", "-"*28, "-"*12, "-"*8)
    for util_label, keys in _ROLLUP:
        total = sum(merged.get(k, 0.0) for k in keys)
        unit  = _META.get(keys[0], ("", "-"))[1]
        flag  = " <<<" if total > 0 else ""
        logger.info("  %-28s  %12.2f  %8s%s", util_label, total, unit, flag)


def test_asset_capacity(plant_id: str):
    fy_year = YEAR if MONTH >= 4 else YEAR - 1
    fy = f"{fy_year}-{str(fy_year + 1)[-2:]}"
    section(f"Power Asset Capacity  (CPPPowerAssetCapacity)  FY {fy}")

    rows = fetch_power_asset_capacity_all_months(plant_id, fy_year)
    if not rows:
        logger.info("  No capacity records found for FY %s", fy)
        return

    logger.info("  %d asset(s) — Min/Max MW per month", len(rows))

    # Header: Asset | Type | Fixed Min | Fixed Max | Apr Min | Apr Max | … | Mar Min | Mar Max
    _MONTHS = ["Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan", "Feb", "Mar"]
    col_hdr = "".join(f"  {m:>5} {'':>5}" for m in _MONTHS)
    logger.info("  %-25s  %-10s  %6s  %6s%s",
                "Asset", "Type", "FxMin", "FxMax", col_hdr.replace("       ", " Min  Max"))

    sep = "  " + "-"*25 + "  " + "-"*10 + "  " + "-"*6 + "  " + "-"*6 + ("  -----  -----" * 12)
    logger.info(sep)

    for r in rows:
        fx_min = f"{r['fixed_min']:6.0f}" if r["fixed_min"] is not None else "   N/A"
        fx_max = f"{r['fixed_max']:6.0f}" if r["fixed_max"] is not None else "   N/A"
        month_vals = ""
        for m in _MONTHS:
            mn = r.get(f"{m}_Min")
            mx = r.get(f"{m}_Max")
            month_vals += f"  {mn:5.0f}" if mn is not None else "    N/A"
            month_vals += f"  {mx:5.0f}" if mx is not None else "    N/A"
        logger.info("  %-25s  %-10s  %s  %s%s",
                    r["asset_name"], r["asset_type"] or "-", fx_min, fx_max, month_vals)


def test_demand_matching(plant_id: str):
    section(f"Demand vs Capacity Match  {MONTH}/{YEAR}")
    logger.info("  Running single-month budget calculation …")

    result = run_month(plant_id=plant_id, month=MONTH, year=YEAR, save_to_db=False)

    if not result.get("success"):
        logger.info("  Calculation failed: %s", result.get("message"))
        for r in result.get("reasons", []):
            logger.info("    - %s", r)
        return

    # ── Demand Capacity Match ─────────────────────────────────────────────────
    dc = result.get("demand_capacity")
    if dc:
        match = dc.get("capacity_match", {})
        summary = dc.get("summary", {})
        logger.info("  Data source: %s", dc.get("data_source", "?").upper())
        logger.info("  %-18s  %12s  %12s  %10s  %s",
                    "Utility", "Demand", "Available", "Surplus", "Status")
        logger.info("  %s  %s  %s  %s  %s", "-"*18, "-"*12, "-"*12, "-"*10, "-"*10)

        _UTIL_LABELS = {
            "power":    ("Power",       "MWh"),
            "steam":    ("Steam",       "MT"),
            "air":      ("Air",         "NM3"),
            "dm_water": ("DM Water",    "M3"),
            "cw1":      ("CW1",         "KM3"),
            "cw2":      ("CW2",         "KM3"),
            "raw_water":("Raw Water",   "M3"),
        }
        for key, (label, unit) in _UTIL_LABELS.items():
            m = match.get(key, {})
            if not m:
                continue
            status = m.get("status", "?")
            # pick the right keys depending on utility type
            demand    = m.get("demand_mwh", m.get("demand_mt",  m.get("demand",  0)))
            available = m.get("available_mwh", m.get("available_mt", m.get("available", 0)))
            surplus   = m.get("surplus_mwh", m.get("surplus_mt", m.get("surplus", 0)))
            flag = " <-- SHORTFALL" if status == "SHORTFALL" else ""
            logger.info("  %-18s  %10.1f%s  %10.1f%s  %8.1f%s  %s%s",
                        f"{label} ({unit})",
                        demand, " " * (2 - len(unit)),
                        available, " " * (2 - len(unit)),
                        surplus, " " * (2 - len(unit)),
                        status, flag)

        logger.info("")
        logger.info("  Summary: %d utilities  |  Covered: %d  |  Shortfall: %d  |  No assets: %d",
                    summary.get("total_utilities", 0), summary.get("covered", 0),
                    summary.get("shortfall", 0), summary.get("no_assets", 0))

    # ── Budget / Dispatch Result ──────────────────────────────────────────────
    logger.info("")
    budget_ok  = result.get("budget_success", False)
    converged  = result.get("budget_converged", False)
    iterations = result.get("budget_iterations", 0)
    logger.info("  BUDGET ENGINE:  success=%s  converged=%s  iterations=%d",
                budget_ok, converged, iterations)

    dispatch = result.get("final_dispatch", [])
    if dispatch:
        logger.info("")
        logger.info("  POWER DISPATCH  (%d assets)", len(dispatch))
        logger.info("  %-25s  %-10s  %8s  %10s  %10s  %10s",
                    "Asset", "Type", "Hours", "Net MWh", "Gross MWh", "Load MW")
        logger.info("  %s  %s  %s  %s  %s  %s",
                    "-"*25, "-"*10, "-"*8, "-"*10, "-"*10, "-"*10)
        total_net = 0.0
        for a in dispatch:
            net   = float(a.get("NetMWh",   0))
            gross = float(a.get("GrossMWh", 0))
            load  = float(a.get("LoadMW",   0))
            hrs   = float(a.get("Hours",    0))
            total_net += net
            active = "*" if net > 0 else " "
            logger.info("  %s%-24s  %-10s  %8.1f  %10.2f  %10.2f  %10.2f",
                        active, a.get("AssetName", "?"),
                        a.get("AssetType", "-"),
                        hrs, net, gross, load)
        logger.info("  %-25s  %-10s  %8s  %10.2f", "TOTAL", "", "", total_net)

    steam = result.get("steam_result", {})
    if steam:
        logger.info("")
        logger.info("  STEAM BALANCE")
        for level in ("lp_balance", "mp_balance", "hp_balance", "shp_balance"):
            bal = steam.get(level)
            if bal is None:
                continue
            label = level.replace("_balance", "").upper()
            demand  = bal.get("total_demand", 0)
            supply  = bal.get("total_supply", 0)
            surplus = bal.get("surplus_mt", supply - demand)
            logger.info("  %-4s  demand=%8.1f MT  supply=%8.1f MT  surplus=%8.1f MT",
                        label, demand, supply, surplus)

    uc = result.get("utility_consumption", {})
    if uc:
        logger.info("")
        logger.info("  UTILITY CONSUMPTION SUMMARY")
        logger.info("  SHP from HRSG:     %10.1f MT",    uc.get("shp_from_hrsg_mt",  0))
        logger.info("  HP from PRDS:      %10.1f MT",    uc.get("hp_from_prds_mt",   0))
        logger.info("  MP from PRDS:      %10.1f MT",    uc.get("mp_from_prds_mt",   0))
        logger.info("  LP from PRDS:      %10.1f MT",    uc.get("lp_from_prds_mt",   0))
        logger.info("  LP from STG:       %10.1f MT",    uc.get("lp_from_stg_mt",    0))
        logger.info("  MP from STG:       %10.1f MT",    uc.get("mp_from_stg_mt",    0))
        logger.info("  GT total:          %10.2f MWh",   uc.get("gt_total_mwh",      0))
        logger.info("  STG gross:         %10.2f MWh",   uc.get("stg_gross_mwh",     0))
        logger.info("  Import power:      %10.2f MWh",   uc.get("import_power_mwh",  0))
        logger.info("  U4U power:         %10.4f MWh",   uc.get("u4u_power_mwh",     0))


def run_calculation_full_year(plant_id: str):
    fy_year = YEAR if MONTH >= 4 else YEAR - 1
    fy = f"{fy_year}-{str(fy_year + 1)[-2:]}"

    if MONTH_GIVEN:
        # Single-month calculation
        month_name = {1:"Jan",2:"Feb",3:"Mar",4:"Apr",5:"May",6:"Jun",
                      7:"Jul",8:"Aug",9:"Sep",10:"Oct",11:"Nov",12:"Dec"}[MONTH]
        section(f"CALCULATION — {month_name} {YEAR}  (engine.calculator.run_month)")
        logger.info("  Running single month %s/%s …", MONTH, YEAR)
        mres = run_month(plant_id=plant_id, month=MONTH, year=YEAR, save_to_db=False)
        months_dict = {f"{YEAR}_{MONTH:02d}": mres}
        logger.info("  Done.")
    else:
        # Full-year calculation
        section(f"FULL YEAR CALCULATION — FY {fy}  (engine.calculator.run_full_year)")
        logger.info("  Running all 12 months in parallel …")
        result = run_full_year(plant_id=plant_id, fy_start_year=fy_year, save_to_db=False)
        summary = result.get("summary", {})
        logger.info("  Successful: %d/12   Failed: %d/12   Total norms: %d",
                    summary.get("successful", 0),
                    summary.get("failed", 0),
                    summary.get("total_norms", 0))
        months_dict = result.get("months", {})

    if not months_dict:
        logger.info("  No month results returned.")
        return

    # ── Operational Hours grid ────────────────────────────────────────────────
    logger.info("")
    logger.info("  ASSET OPERATIONAL HOURS  (hours/month — all 12 months of FY %s)", fy)

    # Collect all asset names from any month's op_hours list
    all_assets: dict[str, dict] = {}  # asset_id → {name, type}
    month_hrs: dict[str, dict[str, float | None]] = {}  # month_key → {asset_id → hours}

    for key, mres in months_dict.items():
        op_hours = mres.get("op_hours", [])
        for row in op_hours:
            aid = row["asset_id"]
            if aid not in all_assets:
                all_assets[aid] = {"name": row["asset_name"], "type": row.get("asset_type", "")}

    # For each asset pick the hours from its matching month column
    _MONTH_COL_TITLE = {
        1: "Jan", 2: "Feb", 3: "Mar", 4: "Apr",
        5: "May", 6: "Jun", 7: "Jul", 8: "Aug",
        9: "Sep", 10: "Oct", 11: "Nov", 12: "Dec",
    }
    _FY_MONTHS_ORDER = [(4, fy_year), (5, fy_year), (6, fy_year),
                        (7, fy_year), (8, fy_year), (9, fy_year),
                        (10, fy_year), (11, fy_year), (12, fy_year),
                        (1, fy_year + 1), (2, fy_year + 1), (3, fy_year + 1)]

    # Build { asset_id: { (m,y): hours } }
    asset_month_hrs: dict[str, dict] = {aid: {} for aid in all_assets}
    for key, mres in months_dict.items():
        m = mres.get("month")
        y = mres.get("year")
        mcol = _MONTH_COL_TITLE.get(m, "")
        for row in mres.get("op_hours", []):
            aid = row["asset_id"]
            asset_month_hrs.setdefault(aid, {})[( m, y)] = row.get(mcol)

    col_labels = [_MONTH_COL_TITLE[m] for m, _ in _FY_MONTHS_ORDER]
    header = (f"  {'Asset':<25}  {'Type':<10}"
              + "".join(f"  {lbl:>5}" for lbl in col_labels)
              + "  Total")
    logger.info(header)
    logger.info("  %s  %s%s  %s", "-"*25, "-"*10, "  -----"*12, "-------")
    for aid, meta in sorted(all_assets.items(), key=lambda x: x[1]["name"]):
        hrs_by_my = asset_month_hrs.get(aid, {})
        vals = ""
        total = 0.0
        for my in _FY_MONTHS_ORDER:
            h = hrs_by_my.get(my)
            if h is not None:
                vals += f"  {h:>5.0f}"
                total += h
            else:
                vals += "    N/A"
        logger.info("  %-25s  %-10s%s  %7.0f",
                    meta["name"], meta["type"] or "-", vals, total)

    # ── Power Asset Priority grid ─────────────────────────────────────────────
    logger.info("")
    logger.info("  POWER ASSET PRIORITY  (priority rank — all 12 months of FY %s)", fy)

    all_power: dict[str, dict] = {}
    asset_month_pri: dict[str, dict] = {}
    for key, mres in months_dict.items():
        m = mres.get("month")
        y = mres.get("year")
        mcol = _MONTH_COL_TITLE.get(m, "")
        for row in mres.get("power_priority", []):
            aid = row["asset_id"]
            if aid not in all_power:
                all_power[aid] = {"name": row["asset_name"], "type": row.get("asset_type", "")}
            asset_month_pri.setdefault(aid, {})[(m, y)] = row.get(mcol)

    if all_power:
        header = (f"  {'Asset':<25}  {'Type':<10}"
                  + "".join(f"  {lbl:>4}" for lbl in col_labels))
        logger.info(header)
        logger.info("  %s  %s%s", "-"*25, "-"*10, "  ----"*12)
        for aid, meta in sorted(all_power.items(), key=lambda x: x[1]["name"]):
            pri_by_my = asset_month_pri.get(aid, {})
            vals = ""
            for my in _FY_MONTHS_ORDER:
                p = pri_by_my.get(my)
                vals += f"  {p:>4}" if p is not None else "   N/A"
            logger.info("  %-25s  %-10s%s", meta["name"], meta["type"] or "-", vals)
    else:
        logger.info("  No power priority data found.")

    # ── Steam Asset Priority grid ─────────────────────────────────────────────
    logger.info("")
    logger.info("  STEAM ASSET PRIORITY  (priority rank — all 12 months of FY %s)", fy)

    all_steam: dict[str, dict] = {}
    asset_month_spri: dict[str, dict] = {}
    for key, mres in months_dict.items():
        m = mres.get("month")
        y = mres.get("year")
        mcol = _MONTH_COL_TITLE.get(m, "")
        for row in mres.get("steam_priority", []):
            aid = row["asset_id"]
            if aid not in all_steam:
                all_steam[aid] = {
                    "name": row["asset_name"],
                    "type": row.get("asset_type", ""),
                    "steam_type": row.get("steam_type", ""),
                }
            asset_month_spri.setdefault(aid, {})[(m, y)] = row.get(mcol)

    if all_steam:
        header = (f"  {'Asset':<25}  {'Type':<10}  {'SteamType':<10}"
                  + "".join(f"  {lbl:>4}" for lbl in col_labels))
        logger.info(header)
        logger.info("  %s  %s  %s%s", "-"*25, "-"*10, "-"*10, "  ----"*12)
        for aid, meta in sorted(all_steam.items(), key=lambda x: x[1]["name"]):
            spri_by_my = asset_month_spri.get(aid, {})
            vals = ""
            for my in _FY_MONTHS_ORDER:
                p = spri_by_my.get(my)
                vals += f"  {p:>4}" if p is not None else "   N/A"
            logger.info("  %-25s  %-10s  %-10s%s",
                        meta["name"], meta["type"] or "-", meta["steam_type"] or "-", vals)
    else:
        logger.info("  No steam priority data found.")


def run_for_plant(plant_id: str):
    meta = PLANT_REGISTRY.get(plant_id.upper(), {})
    logger.info("")
    logger.info("#" * 60)
    logger.info("  PLANT: %s  (%s)", meta.get('display_name', plant_id), plant_id)
    logger.info("  Period: %d/%d", MONTH, YEAR)
    logger.info("#" * 60)

    test_plant_info(plant_id)
    test_plant_assets_list(plant_id)
    test_asset_ops_hours(plant_id)
    test_asset_ops_hours_all_months(plant_id)
    test_asset_priority(plant_id)
    test_asset_priority_all_months(plant_id)
    test_steam_asset_priority(plant_id)
    test_steam_asset_priority_all_months(plant_id)
    test_steam_generation_assets(plant_id)
    test_plant_power_info(plant_id)
    test_process_demand_master(plant_id)
    test_demand_segregation(plant_id)
    test_asset_capacity(plant_id)
    test_demand_matching(plant_id)
    if _explicit_plants:  # only run full-year calculation when specific plant(s) are given
        run_calculation_full_year(plant_id)


# ─── main ─────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    label = ", ".join(_explicit_plants) if _explicit_plants else "ALL plants"
    logger.info("Fetching: %s  |  Period: %d/%d", label, MONTH, YEAR)

    try:
        for pid in PLANT_IDS:
            run_for_plant(pid)

        logger.info("")
        logger.info("=" * 60)
        logger.info("  DONE")
        logger.info("=" * 60)

    except Exception as e:
        logger.exception("[ERROR] %s", e)
        sys.exit(1)
