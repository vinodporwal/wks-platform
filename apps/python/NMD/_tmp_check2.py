import sys
sys.path.insert(0, '.')
from engine.dispatch_engine import dispatch_power
from engine.norms_reader import NMDNormsReader
NMDNormsReader.clear_cache()
r = dispatch_power('23BCA1B3-56DD-4C15-A3D6-3C2C9A62E653', 4, 2025)
print(f"total_gen_mwh: {r['total_generation_mwh']}")
print(f"import_mwh: {r.get('import_power', {}).get('total_mwh', 0)}")
print(f"demand_mwh: {r['demand_mwh']}")
print(f"surplus: {r['surplus_mwh']}")
print(f"deficit: {r['deficit_mwh']}")
for a in r['assets']:
    print(f"  {a['asset_name']:30s} dispatched_mwh={a['dispatched_mwh']:10.2f} min_mwh={a['min_mwh']:10.2f} max_mwh={a['max_mwh']:10.2f} priority={a.get('priority',99)}")
