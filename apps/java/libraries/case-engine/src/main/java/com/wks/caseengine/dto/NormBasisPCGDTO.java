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
	
    private String name;
    private String displayName;
    private String uom;
    private String attributeValue;
    private String config;
    private String remarks;
    private String type;
    private String normParameterType;
    private String displayOrder;
    private boolean isEditable;
    private String dependantAttributeId;
    private UUID normParameterId;

}
