package com.wks.caseengine.cpp.dto;

import java.util.UUID;

public interface CPPAssetCapacityProjection {

    UUID getId();
    UUID getAssetFkId();
    UUID getPlantFkId();

    Double getFixedMin();
    Double getFixedMax();

    Double getAprMin();
    Double getAprMax();
    Double getMayMin();
    Double getMayMax();
    Double getJunMin();
    Double getJunMax();
    Double getJulMin();
    Double getJulMax();
    Double getAugMin();
    Double getAugMax();
    Double getSepMin();
    Double getSepMax();
    Double getOctMin();
    Double getOctMax();
    Double getNovMin();
    Double getNovMax();
    Double getDecMin();
    Double getDecMax();
    Double getJanMin();
    Double getJanMax();
    Double getFebMin();
    Double getFebMax();
    Double getMarMin();
    Double getMarMax();

    String getAopYear();
    String getRemarks();
    String getUom();

    String getCreatedDate();
    String getModifiedDate();

    String getAssetName();
    String getAssetType();
    String getPlantName();
    String getAssetCategory();

    UUID getSiteFkId();
    UUID getVerticalFkId();
}
