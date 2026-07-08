"""
NMD Norms Reader — Dynamic norms discovery from DB tables.

Reads NormsMonthDetail / NormsHeader for a given month/year ONCE and organises
the data into a consumption matrix that the NMDIterationLoop can use to
calculate U4U demands generically — without any hardcoded utility names
or norm multipliers.

Key concepts
------------
* **Generation utility** — any UtilityName in NormsHeader with
  AccountName = 'Utilities' (or 'Raw Material').  These are the utilities
  *produced* by the utility plant (POWERGEN, HRSG*_SHP STEAM,
  Boiler Feed Water, D M Water, Cooling Water 1/2, COMPRESSED AIR, …).

* **U4U material** — the MaterialName paired with a generation utility
  row.  It tells us *what that utility consumes* per unit of output
  (e.g. Boiler Feed Water consumes Power_Dis, D M Water, LP Steam_Dis).

* **Reverse-calculated norms** — some norms are not stored directly in
  NormsMonthDetail but are derived from lookup tables during iteration
  (GT NG from CPP_GTHeatRate, HRSG NG from HRSG heat-rate lookup,
  STG SHP from STG extraction lookup).  The reader flags these so the
  iteration loop knows to replace them with computed values.

The reader is cached per (month, year) so that multiple components can
share the same in-memory data without re-querying the database.
"""

from __future__ import annotations

import logging
from functools import lru_cache
from typing import Dict, List, Optional, Set, Tuple

from database.connection import get_connection
from services.norm_lookup_service import _load_month_norm_rows, _clean

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Plant-name constants used for reverse-calculation lookups
# (kept here so the reader is self-contained)
# ---------------------------------------------------------------------------
NMD_UTILITY_PLANT = "NMD - Utility Plant"
NMD_POWER_PLANT_PREFIX = "NMD - Power Plant"
NMD_STG_PLANT = "NMD - STG Power Plant"

# Materials that are reverse-calculated during iteration (not read directly
# from NormsMonthDetail).  The iteration loop will replace the placeholder
# norm with a computed value.
REVERSE_CALC_MATERIALS: Set[str] = {
    "NATURAL GAS",
}

# Utility names that are dispatchable (power dispatch via distribute_by_priority)
DISPATCHABLE_UTILITY = "POWERGEN"


# ---------------------------------------------------------------------------
# Reader cache
# ---------------------------------------------------------------------------
_READER_CACHE: Dict[Tuple[int, int], "NMDNormsReader"] = {}


class NMDNormsReader:
    """
    Cached reader for NMD month-wise norms from the database.

    Usage::

        reader = NMDNormsReader.get_reader(month=4, year=2026)
        matrix = reader.get_consumption_norms()
        gen_utils = reader.get_generation_utilities()
    """

    def __init__(self, month: int, year: int):
        self.month = int(month)
        self.year = int(year)
        self._rows: List[Dict] = []
        self._loaded = False

        # Derived structures (populated by _build_indexes)
        self._consumption_matrix: Dict[str, Dict] = {}
        self._generation_utilities: Set[str] = set()
        self._u4u_materials: Set[str] = set()
        self._powergen_norms: Dict[str, float] = {}
        self._steam_letdown_norms: Dict[str, float] = {}
        self._hrsg_byproduct_norms: Dict[str, float] = {}
        self._stg_steam_norm: float = 0.0
        self._reverse_calc_entries: List[Dict] = []

    # ------------------------------------------------------------------
    # Public class method for cached access
    # ------------------------------------------------------------------

    @classmethod
    def get_reader(cls, month: int, year: int) -> "NMDNormsReader":
        cache_key = (int(month), int(year))
        if cache_key not in _READER_CACHE:
            reader = cls(month, year)
            reader._load()
            _READER_CACHE[cache_key] = reader
        return _READER_CACHE[cache_key]

    @classmethod
    def clear_cache(cls):
        _READER_CACHE.clear()

    # ------------------------------------------------------------------
    # Internal loading & indexing
    # ------------------------------------------------------------------

    def _load(self):
        """Load all norm rows from DB and build indexes."""
        if self._loaded:
            return

        self._rows = _load_month_norm_rows(self.month, self.year)
        self._build_indexes()
        self._loaded = True

        logger.info(
            "  [NMD NormsReader] Loaded %d rows for %d/%d — %d generation utilities, %d U4U materials",
            len(self._rows), self.month, self.year,
            len(self._generation_utilities), len(self._u4u_materials),
        )

    def _build_indexes(self):
        """Build the consumption matrix and derived indexes from raw rows."""
        for row in self._rows:
            utility = (row.get("utility_name") or "").strip()
            material = (row.get("material_name") or "").strip()
            account = (row.get("account_name") or "").strip()
            plant = (row.get("plant_name") or "").strip()
            issuing_plant = (row.get("issuing_plant_name") or "").strip()
            norm = row.get("norm")
            qty = row.get("qty", 0.0) or 0.0

            if not utility or not material or utility == "Total" or material == "Total":
                continue

            # Track all generation utilities (Utilities + Raw Material accounts)
            if account in ("Utilities", "Raw Material"):
                self._generation_utilities.add(utility)

                # Track U4U materials (only for Utilities account)
                if account == "Utilities":
                    self._u4u_materials.add(material)

            # Build consumption matrix entry
            if utility not in self._consumption_matrix:
                self._consumption_matrix[utility] = {
                    "plant_name": plant,
                    "consumptions": [],
                }

            self._consumption_matrix[utility]["consumptions"].append({
                "account": account,
                "material": material,
                "norm": float(norm) if norm is not None else 0.0,
                "qty": float(qty),
                "issuing_plant": issuing_plant,
                "plant_name": plant,
            })

            # Flag reverse-calc entries (NATURAL GAS norms that will be overwritten)
            if account == "Raw Material" and material.upper() in REVERSE_CALC_MATERIALS:
                self._reverse_calc_entries.append({
                    "utility": utility,
                    "material": material,
                    "plant_name": plant,
                })

            # Extract powergen norms (auxiliary power per asset)
            if utility == "POWERGEN" and material == "Power_Dis" and account == "Utilities":
                self._powergen_norms[plant.upper()] = float(norm) if norm is not None else 0.0

            # Extract STG SHP steam consumption norm
            if utility == "POWERGEN" and material == "SHP Steam_Dis" and account == "Utilities":
                if NMD_STG_PLANT.upper() in plant.upper():
                    self._stg_steam_norm = float(norm) if norm is not None else 0.0

            # Extract steam letdown (PRDS) norms
            if utility == "LP Steam PRDS" and material == "MP Steam_Dis":
                self._steam_letdown_norms["LP_to_MP"] = float(norm) if norm is not None else 0.0
            elif utility == "MP Steam PRDS SHP" and material == "HP Steam_Dis":
                self._steam_letdown_norms["MP_to_HP"] = float(norm) if norm is not None else 0.0
            elif utility == "HP Steam PRDS" and material == "SHP Steam_Dis":
                self._steam_letdown_norms["HP_to_SHP"] = float(norm) if norm is not None else 0.0

            # Extract HRSG byproduct norms (LP steam credit)
            if "HRSG" in utility.upper() and "LP STEAM" in material.upper():
                self._hrsg_byproduct_norms[utility.upper()] = float(norm) if norm is not None else 0.0

    # ------------------------------------------------------------------
    # Public API — consumption matrix
    # ------------------------------------------------------------------

    def get_consumption_norms(self) -> Dict[str, Dict]:
        """
        Return the full consumption norms matrix.

        Structure::

            {
                "POWERGEN": {
                    "plant_name": "NMD - Power Plant 1",
                    "consumptions": [
                        {"account": "Utilities", "material": "Power_Dis",
                         "norm": 0.00135, "qty": 0, ...},
                        ...
                    ],
                },
                "HRSG2_SHP STEAM": { ... },
                "Boiler Feed Water": { ... },
                ...
            }
        """
        return self._consumption_matrix

    def get_consumption_norms_flat(self) -> Dict[str, Dict[str, float]]:
        """
        Return a flat version: ``{utility: {material: norm}}``.

        Only includes Utilities-account entries (U4U norms).
        """
        flat: Dict[str, Dict[str, float]] = {}
        for utility, info in self._consumption_matrix.items():
            for c in info["consumptions"]:
                if c["account"] != "Utilities":
                    continue
                if utility not in flat:
                    flat[utility] = {}
                flat[utility][c["material"]] = c["norm"]
        return flat

    # ------------------------------------------------------------------
    # Public API — generation utilities
    # ------------------------------------------------------------------

    def get_generation_utilities(self) -> Set[str]:
        """Return the set of all generation utility names."""
        return self._generation_utilities

    def get_u4u_materials(self) -> Set[str]:
        """Return the set of all U4U material names (Utilities account only)."""
        return self._u4u_materials

    def is_dispatchable(self, utility_name: str) -> bool:
        """True if the utility is dispatchable (power dispatch)."""
        return utility_name.upper() == DISPATCHABLE_UTILITY

    def is_generation_utility(self, utility_name: str) -> bool:
        """True if the utility name appears as a generation utility in norms."""
        return utility_name in self._generation_utilities

    # ------------------------------------------------------------------
    # Public API — specialised norm getters
    # ------------------------------------------------------------------

    def get_powergen_norms(self) -> Dict[str, float]:
        """
        Return auxiliary power norms per power-generation asset.

        Returns::
            {"NMD - POWER PLANT 1": 0.00135, "NMD - STG POWER PLANT": 0.00705, ...}
        """
        return self._powergen_norms

    def get_steam_letdown_norms(self) -> Dict[str, float]:
        """
        Return PRDS letdown norms for the steam cascade.

        Returns::
            {"LP_to_MP": 0.945, "MP_to_HP": 0.900, "HP_to_SHP": 0.936}
        """
        return self._steam_letdown_norms

    def get_hrsg_byproduct_norms(self) -> Dict[str, float]:
        """
        Return LP steam byproduct norms for HRSG assets.

        Returns::
            {"HRSG1_SHP STEAM": -0.15, "HRSG2_SHP STEAM": -0.15, ...}
        """
        return self._hrsg_byproduct_norms

    def get_stg_steam_norm(self) -> float:
        """Return STG SHP steam consumption norm (MT/KWH)."""
        return self._stg_steam_norm

    def get_reverse_calc_entries(self) -> List[Dict]:
        """
        Return list of norms that need reverse calculation during iteration.

        Each entry: ``{"utility": "HRSG2_SHP STEAM", "material": "NATURAL GAS", ...}``
        """
        return self._reverse_calc_entries

    # ------------------------------------------------------------------
    # Public API — norm lookup helpers
    # ------------------------------------------------------------------

    def get_norm(
        self,
        utility_name: str,
        material_name: str,
        account_name: str = "Utilities",
        plant_name: Optional[str] = None,
        issuing_plant_name: Optional[str] = None,
    ) -> float:
        """
        Look up a single norm value from the in-memory row cache.

        Returns 0.0 if not found (no exception raised).
        """
        for row in self._rows:
            if _clean(row["utility_name"]) != _clean(utility_name):
                continue
            if _clean(row["material_name"]) != _clean(material_name):
                continue
            if _clean(row["account_name"]) != _clean(account_name):
                continue
            if plant_name is not None and _clean(row["plant_name"]) != _clean(plant_name):
                continue
            if issuing_plant_name is not None and _clean(row["issuing_plant_name"]) != _clean(issuing_plant_name):
                continue
            norm = row["norm"]
            return float(norm) if norm is not None else 0.0
        return 0.0

    def get_all_consumptions_for_utility(
        self,
        utility_name: str,
        account_name: Optional[str] = "Utilities",
    ) -> List[Dict]:
        """
        Return all consumption entries for a given generation utility.

        Each entry: ``{"account": ..., "material": ..., "norm": ..., "qty": ..., ...}``
        """
        result = []
        info = self._consumption_matrix.get(utility_name)
        if info is None:
            return result
        for c in info["consumptions"]:
            if account_name is not None and c["account"] != account_name:
                continue
            result.append(c)
        return result

    def get_all_consumptions_for_material(
        self,
        material_name: str,
        account_name: Optional[str] = "Utilities",
    ) -> List[Dict]:
        """
        Return all consumption entries where the *material* matches.

        This is the reverse lookup: given a material (e.g. 'Power_Dis'),
        find every utility that consumes it.
        """
        result = []
        for utility, info in self._consumption_matrix.items():
            for c in info["consumptions"]:
                if _clean(c["material"]) != _clean(material_name):
                    continue
                if account_name is not None and c["account"] != account_name:
                    continue
                result.append({
                    "utility": utility,
                    **c,
                })
        return result

    # ------------------------------------------------------------------
    # Public API — BPC generation quantities (from QTY column)
    # ------------------------------------------------------------------

    def get_bpc_generation_quantities(self) -> Dict[str, float]:
        """
        Return BPC generation quantities from the QTY column of
        NormsMonthDetail for each generation utility.

        Returns::
            {"POWERGEN": 110880.0, "Boiler Feed Water": 69965.28, ...}
        """
        gen_qty: Dict[str, float] = {}
        for row in self._rows:
            utility = (row.get("utility_name") or "").strip()
            account = (row.get("account_name") or "").strip()
            material = (row.get("material_name") or "").strip()
            qty = row.get("qty", 0.0) or 0.0

            # BPC Gen Qty is stored in rows where the utility produces itself
            # i.e., UtilityName == MaterialName (self-consumption / generation)
            if account in ("Utilities", "Raw Material") and utility and utility != "Total":
                if utility not in gen_qty or qty > (gen_qty.get(utility, 0) or 0):
                    # Take the maximum QTY found for this utility
                    if qty > 0:
                        gen_qty[utility] = float(qty)
        return gen_qty

    def get_bpc_quantities(self) -> Dict[str, float]:
        """
        Return BPC consumption quantities (QTY column) for each
        utility/material pair.

        Returns::
            {"POWERGEN::Power_Dis": 40180.14, "Boiler Feed Water::Power_Dis": 12345.6, ...}
        """
        bpc_qty: Dict[str, float] = {}
        for row in self._rows:
            utility = (row.get("utility_name") or "").strip()
            material = (row.get("material_name") or "").strip()
            account = (row.get("account_name") or "").strip()
            qty = row.get("qty", 0.0) or 0.0

            if account in ("Utilities", "Raw Material") and utility and material:
                key = f"{utility}::{material}"
                bpc_qty[key] = float(qty)
        return bpc_qty

    # ------------------------------------------------------------------
    # Diagnostics
    # ------------------------------------------------------------------

    def print_summary(self):
        """Print a summary of discovered norms for debugging."""
        print(f"\n  [NMD NormsReader Summary] Month: {self.month}/{self.year}")
        print(f"  Total rows loaded: {len(self._rows)}")
        print(f"  Generation utilities: {len(self._generation_utilities)}")
        print(f"  U4U materials: {len(self._u4u_materials)}")
        print(f"  Powergen norms: {len(self._powergen_norms)}")
        print(f"  Steam letdown norms: {len(self._steam_letdown_norms)}")
        print(f"  HRSG byproduct norms: {len(self._hrsg_byproduct_norms)}")
        print(f"  Reverse-calc entries: {len(self._reverse_calc_entries)}")
        print(f"  STG SHP norm: {self._stg_steam_norm:.7f} MT/KWH")

        print(f"\n  Generation Utilities:")
        for u in sorted(self._generation_utilities):
            consumptions = self._consumption_matrix.get(u, {}).get("consumptions", [])
            u4u_count = sum(1 for c in consumptions if c["account"] == "Utilities")
            rm_count = sum(1 for c in consumptions if c["account"] == "Raw Material")
            dispatchable = " [DISPATCHABLE]" if self.is_dispatchable(u) else ""
            print(f"    {u}{dispatchable} — {u4u_count} U4U, {rm_count} raw material")

        if self._reverse_calc_entries:
            print(f"\n  Reverse-calculated norms:")
            for entry in self._reverse_calc_entries:
                print(f"    {entry['utility']} → {entry['material']} (plant: {entry['plant_name']})")
