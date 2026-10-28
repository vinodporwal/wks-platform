package com.wks.caseengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class NormsMaterialDropdownDTO {

    private String unitId;
    private String materialId;
    private String unit;
    private String displayName;
    private String uom;
}
