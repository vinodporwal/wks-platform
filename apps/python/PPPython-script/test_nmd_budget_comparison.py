"""
Test script: Compare dynamic budget generator vs existing hardcoded implementation.

Runs both build_nmd_budget_comparison_text() (existing) and
generate_nmd_budget_comparison() (new dynamic) with the same calculation
result and compares the outputs line-by-line.

Usage:
    py -3.11 test_nmd_budget_comparison.py --month 4 --year 2026 --plant-id <uuid>

    py -3.11 test_nmd_budget_comparison.py --month 4 --year 2026 --dry-run
"""

import argparse
import os
import sys
import time
import traceback
from typing import List, Tuple

# Ensure services are importable
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))


def _parse_args():
    parser = argparse.ArgumentParser(description="Compare NMD budget generators")
    parser.add_argument("--month", type=int, default=4, help="Financial month (1-12)")
    parser.add_argument("--year", type=int, default=2026, help="Calendar year")
    parser.add_argument("--plant-id", type=str, default=None, help="CPP Plant UUID")
    parser.add_argument("--dry-run", action="store_true", help="Use mock data instead of DB")
    parser.add_argument("--bpc-path", type=str, default=None, help="Path to BPC.ods file")
    parser.add_argument("--output-dir", type=str, default=None, help="Directory to write comparison files")
    return parser.parse_args()


def _financial_year(month: int, year: int) -> int:
    if month >= 4:
        return year
    return year - 1


def _build_mock_calculation_result() -> dict:
    """
    Build a mock calculation result for dry-run testing without DB.

    This mimics the structure returned by calculate_budget_with_iteration().
    """
    return {
        "usd_result": {
            "final_dispatch": [
                {"AssetName": "JMD - C2-GTG 1 (Plant-1)", "GrossMWh": 22320, "NetMWh": 21504,
                 "Hours": 720, "LoadMW": 31.0, "HeatRate": 2850, "FreeSteam": 0.28},
                {"AssetName": "JMD - C2-GTG 2 (Plant-2)", "GrossMWh": 0, "NetMWh": 0,
                 "Hours": 0, "LoadMW": 0},
                {"AssetName": "JMD - C2-GTG 3 (Plant-3)", "GrossMWh": 5810, "NetMWh": 5600,
                 "Hours": 720, "LoadMW": 8.07, "HeatRate": 2900, "FreeSteam": 0.28},
                {"AssetName": "JMD - C2-STG (Steam Turbine)", "GrossMWh": 10800, "NetMWh": 10500,
                 "Hours": 720, "LoadMW": 15.0},
            ],
            "power_result": {
                "totalDemandUnits": 45314,
                "totalNetGeneration": 27104,
                "totalGrossGeneration": 38930,
                "mandatoryImportUsed": 18000,
                "processDemand": 25000,
                "fixedDemand": 2500,
                "u4uPower": 16072,
            },
            "final_steam_balance": {
                "shp_balance": {
                    "shp_total_demand": 100858,
                    "shp_total_supply": 100858,
                    "can_meet_demand": True,
                },
                "hp_balance": {
                    "hp_total": 15000,
                    "hp_from_prds": 15000,
                },
                "mp_balance": {
                    "mp_total": 30000,
                    "mp_from_prds": 21000,
                    "mp_from_stg": 9000,
                    "mp_stg_ratio": 0.30,
                    "mp_prds_ratio": 0.70,
                },
                "lp_balance": {
                    "lp_total": 50000,
                    "lp_from_prds": 19000,
                    "lp_from_stg": 31000,
                    "lp_stg_ratio": 0.62,
                    "lp_prds_ratio": 0.38,
                },
            },
            "stg_extraction": {
                "lp_from_stg": 31000,
                "mp_from_stg": 9000,
                "lp_stg_ratio": 0.62,
                "mp_stg_ratio": 0.30,
                "sp_steam_power": 3.385,
                "stg_shp_inlet_mt": 36500,
                "stg_condensate_m3": 21000,
                "stg_condensate_norm": -0.0029,
                "steam_for_power_tph": 50.78,
                "condensing_load_m3hr": 29.17,
            },
            "hrsg_dispatch": {
                "total_free_steam_mt": 25986,
                "total_shp_supply_mt": 100858,
                "min_supply_mt": 100858,
                "hrsg1_dispatched_mt": 25986,
                "hrsg2_dispatched_mt": 74872,
                "hrsg3_dispatched_mt": 0,
                "hrsg_dispatch": [
                    {"name": "HRSG-1", "dispatched_supp_mt": 0, "free_steam_mt": 25986},
                    {"name": "HRSG-2", "dispatched_supp_mt": 74872, "free_steam_mt": 0},
                    {"name": "HRSG-3", "dispatched_supp_mt": 0, "free_steam_mt": 0},
                ],
            },
        },
        "power_result": {
            "totalDemandUnits": 45314,
            "totalNetGeneration": 27104,
            "mandatoryImportUsed": 18000,
        },
        "steam_result": {
            "shp_balance": {"shp_total_demand": 100858},
            "hp_balance": {"hp_total": 15000, "hp_from_prds": 15000},
            "mp_balance": {"mp_total": 30000, "mp_from_prds": 21000, "mp_from_stg": 9000},
            "lp_balance": {"lp_total": 50000, "lp_from_prds": 19000, "lp_from_stg": 31000},
        },
        "stg_extraction": {
            "lp_from_stg": 31000,
            "mp_from_stg": 9000,
            "sp_steam_power": 3.385,
            "stg_shp_inlet_mt": 36500,
            "stg_condensate_m3": 21000,
        },
        "utility_consumption": {
            "bfw": {"total_m3": 69965},
            "dm_water": {"total_m3": 102010},
            "cooling_water": {"cw1_total_km3": 15194, "cw2_total_km3": 23384},
            "compressed_air": {"total_nm3": 6095102},
            "oxygen_mt": 5786,
            "effluent_m3": 243000,
            "shp_from_hrsg1": 25986,
            "shp_from_hrsg2": 74872,
            "shp_from_hrsg3": 0,
            "natural_gas": {
                "gt1_ng_norm": 0.0095,
                "gt2_ng_norm": 0.0101,
                "gt3_ng_norm": 0.0095,
                "hrsg1_ng_norm": 2.8064,
                "hrsg2_ng_norm": 2.8064,
                "hrsg3_ng_norm": 2.8168,
                "gt1_mmbtu": 211904,
                "gt2_mmbtu": 0,
                "gt3_mmbtu": 55195,
            },
        },
    }


def _run_existing(
    month: int, year: int, financial_year: int,
    calc_result: dict, bpc_path: str,
) -> Tuple[str, dict, dict, float]:
    """Run existing hardcoded comparison."""
    from services.nmd_budget_comparison_service import build_nmd_budget_comparison_text

    t0 = time.time()
    text, cpp_totals, bpc_totals = build_nmd_budget_comparison_text(
        month, year, financial_year, calc_result, bpc_path,
    )
    elapsed = time.time() - t0
    return text, cpp_totals, bpc_totals, elapsed


def _run_dynamic(
    month: int, year: int, financial_year: int,
    calc_result: dict, bpc_path: str,
) -> Tuple[str, dict, dict, float]:
    """Run new dynamic comparison."""
    from services.nmd_budget_generator import generate_nmd_budget_comparison

    t0 = time.time()
    text, cpp_totals, bpc_totals = generate_nmd_budget_comparison(
        month, year, financial_year, calc_result, bpc_path,
    )
    elapsed = time.time() - t0
    return text, cpp_totals, bpc_totals, elapsed


def _parse_line(line: str) -> dict:
    """Parse a comparison output line into its components."""
    parts = line.split()
    if len(parts) < 8:
        return None
    return {
        "raw": line,
        "plant": parts[0] if len(parts) > 0 else "",
        "utility": parts[1] if len(parts) > 1 else "",
        "material": parts[2] if len(parts) > 2 else "",
        "uom": parts[3] if len(parts) > 3 else "",
    }


def _extract_data_lines(text: str) -> List[str]:
    """Extract only data lines (not headers/separators) from comparison text."""
    lines = []
    for line in text.split("\n"):
        stripped = line.strip()
        if not stripped:
            continue
        if stripped.startswith("=") or stripped.startswith("-"):
            continue
        if "GENERATING PLANT" in stripped or "NMD BUDGET FORMAT" in stripped:
            continue
        if "DISTRIBUTION SECTION" in stripped or "SUMMARY" in stripped:
            continue
        if "Total Calculated" in stripped or "Total Reference" in stripped:
            continue
        if "Difference:" in stripped or "% Difference:" in stripped:
            continue
        # Data lines have multiple space-separated fields
        if len(stripped.split()) >= 5:
            lines.append(stripped)
    return lines


def _compare_lines(existing_text: str, dynamic_text: str) -> dict:
    """Compare two comparison texts and report differences."""
    existing_lines = _extract_data_lines(existing_text)
    dynamic_lines = _extract_data_lines(dynamic_text)

    # Parse lines into material-keyed dicts
    existing_map = {}
    for line in existing_lines:
        parsed = _parse_line(line)
        if parsed:
            key = f"{parsed['plant']}|{parsed['utility']}|{parsed['material']}"
            existing_map[key] = line

    dynamic_map = {}
    for line in dynamic_lines:
        parsed = _parse_line(line)
        if parsed:
            key = f"{parsed['plant']}|{parsed['utility']}|{parsed['material']}"
            dynamic_map[key] = line

    # Find matching, missing, and extra keys
    existing_keys = set(existing_map.keys())
    dynamic_keys = set(dynamic_map.keys())

    matching = existing_keys & dynamic_keys
    missing_in_dynamic = existing_keys - dynamic_keys
    extra_in_dynamic = dynamic_keys - existing_keys

    # Check for value differences in matching keys
    value_diffs = []
    for key in sorted(matching):
        if existing_map[key] != dynamic_map[key]:
            value_diffs.append({
                "key": key,
                "existing": existing_map[key],
                "dynamic": dynamic_map[key],
            })

    return {
        "existing_line_count": len(existing_lines),
        "dynamic_line_count": len(dynamic_lines),
        "matching_keys": len(matching),
        "missing_in_dynamic": sorted(missing_in_dynamic),
        "extra_in_dynamic": sorted(extra_in_dynamic),
        "value_differences": value_diffs,
    }


def main():
    args = _parse_args()
    month = args.month
    year = args.year
    financial_year = _financial_year(month, year)

    bpc_path = args.bpc_path or os.path.join(
        os.path.dirname(os.path.abspath(__file__)), "BPC.ods"
    )

    print("=" * 80)
    print("NMD Budget Comparison Test: Existing vs Dynamic")
    print("=" * 80)
    print(f"  Month: {month}, Year: {year}, FY: {financial_year}-{str(financial_year + 1)[-2:]}")
    print(f"  BPC path: {bpc_path}")
    print(f"  Mode: {'DRY RUN (mock data)' if args.dry_run else 'LIVE (DB calculation)'}")
    print()

    # ------------------------------------------------------------------
    # Step 1: Get calculation result
    # ------------------------------------------------------------------
    if args.dry_run:
        print("  Using mock calculation result...")
        calc_result = _build_mock_calculation_result()
    else:
        print("  Running budget calculation with iteration...")
        from services.budget_service import calculate_budget_with_iteration
        from services.process_demand_service import get_process_demand_for_month
        from services.fixed_consumption_service import get_fixed_consumption_for_month

        # Fetch process demands
        process_demands = get_process_demand_for_month(month, year)
        fixed_demands = get_fixed_consumption_for_month(month, year)

        # Extract demands
        lp_process = process_demands.get("LP Steam_Dis", 0)
        mp_process = process_demands.get("MP Steam_Dis", 0)
        hp_process = process_demands.get("HP Steam_Dis", 0)
        shp_process = process_demands.get("SHP Steam_Dis", 0)
        power_process = process_demands.get("Power_Dis", 0)

        lp_fixed = fixed_demands.get("LP Steam_Dis", 0)
        mp_fixed = fixed_demands.get("MP Steam_Dis", 0)
        hp_fixed = fixed_demands.get("HP Steam_Dis", 0)
        shp_fixed = fixed_demands.get("SHP Steam_Dis", 0)
        power_fixed = fixed_demands.get("Power_Dis", 0)

        plant_id = args.plant_id or "00000000-0000-0000-0000-000000000000"

        calc_result = calculate_budget_with_iteration(
            month=month, year=year, cpp_plant_id=plant_id,
            lp_process=lp_process, lp_fixed=lp_fixed,
            mp_process=mp_process, mp_fixed=mp_fixed,
            hp_process=hp_process, hp_fixed=hp_fixed,
            shp_process=shp_process, shp_fixed=shp_fixed,
        )

    print(f"  Calculation result keys: {list(calc_result.keys())}")
    print()

    # ------------------------------------------------------------------
    # Step 2: Run existing (hardcoded) comparison
    # ------------------------------------------------------------------
    print("  Running EXISTING build_nmd_budget_comparison_text()...")
    try:
        ex_text, ex_cpp, ex_bpc, ex_time = _run_existing(
            month, year, financial_year, calc_result, bpc_path,
        )
        print(f"    ✓ Completed in {ex_time:.2f}s, {len(ex_text)} chars")
    except Exception as e:
        print(f"    ✗ FAILED: {e}")
        traceback.print_exc()
        return

    # ------------------------------------------------------------------
    # Step 3: Run new dynamic comparison
    # ------------------------------------------------------------------
    print("  Running DYNAMIC generate_nmd_budget_comparison()...")
    try:
        dyn_text, dyn_cpp, dyn_bpc, dyn_time = _run_dynamic(
            month, year, financial_year, calc_result, bpc_path,
        )
        print(f"    ✓ Completed in {dyn_time:.2f}s, {len(dyn_text)} chars")
    except Exception as e:
        print(f"    ✗ FAILED: {e}")
        traceback.print_exc()
        return

    # ------------------------------------------------------------------
    # Step 4: Compare results
    # ------------------------------------------------------------------
    print()
    print("=" * 80)
    print("COMPARISON RESULTS")
    print("=" * 80)

    comparison = _compare_lines(ex_text, dyn_text)

    print(f"  Existing lines:  {comparison['existing_line_count']}")
    print(f"  Dynamic lines:   {comparison['dynamic_line_count']}")
    print(f"  Matching keys:   {comparison['matching_keys']}")
    print(f"  Missing in dynamic: {len(comparison['missing_in_dynamic'])}")
    print(f"  Extra in dynamic:   {len(comparison['extra_in_dynamic'])}")
    print(f"  Value differences:  {len(comparison['value_differences'])}")
    print()

    if comparison['missing_in_dynamic']:
        print("  --- Lines MISSING in dynamic (present in existing) ---")
        for key in comparison['missing_in_dynamic'][:20]:
            print(f"    {key}")
        if len(comparison['missing_in_dynamic']) > 20:
            print(f"    ... and {len(comparison['missing_in_dynamic']) - 20} more")
        print()

    if comparison['extra_in_dynamic']:
        print("  --- Lines EXTRA in dynamic (not in existing) ---")
        for key in comparison['extra_in_dynamic'][:20]:
            print(f"    {key}")
        if len(comparison['extra_in_dynamic']) > 20:
            print(f"    ... and {len(comparison['extra_in_dynamic']) - 20} more")
        print()

    if comparison['value_differences']:
        print("  --- VALUE DIFFERENCES (first 10) ---")
        for diff in comparison['value_differences'][:10]:
            print(f"    Key: {diff['key']}")
            print(f"      Existing: {diff['existing']}")
            print(f"      Dynamic:  {diff['dynamic']}")
            print()
        if len(comparison['value_differences']) > 10:
            print(f"    ... and {len(comparison['value_differences']) - 10} more")
        print()

    # ------------------------------------------------------------------
    # Step 5: Compare totals
    # ------------------------------------------------------------------
    print("  --- TOTALS COMPARISON ---")
    ex_total_cpp = sum(ex_cpp.values()) if ex_cpp else 0
    ex_total_bpc = sum(ex_bpc.values()) if ex_bpc else 0
    dyn_total_cpp = sum(dyn_cpp.values()) if dyn_cpp else 0
    dyn_total_bpc = sum(dyn_bpc.values()) if dyn_bpc else 0

    print(f"  Existing: CPP total = {ex_total_cpp:,.2f}, BPC total = {ex_total_bpc:,.2f}")
    print(f"  Dynamic:  CPP total = {dyn_total_cpp:,.2f}, BPC total = {dyn_total_bpc:,.2f}")
    print(f"  CPP diff: {abs(ex_total_cpp - dyn_total_cpp):,.2f}")
    print(f"  BPC diff: {abs(ex_total_bpc - dyn_total_bpc):,.2f}")
    print()

    # ------------------------------------------------------------------
    # Step 6: Write output files if requested
    # ------------------------------------------------------------------
    if args.output_dir:
        os.makedirs(args.output_dir, exist_ok=True)
        ex_path = os.path.join(args.output_dir, f"existing_{month:02d}_{year}.txt")
        dyn_path = os.path.join(args.output_dir, f"dynamic_{month:02d}_{year}.txt")
        with open(ex_path, "w", encoding="utf-8") as f:
            f.write(ex_text)
        with open(dyn_path, "w", encoding="utf-8") as f:
            f.write(dyn_text)
        print(f"  Written: {ex_path}")
        print(f"  Written: {dyn_path}")
        print()

    # ------------------------------------------------------------------
    # Step 7: Final verdict
    # ------------------------------------------------------------------
    total_diffs = (
        len(comparison['missing_in_dynamic'])
        + len(comparison['extra_in_dynamic'])
        + len(comparison['value_differences'])
    )

    print("=" * 80)
    if total_diffs == 0:
        print("✓ PASS: Outputs match perfectly!")
    elif total_diffs <= 5:
        print(f"⚠ CLOSE: {total_diffs} minor differences found")
    else:
        print(f"✗ MISMATCH: {total_diffs} differences found")
    print("=" * 80)


if __name__ == "__main__":
    main()
