"""JMD pooled-model validation harness.

Runs (or loads a cached) JMD single-model month and validates the results
against the BPC golden-master files:

    1. Sender Receiver Costcenter.csv — the BPC demand register.
       Column A = utility demanded, column E = the sending CPP utility plant
       node, column I = consuming utility (U4U leg), column O = material
       activity (empty -> process + fixed demand), columns Q..AB = months.
       'Total' rows are the sheet's own totals and are always skipped.

    2. Norm, Qty, Cost .csv — per-producer generation + per-material
       consumption (via engine.jmd_excel_report helpers).

Suites:
    S1  Convergence + pool closure
    S2  Utility demand vs register (total and process+fixed split)
    S3  Utility/asset generation vs BPC gen qty
    S4  Consumption quantities vs BPC Norm,Qty,Cost
    S5  Power pool: per-CPP demand/gen/plug/CTU + per-asset POWERGEN
    S6  Inter-CPP utility transfers vs register edges
    S7  Regression vs a saved JSON baseline (optional)

Usage (from apps/python/JMD):
    py scripts/jmd_validate.py --month 4 --year 2026
    py scripts/jmd_validate.py --month 4 --year 2026 --use-cache
    py scripts/jmd_validate.py --month 4 --year 2026 --save-baseline base.json
    py scripts/jmd_validate.py --month 4 --year 2026 --baseline base.json

Read-only: no database writes, no changes to the calculation engine.
"""

import argparse
import csv
import json
import logging
import os
import pickle
import re
import sys
from collections import defaultdict
from pathlib import Path

_JMD_DIR = Path(__file__).resolve().parent.parent
if str(_JMD_DIR) not in sys.path:
    sys.path.insert(0, str(_JMD_DIR))

from engine.jmd_excel_report import (
    load_bpc_rows,
    _bpc_lookup,
    _bpc_gen_qty,
    _norm_txt,
    _cpp_of_bpc_row,
    _POWER_NODE_BY_CPP,
    BPC_OUTPUT_CSV,
)
from engine.jmd_orchestrator import CPP_ORDER, cpp_name

DTA = "A4AF8441-73AD-4F9F-BCF4-6734E8202F7A"
DTA_PCG = "F6D82E68-C3B6-494F-9905-48F19DC611E3"
SEZ = "2DFEE33F-4CFD-4887-B9DD-53388AA95271"
SEZ_PCG = "D2C7FBAD-7E00-4642-B3B2-5A768FAC8D45"
C2 = "BA558F95-8A3F-4769-9C78-FF7B6C639DDF"

CPP_SHORT = {
    DTA: "DTA-CPP", DTA_PCG: "DTA-PCG-CPP", SEZ: "SEZ-CPP",
    SEZ_PCG: "SEZ-PCG-CPP", C2: "C2-CPP",
}

REGISTER_CSV = _JMD_DIR.parent / "files" / "Sender Receiver Costcenter.csv"
CACHE_DIR = _JMD_DIR / "output" / "validation_cache"

# C2-complex process plants — their utility demands register at the C2 node
# even though their names start with 'JMD - DTA-'.
_C2_PLANTS = {
    "jmddtaldpe", "jmddtalldpe", "jmddtameg", "jmddtac2cracker",
    "jmddtamegc2gantry", "jmdrevprocdtac2",
}


# ---------------------------------------------------------------------------
# Plant-name -> CPP mapping
# ---------------------------------------------------------------------------

def _node_to_cpp(name: str):
    """Map a Sender/Receiver 'CPP Plant' (col E) name to a CPP UUID."""
    n = _norm_txt(name)
    if not n or n == "total":
        return None
    if "sezpcg" in n:
        return SEZ_PCG
    if "dtapcg" in n:
        return DTA_PCG
    if "c2" in n:
        return C2
    if "sez" in n:
        return SEZ
    if ("gtpowerplant" in n or "sgtplant" in n or "gtg" in n
            or "utility" in n):
        return DTA
    return None


def _recv_to_cpp(name: str):
    """Map a Receiver 'Plant' (col M) name to a CPP UUID.

    Handles process plants physically inside the C2 complex whose names
    start with 'JMD - DTA-'.
    """
    n = _norm_txt(name)
    if not n or n == "total":
        return None
    if n in _C2_PLANTS:
        return C2
    if "sezpcg" in n:
        return SEZ_PCG
    if "dtapcg" in n:
        return DTA_PCG
    if "c2" in n:
        return C2
    if "sez" in n:
        return SEZ
    if ("dta" in n or "jamnagar" in n or "township" in n or "fab" in n
            or "aromatics" in n or "hpib" in n or "utility" in n
            or "gtpowerplant" in n or "sgtplant" in n or "gtg" in n):
        return DTA
    return None


# ---------------------------------------------------------------------------
# BPC register (Sender Receiver Costcenter.csv)
# ---------------------------------------------------------------------------

def _month_col(month: int) -> int:
    """Calendar month -> register column index (col Q=16 is April/FY month 1)."""
    return 16 + ((month - 4) % 12)


def _fnum(s) -> float:
    try:
        return float(str(s or "").strip().replace(",", ""))
    except (TypeError, ValueError):
        return 0.0


def load_register(month: int, path: Path = REGISTER_CSV):
    """Parse the BPC demand register for one month.

    Returns:
        demands[(cpp, util_norm)] = total qty (process+fixed+U4U)
        proc_fix[(cpp, util_norm)] = qty where Material Activity (col O) empty
        edges[(sender_cpp, recv_cpp, util_norm)] = cross-CPP qty
        display[(util_norm)] = original utility name text
    """
    col = _month_col(month)
    demands = defaultdict(float)
    proc_fix = defaultdict(float)
    edges = defaultdict(float)
    display = {}

    with Path(path).open("r", encoding="utf-16", newline="") as fh:
        for r in csv.reader(fh, delimiter="\t"):
            if len(r) <= col:
                continue
            util = (r[0] or "").strip()
            node = (r[4] or "").strip()
            if not util or util == "Utility" or util == "Total":
                continue
            # the sheet's own per-block 'Total' rows carry 'Total' in the
            # material/plant/activity columns — never count them.
            if (str(r[8]).strip() == "Total" or str(r[12]).strip() == "Total"
                    or str(r[14]).strip() == "Total"):
                continue
            qty = _fnum(r[col])
            if qty == 0.0:
                continue
            u_n = _norm_txt(util)
            display.setdefault(u_n, util)
            sender_cpp = _node_to_cpp(node)
            recv_cpp = _recv_to_cpp(r[12] if len(r) > 12 else "")
            # process plants appear as senders for return flows — attribute
            # them to their owning CPP (LDPE/LLDPE/MEG etc. are C2's).
            if not sender_cpp:
                sender_cpp = _recv_to_cpp(node)

            if sender_cpp:
                demands[(sender_cpp, u_n)] += qty
                if not str(r[14]).strip():
                    proc_fix[(sender_cpp, u_n)] += qty
                if recv_cpp and recv_cpp != sender_cpp:
                    edges[(sender_cpp, recv_cpp, u_n)] += qty
    return demands, proc_fix, edges, display


# ---------------------------------------------------------------------------
# Check bookkeeping
# ---------------------------------------------------------------------------

class Check:
    __slots__ = ("suite", "cpp", "item", "model", "bpc", "status", "note")

    def __init__(self, suite, cpp, item, model, bpc, status, note=""):
        self.suite, self.cpp, self.item = suite, cpp, item
        self.model, self.bpc = model, bpc
        self.status, self.note = status, note


def _status(model, bpc, tol_pct, tol_abs):
    diff = model - bpc
    band = max(tol_abs, abs(bpc) * tol_pct / 100.0)
    return "PASS" if abs(diff) <= band else "FAIL"


def _pct(model, bpc):
    if not bpc:
        return float("inf") if model else 0.0
    return (model - bpc) / abs(bpc) * 100.0


# ---------------------------------------------------------------------------
# Model result extraction
# ---------------------------------------------------------------------------

def model_demands(result):
    """per-CPP dicts: {util_norm: qty} for total and process+fixed legs."""
    total, pf = {}, {}
    transfers = result.get("utility_transfers") or []
    for pid in CPP_ORDER:
        p = result["plants"][pid]
        td = p.get("final_total_demands") or {}
        u4u = p.get("final_u4u_demands") or {}
        ext = p.get("ext_demands") or {}
        t = {_norm_txt(k): float(v) for k, v in td.items()}
        initial = {}
        for k, v in td.items():
            kn = _norm_txt(k)
            initial[kn] = float(v) - u4u.get(k, 0.0) - ext.get(k, 0.0)
        # proc/fix leg of routed transfers belongs to the sender's PF demand
        for tr in transfers:
            if tr.get("sender_plant_id") == pid and tr.get("leg") == "process_fixed":
                initial[_norm_txt(tr["material"])] = (
                    initial.get(_norm_txt(tr["material"]), 0.0)
                    + float(tr["quantity"]))
        total[pid], pf[pid] = t, initial
    return total, pf


def model_generations(result):
    """per-CPP {producer_norm: generation} from the dynamic U4U table."""
    gens = {}
    for pid in CPP_ORDER:
        m = {}
        for rec in (result["plants"][pid].get("final_dynamic_table") or []):
            prod = _norm_txt(rec.get("producer") or "")
            if prod and prod not in m:
                m[prod] = float(rec.get("generation") or 0.0)
        gens[pid] = m
    return gens


def model_consumptions(result):
    """per-CPP list of (producer, producer_utility, material, quantity)."""
    rows = {}
    for pid in CPP_ORDER:
        lst = []
        for rec in (result["plants"][pid].get("final_dynamic_table") or []):
            lst.append((rec.get("producer") or "",
                        rec.get("producer_utility") or "",
                        rec.get("material") or "",
                        float(rec.get("quantity") or 0.0)))
        rows[pid] = lst
    return rows


def model_power(result):
    """per-CPP {demand,gen,plug,ctu} + per-asset dispatched MWh."""
    out = {}
    for pid in CPP_ORDER:
        p = result["plants"][pid]
        pr = p.get("final_power_result") or {}
        assets = {a.get("asset_name", ""): float(a.get("dispatched_mwh") or 0.0)
                  for a in pr.get("assets", [])}
        out[pid] = {
            "demand": float(pr.get("demand_mwh") or 0.0),
            "gen": float(pr.get("total_generation_mwh") or 0.0),
            "plug": float(pr.get("plug_mwh") or 0.0),
            "ctu": float((p.get("import_power") or {}).get("total_mwh") or 0.0),
            "assets": assets,
        }
    return out


def model_transfers(result):
    """{(sender_cpp, consumer_cpp, material_norm): qty}"""
    m = defaultdict(float)
    for tr in (result.get("utility_transfers") or []):
        key = (tr.get("sender_plant_id"), tr.get("consumer_plant_id"),
               _norm_txt(tr.get("material") or ""))
        m[key] += float(tr.get("quantity") or 0.0)
    return m


# ---------------------------------------------------------------------------
# Suites
# ---------------------------------------------------------------------------

def suite_convergence(result, checks):
    ok = True
    for pid in CPP_ORDER:
        conv = bool(result["plants"][pid].get("converged"))
        checks.append(Check("S1", CPP_SHORT[pid], "U4U converged",
                            1.0 if conv else 0.0, 1.0,
                            "PASS" if conv else "FAIL"))
        ok = ok and conv
    pool = result.get("power_pool") or {}
    resid = abs(float(pool.get("closure_residual_mwh") or 0.0))
    checks.append(Check("S1", "JMD", "Pool closure residual MWh",
                        resid, 0.0, "PASS" if resid <= 0.5 else "FAIL"))
    return checks


def suite_demand(result, reg_dem, reg_pf, disp, checks, tol_pct, tol_abs):
    m_total, m_pf = model_demands(result)
    m_gen = model_generations(result)
    # per-CPP power-asset dispatch (KWH) for 'POWERGEN*' register rows
    m_assets = {}
    for pid in CPP_ORDER:
        assets = {}
        for a in ((result["plants"][pid].get("final_power_result") or {})
                  .get("assets", [])):
            assets[a.get("asset_name", "")] = \
                float(a.get("dispatched_mwh") or 0.0) * 1000.0
        m_assets[pid] = assets

    for pid in CPP_ORDER:
        cpp = CPP_SHORT[pid]
        keys = {u for (c, u) in reg_dem if c == pid} | set(m_total[pid])
        pg_keys = {u for u in keys if u.startswith("powergen")}
        keys -= pg_keys
        if pg_keys:
            # all POWERGEN* register keys = this CPP's total pooled generation
            bpc_pg = sum(reg_dem.get((pid, u), 0.0) for u in pg_keys)
            model_pg = sum(m_assets[pid].values())
            checks.append(Check("S2-gen", cpp, "POWERGEN (all assets)",
                                model_pg, bpc_pg,
                                _status(model_pg, bpc_pg, tol_pct, tol_abs),
                                "pooled power generation"))
        for u in sorted(keys):
            bpc_t = reg_dem.get((pid, u), 0.0)
            scale = 1000.0 if "power" in u else 1.0  # model stores MWh

            if u == "power":
                # 'Power' register row = the import/export plug
                plug = float((result["plants"][pid]
                              .get("final_power_result") or {})
                             .get("plug_mwh") or 0.0) * 1000.0
                checks.append(Check("S2-plug", cpp, "Power (plug)",
                                    plug, bpc_t,
                                    _status(plug, bpc_t, tol_pct, tol_abs),
                                    "power plug"))
            elif re.match(r"steam\(", u):
                # 'STEAM(HP)' etc. = receiver-side booking of a cross-CPP
                # steam transfer — steam routing is intentionally out of scope.
                checks.append(Check("S2-info", cpp, disp.get(u, u), 0.0,
                                    bpc_t, "INFO",
                                    "cross-CPP steam booking (not routed)"))
            elif u in m_total[pid] or u not in m_gen[pid]:
                # demand-register row -> total demand (proc+fix+u4u+ext)
                model_t = m_total[pid].get(u, 0.0) * scale
                st = _status(model_t, bpc_t, tol_pct, tol_abs)
                checks.append(Check("S2-demand", cpp, disp.get(u, u),
                                    model_t, bpc_t, st,
                                    "total proc+fix+u4u"))
                bpc_pf = reg_pf.get((pid, u), 0.0)
                model_pfv = m_pf[pid].get(u, 0.0) * scale
                checks.append(Check("S2-procfix", cpp, disp.get(u, u),
                                    model_pfv, bpc_pf,
                                    _status(model_pfv, bpc_pf,
                                            tol_pct, tol_abs),
                                    "process+fixed only"))
            else:
                # producer-level row (AUXBOIL*_SHP STEAM, STG*_HP STEAM ...)
                # -> the register qty is the generation demand on that producer
                model_g = m_gen[pid].get(u, 0.0)
                checks.append(Check("S2-gen", cpp, disp.get(u, u),
                                    model_g, bpc_t,
                                    _status(model_g, bpc_t, tol_pct, tol_abs),
                                    "producer generation"))
    return checks


def suite_generation(result, bpc_rows, checks, tol_pct, tol_abs):
    m_gen = model_generations(result)
    for pid in CPP_ORDER:
        cpp = CPP_SHORT[pid]
        for prod, mv in sorted(m_gen[pid].items()):
            bpc = _bpc_gen_qty(bpc_rows, cpp, prod, prod)
            if bpc is None:
                checks.append(Check("S3", cpp, prod, mv, 0.0,
                                    "INFO", "no BPC gen row"))
                continue
            checks.append(Check("S3", cpp, prod, mv, bpc,
                                _status(mv, bpc, tol_pct, tol_abs)))
    return checks


def suite_consumption(result, bpc_rows, checks, tol_pct, tol_abs):
    cons = model_consumptions(result)
    for pid in CPP_ORDER:
        cpp = CPP_SHORT[pid]
        for producer, prod_util, material, qty in cons[pid]:
            bpc = _bpc_lookup(bpc_rows, cpp, producer, prod_util, material)
            if bpc is None and _norm_txt(prod_util) == "powerdis":
                # supply-record rows carry the source asset/edge as the
                # material — BPC books them as POWERGEN/<edge> at the node.
                bpc = _bpc_lookup(bpc_rows, cpp, "", "Power_Dis",
                                  "POWERGEN", issuing_plant=material)
                if bpc is None:
                    # PCG assets book as POWERGEN_GTn / POWERGEN_STGn
                    mm = re.search(r"(GT|STG)[^0-9]*(\d+)", material,
                                   re.IGNORECASE)
                    if mm:
                        bpc = _bpc_lookup(
                            bpc_rows, cpp, "", "Power_Dis",
                            "POWERGEN_%s%s" % (mm.group(1).upper(),
                                               mm.group(2)),
                            issuing_plant=_POWER_NODE_BY_CPP[pid][0])
                if bpc is None:
                    bpc = _bpc_lookup(bpc_rows, cpp, "", "Power_Dis",
                                      material,
                                      issuing_plant=_POWER_NODE_BY_CPP[pid][0])
            if bpc is None:
                checks.append(Check("S4", cpp, f"{producer}|{material}",
                                    qty, 0.0, "INFO", "no BPC row"))
                continue
            checks.append(Check("S4", cpp, f"{producer}|{material}",
                                qty, bpc,
                                _status(qty, bpc, tol_pct, tol_abs)))
    return checks


def suite_power(result, bpc_rows, reg_dem, checks, tol_pct, tol_abs):
    mp = model_power(result)
    for pid in CPP_ORDER:
        cpp = CPP_SHORT[pid]
        node = _POWER_NODE_BY_CPP[pid][0]
        # BPC: gen = sum of POWERGEN rows at this CPP's power node;
        # plug = 'Power' row; CTU = 'Power from CTU'; demand = register.
        # PCG nodes label the material POWERGEN_GT1/GT2/STG4 — match the prefix
        bpc_gen = sum(
            r["quantity"] for r in bpc_rows
            if _norm_txt(r["utility"]) == "powerdis"
            and _norm_txt(r["gen_plant"]) == _norm_txt(node)
            and _norm_txt(r["material"]).startswith("powergen"))
        bpc_plug = sum(
            r["quantity"] for r in bpc_rows
            if _norm_txt(r["utility"]) == "powerdis"
            and _norm_txt(r["gen_plant"]) == _norm_txt(node)
            and _norm_txt(r["material"]) == "power")
        bpc_ctu = sum(
            r["quantity"] for r in bpc_rows
            if _norm_txt(r["utility"]) == "powerdis"
            and _norm_txt(r["gen_plant"]) == _norm_txt(node)
            and _norm_txt(r["material"]) == "powerfromctu")
        bpc_dem = reg_dem.get((pid, "powerdis"), 0.0)

        for label, mv, bv in (
            # demand_mwh is net of CTU; add it back for the register compare
            ("Power demand KWH", (mp[pid]["demand"] + mp[pid]["ctu"]) * 1000.0,
             bpc_dem),
            ("Power gen KWH", mp[pid]["gen"] * 1000.0, bpc_gen),
            ("Power plug KWH", mp[pid]["plug"] * 1000.0, bpc_plug),
            ("CTU import KWH", mp[pid]["ctu"] * 1000.0, bpc_ctu),
        ):
            checks.append(Check("S5", cpp, label, mv, bv,
                                _status(mv, bv, tol_pct, tol_abs)))

        # per-asset POWERGEN — DTA/SEZ/C2 key on issuing plant = asset name;
        # PCG nodes carry material POWERGEN_GTn / POWERGEN_STGn instead.
        for a_name, mwh in sorted(mp[pid]["assets"].items()):
            bpc_a = _bpc_lookup(bpc_rows, cpp, "", "Power_Dis", "POWERGEN",
                                issuing_plant=a_name)
            if bpc_a is None:
                mm = re.search(r"(GT|STG)[^0-9]*(\d+)", a_name, re.IGNORECASE)
                if mm:
                    mat = "POWERGEN_%s%s" % (mm.group(1).upper(), mm.group(2))
                    bpc_a = _bpc_lookup(bpc_rows, cpp, "", "Power_Dis",
                                        mat, issuing_plant=node)
            if bpc_a is None:
                checks.append(Check("S5-asset", cpp, a_name,
                                    mwh * 1000.0, 0.0, "INFO",
                                    "no BPC POWERGEN row"))
                continue
            checks.append(Check("S5-asset", cpp, a_name,
                                mwh * 1000.0, bpc_a,
                                _status(mwh * 1000.0, bpc_a,
                                        tol_pct, tol_abs)))
    return checks


def suite_transfers(result, reg_edges, checks, tol_pct, tol_abs):
    m_tr = model_transfers(result)
    keys = set(m_tr) | set(reg_edges)
    for (s, c, u) in sorted(keys):
        mv = m_tr.get((s, c, u), 0.0)
        bv = reg_edges.get((s, c, u), 0.0)
        if "steam" in u or "power" in u:
            # steam transfer intentionally untouched; power is pooled, not
            # routed as a material transfer — informational only.
            checks.append(Check("S6", f"{CPP_SHORT.get(s,s)}->{CPP_SHORT.get(c,c)}",
                                u, mv, bv, "INFO",
                                "out of scope (power pooled / steam not routed)"))
            continue
        checks.append(Check("S6", f"{CPP_SHORT.get(s,s)}->{CPP_SHORT.get(c,c)}",
                            u, mv, bv, _status(mv, bv, tol_pct, tol_abs)))
    return checks


# ---------------------------------------------------------------------------
# Baseline regression
# ---------------------------------------------------------------------------

def _snapshot(result):
    total, pf = model_demands(result)
    gens = model_generations(result)
    cons = model_consumptions(result)
    mp = model_power(result)
    tr = model_transfers(result)
    snap = {
        "demands": {CPP_SHORT[p]: m for p, m in total.items()},
        "proc_fix": {CPP_SHORT[p]: m for p, m in pf.items()},
        "generation": {CPP_SHORT[p]: m for p, m in gens.items()},
        "consumption": {CPP_SHORT[p]: {f"{a}|{b}": q for a, _u, b, q in cons[p]}
                        for p in cons},
        "power": {CPP_SHORT[p]: {k: v for k, v in mp[p].items() if k != "assets"}
                  for p in mp},
        "assets": {CPP_SHORT[p]: mp[p]["assets"] for p in mp},
        "transfers": {f"{CPP_SHORT.get(k[0],k[0])}|{CPP_SHORT.get(k[1],k[1])}|{k[2]}": v
                      for k, v in tr.items()},
    }
    return snap


def suite_regression(result, baseline_path, checks, tol_pct, tol_abs):
    with open(baseline_path, "r", encoding="utf-8") as fh:
        base = json.load(fh)
    snap = _snapshot(result)
    for section in snap:
        keys = set()
        if isinstance(snap[section], dict):
            for cpp, m in snap[section].items():
                if isinstance(m, dict):
                    keys |= {(cpp, k) for k in m}
            for cpp, m in (base.get(section) or {}).items():
                if isinstance(m, dict):
                    keys |= {(cpp, k) for k in m}
        for cpp, k in sorted(keys):
            mv = (snap[section].get(cpp) or {}).get(k, 0.0)
            bv = ((base.get(section) or {}).get(cpp) or {}).get(k, 0.0)
            checks.append(Check("S7", cpp, f"{section}|{k}", mv, bv,
                                _status(mv, bv, tol_pct, tol_abs),
                                "baseline regression"))
    return checks


# ---------------------------------------------------------------------------
# Reporting
# ---------------------------------------------------------------------------

def report(checks, full=False):
    by_suite = defaultdict(list)
    for c in checks:
        by_suite[c.suite].append(c)
    grand = {"PASS": 0, "FAIL": 0, "INFO": 0}
    print()
    print("=" * 96)
    print("  JMD MODEL VALIDATION REPORT")
    print("=" * 96)
    for suite in sorted(by_suite):
        rows = by_suite[suite]
        counts = defaultdict(int)
        for c in rows:
            counts[c.status] += 1
            grand[c.status] += 1
        print()
        print("  %s  —  %d pass / %d fail / %d info" %
              (suite, counts["PASS"], counts["FAIL"], counts["INFO"]))
        print("  " + "-" * 92)
        for c in rows:
            if c.status == "PASS" and not full:
                continue
            diff = c.model - c.bpc
            print("    %-4s  %-22s %-46s model=%-16s bpc=%-16s diff=%+.2f (%+.2f%%) %s"
                  % (c.status, c.cpp, c.item[:46],
                     format(c.model, ",.2f"), format(c.bpc, ",.2f"),
                     diff, _pct(c.model, c.bpc), c.note))
    print()
    print("=" * 96)
    print("  TOTAL: %d PASS  %d FAIL  %d INFO"
          % (grand["PASS"], grand["FAIL"], grand["INFO"]))
    print("=" * 96)
    return grand["FAIL"]


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    ap = argparse.ArgumentParser(description="JMD pooled-model validation")
    ap.add_argument("--month", type=int, default=4)
    ap.add_argument("--year", type=int, default=2026)
    ap.add_argument("--tol-pct", type=float, default=1.0,
                    help="relative tolerance %% (default 1.0)")
    ap.add_argument("--tol-abs", type=float, default=1.0,
                    help="absolute tolerance floor (default 1.0)")
    ap.add_argument("--use-cache", action="store_true",
                    help="load a cached model result instead of running")
    ap.add_argument("--save-cache", action="store_true",
                    help="cache the model result for --use-cache runs")
    ap.add_argument("--baseline", default="",
                    help="JSON baseline file to regression-compare against")
    ap.add_argument("--save-baseline", default="",
                    help="write the model snapshot to this JSON file")
    ap.add_argument("--csv-out", default="",
                    help="optional CSV dump of all check rows")
    ap.add_argument("--full", action="store_true",
                    help="print PASS rows too (default prints FAIL/INFO only)")
    ap.add_argument("--suite", default="all",
                    help="comma list: convergence,demand,generation,"
                         "consumption,power,transfers,regression or 'all'")
    args = ap.parse_args()

    logging.basicConfig(level=logging.WARNING,
                        format="%(asctime)s  %(levelname)-7s  %(message)s")

    cache_file = CACHE_DIR / f"jmd_result_{args.year}_{args.month:02d}.pkl"

    if args.use_cache:
        print("Loading cached model result: %s" % cache_file)
        with open(cache_file, "rb") as fh:
            result = pickle.load(fh)
    else:
        from engine.jmd_orchestrator import run_jmd_month
        print("Running JMD model %02d/%d ..." % (args.month, args.year))
        result = run_jmd_month(args.month, args.year)
        if args.save_cache:
            CACHE_DIR.mkdir(parents=True, exist_ok=True)
            with open(cache_file, "wb") as fh:
                pickle.dump(result, fh)
            print("Cached result -> %s" % cache_file)

    month_label = "%02d/%d" % (args.month, args.year)
    print("Loading BPC reference data for %s ..." % month_label)
    bpc_rows = load_bpc_rows(args.month, BPC_OUTPUT_CSV)
    reg_dem, reg_pf, reg_edges, reg_disp = load_register(args.month)
    print("  Norm/Qty/Cost rows: %d | register demand keys: %d"
          % (len(bpc_rows), len(reg_dem)))

    suites = {s.strip().lower() for s in args.suite.split(",")}
    want = lambda s: "all" in suites or s in suites

    checks = []
    if want("convergence"):
        suite_convergence(result, checks)
    if want("demand"):
        suite_demand(result, reg_dem, reg_pf, reg_disp, checks,
                     args.tol_pct, args.tol_abs)
    if want("generation"):
        suite_generation(result, bpc_rows, checks, args.tol_pct, args.tol_abs)
    if want("consumption"):
        suite_consumption(result, bpc_rows, checks, args.tol_pct, args.tol_abs)
    if want("power"):
        suite_power(result, bpc_rows, reg_dem, checks,
                    args.tol_pct, args.tol_abs)
    if want("transfers"):
        suite_transfers(result, reg_edges, checks, args.tol_pct, args.tol_abs)
    if args.baseline and want("regression"):
        suite_regression(result, args.baseline, checks,
                         args.tol_pct, args.tol_abs)

    if args.save_baseline:
        snap = _snapshot(result)
        with open(args.save_baseline, "w", encoding="utf-8") as fh:
            json.dump(snap, fh, indent=1)
        print("Baseline snapshot -> %s" % args.save_baseline)

    fails = report(checks, full=args.full)

    if args.csv_out:
        with open(args.csv_out, "w", newline="", encoding="utf-8") as fh:
            w = csv.writer(fh)
            w.writerow(["suite", "cpp", "item", "model", "bpc", "diff",
                        "diff_pct", "status", "note"])
            for c in checks:
                w.writerow([c.suite, c.cpp, c.item, c.model, c.bpc,
                            c.model - c.bpc, _pct(c.model, c.bpc),
                            c.status, c.note])
        print("Check rows -> %s" % args.csv_out)

    sys.exit(1 if fails else 0)


if __name__ == "__main__":
    main()
