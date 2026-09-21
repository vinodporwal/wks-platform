package com.wks.caseengine.RefineryUtility.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.RefineryUtility.dto.MonthWiseConstantsDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface RefineryUtilityConfigurationService {
    
    public AOPMessageVM getMonthWiseConstants(String year, String plantFKId);
    public List<MonthWiseConstantsDTO> saveMonthWiseConstants(String year, String plantFKId, List<MonthWiseConstantsDTO> monthWiseConstantsDTOList);
    public byte[] exportMonthWiseConstants(String year, String plantFKId,boolean isAfterSave,List<MonthWiseConstantsDTO> dtoList,Boolean isSummerWinter);
	public AOPMessageVM importMonthWiseConstants(String year,UUID plantId,MultipartFile file,Boolean isSummerWinter);
    public AOPMessageVM checkIsSummerWinterPlant(String plantId);
}
