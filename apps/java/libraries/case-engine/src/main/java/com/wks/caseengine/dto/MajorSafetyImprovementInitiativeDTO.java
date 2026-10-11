package com.wks.caseengine.dto;

import java.util.Date;

import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Sp_GetMajorSafetyImprovementInitiative stored procedure result row.
 */
@Configuration
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MajorSafetyImprovementInitiativeDTO {
    private String id;
    private String plantId;
    private String plantName;
    private String plantDisplayName;
    private String plant;
    private String initiativeDescription;
    private String category;
    private String outcome;
    private String recommendation;
    private Date targetDate;
    private String responsibility;
    private String remark;
    private String aopYear;
    private String siteId;
    private String siteFkId;
    private String modifiedBy;
    private String updatedBy;
    private Date modifiedOn;
    private Date updatedDateTime;
    private String saveStatus;
    private String errDescription;
}
