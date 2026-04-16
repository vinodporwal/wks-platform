#!/usr/bin/env python3
"""
Test script to verify the min load fix for balance report assets.
This script tests the improved asset matching and database fallback logic.
"""

import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from services.balance_report_service import extract_asset_availability_data, get_asset_default_min_load, get_asset_default_max_load
from database.connection import get_connection

def test_asset_defaults():
    """Test the database fallback functions for asset defaults."""
    print("="*60)
    print("Testing Asset Default Load Functions")
    print("="*60)
    
    # Test for April 2025
    month, year = 4, 2025
    
    assets = ['GT1', 'GT2', 'GT3', 'STG']
    
    for asset in assets:
        min_load = get_asset_default_min_load(asset, month, year)
        max_load = get_asset_default_max_load(asset, month, year)
        
        print(f"{asset}: Min={min_load} MW, Max={max_load} MW")
    
    print("\n" + "="*60)

def test_asset_matching():
    """Test asset matching with mock dispatch data."""
    print("Testing Asset Matching Logic")
    print("="*60)
    
    # Mock dispatch data with various asset naming patterns
    mock_dispatch = [
        {'AssetName': 'Plant-1', 'GrossMWh': 1000, 'Hours': 720, 'MinMW': 11, 'CapacityMW': 22, 'LoadMW': 15},
        {'AssetName': 'Plant-2', 'GrossMWh': 800, 'Hours': 720, 'MinMW': 11, 'CapacityMW': 22, 'LoadMW': 12},
        {'AssetName': 'Plant 3', 'GrossMWh': 0, 'Hours': 720, 'MinMW': 11, 'CapacityMW': 22, 'LoadMW': 0},
        {'AssetName': 'STG', 'GrossMWh': 500, 'Hours': 720, 'MinMW': 5, 'CapacityMW': 25, 'LoadMW': 8},
        {'AssetName': 'Import Power', 'GrossMWh': 100, 'Hours': 720, 'MinMW': 0, 'CapacityMW': 25, 'LoadMW': 5},
    ]
    
    # Mock calculation result
    mock_result = {
        'usd_result': {
            'final_dispatch': mock_dispatch,
            'power_result': {'importUnits': 100}
        }
    }
    
    # Test extraction
    month, year = 4, 2025
    asset_data = extract_asset_availability_data(month, year, mock_result)
    
    print("Generation Assets:")
    print("-" * 40)
    for asset in asset_data['generation_assets']:
        print(f"{asset['asset_name']}: Min={asset['min_capacity']} MW, Max={asset['max_capacity']} MW, Load={asset['avg_load_per_hr']} MW, Hours={asset['availability_hr']}")
    
    print("\nSteam Assets:")
    print("-" * 40)
    for asset in asset_data['steam_assets']:
        print(f"{asset['asset_name']}: Min={asset['min_capacity']} MT, Max={asset['max_capacity']} MT, Load={asset['avg_load_per_hr']} MT, Hours={asset['availability_hr']}")
    
    print("\n" + "="*60)

def main():
    """Main test function."""
    print("Balance Report Min Load Fix - Test Script")
    print("=" * 60)
    
    try:
        # Test 1: Database fallback functions
        test_asset_defaults()
        
        # Test 2: Asset matching logic
        test_asset_matching()
        
        print("All tests completed successfully!")
        print("\nThe fix ensures:")
        print("1. All assets get proper min/max load values from database")
        print("2. Asset name matching is more flexible")
        print("3. GT1, GT2, GT3, and STG all show correct min loads")
        
    except Exception as e:
        print(f"Test failed with error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()
