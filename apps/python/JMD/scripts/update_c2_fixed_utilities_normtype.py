"""
Mark the 18 C2 CPP fixed-consumption utilities as NormType=10 (Quantity) and zero their norms.

These utilities have blank/0/null norms with non-zero quantity in the authoritative
"Norm, Qty, Cost .csv" (and C2_JMD.ODS), i.e. their quantity is fixed and does not
depend on the generating utility's generation. They must follow the same convention
as SEZ / SEZ-PCG: CPPNorms.NormType_FK_Id = 10 with Norms = 0 in CPPNorms and
NormsMonthDetail. Quantity / QTY / Amount / Price are NOT touched.

Usage:
    py update_c2_fixed_utilities_normtype.py           # dry run
    py update_c2_fixed_utilities_normtype.py --execute # apply
"""

import argparse
import os
import sys
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from database.connection import get_connection

FINANCIAL_YEAR = "2026-27"
REMARK = "Fixed consumption utility; norms set to 0 as per Norm,Qty,Cost CSV (FY 2026-27)"
MODIFIED_BY = "C2FixedUtilityTypeScript"

MONTH_COLS = [
    "Apr_Norms", "May_Norms", "Jun_Norms", "Jul_Norms", "Aug_Norms",
    "Sep_Norms", "Oct_Norms", "Nov_Norms", "Dec_Norms",
    "Jan_Norms", "Feb_Norms", "Mar_Norms",
]

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


def _verify_headers(cur):
    """Identity + structure pre-checks. Returns per-header CPPNorms row id, or aborts."""
    cpp_ids = {}
    problems = []
    for hid, plant, util, acct, mat in HEADERS:
        cur.execute(
            """SELECT p.Name, nh.UtilityName, nh.AccountName, nh.MaterialName, nh.IsActive
               FROM NormsHeader nh WITH (NOLOCK)
               JOIN Plants p WITH (NOLOCK) ON p.Id = nh.Plant_FK_Id
               WHERE nh.Id = CAST(? AS uniqueidentifier)""",
            hid,
        )
        row = cur.fetchone()
        if not row:
            problems.append(f"NormsHeader {hid} not found")
            continue
        if (row[0].strip(), row[1].strip(), row[2].strip(), row[3].strip()) != (plant, util, acct, mat) or not row[4]:
            problems.append(f"Identity mismatch for {hid}: DB=({row[0]!r},{row[1]!r},{row[2]!r},{row[3]!r},active={row[4]})")
            continue

        cur.execute(
            """SELECT Id, NormType_FK_Id FROM CPPNorms WITH (NOLOCK)
               WHERE NormsHeader_FK_Id = CAST(? AS uniqueidentifier) AND FinancialYear = ?""",
            hid, FINANCIAL_YEAR,
        )
        cpp = cur.fetchall()
        if len(cpp) != 1:
            problems.append(f"{util}/{mat}: expected exactly 1 CPPNorms row for FY {FINANCIAL_YEAR}, found {len(cpp)}")
            continue
        if cpp[0][1] not in (6, 9, 10):
            problems.append(f"{util}/{mat}: unexpected current NormType {cpp[0][1]} (expected 6/9/10)")
            continue

        cur.execute(
            """SELECT COUNT(*) FROM NormsMonthDetail nmd WITH (NOLOCK)
               JOIN FinancialYearMonth fym WITH (NOLOCK) ON fym.Id = nmd.FinancialYearMonth_FK_Id
               WHERE nmd.NormsHeader_FK_Id = CAST(? AS uniqueidentifier)
                 AND ((fym.Year = 2026 AND fym.Month >= 4) OR (fym.Year = 2027 AND fym.Month <= 3))""",
            hid,
        )
        nmd_count = cur.fetchone()[0]
        if nmd_count != 12:
            problems.append(f"{util}/{mat}: expected 12 NormsMonthDetail rows in FY {FINANCIAL_YEAR}, found {nmd_count}")
            continue

        cpp_ids[hid] = cpp[0][0]

    if problems:
        print("PRE-CHECK FAILURES:")
        for p in problems:
            print("  " + p)
        sys.exit(1)
    print(f"Pre-checks passed: {len(cpp_ids)}/18 headers verified (identity, 1 CPPNorms row, 12 NMD rows each).")
    return cpp_ids


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--execute", action="store_true", help="Actually apply updates")
    args = parser.parse_args()

    conn = get_connection()
    cur = conn.cursor()
    now = datetime.now()

    try:
        cpp_ids = _verify_headers(cur)

        print(f"\n{'Plant':<22} {'Utility':<17} {'Material':<32} {'CurType':>7} {'Apr_Norms':>12} {'Jan_Norms':>12}  ->  NewType  NewNorms")
        print("-" * 130)
        for hid, plant, util, acct, mat in HEADERS:
            cur.execute(
                """SELECT NormType_FK_Id, Apr_Norms, Jan_Norms FROM CPPNorms WITH (NOLOCK)
                   WHERE NormsHeader_FK_Id = CAST(? AS uniqueidentifier) AND FinancialYear = ?""",
                hid, FINANCIAL_YEAR,
            )
            r = cur.fetchone()
            print(f"{plant:<22} {util:<17} {mat[:32]:<32} {r[0]:>7} {float(r[1] or 0):>12.8f} {float(r[2] or 0):>12.8f}  ->    10        0")

        if not args.execute:
            print("\n[DRY RUN] Would apply to exactly these 18 headers:")
            print("  CPPNorms        : NormType_FK_Id=10, all 12 month norms=0, Remarks/ModifiedBy/ModifiedDate  (18 rows)")
            print("  NormsMonthDetail: Norms=0 only (Quantity/QTY/Amount/Price untouched)                      (216 rows)")
            print("  NormsHeader     : Remarks updated for traceability                                    (18 rows)")
            print("Re-run with --execute to apply.")
            return

        updated_cpp = updated_nmd = updated_nh = 0
        for hid, plant, util, acct, mat in HEADERS:
            cols = ", ".join(f"{c} = 0" for c in MONTH_COLS)
            cur.execute(
                f"""UPDATE CPPNorms
                    SET NormType_FK_Id = 10, {cols},
                        Remarks = ?, ModifiedBy = ?, ModifiedDate = ?
                    WHERE NormsHeader_FK_Id = CAST(? AS uniqueidentifier) AND FinancialYear = ?""",
                REMARK, MODIFIED_BY, now, hid, FINANCIAL_YEAR,
            )
            updated_cpp += cur.rowcount

            cur.execute(
                """UPDATE nmd SET nmd.Norms = 0
                   FROM NormsMonthDetail nmd
                   JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
                   WHERE nmd.NormsHeader_FK_Id = CAST(? AS uniqueidentifier)
                     AND ((fym.Year = 2026 AND fym.Month >= 4) OR (fym.Year = 2027 AND fym.Month <= 3))""",
                hid,
            )
            updated_nmd += cur.rowcount

            cur.execute(
                "UPDATE NormsHeader SET Remarks = ? WHERE Id = CAST(? AS uniqueidentifier)",
                REMARK, hid,
            )
            updated_nh += cur.rowcount

        expected_cpp, expected_nmd, expected_nh = len(HEADERS), len(HEADERS) * 12, len(HEADERS)
        if (updated_cpp, updated_nmd, updated_nh) != (expected_cpp, expected_nmd, expected_nh):
            raise SystemExit(
                f"ABORT - rowcount mismatch: CPPNorms={updated_cpp}/{expected_cpp}, "
                f"NMD={updated_nmd}/{expected_nmd}, NH={updated_nh}/{expected_nh} (rolled back)"
            )

        conn.commit()
        print(f"\nCommitted: CPPNorms={updated_cpp}, NormsMonthDetail={updated_nmd}, NormsHeader={updated_nh}")

        # Post-verification
        bad = 0
        for hid, plant, util, acct, mat in HEADERS:
            cur.execute(
                f"""SELECT NormType_FK_Id, {', '.join(MONTH_COLS)} FROM CPPNorms
                    WHERE NormsHeader_FK_Id = CAST(? AS uniqueidentifier) AND FinancialYear = ?""",
                hid, FINANCIAL_YEAR,
            )
            r = cur.fetchone()
            if r[0] != 10 or any(float(v or 0) != 0.0 for v in r[1:]):
                bad += 1
                print(f"  POST-CHECK FAIL (CPPNorms): {util}/{mat}")
            cur.execute(
                """SELECT COUNT(*) FROM NormsMonthDetail nmd
                   JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
                   WHERE nmd.NormsHeader_FK_Id = CAST(? AS uniqueidentifier)
                     AND ((fym.Year = 2026 AND fym.Month >= 4) OR (fym.Year = 2027 AND fym.Month <= 3))
                     AND (nmd.Norms IS NOT NULL AND nmd.Norms <> 0)""",
                hid,
            )
            if cur.fetchone()[0] != 0:
                bad += 1
                print(f"  POST-CHECK FAIL (NMD): {util}/{mat}")
        print("Post-verification:", "ALL 18 HEADERS OK (type=10, norms=0)" if bad == 0 else f"{bad} FAILURES")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
