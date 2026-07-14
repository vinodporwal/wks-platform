"""
Test script to verify HRSG heat rate data from CPP_HRSGHeatRate table.
Fetches data, interpolates heat rate for actual steam load, and calculates
MMBTU quantity and reverse norm — same methodology as PPPython-script.
"""
import sys
import os
import logging

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger(__name__)

from database.queries import (
    fetch_hrsg_heat_rate_lookup,
    fetch_steam_generation_assets,
)
from engine.norms_reader_factory import get_norms_reader

# Constants (same as PPPython-script)
BTU_LB_TO_MMBTU_MT = 0.00396567

PLANT_ID = "BA558F95-8A3F-4769-9C78-FF7B6C639DDF"  # C2
MONTH = 4
YEAR = 2026


def interpolate_hrsg_heat_rate(hrsg_name: str, steam_flow_tph: float, lookup_df):
    """
    Interpolate heat rate (BTU/lb) for a given HRSG at a given steam flow (TPH).
    Same logic as PPPython-script get_hrsg_heat_rate_for_load.
    """
    if lookup_df is None or lookup_df.empty:
        return 0.0, 0.0, False

    # Normalize names for matching
    normalized_name = str(hrsg_name).upper().replace("-", "").replace(" ", "")
    lookup_norm = (
        lookup_df["HRSGName"]
        .astype(str)
        .str.upper()
        .str.replace("-", "", regex=False)
        .str.replace(" ", "", regex=False)
    )
    hrsg_df = lookup_df[lookup_norm == normalized_name]

    if hrsg_df.empty:
        # Try substring match
        for idx, row in lookup_df.iterrows():
            db_name = str(row["HRSGName"]).upper().replace("-", "").replace(" ", "")
            if normalized_name in db_name or db_name in normalized_name:
                hrsg_df = lookup_df[lookup_df["HRSGName"] == row["HRSGName"]]
                break

    if hrsg_df.empty:
        return 0.0, 0.0, False

    # Sort by LoadTPH to ensure proper interpolation
    hrsg_df = hrsg_df.sort_values("LoadTPH").reset_index(drop=True)

    # Ensure numeric
    loads = hrsg_df["LoadTPH"].astype(float).values
    heat_rates = hrsg_df["HeatRateBTUlb"].astype(float).values

    if steam_flow_tph <= 0:
        return 0.0, 0.0, False

    min_load = loads.min()
    max_load = loads.max()

    # Clamp to min
    if steam_flow_tph <= min_load:
        hr = heat_rates[0]  # first = lowest load since sorted ASC
        return float(hr), float(hr) * BTU_LB_TO_MMBTU_MT, False

    # Clamp to max
    if steam_flow_tph >= max_load:
        hr = heat_rates[-1]
        return float(hr), float(hr) * BTU_LB_TO_MMBTU_MT, False

    # Find bracketing values
    for i in range(len(loads) - 1):
        if loads[i] <= steam_flow_tph <= loads[i + 1]:
            if loads[i] == loads[i + 1]:
                hr = heat_rates[i]
                return float(hr), float(hr) * BTU_LB_TO_MMBTU_MT, False
            # Linear interpolation
            frac = (steam_flow_tph - loads[i]) / (loads[i + 1] - loads[i])
            hr = heat_rates[i] + frac * (heat_rates[i + 1] - heat_rates[i])
            return float(hr), float(hr) * BTU_LB_TO_MMBTU_MT, True

    # Fallback: closest
    closest_idx = abs(loads - steam_flow_tph).argmin()
    hr = heat_rates[closest_idx]
    return float(hr), float(hr) * BTU_LB_TO_MMBTU_MT, False


def main():
    logger.info("=" * 90)
    logger.info("  HRSG Heat Rate Verification Test — C2 Plant, April 2026")
    logger.info("=" * 90)

    # 1. Fetch HRSG heat rate data from DB
    logger.info("\n  [1] Fetching HRSG heat rate from CPP_HRSGHeatRate table...")
    hrsg_df = fetch_hrsg_heat_rate_lookup(PLANT_ID, MONTH, YEAR)

    if hrsg_df is None or hrsg_df.empty:
        logger.error("  No HRSG heat rate data found in database!")
        return

    logger.info("  DataFrame columns: %s", list(hrsg_df.columns))
    logger.info("  Total rows: %d", len(hrsg_df))
    logger.info("  Unique HRSG names: %s", hrsg_df['HRSGName'].unique())

    # 2. Fetch steam generation assets
    logger.info("\n  [2] Fetching steam generation assets...")
    steam_assets = fetch_steam_generation_assets(PLANT_ID)
    hrsg_assets = [a for a in steam_assets if "HRSG" in (a.get("asset_type", "") + a.get("asset_name", "")).upper()]
    logger.info("  Total steam assets: %d, HRSG assets: %d", len(steam_assets), len(hrsg_assets))
    for a in hrsg_assets:
        logger.info("    %s (type=%s, id=%s)", a["asset_name"], a["asset_type"], a["asset_id"])

    # 3. Show HRSG heat rate data per asset
    logger.info("\n  [3] HRSG Heat Rate Data per Asset:")
    logger.info("  %s", "-" * 90)

    for hrsg_name in hrsg_df['HRSGName'].unique():
        data = hrsg_df[hrsg_df['HRSGName'] == hrsg_name].sort_values('LoadTPH')
        loads = data['LoadTPH'].astype(float).values
        heat_rates = data['HeatRateBTUlb'].astype(float).values

        logger.info("\n  HRSG: %s", hrsg_name)
        logger.info("  Load range: %.2f — %.2f TPH (%d entries)", loads[0], loads[-1], len(loads))
        logger.info("  Sample data (first 5 + last 2):")
        logger.info("    %-10s  %-15s  %-15s", "LoadTPH", "HeatRate(BTU/lb)", "NG Norm(MMBTU/MT)")
        for i in range(min(5, len(loads))):
            ng_norm = heat_rates[i] * BTU_LB_TO_MMBTU_MT
            logger.info("    %-10.2f  %-15.2f  %-15.7f", loads[i], heat_rates[i], ng_norm)
        if len(loads) > 5:
            logger.info("    ...")
            for i in range(max(5, len(loads) - 2), len(loads)):
                ng_norm = heat_rates[i] * BTU_LB_TO_MMBTU_MT
                logger.info("    %-10.2f  %-15.2f  %-15.7f", loads[i], heat_rates[i], ng_norm)

    # 4. Get ODS norms for comparison
    logger.info("\n\n  [4] Fetching ODS norms for comparison...")
    norms_reader = get_norms_reader(PLANT_ID, MONTH, YEAR)
    if norms_reader.is_available:
        raw_material_norms = norms_reader.get_raw_material_norms()
        logger.info("  Raw material (fuel) norms from ODS:")
        for util, norm in raw_material_norms.items():
            logger.info("    %-35s  norm=%.6f MMBTU/MT", util, norm)
    else:
        raw_material_norms = {}
        logger.warning("  ODS norms reader not available")

    # 5. Test calculation at sample steam loads
    logger.info("\n\n  [5] HRSG Reverse Norm Calculation at Sample Loads:")
    logger.info("  %s", "-" * 90)

    # Test at a few representative steam flows
    test_loads_tph = [50.0, 100.0, 150.0, 200.0]
    op_hours = 720  # typical full month

    for hrsg_name in hrsg_df['HRSGName'].unique():
        logger.info("\n  HRSG: %s", hrsg_name)

        # Find ODS norm for this HRSG
        ods_norm = None
        for util, norm in raw_material_norms.items():
            if hrsg_name.upper().replace("-", "").replace(" ", "") in util.upper().replace("-", "").replace(" ", ""):
                ods_norm = norm
                break
        if ods_norm is None:
            # Try matching with _SHP STEAM suffix
            for util, norm in raw_material_norms.items():
                if "HRSG" in util.upper() and "SHP" in util.upper():
                    ods_norm = norm
                    break

        logger.info("  ODS norm: %s", f"{ods_norm:.6f}" if ods_norm else "NOT FOUND")
        logger.info("  %-12s  %-12s  %-15s  %-15s  %-15s  %-15s  %s",
                     "LoadTPH", "Hours", "TotalSHP(MT)", "HeatRate(BTU/lb)", "NG Norm", "MMBTU Qty", "vs ODS")
        logger.info("  %s", "-" * 90)

        for load_tph in test_loads_tph:
            total_shp_mt = load_tph * op_hours

            hr, ng_norm, interp = interpolate_hrsg_heat_rate(hrsg_name, load_tph, hrsg_df)
            mmbtu_qty = total_shp_mt * ng_norm

            vs_ods = ""
            if ods_norm and ng_norm > 0:
                diff_pct = ((ng_norm - ods_norm) / ods_norm) * 100
                vs_ods = f"Δ={diff_pct:+.2f}%"

            logger.info("  %-12.2f  %-12d  %-15.2f  %-15.2f  %-15.7f  %-15.2f  %s",
                         load_tph, op_hours, total_shp_mt, hr, ng_norm, mmbtu_qty, vs_ods)

    # 6. Full simulation: use actual steam dispatch values
    # We'll run a full dispatch to get actual total_output_mt per HRSG
    logger.info("\n\n  [6] Full Simulation with Actual Steam Dispatch:")
    logger.info("  %s", "-" * 90)

    from database.queries import fetch_gt_heat_rate_lookup
    from engine.dispatch_engine import dispatch_power, dispatch_steam

    gt_df = fetch_gt_heat_rate_lookup(PLANT_ID, MONTH, YEAR)

    logger.info("\n  Running power dispatch...")
    power_result = dispatch_power(PLANT_ID, MONTH, YEAR, gt_heat_rate_df=gt_df)
    logger.info("  Power generation: %.2f MWh", power_result.get("total_generation_mwh", 0))

    logger.info("\n  Running steam dispatch...")
    steam_result = dispatch_steam(PLANT_ID, MONTH, YEAR, power_result=power_result, ods_reader=norms_reader)
    steam_assets_result = steam_result.get("assets", [])

    logger.info("\n  HRSG Results from Steam Dispatch:")
    logger.info("  %-25s  %-10s  %-12s  %-12s  %-12s  %-12s",
                 "Asset Name", "Hours", "Disp_MT", "Free_MT", "Total_MT", "Flow_TPH")
    logger.info("  %s", "-" * 90)

    for a in steam_assets_result:
        if a.get("asset_type", "").upper() != "HRSG":
            continue
        name = a["asset_name"]
        hours = a.get("op_hours", 0)
        disp_mt = a.get("dispatched_mt", 0)
        free_mt = a.get("free_steam_mt", 0)
        total_mt = a.get("total_output_mt", disp_mt + free_mt)
        flow_tph = total_mt / hours if hours > 0 else 0

        logger.info("  %-25s  %-10.0f  %-12.2f  %-12.2f  %-12.2f  %-12.4f",
                     name, hours, disp_mt, free_mt, total_mt, flow_tph)

        # Calculate reverse norm
        hr, ng_norm, interp = interpolate_hrsg_heat_rate(name, flow_tph, hrsg_df)
        mmbtu_qty = total_mt * ng_norm

        # Find ODS norm
        ods_norm = None
        for util, norm in raw_material_norms.items():
            if name.upper().replace("-", "").replace(" ", "") in util.upper().replace("-", "").replace(" ", ""):
                ods_norm = norm
                break

        logger.info("    → HeatRate=%.2f BTU/lb, NG Norm=%.7f MMBTU/MT, MMBTU=%.2f, interpolated=%s",
                     hr, ng_norm, mmbtu_qty, interp)
        if ods_norm:
            diff_pct = ((ng_norm - ods_norm) / ods_norm) * 100 if ng_norm > 0 else 0
            ods_mmbtu = total_mt * ods_norm
            logger.info("    → ODS norm=%.7f, ODS MMBTU=%.2f, Δ norm=%.2f%%, Δ MMBTU=%.2f",
                         ods_norm, ods_mmbtu, diff_pct, mmbtu_qty - ods_mmbtu)
        logger.info("")

    logger.info("\n" + "=" * 90)
    logger.info("  Test Complete")
    logger.info("=" * 90)


if __name__ == "__main__":
    main()
