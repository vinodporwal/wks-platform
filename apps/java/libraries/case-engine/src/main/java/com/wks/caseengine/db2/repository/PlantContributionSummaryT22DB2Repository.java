package com.wks.caseengine.db2.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.db2.entity.PlantContributionSummaryT22DB2;

@Repository
public interface PlantContributionSummaryT22DB2Repository extends JpaRepository<PlantContributionSummaryT22DB2, UUID> {
}
