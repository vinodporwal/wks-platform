package com.wks.caseengine.RefineryUtility.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthWiseConstantsDTO {
   
    private String normParameterFKId;
    private String Name;
    private String DisplayName;
    private String UOM;
    private String normTypeName;
    private Double apr;
    private Double may;
    private Double jun;
    private Double jul;
    private Double aug;
    private Double sep;
    private Double oct;
    private Double nov;
    private Double dec;
    private Double jan;
    private Double feb;
    private Double mar;
    private String auditYear;
    private String remarks;
    private String displayOrder;
    private Boolean isEditable;
    private String saveStatus;
    private String errDescription;
    
}
