package com.wks.caseengine.cpp.dto.norm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CPPRPOMonthlyConfigRequestDTO {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("cppPlantId")
    private UUID cppPlantId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("type")
    private String type;

    @JsonProperty("aopYear")
    private String aopYear;

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

    @JsonProperty("isActive")
    private Integer isActive;

    @JsonProperty("remarks")
    private String remarks;

    @JsonProperty("modifiedBy")
    private String modifiedBy;
}

