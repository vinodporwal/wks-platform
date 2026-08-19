package com.wks.caseengine.service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.dto.AopApprovalHistoryDTO;
import com.wks.caseengine.entity.AopApprovalHistory;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.repository.AopApprovalHistoryRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;

@Service
public class AopApprovalAuditServiceImpl implements AopApprovalAuditService {

    @Autowired
    private AopApprovalHistoryRepository auditRepository;

    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private VerticalsRepository verticalRepository;

    @Override
    @Transactional
    public UUID record(String caseId, String year, UUID plantFkId, String gateName, String gateDisplayName,
            Integer sequence, String action, String actorUserId, String actorRole, String remark,
            String fromGate, String toGate) {

        AopApprovalHistory.AopApprovalHistoryBuilder builder = AopApprovalHistory.builder()
                .caseId(caseId)
                .year(year)
                .plantFkId(plantFkId)
                .gateName(gateName)
                .gateDisplayName(gateDisplayName)
                .sequence(sequence)
                .action(action)
                .actorUserId(actorUserId)
                .actorRole(actorRole)
                .remark(remark)
                .fromGate(fromGate)
                .toGate(toGate)
                .actionAt(OffsetDateTime.now());

        // Resolve site + vertical from the Plants master (single source of truth).
        if (plantFkId != null) {
            Plants plant = plantsRepository.findById(plantFkId).orElse(null);
            if (plant != null) {
                builder.plantName(plant.getName())
                        .siteFkId(plant.getSiteFkId())
                        .verticalFkId(plant.getVerticalFKId());

                if (plant.getSiteFkId() != null) {
                    Optional<Sites> site = siteRepository.findById(plant.getSiteFkId());
                    site.ifPresent(s -> builder.siteName(s.getName()));
                }
                if (plant.getVerticalFKId() != null) {
                    Optional<Verticals> vertical = verticalRepository.findById(plant.getVerticalFKId());
                    vertical.ifPresent(v -> builder.verticalName(v.getName()));
                }
            }
        }

        return auditRepository.save(builder.build()).getId();
    }

    @Override
    @Transactional
    public void completeToGate(List<UUID> ids, String toGate) {
        if (ids == null || ids.isEmpty() || toGate == null) {
            return;
        }
        for (AopApprovalHistory row : auditRepository.findAllById(ids)) {
            if (row.getToGate() == null) {
                row.setToGate(toGate);
            }
        }
        // Dirty-checked on flush; ids come from rows written earlier in this
        // transaction, so they are already managed.
    }

    @Override
    @Transactional(readOnly = true)
    public List<AopApprovalHistoryDTO> getAuditTrail(UUID plantFkId, String year) {
        return auditRepository.findAllByPlantFkIdAndYearOrderByActionAtAsc(plantFkId, year)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AopApprovalHistoryDTO> getMyActions(String actorUserId) {
        return auditRepository.findAllByActorUserIdOrderByActionAtDesc(actorUserId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActedInCurrentCycle(String caseId, String gateName, String actorUserId,
            OffsetDateTime visitStart) {
        if (caseId == null || gateName == null || actorUserId == null) {
            return false;
        }
        if (visitStart == null) {
            return auditRepository
                    .existsByCaseIdAndGateNameAndActorUserId(caseId, gateName, actorUserId);
        }
        return auditRepository
                .existsByCaseIdAndGateNameAndActorUserIdAndActionAtAfter(caseId, gateName, actorUserId, visitStart);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasCompleted(String caseId) {
        return caseId != null && auditRepository.existsByCaseIdAndToGate(caseId, "COMPLETED");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasGate5Reverted(String caseId) {
        if (caseId == null) {
            return false;
        }
        // Newest first: a Gate 5 reject arms skip until this plan is fully approved.
        // An older completed cycle on the same case id must not arm a new submit.
        List<AopApprovalHistory> trail = auditRepository.findAllByCaseIdOrderByActionAtDesc(caseId);
        if (trail == null || trail.isEmpty()) {
            return false;
        }
        for (AopApprovalHistory row : trail) {
            if (row == null) {
                continue;
            }
            if ("COMPLETED".equals(row.getToGate())) {
                return false;
            }
            if ("gate5".equals(row.getGateName()) && "REVERTED".equals(row.getAction())) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> approvedRolesInCurrentVisit(String caseId, String gateName, OffsetDateTime visitStart) {
        if (caseId == null || gateName == null) {
            return Collections.emptySet();
        }
        List<AopApprovalHistory> rows = visitStart == null
                ? auditRepository.findAllByCaseIdAndGateName(caseId, gateName)
                : auditRepository.findAllByCaseIdAndGateNameAndActionAtAfter(caseId, gateName, visitStart);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> approved = new HashSet<>();
        for (AopApprovalHistory row : rows) {
            if (row.getActorRole() == null || row.getActorRole().isBlank()) {
                continue;
            }
            String action = row.getAction();
            if ("APPROVED".equals(action) || "SUBMITTED".equals(action)) {
                approved.add(row.getActorRole());
            }
        }
        return approved;
    }

    private AopApprovalHistoryDTO toDTO(AopApprovalHistory h) {
        return AopApprovalHistoryDTO.builder()
                .id(h.getId() != null ? h.getId().toString() : null)
                .caseId(h.getCaseId())
                .year(h.getYear())
                .plantFkId(h.getPlantFkId() != null ? h.getPlantFkId().toString() : null)
                .plantName(h.getPlantName())
                .siteFkId(h.getSiteFkId() != null ? h.getSiteFkId().toString() : null)
                .siteName(h.getSiteName())
                .verticalFkId(h.getVerticalFkId() != null ? h.getVerticalFkId().toString() : null)
                .verticalName(h.getVerticalName())
                .gateName(h.getGateName())
                .gateDisplayName(h.getGateDisplayName())
                .sequence(h.getSequence())
                .action(h.getAction())
                .actorUserId(h.getActorUserId())
                .actorRole(h.getActorRole())
                .remark(h.getRemark())
                .fromGate(h.getFromGate())
                .toGate(h.getToGate())
                .actionAt(h.getActionAt() != null ? h.getActionAt().toString() : null)
                .build();
    }
}
