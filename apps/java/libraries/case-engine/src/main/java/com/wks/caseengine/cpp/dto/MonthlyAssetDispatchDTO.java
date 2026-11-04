package com.wks.caseengine.cpp.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO for the Monthly Asset Dispatch output grid.
 *
 * <p>Each instance maps <b>one row</b> returned by
 * {@code dbo.CPP_GetMonthlyAssetDispatch}, which includes
 * {@code AOPYear} and {@code MonthNo} columns. Data is displayed
 * vertically — no pivot.
 *
 * <p>SP column → field mapping:
 * <pre>
 *   CPPPlant            → cppPlant
 *   [Plant Code]        → plantCode
 *   [Utility Plant]     → utilityPlant
 *   [Utility Plant Code]→ utilityPlantCode
 *   Utility             → utility
 *   [Utility Code]      → utilityCode
 *   Asset               → asset
 *   [Material Plant]    → materialPlant
 *   [Material Plant Code]→ materialPlantCode
 *   Material            → material
 *   [Material Code]     → materialCode
 *   Priority            → priority
 *   [Min Loading]       → minLoading
 *   [Max Loading]       → maxLoading
 *   LoadingUOM          → loadingUOM
 *   [Net Operating Hours]→ netOperatingHours
 *   [StandBy Load]      → standByLoad
 *   [StandBy Loading]   → standByLoading
 *   [Cumulative Load]   → cumulativeLoad
 *   [Heat Rate]         → heatRate
 *   HeatRateUOM         → heatRateUOM
 *   [Heat for Power]    → heatForPower
 *   [Mandatory to run]  → mandatoryToRun
 *   Loading             → loading
 *   Id                  → id
 *   CPPPlantId          → cppPlantId
 *   NormHeaderFkId      → normHeaderFkId
 *   AssetId             → assetId
 *   AssetMasterType     → assetMasterType
 *   AssetType           → assetType
 *   AOPYear             → aopYear
 *   MonthNo             → monthNo
 *   GenerationQty       → generationQty
 *   GenerationUOM       → generationUOM
 *   IsAvailable         → isAvailable
 *   IsDispatched        → isDispatched
 *   SourceType          → sourceType
 *   SourceDirection     → sourceDirection
 * </pre>
 */
@Data
public class MonthlyAssetDispatchDTO {

    /* ── Record identifiers ─────────────────────────────────────────────────── */

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("cppPlantId")
    private UUID cppPlantId;

    @JsonProperty("normHeaderFkId")
    private UUID normHeaderFkId;

    @JsonProperty("assetId")
    private UUID assetId;

    /* ── CPP Plant ──────────────────────────────────────────────────────────── */

    private String cppPlant;
    private String plantCode;

    /* ── Utility Plant ──────────────────────────────────────────────────────── */

    private String utilityPlant;
    private String utilityPlantCode;

    /* ── Utility (NormParameter) ────────────────────────────────────────────── */

    private String utility;
    private String utilityCode;

    /* ── Asset ──────────────────────────────────────────────────────────────── */

    private String asset;
    private String assetMasterType;
    private String assetType;

    /* ── Material Plant ─────────────────────────────────────────────────────── */

    private String materialPlant;
    private String materialPlantCode;

    /* ── Material (NormParameter) ───────────────────────────────────────────── */

    private String material;
    private String materialCode;

    /* ── Dispatch details ───────────────────────────────────────────────────── */

    private Integer priority;

    private BigDecimal minLoading;
    private BigDecimal maxLoading;
    private String loadingUOM;

    private BigDecimal netOperatingHours;

    private BigDecimal standByLoad;
    private BigDecimal standByLoading;
    private BigDecimal cumulativeLoad;

    private BigDecimal heatRate;
    private String heatRateUOM;

    private BigDecimal heatForPower;

    private Boolean mandatoryToRun;

    private BigDecimal loading;

    /* ── Financial year & month ─────────────────────────────────────────────── */

    private String aopYear;
    private Integer monthNo;
    private String month;

    /* ── Generation ─────────────────────────────────────────────────────────── */

    private BigDecimal generationQty;
    private String generationUOM;

    /* ── Status ─────────────────────────────────────────────────────────────── */

    private Boolean isAvailable;
    private Boolean isDispatched;

    /* ── Source ─────────────────────────────────────────────────────────────── */

    private String sourceType;
    private String sourceDirection;

    /* ── Audit ──────────────────────────────────────────────────────────────── */

    private String createdAt;
    private String updatedAt;
}
