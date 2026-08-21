package com.wks.caseengine.cpp.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface JMDOutputHeatRateService {

    AOPMessageVM getHeatRateSummary(List<UUID> plantIds, String financialYear);

    byte[] exportHeatRateSummary(List<UUID> plantIds, String financialYear) throws IOException;
}
