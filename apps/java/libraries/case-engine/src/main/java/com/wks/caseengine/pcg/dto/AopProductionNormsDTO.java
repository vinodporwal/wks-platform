package com.wks.caseengine.pcg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class AopProductionNormsDTO {

    private String particulars;
    private Double value;
}
