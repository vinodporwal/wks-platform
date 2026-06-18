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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.wks.caseengine.loader.utils.SecretGenerator;

import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnProperty("keycloak.data.import.enabled")
@Order(2)
@Slf4j
public class KeycloakDataImportCommandRunner implements CommandLineRunner {

	@Value("${keycloak.data.import.url}")
	private String url;

	@Value("${keycloak.data.import.admin}")
	private String admin;

	@Value("${keycloak.data.import.adminpass}")
	private String adminPassword;

	@Value("${keycloak.data.import.realm}")
	private String realmName;

	@Value("${keycloak.data.import.portal-clientid}")
	private String portalClientId;

	@Value("${keycloak.data.import.externaltasks-clientid}")
	private String externalTasksClientId;

	@Value("${keycloak.data.import.externaltasks-secret}")
	private String externalTasksSecret;

	@Value("${keycloak.data.import.emailtocase-clientid}")
	private String emailToCaseClientId;

	@Value("${keycloak.data.import.emailtocase-secret}")
	private String emailToCaseSecret;

	@Value("${keycloak.data.import.redirecturl}")
	private String redirectUrl;

	@Value("${keycloak.data.import.weborigins}")
	private String webOrigins;

	@Value("${keycloak.data.import.username}")
	private String username;

	@Value("${keycloak.data.import.firstname}")
	private String firstname;

	@Value("${keycloak.data.import.lastname}")
	private String lastname;

	@Value("${keycloak.data.import.email}")
	private String email;

	@Value("${keycloak.data.import.password}")
	private String userPassword;

	@Autowired
	private GsonBuilder gsonBuilder;

	@Override
	public void run(String... args) throws IOException {
		log.info("Start of data importing");

		Keycloak keycloak = Keycloak.getInstance(url, "master", admin, adminPassword, "admin-cli");

		List<ClientRepresentation> clients = new ArrayList<>();
		ClientRepresentation portalClient = new ClientRepresentation();
		portalClient.setClientId(portalClientId);
		portalClient.setPublicClient(true);
		portalClient.setProtocol("openid-connect");
		portalClient.setDirectAccessGrantsEnabled(true);
		portalClient.setStandardFlowEnabled(true);
		portalClient.setFullScopeAllowed(true);
		portalClient.setClientAuthenticatorType("client-secret");
		portalClient.setRedirectUris(Arrays.asList(redirectUrl));
		portalClient.setWebOrigins(Arrays.asList(webOrigins));
		portalClient.setDefaultClientScopes(createDefaultClientScopes());
		portalClient.setOptionalClientScopes(createOptionalClientScopes());
		clients.add(portalClient);

		ClientRepresentation externalTasksClient = new ClientRepresentation();
		externalTasksClient.setClientId(externalTasksClientId);
		externalTasksClient.setSecret(externalTasksSecret);
		externalTasksClient.setPublicClient(true);
		externalTasksClient.setProtocol("openid-connect");
		externalTasksClient.setDirectAccessGrantsEnabled(true);
		externalTasksClient.setStandardFlowEnabled(true);
		externalTasksClient.setServiceAccountsEnabled(true);
		externalTasksClient.setAuthorizationServicesEnabled(true);
		externalTasksClient.setFullScopeAllowed(true);
		externalTasksClient.setClientAuthenticatorType("client-secret");
		externalTasksClient.setRedirectUris(Arrays.asList(redirectUrl));
		externalTasksClient.setWebOrigins(Arrays.asList(webOrigins));
		externalTasksClient.setDefaultClientScopes(createDefaultClientScopes());
		externalTasksClient.setOptionalClientScopes(createOptionalClientScopes());
		clients.add(externalTasksClient);

		ClientRepresentation emailToCaseClient = new ClientRepresentation();
		emailToCaseClient.setClientId(emailToCaseClientId);
		emailToCaseClient.setSecret(emailToCaseSecret);
		emailToCaseClient.setPublicClient(true);
		emailToCaseClient.setProtocol("openid-connect");
		emailToCaseClient.setDirectAccessGrantsEnabled(true);
		emailToCaseClient.setStandardFlowEnabled(true);
		emailToCaseClient.setServiceAccountsEnabled(true);
		emailToCaseClient.setAuthorizationServicesEnabled(true);
		emailToCaseClient.setFullScopeAllowed(true);
		emailToCaseClient.setClientAuthenticatorType("client-secret");
		emailToCaseClient.setRedirectUris(Arrays.asList(redirectUrl));
		emailToCaseClient.setWebOrigins(Arrays.asList(webOrigins));
		emailToCaseClient.setDefaultClientScopes(createDefaultClientScopes());
		emailToCaseClient.setOptionalClientScopes(createOptionalClientScopes());
		clients.add(emailToCaseClient);

		RealmRepresentation realm = new RealmRepresentation();
		realm.setRealm(realmName);
		realm.setUsers(createUsers());
		realm.setClients(clients);
		realm.setClientScopes(createScopes());
		realm.setEnabled(true);

		RolesRepresentation roleRepresentation = new RolesRepresentation();
		roleRepresentation.setRealm(createRealmRoles());
		realm.setRoles(roleRepresentation);
		realm.setGroups(createGroups());

		try {

			boolean realmExists = keycloak.realms().findAll().stream()
					.anyMatch(r -> realmName.equals(r.getRealm()));

			if (!realmExists) {
				keycloak.realms().create(realm);

				addUserToGroups(keycloak, externalTasksClientId, Arrays.asList("user", "manager", "email-to-case"));
				addUserToGroups(keycloak, emailToCaseClientId, Arrays.asList("email-to-case"));
			} else {
				// Realm already provisioned: keep it idempotent by re-applying the
				// (env-driven) redirect URIs / web origins so a redeploy never needs
				// manual Keycloak changes.
				log.info("Realm '{}' already exists; updating client redirect URIs / web origins", realmName);
				updateClientUrls(keycloak, portalClientId);
				updateClientUrls(keycloak, externalTasksClientId);
				updateClientUrls(keycloak, emailToCaseClientId);
				ensureRealmRoles(keycloak);
			}

			// Provision the application roles as CLIENT roles on the portal client
			// (the portal reads them from token.resource_access[wks-portal].roles)
			// and grant the default admin user the roles needed to use the UI.
			// Runs for both fresh and existing realms so no manual Keycloak changes
			// are ever required.
			ensurePortalClientRoles(keycloak);

		} catch (Exception e) {
			log.error("error to create keycloack", e);
		}

		log.info("End of data importing");
	}

	private static final List<String> PORTAL_CLIENT_ROLES = Arrays.asList("case_viewer", "case_editor", "case_creator",
			"admin");

	private void ensurePortalClientRoles(final Keycloak keycloak) {
		try {
			List<ClientRepresentation> clients = keycloak.realm(realmName).clients().findByClientId(portalClientId);
			if (clients == null || clients.isEmpty()) {
				log.warn("Client '{}' not found in realm '{}'; skipping client-role provisioning", portalClientId,
						realmName);
				return;
			}
			String clientUuid = clients.get(0).getId();
			ClientResource clientResource = keycloak.realm(realmName).clients().get(clientUuid);

			List<String> existing = clientResource.roles().list().stream().map(RoleRepresentation::getName)
					.collect(java.util.stream.Collectors.toList());
			for (String roleName : PORTAL_CLIENT_ROLES) {
				if (!existing.contains(roleName)) {
					RoleRepresentation role = new RoleRepresentation();
					role.setName(roleName);
					clientResource.roles().create(role);
					log.info("Created client role '{}' on client '{}'", roleName, portalClientId);
				}
			}

			// Grant the default admin user the roles needed to use the portal UI.
			assignClientRolesToUser(keycloak, clientUuid, username, Arrays.asList("admin", "case_creator"));

			// Remove now-unused realm-level duplicates from earlier deploys.
			removeStrayRealmRoles(keycloak);
		} catch (Exception e) {
			log.warn("Could not provision portal client roles: {}", e.getMessage());
		}
	}

	private void assignClientRolesToUser(final Keycloak keycloak, final String clientUuid, final String userName,
			final List<String> roleNames) {
		try {
			List<UserRepresentation> users = keycloak.realm(realmName).users().search(userName, true);
			if (users == null || users.isEmpty()) {
				log.warn("User '{}' not found; skipping client-role assignment", userName);
				return;
			}
			String userId = users.get(0).getId();
			ClientResource clientResource = keycloak.realm(realmName).clients().get(clientUuid);
			List<RoleRepresentation> toAssign = new ArrayList<>();
			for (String roleName : roleNames) {
				toAssign.add(clientResource.roles().get(roleName).toRepresentation());
			}
			keycloak.realm(realmName).users().get(userId).roles().clientLevel(clientUuid).add(toAssign);
			log.info("Assigned client roles {} to user '{}'", roleNames, userName);
		} catch (Exception e) {
			log.warn("Could not assign client roles to user '{}': {}", userName, e.getMessage());
		}
	}

	private void removeStrayRealmRoles(final Keycloak keycloak) {
		for (String roleName : PORTAL_CLIENT_ROLES) {
			try {
				boolean exists = keycloak.realm(realmName).roles().list().stream()
						.anyMatch(r -> roleName.equals(r.getName()));
				if (exists) {
					keycloak.realm(realmName).roles().deleteRole(roleName);
					log.info("Removed stray realm role '{}' (now a client role)", roleName);
				}
			} catch (Exception e) {
				log.warn("Could not remove stray realm role '{}': {}", roleName, e.getMessage());
			}
		}
	}

	private void updateClientUrls(final Keycloak keycloak, final String clientId) {
		try {
			List<ClientRepresentation> found = keycloak.realm(realmName).clients().findByClientId(clientId);
			if (found == null || found.isEmpty()) {
				log.warn("Client '{}' not found in realm '{}'; skipping URL update", clientId, realmName);
				return;
			}
			ClientRepresentation client = found.get(0);
			client.setRedirectUris(Arrays.asList(redirectUrl));
			client.setWebOrigins(Arrays.asList(webOrigins));
			keycloak.realm(realmName).clients().get(client.getId()).update(client);
			log.info("Updated redirect URIs / web origins for client '{}'", clientId);
		} catch (Exception e) {
			log.warn("Could not update URLs for client '{}': {}", clientId, e.getMessage());
		}
	}

	private void ensureRealmRoles(final Keycloak keycloak) {
		try {
			List<RoleRepresentation> desired = createRealmRoles();
			List<String> existing = keycloak.realm(realmName).roles().list().stream()
					.map(RoleRepresentation::getName).collect(java.util.stream.Collectors.toList());
			for (RoleRepresentation role : desired) {
				if (!existing.contains(role.getName())) {
					keycloak.realm(realmName).roles().create(role);
					log.info("Created missing realm role '{}'", role.getName());
				}
			}
		} catch (Exception e) {
			log.warn("Could not ensure realm roles: {}", e.getMessage());
		}
	}

	private List<String> createOptionalClientScopes() {
		return Arrays.asList("address", "phone", "offline_access", "microprofile-jwt");
	}

	private List<String> createDefaultClientScopes() {
		return Arrays.asList("web-origins", "acr", "org", "roles", "profile", "email");
	}

	private List<UserRepresentation> createUsers() {
		List<UserRepresentation> users = new ArrayList<>();

		UserRepresentation user = new UserRepresentation();
		user.setEnabled(true);
		user.setUsername(username);
		user.setFirstName(firstname);
		user.setLastName(lastname);
		user.setEmail(email);
		user.setGroups(Arrays.asList("user", "manager"));

		CredentialRepresentation password = new CredentialRepresentation();
		password.setTemporary(true);
		password.setType(CredentialRepresentation.PASSWORD);

		if (userPassword == null || userPassword.isBlank()) {
			password.setValue(SecretGenerator.create(16));
		} else {
			password.setValue(userPassword);
		}

		user.setCredentials(Arrays.asList(password));
		users.add(user);

		log.info("Password generated for user name:  {}", password.getValue());

		return users;
	}

	private List<GroupRepresentation> createGroups() {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		InputStream stream = contextClassLoader.getResourceAsStream("realmGroups.json");
		return gsonBuilder.create().fromJson(new InputStreamReader(stream), new TypeToken<List<GroupRepresentation>>() {
		}.getType());
	}

	private List<ClientScopeRepresentation> createScopes() throws IOException {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		InputStream stream = contextClassLoader.getResourceAsStream("clientScopes.json");
		List<ClientScopeRepresentation> clients = gsonBuilder.create().fromJson(new InputStreamReader(stream),
				new TypeToken<List<ClientScopeRepresentation>>() {
				}.getType());
		clients.forEach(f -> {
			if (f.getName().equals("org")) {
				f.getProtocolMappers().get(0).getConfig().put("claim.value", realmName);
			}
		});
		return clients;
	}

	@SuppressWarnings("rawtypes")
	private List<RoleRepresentation> createRealmRoles() throws IOException {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		InputStream stream = contextClassLoader.getResourceAsStream("realmRoles.json");
		List<HashMap<String, String>> out = gsonBuilder.create().fromJson(new InputStreamReader(stream),
				new TypeToken<List<HashMap>>() {
				}.getType());
		List<RoleRepresentation> roles = new ArrayList<>();
		out.forEach(r -> {
			roles.add(new RoleRepresentation(r.get("name"), r.get("description"), false));
		});
		return roles;
	}

	private void addUserToGroups(final Keycloak keycloak, final String userId, List<String>... groupNames) {
		UserRepresentation user = keycloak.realm(realmName).users().search("service-account-" + userId).get(0);
		UserResource userResource = keycloak.realm(realmName).users().get(user.getId());

		// If groupNames is provided, join the user to the specified groups
		if (groupNames != null && groupNames.length > 0) {
			for (List<String> names : groupNames) {
				if (names != null) {
					for (String groupName : names) {
						keycloak.realm(realmName).groups().groups().stream()
								.filter(group -> group.getName().equals(groupName)).findFirst()
								.ifPresent(group -> userResource.joinGroup(group.getId()));
					}
				}
			}
		} else {
			// If no group names provided, add the user to all groups
			keycloak.realm(realmName).groups().groups().forEach(group -> userResource.joinGroup(group.getId()));
		}
	}

}
