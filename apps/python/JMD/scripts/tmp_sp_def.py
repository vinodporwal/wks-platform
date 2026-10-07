"""Dump SP definitions for the JMD fixed-consumption / demand fetch procedures."""
import sys, logging
sys.path.insert(0, ".")
sys.stdout.reconfigure(encoding="utf-8", errors="replace")
logging.disable(logging.CRITICAL)
from database.connection import get_connection

conn = get_connection()
cur = conn.cursor()

for sp in ["CPP_GetFixedConsumptionByPlant",
           "CPP_NMD_GetProcessDemandByYear",
           "CPP_GetProcessDemandByYear"]:
    cur.execute(
        "SELECT OBJECT_DEFINITION(OBJECT_ID(?))", "dbo." + sp)
    row = cur.fetchone()
    print("=" * 80)
    print("SP:", sp)
    print("=" * 80)
    print(row[0] if row and row[0] else "(not found)")

conn.close()
