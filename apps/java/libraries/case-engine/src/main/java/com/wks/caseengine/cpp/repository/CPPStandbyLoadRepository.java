package com.wks.caseengine.cpp.repository;

import com.wks.caseengine.cpp.dto.CPPStandbyLoadProjection;
import com.wks.caseengine.cpp.entity.CPPStandbyLoad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@Transactional
public interface CPPStandbyLoadRepository extends JpaRepository<CPPStandbyLoad, UUID> {

    @Modifying
    @Transactional
    @Query(value =
        "UPDATE dbo.CPPStandByLoad SET " +
        "  Apr = :apr, May = :may, Jun = :jun, Jul = :jul, " +
        "  Aug = :aug, Sep = :sep, Oct = :oct, Nov = :nov, " +
        "  Dec = :dec, Jan = :jan, Feb = :feb, Mar = :mar, " +
        "  Remarks = :remarks, UOM = :uom, UpdatedDate = GETDATE() " +
        "WHERE Id = :id",
        nativeQuery = true)
    int updateStandbyLoad(
            @Param("id")      UUID id,
            @Param("apr")     Double apr,
            @Param("may")     Double may,
            @Param("jun")     Double jun,
            @Param("jul")     Double jul,
            @Param("aug")     Double aug,
            @Param("sep")     Double sep,
            @Param("oct")     Double oct,
            @Param("nov")     Double nov,
            @Param("dec")     Double dec,
            @Param("jan")     Double jan,
            @Param("feb")     Double feb,
            @Param("mar")     Double mar,
            @Param("remarks") String remarks,
            @Param("uom")     String uom);

    @Query(value = """
            WITH Combined AS (
                SELECT
                    c.Id AS id,
                    c.Asset_FK_Id AS assetFkId,
                    c.Apr AS apr,
                    c.May AS may,
                    c.Jun AS jun,
                    c.Jul AS jul,
                    c.Aug AS aug,
                    c.Sep AS sep,
                    c.Oct AS oct,
                    c.Nov AS nov,
                    c.Dec AS dec,
                    c.Jan AS jan,
                    c.Feb AS feb,
                    c.Mar AS mar,
                    CAST(c.AOPYear AS VARCHAR(10)) AS aopYear,
                    c.Remarks AS remarks,
                    COALESCE(npg.UOM, npd.UOM) AS uom,
                    CONVERT(VARCHAR, c.CreatedDate, 120) AS createdDate,
                    CONVERT(VARCHAR, c.UpdatedDate, 120) AS modifiedDate,
                    pl.DisplayName AS generatingPlant,
                    npd.Name AS utilityDistributed,
                    npd.SAPMaterialCode AS distributedSapCode,
                    npg.Name AS utilityGenerated,
                    npg.SAPMaterialCode AS generatedUtilityCode,
                    pl.DisplayName AS cppPlantName,
                    pl.Name AS plantCode,
                    c.Type AS type,
                    pl.Site_FK_Id AS siteFkId,
                    pl.Vertical_FK_Id AS verticalFkId,
                    pl.Id AS plantFkId
                FROM [dbo].[CPPStandByLoad] c WITH(NOLOCK)
                LEFT JOIN PowerGenerationAssets a WITH(NOLOCK) ON c.Asset_FK_Id = a.AssetId
                LEFT JOIN Plants pl WITH(NOLOCK) ON a.CPPPLANT_FK_Id = pl.Id
                LEFT JOIN NormParameters npg WITH(NOLOCK) ON npg.Id = a.UtilityGeneration_FK_Id
                LEFT JOIN NormParameters npd WITH(NOLOCK) ON npd.Id = a.UtilityDistributed_FK_Id
                WHERE a.CPPPLANT_FK_Id IN :plantIds
                  AND c.AOPYear = :aopYear
                  AND c.Type = 'power'

                UNION ALL

                SELECT
                    c.Id AS id,
                    c.Asset_FK_Id AS assetFkId,
                    c.Apr AS apr,
                    c.May AS may,
                    c.Jun AS jun,
                    c.Jul AS jul,
                    c.Aug AS aug,
                    c.Sep AS sep,
                    c.Oct AS oct,
                    c.Nov AS nov,
                    c.Dec AS dec,
                    c.Jan AS jan,
                    c.Feb AS feb,
                    c.Mar AS mar,
                    CAST(c.AOPYear AS VARCHAR(10)) AS aopYear,
                    c.Remarks AS remarks,
                    COALESCE(npg2.UOM, npd2.UOM) AS uom,
                    CONVERT(VARCHAR, c.CreatedDate, 120) AS createdDate,
                    CONVERT(VARCHAR, c.UpdatedDate, 120) AS modifiedDate,
                    pl2.DisplayName AS generatingPlant,
                    npd2.Name AS utilityDistributed,
                    npd2.SAPMaterialCode AS distributedSapCode,
                    npg2.Name AS utilityGenerated,
                    npg2.SAPMaterialCode AS generatedUtilityCode,
                    pl2.DisplayName AS cppPlantName,
                    pl2.Name AS plantCode,
                    c.Type AS type,
                    pl2.Site_FK_Id AS siteFkId,
                    pl2.Vertical_FK_Id AS verticalFkId,
                    pl2.Id AS plantFkId
                FROM [dbo].[CPPStandByLoad] c WITH(NOLOCK)
                LEFT JOIN [dbo].[CPPSteamGenerationAsset] sa WITH(NOLOCK) ON c.Asset_FK_Id = sa.AssetId
                LEFT JOIN Plants pl2 WITH(NOLOCK) ON sa.CPPPLANT_FK_Id = pl2.Id
                LEFT JOIN NormParameters npg2 WITH(NOLOCK) ON npg2.Id = sa.UtilityGeneration_FK_Id
                LEFT JOIN NormParameters npd2 WITH(NOLOCK) ON npd2.Id = sa.UtilityDistributed_FK_Id
                WHERE sa.CPPPLANT_FK_Id IN :plantIds
                  AND c.AOPYear = :aopYear
                  AND c.Type = 'steam'
            )
            SELECT
                id, assetFkId, plantFkId,
                apr, may, jun, jul, aug, sep, oct, nov, dec, jan, feb, mar,
                aopYear, remarks, uom, createdDate, modifiedDate,
                generatingPlant, utilityDistributed, distributedSapCode,
                utilityGenerated, generatedUtilityCode,
                cppPlantName, plantCode, type,
                siteFkId, verticalFkId
            FROM Combined
            ORDER BY type, generatingPlant, utilityGenerated
            """, nativeQuery = true)
    List<CPPStandbyLoadProjection> findStandbyLoadByPlants(
            @Param("plantIds") List<UUID> plantIds,
            @Param("aopYear") String aopYear);

    @Query(value = "SELECT c.* FROM [dbo].[CPPStandByLoad] c WITH(NOLOCK) " +
                   "LEFT JOIN PowerGenerationAssets a WITH(NOLOCK) ON c.Asset_FK_Id = a.AssetId " +
                   "WHERE a.CPPPLANT_FK_Id = :plantId AND c.AOPYear = :aopYear AND c.Type = 'power'",
            nativeQuery = true)
    List<CPPStandbyLoad> findPowerByPlantIdAndAopYear(
            @Param("plantId") UUID plantId,
            @Param("aopYear") String aopYear);

    @Query(value = "SELECT c.* FROM [dbo].[CPPStandByLoad] c WITH(NOLOCK) " +
                   "LEFT JOIN [dbo].[CPPSteamGenerationAsset] sa WITH(NOLOCK) ON c.Asset_FK_Id = sa.AssetId " +
                   "WHERE sa.CPPPLANT_FK_Id = :plantId AND c.AOPYear = :aopYear AND c.Type = 'steam'",
            nativeQuery = true)
    List<CPPStandbyLoad> findSteamByPlantIdAndAopYear(
            @Param("plantId") UUID plantId,
            @Param("aopYear") String aopYear);
}
