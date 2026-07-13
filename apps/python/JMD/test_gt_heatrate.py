"""
Test script to verify GT heat rate data from CPP_GTHeatRate table.
Fetches data, interpolates heat rate for a sample load, and calculates MMBTU quantity.
"""
import sys
import os
import logging

# Setup path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger(__name__)

from database.queries import fetch_gt_heat_rate_lookup, fetch_plant_assets
from engine.dispatch_engine import build_gt_heat_rate_lookup, _lookup_gt_heat_rate, _lookup_gt_load_factor

# Constants for MMBTU reverse calculation (same as u4u_iteration_loop.py)
_KCAL_TO_BTU = 3.96567
_BTU_TO_MMBTU = 1_000_000
_FREE_STEAM_ENERGY_KCAL_KG = 760.87  # (810 - 110) / 0.92

PLANT_ID = "BA558F95-8A3F-4769-9C78-FF7B6C639DDF"  # C2
MONTH = 4
YEAR = 2026


def main():
    logger.info("=" * 80)
    logger.info("  GT Heat Rate Verification Test — C2 Plant, April 2026")
    logger.info("=" * 80)

    # 1. Fetch GT heat rate data from DB
    logger.info("\n  [1] Fetching GT heat rate from CPP_GTHeatRate table...")
    gt_df = fetch_gt_heat_rate_lookup(PLANT_ID, MONTH, YEAR)

    if gt_df is None or gt_df.empty:
        logger.error("  No GT heat rate data found in database!")
        return

    logger.info("  DataFrame columns: %s", list(gt_df.columns))
    logger.info("  Total rows: %d", len(gt_df))
    logger.info("  Unique AssetIds: %s", gt_df['AssetId'].unique())
    logger.info("  AssetId dtype: %s", gt_df['AssetId'].dtype)

    # 2. Fetch plant assets to get asset_id → asset_name mapping
    logger.info("\n  [2] Fetching plant assets for ID-to-name mapping...")
    assets = fetch_plant_assets(PLANT_ID)
    asset_by_id = {a["asset_id"]: a for a in assets}

    # 3. Build lookup dict (same as dispatch engine does)
    logger.info("\n  [3] Building GT heat rate lookup from database...")
    gt_lookup = build_gt_heat_rate_lookup(gt_df)
    logger.info("  Lookup keys (AssetIds): %s", list(gt_lookup.keys()))

    # 4. Show data per GT asset
    logger.info("\n  [4] GT Heat Rate Data per Asset:")
    logger.info("  %s", "-" * 80)

    for asset_id, table in gt_lookup.items():
        asset_info = asset_by_id.get(asset_id, {})
        asset_name = asset_info.get("asset_name", "UNKNOWN")
        asset_type = asset_info.get("asset_type", "")

        logger.info("\n  Asset: %s (%s)", asset_name, asset_type)
        logger.info("  AssetId: %s", asset_id)
        logger.info("  Load range: %.2f MW — %.2f MW (%d entries)",
                     table[0][0], table[-1][0], len(table))
        logger.info("  Sample data (first 5 rows):")
        logger.info("    %-10s  %-15s  %-15s", "LoadMW", "HeatRate", "FreeSteamFactor")
        for load, hr, fsf in table[:5]:
            logger.info("    %-10.2f  %-15.2f  %-15.2f", load, hr, fsf)
        logger.info("  ...")
        logger.info("    %-10.2f  %-15.2f  %-15.2f", table[-1][0], table[-1][1], table[-1][2])

    # 5. Test interpolation at sample loads and calculate MMBTU
    logger.info("\n  [5] MMBTU Calculation at Sample Loads:")
    logger.info("  %s", "-" * 80)

    # Test at a few representative loads
    test_loads = [50.0, 75.0, 100.0, 110.0]

    # Assume 720 operating hours (typical full month)
    op_hours = 720

    for asset_id, table in gt_lookup.items():
        asset_info = asset_by_id.get(asset_id, {})
        asset_name = asset_info.get("asset_name", "UNKNOWN")

        logger.info("\n  Asset: %s", asset_name)
        logger.info("  %-10s  %-12s  %-12s  %-15s  %-15s  %-15s",
                     "LoadMW", "HeatRate", "FreeSteam", "GrossKWH", "NG Norm", "MMBTU Qty")
        logger.info("  %s", "-" * 80)

        for load_mw in test_loads:
            # Interpolate heat rate and free steam factor
            hr = _lookup_gt_heat_rate(load_mw, table)
            fsf = _lookup_gt_load_factor(load_mw, table)

            # Calculate gross generation in kWh
            gross_mwh = load_mw * op_hours
            gross_kwh = gross_mwh * 1000.0

            # Reverse-calculate NG Norm (MMBTU/KWH) — same formula as u4u_iteration_loop.py
            # NG Norm = KCAL_TO_BTU * (heat_rate - free_steam_factor * FREE_STEAM_ENERGY) / BTU_TO_MMBTU
            ng_norm = _KCAL_TO_BTU * (hr - fsf * _FREE_STEAM_ENERGY_KCAL_KG) / _BTU_TO_MMBTU

            # MMBTU Quantity = Gross KWH × NG Norm
            mmbtu_qty = gross_kwh * ng_norm

            logger.info("  %-10.2f  %-12.2f  %-12.4f  %-15.2f  %-15.8f  %-15.2f",
                         load_mw, hr, fsf, gross_kwh, ng_norm, mmbtu_qty)

    # 6. Compare with hardcoded fallback
    logger.info("\n\n  [6] Comparison: Database vs Hardcoded Fallback:")
    logger.info("  %s", "-" * 80)

    from engine.dispatch_engine import _GT_LOAD_LOOKUP, _ASSET_TO_EQUIPMENT_TYPE

    for asset_id, db_table in gt_lookup.items():
        asset_info = asset_by_id.get(asset_id, {})
        asset_name = asset_info.get("asset_name", "UNKNOWN")
        equip_type = _ASSET_TO_EQUIPMENT_TYPE.get(asset_name, "")
        hc_table = _GT_LOAD_LOOKUP.get(equip_type, [])

        logger.info("\n  Asset: %s (DB AssetId: %s, Hardcoded key: %s)",
                     asset_name, asset_id, equip_type)

        if not hc_table:
            logger.info("  No hardcoded fallback for this asset")
            continue

        # Compare at a few loads
        for load_mw in [50.0, 75.0, 100.0, 110.0]:
            db_hr = _lookup_gt_heat_rate(load_mw, db_table)
            db_fsf = _lookup_gt_load_factor(load_mw, db_table)
            hc_hr = _lookup_gt_heat_rate(load_mw, hc_table)
            hc_fsf = _lookup_gt_load_factor(load_mw, hc_table)

            hr_diff = abs(db_hr - hc_hr)
            fsf_diff = abs(db_fsf - hc_fsf)

            match_str = "MATCH" if (hr_diff < 0.1 and fsf_diff < 0.01) else "DIFF"
            logger.info("    Load %-6.1f MW | DB HR: %-10.2f  HC HR: %-10.2f  (Δ=%.2f) | "
                         "DB FSF: %-8.4f  HC FSF: %-8.4f  (Δ=%.4f)  [%s]",
                         load_mw, db_hr, hc_hr, hr_diff, db_fsf, hc_fsf, fsf_diff, match_str)

    logger.info("\n" + "=" * 80)
    logger.info("  Test Complete")
    logger.info("=" * 80)


if __name__ == "__main__":
    main()
