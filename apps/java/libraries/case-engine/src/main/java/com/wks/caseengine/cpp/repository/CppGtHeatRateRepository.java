package com.wks.caseengine.cpp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.cpp.entity.CppGtHeatRate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;


@Repository
public interface CppGtHeatRateRepository extends JpaRepository<CppGtHeatRate, UUID> {

    List<CppGtHeatRate> findByAssetFkId(UUID assetFkId);

    List<CppGtHeatRate> findByAssetFkIdAndFinancialYear(UUID assetFkId, String financialYear);

    List<CppGtHeatRate> findByAssetFkIdInAndFinancialYear(List<UUID> assetFkIds, String financialYear);
    
    @Query(value = "SELECT * FROM CPP_GTHeatRate WHERE Asset_FK_Id = :assetFkId AND FinancialYear = :financialYear", nativeQuery = true)
    List<CppGtHeatRate> findByAssetFkIdAndFinancialYearNative(
        @Param("assetFkId") UUID assetFkId, 
        @Param("financialYear") String financialYear
    );
}