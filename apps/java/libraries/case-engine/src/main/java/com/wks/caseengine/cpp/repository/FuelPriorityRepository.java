package com.wks.caseengine.cpp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

}
