package com.wks.caseengine.db2.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.db2.entity.TurnAroundPlanDB2;

@Repository
public interface TurnAroundPlanReportDB2Repository extends JpaRepository<TurnAroundPlanDB2, UUID> {
}
