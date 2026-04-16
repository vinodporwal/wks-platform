USE [RIL.AOP]
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

-- =============================================
-- Author:      CPP Team
-- Create date: 2026-03-09
-- Description: Calculate Plant Requirement Data - Step 1
-- Version:     1.0 - Initial version with process plant and material fetching
-- =============================================
ALTER PROCEDURE [dbo].[CPP_NMD_CalculatePlantRequirement_v1]
(
    @NMDCPP_Id UNIQUEIDENTIFIER,
    @AOPYear VARCHAR(10)  -- Format: '2025-26'
)
AS
BEGIN
    SET NOCOUNT ON;

    -- =============================================
    -- Step 1: Fetch Associated Process Plants with Production Material Mapping
    -- =============================================
    -- Get all process plants mapped to the given NMDCPP plant
    -- where IsProcessPlant = 1 and get the ProductionMaterialNorm_FK_Id
    
    DECLARE @ProcessPlants TABLE
    (
        ProcessPlant_FK_Id UNIQUEIDENTIFIER,
        PlantName NVARCHAR(200),
        PlantCode NVARCHAR(100),
        ProductionMaterialNorm_FK_Id UNIQUEIDENTIFIER
    );

    INSERT INTO @ProcessPlants (ProcessPlant_FK_Id, PlantName, PlantCode, ProductionMaterialNorm_FK_Id)
    SELECT DISTINCT
        pcm.Plant_FK_Id,
        p.DisplayName AS PlantName,
        p.PlantCode,
        pcm.ProductionMaterialNorm_FK_Id
    FROM PowerConsumptionPlantMapping pcm
    INNER JOIN Plants p ON p.Id = pcm.Plant_FK_Id
    WHERE pcm.Consumption_FK_Id = @NMDCPP_Id
      AND pcm.IsProcessPlant = 1;  -- Only process plants

    -- Debug: Show process plants found with production material mapping
    SELECT 
        'Process Plants with Production Material Mapping' AS DataType,
        ProcessPlant_FK_Id,
        PlantName,
        PlantCode,
        ProductionMaterialNorm_FK_Id
    FROM @ProcessPlants;

    -- =============================================
    -- Step 2: Fetch Production Materials using ProductionMaterialNorm_FK_Id
    -- =============================================
    -- Get specific production material from the mapping table
    
    DECLARE @ProductionMaterials TABLE
    (
        ProcessPlant_FK_Id UNIQUEIDENTIFIER,
        PlantName NVARCHAR(200),
        ProductionMaterial_FK_Id UNIQUEIDENTIFIER,
        MaterialName NVARCHAR(200),
        SAPMaterialCode NVARCHAR(100),
        UOM NVARCHAR(50)
    );

    INSERT INTO @ProductionMaterials 
    (
        ProcessPlant_FK_Id,
        PlantName,
        ProductionMaterial_FK_Id,
        MaterialName,
        SAPMaterialCode,
        UOM
    )
    SELECT 
        pp.ProcessPlant_FK_Id,
        pp.PlantName,
        np.Id AS ProductionMaterial_FK_Id,
        np.DisplayName AS MaterialName,
        np.SAPMaterialCode,
        np.UOM
    FROM @ProcessPlants pp
    INNER JOIN NormParameters np 
        ON (
            -- If ProductionMaterialNorm_FK_Id is specified, use that specific material
            (pp.ProductionMaterialNorm_FK_Id IS NOT NULL AND np.Id = pp.ProductionMaterialNorm_FK_Id)
            OR
            -- If ProductionMaterialNorm_FK_Id is NULL, get all production materials for this plant
            (pp.ProductionMaterialNorm_FK_Id IS NULL AND np.Plant_FK_Id = pp.ProcessPlant_FK_Id AND np.NormType_FK_Id = 1)
        );

    -- =============================================
    -- Step 3: Skip - Don't fetch raw materials from NormParameters
    -- We'll get consumption utilities directly from AOPConsumptionNorm
    -- =============================================

    -- =============================================
    -- Step 4: Fetch Production Quantities
    -- =============================================
    -- Get actual production quantities from AOP table for each production material
    
    DECLARE @ProductionQuantities TABLE
    (
        ProcessPlant_FK_Id UNIQUEIDENTIFIER,
        PlantName NVARCHAR(200),
        ProductionMaterial_FK_Id UNIQUEIDENTIFIER,
        MaterialName NVARCHAR(200),
        SAPMaterialCode NVARCHAR(100),
        UOM NVARCHAR(50),
        Apr DECIMAL(18,6),
        May DECIMAL(18,6),
        Jun DECIMAL(18,6),
        Jul DECIMAL(18,6),
        Aug DECIMAL(18,6),
        Sep DECIMAL(18,6),
        Oct DECIMAL(18,6),
        Nov DECIMAL(18,6),
        Dec DECIMAL(18,6),
        Jan DECIMAL(18,6),
        Feb DECIMAL(18,6),
        Mar DECIMAL(18,6)
    );

    INSERT INTO @ProductionQuantities
    SELECT 
        pm.ProcessPlant_FK_Id,
        pm.PlantName,
        pm.ProductionMaterial_FK_Id,
        pm.MaterialName,
        pm.SAPMaterialCode,
        pm.UOM,
        ISNULL(aop.April, 0) AS Apr,
        ISNULL(aop.May, 0) AS May,
        ISNULL(aop.June, 0) AS Jun,
        ISNULL(aop.July, 0) AS Jul,
        ISNULL(aop.Aug, 0) AS Aug,
        ISNULL(aop.Sep, 0) AS Sep,
        ISNULL(aop.Oct, 0) AS Oct,
        ISNULL(aop.Nov, 0) AS Nov,
        ISNULL(aop.Dec, 0) AS Dec,
        ISNULL(aop.Jan, 0) AS Jan,
        ISNULL(aop.Feb, 0) AS Feb,
        ISNULL(aop.March, 0) AS Mar
    FROM @ProductionMaterials pm
    LEFT JOIN AOP aop
        ON aop.Material_FK_Id = pm.ProductionMaterial_FK_Id
       AND aop.Plant_FK_Id = pm.ProcessPlant_FK_Id
       AND aop.AOPYear = @AOPYear;

    -- Combined: Production Materials with Quantities
    SELECT 
        'Production Materials with Quantities' AS DataType,
        ProcessPlant_FK_Id,
        PlantName,
        ProductionMaterial_FK_Id,
        MaterialName,
        SAPMaterialCode,
        UOM,
        Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec, Jan, Feb, Mar,
        (Apr + May + Jun + Jul + Aug + Sep + Oct + Nov + Dec + Jan + Feb + Mar) AS Total_Annual_Production
    FROM @ProductionQuantities;

    -- =============================================
    -- Step 5: Fetch Consumption Norms from AOPConsumptionNorm
    -- =============================================
    -- Get consumption norms directly from AOPConsumptionNorm for process plants
    -- Then lookup material details from NormParameters using Material_FK_Id
    
    DECLARE @ConsumptionNorms TABLE
    (
        Material_FK_Id UNIQUEIDENTIFIER,
        MaterialName NVARCHAR(200),
        SAPMaterialCode NVARCHAR(100),
        UOM NVARCHAR(50),
        ProcessPlant_FK_Id UNIQUEIDENTIFIER,
        ProcessPlantName NVARCHAR(200),
        NormSource NVARCHAR(50),  -- 'Regular' or 'Grade'
        Apr DECIMAL(18,6),
        May DECIMAL(18,6),
        Jun DECIMAL(18,6),
        Jul DECIMAL(18,6),
        Aug DECIMAL(18,6),
        Sep DECIMAL(18,6),
        Oct DECIMAL(18,6),
        Nov DECIMAL(18,6),
        Dec DECIMAL(18,6),
        Jan DECIMAL(18,6),
        Feb DECIMAL(18,6),
        Mar DECIMAL(18,6)
    );

    -- Define the specific utilities we want to track
    DECLARE @RequiredUtilities TABLE (UtilityName NVARCHAR(200));
    INSERT INTO @RequiredUtilities VALUES 
        ('COMPRESSED AIR'),
        ('Cooling Water 2'),
        ('D M Water'),
        ('HP Steam_Dis'),
        ('LP Steam_Dis'),
        ('MP Steam_Dis'),
        ('Nitrogen Gas'),
        ('Oxygen'),
        ('Power_Dis'),
        ('Water');

    -- Get consumption norms from AOPConsumptionNorm for all process plants
    -- Then lookup material details from NormParameters
    -- Filter to only required utilities
    INSERT INTO @ConsumptionNorms
    SELECT 
        np.Id AS Material_FK_Id,
        np.DisplayName AS MaterialName,
        np.SAPMaterialCode,
        np.UOM,
        pp.ProcessPlant_FK_Id,
        pp.PlantName AS ProcessPlantName,
        CASE 
            WHEN cn.Id IS NOT NULL THEN 'Regular'
            WHEN cg.Id IS NOT NULL THEN 'Grade'
            ELSE 'None'
        END AS NormSource,
        COALESCE(cn.April, cg.April, 0) AS Apr,
        COALESCE(cn.May, cg.May, 0) AS May,
        COALESCE(cn.June, cg.June, 0) AS Jun,
        COALESCE(cn.July, cg.July, 0) AS Jul,
        COALESCE(cn.Aug, cg.Aug, 0) AS Aug,
        COALESCE(cn.Sep, cg.Sep, 0) AS Sep,
        COALESCE(cn.Oct, cg.Oct, 0) AS Oct,
        COALESCE(cn.Nov, cg.Nov, 0) AS Nov,
        COALESCE(cn.Dec, cg.Dec, 0) AS Dec,
        COALESCE(cn.Jan, cg.Jan, 0) AS Jan,
        COALESCE(cn.Feb, cg.Feb, 0) AS Feb,
        COALESCE(cn.March, cg.March, 0) AS Mar
    FROM @ProcessPlants pp
    -- Get all consumption norms for this process plant
    LEFT JOIN AOPConsumptionNorm cn
        ON cn.Plant_FK_Id = pp.ProcessPlant_FK_Id
       AND cn.AOPYear = @AOPYear
    LEFT JOIN AOPConsumptionNormGrade cg
        ON cg.Plant_FK_Id = pp.ProcessPlant_FK_Id
       AND cg.AOPYear = @AOPYear
       AND cg.Material_FK_Id = cn.Material_FK_Id
       AND cn.Id IS NULL  -- Only use grade if regular doesn't exist
    -- Lookup material details from NormParameters using Material_FK_Id
    INNER JOIN NormParameters np
        ON np.Id = COALESCE(cn.Material_FK_Id, cg.Material_FK_Id)
    -- Filter to only required utilities
    INNER JOIN @RequiredUtilities ru
        ON np.DisplayName = ru.UtilityName
    WHERE cn.Id IS NOT NULL OR cg.Id IS NOT NULL;

    -- Consumption Utilities with Norms (from AOPConsumptionNorm)
    SELECT 
        'Consumption Utilities with Norms' AS DataType,
        Material_FK_Id,
        MaterialName,
        SAPMaterialCode,
        UOM,
        ProcessPlant_FK_Id,
        ProcessPlantName,
        NormSource,
        Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec, Jan, Feb, Mar,
        (Apr + May + Jun + Jul + Aug + Sep + Oct + Nov + Dec + Jan + Feb + Mar) AS Total_Annual_Norm
    FROM @ConsumptionNorms;

    -- =============================================
    -- Step 6: Calculate Plant Requirement (Consumption)
    -- =============================================
    -- Formula: Consumption = Production Quantity × Consumption Norm
    -- This gives actual utility consumption based on production
    
    SELECT 
        'Calculated Plant Requirement' AS DataType,
        pq.ProcessPlant_FK_Id,
        pq.PlantName AS ProcessPlantName,
        cn.Material_FK_Id AS Utility_FK_Id,
        cn.MaterialName AS UtilityName,
        cn.SAPMaterialCode AS UtilityCode,
        cn.UOM AS UtilityUOM,
        cn.NormSource,
        
        -- Monthly Calculations: SUM(Production × Norm) for all production materials
        -- This handles both single material and multiple materials (when ProductionMaterialNorm_FK_Id is NULL)
        SUM(pq.Apr * cn.Apr) AS Apr_Consumption,
        SUM(pq.May * cn.May) AS May_Consumption,
        SUM(pq.Jun * cn.Jun) AS Jun_Consumption,
        SUM(pq.Jul * cn.Jul) AS Jul_Consumption,
        SUM(pq.Aug * cn.Aug) AS Aug_Consumption,
        SUM(pq.Sep * cn.Sep) AS Sep_Consumption,
        SUM(pq.Oct * cn.Oct) AS Oct_Consumption,
        SUM(pq.Nov * cn.Nov) AS Nov_Consumption,
        SUM(pq.Dec * cn.Dec) AS Dec_Consumption,
        SUM(pq.Jan * cn.Jan) AS Jan_Consumption,
        SUM(pq.Feb * cn.Feb) AS Feb_Consumption,
        SUM(pq.Mar * cn.Mar) AS Mar_Consumption,
        
        -- Total for the year
        SUM(
            pq.Apr * cn.Apr + pq.May * cn.May + pq.Jun * cn.Jun +
            pq.Jul * cn.Jul + pq.Aug * cn.Aug + pq.Sep * cn.Sep +
            pq.Oct * cn.Oct + pq.Nov * cn.Nov + pq.Dec * cn.Dec +
            pq.Jan * cn.Jan + pq.Feb * cn.Feb + pq.Mar * cn.Mar
        ) AS Total_Annual_Consumption,
        
        -- Show which production materials contributed to this consumption
        STRING_AGG(pq.MaterialName, ', ') AS ProductionMaterials_Used
        
    FROM @ProductionQuantities pq
    INNER JOIN @ConsumptionNorms cn
        ON cn.ProcessPlant_FK_Id = pq.ProcessPlant_FK_Id  -- Match by process plant
    GROUP BY
        pq.ProcessPlant_FK_Id,
        pq.PlantName,
        cn.Material_FK_Id,
        cn.MaterialName,
        cn.SAPMaterialCode,
        cn.UOM,
        cn.NormSource
    ORDER BY 
        pq.PlantName,
        cn.MaterialName;

    -- =============================================
    -- Step 7: Summary Output
    -- =============================================
    SELECT 
        @NMDCPP_Id AS NMDCPP_Id,
        @AOPYear AS AOPYear,
        (SELECT COUNT(*) FROM @ProcessPlants) AS ProcessPlantCount,
        (SELECT COUNT(*) FROM @ProductionMaterials) AS ProductionMaterialCount,
        (SELECT COUNT(*) FROM @ProductionQuantities WHERE Apr > 0 OR May > 0 OR Jun > 0) AS ProductionRecordsFound,
        (SELECT COUNT(*) FROM @ConsumptionNorms WHERE NormSource != 'None') AS ConsumptionUtilitiesFound;

END;
GO
