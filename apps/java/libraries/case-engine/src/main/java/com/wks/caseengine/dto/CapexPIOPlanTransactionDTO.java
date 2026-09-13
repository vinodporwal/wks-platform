package com.wks.caseengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;
import java.util.UUID;

import org.springframework.context.annotation.Configuration;

@Configuration
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class CapexPIOPlanTransactionDTO {


    private UUID id;
    private String proposal;
    private String category;
    private String justification;
    private Double costRsCr;
    private Double benefitRsCr;
    private Date targetPlan;
    private String statusPlan;
    private String remarks;
    private UUID siteId;
    private String aopYear;
    private String updatedBy;
    private Date updatedDate;
}
