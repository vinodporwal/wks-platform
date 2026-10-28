package com.wks.caseengine.db2.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.db2.entity.AnnualProductionPlanReportDB2;

@Repository
public interface AnnualProductionPlanReportDB2Repository extends JpaRepository<AnnualProductionPlanReportDB2, UUID> {

    @Query(value = "SELECT TOP (1) RowNo FROM AnnualProductionPlanReport where ReportType = 'assumptions' and AOPYear= :year and Plant_FK_Id = :plantId  ORDER BY RowNo DESC", nativeQuery = true)
    Integer findLatestRowNo(@Param("year") String year, @Param("plantId") String plantId);
}
