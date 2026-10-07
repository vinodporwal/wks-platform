"""Dump all cross-CPP steam rows + STEAM(<grade>) plug rows for all 12 months.

Register layout (UTF-16 TSV):
  col A(0) Utility | col E(4) sender node | col M(12) receiver plant
  col O(14) activity | cols Q..AB (16..27) = Apr..Mar
"""
import csv
import sys
from collections import defaultdict
from pathlib import Path

CSV = Path(__file__).resolve().parent.parent.parent / "files" / "Sender Receiver Costcenter.csv"
MONTHS = ["Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec","Jan","Feb","Mar"]
DAYS   = [30,31,30,31,31,30,31,30,31,31,28,31]

def fnum(s):
    try:
        return float(str(s or "").strip().replace(",", ""))
    except (TypeError, ValueError):
        return 0.0

rows = []
with CSV.open("r", encoding="utf-16", newline="") as fh:
    for r in csv.reader(fh, delimiter="\t"):
        if len(r) <= 27:
            continue
        util = (r[0] or "").strip()
        node = (r[4] or "").strip()
        recv = (r[12] or "").strip()
        act  = (r[14] or "").strip() if len(r) > 14 else ""
        if not util or util == "Utility" or util == "Total":
            continue
        if str(r[8]).strip() == "Total" or str(r[12]).strip() == "Total" or str(r[14]).strip() == "Total":
            continue
        qtys = [fnum(r[c]) for c in range(16, 28)]
        rows.append((util, node, recv, act, qtys))

# --- all rows whose utility or material mentions a steam plug/grade crossing CPP nodes
CPP_NODES = {"36BK","36KO","36KQ","36BX","36BW","36H2","36H3","36FT","36GW","36G5"}
def node_of(s):
    for n in CPP_NODES:
        if n.lower() in s.lower():
            return n
    return None

print("=== STEAM(<grade>) plug rows (self-issued imports/exports) ===")
plug = defaultdict(lambda: [0.0]*12)
meta = {}
for util, node, recv, act, qtys in rows:
    if "STEAM(" in util.upper() or "STEAM(" in node.upper():
        key = (util, node)
        for i,q in enumerate(qtys): plug[key][i]+=q
        meta[key]=(util,node)
for (util,node),q in sorted(plug.items()):
    if all(abs(v)<0.5 for v in q): continue
    print(f"\n{util} | node={node}")
    for i,v in enumerate(q):
        if abs(v)>0.5: print(f"   {MONTHS[i]:4s} {v:>12,.0f} MT   {v/(DAYS[i]*24):>7.2f} TPH")

print()
print("=== booked rows between CPP steam nodes (any steam utility) ===")
steam_rows = defaultdict(lambda: [0.0]*12)
for util, node, recv, act, qtys in rows:
    un = util.lower()
    if "steam" not in un and "shp" not in un:
        continue
    s, r = node_of(node), node_of(recv)
    if s and r and s != r and {s,r} != {"36BX","36BW"}:  # 36BX->36BW is intra-SEZ
        key = (s+"->"+r, util)
        for i,q in enumerate(qtys): steam_rows[key][i]+=q
for (edge,util),q in sorted(steam_rows.items()):
    if all(abs(v)<0.5 for v in q): continue
    print(f"\n{edge} | {util}")
    for i,v in enumerate(q):
        if abs(v)>0.5: print(f"   {MONTHS[i]:4s} {v:>12,.0f} MT   {v/(DAYS[i]*24):>7.2f} TPH")
