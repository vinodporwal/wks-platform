"""
Update C2 CPP zero-norm U4U entries in NormsMonthDetail and CPPNorms for ALL months.

For each month block in C2_JMD.ODS (April to March), reverse-calculates norms
(Quantity / Gen Qty) for rows where the ODS norm is zero/NaN but Quantity > 0,
then patches NormsMonthDetail and CPPNorms. Adds remarks and timestamps.

Usage:
    py update_c2_zero_norms_all_months.py          # dry run
    py update_c2_zero_norms_all_months.py --execute
"""

import argparse
import math
import os
import sys
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import pandas as pd
from database.connection import get_connection

C2_PLANT_ID = "BA558F95-8A3F-4769-9C78-FF7B6C639DDF"
ODS_FILE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "..", "files", "C2_JMD.ods")

REMARK = "Norm reverse-calculated from C2_JMD.ODS Generation Qty"
MODIFIED_BY = "ReverseNormScript"

COL_UTILITY_PLANT = 0
COL_UTILITY = 2
COL_ACCOUNT = 5
COL_MATERIAL = 6
COL_MATERIAL_UOM = 8
OFFSET_NORMS = 0
OFFSET_QUANTITY = 1
OFFSET_GEN_QTY = 4
_DATA_START_ROW = 4

MONTH_COL_MAP = {
    1: "Jan_Norms", 2: "Feb_Norms", 3: "Mar_Norms",
    4: "Apr_Norms", 5: "May_Norms", 6: "Jun_Norms",
    7: "Jul_Norms", 8: "Aug_Norms", 9: "Sep_Norms",
    10: "Oct_Norms", 11: "Nov_Norms", 12: "Dec_Norms",
}


def _to_float(value):
    if value is None or pd.isna(value):
        return 0.0
    try:
        return float(value)
    except (ValueError, TypeError):
        return 0.0


def _is_total_row(material, account):
    return str(material).strip().lower() == "total" or str(account).strip().lower() == "total"


def _parse_month_year(value):
    """Parse ODS header like 2026.04 -> (month=4, year=2026)."""
    try:
        v = float(value)
        year = int(v)
        month = int(round((v - year) * 100))
        return month, year
    except (ValueError, TypeError):
        return None, None


def _load_reverse_norms_by_month(df):
    """Return { (month, year, utility_plant, utility, material): reverse_norm }."""
    results = {}
    row_1 = df.iloc[1].tolist()

    for block_idx in range(12):
        month_col = 11 + block_idx * 6
        month_year_val = row_1[month_col]
        month, year = _parse_month_year(month_year_val)
        if month is None or year is None:
            print(f"Block {block_idx}: could not parse month/year from {month_year_val}")
            continue

        qty_col = month_col + OFFSET_QUANTITY
        gen_col = month_col + OFFSET_GEN_QTY

        producers = {}
        for idx in range(_DATA_START_ROW, len(df)):
            row = df.iloc[idx]
            utility = str(row[COL_UTILITY]).strip()
            if not utility or utility.lower() == "nan":
                continue

            material = str(row[COL_MATERIAL]).strip()
            account = str(row[COL_ACCOUNT]).strip()
            if _is_total_row(material, account):
                continue

            qty = _to_float(row[qty_col])
            gen_qty = _to_float(row[gen_col])

            if utility not in producers:
                producers[utility] = {"gen_qty": 0.0, "rows": []}
            if gen_qty > 0 and producers[utility]["gen_qty"] == 0:
                producers[utility]["gen_qty"] = gen_qty

            producers[utility]["rows"].append({
                "utility_plant": str(row[COL_UTILITY_PLANT]).strip(),
                "utility": utility,
                "material": material,
                "ods_norm": _to_float(row[month_col]),
                "quantity": qty,
            })

        for utility, pdata in producers.items():
            gen_qty = pdata["gen_qty"]
            for rec in pdata["rows"]:
                qty = rec["quantity"]
                ods_norm = rec["ods_norm"]
                if (ods_norm == 0 or math.isnan(ods_norm)) and qty != 0 and gen_qty > 0:
                    reverse_norm = qty / gen_qty
                    key = (month, year, rec["utility_plant"].strip().lower(), utility.strip().lower(), rec["material"].strip().lower())
                    results[key] = reverse_norm

    return results


def _resolve_and_update(conn, reverse_map, execute=False):
    cur = conn.cursor()
    now = datetime.now()

    # Build FY month id map
    cur.execute("SELECT Id, Month, Year FROM FinancialYearMonth")
    fym_map = {(r[1], r[2]): r[0] for r in cur.fetchall()}

    # Get C2 sub-plants
    cur.execute(
        "SELECT Id, Name FROM Plants WHERE TRY_CONVERT(uniqueidentifier, SourceName) = CAST(? AS uniqueidentifier) AND IsActive = 1",
        C2_PLANT_ID,
    )
    plant_id_to_name = {r[0]: r[1] for r in cur.fetchall()}

    updated_headers = set()
    update_count_nmd = 0
    update_count_cpp = 0

    print(f"\n{'Month':<8} {'Year':<6} {'Plant':<30} {'Utility':<20} {'Material':<35} {'New Norm':<16}")
    print("-" * 125)

    for (month, year, util_plant_l, util_l, mat_l), reverse_norm in reverse_map.items():
        fym_id = fym_map.get((month, year))
        if not fym_id:
            print(f"FYM not found for {month:02d}/{year}")
            continue

        for plant_id, plant_name in plant_id_to_name.items():
            if plant_name.strip().lower() != util_plant_l:
                continue

            # Find NormsHeader and NormsMonthDetail
            cur.execute(
                """
                SELECT nh.Id, nmd.Id
                FROM NormsHeader nh WITH (NOLOCK)
                INNER JOIN NormsMonthDetail nmd WITH (NOLOCK) ON nmd.NormsHeader_FK_Id = nh.Id
                WHERE nh.Plant_FK_Id = ?
                  AND LOWER(nh.UtilityName) = ?
                  AND LOWER(nh.MaterialName) = ?
                  AND nmd.FinancialYearMonth_FK_Id = ?
                  AND nh.IsActive = 1
                """,
                plant_id, util_l, mat_l, fym_id,
            )
            row = cur.fetchone()
            if not row:
                continue

            header_id, nmd_id = row

            # Determine FY label for CPPNorms
            fy_start = year if month >= 4 else year - 1
            fy_label = f"{fy_start}-{str(fy_start + 1)[-2:]}"
            month_col_name = MONTH_COL_MAP[month]

            # Find CPPNorms
            cur.execute(
                "SELECT Id FROM CPPNorms WHERE NormsHeader_FK_Id = ? AND FinancialYear = ?",
                header_id, fy_label,
            )
            cpp_row = cur.fetchone()

            print(f"{month:<8} {year:<6} {plant_name:<30} {util_l:<20} {mat_l:<35} {reverse_norm:<16.8f}")

            if execute:
                cur.execute(
                    "UPDATE NormsMonthDetail SET Norms = ? WHERE Id = CAST(? AS uniqueidentifier)",
                    (reverse_norm, nmd_id),
                )
                update_count_nmd += cur.rowcount

                if header_id not in updated_headers:
                    cur.execute(
                        "UPDATE NormsHeader SET Remarks = ? WHERE Id = CAST(? AS uniqueidentifier)",
                        (REMARK, header_id),
                    )
                    updated_headers.add(header_id)

                if cpp_row:
                    cur.execute(
                        f"""
                        UPDATE CPPNorms
                        SET {month_col_name} = ?, Remarks = ?, ModifiedBy = ?, ModifiedDate = ?
                        WHERE Id = CAST(? AS uniqueidentifier)
                        """,
                        (reverse_norm, REMARK, MODIFIED_BY, now, cpp_row[0]),
                    )
                    update_count_cpp += cur.rowcount

    if execute:
        conn.commit()
        print(f"\n✓ Updates committed: NormsMonthDetail={update_count_nmd}, CPPNorms={update_count_cpp}, NormsHeader={len(updated_headers)}")
    else:
        print(f"\n[DRY RUN] Would update: NormsMonthDetail rows for {len(reverse_map)} month-material combinations.")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--execute", action="store_true", help="Actually apply updates")
    args = parser.parse_args()

    if not os.path.exists(ODS_FILE):
        print(f"ODS file not found: {ODS_FILE}")
        return

    df = pd.read_excel(ODS_FILE, engine="odf", header=None)
    reverse_map = _load_reverse_norms_by_month(df)
    print(f"Total zero-norm month-material combinations to fix: {len(reverse_map)}")

    conn = get_connection()
    try:
        _resolve_and_update(conn, reverse_map, execute=args.execute)
    finally:
        conn.close()


if __name__ == "__main__":
    main()
