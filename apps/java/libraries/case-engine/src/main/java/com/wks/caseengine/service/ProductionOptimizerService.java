package com.wks.caseengine.service;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface ProductionOptimizerService {

	AOPMessageVM getProductionOptimizer(String plantId, String aopYear, String lineFkId, String type);
	AOPMessageVM calculateProductionOptimizer(String plantId, String aopYear);

}

