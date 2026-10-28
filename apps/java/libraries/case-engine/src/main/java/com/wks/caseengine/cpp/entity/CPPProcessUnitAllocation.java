package com.wks.caseengine.cpp.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "CPPProcessUnitAllocation", schema = "dbo")
@Data
public class CPPProcessUnitAllocation {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "CPPPlant_FK_ID", nullable = false)
    private UUID cppPlantFkId;

    @Column(name = "ImportPower_FK_ID", nullable = false)
    private UUID importPowerFkId;

    @Column(name = "NormParameter_FK_Id")
    private UUID normParameterFkId;

    @Column(name = "Process_Plant_Name")
    private String processPlantName;

    @Column(name = "Process_Plant_Code")
    private String processPlantCode;

    @Column(name = "AOPYear", nullable = false)
    private String aopYear;

    // ── Monthly allocation values ──────────────────────────────────────────
    @Column(name = "Apr")   private BigDecimal apr;
    @Column(name = "May")   private BigDecimal may;
    @Column(name = "Jun")   private BigDecimal jun;
    @Column(name = "Jul")   private BigDecimal jul;
    @Column(name = "Aug")   private BigDecimal aug;
    @Column(name = "Sep")   private BigDecimal sep;
    @Column(name = "Oct")   private BigDecimal oct;
    @Column(name = "Nov")   private BigDecimal nov;
    @Column(name = "[Dec]") private BigDecimal dec;
    @Column(name = "Jan")   private BigDecimal jan;
    @Column(name = "Feb")   private BigDecimal feb;
    @Column(name = "Mar")   private BigDecimal mar;

    // ── Balance (source total − allocated) ────────────────────────────────
    @Column(name = "BalanceApr")  private BigDecimal balanceApr;
    @Column(name = "BalanceMay")  private BigDecimal balanceMay;
    @Column(name = "BalanceJun")  private BigDecimal balanceJun;
    @Column(name = "BalanceJul")  private BigDecimal balanceJul;
    @Column(name = "BalanceAug")  private BigDecimal balanceAug;
    @Column(name = "BalanceSep")  private BigDecimal balanceSep;
    @Column(name = "BalanceOct")  private BigDecimal balanceOct;
    @Column(name = "BalanceNov")  private BigDecimal balanceNov;
    @Column(name = "BalanceDec")  private BigDecimal balanceDec;
    @Column(name = "BalanceJan")  private BigDecimal balanceJan;
    @Column(name = "BalanceFeb")  private BigDecimal balanceFeb;
    @Column(name = "BalanceMar")  private BigDecimal balanceMar;

    @Column(name = "Remarks")
    private String remarks;

    @Column(name = "CreatedDate", updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "UpdatedDate")
    private LocalDateTime updatedDate;
}
