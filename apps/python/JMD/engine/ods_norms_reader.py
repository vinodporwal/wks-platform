"""
Unified ODS Norms Reader for JMD Plants
=======================================
Reads the plant-specific JMD_*.ods file ONCE and provides all norms data
through a single, cached interface.

Currently provides:
  1. Powergen norms       — auxiliary power consumption per GT/STG asset
  2. Steam letdown norms  — PRDS cascade factors (LP→MP, MP→HP, HP→SHP)
  3. HRSG byproduct norms — LP steam credit per MT of SHP generated
  4. U4U norms matrix     — Utility-for-Utility dependency matrix
  5. Steam distribution   — how each steam grade is sourced (HRSG, PRDS, etc.)
  6. STG steam consumption— SHP steam consumed by STG per KWH
  7. Raw material norms   — fuel (SynGas) consumption per asset

Architecture:
  - ODSNormsReader is instantiated once per (plant_id, month, year).
  - The ODS file is loaded into a pandas DataFrame exactly once.
  - All norm extraction methods operate on the in-memory DataFrame.
  - A module-level cache (_READER_CACHE) avoids re-reading the same file
    when multiple components need norms for the same plant/month/year.
  - When the data source moves to DB tables in the future, only this
    class needs to be updated — all consumers continue to call the same
    methods.

ODS File Layout (C2_JMD.ods):
  Row 0: Year (e.g., 2026)
  Row 1: Month-Year float (e.g., 2026.04 for April 2026)
  Row 2: Month-Year float (duplicate)
  Row 3: Column headers
  Row 4+: Data rows

  Columns 0-10: Metadata (Utility Plant, Utility, Account, Material, etc.)
  Columns 11+: Month data in groups of 6 (Norms, Quantity, Amount, Price, Gen Qty, Rate Per Unit)

  Each month occupies 6 consecutive columns starting from col 11.
  April = cols 11-16, May = cols 17-22, ..., March = cols 77-82.
"""

import os
import logging
import pandas as pd
from typing import Optional

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Column index constants (0-based, from the ODS header row at index 3)
# ---------------------------------------------------------------------------
_COL_UTILITY_PLANT   = 0
_COL_UTILITY_PLANT_ID = 1
_COL_UTILITY         = 2
_COL_UTILITY_ID      = 3
_COL_UTILITY_UOM     = 4
_COL_ACCOUNT         = 5
_COL_MATERIAL        = 6
_COL_MATERIAL_ID     = 7
_COL_MATERIAL_UOM    = 8
_COL_ISSUING_PLANT   = 9
_COL_ISSUING_PLANT_ID = 10
_COL_NORMS           = 11  # First norms column (April)
_COLS_PER_MONTH      = 6   # Each month has 6 sub-columns

# Sub-column offsets within a month group
_OFFSET_NORMS        = 0
_OFFSET_QUANTITY     = 1
_OFFSET_AMOUNT       = 2
_OFFSET_PRICE        = 3
_OFFSET_GEN_QTY      = 4
_OFFSET_RATE         = 5

# Data starts at row index 4 (after 3 header rows)
_DATA_START_ROW      = 4


def _resolve_excel_path(plant_id: str) -> str:
    """Resolve the ODS file path for a given plant."""
    try:
        from plant_mapper import get_plant
        plant_info = get_plant(plant_id)
        short_code = plant_info.get("short_code", "c2").upper()
    except Exception:
        short_code = "C2"

    filenames = [f"{short_code}_JMD.ods", f"JMD_{short_code}.ods"]
    for filename in filenames:
        for path in [filename, f"../{filename}", f"engine/{filename}"]:
            if os.path.exists(path):
                return os.path.abspath(path)
        base_dir = os.path.dirname(os.path.abspath(__file__))
        for _ in range(3):
            p = os.path.join(base_dir, filename)
            if os.path.exists(p):
                return p
            base_dir = os.path.dirname(base_dir)
    return f"{short_code}_JMD.ods"


# ---------------------------------------------------------------------------
# Reader cache — avoids re-instantiating for the same plant/month/year
# ---------------------------------------------------------------------------
_READER_CACHE: dict = {}


class ODSNormsReader:
    """
    Unified reader for JMD plant ODS norms file.

    Loads the ODS file once and exposes typed methods for each norm category.
    Use get_reader() class method to get a cached instance.
    """

    def __init__(self, plant_id: str, month: int, year: int):
        self.plant_id = plant_id
        self.month = month
        self.year = year
        self.filepath = _resolve_excel_path(plant_id)
        self._df: Optional[pd.DataFrame] = None
        self._month_col_idx: Optional[int] = None
        self._loaded = False

    # ------------------------------------------------------------------
    # Public class method for cached access
    # ------------------------------------------------------------------

    @classmethod
    def get_reader(cls, plant_id: str, month: int, year: int) -> "ODSNormsReader":
        """Get a cached ODSNormsReader instance for the given plant/month/year."""
        cache_key = (plant_id.upper(), month, year)
        if cache_key not in _READER_CACHE:
            reader = cls(plant_id, month, year)
            reader._load()
            _READER_CACHE[cache_key] = reader
        return _READER_CACHE[cache_key]

    @classmethod
    def clear_cache(cls):
        """Clear the reader cache (useful for testing or long-running processes)."""
        _READER_CACHE.clear()

    # ------------------------------------------------------------------
    # Internal loading
    # ------------------------------------------------------------------

    def _load(self):
        """Load the ODS file and locate the month column."""
        if self._loaded:
            return

        if not os.path.exists(self.filepath):
            logger.warning("  [ODS] File not found: %s", self.filepath)
            self._loaded = True
            return

        try:
            self._df = pd.read_excel(self.filepath, engine="odf", header=None)
            self._month_col_idx = self._find_month_column()
            self._loaded = True

            if self._month_col_idx is not None:
                logger.info(
                    "  [ODS] Loaded %s (%d rows, %d cols) — month %d/%d at col %d",
                    os.path.basename(self.filepath),
                    len(self._df), len(self._df.columns),
                    self.month, self.year, self._month_col_idx,
                )
            else:
                logger.warning(
                    "  [ODS] Month %d/%d not found in %s",
                    self.month, self.year, os.path.basename(self.filepath),
                )
        except Exception as e:
            logger.error("  [ODS] Error loading %s: %s", self.filepath, e)
            self._loaded = True

    def _find_month_column(self) -> Optional[int]:
        """Find the column index for the target month/year."""
        if self._df is None:
            return None

        target_val = float(self.year) + float(self.month) / 100.0
        row_1 = self._df.iloc[1].tolist()

        for col_idx, val in enumerate(row_1):
            try:
                if pd.notna(val) and abs(float(val) - target_val) < 1e-5:
                    return col_idx
            except (ValueError, TypeError):
                continue

        return None

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _iter_data_rows(self):
        """Iterate over data rows (row 4 onwards), yielding (row_idx, row_list)."""
        if self._df is None:
            return
        for idx in range(_DATA_START_ROW, len(self._df)):
            yield idx, self._df.iloc[idx].tolist()

    def _get_norm_val(self, row_list: list) -> float:
        """Extract the norm value for the target month from a row."""
        if self._month_col_idx is None or len(row_list) <= self._month_col_idx:
            return 0.0
        val = row_list[self._month_col_idx]
        try:
            return float(val) if pd.notna(val) else 0.0
        except (ValueError, TypeError):
            return 0.0

    def _get_quantity_val(self, row_list: list) -> float:
        """Extract the quantity value for the target month from a row."""
        if self._month_col_idx is None:
            return 0.0
        qty_col = self._month_col_idx + _OFFSET_QUANTITY
        if len(row_list) <= qty_col:
            return 0.0
        val = row_list[qty_col]
        try:
            return float(val) if pd.notna(val) else 0.0
        except (ValueError, TypeError):
            return 0.0

    @property
    def is_available(self) -> bool:
        """True if the ODS file was loaded and the target month was found."""
        return self._df is not None and self._month_col_idx is not None

    # ------------------------------------------------------------------
    # 1. Powergen Norms — auxiliary power consumption per asset
    # ------------------------------------------------------------------

    def get_powergen_norms(self) -> dict:
        """
        Return auxiliary power norms for each power generation asset.

        Looks for rows where:
          Utility == "POWERGEN" and Material == "Power_Dis"

        Returns:
            {"JMD - C2-GTG 1": 0.00135, "JMD - C2-GTG 2": 0.00135, "JMD - C2-STG 1": 0.00705}
        """
        if not self.is_available:
            return {}

        norms_map = {}
        for _, row in self._iter_data_rows():
            utility = str(row[_COL_UTILITY]).strip()
            material = str(row[_COL_MATERIAL]).strip()

            if utility == "POWERGEN" and material == "Power_Dis":
                plant_name = str(row[_COL_UTILITY_PLANT]).strip().upper()
                norms_map[plant_name] = self._get_norm_val(row)

        return norms_map

    # ------------------------------------------------------------------
    # 2. Steam Letdown Norms — PRDS cascade factors
    # ------------------------------------------------------------------

    def get_steam_letdown_norms(self) -> dict:
        """
        Return PRDS letdown norms for the steam cascade.

        The cascade flows from high pressure to low pressure:
          SHP → HP (via HP Steam PRDS)    norm = 0.936 (1 MT HP needs 0.936 MT SHP)
          HP  → MP (via MP Steam PRDS SHP) norm = 0.900 (1 MT MP needs 0.900 MT HP)
          MP  → LP (via LP Steam PRDS)    norm = 0.945 (1 MT LP needs 0.945 MT MP)

        Returns:
            {"LP_to_MP": 0.945, "MP_to_HP": 0.900, "HP_to_SHP": 0.936}
        """
        if not self.is_available:
            return {}

        norms = {}
        for _, row in self._iter_data_rows():
            utility = str(row[_COL_UTILITY]).strip()
            material = str(row[_COL_MATERIAL]).strip()

            if utility == "LP Steam PRDS" and material == "MP Steam_Dis":
                norms["LP_to_MP"] = self._get_norm_val(row)
            elif utility == "MP Steam PRDS SHP" and material == "HP Steam_Dis":
                norms["MP_to_HP"] = self._get_norm_val(row)
            elif utility == "HP Steam PRDS" and material == "SHP Steam_Dis":
                norms["HP_to_SHP"] = self._get_norm_val(row)

        return norms

    # ------------------------------------------------------------------
    # 3. HRSG Byproduct Norms — LP steam credit from HRSG SHP generation
    # ------------------------------------------------------------------

    def get_hrsg_byproduct_norms(self) -> dict:
        """
        Return LP steam byproduct norms for HRSG assets.

        When an HRSG generates SHP steam, it also produces LP steam as a byproduct.
        The norm is negative (credit), e.g., -0.15 means 0.15 MT LP produced per 1 MT SHP.

        Returns:
            {"HRSG1_C2_SHP STEAM": -0.15, "HRSG2_C2_SHP STEAM": -0.15}
        """
        if not self.is_available:
            return {}

        byproducts = {}
        for _, row in self._iter_data_rows():
            utility = str(row[_COL_UTILITY]).strip()
            material = str(row[_COL_MATERIAL]).strip()

            if "HRSG" in utility.upper() and "LP STEAM" in material.upper():
                byproducts[utility.upper()] = self._get_norm_val(row)

        return byproducts

    # ------------------------------------------------------------------
    # 4. U4U Norms Matrix — Utility-for-Utility dependency matrix
    # ------------------------------------------------------------------

    def get_u4u_norms_matrix(self) -> dict:
        """
        Return the U4U dependency matrix for utility-for-utility calculations.

        Filters rows where Account == 'Utilities' and builds:
            {consumer: {producer: {"norm": float, "qty": float}}}

        This replaces the standalone fetch_u4u_norms_from_excel() function.
        """
        if not self.is_available:
            return {}

        norms_matrix = {}
        for _, row in self._iter_data_rows():
            account = str(row[_COL_ACCOUNT]).strip()

            if account != "Utilities":
                continue

            consumer_name = str(row[_COL_UTILITY]).strip()
            consumer_plant_id = str(row[_COL_UTILITY_PLANT_ID]).strip() if pd.notna(row[_COL_UTILITY_PLANT_ID]) else ""
            consumer = f"{consumer_name}::{consumer_plant_id}" if consumer_plant_id else consumer_name

            producer_name = str(row[_COL_MATERIAL]).strip()
            producer_plant_id = str(row[_COL_ISSUING_PLANT_ID]).strip() if pd.notna(row[_COL_ISSUING_PLANT_ID]) else ""
            producer = f"{producer_name}::{producer_plant_id}" if producer_plant_id else producer_name

            if not consumer_name or not producer_name or consumer_name == "nan" or producer_name == "nan":
                continue

            norm_val = self._get_norm_val(row)
            qty_val = self._get_quantity_val(row)

            if consumer not in norms_matrix:
                norms_matrix[consumer] = {}
            norms_matrix[consumer][producer] = {"norm": norm_val, "qty": qty_val}

        return norms_matrix

    # ------------------------------------------------------------------
    # 5. Steam Distribution Factors — how each steam grade is sourced
    # ------------------------------------------------------------------

    def get_steam_distribution(self) -> dict:
        """
        Return distribution factors showing how each steam grade is sourced.

        For example, LP Steam_Dis is sourced from:
          - HRSG1_C2_LP STEAM (norm 0.387271)
          - HRSG2_C2_LP STEAM (norm 0.245271)
          - LP Steam PRDS     (norm 0.367458)

        SHP Steam_Dis is sourced from:
          - AUXBOIL7_SHP STEAM (norm 0.226682)
          - HRSG1_C2_SHP STEAM (norm 0.473460)
          - HRSG2_C2_SHP STEAM (norm 0.299858)

        Returns:
            {"LP Steam_Dis": {"HRSG1_C2_LP STEAM": 0.387, ...}, ...}
        """
        if not self.is_available:
            return {}

        distribution = {}
        steam_grades = ["SHP Steam_Dis", "HP Steam_Dis", "MP Steam_Dis", "LP Steam_Dis"]

        for _, row in self._iter_data_rows():
            utility = str(row[_COL_UTILITY]).strip()
            material = str(row[_COL_MATERIAL]).strip()
            account = str(row[_COL_ACCOUNT]).strip()

            if utility in steam_grades and account == "Utilities":
                norm_val = self._get_norm_val(row)
                if norm_val > 0 or pd.notna(row[self._month_col_idx]):
                    if utility not in distribution:
                        distribution[utility] = {}
                    distribution[utility][material] = norm_val

        return distribution

    # ------------------------------------------------------------------
    # 6. STG Steam Consumption — SHP steam consumed by STG per KWH
    # ------------------------------------------------------------------

    def get_stg_steam_consumption_norms(self) -> dict:
        """
        Return STG steam consumption norms.

        STG consumes SHP steam to generate power. The norm is in MT of SHP per KWH.

        Looks for rows where:
          Utility == "POWERGEN", Material == "SHP Steam_Dis"
          and Utility Plant contains "STG"

        Returns:
            {"JMD - C2-STG 1": 0.00364}
        """
        if not self.is_available:
            return {}

        stg_norms = {}
        for _, row in self._iter_data_rows():
            utility = str(row[_COL_UTILITY]).strip()
            material = str(row[_COL_MATERIAL]).strip()
            plant_name = str(row[_COL_UTILITY_PLANT]).strip()

            if utility == "POWERGEN" and material == "SHP Steam_Dis":
                if "STG" in plant_name.upper():
                    stg_norms[plant_name.upper()] = self._get_norm_val(row)

        return stg_norms

    # ------------------------------------------------------------------
    # 7. Raw Material Norms — fuel consumption per asset
    # ------------------------------------------------------------------

    def get_raw_material_norms(self) -> dict:
        """
        Return raw material (fuel) consumption norms for each asset.

        Looks for rows where Account == 'Raw Material'.
        Typically returns SynGas consumption per MT of steam or per KWH of power.

        Returns:
            {"AUXBOIL7_SHP STEAM": 2.876221, "HRSG1_C2_SHP STEAM": 3.376244, ...}
        """
        if not self.is_available:
            return {}

        raw_norms = {}
        for _, row in self._iter_data_rows():
            utility = str(row[_COL_UTILITY]).strip()
            material = str(row[_COL_MATERIAL]).strip()
            account = str(row[_COL_ACCOUNT]).strip()

            if account == "Raw Material" and material == "SynGas(Unshift)":
                raw_norms[utility] = self._get_norm_val(row)

        return raw_norms

    # ------------------------------------------------------------------
    # 8. PRDS BFW Consumption — Boiler Feed Water consumed by PRDS
    # ------------------------------------------------------------------

    def get_prds_bfw_norms(self) -> dict:
        """
        Return Boiler Feed Water consumption norms for each PRDS station.

        PRDS stations consume BFW for desuperheating when reducing steam pressure.

        Returns:
            {"LP Steam PRDS": 0.055, "MP Steam PRDS SHP": 0.1, "HP Steam PRDS": 0.064}
        """
        if not self.is_available:
            return {}

        bfw_norms = {}
        for _, row in self._iter_data_rows():
            utility = str(row[_COL_UTILITY]).strip()
            material = str(row[_COL_MATERIAL]).strip()
            account = str(row[_COL_ACCOUNT]).strip()

            if account == "Utilities" and material == "Boiler Feed Water" and "PRDS" in utility.upper():
                bfw_norms[utility] = self._get_norm_val(row)

        return bfw_norms

    # ------------------------------------------------------------------
    # 9. Comprehensive Consumption Norms — all utilities consumed per UOM
    #    of generation for every producing asset/utility in the ODS
    # ------------------------------------------------------------------

    def get_consumption_norms(self) -> dict:
        """
        Return the full consumption norms matrix for every producing utility/asset.

        For each producer (e.g., AUXBOIL7_SHP STEAM, POWERGEN, Boiler Feed Water),
        this returns all consumed utilities/materials and their norm per UOM of
        production.

        Includes both 'Utilities' and 'Raw Material' account rows.

        Structure:
            {
                "AUXBOIL7_SHP STEAM": {
                    "producer_uom": "MT",
                    "consumptions": [
                        {"account": "Raw Material", "material": "SynGas(Unshift)",
                         "norm": 2.876221, "material_uom": "MMBTU"},
                        {"account": "Utilities", "material": "Boiler Feed Water",
                         "norm": 1.01, "material_uom": "M3"},
                        {"account": "Utilities", "material": "Power_Dis",
                         "norm": 3.85, "material_uom": "KWH"},
                    ]
                },
                "HRSG1_C2_SHP STEAM": { ... },
                "POWERGEN": { ... },
                ...
            }

        This is the single-point reference for all utility consumption norms.
        When migrating to DB tables, replace the internal implementation of this
        method — all consumers continue to call the same interface.
        """
        if not self.is_available:
            return {}

        norms_map = {}
        for _, row in self._iter_data_rows():
            utility = str(row[_COL_UTILITY]).strip()
            account = str(row[_COL_ACCOUNT]).strip()
            material = str(row[_COL_MATERIAL]).strip()

            if account not in ("Utilities", "Raw Material"):
                continue
            if not utility or utility == "nan" or not material or material == "nan":
                continue
            if utility == "Total" or material == "Total":
                continue

            utility_id = str(row[_COL_UTILITY_ID]).strip() if pd.notna(row[_COL_UTILITY_ID]) else ""
            utility_plant = str(row[_COL_UTILITY_PLANT]).strip() if pd.notna(row[_COL_UTILITY_PLANT]) else ""
            utility_plant_id = str(row[_COL_UTILITY_PLANT_ID]).strip() if pd.notna(row[_COL_UTILITY_PLANT_ID]) else ""
            material_id = str(row[_COL_MATERIAL_ID]).strip() if pd.notna(row[_COL_MATERIAL_ID]) else ""
            issuing_plant_id = str(row[_COL_ISSUING_PLANT_ID]).strip() if pd.notna(row[_COL_ISSUING_PLANT_ID]) else ""
            producer_uom = str(row[_COL_UTILITY_UOM]).strip() if pd.notna(row[_COL_UTILITY_UOM]) else ""
            material_uom = str(row[_COL_MATERIAL_UOM]).strip() if pd.notna(row[_COL_MATERIAL_UOM]) else ""
            norm_val = self._get_norm_val(row)

            # Use composite key when multiple plants produce the same utility
            # (e.g., POWERGEN appears 3 times: GTG1 at 36H0, GTG2 at 36H0, STG at 36H2)
            if utility not in norms_map:
                norms_map[utility] = {
                    "producer_uom": producer_uom,
                    "utility_id": utility_id,
                    "utility_plant_id": utility_plant_id,
                    "sub_assets": [],
                    "consumptions": [],
                }

            # Track sub-asset info for multi-asset producers like POWERGEN
            sub_key = f"{utility_plant}::{utility_plant_id}"
            if sub_key not in [s["key"] for s in norms_map[utility]["sub_assets"]]:
                norms_map[utility]["sub_assets"].append({
                    "key": sub_key,
                    "plant_name": utility_plant,
                    "plant_id": utility_plant_id,
                })

            norms_map[utility]["consumptions"].append({
                "account": account,
                "material": material,
                "material_id": material_id,
                "issuing_plant_id": issuing_plant_id,
                "norm": norm_val,
                "material_uom": material_uom,
                "source_plant": utility_plant,
                "source_plant_id": utility_plant_id,
            })

        return norms_map

    def get_consumption_norms_flat(self) -> dict:
        """
        Return a flat version of consumption norms for quick lookup.

        Structure:
            {
                "AUXBOIL7_SHP STEAM": {
                    "SynGas(Unshift)": 2.876221,
                    "Boiler Feed Water": 1.01,
                    "Power_Dis": 3.85,
                },
                "HRSG1_C2_SHP STEAM": {
                    "SynGas(Unshift)": 3.376244,
                    "Boiler Feed Water": 1.16,
                    "HRSG1_C2_LP STEAM": -0.15,
                    "Power_Dis": 0.285,
                },
                ...
            }

        This is the most convenient format for code that just needs
        `norm = norms[producer][consumer]`.
        """
        full = self.get_consumption_norms()
        flat = {}
        for producer, info in full.items():
            flat[producer] = {}
            for c in info["consumptions"]:
                flat[producer][c["material"]] = c["norm"]
        return flat

    def get_bpc_generation_quantities(self) -> dict:
        """
        Return BPC generation quantities per producer from the ODS file.

        For each producer (utility), picks any material row with non-zero norm
        and non-zero quantity, then BPC_gen_qty = quantity / norm.

        For multi-asset producers like POWERGEN, keys by source_plant (asset name).
        For single-asset producers, keys by utility name.

        Returns:
            {"JMD - C2-GTG 1": 54100000.0, "AUXBOIL7_SHP STEAM": 62820.0, ...}
        """
        if not self.is_available:
            return {}

        result: dict = {}

        for _, row in self._iter_data_rows():
            utility = str(row[_COL_UTILITY]).strip()
            account = str(row[_COL_ACCOUNT]).strip()
            material = str(row[_COL_MATERIAL]).strip()

            if account not in ("Utilities", "Raw Material"):
                continue
            if not utility or utility == "nan" or not material or material == "nan":
                continue
            if utility == "Total" or material == "Total":
                continue

            utility_plant = str(row[_COL_UTILITY_PLANT]).strip() if pd.notna(row[_COL_UTILITY_PLANT]) else ""
            norm_val = self._get_norm_val(row)
            qty_val = self._get_quantity_val(row)

            if norm_val == 0 or qty_val == 0:
                continue

            gen_qty = qty_val / norm_val

            if utility == "POWERGEN":
                key = utility_plant
            else:
                key = utility

            if key and key not in result:
                result[key] = gen_qty

        return result

    def get_bpc_quantities(self) -> dict:
        """
        Return BPC quantities per producer+material from the ODS file.

        For multi-asset producers like POWERGEN, keys by source_plant (asset name).
        For single-asset producers, keys by utility name.

        Returns:
            {"JMD - C2-GTG 1": {"SynGas(Unshift)": 328541.30, "Power_Dis": 101533.57, ...},
             "AUXBOIL7_SHP STEAM": {"SynGas(Unshift)": 247872.88, "Boiler Feed Water": 87041.85, ...}}
        """
        if not self.is_available:
            return {}

        result: dict = {}

        for _, row in self._iter_data_rows():
            utility = str(row[_COL_UTILITY]).strip()
            account = str(row[_COL_ACCOUNT]).strip()
            material = str(row[_COL_MATERIAL]).strip()

            if account not in ("Utilities", "Raw Material"):
                continue
            if not utility or utility == "nan" or not material or material == "nan":
                continue
            if utility == "Total" or material == "Total":
                continue

            utility_plant = str(row[_COL_UTILITY_PLANT]).strip() if pd.notna(row[_COL_UTILITY_PLANT]) else ""
            qty_val = self._get_quantity_val(row)

            if utility == "POWERGEN":
                key = utility_plant
            else:
                key = utility

            if key:
                if key not in result:
                    result[key] = {}
                result[key][material] = qty_val

        return result

    def get_asset_consumption(self, asset_name: str, consumed_utility: str = None) -> float:
        """
        Quick lookup: get the consumption norm of a specific utility by a specific asset.

        Args:
            asset_name:        Name of the producing asset (e.g., "HRSG1_C2_SHP STEAM")
            consumed_utility:  Name of the consumed utility (e.g., "Power_Dis")
                              If None, returns all consumptions for the asset.

        Returns:
            float norm value, or 0.0 if not found.
            If consumed_utility is None, returns dict of all consumptions.
        """
        flat = self.get_consumption_norms_flat()
        asset_key = asset_name.strip()

        # Try exact match first, then case-insensitive
        if asset_key not in flat:
            for k in flat:
                if k.upper() == asset_key.upper():
                    asset_key = k
                    break

        if asset_key not in flat:
            return 0.0 if consumed_utility else {}

        if consumed_utility is None:
            return flat[asset_key]

        return flat[asset_key].get(consumed_utility, 0.0) if consumed_utility in flat[asset_key] else \
            next((v for k, v in flat[asset_key].items()
                  if k.upper() == consumed_utility.upper()), 0.0)

    # ------------------------------------------------------------------
    # Convenience: get all norms in one call
    # ------------------------------------------------------------------

    def get_all_norms(self) -> dict:
        """
        Return all available norms in a single dictionary.

        Useful for logging or passing to downstream modules.
        """
        return {
            "powergen": self.get_powergen_norms(),
            "steam_letdown": self.get_steam_letdown_norms(),
            "hrsg_byproduct": self.get_hrsg_byproduct_norms(),
            "u4u_matrix": self.get_u4u_norms_matrix(),
            "steam_distribution": self.get_steam_distribution(),
            "stg_steam_consumption": self.get_stg_steam_consumption_norms(),
            "raw_material": self.get_raw_material_norms(),
            "prds_bfw": self.get_prds_bfw_norms(),
            "consumption_norms": self.get_consumption_norms(),
            "consumption_norms_flat": self.get_consumption_norms_flat(),
        }

    # ------------------------------------------------------------------
    # Logging helper
    # ------------------------------------------------------------------

    def log_all_norms(self):
        """Log all extracted norms for debugging."""
        if not self.is_available:
            logger.warning("  [ODS] No norms available — file not loaded or month not found")
            return

        letdown = self.get_steam_letdown_norms()
        byproduct = self.get_hrsg_byproduct_norms()
        powergen = self.get_powergen_norms()
        stg_steam = self.get_stg_steam_consumption_norms()
        distribution = self.get_steam_distribution()

        logger.info("  [ODS] ===== NORMS SUMMARY (%s %d/%d) =====",
                     os.path.basename(self.filepath), self.month, self.year)

        logger.info("  [ODS] Powergen aux power norms:")
        for asset, norm in powergen.items():
            logger.info("  [ODS]   %-25s  aux_norm=%.6f", asset, norm)

        logger.info("  [ODS] Steam letdown norms (PRDS cascade):")
        logger.info("  [ODS]   LP→MP: %.4f  (1 MT LP needs this much MP)", letdown.get("LP_to_MP", 0))
        logger.info("  [ODS]   MP→HP: %.4f  (1 MT MP needs this much HP)", letdown.get("MP_to_HP", 0))
        logger.info("  [ODS]   HP→SHP: %.4f (1 MT HP needs this much SHP)", letdown.get("HP_to_SHP", 0))

        logger.info("  [ODS] HRSG byproduct (LP steam credit):")
        for asset, norm in byproduct.items():
            logger.info("  [ODS]   %-25s  byproduct_norm=%.4f MT LP / MT SHP", asset, norm)

        if stg_steam:
            logger.info("  [ODS] STG steam consumption:")
            for asset, norm in stg_steam.items():
                logger.info("  [ODS]   %-25s  shp_norm=%.6f MT/KWH", asset, norm)

        logger.info("  [ODS] Steam distribution factors:")
        for grade, sources in distribution.items():
            parts = [f"{k}: {v:.4f}" for k, v in sources.items()]
            logger.info("  [ODS]   %s → %s", grade, ", ".join(parts))

        # ── Detailed consumption norms matrix ──
        # Explained simply: "To make 1 unit of X, you need A amount of Y, B amount of Z, ..."
        consumption = self.get_consumption_norms()
        logger.info("")
        logger.info("  [ODS] ╔══════════════════════════════════════════════════════════════════╗")
        logger.info("  [ODS] ║  CONSUMPTION NORMS — Who consumes what to produce what          ║")
        logger.info("  [ODS] ║  (Each block = one producer. Shows what it needs to make 1 unit)║")
        logger.info("  [ODS] ╚══════════════════════════════════════════════════════════════════╝")
        logger.info("  [ODS]")
        logger.info("  [ODS]  Think of it like a recipe card:")
        logger.info("  [ODS]  - The PRODUCER is what the plant makes (like a cake)")
        logger.info("  [ODS]  - The INGREDIENTS are what it needs to make it (flour, eggs, etc.)")
        logger.info("  [ODS]  - The NORM is how much of each ingredient is needed for 1 unit of output")
        logger.info("  [ODS]  - A negative norm means it's a BYPRODUCT (you get it for free while making something else)")
        logger.info("  [ODS]")

        for producer, info in sorted(consumption.items()):
            uom = info["producer_uom"]
            sap_code = info.get("utility_id", "---")
            plant_code = info.get("utility_plant_id", "---")
            sub_assets = info.get("sub_assets", [])
            consumptions = info["consumptions"]

            logger.info("  [ODS]  ┌──────────────────────────────────────────────────────────────")
            logger.info("  [ODS]  │ PRODUCER: %-35s", producer)
            logger.info("  [ODS]  │ SAP Code: %-20s  Plant Code: %s", sap_code, plant_code)
            logger.info("  [ODS]  │ Makes: 1 %s of %s", uom, producer)

            # Show sub-assets if there are multiple (like POWERGEN having GTG1, GTG2, STG)
            if len(sub_assets) > 1:
                logger.info("  [ODS]  │")
                logger.info("  [ODS]  │  This producer has %d sub-assets (different machines):", len(sub_assets))
                for sa in sub_assets:
                    logger.info("  [ODS]  │    • %-30s  (Plant ID: %s)", sa["plant_name"], sa["plant_id"])

            logger.info("  [ODS]  │")
            logger.info("  [ODS]  │  Needs these ingredients (to make 1 %s):", uom)
            logger.info("  [ODS]  │")

            # If multiple sub-assets, group consumptions by source_plant
            if len(sub_assets) > 1:
                for sa in sub_assets:
                    sa_consumptions = [c for c in consumptions
                                       if c.get("source_plant", "") == sa["plant_name"]]
                    if not sa_consumptions:
                        continue
                    logger.info("  [ODS]  │  ▸ Sub-asset: %s (Plant ID: %s)", sa["plant_name"], sa["plant_id"])
                    logger.info("  [ODS]  │    %-6s  %-32s  %-15s  %-12s  %-10s",
                                 "Type", "Ingredient (Material)", "Material SAP Code", "Norm", "UOM")
                    logger.info("  [ODS]  │    %-6s  %-32s  %-15s  %-12s  %-10s",
                                 "------", "--------------------------------", "---------------", "------------", "----------")
                    for c in sa_consumptions:
                        norm_str = f"{c['norm']:.6f}" if c['norm'] != 0 else "0"
                        type_label = "Utility" if c["account"] == "Utilities" else "Raw Mat."
                        mat_id = c.get("material_id", "---") or "---"
                        logger.info("  [ODS]  │    %-6s  %-32s  %-15s  %12s  %-10s",
                                     type_label, c["material"], mat_id, norm_str, c["material_uom"])

                    # Simple explanation per sub-asset
                    consumables_sa = [c for c in sa_consumptions if c["norm"] > 0]
                    byproducts_sa = [c for c in sa_consumptions if c["norm"] < 0]
                    if consumables_sa:
                        logger.info("  [ODS]  │    In simple words: To make 1 %s from %s, you need:",
                                   uom, sa["plant_name"])
                        for c in consumables_sa:
                            logger.info("  [ODS]  │      → %.6f %s of %s", c["norm"], c["material_uom"], c["material"])
                    if byproducts_sa:
                        logger.info("  [ODS]  │    And as a bonus (byproduct), you also GET:")
                        for c in byproducts_sa:
                            logger.info("  [ODS]  │      → %.6f %s of %s (this is extra output, not input!)",
                                       abs(c["norm"]), c["material_uom"], c["material"])
                    logger.info("  [ODS]  │")
            else:
                # Single sub-asset — simpler format
                logger.info("  [ODS]  │  %-6s  %-32s  %-15s  %-12s  %-10s",
                             "Type", "Ingredient (Material)", "Material SAP Code", "Norm", "UOM")
                logger.info("  [ODS]  │  %-6s  %-32s  %-15s  %-12s  %-10s",
                             "------", "--------------------------------", "---------------", "------------", "----------")

                for c in consumptions:
                    norm_str = f"{c['norm']:.6f}" if c['norm'] != 0 else "0"
                    type_label = "Utility" if c["account"] == "Utilities" else "Raw Mat."
                    mat_id = c.get("material_id", "---") or "---"
                    logger.info("  [ODS]  │  %-6s  %-32s  %-15s  %12s  %-10s",
                                 type_label, c["material"], mat_id, norm_str, c["material_uom"])

                # Simple explanation
                logger.info("  [ODS]  │")
                consumables = [c for c in consumptions if c["norm"] > 0]
                byproducts = [c for c in consumptions if c["norm"] < 0]
                if consumables:
                    logger.info("  [ODS]  │  In simple words: To make 1 %s of %s, you need:", uom, producer)
                    for c in consumables:
                        logger.info("  [ODS]  │    → %.6f %s of %s", c["norm"], c["material_uom"], c["material"])
                if byproducts:
                    logger.info("  [ODS]  │  And as a bonus (byproduct), you also GET:")
                    for c in byproducts:
                        logger.info("  [ODS]  │    → %.6f %s of %s (this is extra output, not input!)",
                                   abs(c["norm"]), c["material_uom"], c["material"])

            logger.info("  [ODS]  └──────────────────────────────────────────────────────────────")
            logger.info("  [ODS]")

        logger.info("  [ODS]  Total producers with consumption norms: %d", len(consumption))
        logger.info("  [ODS] ===================================")
