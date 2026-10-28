package com.wks.caseengine.service;

import java.util.List;


import com.wks.caseengine.dto.PlantShutdownSlowdownNormsDurationDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface PlantShutdownSlowdownNormsDurationService {
    AOPMessageVM getPlantShutdownSlowdownNormsDuration(String plantId, String year);
    AOPMessageVM deletePlantShutdownSlowdownNormsDuration(String id);
    
    public AOPMessageVM updatePlantShutdownSlowdownNormsDuration(
             String plantId,
            String year, List<PlantShutdownSlowdownNormsDurationDTO> plantShutdownSlowdownNormsDurationDTOs);
}

