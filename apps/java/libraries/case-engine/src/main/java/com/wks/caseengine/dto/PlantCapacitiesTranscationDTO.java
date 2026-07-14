package com.wks.caseengine.dto;

import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Configuration
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class PlantCapacitiesTranscationDTO {
    
    private String masterId;
    private String transactionId;
    private String siteName;
    private String plantName;
    private String uom;
    private String min;
    private String max;
    private String remarks;
    private String aopYear;
    private Integer displayOrder;
    private Boolean isEditable;
    private Boolean isVisible;
    private String plantId;
}
