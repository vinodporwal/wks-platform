"""Isolated validation of the new DTA STG extraction methodology."""
from engine.dta_stg_calc import calculate_dta_stg_extraction
from engine.dta_stg_config import get_dta_stg_config

config = get_dta_stg_config()

print("=" * 80)
print("DTA STG extraction methodology validation")
print("=" * 80)

for cf in (25.0, 20.0, 30.0):
    calc = calculate_dta_stg_extraction(
        config["max_hp_extraction_tph"],
        config["max_mp_extraction_tph"],
        cf,
        config,
    )
    print(f"\nCondensate = {cf:.1f} TPH")
    print(f"  HHP                = {calc['shp_inlet_tph']:.2f} TPH")
    print(f"  HP HHP Equivalent  = {calc['hp_hhp_equivalent_tph']:.2f} TPH")
    print(f"  MP HHP Equivalent  = {calc['mp_hhp_equivalent_tph']:.2f} TPH")
    print(f"  Net HHP            = {calc['net_hhp_tph']:.2f} TPH")
    print(f"  SSC                = {calc['ssc_kg_kwh']:.4f} kg/kWh")
    print(f"  MW                 = {calc['mw']:.4f} MW")
    print(f"  Heat Rate          = {calc['heat_rate_kcal_kwh']:.2f} kcal/kWh")

print("\n" + "=" * 80)
print("Base case (CF = 25) full report:")
base = calculate_dta_stg_extraction(
    config["max_hp_extraction_tph"],
    config["max_mp_extraction_tph"],
    25.0,
    config,
)
for k, v in base.items():
    print(f"  {k:<24} = {v}")
print("=" * 80)
