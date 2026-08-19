package com.wks.caseengine.cpp.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FuelMasterWithCategoryDTO {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("fuelCode")
    private String fuelCode;

    @JsonProperty("fuelName")
    private String fuelName;

    @JsonProperty("fuelDisplayName")
    private String fuelDisplayName;

    @JsonProperty("type")
    private String type;

    @JsonProperty("uom")
    private String uom;

    @JsonProperty("categoryFkId")
    private UUID categoryFkId;

    @JsonProperty("categoryName")
    private String categoryName;

    @JsonProperty("categoryDisplayName")
    private String categoryDisplayName;
}
