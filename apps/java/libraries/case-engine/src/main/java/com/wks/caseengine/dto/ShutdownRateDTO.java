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
public class ShutdownRateDTO {
	
	private String normParameterFKId;
	private Double majorShutdown;
	private Double oneDayShutdown;
	private String remarks;
	private String auditYear;
	private String uom;
	private String normTypeName;
	private Boolean isEditable;
	private String displayName;
	private String type;
}
