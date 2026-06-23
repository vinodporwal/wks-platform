from database.connection import get_connection

conn = get_connection()
cur = conn.cursor()
q = '''
SELECT TOP 10
    a.AssetName, 
    cap.AOPYear,
    cap.apr_Min, 
    cap.Fixed_Min
FROM PowerGenerationAssets a
LEFT JOIN CPPPowerAssetCapacity cap ON cap.Asset_FK_Id = a.AssetId
WHERE a.AssetName LIKE '%C2%'
'''
cur.execute(q)
for r in cur.fetchall():
    print(f"{r.AssetName} ({r.AOPYear}): apr_Min={r.apr_Min}, Fixed_Min={r.Fixed_Min}")
