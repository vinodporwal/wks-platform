package com.wks.caseengine.cpp.dto;

import java.util.UUID;

/**
 * Flat projection from the native SQL query that fetches import procurement plants
 * and their associated NormParameter sources for a given CPP plant.
 * One row per NormParameter per procurement plant.
 * Grouped into ImportPowerProcurementPlantDTO in the service layer.
 */
public interface ImportProcurementPlantProjection {

    UUID getProcurementPlantId();

    String getPlantName();

    UUID getCppPlantId();

    UUID getNormParameterId();

    String getNormName();

    String getNormDisplayName();

    String getSapCode();

    String getUom();
}
