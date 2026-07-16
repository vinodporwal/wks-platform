package com.wks.caseengine.dto;

import java.util.List;

import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AOP approval status for a single (plant, year): the stepper, the current gate,
 * the task the caller may act on (if any), and the server-computed button state.
 */
@Configuration
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class AopWorkflowStatusDTO {
    private boolean exists;
    private String caseId;
    private String plantId;
    private String plantName;
    private String siteName;
    private String verticalName;
    private String year;

    private String currentGateName;
    private String currentGateDisplayName;
    private Integer currentSequence;

    /** Camunda task the caller can act on for this workflow (null if none). */
    private String taskId;
    /** Which of the caller's roles that task maps to. */
    private String assignedRole;

    private List<WorkflowStepsMasterDTO> steps;
    private AopViewerDTO viewer;
}
