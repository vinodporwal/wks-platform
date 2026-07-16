package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.ShutdownSummaryLastFourYearDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface ShutdownSummaryLastFourYearService {
    AOPMessageVM getShutdownSummaryLastFourYear(String plantId, String year);
    AOPMessageVM deleteShutdownSummaryLastFourYear(String id);
    AOPMessageVM updateShutdownSummaryLastFourYear(String plantId,String year,List<ShutdownSummaryLastFourYearDTO> shutdownSummaryLastFourYearDTOs);
}

