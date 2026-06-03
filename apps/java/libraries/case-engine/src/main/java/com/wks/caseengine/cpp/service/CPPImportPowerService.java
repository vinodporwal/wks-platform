package com.wks.caseengine.cpp.service;

import com.wks.caseengine.dto.CPPImportPowerResponseDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface CPPImportPowerService {

    AOPMessageVM getImportedPowerPlans(
            List<UUID> plantIds,
            String aopYear);

    AOPMessageVM saveImportedPowerPlans(
            List<UUID> plantIds,
            String aopYear,
            List<CPPImportPowerResponseDTO> payload);

    byte[] exportImportedPowerPlans(
            List<UUID> plantIds,
            String aopYear);

    AOPMessageVM importImportedPowerPlans(
            List<UUID> plantIds,
            String aopYear,
            MultipartFile file);
}
