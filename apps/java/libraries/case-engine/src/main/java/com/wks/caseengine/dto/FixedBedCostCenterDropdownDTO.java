package com.wks.caseengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class FixedBedCostCenterDropdownDTO {

    private String masterId;
    private String value;
    private String label;
    private String displayLabel;
    private String costCenterCode;
    private String costCenterDescription;
    private Double displayOrder;
}
