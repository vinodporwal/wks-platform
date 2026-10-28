package com.wks.caseengine.db2.entity;

import java.math.BigDecimal;
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
@Table(name = "RoutineShutdownPreviousYears")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutineShutdownPreviousYears {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "Activities")
    private String activities;

    @Column(name = "PrevYear1")
    private Double prevYear1;

    @Column(name = "PrevYear2")
    private Double prevYear2;

    @Column(name = "PrevYear3")
    private Double prevYear3;

    @Column(name = "PrevYear4")
    private Double prevYear4;

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

