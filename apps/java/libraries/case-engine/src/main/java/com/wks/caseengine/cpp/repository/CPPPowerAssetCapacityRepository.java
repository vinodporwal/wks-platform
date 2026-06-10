package com.wks.caseengine.cpp.repository;

import com.wks.caseengine.cpp.dto.CPPAssetCapacityProjection;
import com.wks.caseengine.cpp.entity.CPPPowerAssetCapacity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CPPPowerAssetCapacityRepository extends JpaRepository<CPPPowerAssetCapacity, UUID> {

    @Query(value = """
        WITH Combined AS (
            SELECT
                c.Id AS id,
                c.Asset_FK_Id AS assetFkId,
                c.Fixed_Min AS fixedMin,
                c.Fixed_Max AS fixedMax,
                c.Apr_Min AS aprMin,
                c.Apr_Max AS aprMax,
                c.May_Min AS mayMin,
                c.May_Max AS mayMax,
                c.Jun_Min AS junMin,
                c.Jun_Max AS junMax,
                c.Jul_Min AS julMin,
                c.Jul_Max AS julMax,
                c.Aug_Min AS augMin,
                c.Aug_Max AS augMax,
                c.Sep_Min AS sepMin,
                c.Sep_Max AS sepMax,
                c.Oct_Min AS octMin,
                c.Oct_Max AS octMax,
                c.Nov_Min AS novMin,
                c.Nov_Max AS novMax,
                c.Dec_Min AS decMin,
                c.Dec_Max AS decMax,
                c.Jan_Min AS janMin,
                c.Jan_Max AS janMax,
                c.Feb_Min AS febMin,
                c.Feb_Max AS febMax,
                c.Mar_Min AS marMin,
                c.Mar_Max AS marMax,
                CAST(c.AOPYear AS VARCHAR(10)) AS aopYear,
                c.Remarks AS remarks,
                c.UOM AS uom,
                CONVERT(VARCHAR, c.CreatedDate, 120) AS createdDate,
                CONVERT(VARCHAR, c.UpdatedDate, 120) AS modifiedDate,
                a.AssetName AS assetName,
                a.AssetType AS assetType,
                pl.DisplayName AS plantName,
                pl.Name AS plantCode,
                'Power' AS assetCategory,
                pl.Site_FK_Id AS siteFkId,
                pl.Vertical_FK_Id AS verticalFkId,
                pl.Id AS plantFkId,
                0 AS categoryOrder,
                a.DisplayName AS sortDisplayName,
                npd.Name AS utilityDistributed,
                npd.SAPMaterialCode AS distributedSapCode,
                npg.Name AS utilityGenerated,
                npg.SAPMaterialCode AS generatedUtilityCode,
                CASE
                    WHEN CHARINDEX('-', a.DisplayName) > 0
                    THEN LEFT(a.DisplayName, CHARINDEX('-', a.DisplayName) - 1)
                    ELSE a.DisplayName
                END AS namePrefix,
                CASE
                    WHEN CHARINDEX('-', a.DisplayName) > 0
                    THEN TRY_CAST(RIGHT(a.DisplayName, LEN(a.DisplayName) - CHARINDEX('-', a.DisplayName)) AS INT)
                    ELSE 0
                END AS nameNumber
            FROM [RIL.AOP].[dbo].[CPPPowerAssetCapacity] c WITH(NOLOCK)
            LEFT JOIN PowerGenerationAssets a WITH(NOLOCK) ON c.Asset_FK_Id = a.AssetId
            LEFT JOIN Plants pl WITH(NOLOCK) ON a.CPPPLANT_FK_Id = pl.Id
            LEFT JOIN NormParameters npg WITH(NOLOCK) ON npg.Id = a.UtilityGeneration_FK_Id
            LEFT JOIN NormParameters npd WITH(NOLOCK) ON npd.Id = a.UtilityDistributed_FK_Id
            WHERE a.CPPPLANT_FK_Id IN :plantIds
              AND c.AOPYear = :aopYear

            UNION ALL

            SELECT
                s.Id AS id,
                s.Asset_FK_Id AS assetFkId,
                s.Fixed_Min AS fixedMin,
                s.Fixed_Max AS fixedMax,
                s.Apr_Min AS aprMin,
                s.Apr_Max AS aprMax,
                s.May_Min AS mayMin,
                s.May_Max AS mayMax,
                s.Jun_Min AS junMin,
                s.Jun_Max AS junMax,
                s.Jul_Min AS julMin,
                s.Jul_Max AS julMax,
                s.Aug_Min AS augMin,
                s.Aug_Max AS augMax,
                s.Sep_Min AS sepMin,
                s.Sep_Max AS sepMax,
                s.Oct_Min AS octMin,
                s.Oct_Max AS octMax,
                s.Nov_Min AS novMin,
                s.Nov_Max AS novMax,
                s.Dec_Min AS decMin,
                s.Dec_Max AS decMax,
                s.Jan_Min AS janMin,
                s.Jan_Max AS janMax,
                s.Feb_Min AS febMin,
                s.Feb_Max AS febMax,
                s.Mar_Min AS marMin,
                s.Mar_Max AS marMax,
                CAST(s.AOPYear AS VARCHAR(20)) AS aopYear,
                s.Remarks AS remarks,
                s.UOM AS uom,
                CONVERT(VARCHAR, s.CreatedDate, 120) AS createdDate,
                CONVERT(VARCHAR, s.UpdatedDate, 120) AS modifiedDate,
                sa.AssetName AS assetName,
                sa.AssetType AS assetType,
                pl2.DisplayName AS plantName,
                pl2.Name AS plantCode,
                'Steam' AS assetCategory,
                pl2.Site_FK_Id AS siteFkId,
                pl2.Vertical_FK_Id AS verticalFkId,
                pl2.Id AS plantFkId,
                1 AS categoryOrder,
                sa.DisplayName AS sortDisplayName,
                npd2.Name AS utilityDistributed,
                npd2.SAPMaterialCode AS distributedSapCode,
                npg2.Name AS utilityGenerated,
                npg2.SAPMaterialCode AS generatedUtilityCode,
                CASE
                    WHEN CHARINDEX('-', sa.DisplayName) > 0
                    THEN LEFT(sa.DisplayName, CHARINDEX('-', sa.DisplayName) - 1)
                    ELSE sa.DisplayName
                END AS namePrefix,
                CASE
                    WHEN CHARINDEX('-', sa.DisplayName) > 0
                    THEN TRY_CAST(RIGHT(sa.DisplayName, LEN(sa.DisplayName) - CHARINDEX('-', sa.DisplayName)) AS INT)
                    ELSE 0
                END AS nameNumber
            FROM [RIL.AOP].[dbo].[CPPSteamAssetCapacity] s WITH(NOLOCK)
            LEFT JOIN [RIL.AOP].[dbo].[CPPSteamGenerationAsset] sa WITH(NOLOCK) ON s.Asset_FK_Id = sa.AssetId
            LEFT JOIN Plants pl2 WITH(NOLOCK) ON sa.CPPPLANT_FK_Id = pl2.Id
            LEFT JOIN NormParameters npg2 WITH(NOLOCK) ON npg2.Id = sa.UtilityGeneration_FK_Id
            LEFT JOIN NormParameters npd2 WITH(NOLOCK) ON npd2.Id = sa.UtilityDistributed_FK_Id
            WHERE sa.CPPPLANT_FK_Id IN :plantIds
              AND s.AOPYear = :aopYear
        )
        SELECT
            id, assetFkId, plantFkId, fixedMin, fixedMax,
            aprMin, aprMax, mayMin, mayMax, junMin, junMax, julMin, julMax,
            augMin, augMax, sepMin, sepMax, octMin, octMax, novMin, novMax,
            decMin, decMax, janMin, janMax, febMin, febMax, marMin, marMax,
            aopYear, remarks, uom, createdDate, modifiedDate,
            assetName, assetType, plantName, plantCode, assetCategory,
            utilityDistributed, distributedSapCode, utilityGenerated, generatedUtilityCode,
            siteFkId, verticalFkId
        FROM Combined
        ORDER BY categoryOrder, plantName, namePrefix, nameNumber, sortDisplayName
        """, nativeQuery = true)
    List<CPPAssetCapacityProjection> findAssetCapacitiesByPlants(
            @Param("plantIds") List<UUID> plantIds,
            @Param("aopYear") String aopYear);
}
