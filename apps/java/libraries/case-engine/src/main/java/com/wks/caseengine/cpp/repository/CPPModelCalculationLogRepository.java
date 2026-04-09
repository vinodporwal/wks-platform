/*
 * WKS Platform - Open-Source Project
 * 
 * This file is part of the WKS Platform, an open-source project developed by WKS Power.
 * 
 * WKS Platform is licensed under the MIT License.
 * 
 * © 2021 WKS Power. All rights reserved.
 * 
 * For licensing information, see the LICENSE file in the root directory of the project.
 */
package com.wks.caseengine.cpp.repository;

import com.wks.caseengine.cpp.entity.CPPModelCalculationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CPP Model Calculation Logs
 */
@Repository
public interface CPPModelCalculationLogRepository extends JpaRepository<CPPModelCalculationLog, UUID> {

    /**
     * Find all parent execution records (full year runs)
     * Parent records have parentExecutionFkId = NULL
     */
    @NativeQuery("SELECT * FROM CPPModelCalculationLogs WITH(NOLOCK) WHERE ParentExecution_FK_Id IS NULL ORDER BY ExecutionDateTime DESC")
    List<CPPModelCalculationLog> findAllParentExecutions();

    /**
     * Find parent executions by financial year
     */
    @NativeQuery("SELECT * FROM CPPModelCalculationLogs WITH(NOLOCK) WHERE ParentExecution_FK_Id IS NULL AND FinancialYear = :financialYear ORDER BY ExecutionDateTime DESC")
    List<CPPModelCalculationLog> findParentExecutionsByFinancialYear(@Param("financialYear") Integer financialYear);

    /**
     * Find parent executions by status
     */
    @NativeQuery("SELECT * FROM CPPModelCalculationLogs WITH(NOLOCK) WHERE ParentExecution_FK_Id IS NULL AND Status = :status ORDER BY ExecutionDateTime DESC")
    List<CPPModelCalculationLog> findParentExecutionsByStatus(@Param("status") String status);

    /**
     * Find all monthly child records for a parent execution
     */
    @NativeQuery("SELECT * FROM CPPModelCalculationLogs WITH(NOLOCK) WHERE ParentExecution_FK_Id = :parentId ORDER BY Month ASC")
    List<CPPModelCalculationLog> findMonthlyLogsByParentId(@Param("parentId") UUID parentId);

    /**
     * Find specific monthly log by parent ID and month
     */
    @NativeQuery("SELECT * FROM CPPModelCalculationLogs WITH(NOLOCK) WHERE ParentExecution_FK_Id = :parentId AND Month = :month")
    Optional<CPPModelCalculationLog> findMonthlyLogByParentIdAndMonth(
        @Param("parentId") UUID parentId, 
        @Param("month") Integer month
    );

    /**
     * Find parent execution by ID (with null check for parentExecutionFkId)
     */
    @NativeQuery("SELECT * FROM CPPModelCalculationLogs WITH(NOLOCK) WHERE Id = :id AND ParentExecution_FK_Id IS NULL")
    Optional<CPPModelCalculationLog> findParentExecutionById(@Param("id") UUID id);

    /**
     * Check if parent execution exists
     */
    @NativeQuery("SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM CPPModelCalculationLogs WITH(NOLOCK) WHERE Id = :id AND ParentExecution_FK_Id IS NULL")
    Integer existsParentExecutionById(@Param("id") UUID id);

    /**
     * Count monthly logs for a parent execution
     */
    @NativeQuery("SELECT COUNT(*) FROM CPPModelCalculationLogs WITH(NOLOCK) WHERE ParentExecution_FK_Id = :parentId")
    long countMonthlyLogsByParentId(@Param("parentId") UUID parentId);

    /**
     * Get latest parent execution
     */
    @NativeQuery("SELECT TOP 1 * FROM CPPModelCalculationLogs WITH(NOLOCK) WHERE ParentExecution_FK_Id IS NULL ORDER BY ExecutionDateTime DESC")
    Optional<CPPModelCalculationLog> findLatestParentExecution();

    /**
     * Find parent executions with filters (financial year, status)
     */
    @NativeQuery("SELECT * FROM CPPModelCalculationLogs WITH(NOLOCK) WHERE ParentExecution_FK_Id IS NULL " +
           "AND (:financialYear IS NULL OR FinancialYear = :financialYear) " +
           "AND (:status IS NULL OR Status = :status) " +
           "ORDER BY ExecutionDateTime DESC")
    List<CPPModelCalculationLog> findParentExecutionsWithFilters(
        @Param("financialYear") Integer financialYear,
        @Param("status") String status
    );
}
