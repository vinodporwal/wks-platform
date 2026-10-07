"""
JMD single-model Excel report
=============================

Combined JMD workbook for one month, mirroring the per-CPP report layout:

  * "Summary"          — run metadata + per-CPP convergence
  * "Power Pool"       — pooled electrical balance across all 5 CPPs:
                         per-CPP demand / CTU / generation / plug vs BPC,
                         the derived transfer matrix, and the per-asset
                         dispatch table for the whole pool
  * "U4U Summary"      — one row per (CPP, producer): Gen Qty vs BPC Gen Qty
  * "U4U Consumption"  — one row per (CPP, producer, material), same layout
                         as the single-CPP report plus a CPP column and the
                         Issuing Plant column:
                           Utility Plant | Utility | UOM | Gen Qty
                           | BPC Gen Qty | Gen Diff % | Account | Material
                           | Material UOM | Issuing Plant | Norm | Quantity
                           | BPC Quantity | Qty Diff %
                         The electrical balance rows (POWER plug,
                         Power from CTU, POWERGEN per asset) are synthesised
                         from the pool result in BPC Power_Dis format.

BPC comparison columns are sourced from "Norm, Qty, Cost .csv" — the same
golden-master file used by scripts/jmd_power_balance.py.  Per-utility BPC
generation is derived as quantity/norm on each row (every material row of a
utility divides out to the same generation quantity).
"""

import calendar
import csv
import logging
import os
import re
from collections import defaultdict
from datetime import datetime
from pathlib import Path

from engine.excel_report import (
    _HAS_OPENPYXL,
    _header_row,
    _data_row,
    _section_title,
    _auto_width,
    _pct_diff,
    _round_or_blank,
    _TOTAL_FILL,
    _SUB_FILL,
)
from engine.jmd_orchestrator import CPP_ORDER, cpp_name

if _HAS_OPENPYXL:
    from openpyxl import Workbook
    from openpyxl.utils import get_column_letter

logger = logging.getLogger(__name__)

JMD_ROOT = Path(__file__).resolve().parent.parent
BPC_OUTPUT_CSV = JMD_ROOT.parent / "files" / "Norm, Qty, Cost .csv"

# Power_Dis distribution node (name, plant code) per CPP — from the BPC
# Distribution Mapping (identifies pool membership, not transfer direction).
_POWER_NODE_BY_CPP = {
    "A4AF8441-73AD-4F9F-BCF4-6734E8202F7A": ("JMD - Utility/Power Dist",        "36BK"),
    "F6D82E68-C3B6-494F-9905-48F19DC611E3": ("RIL-JW Plant-DTA PCG",            "36KO"),
    "2DFEE33F-4CFD-4887-B9DD-53388AA95271": ("JMD - SEZ Distribution (Power)",  "36BX"),
    "D2C7FBAD-7E00-4642-B3B2-5A768FAC8D45": ("RIL-JW Plant-SEZ PCG",            "36KQ"),
    "BA558F95-8A3F-4769-9C78-FF7B6C639DDF": ("JMD - DTA-C2 Power & UTILITY",    "36H2"),
}


# ---------------------------------------------------------------------------
# BPC "Norm, Qty, Cost .csv" parsing
# ---------------------------------------------------------------------------

def _norm_txt(s) -> str:
    return re.sub(r"[^a-z0-9]", "", str(s or "").lower())


def _num(s) -> float:
    try:
        return float(str(s or "").strip().replace(",", ""))
    except (TypeError, ValueError):
        return 0.0


def _month_index(month: int) -> int:
    """Calendar month -> BPC column index (Apr=0 ... Mar=11)."""
    return (month - 4) % 12


def load_bpc_rows(month: int, path: Path = BPC_OUTPUT_CSV) -> list:
    """Parse one month's rows out of the BPC Norm/Qty/Cost CSV.

    Returns a list of dicts:
        gen_plant, gen_plant_id, utility, account, material,
        issuing_plant, issuing_plant_id, material_uom,
        norm, quantity, cpp  (CPP membership derived from plant names)
    """
    qty_col = 12 + _month_index(month) * 4
    norm_col = 11 + _month_index(month) * 4

    rows = []
    with Path(path).open("r", encoding="utf-16", newline="") as handle:
        for r in csv.reader(handle, delimiter="\t"):
            if len(r) <= qty_col:
                continue
            utility = r[2].strip() if len(r) > 2 else ""
            material = r[6].strip() if len(r) > 6 else ""
            if not utility or not material or utility == "Utility":
                continue
            rows.append({
                "gen_plant": r[0].strip(),
                "gen_plant_id": r[1].strip(),
                "utility": utility,
                "account": r[5].strip() if len(r) > 5 else "",
                "material": material,
                "issuing_plant": r[8].strip() if len(r) > 8 else "",
                "issuing_plant_id": r[9].strip() if len(r) > 9 else "",
                "material_uom": r[10].strip() if len(r) > 10 else "",
                "norm": _num(r[norm_col]),
                "quantity": _num(r[qty_col]),
            })
    for r in rows:
        r["cpp"] = _cpp_of_bpc_row(r)
    return rows


def _cpp_of_bpc_row(r: dict) -> str:
    """Map a BPC row to its owning CPP using the generating/issuing plant
    names.  Order matters: PCG names must be tested before their parent."""
    for name in (r["gen_plant"], r["issuing_plant"]):
        n = _norm_txt(name)
        if not n or n == "noplant":
            continue
        if "sezpcg" in n:
            return "SEZ-PCG-CPP"
        if "dtapcg" in n:
            return "DTA-PCG-CPP"
        if "sez" in n:
            return "SEZ-CPP"
        if "c2" in n:
            return "C2-CPP"
        return "DTA-CPP"
    return "DTA-CPP"


def _bpc_lookup(bpc_rows: list, cpp: str, producer: str, utility: str,
                material: str, issuing_plant: str = "") -> float | None:
    """Best-effort BPC quantity for one model record.

    Asset-level producers (GTs, HRSGs, ...) match their own generating-plant
    rows first — otherwise a shared material like SynGas would sum across all
    assets of the CPP.  Plant-level producers match by utility name within
    the CPP; an exact issuing-plant match wins when present; remaining
    multiple rows are summed.
    """
    nu, nm, np_, ni = _norm_txt(utility), _norm_txt(material), _norm_txt(producer), _norm_txt(issuing_plant)

    # 1. Asset-level producer: rows whose generating plant IS the producer.
    pool = [
        r for r in bpc_rows
        if _norm_txt(r["gen_plant"]) == np_ and _norm_txt(r["material"]) == nm
    ]

    # 2. Utility-level producer: rows for that utility within this CPP.
    if not pool:
        pool = [
            r for r in bpc_rows
            if _norm_txt(r["utility"]) == nu and _norm_txt(r["material"]) == nm
            and r["cpp"] == cpp
        ]

    if not pool:
        return None

    if ni:
        exact = [r for r in pool if _norm_txt(r["issuing_plant"]) == ni]
        if exact:
            pool = exact
    return sum(r["quantity"] for r in pool)


def _bpc_gen_qty(bpc_rows: list, cpp: str, producer: str, utility: str) -> float | None:
    """BPC generation quantity for a producer — quantity/norm on its rows.

    For asset producers (power/steam assets) match the generating-plant name;
    otherwise match the utility name within the CPP.  All rows of one
    generating entity share the same generation, so the median is stable.
    Power_Dis is a distribution node, not a producer: its "generation" is the
    total quantity distributed, i.e. the sum of its material rows.
    """
    np_, nu = _norm_txt(producer), _norm_txt(utility)

    if nu == "powerdis":
        total = sum(
            r["quantity"] for r in bpc_rows
            if _norm_txt(r["utility"]) == nu and r["cpp"] == cpp
        )
        return total or None

    vals = [
        r["quantity"] / r["norm"] for r in bpc_rows
        if r["norm"] and _norm_txt(r["gen_plant"]) == np_
    ]
    if not vals:
        vals = [
            r["quantity"] / r["norm"] for r in bpc_rows
            if r["norm"] and _norm_txt(r["utility"]) == nu and r["cpp"] == cpp
        ]
    if not vals:
        return None
    vals.sort()
    return vals[len(vals) // 2]


# ---------------------------------------------------------------------------
# Sheet 1 — Summary
# ---------------------------------------------------------------------------

def _write_jmd_summary(ws, result: dict):
    ws.title = "Summary"
    _header_row(ws, 1, 1, ["Field", "Value"])
    rows = [
        ("Month",        result.get("month", "")),
        ("Year",         result.get("year", "")),
        ("FY",           result.get("fy", "")),
        ("Converged",    result.get("converged", "")),
        ("Iterations",   result.get("iterations_used", "")),
        ("Exec time (s)", result.get("execution_time_seconds", "")),
        ("Generated",    datetime.now().strftime("%Y-%m-%d %H:%M:%S")),
    ]
    r = 2
    for k, v in rows:
        _data_row(ws, r, [k, str(v)])
        r += 1

    r += 1
    _section_title(ws, r, "Per-CPP convergence", 5)
    r += 1
    _header_row(ws, 1, r, ["CPP", "Converged", "Iterations", "Power Gen MWh", "Steam Gen MT"])
    r += 1
    for pid in CPP_ORDER:
        p = (result.get("plants") or {}).get(pid)
        if not p:
            _data_row(ws, r, [cpp_name(pid), "NOT RUN", "", "", ""])
            r += 1
            continue
        pr = p.get("final_power_result") or {}
        sr = p.get("final_steam_result") or {}
        _data_row(ws, r, [
            cpp_name(pid),
            bool(p.get("converged")),
            p.get("iterations_used", 0),
            round(pr.get("total_generation_mwh", 0.0), 2),
            round(sr.get("total_generation_mt", 0.0), 2),
        ])
        r += 1
    _auto_width(ws)


# ---------------------------------------------------------------------------
# Sheet 2 — JMD power pool balance
# ---------------------------------------------------------------------------

def _write_power_pool(ws, result: dict, bpc_baseline: dict = None):
    ws.title = "Power Pool"
    pool = result.get("power_pool") or {}
    month_label = _month_label(result.get("month", 0))

    row = 1
    _section_title(ws, row, f"JMD POWER POOL — {month_label} {result.get('year', '')}", 9)
    row += 1
    for label, val in [
        ("Pool demand MWh",       pool.get("total_demand_mwh", 0.0)),
        ("Pool generation MWh",   pool.get("total_generation_mwh", 0.0)),
        ("Pool surplus MWh",      pool.get("surplus_mwh", 0.0)),
        ("Pool deficit MWh",      pool.get("deficit_mwh", 0.0)),
        ("Plug closure MWh",      pool.get("closure_residual_mwh", 0.0)),
    ]:
        _data_row(ws, row, [label, round(val, 2)])
        row += 1

    row += 1
    _section_title(ws, row, "Per-CPP electrical position", 9)
    row += 1
    headers = ["CPP", "Demand MWh", "External Import MWh", "Generation MWh",
               "Plug MWh (+import / -export)"]
    if bpc_baseline:
        headers = ["CPP", "Demand MWh", "External Import MWh",
                   "Generation MWh", "BPC Gen MWh", "Gen Diff MWh",
                   "Gen Diff %", "Plug MWh (+import / -export)",
                   "BPC Plug MWh", "Plug Diff MWh", "Plug Diff %"]
    _header_row(ws, 1, row, headers)
    row += 1

    bpc_month = (bpc_baseline or {}).get(month_label, {})
    tot = defaultdict(float)
    for pid in CPP_ORDER:
        p = (result.get("plants") or {}).get(pid)
        if not p:
            continue
        pr = p.get("final_power_result") or {}
        ext = (p.get("import_power") or {}).get("total_mwh", 0.0)
        plug = pr.get("plug_mwh", 0.0)
        gen = pr.get("total_generation_mwh", 0.0)
        # demand_mwh is dispatch demand (net of CTU import); the BPC register
        # counts CTU-supplied consumption as demand, so show the true total.
        dem = pr.get("demand_mwh", 0.0) + ext
        bpc = bpc_month.get(cpp_name(pid), {}) if bpc_baseline else {}
        bpc_gen = bpc.get("generation_mwh", 0.0)
        bpc_plug = bpc.get("plug_mwh", 0.0)
        vals = [
            cpp_name(pid),
            round(dem, 2),
            round(ext, 2),
        ]
        if bpc_baseline:
            vals += [
                round(gen, 2),
                round(bpc_gen, 2),
                round(gen - bpc_gen, 2),
                round((gen - bpc_gen) / bpc_gen * 100, 2) if bpc_gen else None,
                round(plug, 2),
                round(bpc_plug, 2),
                round(plug - bpc_plug, 2),
                round((plug - bpc_plug) / abs(bpc_plug) * 100, 2)
                if bpc_plug else None,
            ]
        else:
            vals += [round(gen, 2), round(plug, 2)]
        _data_row(ws, row, vals)
        row += 1
        tot["dem"] += dem
        tot["ext"] += ext
        tot["gen"] += gen
        tot["plug"] += plug
        tot["bpc_gen"] += bpc_gen
        tot["bpc_plug"] += bpc_plug

    tot_vals = ["TOTAL", round(tot["dem"], 2), round(tot["ext"], 2)]
    if bpc_baseline:
        tot_vals += [
            round(tot["gen"], 2),
            round(tot["bpc_gen"], 2),
            round(tot["gen"] - tot["bpc_gen"], 2),
            round((tot["gen"] - tot["bpc_gen"]) / tot["bpc_gen"] * 100, 2)
            if tot["bpc_gen"] else None,
            round(tot["plug"], 2),
            round(tot["bpc_plug"], 2),
            round(tot["plug"] - tot["bpc_plug"], 2),
            round((tot["plug"] - tot["bpc_plug"]) / abs(tot["bpc_plug"]) * 100, 2)
            if abs(tot["bpc_plug"]) > 0.01 else None,
        ]
    else:
        tot_vals += [round(tot["gen"], 2), round(tot["plug"], 2)]
    _data_row(ws, row, tot_vals)
    row += 1

    row += 1
    _section_title(ws, row, "Derived inter-CPP transfers", 3)
    row += 1
    _header_row(ws, 1, row, ["Sender", "Receiver", "MWh"])
    row += 1
    for t in pool.get("transfers", []):
        _data_row(ws, row, [
            cpp_name(t["sender_plant_id"]),
            cpp_name(t["receiver_plant_id"]),
            round(t["quantity_mwh"], 2),
        ])
        row += 1

    row += 1
    _section_title(ws, row, "Pooled asset dispatch", 16)
    row += 1
    _header_row(ws, 1, row, [
        "CPP", "Asset", "Type", "Pri", "Man", "Hours",
        "Min MW", "Max MW", "Disp MW", "Disp MWh", "Load %",
        "Aux MWh", "Aux Norm", "Free Steam MT", "Heat Rate",
        "STG SHP Cons MT", "STG MP Ext MT",
    ])
    row += 1
    for pid in CPP_ORDER:
        p = (result.get("plants") or {}).get(pid)
        if not p:
            continue
        for a in (p.get("final_power_result") or {}).get("assets", []):
            _data_row(ws, row, [
                cpp_name(pid),
                a.get("asset_name", ""),
                a.get("asset_type", ""),
                a.get("priority", ""),
                "Y" if a.get("mandatory") == 1 else "-",
                a.get("op_hours", 0.0),
                round(a.get("min_mw", 0.0), 2),
                round(a.get("max_mw", 0.0), 2),
                round(a.get("dispatched_mw", 0.0), 4),
                round(a.get("dispatched_mwh", 0.0), 2),
                round(a.get("load_percent", 0.0), 1),
                round(a.get("aux_power", 0.0), 2),
                round(a.get("aux_power_norm", 0.0), 6),
                round(a.get("free_steam_mt", 0.0), 2),
                round(a.get("heat_rate", 0.0), 1),
                round(a.get("stg_shp_consumption_mt", a.get("stg_hp_consumption_mt", 0.0)) or 0.0, 2),
                round(a.get("stg_mp_extraction_mt", 0.0) or 0.0, 2),
            ])
            row += 1
    _auto_width(ws)


# ---------------------------------------------------------------------------
# Sheet 3 — Inter-CPP utility transfers
# ---------------------------------------------------------------------------

def _write_utility_transfers(ws, result: dict, bpc_rows: list):
    ws.title = "Utility Transfers"
    transfers = result.get("utility_transfers") or []
    if not transfers:
        ws.cell(row=1, column=1).value = "No inter-CPP utility transfers"
        return

    # BPC cross-CPP edges for comparison — same sender mapping as the model.
    # Two granularities: per producer row (sender, consumer, utility, material)
    # and per material edge (sender, consumer, material).
    from engine.jmd_orchestrator import _issuing_plant_to_cpp
    bpc_edges = {}
    bpc_row_edges = {}
    for r in bpc_rows:
        sender_pid = _issuing_plant_to_cpp(r["issuing_plant"])
        if not sender_pid:
            continue
        sender = cpp_name(sender_pid)
        consumer = r["cpp"]
        if consumer == sender:
            continue
        n = _norm_txt(r["material"])
        if "steam" in n or "power" in n:
            continue
        bpc_edges[(sender, consumer, r["material"])] = \
            bpc_edges.get((sender, consumer, r["material"]), 0.0) + r["quantity"]
        bpc_row_edges[(sender, consumer, r["utility"], r["material"])] = \
            bpc_row_edges.get((sender, consumer, r["utility"], r["material"]),
                              0.0) + r["quantity"]

    headers = ["Sender", "Consumer", "Consumer Producer", "Material",
               "Leg", "Qty", "BPC Qty", "Qty Diff %", "Issuing Plant"]
    _header_row(ws, 1, 1, headers)

    row = 2
    for t in sorted(
        transfers,
        key=lambda t: (cpp_name(t["sender_plant_id"]),
                       cpp_name(t["consumer_plant_id"]),
                       t["material"], t.get("leg", "")),
    ):
        bq = None
        if t.get("leg") == "u4u":
            bq = bpc_row_edges.get(
                (cpp_name(t["sender_plant_id"]),
                 cpp_name(t["consumer_plant_id"]),
                 t.get("consumer_producer", ""), t["material"]))
        _data_row(ws, row, [
            cpp_name(t["sender_plant_id"]),
            cpp_name(t["consumer_plant_id"]),
            t.get("consumer_producer", ""),
            t["material"],
            t.get("leg", ""),
            round(t["quantity"], 2),
            round(bq, 2) if bq is not None else "",
            _round_or_blank(_pct_diff(t["quantity"], bq)),
            t.get("issuing_plant", ""),
        ])
        row += 1

    # Aggregated per edge (sender, consumer, material) vs BPC
    row += 1
    _section_title(ws, row, "Edge totals vs BPC", 9)
    row += 1
    _header_row(ws, 1, row, ["Sender", "Consumer", "Material",
                             "U4U leg", "Proc+Fix leg", "Total",
                             "BPC Qty", "Diff %", ""])
    row += 1
    agg = {}
    for t in transfers:
        key = (t["sender_plant_id"], t["consumer_plant_id"], t["material"])
        e = agg.setdefault(key, {"u4u": 0.0, "process_fixed": 0.0})
        e[t.get("leg", "u4u")] += t["quantity"]
    for (s, c, m), e in sorted(agg.items(),
                               key=lambda kv: (cpp_name(kv[0][0]),
                                               cpp_name(kv[0][1]), kv[0][2])):
        bq = bpc_edges.get((cpp_name(s), cpp_name(c), m))
        total = e["u4u"] + e["process_fixed"]
        _data_row(ws, row, [
            cpp_name(s), cpp_name(c), m,
            round(e["u4u"], 2), round(e["process_fixed"], 2), round(total, 2),
            round(bq, 2) if bq is not None else "",
            _round_or_blank(_pct_diff(e["u4u"], bq)),
            "",
        ], fill=_TOTAL_FILL)
        row += 1

    ws.freeze_panes = "A2"
    _auto_width(ws, min_w=8, max_w=34)


# ---------------------------------------------------------------------------
# Sheet 3b — Inter-site Steam Transfers
# ---------------------------------------------------------------------------

def _steam_edge_meta(month=None, year=None):
    """Edge lookup + short plain-language labels for reporting.

    Bands come from dbo.CPP_IntersiteSteamTransfer for the run month when
    available (the config's static min/max are only fallbacks/defaults).
    """
    from engine.steam_transfer_router import STEAM_EDGES, _band_for
    meta = {}
    for e in STEAM_EDGES:
        min_tph, max_tph = e.get("min_tph"), e.get("max_tph")
        if month and year:
            try:
                lo, hi = _band_for(e, month, year)
                if lo:
                    min_tph = lo
                if hi:
                    max_tph = hi
            except Exception:
                pass
        meta[e["id"]] = {
            "config_sender": e["sender"],
            "config_receiver": e["receiver"],
            "audit_only": e.get("audit_only", False),
            "min_tph": min_tph,
            "max_tph": max_tph,
        }
    return meta


_STEAM_KIND_LABEL = {
    "commit":        "Committed minimum flow",
    "pull":          "Receiver pull",
    "push":          "Surplus push",
    "push_pull":     "Surplus push",
    "pull_pull":     "Demand-matched flow",
    "residual_plug": "Balance trim (no pipeline)",
}


def _steam_grade(material: str) -> str:
    """'SHP Steam_Dis' -> 'SHP'."""
    return material.split()[0].upper() if material else ""


def _write_steam_transfers(ws, result: dict):
    ws.title = "Steam Transfers"
    transfers = result.get("steam_transfers") or []
    mode = (result.get("steam_transfer_mode") or "").upper()
    if not transfers:
        ws.cell(row=1, column=1).value = \
            f"No inter-site steam transfers (mode={mode or 'OFF'})"
        return

    month, year = result.get("month", 0), result.get("year", 0)
    try:
        hours = calendar.monthrange(year, month)[1] * 24.0
    except Exception:
        hours = 720.0
    meta = _steam_edge_meta(month, year)

    _section_title(ws, 1,
                   f"Inter-site steam transfers - {month}/{year} "
                   f"({hours:,.0f} h, mode: {mode or 'OFF'})", 11)
    _header_row(ws, 1, 2, [
        "Edge", "From", "To", "Grade", "Qty (MT)", "BPC booked (MT)",
        "Diff %", "Avg TPH", "Band (TPH)", "Basis", "Remark",
    ])

    imports, exports = {}, {}
    row = 3
    for t in sorted(transfers, key=lambda t: t.get("edge") or "Z"):
        edge_id = t.get("edge") or "-"
        em = meta.get(edge_id, {})
        audit = (t.get("basis") == "audit_only"
                 or em.get("audit_only", False))
        sender, receiver = t.get("sender"), t.get("receiver")
        if t.get("basis") == "solved":
            sender = em.get("config_sender", sender)
            receiver = em.get("config_receiver", receiver)
        snd = cpp_name(sender) if sender else "-"
        rcv = cpp_name(receiver) if receiver else "-"
        qty = float(t.get("quantity") or 0.0)
        booked = t.get("booked_mt")
        if booked is None:
            booked = qty
        band = "-"
        if em.get("min_tph") and em.get("max_tph"):
            band = ("%g" % em["min_tph"]
                    if em["min_tph"] == em["max_tph"]
                    else "%g-%g" % (em["min_tph"], em["max_tph"]))
        if booked:
            diff_pct = round((qty - booked) / booked * 100.0, 2)
        else:
            diff_pct = "-"
        if audit:
            basis, remark = "Audit", "Audit only - not a demand transfer"
        else:
            basis = ("BPC booked" if t.get("basis") == "bpc_pinned"
                     else (mode or "-").title())
            remark = _STEAM_KIND_LABEL.get(t.get("kind", ""),
                                           t.get("kind", ""))
        _data_row(ws, row, [
            edge_id, snd, rcv, _steam_grade(t["material"]),
            round(qty, 2), round(booked, 2), diff_pct,
            round(qty / hours, 1) if qty else "-",
            band, basis, remark,
        ])
        if not audit:
            if sender:
                exports[snd] = exports.get(snd, 0.0) + qty
            if receiver:
                imports[rcv] = imports.get(rcv, 0.0) + qty
        row += 1

    row += 1
    _section_title(ws, row, "Net inter-site position (MT)", 9)
    row += 1
    _header_row(ws, 1, row, [
        "CPP", "Imports (MT)", "Exports (MT)", "Net (MT)", "Position",
    ])
    row += 1
    for pid in CPP_ORDER:
        name = cpp_name(pid)
        imp, exp = imports.get(name, 0.0), exports.get(name, 0.0)
        if not imp and not exp:
            continue
        net = exp - imp
        position = ("net exporter" if net > 0
                    else "net importer" if net < 0 else "balanced")
        _data_row(ws, row, [
            name, round(imp, 2), round(exp, 2), round(net, 2), position,
        ])
        row += 1
    ws.cell(row=row, column=1).value = (
        "One-sided commitments (SEZ-PCG -> SEZ LLP desal leg) obligate only "
        "the sender - no receiver credit is booked at SEZ.")

    ws.freeze_panes = "A3"
    _auto_width(ws, min_w=8, max_w=40)


# ---------------------------------------------------------------------------
# Sheet 4 — U4U Summary (per CPP + producer)
# ---------------------------------------------------------------------------

def _write_u4u_summary(ws, result: dict, bpc_rows: list):
    ws.title = "U4U Summary"
    headers = [
        "CPP", "Utility Plant", "Utility", "UOM", "Gen Qty",
        "BPC Gen Qty", "Gen Diff %", "# Materials", "Materials Consumed",
    ]
    _header_row(ws, 1, 1, headers)
    row = 2
    for pid in CPP_ORDER:
        p = (result.get("plants") or {}).get(pid)
        if not p:
            continue
        name = cpp_name(pid)
        records = _plant_records(pid, p)

        by_producer: dict = {}
        for rec in records:
            key = (rec["producer"], rec.get("producer_utility", ""))
            s = by_producer.setdefault(key, {
                "uom": rec.get("producer_uom", ""),
                "generation": rec.get("generation", 0.0),
                "materials": [],
            })
            s["materials"].append(rec.get("material", ""))

        for (producer, utility), s in sorted(by_producer.items()):
            bpc_gen = _bpc_gen_qty(bpc_rows, name, producer, utility)
            fill = _SUB_FILL if row % 2 == 0 else None
            _data_row(ws, row, [
                name, producer, utility, s["uom"],
                round(s["generation"], 2),
                round(bpc_gen, 2) if bpc_gen is not None else "",
                _round_or_blank(_pct_diff(s["generation"], bpc_gen)),
                len(s["materials"]), ", ".join(s["materials"]),
            ], fill=fill)
            row += 1

    ws.freeze_panes = "A2"
    ws.auto_filter.ref = f"A1:{get_column_letter(len(headers))}{row - 1}"
    _auto_width(ws, min_w=8, max_w=32)


# ---------------------------------------------------------------------------
# Sheet 4 — U4U Consumption (per CPP + producer + material)
# ---------------------------------------------------------------------------

def _norms_lookup(consumption_norms: dict, rec: dict) -> dict:
    """Best-effort identity lookup for a detail record — issuing plant, ids."""
    producer = str(rec.get("producer", ""))
    utility = str(rec.get("producer_utility", ""))
    material = str(rec.get("material", ""))

    for key in (utility, producer):
        info = consumption_norms.get(key)
        if not info:
            continue
        consumptions = info.get("consumptions", [])
        for c in consumptions:
            if c.get("material") == material and str(c.get("source_plant", "")).strip() == producer:
                return {"info": info, "c": c}
        for c in consumptions:
            if c.get("material") == material:
                return {"info": info, "c": c}
        return {"info": info, "c": None}
    return {"info": None, "c": None}


def _power_dis_records(plant_id: str, plant: dict) -> list:
    """Synthesised BPC-format electrical rows for one CPP's Power_Dis node.

    Quantities in KWH, BPC sign convention (Power: +import / -export).
    """
    pr = plant.get("final_power_result") or {}
    node_name, node_id = _POWER_NODE_BY_CPP.get(plant_id, ("", ""))
    power_utility = plant.get("power_ods_material") or "Power_Dis"

    total_kwh = (
        float(pr.get("total_generation_mwh", 0.0))
        + float((plant.get("import_power") or {}).get("total_mwh", 0.0))
        + float(pr.get("plug_mwh", 0.0))
    ) * 1000.0

    recs = []

    plug_kwh = float(pr.get("plug_mwh", 0.0)) * 1000.0
    recs.append({
        "producer": node_name, "producer_utility": power_utility,
        "producer_uom": "KWH", "generation": total_kwh,
        "account": "Utilities", "material": "Power", "material_uom": "KWH",
        "issuing_plant": node_name, "issuing_plant_id": node_id,
        "norm": (plug_kwh / total_kwh) if total_kwh else 0.0,
        "quantity": plug_kwh,
    })

    for src in (plant.get("import_power") or {}).get("per_source", []):
        qty_kwh = float(src.get("mwh", 0.0)) * 1000.0
        if qty_kwh == 0.0:
            continue
        src_name = str(src.get("source_name", "")).strip()
        material = src_name if src_name.lower().startswith("power") else f"Power from {src_name}"
        recs.append({
            "producer": node_name, "producer_utility": power_utility,
            "producer_uom": "KWH", "generation": total_kwh,
            "account": "Utilities", "material": material, "material_uom": "KWH",
            "issuing_plant": node_name, "issuing_plant_id": node_id,
            "norm": (qty_kwh / total_kwh) if total_kwh else 0.0,
            "quantity": qty_kwh,
        })

    for a in pr.get("assets", []):
        qty_kwh = float(a.get("dispatched_mwh", 0.0)) * 1000.0
        if qty_kwh <= 0.0:
            continue
        recs.append({
            "producer": node_name, "producer_utility": power_utility,
            "producer_uom": "KWH", "generation": total_kwh,
            "account": "Utilities", "material": "POWERGEN", "material_uom": "KWH",
            "issuing_plant": a.get("asset_name", ""),
            "issuing_plant_id": a.get("plant_code", ""),
            "norm": (qty_kwh / total_kwh) if total_kwh else 0.0,
            "quantity": qty_kwh,
        })

    return recs


def _plant_records(plant_id: str, plant: dict) -> list:
    """All detail rows for one CPP: synthesised Power_Dis rows + every other
    producer's records (the Power_Dis node's own records are replaced)."""
    power_utility = str(plant.get("power_ods_material") or "Power_Dis")
    records = list(_power_dis_records(plant_id, plant))
    source = plant.get("final_dynamic_table") or plant.get("final_detail_records") or []
    records += [
        rec for rec in source
        if str(rec.get("producer_utility", "")).strip() != power_utility
    ]
    return records


def _write_u4u_consumption(ws, result: dict, bpc_rows: list, cpp_filter: str = None):
    """Write the U4U consumption sheet for one CPP or for all CPPs.

    If ``cpp_filter`` is set, the sheet is named for that CPP and contains
    only its rows; otherwise it contains all CPPs in display order.
    """
    if cpp_filter:
        ws.title = f"U4U Consumption - {cpp_name(cpp_filter)}"
    else:
        ws.title = "U4U Consumption"
    headers = [
        "CPP", "Utility Plant", "Utility", "UOM", "Gen Qty",
        "BPC Gen Qty", "Gen Diff %",
        "Account", "Material", "Material UOM", "Issuing Plant",
        "Norm", "Quantity", "BPC Quantity", "Qty Diff %",
    ]
    _header_row(ws, 1, 1, headers)

    row = 2
    pids = [cpp_filter] if cpp_filter else CPP_ORDER
    for pid in pids:
        p = (result.get("plants") or {}).get(pid)
        if not p:
            continue
        name = cpp_name(pid)
        consumption_norms = p.get("consumption_norms") or {}
        ods_bpc_qty = p.get("final_bpc_quantities") or {}
        ods_bpc_gen = p.get("final_bpc_gen_quantities") or {}

        for rec in _plant_records(pid, p):
            lk = _norms_lookup(consumption_norms, rec)
            c = lk["c"]
            issuing = rec.get("issuing_plant") or ((c or {}).get("issuing_plant") or "")

            bq = _bpc_lookup(
                bpc_rows, name,
                rec.get("producer", ""), rec.get("producer_utility", ""),
                rec.get("material", ""), issuing,
            )
            if bq is None:
                bq = rec.get("bpc_quantity")
            if bq is None:
                bq = (ods_bpc_qty.get(rec.get("producer", "")) or {}).get(rec.get("material", ""))

            bpc_gen = _bpc_gen_qty(
                bpc_rows, name,
                rec.get("producer", ""), rec.get("producer_utility", ""),
            )
            if bpc_gen is None:
                bpc_gen = ods_bpc_gen.get(rec.get("producer", ""))

            _data_row(ws, row, [
                name,
                rec.get("producer", ""),
                rec.get("producer_utility", ""),
                rec.get("producer_uom", ""),
                round(rec.get("generation", 0.0), 2),
                round(bpc_gen, 2) if bpc_gen is not None else "",
                _round_or_blank(_pct_diff(rec.get("generation", 0.0), bpc_gen)),
                rec.get("account", ""),
                rec.get("material", ""),
                rec.get("material_uom", ""),
                issuing,
                round(rec.get("norm", 0.0), 6),
                round(rec.get("quantity", 0.0), 2),
                round(bq, 2) if bq is not None else "",
                _round_or_blank(_pct_diff(rec.get("quantity", 0.0), bq)),
            ])
            row += 1

    ws.freeze_panes = "A2"
    ws.auto_filter.ref = f"A1:{get_column_letter(len(headers))}{row - 1}"
    _auto_width(ws, min_w=8, max_w=32)


def _month_label(month: int) -> str:
    return {
        1: "Jan", 2: "Feb", 3: "Mar", 4: "Apr", 5: "May", 6: "Jun",
        7: "Jul", 8: "Aug", 9: "Sep", 10: "Oct", 11: "Nov", 12: "Dec",
    }.get(month, "")


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

def write_jmd_month_report(result: dict, output_folder: str,
                           bpc_baseline: dict = None) -> str:
    """Write the combined JMD Excel report for one month.

    Args:
        result:        dict from engine.jmd_orchestrator.run_jmd_month()
        output_folder: destination directory (created if missing)
        bpc_baseline:  optional {month: {cpp_name: {plug_mwh, generation_mwh}}}
                       for the Power Pool comparison columns

    Returns:
        Absolute path to the generated .xlsx file.
    """
    if not _HAS_OPENPYXL:
        raise ImportError("openpyxl is required: pip install openpyxl")

    month = result.get("month", 0)
    year = result.get("year", 0)
    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = f"JMD_ALL_CPP_{year}_{month:02d}_{ts}.xlsx"

    os.makedirs(output_folder, exist_ok=True)
    filepath = os.path.join(output_folder, filename)

    try:
        bpc_rows = load_bpc_rows(month)
    except Exception as e:
        logger.warning("  [EXCEL] Could not load BPC Norm/Qty/Cost file: %s", e)
        bpc_rows = []

    wb = Workbook()
    wb.remove(wb.active)

    _write_jmd_summary(wb.create_sheet(), result)
    _write_power_pool(wb.create_sheet(), result, bpc_baseline)
    _write_utility_transfers(wb.create_sheet(), result, bpc_rows)
    _write_steam_transfers(wb.create_sheet(), result)
    _write_u4u_summary(wb.create_sheet(), result, bpc_rows)
    # Per-CPP U4U consumption sheets first, then the combined view.
    for pid in CPP_ORDER:
        if pid in (result.get("plants") or {}):
            _write_u4u_consumption(wb.create_sheet(), result, bpc_rows,
                                   cpp_filter=pid)
    _write_u4u_consumption(wb.create_sheet(), result, bpc_rows)

    wb.save(filepath)
    logger.info("  [EXCEL] JMD month report saved: %s", filepath)
    return filepath
