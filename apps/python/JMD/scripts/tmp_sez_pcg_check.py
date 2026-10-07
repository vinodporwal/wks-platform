"""Temporary regression check: SEZ-PCG U4U loop still converges after the ODS layout fix."""
import sys, os, logging
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
logging.basicConfig(level=logging.INFO, format="%(message)s")

PLANT_ID = "D2C7FBAD-7E00-4642-B3B2-5A768FAC8D45"
MONTH, YEAR = 4, 2026

from engine.db_norms_reader import DBNormsReader
from database import queries
from engine.u4u_iteration_loop import U4UIterationLoop
from database.heat_rate_json_fallback import build_gt_heat_rate_df, build_hrsg_heat_rate_df

reader = DBNormsReader.get_reader(PLANT_ID, MONTH, YEAR)
loop = U4UIterationLoop(
    PLANT_ID, MONTH, YEAR, queries.fetch_process_demands_raw(PLANT_ID, MONTH, YEAR),
    ods_reader=reader,
    gt_heat_rate_df=build_gt_heat_rate_df(PLANT_ID),
    hrsg_heat_rate_df=build_hrsg_heat_rate_df(PLANT_ID),
)
result = loop.run()
print("\nSEZ-PCG converged=%s iterations=%s baseline_authoritative=%s"
      % (result.get("converged"), result.get("iterations_used"),
         result.get("bpc_baseline_authoritative")))
