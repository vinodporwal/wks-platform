"""
Test Script: Verify GT MinMW extraction fix in balance_report_service
Verifies that MinMW values are now correctly extracted from dispatch entries
"""

import sys
sys.path.insert(0, 'C:\\Users\\shrik\\Desktop\\Project\\fork repo\\development\\New\\wks-platform\\apps\\python\\PPPython-script')

from services.iteration_service import usd_iterate
from services.balance_report_service import extract_asset_availability_data

# Test April 2025-26
month = 4
year = 2026
cpp_plant_id = "narmada-midstream-dev"

# Default demands
lp_process = 20077.27
lp_fixed = 0.0
mp_process = 20328.56
mp_fixed = 0.0
hp_process = 2615.37
hp_fixed = 0.0
shp_process = 20127.06
shp_fixed = 0.0

print("="*70)
print("TEST: GT MinMW Extraction Fix")
print("="*70)
print(f"Testing: April {year} (Month {month})")
print()

# Run iteration
print("Running usd_iterate()...")
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
)

if not usd_result.get("success"):
    print(f"❌ USD iteration failed: {usd_result.get('message')}")
    print(f"   Error type: {usd_result.get('error_type')}")
    sys.exit(1)

print("✅ USD iteration completed")
print()

# Create calculation result structure
calculation_result = {
    "usd_result": usd_result,
    "power_result": usd_result.get("power_result"),
}

# Extract asset data
print("Extracting asset availability data...")
asset_data = extract_asset_availability_data(month, year, calculation_result)

print()
print("="*70)
print("ASSET AVAILABILITY DATA - GENERATION ASSETS")
print("="*70)

generation_assets = asset_data.get("generation_assets", [])

print()
for asset in generation_assets:
    asset_name = asset.get("asset_name")
    min_cap = asset.get("min_capacity")
    max_cap = asset.get("max_capacity")
    availability_hr = asset.get("availability_hr")
    avg_load = asset.get("avg_load_per_hr")
    
    # Check for the fix
    if min_cap == 0 and availability_hr > 0:
        status = "❌ PROBLEM"
    elif min_cap > 0 and availability_hr > 0:
        status = "✅ FIXED"
    else:
        status = "⚠ OFF"
    
    print(f"{asset_name:<10} Min={min_cap:<6.1f} Max={max_cap:<6.1f} Hours={availability_hr:<4} Load={avg_load:<8} {status}")

print()
print("="*70)
print("DIAGNOSTIC")
print("="*70)

# Check if any active asset has zero MinMW
active_assets_with_zero_min = [
    a for a in generation_assets 
    if a.get("availability_hr") > 0 and a.get("min_capacity") == 0
]

if active_assets_with_zero_min:
    print(f"❌ STILL BROKEN: {len(active_assets_with_zero_min)} active asset(s) with MinMW=0")
    for asset in active_assets_with_zero_min:
        print(f"   - {asset['asset_name']}")
else:
    print("✅ FIX SUCCESSFUL: All active assets have correct MinMW values")

print()
print("="*70)

