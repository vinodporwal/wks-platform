package com.wks.caseengine.service;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface ShutdownSummaryLastFourYearService {
    AOPMessageVM getShutdownSummaryLastFourYear(String plantId, String year);
}

