package com.wks.caseengine.service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.bpm.engine.client.facade.BpmEngineClientFacade;
import com.wks.bpm.engine.model.spi.ProcessInstance;
import com.wks.bpm.engine.model.spi.ProcessVariable;
import com.wks.bpm.engine.model.spi.ProcessVariableType;
import com.wks.bpm.engine.model.spi.Task;
import com.wks.caseengine.dto.AopPendingItemDTO;
import com.wks.caseengine.dto.AopStepRoleDTO;
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
    /** Recorded as the destination when a decision ends the process. */
    private static final String COMPLETED = "COMPLETED";
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_COMPLETED = "completed";
    /** Camunda REST emits offsets without a colon, e.g. 2026-07-21T15:30:00.000+0000. */
    private static final DateTimeFormatter ENGINE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Autowired
    private ProcessInstanceService processInstanceService;
    @Autowired
    private TaskService taskService;
    /** Direct engine access for task completion — bypasses the case-instance hooks. */
    @Autowired
    private BpmEngineClientFacade processEngineClientFacade;
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
        return start(plantId, year, actorUserId, null, null);
    }

    @Transactional
    public WorkflowDTO start(String plantId, String year, String actorUserId, String remark, String actorRole) {
        UUID plantUuid = UUID.fromString(plantId);
        Plants plant = plantsRepository.findById(plantUuid)
                .orElseThrow(() -> new IllegalArgumentException("Unknown plant: " + plantId));

        String businessKey = businessKey(plantId, year);
        List<Workflow> existing = workflowRepository.findAllByYearAndPlantFKIdAndIsDeletedFalse(year, plantUuid);
        if (!existing.isEmpty()) {
            if (auditService.hasCompleted(businessKey)) {
                throw new WorkflowConflictException(
                        "AOP workflow for plant " + plantId + " and year " + year
                                + " is already fully approved");
            }
            // Live Camunda process still running — true conflict.
            if (findProcessInstanceId(businessKey) != null) {
                throw new WorkflowConflictException(
                        "An AOP workflow already exists for plant " + plantId + " and year " + year);
            }
            // DB row left behind after a Camunda wipe/restart — reuse it instead of 409.
            log.warn("AOP start: reusing dead workflow row for {}", businessKey);
            Workflow wf = existing.get(0);
            return restartExistingWorkflow(wf, plant, year, businessKey, actorUserId, remark, actorRole);
        }

        UUID masterId = resolveWorkflowMasterId(plant.getVerticalFKId());
        Map<String, StepMeta> steps = loadStepMeta(masterId);

        // Inject each gate's approver roles as comma-delimited process variables.
        List<ProcessVariable> vars = buildStartVariables(masterId, plantId, year, remark);
        // DB unique-guard alone is not enough: prior failed starts / restarts can leave
        // orphan Camunda instances on the same businessKey. Those mix into task queries
        // and make Approve/Revert target the wrong gate.
        deleteOrphanProcessInstances(businessKey, null);
        String processInstanceId = startEngineProcess(businessKey, vars);

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

        recordSubmitted(businessKey, year, plantUuid, steps, actorUserId, actorRole, remark);
        notifyGate(masterId, "gate1", steps, plant, year, businessKey,
                "AOP submitted – Gate 1 approval required");

        return toWorkflowDto(wf, plantId, year, plant);
    }

    private WorkflowDTO restartExistingWorkflow(Workflow wf, Plants plant, String year, String businessKey,
            String actorUserId, String remark, String actorRole) {
        UUID masterId = resolveWorkflowMasterId(plant.getVerticalFKId());
        Map<String, StepMeta> steps = loadStepMeta(masterId);
        List<ProcessVariable> vars = buildStartVariables(masterId, plant.getId().toString(), year, remark);
        deleteOrphanProcessInstances(businessKey, null);
        String processInstanceId = startEngineProcess(businessKey, vars);
        wf.setProcessInstanceId(processInstanceId);
        workflowRepository.save(wf);
        recordSubmitted(businessKey, year, plant.getId(), steps, actorUserId, actorRole, remark);
        notifyGate(masterId, "gate1", steps, plant, year, businessKey,
                "AOP submitted – Gate 1 approval required");
        return toWorkflowDto(wf, plant.getId().toString(), year, plant);
    }

    private List<ProcessVariable> buildStartVariables(UUID masterId, String plantId, String year, String remark) {
        List<ProcessVariable> vars = new ArrayList<>();
        vars.add(strVar("prepareRoles", requiredRoles(masterId, PREPARE)));
        for (String gate : GATES) {
            vars.add(strVar(gate + "Roles", requiredRoles(masterId, gate)));
        }
        vars.add(strVar("plantId", plantId));
        vars.add(strVar("year", year));
        if (remark != null && !remark.isBlank()) {
            vars.add(strVar("remark", remark.trim()));
        }
        return vars;
    }

    private void recordSubmitted(String businessKey, String year, UUID plantUuid, Map<String, StepMeta> steps,
            String actorUserId, String actorRole, String remark) {
        StepMeta prep = steps.get(PREPARE);
        auditService.record(businessKey, year, plantUuid, PREPARE,
                prep != null ? prep.displayName : null, prep != null ? prep.sequence : null,
                SUBMITTED, actorUserId, actorRole,
                remark != null && !remark.isBlank() ? remark.trim() : null,
                PREPARE, "gate1");
    }

    private WorkflowDTO toWorkflowDto(Workflow wf, String plantId, String year, Plants plant) {
        return WorkflowDTO.builder()
                .caseId(wf.getCaseId())
                .plantFkId(plantId)
                .year(year)
                .siteFKId(asString(plant.getSiteFkId()))
                .verticalFKId(asString(plant.getVerticalFKId()))
                .caseDefId(CASE_DEF_ID)
                .processInstanceId(wf.getProcessInstanceId())
                .isDeleted(Boolean.FALSE)
                .build();
    }

    /**
     * Start the engine process and return its instance id, never null.
     *
     * <p>The shared C7 client swallows engine errors and returns {@code null}
     * (see C7EngineClient#startProcess). Persisting a Workflow row off the back
     * of that would leave an orphan: no process in Camunda, no tasks, no gates —
     * yet the row blocks re-submission via the one-active-workflow guard. So we
     * fail loudly here, before anything is written.</p>
     *
     * <p>If the start returned an instance without an id, the process may still
     * exist (only the response mapping failed), so we look it up by business key
     * before giving up.</p>
     */
    private String startEngineProcess(String businessKey, List<ProcessVariable> vars) {
        ProcessInstance pi = processInstanceService.start(PROCESS_KEY, Optional.of(businessKey), vars);
        String id = pi != null ? pi.getId() : null;

        if (id == null || id.isBlank()) {
            id = findProcessInstanceId(businessKey);
        }
        if (id == null || id.isBlank()) {
            throw new IllegalStateException("Failed to start Camunda process '" + PROCESS_KEY
                    + "' for businessKey " + businessKey
                    + ". Check that the process is deployed under the configured tenant and that the"
                    + " c7-client jar (AopGateDecisionListener) is on the engine classpath;"
                    + " the engine error is logged by C7EngineClient as 'Error starting process'.");
        }
        return id;
    }

    /**
     * Fill in a missing processInstanceId on an existing row from the live engine.
     * No-op when already set or when no instance can be resolved.
     */
    private void backfillProcessInstanceId(Workflow wf, String businessKey) {
        if (wf == null || (wf.getProcessInstanceId() != null && !wf.getProcessInstanceId().isBlank())) {
            return;
        }
        String id = findProcessInstanceId(businessKey);
        if (id != null) {
            wf.setProcessInstanceId(id);
            workflowRepository.save(wf);
            log.info("AOP: backfilled processInstanceId {} for businessKey {}", id, businessKey);
        }
    }

    /** Resolve a running instance id by business key, or null if there is none. */
    private String findProcessInstanceId(String businessKey) {
        try {
            List<ProcessInstance> found = processInstanceService.find(
                    Optional.of(PROCESS_KEY), Optional.of(businessKey), Optional.empty());
            if (found != null && !found.isEmpty() && found.get(0) != null) {
                return found.get(0).getId();
            }
        } catch (Exception ex) {
            log.warn("AOP: process instance lookup failed for businessKey {}: {}", businessKey, ex.getMessage());
        }
        return null;
    }

    @Override
    @Transactional
    public void act(String taskId, String plantId, String year, String gateName, String decision,
            String remark, String actorUserId, String actorRole, List<String> callerRoles) {

        UUID plantUuid = UUID.fromString(plantId);
        Plants plant = plantsRepository.findById(plantUuid)
                .orElseThrow(() -> new IllegalArgumentException("Unknown plant: " + plantId));
        UUID masterId = resolveWorkflowMasterId(plant.getVerticalFKId());
        Map<String, StepMeta> steps = loadStepMeta(masterId);
        String businessKey = businessKey(plantId, year);

        List<Workflow> active = workflowRepository.findAllByYearAndPlantFKIdAndIsDeletedFalse(year, plantUuid);
        if (active.isEmpty()) {
            throw new IllegalStateException("No active AOP workflow for plant " + plantId + " and year " + year);
        }
        String processInstanceId = ensureLiveProcess(active.get(0), plant, year, businessKey);
        List<Task> openTasks = tasksForActing(businessKey, processInstanceId);

        // Gate must come from the selected Camunda task, not the client payload.
        // Orphan process instances on the same businessKey previously made status
        // report "prepare" while the caller's taskId belonged to gate1 — matching
        // on the client gateName then silently no-op'd every Approve/Revert.
        Task selected = findTaskById(openTasks, taskId);
        String resolvedGate = selected != null
                ? stepNameForTask(selected.getTaskDefinitionKey())
                : (gateName != null && !gateName.isBlank() ? gateName : null);
        if (resolvedGate == null) {
            throw new IllegalArgumentException("Cannot resolve gate for task " + taskId);
        }
        if (gateName != null && !gateName.isBlank() && !gateName.equals(resolvedGate)) {
            log.warn("AOP act: client gateName '{}' disagrees with task {} gate '{}'; using task gate",
                    gateName, taskId, resolvedGate);
        }

        String normalized = PREPARE.equals(resolvedGate)
                ? APPROVED
                : (REVERTED.equalsIgnoreCase(decision) ? REVERTED : APPROVED);
        // Prefer SUBMITTED in the audit trail for prepare (matches first /start).
        String auditAction = PREPARE.equals(resolvedGate) ? SUBMITTED : normalized;
        StepMeta gateMeta = steps.get(resolvedGate);

        // Approve: one role-slot only. Multi-instance gates (Functional Heads and
        // any other 1..N role gate) keep a task per configured role; completing
        // every role the caller currently holds would consume a later assignment
        // they have not acted as. Reassignment mid-visit (remove role A, add role B)
        // must still find B's open task.
        // Prepare: complete every multi-instance slot so one Submit advances.
        // Revert: any one reverter must leave the gate — complete remaining slots
        // (BPMN completionCondition also cancels them on new deployments; this
        // covers in-flight processes still on the old definition).
        List<Task> toComplete;
        if (PREPARE.equals(resolvedGate) || REVERTED.equals(normalized)) {
            toComplete = allOpenTasksAtGate(openTasks, resolvedGate, taskId);
        } else {
            Task target = resolveApproveTask(openTasks, resolvedGate, selected, callerRoles);
            toComplete = target != null ? List.of(target) : List.of();
        }

        // Replayed request: the caller has no open task left at this gate, so this
        // decision has already been applied (a double-clicked button, a retried
        // request, a stale taskId from a page that has not refreshed). Recording it
        // again would add a duplicate audit row for an engine action that never
        // happened. Treat it as a no-op rather than an error - the caller's intent
        // was satisfied by the first request.
        if (toComplete.isEmpty()) {
            log.info("AOP act: no open task for {} at {} (user {}, roles {}) - already decided, ignoring replay",
                    businessKey, resolvedGate, actorUserId, callerRoles);
            return;
        }

        List<ProcessVariable> vars = new ArrayList<>();
        vars.add(strVar("decision", normalized));
        vars.add(strVar("remark", remark != null ? remark : ""));

        List<UUID> auditIds = new ArrayList<>();
        if (PREPARE.equals(resolvedGate) || REVERTED.equals(normalized)) {
            // One human action — one history row. Peer multi-instance slots are
            // still completed (or already cancelled by BPMN completionCondition).
            String actionRole = actorRole;
            if (actionRole == null || actionRole.isBlank()) {
                Task picked = findTaskById(toComplete, taskId);
                actionRole = picked != null && picked.getAssignee() != null
                        ? picked.getAssignee()
                        : toComplete.get(0).getAssignee();
            }
            auditIds.add(auditService.record(businessKey, year, plantUuid, resolvedGate,
                    gateMeta != null ? gateMeta.displayName : null,
                    gateMeta != null ? gateMeta.sequence : null,
                    auditAction, actorUserId, actionRole,
                    remark, resolvedGate, null));
            for (Task task : toComplete) {
                try {
                    processEngineClientFacade.complete(task.getId(), vars);
                } catch (Exception ex) {
                    // Sibling may already be cancelled by MI completionCondition.
                    log.warn("AOP act: could not complete task {} at {} ({}): {}",
                            task.getId(), resolvedGate, task.getAssignee(), ex.getMessage());
                }
            }
        } else {
            for (Task task : toComplete) {
                // Audit first: the trail must not claim an approval the engine rejected.
                // toGate is unknown until the engine has routed — back-filled below.
                // Gate multi-instance approve: one audit row per role completed in this act.
                auditIds.add(auditService.record(businessKey, year, plantUuid, resolvedGate,
                        gateMeta != null ? gateMeta.displayName : null,
                        gateMeta != null ? gateMeta.sequence : null,
                        auditAction, actorUserId,
                        task.getAssignee() != null ? task.getAssignee() : actorRole,
                        remark, resolvedGate, null));

                // Deliberately NOT case-engine's TaskService: its TaskCompleteListener
                // resolves a CaseInstance by businessKey and throws
                // CaseInstanceNotFoundException for an AOP plan, which has no case
                // document. That fires after the engine call, so the task completes but
                // the request 500s. AOP is a plain Camunda process — go straight to the
                // engine client.
                processEngineClientFacade.complete(task.getId(), vars);
            }
        }

        // Where did the plan actually land? Asking the engine after the fact is the
        // only honest answer: an APPROVED at a multi-instance gate does not by
        // itself advance anything (a peer may still revert it), so anything derived
        // from the decision alone would be a guess. Same gate back = still waiting
        // on that gate's other approvers.
        List<Task> remaining = safeFind(businessKey, processInstanceId);
        String destination = remaining.isEmpty() ? COMPLETED : currentStepOf(remaining, steps);
        auditService.completeToGate(auditIds, destination);

        // Notify the approvers of whatever gate is now active (skip the gate just
        // acted on, so parallel completions within a gate don't re-email it).
        notifyNextGates(masterId, resolvedGate, steps, plant, year, businessKey, processInstanceId);
    }

    /**
     * The single role-slot to complete for an approve. Prefer the selected task
     * when the caller currently holds its assignee role; otherwise any open task
     * at this gate assigned to a role they currently hold. Role-agnostic: a
     * mid-visit reassignment to a different approver role still finds that role's
     * remaining task.
     */
    private Task resolveApproveTask(List<Task> openTasks, String gateName, Task selected,
            List<String> callerRoles) {
        if (selected != null
                && gateName.equals(stepNameForTask(selected.getTaskDefinitionKey()))
                && holdsRole(callerRoles, selected.getAssignee())) {
            return selected;
        }
        if (openTasks == null) {
            return null;
        }
        for (Task task : openTasks) {
            if (task == null || task.getId() == null) {
                continue;
            }
            if (!gateName.equals(stepNameForTask(task.getTaskDefinitionKey()))) {
                continue;
            }
            if (holdsRole(callerRoles, task.getAssignee())) {
                return task;
            }
        }
        return null;
    }

    /** Case-insensitive: JWT / Keycloak role names vs Camunda task assignee. */
    private boolean holdsRole(List<String> callerRoles, String assignee) {
        if (callerRoles == null || assignee == null || assignee.isBlank()) {
            return false;
        }
        for (String role : callerRoles) {
            if (role != null && role.equalsIgnoreCase(assignee)) {
                return true;
            }
        }
        return false;
    }

    /**
     * All open tasks at a step (used for Prepare/rework submit). Selected task
     * first when still present; otherwise remaining instances in any order.
     */
    private List<Task> allOpenTasksAtGate(List<Task> openTasks, String gateName, String taskId) {
        List<Task> matched = new ArrayList<>();
        Task selected = null;
        Set<String> seen = new HashSet<>();
        for (Task task : openTasks) {
            if (task.getId() == null || !seen.add(task.getId())) {
                continue;
            }
            if (!gateName.equals(stepNameForTask(task.getTaskDefinitionKey()))) {
                continue;
            }
            if (taskId != null && taskId.equals(task.getId())) {
                selected = task;
            } else {
                matched.add(task);
            }
        }
        if (selected != null) {
            matched.add(0, selected);
        }
        return matched;
    }

    private Task findTaskById(List<Task> tasks, String taskId) {
        if (taskId == null || tasks == null) {
            return null;
        }
        for (Task task : tasks) {
            if (taskId.equals(task.getId())) {
                return task;
            }
        }
        return null;
    }

    /* ------------------------------------------------------------------ helpers */

    private void notifyNextGates(UUID masterId, String actedGate, Map<String, StepMeta> steps,
            Plants plant, String year, String businessKey, String processInstanceId) {
        List<Task> active;
        try {
            active = filterByProcessInstance(taskService.find(Optional.of(businessKey)), processInstanceId);
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

    /**
     * The AOP approval configuration — gates and their approver roles.
     *
     * <p>One definition serves every vertical and every plant. The gates
     * themselves are fixed in the BPMN, so a per-vertical copy of the master
     * could only ever differ in its role lists, and the flow is the same for
     * the whole business: a single global row (the one with no vertical) is
     * the definition.</p>
     *
     * <p>A vertical may still override it by owning a row of its own, and that
     * row wins where it exists. That is what makes this deployable ahead of the
     * data migration: until the global row is seeded, today's per-vertical rows
     * are still found and nothing changes.</p>
     */
    private UUID resolveWorkflowMasterId(UUID verticalId) {
        if (verticalId != null) {
            List<WorkflowMaster> override =
                    workflowMasterRepository.findAllByWorkflowIdAndVerticalFKId(PROCESS_KEY, verticalId);
            if (override != null && !override.isEmpty()) {
                return override.get(0).getId();
            }
        }

        List<WorkflowMaster> global = workflowMasterRepository.findAllByWorkflowIdAndVerticalFKIdIsNull(PROCESS_KEY);
        if (global == null || global.isEmpty()) {
            throw new IllegalStateException("No WorkflowMaster configured for workflow " + PROCESS_KEY
                    + " - seed the global definition (Vertical_FK_Id NULL)");
        }
        return global.get(0).getId();
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

        // Self-heal rows written before the start-failure guard existed (or by the
        // legacy submit path, which never carried a process instance id).
        Workflow wf = active.get(0);

        // Gate 5 approve ends the Camunda process. Do not treat that as a missing
        // engine instance and restart at Gate 1.
        if (auditService.hasCompleted(businessKey)) {
            deleteOrphanProcessInstances(businessKey, null);
            markAllCompleted(steps);
            AopViewerDTO viewer = AopViewerDTO.builder()
                    .mode("READ_ONLY")
                    .roles(roles)
                    .build();
            return dto.exists(true)
                    .currentGateName(COMPLETED)
                    .currentGateDisplayName("Approved")
                    .currentSequence(null)
                    .viewer(viewer)
                    .build();
        }

        String processInstanceId;
        try {
            processInstanceId = ensureLiveProcess(wf, plant, year, businessKey);
        } catch (Exception ex) {
            // Do not turn a Camunda outage / missing plugin into a 500 on status —
            // the UI still needs to render. Act/start will surface the real error.
            log.error("AOP status: could not ensure live process for {}: {}", businessKey, ex.getMessage());
            AopViewerDTO viewer = AopViewerDTO.builder()
                    .mode("READ_ONLY")
                    .roles(roles)
                    .build();
            return dto.exists(true)
                    .currentGateName(null)
                    .currentGateDisplayName(null)
                    .currentSequence(null)
                    .viewer(viewer)
                    .build();
        }

        if (processInstanceId == null) {
            AopViewerDTO viewer = AopViewerDTO.builder()
                    .mode("READ_ONLY")
                    .roles(roles)
                    .build();
            return dto.exists(true)
                    .currentGateName(null)
                    .currentGateDisplayName(null)
                    .currentSequence(null)
                    .viewer(viewer)
                    .build();
        }

        List<Task> tasks = safeFind(businessKey, processInstanceId);
        String currentStep = currentStepOf(tasks, meta);
        Integer currentSeq = currentStep != null && meta.containsKey(currentStep) ? meta.get(currentStep).sequence : null;
        markStatuses(steps, currentSeq);

        // Actionable when any currently held role still has an open task. Prefer
        // the current gate so a role match on a different step cannot win.
        // Eligibility is per remaining role, not per person: acting as one
        // approver role must not block a later assignment to a different role at
        // the same visit. Prepare/rework is unchanged — any preparer may finish
        // remaining multi-instance slots below.
        Task mine = tasks.stream()
                .filter(t -> holdsRole(roles, t.getAssignee()))
                .filter(t -> currentStep == null
                        || currentStep.equals(stepNameForTask(t.getTaskDefinitionKey())))
                .findFirst()
                .orElseGet(() -> tasks.stream()
                        .filter(t -> holdsRole(roles, t.getAssignee()))
                        .findFirst()
                        .orElse(null));
        // After one preparer completes their multi-instance slot, peer prepare
        // tasks remain under other roles. Any preparer must still be able to
        // Submit and clear those so the plan advances to Gate 1.
        if (mine == null && PREPARE.equals(currentStep) && callerIsPreparer) {
            mine = tasks.stream()
                    .filter(t -> PREPARE.equals(stepNameForTask(t.getTaskDefinitionKey())))
                    .findFirst()
                    .orElse(null);
        }

        AopViewerDTO.AopViewerDTOBuilder viewer = AopViewerDTO.builder().roles(roles);
        if (mine != null) {
            String mineGate = stepNameForTask(mine.getTaskDefinitionKey());
            // The gate the caller can act on is the source of truth for button state
            // and the gateName the UI will send back on Approve/Revert.
            String actionGate = mineGate != null ? mineGate : currentStep;
            Integer actionSeq = actionGate != null && meta.containsKey(actionGate)
                    ? meta.get(actionGate).sequence : currentSeq;
            if (actionGate != null && !actionGate.equals(currentStep)) {
                markStatuses(steps, actionSeq);
            }
            // Prepare / prepareRework is a submit step, not a gate decision.
            // Approvers see Approve+Revert; preparers only see Submit for Approval.
            boolean prepareStage = PREPARE.equals(actionGate);
            viewer.mode("ACTION")
                    .canApprove(!prepareStage)
                    .canRevert(!prepareStage)
                    .canEdit(prepareStage)
                    .canSubmit(prepareStage)
                    .remarkMandatory(actionGate != null && meta.containsKey(actionGate)
                            && !meta.get(actionGate).remarksDisabled);
            dto.taskId(mine.getId()).assignedRole(mine.getAssignee());
            return dto.exists(true)
                    .currentGateName(actionGate)
                    .currentGateDisplayName(displayNameOf(meta, actionGate))
                    .currentSequence(actionSeq)
                    .viewer(viewer.build())
                    .build();
        }
        viewer.mode("READ_ONLY");

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
            Plants plant = wf.getPlantFKId() != null ? plantsRepository.findById(wf.getPlantFKId()).orElse(null) : null;
            UUID masterId = resolveWorkflowMasterId(wf.getVerticalFKId());
            Map<String, StepMeta> meta = loadStepMeta(masterId);

            if (auditService.hasCompleted(wf.getCaseId())) {
                String lastGate = GATES.get(GATES.size() - 1);
                items.add(AopPendingItemDTO.builder()
                        .caseId(wf.getCaseId())
                        .plantId(wf.getPlantFKId() != null ? wf.getPlantFKId().toString() : null)
                        .plantName(plant != null ? plant.getName() : null)
                        .siteName(siteName(wf.getSiteFKId()))
                        .verticalName(verticalName(wf.getVerticalFKId()))
                        .year(wf.getYear())
                        .gateName(COMPLETED)
                        .gateDisplayName("Approved")
                        .sequence(null)
                        .assignedRole(null)
                        .taskId(null)
                        .listOfRoles(listOfRolesForStep(masterId, lastGate, wf.getCaseId(), List.of()))
                        .status(STATUS_COMPLETED)
                        .actions(AopViewerDTO.builder()
                                .mode("READ_ONLY")
                                .canApprove(false)
                                .canRevert(false)
                                .canEdit(false)
                                .canSubmit(false)
                                .remarkMandatory(false)
                                .roles(roles).build())
                        .build());
                continue;
            }

            String processInstanceId = canonicalProcessInstanceId(wf, wf.getCaseId());
            List<Task> tasks = safeFind(wf.getCaseId(), processInstanceId);
            if (tasks == null || tasks.isEmpty()) {
                continue;
            }

            // Open task for any currently held role is enough. A prior action at
            // this gate under a different role (reassignment mid-visit) must not
            // hide the remaining role's task.
            Task mine = tasks.stream()
                    .filter(t -> holdsRole(roles, t.getAssignee()))
                    .findFirst().orElse(null);

            boolean isActionable = mine != null;
            Task targetTask = isActionable ? mine : tasks.get(0);

            String stepName = stepNameForTask(targetTask.getTaskDefinitionKey());
            StepMeta sm = stepName != null ? meta.get(stepName) : null;
            boolean prepareStage = PREPARE.equals(stepName);

            AopViewerDTO actions;
            if (isActionable) {
                actions = AopViewerDTO.builder()
                        .mode("ACTION")
                        .canApprove(!prepareStage)
                        .canRevert(!prepareStage)
                        .canEdit(prepareStage)
                        .canSubmit(prepareStage)
                        .remarkMandatory(sm != null && !sm.remarksDisabled)
                        .roles(roles).build();
            } else {
                actions = AopViewerDTO.builder()
                        .mode("READ_ONLY")
                        .canApprove(false)
                        .canRevert(false)
                        .canEdit(false)
                        .canSubmit(false)
                        .remarkMandatory(false)
                        .roles(roles).build();
            }

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
                    .assignedRole(targetTask.getAssignee())
                    .taskId(isActionable && mine != null ? mine.getId() : null)
                    .listOfRoles(listOfRolesForStep(masterId, stepName, wf.getCaseId(), tasks))
                    .status(STATUS_PENDING)
                    .actions(actions)
                    .build());
        }
        return items;
    }

    /**
     * Approver roles for the current gate, each marked approved (already acted
     * this visit) or pending (still has an open task / has not approved).
     */
    private List<AopStepRoleDTO> listOfRolesForStep(UUID masterId, String stepName, String caseId,
            List<Task> tasks) {
        List<AopStepRoleDTO> result = new ArrayList<>();
        if (stepName == null) {
            return result;
        }

        Set<String> pendingAssignees = new HashSet<>();
        if (tasks != null) {
            for (Task task : tasks) {
                if (stepName.equals(stepNameForTask(task.getTaskDefinitionKey()))
                        && task.getAssignee() != null && !task.getAssignee().isBlank()) {
                    pendingAssignees.add(task.getAssignee());
                }
            }
        }

        Set<String> approvedRoles = auditService.approvedRolesInCurrentVisit(
                caseId, stepName, visitStartOf(tasks, stepName));

        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        ordered.addAll(activeRoles(masterId, stepName));
        ordered.addAll(pendingAssignees);
        ordered.addAll(approvedRoles);

        for (String role : ordered) {
            boolean approved = approvedRoles.contains(role) && !pendingAssignees.contains(role);
            result.add(AopStepRoleDTO.builder().role(role).approved(approved).build());
        }
        return result;
    }

    /**
     * When the plan entered {@code stepName} — the earliest creation time among the
     * gate's currently open tasks. A gate's instances are all created together on
     * entry, so this marks the start of the current visit; re-entering after a
     * revert creates fresh tasks and moves the boundary forward.
     *
     * @return null if no open task at that step carries a parsable timestamp
     */
    private OffsetDateTime visitStartOf(List<Task> tasks, String stepName) {
        if (tasks == null || stepName == null) {
            return null;
        }
        OffsetDateTime earliest = null;
        for (Task task : tasks) {
            if (!stepName.equals(stepNameForTask(task.getTaskDefinitionKey()))) {
                continue;
            }
            OffsetDateTime created = parseEngineTimestamp(task.getCreated());
            if (created != null && (earliest == null || created.isBefore(earliest))) {
                earliest = created;
            }
        }
        return earliest;
    }

    /**
     * Parse a Camunda REST timestamp. The engine emits an offset without a colon
     * ("2026-07-21T15:30:00.000+0000"), which ISO_OFFSET_DATE_TIME rejects, so try
     * the engine's own format first and fall back to strict ISO.
     */
    private OffsetDateTime parseEngineTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value, ENGINE_TIMESTAMP);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(value);
            } catch (DateTimeParseException ex) {
                log.warn("AOP: could not parse task timestamp '{}'", value);
                return null;
            }
        }
    }

    /**
     * Task lookup for the decision path, where an empty list is load-bearing: it
     * is read as "this decision was already applied" and suppresses the audit row.
     * A failed lookup must therefore never come back empty — it throws, so the
     * caller gets a 500 and can retry, rather than having their approval silently
     * swallowed. Read paths use {@link #safeFind}, where degrading to an empty
     * list only costs a button.
     */
    private List<Task> tasksForActing(String businessKey, String processInstanceId) {
        try {
            return filterByProcessInstance(taskService.find(Optional.of(businessKey)), processInstanceId);
        } catch (Exception ex) {
            throw new IllegalStateException("AOP act: could not list tasks for " + businessKey
                    + " - refusing to record a decision without confirming it reached the engine", ex);
        }
    }

    private List<Task> safeFind(String businessKey, String processInstanceId) {
        try {
            return filterByProcessInstance(taskService.find(Optional.of(businessKey)), processInstanceId);
        } catch (Exception ex) {
            log.error("AOP status: could not list tasks for {}: {}", businessKey, ex.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Task> filterByProcessInstance(List<Task> tasks, String processInstanceId) {
        if (tasks == null) {
            return new ArrayList<>();
        }
        if (processInstanceId == null || processInstanceId.isBlank()) {
            return new ArrayList<>(tasks);
        }
        List<Task> filtered = new ArrayList<>();
        for (Task task : tasks) {
            if (processInstanceId.equals(task.getProcessInstanceId())) {
                filtered.add(task);
            }
        }
        return filtered;
    }

    /**
     * The Workflow row's process instance is the single source of truth. Extra
     * Camunda instances sharing the business key (failed restarts, double submits
     * before the DB guard) are deleted so task queries cannot mix gates.
     */
    private String canonicalProcessInstanceId(Workflow wf, String businessKey) {
        backfillProcessInstanceId(wf, businessKey);
        String keepId = wf != null ? wf.getProcessInstanceId() : null;
        if ((keepId == null || keepId.isBlank()) && businessKey != null) {
            keepId = findProcessInstanceId(businessKey);
            if (keepId != null && wf != null) {
                wf.setProcessInstanceId(keepId);
                workflowRepository.save(wf);
            }
        }
        deleteOrphanProcessInstances(businessKey, keepId);
        return keepId;
    }

    /**
     * Ensure the Workflow row points at a live Camunda process. If the engine was
     * wiped/recreated (or the instance was deleted) while the DB row remained,
     * restart the process in place so Approve/Revert buttons have a real task —
     * except when the plan already finished Gate 5 (COMPLETED); that must stay
     * terminal and must never be resurrected at gate1.
     */
    private String ensureLiveProcess(Workflow wf, Plants plant, String year, String businessKey) {
        if (auditService.hasCompleted(businessKey)) {
            deleteOrphanProcessInstances(businessKey, null);
            return null;
        }

        String keepId = canonicalProcessInstanceId(wf, businessKey);
        String liveId = findProcessInstanceId(businessKey);
        if (liveId != null) {
            if (!liveId.equals(keepId)) {
                wf.setProcessInstanceId(liveId);
                workflowRepository.save(wf);
                deleteOrphanProcessInstances(businessKey, liveId);
            }
            return liveId;
        }

        log.warn("AOP: workflow row exists for {} but no live Camunda process; restarting at gate1", businessKey);
        UUID masterId = resolveWorkflowMasterId(plant.getVerticalFKId());
        List<ProcessVariable> vars = buildStartVariables(masterId, plant.getId().toString(), year, null);
        String newId = startEngineProcess(businessKey, vars);
        wf.setProcessInstanceId(newId);
        workflowRepository.save(wf);
        return newId;
    }

    private void markAllCompleted(List<WorkflowStepsMasterDTO> steps) {
        if (steps == null) {
            return;
        }
        for (WorkflowStepsMasterDTO s : steps) {
            s.setStatus("completed");
        }
    }

    private void deleteOrphanProcessInstances(String businessKey, String keepId) {
        try {
            List<ProcessInstance> found = processInstanceService.find(
                    Optional.of(PROCESS_KEY), Optional.of(businessKey), Optional.empty());
            if (found == null || found.isEmpty()) {
                return;
            }
            // keepId == null means "delete every instance" (used before a fresh start).
            // Never fall back to retaining found.get(0) in that case — that left orphans
            // alive and the next start created yet another process on the same key.
            for (ProcessInstance pi : found) {
                if (pi == null || pi.getId() == null) {
                    continue;
                }
                if (keepId != null && !keepId.isBlank() && keepId.equals(pi.getId())) {
                    continue;
                }
                try {
                    processInstanceService.delete(pi.getId());
                    log.warn("AOP: deleted orphan process instance {} for businessKey {} (kept {})",
                            pi.getId(), businessKey, keepId);
                } catch (Exception ex) {
                    log.error("AOP: failed to delete orphan process instance {} for {}: {}",
                            pi.getId(), businessKey, ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("AOP: orphan process-instance cleanup failed for {}: {}", businessKey, ex.getMessage());
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

    /** completed for steps before the current sequence, inprogress at it, pending after. */
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
            } else {
                s.setStatus("pending");
            }
        }
    }

    private List<String> activeRoles(UUID masterId, String stepName) {
        List<String> roles = workflowStepRolesRepository.findActiveRolesByWorkflowMasterAndStepName(masterId, stepName);
        return roles != null ? roles : new ArrayList<>();
    }

    private boolean rolesIntersect(List<String> a, List<String> b) {
        if (a == null || b == null) {
            return false;
        }
        for (String r : a) {
            if (holdsRole(b, r)) {
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
