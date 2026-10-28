package com.wks.caseengine.cpp.dto;

import java.util.UUID;

import lombok.Data;

/**
 * Response DTO for the JMD Intersite Steam Transfer GET/POST APIs.
 *
 * <p>Each fiscal-year month (Apr → Mar) is exposed as flat {@code minXxx}/{@code maxXxx}
 * fields, matching the SQL column names from
 * {@code dbo.CPP_IntersiteSteamTransfer}.</p>
 */
@Data
public class IntersiteSteamTransferDto {

    private UUID id;

    // ── Static columns ──────────────────────────────────────────────
    private String cppPlantName;
    private String cppPlantCode;
    private String normParameterName;
    private String sapMaterialCode;
    private String uom;

    private String senderPlantName;
    private String senderPlantCode;
    private String senderCostCenterName;
    private String senderCostCenterCode;

    private String receiverPlantName;
    private String receiverPlantCode;
    private String receiverCostCenterName;
    private String receiverCostCenterCode;

    private String aopYear;

    // ── Min/Max month fields (Apr → Mar) ────────────────────────────
    private Double minApr;
    private Double maxApr;
    private Double minMay;
    private Double maxMay;
    private Double minJun;
    private Double maxJun;
    private Double minJul;
    private Double maxJul;
    private Double minAug;
    private Double maxAug;
    private Double minSep;
    private Double maxSep;
    private Double minOct;
    private Double maxOct;
    private Double minNov;
    private Double maxNov;
    private Double minDec;
    private Double maxDec;
    private Double minJan;
    private Double maxJan;
    private Double minFeb;
    private Double maxFeb;
    private Double minMar;
    private Double maxMar;

    private String remarks;

    // ── FK IDs (hidden, used for save/update) ───────────────────────
    private UUID cppPlantFkId;
    private UUID normParameterFkId;
    private UUID senderPlantFkId;
    private UUID senderCostCenterFkId;
    private UUID receiverPlantFkId;
    private UUID receiverCostCenterFkId;

    // ── Import/export tracking ──────────────────────────────────────
    private String saveStatus;
    private String errDescription;
    private String rowHash;
}
