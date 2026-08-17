package com.wks.caseengine.cpp.dto;

public interface FuelWithCategoryProjection {
    String getId();
    String getFuelCode();
    String getFuelName();
    String getFuelDisplayName();
    String getType();
    String getUom();
    String getCategoryFkId();
    String getCategoryName();
    String getCategoryDisplayName();
}
