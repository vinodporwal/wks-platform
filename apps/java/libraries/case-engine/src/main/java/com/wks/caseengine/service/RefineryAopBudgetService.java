package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.PlantCapacitiesTranscationDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface RefineryAopBudgetService {
    
    public AOPMessageVM getPlantCapacitiesTranscation(String plantId, String aopYear);
    public AOPMessageVM savePlantCapacitiesTranscation(List<PlantCapacitiesTranscationDTO> plantCapacitiesTranscationDTOs);
}
