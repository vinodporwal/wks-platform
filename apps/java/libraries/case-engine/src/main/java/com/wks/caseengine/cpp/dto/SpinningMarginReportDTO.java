package com.wks.caseengine.cpp.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO for the Spinning Margin Maintained report.
 *
 * <p>Maps rows from {@code dbo.CPPMonthlySpinningMarginMaintained} joined with
 * the Plants table to resolve the plant name and NormParameters table to
 * resolve the utility name and code. The table stores monthly
 * spinning margin values per MarginType (POWER / STEAM), Plant, and NormParameter.</p>
 */
@Data
public class SpinningMarginReportDTO {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("aopYear")
    private String aopYear;

    @JsonProperty("marginType")
    private String marginType;

    @JsonProperty("plantFkId")
    private UUID plantFkId;

    @JsonProperty("plantName")
    private String plantName;

    @JsonProperty("utilityName")
    private String utilityName;

    @JsonProperty("utilityCode")
    @JsonAlias({"sapCode", "sapMaterialCode"})
    private String utilityCode;

    @JsonProperty("normParameterFkId")
    private UUID normParameterFkId;

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
    private Double aprTarget;
    private Double mayTarget;
    private Double junTarget;
    private Double julTarget;
    private Double augTarget;
    private Double sepTarget;
    private Double octTarget;
    private Double novTarget;
    private Double decTarget;
    private Double janTarget;
    private Double febTarget;
    private Double marTarget;

    private String remarks;
    private String modifiedBy;

    @JsonProperty("dataHash")
    private String dataHash;
}
