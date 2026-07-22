"""
Read C2_JMD.ods and reverse-calculate U4U norms from the ODS's own Quantity and Gen Qty columns.

For each producer (generation utility), the Generation Qty column in the ODS gives the producer's
generation.  For each U4U/Raw Material row under that producer, reverse norm is:

    reverse_norm = Quantity / Gen Qty

Rows where the ODS norm is zero/NaN but Quantity is non-zero are highlighted — these are the
problematic norms that can be corrected from the ODS data.
"""

import sys
import os
import math

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import pandas as pd

ODS_FILE = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "..", "files", "C2_JMD.ods"
)

MONTH = 5   # May
YEAR = 2026

# C2 ODS month block layout
COL_UTILITY_PLANT = 0
COL_UTILITY = 2
COL_ACCOUNT = 5
COL_MATERIAL = 6
COL_MATERIAL_UOM = 8

OFFSET_NORMS = 0
OFFSET_QUANTITY = 1
OFFSET_GEN_QTY = 4

_DATA_START_ROW = 4


def _to_float(value):
    if value is None:
        return 0.0
    if pd.isna(value):
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


def main():
    if not os.path.exists(ODS_FILE):
        print(f"ODS file not found: {ODS_FILE}")
        return

    df = pd.read_excel(ODS_FILE, engine="odf", header=None)
    month_col = _find_month_column(df)
    if month_col is None:
        print(f"Month {MONTH}/{YEAR} not found in ODS file")
        return

    norm_col = month_col + OFFSET_NORMS
    qty_col = month_col + OFFSET_QUANTITY
    gen_col = month_col + OFFSET_GEN_QTY

    # Pass 1: collect rows per producer and resolve a single Gen Qty per producer
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

        norm = _to_float(row[norm_col])
        qty = _to_float(row[qty_col])
        gen_qty = _to_float(row[gen_col])

        if utility not in producers:
            producers[utility] = {
                "gen_qty_candidates": [],
                "rows": []
            }

        producers[utility]["gen_qty_candidates"].append(gen_qty)

        producers[utility]["rows"].append({
            "idx": idx,
            "utility_plant": str(row[COL_UTILITY_PLANT]).strip(),
            "utility": utility,
            "account": account,
            "material": material,
            "material_uom": str(row[COL_MATERIAL_UOM]).strip() if pd.notna(row[COL_MATERIAL_UOM]) else "",
            "ods_norm": norm,
            "quantity": qty,
            "gen_qty": gen_qty,
        })

    # Pick the first non-zero, non-NaN Gen Qty for each producer
    for utility, pdata in producers.items():
        candidates = [g for g in pdata["gen_qty_candidates"] if g > 0]
        pdata["resolved_gen_qty"] = candidates[0] if candidates else 0.0

    print(f"C2 ODS Reverse Norm Calculation — {MONTH:02d}/{YEAR}")
    print(f"ODS file: {ODS_FILE}")
    print(f"Month column index: {month_col}")
    print("=" * 150)
    print(f"{'Producer (Utility)':<30} {'Account':<18} {'Material':<35} {'UOM':<8} {'ODS Norm':<14} {'Quantity':<16} {'Gen Qty':<16} {'Reverse Norm':<16}")
    print("-" * 150)

    zero_norm_rows = []
    total_rows = 0

    for utility in sorted(producers.keys()):
        pdata = producers[utility]
        gen_qty = pdata["resolved_gen_qty"]

        for rec in pdata["rows"]:
            total_rows += 1
            qty = rec["quantity"]
            ods_norm = rec["ods_norm"]

            if gen_qty > 0 and qty != 0:
                reverse_norm = qty / gen_qty
            else:
                reverse_norm = 0.0

            marker = ""
            if (ods_norm == 0 or math.isnan(ods_norm)) and qty != 0:
                marker = " <<<< ZERO/NO ODS NORM"
                zero_norm_rows.append({
                    **rec,
                    "gen_qty": gen_qty,
                    "reverse_norm": reverse_norm,
                })

            norm_display = f"{ods_norm:.6f}" if not math.isnan(ods_norm) else "nan"
            print(f"{rec['utility']:<30} {rec['account']:<18} {rec['material']:<35} {rec['material_uom']:<8} "
                  f"{norm_display:<14} {qty:<16.2f} {gen_qty:<16.2f} {reverse_norm:<16.8f}{marker}")

    print("=" * 150)
    print(f"Total U4U/Raw Material rows: {total_rows}")
    print(f"Rows with zero/NaN ODS norm but non-zero quantity: {len(zero_norm_rows)}")

    if zero_norm_rows:
        print("\nReverse-calculated norms for zero/NaN ODS norm rows:")
        print(f"{'Producer (Utility)':<30} {'Account':<18} {'Material':<35} {'UOM':<8} {'Quantity':<16} {'Gen Qty':<16} {'Reverse Norm':<16}")
        print("-" * 150)
        for r in zero_norm_rows:
            print(f"{r['utility']:<30} {r['account']:<18} {r['material']:<35} {r['material_uom']:<8} "
                  f"{r['quantity']:<16.2f} {r['gen_qty']:<16.2f} {r['reverse_norm']:<16.8f}")


if __name__ == "__main__":
    main()
