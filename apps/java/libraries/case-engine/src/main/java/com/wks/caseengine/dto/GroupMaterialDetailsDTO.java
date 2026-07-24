package com.wks.caseengine.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMaterialDetailsDTO {

    private String id;
    private String name;
    private String sapMaterialCode;
    private String groupName;
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
    private String normParameterFKId;
    private String plantFKId;
    private Integer displayOrder;
}
