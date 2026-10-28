"""
Shared CLI entry point used by every plant's run.py.

    from engine.run_plant import main
    main(plant_id="<UUID>", plant_name="DTA CPP")

CLI usage (from any plant folder):
    python run.py --fy 2025
    python run.py --month 4 --year 2025
    python run.py --month 4 --year 2025 --no-excel
    python run.py --fy 2025 --no-save
"""

import sys
import os
import argparse
import logging
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from engine.calculator import run_month, run_full_year
from engine.excel_report import write_month_report, write_full_year_report
from engine.report_logger import setup_logging, LogCapture, save_text_log, save_full_year_log

logger = logging.getLogger(__name__)

_DEFAULT_OUTPUT = os.path.join(
    os.path.dirname(os.path.dirname(__file__)), "output"
)


def main(plant_id: str, plant_name: str, output_folder: str = None):
    setup_logging()
    output_folder = output_folder or _DEFAULT_OUTPUT

    parser = argparse.ArgumentParser(
        description=f"Run budget calculation for {plant_name}",
    )
    parser.add_argument("--fy",    type=int, help="FY start year (e.g. 2025 for FY 2025-26)")
    parser.add_argument("--month", type=int, help="Month (1-12)")
    parser.add_argument("--year",  type=int, help="Calendar year")
    parser.add_argument("--no-save",  action="store_true", help="Skip DB save")
    parser.add_argument("--no-excel", action="store_true", help="Skip Excel export")
    parser.add_argument("--no-log",   action="store_true", help="Skip text log file")
    args = parser.parse_args()

    save_to_db = not args.no_save

    # ── Single month ─────────────────────────────────────────────────────────
    if args.month and args.year:
        logger.info("[RUN] %s  month=%d  year=%d", plant_name, args.month, args.year)

        with LogCapture() as cap:
            result = run_month(plant_id, args.month, args.year, save_to_db=save_to_db)

        _print_month_summary(result)

        plant_folder = os.path.join(output_folder, plant_name.replace(" ", "_"))

        if not args.no_excel:
            try:
                path = write_month_report(result, plant_folder)
                logger.info("[EXCEL] %s", path)
            except Exception as e:
                logger.error("[EXCEL] Failed: %s", e)

        if not args.no_log:
            try:
                path = save_text_log(cap.text, plant_name,
                                     args.month, args.year, plant_folder)
                logger.info("[LOG]   %s", path)
            except Exception as e:
                logger.error("[LOG] Failed: %s", e)

        return result

    # ── Full financial year ────────────────────────────────────────────────
    if args.fy:
        logger.info("[RUN] %s  FY %d-%s", plant_name, args.fy, str(args.fy + 1)[-2:])

        with LogCapture() as cap:
            result = run_full_year(plant_id, args.fy, save_to_db=save_to_db)

        _print_full_year_summary(result)

        plant_folder = os.path.join(output_folder, plant_name.replace(" ", "_"))

        if not args.no_excel:
            try:
                path = write_full_year_report(result, plant_folder)
                logger.info("[EXCEL] %s", path)
            except Exception as e:
                logger.error("[EXCEL] Failed: %s", e)

        if not args.no_log:
            try:
                path = save_full_year_log(cap.text, plant_name,
                                          result["fy"], plant_folder)
                logger.info("[LOG]   %s", path)
            except Exception as e:
                logger.error("[LOG] Failed: %s", e)

        return result

    parser.print_help()
    sys.exit(1)


# ---------------------------------------------------------------------------
# Console summary printers
# ---------------------------------------------------------------------------

def _print_month_summary(result: dict):
    sep = "=" * 60
    print(sep)
    print(f"  {result['plant_name']}  {result['month']}/{result['year']}  ({result['fy']})")
    print(sep)
    print(f"  Success:     {result['success']}")
    print(f"  Assets:      {len(result.get('assets', []))}")
    print(f"  Norms:       {len(result.get('norms', []))}")
    print(f"  Import MWh:  {result.get('import_power', {}).get('total_mwh', 0):,.2f}")
    print(f"  Exec time:   {result['execution_time_seconds']}s")
    if not result['success']:
        print(f"  Message:     {result.get('message', '')}")
        for r in result.get('reasons', []):
            print(f"    - {r}")
    print(sep)


def _print_full_year_summary(result: dict):
    sep = "=" * 60
    s = result["summary"]
    print(sep)
    print(f"  {result['plant_name']}  FY {result['fy']}")
    print(sep)
    print(f"  Months OK:   {s['successful']}/12")
    print(f"  Months FAIL: {s['failed']}/12")
    print(f"  Total Norms: {s['total_norms']}")
    for key, m in result["months"].items():
        status = "OK  " if m.get("success") else "FAIL"
        print(f"  [{status}] {key}  assets={len(m.get('assets',[]))}  "
              f"norms={len(m.get('norms',[]))}  {m['execution_time_seconds']}s")
        for r in m.get("reasons", []):
            print(f"           - {r}")
    print(sep)
