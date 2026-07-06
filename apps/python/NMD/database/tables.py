class T:
    """Table and column names used by NMD queries.py.

    NMD uses a different schema from JMD:
      - Power assets: PowerGenerationAssets filtered by AssetType (no CPPPLANT_FK_Id filter)
      - Operational hours: OperationalHours table (one row per month via FinancialMonthId)
      - Asset availability: AssetAvailability single table (Priority + Min/Max + FixedMin/Max)
      - Steam assets: SteamGenerationAssets with inline capacity columns + LinkedPowerAssetId
      - Fixed consumption: UtilityFixedConsumption (FinancialYearMonth-based)
      - Process demand: CalculatedProcessDemand via CPP_NMD_GetProcessDemandByYear SP
    """

    # --- Core lookup tables ---
    PLANTS                      = "Plants"
    FINANCIAL_YEAR_MONTH        = "FinancialYearMonth"
    NORM_PARAMETERS             = "NormParameters"

    # --- Process demand ---
    PROCESS_DEMAND_MASTER       = "ProcessDemandMaster"
    CALCULATED_PROCESS_DEMAND   = "CalculatedProcessDemand"
    PROCESS_DEMAND_SP           = "CPP_NMD_GetProcessDemandByYear"

    # --- Fixed consumption ---
    UTILITY_FIXED_CONSUMPTION   = "UtilityFixedConsumption"
    UFC_FYM_FK                  = "FinancialYearMonth_FK_Id"
    UFC_NORM_PARAM_FK           = "NormParameter_FK_Id"
    UFC_COST_CENTER_FK          = "CostCenter_FK_Id"
    UFC_VALUE_COL               = "ConsumptionValue"
    CPP_COST_CENTERS            = "CPPCostCenters"
    FIXED_CONSUMPTION_PLANT_MAP = "FixedConsumptionPlantMapping"

    # --- Power generation assets ---
    POWER_GENERATION_ASSETS     = "PowerGenerationAssets"
    PGA_ASSET_ID                = "AssetId"
    PGA_ASSET_NAME              = "AssetName"
    PGA_ASSET_TYPE              = "AssetType"
    PGA_PLANT_CODE              = "PlantCode"
    PGA_CPP_PLANT_FK            = "CPPPLANT_FK_Id"

    # --- Operational hours (one row per asset per month) ---
    OPERATIONAL_HOURS           = "OperationalHours"
    OH_ASSET_FK                 = "Asset_FK_Id"
    OH_FYM_FK                   = "FinancialMonthId"
    OH_HOURS_COL                = "OperationalHours"

    # --- Asset availability (Priority + Min/Max capacity, one row per asset per month) ---
    ASSET_AVAILABILITY          = "AssetAvailability"
    AA_ASSET_ID                 = "AssetId"
    AA_FYM_FK                   = "FinancialYearMonthId"
    AA_PRIORITY                 = "Priority"
    AA_MIN_OP_CAPACITY          = "MinOperatingCapacity"
    AA_MAX_OP_CAPACITY          = "MaxOperatingCapacity"
    AA_FIXED_MIN                = "FixedMin"
    AA_FIXED_MAX                = "FixedMax"
    AA_IS_AVAILABLE             = "isAssetAvailable"
    AA_OP_HOURS                 = "operationalHours"

    # --- Steam generation assets (inline capacity + GT linkage) ---
    STEAM_GENERATION_ASSETS     = "SteamGenerationAssets"
    SGA_ASSET_ID                = "AssetId"
    SGA_ASSET_NAME              = "AssetName"
    SGA_ASSET_TYPE              = "AssetType"
    SGA_MIN_CAPACITY_MT         = "MinCapacityMT"
    SGA_MAX_CAPACITY_MT         = "MaxCapacityMT"
    SGA_STEAM_TYPE              = "SteamType"
    SGA_LINKED_POWER_ASSET_ID   = "LinkedPowerAssetId"
    SGA_PRIORITY                = "Priority"
    SGA_EFFICIENCY              = "Efficiency"
    SGA_IS_ALWAYS_AVAILABLE     = "IsAlwaysAvailable"

    # --- Import power (multi-source, same as JMD) ---
    CPP_IMPORT_POWER_SOURCE_MAPPING   = "CPPImportPowerSourceMapping"
    CPP_IMPORT_POWER_CAPACITY         = "CPPImportPowerCapacity"
    CPP_IMPORT_POWER_OPERATIONAL_HOURS= "CPPImportPowerOperationalHours"
    IMPORT_PLANT_FK             = "CPPPlant_FK_Id"
    IMPORT_SOURCE_FK            = "ImportPowerSource_FK_Id"
    IMPORT_FINANCIAL_YEAR       = "FinancialYear"

    # --- Norms (same as JMD) ---
    NORMS_HEADER                = "NormsHeader"
    NORMS_MONTH_DETAIL          = "NormsMonthDetail"
    NORMS_HEADER_PLANT_FK       = "Plant_FK_Id"

    # --- Heat rate tables (same as JMD) ---
    CPP_GT_HEAT_RATE            = "CPP_GTHeatRate"
    GT_HR_ASSET_NAME            = "AssetName"
    GT_HR_LOAD_COL              = "GTLoad"
    GT_HR_VALUE_COL             = "FinalHeatRate"
    GT_HR_FREE_STEAM_FACTOR     = "FreeSteamFactor"
    GT_HR_FINANCIAL_YEAR        = "FinancialYear"

    CPP_HRSG_HEAT_RATE          = "CPP_HRSGHeatRate"
    HRSG_HR_ASSET_FK            = "Asset_FK_Id"
    HRSG_HR_LOAD_COL            = "HRSGLoad"
    HRSG_HR_VALUE_COL           = "FinalHeatRate"
    HRSG_HR_FINANCIAL_YEAR      = "FinancialYear"
