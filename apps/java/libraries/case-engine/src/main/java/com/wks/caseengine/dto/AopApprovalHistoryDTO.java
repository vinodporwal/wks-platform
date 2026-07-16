package com.wks.caseengine.dto;

import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Configuration
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class AopApprovalHistoryDTO {
    private String id;
    private String caseId;
    private String year;
    private String plantFkId;
    private String plantName;
    private String siteFkId;
    private String siteName;
    private String verticalFkId;
    private String verticalName;
    private String gateName;
    private String gateDisplayName;
    private Integer sequence;
    private String action;
    private String actorUserId;
    private String actorRole;
    private String remark;
    private String fromGate;
    private String toGate;
    private String actionAt;
}
