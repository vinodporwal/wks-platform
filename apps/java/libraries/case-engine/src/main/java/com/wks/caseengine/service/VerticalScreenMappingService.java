package com.wks.caseengine.service;

import java.util.Map;

public interface VerticalScreenMappingService {
	
	public Map<String, Object> getVerticalScreenMapping(String verticalId) throws Exception;

	public Map<String, Object> getPlantScreenMapping(String plantId, String aopYear) throws Exception;

	public Map<String, Object> getVerticalScreensWithMenuValue() throws Exception;

}
