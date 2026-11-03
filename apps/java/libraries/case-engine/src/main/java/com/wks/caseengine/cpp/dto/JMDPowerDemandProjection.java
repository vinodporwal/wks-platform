package com.wks.caseengine.cpp.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Projection interface for the native SP {@code dbo.CPP_GetJMDPowerDemand}.
 *
 * <p>Getter names must match the column aliases returned by the stored procedure
 * (Spring Data JPA maps them by name).</p>
 */
public interface JMDPowerDemandProjection {

    UUID getId();

    UUID getCppPlantId();
    String getCppPlantName();

    UUID getNormParameterId();

    String getUtilityName();
    String getUtilityId();
    String getUtilityUOM();

    String getFinancialYear();
    String getType();

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
