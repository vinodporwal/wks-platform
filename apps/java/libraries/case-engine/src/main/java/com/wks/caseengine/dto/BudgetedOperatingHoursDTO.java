package com.wks.caseengine.dto;

import java.util.Date;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetedOperatingHoursDTO {
    
   

    private UUID id;
    private UUID gradeId;
    private String displayName;
    private boolean isEditable;
    private String uom;
    private UUID plantId;
    private String aopYear;
    private UUID lineId;
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
    private String remarks;
    private String modifiedBy;
    private Date modifiedDateTime;
    private String saveStatus;
    private String errDescription;
    private String lineName;
}
