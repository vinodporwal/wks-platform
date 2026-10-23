package com.wks.caseengine.cpp.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FuelAvailabilityTransactionDTO {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("cppPlantFkId")
    private UUID cppPlantFkId;

    @JsonProperty("cppPlantName")
    private String cppPlantName;

    @JsonProperty("fuelId")
    private UUID fuelId;

    @JsonProperty("fuelName")
    private String fuelName;

    @JsonProperty("fuelDisplayName")
    private String fuelDisplayName;

    @JsonProperty("fuelCode")
    private String fuelCode;

    @JsonProperty("categoryId")
    private UUID categoryId;

    @JsonProperty("categoryName")
    private String categoryName;

    @JsonProperty("categoryDisplayName")
    private String categoryDisplayName;

    @JsonProperty("calorificValue")
    private Double calorificValue;

    @JsonProperty("type")
    private String type;

    @JsonProperty("uom")
    private String uom;

    @JsonProperty("financialYear")
    private String financialYear;

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

    private Double costApr;
    private Double costMay;
    private Double costJun;
    private Double costJul;
    private Double costAug;
    private Double costSep;
    private Double costOct;
    private Double costNov;
    private Double costDec;
    private Double costJan;
    private Double costFeb;
    private Double costMar;

    @JsonProperty("remarks")
    private String remarks;

    @JsonProperty("dataHash")
    private String dataHash;
}
