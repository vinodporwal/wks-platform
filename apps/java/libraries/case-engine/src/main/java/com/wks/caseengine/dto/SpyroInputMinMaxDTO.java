package com.wks.caseengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class SpyroInputMinMaxDTO {

    private String displayName;
    private String uom;
    private String idMin;
    private String idMax;
    private String aprMin;
    private String aprMax;
    private String mayMin;
    private String mayMax;
    private String junMin;
    private String junMax;
    private String julMin;
    private String julMax;
    private String augMin;
    private String augMax;
    private String sepMin;
    private String sepMax;
    private String octMin;
    private String octMax;
    private String novMin;
    private String novMax;
    private String decMin;
    private String decMax;
    private String janMin;
    private String janMax;
    private String febMin;
    private String febMax;
    private String marMin;
    private String marMax;
    private String minWeightAverage;
    private String maxWeightAverage;

    private String saveStatus;
    private String errDescription;
}
