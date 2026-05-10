package com.wks.caseengine.tcs.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wks.caseengine.tcs.dto.PCGOutlookProjection;
import com.wks.caseengine.entity.DummyEntity;
import com.wks.caseengine.entity.PCGOutlookDataEntity;

import jakarta.transaction.Transactional;

@org.springframework.stereotype.Repository
@Transactional
public interface PCGOutlookRepository extends JpaRepository<DummyEntity, Long> {

    @Query(
        value = """
            EXEC Get_TCS_PCGOutlook
               @verticalId = :verticalId,
                @Site_FK_Id = :siteId,
                @FinancialYear = :financialYear
        """,
        nativeQuery = true
    )
    List<PCGOutlookProjection> getPcgOutlookByVerticalIdAndSiteAndFY(
        @Param("verticalId") UUID verticalId,
            @Param("siteId") UUID siteId,
            @Param("financialYear") String financialYear
    );

    @Query("SELECT e FROM PCGOutlookDataEntity e WHERE e.verticalId = :verticalId AND e.siteId = :siteId AND e.financialYear = :financialYear")
    List<PCGOutlookDataEntity> getPcgOutlookData(
        @Param("verticalId") UUID verticalId,
        @Param("siteId") UUID siteId,
        @Param("financialYear") String financialYear
    );
 
    @Query(
        value = """
            select FinancialYearMonthId from TCS_PCGOutlook where Vertical_FK_Id = :verticalId and Site_FK_Id = :siteId and FinancialYearMonthId in ( :financialYearMonthIds )
        """,
        nativeQuery = true
    )
    List<UUID> getPcgOutlookFinancialYearMonthIdsByVerticalIdAndSiteAndFY(
        @Param("verticalId") UUID verticalId,
        @Param("siteId") UUID siteId,
        @Param("financialYearMonthIds") List<UUID> financialYearMonthIds
    );
    
 
}


