package com.wks.caseengine.cpp.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SpinningMarginDTO {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("cppPlantName")
    private String cppPlantName;

    @JsonProperty("utilityName")
    private String utilityName;

    @JsonProperty("utilityCode")
    private String utilityCode;

    @JsonProperty("uom")
    private String uom;

    @JsonProperty("aopYear")
    private String aopYear;

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

    @JsonProperty("remarks")
    private String remarks;

    @JsonProperty("dataHash")
    private String dataHash;
}
