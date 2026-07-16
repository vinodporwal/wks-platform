package com.wks.caseengine.cpp.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class FixedConsumptionCreateRequestDto {

    private UUID id;
    private UUID parentPlantId;
    private UUID recieverPlantId;
    private UUID costCenterId;
    private UUID senderPlantId;
    private UUID cppUtilityId;
    private String remarks;
    private String aopYear;
}
