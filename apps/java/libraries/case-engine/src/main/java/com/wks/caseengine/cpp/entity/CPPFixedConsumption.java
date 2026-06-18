package com.wks.caseengine.cpp.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "CPPFixConsuption", schema = "dbo")
@Getter
@Setter
public class CPPFixedConsumption {

    @Id
    @Column(name = "Id")
    private UUID id;

    @Column(name = "Plant_FK_Id")
    private UUID plantFkId;

    @Column(name = "CPP_CostCenter_FK_Id")
    private UUID cppCostCenterFkId;

    @Column(name = "NormParameter_FK_Id")
    private UUID normParameterFkId;

    @Column(name = "Apr")
    private Double apr;

    @Column(name = "May")
    private Double may;

    @Column(name = "Jun")
    private Double jun;

    @Column(name = "Jul")
    private Double jul;

    @Column(name = "Aug")
    private Double aug;

    @Column(name = "Sep")
    private Double sep;

    @Column(name = "Oct")
    private Double oct;

    @Column(name = "Nov")
    private Double nov;

    @Column(name = "Dec")
    private Double dec;

    @Column(name = "Jan")
    private Double jan;

    @Column(name = "Feb")
    private Double feb;

    @Column(name = "Mar")
    private Double mar;

    @Column(name = "Remarks")
    private String remarks;

    @Column(name = "AOPYear")
    private String aopYear;

    @Column(name = "CreatedDate")
    private LocalDateTime createdDate;

    @Column(name = "UpdatedDate")
    private LocalDateTime updatedDate;
}
