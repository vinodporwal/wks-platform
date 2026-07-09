package com.wks.caseengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ChemGradeDTO {
    
    private String particulars;
    private Double l1K67;
    private Double l2K67;
    private Double l2K67F;
    private Double l2K57;
    private String l1K67Id;
    private String l2K67Id;
    private String l2K67FId;
    private String l2K57Id;
    private Boolean isEditable;
}
