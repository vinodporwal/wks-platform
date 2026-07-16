package com.wks.caseengine.service;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface ProductionOptimizerService {

	AOPMessageVM getProductionOptimizer(String plantId, String aopYear, String lineFkId, String type);
	AOPMessageVM getCombinedProductionOptimizer(String plantId, String aopYear, String type);
	AOPMessageVM calculateProductionOptimizer(String plantId, String aopYear);
	AOPMessageVM getCombinedProductionOptimizerDropdown(String plantId);
	AOPMessageVM getProductionOptimizerDropdown(String plantId);
	byte[] exportProductionOptimizer(String plantId, String aopYear, String type);

}

