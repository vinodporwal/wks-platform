package com.wks.caseengine.cpp.repository;

import com.wks.caseengine.cpp.dto.CPPAssetOperationalHoursProjection;
import com.wks.caseengine.cpp.entity.CPPAssetOperationalHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CPPAssetOperationalHoursRepository extends JpaRepository<CPPAssetOperationalHours, UUID> {

    Optional<CPPAssetOperationalHours> findByAssetFkIdAndPlantFkIdAndAopYear(UUID assetFkId, UUID plantFkId, String aopYear);

    List<CPPAssetOperationalHours> findByPlantFkIdAndAopYear(UUID plantFkId, String aopYear);

    @Query(value = """
        WITH Combined AS (
    SELECT
        h.Id AS id,
        h.Asset_FK_Id AS assetFkId,

        npd.Name AS utilityDistributed,
        npd.SAPMaterialCode AS distributedSapCode,
        npg.Name AS utilityGenerated,
        npg.SAPMaterialCode AS generatedUtilityCode,

        h.Apr AS apr,
        h.May AS may,
        h.Jun AS jun,
        h.Jul AS jul,
        h.Aug AS aug,
        h.Sep AS sep,
        h.Oct AS oct,
        h.Nov AS nov,
        h.[Dec] AS dec,
        h.Jan AS jan,
        h.Feb AS feb,
        h.Mar AS mar,
        CAST(h.AOPYear AS VARCHAR(10)) AS aopYear,
        h.Remarks AS remarks,
        h.Site_FK_Id AS siteFkId,
        h.Vertical_FK_ID AS verticalFkId,
        h.Plant_FK_Id AS plantFkId,
        CONVERT(VARCHAR(19), h.CreatedDate, 120) AS createdDate,
        CONVERT(VARCHAR(19), h.ModifiedDate, 120) AS modifiedDate,
        pga.AssetName AS assetName,
        pga.AssetType AS assetType,
        pl.DisplayName AS plantName,
        'Power' AS assetCategory,
        0 AS categoryOrder,
        pga.DisplayName AS sortDisplayName,

        CASE
            WHEN CHARINDEX('-', pga.DisplayName) > 0
            THEN LEFT(pga.DisplayName, CHARINDEX('-', pga.DisplayName) - 1)
            ELSE pga.DisplayName
        END AS namePrefix,

        CASE
            WHEN CHARINDEX('-', pga.DisplayName) > 0
            THEN TRY_CAST(RIGHT(pga.DisplayName,
                    LEN(pga.DisplayName) - CHARINDEX('-', pga.DisplayName)) AS INT)
            ELSE 0
        END AS nameNumber

        FROM [dbo].[CPPAssetOperationalHours] h WITH(NOLOCK)

        LEFT JOIN PowerGenerationAssets pga WITH(NOLOCK)
            ON pga.AssetId = h.Asset_FK_Id

        LEFT JOIN NormParameters npg WITH(NOLOCK)
            ON npg.Id = pga.UtilityGeneration_FK_Id

        LEFT JOIN NormParameters npd WITH(NOLOCK)
            ON npd.Id = pga.UtilityDistributed_FK_Id

        LEFT JOIN Plants pl WITH(NOLOCK)
            ON pl.Id = h.Plant_FK_Id

        WHERE h.Plant_FK_Id IN :plantIds
        AND h.AOPYear = :financialYear

        UNION ALL

        SELECT
            sh.Id AS id,
            sh.SteamAsset_FK_Id AS assetFkId,

            npd2.Name AS utilityDistributed,
            npd2.SAPMaterialCode AS distributedSapCode,
            npg2.Name AS utilityGenerated,
            npg2.SAPMaterialCode AS generatedUtilityCode,

            sh.Apr AS apr,
            sh.May AS may,
            sh.Jun AS jun,
            sh.Jul AS jul,
            sh.Aug AS aug,
            sh.Sep AS sep,
            sh.Oct AS oct,
            sh.Nov AS nov,
            sh.[Dec] AS dec,
            sh.Jan AS jan,
            sh.Feb AS feb,
            sh.Mar AS mar,
            CAST(sh.AOPYear AS VARCHAR(20)) AS aopYear,
            sh.Remarks AS remarks,
            sh.Site_FK_Id AS siteFkId,
            sh.Vertical_FK_ID AS verticalFkId,
            sh.Plant_FK_Id AS plantFkId,
            CONVERT(VARCHAR(19), sh.CreatedDate, 120) AS createdDate,
            CONVERT(VARCHAR(19), sh.UpdatedDate, 120) AS modifiedDate,
            sga.AssetName AS assetName,
            sga.AssetType AS assetType,
            pl2.DisplayName AS plantName,
            'Steam' AS assetCategory,
            1 AS categoryOrder,
            sga.DisplayName AS sortDisplayName,

            CASE
                WHEN CHARINDEX('-', sga.DisplayName) > 0
                THEN LEFT(sga.DisplayName, CHARINDEX('-', sga.DisplayName) - 1)
                ELSE sga.DisplayName
            END AS namePrefix,

            CASE
                WHEN CHARINDEX('-', sga.DisplayName) > 0
                THEN TRY_CAST(RIGHT(sga.DisplayName,
                        LEN(sga.DisplayName) - CHARINDEX('-', sga.DisplayName)) AS INT)
                ELSE 0
            END AS nameNumber

        FROM [dbo].[CPPSteamAssetsOperationalHours] sh WITH(NOLOCK)

        LEFT JOIN [dbo].[CPPSteamGenerationAsset] sga WITH(NOLOCK)
            ON sga.AssetId = sh.SteamAsset_FK_Id

        LEFT JOIN NormParameters npg2 WITH(NOLOCK)
            ON npg2.Id = sga.UtilityGeneration_FK_Id

        LEFT JOIN NormParameters npd2 WITH(NOLOCK)
            ON npd2.Id = sga.UtilityDistributed_FK_Id

        LEFT JOIN Plants pl2 WITH(NOLOCK)
            ON pl2.Id = sh.Plant_FK_Id

        WHERE sh.Plant_FK_Id IN :plantIds
        AND sh.AOPYear = :financialYear
    )

    SELECT
        id,
        assetFkId,
        utilityDistributed,
        distributedSapCode,
        utilityGenerated,
        generatedUtilityCode,
        apr,
        may,
        jun,
        jul,
        aug,
        sep,
        oct,
        nov,
        dec,
        jan,
        feb,
        mar,
        aopYear,
        remarks,
        siteFkId,
        verticalFkId,
        plantFkId,
        createdDate,
        modifiedDate,
        assetName,
        assetType,
        plantName,
        assetCategory
    FROM Combined
    ORDER BY
        categoryOrder,
        plantName,
        namePrefix,
        nameNumber,
        sortDisplayName;
        """, nativeQuery = true)
    List<CPPAssetOperationalHoursProjection> findOperationalHoursByPlantsAndYear(
            @Param("plantIds") List<UUID> plantIds,
            @Param("financialYear") String financialYear);
    /**
     * Fetches all Power and Steam assets for the given plants directly from the asset tables,
     * with NULL for all monthly hours. Used to seed zero-hour records when no historical
     * operational hours data exists for any financial year.
     */
    @Query(value = """
        WITH AssetBase AS (
            SELECT
                CAST(NULL AS UNIQUEIDENTIFIER) AS id,
                pga.AssetId                    AS assetFkId,
                npd.Name                       AS utilityDistributed,
                npd.SAPMaterialCode            AS distributedSapCode,
                npg.Name                       AS utilityGenerated,
                npg.SAPMaterialCode            AS generatedUtilityCode,
                CAST(NULL AS FLOAT)            AS apr,
                CAST(NULL AS FLOAT)            AS may,
                CAST(NULL AS FLOAT)            AS jun,
                CAST(NULL AS FLOAT)            AS jul,
                CAST(NULL AS FLOAT)            AS aug,
                CAST(NULL AS FLOAT)            AS sep,
                CAST(NULL AS FLOAT)            AS oct,
                CAST(NULL AS FLOAT)            AS nov,
                CAST(NULL AS FLOAT)            AS dec,
                CAST(NULL AS FLOAT)            AS jan,
                CAST(NULL AS FLOAT)            AS feb,
                CAST(NULL AS FLOAT)            AS mar,
                CAST(NULL AS VARCHAR(20))      AS aopYear,
                CAST(NULL AS VARCHAR(500))     AS remarks,
                pl.Site_FK_Id                  AS siteFkId,
                pl.Vertical_FK_Id              AS verticalFkId,
                pl.Id                          AS plantFkId,
                CAST(NULL AS VARCHAR(19))      AS createdDate,
                CAST(NULL AS VARCHAR(19))      AS modifiedDate,
                pga.AssetName                  AS assetName,
                pga.AssetType                  AS assetType,
                pl.DisplayName                 AS plantName,
                'Power'                        AS assetCategory,
                0                              AS categoryOrder,
                pga.DisplayName                AS sortDisplayName,
                CASE
                    WHEN CHARINDEX('-', pga.DisplayName) > 0
                    THEN LEFT(pga.DisplayName, CHARINDEX('-', pga.DisplayName) - 1)
                    ELSE pga.DisplayName
                END AS namePrefix,
                CASE
                    WHEN CHARINDEX('-', pga.DisplayName) > 0
                    THEN TRY_CAST(RIGHT(pga.DisplayName,
                            LEN(pga.DisplayName) - CHARINDEX('-', pga.DisplayName)) AS INT)
                    ELSE 0
                END AS nameNumber
            FROM PowerGenerationAssets pga WITH(NOLOCK)
            LEFT JOIN Plants pl  WITH(NOLOCK) ON pl.Id  = pga.CPPPLANT_FK_Id
            LEFT JOIN NormParameters npg WITH(NOLOCK) ON npg.Id = pga.UtilityGeneration_FK_Id
            LEFT JOIN NormParameters npd WITH(NOLOCK) ON npd.Id = pga.UtilityDistributed_FK_Id
            WHERE pga.CPPPLANT_FK_Id IN :plantIds

            UNION ALL

            SELECT
                CAST(NULL AS UNIQUEIDENTIFIER) AS id,
                sga.AssetId                    AS assetFkId,
                npd2.Name                      AS utilityDistributed,
                npd2.SAPMaterialCode           AS distributedSapCode,
                npg2.Name                      AS utilityGenerated,
                npg2.SAPMaterialCode           AS generatedUtilityCode,
                CAST(NULL AS FLOAT)            AS apr,
                CAST(NULL AS FLOAT)            AS may,
                CAST(NULL AS FLOAT)            AS jun,
                CAST(NULL AS FLOAT)            AS jul,
                CAST(NULL AS FLOAT)            AS aug,
                CAST(NULL AS FLOAT)            AS sep,
                CAST(NULL AS FLOAT)            AS oct,
                CAST(NULL AS FLOAT)            AS nov,
                CAST(NULL AS FLOAT)            AS dec,
                CAST(NULL AS FLOAT)            AS jan,
                CAST(NULL AS FLOAT)            AS feb,
                CAST(NULL AS FLOAT)            AS mar,
                CAST(NULL AS VARCHAR(20))      AS aopYear,
                CAST(NULL AS VARCHAR(500))     AS remarks,
                pl2.Site_FK_Id                 AS siteFkId,
                pl2.Vertical_FK_Id             AS verticalFkId,
                pl2.Id                         AS plantFkId,
                CAST(NULL AS VARCHAR(19))      AS createdDate,
                CAST(NULL AS VARCHAR(19))      AS modifiedDate,
                sga.AssetName                  AS assetName,
                sga.AssetType                  AS assetType,
                pl2.DisplayName                AS plantName,
                'Steam'                        AS assetCategory,
                1                              AS categoryOrder,
                sga.DisplayName                AS sortDisplayName,
                CASE
                    WHEN CHARINDEX('-', sga.DisplayName) > 0
                    THEN LEFT(sga.DisplayName, CHARINDEX('-', sga.DisplayName) - 1)
                    ELSE sga.DisplayName
                END AS namePrefix,
                CASE
                    WHEN CHARINDEX('-', sga.DisplayName) > 0
                    THEN TRY_CAST(RIGHT(sga.DisplayName,
                            LEN(sga.DisplayName) - CHARINDEX('-', sga.DisplayName)) AS INT)
                    ELSE 0
                END AS nameNumber
            FROM [dbo].[CPPSteamGenerationAsset] sga WITH(NOLOCK)
            LEFT JOIN Plants pl2 WITH(NOLOCK) ON pl2.Id = sga.CPPPLANT_FK_Id
            LEFT JOIN NormParameters npg2 WITH(NOLOCK) ON npg2.Id = sga.UtilityGeneration_FK_Id
            LEFT JOIN NormParameters npd2 WITH(NOLOCK) ON npd2.Id = sga.UtilityDistributed_FK_Id
            WHERE sga.CPPPLANT_FK_Id IN :plantIds
        )
        SELECT
            id, assetFkId, utilityDistributed, distributedSapCode,
            utilityGenerated, generatedUtilityCode,
            apr, may, jun, jul, aug, sep, oct, nov, dec, jan, feb, mar,
            aopYear, remarks, siteFkId, verticalFkId, plantFkId,
            createdDate, modifiedDate, assetName, assetType, plantName, assetCategory
        FROM AssetBase
        ORDER BY categoryOrder, plantName, namePrefix, nameNumber, sortDisplayName;
        """, nativeQuery = true)
    List<CPPAssetOperationalHoursProjection> findAllAssetsForPlants(
            @Param("plantIds") List<UUID> plantIds);
}
