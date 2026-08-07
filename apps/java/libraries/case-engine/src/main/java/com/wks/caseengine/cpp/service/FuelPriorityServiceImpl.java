package com.wks.caseengine.cpp.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wks.caseengine.cpp.dto.AssetFuelPriorityDto;
import com.wks.caseengine.cpp.dto.AssetFuelPriorityProjection;
import com.wks.caseengine.cpp.dto.CompatibleFuelAssetDto;
import com.wks.caseengine.cpp.dto.CompatibleFuelAssetProjection;
import com.wks.caseengine.cpp.dto.FuelMasterDto;
import com.wks.caseengine.cpp.dto.FuelMasterProjection;
import com.wks.caseengine.cpp.dto.PlantFuelAvailabilityMonthlyDto;
import com.wks.caseengine.cpp.dto.PlantFuelAvailabilityMonthlyProjection;
import com.wks.caseengine.cpp.dto.PlantWiseFuelPriorityDto;
import com.wks.caseengine.cpp.dto.PlantWiseFuelPriorityProjection;
import com.wks.caseengine.cpp.entity.CPPAssetFuelPriority;
import com.wks.caseengine.cpp.entity.CPPPlantFuelAvailabilityMonthly;
import com.wks.caseengine.cpp.repository.CPPAssetFuelPriorityRepository;
import com.wks.caseengine.cpp.repository.CPPPlantFuelAvailabilityMonthlyRepository;
import com.wks.caseengine.cpp.repository.FuelPriorityRepository;
import com.wks.caseengine.message.vm.AOPMessageVM;

@Service
public class FuelPriorityServiceImpl implements FuelPriorityService {

    private static final Logger logger = LoggerFactory.getLogger(FuelPriorityServiceImpl.class);

    @Autowired
    private FuelPriorityRepository repository;

    @Autowired
    private CPPAssetFuelPriorityRepository assetFuelPriorityRepository;

    @Autowired
    private CPPPlantFuelAvailabilityMonthlyRepository monthlyRepository;

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
        logger.info("Projection Data: Id={}, PlantName={}, FuelName={}, FuelDisplayName={}, FuelFkId={}, Priority={}, Quantity={}, Remarks={}, AopYear={}",
                p.getId(), p.getPlantName(), p.getFuelName(), p.getFuelDisplayName(), p.getFuelFkId(), p.getPriority(), p.getQuantity(), p.getRemarks(), p.getAopYear());
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

    @Override
    public List<PlantFuelAvailabilityMonthlyDto> getPlantFuelAvailabilityMonthly(String plantIds, String financialYear) {
        return monthlyRepository.getPlantFuelAvailabilityMonthly(plantIds, financialYear)
                .stream().map(this::toMonthlyDto).toList();
    }

    private PlantFuelAvailabilityMonthlyDto toMonthlyDto(PlantFuelAvailabilityMonthlyProjection p) {
        PlantFuelAvailabilityMonthlyDto dto = new PlantFuelAvailabilityMonthlyDto();
        if (p.getId() != null) dto.setId(UUID.fromString(p.getId()));
        dto.setPlantName(p.getPlantName());
        if (p.getPlantFkId() != null) dto.setPlantFkId(UUID.fromString(p.getPlantFkId()));
        if (p.getAprFuelFkId() != null) dto.setAprFuelFkId(UUID.fromString(p.getAprFuelFkId()));
        dto.setAprFuelName(p.getAprFuelName());
        dto.setAprPriority(p.getAprPriority());
        dto.setAprQuantity(p.getAprQuantity());
        if (p.getMayFuelFkId() != null) dto.setMayFuelFkId(UUID.fromString(p.getMayFuelFkId()));
        dto.setMayFuelName(p.getMayFuelName());
        dto.setMayPriority(p.getMayPriority());
        dto.setMayQuantity(p.getMayQuantity());
        if (p.getJunFuelFkId() != null) dto.setJunFuelFkId(UUID.fromString(p.getJunFuelFkId()));
        dto.setJunFuelName(p.getJunFuelName());
        dto.setJunPriority(p.getJunPriority());
        dto.setJunQuantity(p.getJunQuantity());
        if (p.getJulFuelFkId() != null) dto.setJulFuelFkId(UUID.fromString(p.getJulFuelFkId()));
        dto.setJulFuelName(p.getJulFuelName());
        dto.setJulPriority(p.getJulPriority());
        dto.setJulQuantity(p.getJulQuantity());
        if (p.getAugFuelFkId() != null) dto.setAugFuelFkId(UUID.fromString(p.getAugFuelFkId()));
        dto.setAugFuelName(p.getAugFuelName());
        dto.setAugPriority(p.getAugPriority());
        dto.setAugQuantity(p.getAugQuantity());
        if (p.getSepFuelFkId() != null) dto.setSepFuelFkId(UUID.fromString(p.getSepFuelFkId()));
        dto.setSepFuelName(p.getSepFuelName());
        dto.setSepPriority(p.getSepPriority());
        dto.setSepQuantity(p.getSepQuantity());
        if (p.getOctFuelFkId() != null) dto.setOctFuelFkId(UUID.fromString(p.getOctFuelFkId()));
        dto.setOctFuelName(p.getOctFuelName());
        dto.setOctPriority(p.getOctPriority());
        dto.setOctQuantity(p.getOctQuantity());
        if (p.getNovFuelFkId() != null) dto.setNovFuelFkId(UUID.fromString(p.getNovFuelFkId()));
        dto.setNovFuelName(p.getNovFuelName());
        dto.setNovPriority(p.getNovPriority());
        dto.setNovQuantity(p.getNovQuantity());
        if (p.getDecFuelFkId() != null) dto.setDecFuelFkId(UUID.fromString(p.getDecFuelFkId()));
        dto.setDecFuelName(p.getDecFuelName());
        dto.setDecPriority(p.getDecPriority());
        dto.setDecQuantity(p.getDecQuantity());
        if (p.getJanFuelFkId() != null) dto.setJanFuelFkId(UUID.fromString(p.getJanFuelFkId()));
        dto.setJanFuelName(p.getJanFuelName());
        dto.setJanPriority(p.getJanPriority());
        dto.setJanQuantity(p.getJanQuantity());
        if (p.getFebFuelFkId() != null) dto.setFebFuelFkId(UUID.fromString(p.getFebFuelFkId()));
        dto.setFebFuelName(p.getFebFuelName());
        dto.setFebPriority(p.getFebPriority());
        dto.setFebQuantity(p.getFebQuantity());
        if (p.getMarFuelFkId() != null) dto.setMarFuelFkId(UUID.fromString(p.getMarFuelFkId()));
        dto.setMarFuelName(p.getMarFuelName());
        dto.setMarPriority(p.getMarPriority());
        dto.setMarQuantity(p.getMarQuantity());
        dto.setRemarks(p.getRemarks());
        dto.setAopYear(p.getAopYear());
        return dto;
    }

    @Override
    public AOPMessageVM updatePlantFuelAvailabilityMonthly(List<PlantFuelAvailabilityMonthlyDto> payload) {
        AOPMessageVM response = new AOPMessageVM();
        int saved = 0;
        int skipped = 0;

        if (payload == null || payload.isEmpty()) {
            response.setCode(400);
            response.setMessage("Payload is empty");
            return response;
        }

        for (PlantFuelAvailabilityMonthlyDto dto : payload) {
            if (dto.getPlantFkId() == null) {
                skipped++;
                continue;
            }

            CPPPlantFuelAvailabilityMonthly entity;
            if (dto.getId() != null) {
                entity = monthlyRepository.findById(dto.getId()).orElse(null);
                if (entity == null) {
                    skipped++;
                    continue;
                }
            } else {
                entity = new CPPPlantFuelAvailabilityMonthly();
                entity.setCreatedDate(LocalDateTime.now());
            }

            entity.setPlantFkId(dto.getPlantFkId());
            entity.setAopYear(dto.getAopYear());
            entity.setAprFuelFkId(dto.getAprFuelFkId());
            entity.setAprPriority(dto.getAprPriority());
            entity.setAprQuantity(dto.getAprQuantity());
            entity.setMayFuelFkId(dto.getMayFuelFkId());
            entity.setMayPriority(dto.getMayPriority());
            entity.setMayQuantity(dto.getMayQuantity());
            entity.setJunFuelFkId(dto.getJunFuelFkId());
            entity.setJunPriority(dto.getJunPriority());
            entity.setJunQuantity(dto.getJunQuantity());
            entity.setJulFuelFkId(dto.getJulFuelFkId());
            entity.setJulPriority(dto.getJulPriority());
            entity.setJulQuantity(dto.getJulQuantity());
            entity.setAugFuelFkId(dto.getAugFuelFkId());
            entity.setAugPriority(dto.getAugPriority());
            entity.setAugQuantity(dto.getAugQuantity());
            entity.setSepFuelFkId(dto.getSepFuelFkId());
            entity.setSepPriority(dto.getSepPriority());
            entity.setSepQuantity(dto.getSepQuantity());
            entity.setOctFuelFkId(dto.getOctFuelFkId());
            entity.setOctPriority(dto.getOctPriority());
            entity.setOctQuantity(dto.getOctQuantity());
            entity.setNovFuelFkId(dto.getNovFuelFkId());
            entity.setNovPriority(dto.getNovPriority());
            entity.setNovQuantity(dto.getNovQuantity());
            entity.setDecFuelFkId(dto.getDecFuelFkId());
            entity.setDecPriority(dto.getDecPriority());
            entity.setDecQuantity(dto.getDecQuantity());
            entity.setJanFuelFkId(dto.getJanFuelFkId());
            entity.setJanPriority(dto.getJanPriority());
            entity.setJanQuantity(dto.getJanQuantity());
            entity.setFebFuelFkId(dto.getFebFuelFkId());
            entity.setFebPriority(dto.getFebPriority());
            entity.setFebQuantity(dto.getFebQuantity());
            entity.setMarFuelFkId(dto.getMarFuelFkId());
            entity.setMarPriority(dto.getMarPriority());
            entity.setMarQuantity(dto.getMarQuantity());
            entity.setRemarks(dto.getRemarks());
            entity.setUpdatedDate(LocalDateTime.now());

            monthlyRepository.save(entity);
            saved++;
        }

        response.setCode(200);
        response.setMessage("Plant fuel availability monthly saved. Saved: " + saved + ", Skipped: " + skipped);
        return response;
    }

    @Override
    public AOPMessageVM deletePlantFuelAvailabilityMonthly(UUID id) {
        AOPMessageVM response = new AOPMessageVM();
        try {
            if (!monthlyRepository.existsById(id)) {
                response.setCode(404);
                response.setMessage("Record not found with id: " + id);
                return response;
            }
            monthlyRepository.deleteById(id);
            response.setCode(200);
            response.setMessage("Record deleted successfully");
        } catch (Exception e) {
            response.setCode(500);
            response.setMessage("Error deleting record: " + e.getMessage());
        }
        return response;
    }

    @Override
    public List<AssetFuelPriorityDto> getAssetFuelPriority(String plantIds, String financialYear) {
        return assetFuelPriorityRepository.getAssetFuelPriority(plantIds, financialYear)
                .stream().map(this::toAssetFuelPriorityDto).toList();
    }

    private AssetFuelPriorityDto toAssetFuelPriorityDto(AssetFuelPriorityProjection p) {
        AssetFuelPriorityDto dto = new AssetFuelPriorityDto();

        if (p.getId() != null)           dto.setId(UUID.fromString(p.getId()));
        if (p.getAssetId() != null)      dto.setAssetId(UUID.fromString(p.getAssetId()));
        dto.setAssetName(p.getAssetName());
        dto.setAssetType(p.getAssetType());
        if (p.getCppPlantFkId() != null) dto.setCppPlantFkId(UUID.fromString(p.getCppPlantFkId()));
        dto.setPlantName(p.getPlantName());
        // April
        if (p.getAprPrimary() != null)   dto.setAprPrimary(UUID.fromString(p.getAprPrimary()));
        if (p.getAprSecondary() != null) dto.setAprSecondary(UUID.fromString(p.getAprSecondary()));
        if (p.getAprTertiary() != null)  dto.setAprTertiary(UUID.fromString(p.getAprTertiary()));

        // May
        if (p.getMayPrimary() != null)   dto.setMayPrimary(UUID.fromString(p.getMayPrimary()));
        if (p.getMaySecondary() != null) dto.setMaySecondary(UUID.fromString(p.getMaySecondary()));
        if (p.getMayTertiary() != null)  dto.setMayTertiary(UUID.fromString(p.getMayTertiary()));

        // June
        if (p.getJunPrimary() != null)   dto.setJunPrimary(UUID.fromString(p.getJunPrimary()));
        if (p.getJunSecondary() != null) dto.setJunSecondary(UUID.fromString(p.getJunSecondary()));
        if (p.getJunTertiary() != null)  dto.setJunTertiary(UUID.fromString(p.getJunTertiary()));

        // July
        if (p.getJulPrimary() != null)   dto.setJulPrimary(UUID.fromString(p.getJulPrimary()));
        if (p.getJulSecondary() != null) dto.setJulSecondary(UUID.fromString(p.getJulSecondary()));
        if (p.getJulTertiary() != null)  dto.setJulTertiary(UUID.fromString(p.getJulTertiary()));

        // August
        if (p.getAugPrimary() != null)   dto.setAugPrimary(UUID.fromString(p.getAugPrimary()));
        if (p.getAugSecondary() != null) dto.setAugSecondary(UUID.fromString(p.getAugSecondary()));
        if (p.getAugTertiary() != null)  dto.setAugTertiary(UUID.fromString(p.getAugTertiary()));

        // September
        if (p.getSepPrimary() != null)   dto.setSepPrimary(UUID.fromString(p.getSepPrimary()));
        if (p.getSepSecondary() != null) dto.setSepSecondary(UUID.fromString(p.getSepSecondary()));
        if (p.getSepTertiary() != null)  dto.setSepTertiary(UUID.fromString(p.getSepTertiary()));

        // October
        if (p.getOctPrimary() != null)   dto.setOctPrimary(UUID.fromString(p.getOctPrimary()));
        if (p.getOctSecondary() != null) dto.setOctSecondary(UUID.fromString(p.getOctSecondary()));
        if (p.getOctTertiary() != null)  dto.setOctTertiary(UUID.fromString(p.getOctTertiary()));

        // November
        if (p.getNovPrimary() != null)   dto.setNovPrimary(UUID.fromString(p.getNovPrimary()));
        if (p.getNovSecondary() != null) dto.setNovSecondary(UUID.fromString(p.getNovSecondary()));
        if (p.getNovTertiary() != null)  dto.setNovTertiary(UUID.fromString(p.getNovTertiary()));

        // December
        if (p.getDecPrimary() != null)   dto.setDecPrimary(UUID.fromString(p.getDecPrimary()));
        if (p.getDecSecondary() != null) dto.setDecSecondary(UUID.fromString(p.getDecSecondary()));
        if (p.getDecTertiary() != null)  dto.setDecTertiary(UUID.fromString(p.getDecTertiary()));

        // January
        if (p.getJanPrimary() != null)   dto.setJanPrimary(UUID.fromString(p.getJanPrimary()));
        if (p.getJanSecondary() != null) dto.setJanSecondary(UUID.fromString(p.getJanSecondary()));
        if (p.getJanTertiary() != null)  dto.setJanTertiary(UUID.fromString(p.getJanTertiary()));

        // February
        if (p.getFebPrimary() != null)   dto.setFebPrimary(UUID.fromString(p.getFebPrimary()));
        if (p.getFebSecondary() != null) dto.setFebSecondary(UUID.fromString(p.getFebSecondary()));
        if (p.getFebTertiary() != null)  dto.setFebTertiary(UUID.fromString(p.getFebTertiary()));

        // March
        if (p.getMarPrimary() != null)   dto.setMarPrimary(UUID.fromString(p.getMarPrimary()));
        if (p.getMarSecondary() != null) dto.setMarSecondary(UUID.fromString(p.getMarSecondary()));
        if (p.getMarTertiary() != null)  dto.setMarTertiary(UUID.fromString(p.getMarTertiary()));

        dto.setRemarks(p.getRemarks());
        dto.setCreatedDate(p.getCreatedDate());
        dto.setModifiedDate(p.getModifiedDate());

        return dto;
    }

    @Override
    public AOPMessageVM updateAssetFuelPriority(List<AssetFuelPriorityDto> payload) {
        AOPMessageVM response = new AOPMessageVM();
        int updated = 0;
        int skipped = 0;

        if (payload == null || payload.isEmpty()) {
            response.setCode(400);
            response.setMessage("Payload is empty");
            return response;
        }

        for (AssetFuelPriorityDto dto : payload) {
            if (dto.getId() == null) {
                skipped++;
                continue;
            }

            CPPAssetFuelPriority entity = assetFuelPriorityRepository.findById(dto.getId()).orElse(null);
            if (entity == null) {
                skipped++;
                continue;
            }

            // April  — entity fields are String (nvarchar), DTO fields are UUID
            entity.setAprPrimary(dto.getAprPrimary()     != null ? dto.getAprPrimary().toString()     : null);
            entity.setAprSecondary(dto.getAprSecondary() != null ? dto.getAprSecondary().toString()   : null);
            entity.setAprTertiary(dto.getAprTertiary()   != null ? dto.getAprTertiary().toString()    : null);
            // May
            entity.setMayPrimary(dto.getMayPrimary()     != null ? dto.getMayPrimary().toString()     : null);
            entity.setMaySecondary(dto.getMaySecondary() != null ? dto.getMaySecondary().toString()   : null);
            entity.setMayTertiary(dto.getMayTertiary()   != null ? dto.getMayTertiary().toString()    : null);
            // June
            entity.setJunPrimary(dto.getJunPrimary()     != null ? dto.getJunPrimary().toString()     : null);
            entity.setJunSecondary(dto.getJunSecondary() != null ? dto.getJunSecondary().toString()   : null);
            entity.setJunTertiary(dto.getJunTertiary()   != null ? dto.getJunTertiary().toString()    : null);
            // July
            entity.setJulPrimary(dto.getJulPrimary()     != null ? dto.getJulPrimary().toString()     : null);
            entity.setJulSecondary(dto.getJulSecondary() != null ? dto.getJulSecondary().toString()   : null);
            entity.setJulTertiary(dto.getJulTertiary()   != null ? dto.getJulTertiary().toString()    : null);
            // August
            entity.setAugPrimary(dto.getAugPrimary()     != null ? dto.getAugPrimary().toString()     : null);
            entity.setAugSecondary(dto.getAugSecondary() != null ? dto.getAugSecondary().toString()   : null);
            entity.setAugTertiary(dto.getAugTertiary()   != null ? dto.getAugTertiary().toString()    : null);
            // September
            entity.setSepPrimary(dto.getSepPrimary()     != null ? dto.getSepPrimary().toString()     : null);
            entity.setSepSecondary(dto.getSepSecondary() != null ? dto.getSepSecondary().toString()   : null);
            entity.setSepTertiary(dto.getSepTertiary()   != null ? dto.getSepTertiary().toString()    : null);
            // October
            entity.setOctPrimary(dto.getOctPrimary()     != null ? dto.getOctPrimary().toString()     : null);
            entity.setOctSecondary(dto.getOctSecondary() != null ? dto.getOctSecondary().toString()   : null);
            entity.setOctTertiary(dto.getOctTertiary()   != null ? dto.getOctTertiary().toString()    : null);
            // November
            entity.setNovPrimary(dto.getNovPrimary()     != null ? dto.getNovPrimary().toString()     : null);
            entity.setNovSecondary(dto.getNovSecondary() != null ? dto.getNovSecondary().toString()   : null);
            entity.setNovTertiary(dto.getNovTertiary()   != null ? dto.getNovTertiary().toString()    : null);
            // December
            entity.setDecPrimary(dto.getDecPrimary()     != null ? dto.getDecPrimary().toString()     : null);
            entity.setDecSecondary(dto.getDecSecondary() != null ? dto.getDecSecondary().toString()   : null);
            entity.setDecTertiary(dto.getDecTertiary()   != null ? dto.getDecTertiary().toString()    : null);
            // January
            entity.setJanPrimary(dto.getJanPrimary()     != null ? dto.getJanPrimary().toString()     : null);
            entity.setJanSecondary(dto.getJanSecondary() != null ? dto.getJanSecondary().toString()   : null);
            entity.setJanTertiary(dto.getJanTertiary()   != null ? dto.getJanTertiary().toString()    : null);
            // February
            entity.setFebPrimary(dto.getFebPrimary()     != null ? dto.getFebPrimary().toString()     : null);
            entity.setFebSecondary(dto.getFebSecondary() != null ? dto.getFebSecondary().toString()   : null);
            entity.setFebTertiary(dto.getFebTertiary()   != null ? dto.getFebTertiary().toString()    : null);
            // March
            entity.setMarPrimary(dto.getMarPrimary()     != null ? dto.getMarPrimary().toString()     : null);
            entity.setMarSecondary(dto.getMarSecondary() != null ? dto.getMarSecondary().toString()   : null);
            entity.setMarTertiary(dto.getMarTertiary()   != null ? dto.getMarTertiary().toString()    : null);

            entity.setRemarks(dto.getRemarks());
            entity.setModifiedDate(LocalDateTime.now());

            assetFuelPriorityRepository.save(entity);
            updated++;
        }

        response.setCode(200);
        response.setMessage("Asset fuel priority updated. Updated: " + updated + ", Skipped: " + skipped);
        return response;
    }

    @Override
    public List<CompatibleFuelAssetDto> getCompatibleFuelAssets(String plantIds) {
        if (plantIds == null || plantIds.trim().isEmpty()) {
            return repository.getCompatibleFuelAssets()
                    .stream().map(this::toCompatibleFuelAssetDto).toList();
        }
        return repository.getCompatibleFuelAssetsByPlants(plantIds)
                .stream().map(this::toCompatibleFuelAssetDto).toList();
    }

    private CompatibleFuelAssetDto toCompatibleFuelAssetDto(CompatibleFuelAssetProjection p) {
        CompatibleFuelAssetDto dto = new CompatibleFuelAssetDto();
        if (p.getAssetId() != null) {
            dto.setAssetId(UUID.fromString(p.getAssetId()));
        }
        dto.setAssetName(p.getAssetName());
        dto.setAssetType(p.getAssetType());
        if (p.getCppPlantFkId() != null) {
            dto.setCppPlantFkId(UUID.fromString(p.getCppPlantFkId()));
        }
        dto.setPlantName(p.getPlantName());
        dto.setAssetCategory(p.getAssetCategory());
        dto.setCompatibleFuel(p.getCompatibleFuel());
        return dto;
    }

    @Override
    public AOPMessageVM updateCompatibleFuelAssets(List<CompatibleFuelAssetDto> payload) {
        AOPMessageVM response = new AOPMessageVM();
        int updated = 0;
        int skipped = 0;

        if (payload == null || payload.isEmpty()) {
            response.setCode(400);
            response.setMessage("Payload is empty");
            return response;
        }

        for (CompatibleFuelAssetDto dto : payload) {
            if (dto.getAssetId() == null) {
                skipped++;
                continue;
            }
            if (dto.getCompatibleFuel() == null || dto.getCompatibleFuel().trim().isEmpty()) {
                skipped++;
                continue;
            }

            int rows = 0;
            if ("Power".equalsIgnoreCase(dto.getAssetCategory())) {
                rows = repository.updatePowerAssetCompatibleFuel(
                        dto.getAssetId().toString(),
                        dto.getCompatibleFuel());
            } else if ("Steam".equalsIgnoreCase(dto.getAssetCategory())) {
                rows = repository.updateSteamAssetCompatibleFuel(
                        dto.getAssetId().toString(),
                        dto.getCompatibleFuel());
            } else {
                skipped++;
                continue;
            }

            if (rows > 0) {
                updated++;
            } else {
                skipped++;
            }
        }

        response.setCode(200);
        response.setMessage("Compatible fuel assets updated. Updated: " + updated + ", Skipped: " + skipped);
        return response;
    }

}
