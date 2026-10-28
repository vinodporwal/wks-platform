package com.wks.caseengine.cpp.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CPPProcessUnitAllocationDTO {

    private UUID id;

    // ── FK fields ──────────────────────────────────────────────────────────
    private UUID cppPlantId;         // → CPPPlant_FK_ID
    private UUID sourceId;           // → ImportPower_FK_ID
    private UUID normParameterFkId;  // → NormParameter_FK_Id
    private String processPlantName;  // → ProcessPlantName
    private String processPlantCode;  // → ProcessPlantCode

    // ── Display fields (UI labels — resolved via SP JOINs on GET) ──────────
    private String procurementPlant;
    private String plantName;
    private String utility;
    private String material;
    private String materialDisplayName;
    private String uom;
    private String processUnit;

    private String aopYear;
    private String remarks;

    private String createdDate;
    private String updatedDate;

    // ── Source monthly values (from CPPImportPower via SP) ─────────────────
    private BigDecimal sourceApr;
    private BigDecimal sourceMay;
    private BigDecimal sourceJun;
    private BigDecimal sourceJul;
    private BigDecimal sourceAug;
    private BigDecimal sourceSep;
    private BigDecimal sourceOct;
    private BigDecimal sourceNov;
    private BigDecimal sourceDec;
    private BigDecimal sourceJan;
    private BigDecimal sourceFeb;
    private BigDecimal sourceMar;

    // ── Monthly allocation values ──────────────────────────────────────────
    private BigDecimal apr;
    private BigDecimal may;
    private BigDecimal jun;
    private BigDecimal jul;
    private BigDecimal aug;
    private BigDecimal sep;
    private BigDecimal oct;
    private BigDecimal nov;
    private BigDecimal dec;
    private BigDecimal jan;
    private BigDecimal feb;
    private BigDecimal mar;

    // ── Balance (source total − allocated) ────────────────────────────────
    private BigDecimal balanceApr;
    private BigDecimal balanceMay;
    private BigDecimal balanceJun;
    private BigDecimal balanceJul;
    private BigDecimal balanceAug;
    private BigDecimal balanceSep;
    private BigDecimal balanceOct;
    private BigDecimal balanceNov;
    private BigDecimal balanceDec;
    private BigDecimal balanceJan;
    private BigDecimal balanceFeb;
    private BigDecimal balanceMar;
}
