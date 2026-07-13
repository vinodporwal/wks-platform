"""
Norms Reader Factory
====================
Factory function to return either ODS-based or database-based norms reader
based on the USE_DATABASE_NORMS feature flag.

This provides a single entry point for norms reader instantiation,
allowing seamless switching between ODS and DB sources without modifying
calling code.
"""

import logging
from config.db_config import USE_DATABASE_NORMS
from engine.ods_norms_reader import ODSNormsReader
from engine.db_norms_reader import DBNormsReader

logger = logging.getLogger(__name__)


def get_norms_reader(plant_id: str, month: int, year: int):
    """
    Factory function to return ODS or DB reader based on feature flag.
    
    Args:
        plant_id: CPP plant UUID
        month: Month (1-12)
        year: Year (e.g., 2026)
    
    Returns:
        Either ODSNormsReader or DBNormsReader instance (cached)
    
    The returned object has identical interface regardless of source:
        - get_consumption_norms()
        - get_powergen_norms()
        - get_steam_letdown_norms()
        - get_hrsg_byproduct_norms()
        - get_u4u_norms_matrix()
        - get_steam_distribution()
        - get_stg_steam_consumption_norms()
        - get_raw_material_norms()
        - get_prds_bfw_norms()
        - is_available property
    """
    if USE_DATABASE_NORMS:
        logger.info("  [NORMS FACTORY] Using DATABASE norms reader for plant %s, month %d, year %d",
                    plant_id, month, year)
        return DBNormsReader.get_reader(plant_id, month, year)
    else:
        logger.info("  [NORMS FACTORY] Using ODS norms reader for plant %s, month %d, year %d",
                    plant_id, month, year)
        return ODSNormsReader.get_reader(plant_id, month, year)


def clear_all_caches():
    """Clear caches for both ODS and DB readers."""
    ODSNormsReader.clear_cache()
    DBNormsReader.clear_cache()
    logger.info("  [NORMS FACTORY] Cleared all reader caches")
