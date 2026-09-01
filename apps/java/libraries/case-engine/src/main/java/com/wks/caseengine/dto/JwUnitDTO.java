package com.wks.caseengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class JwUnitDTO {

    private String id;
    private String normParameterFkId;
    private String siteFkId;
    private String aopYear;

    private String normParameterTypeName;
    private String normParameterTypeDisplayName;
    private String productName;
    private String displayName;
    private String uom;
    private Integer sequenceOrder;

    private Double april;
    private Double may;
    private Double june;
    private Double july;
    private Double aug;
    private Double sep;
    private Double oct;
    private Double nov;
    private Double dec;
    private Double jan;
    private Double feb;
    private Double march;

    private Double avgNorms;
    private String remarks;
    private Boolean isEditable;
    private Boolean isVisible;

    private String saveStatus;
    private String errorMessage;
}
