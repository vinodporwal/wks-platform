"""
Fetch JMD C2 generation utilities and their U4U consumption norms from DB.

Lists each generation utility (POWERGEN, steam assets, PRDS, etc.) along with
the U4U utilities/raw materials it consumes, the norm, and quantity from
NormsMonthDetail. Highlights zero norms which may be causing missing U4U demand.
"""

import sys
import os

# Add parent directory to path so JMD imports work when script is in scripts/
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from database.queries import fetch_norm_rows_for_jmd

C2_PLANT_ID = "BA558F95-8A3F-4769-9C78-FF7B6C639DDF"
MONTH = 5   # May
YEAR = 2026


def main():
    print(f"Fetching C2 CPP norms (Plant: {C2_PLANT_ID}, Month: {MONTH}, Year: {YEAR})")
    print("=" * 120)

    rows = fetch_norm_rows_for_jmd(C2_PLANT_ID, MONTH, YEAR)
    if not rows:
        print("No rows found.")
        return

    # Group by generation utility
    grouped = {}
    for row in rows:
        utility = row["utility_name"]
        if utility not in grouped:
            grouped[utility] = []
        grouped[utility].append(row)

    print(f"{'Utility (Generation)':<30} {'Plant':<30} {'Account':<15} {'Material (U4U)':<30} {'Norm':<14} {'Qty':<14} {'Material UOM':<12}")
    print("-" * 120)

    zero_norm_rows = []

    for utility in sorted(grouped.keys()):
        for row in grouped[utility]:
            plant = row["plant_name"]
            account = row["account_name"]
            material = row["material_name"]
            norm = row["norm"]
            qty = row["qty"]
            uom = row["material_uom"]

            marker = " <<<< ZERO NORM" if norm == 0 else ""
            print(f"{utility:<30} {plant:<30} {account:<15} {material:<30} {norm:<14.6f} {qty:<14.2f} {uom:<12}{marker}")

            if norm == 0:
                zero_norm_rows.append({
                    "utility": utility,
                    "plant": plant,
                    "account": account,
                    "material": material,
                    "norm": norm,
                    "qty": qty,
                    "uom": uom,
                })

    print("=" * 120)
    print(f"Total rows: {len(rows)}")
    print(f"Zero-norm rows: {len(zero_norm_rows)}")

    if zero_norm_rows:
        print("\nZero-norm U4U entries (these may cause missing U4U demand):")
        print(f"{'Utility (Generation)':<30} {'Plant':<30} {'Account':<15} {'Material (U4U)':<30} {'Qty':<14}")
        print("-" * 120)
        for r in zero_norm_rows:
            print(f"{r['utility']:<30} {r['plant']:<30} {r['account']:<15} {r['material']:<30} {r['qty']:<14.2f}")


if __name__ == "__main__":
    main()
