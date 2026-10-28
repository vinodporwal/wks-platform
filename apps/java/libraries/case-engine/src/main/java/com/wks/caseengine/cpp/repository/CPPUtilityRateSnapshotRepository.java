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

    /**
     * Fetch all utility rate snapshots for a set of CPP plants (JMD multiplant)
     * and a financial year, JOINed with the Plants table to resolve the parent
     * CPP plant name. Ordered by plant name and utility name.
     *
     * Result row column order (Object[]):
     *   0: Id, 1: CPPPlantId, 2: FinancialYear, 3: PlantName, 4: SiteDescription,
     *   5: PlantCode, 6: UtilityName, 7: UtilityId, 8: UOM,
     *   9..20: Apr_Price .. Mar_Price,
     *  21: WeightedAvgPrice, 22: CppPlantName (from Plants.Name)
     */
    @Query(value = "SELECT s.Id, s.CPPPlantId, s.FinancialYear, s.PlantName, s.SiteDescription, " +
           "       s.PlantCode, s.UtilityName, s.UtilityId, s.UOM, " +
           "       s.Apr_Price, s.May_Price, s.Jun_Price, s.Jul_Price, s.Aug_Price, s.Sep_Price, " +
           "       s.Oct_Price, s.Nov_Price, s.Dec_Price, s.Jan_Price, s.Feb_Price, s.Mar_Price, " +
           "       s.WeightedAvgPrice, p.Name AS CppPlantName " +
           "FROM dbo.CPPUtilityRateSnapshot s " +
           "JOIN dbo.Plants p ON s.CPPPlantId = p.Id " +
           "WHERE s.CPPPlantId IN (:cppPlantIds) " +
           "  AND s.FinancialYear = :financialYear " +
           "ORDER BY s.PlantName, s.UtilityName",
           nativeQuery = true)
    List<Object[]> findByCppPlantIdInAndFinancialYear(
            @Param("cppPlantIds")   List<UUID> cppPlantIds,
            @Param("financialYear") String    financialYear);
}
