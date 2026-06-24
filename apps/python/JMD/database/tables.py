"""
JMD Database Table & Column Registry
=====================================
Single source of truth for every table name and key column name used by queries.py.

To update when the real DB schema changes:
  - Change the value here; queries.py picks it up automatically.
  - If a missing table now exists in the DB, set its USE_DUMMY flag to False
    so live data is fetched instead of the fallback JSON.

Usage:
    from database.tables import T, USE_DUMMY
"""


class T:
    """Table and column names."""

    # ── Core lookup tables ─────────────────────────────────────────────────
    PLANTS                      = "Plants"
    FINANCIAL_YEAR_MONTH        = "FinancialYearMonth"
    NORM_PARAMETERS             = "NormParameters"

    # ── Fixed consumption ──────────────────────────────────────────────────
    FIXED_CONSUMPTION           = "CPPFixedConsumption"
    FIXED_CONSUMPTION_LEGACY    = "CPPFixedConsumption"
    FIXED_CONSUMPTION_MONTH_FK  = "FinancialYearMonthId"   # legacy FK col in older schema
    FIXED_CONSUMPTION_VALUE     = "PowerRequirement"        # legacy single-value column

    # ── Process demand ─────────────────────────────────────────────────────
    PROCESS_DEMAND_MASTER       = "ProcessDemandMaster"        # config: process plant → utility → cpp plant
    CALCULATED_PROCESS_DEMAND   = "CalculatedProcessDemand"

    # ── Power generation assets ────────────────────────────────────────────
    POWER_GENERATION_ASSETS     = "PowerGenerationAssets"
    PGA_PLANT_FK                = "CPPPLANT_FK_Id"          # plant filter col

    # ── Steam generation assets ────────────────────────────────────────────
    CPP_STEAM_GENERATION_ASSET  = "CPPSteamGenerationAsset"
    SGA_PLANT_FK                = "CPPPLANT_FK_Id"          # plant filter col

    # ── Operational hours (wide-column, per FY) ────────────────────────────
    CPP_ASSET_OPERATIONAL_HOURS = "CPPAssetOperationalHours"
    CPP_STEAM_ASSETS_OPERATIONAL_HOURS = "CPPSteamAssetsOperationalHours"
    CAOH_ASSET_FK               = "Asset_FK_Id"
    CAOH_YEAR_COL               = "AOPYear"

    # ── Asset priority tables ──────────────────────────────────────────────
    CPP_POWER_ASSET_PRIORITY    = "CPPPowerAssetPriority"
    CPP_STEAM_ASSETS_PRIORITY   = "CPPSteamAssetsPriority"
    PRIORITY_ASSET_FK           = "Asset_FK_Id"
    PRIORITY_PLANT_FK           = "Plant_FK_Id"
    PRIORITY_YEAR_COL           = "AOPYear"

    # ── Asset capacity tables ──────────────────────────────────────────────
    CPP_POWER_ASSET_CAPACITY    = "CPPPowerAssetCapacity"   # {Mon}_Min / {Mon}_Max per month
    CPP_STEAM_ASSET_CAPACITY    = "CPPSteamAssetCapacity"
    CAPACITY_ASSET_FK           = "Asset_FK_Id"
    CAPACITY_YEAR_COL           = "AOPYear"

    # ── Import power ───────────────────────────────────────────────────────
    CPP_IMPORT_POWER_SOURCE_MAPPING   = "CPPImportPowerSourceMapping"
    CPP_IMPORT_POWER_CAPACITY         = "CPPImportPowerCapacity"
    CPP_IMPORT_POWER_OPERATIONAL_HOURS= "CPPImportPowerOperationalHours"
    IMPORT_PLANT_FK             = "CPPPlant_FK_Id"

    # ── Norms ──────────────────────────────────────────────────────────────
    NORMS_HEADER                = "NormsHeader"
    NORMS_MONTH_DETAIL          = "NormsMonthDetail"
    NORMS_HEADER_PLANT_FK       = "Plant_FK_Id"             # correct col (not CPPPlant_FK_Id)

    # ── HRSG heat rate lookup ──────────────────────────────────────────────
    CPP_HRSG_HEAT_RATE          = "CPP_HRSGHeatRate"
    HRSG_HR_ASSET_FK            = "Asset_FK_Id"             # joined via CPPSteamGenerationAsset
    HRSG_HR_LOAD_COL            = "HRSGLoad"
    HRSG_HR_VALUE_COL           = "FinalHeatRate"
    HRSG_HR_YEAR_COL            = "FinancialYear"

    # ── GT heat rate lookup ────────────────────────────────────────────────
    CPP_GT_HEAT_RATE            = "CPP_GTHeatRate"
    GT_HR_ASSET_FK              = "Asset_FK_Id"             # joined via PowerGenerationAssets
    GT_HR_LOAD_COL              = "GTLoad"
    GT_HR_VALUE_COL             = "FinalHeatRate"
    GT_HR_YEAR_COL              = "FinancialYear"

    # ── HRSG availability (missing in DB — uses dummy JSON fallback) ───────
    HRSG_AVAILABILITY           = "HRSGAvailability"        # does not exist yet
    HRSG_ASSETS                 = "HRSGAssets"              # does not exist yet

    # ── STG extraction lookup (missing in DB — uses dummy JSON fallback) ───
    CPP_STG_EXTRACTION_LOOKUP   = "CPP_STGExtractionLookup" # does not exist yet

    # ── Legacy / alternate tables (kept for reference) ─────────────────────
    OPERATIONAL_HOURS           = "OperationalHours"        # old per-month table
    ASSET_AVAILABILITY          = "AssetAvailability"       # old availability table


# Tables that don't exist in the DB yet.
# Set a flag to False once the real table is created and populated —
# the corresponding fetch function will then hit the DB instead of dummy JSON.
USE_DUMMY = {
    T.HRSG_AVAILABILITY:        True,
    T.HRSG_ASSETS:              True,
    T.CPP_STG_EXTRACTION_LOOKUP: True,
}
