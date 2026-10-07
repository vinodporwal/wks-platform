"""Focused tests for the standalone global JMD power balance."""

import csv
import sys
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1] / "scripts"
if str(SCRIPTS) not in sys.path:
    sys.path.insert(0, str(SCRIPTS))

from jmd_power_balance import (  # noqa: E402
    CppPowerInput,
    PowerAsset,
    read_bpc_power_baseline,
    read_register_power_demand,
    solve_global_power_balance,
    validate_balanced,
    validate_bpc_power_closure,
)


CPP_IDS = (
    "DTA-CPP",
    "DTA-PCG-CPP",
    "SEZ-CPP",
    "SEZ-PCG-CPP",
    "C2-CPP",
)


def test_global_dispatch_closes_five_cpp_pool():
    inputs = [
        CppPowerInput("DTA-CPP", 100.0, 20.0),
        CppPowerInput("DTA-PCG-CPP", 40.0),
        CppPowerInput("SEZ-CPP", 80.0),
        CppPowerInput("SEZ-PCG-CPP", 60.0),
        CppPowerInput("C2-CPP", 20.0),
    ]
    assets = [
        PowerAsset("sez-gt", "SEZ-CPP", "SEZ GT", 1, 1.0, 0.0, 200.0),
        PowerAsset("pcg-gt", "DTA-PCG-CPP", "DTA PCG GT", 1, 1.0, 0.0, 100.0),
        PowerAsset("dta-gt", "DTA-CPP", "DTA GT", 2, 1.0, 0.0, 100.0),
        PowerAsset("c2-gt", "C2-CPP", "C2 GT", 3, 1.0, 0.0, 100.0),
        PowerAsset("sez-pcg-stg", "SEZ-PCG-CPP", "SEZ PCG STG", 4, 1.0, 0.0, 100.0),
    ]

    result = solve_global_power_balance(4, 2026, inputs, assets)

    validate_balanced(result)
    assert result.total_demand_mwh == 300.0
    assert result.total_external_import_mwh == 20.0
    assert result.total_generation_mwh == 280.0
    assert abs(sum(result.plug_by_cpp_mwh.values())) < 1e-9
    assert result.unmatched_import_mwh == 0.0
    assert result.unmatched_export_mwh == 0.0
    assert sum(transfer.quantity_mwh for transfer in result.transfers) == 160.0


def test_mandatory_minimum_is_reported_as_surplus():
    inputs = [CppPowerInput("DTA-CPP", 10.0)]
    assets = [PowerAsset("dta-mandatory", "DTA-CPP", "DTA mandatory GT", 1, 1.0, 50.0, 100.0, True)]

    result = solve_global_power_balance(4, 2026, inputs, assets)

    assert result.total_generation_mwh == 50.0
    assert result.surplus_mwh == 40.0
    assert result.closure_residual_mwh == -40.0


def _bpc_row(node, material, april_quantity):
    row = [""] * 59
    row[0] = "distribution"
    row[1] = node
    row[2] = "Power_Dis"
    row[4] = "KWH"
    row[6] = material
    row[12] = str(april_quantity)
    return row


def test_bpc_power_parser_validates_signed_plug_closure(tmp_path):
    path = tmp_path / "Norm, Qty, Cost .csv"
    rows = [[""] * 59 for _ in range(4)]
    rows.extend(
        [
            _bpc_row("36H2", "Power", 10_000),
            _bpc_row("36BK", "Power", -20_000),
            _bpc_row("36KO", "Power", 5_000),
            _bpc_row("36BX", "Power", -15_000),
            _bpc_row("36KQ", "Power", 20_000),
            _bpc_row("36BK", "Power from CTU", 30_000),
            _bpc_row("36BK", "POWERGEN", 100_000),
        ]
    )
    with path.open("w", encoding="utf-16", newline="") as handle:
        csv.writer(handle, delimiter="\t").writerows(rows)

    baseline = read_bpc_power_baseline(path)
    residuals = validate_bpc_power_closure(baseline)

    assert residuals["Apr"] == 0.0
    assert baseline["Apr"]["C2-CPP"]["plug_mwh"] == 10.0
    assert baseline["Apr"]["DTA-CPP"]["ctu_mwh"] == 30.0
    assert baseline["Apr"]["DTA-CPP"]["generation_mwh"] == 100.0


def _register_row(utility, node, material_activity, april_kwh):
    row = [""] * 28
    row[0] = utility
    row[4] = node
    row[14] = material_activity
    row[16] = str(april_kwh)
    return row


def test_register_power_demand_splits_u4u_leg(tmp_path):
    path = tmp_path / "Sender Receiver Costcenter.csv"
    rows = [
        _register_row("Power_Dis", "JMD - Utility/Power Dist", "", 200_000),
        _register_row("Power_Dis", "JMD - Utility/Power Dist", "Cooling Water", 78_000),
        _register_row("Power_Dis", "JMD - SEZ Distribution (Power)", "Oxygen", 57_000),
        _register_row("Power_Dis", "JMD - DTA-C2 Power & UTILITY", "", 80_000),
        _register_row("LP Steam_Dis", "JMD - Utility/Power Dist", "", 999_999),
        _register_row("Power_Dis", "Unknown Node", "x", 5_000),
        _register_row("Power_Dis", "Total", "x", 5_000),
    ]
    with path.open("w", encoding="utf-16", newline="") as handle:
        csv.writer(handle, delimiter="\t").writerows(rows)

    demand = read_register_power_demand(4, path)

    assert demand["DTA-CPP"]["proc_fixed_mwh"] == 200.0
    assert demand["DTA-CPP"]["u4u_mwh"] == 78.0
    assert demand["DTA-CPP"]["total_mwh"] == 278.0
    assert demand["SEZ-CPP"]["u4u_mwh"] == 57.0
    assert demand["C2-CPP"]["proc_fixed_mwh"] == 80.0
    assert demand["SEZ-PCG-CPP"]["total_mwh"] == 0.0


def test_u4u_decomposition_flows_into_result():
    inputs = [
        CppPowerInput("DTA-CPP", 300.0, 0.0, process_fixed_mwh=220.0, u4u_mwh=80.0),
        CppPowerInput("C2-CPP", 100.0, 0.0, process_fixed_mwh=48.0, u4u_mwh=52.0),
    ]
    assets = [PowerAsset("gt", "DTA-CPP", "DTA GT", 1, 1.0, 0.0, 400.0)]

    result = solve_global_power_balance(4, 2026, inputs, assets)

    assert result.u4u_by_cpp_mwh["DTA-CPP"] == 80.0
    assert result.process_fixed_by_cpp_mwh["C2-CPP"] == 48.0
    assert result.total_demand_mwh == 400.0
