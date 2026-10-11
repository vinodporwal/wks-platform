package com.wks.caseengine.cpp.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "CPP_FuelRatio", schema = "dbo")
@Data
public class CPPFuelRatio {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "CPPPlant_FK_Id", nullable = false)
    private UUID cppPlantFkId;

    @Column(name = "Fuel_FK_Id")
    private UUID fuelFkId;

    @Column(name = "FuelName", nullable = false, length = 100)
    private String fuelName;

    @Column(name = "GCV")
    private Double gcv;

    @Column(name = "PercentageByWt")
    private Double percentageByWt;

    @Column(name = "Remarks", length = 500)
    private String remarks;

    @Column(name = "AOPYear", nullable = false, length = 20)
    private String aopYear;

    @Column(name = "CreatedDate", updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "UpdatedDate")
    private LocalDateTime updatedDate;
}
