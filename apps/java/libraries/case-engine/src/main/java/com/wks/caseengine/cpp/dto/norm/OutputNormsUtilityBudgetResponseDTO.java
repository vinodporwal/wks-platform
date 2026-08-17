package com.wks.caseengine.cpp.dto.norm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class OutputNormsUtilityBudgetResponseDTO {

    @JsonProperty("id")
    private Integer id;
    
    @JsonProperty("cppPlantId")
    private String cppPlantId;
    
    @JsonProperty("cppPlantName")
    private String cppPlantName;

    @JsonProperty("generatingPlantName")
    private String generatingPlantName;

    @JsonProperty("utilityName")
    private String utilityName;

    @JsonProperty("utilityId")
    private String utilityId;

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

    @JsonProperty("apr")
    private OutputNormsUtilityBudgetMonthDTO apr;

    @JsonProperty("may")
    private OutputNormsUtilityBudgetMonthDTO may;

    @JsonProperty("jun")
    private OutputNormsUtilityBudgetMonthDTO jun;

    @JsonProperty("jul")
    private OutputNormsUtilityBudgetMonthDTO jul;

    @JsonProperty("aug")
    private OutputNormsUtilityBudgetMonthDTO aug;

    @JsonProperty("sep")
    private OutputNormsUtilityBudgetMonthDTO sep;

    @JsonProperty("oct")
    private OutputNormsUtilityBudgetMonthDTO oct;

    @JsonProperty("nov")
    private OutputNormsUtilityBudgetMonthDTO nov;

    @JsonProperty("dec")
    private OutputNormsUtilityBudgetMonthDTO dec;

    @JsonProperty("jan")
    private OutputNormsUtilityBudgetMonthDTO jan;

    @JsonProperty("feb")
    private OutputNormsUtilityBudgetMonthDTO feb;

    @JsonProperty("mar")
    private OutputNormsUtilityBudgetMonthDTO mar;

    @JsonProperty("remarks")
    private String remarks;

    // Fields for import/export tracking
    private String saveStatus;
    private String errDescription;

    @JsonProperty("dataHash")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String dataHash;
}


