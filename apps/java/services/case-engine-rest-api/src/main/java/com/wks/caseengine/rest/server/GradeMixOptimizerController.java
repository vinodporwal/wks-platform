package com.wks.caseengine.rest.server;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import com.wks.caseengine.crude.serviceimpl.GradeMixOptimizerService;
import com.wks.caseengine.message.vm.AOPMessageVM;

@RestController
@RequestMapping("task")
public class GradeMixOptimizerController {
    
    @Autowired
    private GradeMixOptimizerService gradeMixOptimizerService;

    @GetMapping("/grade-mix-optimizer-constants")
    public AOPMessageVM getGradeMixOptimizerConstants(@RequestParam String plantId, @RequestParam String aopYear) {
        return gradeMixOptimizerService.getGradeMixOptimizerConstants(UUID.fromString(plantId), aopYear);
    }

    @GetMapping("/calculate-budget-operation-hours")
    public AOPMessageVM calculateBudgetOperationHours(@RequestParam String plantId, @RequestParam String aopYear) {
        return gradeMixOptimizerService.calculateBudgetOperationHours(UUID.fromString(plantId), aopYear);
    }

    @GetMapping("/calculated-proposed-business-demand")
    public AOPMessageVM getCalculatedProposedBusinessDemand(@RequestParam String plantId, @RequestParam String aopYear, @RequestParam String lineId) {
        return gradeMixOptimizerService.getCalculatedProposedBusinessDemand(UUID.fromString(plantId), aopYear, lineId);
    }
}
