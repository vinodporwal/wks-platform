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
public class NaphthaQualityDTO {

    private String section;
    private String name;
    private Double max;
    private Double min;
    private Double months;
    private String maxId;
    private String minId;
    private String monthsId;
}