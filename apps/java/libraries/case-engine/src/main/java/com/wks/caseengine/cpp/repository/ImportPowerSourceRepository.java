package com.wks.caseengine.cpp.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.entity.CPPImportPowerSourceMapping;

@Repository
public interface ImportPowerSourceRepository extends JpaRepository<CPPImportPowerSourceMapping, UUID> {
    
    List<CPPImportPowerSourceMapping> findByCppPlantFkId(UUID cppPlantFkId);
    
    List<CPPImportPowerSourceMapping> findByCppPlantFkIdAndIsActive(UUID cppPlantFkId, Boolean isActive);
    
    @Query(value = "SELECT * FROM CPPImportPowerSourceMapping WITH(NOLOCK) WHERE SourceName = :sourceName AND Plant_FK_Id = :plantId", nativeQuery = true)
    List<CPPImportPowerSourceMapping> findBySourceNameAndPlantId(@Param("sourceName") String sourceName, @Param("plantId") UUID plantId);

    @Modifying
    @Query(value = "DELETE FROM CPP_AssetNorms_Mapping WHERE AssetId = :assetId", nativeQuery = true)
    void deleteAssetNormsMappingByAssetId(@Param("assetId") UUID assetId);

    @Modifying
    @Query(value = "INSERT INTO CPP_AssetNorms_Mapping (NormParameters_ID, AssetId) VALUES (:normParameterId, :assetId)", nativeQuery = true)
    void insertAssetNormsMapping(@Param("normParameterId") UUID normParameterId, @Param("assetId") UUID assetId);
}
