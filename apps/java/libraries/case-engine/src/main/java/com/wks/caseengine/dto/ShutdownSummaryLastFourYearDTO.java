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
public class ShutdownSummaryLastFourYearDTO {
    private String id;
    private String lastFourYears;
    private Double totalAvailableHours;
    private Double budgetedShutdownHours;
    private Double actualNoOfTurnaroundHrs;
    private Double actualNoOfPlannedSD;
    private Double actualNoOfRoutineSDHrs;
    private Double totalActualPlannedSDHrs;
    private Double process;
    private Double mech;
    private Double inst;
    private Double elect;
    private Double utility;
    private Double upStreamDownStream;
    private Double extFeedStock;
    private Double business;
    private Double others;
    private Double totalUnplannedSD;
    private Double unplannedSlowdownHours;
    private String year;
    private String plantFkId;
    private Date createdOn;
    private Date modifiedOn;
    private String updatedBy;
    private String remarks;
}

