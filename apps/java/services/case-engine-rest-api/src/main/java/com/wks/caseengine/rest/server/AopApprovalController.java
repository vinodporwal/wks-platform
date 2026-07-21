package com.wks.caseengine.rest.server;

import java.util.List;
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
import com.wks.caseengine.service.WorkflowServiceImpl;

/**
 * REST API for the isolated AOP approval workflow. Distinct base path
 * ({@code /aop-approval}) so nothing here overlaps the shared {@code /task}
 * endpoints or TCS. Caller identity (user id + roles) always comes from the JWT.
 */
@RestController
@RequestMapping("aop-approval")
public class AopApprovalController {

    @Autowired
    private AopApprovalWorkflowService aopApprovalWorkflowService;

    @Autowired
    private AopApprovalAuditService aopApprovalAuditService;

    /** Start a new AOP approval workflow for a plant + year (409 if one exists). */
    @PostMapping(value = "/start")
    public WorkflowDTO start(@RequestParam String plantId, @RequestParam String year) {
        return aopApprovalWorkflowService.start(plantId, year, currentUserId());
    }

    /** Apply a gate decision (approve / revert) with an optional remark. */
    @PostMapping(value = "/act")
    public ResponseEntity<Void> act(@RequestBody AopActRequestDTO req) {
        aopApprovalWorkflowService.act(req.getTaskId(), req.getPlantId(), req.getYear(), req.getGateName(),
                req.getDecision(), req.getRemark(), currentUserId(), req.getActorRole(),
                WorkflowServiceImpl.extractRoles());
        return ResponseEntity.noContent().build();
    }

    /** Status + server-computed button state for a single (plant, year). */
    @GetMapping(value = "/status")
    public AopWorkflowStatusDTO status(@RequestParam String plantId, @RequestParam String year) {
        return aopApprovalWorkflowService.getStatus(plantId, year, currentUserId(), WorkflowServiceImpl.extractRoles());
    }

    /** The caller's "My Approvals" inbox across all plants. */
    @GetMapping(value = "/my-pending")
    public List<AopPendingItemDTO> myPending() {
        return aopApprovalWorkflowService.getMyPending(currentUserId(), WorkflowServiceImpl.extractRoles());
    }

    /** Full audit trail for a plant + year. */
    @GetMapping(value = "/audit-trail")
    public List<AopApprovalHistoryDTO> auditTrail(@RequestParam String plantId, @RequestParam String year) {
        return aopApprovalAuditService.getAuditTrail(UUID.fromString(plantId), year);
    }

    /** Resolve the caller's user id from the JWT (preferred_username, else sub). */
    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
            String username = jwt.getClaimAsString("preferred_username");
            return username != null ? username : jwt.getClaimAsString("sub");
        }
        return null;
    }
}
