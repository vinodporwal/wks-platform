package com.wks.caseengine.cpp.dto.norm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class NormBasedUtilityBudgetSummaryResponseDTO {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("generatingPlantName")
    private String generatingPlantName;

    @JsonProperty("utilityName")
    private String utilityName;

    @JsonProperty("utilityId")
    private String utilityId;

    @JsonProperty("cppPlantName")
    private String cppPlantName;

    @JsonProperty("cppPlantId")
    private String cppPlantId;

    @JsonProperty("uom")
    private String uom;

    @JsonProperty("accountName")
    private String accountName;

    @JsonProperty("materialName")
    private String materialName;

    @JsonProperty("materialId")
    private String materialId;

    @JsonProperty("issuingPlantName")
    private String issuingPlantName;

    @JsonProperty("issuingUom")
    private String issuingUom;

    @JsonProperty("generationUom")
    private String generationUom;

    @JsonProperty("normHeaderId")
    private String normHeaderId;

    @JsonProperty("q1")
    private NormBasedUtilityBudgetSummaryPeriodDTO q1;

    @JsonProperty("q2")
    private NormBasedUtilityBudgetSummaryPeriodDTO q2;

    @JsonProperty("q3")
    private NormBasedUtilityBudgetSummaryPeriodDTO q3;

    @JsonProperty("q4")
    private NormBasedUtilityBudgetSummaryPeriodDTO q4;

    @JsonProperty("annual")
    private NormBasedUtilityBudgetSummaryPeriodDTO annual;
}
