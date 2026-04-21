package com.wks.caseengine.db2.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import com.wks.caseengine.db2.entity.RoutineShutdownDetails;

@Repository
public interface RoutineShutdownDetailsRepository extends JpaRepository<RoutineShutdownDetails, UUID> {

    List<RoutineShutdownDetails> findByPlantFkIdAndYear(UUID plantFkId, String year);
}

