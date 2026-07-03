package com.wks.caseengine.cpp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.wks.caseengine.cpp.entity.PowerGenerationAsset;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;


@Repository
public interface PowerGenerationAssetRepository extends JpaRepository<PowerGenerationAsset, UUID> {

    // Custom finder method examples (Spring will auto-generate the SQL query for these):
    
    List<PowerGenerationAsset> findByPlantCode(String plantCode);
    
    List<PowerGenerationAsset> findByAssetType(String assetType);
    
    List<PowerGenerationAsset> findByCppPlantFkId(UUID cppPlantFkId);
    
    @Query(value = "SELECT * FROM PowerGenerationAssets WHERE CPPPLANT_FK_Id IN (:plantIds) AND AssetType = :assetType", nativeQuery = true)
    List<PowerGenerationAsset> findByPlantIdsAndAssetType(
        @Param("plantIds") List<UUID> plantIds, 
        @Param("assetType") String assetType
    );
}
