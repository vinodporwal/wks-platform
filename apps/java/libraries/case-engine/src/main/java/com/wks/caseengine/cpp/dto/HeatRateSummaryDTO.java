package com.wks.caseengine.cpp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class HeatRateSummaryDTO {

    @JsonProperty("siteName")
    private String siteName;

    @JsonProperty("cppPlantId")
    private String cppPlantId;

    @JsonProperty("cppPlantName")
    private String cppPlantName;

    @JsonProperty("assetType")
    private String assetType;

    @JsonProperty("assetName")
    private String assetName;

    @JsonProperty("utilityId")
    private String utilityId;

    @JsonProperty("load")
    private Double load;

    @JsonProperty("finalHeatRate")
    private Double finalHeatRate;
}
