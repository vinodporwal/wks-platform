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
@Table(name = "MajorPeopleInitiative")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MajorPeopleInitiative {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "PlantId")
    private UUID plantId;

    @Column(name = "InitiativeDescription", columnDefinition = "VARCHAR(MAX)")
    private String initiativeDescription;

    @Column(name = "ExpectedOutcome", columnDefinition = "VARCHAR(MAX)")
    private String expectedOutcome;

    @Column(name = "TargetDate")
    @Temporal(TemporalType.DATE)
    private Date targetDate;

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
