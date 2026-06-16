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
package com.wks.caseengine.loader.runner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Configures the realm authentication mechanism based on {@code WKS_AUTH_MODE}.
 *
 * <ul>
 * <li>{@code default} - plain Keycloak username/password login. Any previously
 * brokered OIDC/ADFS identity provider is removed so the flag can be toggled
 * back cleanly.</li>
 * <li>{@code oidc} - brokers to an external OpenID Connect provider using the
 * {@code OIDC_*} parameters.</li>
 * <li>{@code adfs} - brokers to ADFS. SAML by default (import from federation
 * metadata or explicit endpoints); OIDC when {@code ADFS_PROTOCOL=oidc}.</li>
 * </ul>
 *
 * Everything is provisioned through the Keycloak Admin API at deploy time, so
 * no manual changes are ever required inside Keycloak. Runs after the realm and
 * clients have been created by {@link KeycloakDataImportCommandRunner}.
 */
@Component
@ConditionalOnProperty("keycloak.data.import.enabled")
@Order(3)
@Slf4j
public class KeycloakIdentityProviderConfigurer implements CommandLineRunner {

	private static final String REDIRECTOR_PROVIDER_ID = "identity-provider-redirector";
	private static final String REDIRECTOR_CONFIG_ALIAS = "wks-default-idp-redirector";

	@Value("${keycloak.data.import.url}")
	private String url;

	@Value("${keycloak.data.import.admin}")
	private String admin;

	@Value("${keycloak.data.import.adminpass}")
	private String adminPassword;

	@Value("${keycloak.data.import.realm}")
	private String realmName;

	@Value("${keycloak.auth.mode:default}")
	private String authMode;

	@Value("${keycloak.auth.redirect-to-idp:true}")
	private boolean redirectToIdp;

	// OIDC
	@Value("${keycloak.auth.oidc.alias:oidc}")
	private String oidcAlias;
	@Value("${keycloak.auth.oidc.display-name:Single Sign-On}")
	private String oidcDisplayName;
	@Value("${keycloak.auth.oidc.client-id:}")
	private String oidcClientId;
	@Value("${keycloak.auth.oidc.client-secret:}")
	private String oidcClientSecret;
	@Value("${keycloak.auth.oidc.authorization-url:}")
	private String oidcAuthorizationUrl;
	@Value("${keycloak.auth.oidc.token-url:}")
	private String oidcTokenUrl;
	@Value("${keycloak.auth.oidc.user-info-url:}")
	private String oidcUserInfoUrl;
	@Value("${keycloak.auth.oidc.jwks-url:}")
	private String oidcJwksUrl;
	@Value("${keycloak.auth.oidc.issuer:}")
	private String oidcIssuer;
	@Value("${keycloak.auth.oidc.logout-url:}")
	private String oidcLogoutUrl;
	@Value("${keycloak.auth.oidc.default-scope:openid profile email}")
	private String oidcDefaultScope;

	// ADFS
	@Value("${keycloak.auth.adfs.alias:adfs}")
	private String adfsAlias;
	@Value("${keycloak.auth.adfs.display-name:ADFS}")
	private String adfsDisplayName;
	@Value("${keycloak.auth.adfs.protocol:saml}")
	private String adfsProtocol;
	@Value("${keycloak.auth.adfs.metadata-url:}")
	private String adfsMetadataUrl;
	@Value("${keycloak.auth.adfs.entity-id:}")
	private String adfsEntityId;
	@Value("${keycloak.auth.adfs.single-sign-on-service-url:}")
	private String adfsSsoUrl;
	@Value("${keycloak.auth.adfs.single-logout-service-url:}")
	private String adfsSloUrl;
	@Value("${keycloak.auth.adfs.signing-certificate:}")
	private String adfsSigningCertificate;
	@Value("${keycloak.auth.adfs.client-id:}")
	private String adfsClientId;
	@Value("${keycloak.auth.adfs.client-secret:}")
	private String adfsClientSecret;
	@Value("${keycloak.auth.adfs.authorization-url:}")
	private String adfsAuthorizationUrl;
	@Value("${keycloak.auth.adfs.token-url:}")
	private String adfsTokenUrl;
	@Value("${keycloak.auth.adfs.jwks-url:}")
	private String adfsJwksUrl;
	@Value("${keycloak.auth.adfs.issuer:}")
	private String adfsIssuer;

	@Override
	public void run(String... args) {
		String mode = authMode == null ? "default" : authMode.trim().toLowerCase();
		log.info("Configuring Keycloak authentication mode '{}' for realm '{}'", mode, realmName);

		Keycloak keycloak = Keycloak.getInstance(url, "master", admin, adminPassword, "admin-cli");
		try {
			switch (mode) {
			case "oidc":
				configureOidc(keycloak);
				break;
			case "adfs":
				configureAdfs(keycloak);
				break;
			case "default":
			default:
				// Plain username/password: ensure no brokered IdP / auto-redirect remains.
				removeIdentityProvider(keycloak, oidcAlias);
				removeIdentityProvider(keycloak, adfsAlias);
				clearDefaultIdpRedirector(keycloak);
				log.info("Default username/password authentication in effect (no external IdP).");
				break;
			}
		} catch (Exception e) {
			// Never fail the loader because of auth wiring; log loudly instead.
			log.error("Failed to configure authentication mode '{}'", mode, e);
		}
	}

	private void configureOidc(Keycloak keycloak) {
		if (isBlank(oidcClientId) || isBlank(oidcAuthorizationUrl) || isBlank(oidcTokenUrl)) {
			log.error("WKS_AUTH_MODE=oidc but OIDC_CLIENT_ID / OIDC_AUTHORIZATION_URL / OIDC_TOKEN_URL are missing. "
					+ "Skipping OIDC provider configuration.");
			return;
		}

		Map<String, String> config = new HashMap<>();
		config.put("clientId", oidcClientId);
		config.put("clientSecret", oidcClientSecret);
		config.put("authorizationUrl", oidcAuthorizationUrl);
		config.put("tokenUrl", oidcTokenUrl);
		putIfNotBlank(config, "userInfoUrl", oidcUserInfoUrl);
		putIfNotBlank(config, "issuer", oidcIssuer);
		putIfNotBlank(config, "logoutUrl", oidcLogoutUrl);
		config.put("defaultScope", oidcDefaultScope);
		config.put("clientAuthMethod", "client_secret_post");
		config.put("syncMode", "IMPORT");
		if (!isBlank(oidcJwksUrl)) {
			config.put("jwksUrl", oidcJwksUrl);
			config.put("useJwksUrl", "true");
			config.put("validateSignature", "true");
		} else {
			config.put("validateSignature", "false");
		}

		IdentityProviderRepresentation idp = baseIdp(oidcAlias, "oidc", oidcDisplayName);
		idp.setConfig(config);
		upsertIdentityProvider(keycloak, idp);
		applyRedirectPreference(keycloak, oidcAlias);
		log.info("OIDC identity provider '{}' provisioned for realm '{}'.", oidcAlias, realmName);
	}

	private void configureAdfs(Keycloak keycloak) {
		String protocol = adfsProtocol == null ? "saml" : adfsProtocol.trim().toLowerCase();

		if ("oidc".equals(protocol)) {
			if (isBlank(adfsClientId) || isBlank(adfsAuthorizationUrl) || isBlank(adfsTokenUrl)) {
				log.error("WKS_AUTH_MODE=adfs ADFS_PROTOCOL=oidc but ADFS_CLIENT_ID / ADFS_AUTHORIZATION_URL / "
						+ "ADFS_TOKEN_URL are missing. Skipping ADFS provider configuration.");
				return;
			}
			Map<String, String> config = new HashMap<>();
			config.put("clientId", adfsClientId);
			config.put("clientSecret", adfsClientSecret);
			config.put("authorizationUrl", adfsAuthorizationUrl);
			config.put("tokenUrl", adfsTokenUrl);
			putIfNotBlank(config, "issuer", adfsIssuer);
			config.put("defaultScope", "openid profile email");
			config.put("clientAuthMethod", "client_secret_post");
			config.put("syncMode", "IMPORT");
			if (!isBlank(adfsJwksUrl)) {
				config.put("jwksUrl", adfsJwksUrl);
				config.put("useJwksUrl", "true");
				config.put("validateSignature", "true");
			} else {
				config.put("validateSignature", "false");
			}
			IdentityProviderRepresentation idp = baseIdp(adfsAlias, "oidc", adfsDisplayName);
			idp.setConfig(config);
			upsertIdentityProvider(keycloak, idp);
			applyRedirectPreference(keycloak, adfsAlias);
			log.info("ADFS (OIDC) identity provider '{}' provisioned for realm '{}'.", adfsAlias, realmName);
			return;
		}

		// SAML (default for ADFS)
		Map<String, String> config = new HashMap<>();
		if (!isBlank(adfsMetadataUrl)) {
			// Import endpoints/certificate from the ADFS federation metadata.
			try {
				Map<String, Object> importData = new HashMap<>();
				importData.put("providerId", "saml");
				importData.put("fromUrl", adfsMetadataUrl);
				Map<String, String> imported = keycloak.realm(realmName).identityProviders().importFrom(importData);
				if (imported != null) {
					config.putAll(imported);
				}
				log.info("Imported ADFS SAML metadata from {}", adfsMetadataUrl);
			} catch (Exception e) {
				log.error("Could not import ADFS SAML metadata from {} - falling back to explicit params",
						adfsMetadataUrl, e);
			}
		}
		// Explicit params (used when no metadata URL, or to override imported values).
		putIfNotBlank(config, "idpEntityId", adfsEntityId);
		putIfNotBlank(config, "singleSignOnServiceUrl", adfsSsoUrl);
		putIfNotBlank(config, "singleLogoutServiceUrl", adfsSloUrl);
		putIfNotBlank(config, "signingCertificate", adfsSigningCertificate);
		config.putIfAbsent("nameIDPolicyFormat", "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");
		config.putIfAbsent("postBindingResponse", "true");
		config.putIfAbsent("postBindingAuthnRequest", "true");
		config.putIfAbsent("validateSignature", isBlank(adfsSigningCertificate) ? "false" : "true");
		config.put("syncMode", "IMPORT");

		if (isBlank(config.get("singleSignOnServiceUrl"))) {
			log.error("WKS_AUTH_MODE=adfs (saml) but no SSO URL resolved (set ADFS_METADATA_URL or ADFS_SSO_URL). "
					+ "Skipping ADFS provider configuration.");
			return;
		}

		IdentityProviderRepresentation idp = baseIdp(adfsAlias, "saml", adfsDisplayName);
		idp.setConfig(config);
		upsertIdentityProvider(keycloak, idp);
		applyRedirectPreference(keycloak, adfsAlias);
		log.info("ADFS (SAML) identity provider '{}' provisioned for realm '{}'.", adfsAlias, realmName);
	}

	private IdentityProviderRepresentation baseIdp(String alias, String providerId, String displayName) {
		IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
		idp.setAlias(alias);
		idp.setProviderId(providerId);
		idp.setEnabled(true);
		idp.setDisplayName(displayName);
		idp.setTrustEmail(true);
		idp.setStoreToken(false);
		idp.setLinkOnly(false);
		idp.setFirstBrokerLoginFlowAlias("first broker login");
		return idp;
	}

	/** Create or replace the identity provider so re-deploys are idempotent. */
	private void upsertIdentityProvider(Keycloak keycloak, IdentityProviderRepresentation idp) {
		removeIdentityProvider(keycloak, idp.getAlias());
		keycloak.realm(realmName).identityProviders().create(idp).close();
	}

	private void removeIdentityProvider(Keycloak keycloak, String alias) {
		if (isBlank(alias)) {
			return;
		}
		try {
			List<IdentityProviderRepresentation> all = keycloak.realm(realmName).identityProviders().findAll();
			boolean exists = all != null && all.stream().anyMatch(p -> alias.equals(p.getAlias()));
			if (exists) {
				keycloak.realm(realmName).identityProviders().get(alias).remove();
				log.info("Removed existing identity provider '{}'", alias);
			}
		} catch (Exception e) {
			log.warn("Could not remove identity provider '{}': {}", alias, e.getMessage());
		}
	}

	/**
	 * If redirect-to-idp is enabled, configure the browser flow's Identity Provider
	 * Redirector so the login page auto-forwards to the given IdP. Otherwise the IdP
	 * is simply shown as a button next to the local login form.
	 */
	private void applyRedirectPreference(Keycloak keycloak, String alias) {
		if (redirectToIdp) {
			setDefaultIdpRedirector(keycloak, alias);
		} else {
			clearDefaultIdpRedirector(keycloak);
		}
	}

	private void setDefaultIdpRedirector(Keycloak keycloak, String alias) {
		try {
			AuthenticationManagementResource flows = keycloak.realm(realmName).flows();
			for (AuthenticationExecutionInfoRepresentation exec : flows.getExecutions("browser")) {
				if (REDIRECTOR_PROVIDER_ID.equals(exec.getProviderId())) {
					// Remove a previous config to stay idempotent.
					if (exec.getAuthenticationConfig() != null) {
						try {
							keycloak.realm(realmName).flows().removeAuthenticatorConfig(exec.getAuthenticationConfig());
						} catch (Exception ignore) {
							// no-op
						}
					}
					AuthenticatorConfigRepresentation cfg = new AuthenticatorConfigRepresentation();
					cfg.setAlias(REDIRECTOR_CONFIG_ALIAS);
					Map<String, String> c = new HashMap<>();
					c.put("defaultProvider", alias);
					cfg.setConfig(c);
					flows.newExecutionConfig(exec.getId(), cfg).close();
					log.info("Login will auto-redirect to identity provider '{}'.", alias);
					return;
				}
			}
			log.warn("Identity Provider Redirector execution not found in 'browser' flow; "
					+ "auto-redirect not configured.");
		} catch (Exception e) {
			log.warn("Could not configure auto-redirect to IdP: {}", e.getMessage());
		}
	}

	private void clearDefaultIdpRedirector(Keycloak keycloak) {
		try {
			AuthenticationManagementResource flows = keycloak.realm(realmName).flows();
			for (AuthenticationExecutionInfoRepresentation exec : flows.getExecutions("browser")) {
				if (REDIRECTOR_PROVIDER_ID.equals(exec.getProviderId()) && exec.getAuthenticationConfig() != null) {
					flows.removeAuthenticatorConfig(exec.getAuthenticationConfig());
					log.info("Cleared IdP auto-redirect from 'browser' flow.");
				}
			}
		} catch (Exception e) {
			log.warn("Could not clear IdP auto-redirect: {}", e.getMessage());
		}
	}

	private static boolean isBlank(String s) {
		return s == null || s.isBlank();
	}

	private static void putIfNotBlank(Map<String, String> map, String key, String value) {
		if (!isBlank(value)) {
			map.put(key, value);
		}
	}
}
