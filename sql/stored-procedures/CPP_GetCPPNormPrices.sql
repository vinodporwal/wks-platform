USE [RIL.AOP]
GO

/****** Object:  StoredProcedure [dbo].[CPP_GetCPPNormPrices] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE OR ALTER PROCEDURE [dbo].[CPP_GetCPPNormPrices]
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
        ROW_NUMBER() OVER (ORDER BY fd.GeneratingPlantName, fd.AccountName, fd.DisplayOrder, fd.NormParameterDisplayOrder) AS id,
        cmp.Id AS cppMonthWisePriceId,
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
        cmp.AOPYear AS aopYear,
        ISNULL(cmp.Apr_Price, 0) AS aprPrice,
        ISNULL(cmp.May_Price, 0) AS mayPrice,
        ISNULL(cmp.Jun_Price, 0) AS junPrice,
        ISNULL(cmp.Jul_Price, 0) AS julPrice,
        ISNULL(cmp.Aug_Price, 0) AS augPrice,
        ISNULL(cmp.Sep_Price, 0) AS sepPrice,
        ISNULL(cmp.Oct_Price, 0) AS octPrice,
        ISNULL(cmp.Nov_Price, 0) AS novPrice,
        ISNULL(cmp.Dec_Price, 0) AS decPrice,
        ISNULL(cmp.Jan_Price, 0) AS janPrice,
        ISNULL(cmp.Feb_Price, 0) AS febPrice,
        ISNULL(cmp.Mar_Price, 0) AS marPrice,
        cmp.Remarks AS remarks,
        cmp.PriceSource AS priceSource,
        cmp.ModifiedBy AS modifiedBy,
        cmp.CreatedDate AS createdDate,
        cmp.UpdatedDate AS updatedDate
    FROM FinalData fd
    LEFT JOIN CPPMonthWisePrice cmp 
        ON cmp.NormsHeader_FK_Id = fd.NormHeaderId 
        AND cmp.FinancialYear = @FinancialYear
    ORDER BY fd.GeneratingPlantName, fd.UtilityName, fd.DisplayOrder, fd.NormParameterDisplayOrder;

END
GO
