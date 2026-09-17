package com.wks.caseengine.vgoht.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConstantDTO {
    
    private String id;
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
    private String saveStatus;
    private String errDescription;
    
}
