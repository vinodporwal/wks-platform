import sys
sys.path.insert(0, '.')

# 1. Check BPC reader
from engine.bpc_ods_reader import BPCODSReader
BPCODSReader.clear_cache()
bpc = BPCODSReader.get_reader(4, 2025)
bpc.load()
print("=== BPC Generation Quantities ===")
gen_map = bpc.get_bpc_generation_quantities()
for k in sorted(gen_map.keys()):
    print(f"  {k:35s} = {gen_map[k]:14.2f}")

print("\n=== BPC Quantities (POWERGEN assets) ===")
qty_map = bpc.get_bpc_quantities()
for k in sorted(qty_map.keys()):
    if "Power Plant" in k or "STG" in k:
        mats = qty_map[k]
        print(f"  {k:35s}: {mats}")

# 2. Check norms reader consumption matrix for POWERGEN
from engine.norms_reader import NMDNormsReader
NMDNormsReader.clear_cache()
nr = NMDNormsReader.get_reader(4, 2025)
norms = nr.get_consumption_norms()
print("\n=== Norms Reader: POWERGEN entries ===")
for key, info in norms.items():
    if info.get("utility", "").upper() == "POWERGEN":
        print(f"  key={key:35s} | plant={info['plant_name']:35s} | utility={info.get('utility','')}")
        for c in info["consumptions"]:
            if c["account"] == "Utilities":
                print(f"    material={c['material']:25s} | norm={c['norm']:.6f}")

# 3. Check per-asset generation from dispatch
from engine.dispatch_engine import dispatch_power
NMDNormsReader.clear_cache()
r = dispatch_power('23BCA1B3-56DD-4C15-A3D6-3C2C9A62E653', 4, 2025)
print("\n=== Power Dispatch (per asset) ===")
for a in r.get('assets', []):
    name = a["asset_name"]
    mwh = a["dispatched_mwh"]
    print(f"  {name:30s} | dispatched_mwh={mwh:12.2f}")
print(f"  total_generation_mwh={r.get('total_generation_mwh', 0)}")

