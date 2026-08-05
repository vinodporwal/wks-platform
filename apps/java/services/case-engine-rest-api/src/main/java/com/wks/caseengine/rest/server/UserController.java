package com.wks.caseengine.rest.server;

import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.wks.caseengine.dto.BulkRoleAssignmentRequest;
import com.wks.caseengine.dto.RoleCreateRequest;
import com.wks.caseengine.dto.UsersByRolesRequest;
import com.wks.caseengine.service.KeycloakUserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/task/users")
public class UserController {

	private final KeycloakUserService userService;

	public UserController(KeycloakUserService userService) {
		this.userService = userService;
	}

	@GetMapping
	public Map<String, Object> getUsers() throws Exception {
		return userService.getUsers();
	}

	@GetMapping("/role/{roleName}")
	public List<UserRepresentation> getUsersWithRole(@PathVariable String roleName) throws Exception {

		return userService.getUsersWithRole(roleName);
	}

	/**
	 * List users that hold any of the given realm roles (union), with pagination.
	 * POST /task/users/by-roles
	 * Body: { "roles": ["cts_head", "plant_manager"], "page": 1, "size": 20 }
	 */
	@PostMapping("/by-roles")
	public Map<String, Object> getUsersByRoles(@RequestBody UsersByRolesRequest request) throws Exception {
		return userService.getUsersByRoles(request.getRoles(), request.getPage(), request.getSize());
	}

	@PutMapping("/revoke-access/{userId}")
	public Map<String, Object> revokeUserAccess(@PathVariable String userId, @RequestBody Map<String, Object> data) throws Exception {
		return userService.revokeUserAccess(userId, data);
	}

	@PutMapping()
	public Map<String, Object> updateUser(@RequestBody Map<String, Object> data) throws Exception {
		return userService.updateUser(data);
	}

	/**
	 * Assign realm roles to users. Each user can have a different set of roles.
	 * POST /task/users/roles/assign
	 * Body: { "assignments": [ { "userId": "...", "roles": ["role1"] }, ... ] }
	 */
	@PostMapping("/roles/assign")
	public ResponseEntity<Map<String, Object>> assignRolesToUsers(@RequestBody BulkRoleAssignmentRequest request) throws Exception {
		Map<String, Object> result = userService.assignRolesToUsers(request.getAssignments());
		int status = result.get("status") instanceof Integer ? (Integer) result.get("status") : 200;
		return ResponseEntity.status(status == 207 ? HttpStatus.MULTI_STATUS : HttpStatus.OK).body(result);
	}

	/**
	 * Update realm roles from an Excel file (replace user's direct realm roles).
	 * POST /task/users/roles/assign-excel
	 * multipart form field "file" (.xlsx)
	 * Columns: username | roles  (roles comma-separated)
	 */
	@PostMapping(value = "/roles/assign-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Map<String, Object>> assignRolesFromExcel(@RequestParam("file") MultipartFile file)
			throws Exception {
		Map<String, Object> result = userService.assignRolesFromExcel(file);
		int status = result.get("status") instanceof Integer ? (Integer) result.get("status") : 200;
		HttpStatus httpStatus = status == 207 ? HttpStatus.MULTI_STATUS
				: status == 400 ? HttpStatus.BAD_REQUEST
				: HttpStatus.OK;
		return ResponseEntity.status(httpStatus).body(result);
	}

	/**
	 * Create a new realm role.
	 * POST /task/users/roles
	 * Body: { "name": "role_name", "description": "optional" }
	 */
	@PostMapping("/roles")
	public ResponseEntity<Map<String, Object>> createRole(@RequestBody RoleCreateRequest request) throws Exception {
		Map<String, Object> result = userService.createRealmRole(request.getName(), request.getDescription());
		int status = result.get("status") instanceof Integer ? (Integer) result.get("status") : 201;
		HttpStatus httpStatus = status == 409 ? HttpStatus.CONFLICT
				: status == 201 ? HttpStatus.CREATED
				: HttpStatus.OK;
		return ResponseEntity.status(httpStatus).body(result);
	}

	/**
	 * Delete a realm role.
	 * DELETE /task/users/roles/{roleName}
	 */
	@DeleteMapping("/roles/{roleName}")
	public ResponseEntity<Map<String, Object>> deleteRole(@PathVariable String roleName) throws Exception {
		Map<String, Object> result = userService.deleteRealmRole(roleName);
		int status = result.get("status") instanceof Integer ? (Integer) result.get("status") : 200;
		HttpStatus httpStatus = status == 404 ? HttpStatus.NOT_FOUND : HttpStatus.OK;
		return ResponseEntity.status(httpStatus).body(result);
	}

	/**
	 * List available realm roles.
	 * GET /task/users/roles
	 * Optional: ?q=searchTerm&page=1&size=20
	 */
	@GetMapping("/roles")
	public Map<String, Object> getRealmRoles(
			@RequestParam(value = "q", required = false) String q,
			@RequestParam(value = "page", required = false) Integer page,
			@RequestParam(value = "size", required = false) Integer size) throws Exception {
		return userService.getRealmRoles(q, page, size);
	}

	/**
	 * Fetch roles associated with a specific user.
	 * GET /task/users/{userId}/roles
	 */
	@GetMapping("/{userId}/roles")
	public Map<String, Object> getUserRoles(@PathVariable String userId) throws Exception {
		return userService.getUserRoles(userId);
	}

	/**
	 * Unassign a role from a specific user.
	 * DELETE /task/users/{userId}/roles/{roleName}
	 */
	@DeleteMapping("/{userId}/roles/{roleName}")
	public ResponseEntity<Map<String, Object>> unassignRoleFromUser(
			@PathVariable String userId,
			@PathVariable String roleName) throws Exception {
		Map<String, Object> result = userService.unassignRoleFromUser(userId, roleName);
		int status = result.get("status") instanceof Integer ? (Integer) result.get("status") : 200;
		return ResponseEntity.status(status).body(result);
	}
	
	@GetMapping("/groups")
    public Map<String, Object> getUserGroups() throws Exception {
        return userService.getAllGroups();
    }
	
	@GetMapping("/search")
	public Map<String, Object> getUsers(@RequestParam("search") String search) throws Exception {
		return userService.searchUsers(search);
	}
}
