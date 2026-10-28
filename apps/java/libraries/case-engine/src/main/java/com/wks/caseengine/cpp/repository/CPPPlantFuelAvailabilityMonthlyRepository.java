package com.wks.caseengine.cpp.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.cpp.dto.PlantFuelAvailabilityMonthlyProjection;
import com.wks.caseengine.cpp.entity.CPPPlantFuelAvailabilityMonthly;

@Repository
@Transactional
public interface CPPPlantFuelAvailabilityMonthlyRepository extends JpaRepository<CPPPlantFuelAvailabilityMonthly, UUID> {

    @Query(value = "EXEC dbo.CPP_GetPlantFuelAvailabilityMonthly :plantIds, :financialYear", nativeQuery = true)
    List<PlantFuelAvailabilityMonthlyProjection> getPlantFuelAvailabilityMonthly(
            @Param("plantIds") String plantIds,
            @Param("financialYear") String financialYear);
}
