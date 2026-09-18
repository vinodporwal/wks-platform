package com.wks.caseengine.RefineryUtility.service;

import java.util.List;

import com.wks.caseengine.RefineryUtility.dto.MonthWiseConstantsDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface RefineryUtilityConfigurationService {
    
    public AOPMessageVM getMonthWiseConstants(String year, String plantFKId);
    public List<MonthWiseConstantsDTO> saveMonthWiseConstants(String year, String plantFKId, List<MonthWiseConstantsDTO> monthWiseConstantsDTOList);
}
