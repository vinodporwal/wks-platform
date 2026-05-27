package com.wks.caseengine.cpp.service;

import com.wks.caseengine.dto.AssetPriorityRequestDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

import java.util.List;
import java.util.UUID;

public interface JMDAssetPriorityService {

    AOPMessageVM getAssetPrioritiesForPlants(
            List<UUID> plantIds,
            String aopYear);

    AOPMessageVM saveAssetPriorities(
            List<UUID> plantIds,
            String aopYear,
            AssetPriorityRequestDTO payload);
}
