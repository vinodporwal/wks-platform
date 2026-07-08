package com.wks.caseengine.rest.server;

import java.text.ParseException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import com.wks.caseengine.cases.definition.service.KeycloakService;
import com.wks.api.security.SsoSessionStore;

@RestController
@RequestMapping("/sso")
@Slf4j
public class SsoController {

    @Value("${sso.allowed-origin:http://localhost:3000}")
    private String allowedOrigin;

    @Value("${keycloak.realm:${KEYCLOAK_REALM_NAME:localhost}}")
    private String defaultRealm;

    @Autowired
    private KeycloakService keycloakService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing token");
        }

        JWTClaimsSet claims;
        try {
            claims = JWTParser.parse(token).getJWTClaimsSet();
        } catch (ParseException e) {
            log.warn("SSO login failed - invalid token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        if (claims.getExpirationTime() == null ||
                claims.getExpirationTime().getTime() < System.currentTimeMillis()) {
            log.warn("SSO login failed - token expired for sub: {}", claims.getSubject());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token expired");
        }

        String userId = claims.getSubject();
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token claims");
        }

        // Resolve org/tenant — fall back to default realm if APM token has no org claim
        String org = (String) claims.getClaim("org");
        if (org == null || org.isBlank()) {
            org = defaultRealm;
            log.info("SSO: no org claim in token, using default realm: {}", org);
        }

        // Generate a unique SSO session ID
        String ssoSessionId = UUID.randomUUID().toString();

        // Store session data in shared map — read by SsoSessionAuthFilter and InjectorTenantHandlerInterceptor
        Map<String, String> sessionData = new ConcurrentHashMap<>();
        sessionData.put("userId", userId);
        sessionData.put("org", org);
        sessionData.put("token", token);
        SsoSessionStore.STORE.put(ssoSessionId, sessionData);

        // Set WKS_SSO_SESSION cookie — separate from JSESSIONID to avoid
        // colliding with standalone WKS users on the same domain
        String cookieValue = "WKS_SSO_SESSION=" + ssoSessionId
                + "; Path=/; HttpOnly; SameSite=None; Secure";
        response.setHeader("Set-Cookie", cookieValue);

        log.info("SSO session created for user: {}, org: {}", userId, org);
        return ResponseEntity.ok(Map.of("status", "created", "userId", userId));
    }

    @GetMapping("/userinfo")
    public ResponseEntity<?> userinfo(HttpServletRequest request) {
        String ssoSessionId = getCookieValue(request, "WKS_SSO_SESSION");
        if (ssoSessionId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No SSO session");
        }

        Map<String, String> sessionData = SsoSessionStore.STORE.get(ssoSessionId);
        if (sessionData == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("SSO session not found");
        }

        String userId = sessionData.get("userId");
        String idpAlias = sessionData.getOrDefault("idpAlias", "oidc");
        try {
            Map<String, Object> user = keycloakService.getUserByFederatedId(userId, idpAlias);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found in Keycloak for sub: " + userId);
            }

            String keycloakId = (String) user.get("id");
            String firstName = (String) user.getOrDefault("firstName", "");
            String lastName = (String) user.getOrDefault("lastName", "");

            Map<String, Object> info = new java.util.HashMap<>();
            info.put("sub", keycloakId);
            info.put("name", (firstName + " " + lastName).trim());
            info.put("given_name", firstName);
            info.put("family_name", lastName);
            info.put("email", user.getOrDefault("email", ""));
            info.put("preferred_username", user.getOrDefault("username", ""));

            // wks-portal client roles
            java.util.List<String> clientRoles = keycloakService.getClientRolesForUser(keycloakId, "wks-portal");
            info.put("wks_portal_roles", clientRoles);

            log.info("SSO userinfo success for sub={} keycloakId={} roles={}", userId, keycloakId, clientRoles);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.warn("SSO userinfo lookup failed for userId {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to fetch user info: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String ssoSessionId = getCookieValue(request, "WKS_SSO_SESSION");
        if (ssoSessionId != null) {
            SsoSessionStore.STORE.remove(ssoSessionId);
            log.info("SSO session removed: {}", ssoSessionId);
        }

        String clearCookie = "WKS_SSO_SESSION=; Path=/; HttpOnly; SameSite=None; Secure; Max-Age=0";
        response.setHeader("Set-Cookie", clearCookie);

        return ResponseEntity.ok(Map.of("status", "logged_out"));
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}
