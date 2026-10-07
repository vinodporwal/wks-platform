"""
Draft steam-balance cascade — April 2026 (analysis only, read-only).

Replays the site-proposed intersite steam cascade on the engine's own
converged bpc-mode JMD state.

Ledger reconstruction per (plant, header material):
    header supply = SUM over the header producer's consumption rows:
                    norm x total_demands[consumed material]
                    (STEAM(<grade>) plug rows excluded = booked imports)
    intrinsic supply = header supply + |applied import credits|
                       (restore generation displaced at receivers)
    intrinsic demand = total_demands[header] - booked transfer legs at node
    position         = intrinsic supply - intrinsic demand   (+ = surplus)

Cascade (site order):
    SEZ-PCG  : E3 HP pull, E4 SHP via DTA hub, E5 IP obligation, E6 LLP audit
    DTA-PCG  : E1 SHP <-> DTA
    DTA/SEZ  : swing plants absorb

Bands are currently 20-320 TPH placeholders on every edge — to be replaced
by monthwise DB configuration.

Run:  py scripts\\draft_steam_cascade.py
"""

import json
import logging
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from engine.jmd_orchestrator import run_jmd_month, cpp_name
from engine.steam_transfer_router import _month_hours

MONTH = int(sys.argv[1]) if len(sys.argv) > 1 else 4
YEAR  = int(sys.argv[2]) if len(sys.argv) > 2 else 2026
HOURS = _month_hours(MONTH, YEAR)

DTA     = "A4AF8441-73AD-4F9F-BCF4-6734E8202F7A"
DTA_PCG = "F6D82E68-C3B6-494F-9905-48F19DC611E3"
SEZ     = "2DFEE33F-4CFD-4887-B9DD-53388AA95271"
SEZ_PCG = "D2C7FBAD-7E00-4642-B3B2-5A768FAC8D45"

# Booked transfer legs inside fixed consumption.  Legs are fused into
# shared fixed rows (DTA SHP one row = E1+E4; SEZ-PCG IP one row = own+E5)
# so stripping is by AMOUNT at the node, not by row.  The per-month leg
# qty is read from the receiver's STEAM(<grade>) plug row (booked basis).
LEG_MAP = {
    # edge: (node where the leg sits in fixed demand)  (plug read at)
    "E1": {"strip": (DTA,     "SHP Steam_Dis"), "plug": (DTA,     "SHP Steam_Dis")},
    "E4": {"strip": (DTA,     "SHP Steam_Dis"), "plug": (SEZ_PCG, "SHP Steam_Dis")},
    "E2": {"strip": (SEZ,     "HP Steam_Dis"),  "plug": (DTA,     "HP Steam_Dis")},
    "E3": {"strip": (SEZ_PCG, "HP Steam_Dis"),  "plug": (SEZ_PCG, "HP Steam_Dis")},
    "E5": {"strip": (SEZ_PCG, "IP Steam_Dis"),  "plug": (DTA_PCG, "IP Steam_Dis")},
}

# E6 booked desalination leg per month (SEZ desal consumption of PCG LLP).
E6_BOOKED = {4: 111_405.0, 5: 130_795.0, 6: 125_496.0, 7: 128_563.0,
             8: 123_430.0, 9: 115_488.0, 10: 135_557.0, 11: 113_544.0,
             12: 133_771.0, 1: 99_398.0, 2: 108_864.0, 3: 132_358.0}

# Placeholder bands — overridden by CPP_IntersiteSteamTransfer per month.
BANDS = {
    "E1": (20.0, 320.0),   # DTA-PCG <-> DTA      SHP
    "E4": (20.0, 320.0),   # PCG <-> SEZ-PCG      SHP (via DTA hub)
    "E2": (20.0, 320.0),   # SEZ -> DTA           HP
    "E3": (20.0, 320.0),   # SEZ -> SEZ-PCG       HP
    "E5": (20.0, 320.0),   # SEZ-PCG -> DTA-PCG   IP
    "E6": (20.0, 320.0),   # SEZ-PCG -> SEZ       LLP (audit)
}

# (sender_plant, receiver_plant, grade) -> edge id, matching the rows of
# dbo.CPP_IntersiteSteamTransfer (NormParameters.Name gives the grade).
EDGE_KEY = {
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


def _fy_string(month: int, year: int) -> str:
    fy = year if month >= 4 else year - 1
    return f"{fy}-{str(fy + 1)[-2:]}"


def load_bands(month: int, year: int) -> dict:
    """Read Min_<Mon>/Max_<Mon> TPH from dbo.CPP_IntersiteSteamTransfer for
    the month's AOP year.  Falls back to the 20-320 placeholder per edge
    when the table/row/cell is missing.  A 0/NULL max means uncapped."""
    bands = dict(BANDS)
    try:
        from database.connection import get_connection
        conn = get_connection()
        cur = conn.cursor()
        cur.execute(
            f"""
            SELECT t.Min_{_MONTH_COL[month]}, t.Max_{_MONTH_COL[month]},
                   t.SenderPlant_FK_Id, t.ReceiverPlant_FK_Id, np.Name
            FROM dbo.CPP_IntersiteSteamTransfer t
            JOIN dbo.NormParameters np ON np.Id = t.NormParameter_FK_Id
            WHERE t.AOP_Year = ?
            """, _fy_string(month, year))
        seen = set()
        for lo, hi, s, r, gname in cur.fetchall():
            grade = str(gname).split()[0].upper()
            eid = EDGE_KEY.get((str(s).upper(), str(r).upper(), grade))
            if eid is None:
                continue
            seen.add(eid)
            bands[eid] = (float(lo or 0.0),
                          float(hi) if hi and float(hi) > 0 else None)
        conn.close()
        missing = set(BANDS) - seen
        print(f"[bands] DB { _fy_string(month, year)} {_MONTH_COL[month]}: "
              + ", ".join(f"{e}={bands[e][0]:g}-{bands[e][1] if bands[e][1] else 'inf'}TPH"
                          for e in sorted(bands))
              + (f"   (no row: {sorted(missing)} -> placeholder)" if missing else ""))
    except Exception as exc:
        print(f"[bands] DB read failed ({exc}) — using 20-320 placeholders")
    return bands

STEAM_HEADERS = [
    "SHP Steam_Dis", "HP Steam_Dis", "MP Steam_Dis",
    "IP Steam_Dis", "LP Steam_Dis", "LP Steam Dis",
    "LLP Steam Dis", "IHP Steam Dis", "IHP Steam_Dis",
]


def _is_plug(material: str) -> bool:
    n = material.strip().upper()
    return n.startswith("STEAM(")


def _is_steamish(material: str) -> bool:
    return "STEAM" in material.upper()


def header_supply(pid, header, total_d, norms_by_plant):
    """Reconstruct header inflow: for each consumption row of the header
    producer, norm x ledger-demand of the consumed (producer) material.
    Plug rows are counted separately so they can be stripped."""
    info = norms_by_plant.get(pid, {}).get(header) or {}
    total, plug = 0.0, 0.0
    parts = []
    for c in info.get("consumptions", []):
        mat = c.get("material", "")
        if not _is_steamish(mat):
            continue
        if _is_plug(mat):
            plug += float(c.get("quantity", 0.0) or 0.0)
            continue
        # Inflow = the feed material's own ledger output (norm is only the
        # booked ratio, e.g. superheater->SHP 0.9389 of header gen).  If the
        # feed never entered the model ledger (qty ~0, e.g. SEZ-PCG's LLP
        # PRDS feed), fall back to the booked inflow qty and flag it.
        ledger = float(total_d[pid].get(mat, 0.0))
        q = ledger if abs(ledger) > 1.0 else float(c.get("quantity", 0.0) or 0.0)
        total += q
        parts.append((mat, q, abs(ledger) <= 1.0))
    return total, plug, parts


STATE_CACHE = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    f".cascade_state_{MONTH}_{YEAR}.json")


def build_state() -> dict:
    """Run the model (bpc mode) and compute intrinsic header positions.

    PRDS feed re-parenting: a PRDS producer's input draw (e.g. IP STEAM
    PRDS consuming HP Steam_Dis @0.89) is demand on the SOURCE header.  At
    the PCG plants that feed demand never lands in the u4u ledger; at
    DTA/SEZ it does.  Add it only where missing (u4u closer to non-PRDS
    sum than to the all-consumers sum).
    """
    logging.disable(logging.CRITICAL)
    result = run_jmd_month(MONTH, YEAR, steam_transfer_mode="bpc")
    logging.disable(logging.NOTSET)

    plants  = result["plants"]
    total_d = {pid: plants[pid]["final_total_demands"] for pid in plants}
    u4u_d   = {pid: plants[pid]["final_u4u_demands"] for pid in plants}
    ext_s   = {pid: plants[pid]["ext_steam"] for pid in plants}
    norms   = {pid: plants[pid]["consumption_norms"] for pid in plants}

    def prds_feed_gap(pid, header):
        non_prds = prds = 0.0
        for prod, info in norms[pid].items():
            for c in info.get("consumptions", []):
                if c.get("material") != header:
                    continue
                q = float(c.get("norm") or 0.0) \
                    * float(total_d[pid].get(prod, 0.0))
                if "PRDS" in prod.upper():
                    prds += q
                else:
                    non_prds += q
        u = float(u4u_d[pid].get(header, 0.0))
        missing = abs(u - non_prds) <= abs(u - non_prds - prds)
        return prds if missing else 0.0

    # per-month booked legs: qty of the STEAM(<grade>) plug row at the
    # plug node, stripped at the booking node (which may differ — E4's
    # transit leg is booked at DTA while the plug sits at SEZ-PCG).
    leg_at = {}
    booked = {}
    for eid, cfg in LEG_MAP.items():
        ppid, pmat = cfg["plug"]
        info = norms.get(ppid, {}).get(pmat) or {}
        qty = 0.0
        for c in info.get("consumptions", []):
            if _is_plug(c.get("material", "")):
                qty += float(c.get("quantity", 0.0) or 0.0)
        booked[eid] = qty
        leg_at[cfg["strip"]] = leg_at.get(cfg["strip"], 0.0) + qty
    booked["E6"] = E6_BOOKED.get(MONTH, 0.0)

    rows = []
    supply_parts = {}
    for pid in (SEZ_PCG, DTA_PCG, DTA, SEZ):
        for m in total_d[pid]:
            if m not in STEAM_HEADERS:
                continue
            sup, plug, parts = header_supply(pid, m, total_d, norms)
            supply_parts[f"{pid}|{m}"] = [[mat, q, fb] for mat, q, fb in parts]
            sup_int = sup + max(0.0, -float(ext_s[pid].get(m, 0.0)))
            legs = leg_at.get((pid, m), 0.0)
            feed = prds_feed_gap(pid, m)
            dem_int = float(total_d[pid].get(m, 0.0)) - legs + feed
            rows.append({
                "pid": pid, "mat": m, "supply": sup_int, "plug": plug,
                "dem_int": dem_int, "legs": legs, "feed": feed,
                "pos": sup_int - dem_int,
            })

    return {"month": MONTH, "year": YEAR, "hours": HOURS,
            "converged": bool(result["converged"]),
            "iters": result["iterations_used"],
            "rows": rows, "booked": booked,
            "supply_parts": supply_parts}


def main():
    if "--reuse" in sys.argv and os.path.exists(STATE_CACHE):
        with open(STATE_CACHE) as fh:
            state = json.load(fh)
        print(f"[state] reused cache {os.path.basename(STATE_CACHE)}")
    else:
        state = build_state()
        with open(STATE_CACHE, "w") as fh:
            json.dump(state, fh)
        print(f"[state] model run cached -> {os.path.basename(STATE_CACHE)}")

    BANDS.update(load_bands(MONTH, YEAR))

    pos = {pid: {} for pid in (SEZ_PCG, DTA_PCG, DTA, SEZ)}
    booked = dict(state["booked"])
    supply_parts = {}
    print("=" * 82)
    print(f"DRAFT STEAM CASCADE  {MONTH}/{YEAR}  ({HOURS:.0f} h)   "
          f"converged={state['converged']} iters={state['iters']}")
    print("=" * 82)
    print("\n--- INTRINSIC POSITION (ledger basis: header supply vs demand) ---")
    print(f"{'CPP':10} {'header':16} {'supply':>11} {'of which plug':>13} "
          f"{'demand':>11} {'legs':>9} {'+PRDS feed':>10} {'position':>11} "
          f"{'TPH':>7}")
    for r in state["rows"]:
        pos[r["pid"]][r["mat"]] = r["pos"]
        supply_parts[(r["pid"], r["mat"])] = [
            tuple(p) for p in state["supply_parts"].get(
                f"{r['pid']}|{r['mat']}", [])]
        print(f"{cpp_name(r['pid'])[:10]:10} {r['mat'][:16]:16} "
              f"{r['supply']:11,.0f} {r['plug']:13,.0f} "
              f"{r['dem_int']:11,.0f} {r['legs']:9,.0f} "
              f"{r['feed']:10,.0f} {r['pos']:+11,.0f} "
              f"{r['pos']/HOURS:+7.1f}")

    warnings = []
    flows = {}

    def band(q, edge, label):
        lo, hi = BANDS[edge]
        if 0 < q < lo * HOURS:
            warnings.append(f"{edge} {label}: {q/HOURS:.1f} TPH < min "
                            f"{lo:.0f} -> forced to {lo*HOURS:,.0f}")
            return lo * HOURS
        if hi and q > hi * HOURS:
            warnings.append(f"{edge} {label}: {q/HOURS:.1f} TPH > max "
                            f"{hi:.0f} -> capped {hi*HOURS:,.0f}")
            return hi * HOURS
        return q

    # --- Stage 1: SEZ-PCG ----------------------------------------------------
    print("\n--- STAGE 1: SEZ-PCG ---")
    shp_pcg = pos[SEZ_PCG].get("SHP Steam_Dis", 0.0)
    hp_pcg  = pos[SEZ_PCG].get("HP Steam_Dis", 0.0)
    llp_pcg = pos[SEZ_PCG].get("LLP Steam Dis", 0.0)

    e3 = band(max(0.0, -hp_pcg), "E3", "SEZ-PCG HP pull")
    flows["E3"] = (SEZ, SEZ_PCG, e3)
    print(f"  HP  pos {hp_pcg:+11,.0f} -> E3 pull {e3:11,.0f} ({e3/HOURS:5.1f})")
    if hp_pcg > 0:
        warnings.append(
            f"E3 anomaly: SEZ-PCG HP ledger shows +{hp_pcg:,.0f} surplus "
            f"(byproduct credits -93,832 vs consumption) while BPC books "
            f"a 41,057 import — confirm the HP credit rows with site")

    if shp_pcg < 0:
        e4 = band(-shp_pcg, "E4", "SEZ-PCG SHP deficit")
        flows["E4"] = (DTA, SEZ_PCG, e4)
        print(f"  SHP pos {shp_pcg:+11,.0f} -> E4 pull {e4:11,.0f} "
              f"({e4/HOURS:5.1f}) [via DTA hub]")
    else:
        e4 = band(shp_pcg, "E4", "SEZ-PCG SHP surplus")
        flows["E4"] = (SEZ_PCG, DTA, e4)
        print(f"  SHP pos {shp_pcg:+11,.0f} -> E4 push {e4:11,.0f} "
              f"({e4/HOURS:5.1f}) [via DTA hub]")

    # E5 — sender-side committed leg (fused into the 33,686 IP fixed row)
    ip_pcg = pos[SEZ_PCG].get("IP Steam_Dis", 0.0)
    e5 = band(max(0.0, ip_pcg), "E5", "SEZ-PCG IP export")
    flows["E5"] = (SEZ_PCG, DTA_PCG, e5)
    print(f"  IP  pos {ip_pcg:+11,.0f} -> E5 push {e5:11,.0f} ({e5/HOURS:5.1f})")

    # E6 — audit: LLP position should equal the desalination leg
    e6_booked = booked.get("E6", 0.0)
    print(f"  LLP pos {llp_pcg:+11,.0f} -> E6 audit (booked desal leg "
          f"{e6_booked:,.0f}, diff {llp_pcg - e6_booked:+,.0f})")
    flows["E6"] = (SEZ_PCG, SEZ, llp_pcg)
    if abs(llp_pcg - e6_booked) > 5000:
        warnings.append(
            f"E6 LLP reconcile gap: position {llp_pcg:+,.0f} vs booked "
            f"desal leg {e6_booked:,.0f}")

    # --- Stage 2: DTA-PCG ----------------------------------------------------
    print("\n--- STAGE 2: DTA-PCG ---")
    shp_dpcg = pos[DTA_PCG].get("SHP Steam_Dis", 0.0)
    if shp_dpcg > 0:
        e1 = band(shp_dpcg, "E1", "DTA-PCG SHP surplus")
        flows["E1"] = (DTA_PCG, DTA, e1)
        print(f"  SHP pos {shp_dpcg:+11,.0f} -> E1 push {e1:11,.0f} "
              f"({e1/HOURS:5.1f})")
    else:
        e1 = band(-shp_dpcg, "E1", "DTA-PCG SHP deficit")
        flows["E1"] = (DTA, DTA_PCG, e1)
        print(f"  SHP pos {shp_dpcg:+11,.0f} -> E1 pull {e1:11,.0f} "
              f"({e1/HOURS:5.1f}) [reversed]")
    hp_dpcg = pos[DTA_PCG].get("HP Steam_Dis", 0.0)
    print(f"  HP  pos {hp_dpcg:+11,.0f}  (no E7 edge — stays internal)")
    if abs(hp_dpcg) > 5000:
        warnings.append(
            f"DTA-PCG HP residual {hp_dpcg:+,.0f} with no intersite edge — "
            f"internal PRDS must cover; check ledger")

    # --- Stage 3: DTA swing + SEZ -------------------------------------------
    print("\n--- STAGE 3: DTA (SHP swing) & SEZ (HP) ---")
    s1, r1, q1 = flows["E1"]
    s4, r4, q4 = flows["E4"]
    dta_pool = pos[DTA].get("SHP Steam_Dis", 0.0)
    dta_pool += q1 if r1 == DTA else -q1
    dta_pool += q4 if r4 == DTA else -q4
    print(f"  DTA intrinsic {pos[DTA].get('SHP Steam_Dis',0.0):+11,.0f} "
          f"+E1 {q1 if r1==DTA else -q1:+11,.0f} "
          f"+E4 {q4 if r4==DTA else -q4:+11,.0f} = {dta_pool:+11,.0f}")
    if dta_pool > 0:
        warnings.append(f"DTA SHP surplus {dta_pool:,.0f} post-transfer — "
                        f"shed lowest-priority assets (aux turndown)")
    else:
        warnings.append(f"DTA SHP deficit {-dta_pool:,.0f} post-transfer — "
                        f"aux boiler cover required")

    # SEZ HP: exports E2 (committed ~40 TPH) + E3 (pull)
    e2_commit = booked.get("E2", 0.0)   # committed SEZ->DTA HP export —
                                        # later from monthwise config table
    e2 = band(max(e2_commit, BANDS["E2"][0] * HOURS), "E2", "committed")
    flows["E2"] = (SEZ, DTA, e2)
    sez_hp_pos = pos[SEZ].get("HP Steam_Dis", 0.0)
    sez_need = e2 + e3 - max(0.0, sez_hp_pos)
    print(f"  SEZ HP pos {sez_hp_pos:+11,.0f}; obligations E2 {e2:,.0f} + "
          f"E3 {e3:,.0f}")
    if sez_need > 0:
        warnings.append(
            f"SEZ HP: obligations {e2+e3:,.0f} exceed surplus "
            f"{max(0.0,sez_hp_pos):,.0f} — needs +{sez_need:,.0f} extra "
            f"generation (aux/HRSG headroom) or partial transfer + warn")

    # --- solved vs booked ----------------------------------------------------
    print("\n--- SOLVED vs BOOKED ---")
    print(f"{'edge':4} {'flow':24} {'solved':>11} {'TPH':>7} {'booked':>11} "
          f"{'diff':>10}")
    for eid in ("E3", "E4", "E5", "E1", "E2", "E6"):
        s, r, q = flows.get(eid, (None, None, 0.0))
        b = booked.get(eid, 0.0)
        print(f"{eid:4} {cpp_name(s)[:11] + '->' + cpp_name(r)[:11]:24} "
              f"{q:11,.0f} {q/HOURS:7.1f} {b:11,.0f} {q-b:+10,.0f}")

    # --- post-transfer residuals ---------------------------------------------
    print("\n--- POST-TRANSFER RESIDUALS ---")
    edge_mat = {"E1": "SHP Steam_Dis", "E2": "HP Steam_Dis",
                "E3": "HP Steam_Dis", "E4": "SHP Steam_Dis",
                "E5": "IP Steam_Dis"}
    res_map = {pid: dict(pos[pid]) for pid in pos}
    for eid, m in edge_mat.items():
        s, r, q = flows[eid]
        res_map[s][m] = res_map[s].get(m, 0.0) - q
        res_map[r][m] = res_map[r].get(m, 0.0) + q
    for pid in (SEZ_PCG, DTA_PCG, DTA, SEZ):
        for m, v in res_map[pid].items():
            tag = "ok" if abs(v) < 6000 else "<== check"
            if (pid, m) == (DTA_PCG, "IP Steam_Dis"):
                tag = "displaced letdown (E5 credit backs off IP PRDS)"
            elif (pid, m) == (SEZ, "HP Steam_Dis"):
                tag = "E3 share unserved — SEZ must generate it (aux headroom)"
            elif (pid, m) == (SEZ_PCG, "LLP Steam Dis"):
                tag = "E6 export to SEZ desal (audit)"
            elif (pid, m) == (DTA, "SHP Steam_Dis"):
                tag = "swing surplus — shed lowest-priority aux"
            print(f"  {cpp_name(pid)[:10]:10} {m[:16]:16} residual "
                  f"{v:+11,.0f}  {tag}")

    # --- supply decomposition audit ------------------------------------------
    print("\n--- SUPPLY DECOMPOSITION (header inflow audit) ---")
    for (pid, m), parts in supply_parts.items():
        if not parts:
            continue
        desc = " + ".join(
            f"{mat}={q:,.0f}{'[booked-fallback]' if fb else ''}"
            for mat, q, fb in parts if abs(q) > 1)
        print(f"  {cpp_name(pid)[:10]:10} {m[:16]:16} <- {desc}")

    # --- warnings -------------------------------------------------------------
    print("\n" + "=" * 82)
    print("[STEAM-XFER] ============ WARNING SUMMARY ============")
    for w in warnings:
        print(f"  WARN  {w}")
    print("=" * 82)


if __name__ == "__main__":
    main()
