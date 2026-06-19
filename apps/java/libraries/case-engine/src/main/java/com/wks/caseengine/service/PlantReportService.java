package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.PlantReportDTO;
import com.wks.caseengine.dto.PlantSafetyImprovementDTO;
import com.wks.caseengine.dto.ProfitImprovementInitiativeDTO;
import com.wks.caseengine.dto.ReliabilityImprovementDTO;
import com.wks.caseengine.dto.SiteSafetyPerformanceTargetsDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface PlantReportService {

    AOPMessageVM getPlantReport(String plantId, String aopYear);

    AOPMessageVM savePlantReport(List<PlantReportDTO> plantReportDTOs);

    AOPMessageVM getPlantSafetyImprovement(String plantId, String aopYear);

    AOPMessageVM savePlantSafetyImprovement(List<PlantSafetyImprovementDTO> plantSafetyImprovementDTOs);

    AOPMessageVM getProfitImprovementInitiative(String plantId, String aopYear);

    AOPMessageVM saveProfitImprovementInitiative(List<ProfitImprovementInitiativeDTO> profitImprovementInitiativeDTOs);

    AOPMessageVM getReliabilityImprovement(String plantId, String aopYear);

    AOPMessageVM saveReliabilityImprovement(List<ReliabilityImprovementDTO> reliabilityImprovementDTOs);

    AOPMessageVM getSiteSafetyPerformanceTargets(String siteId, String aopYear);

    AOPMessageVM saveSiteSafetyPerformanceTargets(List<SiteSafetyPerformanceTargetsDTO> siteSafetyPerformanceTargetsDTOs);
}
