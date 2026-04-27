package com.wks.caseengine.rest.config.security;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

/**
 * If a valid SSO session exists (created by SsoController for APM iframe users),
 * pre-authenticate the request and strip the Authorization header so Spring Security's
 * BearerTokenAuthenticationFilter doesn't attempt to validate the APM token against Keycloak.
 * Standalone WKS users are unaffected — they have no SSO session.
 */
@Slf4j
public class SsoSessionAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session != null) {
            String userId = (String) session.getAttribute("userId");
            String token  = (String) session.getAttribute("token");
            if (userId != null && !userId.isBlank() && token != null) {
                log.debug("SsoSessionAuthFilter: pre-authenticating user {} from SSO session", userId);

                // Set authentication so downstream filters see an authenticated principal
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId, token, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);

                // Strip Authorization header so BearerTokenAuthenticationFilter
                // doesn't overwrite our session-based auth with a failed APM token validation
                request = new HttpServletRequestWrapper(request) {
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

        filterChain.doFilter(request, response);
    }
}
