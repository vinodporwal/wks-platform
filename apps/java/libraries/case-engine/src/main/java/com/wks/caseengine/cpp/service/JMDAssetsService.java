package com.wks.caseengine.cpp.service;

import com.wks.caseengine.dto.CPPAssetOperationalHoursResponseDto;

import java.util.List;
import java.util.UUID;

public interface JMDAssetsService {

    List<CPPAssetOperationalHoursResponseDto> getOperationalHoursForPlants(
            List<UUID> plantIds,
            String financialYear);
}
