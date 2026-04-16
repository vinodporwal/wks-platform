USE [RIL.AOP]
GO
/****** Object:  StoredProcedure [dbo].[CPP_CalculateHRSGHeatRate_ByDateRange]    Script Date: 3/13/2026 6:26:02 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- =============================================
-- Stored Procedure: CPP_CalculateHRSGHeatRate_ByDateRange
-- Description: Calculate HRSG heat rates for different load points based on date range
-- Author: System
-- Date: 2026-02-23
-- Last Modified: 2026-03-13 (Updated per client requirements: exclude <600 kcal/kg, avg of 8 from top 10)
-- =============================================
-- 
-- Purpose:
-- Calculates HRSG heat rates from FCNA fuel bill data for specific date ranges.
-- Groups data by load ranges (40 MT, 50 MT, 60 MT, etc.) and calculates average heat rate.
-- Uses AvgMW (hourly average) for load bucketing, same as GT heat rate calculation.
--
-- Client Requirements (2026-03-13):
-- - OEM Heat Rate: 745 kcal/kg (FD fan mode, 94% efficiency)
-- - Previous Year Heat Rate: 678 kcal/kg (GT mode), 745 kcal/kg (FD mode)
-- - Proposed Heat Rate: Exclude datapoints below 600 kcal/kg, calculate average of 8 values from top 10 (excluding top 2)
--
-- Parameters:
-- @StartDate - Start date for calculation (YYYY-MM-DD)
-- @EndDate - End date for calculation (YYYY-MM-DD)
-- @AssetName - HRSG asset name (e.g., 'HRSG-1', 'HRSG-2', 'HRSG-3', 'HRSG--2' will be normalized to 'HRSG-2')
--
-- Returns:
-- HRSGLoad (MT/hr), HeatRate (kcal/kg) - calculated per client requirements
-- =============================================

ALTER PROCEDURE [dbo].[CPP_CalculateHRSGHeatRate_ByDateRange]
    @StartDate DATE,
    @EndDate DATE,
    @AssetName NVARCHAR(50)
AS
BEGIN
    SET NOCOUNT ON;
    
    -- Normalize asset name: Replace double dash with single dash (HRSG--2 -> HRSG-2)
    SET @AssetName = REPLACE(@AssetName, 'HRSG--', 'HRSG-');
    
    -- Declare variables for load range configuration
    DECLARE @MinLoad DECIMAL(10,2) = 30.0;  -- Starting load: 30 MT/hr
    DECLARE @LoadInterval DECIMAL(10,2) = 5.0;  -- Load interval: 5 MT/hr
    DECLARE @MaxLoad DECIMAL(10,2) = 135.0;  -- Maximum load: 135 MT/hr (max HRSG capacity)
    DECLARE @MinRecordsRequired INT = 8;  -- Minimum 8 samples required for average calculation
    DECLARE @TopRecordsToFetch INT = 10;  -- Fetch top 10 best samples
    DECLARE @TopRecordsToExclude INT = 2;  -- Exclude top 2 best samples
    DECLARE @MaxValidHeatRate DECIMAL(18,4) = 10000;  -- Maximum valid heat rate
    DECLARE @MinValidHeatRate DECIMAL(18,4) = 600.0;  -- Minimum valid heat rate (exclude below 600 kcal/kg)
    DECLARE @FreeSteamFactor DECIMAL(10,4) = 1.97;  -- Free steam factor (same as GT)
    DECLARE @OEMHeatRate DECIMAL(18,4) = 745.0;  -- OEM default: 745 kcal/kg (FD fan mode, 94% efficiency)
    DECLARE @PreviousYearGTMode DECIMAL(18,4) = 678.0;  -- Previous year GT mode: 678 kcal/kg
    DECLARE @PreviousYearFDMode DECIMAL(18,4) = 745.0;  -- Previous year FD mode: 745 kcal/kg
    
    -- Variables for loop
    DECLARE @HRSGLoad DECIMAL(10,2);
    DECLARE @LoadRangeLow DECIMAL(10,2);
    DECLARE @LoadRangeHigh DECIMAL(10,2);
    
    -- Variables for calculation results
    DECLARE @RecordCount INT;
    DECLARE @BestHeatRate DECIMAL(18,4);
    DECLARE @AvgHeatRate DECIMAL(18,4);
    DECLARE @DateRangeStart DATETIME;
    DECLARE @DateRangeEnd DATETIME;
    
    -- Temp table to hold calculation results (similar to GT SP)
    CREATE TABLE #HeatRateResults (
        AssetName NVARCHAR(100),
        HRSGLoad DECIMAL(10,2),
        BestArchivedHeatRate DECIMAL(18,4),
        DisplayedAvgHeatRate DECIMAL(18,4),
        FreeSteamFactor DECIMAL(10,4),
        RecordCount INT,
        DateRangeStart DATETIME,
        DateRangeEnd DATETIME,
        LoadRangeLow DECIMAL(10,2),
        LoadRangeHigh DECIMAL(10,2)
    );
    
    PRINT 'Starting HRSG Heat Rate Calculation for Date Range...';
    PRINT '====================================================';
    PRINT 'Asset: ' + @AssetName;
    PRINT 'Date Range: ' + CONVERT(VARCHAR(10), @StartDate, 120) + ' to ' + CONVERT(VARCHAR(10), @EndDate, 120);
    PRINT 'Logic: Exclude HR < 600 kcal/kg, Average of 8 values from top 10 (excluding top 2)';
    PRINT '====================================================';
    
    -- Validate asset name
    IF @AssetName NOT IN ('HRSG-1', 'HRSG-2', 'HRSG-3')
    BEGIN
        PRINT 'ERROR: Invalid asset name. Must be HRSG-1, HRSG-2, or HRSG-3';
        RETURN;
    END
    
    -- Loop through load points: 30, 35, 40, ..., 135 MT/hr
    SET @HRSGLoad = @MinLoad;
    
    WHILE @HRSGLoad <= @MaxLoad
    BEGIN
        -- Calculate load range (±2.5 MT/hr for 5 MT interval)
        SET @LoadRangeLow = @HRSGLoad - (@LoadInterval / 2);
        SET @LoadRangeHigh = @HRSGLoad + (@LoadInterval / 2);
        
        -- Query data and calculate heat rates
        -- Step 1: Calculate heat rate from KCAL and GenerateMW (HeatRate = KCAL / GenerateMW in kcal/kg)
        -- Step 2: Get top 10 samples, excluding values below 600 kcal/kg
        -- Step 3: Exclude top 2 best samples
        -- Step 4: Calculate average of remaining 8 samples
        SELECT 
            @RecordCount = COUNT(*),
            @BestHeatRate = MIN(CalculatedHeatRate),
            @AvgHeatRate = AVG(CalculatedHeatRate),
            @DateRangeStart = MIN(Date),
            @DateRangeEnd = MAX(Date)
        FROM (
            -- Get top 10 samples, then exclude top 2 (rows 1-2), keep rows 3-10 for average
            SELECT CalculatedHeatRate, Date, ROW_NUMBER() OVER (ORDER BY CalculatedHeatRate ASC) AS RowNum
            FROM (
                SELECT TOP (@TopRecordsToFetch) 
                    (KCAL / NULLIF(GenerateMW, 0)) AS CalculatedHeatRate,
                    Date
                FROM dbo.CPP_NMD_FCNA_FuelBill
                WHERE AssetName = @AssetName
                  AND Date BETWEEN @StartDate AND @EndDate
                  AND AvgMW BETWEEN @LoadRangeLow AND @LoadRangeHigh  -- Use AvgMW (steam flow MT/hr) for load bucketing
                  AND KCAL IS NOT NULL
                  AND GenerateMW IS NOT NULL
                  AND GenerateMW > 0
                  AND (KCAL / NULLIF(GenerateMW, 0)) >= @MinValidHeatRate  -- Exclude values below 600 kcal/kg
                  AND (KCAL / NULLIF(GenerateMW, 0)) <= @MaxValidHeatRate
                ORDER BY (KCAL / NULLIF(GenerateMW, 0)) ASC
            ) AS Top10Samples
        ) AS RankedSamples
        WHERE RowNum > @TopRecordsToExclude;  -- Exclude top 2 best samples
        
        -- Apply default heat rate if insufficient samples (< 8 samples)
        IF @RecordCount < @MinRecordsRequired
        BEGIN
            -- Use OEM default heat rate (745 kcal/kg for FD mode)
            SET @BestHeatRate = @OEMHeatRate;
            SET @AvgHeatRate = @OEMHeatRate;
            
            PRINT '  Load ' + CAST(@HRSGLoad AS VARCHAR(10)) + ' MT/hr: ' + 
                  CAST(@RecordCount AS VARCHAR(10)) + ' valid records (< ' + 
                  CAST(@MinRecordsRequired AS VARCHAR(10)) + ' required) - Using OEM default HR=' + 
                  CAST(CAST(@AvgHeatRate AS DECIMAL(10,2)) AS VARCHAR(20));
        END
        ELSE
        BEGIN
            PRINT '  Load ' + CAST(@HRSGLoad AS VARCHAR(10)) + ' MT/hr: ' + 
                  CAST(@RecordCount AS VARCHAR(10)) + ' valid records, ' +
                  'Best=' + CAST(CAST(@BestHeatRate AS DECIMAL(10,2)) AS VARCHAR(20)) + ', ' +
                  'Avg=' + CAST(CAST(@AvgHeatRate AS DECIMAL(10,2)) AS VARCHAR(20));
        END
        
        -- Insert results
        INSERT INTO #HeatRateResults (
            AssetName, HRSGLoad, BestArchivedHeatRate, DisplayedAvgHeatRate, FreeSteamFactor,
            RecordCount, DateRangeStart, DateRangeEnd, LoadRangeLow, LoadRangeHigh
        )
        VALUES (
            @AssetName, @HRSGLoad, @BestHeatRate, @AvgHeatRate, @FreeSteamFactor,
            @RecordCount, @DateRangeStart, @DateRangeEnd, @LoadRangeLow, @LoadRangeHigh
        );
        
        -- Increment load by interval
        SET @HRSGLoad = @HRSGLoad + @LoadInterval;
    END;
    
    PRINT '';
    PRINT '====================================================';
    PRINT 'Calculation completed. Returning results...';
    PRINT '====================================================';
    
    -- Return results (similar to GT SP format)
    -- Note: Returning DisplayedAvgHeatRate as HeatRate to match Java service expectations
    SELECT 
        AssetName,
        HRSGLoad,
        BestArchivedHeatRate,
        DisplayedAvgHeatRate AS HeatRate,  -- Alias to match Java service column name
        FreeSteamFactor,
        RecordCount,
        DateRangeStart,
        DateRangeEnd,
        LoadRangeLow,
        LoadRangeHigh
    FROM #HeatRateResults
    ORDER BY HRSGLoad;
    
    -- Cleanup
    DROP TABLE #HeatRateResults;
    
END;
