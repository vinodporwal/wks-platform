"""
Demand Segregation & Capacity Matching for JMD plants.

Workflow
--------
1. load_consumption(plant_id, month)
   - Tries DB first (fetch_process_demands + fetch_fixed_consumption).
   - Falls back to dummy_consumption.json when DB returns all-zeros.

2. segregate_demand(consumption)
   - Splits combined consumption dict into per-utility segments
     with total = process + fixed, unit label, and category.

3. load_asset_capacity(plant_id, result)
   - Tries DB assets (from calculator result).
   - Falls back to dummy asset_capacity from JSON when DB list is empty.

4. match_capacity(demand_segments, asset_capacity)
   - For each utility, dispatches assets in priority order until demand
     is met or all assets are exhausted.
   - Returns per-utility coverage verdict: COVERED / SHORTFALL / NO_ASSETS.

5. run_demand_capacity(plant_id, month, year, result)
   - Top-level entry point called from calculator result dict.
   - Returns a structured report dict.
"""

import json
import logging
import os

logger = logging.getLogger(__name__)

_DATA_FILE = os.path.join(os.path.dirname(os.path.dirname(__file__)), "data", "dummy_consumption.json")

# Month number → FY array index (Apr=0 … Mar=11)
_MONTH_IDX = {4: 0, 5: 1, 6: 2, 7: 3, 8: 4, 9: 5, 10: 6, 11: 7, 12: 8, 1: 9, 2: 10, 3: 11}

_MONTH_NAMES = {
    1: "Jan", 2: "Feb", 3: "Mar", 4: "Apr", 5: "May", 6: "Jun",
    7: "Jul", 8: "Aug", 9: "Sep", 10: "Oct", 11: "Nov", 12: "Dec",
}

# Metadata for each demand key: (label, unit, category)
_DEMAND_META = {
    "power_process":     ("Power (Process)",      "KWH",  "power"),
    "power_fixed":       ("Power (Fixed)",         "MWh",  "power"),
    "lp_process":        ("LP Steam (Process)",    "MT",   "steam"),
    "lp_fixed":          ("LP Steam (Fixed)",      "MT",   "steam"),
    "mp_process":        ("MP Steam (Process)",    "MT",   "steam"),
    "mp_fixed":          ("MP Steam (Fixed)",      "MT",   "steam"),
    "hp_process":        ("HP Steam (Process)",    "MT",   "steam"),
    "hp_fixed":          ("HP Steam (Fixed)",      "MT",   "steam"),
    "shp_process":       ("SHP Steam (Process)",   "MT",   "steam"),
    "shp_fixed":         ("SHP Steam (Fixed)",     "MT",   "steam"),
    "air_process":       ("Compressed Air",        "NM3",  "utilities"),
    "nitrogen_asu_process": ("Nitrogen (ASU)",      "NM3",  "utilities"),
    "dm_process":        ("DM Water",              "M3",   "utilities"),
    "cw1_process":       ("Cooling Water 1",       "KM3",  "utilities"),
    "cw2_process":       ("Cooling Water 2",       "KM3",  "utilities"),
    "cooling_water_process": ("Cooling Water",     "KM3",  "utilities"),
    "raw_water_process": ("Raw Water",             "M3",   "utilities"),
    "utility_water_process": ("Utility Water",     "M3",   "utilities"),
    "ret_steam_condensate_process": ("Ret Steam Condensate", "M3", "utilities"),
    "oxygen_process":    ("Oxygen",                "MT",   "utilities"),
    "effluent_process":  ("Effluent",              "M3",   "utilities"),
}

# Aggregate utility → keys that contribute to it
_UTILITY_ROLLUP = {
    "power": ["power_process", "power_fixed"],
    "lp":    ["lp_process",    "lp_fixed"],
    "mp":    ["mp_process",    "mp_fixed"],
    "hp":    ["hp_process",    "hp_fixed"],
    "shp":   ["shp_process",   "shp_fixed"],
    "air":          ["air_process"],
    "nitrogen_asu": ["nitrogen_asu_process"],
    "dm_water":     ["dm_process"],
    "cw1":          ["cw1_process"],
    "cw2":          ["cw2_process"],
    "cooling_water": ["cooling_water_process"],
    "raw_water":    ["raw_water_process"],
    "utility_water": ["utility_water_process"],
    "ret_steam_condensate": ["ret_steam_condensate_process"],
    "oxygen":       ["oxygen_process"],
    "effluent":     ["effluent_process"],
}


# ---------------------------------------------------------------------------
# JSON loader (cached)
# ---------------------------------------------------------------------------

_json_cache: dict = {}


def _load_json() -> dict:
    global _json_cache
    if not _json_cache:
        with open(_DATA_FILE, "r") as f:
            _json_cache = json.load(f)
    return _json_cache


# ---------------------------------------------------------------------------
# 1. Load consumption — DB first, JSON fallback
# ---------------------------------------------------------------------------

def load_consumption(plant_id: str, month: int, year: int, db_demands: dict) -> dict:
    """
    Return a merged fixed+process consumption dict for the plant/month.

    Uses db_demands (already fetched by calculator) if non-zero.
    Falls back to JSON dummy values when DB returned all zeros.

    Args:
        plant_id:   plant UUID
        month:      1-12
        year:       calendar year
        db_demands: dict from calculator result["demands"] (merged fixed+process)

    Returns:
        dict with all demand keys, values filled from DB or JSON.
    """
    pid = plant_id.upper()
    idx = _MONTH_IDX.get(month)
    if idx is None:
        logger.warning("[DC] Invalid month %s", month)
        return db_demands or {}

    # Check if DB returned meaningful data (any non-zero value)
    has_db_data = db_demands and any(v > 0 for v in db_demands.values())
    if has_db_data:
        logger.debug("[DC] Using DB consumption data for %s %s/%s", pid, month, year)
        return db_demands

    logger.debug("[DC] DB returned empty — using dummy JSON for %s %s/%s", pid, month, year)
    data = _load_json()
    plant_data = data.get("plants", {}).get(pid, {})

    result = {}

    fixed = plant_data.get("fixed_consumption", {})
    for key in ("power_fixed", "lp_fixed", "mp_fixed", "hp_fixed", "shp_fixed"):
        vals = fixed.get(key, [])
        result[key] = float(vals[idx]) if idx < len(vals) else 0.0

    process = plant_data.get("process_demands", {})
    for key in (
        "power_process", "lp_process", "mp_process", "hp_process", "shp_process",
        "air_process", "nitrogen_asu_process", "dm_process", "cw1_process",
        "cw2_process", "cooling_water_process", "raw_water_process",
        "utility_water_process", "ret_steam_condensate_process",
        "oxygen_process", "effluent_process",
    ):
        vals = process.get(key, [])
        result[key] = float(vals[idx]) if idx < len(vals) else 0.0

    return result


# ---------------------------------------------------------------------------
# 2. Demand segregation
# ---------------------------------------------------------------------------

def segregate_demand(consumption: dict) -> dict:
    """
    Split consumption dict into per-utility segments and roll up to aggregate totals.

    Returns:
        {
            "segments": {
                "power_process": {"label": "Power (Process)", "value": ..., "unit": "KWH", "category": "power"},
                ...
            },
            "totals": {
                "power":    {"total": ..., "unit": "KWH/MWh", "components": {...}},
                "lp":       {"total": ..., "unit": "MT",      "components": {...}},
                ...
            }
        }
    """
    segments = {}
    for key, value in consumption.items():
        meta = _DEMAND_META.get(key)
        if meta:
            label, unit, category = meta
            segments[key] = {
                "label":    label,
                "value":    round(float(value), 2),
                "unit":     unit,
                "category": category,
            }

    totals = {}
    for util, keys in _UTILITY_ROLLUP.items():
        components = {k: round(float(consumption.get(k, 0.0)), 2) for k in keys}
        total_val  = sum(components.values())
        # Pick unit from first key's meta
        unit = _DEMAND_META.get(keys[0], ("", "", ""))[1]
        totals[util] = {
            "total":      round(total_val, 2),
            "unit":       unit,
            "components": components,
        }

    return {"segments": segments, "totals": totals}


# ---------------------------------------------------------------------------
# 3. Load asset capacity — DB assets first, JSON fallback
# ---------------------------------------------------------------------------

def load_asset_capacity(plant_id: str, db_assets: list, db_steam_assets: list) -> dict:
    """
    Build a unified asset capacity dict for the plant.

    Tries DB asset lists. Falls back to JSON dummy when both lists are empty.

    Returns:
        {
            "power_assets":   [{"asset_name", "type", "capacity_mw", "min_mw", "max_mw", "op_hours_month"}, ...],
            "steam_assets":   [{"asset_name", "type", "steam_type", "capacity_tph", ...}, ...],
            "utility_assets": [{"asset_name", "utility", "capacity_*", ...}, ...],
            "source":         "db" | "json"
        }
    """
    pid = plant_id.upper()
    has_db = bool(db_assets or db_steam_assets)

    if has_db:
        logger.debug("[DC] Using DB asset list for %s", pid)
        # Normalise DB power assets — capacity_mw comes from CPPAssetOperationalHours max
        power_assets = []
        for a in (db_assets or []):
            power_assets.append({
                "asset_name":      a.get("asset_name", a.get("AssetName", "")),
                "type":            a.get("asset_type", a.get("AssetType", "")),
                "capacity_mw":     float(a.get("max_capacity_mw", a.get("MaxOperatingCapacity") or 0)),
                "min_mw":          float(a.get("min_capacity_mw", a.get("MinOperatingCapacity") or 0)),
                "max_mw":          float(a.get("max_capacity_mw", a.get("MaxOperatingCapacity") or 0)),
                "op_hours_month":  float(a.get("operational_hours", a.get("OperationalHours") or 720)),
            })

        steam_assets = []
        for a in (db_steam_assets or []):
            steam_assets.append({
                "asset_name":     a.get("asset_name", ""),
                "type":           a.get("asset_type", ""),
                "steam_type":     a.get("steam_type", ""),
                "capacity_tph":   0.0,  # not in DB power asset table — use JSON supplement
                "min_tph":        0.0,
                "max_tph":        0.0,
                "op_hours_month": 720,
            })

        return {
            "power_assets":   power_assets,
            "steam_assets":   steam_assets,
            "utility_assets": [],
            "source":         "db",
        }

    logger.debug("[DC] DB assets empty — using dummy JSON for %s", pid)
    data    = _load_json()
    cap = data.get("asset_capacity", {}).get(pid, {})
    return {
        "power_assets":   cap.get("power_assets",   []),
        "steam_assets":   cap.get("steam_assets",   []),
        "utility_assets": cap.get("utility_assets", []),
        "source":         "json",
    }


# ---------------------------------------------------------------------------
# 4. Capacity matching
# ---------------------------------------------------------------------------

def match_capacity(demand_totals: dict, asset_capacity: dict, op_hours: int = 720) -> dict:
    """
    Dispatch assets against each utility demand and report coverage.

    Power logic:
        available_mwh = sum(asset.capacity_mw × op_hours_month) across power_assets
        demand_mwh    = power.total (KWH → MWh conversion ÷ 1000)

    Steam logic (LP/MP/HP/SHP all fed from SHP generation via let-downs):
        available_mt  = sum(asset.capacity_tph × op_hours_month) across steam_assets
        demand_mt     = shp + hp + mp + lp totals (MT)

    Utility logic (air, dm, cw, water):
        available = capacity × op_hours
        demand    = total from demand_totals

    Args:
        demand_totals: output of segregate_demand()["totals"]
        asset_capacity: output of load_asset_capacity()
        op_hours: default monthly operating hours when not set per asset

    Returns:
        {
            "power":   { "demand_mwh", "available_mwh", "surplus_mwh",
                         "status": "COVERED|SHORTFALL|NO_ASSETS",
                         "assets_dispatched": [...] },
            "steam":   { "demand_mt",  "available_mt",  "surplus_mt",  "status", "assets_dispatched" },
            "air":     { "demand_nm3", "available_nm3", "surplus_nm3", "status", "assets_dispatched" },
            ...
        }
    """
    results = {}

    # --- POWER ---
    power_demand_kwh = demand_totals.get("power", {}).get("total", 0.0)
    power_demand_mwh = round(power_demand_kwh / 1000.0, 2)
    power_assets     = asset_capacity.get("power_assets", [])
    results["power"] = _match_power(power_demand_mwh, power_assets)

    # --- STEAM (SHP → let-downs cover HP/MP/LP) ---
    steam_demand_mt = sum(
        demand_totals.get(u, {}).get("total", 0.0)
        for u in ("shp", "hp", "mp", "lp")
    )
    steam_assets = asset_capacity.get("steam_assets", [])
    results["steam"] = _match_steam(round(steam_demand_mt, 2), steam_assets)

    # --- UTILITY ASSETS ---
    utility_map = {
        "air":        ("air_process",       "NM3",  "capacity_nm3h"),
        "nitrogen_asu": ("nitrogen_asu",    "NM3",  "capacity_nm3h"),
        "dm_water":   ("dm_water",          "M3",   "capacity_m3h"),
        "cw1":        ("cw1",               "KM3",  "capacity_km3h"),
        "cw2":        ("cw2",               "KM3",  "capacity_km3h"),
        "cooling_water": ("cooling_water",  "KM3",  "capacity_km3h"),
        "raw_water":  ("raw_water",         "M3",   "capacity_m3h"),
        "utility_water": ("utility_water",  "M3",   "capacity_m3h"),
        "ret_steam_condensate": ("ret_steam_condensate", "M3", "capacity_m3h"),
    }
    util_assets = asset_capacity.get("utility_assets", [])
    for util_key, (demand_key, unit, cap_field) in utility_map.items():
        demand_val = demand_totals.get(demand_key, {}).get("total", 0.0)
        matching   = [a for a in util_assets if a.get("utility") == util_key]
        results[util_key] = _match_utility(util_key, demand_val, unit, cap_field, matching)

    return results


def _match_power(demand_mwh: float, power_assets: list) -> dict:
    if not power_assets:
        return {
            "demand_mwh": demand_mwh, "available_mwh": 0.0, "surplus_mwh": -demand_mwh,
            "status": "NO_ASSETS", "assets_dispatched": [],
        }

    dispatched = []
    available  = 0.0
    remaining  = demand_mwh

    for a in power_assets:
        if remaining <= 0:
            break
        hrs      = float(a.get("op_hours_month", 720))
        max_mwh  = float(a.get("max_mw", a.get("capacity_mw", 0))) * hrs
        contrib  = min(max_mwh, remaining)
        available += max_mwh
        remaining -= contrib
        dispatched.append({
            "asset_name":    a.get("asset_name"),
            "type":          a.get("type", ""),
            "capacity_mw":   a.get("capacity_mw", a.get("max_mw", 0)),
            "op_hours":      hrs,
            "available_mwh": round(max_mwh, 2),
            "dispatched_mwh": round(contrib, 2),
        })

    # Count remaining capacity from un-needed assets
    for a in power_assets[len(dispatched):]:
        hrs     = float(a.get("op_hours_month", 720))
        max_mwh = float(a.get("max_mw", a.get("capacity_mw", 0))) * hrs
        available += max_mwh

    surplus = round(available - demand_mwh, 2)
    return {
        "demand_mwh":        round(demand_mwh, 2),
        "available_mwh":     round(available, 2),
        "surplus_mwh":       surplus,
        "status":            "COVERED" if surplus >= 0 else "SHORTFALL",
        "assets_dispatched": dispatched,
    }


def _match_steam(demand_mt: float, steam_assets: list) -> dict:
    if not steam_assets:
        return {
            "demand_mt": demand_mt, "available_mt": 0.0, "surplus_mt": -demand_mt,
            "status": "NO_ASSETS", "assets_dispatched": [],
        }

    dispatched = []
    available  = 0.0
    remaining  = demand_mt

    for a in steam_assets:
        hrs         = float(a.get("op_hours_month", 720))
        cap_tph     = float(a.get("max_tph", a.get("capacity_tph", 0)))
        avail_mt    = cap_tph * hrs
        contrib     = min(avail_mt, remaining) if remaining > 0 else 0.0
        available  += avail_mt
        remaining  -= contrib
        dispatched.append({
            "asset_name":     a.get("asset_name"),
            "type":           a.get("type", ""),
            "steam_type":     a.get("steam_type", ""),
            "capacity_tph":   cap_tph,
            "op_hours":       hrs,
            "available_mt":   round(avail_mt, 2),
            "dispatched_mt":  round(contrib, 2),
        })

    surplus = round(available - demand_mt, 2)
    return {
        "demand_mt":         round(demand_mt, 2),
        "available_mt":      round(available, 2),
        "surplus_mt":        surplus,
        "status":            "COVERED" if surplus >= 0 else "SHORTFALL",
        "assets_dispatched": dispatched,
    }


def _match_utility(util_key: str, demand: float, unit: str, cap_field: str, assets: list) -> dict:
    if not assets:
        return {
            "utility":   util_key,
            "demand":    round(demand, 2),
            "available": 0.0,
            "surplus":   round(-demand, 2),
            "unit":      unit,
            "status":    "NO_ASSETS",
            "assets_dispatched": [],
        }

    dispatched = []
    available  = 0.0
    remaining  = demand

    for a in assets:
        hrs      = float(a.get("op_hours_month", 720))
        cap_rate = float(a.get(cap_field, 0))
        avail    = cap_rate * hrs
        contrib  = min(avail, remaining) if remaining > 0 else 0.0
        available += avail
        remaining -= contrib
        dispatched.append({
            "asset_name":    a.get("asset_name"),
            "capacity_rate": cap_rate,
            "op_hours":      hrs,
            "available":     round(avail, 2),
            "dispatched":    round(contrib, 2),
        })

    surplus = round(available - demand, 2)
    return {
        "utility":   util_key,
        "demand":    round(demand, 2),
        "available": round(available, 2),
        "surplus":   surplus,
        "unit":      unit,
        "status":    "COVERED" if surplus >= 0 else "SHORTFALL",
        "assets_dispatched": dispatched,
    }


# ---------------------------------------------------------------------------
# 5. Top-level entry point
# ---------------------------------------------------------------------------

def run_demand_capacity(plant_id: str, month: int, year: int, calc_result: dict) -> dict:
    """
    Run demand segregation and capacity matching using a calculator result dict.

    Args:
        plant_id:    plant UUID
        month:       1-12
        year:        calendar year
        calc_result: dict from engine.calculator.run_month()

    Returns:
        {
            "plant_id", "plant_name", "month", "year",
            "data_source": "db" | "json",
            "consumption": {...},       # raw merged values
            "demand_segregation": {     # segments + totals
                "segments": {...},
                "totals":   {...},
            },
            "asset_capacity": {...},    # normalised asset lists
            "capacity_match": {...},    # per-utility coverage
            "summary": {
                "total_utilities": int,
                "covered": int,
                "shortfall": int,
                "no_assets": int,
            }
        }
    """
    plant_name = calc_result.get("plant_name", plant_id)

    # Step 1 — consumption
    db_demands  = calc_result.get("demands") or {}
    consumption = load_consumption(plant_id, month, year, db_demands)
    data_source = "db" if (db_demands and any(v > 0 for v in db_demands.values())) else "json"

    # Step 2 — segregate
    segregation = segregate_demand(consumption)

    # Step 3 — assets
    db_assets       = calc_result.get("assets") or []
    db_steam_assets = calc_result.get("steam_assets") or []
    asset_cap       = load_asset_capacity(plant_id, db_assets, db_steam_assets)
    if asset_cap["source"] == "db":
        data_source = "db"

    # Step 4 — match
    capacity_match = match_capacity(segregation["totals"], asset_cap)

    # Summary
    statuses = [v.get("status") for v in capacity_match.values()]
    summary  = {
        "total_utilities": len(statuses),
        "covered":         statuses.count("COVERED"),
        "shortfall":       statuses.count("SHORTFALL"),
        "no_assets":       statuses.count("NO_ASSETS"),
    }

    return {
        "plant_id":           plant_id,
        "plant_name":         plant_name,
        "month":              month,
        "year":               year,
        "data_source":        data_source,
        "consumption":        consumption,
        "demand_segregation": segregation,
        "asset_capacity":     asset_cap,
        "capacity_match":     capacity_match,
        "summary":            summary,
    }
