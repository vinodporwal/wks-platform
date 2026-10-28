package com.wks.caseengine.db2.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.db2.entity.PlantContributionDB2;

@Repository
public interface PlantContributionDB2Repository extends JpaRepository<PlantContributionDB2, UUID> {
}
