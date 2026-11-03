package com.wks.caseengine.cpp.dto.norm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CPPActualRateEDRPORequestDTO {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("cppPlantFkId")
    private UUID cppPlantFkId;

    @JsonProperty("cppPlantName")
    private String cppPlantName;

    @JsonProperty("accountName")
    private String accountName;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("rate")
    private String rate;

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

    @JsonProperty("aopYear")
    private String aopYear;

    @JsonProperty("remarks")
    private String remarks;

    @JsonProperty("modifiedBy")
    private String modifiedBy;
}

