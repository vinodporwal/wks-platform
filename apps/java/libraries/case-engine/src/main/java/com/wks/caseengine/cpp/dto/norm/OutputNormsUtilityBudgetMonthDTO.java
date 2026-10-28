package com.wks.caseengine.cpp.dto.norm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class OutputNormsUtilityBudgetMonthDTO {

    @JsonProperty("Norms")
    private Double norms;

    @JsonProperty("Quantity")
    private Double quantity;

    @JsonProperty("Amount")
    private Double amount;

    @JsonProperty("Price")
    private Double price;

    @JsonProperty("financialYearMonthFkId")
    private String financialYearMonthFkId;

    @JsonProperty("QTY")
    private Double qty;

    @JsonProperty("generationUom")
    private String generationUom;

    @JsonProperty("Remarks")
    private String remarks;
}

