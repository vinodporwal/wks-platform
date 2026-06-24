package com.wks.caseengine.cpp.repository;

import com.wks.caseengine.cpp.entity.CPPSteamAssetCapacity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CPPSteamAssetCapacityRepository extends JpaRepository<CPPSteamAssetCapacity, UUID> {

    @Query(value = "SELECT s.* FROM [RIL.AOP].[dbo].[CPPSteamAssetCapacity] s WITH(NOLOCK) " +
                   "LEFT JOIN [RIL.AOP].[dbo].[CPPSteamGenerationAsset] sa WITH(NOLOCK) ON s.Asset_FK_Id = sa.AssetId " +
                   "WHERE sa.CPPPLANT_FK_Id = :plantId AND s.AOPYear = :aopYear", nativeQuery = true)
    List<CPPSteamAssetCapacity> findByPlantIdAndAopYear(@Param("plantId") UUID plantId, @Param("aopYear") String aopYear);
}
