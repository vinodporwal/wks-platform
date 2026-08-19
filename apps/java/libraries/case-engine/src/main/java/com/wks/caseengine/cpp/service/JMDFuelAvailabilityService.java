package com.wks.caseengine.cpp.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.dto.FuelAvailabilityTransactionDTO;
import com.wks.caseengine.cpp.dto.FuelMasterWithCategoryDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface JMDFuelAvailabilityService {

    AOPMessageVM getFuelAvailability(List<UUID> plantIds, String financialYear, String type);

    List<FuelMasterWithCategoryDTO> getFuels(String type);

    AOPMessageVM saveFuelAvailability(List<UUID> plantIds, String financialYear,
                                      List<FuelAvailabilityTransactionDTO> dtoList);

    byte[] exportFuelAvailability(List<UUID> plantIds, String financialYear, String type) throws IOException;

    AOPMessageVM importFuelAvailability(List<UUID> plantIds, String financialYear, MultipartFile file) throws IOException;

    AOPMessageVM deleteFuelAvailability(UUID id);
}
