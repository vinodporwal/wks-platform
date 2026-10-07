"""End-to-end DTA PRDS capacity-bound test.

Runs dispatch_steam for DTA (April 2026) against real DB data with the
HP PRDS capacity monkeypatched down, verifying:
  - output clamps at cap, residual reported as unmet (deficit-only)
  - parent feed = capped output x norm
  - zero op-hours -> zero output, full demand unmet
"""
import os
import pickle
import sys

_JMD = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
sys.path.insert(0, _JMD)

import engine.dispatch_engine as de

DTA = "A4AF8441-73AD-4F9F-BCF4-6734E8202F7A"
CACHE = os.path.join(_JMD, "output", "validation_cache", "jmd_result_2026_04.pkl")

power_result = pickle.load(open(CACHE, "rb"))["plants"][DTA]["final_power_result"]

fail = 0

def check(name, cond, detail=""):
    global fail
    print(("PASS" if cond else "FAIL"), name, detail)
    if not cond:
        fail += 1


# --- Scenario A: real caps (April demand should not bind) -------------------
res = de.dispatch_steam(DTA, 4, 2026, power_result)
dd = res["demand_detail"]
prds = {p["produces"]: p for p in res["prds_assets"]}
hp = prds["HP Steam_Dis"]
check("A: HP PRDS under cap", 0 < hp["dispatched_mt"] <= hp["max_mt"],
      f"out={hp['dispatched_mt']:.2f} cap={hp['max_mt']:.0f}")
check("A: feed = out x norm",
      abs(hp["dispatched_mt"] * hp["norm"] - dd.get("hp_letdown", 0)) < 0.01,
      f"feed={dd.get('hp_letdown', 0):.2f}")
check("A: no unmet", res["prds_unmet_mt"] == 0.0, f"unmet={res['prds_unmet_mt']}")


# --- Scenario B: shrink HP PRDS cap to 300 TPH (216,000 MT) -----------------
orig_caps = de.fetch_steam_asset_capacity_all_months
def shrunk_caps(plant_id, fy_year):
    caps = orig_caps(plant_id, fy_year)
    for row in caps:
        if row.get("asset_name") == "HP Steam PRDS":
            for k in list(row):
                if k.endswith("_Max"):
                    row[k] = 300.0
    return caps

de.fetch_steam_asset_capacity_all_months = shrunk_caps
try:
    res = de.dispatch_steam(DTA, 4, 2026, power_result)
    dd = res["demand_detail"]
    hp = {p["produces"]: p for p in res["prds_assets"]}["HP Steam_Dis"]
    check("B: HP PRDS clamped at cap", abs(hp["dispatched_mt"] - 216000.0) < 0.01,
          f"out={hp['dispatched_mt']:.2f}")
    check("B: unmet = net - cap",
          abs(hp["unmet_mt"] - (dd["hp_net"] - 216000.0)) < 0.01,
          f"unmet={hp['unmet_mt']:.2f} net={dd['hp_net']:.2f}")
    check("B: bounded feed = cap x norm",
          abs(dd["hp_letdown"] - 216000.0 * 0.936) < 0.01,
          f"feed={dd['hp_letdown']:.2f}")
    check("B: deficit reported", res["prds_unmet_mt"] > 0
          and "PRDS deficit" in res["message"], res["message"])
finally:
    de.fetch_steam_asset_capacity_all_months = orig_caps


# --- Scenario C: HP PRDS zero operational hours -----------------------------
orig_hours = de.fetch_steam_asset_operational_hours
def zero_hours(plant_id, month, year):
    hrs = orig_hours(plant_id, month, year)
    for row in hrs:
        if row.get("asset_name") == "HP Steam PRDS":
            row["operational_hours"] = 0.0
    return hrs

de.fetch_steam_asset_operational_hours = zero_hours
try:
    res = de.dispatch_steam(DTA, 4, 2026, power_result)
    dd = res["demand_detail"]
    hp = {p["produces"]: p for p in res["prds_assets"]}["HP Steam_Dis"]
    check("C: zero hours -> zero output", hp["dispatched_mt"] == 0.0,
          f"out={hp['dispatched_mt']}")
    check("C: full demand unmet", abs(hp["unmet_mt"] - max(0.0, dd["hp_net"])) < 0.01,
          f"unmet={hp['unmet_mt']:.2f} net={dd['hp_net']:.2f}")
    check("C: zero feed to SHP", dd["hp_letdown"] == 0.0, f"feed={dd['hp_letdown']}")
finally:
    de.fetch_steam_asset_operational_hours = orig_hours

print()
print("ALL PASS" if fail == 0 else f"{fail} FAILURES")
sys.exit(1 if fail else 0)
