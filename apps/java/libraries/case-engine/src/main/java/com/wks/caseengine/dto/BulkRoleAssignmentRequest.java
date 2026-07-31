package com.wks.caseengine.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class BulkRoleAssignmentRequest {
	private List<UserRoleAssignment> assignments;

	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	@Data
	public static class UserRoleAssignment {
		private String userId;
		private List<String> roles;
	}
}
