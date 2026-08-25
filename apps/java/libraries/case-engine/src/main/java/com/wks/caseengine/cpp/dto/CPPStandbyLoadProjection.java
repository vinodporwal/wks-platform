package com.wks.caseengine.cpp.dto;

import java.util.UUID;

public interface CPPStandbyLoadProjection {

    UUID getId();
    UUID getAssetFkId();
    UUID getPlantFkId();

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
    String getUom();

    String getCreatedDate();
    String getModifiedDate();

    String getGeneratingPlant();
    String getUtilityDistributed();
    String getDistributedSapCode();
    String getUtilityGenerated();
    String getGeneratedUtilityCode();
    String getCppPlantName();
    String getPlantCode();
    String getType();

    UUID getSiteFkId();
    UUID getVerticalFkId();
}
