
// kept for the future application. currently not used. 23 Nov 2025

package com.wks.caseengine.cases.definition.service;

import java.util.List;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KeycloakService { 
    
    @Value("${keycloak.url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.client-id}")
    private String keycloakClientId;

    @Value("${keycloak.username}")
    private String keycloakUsername;

    @Value("${keycloak.password}")
    private String keycloakPassword;

    private Keycloak getKeycloakInstance() { 
        return KeycloakBuilder.builder()
            .serverUrl(keycloakUrl)
           // .realm(keycloakRealm)
           .realm("master")
            .grantType(OAuth2Constants.PASSWORD)
            .username(keycloakUsername)
            .password(keycloakPassword)
          //  .clientId(keycloakClientId)
          .clientId("admin-cli")
            .build();
    }

   public  List<GroupRepresentation> getAllGroups() {
        
    try {Keycloak keycloak = getKeycloakInstance();   
    
    GroupsResource groupResource = keycloak.realm(keycloakRealm).groups();

    groupResource.group("abc").members();
    return groupResource.groups();
    
    } catch (Exception e) {
        throw new RuntimeException(" ************* KeycloakService: Failed to get all groups", e);
    }
        
   }

   public UserRepresentation getUserById(String userId) {
       try {
           Keycloak keycloak = getKeycloakInstance();
           return keycloak.realm(keycloakRealm).users().get(userId).toRepresentation();
       } catch (Exception e) {
           throw new RuntimeException(" ************* KeycloakService: Failed to get user by id: " + userId, e);
       }
   }

   public List<UserRepresentation> getGroupMembers(String groupName) {
   
     
     
          try {
           Keycloak keycloak = getKeycloakInstance();
           GroupRepresentation grp = keycloak.realm(keycloakRealm)
           .groups()
           .groups()
           .stream()
           .filter(g -> g.getName().equalsIgnoreCase(groupName))
           .findFirst()
           .orElseThrow(() -> new RuntimeException("Group not found: " + groupName));
            return  keycloak.realm(keycloakRealm).groups().group(grp.getId()).members();
          } catch (Exception e) {
            throw new RuntimeException(" ************* KeycloakService: Failed to get group members", e);
          }
   }

    

    
}
