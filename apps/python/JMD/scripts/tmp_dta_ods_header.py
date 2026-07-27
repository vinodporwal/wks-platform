"""Inspect DTA ODS header row/month columns quickly (only first 5 rows)."""
import pandas as pd
p = r"c:\Users\shrik\Desktop\Project\fork repo\development\New\JMD new python script\wks-platform\apps\python\files\DTA_JMD.ods"
df = pd.read_excel(p, engine="odf", header=None, nrows=5)
print("shape:", df.shape)
print("\n-- row 0 first 40 cols --")
for i in range(min(40, len(df.columns))):
    print(i, repr(df.iloc[0, i]))
print("\n-- row 1 month values first 80 cols --")
for i in range(min(80, len(df.columns))):
    v = df.iloc[1, i]
    if pd.notna(v):
        print(i, repr(v))
print("\n-- row 2 first 20 cols --")
for i in range(min(20, len(df.columns))):
    print(i, repr(df.iloc[2, i]))
print("\n-- row 3 first 20 cols --")
for i in range(min(20, len(df.columns))):
    print(i, repr(df.iloc[3, i]))
