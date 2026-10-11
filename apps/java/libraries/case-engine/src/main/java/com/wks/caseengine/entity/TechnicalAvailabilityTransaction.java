package com.wks.caseengine.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "TechnicalAvailibilityTransaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicalAvailabilityTransaction {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "plantId")
    private UUID plantId;

    @Column(name = "siteId")
    private UUID siteId;

    @Column(name = "fyPrevAOP")
    private Double fyPrevAOP;

    @Column(name = "fyPrevActual")
    private Double fyPrevActual;

    @Column(name = "fyCurrentAOP")
    private Double fyCurrentAOP;

    @Column(name = "Remarks")
    private String remarks;

    @Column(name = "ModifiedBy")
    private String modifiedBy;

    @Column(name = "ModifiedOn")
    private Date modifiedOn;

    @Column(name = "aopYear")
    private String aopYear;

    @Column(name = "isEditable")
    private Boolean isEditable;

    @Column(name = "isVisible")
    private Boolean isVisible;
}
