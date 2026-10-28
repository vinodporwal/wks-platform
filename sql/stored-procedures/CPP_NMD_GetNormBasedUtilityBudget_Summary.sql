USE [RIL.AOP]
GO
/****** Object:  StoredProcedure [dbo].[CPP_NMD_GetNormBasedUtilityBudget_Summary] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE OR ALTER PROCEDURE [dbo].[CPP_NMD_GetNormBasedUtilityBudget_Summary]
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
            nh.MaterialId,
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
            nmd.Norms,
            nmd.Price,
            nmd.GenerationUOM,
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
    ),

    -------------------------------------------------------------------------
    -- QUARTER AGGREGATION
    -------------------------------------------------------------------------
    QuarterAgg AS
    (
        SELECT
            md.NormsHeader_FK_Id,
            CASE
                WHEN md.Month BETWEEN 4 AND 6 THEN 'Q1'
                WHEN md.Month BETWEEN 7 AND 9 THEN 'Q2'
                WHEN md.Month BETWEEN 10 AND 12 THEN 'Q3'
                WHEN md.Month BETWEEN 1 AND 3 THEN 'Q4'
            END AS QuarterKey,
            SUM(CAST(COALESCE(md.QTY, 0) AS DECIMAL(18, 6))) AS SumQty,
            SUM(CAST(COALESCE(md.Norms, 0) AS DECIMAL(18, 6)) * CAST(COALESCE(md.QTY, 0) AS DECIMAL(18, 6))) AS SumNormsQty,
            SUM(CAST(COALESCE(md.Price, 0) AS DECIMAL(18, 6)) * CAST(COALESCE(md.QTY, 0) AS DECIMAL(18, 6))) AS SumPriceQty
        FROM MonthDetails md
        GROUP BY
            md.NormsHeader_FK_Id,
            CASE
                WHEN md.Month BETWEEN 4 AND 6 THEN 'Q1'
                WHEN md.Month BETWEEN 7 AND 9 THEN 'Q2'
                WHEN md.Month BETWEEN 10 AND 12 THEN 'Q3'
                WHEN md.Month BETWEEN 1 AND 3 THEN 'Q4'
            END
    ),

    QuarterCalcs AS
    (
        SELECT
            qa.NormsHeader_FK_Id,
            qa.QuarterKey,
            qa.SumQty,
            CASE WHEN qa.SumQty IS NULL OR qa.SumQty = 0 THEN NULL ELSE qa.SumNormsQty / qa.SumQty END AS WeightedNorms,
            CASE WHEN qa.SumQty IS NULL OR qa.SumQty = 0 THEN NULL ELSE qa.SumPriceQty / qa.SumQty END AS WeightedPrice,
            CASE WHEN qa.SumQty IS NULL OR qa.SumQty = 0 THEN NULL ELSE qa.SumNormsQty END AS Quantity,
            CASE WHEN qa.SumQty IS NULL OR qa.SumQty = 0 THEN NULL ELSE qa.SumNormsQty * (qa.SumPriceQty / qa.SumQty) END AS Amount
        FROM QuarterAgg qa
    ),

    -------------------------------------------------------------------------
    -- ANNUAL AGGREGATION
    -------------------------------------------------------------------------
    AnnualAgg AS
    (
        SELECT
            md.NormsHeader_FK_Id,
            SUM(CAST(COALESCE(md.QTY, 0) AS DECIMAL(18, 6))) AS SumQty,
            SUM(CAST(COALESCE(md.Norms, 0) AS DECIMAL(18, 6)) * CAST(COALESCE(md.QTY, 0) AS DECIMAL(18, 6))) AS SumNormsQty,
            SUM(CAST(COALESCE(md.Price, 0) AS DECIMAL(18, 6)) * CAST(COALESCE(md.QTY, 0) AS DECIMAL(18, 6))) AS SumPriceQty
        FROM MonthDetails md
        GROUP BY md.NormsHeader_FK_Id
    ),

    AnnualCalcs AS
    (
        SELECT
            aa.NormsHeader_FK_Id,
            aa.SumQty,
            CASE WHEN aa.SumQty IS NULL OR aa.SumQty = 0 THEN NULL ELSE aa.SumNormsQty / aa.SumQty END AS WeightedNorms,
            CASE WHEN aa.SumQty IS NULL OR aa.SumQty = 0 THEN NULL ELSE aa.SumPriceQty / aa.SumQty END AS WeightedPrice,
            CASE WHEN aa.SumQty IS NULL OR aa.SumQty = 0 THEN NULL ELSE aa.SumNormsQty END AS Quantity,
            CASE WHEN aa.SumQty IS NULL OR aa.SumQty = 0 THEN NULL ELSE aa.SumNormsQty * (aa.SumPriceQty / aa.SumQty) END AS Amount
        FROM AnnualAgg aa
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
        fd.MaterialId         AS materialId,
        fd.IssuingPlantName   AS issuingPlantName,
        fd.IssuingUOM         AS issuingUom,
        COALESCE(guom.CommonGenerationUOM, '') AS generationUom,

        -- Q1
        ISNULL((SELECT 
                qc.WeightedNorms AS norms,
                qc.Quantity AS quantity,
                qc.Amount AS amount,
                qc.WeightedPrice AS price,
                qc.SumQty AS QTY,
                COALESCE(guom.CommonGenerationUOM, '') AS generationUom
                FROM QuarterCalcs qc
                WHERE qc.NormsHeader_FK_Id = fd.NormHeaderId AND qc.QuarterKey = 'Q1'
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"QTY":null,"generationUom":null}') AS q1,

        -- Q2
        ISNULL((SELECT 
                qc.WeightedNorms AS norms,
                qc.Quantity AS quantity,
                qc.Amount AS amount,
                qc.WeightedPrice AS price,
                qc.SumQty AS QTY,
                COALESCE(guom.CommonGenerationUOM, '') AS generationUom
                FROM QuarterCalcs qc
                WHERE qc.NormsHeader_FK_Id = fd.NormHeaderId AND qc.QuarterKey = 'Q2'
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"QTY":null,"generationUom":null}') AS q2,

        -- Q3
        ISNULL((SELECT 
                qc.WeightedNorms AS norms,
                qc.Quantity AS quantity,
                qc.Amount AS amount,
                qc.WeightedPrice AS price,
                qc.SumQty AS QTY,
                COALESCE(guom.CommonGenerationUOM, '') AS generationUom
                FROM QuarterCalcs qc
                WHERE qc.NormsHeader_FK_Id = fd.NormHeaderId AND qc.QuarterKey = 'Q3'
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"QTY":null,"generationUom":null}') AS q3,

        -- Q4
        ISNULL((SELECT 
                qc.WeightedNorms AS norms,
                qc.Quantity AS quantity,
                qc.Amount AS amount,
                qc.WeightedPrice AS price,
                qc.SumQty AS QTY,
                COALESCE(guom.CommonGenerationUOM, '') AS generationUom
                FROM QuarterCalcs qc
                WHERE qc.NormsHeader_FK_Id = fd.NormHeaderId AND qc.QuarterKey = 'Q4'
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"QTY":null,"generationUom":null}') AS q4,

        -- Annual
        ISNULL((SELECT 
                ac.WeightedNorms AS norms,
                ac.Quantity AS quantity,
                ac.Amount AS amount,
                ac.WeightedPrice AS price,
                ac.SumQty AS QTY,
                COALESCE(guom.CommonGenerationUOM, '') AS generationUom
                FROM AnnualCalcs ac
                WHERE ac.NormsHeader_FK_Id = fd.NormHeaderId
                FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES),
               '{"norms":null,"quantity":null,"amount":null,"price":null,"QTY":null,"generationUom":null}') AS annual

    FROM FinalData fd
    LEFT JOIN GenerationUOMData guom
        ON fd.NormHeaderId = guom.NormHeaderId
    ORDER BY fd.GeneratingPlantName, fd.DisplayOrder, fd.NormParameterDisplayOrder;

END
GO
