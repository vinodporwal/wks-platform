package com.wks.caseengine.RefineryUtility.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SelectedPlantOwnerDTO {

    private String id;                   
    private String plantOwnerSelection;  
    private String plantId;             
    private String remarks;     
    private String normParameterId;         
    private String saveStatus;
    private String errDescription;
}
