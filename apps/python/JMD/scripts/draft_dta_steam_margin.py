"""DRAFT — DTA steam spinning margin dispatch validation.

Does NOT modify any source file or the DB.  At runtime it patches the
steam dispatch so DTA (only) uses the site-spec AB-margin sequence:

  required margin R = highest DISPATCHED total-output TPH among online
                      generators (DTA May: HRSG10 @ 275 TPH incl free steam)

  Demand-first flow:
  1. Free steam credited upstream; mandatory assets at min.
  2. HRSGs ramp to max (priority order).
  3. Active ABs (mandatory) ramp by priority for the residual.
  4. Still unmet -> bring next-priority NON-MANDATORY AB online at min,
     only if op_hours>0 (0-hour ABs can never activate); redispatch.
  5. Margin is MEASURED, not enforced:
     held = sum(Fixed_Max - dispatched TPH) over active ABs;
     reported vs required R.

Dispatch order within a pass: mandatory mins -> HRSG ramp to max ->
ABs ramp to (Max - margin share).  HRSGs NEVER hold margin (spec).

Usage:
    py scripts/draft_dta_steam_margin.py            # margin dispatch run
    py scripts/draft_dta_steam_margin.py --baseline # generic-ramp control
"""
from __future__ import annotations

import inspect
import logging
import os
import sys
import textwrap

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

import engine.dispatch_engine as de
from engine.calculator import run_month

DTA_PLANT_ID = "A4AF8441-73AD-4F9F-BCF4-6734E8202F7A"
logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)
if not any(isinstance(h, logging.StreamHandler) for h in logger.handlers):
    logger.addHandler(logging.StreamHandler(sys.stdout))
    logger.propagate = False


def _dispatch_steam_dta_margin(dispatch_assets, net_shp_to_dispatch):
    """DTA site-spec margin dispatch — v2 spec (user 2026-10-10).

    Sequence:
      1. Free steam already credited upstream; mandatory assets @ min.
      2. HRSGs ramp to max (priority order).
      3. Required margin = highest DISPATCHED total-output TPH among
         online generators (e.g. HRSG10 @ 275 incl. free steam).
      4. Active ABs = op_hours>0 only (0-hour ABs CANNOT come online).
         Each AB capped at  May_Max - margin/len(active_ABs).
      5. Held margin = sum(May_Max - dispatched_tph) over active ABs.
      6. Residual -> step-3 GT/HRSG warning.
    """
    from collections import defaultdict

    def _reset():
        for a in dispatch_assets:
            a["dispatched_tph"] = 0.0
            a["dispatched_mt"] = 0.0

    def _ramp(assets, remaining_mt, caps=None):
        groups = defaultdict(list)
        for a in assets:
            if a["op_hours"] > 0:
                groups[a["priority"]].append(a)
        for pri in sorted(groups):
            if remaining_mt <= 0.001:
                break
            while remaining_mt > 0.001:
                uncapped = []
                for a in groups[pri]:
                    cap = (caps or {}).get(id(a), a["max_tph"])
                    h = cap - a["dispatched_tph"]
                    if h > 0.001:
                        uncapped.append((a, h))
                if not uncapped:
                    break
                total_hrs = sum(a["op_hours"] for a, _ in uncapped)
                if total_hrs <= 0:
                    break
                add_tph = remaining_mt / total_hrs
                saturated = []
                for a, h in uncapped:
                    d = min(add_tph, h)
                    a["dispatched_tph"] += d
                    a["dispatched_mt"] = a["dispatched_tph"] * a["op_hours"]
                    remaining_mt -= d * a["op_hours"]
                    if d >= h - 0.001:
                        saturated.append(a)
                if not saturated:
                    break
        return remaining_mt

    abs_ = [a for a in dispatch_assets
            if a["asset_type"] == "AUXBOILER" and a["op_hours"] > 0]
    hrsgs = [a for a in dispatch_assets
             if a["asset_type"] == "HRSG" and a["op_hours"] > 0]

    # DRAFT scenario: HRSG10 total output locked at 240 TPH (supp cap =
    # 240 - free steam rate) — reduces required margin to 240.
    for a in hrsgs:
        if a["asset_name"].upper().startswith("HRSG10") and "--cap240" in sys.argv:
            fs_tph = (a.get("free_steam_mt") or 0.0) / a["op_hours"]
            a["max_tph"] = max(0.0, 240.0 - fs_tph)

    # 0. Fixed SHP import credit — DTA-PCG -> DTA booked 120 TPH
    #    (CPP_IntersiteSteamTransfer Min_May=Max_May=120) = 89,280 MT.
    #    Individual-mode demand must be reduced by this committed import.
    IMPORT_SHP_MT = 120.0 * 744.0
    net_shp_to_dispatch = max(0.0, net_shp_to_dispatch - IMPORT_SHP_MT)
    logger.info("  [DTA MARGIN] Fixed SHP import credited: %.2f MT "
                "(DTA-PCG 120 TPH booked)", IMPORT_SHP_MT)

    # 1. Mandatory assets at MIN
    for a in dispatch_assets:
        if a["op_hours"] > 0 and a["mandatory"] == 1:
            a["dispatched_tph"] = a["min_tph"]
            a["dispatched_mt"] = a["min_tph"] * a["op_hours"]
    covered = sum(a["dispatched_mt"] for a in dispatch_assets)

    # 2. HRSGs ramp to meet remaining demand (to max, priority order)
    residual = _ramp(hrsgs, net_shp_to_dispatch - covered)

    # 3. Required margin = highest DISPATCHED total-output TPH among
    #    online generators (HRSG: supp dispatched + free steam).
    req_margin_tph = 0.0
    req_src = None
    for a in dispatch_assets:
        if a["op_hours"] <= 0:
            continue
        out_tph = (a["dispatched_mt"] + (a.get("free_steam_mt") or 0.0)) / a["op_hours"]
        if out_tph > req_margin_tph:
            req_margin_tph, req_src = out_tph, a["asset_name"]

    # 4. Demand-first flow (v4 spec): ramp ABs that are online & active
    #    (mandatory, or already activated) by priority for the residual.
    #    If still unmet, bring the next-priority NON-MANDATORY AB online —
    #    only if it has op_hours > 0 (0-hour ABs can never activate) —
    #    at min, then redispatch.  No margin caps; margin is MEASURED.
    abs_all = [a for a in dispatch_assets if a["asset_type"] == "AUXBOILER"]

    def _ab_active(a):
        return (a["op_hours"] > 0
                and (a["mandatory"] == 1 or a.get("_activated")))

    def _ab_pass():
        # reset all ABs, re-seat actives at min, ramp for unmet
        for a in abs_all:
            a["dispatched_tph"] = 0.0
            a["dispatched_mt"] = 0.0
        active = [a for a in abs_all if _ab_active(a)]
        for a in active:
            a["dispatched_tph"] = a["min_tph"]
            a["dispatched_mt"] = a["min_tph"] * a["op_hours"]
        unmet = net_shp_to_dispatch - sum(
            a["dispatched_mt"] for a in dispatch_assets)
        return _ramp(active, unmet)

    while True:
        residual = _ab_pass()
        if residual <= 0.001:
            break
        candidates = sorted(
            (a for a in abs_all
             if a["op_hours"] > 0 and a["mandatory"] != 1
             and not a.get("_activated")),
            key=lambda a: a["priority"])
        if not candidates:
            break
        nxt = candidates[0]
        nxt["_activated"] = True
        logger.info(
            "  [DTA MARGIN] Demand unmet %.2f MT — bringing non-mandatory "
            "%s online at MIN (%.2f TPH)", residual, nxt["asset_name"],
            nxt["min_tph"])

    held = sum(max(0.0, a["fixed_max_tph"] - a["dispatched_tph"])
               for a in abs_all if a["op_hours"] > 0)
    n_act = sum(1 for a in abs_all if _ab_active(a))
    logger.info(
        "  [DTA MARGIN] Required %.2f TPH (%s); held %.2f TPH across %d "
        "active ABs%s",
        req_margin_tph, req_src, held, n_act,
        "" if held >= req_margin_tph - 0.001
        else f"  *** SHORTFALL {req_margin_tph - held:.2f} TPH ***")
    if residual > 0.01:
        logger.warning(
            "  [DTA MARGIN] Steam deficit %.2f MT — all online HRSG/AB "
            "capacity exhausted", residual)


# --- runtime patch -----------------------------------------------------------

_ANCHOR = """        if net_shp_to_dispatch > 0 and steam_margin_report:
            # ── C2 merit-order dispatch (site spec): mandatory ABs @ min →
            # HRSG supplementary to max → activate next-priority offline AB
            # @ min (full redispatch each activation) → ramp ABs by priority.
            _dispatch_steam_c2(dispatch_assets, net_shp_to_dispatch)"""

_INJECTION = """        elif net_shp_to_dispatch > 0 and plant_id.upper() == _DTA_PLANT_ID:
            # ── DRAFT: DTA site-spec AB spinning-margin dispatch
            _dispatch_steam_dta_margin(dispatch_assets, net_shp_to_dispatch)"""


def apply_draft_patch():
    # expose helper to the module namespace where the branch executes
    de._dispatch_steam_dta_margin = _dispatch_steam_dta_margin
    fn_name = None
    for name, fn in inspect.getmembers(de, inspect.isfunction):
        try:
            src = inspect.getsource(fn)
        except (OSError, TypeError):
            continue
        if "_dispatch_steam_c2(dispatch_assets, net_shp_to_dispatch)" in src and "_dispatch_steam_c2" not in name:
            fn_name = name
            target_src = src
            break
    if not fn_name:
        raise RuntimeError("dispatch function containing C2 branch not found")
    if _ANCHOR not in target_src:
        raise RuntimeError("anchor block not found — source changed?")
    patched = target_src.replace(_ANCHOR, _ANCHOR + "\n" + _INJECTION, 1)
    patched = textwrap.dedent(patched)
    ns = dict(de.__dict__)
    exec(compile(patched, "<draft_margin>", "exec"), ns)
    setattr(de, fn_name, ns[fn_name])
    # callers did `from engine.dispatch_engine import dispatch_steam` —
    # they hold a stale bound reference, so rebind inside each importer too
    for mod_name in ("engine.u4u_iteration_loop", "engine.jmd_orchestrator",
                     "engine.calculator"):
        mod = sys.modules.get(mod_name)
        if mod is not None and getattr(mod, fn_name, None) is not ns[fn_name]:
            if hasattr(mod, fn_name):
                setattr(mod, fn_name, ns[fn_name])
    print(f"[DRAFT] Patched {fn_name} — DTA routed to AB-margin dispatch")


def main():
    patch = "--baseline" not in sys.argv
    if patch:
        apply_draft_patch()
        print("[DRAFT] Running DTA 5/2026 with AB-margin dispatch (no save)…")
    else:
        print("[DRAFT] Running DTA 5/2026 baseline (no save)…")

    res = run_month(plant_id=DTA_PLANT_ID, month=5, year=2026, save_to_db=False)
    print("\n===================  RESULT (margin=%s)  ===================" % patch)
    print("success:", res.get("success"), "| message:", res.get("message"))
    u4u = res.get("u4u_iteration") or {}
    print("converged:", u4u.get("converged"), "| iters:", u4u.get("iterations_used"))
    sr = u4u.get("final_steam_result") or {}
    print("steam gen:", f"{sr.get('total_generation_mt', 0):,.2f}",
          "| deficit:", f"{sr.get('deficit_mt', 0):,.2f}",
          "| surplus:", f"{sr.get('surplus_mt', 0):,.2f}")
    dd = sr.get("demand_detail") or {}
    for g in ("lp", "mp", "hp", "shp"):
        keys = {k: dd.get(f"{g}_{k}") for k in ("process", "fixed", "letdown", "byproduct", "net")}
        if any(v is not None for v in keys.values()):
            print(f"    {g.upper():4} " + "  ".join(
                f"{k}={v:,.2f}" for k, v in keys.items() if v is not None))
    print("  steam assets:")
    for a in sorted(sr.get("assets", []), key=lambda x: x.get("asset_name", "")):
        if float(a.get("total_output_mt", 0) or 0) > 0 or a.get("op_hours", 0) > 0:
            print(
                f"    {str(a.get('asset_name'))[:28]:30} hrs={a.get('op_hours', 0):>6} "
                f"supp={float(a.get('supplementary_mt', a.get('dispatched_mt', 0)) or 0):>12,.2f} "
                f"free={float(a.get('free_steam_mt', 0) or 0):>12,.2f} "
                f"total={float(a.get('total_output_mt', 0) or 0):>12,.2f}")


if __name__ == "__main__":
    main()
