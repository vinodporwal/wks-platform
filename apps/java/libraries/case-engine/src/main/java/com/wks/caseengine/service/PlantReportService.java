package com.wks.caseengine.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.ConversionVariableCostDTO;
import com.wks.caseengine.dto.PlantReportDTO;
import com.wks.caseengine.dto.PlantSafetyImprovementDTO;
import com.wks.caseengine.dto.ProfitImprovementInitiativeDTO;
import com.wks.caseengine.dto.ReliabilityImprovementDTO;
import com.wks.caseengine.dto.SiteSafetyPerformanceTargetsDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface PlantReportService {

    AOPMessageVM getPlantReport(String plantId, String aopYear);

    List<PlantReportDTO> savePlantReport(List<PlantReportDTO> plantReportDTOs);

    AOPMessageVM getPlantSafetyImprovement(String plantId, String aopYear);

    AOPMessageVM savePlantSafetyImprovement(List<PlantSafetyImprovementDTO> plantSafetyImprovementDTOs);

    AOPMessageVM deletePlantSafetyImprovement(String id);

    AOPMessageVM getProfitImprovementInitiative(String plantId, String aopYear);

    AOPMessageVM saveProfitImprovementInitiative(List<ProfitImprovementInitiativeDTO> profitImprovementInitiativeDTOs);

    AOPMessageVM deleteProfitImprovementInitiative(String id);

    AOPMessageVM getReliabilityImprovement(String plantId, String aopYear);

    AOPMessageVM saveReliabilityImprovement(List<ReliabilityImprovementDTO> reliabilityImprovementDTOs);

    AOPMessageVM deleteReliabilityImprovement(String id);

    AOPMessageVM getSiteSafetyPerformanceTargets(String siteId, String aopYear);

    AOPMessageVM saveSiteSafetyPerformanceTargets(List<SiteSafetyPerformanceTargetsDTO> siteSafetyPerformanceTargetsDTOs);

    AOPMessageVM getConversionVariableCostData(String siteId, String aopYear);

    AOPMessageVM saveConversionVariableCostData(List<ConversionVariableCostDTO> conversionVariableCostDTOs);

    byte[] createPlantReportExcel(String plantId, String aopYear, boolean isAfterSave, List<PlantReportDTO> dtoList);

    AOPMessageVM importPlantReportExcel(String plantId, String aopYear, MultipartFile file);

    byte[] exportPlantSafetyImprovement(String plantId, String aopYear, boolean isAfterSave, List<PlantSafetyImprovementDTO> dtoList);

    AOPMessageVM importPlantSafetyImprovementExcel(String plantId, String aopYear, MultipartFile file);

    byte[] exportProfitImprovementInitiative(String plantId, String aopYear, boolean isAfterSave, List<ProfitImprovementInitiativeDTO> dtoList);

    AOPMessageVM importProfitImprovementInitiativeExcel(String plantId, String aopYear, MultipartFile file);

    byte[] exportReliabilityImprovement(String plantId, String aopYear, boolean isAfterSave, List<ReliabilityImprovementDTO> dtoList);

    AOPMessageVM importReliabilityImprovementExcel(String plantId, String aopYear, MultipartFile file);
}
