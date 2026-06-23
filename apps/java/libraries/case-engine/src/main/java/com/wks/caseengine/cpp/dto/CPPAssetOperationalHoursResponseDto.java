package com.wks.caseengine.cpp.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPPAssetOperationalHoursResponseDto {

    private UUID id;
    private UUID assetFkId;
    private String utilityDistributed;
    private String distributedSapCode;
    private String utilityGenerated;
    private String generatedUtilityCode;

    private Double apr;
    private Double may;
    private Double jun;
    private Double jul;
    private Double aug;
    private Double sep;
    private Double oct;
    private Double nov;
    private Double dec;
    private Double jan;
    private Double feb;
    private Double mar;

    private String aopYear;
    private String remarks;

    private UUID siteFkId;
    private UUID verticalFkId;
    private UUID plantFkId;

    private String createdDate;
    private String modifiedDate;

    private String assetName;
    private String plantName;
    private String assetType;
    private String assetCategory;
}
