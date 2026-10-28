package com.wks.caseengine.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;
import com.wks.caseengine.dto.ShutDownPlanDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface ShutdownSlowdownExportImportService {
	
	public byte[] exportShutdown(String year, String plantFKId,boolean isAfterSave,List<ShutDownPlanDTO> dtoList);
	public AOPMessageVM importShutdown(String year,UUID plantId,MultipartFile file);
	
	public byte[] exportSlowdown(String year, String plantFKId,boolean isAfterSave,List<ShutDownPlanDTO> dtoList);
	public AOPMessageVM importSlowdown(String year,UUID plantId,MultipartFile file);
	
}
