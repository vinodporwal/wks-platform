"""
JMD-Wide Orchestrator — single-model run with pooled power dispatch
====================================================================

Drives all five JMD CPPs through one coupled monthly iteration:

    each iteration:
        1. per-CPP dispatch demands (process + fixed + U4U increment - CTU)
        2. ONE pooled power dispatch across all CPP assets by priority
        3. split the dispatch back per CPP (owning plant's norms/curves)
        4. per-CPP steam dispatch + U4U cascade (unchanged engine code)
        5. convergence check across all five CPPs

Inter-CPP power plugs are DERIVED (plug = demand - generation, net of CTU),
not prescribed from BPC — they are reported as pool transfers and validated
against the BPC baseline by the CLI layer.  Steam, water, nitrogen, and all
other utility logic is identical to the per-CPP run; only the power
dispatch step is pooled.

Read-only: this module never writes to the database.
"""

import logging
import time

import pandas as pd

from plant_mapper import PLANT_REGISTRY
from database.queries import (
    fetch_import_power,
    fetch_gt_heat_rate_lookup,
    fetch_hrsg_heat_rate_lookup,
    fetch_auxboiler_heat_rate_lookup,
)
from engine.calculator import (
    _get_demands,
    _get_interplant_export_demands,
    _fy_string,
    _fy_months,
)
from engine.dispatch_engine import dispatch_power_pool, dispatch_steam
from engine.norms_reader_factory import get_norms_reader
from engine.steam_transfer_router import (resolve_steam_transfers,
                                          validate_transfers,
                                          steam_balance_report)
from engine.u4u_iteration_loop import (
    U4UIterationLoop,
    MAX_ITERATIONS,
    FIXED_CONSUMPTION_NORM_TYPE,
    _normalize_for_match,
)

logger = logging.getLogger(__name__)

# JMD pool membership — all five logical CPPs in display order.
CPP_ORDER = (
    "A4AF8441-73AD-4F9F-BCF4-6734E8202F7A",  # DTA-CPP
    "F6D82E68-C3B6-494F-9905-48F19DC611E3",  # DTA-PCG-CPP
    "2DFEE33F-4CFD-4887-B9DD-53388AA95271",  # SEZ-CPP
    "D2C7FBAD-7E00-4642-B3B2-5A768FAC8D45",  # SEZ-PCG-CPP
    "BA558F95-8A3F-4769-9C78-FF7B6C639DDF",  # C2-CPP
)


def cpp_name(plant_id: str) -> str:
    """Logical CPP display name for a plant UUID."""
    return PLANT_REGISTRY.get(plant_id.upper(), {}).get("name", plant_id)


# ---------------------------------------------------------------------------
# Cross-CPP utility transfer routing
# ---------------------------------------------------------------------------
# When a producer in CPP-A consumes a material issued by CPP-B's utility
# plant, that consumption is demand on B's producer, not on A's.  These are
# the BPC Distribution Mapping plant names that own utility generation —
# anything else (process plants, 'No Plant', asset names) is external.
_ISSUING_PLANT_TO_CPP = {
    "JMD - Utility Plant":            "A4AF8441-73AD-4F9F-BCF4-6734E8202F7A",
    "JMD - Utility/Power Dist":       "A4AF8441-73AD-4F9F-BCF4-6734E8202F7A",
    "JMD - SEZ Utility Plant":        "2DFEE33F-4CFD-4887-B9DD-53388AA95271",
    "JMD - SEZ Distribution (Power)": "2DFEE33F-4CFD-4887-B9DD-53388AA95271",
    "JMD - C2 Utility Plant":         "BA558F95-8A3F-4769-9C78-FF7B6C639DDF",
    "JMD - DTA-C2 Power & UTILITY":   "BA558F95-8A3F-4769-9C78-FF7B6C639DDF",
    "RIL-JW Plant-DTA PCG":           "F6D82E68-C3B6-494F-9905-48F19DC611E3",
    "RIL-JW Plant-SEZ PCG":           "D2C7FBAD-7E00-4642-B3B2-5A768FAC8D45",
}
_ISSUING_PLANT_TO_CPP_NORM = {
    _normalize_for_match(k): v for k, v in _ISSUING_PLANT_TO_CPP.items()
}

# Ambiguity warnings are emitted once per run, not per iteration.
_warned_ambiguous_xfers = set()


def _issuing_plant_to_cpp(issuing: str):
    """Map an issuing-plant name to its CPP plant_id, or None if the name is
    not one of the five CPPs' utility/distribution plants."""
    return _ISSUING_PLANT_TO_CPP_NORM.get(_normalize_for_match(issuing or ""))


def _is_transferable_material(material: str, sender_producers: set) -> bool:
    """A material routes cross-CPP only if the SENDER actually produces it as
    a utility.  Steam and power are excluded — steam stays per-CPP for now
    and power is already pooled."""
    if material not in sender_producers:
        return False
    n = _normalize_for_match(material)
    return "steam" not in n and "power" not in n


def _collect_utility_transfers(loops: dict, prepared: list, details: dict):
    """Compute cross-CPP utility demand for the current iteration.

    Leg 1 (U4U): for every consumption row in the consumer's norms whose
    issuing plant belongs to another CPP, quantity = producer generation ×
    norm (or the fixed quantity for fixed-consumption norms) routes to the
    sender's demand for that material.  Raw ``all_consumption_norms`` rows
    are scanned, so rows the own-plant duplicate filter dropped are still
    captured.

    Leg 2 (process/fixed): the consumer's initial demand for a material it
    does not produce routes to the CPP that does (resolved via the observed
    issuing edges; unambiguous single-producer materials route directly).

    ``details`` is this iteration's detail records per plant — used to build
    each consumer's producer/asset generation map (the dynamic table is only
    built post-convergence).

    Returns (ext_demands, transfer_rows):
        ext_demands[sender_pid][material] = total qty owed by the sender
        transfer_rows                     = per-edge audit rows for reporting
    """
    ext_demands = {pid: {} for pid in prepared}
    transfer_rows = []
    observed_senders = {}  # (consumer_pid, material) -> sender_pid

    for pid in prepared:  # consumer side
        loop = loops[pid]
        all_norms = getattr(loop, "all_consumption_norms", None) \
            or loop.consumption_norms

        # producer/asset name -> generation from this iteration's records
        gen_map = {}
        for rec in details.get(pid, []):
            gen_map[_normalize_for_match(rec.get("producer", ""))] = \
                rec.get("generation", 0.0)

        for producer_name, info in all_norms.items():
            for c in info.get("consumptions", []):
                issuing = c.get("issuing_plant") or c.get("material_uom", "")
                sender = _issuing_plant_to_cpp(issuing)
                if not sender or sender == pid or sender not in loops:
                    continue
                material = c["material"]
                if not _is_transferable_material(
                    material, loops[sender]._all_producers
                ):
                    continue
                if c.get("norm_type") == FIXED_CONSUMPTION_NORM_TYPE:
                    qty = float(c.get("quantity", 0.0) or 0.0)
                else:
                    gen = gen_map.get(
                        _normalize_for_match(c.get("source_plant") or ""),
                        gen_map.get(_normalize_for_match(producer_name), 0.0),
                    )
                    qty = gen * float(c.get("norm", 0.0) or 0.0)
                if qty == 0.0:
                    continue
                ext_demands[sender][material] = (
                    ext_demands[sender].get(material, 0.0) + qty
                )
                observed_senders.setdefault((pid, material), sender)
                transfer_rows.append({
                    "consumer_plant_id": pid,
                    "consumer_producer": producer_name,
                    "material": material,
                    "issuing_plant": issuing,
                    "sender_plant_id": sender,
                    "quantity": qty,
                    "leg": "u4u",
                })

    for pid in prepared:  # process/fixed leg
        loop = loops[pid]
        own = loop._all_producers
        for material, qty in (loop._initial_utility_demands or {}).items():
            if not qty or material in own:
                continue
            sender = observed_senders.get((pid, material))
            if sender is None:
                producers = [
                    other for other in prepared
                    if other != pid
                    and _is_transferable_material(
                        material, loops[other]._all_producers
                    )
                ]
                if len(producers) == 1:
                    sender = producers[0]
                else:
                    if producers and (pid, material) not in _warned_ambiguous_xfers:
                        _warned_ambiguous_xfers.add((pid, material))
                        logger.warning(
                            "  [XFER] %s demand '%s'=%.2f: ambiguous senders %s — "
                            "no issuing edge observed, left unrouted",
                            cpp_name(pid), material, qty,
                            [cpp_name(p) for p in producers],
                        )
                    continue
            ext_demands[sender][material] = (
                ext_demands[sender].get(material, 0.0) + float(qty)
            )
            transfer_rows.append({
                "consumer_plant_id": pid,
                "consumer_producer": "(process+fixed)",
                "material": material,
                "issuing_plant": "",
                "sender_plant_id": sender,
                "quantity": float(qty),
                "leg": "process_fixed",
            })

    return ext_demands, transfer_rows


def _load_cpp_inputs(plant_id: str, month: int, year: int,
                     include_dynamic_exports: bool = True) -> dict:
    """Fetch one CPP's inputs — mirrors the fetch block of run_month().

    Returns:
        {
            "demands":                  merged process+fixed demand dict,
            "import_power":             fetch_import_power result,
            "gt_df":                    GT heat rate DataFrame (or None),
            "hrsg_df":                  HRSG+AUXB heat rate DataFrame (or None),
            "norms_reader":             ODS/DB norms reader,
            "interplant_export_demands": export obligations dict,
        }
    """
    demands = _get_demands(plant_id, month, year)

    import_power = fetch_import_power(plant_id, month, year) or {
        "success": False, "total_mwh": 0.0, "per_source": [],
    }

    hrsg_df = fetch_hrsg_heat_rate_lookup(plant_id, month, year)
    auxb_df = fetch_auxboiler_heat_rate_lookup(plant_id, month, year)
    # Aux boiler curves share the HRSG column shape — merge them so the
    # reverse MMBTU norm applies to both asset kinds (same as run_month).
    if auxb_df is not None and not auxb_df.empty:
        if hrsg_df is None or hrsg_df.empty:
            hrsg_df = auxb_df
        else:
            hrsg_df = pd.concat([hrsg_df, auxb_df], ignore_index=True)
    gt_df = fetch_gt_heat_rate_lookup(plant_id, month, year)

    norms_reader = get_norms_reader(plant_id, month, year)
    if norms_reader.is_available:
        norms_reader.log_all_norms()

    interplant_export_demands = (
        _get_interplant_export_demands(plant_id, month, year)
        if include_dynamic_exports else {}
    )

    return {
        "demands": demands,
        "import_power": import_power,
        "gt_df": gt_df,
        "hrsg_df": hrsg_df,
        "norms_reader": norms_reader,
        "interplant_export_demands": interplant_export_demands,
    }


def run_jmd_month(month: int, year: int,
                  include_dynamic_exports: bool = True,
                  max_iterations: int = MAX_ITERATIONS,
                  route_interplant_utilities: bool = True,
                  steam_transfer_mode: str = "bpc") -> dict:
    """Run the JMD complex as a single model for one month.

    Power is dispatched once per iteration across the combined asset pool of
    all five CPPs (min-load-first + priority ramp).  Each CPP's steam
    dispatch, U4U cascade, and convergence logic is unchanged — the loop
    iterates in lockstep until every CPP's U4U demands are stable.

    With ``route_interplant_utilities`` (default), each CPP's consumption of
    a material issued by another CPP's utility plant routes to the sender's
    demand — the sender sees its own process+fixed+U4U plus every consumer
    CPP's demand for that utility.  Steam and power materials are excluded.
    The legacy ``interplant_export_demands`` path is disabled in that mode
    because it would double-count the same edges.

    Args:
        month:                     1-12
        year:                      calendar year
        include_dynamic_exports:   legacy export calc; ignored when routing
                                   is enabled (kept for A/B runs)
        max_iterations:            cap on the coupled loop
        route_interplant_utilities: enable cross-CPP utility demand routing
        steam_transfer_mode:       'bpc' (default) pins inter-site steam
                                   transfers to the booked STEAM(<grade>)
                                   plugs; 'solve' derives them each
                                   iteration from per-CPP residuals within
                                   the documented TPH bands; 'off' keeps the
                                   legacy plug handling (A/B comparison).

    Returns:
        {
            "month", "year", "fy", "converged", "iterations_used",
            "plants":   {plant_id: u4u_result-shaped dict + inputs},
            "power_pool": pooled dispatch summary (per-CPP demand/gen/plug,
                          transfer decomposition, closure residual),
            "utility_transfers": [{consumer, producer, material, sender,
                                   issuing_plant, quantity, leg}],
            "ext_demands": {plant_id: {material: qty}},
            "execution_time_seconds",
        }
    """
    start = time.time()
    fy = _fy_string(month, year)

    logger.info("")
    logger.info("  %s", "=" * 78)
    logger.info("  JMD SINGLE-MODEL RUN  %s/%d  (%s)", month, year, fy)
    logger.info("  %s", "=" * 78)

    inputs = {}
    loops = {}
    for plant_id in CPP_ORDER:
        inputs[plant_id] = _load_cpp_inputs(
            plant_id,
            month, year,
            # Routing makes the legacy static/dynamic export path redundant —
            # keeping both would double-count the same edges.
            include_dynamic_exports and not route_interplant_utilities,
        )
        loops[plant_id] = U4UIterationLoop(
            plant_id=plant_id,
            month=month,
            year=year,
            initial_demands=inputs[plant_id]["demands"],
            ods_reader=inputs[plant_id]["norms_reader"],
            import_power=inputs[plant_id]["import_power"],
            gt_heat_rate_df=inputs[plant_id]["gt_df"],
            hrsg_heat_rate_df=inputs[plant_id]["hrsg_df"],
            interplant_export_demands=inputs[plant_id]["interplant_export_demands"],
            pooled_power=True,
            steam_transfer_mode=steam_transfer_mode,
        )

    prepared = []
    for plant_id in CPP_ORDER:
        if loops[plant_id].prepare():
            prepared.append(plant_id)
        else:
            logger.warning(
                "  [JMD] %s has no consumption norms — excluded from pool",
                cpp_name(plant_id),
            )

    u4u_demands = {
        pid: {m: 0.0 for m in loops[pid]._all_producers} for pid in prepared
    }
    prev_total = {
        pid: {m: 0.0 for m in loops[pid]._all_producers} for pid in prepared
    }
    ext_demands = {pid: {} for pid in prepared}
    prev_ext = {pid: {} for pid in prepared}
    utility_transfers = []
    # Inter-site steam transfer channel (E1-E6 edge model, see
    # steam_transfer_router.py).  C2 has no edges and stays isolated.
    ext_steam = {pid: {} for pid in prepared}
    prev_ext_steam = {pid: {} for pid in prepared}
    steam_transfers = []
    if steam_transfer_mode != "off":
        logger.info("  [JMD] Inter-site steam transfers: mode=%s", steam_transfer_mode)
        # BPC-pinned transfers are static — compute them once so iteration 1
        # already dispatches with the correct import/export obligations
        # (avoids a first-iteration overshoot before convergence).
        if steam_transfer_mode == "bpc":
            ext_steam, steam_transfers = resolve_steam_transfers(
                steam_transfer_mode, loops, prepared,
                month=month, year=year,
            )

    jmd_converged = False
    converged_by_plant = {}
    iterations_used = 0
    power_results = {}
    steam_results = {}
    total_demands = {}
    pool_summary = {}

    for iteration in range(1, max_iterations + 1):
        iterations_used = iteration

        # 1. Per-CPP demands → dispatch demands (power plug injection off).
        #    ext_demands carries utilities owed to this CPP by consumers in
        #    other CPPs (computed at the end of the previous iteration).
        dispatch_demands = {}
        total_demands = {}
        for pid in prepared:
            loop = loops[pid]
            td = {
                m: loop._initial_utility_demands.get(m, 0.0)
                   + u4u_demands[pid].get(m, 0.0)
                   + ext_demands[pid].get(m, 0.0)
                for m in loop._all_producers
            }
            total_demands[pid] = td
            dd = loop._build_dispatch_demands(td)
            # Inter-site steam adjustments from the previous iteration's
            # resolution: positive = export obligation, negative = import
            # credit.  Applied directly to dispatch demands so the signal
            # reaches the cascade for every plant (including the branching
            # plants whose SHP path skips the U4U increment).
            for m, q in ext_steam.get(pid, {}).items():
                dd[m] = dd.get(m, 0.0) + q
            dispatch_demands[pid] = dd

        # 2. ONE pooled power dispatch across all five CPPs.
        pool = dispatch_power_pool(
            prepared, month, year,
            demands_by_plant=dispatch_demands,
            ods_readers={pid: loops[pid].ods_reader for pid in prepared},
            gt_heat_rate_dfs={pid: inputs[pid]["gt_df"] for pid in prepared},
        )
        pool_summary = pool["pool"]
        power_results = pool["plants"]

        # 3. Per-CPP steam dispatch — unchanged engine code.
        steam_results = {}
        for pid in prepared:
            steam_results[pid] = dispatch_steam(
                pid, month, year,
                power_result=power_results[pid],
                demands=dispatch_demands[pid],
                ods_reader=loops[pid].ods_reader,
            )

        # 4. Per-CPP U4U cascade + convergence check.
        new_u4u = {}
        details = {}
        for pid in prepared:
            loop = loops[pid]
            u4u, detail_records = loop._calculate_all_u4u(
                power_results[pid], steam_results[pid], total_demands[pid],
            )
            new_u4u[pid] = u4u
            details[pid] = detail_records
            loop._log_iteration(
                iteration, total_demands[pid], prev_total[pid],
                u4u, u4u_demands[pid], detail_records,
                power_results[pid], steam_results[pid],
            )

        # Cross-CPP utility transfers — computed from this iteration's
        # results, applied to senders' demands next iteration.
        if route_interplant_utilities:
            ext_demands, utility_transfers = _collect_utility_transfers(
                loops, prepared, details
            )
            if iteration == 1 or utility_transfers:
                logger.info("")
                logger.info("  [JMD] Inter-CPP utility demand this iteration:")
                by_edge = {}
                for t in utility_transfers:
                    key = (t["sender_plant_id"], t["consumer_plant_id"], t["material"])
                    by_edge[key] = by_edge.get(key, 0.0) + t["quantity"]
                for (s, c, m), q in sorted(by_edge.items()):
                    logger.info(
                        "    %s -> %s | %-25s %.2f",
                        cpp_name(s), cpp_name(c), m, q,
                    )

        # Inter-site steam transfers — computed from this iteration's
        # results (solve mode) or pinned to BPC plugs, applied next
        # iteration via ext_steam.
        if steam_transfer_mode != "off":
            ext_steam, steam_transfers = resolve_steam_transfers(
                steam_transfer_mode, loops, prepared,
                steam_results=steam_results,
                total_demands=total_demands,
                ext_applied=ext_steam,
                month=month, year=year,
                u4u_demands=new_u4u,
            )
            for w in validate_transfers(steam_transfers):
                logger.warning("  [STEAM-XFER] %s", w)
            if steam_transfers:
                logger.info("")
                logger.info("  [JMD] Inter-site steam transfers this iteration (%s):",
                            steam_transfer_mode)
                for t in steam_transfers:
                    snd = cpp_name(t["sender"]) if t.get("sender") else "-"
                    logger.info(
                        "    %s %s -> %s | %-15s %12.2f MT  (%s)",
                        t.get("edge", "-"), snd, cpp_name(t["receiver"]),
                        t["material"], t["quantity"], t.get("basis", ""),
                    )

        for pid in prepared:
            loop = loops[pid]
            # Convergence compares own U4U + incoming routed demand so a
            # sender only converges once its external obligations settle.
            conv = {
                m: new_u4u[pid].get(m, 0.0)
                   + ext_demands[pid].get(m, 0.0)
                   + ext_steam[pid].get(m, 0.0)
                for m in loop._all_producers
            }
            prev_conv_map = {
                m: u4u_demands[pid].get(m, 0.0)
                   + prev_ext[pid].get(m, 0.0)
                   + prev_ext_steam[pid].get(m, 0.0)
                for m in loop._all_producers
            }
            converged_by_plant[pid] = loop._check_convergence(conv, prev_conv_map)
        prev_ext = ext_demands
        prev_ext_steam = ext_steam

        for pid in prepared:
            loop = loops[pid]
            loop.iteration_history.append({
                "iteration": iteration,
                "total_demands": dict(total_demands[pid]),
                "u4u_demands": dict(new_u4u[pid]),
                "power_generation_mwh": power_results[pid].get(
                    "total_generation_mwh", 0.0),
                "steam_generation_mt": steam_results[pid].get(
                    "total_generation_mt", 0.0),
                "converged": converged_by_plant[pid],
            })
            loop.iterations_used = iteration
            loop.final_power_result = power_results[pid]
            loop.final_steam_result = steam_results[pid]
            loop.final_u4u_demands = dict(new_u4u[pid])
            loop.final_total_demands = dict(total_demands[pid])
            loop.final_detail_records = details[pid]

        u4u_demands = new_u4u
        prev_total = total_demands

        if all(converged_by_plant.values()):
            jmd_converged = True
            for pid in prepared:
                loops[pid].converged = True
            logger.info("")
            logger.info("  [JMD] ALL CPPs CONVERGED in %d iterations", iteration)
            break

    # Diagnostic: when transfers are pinned to BPC plugs, also resolve what
    # the converged residuals imply per edge ("model" side for reporting).
    # ext is discarded — this never affects dispatch.
    if steam_transfer_mode == "bpc" and steam_transfers and steam_results:
        try:
            _, solved_rows = resolve_steam_transfers(
                "solve", loops, prepared,
                steam_results=steam_results,
                total_demands=total_demands,
                ext_applied=ext_steam,
                month=month, year=year,
                u4u_demands=u4u_demands,
            )
            solved_by_edge = {r.get("edge"): r for r in solved_rows}
            for t in steam_transfers:
                srow = solved_by_edge.get(t.get("edge"))
                if not srow:
                    continue
                t["model_qty"] = srow["quantity"]
                if (srow.get("sender"), srow.get("receiver")) != (
                        t.get("sender"), t.get("receiver")):
                    t["model_sender"] = srow.get("sender")
                    t["model_receiver"] = srow.get("receiver")
        except Exception as e:
            logger.warning("  [STEAM-XFER] Model-implied diagnostic failed: %s", e)

    # Per-CPP per-grade steam balance for reporting (power-pool style).
    # Read-only diagnostic — never feeds back into dispatch.
    steam_balance = []
    if steam_transfer_mode != "off" and steam_transfers and steam_results:
        try:
            steam_balance = steam_balance_report(
                loops, prepared, steam_results, total_demands,
                ext_steam, steam_transfers,
                u4u_demands=u4u_demands)
        except Exception as e:
            logger.warning("  [STEAM-XFER] Steam balance report failed: %s", e)

    # Consolidated end-of-run warning block — surfaces the final
    # iteration's band clamps, sender shortfalls and audit gaps once, so
    # they are not lost inside per-iteration transfer logs.
    if steam_transfer_mode != "off" and steam_transfers:
        xfer_warns = validate_transfers(steam_transfers)
        if xfer_warns:
            logger.warning("")
            logger.warning("  [STEAM-XFER] ======== TRANSFER WARNING SUMMARY ========")
            for w in xfer_warns:
                logger.warning("  [STEAM-XFER] %s", w)
            logger.warning("  [STEAM-XFER] ==============================================")

    for pid in prepared:
        loops[pid].finalize_run()

    plants = {}
    for pid in prepared:
        loop = loops[pid]
        plants[pid] = {
            "plant_id": pid,
            "plant_name": cpp_name(pid),
            "converged": converged_by_plant.get(pid, False),
            "iterations_used": loop.iterations_used,
            "final_power_result": loop.final_power_result,
            "final_steam_result": loop.final_steam_result,
            "final_u4u_demands": loop.final_u4u_demands,
            "final_total_demands": loop.final_total_demands,
            "final_detail_records": loop.final_detail_records,
            "final_dynamic_table": getattr(loop, "final_dynamic_table", []),
            "final_bpc_gen_quantities": loop._bpc_gen_quantities,
            "final_bpc_quantities": loop._bpc_quantities,
            "bpc_baseline_authoritative": loop._bpc_baseline_authoritative,
            "iteration_history": loop.iteration_history,
            "interplant_skipped": dict(loop._interplant_skipped),
            "demands": inputs[pid]["demands"],
            "import_power": inputs[pid]["import_power"],
            # Reporting support — identity lookups + the plant's Power_Dis
            # material name so the report can rebuild BPC-style plug rows.
            "consumption_norms": loop.consumption_norms,
            "power_ods_material": getattr(loop, "_power_ods_material", "") or "",
            # Routed cross-CPP utility demand owed to this CPP (sender view)
            "ext_demands": dict(ext_demands.get(pid, {})),
            # Inter-site steam adjustments applied to this CPP's dispatch
            # demands (positive = export obligation, negative = import credit)
            "ext_steam": dict(ext_steam.get(pid, {})),
        }

    return {
        "month": month,
        "year": year,
        "fy": fy,
        "converged": jmd_converged,
        "iterations_used": iterations_used,
        "converged_by_plant": converged_by_plant,
        "plants": plants,
        "power_pool": pool_summary,
        "utility_transfers": utility_transfers,
        "steam_transfers": steam_transfers,
        "steam_balance": steam_balance,
        "steam_transfer_mode": steam_transfer_mode,
        "execution_time_seconds": round(time.time() - start, 2),
    }


def run_jmd_year(fy_start_year: int,
                 include_dynamic_exports: bool = True,
                 max_iterations: int = MAX_ITERATIONS,
                 route_interplant_utilities: bool = True,
                 steam_transfer_mode: str = "bpc") -> dict:
    """Run the JMD single-model for all 12 months of a financial year."""
    results = {}
    for month, year in _fy_months(fy_start_year):
        key = f"{year}_{month:02d}"
        results[key] = run_jmd_month(
            month, year,
            include_dynamic_exports=include_dynamic_exports,
            max_iterations=max_iterations,
            route_interplant_utilities=route_interplant_utilities,
            steam_transfer_mode=steam_transfer_mode,
        )
    return {
        "fy": f"{fy_start_year}-{str(fy_start_year + 1)[-2:]}",
        "months": results,
    }
