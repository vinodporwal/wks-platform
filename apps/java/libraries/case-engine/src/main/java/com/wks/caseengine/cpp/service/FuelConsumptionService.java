package com.wks.caseengine.cpp.service;

import java.io.IOException;
import java.util.UUID;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface FuelConsumptionService {

    AOPMessageVM getFuelConsumption(UUID siteId, String financialYear);

    byte[] exportFuelConsumption(UUID siteId, String financialYear) throws IOException;
}
