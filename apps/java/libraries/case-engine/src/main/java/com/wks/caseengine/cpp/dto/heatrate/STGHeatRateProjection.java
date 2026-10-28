package com.wks.caseengine.cpp.dto.heatrate;

import java.util.UUID;

public interface STGHeatRateProjection {
    UUID getId();
    String getEquipType();
    String getCPPUtility();
    Double getSTGLoad();
    Double getHeatRate();
    String getRemarks();
    Double getPreviousYearHeatRate();
    Double getFinalHeatRate();
    Double getOEMHeatRate();
    String getSelectedHeatRate();
}
