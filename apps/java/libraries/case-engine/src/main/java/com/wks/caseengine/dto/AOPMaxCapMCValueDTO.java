package com.wks.caseengine.dto;

import java.util.Date;

import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for vwPTA_HMD_AOPMAXCAPMCValues view row.
 */
@Configuration
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class AOPMaxCapMCValueDTO {
    private String id;
    private String verticalName;
    private String siteName;
    private String plantName;
    private String productName;
    private String siteFKId;
    private String plantFKId;
    private String verticalFKId;
    private String materialFKId;
    private String monthName;
    private Double monthValue;
    private String financialYear;
    private String remarks;
    private Date createdOn;
    private Date modifiedOn;
    private String mcuVersion;
    private String updatedBy;
    private Integer normParameterDisplayOrder;
    private Integer isValid;
}
