package com.wks.caseengine.cpp.dto;

import java.util.UUID;

/**
 * Projection interface for the native SQL query that reads from
 * {@code dbo.CPPInterSitePowerTransfer} with JOINs to {@code dbo.Plants}
 * for From/To plant names.
 *
 * <p>Getter names must match the column aliases in the SQL query
 * (Spring Data JPA maps them by name).</p>
 */
public interface InterSitePowerTransferProjection {

    UUID getId();

    UUID getFromPlantId();
    String getFromPlantName();

    UUID getToPlantId();
    String getToPlantName();

    String getUom();
    String getFinancialYear();

    Double getApr();
    Double getMay();
    Double getJun();
    Double getJul();
    Double getAug();
    Double getSep();
    Double getOct();
    Double getNov();
    Double getDec();
    Double getJan();
    Double getFeb();
    Double getMar();

    String getRemarks();
}
