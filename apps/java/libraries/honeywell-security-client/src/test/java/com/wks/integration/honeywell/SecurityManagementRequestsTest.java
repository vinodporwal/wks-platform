package com.wks.integration.honeywell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.List;
import org.junit.jupiter.api.Test;

class SecurityManagementRequestsTest {
    @Test void buildsDynamicScopeRoleQuery() {
        var query = SecurityManagementRequests.scopeRoleMembers(List.of("scope-a"), List.of("role-a"), true);
        assertThat(query.getScopeNames().getValue().getString()).containsExactly("scope-a");
        assertThat(query.getRoleNames().getValue().getString()).containsExactly("role-a");
        assertThat(query.isAllRoles()).isTrue();
    }
    @Test void rejectsBlankBusinessValues() {
        assertThatThrownBy(() -> SecurityManagementRequests.hierarchy(" ", "h"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("modelName");
        assertThatThrownBy(() -> new SecurityManagementModels.ScopeRoleMembersRequest(List.of(" "), List.of(), false))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("scope name");
    }
    @Test void buildsScopeContextsAndQueryAllRequests() {
        var hierarchy = SecurityManagementRequests.hierarchy("model", "hierarchy");
        assertThat(hierarchy.getModelName()).isEqualTo("model"); assertThat(hierarchy.getHierarchyName()).isEqualTo("hierarchy");
        var all = SecurityManagementRequests.allUserAssignedOperationsByScope();
        assertThat(all.getQueryOperationsChoise().getValue().getQueryAll().getValue()).isTrue();
        assertThat(all.getSearchScopes()).isNull();
    }
    @Test void buildsScopesContextForUserWithOptionalHierarchy() {
        var absent = SecurityManagementRequests.scopesContextForUser(List.of("read"), null);
        assertThat(absent.getOperationValues().getString()).containsExactly("read"); assertThat(absent.getModelHierarchyName()).isNull();
        var present = SecurityManagementRequests.scopesContextForUser(List.of("read"), SecurityManagementRequests.hierarchy("m", "h"));
        assertThat(present.getModelHierarchyName().getHierarchyName()).isEqualTo("h");
    }
}
