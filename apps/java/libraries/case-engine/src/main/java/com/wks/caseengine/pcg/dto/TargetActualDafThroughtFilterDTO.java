package com.wks.caseengine.pcg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class TargetActualDafThroughtFilterDTO {

    private UUID normParameterId;
    private String displayName;
    private Double targetValue;
    private Double range;
    private String remarks;
    private String aopYear;
    private UUID plantId;
}
