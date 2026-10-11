"""Temp: update Fixed_Max 25 -> 180 for DTA-PCG SHP HRSGs (AOPYear 2026-27)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from database.connection import get_connection

DTA_PCG_PLANT_ID = "F6D82E68-C3B6-494F-9905-48F19DC611E3"
FY = "2026-27"
ASSETS = ("HRSG1_LKPL_SHP STEAM", "HRSG2_LKPL_SHP STEAM")

conn = get_connection()
cur = conn.cursor()

cur.execute(
    """
    UPDATE cap
    SET cap.Fixed_Max = 180
    FROM CPPSteamAssetCapacity cap
    JOIN CPPSteamGenerationAsset a
         ON a.AssetId = cap.Asset_FK_Id
    WHERE a.CPPPLANT_FK_Id = ?
      AND cap.AOPYear = ?
      AND a.AssetName IN (?, ?)
      AND cap.Fixed_Max = 25
    """,
    (DTA_PCG_PLANT_ID, FY, *ASSETS),
)
print(f"Rows updated: {cur.rowcount}")

cur.execute(
    """
    SELECT a.AssetName, cap.AOPYear, cap.Fixed_Min, cap.Fixed_Max
    FROM CPPSteamGenerationAsset a
    JOIN CPPSteamAssetCapacity cap ON cap.Asset_FK_Id = a.AssetId
    WHERE a.CPPPLANT_FK_Id = ? AND a.AssetName IN (?, ?)
    ORDER BY a.AssetName, cap.AOPYear
    """,
    (DTA_PCG_PLANT_ID, *ASSETS),
)
for r in cur.fetchall():
    print(f"{r[0]:<28} {r[1]:<8} FixMin={r[2]}  FixMax={r[3]}")

conn.commit()
print("Committed.")
conn.close()
