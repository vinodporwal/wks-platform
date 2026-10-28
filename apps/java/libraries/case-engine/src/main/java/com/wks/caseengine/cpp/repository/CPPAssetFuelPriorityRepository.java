package com.wks.caseengine.cpp.repository;

import com.wks.caseengine.cpp.dto.AssetFuelPriorityProjection;
import com.wks.caseengine.cpp.entity.CPPAssetFuelPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@Transactional
public interface CPPAssetFuelPriorityRepository extends JpaRepository<CPPAssetFuelPriority, UUID> {

    @Query(value = "EXEC dbo.CPP_GetAssetFuelPriority :plantIds, :financialYear", nativeQuery = true)
    List<AssetFuelPriorityProjection> getAssetFuelPriority(
            @Param("plantIds") String plantIds,
            @Param("financialYear") String financialYear);

}
