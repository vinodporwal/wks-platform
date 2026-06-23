package com.wks.caseengine.cpp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.cpp.dto.CompatibleFuelAssetProjection;
import com.wks.caseengine.cpp.dto.FuelMasterProjection;
import com.wks.caseengine.cpp.dto.PlantWiseFuelPriorityProjection;
import com.wks.caseengine.entity.DummyEntity;

@Repository
@Transactional
public interface FuelPriorityRepository extends JpaRepository<DummyEntity, Long> {

    @Query(value = "SELECT CAST(Id AS varchar(36)) as Id, FuelName, FuelDisplayName FROM CPP_FuelMaster WITH(NOLOCK)", nativeQuery = true)
    List<FuelMasterProjection> getFuelMaster();

    @Query(value = "EXEC dbo.CPP_GetPlantWiseFuelPriority :plantIds, :financialYear", nativeQuery = true)
    List<PlantWiseFuelPriorityProjection> getPlantWiseFuelPriority(@org.springframework.data.repository.query.Param("plantIds") String plantIds, @org.springframework.data.repository.query.Param("financialYear") String financialYear);

    @Modifying
    @Query(value = "UPDATE CPP_PlantFuelAvailability SET Fuel_FK_Id = :fuelFkId, Priority = :priority, Quantity = :quantity, Remarks = :remarks WHERE Id = :id", nativeQuery = true)
    int updatePlantFuelAvailability(
            @org.springframework.data.repository.query.Param("id") String id,
            @org.springframework.data.repository.query.Param("fuelFkId") String fuelFkId,
            @org.springframework.data.repository.query.Param("priority") Integer priority,
            @org.springframework.data.repository.query.Param("quantity") Integer quantity,
            @org.springframework.data.repository.query.Param("remarks") String remarks);

    @Query(value = "SELECT CAST(pga.AssetId AS varchar(36)) as AssetId, pga.AssetName, pga.AssetType, CAST(pga.CPPPLANT_FK_Id AS varchar(36)) as CppPlantFkId, p.DisplayName as PlantName, 'Power' as AssetCategory, pga.CompatibleFuel " +
            "FROM PowerGenerationAssets pga WITH(NOLOCK) " +
            "LEFT JOIN Plants p WITH(NOLOCK) ON p.Id = pga.CPPPLANT_FK_Id " +
            "UNION ALL " +
            "SELECT CAST(sga.AssetId AS varchar(36)) as AssetId, sga.AssetName, sga.AssetType, CAST(sga.CPPPLANT_FK_Id AS varchar(36)) as CppPlantFkId, p.DisplayName as PlantName, 'Steam' as AssetCategory, sga.CompatibleFuel " +
            "FROM CPPSteamGenerationAsset sga WITH(NOLOCK) " +
            "LEFT JOIN Plants p WITH(NOLOCK) ON p.Id = sga.CPPPLANT_FK_Id " +
            "ORDER BY AssetName", nativeQuery = true)
    List<CompatibleFuelAssetProjection> getCompatibleFuelAssets();

    @Query(value = "SELECT CAST(pga.AssetId AS varchar(36)) as AssetId, pga.AssetName, pga.AssetType, CAST(pga.CPPPLANT_FK_Id AS varchar(36)) as CppPlantFkId, p.DisplayName as PlantName, 'Power' as AssetCategory, pga.CompatibleFuel " +
            "FROM PowerGenerationAssets pga WITH(NOLOCK) " +
            "LEFT JOIN Plants p WITH(NOLOCK) ON p.Id = pga.CPPPLANT_FK_Id " +
            "WHERE pga.CPPPLANT_FK_Id IN (SELECT TRY_CAST(value AS UNIQUEIDENTIFIER) FROM STRING_SPLIT(:plantIds, ',')) " +
            "UNION ALL " +
            "SELECT CAST(sga.AssetId AS varchar(36)) as AssetId, sga.AssetName, sga.AssetType, CAST(sga.CPPPLANT_FK_Id AS varchar(36)) as CppPlantFkId, p.DisplayName as PlantName, 'Steam' as AssetCategory, sga.CompatibleFuel " +
            "FROM CPPSteamGenerationAsset sga WITH(NOLOCK) " +
            "LEFT JOIN Plants p WITH(NOLOCK) ON p.Id = sga.CPPPLANT_FK_Id " +
            "WHERE sga.CPPPLANT_FK_Id IN (SELECT TRY_CAST(value AS UNIQUEIDENTIFIER) FROM STRING_SPLIT(:plantIds, ',')) " +
            "ORDER BY AssetName", nativeQuery = true)
    List<CompatibleFuelAssetProjection> getCompatibleFuelAssetsByPlants(@org.springframework.data.repository.query.Param("plantIds") String plantIds);

    @Modifying
    @Query(value = "UPDATE PowerGenerationAssets SET CompatibleFuel = :compatibleFuel WHERE AssetId = :assetId", nativeQuery = true)
    int updatePowerAssetCompatibleFuel(
            @org.springframework.data.repository.query.Param("assetId") String assetId,
            @org.springframework.data.repository.query.Param("compatibleFuel") String compatibleFuel);

    @Modifying
    @Query(value = "UPDATE CPPSteamGenerationAsset SET CompatibleFuel = :compatibleFuel WHERE AssetId = :assetId", nativeQuery = true)
    int updateSteamAssetCompatibleFuel(
            @org.springframework.data.repository.query.Param("assetId") String assetId,
            @org.springframework.data.repository.query.Param("compatibleFuel") String compatibleFuel);

}
