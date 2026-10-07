package com.wks.caseengine.pcg.service;

import java.util.List;
import java.util.UUID;

import com.wks.caseengine.pcg.dto.GasifierDropdownDTO;
import com.wks.caseengine.pcg.dto.ShutdownTaTransactionDTO;

public interface ShutdownActivitiesService {

    List<GasifierDropdownDTO> getGasifierDropdown(UUID plantId, String aopYear);

    List<ShutdownTaTransactionDTO> getShutdownTaTransactions(UUID plantId, String aopYear);

    List<ShutdownTaTransactionDTO> saveShutdownTaTransactions(UUID plantId, List<ShutdownTaTransactionDTO> shutdownTaTransactionDTOList);
}
