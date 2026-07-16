package com.wks.caseengine.service;

import java.util.List;
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
     * master by {@code plantFkId}; the row is immutable once written.
     *
     * @param action one of SUBMITTED, APPROVED, REVERTED
     */
    void record(String caseId, String year, UUID plantFkId, String gateName, String gateDisplayName,
            Integer sequence, String action, String actorUserId, String actorRole, String remark,
            String fromGate, String toGate);

    List<AopApprovalHistoryDTO> getAuditTrail(UUID plantFkId, String year);

    List<AopApprovalHistoryDTO> getMyActions(String actorUserId);
}
