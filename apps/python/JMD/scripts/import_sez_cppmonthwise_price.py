"""
Import missing SEZ CPPMonthWisePrice rows from SEZ_JMD.ods
==========================================================
For every SEZ NormsHeader record, checks that a CPPMonthWisePrice row exists for
FY 2026-27. Missing rows are inserted with monthly prices read from SEZ_JMD.ods.
Empty ODS cells are treated as 0.

Usage:
    python scripts/import_sez_cppmonthwise_price.py              # dry run
    python scripts/import_sez_cppmonthwise_price.py --execute    # insert
"""

import argparse
import json
import os
import sys
import uuid
from datetime import datetime

import pandas as pd

JMD_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, JMD_DIR)
from database.connection import get_connection

# ---------------------------------------------------------------------------
# CONFIG
# ---------------------------------------------------------------------------
ODS_PATH = os.path.join(JMD_DIR, "..", "files", "SEZ_JMD.ods")
FINANCIAL_YEAR = "2026-27"

HEADER_ROW_IDX = 3
DATA_START_ROW_IDX = 4

COL_UTILITY_PLANT = 0
COL_UTILITY = 2
COL_ACCOUNT = 5
COL_MATERIAL = 6
COL_ISSUING_PLANT = 8

FIRST_MONTH_COL = 11
MONTH_BLOCK_WIDTH = 4
NUM_MONTHS = 12

OFFSET_PRICE = 3

MONTH_BLOCKS = [
    (4, "Apr"), (5, "May"), (6, "Jun"), (7, "Jul"), (8, "Aug"), (9, "Sep"),
    (10, "Oct"), (11, "Nov"), (12, "Dec"), (1, "Jan"), (2, "Feb"), (3, "Mar"),
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


def _read_ods_data():
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

        if material == "-":
            material = account

        monthly_prices = {}
        for i, (month_num, month_name) in enumerate(MONTH_BLOCKS):
            col = FIRST_MONTH_COL + i * MONTH_BLOCK_WIDTH + OFFSET_PRICE
            monthly_prices[month_name] = _to_float(r[col])

        rows.append({
            "utility_plant": utility_plant,
            "utility": utility,
            "account": account,
            "material": material,
            "issuing_plant": issuing_plant,
            "monthly_prices": monthly_prices,
        })
    return rows


def _build_plant_map(ods_rows, db_plants):
    by_name = {_normalize(p["name"]): p["id"] for p in db_plants}

    plant_map = {}
    for ods_name in sorted(set(r["utility_plant"] for r in ods_rows)):
        norm = _normalize(ods_name)
        if norm in by_name:
            plant_map[ods_name] = by_name[norm]
            continue
        short = norm.replace("jmd - ", "").strip()
        if short in by_name:
            plant_map[ods_name] = by_name[short]
            continue
        for db_name, pid in by_name.items():
            db_short = db_name.replace("jmd - ", "").strip()
            if short == db_short or short in db_short or db_short in short:
                plant_map[ods_name] = pid
                break
    return plant_map


def _load_norms_header(conn, plant_ids):
    cur = conn.cursor()
    placeholders = ",".join("?" * len(plant_ids))
    cur.execute(
        f"""
        SELECT nh.Id, nh.Plant_FK_Id, nh.UtilityName, nh.AccountName, nh.MaterialName, nh.IssuingPlantName
        FROM NormsHeader nh
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
        })
    return headers


def _match_rows(ods_rows, norms_headers, plant_map):
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
        key = (
            nh["plant_fk_id"].lower(),
            _normalize(nh["utility"]),
            _normalize(nh["account"]),
            _normalize(nh["material"]),
        )
        candidates = ods_index.get(key, [])

        if not candidates:
            unmatched_db.append(nh)
            continue

        selected = None
        if len(candidates) == 1:
            selected = candidates[0]
        else:
            nh_issuing = _normalize(nh["issuing_plant"])
            for cand in candidates:
                cand_issuing = _normalize(cand["issuing_plant"])
                if cand_issuing == nh_issuing or cand_issuing in nh_issuing or nh_issuing in cand_issuing:
                    selected = cand
                    break
            if not selected:
                selected = candidates[0]

        matched.append({"norms_header": nh, "ods": selected})
        used_ods_keys.add(id(selected))

    unmatched_ods = [ods for ods in ods_rows if id(ods) not in used_ods_keys and ods["utility_plant"] in plant_map]
    return matched, unmatched_db, unmatched_ods


def _load_existing_prices(conn, nh_ids):
    if not nh_ids:
        return set()
    cur = conn.cursor()
    placeholders = ",".join("?" * len(nh_ids))
    cur.execute(
        f"""
        SELECT UPPER(NormsHeader_FK_Id)
        FROM CPPMonthWisePrice
        WHERE NormsHeader_FK_Id IN ({placeholders}) AND FinancialYear = '2026-27'
        """,
        list(nh_ids),
    )
    return {str(row[0]).upper() for row in cur.fetchall()}


INSERT_SQL = """
INSERT INTO CPPMonthWisePrice (
    Id, NormsHeader_FK_Id, FinancialYear, AOPYear,
    Apr_Price, May_Price, Jun_Price, Jul_Price, Aug_Price, Sep_Price,
    Oct_Price, Nov_Price, Dec_Price, Jan_Price, Feb_Price, Mar_Price,
    Remarks, PriceSource, CreatedDate, UpdatedDate, ModifiedBy, ValueType
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE(), GETDATE(), ?, ?)
"""


def _build_insert_params(record_id, nh_id, prices):
    return [
        record_id, nh_id, FINANCIAL_YEAR, FINANCIAL_YEAR,
        prices["Apr"], prices["May"], prices["Jun"], prices["Jul"],
        prices["Aug"], prices["Sep"], prices["Oct"], prices["Nov"],
        prices["Dec"], prices["Jan"], prices["Feb"], prices["Mar"],
        "Imported from SEZ_JMD.ods",
        "SEZ_JMD.ods",
        "SYSTEM",
        "Price",
    ]


def main():
    parser = argparse.ArgumentParser(description="Import missing SEZ CPPMonthWisePrice rows from ODS")
    parser.add_argument("--execute", action="store_true", help="Actually insert rows")
    args = parser.parse_args()

    os.makedirs(OUTPUT_DIR, exist_ok=True)
    os.makedirs(MANIFEST_DIR, exist_ok=True)

    print("Reading SEZ_JMD.ods...")
    ods_rows = _read_ods_data()
    print(f"  ODS data rows: {len(ods_rows)}")

    conn = get_connection()
    cur = conn.cursor()

    print("Loading plants...")
    cur.execute("SELECT Id, Name FROM Plants WHERE IsActive = 1")
    db_plants = [{"id": str(row[0]), "name": str(row[1])} for row in cur.fetchall()]

    print("Building plant mapping...")
    plant_map = _build_plant_map(ods_rows, db_plants)
    print(f"  Mapped ODS Utility Plants: {len(plant_map)}")
    for ods_name, pid in sorted(plant_map.items()):
        print(f"    {ods_name} -> {pid}")

    unmapped = set(r["utility_plant"] for r in ods_rows) - set(plant_map.keys())
    if unmapped:
        print(f"  WARNING: Unmapped ODS Utility Plants: {unmapped}")

    print("Loading SEZ NormsHeader rows...")
    norms_headers = _load_norms_header(conn, list(plant_map.values()))
    print(f"  NormsHeader rows: {len(norms_headers)}")

    print("Matching ODS rows to NormsHeader...")
    matched, unmatched_db, unmatched_ods = _match_rows(ods_rows, norms_headers, plant_map)
    print(f"  Matched: {len(matched)}")
    print(f"  Unmatched DB rows: {len(unmatched_db)}")
    print(f"  Unmatched ODS rows: {len(unmatched_ods)}")

    if unmatched_db:
        print("\nWARNING: NormsHeader rows with no ODS match (first 20):")
        for r in unmatched_db[:20]:
            print(f"  {r}")
    if unmatched_ods:
        print("\nWARNING: ODS rows with no NormsHeader match (first 20):")
        for r in unmatched_ods[:20]:
            print(f"  {r}")

    existing = _load_existing_prices(conn, [m["norms_header"]["id"] for m in matched])
    print(f"  Existing CPPMonthWisePrice rows for 2026-27: {len(existing)}")

    to_insert = [m for m in matched if m["norms_header"]["id"].upper() not in existing]
    print(f"\nRows to insert: {len(to_insert)}")

    if not args.execute:
        print("\n[DRY RUN] No database changes made. Use --execute to apply.")
        print("\nSample rows that would be inserted:")
        for item in to_insert[:10]:
            nh = item["norms_header"]
            print(f"  {nh['utility']} / {nh['material']}: {item['ods']['monthly_prices']}")
        conn.close()
        return

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    manifest = {
        "run_at": timestamp,
        "ods_file": ODS_PATH,
        "financial_year": FINANCIAL_YEAR,
        "inserted_ids": [],
    }
    sql_output = []
    inserted = 0
    errors = []

    try:
        for item in to_insert:
            record_id = str(uuid.uuid4()).upper()
            params = _build_insert_params(record_id, item["norms_header"]["id"], item["ods"]["monthly_prices"])
            sql_output.append((INSERT_SQL.strip(), params))
            try:
                cur.execute(INSERT_SQL, params)
                manifest["inserted_ids"].append({
                    "id": record_id,
                    "norms_header_fk_id": item["norms_header"]["id"],
                })
                inserted += 1
            except Exception as e:
                errors.append((item, str(e)))

        if errors:
            conn.rollback()
            print(f"\nErrors encountered ({len(errors)}). Transaction rolled back.")
            for item, err in errors[:10]:
                print(f"  {item}: {err}")
            return

        conn.commit()
        print(f"\nCommitted: inserted={inserted}")

        sql_file = os.path.join(OUTPUT_DIR, f"import_sez_cppmonthwise_price_{timestamp}.sql")
        with open(sql_file, "w", encoding="utf-8") as f:
            for sql, params in sql_output:
                f.write(f"-- params: {params}\n")
                f.write(sql)
                f.write("\n")
        print(f"SQL output written: {sql_file}")

        manifest_file = os.path.join(MANIFEST_DIR, f"import_sez_cppmonthwise_price_{timestamp}.json")
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
