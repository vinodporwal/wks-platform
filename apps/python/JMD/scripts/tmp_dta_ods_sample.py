"""Dump first 15 DTA data rows for April (cols 0-20) and list April month block."""
import pandas as pd
p = r"c:\Users\shrik\Desktop\Project\fork repo\development\New\JMD new python script\wks-platform\apps\python\files\DTA_JMD.ods"
df = pd.read_excel(p, engine="odf", header=None, nrows=25)
print("April block columns: 11-14 (Norms, Quantity, Amount, Price)")
for r in range(4, 25):
    vals = [df.iloc[r, c] for c in range(0, 20)]
    # filter to useful
    row_text = " | ".join(f"{c}={v}" for c, v in enumerate(vals))
    print(f"row {r}: {row_text}")
