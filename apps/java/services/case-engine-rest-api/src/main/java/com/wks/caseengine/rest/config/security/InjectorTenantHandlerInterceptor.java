/*
 * WKS Platform - Open-Source Project
 * 
 * This file is part of the WKS Platform, an open-source project developed by WKS Power.
 * 
 * WKS Platform is licensed under the MIT License.
 * 
 * © 2021 WKS Power. All rights reserved.
 * 
 * For licensing information, see the LICENSE file in the root directory of the project.
 */
package com.wks.caseengine.rest.config.security;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.wks.api.security.BearerTokenHandlerInputResolver;
import com.wks.api.security.context.SecurityContextTenantHolder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class InjectorTenantHandlerInterceptor implements HandlerInterceptor {

	@Autowired
	private SecurityContextTenantHolder tenantHolder;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		setTenantId(request, tenantHolder);
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			@Nullable Exception ex) {
		tenantHolder.clear();
	}

	private void setTenantId(HttpServletRequest request, SecurityContextTenantHolder tenantHolder) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		BearerTokenHandlerInputResolver handler = new BearerTokenHandlerInputResolver();

		Map<String, Object> params = handler.resolver(request, authentication);

		String tenantId = params.isEmpty() ? null : (String) params.get("org");
		String userId   = params.isEmpty() ? null : (String) params.get("sub");

		// For APM iframe SSO users: JWT has no org claim, fall back to SSO session store
		if (tenantId == null || tenantId.isBlank()) {
			String ssoSessionId = getCookieValue(request, "WKS_SSO_SESSION");
			if (ssoSessionId != null) {
				java.util.Map<String, String> sessionData = com.wks.api.security.SsoSessionStore.STORE.get(ssoSessionId);
				if (sessionData != null) {
					String sessionOrg    = sessionData.get("org");
					String sessionUserId = sessionData.get("userId");
					if (sessionOrg != null && !sessionOrg.isBlank()) {
						log.info("InjectorTenantHandlerInterceptor: using org from SSO session: {}", sessionOrg);
						tenantId = sessionOrg;
					}
					if ((userId == null || userId.isBlank()) && sessionUserId != null) {
						userId = sessionUserId;
					}
				}
			}
		}

		if (tenantId == null || tenantId.isBlank()) {
			log.warn("Could not find tenantId — request may fail");
		}

		tenantHolder.setTenantId(tenantId);
		tenantHolder.setUserId(userId);
	}

	private String getCookieValue(HttpServletRequest request, String name) {
		if (request.getCookies() == null) return null;
		for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
			if (name.equals(cookie.getName())) return cookie.getValue();
		}
		return null;
	}

}
