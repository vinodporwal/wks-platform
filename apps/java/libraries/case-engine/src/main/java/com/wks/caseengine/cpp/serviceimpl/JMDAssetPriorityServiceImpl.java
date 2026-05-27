package com.wks.caseengine.cpp.serviceimpl;

import com.wks.caseengine.dto.CPPAssetPriorityResponseDto;
import com.wks.caseengine.dto.AssetPriorityRequestDTO;
import com.wks.caseengine.cpp.dto.CPPAssetPriorityProjection;
import com.wks.caseengine.cpp.entity.CPPPowerAssetPriority;
import com.wks.caseengine.cpp.entity.CPPSteamAssetPriority;
import com.wks.caseengine.cpp.repository.CPPPowerAssetPriorityRepository;
import com.wks.caseengine.cpp.repository.CPPSteamAssetPriorityRepository;
import com.wks.caseengine.cpp.service.JMDAssetPriorityService;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JMDAssetPriorityServiceImpl implements JMDAssetPriorityService {

    private static final Logger logger = LoggerFactory.getLogger(JMDAssetPriorityServiceImpl.class);

    @Autowired
    private CPPPowerAssetPriorityRepository repository;

    @Autowired
    private CPPSteamAssetPriorityRepository steamRepository;

    @Override
    public AOPMessageVM getAssetPrioritiesForPlants(List<UUID> plantIds, String aopYear) {

        logger.info("[GET Service] Fetching asset priorities for plantIds: {}, aopYear: {}", plantIds, aopYear);
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        
        try {
            logger.debug("[GET Service] Executing repository query...");
            List<CPPAssetPriorityProjection> projections =
                    repository.findAssetPrioritiesByPlants(plantIds, aopYear);
            logger.info("[GET Service] Query returned {} records", projections.size());

            List<CPPAssetPriorityResponseDto> allResults = projections.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());

            // Separate power and steam assets
            List<CPPAssetPriorityResponseDto> powerPriorities = allResults.stream()
                    .filter(dto -> "Power".equals(dto.getAssetCategory()))
                    .collect(Collectors.toList());
            logger.info("[GET Service] Filtered {} power assets", powerPriorities.size());

            List<CPPAssetPriorityResponseDto> steamPriorities = allResults.stream()
                    .filter(dto -> "Steam".equals(dto.getAssetCategory()))
                    .collect(Collectors.toList());
            logger.info("[GET Service] Filtered {} steam assets", steamPriorities.size());

            Map<String, Object> data = new HashMap<>();
            data.put("PowerAssetPriorities", powerPriorities);
            data.put("SteamAssetPriorities", steamPriorities);

            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(data);
            logger.info("[GET Service] Successfully fetched asset priorities data");
        } catch (Exception e) {
            logger.error("[GET Service] Error fetching asset priorities: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to fetch data: " + e.getMessage());
            aopMessageVM.setData(null);
        }

        return aopMessageVM;
    }

    private CPPAssetPriorityResponseDto mapToDto(CPPAssetPriorityProjection projection) {
        CPPAssetPriorityResponseDto dto = new CPPAssetPriorityResponseDto();

        dto.setId(projection.getId());
        dto.setAssetFkId(projection.getAssetFkId());
        dto.setPlantFkId(projection.getPlantFkId());

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

        dto.setRemarks(projection.getRemarks());

        dto.setAssetCategory(projection.getAssetCategory());
        dto.setSiteFkId(projection.getSiteFkId());
        dto.setVerticalFkId(projection.getVerticalFkId());

        dto.setCreatedDate(projection.getCreatedDate());
        dto.setModifiedDate(projection.getModifiedDate());

        dto.setAssetName(projection.getAssetName());
        dto.setPlantName(projection.getPlantName());
        dto.setAssetType(projection.getAssetType());

        return dto;
    }

    @Override
    @Transactional
    public AOPMessageVM saveAssetPriorities(
            List<UUID> plantIds,
            String aopYear,
            AssetPriorityRequestDTO payload) {

        logger.info("[POST Service] Saving asset priorities for plantIds: {}, aopYear: {}", plantIds, aopYear);
        AOPMessageVM aopMessageVM = new AOPMessageVM();

        try {
            int powerSaved = 0;
            int steamSaved = 0;
            int powerSkipped = 0;
            int steamSkipped = 0;
            
            List<Map<String, String>> missingIdRecords = new ArrayList<>();

            // Process Power Asset Priorities
            if (payload.getPowerResponse() != null) {
                logger.info("[POST Service] Processing {} power asset priority records", payload.getPowerResponse().size());
                for (CPPAssetPriorityResponseDto dto : payload.getPowerResponse()) {
                    if (dto.getId() == null) {
                        powerSkipped++;
                        String assetInfo = String.format("Power Asset: %s (AssetFkId: %s)", 
                            dto.getAssetName() != null ? dto.getAssetName() : "Unknown",
                            dto.getAssetFkId() != null ? dto.getAssetFkId().toString() : "null");
                        logger.warn("[POST Service - Power] Skipping record without ID - {}", assetInfo);
                        
                        Map<String, String> missingRecord = new HashMap<>();
                        missingRecord.put("assetName", dto.getAssetName() != null ? dto.getAssetName() : "Unknown");
                        missingRecord.put("assetFkId", dto.getAssetFkId() != null ? dto.getAssetFkId().toString() : "null");
                        missingRecord.put("category", "Power");
                        missingRecord.put("reason", "ID is null - cannot update non-existent record");
                        missingIdRecords.add(missingRecord);
                        continue;
                    }
                    
                    logger.debug("[POST Service] Saving power asset priority - ID: {}, AssetFkId: {}", dto.getId(), dto.getAssetFkId());
                    boolean saved = savePowerAssetPriority(dto);
                    if (saved) {
                        powerSaved++;
                    } else {
                        powerSkipped++;
                        Map<String, String> missingRecord = new HashMap<>();
                        missingRecord.put("id", dto.getId().toString());
                        missingRecord.put("assetName", dto.getAssetName() != null ? dto.getAssetName() : "Unknown");
                        missingRecord.put("assetFkId", dto.getAssetFkId() != null ? dto.getAssetFkId().toString() : "null");
                        missingRecord.put("category", "Power");
                        missingRecord.put("reason", "Record with this ID does not exist in database");
                        missingIdRecords.add(missingRecord);
                    }
                }
                logger.info("[POST Service] Power assets - Saved: {}, Skipped: {}", powerSaved, powerSkipped);
            } else {
                logger.info("[POST Service] No power asset priorities to process");
            }

            // Process Steam Asset Priorities
            if (payload.getSteamResponse() != null) {
                logger.info("[POST Service] Processing {} steam asset priority records", payload.getSteamResponse().size());
                for (CPPAssetPriorityResponseDto dto : payload.getSteamResponse()) {
                    if (dto.getId() == null) {
                        steamSkipped++;
                        String assetInfo = String.format("Steam Asset: %s (AssetFkId: %s)", 
                            dto.getAssetName() != null ? dto.getAssetName() : "Unknown",
                            dto.getAssetFkId() != null ? dto.getAssetFkId().toString() : "null");
                        logger.warn("[POST Service - Steam] Skipping record without ID - {}", assetInfo);
                        
                        Map<String, String> missingRecord = new HashMap<>();
                        missingRecord.put("assetName", dto.getAssetName() != null ? dto.getAssetName() : "Unknown");
                        missingRecord.put("assetFkId", dto.getAssetFkId() != null ? dto.getAssetFkId().toString() : "null");
                        missingRecord.put("category", "Steam");
                        missingRecord.put("reason", "ID is null - cannot update non-existent record");
                        missingIdRecords.add(missingRecord);
                        continue;
                    }
                    
                    logger.debug("[POST Service] Saving steam asset priority - ID: {}, AssetFkId: {}", dto.getId(), dto.getAssetFkId());
                    boolean saved = saveSteamAssetPriority(dto);
                    if (saved) {
                        steamSaved++;
                    } else {
                        steamSkipped++;
                        Map<String, String> missingRecord = new HashMap<>();
                        missingRecord.put("id", dto.getId().toString());
                        missingRecord.put("assetName", dto.getAssetName() != null ? dto.getAssetName() : "Unknown");
                        missingRecord.put("assetFkId", dto.getAssetFkId() != null ? dto.getAssetFkId().toString() : "null");
                        missingRecord.put("category", "Steam");
                        missingRecord.put("reason", "Record with this ID does not exist in database");
                        missingIdRecords.add(missingRecord);
                    }
                }
                logger.info("[POST Service] Steam assets - Saved: {}, Skipped: {}", steamSaved, steamSkipped);
            } else {
                logger.info("[POST Service] No steam asset priorities to process");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("powerAssetsSaved", powerSaved);
            data.put("steamAssetsSaved", steamSaved);
            data.put("totalSaved", powerSaved + steamSaved);
            data.put("powerAssetsSkipped", powerSkipped);
            data.put("steamAssetsSkipped", steamSkipped);
            data.put("totalSkipped", powerSkipped + steamSkipped);
            
            if (!missingIdRecords.isEmpty()) {
                data.put("skippedRecords", missingIdRecords);
            }

            if (missingIdRecords.isEmpty()) {
                aopMessageVM.setCode(200);
                aopMessageVM.setMessage("All asset priorities saved successfully");
            } else {
                aopMessageVM.setCode(207); // 207 Multi-Status - partial success
                aopMessageVM.setMessage(String.format("Partial success: %d saved, %d skipped (missing or invalid IDs)", 
                    powerSaved + steamSaved, powerSkipped + steamSkipped));
            }
            
            aopMessageVM.setData(data);
            logger.info("[POST Service] Save operation completed - Saved: {} (Power: {}, Steam: {}), Skipped: {} (Power: {}, Steam: {})", 
                powerSaved + steamSaved, powerSaved, steamSaved, powerSkipped + steamSkipped, powerSkipped, steamSkipped);

        } catch (Exception e) {
            logger.error("[POST Service] Error saving asset priorities: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to save asset priorities: " + e.getMessage());
            aopMessageVM.setData(null);
        }

        return aopMessageVM;
    }

    private boolean savePowerAssetPriority(CPPAssetPriorityResponseDto dto) {
        logger.debug("[POST Service - Power] Updating existing record with ID: {}", dto.getId());
        
        // Only update existing records - do not create new ones
        var optionalEntity = repository.findById(dto.getId());
        
        if (optionalEntity.isEmpty()) {
            logger.error("[POST Service - Power] Record with ID {} not found in database. Asset: {}", 
                dto.getId(), dto.getAssetName());
            return false;
        }
        
        CPPPowerAssetPriority entity = optionalEntity.get();
        entity.setUpdatedDate(LocalDateTime.now());

        // Map DTO to Entity
        // entity.setAssetFkId(dto.getAssetFkId());
        // entity.setPlantFkId(dto.getPlantFkId());

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

        entity.setRemarks(dto.getRemarks());

        CPPPowerAssetPriority saved = repository.save(entity);
        logger.debug("[POST Service - Power] Successfully updated entity with ID: {}", saved.getId());
        return true;
    }

    private boolean saveSteamAssetPriority(CPPAssetPriorityResponseDto dto) {
        logger.debug("[POST Service - Steam] Updating existing record with ID: {}", dto.getId());
        
        // Only update existing records - do not create new ones
        var optionalEntity = steamRepository.findById(dto.getId());
        
        if (optionalEntity.isEmpty()) {
            logger.error("[POST Service - Steam] Record with ID {} not found in database. Asset: {}", 
                dto.getId(), dto.getAssetName());
            return false;
        }
        
        CPPSteamAssetPriority entity = optionalEntity.get();
        entity.setUpdatedDate(LocalDateTime.now());

        // Map DTO to Entity
        entity.setAssetFkId(dto.getAssetFkId());
        entity.setPlantFkId(dto.getPlantFkId());

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

        entity.setRemarks(dto.getRemarks());

        CPPSteamAssetPriority saved = steamRepository.save(entity);
        logger.debug("[POST Service - Steam] Successfully updated entity with ID: {}", saved.getId());
        return true;
    }
}
