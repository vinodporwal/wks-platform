package com.wks.caseengine.dto;

import java.util.List;

import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row in a user's "My Approvals" inbox — an AOP workflow identified by
 * (plantId, year), including in-flight ({@code status=pending}) and fully
 * approved ({@code status=completed}) processes.
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

    /**
     * Every approver role at {@code gateName}, with {@code approved} true after
     * that role has acted in the current visit and false while still pending.
     */
    private List<AopStepRoleDTO> listOfRoles;

    /** {@code pending} while the process is in flight; {@code completed} after Gate 5 approval. */
    private String status;

    private AopViewerDTO actions;
}
