package com.wks.integration.honeywell;

import static com.wks.integration.honeywell.SecurityManagementModels.*;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.function.Function;
import javax.net.ssl.SSLException;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.transport.http.HTTPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.wks.integration.honeywell.generated.arrays.ArrayOfstring;
import com.wks.integration.honeywell.generated.core.*;
import com.wks.integration.honeywell.generated.security.*;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.ws.WebFault;
import jakarta.xml.ws.soap.SOAPFaultException;

/** Reusable singleton proxy; its CXF request/transport context must remain immutable after construction. */
final class CxfSecurityManagementClient implements SecurityManagementClient, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(CxfSecurityManagementClient.class);
    private final SecurityMgmtContract port;
    private final Client cxfClient;

    CxfSecurityManagementClient(SecurityMgmtContract port, Client cxfClient) { this.port = port; this.cxfClient = cxfClient; }

    public List<ScopeInfo> getAllScopes() {
        return call("GetAllScopes", port::getAllScopes, value -> safe(value.getSecScopeDetail()).stream()
                .map(v -> new ScopeInfo(v.getName(), v.getTypeName(), value(v.getDescription()))).toList());
    }
    public List<ScopeRoleMembersResult> getScopeRoleMembers(ScopeRoleMembersRequest request) {
        if (request == null) throw new IllegalArgumentException("request must not be null");
        return call("GetScopeRoleMembers", () -> port.getScopeRoleMembers(SecurityManagementRequests.scopeRoleMembers(
                request.scopeNames(), request.roleNames(), request.allRoles())), value -> safe(value.getScopeRoleMembers()).stream()
                .map(v -> new ScopeRoleMembersResult(value(v.getScopeName()), value(v.getRoleName()),
                        v.getRoleMembers() == null || v.getRoleMembers().getValue() == null ? List.of() :
                        safe(v.getRoleMembers().getValue().getRoleMembers()).stream().map(CxfSecurityManagementClient::member).toList())).toList());
    }
    public List<ScopeContextResult> getScopeContexts(ScopeContextsRequest request) {
        if (request == null) throw new IllegalArgumentException("request must not be null");
        return call("GetScopeContexts", () -> port.getScopeContexts(SecurityManagementRequests.hierarchy(
                request.modelName(), request.hierarchyName())), value -> safe(value.getScopeContextResponseRoles()).stream().map(v -> {
                    ScopeHierarchyLocation h = v.getScopeHierarchyLocation();
                    List<String> roles = v.getRoleNames() == null ? List.of() : safe(v.getRoleNames().getRoleName()).stream().map(RoleName::getName).toList();
                    return new ScopeContextResult(h == null ? null : h.getScopeName(), h == null ? null : h.getHierarchyLocation(),
                            h == null ? null : h.getHierarchyExclusionList(), roles);
                }).toList());
    }
    public List<UserAssignedOperation> getUserAssignedOperationsByScope(UserAssignedOperationsRequest request) {
        if (request == null) throw new IllegalArgumentException("request must not be null");
        return call("GetUserAssignedOperationsByScope", () -> port.getUserAssignedOperationsByScope(
                SecurityManagementRequests.allUserAssignedOperationsByScope()), value -> safe(value.getOperations()).stream().map(v ->
                new UserAssignedOperation(v.getId(), v.getName(), value(v.getDescription()), v.getGroup(), v.getAccess(),
                        v.getOperationValue(), v.getScopes() == null || v.getScopes().getValue() == null ? List.of() :
                        safe(v.getScopes().getValue().getScope()).stream().map(Scope::getName).toList())).toList());
    }
    public List<ScopesContextForUserResult> getScopesContextForUser(ScopesContextForUserRequest request) {
        if (request == null) throw new IllegalArgumentException("request must not be null");
        ModelHierarchy h = request.hierarchy();
        return call("GetScopesContextForUser", () -> port.getScopesContextForUser(SecurityManagementRequests.scopesContextForUser(
                request.operationValues(), h == null ? null : SecurityManagementRequests.hierarchy(h.modelName(), h.hierarchyName()))),
                value -> safe(value.getScopeContextResponseOperations()).stream().map(v -> {
                    ScopeHierarchyLocation l = v.getScopeHierarchyLocation();
                    List<String> operations = v.getOperationValues() == null ? List.of() : safe(v.getOperationValues().getString());
                    return new ScopesContextForUserResult(l == null ? null : l.getScopeName(), l == null ? null : l.getHierarchyLocation(),
                            l == null ? null : l.getHierarchyExclusionList(), operations);
                }).toList());
    }

    private <T, R> R call(String operation, CheckedSupplier<T> action, Function<T, R> mapper) {
        long started = System.nanoTime();
        try {
            LOG.debug("Honeywell stage=HONEYWELL_SOAP_INVOKE_START operation={}", operation);
            T response = action.get();
            LOG.debug("Honeywell stage=HONEYWELL_SOAP_SUCCESS operation={} durationMs={}", operation, elapsed(started));
            if (response == null) throw new SecurityManagementException(SecurityManagementException.Category.INVALID_RESPONSE,
                    operation + " returned no response", null);
            try {
                R result = mapper.apply(response);
                LOG.debug("Honeywell stage=HONEYWELL_MAPPING_SUCCESS operation={} durationMs={}", operation, elapsed(started));
                return result;
            } catch (RuntimeException mappingFailure) {
                throw new SecurityManagementException(SecurityManagementException.Category.INVALID_RESPONSE,
                        operation + " response mapping failed", mappingFailure);
            }
        } catch (SecurityManagementException e) { throw e;
        } catch (Exception e) {
            SecurityManagementException.Category category = classify(e);
            LOG.warn("Honeywell operation={} failed category={} durationMs={}", operation, category, elapsed(started));
            throw new SecurityManagementException(category, operation + " failed", e);
        }
    }

    static SecurityManagementException.Category classify(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause())
            if (cause instanceof SecurityManagementException integrationFailure) return integrationFailure.getCategory();
        for (Throwable cause = error; cause != null; cause = cause.getCause())
            if (cause.getClass().isAnnotationPresent(WebFault.class) && cause.getClass().getName().startsWith("com.wks.integration.honeywell.generated."))
                return SecurityManagementException.Category.SOAP_FAULT;
        for (Throwable cause = error; cause != null; cause = cause.getCause()) if (cause instanceof SOAPFaultException) return SecurityManagementException.Category.SOAP_FAULT;
        for (Throwable cause = error; cause != null; cause = cause.getCause()) if (cause instanceof HTTPException http) {
            if (http.getResponseCode() == 401) return SecurityManagementException.Category.AUTHENTICATION;
            if (http.getResponseCode() == 403) return SecurityManagementException.Category.AUTHORIZATION;
            if (http.getResponseCode() >= 500) return SecurityManagementException.Category.CONNECTION;
        }
        for (Throwable cause = error; cause != null; cause = cause.getCause()) if (cause instanceof SocketTimeoutException) return SecurityManagementException.Category.TIMEOUT;
        for (Throwable cause = error; cause != null; cause = cause.getCause()) if (cause instanceof SSLException) return SecurityManagementException.Category.TLS;
        for (Throwable cause = error; cause != null; cause = cause.getCause()) if (cause instanceof ConnectException) return SecurityManagementException.Category.CONNECTION;
        for (Throwable cause = error; cause != null; cause = cause.getCause()) if (cause instanceof ResponseSizeLimitInterceptor.ResponseTooLargeException)
            return SecurityManagementException.Category.INVALID_RESPONSE;
        return SecurityManagementException.Category.UNEXPECTED;
    }

    @Override public void close() { cxfClient.destroy(); }
    private static long elapsed(long start) { return (System.nanoTime() - start) / 1_000_000; }
    private static RoleMemberInfo member(RoleMember v) { return new RoleMemberInfo(value(v.getName()), value(v.getSecurityIdentifier()),
            value(v.getType()), value(v.getDisplayName()), value(v.getEmail())); }
    private static <T> List<T> safe(List<T> values) { return values == null ? List.of() : values; }
    private static <T> T value(JAXBElement<T> value) { return value == null ? null : value.getValue(); }
    @FunctionalInterface private interface CheckedSupplier<T> { T get() throws Exception; }
}
