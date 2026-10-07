"""
JMD inter-site steam transfer routing
=====================================

Implements the validated inter-CPP steam network for the four interconnected
CPPs (DTA, DTA-PCG, SEZ, SEZ-PCG).  C2 is deliberately absent — it is an
islanded steam system (C2/IIR flows are internal) and must never receive or
send inter-site steam.

Confirmed edge set (see JMD_INTERSITE_STEAM_DESIGN.md for sources):

    E1  DTA-PCG -> DTA      SHP   push/pull, bidirectional, 50-540 TPH
    E2  SEZ     -> DTA      HP    committed minimum, 40-200 TPH
    E3  SEZ     -> SEZ-PCG  HP    committed minimum, 55-250 TPH
    E4  DTA-PCG -> SEZ-PCG  SHP   push/pull, bidirectional, ~50 TPH min (soft)
    E5  SEZ-PCG -> DTA-PCG  IP    receiver pull (SEZ-PCG is the only IP source)
    E6  SEZ-PCG -> SEZ      LLP   sender surplus push (audit only — SEZ has
                                  no LLP material in its books)

How the bookings actually work (validated against all 12 months of register
data, Apr 2026 - Mar 2027):

  * The receiver books a positive STEAM(<grade>) plug = imported supply.
  * The physical demand leg is booked at different nodes per edge:
      - E1: receiver's node (DTA fixed SHP row 57,600)
      - E2: sender's node  (SEZ fixed HP row 28,800 — sender already carries
                          the obligation inside its own demand)
      - E3: receiver's node (SEZ-PCG fixed HP row 41,057)
      - E4: transit node   (the leg is booked at DTA's distribution node —
                          32,369 in Apr, matching SEZ-PCG's plug every month)
      - E5: sender's node  (SEZ-PCG fixed IP row 25,982)
  * PCG plants' fixed "generation" rows already contain the plug import
    (e.g. SEZ-PCG HP Steam_Dis gen = 41,057 = the E3 import), so no
    receiver-side credit is applied for those materials.

Two resolution modes:
  * 'bpc'   — transfers pinned to the booked STEAM(<grade>) plug quantities;
              reproduces the BPC accounting exactly (reference mode).
  * 'solve' — quantities resolved each iteration from per-CPP residuals:
              commit edges always run at their minimum, pull edges cover
              receiver deficits, push edges evacuate sender surplus, and the
              bidirectional SHP links pick direction from the net positions.

Read-only: this module never writes to the database.
"""

import calendar
import logging
import re as _re

logger = logging.getLogger(__name__)

# Plant UUIDs (duplicated from jmd_orchestrator.CPP_ORDER — kept local so the
# router stays import-light and cannot accidentally drag the orchestrator).
DTA     = "A4AF8441-73AD-4F9F-BCF4-6734E8202F7A"
DTA_PCG = "F6D82E68-C3B6-494F-9905-48F19DC611E3"
SEZ     = "2DFEE33F-4CFD-4887-B9DD-53388AA95271"
SEZ_PCG = "D2C7FBAD-7E00-4642-B3B2-5A768FAC8D45"
C2      = "BA558F95-8A3F-4769-9C78-FF7B6C639DDF"  # islanded — never routed

# ---------------------------------------------------------------------------
# Edge table
# ---------------------------------------------------------------------------
# kind:
#   'commit'    — minimum-flow obligation; always runs at >= min_tph*hours
#   'pull'      — driven by receiver deficit at that grade
#   'push'      — driven by sender surplus (must-run evacuation)
#   'push_pull' — surplus-driven bidirectional link; direction resolved from
#                 each end's net position (evacuation semantics: pushes the
#                 residual surplus after higher-priority commitments)
#   'pull_pull' — deficit-matched bidirectional link; flow = receiver pull
#                 floored at min_tph*hours, capped by sender surplus
#
# receiver_credit:  subtract flow from receiver dispatch demand (only when
#                   the receiver's own generation does NOT already embody
#                   the plug import)
# sender_leg_booked: True when the export leg already sits inside the
#                   sender's booked demand (E2, E5, E6) — then no extra
#                   sender obligation is needed in BPC mode
# transit_plant:    node where the physical demand leg is booked instead of
#                   the sender (E4 only — the leg sits on DTA's distribution
#                   node and must be lifted off DTA's demand)
# audit_only:       report the edge but apply no demand adjustments (E6)
# ---------------------------------------------------------------------------

STEAM_EDGES = [
    {
        "id": "E1", "sender": DTA_PCG, "receiver": DTA,
        "material": "SHP Steam_Dis", "grade": "SHP",
        "kind": "push_pull", "bidirectional": True,
        "min_tph": 50.0, "max_tph": 320.0,
        "receiver_credit": True, "sender_leg_booked": False,
        "transit_plant": None, "audit_only": False,
        "source": "SHP Operating Philosophy; Constraints doc 50-540 TPH",
    },
    {
        "id": "E2", "sender": SEZ, "receiver": DTA,
        "material": "HP Steam_Dis", "grade": "HP",
        "kind": "commit", "bidirectional": False,
        "min_tph": 40.0, "max_tph": 200.0,
        "receiver_credit": True, "sender_leg_booked": True,
        "transit_plant": None, "audit_only": False,
        "source": "Constraints doc: SEZ->DTA HP 40-200 TPH; 40 TPH booked all 12 months",
    },
    {
        "id": "E3", "sender": SEZ, "receiver": SEZ_PCG,
        "material": "HP Steam_Dis", "grade": "HP",
        "kind": "commit", "bidirectional": False,
        "min_tph": 55.0, "max_tph": 250.0,
        "receiver_credit": False, "sender_leg_booked": False,
        "gen_embodies_plug": True,
        "transit_plant": None, "audit_only": False,
        "source": "Constraints doc: SEZ->SEZ-PCG HP 55-250 TPH",
    },
    {
        "id": "E4", "sender": DTA_PCG, "receiver": SEZ_PCG,
        "material": "SHP Steam_Dis", "grade": "SHP",
        "kind": "pull_pull", "bidirectional": True,
        "min_tph": 40.0, "max_tph": 320.0,
        "receiver_credit": False, "sender_leg_booked": False,
        "gen_embodies_plug": True,
        "transit_plant": DTA, "audit_only": False,
        "source": "SHP Operating Philosophy (~50 TPH min, soft); leg booked at DTA node",
    },
    {
        "id": "E5", "sender": SEZ_PCG, "receiver": DTA_PCG,
        "material": "IP Steam_Dis", "grade": "IP",
        "kind": "push", "bidirectional": False,
        "min_tph": 20.0, "max_tph": 320.0,
        "receiver_credit": True, "sender_leg_booked": True,
        # DTA-PCG's booked IP generation IS the plug import (25,982 Apr) —
        # in solve mode the plug-embedded gen covers the demand, so only
        # the delta beyond the booked plug needs an extra credit.
        "gen_embodies_plug": True,
        "transit_plant": None, "audit_only": False,
        "source": "booked plug — SEZ-PCG is the only IP producer",
    },
    {
        "id": "E6", "sender": SEZ_PCG, "receiver": SEZ,
        "material": "LLP Steam Dis", "grade": "LLP",
        "kind": "commit", "bidirectional": False,
        "min_tph": 0.0, "max_tph": None,
        "receiver_credit": False, "sender_leg_booked": False,
        # SEZ has no LLP header — the desal flow is a one-sided sender
        # obligation: it must sit inside SEZ-PCG's LLP demand so the
        # dispatch cascade runs the LLP->LP->MP PRDS chain (BPC closes
        # LLP at byproduct+PRDS = desal+export and LP at ~305 MT).
        "one_sided": True,
        "transit_plant": None, "audit_only": False,
        "source": "register: RIL-JW Plant-SEZ PCG -> JMD - SEZ Utility Plant",
    },
]

# (receiver_plant_id, grade) -> edge id, for BPC plug -> edge resolution.
# (DTA-PCG, 'SHP') is deliberately absent: its small positive plug (4-52 TPH)
# is an internal balance trim, not a pipeline flow.
_PLUG_EDGE = {
    (DTA,     "HP"):  "E2",
    (DTA,     "SHP"): "E1",
    (DTA_PCG, "IP"):  "E5",
    (SEZ_PCG, "HP"):  "E3",
    (SEZ_PCG, "SHP"): "E4",
}

_EDGES_BY_ID = {e["id"]: e for e in STEAM_EDGES}

# Branching-cascade plants: their SHP dispatch demand skips the U4U increment
# (SHP generation is fixed pass-through), so the cascade '{grade}_net' for
# SHP excludes utility-for-utility demand.  Positions for SHP at these
# plants must be taken against total_demands (which does carry the U4U).
_BRANCHING_PLANTS = {DTA_PCG, SEZ_PCG}

# Materials that hold a gasifier pass-through supply inside the plant's
# initial demands (negative quantity = supply credit at the PCG plants).
_PASSTHROUGH_HINT = "GASIFIER"


def _edge(edge_id):
    return _EDGES_BY_ID.get(edge_id)


def _month_hours(month: int, year: int) -> float:
    return float(calendar.monthrange(year, month)[1] * 24)


def _add(ext: dict, pid: str, material: str, qty: float) -> None:
    ext.setdefault(pid, {})
    ext[pid][material] = ext[pid].get(material, 0.0) + qty


def _plug_qty(loop, material: str) -> float:
    """Positive STEAM(<grade>) plug booked against a steam material."""
    grade = material.split()[0].upper()
    try:
        q = loop._lookup_bpc_qty(material, f"STEAM({grade})")
    except Exception:
        return 0.0
    return float(q or 0.0)


def _booked_export_qty(loop, material: str) -> float:
    """Booked export magnitude for an audit-only edge: the sender's
    negative initial demand for the material (tolerant name match —
    'LLP Steam Dis' vs 'LLP Steam_Dis')."""
    norm = "".join(ch for ch in material.lower() if ch.isalnum())
    qty = 0.0
    for name, v in (getattr(loop, "_initial_utility_demands", {}) or {}).items():
        if "".join(ch for ch in name.lower() if ch.isalnum()) == norm:
            qty = max(qty, -float(v or 0.0))
    return qty


def _passthrough_supply(loop, material: str) -> float:
    """Gasifier pass-through supply for a grade (PCG plants): the negative
    'Gasifier_ SHP Steam' initial demand is supply, not consumption."""
    grade = material.split()[0].upper()
    total = 0.0
    # Word-boundary match: 'HP' must not match inside 'SHP', 'LP' inside 'LLP'.
    grade_re = _re.compile(r"\b" + _re.escape(grade) + r"\b")
    for name, qty in (getattr(loop, "_initial_utility_demands", None) or {}).items():
        if _PASSTHROUGH_HINT in name.upper() and grade_re.search(name.upper()):
            total += max(0.0, -float(qty or 0.0))
    return total


# ---------------------------------------------------------------------------
# Monthwise flow bands — dbo.CPP_IntersiteSteamTransfer
# ---------------------------------------------------------------------------
# Rows are keyed by (SenderPlant, ReceiverPlant, NormParameter grade) with
# Min_<Mon>/Max_<Mon> TPH columns per AOP year.  They override the static
# min_tph/max_tph on the edge definitions; missing rows/cells keep the edge
# default.  A 0/NULL max means uncapped.

_EDGE_KEY = {
    (DTA_PCG, DTA,     "SHP"): "E1",
    (DTA_PCG, SEZ_PCG, "SHP"): "E4",
    (SEZ,     DTA,     "HP"):  "E2",
    (SEZ,     SEZ_PCG, "HP"):  "E3",
    (SEZ_PCG, DTA_PCG, "IP"):  "E5",
    (SEZ_PCG, SEZ,     "LLP"): "E6",
}
_MONTH_COL = {1: "Jan", 2: "Feb", 3: "Mar", 4: "Apr", 5: "May", 6: "Jun",
              7: "Jul", 8: "Aug", 9: "Sep", 10: "Oct", 11: "Nov",
              12: "Dec"}
_band_cache = {}


def _fy_string(month: int, year: int) -> str:
    fy = year if month >= 4 else year - 1
    return f"{fy}-{str(fy + 1)[-2:]}"


def _db_bands(month: int, year: int) -> dict:
    """{edge_id: (min_tph, max_tph|None)} for the run month's AOP year."""
    key = (month, year)
    if key in _band_cache:
        return _band_cache[key]
    bands = {}
    try:
        from database.connection import get_connection
        conn = get_connection()
        try:
            cur = conn.cursor()
            cur.execute(
                f"""
                SELECT t.Min_{_MONTH_COL[month]}, t.Max_{_MONTH_COL[month]},
                       t.SenderPlant_FK_Id, t.ReceiverPlant_FK_Id, np.Name
                FROM dbo.CPP_IntersiteSteamTransfer t
                JOIN dbo.NormParameters np ON np.Id = t.NormParameter_FK_Id
                WHERE t.AOP_Year = ?
                """, _fy_string(month, year))
            for lo, hi, s, r, gname in cur.fetchall():
                grade = str(gname).split()[0].upper()
                eid = _EDGE_KEY.get((str(s).upper(), str(r).upper(), grade))
                if eid is None:
                    continue
                bands[eid] = (float(lo or 0.0),
                              float(hi) if hi and float(hi) > 0 else None)
        finally:
            conn.close()
    except Exception as exc:
        logger.warning("  [STEAM-XFER] band config read failed (%s) — "
                       "using edge defaults", exc)
    _band_cache[key] = bands
    return bands


def _band_for(edge: dict, month: int, year: int):
    """Effective (min_tph, max_tph|inf) for an edge this month."""
    lo, hi = _db_bands(month, year).get(
        edge["id"], (edge.get("min_tph", 0.0), edge.get("max_tph")))
    return lo, hi if hi else float("inf")


# ---------------------------------------------------------------------------
# Booked-leg model — legs are transfer VOLUMES booked at distribution
# nodes, not consumption.  Leg node per edge:
#   transit_plant (E4@DTA) > sender (sender_leg_booked: E2, E5) > receiver
# Leg amount = the receiver's booked STEAM(<grade>) plug (monthwise).
# Legs are fused into shared fixed rows (DTA SHP row = E1+E4), so they are
# always stripped by AMOUNT at the leg node, never by row.
# ---------------------------------------------------------------------------

def _leg_node(edge: dict):
    if edge.get("transit_plant"):
        return edge["transit_plant"]
    if edge.get("sender_leg_booked"):
        return edge["sender"]
    return edge["receiver"]


def _leg_qty(loops: dict, edge: dict) -> float:
    r_loop = loops.get(edge["receiver"])
    if r_loop is None:
        return 0.0
    return _plug_qty(r_loop, edge["material"])


def _legs_at(loops: dict, pid: str, material: str) -> float:
    """Total booked leg volume sitting inside pid's demand for material."""
    total = 0.0
    for e in STEAM_EDGES:
        if e.get("audit_only") or e["material"] != material:
            continue
        if _leg_node(e) == pid:
            total += _leg_qty(loops, e)
    return total


def _is_plug_mat(material: str) -> bool:
    return material.strip().upper().startswith("STEAM(")


def _header_supply(loop, total_demands_pid: dict, material: str) -> float:
    """Header inflow: sum of feed-material ledger outputs feeding the
    header producer's consumption rows (the feed's full ledger output is
    the inflow; the norm is only the booked ratio).  STEAM(<grade>) plug
    rows = booked imports — excluded.  Feed materials absent from the
    ledger (qty ~0) fall back to their booked inflow qty."""
    info = (getattr(loop, "consumption_norms", {}) or {}).get(material) or {}
    total = 0.0
    try:
        bpc_gen = loop._bpc_gen_quantities or {}
    except Exception:
        bpc_gen = {}
    for c in info.get("consumptions", []):
        mat = c.get("material", "")
        if "STEAM" not in str(mat).upper() or _is_plug_mat(str(mat)):
            continue
        ledger = float((total_demands_pid or {}).get(mat, 0.0))
        if abs(ledger) > 1.0:
            total += ledger
        else:
            # Feed absent from the ledger — prefer the BPC-booked
            # generation of the feed material (e.g. SEZ-PCG 'LP Steam
            # PRDS' = 305 in BPC; the norms row's configured quantity
            # 26,940 is a capacity-like figure, not the booked flow).
            fb = bpc_gen.get(mat)
            total += float(fb) if fb is not None \
                else float(c.get("quantity") or 0.0)
    return total


def _prds_feed_gap(loop, total_demands_pid: dict, u4u_demands_pid: dict,
                   header: str) -> float:
    """PRDS feed demand missing from the header's ledger.

    A PRDS producer's input draw (e.g. 'IP STEAM PRDS' consuming
    'HP Steam_Dis' @0.89) is demand on the SOURCE header.  At the PCG
    plants that draw never lands in the u4u ledger; at DTA/SEZ it already
    does.  Returns the PRDS-feed total only when the u4u ledger is closer
    to the non-PRDS consumer sum (i.e. the feeds are missing).
    """
    norms = getattr(loop, "consumption_norms", {}) or {}
    u = float((u4u_demands_pid or {}).get(header, 0.0))
    non_prds = prds = 0.0
    for prod, info in norms.items():
        for c in info.get("consumptions", []):
            if c.get("material") != header:
                continue
            if "PRDS" in str(prod).upper():
                prod_td = float(
                    (total_demands_pid or {}).get(prod, 0.0))
                if prod_td > 0:
                    q = float(c.get("norm") or 0.0) * prod_td
                else:
                    # Producer's ledger demand is clamped at 0 (its output
                    # header is net-byproduct — e.g. SEZ-PCG LLP Steam
                    # PRDS) so norm x demand never fires.  The consumption
                    # row's booked quantity is already the feed input MT.
                    q = float(c.get("quantity") or 0.0)
                prds += q
            else:
                non_prds += float(c.get("norm") or 0.0) \
                    * float((total_demands_pid or {}).get(prod, 0.0))
    if not prds:
        return 0.0
    missing = abs(u - non_prds) <= abs(u - non_prds - prds)
    return prds if missing else 0.0


# ---------------------------------------------------------------------------
# Per-material supply / position for the solver
# ---------------------------------------------------------------------------

def _local_supply(loop, steam_result: dict, material: str) -> float:
    """Local (non-imported) supply destined to a steam material's header.

    Used for the solver's pull/surplus positions.  Excludes the plug import
    itself: for PCG plants the booked BPC 'generation' of imported grades
    (SHP/HP/IP) already contains the plug, so the plug quantity is stripped.

    Top grade:  dispatched generation + gasifier pass-through.
    DTA grades: bounded PRDS output (lp/mp/hp) + pass-through.
    Others:     non-dispatchable booked generation minus the plug import.
    """
    dd = (steam_result or {}).get("demand_detail") or {}
    grade_l = material.split()[0].lower()
    top = str(dd.get("_top_grade") or "").lower()

    supply = _passthrough_supply(loop, material)

    if grade_l and grade_l == top:
        supply += float((steam_result or {}).get("total_generation_mt") or 0.0)
        return supply

    prds_key = f"{grade_l}_prds_out"
    if prds_key in dd:
        return supply + float(dd.get(prds_key) or 0.0)

    # Non-dispatchable material (PCG plants): booked BPC generation net of
    # the plug import = local production.
    try:
        bpc_gen = float(loop._lookup_bpc_gen_qty(material) or 0.0)
    except Exception:
        bpc_gen = 0.0
    plug = max(0.0, _plug_qty(loop, material))
    return supply + max(0.0, bpc_gen - plug)


def _position(loops, steam_results, total_demands, ext_applied,
              pid: str, material: str, u4u_demands: dict = None) -> float:
    """Signed intrinsic net position for a material: + surplus / - deficit.

    Ledger reconstruction (validated Apr + Oct 2026):

      demand = total_demands[material]
               − booked transfer legs at this node (legs are transfer
                 VOLUMES booked at dist nodes, not consumption — strip by
                 configured amount, they are fused into shared fixed rows)
               + PRDS feed re-parenting: a PRDS's draw on the source header
                 never reaches the u4u ledger at PCG plants (it does at
                 DTA/SEZ) — added only where missing
      supply = Σ feed-material ledger inflows to the header producer
               (STEAM(<grade>) plug rows excluded = booked imports)
               + gasifier/process pass-through credits
               + |applied import credits| — restore generation displaced
                 at receivers by the previous iteration's credits

    ext_steam only enters dispatch_demands, never total_demands, so this
    basis is a stable map across iterations.
    """
    if pid not in loops:
        return 0.0
    loop = loops[pid]
    td = (total_demands.get(pid) or {})
    applied = float((ext_applied.get(pid) or {}).get(material, 0.0))

    demand = float(td.get(material, 0.0)) \
        - _legs_at(loops, pid, material) \
        + _prds_feed_gap(
            loop, td, (u4u_demands or {}).get(pid), material)

    if material in (getattr(loop, "consumption_norms", {}) or {}):
        # Header-source decomposition already carries pass-through steam
        # transitively (gasifier -> superheater/HRSG feed -> header), so
        # _passthrough_supply must NOT be added again on this basis.
        supply = _header_supply(loop, td, material)
    else:
        supply = _local_supply(loop, steam_results.get(pid), material) \
            + _passthrough_supply(loop, material)
    supply += max(0.0, -applied)
    return supply - demand


def _edge_for_receiver_material(pid: str, material: str):
    """Edge whose receiver is `pid` and material matches (any grade)."""
    grade = material.split()[0].upper()
    eid = _PLUG_EDGE.get((pid.upper(), grade))
    return _EDGES_BY_ID.get(eid)


# ---------------------------------------------------------------------------
# BPC-pinned resolution
# ---------------------------------------------------------------------------

def _one_sided_commit_qty(loop, edge: dict, month: int, year: int) -> float:
    """Committed one-sided export obligation in MT (E6 desal leg).

    The DB band min pins the booked commitment (min=max=booked for every
    configured month).  When the band cell is empty, fall back to the
    ledger-implied exportable residual:
        |negative initial demand| (byproduct position)
        + booked PRDS generation − in-booked (non-PRDS) consumers.
    """
    lo, _ = _band_for(edge, month, year)
    if lo and lo > 0:
        return lo * _month_hours(month, year)
    if loop is None:
        return 0.0
    m = edge["material"]
    exportable = _booked_export_qty(loop, m)
    init = getattr(loop, "_initial_utility_demands", {}) or {}
    norms = getattr(loop, "consumption_norms", {}) or {}
    own = 0.0
    for prod, info in norms.items():
        if "PRDS" in str(prod).upper():
            continue
        for c in info.get("consumptions", []) or []:
            if str(c.get("material")) == m:
                own += float(c.get("norm") or 0.0) \
                    * float(init.get(prod, 0.0))
    try:
        prds_gen = float(
            (loop._bpc_gen_quantities or {}).get(m) or 0.0)
    except Exception:
        prds_gen = 0.0
    return max(0.0, exportable + prds_gen - own)


def _resolve_bpc(loops: dict, prepared: list,
                 month: int = None, year: int = None):
    """Pin every transfer to the booked STEAM(<grade>) plug quantity.

    Receiver plugs are read from each loop's BPC quantity table.  For each
    plug the matching edge determines: receiver credit (if the receiver's
    generation doesn't already embody the import), sender obligation (if the
    sender's booked demand lacks the export leg), and transit-leg removal
    (E4's leg booked at DTA's node).  Plugs with no edge (DTA-PCG's small
    SHP trim) stay receiver-side credits, as today.
    """
    ext = {pid: {} for pid in prepared}
    rows = []

    for pid in prepared:
        loop = loops[pid]
        for material in getattr(loop, "_initial_steam_mt", {}):
            plug = _plug_qty(loop, material)
            if plug <= 0:
                continue
            grade = material.split()[0].upper()
            eid = _PLUG_EDGE.get((pid.upper(), grade))
            edge = _edge(eid)

            if edge is None:
                # No physical edge — internal balance trim (e.g. DTA-PCG's
                # small STEAM(SHP) plug).  Receiver-side import credit only.
                _add(ext, pid, material, -plug)
                rows.append({
                    "edge": "-", "kind": "residual_plug",
                    "sender": None, "receiver": pid, "material": material,
                    "quantity": plug, "booked_mt": round(plug, 2),
                    "basis": "bpc_pinned",
                    "note": "STEAM(%s) plug — internal balance trim, no edge" % grade,
                })
                continue

            if edge["receiver_credit"]:
                _add(ext, pid, material, -plug)
            if not edge["sender_leg_booked"]:
                _add(ext, edge["sender"], material, plug)
            if edge.get("transit_plant"):
                _add(ext, edge["transit_plant"], material, -plug)
            rows.append({
                "edge": edge["id"], "kind": edge["kind"],
                "sender": edge["sender"], "receiver": pid,
                "material": material, "quantity": plug,
                "booked_mt": round(plug, 2),
                "basis": "bpc_pinned", "note": edge["source"],
            })

    # One-sided sender commitments (E6 desal leg): the export is a real
    # obligation on the sender's LLP header — booking it as demand is
    # what makes the LLP->LP->MP PRDS chain dispatch like BPC (LLP PRDS
    # ~39.9k -> LP feed ~39.5k -> LP PRDS ~305).  No receiver credit:
    # SEZ has no LLP header to book the import against.
    for e in STEAM_EDGES:
        if not e.get("one_sided"):
            continue
        s_loop = loops.get(e["sender"])
        qty = _one_sided_commit_qty(s_loop, e, month, year)
        if qty <= 0:
            continue
        _add(ext, e["sender"], e["material"], qty)
        rows.append({
            "edge": e["id"], "kind": e["kind"],
            "sender": e["sender"], "receiver": e["receiver"],
            "material": e["material"], "quantity": round(qty, 2),
            "booked_mt": round(qty, 2),
            "basis": "bpc_pinned",
            "note": e["source"] + " | one-sided sender commitment",
        })

    return ext, rows


# ---------------------------------------------------------------------------
# Solver-driven resolution (residual-based, band-constrained)
# ---------------------------------------------------------------------------

def _resolve_solve(loops: dict, prepared: list, steam_results: dict,
                   total_demands: dict, ext_applied: dict,
                   month: int, year: int, u4u_demands: dict = None):
    """Resolve transfers from this iteration's intrinsic net positions.

    Solve mode = BPC baseline + delta: booked legs stay inside
    total_demands, so every ext adjustment is (solved − booked):

      obligation node (sender, or transit node for E4's forward flow):
          ext += qty − leg booked there
      receiver:
          ext += embedded_plug − qty   (negative = import credit;
          positive = the booked plug over-states the solved import and
          the receiver must generate the difference)

    commit edges always run at >= min; pull edges cover receiver deficit;
    push edges evacuate sender surplus; bidirectional links pick direction
    from the two ends' positions.  Below-min flows send what is available
    and warn (site rule: generate the gap first, else log + send actual).
    Band min/max come from dbo.CPP_IntersiteSteamTransfer per month.
    """
    hours = _month_hours(month, year)
    ext = {pid: {} for pid in prepared}
    rows = []

    def pos(pid, material):
        return _position(loops, steam_results, total_demands, ext_applied,
                         pid, material, u4u_demands=u4u_demands)

    def pull(pid, material):
        return max(0.0, -pos(pid, material))

    def surplus(pid, material):
        return max(0.0, pos(pid, material))

    # Surplus pool accounting: obligations already committed on the same
    # (node, material) reduce the surplus available to later edges — two
    # links drawing on the same pool must not double-count it.
    committed = {}

    def avail_surplus(pid, material):
        return max(0.0, pos(pid, material)
                   - committed.get((pid, material), 0.0))

    def commit_to(pid, material, qty):
        committed[(pid, material)] = \
            committed.get((pid, material), 0.0) + qty

    _priority = {"commit": 0, "pull": 0, "pull_pull": 0,
                 "push": 1, "push_pull": 1}
    ordered = sorted(STEAM_EDGES,
                     key=lambda x: _priority.get(x["kind"], 0))

    for e in ordered:
        s, r, m = e["sender"], e["receiver"], e["material"]
        if s not in loops or r not in loops:
            continue
        min_tph, max_tph = _band_for(e, month, year)
        min_mt = min_tph * hours
        cap = max_tph * hours if max_tph != float("inf") else float("inf")
        kind = e["kind"]
        qty = 0.0
        direction = (s, r)  # resolved sender, receiver
        warn = []

        if e.get("one_sided"):
            # One-sided sender commitment (E6): the desal export is a
            # real obligation on the sender's header — booking it as
            # demand is what makes the LLP->LP->MP PRDS chain dispatch
            # (BPC closes the residual by running LLP Steam PRDS ~39.9k
            # which draws ~39.5k LP).  No receiver credit: SEZ has no
            # LLP header to book the import against.
            implied = pos(s, m)
            qty = _one_sided_commit_qty(
                loops.get(s), e, month, year) or max(0.0, implied)
            _add(ext, s, m, qty)
            if implied > 0 and abs(qty - implied) > 0.05 * implied:
                warn.append(
                    f"{e['id']} committed {qty:,.0f} vs implied "
                    f"exportable {implied:,.0f}")
            rows.append({
                "edge": e["id"], "kind": kind, "sender": s, "receiver": r,
                "material": m, "quantity": round(qty, 2),
                "booked_mt": round(qty, 2),
                "basis": "solved",
                "note": f"{e['source']} | one-sided sender commitment",
                "warnings": warn,
            })
            continue

        # Node whose surplus pool supplies the flow: the transit node for
        # E4's forward direction (DTA hub supplies SEZ-PCG — DTA-PCG's own
        # surplus is reserved for E1), else the resolved sender.
        pool_node = e.get("transit_plant") or s

        if kind == "commit":
            # Minimum-flow obligation: always runs at >= min, expands to
            # cover receiver pull when the deficit is larger.
            qty = max(min_mt, pull(r, m))
            qty = min(qty, cap)
            if pull(r, m) > cap:
                warn.append(
                    f"{e['id']} receiver pull {pull(r, m)/hours:.1f} TPH "
                    f"> max {max_tph:g} — capped; residual stays at "
                    f"receiver")
            if avail_surplus(s, m) < qty - 0.1 * hours:
                warn.append(
                    f"{e['id']} sender surplus "
                    f"{avail_surplus(s, m)/hours:.1f} TPH < committed "
                    f"{qty/hours:.1f} TPH — sender must dispatch "
                    f"headroom (aux/HRSG) or transfer is partial")
        elif kind == "pull":
            qty = min(pull(r, m), cap)
            if 0 < qty < min_mt:
                warn.append(
                    f"{e['id']} pull {qty/hours:.1f} TPH < min "
                    f"{min_tph:g} — sending available (site rule)")
            if pull(r, m) > cap:
                warn.append(
                    f"{e['id']} receiver pull {pull(r, m)/hours:.1f} TPH "
                    f"> max {max_tph:g} — capped")
        elif kind == "push":
            qty = min(avail_surplus(s, m), cap)
            if 0 < qty < min_mt:
                warn.append(
                    f"{e['id']} sender surplus {qty/hours:.1f} TPH < min "
                    f"{min_tph:g} — sending available (site rule)")
            if avail_surplus(s, m) > cap:
                warn.append(
                    f"{e['id']} sender surplus exceeds max {max_tph:g} TPH "
                    f"— residual stays at sender")
        elif kind == "push_pull":
            # Surplus-evacuation link (E1): the surplus end pushes its
            # must-run excess (after higher-priority commitments on the
            # same pool); the receiver absorbs it by shedding its own
            # dispatchable generation.  Direction from positions.
            s_pos, r_pos = pos(s, m), pos(r, m)
            if s_pos > 0:
                qty = avail_surplus(s, m)
            elif r_pos > 0 and e["bidirectional"]:
                direction = (r, s)
                pool_node = r
                qty = avail_surplus(r, m)
            if qty > cap:
                warn.append(
                    f"{e['id']} surplus {qty/hours:.1f} TPH > max "
                    f"{max_tph:g} — capped; residual stays at sender")
            qty = min(qty, cap)
            if 0 < qty < min_mt:
                warn.append(
                    f"{e['id']} flow {qty/hours:.1f} TPH < min "
                    f"{min_tph:g} — sending available (site rule)")
        elif kind == "pull_pull":
            # Deficit-matched link (E4): receiver pull floored at min when
            # the link is active, capped by the supplying pool's surplus
            # and the link capacity.  Below-min sender supply sends what
            # is available + warns (site rule).
            s_pos, r_pos = pos(pool_node, m), pos(r, m)
            if r_pos < 0 and s_pos > 0:
                want = max(-r_pos, min_mt)
                qty = min(want, avail_surplus(pool_node, m), cap)
                if qty < min_mt:
                    warn.append(
                        f"{e['id']} supplying pool surplus {qty/hours:.1f} "
                        f"TPH < min {min_tph:g} — sending available "
                        f"(site rule)")
            elif s_pos < 0 and r_pos > 0 and e["bidirectional"]:
                direction = (r, s)
                pool_node = r
                want = max(-s_pos, min_mt)
                qty = min(want, avail_surplus(r, m), cap)
                if qty < min_mt:
                    warn.append(
                        f"{e['id']} reversed-flow surplus {qty/hours:.1f} "
                        f"TPH < min {min_tph:g} — sending available")
            elif r_pos < 0 and s_pos <= 0:
                warn.append(
                    f"{e['id']} receiver deficit {-r_pos/hours:.1f} TPH "
                    f"but supplying pool has no surplus — no transfer; "
                    f"deficit stays at receiver")
            if pull(r, m) > cap:
                warn.append(
                    f"{e['id']} receiver pull {pull(r, m)/hours:.1f} TPH "
                    f"> max {max_tph:g} — capped")
        qty = max(0.0, min(qty, cap))
        if qty <= 0:
            continue
        commit_to(pool_node, m, qty)

        snd, rcv = direction
        fwd = direction == (e["sender"], e["receiver"])
        # Obligation node carries the export volume as a demand adjustment:
        # solved qty minus the leg already booked there (delta vs booked).
        ob_node = pool_node if fwd else snd
        leg_ob = _leg_qty(loops, e) if _leg_node(e) == ob_node else 0.0
        _add(ext, ob_node, m, qty - leg_ob)
        # Reversed flow on a transit edge: strip the stale booked leg at
        # the transit node (it booked the forward direction's volume).
        if e.get("transit_plant") and not fwd:
            _add(ext, e["transit_plant"], m, -_leg_qty(loops, e))
        # Receiver credit delta vs the plug embedded in its booked gen.
        rcv_loop = loops[rcv]
        if m in getattr(rcv_loop, "_all_producers", set()) or \
           m in (getattr(rcv_loop, "_initial_steam_mt", {}) or {}):
            embedded = _plug_qty(rcv_loop, m) \
                if e.get("gen_embodies_plug") else 0.0
            credit = embedded - qty
            if abs(credit) > 1.0:
                _add(ext, rcv, m, credit)
                if credit > 0.1 * hours:
                    warn.append(
                        f"{e['id']} solved {qty/hours:.1f} TPH < booked "
                        f"plug {embedded/hours:.1f} — receiver must "
                        f"generate {credit/hours:.1f} TPH more")
        booked = _leg_qty(loops, e)
        rows.append({
            "edge": e["id"], "kind": kind, "sender": snd, "receiver": rcv,
            "material": m, "quantity": round(qty, 2),
            "booked_mt": round(booked, 2),
            "basis": "solved",
            "note": f"{e['source']} | pos(s)={pos(snd, m):,.0f} "
                    f"pos(r)={pos(rcv, m):,.0f}",
            "warnings": warn,
        })

    # Damping: blend this iteration's adjustments with the applied ones.
    # The receiver-credit feedback (credit -> dispatch -> demand -> pull
    # -> credit) has near-unit gain and oscillates period-2 without it
    # (observed: E3 swinging +/-0.6 TPH and never converging).
    damped = {pid: {} for pid in prepared}
    for pid in prepared:
        keys = set(ext.get(pid, {})) | set(
            (ext_applied or {}).get(pid, {}))
        for m in keys:
            v = 0.5 * (float(ext.get(pid, {}).get(m, 0.0))
                       + float((ext_applied or {}).get(pid, {}).get(m, 0.0)))
            if abs(v) > 1e-6:
                damped[pid][m] = v
    return damped, rows


# ---------------------------------------------------------------------------
# Per-CPP steam balance report (power-pool style)
# ---------------------------------------------------------------------------

def steam_balance_report(loops: dict, prepared: list, steam_results: dict,
                         total_demands: dict, ext_applied: dict,
                         transfers: list,
                         u4u_demands: dict = None) -> list:
    """Per-CPP per-grade steam balance, the steam analog of the power pool.

    For every steam distribution header ('<grade> Steam_Dis'/'Steam Dis')
    at every plant — transfer-touched or not:
      demand_mt      intrinsic gross header consumption (total_demands minus
                     transit-leg volumes booked at a transit node — E4's leg
                     inside DTA's books is not DTA's own demand); negative
                     values are net-producing headers (byproduct/letdown
                     export positions)
      supply_mt      local supply on the header basis: top-grade dispatch +
                     gasifier/process pass-through + PRDS letdown ('net'
                     residual where no prds_out row exists) + STG extraction
                     + process byproducts.  Booked-plug-embodied grades use
                     the booked-production basis instead.
      bpc_gen_mt     BPC-booked generation for the material
      need_mt        residual need = demand - supply  (+ = import needed);
                     transit-leg demand booked at a transit node (E4's leg
                     inside DTA's books) is excluded — it is not the plant's
                     own demand
      booked_net_mt  net booked imports - exports on the APPLIED basis:
                     receiver legs always count; sender legs already embedded
                     in the sender's booked demand (sender_leg_booked: E2,
                     E5) are excluded; transit legs excluded at the transit
                     node.  need_mt - booked_net_mt equals the post-transfer
                     ledger deficit
      model_net_mt   model-implied net on the same applied basis
      residual_mt    need_mt - booked_net_mt  (+ = residual deficit after
                     the booked flows; matches the dispatch balance)
    """
    def _b_ends(t):
        if t.get("basis") == "solved":
            e = _EDGES_BY_ID.get(t.get("edge")) or {}
            return e.get("sender"), e.get("receiver")
        return t.get("sender"), t.get("receiver")

    def _m_ends(t):
        if t.get("basis") == "solved":
            return t.get("sender"), t.get("receiver")
        return (t.get("model_sender", t.get("sender")),
                t.get("model_receiver", t.get("receiver")))

    def _b_qty(t):
        return t.get("booked_mt") if t.get("basis") == "solved" \
            else t.get("quantity")

    def _m_qty(t):
        return t.get("quantity") if t.get("basis") == "solved" \
            else t.get("model_qty")

    real = [t for t in transfers if t.get("basis") != "audit_only"]
    mats = {pid: set() for pid in prepared}
    for t in real:
        meta = _EDGES_BY_ID.get(t.get("edge")) or {}
        ends = [_b_ends(t)[0]]
        if not meta.get("one_sided"):
            ends.append(_b_ends(t)[1])
        for pid in ends:
            if pid in mats:
                mats[pid].add(t["material"])

    bnet = {pid: {} for pid in prepared}
    mnet = {pid: {} for pid in prepared}
    bimp = {pid: {} for pid in prepared}   # booked receiver legs only
    for t in real:
        m = t["material"]
        meta = _EDGES_BY_ID.get(t.get("edge")) or {}
        booked_in_sender_demand = bool(meta.get("sender_leg_booked"))
        one_sided = bool(meta.get("one_sided"))
        bq = _b_qty(t) or 0.0
        bs, br = _b_ends(t)
        if br in bnet and not one_sided:
            bnet[br][m] = bnet[br].get(m, 0.0) + bq
            bimp[br][m] = bimp[br].get(m, 0.0) + bq
        if bs in bnet and not booked_in_sender_demand:
            bnet[bs][m] = bnet[bs].get(m, 0.0) - bq
        mq = _m_qty(t)
        if mq is None:
            continue
        ms, mr = _m_ends(t)
        # Solved transit flows (E4 forward) obligate the transit hub —
        # attribute the model export there, not at edge.sender.
        if t.get("basis") == "solved" and meta.get("transit_plant") \
                and ms == meta["sender"]:
            ms = meta["transit_plant"]
        if mr in mnet and not one_sided:
            mnet[mr][m] = mnet[mr].get(m, 0.0) + mq
        if ms in mnet and not booked_in_sender_demand:
            mnet[ms][m] = mnet[ms].get(m, 0.0) - mq

    # Steam distribution headers look like '<grade> Steam_Dis' /
    # '<grade> Steam Dis' — distinguishable from asset materials
    # ('HRSG1_SHP STEAM', 'MP Steam PRDS SHP', 'STG5_HP STEAM').
    hdr_re = _re.compile(
        r"^\s*(ehp|shp|ihp|hp|mp|ip|lp|llp)\s*steam[\s_]*dis\b", _re.I)
    grade_order = {"shp": 0, "hp": 1, "ihp": 2, "mp": 3, "ip": 4,
                   "lp": 5, "llp": 6, "ehp": 7}

    rows = []
    for pid in prepared:
        loop = loops.get(pid)
        if loop is None:
            continue
        sr = steam_results.get(pid) or {}
        dd = sr.get("demand_detail") or {}
        top_l = str(dd.get("_top_grade") or "").lower()
        total_gen = float(sr.get("total_generation_mt") or 0.0)

        mats_all = set(mats[pid])
        for name in (total_demands.get(pid) or {}):
            if hdr_re.match(str(name)):
                mats_all.add(name)

        for m in sorted(mats_all,
                        key=lambda x: grade_order.get(
                            str(x).split()[0].lower(), 99)):
            grade_l = str(m).split()[0].lower()
            edge = _edge_for_receiver_material(pid, m)
            gen_embodies = bool(edge and edge.get("gen_embodies_plug"))
            pt = _passthrough_supply(loop, m)

            # Local supply base: the header-source decomposition (sum of
            # feed-material ledger inflows, plugs excluded) — includes aux
            # boilers, HRSGs and STG extraction, which total_generation_mt
            # alone misses (it produced a false ~180k HP deficit at SEZ).
            # Feed rows already carry STG/PRDS contributions, so no extras.
            real_base = False
            if m in (getattr(loop, "consumption_norms", {}) or {}):
                base = _header_supply(
                    loop, (total_demands.get(pid) or {}), m)
            elif grade_l == top_l:
                base = total_gen + pt
                real_base = True
            elif gen_embodies:
                base = _local_supply(loop, sr, m)
            elif f"{grade_l}_prds_out" in dd:
                base = float(dd.get(f"{grade_l}_prds_out") or 0.0) + pt
            elif f"{grade_l}_net" in dd:
                base = float(dd.get(f"{grade_l}_net") or 0.0) + pt
            else:
                base = _local_supply(loop, sr, m)

            # STG extraction and process byproducts feed the lower-grade
            # headers directly — only added to real cascade bases that do
            # not already carry them.
            extraction = float(dd.get(f"stg_{grade_l}_extraction_mt") or 0.0)
            byprod = -min(0.0, float(dd.get(f"{grade_l}_byproduct") or 0.0))
            extras = (extraction + byprod) if real_base else 0.0

            supply = base + extras
            demand = float((total_demands.get(pid) or {}).get(m, 0.0)) \
                - _legs_at(loops, pid, m) \
                + _prds_feed_gap(
                    loop, (total_demands.get(pid) or {}),
                    (u4u_demands or {}).get(pid), m)
            need = demand - supply
            try:
                bpc_gen = float(loop._bpc_gen_quantities.get(m) or 0.0)
            except Exception:
                bpc_gen = 0.0
            # BPC 'generation' books the header throughput (local + imported
            # plug).  Subtract the booked imports so the comparison is on the
            # local-production basis the Generation column uses.
            bpc_gen_local = bpc_gen - bimp[pid].get(m, 0.0)
            rows.append({
                "plant_id": pid, "material": m,
                "demand_mt": round(demand, 2),
                "supply_mt": round(supply, 2),
                "bpc_gen_mt": round(bpc_gen_local, 2),
                "bpc_gen_booked_mt": round(bpc_gen, 2),
                "need_mt": round(need, 2),
                "booked_net_mt": round(bnet[pid].get(m, 0.0), 2),
                "model_net_mt": round(mnet[pid].get(m, 0.0), 2),
                "residual_mt": round(need - bnet[pid].get(m, 0.0), 2),
            })
    return rows


# ---------------------------------------------------------------------------
# Entry point used by the orchestrator
# ---------------------------------------------------------------------------

def resolve_steam_transfers(mode: str, loops: dict, prepared: list,
                            steam_results: dict = None,
                            total_demands: dict = None,
                            ext_applied: dict = None,
                            month: int = None, year: int = None,
                            u4u_demands: dict = None):
    """Return (ext_steam, transfer_rows) for the given mode.

    ext_steam[plant_id][material] is a signed demand adjustment applied to
    the plant's next-iteration dispatch demands:
        positive -> export obligation (sender must supply this much more)
        negative -> import credit    (receiver's demand is covered by import)
    """
    mode = (mode or "off").lower()
    if mode == "off":
        return {pid: {} for pid in prepared}, []
    if mode == "bpc":
        return _resolve_bpc(loops, prepared, month=month, year=year)
    if mode == "solve":
        return _resolve_solve(
            loops, prepared, steam_results or {}, total_demands or {},
            ext_applied or {}, month, year, u4u_demands=u4u_demands,
        )
    raise ValueError(f"Unknown steam transfer mode: {mode!r}")


def validate_transfers(rows: list) -> list:
    """Sanity checks on a resolved transfer set, plus any per-edge
    warnings emitted during resolution (band clamps, sender shortfall,
    audit gaps).  Returns a list of warning strings."""
    warnings = []
    by_edge = {}
    for t in rows:
        warnings.extend(t.get("warnings") or [])
        if t.get("edge") and t["edge"] != "-":
            by_edge[t["edge"]] = by_edge.get(t["edge"], 0.0) + t["quantity"]
    for eid, qty in by_edge.items():
        e = _edge(eid)
        if not e:
            continue
        if e.get("audit_only"):
            continue
        if qty < 0:
            warnings.append(f"{eid}: negative transfer quantity {qty}")
    return warnings
