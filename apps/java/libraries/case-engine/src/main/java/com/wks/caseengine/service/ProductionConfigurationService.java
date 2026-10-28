package com.wks.caseengine.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.ConfigurationDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface ProductionConfigurationService {

	AOPMessageVM getProductionConfiguration(String year, UUID plantId);
	AOPMessageVM getProductionConfigurationElastomer(String year, UUID plantId);
	public byte[] exportProductionConfiguration(String year, String plantFKId,boolean isAfterSave,List<ConfigurationDTO> dtoList);
	public AOPMessageVM importProductionConfiguration(String year, UUID plantId, MultipartFile file);
}

