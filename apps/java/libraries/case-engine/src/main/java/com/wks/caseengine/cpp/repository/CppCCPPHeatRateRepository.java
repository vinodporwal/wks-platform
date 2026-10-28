package com.wks.caseengine.cpp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.cpp.entity.CppCCPPHeatRate;

import java.util.List;
import java.util.UUID;


@Repository
public interface CppCCPPHeatRateRepository extends JpaRepository<CppCCPPHeatRate, UUID> {

	@Query(value = "SELECT * FROM CPP_CCPPHeatRate WHERE Asset_FK_Id = :assetFkId AND FinancialYear = :financialYear order by CCPPLoad", nativeQuery = true)
    List<CppCCPPHeatRate> findByAssetFkIdAndFinancialYearNative(
        @Param("assetFkId") UUID assetFkId, 
        @Param("financialYear") String financialYear
    );
    
    @Query(value = "EXEC [dbo].[CPP_CalculateCommonCCPPHeatRate_ByDateRange] " +
            "@StartDate = :startDate, " +
            "@EndDate = :endDate, " +
            "@AssetId = :assetId, " +
            "@PlantIds = :plantIds", nativeQuery = true)
    List<Object[]> executeCalculateCommonCCPPHeatRateSP(
     @Param("startDate") String startDate,
     @Param("endDate") String endDate,
     @Param("assetId") UUID assetId,
     @Param("plantIds") String plantIds);
    
    
}
