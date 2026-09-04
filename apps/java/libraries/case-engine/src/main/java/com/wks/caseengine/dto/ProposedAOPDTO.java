package com.wks.caseengine.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProposedAOPDTO {
    
    private UUID id;
    private UUID normParameterId;
    private UUID normParameterTypeId;
    private String normParameterTypeDisplayName;
    private String productName;
    private String uom;
    private Double lastFY;
    private Double sysGrn;
    private Double proposed;
    private String remarks;
    private UUID plantId;
    private String aopYear;
    private UUID gradeId;
    private String sapCode;
    private String saveStatus;
    private String errDescription;
}
