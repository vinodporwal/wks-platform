package com.wks.caseengine.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.PlantCapacitiesTranscationDTO;
import com.wks.caseengine.dto.VerticalsDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface RefineryAopBudgetService {
    
    public AOPMessageVM getPlantCapacitiesTranscation(String plantId, String aopYear);
    public List<PlantCapacitiesTranscationDTO> savePlantCapacitiesTranscation(List<PlantCapacitiesTranscationDTO> plantCapacitiesTranscationDTOs);
    public byte[] createPlantCapacitiesExcel(String plantId, String aopYear, boolean isAfterSave, List<PlantCapacitiesTranscationDTO> dtoList);
    public AOPMessageVM importPlantCapacitiesExcel(String plantId, String aopYear, MultipartFile file);
    public VerticalsDTO getDropDownData(String verticalId);
}
