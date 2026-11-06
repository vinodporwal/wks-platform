package com.wks.caseengine.cpp.utility;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for resolving the currently authenticated user's information
 * from the Keycloak JWT token available in Spring Security's SecurityContext.
 *
 * <p>Usage in a service or controller:</p>
 * <pre>
 *   CPPUserContext user = CPPUserContext.current();
 *   log.info("User: {}", user.toLogString());
 *   String modifiedBy = user.getPreferredUsername();
 * </pre>
 */
public class CPPUserContext {

    private static final Logger log = LoggerFactory.getLogger(CPPUserContext.class);

    // ── Identity fields ───────────────────────────────────────────────────────

    /** Keycloak internal UUID — stable, never changes even if username/email changes */
    @Getter private final String sub;

    /** Login username (e.g. "kiran.patel") — most common value used for modifiedBy */
    @Getter private final String preferredUsername;

    /** User's email address (e.g. "kiran.patel@domain.com") */
    @Getter private final String email;

    /** Full display name (e.g. "Kiran Patel") */
    @Getter private final String name;

    /** First name (e.g. "Kiran") */
    @Getter private final String givenName;

    /** Last name (e.g. "Patel") */
    @Getter private final String familyName;

    /** Organisation / Tenant ID from the "org" claim */
    @Getter private final String org;

    /** Realm-level roles from "realm_access.roles" */
    @Getter private final List<String> realmRoles;

    /** Whether the JWT was present and successfully parsed */
    @Getter private final boolean authenticated;

    // ── Constructor (private — use CPPUserContext.current()) ─────────────────

    private CPPUserContext(String sub, String preferredUsername, String email,
                           String name, String givenName, String familyName,
                           String org, List<String> realmRoles, boolean authenticated) {
        this.sub               = sub;
        this.preferredUsername = preferredUsername;
        this.email             = email;
        this.name              = name;
        this.givenName         = givenName;
        this.familyName        = familyName;
        this.org               = org;
        this.realmRoles        = realmRoles;
        this.authenticated     = authenticated;
    }

    // ── Factory method ───────────────────────────────────────────────────────

    /**
     * Resolves the current user from the SecurityContext JWT token.
     * Returns an unauthenticated placeholder if no JWT is present.
     */
    public static CPPUserContext current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken)) {
            log.warn("[CPPUserContext] No JwtAuthenticationToken found in SecurityContext");
            return unauthenticated();
        }

        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuth.getToken();

        String sub               = jwt.getClaimAsString("sub");
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        String email             = jwt.getClaimAsString("email");
        String fullName          = jwt.getClaimAsString("name");
        String givenName         = jwt.getClaimAsString("given_name");
        String familyName        = jwt.getClaimAsString("family_name");
        String org               = jwt.getClaimAsString("org");

        // Extract realm_access.roles
        List<String> realmRoles = Collections.emptyList();
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> rawRoles) {
            realmRoles = rawRoles.stream()
                    .filter(r -> r instanceof String)
                    .map(r -> (String) r)
                    .toList();
        }

        return new CPPUserContext(sub, preferredUsername, email, fullName,
                givenName, familyName, org, realmRoles, true);
    }

    // ── Convenience methods ───────────────────────────────────────────────────

    /**
     * Returns all user fields as an ordered map — useful for logging.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sub",                sub);
        map.put("preferred_username", preferredUsername);
        map.put("email",              email);
        map.put("name",               name);
        map.put("given_name",         givenName);
        map.put("family_name",        familyName);
        map.put("org",                org);
        map.put("realm_roles",        realmRoles);
        map.put("authenticated",      authenticated);
        return map;
    }

    /**
     * Returns a human-readable single-line string of all fields — for log statements.
     */
    public String toLogString() {
        return "CPPUserContext{" +
                "sub='" + sub + '\'' +
                ", preferred_username='" + preferredUsername + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", given_name='" + givenName + '\'' +
                ", family_name='" + familyName + '\'' +
                ", org='" + org + '\'' +
                ", realm_roles=" + realmRoles +
                ", authenticated=" + authenticated +
                '}';
    }

    /**
     * Logs all user fields using the provided logger at INFO level.
     * Call this from a service/controller to inspect what Keycloak provides.
     *
     * <pre>CPPUserContext.current().logAll(log, "GET /rpo-monthly-config");</pre>
     */
    public void logAll(Logger logger, String context) {
        logger.info("[CPPUserContext] [{}] sub               = {}", context, sub);
        logger.info("[CPPUserContext] [{}] preferred_username = {}", context, preferredUsername);
        logger.info("[CPPUserContext] [{}] email              = {}", context, email);
        logger.info("[CPPUserContext] [{}] name               = {}", context, name);
        logger.info("[CPPUserContext] [{}] given_name         = {}", context, givenName);
        logger.info("[CPPUserContext] [{}] family_name        = {}", context, familyName);
        logger.info("[CPPUserContext] [{}] org                = {}", context, org);
        logger.info("[CPPUserContext] [{}] realm_roles        = {}", context, realmRoles);
        logger.info("[CPPUserContext] [{}] authenticated      = {}", context, authenticated);
    }

    // ── Fallback ──────────────────────────────────────────────────────────────

    private static CPPUserContext unauthenticated() {
        return new CPPUserContext(null, "SYSTEM", null, null,
                null, null, null, Collections.emptyList(), false);
    }
}

