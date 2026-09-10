"""
JMD SEZ-PCG Norms Import Script (ODS Source)
=============================================
Imports Norm/Quantity/Amount/Price data for JMD SEZ-PCG plants from the ODS file
"SEZ-PCG_JMD.ods" (OpenDocument Spreadsheet) into:
    Plants -> NormParameters -> NormsHeader -> NormsMonthDetail
                                        -> CPPNorms
                                        -> CPPMonthWisePrice

Usage:
    python scripts/import_sez_pcg_norms_ods.py                 # dry run (default)
    python scripts/import_sez_pcg_norms_ods.py --execute        # actually insert

Phase 1 (dry run, DRY_RUN=True):
    - Reads and validates the ODS data.
    - Compares against current DB state (Plants, NormParameters, NormsHeader,
      NormsMonthDetail, CPPNorms, CPPMonthWisePrice).
    - Prints a validation report.
    - Writes SELECT check queries and generated INSERT statements to a .sql file.
    - Makes NO changes to the database.

Phase 2 (real run, DRY_RUN=False, requires --execute):
    - Re-validates, then inserts new records inside a single transaction.
    - Rolls back on any error.
    - Skips anything that already exists (no duplicate inserts).
    - On commit, writes a manifest of every inserted Id to scripts/manifests/.
"""

import argparse
import json
import logging
import os
import sys
import uuid
from datetime import datetime

import pandas as pd

# Add JMD app directory to path for database connection
JMD_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, JMD_DIR)
from database.connection import get_connection

# ============================================================
# CONFIG
# ============================================================
ODS_PATH = os.path.join(JMD_DIR, "..", "files", "SEZ-PCG_JMD.ods")

# SEZ-PCG has only one plant in the ODS.
ALLOWED_PLANTS = [
    "RIL-JW Plant-SEZ PCG",
]

# Sibling plant whose NormParameters template fields (Type, NormParameterType_FK_Id,
# IsHistorical, DisplayOrder, IsEditable, IsVisible, CalculationType) are copied
# when creating new NormParameters for plants that don't yet have any.
SIBLING_PLANT_NAME_FOR_NEW_ROWS = "RIL-JW Plant-SEZ PCG"

# No new Plants rows needed - the SEZ-PCG plant already exists in the DB.
NEW_PLANTS_TO_CREATE = {}

# SEZ-PCG-CPP parent plant UUID - used as NormType reference.
# Its NormParameters have NormType_FK_Id set (1=Production, 2=Consumption),
# which is used to assign NormType to new NormParameters created from the ODS.
NORM_TYPE_REFERENCE_PLANT_ID = "D2C7FBAD-7E00-4642-B3B2-5A768FAC8D45"

# SEZ-PCG ODS layout (DIFFERENT from DTA/SEZ ODS):
#   - Row 0: Month names (April..March) in cols 11+
#   - Row 1: Column headers (Generating Plant, ..., UOM, Norms, Quantity, Amount, Price)
#   - Row 2+: Data rows
#   - No FY label in the ODS - hardcode FY 2026-27
#   - Month block width = 4: Norms, Quantity, Amount (Rs.), Price
HEADER_ROW_IDX = 1
DATA_START_ROW_IDX = 2
MONTH_NAME_ROW_IDX = 0
COL_UTILITY_PLANT = 0
COL_UTILITY_PLANT_ID = 1
COL_UTILITY = 2
COL_UTILITY_ID = 3
COL_UTILITY_UOM = 4
COL_ACCOUNT = 5
COL_MATERIAL = 6
COL_MATERIAL_ID = 7
COL_ISSUING_PLANT = 8
COL_ISSUING_PLANT_ID = 9
COL_ISSUING_UOM = 10
FIRST_MONTH_COL = 11
MONTH_BLOCK_WIDTH = 4
NUM_MONTHS = 12

# Hardcoded FY since SEZ-PCG ODS has no FY label
FY_START_YEAR = 2026
FINANCIAL_YEAR = "2026-27"

_MONTH_NAME_TO_NUM = {
    "January": 1, "February": 2, "March": 3, "April": 4,
    "May": 5, "June": 6, "July": 7, "August": 8,
    "September": 9, "October": 10, "November": 11, "December": 12,
}

CPP_MONTH_COLUMN = {
    4: "Apr_Norms", 5: "May_Norms", 6: "Jun_Norms", 7: "Jul_Norms",
    8: "Aug_Norms", 9: "Sep_Norms", 10: "Oct_Norms", 11: "Nov_Norms",
    12: "Dec_Norms", 1: "Jan_Norms", 2: "Feb_Norms", 3: "Mar_Norms",
}

CPP_PRICE_COLUMN = {
    4: "Apr_Price", 5: "May_Price", 6: "Jun_Price", 7: "Jul_Price",
    8: "Aug_Price", 9: "Sep_Price", 10: "Oct_Price", 11: "Nov_Price",
    12: "Dec_Price", 1: "Jan_Price", 2: "Feb_Price", 3: "Mar_Price",
}

# CPPNorms.NormType_FK_Id: 6 = "Fixed" (AOP/budget-sourced norms).
CPP_NORM_TYPE_FIXED = 6

OUTPUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "output")
SQL_OUTPUT_FILE = os.path.join(OUTPUT_DIR, f"jmd_sez_pcg_norms_ods_import_{datetime.now():%Y%m%d_%H%M%S}.sql")
MANIFEST_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "manifests")
MANIFEST_FILE = os.path.join(MANIFEST_DIR, f"jmd_sez_pcg_norms_ods_import_{datetime.now():%Y%m%d_%H%M%S}.json")
DRY_RUN = True

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("jmd_sez_pcg_norms_ods_import")


# ============================================================
# Helpers
# ============================================================
def _clean_str(val) -> str:
    if pd.isna(val):
        return ""
    s = str(val).strip()
    return "" if s == "-" else s


def _clean_sap_code(val):
    if pd.isna(val):
        return None
    s = str(val).strip()
    return None if s == "" or s == "-" else s


def _to_float(val):
    if pd.isna(val):
        return None
    if isinstance(val, str):
        val = val.replace(",", "").strip()
        if val == "" or val == "-":
            return None
    try:
        return float(val)
    except (TypeError, ValueError):
        return None


def _all_blank(values) -> bool:
    return all(pd.isna(v) for v in values)


def _na_to_none(val):
    if val is None or (isinstance(val, float) and pd.isna(val)):
        return None
    return val


def _financial_year_label(fy_start_year: int) -> str:
    return f"{fy_start_year}-{str(fy_start_year + 1)[-2:]}"


def _month_name_to_num_year(month_name: str, fy_start_year: int) -> tuple:
    """'April' + 2026 -> (4, 2026); 'January' + 2026 -> (1, 2027)."""
    month_num = _MONTH_NAME_TO_NUM[month_name.strip()]
    cal_year = fy_start_year if month_num >= 4 else fy_start_year + 1
    return month_num, cal_year


def resolve_fiscal_year(month_num: int, calendar_year: int) -> int:
    return calendar_year if month_num >= 4 else calendar_year - 1


# ============================================================
# STEP 1: Load ODS data
# ============================================================
def load_ods_data(path: str = ODS_PATH) -> pd.DataFrame:
    """Read ODS and rebuild as tidy long-format DataFrame."""
    logger.info(f"Reading ODS file: {path}")
    if not os.path.exists(path):
        raise FileNotFoundError(f"ODS file not found: {path}")

    raw = pd.read_excel(path, engine="odf", header=None)
    logger.info(f"Raw ODS shape: {raw.shape}")

    # SEZ-PCG ODS has no FY label - use hardcoded FY_START_YEAR
    fy_start_year = FY_START_YEAR
    logger.info(f"FY start year (hardcoded): {fy_start_year}")

    header_row = raw.iloc[HEADER_ROW_IDX]
    logger.info(f"Header row ({HEADER_ROW_IDX}) columns: {list(header_row.values)}")

    # Extract month periods from MONTH_NAME_ROW_IDX (row 0 for SEZ-PCG)
    month_periods = []
    for month_idx in range(NUM_MONTHS):
        base_col = FIRST_MONTH_COL + month_idx * MONTH_BLOCK_WIDTH
        month_name = str(raw.iloc[MONTH_NAME_ROW_IDX][base_col]).strip()
        month_num, cal_year = _month_name_to_num_year(month_name, fy_start_year)
        month_periods.append((month_num, cal_year))
    logger.info(f"Detected month periods: {month_periods}")
    validate_financial_year_periods(month_periods)

    data = raw.iloc[DATA_START_ROW_IDX:].reset_index(drop=True)
    plant_col = data[COL_UTILITY_PLANT].astype(str).str.strip()
    data = data[plant_col.isin(ALLOWED_PLANTS)].reset_index(drop=True)
    logger.info(f"Rows after filtering to allowed JMD SEZ-PCG plants: {len(data)}")

    account_col = data[COL_ACCOUNT].astype(str).str.strip()
    material_col = data[COL_MATERIAL].astype(str).str.strip()
    subtotal_mask = (account_col.str.lower() == "total") & (material_col.str.lower() == "total")
    if subtotal_mask.any():
        logger.info(f"Dropping {subtotal_mask.sum()} subtotal rows.")
        data = data[~subtotal_mask].reset_index(drop=True)

    records = []
    for _, row in data.iterrows():
        utility_name = _clean_str(row[COL_UTILITY])
        utility_id = _clean_str(row[COL_UTILITY_ID])
        identity = {
            "plant_name": _clean_str(row[COL_UTILITY_PLANT]),
            "plant_code": _clean_str(row[COL_UTILITY_PLANT_ID]),
            "utility_name": utility_name,
            "utility_id": utility_id,
            "utility_uom": _clean_str(row[COL_UTILITY_UOM]),
            "account_name": _clean_str(row[COL_ACCOUNT]),
            "material_name": _clean_str(row[COL_MATERIAL]),
            "material_id": _clean_sap_code(row[COL_MATERIAL_ID]),
            "material_uom": _clean_str(row[COL_ISSUING_UOM]),
            "issuing_plant_name": _clean_str(row[COL_ISSUING_PLANT]),
            "issuing_plant_id": _clean_str(row[COL_ISSUING_PLANT_ID]),
        }
        for month_idx, (month_num, cal_year) in enumerate(month_periods):
            base_col = FIRST_MONTH_COL + month_idx * MONTH_BLOCK_WIDTH
            norms, quantity, amount, price = (
                row[base_col],
                row[base_col + 1],
                row[base_col + 2],
                row[base_col + 3],
            )
            if _all_blank([norms, quantity, amount, price]):
                continue
            rec = dict(identity)
            rec.update({
                "month_num": month_num,
                "calendar_year": cal_year,
                "norms": _to_float(norms),
                "quantity": _to_float(quantity),
                "amount": _to_float(amount),
                "price": _to_float(price),
            })
            records.append(rec)

    df = pd.DataFrame.from_records(records)
    logger.info(f"Tidy long-format rows built: {len(df)}")
    return df


# ============================================================
# STEP 2: Validate ODS columns
# ============================================================
def validate_ods_columns(raw_header_row) -> list:
    problems = []
    expected_identity = {
        COL_UTILITY_PLANT: "Generating Plant",
        COL_UTILITY_PLANT_ID: "Generating Plant ID",
        COL_UTILITY: "Utility",
        COL_UTILITY_ID: "Utility ID",
        COL_UTILITY_UOM: "UOM",
        COL_ACCOUNT: "Account",
        COL_MATERIAL: "Material",
        COL_MATERIAL_ID: "Material ID",
        COL_ISSUING_PLANT: "Issuing Plant",
        COL_ISSUING_PLANT_ID: "Issuing Plant ID",
        COL_ISSUING_UOM: "UOM",
    }
    for idx, expected_name in expected_identity.items():
        actual = str(raw_header_row[idx]).strip()
        if expected_name.lower() not in actual.lower():
            problems.append(f"Column {idx}: expected '{expected_name}', found '{actual}'")
    for month_idx in range(NUM_MONTHS):
        base_col = FIRST_MONTH_COL + month_idx * MONTH_BLOCK_WIDTH
        for offset, metric in enumerate(["Norms", "Quantity", "Amount", "Price"]):
            actual = str(raw_header_row[base_col + offset]).strip()
            if metric.lower() not in actual.lower():
                problems.append(
                    f"Month block {month_idx} col {base_col + offset}: "
                    f"expected '{metric}', found '{actual}'"
                )
    if problems:
        logger.warning(f"Column validation found {len(problems)} issue(s):")
        for p in problems:
            logger.warning(f"  - {p}")
    else:
        logger.info("Column validation passed.")
    return problems


def validate_financial_year_periods(month_periods: list) -> None:
    fiscal_years = {resolve_fiscal_year(m, y) for m, y in month_periods}
    if len(fiscal_years) != 1:
        logger.error(
            f"ODS month columns span multiple financial years {sorted(fiscal_years)}. "
            f"Periods: {month_periods}"
        )
        return
    fy_start = next(iter(fiscal_years))
    expected = [(m, fy_start) for m in range(4, 13)] + [(m, fy_start + 1) for m in range(1, 4)]
    if month_periods != expected:
        logger.error(
            f"ODS month columns not in expected Apr-Mar order for FY "
            f"{_financial_year_label(fy_start)}. Expected: {expected}. Found: {month_periods}"
        )
        return
    logger.info(
        f"Financial year period check passed: FY {_financial_year_label(fy_start)} "
        f"covers Apr {fy_start} - Mar {fy_start + 1}."
    )


# ============================================================
# STEP 3: Database connection
# ============================================================
def get_database_connection():
    return get_connection()


# ============================================================
# STEP 4: Fetch JMD SEZ-PCG plants
# ============================================================
def fetch_jmd_plants(conn) -> tuple:
    cur = conn.cursor()
    placeholders = ",".join("?" for _ in ALLOWED_PLANTS)
    cur.execute(
        f"SELECT Id, Name, SourceName, IsActive, PlantCode FROM Plants "
        f"WHERE Name IN ({placeholders}) OR SourceName IN ({placeholders})",
        ALLOWED_PLANTS + ALLOWED_PLANTS,
    )
    rows = cur.fetchall()
    plant_map, plant_code_map = {}, {}
    for plant_id, name, source_name, is_active, plant_code in rows:
        matched_name = (
            name if name in ALLOWED_PLANTS
            else (source_name if source_name in ALLOWED_PLANTS else None)
        )
        if matched_name:
            plant_map[matched_name] = str(plant_id)
            plant_code_map[matched_name] = plant_code
    missing = [p for p in ALLOWED_PLANTS if p not in plant_map]
    if missing:
        logger.warning(f"Missing plants: {missing}")
    else:
        logger.info(f"All required JMD SEZ-PCG plants found: {plant_map}")
    return plant_map, plant_code_map


def ensure_missing_plants_exist(conn, dry_run: bool) -> dict:
    """Create Plants rows for any names in NEW_PLANTS_TO_CREATE that don't exist.

    For SEZ-PCG, NEW_PLANTS_TO_CREATE is empty (plant already exists), so
    this is a no-op that just returns empty results.
    """
    if not NEW_PLANTS_TO_CREATE:
        logger.info("No new Plants rows needed - all SEZ-PCG plants already exist.")
        return {"created": [], "already_existed": list(ALLOWED_PLANTS), "created_ids": []}
    raise NotImplementedError("Plant creation not configured for SEZ-PCG.")


# ============================================================
# STEP 5: Fetch norm parameters
# ============================================================
def fetch_norm_parameters(conn, plant_ids: dict) -> dict:
    if not plant_ids:
        return {}
    cur = conn.cursor()
    placeholders = ",".join("?" for _ in plant_ids)
    cur.execute(
        f"SELECT Id, Name, Plant_FK_Id FROM NormParameters "
        f"WHERE Plant_FK_Id IN ({placeholders})",
        list(plant_ids.values()),
    )
    param_map = {}
    for param_id, name, plant_fk in cur.fetchall():
        param_map[(str(plant_fk), (name or "").strip().upper())] = str(param_id)
    logger.info(f"Fetched {len(param_map)} plant-scoped NormParameters.")
    return param_map


def lookup_norm_parameter(param_map: dict, plant_id: str, name: str):
    return param_map.get((plant_id, (name or "").strip().upper()))


NORM_PARAMETER_TEMPLATE_FIELDS = [
    "Type", "NormParameterType_FK_Id", "IsHistorical",
    "DisplayOrder", "IsEditable", "IsVisible", "CalculationType",
]
NORM_TYPE_PRODUCTION = 1
NORM_TYPE_CONSUMPTION = 2


def fetch_norm_type_reference(conn) -> dict:
    """Build {utility_name_upper: NormType_FK_Id} from SEZ-PCG-CPP parent's NormParameters."""
    cur = conn.cursor()
    cur.execute(
        "SELECT Name, NormType_FK_Id FROM NormParameters "
        "WHERE Plant_FK_Id = ? AND NormType_FK_Id IS NOT NULL",
        (NORM_TYPE_REFERENCE_PLANT_ID,),
    )
    reference = {
        (name or "").strip().upper(): norm_type
        for name, norm_type in cur.fetchall()
    }
    logger.info(f"Built NormType reference with {len(reference)} names from SEZ-PCG-CPP.")
    return reference


def fetch_norm_parameter_template(conn, plant_ids: dict) -> dict:
    if not plant_ids:
        return {}
    cur = conn.cursor()
    placeholders = ",".join("?" for _ in plant_ids)
    cols = ", ".join(NORM_PARAMETER_TEMPLATE_FIELDS)
    cur.execute(
        f"SELECT Plant_FK_Id, {cols} FROM NormParameters "
        f"WHERE Plant_FK_Id IN ({placeholders})",
        list(plant_ids.values()),
    )
    templates = {}
    for row in cur.fetchall():
        plant_fk = str(row[0])
        templates.setdefault(plant_fk, dict(zip(NORM_PARAMETER_TEMPLATE_FIELDS, row[1:])))
    sibling_id = plant_ids.get(SIBLING_PLANT_NAME_FOR_NEW_ROWS)
    fallback_template = templates.get(sibling_id)
    for plant_name, plant_id in plant_ids.items():
        if plant_id not in templates and fallback_template:
            templates[plant_id] = fallback_template
    logger.info(f"Built NormParameters templates for {len(templates)} plant(s).")
    return templates


# ============================================================
# STEP 6: Check existing norms
# ============================================================
def check_existing_norms(conn, plant_ids: dict) -> dict:
    if not plant_ids:
        return {
            "headers": {}, "month_details": {}, "cpp_norms": {},
            "cpp_prices": {}, "max_display_order": {},
        }
    cur = conn.cursor()
    placeholders = ",".join("?" for _ in plant_ids)
    cur.execute(
        f"SELECT nh.Id, nh.Plant_FK_Id, nh.UtilityName, nh.AccountName, "
        f"nh.MaterialName, nh.IssuingPlantName, nh.UtilityUOM, nh.IssuingUOM, "
        f"nh.DisplayOrder "
        f"FROM NormsHeader nh "
        f"WHERE nh.Plant_FK_Id IN ({placeholders}) AND nh.IsActive = 1",
        list(plant_ids.values()),
    )
    headers, max_display_order = {}, {}
    for r in cur.fetchall():
        header_id, plant_fk, utility, account, material, issuing_plant, uom, issuing_uom, disp_order = r
        key = (
            str(plant_fk),
            utility.strip().upper(),
            account.strip().upper(),
            material.strip().upper(),
            (issuing_plant or "").strip().upper(),
        )
        headers[key] = str(header_id)
        max_display_order[str(plant_fk)] = max(
            max_display_order.get(str(plant_fk), 0), disp_order or 0
        )

    month_details = {}
    if headers:
        header_ids = list(headers.values())
        ph = ",".join("?" for _ in header_ids)
        cur.execute(
            f"SELECT nmd.NormsHeader_FK_Id, fym.Month, fym.Year, nmd.Id "
            f"FROM NormsMonthDetail nmd "
            f"INNER JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id "
            f"WHERE nmd.NormsHeader_FK_Id IN ({ph})",
            header_ids,
        )
        for header_fk, month, year, detail_id in cur.fetchall():
            month_details[(str(header_fk), month, year)] = str(detail_id)

    cpp_norms = {}
    if headers:
        header_ids = list(headers.values())
        ph = ",".join("?" for _ in header_ids)
        cur.execute(
            f"SELECT NormsHeader_FK_Id, FinancialYear, Id FROM CPPNorms "
            f"WHERE NormsHeader_FK_Id IN ({ph})",
            header_ids,
        )
        for header_fk, fy, cpp_id in cur.fetchall():
            cpp_norms[(str(header_fk), fy)] = str(cpp_id)

    cpp_prices = {}
    if headers:
        header_ids = list(headers.values())
        ph = ",".join("?" for _ in header_ids)
        cur.execute(
            f"SELECT NormsHeader_FK_Id, FinancialYear, Id FROM CPPMonthWisePrice "
            f"WHERE NormsHeader_FK_Id IN ({ph})",
            header_ids,
        )
        for header_fk, fy, price_id in cur.fetchall():
            cpp_prices[(str(header_fk), fy)] = str(price_id)

    logger.info(
        f"Existing records - NormsHeader: {len(headers)}, "
        f"NormsMonthDetail: {len(month_details)}, "
        f"CPPNorms: {len(cpp_norms)}, "
        f"CPPMonthWisePrice: {len(cpp_prices)}"
    )
    return {
        "headers": headers,
        "month_details": month_details,
        "cpp_norms": cpp_norms,
        "cpp_prices": cpp_prices,
        "max_display_order": max_display_order,
    }


# ============================================================
# STEP 7: Fetch FinancialYearMonth map
# ============================================================
def fetch_financial_year_months(conn) -> dict:
    cur = conn.cursor()
    cur.execute("SELECT Id, Month, Year FROM FinancialYearMonth")
    return {(month, year): str(fym_id) for fym_id, month, year in cur.fetchall()}


# ============================================================
# STEP 8: Prepare NormsHeader records
# ============================================================
def prepare_norms_header_records(
    df: pd.DataFrame,
    plant_ids: dict,
    plant_code_map: dict,
    param_map: dict,
    param_templates: dict,
    norm_type_reference: dict,
    existing: dict,
) -> tuple:
    identity_cols = [
        "plant_name", "plant_code", "utility_name", "utility_id",
        "utility_uom", "account_name", "material_name", "material_id",
        "material_uom", "issuing_plant_name", "issuing_plant_id",
    ]
    unique_headers = df[identity_cols].drop_duplicates().reset_index(drop=True)
    headers, new_params_by_key, next_display_order = (
        [], {}, dict(existing.get("max_display_order", {}))
    )
    for _, row in unique_headers.iterrows():
        plant_id = plant_ids.get(row["plant_name"])
        if not plant_id:
            continue
        material_name = row["material_name"] or row["account_name"]
        key = (
            plant_id,
            row["utility_name"].upper(),
            row["account_name"].upper(),
            material_name.upper(),
            row["issuing_plant_name"].upper(),
        )
        existing_id = existing["headers"].get(key)
        used_material_name = bool(row["material_name"])
        if used_material_name:
            norm_param_id = lookup_norm_parameter(param_map, plant_id, material_name)
        else:
            norm_param_id = lookup_norm_parameter(param_map, plant_id, row["utility_name"])
        if not norm_param_id:
            param_name = material_name if used_material_name else row["utility_name"]
            param_key = (plant_id, param_name.upper())
            new_param = new_params_by_key.get(param_key)
            if not new_param:
                template = param_templates.get(plant_id, {})
                fallback_norm_type = (
                    NORM_TYPE_CONSUMPTION if used_material_name else NORM_TYPE_PRODUCTION
                )
                norm_type = norm_type_reference.get(param_name.upper(), fallback_norm_type)
                new_param = {
                    "id": str(uuid.uuid4()).upper(),
                    "plant_id": plant_id,
                    "name": param_name,
                    "uom": row["material_uom"] or row["utility_uom"] or None,
                    "sap_material_code": _na_to_none(row["material_id"]),
                    "type": template.get("Type"),
                    "norm_parameter_type_fk_id": template.get("NormParameterType_FK_Id"),
                    "is_historical": template.get("IsHistorical"),
                    "display_order": template.get("DisplayOrder"),
                    "is_editable": template.get("IsEditable"),
                    "is_visible": template.get("IsVisible"),
                    "calculation_type": template.get("CalculationType"),
                    "norm_type_fk_id": norm_type,
                }
                new_params_by_key[param_key] = new_param
                param_map[param_key] = new_param["id"]
            norm_param_id = new_param["id"]
        if existing_id:
            header_id, is_new = existing_id, False
        else:
            header_id, is_new = str(uuid.uuid4()).upper(), True
            next_display_order[plant_id] = next_display_order.get(plant_id, 0) + 1
        headers.append({
            "id": header_id,
            "is_new": is_new,
            "plant_id": plant_id,
            "plant_name": row["plant_name"],
            "plant_code": _na_to_none(row["plant_code"]),
            "utility_name": row["utility_name"],
            "utility_id": _na_to_none(row["utility_id"]),
            "utility_uom": row["utility_uom"],
            "account_name": row["account_name"],
            "material_name": material_name,
            "material_id": _na_to_none(row["material_id"]),
            "issuing_plant_name": _na_to_none(row["issuing_plant_name"]) or None,
            "issuing_uom": _na_to_none(row["material_uom"]) or None,
            "norm_parameter_id": norm_param_id,
            "display_order": next_display_order.get(plant_id) if is_new else None,
        })
    new_norm_parameters = list(new_params_by_key.values())
    logger.info(
        f"Prepared {len(headers)} NormsHeader records "
        f"({sum(h['is_new'] for h in headers)} new, "
        f"{sum(not h['is_new'] for h in headers)} existing)."
    )
    logger.info(f"Prepared {len(new_norm_parameters)} new NormParameters records.")
    return headers, new_norm_parameters


# ============================================================
# STEP 9: Prepare NormsMonthDetail records
# ============================================================
def prepare_norms_month_detail_records(
    df: pd.DataFrame, header_lookup: dict, fym_map: dict, existing: dict, plant_ids: dict
) -> list:
    df = df.copy()
    df["_plant_id"] = df["plant_name"].map(
        lambda x: plant_ids.get(x) if plant_ids else None
    )
    details, missing_fym = [], set()
    for _, row in df.iterrows():
        plant_id = row.get("_plant_id")
        if not plant_id:
            continue
        material_name = row["material_name"] or row["account_name"]
        key = (
            plant_id,
            row["utility_name"].upper(),
            row["account_name"].upper(),
            material_name.upper(),
            row["issuing_plant_name"].upper(),
        )
        header = header_lookup.get(key)
        if not header:
            continue
        calendar_year = row["calendar_year"]
        fiscal_year = resolve_fiscal_year(row["month_num"], calendar_year)
        fym_id = fym_map.get((row["month_num"], calendar_year))
        if not fym_id:
            missing_fym.add((row["month_num"], calendar_year))
            continue
        detail_key = (header["id"], row["month_num"], calendar_year)
        existing_id = existing["month_details"].get(detail_key)
        quantity = _na_to_none(row["quantity"])
        details.append({
            "id": existing_id or str(uuid.uuid4()).upper(),
            "is_new": existing_id is None,
            "norms_header_id": header["id"],
            "fym_id": fym_id,
            "month_num": row["month_num"],
            "year": calendar_year,
            "fiscal_year": fiscal_year,
            "norms": _na_to_none(row["norms"]),
            "quantity": quantity,
            "qty": quantity,
            "amount": _na_to_none(row["amount"]),
            "price": _na_to_none(row["price"]),
        })
    if missing_fym:
        logger.warning(f"Missing FinancialYearMonth rows for: {sorted(missing_fym)}")
    logger.info(
        f"Prepared {len(details)} NormsMonthDetail records "
        f"({sum(d['is_new'] for d in details)} new, "
        f"{sum(not d['is_new'] for d in details)} existing)."
    )
    return details


# ============================================================
# STEP 10: Prepare CPPNorms records
# ============================================================
def prepare_cpp_norms_records(month_details: list, headers: list, existing: dict) -> list:
    grouped = {}
    for d in month_details:
        fy_label = _financial_year_label(d["fiscal_year"])
        grouped.setdefault((d["norms_header_id"], fy_label), {})[d["month_num"]] = d["norms"]
    records = []
    for (header_id, fy_label), month_norms in grouped.items():
        existing_id = existing["cpp_norms"].get((header_id, fy_label))
        records.append({
            "id": existing_id or str(uuid.uuid4()).upper(),
            "is_new": existing_id is None,
            "norms_header_id": header_id,
            "financial_year": fy_label,
            "aop_year": fy_label,
            "norm_type_fk_id": CPP_NORM_TYPE_FIXED,
            "months": {CPP_MONTH_COLUMN[m]: v for m, v in month_norms.items()},
        })
    logger.info(
        f"Prepared {len(records)} CPPNorms records "
        f"({sum(r['is_new'] for r in records)} new, "
        f"{sum(not r['is_new'] for r in records)} existing)."
    )
    return records


# ============================================================
# STEP 11: Prepare CPPMonthWisePrice records
# ============================================================
def prepare_cpp_monthwise_price_records(month_details: list, headers: list, existing: dict) -> list:
    grouped = {}
    for d in month_details:
        fy_label = _financial_year_label(d["fiscal_year"])
        grouped.setdefault((d["norms_header_id"], fy_label), {})[d["month_num"]] = d["price"]
    records = []
    for (header_id, fy_label), month_prices in grouped.items():
        existing_id = existing["cpp_prices"].get((header_id, fy_label))
        records.append({
            "id": existing_id or str(uuid.uuid4()).upper(),
            "is_new": existing_id is None,
            "norms_header_id": header_id,
            "financial_year": fy_label,
            "aop_year": fy_label,
            "months": {CPP_PRICE_COLUMN[m]: v for m, v in month_prices.items()},
        })
    logger.info(
        f"Prepared {len(records)} CPPMonthWisePrice records "
        f"({sum(r['is_new'] for r in records)} new, "
        f"{sum(not r['is_new'] for r in records)} existing)."
    )
    return records


# ============================================================
# SQL generation helpers
# ============================================================
def _sql_str(val):
    if val is None or val == "" or (isinstance(val, float) and pd.isna(val)):
        return "NULL"
    escaped = str(val).replace("'", "''")
    return f"N'{escaped}'"


def _sql_num(val):
    if val is None or (isinstance(val, float) and pd.isna(val)):
        return "NULL"
    return str(val)


def _sql_bit(val):
    if val is None or (isinstance(val, float) and pd.isna(val)):
        return "NULL"
    return "1" if val else "0"


def generate_sql_file(
    headers, month_details, cpp_records, price_records,
    check_queries: list, out_path: str = SQL_OUTPUT_FILE,
    new_norm_parameters: list = None,
):
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    lines = []
    lines.append("-- ============================================================")
    lines.append("-- JMD SEZ-PCG Norms Import (ODS) - Generated SQL")
    lines.append(f"-- Generated: {datetime.now().isoformat()}")
    lines.append("-- ============================================================")
    lines.append("")
    lines.append("-- ---------- CHECK QUERIES ----------")
    for q in check_queries:
        lines.append(q.strip())
        lines.append("")
    lines.append("-- ---------- INSERT: NormParameters (new only) ----------")
    lines.append("BEGIN TRANSACTION;")
    for p in new_norm_parameters or []:
        lines.append(
            f"INSERT INTO NormParameters (Id, Name, DisplayName, UOM, Type, "
            f"NormParameterType_FK_Id, Plant_FK_Id, NormType_FK_Id, IsHistorical, "
            f"DisplayOrder, IsEditable, IsVisible, CalculationType, SAPMaterialCode) "
            f"VALUES ({_sql_str(p['id'])}, {_sql_str(p['name'])}, {_sql_str(p['name'])}, "
            f"{_sql_str(p['uom'])}, {_sql_str(p['type'])}, "
            f"{_sql_str(p['norm_parameter_type_fk_id'])}, {_sql_str(p['plant_id'])}, "
            f"{_sql_num(p['norm_type_fk_id'])}, {_sql_bit(p['is_historical'])}, "
            f"{_sql_num(p['display_order'])}, {_sql_bit(p['is_editable'])}, "
            f"{_sql_bit(p['is_visible'])}, {_sql_num(p['calculation_type'])}, "
            f"{_sql_str(p['sap_material_code'])});"
        )
    lines.append("")
    lines.append("-- ---------- INSERT: NormsHeader (new only) ----------")
    for h in headers:
        if not h["is_new"]:
            continue
        lines.append(
            f"INSERT INTO NormsHeader (Id, Plant_FK_Id, UtilityName, UtilityId, "
            f"UtilityUOM, AccountName, MaterialName, MaterialId, IssuingPlantName, "
            f"NormParameter_FK_Id, IsActive, IssuingUOM, DisplayOrder, plantCode) "
            f"VALUES ({_sql_str(h['id'])}, {_sql_str(h['plant_id'])}, "
            f"{_sql_str(h['utility_name'])}, {_sql_str(h['utility_id'])}, "
            f"{_sql_str(h['utility_uom'])}, {_sql_str(h['account_name'])}, "
            f"{_sql_str(h['material_name'])}, {_sql_str(h['material_id'])}, "
            f"{_sql_str(h['issuing_plant_name'])}, {_sql_str(h['norm_parameter_id'])}, "
            f"1, {_sql_str(h['issuing_uom'])}, {_sql_num(h['display_order'])}, "
            f"{_sql_str(h['plant_code'])});"
        )
    lines.append("")
    lines.append("-- ---------- INSERT: NormsMonthDetail (new only) ----------")
    for d in month_details:
        if not d["is_new"]:
            continue
        lines.append(
            f"INSERT INTO NormsMonthDetail (Id, NormsHeader_FK_Id, "
            f"FinancialYearMonth_FK_Id, Norms, Quantity, Amount, Price, QTY) "
            f"VALUES ({_sql_str(d['id'])}, {_sql_str(d['norms_header_id'])}, "
            f"{_sql_str(d['fym_id'])}, {_sql_num(d['norms'])}, "
            f"{_sql_num(d['quantity'])}, {_sql_num(d['amount'])}, "
            f"{_sql_num(d['price'])}, {_sql_num(d['qty'])});"
        )
    lines.append("")
    lines.append("-- ---------- INSERT: CPPNorms (new only) ----------")
    for c in cpp_records:
        if not c["is_new"]:
            continue
        month_cols = ", ".join(c["months"].keys())
        month_vals = ", ".join(_sql_num(v) for v in c["months"].values())
        extra_cols = (
            ", NormsHeader_FK_Id, FinancialYear, AOPYear, NormType_FK_Id, "
            "ApplyActualNormToAll, CreatedBy, CreatedDate"
        )
        extra_vals = (
            f", {_sql_str(c['norms_header_id'])}, {_sql_str(c['financial_year'])}, "
            f"{_sql_str(c['aop_year'])}, {c['norm_type_fk_id']}, 0, "
            f"N'JMDImportScript', GETDATE()"
        )
        lines.append(
            f"INSERT INTO CPPNorms (Id{', ' + month_cols if month_cols else ''}"
            f"{extra_cols}) VALUES ({_sql_str(c['id'])}"
            f"{', ' + month_vals if month_vals else ''}{extra_vals});"
        )
    lines.append("")
    lines.append("-- ---------- INSERT: CPPMonthWisePrice (new only) ----------")
    for p in price_records:
        if not p["is_new"]:
            continue
        month_cols = ", ".join(p["months"].keys())
        month_vals = ", ".join(_sql_num(v) for v in p["months"].values())
        extra_cols = (
            ", NormsHeader_FK_Id, FinancialYear, AOPYear, Remarks, PriceSource, "
            "CreatedDate, UpdatedDate, ModifiedBy, ValueType"
        )
        extra_vals = (
            f", {_sql_str(p['norms_header_id'])}, {_sql_str(p['financial_year'])}, "
            f"{_sql_str(p['aop_year'])}, N'Imported from SEZ-PCG_JMD.ods', "
            f"N'SEZ-PCG_JMD.ods', GETDATE(), GETDATE(), N'SYSTEM', N'Price'"
        )
        lines.append(
            f"INSERT INTO CPPMonthWisePrice (Id{', ' + month_cols if month_cols else ''}"
            f"{extra_cols}) VALUES ({_sql_str(p['id'])}"
            f"{', ' + month_vals if month_vals else ''}{extra_vals});"
        )
    lines.append("")
    lines.append("-- COMMIT;   -- uncomment to apply after review")
    lines.append("-- ROLLBACK; -- uncomment to discard")
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    logger.info(f"Generated SQL file written to: {out_path}")
    return out_path


def build_check_queries() -> list:
    plants_in = ", ".join(f"'{p}'" for p in ALLOWED_PLANTS)
    return [
        f"""-- 1. Check JMD SEZ-PCG plants
SELECT * FROM Plants WHERE Name IN ({plants_in}) OR SourceName IN ({plants_in});""",
        f"""-- 2. Check existing norm parameters for JMD SEZ-PCG plants
SELECT * FROM NormParameters WHERE Plant_FK_Id IN (
    SELECT Id FROM Plants WHERE Name IN ({plants_in}) OR SourceName IN ({plants_in})
);""",
        f"""-- 3. Check existing NormsHeader records for JMD SEZ-PCG plants
SELECT nh.* FROM NormsHeader nh
JOIN Plants p ON nh.Plant_FK_Id = p.Id
WHERE p.Name IN ({plants_in}) OR p.SourceName IN ({plants_in});""",
        f"""-- 4. Check existing NormsMonthDetail records
SELECT nmd.* FROM NormsMonthDetail nmd
JOIN NormsHeader nh ON nmd.NormsHeader_FK_Id = nh.Id
JOIN Plants p ON nh.Plant_FK_Id = p.Id
WHERE p.Name IN ({plants_in}) OR p.SourceName IN ({plants_in});""",
        f"""-- 5. Check existing CPPNorms records
SELECT cn.* FROM CPPNorms cn
JOIN NormsHeader nh ON cn.NormsHeader_FK_Id = nh.Id
JOIN Plants p ON nh.Plant_FK_Id = p.Id
WHERE p.Name IN ({plants_in}) OR p.SourceName IN ({plants_in});""",
        f"""-- 6. Check existing CPPMonthWisePrice records
SELECT cp.* FROM CPPMonthWisePrice cp
JOIN NormsHeader nh ON cp.NormsHeader_FK_Id = nh.Id
JOIN Plants p ON nh.Plant_FK_Id = p.Id
WHERE p.Name IN ({plants_in}) OR p.SourceName IN ({plants_in});""",
    ]


# ============================================================
# Validation report
# ============================================================
def print_validation_report(
    df, plant_ids, headers, month_details, cpp_records, price_records,
    column_problems, new_norm_parameters=None,
):
    new_norm_parameters = new_norm_parameters or []
    total_rows = len(df)
    missing_plants = [p for p in ALLOWED_PLANTS if p not in plant_ids]
    new_headers = [h for h in headers if h["is_new"]]
    existing_headers = [h for h in headers if not h["is_new"]]
    new_details = [d for d in month_details if d["is_new"]]
    existing_details = [d for d in month_details if not d["is_new"]]
    new_cpp = [c for c in cpp_records if c["is_new"]]
    existing_cpp = [c for c in cpp_records if not c["is_new"]]
    new_prices = [p for p in price_records if p["is_new"]]
    existing_prices = [p for p in price_records if not p["is_new"]]

    print("\n" + "=" * 70)
    print("JMD SEZ-PCG NORMS IMPORT (ODS) - VALIDATION REPORT")
    print("=" * 70)
    print(f"Total ODS rows (month-level, JMD SEZ-PCG plants only): {total_rows}")
    print(f"Column validation issues: {len(column_problems)}")
    print(f"Missing plants: {missing_plants if missing_plants else 'None'}")
    print()
    print(f"NormParameters - new (to be created): {len(new_norm_parameters)}")
    print(f"NormsHeader  - new: {len(new_headers)}, existing (reused): {len(existing_headers)}")
    print(
        f"NormsMonthDetail - new: {len(new_details)}, "
        f"existing (will be skipped): {len(existing_details)}"
    )
    print(f"CPPNorms - new: {len(new_cpp)}, existing (will be skipped): {len(existing_cpp)}")
    print(
        f"CPPMonthWisePrice - new: {len(new_prices)}, "
        f"existing (will be skipped): {len(existing_prices)}"
    )

    fy_periods = sorted({(d["month_num"], d["year"]) for d in month_details})
    fy_labels = sorted({_financial_year_label(d["fiscal_year"]) for d in month_details})
    print(f"Financial year(s) covered: {fy_labels}")
    print(
        f"Calendar (month, year) periods to be written to NormsMonthDetail: {fy_periods}"
    )
    print("=" * 70 + "\n")


# ============================================================
# Insert records into database
# ============================================================
def insert_records(
    conn, new_norm_parameters: list, headers: list, month_details: list,
    cpp_records: list, price_records: list, plant_ids_created: list = None,
) -> dict:
    manifest = {
        "norm_parameters": [],
        "norms_headers": [],
        "norms_month_details": [],
        "cpp_norms": [],
        "cpp_monthwise_prices": [],
        "plants": list(plant_ids_created or []),
    }
    cur = conn.cursor()
    try:
        # 1. NormParameters
        for p in new_norm_parameters:
            cur.execute(
                "INSERT INTO NormParameters (Id, Name, DisplayName, UOM, Type, "
                "NormParameterType_FK_Id, Plant_FK_Id, NormType_FK_Id, IsHistorical, "
                "DisplayOrder, IsEditable, IsVisible, CalculationType, SAPMaterialCode) "
                "VALUES (CAST(? AS uniqueidentifier), ?, ?, ?, ?, "
                "CAST(? AS uniqueidentifier), CAST(? AS uniqueidentifier), ?, ?, ?, ?, ?, ?, ?)",
                (
                    p["id"], p["name"], p["name"], p["uom"], p["type"],
                    p["norm_parameter_type_fk_id"], p["plant_id"], p["norm_type_fk_id"],
                    p["is_historical"], p["display_order"], p["is_editable"],
                    p["is_visible"], p["calculation_type"], p["sap_material_code"],
                ),
            )
            manifest["norm_parameters"].append(p["id"])
        logger.info(f"Inserted {len(new_norm_parameters)} NormParameters records.")

        # 2. NormsHeader
        for h in headers:
            if not h["is_new"]:
                continue
            cur.execute(
                "INSERT INTO NormsHeader (Id, Plant_FK_Id, UtilityName, UtilityId, "
                "UtilityUOM, AccountName, MaterialName, MaterialId, IssuingPlantName, "
                "NormParameter_FK_Id, IsActive, IssuingUOM, DisplayOrder, plantCode) "
                "VALUES (CAST(? AS uniqueidentifier), CAST(? AS uniqueidentifier), "
                "?, ?, ?, ?, ?, ?, ?, CAST(? AS uniqueidentifier), ?, ?, ?, ?)",
                (
                    h["id"], h["plant_id"], h["utility_name"], h["utility_id"],
                    h["utility_uom"], h["account_name"], h["material_name"],
                    h["material_id"], h["issuing_plant_name"], h["norm_parameter_id"],
                    1, h["issuing_uom"], h["display_order"], h["plant_code"],
                ),
            )
            manifest["norms_headers"].append(h["id"])
        logger.info(f"Inserted {sum(h['is_new'] for h in headers)} NormsHeader records.")

        # 3. NormsMonthDetail
        for d in month_details:
            if not d["is_new"]:
                continue
            cur.execute(
                "INSERT INTO NormsMonthDetail (Id, NormsHeader_FK_Id, "
                "FinancialYearMonth_FK_Id, Norms, Quantity, Amount, Price, QTY) "
                "VALUES (CAST(? AS uniqueidentifier), CAST(? AS uniqueidentifier), "
                "CAST(? AS uniqueidentifier), ?, ?, ?, ?, ?)",
                (
                    d["id"], d["norms_header_id"], d["fym_id"], d["norms"],
                    d["quantity"], d["amount"], d["price"], d["qty"],
                ),
            )
            manifest["norms_month_details"].append(d["id"])
        logger.info(
            f"Inserted {sum(d['is_new'] for d in month_details)} NormsMonthDetail records."
        )

        # 4. CPPNorms
        for c in cpp_records:
            if not c["is_new"]:
                continue
            month_cols = list(c["months"].keys())
            month_vals = list(c["months"].values())
            col_clause = ", " + ", ".join(month_cols) if month_cols else ""
            val_clause = ", " + ", ".join(["?"] * len(month_vals)) if month_vals else ""
            cur.execute(
                f"INSERT INTO CPPNorms (Id{col_clause}, NormsHeader_FK_Id, "
                f"FinancialYear, AOPYear, NormType_FK_Id, ApplyActualNormToAll, "
                f"CreatedBy, CreatedDate) "
                f"VALUES (CAST(? AS uniqueidentifier){val_clause}, "
                f"CAST(? AS uniqueidentifier), ?, ?, ?, 0, N'JMDImportScript', GETDATE())",
                [c["id"]] + month_vals + [
                    c["norms_header_id"], c["financial_year"],
                    c["aop_year"], c["norm_type_fk_id"],
                ],
            )
            manifest["cpp_norms"].append(c["id"])
        logger.info(f"Inserted {sum(r['is_new'] for r in cpp_records)} CPPNorms records.")

        # 5. CPPMonthWisePrice
        for p in price_records:
            if not p["is_new"]:
                continue
            month_cols = list(p["months"].keys())
            month_vals = list(p["months"].values())
            col_clause = ", " + ", ".join(month_cols) if month_cols else ""
            val_clause = ", " + ", ".join(["?"] * len(month_vals)) if month_vals else ""
            cur.execute(
                f"INSERT INTO CPPMonthWisePrice (Id{col_clause}, NormsHeader_FK_Id, "
                f"FinancialYear, AOPYear, Remarks, PriceSource, CreatedDate, "
                f"UpdatedDate, ModifiedBy, ValueType) "
                f"VALUES (CAST(? AS uniqueidentifier){val_clause}, "
                f"CAST(? AS uniqueidentifier), ?, ?, ?, ?, GETDATE(), GETDATE(), ?, ?)",
                [p["id"]] + month_vals + [
                    p["norms_header_id"], p["financial_year"], p["aop_year"],
                    "Imported from SEZ-PCG_JMD.ods", "SEZ-PCG_JMD.ods",
                    "SYSTEM", "Price",
                ],
            )
            manifest["cpp_monthwise_prices"].append(p["id"])
        logger.info(
            f"Inserted {sum(p['is_new'] for p in price_records)} "
            f"CPPMonthWisePrice records."
        )

        conn.commit()
        logger.info("COMMIT successful. All records inserted.")
        return manifest
    except Exception as e:
        conn.rollback()
        logger.error(f"Error during insert. Transaction rolled back: {e}")
        raise


# ============================================================
# MAIN
# ============================================================
def main():
    parser = argparse.ArgumentParser(
        description="Import JMD SEZ-PCG norms from ODS into NormsHeader + transactional tables"
    )
    parser.add_argument(
        "--execute", action="store_true",
        help="Actually insert rows (default: dry run)"
    )
    args = parser.parse_args()

    os.makedirs(OUTPUT_DIR, exist_ok=True)
    os.makedirs(MANIFEST_DIR, exist_ok=True)

    # Step 1: Load ODS
    raw = pd.read_excel(ODS_PATH, engine="odf", header=None)
    column_problems = validate_ods_columns(raw.iloc[HEADER_ROW_IDX])
    df = load_ods_data()

    # Step 2: Connect and fetch DB state
    conn = get_database_connection()
    plant_ids, plant_code_map = fetch_jmd_plants(conn)
    plants_result = ensure_missing_plants_exist(conn, dry_run=not args.execute)
    param_map = fetch_norm_parameters(conn, plant_ids)
    param_templates = fetch_norm_parameter_template(conn, plant_ids)
    norm_type_reference = fetch_norm_type_reference(conn)
    existing = check_existing_norms(conn, plant_ids)
    fym_map = fetch_financial_year_months(conn)

    # Step 3: Prepare records
    headers, new_norm_parameters = prepare_norms_header_records(
        df, plant_ids, plant_code_map, param_map, param_templates,
        norm_type_reference, existing,
    )
    header_lookup = {
        (h["plant_id"], h["utility_name"].upper(), h["account_name"].upper(),
         h["material_name"].upper(), (h["issuing_plant_name"] or "").upper()): h
        for h in headers
    }
    month_details = prepare_norms_month_detail_records(
        df, header_lookup, fym_map, existing, plant_ids
    )
    cpp_records = prepare_cpp_norms_records(month_details, headers, existing)
    price_records = prepare_cpp_monthwise_price_records(month_details, headers, existing)

    # Step 4: Validation report
    print_validation_report(
        df, plant_ids, headers, month_details, cpp_records, price_records,
        column_problems, new_norm_parameters,
    )

    # Step 5: Generate SQL file
    check_queries = build_check_queries()
    generate_sql_file(
        headers, month_details, cpp_records, price_records,
        check_queries, new_norm_parameters=new_norm_parameters,
    )

    if not args.execute:
        print("[DRY RUN] No database changes made. Use --execute to apply.")
        conn.close()
        return

    # Step 6: Execute inserts
    manifest = insert_records(
        conn, new_norm_parameters, headers, month_details,
        cpp_records, price_records,
        plant_ids_created=plants_result.get("created_ids", []),
    )

    # Step 7: Write manifest
    with open(MANIFEST_FILE, "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2)
    logger.info(f"Manifest written to: {MANIFEST_FILE}")

    conn.close()
    print("\nImport complete. All records inserted successfully.")


if __name__ == "__main__":
    main()
