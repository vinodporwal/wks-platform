package com.wks.caseengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NormConfigurationDTO {

    private String normParameterFkId;
    private Double jan;
    private Double feb;
    private Double mar;
    private Double apr;
    private Double may;
    private Double jun;
    private Double jul;
    private Double aug;
    private Double sep;
    private Double oct;
    private Double nov;
    private Double dec;
    private String remarks;
    private String auditYear;
    private String uom;
    private String normTypeName;
    private Boolean isEditable;
    private String displayName;
    private String type;
    private String saveStatus;
    private String errDescription;
}

