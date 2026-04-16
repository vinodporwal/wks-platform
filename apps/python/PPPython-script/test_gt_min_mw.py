"""
Test Script: Check if GT MinMW is being populated in dispatch entries
This diagnostic script helps verify if the issue is in power_service generation
or in balance_report_service extraction.
"""

import sys
sys.path.insert(0, 'C:\\Users\\shrik\\Desktop\\Project\\fork repo\\development\\New\\wks-platform\\apps\\python\\PPPython-script')

from services.power_service import distribute_by_priority
from database.connection import get_connection

# Test April 2025-26
month = 4
year = 2026
cpp_plant_id = "narmada-midstream-dev"  # Adjust if needed

print("="*70)
print("DIAGNOSTIC TEST: GT MinMW Population")
print("="*70)
print(f"Testing: April {year} (Month {month})")
print()

# Get power dispatch
print("Calling distribute_by_priority()...")
result = distribute_by_priority(month, year, cpp_plant_id)

print()
print("="*70)
print("DISPATCH PLAN CONTENTS")
print("="*70)

dispatch_plan = result.get("dispatchPlan", [])

if not dispatch_plan:
    print("❌ ERROR: No dispatch plan returned!")
    print(f"Result keys: {result.keys()}")
    sys.exit(1)

print(f"✅ Dispatch plan has {len(dispatch_plan)} assets\n")

# Check each asset's MinMW
for i, asset in enumerate(dispatch_plan):
    asset_name = asset.get("AssetName", "UNKNOWN")
    min_mw = asset.get("MinMW", None)
    capacity_mw = asset.get("CapacityMW", None)
    load_mw = asset.get("LoadMW", None)
    gross_mwh = asset.get("GrossMWh", None)
    
    print(f"Asset {i}: {asset_name}")
    print(f"  MinMW:       {min_mw}")
    print(f"  CapacityMW:  {capacity_mw}")
    print(f"  LoadMW:      {load_mw}")
    print(f"  GrossMWh:    {gross_mwh}")
    print()

# Specifically check GT assets
print("="*70)
print("GT ASSETS ANALYSIS")
print("="*70)

gt_assets = [a for a in dispatch_plan if "GT" in a.get("AssetName", "").upper()]

if not gt_assets:
    print("⚠ No GT assets found in dispatch plan!")
else:
    print(f"✅ Found {len(gt_assets)} GT assets\n")
    for asset in gt_assets:
        asset_name = asset.get("AssetName", "UNKNOWN")
        min_mw = asset.get("MinMW", "NOT SET")
        
        if min_mw == "NOT SET" or min_mw == 0:
            print(f"❌ {asset_name}: MinMW = {min_mw} (PROBLEM!)")
        else:
            print(f"✅ {asset_name}: MinMW = {min_mw} MW (OK)")

# Check STG
print()
print("="*70)
print("STG ASSET ANALYSIS")
print("="*70)

stg_assets = [a for a in dispatch_plan if "STG" in a.get("AssetName", "").upper()]

if not stg_assets:
    print("⚠ No STG asset found in dispatch plan!")
else:
    for asset in stg_assets:
        asset_name = asset.get("AssetName", "UNKNOWN")
        min_mw = asset.get("MinMW", "NOT SET")
        
        if min_mw == "NOT SET" or min_mw == 0:
            print(f"❌ {asset_name}: MinMW = {min_mw} (PROBLEM!)")
        else:
            print(f"✅ {asset_name}: MinMW = {min_mw} MW (OK)")

print()
print("="*70)
print("DIAGNOSIS")
print("="*70)

# Check if MinMW is populated in ANY dispatch entry
min_mw_values = [a.get("MinMW") for a in dispatch_plan]
has_min_mw = any(v is not None and v != 0 for v in min_mw_values)

if has_min_mw:
    print("✅ MinMW IS being populated in dispatch entries from power_service")
    print("   Issue is likely in balance_report_service extraction")
else:
    print("❌ MinMW is NOT being populated in dispatch entries from power_service")
    print("   Issue is in power_service._create_dispatch_entry() function")

print()
print("="*70)
