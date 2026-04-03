"""
Other Utilities Balance Extraction
====================================
Extract balance data for BFW, DM Water, Cooling Water, Compressed Air, Oxygen, Effluent, and Raw Water.
This module provides extraction functions for Section IV: Other Utilities Balance.
"""

from typing import Dict, List
from services.balance_report_service import get_plant_wise_demand, get_fixed_consumption_details, get_utility_norm_from_db


def extract_bfw_balance_data(month: int, year: int, calculation_result: dict) -> Dict:
    """
    Extract BFW (Boiler Feed Water) balance data.
    
    Demand sources:
    - HRSG1, HRSG2, HRSG3 (for SHP generation)
    - HP PRDS, MP PRDS, LP PRDS
    - Process consumption (from database)
    - Fixed consumption (from database)
    
    Supply:
    - BFW Plant Production = Total Demand
    """
    utility_consumption = calculation_result.get('utility_consumption', {})
    bfw_data = utility_consumption.get('bfw', {})
    
    # U4U Demand components
    u4u_items = []
    
    # HRSG BFW consumption
    hrsg1_bfw = bfw_data.get('hrsg1_m3', 0)
    hrsg2_bfw = bfw_data.get('hrsg2_m3', 0)
    hrsg3_bfw = bfw_data.get('hrsg3_m3', 0)
    
    # Fetch norms from database
    hrsg1_norm = get_utility_norm_from_db(month, year, 'NMD - Utility Plant', 'HRSG1_SHP STEAM', 'Boiler Feed Water')
    hrsg2_norm = get_utility_norm_from_db(month, year, 'NMD - Utility Plant', 'HRSG2_SHP STEAM', 'Boiler Feed Water')
    hrsg3_norm = get_utility_norm_from_db(month, year, 'NMD - Utility Plant', 'HRSG3_SHP STEAM', 'Boiler Feed Water')
    
    u4u_items.append({'name': 'HRSG1', 'quantity': hrsg1_bfw, 'norm': hrsg1_norm})
    u4u_items.append({'name': 'HRSG2', 'quantity': hrsg2_bfw, 'norm': hrsg2_norm})
    u4u_items.append({'name': 'HRSG3', 'quantity': hrsg3_bfw, 'norm': hrsg3_norm})
    
    # PRDS BFW consumption
    hp_prds_bfw = bfw_data.get('hp_prds_m3', 0)
    mp_prds_bfw = bfw_data.get('mp_prds_m3', 0)
    lp_prds_bfw = bfw_data.get('lp_prds_m3', 0)
    
    hp_prds_norm = get_utility_norm_from_db(month, year, 'NMD - Utility Plant', 'HP Steam PRDS', 'Boiler Feed Water')
    mp_prds_norm = get_utility_norm_from_db(month, year, 'NMD - Utility Plant', 'MP Steam PRDS SHP', 'Boiler Feed Water')
    lp_prds_norm = get_utility_norm_from_db(month, year, 'NMD - Utility Plant', 'LP Steam PRDS', 'Boiler Feed Water')
    
    u4u_items.append({'name': 'HP PRDS', 'quantity': hp_prds_bfw, 'norm': hp_prds_norm})
    u4u_items.append({'name': 'MP PRDS', 'quantity': mp_prds_bfw, 'norm': mp_prds_norm})
    u4u_items.append({'name': 'LP PRDS', 'quantity': lp_prds_bfw, 'norm': lp_prds_norm})
    
    # Process and Fixed demands (from database)
    process_items = get_plant_wise_demand(month, year, 'Boiler Feed Water')
    fixed_items = get_fixed_consumption_details(month, year, 'Boiler Feed Water')
    
    total_demand = bfw_data.get('total_m3', 0)
    
    return {
        'demand': {
            'u4u_items': u4u_items,
            'process_items': process_items,
            'fixed_items': fixed_items,
            'total': total_demand
        },
        'supply': {
            'plant_production': total_demand
        },
        'balance': 0.0,  # Should always be balanced
        'unit': 'M3'
    }


def extract_dm_water_balance_data(month: int, year: int, calculation_result: dict) -> Dict:
    """
    Extract DM Water balance data.
    
    Demand sources:
    - For BFW production (BFW × norm) - U4U
    - Process consumption (from calculation) - Process
    - Fixed consumption (from database) - Fixed
    
    Supply:
    - DM Water Plant Production = Total Demand
    
    Note: Process demand is stored in utility_consumption, not fetched from database separately,
    because it's already included in the total calculation.
    """
    utility_consumption = calculation_result.get('utility_consumption', {})
    dm_data = utility_consumption.get('dm_water', {})
    
    # U4U Demand components
    u4u_items = []
    
    # DM for BFW
    dm_for_bfw = dm_data.get('for_bfw_m3', 0)
    dm_bfw_norm = get_utility_norm_from_db(month, year, 'NMD - Utility Plant', 'Boiler Feed Water', 'D M Water')
    
    u4u_items.append({'name': 'For BFW Production', 'quantity': dm_for_bfw, 'norm': dm_bfw_norm})
    
    # Process demand - always fetch plant-wise breakdown from database
    process_items = get_plant_wise_demand(month, year, 'D M Water')
    
    # Fixed demands
    fixed_items = get_fixed_consumption_details(month, year, 'D M Water')
    
    total_demand = dm_data.get('total_m3', 0)
    
    return {
        'demand': {
            'u4u_items': u4u_items,
            'process_items': process_items,
            'fixed_items': fixed_items,
            'total': total_demand
        },
        'supply': {
            'plant_production': total_demand
        },
        'balance': 0.0,
        'unit': 'M3'
    }


def extract_cooling_water_1_balance_data(month: int, year: int, calculation_result: dict) -> Dict:
    """
    Extract Cooling Water 1 balance data.
    
    Demand sources:
    - Process consumption (from database)
    
    Supply:
    - CW1 Plant Production = Total Demand
    """
    utility_consumption = calculation_result.get('utility_consumption', {})
    cw_data = utility_consumption.get('cooling_water', {})
    up_data = utility_consumption.get('utility_plant', {})
    raw_water_data = utility_consumption.get('raw_water', {})
    air_data = utility_consumption.get('compressed_air', {})

    # U4U Demand components for CW1
    u4u_items = []

    # Power consumed by CW1 plant (convert KWH to MWH for display)
    cw1_power_kwh = up_data.get('cw1_kwh', 0)
    cw1_power_norm = get_utility_norm_from_db(month, year, 'NMD - Utility Plant', 'Cooling Water 1', 'Power_Dis')
    u4u_items.append({'name': 'CW1 Power Plant', 'quantity': round(cw1_power_kwh / 1000, 2), 'norm': cw1_power_norm})

    # Raw water for CW1
    cw1_raw_water = raw_water_data.get('cw1_m3', 0)
    cw1_water_norm = get_utility_norm_from_db(month, year, 'NMD - Utility Plant', 'Cooling Water 1', 'Water')
    u4u_items.append({'name': 'CW1 Raw Water', 'quantity': cw1_raw_water, 'norm': cw1_water_norm})

    # Compressed air for CW1
    cw1_air = air_data.get('cw1_nm3', 0)
    cw1_air_norm = get_utility_norm_from_db(month, year, 'NMD - Utility Plant', 'Cooling Water 1', 'COMPRESSED AIR')
    u4u_items.append({'name': 'CW1 Compressed Air', 'quantity': cw1_air, 'norm': cw1_air_norm})

    # Process demands
    process_items = get_plant_wise_demand(month, year, 'Cooling Water 1')
    fixed_items = get_fixed_consumption_details(month, year, 'Cooling Water 1')

    total_demand = cw_data.get('cw1_total_km3', 0)

    return {
        'demand': {
            'u4u_items': u4u_items,
            'process_items': process_items,
            'fixed_items': fixed_items,
            'total': total_demand
        },
        'supply': {
            'plant_production': total_demand
        },
        'balance': 0.0,
        'unit': 'KM3'
    }


def extract_cooling_water_2_balance_data(month: int, year: int, calculation_result: dict) -> Dict:
    """
    Extract Cooling Water 2 balance data.
    
    Demand sources:
    - GT1, GT2, GT3 (fixed when operating)
    - STG (fixed when operating)
    - BFW Plant (fixed)
    - Compressed Air Plant (fixed)
    - Oxygen Plant (per MT produced)
    - Process consumption (from database)
    
    Supply:
    - CW2 Plant Production = Total Demand
    """
    utility_consumption = calculation_result.get('utility_consumption', {})
    cw_data = utility_consumption.get('cooling_water', {})
    
    # U4U Demand components
    u4u_items = []
    
    # Power plants CW2
    gt1_cw2 = cw_data.get('cw2_gt1_km3', 0)
    gt2_cw2 = cw_data.get('cw2_gt2_km3', 0)
    gt3_cw2 = cw_data.get('cw2_gt3_km3', 0)
    stg_cw2 = cw_data.get('cw2_stg_km3', 0)
    
    # Fetch norms (these are typically fixed values per month)
    gt_cw2_norm = get_utility_norm_from_db(month, year, 'NMD - Power Plant 2', 'POWERGEN', 'Cooling Water 2')
    stg_cw2_norm = get_utility_norm_from_db(month, year, 'NMD - STG Power Plant', 'POWERGEN', 'Cooling Water 2')
    
    u4u_items.append({'name': 'GT1', 'quantity': gt1_cw2, 'norm': gt_cw2_norm})
    u4u_items.append({'name': 'GT2', 'quantity': gt2_cw2, 'norm': gt_cw2_norm})
    u4u_items.append({'name': 'GT3', 'quantity': gt3_cw2, 'norm': gt_cw2_norm})
    u4u_items.append({'name': 'STG', 'quantity': stg_cw2, 'norm': stg_cw2_norm})

    # Utility plants CW2
    bfw_cw2 = cw_data.get('cw2_bfw_km3', 0)
    air_cw2 = cw_data.get('cw2_air_km3', 0)
    oxygen_cw2 = cw_data.get('cw2_oxygen_km3', 0)

    bfw_cw2_norm = get_utility_norm_from_db(month, year, 'NMD - Utility Plant', 'Boiler Feed Water', 'Cooling Water 2')
    air_cw2_norm = get_utility_norm_from_db(month, year, 'NMD - Utility Plant', 'COMPRESSED AIR', 'Cooling Water 2')
    oxygen_cw2_norm = get_utility_norm_from_db(month, year, 'NMD - Utility Plant', 'Oxygen', 'Cooling Water 2')

    u4u_items.append({'name': 'BFW Plant', 'quantity': bfw_cw2, 'norm': bfw_cw2_norm})
    u4u_items.append({'name': 'Compressed Air Plant', 'quantity': air_cw2, 'norm': air_cw2_norm})
    u4u_items.append({'name': 'Oxygen Plant', 'quantity': oxygen_cw2, 'norm': oxygen_cw2_norm})
    
    # Process and Fixed demands
    process_items = get_plant_wise_demand(month, year, 'Cooling Water 2')
    fixed_items = get_fixed_consumption_details(month, year, 'Cooling Water 2')
    
    total_demand = cw_data.get('cw2_total_km3', 0)
    
    return {
        'demand': {
            'u4u_items': u4u_items,
            'process_items': process_items,
            'fixed_items': fixed_items,
            'total': total_demand
        },
        'supply': {
            'plant_production': total_demand
        },
        'balance': 0.0,
        'unit': 'KM3'
    }


def extract_compressed_air_balance_data(month: int, year: int, calculation_result: dict) -> Dict:
    """
    Extract Compressed Air balance data.
    
    Demand sources:
    - GT1, GT2, GT3 (fixed when operating)
    - STG (fixed when operating)
    - HRSG1, HRSG2, HRSG3 (fixed when operating)
    - CW1 Plant (fixed)
    - CW2 Plant (fixed)
    - DM Water Plant (per M³ DM)
    - Process consumption (from database)
    
    Supply:
    - Compressed Air Plant Production = Total Demand
    """
    utility_consumption = calculation_result.get('utility_consumption', {})
    air_data = utility_consumption.get('compressed_air', {})
    
    # U4U Demand components
    u4u_items = []
    
    # Power plants
    gt1_air = air_data.get('gt1_nm3', 0)
    gt2_air = air_data.get('gt2_nm3', 0)
    gt3_air = air_data.get('gt3_nm3', 0)
    stg_air = air_data.get('stg_nm3', 0)
    
    gt_air_norm = get_utility_norm_from_db(month, year, 'NMD - Power Plant 2', 'POWERGEN', 'COMPRESSED AIR')
    stg_air_norm = get_utility_norm_from_db(month, year, 'NMD - STG Power Plant', 'POWERGEN', 'COMPRESSED AIR')
    
    u4u_items.append({'name': 'GT1', 'quantity': gt1_air, 'norm': gt_air_norm})
    u4u_items.append({'name': 'GT2', 'quantity': gt2_air, 'norm': gt_air_norm})
    u4u_items.append({'name': 'GT3', 'quantity': gt3_air, 'norm': gt_air_norm})
    u4u_items.append({'name': 'STG', 'quantity': stg_air, 'norm': stg_air_norm})
    
    # HRSGs
    hrsg1_air = air_data.get('hrsg1_nm3', 0)
    hrsg2_air = air_data.get('hrsg2_nm3', 0)
    hrsg3_air = air_data.get('hrsg3_nm3', 0)
    
    hrsg_air_norm = get_utility_norm_from_db(month, year, 'NMD - Utility Plant', 'HRSG2_SHP STEAM', 'COMPRESSED AIR')
    
    u4u_items.append({'name': 'HRSG1', 'quantity': hrsg1_air, 'norm': hrsg_air_norm})
    u4u_items.append({'name': 'HRSG2', 'quantity': hrsg2_air, 'norm': hrsg_air_norm})
    u4u_items.append({'name': 'HRSG3', 'quantity': hrsg3_air, 'norm': hrsg_air_norm})
    
    # Utility plants
    cw1_air = air_data.get('cw1_nm3', 0)
    cw2_air = air_data.get('cw2_nm3', 0)
    
    cw1_air_norm = get_utility_norm_from_db(month, year, 'NMD - Utility Plant', 'Cooling Water 1', 'COMPRESSED AIR')
    cw2_air_norm = get_utility_norm_from_db(month, year, 'NMD - Utility Plant', 'Cooling Water 2', 'COMPRESSED AIR')
    
    u4u_items.append({'name': 'CW1 Plant', 'quantity': cw1_air, 'norm': cw1_air_norm})
    u4u_items.append({'name': 'CW2 Plant', 'quantity': cw2_air, 'norm': cw2_air_norm})
    
    # Process and Fixed demands
    process_items = get_plant_wise_demand(month, year, 'COMPRESSED AIR')
    fixed_items = get_fixed_consumption_details(month, year, 'COMPRESSED AIR')
    
    total_demand = air_data.get('total_nm3', 0)
    
    return {
        'demand': {
            'u4u_items': u4u_items,
            'process_items': process_items,
            'fixed_items': fixed_items,
            'total': total_demand
        },
        'supply': {
            'plant_production': total_demand
        },
        'balance': 0.0,
        'unit': 'NM3'
    }


def extract_oxygen_balance_data(month: int, year: int, calculation_result: dict) -> Dict:
    """
    Extract Oxygen balance data.
    
    Demand sources:
    - Process consumption (from database)
    
    Supply:
    - Oxygen Plant Production = Total Demand
    
    Note: Nitrogen byproduct credit is not shown (no negative numbers)
    """
    # Oxygen is stored in utility_consumption dictionary
    utility_consumption = calculation_result.get('utility_consumption', {})
    oxygen_mt = abs(utility_consumption.get('oxygen_mt', 0))  # Use absolute value
    
    # Process and Fixed demands
    process_items = get_plant_wise_demand(month, year, 'Oxygen')
    fixed_items = get_fixed_consumption_details(month, year, 'Oxygen')
    
    return {
        'demand': {
            'u4u_items': [],
            'process_items': process_items,
            'fixed_items': fixed_items,
            'total': oxygen_mt
        },
        'supply': {
            'plant_production': oxygen_mt
        },
        'balance': 0.0,
        'unit': 'MT'
    }


def extract_effluent_balance_data(month: int, year: int, calculation_result: dict) -> Dict:
    """
    Extract Effluent Treatment balance data.
    
    Demand sources:
    - Process consumption (from database)
    
    Supply:
    - Effluent Treatment Plant = Total Demand
    """
    utilities = calculation_result.get('utilities', {})
    effluent_m3 = utilities.get('effluent_m3', 0)
    
    # Process and Fixed demands
    process_items = get_plant_wise_demand(month, year, 'Effluent Treated')
    fixed_items = get_fixed_consumption_details(month, year, 'Effluent Treated')
    
    return {
        'demand': {
            'u4u_items': [],
            'process_items': process_items,
            'fixed_items': fixed_items,
            'total': effluent_m3
        },
        'supply': {
            'plant_production': effluent_m3
        },
        'balance': 0.0,
        'unit': 'M3'
    }


def extract_raw_water_balance_data(month: int, year: int, calculation_result: dict) -> Dict:
    """
    Extract Raw Water balance data.
    
    Demand sources:
    - CW1 Plant (CW × norm × 53.12%)
    - CW2 Plant (CW × norm × 46.88%)
    - DM Water Plant (DM × norm)
    - HRSG2, HRSG3 (per MT SHP)
    - Effluent (per M³)
    
    Supply:
    - Raw Water Source = Total Demand
    """
    utility_consumption = calculation_result.get('utility_consumption', {})
    raw_water_data = utility_consumption.get('raw_water', {})
    
    # U4U Demand components
    u4u_items = []
    
    # CW1 and CW2 plants
    cw1_water = raw_water_data.get('cw1_m3', 0)
    cw2_water = raw_water_data.get('cw2_m3', 0)
    
    cw1_water_norm = get_utility_norm_from_db(month, year, 'NMD - Utility Plant', 'Cooling Water 1', 'Water')
    cw2_water_norm = get_utility_norm_from_db(month, year, 'NMD - Utility Plant', 'Cooling Water 2', 'Water')
    
    u4u_items.append({'name': 'CW1 Plant', 'quantity': cw1_water, 'norm': cw1_water_norm})
    u4u_items.append({'name': 'CW2 Plant', 'quantity': cw2_water, 'norm': cw2_water_norm})
    
    # DM Water plant
    dm_water = raw_water_data.get('dm_m3', 0)
    dm_water_norm = get_utility_norm_from_db(month, year, 'NMD - Utility Plant', 'D M Water', 'Water')
    
    u4u_items.append({'name': 'DM Water Plant', 'quantity': dm_water, 'norm': dm_water_norm})
    
    # HRSGs
    hrsg2_water = raw_water_data.get('hrsg2_m3', 0)
    hrsg3_water = raw_water_data.get('hrsg3_m3', 0)
    
    hrsg_water_norm = get_utility_norm_from_db(month, year, 'NMD - Utility Plant', 'HRSG2_SHP STEAM', 'Water')
    
    u4u_items.append({'name': 'HRSG2', 'quantity': hrsg2_water, 'norm': hrsg_water_norm})
    u4u_items.append({'name': 'HRSG3', 'quantity': hrsg3_water, 'norm': hrsg_water_norm})
    
    # Effluent
    effluent_water = raw_water_data.get('effluent_m3', 0)
    effluent_water_norm = get_utility_norm_from_db(month, year, 'NMD - Utility Plant', 'Effluent Treated', 'Water')
    
    u4u_items.append({'name': 'Effluent Treatment', 'quantity': effluent_water, 'norm': effluent_water_norm})
    
    # Process and Fixed demands (typically none for raw water)
    process_items = get_plant_wise_demand(month, year, 'Water')
    fixed_items = get_fixed_consumption_details(month, year, 'Water')
    
    total_demand = raw_water_data.get('total_m3', 0)
    
    return {
        'demand': {
            'u4u_items': u4u_items,
            'process_items': process_items,
            'fixed_items': fixed_items,
            'total': total_demand
        },
        'supply': {
            'plant_production': total_demand,
            'label': 'Raw Water Intake (External Source)'
        },
        'balance': 0.0,
        'unit': 'M3'
    }
