package com.wks.caseengine.dto;

import java.util.Date;

import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Sp_GetMajorProfitImprovement stored procedure result row.
 */
@Configuration
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MajorProfitImprovementDTO {
    private String id;
    private String plantId;
    private String plantName;
    private String plantDisplayName;
    private String plant;
    private String initiativeDescription;
    private String category;
    private Double cost;
    private String outcome;
    private Date targetDate;
    private String remark;
    private String siteFkId;
    private String aopYear;
    private String modifiedBy;
    private Date modifiedOn;
    private String saveStatus;
    private String errDescription;
}
