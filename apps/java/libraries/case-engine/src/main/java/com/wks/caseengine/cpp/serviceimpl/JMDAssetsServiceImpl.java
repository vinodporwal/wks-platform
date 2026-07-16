package com.wks.caseengine.cpp.serviceimpl;

import com.wks.caseengine.dto.JMDOperationalHoursRequestDTO;
import com.wks.caseengine.cpp.dto.CPPAssetOperationalHoursProjection;
import com.wks.caseengine.cpp.dto.CPPAssetOperationalHoursResponseDto;
import com.wks.caseengine.cpp.entity.CPPAssetOperationalHours;
import com.wks.caseengine.cpp.entity.CPPSteamAssetsOperationalHours;
import com.wks.caseengine.cpp.repository.CPPAssetOperationalHoursRepository;
import com.wks.caseengine.cpp.service.JMDAssetsService;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JMDAssetsServiceImpl implements JMDAssetsService {

    private static final Logger logger = LoggerFactory.getLogger(JMDAssetsServiceImpl.class);

    @Autowired
    private CPPAssetOperationalHoursRepository repository;

    @Autowired
    private com.wks.caseengine.cpp.repository.CPPSteamAssetsOperationalHoursRepository steamRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

            // ── TIER 2: CARRY-FORWARD – clone from previous FY if requested FY is empty ──
            if (projections.isEmpty()) {
                logger.info("[GET Service] No records found for {}. Attempting carry-forward from previous financial year.", financialYear);
                String previousYear = derivePreviousFinancialYear(financialYear);

                if (previousYear != null) {
                    logger.info("[GET Service] Looking for carry-forward source in financialYear: {}", previousYear);

                    // Carry-forward Power assets
                    for (UUID plantId : plantIds) {
                        List<CPPAssetOperationalHours> prevPowerRecords = repository.findByPlantFkIdAndAopYear(plantId, previousYear);
                        if (!prevPowerRecords.isEmpty()) {
                            logger.info("[GET Service] Carrying forward {} power records for plantId: {} from {}", prevPowerRecords.size(), plantId, previousYear);
                            List<CPPAssetOperationalHours> clones = new ArrayList<>();
                            for (CPPAssetOperationalHours src : prevPowerRecords) {
                                CPPAssetOperationalHours clone = new CPPAssetOperationalHours();
                                clone.setId(UUID.randomUUID());
                                clone.setAssetFkId(src.getAssetFkId());
                                clone.setApr(src.getApr());
                                clone.setMay(src.getMay());
                                clone.setJun(src.getJun());
                                clone.setJul(src.getJul());
                                clone.setAug(src.getAug());
                                clone.setSep(src.getSep());
                                clone.setOct(src.getOct());
                                clone.setNov(src.getNov());
                                clone.setDec(src.getDec());
                                clone.setJan(src.getJan());
                                clone.setFeb(src.getFeb());
                                clone.setMar(src.getMar());
                                clone.setAopYear(financialYear);
                                clone.setRemarks(src.getRemarks());
                                clone.setSiteFkId(src.getSiteFkId());
                                clone.setVerticalFkId(src.getVerticalFkId());
                                clone.setPlantFkId(src.getPlantFkId());
                                clone.setCreatedDate(LocalDateTime.now());
                                clone.setModifiedDate(LocalDateTime.now());
                                clones.add(clone);
                            }
                            repository.saveAll(clones);
                            logger.info("[GET Service] Saved {} carry-forward power records for plantId: {}", clones.size(), plantId);
                        }

                        // Carry-forward Steam assets
                        List<CPPSteamAssetsOperationalHours> prevSteamRecords = steamRepository.findByPlantFkIdAndAopYear(plantId, previousYear);
                        if (!prevSteamRecords.isEmpty()) {
                            logger.info("[GET Service] Carrying forward {} steam records for plantId: {} from {}", prevSteamRecords.size(), plantId, previousYear);
                            List<CPPSteamAssetsOperationalHours> steamClones = new ArrayList<>();
                            for (CPPSteamAssetsOperationalHours src : prevSteamRecords) {
                                CPPSteamAssetsOperationalHours clone = new CPPSteamAssetsOperationalHours();
                                clone.setId(UUID.randomUUID());
                                clone.setSteamAssetFkId(src.getSteamAssetFkId());
                                clone.setApr(src.getApr());
                                clone.setMay(src.getMay());
                                clone.setJun(src.getJun());
                                clone.setJul(src.getJul());
                                clone.setAug(src.getAug());
                                clone.setSep(src.getSep());
                                clone.setOct(src.getOct());
                                clone.setNov(src.getNov());
                                clone.setDec(src.getDec());
                                clone.setJan(src.getJan());
                                clone.setFeb(src.getFeb());
                                clone.setMar(src.getMar());
                                clone.setAopYear(financialYear);
                                clone.setRemarks(src.getRemarks());
                                clone.setSiteFkId(src.getSiteFkId());
                                clone.setVerticalFkId(src.getVerticalFkId());
                                clone.setPlantFkId(src.getPlantFkId());
                                clone.setCreatedDate(LocalDateTime.now());
                                clone.setUpdatedDate(LocalDateTime.now());
                                steamClones.add(clone);
                            }
                            steamRepository.saveAll(steamClones);
                            logger.info("[GET Service] Saved {} carry-forward steam records for plantId: {}", steamClones.size(), plantId);
                        }
                    }

                    // Re-query after carry-forward attempt
                    projections = repository.findOperationalHoursByPlantsAndYear(plantIds, financialYear);
                    logger.info("[GET Service] After carry-forward, re-query returned {} records for {}", projections.size(), financialYear);
                } else {
                    logger.warn("[GET Service] Could not derive previous financial year from '{}'. Skipping carry-forward.", financialYear);
                }
            }

            // ── TIER 3: ZERO-SEED – both FY and previous FY have no data → seed from asset tables ──
            if (projections.isEmpty()) {
                logger.info("[GET Service] No carry-forward data found. Seeding zero operational hours from asset tables for {}.", financialYear);

                List<CPPAssetOperationalHoursProjection> assetProjections = repository.findAllAssetsForPlants(plantIds);
                logger.info("[GET Service] Found {} assets in asset tables to zero-seed.", assetProjections.size());

                if (!assetProjections.isEmpty()) {
                    List<CPPAssetOperationalHours>          powerSeeds = new ArrayList<>();
                    List<CPPSteamAssetsOperationalHours>    steamSeeds = new ArrayList<>();

                    for (CPPAssetOperationalHoursProjection asset : assetProjections) {
                        if ("Power".equals(asset.getAssetCategory())) {
                            CPPAssetOperationalHours seed = new CPPAssetOperationalHours();
                            seed.setId(UUID.randomUUID());
                            seed.setAssetFkId(asset.getAssetFkId());
                            seed.setApr(0.0);  seed.setMay(0.0);  seed.setJun(0.0);
                            seed.setJul(0.0);  seed.setAug(0.0);  seed.setSep(0.0);
                            seed.setOct(0.0);  seed.setNov(0.0);  seed.setDec(0.0);
                            seed.setJan(0.0);  seed.setFeb(0.0);  seed.setMar(0.0);
                            seed.setAopYear(financialYear);
                            seed.setRemarks(null);
                            seed.setSiteFkId(asset.getSiteFkId());
                            seed.setVerticalFkId(asset.getVerticalFkId());
                            seed.setPlantFkId(asset.getPlantFkId());
                            seed.setCreatedDate(LocalDateTime.now());
                            seed.setModifiedDate(LocalDateTime.now());
                            powerSeeds.add(seed);
                        } else if ("Steam".equals(asset.getAssetCategory())) {
                            CPPSteamAssetsOperationalHours seed = new CPPSteamAssetsOperationalHours();
                            seed.setId(UUID.randomUUID());
                            seed.setSteamAssetFkId(asset.getAssetFkId());
                            seed.setApr(0.0);  seed.setMay(0.0);  seed.setJun(0.0);
                            seed.setJul(0.0);  seed.setAug(0.0);  seed.setSep(0.0);
                            seed.setOct(0.0);  seed.setNov(0.0);  seed.setDec(0.0);
                            seed.setJan(0.0);  seed.setFeb(0.0);  seed.setMar(0.0);
                            seed.setAopYear(financialYear);
                            seed.setRemarks(null);
                            seed.setSiteFkId(asset.getSiteFkId());
                            seed.setVerticalFkId(asset.getVerticalFkId());
                            seed.setPlantFkId(asset.getPlantFkId());
                            seed.setCreatedDate(LocalDateTime.now());
                            seed.setUpdatedDate(LocalDateTime.now());
                            steamSeeds.add(seed);
                        }
                    }

                    if (!powerSeeds.isEmpty()) {
                        repository.saveAll(powerSeeds);
                        logger.info("[GET Service] Inserted {} zero-seeded power records for {}.", powerSeeds.size(), financialYear);
                    }
                    if (!steamSeeds.isEmpty()) {
                        steamRepository.saveAll(steamSeeds);
                        logger.info("[GET Service] Inserted {} zero-seeded steam records for {}.", steamSeeds.size(), financialYear);
                    }

                    // Final re-query to return the seeded data with real IDs
                    projections = repository.findOperationalHoursByPlantsAndYear(plantIds, financialYear);
                    logger.info("[GET Service] After zero-seed, re-query returned {} records for {}.", projections.size(), financialYear);
                } else {
                    logger.warn("[GET Service] No assets found in asset tables for plantIds: {}. Returning empty response.", plantIds);
                }
            }


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

    /**
     * Derives the previous financial year string.
     * Expected format: "YYYY-YY" (e.g. "2026-27" → "2025-26", "2025-26" → "2024-25").
     * Returns null if the format is unrecognised.
     */
    private String derivePreviousFinancialYear(String financialYear) {
        try {
            // Format: "2026-27"
            String[] parts = financialYear.split("-");
            if (parts.length != 2) return null;
            int startYear = Integer.parseInt(parts[0]);
            int prevStart = startYear - 1;
            int prevEnd = prevStart + 1;
            // Build suffix as last 2 digits
            String prevEndSuffix = String.format("%02d", prevEnd % 100);
            return prevStart + "-" + prevEndSuffix;
        } catch (Exception e) {
            logger.warn("[GET Service] Could not parse financial year '{}': {}", financialYear, e.getMessage());
            return null;
        }
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
            int powerSkipped = 0;
            int steamSkipped = 0;
            
            List<Map<String, String>> missingIdRecords = new ArrayList<>();

            // Process Power Operational Hours
            if (payload.getPowerResponse() != null) {
                logger.info("[POST Service] Processing {} power operational hours records", payload.getPowerResponse().size());
                for (CPPAssetOperationalHoursResponseDto dto : payload.getPowerResponse()) {
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
                    
                    logger.debug("[POST Service] Saving power asset - ID: {}, AssetFkId: {}", dto.getId(), dto.getAssetFkId());
                    boolean saved = savePowerOperationalHours(dto, financialYear);
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
                logger.info("[POST Service] No power operational hours to process");
            }

            // Process Steam Operational Hours
            if (payload.getSteamResponse() != null) {
                logger.info("[POST Service] Processing {} steam operational hours records", payload.getSteamResponse().size());
                for (CPPAssetOperationalHoursResponseDto dto : payload.getSteamResponse()) {
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
                    
                    logger.debug("[POST Service] Saving steam asset - ID: {}, AssetFkId: {}", dto.getId(), dto.getAssetFkId());
                    boolean saved = saveSteamOperationalHours(dto, financialYear);
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
                logger.info("[POST Service] No steam operational hours to process");
            }

            // Sync linked GT/HRSG operational hours
            int cascadeSynced = syncLinkedOperationalHours(payload, financialYear);
            if (cascadeSynced > 0) {
                logger.info("[POST Service] Cascade-synced {} linked operational hours records", cascadeSynced);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("powerAssetsSaved", powerSaved);
            data.put("steamAssetsSaved", steamSaved);
            data.put("totalSaved", powerSaved + steamSaved);
            data.put("powerAssetsSkipped", powerSkipped);
            data.put("steamAssetsSkipped", steamSkipped);
            data.put("totalSkipped", powerSkipped + steamSkipped);
            data.put("cascadeSynced", cascadeSynced);
            
            if (!missingIdRecords.isEmpty()) {
                data.put("skippedRecords", missingIdRecords);
            }

            if (missingIdRecords.isEmpty()) {
                aopMessageVM.setCode(200);
                aopMessageVM.setMessage("All operational hours saved successfully");
            } else {
                aopMessageVM.setCode(207); // 207 Multi-Status - partial success
                aopMessageVM.setMessage(String.format("Partial success: %d saved, %d skipped (missing or invalid IDs)", 
                    powerSaved + steamSaved, powerSkipped + steamSkipped));
            }
            
            aopMessageVM.setData(data);
            logger.info("[POST Service] Save operation completed - Saved: {} (Power: {}, Steam: {}), Skipped: {} (Power: {}, Steam: {})", 
                powerSaved + steamSaved, powerSaved, steamSaved, powerSkipped + steamSkipped, powerSkipped, steamSkipped);

        } catch (Exception e) {
            logger.error("[POST Service] Error saving operational hours: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to save operational hours: " + e.getMessage());
            aopMessageVM.setData(null);
        }

        return aopMessageVM;
    }

    private boolean savePowerOperationalHours(CPPAssetOperationalHoursResponseDto dto, String financialYear) {
        logger.debug("[POST Service - Power] Updating existing record with ID: {}", dto.getId());
        
        // Only update existing records - do not create new ones
        var optionalEntity = repository.findById(dto.getId());
        
        if (optionalEntity.isEmpty()) {
            logger.error("[POST Service - Power] Record with ID {} not found in database. Asset: {}", 
                dto.getId(), dto.getAssetName());
            return false;
        }
        
        CPPAssetOperationalHours entity = optionalEntity.get();
        entity.setModifiedDate(LocalDateTime.now());

        // Update only monthly operational hours (Apr to Mar) and remarks
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

        CPPAssetOperationalHours saved = repository.save(entity);
        logger.debug("[POST Service - Power] Successfully updated entity with ID: {} - Monthly hours and remarks updated", saved.getId());
        return true;
    }

    private boolean saveSteamOperationalHours(CPPAssetOperationalHoursResponseDto dto, String financialYear) {
        logger.debug("[POST Service - Steam] Updating existing record with ID: {}", dto.getId());
        
        // Only update existing records - do not create new ones
        var optionalEntity = steamRepository.findById(dto.getId());
        
        if (optionalEntity.isEmpty()) {
            logger.error("[POST Service - Steam] Record with ID {} not found in database. Asset: {}", 
                dto.getId(), dto.getAssetName());
            return false;
        }
        
        CPPSteamAssetsOperationalHours entity = optionalEntity.get();
        entity.setUpdatedDate(LocalDateTime.now());

        // Update only monthly operational hours (Apr to Mar) and remarks
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

        CPPSteamAssetsOperationalHours saved = steamRepository.save(entity);
        logger.debug("[POST Service - Steam] Successfully updated entity with ID: {} - Monthly hours and remarks updated", saved.getId());
        return true;
    }

    /**
     * Sync operational hours between linked GT (Power) and HRSG (Steam) assets.
     * If a GT is modified, all linked HRSGs get the same values.
     * If an HRSG is modified (without its GT), the GT and all sibling HRSGs get the same values.
     * Requires CPPSteamGenerationAsset.LinkedPowerAsset_FK_ID column.
     */
    private int syncLinkedOperationalHours(JMDOperationalHoursRequestDTO payload, String financialYear) {
        int cascadeCount = 0;
        Set<String> processedGroups = new HashSet<>();

        // Index modified power records by plantId:assetFkId
        Map<String, CPPAssetOperationalHoursResponseDto> modifiedPowerByKey = new HashMap<>();
        if (payload.getPowerResponse() != null) {
            for (CPPAssetOperationalHoursResponseDto dto : payload.getPowerResponse()) {
                if (dto.getAssetFkId() != null && dto.getPlantFkId() != null) {
                    modifiedPowerByKey.put(dto.getPlantFkId() + ":" + dto.getAssetFkId(), dto);
                }
            }
        }

        // Index modified steam records by plantId:assetFkId
        Map<String, CPPAssetOperationalHoursResponseDto> modifiedSteamByKey = new HashMap<>();
        if (payload.getSteamResponse() != null) {
            for (CPPAssetOperationalHoursResponseDto dto : payload.getSteamResponse()) {
                if (dto.getAssetFkId() != null && dto.getPlantFkId() != null) {
                    modifiedSteamByKey.put(dto.getPlantFkId() + ":" + dto.getAssetFkId(), dto);
                }
            }
        }

        // Gather all plantIds involved
        Set<UUID> plantIds = new HashSet<>();
        modifiedPowerByKey.values().forEach(d -> plantIds.add(d.getPlantFkId()));
        modifiedSteamByKey.values().forEach(d -> plantIds.add(d.getPlantFkId()));

        for (UUID plantId : plantIds) {
            // Load all GT operational hour records for this plant+year
            List<CPPAssetOperationalHours> gtRecords;
            try {
                gtRecords = repository.findByPlantFkIdAndAopYear(plantId, financialYear);
            } catch (Exception e) {
                logger.warn("[Sync] Could not load GT records for plant {} year {}: {}", plantId, financialYear, e.getMessage());
                continue;
            }

            for (CPPAssetOperationalHours gtRecord : gtRecords) {
                UUID gtAssetId = gtRecord.getAssetFkId();
                if (gtAssetId == null) continue;

                String groupKey = plantId + ":" + gtAssetId;
                if (processedGroups.contains(groupKey)) continue;

                // Find HRSGs linked to this GT via CPPSteamGenerationAsset.LinkedPowerAsset_FK_ID
                List<UUID> linkedHrsgIds;
                try {
                    linkedHrsgIds = jdbcTemplate.queryForList(
                            "SELECT AssetId FROM [dbo].[CPPSteamGenerationAsset] WITH(NOLOCK) " +
                            "WHERE LinkedPowerAsset_FK_ID = ? AND CPPPLANT_FK_Id = ?",
                            UUID.class, gtAssetId, plantId);
                } catch (Exception ex) {
                    logger.warn("[Sync] Could not query linked HRSGs for GT {} plant {}: {}", gtAssetId, plantId, ex.getMessage());
                    continue;
                }

                if (linkedHrsgIds == null || linkedHrsgIds.isEmpty()) continue;

                boolean gtModified = modifiedPowerByKey.containsKey(groupKey);
                boolean anyHrsgModified = linkedHrsgIds.stream()
                        .anyMatch(id -> modifiedSteamByKey.containsKey(plantId + ":" + id));

                // Skip this group if nothing in it was modified
                if (!gtModified && !anyHrsgModified) continue;
                processedGroups.add(groupKey);

                // Determine source-of-truth monthly values
                Double sourceApr, sourceMay, sourceJun, sourceJul, sourceAug, sourceSep;
                Double sourceOct, sourceNov, sourceDec, sourceJan, sourceFeb, sourceMar;
                String sourceRemarks;

                if (gtModified) {
                    CPPAssetOperationalHoursResponseDto src = modifiedPowerByKey.get(groupKey);
                    sourceApr = src.getApr(); sourceMay = src.getMay(); sourceJun = src.getJun();
                    sourceJul = src.getJul(); sourceAug = src.getAug(); sourceSep = src.getSep();
                    sourceOct = src.getOct(); sourceNov = src.getNov(); sourceDec = src.getDec();
                    sourceJan = src.getJan(); sourceFeb = src.getFeb(); sourceMar = src.getMar();
                    sourceRemarks = src.getRemarks();
                } else {
                    UUID srcHrsgId = linkedHrsgIds.stream()
                            .filter(id -> modifiedSteamByKey.containsKey(plantId + ":" + id))
                            .findFirst().orElse(null);
                    CPPAssetOperationalHoursResponseDto src = modifiedSteamByKey.get(plantId + ":" + srcHrsgId);
                    sourceApr = src.getApr(); sourceMay = src.getMay(); sourceJun = src.getJun();
                    sourceJul = src.getJul(); sourceAug = src.getAug(); sourceSep = src.getSep();
                    sourceOct = src.getOct(); sourceNov = src.getNov(); sourceDec = src.getDec();
                    sourceJan = src.getJan(); sourceFeb = src.getFeb(); sourceMar = src.getMar();
                    sourceRemarks = src.getRemarks();
                }

                // Update GT if it wasn't the source of truth
                if (!gtModified) {
                    gtRecord.setApr(sourceApr); gtRecord.setMay(sourceMay); gtRecord.setJun(sourceJun);
                    gtRecord.setJul(sourceJul); gtRecord.setAug(sourceAug); gtRecord.setSep(sourceSep);
                    gtRecord.setOct(sourceOct); gtRecord.setNov(sourceNov); gtRecord.setDec(sourceDec);
                    gtRecord.setJan(sourceJan); gtRecord.setFeb(sourceFeb); gtRecord.setMar(sourceMar);
                    gtRecord.setRemarks(sourceRemarks);
                    gtRecord.setModifiedDate(LocalDateTime.now());
                    repository.save(gtRecord);
                    cascadeCount++;
                    logger.info("[Sync] Updated GT {} to match linked HRSG values", gtAssetId);
                }

                // Update all linked HRSGs
                for (UUID hrsgId : linkedHrsgIds) {
                    Optional<CPPSteamAssetsOperationalHours> hrsgOpt =
                            steamRepository.findBySteamAssetFkIdAndPlantFkIdAndAopYear(hrsgId, plantId, financialYear);
                    if (hrsgOpt.isPresent()) {
                        CPPSteamAssetsOperationalHours hrsg = hrsgOpt.get();
                        hrsg.setApr(sourceApr); hrsg.setMay(sourceMay); hrsg.setJun(sourceJun);
                        hrsg.setJul(sourceJul); hrsg.setAug(sourceAug); hrsg.setSep(sourceSep);
                        hrsg.setOct(sourceOct); hrsg.setNov(sourceNov); hrsg.setDec(sourceDec);
                        hrsg.setJan(sourceJan); hrsg.setFeb(sourceFeb); hrsg.setMar(sourceMar);
                        hrsg.setRemarks(sourceRemarks);
                        hrsg.setUpdatedDate(LocalDateTime.now());
                        steamRepository.save(hrsg);
                        cascadeCount++;
                        logger.info("[Sync] Updated HRSG {} to match GT {} values", hrsgId, gtAssetId);
                    } else {
                        logger.warn("[Sync] No operational hours record found for HRSG {} plant {} year {}",
                                hrsgId, plantId, financialYear);
                    }
                }
            }
        }

        return cascadeCount;
    }

    @Override
    public byte[] exportPowerOperationalHours(List<UUID> plantIds, String financialYear) {
        logger.info("[Export Power] Exporting power operational hours for plantIds: {}, financialYear: {}", plantIds, financialYear);
        
        try {
            AOPMessageVM response = getOperationalHoursForPlants(plantIds, financialYear);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.getData();
            
            @SuppressWarnings("unchecked")
            List<CPPAssetOperationalHoursResponseDto> powerData = 
                (List<CPPAssetOperationalHoursResponseDto>) data.get("PowerOperationalHours");
            
            logger.info("[Export Power] Received {} power assets from GET service", powerData != null ? powerData.size() : 0);
            
            // Log all asset names for debugging
            if (powerData != null && !powerData.isEmpty()) {
                logger.debug("[Export Power] Asset list before sorting:");
                for (int i = 0; i < powerData.size(); i++) {
                    CPPAssetOperationalHoursResponseDto asset = powerData.get(i);
                    logger.debug("[Export Power]   [{}] Plant: {}, AssetType: {}, AssetName: {}, ID: {}", 
                        i + 1, asset.getPlantName(), asset.getAssetType(), asset.getAssetName(), asset.getId());
                }
            }
            
            // Sort by plantName first, then by assetType
            if (powerData != null && !powerData.isEmpty()) {
                int beforeSort = powerData.size();
                powerData.sort(Comparator
                    .comparing(CPPAssetOperationalHoursResponseDto::getPlantName, 
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(CPPAssetOperationalHoursResponseDto::getAssetType, 
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                int afterSort = powerData.size();
                logger.info("[Export Power] Sorted {} power assets by plantName and assetType (before: {}, after: {})", 
                    afterSort, beforeSort, afterSort);
                
                if (beforeSort != afterSort) {
                    logger.error("[Export Power] WARNING: Asset count changed during sorting! Before: {}, After: {}", 
                        beforeSort, afterSort);
                }
            }
            
            logger.info("[Export Power] Generating Excel for {} power assets", powerData != null ? powerData.size() : 0);
            
            return generateExcel(powerData, "Power Operational Hours", financialYear);
            
        } catch (Exception e) {
            logger.error("[Export Power] Error exporting power operational hours: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public byte[] exportSteamOperationalHours(List<UUID> plantIds, String financialYear) {
        logger.info("[Export Steam] Exporting steam operational hours for plantIds: {}, financialYear: {}", plantIds, financialYear);
        
        try {
            AOPMessageVM response = getOperationalHoursForPlants(plantIds, financialYear);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.getData();
            
            @SuppressWarnings("unchecked")
            List<CPPAssetOperationalHoursResponseDto> steamData = 
                (List<CPPAssetOperationalHoursResponseDto>) data.get("SteamOperationalHours");
            
            // Sort by plantName first, then by assetType
            if (steamData != null && !steamData.isEmpty()) {
                steamData.sort(Comparator
                    .comparing(CPPAssetOperationalHoursResponseDto::getPlantName, 
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(CPPAssetOperationalHoursResponseDto::getAssetType, 
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                logger.debug("[Export Steam] Sorted {} steam assets by plantName and assetType", steamData.size());
            }
            
            logger.info("[Export Steam] Generating Excel for {} steam assets", steamData != null ? steamData.size() : 0);
            
            return generateExcel(steamData, "Steam Operational Hours", financialYear);
            
        } catch (Exception e) {
            logger.error("[Export Steam] Error exporting steam operational hours: {}", e.getMessage(), e);
            return null;
        }
    }

    private byte[] generateExcel(List<CPPAssetOperationalHoursResponseDto> dataList, String sheetName, String financialYear) throws Exception {
        logger.info("[Excel Generation] Creating workbook for sheet: {} with {} records", sheetName, dataList != null ? dataList.size() : 0);
        
        if (dataList != null && !dataList.isEmpty()) {
            logger.debug("[Excel Generation] Assets to be written to Excel:");
            for (int i = 0; i < dataList.size(); i++) {
                CPPAssetOperationalHoursResponseDto asset = dataList.get(i);
                logger.debug("[Excel Generation]   [{}] Plant: {}, AssetType: {}, AssetName: {}", 
                    i + 1, asset.getPlantName(), asset.getAssetType(), asset.getAssetName());
            }
        }
        
        // Calculate total available hours per month based on financial year
        Map<String, Double> totalHoursByMonth = calculateTotalAvailableHours(financialYear);
        
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle remarksStyle = createRemarksStyle(workbook);

        String startYearSuffix = financialYear.substring(2, 4);
        String endYearSuffix = financialYear.substring(5, 7);
        
        int currentRow = 0;
        int col = 0;

        // Create top header row (Row 0) with merged cells for months
        Row topHeaderRow = sheet.createRow(currentRow++);
        col = 0;
        
        // Static columns that span both rows
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Asset Name", headerStyle);
        col++;
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Asset Type", headerStyle);
        col++;
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Plant Name", headerStyle);
        col++;
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Utility Distributed", headerStyle);
        col++;
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Distributed SAP Code", headerStyle);
        col++;
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Utility Generated", headerStyle);
        col++;
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Generated Utility Code", headerStyle);
        col++;
        
        // Month headers (each month spans 2 columns: Shut Down Hrs, Operational Hrs)
        String[] months = {"Apr-" + startYearSuffix, "May-" + startYearSuffix, "Jun-" + startYearSuffix, "Jul-" + startYearSuffix,
                "Aug-" + startYearSuffix, "Sep-" + startYearSuffix, "Oct-" + startYearSuffix, "Nov-" + startYearSuffix,
                "Dec-" + startYearSuffix, "Jan-" + endYearSuffix, "Feb-" + endYearSuffix, "Mar-" + endYearSuffix};
        
        int monthStartCol = col;
        for (String month : months) {
            createMergedHeaderCell(sheet, topHeaderRow, 0, 0, col, col + 1, month, headerStyle);
            col += 2;
        }
        
        int remarksCol = col;
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Remarks", headerStyle);
        col++;
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "id", headerStyle);
        int idCol = col;
        col++;
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "assetFkId", headerStyle);
        int assetFkIdCol = col;
        col++;
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "assetCategory", headerStyle);
        int assetCategoryCol = col;
        col++;
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "dataHash", headerStyle);
        int dataHashCol = col;
        col++;
        
        int totalColumns = col;
        
        // Create sub-header row (Row 1) for month details
        Row subHeaderRow = sheet.createRow(currentRow++);
        col = monthStartCol;
        
        // Sub-headers for each month (Shut Down Hrs, Operational Hrs)
        for (int i = 0; i < 12; i++) {
            Cell cell = subHeaderRow.createCell(col++);
            cell.setCellValue("Shut Down Hrs");
            cell.setCellStyle(headerStyle);
            
            cell = subHeaderRow.createCell(col++);
            cell.setCellValue("Operational Hrs");
            cell.setCellStyle(headerStyle);
        }
        
        // Create styles for locked and unlocked cells
        CellStyle lockedStyle = createLockedCellStyle(workbook);
        CellStyle unlockedStyle = createUnlockedCellStyle(workbook);
        CellStyle editableRemarksStyle = createEditableRemarksStyle(workbook);
        
        // Data rows
        int rowCount = 0;
        for (CPPAssetOperationalHoursResponseDto dto : dataList) {
            rowCount++;
            int excelRowNum = currentRow + 1; // Excel row number (1-indexed)
            Row row = sheet.createRow(currentRow++);
            col = 0;
            logger.debug("[Excel Generation] Writing row {} for asset: {}", rowCount, dto.getAssetName());

            createCell(row, col++, dto.getAssetName(), dataStyle);
            createCell(row, col++, dto.getAssetType(), dataStyle);
            createCell(row, col++, dto.getPlantName(), dataStyle);
            createCell(row, col++, dto.getUtilityDistributed(), dataStyle);
            createCell(row, col++, dto.getDistributedSapCode(), dataStyle);
            createCell(row, col++, dto.getUtilityGenerated(), dataStyle);
            createCell(row, col++, dto.getGeneratedUtilityCode(), dataStyle);
            
            // Monthly hours with formulas (convert operational to shutdown for export)
            setMonthCellValuesWithFormulas(row, col, convertOperationalToShutdown(dto.getApr(), totalHoursByMonth.get("apr")), totalHoursByMonth.get("apr"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, convertOperationalToShutdown(dto.getMay(), totalHoursByMonth.get("may")), totalHoursByMonth.get("may"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, convertOperationalToShutdown(dto.getJun(), totalHoursByMonth.get("jun")), totalHoursByMonth.get("jun"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, convertOperationalToShutdown(dto.getJul(), totalHoursByMonth.get("jul")), totalHoursByMonth.get("jul"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, convertOperationalToShutdown(dto.getAug(), totalHoursByMonth.get("aug")), totalHoursByMonth.get("aug"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, convertOperationalToShutdown(dto.getSep(), totalHoursByMonth.get("sep")), totalHoursByMonth.get("sep"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, convertOperationalToShutdown(dto.getOct(), totalHoursByMonth.get("oct")), totalHoursByMonth.get("oct"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, convertOperationalToShutdown(dto.getNov(), totalHoursByMonth.get("nov")), totalHoursByMonth.get("nov"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, convertOperationalToShutdown(dto.getDec(), totalHoursByMonth.get("dec")), totalHoursByMonth.get("dec"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, convertOperationalToShutdown(dto.getJan(), totalHoursByMonth.get("jan")), totalHoursByMonth.get("jan"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, convertOperationalToShutdown(dto.getFeb(), totalHoursByMonth.get("feb")), totalHoursByMonth.get("feb"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, convertOperationalToShutdown(dto.getMar(), totalHoursByMonth.get("mar")), totalHoursByMonth.get("mar"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            
            createCell(row, col++, dto.getRemarks(), editableRemarksStyle);
            createCell(row, col++, dto.getId() != null ? dto.getId().toString() : "", dataStyle);
            createCell(row, col++, dto.getAssetFkId() != null ? dto.getAssetFkId().toString() : "", dataStyle);
            createCell(row, col++, dto.getAssetCategory(), dataStyle);
            
            // Generate and store hashcode from operational hours + remarks
            String dataHash = generateOperationalHoursHash(dto);
            createCell(row, col++, dataHash, dataStyle);
        }

        // Hide ID columns and hashcode column
        sheet.setColumnHidden(idCol, true);
        sheet.setColumnHidden(assetFkIdCol, true);
        sheet.setColumnHidden(assetCategoryCol, true);
        sheet.setColumnHidden(dataHashCol, true);

        // Auto-size columns
        for (int i = 0; i < totalColumns; i++) {
            if (i == remarksCol) {
                sheet.setColumnWidth(i, 8000);
                continue;
            }
            sheet.autoSizeColumn(i);
        }
        
        // Protect sheet to enforce locked cells
        sheet.protectSheet("");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        logger.info("[Excel Generation] Successfully generated Excel with {} data rows (expected: {}, actual rows written: {})", 
            dataList.size(), dataList.size(), rowCount);
        
        if (dataList.size() != rowCount) {
            logger.error("[Excel Generation] ERROR: Mismatch between expected rows ({}) and actual rows written ({})", 
                dataList.size(), rowCount);
        }
        
        return outputStream.toByteArray();
    }
    
    /**
     * Generate error Excel file with Status and Comment columns for failed records
     */
    private byte[] generateErrorExcel(List<CPPAssetOperationalHoursResponseDto> dataList, List<String> failureReasons, String sheetName, String financialYear) throws Exception {
        logger.info("[Error Excel Generation] Creating error workbook for sheet: {} with {} failed records", sheetName, dataList != null ? dataList.size() : 0);
        
        Map<String, Double> totalHoursByMonth = calculateTotalAvailableHours(financialYear);
        
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle remarksStyle = createRemarksStyle(workbook);
        CellStyle errorStyle = createErrorCellStyle(workbook);

        String startYearSuffix = financialYear.substring(2, 4);
        String endYearSuffix = financialYear.substring(5, 7);
        
        int currentRow = 0;
        int col = 0;

        // Create top header row (Row 0) with merged cells for months
        Row topHeaderRow = sheet.createRow(currentRow++);
        col = 0;
        
        // Static columns that span both rows
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Asset Name", headerStyle);
        col++;
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Asset Type", headerStyle);
        col++;
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Plant Name", headerStyle);
        col++;
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Utility Distributed", headerStyle);
        col++;
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Distributed SAP Code", headerStyle);
        col++;
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Utility Generated", headerStyle);
        col++;
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Generated Utility Code", headerStyle);
        col++;
        
        // Month headers
        String[] months = {"Apr-" + startYearSuffix, "May-" + startYearSuffix, "Jun-" + startYearSuffix, "Jul-" + startYearSuffix,
                "Aug-" + startYearSuffix, "Sep-" + startYearSuffix, "Oct-" + startYearSuffix, "Nov-" + startYearSuffix,
                "Dec-" + startYearSuffix, "Jan-" + endYearSuffix, "Feb-" + endYearSuffix, "Mar-" + endYearSuffix};
        
        int monthStartCol = col;
        for (String month : months) {
            createMergedHeaderCell(sheet, topHeaderRow, 0, 0, col, col + 1, month, headerStyle);
            col += 2;
        }
        
        int remarksCol = col;
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Remarks", headerStyle);
        col++;
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "id", headerStyle);
        int idCol = col;
        col++;
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "assetFkId", headerStyle);
        int assetFkIdCol = col;
        col++;
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "assetCategory", headerStyle);
        int assetCategoryCol = col;
        col++;
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "dataHash", headerStyle);
        int dataHashCol = col;
        col++;
        
        // Add Status and Comment columns for error file
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Status", headerStyle);
        int statusCol = col;
        col++;
        createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Comment", headerStyle);
        int commentCol = col;
        col++;
        
        int totalColumns = col;
        
        // Create sub-header row (Row 1) for month details
        Row subHeaderRow = sheet.createRow(currentRow++);
        col = monthStartCol;
        
        for (int i = 0; i < 12; i++) {
            Cell cell = subHeaderRow.createCell(col++);
            cell.setCellValue("Shut Down Hrs");
            cell.setCellStyle(headerStyle);
            
            cell = subHeaderRow.createCell(col++);
            cell.setCellValue("Operational Hrs");
            cell.setCellStyle(headerStyle);
        }
        
        // Data rows
        int rowCount = 0;
        for (int i = 0; i < dataList.size(); i++) {
            CPPAssetOperationalHoursResponseDto dto = dataList.get(i);
            String errorComment = failureReasons.get(i);
            rowCount++;
            int excelRowNum = currentRow + 1;
            Row row = sheet.createRow(currentRow++);
            col = 0;
            logger.debug("[Error Excel Generation] Writing error row {} for asset: {} - {}", rowCount, dto.getAssetName(), errorComment);

            createCell(row, col++, dto.getAssetName(), dataStyle);
            createCell(row, col++, dto.getAssetType(), dataStyle);
            createCell(row, col++, dto.getPlantName(), dataStyle);
            createCell(row, col++, dto.getUtilityDistributed(), dataStyle);
            createCell(row, col++, dto.getDistributedSapCode(), dataStyle);
            createCell(row, col++, dto.getUtilityGenerated(), dataStyle);
            createCell(row, col++, dto.getGeneratedUtilityCode(), dataStyle);
            
            // Monthly hours
            setMonthCellValuesWithFormulas(row, col, dto.getApr(), totalHoursByMonth.get("apr"), excelRowNum, dataStyle, dataStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, dto.getMay(), totalHoursByMonth.get("may"), excelRowNum, dataStyle, dataStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, dto.getJun(), totalHoursByMonth.get("jun"), excelRowNum, dataStyle, dataStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, dto.getJul(), totalHoursByMonth.get("jul"), excelRowNum, dataStyle, dataStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, dto.getAug(), totalHoursByMonth.get("aug"), excelRowNum, dataStyle, dataStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, dto.getSep(), totalHoursByMonth.get("sep"), excelRowNum, dataStyle, dataStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, dto.getOct(), totalHoursByMonth.get("oct"), excelRowNum, dataStyle, dataStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, dto.getNov(), totalHoursByMonth.get("nov"), excelRowNum, dataStyle, dataStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, dto.getDec(), totalHoursByMonth.get("dec"), excelRowNum, dataStyle, dataStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, dto.getJan(), totalHoursByMonth.get("jan"), excelRowNum, dataStyle, dataStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, dto.getFeb(), totalHoursByMonth.get("feb"), excelRowNum, dataStyle, dataStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, dto.getMar(), totalHoursByMonth.get("mar"), excelRowNum, dataStyle, dataStyle);
            col += 2;
            
            createCell(row, col++, dto.getRemarks(), remarksStyle);
            createCell(row, col++, dto.getId() != null ? dto.getId().toString() : "", dataStyle);
            createCell(row, col++, dto.getAssetFkId() != null ? dto.getAssetFkId().toString() : "", dataStyle);
            createCell(row, col++, dto.getAssetCategory(), dataStyle);
            
            // Generate and store hashcode
            String dataHash = generateOperationalHoursHash(dto);
            createCell(row, col++, dataHash, dataStyle);
            
            // Status column - mark as Failed
            createCell(row, col++, "Failed", errorStyle);
            
            // Comment column - show failure reason
            createCell(row, col++, errorComment, errorStyle);
        }

        // Hide ID columns and hashcode column
        sheet.setColumnHidden(idCol, true);
        sheet.setColumnHidden(assetFkIdCol, true);
        sheet.setColumnHidden(assetCategoryCol, true);
        sheet.setColumnHidden(dataHashCol, true);

        // Auto-size columns
        for (int i = 0; i < totalColumns; i++) {
            if (i == remarksCol || i == commentCol) {
                sheet.setColumnWidth(i, 8000);
                continue;
            }
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        logger.info("[Error Excel Generation] Successfully generated error Excel with {} failed records", dataList.size());
        
        return outputStream.toByteArray();
    }
    
    private Map<String, Double> calculateTotalAvailableHours(String financialYear) {
        Map<String, Double> totalHours = new LinkedHashMap<>();
        
        String[] parts = financialYear.split("-");
        int startYear = Integer.parseInt(parts[0]);
        int endYear = startYear + 1;
        
        totalHours.put("apr", (double) getDaysInMonth(4, startYear) * 24);
        totalHours.put("may", (double) getDaysInMonth(5, startYear) * 24);
        totalHours.put("jun", (double) getDaysInMonth(6, startYear) * 24);
        totalHours.put("jul", (double) getDaysInMonth(7, startYear) * 24);
        totalHours.put("aug", (double) getDaysInMonth(8, startYear) * 24);
        totalHours.put("sep", (double) getDaysInMonth(9, startYear) * 24);
        totalHours.put("oct", (double) getDaysInMonth(10, startYear) * 24);
        totalHours.put("nov", (double) getDaysInMonth(11, startYear) * 24);
        totalHours.put("dec", (double) getDaysInMonth(12, startYear) * 24);
        totalHours.put("jan", (double) getDaysInMonth(1, endYear) * 24);
        totalHours.put("feb", (double) getDaysInMonth(2, endYear) * 24);
        totalHours.put("mar", (double) getDaysInMonth(3, endYear) * 24);
        
        return totalHours;
    }
    
    private int getDaysInMonth(int month, int year) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(year, month - 1, 1);
        return calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
    }
    

    private void createMergedHeaderCell(Sheet sheet, Row row, int rowStart, int rowEnd, 
                                       int colStart, int colEnd, String value, CellStyle style) {
        if (rowStart != rowEnd || colStart != colEnd) {
            sheet.addMergedRegion(new CellRangeAddress(rowStart, rowEnd, colStart, colEnd));
        }
        
        Cell cell = row.createCell(colStart);
        cell.setCellValue(value);
        cell.setCellStyle(style);
        
        for (int r = rowStart; r <= rowEnd; r++) {
            Row currentRow = sheet.getRow(r);
            if (currentRow == null) {
                currentRow = sheet.createRow(r);
            }
            for (int c = colStart; c <= colEnd; c++) {
                Cell currentCell = currentRow.getCell(c);
                if (currentCell == null) {
                    currentCell = currentRow.createCell(c);
                }
                currentCell.setCellStyle(style);
            }
        }
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void setNumericCell(Row row, int col, Double value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value);
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createRemarksStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setWrapText(true);
        return style;
    }
    
    private CellStyle createEditableRemarksStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setWrapText(true);
        style.setLocked(false);
        return style;
    }
    
    private CellStyle createLockedCellStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setLocked(true);
        return style;
    }
    
    private CellStyle createUnlockedCellStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setLocked(false);
        return style;
    }
    
    private CellStyle createErrorCellStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        Font font = workbook.createFont();
        font.setColor(IndexedColors.RED.getIndex());
        font.setBold(true);
        style.setFont(font);
        style.setWrapText(true);
        return style;
    }
    
    private void setMonthCellValuesWithFormulas(Row row, int startCol, Double shutdownHours, Double totalHours, int rowNum, CellStyle unlockedStyle, CellStyle lockedStyle) {
        // Shutdown Hrs cell - editable, with validation
        Cell shutdownCell = row.createCell(startCol);
        if (shutdownHours != null) {
            shutdownCell.setCellValue(shutdownHours);
        } else {
            shutdownCell.setCellValue("");
        }
        shutdownCell.setCellStyle(unlockedStyle);
        
        // Operational Hrs cell - formula-based, locked
        Cell operationalCell = row.createCell(startCol + 1);
        String colLetter = getCellColumnLetter(startCol);
        // Formula: Operational Hrs = Total Available Hours - Shutdown Hrs
        // Without = sign (POI adds it automatically)
        String formula = String.valueOf(totalHours != null ? totalHours.intValue() : 0) + "-" + colLetter + rowNum;
        operationalCell.setCellFormula(formula);
        operationalCell.setCellStyle(lockedStyle);
    }
    
    private String getCellColumnLetter(int col) {
        StringBuilder result = new StringBuilder();
        col++; // Convert to 1-based indexing
        while (col > 0) {
            col--;
            result.insert(0, (char) ('A' + col % 26));
            col /= 26;
        }
        return result.toString();
    }
    
    /**
     * Generate hashcode from operational hours data (Apr-Mar) and remarks
     * This is used to detect if data has changed during import
     */
    private String generateOperationalHoursHash(CPPAssetOperationalHoursResponseDto dto) {
        try {
            StringBuilder dataToHash = new StringBuilder();
            
            // Append all monthly operational hours
            dataToHash.append(dto.getApr() != null ? dto.getApr().toString() : "null").append("|");
            dataToHash.append(dto.getMay() != null ? dto.getMay().toString() : "null").append("|");
            dataToHash.append(dto.getJun() != null ? dto.getJun().toString() : "null").append("|");
            dataToHash.append(dto.getJul() != null ? dto.getJul().toString() : "null").append("|");
            dataToHash.append(dto.getAug() != null ? dto.getAug().toString() : "null").append("|");
            dataToHash.append(dto.getSep() != null ? dto.getSep().toString() : "null").append("|");
            dataToHash.append(dto.getOct() != null ? dto.getOct().toString() : "null").append("|");
            dataToHash.append(dto.getNov() != null ? dto.getNov().toString() : "null").append("|");
            dataToHash.append(dto.getDec() != null ? dto.getDec().toString() : "null").append("|");
            dataToHash.append(dto.getJan() != null ? dto.getJan().toString() : "null").append("|");
            dataToHash.append(dto.getFeb() != null ? dto.getFeb().toString() : "null").append("|");
            dataToHash.append(dto.getMar() != null ? dto.getMar().toString() : "null").append("|");
            
            // Append remarks
            dataToHash.append(dto.getRemarks() != null ? dto.getRemarks() : "null");
            
            // Generate SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(dataToHash.toString().getBytes("UTF-8"));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            logger.error("[Hash Generation] Error generating hash: {}", e.getMessage(), e);
            return "";
        }
    }
    
    /**
     * Check if the imported record has been modified compared to DB
     * Returns true if data or remarks have changed
     */
    private boolean isRecordModified(CPPAssetOperationalHoursResponseDto dto, Map<String, Double> totalHoursByMonth) {
        try {
            if (dto.getId() == null) {
                return true; // New record, treat as modified
            }

            Double dbApr = null, dbMay = null, dbJun = null, dbJul = null, dbAug = null, dbSep = null,
                   dbOct = null, dbNov = null, dbDec = null, dbJan = null, dbFeb = null, dbMar = null;
            String dbRemarks = null;

            if ("Steam".equals(dto.getAssetCategory())) {
                var optionalEntity = steamRepository.findById(dto.getId());
                if (optionalEntity.isEmpty()) {
                    return true;
                }
                CPPSteamAssetsOperationalHours dbEntity = optionalEntity.get();
                dbApr = dbEntity.getApr();
                dbMay = dbEntity.getMay();
                dbJun = dbEntity.getJun();
                dbJul = dbEntity.getJul();
                dbAug = dbEntity.getAug();
                dbSep = dbEntity.getSep();
                dbOct = dbEntity.getOct();
                dbNov = dbEntity.getNov();
                dbDec = dbEntity.getDec();
                dbJan = dbEntity.getJan();
                dbFeb = dbEntity.getFeb();
                dbMar = dbEntity.getMar();
                dbRemarks = dbEntity.getRemarks();
            } else {
                var optionalEntity = repository.findById(dto.getId());
                if (optionalEntity.isEmpty()) {
                    return true;
                }
                CPPAssetOperationalHours dbEntity = optionalEntity.get();
                dbApr = dbEntity.getApr();
                dbMay = dbEntity.getMay();
                dbJun = dbEntity.getJun();
                dbJul = dbEntity.getJul();
                dbAug = dbEntity.getAug();
                dbSep = dbEntity.getSep();
                dbOct = dbEntity.getOct();
                dbNov = dbEntity.getNov();
                dbDec = dbEntity.getDec();
                dbJan = dbEntity.getJan();
                dbFeb = dbEntity.getFeb();
                dbMar = dbEntity.getMar();
                dbRemarks = dbEntity.getRemarks();
            }

            // Create DTO from DB entity (DB stores operational hours)
            CPPAssetOperationalHoursResponseDto dbDto = new CPPAssetOperationalHoursResponseDto();
            dbDto.setApr(dbApr);
            dbDto.setMay(dbMay);
            dbDto.setJun(dbJun);
            dbDto.setJul(dbJul);
            dbDto.setAug(dbAug);
            dbDto.setSep(dbSep);
            dbDto.setOct(dbOct);
            dbDto.setNov(dbNov);
            dbDto.setDec(dbDec);
            dbDto.setJan(dbJan);
            dbDto.setFeb(dbFeb);
            dbDto.setMar(dbMar);
            dbDto.setRemarks(dbRemarks);

            // Create a copy of imported DTO and convert shutdown hours to operational hours for fair comparison
            // DB stores operational hours, but Excel/import DTO contains shutdown hours
            CPPAssetOperationalHoursResponseDto importedDtoForHash = new CPPAssetOperationalHoursResponseDto();
            importedDtoForHash.setApr(convertShutdownToOperational(dto.getApr(), totalHoursByMonth.get("apr")));
            importedDtoForHash.setMay(convertShutdownToOperational(dto.getMay(), totalHoursByMonth.get("may")));
            importedDtoForHash.setJun(convertShutdownToOperational(dto.getJun(), totalHoursByMonth.get("jun")));
            importedDtoForHash.setJul(convertShutdownToOperational(dto.getJul(), totalHoursByMonth.get("jul")));
            importedDtoForHash.setAug(convertShutdownToOperational(dto.getAug(), totalHoursByMonth.get("aug")));
            importedDtoForHash.setSep(convertShutdownToOperational(dto.getSep(), totalHoursByMonth.get("sep")));
            importedDtoForHash.setOct(convertShutdownToOperational(dto.getOct(), totalHoursByMonth.get("oct")));
            importedDtoForHash.setNov(convertShutdownToOperational(dto.getNov(), totalHoursByMonth.get("nov")));
            importedDtoForHash.setDec(convertShutdownToOperational(dto.getDec(), totalHoursByMonth.get("dec")));
            importedDtoForHash.setJan(convertShutdownToOperational(dto.getJan(), totalHoursByMonth.get("jan")));
            importedDtoForHash.setFeb(convertShutdownToOperational(dto.getFeb(), totalHoursByMonth.get("feb")));
            importedDtoForHash.setMar(convertShutdownToOperational(dto.getMar(), totalHoursByMonth.get("mar")));
            importedDtoForHash.setRemarks(dto.getRemarks());

            // Compare hashes
            String dbHash = generateOperationalHoursHash(dbDto);
            String importedHash = generateOperationalHoursHash(importedDtoForHash);

            boolean modified = !dbHash.equals(importedHash);
            if (!modified) {
                logger.debug("[isRecordModified] Record {} unchanged - hash match", dto.getId());
            } else {
                logger.debug("[isRecordModified] Record {} modified - hash mismatch", dto.getId());
            }
            return modified;
        } catch (Exception e) {
            logger.error("[isRecordModified] Error checking if record modified for ID {}: {}", dto.getId(), e.getMessage());
            return true; // On error, assume modified to be safe
        }
    }

    private Double convertShutdownToOperational(Double shutdownHours, Double totalHours) {
        if (shutdownHours == null || totalHours == null) {
            return null;
        }
        return totalHours - shutdownHours;
    }
    
    @Override
    public AOPMessageVM importPowerOperationalHours(List<UUID> plantIds, String financialYear, MultipartFile file) {
        logger.info("[Import Power Operational Hours] Starting import for plantIds: {}, financialYear: {}", plantIds, financialYear);
        
        AOPMessageVM response = new AOPMessageVM();
        try {
            // Read Excel file and parse shutdown hours
            List<CPPAssetOperationalHoursResponseDto> excelData = readOperationalHoursExcelJMD(file.getInputStream(), financialYear);
            logger.info("[Import Power Operational Hours] Read {} records from Excel", excelData.size());
            
            // Validate and calculate operational hours
            Map<String, Double> totalHoursByMonth = calculateTotalAvailableHours(financialYear);
            List<CPPAssetOperationalHoursResponseDto> validRecords = new ArrayList<>();
            List<CPPAssetOperationalHoursResponseDto> failedRecords = new ArrayList<>();
            List<String> failureReasons = new ArrayList<>();
            int skippedCount = 0;
            
            for (CPPAssetOperationalHoursResponseDto dto : excelData) {
                // FIRST: Check if record was actually modified by user
                // Skip unchanged records - they don't need validation or saving
                if (!isRecordModified(dto, totalHoursByMonth)) {
                    skippedCount++;
                    logger.debug("[Import Power Operational Hours] Skipping unchanged record: {}", dto.getAssetName());
                    continue;
                }
                
                // SECOND: Only validate records that were actually modified
                String validationError = validateShutdownHoursData(dto, totalHoursByMonth);
                if (validationError != null) {
                    failedRecords.add(dto);
                    failureReasons.add(validationError);
                    logger.warn("[Import Power Operational Hours] Invalid record - {}: {}", dto.getAssetName(), validationError);
                } else {
                    // Clone the DTO before converting to operational hours
                    CPPAssetOperationalHoursResponseDto recordToSave = cloneDto(dto);
                    calculateOperationalHoursFromShutdown(recordToSave, totalHoursByMonth);
                    validRecords.add(recordToSave);
                }
            }
            
            logger.info("[Import Power Operational Hours] {} records unchanged (skipped), {} modified records to process", skippedCount, excelData.size() - skippedCount);
            
            // Try to save valid records and track any that fail during save
            if (!validRecords.isEmpty()) {
                try {
                    JMDOperationalHoursRequestDTO payload = new JMDOperationalHoursRequestDTO();
                    payload.setPowerResponse(validRecords);
                    AOPMessageVM saveResult = saveOperationalHours(plantIds, financialYear, payload);
                    
                    // Check if any records were skipped during save (missing IDs)
                    if (saveResult.getCode() == 207) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> saveData = (Map<String, Object>) saveResult.getData();
                        if (saveData != null && saveData.containsKey("skippedRecords")) {
                            @SuppressWarnings("unchecked")
                            List<String> skippedRecords = (List<String>) saveData.get("skippedRecords");
                            logger.warn("[Import Power Operational Hours] {} records skipped during save", skippedRecords.size());
                        }
                    }
                    
                    logger.info("[Import Power Operational Hours] Successfully processed {} records", validRecords.size());
                } catch (Exception e) {
                    logger.error("[Import Power Operational Hours] Error saving records: {}", e.getMessage(), e);
                    // Mark all valid records as failed if save fails, converting operational back to shutdown hours
                    for (CPPAssetOperationalHoursResponseDto failedDto : validRecords) {
                        CPPAssetOperationalHoursResponseDto originalDto = cloneDto(failedDto);
                        originalDto.setApr(convertShutdownToOperational(failedDto.getApr(), totalHoursByMonth.get("apr")));
                        originalDto.setMay(convertShutdownToOperational(failedDto.getMay(), totalHoursByMonth.get("may")));
                        originalDto.setJun(convertShutdownToOperational(failedDto.getJun(), totalHoursByMonth.get("jun")));
                        originalDto.setJul(convertShutdownToOperational(failedDto.getJul(), totalHoursByMonth.get("jul")));
                        originalDto.setAug(convertShutdownToOperational(failedDto.getAug(), totalHoursByMonth.get("aug")));
                        originalDto.setSep(convertShutdownToOperational(failedDto.getSep(), totalHoursByMonth.get("sep")));
                        originalDto.setOct(convertShutdownToOperational(failedDto.getOct(), totalHoursByMonth.get("oct")));
                        originalDto.setNov(convertShutdownToOperational(failedDto.getNov(), totalHoursByMonth.get("nov")));
                        originalDto.setDec(convertShutdownToOperational(failedDto.getDec(), totalHoursByMonth.get("dec")));
                        originalDto.setJan(convertShutdownToOperational(failedDto.getJan(), totalHoursByMonth.get("jan")));
                        originalDto.setFeb(convertShutdownToOperational(failedDto.getFeb(), totalHoursByMonth.get("feb")));
                        originalDto.setMar(convertShutdownToOperational(failedDto.getMar(), totalHoursByMonth.get("mar")));
                        failedRecords.add(originalDto);
                        failureReasons.add("Save failed: " + e.getMessage());
                    }
                }
            }
            
            // Prepare response
            if (failedRecords.isEmpty()) {
                response.setCode(200);
                if (validRecords.isEmpty() && skippedCount > 0) {
                    response.setMessage("No changes detected in imported records. All " + skippedCount + " records unchanged.");
                } else {
                    response.setMessage("All power operational hours imported successfully. " + skippedCount + " records unchanged, " + validRecords.size() + " records updated.");
                }
            } else {
                // Export only failed records to Excel file with Status and Comment columns
                byte[] failedRecordsFile = generateErrorExcel(failedRecords, failureReasons, "Power Operational Hours", financialYear);
                String base64File = java.util.Base64.getEncoder().encodeToString(failedRecordsFile);
                response.setCode(400);
                response.setMessage("Partial import: " + validRecords.size() + " saved, " + failedRecords.size() + " failed, " + skippedCount + " unchanged. Download file for details.");
                response.setData(base64File);
                logger.info("[Import Power Operational Hours] Exported {} failed records to Excel", failedRecords.size());
            }
            
            logger.info("[Import Power Operational Hours] Import completed - Unchanged: {}, Saved: {}, Failed: {}", skippedCount, validRecords.size(), failedRecords.size());
        } catch (Exception e) {
            logger.error("[Import Power Operational Hours] Error during import: {}", e.getMessage(), e);
            response.setCode(500);
            response.setMessage("Failed to import power operational hours: " + e.getMessage());
        }
        return response;
    }
    
    @Override
    public AOPMessageVM importSteamOperationalHours(List<UUID> plantIds, String financialYear, MultipartFile file) {
        logger.info("[Import Steam Operational Hours] Starting import for plantIds: {}, financialYear: {}", plantIds, financialYear);
        
        AOPMessageVM response = new AOPMessageVM();
        try {
            // Read Excel file and parse shutdown hours
            List<CPPAssetOperationalHoursResponseDto> excelData = readOperationalHoursExcelJMD(file.getInputStream(), financialYear);
            logger.info("[Import Steam Operational Hours] Read {} records from Excel", excelData.size());
            
            // Validate and calculate operational hours
            Map<String, Double> totalHoursByMonth = calculateTotalAvailableHours(financialYear);
            List<CPPAssetOperationalHoursResponseDto> validRecords = new ArrayList<>();
            List<CPPAssetOperationalHoursResponseDto> failedRecords = new ArrayList<>();
            List<String> failureReasons = new ArrayList<>();
            
            int skippedCount = 0;
            
            for (CPPAssetOperationalHoursResponseDto dto : excelData) {
                // FIRST: Check if record was actually modified by user
                // Skip unchanged records - they don't need validation or saving
                if (!isRecordModified(dto, totalHoursByMonth)) {
                    skippedCount++;
                    logger.debug("[Import Steam Operational Hours] Skipping unchanged record: {}", dto.getAssetName());
                    continue;
                }
                
                // SECOND: Only validate records that were actually modified
                String validationError = validateShutdownHoursData(dto, totalHoursByMonth);
                if (validationError != null) {
                    failedRecords.add(dto);
                    failureReasons.add(validationError);
                    logger.warn("[Import Steam Operational Hours] Invalid record - {}: {}", dto.getAssetName(), validationError);
                } else {
                    // Clone the DTO before converting to operational hours
                    CPPAssetOperationalHoursResponseDto recordToSave = cloneDto(dto);
                    calculateOperationalHoursFromShutdown(recordToSave, totalHoursByMonth);
                    validRecords.add(recordToSave);
                }
            }
            
            logger.info("[Import Steam Operational Hours] {} records unchanged (skipped), {} modified records to process", skippedCount, excelData.size() - skippedCount);
            
            // Try to save valid records and track any that fail during save
            if (!validRecords.isEmpty()) {
                try {
                    JMDOperationalHoursRequestDTO payload = new JMDOperationalHoursRequestDTO();
                    payload.setSteamResponse(validRecords);
                    AOPMessageVM saveResult = saveOperationalHours(plantIds, financialYear, payload);
                    
                    // Check if any records were skipped during save (missing IDs)
                    if (saveResult.getCode() == 207) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> saveData = (Map<String, Object>) saveResult.getData();
                        if (saveData != null && saveData.containsKey("skippedRecords")) {
                            @SuppressWarnings("unchecked")
                            List<String> skippedRecords = (List<String>) saveData.get("skippedRecords");
                            logger.warn("[Import Steam Operational Hours] {} records skipped during save", skippedRecords.size());
                        }
                    }
                    
                    logger.info("[Import Steam Operational Hours] Successfully processed {} records", validRecords.size());
                } catch (Exception e) {
                    logger.error("[Import Steam Operational Hours] Error saving records: {}", e.getMessage(), e);
                    // Mark all valid records as failed if save fails, converting operational back to shutdown hours
                    for (CPPAssetOperationalHoursResponseDto failedDto : validRecords) {
                        CPPAssetOperationalHoursResponseDto originalDto = cloneDto(failedDto);
                        originalDto.setApr(convertShutdownToOperational(failedDto.getApr(), totalHoursByMonth.get("apr")));
                        originalDto.setMay(convertShutdownToOperational(failedDto.getMay(), totalHoursByMonth.get("may")));
                        originalDto.setJun(convertShutdownToOperational(failedDto.getJun(), totalHoursByMonth.get("jun")));
                        originalDto.setJul(convertShutdownToOperational(failedDto.getJul(), totalHoursByMonth.get("jul")));
                        originalDto.setAug(convertShutdownToOperational(failedDto.getAug(), totalHoursByMonth.get("aug")));
                        originalDto.setSep(convertShutdownToOperational(failedDto.getSep(), totalHoursByMonth.get("sep")));
                        originalDto.setOct(convertShutdownToOperational(failedDto.getOct(), totalHoursByMonth.get("oct")));
                        originalDto.setNov(convertShutdownToOperational(failedDto.getNov(), totalHoursByMonth.get("nov")));
                        originalDto.setDec(convertShutdownToOperational(failedDto.getDec(), totalHoursByMonth.get("dec")));
                        originalDto.setJan(convertShutdownToOperational(failedDto.getJan(), totalHoursByMonth.get("jan")));
                        originalDto.setFeb(convertShutdownToOperational(failedDto.getFeb(), totalHoursByMonth.get("feb")));
                        originalDto.setMar(convertShutdownToOperational(failedDto.getMar(), totalHoursByMonth.get("mar")));
                        failedRecords.add(originalDto);
                        failureReasons.add("Save failed: " + e.getMessage());
                    }
                }
            }
            
            // Prepare response
            if (failedRecords.isEmpty()) {
                response.setCode(200);
                if (validRecords.isEmpty() && skippedCount > 0) {
                    response.setMessage("No changes detected in imported records. All " + skippedCount + " records unchanged.");
                } else {
                    response.setMessage("All steam operational hours imported successfully. " + skippedCount + " records unchanged, " + validRecords.size() + " records updated.");
                }
            } else {
                // Export only failed records to Excel file with Status and Comment columns
                byte[] failedRecordsFile = generateErrorExcel(failedRecords, failureReasons, "Steam Operational Hours", financialYear);
                String base64File = java.util.Base64.getEncoder().encodeToString(failedRecordsFile);
                response.setCode(400);
                response.setMessage("Partial import: " + validRecords.size() + " saved, " + failedRecords.size() + " failed, " + skippedCount + " unchanged. Download file for details.");
                response.setData(base64File);
                logger.info("[Import Steam Operational Hours] Exported {} failed records to Excel", failedRecords.size());
            }
            
            logger.info("[Import Steam Operational Hours] Import completed - Unchanged: {}, Saved: {}, Failed: {}", skippedCount, validRecords.size(), failedRecords.size());
        } catch (Exception e) {
            logger.error("[Import Steam Operational Hours] Error during import: {}", e.getMessage(), e);
            response.setCode(500);
            response.setMessage("Failed to import steam operational hours: " + e.getMessage());
        }
        return response;
    }
    
    private List<CPPAssetOperationalHoursResponseDto> readOperationalHoursExcelJMD(InputStream inputStream, String financialYear) throws Exception {
        List<CPPAssetOperationalHoursResponseDto> dataList = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            
            // Skip both header rows (top header and sub-header)
            if (rowIterator.hasNext()) {
                rowIterator.next(); // Skip top header row
            }
            if (rowIterator.hasNext()) {
                rowIterator.next(); // Skip sub-header row
            }
            
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                
                // Skip completely empty rows
                boolean isRowEmpty = true;
                for (int i = 0; i < row.getLastCellNum(); i++) {
                    Cell cell = row.getCell(i);
                    if (cell != null && !cell.toString().trim().isEmpty()) {
                        isRowEmpty = false;
                        break;
                    }
                }
                if (isRowEmpty) {
                    logger.debug("[Excel Import] Skipping empty row: {}", row.getRowNum());
                    continue;
                }
                
                CPPAssetOperationalHoursResponseDto dto = new CPPAssetOperationalHoursResponseDto();
                
                try {
                    int col = 0;
                    // Static columns
                    dto.setAssetName(getStringCellValue(row.getCell(col++)));
                    dto.setAssetType(getStringCellValue(row.getCell(col++)));
                    dto.setPlantName(getStringCellValue(row.getCell(col++)));
                    dto.setUtilityDistributed(getStringCellValue(row.getCell(col++)));
                    dto.setDistributedSapCode(getStringCellValue(row.getCell(col++)));
                    dto.setUtilityGenerated(getStringCellValue(row.getCell(col++)));
                    dto.setGeneratedUtilityCode(getStringCellValue(row.getCell(col++)));
                    
                    // Monthly shutdown hours (columns H, J, L, N, P, R, T, V, X, Z, AB, AD)
                    // Column H (Apr Shutdown)
                    dto.setApr(getNumericCellValue(row.getCell(col++)));
                    col++; // Skip operational hrs column
                    // Column J (May Shutdown)
                    dto.setMay(getNumericCellValue(row.getCell(col++)));
                    col++; // Skip operational hrs column
                    // Column L (Jun Shutdown)
                    dto.setJun(getNumericCellValue(row.getCell(col++)));
                    col++; // Skip operational hrs column
                    // Column N (Jul Shutdown)
                    dto.setJul(getNumericCellValue(row.getCell(col++)));
                    col++; // Skip operational hrs column
                    // Column P (Aug Shutdown)
                    dto.setAug(getNumericCellValue(row.getCell(col++)));
                    col++; // Skip operational hrs column
                    // Column R (Sep Shutdown)
                    dto.setSep(getNumericCellValue(row.getCell(col++)));
                    col++; // Skip operational hrs column
                    // Column T (Oct Shutdown)
                    dto.setOct(getNumericCellValue(row.getCell(col++)));
                    col++; // Skip operational hrs column
                    // Column V (Nov Shutdown)
                    dto.setNov(getNumericCellValue(row.getCell(col++)));
                    col++; // Skip operational hrs column
                    // Column X (Dec Shutdown)
                    dto.setDec(getNumericCellValue(row.getCell(col++)));
                    col++; // Skip operational hrs column
                    // Column Z (Jan Shutdown)
                    dto.setJan(getNumericCellValue(row.getCell(col++)));
                    col++; // Skip operational hrs column
                    // Column AB (Feb Shutdown)
                    dto.setFeb(getNumericCellValue(row.getCell(col++)));
                    col++; // Skip operational hrs column
                    // Column AD (Mar Shutdown)
                    dto.setMar(getNumericCellValue(row.getCell(col++)));
                    col++; // Skip operational hrs column
                    
                    // Remarks and hidden columns
                    dto.setRemarks(getStringCellValue(row.getCell(col++)));
                    
                    String idStr = getStringCellValue(row.getCell(col++));
                    if (idStr != null && !idStr.isEmpty()) {
                        dto.setId(UUID.fromString(idStr));
                    }
                    
                    String assetFkIdStr = getStringCellValue(row.getCell(col++));
                    if (assetFkIdStr != null && !assetFkIdStr.isEmpty()) {
                        dto.setAssetFkId(UUID.fromString(assetFkIdStr));
                    }
                    
                    dto.setAssetCategory(getStringCellValue(row.getCell(col++)));
                    
                    // Read the stored hash from Excel
                    String storedHash = getStringCellValue(row.getCell(col++));
                    // Store it temporarily in a custom field (we'll use it for validation)
                    // For now, we'll validate it in the validation method
                    
                    dataList.add(dto);
                    logger.debug("[Excel Import] Successfully read row for asset: {}", dto.getAssetName());
                    
                } catch (Exception e) {
                    logger.error("[Excel Import] Error reading row {}: {}", row.getRowNum(), e.getMessage());
                }
            }
        }
        
        return dataList;
    }
    
    private String validateShutdownHoursData(CPPAssetOperationalHoursResponseDto dto, Map<String, Double> totalHoursByMonth) {
        if (dto.getId() == null) {
            return "Record ID is missing";
        }
        
        // Validate remarks is not empty
        if (dto.getRemarks() == null || dto.getRemarks().trim().isEmpty()) {
            return "Remarks field is mandatory and cannot be empty";
        }
        
        // Check if remarks have been updated - MANDATORY requirement
        // This is only called for records that isRecordModified() detected as changed
        try {
            String dbRemarks = "";
            if ("Steam".equals(dto.getAssetCategory())) {
                var optionalEntity = steamRepository.findById(dto.getId());
                if (optionalEntity.isPresent()) {
                    dbRemarks = optionalEntity.get().getRemarks() != null ? optionalEntity.get().getRemarks().trim() : "";
                }
            } else {
                var optionalEntity = repository.findById(dto.getId());
                if (optionalEntity.isPresent()) {
                    dbRemarks = optionalEntity.get().getRemarks() != null ? optionalEntity.get().getRemarks().trim() : "";
                }
            }
            String importedRemarks = dto.getRemarks() != null ? dto.getRemarks().trim() : "";
            
            // CRITICAL: Remarks MUST be different from DB value
            // This ensures users document why they're making changes
            if (dbRemarks.equals(importedRemarks)) {
                return "Remarks must be updated to explain the changes. Current remarks are identical to the database value.";
            }
        } catch (Exception e) {
            logger.error("[Validation] Error checking remarks for ID {}: {}", dto.getId(), e.getMessage());
            // Continue with other validations if DB check fails
        }
        
        // Validate each month's shutdown hours (must be >= 0 and <= max hours)
        if (dto.getApr() != null && (dto.getApr() < 0 || dto.getApr() > totalHoursByMonth.get("apr"))) {
            return "April shutdown hours (" + dto.getApr() + ") must be between 0 and " + totalHoursByMonth.get("apr");
        }
        if (dto.getMay() != null && (dto.getMay() < 0 || dto.getMay() > totalHoursByMonth.get("may"))) {
            return "May shutdown hours (" + dto.getMay() + ") must be between 0 and " + totalHoursByMonth.get("may");
        }
        if (dto.getJun() != null && (dto.getJun() < 0 || dto.getJun() > totalHoursByMonth.get("jun"))) {
            return "June shutdown hours (" + dto.getJun() + ") must be between 0 and " + totalHoursByMonth.get("jun");
        }
        if (dto.getJul() != null && (dto.getJul() < 0 || dto.getJul() > totalHoursByMonth.get("jul"))) {
            return "July shutdown hours (" + dto.getJul() + ") must be between 0 and " + totalHoursByMonth.get("jul");
        }
        if (dto.getAug() != null && (dto.getAug() < 0 || dto.getAug() > totalHoursByMonth.get("aug"))) {
            return "August shutdown hours (" + dto.getAug() + ") must be between 0 and " + totalHoursByMonth.get("aug");
        }
        if (dto.getSep() != null && (dto.getSep() < 0 || dto.getSep() > totalHoursByMonth.get("sep"))) {
            return "September shutdown hours (" + dto.getSep() + ") must be between 0 and " + totalHoursByMonth.get("sep");
        }
        if (dto.getOct() != null && (dto.getOct() < 0 || dto.getOct() > totalHoursByMonth.get("oct"))) {
            return "October shutdown hours (" + dto.getOct() + ") must be between 0 and " + totalHoursByMonth.get("oct");
        }
        if (dto.getNov() != null && (dto.getNov() < 0 || dto.getNov() > totalHoursByMonth.get("nov"))) {
            return "November shutdown hours (" + dto.getNov() + ") must be between 0 and " + totalHoursByMonth.get("nov");
        }
        if (dto.getDec() != null && (dto.getDec() < 0 || dto.getDec() > totalHoursByMonth.get("dec"))) {
            return "December shutdown hours (" + dto.getDec() + ") must be between 0 and " + totalHoursByMonth.get("dec");
        }
        if (dto.getJan() != null && (dto.getJan() < 0 || dto.getJan() > totalHoursByMonth.get("jan"))) {
            return "January shutdown hours (" + dto.getJan() + ") must be between 0 and " + totalHoursByMonth.get("jan");
        }
        if (dto.getFeb() != null && (dto.getFeb() < 0 || dto.getFeb() > totalHoursByMonth.get("feb"))) {
            return "February shutdown hours (" + dto.getFeb() + ") must be between 0 and " + totalHoursByMonth.get("feb");
        }
        if (dto.getMar() != null && (dto.getMar() < 0 || dto.getMar() > totalHoursByMonth.get("mar"))) {
            return "March shutdown hours (" + dto.getMar() + ") must be between 0 and " + totalHoursByMonth.get("mar");
        }
        
        return null; // Valid
    }
    
    private void calculateOperationalHoursFromShutdown(CPPAssetOperationalHoursResponseDto dto, Map<String, Double> totalHoursByMonth) {
        // Calculate operational hours: Total Available - Shutdown Hours
        if (dto.getApr() != null) {
            dto.setApr(totalHoursByMonth.get("apr") - dto.getApr());
        }
        if (dto.getMay() != null) {
            dto.setMay(totalHoursByMonth.get("may") - dto.getMay());
        }
        if (dto.getJun() != null) {
            dto.setJun(totalHoursByMonth.get("jun") - dto.getJun());
        }
        if (dto.getJul() != null) {
            dto.setJul(totalHoursByMonth.get("jul") - dto.getJul());
        }
        if (dto.getAug() != null) {
            dto.setAug(totalHoursByMonth.get("aug") - dto.getAug());
        }
        if (dto.getSep() != null) {
            dto.setSep(totalHoursByMonth.get("sep") - dto.getSep());
        }
        if (dto.getOct() != null) {
            dto.setOct(totalHoursByMonth.get("oct") - dto.getOct());
        }
        if (dto.getNov() != null) {
            dto.setNov(totalHoursByMonth.get("nov") - dto.getNov());
        }
        if (dto.getDec() != null) {
            dto.setDec(totalHoursByMonth.get("dec") - dto.getDec());
        }
        if (dto.getJan() != null) {
            dto.setJan(totalHoursByMonth.get("jan") - dto.getJan());
        }
        if (dto.getFeb() != null) {
            dto.setFeb(totalHoursByMonth.get("feb") - dto.getFeb());
        }
        if (dto.getMar() != null) {
            dto.setMar(totalHoursByMonth.get("mar") - dto.getMar());
        }
    }
    
    private String getStringCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.STRING) {
                return cell.getStringCellValue();
            } else if (cell.getCellType() == CellType.NUMERIC) {
                return String.valueOf((int) cell.getNumericCellValue());
            }
        } catch (Exception e) {
            logger.debug("[Excel Import] Error reading cell: {}", e.getMessage());
        }
        return null;
    }
    
    private Double getNumericCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue();
                if (value != null && !value.isEmpty()) {
                    return Double.parseDouble(value);
                }
            }
        } catch (Exception e) {
            logger.debug("[Excel Import] Error reading numeric cell: {}", e.getMessage());
        }
        return null;
    }

    private Double convertOperationalToShutdown(Double operationalHours, Double totalHours) {
        if (operationalHours == null || totalHours == null) {
            return null;
        }
        return totalHours - operationalHours;
    }

    private CPPAssetOperationalHoursResponseDto cloneDto(CPPAssetOperationalHoursResponseDto source) {
        if (source == null) {
            return null;
        }
        CPPAssetOperationalHoursResponseDto target = new CPPAssetOperationalHoursResponseDto();
        target.setId(source.getId());
        target.setAssetFkId(source.getAssetFkId());
        target.setUtilityDistributed(source.getUtilityDistributed());
        target.setDistributedSapCode(source.getDistributedSapCode());
        target.setUtilityGenerated(source.getUtilityGenerated());
        target.setGeneratedUtilityCode(source.getGeneratedUtilityCode());

        target.setApr(source.getApr());
        target.setMay(source.getMay());
        target.setJun(source.getJun());
        target.setJul(source.getJul());
        target.setAug(source.getAug());
        target.setSep(source.getSep());
        target.setOct(source.getOct());
        target.setNov(source.getNov());
        target.setDec(source.getDec());
        target.setJan(source.getJan());
        target.setFeb(source.getFeb());
        target.setMar(source.getMar());

        target.setAopYear(source.getAopYear());
        target.setRemarks(source.getRemarks());

        target.setAssetCategory(source.getAssetCategory());
        target.setSiteFkId(source.getSiteFkId());
        target.setVerticalFkId(source.getVerticalFkId());
        target.setPlantFkId(source.getPlantFkId());

        target.setCreatedDate(source.getCreatedDate());
        target.setModifiedDate(source.getModifiedDate());

        target.setAssetName(source.getAssetName());
        target.setPlantName(source.getPlantName());
        target.setAssetType(source.getAssetType());
        return target;
    }
}
