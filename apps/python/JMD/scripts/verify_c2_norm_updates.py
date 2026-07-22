"""
Verify C2 zero-norm updates in NormsMonthDetail and CPPNorms.
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from database.connection import get_connection

C2_PLANT_ID = "BA558F95-8A3F-4769-9C78-FF7B6C639DDF"
MONTH = 5
YEAR = 2026

TARGET_UTILITY_MATERIALS = [
    ("Oxygen", "Cooling Water"),
    ("Utility Water", "CHEM CHLORINE"),
    ("Utility Water", "VASUCOR 355"),
    ("NITROGEN_ASU", "Cooling Water"),
    ("Utility Water", "CORROCIL 952S"),
    ("Cooling Water", "CHEM SODIUM HYPOCHLORITE"),
    ("COMPRESSED AIR", "Cooling Water"),
    ("Cooling Water", "CHEM SULPHURIC ACID 1.84 KG/M3"),
    ("Utility Water", "Power_Dis"),
    ("Cooling Water", "VASUFLOC - 5596"),
]


def main():
    conn = get_connection()
    cur = conn.cursor()

    cur.execute(
        "SELECT Id FROM FinancialYearMonth WHERE Month = ? AND Year = ?",
        (MONTH, YEAR),
    )
    row = cur.fetchone()
    if not row:
        print("FinancialYearMonth not found")
        return
    fym_id = row[0]

    placeholders = ",".join("?" for _ in TARGET_UTILITY_MATERIALS)
    utility_names = [u for u, _ in TARGET_UTILITY_MATERIALS]
    material_names = [m for _, m in TARGET_UTILITY_MATERIALS]

    # Verify NormsMonthDetail values and remarks
    query = f"""
    SELECT p.Name, nh.UtilityName, nh.MaterialName, nmd.Norms, nh.Remarks
    FROM NormsMonthDetail nmd WITH (NOLOCK)
    INNER JOIN NormsHeader nh WITH (NOLOCK) ON nh.Id = nmd.NormsHeader_FK_Id
    INNER JOIN Plants p WITH (NOLOCK) ON p.Id = nh.Plant_FK_Id
    INNER JOIN FinancialYearMonth fym WITH (NOLOCK) ON fym.Id = nmd.FinancialYearMonth_FK_Id
    WHERE p.Id IN (
        SELECT Id FROM Plants WHERE TRY_CONVERT(uniqueidentifier, SourceName) = CAST(? AS uniqueidentifier) AND IsActive = 1
    )
    AND fym.Month = ? AND fym.Year = ?
    AND nh.UtilityName IN ({placeholders})
    AND nh.MaterialName IN ({placeholders})
    ORDER BY nh.UtilityName, nh.MaterialName
    """
    params = [C2_PLANT_ID, MONTH, YEAR] + utility_names + material_names
    cur.execute(query, params)

    print(f"NormsMonthDetail verification for C2 CPP {MONTH:02d}/{YEAR}")
    print(f"{'Plant':<35} {'Utility':<20} {'Material':<35} {'Norm':<16} {'Remarks':<50}")
    print("-" * 150)
    for row in cur.fetchall():
        print(f"{str(row[0]):<35} {str(row[1]):<20} {str(row[2]):<35} {float(row[3] or 0):<16.8f} {str(row[4] or ''):<50}")

    # Verify CPPNorms May_Norms and remarks
    print("\n" + "=" * 150)
    print("CPPNorms verification (May_Norms column)")
    print(f"{'Utility':<20} {'Material':<35} {'May_Norms':<16} {'Remarks':<50} {'ModifiedBy':<20} {'ModifiedDate'}")
    print("-" * 150)

    cur.execute(
        f"""
        SELECT nh.UtilityName, nh.MaterialName, cn.May_Norms, cn.Remarks, cn.ModifiedBy, cn.ModifiedDate
        FROM CPPNorms cn WITH (NOLOCK)
        INNER JOIN NormsHeader nh WITH (NOLOCK) ON nh.Id = cn.NormsHeader_FK_Id
        INNER JOIN Plants p WITH (NOLOCK) ON p.Id = nh.Plant_FK_Id
        WHERE p.Id IN (
            SELECT Id FROM Plants WHERE TRY_CONVERT(uniqueidentifier, SourceName) = CAST(? AS uniqueidentifier) AND IsActive = 1
        )
        AND cn.FinancialYear = '2026-27'
        AND nh.UtilityName IN ({placeholders})
        AND nh.MaterialName IN ({placeholders})
        ORDER BY nh.UtilityName, nh.MaterialName
        """,
        [C2_PLANT_ID] + utility_names + material_names,
    )
    for row in cur.fetchall():
        print(f"{str(row[0]):<20} {str(row[1]):<35} {float(row[2] or 0):<16.8f} {str(row[3] or ''):<50} {str(row[4] or ''):<20} {row[5]}")

    conn.close()


if __name__ == "__main__":
    main()
