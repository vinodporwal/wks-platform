package com.wks.caseengine.aop;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.Expression;

/**
 * After a GMS Head (Gate 5) rejection, Functional Heads (Gate 2) stay skipped
 * until the plan is fully approved. Later Site Head / Gate 4 reverts do not
 * put Gate 2 back in the chain.
 *
 * <p>{@code shortLoopPhase = ACTIVE} is set on Gate 5 revert and is not cleared
 * while the process is running. Gate 1 then routes Plant Manager approve
 * directly to Gate 3.</p>
 *
 * <p>Canonical source lives in {@code case-engine}; this copy is kept in sync
 * for the engine classpath ({@code c7-plugins} userlib).</p>
 */
public class AopShortLoopListener implements ExecutionListener {

    public static final String VAR = "shortLoopPhase";
    public static final String NONE = "NONE";
    public static final String AWAITING = "AWAITING_PREPARER_CORRECTION";
    public static final String ACTIVE = "ACTIVE";
    public static final String STATUS_TAG = "Awaiting Preparer correction";

    public static final String ACTION_GATE5_REVERT = "GATE5_REVERT";
    public static final String ACTION_GATE3_REVERT = "GATE3_REVERT";
    public static final String ACTION_GATE3_APPROVE = "GATE3_APPROVE";

    /**
     * Injected from {@code camunda:field name="action"}. Camunda supplies a
     * {@link Expression} ({@code FixedValue}), not a String.
     */
    private Expression action;

    public void setAction(Expression action) {
        this.action = action;
    }

    @Override
    public void notify(DelegateExecution execution) {
        String act = readAction(execution);
        if (act == null) {
            return;
        }
        if (ACTION_GATE5_REVERT.equals(act) || "Flow_gw5_revert".equals(act)) {
            execution.setVariable(VAR, ACTIVE);
        }
        // Gate 3 approve/revert must not clear the flag — skip lasts until approved.
    }

    private String readAction(DelegateExecution execution) {
        if (action != null) {
            Object val = action.getValue(execution);
            if (val != null) {
                String text = val.toString();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return execution.getCurrentTransitionId();
    }
}
