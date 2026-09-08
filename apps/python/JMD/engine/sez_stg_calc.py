import json
import os

_CURVE_PATH = os.path.join(
    os.path.dirname(os.path.dirname(__file__)), "data", "sez_stg_curve.json"
)


def load_sez_stg_curve() -> dict:
    with open(_CURVE_PATH, encoding="utf-8") as f:
        return json.load(f)


def calculate_sez_stg_extraction(load_mw: float, config: dict = None) -> dict:
    config = config or load_sez_stg_curve()
    curve = sorted(config["curve"], key=lambda row: float(row["load_mw"]))
    load = float(load_mw)

    if load <= 0 or not curve:
        return {
            "load_mw": max(0.0, load),
            "hp_inlet_tph": 0.0,
            "hp_for_power_tph": 0.0,
            "hp_for_mp_extraction_tph": 0.0,
            "mp_extraction_tph": 0.0,
            "ssc_kg_kwh": 0.0,
            "proposed_heat_rate_kcal_kwh": 0.0,
            "fy2025_26_heat_rate_kcal_kwh": 0.0,
        }

    if load <= float(curve[0]["load_mw"]):
        point = {key: float(value) for key, value in curve[0].items()}
    elif load >= float(curve[-1]["load_mw"]):
        point = {key: float(value) for key, value in curve[-1].items()}
    else:
        lower = max(
            (row for row in curve if float(row["load_mw"]) <= load),
            key=lambda row: float(row["load_mw"]),
        )
        upper = min(
            (row for row in curve if float(row["load_mw"]) >= load),
            key=lambda row: float(row["load_mw"]),
        )
        if lower == upper:
            point = {key: float(value) for key, value in lower.items()}
        else:
            fraction = (
                (load - float(lower["load_mw"]))
                / (float(upper["load_mw"]) - float(lower["load_mw"]))
            )
            point = {
                key: float(lower[key]) + fraction * (float(upper[key]) - float(lower[key]))
                for key in lower
            }

    hp_for_mp = max(0.0, point["hp_inlet_tph"] - point["hp_for_power_tph"])
    mp_extraction = hp_for_mp * (
        float(config["hp_exergy_factor"]) / float(config["mp_exergy_factor"])
    )
    point.update({
        "load_mw": load,
        "hp_for_mp_extraction_tph": hp_for_mp,
        "mp_extraction_tph": mp_extraction,
    })
    return point
