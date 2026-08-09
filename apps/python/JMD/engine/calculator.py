"""
Shared calculation engine for all JMD plants.

Usage (from any plant run.py):
    from engine.calculator import run_month, run_full_year

Both functions are parameterised by plant_id — no duplication across plant folders.
"""

import sys
import os
import time
import threading
import concurrent.futures
import multiprocessing
import logging
from datetime import datetime
from io import StringIO

sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from database.queries import (
    fetch_plant_info,
    fetch_asset_operational_hours_all_months,
    fetch_asset_priority_all_months,
    fetch_steam_asset_priority_all_months,
    fetch_steam_generation_assets,
    fetch_plant_assets,
    fetch_process_demands,
    fetch_fixed_consumption,
    fetch_process_demands_raw,
    fetch_fixed_consumption_raw,
    fetch_import_power,
    fetch_norms,
    fetch_hrsg_availability,
    fetch_stg_extraction_lookup,
    fetch_hrsg_heat_rate_lookup,
    fetch_gt_heat_rate_lookup,
    fetch_power_asset_capacity_all_months,
    fetch_steam_asset_capacity_all_months,
    DataFetchError,
)
from plant_mapper import PLANT_REGISTRY
from engine.demand_capacity import run_demand_capacity
from engine.budget import calculate_budget
from engine.dispatch_engine import dispatch_power, dispatch_steam
from engine.norms_reader_factory import get_norms_reader
from engine.u4u_iteration_loop import U4UIterationLoop
from services.norms_save_service import save_calculated_norms

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# FY helpers
# ---------------------------------------------------------------------------

_MONTH_NAMES = {
    1: "January", 2: "February", 3: "March", 4: "April",
    5: "May",     6: "June",     7: "July",   8: "August",
    9: "September", 10: "October", 11: "November", 12: "December",
}

_FY_MONTHS = [4, 5, 6, 7, 8, 9, 10, 11, 12, 1, 2, 3]


def _fy_string(month: int, year: int) -> str:
    start = year if month >= 4 else year - 1
    return f"{start}-{str(start + 1)[-2:]}"


def _fy_months(fy_start_year: int) -> list:
    """Return list of (month, year) for a full FY starting in April."""
    return [
        (4, fy_start_year), (5, fy_start_year), (6, fy_start_year),
        (7, fy_start_year), (8, fy_start_year), (9, fy_start_year),
        (10, fy_start_year), (11, fy_start_year), (12, fy_start_year),
        (1, fy_start_year + 1), (2, fy_start_year + 1), (3, fy_start_year + 1),
    ]


# ---------------------------------------------------------------------------
# Demand fetch (process + fixed combined)
# ---------------------------------------------------------------------------

def _get_demands(plant_id: str, month: int, year: int) -> dict:
    process = fetch_process_demands_raw(plant_id, month, year)
    fixed   = fetch_fixed_consumption_raw(plant_id, month, year)
    _log_utility_demand_rollup(process, fixed, month, year)
    merged = {}
    for k, v in process.items():
        merged[k] = merged.get(k, 0.0) + v
    for k, v in fixed.items():
        merged[k] = merged.get(k, 0.0) + v
    return merged


def _log_utility_demand_rollup(process: dict, fixed: dict, month: int, year: int) -> None:
    """Log utility-wise process, fixed, and combined demand totals.

    Derives utility list entirely from what is present in the DB dicts — no
    hardcoded utility names.  Power values from process demand are in KWH and
    are detected by normalized name so they are displayed in MWh.
    """
    from engine.u4u_iteration_loop import _normalize_for_match  # local to avoid circular import
    all_utilities = sorted(set(process) | set(fixed))

    logger.info("  [DEMAND] ==================================================")
    logger.info("  [DEMAND] Utility-wise demand rollup  (%s/%s)", month, year)
    logger.info("  [DEMAND] %-28s  %12s  %12s  %12s", "Utility", "Process", "Fixed", "Total")
    logger.info("  [DEMAND] %s  %s  %s  %s", "-" * 28, "-" * 12, "-" * 12, "-" * 12)

    for util_name in all_utilities:
        process_val = float(process.get(util_name, 0.0))
        fixed_val   = float(fixed.get(util_name, 0.0))
        is_power = _normalize_for_match(util_name) in ("powerdis", "power")
        if is_power:
            process_val = process_val / 1000.0
        total_val = process_val + fixed_val
        logger.info(
            "  [DEMAND] %-28s  %12.2f  %12.2f  %12.2f",
            util_name, process_val, fixed_val, total_val,
        )

    logger.info("  [DEMAND] ==================================================")


# ---------------------------------------------------------------------------
# Single month calculation
# ---------------------------------------------------------------------------

def run_month(plant_id: str, month: int, year: int, save_to_db: bool = True) -> dict:
    """
    Run the full budget calculation for one plant / one month.

    Fetches:
      - process demands (CalculatedProcessDemand)
      - fixed consumption (CPPFixedConsumption)
      - operational hours (CPPAssetOperationalHours)
      - asset priority    (CPPPowerAssetPriority + CPPSteamAssetsPriority)
      - import power      (CPPImportPower*)
      - norms             (NormsMonthDetail)
      - HRSG / STG / GT lookup tables

    Returns a result dict:
        {
            "plant_id":      str,
            "plant_name":    str,
            "month":         int,
            "year":          int,
            "fy":            str,
            "demands":       dict,
            "assets":        list,
            "op_hours":      list,    all 12 months
            "power_priority":list,    all 12 months
            "steam_priority":list,    all 12 months
            "steam_assets":  list,
            "import_power":  dict,
            "norms":         list,
            "hrsg_avail":    list,
            "stg_lookup":    DataFrame,
            "hrsg_heat_rate":DataFrame,
            "gt_heat_rate":  DataFrame,
            "success":       bool,
            "message":       str,
            "execution_time_seconds": float,
        }
    """
    start = time.time()
    meta = PLANT_REGISTRY.get(plant_id.upper(), {})
    plant_name = meta.get("display_name", plant_id)
    fy_start = year if month >= 4 else year - 1
    fy = _fy_string(month, year)

    logger.info("  [CALC] %s  %s/%s  (%s)", plant_name, month, year, fy)

    # Fetch each data source individually. DataFetchError means a DB schema problem
    # (missing table/column). We log it as a warning but continue — power_dispatch
    # and demand_capacity both have dummy-data fallbacks so partial DB data is OK.
    warnings = []

    def _fetch(label, fn, *args, default=None, **kwargs):
        """Call fn(*args) and return its result; on any error return default and log."""
        try:
            return fn(*args, **kwargs)
        except DataFetchError as e:
            warnings.append(f"{label}: {e.original}")
            logger.warning("  [CALC] %s fetch failed (schema): %s", label, e.original)
            return default
        except Exception as e:
            logger.warning("  [CALC] %s fetch failed: %s", label, e)
            warnings.append(f"{label}: {e}")
            return default

    demands      = _fetch("demands",      _get_demands,                             plant_id, month, year, default={})
    assets       = _fetch("assets",       fetch_plant_assets,                       plant_id, default=[])
    op_hours     = _fetch("op_hours",     fetch_asset_operational_hours_all_months, plant_id, fy_start, default=[])
    power_pri    = _fetch("power_pri",    fetch_asset_priority_all_months,          plant_id, fy_start, default=[])
    steam_pri    = _fetch("steam_pri",    fetch_steam_asset_priority_all_months,    plant_id, fy_start, default=[])
    steam_assets = _fetch("steam_assets", fetch_steam_generation_assets,            plant_id, default=[])
    import_power = _fetch("import_power", fetch_import_power,                       plant_id, month, year,
                          default={"success": False, "total_mwh": 0.0, "per_source": []})
    norms        = _fetch("norms",        fetch_norms,                              plant_id, month, year, default=[])
    hrsg_avail   = _fetch("hrsg_avail",   fetch_hrsg_availability,                  plant_id, month, year, default=[])
    stg_df       = _fetch("stg_lookup",   fetch_stg_extraction_lookup,              plant_id, month, year, default=None)
    hrsg_df      = _fetch("hrsg_hr",      fetch_hrsg_heat_rate_lookup,              plant_id, month, year, default=None)
    gt_df        = _fetch("gt_hr",        fetch_gt_heat_rate_lookup,                plant_id, month, year, default=None)

    power_caps   = _fetch("power_caps",   fetch_power_asset_capacity_all_months,    plant_id, fy_start, default=[])
    steam_caps   = _fetch("steam_caps",   fetch_steam_asset_capacity_all_months,    plant_id, fy_start, default=[])

    reasons = warnings  # kept for backward-compat in result dict (non-fatal)

    result = {
        "plant_id":       plant_id,
        "plant_name":     plant_name,
        "month":          month,
        "year":           year,
        "fy":             fy,
        "demands":        demands,
        "assets":         assets,
        "power_caps":     power_caps,
        "steam_caps":     steam_caps,
        "op_hours":       op_hours,
        "power_priority": power_pri,
        "steam_priority": steam_pri,
        "steam_assets":   steam_assets,
        "import_power":   import_power,
        "norms":          norms,
        "hrsg_avail":     hrsg_avail,
        "stg_lookup":     stg_df,
        "hrsg_heat_rate": hrsg_df,
        "gt_heat_rate":   gt_df,
        "success":        True,
        "message":        "OK" if not warnings else f"OK (with {len(warnings)} fetch warning(s))",
        "reasons":        warnings,
        "execution_time_seconds": round(time.time() - start, 2),
    }

    # Demand segregation + capacity matching (uses JSON fallback when DB empty)
    try:
        result["demand_capacity"] = run_demand_capacity(plant_id, month, year, result)
    except Exception as e:
        logger.warning("  [CALC] demand_capacity skipped: %s", e)
        result["demand_capacity"] = None

    # Load norms once — shared by power dispatch and steam dispatch
    # Uses factory to select ODS or DB reader based on feature flag
    norms_reader = get_norms_reader(plant_id, month, year)
    if norms_reader.is_available:
        norms_reader.log_all_norms()

    # U4U Iteration Loop — iteratively dispatches power + steam and calculates
    # U4U consumption cascades until convergence (0.01% tolerance on all utilities).
    # This replaces the standalone power/steam dispatch calls above.
    try:
        u4u_loop = U4UIterationLoop(
            plant_id=plant_id,
            month=month,
            year=year,
            initial_demands=demands,
            ods_reader=norms_reader,
            import_power=import_power,
            gt_heat_rate_df=gt_df,
            hrsg_heat_rate_df=hrsg_df,
        )
        u4u_result = u4u_loop.run()
        result["u4u_iteration"] = u4u_result
        result["power_dispatch"] = u4u_result.get("final_power_result")
        result["new_steam_dispatch"] = u4u_result.get("final_steam_result")
        result["u4u_converged"] = u4u_result.get("converged", False)
        result["u4u_iterations"] = u4u_result.get("iterations_used", 0)

        # Save calculated norms to database
        save_result = save_calculated_norms(month, year, u4u_result, dry_run=False)
        result["norms_save"] = save_result
    except Exception as e:
        logger.warning("  [CALC] U4U iteration loop failed: %s", e)
        result["u4u_iteration"] = None
        result["power_dispatch"] = None
        result["new_steam_dispatch"] = None
        result["u4u_converged"] = False
        result["u4u_iterations"] = 0

    # OLD budget calculation (power dispatch + steam balance + USD iteration)
    # Disabled — being replaced by new dispatch_engine.py step by step.
    # Kept here for reference; will be removed once full replacement is complete.
    # try:
    #     demands = {**(demands or {})}
    #     budget = calculate_budget(
    #         plant_id=plant_id,
    #         plant_name=plant_name,
    #         month=month,
    #         year=year,
    #         norms=norms or [],
    #         hrsg_avail=hrsg_avail or [],
    #         stg_extraction_df=stg_df,
    #         demands=demands,
    #         save_to_db=save_to_db,
    #     )
    #     result["budget"] = budget
    #     result["budget_success"]     = budget.get("overall_success", False)
    #     result["budget_converged"]   = budget.get("converged", False)
    #     result["budget_iterations"]  = budget.get("iterations_used", 0)
    #     result["final_dispatch"]     = budget.get("final_dispatch", [])
    #     result["steam_result"]       = budget.get("steam_result")
    #     result["hrsg_dispatch"]      = budget.get("hrsg_dispatch")
    #     result["utility_consumption"]= budget.get("utility_consumption")
    #     result["supplementary_firing_mt"] = budget.get("supplementary_firing_mt", 0)
    # except Exception as e:
    #     logger.warning("  [CALC] budget calculation skipped: %s", e)
    #     result["budget"] = None
    #     result["budget_success"] = False

    result["execution_time_seconds"] = round(time.time() - start, 2)
    logger.info(
        "  [CALC] Done  %s/%s  (%.1fs)  assets=%d  norms=%d",
        month, year, result["execution_time_seconds"],
        len(assets), len(norms),
    )
    return result


# ---------------------------------------------------------------------------
# Full financial year (parallel)
# ---------------------------------------------------------------------------

def run_full_year(
    plant_id: str,
    fy_start_year: int,
    save_to_db: bool = True,
    max_workers: int = None,
) -> dict:
    """
    Run budget calculation for all 12 months of a financial year.

    Args:
        plant_id:      CPP plant UUID
        fy_start_year: e.g. 2025 for FY 2025-26 (April 2025 – March 2026)
        save_to_db:    passed through to run_month
        max_workers:   thread pool size (default: min(4, cpu_count))

    Returns:
        {
            "plant_id":   str,
            "fy":         str,
            "run_at":     str  (ISO datetime),
            "months":     { "2025_04": result_dict, ... },
            "summary": {
                "successful": int,
                "failed":     int,
                "total_assets": int,
                "total_norms":  int,
            }
        }
    """
    meta = PLANT_REGISTRY.get(plant_id.upper(), {})
    plant_name = meta.get("display_name", plant_id)
    fy = f"{fy_start_year}-{str(fy_start_year + 1)[-2:]}"
    months = _fy_months(fy_start_year)
    workers = max_workers or min(4, multiprocessing.cpu_count())

    logger.info("[FULL YEAR] %s  FY %s  (%d workers)", plant_name, fy, workers)

    print_lock = threading.Lock()
    _real_stdout = sys.stdout
    results = {}

    def _run(month_year):
        m, y = month_year
        res = run_month(plant_id, m, y, save_to_db)
        status = "OK  " if res["success"] else "FAIL"
        with print_lock:
            _real_stdout.write(
                f"  [{status}] {_MONTH_NAMES[m]} {y}"
                f"  assets={len(res.get('assets', []))}"
                f"  norms={len(res.get('norms', []))}"
                f"  ({res['execution_time_seconds']:.1f}s)\n"
            )
            _real_stdout.flush()
        return (f"{y}_{m:02d}", res)

    run_start = time.time()
    with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as ex:
        for key, res in ex.map(_run, months):
            results[key] = res

    elapsed = time.time() - run_start

    # Sort into FY order
    month_order = {f"{y}_{m:02d}": i for i, (m, y) in enumerate(months)}
    sorted_results = dict(sorted(results.items(), key=lambda kv: month_order.get(kv[0], 99)))

    successful = sum(1 for r in sorted_results.values() if r.get("success"))
    failed     = sum(1 for r in sorted_results.values() if not r.get("success"))
    total_norms = sum(len(r.get("norms", [])) for r in sorted_results.values())

    logger.info("[FULL YEAR] Done  FY %s  %d/12 OK  %.1fs", fy, successful, elapsed)

    return {
        "plant_id":  plant_id,
        "plant_name": plant_name,
        "fy":        fy,
        "run_at":    datetime.now().isoformat(timespec="seconds"),
        "months":    sorted_results,
        "summary": {
            "successful":   successful,
            "failed":       failed,
            "total_months": len(months),
            "total_norms":  total_norms,
        },
    }
