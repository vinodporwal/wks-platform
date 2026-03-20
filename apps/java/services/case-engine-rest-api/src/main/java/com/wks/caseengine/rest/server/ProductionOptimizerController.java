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

	@GetMapping(value = "/combined-production-optimizer")
	public AOPMessageVM getCombinedProductionOptimizer(@RequestParam String plantId, @RequestParam String aopYear,
			@RequestParam String type) {
		return productionOptimizerService.getCombinedProductionOptimizer(plantId, aopYear, type);
	}

	@GetMapping(value = "/calculate-production-optimizer")
	public AOPMessageVM calculateProductionOptimizer(@RequestParam String plantId, @RequestParam String aopYear) {
		return productionOptimizerService.calculateProductionOptimizer(plantId, aopYear);
	}

	@GetMapping(value = "/combined-production-optimizer-dropdown")
	public AOPMessageVM getCombinedProductionOptimizerDropdown(@RequestParam String plantId) {
		return productionOptimizerService.getCombinedProductionOptimizerDropdown(plantId);
	}

	@GetMapping(value = "/production-optimizer-dropdown")
	public AOPMessageVM getProductionOptimizerDropdown(@RequestParam String plantId) {
		return productionOptimizerService.getProductionOptimizerDropdown(plantId);
	}
}

