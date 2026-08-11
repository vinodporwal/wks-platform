package com.wks.integration.honeywell;

import java.util.List;

/** Minimal, transport-independent models for the five supported read operations. */
public final class SecurityManagementModels {
    private SecurityManagementModels() { }

    public record ScopeInfo(String name, String typeName, String description) { }
    public record ScopeRoleMembersRequest(List<String> scopeNames, List<String> roleNames, boolean allRoles) {
        public ScopeRoleMembersRequest { scopeNames = copy(scopeNames, "scope name"); roleNames = copy(roleNames, "role name"); }
    }
    public record RoleMemberInfo(String name, String securityIdentifier, String type, String displayName, String email) { }
    public record ScopeRoleMembersResult(String scopeName, String roleName, List<RoleMemberInfo> members) {
        public ScopeRoleMembersResult { members = List.copyOf(members); }
    }
    public record ScopeContextsRequest(String modelName, String hierarchyName) {
        public ScopeContextsRequest { modelName = text(modelName, "model name"); hierarchyName = text(hierarchyName, "hierarchy name"); }
    }
    public record ScopeContextResult(String scopeName, String hierarchyLocation, String hierarchyExclusionList,
                                     List<String> roleNames) {
        public ScopeContextResult { roleNames = List.copyOf(roleNames); }
    }
    /** Marker for the only contract-safe form currently exposed: QueryAll=true with no scoped criteria. */
    public record UserAssignedOperationsRequest() { }
    public record UserAssignedOperation(int id, String name, String description, String group, String access,
                                        String operationValue, List<String> scopeNames) {
        public UserAssignedOperation { scopeNames = List.copyOf(scopeNames); }
    }
    public record ModelHierarchy(String modelName, String hierarchyName) {
        public ModelHierarchy { modelName = text(modelName, "model name"); hierarchyName = text(hierarchyName, "hierarchy name"); }
    }
    public record ScopesContextForUserRequest(List<String> operationValues, ModelHierarchy hierarchy) {
        public ScopesContextForUserRequest { operationValues = copy(operationValues, "operation value"); }
    }
    public record ScopesContextForUserResult(String scopeName, String hierarchyLocation,
                                             String hierarchyExclusionList, List<String> operationValues) {
        public ScopesContextForUserResult { operationValues = List.copyOf(operationValues); }
    }

    private static List<String> copy(List<String> values, String field) {
        if (values == null) throw new IllegalArgumentException(field + " list must not be null");
        values.forEach(value -> text(value, field));
        return List.copyOf(values);
    }
    private static String text(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}
