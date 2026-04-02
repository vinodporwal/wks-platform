package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.ShutdownDetailsDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface ShutdownDetailsService {
    AOPMessageVM getShutdownDetails(String plantId, String year, String type);

    
    AOPMessageVM saveShutdownDetails(String plantId, String year, List<ShutdownDetailsDTO> shutdownDetailsDTOs);

    AOPMessageVM saveRoutineShutdwn(String plantId, String year, List<ShutdownDetailsDTO> shutdownDetailsDTOs);
    AOPMessageVM saveRoutineShutdownPreviousYears(String plantId, String year, List<ShutdownDetailsDTO> shutdownDetailsDTO);

    AOPMessageVM deletePlannedShutdownDetails(String id);

    AOPMessageVM deleteRoutineShutdownPreviousYears(String id);
    
    AOPMessageVM deleteRoutineShutdown(String id);
    
    
}

