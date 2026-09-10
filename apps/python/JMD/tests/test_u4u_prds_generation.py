from types import SimpleNamespace

from engine.dispatch_engine import _find_linked_gt_asset
from engine.u4u_iteration_loop import U4UIterationLoop, _DTA_PLANT_ID, _prds_generation_from_dispatch


def test_hrsg_linking_supports_gt_gtg_and_gt_power_plant_names():
    power_assets = [
        {"asset_name": "JMD - GT 2"},
        {"asset_name": "JMD - GT Power Plant 3"},
        {"asset_name": "JMD - DTA-GTG 10"},
    ]

    assert _find_linked_gt_asset("HRSG2_SHP STEAM", power_assets)["asset_name"] == "JMD - GT 2"
    assert _find_linked_gt_asset("HRSG3_SHP STEAM", power_assets)["asset_name"] == "JMD - GT Power Plant 3"
    assert _find_linked_gt_asset("HRSG10_SHP STEAM", power_assets)["asset_name"] == "JMD - DTA-GTG 10"
    assert _find_linked_gt_asset("HRSG4_SHP STEAM", power_assets) is None


CASCADE = [
    {"prds": "LP Steam PRDS", "produces": "LP Steam_Dis"},
    {"prds": "MP Steam PRDS SHP", "produces": "MP Steam_Dis"},
    {"prds": "HP Steam PRDS", "produces": "HP Steam_Dis"},
]


def test_prds_generation_uses_dispatched_net_output_for_each_grade():
    demand_detail = {
        "lp_letdown": 12610.86,
        "lp_net": 13344.83,
        "mp_letdown": 72669.95,
        "mp_net": 80744.39,
        "hp_letdown": 480858.15,
        "hp_net": 513737.34,
    }

    assert _prds_generation_from_dispatch("LP Steam PRDS", CASCADE, demand_detail, 81409.62) == 13344.83
    assert _prds_generation_from_dispatch("MP Steam PRDS SHP", CASCADE, demand_detail, 176133.53) == 80744.39
    assert _prds_generation_from_dispatch("HP Steam PRDS", CASCADE, demand_detail, 635467.39) == 513737.34


def test_prds_generation_falls_back_without_active_dispatch_letdown():
    assert _prds_generation_from_dispatch("MP Steam PRDS SHP", CASCADE, {}, 123.45) == 123.45
    assert _prds_generation_from_dispatch("Unknown PRDS", CASCADE, {"mp_letdown": 1, "mp_net": 2}, 123.45) == 123.45


def test_dta_hp_u4u_uses_residual_mp_prds_after_stg_extraction():
    loop = U4UIterationLoop.__new__(U4UIterationLoop)
    loop.plant_id = _DTA_PLANT_ID
    loop._raw_process = {}
    loop._raw_fixed = {}
    loop._all_producers = {"LP Steam_Dis", "MP Steam_Dis", "HP Steam_Dis"}
    loop.ods_reader = SimpleNamespace(
        get_steam_letdown_norms=lambda: {
            "_cascade": [
                {**CASCADE[0], "consumes": "MP Steam_Dis", "norm": 0.945},
                {**CASCADE[1], "consumes": "HP Steam_Dis", "norm": 0.9},
                {**CASCADE[2], "consumes": "SHP Steam_Dis", "norm": 0.936},
            ]
        },
        get_hrsg_byproduct_norms=lambda: {},
    )
    steam_result = {
        "assets": [],
        "demand_detail": {
            "mp_net": 80744.39,
            "hp_net": 326417.95,
            "stg_mp_extraction_mt": 108000.0,
        },
    }

    u4u, details = loop._calculate_steam_cascade_u4u(
        steam_result,
        {
            "LP Steam_Dis": 81409.62,
            "MP Steam_Dis": 176133.53,
            "HP Steam_Dis": 549617.95,
        },
    )

    mp_prds = next(row for row in details if row["producer"] == "MP Steam PRDS SHP")
    hp_prds = next(row for row in details if row["producer"] == "HP Steam PRDS")
    assert mp_prds["generation"] == 80744.39
    assert round(mp_prds["quantity"], 2) == 72669.95
    assert round(u4u["HP Steam_Dis"], 2) == 72669.95
    assert hp_prds["generation"] == 326417.95
    assert round(hp_prds["quantity"], 2) == 305527.20


def test_dta_hp_dispatch_demand_is_reduced_by_fixed_import():
    loop = U4UIterationLoop.__new__(U4UIterationLoop)
    loop.plant_id = _DTA_PLANT_ID
    loop._raw_process = {"HP Steam_Dis": 372884.0}
    loop._raw_fixed = {"HP Steam_Dis": 58464.0}
    loop._initial_steam_mt = {"HP Steam_Dis": 431348.0}
    loop._power_ods_material = None
    loop._lookup_bpc_qty = lambda utility, material: 28800.0

    demands = loop._build_dispatch_demands({"HP Steam_Dis": 549617.95})

    assert round(demands["HP Steam_Dis"], 2) == 520817.95


def test_non_dta_mp_cascade_keeps_gross_demand_behavior():
    loop = U4UIterationLoop.__new__(U4UIterationLoop)
    loop.plant_id = "OTHER-PLANT"
    loop._raw_process = {}
    loop._raw_fixed = {}
    loop._all_producers = {"LP Steam_Dis", "MP Steam_Dis", "HP Steam_Dis"}
    loop.ods_reader = SimpleNamespace(
        get_steam_letdown_norms=lambda: {
            "_cascade": [
                {**CASCADE[0], "consumes": "MP Steam_Dis", "norm": 0.945},
                {**CASCADE[1], "consumes": "HP Steam_Dis", "norm": 0.9},
                {**CASCADE[2], "consumes": "SHP Steam_Dis", "norm": 0.936},
            ]
        },
        get_hrsg_byproduct_norms=lambda: {},
    )

    u4u, details = loop._calculate_steam_cascade_u4u(
        {"assets": [], "demand_detail": {"mp_net": 80744.39}},
        {"LP Steam_Dis": 81409.62, "MP Steam_Dis": 176133.53},
    )

    mp_prds = next(row for row in details if row["producer"] == "MP Steam PRDS SHP")
    assert mp_prds["generation"] == 176133.53
    assert round(u4u["HP Steam_Dis"], 2) == 158520.18
