package com.wks.caseengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TankConfigurationDTO {
    
    private String normParameterFKId;
    private Boolean jan;
    private Boolean feb;
    private Boolean mar;
    private Boolean apr;
    private Boolean may;
    private Boolean jun;
    private Boolean jul;
    private Boolean aug;
    private Boolean sep;
    private Boolean oct;
    private Boolean nov;
    private Boolean dec;
    private Integer volume;
    private String remarks;
    private String auditYear;
    private String uom;
    private String normTypeName;
    private Boolean isEditable;
    private String displayName;
}
