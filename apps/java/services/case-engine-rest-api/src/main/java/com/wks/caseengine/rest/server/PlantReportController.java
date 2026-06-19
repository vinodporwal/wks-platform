package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.dto.PlantReportDTO;
import com.wks.caseengine.dto.PlantSafetyImprovementDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.PlantReportService;

@RestController
@RequestMapping("task")
public class PlantReportController {

    @Autowired
    private PlantReportService plantReportService;

    @GetMapping(value = "/plant-report")
    public AOPMessageVM getPlantReport(@RequestParam String plantId, @RequestParam String aopYear) {
        return plantReportService.getPlantReport(plantId, aopYear);
    }

    @PostMapping(value = "/plant-report")
    public AOPMessageVM savePlantReport(@RequestBody List<PlantReportDTO> plantReportDTOs) {
        return plantReportService.savePlantReport(plantReportDTOs);
    }

    @GetMapping(value = "/plant-safety-improvement")
    public AOPMessageVM getPlantSafetyImprovement(@RequestParam String plantId, @RequestParam String aopYear) {
        return plantReportService.getPlantSafetyImprovement(plantId, aopYear);
    }

    @PostMapping(value = "/plant-safety-improvement")
    public AOPMessageVM savePlantSafetyImprovement(@RequestBody List<PlantSafetyImprovementDTO> plantSafetyImprovementDTOs) {
        return plantReportService.savePlantSafetyImprovement(plantSafetyImprovementDTOs);
    }
}
