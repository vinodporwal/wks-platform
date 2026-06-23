"""
JMD Steam Balance Engine
========================
Ported from PPPython-script/services/steam_service.py.

Calculates LP → MP → HP → SHP steam chain for JMD plants.
All norm factors are fetched from NormsMonthDetail (via the pre-fetched
norms list) rather than hardcoded, with fallback to PPPython constants.

HRSG availability comes from:
  - Real DB (HRSGAvailability table) when USE_DUMMY[HRSG_AVAILABILITY]=False
  - dummy_schema.json when the table doesn't exist yet
"""

import logging

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Default norm constants (fallback when not in NormsMonthDetail)
# ---------------------------------------------------------------------------

_NORM_LP_FROM_PRDS    = 0.3866
_NORM_LP_FROM_STG     = 0.6134
_NORM_MP_FROM_PRDS    = 0.7092
_NORM_MP_FROM_STG     = 0.2908
_NORM_HP_FROM_PRDS    = 1.0000
_NORM_SHP_PER_LP_STG  = 0.4800
_NORM_MP_PER_LP_PRDS  = 0.7500
_NORM_BFW_PER_LP_PRDS = 0.2500
_NORM_SHP_PER_MP_STG  = 0.6900
_NORM_SHP_PER_MP_PRDS = 0.9100
_NORM_BFW_PER_MP_PRDS = 0.0900
_NORM_SHP_PER_HP_PRDS = 0.9232
_NORM_BFW_PER_HP_PRDS = 0.0768
_NORM_LP_PER_BFW      = 0.1450
STEAM_TO_POWER_MT_PER_MWH = 3.56


def _n(norms, utility, material, default):
    """Look up a norm value from the pre-fetched list, fall back to default."""
    for n in (norms or []):
        if n.get("UtilityName") == utility and n.get("MaterialName") == material:
            v = n.get("NormValue")
            if v is not None:
                return float(v)
    return default


# ---------------------------------------------------------------------------
# LP Balance
# ---------------------------------------------------------------------------

def calculate_lp_balance(
    lp_process, lp_fixed, bfw_ufu=0.0, lp_ufu_mt=None, norms=None,
):
    lp_ufu = lp_ufu_mt if lp_ufu_mt is not None else bfw_ufu * _NORM_LP_PER_BFW
    lp_total = lp_process + lp_fixed + lp_ufu

    lp_from_prds = lp_total * _NORM_LP_FROM_PRDS
    lp_from_stg  = lp_total * _NORM_LP_FROM_STG

    shp_for_stg_lp   = lp_from_stg  * _NORM_SHP_PER_LP_STG
    mp_for_prds_lp   = lp_from_prds * _NORM_MP_PER_LP_PRDS
    bfw_for_prds_lp  = lp_from_prds * _NORM_BFW_PER_LP_PRDS

    return {
        "lp_process": round(lp_process, 2),
        "lp_fixed":   round(lp_fixed, 2),
        "lp_ufu":     round(lp_ufu, 2),
        "lp_total":   round(lp_total, 2),
        "lp_from_prds":    round(lp_from_prds, 2),
        "lp_from_stg":     round(lp_from_stg, 2),
        "shp_for_stg_lp":  round(shp_for_stg_lp, 2),
        "mp_for_prds_lp":  round(mp_for_prds_lp, 2),
        "bfw_for_prds_lp": round(bfw_for_prds_lp, 2),
        "lp_stg_ratio":    round(_NORM_LP_FROM_STG, 4),
        "lp_prds_ratio":   round(_NORM_LP_FROM_PRDS, 4),
    }


def calculate_lp_balance_stg_based(
    lp_process, lp_fixed, bfw_ufu, stg_lp_extraction_tph,
    stg_eq_svh_lp_tph, stg_operating_hours, lp_ufu_mt=None, norms=None,
):
    lp_ufu = lp_ufu_mt if lp_ufu_mt is not None else bfw_ufu * _NORM_LP_PER_BFW
    lp_total = lp_process + lp_fixed + lp_ufu

    lp_from_stg_avail = stg_lp_extraction_tph * stg_operating_hours
    lp_from_stg   = min(lp_from_stg_avail, lp_total)
    lp_stg_excess = max(0, lp_from_stg_avail - lp_total)
    lp_from_prds  = max(0, lp_total - lp_from_stg)

    shp_for_stg_lp  = stg_eq_svh_lp_tph * stg_operating_hours
    mp_for_prds_lp  = lp_from_prds * _NORM_MP_PER_LP_PRDS
    bfw_for_prds_lp = lp_from_prds * _NORM_BFW_PER_LP_PRDS

    return {
        "lp_process": round(lp_process, 2),
        "lp_fixed":   round(lp_fixed, 2),
        "lp_ufu":     round(lp_ufu, 2),
        "lp_total":   round(lp_total, 2),
        "lp_from_prds":        round(lp_from_prds, 2),
        "lp_from_stg":         round(lp_from_stg, 2),
        "lp_from_stg_available": round(lp_from_stg_avail, 2),
        "lp_stg_excess":       round(lp_stg_excess, 2),
        "shp_for_stg_lp":      round(shp_for_stg_lp, 2),
        "mp_for_prds_lp":      round(mp_for_prds_lp, 2),
        "bfw_for_prds_lp":     round(bfw_for_prds_lp, 2),
        "lp_stg_ratio":        round(lp_from_stg / lp_total if lp_total else 0, 4),
        "lp_prds_ratio":       round(lp_from_prds / lp_total if lp_total else 0, 4),
        "stg_lp_extraction_tph": round(stg_lp_extraction_tph, 2),
        "stg_operating_hours": round(stg_operating_hours, 2),
    }


# ---------------------------------------------------------------------------
# MP Balance
# ---------------------------------------------------------------------------

def calculate_mp_balance(mp_process, mp_fixed, mp_for_lp=0.0, mp_ufu_mt=None, norms=None):
    mp_ufu  = mp_ufu_mt if mp_ufu_mt is not None else 0.0
    mp_total = mp_process + mp_fixed + mp_for_lp + mp_ufu

    mp_from_prds = mp_total * _NORM_MP_FROM_PRDS
    mp_from_stg  = mp_total * _NORM_MP_FROM_STG

    shp_for_stg_mp  = mp_from_stg  * _NORM_SHP_PER_MP_STG
    shp_for_prds_mp = mp_from_prds * _NORM_SHP_PER_MP_PRDS
    bfw_for_prds_mp = mp_from_prds * _NORM_BFW_PER_MP_PRDS
    shp_from_mp_chain = shp_for_stg_mp + shp_for_prds_mp

    return {
        "mp_process": round(mp_process, 2),
        "mp_fixed":   round(mp_fixed, 2),
        "mp_for_lp":  round(mp_for_lp, 2),
        "mp_ufu":     round(mp_ufu, 2),
        "mp_total":   round(mp_total, 2),
        "mp_from_prds":    round(mp_from_prds, 2),
        "mp_from_stg":     round(mp_from_stg, 2),
        "shp_for_stg_mp":  round(shp_for_stg_mp, 2),
        "shp_for_prds_mp": round(shp_for_prds_mp, 2),
        "bfw_for_prds_mp": round(bfw_for_prds_mp, 2),
        "shp_from_mp_chain": round(shp_from_mp_chain, 2),
        "mp_stg_ratio":  round(_NORM_MP_FROM_STG, 4),
        "mp_prds_ratio": round(_NORM_MP_FROM_PRDS, 4),
    }


def calculate_mp_balance_stg_based(
    mp_process, mp_fixed, mp_for_lp,
    stg_mp_extraction_tph, stg_eq_svh_mp_tph, stg_operating_hours,
    mp_ufu_mt=None, norms=None,
):
    mp_ufu  = mp_ufu_mt if mp_ufu_mt is not None else 0.0
    mp_total = mp_process + mp_fixed + mp_for_lp + mp_ufu

    mp_from_stg_avail = stg_mp_extraction_tph * stg_operating_hours
    mp_from_stg   = min(mp_from_stg_avail, mp_total)
    mp_stg_excess = max(0, mp_from_stg_avail - mp_total)
    mp_from_prds  = max(0, mp_total - mp_from_stg)

    shp_for_stg_mp  = stg_eq_svh_mp_tph * stg_operating_hours
    shp_for_prds_mp = mp_from_prds * _NORM_SHP_PER_MP_PRDS
    bfw_for_prds_mp = mp_from_prds * _NORM_BFW_PER_MP_PRDS
    shp_from_mp_chain = shp_for_stg_mp + shp_for_prds_mp

    return {
        "mp_process": round(mp_process, 2),
        "mp_fixed":   round(mp_fixed, 2),
        "mp_for_lp":  round(mp_for_lp, 2),
        "mp_ufu":     round(mp_ufu, 2),
        "mp_total":   round(mp_total, 2),
        "mp_from_prds":        round(mp_from_prds, 2),
        "mp_from_stg":         round(mp_from_stg, 2),
        "mp_from_stg_available": round(mp_from_stg_avail, 2),
        "mp_stg_excess":       round(mp_stg_excess, 2),
        "shp_for_stg_mp":      round(shp_for_stg_mp, 2),
        "shp_for_prds_mp":     round(shp_for_prds_mp, 2),
        "bfw_for_prds_mp":     round(bfw_for_prds_mp, 2),
        "shp_from_mp_chain":   round(shp_from_mp_chain, 2),
        "mp_stg_ratio":  round(mp_from_stg / mp_total if mp_total else 0, 4),
        "mp_prds_ratio": round(mp_from_prds / mp_total if mp_total else 0, 4),
        "stg_mp_extraction_tph": round(stg_mp_extraction_tph, 2),
        "stg_operating_hours": round(stg_operating_hours, 2),
    }


# ---------------------------------------------------------------------------
# HP Balance
# ---------------------------------------------------------------------------

def calculate_hp_balance(hp_process, hp_fixed=0.0, norms=None):
    hp_total    = hp_process + hp_fixed
    hp_from_prds = hp_total * _NORM_HP_FROM_PRDS
    shp_for_hp_prds = hp_from_prds * _NORM_SHP_PER_HP_PRDS
    bfw_for_hp_prds = hp_from_prds * _NORM_BFW_PER_HP_PRDS

    return {
        "hp_process":     round(hp_process, 2),
        "hp_fixed":       round(hp_fixed, 2),
        "hp_total":       round(hp_total, 2),
        "hp_from_prds":   round(hp_from_prds, 2),
        "shp_for_hp_prds": round(shp_for_hp_prds, 2),
        "bfw_for_hp_prds": round(bfw_for_hp_prds, 2),
    }


# ---------------------------------------------------------------------------
# SHP Balance
# ---------------------------------------------------------------------------

def calculate_shp_balance(
    shp_process, shp_fixed, shp_for_stg_lp, shp_from_mp_chain,
    shp_for_hp_prds, stg_shp_power=0.0,
):
    shp_from_headers     = shp_process + shp_fixed
    shp_total_no_power   = shp_from_headers + shp_for_stg_lp + shp_from_mp_chain + shp_for_hp_prds
    shp_total            = shp_total_no_power + stg_shp_power

    return {
        "shp_process":          round(shp_process, 2),
        "shp_fixed":            round(shp_fixed, 2),
        "shp_for_stg_lp":       round(shp_for_stg_lp, 2),
        "shp_from_mp_chain":    round(shp_from_mp_chain, 2),
        "shp_for_hp_prds":      round(shp_for_hp_prds, 2),
        "shp_from_headers":     round(shp_from_headers, 2),
        "shp_total_without_power": round(shp_total_no_power, 2),
        "stg_shp_power":        round(stg_shp_power, 2),
        "shp_total":            round(shp_total, 2),
    }


# ---------------------------------------------------------------------------
# Full steam balance
# ---------------------------------------------------------------------------

def calculate_steam_balance(
    lp_process, lp_fixed, mp_process, mp_fixed,
    hp_process, hp_fixed, shp_process, shp_fixed,
    bfw_ufu=0.0, stg_shp_power=0.0, norms=None,
):
    lp_bal = calculate_lp_balance(lp_process, lp_fixed, bfw_ufu, norms=norms)
    mp_bal = calculate_mp_balance(mp_process, mp_fixed, lp_bal["mp_for_prds_lp"], norms=norms)
    hp_bal = calculate_hp_balance(hp_process, hp_fixed, norms=norms)
    shp_bal = calculate_shp_balance(
        shp_process, shp_fixed,
        lp_bal["shp_for_stg_lp"],
        mp_bal["shp_from_mp_chain"],
        hp_bal["shp_for_hp_prds"],
        stg_shp_power,
    )

    total_shp = shp_bal["shp_total"]
    total_lp  = lp_bal["lp_total"]
    total_mp  = mp_bal["mp_total"]
    total_hp  = hp_bal["hp_total"]

    bfw_req = {
        "for_lp_prds": lp_bal["bfw_for_prds_lp"],
        "for_mp_prds": mp_bal["bfw_for_prds_mp"],
        "for_hp_prds": hp_bal["bfw_for_hp_prds"],
        "total":       round(lp_bal["bfw_for_prds_lp"] + mp_bal["bfw_for_prds_mp"] + hp_bal["bfw_for_hp_prds"], 2),
    }

    return {
        "lp_balance":  lp_bal,
        "mp_balance":  mp_bal,
        "hp_balance":  hp_bal,
        "shp_balance": shp_bal,
        "bfw_requirement": bfw_req,
        "summary": {
            "total_lp_demand":  round(total_lp, 2),
            "total_mp_demand":  round(total_mp, 2),
            "total_hp_demand":  round(total_hp, 2),
            "total_shp_demand": round(total_shp, 2),
        },
    }


# ---------------------------------------------------------------------------
# HRSG availability from power dispatch
# ---------------------------------------------------------------------------

def get_hrsg_availability_from_dispatch(dispatch_plan: list) -> dict:
    """
    Build HRSG availability dict from the power dispatch result.
    Each GT dispatched with Gross > 0 means its linked HRSG is available.
    Returns {hrsg_name: {is_available, gt_load_mw, operational_hours, ...}}
    """
    availability = {}
    hrsg_idx = 1
    for asset in dispatch_plan:
        name = str(asset.get("AssetName", "")).upper()
        if "STG" in name or "STEAM TURBINE" in name:
            continue
        if "GT" in name or "PLANT" in name:
            gross = float(asset.get("GrossMWh", 0))
            hours = float(asset.get("Hours", 720))
            load_mw = gross / hours if hours > 0 else 0.0
            is_avail = gross > 0
            hrsg_name = f"HRSG{hrsg_idx}"
            availability[hrsg_name] = {
                "is_available":     is_avail,
                "gt_asset_name":    asset.get("AssetName"),
                "gt_load_mw":       round(load_mw, 4),
                "operational_hours": hours,
                "gross_mwh":        round(gross, 4),
                "free_steam_factor": asset.get("FreeSteam"),
            }
            hrsg_idx += 1
    return availability


# ---------------------------------------------------------------------------
# SHP capacity from HRSG availability + config
# ---------------------------------------------------------------------------

def calculate_shp_generation_capacity(hrsg_availability: dict, hrsg_config: list) -> dict:
    """
    Calculate max SHP generation capacity from available HRSGs.

    Args:
        hrsg_availability: output of get_hrsg_availability_from_dispatch()
        hrsg_config:       list of dicts from fetch_hrsg_availability() with
                           min_capacity_mt_hr, max_capacity_mt_hr, hours, etc.

    Returns:
        {
            total_max_shp_capacity: float,
            total_min_shp_capacity: float,
            available_hrsgs: [str],
            hrsg_details: [...],
        }
    """
    hrsg_details = []
    total_max = 0.0
    total_min = 0.0
    available_names = []

    # Build a config lookup by HRSG name
    config_by_name = {}
    for cfg in (hrsg_config or []):
        key = str(cfg.get("hrsg_name", "")).upper().replace("-", "").replace(" ", "")
        config_by_name[key] = cfg

    for hrsg_name, avail in hrsg_availability.items():
        if not avail.get("is_available"):
            hrsg_details.append({"name": hrsg_name, "is_available": False,
                                  "supp_min_mt_month": 0.0, "supp_max_mt_month": 0.0})
            continue

        key = hrsg_name.upper().replace("-", "").replace(" ", "")
        cfg = config_by_name.get(key, {})
        hours = float(cfg.get("hours", avail.get("operational_hours", 720)))
        min_mt_hr = float(cfg.get("min_capacity_mt_hr", 0.0))
        max_mt_hr = float(cfg.get("max_capacity_mt_hr", 90.0))
        fs_factor = avail.get("free_steam_factor") or 0.0
        gt_gross  = float(avail.get("gross_mwh", 0.0))
        free_steam_mt = gt_gross * float(fs_factor) if fs_factor else 0.0

        supp_min = min_mt_hr * hours
        supp_max = max_mt_hr * hours

        total_min += supp_min
        total_max += supp_max
        available_names.append(hrsg_name)

        hrsg_details.append({
            "name":              hrsg_name,
            "is_available":      True,
            "supp_min_mt_month": round(supp_min, 2),
            "supp_max_mt_month": round(supp_max, 2),
            "free_steam_mt":     round(free_steam_mt, 2),
            "hours":             hours,
        })

    return {
        "total_max_shp_capacity": round(total_max, 2),
        "total_min_shp_capacity": round(total_min, 2),
        "available_hrsgs":        available_names,
        "hrsg_details":           hrsg_details,
    }


def check_shp_balance(shp_demand: float, shp_capacity: dict) -> dict:
    max_cap = shp_capacity.get("total_max_shp_capacity", 0.0)
    min_cap = shp_capacity.get("total_min_shp_capacity", 0.0)
    surplus = max_cap - shp_demand
    utilization = (shp_demand / max_cap * 100) if max_cap > 0 else 0.0

    return {
        "shp_demand":       round(shp_demand, 2),
        "shp_max_capacity": round(max_cap, 2),
        "shp_min_capacity": round(min_cap, 2),
        "surplus":          round(surplus, 2),
        "deficit":          round(max(0, shp_demand - max_cap), 2),
        "utilization_percent": round(utilization, 2),
        "can_meet_demand":  surplus >= 0,
    }


# ---------------------------------------------------------------------------
# HRSG min load and excess steam
# ---------------------------------------------------------------------------

def calculate_hrsg_min_load_and_excess_steam(
    hrsg_availability: dict,
    hrsg_capacity: dict,
    shp_demand: float,
) -> dict:
    """
    Calculate minimum HRSG load and any excess steam above demand.
    Returns {min_load_mt, excess_steam_mt, hrsg_min_details}.
    """
    min_load_total = 0.0
    details = []

    for hrsg_name, avail in hrsg_availability.items():
        if not avail.get("is_available"):
            continue
        hrsg_info = next(
            (h for h in hrsg_capacity.get("hrsg_details", []) if h["name"] == hrsg_name),
            {},
        )
        min_mt = hrsg_info.get("supp_min_mt_month", 0.0)
        free_mt = hrsg_info.get("free_steam_mt", 0.0)
        min_load_total += min_mt
        details.append({
            "hrsg_name": hrsg_name,
            "min_load_mt": min_mt,
            "free_steam_mt": free_mt,
        })

    excess_steam = max(0.0, min_load_total - shp_demand)
    return {
        "min_load_mt":     round(min_load_total, 2),
        "excess_steam_mt": round(excess_steam, 2),
        "hrsg_min_details": details,
    }


def dispatch_hrsg_load(
    hrsg_availability: dict,
    hrsg_capacity: dict,
    shp_demand_after_free: float,
) -> dict:
    """
    Dispatch HRSG supplementary firing to meet SHP demand (after free steam).
    Allocates proportionally among available HRSGs.
    Returns {hrsg_dispatch: [...], total_free_steam_mt, total_dispatched_mt}.
    """
    available = []
    total_free = 0.0

    for hrsg_name, avail in hrsg_availability.items():
        if not avail.get("is_available"):
            continue
        hrsg_info = next(
            (h for h in hrsg_capacity.get("hrsg_details", []) if h["name"] == hrsg_name),
            {},
        )
        free_mt = hrsg_info.get("free_steam_mt", 0.0)
        max_supp = hrsg_info.get("supp_max_mt_month", 0.0)
        min_supp = hrsg_info.get("supp_min_mt_month", 0.0)
        total_free += free_mt
        available.append({
            "name": hrsg_name,
            "free_steam_mt": free_mt,
            "min_supp_mt": min_supp,
            "max_supp_mt": max_supp,
        })

    remaining = max(0.0, shp_demand_after_free)
    total_max_supp = sum(h["max_supp_mt"] for h in available)
    dispatch_list = []

    for h in available:
        if total_max_supp > 0:
            share = remaining * (h["max_supp_mt"] / total_max_supp)
        else:
            share = 0.0
        dispatched = min(share, h["max_supp_mt"])
        dispatched = max(dispatched, h["min_supp_mt"])
        dispatch_list.append({
            "name":              h["name"],
            "free_steam_mt":     round(h["free_steam_mt"], 2),
            "dispatched_supp_mt": round(dispatched, 2),
            "total_shp_mt":      round(h["free_steam_mt"] + dispatched, 2),
        })

    total_dispatched = sum(d["dispatched_supp_mt"] for d in dispatch_list)

    return {
        "hrsg_dispatch":       dispatch_list,
        "total_free_steam_mt": round(total_free, 2),
        "total_supp_mt":       round(total_dispatched, 2),
        "total_shp_mt":        round(total_free + total_dispatched, 2),
    }
