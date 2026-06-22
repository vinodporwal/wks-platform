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
package com.wks.caseengine.rest.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.wks.caseengine.rest.config.security.InjectorTenantHandlerInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Autowired
	private InjectorTenantHandlerInterceptor tenantHandler;

	@Value("${react.frontend.url:http://localhost:3001}")
	private String frontendUrl;

	@Value("${sso.allowed-origin:http://localhost:3000}")
	private String apmOrigin;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
				.allowedOrigins(frontendUrl, apmOrigin)
				.allowedHeaders("*")
				.allowedMethods("*")
				.allowCredentials(true);
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(tenantHandler);
	}

}
