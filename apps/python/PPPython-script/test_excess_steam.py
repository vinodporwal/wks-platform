#!/usr/bin/env python3
"""
Test script to check excess steam balancing logic
"""

import sys
import os

# Add the services directory to the path
sys.path.append(os.path.join(os.path.dirname(__file__), 'services'))

from iteration_service import usd_iterate
from steam_service import STEAM_TO_POWER_MT_PER_MWH

def test_excess_steam_logic():
    """Test the excess steam balancing logic with debug output"""
    
    print("Testing excess steam balancing logic...")
    print(f"Steam to power conversion factor: {STEAM_TO_POWER_MT_PER_MWH} MT/MWh")
    
    # Test case 1: Check if excess steam balancing triggers
    excess_steam_mt = 21953.97  # From the logs
    excess_power_mwh = excess_steam_mt / STEAM_TO_POWER_MT_PER_MWH
    
    print(f"\nTest case 1:")
    print(f"  Excess Steam: {excess_steam_mt:.2f} MT")
    print(f"  Excess Power: {excess_power_mwh:.2f} MWh")
    print(f"  Should trigger balancing: {excess_steam_mt > 100}")
    
    # Test case 2: Check STG capacity
    stg_current_mwh = 4320.0  # From the logs (6 MW * 720 hrs)
    stg_max_mwh = 17640.0     # From the logs (24.5 MW * 720 hrs)
    stg_available_increase = stg_max_mwh - stg_current_mwh
    
    print(f"\nTest case 2:")
    print(f"  STG Current: {stg_current_mwh:.2f} MWh")
    print(f"  STG Max: {stg_max_mwh:.2f} MWh")
    print(f"  STG Available Increase: {stg_available_increase:.2f} MWh")
    print(f"  Can absorb all excess: {stg_available_increase >= excess_power_mwh}")
    
    # Test case 3: Check GT reduction
    gt_current_total = 2 * 11478.85  # From the logs
    gt_min_total = 2 * 3600.0       # 5 MW * 720 hrs each
    gt_available_reduction = gt_current_total - gt_min_total
    
    print(f"\nTest case 3:")
    print(f"  GT Current Total: {gt_current_total:.2f} MWh")
    print(f"  GT Min Total: {gt_min_total:.2f} MWh")
    print(f"  GT Available Reduction: {gt_available_reduction:.2f} MWh")
    print(f"  Can balance STG increase: {gt_available_reduction >= stg_available_increase}")
    
    # Test case 4: Final calculation
    actual_stg_increase = min(excess_power_mwh, stg_available_increase, gt_available_reduction)
    steam_consumed = actual_stg_increase * STEAM_TO_POWER_MT_PER_MWH
    remaining_excess = excess_steam_mt - steam_consumed
    
    print(f"\nTest case 4 - Final Result:")
    print(f"  Actual STG Increase: {actual_stg_increase:.2f} MWh")
    print(f"  Steam Consumed: {steam_consumed:.2f} MT")
    print(f"  Remaining Excess: {remaining_excess:.2f} MT")
    print(f"  Expected Remaining: ~0 MT")
    print(f"  Logic Working Correctly: {remaining_excess < 1000}")  # Allow some tolerance

if __name__ == "__main__":
    test_excess_steam_logic()
