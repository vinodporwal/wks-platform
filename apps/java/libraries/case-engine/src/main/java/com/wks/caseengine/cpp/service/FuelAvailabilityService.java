package com.wks.caseengine.cpp.service;

import java.util.List;
import java.util.UUID;

import com.wks.caseengine.dto.FuelAvailabilityDto;

public interface FuelAvailabilityService {
    
    List<FuelAvailabilityDto> getFuelAvailability(UUID cppId, String financialYear, String fuelType);
    
    FuelAvailabilityDto saveFuelAvailability(FuelAvailabilityDto fuelAvailabilityDto);
    
    List<FuelAvailabilityDto> saveFuelAvailabilityBulk(List<FuelAvailabilityDto> fuelAvailabilityDtos);
    
    void deleteFuelAvailability(UUID id);
}
