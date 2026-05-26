package com.wks.caseengine.cpp.dto;

import java.util.UUID;

public interface CPPAssetOperationalHoursProjection {

    UUID getId();
    UUID getAssetFkId();
    String getUtilityDistributed();
    String getDistributedSapCode();
    String getUtilityGenerated();
    String getGeneratedUtilityCode();

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

    String getAopYear();
    String getRemarks();

    UUID getSiteFkId();
    UUID getVerticalFkId();
    UUID getPlantFkId();

    String getCreatedDate();
    String getModifiedDate();

    String getAssetName();
    String getAssetType();
    String getPlantName();
}
