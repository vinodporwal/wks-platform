"""Temporary smoke test for DTA-PCG U4U loop — cascade + byproduct verification."""
import sys, os, logging
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

logging.basicConfig(level=logging.INFO, format="%(message)s")

PLANT_ID = "F6D82E68-C3B6-494F-9905-48F19DC611E3"
MONTH, YEAR = 4, 2026

from engine.db_norms_reader import DBNormsReader

reader = DBNormsReader.get_reader(PLANT_ID, MONTH, YEAR)

print("=== CASCADE ===")
letdown = reader.get_steam_letdown_norms()
for step in letdown.get("_cascade", []):
    print(f"  {step['prds']:25s} consumes={step['consumes']:20s} produces={step['produces']:20s} norm={step['norm']}")

print("\n=== BYPRODUCT NORMS BY GRADE ===")
for grade, norms in reader.get_hrsg_byproduct_norms_by_grade().items():
    for util, n in norms.items():
        print(f"  {grade}: {util} = {n}")

print("\n=== U4U LOOP ===")
from database import queries
from engine.u4u_iteration_loop import U4UIterationLoop
from database.heat_rate_json_fallback import (
    build_gt_heat_rate_df, build_hrsg_heat_rate_df,
)
initial_demands = queries.fetch_process_demands_raw(PLANT_ID, MONTH, YEAR)
loop = U4UIterationLoop(
    PLANT_ID, MONTH, YEAR, initial_demands, ods_reader=reader,
    gt_heat_rate_df=build_gt_heat_rate_df(PLANT_ID),
    hrsg_heat_rate_df=build_hrsg_heat_rate_df(PLANT_ID),
)
result = loop.run()

print("\n=== STEAM DEMAND DETAIL ===")
dd = (loop.final_steam_result or {}).get("demand_detail", {})
for k, v in sorted(dd.items()):
    if isinstance(v, (int, float)):
        print(f"  {k:25s} = {v:,.2f}")
    else:
        print(f"  {k:25s} = {v}")

print("\n=== DISPATCHED STEAM ASSETS ===")
for a in (loop.final_steam_result or {}).get("assets", []):
    print(f"  {a.get('asset_name'):40s} type={a.get('asset_type'):10s} "
          f"disp={a.get('dispatched_mt', 0):>12,.2f} total={a.get('total_output_mt', 0):>12,.2f} "
          f"free={a.get('free_steam_mt', 0):>12,.2f}")
