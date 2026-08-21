package com.wks.caseengine.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.FinishingShutdownConfigDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface FinishingShutdownService {

	public AOPMessageVM getFinishingShutdown(String plantId, String year);
	public List<FinishingShutdownConfigDTO> saveFinishingShutdown(String year, String plantFKId, List<FinishingShutdownConfigDTO> finishingShutdownConfigDTOs);
	public AOPMessageVM deleteFinishingShutdown(String id);

	byte[] createFinishingShutdownExcel(String plantId, String year, boolean isAfterSave, List<FinishingShutdownConfigDTO> dtoList);
	AOPMessageVM importFinishingShutdownExcel(String year, String plantId, MultipartFile file);
}
