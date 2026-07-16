package com.wks.caseengine.cpp.dto.norm;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CPPNormPricesRequestDTO {

    @JsonProperty("cppMonthWisePriceId")
    private UUID cppMonthWisePriceId;

    @JsonProperty("normsHeaderFkId")
    private UUID normsHeaderFkId;

    private String aopYear;

    private BigDecimal aprPrice;
    private BigDecimal mayPrice;
    private BigDecimal junPrice;
    private BigDecimal julPrice;
    private BigDecimal augPrice;
    private BigDecimal sepPrice;
    private BigDecimal octPrice;
    private BigDecimal novPrice;
    private BigDecimal decPrice;
    private BigDecimal janPrice;
    private BigDecimal febPrice;
    private BigDecimal marPrice;

    private String remarks;
    private String priceSource;

    private String generatingPlantName;
    private String utilityName;
    private String utilityId;
    private String uom;
    private String accountName;
    private String materialName;
    private String materialId;
    private String issuingPlantName;
    private String issuingUom;
    private String valueType;
}
