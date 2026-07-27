"""Check NormsMonthDetail precision for small DTA norms."""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from database.connection import get_connection

conn = get_connection()
cur = conn.cursor()

cur.execute("""
    SELECT nmd.Norms, nh.UtilityName, nh.MaterialName, p.Name, fym.Month, fym.Year
    FROM NormsMonthDetail nmd
    INNER JOIN NormsHeader nh ON nh.Id = nmd.NormsHeader_FK_Id
    INNER JOIN Plants p ON p.Id = nh.Plant_FK_Id
    INNER JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
    WHERE nh.Remarks LIKE '%DTA_JMD.ODS%'
      AND LOWER(nh.MaterialName) IN ('cooling water', 'nitrogen')
      AND LOWER(nh.UtilityName) = 'powergen'
    ORDER BY p.Name, fym.Month
""")
rows = cur.fetchall()
print(f"Found {len(rows)} rows")
for r in rows[:20]:
    print(r)

conn.close()
