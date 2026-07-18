package com.wks.caseengine.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@Builder
@AllArgsConstructor
public class GroupedSelectionDTO {
    
    private UUID id;
    private String name;
    private String displayName;
    private String uom;
    private String value;
    private boolean status;  // Expression
    private UUID dependantAttributeId;
    private UUID normParameterTypeFkId;
    private UUID plantFkId;
    private boolean isEditable;
    private String sapMaterialCode;
    private String normParameterType;
    private String aopYear;
}
