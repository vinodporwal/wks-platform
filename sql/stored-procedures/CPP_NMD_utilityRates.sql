USE [RIL.AOP]
GO
/****** Object:  StoredProcedure [dbo].[CPP_NMD_utilityRates] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE OR ALTER PROCEDURE [dbo].[CPP_NMD_utilityRates]
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
    -- GENERATING PLANTS
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
            nh.Plant_FK_Id AS PlantId,
            nh.UtilityName,
            nh.UtilityId,
            nh.UtilityUOM,
            nh.DisplayOrder
        FROM NormsHeader nh
        INNER JOIN GeneratingPlants gp
            ON gp.GeneratingPlantId = nh.Plant_FK_Id
        WHERE nh.IsActive = 1
    ),
    UtilityGroups AS
    (
        SELECT
            nd.PlantId,
            nd.UtilityName,
            nd.UtilityId,
            nd.UtilityUOM,
            MIN(nd.DisplayOrder) AS UtilityDisplayOrder
        FROM NormData nd
        GROUP BY
            nd.PlantId,
            nd.UtilityName,
            nd.UtilityId,
            nd.UtilityUOM
    ),
    PlantData AS
    (
        SELECT
            ug.PlantId,
            s.DisplayName AS SiteDescription,
            p.DisplayName AS UtilityPlant,
            p.PlantCode AS UtilityPlantId,
            ug.UtilityName,
            ug.UtilityId,
            ug.UtilityUOM,
            ug.UtilityDisplayOrder
        FROM UtilityGroups ug
        LEFT JOIN Plants p
            ON ug.PlantId = p.Id
        LEFT JOIN Sites s
            ON p.Site_FK_Id = s.Id
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
            nmd.Norms,
            nmd.Price,
            nmd.QTY
        FROM NormsMonthDetail nmd
        INNER JOIN FinancialYearMonth fym
            ON fym.Id = nmd.FinancialYearMonth_FK_Id
        WHERE 
            (
                fym.Year = @StartYear AND fym.Month >= 4
            )
            OR
            (
                fym.Year = @EndYear   AND fym.Month <= 3
            )
    ),
    MonthAgg AS
    (
        SELECT
            nd.PlantId,
            nd.UtilityName,
            nd.UtilityId,
            nd.UtilityUOM,
            md.Month,
            md.Year,
            SUM(
                CAST(COALESCE(md.Norms, 0) AS DECIMAL(18, 6))
                * CAST(COALESCE(md.Price, 0) AS DECIMAL(18, 6))
                * CAST(COALESCE(md.QTY, 0) AS DECIMAL(18, 6))
            ) AS SumAmount,
            MAX(CAST(COALESCE(md.QTY, 0) AS DECIMAL(18, 6))) AS SumQty
        FROM NormData nd
        INNER JOIN MonthDetails md
            ON md.NormsHeader_FK_Id = nd.NormHeaderId
        GROUP BY
            nd.PlantId,
            nd.UtilityName,
            nd.UtilityId,
            nd.UtilityUOM,
            md.Month,
            md.Year
    ),
    MonthPrice AS
    (
        SELECT
            ma.*,
            CASE
                WHEN ma.SumQty IS NULL OR ma.SumQty = 0 THEN NULL
                ELSE ma.SumAmount / ma.SumQty
            END AS MonthPrice
        FROM MonthAgg ma
    ),
    AnnualAgg AS
    (
        SELECT
            PlantId,
            UtilityName,
            UtilityId,
            UtilityUOM,
            SUM(SumAmount) AS TotalAmount,
            SUM(SumQty) AS TotalQty
        FROM MonthAgg
        GROUP BY
            PlantId,
            UtilityName,
            UtilityId,
            UtilityUOM
    ),
    AnnualPrice AS
    (
        SELECT
            aa.*,
            CASE
                WHEN aa.TotalQty IS NULL OR aa.TotalQty = 0 THEN NULL
                ELSE aa.TotalAmount / aa.TotalQty
            END AS WeightedAvgPrice
        FROM AnnualAgg aa
    )
    SELECT
        ROW_NUMBER() OVER (ORDER BY pd.SiteDescription, pd.UtilityPlant, pd.UtilityDisplayOrder, pd.UtilityName) AS id,
        pd.SiteDescription AS siteDescription,
        pd.UtilityPlant AS utilityPlant,
        pd.UtilityPlantId AS utilityPlantId,
        pd.UtilityName AS utilityName,
        pd.UtilityId AS utilityId,
        pd.UtilityUOM AS uom,
        (SELECT mp.MonthPrice FROM MonthPrice mp WHERE mp.PlantId = pd.PlantId AND mp.UtilityName = pd.UtilityName AND mp.UtilityId = pd.UtilityId AND mp.UtilityUOM = pd.UtilityUOM AND mp.Month = 4) AS apr,
        (SELECT mp.MonthPrice FROM MonthPrice mp WHERE mp.PlantId = pd.PlantId AND mp.UtilityName = pd.UtilityName AND mp.UtilityId = pd.UtilityId AND mp.UtilityUOM = pd.UtilityUOM AND mp.Month = 5) AS may,
        (SELECT mp.MonthPrice FROM MonthPrice mp WHERE mp.PlantId = pd.PlantId AND mp.UtilityName = pd.UtilityName AND mp.UtilityId = pd.UtilityId AND mp.UtilityUOM = pd.UtilityUOM AND mp.Month = 6) AS jun,
        (SELECT mp.MonthPrice FROM MonthPrice mp WHERE mp.PlantId = pd.PlantId AND mp.UtilityName = pd.UtilityName AND mp.UtilityId = pd.UtilityId AND mp.UtilityUOM = pd.UtilityUOM AND mp.Month = 7) AS jul,
        (SELECT mp.MonthPrice FROM MonthPrice mp WHERE mp.PlantId = pd.PlantId AND mp.UtilityName = pd.UtilityName AND mp.UtilityId = pd.UtilityId AND mp.UtilityUOM = pd.UtilityUOM AND mp.Month = 8) AS aug,
        (SELECT mp.MonthPrice FROM MonthPrice mp WHERE mp.PlantId = pd.PlantId AND mp.UtilityName = pd.UtilityName AND mp.UtilityId = pd.UtilityId AND mp.UtilityUOM = pd.UtilityUOM AND mp.Month = 9) AS sep,
        (SELECT mp.MonthPrice FROM MonthPrice mp WHERE mp.PlantId = pd.PlantId AND mp.UtilityName = pd.UtilityName AND mp.UtilityId = pd.UtilityId AND mp.UtilityUOM = pd.UtilityUOM AND mp.Month = 10) AS oct,
        (SELECT mp.MonthPrice FROM MonthPrice mp WHERE mp.PlantId = pd.PlantId AND mp.UtilityName = pd.UtilityName AND mp.UtilityId = pd.UtilityId AND mp.UtilityUOM = pd.UtilityUOM AND mp.Month = 11) AS nov,
        (SELECT mp.MonthPrice FROM MonthPrice mp WHERE mp.PlantId = pd.PlantId AND mp.UtilityName = pd.UtilityName AND mp.UtilityId = pd.UtilityId AND mp.UtilityUOM = pd.UtilityUOM AND mp.Month = 12) AS dec,
        (SELECT mp.MonthPrice FROM MonthPrice mp WHERE mp.PlantId = pd.PlantId AND mp.UtilityName = pd.UtilityName AND mp.UtilityId = pd.UtilityId AND mp.UtilityUOM = pd.UtilityUOM AND mp.Month = 1) AS jan,
        (SELECT mp.MonthPrice FROM MonthPrice mp WHERE mp.PlantId = pd.PlantId AND mp.UtilityName = pd.UtilityName AND mp.UtilityId = pd.UtilityId AND mp.UtilityUOM = pd.UtilityUOM AND mp.Month = 2) AS feb,
        (SELECT mp.MonthPrice FROM MonthPrice mp WHERE mp.PlantId = pd.PlantId AND mp.UtilityName = pd.UtilityName AND mp.UtilityId = pd.UtilityId AND mp.UtilityUOM = pd.UtilityUOM AND mp.Month = 3) AS mar,
        ap.WeightedAvgPrice AS weightedAvgPrice
    FROM PlantData pd
    LEFT JOIN AnnualPrice ap
        ON ap.PlantId = pd.PlantId
        AND ap.UtilityName = pd.UtilityName
        AND ap.UtilityId = pd.UtilityId
        AND ap.UtilityUOM = pd.UtilityUOM
    ORDER BY pd.SiteDescription, pd.UtilityPlant, pd.UtilityDisplayOrder, pd.UtilityName;
END
GO
