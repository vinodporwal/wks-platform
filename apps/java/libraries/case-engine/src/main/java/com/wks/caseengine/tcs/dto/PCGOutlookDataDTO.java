package com.wks.caseengine.tcs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PCGOutlookDataDTO {
    
    private String month; // The formatted string e.g., "Jan-26"
    
    // Gasifier Availability
    private Double gasifierAvailabilityTotal;
    private Double gasifierAvailabilityDta;
    private Double gasifierAvailabilitySez;
    
    // SynGas Production
    private Double synGasProductionTotal;
    private Double synGasProductionDta;
    private Double synGasProductionSez;
    
    // CGE (%)
    private Double cge;
    
    // Additional Fields
    private String remark;
    private Boolean isCarryForward;
    
    // Optional: Fields for import error handling
    private String saveStatus;
    private String errDescription;
}
