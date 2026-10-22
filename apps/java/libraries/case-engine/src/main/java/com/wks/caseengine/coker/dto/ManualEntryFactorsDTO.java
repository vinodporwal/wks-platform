package com.wks.caseengine.coker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualEntryFactorsDTO {
    private String normParameterFKId;
    private String displayName;
    private String apr;
    private String may;
    private String jun;
    private String remarks;
    private String uom;
    private String normParameterTypeName;
    private String type;
    private Integer displayOrder;
    private Boolean isEditable;
}
