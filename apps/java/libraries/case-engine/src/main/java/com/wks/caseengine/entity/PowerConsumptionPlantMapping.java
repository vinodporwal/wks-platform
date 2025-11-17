package com.wks.caseengine.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

@Entity
@Table(name = "PowerConsumptionPlantMapping")
@Data
public class PowerConsumptionPlantMapping {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "Name", nullable = false, length = 255)
    private String name;

    @Column(name = "DisplayName", nullable = false, length = 255)
    private String displayName;

    @Column(name = "IsActive")
    private Boolean isActive;

    @Column(name = "DisplayOrder")
    private Integer displayOrder;
    
    @Column(name = "Site_FK_Id")
    private UUID siteFKId;

    @Column(name = "Plant_FK_Id")
    private UUID plantFKId;
}
