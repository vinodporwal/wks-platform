package com.wks.caseengine.cpp.service;

import java.util.List;

import com.wks.caseengine.cpp.dto.AssetFuelPriorityDto;
import com.wks.caseengine.cpp.dto.CompatibleFuelAssetDto;
import com.wks.caseengine.cpp.dto.FuelMasterDto;
import com.wks.caseengine.cpp.dto.PlantWiseFuelPriorityDto;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface FuelPriorityService {
    List<FuelMasterDto> getFuelMaster();
    List<PlantWiseFuelPriorityDto> getPlantWiseFuelPriority(String plantIds, String financialYear);
    AOPMessageVM updatePlantFuelAvailability(List<PlantWiseFuelPriorityDto> payload);
    List<AssetFuelPriorityDto> getAssetFuelPriority(String plantIds, String financialYear);
    AOPMessageVM updateAssetFuelPriority(List<AssetFuelPriorityDto> payload);
    List<CompatibleFuelAssetDto> getCompatibleFuelAssets(String plantIds);
    AOPMessageVM updateCompatibleFuelAssets(List<CompatibleFuelAssetDto> payload);
}

