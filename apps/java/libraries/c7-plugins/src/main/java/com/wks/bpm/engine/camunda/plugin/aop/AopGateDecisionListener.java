package com.wks.bpm.engine.camunda.plugin.aop;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.RuntimeService;

/**
 * In-engine task listener that aggregates a multi-instance gate's per-approver
 * decisions into a single process variable {@code <gateName>Result} that the
 * downstream exclusive gateway routes on.
 *
 * <p>Runs INSIDE the standalone Camunda server (referenced from the BPMN via
 * {@code camunda:class}), the same way {@code NotifyAssigneeTaskListener} does —
 * because Spring beans in case-engine-rest-api are not reachable from the
 * engine. It uses only the engine's own {@link RuntimeService}, no Spring / no
 * DB.</p>
 *
 * <p>Policy = ALL must approve to advance; <b>any single REVERTED exits early</b>:</p>
 * <ul>
 *   <li><b>create</b> — fires as each instance task is created at gate entry
 *       (all creates precede any completion), resetting {@code <gate>Result} so
 *       a stale value from a previous visit (after a revert) cannot leak in.</li>
 *   <li><b>complete</b> — each approver's decision is folded in with
 *       REVERTED-wins: once any instance reverts the gate result is REVERTED and
 *       stays there; only if every instance approves does it remain APPROVED.
 *       The BPMN multi-instance {@code completionCondition} on each gate exits
 *       as soon as {@code <gate>Result == 'REVERTED'}, cancelling sibling tasks
 *       so the exclusive gateway can route without waiting for the full committee.</li>
 * </ul>
 *
 * <p>The per-approver choice is read from the task-local variable
 * {@code decision} ("APPROVED" | "REVERTED"), supplied by the frontend when it
 * completes the task. Anything other than an explicit "REVERTED" counts as
 * APPROVED (the forward path).</p>
 */
public class AopGateDecisionListener implements TaskListener {

    private static final String APPROVED = "APPROVED";
    private static final String REVERTED = "REVERTED";
    private static final String DECISION_VAR = "decision";
    private static final String RESULT_SUFFIX = "Result";

    @Override
    public void notify(DelegateTask delegateTask) {
        String gateName = delegateTask.getTaskDefinitionKey();
        if (gateName == null || gateName.isEmpty()) {
            return;
        }
        String resultVar = gateName + RESULT_SUFFIX;
        String eventName = delegateTask.getEventName();

        RuntimeService runtimeService = delegateTask.getProcessEngineServices().getRuntimeService();
        String processInstanceId = delegateTask.getProcessInstanceId();

        if (TaskListener.EVENTNAME_CREATE.equals(eventName)) {
            // Reset the gate result at entry (before any completion) so a prior
            // visit's value cannot survive a revert-and-re-enter cycle.
            runtimeService.setVariable(processInstanceId, resultVar, null);
            return;
        }

        if (TaskListener.EVENTNAME_COMPLETE.equals(eventName)) {
            Object current = runtimeService.getVariable(processInstanceId, resultVar);
            if (REVERTED.equals(current)) {
                // REVERTED wins — nothing more to decide for this gate.
                return;
            }

            Object decisionObj = delegateTask.getVariable(DECISION_VAR);
            String decision = decisionObj != null ? decisionObj.toString() : null;

            if (REVERTED.equalsIgnoreCase(decision)) {
                runtimeService.setVariable(processInstanceId, resultVar, REVERTED);
            } else {
                runtimeService.setVariable(processInstanceId, resultVar, APPROVED);
            }
        }
    }
}
