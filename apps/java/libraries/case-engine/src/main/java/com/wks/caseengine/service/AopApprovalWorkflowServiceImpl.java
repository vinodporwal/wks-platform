package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.bpm.engine.model.spi.ProcessInstance;
import com.wks.bpm.engine.model.spi.ProcessVariable;
import com.wks.bpm.engine.model.spi.ProcessVariableType;
import com.wks.bpm.engine.model.spi.Task;
import com.wks.caseengine.dto.AopPendingItemDTO;
import com.wks.caseengine.dto.AopViewerDTO;
import com.wks.caseengine.dto.AopWorkflowStatusDTO;
import com.wks.caseengine.dto.WorkflowDTO;
import com.wks.caseengine.dto.WorkflowStepsMasterDTO;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.entity.Workflow;
import com.wks.caseengine.entity.WorkflowMaster;
import com.wks.caseengine.exception.WorkflowConflictException;
import com.wks.caseengine.process.instance.ProcessInstanceService;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.repository.WorkflowMasterRepository;
import com.wks.caseengine.repository.WorkflowRepository;
import com.wks.caseengine.repository.WorkflowStepRolesRepository;
import com.wks.caseengine.repository.WorkflowStepsMasterRepository;
import com.wks.caseengine.tasks.TaskService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AopApprovalWorkflowServiceImpl implements AopApprovalWorkflowService {

    private static final String PROCESS_KEY = "AOP_Approval_v2";
    private static final String CASE_DEF_ID = "AOP_Approval_v2";
    private static final String PREPARE = "prepare";
    private static final String PREPARE_REWORK = "prepareRework";
    private static final List<String> GATES = List.of("gate1", "gate2", "gate3", "gate4", "gate5");
    private static final String APPROVED = "APPROVED";
    private static final String REVERTED = "REVERTED";
    private static final String SUBMITTED = "SUBMITTED";

    @Autowired
    private ProcessInstanceService processInstanceService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private WorkflowRepository workflowRepository;
    @Autowired
    private WorkflowMasterRepository workflowMasterRepository;
    @Autowired
    private WorkflowStepsMasterRepository workflowStepsMasterRepository;
    @Autowired
    private WorkflowStepRolesRepository workflowStepRolesRepository;
    @Autowired
    private PlantsRepository plantsRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private VerticalsRepository verticalRepository;
    @Autowired
    private AopApprovalAuditService auditService;
    @Autowired
    private AopWorkflowNotificationService notificationService;

    @Override
    @Transactional
    public WorkflowDTO start(String plantId, String year, String actorUserId) {
        UUID plantUuid = UUID.fromString(plantId);
        Plants plant = plantsRepository.findById(plantUuid)
                .orElseThrow(() -> new IllegalArgumentException("Unknown plant: " + plantId));

        // Single active workflow per (plant, year).
        if (!workflowRepository.findAllByYearAndPlantFKIdAndIsDeletedFalse(year, plantUuid).isEmpty()) {
            throw new WorkflowConflictException(
                    "An AOP workflow already exists for plant " + plantId + " and year " + year);
        }

        UUID masterId = resolveWorkflowMasterId(plant.getVerticalFKId());
        Map<String, StepMeta> steps = loadStepMeta(masterId);

        // Inject each gate's approver roles as comma-delimited process variables.
        List<ProcessVariable> vars = new ArrayList<>();
        vars.add(strVar("prepareRoles", requiredRoles(masterId, PREPARE)));
        for (String gate : GATES) {
            vars.add(strVar(gate + "Roles", requiredRoles(masterId, gate)));
        }
        vars.add(strVar("plantId", plantId));
        vars.add(strVar("year", year));

        String businessKey = businessKey(plantId, year);
        ProcessInstance pi = processInstanceService.start(PROCESS_KEY, Optional.of(businessKey), vars);
        String processInstanceId = pi != null ? pi.getId() : null;

        Workflow wf = Workflow.builder()
                .caseDefId(CASE_DEF_ID)
                .caseId(businessKey)
                .plantFKId(plantUuid)
                .siteFKId(plant.getSiteFkId())
                .verticalFKId(plant.getVerticalFKId())
                .year(year)
                .processInstanceId(processInstanceId)
                .isDeleted(Boolean.FALSE)
                .build();
        workflowRepository.save(wf);

        StepMeta prep = steps.get(PREPARE);
        auditService.record(businessKey, year, plantUuid, PREPARE,
                prep != null ? prep.displayName : null, prep != null ? prep.sequence : null,
                SUBMITTED, actorUserId, null, null, PREPARE, "gate1");

        notifyGate(masterId, "gate1", steps, plant, year, businessKey,
                "AOP submitted – Gate 1 approval required");

        return WorkflowDTO.builder()
                .caseId(businessKey)
                .plantFkId(plantId)
                .year(year)
                .siteFKId(asString(plant.getSiteFkId()))
                .verticalFKId(asString(plant.getVerticalFKId()))
                .caseDefId(CASE_DEF_ID)
                .processInstanceId(processInstanceId)
                .isDeleted(Boolean.FALSE)
                .build();
    }

    @Override
    @Transactional
    public void act(String taskId, String plantId, String year, String gateName, String decision,
            String remark, String actorUserId, String actorRole) {

        UUID plantUuid = UUID.fromString(plantId);
        Plants plant = plantsRepository.findById(plantUuid)
                .orElseThrow(() -> new IllegalArgumentException("Unknown plant: " + plantId));
        UUID masterId = resolveWorkflowMasterId(plant.getVerticalFKId());
        Map<String, StepMeta> steps = loadStepMeta(masterId);
        String businessKey = businessKey(plantId, year);

        String normalized = REVERTED.equalsIgnoreCase(decision) ? REVERTED : APPROVED;

        // 1) Audit the action (immutable trail: who / role / gate / decision / remark).
        StepMeta gateMeta = steps.get(gateName);
        auditService.record(businessKey, year, plantUuid, gateName,
                gateMeta != null ? gateMeta.displayName : null, gateMeta != null ? gateMeta.sequence : null,
                normalized, actorUserId, actorRole, remark, gateName, null);

        // 2) Complete the Camunda task with the decision (drives the gateway routing).
        List<ProcessVariable> vars = new ArrayList<>();
        vars.add(strVar("decision", normalized));
        vars.add(strVar("remark", remark != null ? remark : ""));
        taskService.complete(taskId, vars);

        // 3) Notify the approvers of whatever gate is now active (skip the gate just
        //    acted on, so parallel completions within a gate don't re-email it).
        notifyNextGates(masterId, gateName, steps, plant, year, businessKey);
    }

    /* ------------------------------------------------------------------ helpers */

    private void notifyNextGates(UUID masterId, String actedGate, Map<String, StepMeta> steps,
            Plants plant, String year, String businessKey) {
        List<Task> active;
        try {
            active = taskService.find(Optional.of(businessKey));
        } catch (Exception ex) {
            log.error("AOP notify: could not list tasks for {}: {}", businessKey, ex.getMessage());
            return;
        }
        if (active == null || active.isEmpty()) {
            return; // terminal (approved) or nothing pending
        }
        Set<String> notified = new HashSet<>();
        for (Task task : active) {
            String stepName = stepNameForTask(task.getTaskDefinitionKey());
            if (stepName == null || stepName.equals(actedGate) || !notified.add(stepName)) {
                continue;
            }
            notifyGate(masterId, stepName, steps, plant, year, businessKey,
                    "AOP approval required – " + displayNameOf(steps, stepName));
        }
    }

    private void notifyGate(UUID masterId, String stepName, Map<String, StepMeta> steps,
            Plants plant, String year, String businessKey, String subject) {
        List<String> roles = workflowStepRolesRepository
                .findActiveRolesByWorkflowMasterAndStepName(masterId, stepName);
        if (roles == null || roles.isEmpty()) {
            return;
        }
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("headline", "An AOP plan requires your review.");
        placeholders.put("plantName", plant.getName());
        placeholders.put("year", year);
        placeholders.put("gateDisplayName", displayNameOf(steps, stepName));
        placeholders.put("caseId", businessKey);
        notificationService.notifyRoles(subject, roles, null, placeholders);
    }

    private UUID resolveWorkflowMasterId(UUID verticalId) {
        List<WorkflowMaster> masters = workflowMasterRepository.findAllByVerticalFKId(verticalId);
        if (masters == null || masters.isEmpty()) {
            throw new IllegalStateException("No WorkflowMaster configured for vertical " + verticalId);
        }
        return masters.get(0).getId();
    }

    /** Comma-delimited active roles for a step; fails if a gate has no roles. */
    private String requiredRoles(UUID masterId, String stepName) {
        List<String> roles = workflowStepRolesRepository
                .findActiveRolesByWorkflowMasterAndStepName(masterId, stepName);
        if (roles == null || roles.isEmpty()) {
            throw new IllegalStateException("No active roles configured for gate '" + stepName + "'");
        }
        return String.join(",", roles);
    }

    /** Ordered gate steps (prepare, gate1..gate5) as DTOs, by sequence. */
    private List<WorkflowStepsMasterDTO> loadSteps(UUID masterId) {
        List<WorkflowStepsMasterDTO> steps = new ArrayList<>();
        for (Object[] row : workflowStepsMasterRepository.findAllByWorkflowMasterFKId(masterId)) {
            steps.add(WorkflowStepsMasterDTO.builder()
                    .id(row[0] != null ? row[0].toString() : null)
                    .name(row[1] != null ? row[1].toString() : null)
                    .displayName(row[2] != null ? row[2].toString() : null)
                    .sequence(row[3] != null ? Integer.valueOf(row[3].toString()) : null)
                    .isRemarksDisabled(row[4] != null && Boolean.parseBoolean(row[4].toString()))
                    .build());
        }
        return steps;
    }

    private Map<String, StepMeta> loadStepMeta(UUID masterId) {
        Map<String, StepMeta> map = new HashMap<>();
        for (WorkflowStepsMasterDTO s : loadSteps(masterId)) {
            if (s.getName() != null) {
                map.put(s.getName(), new StepMeta(s.getDisplayName(), s.getSequence(),
                        Boolean.TRUE.equals(s.getIsRemarksDisabled())));
            }
        }
        return map;
    }

    private String displayNameOf(Map<String, StepMeta> steps, String stepName) {
        StepMeta m = steps.get(stepName);
        return m != null && m.displayName != null ? m.displayName : stepName;
    }

    /** The rework task maps to the 'prepare' role/step config. */
    private String stepNameForTask(String taskDefinitionKey) {
        if (taskDefinitionKey == null) {
            return null;
        }
        return PREPARE_REWORK.equals(taskDefinitionKey) ? PREPARE : taskDefinitionKey;
    }

    private ProcessVariable strVar(String name, String value) {
        return ProcessVariable.builder()
                .name(name).value(value).type(ProcessVariableType.STRING.getValue()).build();
    }

    private String businessKey(String plantId, String year) {
        return "AOP_" + plantId + "_" + year;
    }

    private String asString(UUID id) {
        return id != null ? id.toString() : null;
    }

    private static final class StepMeta {
        final String displayName;
        final Integer sequence;
        final boolean remarksDisabled;

        StepMeta(String displayName, Integer sequence, boolean remarksDisabled) {
            this.displayName = displayName;
            this.sequence = sequence;
            this.remarksDisabled = remarksDisabled;
        }
    }

    /* ------------------------------------------------------- status + inbox */

    @Override
    @Transactional(readOnly = true)
    public AopWorkflowStatusDTO getStatus(String plantId, String year, String callerUserId, List<String> callerRoles) {
        UUID plantUuid = UUID.fromString(plantId);
        Plants plant = plantsRepository.findById(plantUuid)
                .orElseThrow(() -> new IllegalArgumentException("Unknown plant: " + plantId));
        UUID masterId = resolveWorkflowMasterId(plant.getVerticalFKId());
        Map<String, StepMeta> meta = loadStepMeta(masterId);
        List<WorkflowStepsMasterDTO> steps = loadSteps(masterId);
        String businessKey = businessKey(plantId, year);
        List<String> roles = callerRoles != null ? callerRoles : new ArrayList<>();

        AopWorkflowStatusDTO.AopWorkflowStatusDTOBuilder dto = AopWorkflowStatusDTO.builder()
                .caseId(businessKey)
                .plantId(plantId)
                .plantName(plant.getName())
                .siteName(siteName(plant.getSiteFkId()))
                .verticalName(verticalName(plant.getVerticalFKId()))
                .year(year)
                .steps(steps);

        List<Workflow> active = workflowRepository.findAllByYearAndPlantFKIdAndIsDeletedFalse(year, plantUuid);
        boolean callerIsPreparer = rolesIntersect(roles, activeRoles(masterId, PREPARE));

        if (active.isEmpty()) {
            // Not started yet — prepare stage; a preparer may submit.
            markStatuses(steps, 1);
            AopViewerDTO viewer = AopViewerDTO.builder()
                    .mode(callerIsPreparer ? "EDIT" : "READ_ONLY")
                    .canSubmit(callerIsPreparer)
                    .canEdit(callerIsPreparer)
                    .roles(roles)
                    .build();
            return dto.exists(false).currentGateName(PREPARE)
                    .currentGateDisplayName(displayNameOf(meta, PREPARE))
                    .currentSequence(meta.containsKey(PREPARE) ? meta.get(PREPARE).sequence : 1)
                    .viewer(viewer).build();
        }

        List<Task> tasks = safeFind(businessKey);
        String currentStep = currentStepOf(tasks, meta);
        Integer currentSeq = currentStep != null && meta.containsKey(currentStep) ? meta.get(currentStep).sequence : null;
        markStatuses(steps, currentSeq);

        // Actionable task = one whose assignee role is held by the caller.
        Task mine = tasks.stream()
                .filter(t -> t.getAssignee() != null && roles.contains(t.getAssignee()))
                .findFirst().orElse(null);

        AopViewerDTO.AopViewerDTOBuilder viewer = AopViewerDTO.builder().roles(roles);
        if (mine != null) {
            boolean prepareStage = PREPARE.equals(currentStep);
            viewer.mode("ACTION")
                    .canApprove(true)
                    .canRevert(true)
                    .canEdit(prepareStage)
                    .canSubmit(prepareStage)
                    .remarkMandatory(currentStep != null && meta.containsKey(currentStep)
                            && !meta.get(currentStep).remarksDisabled);
            dto.taskId(mine.getId()).assignedRole(mine.getAssignee());
        } else {
            viewer.mode("READ_ONLY");
        }

        return dto.exists(true)
                .currentGateName(currentStep)
                .currentGateDisplayName(displayNameOf(meta, currentStep))
                .currentSequence(currentSeq)
                .viewer(viewer.build())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AopPendingItemDTO> getMyPending(String callerUserId, List<String> callerRoles) {
        List<AopPendingItemDTO> items = new ArrayList<>();
        List<String> roles = callerRoles != null ? callerRoles : new ArrayList<>();
        if (roles.isEmpty()) {
            return items;
        }
        for (Workflow wf : workflowRepository.findAllByCaseDefIdAndIsDeletedFalse(CASE_DEF_ID)) {
            List<Task> tasks = safeFind(wf.getCaseId());
            Task mine = tasks.stream()
                    .filter(t -> t.getAssignee() != null && roles.contains(t.getAssignee()))
                    .findFirst().orElse(null);
            if (mine == null) {
                continue;
            }
            Plants plant = wf.getPlantFKId() != null ? plantsRepository.findById(wf.getPlantFKId()).orElse(null) : null;
            UUID masterId = resolveWorkflowMasterId(wf.getVerticalFKId());
            Map<String, StepMeta> meta = loadStepMeta(masterId);
            String stepName = stepNameForTask(mine.getTaskDefinitionKey());
            StepMeta sm = stepName != null ? meta.get(stepName) : null;

            AopViewerDTO actions = AopViewerDTO.builder()
                    .mode("ACTION").canApprove(true).canRevert(true)
                    .canEdit(PREPARE.equals(stepName)).canSubmit(PREPARE.equals(stepName))
                    .remarkMandatory(sm != null && !sm.remarksDisabled)
                    .roles(roles).build();

            items.add(AopPendingItemDTO.builder()
                    .caseId(wf.getCaseId())
                    .plantId(wf.getPlantFKId() != null ? wf.getPlantFKId().toString() : null)
                    .plantName(plant != null ? plant.getName() : null)
                    .siteName(siteName(wf.getSiteFKId()))
                    .verticalName(verticalName(wf.getVerticalFKId()))
                    .year(wf.getYear())
                    .gateName(stepName)
                    .gateDisplayName(sm != null ? sm.displayName : stepName)
                    .sequence(sm != null ? sm.sequence : null)
                    .assignedRole(mine.getAssignee())
                    .taskId(mine.getId())
                    .actions(actions)
                    .build());
        }
        return items;
    }

    private List<Task> safeFind(String businessKey) {
        try {
            List<Task> tasks = taskService.find(Optional.of(businessKey));
            return tasks != null ? tasks : new ArrayList<>();
        } catch (Exception ex) {
            log.error("AOP status: could not list tasks for {}: {}", businessKey, ex.getMessage());
            return new ArrayList<>();
        }
    }

    /** The active gate = the lowest-sequence step among the active tasks. */
    private String currentStepOf(List<Task> tasks, Map<String, StepMeta> meta) {
        String current = null;
        int best = Integer.MAX_VALUE;
        for (Task t : tasks) {
            String step = stepNameForTask(t.getTaskDefinitionKey());
            StepMeta sm = step != null ? meta.get(step) : null;
            int seq = sm != null && sm.sequence != null ? sm.sequence : Integer.MAX_VALUE;
            if (seq < best) {
                best = seq;
                current = step;
            }
        }
        return current;
    }

    /** completed for steps before the current sequence, inprogress at it. */
    private void markStatuses(List<WorkflowStepsMasterDTO> steps, Integer currentSeq) {
        if (currentSeq == null) {
            return;
        }
        for (WorkflowStepsMasterDTO s : steps) {
            if (s.getSequence() == null) {
                continue;
            }
            if (s.getSequence() < currentSeq) {
                s.setStatus("completed");
            } else if (s.getSequence().equals(currentSeq)) {
                s.setStatus("inprogress");
            }
        }
    }

    private List<String> activeRoles(UUID masterId, String stepName) {
        List<String> roles = workflowStepRolesRepository.findActiveRolesByWorkflowMasterAndStepName(masterId, stepName);
        return roles != null ? roles : new ArrayList<>();
    }

    private boolean rolesIntersect(List<String> a, List<String> b) {
        for (String r : a) {
            if (b.contains(r)) {
                return true;
            }
        }
        return false;
    }

    private String siteName(UUID siteId) {
        if (siteId == null) {
            return null;
        }
        return siteRepository.findById(siteId).map(Sites::getName).orElse(null);
    }

    private String verticalName(UUID verticalId) {
        if (verticalId == null) {
            return null;
        }
        return verticalRepository.findById(verticalId).map(Verticals::getName).orElse(null);
    }
}
