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
@Table(name = "MCUCapacityUtilization")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MCUCapacityUtilization {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "Plant", length = 255)
    private String plant;

    @Column(name = "PrevAOP")
    private Integer prevAop;

    @Column(name = "PrevActual")
    private Integer prevActual;

    @Column(name = "AOP")
    private Integer aop;

    @Column(name = "Remarks", length = 500)
    private String remarks;

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
