package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.AopPendingItemDTO;
import com.wks.caseengine.dto.AopWorkflowStatusDTO;
import com.wks.caseengine.dto.WorkflowDTO;

/**
 * Orchestrates the isolated AOP approval workflow ({@code AOP_Approval_v2}).
 *
 * <p>Starts the Camunda process directly (injecting each gate's approver roles
 * from WorkflowStepRoles), and on every gate decision writes the audit trail and
 * emails the newly-active gate's approvers. Keeps the shared
 * {@code submitWorkflow}/{@code completeTaskWithComment} paths — and TCS —
 * untouched.</p>
 */
public interface AopApprovalWorkflowService {

    /**
     * Start a new AOP approval workflow for a plant + year. Site and vertical are
     * resolved from the Plants master. Fails with a 409 if an active workflow
     * already exists for the plant + year.
     *
     * @param actorUserId the submitting user (from the JWT)
     */
    WorkflowDTO start(String plantId, String year, String actorUserId);

    /**
     * Same as {@link #start(String, String, String)} but persists the preparer's
     * remark and acting role on the SUBMITTED audit row.
     */
    WorkflowDTO start(String plantId, String year, String actorUserId, String remark, String actorRole);

    /**
     * Apply a gate decision: record the audit rows, complete the Camunda task(s)
     * with the decision, and notify the next gate's approvers.
     *
     * <p>Approve completes only the selected role's task. A user who holds several
     * roles at the same gate (common at Gate 2) must approve/reject once per role.
     * Revert still leaves the gate for everyone (any one reverter wins).</p>
     *
     * @param decision APPROVED or REVERTED
     * @param callerRoles the caller's realm roles, used to authorize the selected task
     */
    void act(String taskId, String plantId, String year, String gateName, String decision,
            String remark, String actorUserId, String actorRole, List<String> callerRoles);

    /**
     * Status + server-computed button state for a single (plant, year). The
     * viewer block is derived from the caller's roles vs. the active task's
     * assignee role — never trusted from the client.
     */
    AopWorkflowStatusDTO getStatus(String plantId, String year, String callerUserId, List<String> callerRoles);

    /**
     * Every active AOP workflow currently pending on one of the caller's roles —
     * the "My Approvals" inbox, keyed by (plant, year) across all plants.
     */
    List<AopPendingItemDTO> getMyPending(String callerUserId, List<String> callerRoles);
}
