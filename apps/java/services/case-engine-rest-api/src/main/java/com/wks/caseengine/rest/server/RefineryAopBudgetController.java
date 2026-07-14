package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.wks.caseengine.dto.PlantCapacitiesTranscationDTO;
import com.wks.caseengine.service.RefineryAopBudgetService;
import com.wks.caseengine.message.vm.AOPMessageVM;

@RestController
@RequestMapping("task")
public class RefineryAopBudgetController {
    
    @Autowired
    private RefineryAopBudgetService refineryAopBudgetService;

    @GetMapping("/plant-capacities-transcation")
    public AOPMessageVM getPlantCapacitiesTranscation(@RequestParam String plantId, @RequestParam String aopYear) {
        return refineryAopBudgetService.getPlantCapacitiesTranscation(plantId, aopYear);
    }

    @PostMapping("/plant-capacities-transcation")
    public AOPMessageVM savePlantCapacitiesTranscation(@RequestBody List<PlantCapacitiesTranscationDTO> plantCapacitiesTranscationDTOs) {
        return refineryAopBudgetService.savePlantCapacitiesTranscation(plantCapacitiesTranscationDTOs);
    }
}
