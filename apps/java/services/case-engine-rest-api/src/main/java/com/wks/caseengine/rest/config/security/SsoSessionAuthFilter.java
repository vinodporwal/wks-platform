package com.wks.caseengine.rest.config.security;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

/**
 * If a valid SSO session exists (created by SsoController), pre-authenticate
 * the request so Spring Security skips Bearer token / JWK validation.
 * This allows APM iframe users to call all APIs using their session cookie
 * without needing a WKS-issued JWT.
 * Standalone WKS users are unaffected — they have no SSO session.
 */
@Slf4j
public class SsoSessionAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Only act if no authentication is already set
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                String userId = (String) session.getAttribute("userId");
                String token  = (String) session.getAttribute("token");
                if (userId != null && !userId.isBlank() && token != null) {
                    log.debug("SsoSessionAuthFilter: pre-authenticating user {} from SSO session", userId);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userId, token, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
