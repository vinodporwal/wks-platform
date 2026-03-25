package com.wks.caseengine.service;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface ShutdownDetailsService {
    AOPMessageVM getShutdownDetails(String plantId, String year, String type);

   
    AOPMessageVM saveShutdownDetails(String plantId, String year, java.util.List<com.wks.caseengine.dto.ShutdownDetailsDTO> shutdownDetailsDTOs);

   
    AOPMessageVM saveRoutineShutdownPreviousYears(String plantId, String year, com.wks.caseengine.dto.ShutdownDetailsDTO shutdownDetailsDTO);

    AOPMessageVM deletePlannedShutdownDetails(String id);

    AOPMessageVM deleteRoutineShutdownPreviousYears(String id);
}

