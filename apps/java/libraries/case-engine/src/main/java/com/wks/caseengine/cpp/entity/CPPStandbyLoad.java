package com.wks.caseengine.cpp.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "CPPStandByLoad", schema = "dbo")
@Data
public class CPPStandbyLoad {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "Asset_FK_Id")
    private UUID assetFkId;

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

    @Column(name = "AOPYear")
    private String aopYear;

    @Column(name = "Remarks")
    private String remarks;

    @Column(name = "CreatedDate")
    private LocalDateTime createdDate;

    @Column(name = "UpdatedDate")
    private LocalDateTime updatedDate;

    @Column(name = "Type")
    private String type;

    @Column(name = "UOM")
    private String uom;
}
