package com.wks.caseengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ModeSelectionDTO {
    
    private String id;
    private String modeId;
    private String verticalId;
    private String siteId;
    private String plantId;
    private String aopYear;
    private String modeName;
    private String displayName;
    private Integer displayOrder;
    private String type;
    private Boolean isChecked;
}
