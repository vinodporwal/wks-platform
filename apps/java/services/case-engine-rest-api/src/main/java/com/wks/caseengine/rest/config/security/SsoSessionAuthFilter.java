package com.wks.caseengine.rest.config.security;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.wks.caseengine.rest.server.SsoController;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SsoSessionAuthFilter extends OncePerRequestFilter {

    private static final String SSO_COOKIE_NAME = "WKS_SSO_SESSION";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ssoSessionId = getCookieValue(request, SSO_COOKIE_NAME);

        if (ssoSessionId != null) {
            Map<String, String> sessionData = SsoController.SSO_SESSION_STORE.get(ssoSessionId);
            if (sessionData != null) {
                String userId = sessionData.get("userId");
                String token  = sessionData.get("token");

                if (userId != null && !userId.isBlank()) {
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
