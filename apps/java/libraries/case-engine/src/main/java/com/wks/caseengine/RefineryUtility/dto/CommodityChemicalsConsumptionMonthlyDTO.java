package com.wks.caseengine.RefineryUtility.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommodityChemicalsConsumptionMonthlyDTO {
    
    private String normParameterFKId;
    private String name;
    private String displayName;
    private String uom;
    private String normTypeName;
    private Double april;
    private Double may;
    private Double june;
    private Double july;
    private Double august;
    private Double september;
    private Double october;
    private Double november;
    private Double december;
    private Double january;
    private Double february;
    private Double march;
    private String auditYear;
    private String remarks;
    private Integer displayOrder;
    private Boolean isEditable;
    private String saveStatus;
    private String errorMessage;
}
