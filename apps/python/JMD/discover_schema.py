"""
Run once to print columns for the three plant-asset-hours tables.
    python3 discover_schema.py
"""
import sys, os
sys.path.insert(0, os.path.dirname(__file__))

from database.connection import get_connection

TABLES = [
    "Plants",
    "PowerGenerationAssets",
    "CPPAssetOperationalHours",
    "CPPPowerAssetPriority",
    "CPPSteamGenerationAsset",
    "CPPPowerAssetPriority",
    "CPPSteamAssetsPriority",
]

conn = get_connection()
cur  = conn.cursor()
try:
    for tbl in TABLES:
        cur.execute(
            "SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
            "WHERE TABLE_NAME = ? ORDER BY ORDINAL_POSITION",
            (tbl,),
        )
        rows = cur.fetchall()
        if rows:
            print(f"\n[{tbl}]")
            for r in rows:
                print(f"  {r[0]}  ({r[1]})")
        else:
            print(f"\n[{tbl}]  *** NOT FOUND ***")
finally:
    conn.close()
