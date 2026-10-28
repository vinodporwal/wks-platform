package com.wks.caseengine.db2.entity;

import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "PlantContribution")
public class PlantContributionDB2 {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ReportType")
    private String reportType;

    @Column(name = "RowNo")
    private Integer rowNo;

    @Column(name = "Material")
    private String material;

    @Column(name = "UOM")
    private String uom;

    @Column(name = "Price")
    private Integer price;

    @Column(name = "BudgetPrevYear")
    private Double budgetPrevYear;

    @Column(name = "ActualPrevYear")
    private Double actualPrevYear;

    @Column(name = "BudgetCurrentYear")
    private Double budgetCurrentYear;

    @Column(name = "BudgetPrevYearCost")
    private Double budgetPrevYearCost;

    @Column(name = "ActualPrevYearCost")
    private Double actualPrevYearCost;

    @Column(name = "BudgetCurrentYearCost")
    private Double budgetCurrentYearCost;

    @Column(name = "Plant_FK_Id")
    private UUID plantFkId;

    @Column(name = "AOPYear")
    private String aopYear;

    @Column(name = "Remark")
    private String remark;
}
