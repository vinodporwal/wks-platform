#!/usr/bin/env python3
"""
Test script to verify excess steam balancing fix
"""

import sys
import os

# Add the script root to the path so imports behave like the app
sys.path.append(os.path.dirname(__file__))

from services.iteration_service import plan_excess_steam_absorption

def test_excess_steam_fix():
    """Test the excess steam balancing fix"""
    
    print("Testing excess steam balancing fix...")
    print("="*60)
    
    # Test case 1: Normal case without export
    print("\n1. NORMAL CASE - GTs can reduce:")
    excess_steam_mt = 21953.97
    stg_available_increase = 13320.00  # STG can increase
    gt_available_reduction = 5000.00   # GTs can reduce

    result = plan_excess_steam_absorption(
        excess_steam_mt=excess_steam_mt,
        stg_available_increase_mwh=stg_available_increase,
        gt_available_reduction_mwh=gt_available_reduction,
        export_available=False,
    )
    
    print(f"  Excess Steam: {excess_steam_mt:.2f} MT")
    print(f"  Balance Method: {result['balance_method']}")
    print(f"  STG Increase: {result['actual_stg_increase_mwh']:.2f} MWh")
    print(f"  GT Reduction: {result['gt_reduction_applied_mwh']:.2f} MWh")
    print(f"  Steam Consumed: {result['steam_absorbed_mt']:.2f} MT")
    print(f"  Remaining Excess: {result['remaining_excess_steam_mt']:.2f} MT")
    assert abs(result["actual_stg_increase_mwh"] - 5000.0) < 0.01
    assert abs(result["gt_reduction_applied_mwh"] - 5000.0) < 0.01
    assert abs(result["export_power_mwh"]) < 0.01
    
    # Test case 2: Hybrid case with export available
    print("\n2. HYBRID CASE - GT reduction plus export:")
    result = plan_excess_steam_absorption(
        excess_steam_mt=excess_steam_mt,
        stg_available_increase_mwh=stg_available_increase,
        gt_available_reduction_mwh=3000.0,
        export_available=True,
    )
    
    print(f"  Excess Steam: {excess_steam_mt:.2f} MT")
    print(f"  Balance Method: {result['balance_method']}")
    print(f"  STG Increase: {result['actual_stg_increase_mwh']:.2f} MWh")
    print(f"  GT Reduction: {result['gt_reduction_applied_mwh']:.2f} MWh")
    print(f"  Power Export: {result['export_power_mwh']:.2f} MWh")
    print(f"  Steam Consumed: {result['steam_absorbed_mt']:.2f} MT")
    print(f"  Remaining Excess: {result['remaining_excess_steam_mt']:.2f} MT")
    assert result["balance_method"] == "GT_REDUCTION_PLUS_EXPORT"
    assert result["actual_stg_increase_mwh"] > result["gt_reduction_applied_mwh"]
    
    # Test case 3: Export-only case
    print("\n3. POWER EXPORT CASE - GTs at MIN, export available:")
    result = plan_excess_steam_absorption(
        excess_steam_mt=excess_steam_mt,
        stg_available_increase_mwh=stg_available_increase,
        gt_available_reduction_mwh=0.0,
        export_available=True,
    )
    
    print(f"  Excess Steam: {excess_steam_mt:.2f} MT")
    print(f"  Balance Method: {result['balance_method']}")
    print(f"  STG Increase: {result['actual_stg_increase_mwh']:.2f} MWh")
    print(f"  Power Export: {result['export_power_mwh']:.2f} MWh")
    print(f"  Steam Consumed: {result['steam_absorbed_mt']:.2f} MT")
    print(f"  Remaining Excess: {result['remaining_excess_steam_mt']:.2f} MT")
    assert result["balance_method"] == "POWER_EXPORT"
    assert abs(result["export_power_mwh"] - result["actual_stg_increase_mwh"]) < 0.01

    # Test case 4: No export and no GT headroom
    print("\n4. BLOCKED CASE - GTs at MIN, export unavailable:")
    result = plan_excess_steam_absorption(
        excess_steam_mt=excess_steam_mt,
        stg_available_increase_mwh=stg_available_increase,
        gt_available_reduction_mwh=0.0,
        export_available=False,
    )
    print(f"  Balance Method: {result['balance_method']}")
    print(f"  STG Increase: {result['actual_stg_increase_mwh']:.2f} MWh")
    print(f"  Remaining Excess: {result['remaining_excess_steam_mt']:.2f} MT")
    assert result["balance_method"] == "NO_BALANCING_PATH"
    assert abs(result["actual_stg_increase_mwh"]) < 0.01
    
    print("\n" + "="*60)
    print("FIX VERIFICATION COMPLETE")
    print("The fix now respects both GT reduction headroom and export availability.")

if __name__ == "__main__":
    test_excess_steam_fix()
