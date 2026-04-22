package com.wks.caseengine.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthwiseOperatingHoursDTO {
    private String id;
    private String month;

    private Double totalAvailableHrs;
    private Double plannedTurnaroundHrs;
    private Double plannedShutdownOtherThanTurnaroundHrs;
    private Double routineShutdownHrs;
    private Double slowdownHrs;
    private Double netOperatingHours;

    private String remarks;

    private String year;
    private String plantFkId;

    private Date createdOn;
    private Date modifiedOn;
    private String updatedBy;
}

