package com.wks.bpm.engine.camunda.plugin.aop;

/**
 * Compatibility alias for process definitions deployed before the listeners
 * moved to {@code com.wks.bpm.externaltask.plugin}. Camunda resolves
 * {@code camunda:class} by FQCN; keep this type on the engine classpath so
 * already-deployed {@code AOP_Approval_v2} versions keep starting.
 */
public class AopRoleCollectionListener
        extends com.wks.bpm.externaltask.plugin.AopRoleCollectionListener {
}
