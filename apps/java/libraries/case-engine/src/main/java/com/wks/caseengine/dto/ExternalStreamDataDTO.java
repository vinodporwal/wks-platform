package com.wks.caseengine.dto;

import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for CRACKER_VMD_GetExternalStreamData stored procedure result row.
 */
@Configuration
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ExternalStreamDataDTO {
    private String verticalId;
    private String plantId;
    private String normParameterId;
    private String particulars;
    private String normParameterTypeDisplayName;
    private String normParameterTypeFkId;
    private String type;
    private String uom;
    private String auditYear;
    private String remarks;

    private Double jan;
    private Double feb;
    private Double mar;
    private Double apr;
    private Double may;
    private Double jun;
    private Double jul;
    private Double aug;
    private Double sep;
    private Double oct;
    private Double nov;
    private Double dec;

    private Integer isEditable;
    private Integer normParameterDisplayOrder;
    private String name;
    private Integer rno;
}

