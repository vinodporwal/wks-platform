# Synthetic scenario tests for steam_transfer_router.resolve_steam_transfers
# in 'solve' mode.  Uses stub loop objects — no DB access.
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from engine.steam_transfer_router import (
    resolve_steam_transfers, DTA, DTA_PCG, SEZ, SEZ_PCG, C2,
)
from engine.jmd_orchestrator import CPP_ORDER  # for hour lookups? not needed


class StubLoop:
    def __init__(self, plug=None, bpc_gen=None, init_util=None, producers=None):
        self._bpc_steam_plug = plug or {}
        self.bpc_gen_quantities = bpc_gen or {}
        self._initial_utility_demands = init_util or {}
        self._initial_steam_mt = {}
        self._all_producers = producers or set()

    def _lookup_bpc_gen_qty(self, material):
        return self.bpc_gen_quantities.get(material)

    def _lookup_bpc_qty(self, material, producer):
        if str(producer).startswith("STEAM("):
            return self._bpc_steam_plug.get(material)
        return self.bpc_gen_quantities.get(material)


def steam_result(detail=None, gen=0.0):
    return {"demand_detail": detail or {}, "total_generation_mt": gen}


def demand_detail(top="shp", **nets):
    d = {"_top_grade": top}
    d.update(nets)
    return d


def transfer_of(rows, eid):
    for r in rows:
        if r["edge"] == eid:
            return r
    return None


H = 720.0
loops, results, demands = {}, {}, {}

# Baseline "April-like" state:
#  DTA-PCG: SHP supply (passthrough 316k + dispatched 103.6k) vs demand 362k -> +57.8k surplus
#  DTA:     SHP net 633k vs supply 617.8k -> -15.2k deficit
#  SEZ-PCG: SHP demand 529.6k vs passthrough 497.3k -> -32.4k pull (top grade)
#  SEZ-PCG: HP demand 84.5k, gen embodies plug 41.1k -> pull 84.5k (gen_embodies)
#  DTA-PCG: IP demand 26k embodies plug -> pull 26k
#  SEZ:     HP supply 1,173.6k vs demand 1,501.5k -> deficit (commit edge still fires)
#  DTA:     HP net 344.4k vs prds_out 344.4k -> pull 0 -> commit floor only
loops[DTA_PCG] = StubLoop(
    plug={"SHP Steam_Dis": 4343.54, "IP Steam_Dis": 25982.13},
    bpc_gen={"SHP Steam_Dis": 423599.0, "IP Steam_Dis": 25982.13},
    init_util={"Gasifier SHP supply": -316178.0},
    producers={"IP Steam_Dis"},
)
loops[SEZ_PCG] = StubLoop(
    plug={"HP Steam_Dis": 41057.29, "SHP Steam_Dis": 32368.73},
    bpc_gen={"HP Steam_Dis": 41057.29, "SHP Steam_Dis": 529630.0,
             "IP Steam_Dis": 64000.0, "LLP Steam Dis": 39867.0},
    init_util={"Gasifier SHP supply": -497261.0},
    producers={"HP Steam_Dis", "SHP Steam_Dis", "IP Steam_Dis", "LLP Steam Dis"},
)
loops[SEZ] = StubLoop(bpc_gen={}, producers={"HP Steam_Dis"})
loops[DTA] = StubLoop(
    plug={"HP Steam_Dis": 28800.0, "SHP Steam_Dis": 57600.0},
    bpc_gen={"HP Steam_Dis": 28800.0, "SHP Steam_Dis": 0.0},
    producers={"HP Steam_Dis", "SHP Steam_Dis"},
)
loops[C2] = StubLoop(producers={"SHP Steam_Dis", "LP Steam_Dis"})

# steam_results: supply sides via prds_out / top-grade gen; demands via nets
results[DTA_PCG] = steam_result(
    demand_detail("shp", shp_net=12068.0, ip_net=-1.0), gen=103574.0)
results[SEZ_PCG] = steam_result(
    demand_detail("shp", shp_net=0.0, hp_net=84479.0, ip_net=0.0), gen=0.0)
results[SEZ] = steam_result(
    demand_detail("shp", shp_net=1501500.0, hp_net=0.0), gen=1173600.0)
results[DTA] = steam_result(
    demand_detail("shp", shp_net=632971.0, hp_net=344362.0,
                  hp_prds_out=344362.0), gen=617760.0)
results[C2] = steam_result(demand_detail("shp", shp_net=380000.0), gen=379597.0)

demands[DTA_PCG] = {"SHP Steam_Dis": 361926.0, "IP Steam_Dis": 25982.13}
demands[SEZ_PCG] = {"SHP Steam_Dis": 529630.0, "HP Steam_Dis": 84479.0,
                    "IP Steam_Dis": 64000.0, "LLP Steam Dis": -181293.0}
demands[SEZ] = {"SHP Steam_Dis": 1501500.0, "HP Steam_Dis": 1501500.0}
demands[DTA] = {"SHP Steam_Dis": 620775.0, "HP Steam_Dis": 344362.0}
demands[C2] = {"SHP Steam_Dis": 380000.0, "LP Steam_Dis": 0.0}

passed, failed = [], []


def check(name, cond, detail=""):
    (passed if cond else failed).append(name)
    print(("PASS" if cond else "FAIL"), name, detail)


# ---------- Scenario 1: April-like -> forward direction on all edges --------
ext, rows = resolve_steam_transfers(
    "solve", loops, list(loops), results, demands, {}, month=4, year=2026)

t1 = transfer_of(rows, "E1")
# E1 is surplus-evacuation: it gets the surplus left after E4's prior
# commitment on the same pool: 57,826 - 36,000 = 21,826.
check("S1 E1 fwd PCG->DTA residual 21.8k (pool conserved)",
      t1 and t1["sender"] == DTA_PCG and abs(t1["quantity"] - 21826) < 500,
      f"qty={t1['quantity'] if t1 else None}")
t4 = transfer_of(rows, "E4")
check("S1 E4 PCG->SEZ-PCG at commit floor 36k",
      t4 and t4["sender"] == DTA_PCG and abs(t4["quantity"] - 36000) < 1,
      f"qty={t4['quantity'] if t4 else None}")
t2 = transfer_of(rows, "E2")
check("S1 E2 commit floor 28.8k", t2 and abs(t2["quantity"] - 28800) < 1)
t3 = transfer_of(rows, "E3")
check("S1 E3 pull 84.5k>floor", t3 and abs(t3["quantity"] - 84479) < 500,
      f"qty={t3['quantity'] if t3 else None}")
check("S1 C2 no transfers", not ext.get(C2)
      and not any(r["sender"] == C2 or r["receiver"] == C2 for r in rows))
check("S1 DTA credit <=0",
      ext[DTA]["SHP Steam_Dis"] < 0 and ext[DTA]["HP Steam_Dis"] < 0)

# ---------- Scenario 2: direction flip — PCG deficit, DTA surplus ----------
res2 = dict(results)
res2[DTA_PCG] = steam_result(
    demand_detail("shp", shp_net=12068.0, ip_net=-1.0), gen=0.0)   # GTs off
res2[DTA] = steam_result(
    demand_detail("shp", shp_net=500000.0, hp_net=344362.0,
                  hp_prds_out=344362.0), gen=650000.0)
dem2 = dict(demands)
dem2[DTA_PCG] = {"SHP Steam_Dis": 400000.0, "IP Steam_Dis": 25982.13}
ext2, rows2 = resolve_steam_transfers(
    "solve", loops, list(loops), res2, dem2, {}, month=4, year=2026)
t1r = transfer_of(rows2, "E1")
check("S2 E1 REVERSED DTA->PCG",
      t1r and t1r["sender"] == DTA and t1r["receiver"] == DTA_PCG
      and t1r["quantity"] > 0,
      f"sender={t1r['sender'] if t1r else None} qty={t1r['quantity'] if t1r else None}")

# ---------- Scenario 3: capacity binding — huge PCG surplus ----------
res3 = dict(results)
res3[DTA_PCG] = steam_result(
    demand_detail("shp", shp_net=12068.0, ip_net=-1.0), gen=600000.0)  # huge
ext3, rows3 = resolve_steam_transfers(
    "solve", loops, list(loops), res3, demands, {}, month=4, year=2026)
t1c = transfer_of(rows3, "E1")
check("S3 E1 capped at 540TPH*720=388,800",
      t1c and abs(t1c["quantity"] - 388800.0) < 1,
      f"qty={t1c['quantity'] if t1c else None}")

# ---------- Scenario 4: zero hours -> edge inactive ----------
ext4, rows4 = resolve_steam_transfers(
    "solve", loops, list(loops), results, demands, {}, month=1, year=2099)
# _op_hours uses month table; unknown year -> default 720? verify behavior
print("   S4 rows:", [(r["edge"], r["quantity"]) for r in rows4])

# ---------- Scenario 5: SEZ-PCG SHP pull below floor -> floor wins ----------
res5 = dict(results)
res5[SEZ_PCG] = steam_result(
    demand_detail("shp", shp_net=0.0, hp_net=84479.0, ip_net=0.0), gen=0.0)
dem5 = dict(demands)
dem5[SEZ_PCG] = dict(demands[SEZ_PCG])
dem5[SEZ_PCG]["SHP Steam_Dis"] = 505000.0   # pull ~7.7k < floor
ext5, rows5 = resolve_steam_transfers(
    "solve", loops, list(loops), res5, dem5, {}, month=4, year=2026)
t4f = transfer_of(rows5, "E4")
check("S5 E4 floor 36k when pull<floor",
      t4f and abs(t4f["quantity"] - 36000) < 1,
      f"qty={t4f['quantity'] if t4f else None}")

# ---------- Scenario 6: conservation — ext sums ----------
# Non-booked legs must conserve across the pool.
for eid in ("E1", "E4"):
    tr = transfer_of(rows, eid)
    if tr:
        tot = sum(pl.get(tr["material"], 0.0) for pl in ext.values())
        print(f"   conservation check {eid}: material {tr['material']} pool sum={tot:,.1f}")

print()
print(f"{len(passed)} passed, {len(failed)} failed")
if failed:
    print("FAILED:", failed)
    sys.exit(1)
