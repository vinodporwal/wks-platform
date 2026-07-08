package com.wks.caseengine.cases.definition.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * Calls the Keycloak Admin REST API directly via HTTP — same as the verified shell script.
 * Uses keycloak.url (e.g. http://localhost:8082/cm) and admin credentials.
 */
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

    private final RestTemplate rest = new RestTemplate();

    // Step 1: get admin token from master realm (mirrors the script's Step 1)
    private String getAdminToken() {
        String url = keycloakUrl + "/realms/master/protocol/openid-connect/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", "admin-cli");
        body.add("username", keycloakUsername);
        body.add("password", keycloakPassword);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map<String, Object>> response = rest.exchange(
            url, HttpMethod.POST,
            new HttpEntity<>(body, headers),
            new ParameterizedTypeReference<>() {}
        );

        Map<String, Object> tokenData = response.getBody();
        if (tokenData == null || !tokenData.containsKey("access_token")) {
            throw new RuntimeException("Failed to obtain admin token from Keycloak");
        }
        return (String) tokenData.get("access_token");
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return h;
    }

    /**
     * Look up a Keycloak user by their external IDP user ID.
     * Mirrors: GET /admin/realms/{realm}/users?idpAlias={alias}&idpUserId={id}
     */
    @SuppressWarnings("unchecked")
    @Value("${keycloak.idp-alias:oidc}")
    private String idpAlias;

    public Map<String, Object> getUserByFederatedId(String externalUserId) {
        try {
            String token = getAdminToken();
            String url = UriComponentsBuilder
                .fromHttpUrl(keycloakUrl + "/admin/realms/" + keycloakRealm + "/users")
                .queryParam("idpAlias", idpAlias)
                .queryParam("idpUserId", externalUserId)
                .toUriString();

            log.info("getUserByFederatedId GET {}", url);

            ResponseEntity<List<Map<String, Object>>> response = rest.exchange(
                url, HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                new ParameterizedTypeReference<>() {}
            );

            log.info("getUserByFederatedId status={} body={}", response.getStatusCode(), response.getBody());

            List<Map<String, Object>> users = response.getBody();
            if (users != null && !users.isEmpty()) {
                log.info("getUserByFederatedId found user={}", users.get(0).get("username"));
                return users.get(0);
            }
            log.warn("getUserByFederatedId: empty result for externalUserId={} idpAlias={}", externalUserId, idpAlias);
            return null;
        } catch (Exception e) {
            log.warn("getUserByFederatedId failed for externalUserId={}: {}", externalUserId, e.getMessage());
            return null;
        }
    }

    /**
     * Returns the wks-portal client role names for a given Keycloak user UUID.
     * Mirrors: GET /admin/realms/{realm}/users/{id}/role-mappings
     */
    @SuppressWarnings("unchecked")
    public List<String> getClientRolesForUser(String keycloakUserId, String clientId) {
        try {
            String token = getAdminToken();
            String url = keycloakUrl + "/admin/realms/" + keycloakRealm + "/users/" + keycloakUserId + "/role-mappings";

            ResponseEntity<Map<String, Object>> response = rest.exchange(
                url, HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null) return Collections.emptyList();

            Map<String, Object> clientMappings = (Map<String, Object>) body.get("clientMappings");
            if (clientMappings == null || !clientMappings.containsKey(clientId)) return Collections.emptyList();

            Map<String, Object> clientEntry = (Map<String, Object>) clientMappings.get(clientId);
            List<Map<String, Object>> mappings = (List<Map<String, Object>>) clientEntry.get("mappings");
            if (mappings == null) return Collections.emptyList();

            return mappings.stream()
                .map(m -> (String) m.get("name"))
                .filter(n -> n != null)
                .toList();
        } catch (Exception e) {
            log.warn("getClientRolesForUser failed for userId={}: {}", keycloakUserId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
