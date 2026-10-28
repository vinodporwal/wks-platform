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
@Table(name = "NormAttributeTransactionGradeWise")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NormAttributeTransactionGradeWise {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "Grade_FK_ID")
    private UUID gradeFkId;

    @Column(name = "Material_FK_ID")
    private UUID materialFkId;

    @Column(name = "Plant_FK_ID")
    private UUID plantFkId;

    @Column(name = "AttributeValue")
    private Double attributeValue;

    @Column(name = "USER_NAME")
    private String userName;

    @Column(name = "AOPYear")
    private String aopYear;

    @Column(name = "CreatedOn")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdOn;

    @Column(name = "ModifiedOn")
    @Temporal(TemporalType.TIMESTAMP)
    private Date modifiedOn;
}
