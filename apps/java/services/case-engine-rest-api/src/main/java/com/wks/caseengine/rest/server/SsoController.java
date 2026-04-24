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

    // In-memory store: sessionId -> userId (prevents duplicate sessions per user)
    private static final Map<String, String> userSessionMap = new ConcurrentHashMap<>();

    @Value("${sso.allowed-origin:http://localhost:3000}")
    private String allowedOrigin;

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

        // Check token expiry
        if (claims.getExpirationTime() == null ||
                claims.getExpirationTime().getTime() < System.currentTimeMillis()) {
            log.warn("SSO login failed - token expired for sub: {}", claims.getSubject());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token expired");
        }

        String userId = claims.getSubject();
        String org = (String) claims.getClaim("org");

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token claims");
        }

        // Reuse existing session if already valid for this user
        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            String existingUserId = (String) existingSession.getAttribute("userId");
            if (userId.equals(existingUserId)) {
                log.debug("SSO reusing existing session for user: {}", userId);
                // Refresh session expiry to match token expiry
                long tokenExp = claims.getExpirationTime().getTime();
                int maxAge = (int) ((tokenExp - System.currentTimeMillis()) / 1000);
                existingSession.setMaxInactiveInterval(maxAge);
                return ResponseEntity.ok(Map.of("status", "reused", "userId", userId));
            }
            // Different user on same session - invalidate it
            existingSession.invalidate();
        }

        // Create new session
        HttpSession session = request.getSession(true);
        session.setAttribute("userId", userId);
        session.setAttribute("org", org != null ? org : "");
        session.setAttribute("token", token);

        // Tie session lifetime to token expiry
        long tokenExp = claims.getExpirationTime().getTime();
        int maxAge = (int) ((tokenExp - System.currentTimeMillis()) / 1000);
        session.setMaxInactiveInterval(maxAge);

        // Track user->session mapping
        userSessionMap.put(userId, session.getId());

        // Set session cookie as HttpOnly + SameSite=None for iframe cross-site context
        String cookieValue = "JSESSIONID=" + session.getId()
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

        // Clear the cookie
        Cookie cookie = new Cookie("JSESSIONID", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of("status", "logged_out"));
    }
}
