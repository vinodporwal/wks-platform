package com.wks.caseengine.cpp.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "CPPSteamAssetsPriority", schema = "dbo")
@Data
public class CPPSteamAssetPriority {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "Asset_FK_Id")
    private UUID assetFkId;

    @Column(name = "Plant_FK_Id")
    private UUID plantFkId;

    @Column(name = "Apr")
    private Integer apr;

    @Column(name = "May")
    private Integer may;

    @Column(name = "Jun")
    private Integer jun;

    @Column(name = "Jul")
    private Integer jul;

    @Column(name = "Aug")
    private Integer aug;

    @Column(name = "Sep")
    private Integer sep;

    @Column(name = "Oct")
    private Integer oct;

    @Column(name = "Nov")
    private Integer nov;

    @Column(name = "Dec")
    private Integer dec;

    @Column(name = "Jan")
    private Integer jan;

    @Column(name = "Feb")
    private Integer feb;

    @Column(name = "Mar")
    private Integer mar;

    @Column(name = "Remarks")
    private String remarks;

    @Column(name = "CreatedDate")
    private LocalDateTime createdDate;

    @Column(name = "UpdatedDate")
    private LocalDateTime updatedDate;

    @Column(name = "AOPYear")
    private String aopYear;
}
