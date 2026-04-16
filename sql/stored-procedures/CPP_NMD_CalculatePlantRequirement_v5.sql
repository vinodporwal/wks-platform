USE [RIL.AOP]
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

-- =============================================
-- Author:      CPP Team
-- Create date: 2026-03-21
-- Description: Calculate Plant Requirement by calling plant-specific SPs
-- Version:     5.0 - Call predefined plant SPs instead of calculating from AOP/AOPConsumption
-- =============================================
CREATE OR ALTER PROCEDURE [dbo].[CPP_NMD_CalculatePlantRequirement_v5]
(
    @FinancialYear VARCHAR(10),  -- Format: '2026-27'
    @ReturnResults BIT = 1       -- 1 = Return result set, 0 = Silent mode (for use by other SPs)
)
AS
BEGIN
    SET NOCOUNT ON;
    
    PRINT 'Starting calculation for financial year: ' + @FinancialYear;
    PRINT '======================================';
    
    -- =============================================
    -- Step 1: Check if data already exists for this financial year
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
        
        -- Return existing data only if @ReturnResults = 1
        IF @ReturnResults = 1
        BEGIN
            SELECT 
                id,
                financial_year,
                process_plant,
                process_plant_id,
                cpp_utility,
                cpp_utility_id,
                cpp_plant,
                cpp_plant_id,
                uom,
                apr, may, jun, jul, aug, sep, oct, nov, dec, jan, feb, mar,
                1 AS is_calculated,  -- Always 1 since data exists in CalculatedProcessDemand
                remarks
            FROM CalculatedProcessDemand
            WHERE financial_year = @FinancialYear
            ORDER BY process_plant, cpp_utility;
        END
        
        RETURN;
    END

    PRINT 'No existing data found. Starting calculation...';

    -- =============================================
    -- Step 2: Create temp table to store all plant consumption data
    -- =============================================
    CREATE TABLE #PlantConsumptionData
    (
        PlantName NVARCHAR(200),
        PlantId NVARCHAR(100),
        MaterialName NVARCHAR(200),
        MaterialCode NVARCHAR(100),
        UOM NVARCHAR(50),
        Apr DECIMAL(18,6),
        May DECIMAL(18,6),
        Jun DECIMAL(18,6),
        Jul DECIMAL(18,6),
        Aug DECIMAL(18,6),
        Sep DECIMAL(18,6),
        Oct DECIMAL(18,6),
        Nov DECIMAL(18,6),
        [Dec] DECIMAL(18,6),
        Jan DECIMAL(18,6),
        Feb DECIMAL(18,6),
        Mar DECIMAL(18,6),
        SourceSP NVARCHAR(100)
    );

    -- =============================================
    -- Step 3: Call Plant-Specific SPs and Insert Results
    -- =============================================
    
    -- Plant 1: EOEG
    BEGIN TRY
        PRINT 'Calling PlantConsumptionSummaryReport for EOEG...';
        
        CREATE TABLE #EOEG_Result (
            NormType NVARCHAR(200),
            Material NVARCHAR(200),
            UOM NVARCHAR(50),
            Spec NVARCHAR(50),
            April DECIMAL(18,6),
            May DECIMAL(18,6),
            June DECIMAL(18,6),
            July DECIMAL(18,6),
            August DECIMAL(18,6),
            September DECIMAL(18,6),
            October DECIMAL(18,6),
            November DECIMAL(18,6),
            December DECIMAL(18,6),
            January DECIMAL(18,6),
            February DECIMAL(18,6),
            March DECIMAL(18,6),
            Total DECIMAL(18,6),
            Id UNIQUEIDENTIFIER,
            Remarks NVARCHAR(MAX)
        );
        
        INSERT INTO #EOEG_Result
        EXEC PlantConsumptionSummaryReport 
            @PlantId = 'C5102765-E0A1-4CC6-B7A0-4F937B91EB6D', 
            @year = @FinancialYear, 
            @ReportType = 'NormQuantity';
        
        INSERT INTO #PlantConsumptionData (PlantName, PlantId, MaterialName, MaterialCode, UOM, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, [Dec], Jan, Feb, Mar, SourceSP)
        SELECT 
            'EOEG' AS PlantName,
            'EOEG' AS PlantId,
            Material,
            Material AS MaterialCode,
            UOM,
            COALESCE(April, 0),
            COALESCE(May, 0),
            COALESCE(June, 0),
            COALESCE(July, 0),
            COALESCE(August, 0),
            COALESCE(September, 0),
            COALESCE(October, 0),
            COALESCE(November, 0),
            COALESCE(December, 0),
            COALESCE(January, 0),
            COALESCE(February, 0),
            COALESCE(March, 0),
            'PlantConsumptionSummaryReport' AS SourceSP
        FROM #EOEG_Result
        WHERE NormType = 'UtilityConsumption';
        
        DROP TABLE #EOEG_Result;
        PRINT 'EOEG data fetched successfully';
    END TRY
    BEGIN CATCH
        PRINT 'ERROR: Failed to fetch EOEG data - ' + ERROR_MESSAGE();
        IF OBJECT_ID('tempdb..#EOEG_Result') IS NOT NULL DROP TABLE #EOEG_Result;
    END CATCH

    -- Plant 2: Gas Cracker
    BEGIN TRY
        PRINT 'Calling CRACKER_NMD_GetFinalNormsProductionReport for Gas Cracker...';
        
        -- Extract year parts for dynamic column names
        DECLARE @YearStart VARCHAR(2) = RIGHT(LEFT(@FinancialYear, 4), 2);
        DECLARE @YearEnd VARCHAR(2) = RIGHT(@FinancialYear, 2);
        
        CREATE TABLE #GasCracker_Result (
            [SAP MATERIAL CODE] NVARCHAR(100),
            Particular NVARCHAR(200),
            UOM NVARCHAR(50),
            Apr DECIMAL(18,6),
            May DECIMAL(18,6),
            Jun DECIMAL(18,6),
            Jul DECIMAL(18,6),
            Aug DECIMAL(18,6),
            Sep DECIMAL(18,6),
            Oct DECIMAL(18,6),
            Nov DECIMAL(18,6),
            [Dec] DECIMAL(18,6),
            Jan DECIMAL(18,6),
            Feb DECIMAL(18,6),
            Mar DECIMAL(18,6)
        );
        
        INSERT INTO #GasCracker_Result
        EXEC [dbo].[CRACKER_NMD_GetFinalNormsProductionReport] 
            @PlantId = '674ED0D4-2A8F-43D5-B454-D0CAD0AC2064', 
            @aopYear = @FinancialYear, 
            @reportType = 'Utility Consumption';
        
        INSERT INTO #PlantConsumptionData (PlantName, PlantId, MaterialName, MaterialCode, UOM, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, [Dec], Jan, Feb, Mar, SourceSP)
        SELECT 
            'Gas Cracker' AS PlantName,
            'GC' AS PlantId,
            Particular,
            [SAP MATERIAL CODE] AS MaterialCode,
            UOM,
            COALESCE(Apr, 0),
            COALESCE(May, 0),
            COALESCE(Jun, 0),
            COALESCE(Jul, 0),
            COALESCE(Aug, 0),
            COALESCE(Sep, 0),
            COALESCE(Oct, 0),
            COALESCE(Nov, 0),
            COALESCE([Dec], 0),
            COALESCE(Jan, 0),
            COALESCE(Feb, 0),
            COALESCE(Mar, 0),
            'CRACKER_NMD_GetFinalNormsProductionReport' AS SourceSP
        FROM #GasCracker_Result;
        
        DROP TABLE #GasCracker_Result;
        PRINT 'Gas Cracker data fetched successfully';
    END TRY
    BEGIN CATCH
        PRINT 'ERROR: Failed to fetch Gas Cracker data - ' + ERROR_MESSAGE();
        IF OBJECT_ID('tempdb..#GasCracker_Result') IS NOT NULL DROP TABLE #GasCracker_Result;
    END CATCH

    -- Plant 3: LDPE 40N5 (Plant 1)
    BEGIN TRY
        PRINT 'Calling PE_NMD_PlantConsumptionSummaryReport for LDPE 40N5 (Plant 1)...';
        
        CREATE TABLE #LDPE1_Result (
            NormType NVARCHAR(200),
            Material NVARCHAR(200),
            UOM NVARCHAR(50),
            Spec NVARCHAR(50),
            April DECIMAL(18,6),
            May DECIMAL(18,6),
            June DECIMAL(18,6),
            July DECIMAL(18,6),
            August DECIMAL(18,6),
            September DECIMAL(18,6),
            October DECIMAL(18,6),
            November DECIMAL(18,6),
            December DECIMAL(18,6),
            January DECIMAL(18,6),
            February DECIMAL(18,6),
            March DECIMAL(18,6),
            Total DECIMAL(18,6),
            Id UNIQUEIDENTIFIER,
            Remarks NVARCHAR(MAX)
        );
        
        INSERT INTO #LDPE1_Result
        EXEC PE_NMD_PlantConsumptionSummaryReport 
            @PlantId = 'EAE21BDC-D41C-4990-96FD-8B0E6E6F0A57', 
            @year = @FinancialYear, 
            @ReportType = 'NormQuantity';
        
        INSERT INTO #PlantConsumptionData (PlantName, PlantId, MaterialName, MaterialCode, UOM, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, [Dec], Jan, Feb, Mar, SourceSP)
        SELECT 
            'LDPE 40N5' AS PlantName,
            'LDPE1' AS PlantId,
            Material,
            Material AS MaterialCode,
            UOM,
            COALESCE(April, 0),
            COALESCE(May, 0),
            COALESCE(June, 0),
            COALESCE(July, 0),
            COALESCE(August, 0),
            COALESCE(September, 0),
            COALESCE(October, 0),
            COALESCE(November, 0),
            COALESCE(December, 0),
            COALESCE(January, 0),
            COALESCE(February, 0),
            COALESCE(March, 0),
            'PE_NMD_PlantConsumptionSummaryReport' AS SourceSP
        FROM #LDPE1_Result
        WHERE NormType = 'UtilityConsumption';
        
        DROP TABLE #LDPE1_Result;
        PRINT 'LDPE 40N5 (Plant 1) data fetched successfully';
    END TRY
    BEGIN CATCH
        PRINT 'ERROR: Failed to fetch LDPE 40N5 (Plant 1) data - ' + ERROR_MESSAGE();
        IF OBJECT_ID('tempdb..#LDPE1_Result') IS NOT NULL DROP TABLE #LDPE1_Result;
    END CATCH

    -- Plant 4: LDPE 40N5 (Plant 2)
    BEGIN TRY
        PRINT 'Calling PE_NMD_PlantConsumptionSummaryReport for LDPE 40N5 (Plant 2)...';
        
        CREATE TABLE #LDPE2_Result (
            NormType NVARCHAR(200),
            Material NVARCHAR(200),
            UOM NVARCHAR(50),
            Spec NVARCHAR(50),
            April DECIMAL(18,6),
            May DECIMAL(18,6),
            June DECIMAL(18,6),
            July DECIMAL(18,6),
            August DECIMAL(18,6),
            September DECIMAL(18,6),
            October DECIMAL(18,6),
            November DECIMAL(18,6),
            December DECIMAL(18,6),
            January DECIMAL(18,6),
            February DECIMAL(18,6),
            March DECIMAL(18,6),
            Total DECIMAL(18,6),
            Id UNIQUEIDENTIFIER,
            Remarks NVARCHAR(MAX)
        );
        
        INSERT INTO #LDPE2_Result
        EXEC PE_NMD_PlantConsumptionSummaryReport 
            @PlantId = '9E37E963-BFA3-477B-B2E5-3F9825957899', 
            @year = @FinancialYear, 
            @ReportType = 'NormQuantity';
        
        INSERT INTO #PlantConsumptionData (PlantName, PlantId, MaterialName, MaterialCode, UOM, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, [Dec], Jan, Feb, Mar, SourceSP)
        SELECT 
            'LDPE 40N5' AS PlantName,
            'LDPE2' AS PlantId,
            Material,
            Material AS MaterialCode,
            UOM,
            COALESCE(April, 0),
            COALESCE(May, 0),
            COALESCE(June, 0),
            COALESCE(July, 0),
            COALESCE(August, 0),
            COALESCE(September, 0),
            COALESCE(October, 0),
            COALESCE(November, 0),
            COALESCE(December, 0),
            COALESCE(January, 0),
            COALESCE(February, 0),
            COALESCE(March, 0),
            'PE_NMD_PlantConsumptionSummaryReport' AS SourceSP
        FROM #LDPE2_Result
        WHERE NormType = 'UtilityConsumption';
        
        DROP TABLE #LDPE2_Result;
        PRINT 'LDPE 40N5 (Plant 2) data fetched successfully';
    END TRY
    BEGIN CATCH
        PRINT 'ERROR: Failed to fetch LDPE 40N5 (Plant 2) data - ' + ERROR_MESSAGE();
        IF OBJECT_ID('tempdb..#LDPE2_Result') IS NOT NULL DROP TABLE #LDPE2_Result;
    END CATCH

    -- Plant 5: PP
    BEGIN TRY
        PRINT 'Calling PP_NMD_PlantConsumptionSummaryReport for PP...';
        
        CREATE TABLE #PP_Result (
            NormType NVARCHAR(200),
            Material NVARCHAR(200),
            UOM NVARCHAR(50),
            Spec NVARCHAR(50),
            April DECIMAL(18,6),
            May DECIMAL(18,6),
            June DECIMAL(18,6),
            July DECIMAL(18,6),
            August DECIMAL(18,6),
            September DECIMAL(18,6),
            October DECIMAL(18,6),
            November DECIMAL(18,6),
            December DECIMAL(18,6),
            January DECIMAL(18,6),
            February DECIMAL(18,6),
            March DECIMAL(18,6),
            Total DECIMAL(18,6),
            Id UNIQUEIDENTIFIER,
            Remarks NVARCHAR(MAX)
        );
        
        INSERT INTO #PP_Result
        EXEC PP_NMD_PlantConsumptionSummaryReport 
            @PlantId = 'B60FC17A-4E3D-4D49-A5E6-0A739100E6C8', 
            @year = @FinancialYear, 
            @ReportType = 'NormQuantity';
        
        INSERT INTO #PlantConsumptionData (PlantName, PlantId, MaterialName, MaterialCode, UOM, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, [Dec], Jan, Feb, Mar, SourceSP)
        SELECT 
            'PP' AS PlantName,
            'PP' AS PlantId,
            Material,
            Material AS MaterialCode,
            UOM,
            COALESCE(April, 0),
            COALESCE(May, 0),
            COALESCE(June, 0),
            COALESCE(July, 0),
            COALESCE(August, 0),
            COALESCE(September, 0),
            COALESCE(October, 0),
            COALESCE(November, 0),
            COALESCE(December, 0),
            COALESCE(January, 0),
            COALESCE(February, 0),
            COALESCE(March, 0),
            'PP_NMD_PlantConsumptionSummaryReport' AS SourceSP
        FROM #PP_Result
        WHERE NormType = 'UtilityConsumption';
        
        DROP TABLE #PP_Result;
        PRINT 'PP data fetched successfully';
    END TRY
    BEGIN CATCH
        PRINT 'ERROR: Failed to fetch PP data - ' + ERROR_MESSAGE();
        IF OBJECT_ID('tempdb..#PP_Result') IS NOT NULL DROP TABLE #PP_Result;
    END CATCH

    DECLARE @FetchedRecordCount INT;
    SELECT @FetchedRecordCount = COUNT(*) FROM #PlantConsumptionData;
    PRINT 'Step 3: Total records fetched from plant SPs: ' + CAST(@FetchedRecordCount AS VARCHAR(10));

    -- =============================================
    -- Step 4: Get Previous Year's Data Structure
    -- =============================================
    DECLARE @PreviousYear VARCHAR(10);
    DECLARE @CurrentYearStart INT = CAST(LEFT(@FinancialYear, 4) AS INT);
    DECLARE @CurrentYearEnd INT = CAST(RIGHT(@FinancialYear, 2) AS INT);
    SET @PreviousYear = CAST(@CurrentYearStart - 1 AS VARCHAR(4)) + '-' + RIGHT('0' + CAST(@CurrentYearEnd - 1 AS VARCHAR(2)), 2);

    PRINT 'Step 4: Fetching previous year structure from: ' + @PreviousYear;

    CREATE TABLE #PreviousYearData
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

    INSERT INTO #PreviousYearData
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
    SELECT @PreviousYearCount = COUNT(*) FROM #PreviousYearData;
    PRINT 'Previous year records found: ' + CAST(@PreviousYearCount AS VARCHAR(10));

    -- =============================================
    -- Step 5: CPP Plant Details (will use from previous year data)
    -- =============================================
    -- Note: cpp_plant and cpp_plant_id are preserved from previous year
    -- to ensure JOIN with ProcessDemandMaster works correctly

    -- =============================================
    -- Step 6: Merge Previous Year Structure with Fetched Data
    -- =============================================
    
    -- Step 6a: Create PlantId Mapping Table
    CREATE TABLE #PlantIdMapping (
        SPPlantId NVARCHAR(100),
        ActualPlantCode NVARCHAR(100)
    );
    
    INSERT INTO #PlantIdMapping VALUES
        ('EOEG', '40N3'),      -- EOEG maps to NMD - EG
        ('GC', '40N8'),        -- Gas Cracker maps to NMD - GAS CRACKER
        ('LDPE1', '40N5'),     -- LDPE 40N5 (Plant 1) maps to NMD - LDPE
        ('LDPE2', '40N6'),     -- LDPE 40N5 (Plant 2) maps to NMD - LLDE / HDPE
        ('PP', '40N7');        -- PP maps to NMD - PP
    
    PRINT 'Step 6a: PlantId mapping created';
    
    -- Step 6b: Create Final Data Table
    CREATE TABLE #FinalData
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
        [dec] DECIMAL(18,6),
        jan DECIMAL(18,6),
        feb DECIMAL(18,6),
        mar DECIMAL(18,6),
        created_at DATETIME,
        updated_at DATETIME,
        remarks NVARCHAR(MAX)
    );

    INSERT INTO #FinalData
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
        -- Apply UOM conversion factors based on source UOM vs target UOM
        CASE 
            -- Gas Cracker: KNM3 → NM3 (multiply by 1000)
            WHEN UPPER(pcd.UOM) = 'KNM3' AND UPPER(pyd.uom) = 'NM3' THEN COALESCE(pcd.Apr, 0) * 1000
            -- Gas Cracker: MWH → KWH (multiply by 1000)
            WHEN UPPER(pcd.UOM) = 'MWH' AND UPPER(pyd.uom) = 'KWH' THEN COALESCE(pcd.Apr, 0) * 1000
            -- Gas Cracker: D M Water MT → M3 (1:1 for water density)
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'M3' AND UPPER(pcd.MaterialCode) = '310027966' THEN COALESCE(pcd.Apr, 0)
            -- Gas Cracker: Nitrogen MT → NM3 (multiply by 800.117 based on N2 density)
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'NM3' AND (UPPER(pcd.MaterialCode) = '310027971' OR UPPER(pcd.MaterialName) = 'NITROGEN') THEN COALESCE(pcd.Apr, 0) * 800.117
            ELSE COALESCE(pcd.Apr, 0)
        END AS apr,
        CASE 
            WHEN UPPER(pcd.UOM) = 'KNM3' AND UPPER(pyd.uom) = 'NM3' THEN COALESCE(pcd.May, 0) * 1000
            WHEN UPPER(pcd.UOM) = 'MWH' AND UPPER(pyd.uom) = 'KWH' THEN COALESCE(pcd.May, 0) * 1000
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'M3' AND UPPER(pcd.MaterialCode) = '310027966' THEN COALESCE(pcd.May, 0)
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'NM3' AND (UPPER(pcd.MaterialCode) = '310027971' OR UPPER(pcd.MaterialName) = 'NITROGEN') THEN COALESCE(pcd.May, 0) * 800.117
            ELSE COALESCE(pcd.May, 0)
        END AS may,
        CASE 
            WHEN UPPER(pcd.UOM) = 'KNM3' AND UPPER(pyd.uom) = 'NM3' THEN COALESCE(pcd.Jun, 0) * 1000
            WHEN UPPER(pcd.UOM) = 'MWH' AND UPPER(pyd.uom) = 'KWH' THEN COALESCE(pcd.Jun, 0) * 1000
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'M3' AND UPPER(pcd.MaterialCode) = '310027966' THEN COALESCE(pcd.Jun, 0)
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'NM3' AND (UPPER(pcd.MaterialCode) = '310027971' OR UPPER(pcd.MaterialName) = 'NITROGEN') THEN COALESCE(pcd.Jun, 0) * 800.117
            ELSE COALESCE(pcd.Jun, 0)
        END AS jun,
        CASE 
            WHEN UPPER(pcd.UOM) = 'KNM3' AND UPPER(pyd.uom) = 'NM3' THEN COALESCE(pcd.Jul, 0) * 1000
            WHEN UPPER(pcd.UOM) = 'MWH' AND UPPER(pyd.uom) = 'KWH' THEN COALESCE(pcd.Jul, 0) * 1000
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'M3' AND UPPER(pcd.MaterialCode) = '310027966' THEN COALESCE(pcd.Jul, 0)
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'NM3' AND (UPPER(pcd.MaterialCode) = '310027971' OR UPPER(pcd.MaterialName) = 'NITROGEN') THEN COALESCE(pcd.Jul, 0) * 800.117
            ELSE COALESCE(pcd.Jul, 0)
        END AS jul,
        CASE 
            WHEN UPPER(pcd.UOM) = 'KNM3' AND UPPER(pyd.uom) = 'NM3' THEN COALESCE(pcd.Aug, 0) * 1000
            WHEN UPPER(pcd.UOM) = 'MWH' AND UPPER(pyd.uom) = 'KWH' THEN COALESCE(pcd.Aug, 0) * 1000
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'M3' AND UPPER(pcd.MaterialCode) = '310027966' THEN COALESCE(pcd.Aug, 0)
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'NM3' AND (UPPER(pcd.MaterialCode) = '310027971' OR UPPER(pcd.MaterialName) = 'NITROGEN') THEN COALESCE(pcd.Aug, 0) * 800.117
            ELSE COALESCE(pcd.Aug, 0)
        END AS aug,
        CASE 
            WHEN UPPER(pcd.UOM) = 'KNM3' AND UPPER(pyd.uom) = 'NM3' THEN COALESCE(pcd.Sep, 0) * 1000
            WHEN UPPER(pcd.UOM) = 'MWH' AND UPPER(pyd.uom) = 'KWH' THEN COALESCE(pcd.Sep, 0) * 1000
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'M3' AND UPPER(pcd.MaterialCode) = '310027966' THEN COALESCE(pcd.Sep, 0)
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'NM3' AND (UPPER(pcd.MaterialCode) = '310027971' OR UPPER(pcd.MaterialName) = 'NITROGEN') THEN COALESCE(pcd.Sep, 0) * 800.117
            ELSE COALESCE(pcd.Sep, 0)
        END AS sep,
        CASE 
            WHEN UPPER(pcd.UOM) = 'KNM3' AND UPPER(pyd.uom) = 'NM3' THEN COALESCE(pcd.Oct, 0) * 1000
            WHEN UPPER(pcd.UOM) = 'MWH' AND UPPER(pyd.uom) = 'KWH' THEN COALESCE(pcd.Oct, 0) * 1000
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'M3' AND UPPER(pcd.MaterialCode) = '310027966' THEN COALESCE(pcd.Oct, 0)
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'NM3' AND (UPPER(pcd.MaterialCode) = '310027971' OR UPPER(pcd.MaterialName) = 'NITROGEN') THEN COALESCE(pcd.Oct, 0) * 800.117
            ELSE COALESCE(pcd.Oct, 0)
        END AS oct,
        CASE 
            WHEN UPPER(pcd.UOM) = 'KNM3' AND UPPER(pyd.uom) = 'NM3' THEN COALESCE(pcd.Nov, 0) * 1000
            WHEN UPPER(pcd.UOM) = 'MWH' AND UPPER(pyd.uom) = 'KWH' THEN COALESCE(pcd.Nov, 0) * 1000
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'M3' AND UPPER(pcd.MaterialCode) = '310027966' THEN COALESCE(pcd.Nov, 0)
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'NM3' AND (UPPER(pcd.MaterialCode) = '310027971' OR UPPER(pcd.MaterialName) = 'NITROGEN') THEN COALESCE(pcd.Nov, 0) * 800.117
            ELSE COALESCE(pcd.Nov, 0)
        END AS nov,
        CASE 
            WHEN UPPER(pcd.UOM) = 'KNM3' AND UPPER(pyd.uom) = 'NM3' THEN COALESCE(pcd.[Dec], 0) * 1000
            WHEN UPPER(pcd.UOM) = 'MWH' AND UPPER(pyd.uom) = 'KWH' THEN COALESCE(pcd.[Dec], 0) * 1000
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'M3' AND UPPER(pcd.MaterialCode) = '310027966' THEN COALESCE(pcd.[Dec], 0)
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'NM3' AND (UPPER(pcd.MaterialCode) = '310027971' OR UPPER(pcd.MaterialName) = 'NITROGEN') THEN COALESCE(pcd.[Dec], 0) * 800.117
            ELSE COALESCE(pcd.[Dec], 0)
        END AS [dec],
        CASE 
            WHEN UPPER(pcd.UOM) = 'KNM3' AND UPPER(pyd.uom) = 'NM3' THEN COALESCE(pcd.Jan, 0) * 1000
            WHEN UPPER(pcd.UOM) = 'MWH' AND UPPER(pyd.uom) = 'KWH' THEN COALESCE(pcd.Jan, 0) * 1000
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'M3' AND UPPER(pcd.MaterialCode) = '310027966' THEN COALESCE(pcd.Jan, 0)
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'NM3' AND (UPPER(pcd.MaterialCode) = '310027971' OR UPPER(pcd.MaterialName) = 'NITROGEN') THEN COALESCE(pcd.Jan, 0) * 800.117
            ELSE COALESCE(pcd.Jan, 0)
        END AS jan,
        CASE 
            WHEN UPPER(pcd.UOM) = 'KNM3' AND UPPER(pyd.uom) = 'NM3' THEN COALESCE(pcd.Feb, 0) * 1000
            WHEN UPPER(pcd.UOM) = 'MWH' AND UPPER(pyd.uom) = 'KWH' THEN COALESCE(pcd.Feb, 0) * 1000
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'M3' AND UPPER(pcd.MaterialCode) = '310027966' THEN COALESCE(pcd.Feb, 0)
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'NM3' AND (UPPER(pcd.MaterialCode) = '310027971' OR UPPER(pcd.MaterialName) = 'NITROGEN') THEN COALESCE(pcd.Feb, 0) * 800.117
            ELSE COALESCE(pcd.Feb, 0)
        END AS feb,
        CASE 
            WHEN UPPER(pcd.UOM) = 'KNM3' AND UPPER(pyd.uom) = 'NM3' THEN COALESCE(pcd.Mar, 0) * 1000
            WHEN UPPER(pcd.UOM) = 'MWH' AND UPPER(pyd.uom) = 'KWH' THEN COALESCE(pcd.Mar, 0) * 1000
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'M3' AND UPPER(pcd.MaterialCode) = '310027966' THEN COALESCE(pcd.Mar, 0)
            WHEN UPPER(pcd.UOM) = 'MT' AND UPPER(pyd.uom) = 'NM3' AND (UPPER(pcd.MaterialCode) = '310027971' OR UPPER(pcd.MaterialName) = 'NITROGEN') THEN COALESCE(pcd.Mar, 0) * 800.117
            ELSE COALESCE(pcd.Mar, 0)
        END AS mar,
        GETDATE() AS created_at,
        GETDATE() AS updated_at,
        pyd.remarks
    FROM #PreviousYearData pyd
    LEFT JOIN #PlantIdMapping pim
        ON pim.ActualPlantCode = pyd.process_plant_id
    LEFT JOIN #PlantConsumptionData pcd
        ON pcd.PlantId = pim.SPPlantId
        AND (
            -- Match by normalized material name (case-insensitive, remove spaces/underscores)
            REPLACE(REPLACE(UPPER(pcd.MaterialName), ' ', ''), '_', '') = REPLACE(REPLACE(UPPER(pyd.cpp_utility), ' ', ''), '_', '')
            OR
            -- Or match by material code if available
            pcd.MaterialCode = pyd.cpp_utility_id
            OR
            -- Special case: Match "Nitrogen" with "Nitrogen Gas" for Gas Cracker
            (pcd.PlantId = 'GC' AND UPPER(pcd.MaterialName) = 'NITROGEN' AND UPPER(pyd.cpp_utility) = 'NITROGEN GAS')
        );

    DECLARE @FinalDataCount INT;
    SELECT @FinalDataCount = COUNT(*) FROM #FinalData;
    PRINT 'Step 6b: Final data prepared: ' + CAST(@FinalDataCount AS VARCHAR(10)) + ' records';

    -- =============================================
    -- Step 7: Save to CalculatedProcessDemand
    -- =============================================
    INSERT INTO CalculatedProcessDemand
    (
        id, financial_year, process_plant, process_plant_id, 
        cpp_utility, cpp_utility_id, cpp_plant, cpp_plant_id,
        uom, apr, may, jun, jul, aug, sep, oct, nov, [dec], jan, feb, mar,
        created_at, updated_at, remarks
    )
    SELECT 
        id, financial_year, process_plant, process_plant_id,
        cpp_utility, cpp_utility_id, cpp_plant, cpp_plant_id,
        uom, apr, may, jun, jul, aug, sep, oct, nov, [dec], jan, feb, mar,
        created_at, updated_at, remarks
    FROM #FinalData;

    DECLARE @SavedRecordCount INT = @@ROWCOUNT;
    PRINT 'Step 7: Saved to CalculatedProcessDemand: ' + CAST(@SavedRecordCount AS VARCHAR(10)) + ' records';

    -- =============================================
    -- Step 8: Return Saved Results (only if @ReturnResults = 1)
    -- =============================================
    IF @ReturnResults = 1
    BEGIN
        SELECT 
            id,
            financial_year,
            process_plant,
            process_plant_id,
            cpp_utility,
            cpp_utility_id,
            cpp_plant,
            cpp_plant_id,
            uom,
            apr, may, jun, jul, aug, sep, oct, nov, [dec], jan, feb, mar,
            1 AS is_calculated,  -- Always 1 since this SP calculates the data
            remarks
        FROM #FinalData
        ORDER BY process_plant, cpp_utility;
    END

    -- Cleanup
    DROP TABLE #PlantConsumptionData;
    DROP TABLE #PreviousYearData;
    DROP TABLE #PlantIdMapping;
    DROP TABLE #FinalData;

    PRINT '======================================';
    PRINT 'Calculation and save completed successfully!';
    PRINT 'Total records saved: ' + CAST(@SavedRecordCount AS VARCHAR(10));
    PRINT 'Total records fetched from plant SPs: ' + CAST(@FetchedRecordCount AS VARCHAR(10));

END;
GO
