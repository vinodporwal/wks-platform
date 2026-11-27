package com.wks.caseengine.pcg.dto;

import java.util.Date;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShutdownSNGTransactionDTO {
    
    private String id;
    private String name;
    private String description;
    private Double durationInHrs;
    private Date maintStartDateTime;
    private Date maintEndDateTime;
    private Integer durationInMins;
    private String maintForMonth;
    private String auditYear;
    private String remarks;
    private String createdOn;
    private String user;
    private String version;
    private String plantMaintenanceFKId;
    private String normParameterFKId;
    private String plantFKId;
    private String saveStatus;
    private String errDescription;
}
