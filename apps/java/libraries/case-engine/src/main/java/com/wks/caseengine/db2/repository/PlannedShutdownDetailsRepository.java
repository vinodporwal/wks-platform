package com.wks.caseengine.db2.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.db2.entity.PlannedShutdownDetails;

@Repository
public interface PlannedShutdownDetailsRepository extends JpaRepository<PlannedShutdownDetails, UUID> {

    List<PlannedShutdownDetails> findByPlantFkIdAndYear(UUID plantFkId, String year);
}

