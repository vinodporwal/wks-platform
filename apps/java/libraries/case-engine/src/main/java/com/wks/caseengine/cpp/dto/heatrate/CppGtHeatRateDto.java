package com.wks.caseengine.cpp.dto.heatrate;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Data
@NoArgsConstructor
public class CppGtHeatRateDto {

    private String id;
    private String assetFkId;
    private String equipType;
    private String cppUtility;
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
    private Double proposedYearFinalHeatRate;
    private String saveStatus;
    private String errDescription;
}
