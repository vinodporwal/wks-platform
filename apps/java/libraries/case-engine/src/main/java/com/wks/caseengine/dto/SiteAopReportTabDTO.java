package com.wks.caseengine.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteAopReportTabDTO {

    private UUID id;
    private String tabName;
    private String tabDisplayName;
    private Integer tabSequence;
    private Boolean isVisible;
}
