package com.wks.caseengine.cpp.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.FuelAvailabilityDto;

public interface FuelAvailabilityService {
    
    List<FuelAvailabilityDto> getFuelAvailability(UUID cppId, String financialYear, String fuelType);
    
    FuelAvailabilityDto saveFuelAvailability(FuelAvailabilityDto dto);
    
    List<FuelAvailabilityDto> saveFuelAvailabilityBulk(List<FuelAvailabilityDto> dtos);
    
    void deleteFuelAvailability(UUID id);
    
    byte[] exportFuelAvailability(UUID cppId, String financialYear, String fuelType) throws IOException;
    
    void importFuelAvailability(MultipartFile file) throws IOException;
}
