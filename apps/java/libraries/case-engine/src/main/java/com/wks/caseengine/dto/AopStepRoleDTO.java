package com.wks.caseengine.dto;

import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One approver role at the current gate, with whether that role has already
 * approved ({@code true}) or is still pending ({@code false}).
 */
@Configuration
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class AopStepRoleDTO {
    private String role;
    /** {@code true} if this role has approved at the current step; {@code false} if still pending. */
    private boolean approved;
}
