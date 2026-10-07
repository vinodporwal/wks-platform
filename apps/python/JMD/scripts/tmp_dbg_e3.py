import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from engine.steam_transfer_router import (
    _position, _local_supply, _edge_for_receiver_material, SEZ_PCG, C2,
    resolve_steam_transfers, STEAM_EDGES,
)


class L:
    def __init__(self):
        self._bpc_steam_plug = {"HP Steam_Dis": 41057.29, "SHP Steam_Dis": 32368.73}
        self.bpc_gen_quantities = {"HP Steam_Dis": 41057.29, "SHP Steam_Dis": 529630.0,
                                 "IP Steam_Dis": 64000.0, "LLP Steam Dis": 39867.0}
        self._initial_utility_demands = {"Gasifier SHP supply": -497261.0}
        self._initial_steam_mt = {}
        self._all_producers = {"HP Steam_Dis", "SHP Steam_Dis"}

    def _lookup_bpc_gen_qty(self, m):
        return self.bpc_gen_quantities.get(m)


loop = L()
sr = {"demand_detail": {"_top_grade": "shp", "hp_net": 84479.0}, "total_generation_mt": 0.0}
td = {"SHP Steam_Dis": 529630.0, "HP Steam_Dis": 84479.0, "LLP Steam Dis": -181293.0}

loops = {SEZ_PCG: loop}
res = {SEZ_PCG: sr}
dem = {SEZ_PCG: td}

e = _edge_for_receiver_material(SEZ_PCG, "HP Steam_Dis")
print("edge:", e["id"] if e else None, "gen_embodies:", e and e.get("gen_embodies_plug"))
print("supply:", _local_supply(loop, sr, "HP Steam_Dis"))
print("pos   :", _position(loops, res, dem, {}, SEZ_PCG, "HP Steam_Dis"))
