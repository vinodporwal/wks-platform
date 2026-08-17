"""
Database Norms Reader for JMD Plants
====================================
Reads norms from CPPNorms database tables (NormsHeader, NormsMonthDetail)
instead of ODS files. Provides identical interface to ODSNormsReader
for seamless migration.

This class follows the NMD approach:
- Fetches norms filtered by Plant_FK_Id via NormsHeader table
- Uses WITH (NOLOCK) hints to prevent data locks
- Builds consumption matrix dynamically from database rows
- Caches reader instances per (plant_id, month, year)

Interface mirrors ODSNormsReader exactly:
- Same method signatures
- Same return data structures
- Same caching behavior
"""

import logging
from typing import Optional, Dict, Set, List

from database.queries import fetch_norm_rows_for_jmd

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Reader cache — avoids re-instantiating for the same plant/month/year
# ---------------------------------------------------------------------------
_READER_CACHE: dict = {}


class DBNormsReader:
    """
    Database-based norms reader for JMD plants.
    
    Mirrors ODSNormsReader interface exactly, fetching data from
    NormsHeader and NormsMonthDetail tables instead of ODS files.
    
    Use get_reader() class method to get a cached instance.
    """

    def __init__(self, plant_id: str, month: int, year: int):
        self.plant_id = plant_id
        self.month = month
        self.year = year
        self._rows: List[Dict] = []
        self._loaded = False

        # Derived structures (populated by _build_indexes)
        self._consumption_matrix: Dict[str, Dict] = {}
        self._powergen_norms: Dict[str, float] = {}
        self._steam_letdown_norms: Dict[str, Dict] = {}
        self._hrsg_byproduct_norms: Dict[str, float] = {}
        self._u4u_norms_matrix: Dict[str, Dict] = {}
        self._steam_distribution: Dict[str, Dict] = {}
        self._stg_steam_norms: Dict[str, float] = {}
        self._raw_material_norms: Dict[str, float] = {}
        self._prds_bfw_norms: Dict[str, float] = {}
        self._bpc_gen_quantities: Dict[str, float] = {}
        self._bpc_quantities: Dict[str, Dict] = {}

    # ------------------------------------------------------------------
    # Public class method for cached access
    # ------------------------------------------------------------------

    @classmethod
    def get_reader(cls, plant_id: str, month: int, year: int) -> "DBNormsReader":
        """Get a cached DBNormsReader instance for the given plant/month/year."""
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
        """Load norms from database and build indexes."""
        if self._loaded:
            return

        logger.info("  [DB NORMS READER] Loading norms for plant %s, month %d, year %d",
                    self.plant_id, self.month, self.year)

        self._rows = fetch_norm_rows_for_jmd(self.plant_id, self.month, self.year)
        
        if not self._rows:
            logger.warning("  [DB NORMS READER] No norm rows found in database")
            self._loaded = True
            return

        self._build_indexes()
        self._loaded = True
        logger.info("  [DB NORMS READER] Loaded %d norm rows, built indexes", len(self._rows))

    def _build_indexes(self):
        """Build consumption matrix and other derived structures from database rows."""
        # Build consumption matrix (main structure used by U4U loop)
        for row in self._rows:
            utility_name = row["utility_name"]
            account_name = row["account_name"]
            material_name = row["material_name"]
            norm_val = row["norm"]
            qty_val = row["qty"]
            material_uom = row["material_uom"]
            utility_uom = row["utility_uom"]
            issuing_plant_name = row["issuing_plant_name"]

            # Skip empty or invalid entries
            if not utility_name or not material_name:
                continue
            if utility_name == "Total" or material_name == "Total":
                continue

            # Initialize producer entry if not exists
            if utility_name not in self._consumption_matrix:
                self._consumption_matrix[utility_name] = {
                    "producer_uom": utility_uom,
                    "utility_id": row["utility_id"],
                    "utility_plant_id": self.plant_id,
                    "sub_assets": [],
                    "consumptions": [],
                }

            # Add consumption entry
            self._consumption_matrix[utility_name]["consumptions"].append({
                "account": account_name,
                "material": material_name,
                "material_id": row["material_id"],
                "issuing_plant_id": "",  # Not available in current DB schema
                "issuing_plant": issuing_plant_name,  # Used to detect inter-plant transfers
                "norm": norm_val,
                "quantity": qty_val,
                "material_uom": material_uom,
                "source_plant": row["plant_name"],
                "source_plant_id": self.plant_id,
                "norms_header_id": row.get("norms_header_id"),
                "norms_month_detail_id": row.get("norms_month_detail_id"),
            })

        # Build specialized indexes for specific norm types
        self._build_powergen_norms()
        self._build_steam_letdown_norms()
        self._build_hrsg_byproduct_norms()
        self._build_u4u_norms_matrix()
        self._build_steam_distribution()
        self._build_stg_steam_norms()
        self._build_raw_material_norms()
        self._build_prds_bfw_norms()

    def _build_powergen_norms(self):
        """Build powergen norms (auxiliary power per asset)."""
        for row in self._rows:
            utility_name = row["utility_name"]
            material_name = row["material_name"]
            
            if utility_name == "POWERGEN" and material_name == "Power_Dis":
                plant_name = row["plant_name"].upper()
                self._powergen_norms[plant_name] = row["norm"]

    def _build_steam_letdown_norms(self):
        """Build steam letdown norms (PRDS cascade factors)."""
        # Collect all _Dis steam materials
        dis_materials = set()
        for row in self._rows:
            material = row["material_name"]
            if material and material.endswith("_Dis") and "Steam" in material:
                dis_materials.add(material)

        # Collect PRDS entries
        prds_map = {}
        for row in self._rows:
            utility = row["utility_name"]
            material = row["material_name"]
            account = row["account_name"]
            
            if account == "Utilities" and "PRDS" in utility.upper() and material in dis_materials:
                prds_map[utility] = {
                    "consumes": material,
                    "norm": row["norm"],
                }

        # Build cascade ordered from lowest to highest pressure: LP → MP → HP → SHP
        # This order is critical for dispatch_engine to correctly identify the top grade
        pressure_order = {"LP": 0, "MP": 1, "HP": 2, "SHP": 3}
        cascade = []
        for prds_name, info in prds_map.items():
            # Try to infer what this PRDS produces from its name
            # This is a simplified approach - may need adjustment based on actual naming
            if "LP" in prds_name.upper():
                produces = "LP Steam_Dis"
            elif "MP" in prds_name.upper():
                produces = "MP Steam_Dis"
            elif "HP" in prds_name.upper():
                produces = "HP Steam_Dis"
            else:
                produces = material  # fallback
            
            cascade.append({
                "prds": prds_name,
                "produces": produces,
                "consumes": info["consumes"],
                "norm": info["norm"],
            })
        
        # Sort cascade by pressure order (lowest to highest)
        cascade.sort(key=lambda step: pressure_order.get(step["produces"].replace(" Steam_Dis", ""), 99))

        # Build flat legacy keys
        norms = {"_cascade": cascade}
        for step in cascade:
            prod_prefix = step["produces"].replace(" Steam_Dis", "").replace("_Dis", "")
            cons_prefix = step["consumes"].replace(" Steam_Dis", "").replace("_Dis", "")
            norms[f"{prod_prefix}_to_{cons_prefix}"] = step["norm"]

        self._steam_letdown_norms = norms

    def _build_hrsg_byproduct_norms(self):
        """Build byproduct norms (LP steam credits) for all steam generation assets."""
        for row in self._rows:
            utility = row["utility_name"]
            material = row["material_name"]
            
            if "LP STEAM" in material.upper() and row["norm"] is not None and row["norm"] < 0:
                self._hrsg_byproduct_norms[utility.upper()] = row["norm"]

    def _build_u4u_norms_matrix(self):
        """Build U4U norms matrix (utility-for-utility dependencies)."""
        for row in self._rows:
            account = row["account_name"]
            
            if account != "Utilities":
                continue

            consumer = row["utility_name"]
            producer = row["material_name"]
            
            if not consumer or not producer:
                continue

            if consumer not in self._u4u_norms_matrix:
                self._u4u_norms_matrix[consumer] = {}
            
            self._u4u_norms_matrix[consumer][producer] = {
                "norm": row["norm"],
                "qty": row["qty"],
            }

    def _build_steam_distribution(self):
        """Build steam distribution factors."""
        for row in self._rows:
            utility = row["utility_name"]
            material = row["material_name"]
            account = row["account_name"]
            
            if utility.endswith("_Dis") and "Steam" in utility and account == "Utilities":
                norm_val = row["norm"]
                if norm_val > 0 or norm_val < 0:  # Include positive and negative norms
                    if utility not in self._steam_distribution:
                        self._steam_distribution[utility] = {}
                    self._steam_distribution[utility][material] = norm_val

    def _build_stg_steam_norms(self):
        """Build STG steam consumption norms."""
        for row in self._rows:
            utility = row["utility_name"]
            material = row["material_name"]
            
            if utility == "POWERGEN" and material == "SHP Steam_Dis":
                plant_name = row["plant_name"].upper()
                if "STG" in plant_name:
                    self._stg_steam_norms[plant_name] = row["norm"]

    def _build_raw_material_norms(self):
        """Build raw material (fuel) norms."""
        for row in self._rows:
            utility = row["utility_name"]
            material = row["material_name"]
            account = row["account_name"]
            
            if account == "Raw Material" and material == "SynGas(Unshift)":
                self._raw_material_norms[utility] = row["norm"]

    def _build_prds_bfw_norms(self):
        """Build PRDS BFW consumption norms."""
        for row in self._rows:
            utility = row["utility_name"]
            material = row["material_name"]
            account = row["account_name"]
            
            if account == "Utilities" and material == "Boiler Feed Water" and "PRDS" in utility.upper():
                self._prds_bfw_norms[utility] = row["norm"]

    # ------------------------------------------------------------------
    # Public methods (mirroring ODSNormsReader interface)
    # ------------------------------------------------------------------

    @property
    def is_available(self) -> bool:
        """True if norms were loaded successfully from database."""
        return self._loaded and len(self._rows) > 0

    def get_consumption_norms(self, include_all_accounts: bool = False) -> dict:
        """
        Return the consumption norms matrix for every producing utility/asset.
        
        Structure matches ODSNormsReader exactly.
        """
        if not self.is_available:
            return {}
        
        if include_all_accounts:
            return self._consumption_matrix
        
        # Filter to only Utilities and Raw Material accounts (default for U4U)
        filtered = {}
        for producer, info in self._consumption_matrix.items():
            filtered[producer] = {
                "producer_uom": info["producer_uom"],
                "utility_id": info["utility_id"],
                "utility_plant_id": info["utility_plant_id"],
                "sub_assets": info["sub_assets"],
                "consumptions": [
                    c for c in info["consumptions"]
                    if c["account"] in ("Utilities", "Raw Material")
                ],
            }
        return filtered

    def get_all_consumption_norms(self) -> dict:
        """Return full consumption norms including all account types."""
        return self.get_consumption_norms(include_all_accounts=True)

    def get_consumption_norms_flat(self) -> dict:
        """Return flat version of consumption norms for quick lookup."""
        full = self.get_consumption_norms()
        flat = {}
        for producer, info in full.items():
            flat[producer] = {}
            for c in info["consumptions"]:
                flat[producer][c["material"]] = c["norm"]
        return flat

    def get_powergen_norms(self) -> dict:
        """Return auxiliary power norms for each power generation asset."""
        return self._powergen_norms

    def get_steam_letdown_norms(self) -> dict:
        """Return PRDS letdown norms for steam cascade."""
        return self._steam_letdown_norms

    def get_hrsg_byproduct_norms(self) -> dict:
        """Return LP steam byproduct norms for HRSG assets."""
        return self._hrsg_byproduct_norms

    def get_u4u_norms_matrix(self) -> dict:
        """Return U4U dependency matrix."""
        return self._u4u_norms_matrix

    def get_steam_distribution(self) -> dict:
        """Return steam distribution factors."""
        return self._steam_distribution

    def get_stg_steam_consumption_norms(self) -> dict:
        """Return STG steam consumption norms."""
        return self._stg_steam_norms

    def get_raw_material_norms(self) -> dict:
        """Return raw material (fuel) consumption norms."""
        return self._raw_material_norms

    def get_prds_bfw_norms(self) -> dict:
        """Return PRDS BFW consumption norms."""
        return self._prds_bfw_norms

    def get_bpc_generation_quantities(self) -> dict:
        """
        Return BPC generation quantities per producer from the database.

        For each producer (utility), picks any material row with non-zero norm
        and non-zero quantity, then BPC_gen_qty = quantity / norm.

        For multi-asset producers like POWERGEN, keys by source_plant (asset name).
        For single-asset producers, keys by utility name.

        Returns:
            {"JMD - C2-GTG 1": 54100000.0, "AUXBOIL7_SHP STEAM": 62820.0, ...}
        """
        if not self._rows:
            return {}

        result: dict = {}

        for row in self._rows:
            utility = row["utility_name"]
            account = row["account_name"]
            material = row["material_name"]
            source_plant = row["plant_name"]

            if account not in ("Utilities", "Raw Material"):
                continue
            if not utility or not material:
                continue
            if utility == "Total" or material == "Total":
                continue

            norm_val = row["norm"]
            qty_val = row["qty"]

            if norm_val == 0 or qty_val == 0:
                continue

            gen_qty = qty_val / norm_val

            if utility == "POWERGEN":
                key = source_plant
            else:
                key = utility

            if key and key not in result:
                result[key] = gen_qty

        return result

    def get_bpc_quantities(self) -> dict:
        """
        Return BPC quantities per producer+material from the database.

        For multi-asset producers like POWERGEN, keys by source_plant (asset name).
        For single-asset producers, keys by utility name.

        Returns:
            {"JMD - C2-GTG 1": {"SynGas(Unshift)": 328541.30, "Power_Dis": 101533.57, ...},
             "AUXBOIL7_SHP STEAM": {"SynGas(Unshift)": 247872.88, "Boiler Feed Water": 87041.85, ...}}
        """
        if not self._rows:
            return {}

        result: dict = {}

        for row in self._rows:
            utility = row["utility_name"]
            account = row["account_name"]
            material = row["material_name"]
            source_plant = row["plant_name"]

            if not utility or not material:
                continue
            if utility == "Total" or material == "Total":
                continue

            qty_val = row["qty"]

            if utility == "POWERGEN":
                key = source_plant
            else:
                key = utility

            if key:
                if key not in result:
                    result[key] = {}
                result[key][material] = qty_val

        return result

    def log_all_norms(self):
        """Log all loaded norms for debugging."""
        logger.info("  [DB NORMS READER] === Norms Summary ===")
        logger.info("  [DB NORMS READER] Total rows loaded: %d", len(self._rows))
        logger.info("  [DB NORMS READER] Consumption matrix producers: %d", len(self._consumption_matrix))
        logger.info("  [DB NORMS READER] Powergen norms: %d", len(self._powergen_norms))
        logger.info("  [DB NORMS READER] Steam letdown norms: %d", len(self._steam_letdown_norms))
        logger.info("  [DB NORMS READER] HRSG byproduct norms: %d", len(self._hrsg_byproduct_norms))
        logger.info("  [DB NORMS READER] U4U matrix entries: %d", len(self._u4u_norms_matrix))
        logger.info("  [DB NORMS READER] Steam distribution entries: %d", len(self._steam_distribution))
        logger.info("  [DB NORMS READER] STG steam norms: %d", len(self._stg_steam_norms))
        logger.info("  [DB NORMS READER] Raw material norms: %d", len(self._raw_material_norms))
        logger.info("  [DB NORMS READER] PRDS BFW norms: %d", len(self._prds_bfw_norms))
