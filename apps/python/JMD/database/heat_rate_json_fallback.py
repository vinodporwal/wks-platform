"""
Heat-rate JSON fallback for plants whose GT/HRSG heat-rate curves are not yet
in the database (CPP_GTHeatRate / CPP_HRSGHeatRate tables).

Loads `data/sez_heat_rates.json` (or any other plant-specific JSON placed in
the data dir) and builds pandas DataFrames with the same columns that the
normal DB fetchers return, so the engine can consume them transparently.

JSON schema (see generate_sez_heat_rates_json.py):
{
  "_plant_id": "<UUID>",
  "_financial_year": "2026-27",
  "gt_heat_rates": {
    "<AssetName>": {
      "asset_id": "<UUID>",
      "asset_name": "<AssetName>",
      "curve": [{"load_mw": 41.0, "heat_rate_kcal_kwh": 3569.98, "free_steam_factor": 2.09}, ...]
    }, ...
  },
  "hrsg_heat_rates": {
    "<AssetName>": {
      "asset_id": "<UUID>",
      "asset_name": "<AssetName>",
      "curve": [{"load_tph": 1.0, "heat_rate_btu_lb": 715.0, "free_steam_factor": 0.0}, ...]
    }, ...
  }
}
"""
import json
import logging
import os

import pandas as pd

logger = logging.getLogger(__name__)

_DATA_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "data")

# Map plant_id → JSON filename. Add more entries as new plants get JSON fallbacks.
_JSON_FILES = {
    "2DFEE33F-4CFD-4887-B9DD-53388AA95271": "sez_heat_rates.json",
}

_cache: dict = {}


def _load_json(plant_id: str) -> dict:
    """Load and cache the heat-rate JSON for a plant. Returns {} if none."""
    global _cache
    if plant_id in _cache:
        return _cache[plant_id]

    fname = _JSON_FILES.get(plant_id)
    if not fname:
        _cache[plant_id] = {}
        return {}

    path = os.path.join(_DATA_DIR, fname)
    if not os.path.exists(path):
        logger.warning("  [HEAT RATE JSON] File not found: %s", path)
        _cache[plant_id] = {}
        return {}

    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    _cache[plant_id] = data
    logger.info("  [HEAT RATE JSON] Loaded %s (%d GT, %d HRSG/AB curves)",
                fname, len(data.get("gt_heat_rates", {})),
                len(data.get("hrsg_heat_rates", {})))
    return data


def has_gt_heat_rate_json(plant_id: str) -> bool:
    """True if a JSON fallback file is registered for this plant."""
    return plant_id in _JSON_FILES


def has_hrsg_heat_rate_json(plant_id: str) -> bool:
    """True if a JSON fallback file is registered for this plant."""
    return plant_id in _JSON_FILES


def build_gt_heat_rate_df(plant_id: str, fy: str = None) -> pd.DataFrame:
    """
    Build a GT heat-rate DataFrame matching fetch_gt_heat_rate_lookup output.

    Columns: AssetId, GTName, LoadMW, HeatRateKCALKWH, FreeSteamFactor, FinancialYear
    """
    data = _load_json(plant_id)
    gt_section = data.get("gt_heat_rates", {})
    if not gt_section:
        return pd.DataFrame()

    json_fy = data.get("_financial_year", fy)
    rows = []
    for name, info in gt_section.items():
        asset_id = info.get("asset_id", "")
        for pt in info.get("curve", []):
            rows.append({
                "AssetId":          asset_id,
                "GTName":           name,
                "LoadMW":           float(pt["load_mw"]),
                "HeatRateKCALKWH":  float(pt["heat_rate_kcal_kwh"]),
                "FreeSteamFactor":  float(pt["free_steam_factor"]),
                "FinancialYear":    json_fy,
            })
    if not rows:
        return pd.DataFrame()

    df = pd.DataFrame(rows)
    df["AssetId"] = df["AssetId"].astype(str)
    for col in ["LoadMW", "HeatRateKCALKWH", "FreeSteamFactor"]:
        df[col] = df[col].astype(float)
    df = df.sort_values(["AssetId", "LoadMW"]).reset_index(drop=True)
    return df


def build_hrsg_heat_rate_df(plant_id: str, fy: str = None) -> pd.DataFrame:
    """
    Build an HRSG/AuxBoiler heat-rate DataFrame matching fetch_hrsg_heat_rate_lookup output.

    Columns: HRSGName, LoadTPH, HeatRateBTUlb, FinancialYear
    """
    data = _load_json(plant_id)
    hrsg_section = data.get("hrsg_heat_rates", {})
    if not hrsg_section:
        return pd.DataFrame()

    json_fy = data.get("_financial_year", fy)
    rows = []
    for name, info in hrsg_section.items():
        for pt in info.get("curve", []):
            rows.append({
                "HRSGName":       name,
                "LoadTPH":        float(pt["load_tph"]),
                "HeatRateBTUlb":  float(pt["heat_rate_btu_lb"]),
                "FinancialYear":  json_fy,
            })
    if not rows:
        return pd.DataFrame()

    df = pd.DataFrame(rows)
    for col in ["LoadTPH", "HeatRateBTUlb"]:
        df[col] = df[col].astype(float)
    df = df.sort_values(["HRSGName", "LoadTPH"]).reset_index(drop=True)
    return df


def build_gt_heat_curve_map(plant_id: str, fy: str = None) -> dict:
    """
    Build a GT heat-rate curve map matching _fetch_gt_heat_curve_map output.

    Returns: {asset_name_upper: DataFrame(GTLoad, HeatRate, FreeSteamFactor)}
    """
    df = build_gt_heat_rate_df(plant_id, fy)
    if df.empty:
        return {}

    curve_map = {}
    for name, grp in df.groupby("GTName"):
        sub = grp.sort_values("LoadMW").reset_index(drop=True)
        curve_map[name.upper()] = pd.DataFrame({
            "AssetName":        sub["GTName"],
            "GTLoad":           sub["LoadMW"],
            "HeatRate":         sub["HeatRateKCALKWH"],
            "FreeSteamFactor":  sub["FreeSteamFactor"],
        })
    return curve_map
