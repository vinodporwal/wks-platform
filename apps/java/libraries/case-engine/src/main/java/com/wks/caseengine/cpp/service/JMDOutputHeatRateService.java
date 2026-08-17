package com.wks.caseengine.cpp.service;

import java.io.IOException;
import java.util.UUID;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface JMDOutputHeatRateService {

    AOPMessageVM getHeatRateSummary(UUID siteId, String financialYear);

    byte[] exportHeatRateSummary(UUID siteId, String financialYear) throws IOException;
}
