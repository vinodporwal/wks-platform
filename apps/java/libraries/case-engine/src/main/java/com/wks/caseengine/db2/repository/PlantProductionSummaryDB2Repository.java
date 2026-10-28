package com.wks.caseengine.db2.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.db2.entity.PlantProductionSummaryDB2;

@Repository
public interface PlantProductionSummaryDB2Repository extends JpaRepository<PlantProductionSummaryDB2, UUID> {
}
