package com.wks.caseengine.cpp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.cpp.entity.CppAuxBoilerHeatRate;

import java.util.List;
import java.util.UUID;


@Repository
public interface CppAuxBoilerHeatRateRepository extends JpaRepository<CppAuxBoilerHeatRate, UUID> {

	@Query(value = "SELECT * FROM CPP_AUXBoilerHeatRate WHERE Asset_FK_Id = :assetFkId AND FinancialYear = :financialYear", nativeQuery = true)
    List<CppAuxBoilerHeatRate> findByAssetFkIdAndFinancialYearNative(
        @Param("assetFkId") UUID assetFkId, 
        @Param("financialYear") String financialYear
    );
    
    @Query(value = "EXEC [dbo].[CPP_CalculateCommonAUXBoilerHeatRate_ByDateRange] " +
            "@StartDate = :startDate, " +
            "@EndDate = :endDate, " +
            "@AssetId = :assetId, " +
            "@PlantIds = :plantIds", nativeQuery = true)
    List<Object[]> executeCalculateCommonAUXBoilerHeatRateSP(
     @Param("startDate") String startDate,
     @Param("endDate") String endDate,
     @Param("assetId") UUID assetId,
     @Param("plantIds") String plantIds);
    
    
}
