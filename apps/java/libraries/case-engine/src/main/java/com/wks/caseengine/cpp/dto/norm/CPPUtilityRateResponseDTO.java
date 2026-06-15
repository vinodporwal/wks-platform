package com.wks.caseengine.cpp.dto.norm;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class CPPUtilityRateResponseDTO {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("siteDescription")
    private String siteDescription;

    @JsonProperty("utilityPlant")
    private String utilityPlant;

    @JsonProperty("utilityPlantId")
    private String utilityPlantId;

    @JsonProperty("utilityName")
    private String utilityName;

    @JsonProperty("utilityId")
    private String utilityId;

    @JsonProperty("uom")
    private String uom;

    @JsonProperty("apr")
    private BigDecimal apr;

    @JsonProperty("may")
    private BigDecimal may;

    @JsonProperty("jun")
    private BigDecimal jun;

    @JsonProperty("jul")
    private BigDecimal jul;

    @JsonProperty("aug")
    private BigDecimal aug;

    @JsonProperty("sep")
    private BigDecimal sep;

    @JsonProperty("oct")
    private BigDecimal oct;

    @JsonProperty("nov")
    private BigDecimal nov;

    @JsonProperty("dec")
    private BigDecimal dec;

    @JsonProperty("jan")
    private BigDecimal jan;

    @JsonProperty("feb")
    private BigDecimal feb;

    @JsonProperty("mar")
    private BigDecimal mar;

    @JsonProperty("weightedAvgPrice")
    private BigDecimal weightedAvgPrice;
}
