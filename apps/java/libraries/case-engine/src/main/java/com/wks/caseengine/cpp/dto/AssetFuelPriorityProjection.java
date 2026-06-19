package com.wks.caseengine.cpp.dto;

public interface AssetFuelPriorityProjection {

    String getId();
    String getAssetId();
    String getAssetName();
    String getAssetType();
    String getCppPlantFkId();
    String getPlantName();
    // April
    String getAprPrimary();
    String getAprSecondary();
    String getAprTertiary();

    // May
    String getMayPrimary();
    String getMaySecondary();
    String getMayTertiary();

    // June
    String getJunPrimary();
    String getJunSecondary();
    String getJunTertiary();

    // July
    String getJulPrimary();
    String getJulSecondary();
    String getJulTertiary();

    // August
    String getAugPrimary();
    String getAugSecondary();
    String getAugTertiary();

    // September
    String getSepPrimary();
    String getSepSecondary();
    String getSepTertiary();

    // October
    String getOctPrimary();
    String getOctSecondary();
    String getOctTertiary();

    // November
    String getNovPrimary();
    String getNovSecondary();
    String getNovTertiary();

    // December
    String getDecPrimary();
    String getDecSecondary();
    String getDecTertiary();

    // January
    String getJanPrimary();
    String getJanSecondary();
    String getJanTertiary();

    // February
    String getFebPrimary();
    String getFebSecondary();
    String getFebTertiary();

    // March
    String getMarPrimary();
    String getMarSecondary();
    String getMarTertiary();

    String getRemarks();
    String getCreatedDate();
    String getModifiedDate();
}
