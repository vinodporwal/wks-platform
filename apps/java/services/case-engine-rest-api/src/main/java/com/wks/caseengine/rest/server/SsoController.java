package com.wks.caseengine.rest.server;

import java.text.ParseException;
import java.util.Map;
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

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/sso")
@Slf4j
public class SsoController {

    private static final Map<String, String> userSessionMap = new ConcurrentHashMap<>();

    @Value("${sso.allowed-origin:http://localhost:3000}")
    private String allowedOrigin;

    @Value("${keycloak.realm:${KEYCLOAK_REALM_NAME:localhost}}")
    private String defaultRealm;

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

        // Use org from token; check ext.tenant_id as fallback; then default realm
        String org = (String) claims.getClaim("org");
        if (org == null || org.isBlank()) {
            try {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> ext = (java.util.Map<String, Object>) claims.getClaim("ext");
                if (ext != null) {
                    org = (String) ext.get("tenant_id");
                }
            } catch (Exception ignored) {}
        }
        if (org == null || org.isBlank()) {
            org = defaultRealm;
            log.info("SSO: no org/tenant_id in token, using default realm: {}", org);
        }

        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            String existingUserId = (String) existingSession.getAttribute("userId");
            if (userId.equals(existingUserId)) {
                log.debug("SSO reusing existing session for user: {}", userId);
                long tokenExp = claims.getExpirationTime().getTime();
                int maxAge = (int) ((tokenExp - System.currentTimeMillis()) / 1000);
                existingSession.setMaxInactiveInterval(maxAge);
                return ResponseEntity.ok(Map.of("status", "reused", "userId", userId));
            }
            existingSession.invalidate();
        }

        HttpSession session = request.getSession(true);
        session.setAttribute("userId", userId);
        session.setAttribute("org", org);
        session.setAttribute("token", token);

        long tokenExp = claims.getExpirationTime().getTime();
        int maxAge = (int) ((tokenExp - System.currentTimeMillis()) / 1000);
        session.setMaxInactiveInterval(maxAge);

        userSessionMap.put(userId, session.getId());

        // Use a distinct cookie name to avoid colliding with the standard JSESSIONID
        // used by standalone WKS users on the same domain
        String cookieValue = "WKS_SSO_SESSION=" + session.getId()
                + "; Path=/; HttpOnly; SameSite=None; Secure";
        response.setHeader("Set-Cookie", cookieValue);

        log.info("SSO session created for user: {}, org: {}", userId, org);
        return ResponseEntity.ok(Map.of("status", "created", "userId", userId));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String userId = (String) session.getAttribute("userId");
            userSessionMap.remove(userId);
            session.invalidate();
            log.info("SSO session invalidated for user: {}", userId);
        }

        Cookie cookie = new Cookie("JSESSIONID", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of("status", "logged_out"));
    }
}
