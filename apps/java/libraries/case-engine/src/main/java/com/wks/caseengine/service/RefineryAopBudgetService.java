package com.wks.caseengine.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.PlantCapacitiesTranscationDTO;
import com.wks.caseengine.dto.RefineryShutdownDTO;
import com.wks.caseengine.dto.RefinerySlowdownTranscationDTO;
import com.wks.caseengine.dto.VerticalsDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface RefineryAopBudgetService {
    
    public AOPMessageVM getPlantCapacitiesTranscation(String plantId, String aopYear);
    public List<PlantCapacitiesTranscationDTO> savePlantCapacitiesTranscation(List<PlantCapacitiesTranscationDTO> plantCapacitiesTranscationDTOs);
    public byte[] createPlantCapacitiesExcel(String plantId, String aopYear, boolean isAfterSave, List<PlantCapacitiesTranscationDTO> dtoList);
    public AOPMessageVM importPlantCapacitiesExcel(String plantId, String aopYear, MultipartFile file);
    public VerticalsDTO getDropDownData(String verticalId);
    public AOPMessageVM getRefineryShutdownData(String plantId, String aopYear);
    public List<RefineryShutdownDTO> saveRefineryShutdownData(List<RefineryShutdownDTO> refineryShutdownDTOs);
    public byte[] createRefineryShutdownExcel(String plantId, String aopYear, boolean isAfterSave, List<RefineryShutdownDTO> dtoList);
    public AOPMessageVM importRefineryShutdownExcel(String plantId, String aopYear, MultipartFile file);
    public AOPMessageVM deleteRefineryShutdownData(String id);
    public AOPMessageVM getRefinerySlowdownData(String plantId, String aopYear);
    public List<RefinerySlowdownTranscationDTO> saveRefinerySlowdownData(List<RefinerySlowdownTranscationDTO> refinerySlowdownDTOs);
    public byte[] createRefinerySlowdownExcel(String plantId, String aopYear, boolean isAfterSave, List<RefinerySlowdownTranscationDTO> dtoList);
    public AOPMessageVM importRefinerySlowdownExcel(String plantId, String aopYear, MultipartFile file);
   
}
