package com.wks.caseengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class FixedBedAndLabCostDTO {

    private String id;
    private String costCenterMasterId;
    private String materialMasterId;
    private String aopYear;

    private String costCenterDescription;
    private String material;
    private String uom;

    private Double apr;
    private Double may;
    private Double jun;
    private Double jul;
    private Double aug;
    private Double sep;
    private Double oct;
    private Double nov;
    private Double dec;
    private Double jan;
    private Double feb;
    private Double mar;

    private String remarks;

    private String saveStatus;
    private String errorMessage;
}
