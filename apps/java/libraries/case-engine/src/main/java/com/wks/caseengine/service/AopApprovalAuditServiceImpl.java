package com.wks.caseengine.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
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
    public void record(String caseId, String year, UUID plantFkId, String gateName, String gateDisplayName,
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

        auditRepository.save(builder.build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AopApprovalHistoryDTO> getAuditTrail(UUID plantFkId, String year) {
        return auditRepository.findAllByPlantFkIdAndYearOrderByActionAtDesc(plantFkId, year)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AopApprovalHistoryDTO> getMyActions(String actorUserId) {
        return auditRepository.findAllByActorUserIdOrderByActionAtDesc(actorUserId)
                .stream().map(this::toDTO).collect(Collectors.toList());
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
