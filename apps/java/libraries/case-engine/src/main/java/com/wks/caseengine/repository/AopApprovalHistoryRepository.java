package com.wks.caseengine.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.entity.AopApprovalHistory;

@Repository
public interface AopApprovalHistoryRepository extends JpaRepository<AopApprovalHistory, UUID> {

    /** Full audit trail for a plant + year, oldest action first. */
    List<AopApprovalHistory> findAllByPlantFkIdAndYearOrderByActionAtAsc(UUID plantFkId, String year);

    /** Full audit trail for a workflow instance, most recent action first. */
    List<AopApprovalHistory> findAllByCaseIdOrderByActionAtDesc(String caseId);

    /** Actions taken by a specific user, most recent first (for "my actions"). */
    List<AopApprovalHistory> findAllByActorUserIdOrderByActionAtDesc(String actorUserId);

    /**
     * Most recent action that (re)started a pass through the gates — a submit or
     * any revert. Everything recorded after it belongs to the current cycle.
     */
    Optional<AopApprovalHistory> findTopByCaseIdAndActionInOrderByActionAtDesc(
            String caseId, List<String> actions);

    /** Has this user acted at this gate since the given cycle boundary? */
    boolean existsByCaseIdAndGateNameAndActorUserIdAndActionAtAfter(
            String caseId, String gateName, String actorUserId, OffsetDateTime after);

    /** Has this user ever acted at this gate (used when there is no boundary yet)? */
    boolean existsByCaseIdAndGateNameAndActorUserId(
            String caseId, String gateName, String actorUserId);

    /** All actions at a gate (used when there is no current-visit boundary yet). */
    List<AopApprovalHistory> findAllByCaseIdAndGateName(String caseId, String gateName);

    /** Actions at a gate after the current visit started. */
    List<AopApprovalHistory> findAllByCaseIdAndGateNameAndActionAtAfter(
            String caseId, String gateName, OffsetDateTime after);

    /** True once Gate 5 approval has finished the process ({@code toGate = COMPLETED}). */
    boolean existsByCaseIdAndToGate(String caseId, String toGate);

    /** Most recent audit row for this workflow, if any. */
    Optional<AopApprovalHistory> findTopByCaseIdOrderByActionAtDesc(String caseId);
}
