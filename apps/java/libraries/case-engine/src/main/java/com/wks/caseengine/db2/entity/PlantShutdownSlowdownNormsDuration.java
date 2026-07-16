package com.wks.caseengine.db2.entity;

import java.util.Date;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PlantShutdownSlowdownNormsDuration")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlantShutdownSlowdownNormsDuration {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "CriticalRoutineActivity")
    private String criticalRoutineActivity;

    @Column(name = "BestAchievedLastYearFrequency")
    private Double bestAchievedLastYearFrequency;

    @Column(name = "BestAchievedLastYearDuration")
    private Double bestAchievedLastYearDuration;

    @Column(name = "BestAchievedGroupFrequency")
    private Double bestAchievedGroupFrequency;

    @Column(name = "BestAchievedGroupDuration")
    private Double bestAchievedGroupDuration;

    @Column(name = "ActualFrequency")
    private Double actualFrequency;

    @Column(name = "PrevYearDuration")
    private Double prevYearDuration;

    @Column(name = "BudgetFrequency")
    private Double budgetFrequency;

    @Column(name = "CurrentYearDuration")
    private Double currentYearDuration;

    @Column(name = "ActivitiesClubbed")
    private String activitiesClubbed;

    @Column(name = "ExplanationNotProposing")
    private String explanationNotProposing;

    @Column(name = "ThroughputReductionDuringPeriod")
    private Double throughputReductionDuringPeriod;

    @Column(name = "IsProductionLossRecoverable")
    private String isProductionLossRecoverable;

    @Column(name = "Year")
    private String year;

    @Column(name = "PlantId")
    private UUID plantId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CreatedOn")
    private Date createdOn;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ModifiedOn")
    private Date modifiedOn;

    @Column(name = "UpdatedBy")
    private String updatedBy;
    
    @Column(name = "Remark")
    private String remark;
    
    @Column(name = "Remarks")
    private String remarks;
    
    
}

