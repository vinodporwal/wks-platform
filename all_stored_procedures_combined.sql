USE [RIL.AOP]
GO

USE [RIL.AOP]
GO
/****** Object:  StoredProcedure [dbo].[CPP_CalculateGTHeatRate]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE PROCEDURE [dbo].[CPP_CalculateGTHeatRate]
AS
BEGIN
    SET NOCOUNT ON;
    
    -- Constants
    DECLARE @UtilityId NVARCHAR(50) = '310027907';
    DECLARE @FreeSteamFactor DECIMAL(10,4) = 1.97;
    DECLARE @MinRecordsRequired INT = 1;  -- Minimum 1 sample
    DECLARE @MaxRecordsToUse INT = 50;  -- Maximum 50 samples
    DECLARE @MaxValidHeatRate DECIMAL(18,4) = 10000;
    DECLARE @LoadRangeWidth DECIMAL(10,2) = 0.25;  -- ±0.25 MW
    DECLARE @FinancialYear NVARCHAR(20) = '2026-27';
    
    -- Variables for loop
    DECLARE @AssetName NVARCHAR(100);
    DECLARE @AssetFKId UNIQUEIDENTIFIER;
    DECLARE @GTLoad DECIMAL(10,2);
    DECLARE @LoadRangeLow DECIMAL(10,2);
    DECLARE @LoadRangeHigh DECIMAL(10,2);
    
    -- Variables for calculation results
    DECLARE @RecordCount INT;
    DECLARE @BestHeatRate DECIMAL(18,4);
    DECLARE @AvgHeatRate DECIMAL(18,4);
    DECLARE @DateRangeStart DATETIME;
    DECLARE @DateRangeEnd DATETIME;
    DECLARE @MinLoadHeatRate DECIMAL(18,4);
    DECLARE @MinLoadAvgHeatRate DECIMAL(18,4);
    
    -- Temp table to hold calculation results
    CREATE TABLE #HeatRateResults (
        Asset_FK_Id UNIQUEIDENTIFIER,
        AssetName NVARCHAR(100),
        GTLoad DECIMAL(10,2),
        BestArchivedHeatRate DECIMAL(18,4),
        DisplayedAvgHeatRate DECIMAL(18,4),
        RecordCount INT,
        DateRangeStart DATETIME,
        DateRangeEnd DATETIME,
        LoadRangeLow DECIMAL(10,2),
        LoadRangeHigh DECIMAL(10,2)
    );
    
    PRINT 'Starting GT Heat Rate Calculation...';
    PRINT '======================================';
    
    -- Cursor for assets
    DECLARE asset_cursor CURSOR FOR
    SELECT DISTINCT AssetName 
    FROM dbo.CPP_NMD_FCNA_FuelBill
    WHERE AssetName IN ('GT-1', 'GT-2', 'GT-3')
    ORDER BY AssetName;
    
    OPEN asset_cursor;
    FETCH NEXT FROM asset_cursor INTO @AssetName;
    
    WHILE @@FETCH_STATUS = 0
    BEGIN
        PRINT '';
        PRINT 'Processing Asset: ' + @AssetName;
        PRINT '-----------------------------------';

        SELECT TOP 1
            @AssetFKId = AssetId
        FROM dbo.PowerGenerationAssets
        WHERE AssetName = @AssetName
        ORDER BY AssetId;
        
        -- First, get heat rates for minimum load (6 MW) to use for 0 MW
        SET @GTLoad = 6.0;
        SET @LoadRangeLow = @GTLoad - @LoadRangeWidth;
        SET @LoadRangeHigh = @GTLoad + (@LoadRangeWidth - 0.01);
        
        SELECT 
            @MinLoadHeatRate = CASE 
                WHEN COUNT(*) >= @MinRecordsRequired THEN MIN(HeatRate)
                ELSE 0
            END,
            @MinLoadAvgHeatRate = CASE 
                WHEN COUNT(*) >= @MinRecordsRequired THEN AVG(HeatRate)
                ELSE 0
            END
        FROM (
            SELECT TOP (@MaxRecordsToUse) HeatRate
            FROM dbo.CPP_NMD_FCNA_FuelBill
            WHERE AssetName = @AssetName
              AND AvgMW BETWEEN @LoadRangeLow AND @LoadRangeHigh
              AND HeatRate IS NOT NULL
              AND HeatRate > 0
              AND HeatRate <= @MaxValidHeatRate
            ORDER BY HeatRate
        ) AS TopSamples;
        
        -- Insert 0 MW row with same heat rate as minimum load (6 MW)
        INSERT INTO #HeatRateResults (
            Asset_FK_Id, AssetName, GTLoad, BestArchivedHeatRate, DisplayedAvgHeatRate,
            RecordCount, DateRangeStart, DateRangeEnd, LoadRangeLow, LoadRangeHigh
        )
        VALUES (
            @AssetFKId, @AssetName, 0.0, @MinLoadHeatRate, @MinLoadAvgHeatRate,
            0, NULL, NULL, 0.0, 0.0
        );
        
        PRINT '  Added 0 MW row with heat rate from 6 MW data';
        
        -- Loop through load points: 6.0, 6.5, 7.0, ..., 22.0
        SET @GTLoad = 6.0;
        
        WHILE @GTLoad <= 22.0
        BEGIN
            -- Calculate load range
            SET @LoadRangeLow = @GTLoad - @LoadRangeWidth;
            SET @LoadRangeHigh = @GTLoad + (@LoadRangeWidth - 0.01);
            
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
                  AND AvgMW BETWEEN @LoadRangeLow AND @LoadRangeHigh
                  AND HeatRate IS NOT NULL
                  AND HeatRate > 0
                  AND HeatRate <= @MaxValidHeatRate
                ORDER BY HeatRate
            ) AS TopSamples;
            
            -- Insert results
            INSERT INTO #HeatRateResults (
                Asset_FK_Id, AssetName, GTLoad, BestArchivedHeatRate, DisplayedAvgHeatRate,
                RecordCount, DateRangeStart, DateRangeEnd, LoadRangeLow, LoadRangeHigh
            )
            VALUES (
                @AssetFKId, @AssetName, @GTLoad, @BestHeatRate, @AvgHeatRate,
                @RecordCount, @DateRangeStart, @DateRangeEnd, @LoadRangeLow, @LoadRangeHigh
            );
            
            -- Print progress
            IF @RecordCount >= @MinRecordsRequired
                PRINT '  Load ' + CAST(@GTLoad AS VARCHAR(10)) + ' MW: ' + 
                      CAST(@RecordCount AS VARCHAR(10)) + ' records, ' +
                      'Best=' + CAST(CAST(@BestHeatRate AS DECIMAL(10,2)) AS VARCHAR(20)) + ', ' +
                      'Avg=' + CAST(CAST(@AvgHeatRate AS DECIMAL(10,2)) AS VARCHAR(20));
            ELSE
                PRINT '  Load ' + CAST(@GTLoad AS VARCHAR(10)) + ' MW: ' + 
                      CAST(@RecordCount AS VARCHAR(10)) + ' records (< ' + 
                      CAST(@MinRecordsRequired AS VARCHAR(10)) + ' required) - SKIPPED';
            
            -- Increment load by 0.5 MW
            SET @GTLoad = @GTLoad + 0.5;
        END;
        
        FETCH NEXT FROM asset_cursor INTO @AssetName;
    END;
    
    CLOSE asset_cursor;
    DEALLOCATE asset_cursor;
    
    PRINT '';
    PRINT '======================================';
    PRINT 'Saving results to CPP_GTHeatRate table...';
    
    -- Merge results into target table
    MERGE INTO dbo.CPP_GTHeatRate AS target
    USING #HeatRateResults AS source
    ON target.AssetName = source.AssetName 
       AND target.FinancialYear = @FinancialYear
       AND target.GTLoad = source.GTLoad
    WHEN MATCHED THEN
        UPDATE SET
            Asset_FK_Id = source.Asset_FK_Id,
            BestArchivedHeatRate = source.BestArchivedHeatRate,
            DisplayedAvgHeatRate = source.DisplayedAvgHeatRate,
            RecordCount = source.RecordCount,
            DateRangeStart = source.DateRangeStart,
            DateRangeEnd = source.DateRangeEnd,
            LoadRangeLow = source.LoadRangeLow,
            LoadRangeHigh = source.LoadRangeHigh,
            UtilityId = @UtilityId,
            FreeSteamFactor = @FreeSteamFactor,
            UpdatedDate = GETDATE()
    WHEN NOT MATCHED THEN
        INSERT (
            Asset_FK_Id, AssetName, UtilityId, FinancialYear, GTLoad, BestArchivedHeatRate, DisplayedAvgHeatRate,
            FreeSteamFactor, RecordCount, DateRangeStart, DateRangeEnd,
            LoadRangeLow, LoadRangeHigh, CreatedDate, UpdatedDate
        )
        VALUES (
            source.Asset_FK_Id, source.AssetName, @UtilityId, @FinancialYear, source.GTLoad, source.BestArchivedHeatRate, 
            source.DisplayedAvgHeatRate, @FreeSteamFactor, source.RecordCount,
            source.DateRangeStart, source.DateRangeEnd, source.LoadRangeLow, source.LoadRangeHigh,
            GETDATE(), GETDATE()
        );

    UPDATE gt
    SET Asset_FK_Id = p.AssetId
    FROM dbo.CPP_GTHeatRate gt
    CROSS APPLY (
        SELECT TOP 1 AssetId
        FROM dbo.PowerGenerationAssets p
        WHERE p.AssetName = gt.AssetName
        ORDER BY p.AssetId
    ) p
    WHERE gt.Asset_FK_Id IS NULL;
    
    DECLARE @TotalRecords INT;
    SELECT @TotalRecords = COUNT(*) FROM #HeatRateResults;
    
    PRINT 'Completed! Total records saved: ' + CAST(@TotalRecords AS VARCHAR(10));
    PRINT '======================================';
    
    -- Cleanup
    DROP TABLE #HeatRateResults;
    
    -- Return summary
    SELECT 
        AssetName,
        FinancialYear,
        COUNT(*) AS TotalLoadPoints,
        SUM(CASE WHEN BestArchivedHeatRate > 0 THEN 1 ELSE 0 END) AS ValidLoadPoints,
        MIN(GTLoad) AS MinLoad,
        MAX(GTLoad) AS MaxLoad,
        AVG(CASE WHEN BestArchivedHeatRate > 0 THEN BestArchivedHeatRate ELSE NULL END) AS AvgBestHeatRate,
        MIN(DateRangeStart) AS DataFrom,
        MAX(DateRangeEnd) AS DataTo
    FROM dbo.CPP_GTHeatRate
    WHERE FinancialYear = @FinancialYear
    GROUP BY AssetName, FinancialYear
    ORDER BY AssetName;
    
END;
GO
/****** Object:  StoredProcedure [dbo].[CPP_CalculateGTHeatRate_ByDateRange]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE PROCEDURE [dbo].[CPP_CalculateGTHeatRate_ByDateRange]
    @StartDate DATE,
    @EndDate DATE,
    @AssetName NVARCHAR(100)  -- 'GT-1', 'GT-2', or 'GT-3'
AS
BEGIN
    SET NOCOUNT ON;
    
    -- Constants (same as original SP)
    DECLARE @UtilityId NVARCHAR(50) = '310027907';
    DECLARE @FreeSteamFactor DECIMAL(10,4) = 1.97;
    DECLARE @MinRecordsRequired INT = 1;  -- Minimum 1 sample
    DECLARE @MaxRecordsToUse INT = 50;  -- Maximum 50 samples
    DECLARE @MaxValidHeatRate DECIMAL(18,4) = 10000;
    DECLARE @LoadRangeWidth DECIMAL(10,2) = 0.25;  -- ±0.25 MW
    
    -- Variables for loop
    DECLARE @GTLoad DECIMAL(10,2);
    DECLARE @LoadRangeLow DECIMAL(10,2);
    DECLARE @LoadRangeHigh DECIMAL(10,2);
    
    -- Variables for calculation results
    DECLARE @RecordCount INT;
    DECLARE @BestHeatRate DECIMAL(18,4);
    DECLARE @AvgHeatRate DECIMAL(18,4);
    DECLARE @DateRangeStart DATETIME;
    DECLARE @DateRangeEnd DATETIME;
    DECLARE @MinLoadHeatRate DECIMAL(18,4);
    DECLARE @MinLoadAvgHeatRate DECIMAL(18,4);
    
    -- Temp table to hold calculation results
    CREATE TABLE #HeatRateResults (
        AssetName NVARCHAR(100),
        GTLoad DECIMAL(10,2),
        BestArchivedHeatRate DECIMAL(18,4),
        DisplayedAvgHeatRate DECIMAL(18,4),
        FreeSteamFactor DECIMAL(10,4),
        RecordCount INT,
        DateRangeStart DATETIME,
        DateRangeEnd DATETIME,
        LoadRangeLow DECIMAL(10,2),
        LoadRangeHigh DECIMAL(10,2)
    );
    
    PRINT 'Starting GT Heat Rate Calculation for Date Range...';
    PRINT '====================================================';
    PRINT 'Asset: ' + @AssetName;
    PRINT 'Date Range: ' + CONVERT(VARCHAR(10), @StartDate, 120) + ' to ' + CONVERT(VARCHAR(10), @EndDate, 120);
    PRINT '====================================================';
    
    -- Validate asset name
    IF @AssetName NOT IN ('GT-1', 'GT-2', 'GT-3')
    BEGIN
        PRINT 'ERROR: Invalid asset name. Must be GT-1, GT-2, or GT-3';
        RETURN;
    END
    
    -- First, get heat rates for minimum load (6 MW) to use for 0 MW
    SET @GTLoad = 6.0;
    SET @LoadRangeLow = @GTLoad - @LoadRangeWidth;
    SET @LoadRangeHigh = @GTLoad + (@LoadRangeWidth - 0.01);
    
    SELECT 
        @MinLoadHeatRate = CASE 
            WHEN COUNT(*) >= @MinRecordsRequired THEN MIN(HeatRate)
            ELSE 0
        END,
        @MinLoadAvgHeatRate = CASE 
            WHEN COUNT(*) >= @MinRecordsRequired THEN AVG(HeatRate)
            ELSE 0
        END
    FROM (
        SELECT TOP (@MaxRecordsToUse) HeatRate
        FROM dbo.CPP_NMD_FCNA_FuelBill
        WHERE AssetName = @AssetName
          AND Date BETWEEN @StartDate AND @EndDate  -- DATE FILTER
          AND AvgMW BETWEEN @LoadRangeLow AND @LoadRangeHigh
          AND HeatRate IS NOT NULL
          AND HeatRate > 0
          AND HeatRate <= @MaxValidHeatRate
        ORDER BY HeatRate
    ) AS TopSamples;
    
    -- Insert 0 MW row with same heat rate as minimum load (6 MW)
    INSERT INTO #HeatRateResults (
        AssetName, GTLoad, BestArchivedHeatRate, DisplayedAvgHeatRate, FreeSteamFactor,
        RecordCount, DateRangeStart, DateRangeEnd, LoadRangeLow, LoadRangeHigh
    )
    VALUES (
        @AssetName, 0.0, @MinLoadHeatRate, @MinLoadAvgHeatRate, @FreeSteamFactor,
        0, NULL, NULL, 0.0, 0.0
    );
    
    PRINT '  Added 0 MW row with heat rate from 6 MW data';
    
    -- Loop through load points: 6.0, 6.5, 7.0, ..., 22.0
    SET @GTLoad = 6.0;
    
    WHILE @GTLoad <= 22.0
    BEGIN
        -- Calculate load range
        SET @LoadRangeLow = @GTLoad - @LoadRangeWidth;
        SET @LoadRangeHigh = @GTLoad + (@LoadRangeWidth - 0.01);
        
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
              AND Date BETWEEN @StartDate AND @EndDate  -- DATE FILTER
              AND AvgMW BETWEEN @LoadRangeLow AND @LoadRangeHigh
              AND HeatRate IS NOT NULL
              AND HeatRate > 0
              AND HeatRate <= @MaxValidHeatRate
            ORDER BY HeatRate
        ) AS TopSamples;
        
        -- Insert results
        INSERT INTO #HeatRateResults (
            AssetName, GTLoad, BestArchivedHeatRate, DisplayedAvgHeatRate, FreeSteamFactor,
            RecordCount, DateRangeStart, DateRangeEnd, LoadRangeLow, LoadRangeHigh
        )
        VALUES (
            @AssetName, @GTLoad, @BestHeatRate, @AvgHeatRate, @FreeSteamFactor,
            @RecordCount, @DateRangeStart, @DateRangeEnd, @LoadRangeLow, @LoadRangeHigh
        );
        
        -- Print progress
        IF @RecordCount >= @MinRecordsRequired
            PRINT '  Load ' + CAST(@GTLoad AS VARCHAR(10)) + ' MW: ' + 
                  CAST(@RecordCount AS VARCHAR(10)) + ' records, ' +
                  'Best=' + CAST(CAST(@BestHeatRate AS DECIMAL(10,2)) AS VARCHAR(20)) + ', ' +
                  'Avg=' + CAST(CAST(@AvgHeatRate AS DECIMAL(10,2)) AS VARCHAR(20));
        ELSE
            PRINT '  Load ' + CAST(@GTLoad AS VARCHAR(10)) + ' MW: ' + 
                  CAST(@RecordCount AS VARCHAR(10)) + ' records (< ' + 
                  CAST(@MinRecordsRequired AS VARCHAR(10)) + ' required) - SKIPPED';
        
        -- Increment load by 0.5 MW
        SET @GTLoad = @GTLoad + 0.5;
    END;
    
    PRINT '';
    PRINT '====================================================';
    PRINT 'Calculation completed. Returning results...';
    PRINT '====================================================';
    
    -- Return results (NO TABLE INSERT)
    -- Note: Returning DisplayedAvgHeatRate as HeatRate to match Java service expectations
    SELECT 
        AssetName,
        GTLoad,
        BestArchivedHeatRate,
        DisplayedAvgHeatRate AS HeatRate,  -- Alias to match Java service column name
        FreeSteamFactor,
        RecordCount,
        DateRangeStart,
        DateRangeEnd,
        LoadRangeLow,
        LoadRangeHigh
    FROM #HeatRateResults
    ORDER BY GTLoad;
    
    -- Cleanup
    DROP TABLE #HeatRateResults;
    
END;
GO
/****** Object:  StoredProcedure [dbo].[CPP_CalculateHRSGHeatRate]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE PROCEDURE [dbo].[CPP_CalculateHRSGHeatRate]
AS
BEGIN
    SET NOCOUNT ON;
    
    DECLARE @AssetName NVARCHAR(100);
    DECLARE @HRSGLoad DECIMAL(10,2);
    DECLARE @LoadRangeLow DECIMAL(10,2);
    DECLARE @LoadRangeHigh DECIMAL(10,2);
    DECLARE @BestHeatRate DECIMAL(18,4);
    DECLARE @AvgHeatRate DECIMAL(18,4);
    DECLARE @RecordCount INT;
    DECLARE @DateStart DATETIME;
    DECLARE @DateEnd DATETIME;
    DECLARE @UtilityId NVARCHAR(50) = '310027907';
    DECLARE @FreeSteamFactor DECIMAL(10,4) = 1.97;
    DECLARE @MinRecordsRequired INT = 1;  -- Minimum 1 sample
    DECLARE @MaxRecordsToUse INT = 50;  -- Maximum 50 samples
    DECLARE @MaxValidHeatRate DECIMAL(18,4) = 10000;
    DECLARE @FinancialYear NVARCHAR(20) = '2026-27';
    
    DECLARE @TotalRecordsProcessed INT = 0;
    DECLARE @TotalLoadsCalculated INT = 0;
    DECLARE @MinLoadHeatRate DECIMAL(18,4);
    DECLARE @MinLoadAvgHeatRate DECIMAL(18,4);
    
    PRINT '============================================================';
    PRINT 'CPP HRSG Heat Rate Calculation';
    PRINT 'Started at: ' + CONVERT(VARCHAR, GETDATE(), 120);
    PRINT '============================================================';
    
    -- Create temp table to store results
    CREATE TABLE #HeatRateResults (
        AssetName NVARCHAR(100),
        HRSGLoad DECIMAL(10,2),
        LoadRangeLow DECIMAL(10,2),
        LoadRangeHigh DECIMAL(10,2),
        BestArchivedHeatRate DECIMAL(18,4),
        DisplayedAvgHeatRate DECIMAL(18,4),
        RecordCount INT,
        DateRangeStart DATETIME,
        DateRangeEnd DATETIME
    );
    
    -- Cursor for HRSG assets
    DECLARE asset_cursor CURSOR FOR
        SELECT DISTINCT 'HRSG-1'
        UNION SELECT 'HRSG-2'
        UNION SELECT 'HRSG-3';
    
    OPEN asset_cursor;
    FETCH NEXT FROM asset_cursor INTO @AssetName;
    
    WHILE @@FETCH_STATUS = 0
    BEGIN
        PRINT '';
        PRINT 'Processing Asset: ' + @AssetName;
        PRINT '------------------------------------------------------------';
        
        -- First, get heat rates for minimum load (30 MT) to use for 0 MT
        SET @HRSGLoad = 30.0;
        SET @LoadRangeLow = 30.0;
        SET @LoadRangeHigh = 39.99;
        
        SELECT 
            @MinLoadHeatRate = CASE WHEN COUNT(*) >= @MinRecordsRequired THEN MIN(HeatRate) ELSE 0 END,
            @MinLoadAvgHeatRate = CASE WHEN COUNT(*) >= @MinRecordsRequired THEN AVG(HeatRate) ELSE 0 END
        FROM (
            SELECT TOP (@MaxRecordsToUse) HeatRate
            FROM dbo.CPP_NMD_FCNA_FuelBill
            WHERE AssetName = @AssetName
              AND AvgMW BETWEEN @LoadRangeLow AND @LoadRangeHigh
              AND HeatRate IS NOT NULL
              AND HeatRate > 0
              AND HeatRate <= @MaxValidHeatRate
            ORDER BY HeatRate  -- Order by heat rate to get best samples
        ) AS TopSamples;
        
        -- Insert 0 MT row with same heat rate as minimum load (30 MT)
        INSERT INTO #HeatRateResults (
            AssetName, HRSGLoad, LoadRangeLow, LoadRangeHigh,
            BestArchivedHeatRate, DisplayedAvgHeatRate, RecordCount,
            DateRangeStart, DateRangeEnd
        )
        VALUES (
            @AssetName, 0.0, 0.0, 0.0,
            @MinLoadHeatRate, @MinLoadAvgHeatRate, 0,
            NULL, NULL
        );
        
        PRINT '  Added 0 MT row with heat rate from 30 MT data';
        
        -- Loop through load points: 30 MT to 130 MT in 10 MT increments
        SET @HRSGLoad = 30.0;
        
        WHILE @HRSGLoad <= 130.0
        BEGIN
            -- Calculate load range: 10 MT span (e.g., 30-39, 40-49, etc.)
            SET @LoadRangeLow = @HRSGLoad;
            SET @LoadRangeHigh = @HRSGLoad + 9.99;  -- 39.99 instead of 40 to avoid overlap
            
            -- Query data from CPP_NMD_FCNA_FuelBill for this load range (limit to top 50 best samples)
            SELECT 
                @RecordCount = COUNT(*),
                @BestHeatRate = CASE WHEN COUNT(*) >= @MinRecordsRequired THEN MIN(HeatRate) ELSE 0 END,
                @AvgHeatRate = CASE WHEN COUNT(*) >= @MinRecordsRequired THEN AVG(HeatRate) ELSE 0 END,
                @DateStart = MIN(Date),
                @DateEnd = MAX(Date)
            FROM (
                SELECT TOP (@MaxRecordsToUse) HeatRate, Date
                FROM dbo.CPP_NMD_FCNA_FuelBill
                WHERE AssetName = @AssetName
                  AND AvgMW BETWEEN @LoadRangeLow AND @LoadRangeHigh
                  AND HeatRate IS NOT NULL
                  AND HeatRate > 0
                  AND HeatRate <= @MaxValidHeatRate
                ORDER BY HeatRate  -- Order by heat rate to get best samples
            ) AS TopSamples;
            
            -- Insert into temp table
            INSERT INTO #HeatRateResults (
                AssetName, HRSGLoad, LoadRangeLow, LoadRangeHigh,
                BestArchivedHeatRate, DisplayedAvgHeatRate, RecordCount,
                DateRangeStart, DateRangeEnd
            )
            VALUES (
                @AssetName, @HRSGLoad, @LoadRangeLow, @LoadRangeHigh,
                @BestHeatRate, @AvgHeatRate, @RecordCount,
                @DateStart, @DateEnd
            );
            
            SET @TotalRecordsProcessed = @TotalRecordsProcessed + @RecordCount;
            SET @TotalLoadsCalculated = @TotalLoadsCalculated + 1;
            
            -- Print progress every 20 MT
            IF @HRSGLoad % 20 = 0
                PRINT '  Processed load ' + CAST(@HRSGLoad AS VARCHAR) + ' MT - Records found: ' + CAST(@RecordCount AS VARCHAR);
            
            -- Increment by 10 MT
            SET @HRSGLoad = @HRSGLoad + 10.0;
        END
        
        PRINT '  Completed ' + @AssetName + ' - Total loads processed: 12 (0, 30-130)';
        
        FETCH NEXT FROM asset_cursor INTO @AssetName;
    END
    
    CLOSE asset_cursor;
    DEALLOCATE asset_cursor;
    
    PRINT '';
    PRINT 'Merging results into CPP_HRSGHeatRate table...';
    
    -- MERGE results into main table
    MERGE INTO dbo.CPP_HRSGHeatRate AS target
    USING #HeatRateResults AS source
    ON target.AssetName = source.AssetName 
       AND target.FinancialYear = @FinancialYear
       AND target.HRSGLoad = source.HRSGLoad
    WHEN MATCHED THEN
        UPDATE SET
            BestArchivedHeatRate = source.BestArchivedHeatRate,
            DisplayedAvgHeatRate = source.DisplayedAvgHeatRate,
            RecordCount = source.RecordCount,
            DateRangeStart = source.DateRangeStart,
            DateRangeEnd = source.DateRangeEnd,
            LoadRangeLow = source.LoadRangeLow,
            LoadRangeHigh = source.LoadRangeHigh,
            UpdatedDate = GETDATE()
    WHEN NOT MATCHED THEN
        INSERT (
            AssetName, UtilityId, FinancialYear, HRSGLoad,
            BestArchivedHeatRate, DisplayedAvgHeatRate, FreeSteamFactor,
            RecordCount, DateRangeStart, DateRangeEnd,
            LoadRangeLow, LoadRangeHigh
        )
        VALUES (
            source.AssetName, @UtilityId, @FinancialYear, source.HRSGLoad,
            source.BestArchivedHeatRate, source.DisplayedAvgHeatRate, @FreeSteamFactor,
            source.RecordCount, source.DateRangeStart, source.DateRangeEnd,
            source.LoadRangeLow, source.LoadRangeHigh
        );
    
    -- Clean up
    DROP TABLE #HeatRateResults;
    
    PRINT '';
    PRINT '============================================================';
    PRINT 'HRSG Heat Rate Calculation Completed';
    PRINT 'Total records processed: ' + CAST(@TotalRecordsProcessed AS VARCHAR);
    PRINT 'Total load points calculated: ' + CAST(@TotalLoadsCalculated AS VARCHAR);
    PRINT 'Completed at: ' + CONVERT(VARCHAR, GETDATE(), 120);
    PRINT '============================================================';
    
    -- Return summary
    SELECT 
        AssetName,
        COUNT(*) AS LoadPointsCalculated,
        SUM(RecordCount) AS TotalRecordsUsed,
        SUM(CASE WHEN BestArchivedHeatRate > 0 THEN 1 ELSE 0 END) AS ValidLoadPoints,
        MIN(HRSGLoad) AS MinLoad,
        MAX(HRSGLoad) AS MaxLoad,
        MIN(DateRangeStart) AS EarliestDate,
        MAX(DateRangeEnd) AS LatestDate
    FROM dbo.CPP_HRSGHeatRate
    WHERE FinancialYear = @FinancialYear
    GROUP BY AssetName
    ORDER BY AssetName;
    
END
GO
/****** Object:  StoredProcedure [dbo].[CPP_CalculateHRSGHeatRate_ByDateRange]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- =============================================
-- Stored Procedure: CPP_CalculateHRSGHeatRate_ByDateRange
-- Description: Calculate HRSG heat rates for different load points based on date range
-- Author: System
-- Date: 2026-02-23
-- =============================================
-- 
-- Purpose:
-- Calculates HRSG heat rates from FCNA fuel bill data for specific date ranges.
-- Groups data by load ranges (40 MT, 50 MT, 60 MT, etc.) and calculates average heat rate.
-- Uses AvgMW (hourly average) for load bucketing, same as GT heat rate calculation.
--
-- Parameters:
-- @StartDate - Start date for calculation (YYYY-MM-DD)
-- @EndDate - End date for calculation (YYYY-MM-DD)
-- @AssetName - HRSG asset name (e.g., 'HRSG-1', 'HRSG-2', 'HRSG-3')
--
-- Returns:
-- HRSGLoad (MT/hr), HeatRate (kcal/MT)
-- =============================================

CREATE   PROCEDURE [dbo].[CPP_CalculateHRSGHeatRate_ByDateRange]
    @StartDate DATE,
    @EndDate DATE,
    @AssetName NVARCHAR(50)
AS
BEGIN
    SET NOCOUNT ON;
    
    -- Declare variables for load range configuration
    DECLARE @MinLoad DECIMAL(10,2) = 30.0;  -- Starting load: 30 MT/hr
    DECLARE @LoadInterval DECIMAL(10,2) = 5.0;  -- Load interval: 5 MT/hr
    DECLARE @MaxLoad DECIMAL(10,2) = 135.0;  -- Maximum load: 135 MT/hr (max HRSG capacity)
    DECLARE @MinRecordsRequired INT = 3;  -- Minimum 3 samples
    DECLARE @MaxRecordsToUse INT = 50;  -- Maximum 50 samples (same as GT)
    DECLARE @MaxValidHeatRate DECIMAL(18,4) = 10000;  -- Maximum valid heat rate
    DECLARE @FreeSteamFactor DECIMAL(10,4) = 1.97;  -- Free steam factor (same as GT)
    DECLARE @DefaultHeatRateBelow55 DECIMAL(18,4) = 750.0;  -- Default heat rate for load < 55 MT/hr when samples <= 3
    DECLARE @DefaultHeatRateAbove55 DECIMAL(18,4) = 738.0;  -- Default heat rate for load >= 55 MT/hr when samples <= 3
    
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
GO
/****** Object:  StoredProcedure [dbo].[CPP_CarryForwardFixedConsumption]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE   PROCEDURE [dbo].[CPP_CarryForwardFixedConsumption]
    @CostCenterId NVARCHAR(MAX),
    @CurrentYearStart NVARCHAR(4),
    @CurrentYearEnd NVARCHAR(4),
    @NextYearStart NVARCHAR(4),
    @NextYearEnd NVARCHAR(4)
AS
BEGIN
    SET NOCOUNT ON;

    BEGIN TRY
        BEGIN TRANSACTION;

        -------------------------------------------------------------------------
        -- Step 1: Create mapping between current year and next year FYM IDs
        -- Maps same month across different years (Apr2025 -> Apr2026, etc.)
        -- CostCenter remains SAME across all years
        -------------------------------------------------------------------------
        DECLARE @FYMMapping TABLE (
            CurrentFymId NVARCHAR(MAX),
            NextFymId NVARCHAR(MAX),
            Month INT
        );

        INSERT INTO @FYMMapping (CurrentFymId, NextFymId, Month)
        SELECT 
            current_fym.Id AS CurrentFymId,
            next_fym.Id AS NextFymId,
            current_fym.Month
        FROM [RIL.AOP].dbo.FinancialYearMonth current_fym
        INNER JOIN [RIL.AOP].dbo.FinancialYearMonth next_fym
            ON current_fym.Month = next_fym.Month
        WHERE (current_fym.Year = @CurrentYearStart AND current_fym.Month >= 4)
           OR (current_fym.Year = @CurrentYearEnd AND current_fym.Month <= 3)
        AND (next_fym.Year = @NextYearStart AND next_fym.Month >= 4)
           OR (next_fym.Year = @NextYearEnd AND next_fym.Month <= 3);

        -------------------------------------------------------------------------
        -- Step 2: UPSERT - Update existing or Insert new records
        -- Cost Center and Cost Center ID are COMMON across all years
        -- Only Financial Year Month changes year to year
        -------------------------------------------------------------------------
        DECLARE @UpdatedCount INT = 0;
        DECLARE @InsertedCount INT = 0;
        DECLARE @CostCenterIdUUID UNIQUEIDENTIFIER = CAST(@CostCenterId AS UNIQUEIDENTIFIER);

        -- Update existing records in next year with current year values
        -- SAME cost center, SAME utility (NormParameter), but DIFFERENT FYM (different month year)
        UPDATE ufc_next
        SET ufc_next.ConsumptionValue = ufc_current.ConsumptionValue
        FROM [RIL.AOP].dbo.UtilityFixedConsumption ufc_next
        INNER JOIN @FYMMapping mapping
            ON ufc_next.FinancialYearMonth_FK_Id = mapping.NextFymId
        INNER JOIN [RIL.AOP].dbo.UtilityFixedConsumption ufc_current
            ON ufc_current.FinancialYearMonth_FK_Id = mapping.CurrentFymId
            AND ufc_current.NormParameter_FK_Id = ufc_next.NormParameter_FK_Id
            AND ufc_current.CostCenter_FK_Id = ufc_next.CostCenter_FK_Id
        WHERE ufc_next.CostCenter_FK_Id = @CostCenterIdUUID
        AND ufc_current.CostCenter_FK_Id = @CostCenterIdUUID;

        SET @UpdatedCount = @@ROWCOUNT;

        -- Insert new records for next year that don't exist yet
        -- Uses SAME cost center and utility as current year
        INSERT INTO [RIL.AOP].dbo.UtilityFixedConsumption 
            (FinancialYearMonth_FK_Id, NormParameter_FK_Id, CostCenter_FK_Id, ConsumptionValue)
        SELECT 
            mapping.NextFymId AS FinancialYearMonth_FK_Id,
            ufc.NormParameter_FK_Id,
            ufc.CostCenter_FK_Id,      -- SAME cost center (no change)
            ufc.ConsumptionValue
        FROM [RIL.AOP].dbo.UtilityFixedConsumption ufc
        INNER JOIN @FYMMapping mapping
            ON ufc.FinancialYearMonth_FK_Id = mapping.CurrentFymId
        WHERE ufc.CostCenter_FK_Id = @CostCenterIdUUID
        AND NOT EXISTS (
            -- Check if this record already exists for next year
            -- SAME cost center, SAME utility, but NEXT year's FYM
            SELECT 1 FROM [RIL.AOP].dbo.UtilityFixedConsumption ufc_check
            WHERE ufc_check.FinancialYearMonth_FK_Id = mapping.NextFymId
            AND ufc_check.NormParameter_FK_Id = ufc.NormParameter_FK_Id
            AND ufc_check.CostCenter_FK_Id = @CostCenterIdUUID
        );

        SET @InsertedCount = @@ROWCOUNT;

        COMMIT TRANSACTION;

        -- Return success with counts
        SELECT @UpdatedCount AS RecordsUpdated, @InsertedCount AS RecordsInserted, (@UpdatedCount + @InsertedCount) AS TotalRecordsCarriedForward;

    END TRY
    BEGIN CATCH
        ROLLBACK TRANSACTION;
        
        -- Raise error with details
        DECLARE @ErrorMessage NVARCHAR(MAX) = ERROR_MESSAGE();
        DECLARE @ErrorSeverity INT = ERROR_SEVERITY();
        DECLARE @ErrorState INT = ERROR_STATE();
        
        RAISERROR (@ErrorMessage, @ErrorSeverity, @ErrorState);
    END CATCH;

END;
GO
/****** Object:  StoredProcedure [dbo].[CPP_CleanupFixedConsumption]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

-- Cleanup script to delete fixed consumption data for a specific financial year
-- Use this to clean up old/random data before implementing auto carry-forward

CREATE PROCEDURE [dbo].[CPP_CleanupFixedConsumption]
    @FinancialYear NVARCHAR(20),  -- e.g., '2026-27'
    @DryRun BIT = 1               -- 1 = Show what will be deleted, 0 = Actually delete
AS
BEGIN
    SET NOCOUNT ON;

    BEGIN TRY
        DECLARE @StartYear INT,
                @EndYear INT,
                @RecordsToDelete INT;

        -- Parse: '2026-27' -> @StartYear = 2026, @EndYear = 2027
        SET @StartYear = CAST(LEFT(@FinancialYear, 4) AS INT);
        SET @EndYear   = CAST(LEFT(@FinancialYear, 2) + RIGHT(@FinancialYear, 2) AS INT);

        -- Find all records to be deleted
        SELECT @RecordsToDelete = COUNT(*)
        FROM [RIL.AOP].dbo.UtilityFixedConsumption ufc
        INNER JOIN [RIL.AOP].dbo.FinancialYearMonth fym
            ON fym.Id = ufc.FinancialYearMonth_FK_Id
        WHERE (
            (fym.Year = @StartYear AND fym.Month >= 4)
            OR (fym.Year = @EndYear AND fym.Month <= 3)
        );

        IF @DryRun = 1
        BEGIN
            -- DRY RUN: Show what will be deleted
            PRINT '=== DRY RUN: Records that will be DELETED for ' + @FinancialYear + ' ==='
            PRINT 'Total records to delete: ' + CAST(@RecordsToDelete AS NVARCHAR(10));
            PRINT '';
            
            SELECT 
                'DELETE' AS Action,
                cc.CostCenterName,
                np.Name AS UtilityName,
                fym.Month,
                fym.Year,
                ufc.ConsumptionValue,
                ufc.Id AS UtilityFixedConsumptionId
            FROM [RIL.AOP].dbo.UtilityFixedConsumption ufc
            INNER JOIN [RIL.AOP].dbo.FinancialYearMonth fym
                ON fym.Id = ufc.FinancialYearMonth_FK_Id
            INNER JOIN [RIL.AOP].dbo.CPPCostCenters cc
                ON cc.CostCenterId = ufc.CostCenter_FK_Id
            INNER JOIN [RIL.AOP].dbo.NormParameters np
                ON np.Id = ufc.NormParameter_FK_Id
            WHERE (
                (fym.Year = @StartYear AND fym.Month >= 4)
                OR (fym.Year = @EndYear AND fym.Month <= 3)
            )
            ORDER BY cc.CostCenterName, np.Name, fym.Month;
            
            PRINT '';
            PRINT 'To actually DELETE these records, run this SP again with @DryRun = 0';
        END
        ELSE
        BEGIN
            -- ACTUAL DELETE
            BEGIN TRANSACTION;
            
            DELETE FROM [RIL.AOP].dbo.UtilityFixedConsumption
            WHERE FinancialYearMonth_FK_Id IN (
                SELECT fym.Id
                FROM [RIL.AOP].dbo.FinancialYearMonth fym
                WHERE (
                    (fym.Year = @StartYear AND fym.Month >= 4)
                    OR (fym.Year = @EndYear AND fym.Month <= 3)
                )
            );

            DECLARE @DeletedCount INT = @@ROWCOUNT;
            
            COMMIT TRANSACTION;
            
            PRINT '=== DELETION COMPLETE ==='
            PRINT 'Financial Year: ' + @FinancialYear;
            PRINT 'Records deleted: ' + CAST(@DeletedCount AS NVARCHAR(10));
            PRINT 'Next time you fetch data for ' + @FinancialYear + ', it will auto carry-forward from ' + CAST(@StartYear - 1 AS NVARCHAR(4)) + '-' + RIGHT(CAST(@StartYear AS NVARCHAR(4)), 2);
        END;

    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;
            
        DECLARE @ErrorMessage NVARCHAR(MAX) = ERROR_MESSAGE();
        DECLARE @ErrorSeverity INT = ERROR_SEVERITY();
        DECLARE @ErrorState INT = ERROR_STATE();
        
        RAISERROR (@ErrorMessage, @ErrorSeverity, @ErrorState);
    END CATCH;
END;
GO
/****** Object:  StoredProcedure [dbo].[CPP_Get_ImportPowerCapacity]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE PROCEDURE [dbo].[CPP_Get_ImportPowerCapacity]
(
    @CppPlantId UNIQUEIDENTIFIER,
    @FinancialYear NVARCHAR(10)
)
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        ips.Id AS SourceId,
        ips.SourceName,
        ips.MaterialCode,
        COALESCE(np.SAPMaterialCode, '') AS SAPMaterialCode,
        COALESCE(np.Name, '') AS UtilityName,
        COALESCE(p.Name, '') AS PlantName,
        
        -- Capacity values (in MW)
        COALESCE(ipc.Apr, 0) AS Apr,
        COALESCE(ipc.May, 0) AS May,
        COALESCE(ipc.Jun, 0) AS Jun,
        COALESCE(ipc.Jul, 0) AS Jul,
        COALESCE(ipc.Aug, 0) AS Aug,
        COALESCE(ipc.Sep, 0) AS Sep,
        COALESCE(ipc.Oct, 0) AS Oct,
        COALESCE(ipc.Nov, 0) AS Nov,
        COALESCE(ipc.Dec, 0) AS Dec,
        COALESCE(ipc.Jan, 0) AS Jan,
        COALESCE(ipc.Feb, 0) AS Feb,
        COALESCE(ipc.Mar, 0) AS Mar,
        
        COALESCE(ipc.UOM, 'MW') AS UOM,
        COALESCE(ipc.Remarks, '') AS Remarks

    FROM CPPImportPowerSourceMapping ips
    
    LEFT JOIN CPPImportPowerCapacity ipc
        ON ipc.ImportPowerSource_FK_Id = ips.Id
        AND ipc.FinancialYear = @FinancialYear
    
    LEFT JOIN NormParameters np ON np.Id = ips.NormParameter_FK_Id
    LEFT JOIN Plants p ON p.Id = ips.Plant_FK_Id

    WHERE ips.CPPPlant_FK_Id = @CppPlantId
      AND ips.IsActive = 1
    
    ORDER BY ips.SourceName;
END

GO
/****** Object:  StoredProcedure [dbo].[CPP_Get_ImportPowerOperationalHours]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE PROCEDURE [dbo].[CPP_Get_ImportPowerOperationalHours]
(
    @CppPlantId UNIQUEIDENTIFIER,
    @FinancialYear NVARCHAR(10)
)
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        ips.Id AS SourceId,
        ips.SourceName,
        ips.MaterialCode,
        COALESCE(np.SAPMaterialCode, '') AS SAPMaterialCode,
        COALESCE(np.Name, '') AS UtilityName,
        COALESCE(p.Name, '') AS PlantName,
        
        -- Operational Hours (in hours)
        COALESCE(ipoh.Apr, 0) AS Apr,
        COALESCE(ipoh.May, 0) AS May,
        COALESCE(ipoh.Jun, 0) AS Jun,
        COALESCE(ipoh.Jul, 0) AS Jul,
        COALESCE(ipoh.Aug, 0) AS Aug,
        COALESCE(ipoh.Sep, 0) AS Sep,
        COALESCE(ipoh.Oct, 0) AS Oct,
        COALESCE(ipoh.Nov, 0) AS Nov,
        COALESCE(ipoh.Dec, 0) AS Dec,
        COALESCE(ipoh.Jan, 0) AS Jan,
        COALESCE(ipoh.Feb, 0) AS Feb,
        COALESCE(ipoh.Mar, 0) AS Mar,
        
        COALESCE(ipoh.Remarks, '') AS Remarks

    FROM CPPImportPowerSourceMapping ips
    
    LEFT JOIN CPPImportPowerOperationalHours ipoh
        ON ipoh.ImportPowerSource_FK_Id = ips.Id
        AND ipoh.FinancialYear = @FinancialYear
    
    LEFT JOIN NormParameters np ON np.Id = ips.NormParameter_FK_Id
    LEFT JOIN Plants p ON p.Id = ips.Plant_FK_Id

    WHERE ips.CPPPlant_FK_Id = @CppPlantId
      AND ips.IsActive = 1
    
    ORDER BY ips.SourceName;
END

GO
/****** Object:  StoredProcedure [dbo].[CPP_GetCPPNorms]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE   PROCEDURE [dbo].[CPP_GetCPPNorms]
(
    @CPPPlantId UNIQUEIDENTIFIER,
    @FinancialYear NVARCHAR(20)
)
AS
BEGIN
    SET NOCOUNT ON;

    ;WITH GeneratingPlants AS
    (
        SELECT AssociatedPlant_FK_Id AS GeneratingPlantId
        FROM PowerGenerationPlantsMapping
        WHERE CPPPlantId = @CPPPlantId
    ),
    NormData AS
    (
        SELECT 
            nh.Id AS NormHeaderId,
            nh.Plant_FK_Id AS GeneratingPlantId,
            nh.PlantCode,
            nh.UtilityName,
            nh.UtilityId,
            nh.UtilityUOM,
            nh.AccountName,
            nh.MaterialName,
            nh.MaterialId,
            nh.IssuingPlantName,
            nh.IssuingPlant_FK_Id,
            nh.IssuingUOM,
            nh.NormParameter_FK_Id,
            nh.DisplayOrder
        FROM NormsHeader nh
        INNER JOIN GeneratingPlants gp ON gp.GeneratingPlantId = nh.Plant_FK_Id
        WHERE nh.IsActive = 1
            AND nh.AccountName = 'Utilities'  -- Filter for Utilities account only
    ),
    JoinedNorms AS
    (
        SELECT 
            nd.*,
            np.DisplayName AS NormParameterName,
            np.UOM AS ParameterUOM,
            np.SAPMaterialCode,
            np.DisplayOrder AS NormParameterDisplayOrder
        FROM NormData nd
        LEFT JOIN NormParameters np ON nd.NormParameter_FK_Id = np.Id
    ),
    FinalData AS
    (
        SELECT 
            jn.*,
            p.Name AS GeneratingPlantName,
            p.DisplayName AS GeneratingPlantDisplayName,
            p.PlantCode AS GeneratingPlantCode
        FROM JoinedNorms jn
        LEFT JOIN Plants p ON jn.GeneratingPlantId = p.Id
    )
    SELECT
        ROW_NUMBER() OVER (ORDER BY fd.GeneratingPlantName, fd.DisplayOrder, fd.NormParameterDisplayOrder) AS id,
        cn.Id AS cppNormsId,
        fd.NormHeaderId AS normsHeaderFkId,
        fd.GeneratingPlantName AS generatingPlantName,
        fd.UtilityName AS utilityName,
        fd.UtilityId AS utilityId,
        COALESCE(fd.ParameterUOM, fd.UtilityUOM) AS uom,
        fd.AccountName AS accountName,
        fd.MaterialName AS materialName,
        fd.MaterialId AS materialId,
        fd.IssuingPlantName AS issuingPlantName,
        fd.IssuingUOM AS issuingUom,
        cn.AOPYear AS aopYear,
        cn.NormType_FK_Id AS normTypeFkId,
        nt.NormName AS normTypeName,
        cn.Apr_Norms AS aprNorms,
        cn.May_Norms AS mayNorms,
        cn.Jun_Norms AS junNorms,
        cn.Jul_Norms AS julNorms,
        cn.Aug_Norms AS augNorms,
        cn.Sep_Norms AS sepNorms,
        cn.Oct_Norms AS octNorms,
        cn.Nov_Norms AS novNorms,
        cn.Dec_Norms AS decNorms,
        cn.Jan_Norms AS janNorms,
        cn.Feb_Norms AS febNorms,
        cn.Mar_Norms AS marNorms,
        cn.Remarks AS remarks,
        cn.ModifiedBy AS modifiedBy,
        cn.ModifiedDate AS modifiedDate,
        calc.NORSM_Value AS calculatedNorms,
        cn.ApplyActualNormToAll AS applyActualNormToAll
    FROM FinalData fd
    INNER JOIN CPPNorms cn 
        ON cn.NormsHeader_FK_Id = fd.NormHeaderId 
        AND cn.FinancialYear = @FinancialYear
    INNER JOIN NormTypes nt ON nt.Id = cn.NormType_FK_Id
    LEFT JOIN CPP_utilitiesCalculatednorms calc
        ON calc.Plant = fd.PlantCode
        AND calc.Input_Material_Name = fd.MaterialName
        AND calc.Product_Material_Name = fd.UtilityName
        AND calc.FinancialYear = @FinancialYear
    WHERE nt.NormName = 'Fixed'
    ORDER BY fd.GeneratingPlantName, fd.DisplayOrder, fd.NormParameterDisplayOrder;

END
GO
/****** Object:  StoredProcedure [dbo].[CPP_NMD_Get_UtilityPlant_OperationalHours]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE   PROCEDURE [dbo].[CPP_NMD_Get_UtilityPlant_OperationalHours]
(
    @cppPlantId UNIQUEIDENTIFIER,
    @financialYear VARCHAR(7)   -- e.g. '2025-26'
)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @StartYear INT = CAST(LEFT(@financialYear, 4) AS INT);
    DECLARE @EndYear   INT = @StartYear + 1;

    /*-------------------------------------------------------
      Get FinancialYearMonth IDs for Apr–Mar
    -------------------------------------------------------*/
    WITH FYMonths AS (
        SELECT 
            fym.Id,
            fym.Month,
            fym.Year
        FROM FinancialYearMonth fym
        WHERE (fym.Year = @StartYear AND fym.Month BETWEEN 4 AND 12)
           OR (fym.Year = @EndYear   AND fym.Month BETWEEN 1 AND 3)
    )

    /*-------------------------------------------------------
      Final Result
    -------------------------------------------------------*/
    SELECT
    	Max(pga.AssetId) as AssetId,
		Max(pga_ccp.AssetName) as AssetName,
		Max(u.Type) as AssetType,
        u.PlantAsset as UtilityPlantAsset, 
        u.Id AS UtilityPlantAssetId,
        nd.Name AS UtilityDistributed,
		nd.SAPMaterialCode AS UtilityDistributedSAPCode,
        ug.Name AS UtilityGenerated,
		ug.SAPMaterialCode AS UtilityGeneratedSAPCode,
        u.Type,

        MAX(CASE WHEN fm.Month = 4  THEN oh.OperationalHours END) AS Apr,
        MAX(CASE WHEN fm.Month = 5  THEN oh.OperationalHours END) AS May,
        MAX(CASE WHEN fm.Month = 6  THEN oh.OperationalHours END) AS Jun,
        MAX(CASE WHEN fm.Month = 7  THEN oh.OperationalHours END) AS Jul,
        MAX(CASE WHEN fm.Month = 8  THEN oh.OperationalHours END) AS Aug,
        MAX(CASE WHEN fm.Month = 9  THEN oh.OperationalHours END) AS Sep,
        MAX(CASE WHEN fm.Month = 10 THEN oh.OperationalHours END) AS Oct,
        MAX(CASE WHEN fm.Month = 11 THEN oh.OperationalHours END) AS Nov,
        MAX(CASE WHEN fm.Month = 12 THEN oh.OperationalHours END) AS Dec,
        MAX(CASE WHEN fm.Month = 1  THEN oh.OperationalHours END) AS Jan,
        MAX(CASE WHEN fm.Month = 2  THEN oh.OperationalHours END) AS Feb,
        MAX(CASE WHEN fm.Month = 3  THEN oh.OperationalHours END) AS Mar

    FROM UtilityPlantAssets u
        INNER JOIN PowerGenerationAssets pga_ccp
            ON pga_ccp.AssetId = u.PowerGenerationAsset_FK_Id
           AND pga_ccp.CPPPLANT_FK_Id = @cppPlantId

        LEFT JOIN NormParameters nd
            ON nd.Id = u.UtilityDistributed

        LEFT JOIN NormParameters ug
            ON ug.Id = u.UtilityGenerated

        JOIN PowerGenerationAssets pga    -- nodified to simple join
            ON pga.AssetId = u.Linked_OpHrs_Asset

        LEFT JOIN OperationalHours oh
            ON oh.Asset_FK_Id = u.Linked_OpHrs_Asset

        LEFT JOIN FYMonths fm
            ON fm.Id = oh.FinancialMonthId

    WHERE u.FinancialYear = @financialYear

    GROUP BY
        u.PlantAsset,
        u.Id,
        nd.Name,
        ug.Name,
		ug.SAPMaterialCode,
		nd.SAPMaterialCode,
        pga.AssetName,
        u.Type

    ORDER BY u.PlantAsset;

END;
GO
/****** Object:  StoredProcedure [dbo].[CPP_NMD_Get_UtilityPlantAssets]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE   PROCEDURE [dbo].[CPP_NMD_Get_UtilityPlantAssets]
(
    @CppId UNIQUEIDENTIFIER,
    @FinancialYear VARCHAR(10)
)
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        PGA.AssetName,
        UPA.Id as UtilityPlantAssetId,
        UPA.PlantAsset as UtilityPlantAsset,
        NG.Name AS UtilityGenerated,
		NG.SAPMaterialCode as UtilityGeneratedSAPCode,
        ND.Name AS UtilityDistributed,
		ND.SAPMaterialCode as UtilityDistributedSAPCode,
        UPA.Apr,
        UPA.May,
        UPA.Jun,
        UPA.Jul,
        UPA.Aug,
        UPA.Sep,
        UPA.Oct,
        UPA.Nov,
        UPA.Dec,
        UPA.Jan,
        UPA.Feb,
        UPA.Mar,
        UPA.Remarks,
        UPA.Type

    FROM PowerGenerationAssets PGA
    INNER JOIN UtilityPlantAssets UPA
        ON UPA.PowerGenerationAsset_FK_Id = PGA.AssetId

    LEFT JOIN NormParameters NG
        ON NG.Id = UPA.UtilityGenerated

    LEFT JOIN NormParameters ND
        ON ND.Id = UPA.UtilityDistributed

    WHERE PGA.CPPPLANT_FK_Id = @CppId
      AND UPA.FinancialYear = @FinancialYear

	  order by UPA.PlantAsset;
END
GO
/****** Object:  StoredProcedure [dbo].[CPP_NMD_GetAssetCapacity]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO



CREATE     PROCEDURE [dbo].[CPP_NMD_GetAssetCapacity]
(
    @CppId UNIQUEIDENTIFIER,
    @FinancialYear VARCHAR(7)   -- e.g. '2025-26'
)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @StartYear INT = CAST(LEFT(@FinancialYear, 4) AS INT);
    DECLARE @EndYear   INT = CAST('20' + RIGHT(@FinancialYear, 2) AS INT);

    ;WITH FYM AS (
        SELECT
            fym.Id,
            fym.Month,
            fym.Year
        FROM FinancialYearMonth fym
        WHERE
            (fym.Year = @StartYear AND fym.Month BETWEEN 4 AND 12)
         OR (fym.Year = @EndYear   AND fym.Month BETWEEN 1 AND 3)
    )
    SELECT
        -- GenerationPlant
        pga.AssetName,
		pga.AssetId,
        pga.PlantCode,
		

        -- UtilityDistributed (NormType_FK_Id = 2)
        MAX(CASE WHEN np.NormType_FK_Id = 2 THEN np.DisplayName END)     AS UtilityDistributedName,
        MAX(CASE WHEN np.NormType_FK_Id = 2 THEN np.SAPMaterialCode END) AS UtilityDistributedSAP,

        -- UtilityGenerated (NormType_FK_Id = 1)
        MAX(CASE WHEN np.NormType_FK_Id = 1 THEN np.DisplayName END)     AS UtilityGeneratedName,
        MAX(CASE WHEN np.NormType_FK_Id = 1 THEN np.SAPMaterialCode END) AS UtilityGeneratedSAP,

        -- UOM (assuming same per asset)
        MAX(np.UOM) AS UOM,

        -- Fixed Min / Max (same across months)
        MAX(aa.FixedMin) AS FixedMin,
        MAX(aa.FixedMax) AS FixedMax,
		MAX(aa.Remarks) as Remarks,

     -- April
MAX(CASE WHEN fym.Month = 4 THEN aa.MinOperatingCapacity END) AS AprMinCapacity,
MAX(CASE WHEN fym.Month = 4 THEN aa.MaxOperatingCapacity END) AS AprMaxCapacity,

-- May
MAX(CASE WHEN fym.Month = 5 THEN aa.MinOperatingCapacity END) AS MayMinCapacity,
MAX(CASE WHEN fym.Month = 5 THEN aa.MaxOperatingCapacity END) AS MayMaxCapacity,

-- June
MAX(CASE WHEN fym.Month = 6 THEN aa.MinOperatingCapacity END) AS JunMinCapacity,
MAX(CASE WHEN fym.Month = 6 THEN aa.MaxOperatingCapacity END) AS JunMaxCapacity,

-- July
MAX(CASE WHEN fym.Month = 7 THEN aa.MinOperatingCapacity END) AS JulMinCapacity,
MAX(CASE WHEN fym.Month = 7 THEN aa.MaxOperatingCapacity END) AS JulMaxCapacity,

-- August
MAX(CASE WHEN fym.Month = 8 THEN aa.MinOperatingCapacity END) AS AugMinCapacity,
MAX(CASE WHEN fym.Month = 8 THEN aa.MaxOperatingCapacity END) AS AugMaxCapacity,

-- September
MAX(CASE WHEN fym.Month = 9 THEN aa.MinOperatingCapacity END) AS SepMinCapacity,
MAX(CASE WHEN fym.Month = 9 THEN aa.MaxOperatingCapacity END) AS SepMaxCapacity,

-- October
MAX(CASE WHEN fym.Month = 10 THEN aa.MinOperatingCapacity END) AS OctMinCapacity,
MAX(CASE WHEN fym.Month = 10 THEN aa.MaxOperatingCapacity END) AS OctMaxCapacity,

-- November
MAX(CASE WHEN fym.Month = 11 THEN aa.MinOperatingCapacity END) AS NovMinCapacity,
MAX(CASE WHEN fym.Month = 11 THEN aa.MaxOperatingCapacity END) AS NovMaxCapacity,

-- December
MAX(CASE WHEN fym.Month = 12 THEN aa.MinOperatingCapacity END) AS DecMinCapacity,
MAX(CASE WHEN fym.Month = 12 THEN aa.MaxOperatingCapacity END) AS DecMaxCapacity,

-- January
MAX(CASE WHEN fym.Month = 1 THEN aa.MinOperatingCapacity END) AS JanMinCapacity,
MAX(CASE WHEN fym.Month = 1 THEN aa.MaxOperatingCapacity END) AS JanMaxCapacity,

-- February
MAX(CASE WHEN fym.Month = 2 THEN aa.MinOperatingCapacity END) AS FebMinCapacity,
MAX(CASE WHEN fym.Month = 2 THEN aa.MaxOperatingCapacity END) AS FebMaxCapacity,

-- March
MAX(CASE WHEN fym.Month = 3 THEN aa.MinOperatingCapacity END) AS MarMinCapacity,
MAX(CASE WHEN fym.Month = 3 THEN aa.MaxOperatingCapacity END) AS MarMaxCapacity


    FROM PowerGenerationAssets pga
	  JOIN AssetAvailability aa
           ON aa.AssetId = pga.AssetId

		   LEFT JOIN CPP_AssetNorms_Mapping anm
		    ON anm.AssetId = pga.AssetId
    LEFT JOIN NormParameters np
           ON np.Id = anm.NormParameters_ID
   
    LEFT JOIN FYM fym
           ON fym.Id = aa.FinancialYearMonthId

    WHERE pga.CPPPLANT_FK_Id = @CppId

    GROUP BY
        pga.AssetId,
        pga.AssetName,
        pga.PlantCode

    ORDER BY pga.AssetName;
END;
GO
/****** Object:  StoredProcedure [dbo].[CPP_NMD_GetAssetPriority]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE    PROCEDURE [dbo].[CPP_NMD_GetAssetPriority]
(
    @CppId UNIQUEIDENTIFIER,
    @FinancialYear VARCHAR(9)   -- e.g. '2025-26'
)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @StartYear INT;
    DECLARE @EndYear INT;

    -- Extract years from '2025-26'
    SET @StartYear = CAST(LEFT(@FinancialYear, 4) AS INT);
    SET @EndYear   = CAST('20' + RIGHT(@FinancialYear, 2) AS INT);

    SELECT
        pga.AssetId,
        pga.AssetName,
		Max(pga.Remarks) as Remarks,
		Max(pga.AssetType) as AssetType,
		Max(Priority_Remarks) as Remarks,

        MAX(CASE WHEN fym.Month = 4  AND fym.Year = @StartYear THEN aa.Priority END) AS April,
        MAX(CASE WHEN fym.Month = 5  AND fym.Year = @StartYear THEN aa.Priority END) AS May,
        MAX(CASE WHEN fym.Month = 6  AND fym.Year = @StartYear THEN aa.Priority END) AS June,
        MAX(CASE WHEN fym.Month = 7  AND fym.Year = @StartYear THEN aa.Priority END) AS July,
        MAX(CASE WHEN fym.Month = 8  AND fym.Year = @StartYear THEN aa.Priority END) AS August,
        MAX(CASE WHEN fym.Month = 9  AND fym.Year = @StartYear THEN aa.Priority END) AS September,
        MAX(CASE WHEN fym.Month = 10 AND fym.Year = @StartYear THEN aa.Priority END) AS October,
        MAX(CASE WHEN fym.Month = 11 AND fym.Year = @StartYear THEN aa.Priority END) AS November,
        MAX(CASE WHEN fym.Month = 12 AND fym.Year = @StartYear THEN aa.Priority END) AS December,

        MAX(CASE WHEN fym.Month = 1  AND fym.Year = @EndYear THEN aa.Priority END) AS January,
        MAX(CASE WHEN fym.Month = 2  AND fym.Year = @EndYear THEN aa.Priority END) AS February,
        MAX(CASE WHEN fym.Month = 3  AND fym.Year = @EndYear THEN aa.Priority END) AS March

    FROM PowerGenerationAssets pga
     JOIN AssetAvailability aa
        ON aa.AssetId = pga.AssetId
    LEFT JOIN FinancialYearMonth fym
        ON fym.Id = aa.FinancialYearMonthId
       AND (
                (fym.Year = @StartYear AND fym.Month BETWEEN 4 AND 12)
             OR (fym.Year = @EndYear   AND fym.Month BETWEEN 1 AND 3)
           )

    WHERE pga.CPPPLANT_FK_Id = @CppId

    GROUP BY
        pga.AssetId,
        pga.AssetName

    ORDER BY
        pga.AssetName;
END;
GO
/****** Object:  StoredProcedure [dbo].[CPP_NMD_GetFixedConsumptionByPlant]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE   PROCEDURE [dbo].[CPP_NMD_GetFixedConsumptionByPlant]
(
    @PlantId       UNIQUEIDENTIFIER,
    @FinancialYear NVARCHAR(20)  -- e.g. '2025-26'
)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @StartYear INT,
            @EndYear   INT,
            @cols      NVARCHAR(MAX),
            @sql       NVARCHAR(MAX);

    -- '2025-26' -> @StartYear = 2025, @EndYear = 2026
    SET @StartYear = CAST(LEFT(@FinancialYear, 4) AS INT);
    SET @EndYear   = CAST(LEFT(@FinancialYear, 2) + RIGHT(@FinancialYear, 2) AS INT);

    -- Fixed month columns: Apr–Mar
    SET @cols = '[Apr],[May],[Jun],[Jul],[Aug],[Sep],[Oct],[Nov],[Dec],[Jan],[Feb],[Mar]';

    SET @sql = N'
    ;WITH Meta AS
    (
        -- All plant / CC / utility combinations for this plant
        SELECT DISTINCT
            pm.PlantName,
            pm.PlantCode,
            cc.CostCenterName,
            cc.CostCenterCode,
            cc.CostCenterId,
            np.Name AS UtilityName,
            np.SAPMaterialCode AS UtilitySAP,
            -- Special handling: For Water utility, always use NMD-Rev Proc
            CASE 
                WHEN np.Name = ''Water'' THEN ''NMD-Rev Proc''
                ELSE COALESCE(up.Name, pm.PlantName)
            END AS UtilityPlantName,
            CASE 
                WHEN np.Name = ''Water'' THEN ''40N0''
                ELSE COALESCE(up.PlantCode, pm.PlantCode)
            END AS UtilityPlantCode,
            np.UOM,
            np.Id AS NormParameterId
        FROM [RIL.AOP].dbo.FixedConsumptionPlantMapping pm
        JOIN [RIL.AOP].dbo.CPPCostCenters cc
             ON cc.Plant_FK_Id = pm.Id
        JOIN [RIL.AOP].dbo.CostCenterNormParameterMapping map
             ON map.CostCenterFK_Id = cc.CostCenterId
        JOIN [RIL.AOP].[dbo].[NormParameters] np
             ON np.Id = map.NormParameterFK_Id
        LEFT JOIN [RIL.AOP].dbo.Plants up
             ON up.Id = np.Plant_FK_Id
        WHERE pm.Plant_FK_Id = @PlantId
    ),
    BaseData AS
    (
        -- Left join FY consumption for the requested FY
        SELECT
            m.PlantName,
            m.PlantCode,
            m.CostCenterName,
            m.CostCenterCode,
            m.CostCenterId,
            m.UtilityName,
            m.UtilitySAP,
            m.UtilityPlantName,
            m.UtilityPlantCode,
            m.UOM,
            m.NormParameterId,
			rm.Remarks,
			rm.Id as RemarkId,
			ufc.NormParameter_FK_Id,
			ufc.CostCenter_FK_Id,
            CASE 
                WHEN fy.Month = 4  THEN ''Apr''
                WHEN fy.Month = 5  THEN ''May''
                WHEN fy.Month = 6  THEN ''Jun''
                WHEN fy.Month = 7  THEN ''Jul''
                WHEN fy.Month = 8  THEN ''Aug''
                WHEN fy.Month = 9  THEN ''Sep''
                WHEN fy.Month = 10 THEN ''Oct''
                WHEN fy.Month = 11 THEN ''Nov''
                WHEN fy.Month = 12 THEN ''Dec''
                WHEN fy.Month = 1  THEN ''Jan''
                WHEN fy.Month = 2  THEN ''Feb''
                WHEN fy.Month = 3  THEN ''Mar''
            END AS MonthName,
            ufc.ConsumptionValue
        FROM Meta m
        LEFT JOIN [RIL.AOP].dbo.UtilityFixedConsumption ufc
             ON ufc.CostCenter_FK_Id    = m.CostCenterId
            AND ufc.NormParameter_FK_Id = m.NormParameterId
        LEFT JOIN [RIL.AOP].dbo.FinancialYearMonth fy
             ON fy.Id = ufc.FinancialYearMonth_FK_Id
            AND (
                    (fy.Year = @StartYear AND fy.Month BETWEEN 4 AND 12)
                 OR (fy.Year = @EndYear   AND fy.Month BETWEEN 1 AND 3)
                )

		Left Join UtilityFixedConsumption_Remarks rm
		on rm.NormParameter_FK_Id = ufc.NormParameter_FK_Id
		AND rm.CostCenter_FK_Id = ufc.CostCenter_FK_Id
    )
    SELECT
        PlantName,
        PlantCode,
        CostCenterName,
        CostCenterCode,
        UtilityName,
        UtilitySAP,
        UtilityPlantName,
        UtilityPlantCode,
        UOM,
		NormParameterId,
		Remarks,
		RemarkId,
		NormParameter_FK_Id,
		CostCenter_FK_Id,
        ' + @cols + N'
    FROM BaseData
    PIVOT
    (
        MAX(ConsumptionValue)
        FOR MonthName IN (' + @cols + N')
    ) AS pvt
    ORDER BY 
        PlantName,
        CostCenterName,
        UtilityName;';

    EXEC sp_executesql
        @sql,
        N'@PlantId UNIQUEIDENTIFIER, @StartYear INT, @EndYear INT',
        @PlantId = @PlantId,
        @StartYear = @StartYear,
        @EndYear = @EndYear;
END;

GO
/****** Object:  StoredProcedure [dbo].[CPP_NMD_GetFixedConsumptionByPlantV2]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE   PROCEDURE [dbo].[CPP_NMD_GetFixedConsumptionByPlantV2]
(
    @PlantId       UNIQUEIDENTIFIER,
    @FinancialYear NVARCHAR(20)  -- e.g. '2025-26'
)
AS
BEGIN
    SET NOCOUNT ON;

    BEGIN TRY
        DECLARE @StartYear INT,
                @EndYear   INT,
                @PrevStartYear INT,
                @PrevEndYear INT,
                @cols      NVARCHAR(MAX),
                @sql       NVARCHAR(MAX),
                @DataExistsCount INT;

        -- Parse: '2025-26' -> @StartYear = 2025, @EndYear = 2026
        SET @StartYear = CAST(LEFT(@FinancialYear, 4) AS INT);
        SET @EndYear   = CAST(LEFT(@FinancialYear, 2) + RIGHT(@FinancialYear, 2) AS INT);

        -- Calculate previous year: 2025-26 -> 2024-25
        SET @PrevStartYear = @StartYear - 1;
        SET @PrevEndYear   = @EndYear - 1;

        -------------------------------------------------------------------------
        -- Step 1: Check if data exists for selected financial year
        -------------------------------------------------------------------------
        SELECT @DataExistsCount = COUNT(*)
        FROM [RIL.AOP].dbo.UtilityFixedConsumption ufc
        INNER JOIN [RIL.AOP].dbo.FinancialYearMonth fym
            ON fym.Id = ufc.FinancialYearMonth_FK_Id
        INNER JOIN [RIL.AOP].dbo.CPPCostCenters cc
            ON cc.CostCenterId = ufc.CostCenter_FK_Id
        WHERE cc.Plant_FK_Id = @PlantId
        AND (
            (fym.Year = @StartYear AND fym.Month >= 4)
            OR (fym.Year = @EndYear AND fym.Month <= 3)
        );

        -------------------------------------------------------------------------
        -- Step 2: If NO data exists for selected year, auto carry-forward from previous year
        -------------------------------------------------------------------------
        IF @DataExistsCount = 0
        BEGIN
            -- Simple approach: For each month in previous year, find corresponding month in current year
            -- Then copy all consumption data
            
            INSERT INTO [RIL.AOP].dbo.UtilityFixedConsumption 
                (FinancialYearMonth_FK_Id, NormParameter_FK_Id, CostCenter_FK_Id, ConsumptionValue)
            SELECT 
                curr_fym.Id AS FinancialYearMonth_FK_Id,
                ufc.NormParameter_FK_Id,
                ufc.CostCenter_FK_Id,
                ufc.ConsumptionValue
            FROM [RIL.AOP].dbo.UtilityFixedConsumption ufc
            INNER JOIN [RIL.AOP].dbo.FinancialYearMonth prev_fym
                ON ufc.FinancialYearMonth_FK_Id = prev_fym.Id
            INNER JOIN [RIL.AOP].dbo.FinancialYearMonth curr_fym
                ON curr_fym.Month = prev_fym.Month
            INNER JOIN [RIL.AOP].dbo.CPPCostCenters cc
                ON cc.CostCenterId = ufc.CostCenter_FK_Id
            WHERE cc.Plant_FK_Id = @PlantId
            -- Previous year financial year (Apr-Mar)
            AND ((prev_fym.Year = @PrevStartYear AND prev_fym.Month >= 4)
                 OR (prev_fym.Year = @PrevEndYear AND prev_fym.Month <= 3))
            -- Current year financial year (Apr-Mar)
            AND ((curr_fym.Year = @StartYear AND curr_fym.Month >= 4)
                 OR (curr_fym.Year = @EndYear AND curr_fym.Month <= 3))
            -- Avoid duplicates
            AND NOT EXISTS (
                SELECT 1 FROM [RIL.AOP].dbo.UtilityFixedConsumption ufc_check
                WHERE ufc_check.FinancialYearMonth_FK_Id = curr_fym.Id
                AND ufc_check.NormParameter_FK_Id = ufc.NormParameter_FK_Id
                AND ufc_check.CostCenter_FK_Id = ufc.CostCenter_FK_Id
            );
        END;

        -------------------------------------------------------------------------
        -- Step 3: Now retrieve and return the data (after auto carry-forward if needed)
        -------------------------------------------------------------------------
        SET @cols = '[Apr],[May],[Jun],[Jul],[Aug],[Sep],[Oct],[Nov],[Dec],[Jan],[Feb],[Mar]';

        SET @sql = N'
        ;WITH Meta AS
        (
            -- All plant / CC / utility combinations for this plant
            SELECT DISTINCT
                pm.PlantName,
                pm.PlantCode,
                cc.CostCenterName,
                cc.CostCenterCode,
                cc.CostCenterId,
                np.Name AS UtilityName,
                np.SAPMaterialCode AS UtilitySAP,
                up.Name      AS UtilityPlantName,
                up.PlantCode AS UtilityPlantCode,
                np.UOM,
                np.Id AS NormParameterId
            FROM [RIL.AOP].dbo.FixedConsumptionPlantMapping pm
            JOIN [RIL.AOP].dbo.CPPCostCenters cc
                 ON cc.Plant_FK_Id = pm.Id
            JOIN [RIL.AOP].dbo.CostCenterNormParameterMapping map
                 ON map.CostCenterFK_Id = cc.CostCenterId
            JOIN [RIL.AOP].[dbo].[NormParameters] np
                 ON np.Id = map.NormParameterFK_Id
            LEFT JOIN [RIL.AOP].dbo.Plants up
                 ON up.Id = np.Plant_FK_Id
            WHERE pm.Plant_FK_Id = @PlantId
        ),
        BaseData AS
        (
            -- Left join FY consumption for the requested FY
            SELECT
                m.PlantName,
                m.PlantCode,
                m.CostCenterName,
                m.CostCenterCode,
                m.CostCenterId,
                m.UtilityName,
                m.UtilitySAP,
                m.UtilityPlantName,
                m.UtilityPlantCode,
                m.UOM,
                m.NormParameterId,
                rm.Remarks,
                rm.Id as RemarkId,
                ufc.NormParameter_FK_Id,
                ufc.CostCenter_FK_Id,
                CASE 
                    WHEN fy.Month = 4  THEN ''Apr''
                    WHEN fy.Month = 5  THEN ''May''
                    WHEN fy.Month = 6  THEN ''Jun''
                    WHEN fy.Month = 7  THEN ''Jul''
                    WHEN fy.Month = 8  THEN ''Aug''
                    WHEN fy.Month = 9  THEN ''Sep''
                    WHEN fy.Month = 10 THEN ''Oct''
                    WHEN fy.Month = 11 THEN ''Nov''
                    WHEN fy.Month = 12 THEN ''Dec''
                    WHEN fy.Month = 1  THEN ''Jan''
                    WHEN fy.Month = 2  THEN ''Feb''
                    WHEN fy.Month = 3  THEN ''Mar''
                END AS MonthName,
                ufc.ConsumptionValue
            FROM Meta m
            LEFT JOIN [RIL.AOP].dbo.FinancialYearMonth fy
                 ON (
                        (fy.Year = @StartYear AND fy.Month BETWEEN 4 AND 12)
                     OR (fy.Year = @EndYear   AND fy.Month BETWEEN 1 AND 3)
                    )
            LEFT JOIN [RIL.AOP].dbo.UtilityFixedConsumption ufc
                 ON ufc.CostCenter_FK_Id    = m.CostCenterId
                AND ufc.NormParameter_FK_Id = m.NormParameterId
                AND ufc.FinancialYearMonth_FK_Id = fy.Id
            LEFT JOIN UtilityFixedConsumption_Remarks rm
                 ON rm.NormParameter_FK_Id = ufc.NormParameter_FK_Id
                AND rm.CostCenter_FK_Id = ufc.CostCenter_FK_Id
        )
        SELECT
            PlantName,
            PlantCode,
            CostCenterName,
            CostCenterCode,
            UtilityName,
            UtilitySAP,
            UtilityPlantName,
            UtilityPlantCode,
            UOM,
            NormParameterId,
            Remarks,
            RemarkId,
            NormParameter_FK_Id,
            CostCenter_FK_Id,
            ' + @cols + N'
        FROM BaseData
        PIVOT
        (
            MAX(ConsumptionValue)
            FOR MonthName IN (' + @cols + N')
        ) AS pvt
        ORDER BY 
            PlantName,
            CostCenterName,
            UtilityName;';

        EXEC sp_executesql
            @sql,
            N'@PlantId UNIQUEIDENTIFIER, @StartYear INT, @EndYear INT',
            @PlantId = @PlantId,
            @StartYear = @StartYear,
            @EndYear = @EndYear;

    END TRY
    BEGIN CATCH
        DECLARE @ErrorMessage NVARCHAR(MAX) = ERROR_MESSAGE();
        DECLARE @ErrorSeverity INT = ERROR_SEVERITY();
        DECLARE @ErrorState INT = ERROR_STATE();
        
        RAISERROR (@ErrorMessage, @ErrorSeverity, @ErrorState);
    END CATCH;
END;
GO
/****** Object:  StoredProcedure [dbo].[CPP_NMD_GetFixedConsumptionByPlantv3]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE   PROCEDURE [dbo].[CPP_NMD_GetFixedConsumptionByPlantv3]
(
    @PlantId       UNIQUEIDENTIFIER,
    @FinancialYear NVARCHAR(20)  -- e.g. '2025-26'
)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @StartYear INT,
            @EndYear   INT,
            @cols      NVARCHAR(MAX),
            @sql       NVARCHAR(MAX);

    -- '2025-26' -> @StartYear = 2025, @EndYear = 2026
    SET @StartYear = CAST(LEFT(@FinancialYear, 4) AS INT);
    SET @EndYear   = CAST(LEFT(@FinancialYear, 2) + RIGHT(@FinancialYear, 2) AS INT);

    -- Fixed month columns: Apr–Mar
    SET @cols = '[Apr],[May],[Jun],[Jul],[Aug],[Sep],[Oct],[Nov],[Dec],[Jan],[Feb],[Mar]';

    SET @sql = N'
    ;WITH Meta AS
    (
        -- All plant / CC / utility combinations for this plant
        SELECT DISTINCT
            pm.PlantName,
            pm.PlantCode,
            cc.CostCenterName,
            cc.CostCenterCode,
            cc.CostCenterId,
            np.Name AS UtilityName,
            np.SAPMaterialCode AS UtilitySAP,
            -- Special handling: For Water utility, always use NMD-Rev Proc
            CASE 
                WHEN np.Name = ''Water'' THEN ''NMD-Rev Proc''
                ELSE COALESCE(up.Name, pm.PlantName)
            END AS UtilityPlantName,
            CASE 
                WHEN np.Name = ''Water'' THEN ''40N0''
                ELSE COALESCE(up.PlantCode, pm.PlantCode)
            END AS UtilityPlantCode,
            np.UOM,
            np.Id AS NormParameterId
        FROM [RIL.AOP].dbo.FixedConsumptionPlantMapping pm
        JOIN [RIL.AOP].dbo.CPPCostCenters cc
             ON cc.Plant_FK_Id = pm.Id
        JOIN [RIL.AOP].dbo.CostCenterNormParameterMapping map
             ON map.CostCenterFK_Id = cc.CostCenterId
        JOIN [RIL.AOP].[dbo].[NormParameters] np
             ON np.Id = map.NormParameterFK_Id
        LEFT JOIN [RIL.AOP].dbo.Plants up
             ON up.Id = np.Plant_FK_Id
        WHERE pm.Plant_FK_Id = @PlantId
    ),
    BaseData AS
    (
        -- Left join FY consumption for the requested FY
        SELECT
            m.PlantName,
            m.PlantCode,
            m.CostCenterName,
            m.CostCenterCode,
            m.CostCenterId,
            m.UtilityName,
            m.UtilitySAP,
            m.UtilityPlantName,
            m.UtilityPlantCode,
            m.UOM,
            m.NormParameterId,
			rm.Remarks,
			rm.Id as RemarkId,
			ufc.NormParameter_FK_Id,
			ufc.CostCenter_FK_Id,
            CASE 
                WHEN fy.Month = 4  THEN ''Apr''
                WHEN fy.Month = 5  THEN ''May''
                WHEN fy.Month = 6  THEN ''Jun''
                WHEN fy.Month = 7  THEN ''Jul''
                WHEN fy.Month = 8  THEN ''Aug''
                WHEN fy.Month = 9  THEN ''Sep''
                WHEN fy.Month = 10 THEN ''Oct''
                WHEN fy.Month = 11 THEN ''Nov''
                WHEN fy.Month = 12 THEN ''Dec''
                WHEN fy.Month = 1  THEN ''Jan''
                WHEN fy.Month = 2  THEN ''Feb''
                WHEN fy.Month = 3  THEN ''Mar''
            END AS MonthName,
            ufc.ConsumptionValue
        FROM Meta m
        LEFT JOIN [RIL.AOP].dbo.UtilityFixedConsumption ufc
             ON ufc.CostCenter_FK_Id    = m.CostCenterId
            AND ufc.NormParameter_FK_Id = m.NormParameterId
        LEFT JOIN [RIL.AOP].dbo.FinancialYearMonth fy
             ON fy.Id = ufc.FinancialYearMonth_FK_Id
            AND (
                    (fy.Year = @StartYear AND fy.Month BETWEEN 4 AND 12)
                 OR (fy.Year = @EndYear   AND fy.Month BETWEEN 1 AND 3)
                )

		Left Join UtilityFixedConsumption_Remarks rm
		on rm.NormParameter_FK_Id = ufc.NormParameter_FK_Id
		AND rm.CostCenter_FK_Id = ufc.CostCenter_FK_Id
    )
    SELECT
        PlantName,
        PlantCode,
        CostCenterName,
        CostCenterCode,
        UtilityName,
        UtilitySAP,
        UtilityPlantName,
        UtilityPlantCode,
        UOM,
		NormParameterId,
		Remarks,
		RemarkId,
		NormParameter_FK_Id,
		CostCenter_FK_Id,
        ' + @cols + N'
    FROM BaseData
    PIVOT
    (
        MAX(ConsumptionValue)
        FOR MonthName IN (' + @cols + N')
    ) AS pvt
    ORDER BY 
        PlantName,
        CostCenterName,
        UtilityName;';

    EXEC sp_executesql
        @sql,
        N'@PlantId UNIQUEIDENTIFIER, @StartYear INT, @EndYear INT',
        @PlantId = @PlantId,
        @StartYear = @StartYear,
        @EndYear = @EndYear;
END;
GO
/****** Object:  StoredProcedure [dbo].[CPP_NMD_GetNormBasedUtilityBudget]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE   PROCEDURE [dbo].[CPP_NMD_GetNormBasedUtilityBudget]
(
    @CPPPlantId UNIQUEIDENTIFIER,
    @FinancialYear NVARCHAR(20)
)
AS
BEGIN
    SET NOCOUNT ON;

    -------------------------------------------------------------------------
    -- FINANCIAL YEAR PARSING
    -------------------------------------------------------------------------
    DECLARE @CleanYear NVARCHAR(20) = ISNULL(@FinancialYear, '');
    SET @CleanYear = REPLACE(@CleanYear, ' ', '');
    SET @CleanYear = REPLACE(@CleanYear, '/', '-');

    IF @CleanYear = '' SET @CleanYear = '1900-01';

    IF @CleanYear NOT LIKE '%-%'
    BEGIN
        SET @CleanYear = CONCAT(
            @CleanYear, '-', 
            RIGHT(CONCAT('0', CAST(CAST(RIGHT(@CleanYear,2) AS INT) + 1 AS VARCHAR(4))),2)
        );
    END;

    DECLARE @Part1 NVARCHAR(4), @Part2 NVARCHAR(4);

    SET @Part1 = LEFT(@CleanYear, CHARINDEX('-', @CleanYear) - 1);
    SET @Part2 = RIGHT(@CleanYear, LEN(@CleanYear) - CHARINDEX('-', @CleanYear));

    IF LEN(@Part1) = 2 SET @Part1 = CONCAT('20', @Part1);
    IF LEN(@Part2) = 2 SET @Part2 = CONCAT('20', @Part2);

    DECLARE @StartYear INT = TRY_CAST(@Part1 AS INT);
    DECLARE @EndYear   INT = TRY_CAST(@Part2 AS INT);

    IF @StartYear IS NULL SET @StartYear = 1900;
    IF @EndYear   IS NULL SET @EndYear   = @StartYear + 1;

    -------------------------------------------------------------------------
    -- FETCH GENERATING PLANTS
    -------------------------------------------------------------------------
    ;WITH GeneratingPlants AS
    (
        SELECT AssociatedPlant_FK_Id AS GeneratingPlantId
        FROM PowerGenerationPlantsMapping
        WHERE CPPPlantId = @CPPPlantId
    ),

    NormData AS
    (
        SELECT 
            nh.Id AS NormHeaderId,
            nh.Plant_FK_Id AS GeneratingPlantId,
            nh.UtilityName,
            nh.UtilityId,
            nh.UtilityUOM,
            nh.AccountName,
            nh.MaterialName,
			nh.MaterialId,            -- modified
            nh.IssuingPlantName,
            nh.IssuingPlant_FK_Id,
            nh.IssuingUOM,
            nh.NormParameter_FK_Id,
            nh.DisplayOrder
			
        FROM NormsHeader nh
        INNER JOIN GeneratingPlants gp
            ON gp.GeneratingPlantId = nh.Plant_FK_Id
        WHERE nh.IsActive = 1
    ),

    JoinedNorms AS
    (
        SELECT 
            nd.*,
            np.DisplayName AS NormParameterName,
            np.UOM AS ParameterUOM,
            np.SAPMaterialCode,
            np.DisplayOrder AS NormParameterDisplayOrder
        FROM NormData nd
        LEFT JOIN NormParameters np
            ON nd.NormParameter_FK_Id = np.Id
    ),

    FinalData AS
    (
        SELECT 
            jn.*,
            p.Name AS GeneratingPlantName,
            p.DisplayName AS GeneratingPlantDisplayName,
            p.PlantCode AS GeneratingPlantCode
        FROM JoinedNorms jn
        LEFT JOIN Plants p
            ON jn.GeneratingPlantId = p.Id
    ),

    -------------------------------------------------------------------------
    -- MONTH DETAILS FILTERED BY FINANCIAL YEAR (APR–MAR)
    -------------------------------------------------------------------------
    MonthDetails AS
    (
        SELECT 
            nmd.NormsHeader_FK_Id,
            fym.Month,
            fym.Year,
            fym.Id AS FinancialYearMonthId,
            nmd.Norms,
            nmd.Quantity,
            nmd.Amount,
            nmd.Price,
            nmd.GenerationUOM,
            nmd.QTY,
            nmd.ScenarioType,
            nmd.DisplayOrder,
			nmd.Remarks
        FROM NormsMonthDetail nmd
        INNER JOIN FinancialYearMonth fym
            ON fym.Id = nmd.FinancialYearMonth_FK_Id
        WHERE 
            (
                fym.Year = @StartYear AND fym.Month >= 4  -- Apr–Dec of start year
            )
            OR
            (
                fym.Year = @EndYear   AND fym.Month <= 3  -- Jan–Mar of end year
            )
    ),

    -------------------------------------------------------------------------
    -- EXTRACT GENERATION UOM (COMMON FOR ALL MONTHS - TAKE FROM FIRST AVAILABLE)
    -------------------------------------------------------------------------
    GenerationUOMData AS
    (
        SELECT 
            fd.NormHeaderId,
            (SELECT TOP 1 md.GenerationUOM 
             FROM MonthDetails md 
             WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.GenerationUOM IS NOT NULL) AS CommonGenerationUOM
        FROM FinalData fd
    )

    SELECT
        ROW_NUMBER() OVER (ORDER BY fd.GeneratingPlantName, fd.DisplayOrder, fd.NormParameterDisplayOrder) AS id,

        fd.NormHeaderId AS normsHeaderFkId,
        fd.GeneratingPlantName AS generatingPlantName,
        fd.UtilityName        AS utilityName,
        fd.UtilityId          AS utilityId,
        COALESCE(fd.ParameterUOM, fd.UtilityUOM) AS uom,
        fd.AccountName        AS accountName,
        fd.MaterialName       AS materialName,
		fd.MaterialId         AS materialId,      -- modified
        fd.IssuingPlantName   AS issuingPlantName,
        fd.IssuingUOM         AS issuingUom,
        COALESCE(guom.CommonGenerationUOM, '') AS generationUom,  -- Common generation UOM (new field)

        -- APR
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md 
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 4
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS apr,

        -- MAY
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md 
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 5
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS may,

        -- JUN
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md 
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 6
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS jun,

        -- JUL
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 7
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS jul,

        -- AUG
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md 
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 8
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS aug,

        -- SEP
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md 
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 9
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS sep,

        -- OCT
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md 
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 10
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS oct,

        -- NOV
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md 
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 11
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS nov,

        -- DEC
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md 
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 12
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS [dec],

        -- JAN
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md 
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 1
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS jan,

        -- FEB
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md 
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 2
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS feb,

        -- MAR
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md 
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 3
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS mar

    FROM FinalData fd
    LEFT JOIN GenerationUOMData guom
        ON fd.NormHeaderId = guom.NormHeaderId
    ORDER BY fd.GeneratingPlantName, fd.DisplayOrder, fd.NormParameterDisplayOrder;

END;
GO
/****** Object:  StoredProcedure [dbo].[CPP_NMD_GetPlantConsumptionByMaterial]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO


CREATE   PROCEDURE [dbo].[CPP_NMD_GetPlantConsumptionByMaterial]
    @CPPPlantId UNIQUEIDENTIFIER,
    @AOPYear NVARCHAR(10) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @ResolvedYear NVARCHAR(10);

    -- Resolve financial year
    SET @ResolvedYear = ISNULL(
        @AOPYear,
        CAST(
            CASE WHEN MONTH(GETDATE()) >= 4 THEN YEAR(GETDATE())
                 ELSE YEAR(GETDATE()) - 1 END AS NVARCHAR(4)
        ) + '-' +
        RIGHT(
            CAST(
                CASE WHEN MONTH(GETDATE()) >= 4 THEN YEAR(GETDATE()) + 1
                     ELSE YEAR(GETDATE()) END AS NVARCHAR(4)
            ), 2
        )
    );

    -- Mapped Plant IDs
    DECLARE @MappedPlantIds TABLE (Plant_FK_Id UNIQUEIDENTIFIER);

    INSERT INTO @MappedPlantIds (Plant_FK_Id)
    SELECT DISTINCT Plant_FK_Id
    FROM [RIL.AOP].[dbo].[PowerConsumptionPlantMapping]
    WHERE Consumption_FK_Id = @CPPPlantId;

    -- Norm Parameters for the Plant
    DECLARE @NormParams TABLE 
    (
        Id UNIQUEIDENTIFIER, 
        DisplayName NVARCHAR(200),
        SAPMaterialCode NVARCHAR(100),
        UOM NVARCHAR(50),
		Plant_FK_Id  NVARCHAR(50),
		Remarks varchar(8000)
	
    );

    INSERT INTO @NormParams (Id, DisplayName, SAPMaterialCode, UOM, Plant_FK_Id, Remarks)
    SELECT Id, DisplayName, SAPMaterialCode, UOM, Plant_FK_Id, Remarks
    FROM [RIL.AOP].[dbo].[NormParameters]
-- WHERE Plant_FK_Id = @CPPPlantId;
     where SAPMaterialCode in (select SAPMaterialCode from NormParameters where Plant_FK_Id = @CPPPlantId)
	and Plant_FK_Id in (select Plant_FK_Id from @MappedPlantIds)



    -- Final Select with LEFT JOIN
    SELECT 
	    MAX(p.DisplayName) As PlantName,
		MAX(p.PlantCode) As PlantCode,
		

        np.DisplayName AS CPPUtilities,
        np.SAPMaterialCode AS CPPUtiltiyIds ,
        np.UOM AS UOM,
		Max(np.Remarks) as Remarks,
		

        ISNULL(SUM(acn.April  * 100), 0) AS April,
        ISNULL(SUM(acn.May    * 100), 0) AS May,
        ISNULL(SUM(acn.June   * 100), 0) AS June,
        ISNULL(SUM(acn.July   * 100), 0) AS July,
        ISNULL(SUM(acn.Aug    * 100), 0) AS Aug,
        ISNULL(SUM(acn.Sep    * 100), 0) AS Sep,
        ISNULL(SUM(acn.Oct    * 100), 0) AS Oct,
        ISNULL(SUM(acn.Nov    * 100), 0) AS Nov,
        ISNULL(SUM(acn.Dec    * 100), 0) AS Dec,
        ISNULL(SUM(acn.Jan    * 100), 0) AS Jan,
        ISNULL(SUM(acn.Feb    * 100), 0) AS Feb,
        ISNULL(SUM(acn.March  * 100), 0) AS March,

        -- GRAND TOTAL for the entire financial year
        ISNULL(
            SUM(
                    acn.April  * 100 +
                    acn.May    * 100 +
                    acn.June   * 100 +
                    acn.July   * 100 +
                    acn.Aug    * 100 +
                    acn.Sep    * 100 +
                    acn.Oct    * 100 +
                    acn.Nov    * 100 +
                    acn.Dec    * 100 +
                    acn.Jan    * 100 +
                    acn.Feb    * 100 +
                    acn.March  * 100
                ), 
        0) AS GrandTotal

    FROM @NormParams np
    LEFT JOIN [RIL.AOP].[dbo].[AOPConsumptionNorm] acn
        ON acn.Material_FK_Id = np.Id
        AND acn.Plant_FK_Id IN (SELECT Plant_FK_Id FROM @MappedPlantIds)
        AND acn.AOPYear = @ResolvedYear
		left join Plants p on p.Id = np.Plant_FK_Id

    GROUP BY 
        np.DisplayName,
        np.SAPMaterialCode,
        np.UOM

    ORDER BY np.DisplayName;

END;

GO
/****** Object:  StoredProcedure [dbo].[CPP_NMD_GetPowerGenerationOperationalHours]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO



CREATE     PROCEDURE [dbo].[CPP_NMD_GetPowerGenerationOperationalHours]
(
    @CPPPlantId UNIQUEIDENTIFIER,
    @FinancialYear VARCHAR(7)   -- example: '2025-26'
)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @StartYear INT;
    DECLARE @EndYear INT;

    -- Extract years from '2025-26'
    SET @StartYear = CAST(LEFT(@FinancialYear, 4) AS INT);
    SET @EndYear   = @StartYear + 1;

    SELECT
        pga.AssetName,
		Max(pga.AssetId) as AssetId,
		Max(pga.AssetType) as AssetType,
		Max(oh.Remarks) as Remarks,

        ISNULL(SUM(CASE WHEN fym.Month = 4  AND fym.Year = @StartYear THEN oh.OperationalHours END), 0) AS Apr,
        ISNULL(SUM(CASE WHEN fym.Month = 5  AND fym.Year = @StartYear THEN oh.OperationalHours END), 0) AS May,
        ISNULL(SUM(CASE WHEN fym.Month = 6  AND fym.Year = @StartYear THEN oh.OperationalHours END), 0) AS Jun,
        ISNULL(SUM(CASE WHEN fym.Month = 7  AND fym.Year = @StartYear THEN oh.OperationalHours END), 0) AS Jul,
        ISNULL(SUM(CASE WHEN fym.Month = 8  AND fym.Year = @StartYear THEN oh.OperationalHours END), 0) AS Aug,
        ISNULL(SUM(CASE WHEN fym.Month = 9  AND fym.Year = @StartYear THEN oh.OperationalHours END), 0) AS Sep,
        ISNULL(SUM(CASE WHEN fym.Month = 10 AND fym.Year = @StartYear THEN oh.OperationalHours END), 0) AS Oct,
        ISNULL(SUM(CASE WHEN fym.Month = 11 AND fym.Year = @StartYear THEN oh.OperationalHours END), 0) AS Nov,
        ISNULL(SUM(CASE WHEN fym.Month = 12 AND fym.Year = @StartYear THEN oh.OperationalHours END), 0) AS Dec,

        ISNULL(SUM(CASE WHEN fym.Month = 1  AND fym.Year = @EndYear THEN oh.OperationalHours END), 0) AS Jan,
        ISNULL(SUM(CASE WHEN fym.Month = 2  AND fym.Year = @EndYear THEN oh.OperationalHours END), 0) AS Feb,
        ISNULL(SUM(CASE WHEN fym.Month = 3  AND fym.Year = @EndYear THEN oh.OperationalHours END), 0) AS Mar

    FROM PowerGenerationAssets pga
    LEFT JOIN OperationalHours oh
        ON oh.Asset_FK_Id = pga.AssetId
    LEFT JOIN FinancialYearMonth fym
        ON fym.Id = oh.FinancialMonthId
       AND (
                (fym.Year = @StartYear AND fym.Month BETWEEN 4 AND 12)
             OR (fym.Year = @EndYear   AND fym.Month BETWEEN 1 AND 3)
           )
    WHERE pga.CPPPLANT_FK_Id = @CPPPlantId
	AND pga.AssetType != 'PROC'
    GROUP BY pga.AssetName
    ORDER BY pga.AssetName;

END;
GO
/****** Object:  StoredProcedure [dbo].[CPP_NMD_GetPowerGenerationOperationalHoursv1]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE PROCEDURE [dbo].[CPP_NMD_GetPowerGenerationOperationalHoursv1]
(
    @CPPPlantId UNIQUEIDENTIFIER,
    @FinancialYear VARCHAR(7)   -- example: '2025-26'
)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @StartYear INT;
    DECLARE @EndYear INT;

    -- Extract years from '2025-26'
    SET @StartYear = CAST(LEFT(@FinancialYear, 4) AS INT);
    SET @EndYear   = @StartYear + 1;

    -- ===== UNION: PowerGenerationAssets + ImportPower Sources =====
    
    WITH CombinedOperationalHours AS (
    
    -- Part 1: PowerGenerationAssets (existing logic - UNCHANGED)
    SELECT
        pga.AssetName,
        Max(pga.AssetId) as AssetId,
        Max(pga.AssetType) as AssetType,
        Max(oh.Remarks) as Remarks,
        
        ISNULL(SUM(CASE WHEN fym.Month = 4  AND fym.Year = @StartYear THEN oh.OperationalHours END), 0) AS Apr,
        ISNULL(SUM(CASE WHEN fym.Month = 5  AND fym.Year = @StartYear THEN oh.OperationalHours END), 0) AS May,
        ISNULL(SUM(CASE WHEN fym.Month = 6  AND fym.Year = @StartYear THEN oh.OperationalHours END), 0) AS Jun,
        ISNULL(SUM(CASE WHEN fym.Month = 7  AND fym.Year = @StartYear THEN oh.OperationalHours END), 0) AS Jul,
        ISNULL(SUM(CASE WHEN fym.Month = 8  AND fym.Year = @StartYear THEN oh.OperationalHours END), 0) AS Aug,
        ISNULL(SUM(CASE WHEN fym.Month = 9  AND fym.Year = @StartYear THEN oh.OperationalHours END), 0) AS Sep,
        ISNULL(SUM(CASE WHEN fym.Month = 10 AND fym.Year = @StartYear THEN oh.OperationalHours END), 0) AS Oct,
        ISNULL(SUM(CASE WHEN fym.Month = 11 AND fym.Year = @StartYear THEN oh.OperationalHours END), 0) AS Nov,
        ISNULL(SUM(CASE WHEN fym.Month = 12 AND fym.Year = @StartYear THEN oh.OperationalHours END), 0) AS Dec,
        ISNULL(SUM(CASE WHEN fym.Month = 1  AND fym.Year = @EndYear THEN oh.OperationalHours END), 0) AS Jan,
        ISNULL(SUM(CASE WHEN fym.Month = 2  AND fym.Year = @EndYear THEN oh.OperationalHours END), 0) AS Feb,
        ISNULL(SUM(CASE WHEN fym.Month = 3  AND fym.Year = @EndYear THEN oh.OperationalHours END), 0) AS Mar

    FROM PowerGenerationAssets pga
    LEFT JOIN OperationalHours oh
        ON oh.Asset_FK_Id = pga.AssetId
    LEFT JOIN FinancialYearMonth fym
        ON fym.Id = oh.FinancialMonthId
       AND (
                (fym.Year = @StartYear AND fym.Month BETWEEN 4 AND 12)
             OR (fym.Year = @EndYear   AND fym.Month BETWEEN 1 AND 3)
           )
    WHERE pga.CPPPLANT_FK_Id = @CPPPlantId
      AND pga.AssetType != 'PROC'
    GROUP BY pga.AssetName

    UNION ALL

    -- Part 2: ImportPower Sources (MEL and Power_Dis)
    -- Returns: assetName=sourceName, assetId=sourceId, assetType='Rev Proc'
    SELECT
        ips.SourceName AS AssetName,
        ips.Id AS AssetId,
        'Rev Proc' AS AssetType,
        ipoh.Remarks,
        
        COALESCE(ipoh.Apr, 0) AS Apr,
        COALESCE(ipoh.May, 0) AS May,
        COALESCE(ipoh.Jun, 0) AS Jun,
        COALESCE(ipoh.Jul, 0) AS Jul,
        COALESCE(ipoh.Aug, 0) AS Aug,
        COALESCE(ipoh.Sep, 0) AS Sep,
        COALESCE(ipoh.Oct, 0) AS Oct,
        COALESCE(ipoh.Nov, 0) AS Nov,
        COALESCE(ipoh.Dec, 0) AS Dec,
        COALESCE(ipoh.Jan, 0) AS Jan,
        COALESCE(ipoh.Feb, 0) AS Feb,
        COALESCE(ipoh.Mar, 0) AS Mar

    FROM CPPImportPowerSourceMapping ips
    LEFT JOIN CPPImportPowerOperationalHours ipoh
        ON ipoh.ImportPowerSource_FK_Id = ips.Id
        AND ipoh.FinancialYear = @FinancialYear
    WHERE ips.CPPPlant_FK_Id = @CPPPlantId
      AND ips.IsActive = 1
    )
    
    -- Select from CTE with proper ordering to keep PowerGen and ImportPower sequential
    SELECT *
    FROM CombinedOperationalHours
    ORDER BY AssetType ASC, AssetName ASC;

END;
GO
/****** Object:  StoredProcedure [dbo].[CPP_NMD_GetProcessDemandByYear]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE PROCEDURE [dbo].[CPP_NMD_GetProcessDemandByYear]
    @FinancialYear VARCHAR(10)
AS
BEGIN
    SET NOCOUNT ON;

    SELECT 
        -- Use calculated data ID if exists, otherwise generate new GUID
        ISNULL(c.id, NEWID()) AS id,
        @FinancialYear AS financial_year,
        m.process_plant,
        m.process_plant_id,
        m.cpp_utility,
        m.cpp_utility_id,
        m.cpp_plant,
        m.cpp_plant_id,
        m.uom,
        -- Return calculated values or 0 if not exists
        ISNULL(c.apr, 0) AS apr,
        ISNULL(c.may, 0) AS may,
        ISNULL(c.jun, 0) AS jun,
        ISNULL(c.jul, 0) AS jul,
        ISNULL(c.aug, 0) AS aug,
        ISNULL(c.sep, 0) AS sep,
        ISNULL(c.oct, 0) AS oct,
        ISNULL(c.nov, 0) AS nov,
        ISNULL(c.dec, 0) AS dec,
        ISNULL(c.jan, 0) AS jan,
        ISNULL(c.feb, 0) AS feb,
        ISNULL(c.mar, 0) AS mar,
        -- Flag to indicate if data is calculated or default
        CASE WHEN c.id IS NOT NULL THEN 1 ELSE 0 END AS is_calculated,
        c.remarks
    FROM dbo.ProcessDemandMaster m
    LEFT JOIN dbo.CalculatedProcessDemand c 
        ON m.process_plant_id = c.process_plant_id
        AND m.cpp_utility_id = c.cpp_utility_id
        AND ISNULL(m.cpp_plant_id, '') = ISNULL(c.cpp_plant_id, '')
        AND c.financial_year = @FinancialYear
    WHERE m.is_active = 1
    ORDER BY m.process_plant, m.cpp_utility;
END
GO
/****** Object:  StoredProcedure [dbo].[CPP_NMD_GetValuesForConsecutiveDays]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO


CREATE PROCEDURE [dbo].[CPP_NMD_GetValuesForConsecutiveDays] (@plantId varchar(255)='23BCA1B3-56DD-4C15-A3D6-3C2C9A62E653',
@finYear varchar(100)='2025-26', @PeriodFrom date='2020-04-01', @PeriodTo date='2025-03-31'
)
AS
BEGIN
	WAITFOR DELAY '00:00:05'
	DECLARE @vId varchar(100),@siteId varchar(100)
	SELECT TOP 1 @vId = Vertical_FK_Id, @siteId = Site_FK_Id FROM Plants WHERE Id = @plantId

	EXEC [AROMATICS_LoadMCValues] @finYear,@plantId,@vId,@siteId,1

	UPDATE B SET B.ModifiedOn = GETDATE() FROM NormParameters A  INNER JOIN [NormAttributeTransactions] B ON A.Id = B.NormParameter_FK_Id  
	WHERE A.Name IN ('StartDate','EndDate') and A.Plant_FK_Id = @plantId and B.AuditYear = @finYear


END
GO
/****** Object:  StoredProcedure [dbo].[CPP_UpdateCPPNorms]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE PROCEDURE [dbo].[CPP_UpdateCPPNorms]
(
    @Id UNIQUEIDENTIFIER,
    @NormsHeaderFkId UNIQUEIDENTIFIER,
    @FinancialYear NVARCHAR(20),
    @AOPYear NVARCHAR(20),
    @NormTypeFkId INT,
    @Apr_Norms DECIMAL(18,6) = NULL,
    @May_Norms DECIMAL(18,6) = NULL,
    @Jun_Norms DECIMAL(18,6) = NULL,
    @Jul_Norms DECIMAL(18,6) = NULL,
    @Aug_Norms DECIMAL(18,6) = NULL,
    @Sep_Norms DECIMAL(18,6) = NULL,
    @Oct_Norms DECIMAL(18,6) = NULL,
    @Nov_Norms DECIMAL(18,6) = NULL,
    @Dec_Norms DECIMAL(18,6) = NULL,
    @Jan_Norms DECIMAL(18,6) = NULL,
    @Feb_Norms DECIMAL(18,6) = NULL,
    @Mar_Norms DECIMAL(18,6) = NULL,
    @Remarks NVARCHAR(1000) = NULL,
    @ApplyActualNormToAll BIT = 0,
    @ModifiedBy NVARCHAR(100) = NULL
)
AS
BEGIN
    SET NOCOUNT ON;
    
    BEGIN TRY
        BEGIN TRANSACTION;
        
        DECLARE @ExistingId UNIQUEIDENTIFIER;
        DECLARE @IsUpdate BIT = 0;
        
        -- Check if record exists
        SELECT @ExistingId = Id 
        FROM CPPNorms 
        WHERE NormsHeader_FK_Id = @NormsHeaderFkId 
        AND FinancialYear = @FinancialYear;
        
        IF @ExistingId IS NOT NULL
        BEGIN
            SET @IsUpdate = 1;
            SET @Id = @ExistingId;
        END
        
        -- Insert or Update CPPNorms
        IF @IsUpdate = 1
        BEGIN
            UPDATE CPPNorms
            SET 
                AOPYear = @AOPYear,
                NormType_FK_Id = @NormTypeFkId,
                Apr_Norms = @Apr_Norms,
                May_Norms = @May_Norms,
                Jun_Norms = @Jun_Norms,
                Jul_Norms = @Jul_Norms,
                Aug_Norms = @Aug_Norms,
                Sep_Norms = @Sep_Norms,
                Oct_Norms = @Oct_Norms,
                Nov_Norms = @Nov_Norms,
                Dec_Norms = @Dec_Norms,
                Jan_Norms = @Jan_Norms,
                Feb_Norms = @Feb_Norms,
                Mar_Norms = @Mar_Norms,
                Remarks = @Remarks,
                ApplyActualNormToAll = @ApplyActualNormToAll,
                ModifiedBy = @ModifiedBy,
                ModifiedDate = GETDATE()
            WHERE Id = @Id;
        END
        ELSE
        BEGIN
            IF @Id IS NULL SET @Id = NEWID();
            
            INSERT INTO CPPNorms (
                Id, NormsHeader_FK_Id, FinancialYear, AOPYear, NormType_FK_Id,
                Apr_Norms, May_Norms, Jun_Norms, Jul_Norms, Aug_Norms, Sep_Norms,
                Oct_Norms, Nov_Norms, Dec_Norms, Jan_Norms, Feb_Norms, Mar_Norms,
                Remarks, ApplyActualNormToAll, CreatedBy, CreatedDate
            )
            VALUES (
                @Id, @NormsHeaderFkId, @FinancialYear, @AOPYear, @NormTypeFkId,
                @Apr_Norms, @May_Norms, @Jun_Norms, @Jul_Norms, @Aug_Norms, @Sep_Norms,
                @Oct_Norms, @Nov_Norms, @Dec_Norms, @Jan_Norms, @Feb_Norms, @Mar_Norms,
                @Remarks, @ApplyActualNormToAll, @ModifiedBy, GETDATE()
            );
        END
        
        -- Sync to NormsMonthDetail
        -- Parse financial year to get start and end years
        DECLARE @CleanYear NVARCHAR(20) = ISNULL(@FinancialYear, '');
        SET @CleanYear = REPLACE(@CleanYear, ' ', '');
        SET @CleanYear = REPLACE(@CleanYear, '/', '-');

        IF @CleanYear = '' SET @CleanYear = '1900-01';

        IF @CleanYear NOT LIKE '%-%'
        BEGIN
            SET @CleanYear = CONCAT(
                @CleanYear, '-', 
                RIGHT(CONCAT('0', CAST(CAST(RIGHT(@CleanYear,2) AS INT) + 1 AS VARCHAR(4))),2)
            );
        END;

        DECLARE @Part1 NVARCHAR(4), @Part2 NVARCHAR(4);
        SET @Part1 = LEFT(@CleanYear, CHARINDEX('-', @CleanYear) - 1);
        SET @Part2 = RIGHT(@CleanYear, LEN(@CleanYear) - CHARINDEX('-', @CleanYear));

        IF LEN(@Part1) = 2 SET @Part1 = CONCAT('20', @Part1);
        IF LEN(@Part2) = 2 SET @Part2 = CONCAT('20', @Part2);

        DECLARE @StartYear INT = TRY_CAST(@Part1 AS INT);
        DECLARE @EndYear   INT = TRY_CAST(@Part2 AS INT);

        IF @StartYear IS NULL SET @StartYear = 1900;
        IF @EndYear   IS NULL SET @EndYear   = @StartYear + 1;
        
        -- Update NormsMonthDetail for each month
        DECLARE @MonthNorms TABLE (Month INT, Norms DECIMAL(18,6));
        
        INSERT INTO @MonthNorms VALUES 
            (4, @Apr_Norms), (5, @May_Norms), (6, @Jun_Norms), 
            (7, @Jul_Norms), (8, @Aug_Norms), (9, @Sep_Norms),
            (10, @Oct_Norms), (11, @Nov_Norms), (12, @Dec_Norms),
            (1, @Jan_Norms), (2, @Feb_Norms), (3, @Mar_Norms);
        
        UPDATE nmd
        SET nmd.Norms = mn.Norms
        FROM NormsMonthDetail nmd
        INNER JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
        INNER JOIN @MonthNorms mn ON mn.Month = fym.Month
        WHERE nmd.NormsHeader_FK_Id = @NormsHeaderFkId
        AND (
            (fym.Year = @StartYear AND fym.Month >= 4)
            OR
            (fym.Year = @EndYear AND fym.Month <= 3)
        );
        
        COMMIT TRANSACTION;
        
        SELECT @Id AS Id, 'Success' AS Status, 'CPPNorms updated and synced to NormsMonthDetail' AS Message;
        
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;
            
        DECLARE @ErrorMessage NVARCHAR(4000) = ERROR_MESSAGE();
        DECLARE @ErrorSeverity INT = ERROR_SEVERITY();
        DECLARE @ErrorState INT = ERROR_STATE();
        
        RAISERROR(@ErrorMessage, @ErrorSeverity, @ErrorState);
    END CATCH
END
GO
/****** Object:  StoredProcedure [dbo].[CPP_UpdateNormsFromPythonModel]    Script Date: 2/27/2026 5:41:38 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE   PROCEDURE [dbo].[CPP_UpdateNormsFromPythonModel]
(
    @NormsHeaderFkId UNIQUEIDENTIFIER,
    @FinancialYearMonthFkId UNIQUEIDENTIFIER,
    @Norms DECIMAL(18,6),
    @ModifiedBy NVARCHAR(100) = 'PythonModel'
)
AS
BEGIN
    SET NOCOUNT ON;
    
    BEGIN TRY
        BEGIN TRANSACTION;
        
        -- Update NormsMonthDetail
        UPDATE NormsMonthDetail
        SET 
            Norms = @Norms
        WHERE NormsHeader_FK_Id = @NormsHeaderFkId
        AND FinancialYearMonth_FK_Id = @FinancialYearMonthFkId;
        
        IF @@ROWCOUNT = 0
        BEGIN
            RAISERROR('No matching record found in NormsMonthDetail', 16, 1);
            ROLLBACK TRANSACTION;
            RETURN;
        END
        
        -- Get month and year from FinancialYearMonth
        DECLARE @Month INT, @Year INT;
        SELECT @Month = Month, @Year = Year 
        FROM FinancialYearMonth 
        WHERE Id = @FinancialYearMonthFkId;
        
        -- Determine financial year (Apr-Mar format)
        DECLARE @FinancialYear NVARCHAR(20);
        IF @Month >= 4
            SET @FinancialYear = CONCAT(@Year, '-', RIGHT(CAST(@Year + 1 AS VARCHAR(4)), 2));
        ELSE
            SET @FinancialYear = CONCAT(@Year - 1, '-', RIGHT(CAST(@Year AS VARCHAR(4)), 2));
        
        -- Check if CPPNorms record exists
        DECLARE @CPPNormsId UNIQUEIDENTIFIER;
        SELECT @CPPNormsId = Id 
        FROM CPPNorms 
        WHERE NormsHeader_FK_Id = @NormsHeaderFkId 
        AND FinancialYear = @FinancialYear;
        
        IF @CPPNormsId IS NULL
        BEGIN
            -- CPPNorms record doesn't exist, skip sync
            PRINT 'Warning: No CPPNorms record found for sync. Skipping CPPNorms update.';
            COMMIT TRANSACTION;
            SELECT 'Success' AS Status, 'NormsMonthDetail updated. No CPPNorms record to sync.' AS Message;
            RETURN;
        END
        
        -- Sync to CPPNorms based on month
        IF @Month = 4
            UPDATE CPPNorms SET Apr_Norms = @Norms, ModifiedBy = @ModifiedBy, ModifiedDate = GETDATE() WHERE Id = @CPPNormsId;
        ELSE IF @Month = 5
            UPDATE CPPNorms SET May_Norms = @Norms, ModifiedBy = @ModifiedBy, ModifiedDate = GETDATE() WHERE Id = @CPPNormsId;
        ELSE IF @Month = 6
            UPDATE CPPNorms SET Jun_Norms = @Norms, ModifiedBy = @ModifiedBy, ModifiedDate = GETDATE() WHERE Id = @CPPNormsId;
        ELSE IF @Month = 7
            UPDATE CPPNorms SET Jul_Norms = @Norms, ModifiedBy = @ModifiedBy, ModifiedDate = GETDATE() WHERE Id = @CPPNormsId;
        ELSE IF @Month = 8
            UPDATE CPPNorms SET Aug_Norms = @Norms, ModifiedBy = @ModifiedBy, ModifiedDate = GETDATE() WHERE Id = @CPPNormsId;
        ELSE IF @Month = 9
            UPDATE CPPNorms SET Sep_Norms = @Norms, ModifiedBy = @ModifiedBy, ModifiedDate = GETDATE() WHERE Id = @CPPNormsId;
        ELSE IF @Month = 10
            UPDATE CPPNorms SET Oct_Norms = @Norms, ModifiedBy = @ModifiedBy, ModifiedDate = GETDATE() WHERE Id = @CPPNormsId;
        ELSE IF @Month = 11
            UPDATE CPPNorms SET Nov_Norms = @Norms, ModifiedBy = @ModifiedBy, ModifiedDate = GETDATE() WHERE Id = @CPPNormsId;
        ELSE IF @Month = 12
            UPDATE CPPNorms SET Dec_Norms = @Norms, ModifiedBy = @ModifiedBy, ModifiedDate = GETDATE() WHERE Id = @CPPNormsId;
        ELSE IF @Month = 1
            UPDATE CPPNorms SET Jan_Norms = @Norms, ModifiedBy = @ModifiedBy, ModifiedDate = GETDATE() WHERE Id = @CPPNormsId;
        ELSE IF @Month = 2
            UPDATE CPPNorms SET Feb_Norms = @Norms, ModifiedBy = @ModifiedBy, ModifiedDate = GETDATE() WHERE Id = @CPPNormsId;
        ELSE IF @Month = 3
            UPDATE CPPNorms SET Mar_Norms = @Norms, ModifiedBy = @ModifiedBy, ModifiedDate = GETDATE() WHERE Id = @CPPNormsId;
        
        COMMIT TRANSACTION;
        
        SELECT 'Success' AS Status, 'NormsMonthDetail and CPPNorms updated successfully' AS Message;
        
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;
            
        DECLARE @ErrorMessage NVARCHAR(4000) = ERROR_MESSAGE();
        DECLARE @ErrorSeverity INT = ERROR_SEVERITY();
        DECLARE @ErrorState INT = ERROR_STATE();
        
        RAISERROR(@ErrorMessage, @ErrorSeverity, @ErrorState);
    END CATCH
END
GO



GO

-- =============================================
-- Additional Stored Procedures from script1.sql
-- =============================================

USE [RIL.AOP]
GO
/****** Object:  StoredProcedure [dbo].[CPP_CalculateHRSGHeatRate_ByDateRange]    Script Date: 2/27/2026 5:56:14 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- =============================================
-- Stored Procedure: CPP_CalculateHRSGHeatRate_ByDateRange
-- Description: Calculate HRSG heat rates for different load points based on date range
-- Author: System
-- Date: 2026-02-23
-- =============================================
-- 
-- Purpose:
-- Calculates HRSG heat rates from FCNA fuel bill data for specific date ranges.
-- Groups data by load ranges (40 MT, 50 MT, 60 MT, etc.) and calculates average heat rate.
-- Uses AvgMW (hourly average) for load bucketing, same as GT heat rate calculation.
--
-- Parameters:
-- @StartDate - Start date for calculation (YYYY-MM-DD)
-- @EndDate - End date for calculation (YYYY-MM-DD)
-- @AssetName - HRSG asset name (e.g., 'HRSG-1', 'HRSG-2', 'HRSG-3')
--
-- Returns:
-- HRSGLoad (MT/hr), HeatRate (kcal/MT)
-- =============================================

CREATE   PROCEDURE [dbo].[CPP_CalculateHRSGHeatRate_ByDateRange]
    @StartDate DATE,
    @EndDate DATE,
    @AssetName NVARCHAR(50)
AS
BEGIN
    SET NOCOUNT ON;
    
    -- Declare variables for load range configuration
    DECLARE @MinLoad DECIMAL(10,2) = 30.0;  -- Starting load: 30 MT/hr
    DECLARE @LoadInterval DECIMAL(10,2) = 5.0;  -- Load interval: 5 MT/hr
    DECLARE @MaxLoad DECIMAL(10,2) = 135.0;  -- Maximum load: 135 MT/hr (max HRSG capacity)
    DECLARE @MinRecordsRequired INT = 3;  -- Minimum 3 samples
    DECLARE @MaxRecordsToUse INT = 50;  -- Maximum 50 samples (same as GT)
    DECLARE @MaxValidHeatRate DECIMAL(18,4) = 10000;  -- Maximum valid heat rate
    DECLARE @FreeSteamFactor DECIMAL(10,4) = 1.97;  -- Free steam factor (same as GT)
    DECLARE @DefaultHeatRateBelow55 DECIMAL(18,4) = 750.0;  -- Default heat rate for load < 55 MT/hr when samples <= 3
    DECLARE @DefaultHeatRateAbove55 DECIMAL(18,4) = 738.0;  -- Default heat rate for load >= 55 MT/hr when samples <= 3
    
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
GO
/****** Object:  StoredProcedure [dbo].[CPP_GetCPPNorms]    Script Date: 2/27/2026 5:56:15 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE   PROCEDURE [dbo].[CPP_GetCPPNorms]
(
    @CPPPlantId UNIQUEIDENTIFIER,
    @FinancialYear NVARCHAR(20)
)
AS
BEGIN
    SET NOCOUNT ON;

    ;WITH GeneratingPlants AS
    (
        SELECT AssociatedPlant_FK_Id AS GeneratingPlantId
        FROM PowerGenerationPlantsMapping
        WHERE CPPPlantId = @CPPPlantId
    ),
    NormData AS
    (
        SELECT 
            nh.Id AS NormHeaderId,
            nh.Plant_FK_Id AS GeneratingPlantId,
            nh.PlantCode,
            nh.UtilityName,
            nh.UtilityId,
            nh.UtilityUOM,
            nh.AccountName,
            nh.MaterialName,
            nh.MaterialId,
            nh.IssuingPlantName,
            nh.IssuingPlant_FK_Id,
            nh.IssuingUOM,
            nh.NormParameter_FK_Id,
            nh.DisplayOrder
        FROM NormsHeader nh
        INNER JOIN GeneratingPlants gp ON gp.GeneratingPlantId = nh.Plant_FK_Id
        WHERE nh.IsActive = 1
            AND nh.AccountName = 'Utilities'  -- Filter for Utilities account only
    ),
    JoinedNorms AS
    (
        SELECT 
            nd.*,
            np.DisplayName AS NormParameterName,
            np.UOM AS ParameterUOM,
            np.SAPMaterialCode,
            np.DisplayOrder AS NormParameterDisplayOrder
        FROM NormData nd
        LEFT JOIN NormParameters np ON nd.NormParameter_FK_Id = np.Id
    ),
    FinalData AS
    (
        SELECT 
            jn.*,
            p.Name AS GeneratingPlantName,
            p.DisplayName AS GeneratingPlantDisplayName,
            p.PlantCode AS GeneratingPlantCode
        FROM JoinedNorms jn
        LEFT JOIN Plants p ON jn.GeneratingPlantId = p.Id
    )
    SELECT
        ROW_NUMBER() OVER (ORDER BY fd.GeneratingPlantName, fd.DisplayOrder, fd.NormParameterDisplayOrder) AS id,
        cn.Id AS cppNormsId,
        fd.NormHeaderId AS normsHeaderFkId,
        fd.GeneratingPlantName AS generatingPlantName,
        fd.UtilityName AS utilityName,
        fd.UtilityId AS utilityId,
        COALESCE(fd.ParameterUOM, fd.UtilityUOM) AS uom,
        fd.AccountName AS accountName,
        fd.MaterialName AS materialName,
        fd.MaterialId AS materialId,
        fd.IssuingPlantName AS issuingPlantName,
        fd.IssuingUOM AS issuingUom,
        cn.AOPYear AS aopYear,
        cn.NormType_FK_Id AS normTypeFkId,
        nt.NormName AS normTypeName,
        cn.Apr_Norms AS aprNorms,
        cn.May_Norms AS mayNorms,
        cn.Jun_Norms AS junNorms,
        cn.Jul_Norms AS julNorms,
        cn.Aug_Norms AS augNorms,
        cn.Sep_Norms AS sepNorms,
        cn.Oct_Norms AS octNorms,
        cn.Nov_Norms AS novNorms,
        cn.Dec_Norms AS decNorms,
        cn.Jan_Norms AS janNorms,
        cn.Feb_Norms AS febNorms,
        cn.Mar_Norms AS marNorms,
        cn.Remarks AS remarks,
        cn.ModifiedBy AS modifiedBy,
        cn.ModifiedDate AS modifiedDate,
        calc.NORSM_Value AS calculatedNorms,
        cn.ApplyActualNormToAll AS applyActualNormToAll
    FROM FinalData fd
    INNER JOIN CPPNorms cn 
        ON cn.NormsHeader_FK_Id = fd.NormHeaderId 
        AND cn.FinancialYear = @FinancialYear
    INNER JOIN NormTypes nt ON nt.Id = cn.NormType_FK_Id
    LEFT JOIN CPP_utilitiesCalculatednorms calc
        ON calc.Plant = fd.PlantCode
        AND calc.Input_Material_Name = fd.MaterialName
        AND calc.Product_Material_Name = fd.UtilityName
        AND calc.FinancialYear = @FinancialYear
    WHERE nt.NormName = 'Fixed'
    ORDER BY fd.GeneratingPlantName, fd.DisplayOrder, fd.NormParameterDisplayOrder;

END
GO
/****** Object:  StoredProcedure [dbo].[CPP_NMD_Get_UtilityPlant_OperationalHours]    Script Date: 2/27/2026 5:56:15 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE   PROCEDURE [dbo].[CPP_NMD_Get_UtilityPlant_OperationalHours]
(
    @cppPlantId UNIQUEIDENTIFIER,
    @financialYear VARCHAR(7)   -- e.g. '2025-26'
)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @StartYear INT = CAST(LEFT(@financialYear, 4) AS INT);
    DECLARE @EndYear   INT = @StartYear + 1;

    /*-------------------------------------------------------
      Get FinancialYearMonth IDs for Apr–Mar
    -------------------------------------------------------*/
    WITH FYMonths AS (
        SELECT 
            fym.Id,
            fym.Month,
            fym.Year
        FROM FinancialYearMonth fym
        WHERE (fym.Year = @StartYear AND fym.Month BETWEEN 4 AND 12)
           OR (fym.Year = @EndYear   AND fym.Month BETWEEN 1 AND 3)
    )

    /*-------------------------------------------------------
      Final Result
    -------------------------------------------------------*/
    SELECT
    	Max(pga.AssetId) as AssetId,
		Max(pga_ccp.AssetName) as AssetName,
		Max(u.Type) as AssetType,
        u.PlantAsset as UtilityPlantAsset, 
        u.Id AS UtilityPlantAssetId,
        nd.Name AS UtilityDistributed,
		nd.SAPMaterialCode AS UtilityDistributedSAPCode,
        ug.Name AS UtilityGenerated,
		ug.SAPMaterialCode AS UtilityGeneratedSAPCode,
        u.Type,

        MAX(CASE WHEN fm.Month = 4  THEN oh.OperationalHours END) AS Apr,
        MAX(CASE WHEN fm.Month = 5  THEN oh.OperationalHours END) AS May,
        MAX(CASE WHEN fm.Month = 6  THEN oh.OperationalHours END) AS Jun,
        MAX(CASE WHEN fm.Month = 7  THEN oh.OperationalHours END) AS Jul,
        MAX(CASE WHEN fm.Month = 8  THEN oh.OperationalHours END) AS Aug,
        MAX(CASE WHEN fm.Month = 9  THEN oh.OperationalHours END) AS Sep,
        MAX(CASE WHEN fm.Month = 10 THEN oh.OperationalHours END) AS Oct,
        MAX(CASE WHEN fm.Month = 11 THEN oh.OperationalHours END) AS Nov,
        MAX(CASE WHEN fm.Month = 12 THEN oh.OperationalHours END) AS Dec,
        MAX(CASE WHEN fm.Month = 1  THEN oh.OperationalHours END) AS Jan,
        MAX(CASE WHEN fm.Month = 2  THEN oh.OperationalHours END) AS Feb,
        MAX(CASE WHEN fm.Month = 3  THEN oh.OperationalHours END) AS Mar

    FROM UtilityPlantAssets u
        INNER JOIN PowerGenerationAssets pga_ccp
            ON pga_ccp.AssetId = u.PowerGenerationAsset_FK_Id
           AND pga_ccp.CPPPLANT_FK_Id = @cppPlantId

        LEFT JOIN NormParameters nd
            ON nd.Id = u.UtilityDistributed

        LEFT JOIN NormParameters ug
            ON ug.Id = u.UtilityGenerated

        JOIN PowerGenerationAssets pga    -- nodified to simple join
            ON pga.AssetId = u.Linked_OpHrs_Asset

        LEFT JOIN OperationalHours oh
            ON oh.Asset_FK_Id = u.Linked_OpHrs_Asset

        LEFT JOIN FYMonths fm
            ON fm.Id = oh.FinancialMonthId

    WHERE u.FinancialYear = @financialYear

    GROUP BY
        u.PlantAsset,
        u.Id,
        nd.Name,
        ug.Name,
		ug.SAPMaterialCode,
		nd.SAPMaterialCode,
        pga.AssetName,
        u.Type

    ORDER BY u.PlantAsset;

END;
GO
/****** Object:  StoredProcedure [dbo].[CPP_NMD_Get_UtilityPlantAssets]    Script Date: 2/27/2026 5:56:15 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE   PROCEDURE [dbo].[CPP_NMD_Get_UtilityPlantAssets]
(
    @CppId UNIQUEIDENTIFIER,
    @FinancialYear VARCHAR(10)
)
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        PGA.AssetName,
        UPA.Id as UtilityPlantAssetId,
        UPA.PlantAsset as UtilityPlantAsset,
        NG.Name AS UtilityGenerated,
		NG.SAPMaterialCode as UtilityGeneratedSAPCode,
        ND.Name AS UtilityDistributed,
		ND.SAPMaterialCode as UtilityDistributedSAPCode,
        UPA.Apr,
        UPA.May,
        UPA.Jun,
        UPA.Jul,
        UPA.Aug,
        UPA.Sep,
        UPA.Oct,
        UPA.Nov,
        UPA.Dec,
        UPA.Jan,
        UPA.Feb,
        UPA.Mar,
        UPA.Remarks,
        UPA.Type

    FROM PowerGenerationAssets PGA
    INNER JOIN UtilityPlantAssets UPA
        ON UPA.PowerGenerationAsset_FK_Id = PGA.AssetId

    LEFT JOIN NormParameters NG
        ON NG.Id = UPA.UtilityGenerated

    LEFT JOIN NormParameters ND
        ON ND.Id = UPA.UtilityDistributed

    WHERE PGA.CPPPLANT_FK_Id = @CppId
      AND UPA.FinancialYear = @FinancialYear

	  order by UPA.PlantAsset;
END
GO
/****** Object:  StoredProcedure [dbo].[CPP_NMD_GetAssetCapacity]    Script Date: 2/27/2026 5:56:15 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO



CREATE     PROCEDURE [dbo].[CPP_NMD_GetAssetCapacity]
(
    @CppId UNIQUEIDENTIFIER,
    @FinancialYear VARCHAR(7)   -- e.g. '2025-26'
)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @StartYear INT = CAST(LEFT(@FinancialYear, 4) AS INT);
    DECLARE @EndYear   INT = CAST('20' + RIGHT(@FinancialYear, 2) AS INT);

    ;WITH FYM AS (
        SELECT
            fym.Id,
            fym.Month,
            fym.Year
        FROM FinancialYearMonth fym
        WHERE
            (fym.Year = @StartYear AND fym.Month BETWEEN 4 AND 12)
         OR (fym.Year = @EndYear   AND fym.Month BETWEEN 1 AND 3)
    )
    SELECT
        -- GenerationPlant
        pga.AssetName,
		pga.AssetId,
        pga.PlantCode,
		

        -- UtilityDistributed (NormType_FK_Id = 2)
        MAX(CASE WHEN np.NormType_FK_Id = 2 THEN np.DisplayName END)     AS UtilityDistributedName,
        MAX(CASE WHEN np.NormType_FK_Id = 2 THEN np.SAPMaterialCode END) AS UtilityDistributedSAP,

        -- UtilityGenerated (NormType_FK_Id = 1)
        MAX(CASE WHEN np.NormType_FK_Id = 1 THEN np.DisplayName END)     AS UtilityGeneratedName,
        MAX(CASE WHEN np.NormType_FK_Id = 1 THEN np.SAPMaterialCode END) AS UtilityGeneratedSAP,

        -- UOM (assuming same per asset)
        MAX(np.UOM) AS UOM,

        -- Fixed Min / Max (same across months)
        MAX(aa.FixedMin) AS FixedMin,
        MAX(aa.FixedMax) AS FixedMax,
		MAX(aa.Remarks) as Remarks,

     -- April
MAX(CASE WHEN fym.Month = 4 THEN aa.MinOperatingCapacity END) AS AprMinCapacity,
MAX(CASE WHEN fym.Month = 4 THEN aa.MaxOperatingCapacity END) AS AprMaxCapacity,

-- May
MAX(CASE WHEN fym.Month = 5 THEN aa.MinOperatingCapacity END) AS MayMinCapacity,
MAX(CASE WHEN fym.Month = 5 THEN aa.MaxOperatingCapacity END) AS MayMaxCapacity,

-- June
MAX(CASE WHEN fym.Month = 6 THEN aa.MinOperatingCapacity END) AS JunMinCapacity,
MAX(CASE WHEN fym.Month = 6 THEN aa.MaxOperatingCapacity END) AS JunMaxCapacity,

-- July
MAX(CASE WHEN fym.Month = 7 THEN aa.MinOperatingCapacity END) AS JulMinCapacity,
MAX(CASE WHEN fym.Month = 7 THEN aa.MaxOperatingCapacity END) AS JulMaxCapacity,

-- August
MAX(CASE WHEN fym.Month = 8 THEN aa.MinOperatingCapacity END) AS AugMinCapacity,
MAX(CASE WHEN fym.Month = 8 THEN aa.MaxOperatingCapacity END) AS AugMaxCapacity,

-- September
MAX(CASE WHEN fym.Month = 9 THEN aa.MinOperatingCapacity END) AS SepMinCapacity,
MAX(CASE WHEN fym.Month = 9 THEN aa.MaxOperatingCapacity END) AS SepMaxCapacity,

-- October
MAX(CASE WHEN fym.Month = 10 THEN aa.MinOperatingCapacity END) AS OctMinCapacity,
MAX(CASE WHEN fym.Month = 10 THEN aa.MaxOperatingCapacity END) AS OctMaxCapacity,

-- November
MAX(CASE WHEN fym.Month = 11 THEN aa.MinOperatingCapacity END) AS NovMinCapacity,
MAX(CASE WHEN fym.Month = 11 THEN aa.MaxOperatingCapacity END) AS NovMaxCapacity,

-- December
MAX(CASE WHEN fym.Month = 12 THEN aa.MinOperatingCapacity END) AS DecMinCapacity,
MAX(CASE WHEN fym.Month = 12 THEN aa.MaxOperatingCapacity END) AS DecMaxCapacity,

-- January
MAX(CASE WHEN fym.Month = 1 THEN aa.MinOperatingCapacity END) AS JanMinCapacity,
MAX(CASE WHEN fym.Month = 1 THEN aa.MaxOperatingCapacity END) AS JanMaxCapacity,

-- February
MAX(CASE WHEN fym.Month = 2 THEN aa.MinOperatingCapacity END) AS FebMinCapacity,
MAX(CASE WHEN fym.Month = 2 THEN aa.MaxOperatingCapacity END) AS FebMaxCapacity,

-- March
MAX(CASE WHEN fym.Month = 3 THEN aa.MinOperatingCapacity END) AS MarMinCapacity,
MAX(CASE WHEN fym.Month = 3 THEN aa.MaxOperatingCapacity END) AS MarMaxCapacity


    FROM PowerGenerationAssets pga
	  JOIN AssetAvailability aa
           ON aa.AssetId = pga.AssetId

		   LEFT JOIN CPP_AssetNorms_Mapping anm
		    ON anm.AssetId = pga.AssetId
    LEFT JOIN NormParameters np
           ON np.Id = anm.NormParameters_ID
   
    LEFT JOIN FYM fym
           ON fym.Id = aa.FinancialYearMonthId

    WHERE pga.CPPPLANT_FK_Id = @CppId

    GROUP BY
        pga.AssetId,
        pga.AssetName,
        pga.PlantCode

    ORDER BY pga.AssetName;
END;
GO
/****** Object:  StoredProcedure [dbo].[CPP_NMD_GetAssetPriority]    Script Date: 2/27/2026 5:56:15 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE    PROCEDURE [dbo].[CPP_NMD_GetAssetPriority]
(
    @CppId UNIQUEIDENTIFIER,
    @FinancialYear VARCHAR(9)   -- e.g. '2025-26'
)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @StartYear INT;
    DECLARE @EndYear INT;

    -- Extract years from '2025-26'
    SET @StartYear = CAST(LEFT(@FinancialYear, 4) AS INT);
    SET @EndYear   = CAST('20' + RIGHT(@FinancialYear, 2) AS INT);

    SELECT
        pga.AssetId,
        pga.AssetName,
		Max(pga.Remarks) as Remarks,
		Max(pga.AssetType) as AssetType,
		Max(Priority_Remarks) as Remarks,

        MAX(CASE WHEN fym.Month = 4  AND fym.Year = @StartYear THEN aa.Priority END) AS April,
        MAX(CASE WHEN fym.Month = 5  AND fym.Year = @StartYear THEN aa.Priority END) AS May,
        MAX(CASE WHEN fym.Month = 6  AND fym.Year = @StartYear THEN aa.Priority END) AS June,
        MAX(CASE WHEN fym.Month = 7  AND fym.Year = @StartYear THEN aa.Priority END) AS July,
        MAX(CASE WHEN fym.Month = 8  AND fym.Year = @StartYear THEN aa.Priority END) AS August,
        MAX(CASE WHEN fym.Month = 9  AND fym.Year = @StartYear THEN aa.Priority END) AS September,
        MAX(CASE WHEN fym.Month = 10 AND fym.Year = @StartYear THEN aa.Priority END) AS October,
        MAX(CASE WHEN fym.Month = 11 AND fym.Year = @StartYear THEN aa.Priority END) AS November,
        MAX(CASE WHEN fym.Month = 12 AND fym.Year = @StartYear THEN aa.Priority END) AS December,

        MAX(CASE WHEN fym.Month = 1  AND fym.Year = @EndYear THEN aa.Priority END) AS January,
        MAX(CASE WHEN fym.Month = 2  AND fym.Year = @EndYear THEN aa.Priority END) AS February,
        MAX(CASE WHEN fym.Month = 3  AND fym.Year = @EndYear THEN aa.Priority END) AS March

    FROM PowerGenerationAssets pga
     JOIN AssetAvailability aa
        ON aa.AssetId = pga.AssetId
    LEFT JOIN FinancialYearMonth fym
        ON fym.Id = aa.FinancialYearMonthId
       AND (
                (fym.Year = @StartYear AND fym.Month BETWEEN 4 AND 12)
             OR (fym.Year = @EndYear   AND fym.Month BETWEEN 1 AND 3)
           )

    WHERE pga.CPPPLANT_FK_Id = @CppId

    GROUP BY
        pga.AssetId,
        pga.AssetName

    ORDER BY
        pga.AssetName;
END;
GO
/****** Object:  StoredProcedure [dbo].[CPP_NMD_GetFixedConsumptionByPlant]    Script Date: 2/27/2026 5:56:15 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE   PROCEDURE [dbo].[CPP_NMD_GetFixedConsumptionByPlant]
(
    @PlantId       UNIQUEIDENTIFIER,
    @FinancialYear NVARCHAR(20)  -- e.g. '2025-26'
)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @StartYear INT,
            @EndYear   INT,
            @cols      NVARCHAR(MAX),
            @sql       NVARCHAR(MAX);

    -- '2025-26' -> @StartYear = 2025, @EndYear = 2026
    SET @StartYear = CAST(LEFT(@FinancialYear, 4) AS INT);
    SET @EndYear   = CAST(LEFT(@FinancialYear, 2) + RIGHT(@FinancialYear, 2) AS INT);

    -- Fixed month columns: Apr–Mar
    SET @cols = '[Apr],[May],[Jun],[Jul],[Aug],[Sep],[Oct],[Nov],[Dec],[Jan],[Feb],[Mar]';

    SET @sql = N'
    ;WITH Meta AS
    (
        -- All plant / CC / utility combinations for this plant
        SELECT DISTINCT
            pm.PlantName,
            pm.PlantCode,
            cc.CostCenterName,
            cc.CostCenterCode,
            cc.CostCenterId,
            np.Name AS UtilityName,
            np.SAPMaterialCode AS UtilitySAP,
            -- Special handling: For Water utility, always use NMD-Rev Proc
            CASE 
                WHEN np.Name = ''Water'' THEN ''NMD-Rev Proc''
                ELSE COALESCE(up.Name, pm.PlantName)
            END AS UtilityPlantName,
            CASE 
                WHEN np.Name = ''Water'' THEN ''40N0''
                ELSE COALESCE(up.PlantCode, pm.PlantCode)
            END AS UtilityPlantCode,
            np.UOM,
            np.Id AS NormParameterId
        FROM [RIL.AOP].dbo.FixedConsumptionPlantMapping pm
        JOIN [RIL.AOP].dbo.CPPCostCenters cc
             ON cc.Plant_FK_Id = pm.Id
        JOIN [RIL.AOP].dbo.CostCenterNormParameterMapping map
             ON map.CostCenterFK_Id = cc.CostCenterId
        JOIN [RIL.AOP].[dbo].[NormParameters] np
             ON np.Id = map.NormParameterFK_Id
        LEFT JOIN [RIL.AOP].dbo.Plants up
             ON up.Id = np.Plant_FK_Id
        WHERE pm.Plant_FK_Id = @PlantId
    ),
    BaseData AS
    (
        -- Left join FY consumption for the requested FY
        SELECT
            m.PlantName,
            m.PlantCode,
            m.CostCenterName,
            m.CostCenterCode,
            m.CostCenterId,
            m.UtilityName,
            m.UtilitySAP,
            m.UtilityPlantName,
            m.UtilityPlantCode,
            m.UOM,
            m.NormParameterId,
			rm.Remarks,
			rm.Id as RemarkId,
			ufc.NormParameter_FK_Id,
			ufc.CostCenter_FK_Id,
            CASE 
                WHEN fy.Month = 4  THEN ''Apr''
                WHEN fy.Month = 5  THEN ''May''
                WHEN fy.Month = 6  THEN ''Jun''
                WHEN fy.Month = 7  THEN ''Jul''
                WHEN fy.Month = 8  THEN ''Aug''
                WHEN fy.Month = 9  THEN ''Sep''
                WHEN fy.Month = 10 THEN ''Oct''
                WHEN fy.Month = 11 THEN ''Nov''
                WHEN fy.Month = 12 THEN ''Dec''
                WHEN fy.Month = 1  THEN ''Jan''
                WHEN fy.Month = 2  THEN ''Feb''
                WHEN fy.Month = 3  THEN ''Mar''
            END AS MonthName,
            ufc.ConsumptionValue
        FROM Meta m
        LEFT JOIN [RIL.AOP].dbo.UtilityFixedConsumption ufc
             ON ufc.CostCenter_FK_Id    = m.CostCenterId
            AND ufc.NormParameter_FK_Id = m.NormParameterId
        LEFT JOIN [RIL.AOP].dbo.FinancialYearMonth fy
             ON fy.Id = ufc.FinancialYearMonth_FK_Id
            AND (
                    (fy.Year = @StartYear AND fy.Month BETWEEN 4 AND 12)
                 OR (fy.Year = @EndYear   AND fy.Month BETWEEN 1 AND 3)
                )

		Left Join UtilityFixedConsumption_Remarks rm
		on rm.NormParameter_FK_Id = ufc.NormParameter_FK_Id
		AND rm.CostCenter_FK_Id = ufc.CostCenter_FK_Id
    )
    SELECT
        PlantName,
        PlantCode,
        CostCenterName,
        CostCenterCode,
        UtilityName,
        UtilitySAP,
        UtilityPlantName,
        UtilityPlantCode,
        UOM,
		NormParameterId,
		Remarks,
		RemarkId,
		NormParameter_FK_Id,
		CostCenter_FK_Id,
        ' + @cols + N'
    FROM BaseData
    PIVOT
    (
        MAX(ConsumptionValue)
        FOR MonthName IN (' + @cols + N')
    ) AS pvt
    ORDER BY 
        PlantName,
        CostCenterName,
        UtilityName;';

    EXEC sp_executesql
        @sql,
        N'@PlantId UNIQUEIDENTIFIER, @StartYear INT, @EndYear INT',
        @PlantId = @PlantId,
        @StartYear = @StartYear,
        @EndYear = @EndYear;
END;

GO
/****** Object:  StoredProcedure [dbo].[CPP_NMD_GetNormBasedUtilityBudget]    Script Date: 2/27/2026 5:56:15 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE   PROCEDURE [dbo].[CPP_NMD_GetNormBasedUtilityBudget]
(
    @CPPPlantId UNIQUEIDENTIFIER,
    @FinancialYear NVARCHAR(20)
)
AS
BEGIN
    SET NOCOUNT ON;

    -------------------------------------------------------------------------
    -- FINANCIAL YEAR PARSING
    -------------------------------------------------------------------------
    DECLARE @CleanYear NVARCHAR(20) = ISNULL(@FinancialYear, '');
    SET @CleanYear = REPLACE(@CleanYear, ' ', '');
    SET @CleanYear = REPLACE(@CleanYear, '/', '-');

    IF @CleanYear = '' SET @CleanYear = '1900-01';

    IF @CleanYear NOT LIKE '%-%'
    BEGIN
        SET @CleanYear = CONCAT(
            @CleanYear, '-', 
            RIGHT(CONCAT('0', CAST(CAST(RIGHT(@CleanYear,2) AS INT) + 1 AS VARCHAR(4))),2)
        );
    END;

    DECLARE @Part1 NVARCHAR(4), @Part2 NVARCHAR(4);

    SET @Part1 = LEFT(@CleanYear, CHARINDEX('-', @CleanYear) - 1);
    SET @Part2 = RIGHT(@CleanYear, LEN(@CleanYear) - CHARINDEX('-', @CleanYear));

    IF LEN(@Part1) = 2 SET @Part1 = CONCAT('20', @Part1);
    IF LEN(@Part2) = 2 SET @Part2 = CONCAT('20', @Part2);

    DECLARE @StartYear INT = TRY_CAST(@Part1 AS INT);
    DECLARE @EndYear   INT = TRY_CAST(@Part2 AS INT);

    IF @StartYear IS NULL SET @StartYear = 1900;
    IF @EndYear   IS NULL SET @EndYear   = @StartYear + 1;

    -------------------------------------------------------------------------
    -- FETCH GENERATING PLANTS
    -------------------------------------------------------------------------
    ;WITH GeneratingPlants AS
    (
        SELECT AssociatedPlant_FK_Id AS GeneratingPlantId
        FROM PowerGenerationPlantsMapping
        WHERE CPPPlantId = @CPPPlantId
    ),

    NormData AS
    (
        SELECT 
            nh.Id AS NormHeaderId,
            nh.Plant_FK_Id AS GeneratingPlantId,
            nh.UtilityName,
            nh.UtilityId,
            nh.UtilityUOM,
            nh.AccountName,
            nh.MaterialName,
			nh.MaterialId,            -- modified
            nh.IssuingPlantName,
            nh.IssuingPlant_FK_Id,
            nh.IssuingUOM,
            nh.NormParameter_FK_Id,
            nh.DisplayOrder
			
        FROM NormsHeader nh
        INNER JOIN GeneratingPlants gp
            ON gp.GeneratingPlantId = nh.Plant_FK_Id
        WHERE nh.IsActive = 1
    ),

    JoinedNorms AS
    (
        SELECT 
            nd.*,
            np.DisplayName AS NormParameterName,
            np.UOM AS ParameterUOM,
            np.SAPMaterialCode,
            np.DisplayOrder AS NormParameterDisplayOrder
        FROM NormData nd
        LEFT JOIN NormParameters np
            ON nd.NormParameter_FK_Id = np.Id
    ),

    FinalData AS
    (
        SELECT 
            jn.*,
            p.Name AS GeneratingPlantName,
            p.DisplayName AS GeneratingPlantDisplayName,
            p.PlantCode AS GeneratingPlantCode
        FROM JoinedNorms jn
        LEFT JOIN Plants p
            ON jn.GeneratingPlantId = p.Id
    ),

    -------------------------------------------------------------------------
    -- MONTH DETAILS FILTERED BY FINANCIAL YEAR (APR–MAR)
    -------------------------------------------------------------------------
    MonthDetails AS
    (
        SELECT 
            nmd.NormsHeader_FK_Id,
            fym.Month,
            fym.Year,
            fym.Id AS FinancialYearMonthId,
            nmd.Norms,
            nmd.Quantity,
            nmd.Amount,
            nmd.Price,
            nmd.GenerationUOM,
            nmd.QTY,
            nmd.ScenarioType,
            nmd.DisplayOrder,
			nmd.Remarks
        FROM NormsMonthDetail nmd
        INNER JOIN FinancialYearMonth fym
            ON fym.Id = nmd.FinancialYearMonth_FK_Id
        WHERE 
            (
                fym.Year = @StartYear AND fym.Month >= 4  -- Apr–Dec of start year
            )
            OR
            (
                fym.Year = @EndYear   AND fym.Month <= 3  -- Jan–Mar of end year
            )
    ),

    -------------------------------------------------------------------------
    -- EXTRACT GENERATION UOM (COMMON FOR ALL MONTHS - TAKE FROM FIRST AVAILABLE)
    -------------------------------------------------------------------------
    GenerationUOMData AS
    (
        SELECT 
            fd.NormHeaderId,
            (SELECT TOP 1 md.GenerationUOM 
             FROM MonthDetails md 
             WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.GenerationUOM IS NOT NULL) AS CommonGenerationUOM
        FROM FinalData fd
    )

    SELECT
        ROW_NUMBER() OVER (ORDER BY fd.GeneratingPlantName, fd.DisplayOrder, fd.NormParameterDisplayOrder) AS id,

        fd.NormHeaderId AS normsHeaderFkId,
        fd.GeneratingPlantName AS generatingPlantName,
        fd.UtilityName        AS utilityName,
        fd.UtilityId          AS utilityId,
        COALESCE(fd.ParameterUOM, fd.UtilityUOM) AS uom,
        fd.AccountName        AS accountName,
        fd.MaterialName       AS materialName,
		fd.MaterialId         AS materialId,      -- modified
        fd.IssuingPlantName   AS issuingPlantName,
        fd.IssuingUOM         AS issuingUom,
        COALESCE(guom.CommonGenerationUOM, '') AS generationUom,  -- Common generation UOM (new field)

        -- APR
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md 
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 4
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS apr,

        -- MAY
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md 
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 5
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS may,

        -- JUN
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md 
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 6
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS jun,

        -- JUL
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 7
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS jul,

        -- AUG
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md 
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 8
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS aug,

        -- SEP
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md 
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 9
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS sep,

        -- OCT
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md 
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 10
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS oct,

        -- NOV
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md 
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 11
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS nov,

        -- DEC
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md 
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 12
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS [dec],

        -- JAN
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md 
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 1
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS jan,

        -- FEB
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md 
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 2
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS feb,

        -- MAR
        ISNULL((SELECT 
                md.Norms AS norms,
                md.Quantity AS quantity,
                md.Amount AS amount,
                md.Price AS price,
                md.FinancialYearMonthId AS financialYearMonthFkId,
                md.QTY AS QTY,
                md.ScenarioType AS scenarioType,
                md.DisplayOrder AS displayOrder,
				md.Remarks as remarks       -- modified
                FROM MonthDetails md 
                WHERE md.NormsHeader_FK_Id = fd.NormHeaderId AND md.Month = 3
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"financialYearMonthFkId":null,"QTY":null,"scenarioType":null,"displayOrder":null,"remarks":null}') AS mar

    FROM FinalData fd
    LEFT JOIN GenerationUOMData guom
        ON fd.NormHeaderId = guom.NormHeaderId
    ORDER BY fd.GeneratingPlantName, fd.DisplayOrder, fd.NormParameterDisplayOrder;

END;
GO


