"""
JMD Plant Budgeting - Main Entry Point
=======================================
Interactive entry point for running any JMD plant budget calculation.

Usage:
    python main.py
    python main.py --plant dta --month 4 --year 2025
    python main.py --plant dta,sez --month 4 --year 2025
    python main.py --plant all --fy 2025
    python main.py --plant all --info --month 4 --year 2025   # fetch & display plant data

Plants:
    dta      → DTA-CPP
    dta_pcg  → DTA-PCG-CPP
    sez      → SEZ-CPP
    sez_pcg  → SEZ-PCG-CPP
    c2       → C2-CPP
"""

import sys
import os
import argparse
from datetime import datetime

# Ensure JMD root is on the path so engine/database imports work
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from plant_mapper import PLANT_REGISTRY, get_plant_id_by_short_code, get_all_plants
from engine.calculator import run_month, run_full_year
from engine.report_logger import setup_logging, LogCapture, save_text_log, save_full_year_log
from database.queries import (
    fetch_plant_info,
    fetch_plant_assets,
    fetch_asset_operational_hours,
    fetch_asset_operational_hours_all_months,
    fetch_asset_priority,
    fetch_asset_priority_all_months,
    fetch_steam_asset_priority,
    fetch_steam_asset_priority_all_months,
    fetch_steam_asset_capacity_all_months,
    fetch_steam_generation_assets,
    fetch_steam_asset_operational_hours,
    fetch_plant_power_info,
    fetch_power_asset_capacity_all_months,
    fetch_process_demands,
    fetch_fixed_consumption,
    fetch_process_demand_master,
)

_SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
LOG_FOLDER  = os.path.join(_SCRIPT_DIR, "logs")
OUTPUT_FOLDER = os.path.join(_SCRIPT_DIR, "output")

_MONTH_NAMES = {
    1: "January", 2: "February", 3: "March", 4: "April",
    5: "May",     6: "June",     7: "July",   8: "August",
    9: "September", 10: "October", 11: "November", 12: "December",
}

_SHORT_CODES = {
    "dta":     "A4AF8441-73AD-4F9F-BCF4-6734E8202F7A",
    "dta_pcg": "F6D82E68-C3B6-494F-9905-48F19DC611E3",
    "sez":     "2DFEE33F-4CFD-4887-B9DD-53388AA95271",
    "sez_pcg": "D2C7FBAD-7E00-4642-B3B2-5A768FAC8D45",
    "c2":      "BA558F95-8A3F-4769-9C78-FF7B6C639DDF",
}


def _select_plants_interactive() -> list[tuple[str, str]]:
    """Ask user to select one or more JMD plants. Returns list of (plant_id, display_name)."""
    plants = get_all_plants()
    print("\n--- JMD PLANTS ---")
    for i, p in enumerate(plants, 1):
        print(f"  {i}. {p['display_name']}  [{p['short_code']}]")
    print(f"  a. ALL plants")

    while True:
        choice = input(
            f"\nSelect plant(s) — number, short code, comma-separated, or 'a' for all: "
        ).strip().lower()

        if choice == "a":
            return [(p["plant_id"], p["display_name"]) for p in plants]

        selected = []
        invalid  = []
        for part in [c.strip() for c in choice.split(",")]:
            if part.isdigit():
                idx = int(part) - 1
                if 0 <= idx < len(plants):
                    p = plants[idx]
                    selected.append((p["plant_id"], p["display_name"]))
                else:
                    invalid.append(part)
            elif part in _SHORT_CODES:
                pid  = _SHORT_CODES[part]
                info = PLANT_REGISTRY[pid]
                selected.append((pid, info["display_name"]))
            else:
                invalid.append(part)

        if invalid:
            print(f"  Unknown: {', '.join(invalid)}. Try again.")
            continue
        if not selected:
            print("  No plants selected. Try again.")
            continue

        # Deduplicate while preserving order
        seen = set()
        unique = []
        for pid, name in selected:
            if pid not in seen:
                seen.add(pid)
                unique.append((pid, name))
        return unique


def _print_month_summary(result: dict):
    sep = "=" * 70
    print("\n" + sep)
    print(f"  {result.get('plant_name', '')}  {result.get('month')}/{result.get('year')}  ({result.get('fy', '')})")
    print(sep)
    print(f"  Success:     {result.get('success')}")
    print(f"  Assets:      {len(result.get('assets', []))}")
    print(f"  Norms:       {len(result.get('norms', []))}")
    import_mwh = result.get('import_power', {}).get('total_mwh', 0)
    print(f"  Import MWh:  {import_mwh:,.2f}")
    print(f"  Exec time:   {result.get('execution_time_seconds', 0):.2f}s")
    if not result.get('success'):
        print(f"  Message:     {result.get('message', '')}")
        for r in result.get('reasons', []):
            print(f"    - {r}")
    print(sep)
    _print_demand_capacity(result.get("demand_capacity"))


def _print_demand_capacity(dc: dict):
    if not dc:
        return
    sep  = "-" * 70
    sep2 = "-" * 70
    src  = dc.get("data_source", "?")
    sm   = dc.get("summary", {})

    print(f"\n  DEMAND SEGREGATION & CAPACITY CHECK  [source: {src}]")
    print(sep)

    # Demand totals table
    totals = dc.get("demand_segregation", {}).get("totals", {})
    _UTIL_LABELS = {
        "power":      "Power",
        "shp":        "SHP Steam",
        "hp":         "HP Steam",
        "mp":         "MP Steam",
        "lp":         "LP Steam",
        "air":        "Compressed Air",
        "nitrogen_asu": "Nitrogen ASU",
        "dm_water":   "DM Water",
        "cw1":        "Cooling Water 1",
        "cw2":        "Cooling Water 2",
        "cooling_water": "Cooling Water",
        "raw_water":  "Raw Water",
        "utility_water": "Utility Water",
        "ret_steam_condensate": "Ret Steam Condensate",
        "oxygen":     "Oxygen",
        "effluent":   "Effluent",
    }
    print(f"  {'Utility':<22} {'Total Demand':>16} {'Unit':<8}")
    print(f"  {'-'*22} {'-'*16} {'-'*8}")
    for key, label in _UTIL_LABELS.items():
        t = totals.get(key, {})
        val  = t.get("total", 0.0)
        unit = t.get("unit", "")
        print(f"  {label:<22} {val:>16,.2f} {unit:<8}")

    # Capacity match
    cm = dc.get("capacity_match", {})
    print()
    print(f"  CAPACITY MATCH")
    print(sep2)
    _STATUS_ICON = {"COVERED": "OK", "SHORTFALL": "FAIL", "NO_ASSETS": "?"}

    # Power
    p = cm.get("power", {})
    icon = _STATUS_ICON.get(p.get("status", ""), " ")
    print(f"  [{icon}] Power      demand={p.get('demand_mwh',0):>10,.1f} MWh  "
          f"available={p.get('available_mwh',0):>10,.1f} MWh  "
          f"surplus={p.get('surplus_mwh',0):>+10,.1f}  [{p.get('status','')}]")

    # Steam
    s = cm.get("steam", {})
    icon = _STATUS_ICON.get(s.get("status", ""), " ")
    print(f"  [{icon}] Steam      demand={s.get('demand_mt',0):>10,.1f} MT   "
          f"available={s.get('available_mt',0):>10,.1f} MT   "
          f"surplus={s.get('surplus_mt',0):>+10,.1f}  [{s.get('status','')}]")

    # Utilities
    for util in ("air", "nitrogen_asu", "dm_water", "cw1", "cw2", "cooling_water", "raw_water", "utility_water", "ret_steam_condensate"):
        u    = cm.get(util, {})
        icon = _STATUS_ICON.get(u.get("status", ""), " ")
        lbl  = _UTIL_LABELS.get(util, util)
        unit = u.get("unit", "")
        print(f"  [{icon}] {lbl:<14} demand={u.get('demand',0):>10,.1f} {unit:<4} "
              f"available={u.get('available',0):>10,.1f} {unit:<4} "
              f"surplus={u.get('surplus',0):>+10,.1f}  [{u.get('status','')}]")

    print()
    print(f"  Summary: {sm.get('covered',0)} COVERED  "
          f"{sm.get('shortfall',0)} SHORTFALL  "
          f"{sm.get('no_assets',0)} NO_ASSETS  "
          f"(of {sm.get('total_utilities',0)} utilities)")
    print(sep)


def _print_full_year_summary(result: dict):
    sep = "=" * 70
    s = result.get("summary", {})
    print("\n" + sep)
    print(f"  {result.get('plant_name', '')}  FY {result.get('fy', '')}")
    print(sep)
    print(f"  Months OK:   {s.get('successful', 0)}/12")
    print(f"  Months FAIL: {s.get('failed', 0)}/12")
    print(f"  Total Norms: {s.get('total_norms', 0)}")
    for key, m in result.get("months", {}).items():
        status = "OK  " if m.get("success") else "FAIL"
        print(f"  [{status}] {key}  assets={len(m.get('assets', []))}  "
              f"norms={len(m.get('norms', []))}  {m.get('execution_time_seconds', 0):.2f}s")
        for r in m.get("reasons", []):
            print(f"           - {r}")
    print(sep)


# ---------------------------------------------------------------------------
# --info: fetch and print raw plant data (no budget calculation)
# ---------------------------------------------------------------------------

_ALL_MONTHS_LABELS = ["Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec","Jan","Feb","Mar"]

def _sec(title: str):
    print("\n" + "=" * 70)
    print(f"  {title}")
    print("=" * 70)


def _print_plant_info(plant_id: str):
    _sec("PLANT INFO")
    info = fetch_plant_info(plant_id)
    if not info:
        print("  NOT FOUND in Plants table")
        return
    print(f"  plant_id:          {info['plant_id']}")
    print(f"  name:              {info['name']}")
    print(f"  display_name:      {info['display_name']}")
    print(f"  plant_code:        {info['plant_code']}")
    print(f"  is_active:         {info['is_active']}")
    print(f"  business_category: {info['business_category']}  ({info['business_category_display']})")


def _print_plant_assets(plant_id: str):
    _sec("PLANT ASSETS  (PowerGenerationAssets)")
    rows = fetch_plant_assets(plant_id)
    if not rows:
        print("  No assets found")
        return
    print(f"  {len(rows)} asset(s):")
    print(f"  {'Asset':<25}  {'Type':<12}  {'DisplayName':<25}")
    print(f"  {'-'*25}  {'-'*12}  {'-'*25}")
    for a in rows:
        print(f"  {a['asset_name']:<25}  {(a['asset_type'] or '-'):<12}  {(a['display_name'] or '-'):<25}")


def _print_asset_ops_hours(plant_id: str, month: int, year: int):
    _sec(f"ASSET OPERATIONAL HOURS  ({month}/{year})")
    rows = fetch_asset_operational_hours(plant_id, month, year)
    if not rows:
        print("  No rows found")
        return
    print(f"  {len(rows)} row(s)  [aop_year={rows[0]['aop_year']}]")
    print(f"  {'Asset':<25}  {'Type':<12}  {'Hours':>8}")
    print(f"  {'-'*25}  {'-'*12}  {'-'*8}")
    for r in rows:
        hrs = f"{r['operational_hours']:.2f}" if r['operational_hours'] is not None else "N/A"
        print(f"  {r['asset_name']:<25}  {(r['asset_type'] or '-'):<12}  {hrs:>8}")


def _print_asset_ops_hours_all_months(plant_id: str, month: int, year: int):
    fy_year = year if month >= 4 else year - 1
    _sec(f"ASSET OPERATIONAL HOURS — ALL MONTHS  (FY {fy_year}-{str(fy_year+1)[-2:]})")
    rows = fetch_asset_operational_hours_all_months(plant_id, fy_year)
    if not rows:
        print("  No rows found")
        return
    print(f"  {len(rows)} asset(s)  [aop_year={rows[0]['aop_year']}]")
    hdr = f"  {'Asset':<25}  {'Type':<12}" + "".join(f"  {m:>5}" for m in _ALL_MONTHS_LABELS) + "   Total"
    print(hdr)
    print(f"  {'-'*25}  {'-'*12}" + "  -----"*12 + "  -------")
    for r in rows:
        vals = "".join(f"  {r[m]:>5.0f}" if r[m] is not None else "    N/A" for m in _ALL_MONTHS_LABELS)
        print(f"  {r['asset_name']:<25}  {(r['asset_type'] or '-'):<12}{vals}  {r['total_hours']:7.0f}")


def _print_asset_priority(plant_id: str, month: int, year: int):
    _sec(f"POWER ASSET PRIORITY  ({month}/{year})")
    rows = fetch_asset_priority(plant_id, month, year)
    if not rows:
        print("  No rows found")
        return
    print(f"  {len(rows)} asset(s)  [aop_year={rows[0]['aop_year']}]")
    print(f"  {'Asset':<25}  {'Type':<12}  {'Priority':>8}")
    print(f"  {'-'*25}  {'-'*12}  {'-'*8}")
    for r in rows:
        pri = str(r['priority']) if r['priority'] is not None else "N/A"
        print(f"  {r['asset_name']:<25}  {(r['asset_type'] or '-'):<12}  {pri:>8}")


def _print_asset_priority_all_months(plant_id: str, month: int, year: int):
    fy_year = year if month >= 4 else year - 1
    _sec(f"POWER ASSET PRIORITY — ALL MONTHS  (FY {fy_year}-{str(fy_year+1)[-2:]})")
    rows = fetch_asset_priority_all_months(plant_id, fy_year)
    if not rows:
        print("  No rows found")
        return
    print(f"  {len(rows)} asset(s)  [aop_year={rows[0]['aop_year']}]")
    hdr = f"  {'Asset':<25}  {'Type':<12}" + "".join(f"  {m:>4}" for m in _ALL_MONTHS_LABELS)
    print(hdr)
    print(f"  {'-'*25}  {'-'*12}" + "  ----"*12)
    for r in rows:
        vals = "".join(f"  {r[m]:>4}" if r[m] is not None else "   N/A" for m in _ALL_MONTHS_LABELS)
        print(f"  {r['asset_name']:<25}  {(r['asset_type'] or '-'):<12}{vals}")


def _print_steam_asset_priority(plant_id: str, month: int, year: int):
    _sec(f"STEAM ASSET PRIORITY  ({month}/{year})")
    rows = fetch_steam_asset_priority(plant_id, month, year)
    if not rows:
        print("  No rows found")
        return
    print(f"  {len(rows)} asset(s)  [aop_year={rows[0]['aop_year']}]")
    print(f"  {'Asset':<25}  {'Type':<12}  {'SteamType':<12}  {'Priority':>8}")
    print(f"  {'-'*25}  {'-'*12}  {'-'*12}  {'-'*8}")
    for r in rows:
        pri = str(r['priority']) if r['priority'] is not None else "N/A"
        print(f"  {r['asset_name']:<25}  {(r['asset_type'] or '-'):<12}  {(r['steam_type'] or '-'):<12}  {pri:>8}")


def _print_steam_asset_priority_all_months(plant_id: str, month: int, year: int):
    fy_year = year if month >= 4 else year - 1
    _sec(f"STEAM ASSET PRIORITY — ALL MONTHS  (FY {fy_year}-{str(fy_year+1)[-2:]})")
    rows = fetch_steam_asset_priority_all_months(plant_id, fy_year)
    if not rows:
        print("  No rows found")
        return
    print(f"  {len(rows)} asset(s)  [aop_year={rows[0]['aop_year']}]")
    hdr = f"  {'Asset':<25}  {'Type':<12}  {'SteamType':<12}" + "".join(f"  {m:>4}" for m in _ALL_MONTHS_LABELS)
    print(hdr)
    print(f"  {'-'*25}  {'-'*12}  {'-'*12}" + "  ----"*12)
    for r in rows:
        vals = "".join(f"  {r[m]:>4}" if r[m] is not None else "   N/A" for m in _ALL_MONTHS_LABELS)
        print(f"  {r['asset_name']:<25}  {(r['asset_type'] or '-'):<12}  {(r['steam_type'] or '-'):<12}{vals}")


def _print_steam_generation_assets(plant_id: str):
    _sec("STEAM GENERATION ASSETS  (CPPSteamGenerationAsset)")
    rows = fetch_steam_generation_assets(plant_id)
    if not rows:
        print("  No rows found")
        return
    print(f"  {len(rows)} asset(s):")
    print(f"  {'Asset':<25}  {'Type':<12}  {'SteamType':<12}  {'Visible':<8}  {'Editable':<8}")
    print(f"  {'-'*25}  {'-'*12}  {'-'*12}  {'-'*8}  {'-'*8}")
    for r in rows:
        print(f"  {r['asset_name']:<25}  {(r['asset_type'] or '-'):<12}  "
              f"{(r['steam_type'] or '-'):<12}  {str(r['is_visible']):<8}  {str(r['is_editable']):<8}")


def _print_new_schema_asset_snapshot(plant_id: str, month: int, year: int):
    fy_year = year if month >= 4 else year - 1
    fy_label = f"{fy_year}-{str(fy_year + 1)[-2:]}"
    month_key = _MONTH_NAMES.get(month, str(month))[:3]

    def _fmt(value, precision=2):
        return "N/A" if value is None else f"{value:.{precision}f}"

    _sec(f"NEW SCHEMA ASSET SNAPSHOT  ({_MONTH_NAMES.get(month, month)} {year})")

    power_info = fetch_plant_power_info(plant_id, month, year)
    plant = power_info.get("plant", {})
    assets = power_info.get("assets", [])
    hours = power_info.get("hours", [])
    power_caps = {r["asset_id"]: r for r in fetch_power_asset_capacity_all_months(plant_id, fy_year)}
    power_pri = {r["asset_id"]: r for r in fetch_asset_priority(plant_id, month, year)}
    steam_assets = fetch_steam_generation_assets(plant_id)
    steam_hours = fetch_steam_asset_operational_hours(plant_id, month, year)
    steam_caps = {r["asset_id"]: r for r in fetch_steam_asset_capacity_all_months(plant_id, fy_year)}
    steam_pri = {r["asset_id"]: r for r in fetch_steam_asset_priority(plant_id, month, year)}

    hours_by_asset = {r["asset_id"]: r for r in hours}
    steam_hours_by_asset = {r["asset_id"]: r for r in steam_hours}

    print(f"  Plant:    {plant.get('display_name') or plant.get('name') or plant_id}")
    print(f"  AOP Year: {power_info.get('summary', {}).get('aop_year', fy_label)}")

    print(f"\n  POWER ASSETS")
    print(f"  {'Asset':<25}  {'Type':<10}  {'Hours':>8}  {'Min MW':>8}  {'Max MW':>8}  {'Gen MWh':>10}  {'Prio':>5}")
    print(f"  {'-'*25}  {'-'*10}  {'-'*8}  {'-'*8}  {'-'*8}  {'-'*10}  {'-'*5}")
    total_power_mwh = 0.0
    for asset in assets:
        asset_id = asset.get("asset_id", "")
        hour_row = hours_by_asset.get(asset_id, {})
        cap_row = power_caps.get(asset_id, {})
        pri_row = power_pri.get(asset_id, {})
        op_hours = hour_row.get("operational_hours")
        month_min = cap_row.get(f"{month_key}_Min")
        if month_min is None:
            month_min = cap_row.get("fixed_min")
        if month_min is None:
            month_min = 0.0
        month_max = cap_row.get(f"{month_key}_Max")
        if month_max is None:
            month_max = cap_row.get("fixed_max")
        if month_max is None:
            month_max = 0.0
        gen_mwh = (float(op_hours) * float(month_max)) if (op_hours is not None and month_max is not None) else 0.0
        total_power_mwh += gen_mwh
        print(
            f"  {asset.get('asset_name', ''):<25}  {(asset.get('asset_type') or '-'): <10}"
            f"  {_fmt(op_hours):>8}"
            f"  {_fmt(month_min):>8}"
            f"  {_fmt(month_max):>8}"
            f"  {_fmt(gen_mwh):>10}"
            f"  {('N/A' if pri_row.get('priority') is None else str(pri_row.get('priority'))):>5}"
        )
    print(f"  {'TOTAL':<25}  {'':10}  {'':8}  {'':8}  {'':8}  {_fmt(total_power_mwh):>10}  {'':5}")

    print(f"\n  STEAM ASSETS")
    print(f"  {'Asset':<25}  {'Type':<10}  {'SteamType':<10}  {'Hours':>8}  {'Min TPH':>8}  {'Max TPH':>8}  {'Gen MT':>10}  {'Prio':>5}")
    print(f"  {'-'*25}  {'-'*10}  {'-'*10}  {'-'*8}  {'-'*8}  {'-'*8}  {'-'*10}  {'-'*5}")
    total_steam_mt = 0.0
    steam_hours_available = False
    for asset in steam_assets:
        asset_id = asset.get("asset_id", "")
        hour_row = steam_hours_by_asset.get(asset_id, {})
        cap_row = steam_caps.get(asset_id, {})
        pri_row = steam_pri.get(asset_id, {})
        month_min = cap_row.get(f"{month_key}_Min")
        if month_min is None:
            month_min = cap_row.get("fixed_min")
        if month_min is None:
            month_min = 0.0
        month_max = cap_row.get(f"{month_key}_Max")
        if month_max is None:
            month_max = cap_row.get("fixed_max")
        if month_max is None:
            month_max = 0.0
        op_hours = hour_row.get("operational_hours")
        if op_hours is not None:
            steam_hours_available = True
        gen_mt = (float(op_hours) * float(month_max)) if (op_hours is not None and month_max is not None) else None
        if gen_mt is not None:
            total_steam_mt += gen_mt
        print(
            f"  {asset.get('asset_name', ''):<25}  {(asset.get('asset_type') or '-'): <10}"
            f"  {(asset.get('steam_type') or '-'): <10}"
            f"  {_fmt(op_hours):>8}"
            f"  {_fmt(month_min):>8}"
            f"  {_fmt(month_max):>8}"
            f"  {_fmt(gen_mt):>10}"
            f"  {('N/A' if pri_row.get('priority') is None else str(pri_row.get('priority'))):>5}"
        )
    steam_total_text = _fmt(total_steam_mt) if steam_hours_available else "N/A"
    print(f"  {'TOTAL':<25}  {'':10}  {'':10}  {'':8}  {'':8}  {'':8}  {steam_total_text:>10}  {'':5}")

    print(f"\n  MONTHLY GENERATION SUMMARY")
    print(f"  {'Category':<20}  {'Generation':>14}  {'Unit':<8}")
    print(f"  {'-'*20}  {'-'*14}  {'-'*8}")
    print(f"  {'Power':<20}  {total_power_mwh:>14,.2f}  MWh")
    print(f"  {'Steam':<20}  {steam_total_text if steam_total_text == 'N/A' else f'{total_steam_mt:,.2f}':>14}  MT")


def _print_plant_power_info(plant_id: str, month: int, year: int):
    _sec(f"PLANT POWER INFO  (combined)  ({month}/{year})")
    data = fetch_plant_power_info(plant_id, month, year)
    p = data.get("plant", {})
    s = data.get("summary", {})
    print(f"  Plant:                  {p.get('name')}  ({p.get('display_name')})")
    print(f"  Code:                   {p.get('plant_code')}   Active: {p.get('is_active')}")
    print(f"  AOP Year:               {s.get('aop_year')}")
    print(f"  Total Assets:           {s.get('total_assets')}")
    print(f"  Assets with Hours:      {s.get('assets_with_hours')}")
    print(f"  Total Operational Hrs:  {s.get('total_operational_hours')}")
    hours = data.get("hours", [])
    if hours:
        print(f"\n  {'Asset':<25}  {'Type':<12}  {'Hours':>8}  Utility")
        print(f"  {'-'*25}  {'-'*12}  {'-'*8}  {'-'*7}")
        for h in hours:
            hrs = f"{h['operational_hours']:.2f}" if h['operational_hours'] is not None else "   N/A"
            util = h.get('utility_generated') or h.get('utility_distributed') or "-"
            print(f"  {h['asset_name']:<25}  {(h['asset_type'] or '-'):<12}  {hrs:>8}  {util}")


_FY_MONTH_COLS = ["apr","may","jun","jul","aug","sep","oct","nov","dec","jan","feb","mar"]
_FY_MONTH_HDRS = ["Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec","Jan","Feb","Mar"]


def _fy_string_from_month_year(month: int, year: int) -> str:
    fy_start = year if month >= 4 else year - 1
    return f"{fy_start}-{str(fy_start + 1)[-2:]}"


def _print_process_demand_master(plant_id: str, month: int, year: int):
    fy = _fy_string_from_month_year(month, year)
    _sec(f"PROCESS DEMAND MASTER + CALCULATED DEMAND  (FY {fy})")
    rows = fetch_process_demand_master(plant_id, financial_year=fy)
    if not rows:
        print("  No active rows found for this plant")
        return
    print(f"  {len(rows)} row(s)  [FY {fy}]")
    hdr = (f"  {'Process Plant':<30}  {'CPP Utility':<22}  {'UOM':<5}"
           + "".join(f"  {m:>12}" for m in _FY_MONTH_HDRS))
    print(hdr)
    print(f"  {'-'*30}  {'-'*22}  {'-'*5}" + "  ------------" * 12)
    for r in rows:
        vals = "".join(
            f"  {r[m]:>12,.0f}" if r[m] else "             0"
            for m in _FY_MONTH_COLS
        )
        calc = " *" if r.get("is_calculated") else ""
        print(f"  {r['process_plant']:<30}  {r['cpp_utility']:<22}  {r['uom']:<5}{vals}{calc}")
    print("  (* = calculated data present)")


def _print_calculated_process_demand(plant_id: str, month: int, year: int):
    _sec(f"CALCULATED PROCESS DEMAND — {month}/{year}  (aggregated by utility)")
    demands = fetch_process_demands(plant_id, month, year)

    _LABELS = {
        "power_process":      ("Power (Process)",      "KWH"),
        "lp_process":         ("LP Steam (Process)",   "MT"),
        "mp_process":         ("MP Steam (Process)",   "MT"),
        "hp_process":         ("HP Steam (Process)",   "MT"),
        "shp_process":        ("SHP Steam (Process)",  "MT"),
        "air_process":        ("Compressed Air",       "NM3"),
        "nitrogen_asu_process": ("Nitrogen (ASU)",     "NM3"),
        "dm_process":         ("DM Water",             "M3"),
        "cw1_process":        ("Cooling Water 1",      "KM3"),
        "cw2_process":        ("Cooling Water 2",      "KM3"),
        "cooling_water_process": ("Cooling Water",     "KM3"),
        "raw_water_process":  ("Raw Water",            "M3"),
        "utility_water_process": ("Utility Water",     "M3"),
        "ret_steam_condensate_process": ("Ret Steam Condensate", "M3"),
        "oxygen_process":     ("Oxygen",               "MT"),
        "effluent_process":   ("Effluent",             "M3"),
        "process_feed_water_process": ("Process Feed Water", "M3"),
    }

    print(f"  {'Utility':<28}  {'Value':>16}  {'Unit':<8}")
    print(f"  {'-'*28}  {'-'*16}  {'-'*8}")
    for key, (label, unit) in _LABELS.items():
        val = demands.get(key, 0.0)
        marker = " *" if val > 0 else ""
        print(f"  {label:<28}  {val:>16,.2f}  {unit:<8}{marker}")


def _print_demand_segregation(plant_id: str, month: int, year: int):
    _sec(f"DEMAND SEGREGATION  (Process + Fixed)  ({month}/{year})")
    process = fetch_process_demands(plant_id, month, year)
    fixed   = fetch_fixed_consumption(plant_id, month, year)
    merged  = {**process, **fixed}

    _META = {
        "power_process":     ("Power (Process)",      "KWH"),
        "power_fixed":       ("Power (Fixed)",        "MWh"),
        "lp_process":        ("LP Steam (Process)",   "MT"),
        "lp_fixed":          ("LP Steam (Fixed)",     "MT"),
        "mp_process":        ("MP Steam (Process)",   "MT"),
        "mp_fixed":          ("MP Steam (Fixed)",     "MT"),
        "hp_process":        ("HP Steam (Process)",   "MT"),
        "hp_fixed":          ("HP Steam (Fixed)",     "MT"),
        "shp_process":       ("SHP Steam (Process)",  "MT"),
        "shp_fixed":         ("SHP Steam (Fixed)",    "MT"),
        "air_process":       ("Compressed Air",       "NM3"),
        "air_fixed":         ("Compressed Air (Fixed)", "NM3"),
        "dm_process":        ("DM Water",             "M3"),
        "dm_fixed":          ("DM Water (Fixed)",     "M3"),
        "cw1_process":       ("Cooling Water 1",      "KM3"),
        "cw1_fixed":         ("Cooling Water 1 (Fixed)", "KM3"),
        "cw2_process":       ("Cooling Water 2",      "KM3"),
        "cw2_fixed":         ("Cooling Water 2 (Fixed)", "KM3"),
        "cooling_water_process": ("Cooling Water",     "KM3"),
        "cooling_water_fixed": ("Cooling Water (Fixed)", "KM3"),
        "raw_water_process": ("Raw Water",            "M3"),
        "raw_water_fixed":   ("Raw Water (Fixed)",    "M3"),
        "utility_water_process": ("Utility Water",     "M3"),
        "utility_water_fixed": ("Utility Water (Fixed)", "M3"),
        "nitrogen_asu_process": ("Nitrogen (ASU)",     "NM3"),
        "nitrogen_asu_fixed": ("Nitrogen (ASU) (Fixed)", "NM3"),
        "ret_steam_condensate_process": ("Ret Steam Condensate", "M3"),
        "ret_steam_condensate_fixed": ("Ret Steam Condensate (Fixed)", "M3"),
        "oxygen_process":    ("Oxygen",               "MT"),
        "oxygen_fixed":      ("Oxygen (Fixed)",       "MT"),
        "effluent_process":  ("Effluent",             "M3"),
        "effluent_fixed":    ("Effluent (Fixed)",     "M3"),
        "process_feed_water_process": ("Process Feed Water", "M3"),
        "process_feed_water_fixed":   ("Process Feed Water (Fixed)", "M3"),
    }
    print(f"  {'Demand Component':<28}  {'Value':>12}  {'Unit':<8}")
    print(f"  {'-'*28}  {'-'*12}  {'-'*8}")
    for key, (label, unit) in _META.items():
        val = merged.get(key, 0.0)
        marker = " *" if val > 0 else ""
        print(f"  {label:<28}  {val:>12.2f}  {unit:<8}{marker}")

    utility_rows = [
        ("Power", "power_process", "power_fixed", "MWh", True),
        ("LP Steam", "lp_process", "lp_fixed", "MT", False),
        ("MP Steam", "mp_process", "mp_fixed", "MT", False),
        ("HP Steam", "hp_process", "hp_fixed", "MT", False),
        ("SHP Steam", "shp_process", "shp_fixed", "MT", False),
        ("Compressed Air", "air_process", "air_fixed", "NM3", False),
        ("Nitrogen ASU", "nitrogen_asu_process", "nitrogen_asu_fixed", "NM3", False),
        ("DM Water", "dm_process", "dm_fixed", "M3", False),
        ("CW1", "cw1_process", "cw1_fixed", "KM3", False),
        ("CW2", "cw2_process", "cw2_fixed", "KM3", False),
        ("Cooling Water", "cooling_water_process", "cooling_water_fixed", "KM3", False),
        ("Raw Water", "raw_water_process", "raw_water_fixed", "M3", False),
        ("Utility Water", "utility_water_process", "utility_water_fixed", "M3", False),
        ("Ret Steam Condensate", "ret_steam_condensate_process", "ret_steam_condensate_fixed", "M3", False),
        ("Oxygen", "oxygen_process", "oxygen_fixed", "MT", False),
        ("Effluent", "effluent_process", "effluent_fixed", "M3", False),
    ]

    print(f"\n  UTILITY-WISE SUMMARY  (Process + Fixed)")
    print(f"  {'Utility':<24}  {'Process':>12}  {'Fixed':>12}  {'Total':>12}  {'Unit':<8}")
    print(f"  {'-'*24}  {'-'*12}  {'-'*12}  {'-'*12}  {'-'*8}")
    for label, process_key, fixed_key, unit, convert_process_to_mwh in utility_rows:
        process_val = float(process.get(process_key, 0.0))
        fixed_val   = float(fixed.get(fixed_key, 0.0))
        display_process = process_val / 1000.0 if convert_process_to_mwh else process_val
        total_val = display_process + fixed_val
        marker = " *" if total_val > 0 else ""
        if convert_process_to_mwh:
            unit = "MWh"
        print(
            f"  {label:<24}  {display_process:>12.2f}  {fixed_val:>12.2f}  "
            f"{total_val:>12.2f}  {unit:<8}{marker}"
        )


def _print_asset_capacity(plant_id: str, month: int, year: int):
    fy_year = year if month >= 4 else year - 1
    _sec(f"POWER ASSET CAPACITY  (CPPPowerAssetCapacity)  FY {fy_year}-{str(fy_year+1)[-2:]}")
    rows = fetch_power_asset_capacity_all_months(plant_id, fy_year)
    if not rows:
        print(f"  No capacity records found for FY {fy_year}")
        return
    print(f"  {len(rows)} asset(s) — Fixed Min/Max + monthly Min/Max MW")
    hdr = f"  {'Asset':<25}  {'Type':<10}  {'FxMin':>6}  {'FxMax':>6}" + \
          "".join(f"  {m[:3]:>3}Mn  {m[:3]:>3}Mx" for m in _ALL_MONTHS_LABELS)
    print(hdr)
    print(f"  {'-'*25}  {'-'*10}  {'-'*6}  {'-'*6}" + "  ------  ------"*12)
    for r in rows:
        fx_min = f"{r['fixed_min']:6.0f}" if r['fixed_min'] is not None else "   N/A"
        fx_max = f"{r['fixed_max']:6.0f}" if r['fixed_max'] is not None else "   N/A"
        month_vals = ""
        for m in _ALL_MONTHS_LABELS:
            mn = r.get(f"{m}_Min")
            mx = r.get(f"{m}_Max")
            month_vals += f"  {mn:6.0f}" if mn is not None else "     N/A"
            month_vals += f"  {mx:6.0f}" if mx is not None else "     N/A"
        print(f"  {r['asset_name']:<25}  {(r['asset_type'] or '-'):<10}  {fx_min}  {fx_max}{month_vals}")


def _print_demand_matching(plant_id: str, month: int, year: int):
    _sec(f"DEMAND vs CAPACITY MATCH  ({month}/{year})")
    print("  Running single-month budget calculation …")
    result = run_month(plant_id=plant_id, month=month, year=year, save_to_db=False)

    if not result.get("success"):
        print(f"  Calculation failed: {result.get('message')}")
        for r in result.get("reasons", []):
            print(f"    - {r}")
        return

    dc = result.get("demand_capacity")
    if dc:
        match   = dc.get("capacity_match", {})
        summary = dc.get("summary", {})
        print(f"  Data source: {dc.get('data_source', '?').upper()}")
        print(f"  {'Utility':<18}  {'Demand':>12}  {'Available':>12}  {'Surplus':>10}  Status")
        print(f"  {'-'*18}  {'-'*12}  {'-'*12}  {'-'*10}  {'-'*10}")
        _UTIL_LABELS = {
            "power":     ("Power",      "MWh"),
            "steam":     ("Steam",      "MT"),
            "air":       ("Air",        "NM3"),
            "dm_water":  ("DM Water",   "M3"),
            "cw1":       ("CW1",        "KM3"),
            "cw2":       ("CW2",        "KM3"),
            "raw_water": ("Raw Water",  "M3"),
        }
        for key, (label, unit) in _UTIL_LABELS.items():
            m = match.get(key, {})
            if not m:
                continue
            status  = m.get("status", "?")
            demand  = m.get("demand_mwh",    m.get("demand_mt",    m.get("demand",    0)))
            avail   = m.get("available_mwh", m.get("available_mt", m.get("available", 0)))
            surplus = m.get("surplus_mwh",   m.get("surplus_mt",   m.get("surplus",   0)))
            flag    = " <-- SHORTFALL" if status == "SHORTFALL" else ""
            print(f"  {label+' ('+unit+')':<18}  {demand:>12.1f}  {avail:>12.1f}  {surplus:>10.1f}  {status}{flag}")
        print(f"\n  Summary: {summary.get('total_utilities',0)} utilities  |  "
              f"Covered: {summary.get('covered',0)}  |  "
              f"Shortfall: {summary.get('shortfall',0)}  |  "
              f"No assets: {summary.get('no_assets',0)}")

    budget_ok  = result.get("budget_success", False)
    converged  = result.get("budget_converged", False)
    iterations = result.get("budget_iterations", 0)
    print(f"\n  BUDGET ENGINE:  success={budget_ok}  converged={converged}  iterations={iterations}")

    dispatch = result.get("final_dispatch", [])
    if dispatch:
        print(f"\n  POWER DISPATCH  ({len(dispatch)} assets)")
        print(f"  {'Asset':<25}  {'Type':<10}  {'Hours':>8}  {'Net MWh':>10}  {'Gross MWh':>10}  {'Load MW':>10}")
        print(f"  {'-'*25}  {'-'*10}  {'-'*8}  {'-'*10}  {'-'*10}  {'-'*10}")
        total_net = 0.0
        for a in dispatch:
            net   = float(a.get("NetMWh",   0))
            gross = float(a.get("GrossMWh", 0))
            load  = float(a.get("LoadMW",   0))
            hrs   = float(a.get("Hours",    0))
            total_net += net
            mark = "*" if net > 0 else " "
            print(f"  {mark}{a.get('AssetName','?'):<24}  {(a.get('AssetType','-')):<10}  "
                  f"{hrs:>8.1f}  {net:>10.2f}  {gross:>10.2f}  {load:>10.2f}")
        print(f"  {'TOTAL':<25}  {'':10}  {'':8}  {total_net:>10.2f}")

    steam = result.get("steam_result", {})
    if steam:
        print(f"\n  STEAM BALANCE")
        for level in ("lp_balance", "mp_balance", "hp_balance", "shp_balance"):
            bal = steam.get(level)
            if bal is None:
                continue
            label   = level.replace("_balance", "").upper()
            demand  = bal.get("total_demand", 0)
            supply  = bal.get("total_supply", 0)
            surplus = bal.get("surplus_mt", supply - demand)
            print(f"  {label:<4}  demand={demand:8.1f} MT  supply={supply:8.1f} MT  surplus={surplus:8.1f} MT")

    uc = result.get("utility_consumption", {})
    if uc:
        print(f"\n  UTILITY CONSUMPTION")
        print(f"  SHP from HRSG:  {uc.get('shp_from_hrsg_mt', 0):>10.1f} MT")
        print(f"  GT total:       {uc.get('gt_total_mwh', 0):>10.2f} MWh")
        print(f"  STG gross:      {uc.get('stg_gross_mwh', 0):>10.2f} MWh")
        print(f"  U4U power:      {uc.get('u4u_power_mwh', 0):>10.4f} MWh")


def _run_info_for_plant(plant_id: str, plant_name: str, month: int, year: int):
    print("\n" + "#" * 70)
    print(f"  PLANT: {plant_name}  [{plant_id[:8]}]")
    print(f"  Period: {month}/{year}")
    print("#" * 70)

    _print_plant_info(plant_id)
    _print_plant_assets(plant_id)
    _print_asset_ops_hours(plant_id, month, year)
    _print_asset_ops_hours_all_months(plant_id, month, year)
    _print_asset_priority(plant_id, month, year)
    _print_asset_priority_all_months(plant_id, month, year)
    _print_steam_asset_priority(plant_id, month, year)
    _print_steam_asset_priority_all_months(plant_id, month, year)
    _print_steam_generation_assets(plant_id)
    _print_plant_power_info(plant_id, month, year)
    _print_process_demand_master(plant_id, month, year)
    _print_calculated_process_demand(plant_id, month, year)
    _print_demand_segregation(plant_id, month, year)
    _print_asset_capacity(plant_id, month, year)
    _print_demand_matching(plant_id, month, year)


if __name__ == "__main__":
    setup_logging()

    parser = argparse.ArgumentParser(
        description="JMD Plant Budgeting",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
    python main.py
    python main.py --plant dta --month 4 --year 2025
    python main.py --plant dta,sez --month 4 --year 2025
    python main.py --plant all --fy 2025
    python main.py --plant c2,dta_pcg --fy 2025 --no-save
        """,
    )
    parser.add_argument("--plant",   type=str, default=None,
                        help="Plant short code(s), comma-separated or 'all': dta, dta_pcg, sez, sez_pcg, c2")
    parser.add_argument("--month",   type=int, default=None, help="Month (1-12)")
    parser.add_argument("--year",    type=int, default=None, help="Calendar year")
    parser.add_argument("--fy",      type=int, default=None,
                        help="FY start year (e.g. 2025 for FY 2025-26)")
    parser.add_argument("--no-save", action="store_true", help="Skip DB save")
    parser.add_argument("--info",   action="store_true",
                        help="Fetch and display plant data (info mode, no budget run)")
    args = parser.parse_args()

    print("=" * 70)
    print("JMD PLANT BUDGETING SYSTEM")
    print("=" * 70)

    # ── Resolve plant(s) ──────────────────────────────────────────────────────
    if args.plant:
        raw_codes = [c.strip().lower() for c in args.plant.split(",")]
        if raw_codes == ["all"]:
            raw_codes = list(_SHORT_CODES.keys())
        plants_to_run = []
        for code in raw_codes:
            if code not in _SHORT_CODES:
                print(f"ERROR: Unknown plant code '{code}'. "
                      f"Valid codes: {', '.join(_SHORT_CODES.keys())}, all")
                sys.exit(1)
            pid  = _SHORT_CODES[code]
            name = PLANT_REGISTRY[pid]["display_name"]
            plants_to_run.append((pid, name))
        # Deduplicate preserving order
        seen = set()
        unique_plants = []
        for pid, name in plants_to_run:
            if pid not in seen:
                seen.add(pid)
                unique_plants.append((pid, name))
        plants_to_run = unique_plants
    else:
        plants_to_run = _select_plants_interactive()

    print(f"\nPlants selected ({len(plants_to_run)}):")
    for pid, name in plants_to_run:
        print(f"  - {name}")
    print(f"Save to DB: {not args.no_save}")

    save_to_db = not args.no_save

    # ── Resolve month/year ────────────────────────────────────────────────────
    month = args.month
    year  = args.year
    fy    = args.fy

    if not month and not fy:
        print("\n--- PERIOD ---")
        mode = input("Run single month or full year? (m/y) [m]: ").strip().lower()
        if mode == "y":
            fy_input = input("Enter FY start year (e.g. 2025 for FY 2025-26): ").strip()
            fy = int(fy_input) if fy_input else None
        else:
            month_input = input("Enter Month (1-12): ").strip()
            year_input  = input("Enter Year: ").strip()
            month = int(month_input) if month_input else None
            year  = int(year_input)  if year_input  else None

    if not month and not fy:
        print("\nERROR: Must provide either --month + --year, or --fy")
        parser.print_help()
        sys.exit(1)

    # ── Info mode: fetch and display raw plant data, no budget ───────────────
    if args.info:
        info_month = month or 4
        info_year  = year  or (fy if fy else datetime.now().year)
        if not month and fy:
            info_month = 4
            info_year  = fy
        for plant_id, plant_name in plants_to_run:
            _run_info_for_plant(plant_id, plant_name, info_month, info_year)
        print("\n" + "=" * 70)
        print("  DONE (info mode)")
        print("=" * 70)
        sys.exit(0)

    # ── Run each plant ────────────────────────────────────────────────────────
    for plant_id, plant_name in plants_to_run:
        plant_output = os.path.join(OUTPUT_FOLDER, plant_name.replace(" ", "_"))
        plant_log    = os.path.join(LOG_FOLDER,    plant_name.replace(" ", "_"))

        print("\n" + "=" * 70)
        print(f"  PLANT: {plant_name}")
        print("=" * 70)

        if month and year:
            print(f"\nRunning: {plant_name}  {_MONTH_NAMES.get(month, month)}/{year}")
            print("-" * 70)

            with LogCapture() as cap:
                _print_new_schema_asset_snapshot(plant_id, month, year)
                result = run_month(plant_id, month, year, save_to_db=save_to_db)

            _print_month_summary(result)

            try:
                log_path = save_text_log(cap.text, plant_name, month, year, plant_log)
                print(f"\nLog saved: {log_path}")
            except Exception as e:
                print(f"\n[WARNING] Could not save log: {e}")

            if result.get("success"):
                try:
                    from engine.excel_report import write_month_report
                    excel_path = write_month_report(result, plant_output)
                    print(f"Excel saved: {excel_path}")
                except Exception as e:
                    print(f"[WARNING] Could not generate Excel: {e}")

        elif fy:
            print(f"\nRunning: {plant_name}  FY {fy}-{str(fy + 1)[-2:]}")
            print("-" * 70)

            with LogCapture() as cap:
                result = run_full_year(plant_id, fy, save_to_db=save_to_db)

            _print_full_year_summary(result)

            try:
                log_path = save_full_year_log(cap.text, plant_name, f"{fy}-{str(fy+1)[-2:]}", plant_log)
                print(f"\nLog saved: {log_path}")
            except Exception as e:
                print(f"\n[WARNING] Could not save log: {e}")

            summary = result.get("summary", {})
            if summary.get("successful", 0) > 0:
                try:
                    from engine.excel_report import write_full_year_report
                    excel_path = write_full_year_report(result, plant_output)
                    print(f"Excel saved: {excel_path}")
                except Exception as e:
                    print(f"[WARNING] Could not generate Excel: {e}")
