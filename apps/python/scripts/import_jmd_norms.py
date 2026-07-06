"""
JMD C2 Norms Import Script
============================
Imports Norm/Quantity/Amount/Price data for JMD C2 plants from the CSV export
"Norm, Qty, Cost  (1).csv" (UTF-16, tab-delimited) into:

    Plants -> NormsHeader -> NormsMonthDetail
                          -> CPPNorms

This CSV is a combined export covering multiple business units (DMD, HMD,
JMD, NMD, PMD, VMD, ...) - the JMD C2 plant rows live in the same file as
the DMD rows (see apps/python/DMD/scripts/import_dmd_norms.py, which this
script mirrors). Only the JMD C2 plants (ALLOWED_PLANTS below) are read
and written by this script; every other plant's rows in the CSV are
ignored.

NormsHeader.MaterialId is populated from the CSV's "Material ID" column (the
SAP material code) - this column already exists in the database and already
holds SAP-style codes (verified against production data for DMD; same CSV
and column layout applies here), so no schema change is needed.

The CSV has no "Utility ID" column (the old Excel source did), but
NormsHeader.UtilityId is NOT NULL. Since Utility ID is a per-utility-name
constant, it's backfilled from a lookup built from the old Excel file
(UTILITY_ID_LOOKUP_EXCEL_PATH), which has a clean Utility name -> UtilityId
mapping for these plants.

NormsHeader.plantCode is populated from Plants.PlantCode for the row's plant.

NormsHeader.Remarks and CPP_SR_Mapping_Master_Fk_Id have no source in either
file and are left NULL on new inserts.

Scope: only these JMD C2 plants are processed:
    - JMD - C2 Utility Plant
    - JMD - C2-GTG 1
    - JMD - C2-GTG 2
    - JMD - C2-STG 1
    - JMD - DTA-C2 Power & UTILITY

Usage:
    python scripts/import_jmd_norms.py                 # dry run (default)
    python scripts/import_jmd_norms.py --execute        # actually insert (DRY_RUN=False)

Phase 1 (dry run, DRY_RUN=True):
    - Reads and validates the CSV data.
    - Compares against current DB state (Plants, NormParameters, NormsHeader,
      NormsMonthDetail, CPPNorms).
    - Prints a validation report.
    - Writes SELECT check queries and generated INSERT statements to a .sql file.
    - Makes NO changes to the database.

Phase 2 (real run, DRY_RUN=False, requires --execute):
    - Re-validates, then inserts new records inside a single transaction.
    - Rolls back on any error.
    - Skips anything that already exists (no duplicate inserts).
    - On commit, writes a manifest of every inserted Id to scripts/manifests/,
      so this run can be undone later with rollback_jmd_norms.py.
"""

import argparse
import json
import logging
import os
import sys
import uuid
from datetime import datetime

import pandas as pd

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))  # apps/python/JMD
from database.connection import get_connection  # noqa: E402

# ============================================================
# CONFIG
# ============================================================

# Shared combined export - lives under the DMD app's files/ tree since that's
# where it was originally sourced from, but covers all business units,
# including the JMD C2 plants this script cares about.
CSV_PATH = os.path.join(
    os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
    "DMD", "files", "DMD",
    "DMD HMD Operation Logic Document and HMD Consumption and Norm file",
    "Norm, Qty, Cost  (1).csv",
)
CSV_ENCODING = "utf-16"
CSV_SEP = "\t"

# Old wide Excel source, used only as a lookup for UtilityId (see module
# docstring) since the combined CSV export dropped that column.
UTILITY_ID_LOOKUP_EXCEL_PATH = os.path.join(
    os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
    "DMD", "files", "DMD power & steam balance (1).xlsx",
)
UTILITY_ID_LOOKUP_SHEET = "Norm, Qty, Cost "
UTILITY_ID_LOOKUP_HEADER_ROW_IDX = 3
UTILITY_ID_LOOKUP_DATA_START_ROW_IDX = 4
UTILITY_ID_LOOKUP_COL_PLANT = 0
UTILITY_ID_LOOKUP_COL_UTILITY = 1
UTILITY_ID_LOOKUP_COL_UTILITY_ID = 2

ALLOWED_PLANTS = [
    "JMD - C2 Utility Plant",
    "JMD - C2-GTG 1",
    "JMD - C2-GTG 2",
    "JMD - C2-STG 1",
    "JMD - DTA-C2 Power & UTILITY",
]

# "JMD - C2-GTG 1", "JMD - C2-GTG 2" and "JMD - C2-STG 1" have no row of their
# own in Plants - they only exist as PowerGenerationAssets rows (AssetName
# matches exactly) scoped under the C2-CPP plant via CPPPLANT_FK_Id. Verified
# against the DB (2026-07): Plants has no row named/sourced as any of the
# three, but PowerGenerationAssets has one row per name with
# CPPPLANT_FK_Id = BA558F95-8A3F-4769-9C78-FF7B6C639DDF (Plants.Name = 'C2-CPP'),
# PlantCode 36H0 / 36GX / 36HM respectively.
#
# Confirmed with user: rather than reusing "JMD - C2 Utility Plant"'s own
# Plants.Id for these three, create a proper Plants row for each, mirroring
# the sibling row "JMD - C2 Utility Plant" (Id=E35E3E4F-D399-40E7-B0DA-
# 2EBB264C6E9A) for Site_FK_Id/Vertical_FK_Id/DisplayOrder/SourceName, and
# using each one's own PlantCode from PowerGenerationAssets. See
# ensure_missing_plants_exist().
SIBLING_PLANT_NAME_FOR_NEW_ROWS = "JMD - C2 Utility Plant"
NEW_PLANTS_TO_CREATE = {
    "JMD - C2-GTG 1": {"plant_code": "36H0", "display_name": "JMD - C2-GTG 1"},
    "JMD - C2-GTG 2": {"plant_code": "36GX", "display_name": "JMD - C2-GTG 2"},
    "JMD - C2-STG 1": {"plant_code": "36HM", "display_name": "JMD - C2-STG 1"},
}

# Row index (0-based, after header=None read) where the actual header row lives.
HEADER_ROW_IDX = 2
DATA_START_ROW_IDX = 3

# Fixed identity columns (0-based column index in the sheet)
COL_PLANT = 0
COL_UTILITY = 1
COL_UTILITY_UOM = 2
COL_ACCOUNT = 3
COL_MATERIAL = 4
COL_MATERIAL_ID = 5  # SAP material code -> NormsHeader.MaterialId
COL_MATERIAL_UOM = 6
COL_ISSUING_PLANT = 7
COL_ISSUING_PLANT_ID = 8
FIRST_MONTH_COL = 9  # first "Norms" column of the April block

MONTH_BLOCK_WIDTH = 6  # Norms, Quantity, Amount, Price, Generation Qty, Rate Per Unit
METRICS_PER_MONTH = ["Norms", "Quantity", "Amount", "Price", "Generation Qty", "Rate Per Unit"]
NUM_MONTHS = 12

CPP_MONTH_COLUMN = {
    4: "Apr_Norms", 5: "May_Norms", 6: "Jun_Norms", 7: "Jul_Norms",
    8: "Aug_Norms", 9: "Sep_Norms", 10: "Oct_Norms", 11: "Nov_Norms",
    12: "Dec_Norms", 1: "Jan_Norms", 2: "Feb_Norms", 3: "Mar_Norms",
}

# CPPNorms.NormType_FK_Id: 6 = "Fixed" (AOP/budget-sourced norms).
# NOTE: 7 = "Model Calculated" is reserved for values synced from the python
# calculation model - do NOT use it here.
CPP_NORM_TYPE_FIXED = 6

OUTPUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "output")
SQL_OUTPUT_FILE = os.path.join(
    OUTPUT_DIR, f"jmd_norms_import_{datetime.now():%Y%m%d_%H%M%S}.sql"
)

MANIFEST_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "manifests")
MANIFEST_FILE = os.path.join(
    MANIFEST_DIR, f"jmd_norms_import_{datetime.now():%Y%m%d_%H%M%S}.json"
)

DRY_RUN = True  # Phase switch. Overridden by --execute CLI flag in main().

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
logger = logging.getLogger("jmd_norms_import")


# ============================================================
# STEP 1: Load CSV data
# ============================================================
def _month_label_to_num_year(label: str) -> tuple:
    """'2026.04' / '2026.1' (pandas-truncated '2026.10') -> (month_num, calendar_year)."""
    year_str, month_str = str(label).split(".")
    return int(month_str.ljust(2, "0")[:2]), int(year_str)


def load_utility_id_lookup(path: str = UTILITY_ID_LOOKUP_EXCEL_PATH) -> dict:
    """
    Build {utility_name: utility_id} from the old Excel source, scoped to
    ALLOWED_PLANTS. The combined CSV has no Utility ID column, but
    NormsHeader.UtilityId is NOT NULL, so this backfills it.
    """
    if not os.path.exists(path):
        raise FileNotFoundError(
            f"UtilityId lookup Excel file not found: {path}. "
            "This file is required to populate NormsHeader.UtilityId "
            "(NOT NULL column) since the CSV source doesn't have it."
        )
    raw = pd.read_excel(path, sheet_name=UTILITY_ID_LOOKUP_SHEET, header=None)
    data = raw.iloc[UTILITY_ID_LOOKUP_DATA_START_ROW_IDX:]
    plant_col = data[UTILITY_ID_LOOKUP_COL_PLANT].astype(str).str.strip()
    data = data[plant_col.isin(ALLOWED_PLANTS)]

    lookup = {}
    conflicts = set()
    for _, row in data.iterrows():
        utility = _clean_str(row[UTILITY_ID_LOOKUP_COL_UTILITY])
        utility_id = _clean_str(row[UTILITY_ID_LOOKUP_COL_UTILITY_ID])
        if not utility or not utility_id:
            continue
        key = utility.upper()
        if key in lookup and lookup[key] != utility_id:
            conflicts.add(utility)
        lookup[key] = utility_id

    if conflicts:
        logger.warning(f"UtilityId lookup has conflicting ids for: {sorted(conflicts)}")
    logger.info(f"Built UtilityId lookup with {len(lookup)} utility names from old Excel source.")
    return lookup


def load_csv_data(path: str = CSV_PATH, utility_id_lookup: dict = None) -> pd.DataFrame:
    """
    Read the wide-format CSV with no header, and rebuild it as a tidy
    long-format DataFrame: one row per (Plant, Utility, Account, Material,
    IssuingPlant, Month) with Norms/Quantity/Amount/Price columns.
    """
    logger.info(f"Reading CSV file: {path}")
    if not os.path.exists(path):
        raise FileNotFoundError(f"CSV file not found: {path}")
    utility_id_lookup = utility_id_lookup or {}

    raw = pd.read_csv(path, sep=CSV_SEP, encoding=CSV_ENCODING, header=None)
    logger.info(f"Raw CSV shape: {raw.shape}")

    month_label_row = raw.iloc[1]
    header_row = raw.iloc[HEADER_ROW_IDX]
    logger.info(f"Header row ({HEADER_ROW_IDX}) columns: {list(header_row.values)}")

    # Resolve (month_num, calendar_year) for each of the 12 month blocks.
    month_periods = []
    for month_idx in range(NUM_MONTHS):
        base_col = FIRST_MONTH_COL + month_idx * MONTH_BLOCK_WIDTH
        month_num, cal_year = _month_label_to_num_year(month_label_row[base_col])
        month_periods.append((month_num, cal_year))
    logger.info(f"Detected month periods: {month_periods}")
    validate_financial_year_periods(month_periods)

    data = raw.iloc[DATA_START_ROW_IDX:].reset_index(drop=True)

    # Filter to only the allowed JMD C2 plants as early as possible.
    plant_col = data[COL_PLANT].astype(str).str.strip()
    data = data[plant_col.isin(ALLOWED_PLANTS)].reset_index(drop=True)
    logger.info(f"Rows after filtering to allowed JMD C2 plants: {len(data)}")

    # Drop per-utility subtotal rows (Account == Material == "Total") - these
    # are rollups added by the export, not real norm line items.
    account_col = data[COL_ACCOUNT].astype(str).str.strip()
    material_col = data[COL_MATERIAL].astype(str).str.strip()
    subtotal_mask = (account_col.str.lower() == "total") & (material_col.str.lower() == "total")
    if subtotal_mask.any():
        logger.info(f"Dropping {subtotal_mask.sum()} subtotal rows (Account/Material == 'Total').")
        data = data[~subtotal_mask].reset_index(drop=True)

    # Build tidy long-format rows: one per (identity columns + month).
    missing_utility_id = set()
    records = []
    for _, row in data.iterrows():
        utility_name = _clean_str(row[COL_UTILITY])
        utility_id = utility_id_lookup.get(utility_name.upper())
        if not utility_id:
            missing_utility_id.add(utility_name)

        identity = {
            "plant_name": _clean_str(row[COL_PLANT]),
            "utility_name": utility_name,
            "utility_id": utility_id,
            "utility_uom": _clean_str(row[COL_UTILITY_UOM]),
            "account_name": _clean_str(row[COL_ACCOUNT]),
            "material_name": _clean_str(row[COL_MATERIAL]),
            "material_id": _clean_sap_code(row[COL_MATERIAL_ID]),
            "material_uom": _clean_str(row[COL_MATERIAL_UOM]),
            "issuing_plant_name": _clean_str(row[COL_ISSUING_PLANT]),
            "issuing_plant_id": _clean_str(row[COL_ISSUING_PLANT_ID]),
        }
        for month_idx, (month_num, cal_year) in enumerate(month_periods):
            base_col = FIRST_MONTH_COL + month_idx * MONTH_BLOCK_WIDTH
            norms = row[base_col]
            quantity = row[base_col + 1]
            amount = row[base_col + 2]
            price = row[base_col + 3]

            # Skip months where all 4 core metrics are blank (nothing to import).
            if _all_blank([norms, quantity, amount, price]):
                continue

            rec = dict(identity)
            rec.update(
                {
                    "month_num": month_num,
                    "calendar_year": cal_year,
                    "norms": _to_float(norms),
                    "quantity": _to_float(quantity),
                    "amount": _to_float(amount),
                    "price": _to_float(price),
                }
            )
            records.append(rec)

    if missing_utility_id:
        logger.warning(
            f"No UtilityId found (old Excel lookup) for utilities: {sorted(missing_utility_id)}. "
            "Rows for these utilities will be skipped (UtilityId is NOT NULL)."
        )

    df = pd.DataFrame.from_records(records)
    if not df.empty:
        before = len(df)
        df = df[df["utility_id"].notna()].reset_index(drop=True)
        dropped = before - len(df)
        if dropped:
            logger.warning(f"Dropped {dropped} rows with no resolvable UtilityId.")
    logger.info(f"Tidy long-format rows built: {len(df)}")
    return df


def _clean_str(val) -> str:
    if pd.isna(val):
        return ""
    s = str(val).strip()
    return "" if s == "-" else s


def _clean_sap_code(val):
    """Material ID column (-> NormsHeader.MaterialId): blank / '-' / whitespace-only -> None (NULL in DB)."""
    if pd.isna(val):
        return None
    s = str(val).strip()
    if s == "" or s == "-":
        return None
    return s


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
    """pandas' nullable string dtype surfaces missing cells as float('nan') via
    .iterrows(), not None or pd.NA - normalize before this reaches SQL/DB params."""
    if val is None or (isinstance(val, float) and pd.isna(val)):
        return None
    return val


# ============================================================
# STEP 2: Validate CSV columns
# ============================================================
def validate_csv_columns(raw_header_row) -> list:
    """
    Confirm the identity columns and the first month's metric columns match
    what we expect. Returns a list of problem strings (empty = all good).
    """
    problems = []
    expected_identity = {
        COL_PLANT: "Utility Plant",
        COL_UTILITY: "Utility",
        COL_ACCOUNT: "Account",
        COL_MATERIAL: "Material",
        COL_MATERIAL_ID: "Material ID",
        COL_ISSUING_PLANT: "Issuing Plant",
    }
    for idx, expected_name in expected_identity.items():
        actual = str(raw_header_row[idx]).strip()
        if expected_name.lower() not in actual.lower():
            problems.append(
                f"Column {idx}: expected something like '{expected_name}', found '{actual}'"
            )

    for month_idx in range(NUM_MONTHS):
        base_col = FIRST_MONTH_COL + month_idx * MONTH_BLOCK_WIDTH
        for offset, metric in enumerate(METRICS_PER_MONTH):
            actual = str(raw_header_row[base_col + offset]).strip()
            if metric.split(" ")[0].lower() not in actual.lower():
                problems.append(
                    f"Month block {month_idx} column {base_col + offset}: "
                    f"expected '{metric}', found '{actual}'"
                )

    if problems:
        logger.warning(f"Column validation found {len(problems)} issue(s):")
        for p in problems:
            logger.warning(f"  - {p}")
    else:
        logger.info("Column validation passed: all expected columns found.")
    return problems


# ============================================================
# STEP 3: Database connection
# ============================================================
def get_database_connection():
    """Thin wrapper around the shared connection helper (kept for clarity/structure)."""
    return get_connection()


# ============================================================
# STEP 4: Fetch JMD C2 plants
# ============================================================
def fetch_jmd_plants(conn) -> tuple:
    """
    Fetch Plant Id + PlantCode for each allowed JMD C2 plant.
    Returns ({plant_name: plant_id}, {plant_name: plant_code}).

    Call ensure_missing_plants_exist() first so "JMD - C2-GTG 1",
    "JMD - C2-GTG 2" and "JMD - C2-STG 1" (which have no Plants row of their
    own - they're PowerGenerationAssets rows scoped under the C2-CPP plant)
    have real rows to find here.
    """
    cur = conn.cursor()
    placeholders = ",".join("?" for _ in ALLOWED_PLANTS)
    cur.execute(
        f"""
        SELECT Id, Name, SourceName, IsActive, PlantCode
        FROM Plants
        WHERE Name IN ({placeholders}) OR SourceName IN ({placeholders})
        """,
        ALLOWED_PLANTS + ALLOWED_PLANTS,
    )
    rows = cur.fetchall()

    plant_map = {}
    plant_code_map = {}
    for plant_id, name, source_name, is_active, plant_code in rows:
        matched_name = name if name in ALLOWED_PLANTS else (
            source_name if source_name in ALLOWED_PLANTS else None
        )
        if matched_name:
            plant_map[matched_name] = str(plant_id)
            plant_code_map[matched_name] = plant_code

    missing = [p for p in ALLOWED_PLANTS if p not in plant_map]
    if missing:
        logger.warning(f"Missing plants in Plants table: {missing}")
    else:
        logger.info(f"All required JMD C2 plants found: {plant_map}")

    return plant_map, plant_code_map


def ensure_missing_plants_exist(conn, dry_run: bool) -> dict:
    """
    Create a Plants row for each name in NEW_PLANTS_TO_CREATE that doesn't
    already exist (matched by Name or SourceName, same as fetch_jmd_plants),
    mirroring SIBLING_PLANT_NAME_FOR_NEW_ROWS's Site_FK_Id, Vertical_FK_Id,
    DisplayOrder and SourceName - only Name/DisplayName/PlantCode/Id differ.

    Returns {"created": [names], "already_existed": [names]}.
    In a dry run, only reports what WOULD be created; makes no DB changes.
    """
    cur = conn.cursor()
    cur.execute(
        "SELECT Id, Site_FK_Id, Vertical_FK_Id, DisplayOrder, SourceName, IsActive "
        "FROM Plants WHERE Name = ?",
        (SIBLING_PLANT_NAME_FOR_NEW_ROWS,),
    )
    sibling = cur.fetchone()
    if not sibling:
        raise RuntimeError(
            f"Sibling plant '{SIBLING_PLANT_NAME_FOR_NEW_ROWS}' not found in Plants - "
            "cannot derive Site_FK_Id/Vertical_FK_Id/SourceName for new rows."
        )
    _, site_fk, vertical_fk, display_order, source_name, is_active = sibling

    placeholders = ",".join("?" for _ in NEW_PLANTS_TO_CREATE)
    names = list(NEW_PLANTS_TO_CREATE.keys())
    cur.execute(
        f"SELECT Name, SourceName FROM Plants WHERE Name IN ({placeholders}) OR SourceName IN ({placeholders})",
        names + names,
    )
    existing_names = {name for name, _ in cur.fetchall()}

    created, already_existed, created_ids = [], [], []
    for name, meta in NEW_PLANTS_TO_CREATE.items():
        if name in existing_names:
            already_existed.append(name)
            continue

        if dry_run:
            logger.info(
                f"[DRY RUN] Would create Plants row for '{name}' "
                f"(PlantCode={meta['plant_code']}, SourceName={source_name})"
            )
            created.append(name)
            continue

        new_id = str(uuid.uuid4()).upper()
        cur.execute(
            """
            INSERT INTO Plants
                (Id, Name, DisplayName, Site_FK_Id, Vertical_FK_Id, IsActive,
                 DisplayOrder, SourceName, PlantCode)
            VALUES (CAST(? AS uniqueidentifier), ?, ?, CAST(? AS uniqueidentifier),
                    CAST(? AS uniqueidentifier), ?, ?, ?, ?)
            """,
            (
                new_id, name, meta["display_name"], site_fk, vertical_fk,
                is_active, display_order, source_name, meta["plant_code"],
            ),
        )
        logger.info(f"Created Plants row for '{name}': Id={new_id}, PlantCode={meta['plant_code']}")
        created.append(name)
        created_ids.append(new_id)

    if not dry_run and created:
        conn.commit()
        logger.info(f"COMMIT successful. Created {len(created)} new Plants row(s): {created}")

    return {"created": created, "already_existed": already_existed, "created_ids": created_ids}


# ============================================================
# STEP 5: Fetch norm parameters
# ============================================================
def fetch_norm_parameters(conn, plant_ids: dict) -> dict:
    """
    Fetch NormParameters scoped to the given JMD C2 plant ids.
    Returns {(plant_id, upper_name): norm_parameter_id}.

    NOTE: NormParameters.Name is NOT unique across the whole table (the same
    name like 'D M Water' appears under many other plants), so we only match
    within the plant's own scope. If nothing matches, a new NormParameters
    row is created (see prepare_norm_parameter_records / NORM_PARAMETER_TEMPLATE_FIELDS)
    rather than leaving NormParameter_FK_Id NULL.
    """
    if not plant_ids:
        return {}
    cur = conn.cursor()
    placeholders = ",".join("?" for _ in plant_ids)
    cur.execute(
        f"""
        SELECT Id, Name, Plant_FK_Id
        FROM NormParameters
        WHERE Plant_FK_Id IN ({placeholders})
        """,
        list(plant_ids.values()),
    )
    param_map = {}
    for param_id, name, plant_fk in cur.fetchall():
        key = (str(plant_fk), (name or "").strip().upper())
        param_map[key] = str(param_id)
    logger.info(f"Fetched {len(param_map)} plant-scoped NormParameters for JMD C2 plants.")
    return param_map


def lookup_norm_parameter(param_map: dict, plant_id: str, name: str):
    return param_map.get((plant_id, (name or "").strip().upper()))


# Columns NormParameters has no per-material source for in the CSV (no
# Expression/ExecuteQuery/DependantAttributeId/Type/CalculationType data, and
# NormParameterType_FK_Id/IsHistorical/IsEditable/IsVisible/DisplayOrder
# aren't derivable from a single CSV row) - copied from a template
# NormParameters row instead (confirmed with user). NormType_FK_Id is
# deliberately NOT templated here - it's a real business distinction
# (1 = Production/generating utility, 2 = Consumption utility, per NormTypes
# table) resolved per-new-row instead: see fetch_norm_type_reference() and
# its use in prepare_norms_header_records().
NORM_PARAMETER_TEMPLATE_FIELDS = [
    "Type", "NormParameterType_FK_Id", "IsHistorical",
    "DisplayOrder", "IsEditable", "IsVisible", "CalculationType",
]

# NormType_FK_Id per NormTypes table: 1 = Production (generating utility),
# 2 = Consumption.
NORM_TYPE_PRODUCTION = 1
NORM_TYPE_CONSUMPTION = 2

# Plants.Id for the actual "C2-CPP" plant row (Name = 'C2-CPP', no
# SourceName) - distinct from "JMD - C2 Utility Plant" (E35E3E4F-..., whose
# SourceName points at this same Id). Verified against the DB (2026-07): this
# plant already has NormParameters rows for every CSV Utility-column name
# (POWERGEN, HP/LP/MP Steam PRDS, HRSG*_STEAM, AUXBOIL*_STEAM = NormType 1)
# and every "_Dis" distribution name consumed as a Material input
# (Power_Dis, HP/LP/MP/SHP Steam_Dis = NormType 2) - i.e. real, verified
# classifications for the exact names this import needs, so they're used as
# the primary source instead of guessing (confirmed with user).
NORM_TYPE_REFERENCE_PLANT_ID = "BA558F95-8A3F-4769-9C78-FF7B6C639DDF"


def fetch_norm_type_reference(conn) -> dict:
    """
    Build {upper_name: NormType_FK_Id} from NORM_TYPE_REFERENCE_PLANT_ID's
    existing NormParameters - the verified real Production(1)/Consumption(2)
    classification for utility/material names shared with the CSV. Used as
    the primary NormType_FK_Id source for new NormParameters records; names
    with no entry here fall back to the CSV-position rule (utility name in
    its own "Utility" column = Production, consumed as a Material = Consumption).
    """
    cur = conn.cursor()
    cur.execute(
        "SELECT Name, NormType_FK_Id FROM NormParameters WHERE Plant_FK_Id = ? AND NormType_FK_Id IS NOT NULL",
        (NORM_TYPE_REFERENCE_PLANT_ID,),
    )
    reference = {(name or "").strip().upper(): norm_type for name, norm_type in cur.fetchall()}
    logger.info(f"Built NormType reference with {len(reference)} names from C2-CPP.")
    return reference


def fetch_norm_parameter_template(conn, plant_ids: dict) -> dict:
    """
    For each plant, fetch one representative existing NormParameters row
    (NORM_PARAMETER_TEMPLATE_FIELDS only - NOT NormType_FK_Id, see above) to
    use as a template when creating new NormParameters rows for
    materials/utilities with no existing match. Plants with zero existing
    NormParameters rows (e.g. newly-created C2-GTG/STG plants) fall back to
    SIBLING_PLANT_NAME_FOR_NEW_ROWS's template.

    Returns {plant_id: {field: value, ...}}.
    """
    if not plant_ids:
        return {}
    cur = conn.cursor()
    placeholders = ",".join("?" for _ in plant_ids)
    cols = ", ".join(NORM_PARAMETER_TEMPLATE_FIELDS)
    cur.execute(
        f"SELECT Plant_FK_Id, {cols} FROM NormParameters WHERE Plant_FK_Id IN ({placeholders})",
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
# STEP 6: Check existing norms (NormsHeader / NormsMonthDetail / CPPNorms)
# ============================================================
def check_existing_norms(conn, plant_ids: dict) -> dict:
    """
    Load existing NormsHeader (+ month detail + CPP norms) for the JMD C2
    plants so we can detect duplicates before inserting.
    """
    if not plant_ids:
        return {"headers": {}, "month_details": {}, "cpp_norms": {}}

    cur = conn.cursor()
    placeholders = ",".join("?" for _ in plant_ids)

    cur.execute(
        f"""
        SELECT nh.Id, nh.Plant_FK_Id, nh.UtilityName, nh.AccountName, nh.MaterialName,
               nh.IssuingPlantName, nh.UtilityUOM, nh.IssuingUOM, nh.DisplayOrder
        FROM NormsHeader nh
        WHERE nh.Plant_FK_Id IN ({placeholders}) AND nh.IsActive = 1
        """,
        list(plant_ids.values()),
    )
    headers = {}
    max_display_order = {}
    for r in cur.fetchall():
        header_id, plant_fk, utility, account, material, issuing_plant, uom, issuing_uom, disp_order = r
        key = (str(plant_fk), utility.strip().upper(), account.strip().upper(),
               material.strip().upper(), (issuing_plant or "").strip().upper())
        headers[key] = str(header_id)
        pid = str(plant_fk)
        max_display_order[pid] = max(max_display_order.get(pid, 0), disp_order or 0)

    month_details = {}
    if headers:
        header_ids = list(headers.values())
        ph = ",".join("?" for _ in header_ids)
        cur.execute(
            f"""
            SELECT nmd.NormsHeader_FK_Id, fym.Month, fym.Year, nmd.Id
            FROM NormsMonthDetail nmd
            INNER JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
            WHERE nmd.NormsHeader_FK_Id IN ({ph})
            """,
            header_ids,
        )
        for header_fk, month, year, detail_id in cur.fetchall():
            month_details[(str(header_fk), month, year)] = str(detail_id)

    cpp_norms = {}
    if headers:
        header_ids = list(headers.values())
        ph = ",".join("?" for _ in header_ids)
        cur.execute(
            f"""
            SELECT NormsHeader_FK_Id, FinancialYear, Id
            FROM CPPNorms
            WHERE NormsHeader_FK_Id IN ({ph})
            """,
            header_ids,
        )
        for header_fk, fy, cpp_id in cur.fetchall():
            cpp_norms[(str(header_fk), fy)] = str(cpp_id)

    logger.info(
        f"Existing records found - NormsHeader: {len(headers)}, "
        f"NormsMonthDetail: {len(month_details)}, CPPNorms: {len(cpp_norms)}"
    )
    return {
        "headers": headers,
        "month_details": month_details,
        "cpp_norms": cpp_norms,
        "max_display_order": max_display_order,
    }


# ============================================================
# STEP 7: Fetch FinancialYearMonth map
# ============================================================
def fetch_financial_year_months(conn) -> dict:
    """Returns {(month, year): fym_id}."""
    cur = conn.cursor()
    cur.execute("SELECT Id, Month, Year FROM FinancialYearMonth")
    return {(month, year): str(fym_id) for fym_id, month, year in cur.fetchall()}


def resolve_fiscal_year(month_num: int, calendar_year: int) -> int:
    """
    calendar_year is the calendar year the month falls in (e.g. Apr-2026 -> 2026,
    Jan-2027 -> 2027). FY is labeled by its starting (April) calendar year, so
    Jan-Mar months belong to the FY that started the previous calendar year.

    NOTE: this is ONLY for the CPPNorms.FinancialYear label (e.g. "2026-27").
    FinancialYearMonth.Year (and NormsMonthDetail's fym lookup) stores the
    actual CALENDAR year of the month, not this FY-start year - use
    calendar_year directly for that, never this function's return value.
    """
    if month_num >= 4:
        return calendar_year
    return calendar_year - 1


def validate_financial_year_periods(month_periods: list) -> None:
    """
    Confirm the 12 (month_num, calendar_year) periods detected from the CSV
    map to exactly one financial year running April -> March, e.g. for FY
    2026-27: Apr 2026, May 2026, ..., Dec 2026, Jan 2027, Feb 2027, Mar 2027.
    Logs an error (does not raise) if the periods don't form a clean FY span,
    so a malformed source file is caught before any DB writes.
    """
    fiscal_years = {resolve_fiscal_year(m, y) for m, y in month_periods}
    if len(fiscal_years) != 1:
        logger.error(
            f"CSV month columns span multiple financial years {sorted(fiscal_years)} "
            f"- expected exactly one FY. Periods detected: {month_periods}"
        )
        return

    fy_start = next(iter(fiscal_years))
    expected = [(m, fy_start) for m in range(4, 13)] + [(m, fy_start + 1) for m in range(1, 4)]
    if month_periods != expected:
        logger.error(
            f"CSV month columns are not in the expected Apr-Mar order for FY "
            f"{_financial_year_label(fy_start)}. Expected: {expected}. Found: {month_periods}"
        )
        return

    logger.info(
        f"Financial year period check passed: FY {_financial_year_label(fy_start)} "
        f"covers Apr {fy_start} - Mar {fy_start + 1} "
        f"({month_periods[0]} .. {month_periods[-1]})."
    )


# ============================================================
# STEP 8: Prepare NormsHeader records
# ============================================================
def prepare_norms_header_records(df: pd.DataFrame, plant_ids: dict, plant_code_map: dict,
                                  param_map: dict, param_templates: dict, norm_type_reference: dict,
                                  existing: dict) -> tuple:
    """
    Build one NormsHeader record per unique
    (Plant, Utility, Account, Material, IssuingPlant) combination found in
    the CSV data. Marks each as NEW or EXISTING (reuse existing Id).

    When a header has no matching NormParameter (by material name, falling
    back to utility name), a new NormParameters record is prepared too
    (Name/UOM/SAPMaterialCode from the CSV, remaining columns copied from
    that plant's template - see fetch_norm_parameter_template) instead of
    leaving NormParameter_FK_Id NULL.

    NormType_FK_Id for the new record is resolved from norm_type_reference
    (real Production/Consumption data verified against the C2-CPP plant, see
    fetch_norm_type_reference) by name; if the name has no precedent there,
    it falls back to the CSV-position rule: matched via material_name (a
    material/utility consumed as an input) -> Consumption, matched via the
    row's own utility_name fallback (nothing consumed, this IS the utility
    being generated) -> Production.

    Returns (headers, new_norm_parameters).
    """
    identity_cols = [
        "plant_name", "utility_name", "utility_id", "utility_uom",
        "account_name", "material_name", "material_id", "material_uom",
        "issuing_plant_name", "issuing_plant_id",
    ]
    unique_headers = df[identity_cols].drop_duplicates().reset_index(drop=True)

    headers = []
    new_params_by_key = {}  # (plant_id, upper_name) -> new NormParameters record
    next_display_order = dict(existing.get("max_display_order", {}))

    for _, row in unique_headers.iterrows():
        plant_id = plant_ids.get(row["plant_name"])
        if not plant_id:
            continue  # plant missing - flagged separately in validation report

        # Some accounts (e.g. "Start-up Cost", "Stores & Spares") carry a cost
        # with no specific material in the source. MaterialName is NOT NULL and
        # has no blank precedent in production data, so fall back to the
        # account name rather than inserting an empty string (same convention
        # as the DMD import).
        material_name = row["material_name"] or row["account_name"]

        key = (
            plant_id,
            row["utility_name"].upper(),
            row["account_name"].upper(),
            material_name.upper(),
            row["issuing_plant_name"].upper(),
        )
        existing_id = existing["headers"].get(key)
        # The utility_name fallback lookup only applies when there's genuinely
        # no material on this row (material_name falls back to account_name
        # above, so "no material" in the CSV sense means material_name here
        # equals account_name, i.e. row["material_name"] was blank) - it must
        # NOT be used as a second-chance lookup whenever the material-name
        # match merely fails to find an existing param, otherwise unrelated
        # new materials silently collapse onto the plant's utility-name param
        # (e.g. "Power" and "POWERGEN" as materials both wrongly resolving to
        # the "Power_Dis" utility param - caught and fixed after user review).
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
                fallback_norm_type = NORM_TYPE_CONSUMPTION if used_material_name else NORM_TYPE_PRODUCTION
                norm_type = norm_type_reference.get(param_name.upper(), fallback_norm_type)
                new_param = {
                    "id": str(uuid.uuid4()).upper(),
                    "plant_id": plant_id,
                    "name": param_name,
                    "uom": row["material_uom"] or row["utility_uom"] or None,
                    "sap_material_code": _na_to_none(row["material_id"]),
                    **{
                        _NORM_PARAM_FIELD_TO_KEY[f]: template.get(f)
                        for f in NORM_PARAMETER_TEMPLATE_FIELDS
                    },
                    "norm_type_fk_id": norm_type,
                }
                new_params_by_key[param_key] = new_param
                # Also register it in param_map so later rows for the same
                # (plant, name) reuse this same new Id instead of duplicating.
                param_map[param_key] = new_param["id"]
            norm_param_id = new_param["id"]

        if existing_id:
            header_id = existing_id
            is_new = False
        else:
            header_id = str(uuid.uuid4()).upper()
            is_new = True
            next_display_order[plant_id] = next_display_order.get(plant_id, 0) + 1

        headers.append(
            {
                "id": header_id,
                "is_new": is_new,
                "plant_id": plant_id,
                "plant_name": row["plant_name"],
                "plant_code": _na_to_none(plant_code_map.get(row["plant_name"])),
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
            }
        )

    new_norm_parameters = list(new_params_by_key.values())

    logger.info(
        f"Prepared {len(headers)} NormsHeader records "
        f"({sum(h['is_new'] for h in headers)} new, "
        f"{sum(not h['is_new'] for h in headers)} existing)."
    )
    logger.info(f"Prepared {len(new_norm_parameters)} new NormParameters records.")
    return headers, new_norm_parameters


# Maps NORM_PARAMETER_TEMPLATE_FIELDS (DB column names) to the snake_case keys
# used in the new-NormParameters record dicts built above.
_NORM_PARAM_FIELD_TO_KEY = {
    "Type": "type",
    "NormParameterType_FK_Id": "norm_parameter_type_fk_id",
    "IsHistorical": "is_historical",
    "DisplayOrder": "display_order",
    "IsEditable": "is_editable",
    "IsVisible": "is_visible",
    "CalculationType": "calculation_type",
}


# ============================================================
# STEP 9: Prepare NormsMonthDetail records
# ============================================================
def prepare_norms_month_detail_records(df: pd.DataFrame, header_lookup: dict, fym_map: dict,
                                        existing: dict) -> list:
    """
    Build one NormsMonthDetail record per (header, month) row.
    header_lookup: (plant_id, utility, account, material, issuing_plant) -> header dict
    """
    details = []
    missing_fym = set()

    for _, row in df.iterrows():
        plant_id = row.get("_plant_id")
        if not plant_id:
            continue
        # Mirror the MaterialName fallback applied in prepare_norms_header_records
        # (account name when the source has no material) so keys match.
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

        # FinancialYearMonth.Year is the CALENDAR year of the month itself
        # (verified for the DMD import against the same table), NOT the
        # FY-start year - so the FYM lookup and NormsMonthDetail's stored
        # year must use calendar_year, not resolve_fiscal_year()'s FY-start
        # year. fiscal_year is computed separately below purely for
        # FY-period logging/validation.
        calendar_year = row["calendar_year"]
        fiscal_year = resolve_fiscal_year(row["month_num"], calendar_year)
        fym_id = fym_map.get((row["month_num"], calendar_year))
        if not fym_id:
            missing_fym.add((row["month_num"], calendar_year))
            continue

        detail_key = (header["id"], row["month_num"], calendar_year)
        existing_id = existing["month_details"].get(detail_key)

        # DataFrame.from_records upcasts partially-missing numeric columns to
        # float64, turning None back into float('nan') - normalize before this
        # reaches SQL/DB params (same issue as the NormsHeader string fields).
        quantity = _na_to_none(row["quantity"])
        details.append(
            {
                "id": existing_id or str(uuid.uuid4()).upper(),
                "is_new": existing_id is None,
                "norms_header_id": header["id"],
                "fym_id": fym_id,
                "month_num": row["month_num"],
                "year": calendar_year,
                "fiscal_year": fiscal_year,
                "norms": _na_to_none(row["norms"]),
                "quantity": quantity,
                "qty": quantity,  # No direct source column for QTY; mirrors Quantity (same as DMD import).
                "amount": _na_to_none(row["amount"]),
                "price": _na_to_none(row["price"]),
            }
        )

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
    """
    Roll monthly NormsMonthDetail rows up into one CPPNorms record per
    (header, financial_year), with each month's Norms value in its own column.
    """
    header_by_id = {h["id"]: h for h in headers}

    # Group by (header_id, financial_year_label)
    grouped = {}
    for d in month_details:
        fy_label = _financial_year_label(d["fiscal_year"])
        gkey = (d["norms_header_id"], fy_label)
        grouped.setdefault(gkey, {})[d["month_num"]] = d["norms"]

    records = []
    for (header_id, fy_label), month_norms in grouped.items():
        existing_id = existing["cpp_norms"].get((header_id, fy_label))
        rec = {
            "id": existing_id or str(uuid.uuid4()).upper(),
            "is_new": existing_id is None,
            "norms_header_id": header_id,
            "financial_year": fy_label,
            "aop_year": fy_label,
            "norm_type_fk_id": CPP_NORM_TYPE_FIXED,
            "months": {CPP_MONTH_COLUMN[m]: v for m, v in month_norms.items()},
        }
        records.append(rec)

    logger.info(
        f"Prepared {len(records)} CPPNorms records "
        f"({sum(r['is_new'] for r in records)} new, "
        f"{sum(not r['is_new'] for r in records)} existing)."
    )
    return records


def _financial_year_label(fy_start_year: int) -> str:
    """fy_start_year -> e.g. 2026 -> '2026-27'."""
    return f"{fy_start_year}-{str(fy_start_year + 1)[-2:]}"


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


def generate_sql_file(headers, month_details, cpp_records, check_queries: list,
                       out_path: str = SQL_OUTPUT_FILE, new_norm_parameters: list = None):
    """
    Write SELECT check queries + generated INSERT statements (new records only)
    to a .sql file for manual review. Does not execute anything.
    """
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    lines = []
    lines.append("-- ============================================================")
    lines.append("-- JMD C2 Norms Import - Generated SQL")
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
            "INSERT INTO NormParameters "
            "(Id, Name, DisplayName, UOM, Type, NormParameterType_FK_Id, Plant_FK_Id, NormType_FK_Id, "
            "IsHistorical, DisplayOrder, IsEditable, IsVisible, CalculationType, SAPMaterialCode) VALUES "
            f"({_sql_str(p['id'])}, {_sql_str(p['name'])}, {_sql_str(p['name'])}, {_sql_str(p['uom'])}, "
            f"{_sql_str(p['type'])}, {_sql_str(p['norm_parameter_type_fk_id'])}, {_sql_str(p['plant_id'])}, "
            f"{_sql_num(p['norm_type_fk_id'])}, {_sql_bit(p['is_historical'])}, {_sql_num(p['display_order'])}, "
            f"{_sql_bit(p['is_editable'])}, {_sql_bit(p['is_visible'])}, {_sql_num(p['calculation_type'])}, "
            f"{_sql_str(p['sap_material_code'])});"
        )

    lines.append("")
    lines.append("-- ---------- INSERT: NormsHeader (new only) ----------")
    for h in headers:
        if not h["is_new"]:
            continue
        lines.append(
            "INSERT INTO NormsHeader "
            "(Id, Plant_FK_Id, UtilityName, UtilityId, UtilityUOM, AccountName, MaterialName, MaterialId, "
            "IssuingPlantName, NormParameter_FK_Id, IsActive, IssuingUOM, DisplayOrder, plantCode) VALUES "
            f"({_sql_str(h['id'])}, {_sql_str(h['plant_id'])}, {_sql_str(h['utility_name'])}, "
            f"{_sql_str(h['utility_id'])}, {_sql_str(h['utility_uom'])}, {_sql_str(h['account_name'])}, "
            f"{_sql_str(h['material_name'])}, {_sql_str(h['material_id'])}, "
            f"{_sql_str(h['issuing_plant_name'])}, "
            f"{_sql_str(h['norm_parameter_id'])}, 1, {_sql_str(h['issuing_uom'])}, "
            f"{_sql_num(h['display_order'])}, {_sql_str(h['plant_code'])});"
        )

    lines.append("")
    lines.append("-- ---------- INSERT: NormsMonthDetail (new only) ----------")
    for d in month_details:
        if not d["is_new"]:
            continue
        lines.append(
            "INSERT INTO NormsMonthDetail "
            "(Id, NormsHeader_FK_Id, FinancialYearMonth_FK_Id, Norms, Quantity, Amount, Price, QTY) VALUES "
            f"({_sql_str(d['id'])}, {_sql_str(d['norms_header_id'])}, {_sql_str(d['fym_id'])}, "
            f"{_sql_num(d['norms'])}, {_sql_num(d['quantity'])}, {_sql_num(d['amount'])}, "
            f"{_sql_num(d['price'])}, {_sql_num(d['qty'])});"
        )

    lines.append("")
    lines.append("-- ---------- INSERT: CPPNorms (new only) ----------")
    for c in cpp_records:
        if not c["is_new"]:
            continue
        month_cols = ", ".join(c["months"].keys())
        month_vals = ", ".join(_sql_num(v) for v in c["months"].values())
        extra_cols = ", NormsHeader_FK_Id, FinancialYear, AOPYear, NormType_FK_Id, ApplyActualNormToAll, CreatedBy, CreatedDate"
        extra_vals = (
            f", {_sql_str(c['norms_header_id'])}, {_sql_str(c['financial_year'])}, "
            f"{_sql_str(c['aop_year'])}, {c['norm_type_fk_id']}, 0, N'JMDImportScript', GETDATE()"
        )
        lines.append(
            f"INSERT INTO CPPNorms (Id{', ' + month_cols if month_cols else ''}{extra_cols}) VALUES "
            f"({_sql_str(c['id'])}{', ' + month_vals if month_vals else ''}{extra_vals});"
        )

    lines.append("")
    lines.append("-- COMMIT;   -- uncomment to apply after review")
    lines.append("-- ROLLBACK; -- uncomment to discard")

    with open(out_path, "w") as f:
        f.write("\n".join(lines))
    logger.info(f"Generated SQL file written to: {out_path}")
    return out_path


def build_check_queries() -> list:
    plants_in = ", ".join(f"'{p}'" for p in ALLOWED_PLANTS)
    return [
        f"""-- 1. Check JMD C2 plants
SELECT * FROM Plants
WHERE Name IN ({plants_in})
   OR SourceName IN ({plants_in});""",
        f"""-- 2. Check existing norm parameters for JMD C2 plants
SELECT * FROM NormParameters
WHERE Plant_FK_Id IN (
    SELECT Id FROM Plants WHERE Name IN ({plants_in}) OR SourceName IN ({plants_in})
);""",
        f"""-- 3. Check existing NormsHeader records for JMD C2 plants
SELECT nh.*
FROM NormsHeader nh
JOIN Plants p ON nh.Plant_FK_Id = p.Id
WHERE p.Name IN ({plants_in})
   OR p.SourceName IN ({plants_in});""",
        f"""-- 4. Check existing NormsMonthDetail records
SELECT nmd.*
FROM NormsMonthDetail nmd
JOIN NormsHeader nh ON nmd.NormsHeader_FK_Id = nh.Id
JOIN Plants p ON nh.Plant_FK_Id = p.Id
WHERE p.Name IN ({plants_in})
   OR p.SourceName IN ({plants_in});""",
        f"""-- 5. Check existing CPPNorms records
SELECT cn.*
FROM CPPNorms cn
JOIN NormsHeader nh ON cn.NormsHeader_FK_Id = nh.Id
JOIN Plants p ON nh.Plant_FK_Id = p.Id
WHERE p.Name IN ({plants_in})
   OR p.SourceName IN ({plants_in});""",
    ]


# ============================================================
# Validation report
# ============================================================
def print_validation_report(df, plant_ids, headers, month_details, cpp_records, column_problems,
                             new_norm_parameters: list = None):
    new_norm_parameters = new_norm_parameters or []
    total_rows = len(df)
    missing_plants = [p for p in ALLOWED_PLANTS if p not in plant_ids]
    new_headers = [h for h in headers if h["is_new"]]
    existing_headers = [h for h in headers if not h["is_new"]]
    new_details = [d for d in month_details if d["is_new"]]
    existing_details = [d for d in month_details if not d["is_new"]]
    new_cpp = [c for c in cpp_records if c["is_new"]]
    existing_cpp = [c for c in cpp_records if not c["is_new"]]
    headers_missing_material_id = [h for h in headers if h["is_new"] and not h["material_id"]]

    print("\n" + "=" * 70)
    print("JMD C2 NORMS IMPORT - VALIDATION REPORT")
    print("=" * 70)
    print(f"Total CSV rows (month-level, JMD C2 plants only): {total_rows}")
    print(f"Column validation issues: {len(column_problems)}")
    print(f"Missing plants: {missing_plants if missing_plants else 'None'}")
    print()
    print(f"NormParameters - new (to be created): {len(new_norm_parameters)}")
    print(f"NormsHeader  - new: {len(new_headers)}, existing (reused): {len(existing_headers)}")
    print(f"  -> of the new headers, {len(headers_missing_material_id)} have NO Material ID in source "
          f"(MaterialId will be NULL)")
    print(f"NormsMonthDetail - new: {len(new_details)}, existing (will be skipped): {len(existing_details)}")
    print(f"CPPNorms - new: {len(new_cpp)}, existing (will be skipped): {len(existing_cpp)}")

    fy_periods = sorted({(d["month_num"], d["year"]) for d in month_details})
    fy_labels = sorted({_financial_year_label(d["fiscal_year"]) for d in month_details})
    print(f"Financial year(s) covered: {fy_labels}")
    print(f"Calendar (month, year) periods to be written to NormsMonthDetail: {fy_periods}")
    print("=" * 70 + "\n")


# ============================================================
# STEP 11: Insert records (Phase 2 only)
# ============================================================
def insert_records(conn, headers, month_details, cpp_records, plant_ids_created: list = None,
                    new_norm_parameters: list = None):
    """
    Insert only the NEW records inside a single transaction.
    Existing records (matched during validation) are skipped entirely -
    this function never issues UPDATEs, only INSERTs of rows that don't
    already exist.

    plant_ids_created: Ids of any new Plants rows created earlier in this run
    by ensure_missing_plants_exist() (already committed separately, before
    this transaction) - included in the manifest purely so rollback_jmd_norms.py
    can also undo them.

    new_norm_parameters: NormParameters records prepared by
    prepare_norms_header_records() for materials/utilities with no existing
    match. Inserted first (before NormsHeader, which references them via
    NormParameter_FK_Id).

    On successful commit, writes a manifest of every inserted Id (by table)
    to MANIFEST_FILE, so this exact run can be undone later with
    rollback_jmd_norms.py without guessing which rows belong to it.
    """
    cur = conn.cursor()
    inserted = {"norm_parameters": 0, "headers": 0, "month_details": 0, "cpp_norms": 0}
    inserted_ids = {
        "NormsHeader": [], "NormsMonthDetail": [], "CPPNorms": [],
        "Plants": list(plant_ids_created or []),
        "NormParameters": [],
    }
    try:
        for p in new_norm_parameters or []:
            cur.execute(
                """
                INSERT INTO NormParameters
                    (Id, Name, DisplayName, UOM, Type, NormParameterType_FK_Id, Plant_FK_Id, NormType_FK_Id,
                     IsHistorical, DisplayOrder, IsEditable, IsVisible, CalculationType, SAPMaterialCode)
                VALUES (CAST(? AS uniqueidentifier), ?, ?, ?, ?, CAST(? AS uniqueidentifier),
                        CAST(? AS uniqueidentifier), ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    p["id"], p["name"], p["name"], p["uom"], p["type"],
                    p["norm_parameter_type_fk_id"], p["plant_id"], p["norm_type_fk_id"],
                    p["is_historical"], p["display_order"], p["is_editable"], p["is_visible"],
                    p["calculation_type"], p["sap_material_code"],
                ),
            )
            inserted["norm_parameters"] += 1
            inserted_ids["NormParameters"].append(p["id"])

        for h in headers:
            if not h["is_new"]:
                continue
            cur.execute(
                """
                INSERT INTO NormsHeader
                    (Id, Plant_FK_Id, UtilityName, UtilityId, UtilityUOM, AccountName, MaterialName, MaterialId,
                     IssuingPlantName, NormParameter_FK_Id, IsActive, IssuingUOM, DisplayOrder, plantCode)
                VALUES (CAST(? AS uniqueidentifier), CAST(? AS uniqueidentifier), ?, ?, ?, ?, ?, ?, ?,
                        CAST(? AS uniqueidentifier), 1, ?, ?, ?)
                """,
                (
                    h["id"], h["plant_id"], h["utility_name"], h["utility_id"], h["utility_uom"],
                    h["account_name"], h["material_name"], h["material_id"],
                    h["issuing_plant_name"],
                    h["norm_parameter_id"], h["issuing_uom"], h["display_order"], h["plant_code"],
                ),
            )
            inserted["headers"] += 1
            inserted_ids["NormsHeader"].append(h["id"])

        for d in month_details:
            if not d["is_new"]:
                continue
            cur.execute(
                """
                INSERT INTO NormsMonthDetail
                    (Id, NormsHeader_FK_Id, FinancialYearMonth_FK_Id, Norms, Quantity, Amount, Price, QTY)
                VALUES (CAST(? AS uniqueidentifier), CAST(? AS uniqueidentifier), CAST(? AS uniqueidentifier),
                        ?, ?, ?, ?, ?)
                """,
                (
                    d["id"], d["norms_header_id"], d["fym_id"], d["norms"], d["quantity"],
                    d["amount"], d["price"], d["qty"],
                ),
            )
            inserted["month_details"] += 1
            inserted_ids["NormsMonthDetail"].append(d["id"])

        for c in cpp_records:
            if not c["is_new"]:
                continue
            cols = ["Id", "NormsHeader_FK_Id", "FinancialYear", "AOPYear", "NormType_FK_Id",
                    "ApplyActualNormToAll", "CreatedBy", "CreatedDate"] + list(c["months"].keys())
            uuid_cols = {"Id", "NormsHeader_FK_Id"}
            placeholders = ", ".join(
                "CAST(? AS uniqueidentifier)" if col in uuid_cols else "?" for col in cols
            )
            values = [
                c["id"], c["norms_header_id"], c["financial_year"], c["aop_year"],
                c["norm_type_fk_id"], 0, "JMDImportScript", datetime.now(),
            ] + list(c["months"].values())
            cur.execute(
                f"INSERT INTO CPPNorms ({', '.join(cols)}) VALUES ({placeholders})",
                values,
            )
            inserted["cpp_norms"] += 1
            inserted_ids["CPPNorms"].append(c["id"])

        conn.commit()
        logger.info(f"COMMIT successful. Inserted: {inserted}")

        fy_labels = sorted({_financial_year_label(d["fiscal_year"]) for d in month_details})
        periods = sorted({(d["month_num"], d["year"]) for d in month_details})
        if periods:
            first_m, first_y = periods[0]
            last_m, last_y = periods[-1]
            logger.info(
                f"Financial year confirmation: inserted NormsMonthDetail rows for FY {fy_labels} "
                f"spanning {first_m}/{first_y} .. {last_m}/{last_y} "
                f"(expected Apr-Dec of the FY start year, then Jan-Mar of the following calendar year)."
            )

        manifest_path = write_manifest(inserted_ids)
        logger.info(f"Manifest written to: {manifest_path}")
        logger.info(f"To undo this import: python rollback_jmd_norms.py {manifest_path} --execute")
    except Exception:
        rollback_on_error(conn)
        raise
    return inserted


def write_manifest(inserted_ids: dict, out_path: str = MANIFEST_FILE) -> str:
    """
    Persist exactly which rows this run inserted, by table, so a later
    rollback can delete precisely these rows and nothing else.
    """
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    manifest = {
        "generated_at": datetime.now().isoformat(),
        "source_csv": CSV_PATH,
        "allowed_plants": ALLOWED_PLANTS,
        "inserted_ids": inserted_ids,
    }
    with open(out_path, "w") as f:
        json.dump(manifest, f, indent=2)
    return out_path


def rollback_on_error(conn):
    logger.error("Error occurred during insert - rolling back transaction.")
    conn.rollback()


# ============================================================
# MAIN
# ============================================================
def main():
    global DRY_RUN

    parser = argparse.ArgumentParser(description="Import JMD C2 norms data from CSV into the database.")
    parser.add_argument("--execute", action="store_true",
                         help="Actually insert records (Phase 2). Without this flag, runs as a dry run.")
    args = parser.parse_args()
    DRY_RUN = not args.execute

    logger.info(f"Starting JMD C2 norms import. DRY_RUN={DRY_RUN}")

    # ---- Phase 1: Load + validate CSV ----
    raw = pd.read_csv(CSV_PATH, sep=CSV_SEP, encoding=CSV_ENCODING, header=None)
    column_problems = validate_csv_columns(raw.iloc[HEADER_ROW_IDX])

    utility_id_lookup = load_utility_id_lookup()
    df = load_csv_data(utility_id_lookup=utility_id_lookup)
    if df.empty:
        logger.error("No JMD C2 rows found in CSV after filtering. Aborting.")
        return

    # ---- Connect + fetch reference data ----
    conn = get_database_connection()
    try:
        plants_result = ensure_missing_plants_exist(conn, dry_run=DRY_RUN)
        if plants_result["created"]:
            logger.info(
                f"Plants {'that would be created' if DRY_RUN else 'created'}: {plants_result['created']}"
            )
        if plants_result["already_existed"]:
            logger.info(f"Plants already present (no action): {plants_result['already_existed']}")

        plant_ids, plant_code_map = fetch_jmd_plants(conn)
        if DRY_RUN:
            # In a dry run the new plant rows above were never actually inserted,
            # so fetch_jmd_plants can't find them yet - that's expected. Merge in
            # placeholder ids (valid uniqueidentifier format, so downstream SQL
            # lookups don't choke on them) purely so the rest of the dry-run
            # report can proceed and show what the norms import would look like
            # once the plants exist. These never reach an actual INSERT/UPDATE.
            for name in plants_result["created"]:
                plant_ids.setdefault(name, str(uuid.uuid4()).upper())
                plant_code_map.setdefault(name, NEW_PLANTS_TO_CREATE.get(name, {}).get("plant_code"))
        elif len(plant_ids) < len(ALLOWED_PLANTS):
            logger.error("Not all required JMD C2 plants exist in the Plants table. Aborting.")
            return

        param_map = fetch_norm_parameters(conn, plant_ids)
        param_templates = fetch_norm_parameter_template(conn, plant_ids)
        norm_type_reference = fetch_norm_type_reference(conn)
        existing = check_existing_norms(conn, plant_ids)
        fym_map = fetch_financial_year_months(conn)

        # Attach resolved plant_id to every row for downstream steps.
        df["_plant_id"] = df["plant_name"].map(plant_ids)

        headers, new_norm_parameters = prepare_norms_header_records(
            df, plant_ids, plant_code_map, param_map, param_templates, norm_type_reference, existing
        )
        header_lookup = {
            (h["plant_id"], h["utility_name"].upper(), h["account_name"].upper(),
             h["material_name"].upper(), (h["issuing_plant_name"] or "").upper()): h
            for h in headers
        }
        month_details = prepare_norms_month_detail_records(df, header_lookup, fym_map, existing)
        cpp_records = prepare_cpp_norms_records(month_details, headers, existing)

        print_validation_report(df, plant_ids, headers, month_details, cpp_records,
                                 column_problems, new_norm_parameters)

        check_queries = build_check_queries()
        sql_path = generate_sql_file(headers, month_details, cpp_records, check_queries,
                                      new_norm_parameters=new_norm_parameters)
        print(f"Generated SQL saved to: {sql_path}")

        if DRY_RUN:
            logger.info("DRY RUN complete. No database changes were made.")
            logger.info("Review the generated .sql file, then re-run with --execute to insert.")
            return

        # ---- Phase 2: actual insert ----
        logger.info("Executing Phase 2 insert...")
        result = insert_records(conn, headers, month_details, cpp_records,
                                 plant_ids_created=plants_result["created_ids"],
                                 new_norm_parameters=new_norm_parameters)
        logger.info(f"Import complete: {result}")

    finally:
        conn.close()


if __name__ == "__main__":
    main()
