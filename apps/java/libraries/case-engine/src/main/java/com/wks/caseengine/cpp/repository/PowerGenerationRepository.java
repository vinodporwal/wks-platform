package com.wks.caseengine.cpp.repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.cpp.dto.AssetMonthlyOperationalProjection;
import com.wks.caseengine.cpp.dto.PowerGenerationNormParametersProjection;
import com.wks.caseengine.cpp.dto.PowerGenerationSteamResposeProject;
import com.wks.caseengine.entity.DummyEntity;


@org.springframework.stereotype.Repository
public interface PowerGenerationRepository extends JpaRepository<DummyEntity, Long> {

    @NativeQuery("EXEC dbo.CPP_NMD_GetPowerGenerationOperationalHoursv1 :cppPlantId, :financialYear")
    List<AssetMonthlyOperationalProjection> getOperationalHours(
        @Param("cppPlantId") UUID cppPlantId,
        @Param("financialYear") String financialYear
    );

    // code for post 

    @Modifying
    @Transactional
    @NativeQuery("""
        UPDATE OperationalHours
        SET OperationalHours = :hours
        WHERE Asset_FK_Id = :assetId
          AND FinancialMonthId = :financialMonthId
        """)
    int updateOperationalHours(
            @Param("assetId") UUID assetId,
            @Param("financialMonthId") UUID financialMonthId,
            @Param("hours") Double hours
    );

    @Modifying
    @Transactional
    @NativeQuery("""
        INSERT INTO OperationalHours (Id, Asset_FK_Id, FinancialMonthId, OperationalHours)
        VALUES (NEWID(), :assetId, :financialMonthId, :hours)
        """)
    void insertOperationalHours(
            @Param("assetId") UUID assetId,
            @Param("financialMonthId") UUID financialMonthId,
            @Param("hours") Double hours
    );

    @Modifying
    @Transactional
    @NativeQuery("""
        MERGE OperationalHours AS target
        USING (SELECT :assetId AS Asset_FK_Id, :financialMonthId AS FinancialMonthId) AS source
        ON target.Asset_FK_Id = source.Asset_FK_Id
           AND target.FinancialMonthId = source.FinancialMonthId
        WHEN MATCHED THEN
            UPDATE SET OperationalHours = :hours
        WHEN NOT MATCHED THEN
            INSERT (Id, Asset_FK_Id, FinancialMonthId, OperationalHours)
            VALUES (NEWID(), :assetId, :financialMonthId, :hours);
        """)
    void upsertOperationalHours(
            @Param("assetId") UUID assetId,
            @Param("financialMonthId") UUID financialMonthId,
            @Param("hours") Double hours
    );


    // @Query(value = "select Name, NormType_FK_Id, SAPMaterialCode, AssetId from NormParameters where AssetId in :assetIds", nativeQuery = true)
    // List<PowerGenerationNormParametersProjection> getNormParametersByAssetIds(@Param("assetIds") List<UUID> assetIds);

    @NativeQuery("select np.Name, np.NormType_FK_Id, np.SAPMaterialCode, anm.AssetId from  CPP_AssetNorms_Mapping anm WITH(NOLOCK) join NormParameters np WITH(NOLOCK) on anm.NormParameters_ID = np.Id where anm.AssetId in :assetIds")
    List<PowerGenerationNormParametersProjection> getNormParametersByAssetIds(@Param("assetIds") List<UUID> assetIds);


    @NativeQuery("select * from UtilityPlantAssets WITH(NOLOCK) where PowerGenerationAsset_FK_Id = :powerGenerationAssetId")
    List<PowerGenerationSteamResposeProject> getPowerGenerationSteamResposeProject(@Param("powerGenerationAssetId") UUID powerGenerationAssetId);

    @NativeQuery("""
            EXEC dbo.CPP_NMD_Get_UtilityPlantAssets :cppPlantId, :financialYear
            """)
    List<PowerGenerationSteamResposeProject> getUtilityPlantAssets(@Param("cppPlantId") UUID cppPlantId, @Param("financialYear") String financialYear);


    @NativeQuery("EXEC dbo.CPP_NMD_Get_UtilityPlant_OperationalHours :cppPlantId, :financialYear")
    List<AssetMonthlyOperationalProjection> getLinkedOperationalHoursforUtilityPlant(
        @Param("cppPlantId") UUID cppPlantId,
        @Param("financialYear") String financialYear
    );



}

