package com.wks.caseengine.entity;

import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;


@Getter
@Setter
@Entity
@Table(name = "CapexPIOPlanTransaction", schema = "dbo")
public class CapexPIOPlanTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "Id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "Proposal", length = 500)
    private String proposal;

    @Column(name = "Category", length = 255)
    private String category;

    @Column(name = "Justification", length = 1000)
    private String justification;

    @Column(name = "CostRsCr")
    private Double costRsCr;

    @Column(name = "BenefitRsCr")
    private Double benefitRsCr;

    @Column(name = "TargetPlan")
    private Date targetPlan;

    @Column(name = "StatusPlan", length = 255)
    private String statusPlan;

    @Column(name = "Remarks", length = 500)
    private String remarks;

    @Column(name = "SiteId")
    private UUID siteId;

    @Column(name = "AOPYear", length = 10)
    private String aopYear;

    @Column(name = "UpdatedBy", length = 100)
    private String updatedBy;

    @Column(name = "UpdatedDate")
    private Date updatedDate;
}
