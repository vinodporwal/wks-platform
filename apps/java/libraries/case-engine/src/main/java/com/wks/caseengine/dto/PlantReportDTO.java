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
public class PlantReportDTO {
    
    private UUID id;
    private String kpiName;
    private String uom;
    private Double bestAchieved;
    private Double prevAOP;
    private Double prevActual;
    private Double currentPlan;
    private String remark;
    private String aopYear;
    private UUID plantFkId;
    private Date createdOn;
    private Date modifiedOn;
    private String updatedBy;
    private boolean isEditable;
    private boolean isVisible;
    private Integer displayOrder;
}
