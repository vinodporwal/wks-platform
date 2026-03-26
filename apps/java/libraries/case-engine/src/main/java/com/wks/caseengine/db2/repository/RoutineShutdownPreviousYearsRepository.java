package com.wks.caseengine.db2.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.wks.caseengine.db2.entity.RoutineShutdownPreviousYears;

@Repository
public interface RoutineShutdownPreviousYearsRepository extends JpaRepository<RoutineShutdownPreviousYears, UUID> {

    List<RoutineShutdownPreviousYears> findByPlantFkIdAndYear(UUID plantFkId, String year);
}

