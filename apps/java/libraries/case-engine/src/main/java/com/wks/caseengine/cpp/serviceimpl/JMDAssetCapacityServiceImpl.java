package com.wks.caseengine.cpp.serviceimpl;

import com.wks.caseengine.dto.CPPAssetCapacityResponseDto;
import com.wks.caseengine.dto.AssetCapacityRequestDTO;
import com.wks.caseengine.cpp.dto.CPPAssetCapacityProjection;
import com.wks.caseengine.cpp.entity.CPPPowerAssetCapacity;
import com.wks.caseengine.cpp.entity.CPPSteamAssetCapacity;
import com.wks.caseengine.cpp.repository.CPPPowerAssetCapacityRepository;
import com.wks.caseengine.cpp.repository.CPPSteamAssetCapacityRepository;
import com.wks.caseengine.cpp.service.JMDAssetCapacityService;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

@Service
public class JMDAssetCapacityServiceImpl implements JMDAssetCapacityService {

    private static final Logger logger = LoggerFactory.getLogger(JMDAssetCapacityServiceImpl.class);

    @Autowired
    private CPPPowerAssetCapacityRepository repository;

    @Autowired
    private CPPSteamAssetCapacityRepository steamRepository;

    @Override
    public AOPMessageVM getAssetCapacitiesForPlants(List<UUID> plantIds, String aopYear) {
        logger.info("[GET Service] Fetching asset capacities for plantIds: {}, aopYear: {}", plantIds, aopYear);
        AOPMessageVM aopMessageVM = new AOPMessageVM();

        try {
            List<CPPAssetCapacityProjection> projections =
                    repository.findAssetCapacitiesByPlants(plantIds, aopYear);
            logger.info("[GET Service] Query returned {} records", projections.size());

            List<CPPAssetCapacityResponseDto> allResults = projections.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());

            List<CPPAssetCapacityResponseDto> powerCapacities = allResults.stream()
                    .filter(dto -> "Power".equals(dto.getAssetCategory()))
                    .collect(Collectors.toList());
            logger.info("[GET Service] Filtered {} power assets", powerCapacities.size());

            List<CPPAssetCapacityResponseDto> steamCapacities = allResults.stream()
                    .filter(dto -> "Steam".equals(dto.getAssetCategory()))
                    .collect(Collectors.toList());
            logger.info("[GET Service] Filtered {} steam assets", steamCapacities.size());

            Map<String, Object> data = new HashMap<>();
            data.put("PowerAssetCapacities", powerCapacities);
            data.put("SteamAssetCapacities", steamCapacities);

            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(data);
            logger.info("[GET Service] Successfully fetched asset capacities data");
        } catch (Exception e) {
            logger.error("[GET Service] Error fetching asset capacities: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to fetch data: " + e.getMessage());
            aopMessageVM.setData(null);
        }

        return aopMessageVM;
    }

    private CPPAssetCapacityResponseDto mapToDto(CPPAssetCapacityProjection projection) {
        CPPAssetCapacityResponseDto dto = new CPPAssetCapacityResponseDto();

        dto.setId(projection.getId());
        dto.setAssetFkId(projection.getAssetFkId());
        dto.setPlantFkId(projection.getPlantFkId());

        dto.setFixedMin(projection.getFixedMin());
        dto.setFixedMax(projection.getFixedMax());

        dto.setAprMin(projection.getAprMin());
        dto.setAprMax(projection.getAprMax());
        dto.setMayMin(projection.getMayMin());
        dto.setMayMax(projection.getMayMax());
        dto.setJunMin(projection.getJunMin());
        dto.setJunMax(projection.getJunMax());
        dto.setJulMin(projection.getJulMin());
        dto.setJulMax(projection.getJulMax());
        dto.setAugMin(projection.getAugMin());
        dto.setAugMax(projection.getAugMax());
        dto.setSepMin(projection.getSepMin());
        dto.setSepMax(projection.getSepMax());
        dto.setOctMin(projection.getOctMin());
        dto.setOctMax(projection.getOctMax());
        dto.setNovMin(projection.getNovMin());
        dto.setNovMax(projection.getNovMax());
        dto.setDecMin(projection.getDecMin());
        dto.setDecMax(projection.getDecMax());
        dto.setJanMin(projection.getJanMin());
        dto.setJanMax(projection.getJanMax());
        dto.setFebMin(projection.getFebMin());
        dto.setFebMax(projection.getFebMax());
        dto.setMarMin(projection.getMarMin());
        dto.setMarMax(projection.getMarMax());

        dto.setAopYear(projection.getAopYear());
        dto.setRemarks(projection.getRemarks());
        dto.setUom(projection.getUom());

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

    private boolean isCapacityRecordModified(CPPAssetCapacityResponseDto dto, Object entity) {
        try {
            if (entity instanceof CPPPowerAssetCapacity) {
                CPPPowerAssetCapacity db = (CPPPowerAssetCapacity) entity;
                if (!Objects.equals(dto.getFixedMin(), db.getFixedMin())) return true;
                if (!Objects.equals(dto.getFixedMax(), db.getFixedMax())) return true;
                if (!Objects.equals(dto.getAprMin(), db.getAprMin())) return true;
                if (!Objects.equals(dto.getAprMax(), db.getAprMax())) return true;
                if (!Objects.equals(dto.getMayMin(), db.getMayMin())) return true;
                if (!Objects.equals(dto.getMayMax(), db.getMayMax())) return true;
                if (!Objects.equals(dto.getJunMin(), db.getJunMin())) return true;
                if (!Objects.equals(dto.getJunMax(), db.getJunMax())) return true;
                if (!Objects.equals(dto.getJulMin(), db.getJulMin())) return true;
                if (!Objects.equals(dto.getJulMax(), db.getJulMax())) return true;
                if (!Objects.equals(dto.getAugMin(), db.getAugMin())) return true;
                if (!Objects.equals(dto.getAugMax(), db.getAugMax())) return true;
                if (!Objects.equals(dto.getSepMin(), db.getSepMin())) return true;
                if (!Objects.equals(dto.getSepMax(), db.getSepMax())) return true;
                if (!Objects.equals(dto.getOctMin(), db.getOctMin())) return true;
                if (!Objects.equals(dto.getOctMax(), db.getOctMax())) return true;
                if (!Objects.equals(dto.getNovMin(), db.getNovMin())) return true;
                if (!Objects.equals(dto.getNovMax(), db.getNovMax())) return true;
                if (!Objects.equals(dto.getDecMin(), db.getDecMin())) return true;
                if (!Objects.equals(dto.getDecMax(), db.getDecMax())) return true;
                if (!Objects.equals(dto.getJanMin(), db.getJanMin())) return true;
                if (!Objects.equals(dto.getJanMax(), db.getJanMax())) return true;
                if (!Objects.equals(dto.getFebMin(), db.getFebMin())) return true;
                if (!Objects.equals(dto.getFebMax(), db.getFebMax())) return true;
                if (!Objects.equals(dto.getMarMin(), db.getMarMin())) return true;
                if (!Objects.equals(dto.getMarMax(), db.getMarMax())) return true;
                if (!Objects.equals(dto.getUom(), db.getUom())) return true;
                if (!Objects.equals(dto.getRemarks(), db.getRemarks())) return true;
                return false;
            } else if (entity instanceof CPPSteamAssetCapacity) {
                CPPSteamAssetCapacity db = (CPPSteamAssetCapacity) entity;
                if (!Objects.equals(dto.getFixedMin(), db.getFixedMin())) return true;
                if (!Objects.equals(dto.getFixedMax(), db.getFixedMax())) return true;
                if (!Objects.equals(dto.getAprMin(), db.getAprMin())) return true;
                if (!Objects.equals(dto.getAprMax(), db.getAprMax())) return true;
                if (!Objects.equals(dto.getMayMin(), db.getMayMin())) return true;
                if (!Objects.equals(dto.getMayMax(), db.getMayMax())) return true;
                if (!Objects.equals(dto.getJunMin(), db.getJunMin())) return true;
                if (!Objects.equals(dto.getJunMax(), db.getJunMax())) return true;
                if (!Objects.equals(dto.getJulMin(), db.getJulMin())) return true;
                if (!Objects.equals(dto.getJulMax(), db.getJulMax())) return true;
                if (!Objects.equals(dto.getAugMin(), db.getAugMin())) return true;
                if (!Objects.equals(dto.getAugMax(), db.getAugMax())) return true;
                if (!Objects.equals(dto.getSepMin(), db.getSepMin())) return true;
                if (!Objects.equals(dto.getSepMax(), db.getSepMax())) return true;
                if (!Objects.equals(dto.getOctMin(), db.getOctMin())) return true;
                if (!Objects.equals(dto.getOctMax(), db.getOctMax())) return true;
                if (!Objects.equals(dto.getNovMin(), db.getNovMin())) return true;
                if (!Objects.equals(dto.getNovMax(), db.getNovMax())) return true;
                if (!Objects.equals(dto.getDecMin(), db.getDecMin())) return true;
                if (!Objects.equals(dto.getDecMax(), db.getDecMax())) return true;
                if (!Objects.equals(dto.getJanMin(), db.getJanMin())) return true;
                if (!Objects.equals(dto.getJanMax(), db.getJanMax())) return true;
                if (!Objects.equals(dto.getFebMin(), db.getFebMin())) return true;
                if (!Objects.equals(dto.getFebMax(), db.getFebMax())) return true;
                if (!Objects.equals(dto.getMarMin(), db.getMarMin())) return true;
                if (!Objects.equals(dto.getMarMax(), db.getMarMax())) return true;
                if (!Objects.equals(dto.getUom(), db.getUom())) return true;
                if (!Objects.equals(dto.getRemarks(), db.getRemarks())) return true;
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.error("[isCapacityRecordModified] Error comparing record ID {}: {}", dto.getId(), e.getMessage());
            return true;
        }
    }

    @Override
    @Transactional
    public AOPMessageVM saveAssetCapacities(
            List<UUID> plantIds,
            String aopYear,
            AssetCapacityRequestDTO payload) {

        logger.info("[POST Service] Saving asset capacities for plantIds: {}, aopYear: {}", plantIds, aopYear);
        AOPMessageVM aopMessageVM = new AOPMessageVM();

        try {
            int powerSaved = 0, steamSaved = 0;
            int powerSkipped = 0, steamSkipped = 0;
            int powerUnchanged = 0, steamUnchanged = 0;

            List<Map<String, String>> missingIdRecords = new ArrayList<>();

            if (payload.getPowerResponse() != null) {
                logger.info("[POST Service] Processing {} power asset capacity records", payload.getPowerResponse().size());
                for (CPPAssetCapacityResponseDto dto : payload.getPowerResponse()) {
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

                    var optionalEntity = repository.findById(dto.getId());
                    if (optionalEntity.isPresent() && !isCapacityRecordModified(dto, optionalEntity.get())) {
                        powerUnchanged++;
                        logger.debug("[POST Service - Power] Skipping unchanged record: {}", dto.getAssetName());
                        continue;
                    }

                    boolean saved = savePowerAssetCapacity(dto);
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
                logger.info("[POST Service] Power assets - Saved: {}, Unchanged: {}, Skipped: {}", powerSaved, powerUnchanged, powerSkipped);
            }

            if (payload.getSteamResponse() != null) {
                logger.info("[POST Service] Processing {} steam asset capacity records", payload.getSteamResponse().size());
                for (CPPAssetCapacityResponseDto dto : payload.getSteamResponse()) {
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

                    var optionalEntity = steamRepository.findById(dto.getId());
                    if (optionalEntity.isPresent() && !isCapacityRecordModified(dto, optionalEntity.get())) {
                        steamUnchanged++;
                        logger.debug("[POST Service - Steam] Skipping unchanged record: {}", dto.getAssetName());
                        continue;
                    }

                    boolean saved = saveSteamAssetCapacity(dto);
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
                logger.info("[POST Service] Steam assets - Saved: {}, Unchanged: {}, Skipped: {}", steamSaved, steamUnchanged, steamSkipped);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("powerAssetsSaved", powerSaved);
            data.put("steamAssetsSaved", steamSaved);
            data.put("totalSaved", powerSaved + steamSaved);
            data.put("powerAssetsUnchanged", powerUnchanged);
            data.put("steamAssetsUnchanged", steamUnchanged);
            data.put("totalUnchanged", powerUnchanged + steamUnchanged);
            data.put("powerAssetsSkipped", powerSkipped);
            data.put("steamAssetsSkipped", steamSkipped);
            data.put("totalSkipped", powerSkipped + steamSkipped);

            if (!missingIdRecords.isEmpty()) {
                data.put("skippedRecords", missingIdRecords);
            }

            if (missingIdRecords.isEmpty()) {
                aopMessageVM.setCode(200);
                if (powerSaved == 0 && steamSaved == 0 && (powerUnchanged > 0 || steamUnchanged > 0)) {
                    aopMessageVM.setMessage("No changes detected. All " + (powerUnchanged + steamUnchanged) + " records unchanged.");
                } else {
                    aopMessageVM.setMessage("Asset capacities saved successfully. " + (powerUnchanged + steamUnchanged) + " unchanged, " + (powerSaved + steamSaved) + " updated.");
                }
            } else {
                aopMessageVM.setCode(207);
                aopMessageVM.setMessage(String.format("Partial success: %d saved, %d unchanged, %d skipped (missing or invalid IDs)",
                        powerSaved + steamSaved, powerUnchanged + steamUnchanged, powerSkipped + steamSkipped));
            }

            aopMessageVM.setData(data);
            logger.info("[POST Service] Save completed - Saved: {} (Power: {}, Steam: {}), Unchanged: {} (Power: {}, Steam: {}), Skipped: {} (Power: {}, Steam: {})",
                    powerSaved + steamSaved, powerSaved, steamSaved,
                    powerUnchanged + steamUnchanged, powerUnchanged, steamUnchanged,
                    powerSkipped + steamSkipped, powerSkipped, steamSkipped);

        } catch (Exception e) {
            logger.error("[POST Service] Error saving asset capacities: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to save asset capacities: " + e.getMessage());
            aopMessageVM.setData(null);
        }

        return aopMessageVM;
    }

    private boolean savePowerAssetCapacity(CPPAssetCapacityResponseDto dto) {
        logger.debug("[POST Service - Power] Updating existing record with ID: {}", dto.getId());
        var optionalEntity = repository.findById(dto.getId());
        if (optionalEntity.isEmpty()) {
            logger.error("[POST Service - Power] Record with ID {} not found", dto.getId());
            return false;
        }
        CPPPowerAssetCapacity entity = optionalEntity.get();
        entity.setUpdatedDate(LocalDateTime.now());

        entity.setFixedMin(dto.getFixedMin());
        entity.setFixedMax(dto.getFixedMax());
        entity.setAprMin(dto.getAprMin());
        entity.setAprMax(dto.getAprMax());
        entity.setMayMin(dto.getMayMin());
        entity.setMayMax(dto.getMayMax());
        entity.setJunMin(dto.getJunMin());
        entity.setJunMax(dto.getJunMax());
        entity.setJulMin(dto.getJulMin());
        entity.setJulMax(dto.getJulMax());
        entity.setAugMin(dto.getAugMin());
        entity.setAugMax(dto.getAugMax());
        entity.setSepMin(dto.getSepMin());
        entity.setSepMax(dto.getSepMax());
        entity.setOctMin(dto.getOctMin());
        entity.setOctMax(dto.getOctMax());
        entity.setNovMin(dto.getNovMin());
        entity.setNovMax(dto.getNovMax());
        entity.setDecMin(dto.getDecMin());
        entity.setDecMax(dto.getDecMax());
        entity.setJanMin(dto.getJanMin());
        entity.setJanMax(dto.getJanMax());
        entity.setFebMin(dto.getFebMin());
        entity.setFebMax(dto.getFebMax());
        entity.setMarMin(dto.getMarMin());
        entity.setMarMax(dto.getMarMax());

        entity.setUom(dto.getUom());
        entity.setRemarks(dto.getRemarks());

        repository.save(entity);
        logger.debug("[POST Service - Power] Successfully updated entity with ID: {}", entity.getId());
        return true;
    }

    private boolean saveSteamAssetCapacity(CPPAssetCapacityResponseDto dto) {
        logger.debug("[POST Service - Steam] Updating existing record with ID: {}", dto.getId());
        var optionalEntity = steamRepository.findById(dto.getId());
        if (optionalEntity.isEmpty()) {
            logger.error("[POST Service - Steam] Record with ID {} not found", dto.getId());
            return false;
        }
        CPPSteamAssetCapacity entity = optionalEntity.get();
        entity.setUpdatedDate(LocalDateTime.now());

        entity.setFixedMin(dto.getFixedMin());
        entity.setFixedMax(dto.getFixedMax());
        entity.setAprMin(dto.getAprMin());
        entity.setAprMax(dto.getAprMax());
        entity.setMayMin(dto.getMayMin());
        entity.setMayMax(dto.getMayMax());
        entity.setJunMin(dto.getJunMin());
        entity.setJunMax(dto.getJunMax());
        entity.setJulMin(dto.getJulMin());
        entity.setJulMax(dto.getJulMax());
        entity.setAugMin(dto.getAugMin());
        entity.setAugMax(dto.getAugMax());
        entity.setSepMin(dto.getSepMin());
        entity.setSepMax(dto.getSepMax());
        entity.setOctMin(dto.getOctMin());
        entity.setOctMax(dto.getOctMax());
        entity.setNovMin(dto.getNovMin());
        entity.setNovMax(dto.getNovMax());
        entity.setDecMin(dto.getDecMin());
        entity.setDecMax(dto.getDecMax());
        entity.setJanMin(dto.getJanMin());
        entity.setJanMax(dto.getJanMax());
        entity.setFebMin(dto.getFebMin());
        entity.setFebMax(dto.getFebMax());
        entity.setMarMin(dto.getMarMin());
        entity.setMarMax(dto.getMarMax());

        entity.setUom(dto.getUom());
        entity.setRemarks(dto.getRemarks());

        steamRepository.save(entity);
        logger.debug("[POST Service - Steam] Successfully updated entity with ID: {}", entity.getId());
        return true;
    }

    @Override
    public byte[] exportPowerAssetCapacity(List<UUID> plantIds, String aopYear) {
        logger.info("[Export Power Capacity] Exporting for plantIds: {}, aopYear: {}", plantIds, aopYear);
        try {
            AOPMessageVM response = getAssetCapacitiesForPlants(plantIds, aopYear);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.getData();
            @SuppressWarnings("unchecked")
            List<CPPAssetCapacityResponseDto> powerCapacities =
                    (List<CPPAssetCapacityResponseDto>) data.get("PowerAssetCapacities");
            if (powerCapacities == null) powerCapacities = new ArrayList<>();
            powerCapacities.sort(Comparator.comparing(CPPAssetCapacityResponseDto::getPlantName)
                    .thenComparing(CPPAssetCapacityResponseDto::getAssetName));
            return generateAssetCapacityExcel(powerCapacities, "Power Asset Capacity", aopYear);
        } catch (Exception e) {
            logger.error("[Export Power Capacity] Error: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public byte[] exportSteamAssetCapacity(List<UUID> plantIds, String aopYear) {
        logger.info("[Export Steam Capacity] Exporting for plantIds: {}, aopYear: {}", plantIds, aopYear);
        try {
            AOPMessageVM response = getAssetCapacitiesForPlants(plantIds, aopYear);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.getData();
            @SuppressWarnings("unchecked")
            List<CPPAssetCapacityResponseDto> steamCapacities =
                    (List<CPPAssetCapacityResponseDto>) data.get("SteamAssetCapacities");
            if (steamCapacities == null) steamCapacities = new ArrayList<>();
            steamCapacities.sort(Comparator.comparing(CPPAssetCapacityResponseDto::getPlantName)
                    .thenComparing(CPPAssetCapacityResponseDto::getAssetName));
            return generateAssetCapacityExcel(steamCapacities, "Steam Asset Capacity", aopYear);
        } catch (Exception e) {
            logger.error("[Export Steam Capacity] Error: {}", e.getMessage(), e);
            return null;
        }
    }

    private byte[] generateAssetCapacityExcel(List<CPPAssetCapacityResponseDto> dataList, String sheetName, String aopYear) throws Exception {
        logger.info("[Excel Generation] Creating {} with {} records", sheetName, dataList != null ? dataList.size() : 0);
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        String startYearSuffix = aopYear.substring(2, 4);
        String endYearSuffix = aopYear.substring(5, 7);

        int currentRow = 0;
        int col = 0;

        Row headerRow = sheet.createRow(currentRow++);
        String[] staticHeaders = {"Asset Name", "Asset Type", "Plant Name", "Fixed Min", "Fixed Max", "UOM"};
        for (String h : staticHeaders) {
            createCell(headerRow, col++, h, headerStyle);
        }

        String[] months = {"Apr-" + startYearSuffix, "May-" + startYearSuffix, "Jun-" + startYearSuffix,
                "Jul-" + startYearSuffix, "Aug-" + startYearSuffix, "Sep-" + startYearSuffix,
                "Oct-" + startYearSuffix, "Nov-" + startYearSuffix, "Dec-" + startYearSuffix,
                "Jan-" + endYearSuffix, "Feb-" + endYearSuffix, "Mar-" + endYearSuffix};

        for (String month : months) {
            createCell(headerRow, col++, month + " Min", headerStyle);
            createCell(headerRow, col++, month + " Max", headerStyle);
        }

        int remarksCol = col;
        createCell(headerRow, col++, "Remarks", headerStyle);
        createCell(headerRow, col++, "id", headerStyle);
        int idCol = col - 1;
        createCell(headerRow, col++, "assetFkId", headerStyle);
        int assetFkIdCol = col - 1;
        createCell(headerRow, col++, "assetCategory", headerStyle);
        int assetCategoryCol = col - 1;

        int totalColumns = col;

        for (CPPAssetCapacityResponseDto dto : dataList) {
            Row row = sheet.createRow(currentRow++);
            col = 0;
            createCell(row, col++, dto.getAssetName(), dataStyle);
            createCell(row, col++, dto.getAssetType(), dataStyle);
            createCell(row, col++, dto.getPlantName(), dataStyle);
            setNumericCell(row, col++, dto.getFixedMin(), dataStyle);
            setNumericCell(row, col++, dto.getFixedMax(), dataStyle);
            createCell(row, col++, dto.getUom(), dataStyle);

            setNumericCell(row, col++, dto.getAprMin(), dataStyle);
            setNumericCell(row, col++, dto.getAprMax(), dataStyle);
            setNumericCell(row, col++, dto.getMayMin(), dataStyle);
            setNumericCell(row, col++, dto.getMayMax(), dataStyle);
            setNumericCell(row, col++, dto.getJunMin(), dataStyle);
            setNumericCell(row, col++, dto.getJunMax(), dataStyle);
            setNumericCell(row, col++, dto.getJulMin(), dataStyle);
            setNumericCell(row, col++, dto.getJulMax(), dataStyle);
            setNumericCell(row, col++, dto.getAugMin(), dataStyle);
            setNumericCell(row, col++, dto.getAugMax(), dataStyle);
            setNumericCell(row, col++, dto.getSepMin(), dataStyle);
            setNumericCell(row, col++, dto.getSepMax(), dataStyle);
            setNumericCell(row, col++, dto.getOctMin(), dataStyle);
            setNumericCell(row, col++, dto.getOctMax(), dataStyle);
            setNumericCell(row, col++, dto.getNovMin(), dataStyle);
            setNumericCell(row, col++, dto.getNovMax(), dataStyle);
            setNumericCell(row, col++, dto.getDecMin(), dataStyle);
            setNumericCell(row, col++, dto.getDecMax(), dataStyle);
            setNumericCell(row, col++, dto.getJanMin(), dataStyle);
            setNumericCell(row, col++, dto.getJanMax(), dataStyle);
            setNumericCell(row, col++, dto.getFebMin(), dataStyle);
            setNumericCell(row, col++, dto.getFebMax(), dataStyle);
            setNumericCell(row, col++, dto.getMarMin(), dataStyle);
            setNumericCell(row, col++, dto.getMarMax(), dataStyle);

            createCell(row, col++, dto.getRemarks(), dataStyle);
            createCell(row, col++, dto.getId() != null ? dto.getId().toString() : "", dataStyle);
            createCell(row, col++, dto.getAssetFkId() != null ? dto.getAssetFkId().toString() : "", dataStyle);
            createCell(row, col++, dto.getAssetCategory(), dataStyle);
        }

        sheet.setColumnHidden(idCol, true);
        sheet.setColumnHidden(assetFkIdCol, true);
        sheet.setColumnHidden(assetCategoryCol, true);

        for (int i = 0; i < totalColumns; i++) {
            if (i == remarksCol) {
                sheet.setColumnWidth(i, 8000);
                continue;
            }
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    @Override
    public AOPMessageVM importPowerAssetCapacity(List<UUID> plantIds, String aopYear, MultipartFile file) {
        return importAssetCapacity(plantIds, aopYear, file, true);
    }

    @Override
    public AOPMessageVM importSteamAssetCapacity(List<UUID> plantIds, String aopYear, MultipartFile file) {
        return importAssetCapacity(plantIds, aopYear, file, false);
    }

    private AOPMessageVM importAssetCapacity(List<UUID> plantIds, String aopYear, MultipartFile file, boolean isPower) {
        logger.info("[Import {} Capacity] Importing for plantIds: {}, aopYear: {}", isPower ? "Power" : "Steam", plantIds, aopYear);
        AOPMessageVM response = new AOPMessageVM();
        try {
            List<CPPAssetCapacityResponseDto> excelData = readAssetCapacityExcel(file.getInputStream(), aopYear);
            logger.info("[Import {} Capacity] Read {} records from Excel", isPower ? "Power" : "Steam", excelData.size());

            List<CPPAssetCapacityResponseDto> validRecords = new ArrayList<>();
            List<CPPAssetCapacityResponseDto> failedRecords = new ArrayList<>();
            List<String> failureReasons = new ArrayList<>();
            int skippedCount = 0;

            for (CPPAssetCapacityResponseDto dto : excelData) {
                if (!isCapacityRecordModifiedForImport(dto, isPower)) {
                    skippedCount++;
                    logger.debug("[Import {} Capacity] Skipping unchanged record: {}", isPower ? "Power" : "Steam", dto.getAssetName());
                    continue;
                }

                String validationError = validateAssetCapacityData(dto, isPower);
                if (validationError != null) {
                    failedRecords.add(dto);
                    failureReasons.add(validationError);
                    logger.warn("[Import {} Capacity] Invalid record - {}: {}", isPower ? "Power" : "Steam", dto.getAssetName(), validationError);
                } else {
                    validRecords.add(dto);
                }
            }

            logger.info("[Import {} Capacity] {} unchanged, {} modified to process", isPower ? "Power" : "Steam", skippedCount, excelData.size() - skippedCount);

            if (!validRecords.isEmpty()) {
                try {
                    AssetCapacityRequestDTO payload = new AssetCapacityRequestDTO();
                    if (isPower) {
                        payload.setPowerResponse(validRecords);
                    } else {
                        payload.setSteamResponse(validRecords);
                    }
                    AOPMessageVM saveResult = saveAssetCapacities(plantIds, aopYear, payload);
                    if (saveResult.getCode() == 207) {
                        logger.warn("[Import {} Capacity] Some records skipped during save", isPower ? "Power" : "Steam");
                    }
                    logger.info("[Import {} Capacity] Successfully processed {} records", isPower ? "Power" : "Steam", validRecords.size());
                } catch (Exception e) {
                    logger.error("[Import {} Capacity] Error saving records: {}", isPower ? "Power" : "Steam", e.getMessage(), e);
                    for (CPPAssetCapacityResponseDto failedDto : validRecords) {
                        failedRecords.add(failedDto);
                        failureReasons.add("Save failed: " + e.getMessage());
                    }
                }
            }

            if (failedRecords.isEmpty()) {
                response.setCode(200);
                if (validRecords.isEmpty() && skippedCount > 0) {
                    response.setMessage("No changes detected. All " + skippedCount + " records unchanged.");
                } else {
                    response.setMessage("All asset capacities imported successfully. " + skippedCount + " unchanged, " + validRecords.size() + " updated.");
                }
            } else {
                byte[] failedRecordsFile = generateAssetCapacityErrorExcel(failedRecords, failureReasons,
                        (isPower ? "Power" : "Steam") + " Asset Capacity", aopYear);
                String base64File = java.util.Base64.getEncoder().encodeToString(failedRecordsFile);
                response.setCode(400);
                response.setMessage("Partial import: " + validRecords.size() + " saved, " + failedRecords.size()
                        + " failed, " + skippedCount + " unchanged. Download file for details.");
                response.setData(base64File);
                logger.info("[Import {} Capacity] Exported {} failed records to Excel", isPower ? "Power" : "Steam", failedRecords.size());
            }

            logger.info("[Import {} Capacity] Completed - Unchanged: {}, Saved: {}, Failed: {}",
                    isPower ? "Power" : "Steam", skippedCount, validRecords.size(), failedRecords.size());
        } catch (Exception e) {
            logger.error("[Import {} Capacity] Error during import: {}", isPower ? "Power" : "Steam", e.getMessage(), e);
            response.setCode(500);
            response.setMessage("Failed to import asset capacities: " + e.getMessage());
        }
        return response;
    }

    private boolean isCapacityRecordModifiedForImport(CPPAssetCapacityResponseDto dto, boolean isPower) {
        if (dto.getId() == null) {
            return true;
        }
        try {
            var optionalEntity = isPower ? repository.findById(dto.getId()) : steamRepository.findById(dto.getId());
            if (optionalEntity.isEmpty()) {
                return true;
            }
            return isCapacityRecordModified(dto, optionalEntity.get());
        } catch (Exception e) {
            logger.error("[isCapacityRecordModifiedForImport] Error checking record ID {}: {}", dto.getId(), e.getMessage());
            return true;
        }
    }

    private String validateAssetCapacityData(CPPAssetCapacityResponseDto dto, boolean isPower) {
        if (dto.getId() == null) {
            return "Record ID is missing";
        }
        if (dto.getAssetName() == null || dto.getAssetName().trim().isEmpty()) {
            return "Asset name is required";
        }
        if (dto.getRemarks() == null || dto.getRemarks().trim().isEmpty()) {
            return "Remarks field is mandatory and cannot be empty";
        }
        try {
            String dbRemarks;
            if (isPower) {
                var optionalEntity = repository.findById(dto.getId());
                if (optionalEntity.isEmpty()) {
                    return "Record with this ID does not exist in database";
                }
                dbRemarks = optionalEntity.get().getRemarks();
            } else {
                var optionalEntity = steamRepository.findById(dto.getId());
                if (optionalEntity.isEmpty()) {
                    return "Record with this ID does not exist in database";
                }
                dbRemarks = optionalEntity.get().getRemarks();
            }
            dbRemarks = dbRemarks != null ? dbRemarks.trim() : "";
            String importedRemarks = dto.getRemarks() != null ? dto.getRemarks().trim() : "";
            if (dbRemarks.equals(importedRemarks)) {
                return "Remarks must be updated to explain the changes. Current remarks are identical to the database value.";
            }
        } catch (Exception e) {
            logger.error("[Validation] Error checking remarks for ID {}: {}", dto.getId(), e.getMessage());
        }
        return null;
    }

    private List<CPPAssetCapacityResponseDto> readAssetCapacityExcel(InputStream inputStream, String aopYear) throws Exception {
        List<CPPAssetCapacityResponseDto> records = new ArrayList<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                boolean isRowEmpty = true;
                for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
                    Cell cell = row.getCell(cellIndex);
                    if (cell != null && !cell.toString().trim().isEmpty()) {
                        isRowEmpty = false;
                        break;
                    }
                }
                if (isRowEmpty) continue;

                CPPAssetCapacityResponseDto dto = new CPPAssetCapacityResponseDto();
                int col = 0;
                dto.setAssetName(getCellValue(row, col++));
                dto.setAssetType(getCellValue(row, col++));
                dto.setPlantName(getCellValue(row, col++));
                dto.setFixedMin(getCellDoubleValue(row, col++));
                dto.setFixedMax(getCellDoubleValue(row, col++));
                dto.setUom(getCellValue(row, col++));

                dto.setAprMin(getCellDoubleValue(row, col++));
                dto.setAprMax(getCellDoubleValue(row, col++));
                dto.setMayMin(getCellDoubleValue(row, col++));
                dto.setMayMax(getCellDoubleValue(row, col++));
                dto.setJunMin(getCellDoubleValue(row, col++));
                dto.setJunMax(getCellDoubleValue(row, col++));
                dto.setJulMin(getCellDoubleValue(row, col++));
                dto.setJulMax(getCellDoubleValue(row, col++));
                dto.setAugMin(getCellDoubleValue(row, col++));
                dto.setAugMax(getCellDoubleValue(row, col++));
                dto.setSepMin(getCellDoubleValue(row, col++));
                dto.setSepMax(getCellDoubleValue(row, col++));
                dto.setOctMin(getCellDoubleValue(row, col++));
                dto.setOctMax(getCellDoubleValue(row, col++));
                dto.setNovMin(getCellDoubleValue(row, col++));
                dto.setNovMax(getCellDoubleValue(row, col++));
                dto.setDecMin(getCellDoubleValue(row, col++));
                dto.setDecMax(getCellDoubleValue(row, col++));
                dto.setJanMin(getCellDoubleValue(row, col++));
                dto.setJanMax(getCellDoubleValue(row, col++));
                dto.setFebMin(getCellDoubleValue(row, col++));
                dto.setFebMax(getCellDoubleValue(row, col++));
                dto.setMarMin(getCellDoubleValue(row, col++));
                dto.setMarMax(getCellDoubleValue(row, col++));

                dto.setRemarks(getCellValue(row, col++));

                String idStr = getCellValue(row, col++);
                if (idStr != null && !idStr.trim().isEmpty()) {
                    try {
                        dto.setId(UUID.fromString(idStr));
                    } catch (IllegalArgumentException e) {
                        logger.warn("[IMPORT] Invalid UUID for ID: {}", idStr);
                    }
                }

                String assetFkIdStr = getCellValue(row, col++);
                if (assetFkIdStr != null && !assetFkIdStr.trim().isEmpty()) {
                    try {
                        dto.setAssetFkId(UUID.fromString(assetFkIdStr));
                    } catch (IllegalArgumentException e) {
                        logger.warn("[IMPORT] Invalid UUID for assetFkId: {}", assetFkIdStr);
                    }
                }

                dto.setAssetCategory(getCellValue(row, col++));
                records.add(dto);
            }
        }
        return records;
    }

    private byte[] generateAssetCapacityErrorExcel(List<CPPAssetCapacityResponseDto> failedRecords,
            List<String> failureReasons, String sheetName, String aopYear) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle errorStyle = createErrorCellStyle(workbook);

            String startYearSuffix = aopYear.substring(2, 4);
            String endYearSuffix = aopYear.substring(5, 7);

            Row headerRow = sheet.createRow(0);
            int col = 0;
            String[] staticHeaders = {"Asset Name", "Asset Type", "Plant Name", "Fixed Min", "Fixed Max", "UOM"};
            for (String h : staticHeaders) {
                Cell cell = headerRow.createCell(col++);
                cell.setCellValue(h);
                cell.setCellStyle(headerStyle);
            }

            String[] months = {"Apr-" + startYearSuffix, "May-" + startYearSuffix, "Jun-" + startYearSuffix,
                    "Jul-" + startYearSuffix, "Aug-" + startYearSuffix, "Sep-" + startYearSuffix,
                    "Oct-" + startYearSuffix, "Nov-" + startYearSuffix, "Dec-" + startYearSuffix,
                    "Jan-" + endYearSuffix, "Feb-" + endYearSuffix, "Mar-" + endYearSuffix};

            for (String month : months) {
                Cell cell = headerRow.createCell(col++);
                cell.setCellValue(month + " Min");
                cell.setCellStyle(headerStyle);
                cell = headerRow.createCell(col++);
                cell.setCellValue(month + " Max");
                cell.setCellStyle(headerStyle);
            }

            Cell cell = headerRow.createCell(col++);
            cell.setCellValue("Remarks");
            cell.setCellStyle(headerStyle);
            cell = headerRow.createCell(col++);
            cell.setCellValue("id");
            cell.setCellStyle(headerStyle);
            cell = headerRow.createCell(col++);
            cell.setCellValue("assetFkId");
            cell.setCellStyle(headerStyle);
            cell = headerRow.createCell(col++);
            cell.setCellValue("Status");
            cell.setCellStyle(headerStyle);
            cell = headerRow.createCell(col++);
            cell.setCellValue("Comment");
            cell.setCellStyle(headerStyle);

            int rowNum = 1;
            for (int i = 0; i < failedRecords.size(); i++) {
                CPPAssetCapacityResponseDto dto = failedRecords.get(i);
                String failureReason = failureReasons.get(i);
                Row row = sheet.createRow(rowNum++);
                col = 0;

                createCell(row, col++, dto.getAssetName(), dataStyle);
                createCell(row, col++, dto.getAssetType(), dataStyle);
                createCell(row, col++, dto.getPlantName(), dataStyle);
                setNumericCell(row, col++, dto.getFixedMin(), dataStyle);
                setNumericCell(row, col++, dto.getFixedMax(), dataStyle);
                createCell(row, col++, dto.getUom(), dataStyle);

                setNumericCell(row, col++, dto.getAprMin(), dataStyle);
                setNumericCell(row, col++, dto.getAprMax(), dataStyle);
                setNumericCell(row, col++, dto.getMayMin(), dataStyle);
                setNumericCell(row, col++, dto.getMayMax(), dataStyle);
                setNumericCell(row, col++, dto.getJunMin(), dataStyle);
                setNumericCell(row, col++, dto.getJunMax(), dataStyle);
                setNumericCell(row, col++, dto.getJulMin(), dataStyle);
                setNumericCell(row, col++, dto.getJulMax(), dataStyle);
                setNumericCell(row, col++, dto.getAugMin(), dataStyle);
                setNumericCell(row, col++, dto.getAugMax(), dataStyle);
                setNumericCell(row, col++, dto.getSepMin(), dataStyle);
                setNumericCell(row, col++, dto.getSepMax(), dataStyle);
                setNumericCell(row, col++, dto.getOctMin(), dataStyle);
                setNumericCell(row, col++, dto.getOctMax(), dataStyle);
                setNumericCell(row, col++, dto.getNovMin(), dataStyle);
                setNumericCell(row, col++, dto.getNovMax(), dataStyle);
                setNumericCell(row, col++, dto.getDecMin(), dataStyle);
                setNumericCell(row, col++, dto.getDecMax(), dataStyle);
                setNumericCell(row, col++, dto.getJanMin(), dataStyle);
                setNumericCell(row, col++, dto.getJanMax(), dataStyle);
                setNumericCell(row, col++, dto.getFebMin(), dataStyle);
                setNumericCell(row, col++, dto.getFebMax(), dataStyle);
                setNumericCell(row, col++, dto.getMarMin(), dataStyle);
                setNumericCell(row, col++, dto.getMarMax(), dataStyle);

                createCell(row, col++, dto.getRemarks(), dataStyle);
                createCell(row, col++, dto.getId() != null ? dto.getId().toString() : "", dataStyle);
                createCell(row, col++, dto.getAssetFkId() != null ? dto.getAssetFkId().toString() : "", dataStyle);
                createCell(row, col++, "Failed", errorStyle);
                createCell(row, col++, failureReason, errorStyle);
            }

            for (int i = 0; i < col; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
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

    private String getCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            default:
                return null;
        }
    }

    private Double getCellDoubleValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
        if (cell.getCellType() == CellType.STRING) {
            try {
                String val = cell.getStringCellValue().trim();
                if (!val.isEmpty()) {
                    return Double.parseDouble(val);
                }
            } catch (NumberFormatException e) {
                logger.warn("[IMPORT] Cannot parse double from cell at index {}", cellIndex);
            }
        }
        return null;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
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

    private CellStyle createErrorCellStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        Font font = workbook.createFont();
        font.setColor(IndexedColors.RED.getIndex());
        font.setBold(true);
        style.setFont(font);
        style.setWrapText(true);
        return style;
    }
}
