"""
Diagnostic for CalculatedProcessDemand — verify plant_code resolution and data.
Run from apps/python/JMD/:  python debug_pdm.py
"""
import sys, os
sys.path.insert(0, os.path.dirname(__file__))

from database.connection import get_connection
from database.queries import fetch_plant_info

PLANT_ID = "F6D82E68-C3B6-494F-9905-48F19DC611E3"  # DTA-PCG-CPP

conn = get_connection()
cur  = conn.cursor()

print("\n--- 1. plant_code from fetch_plant_info ---")
info = fetch_plant_info(PLANT_ID)
print("  plant_info:", info)
plant_code = info.get("plant_code", "")
print("  plant_code:", repr(plant_code))

print("\n--- 2. Distinct cpp_plant_id values in CalculatedProcessDemand ---")
cur.execute("SELECT DISTINCT cpp_plant_id FROM dbo.CalculatedProcessDemand")
for row in cur.fetchall():
    print(" ", repr(row[0]))

print("\n--- 3. Distinct financial_year values ---")
cur.execute("SELECT DISTINCT financial_year FROM dbo.CalculatedProcessDemand")
for row in cur.fetchall():
    print(" ", repr(row[0]))

print("\n--- 4. Direct query with plant_code and FY ---")
cur.execute(
    "SELECT cpp_utility, jun FROM dbo.CalculatedProcessDemand "
    "WHERE financial_year = ? AND cpp_plant_id = ?",
    ("2025-26", plant_code)
)
rows = cur.fetchall()
print(f"  Rows found with '{plant_code}': {len(rows)}")

# Also try without the 'B'
alt_code = plant_code.replace("B", "", 1) if "B" in plant_code else plant_code
cur.execute(
    "SELECT cpp_utility, jun FROM dbo.CalculatedProcessDemand "
    "WHERE financial_year = ? AND cpp_plant_id = ?",
    ("2025-26", alt_code)
)
rows2 = cur.fetchall()
print(f"  Rows found with alt '{alt_code}': {len(rows2)}")
for r in rows2:
    print(" ", r)

print("\n--- 5. Sample rows for cpp_plant_id LIKE '36K%' ---")
cur.execute(
    "SELECT TOP 5 cpp_plant_id, cpp_plant, financial_year, cpp_utility, jun "
    "FROM dbo.CalculatedProcessDemand WHERE cpp_plant_id LIKE '36K%'"
)
for r in cur.fetchall():
    print(" ", r)

conn.close()
