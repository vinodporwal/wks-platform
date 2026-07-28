package com.wks.caseengine.dto;

import lombok.Data;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@AllArgsConstructor
@Builder
public class ProdSchedulingConfigDTO {
    

    private UUID id;
    private Integer batchPerDay;
    private Double productionPerBatch;
    private Integer sdWashAfterBatch;
    private Integer sdFlushAfterBatch;
    private Integer sdWashHr;
    private Integer sdFlushHr;
    private Integer quarterlySDHr;
    private String aopYear;
    private UUID plantId;
}
