package com.wks.caseengine.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.NormConfigurationDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface ProductionRangeService {
	
    AOPMessageVM getProductionRange(String plantId, String aopYear);
    AOPMessageVM getProductionRangeLimit(String plantId, String aopYear);
    public byte[] exportProductionRange(String year, String plantFKId,boolean isAfterSave,List<NormConfigurationDTO> dtoList);
    public byte[] exportProductionRangeLimit(String year, String plantFKId,boolean isAfterSave,List<NormConfigurationDTO> dtoList);
    public AOPMessageVM importProductionRange(String year,UUID plantId,MultipartFile file,boolean isMinMax);
    public AOPMessageVM importProductionRangeLimit(String year,UUID plantId,MultipartFile file);
}

