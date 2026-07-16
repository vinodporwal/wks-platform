package com.wks.caseengine.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.entity.WorkflowStepRoles;

@Repository
public interface WorkflowStepRolesRepository extends JpaRepository<WorkflowStepRoles, UUID> {

    /** Active approver roles for a single gate, by WorkflowStepsMaster id. */
    List<WorkflowStepRoles> findAllByWorkflowStepFKIdAndIsActiveTrue(UUID workflowStepFKId);

    /**
     * Active roles for a gate identified by its step Name within a workflow
     * definition. Used by the runtime to load the multi-instance role
     * collection for a gate (e.g. gate1..gate5) without hardcoding roles.
     */
    @Query(value = "SELECT wsr.Role "
            + "FROM dbo.WorkflowStepRoles wsr "
            + "INNER JOIN dbo.WorkflowStepsMaster wsm ON wsm.Id = wsr.WorkflowStep_FK_Id "
            + "WHERE wsm.WorkflowMaster_FK_Id = :workflowMasterId "
            + "AND wsm.Name = :stepName "
            + "AND wsr.isActive = 1 "
            + "ORDER BY wsr.Role",
            nativeQuery = true)
    List<String> findActiveRolesByWorkflowMasterAndStepName(
            @Param("workflowMasterId") UUID workflowMasterId,
            @Param("stepName") String stepName);
}
