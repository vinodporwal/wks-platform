package com.wks.caseengine.cpp.dto;

import java.util.UUID;

public interface CPPAssetPriorityProjection {
    
    UUID getId();
    UUID getAssetFkId();
    UUID getPlantFkId();
    
    Integer getApr();
    Integer getMay();
    Integer getJun();
    Integer getJul();
    Integer getAug();
    Integer getSep();
    Integer getOct();
    Integer getNov();
    Integer getDec();
    Integer getJan();
    Integer getFeb();
    Integer getMar();
    
    String getRemarks();
    String getCreatedDate();
    String getModifiedDate();
    
    // Asset details
    String getAssetName();
    String getAssetType();
    
    // Plant details
    String getPlantName();
    
    // Asset category (Power/Steam)
    String getAssetCategory();
    
    // Additional IDs
    UUID getSiteFkId();
    UUID getVerticalFkId();
}
