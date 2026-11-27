package com.wks.caseengine.cpp.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class JMDFixedConsumptionCreateRequestDto {

    private UUID id;
    private UUID parentPlantId;
    private UUID recieverPlantId;
    private UUID receiverCostCenterId;
    private UUID costCenterId;
    private UUID senderNormParameterId;
    private UUID cppUtilityId;
    private String remarks;
    private String aopYear;
}
