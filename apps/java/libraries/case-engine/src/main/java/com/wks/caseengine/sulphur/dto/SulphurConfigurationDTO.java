package com.wks.caseengine.sulphur.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SulphurConfigurationDTO {

    private String displayName;
    private String groupName;
    private String dataType;
    private Boolean isTickRow;
    private Boolean selected;
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
    private String uom;
    private String remarks;
    private String utilities;
    private Integer displayOrder;
    private String normParameterFKId;
}