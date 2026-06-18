package com.wks.caseengine.cpp.service;

import java.util.List;

import com.wks.caseengine.cpp.dto.FuelMasterDto;
import com.wks.caseengine.cpp.dto.PlantWiseFuelPriorityDto;

public interface FuelPriorityService {
    List<FuelMasterDto> getFuelMaster();
    List<PlantWiseFuelPriorityDto> getPlantWiseFuelPriority(String plantIds, String financialYear);
}
