"""Quick DB check for DTA plant IDs."""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from database.connection import get_connection

conn = get_connection()
cur = conn.cursor()
print("-- Plants with DTA in name/source --")
cur.execute("SELECT Id, Name, SourceName, IsActive FROM Plants WHERE Name LIKE '%DTA%' OR SourceName LIKE '%DTA%'")
for r in cur.fetchall():
    print(r)
print("-- FinancialYearMonth Apr 2026 - Mar 2027 --")
cur.execute("SELECT Id, Month, Year FROM FinancialYearMonth WHERE Year=2026 OR (Year=2027 AND Month<=3) ORDER BY Year, Month")
for r in cur.fetchall():
    print(r)
conn.close()
print("done")
