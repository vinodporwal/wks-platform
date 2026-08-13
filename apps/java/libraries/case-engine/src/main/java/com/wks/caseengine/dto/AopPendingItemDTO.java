package com.wks.caseengine.dto;

import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row in a user's "My Approvals" inbox — an open AOP task pending that user's
 * action for a specific assignee role. A plant at Gate 2 can yield multiple rows
 * when the caller holds several Gate 2 roles. Carries the taskId needed to act
 * and denormalised plant/site/vertical names for display.
 */
@Configuration
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class AopPendingItemDTO {
    private String caseId;
    private String plantId;
    private String plantName;
    private String siteName;
    private String verticalName;
    private String year;

    private String gateName;
    private String gateDisplayName;
    private Integer sequence;

    private String assignedRole;
    private String taskId;

    private AopViewerDTO actions;
}
