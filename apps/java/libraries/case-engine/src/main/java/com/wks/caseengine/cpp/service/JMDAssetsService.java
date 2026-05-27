package com.wks.caseengine.cpp.service;

import com.wks.caseengine.dto.JMDOperationalHoursRequestDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

import java.util.List;
import java.util.UUID;

public interface JMDAssetsService {

    AOPMessageVM getOperationalHoursForPlants(
            List<UUID> plantIds,
            String financialYear);

    AOPMessageVM saveOperationalHours(
            List<UUID> plantIds,
            String financialYear,
            JMDOperationalHoursRequestDTO payload);

    byte[] exportPowerOperationalHours(
            List<UUID> plantIds,
            String financialYear);

    byte[] exportSteamOperationalHours(
            List<UUID> plantIds,
            String financialYear);
}
