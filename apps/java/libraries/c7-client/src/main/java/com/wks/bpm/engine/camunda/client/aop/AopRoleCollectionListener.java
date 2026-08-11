package com.wks.bpm.engine.camunda.client.aop;

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;

/**
 * Expands the AOP gate role strings into real {@link List} variables at process
 * start.
 *
 * <p>Each gate fans out multi-instance over its approver roles. The roles arrive
 * from WorkflowStepRoles as a single comma-delimited String variable
 * ({@code gate1Roles} = "operation_head,cts_head"), because that is what the
 * REST start API can carry as a plain variable.</p>
 *
 * <p>The BPMN cannot split that inline: {@code ${gate1Roles.split(',')}} returns
 * a {@code String[]}, and Camunda's multi-instance collection rejects arrays with
 * "didn't resolve to type 'Collection'". So we split here, once, at process start
 * and publish {@code gate1RoleList}... which the loop characteristics reference by
 * name. The variables persist for the life of the instance, so revert loops back
 * through a gate reuse them without re-running this listener.</p>
 *
 * <p>Plain Java on purpose — Camunda runs standalone here, so Spring
 * {@code ${delegateExpression}} beans are not reachable from the engine.</p>
 */
public class AopRoleCollectionListener implements ExecutionListener {

    /** Source variables to expand; each becomes "<name>List". */
    private static final String[] ROLE_VARS = {
            "prepareRoles", "gate1Roles", "gate2Roles", "gate3Roles", "gate4Roles", "gate5Roles"
    };

    @Override
    public void notify(DelegateExecution execution) {
        for (String var : ROLE_VARS) {
            execution.setVariable(listNameOf(var), toList(execution.getVariable(var)));
        }
    }

    /** "gate1Roles" -> "gate1RoleList", "prepareRoles" -> "prepareRoleList". */
    private static String listNameOf(String varName) {
        return varName.substring(0, varName.length() - "s".length()) + "List";
    }

    /**
     * Split a comma-delimited role string into a trimmed, blank-free List. A value
     * that is already a Collection is passed through; null/blank yields an empty
     * list, which makes the gate a no-op rather than an engine error.
     */
    private static List<String> toList(Object raw) {
        List<String> roles = new ArrayList<>();
        if (raw == null) {
            return roles;
        }
        if (raw instanceof Iterable) {
            for (Object o : (Iterable<?>) raw) {
                if (o != null && !o.toString().trim().isEmpty()) {
                    roles.add(o.toString().trim());
                }
            }
            return roles;
        }
        for (String part : raw.toString().split(",")) {
            String role = part.trim();
            if (!role.isEmpty()) {
                roles.add(role);
            }
        }
        return roles;
    }
}
