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
package com.wks.storage.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.vote.UnanimousBased;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import com.wks.api.security.JwksIssuerAuthenticationManagerResolver;
import com.wks.api.security.OpenPolicyAuthzEnforcer;

@Configuration
public class ApiSecurityConfig {

	@Value("${opa.url}")
	private String opaUrl;

	@Value("${keycloak.url}")
	private String keycloakUrl;

	@Value("${keycloak.realm:localhost}")
	private String keycloakRealm;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.cors().and().csrf().disable()
				.addFilterBefore(new SsoSessionCookieAuthFilter(), BearerTokenAuthenticationFilter.class)
	            .authorizeRequests(authz -> authz
	                    .requestMatchers(HttpMethod.GET, "/files1/**").permitAll()  // Allow GET without authentication
	                    .anyRequest().authenticated()
	                    .accessDecisionManager(accessDecisionManager()))  // OPA policy applies to other requests
				.oauth2ResourceServer(oauth2 -> oauth2
						.authenticationManagerResolver(new JwksIssuerAuthenticationManagerResolver(keycloakUrl, keycloakRealm)));
		return http.build();
	}

	public AccessDecisionManager accessDecisionManager() {
		return new UnanimousBased(Arrays.asList(new OpenPolicyAuthzEnforcer(opaUrl)));
	}

}