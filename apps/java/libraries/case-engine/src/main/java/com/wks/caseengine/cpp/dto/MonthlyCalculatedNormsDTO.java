package com.wks.caseengine.cpp.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO for the Monthly Calculated Norms output grid.
 *
 * <p>Mapped from the result set of stored procedure
 * {@code dbo.CPP_GetMonthWiseFixedCalculatedUtilityNorms}, whose columns are read by name:
 * <pre>
 *   Id, FinancialYear, NormsHeader_Id, Plant_FK_Id, PlantName, plantCode,
 *   CPPPlantId, CPPPlant, UtilityName, UtilityId, UtilityUOM,
 *   AccountName, MaterialName, IssuingPlantName, IssuingUOM, MaterialId, NormType,
 *   aprNorms, junNorms, mayNorms, julNorms, augNorms, sepNorms,
 *   octNorms, novNorms, decNorms, janNorms, febNorms, marNorms, CreatedDate
 * </pre>
 *
 * <p>Field names follow the convention used by the
 * {@code Outputs/monthly-calculated-norms/index.js} grid (camelCase), matching the
 * FixedNorms response shape but without the annual {@code actualNorm} column.
 */
@Data
public class MonthlyCalculatedNormsDTO {

    @JsonProperty("id")
    private UUID id;

    private String financialYear;

    @JsonProperty("normsHeaderId")
    private UUID normsHeaderId;

    @JsonProperty("plantFkId")
    private UUID plantFkId;

    private String generatingPlantName;

    private String plantCode;

    @JsonProperty("cppPlantId")
    private UUID cppPlantId;

    private String cppPlantName;

    private String utilityName;

    private String utilityId;

    private String uom;

    private String accountName;

    private String materialName;

    private String issuingPlantName;

    private String issuingUom;

    private String materialId;

    private String normTypeName;

    private BigDecimal aprNorms;
    private BigDecimal mayNorms;
    private BigDecimal junNorms;
    private BigDecimal julNorms;
    private BigDecimal augNorms;
    private BigDecimal sepNorms;
    private BigDecimal octNorms;
    private BigDecimal novNorms;
    private BigDecimal decNorms;
    private BigDecimal janNorms;
    private BigDecimal febNorms;
    private BigDecimal marNorms;
}
