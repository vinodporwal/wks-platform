package com.wks.caseengine.db2.entity;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "TurnAroundPlan")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnAroundPlanDB2 {
    @Id
    @UuidGenerator
    @Column(name = "Id", nullable = false, unique = true)
    private UUID id;

    @Column(name = "RowNo", nullable = true)
    private Integer rowNumber;

    @Column(name = "Plant_FK_Id")
    private UUID plantFkId;

    @Column(name = "AOPYear", nullable = true, length = 255)
    private String aopYear;

    @Column(name = "activity", nullable = false, length = 255)
    private String activity;

    @Column(name = "toDate", nullable = true, length = 255)
    private String toDate;

    @Column(name = "Remark", nullable = true, length = 255)
    private String remark;

    @Column(name = "fromDate", nullable = true, length = 255)
    private String fromDate;

    @Column(name = "durationInHrs", nullable = true)
    private Double durationInHrs;

    @Column(name = "periodInMonths", nullable = true)
    private Double periodInMonths;

    @Column(name = "ReportType")
    private String reportType;
}
