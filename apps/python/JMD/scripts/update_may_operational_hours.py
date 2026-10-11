"""Update May-2026 operational hours to match the Norm/Qty/Cost book.

Scope (FY AOPYear = '2026-27', May column only, DTA plant only):

  Power (CPPAssetOperationalHours):
    JMD - GT Power Plant 1 : 744 -> 0    (book has zero generation)
    JMD - GT Power Plant 3 : 0 -> 744    (book runs it, 23.04M KWH)

  Steam (CPPSteamAssetsOperationalHours):
    HRSG1_SHP STEAM   : 744 -> 0         (linked to GT1)
    HRSG3_SHP STEAM   : 0 -> 744         (linked to GT3)
    AUXBOIL1_SHP STEAM / AUXBOIL1_LP STEAM : 744 -> 0  (book off)
    AUXBOIL6_SHP STEAM / AUXBOIL6_LP STEAM : 744 -> 0  (book unbooked)

Every statement is guarded: the row must exist, belong to the DTA CPP
plant, and currently hold the expected May value.  Aborts on any
mismatch before committing.  Run with --execute to apply.
"""

import sys
import pyodbc

sys.path.insert(0, ".")
from config.db_config import DB_CONFIG

DTA_PLANT = "A4AF8441-73AD-4F9F-BCF4-6734E8202F7A"
FY = "2026-27"

# (table, fk column, asset name, expected current May, new May)
CHANGES = [
    ("CPPAssetOperationalHours",       "Asset_FK_Id",      "JMD - GT Power Plant 1", 744.0, 0.0),
    ("CPPAssetOperationalHours",       "Asset_FK_Id",      "JMD - GT Power Plant 3", 0.0,   744.0),
    ("CPPSteamAssetsOperationalHours", "SteamAsset_FK_Id", "HRSG1_SHP STEAM",        744.0, 0.0),
    ("CPPSteamAssetsOperationalHours", "SteamAsset_FK_Id", "HRSG3_SHP STEAM",        0.0,   744.0),
    ("CPPSteamAssetsOperationalHours", "SteamAsset_FK_Id", "AUXBOIL1_SHP STEAM",     744.0, 0.0),
    ("CPPSteamAssetsOperationalHours", "SteamAsset_FK_Id", "AUXBOIL1_LP STEAM",      744.0, 0.0),
    ("CPPSteamAssetsOperationalHours", "SteamAsset_FK_Id", "AUXBOIL6_SHP STEAM",     744.0, 0.0),
    ("CPPSteamAssetsOperationalHours", "SteamAsset_FK_Id", "AUXBOIL6_LP STEAM",      744.0, 0.0),
]

ASSET_TABLE = {
    "Asset_FK_Id":      "PowerGenerationAssets",
    "SteamAsset_FK_Id": "CPPSteamGenerationAsset",
}


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

    print(f"Mode: {'EXECUTE' if execute else 'DRY-RUN'}\n")

    updates = []
    ok = True
    for table, fk_col, name, expected, new_val in CHANGES:
        asset_tbl = ASSET_TABLE[fk_col]
        # 1. resolve asset — must be exactly one DTA row
        cur.execute(
            f"SELECT AssetId FROM {asset_tbl} "
            f"WHERE AssetName=? AND CPPPLANT_FK_Id=?", (name, DTA_PLANT))
        assets = cur.fetchall()
        if len(assets) != 1:
            print(f"  !! {name}: {len(assets)} assets found in DTA — skip")
            ok = False
            continue
        asset_id = str(assets[0][0])

        # 2. fetch the FY-2026-27 hours row
        cur.execute(
            f"SELECT Id, May FROM {table} "
            f"WHERE {fk_col}=? AND AOPYear=?", (asset_id, FY))
        rows = cur.fetchall()
        if len(rows) != 1:
            print(f"  !! {name}: {len(rows)} hours rows for {FY} — skip")
            ok = False
            continue
        row_id, cur_may = str(rows[0][0]), float(rows[0][1] or 0)

        # 3. verify current value matches expectation
        if abs(cur_may - expected) > 0.01:
            print(f"  !! {name}: current May={cur_may}, expected {expected} — skip")
            ok = False
            continue
        print(f"  ok {name:<28} May {cur_may:>7.1f} -> {new_val:>7.1f}"
              f"   ({table} Id={row_id[:8]}…)")
        updates.append((table, row_id, new_val, name))

    if not ok:
        print("\nABORTING — mismatches found, nothing changed.")
        conn.close()
        sys.exit(1)

    if not execute:
        print(f"\nDRY-RUN complete — {len(updates)} updates prepared. "
              f"Re-run with --execute to apply.")
        conn.close()
        return

    for table, row_id, new_val, name in updates:
        cur.execute(f"UPDATE {table} SET May=? WHERE Id=?", (new_val, row_id))
        print(f"  updated {name:<28} ({table})")

    conn.commit()
    print(f"\nCommitted {len(updates)} updates.")

    # ---- post-validation -------------------------------------------------
    print("\nPost-update May state (DTA):")
    cur.execute(
        """SELECT a.AssetName, oh.May
           FROM PowerGenerationAssets a
           LEFT JOIN CPPAssetOperationalHours oh
                  ON oh.Asset_FK_Id = a.AssetId AND oh.AOPYear = ?
           WHERE a.CPPPLANT_FK_Id = ?
           ORDER BY a.AssetName""", (FY, DTA_PLANT))
    for r in cur.fetchall():
        print(f"  PWR {r[0]:<36} May={r[1]}")
    cur.execute(
        """SELECT a.AssetName, oh.May
           FROM CPPSteamGenerationAsset a
           LEFT JOIN CPPSteamAssetsOperationalHours oh
                  ON oh.SteamAsset_FK_Id = a.AssetId AND oh.AOPYear = ?
           WHERE a.CPPPLANT_FK_Id = ? AND a.AssetName LIKE '%SHP%'
           ORDER BY a.AssetName""", (FY, DTA_PLANT))
    for r in cur.fetchall():
        print(f"  STM {r[0]:<36} May={r[1]}")

    conn.close()


if __name__ == "__main__":
    main()
