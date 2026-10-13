import logging
import os
from engine.dta_stg_config import get_dta_stg_config

logger = logging.getLogger(__name__)


def calculate_dta_stg_extraction(
    hp_extraction_tph: float,
    mp_extraction_tph: float,
    condensate_tph: float = None,
    config: dict = None,
) -> dict:
    """
    Compute the DTA STG operating point from the Digital AOP / Nitin Bande
    methodology.

    Args:
        hp_extraction_tph: HP bleed / extraction in TPH.
        mp_extraction_tph: MP bleed / extraction in TPH.
        condensate_tph:    Condensate flow in TPH. Defaults to config value.
        config:            DTA STG configuration dict. Loaded from
                           data/dta_stg_config.json if None.

    Returns:
        {
            "shp_inlet_tph",            # HHP
            "hp_extraction_tph",
            "mp_extraction_tph",
            "condensate_tph",
            "hp_hhp_equivalent_tph",
            "mp_hhp_equivalent_tph",
            "net_hhp_tph",
            "ssc_kg_kwh",
            "mw",
            "heat_rate_kcal_kwh",
            "hp_to_hhp_factor",
            "mp_to_hhp_factor",
        }
    """
    if config is None:
        config = get_dta_stg_config()

    if condensate_tph is None:
        condensate_tph = float(config["condensate_flow_tph"])

    hp = float(hp_extraction_tph)
    mp = float(mp_extraction_tph)
    cf = float(condensate_tph)

    hp_exergy = float(config["hp_exergy_factor"])
    mp_exergy = float(config["mp_exergy_factor"])
    cond_exergy = float(config["condensate_exergy_factor"])

    # Step 1: total SHP/HHP inlet (mass balance)
    hhp = hp + mp + cf

    # Step 2 & 3: exergy-based HHP equivalents
    hp_to_hhp_factor = hp_exergy / cond_exergy
    mp_to_hhp_factor = mp_exergy / cond_exergy
    hp_hhp_equivalent = hp * hp_to_hhp_factor
    mp_hhp_equivalent = mp * mp_to_hhp_factor

    # Step 4: net HHP available for power
    net_hhp = hhp - hp_hhp_equivalent - mp_hhp_equivalent

    # Step 5: SSC from Nitin Bande regression
    ssc = (
        float(config["ssc_intercept"])
        - float(config["ssc_hp_coefficient"]) * hp
        - float(config["ssc_mp_coefficient"]) * mp
        + float(config["ssc_condensate_coefficient"]) * cf
    )

    # Step 6: MW from physical relationship MW = Net HHP / SSC
    mw = net_hhp / ssc if ssc else 0.0

    # Step 7: heat rate (Digital AOP formula)
    hr = (
        hhp * float(config["heat_rate_hhp_factor"])
        - hp * float(config["heat_rate_hp_factor"])
        - mp * float(config["heat_rate_mp_factor"])
        - cf * float(config["heat_rate_condensate_factor"])
    ) / mw if mw else 0.0

    result = {
        "shp_inlet_tph": hhp,
        "hp_extraction_tph": hp,
        "mp_extraction_tph": mp,
        "condensate_tph": cf,
        "hp_hhp_equivalent_tph": hp_hhp_equivalent,
        "mp_hhp_equivalent_tph": mp_hhp_equivalent,
        "net_hhp_tph": net_hhp,
        "ssc_kg_kwh": ssc,
        "mw": mw,
        "heat_rate_kcal_kwh": hr,
        "hp_to_hhp_factor": hp_to_hhp_factor,
        "mp_to_hhp_factor": mp_to_hhp_factor,
    }

    return result


def log_stg_extraction(asset_name: str, calc: dict) -> None:
    """Log the STG calculation values exactly as requested."""
    logger.info("  STG calculation: %s", asset_name)
    logger.info("    HP Extraction      = %10.2f TPH", calc["hp_extraction_tph"])
    logger.info("    MP Extraction      = %10.2f TPH", calc["mp_extraction_tph"])
    logger.info("    Condensate         = %10.2f TPH", calc["condensate_tph"])
    logger.info("    HHP                = %10.2f TPH", calc["shp_inlet_tph"])
    logger.info("    HP HHP Equivalent  = %10.2f TPH", calc["hp_hhp_equivalent_tph"])
    logger.info("    MP HHP Equivalent  = %10.2f TPH", calc["mp_hhp_equivalent_tph"])
    logger.info("    Net HHP            = %10.2f TPH", calc["net_hhp_tph"])
    logger.info("    SSC                = %10.4f kg/kWh", calc["ssc_kg_kwh"])
    logger.info("    MW                 = %10.4f MW", calc["mw"])
    logger.info("    Heat Rate          = %10.2f kcal/kWh", calc["heat_rate_kcal_kwh"])


def get_default_dta_stg_calc() -> dict:
    """Convenience: compute the default DTA max-extraction operating point."""
    config = get_dta_stg_config()
    return calculate_dta_stg_extraction(
        config["max_hp_extraction_tph"],
        config["max_mp_extraction_tph"],
        config["condensate_flow_tph"],
        config,
    )


def calculate_dta_stg_case(case_cfg: dict, config: dict = None) -> dict:
    """
    Compute one AOP operating case for a DTA STG.

    Workbook semantics ("All STG's Norm calculation - Digital AOP.ods" > DTA STG):
    MW is the stated case output (input), SSC is derived as Net HHP / MW.
    This is the inverse of calculate_dta_stg_extraction(), which derives MW
    from the Nitin Bande SSC regression.

    Args:
        case_cfg: one entry of config["cases"] —
                  {id, hp_extraction_tph, mp_extraction_tph, condensate_tph,
                   hhp_tph, power_generation_mw}
        config:   DTA STG configuration dict (exergy + heat-rate factors).

    Returns the same dict shape as calculate_dta_stg_extraction() plus
    "case_id".
    """
    if config is None:
        config = get_dta_stg_config()

    hp = float(case_cfg["hp_extraction_tph"])
    mp = float(case_cfg["mp_extraction_tph"])
    cf = float(case_cfg["condensate_tph"])
    hhp = float(case_cfg.get("hhp_tph", hp + mp + cf))
    mw = float(case_cfg["power_generation_mw"])

    hp_to_hhp_factor = float(config["hp_exergy_factor"]) / float(config["condensate_exergy_factor"])
    mp_to_hhp_factor = float(config["mp_exergy_factor"]) / float(config["condensate_exergy_factor"])
    hp_hhp_equivalent = hp * hp_to_hhp_factor
    mp_hhp_equivalent = mp * mp_to_hhp_factor
    net_hhp = hhp - hp_hhp_equivalent - mp_hhp_equivalent

    ssc = net_hhp / mw if mw else 0.0
    hr = (
        hhp * float(config["heat_rate_hhp_factor"])
        - hp * float(config["heat_rate_hp_factor"])
        - mp * float(config["heat_rate_mp_factor"])
        - cf * float(config["heat_rate_condensate_factor"])
    ) / mw if mw else 0.0

    return {
        "case_id": case_cfg.get("id", ""),
        "shp_inlet_tph": hhp,
        "hp_extraction_tph": hp,
        "mp_extraction_tph": mp,
        "condensate_tph": cf,
        "hp_hhp_equivalent_tph": hp_hhp_equivalent,
        "mp_hhp_equivalent_tph": mp_hhp_equivalent,
        "net_hhp_tph": net_hhp,
        "ssc_kg_kwh": ssc,
        "mw": mw,
        "heat_rate_kcal_kwh": hr,
        "hp_to_hhp_factor": hp_to_hhp_factor,
        "mp_to_hhp_factor": mp_to_hhp_factor,
    }


def select_dta_stg_demand_case(hp_req_tph: float, mp_req_tph: float,
                               config: dict = None) -> dict:
    """
    Demand-driven case selection per the Digital AOP DTA STG sheet
    (STG_Extraction_Implementation_and_May_Validation_Plan §2.1).

    The case is chosen purely from the required HP/MP extraction thresholds —
    never from power output or which case yields the highest MW:
        HP > 100 & MP >= 70 → Case I   (eta 0.85)
        HP <= 100 & MP >= 70 → Case II  (eta 0.80)
        HP > 100 & MP <  70 → Case III (eta 0.80)
        HP <= 100 & MP <  70 → Case IV  (eta 0.75)
    Boundary: HP exactly 100 is LOW; MP exactly 70 is HIGH.

    Returns the matching calculated case dict plus "efficiency" and
    "case_num" (1-4), or None when the config has no matching case.
    """
    if config is None:
        config = get_dta_stg_config()

    sel = config.get("case_selection", {})
    hp_thr = float(sel.get("hp_high_threshold_tph", 100.0))
    mp_thr = float(sel.get("mp_high_threshold_tph", 70.0))
    eff_map = sel.get("efficiency", {"I": 0.85, "II": 0.80, "III": 0.80, "IV": 0.75})

    if hp_req_tph > hp_thr:
        case_id = "I" if mp_req_tph >= mp_thr else "III"
    else:
        case_id = "II" if mp_req_tph >= mp_thr else "IV"

    for c in get_dta_stg_cases(config):
        if c["case_id"] == case_id:
            out = dict(c)
            out["efficiency"] = float(eff_map.get(case_id, 0.0))
            out["case_num"] = {"I": 1, "II": 2, "III": 3, "IV": 4}[case_id]
            return out
    return None


def calculate_dta_stg_dispatch_point(
    hp_extraction_tph: float,
    mp_extraction_tph: float,
    condensate_tph: float = None,
    config: dict = None,
) -> dict:
    """
    Exergy-derived power for a DTA extraction STG at a required HP/MP duty.

    Baseline power (plan §2.4 — efficiency applied exactly once):
        P = [HP*(cond_ex - hp_ex) + MP*(cond_ex - mp_ex) + C*cond_ex] * eta

    The demand-selected case supplies eta (efficiency) and the reference SSC
    (Net HHP / stated MW of the case) used by callers to convert incremental
    power into additional condensing steam:  dS_TPH = dP_MW * SSC.

    Returns:
        {
            "case_id", "case_num", "efficiency", "ssc_kg_kwh",
            "baseline_power_mw",
            "hp_tph", "mp_tph", "condensate_tph",
            "hp_hhp_equivalent_tph", "mp_hhp_equivalent_tph",
            "equivalent_inlet_tph",   # HP_eq + MP_eq + C (exergy basis)
            "gross_inlet_tph",        # HP + MP + C (physical mass balance)
        }
    """
    if config is None:
        config = get_dta_stg_config()

    if condensate_tph is None:
        condensate_tph = float(config["condensate_flow_tph"])

    hp = float(hp_extraction_tph)
    mp = float(mp_extraction_tph)
    cf = float(condensate_tph)

    hp_ex = float(config["hp_exergy_factor"])
    mp_ex = float(config["mp_exergy_factor"])
    cond_ex = float(config["condensate_exergy_factor"])

    case = select_dta_stg_demand_case(hp, mp, config)
    if case is None:
        # No configured cases — fall back to un-derated exergy (eta = 1) and
        # the Nitin Bande SSC regression so a mis-config never zeroes power.
        logger.warning(
            "  [DTA STG] No demand case available; using eta=1.0 fallback")
        efficiency = 1.0
        case_id, case_num = "", 0
        ssc = (
            float(config["ssc_intercept"])
            - float(config["ssc_hp_coefficient"]) * hp
            - float(config["ssc_mp_coefficient"]) * mp
            + float(config["ssc_condensate_coefficient"]) * cf
        )
    else:
        efficiency = case["efficiency"]
        case_id = case["case_id"]
        case_num = case["case_num"]
        ssc = case["ssc_kg_kwh"]

    baseline_mw = (hp * (cond_ex - hp_ex)
                   + mp * (cond_ex - mp_ex)
                   + cf * cond_ex) * efficiency

    return {
        "case_id": case_id,
        "case_num": case_num,
        "efficiency": efficiency,
        "ssc_kg_kwh": ssc,
        "baseline_power_mw": baseline_mw,
        "hp_tph": hp,
        "mp_tph": mp,
        "condensate_tph": cf,
        "hp_hhp_equivalent_tph": hp * (hp_ex / cond_ex),
        "mp_hhp_equivalent_tph": mp * (mp_ex / cond_ex),
        "equivalent_inlet_tph": hp * (hp_ex / cond_ex) + mp * (mp_ex / cond_ex) + cf,
        "gross_inlet_tph": hp + mp + cf,
    }


def log_dta_stg_dispatch_point(asset_name: str,
                               req_hp_tph: float, req_mp_tph: float,
                               point: dict) -> None:
    """Log the full exergy-baseline derivation for one extraction STG."""
    hp = point["hp_tph"]
    mp = point["mp_tph"]
    cf = point["condensate_tph"]
    eta = point["efficiency"]
    config = get_dta_stg_config()
    hp_ex = float(config["hp_exergy_factor"])
    mp_ex = float(config["mp_exergy_factor"])
    cond_ex = float(config["condensate_exergy_factor"])
    hp_term = hp * (cond_ex - hp_ex)
    mp_term = mp * (cond_ex - mp_ex)
    c_term = cf * cond_ex
    raw = hp_term + mp_term + c_term

    logger.info("  DTA STG dispatch point: %s", asset_name)
    logger.info("    Required extraction  = HP %8.2f / MP %8.2f TPH", req_hp_tph, req_mp_tph)
    logger.info("    Booked extraction    = HP %8.2f / MP %8.2f TPH  (case-capped)", hp, mp)
    logger.info("    Demand case          = Case %s  (eta %.2f, ref SSC %.4f kg/kWh)",
                point["case_id"], eta, point["ssc_kg_kwh"])
    logger.info("    Exergy terms (TPH)   = HP %.2f*(%.3f-%.3f) + MP %.2f*(%.3f-%.3f) + C %.2f*%.3f",
                hp, cond_ex, hp_ex, mp, cond_ex, mp_ex, cf, cond_ex)
    logger.info("                         = %.4f + %.4f + %.4f = %.4f",
                hp_term, mp_term, c_term, raw)
    logger.info("    Baseline power       = %.4f * %.2f = %.4f MW  (mandatory min)",
                raw, eta, point["baseline_power_mw"])
    logger.info("    Inlet at baseline    = HP %.2f + MP %.2f + C %.2f = %.2f TPH SHP",
                hp, mp, cf, point["gross_inlet_tph"])


def dta_stg_condensate_topup(dispatch_point: dict, extra_mw: float,
                             config: dict = None) -> dict:
    """
    Additional condensing steam needed to raise STG output by `extra_mw`.

    Per plan §2.5: HP/MP extraction stay fixed; the extra steam is added to
    the condensate/condensing flow:
        dS_TPH  = extra_mw * SSC           (kg/kWh ~ tonne/MWh)
        C_new   = C_base + dS_TPH
    The returned "recalculated_power_mw" re-derives power from the exergy
    formula at the new condensate — it is reported separately because the
    SSC reference and the exergy formula do not match exactly.
    """
    if config is None:
        config = get_dta_stg_config()

    ssc = float(dispatch_point.get("ssc_kg_kwh", 0.0))
    c_base = float(dispatch_point.get("condensate_tph", 0.0))
    delta_s = max(0.0, float(extra_mw)) * ssc
    c_new = c_base + delta_s

    cond_ex = float(config["condensate_exergy_factor"])
    hp_ex = float(config["hp_exergy_factor"])
    mp_ex = float(config["mp_exergy_factor"])
    eta = float(dispatch_point.get("efficiency", 1.0))
    hp = float(dispatch_point.get("hp_tph", 0.0))
    mp = float(dispatch_point.get("mp_tph", 0.0))
    recalc_mw = (hp * (cond_ex - hp_ex)
                 + mp * (cond_ex - mp_ex)
                 + c_new * cond_ex) * eta

    return {
        "extra_power_mw": float(extra_mw),
        "additional_steam_tph": delta_s,
        "updated_condensate_tph": c_new,
        "target_power_mw": float(dispatch_point.get("baseline_power_mw", 0.0)) + extra_mw,
        "recalculated_power_mw": recalc_mw,
    }


def get_dta_stg_cases(config: dict = None) -> list:
    """Compute all configured AOP operating cases for DTA STGs."""
    if config is None:
        config = get_dta_stg_config()
    return [calculate_dta_stg_case(c, config) for c in config.get("cases", [])]


def select_dta_stg_case(hp_req_tph: float, mp_req_tph: float, config: dict = None) -> dict:
    """
    Select the AOP operating case for a DTA STG from the required HP/MP
    extraction duty (TPH).

    Rule: the smallest case that can still deliver the required extraction
    (case HP >= required HP AND case MP >= required MP).  Among covering
    cases, the one with the least surplus extraction wins.  If no case can
    cover the requirement, the maximum-capability case is returned and a
    warning is logged — the caller should cap the booked extraction at the
    case limits.
    """
    cases = get_dta_stg_cases(config)
    if not cases:
        return None

    covering = [
        c for c in cases
        if c["hp_extraction_tph"] >= hp_req_tph - 1e-6
        and c["mp_extraction_tph"] >= mp_req_tph - 1e-6
    ]
    if not covering:
        best = max(cases, key=lambda c: c["hp_extraction_tph"] + c["mp_extraction_tph"])
        logger.warning(
            "  [DTA STG] Required extraction (HP %.1f / MP %.1f TPH) exceeds all "
            "AOP cases; using Case %s and capping extraction at %.1f/%.1f TPH",
            hp_req_tph, mp_req_tph, best["case_id"],
            best["hp_extraction_tph"], best["mp_extraction_tph"],
        )
        return best

    return min(
        covering,
        key=lambda c: (c["hp_extraction_tph"] - hp_req_tph)
        + (c["mp_extraction_tph"] - mp_req_tph),
    )
