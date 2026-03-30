package com.wks.caseengine.dto;

import java.util.Date;

import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Sp_GetMCUCapacityUtilization stored procedure result row.
 */
@Configuration
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MCUCapacityUtilizationDTO {
    private String id;
    private String plant;
    private Double prevAop;
    private Double prevActual;
    private Double aop;
    private String remarks;
    private String aopYear;
    private String siteFkId;
    private String updatedBy;
    private Date updatedDateTime;
}
