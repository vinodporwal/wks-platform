package com.wks.caseengine;

import java.util.List;

import com.wks.caseengine.cases.instance.CaseInstance;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateEventIdsRequest {

    private List<String> businessKeys;
    private List<String> eventIds;

    private List<CaseInstance.EventUrlItem> eventTrendUrls;
    private List<CaseInstance.EventUrlItem> eventReportUrls;
    
}
