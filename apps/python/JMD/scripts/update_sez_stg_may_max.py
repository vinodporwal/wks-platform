"""Cap SEZ STG1 May-2026 max capacity to the BPC book dispatch.

Book (Norm/Qty/Cost, May): JMD - SEZ STG Power plant generated
14,175,575.26 KWH -> 14,175,575.26 / 744 h = 19.0535 MW avg.
Current CPPPowerAssetCapacity.May_Max = 26.0 MW lets the model run the
STG at ~25.2 MW (18.7 GWh) — over-generating power AND over-extracting
MP steam (387.7 kMT vs book 354.4 kMT).

Scope: one row — 'JMD - SEZ STG Power plant 1', May_Max only,
AOPYear '2026-27'.  STG2 untouched (already 0 op-hours in May).

Guarded: row must exist, belong to the SEZ CPP plant, and currently
hold May_Max = 26.0.  Run with --execute to apply.
"""

import sys
import pyodbc

sys.path.insert(0, ".")
from config.db_config import DB_CONFIG

SEZ_PLANT = "2DFEE33F-4CFD-4887-B9DD-53388AA95271"
FY = "2026-27"
ASSET_NAME = "JMD - SEZ STG Power plant 1"
EXPECTED_CURRENT = 26.0
BOOK_MAY_KWH = 14_175_575.26
NEW_MAX_MW = round(BOOK_MAY_KWH / 744.0 / 1000.0, 4)   # 19.0535 MW


def main():
    execute = "--execute" in sys.argv
    conn = pyodbc.connect(
        f"DRIVER={{{DB_CONFIG['driver']}}};SERVER={DB_CONFIG['server']};"
        f"DATABASE={DB_CONFIG['database']};UID={DB_CONFIG['username']};"
        f"PWD={DB_CONFIG['password']};Encrypt={DB_CONFIG['encrypt']};"
        f"TrustServerCertificate={DB_CONFIG['trustServerCertificate']};"
    )
    conn.autocommit = False
    cur = conn.cursor()

    print(f"Mode: {'EXECUTE' if execute else 'DRY-RUN'}")
    print(f"Target: {ASSET_NAME}  May_Max {EXPECTED_CURRENT} -> {NEW_MAX_MW} MW"
          f"  (book {BOOK_MAY_KWH:,.2f} KWH / 744 h)\n")

    cur.execute(
        "SELECT AssetId FROM PowerGenerationAssets "
        "WHERE AssetName=? AND CPPPLANT_FK_Id=?", (ASSET_NAME, SEZ_PLANT))
    assets = cur.fetchall()
    if len(assets) != 1:
        print(f"  !! {len(assets)} assets named '{ASSET_NAME}' in SEZ — abort")
        sys.exit(1)
    asset_id = str(assets[0][0])

    cur.execute(
        "SELECT ID, May_Min, May_Max, May_Man_Load FROM CPPPowerAssetCapacity "
        "WHERE Asset_FK_Id=? AND AOPYear=?", (asset_id, FY))
    rows = cur.fetchall()
    if len(rows) != 1:
        print(f"  !! {len(rows)} capacity rows for {FY} — abort")
        sys.exit(1)
    row_id, may_min, may_max, man = str(rows[0][0]), rows[0][1], rows[0][2], rows[0][3]
    print(f"  current: May_Min={may_min}  May_Max={may_max}  Man_Load={man}")
    if abs(float(may_max) - EXPECTED_CURRENT) > 0.001:
        print(f"  !! May_Max is {may_max}, expected {EXPECTED_CURRENT} — abort")
        sys.exit(1)
    if NEW_MAX_MW < float(may_min):
        print(f"  !! new max {NEW_MAX_MW} < May_Min {may_min} — abort")
        sys.exit(1)

    if not execute:
        print("\nDRY-RUN — no change. Re-run with --execute to apply.")
        conn.close()
        return

    cur.execute(
        "UPDATE CPPPowerAssetCapacity SET May_Max=? WHERE ID=?",
        (NEW_MAX_MW, row_id))
    conn.commit()
    print(f"\nCommitted: May_Max = {NEW_MAX_MW} MW  (row {row_id[:8]}…)")

    cur.execute(
        "SELECT a.AssetName, c.May_Min, c.May_Max, c.May_Man_Load "
        "FROM PowerGenerationAssets a "
        "JOIN CPPPowerAssetCapacity c ON c.Asset_FK_Id=a.AssetId AND c.AOPYear=? "
        "WHERE a.CPPPLANT_FK_Id=? ORDER BY a.AssetName", (FY, SEZ_PLANT))
    print("\nPost-update SEZ May capacity:")
    for r in cur.fetchall():
        print(f"  {r[0]:<36} min={r[1]:>5} max={r[2]:>6} man={r[3]}")
    conn.close()


if __name__ == "__main__":
    main()
