package com.wks.caseengine.cpp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.cpp.dto.FixedConsumptionProjection;
import com.wks.caseengine.entity.DummyEntity;

@Repository
@Transactional
public interface JMDFixedConsumptionRepository extends JpaRepository<DummyEntity, Long> {

    @Query(value = "EXEC dbo.CPP_GetFixedConsumptionByPlant :plantIds, :financialYear", nativeQuery = true)
    List<FixedConsumptionProjection> getFixedConsumptionForPlants(
            @Param("plantIds") String plantIds,
            @Param("financialYear") String financialYear);
}
