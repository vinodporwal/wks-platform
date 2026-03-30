package com.wks.caseengine.db2.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.wks.caseengine.db2.entity.ShutdownSummaryLastFourYear;

@Repository
public interface ShutdownSummaryLastFourYearRepository extends JpaRepository<ShutdownSummaryLastFourYear, UUID> {

    List<ShutdownSummaryLastFourYear> findByPlantFkIdAndYear(UUID plantFkId, String year);
}

