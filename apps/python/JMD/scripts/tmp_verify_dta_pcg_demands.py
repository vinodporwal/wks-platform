"""
Verify DTA-PCG-CPP process + fixed demands straight from the DB
(RIL.AOP) and compare with the values used by the model in the
latest run log DTA_PCG_CPP_2026_04_20260918_091809.log.

Replicates exactly:
  - fetch_process_demands_raw()   (ProcessDemandMaster JOIN CalculatedProcessDemand)
  - fetch_fixed_consumption_raw() (CPPFixedConsumption JOIN Plants/NormParameters)
for plant F6D82E68-C3B6-494F-9905-48F19DC611E3, month 4, year 2026 (FY 2026-27).
"""

import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from database.connection import get_connection

PLANT_ID = "F6D82E68-C3B6-494F-9905-48F19DC611E3"  # DTA-PCG-CPP
MONTH, YEAR = 4, 2026
FY = "2026-27"
COL = "apr"

# Expected values as logged in [DEMAND] rollup of the latest run
EXPECTED = {
    "Boiler Feed Water":     (697055.00, 0.00),
    "COMPRESED AIR_ASU":     (9413458.00, 0.00),
    "Condensate":            (49284.00, 0.00),
    "Cooling Water":         (3207.00, 12300.00),
    "D M Water":             (13020.00, 0.00),
    "Gasifier_ SHP Steam":   (-316178.00, 0.00),
    "HP Steam_Dis":          (-45063.00, 9458.00),
    "IP Steam_Dis":          (18926.00, 7056.00),
    "LLP Steam Dis":         (-69125.00, 23650.00),
    "LP Steam_Dis":          (-14606.00, 0.00),
    "MP Steam_Dis":          (18456.00, 0.00),
    "NITROGEN_ASU":          (20477065.00, 0.00),
    "Oxygen":                (232366.00, 0.00),
    "Power_Dis":             (50394.45, 0.00),
    "Ret steam condensate":  (-128746.00, 0.00),
    "SHP Steam_Dis":         (3583.00, 8485.00),
    "Utility Water":         (56452.00, 0.00),
}


def fetch_process_detail():
    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT
                m.process_plant,
                m.cpp_utility,
                m.uom,
                ISNULL(c.[{COL}], 0) AS demand,
                c.financial_year
            FROM dbo.ProcessDemandMaster m
            LEFT JOIN dbo.CalculatedProcessDemand c
                ON  m.process_plant_id          = c.process_plant_id
                AND m.cpp_utility_id            = c.cpp_utility_id
                AND ISNULL(m.cpp_plant_id, '')  = ISNULL(c.cpp_plant_id, '')
                AND c.financial_year            = ?
            WHERE m.cpp_plant_fK_id = ?
              AND m.is_active       = 1
            ORDER BY m.cpp_utility, m.process_plant
            """,
            (FY, PLANT_ID),
        )
        return cur.fetchall()
    finally:
        conn.close()


def fetch_fixed_detail():
    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            f"""
            SELECT
                COALESCE(np.Name, '') AS utility_name,
                COALESCE(np.UOM, '')  AS uom,
                SUM(ISNULL(fc.[{COL}], 0)) AS total_consumption,
                COUNT(*) AS rows
            FROM dbo.CPPFixedConsumption fc
            INNER JOIN dbo.Plants p ON p.Id = fc.Plant_FK_Id
            LEFT JOIN dbo.NormParameters np ON np.Id = fc.NormParameter_FK_Id
            WHERE p.SourceName = ?
              AND fc.AOPYear = ?
            GROUP BY np.Name, np.UOM
            ORDER BY np.Name
            """,
            (PLANT_ID, FY),
        )
        rows = cur.fetchall()
        if rows:
            return rows, "SourceName join"
        cur.execute(
            f"""
            SELECT
                COALESCE(np.Name, '') AS utility_name,
                COALESCE(np.UOM, '')  AS uom,
                SUM(ISNULL(fc.[{COL}], 0)) AS total_consumption,
                COUNT(*) AS rows
            FROM dbo.CPPFixedConsumption fc
            LEFT JOIN dbo.NormParameters np ON np.Id = fc.NormParameter_FK_Id
            WHERE fc.Plant_FK_Id = ?
              AND fc.AOPYear = ?
            GROUP BY np.Name, np.UOM
            ORDER BY np.Name
            """,
            (PLANT_ID, FY),
        )
        return cur.fetchall(), "Plant_FK_Id direct"
    finally:
        conn.close()


def main():
    print("=" * 100)
    print(f"DB VERIFICATION — DTA-PCG-CPP  ({PLANT_ID})  month {MONTH}/{YEAR}  FY {FY}")
    print("=" * 100)

    # ------------------------------------------------------------------
    print("\n--- PROCESS DEMAND (ProcessDemandMaster x CalculatedProcessDemand) ---")
    proc_rows = fetch_process_detail()
    process: dict = {}
    for r in proc_rows:
        plant, util, uom, val, fy = r[0], (r[1] or "").strip(), r[2], float(r[3] or 0), r[4]
        process[util] = process.get(util, 0.0) + val
        print(f"  {plant or '(no process plant)':<38} {util:<30} {uom or '':<8} {val:>18,.2f}  (FY={fy})")

    # ------------------------------------------------------------------
    print("\n--- FIXED CONSUMPTION (CPPFixedConsumption x NormParameters) ---")
    fixed_rows, join_mode = fetch_fixed_detail()
    fixed: dict = {}
    print(f"  (join mode: {join_mode})")
    for r in fixed_rows:
        util, uom, val, n = (r[0] or "").strip(), (r[1] or "").strip(), float(r[2] or 0), r[3]
        if uom.upper() == "KWH":
            val = val / 1000.0
        fixed[util] = fixed.get(util, 0.0) + val
        print(f"  {util:<38} {uom:<8} x{n:<4} {val:>18,.2f}  {'(kWh->MWh)' if uom.upper() == 'KWH' else ''}")

    # ------------------------------------------------------------------
    print("\n" + "=" * 100)
    print("COMPARISON: DB values vs model log values")
    print("=" * 100)
    print(f"{'Utility':<28}{'DB Proc':>14}{'DB Fixed':>12}{'DB Total':>14}  |"
          f"{'Log Proc':>13}{'Log Fixed':>11}{'Log Total':>13}  |  Match")
    print("-" * 120)

    all_utils = sorted(set(process) | set(fixed) | set(EXPECTED))
    mismatches = 0
    for util in all_utils:
        db_p = process.get(util, 0.0)
        db_f = fixed.get(util, 0.0)
        if util.strip().lower() in ("power_dis", "power"):
            db_p = db_p / 1000.0  # kWh -> MWh, same as model
        db_t = db_p + db_f
        exp_p, exp_f = EXPECTED.get(util, (None, None))
        if exp_p is None:
            status = "NOT IN LOG"
            mismatches += 1
        else:
            ok = (abs(db_p - exp_p) < 0.01) and (abs(db_f - exp_f) < 0.01)
            status = "OK" if ok else "MISMATCH"
            if not ok:
                mismatches += 1
        print(f"{util:<28}{db_p:>14,.2f}{db_f:>14,.2f}{db_t:>14,.2f}  |"
              f"{(exp_p or 0):>13,.2f}{(exp_f or 0):>11,.2f}{(exp_p or 0) + (exp_f or 0):>13,.2f}  |  {status}"
              if exp_p is not None else
              f"{util:<28}{db_p:>14,.2f}{db_f:>14,.2f}{db_t:>14,.2f}  |"
              f"{'-':>13}{'-':>11}{'-':>13}  |  {status}")

    print("\n" + ("ALL VALUES MATCH DB" if mismatches == 0 else f"{mismatches} MISMATCH(ES) / EXTRA ROWS FOUND"))


if __name__ == "__main__":
    main()
