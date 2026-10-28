package com.wks.caseengine.cpp.dto;

import java.math.BigDecimal;
import java.util.UUID;

public interface SpinningMarginProjection {
    UUID getId();
    String getCPPPlantName();
    String getUtilityName();
    String getUtilityCode();
    String getUom();
    String getAopYear();
    BigDecimal getApr();
    BigDecimal getMay();
    BigDecimal getJun();
    BigDecimal getJul();
    BigDecimal getAug();
    BigDecimal getSep();
    BigDecimal getOct();
    BigDecimal getNov();
    BigDecimal getDec();
    BigDecimal getJan();
    BigDecimal getFeb();
    BigDecimal getMar();
    String getRemarks();
}
