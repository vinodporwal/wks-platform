import pytest

from engine.sez_stg_calc import calculate_sez_stg_extraction
from engine.u4u_iteration_loop import U4UIterationLoop


def test_sez_stg_curve_at_18_mw():
    result = calculate_sez_stg_extraction(18.0)

    assert result["hp_inlet_tph"] == pytest.approx(432.0)
    assert result["hp_for_power_tph"] == pytest.approx(109.0)
    assert result["hp_for_mp_extraction_tph"] == pytest.approx(323.0)
    assert result["mp_extraction_tph"] == pytest.approx(323.0 * 0.238 / 0.178)
    assert result["ssc_kg_kwh"] == pytest.approx(6.05)
    assert result["fy2025_26_heat_rate_kcal_kwh"] == pytest.approx(959.0)


def test_sez_stg_curve_interpolates_per_asset_load():
    result = calculate_sez_stg_extraction(17.9)

    assert result["hp_inlet_tph"] == pytest.approx(430.7)
    assert result["hp_for_power_tph"] == pytest.approx(108.7)
    assert result["hp_for_mp_extraction_tph"] == pytest.approx(322.0)
    assert result["mp_extraction_tph"] == pytest.approx(322.0 * 0.238 / 0.178)


def test_sez_u4u_uses_curve_for_hp_power_and_mp_extraction():
    loop = U4UIterationLoop.__new__(U4UIterationLoop)
    power_result = {
        "assets": [{
            "asset_name": "JMD - SEZ STG Power plant 2",
            "stg_hp_for_power_mt": 109.0 * 720.0,
            "stg_hp_for_mp_consumption_mt": 323.0 * 720.0,
            "stg_mp_extraction_mt": 323.0 * 0.238 / 0.178 * 720.0,
        }]
    }
    details = [
        {
            "producer": "JMD - SEZ STG Power plant 2",
            "material": "HP Steam_Dis",
            "generation": 18_000.0 * 720.0,
            "norm": 0.0058,
            "quantity": 18_000.0 * 720.0 * 0.0058,
        },
        {
            "producer": "STG2_MP STEAM",
            "material": "HP Steam_Dis",
            "generation": 231_790.0,
            "norm": 0.747899,
            "quantity": 231_790.0 * 0.747899,
        },
        {
            "producer": "STG2_MP STEAM",
            "material": "Boiler Feed Water",
            "generation": 231_790.0,
            "norm": 0.049,
            "quantity": 231_790.0 * 0.049,
        },
    ]
    u4u = {
        "HP Steam_Dis": details[0]["quantity"] + details[1]["quantity"],
        "Boiler Feed Water": details[2]["quantity"],
    }

    loop._apply_sez_stg_extraction_u4u(power_result, u4u, details)

    mp_generation = 323.0 * 0.238 / 0.178 * 720.0
    assert details[0]["quantity"] == pytest.approx(109.0 * 720.0)
    assert details[1]["generation"] == pytest.approx(mp_generation)
    assert details[1]["quantity"] == pytest.approx(323.0 * 720.0)
    assert details[1]["norm"] == pytest.approx(0.178 / 0.238)
    assert details[2]["generation"] == pytest.approx(mp_generation)
    assert details[2]["quantity"] == pytest.approx(mp_generation * 0.049)
    assert u4u["HP Steam_Dis"] == pytest.approx((109.0 + 323.0) * 720.0)


def test_powergen_is_selected_before_power_distribution():
    norms = {
        "Power_Dis": {"producer_uom": "KWH", "consumptions": []},
        "POWERGEN": {
            "producer_uom": "KWH",
            "consumptions": [{"source_plant": "JMD - SEZ GT Power plant 1"}],
        },
    }

    name, info = U4UIterationLoop._find_power_producer(norms, [])

    assert name == "POWERGEN"
    assert info is norms["POWERGEN"]


def test_nonstandard_power_producer_uses_asset_name_fallback():
    norms = {
        "Power_Dis": {"producer_uom": "KWH", "consumptions": []},
        "Plant Generation": {
            "producer_uom": "KWH",
            "consumptions": [{"source_plant": "Other CPP GT 1"}],
        },
    }
    assets = [{"asset_name": "Other CPP GT 1"}]

    name, info = U4UIterationLoop._find_power_producer(norms, assets)

    assert name == "Plant Generation"
    assert info is norms["Plant Generation"]


def test_single_kwh_producer_keeps_existing_fallback_behavior():
    norms = {
        "Existing Power Utility": {"producer_uom": "KWH", "consumptions": []},
        "Steam": {"producer_uom": "MT", "consumptions": []},
    }

    name, info = U4UIterationLoop._find_power_producer(norms, [])

    assert name == "Existing Power Utility"
    assert info is norms["Existing Power Utility"]
