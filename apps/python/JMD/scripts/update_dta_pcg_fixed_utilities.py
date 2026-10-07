"""
Mark the 17 DTA-PCG CPP fixed-consumption utilities as NormType=10 (Quantity),
zero their norms, and sync NormsMonthDetail.Quantity from DTA-PCG_JMD.ODS for
ALL 12 months of FY 2026-27.

These utilities have blank/0/null norms with non-zero quantity in
DTA-PCG_JMD.ODS.  With NormType=6 ("Fixed" = norm x generation) or 9
("Calculated"), the engine computes consumption = norm x generation = 0, so the
planned fixed quantities were silently dropped from the U4U chain (seen in the
DTA-PCG Apr-2026 run: e.g. GT2 Cooling Water / NITROGEN_ASU / Utility Water and
the Catalyst & Chemicals rows all showed 0.000000 norm -> 0.00 consumption).

Same convention as DTA / SEZ / SEZ-PCG / C2: CPPNorms.NormType_FK_Id = 10 with
Norms = 0 in CPPNorms and NormsMonthDetail.  Quantity is set from the ODS
(already identical in DB at the time of writing - kept as a verified sync).
QTY / Amount / Price are NOT touched.

Usage:
    py update_dta_pcg_fixed_utilities.py           # dry run
    py update_dta_pcg_fixed_utilities.py --execute # apply
"""

import argparse
import os
import sys
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import pandas as pd
from database.connection import get_connection

FINANCIAL_YEAR = "2026-27"
REMARK = "Fixed consumption utility; norms set to 0 as per DTA-PCG_JMD.ods (FY 2026-27)"
MODIFIED_BY = "DTAPCGFixedUtilityTypeScript"

MONTH_COLS = [
    "Apr_Norms", "May_Norms", "Jun_Norms", "Jul_Norms", "Aug_Norms",
    "Sep_Norms", "Oct_Norms", "Nov_Norms", "Dec_Norms",
    "Jan_Norms", "Feb_Norms", "Mar_Norms",
]

MONTHS = [(4, 2026), (5, 2026), (6, 2026), (7, 2026), (8, 2026), (9, 2026), (10, 2026),
          (11, 2026), (12, 2026), (1, 2027), (2, 2027), (3, 2027)]

FY_WHERE = "((fym.Year = 2026 AND fym.Month >= 4) OR (fym.Year = 2027 AND fym.Month <= 3))"

ODS_FILE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "..", "files", "DTA-PCG_JMD.ods")
# DB stores "Catalyst & Chemicals"; the ODS uses "Catalyst & Chemical".
ODS_ACCT = {"Catalyst & Chemicals": "Catalyst & Chemical"}

# The 17 verified NormsHeader rows (RIL-JW Plant-DTA PCG only):
# (Id, Plant, Utility, Account, Material, IssuingPlant)
HEADERS = [
    ("72CA64EA-1E3A-4490-8CC4-91F370ED65CD", "RIL-JW Plant-DTA PCG", "COMPRESED AIR_ASU", "Utilities", "Cooling Water", "RIL-JW Plant-DTA PCG"),
    ("358E9A5C-7A4C-4FFC-87D0-D0D3312764E9", "RIL-JW Plant-DTA PCG", "COMPRESED AIR_ASU", "Utilities", "MP Steam_Dis", "RIL-JW Plant-DTA PCG"),
    ("955F7827-273D-41E6-9710-1EDB8DA46AB1", "RIL-JW Plant-DTA PCG", "Condensate", "Catalyst & Chemicals", "ACTIVATED CARBON", "Jamnagar-Rev Proc-DTA"),
    ("1DAEA5D2-B24F-4FED-8FE3-5BE183313E0E", "RIL-JW Plant-DTA PCG", "Cooling Water", "Catalyst & Chemicals", "CHEM ANTHRACITE COAL", "Jamnagar-Rev Proc-DTA"),
    ("CA967822-CEEE-4F6B-B409-5C358EFB7955", "RIL-JW Plant-DTA PCG", "Cooling Water", "Catalyst & Chemicals", "CHEMISORBENT;PURAFIL SELECT", "Jamnagar-Rev Proc-DTA"),
    ("764F5FA3-D720-4C8A-9AF9-C9328E97DCB9", "RIL-JW Plant-DTA PCG", "Cooling Water", "Catalyst & Chemicals", "PURACARB PP-1505", "Jamnagar-Rev Proc-DTA"),
    ("BDD8E9D4-BAFA-4366-8A5F-8F4609DB01C2", "RIL-JW Plant-DTA PCG", "Cooling Water", "Catalyst & Chemicals", "SAND,FILTER;SIZE:0.4-0.5MM", "Jamnagar-Rev Proc-DTA"),
    ("19F3D899-7F60-4BE1-9DA9-3BEE4DD96600", "RIL-JW Plant-DTA PCG", "Cooling Water", "Catalyst & Chemicals", "SAND,SIZE:0.7MM-1.25MM", "Jamnagar-Rev Proc-DTA"),
    ("5EA7E3E4-A937-4FA6-A1EB-3205BBDD1064", "RIL-JW Plant-DTA PCG", "Oxygen", "Catalyst & Chemicals", "CHEM LMS 930S", "Jamnagar-Rev Proc-DTA"),
    ("09139B2E-A63E-471C-8093-37D2629CE009", "RIL-JW Plant-DTA PCG", "Oxygen", "Catalyst & Chemicals", "PERLITE FOR COLD BOX INSULATION", "Jamnagar-Rev Proc-DTA"),
    ("16802DA9-825F-48B4-954C-C26E06618E07", "RIL-JW Plant-DTA PCG", "Oxygen", "Catalyst & Chemicals", "REFRIGERANT R-134 A", "Jamnagar-Rev Proc-DTA"),
    ("960EC98E-B7DF-4589-A9D3-98FB69279CCA", "RIL-JW Plant-DTA PCG", "POWERGEN_GT1", "Utilities", "Cooling Water", "RIL-JW Plant-DTA PCG"),
    ("704FDB25-A71C-44AC-9EC8-879A53A37EC8", "RIL-JW Plant-DTA PCG", "POWERGEN_GT1", "Utilities", "NITROGEN_ASU", "RIL-JW Plant-DTA PCG"),
    ("D111713D-B9F7-490F-8E34-ECF762380372", "RIL-JW Plant-DTA PCG", "POWERGEN_GT1", "Utilities", "Utility Water", "JMD - Utility Plant"),
    ("EF27715C-6C20-4B87-8018-9069CD7C2075", "RIL-JW Plant-DTA PCG", "POWERGEN_GT2", "Utilities", "Cooling Water", "RIL-JW Plant-DTA PCG"),
    ("23926ED8-D86C-4E22-AEC2-5C068B588114", "RIL-JW Plant-DTA PCG", "POWERGEN_GT2", "Utilities", "NITROGEN_ASU", "RIL-JW Plant-DTA PCG"),
    ("751BFAB8-FFD7-4B44-BC69-65736F78EC2D", "RIL-JW Plant-DTA PCG", "POWERGEN_GT2", "Utilities", "Utility Water", "JMD - Utility Plant"),
]


def _num(v):
    t = str(v).strip().replace(",", "") if v is not None else ""
    if t in ("", "nan", "None"):
        return None
    try:
        return float(t)
    except ValueError:
        return None


def _s(v):
    t = str(v).strip() if v is not None else ""
    return "" if t in ("nan", "None") else t


def verify_identity(cur):
    """Verify all 17 headers match expected identity (incl. issuing plant). Exits on failure."""
    problems = []
    for hid, plant, util, acct, mat, issuing in HEADERS:
        cur.execute(
            """SELECT p.Name, nh.UtilityName, nh.AccountName, nh.MaterialName, nh.IssuingPlantName, nh.IsActive
               FROM NormsHeader nh WITH (NOLOCK)
               JOIN Plants p WITH (NOLOCK) ON p.Id = nh.Plant_FK_Id
               WHERE nh.Id = CAST(? AS uniqueidentifier)""", hid)
        row = cur.fetchone()
        if not row:
            problems.append("NormsHeader %s not found" % hid)
            continue
        if (row[0].strip(), row[1].strip(), row[2].strip(), row[3].strip(), (row[4] or "").strip()) != (plant, util, acct, mat, issuing) or not row[5]:
            problems.append("Identity mismatch for %s: DB=(%r,%r,%r,%r,%r,active=%s)" % (hid, row[0], row[1], row[2], row[3], row[4], row[5]))
    if problems:
        print("PRE-CHECK FAILURES:")
        for p in problems:
            print("  " + p)
        sys.exit(1)
    print("Identity verified: %d/17 headers (plant/utility/account/material/issuing)." % len(HEADERS))


def find_ods_row(ods, util, acct, mat, issuing):
    """Resolve the ODS row for a header by identity + issuing plant. None if ambiguous."""
    acct_o = ODS_ACCT.get(acct, acct)
    hits = []
    for i in range(2, len(ods)):
        r = ods.iloc[i]
        if (_s(r[2]), _s(r[5]), _s(r[6]), _s(r[8])) == (util, acct_o, mat, issuing):
            hits.append(i)
    if len(hits) == 1:
        return hits[0]
    if hits:
        # multiple ODS rows with same identity: allow only if monthly qty identical
        qtys = [tuple(_num(ods.iloc[i, 11 + m * 4 + 1]) for m in range(12)) for i in hits]
        if all(q == qtys[0] for q in qtys):
            return hits[0]
    return None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--execute", action="store_true", help="Actually apply updates")
    args = parser.parse_args()

    if not os.path.exists(ODS_FILE):
        print("ODS file not found: %s" % ODS_FILE)
        return

    ods = pd.read_excel(ODS_FILE, engine="odf", header=None, dtype=object)
    conn = get_connection()
    cur = conn.cursor()
    now = datetime.now()

    try:
        verify_identity(cur)

        # structural pre-checks
        problems = []
        for hid, plant, util, acct, mat, issuing in HEADERS:
            cur.execute(
                """SELECT NormType_FK_Id FROM CPPNorms WITH (NOLOCK)
                   WHERE NormsHeader_FK_Id = CAST(? AS uniqueidentifier) AND AOPYear = ?""",
                hid, FINANCIAL_YEAR)
            cpp = cur.fetchall()
            if len(cpp) != 1:
                problems.append("%s/%s [%s]: %d CPPNorms rows for FY %s" % (util, mat, issuing, len(cpp), FINANCIAL_YEAR))
                continue
            if cpp[0][0] not in (6, 9, 10):
                problems.append("%s/%s [%s]: current NormType %s (expected 6/9/10)" % (util, mat, issuing, cpp[0][0]))
                continue
            cur.execute(
                """SELECT COUNT(*) FROM NormsMonthDetail nmd WITH (NOLOCK)
                    JOIN FinancialYearMonth fym WITH (NOLOCK) ON fym.Id = nmd.FinancialYearMonth_FK_Id
                    WHERE nmd.NormsHeader_FK_Id = CAST(? AS uniqueidentifier) AND %s""" % FY_WHERE, hid)
            if cur.fetchone()[0] != 12:
                problems.append("%s/%s [%s]: NMD row count != 12" % (util, mat, issuing))
            # resolve ODS row and confirm its norms are all blank/0
            row_idx = find_ods_row(ods, util, acct, mat, issuing)
            if row_idx is None:
                problems.append("%s/%s [%s]: could not resolve unique ODS row" % (util, mat, issuing))
                continue
            ods_row = ods.iloc[row_idx]
            if any(_num(ods_row[11 + m * 4]) not in (None, 0.0) for m in range(12)):
                problems.append("%s/%s [%s]: ODS norm non-zero - not a fixed-quantity row" % (util, mat, issuing))
        if problems:
            print("PRE-CHECK FAILURES:")
            for p in problems:
                print("  " + p)
            sys.exit(1)
        print("Structural pre-checks passed: 1 CPPNorms row + 12 NMD rows per header, ODS norms all blank/0.")

        t10 = 0
        for hid, *_ in HEADERS:
            cur.execute("""SELECT NormType_FK_Id FROM CPPNorms WITH (NOLOCK)
                           WHERE NormsHeader_FK_Id = CAST(? AS uniqueidentifier) AND AOPYear = ?""",
                        hid, FINANCIAL_YEAR)
            if cur.fetchone()[0] == 10:
                t10 += 1
        print("Current state: %d already type-10, %d type-6/9 -> all become type-10 with norms=0." % (t10, len(HEADERS) - t10))

        # quantity sync diff preview (expected: none - DB already mirrors ODS)
        pending_qty = []
        for hid, plant, util, acct, mat, issuing in HEADERS:
            row_idx = find_ods_row(ods, util, acct, mat, issuing)
            ods_row = ods.iloc[row_idx]
            for mi, (month, year) in enumerate(MONTHS):
                cur.execute(
                    """SELECT nmd.Id, nmd.Quantity FROM NormsMonthDetail nmd WITH (NOLOCK)
                       JOIN FinancialYearMonth fym WITH (NOLOCK) ON fym.Id = nmd.FinancialYearMonth_FK_Id
                       WHERE nmd.NormsHeader_FK_Id = CAST(? AS uniqueidentifier)
                         AND fym.Month = ? AND fym.Year = ?""", hid, month, year)
                db = cur.fetchone()
                if not db:
                    continue
                oq = _num(ods_row[11 + mi * 4 + 1])
                if oq is None:
                    continue  # ODS blank qty month - leave DB as-is
                if abs(float(db[1] or 0) - oq) > 1e-6:
                    pending_qty.append((db[0], util, mat, issuing, month, year, float(db[1] or 0), oq))

        if pending_qty:
            print("\nQuantity cells needing update: %d" % len(pending_qty))
            print("%-28s %-38s %-24s %-8s %16s %14s" % ("Utility", "Material", "Issuing", "Month", "DB Quantity", "ODS Quantity"))
            print("-" * 135)
            for nmd_id, util, mat, issuing, month, year, dbq, oq in pending_qty:
                print("%-28s %-38s %-24s %02d/%-5d %16.4f %14.2f" % (util[:28], mat[:38], issuing[:24], month, year, dbq, oq))
        else:
            print("Quantity check: all 17 headers x 12 months already match ODS - no quantity writes needed.")

        if not args.execute:
            print("\n[DRY RUN] Would apply to exactly these 17 headers:")
            print("  CPPNorms        : NormType_FK_Id=10, all 12 month norms=0, Remarks/ModifiedBy/ModifiedDate  (17 rows)")
            print("  NormsMonthDetail: Norms=0 (204 rows); Quantity synced only where it differs from ODS (%d cells)" % len(pending_qty))
            print("  NormsHeader     : Remarks updated for traceability                                    (17 rows)")
            print("Re-run with --execute to apply.")
            return

        updated_cpp = updated_nmd = updated_nh = 0
        for hid, plant, util, acct, mat, issuing in HEADERS:
            cols = ", ".join("%s = 0" % c for c in MONTH_COLS)
            cur.execute(
                """UPDATE CPPNorms SET NormType_FK_Id = 10, %s,
                    Remarks = ?, ModifiedBy = ?, ModifiedDate = ?
                    WHERE NormsHeader_FK_Id = CAST(? AS uniqueidentifier) AND AOPYear = ?""" % cols,
                REMARK, MODIFIED_BY, now, hid, FINANCIAL_YEAR)
            updated_cpp += cur.rowcount

            cur.execute(
                """UPDATE nmd SET nmd.Norms = 0 FROM NormsMonthDetail nmd
                    JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
                    WHERE nmd.NormsHeader_FK_Id = CAST(? AS uniqueidentifier) AND %s""" % FY_WHERE,
                hid)
            updated_nmd += cur.rowcount

            cur.execute(
                "UPDATE NormsHeader SET Remarks = ? WHERE Id = CAST(? AS uniqueidentifier)",
                REMARK, hid)
            updated_nh += cur.rowcount

        updated_qty = 0
        for nmd_id, util, mat, issuing, month, year, dbq, oq in pending_qty:
            cur.execute("UPDATE NormsMonthDetail SET Quantity = ? WHERE Id = CAST(? AS uniqueidentifier)",
                        oq, nmd_id)
            updated_qty += cur.rowcount

        exp = (17, 17 * 12, 17)
        if (updated_cpp, updated_nmd, updated_nh) != exp or updated_qty != len(pending_qty):
            raise SystemExit("ABORT - rowcount mismatch: CPPNorms=%d/%d, NMD=%d/%d, NH=%d/%d, QTY=%d/%d (rolled back)"
                             % (updated_cpp, exp[0], updated_nmd, exp[1], updated_nh, exp[2], updated_qty, len(pending_qty)))

        conn.commit()
        print("\nCommitted: CPPNorms=%d, NormsMonthDetail.Norms=%d, NMD.Quantity=%d, NormsHeader=%d"
              % (updated_cpp, updated_nmd, updated_qty, updated_nh))

        # post-verification
        bad = 0
        for hid, plant, util, acct, mat, issuing in HEADERS:
            cur.execute(
                """SELECT NormType_FK_Id, %s FROM CPPNorms
                    WHERE NormsHeader_FK_Id = CAST(? AS uniqueidentifier) AND AOPYear = ?""" % ", ".join(MONTH_COLS),
                hid, FINANCIAL_YEAR)
            r = cur.fetchone()
            if r[0] != 10 or any(float(v or 0) != 0.0 for v in r[1:]):
                bad += 1
                print("  POST-CHECK FAIL (CPPNorms): %s/%s [%s]" % (util, mat, issuing))
            cur.execute(
                """SELECT COUNT(*) FROM NormsMonthDetail nmd
                    JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
                    WHERE nmd.NormsHeader_FK_Id = CAST(? AS uniqueidentifier) AND %s
                      AND nmd.Norms IS NOT NULL AND nmd.Norms <> 0""" % FY_WHERE, hid)
            if cur.fetchone()[0] != 0:
                bad += 1
                print("  POST-CHECK FAIL (NMD norms): %s/%s [%s]" % (util, mat, issuing))
            # quantity must match ODS wherever ODS has a value
            row_idx = find_ods_row(ods, util, acct, mat, issuing)
            ods_row = ods.iloc[row_idx]
            for mi, (month, year) in enumerate(MONTHS):
                oq = _num(ods_row[11 + mi * 4 + 1])
                if oq is None:
                    continue
                cur.execute(
                    """SELECT nmd.Quantity FROM NormsMonthDetail nmd
                       JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
                       WHERE nmd.NormsHeader_FK_Id = CAST(? AS uniqueidentifier)
                         AND fym.Month = ? AND fym.Year = ?""", hid, month, year)
                db = cur.fetchone()
                if not db or abs(float(db[0] or 0) - oq) > 1e-6:
                    bad += 1
                    print("  POST-CHECK FAIL (quantity): %s/%s [%s] %02d/%d" % (util, mat, issuing, month, year))
        print("Post-verification:", "ALL 17 HEADERS OK (type=10, norms=0, quantity=ODS)" if bad == 0 else "%d FAILURES" % bad)
    finally:
        conn.close()


if __name__ == "__main__":
    main()
