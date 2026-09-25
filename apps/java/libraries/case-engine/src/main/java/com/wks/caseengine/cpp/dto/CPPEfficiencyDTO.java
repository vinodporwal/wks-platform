package com.wks.caseengine.cpp.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CPPEfficiencyDTO {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("cppPlantFkId")
    private UUID cppPlantFkId;

    @JsonProperty("assetFkId")
    private UUID assetFkId;

    @JsonProperty("assetName")
    private String assetName;

    @JsonProperty("type")
    private String type;

    @JsonProperty("isActive")
    private Boolean isActive;

    @JsonProperty("uom")
    private String uom;

    @JsonProperty("value")
    private Double value;

    @JsonProperty("remarks")
    private String remarks;

    @JsonProperty("aopYear")
    private String aopYear;
}
