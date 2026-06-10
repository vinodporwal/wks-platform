package com.wks.caseengine.cpp.service;

import com.wks.caseengine.dto.AssetCapacityRequestDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface JMDAssetCapacityService {

    AOPMessageVM getAssetCapacitiesForPlants(
            List<UUID> plantIds,
            String aopYear);

    AOPMessageVM saveAssetCapacities(
            List<UUID> plantIds,
            String aopYear,
            AssetCapacityRequestDTO payload);

    byte[] exportPowerAssetCapacity(
            List<UUID> plantIds,
            String aopYear);

    byte[] exportSteamAssetCapacity(
            List<UUID> plantIds,
            String aopYear);

    AOPMessageVM importPowerAssetCapacity(
            List<UUID> plantIds,
            String aopYear,
            MultipartFile file);

    AOPMessageVM importSteamAssetCapacity(
            List<UUID> plantIds,
            String aopYear,
            MultipartFile file);
}
