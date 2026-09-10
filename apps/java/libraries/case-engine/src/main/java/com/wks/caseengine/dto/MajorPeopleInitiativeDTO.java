package com.wks.caseengine.dto;

import java.util.Date;

import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Sp_GetMajorPeopleInitiative stored procedure result row.
 */
@Configuration
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MajorPeopleInitiativeDTO {
    private String id;
    private String plantId;
    private String plantName;
    private String plantDisplayName;
    private String plant;
    private String initiativeDescription;
    private String expectedOutcome;
    private String outcome;
    private Date targetDate;
    private String siteId;
    private String siteFkId;
    private String aopYear;
    private String modifiedBy;
    private String updatedBy;
    private Date modifiedOn;
    private Date updatedDateTime;
    private String saveStatus;
    private String errDescription;
}
