"""Verify BPC-pinned steam transfer resolution for April 2026 (read-only)."""
import logging, sys, os
_JMD = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, _JMD)
logging.disable(logging.CRITICAL)

from engine.jmd_orchestrator import CPP_ORDER, cpp_name
from engine.calculator import _get_demands
from engine.u4u_iteration_loop import U4UIterationLoop
from engine.steam_transfer_router import resolve_steam_transfers

DTA = CPP_ORDER[0]; PCG = CPP_ORDER[1]; SEZ = CPP_ORDER[2]; SEZPCG = CPP_ORDER[3]

loops = {}
for pid in CPP_ORDER:
    lp = U4UIterationLoop(plant_id=pid, month=4, year=2026,
        initial_demands=_get_demands(pid, 4, 2026), ods_reader=None,
        import_power={"success": False, "total_mwh": 0.0, "per_source": []},
        gt_heat_rate_df=None, hrsg_heat_rate_df=None,
        interplant_export_demands={}, pooled_power=True,
        steam_transfer_mode="bpc")
    assert lp.prepare(), cpp_name(pid)
    loops[pid] = lp

ext, rows = resolve_steam_transfers("bpc", loops, list(CPP_ORDER),
                                    month=4, year=2026)

print("=== ext_steam (signed demand adjustments) ===")
for pid in CPP_ORDER:
    print(cpp_name(pid))
    for m, q in sorted(ext.get(pid, {}).items()):
        print(f"    {m:20s} {q:>+14,.2f}")

print("\n=== transfer rows ===")
for t in rows:
    snd = cpp_name(t['sender']) if t.get('sender') else '-'
    print(f"  {t['edge']:3s} {snd:10s} -> {cpp_name(t['receiver']):10s} "
          f"{t['material']:16s} {t['quantity']:>12,.2f}  {t['basis']}")

print("\n=== assertions ===")
def chk(name, cond):
    print(("PASS" if cond else "FAIL"), name)
    assert cond, name

chk("DTA SHP credit = -89,969", abs(ext[DTA].get("SHP Steam_Dis",0)+89969)<1)
chk("DTA HP credit = -28,800", abs(ext[DTA].get("HP Steam_Dis",0)+28800)<1)
chk("DTA-PCG SHP net = +85,625 (89,969-4,344)",
    abs(ext[PCG].get("SHP Steam_Dis",0)-85625)<1)
chk("DTA-PCG IP credit = -25,982", abs(ext[PCG].get("IP Steam_Dis",0)+25982)<1)
chk("SEZ HP export = +41,057", abs(ext[SEZ].get("HP Steam_Dis",0)-41057)<1)
chk("SEZ-PCG E6 LLP obligation = +111,384 (154.7 TPH x 720h)",
    abs(ext[SEZPCG].get("LLP Steam Dis",0)-111384)<1)
chk("SEZ-PCG LLP only (no other grades)",
    set(ext[SEZPCG].keys()) == {"LLP Steam Dis"})
chk("C2 untouched", not ext.get(CPP_ORDER[4]))
print("\nAll checks passed.")
