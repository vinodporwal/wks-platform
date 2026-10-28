package com.wks.caseengine.db2.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.db2.entity.PlantContributionSummaryBusinessDemandBasisDB2;

@Repository
public interface PlantContributionSummaryBusinessDemandBasisDB2Repository
        extends JpaRepository<PlantContributionSummaryBusinessDemandBasisDB2, UUID> {
}
