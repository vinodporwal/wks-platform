package com.wks.caseengine.cpp.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "CPP_PlantFuelAvailabilityMonthly", schema = "dbo")
@Data
public class CPPPlantFuelAvailabilityMonthly {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "Plant_FK_Id")
    private UUID plantFkId;

    @Column(name = "AOPYear")
    private String aopYear;

    // April
    @Column(name = "AprFuel_FK_Id")
    private UUID aprFuelFkId;

    @Column(name = "AprPriority")
    private Integer aprPriority;

    @Column(name = "AprQuantity")
    private Double aprQuantity;

    // May
    @Column(name = "MayFuel_FK_Id")
    private UUID mayFuelFkId;

    @Column(name = "MayPriority")
    private Integer mayPriority;

    @Column(name = "MayQuantity")
    private Double mayQuantity;

    // June
    @Column(name = "JunFuel_FK_Id")
    private UUID junFuelFkId;

    @Column(name = "JunPriority")
    private Integer junPriority;

    @Column(name = "JunQuantity")
    private Double junQuantity;

    // July
    @Column(name = "JulFuel_FK_Id")
    private UUID julFuelFkId;

    @Column(name = "JulPriority")
    private Integer julPriority;

    @Column(name = "JulQuantity")
    private Double julQuantity;

    // August
    @Column(name = "AugFuel_FK_Id")
    private UUID augFuelFkId;

    @Column(name = "AugPriority")
    private Integer augPriority;

    @Column(name = "AugQuantity")
    private Double augQuantity;

    // September
    @Column(name = "SepFuel_FK_Id")
    private UUID sepFuelFkId;

    @Column(name = "SepPriority")
    private Integer sepPriority;

    @Column(name = "SepQuantity")
    private Double sepQuantity;

    // October
    @Column(name = "OctFuel_FK_Id")
    private UUID octFuelFkId;

    @Column(name = "OctPriority")
    private Integer octPriority;

    @Column(name = "OctQuantity")
    private Double octQuantity;

    // November
    @Column(name = "NovFuel_FK_Id")
    private UUID novFuelFkId;

    @Column(name = "NovPriority")
    private Integer novPriority;

    @Column(name = "NovQuantity")
    private Double novQuantity;

    // December
    @Column(name = "DecFuel_FK_Id")
    private UUID decFuelFkId;

    @Column(name = "DecPriority")
    private Integer decPriority;

    @Column(name = "DecQuantity")
    private Double decQuantity;

    // January
    @Column(name = "JanFuel_FK_Id")
    private UUID janFuelFkId;

    @Column(name = "JanPriority")
    private Integer janPriority;

    @Column(name = "JanQuantity")
    private Double janQuantity;

    // February
    @Column(name = "FebFuel_FK_Id")
    private UUID febFuelFkId;

    @Column(name = "FebPriority")
    private Integer febPriority;

    @Column(name = "FebQuantity")
    private Double febQuantity;

    // March
    @Column(name = "MarFuel_FK_Id")
    private UUID marFuelFkId;

    @Column(name = "MarPriority")
    private Integer marPriority;

    @Column(name = "MarQuantity")
    private Double marQuantity;

    @Column(name = "Remarks")
    private String remarks;

    @Column(name = "CreatedDate", updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "UpdatedDate")
    private LocalDateTime updatedDate;
}
