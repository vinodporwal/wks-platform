"""
NMD Report Logger — Saves calculation results to text/JSON files.

Writes a structured text report and a machine-readable JSON file
for each month's calculation result.
"""

from __future__ import annotations

import json
import logging
import os
import sys
from datetime import datetime
from io import StringIO
from typing import Dict

logger = logging.getLogger(__name__)

# Default output directory (relative to NMD module root)
_DEFAULT_OUTPUT_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "output")
_DEFAULT_LOG_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "logs")


def _ensure_dir(path: str):
    os.makedirs(path, exist_ok=True)


# ---------------------------------------------------------------------------
# Logging setup + capture (matching JMD report_logger API)
# ---------------------------------------------------------------------------

def setup_logging(level=logging.INFO):
    """Configure root logger for console output with timestamp."""
    logging.basicConfig(
        level=level,
        format="%(asctime)s  %(levelname)-8s  %(message)s",
        datefmt="%H:%M:%S",
        force=True,
    )


class LogCapture:
    """
    Context manager that captures all stdout + logging output to a string.

    Usage:
        with LogCapture() as cap:
            ... do work ...
        print(cap.text)
    """

    def __init__(self):
        self._buf = StringIO()
        self._orig_stdout = None
        self._handler = None
        self.text = ""

    def __enter__(self):
        self._orig_stdout = sys.stdout
        sys.stdout = self
        self._handler = logging.StreamHandler(self._buf)
        self._handler.setFormatter(
            logging.Formatter("%(asctime)s  %(levelname)-8s  %(message)s",
                              datefmt="%H:%M:%S")
        )
        logging.getLogger().addHandler(self._handler)
        return self

    def write(self, text):
        self._orig_stdout.write(text)
        self._buf.write(text)

    def flush(self):
        self._orig_stdout.flush()

    def __exit__(self, *_):
        sys.stdout = self._orig_stdout
        if self._handler:
            logging.getLogger().removeHandler(self._handler)
        self.text = self._buf.getvalue()


def save_text_log(text: str, plant_name: str, month: int, year: int,
                  output_folder: str) -> str:
    """Save a text run log to output_folder. Returns the file path."""
    os.makedirs(output_folder, exist_ok=True)
    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = f"{plant_name.replace(' ', '_')}_{year}_{month:02d}_{ts}.log"
    filepath = os.path.join(output_folder, filename)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(f"NMD Plant Run Log\n")
        f.write(f"Plant:     {plant_name}\n")
        f.write(f"Period:    {month}/{year}\n")
        f.write(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write("=" * 80 + "\n\n")
        f.write(text)
    return filepath


def save_full_year_log(text: str, plant_name: str, fy: str,
                       output_folder: str) -> str:
    """Save a full-year run log."""
    os.makedirs(output_folder, exist_ok=True)
    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = f"{plant_name.replace(' ', '_')}_FY{fy}_{ts}.log"
    filepath = os.path.join(output_folder, filename)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(f"NMD Plant Full-Year Run Log\n")
        f.write(f"Plant:     {plant_name}\n")
        f.write(f"FY:        {fy}\n")
        f.write(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write("=" * 80 + "\n\n")
        f.write(text)
    return filepath


def save_text_report(result: dict, output_dir: str = None) -> str:
    """
    Save a human-readable text report of the calculation result.

    Returns the file path.
    """
    output_dir = output_dir or _DEFAULT_OUTPUT_DIR
    _ensure_dir(output_dir)

    month = result.get("month", 0)
    year = result.get("year", 0)
    plant_name = result.get("plant_name", "NMD")
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

    filename = f"{plant_name}_{year}_{month:02d}_{timestamp}.txt"
    filepath = os.path.join(output_dir, filename)

    lines = []
    sep = "=" * 78

    lines.append(sep)
    lines.append(f"  NMD CALCULATION REPORT")
    lines.append(f"  Plant: {plant_name}  |  Month: {month}/{year}")
    lines.append(f"  Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    lines.append(sep)

    # Execution summary
    lines.append("")
    lines.append(f"  Execution Time:  {result.get('execution_time', 0):.2f} seconds")
    lines.append(f"  Iterations:      {result.get('iterations', 0)}")
    lines.append(f"  Converged:       {'Yes' if result.get('converged') else 'No'}")

    # Power result
    power = result.get("power_result", {})
    if power:
        lines.append("")
        lines.append("  POWER DISPATCH")
        lines.append(f"  Demand:      {power.get('demand_mwh', 0):.2f} MWh")
        lines.append(f"  Generation:  {power.get('total_generation_mwh', 0):.2f} MWh")
        lines.append(f"  Surplus:     {power.get('surplus_mwh', 0):.2f} MWh")
        lines.append(f"  Deficit:     {power.get('deficit_mwh', 0):.2f} MWh")
        lines.append(f"  Free Steam:  {power.get('total_free_steam_mt', 0):.2f} MT")
        lines.append(f"  Aux Power:   {power.get('total_aux_power_mwh', 0):.2f} MWh")

        import_power = power.get("import_power", {})
        if import_power and import_power.get("total_mwh", 0) > 0:
            lines.append(f"  Import Power: {import_power['total_mwh']:.2f} MWh ({import_power.get('source_count', 0)} sources)")

        # Per-asset table
        lines.append("")
        lines.append(f"  {'Asset':<25}  {'MW':>10}  {'MWh':>14}  {'Load%':>8}  {'FreeStm':>10}  {'AuxMWh':>10}")
        lines.append(f"  {'-'*25}  {'-'*10}  {'-'*14}  {'-'*8}  {'-'*10}  {'-'*10}")
        for a in power.get("assets", []):
            lines.append(f"  {a['asset_name']:<25}  {a['dispatched_mw']:>10.2f}  {a['dispatched_mwh']:>14.2f}  {a['load_percent']:>7.1f}%  {a.get('free_steam_mt', 0):>10.2f}  {a.get('aux_power', 0):>10.2f}")

    # Steam result
    steam = result.get("steam_result", {})
    if steam:
        lines.append("")
        lines.append("  STEAM DISPATCH")
        lines.append(f"  Free Steam:           {steam.get('total_free_steam_mt', 0):.2f} MT")
        lines.append(f"  Supplementary Gen:    {steam.get('total_supplementary_generation_mt', 0):.2f} MT")
        lines.append(f"  Total Generation:     {steam.get('total_generation_mt', 0):.2f} MT")
        lines.append(f"  Surplus:              {steam.get('surplus_mt', 0):.2f} MT")
        lines.append(f"  Deficit:              {steam.get('deficit_mt', 0):.2f} MT")

        lines.append("")
        lines.append(f"  {'Asset':<25}  {'TPH':>10}  {'MT':>14}  {'Load%':>8}  {'FreeStm':>10}")
        lines.append(f"  {'-'*25}  {'-'*10}  {'-'*14}  {'-'*8}  {'-'*10}")
        for a in steam.get("assets", []):
            lines.append(f"  {a['asset_name']:<25}  {a['dispatched_tph']:>10.2f}  {a['dispatched_mt']:>14.2f}  {a.get('load_percent', 0):>7.1f}%  {a.get('free_steam_mt', 0):>10.2f}")

    # U4U consumption
    u4u = result.get("u4u_consumption", {})
    if u4u:
        lines.append("")
        lines.append("  U4U CONSUMPTION")
        lines.append(f"  {'Material':<30}  {'Quantity':>14}")
        lines.append(f"  {'-'*30}  {'-'*14}")
        for material, qty in sorted(u4u.items()):
            lines.append(f"  {material:<30}  {qty:>14.2f}")

    # Iteration history
    history = result.get("history", [])
    if history:
        lines.append("")
        lines.append("  ITERATION HISTORY")
        lines.append(f"  {'Iter':>6}  {'Power Demand':>14}  {'Power Gen':>14}  {'Steam Gen':>14}  {'Delta':>10}")
        lines.append(f"  {'-'*6}  {'-'*14}  {'-'*14}  {'-'*14}  {'-'*10}")
        for h in history:
            lines.append(f"  {h['iteration']:>6}  {h['power_demand_mwh']:>14.2f}  {h['power_gen_mwh']:>14.2f}  {h['steam_gen_mt']:>14.2f}  {h['delta_mwh']:>10.2f}")

    lines.append("")
    lines.append(sep)

    with open(filepath, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

    logger.info("  Report saved: %s", filepath)
    return filepath


def save_json_result(result: dict, output_dir: str = None) -> str:
    """
    Save the full calculation result as a JSON file.

    Returns the file path.
    """
    output_dir = output_dir or _DEFAULT_OUTPUT_DIR
    _ensure_dir(output_dir)

    month = result.get("month", 0)
    year = result.get("year", 0)
    plant_name = result.get("plant_name", "NMD")
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

    filename = f"{plant_name}_{year}_{month:02d}_{timestamp}.json"
    filepath = os.path.join(output_dir, filename)

    with open(filepath, "w", encoding="utf-8") as f:
        json.dump(result, f, indent=2, default=str)

    logger.info("  JSON saved: %s", filepath)
    return filepath


def save_result(result: dict, output_dir: str = None) -> dict:
    """
    Save both text report and JSON result.

    Returns:
        {"text_path": str, "json_path": str}
    """
    text_path = save_text_report(result, output_dir)
    json_path = save_json_result(result, output_dir)
    return {"text_path": text_path, "json_path": json_path}
