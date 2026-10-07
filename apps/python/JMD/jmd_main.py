"""
JMD Single-Model Entry Point
============================
Runs the whole JMD complex (all five CPPs) as one coupled model:
one pooled power dispatch across all CPP assets, per-CPP steam/U4U
unchanged.  Read-only — nothing is saved to the database.

Usage:
    py jmd_main.py --month 4 --year 2026
    py jmd_main.py --month 4 --year 2026 --no-dynamic-exports
    py jmd_main.py --fy 2026            # all 12 months
    py jmd_main.py --month 4 --year 2026 --no-bpc
"""

import sys
import os
import argparse

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "scripts"))

from plant_mapper import PLANT_REGISTRY
from engine.jmd_orchestrator import run_jmd_month, run_jmd_year, CPP_ORDER, cpp_name
from engine.jmd_excel_report import write_jmd_month_report
from engine.report_logger import setup_logging

JMD_ROOT = os.path.dirname(os.path.abspath(__file__))
OUTPUT_DIR = os.path.join(JMD_ROOT, "output", "JMD")

_MONTHS_APR_FIRST = ("Apr", "May", "Jun", "Jul", "Aug", "Sep",
                     "Oct", "Nov", "Dec", "Jan", "Feb", "Mar")


def _bpc_power_baseline():
    """Load the BPC power baseline (signed plugs / POWERGEN / CTU) for all months."""
    from jmd_power_balance import read_bpc_power_baseline, DEFAULT_BPC_OUTPUT
    return read_bpc_power_baseline(DEFAULT_BPC_OUTPUT)


def _print_power_pool_summary(result: dict, bpc_baseline=None) -> None:
    pool = result.get("power_pool") or {}
    month = result["month"]
    year = result["year"]
    month_label = _MONTHS_APR_FIRST[(month - 4) % 12]

    print()
    print("=" * 78)
    print(f"  JMD POWER POOL — {month_label} {year}")
    print("=" * 78)
    print(f"  Pool demand:      {pool.get('total_demand_mwh', 0.0):>14,.2f} MWh")
    print(f"  Pool generation:  {pool.get('total_generation_mwh', 0.0):>14,.2f} MWh")
    print(f"  Surplus:          {pool.get('surplus_mwh', 0.0):>14,.2f} MWh")
    print(f"  Deficit:          {pool.get('deficit_mwh', 0.0):>14,.2f} MWh")
    print(f"  Plug closure:     {pool.get('closure_residual_mwh', 0.0):>14,.6f} MWh")
    print()
    print(f"  {'CPP':<13} {'Demand MWh':>14} {'Gen MWh':>14} {'Plug MWh':>14}"
          + (f" {'BPC Plug':>12} {'Plug Diff':>12} {'BPC Gen':>14} {'Gen Diff':>12}" if bpc_baseline else ""))
    print("  " + "-" * (104 if bpc_baseline else 57))

    bpc_month = (bpc_baseline or {}).get(month_label, {})
    for pid in CPP_ORDER:
        plants = result.get("plants", {})
        p = plants.get(pid)
        if not p:
            continue
        pr = p.get("final_power_result") or {}
        plug = pr.get("plug_mwh", 0.0)
        gen = pr.get("total_generation_mwh", 0.0)
        # dispatch demand is net of CTU import — show true total demand
        ext = (p.get("import_power") or {}).get("total_mwh", 0.0)
        dem = pr.get("demand_mwh", 0.0) + ext
        line = (f"  {cpp_name(pid):<13} {dem:>14,.2f} "
                f"{gen:>14,.2f} {plug:>14,.2f}")
        if bpc_baseline:
            bpc = bpc_month.get(cpp_name(pid), {})
            bpc_plug = bpc.get("plug_mwh", 0.0)
            bpc_gen = bpc.get("generation_mwh", 0.0)
            line += (f" {bpc_plug:>12,.2f} {plug - bpc_plug:>12,.2f}"
                     f" {bpc_gen:>14,.2f} {gen - bpc_gen:>12,.2f}")
        print(line)
    print()

    transfers = pool.get("transfers") or []
    if transfers:
        print("  Inter-CPP pool transfers (derived):")
        for t in transfers:
            print(f"    {cpp_name(t['sender_plant_id'])} -> "
                  f"{cpp_name(t['receiver_plant_id'])}: "
                  f"{t['quantity_mwh']:,.2f} MWh")
        print()

    # BPC plug closure check
    if bpc_baseline:
        bpc_residual = sum(v.get("plug_mwh", 0.0) for v in bpc_month.values())
        print(f"  BPC plug closure residual: {bpc_residual:,.6f} MWh")


def _print_utility_transfers(result: dict, bpc_edges: dict = None) -> None:
    """Print routed inter-CPP utility demand, optionally vs BPC edges.

    bpc_edges: {(sender_cpp_name, consumer_cpp_name, material): qty}
    """
    transfers = result.get("utility_transfers") or []
    if not transfers:
        return

    # Aggregate per edge, split by leg (u4u consumptions vs process/fixed)
    edges = {}
    for t in transfers:
        key = (t["sender_plant_id"], t["consumer_plant_id"], t["material"])
        e = edges.setdefault(key, {"u4u": 0.0, "process_fixed": 0.0})
        e[t.get("leg", "u4u")] += t["quantity"]

    print()
    print("  Inter-CPP utility transfers (derived):")
    hdr = f"    {'Sender':<13} {'Consumer':<13} {'Material':<26} {'U4U Qty':>14} {'Proc+Fix Qty':>14}"
    if bpc_edges:
        hdr += f" {'BPC U4U Qty':>14} {'Diff %':>10}"
    print(hdr)
    print("    " + "-" * (95 if bpc_edges else 81))
    for (s, c, m), e in sorted(edges.items(), key=lambda kv: (cpp_name(kv[0][0]), cpp_name(kv[0][1]), kv[0][2])):
        line = (f"    {cpp_name(s):<13} {cpp_name(c):<13} {m:<26} "
                f"{e['u4u']:>14,.2f} {e['process_fixed']:>14,.2f}")
        if bpc_edges:
            bq = bpc_edges.get((cpp_name(s), cpp_name(c), m))
            if bq is not None:
                diff = (e['u4u'] - bq) / bq * 100.0 if bq else None
                line += f" {bq:>14,.2f} {diff:>10.1f}"
            else:
                line += f" {'—':>14} {'—':>10}"
        print(line)
    print()


def _bpc_utility_edges(month: int) -> dict:
    """Cross-CPP non-steam utility edges from the BPC Norm/Qty/Cost file.

    Returns {(sender_cpp_name, consumer_cpp_name, material): qty} summed
    over the BPC consumption rows for the given month.  Sender identity uses
    the same explicit issuing-plant table as the orchestrator, so non-CPP
    sources (process plants, 'No Plant') are never mislabelled.
    """
    from engine.jmd_excel_report import load_bpc_rows
    from engine.jmd_orchestrator import _issuing_plant_to_cpp

    edges = {}
    for r in load_bpc_rows(month):
        consumer = r["cpp"]
        sender_pid = _issuing_plant_to_cpp(r["issuing_plant"])
        if not sender_pid:
            continue
        sender = cpp_name(sender_pid)
        if not consumer or consumer == sender:
            continue
        mat = r["material"]
        n = "".join(ch.lower() for ch in mat if ch.isalnum())
        if "steam" in n or "power" in n:
            continue
        edges[(sender, consumer, mat)] = edges.get((sender, consumer, mat), 0.0) + r["quantity"]
    return edges


def _print_steam_transfers(result: dict) -> None:
    """Print resolved inter-site steam transfers."""
    transfers = result.get("steam_transfers") or []
    if not transfers:
        return
    from engine.jmd_excel_report import _steam_edge_meta, _steam_grade, \
        _STEAM_KIND_LABEL
    import calendar as _cal
    mode = (result.get("steam_transfer_mode") or "").upper()
    try:
        hours = _cal.monthrange(result["year"], result["month"])[1] * 24.0
    except Exception:
        hours = 720.0
    meta = _steam_edge_meta()
    balance = result.get("steam_balance") or []
    if balance:
        print()
        print("  Per-CPP steam balance vs BPC (MT):")
        print(f"    {'CPP':<13} {'Grade':<5} {'Demand':>11} {'Generation':>11} "
              f"{'BPC Gen':>11} {'Gen%':>7} {'Need':>11} {'BPC net':>11} "
              f"{'Model net':>11} {'Uncovered':>11}")
        print("    " + "-" * 103)
        order = {pid: i for i, pid in enumerate(CPP_ORDER)}
        gorder = {"SHP": 0, "HP": 1, "IHP": 2, "MP": 3, "IP": 4,
                  "LP": 5, "LLP": 6, "EHP": 7}
        for b in sorted(balance,
                        key=lambda x: (order.get(x["plant_id"], 99),
                                       gorder.get(
                                           _steam_grade(x["material"]), 99))):
            gdiff = ((b["supply_mt"] - b["bpc_gen_mt"]) / b["bpc_gen_mt"] * 100
                     if b["bpc_gen_mt"] > 0 and b["supply_mt"] > 0 else None)
            gtxt = f"{gdiff:>6.1f}%" if gdiff is not None else f"{'':>7}"
            print(f"    {cpp_name(b['plant_id']):<13} "
                  f"{_steam_grade(b['material']):<5} "
                  f"{b['demand_mt']:>11,.1f} {b['supply_mt']:>11,.1f} "
                  f"{b['bpc_gen_mt']:>11,.1f} {gtxt} "
                  f"{b['need_mt']:>11,.1f} {b['booked_net_mt']:>11,.1f} "
                  f"{b['model_net_mt']:>11,.1f} {b['residual_mt']:>11,.1f}")
    print()
    print(f"  Inter-site steam transfers ({mode}):")
    print(f"    {'Route':<27} {'Grade':<6} {'Flow type':<24} "
          f"{'BPC booked':>12} {'Model':>12} {'Diff':>10} {'Avg TPH':>8}")
    print("    " + "-" * 101)
    for t in sorted(transfers, key=lambda t: t.get("edge") or "Z"):
        snd = cpp_name(t["sender"]) if t.get("sender") else "-"
        route = f"{snd} -> {cpp_name(t['receiver'])}"
        kind = t.get("kind", "")
        flow = _STEAM_KIND_LABEL.get(kind, kind)
        if t.get("basis") == "audit_only":
            flow = "Audit only"
        elif kind == "residual_plug":
            flow = "Balance trim (no pipeline)"
        booked = t.get("booked_mt") if t.get("basis") == "solved" \
            else t.get("quantity")
        model = t.get("quantity") if t.get("basis") == "solved" \
            else t.get("model_qty")
        diff = model - booked if model is not None and booked else None
        btxt = f"{booked:>12,.1f}" if booked is not None else f"{'':>12}"
        mt_ = f"{model:>12,.1f}" if model is not None else f"{'':>12}"
        dtxt = f"{diff:>10,.1f}" if diff is not None else f"{'':>10}"
        tph = f"{booked / hours:>8,.1f}" if booked else f"{'':>8}"
        print(f"    {route:<27} {_steam_grade(t['material']):<6} "
              f"{flow:<24} {btxt} {mt_} {dtxt} {tph}")
    print()


def _print_plant_summary(result: dict) -> None:
    print()
    print("=" * 78)
    print("  PER-CPP SUMMARY")
    print("=" * 78)
    for pid in CPP_ORDER:
        p = (result.get("plants") or {}).get(pid)
        if not p:
            print(f"  {cpp_name(pid):<13}  (not prepared — no norms)")
            continue
        pr = p.get("final_power_result") or {}
        sr = p.get("final_steam_result") or {}
        print(f"  {cpp_name(pid):<13}  converged={str(p.get('converged')):<5} "
              f"iters={p.get('iterations_used', 0):<3} "
              f"power_gen={pr.get('total_generation_mwh', 0.0):>12,.2f} MWh "
              f"steam_gen={sr.get('total_generation_mt', 0.0):>12,.2f} MT")
    print()


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--month", type=int, default=4)
    parser.add_argument("--year", type=int, default=2026)
    parser.add_argument("--fy", type=int, default=None,
                        help="Run the full financial year (e.g. 2026 → Apr 2026–Mar 2027)")
    parser.add_argument("--no-dynamic-exports", action="store_true",
                        help="Skip the recursive inter-plant export demand calc "
                             "(only relevant with --legacy-exports)")
    parser.add_argument("--no-routing", action="store_true",
                        help="Disable cross-CPP utility demand routing")
    parser.add_argument("--steam-transfers", choices=["off", "bpc", "solve"],
                        default="bpc",
                        help="Inter-site steam transfer mode: 'bpc' pins to "
                             "booked plugs (default), 'solve' derives flows "
                             "from residuals within bands, 'off' = legacy")
    parser.add_argument("--legacy-exports", action="store_true",
                        help="Use the old static/recursive export path instead of "
                             "utility routing (A/B comparison)")
    parser.add_argument("--no-bpc", action="store_true",
                        help="Skip BPC baseline comparison")
    args = parser.parse_args(argv)

    setup_logging()

    route = not (args.no_routing or args.legacy_exports)
    include_exports = args.legacy_exports and not args.no_dynamic_exports
    bpc_baseline = None
    bpc_edges = None
    if not args.no_bpc:
        try:
            bpc_baseline = _bpc_power_baseline()
        except Exception as e:
            print(f"  [BPC] Baseline unavailable: {e}")
        try:
            bpc_edges = _bpc_utility_edges(args.month)
        except Exception as e:
            print(f"  [BPC] Utility edge baseline unavailable: {e}")

    if args.fy is not None:
        year_result = run_jmd_year(
            args.fy, include_dynamic_exports=include_exports,
            route_interplant_utilities=route,
            steam_transfer_mode=args.steam_transfers,
        )
        for key, month_result in year_result["months"].items():
            _print_power_pool_summary(month_result, bpc_baseline)
            _print_utility_transfers(month_result, bpc_edges)
            _print_steam_transfers(month_result)
            _print_plant_summary(month_result)
            path = write_jmd_month_report(month_result, OUTPUT_DIR, bpc_baseline)
            print(f"  Excel saved: {path}")
        print(f"\nJMD FY {year_result['fy']} complete — "
              f"{sum(1 for r in year_result['months'].values() if r.get('converged'))}/12 months converged")
        return 0

    result = run_jmd_month(
        args.month, args.year,
        include_dynamic_exports=include_exports,
        route_interplant_utilities=route,
        steam_transfer_mode=args.steam_transfers,
    )
    _print_power_pool_summary(result, bpc_baseline)
    _print_utility_transfers(result, bpc_edges)
    _print_steam_transfers(result)
    _print_plant_summary(result)
    print(f"  Converged: {result['converged']} in {result['iterations_used']} iterations "
          f"({result['execution_time_seconds']:.1f}s)")
    path = write_jmd_month_report(result, OUTPUT_DIR, bpc_baseline)
    print(f"  Excel saved: {path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
