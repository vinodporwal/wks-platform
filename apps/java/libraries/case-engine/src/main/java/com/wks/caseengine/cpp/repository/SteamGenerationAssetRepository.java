package com.wks.caseengine.cpp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.wks.caseengine.cpp.entity.PowerGenerationAsset;
import com.wks.caseengine.cpp.entity.SteamGenerationAsset;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface SteamGenerationAssetRepository extends JpaRepository<SteamGenerationAsset, UUID> {

    
    @Query(value = "SELECT * FROM SteamGenerationAsset WHERE CPPPLANT_FK_Id IN (:plantIds) AND AssetType = :assetType", nativeQuery = true)
    List<SteamGenerationAsset> findByPlantIdsAndAssetType(
        @Param("plantIds") List<UUID> plantIds, 
        @Param("assetType") String assetType
    );
    @Query("SELECT p.assetName FROM SteamGenerationAsset p WHERE p.assetId = :assetId")
    Optional<String> findAssetNameByAssetId(@Param("assetId") UUID assetId);
}
