package com.wks.caseengine.cpp.dto;

import java.util.UUID;

/**
 * Flat projection for Utility-category plants linked to a CPP plant
 * (Plants.SourceName = cppPlantId, BusinessCategoryName = 'Utility').
 */
public interface UtilityPlantProjection {

    UUID getId();

    String getName();
}
