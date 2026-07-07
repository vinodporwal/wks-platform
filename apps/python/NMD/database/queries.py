"""
NMD Database Queries — All DB fetch functions for the NMD module.

Uses NMD-specific schema:
  - PowerGenerationAssets (filtered by AssetType IN ('GT', 'STG'))
  - OperationalHours (one row per asset per month via FinancialMonthId)
  - AssetAvailability (Priority + Min/Max capacity, one row per asset per month)
  - SteamGenerationAssets (inline MinCapacityMT/MaxCapacityMT + LinkedPowerAssetId)
  - UtilityFixedConsumption (FinancialYearMonth-based)
  - CPP_NMD_GetProcessDemandByYear stored procedure for process demands
  - CPPImportPowerSourceMapping / CPPImportPowerCapacity / CPPImportPowerOperationalHours
  - NormsMonthDetail / NormsHeader for norms
  - CPP_GTHeatRate for GT heat rate curves
"""

from __future__ import annotations

import logging
from typing import Dict, List, Optional

from database.connection import get_connection
from database.tables import T
from plant_mapper import NMD_PLANT_ID

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Month helpers
# ---------------------------------------------------------------------------

_MONTH_COL = {
    1: "jan", 2: "feb", 3: "mar", 4: "apr", 5: "may", 6: "jun",
    7: "jul", 8: "aug", 9: "sep", 10: "oct", 11: "nov", 12: "dec",
}

_MONTH_COL_TITLE = {
    1: "Jan", 2: "Feb", 3: "Mar", 4: "Apr", 5: "May", 6: "Jun",
    7: "Jul", 8: "Aug", 9: "Sep", 10: "Oct", 11: "Nov", 12: "Dec",
}


def _fy_string(month: int, year: int) -> str:
    """Return financial year string like '2025-26'."""
    fy_start = year if month >= 4 else year - 1
    return f"{fy_start}-{str(fy_start + 1)[-2:]}"


def _fy_year_short(month: int, year: int) -> str:
    """Return just the start-year string, e.g. '2025'."""
    return str(year if month >= 4 else year - 1)


def _normalize_utility_name(name: str) -> str:
    return "".join(ch.lower() for ch in str(name) if ch.isalnum())


# ---------------------------------------------------------------------------
# Utility name mappings (same as existing NMD)
# ---------------------------------------------------------------------------

UTILITY_MAPPING = {
    "LP Steam":       "lp_process",
    "LP Steam_Dis":   "lp_process",
    "MP Steam":       "mp_process",
    "MP Steam_Dis":   "mp_process",
    "HP Steam":       "hp_process",
    "HP Steam_Dis":   "hp_process",
    "SHP Steam":      "shp_process",
    "SHP Steam_Dis":  "shp_process",
    "Power":          "power_process",
    "Power_Dis":      "power_process",
    "COMPRESSED AIR": "air_process",
    "Compressed Air": "air_process",
    "NITROGEN_ASU":   "nitrogen_asu_process",
    "D M Water":      "dm_process",
    "DM Water":       "dm_process",
    "Cooling Water 1":"cw1_process",
    "CW1":            "cw1_process",
    "Cooling Water 2":"cw2_process",
    "CW2":            "cw2_process",
    "Cooling Water":  "cooling_water_process",
    "Water":          "raw_water_process",
    "Raw Water":      "raw_water_process",
    "Utility Water":  "utility_water_process",
    "Ret steam condensate": "ret_steam_condensate_process",
    "Oxygen":         "oxygen_process",
    "Effluent Treated":"effluent_process",
    "Effluent":       "effluent_process",
    "PROCESS FEED WATER": "process_feed_water_process",
}

_FIXED_UTILITY_MAP = {
    "power": "power_fixed",
    "powerdis": "power_fixed",
    "lpsteam": "lp_fixed",
    "lpsteamdis": "lp_fixed",
    "mpsteam": "mp_fixed",
    "mpsteamdis": "mp_fixed",
    "hpsteam": "hp_fixed",
    "hpsteamdis": "hp_fixed",
    "shpsteam": "shp_fixed",
    "shpsteamdis": "shp_fixed",
    "compressedair": "air_fixed",
    "air": "air_fixed",
    "nitrogenasu": "nitrogen_asu_fixed",
    "dmwater": "dm_fixed",
    "dm": "dm_fixed",
    "coolingwater1": "cw1_fixed",
    "cw1": "cw1_fixed",
    "coolingwater2": "cw2_fixed",
    "cw2": "cw2_fixed",
    "coolingwater": "cooling_water_fixed",
    "rawwater": "raw_water_fixed",
    "water": "raw_water_fixed",
    "utilitywater": "utility_water_fixed",
    "retsteamcondensate": "ret_steam_condensate_fixed",
    "oxygen": "oxygen_fixed",
    "effluenttreated": "effluent_fixed",
    "effluent": "effluent_fixed",
}

_FIXED_RESULT_KEYS = (
    "power_fixed_kwh", "power_fixed",
    "lp_fixed", "mp_fixed", "hp_fixed", "shp_fixed",
    "air_fixed", "nitrogen_asu_fixed", "dm_fixed",
    "cw1_fixed", "cw2_fixed", "cooling_water_fixed",
    "raw_water_fixed", "utility_water_fixed",
    "ret_steam_condensate_fixed",
    "oxygen_fixed", "effluent_fixed",
)


def _empty_fixed_result() -> dict:
    return {key: 0.0 for key in _FIXED_RESULT_KEYS}


# ---------------------------------------------------------------------------
# 1. FinancialYearMonth ID lookup
# ---------------------------------------------------------------------------

def fetch_financial_year_month_id(month: int, year: int) -> str | None:
    """Return the FinancialYearMonth.Id (UUID) for the given month/year."""
    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            f"SELECT Id FROM {T.FINANCIAL_YEAR_MONTH} WHERE [Month] = ? AND [Year] = ?",
            (month, year),
        )
        row = cur.fetchone()
        return str(row[0]) if row else None
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# 2. Process Demands (via CPP_NMD_GetProcessDemandByYear stored procedure)
# ---------------------------------------------------------------------------

def fetch_process_demands(plant_id: str, month: int, year: int) -> dict:
    """
    Fetch aggregated process utility demands for a plant/month.

    Uses CPP_NMD_GetProcessDemandByYear stored procedure which joins
    ProcessDemandMaster to CalculatedProcessDemand.

    Returns dict with keys like 'power_process', 'lp_process', etc.
    """
    fy = _fy_string(month, year)
    col = _MONTH_COL.get(month)
    if not col:
        raise ValueError(f"Invalid month: {month}")

    result = {k: 0.0 for k in (
        "power_process", "lp_process", "mp_process", "hp_process",
        "shp_process", "air_process", "nitrogen_asu_process", "dm_process",
        "cw1_process", "cw2_process", "cooling_water_process",
        "raw_water_process", "utility_water_process",
        "ret_steam_condensate_process", "oxygen_process", "effluent_process",
        "process_feed_water_process",
    )}

    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            f"EXEC dbo.{T.PROCESS_DEMAND_SP} ?, ?",
            (fy, plant_id),
        )
        columns = [col[0].lower() for col in (cur.description or [])]
        if "cpp_utility" not in columns or col not in columns:
            logger.warning("  [PROCESS] SP output missing expected columns; returning zeros")
            return result

        utility_idx = columns.index("cpp_utility")
        month_idx = columns.index(col)
        rows = cur.fetchall()

        logger.info("  [PROCESS] Found %d rows for FY %s, plant %s", len(rows), fy, plant_id)
        for row in rows:
            util_name = row[utility_idx] or ""
            value = float(row[month_idx]) if row[month_idx] is not None else 0.0
            key = UTILITY_MAPPING.get(util_name)
            if key and key in result:
                result[key] += value
                logger.info("  [PROCESS] %-24s  %14.2f  ->  %s", util_name, value, key)
            else:
                logger.info("  [PROCESS] %-24s  %14.2f  ->  UNMAPPED", util_name, value)

        return result
    except Exception as e:
        logger.error("  [PROCESS] Error: %s", e)
        return result
    finally:
        conn.close()


def fetch_process_demands_raw(plant_id: str, month: int, year: int) -> dict:
    """
    Fetch process utility demands keyed by the raw DB utility name.

    Returns: {"LP Steam_Dis": 18336.0, "Power_Dis": 62526.31, ...}
    """
    fy = _fy_string(month, year)
    col = _MONTH_COL.get(month)
    if not col:
        raise ValueError(f"Invalid month: {month}")

    result: dict = {}
    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            f"EXEC dbo.{T.PROCESS_DEMAND_SP} ?, ?",
            (fy, plant_id),
        )
        columns = [col[0].lower() for col in (cur.description or [])]
        if "cpp_utility" not in columns or col not in columns:
            logger.warning("  [PROCESS_RAW] SP output missing expected columns")
            return result

        utility_idx = columns.index("cpp_utility")
        month_idx = columns.index(col)
        for row in cur.fetchall():
            util_name = (row[utility_idx] or "").strip()
            value = float(row[month_idx]) if row[month_idx] is not None else 0.0
            if util_name:
                result[util_name] = result.get(util_name, 0.0) + value
        return result
    except Exception as e:
        logger.error("  [PROCESS_RAW] Error: %s", e)
        return result
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# 3. Fixed Consumption (UtilityFixedConsumption table)
# ---------------------------------------------------------------------------

def fetch_fixed_consumption(plant_id: str, month: int, year: int) -> dict:
    """
    Fetch fixed consumption for a plant/month, rolled up by utility.

    Uses UtilityFixedConsumption table joined with NormParameters,
    CPPCostCenters, and FixedConsumptionPlantMapping.

    Returns a flat dict keyed by utility bucket (power_fixed, lp_fixed, etc.).
    Power values are converted from kWh to MWh.
    """
    result = _empty_fixed_result()

    fym_id = fetch_financial_year_month_id(month, year)
    if not fym_id:
        logger.warning("  [FIXED] FinancialYearMonth not found for %d/%d", month, year)
        return result

    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT
                np.Name AS UtilityName,
                np.UOM,
                SUM(ufc.{T.UFC_VALUE_COL}) AS TotalConsumption
            FROM {T.UTILITY_FIXED_CONSUMPTION} ufc
            JOIN {T.NORM_PARAMETERS} np ON ufc.{T.UFC_NORM_PARAM_FK} = np.Id
            JOIN {T.CPP_COST_CENTERS} cc ON ufc.{T.UFC_COST_CENTER_FK} = cc.CostCenterId
            JOIN {T.FIXED_CONSUMPTION_PLANT_MAP} pm ON cc.Plant_FK_Id = pm.Id
            WHERE ufc.{T.UFC_FYM_FK} = ?
            GROUP BY np.Name, np.UOM
            """,
            (fym_id,),
        )
        rows = cur.fetchall()

        logger.info("  [FIXED] %d utility rows for FYM %s", len(rows), fym_id)
        for row in rows:
            utility_name = row[0] or ""
            uom = (row[1] or "").upper()
            value_raw = float(row[2]) if row[2] is not None else 0.0
            mapped_key = _FIXED_UTILITY_MAP.get(_normalize_utility_name(utility_name))
            value = value_raw / 1000.0 if mapped_key == "power_fixed" else value_raw

            if mapped_key and mapped_key in result:
                if mapped_key == "power_fixed":
                    result["power_fixed_kwh"] += value_raw
                result[mapped_key] += value
                logger.info("  [FIXED] %-24s  %14.2f  ->  %s", utility_name, value, mapped_key)
            else:
                logger.info("  [FIXED] %-24s  %14.2f  ->  UNMAPPED", utility_name, value)

        return result
    except Exception as e:
        logger.error("  [FIXED] Error: %s", e)
        return result
    finally:
        conn.close()


def fetch_fixed_consumption_raw(plant_id: str, month: int, year: int) -> dict:
    """
    Fetch fixed consumption keyed by the raw DB utility name (NormParameters.Name).

    Power values are converted from kWh to MWh.
    """
    fym_id = fetch_financial_year_month_id(month, year)
    if not fym_id:
        return {}

    result: dict = {}
    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT
                np.Name AS UtilityName,
                np.UOM,
                SUM(ufc.{T.UFC_VALUE_COL}) AS TotalConsumption
            FROM {T.UTILITY_FIXED_CONSUMPTION} ufc
            JOIN {T.NORM_PARAMETERS} np ON ufc.{T.UFC_NORM_PARAM_FK} = np.Id
            WHERE ufc.{T.UFC_FYM_FK} = ?
            GROUP BY np.Name, np.UOM
            """,
            (fym_id,),
        )
        for row in cur.fetchall():
            util_name = (row[0] or "").strip()
            uom = (row[1] or "").strip().upper()
            value = float(row[2]) if row[2] is not None else 0.0
            if not util_name:
                continue
            if uom == "KWH":
                value = value / 1000.0
            result[util_name] = result.get(util_name, 0.0) + value
        return result
    except Exception as e:
        logger.error("  [FIXED_RAW] Error: %s", e)
        return result
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# 3b. Consolidated Demand (Process + Fixed) with UOM
# ---------------------------------------------------------------------------

def fetch_consolidated_demand_with_uom(plant_id: str, month: int, year: int) -> list:
    """
    Fetch consolidated process + fixed demand for all utilities with original UOM.

    Returns a list of dicts sorted by utility name:
        [
            {
                "utility_name": "Power_Dis",
                "uom": "KWH",
                "process_value": 26366549.0,
                "fixed_value": 1605.14,      # in original UOM (KWH for power)
                "total_value": 26368154.14,
            },
            ...
        ]

    Process demand comes from CPP_NMD_GetProcessDemandByYear SP (raw utility names).
    Fixed demand comes from UtilityFixedConsumption joined to NormParameters (Name + UOM).
    Both are keyed by the raw DB utility name (e.g. "Power_Dis", "LP Steam_Dis").
    """
    # ── Fetch process demands (raw utility names) ────────────────────────
    process_raw = fetch_process_demands_raw(plant_id, month, year)

    # ── Fetch fixed demands (raw utility names + UOM) ─────────────────────
    fym_id = fetch_financial_year_month_id(month, year)
    fixed_raw: dict = {}
    uom_map: dict = {}

    if fym_id:
        conn = get_connection()
        cur = conn.cursor()
        try:
            cur.execute(
                f"""
                SELECT
                    np.Name AS UtilityName,
                    np.UOM,
                    SUM(ufc.{T.UFC_VALUE_COL}) AS TotalConsumption
                FROM {T.UTILITY_FIXED_CONSUMPTION} ufc
                JOIN {T.NORM_PARAMETERS} np ON ufc.{T.UFC_NORM_PARAM_FK} = np.Id
                WHERE ufc.{T.UFC_FYM_FK} = ?
                GROUP BY np.Name, np.UOM
                """,
                (fym_id,),
            )
            for row in cur.fetchall():
                util_name = (row[0] or "").strip()
                uom = (row[1] or "").strip().upper()
                value = float(row[2]) if row[2] is not None else 0.0
                if not util_name:
                    continue
                fixed_raw[util_name] = fixed_raw.get(util_name, 0.0) + value
                uom_map[util_name] = uom
        except Exception as e:
            logger.error("  [CONSOLIDATED] Fixed demand error: %s", e)
        finally:
            conn.close()

    # ── Fetch UOM for process demand utilities from NormParameters ────────
    # Process SP doesn't return UOM, so look it up
    process_uom_needed = [u for u in process_raw if u not in uom_map]
    if process_uom_needed:
        conn = get_connection()
        cur = conn.cursor()
        try:
            placeholders = ",".join("?" * len(process_uom_needed))
            cur.execute(
                f"""
                SELECT DISTINCT Name, UOM FROM {T.NORM_PARAMETERS}
                WHERE Name IN ({placeholders})
                """,
                process_uom_needed,
            )
            for row in cur.fetchall():
                util_name = (row[0] or "").strip()
                uom = (row[1] or "").strip().upper()
                if util_name and uom:
                    uom_map[util_name] = uom
        except Exception as e:
            logger.error("  [CONSOLIDATED] UOM lookup error: %s", e)
        finally:
            conn.close()

    # ── Merge process + fixed by utility name ─────────────────────────────
    all_utilities = sorted(set(list(process_raw.keys()) + list(fixed_raw.keys())))

    results = []
    for util_name in all_utilities:
        uom = uom_map.get(util_name, "")
        pval = process_raw.get(util_name, 0.0)
        fval = fixed_raw.get(util_name, 0.0)
        results.append({
            "utility_name": util_name,
            "uom": uom,
            "process_value": pval,
            "fixed_value": fval,
            "total_value": pval + fval,
        })

    logger.info("  [CONSOLIDATED] %d utilities (process + fixed) for %d/%d",
                len(results), month, year)
    return results


# ---------------------------------------------------------------------------
# 4. Power Generation Assets
# ---------------------------------------------------------------------------

def fetch_power_generation_assets(plant_id: str) -> list:
    """
    Fetch power generation assets (GT + STG) for a specific CPP plant.

    Filters by CPPPLANT_FK_Id = plant_id AND AssetType IN ('GT', 'STG').

    Returns:
        List of dicts with asset_id, asset_name, asset_type, plant_code, cpp_plant_fk_id.
    """
    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT {T.PGA_ASSET_ID}, {T.PGA_ASSET_NAME}, {T.PGA_ASSET_TYPE},
                   {T.PGA_PLANT_CODE}, {T.PGA_CPP_PLANT_FK}
            FROM {T.POWER_GENERATION_ASSETS}
            WHERE {T.PGA_ASSET_TYPE} IN ('GT', 'STG')
              AND {T.PGA_CPP_PLANT_FK} = ?
            ORDER BY {T.PGA_ASSET_NAME}
            """,
            (plant_id,),
        )
        results = []
        for row in cur.fetchall():
            results.append({
                "asset_id":       str(row[0]) if row[0] else None,
                "asset_name":     row[1],
                "asset_type":     row[2] or "",
                "plant_code":     row[3] or "",
                "cpp_plant_fk_id": str(row[4]) if row[4] else None,
            })
        return results
    except Exception as e:
        logger.error("  [POWER ASSETS] Error: %s", e)
        return []
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# 5. Operational Hours (one row per asset per month)
# ---------------------------------------------------------------------------

def fetch_operational_hours(month: int, year: int, plant_id: str = None) -> dict:
    """
    Fetch operational hours per power asset for a month.

    Uses OperationalHours table joined to PowerGenerationAssets via FinancialMonthId.

    Returns:
        { asset_name: hours_float, ... }
        Hours = 0 means asset not available that month.
    """
    fym_id = fetch_financial_year_month_id(month, year)
    if not fym_id:
        logger.warning("  [OPS HRS] FinancialYearMonth not found for %d/%d", month, year)
        return {}

    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT a.{T.PGA_ASSET_NAME}, COALESCE(oh.{T.OH_HOURS_COL}, 0)
            FROM {T.OPERATIONAL_HOURS} oh
            JOIN {T.POWER_GENERATION_ASSETS} a ON a.{T.PGA_ASSET_ID} = oh.{T.OH_ASSET_FK}
            WHERE oh.{T.OH_FYM_FK} = ?
              AND a.{T.PGA_ASSET_TYPE} IN ('GT', 'STG')
              AND a.{T.PGA_CPP_PLANT_FK} = ?
            ORDER BY a.{T.PGA_ASSET_NAME}
            """,
            (fym_id, plant_id),
        )
        return {row[0]: float(row[1]) if row[1] is not None else 0.0
                for row in cur.fetchall()}
    except Exception as e:
        logger.error("  [OPS HRS] Error: %s", e)
        return {}
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# 6. Asset Availability (Priority + Min/Max capacity)
# ---------------------------------------------------------------------------

def fetch_asset_availability(month: int, year: int, plant_id: str = None) -> list:
    """
    Fetch asset availability (priority, min/max capacity) for all power assets.

    Uses AssetAvailability table joined to PowerGenerationAssets via FinancialYearMonthId.
    Availability is determined from OperationalHours, not from AssetAvailability.isAssetAvailable.

    Returns:
        List of dicts:
        {
            "asset_id": str,
            "asset_name": str,
            "asset_capacity": float (from MaxOperatingCapacity),
            "plant_code": str,
            "priority": int or None,
            "min_operating_capacity": float or None,
            "max_operating_capacity": float or None,
            "fixed_min": float or None,
            "fixed_max": float or None,
        }
    """
    fym_id = fetch_financial_year_month_id(month, year)
    if not fym_id:
        return []

    conn = get_connection()
    cur = conn.cursor()
    try:
        query = f"""
            SELECT
                p.{T.PGA_ASSET_ID},
                p.{T.PGA_ASSET_NAME},
                COALESCE(aa.{T.AA_MAX_OP_CAPACITY}, 0) AS AssetCapacity,
                p.{T.PGA_PLANT_CODE},
                aa.{T.AA_PRIORITY},
                aa.{T.AA_MIN_OP_CAPACITY},
                aa.{T.AA_MAX_OP_CAPACITY},
                aa.{T.AA_FIXED_MIN},
                aa.{T.AA_FIXED_MAX}
            FROM {T.POWER_GENERATION_ASSETS} p
            LEFT JOIN {T.ASSET_AVAILABILITY} aa
                ON p.{T.PGA_ASSET_ID} = aa.{T.AA_ASSET_ID}
                AND aa.{T.AA_FYM_FK} = ?
            WHERE p.{T.PGA_ASSET_TYPE} IN ('GT', 'STG')
            """
        params = [fym_id]
        if plant_id:
            query += f"              AND p.{T.PGA_CPP_PLANT_FK} = ?\n"
            params.append(plant_id)
        query += f"            ORDER BY aa.{T.AA_PRIORITY}, p.{T.PGA_ASSET_NAME}"
        cur.execute(query, tuple(params))
        results = []
        for row in cur.fetchall():
            results.append({
                "asset_id":              str(row[0]) if row[0] else None,
                "asset_name":            row[1],
                "asset_capacity":        float(row[2]) if row[2] else 0.0,
                "plant_code":            row[3] or "",
                "priority":              row[4],
                "min_operating_capacity": float(row[5]) if row[5] is not None else None,
                "max_operating_capacity": float(row[6]) if row[6] is not None else None,
                "fixed_min":             float(row[7]) if row[7] is not None else None,
                "fixed_max":             float(row[8]) if row[8] is not None else None,
            })
        return results
    except Exception as e:
        logger.error("  [AVAIL] Error: %s", e)
        return []
    finally:
        conn.close()


def fetch_asset_availability_with_hours(month: int, year: int, plant_id: str = None) -> list:
    """
    Fetch asset availability + operational hours combined for power assets.

    Returns:
        List of dicts with all fields from fetch_asset_availability plus:
        {
            "operational_hours": float,
            "is_available": bool (True if operational_hours > 0),
        }
    """
    fym_id = fetch_financial_year_month_id(month, year)
    if not fym_id:
        return []

    conn = get_connection()
    cur = conn.cursor()
    try:
        query = f"""
            SELECT
                p.{T.PGA_ASSET_ID},
                p.{T.PGA_ASSET_NAME},
                COALESCE(aa.{T.AA_MAX_OP_CAPACITY}, 0) AS AssetCapacity,
                p.{T.PGA_PLANT_CODE},
                aa.{T.AA_PRIORITY},
                aa.{T.AA_MIN_OP_CAPACITY},
                aa.{T.AA_MAX_OP_CAPACITY},
                aa.{T.AA_FIXED_MIN},
                aa.{T.AA_FIXED_MAX},
                COALESCE(oh.{T.OH_HOURS_COL}, 0) AS OperationalHours
            FROM {T.POWER_GENERATION_ASSETS} p
            LEFT JOIN {T.ASSET_AVAILABILITY} aa
                ON p.{T.PGA_ASSET_ID} = aa.{T.AA_ASSET_ID}
                AND aa.{T.AA_FYM_FK} = ?
            LEFT JOIN {T.OPERATIONAL_HOURS} oh
                ON p.{T.PGA_ASSET_ID} = oh.{T.OH_ASSET_FK}
                AND oh.{T.OH_FYM_FK} = ?
            WHERE p.{T.PGA_ASSET_TYPE} IN ('GT', 'STG')
            """
        params = [fym_id, fym_id]
        if plant_id:
            query += f"              AND p.{T.PGA_CPP_PLANT_FK} = ?\n"
            params.append(plant_id)
        query += f"            ORDER BY aa.{T.AA_PRIORITY}, p.{T.PGA_ASSET_NAME}"
        cur.execute(query, tuple(params))
        results = []
        for row in cur.fetchall():
            op_hours = float(row[9]) if row[9] else 0.0
            results.append({
                "asset_id":              str(row[0]) if row[0] else None,
                "asset_name":            row[1],
                "asset_capacity":        float(row[2]) if row[2] else 0.0,
                "plant_code":            row[3] or "",
                "priority":              row[4],
                "min_operating_capacity": float(row[5]) if row[5] is not None else None,
                "max_operating_capacity": float(row[6]) if row[6] is not None else None,
                "fixed_min":             float(row[7]) if row[7] is not None else None,
                "fixed_max":             float(row[8]) if row[8] is not None else None,
                "operational_hours":     op_hours,
                "is_available":          op_hours > 0,
            })
        return results
    except Exception as e:
        logger.error("  [AVAIL+HRS] Error: %s", e)
        return []
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# 7. Steam Generation Assets (HRSGs with inline capacity + GT linkage)
# ---------------------------------------------------------------------------

def fetch_steam_generation_assets(plant_id: str = None) -> list:
    """
    Fetch steam generation assets (HRSG, STG, PRDS) from SteamGenerationAssets.

    If plant_id is provided, filters HRSGs by the linked power asset's CPPPLANT_FK_Id.
    STG/PRDS assets (with no LinkedPowerAssetId) are always included.

    Returns:
        List of dicts with asset_id, asset_name, asset_type, min/max capacity,
        steam_type, linked_power_asset_id, linked_gt_name, priority, efficiency,
        is_always_available.
    """
    conn = get_connection()
    cur = conn.cursor()
    try:
        if plant_id:
            query = f"""
                SELECT
                    s.{T.SGA_ASSET_ID},
                    s.{T.SGA_ASSET_NAME},
                    s.{T.SGA_ASSET_TYPE},
                    s.{T.SGA_MIN_CAPACITY_MT},
                    s.{T.SGA_MAX_CAPACITY_MT},
                    s.{T.SGA_STEAM_TYPE},
                    s.{T.SGA_LINKED_POWER_ASSET_ID},
                    p.{T.PGA_ASSET_NAME} AS LinkedGTName,
                    s.{T.SGA_PRIORITY},
                    s.{T.SGA_EFFICIENCY},
                    s.{T.SGA_IS_ALWAYS_AVAILABLE}
                FROM {T.STEAM_GENERATION_ASSETS} s
                LEFT JOIN {T.POWER_GENERATION_ASSETS} p
                    ON s.{T.SGA_LINKED_POWER_ASSET_ID} = p.{T.PGA_ASSET_ID}
                WHERE (s.{T.SGA_LINKED_POWER_ASSET_ID} IS NULL
                       OR p.{T.PGA_CPP_PLANT_FK} = ?)
                ORDER BY s.{T.SGA_PRIORITY}, s.{T.SGA_ASSET_NAME}
                """
            cur.execute(query, (plant_id,))
        else:
            query = f"""
                SELECT
                    s.{T.SGA_ASSET_ID},
                    s.{T.SGA_ASSET_NAME},
                    s.{T.SGA_ASSET_TYPE},
                    s.{T.SGA_MIN_CAPACITY_MT},
                    s.{T.SGA_MAX_CAPACITY_MT},
                    s.{T.SGA_STEAM_TYPE},
                    s.{T.SGA_LINKED_POWER_ASSET_ID},
                    p.{T.PGA_ASSET_NAME} AS LinkedGTName,
                    s.{T.SGA_PRIORITY},
                    s.{T.SGA_EFFICIENCY},
                    s.{T.SGA_IS_ALWAYS_AVAILABLE}
                FROM {T.STEAM_GENERATION_ASSETS} s
                LEFT JOIN {T.POWER_GENERATION_ASSETS} p
                    ON s.{T.SGA_LINKED_POWER_ASSET_ID} = p.{T.PGA_ASSET_ID}
                ORDER BY s.{T.SGA_PRIORITY}, s.{T.SGA_ASSET_NAME}
                """
            cur.execute(query)
        results = []
        for row in cur.fetchall():
            results.append({
                "asset_id":              str(row[0]) if row[0] else None,
                "asset_name":            row[1],
                "asset_type":            row[2] or "",
                "min_capacity_mt":       float(row[3]) if row[3] else 0.0,
                "max_capacity_mt":       float(row[4]) if row[4] else 0.0,
                "steam_type":            row[5] or "SHP",
                "linked_power_asset_id": str(row[6]) if row[6] else None,
                "linked_gt_name":        row[7] or None,
                "priority":              row[8] if row[8] is not None else 999,
                "efficiency":            float(row[9]) if row[9] else 1.0,
                "is_always_available":   bool(row[10]) if row[10] is not None else False,
            })
        return results
    except Exception as e:
        logger.error("  [STEAM ASSETS] Error: %s", e)
        return []
    finally:
        conn.close()


def fetch_steam_asset_operational_hours(plant_id: str, month: int, year: int) -> dict:
    """
    Fetch operational hours for steam assets for a specific month.

    For HRSGs: uses the linked power asset's operational hours.
    For STG/PRDS (IsAlwaysAvailable=1): returns 720 (default monthly hours).

    Returns:
        {asset_name: hours_float, ...}
    """
    fym_id = fetch_financial_year_month_id(month, year)
    if not fym_id:
        return {}

    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT
                s.{T.SGA_ASSET_NAME},
                CASE
                    WHEN s.{T.SGA_IS_ALWAYS_AVAILABLE} = 1 THEN 720
                    ELSE COALESCE(oh.{T.OH_HOURS_COL}, 0)
                END AS OperationalHours
            FROM {T.STEAM_GENERATION_ASSETS} s
            LEFT JOIN {T.OPERATIONAL_HOURS} oh
                ON s.{T.SGA_LINKED_POWER_ASSET_ID} = oh.{T.OH_ASSET_FK}
                AND oh.{T.OH_FYM_FK} = ?
            LEFT JOIN {T.POWER_GENERATION_ASSETS} p
                ON s.{T.SGA_LINKED_POWER_ASSET_ID} = p.{T.PGA_ASSET_ID}
            WHERE s.{T.SGA_LINKED_POWER_ASSET_ID} IS NULL
                  OR p.{T.PGA_CPP_PLANT_FK} = ?
            ORDER BY s.{T.SGA_ASSET_NAME}
            """,
            (fym_id, plant_id),
        )
        return {row[0]: float(row[1]) if row[1] is not None else 0.0
                for row in cur.fetchall()}
    except Exception as e:
        logger.error("  [STEAM OPS HRS] Error: %s", e)
        return {}
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# 8. Import Power (multi-source: CPPImportPower* tables)
# ---------------------------------------------------------------------------

def fetch_import_power_sources(plant_id: str, financial_year: str) -> list:
    """
    Fetch all active import power sources for a plant.

    Returns: [{"id": str, "source_name": str}, ...]
    """
    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT Id, SourceName
            FROM {T.CPP_IMPORT_POWER_SOURCE_MAPPING}
            WHERE {T.IMPORT_PLANT_FK} = ? AND IsActive = 1
            ORDER BY SourceName
            """,
            (plant_id,),
        )
        return [{"id": str(row[0]), "source_name": row[1]}
                for row in cur.fetchall()]
    except Exception as e:
        logger.error("  [IMPORT] Error fetching sources: %s", e)
        return []
    finally:
        conn.close()


def fetch_import_power(plant_id: str, month: int, year: int) -> dict:
    """
    Fetch total import power (MWh) across all sources for a plant/month.

    MWh = Capacity_MW × OperationalHours per source, then SUM.

    Returns:
        {
            "success": bool,
            "total_mwh": float,
            "source_count": int,
            "per_source": [{"source_name": str, "capacity_mw": float, "hours": float, "mwh": float}, ...],
            "message": str,
        }
    """
    fy = _fy_string(month, year)
    mcol = _MONTH_COL_TITLE.get(month, "Jan")

    sources = fetch_import_power_sources(plant_id, fy)
    if not sources:
        return {
            "success": True, "total_mwh": 0.0, "source_count": 0,
            "per_source": [], "message": "No import power sources found",
        }

    source_ids = [s["id"] for s in sources]
    placeholders = ",".join(["?"] * len(source_ids))

    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT {T.IMPORT_SOURCE_FK}, [{mcol}]
            FROM {T.CPP_IMPORT_POWER_CAPACITY}
            WHERE {T.IMPORT_SOURCE_FK} IN ({placeholders})
              AND {T.IMPORT_FINANCIAL_YEAR} = ?
            """,
            source_ids + [fy],
        )
        cap_map = {str(r[0]): float(r[1]) if r[1] else 0.0 for r in cur.fetchall()}

        cur.execute(
            f"""
            SELECT {T.IMPORT_SOURCE_FK}, [{mcol}]
            FROM {T.CPP_IMPORT_POWER_OPERATIONAL_HOURS}
            WHERE {T.IMPORT_SOURCE_FK} IN ({placeholders})
              AND {T.IMPORT_FINANCIAL_YEAR} = ?
            """,
            source_ids + [fy],
        )
        hrs_map = {str(r[0]): float(r[1]) if r[1] else 0.0 for r in cur.fetchall()}

        per_source = []
        total_mwh = 0.0
        for s in sources:
            sid = s["id"]
            cap = cap_map.get(sid, 0.0)
            hrs = hrs_map.get(sid, 0.0)
            mwh = cap * hrs
            total_mwh += mwh
            per_source.append({
                "source_name": s["source_name"],
                "capacity_mw": round(cap, 4),
                "hours":       round(hrs, 4),
                "mwh":         round(mwh, 4),
            })

        return {
            "success":      True,
            "total_mwh":    round(total_mwh, 4),
            "source_count": len(sources),
            "per_source":   per_source,
            "message":      f"Calculated import power from {len(sources)} source(s)",
        }
    except Exception as e:
        return {
            "success": False, "total_mwh": 0.0, "source_count": 0,
            "per_source": [], "message": str(e),
        }
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# 9. GT Heat Rate & Free Steam Factor (CPP_GTHeatRate)
# ---------------------------------------------------------------------------

# Map NMD power asset names to CPP_GTHeatRate AssetName keys
_GT_HEAT_RATE_NAME_MAP = {
    # NMD asset name → heat rate curve name
    "NMD-POWER PLANT-1": "GT-1",
    "NMD-POWER PLANT-2": "GT-2",
    "NMD-POWER PLANT-3": "GT-3",
}


def _canonical_gt_name(asset_name: str) -> str | None:
    """Map NMD power asset name to GT-1/GT-2/GT-3 for heat rate lookup."""
    key = (asset_name or "").upper().strip()
    if key in _GT_HEAT_RATE_NAME_MAP:
        return _GT_HEAT_RATE_NAME_MAP[key]
    # Fuzzy fallback
    n = key.replace(" ", "").replace("-", "")
    if "PLANT1" in n or "GT1" in n:
        return "GT-1"
    if "PLANT2" in n or "GT2" in n:
        return "GT-2"
    if "PLANT3" in n or "GT3" in n:
        return "GT-3"
    return None


def fetch_gt_heat_rate_and_free_steam(asset_name: str, gt_load_mw: float,
                                       month: int, year: int) -> tuple:
    """
    Fetch GT Heat Rate (Kcal/KWh) and FreeSteamFactor from CPP_GTHeatRate
    based on the GT's allocated load (MW).

    Uses linear interpolation between nearest GTLoad points.
    Clamps to endpoints if load is outside the curve range.

    Args:
        asset_name: Power asset name (e.g. "NMD-Power Plant-1")
        gt_load_mw: GT allocated load in MW (GrossMWh / OperatingHours)
        month, year: For financial year lookup

    Returns:
        (heat_rate_kcal_kwh, free_steam_factor) or (0.0, 0.0) if not found
    """
    canonical = _canonical_gt_name(asset_name)
    if not canonical:
        return 0.0, 0.0

    fy = _fy_string(month, year)

    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT {T.GT_HR_LOAD_COL}, {T.GT_HR_VALUE_COL}, FreeSteamFactor
            FROM {T.CPP_GT_HEAT_RATE}
            WHERE {T.GT_HR_FINANCIAL_YEAR} = ? AND {T.GT_HR_ASSET_NAME} = ?
            ORDER BY {T.GT_HR_LOAD_COL}
            """,
            (fy, canonical),
        )
        rows = cur.fetchall()
        if not rows:
            logger.warning("  [GT HR] No heat rate data for %s (FY %s)", canonical, fy)
            return 0.0, 0.0

        points = sorted(
            [(float(r[0]), float(r[1]), float(r[2])) for r in rows],
            key=lambda x: x[0],
        )

        if gt_load_mw is None or gt_load_mw <= 0:
            return 0.0, 0.0

        min_load = points[0][0]
        max_load = points[-1][0]

        # Clamp to endpoints
        if gt_load_mw <= min_load:
            return points[0][1], points[0][2]
        if gt_load_mw >= max_load:
            return points[-1][1], points[-1][2]

        # Exact match or interpolation
        lower = None
        upper = None
        for load_pt, hr, fs in points:
            if abs(load_pt - gt_load_mw) < 1e-9:
                return hr, fs
            if load_pt < gt_load_mw:
                lower = (load_pt, hr, fs)
            elif load_pt > gt_load_mw and upper is None:
                upper = (load_pt, hr, fs)
                break

        if lower is None or upper is None:
            return 0.0, 0.0

        x1, hr1, fs1 = lower
        x2, hr2, fs2 = upper
        if abs(x2 - x1) < 1e-9:
            return hr1, fs1

        frac = (gt_load_mw - x1) / (x2 - x1)
        heat = hr1 + frac * (hr2 - hr1)
        steam = fs1 + frac * (fs2 - fs1)

        # Fallback to nearest non-zero if interpolated heat is 0
        if heat == 0.0:
            min_diff = float("inf")
            for load_pt, hr, fs in points:
                if hr != 0.0:
                    diff = abs(load_pt - gt_load_mw)
                    if diff < min_diff:
                        min_diff = diff
                        heat = hr
                        steam = fs

        return heat, steam
    except Exception as e:
        logger.error("  [GT HR] Error: %s", e)
        return 0.0, 0.0
    finally:
        conn.close()


def calculate_free_steam_from_gt(power_assets: list, month: int, year: int) -> dict:
    """
    Calculate free steam (MT) generated by each GT based on its max capacity
    and operational hours for the month.

    For each GT asset:
      - Load (MW) = MaxOperatingCapacity (max load for heat rate lookup)
      - GrossMWh = MaxMW × OperationalHours
      - FreeSteamFactor = from CPP_GTHeatRate at that load
      - Free Steam (MT) = GrossMWh × FreeSteamFactor

    Args:
        power_assets: List from fetch_asset_availability_with_hours()
        month, year: For financial year lookup

    Returns:
        {
            "details": {
                "NMD-Power Plant-1": {
                    "gt_name": "GT-1",
                    "load_mw": 21.9,
                    "gross_mwh": 13608.0,
                    "operational_hours": 620.0,
                    "heat_rate_kcal_kwh": 3292.48,
                    "free_steam_factor": 1.75,
                    "free_steam_mt": 23814.0,
                    "linked_hrsg": "HRSG-1",
                },
                ...
            },
            "total_free_steam_mt": 123456.78,
        }
    """
    # Map GT asset name to linked HRSG name
    _GT_TO_HRSG = {}
    # Build from steam assets if possible — but we don't have them here
    # Use convention: Plant-1 → HRSG-1, Plant-2 → HRSG-2, Plant-3 → HRSG-3
    for a in power_assets:
        name = (a.get("asset_name") or "").upper()
        n = name.replace(" ", "").replace("-", "")
        if "PLANT1" in n or "GT1" in n:
            _GT_TO_HRSG[a["asset_name"]] = "HRSG-1"
        elif "PLANT2" in n or "GT2" in n:
            _GT_TO_HRSG[a["asset_name"]] = "HRSG-2"
        elif "PLANT3" in n or "GT3" in n:
            _GT_TO_HRSG[a["asset_name"]] = "HRSG-3"

    details = {}
    total_free_steam = 0.0

    for a in power_assets:
        asset_name = a.get("asset_name", "")
        asset_type = (a.get("asset_type") or "").upper()
        hrs = a.get("operational_hours", 0) or 0
        max_mw = a.get("max_operating_capacity") or 0
        is_avail = a.get("is_available", False)

        # Only GTs generate free steam (not STG)
        if "STG" in asset_name.upper():
            continue

        canonical = _canonical_gt_name(asset_name)
        linked_hrsg = _GT_TO_HRSG.get(asset_name, "-")

        if not is_avail or hrs <= 0 or max_mw <= 0:
            details[asset_name] = {
                "gt_name": canonical or "-",
                "load_mw": 0.0,
                "gross_mwh": 0.0,
                "operational_hours": 0.0,
                "heat_rate_kcal_kwh": 0.0,
                "free_steam_factor": 0.0,
                "free_steam_mt": 0.0,
                "linked_hrsg": linked_hrsg,
            }
            continue

        gross_mwh = max_mw * hrs
        heat_rate, free_steam_factor = fetch_gt_heat_rate_and_free_steam(
            asset_name, max_mw, month, year
        )
        free_steam_mt = gross_mwh * free_steam_factor if free_steam_factor > 0 else 0.0

        details[asset_name] = {
            "gt_name": canonical or "-",
            "load_mw": round(max_mw, 2),
            "gross_mwh": round(gross_mwh, 2),
            "operational_hours": round(hrs, 1),
            "heat_rate_kcal_kwh": round(heat_rate, 2),
            "free_steam_factor": round(free_steam_factor, 4),
            "free_steam_mt": round(free_steam_mt, 2),
            "linked_hrsg": linked_hrsg,
        }
        total_free_steam += free_steam_mt

    return {
        "details": details,
        "total_free_steam_mt": round(total_free_steam, 2),
    }


# ---------------------------------------------------------------------------
# 10. Norms (NormsMonthDetail / NormsHeader)
# ---------------------------------------------------------------------------

def fetch_norms(plant_id: str, month: int, year: int) -> list:
    """
    Fetch all active norms for a plant/month from NormsMonthDetail/NormsHeader.

    Returns:
        List of dicts with keys: NormHeaderId, PlantName, UtilityName, UtilityUOM,
        MaterialName, IssuingUOM, NormParameterName, NormParameterUOM,
        NormValue, Quantity, Generation, GenerationUOM, Amount, Price, Month, Year.
    """
    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT
                nh.Id              AS NormHeaderId,
                p.Name             AS PlantName,
                nh.UtilityName,
                nh.UtilityUOM,
                nh.AccountName,
                nh.MaterialName,
                nh.IssuingPlantName,
                nh.IssuingUOM,
                np.DisplayName     AS NormParameterName,
                np.UOM             AS NormParameterUOM,
                nmd.Norms          AS NormValue,
                nmd.Quantity,
                nmd.QTY            AS Generation,
                nmd.GenerationUOM,
                nmd.Amount,
                nmd.Price,
                fym.Id             AS FinancialYearMonthId,
                fym.Month,
                fym.Year
            FROM {T.NORMS_MONTH_DETAIL} nmd
            INNER JOIN {T.NORMS_HEADER} nh  ON nh.Id   = nmd.NormsHeader_FK_Id
            INNER JOIN {T.PLANTS} p          ON p.Id    = nh.{T.NORMS_HEADER_PLANT_FK}
            INNER JOIN {T.FINANCIAL_YEAR_MONTH} fym
                                             ON fym.Id  = nmd.FinancialYearMonth_FK_Id
            LEFT  JOIN {T.NORM_PARAMETERS} np ON np.Id = nh.NormParameter_FK_Id
            WHERE fym.Month               = ?
              AND fym.Year                = ?
              AND nh.IsActive             = 1
              AND nh.{T.NORMS_HEADER_PLANT_FK} = ?
            ORDER BY p.Name, nh.DisplayOrder, nmd.DisplayOrder
            """,
            (month, year, plant_id),
        )
        cols = [c[0] for c in cur.description]
        return [dict(zip(cols, row)) for row in cur.fetchall()]
    except Exception as e:
        logger.error("  [NORMS] Error: %s", e)
        return []
    finally:
        conn.close()


def fetch_nmd_sub_plant_ids(parent_plant_id: str) -> List[str]:
    """
    Fetch all sub-plant IDs for NMD CPP plant.
    
    Queries the Plants table where SourceName = parent_plant_id to get
    all sub-plants under the NMD-CPP plant (e.g., NMD - Utility Plant,
    NMD - Power Plant 2, NMD - Power Plant 3, NMD - STG Power Plant).
    
    Args:
        parent_plant_id: The parent NMD CPP plant ID (e.g., '23BCA1B3-56DD-4C15-A3D6-3C2C9A62E653')
    
    Returns:
        List of plant IDs (UUIDs) for all NMD sub-plants
    """
    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT Id
            FROM {T.PLANTS}
            WHERE SourceName = ?
            """
            ,
            (parent_plant_id,)
        )
        rows = cur.fetchall()
        return [row[0] for row in rows]
    except Exception as e:
        logger.error("  [NMD SUB-PLANTS] Error: %s", e)
        return []
    finally:
        conn.close()


def fetch_norm_rows_all_plants(month: int, year: int, plant_id: str = None) -> list:
    """
    Fetch all active norm rows for a month/year (across ALL plants).

    Used by the norms reader to build the consumption matrix.
    Returns list of dicts with: plant_name, utility_name, material_name,
    account_name, issuing_plant_name, norm, qty.
    
    Args:
        month: Month (1-12)
        year: Year (e.g., 2026)
        plant_id: Optional plant UUID to filter by. If None, returns all plants.
                 For NMD, should pass the NMD plant ID to filter only NMD CPP utilities.
    """
    conn = get_connection()
    cur = conn.cursor()
    try:
        # Build query with optional plant filter
        if plant_id:
            # Fetch sub-plant IDs for NMD CPP
            sub_plant_ids = fetch_nmd_sub_plant_ids(plant_id)
            
            if not sub_plant_ids:
                logger.warning("  [NORMS] No sub-plants found for plant_id=%s, returning all norms", plant_id)
                sub_plant_ids = []
            
            # Build IN clause for sub-plant IDs
            placeholders = ','.join(['?' for _ in sub_plant_ids])
            
            query = f"""
            SELECT
                p.Name             AS PlantName,
                nh.UtilityName,
                nh.MaterialName,
                nh.AccountName,
                nh.IssuingPlantName,
                nmd.Norms,
                nmd.QTY
            FROM {T.NORMS_MONTH_DETAIL} nmd
            INNER JOIN {T.NORMS_HEADER} nh ON nh.Id = nmd.NormsHeader_FK_Id
            INNER JOIN {T.PLANTS} p        ON p.Id  = nh.{T.NORMS_HEADER_PLANT_FK}
            INNER JOIN {T.FINANCIAL_YEAR_MONTH} fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
            WHERE fym.Month = ? AND fym.Year = ?
              AND nh.IsActive = 1
              AND nh.{T.NORMS_HEADER_PLANT_FK} IN ({placeholders})
            """
            params = (month, year) + tuple(sub_plant_ids)
        else:
            query = f"""
            SELECT
                p.Name             AS PlantName,
                nh.UtilityName,
                nh.MaterialName,
                nh.AccountName,
                nh.IssuingPlantName,
                nmd.Norms,
                nmd.QTY
            FROM {T.NORMS_MONTH_DETAIL} nmd
            INNER JOIN {T.NORMS_HEADER} nh ON nh.Id = nmd.NormsHeader_FK_Id
            INNER JOIN {T.PLANTS} p        ON p.Id  = nh.{T.NORMS_HEADER_PLANT_FK}
            INNER JOIN {T.FINANCIAL_YEAR_MONTH} fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
            WHERE fym.Month = ? AND fym.Year = ?
              AND nh.IsActive = 1
            """
            params = (month, year)
        
        cur.execute(query, params)
        rows = []
        for row in cur.fetchall():
            rows.append({
                "plant_name":         row[0],
                "utility_name":       row[1],
                "material_name":      row[2],
                "account_name":       row[3],
                "issuing_plant_name": row[4],
                "norm":               float(row[5]) if row[5] is not None else None,
                "qty":                float(row[6]) if row[6] is not None else 0.0,
            })
        return rows
    except Exception as e:
        logger.error("  [NORMS ALL] Error: %s", e)
        return []
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# 11. Generation Utilities & U4U Consumption Matrix (from NormsHeader)
# ---------------------------------------------------------------------------

def fetch_generation_utilities_and_u4u(month: int, year: int) -> dict:
    """
    Fetch all generation utilities and their U4U consumption from NormsHeader
    for all NMD sub-plants.

    This builds the U4U consumption matrix: which utilities are produced (generation)
    and what each utility consumes to produce (U4U consumption).

    Data comes from NormsHeader where AccountName = 'Utilities'.
    - UtilityName = the generation utility (producer)
    - MaterialName = the consumed utility (U4U input)
    - Norm = consumption norm per unit of generation
    - Qty = actual consumption quantity for the month
    - Gen (QTY) = generation quantity for the month
    - IssuingPlantName = the plant that produces the consumed material

    Returns:
        {
            "generation_utilities": [
                {
                    "utility_name": "COMPRESSED AIR",
                    "plant_name": "NMD - Utility Plant",
                    "uom": "NM3",
                    "generation": 7098799.59,
                    "consumes": [
                        {"material": "Cooling Water 2", "issuing_plant": "NMD - Utility Plant", "norm": 0.000025, "qty": 177.47, "uom": "KM3"},
                        {"material": "Power_Dis", "issuing_plant": "NMD - Utility/Power Dist", "norm": 0.165, "qty": 1171301.93, "uom": "KWH"},
                    ],
                },
                ...
            ],
            "u4u_matrix": {
                # producer → {consumer → {norm, qty, uom, issuing_plant}}
                "COMPRESSED AIR": {
                    "Cooling Water 2": {"norm": 0.000025, "qty": 177.47, "uom": "KM3", "issuing_plant": "NMD - Utility Plant"},
                    "Power_Dis": {"norm": 0.165, "qty": 1171301.93, "uom": "KWH", "issuing_plant": "NMD - Utility/Power Dist"},
                },
                ...
            },
            "all_utilities": sorted list of all utility names involved,
            "producer_utilities": sorted list of utilities that generate,
            "consumer_utilities": sorted list of utilities that are consumed (U4U inputs),
        }
    """
    sub_plants = fetch_nmd_sub_plant_ids(NMD_PLANT_ID)
    if not sub_plants:
        return {"generation_utilities": [], "u4u_matrix": {}, "all_utilities": [], "producer_utilities": [], "consumer_utilities": []}

    plant_ids = sub_plants
    placeholders = ",".join(["?"] * len(plant_ids))

    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT
                p.Name             AS PlantName,
                nh.UtilityName,
                nh.MaterialName,
                nh.UtilityUOM,
                nh.IssuingUOM,
                nh.IssuingPlantName,
                nmd.Norms,
                nmd.Quantity,
                nmd.QTY            AS Generation
            FROM {T.NORMS_MONTH_DETAIL} nmd
            INNER JOIN {T.NORMS_HEADER} nh ON nh.Id = nmd.NormsHeader_FK_Id
            INNER JOIN {T.PLANTS} p        ON p.Id  = nh.{T.NORMS_HEADER_PLANT_FK}
            INNER JOIN {T.FINANCIAL_YEAR_MONTH} fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
            WHERE fym.Month = ? AND fym.Year = ?
              AND nh.IsActive = 1
              AND nh.AccountName = 'Utilities'
              AND nh.{T.NORMS_HEADER_PLANT_FK} IN ({placeholders})
            ORDER BY p.Name, nh.DisplayOrder
            """,
            [month, year] + plant_ids,
        )
        rows = cur.fetchall()
    except Exception as e:
        logger.error("  [GEN U4U] Error: %s", e)
        conn.close()
        return {"generation_utilities": [], "u4u_matrix": {}, "all_utilities": [], "producer_utilities": [], "consumer_utilities": []}
    finally:
        conn.close()

    # Build structured data
    # Group by (plant_name, utility_name) → generation info + consumes list
    gen_map = {}  # (plant, utility) → {uom, generation, consumes: []}
    u4u_matrix = {}  # utility → {material → {norm, qty, uom, issuing_plant}}
    all_utilities = set()
    producer_utilities = set()
    consumer_utilities = set()

    for row in rows:
        plant_name = row[0] or ""
        util_name = (row[1] or "").strip()
        material_name = (row[2] or "").strip()
        util_uom = (row[3] or "").strip()
        issuing_uom = (row[4] or "").strip()
        issuing_plant = (row[5] or "").strip()
        norm = float(row[6]) if row[6] is not None else 0.0
        qty = float(row[7]) if row[7] is not None else 0.0
        generation = float(row[8]) if row[8] is not None else 0.0

        if not util_name:
            continue

        key = (plant_name, util_name)
        if key not in gen_map:
            gen_map[key] = {
                "utility_name": util_name,
                "plant_name": plant_name,
                "uom": util_uom,
                "generation": generation,
                "consumes": [],
            }
            producer_utilities.add(util_name)
            all_utilities.add(util_name)

        # Add consumption entry (material = what this utility consumes)
        if material_name and material_name != "No Material":
            consume_entry = {
                "material": material_name,
                "issuing_plant": issuing_plant,
                "norm": norm,
                "qty": qty,
                "uom": issuing_uom,
            }
            gen_map[key]["consumes"].append(consume_entry)
            consumer_utilities.add(material_name)
            all_utilities.add(material_name)

            # Build U4U matrix
            if util_name not in u4u_matrix:
                u4u_matrix[util_name] = {}
            u4u_matrix[util_name][material_name] = {
                "norm": norm,
                "qty": qty,
                "uom": issuing_uom,
                "issuing_plant": issuing_plant,
            }

    generation_utilities = sorted(gen_map.values(), key=lambda x: (x["plant_name"], x["utility_name"]))

    return {
        "generation_utilities": generation_utilities,
        "u4u_matrix": u4u_matrix,
        "all_utilities": sorted(all_utilities),
        "producer_utilities": sorted(producer_utilities),
        "consumer_utilities": sorted(consumer_utilities),
    }


# ---------------------------------------------------------------------------
# 12. CPPNorms — U4U consumption norms per generation utility
# ---------------------------------------------------------------------------

_CPP_NORMS_MONTH_COL = {
    1: "Jan_Norms",  2: "Feb_Norms",  3: "Mar_Norms",  4: "Apr_Norms",
    5: "May_Norms",  6: "Jun_Norms",  7: "Jul_Norms",  8: "Aug_Norms",
    9: "Sep_Norms", 10: "Oct_Norms", 11: "Nov_Norms", 12: "Dec_Norms",
}


def fetch_cpp_norms_for_utilities(month: int, year: int) -> list:
    """
    Fetch U4U consumption norms from the CPPNorms table for all NMD sub-plants.

    CPPNorms stores one row per NormsHeader with 12 monthly norm columns.
    We join to NormsHeader to get UtilityName, MaterialName, AccountName,
    UtilityUOM, IssuingUOM, IssuingPlantName and to Plants for the plant name.

    Only rows where AccountName = 'Utilities' are returned (U4U consumption).

    Returns list of dicts:
        plant_name, utility_name, material_name, utility_uom, issuing_uom,
        issuing_plant, norm (float), account_name
    """
    col = _CPP_NORMS_MONTH_COL.get(month, "Apr_Norms")

    if month >= 4:
        financial_year = f"{year}-{str(year + 1)[-2:]}"
    else:
        financial_year = f"{year - 1}-{str(year)[-2:]}"

    sub_plants = fetch_nmd_sub_plant_ids(NMD_PLANT_ID)
    if not sub_plants:
        return []

    plant_ids = sub_plants
    placeholders = ",".join(["?"] * len(plant_ids))

    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT
                p.Name             AS PlantName,
                nh.UtilityName,
                nh.MaterialName,
                nh.AccountName,
                nh.UtilityUOM,
                nh.IssuingUOM,
                nh.IssuingPlantName,
                cn.{col}           AS NormValue
            FROM CPPNorms cn
            INNER JOIN {T.NORMS_HEADER} nh ON nh.Id = cn.NormsHeader_FK_Id
            INNER JOIN {T.PLANTS} p        ON p.Id  = nh.{T.NORMS_HEADER_PLANT_FK}
            WHERE nh.IsActive = 1
              AND cn.FinancialYear = ?
              AND cn.{col} IS NOT NULL
              AND nh.AccountName = 'Utilities'
              AND nh.{T.NORMS_HEADER_PLANT_FK} IN ({placeholders})
            ORDER BY p.Name, nh.DisplayOrder
            """,
            [financial_year] + plant_ids,
        )
        rows = cur.fetchall()
    except Exception as e:
        logger.error("  [CPP NORMS] Error: %s", e)
        conn.close()
        return []
    finally:
        conn.close()

    result = []
    for row in rows:
        result.append({
            "plant_name":      row[0] or "",
            "utility_name":    (row[1] or "").strip(),
            "material_name":   (row[2] or "").strip(),
            "account_name":    (row[3] or "").strip(),
            "utility_uom":     (row[4] or "").strip(),
            "issuing_uom":     (row[5] or "").strip(),
            "issuing_plant":   (row[6] or "").strip(),
            "norm":            float(row[7]) if row[7] is not None else 0.0,
        })
    return result


# ---------------------------------------------------------------------------
# 10. GT Heat Rate Curves
# ---------------------------------------------------------------------------

def fetch_gt_heat_rate_curves(financial_year: str) -> dict:
    """
    Fetch GT heat-rate curves from CPP_GTHeatRate for all GTs.

    Args:
        financial_year: e.g. "2025-26" or "2025"

    Returns:
        Dict keyed by canonical GT name ("GT-1", "GT-2", "GT-3"):
        {
            "GT-1": [{"gt_load": float, "heat_rate": float, "free_steam_factor": float}, ...],
            ...
        }
    """
    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT {T.GT_HR_ASSET_NAME}, {T.GT_HR_LOAD_COL},
                   {T.GT_HR_VALUE_COL}, {T.GT_HR_FREE_STEAM_FACTOR}
            FROM {T.CPP_GT_HEAT_RATE}
            WHERE {T.GT_HR_FINANCIAL_YEAR} = ?
              AND {T.GT_HR_ASSET_NAME} IN ('GT-1', 'GT-2', 'GT-3', 'GT1', 'GT2', 'GT3')
            ORDER BY {T.GT_HR_ASSET_NAME}, {T.GT_HR_LOAD_COL}
            """,
            (financial_year,),
        )
        curves: dict = {}
        for row in cur.fetchall():
            equip = str(row[0] or "").upper().replace(" ", "").replace("-", "")
            canonical = None
            if "GT1" in equip or "PLANT1" in equip:
                canonical = "GT-1"
            elif "GT2" in equip or "PLANT2" in equip:
                canonical = "GT-2"
            elif "GT3" in equip or "PLANT3" in equip:
                canonical = "GT-3"
            if not canonical:
                continue
            if canonical not in curves:
                curves[canonical] = []
            curves[canonical].append({
                "gt_load":           float(row[1]) if row[1] is not None else 0.0,
                "heat_rate":         float(row[2]) if row[2] is not None else 0.0,
                "free_steam_factor": float(row[3]) if row[3] is not None else 0.0,
            })
        return curves
    except Exception as e:
        logger.error("  [GT HEAT RATE] Error: %s", e)
        return {}
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# 11. STG Extraction Lookup
# ---------------------------------------------------------------------------

def fetch_stg_extraction_lookup() -> list:
    """
    Fetch STG extraction lookup data from database.

    Returns:
        List of dicts with keys: LoadMW, SVHInletTPH, SMBleedFlowTPH, SLExtFlowTPH,
                                 CondensingLoadM3Hr, HeatRateKcalKWH, EqSvhMp, EqSvhLp,
                                 SteamForPower, SpSteamPower
        Sorted by LoadMW ascending.
    """
    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute("""
            SELECT
                LoadMW,
                SVHInletTPH,
                SMBleedFlowTPH,
                SLExtFlowTPH,
                CondensingLoadM3Hr,
                HeatRateKcalKWH,
                ISNULL(EqSvhMp, 0) AS EqSvhMp,
                ISNULL(EqSvhLp, 0) AS EqSvhLp,
                ISNULL(SteamForPower, 0) AS SteamForPower,
                ISNULL(SpSteamPower, 0) AS SpSteamPower
            FROM STGExtractionLookup
            ORDER BY LoadMW ASC
        """)
        rows = cur.fetchall()
    except Exception as e:
        logger.error("  [STG EXTRACTION] Error: %s", e)
        conn.close()
        return []
    finally:
        conn.close()

    result = []
    for row in rows:
        result.append({
            "load_mw": float(row[0]) if row[0] is not None else 0.0,
            "svh_inlet_tph": float(row[1]) if row[1] is not None else 0.0,
            "sm_bleed_flow_tph": float(row[2]) if row[2] is not None else 0.0,
            "sl_ext_flow_tph": float(row[3]) if row[3] is not None else 0.0,
            "condensing_load_m3hr": float(row[4]) if row[4] is not None else 0.0,
            "heat_rate_kcal_kwh": float(row[5]) if row[5] is not None else 0.0,
            "eq_svh_mp": float(row[6]) if row[6] is not None else 0.0,
            "eq_svh_lp": float(row[7]) if row[7] is not None else 0.0,
            "steam_for_power": float(row[8]) if row[8] is not None else 0.0,
            "sp_steam_power": float(row[9]) if row[9] is not None else 0.0,
        })
    return result


def get_stg_extraction_for_load(stg_load_mw: float) -> dict:
    """
    Get STG extraction data for a given STG load by interpolating the lookup table.

    Args:
        stg_load_mw: STG load in MW

    Returns:
        Dict with interpolated extraction rates:
        {
            "svh_inlet_tph": float,
            "sm_bleed_flow_tph": float,
            "sl_ext_flow_tph": float,
            "condensing_load_m3hr": float,
            "heat_rate_kcal_kwh": float,
            "eq_svh_mp": float,
            "eq_svh_lp": float,
            "steam_for_power": float,
            "sp_steam_power": float
        }
    """
    lookup = fetch_stg_extraction_lookup()
    if not lookup:
        return {}

    # Find the two closest load points for interpolation
    # Sort by load_mw
    lookup_sorted = sorted(lookup, key=lambda x: x["load_mw"])

    # Find the bracketing points
    lower = None
    upper = None

    for i, point in enumerate(lookup_sorted):
        if stg_load_mw <= point["load_mw"]:
            upper = point
            if i > 0:
                lower = lookup_sorted[i - 1]
            break
    else:
        # If load is higher than all points, use the highest point
        lower = lookup_sorted[-1]
        upper = None

    # Interpolate
    if lower is None:
        # Load is lower than all points, use the lowest point
        result = upper
    elif upper is None:
        # Load is higher than all points, use the highest point
        result = lower
    elif lower["load_mw"] == upper["load_mw"]:
        # Exact match or same points
        result = lower
    else:
        # Linear interpolation
        t = (stg_load_mw - lower["load_mw"]) / (upper["load_mw"] - lower["load_mw"])
        result = {}
        for key in lower.keys():
            if key == "load_mw":
                result[key] = stg_load_mw
            else:
                result[key] = lower[key] + t * (upper[key] - lower[key])

    return result


