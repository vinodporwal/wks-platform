package com.wks.caseengine.dto;

import java.util.List;

import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Server-computed button state for the AOP approval screen. The frontend renders
 * buttons purely from these flags — it never decides actionability itself.
 *
 * <p>{@code mode}: ACTION (an active task is assigned to one of the caller's
 * roles), EDIT (caller is a preparer at the prepare stage), or READ_ONLY.</p>
 */
@Configuration
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class AopViewerDTO {
    private String mode;
    private boolean canApprove;
    private boolean canRevert;
    private boolean canSubmit;
    private boolean canEdit;
    private boolean remarkMandatory;
    private List<String> roles;
}
