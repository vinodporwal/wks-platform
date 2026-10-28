package com.wks.caseengine.db2.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.db2.entity.MonthwiseConsumptionReportDB2;

@Repository
public interface MonthwiseConsumptionReportDB2Repository extends JpaRepository<MonthwiseConsumptionReportDB2, UUID> {
}
