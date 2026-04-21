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
@Table(name = "MonthwiseOperatingHours")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthwiseOperatingHours {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "Month")
    private String month;

    @Column(name = "TotalAvailableHrs")
    private Double totalAvailableHrs;

    @Column(name = "PlannedTurnaroundHrs")
    private Double plannedTurnaroundHrs;

    @Column(name = "PlannedShutdownOtherThanTurnaroundHrs")
    private Double plannedShutdownOtherThanTurnaroundHrs;

    @Column(name = "RoutineShutdownHrs")
    private Double routineShutdownHrs;

    @Column(name = "SlowdownHrs")
    private Double slowdownHrs;

    @Column(name = "NetOperatingHours")
    private Double netOperatingHours;

    @Column(name = "Remarks")
    private String remarks;

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
}

