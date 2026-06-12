package com.wks.caseengine.cpp.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Snapshot of utility rates written by the Python CPP script after each
 * monthly price calculation.  One row per
 * (CPPPlantId, FinancialYear, PlantName, UtilityName).
 *
 * The Java endpoint reads from this table directly — no stored procedure.
 */
@Entity
@Table(name = "CPPUtilityRateSnapshot", schema = "dbo", catalog = "RIL.AOP")
@Getter
@Setter
public class CPPUtilityRateSnapshot {

    @Id
    @Column(name = "Id")
    private UUID id;

    @Column(name = "CPPPlantId")
    private UUID cppPlantId;

    @Column(name = "FinancialYear")
    private String financialYear;

    @Column(name = "PlantName")
    private String plantName;

    @Column(name = "SiteDescription")
    private String siteDescription;

    @Column(name = "PlantCode")
    private String plantCode;

    @Column(name = "UtilityName")
    private String utilityName;

    @Column(name = "UtilityId")
    private String utilityId;

    @Column(name = "UOM")
    private String uom;

    @Column(name = "Apr_Price")
    private BigDecimal aprPrice;

    @Column(name = "May_Price")
    private BigDecimal mayPrice;

    @Column(name = "Jun_Price")
    private BigDecimal junPrice;

    @Column(name = "Jul_Price")
    private BigDecimal julPrice;

    @Column(name = "Aug_Price")
    private BigDecimal augPrice;

    @Column(name = "Sep_Price")
    private BigDecimal sepPrice;

    @Column(name = "Oct_Price")
    private BigDecimal octPrice;

    @Column(name = "Nov_Price")
    private BigDecimal novPrice;

    @Column(name = "Dec_Price")
    private BigDecimal decPrice;

    @Column(name = "Jan_Price")
    private BigDecimal janPrice;

    @Column(name = "Feb_Price")
    private BigDecimal febPrice;

    @Column(name = "Mar_Price")
    private BigDecimal marPrice;

    @Column(name = "WeightedAvgPrice")
    private BigDecimal weightedAvgPrice;

    @Column(name = "LastUpdatedBy")
    private String lastUpdatedBy;

    @Column(name = "LastUpdatedDate")
    private LocalDateTime lastUpdatedDate;
}
