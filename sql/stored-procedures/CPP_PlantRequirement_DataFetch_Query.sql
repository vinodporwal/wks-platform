--select * from plants where name = 'CPP1'
USE [RIL.AOP]
GO

-- =============================================
-- Query to fetch all data used in Plant Requirement Calculation
-- This shows all intermediate data for debugging and validation
-- =============================================

DECLARE @FinancialYear VARCHAR(10) = '2025-26';  -- Change as needed
DECLARE @CPPId UNIQUEIDENTIFIER = '23BCA1B3-56DD-4C15-A3D6-3C2C9A62E653';


PRINT '======================================';
PRINT 'Financial Year: ' + @FinancialYear;
PRINT 'CPP Plant ID: ' + CAST(@CPPId AS VARCHAR(50));
PRINT '======================================';

-- =============================================
-- 1. ASSOCIATED PROCESS PLANTS
-- =============================================
PRINT '';
PRINT '1. ASSOCIATED PROCESS PLANTS';
PRINT '======================================';

SELECT 
    pcm.Plant_FK_Id,
    p.PlantCode,
    p.DisplayName AS PlantName,
    pcm.ProductName,
    ROW_NUMBER() OVER (
        PARTITION BY p.PlantCode 
        ORDER BY p.DisplayName ASC
    ) AS RowNum
FROM PowerConsumptionPlantMapping pcm
INNER JOIN Plants p ON p.Id = pcm.Plant_FK_Id
WHERE pcm.Consumption_FK_Id = @CPPId
  AND pcm.IsProcessPlant = 1
ORDER BY p.PlantCode, p.DisplayName;

-- =============================================
-- 2. PRODUCTION MATERIALS FROM NORMPARAMETERS (PLANTWISE)
-- =============================================
PRINT '';
PRINT '2. PRODUCTION MATERIALS FROM NORMPARAMETERS (PLANTWISE)';
PRINT '======================================';

;WITH ProcessPlants AS
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
SELECT 
    pp.PlantCode,
    pp.PlantName,
    np.Id AS Material_FK_Id,
    np.DisplayName AS MaterialName,
    np.SAPMaterialCode,
    np.UOM,
    np.NormType_FK_Id,
    nt.Name AS NormType,
    np.IsVisible
FROM ProcessPlants pp
INNER JOIN NormParameters np 
    ON np.Plant_FK_Id = pp.Plant_FK_Id
INNER JOIN NormParameterType nt
    ON nt.Id = np.NormType_FK_Id
WHERE pp.RowNum = 1
  AND CAST(np.IsVisible AS BIT) = 1
  AND nt.Name = 'Production'
ORDER BY pp.PlantCode, np.DisplayName;

-- =============================================
-- 3. PRODUCTION NORMS FROM AOP (PLANTWISE)
-- =============================================
PRINT '';
PRINT '3. PRODUCTION NORMS FROM AOP (PLANTWISE)';
PRINT '======================================';

;WITH ProcessPlants AS
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
),
ProductionMaterials AS
(
    SELECT DISTINCT
        pp.Plant_FK_Id,
        pp.PlantCode,
        pp.PlantName,
        np.Id AS Material_FK_Id,
        np.DisplayName AS MaterialName
    FROM ProcessPlants pp
    INNER JOIN NormParameters np 
        ON np.Plant_FK_Id = pp.Plant_FK_Id
    INNER JOIN NormParameterType nt
        ON nt.Id = np.NormType_FK_Id
    WHERE pp.RowNum = 1
      AND CAST(np.IsVisible AS BIT) = 1
      AND nt.Name = 'Production'
)
SELECT 
    pm.PlantCode,
    pm.PlantName,
    pm.MaterialName,
    A.AOPYear,
    A.April,
    A.May,
    A.June,
    A.July,
    A.Aug,
    A.Sep,
    A.Oct,
    A.Nov,
    A.Dec,
    A.Jan,
    A.Feb,
    A.March,
    (A.April + A.May + A.June + A.July + A.Aug + A.Sep + 
     A.Oct + A.Nov + A.Dec + A.Jan + A.Feb + A.March) AS Total_Annual_Production
FROM ProductionMaterials pm
LEFT JOIN AOP A 
    ON A.Plant_FK_Id = pm.Plant_FK_Id
    AND A.Material_FK_Id = pm.Material_FK_Id
    AND A.AOPYear = @FinancialYear
ORDER BY pm.PlantCode, pm.MaterialName;

-- =============================================
-- 4. AGGREGATED PRODUCTION NORMS (PLANTWISE SUMMARY)
-- =============================================
PRINT '';
PRINT '4. AGGREGATED PRODUCTION NORMS (PLANTWISE SUMMARY)';
PRINT '======================================';

;WITH ProcessPlants AS
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
),
ProductionMaterials AS
(
    SELECT DISTINCT
        pp.Plant_FK_Id,
        pp.PlantCode,
        pp.PlantName,
        np.Id AS Material_FK_Id,
        np.DisplayName AS MaterialName
    FROM ProcessPlants pp
    INNER JOIN NormParameters np 
        ON np.Plant_FK_Id = pp.Plant_FK_Id
    INNER JOIN NormParameterType nt
        ON nt.Id = np.NormType_FK_Id
    WHERE pp.RowNum = 1
      AND CAST(np.IsVisible AS BIT) = 1
      AND nt.Name = 'Production'
)
SELECT 
    pm.PlantCode,
    pm.PlantName,
    SUM(COALESCE(A.April, 0)) AS Total_April,
    SUM(COALESCE(A.May, 0)) AS Total_May,
    SUM(COALESCE(A.June, 0)) AS Total_June,
    SUM(COALESCE(A.July, 0)) AS Total_July,
    SUM(COALESCE(A.Aug, 0)) AS Total_Aug,
    SUM(COALESCE(A.Sep, 0)) AS Total_Sep,
    SUM(COALESCE(A.Oct, 0)) AS Total_Oct,
    SUM(COALESCE(A.Nov, 0)) AS Total_Nov,
    SUM(COALESCE(A.Dec, 0)) AS Total_Dec,
    SUM(COALESCE(A.Jan, 0)) AS Total_Jan,
    SUM(COALESCE(A.Feb, 0)) AS Total_Feb,
    SUM(COALESCE(A.March, 0)) AS Total_March,
    SUM(COALESCE(A.April, 0) + COALESCE(A.May, 0) + COALESCE(A.June, 0) + 
        COALESCE(A.July, 0) + COALESCE(A.Aug, 0) + COALESCE(A.Sep, 0) + 
        COALESCE(A.Oct, 0) + COALESCE(A.Nov, 0) + COALESCE(A.Dec, 0) + 
        COALESCE(A.Jan, 0) + COALESCE(A.Feb, 0) + COALESCE(A.March, 0)) AS Total_Annual
FROM ProductionMaterials pm
LEFT JOIN AOP A 
    ON A.Plant_FK_Id = pm.Plant_FK_Id
    AND A.Material_FK_Id = pm.Material_FK_Id
    AND A.AOPYear = @FinancialYear
GROUP BY pm.PlantCode, pm.PlantName
ORDER BY pm.PlantCode;

-- =============================================
-- 5. CONSUMPTION MATERIALS FROM NORMPARAMETERS (PLANTWISE)
-- =============================================
PRINT '';
PRINT '5. CONSUMPTION MATERIALS FROM NORMPARAMETERS (PLANTWISE)';
PRINT '======================================';

;WITH ProcessPlants AS
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
),
RequiredUtilities AS
(
    SELECT 'COMPRESSED AIR' AS UtilityName UNION ALL
    SELECT 'Cooling Water 2' UNION ALL
    SELECT 'Cooling Water 1' UNION ALL
    SELECT 'D M Water' UNION ALL
    SELECT 'HP Steam_Dis' UNION ALL
    SELECT 'LP Steam_Dis' UNION ALL
    SELECT 'MP Steam_Dis' UNION ALL
    SELECT 'SHP Steam_Dis' UNION ALL
    SELECT 'Nitrogen Gas' UNION ALL
    SELECT 'Oxygen' UNION ALL
    SELECT 'Power_Dis' UNION ALL
    SELECT 'Water'
),
RankedMaterials AS
(
    SELECT 
        pp.Plant_FK_Id,
        pp.PlantCode,
        pp.PlantName,
        np.Id AS Material_FK_Id,
        np.DisplayName AS UtilityName,
        np.SAPMaterialCode,
        np.UOM,
        np.NormType_FK_Id,
        nt.Name AS NormType,
        ROW_NUMBER() OVER (
            PARTITION BY pp.Plant_FK_Id, np.DisplayName
            ORDER BY 
                CASE WHEN np.SAPMaterialCode IS NOT NULL THEN 1 ELSE 2 END,
                np.Id
        ) AS RowNum
    FROM ProcessPlants pp
    INNER JOIN NormParameters np 
        ON np.Plant_FK_Id = pp.Plant_FK_Id
    INNER JOIN NormParameterType nt
        ON nt.Id = np.NormType_FK_Id
    INNER JOIN RequiredUtilities ru 
        ON np.DisplayName = ru.UtilityName
    WHERE pp.RowNum = 1
      AND CAST(np.IsVisible AS BIT) = 1
      AND nt.Name = 'Consumption'
)
SELECT 
    PlantCode,
    PlantName,
    Material_FK_Id,
    UtilityName,
    SAPMaterialCode,
    UOM,
    NormType,
    RowNum
FROM RankedMaterials
ORDER BY PlantCode, UtilityName, RowNum;

-- =============================================
-- 6. CONSUMPTION NORMS FROM AOPConsumptionNorm (PLANTWISE)
-- =============================================
PRINT '';
PRINT '6. CONSUMPTION NORMS FROM AOPConsumptionNorm (PLANTWISE)';
PRINT '======================================';

;WITH ProcessPlants AS
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
),
RequiredUtilities AS
(
    SELECT 'COMPRESSED AIR' AS UtilityName UNION ALL
    SELECT 'Cooling Water 2' UNION ALL
    SELECT 'Cooling Water 1' UNION ALL
    SELECT 'D M Water' UNION ALL
    SELECT 'HP Steam_Dis' UNION ALL
    SELECT 'LP Steam_Dis' UNION ALL
    SELECT 'MP Steam_Dis' UNION ALL
    SELECT 'SHP Steam_Dis' UNION ALL
    SELECT 'Nitrogen Gas' UNION ALL
    SELECT 'Oxygen' UNION ALL
    SELECT 'Power_Dis' UNION ALL
    SELECT 'Water'
),
ConsumptionMaterials AS
(
    SELECT 
        pp.Plant_FK_Id,
        pp.PlantCode,
        pp.PlantName,
        np.Id AS Material_FK_Id,
        np.DisplayName AS UtilityName,
        np.SAPMaterialCode,
        np.UOM,
        ROW_NUMBER() OVER (
            PARTITION BY pp.Plant_FK_Id, np.DisplayName
            ORDER BY 
                CASE WHEN np.SAPMaterialCode IS NOT NULL THEN 1 ELSE 2 END,
                np.Id
        ) AS RowNum
    FROM ProcessPlants pp
    INNER JOIN NormParameters np 
        ON np.Plant_FK_Id = pp.Plant_FK_Id
    INNER JOIN NormParameterType nt
        ON nt.Id = np.NormType_FK_Id
    INNER JOIN RequiredUtilities ru 
        ON np.DisplayName = ru.UtilityName
    WHERE pp.RowNum = 1
      AND CAST(np.IsVisible AS BIT) = 1
      AND nt.Name = 'Consumption'
)
SELECT 
    cm.PlantCode,
    cm.PlantName,
    cm.UtilityName,
    cm.SAPMaterialCode,
    'AOPConsumptionNorm' AS SourceTable,
    cn.AOPYear,
    cn.April,
    cn.May,
    cn.June,
    cn.July,
    cn.Aug,
    cn.Sep,
    cn.Oct,
    cn.Nov,
    cn.Dec,
    cn.Jan,
    cn.Feb,
    cn.March
FROM ConsumptionMaterials cm
INNER JOIN AOPConsumptionNorm cn 
    ON cn.Plant_FK_Id = cm.Plant_FK_Id
    AND cn.Material_FK_Id = cm.Material_FK_Id
    AND cn.AOPYear = @FinancialYear
WHERE cm.RowNum = 1
ORDER BY cm.PlantCode, cm.UtilityName;

-- =============================================
-- 7. CONSUMPTION NORMS FROM AOPConsumptionNormGrade (PLANTWISE)
-- =============================================
PRINT '';
PRINT '7. CONSUMPTION NORMS FROM AOPConsumptionNormGrade (PLANTWISE)';
PRINT '======================================';

;WITH ProcessPlants AS
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
),
RequiredUtilities AS
(
    SELECT 'COMPRESSED AIR' AS UtilityName UNION ALL
    SELECT 'Cooling Water 2' UNION ALL
    SELECT 'Cooling Water 1' UNION ALL
    SELECT 'D M Water' UNION ALL
    SELECT 'HP Steam_Dis' UNION ALL
    SELECT 'LP Steam_Dis' UNION ALL
    SELECT 'MP Steam_Dis' UNION ALL
    SELECT 'SHP Steam_Dis' UNION ALL
    SELECT 'Nitrogen Gas' UNION ALL
    SELECT 'Oxygen' UNION ALL
    SELECT 'Power_Dis' UNION ALL
    SELECT 'Water'
),
ConsumptionMaterials AS
(
    SELECT 
        pp.Plant_FK_Id,
        pp.PlantCode,
        pp.PlantName,
        np.Id AS Material_FK_Id,
        np.DisplayName AS UtilityName,
        np.SAPMaterialCode,
        np.UOM,
        ROW_NUMBER() OVER (
            PARTITION BY pp.Plant_FK_Id, np.DisplayName
            ORDER BY 
                CASE WHEN np.SAPMaterialCode IS NOT NULL THEN 1 ELSE 2 END,
                np.Id
        ) AS RowNum
    FROM ProcessPlants pp
    INNER JOIN NormParameters np 
        ON np.Plant_FK_Id = pp.Plant_FK_Id
    INNER JOIN NormParameterType nt
        ON nt.Id = np.NormType_FK_Id
    INNER JOIN RequiredUtilities ru 
        ON np.DisplayName = ru.UtilityName
    WHERE pp.RowNum = 1
      AND CAST(np.IsVisible AS BIT) = 1
      AND nt.Name = 'Consumption'
)
SELECT 
    cm.PlantCode,
    cm.PlantName,
    cm.UtilityName,
    cm.SAPMaterialCode,
    'AOPConsumptionNormGrade' AS SourceTable,
    cg.AOPYear,
    cg.April,
    cg.May,
    cg.June,
    cg.July,
    cg.Aug,
    cg.Sep,
    cg.Oct,
    cg.Nov,
    cg.Dec,
    cg.Jan,
    cg.Feb,
    cg.March
FROM ConsumptionMaterials cm
INNER JOIN AOPConsumptionNormGrade cg
    ON cg.Plant_FK_Id = cm.Plant_FK_Id
    AND cg.Material_FK_Id = cm.Material_FK_Id
    AND cg.AOPYear = @FinancialYear
WHERE cm.RowNum = 1
ORDER BY cm.PlantCode, cm.UtilityName;

-- =============================================
-- 8. FINAL CONSUMPTION NORMS (WITH PRIORITY LOGIC)
-- =============================================
PRINT '';
PRINT '8. FINAL CONSUMPTION NORMS (WITH PRIORITY LOGIC)';
PRINT '======================================';

;WITH ProcessPlants AS
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
),
RequiredUtilities AS
(
    SELECT 'COMPRESSED AIR' AS UtilityName UNION ALL
    SELECT 'Cooling Water 2' UNION ALL
    SELECT 'Cooling Water 1' UNION ALL
    SELECT 'D M Water' UNION ALL
    SELECT 'HP Steam_Dis' UNION ALL
    SELECT 'LP Steam_Dis' UNION ALL
    SELECT 'MP Steam_Dis' UNION ALL
    SELECT 'SHP Steam_Dis' UNION ALL
    SELECT 'Nitrogen Gas' UNION ALL
    SELECT 'Oxygen' UNION ALL
    SELECT 'Power_Dis' UNION ALL
    SELECT 'Water'
),
ConsumptionMaterials AS
(
    SELECT 
        pp.Plant_FK_Id,
        pp.PlantCode,
        pp.PlantName,
        np.Id AS Material_FK_Id,
        np.DisplayName AS UtilityName,
        np.SAPMaterialCode,
        np.UOM,
        ROW_NUMBER() OVER (
            PARTITION BY pp.Plant_FK_Id, np.DisplayName
            ORDER BY 
                CASE WHEN np.SAPMaterialCode IS NOT NULL THEN 1 ELSE 2 END,
                np.Id
        ) AS RowNum
    FROM ProcessPlants pp
    INNER JOIN NormParameters np 
        ON np.Plant_FK_Id = pp.Plant_FK_Id
    INNER JOIN NormParameterType nt
        ON nt.Id = np.NormType_FK_Id
    INNER JOIN RequiredUtilities ru 
        ON np.DisplayName = ru.UtilityName
    WHERE pp.RowNum = 1
      AND CAST(np.IsVisible AS BIT) = 1
      AND nt.Name = 'Consumption'
),
RankedNorms AS
(
    SELECT 
        cm.PlantCode,
        cm.PlantName,
        cm.UtilityName,
        cm.SAPMaterialCode,
        cm.UOM,
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
            PARTITION BY cm.Plant_FK_Id, cm.Material_FK_Id 
            ORDER BY 
                CASE WHEN cn.Id IS NOT NULL THEN 1 
                     WHEN cg.Id IS NOT NULL THEN 2 
                     ELSE 3 END
        ) AS PriorityRank
    FROM ConsumptionMaterials cm
    LEFT JOIN AOPConsumptionNorm cn 
        ON cn.Plant_FK_Id = cm.Plant_FK_Id
        AND cn.Material_FK_Id = cm.Material_FK_Id
        AND cn.AOPYear = @FinancialYear
    LEFT JOIN AOPConsumptionNormGrade cg
        ON cg.Plant_FK_Id = cm.Plant_FK_Id
        AND cg.Material_FK_Id = cm.Material_FK_Id
        AND cg.AOPYear = @FinancialYear
    WHERE cm.RowNum = 1
)
SELECT 
    PlantCode,
    PlantName,
    UtilityName,
    SAPMaterialCode,
    UOM,
    SourceTable,
    April, May, June, July, Aug, Sep, Oct, Nov, Dec, Jan, Feb, March
FROM RankedNorms
WHERE PriorityRank = 1
ORDER BY PlantCode, UtilityName;

PRINT '';
PRINT '======================================';
PRINT 'Data fetch completed!';
PRINT '======================================';
