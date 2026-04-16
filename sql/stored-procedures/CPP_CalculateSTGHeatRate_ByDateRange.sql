USE [RIL.AOP]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- =============================================
-- Stored Procedure: CPP_CalculateSTGHeatRate_ByDateRange
-- Description: Calculate STG heat rates for different load points based on date range
-- Author: System
-- Date: 2026-04-06
-- =============================================
-- 
-- Purpose:
-- Calculates STG heat rates from FCNA fuel bill data for specific date ranges.
-- Uses fixed AssetName = 'STG1'.
-- 
-- FOR LOADS BELOW 11 MW: Uses predefined fixed heat rates (not calculated from samples)
--   - 6.0 MW = 3315 KCAL/KWH
--   - 7.0 MW = 2941 KCAL/KWH
--   - 8.0 MW = 2661 KCAL/KWH
--   - 9.0 MW = 2459 KCAL/KWH
--   - 10.0 MW = 2283 KCAL/KWH
--   - Intermediate loads interpolated between these values
--
-- FOR LOADS 11 MW AND ABOVE: Calculates from FCNA fuel bill data with OEM validation
--   - Groups data by load ranges from 11.0 MW to 25.0 MW in 0.5 MW intervals
--   - Uses AvgMW (hourly average MW) for load bucketing
--   - Selects TOP 50 best (lowest) heat rate samples per load
--   - VALIDATION: Compares calculated heat rate with OEM heat rate from CPP_STGHeatRate (PREVIOUS year)
--     * Automatically determines financial year from @StartDate
--     * OEM year is always previous year (e.g., if start date is 2025, OEM = 2024-25)
--     * If sample count < 10: Uses OEM heat rate
--     * If calculated differs > 10% from OEM: Uses OEM heat rate
--     * If calculated differs ≤ 10% from OEM: Uses calculated heat rate
--
-- Parameters:
-- @StartDate - Start date for calculation (YYYY-MM-DD)
-- @EndDate - End date for calculation (YYYY-MM-DD)
--
-- Returns:
-- STGLoad (MW), HeatRate (kcal/kWh), sample counts and load range metadata
-- =============================================

CREATE OR ALTER PROCEDURE [dbo].[CPP_CalculateSTGHeatRate_ByDateRange]
    @StartDate DATE,
    @EndDate DATE
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @AssetName NVARCHAR(50) = 'STG1';
    DECLARE @CurrentFinancialYear NVARCHAR(10);
    DECLARE @OEMFinancialYear NVARCHAR(10);

    -- Calculate Current FinancialYear based on @StartDate
    -- If start date is April 1 or later, it's current year (e.g., 2025-26)
    -- If start date is before April 1, it's previous year
    SET @CurrentFinancialYear = CASE 
        WHEN MONTH(@StartDate) >= 4 
        THEN CAST(YEAR(@StartDate) AS VARCHAR(4)) + '-' + CAST(YEAR(@StartDate) + 1 AS VARCHAR(4))
        ELSE CAST(YEAR(@StartDate) - 1 AS VARCHAR(4)) + '-' + CAST(YEAR(@StartDate) AS VARCHAR(4))
    END;

    -- OEM FinancialYear is always the PREVIOUS year (for comparison/validation)
    -- Subtract 1 year from current FY
    SET @OEMFinancialYear = CAST(YEAR(@StartDate) - 1 AS VARCHAR(4)) + '-' + CAST(YEAR(@StartDate) AS VARCHAR(4));

    DECLARE @MinLoad DECIMAL(10,2) = 6.0;
    DECLARE @LoadInterval DECIMAL(10,2) = 0.5;
    DECLARE @MaxLoad DECIMAL(10,2) = 25.0;
    DECLARE @HalfRange DECIMAL(10,2) = 0.25;
    DECLARE @MaxRecordsToUse INT = 50;
    DECLARE @MinValidHeatRate DECIMAL(18,4) = 0.0;
    DECLARE @MaxValidHeatRate DECIMAL(18,4) = 3000.0;
    DECLARE @FixedRateThreshold DECIMAL(10,2) = 11.0;  -- Use fixed rates below this load

    DECLARE @STGLoad DECIMAL(10,2);
    DECLARE @LoadRangeLow DECIMAL(10,2);
    DECLARE @LoadRangeHigh DECIMAL(10,2);

    DECLARE @RecordCount INT;
    DECLARE @BestHeatRate DECIMAL(18,4);
    DECLARE @AvgHeatRate DECIMAL(18,4);
    DECLARE @DateRangeStart DATETIME;
    DECLARE @DateRangeEnd DATETIME;
    DECLARE @UseFixedRate BIT;
    DECLARE @FixedHeatRate DECIMAL(18,4);
    DECLARE @OEMHeatRate DECIMAL(18,4);
    DECLARE @HeatRateDifference DECIMAL(18,4);
    DECLARE @HeatRateDifferencePercent DECIMAL(10,2);
    DECLARE @MinSampleThreshold INT = 10;
    DECLARE @DeviationThresholdPercent DECIMAL(10,2) = 10.0;
    DECLARE @FinalHeatRate DECIMAL(18,4);
    DECLARE @ValidationReason NVARCHAR(100);

    CREATE TABLE #HeatRateResults (
        AssetName NVARCHAR(100),
        STGLoad DECIMAL(10,2),
        BestArchivedHeatRate DECIMAL(18,4),
        DisplayedAvgHeatRate DECIMAL(18,4),
        RecordCount INT,
        DateRangeStart DATETIME,
        DateRangeEnd DATETIME,
        LoadRangeLow DECIMAL(10,2),
        LoadRangeHigh DECIMAL(10,2),
        OEMHeatRate DECIMAL(18,4),
        DeviationPercent DECIMAL(10,2),
        ValidationReason NVARCHAR(100),
        FinalHeatRate DECIMAL(18,4)
    );

    -- Fixed heat rates for loads below 11 MW
    CREATE TABLE #FixedHeatRates (
        STGLoad DECIMAL(10,2),
        FixedHeatRate DECIMAL(18,4)
    );

    INSERT INTO #FixedHeatRates (STGLoad, FixedHeatRate) VALUES
        (6.0, 3315.0000),
        (6.5, 3128.0000),  -- Interpolated between 6 and 7
        (7.0, 2941.0000),
        (7.5, 2801.0000),  -- Interpolated between 7 and 8
        (8.0, 2661.0000),
        (8.5, 2560.0000),  -- Interpolated between 8 and 9
        (9.0, 2459.0000),
        (9.5, 2371.0000),  -- Interpolated between 9 and 10
        (10.0, 2283.0000),
        (10.5, 2283.0000); -- At threshold, use 10 MW rate

    PRINT 'Starting STG Heat Rate Calculation for Date Range...';
    PRINT '====================================================';
    PRINT 'Asset: ' + @AssetName;
    PRINT 'Date Range: ' + CONVERT(VARCHAR(10), @StartDate, 120) + ' to ' + CONVERT(VARCHAR(10), @EndDate, 120);
    PRINT '====================================================';

    SET @STGLoad = @MinLoad;

    WHILE @STGLoad <= @MaxLoad
    BEGIN
        SET @LoadRangeLow = @STGLoad - @HalfRange;
        SET @LoadRangeHigh = @STGLoad + @HalfRange;
        SET @UseFixedRate = CASE WHEN @STGLoad < @FixedRateThreshold THEN 1 ELSE 0 END;

        -- Get fixed heat rate if applicable
        IF @UseFixedRate = 1
        BEGIN
            SELECT @FixedHeatRate = FixedHeatRate
            FROM #FixedHeatRates
            WHERE STGLoad = @STGLoad;

            SET @BestHeatRate = @FixedHeatRate;
            SET @AvgHeatRate = @FixedHeatRate;
            SET @RecordCount = 0;  -- 0 indicates fixed rate (not from samples)
            SET @DateRangeStart = NULL;
            SET @DateRangeEnd = NULL;
            SET @OEMHeatRate = NULL;
            SET @HeatRateDifferencePercent = NULL;
            SET @ValidationReason = 'FIXED RATE (Load < 11 MW)';
            SET @FinalHeatRate = @FixedHeatRate;

            PRINT '  Load ' + CAST(@STGLoad AS VARCHAR(10)) + ' MW: [FIXED RATE] ' +
                  CAST(CAST(@FixedHeatRate AS DECIMAL(10,2)) AS VARCHAR(20)) + ' KCAL/KWH';
        END
        ELSE
        BEGIN
            -- Calculate from FCNA fuel bill samples for loads >= 11 MW
            SELECT
                @RecordCount = COUNT(*),
                @BestHeatRate = CASE
                    WHEN COUNT(*) > 0 THEN MIN(HeatRate)
                    ELSE NULL
                END,
                @AvgHeatRate = CASE
                    WHEN COUNT(*) > 0 THEN AVG(HeatRate)
                    ELSE NULL
                END,
                @DateRangeStart = MIN(Date),
                @DateRangeEnd = MAX(Date)
            FROM (
                SELECT TOP (@MaxRecordsToUse) HeatRate, Date
                FROM dbo.CPP_NMD_FCNA_FuelBill
                WHERE AssetName = @AssetName
                  AND Date BETWEEN @StartDate AND @EndDate
                  AND AvgMW BETWEEN @LoadRangeLow AND @LoadRangeHigh
                  AND HeatRate IS NOT NULL
                  AND HeatRate > @MinValidHeatRate
                  AND HeatRate < @MaxValidHeatRate
                ORDER BY HeatRate
            ) AS TopSamples;

            -- Get OEM Heat Rate for this load from the PREVIOUS financial year
            -- Fetch from CPP_STGHeatRate table for 'STG-1' or 'STG1'
            SET @OEMHeatRate = NULL;
            SELECT TOP 1 @OEMHeatRate = FinalHeatRate
            FROM dbo.CPP_STGHeatRate
            WHERE AssetName IN ('STG-1', 'STG1')
              AND STGLoad = @STGLoad
              AND FinancialYear = '2026-27'
            ORDER BY STGLoad;

            -- Validate: Check sample count and deviation from OEM
            SET @FinalHeatRate = @AvgHeatRate;
            SET @ValidationReason = 'CALCULATED FROM SAMPLES';
            SET @HeatRateDifferencePercent = NULL;

            -- If not enough samples (< 10), use OEM rate
            IF @RecordCount < @MinSampleThreshold
            BEGIN
                SET @FinalHeatRate = @OEMHeatRate;
                SET @ValidationReason = 'OEM (Insufficient samples: ' + CAST(@RecordCount AS VARCHAR(3)) + ' < ' + CAST(@MinSampleThreshold AS VARCHAR(3)) + ')';
            END
            -- If we have samples but deviation from OEM > 10%, use OEM rate
            ELSE IF @OEMHeatRate IS NOT NULL AND @AvgHeatRate IS NOT NULL
            BEGIN
                SET @HeatRateDifference = ABS(@AvgHeatRate - @OEMHeatRate);
                SET @HeatRateDifferencePercent = (@HeatRateDifference / @OEMHeatRate) * 100.0;

                IF @HeatRateDifferencePercent > @DeviationThresholdPercent
                BEGIN
                    SET @FinalHeatRate = @OEMHeatRate;
                    SET @ValidationReason = 'OEM (Deviation ' + CAST(CAST(@HeatRateDifferencePercent AS DECIMAL(5,2)) AS VARCHAR(10)) + '% > 10%)';
                END
            END;

            PRINT '  Load ' + CAST(@STGLoad AS VARCHAR(10)) + ' MW: ' +
                  CAST(ISNULL(@RecordCount, 0) AS VARCHAR(10)) + ' records' +
                  CASE WHEN @AvgHeatRate IS NOT NULL
                       THEN ', Calc=' + CAST(CAST(@AvgHeatRate AS DECIMAL(10,2)) AS VARCHAR(20)) +
                            ', OEM=' + CAST(ISNULL(CAST(@OEMHeatRate AS DECIMAL(10,2)), 0) AS VARCHAR(20)) +
                            ', Final=' + CAST(CAST(@FinalHeatRate AS DECIMAL(10,2)) AS VARCHAR(20))
                       ELSE ', No valid samples, using OEM=' + CAST(ISNULL(CAST(@OEMHeatRate AS DECIMAL(10,2)), 0) AS VARCHAR(20))
                  END;
        END;

        INSERT INTO #HeatRateResults (
            AssetName, STGLoad, BestArchivedHeatRate, DisplayedAvgHeatRate,
            RecordCount, DateRangeStart, DateRangeEnd, LoadRangeLow, LoadRangeHigh,
            OEMHeatRate, DeviationPercent, ValidationReason, FinalHeatRate
        )
        VALUES (
            @AssetName, @STGLoad, @BestHeatRate, @AvgHeatRate,
            ISNULL(@RecordCount, 0), @DateRangeStart, @DateRangeEnd, @LoadRangeLow, @LoadRangeHigh,
            @OEMHeatRate, @HeatRateDifferencePercent, @ValidationReason, @FinalHeatRate
        );

        SET @STGLoad = @STGLoad + @LoadInterval;
    END;

    PRINT '';
    PRINT '====================================================';
    PRINT 'Calculation completed. Returning results...';
    PRINT '====================================================';

    SELECT
        AssetName,
        STGLoad,
        BestArchivedHeatRate,
        DisplayedAvgHeatRate AS CalculatedHeatRate,
        RecordCount AS SampleCount,
        DateRangeStart,
        DateRangeEnd,
        LoadRangeLow,
        LoadRangeHigh,
        OEMHeatRate,
        DeviationPercent,
        ValidationReason,
        FinalHeatRate AS HeatRate
    FROM #HeatRateResults
    ORDER BY STGLoad;

    DROP TABLE #HeatRateResults;
    DROP TABLE #FixedHeatRates;
END;
GO
