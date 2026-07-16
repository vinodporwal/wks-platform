package com.wks.caseengine.dto;

import java.util.Date;

import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Configuration
public class ShutdownDetailsDTO {
    private String id;
    private String activities;

    // Planned shutdown fields
    private Date shutdownFrom;
    private Date shutdownTo;
    private Double durationHrs;
    private String remarks;

    // Routine shutdown monthwise fields
    private Double april;
    private Double may;
    private Double june;
    private Double july;
    private Double august;
    private Double september;
    private Double october;
    private Double november;
    private Double december;
    private Double january;
    private Double february;
    private Double march;

    // Previous years fields
    private Double prevYear1;
    private Double prevYear2;
    private Double prevYear3;
    private Double prevYear4;

    // Common audit fields
    private String year;
    private String plantFkId;
    private Date createdOn;
    private Date modifiedOn;
    private String updatedBy;
}

