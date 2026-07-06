"""
NMD Calculator — Month orchestrator.

Coordinates the full calculation pipeline for a single month:
  1. Fetch process demands and fixed consumption from DB.
  2. Load norms reader (cached per month/year).
  3. Run U4U iteration loop (power dispatch → steam dispatch → U4U update).
  4. Collect results for reporting.
"""

from __future__ import annotations

import logging
import time
from typing import Dict, Optional

from database.queries import fetch_process_demands, fetch_fixed_consumption
from engine.norms_reader import NMDNormsReader
from engine.u4u_iteration_loop import run_u4u_iteration
from plant_mapper import get_plant_by_id

logger = logging.getLogger(__name__)


def run_month(
    plant_id: str,
    month: int,
    year: int,
    norms_reader: NMDNormsReader = None,
) -> dict:
    """
    Run the full NMD calculation for a single plant/month.

    Args:
        plant_id:       NMD plant UUID
        month:          1-12
        year:           calendar year
        norms_reader:   pre-loaded NMDNormsReader (cached automatically if None)

    Returns:
        {
            "plant_id":       str,
            "plant_name":     str,
            "month":          int,
            "year":           int,
            "execution_time": float,
            "iterations":     int,
            "converged":      bool,
            "power_result":   dict,
            "steam_result":   dict,
            "demands":        dict,
            "u4u_consumption": dict,
            "process_demands": dict,
            "fixed_consumption": dict,
            "history":        list,
            "norms_summary":  dict,
        }
    """
    start_time = time.time()

    plant = get_plant_by_id(plant_id)
    plant_name = plant.get("name", "Unknown") if plant else "Unknown"

    logger.info("")
    logger.info("=" * 78)
    logger.info("  NMD CALCULATION  —  %s  —  %d/%d", plant_name, month, year)
    logger.info("=" * 78)

    # 1. Load norms reader
    if norms_reader is None:
        norms_reader = NMDNormsReader.get_reader(month, year)
    norms_reader.print_summary()

    # 2. Fetch process demands
    logger.info("")
    logger.info("  Fetching process demands...")
    process_demands = fetch_process_demands(plant_id, month, year)
    for key, val in sorted(process_demands.items()):
        if val > 0:
            logger.info("    %-30s  %14.2f", key, val)

    # 3. Fetch fixed consumption
    logger.info("")
    logger.info("  Fetching fixed consumption...")
    fixed_consumption = fetch_fixed_consumption(plant_id, month, year)
    for key, val in sorted(fixed_consumption.items()):
        if val > 0:
            logger.info("    %-30s  %14.2f", key, val)

    # 4. Run U4U iteration loop
    logger.info("")
    logger.info("  Starting U4U iteration loop...")
    result = run_u4u_iteration(
        plant_id, month, year,
        process_demands, fixed_consumption,
        norms_reader=norms_reader,
    )

    elapsed = time.time() - start_time

    logger.info("")
    logger.info("  CALCULATION COMPLETE  —  %.2f seconds  —  %d iterations  —  %s",
                elapsed, result["iterations"],
                "CONVERGED" if result["converged"] else "MAX ITERATIONS")
    logger.info("=" * 78)

    return {
        "plant_id":         plant_id,
        "plant_name":       plant_name,
        "month":            month,
        "year":             year,
        "execution_time":   round(elapsed, 2),
        "iterations":       result["iterations"],
        "converged":        result["converged"],
        "power_result":     result["power_result"],
        "steam_result":     result["steam_result"],
        "demands":          result["demands"],
        "u4u_consumption":  result["u4u_consumption"],
        "process_demands":  process_demands,
        "fixed_consumption": fixed_consumption,
        "history":          result["history"],
        "norms_summary": {
            "generation_utilities": list(norms_reader.get_generation_utilities()),
            "u4u_materials":        list(norms_reader.get_u4u_materials()),
            "powergen_norms":       norms_reader.get_powergen_norms(),
            "steam_letdown_norms":  norms_reader.get_steam_letdown_norms(),
            "hrsg_byproduct_norms": norms_reader.get_hrsg_byproduct_norms(),
            "stg_steam_norm":       norms_reader.get_stg_steam_norm(),
        },
    }


def run_year(
    plant_id: str,
    year: int,
    start_month: int = 4,
    end_month: int = 3,
    next_year_end: bool = True,
) -> list:
    """
    Run the calculation for all 12 months of a financial year.

    Args:
        plant_id:       NMD plant UUID
        year:           FY start year (e.g. 2026 for FY 2026-27)
        start_month:    First month of FY (default April = 4)
        end_month:      Last month of FY (default March = 3)
        next_year_end:  If True, end_month is in year+1 (FY spans two calendar years)

    Returns:
        List of run_month() result dicts, one per month.
    """
    results = []
    months_order = list(range(start_month, 13)) + list(range(1, end_month + 1))

    for m in months_order:
        calc_year = year if m >= 4 else year + 1
        logger.info("")
        logger.info("#" * 78)
        logger.info("#  Running month %d/%d", m, calc_year)
        logger.info("#" * 78)

        try:
            result = run_month(plant_id, m, calc_year)
            results.append(result)
        except Exception as e:
            logger.error("  FAILED for %d/%d: %s", m, calc_year, e)
            results.append({
                "plant_id": plant_id,
                "month": m,
                "year": calc_year,
                "error": str(e),
            })

    return results
