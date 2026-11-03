package com.wks.caseengine.RefineryUtility.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AntiscalantDTO {
    
    private String normParameterFKId;
    private String name;
    private String displayName;
    private String uom;
    private String normType;
    private String april;
    private String auditYear;
    private String remarks;
    private Integer displayOrder;
    private Boolean isEditable;
    private String saveStatus;
    private String errDescription;
}
