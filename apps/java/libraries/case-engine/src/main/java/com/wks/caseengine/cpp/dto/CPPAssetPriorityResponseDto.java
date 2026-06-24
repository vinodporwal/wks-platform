package com.wks.caseengine.cpp.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CPPAssetPriorityResponseDto {
    
    private UUID id;
    private UUID assetFkId;
    private UUID plantFkId;
    
    private Integer apr;
    private Integer may;
    private Integer jun;
    private Integer jul;
    private Integer aug;
    private Integer sep;
    private Integer oct;
    private Integer nov;
    private Integer dec;
    private Integer jan;
    private Integer feb;
    private Integer mar;
    
    private String remarks;
    private String createdDate;
    private String modifiedDate;
    
    // Asset details
    private String assetName;
    private String assetType;
    
    // Plant details
    private String plantName;
    
    // Asset category
    private String assetCategory;
    
    // Additional IDs
    private UUID siteFkId;
    private UUID verticalFkId;
}
