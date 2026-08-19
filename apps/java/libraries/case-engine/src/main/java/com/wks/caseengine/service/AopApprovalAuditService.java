package com.wks.caseengine.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.wks.caseengine.dto.AopApprovalHistoryDTO;

/**
 * Records and reads the AOP approval audit trail. Scope (site / vertical, ids +
 * names) is resolved from the Plants master at write time — callers pass only
 * the plant id.
 */
public interface AopApprovalAuditService {

    /**
     * Record one gate action. Site and vertical are resolved from the Plants
     * master by {@code plantFkId}.
     *
     * @param action one of SUBMITTED, APPROVED, REVERTED
     * @param toGate where the plan lands as a result. Pass null when the decision
     *        is being recorded before the engine has routed, then fill it in with
     *        {@link #completeToGate}.
     * @return the id of the row just written
     */
    UUID record(String caseId, String year, UUID plantFkId, String gateName, String gateDisplayName,
            Integer sequence, String action, String actorUserId, String actorRole, String remark,
            String fromGate, String toGate);

    /**
     * Fill in {@code toGate} on rows written earlier in the same request.
     *
     * <p>A decision has to be audited <em>before</em> the engine call — the trail
     * must never claim an approval the engine rejected — but at that point where
     * the plan will land is not yet known, least of all at a multi-instance gate
     * where routing waits on every approver. So the destination is resolved from
     * the engine once the tasks are complete and back-filled here. This is the
     * only mutation an audit row ever undergoes, and it only ever moves the field
     * from null to a value.</p>
     */
    void completeToGate(List<UUID> ids, String toGate);

    List<AopApprovalHistoryDTO> getAuditTrail(UUID plantFkId, String year);

    List<AopApprovalHistoryDTO> getMyActions(String actorUserId);

    /**
     * Has this user already acted at this gate during the gate's current visit?
     *
     * <p>This is a user-level history query. Button eligibility is <em>not</em>
     * based on it: a person who already acted as one approver role and is later
     * assigned a different remaining role at the same gate must still be able to
     * act as that role. The plan re-entering the gate (after a revert) starts a
     * fresh visit.</p>
     *
     * @param visitStart when the gate's current tasks were created, i.e. when the
     *        plan entered this gate. Actions recorded after it belong to this
     *        visit; anything earlier is a previous pass. A revert *within* a
     *        multi-instance gate does not start a new visit — the gate only routes
     *        once every instance has completed — so the task creation time is the
     *        correct boundary, not the last revert. Null falls back to "has this
     *        user ever acted at this gate".
     */
    boolean hasActedInCurrentCycle(String caseId, String gateName, String actorUserId,
            OffsetDateTime visitStart);

    /** True after Gate 5 approval ends the Camunda process ({@code toGate = COMPLETED}). */
    boolean hasCompleted(String caseId);

    /** True if GMS Head (Gate 5) has rejected this plan at least once. */
    boolean hasGate5Reverted(String caseId);

    /**
     * Roles that APPROVED or SUBMITTED at this gate during the current visit.
     * Roles still waiting on an open task are not included.
     */
    Set<String> approvedRolesInCurrentVisit(String caseId, String gateName, OffsetDateTime visitStart);

    /**
     * Timestamp of the most recent audit action for this plan, as ISO-8601.
     * Null when the trail is empty.
     */
    String lastActionTakenDate(String caseId);
}
