package com.wks.caseengine.cpp.dto.norm;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class JMDCPPNormPricesResponseDTO {

    private Long id;

    @JsonProperty("cppMonthWisePriceId")
    private UUID cppMonthWisePriceId;

    @JsonProperty("cppPlantId")
    private UUID cppPlantId;

    private String cppPlantName;

    @JsonProperty("normsHeaderFkId")
    private UUID normsHeaderFkId;

    private String generatingPlantName;
    private String utilityName;
    private String utilityId;
    private String uom;
    private String accountName;
    private String materialName;
    private String materialId;
    private String issuingPlantName;
    private String issuingUom;

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
    private String modifiedBy;

    private String saveStatus;
    private String errDescription;
    private String valueType;

    @JsonProperty("dataHash")
    private String dataHash;
}
