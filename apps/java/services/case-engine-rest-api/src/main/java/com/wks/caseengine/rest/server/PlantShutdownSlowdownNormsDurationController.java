package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.dto.PlantShutdownSlowdownNormsDurationDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.PlantShutdownSlowdownNormsDurationService;

@RestController
@RequestMapping("task")
public class PlantShutdownSlowdownNormsDurationController {

    @Autowired
    private PlantShutdownSlowdownNormsDurationService plantShutdownSlowdownNormsDurationService;

    @GetMapping(value = "/plant-shutdown-slowdown-norms-duration")
    public AOPMessageVM getPlantShutdownSlowdownNormsDuration(
            @RequestParam(required = false) String plantId,
            @RequestParam(required = false) String year) {
        return plantShutdownSlowdownNormsDurationService.getPlantShutdownSlowdownNormsDuration(plantId, year);
    }
    
    @DeleteMapping(value = "/plant-shutdown-slowdown-norms-duration")
    public AOPMessageVM deletePlantShutdownSlowdownNormsDuration(
            @RequestParam String id) {
        return plantShutdownSlowdownNormsDurationService.deletePlantShutdownSlowdownNormsDuration(id);
    }
    
    @PostMapping(value = "/plant-shutdown-slowdown-norms-duration")
    public AOPMessageVM updatePlantShutdownSlowdownNormsDuration(
            @RequestParam(required = false) String plantId,
            @RequestParam(required = false) String year,@RequestBody List<PlantShutdownSlowdownNormsDurationDTO> plantShutdownSlowdownNormsDurationDTOs) {
        return plantShutdownSlowdownNormsDurationService.updatePlantShutdownSlowdownNormsDuration(plantId, year,plantShutdownSlowdownNormsDurationDTOs);
    }
}

