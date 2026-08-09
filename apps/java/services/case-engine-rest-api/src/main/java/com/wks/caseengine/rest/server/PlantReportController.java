package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.ConversionVariableCostDTO;
import com.wks.caseengine.dto.PlantReportDTO;
import com.wks.caseengine.dto.PlantSafetyImprovementDTO;
import com.wks.caseengine.dto.ProfitImprovementInitiativeDTO;
import com.wks.caseengine.dto.ReliabilityImprovementDTO;
import com.wks.caseengine.dto.SiteSafetyPerformanceTargetsDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.PlantReportService;

@RestController
@RequestMapping("task")
public class PlantReportController {

    @Autowired
    private PlantReportService plantReportService;

    @GetMapping(value = "/plant-report") // Sp_PlantSafetyPerformanceTargets
    public AOPMessageVM getPlantReport(@RequestParam String plantId, @RequestParam String aopYear) {
        return plantReportService.getPlantReport(plantId, aopYear);
    }

    @PostMapping(value = "/plant-report")
    public AOPMessageVM savePlantReport(@RequestBody List<PlantReportDTO> plantReportDTOs) {
        List<PlantReportDTO> failedList = plantReportService.savePlantReport(plantReportDTOs);
        if (failedList.isEmpty()) {
            return new AOPMessageVM(200, "Data saved successfully", null);
        } else {
            return new AOPMessageVM(500, "Partial data saved successfully", failedList);
        }
    }

    @GetMapping(value = "/plant-safety-improvement") 
    public AOPMessageVM getPlantSafetyImprovement(@RequestParam String plantId, @RequestParam String aopYear) {
        return plantReportService.getPlantSafetyImprovement(plantId, aopYear);
    }

    @PostMapping(value = "/plant-safety-improvement")
    public AOPMessageVM savePlantSafetyImprovement(@RequestBody List<PlantSafetyImprovementDTO> plantSafetyImprovementDTOs) {
        return plantReportService.savePlantSafetyImprovement(plantSafetyImprovementDTOs);
    }

    @DeleteMapping(value = "/plant-safety-improvement")
    public AOPMessageVM deletePlantSafetyImprovement(@RequestParam String id) {
        return plantReportService.deletePlantSafetyImprovement(id);
    }

    @GetMapping(value = "/profit-improvement-initiative")
    public AOPMessageVM getProfitImprovementInitiative(@RequestParam String plantId, @RequestParam String aopYear) {
        return plantReportService.getProfitImprovementInitiative(plantId, aopYear);
    }

    @PostMapping(value = "/profit-improvement-initiative")
    public AOPMessageVM saveProfitImprovementInitiative(@RequestBody List<ProfitImprovementInitiativeDTO> profitImprovementInitiativeDTOs) {
        return plantReportService.saveProfitImprovementInitiative(profitImprovementInitiativeDTOs);
    }

    @DeleteMapping(value = "/profit-improvement-initiative")
    public AOPMessageVM deleteProfitImprovementInitiative(@RequestParam String id) {
        return plantReportService.deleteProfitImprovementInitiative(id);
    }

    @GetMapping(value = "/reliability-improvement")
    public AOPMessageVM getReliabilityImprovement(@RequestParam String plantId, @RequestParam String aopYear) {
        return plantReportService.getReliabilityImprovement(plantId, aopYear);
    }

    @PostMapping(value = "/reliability-improvement")
    public AOPMessageVM saveReliabilityImprovement(@RequestBody List<ReliabilityImprovementDTO> reliabilityImprovementDTOs) {
        return plantReportService.saveReliabilityImprovement(reliabilityImprovementDTOs);
    }

    @DeleteMapping(value = "/reliability-improvement")
    public AOPMessageVM deleteReliabilityImprovement(@RequestParam String id) {
        return plantReportService.deleteReliabilityImprovement(id);
    }

    @GetMapping(value = "/site-safety-performance") 
    public AOPMessageVM getSiteSafetyPerformanceTargets(@RequestParam String siteId, @RequestParam String aopYear) {
        return plantReportService.getSiteSafetyPerformanceTargets(siteId, aopYear);
    }

    @PostMapping(value = "/site-safety-performance")
    public AOPMessageVM saveSiteSafetyPerformanceTargets(@RequestBody List<SiteSafetyPerformanceTargetsDTO> siteSafetyPerformanceTargetsDTOs) {
        return plantReportService.saveSiteSafetyPerformanceTargets(siteSafetyPerformanceTargetsDTOs);
    }

    @GetMapping(value = "/conversion-variable-cost")
    public AOPMessageVM getConversionVariableCostData(@RequestParam String siteId, @RequestParam String aopYear) {
        return plantReportService.getConversionVariableCostData(siteId, aopYear);
    }

    @PostMapping(value = "/conversion-variable-cost")
    public AOPMessageVM saveConversionVariableCostData(@RequestBody List<ConversionVariableCostDTO> conversionVariableCostDTOs) {
        return plantReportService.saveConversionVariableCostData(conversionVariableCostDTOs);
    }

    @GetMapping(value = "/plant-report-export")
    public ResponseEntity<byte[]> exportPlantReport(
            @RequestParam String plantId,
            @RequestParam String aopYear) {
        try {
            byte[] excelBytes = plantReportService.createPlantReportExcel(plantId, aopYear, false, null);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("plant_report.xlsx")
                    .build());
            headers.setContentLength(excelBytes.length);
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/plant-report-import", consumes = "multipart/form-data")
    public AOPMessageVM importPlantReport(
            @RequestParam String plantId,
            @RequestParam String aopYear,
            @RequestParam("file") MultipartFile file) {
        return plantReportService.importPlantReportExcel(plantId, aopYear, file);
    }
}
