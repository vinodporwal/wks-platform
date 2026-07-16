package com.wks.caseengine.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.entity.CPPFuelAvailability;

@Repository
public interface FuelAvailabilityRepository extends JpaRepository<CPPFuelAvailability, UUID> {
    
    @Query("SELECT f FROM CPPFuelAvailability f WHERE f.cppId = :cppId AND f.financialYear = :financialYear")
    List<CPPFuelAvailability> findByCppIdAndFinancialYear(
        @Param("cppId") UUID cppId, 
        @Param("financialYear") String financialYear
    );
    
    @Query("SELECT f FROM CPPFuelAvailability f WHERE f.cppId = :cppId AND f.financialYear = :financialYear AND f.fuelCategory = :fuelType")
    List<CPPFuelAvailability> findByCppIdAndFinancialYearAndFuelType(
        @Param("cppId") UUID cppId, 
        @Param("financialYear") String financialYear,
        @Param("fuelType") String fuelType
    );
    
    @Query("SELECT f FROM CPPFuelAvailability f WHERE f.cppId = :cppId AND f.financialYear = :financialYear AND f.fuelName = :fuelName")
    CPPFuelAvailability findByCppIdAndFinancialYearAndFuelName(
        @Param("cppId") UUID cppId, 
        @Param("financialYear") String financialYear,
        @Param("fuelName") String fuelName
    );
}
