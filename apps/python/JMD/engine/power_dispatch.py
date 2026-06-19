"""
JMD Power Dispatch Engine
=========================
Mirrors PPPython-script/services/power_service.py but adapted for JMD:

  - Uses CPPAssetOperationalHours (wide-column per FY) instead of OperationalHours
  - Uses CPPPowerAssetPriority   (wide-column per FY) instead of AssetAvailability
  - All functions accept plant_id so the same code runs for all 5 JMD plants
  - Norms fetched from NormsMonthDetail via fetch_norms (already working)

Key differences from PPPython-script:
  - No UtilityForUtilityNorms table; aux norms come from NormsMonthDetail
  - STG SHP norm looked up from NormsMonthDetail instead of hardcoded
  - Heat-rate curve keyed by AssetName, not GT-1/GT-2/GT-3 labels
"""

import json
import logging
import os
from collections import defaultdict

import pandas as pd

from database.connection import get_connection
from database.tables import T
# CPPPowerAssetCapacity month column format: Jun_Min, Jun_Max

from database.queries import (
    fetch_import_power,
    fetch_stg_extraction_lookup,
    get_stg_extraction_for_load,
)

_DUMMY_FILE = os.path.join(
    os.path.dirname(os.path.dirname(__file__)), "data", "dummy_consumption.json"
)
_dummy_cache: dict = {}


def _load_dummy() -> dict:
    global _dummy_cache
    if not _dummy_cache:
        with open(_DUMMY_FILE, encoding="utf-8") as f:
            _dummy_cache = json.load(f)
    return _dummy_cache


def _build_dummy_avail_df(plant_id: str, month: int) -> pd.DataFrame:
    """
    Build an avail DataFrame from dummy_consumption.json asset_capacity section
    when the DB returns no assets for the plant.
    Uses month index (Apr=0) to pick op_hours (all 720 by default).
    """
    data = _load_dummy()
    cap_section = data.get("asset_capacity", {}).get(plant_id.upper(), {})
    power_assets = cap_section.get("power_assets", [])

    rows = []
    for i, a in enumerate(power_assets, start=1):
        rows.append({
            "AssetId":   f"DUMMY-{i:03d}-{plant_id[:8]}",
            "AssetName": a["asset_name"],
            "AssetType": a["type"],
            "OpHours":   float(a.get("op_hours_month", 720)),
            "Priority":  float(i),
            "MinMW":     float(a.get("min_mw", 0)),
            "MaxMW":     float(a.get("max_mw", a.get("capacity_mw", 0))),
            "FixedMin":  float(a.get("min_mw", 0)),
            "FixedMax":  float(a.get("max_mw", a.get("capacity_mw", 0))),
        })
    if not rows:
        return pd.DataFrame()
    return pd.DataFrame(rows)

logger = logging.getLogger(__name__)

ITERATION_LIMIT = 100
TOLERANCE = 1.0  # MWh


# ---------------------------------------------------------------------------
# Month helpers
# ---------------------------------------------------------------------------

_MONTH_COL_TITLE = {
    1: "Jan", 2: "Feb", 3: "Mar", 4: "Apr",
    5: "May", 6: "Jun", 7: "Jul", 8: "Aug",
    9: "Sep", 10: "Oct", 11: "Nov", 12: "Dec",
}


def _fy_string(month: int, year: int) -> str:
    fy_start = year if month >= 4 else year - 1
    return f"{fy_start}-{str(fy_start + 1)[-2:]}"


# ---------------------------------------------------------------------------
# Norm lookup from NormsMonthDetail
# ---------------------------------------------------------------------------

def _get_norm_value(norms: list, utility_name: str, material_name: str,
                    asset_name: str = None) -> float:
    """
    Scan the pre-fetched norms list for a matching row.
    Returns the NormValue (float) or 0.0 if not found.
    """
    for n in norms:
        if n.get("UtilityName") != utility_name:
            continue
        if n.get("MaterialName") != material_name:
            continue
        if asset_name and asset_name.upper() not in str(n.get("PlantName", "")).upper():
            continue
        v = n.get("NormValue")
        return float(v) if v is not None else 0.0
    return 0.0


# ---------------------------------------------------------------------------
# Heat-rate curve fetch
# ---------------------------------------------------------------------------

def _fetch_gt_heat_curve_map(cur, fy: str, plant_id: str) -> dict:
    """
    Fetch GT heat-rate curves for all GTs of the plant.
    Returns {asset_name_upper: DataFrame(GTLoad, HeatRate, FreeSteamFactor)}.
    """
    cur.execute(
        f"""
        SELECT g.AssetName, g.{T.GT_HR_LOAD_COL}, g.{T.GT_HR_VALUE_COL}, g.FreeSteamFactor
        FROM   {T.CPP_GT_HEAT_RATE}          g
        JOIN   {T.POWER_GENERATION_ASSETS}   a ON a.AssetId = g.{T.GT_HR_ASSET_FK}
        WHERE  a.{T.PGA_PLANT_FK}            = ?
          AND  g.{T.GT_HR_YEAR_COL}          = ?
        ORDER  BY g.AssetName, g.{T.GT_HR_LOAD_COL}
        """,
        (plant_id, fy),
    )
    rows = cur.fetchall()
    if not rows:
        return {}

    df = pd.DataFrame([tuple(r) for r in rows], columns=["AssetName", "GTLoad", "HeatRate", "FreeSteamFactor"])
    df["GTLoad"] = df["GTLoad"].astype(float)
    df["HeatRate"] = df["HeatRate"].astype(float)
    df["FreeSteamFactor"] = df["FreeSteamFactor"].astype(float)

    curve_map = {}
    for name, grp in df.groupby("AssetName"):
        curve_map[name.upper()] = grp.sort_values("GTLoad").reset_index(drop=True)
    return curve_map


def _get_heat_rate_for_load(heat_df: pd.DataFrame, load_mw: float):
    """Linear-interpolate heat rate and free steam factor at given MW load."""
    if heat_df is None or heat_df.empty or load_mw is None:
        return None, None

    df = heat_df.sort_values("GTLoad").reset_index(drop=True)
    min_load = float(df["GTLoad"].iloc[0])
    max_load = float(df["GTLoad"].iloc[-1])

    if load_mw <= min_load:
        r = df.iloc[0]
        return float(r["HeatRate"]), float(r["FreeSteamFactor"])
    if load_mw >= max_load:
        r = df.iloc[-1]
        return float(r["HeatRate"]), float(r["FreeSteamFactor"])

    lower = upper = None
    for _, row in df.iterrows():
        val = float(row["GTLoad"])
        if abs(val - load_mw) < 1e-9:
            return float(row["HeatRate"]), float(row["FreeSteamFactor"])
        if val < load_mw:
            lower = row
        elif val > load_mw and upper is None:
            upper = row
            break

    if lower is None or upper is None:
        return None, None

    x1, x2 = float(lower["GTLoad"]), float(upper["GTLoad"])
    if abs(x2 - x1) < 1e-9:
        return float(lower["HeatRate"]), float(lower["FreeSteamFactor"])
    frac = (load_mw - x1) / (x2 - x1)
    hr = float(lower["HeatRate"]) + frac * (float(upper["HeatRate"]) - float(lower["HeatRate"]))
    fs = float(lower["FreeSteamFactor"]) + frac * (float(upper["FreeSteamFactor"]) - float(lower["FreeSteamFactor"]))
    return hr, fs


# ---------------------------------------------------------------------------
# Single dispatch execution
# ---------------------------------------------------------------------------

def _dispatch_once(
    avail_df: pd.DataFrame,
    norms_map: dict,
    demand_units: float,
    heat_curve_map: dict = None,
    stg_extraction_df: pd.DataFrame = None,
    stg_shp_norm_per_kwh: float = None,
) -> tuple:
    """
    Dispatch available assets to meet demand_units (MWh).

    Rules (same as PPPython-script):
      1. All assets run at MIN load first.
      2. Increase by priority order to meet remaining demand.
      3. Assets at same priority share load equally.

    Returns:
        (dispatch_list, total_gross, total_aux, total_net, remaining)
    """
    avail = avail_df.copy()
    asset_dispatch = {}

    total_gross = 0.0
    total_aux = 0.0
    total_net = 0.0

    def _create_entry(r, gross):
        norm = norms_map.get(str(r["AssetId"]).lower(), 0.0)
        aux = gross * norm
        net = gross - aux
        hours = float(r["opHours"])
        asset_name = str(r["AssetName"])
        is_gt = "GT" in asset_name.upper() or "PLANT" in asset_name.upper()
        is_stg = "STG" in asset_name.upper() or "STEAM TURBINE" in asset_name.upper()

        load_mw = gross / hours if hours > 0 else 0.0
        heat_rate = free_steam = stg_shp_req = stg_pwr_req = None

        if is_gt and load_mw > 0 and heat_curve_map:
            curve_df = heat_curve_map.get(asset_name.upper())
            if curve_df is not None:
                heat_rate, free_steam = _get_heat_rate_for_load(curve_df, load_mw)

        if is_stg and gross > 0:
            gross_kwh = gross * 1000
            if stg_extraction_df is not None and not stg_extraction_df.empty and load_mw > 0:
                ext = get_stg_extraction_for_load(load_mw, stg_extraction_df)
                inlet_tph = ext.get("shp_inlet_tph", 0)
                stg_shp_req = (inlet_tph * hours) if inlet_tph > 0 else (gross_kwh * (stg_shp_norm_per_kwh or 0))
            else:
                stg_shp_req = gross_kwh * (stg_shp_norm_per_kwh or 0)
            stg_pwr_req = gross_kwh * norm / 1000

        return {
            "AssetId":              str(r["AssetId"]),
            "AssetName":            asset_name,
            "Priority":             r["Priority"],
            "CapacityMW":           float(r["capacity"]),
            "MinMW":                float(r["minMW"]),
            "Hours":                hours,
            "GrossMWh":             round(gross, 6),
            "AuxMWh":               round(aux, 6),
            "NetMWh":               round(net, 6),
            "AuxFactor":            norm,
            "LoadMW":               round(load_mw, 6),
            "HeatRate":             round(heat_rate, 6) if heat_rate is not None else None,
            "FreeSteam":            round(free_steam, 6) if free_steam is not None else None,
            "STG_SHP_Required_MT":  round(stg_shp_req, 2) if stg_shp_req is not None else None,
            "STG_Power_Required_MWh": round(stg_pwr_req, 2) if stg_pwr_req is not None else None,
        }, aux, net

    # Step 1: all assets at MIN load
    for idx, r in avail.iterrows():
        raw_pri = r["Priority"]
        if pd.isna(raw_pri) or raw_pri is None:
            pri = 999.0
        elif raw_pri <= 0:
            pri = 1.0
        elif raw_pri > 100:
            pri = 100.0
        else:
            pri = float(raw_pri)

        min_e = float(r["minEnergy"])
        asset_dispatch[idx] = {
            "row": r,
            "gross": min_e,
            "min_energy": min_e,
            "max_energy": float(r["maxEnergy"]),
            "priority": pri,
        }
        total_gross += min_e

    total_min = sum(ad["min_energy"] for ad in asset_dispatch.values())
    remaining = float(demand_units) - total_min

    # Step 2: increase by priority to meet remaining
    if remaining > 0:
        priority_groups = defaultdict(list)
        for idx, ad in asset_dispatch.items():
            priority_groups[ad["priority"]].append(idx)

        for pri in sorted(priority_groups.keys()):
            if remaining <= 0:
                break
            group_indices = priority_groups[pri]
            group_avail = [(idx, asset_dispatch[idx]["max_energy"] - asset_dispatch[idx]["gross"])
                           for idx in group_indices
                           if asset_dispatch[idx]["max_energy"] - asset_dispatch[idx]["gross"] > 0]
            if not group_avail:
                continue

            total_avail = sum(a for _, a in group_avail)
            allocation = min(remaining, total_avail)

            if len(group_avail) == 1:
                idx, _ = group_avail[0]
                asset_dispatch[idx]["gross"] += allocation
                remaining -= allocation
            else:
                # Equal MW dispatch within priority group
                remaining_assets = [(idx, asset_dispatch[idx]) for idx, _ in group_avail]
                remaining_mwh = sum(asset_dispatch[idx]["gross"] for idx, _ in group_avail) + allocation

                while remaining_assets:
                    total_hrs = sum(float(ad["row"]["opHours"]) for _, ad in remaining_assets)
                    if total_hrs <= 0:
                        break
                    target_mw = remaining_mwh / total_hrs
                    newly_capped = []
                    for idx, ad in remaining_assets:
                        hrs = float(ad["row"]["opHours"])
                        target_gross = target_mw * hrs
                        if target_gross >= ad["max_energy"] - 1e-6:
                            asset_dispatch[idx]["gross"] = ad["max_energy"]
                            remaining_mwh -= ad["max_energy"]
                            newly_capped.append(idx)
                    if newly_capped:
                        remaining_assets = [(i, a) for i, a in remaining_assets if i not in newly_capped]
                    else:
                        for idx, ad in remaining_assets:
                            hrs = float(ad["row"]["opHours"])
                            asset_dispatch[idx]["gross"] = target_mw * hrs
                        break
                remaining -= allocation

    # Step 3: build dispatch list
    dispatch = []
    total_gross = total_aux = total_net = 0.0
    for idx, ad in sorted(asset_dispatch.items(), key=lambda x: x[1]["priority"]):
        entry, aux, net = _create_entry(ad["row"], ad["gross"])
        dispatch.append(entry)
        total_gross += ad["gross"]
        total_aux += aux
        total_net += net

    remaining_final = max(0.0, float(demand_units) - total_gross)
    return dispatch, total_gross, total_aux, total_net, remaining_final


# ---------------------------------------------------------------------------
# Main dispatch function
# ---------------------------------------------------------------------------

def distribute_by_priority(
    plant_id: str,
    month: int,
    year: int,
    norms: list,
    additional_demand_mwh: float = 0.0,
    stg_max_mwh: float = None,
    stg_min_override_mwh: float = None,
    gt_reduction_mwh: float = 0.0,
    stg_extraction_df: pd.DataFrame = None,
    verbose: bool = True,
) -> dict:
    """
    Dispatch power generation assets to meet demand for a JMD plant/month.

    Args:
        plant_id:              CPP plant UUID
        month, year:           Financial period
        norms:                 Pre-fetched norm list from fetch_norms()
        additional_demand_mwh: U4U power to add to base demand
        stg_max_mwh:           Cap STG generation (for SHP balance iteration)
        stg_min_override_mwh:  Force STG minimum (to absorb excess steam)
        gt_reduction_mwh:      Reduce GT capacity (power balance iteration)
        stg_extraction_df:     Pre-fetched STG extraction DataFrame

    Returns:
        dict matching PPPython-script power_service output structure
    """
    fy = _fy_string(month, year)
    mcol = _MONTH_COL_TITLE[month]

    conn = get_connection()
    cur = conn.cursor()

    try:
        # ── FYM id ───────────────────────────────────────────────────────────
        cur.execute(
            f"SELECT Id FROM {T.FINANCIAL_YEAR_MONTH} WHERE [Month]=? AND [Year]=?",
            (month, year),
        )
        row = cur.fetchone()
        if not row:
            conn.close()
            return {"message": f"FinancialYearMonth {month}/{year} not found."}
        fym_id = str(row[0])

        # ── GT heat-rate curves ───────────────────────────────────────────────
        heat_curve_map = _fetch_gt_heat_curve_map(cur, fy, plant_id)
        if not heat_curve_map:
            # Try previous FY
            prev_fy_start = (year if month >= 4 else year - 1) - 1
            prev_fy = f"{prev_fy_start}-{str(prev_fy_start + 1)[-2:]}"
            heat_curve_map = _fetch_gt_heat_curve_map(cur, prev_fy, plant_id)
            if heat_curve_map:
                logger.warning("  [HEAT RATE] Using fallback FY %s", prev_fy)

        # CalculatedProcessDemand uses lowercase month cols (jun, jul, etc.)
        month_col_lower = mcol.lower()

        # ── Process demands ───────────────────────────────────────────────────
        cur.execute(
            f"""
            SELECT cpp_utility, SUM([{month_col_lower}]) AS Total
            FROM   dbo.{T.CALCULATED_PROCESS_DEMAND}
            WHERE  financial_year = ? AND cpp_plant_id = ?
            GROUP  BY cpp_utility
            """,
            (fy, plant_id),
        )
        _UTIL_MAP = {
            "Power": "power", "Power_Dis": "power",
        }
        plant_demand_kwh = 0.0
        for r in cur.fetchall():
            if r[0] in ("Power", "Power_Dis") and r[1]:
                plant_demand_kwh += float(r[1])
        plant_demand_mwh = plant_demand_kwh / 1000.0

        # Fallback: if DB returned no power demand, use dummy JSON
        if plant_demand_mwh == 0.0:
            _mi = {4:0,5:1,6:2,7:3,8:4,9:5,10:6,11:7,12:8,1:9,2:10,3:11}
            _idx = _mi.get(month, 0)
            _dummy = _load_dummy()
            _pp = _dummy.get("plants", {}).get(plant_id.upper(), {}).get("process_demands", {})
            _kwh = _pp.get("power_process", [0]*12)
            plant_demand_mwh = float(_kwh[_idx]) / 1000.0 if _idx < len(_kwh) else 0.0
            logger.debug("  [DISPATCH] Using dummy power demand %.1f MWh for plant %s", plant_demand_mwh, plant_id)

        # ── Fixed consumption ─────────────────────────────────────────────────
        try:
            cur.execute(
                f"""
                SELECT SUM(fc.{T.FIXED_CONSUMPTION_VALUE})
                FROM   {T.FIXED_CONSUMPTION} fc
                JOIN   {T.FINANCIAL_YEAR_MONTH} fym ON fym.Id = fc.{T.FIXED_CONSUMPTION_MONTH_FK}
                WHERE  fym.[Month]=? AND fym.[Year]=?
                """,
                (month, year),
            )
            row = cur.fetchone()
            fixed_demand_mwh = (float(row[0]) / 1000.0) if row and row[0] else 0.0
        except Exception:
            fixed_demand_mwh = 0.0

        # Fallback: if DB returned no fixed demand, use dummy JSON
        if fixed_demand_mwh == 0.0:
            _mi = {4:0,5:1,6:2,7:3,8:4,9:5,10:6,11:7,12:8,1:9,2:10,3:11}
            _idx = _mi.get(month, 0)
            _dummy = _load_dummy()
            _fc = _dummy.get("plants", {}).get(plant_id.upper(), {}).get("fixed_consumption", {})
            _mwh = _fc.get("power_fixed", [0]*12)
            fixed_demand_mwh = float(_mwh[_idx]) if _idx < len(_mwh) else 0.0

        base_demand = plant_demand_mwh + fixed_demand_mwh
        total_demand = base_demand + additional_demand_mwh

        # CPPPowerAssetCapacity uses Mon_Min / Mon_Max column naming
        min_col = f"{mcol}_Min"
        max_col = f"{mcol}_Max"

        # ── Fetch assets: Priority + OperationalHours + Capacity ─────────────
        cur.execute(
            f"""
            SELECT
                a.AssetId,
                a.AssetName,
                a.AssetType,
                oh.[{mcol}]                                          AS OpHours,
                p.[{mcol}]                                           AS Priority,
                COALESCE(cap.[{min_col}], cap.Fixed_Min, 0)          AS MinMW,
                COALESCE(cap.[{max_col}], cap.Fixed_Max, 0)          AS MaxMW,
                cap.Fixed_Min,
                cap.Fixed_Max
            FROM   {T.POWER_GENERATION_ASSETS}          a
            LEFT JOIN {T.CPP_ASSET_OPERATIONAL_HOURS}   oh
                   ON oh.{T.CAOH_ASSET_FK} = a.AssetId
                  AND oh.{T.CAOH_YEAR_COL} = ?
            LEFT JOIN {T.CPP_POWER_ASSET_PRIORITY}      p
                   ON p.{T.PRIORITY_ASSET_FK}  = a.AssetId
                  AND p.{T.PRIORITY_PLANT_FK}  = ?
                  AND p.{T.PRIORITY_YEAR_COL}  = ?
            LEFT JOIN {T.CPP_POWER_ASSET_CAPACITY}      cap
                   ON cap.{T.CAPACITY_ASSET_FK} = a.AssetId
                  AND cap.{T.CAPACITY_YEAR_COL} = ?
            WHERE  a.{T.PGA_PLANT_FK} = ?
            ORDER  BY p.[{mcol}] ASC, a.AssetName
            """,
            (fy, plant_id, fy, fy, plant_id),
        )
        asset_rows = cur.fetchall()
        conn.close()

    except Exception as exc:
        conn.close()
        logger.error("  [DISPATCH] DB error: %s", exc)
        return {"message": str(exc), "error": True}

    using_dummy_assets = False
    if not asset_rows:
        logger.debug("  [DISPATCH] No DB assets for plant %s — using dummy fallback", plant_id)
        df = _build_dummy_avail_df(plant_id, month)
        if df.empty:
            return {"message": "No assets found for plant (DB empty, no dummy either)."}
        df["IsAvailable"] = 1
        using_dummy_assets = True
    else:
        cols = ["AssetId", "AssetName", "AssetType", "OpHours",
                "Priority", "MinMW", "MaxMW", "FixedMin", "FixedMax"]
        df = pd.DataFrame([tuple(r) for r in asset_rows], columns=cols)
        df["IsAvailable"] = df["OpHours"].apply(lambda x: 1 if x is not None and float(x) > 0 else 0)

    # Separate IMPORT type assets (if any marked as such)
    plant_df = df[df["AssetType"].isin(["GT", "STG"])].copy() if "IMPORT" in df["AssetType"].values else df.copy()
    avail = plant_df[plant_df["IsAvailable"] == 1].copy()

    if avail.empty and not using_dummy_assets:
        # DB assets exist but all have 0 op-hours — fall back to dummy
        logger.debug("  [DISPATCH] All DB assets unavailable for plant %s — using dummy fallback", plant_id)
        df = _build_dummy_avail_df(plant_id, month)
        if df.empty:
            return {"message": "No available assets this month (all hours=0, no dummy)."}
        df["IsAvailable"] = 1
        using_dummy_assets = True
        avail = df.copy()
    elif avail.empty:
        return {"message": "No available assets this month."}

    avail["opHours"] = avail["OpHours"].fillna(0).astype(float)
    avail["minMW"] = avail["MinMW"].apply(lambda x: float(x) if pd.notna(x) else 0.0)
    avail["capacity"] = avail.apply(
        lambda r: float(r["MaxMW"]) if pd.notna(r["MaxMW"]) and float(r["MaxMW"]) > 0
                  else max(float(r["FixedMax"] or 0), float(r["minMW"])),
        axis=1,
    )
    avail["maxEnergy"] = avail["capacity"] * avail["opHours"]
    avail["minEnergy"] = avail["minMW"] * avail["opHours"]
    avail = avail.reset_index(drop=True)

    # ── Aux norms from NormsMonthDetail ──────────────────────────────────────
    norms_map = {}
    stg_shp_norm_per_kwh = 0.0
    for _, r in avail.iterrows():
        asset_name = str(r["AssetName"])
        asset_id = str(r["AssetId"]).lower()
        # look for POWERGEN → Power_Dis norm for this asset
        norm_val = _get_norm_value(norms, "POWERGEN", "Power_Dis", asset_name)
        if norm_val == 0.0:
            norm_val = _get_norm_value(norms, "POWERGEN", "Power_Dis")
        norms_map[asset_id] = norm_val

        if "STG" in asset_name.upper() or "STEAM TURBINE" in asset_name.upper():
            stg_shp_norm_per_kwh = _get_norm_value(norms, "POWERGEN", "SHP Steam_Dis", asset_name)
            if stg_shp_norm_per_kwh == 0.0:
                stg_shp_norm_per_kwh = 0.0036  # fallback

    # ── Import power ──────────────────────────────────────────────────────────
    import_result = fetch_import_power(plant_id, month, year)
    max_import_mwh = import_result.get("total_mwh", 0.0) if import_result.get("success") else 0.0
    actual_import_mwh = max_import_mwh

    net_demand_for_dispatch = total_demand - actual_import_mwh

    # ── Capacity check ────────────────────────────────────────────────────────
    total_max_energy = avail["maxEnergy"].sum()
    if total_demand > total_max_energy + max_import_mwh + TOLERANCE:
        return {
            "message": (
                f"Insufficient capacity: demand={total_demand:.2f} MWh, "
                f"plant_cap={total_max_energy:.2f}, import={max_import_mwh:.2f}"
            ),
            "insufficientCapacity": True,
            "dispatchPlan": [],
        }

    if net_demand_for_dispatch <= 0:
        return {
            "processDemand": round(plant_demand_mwh, 6),
            "fixedDemand": round(fixed_demand_mwh, 6),
            "totalDemandUnits": round(total_demand, 6),
            "mandatoryImportUsed": round(actual_import_mwh, 6),
            "dispatchPlan": [],
            "converged": True,
            "totalGrossGeneration": 0.0,
            "totalAuxConsumption": 0.0,
            "totalNetGeneration": 0.0,
            "importUnits": round(actual_import_mwh, 6),
        }

    # ── Apply STG overrides ───────────────────────────────────────────────────
    for idx, r in avail.iterrows():
        is_stg = "STG" in str(r["AssetName"]).upper()
        if is_stg and stg_max_mwh is not None:
            avail.at[idx, "maxEnergy"] = min(float(r["maxEnergy"]), max(0, stg_max_mwh))
        if is_stg and stg_min_override_mwh is not None and stg_min_override_mwh > 0:
            avail.at[idx, "minEnergy"] = max(float(r["minEnergy"]), stg_min_override_mwh)

    # ── GT reduction ──────────────────────────────────────────────────────────
    if gt_reduction_mwh > 0:
        gt_idx = [(i, r) for i, r in avail.iterrows()
                  if "GT" in str(r["AssetName"]).upper() or "PLANT" in str(r["AssetName"]).upper()]
        rem_red = gt_reduction_mwh
        for i, r in sorted(gt_idx, key=lambda x: x[1].get("Priority", 999), reverse=True):
            if rem_red <= 0:
                break
            avail_room = avail.at[i, "maxEnergy"] - avail.at[i, "minEnergy"]
            cut = min(rem_red, avail_room)
            avail.at[i, "maxEnergy"] -= cut
            rem_red -= cut

    # ── Iterative dispatch ────────────────────────────────────────────────────
    gross_target = net_demand_for_dispatch
    final_dispatch = []
    final_gross = final_aux = final_net = 0.0
    converged = False
    iterations_used = 0

    for it in range(1, ITERATION_LIMIT + 1):
        iterations_used = it
        dispatch, tg, ta, tn, _ = _dispatch_once(
            avail, norms_map, gross_target,
            heat_curve_map, stg_extraction_df, stg_shp_norm_per_kwh,
        )
        error = net_demand_for_dispatch - tn
        final_dispatch, final_gross, final_aux, final_net = dispatch, tg, ta, tn

        if abs(error) <= TOLERANCE or error < -TOLERANCE:
            converged = True
            break

        gross_target += error
        gross_target = max(0.0, min(gross_target, avail["maxEnergy"].sum()))

    total_power_supplied = actual_import_mwh + final_net
    remaining_after_assets = max(0.0, net_demand_for_dispatch - final_net)
    acceptable_deficit = max(50.0, total_demand * 0.01)

    if remaining_after_assets > acceptable_deficit:
        return {
            "message": (
                f"Demand cannot be met after import. "
                f"Deficit: {remaining_after_assets:.2f} MWh"
            ),
            "insufficientCapacityAfterImport": True,
            "dispatchPlan": final_dispatch,
            "mandatoryImportUsed": round(actual_import_mwh, 6),
            "iterationsUsed": iterations_used,
        }

    if verbose:
        logger.info(
            "  [DISPATCH] %d/%d  demand=%.1f  import=%.1f  net_gen=%.1f  total=%.1f  iter=%d  %s",
            month, year, total_demand, actual_import_mwh, final_net,
            total_power_supplied, iterations_used, "CONV" if converged else "NO-CONV",
        )

    return {
        "processDemand":          round(plant_demand_mwh, 6),
        "fixedDemand":            round(fixed_demand_mwh, 6),
        "baseDemand":             round(base_demand, 6),
        "u4uPower":               round(additional_demand_mwh, 6),
        "totalDemandUnits":       round(total_demand, 6),
        "netDemandForDispatch":   round(net_demand_for_dispatch, 6),
        "mandatoryImportUsed":    round(actual_import_mwh, 6),
        "dispatchPlan":           final_dispatch,
        "remainingDemandUnits":   0.0,
        "iterationsUsed":         iterations_used,
        "converged":              converged,
        "totalGrossGeneration":   round(final_gross, 6),
        "totalAuxConsumption":    round(final_aux, 6),
        "totalNetGeneration":     round(final_net, 6),
        "totalPowerSupplied":     round(total_power_supplied, 6),
        "plantGrossCapabilityUnits": round(avail["maxEnergy"].sum(), 6),
        "importUnits":            round(actual_import_mwh, 6),
    }
