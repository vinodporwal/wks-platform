package com.wks.caseengine.cpp.service;

import java.io.IOException;
import java.util.UUID;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface CPPUtilityRateService {

    AOPMessageVM getCPPUtilityRates(UUID cppPlantId, String financialYear);

    byte[] exportCPPUtilityRates(UUID cppPlantId, String financialYear) throws IOException;
}
