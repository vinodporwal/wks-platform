package com.wks.caseengine.service;

import com.wks.caseengine.message.vm.AOPMessageVM;
import java.util.List;

public interface ShutdownRateService {
	
	public AOPMessageVM getShutdownRate(String plantId, String aopYear);
	
	public List<com.wks.caseengine.dto.ShutdownRateDropdownDTO> getShutdownRateDropdown( String plantId);
	
}
