"""
Unified Budget Service - Power & Steam
Follows the Utility Automation Flowchart V0

FLOW:
1. INPUT: Month, Year
2. POWER DEMAND & CAPACITY CHECK
   - Get power demand (Plant + Fixed)
   - Get asset availability (GT1, GT2, GT3, STG)
   - Check: Capacity >= Demand?
   - If NO: Check Import → If still insufficient → ERROR
   - If YES: Dispatch by priority
3. STEAM DEMAND & CAPACITY CHECK
   - Get steam demands (LP, MP, HP, SHP)
   - Calculate LP → MP → HP → SHP chain
   - Get HRSG availability (from GT dispatch)
   - Check: SHP Capacity >= SHP Demand?
   - If NO: Check Supplementary Firing → If still insufficient → ERROR
   - If YES: Allocate to HRSGs
4. OUTPUT: Combined Power + Steam Result
"""

import calendar

from services.power_service import distribute_by_priority
from services.steam_service import (
    calculate_steam_balance,
    get_hrsg_availability_from_dispatch,
    calculate_shp_generation_capacity,
    check_shp_balance,
    HRSG_ASSETS
)
from services.norms_save_service import save_calculated_norms, print_save_summary


POWER_FAILURE_THRESHOLD_MW = 50.0
STEAM_FAILURE_THRESHOLD_MT = 100.0


def _get_month_hours(month: int, year: int) -> int:
    return calendar.monthrange(year, month)[1] * 24


def _build_power_balance_suggestion(power_dispatch: list, power_diff_mw: float) -> str:
    headroom_suggestions = []
    for asset in power_dispatch:
        asset_name = asset.get("AssetName", "Asset")
        current_load = float(asset.get("LoadMW", 0) or 0)
        max_capacity = float(asset.get("CapacityMW", 0) or 0)
        headroom = max_capacity - current_load
        if headroom > 0.5:
            headroom_suggestions.append((asset_name, headroom, max_capacity))

    headroom_suggestions.sort(key=lambda item: item[1], reverse=True)

    if power_diff_mw > 0 and headroom_suggestions:
        top_assets = ", ".join(
            f"{name} by up to {headroom:.2f} MW (max {max_capacity:.2f} MW)"
            for name, headroom, max_capacity in headroom_suggestions[:3]
        )
        return f"Increase generation/import headroom, especially {top_assets}."

    if power_diff_mw > 0:
        return "Increase available generation or import capacity to close the power shortfall."

    if headroom_suggestions:
        top_assets = ", ".join(
            f"{name} by up to {headroom:.2f} MW"
            for name, headroom, _ in headroom_suggestions[:3]
        )
        return f"Reduce generation/import allocation, especially {top_assets}, to remove excess power."

    return "Reduce generation or import allocation to align power supply with demand."


def _build_steam_balance_suggestion(steam_type: str, imbalance_mt: float, utility_consumption: dict, steam_result: dict) -> str:
    steam_type_upper = steam_type.upper()

    if steam_type_upper == "SHP":
        hrsg_sources = []
        for index in [1, 2, 3]:
            generated_mt = float(utility_consumption.get(f"shp_from_hrsg{index}", 0) or 0)
            hrsg_name = f"HRSG{index}"
            max_capacity = float(HRSG_ASSETS.get(hrsg_name, {}).get("max_capacity_mt", 0) or 0)
            if max_capacity > 0:
                hrsg_sources.append((hrsg_name, generated_mt, max_capacity))

        if imbalance_mt > 0 and hrsg_sources:
            suggestions = []
            month_hours = _get_month_hours(steam_result.get("month", 1) or 1, steam_result.get("year", 2000) or 2000)
            for hrsg_name, generated_mt, max_capacity in hrsg_sources:
                avg_generation_mt_hr = generated_mt / month_hours if month_hours > 0 else 0
                headroom = max_capacity - avg_generation_mt_hr
                if headroom > 0.5:
                    suggestions.append(f"{hrsg_name} by up to {headroom:.2f} MT/hr")
            if suggestions:
                return f"Increase SHP generation from {', '.join(suggestions[:3])} or reduce SHP consumers like STG/PRDS demand."
            return "Increase SHP generation from available HRSG capacity or reduce SHP-consuming loads."

        return "Reduce SHP generation or supplementary firing from HRSGs to align with SHP demand."

    if steam_type_upper == "HP":
        return "Adjust HP PRDS output from SHP to match HP demand more closely."

    if steam_type_upper == "MP":
        if imbalance_mt > 0:
            return "Increase MP from PRDS or STG extraction, or reduce MP demand, to close the MP shortfall."
        return "Reduce MP PRDS output or STG extraction to remove excess MP steam."

    if steam_type_upper == "LP":
        if imbalance_mt > 0:
            return "Increase LP from PRDS or STG extraction, or reduce LP demand, to close the LP shortfall."
        return "Reduce LP PRDS output or STG extraction to remove excess LP steam."

    return f"Adjust {steam_type_upper} steam generation sources to align with demand."


def _get_power_balance_totals(usd_result: dict) -> dict:
    power_result = usd_result.get("power_result", {}) or {}
    total_demand_mwh = float(power_result.get("totalDemandUnits", 0) or 0)
    total_supply_mwh = float(power_result.get("totalNetGeneration", 0) or 0) + float(power_result.get("importUnits", 0) or 0)

    return {
        "demand_mwh": total_demand_mwh,
        "generation_mwh": total_supply_mwh,
        "difference_mwh": total_demand_mwh - total_supply_mwh,
    }


def _get_steam_balance_totals(usd_result: dict, steam_type: str) -> dict:
    final_steam_balance = usd_result.get("final_steam_balance", {}) or {}
    steam_type_lower = steam_type.lower()
    steam_balance = final_steam_balance.get(f"{steam_type_lower}_balance", {}) or {}

    if steam_type == "SHP":
        hrsg_dispatch = usd_result.get("hrsg_dispatch", {}) or {}
        demand_mt = float(steam_balance.get("shp_total_demand", 0) or 0)

        # Total SHP generation = free steam + supplementary firing for all HRSGs
        generation_mt = 0.0
        for hrsg_data in hrsg_dispatch.get("hrsg_dispatch", []) or []:
            generation_mt += float(hrsg_data.get("free_steam_mt", 0) or 0)
            generation_mt += float(hrsg_data.get("dispatched_supp_mt", 0) or 0)

        return {
            "demand_mt": demand_mt,
            "generation_mt": generation_mt,
            "difference_mt": demand_mt - generation_mt,
        }

    if steam_type == "HP":
        demand_mt = float(steam_balance.get("hp_total", 0) or 0)
        generation_mt = float(steam_balance.get("hp_from_prds", 0) or 0)
    elif steam_type == "MP":
        demand_mt = float(steam_balance.get("mp_total", 0) or 0)
        generation_mt = float(steam_balance.get("mp_from_prds", 0) or 0) + float(steam_balance.get("mp_from_stg", 0) or 0)
    else:
        demand_mt = float(steam_balance.get("lp_total", 0) or 0)
        generation_mt = float(steam_balance.get("lp_from_prds", 0) or 0) + float(steam_balance.get("lp_from_stg", 0) or 0)

    return {
        "demand_mt": demand_mt,
        "generation_mt": generation_mt,
        "difference_mt": demand_mt - generation_mt,
    }


def _evaluate_balance_failure(result: dict) -> dict | None:
    usd_result = result.get("usd_result", {}) or {}
    utility_consumption = result.get("utility_consumption", {}) or {}
    power_dispatch = usd_result.get("final_dispatch", []) or []
    month = result.get("month")
    year = result.get("year")
    month_hours = _get_month_hours(month, year) if month and year else 1

    failures = []

    power_totals = _get_power_balance_totals(usd_result)
    power_difference_mwh = power_totals["difference_mwh"]
    power_difference_mw = power_difference_mwh / month_hours if month_hours > 0 else power_difference_mwh

    if abs(power_difference_mw) > POWER_FAILURE_THRESHOLD_MW:
        if power_difference_mw > 0:
            reason = (
                f"Power balance failed: demand exceeds supply by {power_difference_mw:.2f} MW average "
                f"({power_difference_mwh:.2f} MWh over the month)."
            )
        else:
            reason = (
                f"Power balance failed: supply exceeds demand by {abs(power_difference_mw):.2f} MW average "
                f"({abs(power_difference_mwh):.2f} MWh over the month)."
            )
        failures.append({
            "type": "POWER_BALANCE_FAILURE",
            "message": reason,
            "suggestion": _build_power_balance_suggestion(power_dispatch, power_difference_mw),
            "difference": round(power_difference_mw, 2),
            "unit": "MW"
        })

    for steam_type in ["SHP", "HP", "MP", "LP"]:
        steam_totals = _get_steam_balance_totals(usd_result, steam_type)
        difference_mt = steam_totals["difference_mt"]
        if abs(difference_mt) > STEAM_FAILURE_THRESHOLD_MT:
            if difference_mt > 0:
                reason = f"{steam_type} steam balance failed: demand exceeds supply by {difference_mt:.2f} MT."
            else:
                reason = f"{steam_type} steam balance failed: supply exceeds demand by {abs(difference_mt):.2f} MT."
            failures.append({
                "type": f"{steam_type}_STEAM_BALANCE_FAILURE",
                "message": reason,
                "suggestion": _build_steam_balance_suggestion(steam_type, difference_mt, utility_consumption, {"month": month, "year": year}),
                "difference": round(difference_mt, 2),
                "unit": "MT"
            })

    if not failures:
        return None

    primary_failure = failures[0]
    combined_message = " | ".join(failure["message"] for failure in failures)
    combined_suggestion = " | ".join(failure["suggestion"] for failure in failures)

    return {
        "error_type": primary_failure["type"],
        "message": combined_message,
        "suggestion": combined_suggestion,
        "failures": failures
    }


# ============================================================
# STEP 1: POWER DEMAND & CAPACITY CHECK
# ============================================================
def check_power_demand_and_dispatch(month: int, year: int, cpp_plant_id: str) -> dict:
    """
    Execute power demand check and dispatch.
    
    Args:
        month: Month number (1-12)
        year: Financial year
        cpp_plant_id: CPP Plant UUID (required for fetching import power)
    
    Returns:
        dict with:
        - success: bool
        - error_type: str (if failed)
        - message: str
        - dispatch_plan: list (if successful)
        - power_result: full result from power_service
    """
    result = distribute_by_priority(month, year, cpp_plant_id)
    
    # Case 1: FYM not found or no assets
    if "dispatchPlan" not in result and \
       not result.get("insufficientCapacity") and \
       not result.get("insufficientCapacityAfterImport"):
        return {
            "success": False,
            "error_type": "DATA_ERROR",
            "message": result.get("message", "Unknown error"),
            "dispatch_plan": [],
            "power_result": result
        }
    
    # Case 2: Insufficient capacity (before dispatch)
    if result.get("insufficientCapacity"):
        return {
            "success": False,
            "error_type": "CAPACITY_INSUFFICIENT",
            "message": result.get("message"),
            "dispatch_plan": [],
            "power_result": result
        }
    
    # Case 3: Insufficient capacity even after import
    if result.get("insufficientCapacityAfterImport"):
        return {
            "success": False,
            "error_type": "IMPORT_INSUFFICIENT",
            "message": result.get("message"),
            "dispatch_plan": result.get("dispatchPlan", []),
            "power_result": result
        }
    
    # Case 4: Success - demand can be fulfilled
    return {
        "success": True,
        "error_type": None,
        "message": "Power demand fulfilled successfully",
        "dispatch_plan": result.get("dispatchPlan", []),
        "power_result": result
    }


# ============================================================
# STEP 2: STEAM DEMAND & CAPACITY CHECK
# ============================================================
def check_steam_demand_and_capacity(
    lp_process: float,
    lp_fixed: float,
    mp_process: float,
    mp_fixed: float,
    hp_process: float,
    hp_fixed: float,
    shp_process: float,
    shp_fixed: float,
    power_dispatch: list,
    bfw_ufu: float = 0.0,
    stg_shp_power: float = 0.0
) -> dict:
    """
    Execute steam demand calculation and capacity check.
    
    Returns:
        dict with:
        - success: bool
        - error_type: str (if failed)
        - message: str
        - steam_balance: dict
        - hrsg_availability: dict
        - shp_capacity: dict
        - shp_balance_check: dict
    """
    # Step 2.1: Calculate steam balance (demand side)
    steam_balance = calculate_steam_balance(
        lp_process=lp_process,
        lp_fixed=lp_fixed,
        mp_process=mp_process,
        mp_fixed=mp_fixed,
        hp_process=hp_process,
        hp_fixed=hp_fixed,
        shp_process=shp_process,
        shp_fixed=shp_fixed,
        bfw_ufu=bfw_ufu,
        stg_shp_power=stg_shp_power
    )
    
    # Step 2.2: Get HRSG availability from power dispatch
    hrsg_availability = get_hrsg_availability_from_dispatch(power_dispatch)
    
    # Step 2.3: Calculate SHP generation capacity
    shp_capacity = calculate_shp_generation_capacity(hrsg_availability)
    
    # Step 2.4: Check SHP balance
    shp_demand = steam_balance["summary"]["total_shp_demand"]
    shp_balance_check = check_shp_balance(shp_demand, shp_capacity)
    
    # Step 2.5: Check if capacity is sufficient
    if not shp_balance_check["can_meet_demand"]:
        # Check if supplementary firing can help (future enhancement)
        # For now, return error
        return {
            "success": False,
            "error_type": "SHP_CAPACITY_INSUFFICIENT",
            "message": (
                f"SHP demand ({shp_demand:.2f} MT) exceeds capacity "
                f"({shp_capacity['total_max_shp_capacity']:.2f} MT). "
                f"Deficit: {shp_balance_check['deficit']:.2f} MT. "
                f"Available HRSGs: {', '.join(shp_capacity['available_hrsgs']) or 'None'}. "
                f"Consider supplementary firing or reducing demand."
            ),
            "steam_balance": steam_balance,
            "hrsg_availability": hrsg_availability,
            "shp_capacity": shp_capacity,
            "shp_balance_check": shp_balance_check
        }
    
    # Success
    return {
        "success": True,
        "error_type": None,
        "message": "Steam demand can be fulfilled",
        "steam_balance": steam_balance,
        "hrsg_availability": hrsg_availability,
        "shp_capacity": shp_capacity,
        "shp_balance_check": shp_balance_check
    }


# ============================================================
# MAIN: COMPLETE BUDGET CALCULATION
# ============================================================
def calculate_budget(
    month: int,
    year: int,
    cpp_plant_id: str,
    lp_process: float,
    lp_fixed: float,
    mp_process: float,
    mp_fixed: float,
    hp_process: float,
    hp_fixed: float,
    shp_process: float,
    shp_fixed: float,
    bfw_ufu: float = 0.0,
    # Process utility consumption (Excel-matched defaults)
    air_process: float = 6095102.0,   # Compressed Air consumed by process plants (NM3)
    cw1_process: float = 15194.0,     # Cooling Water 1 consumed by process plants (KM3)
    cw2_process: float = 9016.0,      # Cooling Water 2 consumed by process plants (KM3)
    dm_process: float = 54779.0,       # DM Water consumed by process plants (M3)
    dm_fixed: float = 0.0,             # DM Water fixed consumption (M3)
    air_fixed: float = 0.0,            # Compressed Air fixed consumption (NM3)
    cw1_fixed: float = 0.0,            # Cooling Water 1 fixed consumption (KM3)
    cw2_fixed: float = 0.0,            # Cooling Water 2 fixed consumption (KM3)
    raw_water_process: float = 0.0,    # Raw Water consumed by process plants (M3)
    raw_water_fixed: float = 0.0,      # Raw Water fixed consumption (M3)
) -> dict:
    """
    Complete budget calculation following the flowchart.
    
    FLOW:
    1. Check Power Demand & Dispatch
    2. If Power OK → Check Steam Demand & Capacity
    3. Return combined result
    
    Args:
        month, year: Financial period
        cpp_plant_id: CPP Plant UUID (required for fetching import power)
        lp/mp/hp/shp_process/fixed: Steam demands (MT)
        bfw_ufu: BFW for UFU (M3)
        cw1_process: Cooling Water 1 process demand (KM3)
        cw2_process: Cooling Water 2 process demand (KM3)
    
    Returns:
        dict with complete budget result
    """
    result = {
        "month": month,
        "year": year,
        "power_check": None,
        "steam_check": None,
        "overall_success": False,
        "errors": []
    }
    
    # =========================================================
    # STEP 1: POWER DEMAND & CAPACITY CHECK
    # =========================================================
    print("\n" + "="*60)
    print("STEP 1: POWER DEMAND & CAPACITY CHECK")
    print("="*60)
    
    power_check = check_power_demand_and_dispatch(month, year, cpp_plant_id)
    result["power_check"] = power_check
    
    if not power_check["success"]:
        print(f"❌ POWER CHECK FAILED: {power_check['error_type']}")
        print(f"   {power_check['message']}")
        result["errors"].append({
            "stage": "POWER",
            "error_type": power_check["error_type"],
            "message": power_check["message"]
        })
        return result
    
    print("✅ POWER CHECK PASSED")
    print(f"   Total Demand: {power_check['power_result'].get('totalDemandUnits', 'N/A')} units")
    print(f"   Total Generation: {power_check['power_result'].get('totalNetGeneration', 'N/A')} units")
    print(f"   Assets Dispatched: {len(power_check['dispatch_plan'])}")
    
    # =========================================================
    # STEP 2: STEAM DEMAND & CAPACITY CHECK
    # =========================================================
    print("\n" + "="*60)
    print("STEP 2: STEAM DEMAND & CAPACITY CHECK")
    print("="*60)
    
    # Calculate STG SHP power requirement from power dispatch
    # STG requires 0.0036 MT SHP per KWh generated
    stg_shp_power = 0.0
    stg_gross_mwh = 0.0
    stg_gross_kwh = 0.0
    stg_power_aux = 0.0
    
    for asset in power_check["dispatch_plan"]:
        if "STG" in asset.get("AssetName", "").upper():
            # Get pre-calculated values from power_service dispatch
            stg_shp_power = asset.get("STG_SHP_Required_MT") or 0.0
            stg_power_aux = asset.get("STG_Power_Required_MWh") or 0.0
            stg_gross_mwh = asset.get("GrossMWh", 0)
            stg_gross_kwh = stg_gross_mwh * 1000  # Convert MWh to KWh
            
            # If not pre-calculated, calculate here (fallback)
            if stg_shp_power == 0.0 and stg_gross_mwh > 0:
                stg_shp_power = stg_gross_kwh * 0.0036  # 0.0036 MT SHP per KWh
                stg_power_aux = stg_gross_kwh * 0.0020 / 1000  # 0.0020 KWh per KWh, convert to MWh
            break
    
    # Log STG requirements in detail
    print("\n--- STG POWER GENERATION REQUIREMENTS ---")
    print(f"   STG Gross Generation:          {stg_gross_mwh:>12.2f} MWh")
    print(f"   STG Gross Generation:          {stg_gross_kwh:>12.2f} KWh")
    print(f"   ─────────────────────────────────────────────")
    print(f"   Norms (per 1 KWh generated):")
    print(f"     Power Required:              {0.0020:>12.4f} KWh/KWh")
    print(f"     SHP Steam Required:          {0.0036:>12.4f} MT/KWh")
    print(f"   ─────────────────────────────────────────────")
    print(f"   Calculation:")
    print(f"     Power = {stg_gross_kwh:.2f} KWh × 0.0020 = {stg_power_aux * 1000:.2f} KWh = {stg_power_aux:.2f} MWh")
    print(f"     SHP   = {stg_gross_kwh:.2f} KWh × 0.0036 = {stg_shp_power:.2f} MT SHP")
    print(f"   ─────────────────────────────────────────────")
    print(f"   STG Power Aux Requirement:     {stg_power_aux:>12.2f} MWh")
    print(f"   STG SHP Steam Requirement:     {stg_shp_power:>12.2f} MT SHP")
    
    steam_check = check_steam_demand_and_capacity(
        lp_process=lp_process,
        lp_fixed=lp_fixed,
        mp_process=mp_process,
        mp_fixed=mp_fixed,
        hp_process=hp_process,
        hp_fixed=hp_fixed,
        shp_process=shp_process,
        shp_fixed=shp_fixed,
        power_dispatch=power_check["dispatch_plan"],
        bfw_ufu=bfw_ufu,
        stg_shp_power=stg_shp_power
    )
    result["steam_check"] = steam_check
    
    if not steam_check["success"]:
        print(f"❌ STEAM CHECK FAILED: {steam_check['error_type']}")
        print(f"   {steam_check['message']}")
        result["errors"].append({
            "stage": "STEAM",
            "error_type": steam_check["error_type"],
            "message": steam_check["message"]
        })
        return result
    
    print("✅ STEAM CHECK PASSED")
    summary = steam_check["steam_balance"]["summary"]
    shp_balance = steam_check["steam_balance"]["shp_balance"]
    
    print(f"\n   LP Demand:                     {summary['total_lp_demand']:>12.2f} MT LP")
    print(f"   MP Demand:                     {summary['total_mp_demand']:>12.2f} MT MP")
    print(f"   HP Demand:                     {summary['total_hp_demand']:>12.2f} MT HP")
    
    # Detailed SHP Demand Breakdown
    print(f"\n   --- SHP DEMAND BREAKDOWN ---")
    print(f"   SHP Process Demand:            {shp_balance['shp_process']:>12.2f} MT SHP")
    print(f"   SHP Fixed Demand:              {shp_balance['shp_fixed']:>12.2f} MT SHP")
    print(f"   SHP for STG→LP:                {shp_balance['shp_for_stg_lp']:>12.2f} MT SHP")
    print(f"   SHP from MP chain:             {shp_balance['shp_from_mp_chain']:>12.2f} MT SHP")
    print(f"   SHP for PRDS→HP:               {shp_balance['shp_for_hp_prds']:>12.2f} MT SHP")
    print(f"   ─────────────────────────────────────────────")
    print(f"   SHP from Headers:              {shp_balance['shp_from_headers']:>12.2f} MT SHP")
    print(f"   SHP Total (without STG power): {shp_balance['shp_total_without_power']:>12.2f} MT SHP")
    print(f"   STG SHP for Power Generation:  {shp_balance['stg_shp_power']:>12.2f} MT SHP  ← From STG")
    print(f"   ─────────────────────────────────────────────")
    print(f"   TOTAL SHP DEMAND:              {summary['total_shp_demand']:>12.2f} MT SHP")
    
    print(f"\n   --- SHP CAPACITY ---")
    print(f"   SHP Max Capacity:              {steam_check['shp_capacity']['total_max_shp_capacity']:>12.2f} MT SHP")
    print(f"   Utilization:                   {steam_check['shp_balance_check']['utilization_percent']:>12.2f} %")
    
    # =========================================================
    # SUCCESS: Both checks passed
    # =========================================================
    result["overall_success"] = True
    
    print("\n" + "="*60)
    print("✅ BUDGET CALCULATION COMPLETE - ALL CHECKS PASSED")
    print("="*60)
    
    return result


# ============================================================
# PRINT DETAILED RESULTS
# ============================================================
def print_detailed_results(result: dict):
    """Print detailed results from budget calculation."""
    
    if not result["overall_success"]:
        print("\n" + "="*60)
        print("❌ BUDGET CALCULATION FAILED")
        print("="*60)
        for error in result["errors"]:
            print(f"\nStage: {error['stage']}")
            print(f"Error: {error['error_type']}")
            print(f"Message: {error['message']}")
        return
    
    # Power Details
    print("\n" + "="*60)
    print("POWER DISPATCH DETAILS")
    print("="*60)
    power = result["power_check"]["power_result"]
    print(f"Total Demand: {power.get('totalDemandUnits'):.2f} MWh")
    print(f"Total Gross Generation: {power.get('totalGrossGeneration'):.2f} MWh")
    print(f"Total Aux Consumption: {power.get('totalAuxConsumption'):.2f} MWh")
    print(f"Total Net Generation: {power.get('totalNetGeneration'):.2f} MWh")
    print(f"Iterations: {power.get('iterationsUsed')}, Converged: {power.get('converged')}")
    
    print("\nAsset Dispatch:")
    for asset in result["power_check"]["dispatch_plan"]:
        print(f"  {asset['AssetName']}: {asset['GrossMWh']:.2f} MWh (Load: {asset.get('LoadMW', 0):.2f} MW)")
    
    # Steam Details
    print("\n" + "="*60)
    print("STEAM BALANCE DETAILS")
    print("="*60)
    steam = result["steam_check"]["steam_balance"]
    
    print("\nLP Balance:")
    for k, v in steam["lp_balance"].items():
        print(f"  {k}: {v}")
    
    print("\nMP Balance:")
    for k, v in steam["mp_balance"].items():
        print(f"  {k}: {v}")
    
    print("\nHP Balance:")
    for k, v in steam["hp_balance"].items():
        print(f"  {k}: {v}")
    
    print("\nSHP Balance:")
    for k, v in steam["shp_balance"].items():
        print(f"  {k}: {v}")
    
    print("\nBFW Requirement:")
    for k, v in steam["bfw_requirement"].items():
        print(f"  {k}: {v}")
    
    # HRSG Availability
    print("\n" + "="*60)
    print("HRSG AVAILABILITY (Linked to GT)")
    print("="*60)
    for hrsg, data in result["steam_check"]["hrsg_availability"].items():
        status = "✅ AVAILABLE" if data["is_available"] else "❌ UNAVAILABLE"
        print(f"{hrsg}: {status}")
        if data["is_available"]:
            print(f"  Hours: {data['operational_hours']} hrs, GT Load: {data['gt_load_mw']:.2f} MW")
    
    # SHP Capacity Check
    print("\n" + "="*60)
    print("SHP CAPACITY CHECK")
    print("="*60)
    shp_check = result["steam_check"]["shp_balance_check"]
    print(f"SHP Demand: {shp_check['shp_demand']} MT")
    print(f"SHP Max Capacity: {shp_check['shp_max_capacity']} MT")
    print(f"SHP Min Capacity: {shp_check['shp_min_capacity']} MT")
    print(f"Utilization: {shp_check['utilization_percent']}%")
    print(f"Surplus: {shp_check['surplus']} MT")


# ============================================================
# MAIN: COMPLETE BUDGET WITH USD ITERATION
# ============================================================
def calculate_budget_with_iteration(
    month: int,
    year: int,
    cpp_plant_id: str,
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
    # Process utility consumption (Excel-matched defaults)
    air_process: float = 6095102.0,   # Compressed Air consumed by process plants (NM3)
    cw1_process: float = 15194.0,     # Cooling Water 1 consumed by process plants (KM3)
    cw2_process: float = 9016.0,      # Cooling Water 2 consumed by process plants (KM3)
    dm_process: float = 54779.0,       # DM Water consumed by process plants (M3)
    dm_fixed: float = 0.0,             # DM Water fixed consumption (M3)
    air_fixed: float = 0.0,            # Compressed Air fixed consumption (NM3)
    cw1_fixed: float = 0.0,            # Cooling Water 1 fixed consumption (KM3)
    cw2_fixed: float = 0.0,            # Cooling Water 2 fixed consumption (KM3)
    raw_water_process: float = 0.0,    # Raw Water consumed by process plants (M3)
    raw_water_fixed: float = 0.0,      # Raw Water fixed consumption (M3)
    oxygen_mt: float = 0.0,            # Oxygen consumed by process plants (MT)
    save_to_db: bool = False,          # Auto-save calculated values to NormsMonthDetail
    hrsg_full_load: bool = False,      # If true, load HRSG without subtracting free steam
) -> dict:
    """
    Complete budget calculation with USD iteration following the flowchart.
    
    FLOW:
    1. Power Dispatch (with iteration for net demand)
    2. Steam Balance Calculation
    3. USD Iteration Loop:
       - Check SHP demand vs capacity
       - Adjust supplementary firing
       - If needed, reduce STG and use import
       - Iterate until converged (0.1% tolerance)
    4. Output final balanced result
    
    Args:
        month, year: Financial period
        cpp_plant_id: CPP Plant UUID (required for fetching import power)
        lp/mp/hp/shp_process/fixed: Steam demands (MT)
        bfw_ufu: BFW for UFU (M3)
        export_available: Whether export power is available
        save_to_db: If True, auto-save calculated values to NormsMonthDetail table
    
    Returns:
        dict with complete budget result including USD iteration
    """
    from services.iteration_service import usd_iterate
    from services.power_service import _VERBOSE_LOGGING
    
    verbose = _VERBOSE_LOGGING
    
    if verbose:
        print("\n" + "="*100)
        print("  BUDGET CALCULATION")
        print("="*100)
        print(f"  Period: {month}/{year}")
        print(f"  Plant ID: {cpp_plant_id}")
        print(f"  Export: {'YES' if export_available else 'NO'}")
    
    # Execute USD Iteration
    usd_result = usd_iterate(
        month=month,
        year=year,
        cpp_plant_id=cpp_plant_id,
        lp_process=lp_process,
        lp_fixed=lp_fixed,
        mp_process=mp_process,
        mp_fixed=mp_fixed,
        hp_process=hp_process,
        hp_fixed=hp_fixed,
        shp_process=shp_process,
        shp_fixed=shp_fixed,
        bfw_ufu=bfw_ufu,
        export_available=export_available,
        dm_process=dm_process,
        dm_fixed=dm_fixed,
        hrsg_full_load=hrsg_full_load
    )
    
    # Print summary if verbose
    if verbose and usd_result.get('error_type'):
        print(f"\n  [ERROR] {usd_result.get('error_type')}: {usd_result.get('message')}")
    
    # Print final results summary if verbose
    if verbose:
        print("\n" + "="*100)
        print("  BUDGET SUMMARY")
        print("="*100)
        
        # Power Summary
        if usd_result.get("power_result"):
            power = usd_result["power_result"]
            print(f"  Power: Demand={power.get('totalDemandUnits', 0):,.2f} MWh, Gen={power.get('totalNetGeneration', 0):,.2f} MWh")
        
        # Steam Summary
        if usd_result.get("final_steam_balance"):
            steam = usd_result["final_steam_balance"]
            summary = steam.get("summary", {})
            print(f"  Steam: SHP Demand={summary.get('total_shp_demand', 0):,.2f} MT")
        
        # SHP Capacity
        if usd_result.get("final_shp_capacity"):
            shp_cap = usd_result["final_shp_capacity"]
            can_meet = usd_result.get("final_shp_balance", {}).get('can_meet_demand', False)
            print(f"  SHP: Capacity={shp_cap.get('total_max_shp_capacity', 0):,.2f} MT, {'CAN MEET' if can_meet else 'CANNOT MEET'}")
        
        print("="*100)
    
    # =========================================================
    # UTILITY CONSUMPTION CALCULATION
    # =========================================================
    from services.utility_service import calculate_utilities_from_dispatch, print_utility_summary, print_utility_output_summary, print_budget_summary
    
    # Extract dispatch data for utility calculation (including HeatRate from power dispatch)
    gt1_gross = 0.0
    gt2_gross = 0.0
    gt3_gross = 0.0
    stg_gross = 0.0
    gt1_avail = False
    gt2_avail = False
    gt3_avail = False
    gt1_heat_rate = None
    gt2_heat_rate = None
    gt3_heat_rate = None
    gt1_free_steam_factor = None
    gt2_free_steam_factor = None
    gt3_free_steam_factor = None
    
    for asset in usd_result.get("final_dispatch", []):
        asset_name = str(asset.get('AssetName', '')).upper()
        gross = asset.get('GrossMWh', 0)
        heat_rate = asset.get('HeatRate')  # Already calculated in power dispatch (KCAL/KWH)
        free_steam_factor = asset.get('FreeSteam')  # FreeSteamFactor from HeatRateLookup
        # Correct mapping: Plant-1 = GT1, Plant-2 = GT2, Plant-3 = GT3
        if 'GT1' in asset_name or 'PLANT-1' in asset_name:
            gt1_gross = gross
            gt1_avail = gross > 0
            gt1_heat_rate = heat_rate
            gt1_free_steam_factor = free_steam_factor
        elif 'GT2' in asset_name or 'PLANT-2' in asset_name:
            gt2_gross = gross
            gt2_avail = gross > 0
            gt2_heat_rate = heat_rate
            gt2_free_steam_factor = free_steam_factor
        elif 'GT3' in asset_name or 'PLANT-3' in asset_name:
            gt3_gross = gross
            gt3_avail = gross > 0
            gt3_heat_rate = heat_rate
            gt3_free_steam_factor = free_steam_factor
        elif 'STG' in asset_name or 'STEAM TURBINE' in asset_name:
            stg_gross = gross
    
    # Get steam balance data
    shp_from_hrsg2 = 0.0
    shp_from_hrsg3 = 0.0
    hp_from_prds = hp_process + hp_fixed
    mp_from_prds = 0.0
    lp_from_prds = 0.0
    lp_from_stg = 0.0
    mp_from_stg = 0.0
    
    if usd_result.get("final_steam_balance"):
        steam = usd_result["final_steam_balance"]
        lp_bal = steam.get("lp_balance", {})
        mp_bal = steam.get("mp_balance", {})
        lp_from_stg = lp_bal.get("lp_from_stg", 0)
        lp_from_prds = lp_bal.get("lp_from_prds", 0)
        mp_from_stg = mp_bal.get("mp_from_stg", 0)
        mp_from_prds = mp_bal.get("mp_from_prds", 0)
    
    # Get HRSG SHP values from HRSG Dispatch (priority-based allocation)
    # HRSG SHP = Free Steam + Dispatched Supplementary Firing (total HRSG output)
    # This total is used for all downstream utility calculations (BFW, Air, Water, etc.)
    shp_from_hrsg1 = 0.0
    shp_from_hrsg2 = 0.0
    shp_from_hrsg3 = 0.0
    total_free_steam = 0.0
    hrsg1_available = False
    hrsg2_available = False
    hrsg3_available = False
    
    # Get free steam + dispatched supp firing from hrsg_dispatch result
    if usd_result.get("hrsg_dispatch"):
        hrsg_dispatch = usd_result["hrsg_dispatch"]
        total_free_steam = hrsg_dispatch.get("total_free_steam_mt", 0)
        
        hrsg_dispatch_list = hrsg_dispatch.get("hrsg_dispatch", [])
        for hrsg_data in hrsg_dispatch_list:
            hrsg_name = hrsg_data.get("name", "").upper()
            # Normalize HRSG name: remove hyphens for matching (HRSG-1 -> HRSG1)
            hrsg_name_normalized = hrsg_name.replace("-", "")
            dispatched_supp = hrsg_data.get("dispatched_supp_mt", 0)
            free_steam = hrsg_data.get("free_steam_mt", 0)
            
            # Total HRSG SHP = free steam + supplementary firing
            total_hrsg_shp = free_steam + dispatched_supp
            
            # HRSG is available only if it has free steam or dispatched supp firing
            is_available = (free_steam > 0 or dispatched_supp > 0)
            
            if 'HRSG1' in hrsg_name_normalized:
                shp_from_hrsg1 = total_hrsg_shp
                hrsg1_available = is_available
            elif 'HRSG2' in hrsg_name_normalized:
                shp_from_hrsg2 = total_hrsg_shp
                hrsg2_available = is_available
            elif 'HRSG3' in hrsg_name_normalized:
                shp_from_hrsg3 = total_hrsg_shp
                hrsg3_available = is_available
    else:
        # Fallback to old logic if hrsg_dispatch not available
        if usd_result.get("final_shp_capacity"):
            shp_cap = usd_result["final_shp_capacity"]
            hrsg_details = shp_cap.get("hrsg_details", [])
            for hrsg_data in hrsg_details:
                hrsg_name = hrsg_data.get("name", "").upper()
                if hrsg_data.get("is_available"):
                    # Use free steam + MIN supp firing as fallback
                    supp_min = hrsg_data.get("supp_min_mt_month", 0)
                    free_steam = hrsg_data.get("free_steam_mt", 0)
                    total_free_steam += free_steam
                    
                    if 'HRSG1' in hrsg_name:
                        shp_from_hrsg1 = free_steam + supp_min
                        hrsg1_available = True
                    elif 'HRSG2' in hrsg_name:
                        shp_from_hrsg2 = free_steam + supp_min
                        hrsg2_available = True
                    elif 'HRSG3' in hrsg_name:
                        shp_from_hrsg3 = free_steam + supp_min
                        hrsg3_available = True
    
    # Note: shp_from_hrsg values now represent total HRSG output (free steam + supplementary firing)
    # Total SHP supply = shp_from_hrsg1 + shp_from_hrsg2 + shp_from_hrsg3
    
    # Extract power result data for utility calculation
    power_result_data = usd_result.get("power_result", {})
    import_power_mwh = power_result_data.get("mandatoryImportUsed", 0)
    total_demand_mwh = power_result_data.get("totalDemandUnits", 0)
    
    # Calculate utilities
    utilities = calculate_utilities_from_dispatch(
        month=month,
        year=year,
        gt1_gross_mwh=gt1_gross,
        gt2_gross_mwh=gt2_gross,
        gt3_gross_mwh=gt3_gross,
        stg_gross_mwh=stg_gross,
        import_power_mwh=import_power_mwh,  # Import power from Rev Proc
        shp_from_hrsg1=shp_from_hrsg1,
        shp_from_hrsg2=shp_from_hrsg2,
        shp_from_hrsg3=shp_from_hrsg3,
        hp_from_prds=hp_from_prds,
        mp_from_prds=mp_from_prds,
        lp_from_prds=lp_from_prds,
        lp_from_stg=lp_from_stg,
        mp_from_stg=mp_from_stg,
        oxygen_mt=oxygen_mt,  # Oxygen process consumption
        effluent_m3=243000.0,  # Default value - can be parameterized
        air_process_nm3=air_process,  # Process compressed air consumption
        cw1_process_km3=cw1_process,  # Cooling Water 1 process consumption
        cw2_process_km3=cw2_process,  # Cooling Water 2 process consumption
        dm_process_m3=dm_process,     # Process DM water consumption
        air_fixed_nm3=air_fixed,      # Fixed Compressed Air consumption
        cw1_fixed_km3=cw1_fixed,      # Fixed Cooling Water 1 consumption
        cw2_fixed_km3=cw2_fixed,      # Fixed Cooling Water 2 consumption
        dm_fixed_m3=dm_fixed,         # Fixed DM water consumption
        raw_water_process_m3=raw_water_process,  # Process Raw Water consumption
        raw_water_fixed_m3=raw_water_fixed,      # Fixed Raw Water consumption
        gt1_heat_rate=gt1_heat_rate,  # Heat rate from power dispatch (KCAL/KWH)
        gt2_heat_rate=gt2_heat_rate,  # Heat rate from power dispatch (KCAL/KWH)
        gt3_heat_rate=gt3_heat_rate,  # Heat rate from power dispatch (KCAL/KWH)
        gt1_free_steam_factor=gt1_free_steam_factor,  # Free steam factor from HeatRateLookup
        gt2_free_steam_factor=gt2_free_steam_factor,  # Free steam factor from HeatRateLookup
        gt3_free_steam_factor=gt3_free_steam_factor,  # Free steam factor from HeatRateLookup
        total_demand_mwh=total_demand_mwh,  # Total demand including U4U
        gt1_available=gt1_avail,
        gt2_available=gt2_avail,
        gt3_available=gt3_avail,
        hrsg1_available=shp_from_hrsg1 > 0,
        hrsg2_available=shp_from_hrsg2 > 0,
        hrsg3_available=shp_from_hrsg3 > 0,
        # HRSG NG calculation from heat rate lookup (reverse calculation)
        hrsg_ng_calculation=usd_result.get("hrsg_ng_calculation"),
    )
    
    # Print utility summary
    print_utility_summary(utilities)
    
    # Print structured utility output table (NMD format)
    print_utility_output_summary(
        utilities=utilities,
        power_dispatch=usd_result.get("final_dispatch", []),
        steam_balance=usd_result.get("final_steam_balance"),
        stg_extraction=usd_result.get("stg_extraction"),
    )
    
    # Final Status
    print("\n" + "="*70)
    if usd_result["converged"]:
        print("USD ITERATION CONVERGED - BUDGET CALCULATION COMPLETE")
    else:
        print("USD ITERATION DID NOT CONVERGE")
        print(f"   Error: {usd_result.get('error_type', 'Unknown')}")
        print(f"   Message: {usd_result.get('message', 'No message')}")
    print("="*70)
    
    # Print clear budget summary at the end
    power_result = usd_result.get("power_result", {})
    print_budget_summary(
        power_dispatch=usd_result.get("final_dispatch", []),
        steam_balance=usd_result.get("final_steam_balance"),
        stg_extraction=usd_result.get("stg_extraction"),
        utilities=utilities,
        shp_capacity=usd_result.get("final_shp_capacity"),
        hrsg_min_load=usd_result.get("hrsg_min_load"),
        hrsg_dispatch=usd_result.get("hrsg_dispatch"),  # New: HRSG dispatch with actual supp firing
        shp_balance=usd_result.get("final_shp_balance"),  # New: SHP demand breakdown
        import_mwh=power_result.get("mandatoryImportUsed", 0),
        total_demand_mwh=power_result.get("totalDemandUnits", 0),
        process_demand_mwh=power_result.get("processDemand", 0),
        fixed_demand_mwh=power_result.get("fixedDemand", 0),
        u4u_power_mwh=power_result.get("u4uPower", 0),
    )
    
    # Build result dictionary first
    result = {
        "month": month,
        "year": year,
        "overall_success": usd_result.get("converged", False),
        "iterations_used": usd_result.get("iterations_used", 0),
        "tolerance_achieved": usd_result.get("tolerance_achieved", 0),
        "usd_result": usd_result,
        "power_result": usd_result.get("power_result"),
        "steam_result": usd_result.get("final_steam_balance"),
        "stg_extraction": usd_result.get("stg_extraction"),
        "shp_capacity": usd_result.get("final_shp_capacity"),
        "shp_balance": usd_result.get("final_shp_balance"),
        "final_lp_balance": usd_result.get("final_lp_balance"),  # STG load-based LP balance
        "final_mp_balance": usd_result.get("final_mp_balance"),  # STG load-based MP balance
        "utility_consumption": utilities,  # Now includes calculated utilities
        "hrsg_full_load_mode": hrsg_full_load, # Store flag for reporting
        "adjustments": {
            "supplementary_firing_mt": usd_result.get("supplementary_firing_mt", 0),
        },
        "errors": [] if usd_result.get("converged", False) else [{
            "stage": "USD_ITERATION",
            "error_type": usd_result.get("error_type"),
            "message": usd_result.get("message"),
        }]
    }

    balance_failure = _evaluate_balance_failure(result)
    if balance_failure:
        result["overall_success"] = False
        result["balance_failure"] = balance_failure
        result["errors"].append({
            "stage": "BALANCE_CHECK",
            "error_type": balance_failure["error_type"],
            "message": balance_failure["message"],
            "suggestion": balance_failure["suggestion"],
        })
        print("\n" + "=" * 70)
        print("BALANCE FAILURE DETECTED")
        print("=" * 70)
        print(f"Reason: {balance_failure['message']}")
        print(f"Suggestion: {balance_failure['suggestion']}")
        print("=" * 70)
    
    # Auto-save calculated values to database if requested
    # Note: We save even if not converged, as we have partial results that are still valid
    # The 'converged' flag in the result indicates whether iteration completed successfully
    if save_to_db and usd_result.get("final_dispatch"):
        save_result = save_calculated_norms(month, year, result, dry_run=False)
        print_save_summary(save_result)
        result["save_result"] = save_result
    
    # Set top-level message for easier status reporting
    if result.get("errors"):
        result["message"] = result["errors"][0]["message"]
    else:
        result["message"] = "Converged"

    return result


# ============================================================
# TEST
# ============================================================
if __name__ == "__main__":
    # This will fail without DB connection
    # Use for structure reference only
    print("Budget Service - Use via main.py with database connection")
