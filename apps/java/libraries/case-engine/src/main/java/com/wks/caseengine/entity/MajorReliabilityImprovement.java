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
@Table(name = "MajorReliabilityImprovementInitiative")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MajorReliabilityImprovement {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "PlantId")
    private UUID plantId;

    @Column(name = "InitiativeDescription", columnDefinition = "varchar(max)")
    private String initiativeDescription;

    @Column(name = "Category", length = 255)
    private String category;

    @Column(name = "Cost", columnDefinition = "varchar(max)")
    private String cost;

    @Column(name = "Outcome", columnDefinition = "varchar(max)")
    private String outcome;

    @Column(name = "TargetDate")
    @Temporal(TemporalType.DATE)
    private Date targetDate;

    @Column(name = "Responsibility", length = 255)
    private String responsibility;

    @Column(name = "SiteId", nullable = false)
    private UUID siteId;

    @Column(name = "AOPYear", length = 7, nullable = false)
    private String aopYear;

    @Column(name = "ModifiedBy", length = 100)
    private String modifiedBy;

    @Column(name = "ModifiedOn")
    @Temporal(TemporalType.TIMESTAMP)
    private Date modifiedOn;
}
