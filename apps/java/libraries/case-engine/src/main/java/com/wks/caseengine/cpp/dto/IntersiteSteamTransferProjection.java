package com.wks.caseengine.cpp.dto;

import java.util.UUID;

/**
 * Projection interface for the native SQL query that reads from
 * {@code dbo.CPP_IntersiteSteamTransfer}.
 *
 * <p>Getter names must match the column aliases in the SQL query
 * (Spring Data JPA maps them by name).</p>
 */
public interface IntersiteSteamTransferProjection {

    UUID getId();

    String getCppPlantName();
    String getCppPlantCode();

    String getNormParameterName();
    String getSapMaterialCode();
    String getUom();

    String getSenderPlantName();
    String getSenderPlantCode();

    String getSenderCostCenterName();
    String getSenderCostCenterCode();

    String getReceiverPlantName();
    String getReceiverPlantCode();

    String getReceiverCostCenterName();
    String getReceiverCostCenterCode();

    String getAopYear();

    Double getMinApr();
    Double getMaxApr();
    Double getMinMay();
    Double getMaxMay();
    Double getMinJun();
    Double getMaxJun();
    Double getMinJul();
    Double getMaxJul();
    Double getMinAug();
    Double getMaxAug();
    Double getMinSep();
    Double getMaxSep();
    Double getMinOct();
    Double getMaxOct();
    Double getMinNov();
    Double getMaxNov();
    Double getMinDec();
    Double getMaxDec();
    Double getMinJan();
    Double getMaxJan();
    Double getMinFeb();
    Double getMaxFeb();
    Double getMinMar();
    Double getMaxMar();

    String getRemarks();

    UUID getCppPlantFkId();
    UUID getNormParameterFkId();
    UUID getSenderPlantFkId();
    UUID getSenderCostCenterFkId();
    UUID getReceiverPlantFkId();
    UUID getReceiverCostCenterFkId();
}
