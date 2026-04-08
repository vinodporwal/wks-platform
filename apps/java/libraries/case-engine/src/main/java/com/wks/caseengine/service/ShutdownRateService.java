package com.wks.caseengine.service;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface ShutdownRateService {
	
	public AOPMessageVM getShutdownRate(String plantId, String aopYear);
	
}
