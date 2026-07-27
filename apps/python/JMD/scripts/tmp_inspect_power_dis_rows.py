"""Inspect C2_JMD.ods rows related to Power_Dis."""
import pandas as pd
import os

ods_path = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "..", "files", "C2_JMD.ods"
)

df = pd.read_excel(ods_path, engine="odf", header=None)

print(f"Total rows: {len(df)}")
print(f"Columns: {len(df.columns)}")

# Find month column for April 2026 (4/2026)
target_val = 2026.04
month_col = None
for col_idx, val in enumerate(df.iloc[1].tolist()):
    try:
        if pd.notna(val) and abs(float(val) - target_val) < 1e-5:
            month_col = col_idx
            break
    except (ValueError, TypeError):
        continue

print(f"Month column for April 2026: {month_col}")

# Search for Power_Dis in any of the first 10 columns
for r in range(4, len(df)):
    for c in range(0, 10):
        val = df.iloc[r, c]
        if pd.notna(val) and "power_dis" in str(val).lower():
            print(f"\nRow {r}: {df.iloc[r, 0:10].tolist()}")
            if month_col is not None:
                norm_col = month_col + 0
                qty_col = month_col + 1
                gen_col = month_col + 4
                print(f"  Norm={df.iloc[r, norm_col]}, Qty={df.iloc[r, qty_col]}, Gen={df.iloc[r, gen_col]}")
            break

# Print all distinct utility names (col 2) for context
print("\nDistinct utility names (col 2):")
utilities = set()
for r in range(4, len(df)):
    u = str(df.iloc[r, 2]).strip()
    if u and u.lower() != "nan":
        utilities.add(u)
print(sorted(utilities))
