package com.wks.caseengine.cpp.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO for the Fuel Consumption output report.
 *
 * <p>Mapped from the result set of stored procedure {@code dbo.CPP_GetFuelConsumption},
 * whose columns are read by name:
 * <pre>
 *   PlantName, PlantCode, PlantId, FuelDisplayName, Fuel_FK_Id, FinancialYear,
 *   Available_{Mon}, Allocated_{Mon}, Remaining_{Mon}  (for each of 12 months Jan→Dec)
 * </pre>
 */
@Data
public class FuelConsumptionDTO {

    @JsonProperty("plantName")
    private String plantName;

    @JsonProperty("plantCode")
    private String plantCode;

    @JsonProperty("plantId")
    private UUID plantId;

    @JsonProperty("fuelDisplayName")
    private String fuelDisplayName;

    @JsonProperty("fuelFkId")
    private UUID fuelFkId;

    private String financialYear;

    // ── January ────────────────────────────────────────────────────────────────
    private BigDecimal janAvailable;
    private BigDecimal janAllocated;
    private BigDecimal janRemaining;

    // ── February ───────────────────────────────────────────────────────────────
    private BigDecimal febAvailable;
    private BigDecimal febAllocated;
    private BigDecimal febRemaining;

    // ── March ──────────────────────────────────────────────────────────────────
    private BigDecimal marAvailable;
    private BigDecimal marAllocated;
    private BigDecimal marRemaining;

    // ── April ──────────────────────────────────────────────────────────────────
    private BigDecimal aprAvailable;
    private BigDecimal aprAllocated;
    private BigDecimal aprRemaining;

    // ── May ────────────────────────────────────────────────────────────────────
    private BigDecimal mayAvailable;
    private BigDecimal mayAllocated;
    private BigDecimal mayRemaining;

    // ── June ───────────────────────────────────────────────────────────────────
    private BigDecimal junAvailable;
    private BigDecimal junAllocated;
    private BigDecimal junRemaining;

    // ── July ───────────────────────────────────────────────────────────────────
    private BigDecimal julAvailable;
    private BigDecimal julAllocated;
    private BigDecimal julRemaining;

    // ── August ─────────────────────────────────────────────────────────────────
    private BigDecimal augAvailable;
    private BigDecimal augAllocated;
    private BigDecimal augRemaining;

    // ── September ──────────────────────────────────────────────────────────────
    private BigDecimal sepAvailable;
    private BigDecimal sepAllocated;
    private BigDecimal sepRemaining;

    // ── October ────────────────────────────────────────────────────────────────
    private BigDecimal octAvailable;
    private BigDecimal octAllocated;
    private BigDecimal octRemaining;

    // ── November ───────────────────────────────────────────────────────────────
    private BigDecimal novAvailable;
    private BigDecimal novAllocated;
    private BigDecimal novRemaining;

    // ── December ───────────────────────────────────────────────────────────────
    private BigDecimal decAvailable;
    private BigDecimal decAllocated;
    private BigDecimal decRemaining;
}
