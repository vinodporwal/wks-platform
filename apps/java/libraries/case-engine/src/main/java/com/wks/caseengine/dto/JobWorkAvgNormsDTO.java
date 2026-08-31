package com.wks.caseengine.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobWorkAvgNormsDTO {
    
    private UUID materialId;
    private String sapMatCode;
    private String materialName;
    private String materialDisplayName;
    private String uom;
    private Boolean isEditable;
    private Boolean isVisible;
    private UUID plantId;
    private UUID groupFkId;
    private String groupName;
    private String groupDisplayName;
    private UUID txnId;
    private String aopYear;
    private Double value;
    private String remarks;
    private String plantName;
    private String status;
    private String saveStatus;
    private String errorDescription;
    private String errDescription;
}
