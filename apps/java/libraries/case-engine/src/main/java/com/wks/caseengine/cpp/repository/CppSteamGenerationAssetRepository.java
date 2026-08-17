package com.wks.caseengine.cpp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.cpp.entity.CppSteamGenerationAsset;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface CppSteamGenerationAssetRepository extends JpaRepository<CppSteamGenerationAsset, UUID> {

    
    @Query(value = "SELECT * FROM CppSteamGenerationAsset WHERE CPPPLANT_FK_Id IN (:plantIds) AND AssetType = :assetType", nativeQuery = true)
    List<CppSteamGenerationAsset> findByPlantIdsAndAssetType(
        @Param("plantIds") List<UUID> plantIds, 
        @Param("assetType") String assetType
    );
    
    @Query("SELECT p.displayName FROM CppSteamGenerationAsset p WHERE p.assetId = :assetId")
    Optional<String> findDisplayNameByAssetId(@Param("assetId") UUID assetId);

    List<CppSteamGenerationAsset> findByLinkedPowerAssetFkIdAndCppPlantFkId(
            UUID linkedPowerAssetFkId, UUID cppPlantFkId);
}
