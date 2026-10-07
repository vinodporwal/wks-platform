package com.wks.caseengine.vgoht.dto;

import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Configuration
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ShutDownCatChemDTO {

    private String normParameterFKId;
    private String displayName;
    private String uom;
    private String sapMaterialCode;
    private Double apr;
    private Double may;
    private Double jun;
    private Double jul;
    private Double aug;
    private Double sep;
    private Double oct;
    private Double nov;
    private Double dec;
    private Double jan;
    private Double feb;
    private Double mar;
    private String remarks;
    private String type;
    private Integer displayOrder;
    private Boolean isEditable;
    private String saveStatus;
    private String errDescription;
}
