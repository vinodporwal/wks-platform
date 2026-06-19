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
public class ReliabilityImprovementDTO {
   

    private UUID id;
    private String initiativeDescription;
    private String outcome;
    private String recommendation;
    private Date targetDate;
    private String remark;
    private String aopYear;
    private UUID plantFkId;
    private Date createdOn;
    private Date modifiedOn;
    private String updatedBy;
    private boolean isEditable;
    private boolean isVisible;
    private int displayOrder;
}
