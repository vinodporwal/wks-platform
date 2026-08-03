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
public class UsersByRolesRequest {
	private List<String> roles;
	private Integer page;
	private Integer size;
}
