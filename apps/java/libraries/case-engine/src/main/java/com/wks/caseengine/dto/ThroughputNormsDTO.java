package com.wks.caseengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ThroughputNormsDTO {

    private String id;
    private String unit;
    private String unitId;
    private String materialCode;
    private String materialCodeDecription;
    private String displayName;
    private String uom;

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
    private String errorMessage;
}
