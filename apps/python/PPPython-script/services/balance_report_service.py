"""
Balance Report Service
======================
Generates Power & Utility Loop Balance Summary Excel Report.

This service creates a comprehensive monthly Excel report showing:
- Section I: Fuel Demand (GTs, STG, HRSGs with MMBTU calculations)
- Section II: Power Balance (detailed demand/supply breakdown)
- Section III: Steam Balance (SHP, HP, MP, LP with demand/supply)
- Section IV+: Other Utilities Balance (BFW, DM Water, CW, Air, Oxygen, Effluent)

The report is generated alongside calculation logs and saved in the same directory.
"""

import os
from datetime import datetime
from typing import Dict, List, Tuple, Optional
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter
from database.connection import get_connection


# ============================================================
# CONSTANTS
# ============================================================
MONTH_NAMES = {
    1: "Jan", 2: "Feb", 3: "Mar", 4: "Apr",
    5: "May", 6: "Jun", 7: "Jul", 8: "Aug",
    9: "Sep", 10: "Oct", 11: "Nov", 12: "Dec"
}

# Excel styling - Consistent color scheme across all sections
SECTION_FILL = PatternFill(start_color="4472C4", end_color="4472C4", fill_type="solid")  # Blue for section headers
SUBSECTION_FILL = PatternFill(start_color="D9E1F2", end_color="D9E1F2", fill_type="solid")  # Light blue for subsection headers
TOTAL_FILL = PatternFill(start_color="FFF2CC", end_color="FFF2CC", fill_type="solid")  # Yellow for totals

SECTION_FONT = Font(name="Calibri", size=14, bold=True, color="FFFFFF")  # White text for section headers
BOLD_FONT = Font(name="Calibri", size=11, bold=True)
NORMAL_FONT = Font(name="Calibri", size=11)

THIN_BORDER = Border(
    left=Side(style='thin'),
    right=Side(style='thin'),
    top=Side(style='thin'),
    bottom=Side(style='thin')
)


# ============================================================
# DATA EXTRACTION FUNCTIONS
# ============================================================

def get_plant_wise_demand(month: int, year: int, utility_name: str) -> List[Dict]:
    """
    Fetch plant-wise demand from CalculatedProcessDemand table.
    
    Args:
        month: Month number (1-12)
        year: Year
        utility_name: Utility name (e.g., 'Power_Dis', 'SHP Steam_Dis')
    
    Returns:
        List of dicts with plant_name and demand_value
    """
    conn = get_connection()
    cursor = conn.cursor()
    
    try:
        # Get financial year string
        if month >= 4:
            fy_string = f"{year}-{str(year + 1)[-2:]}"
        else:
            fy_string = f"{year - 1}-{str(year)[-2:]}"
        
        # Month column mapping
        month_columns = {
            1: "jan", 2: "feb", 3: "mar", 4: "apr", 5: "may", 6: "jun",
            7: "jul", 8: "aug", 9: "sep", 10: "oct", 11: "nov", 12: "dec"
        }
        month_col = month_columns.get(month, "apr")
        
        query = f"""
            SELECT 
                process_plant,
                {month_col} AS demand_value
            FROM dbo.CalculatedProcessDemand
            WHERE financial_year = ? AND cpp_utility = ?
            ORDER BY process_plant
        """
        
        cursor.execute(query, (fy_string, utility_name))
        rows = cursor.fetchall()
        
        result = []
        for row in rows:
            if row[1] and row[1] > 0:  # Only include non-zero values
                result.append({
                    'plant_name': row[0],
                    'demand_value': float(row[1])
                })
        
        return result
        
    except Exception as e:
        print(f"  [BALANCE REPORT] Error fetching plant-wise demand for {utility_name}: {e}")
        return []
    finally:
        conn.close()


def get_fixed_consumption_details(month: int, year: int, utility_name: str) -> List[Dict]:
    """
    Fetch fixed consumption with consumer and plant details.
    
    Args:
        month: Month number (1-12)
        year: Year
        utility_name: Utility name (e.g., 'Power_Dis', 'LP Steam_Dis')
    
    Returns:
        List of dicts with consumer_name, plant_name, and consumption_value
    """
    conn = get_connection()
    cursor = conn.cursor()
    
    try:
        # Get FinancialYearMonth ID
        cursor.execute("""
            SELECT Id FROM FinancialYearMonth WHERE Month = ? AND Year = ?
        """, (month, year))
        fym_row = cursor.fetchone()
        
        if not fym_row:
            return []
        
        fym_id = fym_row[0]
        
        # Query to get fixed consumption with plant details
        # Using the same structure as fixed_consumption_service.py
        query = """
            SELECT 
                cc.CostCenterName AS consumer_name,
                pm.DisplayName AS plant_name,
                ufc.ConsumptionValue AS consumption_value
            FROM UtilityFixedConsumption ufc
            JOIN NormParameters np ON ufc.NormParameter_FK_Id = np.Id
            JOIN CPPCostCenters cc ON ufc.CostCenter_FK_Id = cc.CostCenterId
            JOIN FixedConsumptionPlantMapping pm ON cc.Plant_FK_Id = pm.Id
            WHERE ufc.FinancialYearMonth_FK_Id = ? AND np.Name = ?
            ORDER BY pm.DisplayName, cc.CostCenterName
        """
        
        cursor.execute(query, (fym_id, utility_name))
        rows = cursor.fetchall()
        
        result = []
        for row in rows:
            if row[2] and row[2] > 0:  # Only include non-zero values
                result.append({
                    'consumer_name': row[0],
                    'plant_name': row[1] if row[1] else 'Unknown',
                    'consumption_value': float(row[2])
                })
        
        return result
        
    except Exception as e:
        print(f"  [BALANCE REPORT] Error fetching fixed consumption for {utility_name}: {e}")
        import traceback
        traceback.print_exc()
        return []
    finally:
        conn.close()


def get_ncv_from_fuel_availability(month: int, year: int, fuel_name: str = 'NATURAL GAS') -> float:
    """
    Fetch NCV (Net Calorific Value) from CPPFuelAvailability table for a specific month.
    
    Args:
        month: Month number (1-12)
        year: Year
        fuel_name: Fuel name (default: 'NATURAL GAS')
    
    Returns:
        NCV value in GBT (Gross Billion Therms) or 0 if not found
    """
    conn = get_connection()
    cursor = conn.cursor()
    
    try:
        # Get financial year string
        if month >= 4:
            fy_string = f"{year}-{str(year + 1)[-2:]}"
        else:
            fy_string = f"{year - 1}-{str(year)[-2:]}"
        
        # Month column mapping
        month_columns = {
            1: "Jan", 2: "Feb", 3: "Mar", 4: "Apr", 5: "May", 6: "Jun",
            7: "Jul", 8: "Aug", 9: "Sep", 10: "Oct", 11: "Nov", 12: "Dec"
        }
        month_col = month_columns.get(month, "Apr")
        
        query = f"""
            SELECT {month_col}
            FROM dbo.CPPFuelAvailability
            WHERE FinancialYear = ? AND FuelName = ?
        """
        
        cursor.execute(query, (fy_string, fuel_name))
        row = cursor.fetchone()
        
        if row and row[0]:
            return float(row[0])
        return 0.0
        
    except Exception as e:
        print(f"Error fetching NCV from CPPFuelAvailability: {e}")
        return 0.0
    finally:
        cursor.close()
        conn.close()


def get_gt_heat_rate(month: int, year: int, asset_name: str, gt_load_mw: float) -> float:
    """
    Fetch GT Heat Rate from CPP_GTHeatRate table based on allocated load.
    Uses the heat rate for the load point closest to (but not exceeding) the actual load.
    
    Args:
        month: Month number (1-12)
        year: Year
        asset_name: Asset name (e.g., 'GT-2', 'GT-3')
        gt_load_mw: GT allocated load in MW
    
    Returns:
        Final Heat Rate value or 0 if not found
    """
    conn = get_connection()
    cursor = conn.cursor()
    
    try:
        # Get financial year string
        if month >= 4:
            fy_string = f"{year}-{str(year + 1)[-2:]}"
        else:
            fy_string = f"{year - 1}-{str(year)[-2:]}"
        
        # Query heat rate table - get all load points for this asset, ordered by load
        query = """
            SELECT GTLoad, FinalHeatRate
            FROM dbo.CPP_GTHeatRate
            WHERE FinancialYear = ? AND AssetName = ?
            ORDER BY GTLoad
        """
        
        cursor.execute(query, (fy_string, asset_name))
        rows = cursor.fetchall()
        
        if not rows:
            return 0.0
        
        # Find the appropriate heat rate based on load
        # Use the heat rate for the highest load point that doesn't exceed actual load
        selected_heat_rate = 0.0
        for row in rows:
            load_point = float(row[0])
            heat_rate = float(row[1])
            
            if load_point <= gt_load_mw:
                selected_heat_rate = heat_rate
            else:
                break  # Stop when we exceed the actual load
        
        return selected_heat_rate
        
    except Exception as e:
        print(f"Error fetching GT Heat Rate: {e}")
        return 0.0
    finally:
        cursor.close()
        conn.close()


def extract_asset_availability_data(month: int, year: int, calculation_result: dict) -> Dict:
    """
    Extract asset availability and loading data for all generation assets.
    
    Returns dict with:
    - generation_assets: List of power generation assets (GTs, STG, Import)
    - steam_assets: List of steam generation assets (HRSGs)
    """
    usd_result = calculation_result.get('usd_result', {})
    final_dispatch = usd_result.get('final_dispatch', [])
    power_result = usd_result.get('power_result', {})
    
    # Get HRSG availability from steam calculation
    from services.steam_service import get_hrsg_availability_from_dispatch
    hrsg_availability = {}
    if final_dispatch:
        hrsg_availability = get_hrsg_availability_from_dispatch(final_dispatch)
    
    # Extract generation assets (GTs, STG, Import Power)
    generation_assets = []
    
    # Process GTs
    gt_names = ['GT1', 'GT2', 'GT3']
    for gt_name in gt_names:
        gt_data = None
        for asset in final_dispatch:
            asset_name = asset.get('AssetName', '')
            if gt_name in asset_name or f'Plant-{gt_name[-1]}' in asset_name or f'Plant {gt_name[-1]}' in asset_name:
                gt_data = asset
                break
        
        if gt_data:
            gross_mwh = gt_data.get('GrossMWh', 0)
            hours = gt_data.get('OperatingHours', 720)
            load_mw = gt_data.get('LoadMW', 0)
            
            # If no LoadMW, calculate from GrossMWh and Hours
            if load_mw == 0 and hours > 0:
                load_mw = gross_mwh / hours
            
            generation_assets.append({
                'asset_name': gt_name,
                'availability_hr': hours if gross_mwh > 0 else 0,
                'min_capacity': 0,  # GTs typically have 0 min (can be off)
                'max_capacity': 22,  # Typical GT capacity in MW
                'avg_load_per_hr': round(load_mw, 2)
            })
        else:
            # GT not in dispatch (not running)
            generation_assets.append({
                'asset_name': gt_name,
                'availability_hr': 0,
                'min_capacity': 0,
                'max_capacity': 22,
                'avg_load_per_hr': 0
            })
    
    # Process STG
    stg_data = None
    for asset in final_dispatch:
        asset_name = asset.get('AssetName', '')
        if 'STG' in asset_name:
            stg_data = asset
            break
    
    if stg_data:
        gross_mwh = stg_data.get('GrossMWh', 0)
        hours = stg_data.get('OperatingHours', 720)
        load_mw = stg_data.get('LoadMW', 0)
        
        if load_mw == 0 and hours > 0:
            load_mw = gross_mwh / hours
        
        generation_assets.append({
            'asset_name': 'STG',
            'availability_hr': hours if gross_mwh > 0 else 0,
            'min_capacity': 0,
            'max_capacity': 25,  # Typical STG capacity
            'avg_load_per_hr': round(load_mw, 2)
        })
    else:
        generation_assets.append({
            'asset_name': 'STG',
            'availability_hr': 0,
            'min_capacity': 0,
            'max_capacity': 25,
            'avg_load_per_hr': 0
        })
    
    # Process Import Power
    import_mwh = power_result.get('importUnits', 0)
    import_hours = 720  # Assume full month availability
    # Import allocation should be same as max since we use all import power
    import_allocation = 25 if import_mwh > 0 else 0
    
    generation_assets.append({
        'asset_name': 'Import',
        'availability_hr': import_hours if import_mwh > 0 else 0,
        'min_capacity': 0,
        'max_capacity': 25,  # Plan allocation
        'avg_load_per_hr': import_allocation
    })
    
    # Extract steam assets (HRSGs)
    # Get HRSG allocated load from fuel demand data (already calculated there)
    fuel_data = extract_fuel_demand_data(month, year, calculation_result)
    hrsg_fuel_assets = fuel_data.get('hrsg_assets', [])
    
    # Create map of HRSG allocated load from fuel demand section
    hrsg_load_map = {}
    for hrsg_fuel in hrsg_fuel_assets:
        hrsg_name = hrsg_fuel.get('asset_name', '')
        allocated_load = hrsg_fuel.get('allocated_load_mt_per_hr', 0)
        hrsg_load_map[hrsg_name] = allocated_load
    
    steam_assets = []
    hrsg_names = ['HRSG1', 'HRSG2', 'HRSG3']
    
    # Create GT to HRSG mapping
    gt_hrsg_map = {
        'HRSG1': 'GT1',
        'HRSG2': 'GT2',
        'HRSG3': 'GT3'
    }
    
    for hrsg_name in hrsg_names:
        # Get linked GT availability hours
        linked_gt = gt_hrsg_map.get(hrsg_name)
        gt_hours = 0
        
        # Find the linked GT's availability hours from generation_assets
        for gen_asset in generation_assets:
            if gen_asset['asset_name'] == linked_gt:
                gt_hours = gen_asset['availability_hr']
                break
        
        if hrsg_name in hrsg_availability:
            hrsg_data = hrsg_availability[hrsg_name]
            is_available = hrsg_data.get('is_available', False)
            min_cap = hrsg_data.get('min_capacity_mt', 60)
            max_cap = hrsg_data.get('max_capacity_mt', 130)
            
            # Get allocated load from fuel demand data
            avg_load = hrsg_load_map.get(hrsg_name, 0)
            
            steam_assets.append({
                'asset_name': hrsg_name,
                'availability_hr': gt_hours,  # Use linked GT hours
                'min_capacity': min_cap,
                'max_capacity': max_cap,
                'avg_load_per_hr': round(avg_load, 2)
            })
        else:
            # Get allocated load from fuel demand data even if HRSG availability not found
            avg_load = hrsg_load_map.get(hrsg_name, 0)
            
            steam_assets.append({
                'asset_name': hrsg_name,
                'availability_hr': gt_hours,  # Use linked GT hours even if HRSG data not available
                'min_capacity': 60,
                'max_capacity': 130,
                'avg_load_per_hr': round(avg_load, 2)
            })
    
    return {
        'generation_assets': generation_assets,
        'steam_assets': steam_assets
    }


def extract_fuel_demand_data(month: int, year: int, calculation_result: dict) -> Dict:
    """
    Extract fuel demand data for GTs, STG, and HRSGs.
    
    Args:
        month: Month number (1-12)
        year: Year
        calculation_result: Calculation result dictionary
    
    Returns dict with:
    - gt_assets: List of GT fuel consumption data
    - stg_asset: STG fuel consumption data
    - hrsg_assets: List of HRSG fuel consumption data
    """
    usd_result = calculation_result.get('usd_result', {})
    final_dispatch = usd_result.get('final_dispatch', [])
    utility_consumption = calculation_result.get('utility_consumption', {})
    natural_gas = utility_consumption.get('natural_gas', {})
    stg_extraction = calculation_result.get('stg_extraction', {})
    
    # Fetch NCV from database for this month
    ncv_gbt = get_ncv_from_fuel_availability(month, year, 'NATURAL GAS')
    
    # Get operating hours for the month (default 720 for 30-day month)
    operating_hours = 720  # Will be updated from dispatch data if available
    
    # Extract GT data - Create map from dispatch data
    gt_dispatch_map = {}
    for asset in final_dispatch:
        asset_name = asset.get('AssetName', '')
        if 'GT' in asset_name or 'Power Plant' in asset_name:
            gross_mwh = asset.get('GrossMWh', 0)
            operating_hours = asset.get('OperatingHours', 720)
            
            if 'Plant-1' in asset_name or 'Plant 1' in asset_name:
                gt_dispatch_map['GT1'] = {'gross_mwh': gross_mwh, 'hours': operating_hours}
            elif 'Plant-2' in asset_name or 'Plant 2' in asset_name:
                gt_dispatch_map['GT2'] = {'gross_mwh': gross_mwh, 'hours': operating_hours}
            elif 'Plant-3' in asset_name or 'Plant 3' in asset_name:
                gt_dispatch_map['GT3'] = {'gross_mwh': gross_mwh, 'hours': operating_hours}
    
    # Build GT assets list - ALWAYS show all 3 GTs
    gt_configs = [
        ('GT1', 'GT-1', natural_gas.get('gt1_ng_norm', 0.0094715)),
        ('GT2', 'GT-2', natural_gas.get('gt2_ng_norm', 0.0101463)),
        ('GT3', 'GT-3', natural_gas.get('gt3_ng_norm', 0.0094715))
    ]
    
    gt_assets = []
    for gt_name, gt_db_name, ng_norm in gt_configs:
        dispatch_data = gt_dispatch_map.get(gt_name, {'gross_mwh': 0, 'hours': 720})
        gross_mwh = dispatch_data['gross_mwh']
        gross_kwh = gross_mwh * 1000
        operating_hours = dispatch_data['hours']
        avg_load_mw = gross_mwh / operating_hours if operating_hours > 0 else 0
        
        # If GT is off (gross_mwh = 0), all values should be 0
        if gross_mwh == 0:
            gt_assets.append({
                'asset_name': gt_name,
                'ncv_kcal_kwh': 0,
                'quantity_mmbtu': 0,
                'allocated_load_mw': 0,
                'gross_mwh': 0,
                'heat_rate_kcal_kwh': 0
            })
        else:
            # Fetch heat rate from database based on actual load
            heat_rate_kcal_kwh = get_gt_heat_rate(month, year, gt_db_name, avg_load_mw)
            
            # If heat rate not found in DB, fall back to calculated value
            if heat_rate_kcal_kwh == 0:
                heat_rate_kcal_kwh = ng_norm * 251995.76
            
            quantity_mmbtu = gross_kwh * ng_norm
            
            gt_assets.append({
                'asset_name': gt_name,
                'ncv_kcal_kwh': ncv_gbt,  # Use NCV from database
                'quantity_mmbtu': quantity_mmbtu,
                'allocated_load_mw': avg_load_mw,
                'gross_mwh': gross_mwh,  # Total monthly generation
                'heat_rate_kcal_kwh': heat_rate_kcal_kwh  # Use heat rate from database
            })
    
    # Extract STG data - ALWAYS show STG even if not running
    stg_asset = {
        'asset_name': 'STG',
        'shp_consumed_mt': 0,
        'allocated_load_mw': 0,
        'gross_mwh': 0,
        'ncv_gbt': ncv_gbt  # Use same NCV as GTs
    }
    
    for asset in final_dispatch:
        asset_name = asset.get('AssetName', '')
        if 'STG' in asset_name:
            gross_mwh = asset.get('GrossMWh', 0)
            operating_hours = asset.get('OperatingHours', 720)
            avg_load_mw = gross_mwh / operating_hours if operating_hours > 0 else 0
            
            # STG consumes SHP steam
            shp_inlet_mt = stg_extraction.get('stg_shp_inlet_mt', 0)
            
            stg_asset = {
                'asset_name': 'STG',
                'shp_consumed_mt': shp_inlet_mt,
                'allocated_load_mw': avg_load_mw,
                'gross_mwh': gross_mwh,
                'ncv_gbt': ncv_gbt  # Use same NCV as GTs
            }
            break
    
    # Extract HRSG data - ALWAYS show all 3 HRSGs
    # Get HRSG SHP generation from utility_consumption
    hrsg_assets = []
    hrsg_configs = [
        ('HRSG1', 'shp_from_hrsg1', 'hrsg1_mmbtu', natural_gas.get('hrsg1_ng_norm', 2.8115696)),
        ('HRSG2', 'shp_from_hrsg2', 'hrsg2_mmbtu', natural_gas.get('hrsg2_ng_norm', 2.8115696)),
        ('HRSG3', 'shp_from_hrsg3', 'hrsg3_mmbtu', natural_gas.get('hrsg3_ng_norm', 2.8115696))
    ]
    
    for hrsg_name, shp_key, mmbtu_key, ng_norm_per_mt in hrsg_configs:
        # Get actual SHP generated from utility_consumption
        shp_generated_mt = utility_consumption.get(shp_key, 0)
        
        # Calculate average hourly load (MT/h) for HRSGs
        avg_load_mt_per_hr = shp_generated_mt / operating_hours if operating_hours > 0 else 0
        
        # Get pre-calculated MMBTU from natural_gas (reverse calculated from heat rate)
        quantity_mmbtu = natural_gas.get(mmbtu_key, 0)
        
        # If MMBTU is 0 but SHP is generated, calculate it manually using NG norm
        if quantity_mmbtu == 0 and shp_generated_mt > 0:
            quantity_mmbtu = shp_generated_mt * ng_norm_per_mt
        
        # Heat rate (Kcal/Kg) - convert from MMBTU/MT
        # 1 MMBTU = 251,995.76 Kcal, 1 MT = 1000 Kg
        heat_rate_kcal_kg = (ng_norm_per_mt * 251995.76) / 1000
        
        hrsg_assets.append({
            'asset_name': hrsg_name,
            'ncv_gbt': ncv_gbt,  # Use NCV from database (same as GTs)
            'quantity_mmbtu': quantity_mmbtu,  # Use pre-calculated or calculated MMBTU
            'allocated_load_mt_per_hr': avg_load_mt_per_hr,  # Average hourly load in MT/h
            'heat_rate_kcal_kg': heat_rate_kcal_kg
        })
    
    return {
        'gt_assets': gt_assets,
        'stg_asset': stg_asset,
        'hrsg_assets': hrsg_assets
    }


def extract_power_balance_data(month: int, year: int, calculation_result: dict) -> Dict:
    """
    Extract complete power balance data with detailed demand and supply breakdown.
    """
    usd_result = calculation_result.get('usd_result', {})
    power_result = usd_result.get('power_result', {})
    final_dispatch = usd_result.get('final_dispatch', [])
    utility_consumption = calculation_result.get('utility_consumption', {})
    
    # DEMAND SIDE
    demand_data = {
        'plant_requirement': get_plant_wise_demand(month, year, 'Power_Dis'),
        'fixed_requirement': get_fixed_consumption_details(month, year, 'Power_Dis'),
        'u4u_requirement': [],
        'total_demand_mwh': power_result.get('totalDemandUnits', 0)
    }
    
    # U4U (Utility for Utility) - auxiliary consumption
    u4u_power = utility_consumption.get('u4u_power', {})
    for key, value in u4u_power.items():
        if value > 0:
            # Convert key to readable name
            readable_name = key.replace('_', ' ').title()
            demand_data['u4u_requirement'].append({
                'utility_name': readable_name,
                'consumption_mwh': value / 1000  # Convert KWH to MWH
            })
    
    # SUPPLY SIDE
    supply_data = {
        'assets': [],
        'total_supply_mwh': power_result.get('totalNetGeneration', 0) + power_result.get('importUnits', 0)
    }
    
    # Extract generation by asset
    for asset in final_dispatch:
        asset_name = asset.get('AssetName', '')
        net_mwh = asset.get('NetMWh', 0)
        
        if net_mwh > 0:
            supply_data['assets'].append({
                'asset_name': asset_name,
                'net_mwh': net_mwh
            })
    
    # Add import power
    import_mwh = power_result.get('mandatoryImportUsed', 0)
    if import_mwh > 0:
        supply_data['assets'].append({
            'asset_name': 'Import Power (MEL)',
            'net_mwh': import_mwh
        })
    
    # BALANCE
    balance = demand_data['total_demand_mwh'] - supply_data['total_supply_mwh']
    
    return {
        'demand': demand_data,
        'supply': supply_data,
        'balance_mwh': balance
    }


def extract_steam_balance_data(month: int, year: int, calculation_result: dict, steam_type: str) -> Dict:
    """
    Extract steam balance data for a specific steam type (SHP, HP, MP, LP).
    
    Args:
        steam_type: One of 'SHP', 'HP', 'MP', 'LP'
    """
    usd_result = calculation_result.get('usd_result', {})
    final_steam = usd_result.get('final_steam_balance', {})
    utility_consumption = calculation_result.get('utility_consumption', {})
    stg_extraction = calculation_result.get('stg_extraction', {})
    
    steam_type_lower = steam_type.lower()
    steam_balance = final_steam.get(f'{steam_type_lower}_balance', {})
    
    # Map steam type to utility name in database
    utility_name_map = {
        'SHP': 'SHP Steam_Dis',
        'HP': 'HP Steam_Dis',
        'MP': 'MP Steam_Dis',
        'LP': 'LP Steam_Dis'
    }
    utility_name = utility_name_map.get(steam_type, f'{steam_type} Steam_Dis')
    
    # DEMAND SIDE
    demand_data = {
        'plant_requirement': get_plant_wise_demand(month, year, utility_name),
        'fixed_requirement': get_fixed_consumption_details(month, year, utility_name),
        'stg_consumption': 0,
        'prds_consumption': 0,
        'u4u_requirement': [],
        'total_demand_mt': steam_balance.get(f'{steam_type_lower}_total', 0)
    }
    
    # STG consumption (for SHP only)
    if steam_type == 'SHP':
        demand_data['stg_consumption'] = stg_extraction.get('stg_shp_inlet_mt', 0)
    
    # PRDS consumption
    if steam_type == 'SHP':
        # SHP consumed by PRDS to generate HP, MP, LP
        demand_data['prds_consumption'] = steam_balance.get('shp_to_prds', 0)
    elif steam_type == 'MP':
        # MP consumed by PRDS to generate LP
        demand_data['prds_consumption'] = steam_balance.get('mp_to_prds', 0)
    
    # SUPPLY SIDE
    supply_data = {
        'sources': [],
        'total_supply_mt': steam_balance.get(f'{steam_type_lower}_total_supply', 0) or steam_balance.get(f'total_{steam_type_lower}_supply', 0)
    }
    
    # Extract supply sources based on steam type
    if steam_type == 'SHP':
        # SHP from HRSGs (free + supplementary)
        for i in [1, 2, 3]:
            shp_from_hrsg = utility_consumption.get(f'shp_from_hrsg{i}', 0)
            if shp_from_hrsg > 0:
                supply_data['sources'].append({
                    'source_name': f'HRSG{i}',
                    'supply_mt': shp_from_hrsg
                })
    
    elif steam_type == 'HP':
        # HP from PRDS (SHP → HP)
        hp_from_prds = steam_balance.get('hp_from_prds', 0)
        if hp_from_prds > 0:
            supply_data['sources'].append({
                'source_name': 'HP PRDS (from SHP)',
                'supply_mt': hp_from_prds
            })
    
    elif steam_type == 'MP':
        # MP from PRDS (SHP → MP)
        mp_from_prds = steam_balance.get('mp_from_prds', 0)
        if mp_from_prds > 0:
            supply_data['sources'].append({
                'source_name': 'MP PRDS (from SHP)',
                'supply_mt': mp_from_prds
            })
        
        # MP from STG extraction
        mp_from_stg = stg_extraction.get('mp_from_stg', 0)
        if mp_from_stg > 0:
            supply_data['sources'].append({
                'source_name': 'STG Extraction',
                'supply_mt': mp_from_stg
            })
    
    elif steam_type == 'LP':
        # LP from PRDS (MP → LP)
        lp_from_prds = steam_balance.get('lp_from_prds', 0)
        if lp_from_prds > 0:
            supply_data['sources'].append({
                'source_name': 'LP PRDS (from MP)',
                'supply_mt': lp_from_prds
            })
        
        # LP from STG extraction
        lp_from_stg = stg_extraction.get('lp_from_stg', 0)
        if lp_from_stg > 0:
            supply_data['sources'].append({
                'source_name': 'STG Extraction',
                'supply_mt': lp_from_stg
            })
        
        # LP credit from HRSG (negative - returned to BFW)
        lp_credit = steam_balance.get('lp_credit_from_hrsg', 0)
        if lp_credit != 0:
            supply_data['sources'].append({
                'source_name': 'HRSG LP Credit (Return)',
                'supply_mt': lp_credit
            })
    
    # BALANCE
    balance = demand_data['total_demand_mt'] - supply_data['total_supply_mt']
    
    return {
        'demand': demand_data,
        'supply': supply_data,
        'balance_mt': balance
    }


def extract_utility_balance_data(month: int, year: int, calculation_result: dict, utility_name: str, utility_key: str) -> Dict:
    """
    Extract balance data for other utilities (BFW, DM Water, CW, Air, Oxygen, Effluent).
    
    Args:
        utility_name: Display name (e.g., 'Boiler Feed Water')
        utility_key: Key in utility_consumption dict (e.g., 'bfw')
    """
    utility_consumption = calculation_result.get('utility_consumption', {})
    
    # Special handling for oxygen and effluent which are stored in utility_consumption
    if utility_key == 'oxygen':
        oxygen_mt = abs(utility_consumption.get('oxygen_mt', 0))  # Use absolute value
        demand_data = {
            'plant_requirement': get_plant_wise_demand(month, year, utility_name),
            'fixed_requirement': get_fixed_consumption_details(month, year, utility_name),
            'u4u_requirement': [],
            'total_demand': oxygen_mt
        }
        supply_data = {
            'total_supply': oxygen_mt
        }
        balance = 0.0
        unit = 'MT'
    elif utility_key == 'effluent':
        effluent_m3 = abs(utility_consumption.get('effluent_m3', 0))  # Use absolute value
        demand_data = {
            'plant_requirement': get_plant_wise_demand(month, year, utility_name),
            'fixed_requirement': get_fixed_consumption_details(month, year, utility_name),
            'u4u_requirement': [],
            'total_demand': effluent_m3
        }
        supply_data = {
            'total_supply': effluent_m3
        }
        balance = 0.0
        unit = 'M3'
    else:
        # Standard handling for other utilities
        utility_data = utility_consumption.get(utility_key, {})
        
        # DEMAND SIDE - use absolute values
        total_demand = utility_data.get('total_demand', 0) or utility_data.get('total', 0)
        demand_data = {
            'plant_requirement': get_plant_wise_demand(month, year, utility_name),
            'fixed_requirement': get_fixed_consumption_details(month, year, utility_name),
            'u4u_requirement': [],
            'total_demand': abs(total_demand)  # Use absolute value
        }
        
        # SUPPLY SIDE - use absolute values
        total_supply = utility_data.get('total_m3', 0) or utility_data.get('total_mt', 0) or utility_data.get('total_nm3', 0) or utility_data.get('total_km3', 0)
        supply_data = {
            'total_supply': abs(total_supply)  # Use absolute value
        }
        
        # BALANCE
        balance = demand_data['total_demand'] - supply_data['total_supply']
        unit = utility_data.get('unit', '')
    
    return {
        'demand': demand_data,
        'supply': supply_data,
        'balance': balance,
        'unit': unit
    }


# ============================================================
# EXCEL GENERATION FUNCTIONS
# ============================================================

def create_balance_report_excel(month: int, year: int, calculation_result: dict, output_folder: str) -> str:
    """
    Generate Power & Utility Loop Balance Summary Excel report for a single month.
    
    Args:
        month: Month number (1-12)
        year: Year
        calculation_result: Calculation result dictionary from calculate_budget_with_iteration
        output_folder: Folder to save the Excel file
    
    Returns:
        Path to the generated Excel file
    """
    print(f"\n{'='*70}")
    print(f"GENERATING BALANCE SUMMARY EXCEL REPORT")
    print(f"{'='*70}")
    print(f"  Month: {MONTH_NAMES[month]} {year}")
    print(f"  Output Folder: {output_folder}")
    
    # Create workbook
    wb = Workbook()
    ws = wb.active
    ws.title = f"{MONTH_NAMES[month]}-{year}"
    
    # Column widths will be auto-fitted after all content is written
    # Initial minimum widths
    ws.column_dimensions['A'].width = 20
    ws.column_dimensions['B'].width = 12
    ws.column_dimensions['C'].width = 15
    ws.column_dimensions['D'].width = 20
    ws.column_dimensions['E'].width = 15
    
    current_row = 1
    
    # Title
    ws.merge_cells(f'A{current_row}:E{current_row}')
    title_cell = ws[f'A{current_row}']
    title_cell.value = f"Power & Utility Loop Balance Summary - {MONTH_NAMES[month]} {year}"
    title_cell.font = SECTION_FONT
    title_cell.alignment = Alignment(horizontal='center', vertical='center')
    title_cell.fill = SECTION_FILL
    current_row += 2
    
    # SECTION I: FUEL DEMAND
    current_row = write_fuel_demand_section(ws, current_row, month, year, calculation_result)
    current_row += 2
    
    # SECTION II: POWER BALANCE
    current_row = write_power_balance_section(ws, current_row, month, year, calculation_result)
    current_row += 2
    
    # SECTION III: STEAM BALANCE
    current_row = write_steam_balance_section(ws, current_row, month, year, calculation_result)
    current_row += 2
    
    # SECTION IV: OTHER UTILITIES BALANCE
    from services.other_utilities_balance_writer import write_other_utilities_balance_section
    current_row = write_other_utilities_balance_section(ws, current_row, month, year, calculation_result)
    current_row += 2
    
    # SECTION V: ASSET AVAILABILITY AND LOADING
    current_row = write_asset_availability_section(ws, current_row, month, year, calculation_result)
    
    # Auto-fit column widths based on content
    from openpyxl.cell.cell import MergedCell
    
    for column_cells in ws.columns:
        max_length = 0
        column_letter = None
        
        for cell in column_cells:
            # Skip merged cells
            if isinstance(cell, MergedCell):
                continue
            
            # Get column letter from first non-merged cell
            if column_letter is None:
                column_letter = cell.column_letter
            
            try:
                if cell.value:
                    # Calculate length considering the cell value
                    cell_length = len(str(cell.value))
                    if cell_length > max_length:
                        max_length = cell_length
            except:
                pass
        
        # Set column width with some padding (add 2 for padding)
        if column_letter:
            adjusted_width = min(max_length + 2, 50)  # Cap at 50 to prevent extremely wide columns
            ws.column_dimensions[column_letter].width = max(adjusted_width, 12)  # Minimum width of 12
    
    # Save file
    os.makedirs(output_folder, exist_ok=True)
    filename = f"Balance_Summary_{year}_{month:02d}_{MONTH_NAMES[month]}.xlsx"
    filepath = os.path.join(output_folder, filename)
    
    wb.save(filepath)
    print(f"  ✓ Excel report saved: {filepath}")
    print(f"{'='*70}\n")
    
    return filepath


def write_fuel_demand_section(ws, start_row: int, month: int, year: int, calculation_result: dict) -> int:
    """Write Section I: Fuel Demand to worksheet - Single unified table for all assets."""
    fuel_data = extract_fuel_demand_data(month, year, calculation_result)
    
    row = start_row
    
    # Section header
    ws.merge_cells(f'A{row}:F{row}')
    header_cell = ws[f'A{row}']
    header_cell.value = "SECTION I: FUEL DEMAND"
    header_cell.font = SECTION_FONT
    header_cell.fill = SECTION_FILL
    header_cell.alignment = Alignment(horizontal='left', vertical='center')
    row += 1
    
    # Single table header for all assets
    headers = ['Asset', 'Asset Type', 'NCV', 'Quantity (MMBTU)', 'Allocated Load', 'Heat Rate']
    for col_idx, header in enumerate(headers, start=1):
        cell = ws.cell(row=row, column=col_idx)
        cell.value = header
        cell.font = BOLD_FONT
        cell.fill = SUBSECTION_FILL
        cell.border = THIN_BORDER
        cell.alignment = Alignment(horizontal='center', vertical='center')
    
    # Set column widths for better formatting
    ws.column_dimensions['A'].width = 12  # Asset
    ws.column_dimensions['B'].width = 15  # Asset Type
    ws.column_dimensions['C'].width = 18  # NCV
    ws.column_dimensions['D'].width = 18  # Quantity (MMBTU)
    ws.column_dimensions['E'].width = 18  # Allocated Load
    ws.column_dimensions['F'].width = 18  # Heat Rate
    
    row += 1
    
    # GT Assets
    for gt in fuel_data['gt_assets']:
        ws[f'A{row}'] = gt['asset_name']
        ws[f'B{row}'] = 'Gas Turbine'
        ws[f'C{row}'] = f"{round(gt['ncv_kcal_kwh'], 2)} Kcal/kWh"
        ws[f'D{row}'] = round(gt['quantity_mmbtu'], 2)
        # GT allocated load is total monthly generation in MWh, not average MW
        gross_mwh = gt.get('gross_mwh', gt['allocated_load_mw'] * 720)  # Fallback calculation
        ws[f'E{row}'] = f"{round(gross_mwh, 2)} MWh"
        ws[f'F{row}'] = f"{round(gt['heat_rate_kcal_kwh'], 2)} Kcal/kWh"
        
        for col in range(1, 7):
            ws.cell(row=row, column=col).border = THIN_BORDER
        row += 1
    
    # STG Asset
    if fuel_data['stg_asset']:
        stg = fuel_data['stg_asset']
        ws[f'A{row}'] = stg['asset_name']
        ws[f'B{row}'] = 'Steam Turbine'
        ws[f'C{row}'] = f"{round(stg['ncv_gbt'], 2)} Kcal/kWh"  # Use NCV from database
        ws[f'D{row}'] = '-'
        # STG allocated load is total monthly generation in MWh
        ws[f'E{row}'] = f"{round(stg['gross_mwh'], 2)} MWh"
        ws[f'F{row}'] = f"{round(stg['shp_consumed_mt'], 2)} MT SHP"  # Move SHP to Heat Rate column
        
        for col in range(1, 7):
            ws.cell(row=row, column=col).border = THIN_BORDER
        row += 1
    
    # HRSG Assets
    for hrsg in fuel_data['hrsg_assets']:
        ws[f'A{row}'] = hrsg['asset_name']
        ws[f'B{row}'] = 'HRSG'
        ws[f'C{row}'] = f"{round(hrsg['ncv_gbt'], 2)} Kcal/kg"  # Use NCV from database (Kcal/kg for steam generation)
        ws[f'D{row}'] = round(hrsg['quantity_mmbtu'], 2)
        ws[f'E{row}'] = f"{round(hrsg['allocated_load_mt_per_hr'], 2)} MT/h"  # Average hourly load
        ws[f'F{row}'] = f"{round(hrsg['heat_rate_kcal_kg'], 2)} Kcal/kg"
        
        for col in range(1, 7):
            ws.cell(row=row, column=col).border = THIN_BORDER
        row += 1
    
    return row


def write_power_balance_section(ws, start_row: int, month: int, year: int, calculation_result: dict) -> int:
    """Write Section II: Power Balance with 5-column layout (Demand, Norm, Quantity MW, Generation, Quantity MW)."""
    power_data = extract_power_balance_data(month, year, calculation_result)
    usd_result = calculation_result.get('usd_result', {})
    final_dispatch = usd_result.get('final_dispatch', [])
    utility_consumption = calculation_result.get('utility_consumption', {})
    
    # Auxiliary power norms (from demand_service.py)
    NORM_GT_AUX = 0.0140  # 1.4% for GTs
    NORM_STG_AUX = 0.0020  # 0.2% for STG
    
    # Utility power consumption norms (KWH per unit)
    NORM_BFW_POWER = 9.5000        # KWH per M3
    NORM_DM_POWER = 1.2100         # KWH per M3
    NORM_CW1_POWER = 245.0000      # KWH per KM3
    NORM_CW2_POWER = 250.0000      # KWH per KM3
    NORM_AIR_POWER = 0.1650        # KWH per NM3
    NORM_OXYGEN_POWER = 968.6500   # KWH per MT
    NORM_EFFLUENT_POWER = 3.5400   # KWH per M3
    
    row = start_row
    
    # Section header
    ws.merge_cells(f'A{row}:E{row}')
    header_cell = ws[f'A{row}']
    header_cell.value = "SECTION II: POWER BALANCE"
    header_cell.font = SECTION_FONT
    header_cell.fill = SECTION_FILL
    header_cell.alignment = Alignment(horizontal='left', vertical='center')
    row += 1
    
    # Column headers
    headers = ['Demand', 'Norm', 'Quantity MW', 'Generation by Asset', 'Quantity MW']
    for col_idx, header in enumerate(headers, start=1):
        cell = ws.cell(row=row, column=col_idx)
        cell.value = header
        cell.font = BOLD_FONT
        cell.fill = SUBSECTION_FILL
        cell.border = THIN_BORDER
        cell.alignment = Alignment(horizontal='center', vertical='center')
    row += 1
    
    start_data_row = row
    
    # LEFT SIDE: DEMAND
    # Plant Requirement
    ws[f'A{row}'] = "Plant Requirement (Power)"
    ws[f'A{row}'].font = BOLD_FONT
    row += 1
    
    plant_req_total = 0
    for plant_data in power_data['demand']['plant_requirement']:
        ws[f'A{row}'] = f"  {plant_data['plant_name']}"
        demand_mw = plant_data['demand_value'] / 1000  # Convert KWH to MWH
        ws[f'C{row}'] = round(demand_mw, 2)
        plant_req_total += demand_mw
        for col in [1, 2, 3]:
            ws.cell(row=row, column=col).border = THIN_BORDER
        row += 1
    
    # Fixed Requirement
    ws[f'A{row}'] = "Fixed Requirement (Power)"
    ws[f'A{row}'].font = BOLD_FONT
    row += 1
    
    fixed_req_total = 0
    for fixed_data in power_data['demand']['fixed_requirement']:
        ws[f'A{row}'] = f"  {fixed_data['consumer_name']} ({fixed_data['plant_name']})"
        demand_mw = fixed_data['consumption_value'] / 1000  # Convert KWH to MWH
        ws[f'C{row}'] = round(demand_mw, 2)
        fixed_req_total += demand_mw
        for col in [1, 2, 3]:
            ws.cell(row=row, column=col).border = THIN_BORDER
        row += 1
    
    # Other Utility Requirement (Auxiliary Power for GTs)
    ws[f'A{row}'] = "Other Utility Requirement"
    ws[f'A{row}'].font = BOLD_FONT
    row += 1
    
    aux_power_total = 0
    # Extract GT and STG generation and calculate auxiliary power
    gt_generation = {'GT1': 0, 'GT2': 0, 'GT3': 0}
    stg_generation = 0
    
    for asset in final_dispatch:
        asset_name = asset.get('AssetName', '')
        gross_mwh = asset.get('GrossMWh', 0)
        
        if 'Plant-1' in asset_name or 'Plant 1' in asset_name:
            gt_generation['GT1'] = gross_mwh
        elif 'Plant-2' in asset_name or 'Plant 2' in asset_name:
            gt_generation['GT2'] = gross_mwh
        elif 'Plant-3' in asset_name or 'Plant 3' in asset_name:
            gt_generation['GT3'] = gross_mwh
        elif 'STG' in asset_name:
            stg_generation = gross_mwh
    
    # GT auxiliary power
    for gt_name in ['GT1', 'GT2', 'GT3']:
        ws[f'A{row}'] = f"  {gt_name} - Power"
        ws[f'B{row}'] = NORM_GT_AUX
        aux_mw = gt_generation[gt_name] * NORM_GT_AUX
        ws[f'C{row}'] = round(aux_mw, 2)
        aux_power_total += aux_mw
        for col in [1, 2, 3]:
            ws.cell(row=row, column=col).border = THIN_BORDER
        row += 1
    
    # STG auxiliary power
    ws[f'A{row}'] = f"  STG - Power"
    ws[f'B{row}'] = NORM_STG_AUX
    stg_aux_mw = stg_generation * NORM_STG_AUX
    ws[f'C{row}'] = round(stg_aux_mw, 2)
    aux_power_total += stg_aux_mw
    for col in [1, 2, 3]:
        ws.cell(row=row, column=col).border = THIN_BORDER
    row += 1
    
    # Utility Plant Power (U4U - Utility for Utility)
    # Extract from final_u4u_power (matches console output) instead of utility_consumption
    # This ensures Excel shows the exact same values as the console log
    final_u4u_power = usd_result.get('final_u4u_power', {})
    utility_power_from_u4u = final_u4u_power.get('utility_power', {})
    
    # If final_u4u_power is not available (backward compatibility), fall back to utility_consumption
    if not utility_power_from_u4u:
        utility_power_from_u4u = utility_consumption.get('utility_power', {})
    
    utility_power_items = [
        ('BFW Plant', utility_power_from_u4u.get('bfw_kwh', 0) / 1000, NORM_BFW_POWER),  # Convert KWH to MWH
        ('DM Water Plant', utility_power_from_u4u.get('dm_kwh', 0) / 1000, NORM_DM_POWER),
        ('Cooling Water 1', utility_power_from_u4u.get('cw1_kwh', 0) / 1000, NORM_CW1_POWER),
        ('Cooling Water 2', utility_power_from_u4u.get('cw2_kwh', 0) / 1000, NORM_CW2_POWER),
        ('Compressed Air', utility_power_from_u4u.get('air_kwh', 0) / 1000, NORM_AIR_POWER),
        ('Oxygen Plant', utility_power_from_u4u.get('oxygen_kwh', 0) / 1000, NORM_OXYGEN_POWER),
        ('Effluent Treatment', utility_power_from_u4u.get('effluent_kwh', 0) / 1000, NORM_EFFLUENT_POWER),
    ]
    
    for utility_name, power_mwh, norm_value in utility_power_items:
        if power_mwh > 0:
            ws[f'A{row}'] = f"  {utility_name}"
            ws[f'B{row}'] = norm_value  # Add norm value
            ws[f'C{row}'] = round(power_mwh, 2)
            aux_power_total += power_mwh
            for col in [1, 2, 3]:
                ws.cell(row=row, column=col).border = THIN_BORDER
            row += 1
    
    # Get actual total from calculation result
    usd_power = usd_result.get('power_result', {})
    total_demand_actual = usd_power.get('totalDemandUnits', 0)
    
    # Since we're now using final_u4u_power values, aux_power_total matches the engine's u4uPower
    # No reconciliation needed - the values are identical
    line_items_sum = plant_req_total + fixed_req_total + aux_power_total
    
    # RIGHT SIDE: GENERATION (start from top)
    gen_row = start_data_row
    
    # Generation assets
    generation_total = 0
    asset_generations = []
    
    # Extract all generation assets
    for asset in final_dispatch:
        asset_name = asset.get('AssetName', '')
        net_mwh = asset.get('NetMWh', 0)
        
        if 'Plant-1' in asset_name or 'Plant 1' in asset_name:
            asset_generations.append(('GT1', net_mwh))
        elif 'Plant-2' in asset_name or 'Plant 2' in asset_name:
            asset_generations.append(('GT2', net_mwh))
        elif 'Plant-3' in asset_name or 'Plant 3' in asset_name:
            asset_generations.append(('GT3', net_mwh))
        elif 'STG' in asset_name:
            asset_generations.append(('STG', net_mwh))
    
    # Add import power
    import_mwh = usd_power.get('mandatoryImportUsed', 0)
    if import_mwh > 0:
        asset_generations.append(('Import Power', import_mwh))
    
    # Write generation data
    for asset_name, gen_mwh in asset_generations:
        ws[f'D{gen_row}'] = asset_name
        ws[f'E{gen_row}'] = round(gen_mwh, 2)
        generation_total += gen_mwh
        for col in [4, 5]:
            ws.cell(row=gen_row, column=col).border = THIN_BORDER
        gen_row += 1
    
    # Summary rows at bottom
    row = max(row, gen_row) + 1
    
    # Use actual total from calculation result
    total_demand = total_demand_actual
    
    # Total row
    ws[f'A{row}'] = "Total Power Demand"
    ws[f'C{row}'] = round(total_demand, 2)
    ws[f'D{row}'] = "Total Power Generation"
    ws[f'E{row}'] = round(generation_total, 2)
    for col in [1, 2, 3, 4, 5]:
        ws.cell(row=row, column=col).font = BOLD_FONT
        ws.cell(row=row, column=col).fill = TOTAL_FILL
        ws.cell(row=row, column=col).border = THIN_BORDER
    row += 1
    
    # Imbalance row
    imbalance = generation_total - total_demand
    ws[f'A{row}'] = "Power Imbalance"
    ws[f'C{row}'] = round(imbalance, 2)
    for col in [1, 2, 3]:
        ws.cell(row=row, column=col).font = BOLD_FONT
        ws.cell(row=row, column=col).fill = TOTAL_FILL
        ws.cell(row=row, column=col).border = THIN_BORDER
    row += 1
    
    return row + 1


def write_steam_balance_section(ws, start_row: int, month: int, year: int, calculation_result: dict) -> int:
    """Write Section III: Steam Balance with 5-column layout for LP, MP, HP, SHP."""
    row = start_row
    
    # Section header
    ws.merge_cells(f'A{row}:E{row}')
    header_cell = ws[f'A{row}']
    header_cell.value = "SECTION III: STEAM BALANCE"
    header_cell.font = SECTION_FONT
    header_cell.fill = SECTION_FILL
    header_cell.alignment = Alignment(horizontal='left', vertical='center')
    row += 1
    
    # Write balance for each steam type in order: LP → MP → HP → SHP
    for steam_type in ['LP', 'MP', 'HP', 'SHP']:
        row = write_single_steam_balance(ws, row, month, year, calculation_result, steam_type)
        row += 1  # Add spacing between steam types
    
    return row


def write_single_steam_balance(ws, start_row: int, month: int, year: int, calculation_result: dict, steam_type: str) -> int:
    """Write a single steam type balance with 5-column layout (Demand, Norm, Quantity MT, Generation, Quantity MT)."""
    usd_result = calculation_result.get('usd_result', {})
    final_steam = usd_result.get('final_steam_balance', {})
    stg_extraction = calculation_result.get('stg_extraction', {})
    utility_consumption = calculation_result.get('utility_consumption', {})
    
    steam_type_lower = steam_type.lower()
    steam_balance = final_steam.get(f'{steam_type_lower}_balance', {})
    
    # Map steam type to utility name in database
    utility_name_map = {
        'SHP': 'SHP Steam_Dis',
        'HP': 'HP Steam_Dis',
        'MP': 'MP Steam_Dis',
        'LP': 'LP Steam_Dis'
    }
    utility_name = utility_name_map.get(steam_type, f'{steam_type} Steam_Dis')
    
    row = start_row
    
    # Subsection header
    ws.merge_cells(f'A{row}:E{row}')
    header_cell = ws[f'A{row}']
    header_cell.value = f"{steam_type} STEAM BALANCE"
    header_cell.font = Font(bold=True, size=11)
    header_cell.fill = SUBSECTION_FILL
    header_cell.alignment = Alignment(horizontal='left', vertical='center')
    row += 1
    
    # Column headers
    headers = ['Demand', 'Norm', 'Quantity MT', 'Generation by Asset', 'Quantity MT']
    for col_idx, header in enumerate(headers, start=1):
        cell = ws.cell(row=row, column=col_idx)
        cell.value = header
        cell.font = BOLD_FONT
        cell.fill = SUBSECTION_FILL
        cell.border = THIN_BORDER
        cell.alignment = Alignment(horizontal='center', vertical='center')
    row += 1
    
    start_data_row = row
    
    # LEFT SIDE: DEMAND
    # Plant Requirement
    ws[f'A{row}'] = f"Plant Requirement ({steam_type} Steam)"
    ws[f'A{row}'].font = BOLD_FONT
    row += 1
    
    plant_req_total = 0
    plant_requirements = get_plant_wise_demand(month, year, utility_name)
    for plant_data in plant_requirements:
        ws[f'A{row}'] = f"  {plant_data['plant_name']}"
        # Steam is already in MT in database, no conversion needed
        # Only Power (KWH) needs conversion to MWH
        demand_mt = plant_data['demand_value']
        ws[f'C{row}'] = round(demand_mt, 2)
        plant_req_total += demand_mt
        for col in [1, 2, 3]:
            ws.cell(row=row, column=col).border = THIN_BORDER
        row += 1
    
    # Fixed Requirement
    ws[f'A{row}'] = f"Fixed Requirement ({steam_type} Steam)"
    ws[f'A{row}'].font = BOLD_FONT
    row += 1
    
    fixed_req_total = 0
    fixed_requirements = get_fixed_consumption_details(month, year, utility_name)
    for fixed_data in fixed_requirements:
        ws[f'A{row}'] = f"  {fixed_data['consumer_name']} ({fixed_data['plant_name']})"
        # Steam is already in MT in database, no conversion needed
        consumption_mt = fixed_data['consumption_value']
        ws[f'C{row}'] = round(consumption_mt, 2)
        fixed_req_total += consumption_mt
        for col in [1, 2, 3]:
            ws.cell(row=row, column=col).border = THIN_BORDER
        row += 1
    
    # Other Utility Requirement (U4U for steam)
    ws[f'A{row}'] = "Other Utility Requirement"
    ws[f'A{row}'].font = BOLD_FONT
    row += 1
    
    u4u_total = 0
    
    # Add steam-specific U4U items based on steam type
    if steam_type == 'SHP':
        # SHP consumed by STG for power generation (not extraction)
        # Use stg_shp_power from SHP balance, not stg_shp_inlet_mt which includes extraction
        stg_shp_power = steam_balance.get('stg_shp_power', 0)
        if stg_shp_power > 0:
            ws[f'A{row}'] = "  STG Power Generation"
            ws[f'C{row}'] = round(stg_shp_power, 2)
            u4u_total += stg_shp_power
            for col in [1, 2, 3]:
                ws.cell(row=row, column=col).border = THIN_BORDER
            row += 1
        
        # SHP for LP via STG
        shp_for_lp_stg = steam_balance.get('shp_for_stg_lp', 0)
        if shp_for_lp_stg > 0:
            ws[f'A{row}'] = "  LP Steam (via STG)"
            ws[f'C{row}'] = round(shp_for_lp_stg, 2)
            u4u_total += shp_for_lp_stg
            for col in [1, 2, 3]:
                ws.cell(row=row, column=col).border = THIN_BORDER
            row += 1
        
        # SHP for MP components - extract from MP balance
        mp_balance = final_steam.get('mp_balance', {})
        
        # SHP for MP via STG
        shp_for_mp_stg = mp_balance.get('shp_for_stg_mp', 0)
        if shp_for_mp_stg > 0:
            ws[f'A{row}'] = "  MP Steam (via STG)"
            ws[f'C{row}'] = round(shp_for_mp_stg, 2)
            u4u_total += shp_for_mp_stg
            for col in [1, 2, 3]:
                ws.cell(row=row, column=col).border = THIN_BORDER
            row += 1
        
        # SHP for MP via PRDS
        shp_for_mp_prds = mp_balance.get('shp_for_prds_mp', 0)
        if shp_for_mp_prds > 0:
            ws[f'A{row}'] = "  MP Steam (via PRDS)"
            ws[f'C{row}'] = round(shp_for_mp_prds, 2)
            u4u_total += shp_for_mp_prds
            for col in [1, 2, 3]:
                ws.cell(row=row, column=col).border = THIN_BORDER
            row += 1
        
        # SHP for HP via PRDS
        shp_for_hp_prds = steam_balance.get('shp_for_hp_prds', 0)
        if shp_for_hp_prds > 0:
            ws[f'A{row}'] = "  HP Steam (via PRDS)"
            ws[f'C{row}'] = round(shp_for_hp_prds, 2)
            u4u_total += shp_for_hp_prds
            for col in [1, 2, 3]:
                ws.cell(row=row, column=col).border = THIN_BORDER
            row += 1
    
    elif steam_type == 'MP':
        # MP consumed by PRDS to generate LP
        mp_for_lp = steam_balance.get('mp_for_lp', 0)
        if mp_for_lp > 0:
            ws[f'A{row}'] = "  LP Steam (via PRDS)"
            ws[f'C{row}'] = round(mp_for_lp, 2)
            u4u_total += mp_for_lp
            for col in [1, 2, 3]:
                ws.cell(row=row, column=col).border = THIN_BORDER
            row += 1
    
    # Calculate adjustment between line items and actual calculation total
    usd_steam = usd_result.get('final_steam_balance', {})
    steam_result = usd_steam.get(f'{steam_type_lower}_balance', {})
    
    # SHP uses 'shp_total_demand' key, others use '{type}_total'
    if steam_type == 'SHP':
        total_demand_actual = steam_result.get('shp_total_demand', 0)
    else:
        total_demand_actual = steam_result.get(f'{steam_type_lower}_total', 0)
    
    line_items_sum = plant_req_total + fixed_req_total + u4u_total
    
    # Verify the line items sum matches the calculation total
    # With unit conversion fixed, these should match exactly
    if abs(total_demand_actual - line_items_sum) > 0.01:
        print(f"[WARNING] {steam_type} Steam Balance mismatch:")
        print(f"  Line items sum: {line_items_sum:.2f} MT")
        print(f"  Calculation total: {total_demand_actual:.2f} MT")
        print(f"  Difference: {total_demand_actual - line_items_sum:.2f} MT")
    
    # RIGHT SIDE: GENERATION (start from top)
    gen_row = start_data_row
    
    # Generation sources
    generation_total = 0
    
    # Extract supply sources based on steam type
    if steam_type == 'SHP':
        # SHP from HRSGs
        hrsg_dispatch = usd_result.get('hrsg_dispatch', {})
        hrsg_dispatch_list = hrsg_dispatch.get('hrsg_dispatch', [])
        
        for hrsg_data in hrsg_dispatch_list:
            hrsg_name = hrsg_data.get('name', '')
            total_shp_mt = hrsg_data.get('total_shp_mt', 0)
            if total_shp_mt > 0:
                ws[f'D{gen_row}'] = hrsg_name
                ws[f'E{gen_row}'] = round(total_shp_mt, 2)
                generation_total += total_shp_mt
                for col in [4, 5]:
                    ws.cell(row=gen_row, column=col).border = THIN_BORDER
                gen_row += 1
    
    elif steam_type == 'HP':
        # HP from PRDS (SHP → HP)
        hp_from_prds = steam_balance.get('hp_from_prds', 0)
        if hp_from_prds > 0:
            ws[f'D{gen_row}'] = "HP PRDS (from SHP)"
            ws[f'E{gen_row}'] = round(hp_from_prds, 2)
            generation_total += hp_from_prds
            for col in [4, 5]:
                ws.cell(row=gen_row, column=col).border = THIN_BORDER
            gen_row += 1
    
    elif steam_type == 'MP':
        # MP from PRDS (SHP → MP)
        mp_from_prds = steam_balance.get('mp_from_prds', 0)
        if mp_from_prds > 0:
            ws[f'D{gen_row}'] = "MP PRDS (from SHP)"
            ws[f'E{gen_row}'] = round(mp_from_prds, 2)
            generation_total += mp_from_prds
            for col in [4, 5]:
                ws.cell(row=gen_row, column=col).border = THIN_BORDER
            gen_row += 1
        
        # MP from STG extraction
        mp_from_stg = steam_balance.get('mp_from_stg', 0)
        if mp_from_stg > 0:
            ws[f'D{gen_row}'] = "STG Extraction"
            ws[f'E{gen_row}'] = round(mp_from_stg, 2)
            generation_total += mp_from_stg
            for col in [4, 5]:
                ws.cell(row=gen_row, column=col).border = THIN_BORDER
            gen_row += 1
    
    elif steam_type == 'LP':
        # LP from PRDS (MP → LP)
        lp_from_prds = steam_balance.get('lp_from_prds', 0)
        if lp_from_prds > 0:
            ws[f'D{gen_row}'] = "LP PRDS (from MP)"
            ws[f'E{gen_row}'] = round(lp_from_prds, 2)
            generation_total += lp_from_prds
            for col in [4, 5]:
                ws.cell(row=gen_row, column=col).border = THIN_BORDER
            gen_row += 1
        
        # LP from STG extraction
        lp_from_stg = steam_balance.get('lp_from_stg', 0)
        if lp_from_stg > 0:
            ws[f'D{gen_row}'] = "STG Extraction"
            ws[f'E{gen_row}'] = round(lp_from_stg, 2)
            generation_total += lp_from_stg
            for col in [4, 5]:
                ws.cell(row=gen_row, column=col).border = THIN_BORDER
            gen_row += 1
    
    # Summary rows at bottom
    row = max(row, gen_row) + 1
    
    # Use actual total from calculation result
    total_demand = total_demand_actual
    
    # Total row
    ws[f'A{row}'] = f"Total {steam_type} Steam Demand"
    ws[f'C{row}'] = round(total_demand, 2)
    ws[f'D{row}'] = f"Total {steam_type} Steam Generation"
    ws[f'E{row}'] = round(generation_total, 2)
    for col in [1, 2, 3, 4, 5]:
        ws.cell(row=row, column=col).font = BOLD_FONT
        ws.cell(row=row, column=col).fill = TOTAL_FILL
        ws.cell(row=row, column=col).border = THIN_BORDER
    row += 1
    
    # Imbalance row
    imbalance = generation_total - total_demand
    ws[f'A{row}'] = f"{steam_type} Steam Imbalance"
    ws[f'C{row}'] = round(imbalance, 2)
    for col in [1, 2, 3]:
        ws.cell(row=row, column=col).font = BOLD_FONT
        ws.cell(row=row, column=col).fill = TOTAL_FILL
        ws.cell(row=row, column=col).border = THIN_BORDER
    row += 1
    
    return row


def write_asset_availability_section(ws, start_row: int, month: int, year: int, calculation_result: dict) -> int:
    """Write Section V: Asset Availability and Loading to worksheet."""
    asset_data = extract_asset_availability_data(month, year, calculation_result)
    
    row = start_row
    
    # Section header
    ws.merge_cells(f'A{row}:E{row}')
    header_cell = ws[f'A{row}']
    header_cell.value = "SECTION V: ASSET AVAILABILITY AND LOADING"
    header_cell.font = SECTION_FONT
    header_cell.fill = SECTION_FILL
    header_cell.alignment = Alignment(horizontal='left', vertical='center')
    row += 1
    
    # Generation Assets subsection
    ws.merge_cells(f'A{row}:E{row}')
    subheader_cell = ws[f'A{row}']
    subheader_cell.value = "Generation Assets"
    subheader_cell.font = Font(bold=True, size=11)
    subheader_cell.fill = SUBSECTION_FILL
    subheader_cell.alignment = Alignment(horizontal='left', vertical='center')
    row += 1
    
    # Column headers for generation assets
    gen_headers = ['Asset', 'Availability, Hr', 'Min', 'Max', 'Plan / Allocation by loop']
    for col_idx, header in enumerate(gen_headers, start=1):
        cell = ws.cell(row=row, column=col_idx)
        cell.value = header
        cell.font = BOLD_FONT
        cell.fill = SUBSECTION_FILL
        cell.border = THIN_BORDER
        cell.alignment = Alignment(horizontal='center', vertical='center')
    row += 1
    
    # Write generation assets data
    for asset in asset_data['generation_assets']:
        ws[f'A{row}'] = asset['asset_name']
        ws[f'B{row}'] = asset['availability_hr']
        ws[f'C{row}'] = asset['min_capacity']
        ws[f'D{row}'] = asset['max_capacity']
        ws[f'E{row}'] = asset['avg_load_per_hr']
        
        for col in [1, 2, 3, 4, 5]:
            ws.cell(row=row, column=col).border = THIN_BORDER
            ws.cell(row=row, column=col).alignment = Alignment(horizontal='center', vertical='center')
        
        row += 1
    
    # Add spacing
    row += 1
    
    # Steam Assets subsection
    ws.merge_cells(f'A{row}:E{row}')
    subheader_cell = ws[f'A{row}']
    subheader_cell.value = "Steam Assets"
    subheader_cell.font = Font(bold=True, size=11)
    subheader_cell.fill = SUBSECTION_FILL
    subheader_cell.alignment = Alignment(horizontal='left', vertical='center')
    row += 1
    
    # Column headers for steam assets
    steam_headers = ['Asset', 'Availability, Hr', 'Min', 'Max', 'Plan / Allocation by loop']
    for col_idx, header in enumerate(steam_headers, start=1):
        cell = ws.cell(row=row, column=col_idx)
        cell.value = header
        cell.font = BOLD_FONT
        cell.fill = SUBSECTION_FILL
        cell.border = THIN_BORDER
        cell.alignment = Alignment(horizontal='center', vertical='center')
    row += 1
    
    # Write steam assets data
    for asset in asset_data['steam_assets']:
        ws[f'A{row}'] = asset['asset_name']
        ws[f'B{row}'] = asset['availability_hr']
        ws[f'C{row}'] = asset['min_capacity']
        ws[f'D{row}'] = asset['max_capacity']
        ws[f'E{row}'] = asset['avg_load_per_hr']
        
        for col in [1, 2, 3, 4, 5]:
            ws.cell(row=row, column=col).border = THIN_BORDER
            ws.cell(row=row, column=col).alignment = Alignment(horizontal='center', vertical='center')
        
        row += 1
    
    return row


def write_other_utilities_section(ws, start_row: int, month: int, year: int, calculation_result: dict) -> int:
    """Write Section IV+: Other Utilities Balance to worksheet."""
    row = start_row
    
    # Section header
    ws.merge_cells(f'A{row}:C{row}')
    header_cell = ws[f'A{row}']
    header_cell.value = "SECTION IV+: OTHER UTILITIES BALANCE"
    header_cell.font = SECTION_FONT
    header_cell.fill = SECTION_FILL
    header_cell.alignment = Alignment(horizontal='left', vertical='center')
    row += 1
    
    # Define utilities to include
    utilities = [
        ('Boiler Feed Water', 'bfw', 'M3'),
        ('D M Water', 'dm_water', 'M3'),
        ('Cooling Water 1', 'cooling_water', 'KM3'),
        ('Cooling Water 2', 'cooling_water', 'KM3'),
        ('COMPRESSED AIR', 'compressed_air', 'NM3'),
        ('Oxygen', 'oxygen', 'MT'),
        ('Effluent Treated', 'effluent', 'M3')
    ]
    
    for utility_name, utility_key, unit in utilities:
        utility_data = extract_utility_balance_data(month, year, calculation_result, utility_name, utility_key)
        
        ws[f'A{row}'] = utility_name
        ws[f'A{row}'].font = BOLD_FONT
        ws[f'A{row}'].fill = SUBSECTION_FILL
        row += 1
        
        # Total Demand
        ws[f'A{row}'] = "  Total Demand"
        ws[f'B{row}'] = round(utility_data['demand']['total_demand'], 2)
        ws[f'C{row}'] = unit
        ws[f'A{row}'].font = BOLD_FONT
        row += 1
        
        # Total Supply
        ws[f'A{row}'] = "  Total Supply"
        ws[f'B{row}'] = round(utility_data['supply']['total_supply'], 2)
        ws[f'C{row}'] = unit
        ws[f'A{row}'].font = BOLD_FONT
        row += 1
        
        # Balance
        ws[f'A{row}'] = "  Imbalance"
        ws[f'B{row}'] = round(utility_data['balance'], 2)
        ws[f'C{row}'] = unit
        ws[f'A{row}'].font = BOLD_FONT
        row += 2
    
    return row


# ============================================================
# SECTION IV: OTHER UTILITIES BALANCE
# ============================================================

def get_utility_norm_from_db(month: int, year: int, plant_name: str, utility_name: str, material_name: str) -> float:
    """
    Fetch norm value from NormsMonthDetail table for a specific plant, utility, and material.
    
    Args:
        month: Month number (1-12)
        year: Year
        plant_name: Plant name (e.g., 'NMD - Utility Plant')
        utility_name: Utility name (e.g., 'Boiler Feed Water')
        material_name: Material name (e.g., 'D M Water', 'Power_Dis')
    
    Returns:
        Norm value from database, or 0.0 if not found
    """
    conn = get_connection()
    cursor = conn.cursor()
    
    try:
        query = """
            SELECT nmd.Norms
            FROM NormsMonthDetail nmd
            INNER JOIN NormsHeader nh ON nh.Id = nmd.NormsHeader_FK_Id
            INNER JOIN Plants p ON p.Id = nh.Plant_FK_Id
            INNER JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
            WHERE fym.Month = ? AND fym.Year = ?
              AND p.Name = ?
              AND nh.UtilityName = ?
              AND nh.MaterialName = ?
              AND nh.IsActive = 1
        """
        
        cursor.execute(query, (month, year, plant_name, utility_name, material_name))
        row = cursor.fetchone()
        
        if row and row[0] is not None:
            return float(row[0])
        return 0.0
        
    except Exception as e:
        print(f"  [NORM FETCH] Error fetching norm for {plant_name}/{utility_name}/{material_name}: {e}")
        return 0.0
    finally:
        conn.close()
