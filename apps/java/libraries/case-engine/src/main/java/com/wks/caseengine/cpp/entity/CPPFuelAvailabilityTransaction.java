package com.wks.caseengine.cpp.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "CPPFuelAvailabilityTransaction", schema = "dbo")
@Data
public class CPPFuelAvailabilityTransaction {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "CPPPlantFKId", nullable = false)
    private UUID cppPlantFkId;

    @Column(name = "Fuel_FK_Id", nullable = false)
    private UUID fuelFkId;

    @Column(name = "Type", nullable = false)
    private String type;

    @Column(name = "UOM")
    private String uom;

    @Column(name = "CalorificValue")
    private Double calorificValue;

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

    @Column(name = "Cost_Apr")
    private Double costApr;

    @Column(name = "Cost_May")
    private Double costMay;

    @Column(name = "Cost_Jun")
    private Double costJun;

    @Column(name = "Cost_Jul")
    private Double costJul;

    @Column(name = "Cost_Aug")
    private Double costAug;

    @Column(name = "Cost_Sep")
    private Double costSep;

    @Column(name = "Cost_Oct")
    private Double costOct;

    @Column(name = "Cost_Nov")
    private Double costNov;

    @Column(name = "Cost_Dec")
    private Double costDec;

    @Column(name = "Cost_Jan")
    private Double costJan;

    @Column(name = "Cost_Feb")
    private Double costFeb;

    @Column(name = "Cost_Mar")
    private Double costMar;

    @Column(name = "FinancialYear", nullable = false)
    private String financialYear;

    @Column(name = "Remarks")
    private String remarks;

    @Column(name = "CreatedDate", updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "UpdatedDate")
    private LocalDateTime updatedDate;
}
