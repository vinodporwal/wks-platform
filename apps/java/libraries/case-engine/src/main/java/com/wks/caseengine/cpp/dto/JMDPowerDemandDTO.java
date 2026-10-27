package com.wks.caseengine.cpp.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO returned by the {@code GET /jmd/power-demand} endpoint.
 *
 * <p>Each fiscal-year month (Apr → Mar) is exposed as a flat {@code BigDecimal} field,
 * matching the {@code decimal(18,6)} SQL columns from {@code dbo.CPP_JMD_PowerDemand}.
 * Using {@code BigDecimal} prevents scientific-notation serialization in JSON responses.</p>
 */
@Data
public class JMDPowerDemandDTO {

    private UUID id;

    // ── Plant ─────────────────────────────────────────────────────────
    private UUID cppPlantId;
    private String cppPlantName;

    // ── Utility ───────────────────────────────────────────────────────
    private UUID normParameterId;
    private String utilityName;
    private String utilityId;
    private String utilityUOM;

    // ── Meta ──────────────────────────────────────────────────────────
    private String financialYear;
    private String type;

    // ── Month fields (Apr → Mar) ──────────────────────────────────────
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

    private String remarks;
}
