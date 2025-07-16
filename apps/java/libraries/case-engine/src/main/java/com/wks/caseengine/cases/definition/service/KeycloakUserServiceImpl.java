package com.wks.caseengine.cases.definition.service;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.wks.caseengine.rest.db2.entity.Users;
import com.wks.caseengine.rest.db2.repository.UsersRepository;
import com.wks.caseengine.utility.KeycloakAdminClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class KeycloakUserServiceImpl implements KeycloakUserService {

	@Value("${keycloak.realm.name}")
	private String keycloakRealmName;
	
	private final KeycloakAdminClient keycloakAdminClient;

	@Autowired
	private UsersRepository usersRepository;
	
	public KeycloakUserServiceImpl(KeycloakAdminClient keycloakAdminClient) {
		this.keycloakAdminClient = keycloakAdminClient;
	}

	@Override
	public Map<String, Object> getUsers() throws Exception {
		Map<String, Object> result = new HashMap<String, Object>();

		try {
			Keycloak keycloak = keycloakAdminClient.getInstance();
			List<UserRepresentation> summaryUsers = keycloak.realm(keycloakRealmName).users().list();

			List<Map<String, Object>> userDetails = summaryUsers.stream()
			    .map(user -> {
			        String userId = user.getId();
			        UserResource userResource = keycloak.realm(keycloakRealmName).users().get(userId);
			        UserRepresentation userRep = userResource.toRepresentation();

			        String convertedUsername = convertUsername(userRep.getUsername());
			        String convertedEmail = convertEmail(userRep.getEmail());

			        Users existingUser = usersRepository.findByEmailId(convertedEmail);
			        
			        if(Objects.isNull(existingUser)) {
				        Users users = new Users();
				        users.setUserPkId(UUID.randomUUID().toString());
				        users.setUserId(convertedUsername);
				        users.setEmailId(convertedEmail);
				        usersRepository.save(users);
			        }
   
//			        // Directly assigned realm roles
//			        List<RoleRepresentation> realmRoles = userResource.roles().realmLevel().listEffective(false); // false -> only direct
//			        List<String> realmRoleNames = realmRoles.stream()
//			                .map(RoleRepresentation::getName)
//			                .collect(Collectors.toList());

			        // Construct response map
			        Map<String, Object> userMap = new HashMap<>();
			        userMap.put("user", userRep);
//			        userMap.put("realmRoles", realmRoleNames);
			        return userMap;
			    })
			    .collect(Collectors.toList());

			result.put("status", 200);
			result.put("message", "Users list by realm " + keycloakRealmName + ".");
			result.put("data", userDetails);
		} catch (Exception ex) {
			throw new Exception("Failed to fetch users from keyclok: " + ex.getMessage(), ex);
		}

		return result;
	}

	@Override
	public Map<String, Object> getRealmRoles() throws Exception {
		Map<String, Object> result = new HashMap<String, Object>();
		Keycloak keycloak = keycloakAdminClient.getInstance();

		try {
			List<RoleRepresentation> realmRoles = keycloak.realm(keycloakRealmName).roles().list();

			result.put("status", 200);
			result.put("message", "User roles fetched successfully.");
			result.put("data", realmRoles);
		} catch (Exception ex) {
			throw new Exception("Failed to fetch user roles:" + ex.getMessage(), ex);
		}

		return result;
	}

	@Override
	public Map<String, Object> getAllGroups() throws Exception {
		Map<String, Object> result = new HashMap<String, Object>();
		Keycloak keycloak = keycloakAdminClient.getInstance();

		try {
			List<GroupRepresentation> groups = keycloak.realm(keycloakRealmName).groups().groups();;

			result.put("status", 200);
			result.put("message", "User groups fetched successfully.");
			result.put("data", groups);
		} catch (Exception ex) {
			throw new Exception("Failed to fetch user groups:" + ex.getMessage(), ex);
		}

		return result;
	}

	@Override
	public Map<String, Object> searchUsers(String searchString) throws Exception {
	    Map<String, Object> result = new HashMap<>();
	    Keycloak keycloak = keycloakAdminClient.getInstance(); // assuming this is your singleton

	    try {
	        // This performs a general search across username, first name, last name, and email
	        List<UserRepresentation> users = keycloak.realm(keycloakRealmName)
	                                                 .users()
	                                                 .search(searchString, 0, 100); // optional: start, max

	        result.put("status", 200);
	        result.put("message", "Users fetched successfully.");
	        result.put("data", users);
	    } catch (Exception ex) {
	        throw new Exception("Failed to search users: " + ex.getMessage(), ex);
	    }

	    return result;
	}
	
	private String convertUsername(String input) {
		// Remove digits, split by dot, capitalize each part
		String[] parts = input.replaceAll("\\d", "").split("\\.");
		StringBuilder result = new StringBuilder();

		for (String part : parts) {
			if (!part.isEmpty()) {
				result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
			}
		}

		return result.toString().trim();
	}

	private String convertEmail(String email) {
		// Replace "@text." with "@"
		return email.replaceFirst("@in\\.", "@");
	}
}