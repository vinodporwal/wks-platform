package com.wks.caseengine.aop;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;

/**
 * After a GMS Head (Gate 5) rejection, Functional Heads (Gate 2) stay skipped
 * until the plan is fully approved. Later Site Head / Gate 4 reverts do not
 * put Gate 2 back in the chain.
 *
 * <p>{@code shortLoopPhase = ACTIVE} is set on Gate 5 revert and is not cleared
 * while the process is running. Gate 1 then routes Plant Manager approve
 * directly to Gate 3.</p>
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

    /** Injected from {@code camunda:field name="action"}. */
    private String action;

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public void notify(DelegateExecution execution) {
        String act = action != null && !action.isBlank()
                ? action
                : execution.getCurrentTransitionId();
        if (act == null) {
            return;
        }
        if (ACTION_GATE5_REVERT.equals(act) || "Flow_gw5_revert".equals(act)) {
            execution.setVariable(VAR, ACTIVE);
        }
        // Gate 3 approve/revert must not clear the flag — skip lasts until approved.
    }
}
