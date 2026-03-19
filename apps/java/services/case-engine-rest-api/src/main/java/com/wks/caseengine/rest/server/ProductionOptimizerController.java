package com.wks.caseengine.rest.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.ProductionOptimizerService;

@RestController
@RequestMapping("task")
public class ProductionOptimizerController {

	@Autowired
	private ProductionOptimizerService productionOptimizerService;

	@GetMapping(value = "/production-optimizer")
	public AOPMessageVM getProductionOptimizer(@RequestParam String plantId, @RequestParam String aopYear,
			@RequestParam String lineFkId, @RequestParam String type) {
		return productionOptimizerService.getProductionOptimizer(plantId, aopYear, lineFkId, type);
	}

	@GetMapping(value = "/calculate-production-optimizer")
	public AOPMessageVM calculateProductionOptimizer(@RequestParam String plantId, @RequestParam String aopYear) {
		return productionOptimizerService.calculateProductionOptimizer(plantId, aopYear);
	}
}

