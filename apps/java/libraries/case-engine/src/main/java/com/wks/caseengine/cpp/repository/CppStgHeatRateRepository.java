package com.wks.caseengine.cpp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import com.wks.caseengine.cpp.entity.CppStgHeatRate;
import java.util.List;
import java.util.UUID;


@Repository
public interface CppStgHeatRateRepository extends JpaRepository<CppStgHeatRate, UUID> {

    @Query(value = "SELECT * FROM CPP_STGHeatRate WHERE Asset_FK_Id = :assetFkId AND FinancialYear = :financialYear order by STGLoad", nativeQuery = true)
    List<CppStgHeatRate> findByAssetFkIdAndFinancialYearNative(
        @Param("assetFkId") UUID assetFkId,
        @Param("financialYear") String financialYear
    );

}
