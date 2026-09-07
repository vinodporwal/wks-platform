package com.wks.caseengine.cpp.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA entity for {@code dbo.CPPInterSitePowerTransfer}.
 *
 * <p>Stores month-wise inter-site power transfer entries between CPP plants
 * (From Plant → To Plant) for a given financial year.</p>
 */
@Entity
@Table(name = "CPPInterSitePowerTransfer", schema = "dbo")
@Getter
@Setter
public class CPPInterSitePowerTransfer {

    @Id
    @Column(name = "Id")
    private UUID id;

    @Column(name = "FromPlantId")
    private UUID fromPlantId;

    @Column(name = "ToPlantId")
    private UUID toPlantId;

    @Column(name = "UOM")
    private String uom;

    @Column(name = "FinancialYear")
    private String financialYear;

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

    @Column(name = "CreatedDate")
    private LocalDateTime createdDate;

    @Column(name = "UpdatedDate")
    private LocalDateTime updatedDate;
}
