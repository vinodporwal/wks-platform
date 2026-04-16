from database.db_connection import get_connection

conn = get_connection()
cursor = conn.cursor()

query = """
    SELECT 
        cc.CostCenterName AS consumer_name,
        pm.DisplayName AS plant_name,
        ufc.ConsumptionValue AS consumption_value
    FROM UtilityFixedConsumption ufc
    JOIN NormParameters np ON ufc.NormParameter_FK_Id = np.Id
    JOIN CPPCostCenters cc ON ufc.CostCenter_FK_Id = cc.CostCenterId
    JOIN FixedConsumptionPlantMapping pm ON cc.Plant_FK_Id = pm.Id
    JOIN FinancialYearMonth fym ON ufc.FinancialYearMonth_FK_Id = fym.Id
    WHERE fym.Month = 4 AND fym.Year = 2025 
    AND np.Name = 'D M Water'
    AND ufc.ConsumptionValue > 0
    ORDER BY pm.DisplayName, cc.CostCenterName
"""

cursor.execute(query)
rows = cursor.fetchall()

print("DM Water Fixed Consumption for April 2025:")
print("-" * 60)
total = 0
for row in rows:
    print(f"{row[0]} ({row[1]}): {row[2]} M3")
    total += row[2]

print("-" * 60)
print(f"TOTAL: {total} M3")

conn.close()
