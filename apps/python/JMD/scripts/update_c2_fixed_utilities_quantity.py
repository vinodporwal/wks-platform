"""
Sync NormsMonthDetail.Quantity for the 18 C2 CPP fixed-consumption utilities
from C2_JMD.ODS (identical to the authoritative "Norm, Qty, Cost .csv") for ALL
12 months of FY 2026-27.

Only cells where the DB value differs from the ODS are updated (minimal writes).
Quantity is the value the engine consumes for fixed (NormType=10) rows and is
preserved by the norms save service, so this update sticks. Norms / QTY /
Amount / Price are NOT touched.

Usage:
    py update_c2_fixed_utilities_quantity.py           # dry run
    py update_c2_fixed_utilities_quantity.py --execute # apply
"""

import argparse
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import pandas as pd
from database.connection import get_connection

ODS_FILE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "..", "files", "C2_JMD.ods")
FINANCIAL_YEAR = "2026-27"

MONTHS = [(4, 2026), (5, 2026), (6, 2026), (7, 2026), (8, 2026), (9, 2026), (10, 2026),
          (11, 2026), (12, 2026), (1, 2027), (2, 2027), (3, 2027)]

# ODS Account label for Catalyst & Chemicals
ODS_ACCT = {"Catalyst & Chemicals": "Catalyst & Chemical"}

# The 18 verified NormsHeader rows (C2 CPP only): (Id, Plant, Utility, Account, Material)
HEADERS = [
    ("28C83878-003A-4878-BF46-A0BB21369241", "JMD - C2 Utility Plant", "COMPRESSED AIR", "Utilities", "Cooling Water"),
    ("10C5B548-51F1-4AD7-BD28-8F88487059CA", "JMD - C2 Utility Plant", "Cooling Water", "Catalyst & Chemicals", "CHEM SODIUM HYPOCHLORITE"),
    ("C4C7C77F-F1C4-4FCC-B4E2-ADF657148A7C", "JMD - C2 Utility Plant", "Cooling Water", "Catalyst & Chemicals", "CHEM SULPHURIC ACID 1.84 KG/M3"),
    ("F43029CD-5B07-4157-AC67-CDC6FF1FEC38", "JMD - C2 Utility Plant", "Cooling Water", "Catalyst & Chemicals", "VASUFLOC - 5596"),
    ("575F167A-D1D1-4B9C-AC4E-25EE28A122C7", "JMD - C2 Utility Plant", "NITROGEN_ASU", "Utilities", "Cooling Water"),
    ("D87E0439-FD66-434D-8CA4-0224B89BCE69", "JMD - C2 Utility Plant", "Oxygen", "Utilities", "Cooling Water"),
    ("E1CC45E9-F9EA-414A-A0E4-04CEB663D774", "JMD - C2 Utility Plant", "Utility Water", "Catalyst & Chemicals", "CHEM CHLORINE"),
    ("84C2110D-9712-4A3D-B7E3-30DE13CACB53", "JMD - C2 Utility Plant", "Utility Water", "Catalyst & Chemicals", "CORROCIL 952S"),
    ("06826A68-54C5-4274-9D53-15C34383E48E", "JMD - C2 Utility Plant", "Utility Water", "Catalyst & Chemicals", "VASUCOR 355"),
    ("A7D57D63-C719-4E7B-BC99-C227C466EC8F", "JMD - C2 Utility Plant", "Utility Water", "Utilities", "Power_Dis"),
    ("0311AC8B-87CD-44E5-B329-144538A5543F", "JMD - C2 Utility Plant", "Cooling Water", "Catalyst & Chemicals", "CHEM  SULFAMIC ACID GRADE GP"),
    ("A4B3B49C-D4CD-4A2E-B96D-2F5AD7C894E0", "JMD - C2 Utility Plant", "Effluent Treated", "Utilities", "Cooling Water"),
    ("4B73E666-5EB7-454C-B577-C08D4359105E", "JMD - C2 Utility Plant", "NITROGEN_ASU", "Utilities", "MP Steam_Dis"),
    ("400A7512-BA30-4EDE-A5F0-2500785389A5", "JMD - C2 Utility Plant", "NITROGEN_ASU", "Utilities", "Ret steam condensate"),
    ("9CC747D6-9519-4F73-964F-95DB13DD3114", "JMD - C2-GTG 1", "POWERGEN", "Utilities", "Cooling Water"),
    ("3F7E8DB0-0FD0-4DFC-A9C7-3C713EBE52B6", "JMD - C2-GTG 1", "POWERGEN", "Utilities", "Utility Water"),
    ("8AAC5729-17E0-4F02-802C-4A4DD947D6EA", "JMD - C2-GTG 2", "POWERGEN", "Utilities", "Cooling Water"),
    ("875D213B-DD91-48B4-B40E-AE334411B39B", "JMD - C2-GTG 2", "POWERGEN", "Utilities", "Utility Water"),
]


def _num(v):
    try:
        return float(str(v).strip().replace(",", ""))
    except (ValueError, TypeError, AttributeError):
        return None


def _find_ods_row(ods, plant, util, acct, mat):
    acct_o = ODS_ACCT.get(acct, acct)
    hits = []
    for r in range(4, len(ods)):
        row = ods.iloc[r]
        p = str(row[0]).strip() if pd.notna(row[0]) else ""
        u = str(row[2]).strip() if pd.notna(row[2]) else ""
        a = str(row[5]).strip() if pd.notna(row[5]) else ""
        m = str(row[6]).strip() if pd.notna(row[6]) else ""
        if p == plant and u == util and a == acct_o and m == mat:
            hits.append(r)
    return hits


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--execute", action="store_true", help="Actually apply updates")
    args = parser.parse_args()

    if not os.path.exists(ODS_FILE):
        print(f"ODS file not found: {ODS_FILE}")
        return

    ods = pd.read_excel(ODS_FILE, engine="odf", header=None, dtype=object)
    conn = get_connection()
    cur = conn.cursor()

    try:
        # Pre-checks: identity, single ODS row, type-10 fixed rows, 12 NMD rows
        problems = []
        pending = []  # (nmd_id, util, mat, month, year, db_qty, ods_qty)
        compared = 0
        for hid, plant, util, acct, mat in HEADERS:
            cur.execute(
                """SELECT p.Name, nh.UtilityName, nh.AccountName, nh.MaterialName, nh.IsActive
                   FROM NormsHeader nh WITH (NOLOCK) JOIN Plants p WITH (NOLOCK) ON p.Id = nh.Plant_FK_Id
                   WHERE nh.Id = CAST(? AS uniqueidentifier)""", hid)
            row = cur.fetchone()
            if not row or (row[0].strip(), row[1].strip(), row[2].strip(), row[3].strip()) != (plant, util, acct, mat) or not row[4]:
                problems.append(f"identity mismatch for {hid}")
                continue
            cur.execute(
                """SELECT NormType_FK_Id FROM CPPNorms WITH (NOLOCK)
                   WHERE NormsHeader_FK_Id = CAST(? AS uniqueidentifier) AND FinancialYear = ?""",
                hid, FINANCIAL_YEAR)
            t = cur.fetchone()
            if not t or t[0] != 10:
                problems.append(f"{util}/{mat}: CPPNorms is not NormType=10 (found {t[0] if t else 'no row'}) — quantity would be overwritten by the engine")
                continue

            od_rows = _find_ods_row(ods, plant, util, acct, mat)
            if len(od_rows) != 1:
                problems.append(f"{util}/{mat}: expected exactly 1 ODS row, found {len(od_rows)}")
                continue
            r = od_rows[0]

            for mi, (month, year) in enumerate(MONTHS):
                compared += 1
                cur.execute(
                    """SELECT nmd.Id, nmd.Quantity FROM NormsMonthDetail nmd WITH (NOLOCK)
                       JOIN FinancialYearMonth fym WITH (NOLOCK) ON fym.Id = nmd.FinancialYearMonth_FK_Id
                       WHERE nmd.NormsHeader_FK_Id = CAST(? AS uniqueidentifier)
                         AND fym.Month = ? AND fym.Year = ?""", hid, month, year)
                db = cur.fetchone()
                if not db:
                    problems.append(f"{util}/{mat}: missing NMD row for {month:02d}/{year}")
                    continue
                ods_q = _num(ods.iloc[r, 11 + mi * 6 + 1])
                if ods_q is None:
                    problems.append(f"{util}/{mat}: ODS quantity not numeric for {month:02d}/{year}")
                    continue
                db_q = float(db[1] or 0)
                if abs(db_q - ods_q) > 1e-6:
                    pending.append((db[0], util, mat, month, year, db_q, ods_q))

        if problems:
            print("PRE-CHECK FAILURES:")
            for p in problems:
                print("  " + p)
            sys.exit(1)

        print(f"Pre-checks passed. Compared {compared} cells (18 headers x 12 months).")
        print(f"Cells needing update: {len(pending)}\n")
        print(f"{'Utility':<17} {'Material':<32} {'Month':<8} {'DB Quantity':>16} {'ODS Quantity':>14}")
        print("-" * 95)
        for nmd_id, util, mat, month, year, dbq, oq in pending:
            print(f"{util:<17} {mat[:32]:<32} {month:02d}/{year:<5} {dbq:>16.4f} {oq:>14.2f}")

        if not args.execute:
            print(f"\n[DRY RUN] Would update {len(pending)} NormsMonthDetail.Quantity values (by row Id; nothing else touched).")
            print("Re-run with --execute to apply.")
            return

        updated = 0
        for nmd_id, util, mat, month, year, dbq, oq in pending:
            cur.execute(
                "UPDATE NormsMonthDetail SET Quantity = ? WHERE Id = CAST(? AS uniqueidentifier)",
                oq, nmd_id)
            updated += cur.rowcount

        if updated != len(pending):
            raise SystemExit(f"ABORT - rowcount mismatch {updated}/{len(pending)} (rolled back)")

        conn.commit()
        print(f"\nCommitted: {updated} NormsMonthDetail.Quantity values updated.")

        # Post-verify: re-compare all cells
        bad = 0
        for hid, plant, util, acct, mat in HEADERS:
            od_rows = _find_ods_row(ods, plant, util, acct, mat)
            if len(od_rows) != 1:
                continue
            r = od_rows[0]
            for mi, (month, year) in enumerate(MONTHS):
                cur.execute(
                    """SELECT nmd.Quantity FROM NormsMonthDetail nmd
                       JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
                       WHERE nmd.NormsHeader_FK_Id = CAST(? AS uniqueidentifier)
                         AND fym.Month = ? AND fym.Year = ?""", hid, month, year)
                db = cur.fetchone()
                ods_q = _num(ods.iloc[r, 11 + mi * 6 + 1])
                if not db or abs(float(db[0] or 0) - (ods_q or 0)) > 1e-6:
                    bad += 1
                    print(f"  POST-CHECK FAIL: {util}/{mat} {month:02d}/{year}")
        print("Post-verification:", "ALL 216 CELLS MATCH ODS" if bad == 0 else f"{bad} FAILURES")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
