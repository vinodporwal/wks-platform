"""
JMD USD Iteration Engine
========================
Ported from PPPython-script/services/iteration_service.py.

Runs the power-steam convergence loop for a JMD plant/month:
  1. Dispatch power (GT/STG) to meet demand
  2. Calculate steam balance (LP → MP → HP → SHP chain)
  3. Check SHP capacity from HRSGs
  4. If SHP short → use supplementary firing
  5. If still short → reduce STG, add import
  6. Recalculate U4U power from utilities
  7. Iterate until total power demand converges (tolerance 0.02 MWh)

Differences from PPPython-script:
  - plant_id passed to all dispatch calls
  - HRSG config from hrsg_avail (pre-fetched, includes dummy fallback)
  - Norms from pre-fetched list, not separate DB calls
  - Asset data uses CPPPowerAssetPriority + CPPAssetOperationalHours
"""

import logging

from engine.power_dispatch import distribute_by_priority
from engine.steam_balance import (
    calculate_steam_balance,
    calculate_lp_balance_stg_based,
    calculate_mp_balance_stg_based,
    get_hrsg_availability_from_dispatch,
    calculate_shp_generation_capacity,
    check_shp_balance,
    calculate_hrsg_min_load_and_excess_steam,
    dispatch_hrsg_load,
    STEAM_TO_POWER_MT_PER_MWH,
)
from database.queries import get_stg_extraction_for_load

logger = logging.getLogger(__name__)

USD_ITERATION_LIMIT        = 50
USD_TOLERANCE              = 0.02   # MWh
EXCESS_STEAM_THRESHOLD_MT  = 1.0
STALL_LIMIT                = 3


# ---------------------------------------------------------------------------
# Utility norm lookup helpers
# ---------------------------------------------------------------------------

def _norm(norms, utility, material, default=0.0):
    for n in (norms or []):
        if n.get("UtilityName") == utility and n.get("MaterialName") == material:
            v = n.get("NormValue")
            if v is not None:
                return float(v)
    return default


def _u4u_power_mwh(norms, gt_gross_mwh_map: dict, stg_gross_mwh: float,
                   bfw_m3: float, dm_m3: float, cw1_km3: float,
                   cw2_km3: float, air_nm3: float,
                   oxygen_mt: float = 0.0, effluent_m3: float = 0.0) -> float:
    """
    Calculate total utility-for-utility (U4U) auxiliary power consumption (MWh).
    Uses norms from NormsMonthDetail where available, else falls back to
    PPPython-script constants.
    """
    total_kwh = 0.0

    # BFW power
    bfw_norm = _norm(norms, "BFW", "Power_Dis", 9.5)
    total_kwh += bfw_m3 * bfw_norm

    # DM Water power
    dm_norm = _norm(norms, "DM Water", "Power_Dis", 1.21)
    total_kwh += dm_m3 * dm_norm

    # CW1 power
    cw1_norm = _norm(norms, "Cooling Water 1", "Power_Dis", 245.0)
    total_kwh += cw1_km3 * cw1_norm

    # CW2 power
    cw2_norm = _norm(norms, "Cooling Water 2", "Power_Dis", 250.0)
    total_kwh += cw2_km3 * cw2_norm

    # Compressed Air power
    air_norm = _norm(norms, "COMPRESSED AIR", "Power_Dis", 0.165)
    total_kwh += air_nm3 * air_norm

    # Oxygen power
    oxy_norm = _norm(norms, "Oxygen", "Power_Dis", 968.65)
    total_kwh += oxygen_mt * oxy_norm

    # Effluent power
    eff_norm = _norm(norms, "Effluent Treated", "Power_Dis", 3.54)
    total_kwh += effluent_m3 * eff_norm

    return total_kwh / 1000.0  # KWH → MWh


def _u4u_bfw_m3(norms, shp_hrsg_mt: float, hp_prds_mt: float,
                mp_prds_mt: float, lp_prds_mt: float) -> float:
    """Total BFW (M3) required for steam generation."""
    norm = _norm(norms, "BFW", "SHP Steam_Dis", 1.0)
    return (shp_hrsg_mt + hp_prds_mt + mp_prds_mt + lp_prds_mt) * norm


def _u4u_dm_m3(norms, bfw_m3: float) -> float:
    norm = _norm(norms, "DM Water", "BFW_Dis", 0.86)
    return bfw_m3 * norm


def _u4u_cw2_km3(norms, stg_gross_mwh: float, gt_total_mwh: float) -> float:
    # Default 0.0: without real norms from DB, CW2 u4u cannot be estimated reliably
    # and non-zero defaults cause oscillation. Real values come from NormsMonthDetail.
    norm_stg = _norm(norms, "Cooling Water 2", "POWERGEN_STG", 0.0)
    norm_gt  = _norm(norms, "Cooling Water 2", "POWERGEN_GT",  0.0)
    return stg_gross_mwh * norm_stg + gt_total_mwh * norm_gt


def _u4u_air_nm3(norms, shp_hrsg_mt: float) -> float:
    norm = _norm(norms, "COMPRESSED AIR", "SHP Steam_Dis", 0.0)
    return shp_hrsg_mt * norm


# ---------------------------------------------------------------------------
# Main USD iteration
# ---------------------------------------------------------------------------

def usd_iterate(
    plant_id: str,
    month: int,
    year: int,
    norms: list,
    hrsg_avail: list,
    stg_extraction_df,
    lp_process: float,
    lp_fixed: float,
    mp_process: float,
    mp_fixed: float,
    hp_process: float,
    hp_fixed: float,
    shp_process: float,
    shp_fixed: float,
    bfw_ufu: float = 0.0,
    export_available: bool = False,
    dm_process: float = 0.0,
    dm_fixed: float = 0.0,
    air_process: float = 0.0,
    air_fixed: float = 0.0,
    cw1_process: float = 0.0,
    cw2_process: float = 0.0,
    oxygen_mt: float = 0.0,
    effluent_m3: float = 0.0,
) -> dict:
    """
    Run USD power-steam convergence iteration for a JMD plant/month.

    Returns a result dict matching PPPython-script usd_iterate output:
        {
            converged, iterations_used, error_type, message,
            power_result, final_dispatch,
            final_steam_balance, final_shp_capacity, final_shp_balance,
            hrsg_dispatch, hrsg_min_load,
            final_lp_balance, final_mp_balance,
            stg_extraction,
            supplementary_firing_mt,
        }
    """
    stg_shp_norm = _norm(norms, "POWERGEN", "SHP Steam_Dis", 0.0036)

    u4u_power_mwh = 0.0  # starts at 0, grows each iteration

    # Track STG limits
    stg_max_mwh = None
    stg_min_override_mwh = None
    gt_reduction_mwh = 0.0

    stall_counter = 0
    prev_u4u = None
    prev2_u4u = None  # two iterations ago (period-2 oscillation detection)
    prev_stg = None
    stall_forced = False
    actual_iters = 0

    for it in range(1, USD_ITERATION_LIMIT + 1):
        actual_iters = it
        # ── Step 1: Power dispatch ────────────────────────────────────────────
        power_result = distribute_by_priority(
            plant_id=plant_id,
            month=month,
            year=year,
            norms=norms,
            additional_demand_mwh=u4u_power_mwh,
            stg_max_mwh=stg_max_mwh,
            stg_min_override_mwh=stg_min_override_mwh,
            gt_reduction_mwh=gt_reduction_mwh,
            stg_extraction_df=stg_extraction_df,
            verbose=False,
        )

        if power_result.get("insufficientCapacity") or power_result.get("error"):
            return {
                "converged": False,
                "error_type": "INSUFFICIENT_CAPACITY",
                "message": power_result.get("message", "Insufficient capacity"),
                "iterations_used": it,
                "power_result": power_result,
                "final_dispatch": [],
            }

        dispatch = power_result.get("dispatchPlan", [])

        # Extract GT/STG generation
        gt_gross_map = {}
        stg_gross_mwh = 0.0
        stg_hours = 0.0
        stg_shp_req = 0.0
        stg_extraction = {}

        for asset in dispatch:
            name = str(asset.get("AssetName", "")).upper()
            gross = float(asset.get("GrossMWh", 0))
            hours = float(asset.get("Hours", 720))

            if "STG" in name or "STEAM TURBINE" in name:
                stg_gross_mwh = gross
                stg_hours = hours
                stg_shp_req = float(asset.get("STG_SHP_Required_MT") or 0)

                # Get extraction data for STG load
                stg_load_mw = gross / hours if hours > 0 else 0.0
                if stg_extraction_df is not None and not stg_extraction_df.empty and stg_load_mw > 0:
                    ext = get_stg_extraction_for_load(stg_load_mw, stg_extraction_df)
                    stg_extraction = ext
                    lp_ext_tph = ext.get("lp_extraction_tph", 0.0)
                    mp_ext_tph = ext.get("mp_extraction_tph", 0.0)
                    svh_lp_tph = ext.get("lp_extraction_tph", 0.0)  # physical flow
                    svh_mp_tph = ext.get("mp_extraction_tph", 0.0)
                else:
                    lp_ext_tph = mp_ext_tph = svh_lp_tph = svh_mp_tph = 0.0
            else:
                gt_gross_map[name] = gross

        gt_total_mwh = sum(gt_gross_map.values())

        # ── Step 2: Steam balance ─────────────────────────────────────────────
        # Use STG-extraction-based LP/MP when available, else fixed ratios
        if stg_extraction and stg_hours > 0:
            from engine.steam_balance import (
                calculate_lp_balance_stg_based as _lp_stg,
                calculate_mp_balance_stg_based as _mp_stg,
                calculate_hp_balance,
                calculate_shp_balance,
            )
            lp_bal = _lp_stg(
                lp_process, lp_fixed, bfw_ufu,
                lp_ext_tph, svh_lp_tph, stg_hours,
                norms=norms,
            )
            mp_bal = _mp_stg(
                mp_process, mp_fixed, lp_bal["mp_for_prds_lp"],
                mp_ext_tph, svh_mp_tph, stg_hours,
                norms=norms,
            )
            from engine.steam_balance import calculate_shp_balance as _shp
            hp_bal = calculate_hp_balance(hp_process, hp_fixed, norms=norms)
            shp_bal = _shp(
                shp_process, shp_fixed,
                lp_bal["shp_for_stg_lp"],
                mp_bal["shp_from_mp_chain"],
                hp_bal["shp_for_hp_prds"],
                stg_shp_req,
            )
        else:
            steam = calculate_steam_balance(
                lp_process, lp_fixed, mp_process, mp_fixed,
                hp_process, hp_fixed, shp_process, shp_fixed,
                bfw_ufu=bfw_ufu, stg_shp_power=stg_shp_req, norms=norms,
            )
            lp_bal  = steam["lp_balance"]
            mp_bal  = steam["mp_balance"]
            hp_bal  = steam["hp_balance"]
            shp_bal = steam["shp_balance"]

        shp_total_demand = shp_bal["shp_total"]

        # ── Step 3: HRSG capacity ─────────────────────────────────────────────
        hrsg_dispatch_avail = get_hrsg_availability_from_dispatch(dispatch)
        hrsg_capacity = calculate_shp_generation_capacity(hrsg_dispatch_avail, hrsg_avail)
        shp_check = check_shp_balance(shp_total_demand, hrsg_capacity)

        # ── Step 4: HRSG dispatch ─────────────────────────────────────────────
        total_free_steam = sum(
            h.get("free_steam_mt", 0) for h in hrsg_capacity.get("hrsg_details", [])
            if h.get("is_available")
        )
        shp_after_free = max(0.0, shp_total_demand - total_free_steam)
        hrsg_dispatch_result = dispatch_hrsg_load(hrsg_dispatch_avail, hrsg_capacity, shp_after_free)

        # Check if SHP is met
        total_shp_supply = hrsg_dispatch_result["total_shp_mt"]
        excess_steam_mt = max(0.0, total_shp_supply - shp_total_demand)
        shp_deficit_mt  = max(0.0, shp_total_demand - total_shp_supply)

        supplementary_firing_mt = hrsg_dispatch_result["total_supp_mt"]

        # ── Step 5: Handle excess steam (push into STG) ───────────────────────
        if excess_steam_mt > EXCESS_STEAM_THRESHOLD_MT and stg_gross_mwh >= 0:
            extra_mwh = excess_steam_mt / STEAM_TO_POWER_MT_PER_MWH
            stg_min_override_mwh = (stg_gross_mwh + extra_mwh) if stg_gross_mwh > 0 else extra_mwh
            gt_reduction_mwh = extra_mwh
        else:
            stg_min_override_mwh = None
            gt_reduction_mwh = 0.0

        # ── Step 6: U4U power ─────────────────────────────────────────────────
        shp_from_hrsg = sum(
            d.get("total_shp_mt", 0) for d in hrsg_dispatch_result.get("hrsg_dispatch", [])
        )
        hp_from_prds = hp_bal["hp_from_prds"]
        mp_from_prds = mp_bal["mp_from_prds"]
        lp_from_prds = lp_bal["lp_from_prds"]

        bfw_m3  = _u4u_bfw_m3(norms, shp_from_hrsg, hp_from_prds, mp_from_prds, lp_from_prds)
        dm_m3   = dm_process + dm_fixed + _u4u_dm_m3(norms, bfw_m3)
        cw2_km3 = cw2_process + _u4u_cw2_km3(norms, stg_gross_mwh, gt_total_mwh)
        air_nm3 = air_process + air_fixed + _u4u_air_nm3(norms, shp_from_hrsg)

        new_u4u = _u4u_power_mwh(
            norms, gt_gross_map, stg_gross_mwh,
            bfw_m3, dm_m3, cw1_process, cw2_km3, air_nm3,
            oxygen_mt, effluent_m3,
        )

        # ── Step 7: Convergence check ─────────────────────────────────────────
        delta = abs(new_u4u - u4u_power_mwh)
        logger.debug(
            "  [USD] iter=%d  u4u=%.3f  delta=%.4f  stg=%.1f  shp_deficit=%.1f",
            it, new_u4u, delta, stg_gross_mwh, shp_deficit_mt,
        )

        # Stall detection — catches both consecutive stall and period-2 oscillation
        consecutive_stall = prev_u4u is not None and abs(new_u4u - prev_u4u) < 0.02
        period2_stall = prev2_u4u is not None and abs(new_u4u - prev2_u4u) < 0.02
        if consecutive_stall or period2_stall:
            stall_counter += 1
        else:
            stall_counter = 0
        prev2_u4u = prev_u4u
        prev_u4u = new_u4u

        u4u_power_mwh = new_u4u

        if delta <= USD_TOLERANCE and shp_deficit_mt < 1.0:
            # Converged
            final_steam = {
                "lp_balance":  lp_bal,
                "mp_balance":  mp_bal,
                "hp_balance":  hp_bal,
                "shp_balance": shp_bal,
                "summary": {
                    "total_lp_demand":  lp_bal["lp_total"],
                    "total_mp_demand":  mp_bal["mp_total"],
                    "total_hp_demand":  hp_bal["hp_total"],
                    "total_shp_demand": shp_bal["shp_total"],
                },
            }
            hrsg_min_load = calculate_hrsg_min_load_and_excess_steam(
                hrsg_dispatch_avail, hrsg_capacity, shp_total_demand,
            )

            return {
                "converged":             True,
                "iterations_used":       it,
                "tolerance_achieved":    round(delta, 6),
                "power_result":          power_result,
                "final_dispatch":        dispatch,
                "final_steam_balance":   final_steam,
                "final_shp_capacity":    hrsg_capacity,
                "final_shp_balance":     shp_check,
                "hrsg_dispatch":         hrsg_dispatch_result,
                "hrsg_min_load":         hrsg_min_load,
                "final_lp_balance":      lp_bal,
                "final_mp_balance":      mp_bal,
                "stg_extraction":        stg_extraction,
                "supplementary_firing_mt": supplementary_firing_mt,
                "u4u_power_mwh":         round(u4u_power_mwh, 4),
            }

        if stall_counter >= STALL_LIMIT:
            # Period-2 oscillation: average the two states for a stable result
            if prev2_u4u is not None and period2_stall:
                u4u_power_mwh = (new_u4u + (prev2_u4u or new_u4u)) / 2.0
                logger.debug("  [USD] Period-2 stall at iter %d — averaging u4u to %.3f", it, u4u_power_mwh)
            else:
                logger.debug("  [USD] Stalled at iter %d — forcing convergence", it)
            stall_forced = True
            break

    # ── Did not converge ──────────────────────────────────────────────────────
    final_steam = {
        "lp_balance":  lp_bal,
        "mp_balance":  mp_bal,
        "hp_balance":  hp_bal,
        "shp_balance": shp_bal,
        "summary": {
            "total_lp_demand":  lp_bal["lp_total"],
            "total_mp_demand":  mp_bal["mp_total"],
            "total_hp_demand":  hp_bal["hp_total"],
            "total_shp_demand": shp_bal["shp_total"],
        },
    }
    hrsg_min_load = calculate_hrsg_min_load_and_excess_steam(
        hrsg_dispatch_avail, hrsg_capacity, shp_total_demand,
    )

    return {
        "converged":             stall_forced,
        "error_type":            None if stall_forced else "NO_CONVERGENCE",
        "message":               (
            f"Stall-converged at iter {actual_iters} (period-2 oscillation)"
            if stall_forced else
            f"Did not converge after {USD_ITERATION_LIMIT} iterations"
        ),
        "iterations_used":       actual_iters,
        "tolerance_achieved":    round(delta, 6),
        "power_result":          power_result,
        "final_dispatch":        dispatch,
        "final_steam_balance":   final_steam,
        "final_shp_capacity":    hrsg_capacity,
        "final_shp_balance":     shp_check,
        "hrsg_dispatch":         hrsg_dispatch_result,
        "hrsg_min_load":         hrsg_min_load,
        "final_lp_balance":      lp_bal,
        "final_mp_balance":      mp_bal,
        "stg_extraction":        stg_extraction,
        "supplementary_firing_mt": supplementary_firing_mt,
        "u4u_power_mwh":         round(u4u_power_mwh, 4),
    }
