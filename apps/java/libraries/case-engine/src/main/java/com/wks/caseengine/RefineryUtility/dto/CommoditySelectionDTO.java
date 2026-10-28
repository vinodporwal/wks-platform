package com.wks.caseengine.RefineryUtility.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder 
public class CommoditySelectionDTO {
    
    private String normParameterFKId;
    private String name;
    private String DisplayName;
    private String uom;
    private String normTypeName;
    private String isChecked;
    private String auditYear;
    private String remarks;
    private Integer displayOrder;
    private Boolean isEditable;
    private String saveStatus;
    private String errDescription;
}
