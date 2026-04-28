package com.wks.api.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared in-memory store for APM iframe SSO sessions.
 * Keyed by WKS_SSO_SESSION cookie value.
 * Written by SsoController, read by SsoSessionAuthFilter,
 * InjectorTenantHandlerInterceptor, and JwksIssuerAuthenticationManagerResolver.
 */
public final class SsoSessionStore {

    private SsoSessionStore() {}

    public static final Map<String, Map<String, String>> STORE = new ConcurrentHashMap<>();
}
