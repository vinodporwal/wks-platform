"""
Import C2 CPPMonthWisePrice from C2_JMD.ods
============================================
Reads the Price columns from C2_JMD.ods and inserts one CPPMonthWisePrice row
per C2 NormsHeader record for FY 2026-27.

Existing CPPMonthWisePrice rows are skipped by default. Use --update-existing to
overwrite them with ODS prices.

Usage:
    python scripts/import_c2_cppmonthwise_price.py              # dry run
    python scripts/import_c2_cppmonthwise_price.py --execute    # insert
    python scripts/import_c2_cppmonthwise_price.py --execute --update-existing

A manifest file is written on execute so the run can be rolled back later.
"""

import argparse
import json
import os
import sys
import uuid
from datetime import datetime
from pathlib import Path

import pandas as pd

# Add JMD app directory to path
JMD_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, JMD_DIR)
from database.connection import get_connection

# ---------------------------------------------------------------------------
# CONFIG
# ---------------------------------------------------------------------------
ODS_PATH = os.path.join(JMD_DIR, "..", "files", "C2_JMD.ods")
FINANCIAL_YEAR = "2026-27"
AOP_YEAR = "2026-27"
VALUE_TYPE = "Price"
PRICE_SOURCE = "C2_JMD.ods"
MODIFIED_BY = "SYSTEM"
REMARKS = "Imported from C2_JMD.ods"

HEADER_ROW_IDX = 3
DATA_START_ROW_IDX = 4

COL_UTILITY_PLANT = 0
COL_UTILITY = 2
COL_ACCOUNT = 5
COL_MATERIAL = 6
COL_ISSUING_PLANT = 9

# C2_JMD.ods month block layout: 6 columns per month (Norms, Qty, Amt, Price, Gen Qty, Rate)
FIRST_MONTH_COL = 11
MONTH_BLOCK_WIDTH = 6
PRICE_OFFSET = 3  # Price is the 4th column in each month block
NUM_MONTHS = 12

MONTH_COLUMNS = [
    ("Apr_Price", 4), ("May_Price", 5), ("Jun_Price", 6), ("Jul_Price", 7),
    ("Aug_Price", 8), ("Sep_Price", 9), ("Oct_Price", 10), ("Nov_Price", 11),
    ("Dec_Price", 12), ("Jan_Price", 1), ("Feb_Price", 2), ("Mar_Price", 3),
]

OUTPUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "output")
MANIFEST_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "manifests")


def _normalize(value):
    return str(value or "").strip().lower()


def _to_float(value):
    try:
        if value is None or pd.isna(value):
            return 0.0
        return float(value)
    except (ValueError, TypeError):
        return 0.0


def _read_ods_price_rows():
    """Return list of ODS price rows with month prices."""
    df = pd.read_excel(ODS_PATH, engine="odf", header=None)
    data = df.iloc[DATA_START_ROW_IDX:, :].copy()
    data = data[data[COL_MATERIAL].notna()]
    data = data[data[COL_MATERIAL].astype(str).str.strip().str.lower() != "total"]

    rows = []
    for _, r in data.iterrows():
        utility_plant = str(r[COL_UTILITY_PLANT]).strip() if pd.notna(r[COL_UTILITY_PLANT]) else ""
        utility = str(r[COL_UTILITY]).strip() if pd.notna(r[COL_UTILITY]) else ""
        account = str(r[COL_ACCOUNT]).strip() if pd.notna(r[COL_ACCOUNT]) else ""
        material = str(r[COL_MATERIAL]).strip() if pd.notna(r[COL_MATERIAL]) else ""
        issuing_plant = str(r[COL_ISSUING_PLANT]).strip() if pd.notna(r[COL_ISSUING_PLANT]) else ""

        # ODS stores blank material cells as "-"; DB stores the account name instead.
        if material == "-":
            material = account

        prices = {}
        for i, (col_name, month_num) in enumerate(MONTH_COLUMNS):
            col_idx = FIRST_MONTH_COL + i * MONTH_BLOCK_WIDTH + PRICE_OFFSET
            prices[col_name] = _to_float(r[col_idx])

        rows.append({
            "utility_plant": utility_plant,
            "utility": utility,
            "account": account,
            "material": material,
            "issuing_plant": issuing_plant,
            "prices": prices,
        })

    return rows


def _load_plant_map(conn):
    """Map Utility Plant name -> Plants.Id for the C2 complex."""
    cur = conn.cursor()
    # Pull every active plant; match by name (case-insensitive) to the ODS Utility Plant.
    cur.execute("SELECT Id, Name FROM Plants WHERE IsActive = 1")
    by_name = {}
    for row in cur.fetchall():
        by_name[_normalize(row[1])] = str(row[0])

    ods_plant_names = {
        "JMD - C2 Utility Plant",
        "JMD - DTA-C2 Power & UTILITY",
        "JMD - C2-GTG 1",
        "JMD - C2-GTG 2",
        "JMD - C2-STG 1",
    }

    mapping = {}
    for name in ods_plant_names:
        norm = _normalize(name)
        # exact normalized match
        if norm in by_name:
            mapping[name] = by_name[norm]
            continue
        # fallback: contains the ODS short token
        for db_name, db_id in by_name.items():
            if norm in db_name or db_name in norm:
                mapping[name] = db_id
                break
    return mapping


def _load_norms_header(conn, plant_ids):
    """Load all active NormsHeader rows for the given plant IDs."""
    cur = conn.cursor()
    placeholders = ",".join("?" * len(plant_ids))
    cur.execute(
        f"""
        SELECT nh.Id, nh.Plant_FK_Id, nh.UtilityName, nh.AccountName, nh.MaterialName, nh.IssuingPlantName, p.Name
        FROM NormsHeader nh
        INNER JOIN Plants p ON p.Id = nh.Plant_FK_Id
        WHERE nh.Plant_FK_Id IN ({placeholders}) AND nh.IsActive = 1
        """,
        list(plant_ids),
    )
    headers = []
    for row in cur.fetchall():
        headers.append({
            "id": str(row[0]),
            "plant_fk_id": str(row[1]).lower(),
            "utility": str(row[2] or "").strip(),
            "account": str(row[3] or "").strip(),
            "material": str(row[4] or "").strip(),
            "issuing_plant": str(row[5] or "").strip(),
            "plant_name": str(row[6] or "").strip(),
        })
    return headers


def _match_rows(ods_rows, norms_headers, plant_map):
    """Match each NormsHeader row to an ODS row. Returns matches + unmatched."""
    # Build reverse plant map: DB plant_id -> list of ODS Utility Plant names
    reverse_plant_map = {}
    for ods_name, pid in plant_map.items():
        reverse_plant_map.setdefault(pid.lower(), []).append(ods_name)

    # Index ODS rows by DB plant_id + normalized (utility, account, material)
    ods_index = {}
    for ods in ods_rows:
        plant_id = plant_map.get(ods["utility_plant"])
        if not plant_id:
            continue
        key = (
            plant_id.lower(),
            _normalize(ods["utility"]),
            _normalize(ods["account"]),
            _normalize(ods["material"]),
        )
        ods_index.setdefault(key, []).append(ods)

    matched = []
    unmatched_db = []
    used_ods_keys = set()

    for nh in norms_headers:
        # Find candidate ODS rows for this NormsHeader's plant
        candidate_keys = []
        for ods_name in reverse_plant_map.get(nh["plant_fk_id"], []):
            key = (
                plant_map[ods_name].lower(),
                _normalize(nh["utility"]),
                _normalize(nh["account"]),
                _normalize(nh["material"]),
            )
            candidate_keys.append(key)

        candidates = []
        for key in candidate_keys:
            if key in ods_index:
                candidates.extend(ods_index[key])
                used_ods_keys.add(key)

        if not candidates:
            unmatched_db.append(nh)
        elif len(candidates) == 1:
            matched.append({"norms_header": nh, "ods": candidates[0]})
        else:
            # Multiple ODS rows match; use issuing plant as tiebreaker, else take first
            selected = candidates[0]
            if nh["issuing_plant"]:
                nh_issuing = _normalize(nh["issuing_plant"])
                for cand in candidates:
                    if _normalize(cand["issuing_plant"]) == nh_issuing:
                        selected = cand
                        break
            matched.append({"norms_header": nh, "ods": selected, "duplicate": True})

    # Find ODS rows that were not matched to any NormsHeader
    unmatched_ods = []
    for ods in ods_rows:
        plant_id = plant_map.get(ods["utility_plant"])
        key = (
            plant_id.lower(),
            _normalize(ods["utility"]),
            _normalize(ods["account"]),
            _normalize(ods["material"]),
        )
        if key not in used_ods_keys:
            unmatched_ods.append(ods)

    return matched, unmatched_db, unmatched_ods


def _load_existing_price_ids(conn, norms_header_ids):
    """Return set of NormsHeader_FK_Id already present in CPPMonthWisePrice for FY."""
    if not norms_header_ids:
        return set()
    cur = conn.cursor()
    placeholders = ",".join("?" * len(norms_header_ids))
    cur.execute(
        f"""
        SELECT DISTINCT NormsHeader_FK_Id
        FROM CPPMonthWisePrice
        WHERE NormsHeader_FK_Id IN ({placeholders}) AND FinancialYear = ?
        """,
        list(norms_header_ids) + [FINANCIAL_YEAR],
    )
    return {str(row[0]) for row in cur.fetchall()}


INSERT_SQL = f"""
INSERT INTO CPPMonthWisePrice (
    Id, NormsHeader_FK_Id, FinancialYear, AOPYear,
    {', '.join(m[0] for m in MONTH_COLUMNS)},
    Remarks, PriceSource, CreatedDate, UpdatedDate, ModifiedBy, ValueType
) VALUES (
    ?, ?, ?, ?,
    {', '.join('?' for _ in MONTH_COLUMNS)},
    ?, ?, ?, ?, ?, ?
)
"""


def _build_insert_params(record_id, nh_id, prices):
    now = datetime.now()
    return [
        record_id,
        nh_id,
        FINANCIAL_YEAR,
        AOP_YEAR,
    ] + [prices[m[0]] for m in MONTH_COLUMNS] + [
        REMARKS,
        PRICE_SOURCE,
        now,
        now,
        MODIFIED_BY,
        VALUE_TYPE,
    ]


def main():
    parser = argparse.ArgumentParser(description="Import C2 CPPMonthWisePrice from ODS")
    parser.add_argument("--execute", action="store_true", help="Actually insert rows")
    parser.add_argument("--update-existing", action="store_true", help="Update existing rows with ODS prices")
    args = parser.parse_args()

    os.makedirs(OUTPUT_DIR, exist_ok=True)
    os.makedirs(MANIFEST_DIR, exist_ok=True)

    print("Reading C2_JMD.ods...")
    ods_rows = _read_ods_price_rows()
    print(f"  ODS data rows: {len(ods_rows)}")

    conn = get_connection()
    cur = conn.cursor()

    print("Loading plant mapping...")
    plant_map = _load_plant_map(conn)
    print(f"  Mapped ODS plants: {plant_map}")

    print("Loading NormsHeader rows...")
    norms_headers = _load_norms_header(conn, list(plant_map.values()))
    print(f"  NormsHeader rows: {len(norms_headers)}")

    print("Matching ODS rows to NormsHeader...")
    matched, unmatched_db, unmatched_ods = _match_rows(ods_rows, norms_headers, plant_map)
    print(f"  Matched: {len(matched)}")
    print(f"  Unmatched DB rows: {len(unmatched_db)}")
    print(f"  Unmatched ODS rows: {len(unmatched_ods)}")

    if unmatched_db:
        print("\nWARNING: NormsHeader rows with no ODS match (will be skipped):")
        for r in unmatched_db[:20]:
            print(f"  {r}")
        if len(unmatched_db) > 20:
            print(f"  ... and {len(unmatched_db) - 20} more")

    if unmatched_ods:
        print("\nWARNING: ODS rows with no NormsHeader match (will be skipped):")
        for r in unmatched_ods[:20]:
            print(f"  {r}")
        if len(unmatched_ods) > 20:
            print(f"  ... and {len(unmatched_ods) - 20} more")

    existing_ids = _load_existing_price_ids(conn, [m["norms_header"]["id"] for m in matched])
    print(f"\nExisting CPPMonthWisePrice rows for {FINANCIAL_YEAR}: {len(existing_ids)}")

    to_insert = []
    to_update = []
    for m in matched:
        nh_id = m["norms_header"]["id"]
        if nh_id in existing_ids:
            if args.update_existing:
                to_update.append(m)
        else:
            to_insert.append(m)

    print(f"Rows to insert: {len(to_insert)}")
    print(f"Rows to update (only with --update-existing): {len(to_update)}")

    if not args.execute:
        print("\n[DRY RUN] No database changes made. Use --execute to apply.")
        print("\nSample INSERT parameters:")
        for m in to_insert[:5]:
            record_id = str(uuid.uuid4()).upper()
            params = _build_insert_params(record_id, m["norms_header"]["id"], m["ods"]["prices"])
            print(f"  {INSERT_SQL.strip()[:80]}...")
            print(f"  params: {params[:8]}...")
        conn.close()
        return

    # Execute mode
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    manifest = {
        "run_at": timestamp,
        "financial_year": FINANCIAL_YEAR,
        "ods_file": ODS_PATH,
        "inserted_ids": [],
        "updated_ids": [],
    }

    sql_output = []
    inserted = 0
    updated = 0
    errors = []

    try:
        for m in to_insert:
            record_id = str(uuid.uuid4()).upper()
            nh_id = m["norms_header"]["id"]
            prices = m["ods"]["prices"]
            params = _build_insert_params(record_id, nh_id, prices)
            sql_output.append((INSERT_SQL, params))
            try:
                cur.execute(INSERT_SQL, params)
                manifest["inserted_ids"].append({"id": record_id, "norms_header_fk_id": nh_id})
                inserted += 1
            except Exception as e:
                errors.append((nh_id, str(e)))

        if args.update_existing:
            update_sql = f"""
                UPDATE CPPMonthWisePrice
                SET {', '.join(f"{col} = ?" for col, _ in MONTH_COLUMNS)},
                    UpdatedDate = ?,
                    Remarks = ?,
                    PriceSource = ?,
                    ModifiedBy = ?,
                    ValueType = ?
                WHERE NormsHeader_FK_Id = ? AND FinancialYear = ?
            """
            for m in to_update:
                nh_id = m["norms_header"]["id"]
                prices = m["ods"]["prices"]
                params = [prices[col] for col, _ in MONTH_COLUMNS] + [
                    datetime.now(), REMARKS, PRICE_SOURCE, MODIFIED_BY, VALUE_TYPE,
                    nh_id, FINANCIAL_YEAR,
                ]
                sql_output.append((update_sql, params))
                try:
                    cur.execute(update_sql, params)
                    manifest["updated_ids"].append(nh_id)
                    updated += 1
                except Exception as e:
                    errors.append((nh_id, str(e)))

        if errors:
            conn.rollback()
            print(f"\nErrors encountered ({len(errors)}). Transaction rolled back.")
            for nh_id, err in errors[:10]:
                print(f"  {nh_id}: {err}")
            return

        conn.commit()
        print(f"\nCommitted: inserted={inserted}, updated={updated}")

        sql_file = os.path.join(OUTPUT_DIR, f"import_c2_cppmonthwise_price_{timestamp}.sql")
        with open(sql_file, "w", encoding="utf-8") as f:
            for sql, params in sql_output:
                f.write(f"-- params: {params}\n")
                f.write(sql)
                f.write("\n")
        print(f"SQL output written: {sql_file}")

        manifest_file = os.path.join(MANIFEST_DIR, f"import_c2_cppmonthwise_price_{timestamp}.json")
        with open(manifest_file, "w", encoding="utf-8") as f:
            json.dump(manifest, f, indent=2)
        print(f"Manifest written: {manifest_file}")

    except Exception as e:
        conn.rollback()
        print(f"\nFatal error. Transaction rolled back: {e}")
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    main()
