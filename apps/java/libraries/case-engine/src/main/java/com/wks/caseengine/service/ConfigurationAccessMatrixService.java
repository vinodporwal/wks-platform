package com.wks.caseengine.service;

import com.wks.caseengine.dto.ConfigurationAccessMatrixDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface ConfigurationAccessMatrixService {

	public AOPMessageVM getConfigurationAccessMatrix(String plantId, String siteId, String verticalId,String type);
	public AOPMessageVM getAllConfigurationAccessMatrix();
	public AOPMessageVM getConfigurationAccessMatrixById(String id);
	public AOPMessageVM createConfigurationAccessMatrix(ConfigurationAccessMatrixDTO dto);
	public AOPMessageVM updateConfigurationAccessMatrix(String id, ConfigurationAccessMatrixDTO dto);
	public AOPMessageVM deleteConfigurationAccessMatrix(String id);

}
