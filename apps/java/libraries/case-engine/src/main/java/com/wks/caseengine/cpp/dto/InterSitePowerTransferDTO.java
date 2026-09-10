package com.wks.caseengine.cpp.dto;

import java.util.UUID;

import lombok.Data;

/**
 * DTO for the JMD Inter Site Power Transfer GET/POST APIs.
 *
 * <p>Each fiscal-year month (Apr → Mar) is exposed as a flat field,
 * matching the SQL column names from {@code dbo.CPPInterSitePowerTransfer}.</p>
 */
@Data
public class InterSitePowerTransferDTO {

    private UUID id;

    // ── Plant references ─────────────────────────────────────────────
    private UUID fromPlantId;
    private String fromPlantName;

    private UUID toPlantId;
    private String toPlantName;

    private String uom;
    private String financialYear;

    // ── Month fields (Apr → Mar) ─────────────────────────────────────
    private Double apr;
    private Double may;
    private Double jun;
    private Double jul;
    private Double aug;
    private Double sep;
    private Double oct;
    private Double nov;
    private Double dec;
    private Double jan;
    private Double feb;
    private Double mar;

    private String remarks;

    // ── Import/export tracking ────────────────────────────────────────
    private String saveStatus;
    private String errDescription;
    private String rowHash;
}
