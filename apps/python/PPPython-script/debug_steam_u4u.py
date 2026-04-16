
import sys; sys.path.insert(0, '.')
from database.connection import get_connection
conn = get_connection()
cursor = conn.cursor()

# Find all norms where the Material is LP Steam_Dis (utilities that consume LP steam)
print('=== NormsHeader: Utilities that consume LP Steam_Dis ===')
cursor.execute("""
    SELECT DISTINCT p.Name AS plant, nh.UtilityName, nh.MaterialName
    FROM NormsHeader nh
    JOIN Plants p ON p.Id = nh.Plant_FK_Id
    WHERE nh.MaterialName LIKE '%LP Steam%' AND nh.IsActive=1
    ORDER BY p.Name, nh.UtilityName
""")
for r in cursor.fetchall():
    print(f'  Plant={r[0]} | Utility={r[1]} | Material={r[2]}')

print()
print('=== NormsHeader: Utilities that consume HP Steam_Dis ===')
cursor.execute("""
    SELECT DISTINCT p.Name AS plant, nh.UtilityName, nh.MaterialName
    FROM NormsHeader nh
    JOIN Plants p ON p.Id = nh.Plant_FK_Id
    WHERE nh.MaterialName LIKE '%HP Steam%' AND nh.IsActive=1
    ORDER BY p.Name, nh.UtilityName
""")
for r in cursor.fetchall():
    print(f'  Plant={r[0]} | Utility={r[1]} | Material={r[2]}')

print()
print('=== NormsHeader: Utilities that consume MP Steam_Dis ===')
cursor.execute("""
    SELECT DISTINCT p.Name AS plant, nh.UtilityName, nh.MaterialName
    FROM NormsHeader nh
    JOIN Plants p ON p.Id = nh.Plant_FK_Id
    WHERE nh.MaterialName LIKE '%MP Steam%' AND nh.IsActive=1
    ORDER BY p.Name, nh.UtilityName
""")
for r in cursor.fetchall():
    print(f'  Plant={r[0]} | Utility={r[1]} | Material={r[2]}')

print()
print('=== NormsHeader: Utilities that consume SHP Steam_Dis ===')
cursor.execute("""
    SELECT DISTINCT p.Name AS plant, nh.UtilityName, nh.MaterialName
    FROM NormsHeader nh
    JOIN Plants p ON p.Id = nh.Plant_FK_Id
    WHERE nh.MaterialName LIKE '%SHP Steam%' AND nh.IsActive=1
    ORDER BY p.Name, nh.UtilityName
""")
for r in cursor.fetchall():
    print(f'  Plant={r[0]} | Utility={r[1]} | Material={r[2]}')

print()
print('=== Sample norms values for LP Steam consumers (Apr 2025) ===')
cursor.execute("""
    SELECT p.Name, nh.UtilityName, nh.MaterialName, nmd.Norms, fym.Month, fym.Year
    FROM NormsMonthDetail nmd
    JOIN NormsHeader nh ON nh.Id = nmd.NormsHeader_FK_Id
    JOIN Plants p ON p.Id = nh.Plant_FK_Id
    JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
    WHERE nh.MaterialName LIKE '%LP Steam%' AND nh.IsActive=1
      AND fym.Month=4 AND fym.Year=2025
    ORDER BY p.Name, nh.UtilityName
""")
for r in cursor.fetchall():
    print(f'  Plant={r[0]} | Utility={r[1]} | Material={r[2]} | Norm={r[3]}')

conn.close()
