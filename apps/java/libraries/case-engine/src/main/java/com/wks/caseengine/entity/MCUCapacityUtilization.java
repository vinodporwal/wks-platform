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
@Table(name = "MCUCapacityUtilizationTransaction", schema = "dbo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MCUCapacityUtilization {

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

    @Column(name = "Remarks", columnDefinition = "VARCHAR(MAX)")
    private String remarks;

    @Column(name = "ModifiedBy", length = 255)
    private String modifiedBy;

    @Column(name = "ModifiedOn")
    @Temporal(TemporalType.TIMESTAMP)
    private Date modifiedOn;

    @Column(name = "aopYear", length = 10)
    private String aopYear;

    @Column(name = "isEditable")
    private Boolean isEditable;

    @Column(name = "isVisible")
    private Boolean isVisible;
}
