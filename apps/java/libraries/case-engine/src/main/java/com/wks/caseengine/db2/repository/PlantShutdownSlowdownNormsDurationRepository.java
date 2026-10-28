package com.wks.caseengine.db2.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.db2.entity.PlantShutdownSlowdownNormsDuration;

@Repository
public interface PlantShutdownSlowdownNormsDurationRepository extends JpaRepository<PlantShutdownSlowdownNormsDuration, UUID> {

    List<PlantShutdownSlowdownNormsDuration> findByPlantIdAndYear(UUID plantId, String year);
}

