"""
Update DTA CPP zero-norm U4U entries in NormsMonthDetail and CPPNorms for ALL months.

For each month block in DTA_JMD.ODS (April to March FY 2026-27), reverse-calculates
norms for rows where the ODS norm is zero/NaN but Quantity > 0.  Generation for each
producer is derived from a non-zero-norm row of the same producer as qty/norm.

Usage:
    py update_dta_zero_norms_all_months.py          # dry run
    py update_dta_zero_norms_all_months.py --execute # apply updates
"""

import argparse
import math
import os
import sys
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import pandas as pd
from database.connection import get_connection

DTA_PLANT_ID = "A4AF8441-73AD-4F9F-BCF4-6734E8202F7A"
ODS_FILE = r"c:\Users\shrik\Desktop\Project\fork repo\development\New\JMD new python script\wks-platform\apps\python\files\DTA_JMD.ods"

REMARK = "Norm reverse-calculated from DTA_JMD.ODS Quantity"
MODIFIED_BY = "ReverseNormScript"

COL_UTILITY_PLANT = 0
COL_UTILITY = 2
COL_ACCOUNT = 5
COL_MATERIAL = 6

MONTH_BLOCK_START = 11
MONTH_BLOCK_SIZE = 4
_DATA_START_ROW = 4

MONTH_NAME_TO_NUM = {
    "january": 1, "february": 2, "march": 3, "april": 4,
    "may": 5, "june": 6, "july": 7, "august": 8,
    "september": 9, "october": 10, "november": 11, "december": 12,
}

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


def _parse_month_year(month_name):
    """Map April..March in FY 2026-27 to month/year."""
    month = MONTH_NAME_TO_NUM.get(str(month_name).strip().lower())
    if not month:
        return None, None
    # FY 2026-27: April 2026 - March 2027
    year = 2026 if month >= 4 else 2027
    return month, year


def _month_blocks(df):
    """Yield (month_col, month, year) for each 4-column month block."""
    month_cells = []
    for col in range(MONTH_BLOCK_START, len(df.columns), MONTH_BLOCK_SIZE):
        name = df.iloc[2, col]
        month, year = _parse_month_year(name)
        if month:
            month_cells.append((col, month, year))
    return month_cells


def _load_reverse_norms_by_month(df):
    """Return { (month, year, utility_plant, utility, material): reverse_norm }."""
    results = {}
    blocks = _month_blocks(df)

    for month_col, month, year in blocks:
        qty_col = month_col + 1
        norm_col = month_col

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

            norm_val = _to_float(row[norm_col])
            qty_val = _to_float(row[qty_col])

            util_plant = str(row[COL_UTILITY_PLANT]).strip()
            key = (util_plant.strip().lower(), utility.strip().lower())
            if key not in producers:
                producers[key] = {"gen_qty": 0.0, "rows": []}

            # Derive generation from a non-zero norm row for this producer
            if norm_val > 1e-12 and qty_val > 0:
                derived_gen = qty_val / norm_val
                if producers[key]["gen_qty"] == 0.0:
                    producers[key]["gen_qty"] = derived_gen

            producers[key]["rows"].append({
                "utility_plant": util_plant,
                "utility": utility,
                "material": material,
                "ods_norm": norm_val,
                "quantity": qty_val,
            })

        for (_upl, _ul), pdata in producers.items():
            gen_qty = pdata["gen_qty"]
            if gen_qty <= 0:
                continue
            for rec in pdata["rows"]:
                ods_norm = rec["ods_norm"]
                qty = rec["quantity"]
                if (ods_norm == 0 or math.isnan(ods_norm)) and qty != 0:
                    reverse_norm = qty / gen_qty
                    if math.isfinite(reverse_norm) and abs(reverse_norm) > 1e-12:
                        key = (month, year, rec["utility_plant"].strip().lower(), rec["utility"].strip().lower(), rec["material"].strip().lower())
                        results[key] = reverse_norm

    return results


def _resolve_and_update(conn, reverse_map, execute=False):
    cur = conn.cursor()
    now = datetime.now()

    cur.execute("SELECT Id, Month, Year FROM FinancialYearMonth")
    fym_map = {(r[1], r[2]): r[0] for r in cur.fetchall()}

    cur.execute(
        "SELECT Id, Name FROM Plants WHERE TRY_CONVERT(uniqueidentifier, SourceName) = CAST(? AS uniqueidentifier) AND IsActive = 1",
        DTA_PLANT_ID,
    )
    plant_id_to_name = {r[0]: r[1] for r in cur.fetchall()}

    updated_headers = set()
    update_count_nmd = 0
    update_count_cpp = 0
    skipped = 0

    print(f"\n{'Month':<8} {'Year':<6} {'Plant':<30} {'Utility':<20} {'Material':<35} {'New Norm':<16}")
    print("-" * 125)

    for (month, year, util_plant_l, util_l, mat_l), reverse_norm in reverse_map.items():
        fym_id = fym_map.get((month, year))
        if not fym_id:
            print(f"FYM not found for {month:02d}/{year}")
            skipped += 1
            continue

        plant_id = None
        for pid, pname in plant_id_to_name.items():
            if pname.strip().lower() == util_plant_l:
                plant_id = pid
                break
        if not plant_id:
            skipped += 1
            continue

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
            skipped += 1
            continue

        header_id, nmd_id = row

        fy_start = year if month >= 4 else year - 1
        fy_label = f"{fy_start}-{str(fy_start + 1)[-2:]}"
        month_col_name = MONTH_COL_MAP[month]

        cur.execute(
            "SELECT Id FROM CPPNorms WHERE NormsHeader_FK_Id = ? AND FinancialYear = ?",
            header_id, fy_label,
        )
        cpp_row = cur.fetchone()

        print(f"{month:<8} {year:<6} {util_plant_l:<30} {util_l:<20} {mat_l:<35} {reverse_norm:<16.8f}")

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
        print(f"\nUpdates committed: NormsMonthDetail={update_count_nmd}, CPPNorms={update_count_cpp}, NormsHeader={len(updated_headers)}, Skipped={skipped}")
    else:
        print(f"\n[DRY RUN] Would update {len(reverse_map)} month-material combos (Skipped={skipped}). Run with --execute to apply.")


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
