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
package com.wks.api.security;

import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class JwksIssuerAuthenticationManagerResolver
		implements AuthenticationManagerResolver<HttpServletRequest> {

	private String keycloakUrl;
	private String defaultRealm;

	private final Cache cache = new ConcurrentMapCache("jwkSet");

	public JwksIssuerAuthenticationManagerResolver(String keycloakUrl) {
		this(keycloakUrl, null);
	}

	public JwksIssuerAuthenticationManagerResolver(String keycloakUrl, String defaultRealm) {
		super();
		this.keycloakUrl = keycloakUrl;
		this.defaultRealm = defaultRealm;
	}

	@Override
	public AuthenticationManager resolve(HttpServletRequest request) {
		String origin = request.getHeader("Origin");
		return new ResolvingAuthenticationManager(new RequestProps(origin, keycloakUrl, defaultRealm, cache));
	}

	static class RequestProps {
		String origin;
		String keycloack;
		String defaultRealm;
		Cache cache;

		public RequestProps(String origin, String keycloack, String defaultRealm, Cache cache) {
			super();
			this.origin = origin;
			this.keycloack = keycloack;
			this.defaultRealm = defaultRealm;
			this.cache = cache;
		}
	}

	static class ResolvingAuthenticationManager implements AuthenticationManager {

		private Converter<BearerTokenAuthenticationToken, String> issuerConverter;
		private RequestProps request;

		public ResolvingAuthenticationManager(RequestProps request) {
			this.request = request;
			this.issuerConverter = new JwtClaimIssuerConverter(request);
		}

		@Override
		public Authentication authenticate(Authentication authentication) throws AuthenticationException {
			BearerTokenAuthenticationToken token = (BearerTokenAuthenticationToken) authentication;

			String issuer = this.issuerConverter.convert(token);

			JwtAuthenticationManagerResolver authenticationManagerResolver = new JwtAuthenticationManagerResolver(
					request.cache);

			AuthenticationManager authenticationManager = authenticationManagerResolver.resolve(issuer);
			if (authenticationManager == null) {
				throw new InvalidBearerTokenException("Invalid issuer");
			}

			return authenticationManager.authenticate(authentication);
		}
	}

	static class JwtClaimIssuerConverter implements Converter<BearerTokenAuthenticationToken, String> {

		private RequestProps request;

		public JwtClaimIssuerConverter(RequestProps request) {
			this.request = request;
		}

		@Override
		public String convert(@NonNull BearerTokenAuthenticationToken authentication) {
			if (request.keycloack == "") {
				throw new InvalidBearerTokenException("Missing issuer");
			}

			try {
				String realm = extractRealmFromToken(authentication);
				String issueUrl = String.format("%s/realms/%s/protocol/openid-connect/certs", request.keycloack, realm);
				log.debug("issuer url {}", issueUrl);
				return issueUrl;
			} catch (Exception ex) {
				throw new InvalidBearerTokenException(ex.getMessage(), ex);
			}
		}

		private String extractRealmFromToken(BearerTokenAuthenticationToken authentication) {
			try {
				String token = authentication.getToken();
				JWTClaimsSet claims = JWTParser.parse(token).getJWTClaimsSet();

				// 1. Try org claim (WKS standalone tokens)
				String org = (String) claims.getClaim("org");
				if (org != null && !org.isBlank()) return org;

				// 2. Try ext.tenant_id (APM tokens)
				try {
					@SuppressWarnings("unchecked")
					java.util.Map<String, Object> ext = (java.util.Map<String, Object>) claims.getClaim("ext");
					if (ext != null) {
						String tenantId = (String) ext.get("tenant_id");
						if (tenantId != null && !tenantId.isBlank()) {
							log.debug("Using ext.tenant_id as realm: {}", tenantId);
							return tenantId;
						}
					}
				} catch (Exception ignored) {}

				// 3. Fall back to configured default realm
				if (request.defaultRealm != null && !request.defaultRealm.isBlank()) {
					log.debug("No org/tenant_id in token, using default realm: {}", request.defaultRealm);
					return request.defaultRealm;
				}

				throw new RuntimeException("Token has no org claim and no default realm is configured");
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}
	}

	static class JwtAuthenticationManagerResolver implements AuthenticationManagerResolver<String> {

		private final Map<String, AuthenticationManager> authenticationManagers = new ConcurrentHashMap<>();
		private Cache cache;

		public JwtAuthenticationManagerResolver(Cache cache) {
			this.cache = cache;
		}

		@Override
		public AuthenticationManager resolve(String issuer) {
			AuthenticationManager authenticationManager = this.authenticationManagers.computeIfAbsent(issuer, (k) -> {
				log.debug("Constructing AuthenticationManager");
				log.debug("Resolved AuthenticationManager for issuer '{}'", issuer);

				JwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(issuer).cache(cache).build();

				return new JwtAuthenticationProvider(jwtDecoder)::authenticate;
			});

			return authenticationManager;
		}
	}
}
