package com.wks.caseengine.cpp.dto.heatrate;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CppHrsgHeatRateDto {

    private String id;
    private String assetFkId;
    private String equipType;
    private String cppUtility;
    private Double hrsgLoad;
    private Double finalHeatRate;
    private Double oemHeatRate;
    private String selectedHeatRate;
    private String financialYear;
    private String remarks;
    private Date createdDate;
    private Date updatedDate;
    private Double prevYearFinalHeatRate;
    private Double proposedYearFinalHeatRate;
    private String saveStatus;
    private String errDescription;
}
