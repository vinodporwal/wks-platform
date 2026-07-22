"""
Update C2 CPP zero-norm U4U entries in NormsMonthDetail and CPPNorms.

Calculates reverse norms from C2_JMD.ODS (Quantity / Gen Qty) and patches the
DB for the specific utility-material rows where the ODS/DB norm is zero but the
quantity is non-zero. Adds traceability remarks and updates timestamps.

Usage:
    py update_c2_zero_norms.py          # dry run (default)
    py update_c2_zero_norms.py --execute
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

MONTH = 5
YEAR = 2026
FY_LABEL = "2026-27"
REMARK = "Norm reverse-calculated from C2_JMD.ODS Generation Qty"
MODIFIED_BY = "ReverseNormScript"

# ODS layout (C2)
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


def _find_month_column(df):
    target_val = float(YEAR) + float(MONTH) / 100.0
    row_1 = df.iloc[1].tolist()
    for col_idx, val in enumerate(row_1):
        try:
            if pd.notna(val) and abs(float(val) - target_val) < 1e-5:
                return col_idx
        except (ValueError, TypeError):
            continue
    return None


def _is_total_row(material, account):
    return str(material).strip().lower() == "total" or str(account).strip().lower() == "total"


def _load_reverse_norms_from_ods():
    """Return list of dicts with reverse-calculated zero-norm rows."""
    df = pd.read_excel(ODS_FILE, engine="odf", header=None)
    month_col = _find_month_column(df)
    if month_col is None:
        raise ValueError(f"Month {MONTH}/{YEAR} not found in {ODS_FILE}")

    norm_col = month_col + OFFSET_NORMS
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
            "account": account,
            "material": material,
            "material_uom": str(row[COL_MATERIAL_UOM]).strip() if pd.notna(row[COL_MATERIAL_UOM]) else "",
            "ods_norm": _to_float(row[norm_col]),
            "quantity": qty,
        })

    results = []
    for utility, pdata in producers.items():
        gen_qty = pdata["gen_qty"]
        for rec in pdata["rows"]:
            qty = rec["quantity"]
            ods_norm = rec["ods_norm"]
            if (ods_norm == 0 or math.isnan(ods_norm)) and qty != 0 and gen_qty > 0:
                reverse_norm = qty / gen_qty
                results.append({
                    **rec,
                    "gen_qty": gen_qty,
                    "reverse_norm": reverse_norm,
                })
    return results


def _resolve_db_targets(conn, reverse_rows):
    """Map each reverse row to NormsHeader.Id, NormsMonthDetail.Id, CPPNorms.Id."""
    cur = conn.cursor()

    # Get FinancialYearMonth id for May 2026
    cur.execute("SELECT Id FROM FinancialYearMonth WHERE Month = ? AND Year = ?", (MONTH, YEAR))
    row = cur.fetchone()
    if not row:
        raise ValueError(f"FinancialYearMonth not found for {MONTH}/{YEAR}")
    fym_id = row[0]

    cur.execute(
        "SELECT Id FROM Plants WHERE TRY_CONVERT(uniqueidentifier, SourceName) = CAST(? AS uniqueidentifier) AND IsActive = 1",
        C2_PLANT_ID,
    )
    sub_plant_ids = [r[0] for r in cur.fetchall()]
    if not sub_plant_ids:
        raise ValueError(f"No sub-plants found for C2 plant {C2_PLANT_ID}")

    placeholders = ",".join("?" for _ in sub_plant_ids)
    cur.execute(
        f"SELECT Id, Name FROM Plants WHERE Id IN ({placeholders})",
        sub_plant_ids,
    )
    plant_id_to_name = {r[0]: r[1] for r in cur.fetchall()}

    # Build (plant_name, utility, material) -> reverse_norm map
    # For utility rows in ODS, plant_name is the ODS Utility Plant column.
    # In DB, NormsHeader maps to Plants.Name via Plant_FK_Id.
    reverse_by_key = {}
    for r in reverse_rows:
        key = (r["utility_plant"].strip().lower(), r["utility"].strip().lower(), r["material"].strip().lower())
        reverse_by_key[key] = r

    matched = []

    for plant_id, plant_name in plant_id_to_name.items():
        cur.execute(
            """
            SELECT nh.Id, nh.UtilityName, nh.MaterialName, nmd.Id, nmd.Norms
            FROM NormsHeader nh WITH (NOLOCK)
            INNER JOIN NormsMonthDetail nmd WITH (NOLOCK) ON nmd.NormsHeader_FK_Id = nh.Id
            WHERE nh.Plant_FK_Id = ?
              AND nmd.FinancialYearMonth_FK_Id = ?
              AND nh.IsActive = 1
            """,
            plant_id, fym_id,
        )
        for header_id, utility_name, material_name, nmd_id, current_norm in cur.fetchall():
            key = (plant_name.strip().lower(), str(utility_name).strip().lower(), str(material_name).strip().lower())
            if key in reverse_by_key:
                reverse_row = reverse_by_key[key]
                # Find CPPNorms row
                cur2 = conn.cursor()
                cur2.execute(
                    "SELECT Id, May_Norms FROM CPPNorms WHERE NormsHeader_FK_Id = ? AND FinancialYear = ?",
                    header_id, FY_LABEL,
                )
                cpp_row = cur2.fetchone()
                cpp_id = cpp_row[0] if cpp_row else None
                cpp_current = cpp_row[1] if cpp_row else None

                matched.append({
                    "plant_name": plant_name,
                    "utility_name": utility_name,
                    "material_name": material_name,
                    "header_id": header_id,
                    "nmd_id": nmd_id,
                    "cpp_id": cpp_id,
                    "current_norm": _to_float(current_norm),
                    "reverse_norm": reverse_row["reverse_norm"],
                    "quantity": reverse_row["quantity"],
                    "gen_qty": reverse_row["gen_qty"],
                })

    return matched


def _update_records(conn, matched, execute=False):
    cur = conn.cursor()
    now = datetime.now()
    month_norm_col = MONTH_COL_MAP[MONTH]

    print(f"\n{'Plant':<30} {'Utility':<25} {'Material':<35} {'Current Norm':<14} {'Reverse Norm':<16}")
    print("-" * 130)

    for rec in matched:
        print(f"{rec['plant_name']:<30} {rec['utility_name']:<25} {rec['material_name']:<35} {rec['current_norm']:<14.8f} {rec['reverse_norm']:<16.8f}")

        if execute:
            # Update NormsMonthDetail
            cur.execute(
                "UPDATE NormsMonthDetail SET Norms = ? WHERE Id = CAST(? AS uniqueidentifier)",
                (rec["reverse_norm"], rec["nmd_id"]),
            )

            # Update NormsHeader remarks
            cur.execute(
                "UPDATE NormsHeader SET Remarks = ? WHERE Id = CAST(? AS uniqueidentifier)",
                (REMARK, rec["header_id"]),
            )

            # Update CPPNorms
            if rec["cpp_id"]:
                cur.execute(
                    f"""
                    UPDATE CPPNorms
                    SET {month_norm_col} = ?, Remarks = ?, ModifiedBy = ?, ModifiedDate = ?
                    WHERE Id = CAST(? AS uniqueidentifier)
                    """,
                    (rec["reverse_norm"], REMARK, MODIFIED_BY, now, rec["cpp_id"]),
                )

    if execute:
        conn.commit()
        print("\n✓ Updates committed to database.")
    else:
        print("\n[DRY RUN] No database changes made. Use --execute to apply.")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--execute", action="store_true", help="Actually apply updates")
    args = parser.parse_args()

    if not os.path.exists(ODS_FILE):
        print(f"ODS file not found: {ODS_FILE}")
        return

    reverse_rows = _load_reverse_norms_from_ods()
    print(f"Reverse-calculated zero-norm rows from ODS: {len(reverse_rows)}")

    conn = get_connection()
    try:
        matched = _resolve_db_targets(conn, reverse_rows)
        if not matched:
            print("No matching DB rows found to update.")
            return

        print(f"Matched DB rows: {len(matched)}")
        _update_records(conn, matched, execute=args.execute)
    finally:
        conn.close()


if __name__ == "__main__":
    main()
