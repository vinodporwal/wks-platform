package com.wks.caseengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class OtherFurnanceDetailsDTO {
    
    private String id;
    private String displayName;
    private String attributeValue;
    private String remarks;
}
