package com.wks.caseengine.cpp.repository;

import com.wks.caseengine.cpp.dto.CPPAssetPriorityProjection;
import com.wks.caseengine.cpp.entity.CPPPowerAssetPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CPPPowerAssetPriorityRepository extends JpaRepository<CPPPowerAssetPriority, UUID> {

    @Query(value = """
        WITH Combined AS (
            SELECT
                p.Id AS id,
                p.Asset_FK_Id AS assetFkId,
                p.Plant_FK_Id AS plantFkId,
                p.Apr AS apr,
                p.May AS may,
                p.Jun AS jun,
                p.Jul AS jul,
                p.Aug AS aug,
                p.Sep AS sep,
                p.Oct AS oct,
                p.Nov AS nov,
                p.Dec AS dec,
                p.Jan AS jan,
                p.Feb AS feb,
                p.Mar AS mar,
                p.Remarks AS remarks,
                CONVERT(VARCHAR, p.CreatedDate, 120) AS createdDate,
                CONVERT(VARCHAR, p.UpdatedDate, 120) AS modifiedDate,
                a.AssetName AS assetName,
                a.AssetType AS assetType,
                pl.DisplayName AS plantName,
                'Power' AS assetCategory,
                pl.Site_FK_Id AS siteFkId,
                pl.Vertical_FK_Id AS verticalFkId,
                0 AS categoryOrder,
                a.DisplayName AS sortDisplayName,
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
            FROM [dbo].[CPPPowerAssetPriority] p WITH(NOLOCK)
            LEFT JOIN PowerGenerationAssets a WITH(NOLOCK) ON p.Asset_FK_Id = a.AssetId
            LEFT JOIN Plants pl WITH(NOLOCK) ON p.Plant_FK_Id = pl.Id
            WHERE p.Plant_FK_Id IN :plantIds
              AND p.AOPYear = :aopYear

            UNION ALL

            SELECT
                s.Id AS id,
                s.Asset_FK_Id AS assetFkId,
                s.Plant_FK_Id AS plantFkId,
                s.Apr AS apr,
                s.May AS may,
                s.Jun AS jun,
                s.Jul AS jul,
                s.Aug AS aug,
                s.Sep AS sep,
                s.Oct AS oct,
                s.Nov AS nov,
                s.Dec AS dec,
                s.Jan AS jan,
                s.Feb AS feb,
                s.Mar AS mar,
                s.Remarks AS remarks,
                CONVERT(VARCHAR, s.CreatedDate, 120) AS createdDate,
                CONVERT(VARCHAR, s.UpdatedDate, 120) AS modifiedDate,
                sa.AssetName AS assetName,
                sa.AssetType AS assetType,
                pl2.DisplayName AS plantName,
                'Steam' AS assetCategory,
                pl2.Site_FK_Id AS siteFkId,
                pl2.Vertical_FK_Id AS verticalFkId,
                1 AS categoryOrder,
                sa.DisplayName AS sortDisplayName,
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
            FROM [dbo].[CPPSteamAssetsPriority] s WITH(NOLOCK)
            LEFT JOIN [dbo].[CPPSteamGenerationAsset] sa WITH(NOLOCK) ON s.Asset_FK_Id = sa.AssetId
            LEFT JOIN Plants pl2 WITH(NOLOCK) ON s.Plant_FK_Id = pl2.Id
            WHERE s.Plant_FK_Id IN :plantIds
              AND s.AOPYear = :aopYear
        )
        SELECT
            id, assetFkId, plantFkId,
            apr, may, jun, jul, aug, sep, oct, nov, dec, jan, feb, mar,
            remarks, createdDate, modifiedDate,
            assetName, assetType, plantName, assetCategory, siteFkId, verticalFkId
        FROM Combined
        ORDER BY categoryOrder, plantName, namePrefix, nameNumber, sortDisplayName
        """, nativeQuery = true)
    List<CPPAssetPriorityProjection> findAssetPrioritiesByPlants(
        @Param("plantIds") List<UUID> plantIds,
        @Param("aopYear") String aopYear);

    List<CPPPowerAssetPriority> findByPlantFkIdAndAopYear(UUID plantFkId, String aopYear);

    /**
     * Fetches all Power and Steam assets for the given plants directly from the asset tables,
     * with NULL for all monthly priority values. Used to seed zero-priority records when no
     * historical priority data exists for any financial year.
     */
    @Query(value = """
        WITH AssetBase AS (
            SELECT
                CAST(NULL AS UNIQUEIDENTIFIER) AS id,
                pga.AssetId                    AS assetFkId,
                pga.CPPPLANT_FK_Id             AS plantFkId,
                CAST(NULL AS INT)              AS apr,
                CAST(NULL AS INT)              AS may,
                CAST(NULL AS INT)              AS jun,
                CAST(NULL AS INT)              AS jul,
                CAST(NULL AS INT)              AS aug,
                CAST(NULL AS INT)              AS sep,
                CAST(NULL AS INT)              AS oct,
                CAST(NULL AS INT)              AS nov,
                CAST(NULL AS INT)              AS dec,
                CAST(NULL AS INT)              AS jan,
                CAST(NULL AS INT)              AS feb,
                CAST(NULL AS INT)              AS mar,
                CAST(NULL AS VARCHAR(500))     AS remarks,
                CAST(NULL AS VARCHAR(19))      AS createdDate,
                CAST(NULL AS VARCHAR(19))      AS modifiedDate,
                pga.AssetName                  AS assetName,
                pga.AssetType                  AS assetType,
                pl.DisplayName                 AS plantName,
                'Power'                        AS assetCategory,
                pl.Site_FK_Id                  AS siteFkId,
                pl.Vertical_FK_Id              AS verticalFkId,
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
            LEFT JOIN Plants pl WITH(NOLOCK) ON pl.Id = pga.CPPPLANT_FK_Id
            WHERE pga.CPPPLANT_FK_Id IN :plantIds

            UNION ALL

            SELECT
                CAST(NULL AS UNIQUEIDENTIFIER) AS id,
                sga.AssetId                    AS assetFkId,
                sga.CPPPLANT_FK_Id             AS plantFkId,
                CAST(NULL AS INT)              AS apr,
                CAST(NULL AS INT)              AS may,
                CAST(NULL AS INT)              AS jun,
                CAST(NULL AS INT)              AS jul,
                CAST(NULL AS INT)              AS aug,
                CAST(NULL AS INT)              AS sep,
                CAST(NULL AS INT)              AS oct,
                CAST(NULL AS INT)              AS nov,
                CAST(NULL AS INT)              AS dec,
                CAST(NULL AS INT)              AS jan,
                CAST(NULL AS INT)              AS feb,
                CAST(NULL AS INT)              AS mar,
                CAST(NULL AS VARCHAR(500))     AS remarks,
                CAST(NULL AS VARCHAR(19))      AS createdDate,
                CAST(NULL AS VARCHAR(19))      AS modifiedDate,
                sga.AssetName                  AS assetName,
                sga.AssetType                  AS assetType,
                pl2.DisplayName                AS plantName,
                'Steam'                        AS assetCategory,
                pl2.Site_FK_Id                 AS siteFkId,
                pl2.Vertical_FK_Id             AS verticalFkId,
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
            WHERE sga.CPPPLANT_FK_Id IN :plantIds
        )
        SELECT
            id, assetFkId, plantFkId,
            apr, may, jun, jul, aug, sep, oct, nov, dec, jan, feb, mar,
            remarks, createdDate, modifiedDate,
            assetName, assetType, plantName, assetCategory, siteFkId, verticalFkId
        FROM AssetBase
        ORDER BY categoryOrder, plantName, namePrefix, nameNumber, sortDisplayName
        """, nativeQuery = true)
    List<CPPAssetPriorityProjection> findAllAssetsForPlants(
            @Param("plantIds") List<UUID> plantIds);
}
