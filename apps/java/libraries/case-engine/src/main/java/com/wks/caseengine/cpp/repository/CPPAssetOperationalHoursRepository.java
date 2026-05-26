package com.wks.caseengine.cpp.repository;

import com.wks.caseengine.cpp.dto.CPPAssetOperationalHoursProjection;
import com.wks.caseengine.cpp.entity.CPPAssetOperationalHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CPPAssetOperationalHoursRepository extends JpaRepository<CPPAssetOperationalHours, UUID> {

    @Query(value = """
        SELECT
            h.Id AS id,
            h.Asset_FK_Id AS assetFkId,
            h.utility_distributed AS utilityDistributed,
            h.distributed_sap_code AS distributedSapCode,
            h.utility_generated AS utilityGenerated,
            h.generated_utility_code AS generatedUtilityCode,
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
            'Power' AS assetCategory
        FROM [RIL.AOP].[dbo].[CPPAssetOperationalHours] h WITH(NOLOCK)
        LEFT JOIN PowerGenerationAssets pga WITH(NOLOCK)
            ON pga.AssetId = h.Asset_FK_Id
        LEFT JOIN Plants pl WITH(NOLOCK)
            ON pl.Id = h.Plant_FK_Id
        WHERE h.Plant_FK_Id IN :plantIds
          AND h.AOPYear = :financialYear
        
        UNION ALL
        
        SELECT
            sh.Id AS id,
            sh.SteamAsset_FK_Id AS assetFkId,
            sh.utility_distributed AS utilityDistributed,
            sh.distributed_sap_code AS distributedSapCode,
            sh.utility_generated AS utilityGenerated,
            sh.generated_utility_code AS generatedUtilityCode,
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
            'Steam' AS assetCategory
        FROM [RIL.AOP].[dbo].[CPPSteamAssetsOperationalHours] sh WITH(NOLOCK)
        LEFT JOIN [RIL.AOP].[dbo].[CPPSteamGenerationAsset] sga WITH(NOLOCK)
            ON sga.AssetId = sh.SteamAsset_FK_Id
        LEFT JOIN Plants pl2 WITH(NOLOCK)
            ON pl2.Id = sh.Plant_FK_Id
        WHERE sh.Plant_FK_Id IN :plantIds
          AND sh.AOPYear = :financialYear
        """, nativeQuery = true)
    List<CPPAssetOperationalHoursProjection> findOperationalHoursByPlantsAndYear(
            @Param("plantIds") List<UUID> plantIds,
            @Param("financialYear") String financialYear);
}
