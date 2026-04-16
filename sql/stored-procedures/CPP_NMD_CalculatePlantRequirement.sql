USE [RIL.AOP]
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

-- =============================================
-- Author:      CPP Team
-- Create date: 2026-03-18
-- Description: Calculate and Save Plant Requirement Data
-- Version:     2.0 - Complete implementation with check/calculate/save logic
-- =============================================
CREATE OR ALTER PROCEDURE [dbo].[CPP_NMD_CalculatePlantRequirementv3]
(
    @FinancialYear VARCHAR(10),  -- Format: '2026-27'
    @CPPId UNIQUEIDENTIFIER      -- CPP Plant ID (e.g., NMD - Utility/Power Dist)
)
AS
BEGIN
    SET NOCOUNT ON;
    
    PRINT 'Starting calculation for financial year: ' + @FinancialYear;
    PRINT '======================================';
    
    -- =============================================
    -- Step 1: Fetch Associated Process Plants
    -- =============================================
    DECLARE @ProcessPlants TABLE
    (
        ProcessPlant_FK_Id UNIQUEIDENTIFIER,
        PlantCode NVARCHAR(100),
        PlantName NVARCHAR(200)
    );

    -- Use ROW_NUMBER to pick only one plant per PlantCode (prioritize LLDPE1 over LLDPE2, etc.)
    ;WITH RankedPlants AS
    (
        SELECT DISTINCT
            pcm.Plant_FK_Id,
            p.PlantCode,
            p.DisplayName AS PlantName,
            ROW_NUMBER() OVER (
                PARTITION BY p.PlantCode 
                ORDER BY p.DisplayName ASC
            ) AS RowNum
        FROM PowerConsumptionPlantMapping pcm
        INNER JOIN Plants p ON p.Id = pcm.Plant_FK_Id
        WHERE pcm.Consumption_FK_Id = @CPPId
          AND pcm.IsProcessPlant = 1
    )
    INSERT INTO @ProcessPlants (ProcessPlant_FK_Id, PlantCode, PlantName)
    SELECT 
        Plant_FK_Id,
        PlantCode,
        PlantName
    FROM RankedPlants
    WHERE RowNum = 1;

    DECLARE @ProcessPlantCount INT;
    SELECT @ProcessPlantCount = COUNT(*) FROM @ProcessPlants;
    PRINT 'Step 1: Process Plants Found: ' + CAST(@ProcessPlantCount AS VARCHAR(10));

    -- =============================================
    -- Step 2: Fetch Production Norms (Aggregated by Plant)
    -- =============================================
    DECLARE @ProductionNorms TABLE
    (
        Plant_FK_Id UNIQUEIDENTIFIER,
        PlantCode NVARCHAR(100),
        PlantName NVARCHAR(200),
        Total_April_Production DECIMAL(18,6),
        Total_May_Production DECIMAL(18,6),
        Total_June_Production DECIMAL(18,6),
        Total_July_Production DECIMAL(18,6),
        Total_Aug_Production DECIMAL(18,6),
        Total_Sep_Production DECIMAL(18,6),
        Total_Oct_Production DECIMAL(18,6),
        Total_Nov_Production DECIMAL(18,6),
        Total_Dec_Production DECIMAL(18,6),
        Total_Jan_Production DECIMAL(18,6),
        Total_Feb_Production DECIMAL(18,6),
        Total_March_Production DECIMAL(18,6)
    );

    INSERT INTO @ProductionNorms
    SELECT 
        pp.ProcessPlant_FK_Id,
        pp.PlantCode,
        pp.PlantName,
        SUM(A.April) AS Total_April_Production,
        SUM(A.May) AS Total_May_Production,
        SUM(A.June) AS Total_June_Production,
        SUM(A.July) AS Total_July_Production,
        SUM(A.Aug) AS Total_Aug_Production,
        SUM(A.Sep) AS Total_Sep_Production,
        SUM(A.Oct) AS Total_Oct_Production,
        SUM(A.Nov) AS Total_Nov_Production,
        SUM(A.Dec) AS Total_Dec_Production,
        SUM(A.Jan) AS Total_Jan_Production,
        SUM(A.Feb) AS Total_Feb_Production,
        SUM(A.March) AS Total_March_Production
    FROM @ProcessPlants pp
    INNER JOIN AOP A ON A.Plant_FK_Id = pp.ProcessPlant_FK_Id
    INNER JOIN NormParameters B ON A.Material_FK_Id = B.Id
    INNER JOIN NormParameterType C ON B.NormParameterType_FK_Id = C.Id
    WHERE A.AOPYear = @FinancialYear
      AND C.Name = 'Production'
      AND B.IsVisible = 1
      AND A.AOPRemarks IS NOT NULL
    GROUP BY pp.ProcessPlant_FK_Id, pp.PlantCode, pp.PlantName;

    DECLARE @ProductionNormCount INT;
    SELECT @ProductionNormCount = COUNT(*) FROM @ProductionNorms;
    PRINT 'Step 2: Production Norms Fetched for ' + CAST(@ProductionNormCount AS VARCHAR(10)) + ' plants';

    -- =============================================
    -- Step 3: Define Required Utilities (Filter List)
    -- =============================================
    DECLARE @RequiredUtilities TABLE (UtilityName NVARCHAR(200));
    INSERT INTO @RequiredUtilities VALUES 
        ('COMPRESSED AIR'),
        ('Cooling Water 2'),
        ('Cooling Water 1'),
        ('D M Water'),
        ('HP Steam_Dis'),
        ('LP Steam_Dis'),
        ('MP Steam_Dis'),
        ('SHP Steam_Dis'),
        ('Nitrogen Gas'),
        ('Oxygen'),
        ('Power_Dis'),
        ('Water');

    -- =============================================
    -- Step 4: Fetch Actual Consumption Materials per Plant from NormParameters
    -- =============================================
    -- Get all consumption materials configured for each plant, filtered by required utilities
    DECLARE @PlantConsumptionMaterials TABLE
    (
        Plant_FK_Id UNIQUEIDENTIFIER,
        PlantCode NVARCHAR(100),
        PlantName NVARCHAR(200),
        Material_FK_Id UNIQUEIDENTIFIER,
        UtilityName NVARCHAR(200),
        SAPMaterialCode NVARCHAR(100),
        UOM NVARCHAR(50)
    );

    -- Use ROW_NUMBER to pick only ONE material per plant-utility combination
    ;WITH RankedMaterials AS
    (
        SELECT 
            pp.ProcessPlant_FK_Id,
            pp.PlantCode,
            pp.PlantName,
            np.Id AS Material_FK_Id,
            np.DisplayName AS UtilityName,
            np.SAPMaterialCode,
            np.UOM,
            ROW_NUMBER() OVER (
                PARTITION BY pp.ProcessPlant_FK_Id, np.DisplayName
                ORDER BY 
                    CASE WHEN np.SAPMaterialCode IS NOT NULL THEN 1 ELSE 2 END,
                    np.Id
            ) AS RowNum
        FROM @ProcessPlants pp
        INNER JOIN NormParameters np 
            ON np.Plant_FK_Id = pp.ProcessPlant_FK_Id
        INNER JOIN @RequiredUtilities ru 
            ON np.DisplayName = ru.UtilityName
        WHERE np.IsVisible = 1
            AND np.NormType_FK_Id = 2  -- Consumption type
    )
    INSERT INTO @PlantConsumptionMaterials
    SELECT 
        ProcessPlant_FK_Id,
        PlantCode,
        PlantName,
        Material_FK_Id,
        UtilityName,
        SAPMaterialCode,
        UOM
    FROM RankedMaterials
    WHERE RowNum = 1;

    DECLARE @PlantMaterialCount INT;
    SELECT @PlantMaterialCount = COUNT(*) FROM @PlantConsumptionMaterials;
    PRINT 'Step 4a: Plant Consumption Materials Fetched: ' + CAST(@PlantMaterialCount AS VARCHAR(10)) + ' records';

    -- =============================================
    -- Step 5: Fetch Consumption Norms (with COALESCE logic)
    -- =============================================
    DECLARE @ConsumptionNorms TABLE
    (
        Plant_FK_Id UNIQUEIDENTIFIER,
        PlantCode NVARCHAR(100),
        PlantName NVARCHAR(200),
        Material_FK_Id UNIQUEIDENTIFIER,
        UtilityName NVARCHAR(200),
        SAPMaterialCode NVARCHAR(100),
        UOM NVARCHAR(50),
        SourceTable NVARCHAR(50),
        April DECIMAL(18,6),
        May DECIMAL(18,6),
        June DECIMAL(18,6),
        July DECIMAL(18,6),
        Aug DECIMAL(18,6),
        Sep DECIMAL(18,6),
        Oct DECIMAL(18,6),
        Nov DECIMAL(18,6),
        Dec DECIMAL(18,6),
        Jan DECIMAL(18,6),
        Feb DECIMAL(18,6),
        March DECIMAL(18,6)
    );

    -- Fetch consumption norms with priority: AOPConsumptionNorm > AOPConsumptionNormGrade
    -- Only for materials that exist in NormParameters for each plant
    ;WITH RankedNorms AS
    (
        SELECT 
            pcm.Plant_FK_Id,
            pcm.PlantCode,
            pcm.PlantName,
            pcm.Material_FK_Id,
            pcm.UtilityName,
            pcm.SAPMaterialCode,
            pcm.UOM,
            CASE 
                WHEN cn.Id IS NOT NULL THEN 'AOPConsumptionNorm'
                WHEN cg.Id IS NOT NULL THEN 'AOPConsumptionNormGrade'
                ELSE 'NotFound'
            END AS SourceTable,
            COALESCE(cn.April, cg.April, 0) AS April,
            COALESCE(cn.May, cg.May, 0) AS May,
            COALESCE(cn.June, cg.June, 0) AS June,
            COALESCE(cn.July, cg.July, 0) AS July,
            COALESCE(cn.Aug, cg.Aug, 0) AS Aug,
            COALESCE(cn.Sep, cg.Sep, 0) AS Sep,
            COALESCE(cn.Oct, cg.Oct, 0) AS Oct,
            COALESCE(cn.Nov, cg.Nov, 0) AS Nov,
            COALESCE(cn.Dec, cg.Dec, 0) AS Dec,
            COALESCE(cn.Jan, cg.Jan, 0) AS Jan,
            COALESCE(cn.Feb, cg.Feb, 0) AS Feb,
            COALESCE(cn.March, cg.March, 0) AS March,
            ROW_NUMBER() OVER (
                PARTITION BY pcm.Plant_FK_Id, pcm.Material_FK_Id 
                ORDER BY 
                    CASE WHEN cn.Id IS NOT NULL THEN 1 
                         WHEN cg.Id IS NOT NULL THEN 2 
                         ELSE 3 END
            ) AS RowNum
        FROM @PlantConsumptionMaterials pcm
        LEFT JOIN AOPConsumptionNorm cn 
            ON cn.Plant_FK_Id = pcm.Plant_FK_Id
            AND cn.Material_FK_Id = pcm.Material_FK_Id
            AND cn.AOPYear = @FinancialYear
        LEFT JOIN AOPConsumptionNormGrade cg
            ON cg.Plant_FK_Id = pcm.Plant_FK_Id
            AND cg.Material_FK_Id = pcm.Material_FK_Id
            AND cg.AOPYear = @FinancialYear
    )
    INSERT INTO @ConsumptionNorms
    SELECT 
        Plant_FK_Id,
        PlantCode,
        PlantName,
        Material_FK_Id,
        UtilityName,
        SAPMaterialCode,
        UOM,
        SourceTable,
        April, May, June, July, Aug, Sep, Oct, Nov, Dec, Jan, Feb, March
    FROM RankedNorms
    WHERE RowNum = 1;

    DECLARE @ConsumptionNormCount INT;
    SELECT @ConsumptionNormCount = COUNT(*) FROM @ConsumptionNorms;
    PRINT 'Step 4: Consumption Norms Fetched: ' + CAST(@ConsumptionNormCount AS VARCHAR(10)) + ' records';

    -- =============================================
    -- Step 5: Calculate Plant Requirement (Consumption Quantities)
    -- =============================================
    DECLARE @CalculatedConsumption TABLE
    (
        Plant_FK_Id UNIQUEIDENTIFIER,
        PlantCode NVARCHAR(100),
        PlantName NVARCHAR(200),
        Utility_FK_Id UNIQUEIDENTIFIER,
        UtilityName NVARCHAR(200),
        UtilityCode NVARCHAR(100),
        UtilityUOM NVARCHAR(50),
        SourceTable NVARCHAR(50),
        Apr_Consumption DECIMAL(18,6),
        May_Consumption DECIMAL(18,6),
        Jun_Consumption DECIMAL(18,6),
        Jul_Consumption DECIMAL(18,6),
        Aug_Consumption DECIMAL(18,6),
        Sep_Consumption DECIMAL(18,6),
        Oct_Consumption DECIMAL(18,6),
        Nov_Consumption DECIMAL(18,6),
        Dec_Consumption DECIMAL(18,6),
        Jan_Consumption DECIMAL(18,6),
        Feb_Consumption DECIMAL(18,6),
        Mar_Consumption DECIMAL(18,6)
    );

    -- Use CROSS JOIN to ensure all plants appear with all utilities, even if no production data
    INSERT INTO @CalculatedConsumption
    SELECT 
        pp.ProcessPlant_FK_Id AS Plant_FK_Id,
        pp.PlantCode,
        pp.PlantName,
        pcm.Material_FK_Id AS Utility_FK_Id,
        pcm.UtilityName,
        pcm.SAPMaterialCode AS UtilityCode,
        pcm.UOM AS UtilityUOM,
        COALESCE(cn.SourceTable, 'NotFound') AS SourceTable,
        
        -- Monthly Calculations: Production × Norm (default to 0 if no production data)
        (COALESCE(pn.Total_April_Production, 0) * COALESCE(cn.April, 0)) AS Apr_Consumption,
        (COALESCE(pn.Total_May_Production, 0) * COALESCE(cn.May, 0)) AS May_Consumption,
        (COALESCE(pn.Total_June_Production, 0) * COALESCE(cn.June, 0)) AS Jun_Consumption,
        (COALESCE(pn.Total_July_Production, 0) * COALESCE(cn.July, 0)) AS Jul_Consumption,
        (COALESCE(pn.Total_Aug_Production, 0) * COALESCE(cn.Aug, 0)) AS Aug_Consumption,
        (COALESCE(pn.Total_Sep_Production, 0) * COALESCE(cn.Sep, 0)) AS Sep_Consumption,
        (COALESCE(pn.Total_Oct_Production, 0) * COALESCE(cn.Oct, 0)) AS Oct_Consumption,
        (COALESCE(pn.Total_Nov_Production, 0) * COALESCE(cn.Nov, 0)) AS Nov_Consumption,
        (COALESCE(pn.Total_Dec_Production, 0) * COALESCE(cn.Dec, 0)) AS Dec_Consumption,
        (COALESCE(pn.Total_Jan_Production, 0) * COALESCE(cn.Jan, 0)) AS Jan_Consumption,
        (COALESCE(pn.Total_Feb_Production, 0) * COALESCE(cn.Feb, 0)) AS Feb_Consumption,
        (COALESCE(pn.Total_March_Production, 0) * COALESCE(cn.March, 0)) AS Mar_Consumption
        
    FROM @ProcessPlants pp
    CROSS JOIN @PlantConsumptionMaterials pcm
    LEFT JOIN @ProductionNorms pn
        ON pn.Plant_FK_Id = pp.ProcessPlant_FK_Id
    LEFT JOIN @ConsumptionNorms cn
        ON cn.Plant_FK_Id = pp.ProcessPlant_FK_Id
        AND cn.Material_FK_Id = pcm.Material_FK_Id
    WHERE pcm.Plant_FK_Id = pp.ProcessPlant_FK_Id;

    DECLARE @CalculatedConsumptionCount INT;
    SELECT @CalculatedConsumptionCount = COUNT(*) FROM @CalculatedConsumption;
    PRINT 'Step 5: Consumption Calculated: ' + CAST(@CalculatedConsumptionCount AS VARCHAR(10)) + ' records';

    -- =============================================
    -- Step 6: Get CPP Plant Details
    -- =============================================
    DECLARE @CPPPlantName NVARCHAR(200);
    DECLARE @CPPPlantCode NVARCHAR(100);
    
    SELECT 
        @CPPPlantName = DisplayName,
        @CPPPlantCode = PlantCode
    FROM Plants
    WHERE Id = @CPPId;

    -- =============================================
    -- Step 7: Check if data already exists for this financial year
    -- =============================================
    DECLARE @ExistingRecordCount INT;
    SELECT @ExistingRecordCount = COUNT(*)
    FROM CalculatedProcessDemand
    WHERE financial_year = @FinancialYear;

    IF @ExistingRecordCount > 0
    BEGIN
        PRINT 'Data already exists for financial year ' + @FinancialYear;
        PRINT 'Existing records: ' + CAST(@ExistingRecordCount AS VARCHAR(10));
        PRINT 'Returning existing data...';
        PRINT '======================================';
        
        -- Return existing data
        SELECT 
            financial_year,
            process_plant,
            process_plant_id,
            cpp_utility,
            cpp_utility_id,
            cpp_plant,
            cpp_plant_id,
            uom,
            apr, may, jun, jul, aug, sep, oct, nov, dec, jan, feb, mar,
            (apr + may + jun + jul + aug + sep + oct + nov + dec + jan + feb + mar) AS Total_Annual_Consumption,
            created_at,
            updated_at,
            remarks
        FROM CalculatedProcessDemand
        WHERE financial_year = @FinancialYear
        ORDER BY process_plant_id, cpp_utility;
        
        RETURN;
    END

    -- =============================================
    -- Step 8: Get Previous Year's Data Structure
    -- =============================================
    DECLARE @PreviousYear VARCHAR(10);
    DECLARE @CurrentYearStart INT = CAST(LEFT(@FinancialYear, 4) AS INT);
    DECLARE @CurrentYearEnd INT = CAST(RIGHT(@FinancialYear, 2) AS INT);
    SET @PreviousYear = CAST(@CurrentYearStart - 1 AS VARCHAR(4)) + '-' + RIGHT('0' + CAST(@CurrentYearEnd - 1 AS VARCHAR(2)), 2);

    PRINT 'Step 6: Fetching previous year structure from: ' + @PreviousYear;

    DECLARE @PreviousYearData TABLE
    (
        OldId UNIQUEIDENTIFIER,
        process_plant NVARCHAR(200),
        process_plant_id NVARCHAR(100),
        cpp_utility NVARCHAR(200),
        cpp_utility_id NVARCHAR(100),
        cpp_plant NVARCHAR(200),
        cpp_plant_id NVARCHAR(100),
        uom NVARCHAR(50),
        remarks NVARCHAR(MAX)
    );

    INSERT INTO @PreviousYearData
    SELECT 
        id,
        process_plant,
        process_plant_id,
        cpp_utility,
        cpp_utility_id,
        cpp_plant,
        cpp_plant_id,
        uom,
        remarks
    FROM CalculatedProcessDemand
    WHERE financial_year = @PreviousYear;

    DECLARE @PreviousYearCount INT;
    SELECT @PreviousYearCount = COUNT(*) FROM @PreviousYearData;
    PRINT 'Previous year records found: ' + CAST(@PreviousYearCount AS VARCHAR(10));

    -- =============================================
    -- Step 9: Merge Previous Year Structure with Calculated Data
    -- =============================================
    DECLARE @FinalData TABLE
    (
        id UNIQUEIDENTIFIER,
        financial_year VARCHAR(10),
        process_plant NVARCHAR(200),
        process_plant_id NVARCHAR(100),
        cpp_utility NVARCHAR(200),
        cpp_utility_id NVARCHAR(100),
        cpp_plant NVARCHAR(200),
        cpp_plant_id NVARCHAR(100),
        uom NVARCHAR(50),
        apr DECIMAL(18,6),
        may DECIMAL(18,6),
        jun DECIMAL(18,6),
        jul DECIMAL(18,6),
        aug DECIMAL(18,6),
        sep DECIMAL(18,6),
        oct DECIMAL(18,6),
        nov DECIMAL(18,6),
        dec DECIMAL(18,6),
        jan DECIMAL(18,6),
        feb DECIMAL(18,6),
        mar DECIMAL(18,6),
        created_at DATETIME,
        updated_at DATETIME,
        remarks NVARCHAR(MAX)
    );

    -- Start from previous year data to ensure all plants/utilities are included
    INSERT INTO @FinalData
    SELECT 
        NEWID() AS id,
        @FinancialYear AS financial_year,
        pyd.process_plant,
        pyd.process_plant_id,
        pyd.cpp_utility,
        pyd.cpp_utility_id,
        pyd.cpp_plant,
        pyd.cpp_plant_id,
        pyd.uom,
        COALESCE(cc.Apr_Consumption, 0) AS apr,
        COALESCE(cc.May_Consumption, 0) AS may,
        COALESCE(cc.Jun_Consumption, 0) AS jun,
        COALESCE(cc.Jul_Consumption, 0) AS jul,
        COALESCE(cc.Aug_Consumption, 0) AS aug,
        COALESCE(cc.Sep_Consumption, 0) AS sep,
        COALESCE(cc.Oct_Consumption, 0) AS oct,
        COALESCE(cc.Nov_Consumption, 0) AS nov,
        COALESCE(cc.Dec_Consumption, 0) AS dec,
        COALESCE(cc.Jan_Consumption, 0) AS jan,
        COALESCE(cc.Feb_Consumption, 0) AS feb,
        COALESCE(cc.Mar_Consumption, 0) AS mar,
        GETDATE() AS created_at,
        GETDATE() AS updated_at,
        pyd.remarks
    FROM @PreviousYearData pyd
    LEFT JOIN @CalculatedConsumption cc
        ON cc.PlantCode = pyd.process_plant_id
        AND cc.UtilityCode = pyd.cpp_utility_id;

    DECLARE @FinalDataCount INT;
    SELECT @FinalDataCount = COUNT(*) FROM @FinalData;
    PRINT 'Step 7: Final data prepared: ' + CAST(@FinalDataCount AS VARCHAR(10)) + ' records';

    -- =============================================
    -- Step 10: Save to CalculatedProcessDemand
    -- =============================================
    INSERT INTO CalculatedProcessDemand
    (
        id, financial_year, process_plant, process_plant_id, 
        cpp_utility, cpp_utility_id, cpp_plant, cpp_plant_id,
        uom, apr, may, jun, jul, aug, sep, oct, nov, dec, jan, feb, mar,
        created_at, updated_at, remarks
    )
    SELECT 
        id, financial_year, process_plant, process_plant_id,
        cpp_utility, cpp_utility_id, cpp_plant, cpp_plant_id,
        uom, apr, may, jun, jul, aug, sep, oct, nov, dec, jan, feb, mar,
        created_at, updated_at, remarks
    FROM @FinalData;

    DECLARE @SavedRecordCount INT = @@ROWCOUNT;
    PRINT 'Step 8: Saved to CalculatedProcessDemand: ' + CAST(@SavedRecordCount AS VARCHAR(10)) + ' records';

    -- =============================================
    -- Step 11: Return Saved Results for Verification
    -- =============================================
    SELECT 
        financial_year,
        process_plant,
        process_plant_id,
        cpp_utility,
        cpp_utility_id,
        cpp_plant,
        cpp_plant_id,
        uom,
        apr, may, jun, jul, aug, sep, oct, nov, dec, jan, feb, mar,
        (apr + may + jun + jul + aug + sep + oct + nov + dec + jan + feb + mar) AS Total_Annual_Consumption,
        created_at,
        updated_at,
        remarks
    FROM @FinalData
    ORDER BY process_plant_id, cpp_utility;

    PRINT '======================================';
    PRINT 'Calculation and save completed successfully!';
    PRINT 'Total records saved: ' + CAST(@SavedRecordCount AS VARCHAR(10));
    PRINT 'Total records calculated: ' + CAST(@CalculatedConsumptionCount AS VARCHAR(10));

END;
GO
