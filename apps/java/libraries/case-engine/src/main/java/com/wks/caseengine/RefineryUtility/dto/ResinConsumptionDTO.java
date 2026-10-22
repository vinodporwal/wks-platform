package com.wks.caseengine.RefineryUtility.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResinConsumptionDTO {

    private String normParameterId;
    private String displayName;
    private Double value;
    private String remarks;
    private String aopYear;
    private String plantId;
    private String saveStatus;
    private String errDescription;
}
