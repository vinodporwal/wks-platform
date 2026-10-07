"""Analyse Sender Receiver Costcenter.csv — demand register semantics."""
import csv, sys
sys.path.insert(0, ".")
sys.stdout.reconfigure(encoding="utf-8", errors="replace")
from collections import defaultdict

path = "../files/Sender Receiver Costcenter.csv"
with open(path, encoding="utf-16", newline="") as f:
    rows = list(csv.reader(f, delimiter="\t"))

hdr = rows[1]
MONTH_COLS = {16: "2026.04"}  # Q
def num(s):
    s = str(s).replace(",", "").strip()
    try:
        return float(s)
    except Exception:
        return 0.0

# 1) distinct CPP plants (col E) and their total April demand via col A
by_plant = defaultdict(float)
plants = set()
for r in rows[2:]:
    if len(r) < 17:
        continue
    e = str(r[4]).strip()
    a = str(r[0]).strip()
    if not e or e == "Total":
        continue
    plants.add(e)
    by_plant[e] += num(r[16])

print("=== April total by CPP Plant (col E) ===")
for p, v in sorted(by_plant.items(), key=lambda x: -x[1]):
    print("  %-45s %s" % (p, format(v, ",.0f")))

# 2) C2 Power_Dis check: A='Power_Dis', E='JMD - DTA-C2 Power & UTILITY'
print()
print("=== C2 Power_Dis rows ===")
tot, tot_pf = 0.0, 0.0
for r in rows[2:]:
    if len(r) < 17:
        continue
    if str(r[0]).strip() == "Power_Dis" and str(r[4]).strip() == "JMD - DTA-C2 Power & UTILITY":
        o = str(r[14]).strip()
        v = num(r[16])
        tot += v
        if not o:
            tot_pf += v
        print("  mat=%-14s cc=%-24s plant=%-34s O=%-8s q=%s" % (r[8], r[10], r[12], o, format(v, ",.0f")))
print("  TOTAL demand=%s | process+fixed(O empty)=%s" % (format(tot, ",.0f"), format(tot_pf, ",.0f")))
