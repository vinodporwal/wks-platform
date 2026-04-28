package com.wks.caseengine.rest.config.security;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

/**
 * Reads the WKS_SSO_SESSION cookie (set by SsoController for APM iframe users),
 * looks up the corresponding session, and pre-authenticates the request.
 * Also strips the Authorization header so BearerTokenAuthenticationFilter
 * doesn't attempt to validate the APM token against Keycloak.
 *
 * Uses a separate cookie name (WKS_SSO_SESSION) to avoid colliding with the
 * standard JSESSIONID used by standalone WKS users on the same domain.
 */
@Slf4j
public class SsoSessionAuthFilter extends OncePerRequestFilter {

    private static final String SSO_COOKIE_NAME = "WKS_SSO_SESSION";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ssoSessionId = getCookieValue(request, SSO_COOKIE_NAME);

        if (ssoSessionId != null) {
            // Look up the session by id — getSession(false) returns the current session
            // if its id matches the cookie; we rely on the container to resolve it
            HttpSession session = request.getSession(false);
            if (session != null && ssoSessionId.equals(session.getId())) {
                String userId = (String) session.getAttribute("userId");
                String token  = (String) session.getAttribute("token");

                if (userId != null && !userId.isBlank() && token != null) {
                    log.debug("SsoSessionAuthFilter: pre-authenticating SSO user: {}", userId);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userId, token, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    // Strip Authorization header so BearerTokenAuthenticationFilter skips
                    request = stripAuthorizationHeader(request);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private HttpServletRequest stripAuthorizationHeader(HttpServletRequest request) {
        return new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if ("Authorization".equalsIgnoreCase(name)) return null;
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if ("Authorization".equalsIgnoreCase(name)) return Collections.emptyEnumeration();
                return super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                return Collections.list(super.getHeaderNames())
                        .stream()
                        .filter(h -> !"Authorization".equalsIgnoreCase(h))
                        .collect(java.util.stream.Collectors.collectingAndThen(
                                java.util.stream.Collectors.toList(),
                                Collections::enumeration));
            }
        };
    }
}
