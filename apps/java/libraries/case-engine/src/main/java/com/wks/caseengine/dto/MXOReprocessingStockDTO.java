package com.wks.caseengine.dto;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MXOReprocessingStockDTO {

    private String month;
    private Double mXOOpeningStockInMT;
    private Double mXOGeneration;
    private Double mXOReprocessing;
    private Double mXOClosingStockInMT;
    private String mXOOpeningStockId;
    private String mXOClosingStockId;
    private String saveStatus;
    private String errorMessage;
}
