package com.wks.caseengine.rest.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}

