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
@Table(name = "MajorSafetyImprovementInitiative")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MajorSafetyImprovementInitiative {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "PlantId")
    private UUID plantId;

    @Column(name = "InitiativeDescription", columnDefinition = "VARCHAR(MAX)")
    private String initiativeDescription;

    @Column(name = "Category", length = 255)
    private String category;

    @Column(name = "Outcome", columnDefinition = "VARCHAR(MAX)")
    private String outcome;

    @Column(name = "Recommendation", columnDefinition = "VARCHAR(MAX)")
    private String recommendation;

    @Column(name = "TargetDate")
    @Temporal(TemporalType.DATE)
    private Date targetDate;

    @Column(name = "Responsibility", length = 255)
    private String responsibility;

    @Column(name = "SiteId")
    private UUID siteId;

    @Column(name = "AOPYear", length = 7)
    private String aopYear;

    @Column(name = "ModifiedBy", length = 100)
    private String modifiedBy;

    @Column(name = "ModifiedOn")
    @Temporal(TemporalType.TIMESTAMP)
    private Date modifiedOn;
}
