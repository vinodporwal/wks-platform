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
public class PlantShutdownSlowdownNormsDurationDTO {
    private String id;
    private String criticalRoutineActivity;
    private Double bestAchievedLastYearFrequency;
    private Double bestAchievedLastYearDuration;
    private Double bestAchievedGroupFrequency;
    private Double bestAchievedGroupDuration;
    private Double actualFrequency;
    private Double prevYearDuration;
    private Double budgetFrequency;
    private Double currentYearDuration;
    private String activitiesClubbed;
    private String explanationNotProposing;
    private Double throughputReductionDuringPeriod;
    private String isProductionLossRecoverable;
    private String year;
    private String plantId;
    private Date createdOn;
    private Date modifiedOn;
    private String updatedBy;
    private String remarks;
}

