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
import com.wks.caseengine.message.vm.AOPMessageVM;

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

    @Override
    public AOPMessageVM updatePlantFuelAvailability(List<PlantWiseFuelPriorityDto> payload) {
        AOPMessageVM response = new AOPMessageVM();
        int updated = 0;
        int skipped = 0;

        if (payload == null || payload.isEmpty()) {
            response.setCode(400);
            response.setMessage("Payload is empty");
            return response;
        }

        for (PlantWiseFuelPriorityDto dto : payload) {
            if (dto.getId() == null) {
                skipped++;
                continue;
            }
            if (dto.getFuelFkId() == null) {
                skipped++;
                continue;
            }

            int rows = repository.updatePlantFuelAvailability(
                    dto.getId().toString(),
                    dto.getFuelFkId().toString(),
                    dto.getPriority(),
                    dto.getQuantity(),
                    dto.getRemarks());

            if (rows > 0) {
                updated++;
            } else {
                skipped++;
            }
        }

        response.setCode(200);
        response.setMessage("Plant fuel availability updated. Updated: " + updated + ", Skipped: " + skipped);
        return response;
    }

    private PlantWiseFuelPriorityDto toPlantWiseDto(PlantWiseFuelPriorityProjection p) {
        PlantWiseFuelPriorityDto dto = new PlantWiseFuelPriorityDto();
        if (p.getId() != null) {
            dto.setId(UUID.fromString(p.getId()));
        }
        dto.setPlantName(p.getPlantName());
        dto.setFuelName(p.getFuelName());
        dto.setFuelDisplayName(p.getFuelDisplayName());
        if (p.getFuelFkId() != null) {
            dto.setFuelFkId(UUID.fromString(p.getFuelFkId()));
        }
        dto.setPriority(p.getPriority());
        dto.setQuantity(p.getQuantity());
        dto.setRemarks(p.getRemarks());
        dto.setAopYear(p.getAopYear());
        return dto;
    }

}
