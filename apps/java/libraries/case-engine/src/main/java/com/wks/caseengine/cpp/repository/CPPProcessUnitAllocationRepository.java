package com.wks.caseengine.cpp.repository;

import com.wks.caseengine.cpp.dto.CPPProcessUnitAllocationProjection;
import com.wks.caseengine.cpp.entity.CPPProcessUnitAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CPPProcessUnitAllocationRepository extends JpaRepository<CPPProcessUnitAllocation, UUID> {

    /**
     * Calls stored procedure dbo.CPP_GetProcessUnitAllocation which returns
     * enriched allocation rows with display labels and source monthly values.
     *
     * @param plantIds comma-separated UUID string, e.g. "uuid1,uuid2"
     * @param aopYear  financial year, e.g. "2026-27"
     */
    @Query(value = "EXEC [dbo].[CPP_GetProcessUnitAllocation] @PlantIds = :plantIds, @FinancialYear = :aopYear",
           nativeQuery = true)
    List<CPPProcessUnitAllocationProjection> findByPlantIdsAndAopYear(
            @Param("plantIds") String plantIds,
            @Param("aopYear") String aopYear);


    /**
     * Check whether a record already exists for the same
     * CPPPlant + ImportPower source + ProcessPlantName combination in a given year.
     * Used to prevent duplicate allocations on INSERT.
     */
    @Query(value = """
        SELECT CASE WHEN COUNT(1) > 0 THEN 1 ELSE 0 END
        FROM [dbo].[CPPProcessUnitAllocation] WITH(NOLOCK)
        WHERE CPPPlant_FK_ID    = :cppPlantId
          AND ImportPower_FK_ID = :importPowerId
          AND Process_Plant_Name  = :processPlantName
          AND AOPYear            = :aopYear
        """, nativeQuery = true)
    int existsAllocation(
            @Param("cppPlantId")      UUID cppPlantId,
            @Param("importPowerId")   UUID importPowerId,
            @Param("processPlantName") String processPlantName,
            @Param("aopYear")         String aopYear);


    /**
     * Hard-delete a single allocation by its primary key.
     * Called by the DELETE endpoint.
     */
    @Modifying
    @Query(value = """
        DELETE FROM [dbo].[CPPProcessUnitAllocation]
        WHERE Id = :id
        """, nativeQuery = true)
    void deleteAllocationById(@Param("id") UUID id);
}
