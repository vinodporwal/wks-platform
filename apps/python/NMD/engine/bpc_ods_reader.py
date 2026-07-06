"""
NMD BPC ODS Reader
==================

Reads the NMD BPC.ods file and provides BPC generation quantities and
BPC consumption quantities for comparison with the model's calculated values.

BPC.ods Structure:
  Row 0: Financial year label (e.g., "FY 2026")
  Row 1: Quarter labels (Q1, Q2, Q3, Q4)
  Row 2: Month names (April, May, ..., March)
  Row 3: Column headers
  Row 4+: Data rows (grouped by Generating Plant)

Columns:
  0: Generating Plant (only on first row of group, NaN for continuation)
  1: Utility (only on first row of group)
  2: Utility ID
  3: UOM (utility UOM)
  4: Account
  5: Material
  6: Issuing Plant
  7: UOM (material UOM)
  8: qty (always 0 in data)
  9: Norms (April)
  10: Gen Qty (April, only for some rows)
  11: Quantity (April)
  12: Amount (Rs.) (April)
  13: Price (April)
  14-17: May (Norms, Quantity, Amount, Price)
  18-21: June
  ... and so on for 12 months (April through March)

  April has 5 data columns (including Gen Qty at col 10).
  All other months have 4 data columns each.
"""

from __future__ import annotations

import logging
import os
from typing import Dict, List, Optional

import pandas as pd

logger = logging.getLogger(__name__)


def _norm_key(name: str) -> str:
    """Normalize name for fuzzy matching (lowercase, alphanumeric only)."""
    return "".join(ch.lower() for ch in str(name) if ch.isalnum())

# ---------------------------------------------------------------------------
# Column index constants (0-based, from the ODS header row at index 3)
# ---------------------------------------------------------------------------
_COL_PLANT         = 0
_COL_UTILITY       = 1
_COL_UTILITY_ID    = 2
_COL_UTILITY_UOM   = 3
_COL_ACCOUNT       = 4
_COL_MATERIAL      = 5
_COL_ISSUING_PLANT = 6
_COL_MATERIAL_UOM  = 7
_COL_QTY           = 8

# April starts at col 9 with 5 columns (Norms, GenQty, Quantity, Amount, Price)
# May starts at col 14 with 4 columns each (Norms, Quantity, Amount, Price)
_APRIL_NORM_COL = 9
_APRIL_GENQTY_COL = 10
_APRIL_QTY_COL = 11

# After April, each month has 4 columns
_POST_APRIL_BASE = 14  # May starts here
_COLS_PER_MONTH = 4

# Month order in the ODS: April=0, May=1, ..., March=11
_MONTH_ORDER = {
    4: 0, 5: 1, 6: 2, 7: 3, 8: 4, 9: 5,
    10: 6, 11: 7, 12: 8, 1: 9, 2: 10, 3: 11,
}

# Data starts at row index 4
_DATA_START_ROW = 4


def _get_month_cols(month: int) -> tuple:
    """Return (norm_col, qty_col) for a given calendar month (1-12)."""
    idx = _MONTH_ORDER[month]
    if idx == 0:
        # April
        return (_APRIL_NORM_COL, _APRIL_QTY_COL)
    else:
        norm_col = _POST_APRIL_BASE + (idx - 1) * _COLS_PER_MONTH
        qty_col = norm_col + 1
        return (norm_col, qty_col)


def _resolve_bpc_path() -> str:
    """Resolve the BPC.ods file path."""
    filename = "BPC.ods"
    # Check relative to this file
    base_dir = os.path.dirname(os.path.abspath(__file__))
    for _ in range(3):
        p = os.path.join(base_dir, filename)
        if os.path.exists(p):
            return p
        base_dir = os.path.dirname(base_dir)
    # Check current directory
    if os.path.exists(filename):
        return os.path.abspath(filename)
    return filename


# ---------------------------------------------------------------------------
# Reader cache
# ---------------------------------------------------------------------------
_READER_CACHE: dict = {}


class BPCODSReader:
    """
    Reader for NMD BPC.ods file.

    Provides BPC generation quantities and BPC consumption quantities
    for comparison with model-calculated values.

    Use get_reader() class method to get a cached instance.
    """

    def __init__(self, month: int, year: int):
        self.month = month
        self.year = year
        self.filepath = _resolve_bpc_path()
        self._df: Optional[pd.DataFrame] = None
        self._norm_col: int = 0
        self._qty_col: int = 0
        self._loaded = False
        self.is_available = False

    @classmethod
    def get_reader(cls, month: int, year: int) -> "BPCODSReader":
        """Get a cached reader instance for the given month/year."""
        key = (month, year)
        if key not in _READER_CACHE:
            _READER_CACHE[key] = cls(month, year)
        return _READER_CACHE[key]

    @classmethod
    def clear_cache(cls):
        """Clear the reader cache."""
        _READER_CACHE.clear()

    def load(self):
        """Public method to trigger loading of the ODS file."""
        self._load()

    def _load(self):
        """Load the ODS file and prepare the DataFrame."""
        if self._loaded:
            return

        self._loaded = True

        if not os.path.exists(self.filepath):
            logger.warning("  [BPC] File not found: %s", self.filepath)
            return

        try:
            self._df = pd.read_excel(self.filepath, engine="odf", header=None)
            self._norm_col, self._qty_col = _get_month_cols(self.month)

            # Forward-fill plant name, utility, account, and material within groups
            self._df[_COL_PLANT] = self._df[_COL_PLANT].ffill()
            self._df[_COL_UTILITY] = self._df[_COL_UTILITY].ffill()
            self._df[_COL_ACCOUNT] = self._df[_COL_ACCOUNT].ffill()
            self._df[_COL_MATERIAL] = self._df[_COL_MATERIAL].ffill()

            self.is_available = True
            logger.info("  [BPC] Loaded %s (%d rows, %d cols) for month %d/%d",
                        os.path.basename(self.filepath),
                        len(self._df), len(self._df.columns),
                        self.month, self.year)
            logger.info("  [BPC] Month columns: norm=%d, qty=%d",
                        self._norm_col, self._qty_col)

        except Exception as e:
            logger.error("  [BPC] Error loading %s: %s", self.filepath, e)
            self.is_available = False

    def _iter_data_rows(self):
        """Iterate over data rows (starting from row 4)."""
        self._load()
        if not self.is_available:
            return
        for idx in range(_DATA_START_ROW, len(self._df)):
            row = self._df.iloc[idx]
            yield idx, row

    def _get_norm_val(self, row) -> float:
        """Extract norm value from the row for the configured month."""
        val = row[self._norm_col]
        if pd.isna(val):
            return 0.0
        try:
            return float(val)
        except (ValueError, TypeError):
            return 0.0

    def _get_qty_val(self, row) -> float:
        """Extract quantity value from the row for the configured month."""
        val = row[self._qty_col]
        if pd.isna(val):
            return 0.0
        try:
            return float(val)
        except (ValueError, TypeError):
            return 0.0

    def _get_genqty_val(self, row) -> float:
        """Extract Gen Qty value (only available for April)."""
        if self._norm_col != _APRIL_NORM_COL:
            return 0.0
        val = row[_APRIL_GENQTY_COL]
        if pd.isna(val):
            return 0.0
        try:
            return float(val)
        except (ValueError, TypeError):
            return 0.0

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def get_bpc_generation_quantities(self) -> Dict[str, float]:
        """
        Return BPC generation quantities per producer from the ODS file.

        For each producer (utility), picks any material row with non-zero norm
        and non-zero quantity, then BPC_gen_qty = quantity / norm.

        For POWERGEN, keys by generating plant name (asset name).
        For other utilities, keys by utility name.

        Returns:
            {"NMD - Power Plant 2": 8063310.0, "Boiler Feed Water": 101372.0, ...}
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

            plant = str(row[_COL_PLANT]).strip() if pd.notna(row[_COL_PLANT]) else ""
            issuing_plant = str(row[_COL_ISSUING_PLANT]).strip() if pd.notna(row[_COL_ISSUING_PLANT]) else ""
            norm_val = self._get_norm_val(row)
            qty_val = self._get_qty_val(row)

            if norm_val == 0 or qty_val == 0:
                continue

            gen_qty = qty_val / norm_val

            # POWERGEN rows: key by plant name (for utility=POWERGEN)
            # or by issuing_plant (for material=POWERGEN under Power_Dis)
            if utility == "POWERGEN":
                key = plant
            elif material == "POWERGEN":
                key = issuing_plant
            else:
                key = utility

            if key and key not in result:
                result[key] = gen_qty

        return result

    def get_bpc_quantities(self) -> Dict[str, Dict[str, float]]:
        """
        Return BPC quantities per producer+material from the ODS file.

        For POWERGEN, keys by generating plant name (asset name).
        For other utilities, keys by utility name.

        Returns:
            {
                "NMD - Power Plant 2": {"NATURAL GAS": 81819.53, "COMPRESSED AIR": 30960, ...},
                "Boiler Feed Water": {"Cooling Water 2": 108, "D M Water": 86780.29, ...},
            }
        """
        if not self.is_available:
            return {}

        result: dict = {}

        for _, row in self._iter_data_rows():
            utility = str(row[_COL_UTILITY]).strip()
            account = str(row[_COL_ACCOUNT]).strip()
            material = str(row[_COL_MATERIAL]).strip()

            if account not in ("Utilities", "Raw Material", "By Product"):
                continue
            if not utility or utility == "nan" or not material or material == "nan":
                continue
            if utility == "Total" or material == "Total":
                continue

            plant = str(row[_COL_PLANT]).strip() if pd.notna(row[_COL_PLANT]) else ""
            issuing_plant = str(row[_COL_ISSUING_PLANT]).strip() if pd.notna(row[_COL_ISSUING_PLANT]) else ""
            qty_val = self._get_qty_val(row)

            # POWERGEN rows: key by plant name (for utility=POWERGEN)
            # or by issuing_plant (for material=POWERGEN under Power_Dis)
            if utility == "POWERGEN":
                key = plant
            elif material == "POWERGEN":
                key = issuing_plant
            else:
                key = utility

            if key:
                if key not in result:
                    result[key] = {}
                result[key][material] = qty_val

        return result

    def get_bpc_data_rows(self) -> List[dict]:
        """
        Return all BPC data rows as a list of dicts, with forward-filled
        plant, utility, and account fields.

        Each dict contains:
            plant, utility, utility_uom, account, material, material_uom,
            issuing_plant, norm, quantity, gen_qty
        """
        if not self.is_available:
            return []

        result = []

        for _, row in self._iter_data_rows():
            utility = str(row[_COL_UTILITY]).strip() if pd.notna(row[_COL_UTILITY]) else ""
            material = str(row[_COL_MATERIAL]).strip() if pd.notna(row[_COL_MATERIAL]) else ""
            account = str(row[_COL_ACCOUNT]).strip() if pd.notna(row[_COL_ACCOUNT]) else ""

            if not utility or utility == "nan" or not material or material == "nan":
                continue
            if utility == "Total" or material == "Total":
                continue

            plant = str(row[_COL_PLANT]).strip() if pd.notna(row[_COL_PLANT]) else ""
            utility_uom = str(row[_COL_UTILITY_UOM]).strip() if pd.notna(row[_COL_UTILITY_UOM]) else ""
            material_uom = str(row[_COL_MATERIAL_UOM]).strip() if pd.notna(row[_COL_MATERIAL_UOM]) else ""
            issuing_plant = str(row[_COL_ISSUING_PLANT]).strip() if pd.notna(row[_COL_ISSUING_PLANT]) else ""

            norm_val = self._get_norm_val(row)
            qty_val = self._get_qty_val(row)
            genqty_val = self._get_genqty_val(row)

            result.append({
                "plant": plant,
                "utility": utility,
                "utility_uom": utility_uom,
                "account": account,
                "material": material,
                "material_uom": material_uom,
                "issuing_plant": issuing_plant,
                "norm": norm_val,
                "quantity": qty_val,
                "gen_qty": genqty_val,
            })

        return result

    def get_bpc_gen_qty_for_producer(self, producer: str) -> float:
        """Look up BPC generation quantity for a producer by name (case-insensitive, fuzzy)."""
        gen_map = self.get_bpc_generation_quantities()
        if producer in gen_map:
            return gen_map[producer]
        prod_norm = _norm_key(producer)
        for key, val in gen_map.items():
            if _norm_key(key) == prod_norm:
                return val
        # Fuzzy: check if one contains the other
        for key, val in gen_map.items():
            kn = _norm_key(key)
            if kn and prod_norm and (kn in prod_norm or prod_norm in kn):
                return val
        return 0.0

    def get_bpc_qty_for_producer_material(self, producer: str, material: str) -> float:
        """Look up BPC quantity for a producer+material (case-insensitive, fuzzy)."""
        qty_map = self.get_bpc_quantities()
        bpc_producer = None
        if producer in qty_map:
            bpc_producer = qty_map[producer]
        else:
            prod_norm = _norm_key(producer)
            for key, val in qty_map.items():
                if _norm_key(key) == prod_norm:
                    bpc_producer = val
                    break
            if not bpc_producer:
                for key, val in qty_map.items():
                    kn = _norm_key(key)
                    if kn and prod_norm and (kn in prod_norm or prod_norm in kn):
                        bpc_producer = val
                        break
        if not bpc_producer:
            return 0.0
        if material in bpc_producer:
            return bpc_producer[material]
        mat_norm = _norm_key(material)
        for k, v in bpc_producer.items():
            if _norm_key(k) == mat_norm:
                return v
        return 0.0

    def log_bpc_summary(self):
        """Log a summary of the BPC data for verification."""
        if not self.is_available:
            logger.warning("  [BPC] No data available — file not loaded")
            return

        gen_map = self.get_bpc_generation_quantities()
        qty_map = self.get_bpc_quantities()

        logger.info("  [BPC] ===== BPC SUMMARY (%s %d/%d) =====",
                    os.path.basename(self.filepath), self.month, self.year)
        logger.info("  [BPC] Producers with generation quantities: %d", len(gen_map))
        logger.info("  [BPC] Producers with consumption quantities: %d", len(qty_map))

        for producer in sorted(gen_map.keys()):
            gen = gen_map[producer]
            materials = qty_map.get(producer, {})
            logger.info("  [BPC]   %-30s  gen=%14.2f  materials=%d",
                        producer, gen, len(materials))
