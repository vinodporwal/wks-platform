package com.wks.caseengine.cpp.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO for the Average Asset Loading output grid.
 *
 * <p>Mapped from the result set of stored procedure {@code dbo.CPP_GetAverageAssetLoading},
 * whose columns are read by name:
 * <pre>
 *   cppPlantId, cppPlantName, generatingPlantName,
 *   assetId, assetName, assetType, assetCategory,
 *   utilityName, uom, materialName, issuingPlantName, issuingUom,
 *   {Mon}_GenQty, {Mon}_OpHours, {Mon}_AvgLoading, {Mon}_HeatRate  (for each of 12 months Apr→Mar)
 * </pre>
 */
@Data
public class AverageAssetLoadingDTO {

    @JsonProperty("cppPlantId")
    private UUID cppPlantId;

    private String cppPlantName;

    private String generatingPlantName;

    @JsonProperty("assetId")
    private UUID assetId;

    private String assetName;

    private String assetType;

    private String assetCategory;

    private String utilityName;

    private String uom;

    private String materialName;

    private String issuingPlantName;

    private String issuingUom;

    // ── April ──────────────────────────────────────────────────────────────────
    private BigDecimal aprGenQty;
    private BigDecimal aprOpHours;
    private BigDecimal aprAvgLoading;
    private BigDecimal aprHeatRate;

    // ── May ────────────────────────────────────────────────────────────────────
    private BigDecimal mayGenQty;
    private BigDecimal mayOpHours;
    private BigDecimal mayAvgLoading;
    private BigDecimal mayHeatRate;

    // ── June ───────────────────────────────────────────────────────────────────
    private BigDecimal junGenQty;
    private BigDecimal junOpHours;
    private BigDecimal junAvgLoading;
    private BigDecimal junHeatRate;

    // ── July ───────────────────────────────────────────────────────────────────
    private BigDecimal julGenQty;
    private BigDecimal julOpHours;
    private BigDecimal julAvgLoading;
    private BigDecimal julHeatRate;

    // ── August ─────────────────────────────────────────────────────────────────
    private BigDecimal augGenQty;
    private BigDecimal augOpHours;
    private BigDecimal augAvgLoading;
    private BigDecimal augHeatRate;

    // ── September ──────────────────────────────────────────────────────────────
    private BigDecimal sepGenQty;
    private BigDecimal sepOpHours;
    private BigDecimal sepAvgLoading;
    private BigDecimal sepHeatRate;

    // ── October ────────────────────────────────────────────────────────────────
    private BigDecimal octGenQty;
    private BigDecimal octOpHours;
    private BigDecimal octAvgLoading;
    private BigDecimal octHeatRate;

    // ── November ───────────────────────────────────────────────────────────────
    private BigDecimal novGenQty;
    private BigDecimal novOpHours;
    private BigDecimal novAvgLoading;
    private BigDecimal novHeatRate;

    // ── December ───────────────────────────────────────────────────────────────
    private BigDecimal decGenQty;
    private BigDecimal decOpHours;
    private BigDecimal decAvgLoading;
    private BigDecimal decHeatRate;

    // ── January ────────────────────────────────────────────────────────────────
    private BigDecimal janGenQty;
    private BigDecimal janOpHours;
    private BigDecimal janAvgLoading;
    private BigDecimal janHeatRate;

    // ── February ───────────────────────────────────────────────────────────────
    private BigDecimal febGenQty;
    private BigDecimal febOpHours;
    private BigDecimal febAvgLoading;
    private BigDecimal febHeatRate;

    // ── March ──────────────────────────────────────────────────────────────────
    private BigDecimal marGenQty;
    private BigDecimal marOpHours;
    private BigDecimal marAvgLoading;
    private BigDecimal marHeatRate;
}
