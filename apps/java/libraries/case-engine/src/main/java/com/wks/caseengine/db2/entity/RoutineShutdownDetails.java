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
@Table(name = "RoutineShutdownDetails")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutineShutdownDetails {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "Activities")
    private String activities;

    @Column(name = "April")
    private Double april;

    @Column(name = "May")
    private Double may;

    @Column(name = "June")
    private Double june;

    @Column(name = "July")
    private Double july;

    @Column(name = "August")
    private Double august;

    @Column(name = "September")
    private Double september;

    @Column(name = "October")
    private Double october;

    @Column(name = "November")
    private Double november;

    @Column(name = "December")
    private Double december;

    @Column(name = "January")
    private Double january;

    @Column(name = "February")
    private Double february;

    @Column(name = "March")
    private Double march;

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

