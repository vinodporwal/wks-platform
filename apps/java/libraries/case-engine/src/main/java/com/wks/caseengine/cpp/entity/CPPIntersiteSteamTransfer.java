package com.wks.caseengine.cpp.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "CPP_IntersiteSteamTransfer", schema = "dbo")
@Getter
@Setter
public class CPPIntersiteSteamTransfer {

    @Id
    @Column(name = "Id")
    private UUID id;

    @Column(name = "CPPPlant_FK_Id")
    private UUID cppPlantFkId;

    @Column(name = "NormParameter_FK_Id")
    private UUID normParameterFkId;

    @Column(name = "SenderPlant_FK_Id")
    private UUID senderPlantFkId;

    @Column(name = "SenderCostCenter_FK_Id")
    private UUID senderCostCenterFkId;

    @Column(name = "ReceiverPlant_FK_Id")
    private UUID receiverPlantFkId;

    @Column(name = "ReceiverCostCenter_FK_Id")
    private UUID receiverCostCenterFkId;

    @Column(name = "AOP_Year")
    private String aopYear;

    @Column(name = "Min_Apr")
    private Double minApr;
    @Column(name = "Max_Apr")
    private Double maxApr;

    @Column(name = "Min_May")
    private Double minMay;
    @Column(name = "Max_May")
    private Double maxMay;

    @Column(name = "Min_Jun")
    private Double minJun;
    @Column(name = "Max_Jun")
    private Double maxJun;

    @Column(name = "Min_Jul")
    private Double minJul;
    @Column(name = "Max_Jul")
    private Double maxJul;

    @Column(name = "Min_Aug")
    private Double minAug;
    @Column(name = "Max_Aug")
    private Double maxAug;

    @Column(name = "Min_Sep")
    private Double minSep;
    @Column(name = "Max_Sep")
    private Double maxSep;

    @Column(name = "Min_Oct")
    private Double minOct;
    @Column(name = "Max_Oct")
    private Double maxOct;

    @Column(name = "Min_Nov")
    private Double minNov;
    @Column(name = "Max_Nov")
    private Double maxNov;

    @Column(name = "Min_Dec")
    private Double minDec;
    @Column(name = "Max_Dec")
    private Double maxDec;

    @Column(name = "Min_Jan")
    private Double minJan;
    @Column(name = "Max_Jan")
    private Double maxJan;

    @Column(name = "Min_Feb")
    private Double minFeb;
    @Column(name = "Max_Feb")
    private Double maxFeb;

    @Column(name = "Min_Mar")
    private Double minMar;
    @Column(name = "Max_Mar")
    private Double maxMar;

    @Column(name = "Remarks")
    private String remarks;

    @Column(name = "CreatedDate")
    private LocalDateTime createdDate;

    @Column(name = "UpdatedDate")
    private LocalDateTime updatedDate;
}
