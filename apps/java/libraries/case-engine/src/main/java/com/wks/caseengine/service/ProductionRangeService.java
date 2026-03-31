package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.NormConfigurationDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface ProductionRangeService {
	
    AOPMessageVM getProductionRange(String plantId, String aopYear);
    AOPMessageVM getProductionRangeLimit(String plantId, String aopYear);
    public byte[] exportProductionRange(String year, String plantFKId,boolean isAfterSave,List<NormConfigurationDTO> dtoList);
    public byte[] exportProductionRangeLimit(String year, String plantFKId,boolean isAfterSave,List<NormConfigurationDTO> dtoList);
}

