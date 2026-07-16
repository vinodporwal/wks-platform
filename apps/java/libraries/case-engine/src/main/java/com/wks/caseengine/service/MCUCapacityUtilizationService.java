package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.MCUCapacityUtilizationDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface MCUCapacityUtilizationService {

    AOPMessageVM getMCUCapacityUtilization(String aopYear, String siteId);

    AOPMessageVM updateMCUCapacityUtilization(List<MCUCapacityUtilizationDTO> dtoList);
}
