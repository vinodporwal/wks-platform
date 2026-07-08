"""
Norm lookup service for month-wise utility norms from DB.

This module provides strict, cached lookup helpers so calculation code can
use norms from NormsMonthDetail/NormsHeader instead of hardcoded values.
"""

from functools import lru_cache
from typing import Dict, List, Optional

from database.connection import get_connection


def _clean(value: Optional[str]) -> str:
    return (value or "").strip().upper()


@lru_cache(maxsize=128)
def _load_month_norm_rows(month: int, year: int) -> List[Dict]:
    conn = get_connection()
    cur = conn.cursor()
    try:
        cur.execute(
            """
            SELECT
                p.Name AS PlantName,
                nh.UtilityName,
                nh.MaterialName,
                nh.AccountName,
                nh.IssuingPlantName,
                nmd.Norms,
                nmd.QTY
            FROM NormsMonthDetail nmd
            INNER JOIN NormsHeader nh ON nh.Id = nmd.NormsHeader_FK_Id
            INNER JOIN Plants p ON p.Id = nh.Plant_FK_Id
            INNER JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
            WHERE fym.Month = ? AND fym.Year = ?
              AND nh.IsActive = 1
            """,
            (month, year),
        )
        rows = []
        for row in cur.fetchall():
            rows.append(
                {
                    "plant_name": row[0],
                    "utility_name": row[1],
                    "material_name": row[2],
                    "account_name": row[3],
                    "issuing_plant_name": row[4],
                    "norm": float(row[5]) if row[5] is not None else None,
                    "qty": float(row[6]) if row[6] is not None else 0.0,
                }
            )
        return rows
    finally:
        conn.close()


def clear_norm_cache() -> None:
    _load_month_norm_rows.cache_clear()


def get_month_norm(
    month: int,
    year: int,
    plant_name: str,
    utility_name: str,
    material_name: str,
    account_name: Optional[str] = None,
    issuing_plant_name: Optional[str] = None,
    required: bool = True,
) -> float:
    """
    Fetch a norm value from DB for the given month/year and norm identity.

    Matching priority:
    1) exact Plant + Utility + Material
    2) optional exact AccountName filter
    3) optional exact IssuingPlantName filter

    Raises ValueError when `required=True` and no unique row is found.
    """
    rows = _load_month_norm_rows(int(month), int(year))
    target = [
        row
        for row in rows
        if _clean(row["plant_name"]) == _clean(plant_name)
        and _clean(row["utility_name"]) == _clean(utility_name)
        and _clean(row["material_name"]) == _clean(material_name)
    ]

    if account_name is not None:
        target = [row for row in target if _clean(row["account_name"]) == _clean(account_name)]
    if issuing_plant_name is not None:
        target = [
            row
            for row in target
            if _clean(row["issuing_plant_name"]) == _clean(issuing_plant_name)
        ]

    if not target:
        if required:
            raise ValueError(
                f"Norm not found for Month/Year={month}/{year}, "
                f"Plant='{plant_name}', Utility='{utility_name}', Material='{material_name}', "
                f"Account='{account_name}', IssuingPlant='{issuing_plant_name}'"
            )
        return 0.0

    if len(target) > 1:
        if required:
            sample = ", ".join(
                f"(Account={r['account_name']}, Issuing={r['issuing_plant_name']}, Norm={r['norm']})"
                for r in target[:5]
            )
            raise ValueError(
                f"Ambiguous norm rows for Month/Year={month}/{year}, "
                f"Plant='{plant_name}', Utility='{utility_name}', Material='{material_name}'. "
                f"Provide AccountName/IssuingPlantName. Matches: {sample}"
            )
        return 0.0

    norm = target[0]["norm"]
    if norm is None:
        if required:
            raise ValueError(
                f"Norm is NULL for Month/Year={month}/{year}, "
                f"Plant='{plant_name}', Utility='{utility_name}', Material='{material_name}'"
            )
        return 0.0
    return float(norm)


def get_all_norms_for_material(
    month: int,
    year: int,
    material_name: str,
    plant_name: Optional[str] = None,
    account_name: Optional[str] = "Utilities",
    issuing_plant_name: Optional[str] = None,
) -> List[Dict]:
    """
    Fetch ALL norm rows where MaterialName matches.

    This is used for U4U calculations: given a material (e.g. 'Boiler Feed Water'),
    return every utility that consumes it along with the norm value.

    Returns a list of dicts with keys:
        plant_name, utility_name, material_name, account_name,
        issuing_plant_name, norm, qty
    """
    rows = _load_month_norm_rows(int(month), int(year))
    result = []
    for row in rows:
        if _clean(row["material_name"]) != _clean(material_name):
            continue
        if plant_name is not None and _clean(row["plant_name"]) != _clean(plant_name):
            continue
        if account_name is not None and _clean(row["account_name"]) != _clean(account_name):
            continue
        if issuing_plant_name is not None and _clean(row["issuing_plant_name"]) != _clean(issuing_plant_name):
            continue
        result.append(row)
    return result


def get_all_norms_for_utility(
    month: int,
    year: int,
    utility_name: str,
    plant_name: Optional[str] = None,
    account_name: Optional[str] = "Utilities",
    issuing_plant_name: Optional[str] = None,
) -> List[Dict]:
    """
    Fetch ALL norm rows where UtilityName matches.

    This is the reverse of get_all_norms_for_material: given a utility
    (e.g. 'Boiler Feed Water'), return every material it consumes along
    with the norm value.

    Returns a list of dicts with keys:
        plant_name, utility_name, material_name, account_name,
        issuing_plant_name, norm, qty
    """
    rows = _load_month_norm_rows(int(month), int(year))
    result = []
    for row in rows:
        if _clean(row["utility_name"]) != _clean(utility_name):
            continue
        if plant_name is not None and _clean(row["plant_name"]) != _clean(plant_name):
            continue
        if account_name is not None and _clean(row["account_name"]) != _clean(account_name):
            continue
        if issuing_plant_name is not None and _clean(row["issuing_plant_name"]) != _clean(issuing_plant_name):
            continue
        result.append(row)
    return result
