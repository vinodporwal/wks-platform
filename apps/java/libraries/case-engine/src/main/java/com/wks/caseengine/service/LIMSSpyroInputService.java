package com.wks.caseengine.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.CrackerHMDLoadLIMSSpyroInputDTO;
import com.wks.caseengine.dto.LIMSSpyroInputDTO;
import com.wks.caseengine.dto.NaphthaQualityDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface LIMSSpyroInputService {
	
    AOPMessageVM getLIMSSpyroInput(String plantId, String aopYear);
	AOPMessageVM getLIMSDate(String plantId, String aopYear);
    AOPMessageVM loadLIMSSpyroInput(String plantId, String aopYear, String startDate, String endDate);
    AOPMessageVM saveLIMSSpyroInput(String year, String plantFKId, List<LIMSSpyroInputDTO> lIMSSpyroInputDTOs);
    AOPMessageVM saveNaphthaQuality(String year, String plantFKId, List<NaphthaQualityDTO> naphthaQualityDTOs);
    AOPMessageVM saveCrackerHMDLIMSSpyroInput(String year, String plantFKId, List<CrackerHMDLoadLIMSSpyroInputDTO> crackerHMDLoadLIMSSpyroInputDTOs);
    
    byte[] exportLIMSSpyroInput(String year, String plantFKId, boolean isAfterSave, List<LIMSSpyroInputDTO> dtoList);
    AOPMessageVM importLIMSSpyroInput(String year, UUID plantId, MultipartFile file);

    
    AOPMessageVM getNaphthaQuality(String plantId, String aopYear);
    AOPMessageVM getCrackerHMDLIMSSpyroInput(String plantId, String aopYear);
}

