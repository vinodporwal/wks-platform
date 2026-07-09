package com.wks.caseengine.cpp.dto.heatrate;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CppAuxBoilerHeatRateDto {

    private String id;
    private String assetFkId;
    private String equipType;
    private String cppUtility;
    private String financialYear;
    private Double auxBoilerLoad;
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
