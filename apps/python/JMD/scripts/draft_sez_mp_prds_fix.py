"""DRAFT — SEZ MP Steam PRDS SHP phantom-letdown fix validation.

Does NOT modify any source file or the DB.  At runtime it patches
``U4UIterationLoop._calculate_steam_cascade_u4u`` with the proposed fix
(extend the dispatch ``{grade}_net`` / ``{grade}_prds_out`` guard — already
used by DTA and the branching-cascade plants — to SEZ), then runs the real
single-plant pipeline ``engine.calculator.run_month(sez, 5, 2026,
save_to_db=False)``.

Proposed fix (injected right after the branching-cascade guard in
``_calculate_steam_cascade_u4u``):

    if self.plant_id.upper() == _SEZ_PLANT_ID:
        dispatch_detail = (steam_result or {}).get("demand_detail") or {}
        grade = produces_dis.split()[0].lower()
        out_key = f"{grade}_prds_out"
        net_key = f"{grade}_net"
        if out_key in dispatch_detail:
            net_prds_demand = max(0.0, float(dispatch_detail[out_key]))
        elif net_key in dispatch_detail:
            net_prds_demand = max(0.0, float(dispatch_detail[net_key]))

Why: SEZ's MP demand is fully covered by STG extraction in dispatch
(mp_net = 0), but the unguarded U4U cascade recomputes net_prds_demand
from gross MP demand minus HRSG byproduct only — producing a phantom
354,388 MT PRDS generation that consumes 326,037 MT of HP.

Usage:
    py scripts/draft_sez_mp_prds_fix.py            # patched run + compare
    py scripts/draft_sez_mp_prds_fix.py --baseline # unpatched run (control)
"""
from __future__ import annotations

import inspect
import io
import os
import sys
import textwrap

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

import engine.u4u_iteration_loop as ul
from engine.calculator import run_month

SEZ_PLANT_ID = "2DFEE33F-4CFD-4887-B9DD-53388AA95271"

ANCHOR = """            # Branching-cascade plants (SEZ-PCG, DTA-PCG): use the steam
            # dispatch's net demand as the PRDS generation so that BFW (and
            # other non-letdown U4U) consumption matches the display table.
            # The branching cascade in the steam dispatch computes the
            # correct net demand per grade, which includes letdown to lower
            # grades.  The cascade's own net_prds_demand (from total_demands)
            # does not include the letdown, so it would undercount.
            # A net of exactly 0 is authoritative too — e.g. when a grade's
            # demand is fully covered by a STEAM(<grade>) import, the PRDS
            # must not generate.
            if self.plant_id.upper() in _BRANCHING_CASCADE_PLANTS:
                dispatch_detail = (steam_result or {}).get("demand_detail") or {}
                grade = produces_dis.split()[0].lower()
                net_key = f"{grade}_net"
                if net_key in dispatch_detail:
                    net_prds_demand = max(0.0, float(dispatch_detail[net_key]))
"""

INJECTION = """
            # ── DRAFT FIX ──────────────────────────────────────────────────
            # SEZ: same guard as DTA / branching plants.  The dispatch's
            # {grade}_net already credits the STG MP extraction, so a net of
            # 0 means the PRDS must not generate.  Without this the U4U
            # cascade fabricates a full-demand PRDS run that injects phantom
            # higher-grade demand back into the loop.
            if self.plant_id.upper() == _SEZ_PLANT_ID:
                dispatch_detail = (steam_result or {}).get("demand_detail") or {}
                grade = produces_dis.split()[0].lower()
                out_key = f"{grade}_prds_out"
                net_key = f"{grade}_net"
                if out_key in dispatch_detail:
                    net_prds_demand = max(0.0, float(dispatch_detail[out_key]))
                elif net_key in dispatch_detail:
                    net_prds_demand = max(0.0, float(dispatch_detail[net_key]))
"""


def apply_draft_patch() -> None:
    src = inspect.getsource(ul.U4UIterationLoop._calculate_steam_cascade_u4u)
    if ANCHOR not in src:
        raise RuntimeError("Anchor block not found — function source changed?")
    patched = src.replace(ANCHOR, ANCHOR + INJECTION, 1)
    patched = textwrap.dedent(patched)
    ns = dict(ul.__dict__)  # module globals for name resolution
    exec(compile(patched, "<draft_patch>", "exec"), ns)
    ul.U4UIterationLoop._calculate_steam_cascade_u4u = ns[
        "_calculate_steam_cascade_u4u"
    ]
    print("[DRAFT] Patched _calculate_steam_cascade_u4u with SEZ guard")


def _steam_summary(res: dict) -> dict:
    out = {}
    steam = res.get("steam_result") or {}
    dd = steam.get("demand_detail") or {}
    for g in ("lp", "mp", "hp", "shp"):
        out[f"{g}_net"] = dd.get(f"{g}_net")
        out[f"{g}_letdown"] = dd.get(f"{g}_letdown")
        out[f"{g}_prds_out"] = dd.get(f"{g}_prds_out")
    out["gen_mt"] = steam.get("total_generation_mt") or steam.get("generated_mt")
    out["deficit_mt"] = steam.get("deficit_mt")
    return out


def main() -> None:
    patch = "--baseline" not in sys.argv
    if patch:
        apply_draft_patch()
        print("[DRAFT] Running SEZ 5/2026 WITH draft fix (no DB save)…")
    else:
        print("[DRAFT] Running SEZ 5/2026 baseline (unpatched, no DB save)…")

    res = run_month(plant_id=SEZ_PLANT_ID, month=5, year=2026, save_to_db=False)

    print("\n===================  RESULT (patched=%s)  ===================" % patch)
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
    print("  steam assets (SHP/HP gen):")
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
        print("\n  PRDS / STG U4U rows (producer, material, norm, gen, qty):")
        for d in details:
            if "PRDS" in str(d.get("producer", "")).upper() or "STG" in str(
                d.get("producer", "")
            ).upper():
                print(
                    f"    {str(d.get('producer'))[:26]:28} "
                    f"{str(d.get('material'))[:18]:20} "
                    f"n={d.get('norm')}  gen={float(d.get('generation') or 0):>12,.2f}  "
                    f"qty={float(d.get('quantity') or 0):>12,.2f}"
                )


if __name__ == "__main__":
    main()
