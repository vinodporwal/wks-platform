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
