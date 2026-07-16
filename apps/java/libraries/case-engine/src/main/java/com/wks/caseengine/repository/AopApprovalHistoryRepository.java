package com.wks.caseengine.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.entity.AopApprovalHistory;

@Repository
public interface AopApprovalHistoryRepository extends JpaRepository<AopApprovalHistory, UUID> {

    /** Full audit trail for a plant + year, most recent action first. */
    List<AopApprovalHistory> findAllByPlantFkIdAndYearOrderByActionAtDesc(UUID plantFkId, String year);

    /** Full audit trail for a workflow instance, most recent action first. */
    List<AopApprovalHistory> findAllByCaseIdOrderByActionAtDesc(String caseId);

    /** Actions taken by a specific user, most recent first (for "my actions"). */
    List<AopApprovalHistory> findAllByActorUserIdOrderByActionAtDesc(String actorUserId);
}
