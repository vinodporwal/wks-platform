package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.FurnaceMaintenanceActivityDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface FurnaceMaintenanceActivityService {
	
	public AOPMessageVM getFurnaceMaintenanceActivities(String plantId, String aopYear);
	
}
