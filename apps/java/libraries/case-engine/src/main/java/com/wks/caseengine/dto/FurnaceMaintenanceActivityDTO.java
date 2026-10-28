package com.wks.caseengine.dto;

import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Configuration
public class FurnaceMaintenanceActivityDTO {
	
	private String id;
	private String name;
	private Integer actualRunLength;
	private String remarks;
}
