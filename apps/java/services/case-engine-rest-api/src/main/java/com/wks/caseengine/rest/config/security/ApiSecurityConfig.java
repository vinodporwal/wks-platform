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

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.vote.UnanimousBased;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;

import com.wks.api.security.JwksIssuerAuthenticationManagerResolver;
import com.wks.api.security.OpenPolicyAuthzEnforcer;
import com.wks.api.security.OpenPolicyAuthzEnforcerConfig;

@Configuration
public class ApiSecurityConfig {

	@Value("${opa.url}")
	private String opaUrl;

	@Value("${keycloak.url}")
	private String keycloakUrl;

	@Value("${keycloak.realm:localhost}")
	private String keycloakRealm;

	@Value("${case.engine.actuator.enabled}")
	private Boolean actuatorEnabled;

	@Value("${case.engine.swagger.enabled}")
	private Boolean swaggerEnabled;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.cors().and().csrf().disable()
			// Pre-authenticate requests that have a valid SSO session (APM iframe users)
			// Must run before the OAuth2 resource server filter
			.addFilterBefore(new SsoSessionAuthFilter(), BearerTokenAuthenticationFilter.class)
			.authorizeRequests(authz -> authz
					.filterSecurityInterceptorOncePerRequest(false)
					// SSO endpoints are validated internally, no Bearer token required
					.requestMatchers("/sso/**").permitAll()
					.requestMatchers("/debug/**").permitAll()
					.anyRequest().authenticated()
					.accessDecisionManager(accessDecisionManager()))
			.oauth2ResourceServer(oauth2 -> oauth2
					.authenticationManagerResolver(new JwksIssuerAuthenticationManagerResolver(keycloakUrl, keycloakRealm)));
		return http.build();
	}

	public AccessDecisionManager accessDecisionManager() {
		return new UnanimousBased(Arrays.asList(new OpenPolicyAuthzEnforcer(OpenPolicyAuthzEnforcerConfig.builder()
				.opaAuthURL(opaUrl).actuatorEnabled(actuatorEnabled).swaggerEnabled(swaggerEnabled).build())));
	}
}