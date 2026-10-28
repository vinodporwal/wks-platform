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
                CAST(c.Apr_Man_Load AS INT) AS aprManLoad, 
                c.May_Min AS mayMin,
                c.May_Max AS mayMax,
                CAST(c.May_Man_Load AS INT) AS mayManLoad,
                c.Jun_Min AS junMin,
                c.Jun_Max AS junMax,
                CAST(c.Jun_Man_Load AS INT) AS junManLoad,
                c.Jul_Min AS julMin,
                c.Jul_Max AS julMax,
                CAST(c.Jul_Man_Load AS INT) AS julManLoad,
                c.Aug_Min AS augMin,
                c.Aug_Max AS augMax,
                CAST(c.Aug_Man_Load AS INT) AS augManLoad,
                c.Sep_Min AS sepMin,
                c.Sep_Max AS sepMax,
                CAST(c.Sep_Man_Load AS INT) AS sepManLoad,
                c.Oct_Min AS octMin,
                c.Oct_Max AS octMax,
                CAST(c.Oct_Man_Load AS INT) AS octManLoad,
                c.Nov_Min AS novMin,
                c.Nov_Max AS novMax,
                CAST(c.Nov_Man_Load AS INT) AS novManLoad,
                c.Dec_Min AS decMin,
                c.Dec_Max AS decMax,
                CAST(c.Dec_Man_Load AS INT) AS decManLoad,
                c.Jan_Min AS janMin,
                c.Jan_Max AS janMax,
                CAST(c.Jan_Man_Load AS INT) AS janManLoad,
                c.Feb_Min AS febMin,
                c.Feb_Max AS febMax,
                CAST(c.Feb_Man_Load AS INT) AS febManLoad,
                c.Mar_Min AS marMin,
                c.Mar_Max AS marMax,
                CAST(c.Mar_Man_Load AS INT) AS marManLoad,
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
            FROM [dbo].[CPPPowerAssetCapacity] c WITH(NOLOCK)
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
                CAST(s.Apr_Man_Load AS INT) AS aprManLoad,
                s.May_Min AS mayMin,
                s.May_Max AS mayMax,
                CAST(s.May_Man_Load AS INT) AS mayManLoad,
                s.Jun_Min AS junMin,
                s.Jun_Max AS junMax,
                CAST(s.Jun_Man_Load AS INT) AS junManLoad,
                s.Jul_Min AS julMin,
                s.Jul_Max AS julMax,
                CAST(s.Jul_Man_Load AS INT) AS julManLoad,
                s.Aug_Min AS augMin,
                s.Aug_Max AS augMax,
                CAST(s.Aug_Man_Load AS INT) AS augManLoad,
                s.Sep_Min AS sepMin,
                s.Sep_Max AS sepMax,
                CAST(s.Sep_Man_Load AS INT) AS sepManLoad,
                s.Oct_Min AS octMin,
                s.Oct_Max AS octMax,
                CAST(s.Oct_Man_Load AS INT) AS octManLoad,
                s.Nov_Min AS novMin,
                s.Nov_Max AS novMax,
                CAST(s.Nov_Man_Load AS INT) AS novManLoad,
                s.Dec_Min AS decMin,
                s.Dec_Max AS decMax,
                CAST(s.Dec_Man_Load AS INT) AS decManLoad,
                s.Jan_Min AS janMin,
                s.Jan_Max AS janMax,
                CAST(s.Jan_Man_Load AS INT) AS janManLoad,
                s.Feb_Min AS febMin,
                s.Feb_Max AS febMax,
                CAST(s.Feb_Man_Load AS INT) AS febManLoad,
                s.Mar_Min AS marMin,
                s.Mar_Max AS marMax,
                CAST(s.Mar_Man_Load AS INT) AS marManLoad,
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
            FROM [dbo].[CPPSteamAssetCapacity] s WITH(NOLOCK)
            LEFT JOIN [dbo].[CPPSteamGenerationAsset] sa WITH(NOLOCK) ON s.Asset_FK_Id = sa.AssetId
            LEFT JOIN Plants pl2 WITH(NOLOCK) ON sa.CPPPLANT_FK_Id = pl2.Id
            LEFT JOIN NormParameters npg2 WITH(NOLOCK) ON npg2.Id = sa.UtilityGeneration_FK_Id
            LEFT JOIN NormParameters npd2 WITH(NOLOCK) ON npd2.Id = sa.UtilityDistributed_FK_Id
            WHERE sa.CPPPLANT_FK_Id IN :plantIds
              AND s.AOPYear = :aopYear
        )
        SELECT
            id, assetFkId, plantFkId, fixedMin, fixedMax,
            aprMin, aprMax, aprManLoad, mayMin, mayMax, mayManLoad, junMin, junMax, junManLoad, julMin, julMax, julManLoad,
            augMin, augMax, augManLoad, sepMin, sepMax, sepManLoad, octMin, octMax, octManLoad, novMin, novMax, novManLoad,
            decMin, decMax, decManLoad, janMin, janMax, janManLoad, febMin, febMax, febManLoad, marMin, marMax, marManLoad,
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

    @Query(value = "SELECT c.* FROM [dbo].[CPPPowerAssetCapacity] c WITH(NOLOCK) " +
                   "LEFT JOIN PowerGenerationAssets a WITH(NOLOCK) ON c.Asset_FK_Id = a.AssetId " +
                   "WHERE a.CPPPLANT_FK_Id = :plantId AND c.AOPYear = :aopYear", nativeQuery = true)
    List<CPPPowerAssetCapacity> findByPlantIdAndAopYear(@Param("plantId") UUID plantId, @Param("aopYear") String aopYear);

    @Query(value = """
        WITH AssetBase AS (
            SELECT
                CAST(NULL AS UNIQUEIDENTIFIER) AS id,
                pga.AssetId                    AS assetFkId,
                pl.Id                          AS plantFkId,
                CAST(NULL AS FLOAT)            AS fixedMin,
                CAST(NULL AS FLOAT)            AS fixedMax,
                CAST(NULL AS FLOAT)            AS aprMin,
                CAST(NULL AS FLOAT)            AS aprMax,
                CAST(NULL AS FLOAT)            AS mayMin,
                CAST(NULL AS FLOAT)            AS mayMax,
                CAST(NULL AS FLOAT)            AS junMin,
                CAST(NULL AS FLOAT)            AS junMax,
                CAST(NULL AS FLOAT)            AS julMin,
                CAST(NULL AS FLOAT)            AS julMax,
                CAST(NULL AS FLOAT)            AS augMin,
                CAST(NULL AS FLOAT)            AS augMax,
                CAST(NULL AS FLOAT)            AS sepMin,
                CAST(NULL AS FLOAT)            AS sepMax,
                CAST(NULL AS FLOAT)            AS octMin,
                CAST(NULL AS FLOAT)            AS octMax,
                CAST(NULL AS FLOAT)            AS novMin,
                CAST(NULL AS FLOAT)            AS novMax,
                CAST(NULL AS FLOAT)            AS decMin,
                CAST(NULL AS FLOAT)            AS decMax,
                CAST(NULL AS FLOAT)            AS janMin,
                CAST(NULL AS FLOAT)            AS janMax,
                CAST(NULL AS FLOAT)            AS febMin,
                CAST(NULL AS FLOAT)            AS febMax,
                CAST(NULL AS FLOAT)            AS marMin,
                CAST(NULL AS FLOAT)            AS marMax,
                CAST(NULL AS VARCHAR(10))      AS aopYear,
                CAST(NULL AS VARCHAR(500))     AS remarks,
                CAST(COALESCE(npg.UOM, npd.UOM) AS VARCHAR(50)) AS uom,
                CAST(NULL AS VARCHAR(19))      AS createdDate,
                CAST(NULL AS VARCHAR(19))      AS modifiedDate,
                pga.AssetName                  AS assetName,
                pga.AssetType                  AS assetType,
                pl.DisplayName                 AS plantName,
                pl.Name                        AS plantCode,
                'Power'                        AS assetCategory,
                pl.Site_FK_Id                  AS siteFkId,
                pl.Vertical_FK_Id              AS verticalFkId,
                0                              AS categoryOrder,
                pga.DisplayName                AS sortDisplayName,
                npd.Name                       AS utilityDistributed,
                npd.SAPMaterialCode            AS distributedSapCode,
                npg.Name                       AS utilityGenerated,
                npg.SAPMaterialCode            AS generatedUtilityCode,
                CASE
                    WHEN CHARINDEX('-', pga.DisplayName) > 0
                    THEN LEFT(pga.DisplayName, CHARINDEX('-', pga.DisplayName) - 1)
                    ELSE pga.DisplayName
                END AS namePrefix,
                CASE
                    WHEN CHARINDEX('-', pga.DisplayName) > 0
                    THEN TRY_CAST(RIGHT(pga.DisplayName, LEN(pga.DisplayName) - CHARINDEX('-', pga.DisplayName)) AS INT)
                    ELSE 0
                END AS nameNumber
            FROM PowerGenerationAssets pga WITH(NOLOCK)
            LEFT JOIN Plants pl WITH(NOLOCK) ON pga.CPPPLANT_FK_Id = pl.Id
            LEFT JOIN NormParameters npg WITH(NOLOCK) ON npg.Id = pga.UtilityGeneration_FK_Id
            LEFT JOIN NormParameters npd WITH(NOLOCK) ON npd.Id = pga.UtilityDistributed_FK_Id
            WHERE pga.CPPPLANT_FK_Id IN :plantIds

            UNION ALL

            SELECT
                CAST(NULL AS UNIQUEIDENTIFIER) AS id,
                sga.AssetId                    AS assetFkId,
                pl2.Id                         AS plantFkId,
                CAST(NULL AS FLOAT)            AS fixedMin,
                CAST(NULL AS FLOAT)            AS fixedMax,
                CAST(NULL AS FLOAT)            AS aprMin,
                CAST(NULL AS FLOAT)            AS aprMax,
                CAST(NULL AS FLOAT)            AS mayMin,
                CAST(NULL AS FLOAT)            AS mayMax,
                CAST(NULL AS FLOAT)            AS junMin,
                CAST(NULL AS FLOAT)            AS junMax,
                CAST(NULL AS FLOAT)            AS julMin,
                CAST(NULL AS FLOAT)            AS julMax,
                CAST(NULL AS FLOAT)            AS augMin,
                CAST(NULL AS FLOAT)            AS augMax,
                CAST(NULL AS FLOAT)            AS sepMin,
                CAST(NULL AS FLOAT)            AS sepMax,
                CAST(NULL AS FLOAT)            AS octMin,
                CAST(NULL AS FLOAT)            AS octMax,
                CAST(NULL AS FLOAT)            AS novMin,
                CAST(NULL AS FLOAT)            AS novMax,
                CAST(NULL AS FLOAT)            AS decMin,
                CAST(NULL AS FLOAT)            AS decMax,
                CAST(NULL AS FLOAT)            AS janMin,
                CAST(NULL AS FLOAT)            AS janMax,
                CAST(NULL AS FLOAT)            AS febMin,
                CAST(NULL AS FLOAT)            AS febMax,
                CAST(NULL AS FLOAT)            AS marMin,
                CAST(NULL AS FLOAT)            AS marMax,
                CAST(NULL AS VARCHAR(20))      AS aopYear,
                CAST(NULL AS VARCHAR(500))     AS remarks,
                CAST(COALESCE(npg2.UOM, npd2.UOM) AS VARCHAR(50)) AS uom,
                CAST(NULL AS VARCHAR(19))      AS createdDate,
                CAST(NULL AS VARCHAR(19))      AS modifiedDate,
                sga.AssetName                  AS assetName,
                sga.AssetType                  AS assetType,
                pl2.DisplayName                AS plantName,
                pl2.Name                       AS plantCode,
                'Steam'                        AS assetCategory,
                pl2.Site_FK_Id                 AS siteFkId,
                pl2.Vertical_FK_Id             AS verticalFkId,
                1                              AS categoryOrder,
                sga.DisplayName                AS sortDisplayName,
                npd2.Name                      AS utilityDistributed,
                npd2.SAPMaterialCode           AS distributedSapCode,
                npg2.Name                      AS utilityGenerated,
                npg2.SAPMaterialCode           AS generatedUtilityCode,
                CASE
                    WHEN CHARINDEX('-', sga.DisplayName) > 0
                    THEN LEFT(sga.DisplayName, CHARINDEX('-', sga.DisplayName) - 1)
                    ELSE sga.DisplayName
                END AS namePrefix,
                CASE
                    WHEN CHARINDEX('-', sga.DisplayName) > 0
                    THEN TRY_CAST(RIGHT(sga.DisplayName, LEN(sga.DisplayName) - CHARINDEX('-', sga.DisplayName)) AS INT)
                    ELSE 0
                END AS nameNumber
            FROM [dbo].[CPPSteamGenerationAsset] sga WITH(NOLOCK)
            LEFT JOIN Plants pl2 WITH(NOLOCK) ON sga.CPPPLANT_FK_Id = pl2.Id
            LEFT JOIN NormParameters npg2 WITH(NOLOCK) ON npg2.Id = sga.UtilityGeneration_FK_Id
            LEFT JOIN NormParameters npd2 WITH(NOLOCK) ON npd2.Id = sga.UtilityDistributed_FK_Id
            WHERE sga.CPPPLANT_FK_Id IN :plantIds
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
        FROM AssetBase
        ORDER BY categoryOrder, plantName, namePrefix, nameNumber, sortDisplayName;
        """, nativeQuery = true)
    List<CPPAssetCapacityProjection> findAllAssetsForPlants(
            @Param("plantIds") List<UUID> plantIds);
}
