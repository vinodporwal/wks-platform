package com.wks.caseengine.cpp.service;

import com.wks.caseengine.cpp.dto.AssetPriorityRequestDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.springframework.web.multipart.MultipartFile;

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

    byte[] exportPowerAssetPriority(
            List<UUID> plantIds,
            String aopYear);

    byte[] exportSteamAssetPriority(
            List<UUID> plantIds,
            String aopYear);

    AOPMessageVM importPowerAssetPriority(
            List<UUID> plantIds,
            String aopYear,
            MultipartFile file);

    AOPMessageVM importSteamAssetPriority(
            List<UUID> plantIds,
            String aopYear,
            MultipartFile file);
}
