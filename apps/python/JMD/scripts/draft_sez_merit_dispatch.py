"""DRAFT — SEZ merit-order steam dispatch validation.

Does NOT modify any source file or the DB.  At runtime it flips
``STEAM_SPINNING_MARGIN_ENABLED[SEZ]`` to True so the steam dispatch takes
the C2 merit-order path ``_dispatch_steam_c2`` instead of the generic
priority ramp:

    1. HRSGs collect free steam from linked GTs (inherent — free_steam_mt).
    2. Mandatory/activated ABs at MIN load.
    3. HRSG supplementary firing ramps to MAX (priority order).
    4. Residual deficit -> ABs ramp by priority (optional ABs activated at
       MIN first, then online ABs ramped to MAX).

Why: the generic path ramps AUXBOILs (DB priority 1) before HRSGs (which
inherit their linked GT's power priority 2).  An AB consumes 0.7 MT of HP
spray per MT generated — only 30 % net supply — so AB-first ordering
inflates gross HP demand/generation massively vs the BPC book, which loads
HRSGs first (~212 kMT each) and trims with ABs (~68 kMT each).

Usage:
    py scripts/draft_sez_merit_dispatch.py            # merit-order run
    py scripts/draft_sez_merit_dispatch.py --baseline # generic-ramp control
"""
from __future__ import annotations

import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

import engine.dispatch_engine as de
from engine.calculator import run_month

SEZ_PLANT_ID = "2DFEE33F-4CFD-4887-B9DD-53388AA95271"


def main() -> None:
    patch = "--baseline" not in sys.argv
    if patch:
        de.STEAM_SPINNING_MARGIN_ENABLED[SEZ_PLANT_ID] = True
        print("[DRAFT] SEZ merit-order dispatch enabled (C2 path), no DB save…")
    else:
        print("[DRAFT] Running SEZ 5/2026 baseline (generic ramp, no DB save)…")

    res = run_month(plant_id=SEZ_PLANT_ID, month=5, year=2026, save_to_db=False)

    print("\n===================  RESULT (merit=%s)  ===================" % patch)
    print("success:", res.get("success"), "| message:", res.get("message"))

    u4u = res.get("u4u_iteration") or {}
    print("converged:", u4u.get("converged"), "| iters:", u4u.get("iterations_used"))

    sr = u4u.get("final_steam_result") or {}
    dd = sr.get("demand_detail") or {}
    print("\n  Steam demand_detail:")
    for g in ("lp", "mp", "hp", "shp"):
        keys = {k: dd.get(f"{g}_{k}") for k in ("process", "fixed", "letdown", "byproduct", "net", "prds_out")}
        if any(v is not None for v in keys.values()):
            print(
                f"    {g.upper():4} " + "  ".join(
                    f"{k}={v:,.2f}" for k, v in keys.items() if v is not None
                )
            )
    print("  steam assets (HP gen):")
    for a in sr.get("assets", []):
        if float(a.get("total_output_mt", 0) or 0) > 0 or a.get("op_hours", 0) > 0:
            print(
                f"    {str(a.get('asset_name'))[:28]:30} hrs={a.get('op_hours', 0):>6} "
                f"supp={float(a.get('supplementary_mt', 0) or 0):>12,.2f} "
                f"free={float(a.get('free_steam_mt', 0) or 0):>12,.2f} "
                f"total={float(a.get('total_output_mt', 0) or 0):>12,.2f}"
            )

    details = u4u.get("final_detail_records") or []
    if details:
        print("\n  PRDS / STG / AB U4U rows (producer, material, norm, gen, qty):")
        for d in details:
            prod = str(d.get("producer", "")).upper()
            if "PRDS" in prod or "STG" in prod or "AUXBOIL" in prod:
                print(
                    f"    {str(d.get('producer'))[:26]:28} "
                    f"{str(d.get('material'))[:18]:20} "
                    f"n={d.get('norm')}  gen={float(d.get('generation') or 0):>12,.2f}  "
                    f"qty={float(d.get('quantity') or 0):>12,.2f}"
                )


if __name__ == "__main__":
    main()
