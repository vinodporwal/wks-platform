package com.wks.caseengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class OptimizingVariablesDropdownDTO {
    private String id;
    private String name;
    private String displayName;
    private String uom;
    private String normParameterTypeFKId;
    private Boolean isEditable;
    private Boolean isVisible;
    private Integer displayOrder;
    private String april;
    private String may;
    private String june;
    private String july;
    private String august;
    private String september;
    private String october;
    private String november;
    private String december;
    private String january;
    private String february;
    private String march;
    private String remarks;
}
