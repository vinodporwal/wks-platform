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

	private String materialName;
    private String month;
    private String year;
    private double expectedValue;
    private double actualValue;
    private double difference;

    private String gradeId;
    private String gradeName;   // agar available ho to
    private double enteredValue;
    private double suggestedValue;

	 
}