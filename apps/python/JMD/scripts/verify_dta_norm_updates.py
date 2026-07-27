"""Verify DTA zero-norm updates: NormsHeader.Remarks, NormsMonthDetail.Norms, CPPNorms."""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from database.connection import get_connection

conn = get_connection()
cur = conn.cursor()

cur.execute("""
    SELECT COUNT(*)
    FROM NormsHeader
    WHERE Remarks LIKE '%DTA_JMD.ODS%'
""")
header_count = cur.fetchone()[0]

cur.execute("""
    SELECT COUNT(*)
    FROM CPPNorms
    WHERE Remarks LIKE '%DTA_JMD.ODS%'
""")
cpp_count = cur.fetchone()[0]

print(f"NormsHeader with DTA_JMD.ODS remark: {header_count}")
print(f"CPPNorms rows with DTA_JMD.ODS remark: {cpp_count}")

print("\n-- Sample updated headers (10 rows) --")
cur.execute("""
    SELECT TOP 10 nh.UtilityName, nh.MaterialName, nh.Remarks
    FROM NormsHeader nh
    WHERE nh.Remarks LIKE '%DTA_JMD.ODS%'
""")
for r in cur.fetchall():
    print(r)

print("\n-- Sample CPPNorms with monthly column changed (10 rows) --")
cur.execute("""
    SELECT TOP 10 c.Apr_Norms, c.May_Norms, c.ModifiedBy, c.ModifiedDate, nh.UtilityName, nh.MaterialName
    FROM CPPNorms c
    INNER JOIN NormsHeader nh ON nh.Id = c.NormsHeader_FK_Id
    WHERE c.Remarks LIKE '%DTA_JMD.ODS%'
""")
for r in cur.fetchall():
    print(r)

print("\n-- NormsMonthDetail vs CPPNorms (one sample) --")
cur.execute("""
    SELECT TOP 1 nmd.Norms, c.Apr_Norms, c.May_Norms, c.Jun_Norms, nh.UtilityName, nh.MaterialName
    FROM NormsMonthDetail nmd
    INNER JOIN NormsHeader nh ON nh.Id = nmd.NormsHeader_FK_Id
    INNER JOIN CPPNorms c ON c.NormsHeader_FK_Id = nh.Id AND c.FinancialYear = '2026-27'
    WHERE nmd.Norms != 0 AND c.Remarks LIKE '%DTA_JMD.ODS%'
""")
for r in cur.fetchall():
    print(r)

conn.close()
print("done")
