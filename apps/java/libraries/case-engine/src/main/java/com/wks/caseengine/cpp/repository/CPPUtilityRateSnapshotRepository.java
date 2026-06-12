package com.wks.caseengine.cpp.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.cpp.entity.CPPUtilityRateSnapshot;

@Repository
public interface CPPUtilityRateSnapshotRepository
        extends JpaRepository<CPPUtilityRateSnapshot, UUID> {

    /**
     * Fetch all utility rate snapshots for a given CPP plant and financial year,
     * ordered by plant name and utility name for consistent display.
     */
    @Query("SELECT s FROM CPPUtilityRateSnapshot s " +
           "WHERE s.cppPlantId = :cppPlantId " +
           "  AND s.financialYear = :financialYear " +
           "ORDER BY s.plantName, s.utilityName")
    List<CPPUtilityRateSnapshot> findByCppPlantIdAndFinancialYear(
            @Param("cppPlantId")    UUID   cppPlantId,
            @Param("financialYear") String financialYear);
}
