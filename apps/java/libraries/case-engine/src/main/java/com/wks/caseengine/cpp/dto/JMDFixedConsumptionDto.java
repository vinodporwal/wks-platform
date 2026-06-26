package com.wks.caseengine.cpp.dto;

import java.util.UUID;

import lombok.Data;

/**
 * Response DTO for the JMD Fixed Consumption GET API.
 * This is a separate replica of {@link FixedConsumptionDto} that additionally exposes
 * the {@code id} column returned by the stored procedure
 * (CPP_GetFixedConsumptionByPlant), which is required by the frontend but was
 * previously absent from the API response.
 */
@Data
public class JMDFixedConsumptionDto {

    private UUID id;                    // row Id from SP result

    private String plant;               // plantName
    private String plantId;             // plantCode
    private String costCenter;          // costCenterName
    private String costCenterId;        // costCenterCode
    private String cppUtility;          // NormParameter.DisplayName / utilityName
    private String cppUtilityId;        // utilitySAP
    private String cppPlant;            // utilityPlantName
    private String cppPlantId;          // utilityPlantCode
    private String uom;                 // uom
    private String normParameterId;     // normParameterId
    private Double april;
    private Double may;
    private Double june;
    private Double july;
    private Double aug;
    private Double sep;
    private Double oct;
    private Double nov;
    private Double dec;
    private Double jan;
    private Double feb;
    private Double mar;
    private Double grandTotal;
    private String remarks;
    private UUID remarkId;
    private UUID costCenter_FK_Id;
    private UUID normParameter_FK_Id;

    // Fields for import/export tracking
    private String saveStatus;
    private String errDescription;

    // Hash of month values + remarks, written as hidden column at export time.
    // Used at import time to detect which rows were actually modified.
    private String rowHash;
}
