package com.wks.integration.honeywell;

import java.util.Collection;
import java.util.Objects;

import com.wks.integration.honeywell.generated.arrays.ArrayOfstring;
import com.wks.integration.honeywell.generated.core.ModelHierarchyName;
import com.wks.integration.honeywell.generated.core.ObjectFactory;
import com.wks.integration.honeywell.generated.core.QueryOperationsChoise;
import com.wks.integration.honeywell.generated.core.QueryScopeRoleMembers;
import com.wks.integration.honeywell.generated.core.ScopeContextRequestOperations;
import com.wks.integration.honeywell.generated.core.UserOperationsReqest;

/** Null-safe request builders; no sample scope, hierarchy, role, or operation value is embedded here. */
final class SecurityManagementRequests {
    private static final ObjectFactory FACTORY = new ObjectFactory();

    private SecurityManagementRequests() { }

    static QueryScopeRoleMembers scopeRoleMembers(Collection<String> scopeNames,
            Collection<String> roleNames, boolean allRoles) {
        QueryScopeRoleMembers query = new QueryScopeRoleMembers();
        query.setScopeNames(FACTORY.createQueryScopeRoleMembersScopeNames(strings(scopeNames)));
        query.setRoleNames(FACTORY.createQueryScopeRoleMembersRoleNames(strings(roleNames)));
        query.setAllRoles(allRoles);
        return query;
    }

    static ModelHierarchyName hierarchy(String modelName, String hierarchyName) {
        ModelHierarchyName hierarchy = new ModelHierarchyName();
        hierarchy.setModelName(requireText(modelName, "modelName"));
        hierarchy.setHierarchyName(requireText(hierarchyName, "hierarchyName"));
        return hierarchy;
    }

    static ScopeContextRequestOperations scopesContextForUser(Collection<String> operationValues,
            ModelHierarchyName optionalHierarchy) {
        ScopeContextRequestOperations request = new ScopeContextRequestOperations();
        request.setOperationValues(strings(operationValues));
        request.setModelHierarchyName(optionalHierarchy);
        return request;
    }

    /** Contract-safe QueryAll form. Combining QueryAll with scoped criteria remains a future business decision. */
    static UserOperationsReqest allUserAssignedOperationsByScope() {
        QueryOperationsChoise choice = new QueryOperationsChoise();
        choice.setQueryAll(FACTORY.createQueryOperationsChoiseQueryAll(true));
        UserOperationsReqest request = new UserOperationsReqest();
        request.setQueryOperationsChoise(FACTORY.createUserOperationsReqestQueryOperationsChoise(choice));
        return request;
    }

    private static ArrayOfstring strings(Collection<String> values) {
        Objects.requireNonNull(values, "values");
        ArrayOfstring result = new ArrayOfstring();
        values.stream().map(value -> requireText(value, "list entry")).forEach(result.getString()::add);
        return result;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
