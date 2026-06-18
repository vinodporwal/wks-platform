package com.wks.caseengine.cpp.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wks.caseengine.cpp.dto.FuelMasterDto;
import com.wks.caseengine.cpp.dto.FuelMasterProjection;
import com.wks.caseengine.cpp.dto.PlantWiseFuelPriorityDto;
import com.wks.caseengine.cpp.dto.PlantWiseFuelPriorityProjection;
import com.wks.caseengine.cpp.repository.FuelPriorityRepository;

@Service
public class FuelPriorityServiceImpl implements FuelPriorityService {

    @Autowired
    private FuelPriorityRepository repository;

    @Override
    public List<FuelMasterDto> getFuelMaster() {
        return repository.getFuelMaster().stream().map(this::toDto).toList();
    }

    @Override
    public List<PlantWiseFuelPriorityDto> getPlantWiseFuelPriority(String plantIds, String financialYear) {
        return repository.getPlantWiseFuelPriority(plantIds, financialYear)
            .stream().map(this::toPlantWiseDto).toList();
    }

    private FuelMasterDto toDto(FuelMasterProjection p) {
        FuelMasterDto dto = new FuelMasterDto();
        if (p.getId() != null) {
            dto.setId(UUID.fromString(p.getId()));
        }
        dto.setFuelName(p.getFuelName());
        dto.setFuelDisplayName(p.getFuelDisplayName());
        return dto;
    }

    private PlantWiseFuelPriorityDto toPlantWiseDto(PlantWiseFuelPriorityProjection p) {
        PlantWiseFuelPriorityDto dto = new PlantWiseFuelPriorityDto();
        if (p.getId() != null) {
            dto.setId(UUID.fromString(p.getId()));
        }
        dto.setPlantName(p.getPlantName());
        dto.setFuelName(p.getFuelName());
        dto.setFuelDisplayName(p.getFuelDisplayName());
        dto.setPriority(p.getPriority());
        dto.setQuantity(p.getQuantity());
        dto.setRemarks(p.getRemarks());
        dto.setAopYear(p.getAopYear());
        return dto;
    }
}
