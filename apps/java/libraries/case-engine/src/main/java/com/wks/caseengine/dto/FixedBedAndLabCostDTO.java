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
    private String masterId;
    private String aopYear;

    private String account;
    private String profitCenter;
    private String plant;
    private String unit;
    private String costCenter;
    private String costCenterDescription;
    private String material;
    private String uom;
    private String unitDescription;
    private String subDescription;
    private Double displayOrder;

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
    private Double totalAmount;

    private String remarks;
    private Boolean isEditable;
    private Boolean isActive;
    private Boolean isTransactionExists;

    private String saveStatus;
    private String errorMessage;
}
