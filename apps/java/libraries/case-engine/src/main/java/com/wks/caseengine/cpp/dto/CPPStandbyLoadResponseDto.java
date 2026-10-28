package com.wks.caseengine.cpp.dto;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

@Data
public class CPPStandbyLoadResponseDto {

    @JsonProperty("id")
    private UUID id;
    private UUID assetFkId;
    private String assetName;
    private UUID plantFkId;

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
    private String uom;

    private String createdDate;
    private String modifiedDate;

    private String generatingPlant;
    private String utilityDistributed;
    private String distributedSapCode;
    private String utilityGenerated;
    private String generatedUtilityCode;
    private String cppPlantName;
    private String plantCode;
    private String type;

    private UUID siteFkId;
    private UUID verticalFkId;

    @JsonProperty("dataHash")
    private String dataHash;
}
