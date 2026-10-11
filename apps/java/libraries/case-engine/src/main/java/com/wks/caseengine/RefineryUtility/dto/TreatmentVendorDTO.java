package com.wks.caseengine.RefineryUtility.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data 
@AllArgsConstructor
@NoArgsConstructor
@Builder 
public class TreatmentVendorDTO {
    
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
