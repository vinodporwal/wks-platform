package com.wks.caseengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationErrorDTO {

    private String materialTypeId;
    private String materialTypeName;
    private String materialId;
    private String materialName;
    private String uom;

    private String month;
    private String year;

    // Expected (Weighted Avg)
    private double expectedValue;

    // Actual (MCU Norm)
    private double actualValue;

    private double difference;
    private String matchStatus;

    // Optional (future use)
    private String gradeId;
    private String gradeName;
    private double enteredValue;
    private double suggestedValue;
}