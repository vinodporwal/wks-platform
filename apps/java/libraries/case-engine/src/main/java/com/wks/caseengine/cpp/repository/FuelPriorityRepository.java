package com.wks.caseengine.cpp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
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

}
