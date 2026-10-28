package com.wks.caseengine.dto;

import java.util.UUID;

import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Configuration
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class NormBasisPCGDTO {
	
	private UUID normParameterFkId;
    private String displayName;
    private String dependantAttributeId;
    private String targetValue;
    private String range;
    private String selection;
    private String remarks;
    private String uom;
    private String normParameterTypeDisplayName;
    private String dataType;
    private String dependentAttributeConfig;

}
