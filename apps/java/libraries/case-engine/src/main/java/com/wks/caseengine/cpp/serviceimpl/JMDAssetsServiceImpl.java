package com.wks.caseengine.cpp.serviceimpl;

import com.wks.caseengine.dto.CPPAssetOperationalHoursResponseDto;
import com.wks.caseengine.dto.JMDOperationalHoursRequestDTO;
import com.wks.caseengine.cpp.dto.CPPAssetOperationalHoursProjection;
import com.wks.caseengine.cpp.entity.CPPAssetOperationalHours;
import com.wks.caseengine.cpp.entity.CPPSteamAssetsOperationalHours;
import com.wks.caseengine.cpp.repository.CPPAssetOperationalHoursRepository;
import com.wks.caseengine.cpp.service.JMDAssetsService;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JMDAssetsServiceImpl implements JMDAssetsService {

    private static final Logger logger = LoggerFactory.getLogger(JMDAssetsServiceImpl.class);

    @Autowired
    private CPPAssetOperationalHoursRepository repository;

    @Autowired
    private com.wks.caseengine.cpp.repository.CPPSteamAssetsOperationalHoursRepository steamRepository;

    @Override
    public AOPMessageVM getOperationalHoursForPlants(
            List<UUID> plantIds,
            String financialYear) {

        logger.info("[GET Service] Fetching operational hours for plantIds: {}, financialYear: {}", plantIds, financialYear);
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        
        try {
            logger.debug("[GET Service] Executing repository query...");
            List<CPPAssetOperationalHoursProjection> projections =
                    repository.findOperationalHoursByPlantsAndYear(plantIds, financialYear);
            logger.info("[GET Service] Query returned {} records", projections.size());

            List<CPPAssetOperationalHoursResponseDto> allResults = projections.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());

            // Separate power and steam assets
            List<CPPAssetOperationalHoursResponseDto> powerOperationalHours = allResults.stream()
                    .filter(dto -> "Power".equals(dto.getAssetCategory()))
                    .collect(Collectors.toList());
            logger.info("[GET Service] Filtered {} power assets", powerOperationalHours.size());

            List<CPPAssetOperationalHoursResponseDto> steamOperationalHours = allResults.stream()
                    .filter(dto -> "Steam".equals(dto.getAssetCategory()))
                    .collect(Collectors.toList());
            logger.info("[GET Service] Filtered {} steam assets", steamOperationalHours.size());

            Map<String, Object> data = new HashMap<>();
            data.put("PowerOperationalHours", powerOperationalHours);
            data.put("SteamOperationalHours", steamOperationalHours);

            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(data);
            logger.info("[GET Service] Successfully fetched operational hours data");
        } catch (Exception e) {
            logger.error("[GET Service] Error fetching operational hours: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to fetch data: " + e.getMessage());
            aopMessageVM.setData(null);
        }

        return aopMessageVM;
    }

    private CPPAssetOperationalHoursResponseDto mapToDto(CPPAssetOperationalHoursProjection projection) {
        CPPAssetOperationalHoursResponseDto dto = new CPPAssetOperationalHoursResponseDto();

        dto.setId(projection.getId());
        dto.setAssetFkId(projection.getAssetFkId());
        dto.setUtilityDistributed(projection.getUtilityDistributed());
        dto.setDistributedSapCode(projection.getDistributedSapCode());
        dto.setUtilityGenerated(projection.getUtilityGenerated());
        dto.setGeneratedUtilityCode(projection.getGeneratedUtilityCode());

        dto.setApr(projection.getApr());
        dto.setMay(projection.getMay());
        dto.setJun(projection.getJun());
        dto.setJul(projection.getJul());
        dto.setAug(projection.getAug());
        dto.setSep(projection.getSep());
        dto.setOct(projection.getOct());
        dto.setNov(projection.getNov());
        dto.setDec(projection.getDec());
        dto.setJan(projection.getJan());
        dto.setFeb(projection.getFeb());
        dto.setMar(projection.getMar());

        dto.setAopYear(projection.getAopYear());
        dto.setRemarks(projection.getRemarks());

        dto.setAssetCategory(projection.getAssetCategory());
        dto.setSiteFkId(projection.getSiteFkId());
        dto.setVerticalFkId(projection.getVerticalFkId());
        dto.setPlantFkId(projection.getPlantFkId());

        dto.setCreatedDate(projection.getCreatedDate());
        dto.setModifiedDate(projection.getModifiedDate());

        dto.setAssetName(projection.getAssetName());
        dto.setPlantName(projection.getPlantName());
        dto.setAssetType(projection.getAssetType());

        return dto;
    }

    @Override
    @Transactional
    public AOPMessageVM saveOperationalHours(
            List<UUID> plantIds,
            String financialYear,
            JMDOperationalHoursRequestDTO payload) {

        logger.info("[POST Service] Saving operational hours for plantIds: {}, financialYear: {}", plantIds, financialYear);
        AOPMessageVM aopMessageVM = new AOPMessageVM();

        try {
            int powerSaved = 0;
            int steamSaved = 0;

            // Process Power Operational Hours
            if (payload.getPowerResponse() != null) {
                logger.info("[POST Service] Processing {} power operational hours records", payload.getPowerResponse().size());
                for (CPPAssetOperationalHoursResponseDto dto : payload.getPowerResponse()) {
                    logger.debug("[POST Service] Saving power asset - ID: {}, AssetFkId: {}", dto.getId(), dto.getAssetFkId());
                    savePowerOperationalHours(dto, financialYear);
                    powerSaved++;
                }
                logger.info("[POST Service] Successfully saved {} power operational hours", powerSaved);
            } else {
                logger.info("[POST Service] No power operational hours to process");
            }

            // Process Steam Operational Hours
            if (payload.getSteamResponse() != null) {
                logger.info("[POST Service] Processing {} steam operational hours records", payload.getSteamResponse().size());
                for (CPPAssetOperationalHoursResponseDto dto : payload.getSteamResponse()) {
                    logger.debug("[POST Service] Saving steam asset - ID: {}, AssetFkId: {}", dto.getId(), dto.getAssetFkId());
                    saveSteamOperationalHours(dto, financialYear);
                    steamSaved++;
                }
                logger.info("[POST Service] Successfully saved {} steam operational hours", steamSaved);
            } else {
                logger.info("[POST Service] No steam operational hours to process");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("powerAssetsSaved", powerSaved);
            data.put("steamAssetsSaved", steamSaved);
            data.put("totalSaved", powerSaved + steamSaved);

            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Operational hours saved successfully");
            aopMessageVM.setData(data);
            logger.info("[POST Service] Save operation completed - Power: {}, Steam: {}, Total: {}", powerSaved, steamSaved, powerSaved + steamSaved);

        } catch (Exception e) {
            logger.error("[POST Service] Error saving operational hours: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to save operational hours: " + e.getMessage());
            aopMessageVM.setData(null);
        }

        return aopMessageVM;
    }

    private void savePowerOperationalHours(CPPAssetOperationalHoursResponseDto dto, String financialYear) {
        CPPAssetOperationalHours entity;

        if (dto.getId() != null) {
            logger.debug("[POST Service - Power] Updating existing record with ID: {}", dto.getId());
            // Update existing record
            entity = repository.findById(dto.getId())
                    .orElse(new CPPAssetOperationalHours());
            entity.setModifiedDate(LocalDateTime.now());
        } else {
            logger.debug("[POST Service - Power] Creating new record for AssetFkId: {}", dto.getAssetFkId());
            // Create new record
            entity = new CPPAssetOperationalHours();
            entity.setId(UUID.randomUUID());
            entity.setCreatedDate(LocalDateTime.now());
            entity.setModifiedDate(LocalDateTime.now());
        }

        // Map DTO to Entity
        entity.setAssetFkId(dto.getAssetFkId());
        entity.setUtilityDistributed(dto.getUtilityDistributed());
        entity.setDistributedSapCode(dto.getDistributedSapCode());
        entity.setUtilityGenerated(dto.getUtilityGenerated());
        entity.setGeneratedUtilityCode(dto.getGeneratedUtilityCode());

        entity.setApr(dto.getApr());
        entity.setMay(dto.getMay());
        entity.setJun(dto.getJun());
        entity.setJul(dto.getJul());
        entity.setAug(dto.getAug());
        entity.setSep(dto.getSep());
        entity.setOct(dto.getOct());
        entity.setNov(dto.getNov());
        entity.setDec(dto.getDec());
        entity.setJan(dto.getJan());
        entity.setFeb(dto.getFeb());
        entity.setMar(dto.getMar());

        entity.setAopYear(financialYear);
        entity.setRemarks(dto.getRemarks());
        entity.setSiteFkId(dto.getSiteFkId());
        entity.setVerticalFkId(dto.getVerticalFkId());
        entity.setPlantFkId(dto.getPlantFkId());

        CPPAssetOperationalHours saved = repository.save(entity);
        logger.debug("[POST Service - Power] Saved entity with ID: {}", saved.getId());
    }

    private void saveSteamOperationalHours(CPPAssetOperationalHoursResponseDto dto, String financialYear) {
        CPPSteamAssetsOperationalHours entity;

        if (dto.getId() != null) {
            logger.debug("[POST Service - Steam] Updating existing record with ID: {}", dto.getId());
            // Update existing record
            entity = steamRepository.findById(dto.getId())
                    .orElse(new CPPSteamAssetsOperationalHours());
            entity.setUpdatedDate(LocalDateTime.now());
        } else {
            logger.debug("[POST Service - Steam] Creating new record for SteamAssetFkId: {}", dto.getAssetFkId());
            // Create new record
            entity = new CPPSteamAssetsOperationalHours();
            entity.setId(UUID.randomUUID());
            entity.setCreatedDate(LocalDateTime.now());
            entity.setUpdatedDate(LocalDateTime.now());
        }

        // Map DTO to Entity
        entity.setSteamAssetFkId(dto.getAssetFkId());
        entity.setUtilityDistributed(dto.getUtilityDistributed());
        entity.setDistributedSapCode(dto.getDistributedSapCode());
        entity.setUtilityGenerated(dto.getUtilityGenerated());
        entity.setGeneratedUtilityCode(dto.getGeneratedUtilityCode());

        entity.setApr(dto.getApr());
        entity.setMay(dto.getMay());
        entity.setJun(dto.getJun());
        entity.setJul(dto.getJul());
        entity.setAug(dto.getAug());
        entity.setSep(dto.getSep());
        entity.setOct(dto.getOct());
        entity.setNov(dto.getNov());
        entity.setDec(dto.getDec());
        entity.setJan(dto.getJan());
        entity.setFeb(dto.getFeb());
        entity.setMar(dto.getMar());

        entity.setAopYear(financialYear);
        entity.setRemarks(dto.getRemarks());
        entity.setSiteFkId(dto.getSiteFkId());
        entity.setVerticalFkId(dto.getVerticalFkId());
        entity.setPlantFkId(dto.getPlantFkId());

        CPPSteamAssetsOperationalHours saved = steamRepository.save(entity);
        logger.debug("[POST Service - Steam] Saved entity with ID: {}", saved.getId());
    }
}
