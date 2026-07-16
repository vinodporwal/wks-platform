package com.wks.caseengine.rest.crude;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.crude.service.SteadyStateService;
import com.wks.caseengine.message.vm.AOPMessageVM;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("task")
public class SteadyStateController {
    
    @Autowired
    private SteadyStateService steadyStateService;

    @GetMapping(value = "/crude/calculate-steady-state-norms")
	public AOPMessageVM getNormalOperationNormsDataFromSP(@RequestParam String year, @RequestParam String plantId) {
		 return steadyStateService.calculateExpressionConsumptionNorms(year, plantId);
	}
}
