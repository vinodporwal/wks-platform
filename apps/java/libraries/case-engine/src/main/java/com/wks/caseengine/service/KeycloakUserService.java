package com.wks.caseengine.service;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wks.caseengine.dto.BulkRoleAssignmentRequest;
import com.wks.caseengine.entity.UserScreenMapping;
import com.wks.caseengine.repository.UserScreenMappingRepository;
import com.wks.caseengine.utility.KeycloakAdminClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class KeycloakUserService {

	@Value("${keycloak.realm.name}")
	private String keycloakRealmName;

	@Autowired
	private UserScreenMappingRepository userScreenMappingRepository;
	
	private final KeycloakAdminClient keycloakAdminClient;

	public KeycloakUserService(KeycloakAdminClient keycloakAdminClient) {
		this.keycloakAdminClient = keycloakAdminClient;
	}

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

			        // Directly assigned realm roles
			        List<RoleRepresentation> realmRoles = userResource.roles().realmLevel().listEffective(false); // false -> only direct
			        List<String> realmRoleNames = realmRoles.stream()
			                .map(RoleRepresentation::getName)
			                .collect(Collectors.toList());

			        // Construct response map
			        Map<String, Object> userMap = new HashMap<>();
			        userMap.put("user", userRep);
			        userMap.put("realmRoles", realmRoleNames);
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


	public List<UserRepresentation> getUsersWithRole(String roleName) throws Exception {

		try {
			Keycloak keycloak = keycloakAdminClient.getInstance();
			List<UserRepresentation> summaryUsers = 
			
			keycloak.realm(keycloakRealmName)
            .roles()
            .get(roleName)
            .getUserMembers();

			List<UserRepresentation> userDetails = summaryUsers.stream()
			    .map(user -> {
			        String userId = user.getId();
			        UserResource userResource = keycloak.realm(keycloakRealmName).users().get(userId);
			        UserRepresentation userRep = userResource.toRepresentation();

			      
			        // Construct response map
			        return userRep; 
					
			    })
			    .collect(Collectors.toList());

      	return userDetails;

		} catch (Exception ex) {
			throw new Exception("Failed to fetch users from keyclok: " + ex.getMessage(), ex);
		}

		

		
	}




	public Map<String, Object> updateUser(Map<String, Object> data) throws Exception {
		Map<String, Object> result = new HashMap<>();
		Keycloak keycloak = keycloakAdminClient.getInstance();
		
		try {
		    // Get shared data
		    List<String> userIds = (List<String>) data.get("userIds");
		    Object attrObj = data.get("attributes");
		    Object roleObj = data.get("role");

		    // Step 1: Build plantMapping structure
		    List<Map<String, List<Map<String, List<String>>>>> plantMapping = buildPlantMapping(attrObj);

		    // Step 2: Loop through each userId
		    for (String userId : new HashSet<>(userIds)) {
		        UserResource userResource = keycloak.realm(keycloakRealmName).users().get(userId);
		        UserRepresentation user = userResource.toRepresentation();

		        // Prepare mapping for this user
		        List<Map<String, List<Map<String, List<String>>>>> userPlantMapping = new ArrayList<>();

		        if (attrObj instanceof Map) {
		            Map<String, Object> attrMap = (Map<String, Object>) attrObj;
		            List<Map<String, Object>> plants = (List<Map<String, Object>>) attrMap.get("plants");

		            for (Map<String, Object> vertical : plants) {
		                String verticalId = (String) vertical.get("verticalId");
		                List<Map<String, Object>> sites = (List<Map<String, Object>>) vertical.get("sites");

		                List<Map<String, List<String>>> siteEntries = new ArrayList<>();

		                for (Map<String, Object> site : sites) {
		                    String siteId = (String) site.get("siteId");
		                    List<Map<String, Object>> plantList = (List<Map<String, Object>>) site.get("plants");

		                    List<String> plantIds = new ArrayList<>();

		                    for (Map<String, Object> plant : plantList) {
		                        String plantId = (String) plant.get("plantId");
		                        List<String> screens = (List<String>) plant.get("screens");
		                        List<String> permissions = (List<String>) plant.get("permission");

		                        plantIds.add(plantId);

		                        ObjectMapper objectMapper = new ObjectMapper();
		                        String permissionsString = objectMapper.writeValueAsString(permissions);	                        
		                        
		                        List<UserScreenMapping> existingMappings = userScreenMappingRepository
		                        	    .findByUserIdAndPlantFKIdAndVerticalFKId(
		                        	        UUID.fromString(userId),
		                        	        UUID.fromString(plantId),
		                        	        UUID.fromString(verticalId)
		                        	    );

		                        	Set<String> existingScreenCodes = existingMappings.stream()
		                        	    .map(UserScreenMapping::getScreenCode)
		                        	    .collect(Collectors.toSet());

		                        	List<UserScreenMapping> newMappings = new ArrayList<>();

		                        	for (String screen : screens) {
		                        	    if (!existingScreenCodes.contains(screen)) {
		                        	        UserScreenMapping userScreenMapping = new UserScreenMapping();
		                        	        userScreenMapping.setId(UUID.randomUUID());
		                        	        userScreenMapping.setUserId(UUID.fromString(userId));
		                        	        userScreenMapping.setPlantFKId(UUID.fromString(plantId));
		                        	        userScreenMapping.setVerticalFKId(UUID.fromString(verticalId));
		                        	        userScreenMapping.setScreenCode(screen);
		                        	        userScreenMapping.setPermissions(permissionsString);

		                        	        newMappings.add(userScreenMapping);
		                        	    }
		                        	}

		                        	userScreenMappingRepository.saveAll(newMappings);
		                    }

		                    siteEntries.add(Map.of(siteId, plantIds));
		                }

		                userPlantMapping.add(Map.of(verticalId, siteEntries));
		            }
		        }

		        System.out.println("User screen mapping saved.");

		     // Step 1: Get current attributes
		        Map<String, List<String>> attributes = Optional.ofNullable(user.getAttributes())
		            .orElseGet(HashMap::new);

		        // Step 2: Parse existing plants data if available
		        ObjectMapper objectMapper = new ObjectMapper();
		        List<Map<String, List<Map<String, List<String>>>>> existingPlantMapping = new ArrayList<>();

		        if (attributes.containsKey("plants")) {
		            String existingJson = attributes.get("plants").get(0);
		            existingPlantMapping = objectMapper.readValue(existingJson, new TypeReference<>() {});
		        }

		        // Step 3: Merge newPlantMapping with existingPlantMapping
		        for (Map<String, List<Map<String, List<String>>>> newVertical : userPlantMapping) {
		            for (Map.Entry<String, List<Map<String, List<String>>>> newVerticalEntry : newVertical.entrySet()) {
		                String newVerticalId = newVerticalEntry.getKey();
		                List<Map<String, List<String>>> newSites = newVerticalEntry.getValue();

		                // Check if vertical already exists
		                Optional<Map<String, List<Map<String, List<String>>>>> existingVerticalOpt = existingPlantMapping.stream()
		                    .filter(v -> v.containsKey(newVerticalId))
		                    .findFirst();

		                if (existingVerticalOpt.isPresent()) {
		                    Map<String, List<Map<String, List<String>>>> existingVertical = existingVerticalOpt.get();
		                    List<Map<String, List<String>>> existingSites = existingVertical.get(newVerticalId);

		                    for (Map<String, List<String>> newSiteMap : newSites) {
		                        for (Map.Entry<String, List<String>> newSiteEntry : newSiteMap.entrySet()) {
		                            String newSiteId = newSiteEntry.getKey();
		                            List<String> newPlantIds = newSiteEntry.getValue();

		                            // Find existing site if any
		                            Optional<Map<String, List<String>>> existingSiteOpt = existingSites.stream()
		                                .filter(s -> s.containsKey(newSiteId))
		                                .findFirst();

		                            if (existingSiteOpt.isPresent()) {
		                                Map<String, List<String>> existingSite = existingSiteOpt.get();
		                                List<String> existingPlantIds = existingSite.get(newSiteId);

		                                // Merge plantIds (avoid duplicates)
		                                for (String newPlantId : newPlantIds) {
		                                    if (!existingPlantIds.contains(newPlantId)) {
		                                        existingPlantIds.add(newPlantId);
		                                    }
		                                }
		                            } else {
		                                // Add new site entry
		                                existingSites.add(Map.of(newSiteId, new ArrayList<>(newPlantIds)));
		                            }
		                        }
		                    }
		                } else {
		                    // Add new vertical entry
		                    existingPlantMapping.add(Map.of(newVerticalId, newSites));
		                }
		            }
		        }

		        // Step 4: Set merged plantMapping back to user attribute
		        String finalPlantMappingJson = objectMapper.writeValueAsString(existingPlantMapping);
		        attributes.put("plants", Collections.singletonList(finalPlantMappingJson));
		        user.setAttributes(attributes);
		        userResource.update(user);

		        System.out.println("User attributes updated..");

		        // Assign realm role
		        if (roleObj instanceof String && !((String) roleObj).isBlank()) {
		            String roleName = (String) roleObj;
		            RoleRepresentation roleToAdd = keycloak.realm(keycloakRealmName).roles().get(roleName)
		                .toRepresentation();
		            userResource.roles().realmLevel().add(Collections.singletonList(roleToAdd));
		        }
		    }

		    result.put("status", 200);
		    result.put("message", "Users updated successfully.");
		    result.put("plantMapping", plantMapping);

		} catch (Exception e) {
		    throw new Exception("Failed to update users. Reason: " + e.getMessage(), e);
		}

		return result;
	}

	/**
	 * Assigns realm roles to users in Keycloak. Each user can have a different set of roles.
	 * Roles are added additively (existing roles are not removed).
	 */
	public Map<String, Object> assignRolesToUsers(List<BulkRoleAssignmentRequest.UserRoleAssignment> assignments)
			throws Exception {
		Map<String, Object> result = new HashMap<>();

		if (assignments == null || assignments.isEmpty()) {
			throw new IllegalArgumentException("assignments must not be null or empty.");
		}

		Keycloak keycloak = keycloakAdminClient.getInstance();

		try {
			// Collect all unique role names across assignments for a single lookup pass
			Set<String> allRoleNames = new HashSet<>();
			for (BulkRoleAssignmentRequest.UserRoleAssignment assignment : assignments) {
				if (assignment.getUserId() == null || assignment.getUserId().isBlank()) {
					throw new IllegalArgumentException("Each assignment must include a non-blank userId.");
				}
				if (assignment.getRoles() == null || assignment.getRoles().isEmpty()) {
					throw new IllegalArgumentException(
							"Assignment for userId " + assignment.getUserId() + " must include at least one role.");
				}
				assignment.getRoles().stream()
						.filter(r -> r != null && !r.isBlank())
						.forEach(allRoleNames::add);
			}

			if (allRoleNames.isEmpty()) {
				throw new IllegalArgumentException("At least one non-blank role name is required.");
			}

			Map<String, RoleRepresentation> roleCache = new HashMap<>();
			List<String> unresolvedRoles = new ArrayList<>();

			for (String roleName : allRoleNames) {
				try {
					RoleRepresentation role = keycloak.realm(keycloakRealmName)
							.roles()
							.get(roleName)
							.toRepresentation();
					roleCache.put(roleName, role);
				} catch (Exception ex) {
					unresolvedRoles.add(roleName);
				}
			}

			if (!unresolvedRoles.isEmpty()) {
				throw new IllegalArgumentException("Unknown realm roles: " + unresolvedRoles);
			}

			List<Map<String, Object>> assignmentResults = new ArrayList<>();
			List<String> failedUserIds = new ArrayList<>();

			for (BulkRoleAssignmentRequest.UserRoleAssignment assignment : assignments) {
				String userId = assignment.getUserId();
				List<String> roleNames = assignment.getRoles().stream()
						.filter(r -> r != null && !r.isBlank())
						.distinct()
						.collect(Collectors.toList());

				Map<String, Object> userResult = new HashMap<>();
				userResult.put("userId", userId);
				userResult.put("requestedRoles", roleNames);

				try {
					List<RoleRepresentation> rolesToAdd = roleNames.stream()
							.map(roleCache::get)
							.collect(Collectors.toList());

					UserResource userResource = keycloak.realm(keycloakRealmName).users().get(userId);
					userResource.toRepresentation();
					userResource.roles().realmLevel().add(rolesToAdd);

					List<String> currentRoles = userResource.roles().realmLevel().listEffective(false).stream()
							.map(RoleRepresentation::getName)
							.collect(Collectors.toList());

					userResult.put("status", "success");
					userResult.put("assignedRoles", currentRoles);
				} catch (Exception ex) {
					failedUserIds.add(userId);
					userResult.put("status", "failed");
					userResult.put("error", ex.getMessage());
				}
				assignmentResults.add(userResult);
			}

			result.put("status", failedUserIds.isEmpty() ? 200 : 207);
			result.put("message", failedUserIds.isEmpty()
					? "Roles assigned successfully to all users."
					: "Roles assigned with partial failures.");
			result.put("data", assignmentResults);
			if (!failedUserIds.isEmpty()) {
				result.put("failedUserIds", failedUserIds);
			}
		} catch (IllegalArgumentException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new Exception("Failed to assign roles to users: " + ex.getMessage(), ex);
		}

		return result;
	}

	/**
	 * Lists realm roles with optional search and pagination.
	 * {@code q} filters by role name or description (case-insensitive contains).
	 * {@code page} is 1-based (default 1). {@code size} defaults to 20 (max 100).
	 */
	public Map<String, Object> getRealmRoles(String q, Integer page, Integer size) throws Exception {
		Map<String, Object> result = new HashMap<String, Object>();
		Keycloak keycloak = keycloakAdminClient.getInstance();

		try {
			List<RoleRepresentation> realmRoles = keycloak.realm(keycloakRealmName).roles().list();

			if (q != null && !q.isBlank()) {
				String needle = q.trim().toLowerCase();
				realmRoles = realmRoles.stream()
						.filter(role -> {
							String name = role.getName() != null ? role.getName().toLowerCase() : "";
							String description = role.getDescription() != null
									? role.getDescription().toLowerCase()
									: "";
							return name.contains(needle) || description.contains(needle);
						})
						.collect(Collectors.toList());
				result.put("q", q.trim());
			}

			int pageNumber = (page == null || page < 1) ? 1 : page;
			int pageSize = (size == null || size < 1) ? 20 : Math.min(size, 100);
			int total = realmRoles.size();
			int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
			int fromIndex = Math.min((pageNumber - 1) * pageSize, total);
			int toIndex = Math.min(fromIndex + pageSize, total);
			List<RoleRepresentation> pageData = realmRoles.subList(fromIndex, toIndex);

			result.put("status", 200);
			result.put("message", "User roles fetched successfully.");
			result.put("data", pageData);
			result.put("page", pageNumber);
			result.put("size", pageSize);
			result.put("total", total);
			result.put("totalPages", totalPages);
		} catch (Exception ex) {
			throw new Exception("Failed to fetch user roles:" + ex.getMessage(), ex);
		}

		return result;
	}

	public Map<String, Object> getRealmRoles(String q) throws Exception {
		return getRealmRoles(q, null, null);
	}

	public Map<String, Object> getRealmRoles() throws Exception {
		return getRealmRoles(null, null, null);
	}

	/**
	 * Creates a new realm role in Keycloak.
	 */
	public Map<String, Object> createRealmRole(String name, String description) throws Exception {
		Map<String, Object> result = new HashMap<>();

		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Role name must not be null or blank.");
		}

		String roleName = name.trim();
		Keycloak keycloak = keycloakAdminClient.getInstance();

		try {
			try {
				keycloak.realm(keycloakRealmName).roles().get(roleName).toRepresentation();
				result.put("status", 409);
				result.put("message", "Role already exists: " + roleName);
				return result;
			} catch (Exception ignored) {
				// Role does not exist — proceed with creation
			}

			RoleRepresentation role = new RoleRepresentation();
			role.setName(roleName);
			if (description != null && !description.isBlank()) {
				role.setDescription(description.trim());
			}

			keycloak.realm(keycloakRealmName).roles().create(role);

			RoleRepresentation created = keycloak.realm(keycloakRealmName).roles().get(roleName).toRepresentation();

			result.put("status", 201);
			result.put("message", "Role created successfully.");
			result.put("data", created);
		} catch (IllegalArgumentException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new Exception("Failed to create role: " + ex.getMessage(), ex);
		}

		return result;
	}

	/**
	 * Deletes a realm role from Keycloak.
	 */
	public Map<String, Object> deleteRealmRole(String name) throws Exception {
		Map<String, Object> result = new HashMap<>();

		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Role name must not be null or blank.");
		}

		String roleName = name.trim();
		Keycloak keycloak = keycloakAdminClient.getInstance();

		try {
			try {
				keycloak.realm(keycloakRealmName).roles().get(roleName).toRepresentation();
			} catch (Exception ex) {
				result.put("status", 404);
				result.put("message", "Role not found: " + roleName);
				return result;
			}

			keycloak.realm(keycloakRealmName).roles().deleteRole(roleName);

			result.put("status", 200);
			result.put("message", "Role deleted successfully.");
			result.put("data", Map.of("name", roleName));
		} catch (IllegalArgumentException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new Exception("Failed to delete role: " + ex.getMessage(), ex);
		}

		return result;
	}

	/**
	 * Fetches direct realm roles assigned to a specific user.
	 */
	public Map<String, Object> getUserRoles(String userId) throws Exception {
		Map<String, Object> result = new HashMap<>();

		if (userId == null || userId.isBlank()) {
			throw new IllegalArgumentException("userId must not be null or blank.");
		}

		Keycloak keycloak = keycloakAdminClient.getInstance();

		try {
			UserResource userResource = keycloak.realm(keycloakRealmName).users().get(userId);
			UserRepresentation user = userResource.toRepresentation();

			List<RoleRepresentation> directRoles = userResource.roles().realmLevel().listEffective(false);
			List<String> roleNames = directRoles.stream()
					.map(RoleRepresentation::getName)
					.collect(Collectors.toList());

			Map<String, Object> data = new HashMap<>();
			data.put("userId", user.getId());
			data.put("username", user.getUsername());
			data.put("roles", roleNames);
			data.put("roleDetails", directRoles);

			result.put("status", 200);
			result.put("message", "Roles fetched successfully for user.");
			result.put("data", data);
		} catch (IllegalArgumentException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new Exception("Failed to fetch roles for user " + userId + ": " + ex.getMessage(), ex);
		}

		return result;
	}

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
	    	ex.printStackTrace();
	        throw new Exception("Failed to search users: " + ex.getMessage(), ex);
	    }

	    return result;
	}

	public Map<String, Object> revokeUserAccess(String userId, Map<String, Object> data) throws Exception {
		Map<String, Object> result = new HashMap<>();
		Keycloak keycloak = keycloakAdminClient.getInstance();
		List<UserScreenMapping> mappings = new ArrayList<>();
		try {
			UserResource userResource = keycloak.realm(keycloakRealmName).users().get(userId);
			UserRepresentation user = userResource.toRepresentation();

			Object attrObj = data.get("attributes");
			Object roleObj = data.get("role");

		    List<Map<String, List<Map<String, List<String>>>>> plantMapping = buildPlantMapping(attrObj);

			// Prepare mapping for this user
			List<Map<String, List<Map<String, List<String>>>>> userPlantMapping = new ArrayList<>();
			

			if (attrObj instanceof Map) {
				Map<String, Object> attrMap = (Map<String, Object>) attrObj;
				List<Map<String, Object>> plants = (List<Map<String, Object>>) attrMap.get("plants");

				for (Map<String, Object> vertical : plants) {
					String verticalId = (String) vertical.get("verticalId");
					List<Map<String, Object>> sites = (List<Map<String, Object>>) vertical.get("sites");

					List<Map<String, List<String>>> siteEntries = new ArrayList<>();

					for (Map<String, Object> site : sites) {
						String siteId = (String) site.get("siteId");
						List<Map<String, Object>> plantList = (List<Map<String, Object>>) site.get("plants");

						List<String> plantIds = new ArrayList<>();

						for (Map<String, Object> plant : plantList) {
							String plantId = (String) plant.get("plantId");
							userScreenMappingRepository.deleteAllByUserId(userId,plantId);
							List<String> screens = (List<String>) plant.get("screens");
							List<String> permissions = (List<String>) plant.get("permission");

							plantIds.add(plantId);

							ObjectMapper objectMapper = new ObjectMapper();
							String permissionsString = objectMapper.writeValueAsString(permissions);

							
							List<UserScreenMapping> newMappings = new ArrayList<>();
							

							for (String screen : screens) {
								UserScreenMapping userScreenMapping = new UserScreenMapping();
								userScreenMapping.setId(UUID.randomUUID());
								userScreenMapping.setUserId(UUID.fromString(userId));
								userScreenMapping.setPlantFKId(UUID.fromString(plantId));
								userScreenMapping.setVerticalFKId(UUID.fromString(verticalId));
								userScreenMapping.setScreenCode(screen);
								userScreenMapping.setPermissions(permissionsString);

								newMappings.add(userScreenMapping);
								
							}

							mappings=(userScreenMappingRepository.saveAll(newMappings));
							
						}

						siteEntries.add(Map.of(siteId, plantIds));
					}

					userPlantMapping.add(Map.of(verticalId, siteEntries));
				}
			}

			System.out.println("User screen mapping saved.");

			Map<String, List<String>> attributes = Optional.ofNullable(user.getAttributes()).orElseGet(HashMap::new);

			ObjectMapper objectMapper = new ObjectMapper();

			String finalPlantMappingJson = objectMapper.writeValueAsString(plantMapping);
			attributes.put("plants", Collections.singletonList(finalPlantMappingJson));
			user.setAttributes(attributes);
			userResource.update(user);

			System.out.println("User attributes updated..");

			result.put("status", 200);
			result.put("message", "User access revoked successfully.");
			result.put("data", user);
		} catch (Exception ex) {
			throw new Exception("Failed to revoke user access: " + ex.getMessage(), ex);
		}

		return result;
	}
	
	private List<Map<String, List<Map<String, List<String>>>>> buildPlantMapping(Object attrObj) {
	    if (!(attrObj instanceof Map)) {
	        return new ArrayList<>();
	    }

	    Map<String, Object> attrMap = (Map<String, Object>) attrObj;
	    List<Map<String, Object>> plants = (List<Map<String, Object>>) attrMap.get("plants");
	    List<Map<String, List<Map<String, List<String>>>>> plantMapping = new ArrayList<>();

	    for (Map<String, Object> vertical : plants) {
	        String verticalId = (String) vertical.get("verticalId");
	        List<Map<String, Object>> sites = (List<Map<String, Object>>) vertical.get("sites");

	        List<Map<String, List<String>>> siteEntries = new ArrayList<>();

	        for (Map<String, Object> site : sites) {
	            String siteId = (String) site.get("siteId");
	            List<Map<String, Object>> plantList = (List<Map<String, Object>>) site.get("plants");

	            List<String> plantIds = new ArrayList<>();
	            for (Map<String, Object> plant : plantList) {
	                plantIds.add((String) plant.get("plantId"));
	            }

	            siteEntries.add(Map.of(siteId, plantIds));
	        }

	        plantMapping.add(Map.of(verticalId, siteEntries));
	    }
	    return plantMapping;
	}

}
