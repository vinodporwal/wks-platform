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
@Table(name = "ShutdownSummaryLastFourYear")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShutdownSummaryLastFourYear {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "lastFourYears")
    private String lastFourYears;

    @Column(name = "TotalAvailableHours")
    private Double totalAvailableHours;

    @Column(name = "BudgetedShutdownHours")
    private Double budgetedShutdownHours;

    @Column(name = "ActualNoOfTurnaroundHrs")
    private Double actualNoOfTurnaroundHrs;

    @Column(name = "ActualNoOfPlannedSD")
    private Double actualNoOfPlannedSD;

    @Column(name = "ActualNoOfRoutineSDHrs")
    private Double actualNoOfRoutineSDHrs;

    @Column(name = "TotalActualPlannedSDHrs")
    private Double totalActualPlannedSDHrs;

    @Column(name = "Process")
    private Double process;

    @Column(name = "Mech")
    private Double mech;

    @Column(name = "Inst")
    private Double inst;

    @Column(name = "Elect")
    private Double elect;

    @Column(name = "Utility")
    private Double utility;

    @Column(name = "UpStreamDownStream")
    private Double upStreamDownStream;

    @Column(name = "ExtFeedStock")
    private Double extFeedStock;

    @Column(name = "Business")
    private Double business;

    @Column(name = "Others")
    private Double others;

    @Column(name = "TotalUnplannedSD")
    private Double totalUnplannedSD;

    @Column(name = "UnplannedSlowdownHours")
    private Double unplannedSlowdownHours;

    @Column(name = "Year", length = 7)
    private String year;

    @Column(name = "Plant_FK_Id")
    private UUID plantFkId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CreatedOn")
    private Date createdOn;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ModifiedOn")
    private Date modifiedOn;

    @Column(name = "UpdatedBy", length = 100)
    private String updatedBy;

    @Column(name = "Remarks")
    private String remarks;
}

