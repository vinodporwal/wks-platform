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
-- Last Modified: 2026-03-13 (Added validation: use 745 if samples > 10 AND heat rate < 600)
-- =============================================
-- 
-- Purpose:
-- Calculates HRSG heat rates from FCNA fuel bill data for specific date ranges.
-- Groups data by load ranges (40 MT, 50 MT, 60 MT, etc.) and calculates average heat rate.
-- Uses AvgMW (hourly average) for load bucketing, same as GT heat rate calculation.
-- Special validation: If samples > 10 AND calculated heat rate < 600, use default 745 kcal/MT.
--
-- Parameters:
-- @StartDate - Start date for calculation (YYYY-MM-DD)
-- @EndDate - End date for calculation (YYYY-MM-DD)
-- @AssetName - HRSG asset name (e.g., 'HRSG-1', 'HRSG-2', 'HRSG-3', 'HRSG--2' will be normalized to 'HRSG-2')
--
-- Returns:
-- HRSGLoad (MT/hr), HeatRate (kcal/MT) - with validation: 745 if samples > 10 AND heat rate < 600
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
    DECLARE @MinRecordsRequired INT = 3;  -- Minimum 3 samples
    DECLARE @MaxRecordsToUse INT = 50;  -- Maximum 50 samples (same as GT)
    DECLARE @MaxValidHeatRate DECIMAL(18,4) = 10000;  -- Maximum valid heat rate
    DECLARE @FreeSteamFactor DECIMAL(10,4) = 1.97;  -- Free steam factor (same as GT)
    DECLARE @DefaultHeatRateBelow55 DECIMAL(18,4) = 745.0;  -- Default heat rate for load < 55 MT/hr when samples <= 3
    DECLARE @DefaultHeatRateAbove55 DECIMAL(18,4) = 738.0;  -- Default heat rate for load >= 55 MT/hr when samples <= 3
    DECLARE @MinSamplesForValidation INT = 10;  -- Minimum samples to validate heat rate
    DECLARE @MinValidCalculatedHeatRate DECIMAL(18,4) = 600.0;  -- Minimum valid calculated heat rate
    
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
    PRINT '====================================================';
    
    -- Validate asset name
    IF @AssetName NOT IN ('HRSG-1', 'HRSG-2', 'HRSG-3')
    BEGIN
        PRINT 'ERROR: Invalid asset name. Must be HRSG-1, HRSG-2, or HRSG-3';
        RETURN;
    END
    
    -- Loop through load points: 40, 50, 60, ..., 140 MT/hr
    SET @HRSGLoad = @MinLoad;
    
    WHILE @HRSGLoad <= @MaxLoad
    BEGIN
        -- Calculate load range (±2.5 MT/hr for 5 MT interval)
        SET @LoadRangeLow = @HRSGLoad - (@LoadInterval / 2);
        SET @LoadRangeHigh = @HRSGLoad + (@LoadInterval / 2);
        
        -- Query data and calculate heat rates (limit to top 50 best samples)
        SELECT 
            @RecordCount = COUNT(*),
            @BestHeatRate = CASE 
                WHEN COUNT(*) >= @MinRecordsRequired THEN MIN(HeatRate)
                ELSE 0
            END,
            @AvgHeatRate = CASE 
                WHEN COUNT(*) >= @MinRecordsRequired THEN AVG(HeatRate)
                ELSE 0
            END,
            @DateRangeStart = MIN(Date),
            @DateRangeEnd = MAX(Date)
        FROM (
            SELECT TOP (@MaxRecordsToUse) HeatRate, Date
            FROM dbo.CPP_NMD_FCNA_FuelBill
            WHERE AssetName = @AssetName
              AND Date BETWEEN @StartDate AND @EndDate
              AND AvgMW BETWEEN @LoadRangeLow AND @LoadRangeHigh  -- Use AvgMW for load bucketing
              AND HeatRate IS NOT NULL
              AND HeatRate > 0
              AND HeatRate <= @MaxValidHeatRate
            ORDER BY HeatRate
        ) AS TopSamples;
        
        -- Apply default heat rate if insufficient samples (samples <= 3)
        IF @RecordCount <= @MinRecordsRequired
        BEGIN
            IF @HRSGLoad < 55.0
            BEGIN
                SET @BestHeatRate = @DefaultHeatRateBelow55;
                SET @AvgHeatRate = @DefaultHeatRateBelow55;
            END
            ELSE  -- Load >= 55 MT/hr
            BEGIN
                SET @BestHeatRate = @DefaultHeatRateAbove55;
                SET @AvgHeatRate = @DefaultHeatRateAbove55;
            END
        END
        
        -- Special validation: If samples > 10 AND calculated heat rate < 600, use 745
        -- This handles cases where there's enough data but calculated values are unrealistically low
        IF @RecordCount > @MinSamplesForValidation AND @AvgHeatRate > 0 AND @AvgHeatRate < @MinValidCalculatedHeatRate
        BEGIN
            PRINT '  [VALIDATION] Load ' + CAST(@HRSGLoad AS VARCHAR(10)) + ' MT/hr: ' + 
                  CAST(@RecordCount AS VARCHAR(10)) + ' samples with heat rate ' + 
                  CAST(CAST(@AvgHeatRate AS DECIMAL(10,2)) AS VARCHAR(20)) + 
                  ' < 600, using default 745';
            SET @AvgHeatRate = 745.0;
            SET @BestHeatRate = 745.0;
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
        
        -- Print progress
        IF @RecordCount > @MinRecordsRequired
            PRINT '  Load ' + CAST(@HRSGLoad AS VARCHAR(10)) + ' MT/hr: ' + 
                  CAST(@RecordCount AS VARCHAR(10)) + ' records, ' +
                  'Best=' + CAST(CAST(@BestHeatRate AS DECIMAL(10,2)) AS VARCHAR(20)) + ', ' +
                  'Avg=' + CAST(CAST(@AvgHeatRate AS DECIMAL(10,2)) AS VARCHAR(20));
        ELSE
            PRINT '  Load ' + CAST(@HRSGLoad AS VARCHAR(10)) + ' MT/hr: ' + 
                  CAST(@RecordCount AS VARCHAR(10)) + ' records (<= ' + 
                  CAST(@MinRecordsRequired AS VARCHAR(10)) + ' samples) - Using default HR=' + 
                  CAST(CAST(@AvgHeatRate AS DECIMAL(10,2)) AS VARCHAR(20));
        
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
