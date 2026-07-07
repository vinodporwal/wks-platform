package com.wks.caseengine.cpp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.cpp.entity.CppGtHeatRate;
import com.wks.caseengine.cpp.entity.CppHrsgHeatRate;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface CppHrsgHeatRateRepository extends JpaRepository<CppHrsgHeatRate, UUID> {

    List<CppHrsgHeatRate> findByAssetFkId(UUID assetFkId);

    List<CppHrsgHeatRate> findByAssetFkIdAndFinancialYear(UUID assetFkId, String financialYear);

    List<CppHrsgHeatRate> findByAssetFkIdInAndFinancialYear(List<UUID> assetFkIds, String financialYear);
    
    @Query(value = "SELECT * FROM CPP_HRSGHeatRate WHERE Asset_FK_Id = :assetFkId AND FinancialYear = :financialYear", nativeQuery = true)
    List<CppHrsgHeatRate> findByAssetFkIdAndFinancialYearNative(
        @Param("assetFkId") UUID assetFkId, 
        @Param("financialYear") String financialYear
    );
    
    @Query(value = "EXEC [dbo].[CPP_CalculateCommonHRSGHeatRate_ByDateRange] " +
            "@StartDate = :startDate, " +
            "@EndDate = :endDate, " +
            "@AssetName = :assetName, " +
            "@PlantIds = :plantIds", nativeQuery = true)
    List<Object[]> executeCalculateCommonHRSGHeatRateSP(
     @Param("startDate") String startDate,
     @Param("endDate") String endDate,
     @Param("assetName") String assetName,
     @Param("plantIds") String plantIds);
    
   
}