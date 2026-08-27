package com.wks.caseengine.cpp.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO for the Average Asset Loading output grid.
 *
 * <p>Mapped from the result set of stored procedure {@code dbo.CPP_GetAverageAssetLoading},
 * whose columns are read by name:
 * <pre>
 *   cppPlantId, cppPlantName, generatingPlantName,
 *   assetId, assetName, assetType, assetCategory,
 *   utilityName, uom, materialName, issuingPlantName, issuingUom, loadingUom,
 *   Apr..Mar
 * </pre>
 */
@Data
public class AverageAssetLoadingDTO {

    @JsonProperty("cppPlantId")
    private UUID cppPlantId;

    private String cppPlantName;

    private String generatingPlantName;

    @JsonProperty("assetId")
    private UUID assetId;

    private String assetName;

    private String assetType;

    private String assetCategory;

    private String utilityName;

    private String uom;

    private String materialName;

    private String issuingPlantName;

    private String issuingUom;

    private String loadingUom;

    private BigDecimal apr;
    private BigDecimal may;
    private BigDecimal jun;
    private BigDecimal jul;
    private BigDecimal aug;
    private BigDecimal sep;
    private BigDecimal oct;
    private BigDecimal nov;
    private BigDecimal dec;
    private BigDecimal jan;
    private BigDecimal feb;
    private BigDecimal mar;
}
