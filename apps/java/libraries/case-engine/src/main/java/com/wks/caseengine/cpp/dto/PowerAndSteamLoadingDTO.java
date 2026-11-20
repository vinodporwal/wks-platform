package com.wks.caseengine.cpp.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO for the Power and Asset Loading output grid.
 *
 * <p>Mapped from the result set of stored procedure {@code dbo.CPP_GetPowerAndSteamLoading},
 * whose columns are read by name:
 * <pre>
 *   cppPlantId, cppPlantName, generatingPlantName, generatingPlantCode,
 *   materialCategory, loadingUOM, utilityName, utilityCode, uom, materialName, materialCode,
 *   issuingPlantName, issuingPlantCode, issuingUom,
 *   {Mon}_GenQty, {Mon}_OpHours, {Mon}_Loading, {Mon}_AvgLoading  (for each of 12 months Apr→Mar)
 * </pre>
 */
@Data
public class PowerAndSteamLoadingDTO {

    @JsonProperty("cppPlantId")
    private UUID cppPlantId;

    private String cppPlantName;

    private String generatingPlantName;

    private String generatingPlantCode;

    private String materialCategory;

    private String loadingUOM;

    private String utilityName;

    private String utilityCode;

    private String uom;

    private String materialName;

    private String materialCode;

    private String issuingPlantName;

    private String issuingPlantCode;

    private String issuingUom;

    // ── April ──────────────────────────────────────────────────────────────────
    private BigDecimal aprGenQty;
    private BigDecimal aprOpHours;
    private BigDecimal aprLoading;
    private BigDecimal aprAvgLoading;

    // ── May ────────────────────────────────────────────────────────────────────
    private BigDecimal mayGenQty;
    private BigDecimal mayOpHours;
    private BigDecimal mayLoading;
    private BigDecimal mayAvgLoading;

    // ── June ───────────────────────────────────────────────────────────────────
    private BigDecimal junGenQty;
    private BigDecimal junOpHours;
    private BigDecimal junLoading;
    private BigDecimal junAvgLoading;

    // ── July ───────────────────────────────────────────────────────────────────
    private BigDecimal julGenQty;
    private BigDecimal julOpHours;
    private BigDecimal julLoading;
    private BigDecimal julAvgLoading;

    // ── August ─────────────────────────────────────────────────────────────────
    private BigDecimal augGenQty;
    private BigDecimal augOpHours;
    private BigDecimal augLoading;
    private BigDecimal augAvgLoading;

    // ── September ──────────────────────────────────────────────────────────────
    private BigDecimal sepGenQty;
    private BigDecimal sepOpHours;
    private BigDecimal sepLoading;
    private BigDecimal sepAvgLoading;

    // ── October ────────────────────────────────────────────────────────────────
    private BigDecimal octGenQty;
    private BigDecimal octOpHours;
    private BigDecimal octLoading;
    private BigDecimal octAvgLoading;

    // ── November ───────────────────────────────────────────────────────────────
    private BigDecimal novGenQty;
    private BigDecimal novOpHours;
    private BigDecimal novLoading;
    private BigDecimal novAvgLoading;

    // ── December ───────────────────────────────────────────────────────────────
    private BigDecimal decGenQty;
    private BigDecimal decOpHours;
    private BigDecimal decLoading;
    private BigDecimal decAvgLoading;

    // ── January ────────────────────────────────────────────────────────────────
    private BigDecimal janGenQty;
    private BigDecimal janOpHours;
    private BigDecimal janLoading;
    private BigDecimal janAvgLoading;

    // ── February ───────────────────────────────────────────────────────────────
    private BigDecimal febGenQty;
    private BigDecimal febOpHours;
    private BigDecimal febLoading;
    private BigDecimal febAvgLoading;

    // ── March ──────────────────────────────────────────────────────────────────
    private BigDecimal marGenQty;
    private BigDecimal marOpHours;
    private BigDecimal marLoading;
    private BigDecimal marAvgLoading;
}
