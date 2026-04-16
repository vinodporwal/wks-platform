USE [RIL.AOP]
GO
/****** 
Object:  StoredProcedure [dbo].[CPP_FixedUtilityCalculatedNorms]
Description: Updated to include Catalyst & Chemical and Raw Material mappings for 40NF plant
Modified: Added Catalyst & Chemical and Raw Material input materials for norms calculation
******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
ALTER PROCEDURE [dbo].[CPP_FixedUtilityCalculatedNormsv2]
    @FinancialYear NVARCHAR(10),  -- Format: '2025-26'
    @FromDate DATE,                -- Format: 'YYYY-MM-DD'
    @ToDate DATE                   -- Format: 'YYYY-MM-DD'
AS
BEGIN
    SET NOCOUNT ON;

    -- Convert dates to posting_month format (YYYY-MM)
    DECLARE @FromMonth NVARCHAR(7) = FORMAT(@FromDate, 'yyyy-MM');
    DECLARE @ToMonth NVARCHAR(7) = FORMAT(@ToDate, 'yyyy-MM');

    -- Delete existing records for this financial year before inserting new data
    DELETE FROM [dbo].[CPP_utilitiesCalculatednorms]
    WHERE FinancialYear = @FinancialYear;

    -- Plant-Material Mapping CTE
    -- Define exact Product -> Input Material mapping based on norms structure
    -- UPDATED: Added Catalyst & Chemical and Raw Material mappings for 40NF plant
    WITH PlantProductMaterialMapping AS (
        SELECT Plant_Name, Product_Material, Input_Material FROM (VALUES
            -- 40NB - Power Plant 1 (Produces POWERGEN)
            ('40NB', 'POWERGEN', 'COMPRESSED AIR'),
            ('40NB', 'POWERGEN', 'Cooling Water 2'),
            ('40NB', 'POWERGEN', 'Power_Dis'),
           
            -- 40NC - Power Plant 2 (Produces POWERGEN)
            ('40NC', 'POWERGEN', 'COMPRESSED AIR'),
            ('40NC', 'POWERGEN', 'Cooling Water 2'),
            ('40NC', 'POWERGEN', 'Power_Dis'),
           
            -- 40ND - Power Plant 3 (Produces POWERGEN)
            ('40ND', 'POWERGEN', 'COMPRESSED AIR'),
            ('40ND', 'POWERGEN', 'Cooling Water 2'),
            ('40ND', 'POWERGEN', 'Power_Dis'),
           
            -- 40NE - STG Power Plant (Produces POWERGEN)
            ('40NE', 'POWERGEN', 'COMPRESSED AIR'),
            ('40NE', 'POWERGEN', 'Cooling Water 2'),
            ('40NE', 'POWERGEN', 'Power_Dis'),
            ('40NE', 'POWERGEN', 'SHP Steam_Dis'),
           
            -- ========================================================================
            -- 40NF - Utility Plant (Multiple Products)
            -- ========================================================================
            
            -- Boiler Feed Water (310027927)
            -- Utilities
            ('40NF', 'Boiler Feed Water', 'Cooling Water 2'),
            ('40NF', 'Boiler Feed Water', 'D M Water'),
            ('40NF', 'Boiler Feed Water', 'LP Steam_Dis'),
            ('40NF', 'Boiler Feed Water', 'Power_Dis'),
            -- Catalyst & Chemical (NEW) - Updated to match NormsHeader exact names
            ('40NF', 'Boiler Feed Water', 'CHEM CYCLO HEXY'),
            ('40NF', 'Boiler Feed Water', 'CHEM MORPHOLENE'),
            ('40NF', 'Boiler Feed Water', 'KEM WATREAT B 70M'),
           
            -- COMPRESSED AIR
            -- Utilities
            ('40NF', 'COMPRESSED AIR', 'Cooling Water 2'),
            ('40NF', 'COMPRESSED AIR', 'Power_Dis'),
           
            -- Cooling Water 1
            -- Utilities
            ('40NF', 'Cooling Water 1', 'COMPRESSED AIR'),
            ('40NF', 'Cooling Water 1', 'Power_Dis'),
            -- Raw Material (NEW) - Added from NormsHeader
            ('40NF', 'Cooling Water 1', 'SULPHURIC ACID'),
            ('40NF', 'Cooling Water 1', 'Water'),
           
            -- Cooling Water 2
            -- Utilities
            ('40NF', 'Cooling Water 2', 'COMPRESSED AIR'),
            ('40NF', 'Cooling Water 2', 'Power_Dis'),
            -- Raw Material (NEW) - Added from NormsHeader
            ('40NF', 'Cooling Water 2', 'SULPHURIC ACID'),
            ('40NF', 'Cooling Water 2', 'Water'),
           
            -- D M Water (310027966)
            -- Utilities
            ('40NF', 'D M Water', 'COMPRESSED AIR'),
            ('40NF', 'D M Water', 'Power_Dis'),
            ('40NF', 'D M Water', 'Ret steam condensate'),
            -- Catalyst & Chemical (NEW) - Updated to match NormsHeader exact names
            ('40NF', 'D M Water', 'CAUSTIC SODA LYE – GRADE 1'),
            ('40NF', 'D M Water', 'CHEM ALUM.SULFATE, AL2(SO4)3,18H2O'),
            ('40NF', 'D M Water', 'CHEM  SODIUM SULPHITE;PN:MIS 19OX'),
            ('40NF', 'D M Water', 'POLYELECTROLYTE'),
            ('40NF', 'D M Water', 'SODIUM CHLORIDE IS 797 GRADE1'),
            -- Raw Material (NEW) - Updated to match NormsHeader exact names
            ('40NF', 'D M Water', 'HYDRO CHLORIC ACID (30%) -VIRGIN'),
            ('40NF', 'D M Water', 'Water'),
           
            -- Effluent Treated
            -- Utilities
            ('40NF', 'Effluent Treated', 'Power_Dis'),
            -- Raw Material (NEW) - Added from NormsHeader
            ('40NF', 'Effluent Treated', 'UREA,NITROGEN CONTENT 46%'),
            ('40NF', 'Effluent Treated', 'Water'),
           
            -- HP Steam PRDS
            -- Utilities
            ('40NF', 'HP Steam PRDS', 'Boiler Feed Water'),
            ('40NF', 'HP Steam PRDS', 'SHP Steam_Dis'),
           
            -- HRSG1_SHP STEAM (310027926)
            -- Utilities
            ('40NF', 'HRSG1_SHP STEAM', 'Boiler Feed Water'),
            ('40NF', 'HRSG1_SHP STEAM', 'COMPRESSED AIR'),
            ('40NF', 'HRSG1_SHP STEAM', 'LP Steam_Dis'),
            -- Catalyst & Chemical (NEW) - Updated to match NormsHeader exact names
            ('40NF', 'HRSG1_SHP STEAM', 'CHEM TRISODIUM PHOSPHATE'),
            -- Raw Material (NEW) - Updated to match NormsHeader exact names
            ('40NF', 'HRSG1_SHP STEAM', 'FURNACE OIL ( MEDIUM VISCOSITY GRADE )'),
            ('40NF', 'HRSG1_SHP STEAM', 'NATURAL GAS'),
            ('40NF', 'HRSG1_SHP STEAM', 'Water'),
           
            -- HRSG2_SHP STEAM
            -- Utilities
            ('40NF', 'HRSG2_SHP STEAM', 'Boiler Feed Water'),
            ('40NF', 'HRSG2_SHP STEAM', 'COMPRESSED AIR'),
            ('40NF', 'HRSG2_SHP STEAM', 'LP Steam_Dis'),
            -- Catalyst & Chemical (NEW) - Updated to match NormsHeader exact names
            ('40NF', 'HRSG2_SHP STEAM', 'CHEM TRISODIUM PHOSPHATE'),
            -- Raw Material (NEW) - Updated to match NormsHeader exact names
            ('40NF', 'HRSG2_SHP STEAM', 'FURNACE OIL ( MEDIUM VISCOSITY GRADE )'),
            ('40NF', 'HRSG2_SHP STEAM', 'NATURAL GAS'),
            ('40NF', 'HRSG2_SHP STEAM', 'Water'),
           
            -- HRSG3_SHP STEAM
            -- Utilities
            ('40NF', 'HRSG3_SHP STEAM', 'Boiler Feed Water'),
            ('40NF', 'HRSG3_SHP STEAM', 'COMPRESSED AIR'),
            ('40NF', 'HRSG3_SHP STEAM', 'LP Steam_Dis'),
            -- Catalyst & Chemical (NEW) - Updated to match NormsHeader exact names
            ('40NF', 'HRSG3_SHP STEAM', 'CHEM TRISODIUM PHOSPHATE'),
            -- Raw Material (NEW) - Updated to match NormsHeader exact names
            ('40NF', 'HRSG3_SHP STEAM', 'FURNACE OIL ( MEDIUM VISCOSITY GRADE )'),
            ('40NF', 'HRSG3_SHP STEAM', 'NATURAL GAS'),
            ('40NF', 'HRSG3_SHP STEAM', 'Water'),
           
            -- LP Steam PRDS
            -- Utilities
            ('40NF', 'LP Steam PRDS', 'Boiler Feed Water'),
            ('40NF', 'LP Steam PRDS', 'MP Steam_Dis'),
           
            -- MP Steam PRDS SHP
            -- Utilities
            ('40NF', 'MP Steam PRDS SHP', 'Boiler Feed Water'),
            ('40NF', 'MP Steam PRDS SHP', 'SHP Steam_Dis'),
           
            -- Oxygen
            -- Utilities
            ('40NF', 'Oxygen', 'Cooling Water 2'),
            ('40NF', 'Oxygen', 'Power_Dis'),
            -- By Product (NEW) - Added from NormsHeader
            ('40NF', 'Oxygen', 'Nitrogen Gas'),
           
            -- STG1_LP STEAM
            -- Utilities
            ('40NF', 'STG1_LP STEAM', 'SHP Steam_Dis'),
           
            -- STG1_MP STEAM
            -- Utilities
            ('40NF', 'STG1_MP STEAM', 'SHP Steam_Dis'),
           
            -- Treated Spent Caustic
            -- Utilities
            ('40NF', 'Treated Spent Caustic', 'COMPRESSED AIR'),
            ('40NF', 'Treated Spent Caustic', 'Cooling Water 2'),
            ('40NF', 'Treated Spent Caustic', 'MP Steam_Dis'),
            ('40NF', 'Treated Spent Caustic', 'Power_Dis'),
            -- Raw Material (NEW) - Added from NormsHeader
            ('40NF', 'Treated Spent Caustic', 'SULPHURIC ACID'),
            ('40NF', 'Treated Spent Caustic', 'Water')
        ) AS M(Plant_Name, Product_Material, Input_Material)
    ),
   
    -- Consumption Data CTE
    -- Get consumption per material per process_order
    ConsumptionData AS (
        SELECT
            c.process_order,
            c.plant,
            c.posting_month,
            c.material_descp AS Input_Material,
            MAX(c.material) AS Input_Material_Code,  -- Get SAP code for input material
            SUM(TRY_CONVERT(DECIMAL(18,4), c.actual_posting)) AS Total_Actual_Posting
        FROM
            [RIL.MIIS.STG].[dbo].[STG_Tbl_Process_Order_Data] c
        WHERE
            TRY_CONVERT(DECIMAL(18,4), c.actual_posting) IS NOT NULL
            AND c.posting_month >= @FromMonth  -- Filter by date range
            AND c.posting_month <= @ToMonth
        GROUP BY
            c.process_order,
            c.plant,
            c.posting_month,
            c.material_descp
    ),
   
    -- Production Data CTE
    -- Get production quantity per process_order from Production table
    ProductionData AS (
        SELECT
            process_order,
            plant,
            posting_month,
            MAX(material) AS Output_Material_Code,
            MAX(material_descp) AS Output_Material_Descp,
            SUM(TRY_CONVERT(DECIMAL(18,4), actual_posting)) AS Total_Production_Qty
        FROM
            [RIL.MIIS.STG].[dbo].[STG_Tbl_Process_Order_Production]
        WHERE
            TRY_CONVERT(DECIMAL(18,4), actual_posting) IS NOT NULL
            AND posting_month >= @FromMonth  -- Filter by date range
            AND posting_month <= @ToMonth
        GROUP BY
            process_order,
            plant,
            posting_month
    ),
   
    -- Intermediate Join: Join consumption and production data
    IntermediateData AS (
        SELECT
            c.plant,
            c.posting_month,
            p.Output_Material_Code,
            p.Output_Material_Descp,
            c.Input_Material_Code,
            c.Input_Material,
            c.Total_Actual_Posting,
            p.Total_Production_Qty
        FROM
            ConsumptionData c
            INNER JOIN ProductionData p
                ON c.process_order = p.process_order
                AND c.plant = p.plant
                AND c.posting_month = p.posting_month
            INNER JOIN PlantProductMaterialMapping m
                ON c.plant = m.Plant_Name
                AND p.Output_Material_Descp = m.Product_Material
                AND c.Input_Material = m.Input_Material
    ),
   
    -- Calculate norms per month first
    MonthlyNorms AS (
        SELECT
            plant,
            posting_month,
            Output_Material_Code,
            Output_Material_Descp,
            Input_Material_Code,
            Input_Material,
            Total_Actual_Posting,
            Total_Production_Qty,
            CASE
                WHEN Total_Production_Qty > 0
                THEN Total_Actual_Posting / Total_Production_Qty
                ELSE NULL
            END AS Monthly_NORSM
        FROM
            IntermediateData
    )
   
    -- Insert calculated norms into the table (CTE must be used in the same statement)
    INSERT INTO [dbo].[CPP_utilitiesCalculatednorms] (
        Id,
        FinancialYear,
        Plant,
        Product_Material_Code,
        Product_Material_Name,
        Input_Material_Code,
        Input_Material_Name,
        Avg_Consumption_Qty,
        Avg_Production_Qty,
        NORSM_Value,
        CreatedDate
    )
    SELECT
        NEWID() AS Id,
        @FinancialYear AS FinancialYear,
        plant,
        Output_Material_Code AS Product_Material_Code,
        Output_Material_Descp AS Product_Material_Name,
        Input_Material_Code AS Input_Material_Code,
        Input_Material AS Input_Material_Name,
        AVG(Total_Actual_Posting) AS Avg_Consumption_Qty,
        AVG(Total_Production_Qty) AS Avg_Production_Qty,
        AVG(Monthly_NORSM) AS NORSM_Value,  -- Average the monthly norms instead
        GETDATE() AS CreatedDate
    FROM
        MonthlyNorms
    GROUP BY
        plant,
        Output_Material_Code,
        Output_Material_Descp,
        Input_Material_Code,
        Input_Material;
   
    -- Return the inserted data
    SELECT
        Id,
        FinancialYear,
        Plant,
        Product_Material_Code,
        Product_Material_Name,
        Input_Material_Code,
        Input_Material_Name,
        Avg_Consumption_Qty,
        Avg_Production_Qty,
        NORSM_Value,
        CreatedDate,
        UpdatedDate
    FROM [dbo].[CPP_utilitiesCalculatednorms]
    WHERE FinancialYear = @FinancialYear
    ORDER BY
        Plant,
        Product_Material_Name,
        Input_Material_Name;

END
GO
