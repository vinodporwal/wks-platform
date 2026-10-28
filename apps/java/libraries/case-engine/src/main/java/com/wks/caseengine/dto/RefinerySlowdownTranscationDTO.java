package com.wks.caseengine.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class RefinerySlowdownTranscationDTO {

    private String id;
    private String siteFkId;
    private String siteName;
    private String plantFkId;
    private String plantName;
    private Integer tentativeDurationDays;
    private Double throughputDuringTheSlowdown;
    private String throughputUom;
    private Integer tentativeMonth;
    private String remark;
    private String plantId;
    private String aopYear;
    private String modifiedBy;
    private Date modifiedOn;
    private Boolean isEditable;
    private Boolean isVisible;

    private String saveStatus;
    private String errorMessage;
}
