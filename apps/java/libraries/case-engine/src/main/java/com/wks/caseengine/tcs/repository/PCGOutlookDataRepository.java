package com.wks.caseengine.tcs.repository;

import com.wks.caseengine.entity.PCGOutlookDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PCGOutlookDataRepository extends JpaRepository<PCGOutlookDataEntity, UUID> {

    @Query("SELECT e FROM PCGOutlookDataEntity e WHERE e.verticalId = :verticalId AND e.siteId = :siteId AND e.financialYear = :financialYear")
    List<PCGOutlookDataEntity> getPcgOutlookData(
        @Param("verticalId") UUID verticalId,
        @Param("siteId") UUID siteId,
        @Param("financialYear") String financialYear
    );
}
