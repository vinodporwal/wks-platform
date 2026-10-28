package com.wks.caseengine.cpp.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.wks.caseengine.cpp.dto.AssetFuelPriorityDto;
import com.wks.caseengine.cpp.dto.CompatibleFuelAssetDto;
import com.wks.caseengine.cpp.dto.FuelMasterWithCategoryDTO;
import com.wks.caseengine.cpp.dto.PlantFuelAvailabilityMonthlyDto;
import com.wks.caseengine.cpp.dto.PlantWiseFuelPriorityDto;
import com.wks.caseengine.cpp.entity.CPPAssetFuelPriority;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface FuelPriorityService {
    List<FuelMasterWithCategoryDTO> getFuelMaster();
    List<PlantWiseFuelPriorityDto> getPlantWiseFuelPriority(String plantIds, String financialYear);
    AOPMessageVM updatePlantFuelAvailability(List<PlantWiseFuelPriorityDto> payload);
    List<PlantFuelAvailabilityMonthlyDto> getPlantFuelAvailabilityMonthly(String plantIds, String financialYear);
    AOPMessageVM updatePlantFuelAvailabilityMonthly(List<PlantFuelAvailabilityMonthlyDto> payload);
    AOPMessageVM deletePlantFuelAvailabilityMonthly(UUID id);
    List<AssetFuelPriorityDto> getAssetFuelPriority(String plantIds, String financialYear);
    AOPMessageVM updateAssetFuelPriority(List<AssetFuelPriorityDto> payload);
    Map<UUID, Map<UUID, UUID>> processFuelPriorityNormParameters(List<AssetFuelPriorityDto> payload);
    List<UUID> getNormsHeaderIds(UUID assetId, UUID normParameterId);
    UUID getNormsHeaderId(UUID assetId, UUID normParameterId);
    int ensureNormMonthDetails(UUID normsHeaderId, String financialYear);
    int processFuelPriorityNormMonthDetails(List<AssetFuelPriorityDto> payload, Map<UUID, CPPAssetFuelPriority> entityMap, Map<UUID, Map<UUID, UUID>> resolvedNormParams);
    int ensureCppNorms(UUID normsHeaderId, String financialYear);
    int processFuelPriorityCppNorms(List<AssetFuelPriorityDto> payload, Map<UUID, CPPAssetFuelPriority> entityMap, Map<UUID, Map<UUID, UUID>> resolvedNormParams);
    record MonthlyFuelPrices(
            double apr, double may, double jun, double jul,
            double aug, double sep, double oct, double nov,
            double dec, double jan, double feb, double mar
    ) {
        public static MonthlyFuelPrices zero() {
            return new MonthlyFuelPrices(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    int ensureCppMonthWisePrice(UUID normsHeaderId, String financialYear);
    int ensureCppMonthWisePrice(UUID normsHeaderId, String financialYear, MonthlyFuelPrices prices);
    int processFuelPriorityCppMonthWisePrices(List<AssetFuelPriorityDto> payload, Map<UUID, CPPAssetFuelPriority> entityMap, Map<UUID, Map<UUID, UUID>> resolvedNormParams);
    int cleanUpUnusedFuelRecords(List<AssetFuelPriorityDto> payload, Map<UUID, CPPAssetFuelPriority> entityMap);
    int cleanUpUnusedFuelRecords(UUID assetId, String financialYear, java.util.Set<UUID> finalUsedFuelIds);
    List<CompatibleFuelAssetDto> getCompatibleFuelAssets(String plantIds);
    AOPMessageVM updateCompatibleFuelAssets(List<CompatibleFuelAssetDto> payload);
}

