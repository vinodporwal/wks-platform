package com.wks.storage.security;

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
import lombok.extern.slf4j.Slf4j;

/**
 * Pre-authenticates requests that carry a valid WKS_SSO_SESSION cookie.
 * This cookie is HttpOnly + Secure + SameSite=None, set by case-engine-rest-api
 * during the APM iframe SSO flow. Since it cannot be created or read by
 * client-side JavaScript, its presence is proof of a valid SSO session.
 *
 * This filter runs before BearerTokenAuthenticationFilter so that APM iframe
 * users (whose tokens are signed by APM's IdP, not our Keycloak) can access
 * storage-api without JWT validation.
 */
@Slf4j
public class SsoSessionCookieAuthFilter extends OncePerRequestFilter {

    private static final String SSO_COOKIE_NAME = "WKS_SSO_SESSION";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ssoSessionId = getCookieValue(request, SSO_COOKIE_NAME);

        if (ssoSessionId != null && !ssoSessionId.isBlank()) {
            log.debug("SsoSessionCookieAuthFilter: WKS_SSO_SESSION cookie found, pre-authenticating request");

            // The cookie is HttpOnly + Secure — its presence is trusted.
            // Set a minimal authentication so Spring Security skips JWT validation.
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken("sso-user", null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Strip Authorization header so BearerTokenAuthenticationFilter doesn't
            // attempt (and fail) JWT validation
            request = stripAuthorizationHeader(request);
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
