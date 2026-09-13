package com.wks.caseengine.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.CapexPIOPlanTransactionDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

@Service
public interface CapexPIOService {
	
    AOPMessageVM getCapexPIO(String plantId, String aopYear);
    AOPMessageVM saveCapexPIO(String year, String plantFKId, List<CapexPIOPlanTransactionDTO> lIMSSpyroInputDTOs);    
    byte[] exportCapexPIO(String year, String plantFKId, boolean isAfterSave, List<CapexPIOPlanTransactionDTO> dtoList);
    AOPMessageVM importCapexPIO(String year, UUID plantId, MultipartFile file);
    AOPMessageVM deleteCapexPIOById(UUID id);
}

