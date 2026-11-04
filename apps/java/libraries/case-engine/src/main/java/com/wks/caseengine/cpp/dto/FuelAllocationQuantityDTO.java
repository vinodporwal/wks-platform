package com.wks.caseengine.cpp.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO for the Fuel Allocation Quantity output grid.
 *
 * <p>Mapped from the result set of stored procedure {@code dbo.CPP_GetFuelAllocationQuantity},
 * whose columns are read by name:
 * <pre>
 *   CPPPlantId, CPPPlantName, GeneratingPlantId, GeneratingPlantName,
 *   NormsHeaderId, AssetId, MaterialName, IssuingPlantName, IssuingPlantId,
 *   IssuingUOM, NormParameterId, AccountName, FinancialYear,
 *   {Mon}_GenerationQuantity, {Mon}_AllocationQuantity  (for each of 12 months Apr→Mar)
 * </pre>
 */
@Data
public class FuelAllocationQuantityDTO {

    @JsonProperty("cppPlantId")
    private UUID cppPlantId;

    private String cppPlantName;

    @JsonProperty("generatingPlantId")
    private UUID generatingPlantId;

    private String generatingPlantName;

    @JsonProperty("normsHeaderId")
    private UUID normsHeaderId;

    @JsonProperty("assetId")
    private UUID assetId;

    private String assetName;

    private String materialName;

    private String issuingPlantName;

    @JsonProperty("issuingPlantId")
    private UUID issuingPlantId;

    private String issuingUOM;

    @JsonProperty("normParameterId")
    private UUID normParameterId;

    private String accountName;

    @JsonProperty("isFreeSteamFuel")
    private Boolean isFreeSteamFuel;

    private String financialYear;

    // ── April ──────────────────────────────────────────────────────────────────
    private BigDecimal aprGenerationQuantity;
    private BigDecimal aprAllocationQuantity;

    // ── May ────────────────────────────────────────────────────────────────────
    private BigDecimal mayGenerationQuantity;
    private BigDecimal mayAllocationQuantity;

    // ── June ───────────────────────────────────────────────────────────────────
    private BigDecimal junGenerationQuantity;
    private BigDecimal junAllocationQuantity;

    // ── July ───────────────────────────────────────────────────────────────────
    private BigDecimal julGenerationQuantity;
    private BigDecimal julAllocationQuantity;

    // ── August ─────────────────────────────────────────────────────────────────
    private BigDecimal augGenerationQuantity;
    private BigDecimal augAllocationQuantity;

    // ── September ──────────────────────────────────────────────────────────────
    private BigDecimal sepGenerationQuantity;
    private BigDecimal sepAllocationQuantity;

    // ── October ────────────────────────────────────────────────────────────────
    private BigDecimal octGenerationQuantity;
    private BigDecimal octAllocationQuantity;

    // ── November ───────────────────────────────────────────────────────────────
    private BigDecimal novGenerationQuantity;
    private BigDecimal novAllocationQuantity;

    // ── December ───────────────────────────────────────────────────────────────
    private BigDecimal decGenerationQuantity;
    private BigDecimal decAllocationQuantity;

    // ── January ────────────────────────────────────────────────────────────────
    private BigDecimal janGenerationQuantity;
    private BigDecimal janAllocationQuantity;

    // ── February ───────────────────────────────────────────────────────────────
    private BigDecimal febGenerationQuantity;
    private BigDecimal febAllocationQuantity;

    // ── March ──────────────────────────────────────────────────────────────────
    private BigDecimal marGenerationQuantity;
    private BigDecimal marAllocationQuantity;
}

