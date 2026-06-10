package com.wks.caseengine.coker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class CokerConfigurationDto {
    private String id;
    private String normParameterFKId;
    private String jan;
    private String feb;
    private String mar;
    private String apr;
    private String may;
    private String jun;
    private String jul;
    private String aug;
    private String sep;
    private String oct;
    private String nov;
    private String dec;
    private String march;
    private String remarks;
    private String auditYear;
    private String UOM;
    private String lossCategory;
    private String normType;
    private String ConfigTypeDisplayName;
    private String TypeDisplayName;
    private String ConfigTypeName;
    private String TypeName;
    private String normParameterTypeDisplayName;
    private Boolean isEditable;
    private String productName;
    private String saveStatus;
    private String errDescription;
    private String type;
    private String version;
    private String vertical;
    private String normParamId;

}
