package com.wks.caseengine.cpp.service;

import com.wks.caseengine.message.vm.AOPMessageVM;

import java.util.List;
import java.util.UUID;

public interface JMDAssetsService {

    AOPMessageVM getOperationalHoursForPlants(
            List<UUID> plantIds,
            String financialYear);
}
