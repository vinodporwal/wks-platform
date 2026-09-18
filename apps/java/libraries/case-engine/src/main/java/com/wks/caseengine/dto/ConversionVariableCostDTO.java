package com.wks.caseengine.dto;

import java.util.Date;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversionVariableCostDTO {

    private UUID id;
    private String plantName;
    private String costType;
    private Double previousAop;
    private Double previousActual;
    private Double currentAop;
    private String remark;
    private UUID siteFkId;
    private String aopYear;
    private String modifiedBy;
    private Date modifiedOn;
    private boolean isEditable;
}
