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
@Table(name = "PlannedShutdownDetails")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlannedShutdownDetails {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "Activities")
    private String activities;

    @Temporal(TemporalType.DATE)
    @Column(name = "ShutdownFrom")
    private Date shutdownFrom;

    @Temporal(TemporalType.DATE)
    @Column(name = "ShutdownTo")
    private Date shutdownTo;

    @Column(name = "DurationHrs")
    private Double durationHrs;

    @Column(name = "Remarks")
    private String remarks;

    @Column(name = "Year")
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

