package com.wks.caseengine.rest.server;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.dto.AopActRequestDTO;
import com.wks.caseengine.dto.AopPendingItemDTO;
import com.wks.caseengine.dto.AopWorkflowStatusDTO;
import com.wks.caseengine.dto.AopApprovalHistoryDTO;
import com.wks.caseengine.dto.WorkflowDTO;
import com.wks.caseengine.service.AopApprovalAuditService;
import com.wks.caseengine.service.AopApprovalWorkflowService;
import com.wks.caseengine.service.KeycloakUserService;
import com.wks.caseengine.service.WorkflowServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * REST API for the isolated AOP approval workflow. Distinct base path
 * ({@code /aop-approval}) so nothing here overlaps the shared {@code /task}
 * endpoints or TCS. Caller identity is the JWT user; roles are JWT plus live
 * Keycloak so an admin role change is visible without a token refresh.
 */
@Slf4j
@RestController
@RequestMapping("aop-approval")
public class AopApprovalController {

    @Autowired
    private AopApprovalWorkflowService aopApprovalWorkflowService;

    @Autowired
    private AopApprovalAuditService aopApprovalAuditService;

    @Autowired
    private KeycloakUserService keycloakUserService;

    /** Start a new AOP approval workflow for a plant + year (409 if one exists). */
    @PostMapping(value = "/start")
    public WorkflowDTO start(@RequestParam String plantId,
            @RequestParam String year,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) String actorRole,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        String resolvedRemark = remark;
        String resolvedRole = actorRole;
        if (body != null) {
            if (resolvedRemark == null || resolvedRemark.isBlank()) {
                resolvedRemark = body.get("remark");
            }
            if (resolvedRole == null || resolvedRole.isBlank()) {
                resolvedRole = body.get("actorRole");
            }
        }
        return aopApprovalWorkflowService.start(plantId, year, currentUserId(), resolvedRemark, resolvedRole);
    }

    /** Apply a gate decision (approve / revert) with an optional remark. */
    @PostMapping(value = "/act")
    public ResponseEntity<Void> act(@RequestBody AopActRequestDTO req) {
        aopApprovalWorkflowService.act(req.getTaskId(), req.getPlantId(), req.getYear(), req.getGateName(),
                req.getDecision(), req.getRemark(), currentUserId(), req.getActorRole(),
                currentRoles());
        return ResponseEntity.noContent().build();
    }

    /** Status + server-computed button state for a single (plant, year). */
    @GetMapping(value = "/status")
    public AopWorkflowStatusDTO status(@RequestParam String plantId, @RequestParam String year) {
        return aopApprovalWorkflowService.getStatus(plantId, year, currentUserId(), currentRoles());
    }

    /** The caller's "My Approvals" inbox across all plants. */
    @GetMapping(value = "/my-pending")
    public List<AopPendingItemDTO> myPending() {
        return aopApprovalWorkflowService.getMyPending(currentUserId(), currentRoles());
    }

    /** Full audit trail for a plant + year. */
    @GetMapping(value = "/audit-trail")
    public List<AopApprovalHistoryDTO> auditTrail(@RequestParam String plantId, @RequestParam String year) {
        return aopApprovalAuditService.getAuditTrail(UUID.fromString(plantId), year);
    }

    /**
     * JWT roles plus live Keycloak realm roles. Admin can add/remove any approver
     * role mid-workflow; eligibility must see that change without a token refresh.
     */
    private List<String> currentRoles() {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        addRoles(merged, WorkflowServiceImpl.extractRoles());
        addResourceAccessRoles(merged);
        try {
            addRoles(merged, keycloakUserService.getEffectiveRealmRoleNames(currentUserSub(), currentUserId()));
        } catch (Exception ex) {
            log.warn("AOP: live Keycloak roles unavailable for {}: {}", currentUserId(), ex.getMessage());
        }
        return new ArrayList<>(merged);
    }

    private void addRoles(LinkedHashSet<String> into, List<String> roles) {
        if (roles == null) {
            return;
        }
        for (String role : roles) {
            if (role != null && !role.isBlank()) {
                into.add(role);
            }
        }
    }

    private void addResourceAccessRoles(LinkedHashSet<String> into) {
        Jwt jwt = currentJwt();
        if (jwt == null) {
            return;
        }
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess == null) {
            return;
        }
        for (Object value : resourceAccess.values()) {
            if (!(value instanceof Map<?, ?> clientAccess)) {
                continue;
            }
            Object rolesObj = clientAccess.get("roles");
            if (!(rolesObj instanceof List<?> raw)) {
                continue;
            }
            for (Object item : raw) {
                if (item instanceof String name && !name.isBlank()) {
                    into.add(name);
                }
            }
        }
    }

    /** Resolve the caller's user id from the JWT (preferred_username, else sub). */
    private String currentUserId() {
        Jwt jwt = currentJwt();
        if (jwt == null) {
            return null;
        }
        String username = jwt.getClaimAsString("preferred_username");
        return username != null ? username : jwt.getClaimAsString("sub");
    }

    private String currentUserSub() {
        Jwt jwt = currentJwt();
        return jwt != null ? jwt.getClaimAsString("sub") : null;
    }

    private Jwt currentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        }
        return null;
    }
}
