package com.wks.caseengine.db2.entity;

import java.util.Date;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AnnualProductionPlanReport")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnualProductionPlanReportDB2 {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "Id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "RowNo", nullable = false)
    private Integer rowNo;

    @Column(name = "activity", length = 300)
    private String activity;

    @Column(name = "periodFrom")
    private Date periodFrom;

    @Column(name = "periodTo")
    private Date periodTo;

    @Column(name = "rateValue")
    private Double rateValue;

    @Column(name = "durationHours")
    private Double durationHours;

    @Column(name = "maxHourlyRateValue", length = 20)
    private String maxHourlyRateValue;

    @Column(name = "UOM", length = 50)
    private String uom;

    @Column(name = "Remark", length = 500)
    private String remark;

    @Column(name = "Plant_FK_Id", nullable = false)
    private UUID plantFkId;

    @Column(name = "AOPYear", length = 20)
    private String aopYear;

    @Column(name = "ReportType", length = 20)
    private String reportType;
}
