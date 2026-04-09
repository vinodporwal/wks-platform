package com.wks.caseengine.tcs.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;

import com.wks.caseengine.tcs.dto.PCGOutlookProjection;
import com.wks.caseengine.entity.DummyEntity;

import jakarta.transaction.Transactional;

@org.springframework.stereotype.Repository
@Transactional
public interface PCGOutlookRepository extends JpaRepository<DummyEntity, Long> {

    @NativeQuery("""
            EXEC Get_TCS_PCGOutlook
                @Site_FK_Id = :siteId,
                @FinancialYear = :financialYear
        """)
    List<PCGOutlookProjection> getPcgOutlookBySiteAndFY(
            @Param("siteId") UUID siteId,
            @Param("financialYear") String financialYear
    );
 
    @NativeQuery("""
            select FinancialYearMonthId from TCS_PCGOutlook where Site_FK_Id = :siteId and FinancialYearMonthId in ( :financialYearMonthIds )
        """)
    List<UUID> getPcgOutlookFinancialYearMonthIdsBySiteAndFY(
        @Param("siteId") UUID siteId,
        @Param("financialYearMonthIds") List<UUID> financialYearMonthIds
    );
    
 
}


