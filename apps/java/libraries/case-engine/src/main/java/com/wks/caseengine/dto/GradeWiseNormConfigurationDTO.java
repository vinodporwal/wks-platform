package com.wks.caseengine.dto;

import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Elastomer_JMD_GetGradeWiseNormConfiguration stored procedure result row.
 */
@Configuration
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class GradeWiseNormConfigurationDTO {
    private String name;
    private String grade;
    private String uom;
    private Double iirR1675;
    private Double ciirC1139;
    private Double biirB2232;
    private String materialFKId;
    private String r1675Id;
    private String c1139Id;
    private String b2232Id;
}
