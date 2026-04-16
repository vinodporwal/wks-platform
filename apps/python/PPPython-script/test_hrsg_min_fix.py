"""
Test script to verify HRSG minimum load calculation fix
"""

from services.steam_service import calculate_shp_generation_capacity

# Simulate HRSG availability with 720 operating hours (full month)
hrsg_availability = {
    'HRSG1': {
        'is_available': False,
        'operational_hours': 0,
        'min_capacity_mt': 60,
        'max_capacity_mt': 136,
        'efficiency': 1.00,
        'free_steam_mt': 0.0,
        'gt_load_mw': 0,
        'gt_gross_mwh': 0
    },
    'HRSG2': {
        'is_available': True,
        'operational_hours': 720,
        'min_capacity_mt': 60,
        'max_capacity_mt': 136,
        'efficiency': 1.00,
        'free_steam_mt': 9438.01,
        'gt_load_mw': 6.65,
        'gt_gross_mwh': 4790.87,
        'free_steam_factor': 1.00
    },
    'HRSG3': {
        'is_available': True,
        'operational_hours': 720,
        'min_capacity_mt': 60,
        'max_capacity_mt': 136,
        'efficiency': 1.00,
        'free_steam_mt': 9438.01,
        'gt_load_mw': 6.65,
        'gt_gross_mwh': 4790.87,
        'free_steam_factor': 1.97
    }
}

# Calculate SHP capacity
result = calculate_shp_generation_capacity(hrsg_availability)

print("=" * 90)
print("HRSG MINIMUM LOAD FIX VERIFICATION")
print("=" * 90)
print()

# Check each HRSG detail
for hrsg_detail in result['hrsg_details']:
    if hrsg_detail['is_available']:
        print(f"HRSG: {hrsg_detail['name']}")
        print(f"  Hours: {hrsg_detail['hours']:.0f}")
        print(f"  Min Capacity/hr: {hrsg_detail['min_capacity_per_hr']:.1f} MT/hr")
        print(f"  Max Capacity/hr: {hrsg_detail['max_capacity_per_hr']:.1f} MT/hr")
        print(f"  Efficiency: {hrsg_detail['efficiency']}")
        print(f"  Monthly MIN (should be 60 × 720 = 43,200): {hrsg_detail['supp_min_mt_month']:,.2f} MT")
        print(f"  Monthly MAX (should be 136 × 720 × 1.03 = 100,857.6): {hrsg_detail['supp_max_mt_month']:,.2f} MT")
        print()

print("=" * 90)
print("CALCULATION VERIFICATION:")
print("=" * 90)
print()
print(f"EXPECTED MIN per HRSG: 60 MT/hr × 720 hrs = 43,200.00 MT")
print(f"ACTUAL MIN per HRSG (HRSG2): {result['hrsg_details'][1]['supp_min_mt_month']:,.2f} MT")
print()
print(f"EXPECTED MAX per HRSG: 136 MT/hr × 720 hrs × 1.03 = 100,857.60 MT")
print(f"ACTUAL MAX per HRSG (HRSG2): {result['hrsg_details'][1]['supp_max_mt_month']:,.2f} MT")
print()

if result['hrsg_details'][1]['supp_min_mt_month'] == 43200.00:
    print("✓ FIX VERIFIED: MIN load is now correctly 60 MT/hr (not 61.8)")
else:
    print(f"✗ FIX FAILED: Expected 43,200.00 but got {result['hrsg_details'][1]['supp_min_mt_month']:,.2f}")

print()
print("=" * 90)
print("TOTAL CAPACITIES:")
print("=" * 90)
print(f"Total Free Steam: {result['total_free_steam_mt']:,.2f} MT")
print(f"Total MIN Supplementary: {result['total_supplementary_min_mt']:,.2f} MT (2 HRSGs × 43,200 = 86,400)")
print(f"Total MAX Supplementary: {result['total_supplementary_max_mt']:,.2f} MT")
print(f"Total MIN SHP Capacity: {result['total_min_shp_capacity']:,.2f} MT")
print(f"Total MAX SHP Capacity: {result['total_max_shp_capacity']:,.2f} MT")
