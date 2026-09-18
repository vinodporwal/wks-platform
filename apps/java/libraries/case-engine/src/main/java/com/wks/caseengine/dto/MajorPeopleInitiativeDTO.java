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
    private String plant;
    private String initiativeDescription;
    private String outcome;
    private String recommendation;
    private Date targetDate;
    private String remark;
    private String aopYear;
    private String siteFkId;
    private String updatedBy;
    private Date updatedDateTime;
}
