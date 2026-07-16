package com.wks.caseengine.dto;

import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for a gate decision (approve / revert). The acting user id is
 * taken from the JWT server-side, not from this body.
 */
@Configuration
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class AopActRequestDTO {
    private String taskId;
    private String plantId;
    private String year;
    private String gateName;
    /** APPROVED or REVERTED. */
    private String decision;
    private String remark;
    /** The gate role the caller is acting as (the task's assignee role). */
    private String actorRole;
}
