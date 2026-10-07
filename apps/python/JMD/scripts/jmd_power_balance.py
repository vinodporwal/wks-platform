"""Read-only global JMD power balance.

This module is the first standalone implementation slice for the five-CPP power
pool. It deliberately does not call the production save path and does not write
to SQL Server.

The pure solver accepts all five CPP demands, all available power assets, and
external imports (currently CTU). It dispatches the combined asset pool by
monthly priority and derives each CPP's signed power plug:

    plug = demand - generation - external_import

A balanced run satisfies:

    sum(five CPP plugs) == 0

Demand has three legs:

    demand = process + fixed + U4U

The database stores process and fixed consumption as planned quantities, but
U4U (utility-for-utility) power only as consumption *norms* — the quantity is
derived inside the engine's iteration loop. The standalone loader therefore
takes the planned U4U leg from the BPC demand register
(``Sender Receiver Costcenter.csv``: col A = ``Power_Dis``, col O non-empty =
U4U leg). ``--u4u-source none`` reproduces the raw process+fixed-only view.

The command-line adapter can load inputs read-only from the existing database,
or validate the signed plug closure in the combined BPC output file.
"""

from __future__ import annotations

import argparse
import csv
import json
import logging
import sys
from collections import defaultdict
from dataclasses import asdict, dataclass, field
from pathlib import Path
from typing import Iterable, Mapping, Sequence

# Allow `py scripts/jmd_power_balance.py` from the JMD application directory.
JMD_ROOT = Path(__file__).resolve().parents[1]
if str(JMD_ROOT) not in sys.path:
    sys.path.insert(0, str(JMD_ROOT))

from plant_mapper import PLANT_REGISTRY  # noqa: E402

logger = logging.getLogger(__name__)

CPP_ORDER = (
    "DTA-CPP",
    "DTA-PCG-CPP",
    "SEZ-CPP",
    "SEZ-PCG-CPP",
    "C2-CPP",
)

# These are the validated Power_Dis distribution nodes from the BPC
# Distribution Mapping input. They identify electrical pool membership; they
# are not a static transfer-direction configuration.
POWER_DISTRIBUTION_NODE_BY_CPP = {
    "DTA-CPP": "36BK",
    "DTA-PCG-CPP": "36KO",
    "SEZ-CPP": "36BX",
    "SEZ-PCG-CPP": "36KQ",
    "C2-CPP": "36H2",
}

MONTHS = ("Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan", "Feb", "Mar")
MONTH_NUMBER_BY_INDEX = {index: index + 4 if index < 9 else index - 8 for index in range(12)}

DEFAULT_BPC_OUTPUT = (
    JMD_ROOT.parent / "files" / "Norm, Qty, Cost .csv"
)

# BPC demand register — the only source of *planned* U4U power quantities.
DEFAULT_REGISTER = (
    JMD_ROOT.parent / "files" / "Sender Receiver Costcenter.csv"
)

# Register col E names for the five power distribution nodes. Verified against
# BPC: each node's U4U draw feeds only its own CPP's utility plants, so the
# node name is an unambiguous CPP key.
REGISTER_NODE_TO_CPP = {
    "JMD - Utility/Power Dist": "DTA-CPP",
    "RIL-JW Plant-DTA PCG": "DTA-PCG-CPP",
    "JMD - SEZ Distribution (Power)": "SEZ-CPP",
    "RIL-JW Plant-SEZ PCG": "SEZ-PCG-CPP",
    "JMD - DTA-C2 Power & UTILITY": "C2-CPP",
}


@dataclass(frozen=True)
class PowerAsset:
    """One available power-generation asset for one CPP/month."""

    asset_id: str
    cpp_id: str
    asset_name: str
    priority: int
    operating_hours: float
    min_mw: float
    max_mw: float
    mandatory: bool = False

    @property
    def min_mwh(self) -> float:
        return max(0.0, self.min_mw) * max(0.0, self.operating_hours)

    @property
    def max_mwh(self) -> float:
        return max(0.0, self.max_mw) * max(0.0, self.operating_hours)


@dataclass(frozen=True)
class CppPowerInput:
    cpp_id: str
    demand_mwh: float
    external_import_mwh: float = 0.0
    # Demand decomposition for reporting — demand_mwh remains the solver input.
    process_fixed_mwh: float = 0.0
    u4u_mwh: float = 0.0


@dataclass
class AssetDispatch:
    asset_id: str
    cpp_id: str
    asset_name: str
    priority: int
    operating_hours: float
    min_mw: float
    max_mw: float
    mandatory: bool
    dispatched_mw: float = 0.0
    dispatched_mwh: float = 0.0


@dataclass
class PowerTransfer:
    sender_cpp_id: str
    receiver_cpp_id: str
    quantity_mwh: float


@dataclass
class GlobalPowerBalance:
    month: int
    year: int
    demand_by_cpp_mwh: dict[str, float]
    external_import_by_cpp_mwh: dict[str, float]
    generation_by_cpp_mwh: dict[str, float]
    plug_by_cpp_mwh: dict[str, float]
    total_demand_mwh: float
    total_external_import_mwh: float
    total_generation_mwh: float
    closure_residual_mwh: float
    surplus_mwh: float
    deficit_mwh: float
    transfers: list[PowerTransfer] = field(default_factory=list)
    unmatched_import_mwh: float = 0.0
    unmatched_export_mwh: float = 0.0
    assets: list[AssetDispatch] = field(default_factory=list)
    # Demand decomposition / reconciliation aids (reporting only).
    process_fixed_by_cpp_mwh: dict[str, float] = field(default_factory=dict)
    u4u_by_cpp_mwh: dict[str, float] = field(default_factory=dict)
    register_demand_by_cpp_mwh: dict[str, float] = field(default_factory=dict)

    @property
    def is_balanced(self) -> bool:
        return self.deficit_mwh <= 1e-6 and self.surplus_mwh <= 1e-6 and abs(self.closure_residual_mwh) <= 1e-6


def _cpp_value(mapping: Mapping[str, float], cpp_id: str) -> float:
    return float(mapping.get(cpp_id, 0.0) or 0.0)


def _normalise_asset(asset: PowerAsset | Mapping[str, object]) -> PowerAsset:
    if isinstance(asset, PowerAsset):
        return asset
    return PowerAsset(
        asset_id=str(asset["asset_id"]),
        cpp_id=str(asset["cpp_id"]),
        asset_name=str(asset.get("asset_name", asset["asset_id"])),
        priority=int(asset.get("priority", 999) or 999),
        operating_hours=float(asset.get("operating_hours", asset.get("op_hours", 0.0)) or 0.0),
        min_mw=float(asset.get("min_mw", 0.0) or 0.0),
        max_mw=float(asset.get("max_mw", 0.0) or 0.0),
        mandatory=bool(asset.get("mandatory", False)),
    )


def _allocate_equal_mw(
    dispatch: list[AssetDispatch],
    indexes: list[int],
    allocation_mwh: float,
) -> float:
    """Allocate one priority group at equal additional MW, respecting caps."""
    remaining = max(0.0, allocation_mwh)
    active = list(indexes)

    while active and remaining > 1e-9:
        total_hours = sum(dispatch[index].operating_hours for index in active)
        if total_hours <= 0:
            break

        target_additional_mw = remaining / total_hours
        newly_capped: list[int] = []
        allocated_this_round = 0.0

        for index in active:
            item = dispatch[index]
            headroom_mw = max(0.0, item.max_mw - item.dispatched_mw)
            additional_mw = min(target_additional_mw, headroom_mw)
            if additional_mw <= 0:
                newly_capped.append(index)
                continue

            item.dispatched_mw += additional_mw
            item.dispatched_mwh = item.dispatched_mw * item.operating_hours
            allocated = additional_mw * item.operating_hours
            allocated_this_round += allocated
            if additional_mw >= headroom_mw - 1e-9:
                newly_capped.append(index)

        if allocated_this_round <= 1e-9:
            break
        remaining -= allocated_this_round
        active = [index for index in active if index not in newly_capped]

    return max(0.0, remaining)


def _dispatch_global_assets(
    assets: Sequence[PowerAsset | Mapping[str, object]],
    net_generation_target_mwh: float,
) -> list[AssetDispatch]:
    """Dispatch all JMD assets together using monthly priority order.

    The dispatch policy mirrors the existing JMD all-min-first behavior:

    1. Mandatory assets start at minimum load.
    2. If demand remains, optional assets are brought online at minimum load.
    3. Remaining demand is ramped by ascending priority.
    4. Assets with equal priority share additional load at equal MW.
    """
    normalised = [_normalise_asset(asset) for asset in assets]
    dispatch = [
        AssetDispatch(
            asset_id=asset.asset_id,
            cpp_id=asset.cpp_id,
            asset_name=asset.asset_name,
            priority=asset.priority,
            operating_hours=asset.operating_hours,
            min_mw=asset.min_mw,
            max_mw=asset.max_mw,
            mandatory=asset.mandatory,
        )
        for asset in normalised
        if asset.operating_hours > 0 and asset.max_mw > 0
    ]

    target = max(0.0, float(net_generation_target_mwh))
    total_generation = 0.0

    # Mandatory minimum load.
    for item in dispatch:
        if item.mandatory:
            item.dispatched_mw = min(item.min_mw, item.max_mw)
            item.dispatched_mwh = item.dispatched_mw * item.operating_hours
            total_generation += item.dispatched_mwh

    # Optional units are started only if mandatory minimum generation does not
    # satisfy the net target. This preserves the existing dispatch contract.
    if target > total_generation + 1e-9:
        for item in dispatch:
            if not item.mandatory:
                item.dispatched_mw = min(item.min_mw, item.max_mw)
                item.dispatched_mwh = item.dispatched_mw * item.operating_hours
                total_generation += item.dispatched_mwh

    remaining = max(0.0, target - total_generation)
    if remaining <= 1e-9:
        return dispatch

    priority_groups: dict[int, list[int]] = defaultdict(list)
    for index, item in enumerate(dispatch):
        # A zero-minimum optional asset is still eligible to start when the
        # global pool needs additional generation.
        if item.max_mw > item.dispatched_mw + 1e-9:
            priority_groups[item.priority].append(index)

    for priority in sorted(priority_groups):
        if remaining <= 1e-9:
            break
        indexes = [
            index for index in priority_groups[priority]
            if dispatch[index].max_mw > dispatch[index].dispatched_mw + 1e-9
        ]
        if not indexes:
            continue

        group_headroom = sum(
            (dispatch[index].max_mw - dispatch[index].dispatched_mw)
            * dispatch[index].operating_hours
            for index in indexes
        )
        allocation = min(remaining, group_headroom)
        unallocated = _allocate_equal_mw(dispatch, indexes, allocation)
        remaining -= allocation - unallocated

    return dispatch


def _match_power_transfers(
    plug_by_cpp_mwh: Mapping[str, float],
    tolerance_mwh: float = 1e-6,
) -> tuple[list[PowerTransfer], float, float]:
    """Match signed CPP positions into a deterministic pool transfer matrix.

    This is a reporting decomposition of the closed electrical pool. It does
    not claim that every pair is a dedicated physical line; route constraints
    can be added later from the validated SR/distribution mapping.
    """
    exporters = [[cpp, -value] for cpp, value in plug_by_cpp_mwh.items() if value < -tolerance_mwh]
    importers = [[cpp, value] for cpp, value in plug_by_cpp_mwh.items() if value > tolerance_mwh]
    transfers: list[PowerTransfer] = []

    for exporter in exporters:
        for importer in importers:
            quantity = min(exporter[1], importer[1])
            if quantity <= tolerance_mwh:
                continue
            transfers.append(PowerTransfer(exporter[0], importer[0], round(quantity, 6)))
            exporter[1] -= quantity
            importer[1] -= quantity
            if exporter[1] <= tolerance_mwh:
                break

    unmatched_export = sum(max(0.0, value) for _, value in exporters)
    unmatched_import = sum(max(0.0, value) for _, value in importers)
    return transfers, round(unmatched_import, 6), round(unmatched_export, 6)


def solve_global_power_balance(
    month: int,
    year: int,
    cpp_inputs: Sequence[CppPowerInput],
    assets: Sequence[PowerAsset | Mapping[str, object]],
    tolerance_mwh: float = 1e-6,
) -> GlobalPowerBalance:
    """Solve one month for the complete five-CPP electrical pool."""
    demand_by_cpp = {cpp.cpp_id: float(cpp.demand_mwh) for cpp in cpp_inputs}
    external_by_cpp = {cpp.cpp_id: float(cpp.external_import_mwh) for cpp in cpp_inputs}
    total_demand = sum(demand_by_cpp.values())
    total_external = sum(external_by_cpp.values())

    dispatch = _dispatch_global_assets(assets, total_demand - total_external)
    generation_by_cpp: dict[str, float] = defaultdict(float)
    for item in dispatch:
        generation_by_cpp[item.cpp_id] += item.dispatched_mwh

    generation_by_cpp = {
        cpp_id: round(generation_by_cpp.get(cpp_id, 0.0), 6)
        for cpp_id in demand_by_cpp
    }
    plug_by_cpp = {
        cpp_id: round(
            demand_by_cpp[cpp_id]
            - generation_by_cpp.get(cpp_id, 0.0)
            - external_by_cpp.get(cpp_id, 0.0),
            6,
        )
        for cpp_id in demand_by_cpp
    }

    closure = round(sum(plug_by_cpp.values()), 6)
    system_balance = round(total_generation(dispatch) + total_external - total_demand, 6)
    transfers, unmatched_import, unmatched_export = _match_power_transfers(
        plug_by_cpp, tolerance_mwh=tolerance_mwh
    )

    return GlobalPowerBalance(
        month=month,
        year=year,
        demand_by_cpp_mwh={key: round(value, 6) for key, value in demand_by_cpp.items()},
        external_import_by_cpp_mwh={key: round(value, 6) for key, value in external_by_cpp.items()},
        generation_by_cpp_mwh=generation_by_cpp,
        plug_by_cpp_mwh=plug_by_cpp,
        total_demand_mwh=round(total_demand, 6),
        total_external_import_mwh=round(total_external, 6),
        total_generation_mwh=round(total_generation(dispatch), 6),
        closure_residual_mwh=closure,
        surplus_mwh=max(0.0, system_balance),
        deficit_mwh=max(0.0, -system_balance),
        transfers=transfers,
        unmatched_import_mwh=unmatched_import,
        unmatched_export_mwh=unmatched_export,
        assets=dispatch,
        process_fixed_by_cpp_mwh={
            cpp.cpp_id: round(cpp.process_fixed_mwh, 6) for cpp in cpp_inputs
        },
        u4u_by_cpp_mwh={
            cpp.cpp_id: round(cpp.u4u_mwh, 6) for cpp in cpp_inputs
        },
    )


def total_generation(dispatch: Iterable[AssetDispatch]) -> float:
    return sum(item.dispatched_mwh for item in dispatch)


def validate_balanced(result: GlobalPowerBalance, tolerance_mwh: float = 1e-6) -> None:
    """Raise AssertionError when a closed power pool is not balanced."""
    errors = []
    if abs(result.closure_residual_mwh) > tolerance_mwh:
        errors.append(f"plug closure residual={result.closure_residual_mwh} MWh")
    if result.deficit_mwh > tolerance_mwh:
        errors.append(f"generation deficit={result.deficit_mwh} MWh")
    if result.surplus_mwh > tolerance_mwh:
        errors.append(f"generation surplus={result.surplus_mwh} MWh")
    if result.unmatched_import_mwh > tolerance_mwh or result.unmatched_export_mwh > tolerance_mwh:
        errors.append(
            "transfer decomposition unmatched "
            f"import={result.unmatched_import_mwh} MWh "
            f"export={result.unmatched_export_mwh} MWh"
        )
    if errors:
        raise AssertionError("Global JMD power balance failed: " + "; ".join(errors))


def _number(value: str | None) -> float:
    try:
        return float((value or "").strip().replace(",", ""))
    except (TypeError, ValueError):
        return 0.0


def _bpc_month_quantity(row: list[str], month_index: int) -> float:
    # BPC output layout: 11 identity columns, then each month has
    # Norms/Quantity/Amount/Price. Power quantity is stored in KWH.
    return _number(row[12 + month_index * 4]) if len(row) > 12 + month_index * 4 else 0.0


def read_bpc_power_baseline(path: str | Path) -> dict[str, dict[str, dict[str, float]]]:
    """Read signed Power, POWERGEN, and CTU rows from the combined BPC output.

    Returns ``{month: {cpp_id: {generation_mwh, ctu_mwh, plug_mwh}}}``.
    This validates the BPC electrical closure independently of the database.
    """
    path = Path(path)
    by_month = {
        month: {
            cpp_id: {"generation_mwh": 0.0, "ctu_mwh": 0.0, "plug_mwh": 0.0}
            for cpp_id in CPP_ORDER
        }
        for month in MONTHS
    }
    node_to_cpp = {node: cpp for cpp, node in POWER_DISTRIBUTION_NODE_BY_CPP.items()}

    with path.open("r", encoding="utf-16", newline="") as handle:
        rows = list(csv.reader(handle, delimiter="\t"))

    for row in rows[4:]:
        if len(row) < 13:
            continue
        if row[2].strip().lower() != "power_dis":
            continue
        cpp_id = node_to_cpp.get(row[1].strip())
        if cpp_id is None:
            continue

        material = row[6].strip().upper()
        kind = None
        if material == "POWER":
            kind = "plug_mwh"
        elif material == "POWER FROM CTU":
            kind = "ctu_mwh"
        elif material.startswith("POWERGEN"):
            kind = "generation_mwh"
        if kind is None:
            continue

        for month_index, month in enumerate(MONTHS):
            by_month[month][cpp_id][kind] += _bpc_month_quantity(row, month_index) / 1000.0

    for month in MONTHS:
        for cpp_id in CPP_ORDER:
            for key in by_month[month][cpp_id]:
                by_month[month][cpp_id][key] = round(by_month[month][cpp_id][key], 6)
    return by_month


def validate_bpc_power_closure(
    baseline: Mapping[str, Mapping[str, Mapping[str, float]]],
    tolerance_mwh: float = 0.1,
) -> dict[str, float]:
    """Return monthly signed-plug residuals and fail if BPC is not closed."""
    residuals = {}
    failures = []
    for month, cpp_values in baseline.items():
        residual = round(sum(values["plug_mwh"] for values in cpp_values.values()), 6)
        residuals[month] = residual
        if abs(residual) > tolerance_mwh:
            failures.append(f"{month}={residual} MWh")
    if failures:
        raise AssertionError("BPC power plug closure failed: " + ", ".join(failures))
    return residuals


def read_register_power_demand(
    month: int,
    path: str | Path = DEFAULT_REGISTER,
) -> dict[str, dict[str, float]]:
    """Read the BPC demand register's ``Power_Dis`` rows for one month.

    Register layout (``Sender Receiver Costcenter.csv``, UTF-16/tab):

    - col A (0): utility demanded — only ``Power_Dis`` rows are power demand
    - col E (4): issuing CPP power node name
    - col O (14): material activity — empty = process+fixed leg,
      non-empty = U4U leg (power consumed by a utility producer)
    - cols Q..AB (16..27): months, Q = April of the financial year

    Returns ``{cpp_id: {"total_mwh", "proc_fixed_mwh", "u4u_mwh"}}``.
    """
    col = 16 + ((month - 4) % 12)
    demand = {
        cpp_id: {"total_mwh": 0.0, "proc_fixed_mwh": 0.0, "u4u_mwh": 0.0}
        for cpp_id in CPP_ORDER
    }

    with Path(path).open("r", encoding="utf-16", newline="") as handle:
        for row in csv.reader(handle, delimiter="\t"):
            if len(row) <= col or (row[0] or "").strip() != "Power_Dis":
                continue
            cpp_id = REGISTER_NODE_TO_CPP.get((row[4] or "").strip())
            if cpp_id is None:
                continue
            # The sheet's own 'Total' rows must never be counted.
            if any(str(row[i]).strip() == "Total" for i in (4, 8, 12, 14)):
                continue
            qty_mwh = _number(row[col]) / 1000.0  # register stores KWH
            if qty_mwh == 0.0:
                continue
            demand[cpp_id]["total_mwh"] += qty_mwh
            if str(row[14]).strip():
                demand[cpp_id]["u4u_mwh"] += qty_mwh
            else:
                demand[cpp_id]["proc_fixed_mwh"] += qty_mwh

    for cpp_id in CPP_ORDER:
        for key in demand[cpp_id]:
            demand[cpp_id][key] = round(demand[cpp_id][key], 6)
    return demand


def _load_db_month(
    month: int,
    year: int,
    u4u_source: str = "register",
) -> tuple[list[CppPowerInput], list[PowerAsset], dict[str, dict[str, float]]]:
    """Load one month from existing SELECT-only query helpers.

    Demand = DB process + fixed consumption + planned U4U. U4U is not stored
    as a planned quantity in the database (only norms); with
    ``u4u_source="register"`` the planned U4U leg comes from the BPC demand
    register. ``u4u_source="none"`` gives the raw process+fixed-only view.
    """
    from database.queries import fetch_fixed_consumption, fetch_import_power, fetch_process_demands
    from engine.dispatch_engine import _build_asset_table

    register: dict[str, dict[str, float]] = {}
    if u4u_source == "register":
        try:
            register = read_register_power_demand(month)
        except OSError as exc:
            logger.warning("BPC demand register unavailable (%s) — U4U leg skipped", exc)

    cpp_inputs: list[CppPowerInput] = []
    assets: list[PowerAsset] = []

    for cpp_id in _plant_ids():
        cpp_name = PLANT_REGISTRY[cpp_id]["name"]
        process = fetch_process_demands(cpp_id, month, year)
        fixed = fetch_fixed_consumption(cpp_id, month, year)
        process_mwh = float(process.get("power_process", 0.0)) / 1000.0
        fixed_mwh = float(fixed.get("power_fixed", 0.0))
        process_fixed_mwh = process_mwh + fixed_mwh
        u4u_mwh = register.get(cpp_name, {}).get("u4u_mwh", 0.0)

        import_result = fetch_import_power(cpp_id, month, year)
        ctu_mwh = sum(
            float(source.get("mwh", 0.0))
            for source in import_result.get("per_source", [])
            if "ctu" in str(source.get("source_name", "")).lower()
        )

        cpp_inputs.append(
            CppPowerInput(
                cpp_id=cpp_name,
                demand_mwh=process_fixed_mwh + u4u_mwh,
                external_import_mwh=ctu_mwh,
                process_fixed_mwh=process_fixed_mwh,
                u4u_mwh=u4u_mwh,
            )
        )

        for asset in _build_asset_table(cpp_id, month, year):
            assets.append(
                PowerAsset(
                    asset_id=str(asset["asset_id"]),
                    cpp_id=cpp_name,
                    asset_name=str(asset.get("asset_name", asset["asset_id"])),
                    priority=int(asset.get("priority", 999)),
                    operating_hours=float(asset.get("op_hours", 0.0)),
                    min_mw=float(asset.get("min_mw", 0.0)),
                    max_mw=float(asset.get("max_mw", 0.0)),
                    mandatory=bool(asset.get("mandatory", 0)),
                )
            )
    return cpp_inputs, assets, register


def _plant_ids() -> list[str]:
    return [
        "A4AF8441-73AD-4F9F-BCF4-6734E8202F7A",
        "F6D82E68-C3B6-494F-9905-48F19DC611E3",
        "2DFEE33F-4CFD-4887-B9DD-53388AA95271",
        "D2C7FBAD-7E00-4642-B3B2-5A768FAC8D45",
        "BA558F95-8A3F-4769-9C78-FF7B6C639DDF",
    ]


def run_db_month(
    month: int,
    year: int,
    u4u_source: str = "register",
) -> GlobalPowerBalance:
    cpp_inputs, assets, register = _load_db_month(month, year, u4u_source)
    result = solve_global_power_balance(month, year, cpp_inputs, assets)
    result.register_demand_by_cpp_mwh = {
        cpp_id: values["total_mwh"] for cpp_id, values in register.items()
    }
    return result


def _json_default(value):
    if hasattr(value, "__dataclass_fields__"):
        return asdict(value)
    raise TypeError(f"Cannot serialize {type(value).__name__}")


def _print_result(result: GlobalPowerBalance) -> None:
    print(f"JMD power balance: {MONTHS[(result.month - 4) % 12]} {result.year}")
    print(f"Demand: {result.total_demand_mwh:,.2f} MWh")
    print(f"External CTU: {result.total_external_import_mwh:,.2f} MWh")
    print(f"Generation: {result.total_generation_mwh:,.2f} MWh")
    print(f"Closure residual: {result.closure_residual_mwh:,.6f} MWh")
    print(f"Surplus: {result.surplus_mwh:,.6f} MWh | Deficit: {result.deficit_mwh:,.6f} MWh")
    print("\nCPP positions:")
    for cpp_id in CPP_ORDER:
        print(
            f"  {cpp_id:<13} demand={result.demand_by_cpp_mwh.get(cpp_id, 0.0):>14,.2f} "
            f"generation={result.generation_by_cpp_mwh.get(cpp_id, 0.0):>14,.2f} "
            f"external={result.external_import_by_cpp_mwh.get(cpp_id, 0.0):>12,.2f} "
            f"plug={result.plug_by_cpp_mwh.get(cpp_id, 0.0):>14,.2f}"
        )

    if result.u4u_by_cpp_mwh or result.register_demand_by_cpp_mwh:
        print("\nDemand reconciliation (MWh):")
        print(
            f"  {'CPP':<13} {'proc+fix':>12} {'U4U':>12} {'model total':>12} "
            f"{'register':>12} {'diff':>12}"
        )
        for cpp_id in CPP_ORDER:
            pf = result.process_fixed_by_cpp_mwh.get(cpp_id, 0.0)
            u4u = result.u4u_by_cpp_mwh.get(cpp_id, 0.0)
            total = result.demand_by_cpp_mwh.get(cpp_id, 0.0)
            reg = result.register_demand_by_cpp_mwh.get(cpp_id)
            reg_txt = f"{reg:>12,.2f}" if reg is not None else f"{'—':>12}"
            diff_txt = f"{total - reg:>+12,.2f}" if reg is not None else f"{'—':>12}"
            print(
                f"  {cpp_id:<13} {pf:>12,.2f} {u4u:>12,.2f} {total:>12,.2f} "
                f"{reg_txt} {diff_txt}"
            )

    print("\nPool transfer decomposition:")
    for transfer in result.transfers:
        print(
            f"  {transfer.sender_cpp_id} -> {transfer.receiver_cpp_id}: "
            f"{transfer.quantity_mwh:,.2f} MWh"
        )
    if result.unmatched_import_mwh or result.unmatched_export_mwh:
        print(
            f"Unmatched import={result.unmatched_import_mwh:,.2f} MWh "
            f"export={result.unmatched_export_mwh:,.2f} MWh"
        )


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mode", choices=("bpc", "db"), default="bpc")
    parser.add_argument("--month", type=int, default=4)
    parser.add_argument("--year", type=int, default=2026)
    parser.add_argument("--all-months", action="store_true")
    parser.add_argument("--bpc-output", default=str(DEFAULT_BPC_OUTPUT))
    parser.add_argument(
        "--u4u-source",
        choices=("register", "none"),
        default="register",
        help="db mode: where the planned U4U power leg comes from "
             "(register = BPC demand register; none = process+fixed only)",
    )
    parser.add_argument("--json", action="store_true", dest="as_json")
    args = parser.parse_args(argv)

    if args.mode == "bpc":
        baseline = read_bpc_power_baseline(args.bpc_output)
        residuals = validate_bpc_power_closure(baseline)
        output = {"mode": "bpc", "source": args.bpc_output, "plug_closure_mwh": residuals}
        if args.as_json:
            print(json.dumps(output, indent=2))
        else:
            print(f"Validated BPC power closure: {args.bpc_output}")
            for month, residual in residuals.items():
                print(f"  {month}: {residual:,.6f} MWh")
        return 0

    if args.all_months:
        results = []
        for index in range(12):
            month = MONTH_NUMBER_BY_INDEX[index]
            year = args.year if month >= 4 else args.year + 1
            results.append(run_db_month(month, year, args.u4u_source))
        if args.as_json:
            print(json.dumps(results, default=_json_default, indent=2))
        else:
            for result in results:
                _print_result(result)
                print()
        return 0

    result = run_db_month(args.month, args.year, args.u4u_source)
    if args.as_json:
        print(json.dumps(result, default=_json_default, indent=2))
    else:
        _print_result(result)
    return 0


if __name__ == "__main__":
    logging.basicConfig(level=logging.WARNING, format="%(levelname)s %(message)s")
    raise SystemExit(main())
