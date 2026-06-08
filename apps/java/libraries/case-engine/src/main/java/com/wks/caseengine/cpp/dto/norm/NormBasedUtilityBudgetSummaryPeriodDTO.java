package com.wks.caseengine.cpp.dto.norm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class NormBasedUtilityBudgetSummaryPeriodDTO {

    @JsonProperty("norms")
    private Double norms;

    @JsonProperty("quantity")
    private Double quantity;

    @JsonProperty("amount")
    private Double amount;

    @JsonProperty("price")
    private Double price;

    @JsonProperty("QTY")
    private Double qty;

    @JsonProperty("generationUom")
    private String generationUom;
}
