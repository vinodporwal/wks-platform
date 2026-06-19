"""
JMD Database Queries
====================
All fetch methods for JMD plants, mirroring PPPython-script database layer.
Every function accepts `plant_id` (UUID string) so the same code works for
all five JMD plants without any duplication.

Plant IDs are resolved via plant_mapper.PLANT_REGISTRY.

Usage:
    from database.queries import (
        fetch_process_demands,
        fetch_process_demand_master,
        fetch_power_generation_assets,
        fetch_operational_hours,
        fetch_asset_availability,
        fetch_import_power,
        fetch_norms,
        fetch_fixed_consumption,
        fetch_stg_extraction_lookup,
        fetch_hrsg_heat_rate_lookup,
        fetch_financial_year_month_id,
    )
"""

import json
import logging
import os

import pandas as pd
from database.connection import get_connection
from database.tables import T, USE_DUMMY

_DATA_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "data")


def _load_dummy_schema() -> dict:
    path = os.path.join(_DATA_DIR, "dummy_schema.json")
    with open(path, encoding="utf-8") as f:
        return json.load(f)


logger = logging.getLogger(__name__)

# SQL Server error codes that indicate a schema problem (missing table/column).
# These are not transient — the calculation cannot proceed with empty data.
_SCHEMA_ERROR_CODES = {"42S02", "42S22"}


class DataFetchError(Exception):
    """Raised when a fetch function hits a DB schema error (missing table/column).

    Unlike a generic exception, this signals that the calculation result would
    be silently wrong — not just incomplete — so the caller must abort.
    """
    def __init__(self, label: str, original: Exception):
        self.label = label
        self.original = original
        super().__init__(f"[{label}] {original}")


def _is_schema_error(exc: Exception) -> bool:
    """Return True if *exc* is a SQL Server 'invalid column/object' error."""
    msg = str(exc)
    return any(code in msg for code in _SCHEMA_ERROR_CODES)

# ---------------------------------------------------------------------------
# Month helpers
# ---------------------------------------------------------------------------

_MONTH_COL = {
    1: "jan", 2: "feb", 3: "mar", 4: "apr",
    5: "may", 6: "jun", 7: "jul", 8: "aug",
    9: "sep", 10: "oct", 11: "nov", 12: "dec",
}

_MONTH_COL_TITLE = {
    1: "Jan", 2: "Feb", 3: "Mar", 4: "Apr",
    5: "May", 6: "Jun", 7: "Jul", 8: "Aug",
    9: "Sep", 10: "Oct", 11: "Nov", 12: "Dec",
}


def _fy_string(month: int, year: int) -> str:
    """Return financial year string like '2025-26'."""
    fy_start = year if month >= 4 else year - 1
    return f"{fy_start}-{str(fy_start + 1)[-2:]}"


def _fy_year_short(month: int, year: int) -> str:
    """Return just the start-year string, e.g. '2025'."""
    return str(year if month >= 4 else year - 1)


def _normalize_utility_name(name: str) -> str:
    """Normalize utility names so DB labels can be mapped reliably."""
    return "".join(ch.lower() for ch in str(name) if ch.isalnum())


_FIXED_RESULT_KEYS = (
    "power_fixed_kwh",
    "power_fixed",
    "lp_fixed",
    "mp_fixed",
    "hp_fixed",
    "shp_fixed",
    "air_fixed",
    "nitrogen_asu_fixed",
    "dm_fixed",
    "cw1_fixed",
    "cw2_fixed",
    "cooling_water_fixed",
    "raw_water_fixed",
    "utility_water_fixed",
    "ret_steam_condensate_fixed",
    "oxygen_fixed",
    "effluent_fixed",
)

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

_FIXED_SUMMARY_META = (
    ("power_fixed", "Power", "MWh"),
    ("lp_fixed", "LP Steam", "MT"),
    ("mp_fixed", "MP Steam", "MT"),
    ("hp_fixed", "HP Steam", "MT"),
    ("shp_fixed", "SHP Steam", "MT"),
    ("air_fixed", "Compressed Air", "NM3"),
    ("nitrogen_asu_fixed", "Nitrogen ASU", "NM3"),
    ("dm_fixed", "DM Water", "M3"),
    ("cw1_fixed", "Cooling Water 1", "KM3"),
    ("cw2_fixed", "Cooling Water 2", "KM3"),
    ("cooling_water_fixed", "Cooling Water", "KM3"),
    ("raw_water_fixed", "Raw Water", "M3"),
    ("utility_water_fixed", "Utility Water", "M3"),
    ("ret_steam_condensate_fixed", "Ret Steam Condensate", "M3"),
    ("oxygen_fixed", "Oxygen", "MT"),
    ("effluent_fixed", "Effluent", "M3"),
)


def _empty_fixed_result() -> dict:
    return {key: 0.0 for key in _FIXED_RESULT_KEYS}



# ---------------------------------------------------------------------------
# 1. FinancialYearMonth ID lookup
# ---------------------------------------------------------------------------

def fetch_financial_year_month_id(month: int, year: int) -> str | None:
    """
    Return the FinancialYearMonth.Id (UUID) for the given month/year.

    Args:
        month: 1-12
        year:  calendar year

    Returns:
        UUID string or None if not found.
    """
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
# 2. Process Demands  (CalculatedProcessDemand)
# ---------------------------------------------------------------------------

# Utility name → model parameter key
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
}


def fetch_process_demands(plant_id: str, month: int, year: int) -> dict:
    """
    Fetch aggregated process utility demands for a plant/month.

    Mirrors CPP_NMD_GetProcessDemandByYear: joins ProcessDemandMaster
    (filtered by cpp_plant_fK_id = UUID) to CalculatedProcessDemand
    on process_plant_id + cpp_utility_id + cpp_plant_id + financial_year.

    Args:
        plant_id: CPP plant UUID
        month:    1-12
        year:     calendar year
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
    )}

    conn = get_connection()
    cur = conn.cursor()
    try:
        query = f"""
            SELECT
                m.cpp_utility,
                ISNULL(c.[{col}], 0) AS demand
            FROM dbo.{T.PROCESS_DEMAND_MASTER} m
            LEFT JOIN dbo.{T.CALCULATED_PROCESS_DEMAND} c
                ON  m.process_plant_id          = c.process_plant_id
                AND m.cpp_utility_id            = c.cpp_utility_id
                AND ISNULL(m.cpp_plant_id, '')  = ISNULL(c.cpp_plant_id, '')
                AND c.financial_year            = ?
            WHERE m.cpp_plant_fK_id = ?
              AND m.is_active        = 1
        """
        cur.execute(query, (fy, plant_id))
        rows = cur.fetchall()

        logger.info("  [PROCESS] Found %d rows for FY %s, plant %s", len(rows), fy, plant_id)
        logger.info("  [PROCESS] %-24s  %14s  %-28s", "Utility", "Demand", "Mapped Key")
        logger.info("  [PROCESS] %s  %s  %s", "-" * 24, "-" * 14, "-" * 28)
        for row in rows:
            util_name = row[0] or ""
            value = float(row[1]) if row[1] is not None else 0.0
            key = UTILITY_MAPPING.get(util_name)
            if key and key in result:
                result[key] += value
                logger.info("  [PROCESS] %-24s  %14.2f  %-28s", util_name, value, key)
            else:
                logger.info("  [PROCESS] %-24s  %14.2f  %-28s", util_name, value, "UNMAPPED")

        logger.info("  [PROCESS] %-24s  %14s  %-28s", "TOTAL", "", "")
        for key_name, total in result.items():
            if total:
                logger.info("  [PROCESS]   %-22s %14.2f", key_name, total)

        return result
    except Exception as e:
        logger.error("  [PROCESS] Error: %s", e)
        return result
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# 2b. ProcessDemandMaster — configuration table (process plant → utility → CPP plant)
# ---------------------------------------------------------------------------

def fetch_process_demand_master(plant_id: str, financial_year: str = None) -> list:
    """
    Fetch active ProcessDemandMaster records joined with CalculatedProcessDemand
    for a JMD CPP plant, mirroring CPP_NMD_GetProcessDemandByYear.

    Args:
        plant_id:       CPP plant UUID
        financial_year: e.g. '2025-26'. If None, returns master config only (no month values).

    Returns:
        List of dicts with process_plant, cpp_utility, uom, display_order,
        and apr..mar month values (0 if not calculated).
    """
    conn = get_connection()
    cur = conn.cursor()
    try:
        if financial_year:
            cur.execute(
                f"""
                SELECT
                    m.process_plant,
                    m.process_plant_id,
                    m.cpp_utility,
                    m.cpp_utility_id,
                    m.cpp_plant,
                    m.cpp_plant_id,
                    m.uom,
                    m.display_order,
                    ISNULL(c.apr, 0) AS apr,
                    ISNULL(c.may, 0) AS may,
                    ISNULL(c.jun, 0) AS jun,
                    ISNULL(c.jul, 0) AS jul,
                    ISNULL(c.aug, 0) AS aug,
                    ISNULL(c.sep, 0) AS sep,
                    ISNULL(c.oct, 0) AS oct,
                    ISNULL(c.nov, 0) AS nov,
                    ISNULL(c.dec, 0) AS dec,
                    ISNULL(c.jan, 0) AS jan,
                    ISNULL(c.feb, 0) AS feb,
                    ISNULL(c.mar, 0) AS mar,
                    CASE WHEN c.id IS NOT NULL THEN 1 ELSE 0 END AS is_calculated
                FROM dbo.{T.PROCESS_DEMAND_MASTER} m
                LEFT JOIN dbo.{T.CALCULATED_PROCESS_DEMAND} c
                    ON  m.process_plant_id         = c.process_plant_id
                    AND m.cpp_utility_id           = c.cpp_utility_id
                    AND ISNULL(m.cpp_plant_id, '') = ISNULL(c.cpp_plant_id, '')
                    AND c.financial_year           = ?
                WHERE m.cpp_plant_fK_id = ?
                  AND m.is_active       = 1
                ORDER BY m.process_plant, m.cpp_utility
                """,
                (financial_year, plant_id),
            )
        else:
            cur.execute(
                f"""
                SELECT
                    process_plant,
                    process_plant_id,
                    cpp_utility,
                    cpp_utility_id,
                    cpp_plant,
                    cpp_plant_id,
                    uom,
                    display_order,
                    NULL, NULL, NULL, NULL, NULL, NULL,
                    NULL, NULL, NULL, NULL, NULL, NULL, 0
                FROM dbo.{T.PROCESS_DEMAND_MASTER}
                WHERE cpp_plant_fK_id = ?
                  AND is_active       = 1
                ORDER BY display_order, process_plant, cpp_utility
                """,
                (plant_id,),
            )

        _MONTHS = ["apr","may","jun","jul","aug","sep","oct","nov","dec","jan","feb","mar"]
        results = []
        for row in cur.fetchall():
            entry = {
                "process_plant":    row[0] or "",
                "process_plant_id": row[1] or "",
                "cpp_utility":      row[2] or "",
                "cpp_utility_id":   row[3] or "",
                "cpp_plant":        row[4] or "",
                "cpp_plant_id":     row[5] or "",
                "uom":              row[6] or "",
                "display_order":    int(row[7]) if row[7] is not None else None,
                "is_calculated":    bool(row[20]) if row[20] is not None else False,
            }
            for i, m in enumerate(_MONTHS):
                entry[m] = float(row[8 + i]) if row[8 + i] is not None else 0.0
            results.append(entry)

        logger.info("  [PDM] %d active ProcessDemandMaster rows for plant %s", len(results), plant_id)
        return results
    except Exception as e:
        logger.error("  [PDM] Error fetching ProcessDemandMaster for plant %s: %s", plant_id, e)
        if _is_schema_error(e):
            raise DataFetchError("PROCESS_DEMAND_MASTER", e)
        return []
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# 3. Fixed Consumption
# ---------------------------------------------------------------------------

def fetch_fixed_consumption_by_plant(plant_id: str, month: int, year: int) -> dict:
    """
    Fetch fixed consumption for a plant/month and roll it up by utility.

    Mirrors CPP_GetFixedConsumptionByPlant:
      - resolves Plants.SourceName for the supplied CPP plant UUID
      - filters fixed rows by SourceName and financial year
      - joins CPPCostCentersMaster, NormParameters, and linked utility plant
      - rolls the month column up into utility buckets

    Returns a flat dict keyed by utility bucket, ready to merge with the
    process-demand dict:
        {
            "power_fixed": float,
            "lp_fixed": float,
            ...
        }
    """
    result = _empty_fixed_result()
    fy = _fy_string(month, year)
    col = _MONTH_COL.get(month)
    if not col:
        raise ValueError(f"Invalid month: {month}")

    candidate_tables = [T.FIXED_CONSUMPTION]
    legacy_table = getattr(T, "FIXED_CONSUMPTION_LEGACY", None)
    if legacy_table and legacy_table not in candidate_tables:
        candidate_tables.append(legacy_table)

    logger.info("  [FIXED] ==================================================")
    logger.info("  [FIXED] Fetching fixed consumption for plant %s", plant_id)
    logger.info("  [FIXED] Financial year : %s", fy)
    logger.info("  [FIXED] Selected month : %s", _MONTH_COL_TITLE[month])
    logger.info("  [FIXED] ==================================================")

    conn = get_connection()
    cur = conn.cursor()
    try:
        rows = []
        used_table = None
        logger.info("  [FIXED] Using CPP Plant ID as SourceName to find sub-plants: %s", plant_id)

        for table_name in candidate_tables:
            try:
                logger.info("  [FIXED] Trying table: %s (SourceName filter)", table_name)
                cur.execute(
                    f"""
                    SELECT
                        COALESCE(np.Name, '') AS utility_name,
                        COALESCE(np.UOM, '') AS uom,
                        COALESCE(cc.CostCenterName, '') AS cost_center_name,
                        COALESCE(p.DisplayName, '') AS plant_name,
                        SUM(ISNULL(fc.[{col}], 0)) AS total_consumption
                    FROM dbo.{table_name} fc
                    INNER JOIN dbo.Plants p
                        ON p.Id = fc.Plant_FK_Id
                    LEFT JOIN dbo.CPPCostCentersMaster cc
                        ON cc.CostCenterId = fc.CPP_CostCenter_FK_Id
                    LEFT JOIN dbo.NormParameters np
                        ON np.Id = fc.NormParameter_FK_Id
                    WHERE p.SourceName = ?
                      AND fc.AOPYear = ?
                    GROUP BY
                        COALESCE(np.Name, ''),
                        COALESCE(np.UOM, ''),
                        COALESCE(cc.CostCenterName, ''),
                        COALESCE(p.DisplayName, '')
                    ORDER BY
                        COALESCE(np.Name, ''),
                        COALESCE(cc.CostCenterName, '')
                    """,
                    (plant_id, fy),
                )
                rows = cur.fetchall()
                if rows:
                    used_table = table_name
                    logger.info("  [FIXED] Using table: %s", used_table)
                    break
                logger.info("  [FIXED] No rows returned for SourceName filter; trying Plant_FK_Id fallback")

                logger.info("  [FIXED] Trying table: %s (Plant_FK_Id filter)", table_name)
                cur.execute(
                    f"""
                    SELECT
                        COALESCE(np.Name, '') AS utility_name,
                        COALESCE(np.UOM, '') AS uom,
                        COALESCE(cc.CostCenterName, '') AS cost_center_name,
                        COALESCE(p.DisplayName, '') AS plant_name,
                        SUM(ISNULL(fc.[{col}], 0)) AS total_consumption
                    FROM dbo.{table_name} fc
                    LEFT JOIN dbo.CPPCostCentersMaster cc
                        ON cc.CostCenterId = fc.CPP_CostCenter_FK_Id
                    LEFT JOIN dbo.Plants p
                        ON p.Id = fc.Plant_FK_Id
                    LEFT JOIN dbo.NormParameters np
                        ON np.Id = fc.NormParameter_FK_Id
                    WHERE fc.Plant_FK_Id = ?
                      AND fc.AOPYear = ?
                    GROUP BY
                        COALESCE(np.Name, ''),
                        COALESCE(np.UOM, ''),
                        COALESCE(cc.CostCenterName, ''),
                        COALESCE(p.DisplayName, '')
                    ORDER BY
                        COALESCE(np.Name, ''),
                        COALESCE(cc.CostCenterName, '')
                    """,
                    (plant_id, fy),
                )
                rows = cur.fetchall()
                if rows:
                    used_table = table_name
                    logger.info("  [FIXED] Using table: %s", used_table)
                    break
            except Exception as table_error:
                if _is_schema_error(table_error):
                    logger.warning("  [FIXED] Table %s not available: %s", table_name, table_error)
                    continue
                raise

        if used_table is None:
            logger.warning("  [FIXED] No fixed consumption table available for plant %s", plant_id)
            return result

        logger.info("  [FIXED] Power values are normalized from kWh to MWh for the model")
        logger.info("  [FIXED] %-24s  %-22s  %-18s  %14s", "Plant", "Cost Center", "Utility", "Value")
        logger.info("  [FIXED] %s  %s  %s  %14s", "-" * 24, "-" * 22, "-" * 18, "-" * 14)

        for row in rows:
            utility_name = row[0] or ""
            uom          = row[1] or ""
            cost_center  = row[2] or "-"
            plant_name   = row[3] or "-"
            value_raw    = float(row[4]) if row[4] is not None else 0.0
            mapped_key   = _FIXED_UTILITY_MAP.get(_normalize_utility_name(utility_name))
            value        = value_raw / 1000.0 if mapped_key == "power_fixed" else value_raw

            logger.info(
                "  [FIXED] %-24s  %-22s  %-18s  %14.2f",
                plant_name,
                cost_center,
                utility_name if utility_name else (uom or "UNMAPPED"),
                value,
            )

            if mapped_key and mapped_key in result:
                if mapped_key == "power_fixed":
                    result["power_fixed_kwh"] += value_raw
                result[mapped_key] += value
            else:
                logger.info(
                    "  [FIXED]   -> unmapped utility kept out of rollup: %s",
                    utility_name or uom or "UNKNOWN",
                )

        logger.info("  [FIXED] --------------------------------------------------")
        logger.info("  [FIXED] Utility-wise fixed totals")
        logger.info("  [FIXED] %-24s  %14s  %-8s", "Utility", "Total", "Unit")
        logger.info("  [FIXED] %s  %s  %s", "-" * 24, "-" * 14, "-" * 8)
        for key_name, label, unit in _FIXED_SUMMARY_META:
            total = result.get(key_name, 0.0)
            if total > 0:
                logger.info("  [FIXED] %-24s  %14.2f  %-8s", label, total, unit)

        logger.info("  [FIXED] ==================================================")
        return result
    except Exception as e:
        logger.error("  [FIXED] Error fetching fixed consumption: %s", e)
        if _is_schema_error(e):
            raise DataFetchError("FIXED", e)
        return result
    finally:
        conn.close()


def fetch_fixed_consumption(plant_id: str, month: int, year: int) -> dict:
    """
    Backward-compatible wrapper for fetch_fixed_consumption_by_plant().

    Existing callers still use this function name, but the implementation now
    follows the stored-procedure SourceName-first plant lookup.
    """
    return fetch_fixed_consumption_by_plant(plant_id, month, year)


# ---------------------------------------------------------------------------
# 3b. Plants → PowerGenerationAssets → CPPAssetOperationalHours (new schema)
# ---------------------------------------------------------------------------

def fetch_plant_info(plant_id: str) -> dict:
    """
    Fetch plant record from the Plants table.

    Returns:
        {
            "plant_id": str,
            "name": str,
            "display_name": str,
            "plant_code": str,
            "is_active": bool,
            "business_category": str,
            "business_category_display": str,
        }
        or {} if not found.
    """
    conn = get_connection()
    cur  = conn.cursor()
    try:
        cur.execute(
            f"SELECT Id, Name, DisplayName, PlantCode, IsActive, "
            f"BusinessCategoryName, BusinessCategoryDisplayName "
            f"FROM {T.PLANTS} WHERE Id = ?",
            (plant_id,),
        )
        row = cur.fetchone()
        if not row:
            return {}
        return {
            "plant_id":                  str(row[0]),
            "name":                      row[1],
            "display_name":              row[2],
            "plant_code":                row[3] or "",
            "is_active":                 bool(row[4]),
            "business_category":         row[5] or "",
            "business_category_display": row[6] or "",
        }
    except Exception as e:
        logger.error("  [PLANT INFO] Error: %s", e)
        return {}
    finally:
        conn.close()


def fetch_plant_assets(plant_id: str) -> list:
    """
    Fetch all PowerGenerationAssets for a plant via Plants.Id = CPPPLANT_FK_Id.

    Returns:
        List of dicts:
        {
            "asset_id":                str,
            "asset_name":              str,
            "display_name":            str,
            "asset_type":              str,
            "plant_code":              str,
            "remarks":                 str,
            "utility_generation_fk":   str,
            "utility_distributed_fk":  str,
        }
    """
    conn = get_connection()
    cur  = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT
                a.AssetId,
                a.AssetName,
                a.displayName,
                a.AssetType,
                a.PlantCode,
                a.Remarks,
                a.UtilityGeneration_FK_Id,
                a.UtilityDistributed_FK_Id
            FROM {T.POWER_GENERATION_ASSETS} a
            WHERE a.{T.PGA_PLANT_FK} = ?
            ORDER BY a.AssetName
            """,
            (plant_id,),
        )
        return [
            {
                "asset_id":               str(row[0]),
                "asset_name":             row[1],
                "display_name":           row[2] or "",
                "asset_type":             row[3] or "",
                "plant_code":             row[4] or "",
                "remarks":                row[5] or "",
                "utility_generation_fk":  str(row[6]) if row[6] else "",
                "utility_distributed_fk": str(row[7]) if row[7] else "",
            }
            for row in cur.fetchall()
        ]
    except Exception as e:
        logger.error("  [PLANT ASSETS] Error: %s", e)
        return []
    finally:
        conn.close()


def fetch_asset_operational_hours(plant_id: str, month: int, year: int) -> list:
    """
    Fetch CPPAssetOperationalHours for all assets of a plant for a given month/year.

    CPPAssetOperationalHours stores hours in wide columns (Apr, May, … Mar)
    with AOPYear like '2025-26'.

    Links:
        Plants.Id = PowerGenerationAssets.CPPPLANT_FK_Id
        PowerGenerationAssets.AssetId = CPPAssetOperationalHours.Asset_FK_Id

    Returns:
        List of dicts:
        {
            "asset_id":            str,
            "asset_name":          str,
            "display_name":        str,
            "asset_type":          str,
            "utility_distributed": str,
            "utility_generated":   str,
            "aop_year":            str,   e.g. "2025-26"
            "operational_hours":   float, value from the month column
        }
        Rows with no hours record are included with operational_hours = None.
    """
    fy     = _fy_string(month, year)          # e.g. "2025-26"
    mcol   = _MONTH_COL_TITLE[month]          # e.g. "Apr"

    conn = get_connection()
    cur  = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT
                a.AssetId,
                a.AssetName,
                a.displayName,
                a.AssetType,
                oh.utility_distributed,
                oh.utility_generated,
                oh.{T.CAOH_YEAR_COL},
                oh.[{mcol}]          AS OperationalHours
            FROM {T.POWER_GENERATION_ASSETS} a
            LEFT JOIN {T.CPP_ASSET_OPERATIONAL_HOURS} oh
                   ON oh.{T.CAOH_ASSET_FK} = a.AssetId
                  AND oh.{T.CAOH_YEAR_COL} = ?
            WHERE a.{T.PGA_PLANT_FK} = ?
            ORDER BY a.AssetName
            """,
            (fy, plant_id),
        )
        results = []
        for row in cur.fetchall():
            results.append({
                "asset_id":            str(row[0]),
                "asset_name":          row[1],
                "display_name":        row[2] or "",
                "asset_type":          row[3] or "",
                "utility_distributed": row[4] or "",
                "utility_generated":   row[5] or "",
                "aop_year":            row[6] or fy,
                "operational_hours":   float(row[7]) if row[7] is not None else None,
            })
        return results
    except Exception as e:
        logger.error("  [ASSET OPS HRS] Error: %s", e)
        return []
    finally:
        conn.close()


def fetch_asset_operational_hours_all_months(plant_id: str, year: int) -> list:
    """
    Fetch CPPAssetOperationalHours for all 12 months (full FY) for a plant.

    Returns one row per asset with hours for each month Apr-Mar.
    FY is derived from the given calendar year as the FY start year
    (e.g. year=2025 → FY "2025-26").

    Returns:
        List of dicts:
        {
            "asset_id":   str,
            "asset_name": str,
            "asset_type": str,
            "aop_year":   str,
            "Apr": float|None, "May": float|None, ..., "Mar": float|None,
            "total_hours": float,
        }
    """
    fy = f"{year}-{str(year + 1)[-2:]}"

    conn = get_connection()
    cur  = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT
                a.AssetId, a.AssetName, a.AssetType,
                oh.{T.CAOH_YEAR_COL},
                oh.[Apr], oh.[May], oh.[Jun], oh.[Jul], oh.[Aug], oh.[Sep],
                oh.[Oct], oh.[Nov], oh.[Dec], oh.[Jan], oh.[Feb], oh.[Mar]
            FROM {T.POWER_GENERATION_ASSETS} a
            LEFT JOIN {T.CPP_ASSET_OPERATIONAL_HOURS} oh
                   ON oh.{T.CAOH_ASSET_FK} = a.AssetId
                  AND oh.{T.CAOH_YEAR_COL} = ?
            WHERE a.{T.PGA_PLANT_FK} = ?
            ORDER BY a.AssetName
            """,
            (fy, plant_id),
        )
        _months = ["Apr", "May", "Jun", "Jul", "Aug", "Sep",
                   "Oct", "Nov", "Dec", "Jan", "Feb", "Mar"]
        results = []
        for row in cur.fetchall():
            hrs = {m: (float(row[4 + i]) if row[4 + i] is not None else None)
                   for i, m in enumerate(_months)}
            total = sum(v for v in hrs.values() if v is not None)
            results.append({
                "asset_id":    str(row[0]),
                "asset_name":  row[1],
                "asset_type":  row[2] or "",
                "aop_year":    row[3] or fy,
                **hrs,
                "total_hours": round(total, 2),
            })
        return results
    except Exception as e:
        logger.error("  [ASSET OPS HRS ALL] Error: %s", e)
        return []
    finally:
        conn.close()


def fetch_asset_priority_all_months(plant_id: str, year: int) -> list:
    """
    Fetch CPPPowerAssetPriority for all 12 months (full FY) for a plant.

    Returns one row per asset with the integer priority for each month Apr-Mar.
    FY derived from year as the FY start year (e.g. 2025 → "2025-26").

    Returns:
        List of dicts:
        {
            "asset_id":   str,
            "asset_name": str,
            "asset_type": str,
            "aop_year":   str,
            "Apr": int|None, "May": int|None, ..., "Mar": int|None,
        }
    """
    fy = f"{year}-{str(year + 1)[-2:]}"

    conn = get_connection()
    cur  = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT
                a.AssetId, a.AssetName, a.AssetType,
                p.{T.PRIORITY_YEAR_COL},
                p.[Apr], p.[May], p.[Jun], p.[Jul], p.[Aug], p.[Sep],
                p.[Oct], p.[Nov], p.[Dec], p.[Jan], p.[Feb], p.[Mar]
            FROM {T.POWER_GENERATION_ASSETS} a
            LEFT JOIN {T.CPP_POWER_ASSET_PRIORITY} p
                   ON p.{T.PRIORITY_ASSET_FK} = a.AssetId
                  AND p.{T.PRIORITY_YEAR_COL}  = ?
                  AND p.{T.PRIORITY_PLANT_FK}  = ?
            WHERE a.{T.PGA_PLANT_FK} = ?
            ORDER BY a.AssetName
            """,
            (fy, plant_id, plant_id),
        )
        _months = ["Apr", "May", "Jun", "Jul", "Aug", "Sep",
                   "Oct", "Nov", "Dec", "Jan", "Feb", "Mar"]
        results = []
        for row in cur.fetchall():
            pri = {m: (int(row[4 + i]) if row[4 + i] is not None else None)
                   for i, m in enumerate(_months)}
            results.append({
                "asset_id":   str(row[0]),
                "asset_name": row[1],
                "asset_type": row[2] or "",
                "aop_year":   row[3] or fy,
                **pri,
            })
        return results
    except Exception as e:
        logger.error("  [ASSET PRIORITY ALL] Error: %s", e)
        return []
    finally:
        conn.close()


def fetch_steam_asset_priority_all_months(plant_id: str, year: int) -> list:
    """
    Fetch CPPSteamAssetsPriority for all 12 months (full FY) for a plant.

    Same wide-column schema as CPPPowerAssetPriority but for steam assets.

    Returns:
        List of dicts:
        {
            "asset_id":   str,
            "asset_name": str,
            "asset_type": str,
            "steam_type": str,
            "aop_year":   str,
            "Apr": int|None, "May": int|None, ..., "Mar": int|None,
        }
    """
    fy = f"{year}-{str(year + 1)[-2:]}"

    conn = get_connection()
    cur  = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT
                a.AssetId, a.AssetName, a.AssetType, a.SteamType,
                p.{T.PRIORITY_YEAR_COL},
                p.[Apr], p.[May], p.[Jun], p.[Jul], p.[Aug], p.[Sep],
                p.[Oct], p.[Nov], p.[Dec], p.[Jan], p.[Feb], p.[Mar]
            FROM {T.CPP_STEAM_GENERATION_ASSET} a
            LEFT JOIN {T.CPP_STEAM_ASSETS_PRIORITY} p
                   ON p.{T.PRIORITY_ASSET_FK} = a.AssetId
                  AND p.{T.PRIORITY_YEAR_COL}  = ?
                  AND p.{T.PRIORITY_PLANT_FK}  = ?
            WHERE a.{T.SGA_PLANT_FK} = ?
            ORDER BY a.AssetName
            """,
            (fy, plant_id, plant_id),
        )
        _months = ["Apr", "May", "Jun", "Jul", "Aug", "Sep",
                   "Oct", "Nov", "Dec", "Jan", "Feb", "Mar"]
        results = []
        for row in cur.fetchall():
            pri = {m: (int(row[5 + i]) if row[5 + i] is not None else None)
                   for i, m in enumerate(_months)}
            results.append({
                "asset_id":   str(row[0]),
                "asset_name": row[1],
                "asset_type": row[2] or "",
                "steam_type": row[3] or "",
                "aop_year":   row[4] or fy,
                **pri,
            })
        return results
    except Exception as e:
        logger.error("  [STEAM PRIORITY ALL] Error: %s", e)
        return []
    finally:
        conn.close()


def fetch_asset_priority(plant_id: str, month: int, year: int) -> list:
    """
    Fetch CPPPowerAssetPriority for a plant/month.

    Priority is stored as a month column (Apr-Mar integers), one row per asset per AOPYear.

    Links:
        CPPPowerAssetPriority.Plant_FK_Id = plant_id
        CPPPowerAssetPriority.Asset_FK_Id = PowerGenerationAssets.AssetId

    Returns:
        List of dicts ordered by priority value ascending:
        {
            "asset_id":   str,
            "asset_name": str,
            "asset_type": str,
            "aop_year":   str,
            "priority":   int or None,
        }
        Assets with no priority record are included with priority = None.
    """
    fy   = _fy_string(month, year)
    mcol = _MONTH_COL_TITLE[month]

    conn = get_connection()
    cur  = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT
                a.AssetId,
                a.AssetName,
                a.AssetType,
                p.{T.PRIORITY_YEAR_COL},
                p.[{mcol}]  AS Priority
            FROM {T.POWER_GENERATION_ASSETS} a
            LEFT JOIN {T.CPP_POWER_ASSET_PRIORITY} p
                   ON p.{T.PRIORITY_ASSET_FK} = a.AssetId
                  AND p.{T.PRIORITY_YEAR_COL}  = ?
                  AND p.{T.PRIORITY_PLANT_FK}  = ?
            WHERE a.{T.PGA_PLANT_FK} = ?
            ORDER BY p.[{mcol}] ASC, a.AssetName
            """,
            (fy, plant_id, plant_id),
        )
        results = []
        for row in cur.fetchall():
            results.append({
                "asset_id":   str(row[0]),
                "asset_name": row[1],
                "asset_type": row[2] or "",
                "aop_year":   row[3] or fy,
                "priority":   int(row[4]) if row[4] is not None else None,
            })
        return results
    except Exception as e:
        logger.error("  [ASSET PRIORITY] Error: %s", e)
        return []
    finally:
        conn.close()


def fetch_steam_asset_priority(plant_id: str, month: int, year: int) -> list:
    """
    Fetch CPPSteamAssetsPriority for a plant/month.

    Same wide-column schema as CPPPowerAssetPriority but for steam assets.
    Links: CPPSteamAssetsPriority.Asset_FK_Id = CPPSteamGenerationAsset.AssetId

    Returns:
        List of dicts ordered by priority value ascending:
        {
            "asset_id":   str,
            "asset_name": str,
            "asset_type": str,
            "steam_type": str,
            "aop_year":   str,
            "priority":   int or None,
        }
        Assets with no priority record are included with priority = None.
    """
    fy   = _fy_string(month, year)
    mcol = _MONTH_COL_TITLE[month]

    conn = get_connection()
    cur  = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT
                a.AssetId,
                a.AssetName,
                a.AssetType,
                a.SteamType,
                p.{T.PRIORITY_YEAR_COL},
                p.[{mcol}]  AS Priority
            FROM {T.CPP_STEAM_GENERATION_ASSET} a
            LEFT JOIN {T.CPP_STEAM_ASSETS_PRIORITY} p
                   ON p.{T.PRIORITY_ASSET_FK} = a.AssetId
                  AND p.{T.PRIORITY_YEAR_COL}  = ?
                  AND p.{T.PRIORITY_PLANT_FK}  = ?
            WHERE a.{T.SGA_PLANT_FK} = ?
            ORDER BY p.[{mcol}] ASC, a.AssetName
            """,
            (fy, plant_id, plant_id),
        )
        results = []
        for row in cur.fetchall():
            results.append({
                "asset_id":   str(row[0]),
                "asset_name": row[1],
                "asset_type": row[2] or "",
                "steam_type": row[3] or "",
                "aop_year":   row[4] or fy,
                "priority":   int(row[5]) if row[5] is not None else None,
            })
        return results
    except Exception as e:
        logger.error("  [STEAM ASSET PRIORITY] Error: %s", e)
        return []
    finally:
        conn.close()


def fetch_steam_generation_assets(plant_id: str) -> list:
    """
    Fetch all CPPSteamGenerationAsset records for a plant.

    Returns:
        List of dicts:
        {
            "asset_id":               str,
            "asset_name":             str,
            "asset_type":             str,
            "plant_code":             str,
            "display_name":           str,
            "remarks":                str,
            "steam_type":             str,
            "is_visible":             bool,
            "is_editable":            bool,
            "utility_generation_fk":  str,
            "utility_distributed_fk": str,
        }
    """
    conn = get_connection()
    cur  = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT
                AssetId,
                AssetName,
                AssetType,
                PlantCode,
                DisplayName,
                Remarks,
                SteamType,
                IsVisible,
                IsEditable,
                UtilityGeneration_FK_Id,
                UtilityDistributed_FK_Id
            FROM {T.CPP_STEAM_GENERATION_ASSET}
            WHERE {T.SGA_PLANT_FK} = ?
            ORDER BY AssetName
            """,
            (plant_id,),
        )
        results = []
        for row in cur.fetchall():
            results.append({
                "asset_id":               str(row[0]),
                "asset_name":             row[1],
                "asset_type":             row[2] or "",
                "plant_code":             row[3] or "",
                "display_name":           row[4] or "",
                "remarks":                row[5] or "",
                "steam_type":             row[6] or "",
                "is_visible":             bool(row[7]) if row[7] is not None else False,
                "is_editable":            bool(row[8]) if row[8] is not None else False,
                "utility_generation_fk":  str(row[9]) if row[9] else "",
                "utility_distributed_fk": str(row[10]) if row[10] else "",
            })
        return results
    except Exception as e:
        logger.error("  [STEAM GEN ASSETS] Error: %s", e)
        return []
    finally:
        conn.close()


def fetch_plant_power_info(plant_id: str, month: int, year: int) -> dict:
    """
    Combined fetch: plant info + all assets + operational hours for a month.

    Returns:
        {
            "plant":  dict from fetch_plant_info,
            "assets": list from fetch_plant_assets,
            "hours":  list from fetch_asset_operational_hours,
            "summary": {
                "total_assets":  int,
                "assets_with_hours": int,
                "total_operational_hours": float,
                "month": int,
                "year":  int,
                "aop_year": str,
            }
        }
    """
    plant  = fetch_plant_info(plant_id)
    assets = fetch_plant_assets(plant_id)
    hours  = fetch_asset_operational_hours(plant_id, month, year)

    # Build asset_id → hours lookup for quick merge
    hours_by_asset = {h["asset_id"]: h for h in hours}

    assets_with_hours = sum(
        1 for h in hours if h.get("operational_hours") is not None
    )
    total_hours = sum(
        h["operational_hours"] for h in hours
        if h.get("operational_hours") is not None
    )

    return {
        "plant":   plant,
        "assets":  assets,
        "hours":   hours,
        "summary": {
            "total_assets":            len(assets),
            "assets_with_hours":       assets_with_hours,
            "total_operational_hours": round(total_hours, 2),
            "month":    month,
            "year":     year,
            "aop_year": _fy_string(month, year),
        },
    }


# ---------------------------------------------------------------------------
# 3c. Power Asset Capacity (CPPPowerAssetCapacity — Min/Max MW per month)
# ---------------------------------------------------------------------------

def fetch_power_asset_capacity_all_months(plant_id: str, year: int) -> list:
    """
    Fetch CPPPowerAssetCapacity for all 12 months (full FY) for a plant.

    Columns: {Mon}_Min / {Mon}_Max (e.g. Jun_Min, Jun_Max) plus Fixed_Min / Fixed_Max.
    FY derived from year as the FY start year (e.g. 2025 → "2025-26").

    Returns:
        List of dicts:
        {
            "asset_id":   str,
            "asset_name": str,
            "asset_type": str,
            "aop_year":   str,
            "fixed_min":  float|None,
            "fixed_max":  float|None,
            "Apr_Min": float|None, "Apr_Max": float|None,
            "May_Min": float|None, "May_Max": float|None,
            ... through "Mar_Min" / "Mar_Max"
        }
    """
    fy = f"{year}-{str(year + 1)[-2:]}"
    _months = ["Apr", "May", "Jun", "Jul", "Aug", "Sep",
               "Oct", "Nov", "Dec", "Jan", "Feb", "Mar"]

    min_max_cols = ", ".join(
        f"cap.[{m}_Min], cap.[{m}_Max]" for m in _months
    )

    conn = get_connection()
    cur  = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT
                a.AssetId, a.AssetName, a.AssetType,
                cap.{T.CAPACITY_YEAR_COL},
                cap.Fixed_Min, cap.Fixed_Max,
                {min_max_cols}
            FROM {T.POWER_GENERATION_ASSETS} a
            LEFT JOIN {T.CPP_POWER_ASSET_CAPACITY} cap
                   ON cap.{T.CAPACITY_ASSET_FK} = a.AssetId
                  AND cap.{T.CAPACITY_YEAR_COL} = ?
            WHERE a.{T.PGA_PLANT_FK} = ?
            ORDER BY a.AssetName
            """,
            (fy, plant_id),
        )
        results = []
        for row in cur.fetchall():
            cap_data = {
                "asset_id":   str(row[0]),
                "asset_name": row[1],
                "asset_type": row[2] or "",
                "aop_year":   row[3] or fy,
                "fixed_min":  float(row[4]) if row[4] is not None else None,
                "fixed_max":  float(row[5]) if row[5] is not None else None,
            }
            # Unpack Mon_Min / Mon_Max pairs starting at index 6
            col_idx = 6
            for m in _months:
                cap_data[f"{m}_Min"] = float(row[col_idx])     if row[col_idx]     is not None else None
                cap_data[f"{m}_Max"] = float(row[col_idx + 1]) if row[col_idx + 1] is not None else None
                col_idx += 2
            results.append(cap_data)
        return results
    except Exception as e:
        logger.error("  [ASSET CAPACITY ALL] Error: %s", e)
        return []
    finally:
        conn.close()


def fetch_steam_asset_capacity_all_months(plant_id: str, year: int) -> list:
    """
    Fetch CPPSteamAssetCapacity for all 12 months (full FY) for a plant.

    Same shape as CPPPowerAssetCapacity, but joined to CPPSteamGenerationAsset.
    """
    fy = f"{year}-{str(year + 1)[-2:]}"
    _months = ["Apr", "May", "Jun", "Jul", "Aug", "Sep",
               "Oct", "Nov", "Dec", "Jan", "Feb", "Mar"]

    min_max_cols = ", ".join(
        f"cap.[{m}_Min], cap.[{m}_Max]" for m in _months
    )

    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT
                a.AssetId, a.AssetName, a.AssetType, a.SteamType,
                cap.{T.CAPACITY_YEAR_COL},
                cap.Fixed_Min, cap.Fixed_Max,
                {min_max_cols}
            FROM {T.CPP_STEAM_GENERATION_ASSET} a
            LEFT JOIN {T.CPP_STEAM_ASSET_CAPACITY} cap
                   ON cap.{T.CAPACITY_ASSET_FK} = a.AssetId
                  AND cap.{T.CAPACITY_YEAR_COL} = ?
            WHERE a.{T.SGA_PLANT_FK} = ?
            ORDER BY a.AssetName
            """,
            (fy, plant_id),
        )
        results = []
        for row in cur.fetchall():
            cap_data = {
                "asset_id":   str(row[0]),
                "asset_name": row[1],
                "asset_type": row[2] or "",
                "steam_type": row[3] or "",
                "aop_year":   row[4] or fy,
                "fixed_min":  float(row[5]) if row[5] is not None else None,
                "fixed_max":  float(row[6]) if row[6] is not None else None,
            }
            col_idx = 7
            for m in _months:
                cap_data[f"{m}_Min"] = float(row[col_idx]) if row[col_idx] is not None else None
                cap_data[f"{m}_Max"] = float(row[col_idx + 1]) if row[col_idx + 1] is not None else None
                col_idx += 2
            results.append(cap_data)
        return results
    except Exception as e:
        logger.error("  [STEAM ASSET CAPACITY ALL] Error: %s", e)
        return []
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# 4. Power Generation Assets
# ---------------------------------------------------------------------------

def fetch_power_generation_assets(plant_id: str) -> list:
    """
    Fetch all power generation assets for a plant.

    Queries PowerGenerationAssets filtered by CPPPLANT_FK_Id.

    Returns:
        List of dicts:
        {
            "asset_id":   str (UUID),
            "asset_name": str,
            "capacity_mw":float,
            "plant_code": str,
        }
    """
    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            """
            SELECT AssetId, AssetName, PlantCode
            FROM {T.POWER_GENERATION_ASSETS}
            WHERE {T.PGA_PLANT_FK} = ?
            ORDER BY AssetName
            """,
            (plant_id,),
        )
        cols = [c[0] for c in cur.description]
        results = []
        for row in cur.fetchall():
            d = dict(zip(cols, row))
            results.append({
                "asset_id":   str(d["AssetId"]),
                "asset_name": d["AssetName"],
                "plant_code": d.get("PlantCode", ""),
            })
        return results
    except Exception as e:
        logger.error("  [ASSETS] Error: %s", e)
        return []
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# 5. Operational Hours
# ---------------------------------------------------------------------------

def fetch_operational_hours(plant_id: str, month: int, year: int) -> dict:
    """
    Fetch operational hours per asset for a plant/month.

    Queries OperationalHours joined to PowerGenerationAssets filtered by plant.

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
            SELECT a.AssetName, oh.OperationalHours
            FROM {T.OPERATIONAL_HOURS} oh
            JOIN {T.POWER_GENERATION_ASSETS} a ON a.AssetId = oh.Asset_FK_Id
            WHERE oh.FinancialMonthId = ?
              AND a.{T.PGA_PLANT_FK}  = ?
            ORDER BY a.AssetName
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
# 6. Asset Availability (min/max capacity, priority)
# ---------------------------------------------------------------------------

def fetch_asset_availability(plant_id: str, month: int, year: int) -> list:
    """
    Fetch monthly asset availability (priority, min/max MW) for a plant.

    Queries AssetAvailability joined to PowerGenerationAssets.

    Returns:
        List of dicts:
        {
            "asset_id":       str,
            "asset_name":     str,
            "priority":       int,
            "min_capacity_mw":float,
            "max_capacity_mw":float,
            "is_available":   bool,
            "hours":          float,
        }
    """
    fym_id = fetch_financial_year_month_id(month, year)
    if not fym_id:
        return []

    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT
                a.AssetId,
                a.AssetName,
                aa.Priority,
                aa.minOperatingCapacity,
                aa.MaxOperatingCapacity,
                aa.isAssetAvailable,
                aa.operationalHours
            FROM {T.ASSET_AVAILABILITY} aa
            JOIN {T.POWER_GENERATION_ASSETS} a ON a.AssetId = aa.assetId
            WHERE aa.financialYearMonthId = ?
              AND a.{T.PGA_PLANT_FK}      = ?
            ORDER BY aa.Priority ASC
            """,
            (fym_id, plant_id),
        )
        results = []
        for row in cur.fetchall():
            results.append({
                "asset_id":        str(row[0]),
                "asset_name":      row[1],
                "priority":        int(row[2]) if row[2] is not None else 99,
                "min_capacity_mw": float(row[3]) if row[3] is not None else 0.0,
                "max_capacity_mw": float(row[4]) if row[4] is not None else 0.0,
                "is_available":    bool(row[5]),
                "hours":           float(row[6]) if row[6] is not None else 0.0,
            })
        return results
    except Exception as e:
        logger.error("  [AVAIL] Error: %s", e)
        return []
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# 7. Import Power (multi-source: CPPImportPower* tables)
# ---------------------------------------------------------------------------

def fetch_import_power_sources(plant_id: str, financial_year: str) -> list:
    """
    Fetch all active import power sources for a plant.

    Args:
        plant_id:       CPP plant UUID
        financial_year: e.g. "2025-26"

    Returns:
        [{"id": str, "source_name": str}, ...]
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
    Fetch total import power (MWh) aggregated across all sources for a plant/month.

    Workflow:
        1. Get all active sources for the plant (CPPImportPowerSourceMapping)
        2. Get capacity MW per source (CPPImportPowerCapacity)
        3. Get operational hours per source (CPPImportPowerOperationalHours)
        4. MWh = Capacity × Hours per source, then SUM

    Returns:
        {
            "success":          bool,
            "total_mwh":        float,
            "source_count":     int,
            "per_source": [
                {
                    "source_name":  str,
                    "capacity_mw":  float,
                    "hours":        float,
                    "mwh":          float,
                }, ...
            ],
            "message": str,
        }
    """
    fy_start = year if month >= 4 else year - 1
    fy_end   = str(fy_start + 1)[-2:]
    fy       = f"{fy_start}-{fy_end}"
    mcol     = _MONTH_COL_TITLE.get(month, "Jan")

    sources = fetch_import_power_sources(plant_id, fy)
    if not sources:
        return {
            "success": True, "total_mwh": 0.0, "source_count": 0,
            "per_source": [], "message": "No import power sources found",
        }

    source_ids   = [s["id"] for s in sources]
    placeholders = ",".join(["?"] * len(source_ids))

    conn = get_connection()
    cur  = conn.cursor()
    try:
        # Capacity
        cur.execute(
            f"""
            SELECT ImportPowerSource_FK_Id, [{mcol}]
            FROM {T.CPP_IMPORT_POWER_CAPACITY}
            WHERE ImportPowerSource_FK_Id IN ({placeholders})
              AND FinancialYear = ?
            """,
            source_ids + [fy],
        )
        cap_map = {str(r[0]): float(r[1]) if r[1] else 0.0 for r in cur.fetchall()}

        # Hours
        cur.execute(
            f"""
            SELECT ImportPowerSource_FK_Id, [{mcol}]
            FROM {T.CPP_IMPORT_POWER_OPERATIONAL_HOURS}
            WHERE ImportPowerSource_FK_Id IN ({placeholders})
              AND FinancialYear = ?
            """,
            source_ids + [fy],
        )
        hrs_map = {str(r[0]): float(r[1]) if r[1] else 0.0 for r in cur.fetchall()}

        per_source = []
        total_mwh  = 0.0
        for s in sources:
            sid  = s["id"]
            cap  = cap_map.get(sid, 0.0)
            hrs  = hrs_map.get(sid, 0.0)
            mwh  = cap * hrs
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
# 8. Norms (NormsMonthDetail / NormsHeader)
# ---------------------------------------------------------------------------

def fetch_norms(plant_id: str, month: int, year: int) -> list:
    """
    Fetch all active norms for a plant/month.

    Returns:
        List of dicts with keys:
            NormHeaderId, PlantName, UtilityName, UtilityUOM,
            MaterialName, IssuingUOM, NormParameterName,
            NormParameterUOM, NormValue, Quantity, Generation,
            GenerationUOM, Amount, Price, Month, Year
    """
    conn = get_connection()
    cur  = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT
                nh.Id              AS NormHeaderId,
                p.Name             AS PlantName,
                p.Name             AS PlantDisplayName,
                nh.UtilityName,
                nh.UtilityId,
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
                nh.DisplayOrder    AS HeaderDisplayOrder,
                nmd.DisplayOrder   AS DetailDisplayOrder,
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
        cols    = [c[0] for c in cur.description]
        return [dict(zip(cols, row)) for row in cur.fetchall()]
    except Exception as e:
        logger.error("  [NORMS] Error: %s", e)
        if _is_schema_error(e):
            raise DataFetchError("NORMS", e)
        return []
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# 9. STG Extraction Lookup
# ---------------------------------------------------------------------------

def fetch_stg_extraction_lookup(plant_id: str, month: int = None, year: int = None) -> pd.DataFrame:
    """
    Fetch STG extraction lookup table (load vs LP/MP extraction & SHP inlet).

    Filters by plant. Optionally filters by financial year.

    Returns:
        DataFrame with columns:
            LoadMW, LPExtractionTPH, MPExtractionTPH,
            SHPInletTPH, CondensateM3hr, FreeSteamFactor
        Sorted ascending by LoadMW.
    """
    _STG_COLS = ["LoadMW", "LPExtractionTPH", "MPExtractionTPH",
                 "SHPInletTPH", "CondensateM3hr", "FreeSteamFactor"]

    fy = _fy_string(month, year) if (month and year) else None

    if USE_DUMMY.get(T.CPP_STG_EXTRACTION_LOOKUP):
        logger.debug("  [STG LOOKUP] Using dummy fallback (table not in DB)")
        dummy = _load_dummy_schema()
        plant_key = plant_id.upper()
        for k, v in dummy.get("stg_extraction_lookup", {}).items():
            if k.upper() == plant_key:
                return pd.DataFrame(v, columns=_STG_COLS)
        return pd.DataFrame()

    conn = get_connection()
    cur  = conn.cursor()
    try:
        if fy:
            cur.execute(
                f"""
                SELECT LoadMW, LPExtractionTPH, MPExtractionTPH,
                       SHPInletTPH, CondensateM3hr, FreeSteamFactor
                FROM {T.CPP_STG_EXTRACTION_LOOKUP}
                WHERE CPPPlant_FK_Id = ?
                  AND FinancialYear  = ?
                ORDER BY LoadMW ASC
                """,
                (plant_id, fy),
            )
        else:
            cur.execute(
                f"""
                SELECT LoadMW, LPExtractionTPH, MPExtractionTPH,
                       SHPInletTPH, CondensateM3hr, FreeSteamFactor
                FROM {T.CPP_STG_EXTRACTION_LOOKUP}
                WHERE CPPPlant_FK_Id = ?
                ORDER BY LoadMW ASC
                """,
                (plant_id,),
            )

        rows = cur.fetchall()
        if not rows:
            return pd.DataFrame()
        return pd.DataFrame([list(r) for r in rows], columns=_STG_COLS)
    except Exception as e:
        logger.error("  [STG LOOKUP] Error: %s", e)
        if _is_schema_error(e):
            raise DataFetchError("STG LOOKUP", e)
        return pd.DataFrame()
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# 10. HRSG Heat Rate Lookup
# ---------------------------------------------------------------------------

def fetch_hrsg_heat_rate_lookup(plant_id: str, month: int = None, year: int = None) -> pd.DataFrame:
    """
    Fetch HRSG heat rate lookup records for a plant.

    Optionally filters by financial year string.

    Returns:
        DataFrame with columns:
            HRSGName, LoadTPH, HeatRateBTUlb, NGNormMMBTUMT, FinancialYear
        Sorted by HRSGName, LoadTPH.
    """
    fy = _fy_string(month, year) if (month and year) else None

    conn = get_connection()
    cur  = conn.cursor()
    try:
        if fy:
            cur.execute(
                f"""
                SELECT h.AssetName AS HRSGName, h.{T.HRSG_HR_LOAD_COL} AS LoadTPH,
                       h.{T.HRSG_HR_VALUE_COL} AS HeatRateBTUlb, h.{T.HRSG_HR_YEAR_COL}
                FROM {T.CPP_HRSG_HEAT_RATE} h
                JOIN {T.CPP_STEAM_GENERATION_ASSET} a ON a.AssetId = h.{T.HRSG_HR_ASSET_FK}
                WHERE a.{T.SGA_PLANT_FK}          = ?
                  AND h.{T.HRSG_HR_YEAR_COL}      = ?
                ORDER BY h.AssetName, h.{T.HRSG_HR_LOAD_COL} ASC
                """,
                (plant_id, fy),
            )
        else:
            cur.execute(
                f"""
                SELECT h.AssetName AS HRSGName, h.{T.HRSG_HR_LOAD_COL} AS LoadTPH,
                       h.{T.HRSG_HR_VALUE_COL} AS HeatRateBTUlb, h.{T.HRSG_HR_YEAR_COL}
                FROM {T.CPP_HRSG_HEAT_RATE} h
                JOIN {T.CPP_STEAM_GENERATION_ASSET} a ON a.AssetId = h.{T.HRSG_HR_ASSET_FK}
                WHERE a.{T.SGA_PLANT_FK} = ?
                ORDER BY h.AssetName, h.{T.HRSG_HR_LOAD_COL} ASC
                """,
                (plant_id,),
            )

        rows = cur.fetchall()
        if not rows:
            return pd.DataFrame()

        cols = ["HRSGName", "LoadTPH", "HeatRateBTUlb", "FinancialYear"]
        return pd.DataFrame([list(r) for r in rows], columns=cols)
    except Exception as e:
        logger.error("  [HRSG HR] Error: %s", e)
        if _is_schema_error(e):
            raise DataFetchError("HRSG HR", e)
        return pd.DataFrame()
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# 11. HRSG Availability & Configuration
# ---------------------------------------------------------------------------

def fetch_hrsg_availability(plant_id: str, month: int, year: int) -> list:
    """
    Fetch HRSG availability and capacity config for a plant/month.

    Returns:
        List of dicts:
        {
            "hrsg_name":      str,
            "linked_gt":      str,
            "priority":       int,
            "is_available":   bool,
            "hours":          float,
            "min_capacity_mt_hr": float,
            "max_capacity_mt_hr": float,
        }
    """
    if USE_DUMMY.get(T.HRSG_AVAILABILITY):
        logger.debug("  [HRSG AVAIL] Using dummy fallback (table not in DB)")
        dummy = _load_dummy_schema()
        plant_key = plant_id.upper()
        for k, v in dummy.get("hrsg_availability", {}).items():
            if k.upper() == plant_key:
                return v
        return []

    fym_id = fetch_financial_year_month_id(month, year)
    if not fym_id:
        return []

    conn = get_connection()
    cur  = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT
                h.HRSGName,
                h.LinkedGTName,
                ha.Priority,
                ha.IsAvailable,
                ha.OperationalHours,
                ha.MinCapacityMThr,
                ha.MaxCapacityMThr
            FROM {T.HRSG_AVAILABILITY} ha
            JOIN {T.HRSG_ASSETS} h ON h.Id = ha.HRSG_FK_Id
            WHERE ha.FinancialYearMonth_FK_Id = ?
              AND h.CPPPlant_FK_Id            = ?
            ORDER BY ha.Priority ASC
            """,
            (fym_id, plant_id),
        )
        results = []
        for row in cur.fetchall():
            results.append({
                "hrsg_name":          row[0],
                "linked_gt":          row[1],
                "priority":           int(row[2]) if row[2] is not None else 99,
                "is_available":       bool(row[3]),
                "hours":              float(row[4]) if row[4] is not None else 0.0,
                "min_capacity_mt_hr": float(row[5]) if row[5] is not None else 0.0,
                "max_capacity_mt_hr": float(row[6]) if row[6] is not None else 0.0,
            })
        return results
    except Exception as e:
        logger.error("  [HRSG AVAIL] Error: %s", e)
        if _is_schema_error(e):
            raise DataFetchError("HRSG AVAIL", e)
        return []
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# 12. GT Heat Rate Lookup (Free Steam Factor)
# ---------------------------------------------------------------------------

def fetch_gt_heat_rate_lookup(plant_id: str, month: int = None, year: int = None) -> pd.DataFrame:
    """
    Fetch GT heat rate lookup (load MW vs heat rate KCAL/KWH and free steam factor).

    Returns:
        DataFrame with columns:
            GTName, LoadMW, HeatRateKCALKWH, FreeSteamFactor, FinancialYear
    """
    fy = _fy_string(month, year) if (month and year) else None

    conn = get_connection()
    cur  = conn.cursor()
    try:
        if fy:
            cur.execute(
                f"""
                SELECT g.AssetName AS GTName, g.{T.GT_HR_LOAD_COL} AS LoadMW,
                       g.{T.GT_HR_VALUE_COL} AS HeatRateKCALKWH, g.FreeSteamFactor, g.{T.GT_HR_YEAR_COL}
                FROM {T.CPP_GT_HEAT_RATE} g
                JOIN {T.POWER_GENERATION_ASSETS} a ON a.AssetId = g.{T.GT_HR_ASSET_FK}
                WHERE a.{T.PGA_PLANT_FK}       = ?
                  AND g.{T.GT_HR_YEAR_COL}     = ?
                ORDER BY g.AssetName, g.{T.GT_HR_LOAD_COL} ASC
                """,
                (plant_id, fy),
            )
        else:
            cur.execute(
                f"""
                SELECT g.AssetName AS GTName, g.{T.GT_HR_LOAD_COL} AS LoadMW,
                       g.{T.GT_HR_VALUE_COL} AS HeatRateKCALKWH, g.FreeSteamFactor, g.{T.GT_HR_YEAR_COL}
                FROM {T.CPP_GT_HEAT_RATE} g
                JOIN {T.POWER_GENERATION_ASSETS} a ON a.AssetId = g.{T.GT_HR_ASSET_FK}
                WHERE a.{T.PGA_PLANT_FK} = ?
                ORDER BY g.AssetName, g.{T.GT_HR_LOAD_COL} ASC
                """,
                (plant_id,),
            )

        rows = cur.fetchall()
        if not rows:
            return pd.DataFrame()

        cols = ["GTName", "LoadMW", "HeatRateKCALKWH", "FreeSteamFactor", "FinancialYear"]
        return pd.DataFrame([list(r) for r in rows], columns=cols)
    except Exception as e:
        logger.error("  [GT HR] Error: %s", e)
        if _is_schema_error(e):
            raise DataFetchError("GT HR", e)
        return pd.DataFrame()
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# Interpolation helpers (mirror PPPython-script power_asset_queries.py)
# ---------------------------------------------------------------------------

BTU_LB_TO_MMBTU_MT = 0.00396567  # 1 kcal/kg = 3.96567 BTU/lb


def get_stg_extraction_for_load(
    stg_load_mw: float,
    lookup_df: pd.DataFrame = None,
    plant_id: str = None,
    month: int = None,
    year: int = None,
) -> dict:
    """
    Interpolate LP/MP extraction rates for a given STG load.

    Args:
        stg_load_mw: STG load in MW
        lookup_df:   Pre-fetched DataFrame from fetch_stg_extraction_lookup (optional)
        plant_id:    Required if lookup_df not provided
        month/year:  Required if lookup_df not provided

    Returns:
        {
            lp_extraction_tph, mp_extraction_tph, shp_inlet_tph,
            condensate_m3hr, free_steam_factor, load_mw,
            interpolated: bool
        }
    """
    if lookup_df is None or lookup_df.empty:
        if plant_id is None:
            return {"error": "lookup_df or plant_id required"}
        lookup_df = fetch_stg_extraction_lookup(plant_id, month, year)

    empty_result = {
        "lp_extraction_tph": 0.0, "mp_extraction_tph": 0.0,
        "shp_inlet_tph": 0.0, "condensate_m3hr": 0.0,
        "free_steam_factor": 0.0, "load_mw": 0.0, "interpolated": False,
    }

    if lookup_df.empty:
        return empty_result

    if stg_load_mw <= 0:
        return {**empty_result, "load_mw": 0.0}

    min_load = lookup_df["LoadMW"].min()
    max_load = lookup_df["LoadMW"].max()

    def _row_to_dict(row, load):
        return {
            "lp_extraction_tph": float(row.get("LPExtractionTPH", 0)),
            "mp_extraction_tph": float(row.get("MPExtractionTPH", 0)),
            "shp_inlet_tph":     float(row.get("SHPInletTPH", 0)),
            "condensate_m3hr":   float(row.get("CondensateM3hr", 0)),
            "free_steam_factor": float(row.get("FreeSteamFactor", 0)),
            "load_mw":           float(load),
            "interpolated":      False,
        }

    if stg_load_mw <= min_load:
        return _row_to_dict(lookup_df[lookup_df["LoadMW"] == min_load].iloc[0], min_load)
    if stg_load_mw >= max_load:
        return _row_to_dict(lookup_df[lookup_df["LoadMW"] == max_load].iloc[0], max_load)

    exact = lookup_df[lookup_df["LoadMW"] == stg_load_mw]
    if not exact.empty:
        return _row_to_dict(exact.iloc[0], stg_load_mw)

    lower = lookup_df[lookup_df["LoadMW"] < stg_load_mw].iloc[-1]
    upper = lookup_df[lookup_df["LoadMW"] > stg_load_mw].iloc[0]
    f = (stg_load_mw - lower["LoadMW"]) / (upper["LoadMW"] - lower["LoadMW"])

    def _interp(col):
        return float(lower[col]) + f * (float(upper[col]) - float(lower[col]))

    return {
        "lp_extraction_tph": _interp("LPExtractionTPH"),
        "mp_extraction_tph": _interp("MPExtractionTPH"),
        "shp_inlet_tph":     _interp("SHPInletTPH"),
        "condensate_m3hr":   _interp("CondensateM3hr"),
        "free_steam_factor": _interp("FreeSteamFactor"),
        "load_mw":           stg_load_mw,
        "interpolated":      True,
    }


def get_hrsg_heat_rate_for_load(
    hrsg_name: str,
    hrsg_load_tph: float,
    lookup_df: pd.DataFrame = None,
    plant_id: str = None,
    month: int = None,
    year: int = None,
) -> dict:
    """
    Interpolate HRSG heat rate and NG norm for a given steam load.

    Args:
        hrsg_name:     HRSG identifier (e.g. 'HRSG1')
        hrsg_load_tph: Steam flow in TPH
        lookup_df:     Pre-fetched DataFrame from fetch_hrsg_heat_rate_lookup (optional)
        plant_id:      Required if lookup_df not provided

    Returns:
        {
            hrsg_name, heat_rate_btu_lb, ng_norm_mmbtu_mt,
            hrsg_load_tph, interpolated: bool
        }
    """
    if lookup_df is None or lookup_df.empty:
        if plant_id is None:
            return {"error": "lookup_df or plant_id required"}
        lookup_df = fetch_hrsg_heat_rate_lookup(plant_id, month, year)

    empty_result = {
        "hrsg_name": hrsg_name, "heat_rate_btu_lb": 0.0,
        "ng_norm_mmbtu_mt": 0.0, "hrsg_load_tph": hrsg_load_tph, "interpolated": False,
    }

    if lookup_df.empty or hrsg_load_tph <= 0:
        return empty_result

    norm_name = hrsg_name.upper().replace("-", "").replace(" ", "")
    hrsg_df = lookup_df[
        lookup_df["HRSGName"].astype(str)
        .str.upper().str.replace("-", "", regex=False).str.replace(" ", "", regex=False)
        == norm_name
    ]

    if hrsg_df.empty:
        return {**empty_result, "error": f"No data for {hrsg_name}"}

    min_load = hrsg_df["LoadTPH"].min()
    max_load = hrsg_df["LoadTPH"].max()

    def _make(hr, load):
        return {
            "hrsg_name": hrsg_name,
            "heat_rate_btu_lb": float(hr),
            "ng_norm_mmbtu_mt": round(float(hr) * BTU_LB_TO_MMBTU_MT, 7),
            "hrsg_load_tph": float(load),
            "interpolated": False,
        }

    if hrsg_load_tph <= min_load:
        return _make(hrsg_df[hrsg_df["LoadTPH"] == min_load].iloc[0]["HeatRateBTUlb"], min_load)
    if hrsg_load_tph >= max_load:
        return _make(hrsg_df[hrsg_df["LoadTPH"] == max_load].iloc[0]["HeatRateBTUlb"], max_load)

    exact = hrsg_df[hrsg_df["LoadTPH"] == hrsg_load_tph]
    if not exact.empty:
        return _make(exact.iloc[0]["HeatRateBTUlb"], hrsg_load_tph)

    lower = hrsg_df[hrsg_df["LoadTPH"] < hrsg_load_tph].iloc[-1]
    upper = hrsg_df[hrsg_df["LoadTPH"] > hrsg_load_tph].iloc[0]
    f = (hrsg_load_tph - lower["LoadTPH"]) / (upper["LoadTPH"] - lower["LoadTPH"])
    hr = float(lower["HeatRateBTUlb"]) + f * (float(upper["HeatRateBTUlb"]) - float(lower["HeatRateBTUlb"]))

    return {
        "hrsg_name": hrsg_name,
        "heat_rate_btu_lb": round(hr, 4),
        "ng_norm_mmbtu_mt": round(hr * BTU_LB_TO_MMBTU_MT, 7),
        "hrsg_load_tph": hrsg_load_tph,
        "interpolated": True,
    }


def calculate_hrsg_ng_from_heat_rate(
    hrsg_name: str,
    shp_production_mt: float,
    operational_hours: float,
    lookup_df: pd.DataFrame = None,
    plant_id: str = None,
    month: int = None,
    year: int = None,
) -> dict:
    """
    Calculate HRSG Natural Gas consumption using heat rate lookup.

    Formula:
        steam_flow_tph = shp_production_mt / operational_hours
        ng_norm (MMBTU/MT) = heat_rate (BTU/lb) × 0.00396567
        ng_qty  (MMBTU)    = shp_production_mt × ng_norm

    Returns:
        {
            hrsg_name, shp_production_mt, operational_hours,
            steam_flow_tph, heat_rate_btu_lb, ng_norm_mmbtu_mt,
            ng_quantity_mmbtu, interpolated: bool
        }
    """
    steam_flow_tph = shp_production_mt / operational_hours if operational_hours > 0 else 0.0

    hr_result = get_hrsg_heat_rate_for_load(
        hrsg_name, steam_flow_tph, lookup_df, plant_id, month, year
    )
    heat_rate = hr_result.get("heat_rate_btu_lb", 0.0)
    ng_norm   = hr_result.get("ng_norm_mmbtu_mt", 0.0)

    return {
        "hrsg_name":          hrsg_name,
        "shp_production_mt":  round(shp_production_mt, 2),
        "operational_hours":  round(operational_hours, 2),
        "steam_flow_tph":     round(steam_flow_tph, 4),
        "heat_rate_btu_lb":   heat_rate,
        "ng_norm_mmbtu_mt":   ng_norm,
        "ng_quantity_mmbtu":  round(shp_production_mt * ng_norm, 2),
        "interpolated":       hr_result.get("interpolated", False),
    }


def fetch_complete_asset_data(plant_id: str, month: int, year: int) -> dict:
    """
    Fetch combined asset data: base info + operational hours + availability.

    Mirrors PPPython-script's fetch_complete_asset_data.

    Returns:
        {
            success, month, year, fym_id,
            assets: list (all assets),
            available_assets: list (hours > 0, sorted by priority),
            available_count, unavailable_count
        }
    """
    fym_id = fetch_financial_year_month_id(month, year)
    if not fym_id:
        return {
            "success": False, "message": f"FinancialYearMonth not found for {month}/{year}",
            "month": month, "year": year, "assets": [],
            "available_assets": [], "available_count": 0, "unavailable_count": 0,
        }

    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT
                p.AssetId,
                p.AssetName,
                p.PlantCode,
                p.{T.PGA_PLANT_FK},
                COALESCE(oh.OperationalHours, 0)    AS OperationalHours,
                aa.Priority,
                aa.minOperatingCapacity             AS MinOperatingCapacity,
                aa.MaxOperatingCapacity,
                aa.FixedMin,
                aa.FixedMax
            FROM {T.POWER_GENERATION_ASSETS} p WITH(NOLOCK)
            LEFT JOIN {T.OPERATIONAL_HOURS} oh WITH(NOLOCK)
                   ON oh.Asset_FK_Id = p.AssetId AND oh.FinancialMonthId = ?
            LEFT JOIN {T.ASSET_AVAILABILITY} aa
                   ON aa.assetId = p.AssetId AND aa.financialYearMonthId = ?
            WHERE p.{T.PGA_PLANT_FK} = ?
            ORDER BY aa.Priority, p.AssetName
            """,
            (fym_id, fym_id, plant_id),
        )

        assets = []
        available_assets = []
        available_count = 0
        unavailable_count = 0

        for row in cur.fetchall():
            op_hours = float(row[4]) if row[4] else 0.0
            is_avail = op_hours > 0
            if is_avail:
                available_count += 1
            else:
                unavailable_count += 1

            asset = {
                "AssetId":             str(row[0]) if row[0] else None,
                "AssetName":           row[1],
                "PlantCode":           row[2],
                "CPPPLANT_FK_Id":      str(row[3]) if row[3] else None,
                "OperationalHours":    op_hours,
                "IsAvailable":         is_avail,
                "Priority":            int(row[5]) if row[5] is not None else None,
                "MinOperatingCapacity":float(row[6]) if row[6] is not None else None,
                "MaxOperatingCapacity":float(row[7]) if row[7] is not None else None,
                "FixedMin":            float(row[8]) if row[8] is not None else None,
                "FixedMax":            float(row[9]) if row[9] is not None else None,
            }
            assets.append(asset)
            if is_avail:
                available_assets.append(asset)

        available_assets.sort(key=lambda x: (
            x["Priority"] if x["Priority"] is not None else 999,
            x.get("AssetName", ""),
        ))

        return {
            "success":          True,
            "month":            month,
            "year":             year,
            "fym_id":           fym_id,
            "assets":           assets,
            "available_assets": available_assets,
            "available_count":  available_count,
            "unavailable_count":unavailable_count,
        }
    except Exception as e:
        return {
            "success": False, "message": str(e),
            "month": month, "year": year, "assets": [],
            "available_assets": [], "available_count": 0, "unavailable_count": 0,
        }
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# 13. Convenience: fetch everything for a plant/month in one call
# ---------------------------------------------------------------------------

def fetch_all_plant_data(plant_id: str, month: int, year: int) -> dict:
    """
    Fetch all data needed for budget calculation for a plant/month.

    Calls all individual fetch functions and returns a single dict.

    Returns:
        {
            "plant_id":           str,
            "month":              int,
            "year":               int,
            "process_demands":    dict,
            "fixed_consumption":  dict,
            "assets":             list,
            "operational_hours":  dict,
            "asset_availability": list,
            "import_power":       dict,
            "norms":              list,
            "hrsg_availability":  list,
            "stg_lookup":         pd.DataFrame,
            "hrsg_heat_rate":     pd.DataFrame,
            "gt_heat_rate":       pd.DataFrame,
        }
    """
    logger.info("\n[FETCH ALL] plant=%s  month=%d/%d", plant_id, month, year)

    stg_df  = fetch_stg_extraction_lookup(plant_id, month, year)
    hrsg_df = fetch_hrsg_heat_rate_lookup(plant_id, month, year)
    gt_df   = fetch_gt_heat_rate_lookup(plant_id, month, year)

    return {
        "plant_id":           plant_id,
        "month":              month,
        "year":               year,
        "process_demands":    fetch_process_demands(plant_id, month, year),
        "fixed_consumption":  fetch_fixed_consumption(plant_id, month, year),
        "assets":             fetch_power_generation_assets(plant_id),
        "operational_hours":  fetch_operational_hours(plant_id, month, year),
        "asset_availability": fetch_asset_availability(plant_id, month, year),
        "complete_assets":    fetch_complete_asset_data(plant_id, month, year),
        "import_power":       fetch_import_power(plant_id, month, year),
        "norms":              fetch_norms(plant_id, month, year),
        "hrsg_availability":  fetch_hrsg_availability(plant_id, month, year),
        "stg_lookup":         stg_df,
        "hrsg_heat_rate":     hrsg_df,
        "gt_heat_rate":       gt_df,
    }
