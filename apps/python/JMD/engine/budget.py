"""
JMD Budget Calculation — Top-Level Engine
==========================================
Mirrors PPPython-script/services/budget_service.py:calculate_budget_with_iteration()

Entry point:
    from engine.budget import calculate_budget

Works for all 5 JMD plants — plant_id is passed through every call.
All data is pre-fetched by calculator.py and passed in; no DB calls here.
"""

import logging

from engine.usd_iteration import usd_iterate

logger = logging.getLogger(__name__)


def calculate_budget(
    plant_id: str,
    plant_name: str,
    month: int,
    year: int,
    norms: list,
    hrsg_avail: list,
    stg_extraction_df,
    demands: dict,
    save_to_db: bool = False,
) -> dict:
    """
    Run full budget calculation (power dispatch + steam + USD iteration).

    Args:
        plant_id:         CPP plant UUID
        plant_name:       display name for logging
        month, year:      financial period
        norms:            pre-fetched from fetch_norms()
        hrsg_avail:       pre-fetched from fetch_hrsg_availability()
        stg_extraction_df: pre-fetched from fetch_stg_extraction_lookup()
        demands:          merged dict from fetch_process_demands + fetch_fixed_consumption
        save_to_db:       reserved for future norms save

    Returns:
        dict with:
            overall_success, converged, iterations_used,
            power_result, final_dispatch,
            steam_result, stg_extraction,
            shp_capacity, shp_balance,
            hrsg_dispatch, hrsg_min_load,
            utility_consumption,
            supplementary_firing_mt,
            errors, message
    """
    # Extract demand inputs
    lp_process  = float(demands.get("lp_process",  0.0))
    lp_fixed    = float(demands.get("lp_fixed",    0.0))
    mp_process  = float(demands.get("mp_process",  0.0))
    mp_fixed    = float(demands.get("mp_fixed",    0.0))
    hp_process  = float(demands.get("hp_process",  0.0))
    hp_fixed    = float(demands.get("hp_fixed",    0.0))
    shp_process = float(demands.get("shp_process", 0.0))
    shp_fixed   = float(demands.get("shp_fixed",   0.0))
    air_process = float(demands.get("air_process", 0.0))
    dm_process  = float(demands.get("dm_process",  0.0))
    cw1_process = float(demands.get("cw1_process", 0.0))
    cw2_process = float(demands.get("cw2_process", 0.0))
    oxygen_mt   = float(demands.get("oxygen_process", 0.0))
    effluent_m3 = float(demands.get("effluent_process", 0.0))

    logger.info(
        "  [BUDGET] %s  %d/%d  LP=%.0f  MP=%.0f  HP=%.0f  SHP=%.0f",
        plant_name, month, year, lp_process + lp_fixed,
        mp_process + mp_fixed, hp_process + hp_fixed, shp_process + shp_fixed,
    )

    # Run USD iteration
    usd = usd_iterate(
        plant_id=plant_id,
        month=month,
        year=year,
        norms=norms,
        hrsg_avail=hrsg_avail,
        stg_extraction_df=stg_extraction_df,
        lp_process=lp_process,
        lp_fixed=lp_fixed,
        mp_process=mp_process,
        mp_fixed=mp_fixed,
        hp_process=hp_process,
        hp_fixed=hp_fixed,
        shp_process=shp_process,
        shp_fixed=shp_fixed,
        dm_process=dm_process,
        air_process=air_process,
        cw1_process=cw1_process,
        cw2_process=cw2_process,
        oxygen_mt=oxygen_mt,
        effluent_m3=effluent_m3,
    )

    converged = usd.get("converged", False)

    # Build utility consumption summary
    dispatch = usd.get("final_dispatch", [])
    hrsg_result = usd.get("hrsg_dispatch", {})
    steam = usd.get("final_steam_balance", {})

    gt_gross_map = {}
    stg_gross_mwh = 0.0
    shp_from_hrsg = 0.0

    for asset in dispatch:
        name = str(asset.get("AssetName", "")).upper()
        gross = float(asset.get("GrossMWh", 0))
        if "STG" in name or "STEAM TURBINE" in name:
            stg_gross_mwh = gross
        else:
            gt_gross_map[name] = gross

    for d in hrsg_result.get("hrsg_dispatch", []):
        shp_from_hrsg += float(d.get("total_shp_mt", 0))

    lp_bal = steam.get("lp_balance", {})
    mp_bal = steam.get("mp_balance", {})
    hp_bal = steam.get("hp_balance", {})

    utility_consumption = {
        "shp_from_hrsg_mt":  round(shp_from_hrsg, 2),
        "hp_from_prds_mt":   round(hp_bal.get("hp_from_prds", 0), 2),
        "mp_from_prds_mt":   round(mp_bal.get("mp_from_prds", 0), 2),
        "lp_from_prds_mt":   round(lp_bal.get("lp_from_prds", 0), 2),
        "lp_from_stg_mt":    round(lp_bal.get("lp_from_stg", 0), 2),
        "mp_from_stg_mt":    round(mp_bal.get("mp_from_stg", 0), 2),
        "stg_gross_mwh":     round(stg_gross_mwh, 2),
        "gt_gross_mwh":      {k: round(v, 2) for k, v in gt_gross_map.items()},
        "gt_total_mwh":      round(sum(gt_gross_map.values()), 2),
        "import_power_mwh":  round(usd.get("power_result", {}).get("mandatoryImportUsed", 0), 2),
        "air_process_nm3":   round(air_process, 2),
        "dm_process_m3":     round(dm_process, 2),
        "cw1_process_km3":   round(cw1_process, 2),
        "cw2_process_km3":   round(cw2_process, 2),
        "oxygen_mt":         round(oxygen_mt, 2),
        "effluent_m3":       round(effluent_m3, 2),
        "u4u_power_mwh":     round(usd.get("u4u_power_mwh", 0), 4),
    }

    errors = []
    if not converged:
        errors.append({
            "stage":      "USD_ITERATION",
            "error_type": usd.get("error_type", "NO_CONVERGENCE"),
            "message":    usd.get("message", ""),
        })

    return {
        "plant_id":              plant_id,
        "plant_name":            plant_name,
        "month":                 month,
        "year":                  year,
        "overall_success":       converged,
        "converged":             converged,
        "iterations_used":       usd.get("iterations_used", 0),
        "tolerance_achieved":    usd.get("tolerance_achieved", 0),
        "power_result":          usd.get("power_result"),
        "final_dispatch":        dispatch,
        "steam_result":          steam,
        "stg_extraction":        usd.get("stg_extraction"),
        "shp_capacity":          usd.get("final_shp_capacity"),
        "shp_balance":           usd.get("final_shp_balance"),
        "hrsg_dispatch":         hrsg_result,
        "hrsg_min_load":         usd.get("hrsg_min_load"),
        "final_lp_balance":      usd.get("final_lp_balance"),
        "final_mp_balance":      usd.get("final_mp_balance"),
        "utility_consumption":   utility_consumption,
        "supplementary_firing_mt": usd.get("supplementary_firing_mt", 0),
        "errors":                errors,
        "message":               "Converged" if converged else errors[0]["message"] if errors else "Unknown",
    }
