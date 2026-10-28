package com.wks.caseengine.cpp.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Spring Data projection for CPPProcessUnitAllocation GET query.
 * Maps columns returned by stored procedure dbo.CPP_GetProcessUnitAllocation.
 */
public interface CPPProcessUnitAllocationProjection {

    UUID getId();
    UUID getCppPlantFkId();
    UUID getImportPowerFkId();
    UUID getNormParameterFkId();
    String getProcessPlantName();
    String getProcessPlantCode();

    // Display labels resolved via SP JOINs ────────────────────────────────────
    String getProcurementPlant();
    String getPlantName();
    String getUtility();
    String getMaterial();
    String getMaterialDisplayName();
    String getUom();
    String getProcessUnit();

    String getAopYear();
    String getRemarks();
    String getCreatedDate();
    String getUpdatedDate();

    // Source monthly values (from CPPImportPower) ─────────────────────────────
    BigDecimal getSourceApr();
    BigDecimal getSourceMay();
    BigDecimal getSourceJun();
    BigDecimal getSourceJul();
    BigDecimal getSourceAug();
    BigDecimal getSourceSep();
    BigDecimal getSourceOct();
    BigDecimal getSourceNov();
    BigDecimal getSourceDec();
    BigDecimal getSourceJan();
    BigDecimal getSourceFeb();
    BigDecimal getSourceMar();

    // Allocation monthly values ───────────────────────────────────────────────
    BigDecimal getApr();
    BigDecimal getMay();
    BigDecimal getJun();
    BigDecimal getJul();
    BigDecimal getAug();
    BigDecimal getSep();
    BigDecimal getOct();
    BigDecimal getNov();
    BigDecimal getDec();
    BigDecimal getJan();
    BigDecimal getFeb();
    BigDecimal getMar();

    // Balance (source − allocated) ────────────────────────────────────────────
    BigDecimal getBalanceApr();
    BigDecimal getBalanceMay();
    BigDecimal getBalanceJun();
    BigDecimal getBalanceJul();
    BigDecimal getBalanceAug();
    BigDecimal getBalanceSep();
    BigDecimal getBalanceOct();
    BigDecimal getBalanceNov();
    BigDecimal getBalanceDec();
    BigDecimal getBalanceJan();
    BigDecimal getBalanceFeb();
    BigDecimal getBalanceMar();
}
