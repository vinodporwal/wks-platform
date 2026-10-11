package com.wks.caseengine.cpp.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CPPFuelRatioDTO {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("cppPlantFkId")
    private UUID cppPlantFkId;

    @JsonProperty("fuelFkId")
    private UUID fuelFkId;

    @JsonProperty("fuelName")
    private String fuelName;

    @JsonProperty("gcv")
    private Double gcv;

    @JsonProperty("percentageByWt")
    private Double percentageByWt;

    @JsonProperty("remarks")
    private String remarks;

    @JsonProperty("aopYear")
    private String aopYear;
}
