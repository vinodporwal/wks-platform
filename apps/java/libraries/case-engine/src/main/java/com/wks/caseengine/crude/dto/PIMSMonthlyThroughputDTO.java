package com.wks.caseengine.crude.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PIMSMonthlyThroughputDTO {
    private UUID id;
    private UUID normParameterId;
    private String name;
    private String displayName;
    private String uom;
    private String normParameterType;
    private String displayOrder;
    private Boolean isEditable;
    private String config;
    private String remarks;
    private String type;
    private String apr;
    private String may;
    private String jun;
    private String jul;
    private String aug;
    private String sep;
    private String oct;
    private String nov;
    private String dec;
    private String jan;
    private String feb;
    private String mar;
    private String saveStatus;
    private String errDescription;
}
