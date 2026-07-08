
// kept for the future application. currently not used. 23 Nov 2025

package com.wks.caseengine.cases.definition.service;

import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class KeycloakService {

    @Value("${keycloak.url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.username}")
    private String keycloakUsername;

    @Value("${keycloak.password}")
    private String keycloakPassword;

    private Keycloak getKeycloakInstance() {
        return KeycloakBuilder.builder()
            .serverUrl(keycloakUrl)
            .realm("master")
            .grantType(OAuth2Constants.PASSWORD)
            .username(keycloakUsername)
            .password(keycloakPassword)
            .clientId("admin-cli")
            .build();
    }

    /**
     * Look up a Keycloak user by their external IDP user ID (the "sub" from the external token).
     * Returns null if not found so callers can fall back gracefully.
     */
    public UserRepresentation getUserByFederatedId(String externalUserId, String idpAlias) {
        try {
            Keycloak keycloak = getKeycloakInstance();

            // Try Keycloak's dedicated federated identity search endpoint first
            // GET /admin/realms/{realm}/users?idpAlias={alias}&idpUserId={id}
            try {
                List<UserRepresentation> results = keycloak.realm(keycloakRealm).users()
                    .searchByAttributes("idp_alias:" + idpAlias + " idp_userid:" + externalUserId);
                log.info("getUserByFederatedId searchByAttributes returned {} results for idpAlias={} externalUserId={}", 
                    results == null ? 0 : results.size(), idpAlias, externalUserId);
                if (results != null && !results.isEmpty()) return results.get(0);
            } catch (Exception e) {
                log.warn("searchByAttributes not supported, falling back to scan: {}", e.getMessage());
            }

            // Scan all users and check federated identity links
            List<UserRepresentation> allUsers = keycloak.realm(keycloakRealm).users().list(0, 500);
            log.info("getUserByFederatedId scanning {} users for externalUserId={}", allUsers.size(), externalUserId);
            for (UserRepresentation u : allUsers) {
                var fedIds = keycloak.realm(keycloakRealm).users().get(u.getId()).getFederatedIdentity();
                for (var fi : fedIds) {
                    log.info("  user={} idpAlias={} federatedUserId={} federatedUsername={}", 
                        u.getUsername(), fi.getIdentityProvider(), fi.getUserId(), fi.getUserName());
                    if (externalUserId.equals(fi.getUserId())) {
                        log.info("  -> MATCH found: keycloak user={}", u.getUsername());
                        return u;
                    }
                }
            }
            log.warn("getUserByFederatedId: no user found for externalUserId={}", externalUserId);
            return null;
        } catch (Exception e) {
            log.warn("getUserByFederatedId failed for externalId={}, idpAlias={}: {}", externalUserId, idpAlias, e.getMessage());
            return null;
        }
    }

    /**
     * Look up a Keycloak user by their Keycloak UUID, username, or email.
     */
    public UserRepresentation getUserById(String userId) {
        try {
            Keycloak keycloak = getKeycloakInstance();
            try {
                UserRepresentation user = keycloak.realm(keycloakRealm).users().get(userId).toRepresentation();
                if (user != null) return user;
            } catch (Exception e) {
                // Not a Keycloak UUID — fall through
            }
            List<UserRepresentation> byUsername = keycloak.realm(keycloakRealm).users().search(userId, true);
            if (byUsername != null && !byUsername.isEmpty()) return byUsername.get(0);

            List<UserRepresentation> byEmail = keycloak.realm(keycloakRealm).users().searchByEmail(userId, true);
            if (byEmail != null && !byEmail.isEmpty()) return byEmail.get(0);

            throw new RuntimeException("User not found in Keycloak for identifier: " + userId);
        } catch (Exception e) {
            throw new RuntimeException("KeycloakService: Failed to get user by id: " + userId, e);
        }
    }

    /**
     * Returns the effective client roles for a user under the given clientId (e.g. "wks-portal").
     */
    public List<String> getClientRolesForUser(String userId, String clientId) {
        try {
            Keycloak keycloak = getKeycloakInstance();
            String clientInternalId = keycloak.realm(keycloakRealm).clients()
                .findByClientId(clientId).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Client not found: " + clientId))
                .getId();
            List<RoleRepresentation> roles = keycloak.realm(keycloakRealm)
                .users().get(userId)
                .roles().clientLevel(clientInternalId).listEffective();
            return roles.stream().map(RoleRepresentation::getName).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get client roles for user: " + userId, e);
        }
    }
}

