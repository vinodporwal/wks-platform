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
public class TargetGasifierFilterDTO {

    private UUID id;
    private String gOperation;
    private UUID plantId;
    private String aopYear;
    private UUID normParameterId;
}
