package com.wks.caseengine.dto;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MXOReprocessingDTO {
    
    private String month;
    private String mode;
    private Double mXOGeneration_tph;
    private Double onstream_hrs;
    private Double mXOgeneration_TPM;
    private Double mXODowntime_hrs;
    private Double maxMXOReprocessingRate_tph;
    private String aopYear;
    private String mXODowntimeInHrsId;
    private String maxMXOReprocessingRateInTphId;
    private String remarks;
    private String errorMessage;
    private String saveStatus;
}
