package com.wks.caseengine.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "MajorProfitImprovement")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MajorProfitImprovement {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "Plant", length = 255)
    private String plant;

    @Column(name = "InitiativeDescription", length = 500)
    private String initiativeDescription;

    @Column(name = "Category", length = 255)
    private String category;

    @Column(name = "Outcome", length = 255)
    private String outcome;

    @Column(name = "Recommendation", length = 500)
    private String recommendation;

    @Column(name = "TargetDate")
    @Temporal(TemporalType.DATE)
    private Date targetDate;

    @Column(name = "Remark", length = 500)
    private String remark;

    @Column(name = "AOPYear", length = 7)
    private String aopYear;

    @Column(name = "Site_FK_Id")
    private UUID siteFkId;

    @Column(name = "UpdatedBy", length = 255)
    private String updatedBy;

    @Column(name = "UpdatedDateTime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDateTime;
}

