package com.wks.caseengine.cpp.dto.heatrate;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Data
@NoArgsConstructor
public class CppGtHeatRateDto {

    private UUID id;
    private UUID assetFkId;
    private String assetName;
    private String utilityId;
    private String financialYear;
    private Double gtLoad;
    private Double freeSteamFactor;
    private String remarks;
    private Date createdDate;
    private Date updatedDate;
    private Double finalHeatRate;
    private Double oemHeatRate;
    private String selectedHeatRate;
    private Double prevYearFinalHeatRate;
}
