"""Temporary inspector for DTA ODS and DB plant mapping."""
import os
import sys
import pandas as pd

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from database.connection import get_connection

ODS_PATH = r"c:\Users\shrik\Desktop\Project\fork repo\development\New\JMD new python script\wks-platform\apps\python\files\DTA_JMD.ods"

def inspect_ods():
    df = pd.read_excel(ODS_PATH, engine="odf", header=None)
    print("ODS shape:", df.shape)
    print("\n-- Row 0 (col labels) cols 0-30 --")
    for i in range(min(30, len(df.columns))):
        print(i, repr(df.iloc[0, i]))
    print("\n-- Row 1 (month headers) cols 0-100 --")
    for i in range(min(100, len(df.columns))):
        v = df.iloc[1, i]
        print(i, repr(v))
    print("\n-- Row 3 (sub-headers?) --")
    for i in range(min(20, len(df.columns))):
        print(i, repr(df.iloc[3, i]))
    print("\n-- Distinct utilities (col 2), accounts (col 5), materials (col 6) rows 4:100 --")
    util_set = set()
    acct_set = set()
    mat_set = set()
    for r in range(4, min(100, len(df))):
        util = str(df.iloc[r, 2]).strip() if pd.notna(df.iloc[r, 2]) else ""
        acct = str(df.iloc[r, 5]).strip() if pd.notna(df.iloc[r, 5]) else ""
        mat = str(df.iloc[r, 6]).strip() if pd.notna(df.iloc[r, 6]) else ""
        if util and util.lower() != "nan": util_set.add(util)
        if acct and acct.lower() != "nan": acct_set.add(acct)
        if mat and mat.lower() != "nan": mat_set.add(mat)
    print("Utilities:", sorted(util_set))
    print("Accounts:", sorted(acct_set))
    print("Materials (sample):", sorted(mat_set)[:50])


def inspect_db_plants():
    conn = get_connection()
    cur = conn.cursor()
    cur.execute("SELECT Id, Name, SourceName, IsActive FROM Plants WHERE Name LIKE '%DTA%'")
    rows = cur.fetchall()
    print("\n-- DTA plants --")
    for r in rows:
        print(r)
    # find sub plants with cpp source
    cur.execute("SELECT Id, Name, SourceName, IsActive FROM Plants")
    all_rows = cur.fetchall()
    print("\n-- All plants (DTA relevant) --")
    for r in all_rows:
        if "dta" in str(r[1]).lower() or "dta" in str(r[2]).lower():
            print(r)
    conn.close()


if __name__ == "__main__":
    inspect_ods()
    inspect_db_plants()
