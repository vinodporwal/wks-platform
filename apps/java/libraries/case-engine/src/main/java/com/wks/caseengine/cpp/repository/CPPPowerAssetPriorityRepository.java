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
            pl.Vertical_FK_Id AS verticalFkId
        FROM [RIL.AOP].[dbo].[CPPPowerAssetPriority] p WITH(NOLOCK)
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
            pl2.Vertical_FK_Id AS verticalFkId
        FROM [RIL.AOP].[dbo].[CPPSteamAssetsPriority] s WITH(NOLOCK)
        LEFT JOIN [RIL.AOP].[dbo].[CPPSteamGenerationAsset] sa WITH(NOLOCK) ON s.Asset_FK_Id = sa.AssetId
        LEFT JOIN Plants pl2 WITH(NOLOCK) ON s.Plant_FK_Id = pl2.Id
        WHERE s.Plant_FK_Id IN :plantIds
          AND s.AOPYear = :aopYear
        
        ORDER BY 11, 10, 9
        """, nativeQuery = true)
    List<CPPAssetPriorityProjection> findAssetPrioritiesByPlants(
        @Param("plantIds") List<UUID> plantIds,
        @Param("aopYear") String aopYear);
}
