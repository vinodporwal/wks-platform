package com.wks.caseengine.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VcmAvailabilityConstantDTO {
    
    private UUID id;
    private UUID normParameterFkId;
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
    private boolean isEditable;
    private String displayName;
}
