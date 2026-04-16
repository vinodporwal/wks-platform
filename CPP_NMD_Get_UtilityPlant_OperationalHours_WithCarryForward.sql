USE [RIL.AOP]
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

-- =============================================
-- Author:      CPP Team
-- Create date: 2026-03-10
-- Description: Get Utility Plant Operational Hours with Carry Forward support
--              If no data exists for the requested year, returns previous year's data
--              with "Carry Forwarded" remark
-- =============================================
CREATE PROCEDURE [dbo].[CPP_NMD_Get_UtilityPlant_OperationalHours_WithCarryForward]
(
    @cppPlantId UNIQUEIDENTIFIER,
    @financialYear VARCHAR(7)   -- e.g. '2025-26'
)
AS
BEGIN
    SET NOCOUNT ON;
    SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;

    DECLARE @StartYear INT = CAST(LEFT(@financialYear, 4) AS INT);
    DECLARE @EndYear   INT = @StartYear + 1;
    
    -- Calculate previous financial year
    DECLARE @PrevStartYear INT = @StartYear - 1;
    DECLARE @PrevFinancialYear VARCHAR(7) = CAST(@PrevStartYear AS VARCHAR(4)) + '-' + RIGHT(CAST(@StartYear AS VARCHAR(4)), 2);

    /*-------------------------------------------------------
      Check if data exists for the requested financial year
    -------------------------------------------------------*/
    DECLARE @DataExists BIT = 0;
    
    IF EXISTS (
        SELECT 1 
        FROM UtilityPlantAssets u
        WHERE u.FinancialYear = @financialYear
          AND EXISTS (
              SELECT 1 
              FROM PowerGenerationAssets pga_ccp
              WHERE pga_ccp.AssetId = u.PowerGenerationAsset_FK_Id
                AND pga_ccp.CPPPLANT_FK_Id = @cppPlantId
          )
    )
    BEGIN
        SET @DataExists = 1;
    END

    -- If no data exists, use previous year
    DECLARE @EffectiveYear VARCHAR(7) = CASE WHEN @DataExists = 1 THEN @financialYear ELSE @PrevFinancialYear END;
    DECLARE @EffectiveStartYear INT = CAST(LEFT(@EffectiveYear, 4) AS INT);
    DECLARE @EffectiveEndYear INT = @EffectiveStartYear + 1;

    /*-------------------------------------------------------
      Get FinancialYearMonth IDs for Apr–Mar
    -------------------------------------------------------*/
    WITH FYMonths AS (
        SELECT 
            fym.Id,
            fym.Month,
            fym.Year
        FROM FinancialYearMonth fym
        WHERE (fym.Year = @EffectiveStartYear AND fym.Month BETWEEN 4 AND 12)
           OR (fym.Year = @EffectiveEndYear   AND fym.Month BETWEEN 1 AND 3)
    )

    /*-------------------------------------------------------
      Final Result with Carry Forward Remark
    -------------------------------------------------------*/
    SELECT
        MAX(pga.AssetId) as AssetId,
        MAX(pga_ccp.AssetName) as AssetName,
        MAX(u.Type) as AssetType,
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
        MAX(CASE WHEN fm.Month = 3  THEN oh.OperationalHours END) AS Mar,
        
        -- Add remark indicating if data is carry forwarded
        CASE 
            WHEN @DataExists = 0 THEN 'Carry Forwarded from ' + @PrevFinancialYear
            ELSE NULL
        END AS Remark,
        
        -- Additional metadata
        @financialYear AS RequestedYear,
        @EffectiveYear AS EffectiveYear,
        @DataExists AS IsCurrentYearData

    FROM UtilityPlantAssets u
        INNER JOIN PowerGenerationAssets pga_ccp
            ON pga_ccp.AssetId = u.PowerGenerationAsset_FK_Id
           AND pga_ccp.CPPPLANT_FK_Id = @cppPlantId

        LEFT JOIN NormParameters nd
            ON nd.Id = u.UtilityDistributed

        LEFT JOIN NormParameters ug
            ON ug.Id = u.UtilityGenerated

        JOIN PowerGenerationAssets pga
            ON pga.AssetId = u.Linked_OpHrs_Asset

        LEFT JOIN OperationalHours oh
            ON oh.Asset_FK_Id = u.Linked_OpHrs_Asset

        LEFT JOIN FYMonths fm
            ON fm.Id = oh.FinancialMonthId

    WHERE u.FinancialYear = @EffectiveYear

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
