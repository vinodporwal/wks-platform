package com.wks.caseengine.cpp.service;

import com.wks.caseengine.cpp.dto.FixedConsumptionCreateRequestDto;
import com.wks.caseengine.cpp.dto.JMDFixedConsumptionDto;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface JMDFixedConsumptionService {

    AOPMessageVM getFixedConsumptionForPlants(
            List<UUID> plantIds,
            String financialYear);

    AOPMessageVM saveFixedConsumption(
            List<UUID> plantIds,
            String financialYear,
            List<JMDFixedConsumptionDto> payload);

    byte[] exportFixedConsumption(
            List<UUID> plantIds,
            String financialYear);

    AOPMessageVM importFixedConsumption(
            List<UUID> plantIds,
            String financialYear,
            MultipartFile file);

    AOPMessageVM createFixedConsumption(FixedConsumptionCreateRequestDto request);

    AOPMessageVM updateFixedConsumption(FixedConsumptionCreateRequestDto request);

    AOPMessageVM deleteFixedConsumption(UUID id);
}
