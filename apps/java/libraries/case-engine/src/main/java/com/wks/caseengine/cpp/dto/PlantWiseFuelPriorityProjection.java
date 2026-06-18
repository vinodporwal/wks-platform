package com.wks.caseengine.cpp.dto;

public interface PlantWiseFuelPriorityProjection {
    String getId();
    String getPlantName();
    String getFuelName();
    String getFuelDisplayName();
    Integer getPriority();
    Integer getQuantity();
    String getRemarks();
    String getAopYear();
}
