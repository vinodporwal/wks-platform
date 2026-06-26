"""
Demand Service - Calculates Total Demand for All Utilities
Total Demand = Fixed + Process + U4U (Utility for Utility)

This service calculates the complete demand breakdown for:
- Power (MWH)
- Steam: SHP, HP, MP, LP (MT)
- BFW (M3)
- DM Water (M3)
- Cooling Water 1 & 2 (KM3)
- Compressed Air (NM3)
- Oxygen (MT)
- Effluent (M3)
"""

from database.connection import get_connection
from services.norm_lookup_service import get_month_norm, get_all_norms_for_material

def _norm(
    month: int,
    year: int,
    plant_name: str,
    utility_name: str,
    material_name: str,
    account_name: str = "Utilities",
    issuing_plant_name: str = None,
) -> float:
    return get_month_norm(
        month=month,
        year=year,
        plant_name=plant_name,
        utility_name=utility_name,
        material_name=material_name,
        account_name=account_name,
        issuing_plant_name=issuing_plant_name,
        required=True,
    )


def fetch_fixed_process_demands(month: int, year: int, cpp_plant_id: str = None) -> dict:
    """
    Fetch Fixed and Process demands from database for all utilities.
    
    Returns dict with structure:
    {
        "power": {"fixed": MWH, "process": MWH},
        "steam_lp": {"fixed": MT, "process": MT},
        "steam_mp": {"fixed": MT, "process": MT},
        "steam_hp": {"fixed": MT, "process": MT},
        "steam_shp": {"fixed": MT, "process": MT},
        "bfw": {"fixed": M3, "process": M3},
        "dm_water": {"fixed": M3, "process": M3},
        "cw1": {"fixed": KM3, "process": KM3},
        "cw2": {"fixed": KM3, "process": KM3},
        "compressed_air": {"fixed": NM3, "process": NM3},
        "oxygen": {"fixed": MT, "process": MT},
        "effluent": {"fixed": M3, "process": M3},
    }
    """
    conn = get_connection()
    cur = conn.cursor()
    
    # Get FYM ID
    cur.execute("SELECT Id FROM FinancialYearMonth WHERE [Month]=? AND [Year]=?", (month, year))
    row = cur.fetchone()
    if not row:
        conn.close()
        return None
    fym_id = row[0]
    
   # Fetch Power demands from CalculatedProcessDemand via process_demand_service
    from services.process_demand_service import get_process_demand_for_month
    process_demands = get_process_demand_for_month(month, year, cpp_plant_id)
    power_process_kwh = process_demands.get("power_process", 0.0)
    # Convert KWH to MWh
    power_process = power_process_kwh / 1000.0
    
    # Fetch Fixed consumption from UtilityFixedConsumption table via fixed_consumption_service
    from services.fixed_consumption_service import get_fixed_consumption_for_month
    fixed_consumption = get_fixed_consumption_for_month(month, year)
    power_fixed_kwh = fixed_consumption.get("power_fixed_kwh", 0.0)
    # Convert KWH to MWh
    power_fixed = power_fixed_kwh / 1000.0
    
    conn.close()
    
    # Return demands (steam and other utilities are passed as parameters for now)
    return {
        "power": {"fixed": power_fixed, "process": power_process},
    }


def calculate_u4u_power(
    month: int,
    year: int,
    # Power generation (for aux calculation)
    gt1_gross_mwh: float = 0.0,
    gt2_gross_mwh: float = 0.0,
    gt3_gross_mwh: float = 0.0,
    stg_gross_mwh: float = 0.0,
    # Utility quantities (for power consumption)
    bfw_total_m3: float = 0.0,
    dm_total_m3: float = 0.0,
    cw1_total_km3: float = 0.0,
    cw2_total_km3: float = 0.0,
    air_total_nm3: float = 0.0,
    oxygen_total_mt: float = 0.0,
    effluent_total_m3: float = 0.0,
) -> dict:
    """
    Calculate U4U Power consumption from all utilities.
    
    Returns breakdown of power consumed by each utility.
    """
    # Power Plant Auxiliary (U4U for power generation) - DB norms
    gt1_aux_norm = _norm(month, year, "NMD - Power Plant 1", "POWERGEN", "Power_Dis", issuing_plant_name="NMD - Utility/Power Dist")
    gt2_aux_norm = _norm(month, year, "NMD - Power Plant 2", "POWERGEN", "Power_Dis", issuing_plant_name="NMD - Utility/Power Dist")
    gt3_aux_norm = _norm(month, year, "NMD - Power Plant 3", "POWERGEN", "Power_Dis", issuing_plant_name="NMD - Utility/Power Dist")
    stg_aux_norm = _norm(month, year, "NMD - STG Power Plant", "POWERGEN", "Power_Dis", issuing_plant_name="NMD - Utility/Power Dist")

    gt1_aux_kwh = gt1_gross_mwh * 1000 * gt1_aux_norm
    gt2_aux_kwh = gt2_gross_mwh * 1000 * gt2_aux_norm
    gt3_aux_kwh = gt3_gross_mwh * 1000 * gt3_aux_norm
    stg_aux_kwh = stg_gross_mwh * 1000 * stg_aux_norm
    total_power_aux_kwh = gt1_aux_kwh + gt2_aux_kwh + gt3_aux_kwh + stg_aux_kwh
    
    # Utility Power Consumption (U4U for utilities) - DB norms
    bfw_power_norm = _norm(month, year, "NMD - Utility Plant", "Boiler Feed Water", "Power_Dis")
    dm_power_norm = _norm(month, year, "NMD - Utility Plant", "D M Water", "Power_Dis")
    cw1_power_norm = _norm(month, year, "NMD - Utility Plant", "Cooling Water 1", "Power_Dis")
    cw2_power_norm = _norm(month, year, "NMD - Utility Plant", "Cooling Water 2", "Power_Dis")
    air_power_norm = _norm(month, year, "NMD - Utility Plant", "COMPRESSED AIR", "Power_Dis")
    oxygen_power_norm = _norm(month, year, "NMD - Utility Plant", "Oxygen", "Power_Dis")
    effluent_power_norm = _norm(month, year, "NMD - Utility Plant", "Effluent Treated", "Power_Dis")

    bfw_power_kwh = bfw_total_m3 * bfw_power_norm
    dm_power_kwh = dm_total_m3 * dm_power_norm
    cw1_power_kwh = cw1_total_km3 * cw1_power_norm
    cw2_power_kwh = cw2_total_km3 * cw2_power_norm
    air_power_kwh = air_total_nm3 * air_power_norm
    oxygen_power_kwh = oxygen_total_mt * oxygen_power_norm
    effluent_power_kwh = effluent_total_m3 * effluent_power_norm
    
    total_utility_power_kwh = (bfw_power_kwh + dm_power_kwh + cw1_power_kwh + 
                               cw2_power_kwh + air_power_kwh + oxygen_power_kwh + 
                               effluent_power_kwh)
    
    total_u4u_power_kwh = total_power_aux_kwh + total_utility_power_kwh
    
    return {
        "power_aux": {
            "gt1_kwh": round(gt1_aux_kwh, 2),
            "gt2_kwh": round(gt2_aux_kwh, 2),
            "gt3_kwh": round(gt3_aux_kwh, 2),
            "stg_kwh": round(stg_aux_kwh, 2),
            "total_kwh": round(total_power_aux_kwh, 2),
            "total_mwh": round(total_power_aux_kwh / 1000, 2),
        },
        "utility_power": {
            "bfw_kwh": round(bfw_power_kwh, 2),
            "dm_kwh": round(dm_power_kwh, 2),
            "cw1_kwh": round(cw1_power_kwh, 2),
            "cw2_kwh": round(cw2_power_kwh, 2),
            "air_kwh": round(air_power_kwh, 2),
            "oxygen_kwh": round(oxygen_power_kwh, 2),
            "effluent_kwh": round(effluent_power_kwh, 2),
            "total_kwh": round(total_utility_power_kwh, 2),
            "total_mwh": round(total_utility_power_kwh / 1000, 2),
        },
        "total_u4u_kwh": round(total_u4u_power_kwh, 2),
        "total_u4u_mwh": round(total_u4u_power_kwh / 1000, 2),
    }


def calculate_u4u_bfw(
    month: int,
    year: int,
    shp_from_hrsg_mt: float = 0.0,
    hp_from_prds_mt: float = 0.0,
    mp_from_prds_mt: float = 0.0,
    lp_from_prds_mt: float = 0.0,
    shp_from_hrsg1_mt: float = 0.0,
    shp_from_hrsg2_mt: float = 0.0,
    shp_from_hrsg3_mt: float = 0.0,
) -> dict:
    """
    Calculate U4U BFW consumption from steam generation.

    Dynamically fetches all norms where MaterialName='Boiler Feed Water' from
    the database and maps each UtilityName to the corresponding generation
    quantity.  This replaces the previous hardcoded approach that only looked
    up HRSG2_SHP STEAM, HP Steam PRDS, MP Steam PRDS SHP and LP Steam PRDS.

    Backward-compatible return keys (hrsg_m3, hp_prds_m3, mp_prds_m3,
    lp_prds_m3, total_m3) are always present.  Per-HRSG keys
    (hrsg1_m3, hrsg2_m3, hrsg3_m3) are added when per-HRSG quantities are
    supplied.  Any additional utilities discovered in the DB are included
    with a key derived from the UtilityName.
    """
    # ── generation-quantity map (DB UtilityName → quantity) ──────────
    generation_map: dict[str, float] = {
        "HRSG1_SHP STEAM": shp_from_hrsg1_mt,
        "HRSG2_SHP STEAM": shp_from_hrsg2_mt,
        "HRSG3_SHP STEAM": shp_from_hrsg3_mt,
        "HP Steam PRDS": hp_from_prds_mt,
        "MP Steam PRDS SHP": mp_from_prds_mt,
        "LP Steam PRDS": lp_from_prds_mt,
    }

    # ── dynamic fetch of all BFW norms from DB ───────────────────────
    bfw_rows = get_all_norms_for_material(
        month, year, "Boiler Feed Water", plant_name="NMD - Utility Plant",
    )

    total_bfw = 0.0
    per_hrsg: dict[str, float] = {}          # key → consumption
    prds_breakdown: dict[str, float] = {}
    extra_breakdown: dict[str, float] = {}

    for row in bfw_rows:
        util_name = row["utility_name"] or ""
        norm = row["norm"]
        if norm is None:
            continue

        # Try to match the UtilityName to a generation quantity
        matched_qty = None
        for map_key, qty in generation_map.items():
            if _norm_key_match(map_key, util_name):
                matched_qty = qty
                break

        if matched_qty is not None and matched_qty > 0:
            consumption = matched_qty * norm
            total_bfw += consumption
            _store_bfw_breakdown(
                util_name, consumption, per_hrsg, prds_breakdown, extra_breakdown,
            )

    # ── fallback: per-HRSG quantities not supplied ───────────────────
    # If no per-HRSG consumption was recorded but a total shp_from_hrsg_mt
    # was passed, apply the first HRSG norm found to the total (old behavior).
    if not per_hrsg and shp_from_hrsg_mt > 0:
        for row in bfw_rows:
            util_name = row["utility_name"] or ""
            if "HRSG" in util_name.upper() and row["norm"] is not None:
                consumption = shp_from_hrsg_mt * row["norm"]
                total_bfw += consumption
                per_hrsg[util_name] = consumption
                break  # only one HRSG norm to avoid double-counting

    # ── assemble backward-compatible result ──────────────────────────
    hrsg_total = sum(per_hrsg.values())
    result: dict = {
        "hrsg_m3": round(hrsg_total, 2),
        "hp_prds_m3": round(prds_breakdown.get("HP Steam PRDS", 0.0), 2),
        "mp_prds_m3": round(prds_breakdown.get("MP Steam PRDS SHP", 0.0), 2),
        "lp_prds_m3": round(prds_breakdown.get("LP Steam PRDS", 0.0), 2),
        "total_m3": round(total_bfw, 2),
    }

    # Add per-HRSG keys
    for key, val in per_hrsg.items():
        result[_hrsg_key(key)] = round(val, 2)

    # Add any extra dynamically discovered utilities
    for key, val in extra_breakdown.items():
        result[_extra_key(key)] = round(val, 2)

    return result


def _norm_key_match(map_key: str, util_name: str) -> bool:
    """Case-insensitive match between a generation-map key and a DB UtilityName."""
    return map_key.strip().upper() == util_name.strip().upper()


def _store_bfw_breakdown(
    util_name: str,
    consumption: float,
    per_hrsg: dict,
    prds_breakdown: dict,
    extra_breakdown: dict,
) -> None:
    """Route a BFW consumption value to the appropriate breakdown bucket."""
    upper = util_name.strip().upper()
    if "HRSG" in upper:
        per_hrsg[util_name] = per_hrsg.get(util_name, 0.0) + consumption
    elif "PRDS" in upper:
        prds_breakdown[util_name] = prds_breakdown.get(util_name, 0.0) + consumption
    else:
        extra_breakdown[util_name] = extra_breakdown.get(util_name, 0.0) + consumption


def _hrsg_key(util_name: str) -> str:
    """Convert a DB UtilityName like 'HRSG2_SHP STEAM' to a dict key like 'hrsg2_m3'."""
    upper = util_name.strip().upper()
    for i in (1, 2, 3):
        if f"HRSG{i}" in upper:
            return f"hrsg{i}_m3"
    return "hrsg_m3"


def _extra_key(util_name: str) -> str:
    """Convert an arbitrary DB UtilityName to a snake_case dict key with _m3 suffix."""
    key = util_name.strip().lower().replace(" ", "_").replace("/", "_")
    if not key.endswith("_m3"):
        key += "_m3"
    return key


def calculate_u4u_dm(month: int, year: int, bfw_total_m3: float = 0.0) -> dict:
    """
    Calculate U4U DM Water consumption from BFW.
    """
    dm_per_bfw_norm = _norm(month, year, "NMD - Utility Plant", "Boiler Feed Water", "D M Water")
    dm_for_bfw = bfw_total_m3 * dm_per_bfw_norm
    return {
        "for_bfw_m3": round(dm_for_bfw, 2),
        "total_m3": round(dm_for_bfw, 2),
    }


def calculate_u4u_cw2(
    month: int,
    year: int,
    stg_gross_mwh: float = 0.0,
    gt1_gross_mwh: float = 0.0,
    gt2_gross_mwh: float = 0.0,
    gt3_gross_mwh: float = 0.0,
    shp_from_stg_mt: float = 0.0,
) -> dict:
    """
    Calculate U4U Cooling Water 2 consumption from power plants.
    """
    gt1_cw2_norm = _norm(month, year, "NMD - Power Plant 1", "POWERGEN", "Cooling Water 2", issuing_plant_name="NMD - Utility Plant")
    gt2_cw2_norm = _norm(month, year, "NMD - Power Plant 2", "POWERGEN", "Cooling Water 2", issuing_plant_name="NMD - Utility Plant")
    gt3_cw2_norm = _norm(month, year, "NMD - Power Plant 3", "POWERGEN", "Cooling Water 2", issuing_plant_name="NMD - Utility Plant")
    stg_cw2_norm = _norm(month, year, "NMD - STG Power Plant", "POWERGEN", "Cooling Water 2", issuing_plant_name="NMD - Utility Plant")

    cw2_stg = (stg_gross_mwh * 1000) * stg_cw2_norm
    cw2_gt = (
        (gt1_gross_mwh * 1000 * gt1_cw2_norm)
        + (gt2_gross_mwh * 1000 * gt2_cw2_norm)
        + (gt3_gross_mwh * 1000 * gt3_cw2_norm)
    )
    
    return {
        "stg_km3": round(cw2_stg, 2),
        "gt_km3": round(cw2_gt, 2),
        "total_km3": round(cw2_stg + cw2_gt, 2),
    }


def calculate_u4u_air(
    month: int,
    year: int,
    gt1_gross_mwh: float = 0.0,
    gt2_gross_mwh: float = 0.0,
    gt3_gross_mwh: float = 0.0,
    stg_gross_mwh: float = 0.0,
    shp_from_hrsg_mt: float = 0.0,
    shp_from_hrsg1_mt: float = 0.0,
    shp_from_hrsg2_mt: float = 0.0,
    shp_from_hrsg3_mt: float = 0.0,
    cw1_total_km3: float = 0.0,
    cw2_total_km3: float = 0.0,
    dm_total_m3: float = 0.0,
    bfw_total_m3: float = 0.0,
) -> dict:
    """
    Calculate U4U Compressed Air consumption from power plants and utilities.

    Dynamically fetches all norms where MaterialName='COMPRESSED AIR' from
    the database (NMD - Utility Plant) and maps each UtilityName to the
    corresponding generation quantity.  GT/STG norms are fetched from their
    respective power plant entries.

    Backward-compatible return keys (gt_nm3, stg_nm3, hrsg_nm3, total_nm3)
    are always present.  Per-HRSG keys (hrsg1_nm3, hrsg2_nm3, hrsg3_nm3)
    are added when per-HRSG quantities are supplied.  Any additional
    utilities discovered in the DB are included with a key derived from
    the UtilityName.
    """
    # GT/STG norms from respective power plants
    gt1_air_norm = _norm(month, year, "NMD - Power Plant 1", "POWERGEN", "COMPRESSED AIR", issuing_plant_name="NMD - Utility Plant")
    gt2_air_norm = _norm(month, year, "NMD - Power Plant 2", "POWERGEN", "COMPRESSED AIR", issuing_plant_name="NMD - Utility Plant")
    gt3_air_norm = _norm(month, year, "NMD - Power Plant 3", "POWERGEN", "COMPRESSED AIR", issuing_plant_name="NMD - Utility Plant")
    stg_air_norm = _norm(month, year, "NMD - STG Power Plant", "POWERGEN", "COMPRESSED AIR", issuing_plant_name="NMD - Utility Plant")

    air_gt = (
        (gt1_gross_mwh * 1000 * gt1_air_norm)
        + (gt2_gross_mwh * 1000 * gt2_air_norm)
        + (gt3_gross_mwh * 1000 * gt3_air_norm)
    )
    air_stg = stg_gross_mwh * 1000 * stg_air_norm

    # ── generation-quantity map (DB UtilityName → quantity) ──────────
    generation_map: dict[str, float] = {
        "HRSG1_SHP STEAM": shp_from_hrsg1_mt,
        "HRSG2_SHP STEAM": shp_from_hrsg2_mt,
        "HRSG3_SHP STEAM": shp_from_hrsg3_mt,
        "Cooling Water 1": cw1_total_km3,
        "Cooling Water 2": cw2_total_km3,
        "D M Water": dm_total_m3,
        "Boiler Feed Water": bfw_total_m3,
    }

    # ── dynamic fetch of all compressed air norms from DB ───────────
    air_rows = get_all_norms_for_material(
        month, year, "COMPRESSED AIR", plant_name="NMD - Utility Plant",
    )

    per_hrsg: dict[str, float] = {}
    extra_breakdown: dict[str, float] = {}

    for row in air_rows:
        util_name = row["utility_name"] or ""
        norm = row["norm"]
        if norm is None:
            continue

        matched_qty = None
        for map_key, qty in generation_map.items():
            if _norm_key_match(map_key, util_name):
                matched_qty = qty
                break

        if matched_qty is not None and matched_qty > 0:
            consumption = matched_qty * norm
            upper = util_name.strip().upper()
            if "HRSG" in upper:
                per_hrsg[util_name] = per_hrsg.get(util_name, 0.0) + consumption
            else:
                extra_breakdown[util_name] = extra_breakdown.get(util_name, 0.0) + consumption

    # ── fallback: per-HRSG quantities not supplied ───────────────────
    if not per_hrsg and shp_from_hrsg_mt > 0:
        for row in air_rows:
            util_name = row["utility_name"] or ""
            if "HRSG" in util_name.upper() and row["norm"] is not None:
                consumption = shp_from_hrsg_mt * row["norm"]
                per_hrsg[util_name] = consumption
                break

    # ── assemble backward-compatible result ──────────────────────────
    hrsg_total = sum(per_hrsg.values())
    extra_total = sum(extra_breakdown.values())
    result: dict = {
        "gt_nm3": round(air_gt, 2),
        "stg_nm3": round(air_stg, 2),
        "hrsg_nm3": round(hrsg_total, 2),
        "total_nm3": round(air_gt + air_stg + hrsg_total + extra_total, 2),
    }

    for key, val in per_hrsg.items():
        upper = key.strip().upper()
        hrsg_key = "hrsg_nm3"
        for i in (1, 2, 3):
            if f"HRSG{i}" in upper:
                hrsg_key = f"hrsg{i}_nm3"
                break
        result[hrsg_key] = round(val, 2)

    for key, val in extra_breakdown.items():
        extra_key = key.strip().lower().replace(" ", "_").replace("/", "_")
        if not extra_key.endswith("_nm3"):
            extra_key += "_nm3"
        result[extra_key] = round(val, 2)

    return result


def calculate_u4u_lp_steam(
    bfw_total_m3: float = 0.0,
    shp_from_hrsg1_mt: float = 0.0,
    shp_from_hrsg2_mt: float = 0.0,
    shp_from_hrsg3_mt: float = 0.0,
    norm_bfw: float = 0.0,
    norm_hrsg1: float = 0.0,
    norm_hrsg2: float = 0.0,
    norm_hrsg3: float = 0.0,
) -> dict:
    """
    Calculate U4U LP Steam consumption by utility plants.

    LP steam is consumed by:
    - BFW Plant deaerator: norm * total BFW volume
    - HRSG1/2/3: norm * SHP generated by each HRSG (steam drum heating)

    Norms are fetched from NormsMonthDetail (CPPNorms) and passed in as parameters.

    Returns breakdown of LP consumed by each utility.
    """
    # BFW Plant deaerator uses LP steam for heating
    bfw_lp_mt = bfw_total_m3 * norm_bfw

    # Each HRSG consumes LP steam proportional to its SHP output
    hrsg1_lp_mt = shp_from_hrsg1_mt * norm_hrsg1
    hrsg2_lp_mt = shp_from_hrsg2_mt * norm_hrsg2
    hrsg3_lp_mt = shp_from_hrsg3_mt * norm_hrsg3

    total_lp_mt = bfw_lp_mt + hrsg1_lp_mt + hrsg2_lp_mt + hrsg3_lp_mt

    return {
        "bfw_mt": round(bfw_lp_mt, 2),
        "bfw_norm": norm_bfw,
        "hrsg1_mt": round(hrsg1_lp_mt, 2),
        "hrsg1_norm": norm_hrsg1,
        "hrsg2_mt": round(hrsg2_lp_mt, 2),
        "hrsg2_norm": norm_hrsg2,
        "hrsg3_mt": round(hrsg3_lp_mt, 2),
        "hrsg3_norm": norm_hrsg3,
        "total_mt": round(total_lp_mt, 2),
    }


def calculate_u4u_mp_steam(
    tsc_qty_kl: float = 0.0,
    norm_tsc: float = 0.0,
) -> dict:
    """
    Calculate U4U MP Steam consumption by utility plants.

    MP steam is consumed by:
    - Treated Spent Caustic (TSC): norm (MT MP / KL TSC) * TSC volume

    Note: LP Steam via PRDS is already accounted for in the steam cascade
    (mp_for_lp in the MP balance), so it is NOT included here to avoid
    double-counting.

    Norms are fetched from CPPNorms and passed in as parameters.

    Returns breakdown of MP consumed by each utility.
    """
    tsc_mp_mt = tsc_qty_kl * norm_tsc
    total_mp_mt = tsc_mp_mt

    return {
        "tsc_mt": round(tsc_mp_mt, 2),
        "tsc_norm": norm_tsc,
        "tsc_qty_kl": round(tsc_qty_kl, 2),
        "total_mt": round(total_mp_mt, 2),
    }


def calculate_u4u_shp(
    month: int,
    year: int,
    stg_gross_mwh: float = 0.0,
    lp_from_stg_mt: float = 0.0,
    mp_from_stg_mt: float = 0.0,
    hp_from_prds_mt: float = 0.0,
    mp_from_prds_mt: float = 0.0,
) -> dict:
    """
    Calculate U4U SHP consumption from STG and PRDS.
    """
    stg_shp_norm = _norm(month, year, "NMD - STG Power Plant", "POWERGEN", "SHP Steam_Dis", issuing_plant_name="NMD - Utility/Power Dist")
    shp_lp_stg_norm = _norm(month, year, "NMD - Utility Plant", "STG1_LP STEAM", "SHP Steam_Dis")
    shp_mp_stg_norm = _norm(month, year, "NMD - Utility Plant", "STG1_MP STEAM", "SHP Steam_Dis")
    shp_hp_prds_norm = _norm(month, year, "NMD - Utility Plant", "HP Steam PRDS", "SHP Steam_Dis")
    shp_mp_prds_norm = _norm(month, year, "NMD - Utility Plant", "MP Steam PRDS SHP", "SHP Steam_Dis")

    shp_stg_power = stg_gross_mwh * 1000 * stg_shp_norm
    shp_lp_extraction = lp_from_stg_mt * shp_lp_stg_norm
    shp_mp_extraction = mp_from_stg_mt * shp_mp_stg_norm
    shp_hp_prds = hp_from_prds_mt * shp_hp_prds_norm
    shp_mp_prds = mp_from_prds_mt * shp_mp_prds_norm
    
    total_shp = shp_stg_power + shp_lp_extraction + shp_mp_extraction + shp_hp_prds + shp_mp_prds
    
    return {
        "stg_power_mt": round(shp_stg_power, 2),
        "lp_extraction_mt": round(shp_lp_extraction, 2),
        "mp_extraction_mt": round(shp_mp_extraction, 2),
        "hp_prds_mt": round(shp_hp_prds, 2),
        "mp_prds_mt": round(shp_mp_prds, 2),
        "total_mt": round(total_shp, 2),
    }


def calculate_all_demands(
    month: int,
    year: int,
    # Steam demands (Fixed + Process from input)
    lp_process: float = 0.0,
    lp_fixed: float = 0.0,
    mp_process: float = 0.0,
    mp_fixed: float = 0.0,
    hp_process: float = 0.0,
    hp_fixed: float = 0.0,
    shp_process: float = 0.0,
    shp_fixed: float = 0.0,
    # Other utility demands (Fixed + Process from input)
    bfw_process: float = 0.0,
    bfw_fixed: float = 0.0,
    dm_process: float = 54779.0,
    dm_fixed: float = 0.0,
    cw1_process: float = 15194.0,
    cw1_fixed: float = 0.0,
    cw2_process: float = 9016.0,
    cw2_fixed: float = 0.0,
    air_process: float = 6095102.0,
    air_fixed: float = 0.0,
    oxygen_process: float = 5786.0,
    oxygen_fixed: float = 0.0,
    effluent_process: float = 243000.0,
    effluent_fixed: float = 0.0,
    # Dispatch results (for U4U calculation)
    gt1_gross_mwh: float = 0.0,
    gt2_gross_mwh: float = 0.0,
    gt3_gross_mwh: float = 0.0,
    stg_gross_mwh: float = 0.0,
    shp_from_hrsg_mt: float = 0.0,
    hp_from_prds_mt: float = 0.0,
    mp_from_prds_mt: float = 0.0,
    lp_from_prds_mt: float = 0.0,
    lp_from_stg_mt: float = 0.0,
    mp_from_stg_mt: float = 0.0,
    gt_count: int = 0,
    stg_available: bool = False,
    hrsg_count: int = 0,
) -> dict:
    """
    Calculate complete demand breakdown for all utilities.
    
    Returns:
        dict with Fixed, Process, U4U, and Total for each utility
    """
    # Fetch power demands from database
    db_demands = fetch_fixed_process_demands(month, year)
    power_fixed = db_demands["power"]["fixed"] if db_demands else 0.0
    power_process = db_demands["power"]["process"] if db_demands else 0.0
    
    # Calculate U4U for BFW
    u4u_bfw = calculate_u4u_bfw(
        month,
        year,
        shp_from_hrsg_mt,
        hp_from_prds_mt,
        mp_from_prds_mt,
        lp_from_prds_mt,
    )
    bfw_u4u = u4u_bfw["total_m3"]
    bfw_total = bfw_fixed + bfw_process + bfw_u4u
    
    # Calculate U4U for DM (depends on total BFW)
    u4u_dm = calculate_u4u_dm(month, year, bfw_total)
    dm_u4u = u4u_dm["total_m3"]
    dm_total = dm_fixed + dm_process + dm_u4u
    
    # Calculate U4U for CW2
    stg_shp_norm = _norm(
        month,
        year,
        "NMD - STG Power Plant",
        "POWERGEN",
        "SHP Steam_Dis",
        issuing_plant_name="NMD - Utility/Power Dist",
    )
    shp_for_stg = stg_gross_mwh * 1000 * stg_shp_norm
    u4u_cw2 = calculate_u4u_cw2(
        month,
        year,
        stg_gross_mwh=stg_gross_mwh,
        gt1_gross_mwh=gt1_gross_mwh,
        gt2_gross_mwh=gt2_gross_mwh,
        gt3_gross_mwh=gt3_gross_mwh,
        shp_from_stg_mt=shp_for_stg,
    )
    cw2_u4u = u4u_cw2["total_km3"]
    cw2_total = cw2_fixed + cw2_process + cw2_u4u
    
    # CW1 has no U4U (only process)
    cw1_u4u = 0.0
    cw1_total = cw1_fixed + cw1_process + cw1_u4u
    
    # Calculate U4U for Compressed Air
    u4u_air = calculate_u4u_air(
        month,
        year,
        gt1_gross_mwh=gt1_gross_mwh,
        gt2_gross_mwh=gt2_gross_mwh,
        gt3_gross_mwh=gt3_gross_mwh,
        stg_gross_mwh=stg_gross_mwh,
        shp_from_hrsg_mt=shp_from_hrsg_mt,
    )
    air_u4u = u4u_air["total_nm3"]
    air_total = air_fixed + air_process + air_u4u
    
    # Oxygen and Effluent have no U4U (only process/fixed)
    oxygen_u4u = 0.0
    oxygen_total = oxygen_fixed + oxygen_process + oxygen_u4u
    effluent_u4u = 0.0
    effluent_total = effluent_fixed + effluent_process + effluent_u4u
    
    # Calculate U4U for Power (depends on all utility totals)
    u4u_power = calculate_u4u_power(
        month=month,
        year=year,
        gt1_gross_mwh=gt1_gross_mwh,
        gt2_gross_mwh=gt2_gross_mwh,
        gt3_gross_mwh=gt3_gross_mwh,
        stg_gross_mwh=stg_gross_mwh,
        bfw_total_m3=bfw_total,
        dm_total_m3=dm_total,
        cw1_total_km3=cw1_total,
        cw2_total_km3=cw2_total,
        air_total_nm3=air_total,
        oxygen_total_mt=oxygen_total,
        effluent_total_m3=effluent_total,
    )
    power_u4u_mwh = u4u_power["total_u4u_mwh"]
    power_total = power_fixed + power_process + power_u4u_mwh
    
    # Calculate U4U for SHP
    u4u_shp = calculate_u4u_shp(
        month,
        year,
        stg_gross_mwh,
        lp_from_stg_mt,
        mp_from_stg_mt,
        hp_from_prds_mt,
        mp_from_prds_mt,
    )
    shp_u4u = u4u_shp["total_mt"]
    shp_total = shp_fixed + shp_process + shp_u4u
    
    # LP, MP, HP don't have direct U4U (they drive SHP U4U)
    lp_u4u = 0.0
    lp_total = lp_fixed + lp_process + lp_u4u
    mp_u4u = 0.0
    mp_total = mp_fixed + mp_process + mp_u4u
    hp_u4u = 0.0
    hp_total = hp_fixed + hp_process + hp_u4u
    
    return {
        "power": {
            "fixed": round(power_fixed, 2),
            "process": round(power_process, 2),
            "u4u": round(power_u4u_mwh, 2),
            "total": round(power_total, 2),
            "u4u_detail": u4u_power,
            "unit": "MWH",
        },
        "steam_shp": {
            "fixed": round(shp_fixed, 2),
            "process": round(shp_process, 2),
            "u4u": round(shp_u4u, 2),
            "total": round(shp_total, 2),
            "u4u_detail": u4u_shp,
            "unit": "MT",
        },
        "steam_hp": {
            "fixed": round(hp_fixed, 2),
            "process": round(hp_process, 2),
            "u4u": round(hp_u4u, 2),
            "total": round(hp_total, 2),
            "unit": "MT",
        },
        "steam_mp": {
            "fixed": round(mp_fixed, 2),
            "process": round(mp_process, 2),
            "u4u": round(mp_u4u, 2),
            "total": round(mp_total, 2),
            "unit": "MT",
        },
        "steam_lp": {
            "fixed": round(lp_fixed, 2),
            "process": round(lp_process, 2),
            "u4u": round(lp_u4u, 2),
            "total": round(lp_total, 2),
            "unit": "MT",
        },
        "bfw": {
            "fixed": round(bfw_fixed, 2),
            "process": round(bfw_process, 2),
            "u4u": round(bfw_u4u, 2),
            "total": round(bfw_total, 2),
            "u4u_detail": u4u_bfw,
            "unit": "M3",
        },
        "dm_water": {
            "fixed": round(dm_fixed, 2),
            "process": round(dm_process, 2),
            "u4u": round(dm_u4u, 2),
            "total": round(dm_total, 2),
            "u4u_detail": u4u_dm,
            "unit": "M3",
        },
        "cw1": {
            "fixed": round(cw1_fixed, 2),
            "process": round(cw1_process, 2),
            "u4u": round(cw1_u4u, 2),
            "total": round(cw1_total, 2),
            "unit": "KM3",
        },
        "cw2": {
            "fixed": round(cw2_fixed, 2),
            "process": round(cw2_process, 2),
            "u4u": round(cw2_u4u, 2),
            "total": round(cw2_total, 2),
            "u4u_detail": u4u_cw2,
            "unit": "KM3",
        },
        "compressed_air": {
            "fixed": round(air_fixed, 2),
            "process": round(air_process, 2),
            "u4u": round(air_u4u, 2),
            "total": round(air_total, 2),
            "u4u_detail": u4u_air,
            "unit": "NM3",
        },
        "oxygen": {
            "fixed": round(oxygen_fixed, 2),
            "process": round(oxygen_process, 2),
            "u4u": round(oxygen_u4u, 2),
            "total": round(oxygen_total, 2),
            "unit": "MT",
        },
        "effluent": {
            "fixed": round(effluent_fixed, 2),
            "process": round(effluent_process, 2),
            "u4u": round(effluent_u4u, 2),
            "total": round(effluent_total, 2),
            "unit": "M3",
        },
    }


def print_demand_summary(demands: dict):
    """
    Print detailed demand breakdown table for all utilities.
    """
    print("\n" + "="*100)
    print("UTILITY DEMAND SUMMARY (Fixed + Process + U4U = Total)")
    print("="*100)
    print(f"  {'Utility':<20} {'Unit':<8} {'Fixed':>15} {'Process':>15} {'U4U':>15} {'Total':>15}")
    print(f"  {'-'*88}")
    
    utilities = [
        ("Power", "power"),
        ("SHP Steam", "steam_shp"),
        ("HP Steam", "steam_hp"),
        ("MP Steam", "steam_mp"),
        ("LP Steam", "steam_lp"),
        ("BFW", "bfw"),
        ("DM Water", "dm_water"),
        ("Cooling Water 1", "cw1"),
        ("Cooling Water 2", "cw2"),
        ("Compressed Air", "compressed_air"),
        ("Oxygen", "oxygen"),
        ("Effluent", "effluent"),
    ]
    
    for name, key in utilities:
        d = demands.get(key, {})
        unit = d.get("unit", "")
        fixed = d.get("fixed", 0)
        process = d.get("process", 0)
        u4u = d.get("u4u", 0)
        total = d.get("total", 0)
        print(f"  {name:<20} {unit:<8} {fixed:>15,.2f} {process:>15,.2f} {u4u:>15,.2f} {total:>15,.2f}")
    
    print(f"  {'-'*88}")
    print("="*100)
    
    # Print U4U Power breakdown
    power_u4u = demands.get("power", {}).get("u4u_detail", {})
    if power_u4u:
        print("\n  U4U POWER BREAKDOWN:")
        print(f"  {'-'*60}")
        
        # Power Aux
        aux = power_u4u.get("power_aux", {})
        print(f"    Power Plant Auxiliary:")
        print(f"      GT1 Aux:           {aux.get('gt1_kwh', 0):>15,.2f} KWH")
        print(f"      GT2 Aux:           {aux.get('gt2_kwh', 0):>15,.2f} KWH")
        print(f"      GT3 Aux:           {aux.get('gt3_kwh', 0):>15,.2f} KWH")
        print(f"      STG Aux:           {aux.get('stg_kwh', 0):>15,.2f} KWH")
        print(f"      Subtotal:          {aux.get('total_kwh', 0):>15,.2f} KWH ({aux.get('total_mwh', 0):,.2f} MWH)")
        
        # Utility Power
        util = power_u4u.get("utility_power", {})
        print(f"    Utility Power:")
        print(f"      BFW Power:         {util.get('bfw_kwh', 0):>15,.2f} KWH")
        print(f"      DM Power:          {util.get('dm_kwh', 0):>15,.2f} KWH")
        print(f"      CW1 Power:         {util.get('cw1_kwh', 0):>15,.2f} KWH")
        print(f"      CW2 Power:         {util.get('cw2_kwh', 0):>15,.2f} KWH")
        print(f"      Air Power:         {util.get('air_kwh', 0):>15,.2f} KWH")
        print(f"      Oxygen Power:      {util.get('oxygen_kwh', 0):>15,.2f} KWH")
        print(f"      Effluent Power:    {util.get('effluent_kwh', 0):>15,.2f} KWH")
        print(f"      Subtotal:          {util.get('total_kwh', 0):>15,.2f} KWH ({util.get('total_mwh', 0):,.2f} MWH)")
        
        print(f"  {'-'*60}")
        print(f"    TOTAL U4U POWER:     {power_u4u.get('total_u4u_kwh', 0):>15,.2f} KWH ({power_u4u.get('total_u4u_mwh', 0):,.2f} MWH)")
    
    print("="*100 + "\n")
