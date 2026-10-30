package com.wks.caseengine.cpp.dto.norm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class CPPEdRpoMappingDTO {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("cppPlantFkId")
    private UUID cppPlantFkId;

    @JsonProperty("cppPlantName")
    private String cppPlantName;

    @JsonProperty("cppPlantCode")
    private String cppPlantCode;

    @JsonProperty("normheaderfkid")
    private UUID normheaderfkid;

    @JsonProperty("generatingPlantFkId")
    private UUID generatingPlantFkId;

    @JsonProperty("generatingPlantName")
    private String generatingPlantName;

    @JsonProperty("generatingPlantCode")
    private String generatingPlantCode;

    @JsonProperty("issuingPlantFkId")
    private UUID issuingPlantFkId;

    @JsonProperty("issuingPlantName")
    private String issuingPlantName;

    @JsonProperty("issuingPlantCode")
    private String issuingPlantCode;

    @JsonProperty("utilityNormParameterFkId")
    private UUID utilityNormParameterFkId;

    @JsonProperty("utilityName")
    private String utilityName;

    @JsonProperty("utilitySAPMaterialCode")
    private String utilitySAPMaterialCode;

    @JsonProperty("materialNormParameterFkId")
    private UUID materialNormParameterFkId;

    @JsonProperty("materialName")
    private String materialName;

    @JsonProperty("materialSAPMaterialCode")
    private String materialSAPMaterialCode;

    @JsonProperty("aopYear")
    private String aopYear;

    @JsonProperty("exempt")
    private Boolean exempt;

    @JsonProperty("remark")
    private String remark;

    // Used for import flow: error tracking
    private String saveStatus;
    private String errDescription;

    // Used for change-detection during import
    @JsonProperty("dataHash")
    private String dataHash;
}

