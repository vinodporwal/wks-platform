package com.wks.integration.honeywell;

import java.util.List;
import com.wks.integration.honeywell.SecurityManagementModels.*;

/** Integration boundary for the five read operations demonstrated by the client sample. */
public interface SecurityManagementClient {
    List<ScopeInfo> getAllScopes();
    List<ScopeRoleMembersResult> getScopeRoleMembers(ScopeRoleMembersRequest request);
    List<ScopeContextResult> getScopeContexts(ScopeContextsRequest request);
    List<UserAssignedOperation> getUserAssignedOperationsByScope(UserAssignedOperationsRequest request);
    List<ScopesContextForUserResult> getScopesContextForUser(ScopesContextForUserRequest request);
}
