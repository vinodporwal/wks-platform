package com.wks.caseengine.rest.server;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

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

    @Value("${keycloak.url:http://localhost:8082}")
    private String keycloakUrl;

    @Value("${keycloak.realm:localhost}")
    private String keycloakRealm;

    @Value("${keycloak.client-id:wks-portal}")
    private String keycloakClientId;

    @Value("${keycloak.username:admin}")
    private String keycloakUsername;

    @Value("${keycloak.password:admin}")
    private String keycloakPassword;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        String apmToken = body.get("token");
        if (apmToken == null || apmToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing token");
        }

        JWTClaimsSet claims;
        try {
            claims = JWTParser.parse(apmToken).getJWTClaimsSet();
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
                long tokenExp = claims.getExpirationTime().getTime();
                int maxAge = (int) ((tokenExp - System.currentTimeMillis()) / 1000);
                existingSession.setMaxInactiveInterval(maxAge);
                String cachedWksToken = (String) existingSession.getAttribute("wksToken");
                return ResponseEntity.ok(Map.of("status", "reused", "userId", userId, "wksToken", cachedWksToken != null ? cachedWksToken : ""));
            }
            existingSession.invalidate();
        }

        // Exchange APM token for a WKS Keycloak token using resource owner password grant
        String wksToken;
        try {
            wksToken = fetchWksToken(apmToken, claims);
        } catch (Exception e) {
            log.error("Failed to fetch WKS token for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Token exchange failed: " + e.getMessage());
        }

        // Create new session
        HttpSession session = request.getSession(true);
        session.setAttribute("userId", userId);
        session.setAttribute("org", org != null ? org : "");
        session.setAttribute("apmToken", apmToken);
        session.setAttribute("wksToken", wksToken);

        long tokenExp = claims.getExpirationTime().getTime();
        int maxAge = (int) ((tokenExp - System.currentTimeMillis()) / 1000);
        session.setMaxInactiveInterval(maxAge);

        userSessionMap.put(userId, session.getId());

        String cookieValue = "JSESSIONID=" + session.getId()
                + "; Path=/; HttpOnly; SameSite=None; Secure";
        response.setHeader("Set-Cookie", cookieValue);

        log.info("SSO session created for user: {}, org: {}", userId, org);
        return ResponseEntity.ok(Map.of("status", "created", "userId", userId, "wksToken", wksToken));
    }

    /**
     * Fetches a WKS Keycloak token.
     * Tries token exchange (RFC 8693) first; falls back to default user credentials.
     */
    private String fetchWksToken(String apmToken, JWTClaimsSet claims) {
        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", keycloakUrl, keycloakRealm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Try token exchange first (requires Keycloak token-exchange feature enabled)
        try {
            MultiValueMap<String, String> exchangeParams = new LinkedMultiValueMap<>();
            exchangeParams.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
            exchangeParams.add("client_id", keycloakClientId);
            exchangeParams.add("subject_token", apmToken);
            exchangeParams.add("subject_token_type", "urn:ietf:params:oauth:token-type:access_token");
            exchangeParams.add("requested_token_type", "urn:ietf:params:oauth:token-type:access_token");

            HttpEntity<MultiValueMap<String, String>> exchangeRequest = new HttpEntity<>(exchangeParams, headers);
            Map<?, ?> exchangeResponse = restTemplate.postForObject(tokenUrl, exchangeRequest, Map.class);
            if (exchangeResponse != null && exchangeResponse.containsKey("access_token")) {
                log.info("SSO token exchange succeeded");
                return (String) exchangeResponse.get("access_token");
            }
        } catch (Exception e) {
            log.warn("Token exchange failed, falling back to password grant: {}", e.getMessage());
        }

        // Fallback: use configured default user credentials
        MultiValueMap<String, String> passwordParams = new LinkedMultiValueMap<>();
        passwordParams.add("grant_type", "password");
        passwordParams.add("client_id", keycloakClientId);
        passwordParams.add("username", keycloakUsername);
        passwordParams.add("password", keycloakPassword);

        HttpEntity<MultiValueMap<String, String>> passwordRequest = new HttpEntity<>(passwordParams, headers);
        Map<?, ?> passwordResponse = restTemplate.postForObject(tokenUrl, passwordRequest, Map.class);
        if (passwordResponse != null && passwordResponse.containsKey("access_token")) {
            log.info("SSO password grant fallback succeeded");
            return (String) passwordResponse.get("access_token");
        }

        throw new RuntimeException("Could not obtain WKS token via token exchange or password grant");
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
