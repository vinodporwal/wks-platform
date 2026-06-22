package com.wks.caseengine.cpp.dto;

public interface PlantWiseFuelPriorityProjection {
    String getId();
    String getPlantName();
    String getFuelName();
    String getFuelDisplayName();
    String getFuelFkId();
    Integer getPriority();
    Integer getQuantity();
    String getRemarks();
    String getAopYear();
}

