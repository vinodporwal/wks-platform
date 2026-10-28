package com.wks.caseengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class GradeSelectionDTO {
    
    private String normParameterId;
    private String gradeId;
    private String materialName;
    private boolean isSelected;
    private String remarks;

}
