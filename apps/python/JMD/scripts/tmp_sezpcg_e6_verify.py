"""E6 one-sided commitment verification — April 2026 SEZ-PCG LLP/LP/HP chain."""
import logging, sys, os
_JMD = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, _JMD)
logging.disable(logging.CRITICAL)

MONTH, YEAR = 4, 2026
MODE = sys.argv[1] if len(sys.argv) > 1 else "bpc"

from engine.jmd_orchestrator import run_jmd_month, CPP_ORDER, cpp_name
from engine.steam_transfer_router import STEAM_EDGES

SEZPCG = "D2C7FBAD-7E00-4642-B3B2-5A768FAC8D45"
SEZ = "2DFEE33F-4CFD-4887-B9DD-53388AA95271"

res = run_jmd_month(MONTH, YEAR, steam_transfer_mode=MODE)
print(f"\n=== mode={MODE} converged={res['converged']} iters={res['iterations_used']} ===")

print("\n--- steam transfer rows ---")
for t in res.get("steam_transfers", []):
    snd = cpp_name(t['sender']) if t.get('sender') else '-'
    print(f"  {t['edge']:3s} {snd:11s} -> {cpp_name(t['receiver']):11s} "
          f"{t['material']:15s} {t['quantity']:>12,.2f}  {t['basis']}")

plant = res["plants"][SEZPCG]
steam = plant.get("final_steam_result") or {}
dd = steam.get("demand_detail") or {}
print("\n--- SEZ-PCG demand_detail (key grades) ---")
for k in sorted(dd):
    if k.startswith("_"):
        continue
    v = dd[k]
    if isinstance(v, (int, float)) and (abs(v) > 0.5 or "llp" in k or "lp" in k):
        print(f"  {k:28s} {v:>14,.2f}")

print("\n--- SEZ-PCG final_total_demands (steam grades) ---")
for m, q in sorted((plant.get("final_total_demands") or {}).items()):
    if "STEAM" in m.upper() or "STEAM(" in m.upper():
        print(f"  {m:30s} {q:>14,.2f}")

print("\n--- SEZ-PCG final_u4u_demands (steam grades) ---")
for m, q in sorted((plant.get("final_u4u_demands") or {}).items()):
    if "STEAM" in m.upper() or "STEAM(" in m.upper():
        print(f"  {m:30s} {q:>14,.2f}")

print("\n--- steam balance report (SEZ-PCG + SEZ) ---")
for row in res.get("steam_balance", []):
    if row.get("plant_id") in (SEZPCG, SEZ):
        print(" ", cpp_name(row["plant_id"]), row)
