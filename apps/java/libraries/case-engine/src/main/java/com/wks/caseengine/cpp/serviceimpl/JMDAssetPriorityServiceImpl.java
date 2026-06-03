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

import java.io.ByteArrayOutputStream;
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

    /**
     * Check if a priority record has been modified compared to DB
     * Compares all monthly priority values (Apr-Mar) and remarks
     */
    private boolean isPriorityRecordModified(CPPAssetPriorityResponseDto dto, Object entity) {
        try {
            if (entity instanceof CPPPowerAssetPriority) {
                CPPPowerAssetPriority db = (CPPPowerAssetPriority) entity;
                if (!Objects.equals(dto.getApr(), db.getApr())) return true;
                if (!Objects.equals(dto.getMay(), db.getMay())) return true;
                if (!Objects.equals(dto.getJun(), db.getJun())) return true;
                if (!Objects.equals(dto.getJul(), db.getJul())) return true;
                if (!Objects.equals(dto.getAug(), db.getAug())) return true;
                if (!Objects.equals(dto.getSep(), db.getSep())) return true;
                if (!Objects.equals(dto.getOct(), db.getOct())) return true;
                if (!Objects.equals(dto.getNov(), db.getNov())) return true;
                if (!Objects.equals(dto.getDec(), db.getDec())) return true;
                if (!Objects.equals(dto.getJan(), db.getJan())) return true;
                if (!Objects.equals(dto.getFeb(), db.getFeb())) return true;
                if (!Objects.equals(dto.getMar(), db.getMar())) return true;
                if (!Objects.equals(dto.getRemarks(), db.getRemarks())) return true;
                return false; // All fields match - not modified
            } else if (entity instanceof CPPSteamAssetPriority) {
                CPPSteamAssetPriority db = (CPPSteamAssetPriority) entity;
                if (!Objects.equals(dto.getApr(), db.getApr())) return true;
                if (!Objects.equals(dto.getMay(), db.getMay())) return true;
                if (!Objects.equals(dto.getJun(), db.getJun())) return true;
                if (!Objects.equals(dto.getJul(), db.getJul())) return true;
                if (!Objects.equals(dto.getAug(), db.getAug())) return true;
                if (!Objects.equals(dto.getSep(), db.getSep())) return true;
                if (!Objects.equals(dto.getOct(), db.getOct())) return true;
                if (!Objects.equals(dto.getNov(), db.getNov())) return true;
                if (!Objects.equals(dto.getDec(), db.getDec())) return true;
                if (!Objects.equals(dto.getJan(), db.getJan())) return true;
                if (!Objects.equals(dto.getFeb(), db.getFeb())) return true;
                if (!Objects.equals(dto.getMar(), db.getMar())) return true;
                if (!Objects.equals(dto.getRemarks(), db.getRemarks())) return true;
                return false; // All fields match - not modified
            }
            return true; // Unknown entity type - assume modified to be safe
        } catch (Exception e) {
            logger.error("[isPriorityRecordModified] Error comparing record ID {}: {}", dto.getId(), e.getMessage());
            return true; // On error, assume modified
        }
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
            int powerUnchanged = 0;
            int steamUnchanged = 0;
            
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
                    
                    // Check if record was actually modified
                    var optionalEntity = repository.findById(dto.getId());
                    if (optionalEntity.isPresent() && !isPriorityRecordModified(dto, optionalEntity.get())) {
                        powerUnchanged++;
                        logger.debug("[POST Service - Power] Skipping unchanged record: {}", dto.getAssetName());
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
                logger.info("[POST Service] Power assets - Saved: {}, Unchanged: {}, Skipped: {}", powerSaved, powerUnchanged, powerSkipped);
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
                    
                    // Check if record was actually modified
                    var optionalEntity = steamRepository.findById(dto.getId());
                    if (optionalEntity.isPresent() && !isPriorityRecordModified(dto, optionalEntity.get())) {
                        steamUnchanged++;
                        logger.debug("[POST Service - Steam] Skipping unchanged record: {}", dto.getAssetName());
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
                logger.info("[POST Service] Steam assets - Saved: {}, Unchanged: {}, Skipped: {}", steamSaved, steamUnchanged, steamSkipped);
            } else {
                logger.info("[POST Service] No steam asset priorities to process");
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
                    aopMessageVM.setMessage("Asset priorities saved successfully. " + (powerUnchanged + steamUnchanged) + " unchanged, " + (powerSaved + steamSaved) + " updated.");
                }
            } else {
                aopMessageVM.setCode(207); // 207 Multi-Status - partial success
                aopMessageVM.setMessage(String.format("Partial success: %d saved, %d unchanged, %d skipped (missing or invalid IDs)", 
                    powerSaved + steamSaved, powerUnchanged + steamUnchanged, powerSkipped + steamSkipped));
            }
            
            aopMessageVM.setData(data);
            logger.info("[POST Service] Save operation completed - Saved: {} (Power: {}, Steam: {}), Unchanged: {} (Power: {}, Steam: {}), Skipped: {} (Power: {}, Steam: {})", 
                powerSaved + steamSaved, powerSaved, steamSaved, 
                powerUnchanged + steamUnchanged, powerUnchanged, steamUnchanged,
                powerSkipped + steamSkipped, powerSkipped, steamSkipped);

        } catch (Exception e) {
            logger.error("[POST Service] Error saving asset priorities: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to save asset priorities: " + e.getMessage());
            aopMessageVM.setData(null);
        }

        return aopMessageVM;
    }

    @Override
    public byte[] exportPowerAssetPriority(List<UUID> plantIds, String aopYear) {
        logger.info("[Export Power Priority] Exporting power asset priorities for plantIds: {}, aopYear: {}", plantIds, aopYear);
        
        try {
            AOPMessageVM response = getAssetPrioritiesForPlants(plantIds, aopYear);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.getData();
            
            @SuppressWarnings("unchecked")
            List<CPPAssetPriorityResponseDto> powerData = 
                (List<CPPAssetPriorityResponseDto>) data.get("PowerAssetPriorities");
            
            // Sort by plantName first, then by assetType
            if (powerData != null && !powerData.isEmpty()) {
                powerData.sort(Comparator
                    .comparing(CPPAssetPriorityResponseDto::getPlantName, 
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(CPPAssetPriorityResponseDto::getAssetType, 
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                logger.debug("[Export Power Priority] Sorted {} power assets by plantName and assetType", powerData.size());
            }
            
            logger.info("[Export Power Priority] Generating Excel for {} power assets", powerData != null ? powerData.size() : 0);
            
            return generatePriorityExcel(powerData, "Power Asset Priority", aopYear);
            
        } catch (Exception e) {
            logger.error("[Export Power Priority] Error exporting power asset priorities: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public byte[] exportSteamAssetPriority(List<UUID> plantIds, String aopYear) {
        logger.info("[Export Steam Priority] Exporting steam asset priorities for plantIds: {}, aopYear: {}", plantIds, aopYear);
        
        try {
            AOPMessageVM response = getAssetPrioritiesForPlants(plantIds, aopYear);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.getData();
            
            @SuppressWarnings("unchecked")
            List<CPPAssetPriorityResponseDto> steamData = 
                (List<CPPAssetPriorityResponseDto>) data.get("SteamAssetPriorities");
            
            // Sort by plantName first, then by assetType
            if (steamData != null && !steamData.isEmpty()) {
                steamData.sort(Comparator
                    .comparing(CPPAssetPriorityResponseDto::getPlantName, 
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(CPPAssetPriorityResponseDto::getAssetType, 
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                logger.debug("[Export Steam Priority] Sorted {} steam assets by plantName and assetType", steamData.size());
            }
            
            logger.info("[Export Steam Priority] Generating Excel for {} steam assets", steamData != null ? steamData.size() : 0);
            
            return generatePriorityExcel(steamData, "Steam Asset Priority", aopYear);
            
        } catch (Exception e) {
            logger.error("[Export Steam Priority] Error exporting steam asset priorities: {}", e.getMessage(), e);
            return null;
        }
    }

    private byte[] generatePriorityExcel(List<CPPAssetPriorityResponseDto> dataList, String sheetName, String aopYear) throws Exception {
        logger.info("[Excel Generation] Creating workbook for sheet: {} with {} records", sheetName, dataList != null ? dataList.size() : 0);
        
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle remarksStyle = createRemarksStyle(workbook);

        String startYearSuffix = aopYear.substring(2, 4);
        String endYearSuffix = aopYear.substring(5, 7);
        
        int currentRow = 0;
        int col = 0;

        // Create header row
        Row headerRow = sheet.createRow(currentRow++);
        col = 0;
        
        // Static columns
        createCell(headerRow, col++, "Asset Name", headerStyle);
        createCell(headerRow, col++, "Asset Type", headerStyle);
        createCell(headerRow, col++, "Plant Name", headerStyle);
        
        // Month headers
        String[] months = {"Apr-" + startYearSuffix, "May-" + startYearSuffix, "Jun-" + startYearSuffix, "Jul-" + startYearSuffix,
                "Aug-" + startYearSuffix, "Sep-" + startYearSuffix, "Oct-" + startYearSuffix, "Nov-" + startYearSuffix,
                "Dec-" + startYearSuffix, "Jan-" + endYearSuffix, "Feb-" + endYearSuffix, "Mar-" + endYearSuffix};
        
        for (String month : months) {
            createCell(headerRow, col++, month, headerStyle);
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
        
        // Data rows
        int rowCount = 0;
        for (CPPAssetPriorityResponseDto dto : dataList) {
            rowCount++;
            Row row = sheet.createRow(currentRow++);
            col = 0;
            logger.debug("[Excel Generation] Writing row {} for asset: {}", rowCount, dto.getAssetName());

            createCell(row, col++, dto.getAssetName(), dataStyle);
            createCell(row, col++, dto.getAssetType(), dataStyle);
            createCell(row, col++, dto.getPlantName(), dataStyle);
            
            // Monthly priority values
            setNumericCell(row, col++, dto.getApr() != null ? dto.getApr().doubleValue() : null, dataStyle);
            setNumericCell(row, col++, dto.getMay() != null ? dto.getMay().doubleValue() : null, dataStyle);
            setNumericCell(row, col++, dto.getJun() != null ? dto.getJun().doubleValue() : null, dataStyle);
            setNumericCell(row, col++, dto.getJul() != null ? dto.getJul().doubleValue() : null, dataStyle);
            setNumericCell(row, col++, dto.getAug() != null ? dto.getAug().doubleValue() : null, dataStyle);
            setNumericCell(row, col++, dto.getSep() != null ? dto.getSep().doubleValue() : null, dataStyle);
            setNumericCell(row, col++, dto.getOct() != null ? dto.getOct().doubleValue() : null, dataStyle);
            setNumericCell(row, col++, dto.getNov() != null ? dto.getNov().doubleValue() : null, dataStyle);
            setNumericCell(row, col++, dto.getDec() != null ? dto.getDec().doubleValue() : null, dataStyle);
            setNumericCell(row, col++, dto.getJan() != null ? dto.getJan().doubleValue() : null, dataStyle);
            setNumericCell(row, col++, dto.getFeb() != null ? dto.getFeb().doubleValue() : null, dataStyle);
            setNumericCell(row, col++, dto.getMar() != null ? dto.getMar().doubleValue() : null, dataStyle);
            
            createCell(row, col++, dto.getRemarks(), remarksStyle);
            createCell(row, col++, dto.getId() != null ? dto.getId().toString() : "", dataStyle);
            createCell(row, col++, dto.getAssetFkId() != null ? dto.getAssetFkId().toString() : "", dataStyle);
            createCell(row, col++, dto.getAssetCategory(), dataStyle);
        }

        // Hide ID columns
        sheet.setColumnHidden(idCol, true);
        sheet.setColumnHidden(assetFkIdCol, true);
        sheet.setColumnHidden(assetCategoryCol, true);

        // Auto-size columns
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
        
        logger.info("[Excel Generation] Successfully generated Excel with {} data rows", rowCount);
        
        return outputStream.toByteArray();
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

        // Update only monthly priority values (Apr to Mar) and remarks
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
        logger.debug("[POST Service - Power] Successfully updated entity with ID: {} - Monthly priorities and remarks updated", saved.getId());
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

        // Update only monthly priority values (Apr to Mar) and remarks
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
        logger.debug("[POST Service - Steam] Successfully updated entity with ID: {} - Monthly priorities and remarks updated", saved.getId());
        return true;
    }

    @Override
    public AOPMessageVM importPowerAssetPriority(List<UUID> plantIds, String aopYear, MultipartFile file) {
        return importAssetPriority(plantIds, aopYear, file, true);
    }

    @Override
    public AOPMessageVM importSteamAssetPriority(List<UUID> plantIds, String aopYear, MultipartFile file) {
        return importAssetPriority(plantIds, aopYear, file, false);
    }

    private AOPMessageVM importAssetPriority(List<UUID> plantIds, String aopYear, MultipartFile file, boolean isPower) {
        String assetType = isPower ? "Power" : "Steam";
        String sheetName = isPower ? "Power Asset Priority" : "Steam Asset Priority";

        logger.info("[IMPORT Service - {}] Importing {} asset priority from Excel file: {}", assetType, assetType.toLowerCase(), file.getOriginalFilename());
        AOPMessageVM response = new AOPMessageVM();

        try {
            List<CPPAssetPriorityResponseDto> excelData = readAssetPriorityExcel(file.getInputStream(), aopYear);
            logger.info("[IMPORT Service - {}] Read {} records from Excel", assetType, excelData.size());

            List<CPPAssetPriorityResponseDto> validRecords = new ArrayList<>();
            List<CPPAssetPriorityResponseDto> failedRecords = new ArrayList<>();
            List<String> failureReasons = new ArrayList<>();
            int skippedCount = 0;

            for (CPPAssetPriorityResponseDto dto : excelData) {
                if (!isPriorityRecordModifiedForImport(dto, isPower)) {
                    skippedCount++;
                    logger.debug("[IMPORT Service - {}] Skipping unchanged record: {}", assetType, dto.getAssetName());
                    continue;
                }

                String validationError = validateAssetPriorityData(dto, isPower);
                if (validationError != null) {
                    failedRecords.add(dto);
                    failureReasons.add(validationError);
                    logger.warn("[IMPORT Service - {}] Invalid record - {}: {}", assetType, dto.getAssetName(), validationError);
                } else {
                    validRecords.add(dto);
                }
            }

            logger.info("[IMPORT Service - {}] {} records unchanged (skipped), {} modified records to process",
                    assetType, skippedCount, excelData.size() - skippedCount);

            if (!validRecords.isEmpty()) {
                try {
                    AssetPriorityRequestDTO payload = new AssetPriorityRequestDTO();
                    if (isPower) {
                        payload.setPowerResponse(validRecords);
                    } else {
                        payload.setSteamResponse(validRecords);
                    }

                    AOPMessageVM saveResult = saveAssetPriorities(plantIds, aopYear, payload);

                    if (saveResult.getCode() == 207) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> saveData = (Map<String, Object>) saveResult.getData();
                        if (saveData != null && saveData.containsKey("skippedRecords")) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, String>> skippedRecords =
                                    (List<Map<String, String>>) saveData.get("skippedRecords");
                            logger.warn("[IMPORT Service - {}] {} records skipped during save", assetType, skippedRecords.size());
                        }
                    }

                    logger.info("[IMPORT Service - {}] Successfully processed {} records", assetType, validRecords.size());
                } catch (Exception e) {
                    logger.error("[IMPORT Service - {}] Error saving records: {}", assetType, e.getMessage(), e);
                    for (CPPAssetPriorityResponseDto failedDto : validRecords) {
                        failedRecords.add(failedDto);
                        failureReasons.add("Save failed: " + e.getMessage());
                    }
                }
            }

            if (failedRecords.isEmpty()) {
                response.setCode(200);
                if (validRecords.isEmpty() && skippedCount > 0) {
                    response.setMessage("No changes detected in imported records. All " + skippedCount + " records unchanged.");
                } else {
                    response.setMessage("All " + assetType.toLowerCase() + " asset priorities imported successfully. "
                            + skippedCount + " records unchanged, " + validRecords.size() + " records updated.");
                }
            } else {
                byte[] failedRecordsFile = generateExcelWithErrors(failedRecords, failureReasons, sheetName, aopYear);
                String base64File = java.util.Base64.getEncoder().encodeToString(failedRecordsFile);
                response.setCode(400);
                response.setMessage("Partial import: " + validRecords.size() + " saved, " + failedRecords.size()
                        + " failed, " + skippedCount + " unchanged. Download file for details.");
                response.setData(base64File);
                logger.info("[IMPORT Service - {}] Exported {} failed records to Excel", assetType, failedRecords.size());
            }

            logger.info("[IMPORT Service - {}] Import completed - Unchanged: {}, Saved: {}, Failed: {}",
                    assetType, skippedCount, validRecords.size(), failedRecords.size());
        } catch (Exception e) {
            logger.error("[IMPORT Service - {}] Error during import: {}", assetType, e.getMessage(), e);
            response.setCode(500);
            response.setMessage("Failed to import " + assetType.toLowerCase() + " asset priority: " + e.getMessage());
        }

        return response;
    }

    // ========================================
    // HELPER METHODS FOR IMPORT
    // ========================================

    private List<CPPAssetPriorityResponseDto> readAssetPriorityExcel(java.io.InputStream inputStream, String aopYear) throws Exception {
        List<CPPAssetPriorityResponseDto> records = new ArrayList<>();
        
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Export column layout (must stay in sync with generatePriorityExcel):
            // col 0  = Asset Name
            // col 1  = Asset Type
            // col 2  = Plant Name
            // col 3  = Apr
            // col 4  = May
            // col 5  = Jun
            // col 6  = Jul
            // col 7  = Aug
            // col 8  = Sep
            // col 9  = Oct
            // col 10 = Nov
            // col 11 = Dec
            // col 12 = Jan
            // col 13 = Feb
            // col 14 = Mar
            // col 15 = Remarks
            // col 16 = id        (hidden)
            // col 17 = assetFkId (hidden)
            // col 18 = assetCategory (hidden)
            
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
                if (isRowEmpty) {
                    logger.debug("[IMPORT] Skipping empty row: {}", row.getRowNum());
                    continue;
                }
                
                CPPAssetPriorityResponseDto dto = new CPPAssetPriorityResponseDto();
                
                // Parse basic fields from Excel
                dto.setAssetName(getCellValue(row, 0));
                dto.setAssetType(getCellValue(row, 1));
                dto.setPlantName(getCellValue(row, 2));
                // col 1 = Asset Type, col 2 = Plant Name (read-only display fields, not mapped to DTO)
                
                // Parse monthly priority values (Apr to Mar) - columns 3-14
                dto.setApr(getCellIntValue(row, 3));
                dto.setMay(getCellIntValue(row, 4));
                dto.setJun(getCellIntValue(row, 5));
                dto.setJul(getCellIntValue(row, 6));
                dto.setAug(getCellIntValue(row, 7));
                dto.setSep(getCellIntValue(row, 8));
                dto.setOct(getCellIntValue(row, 9));
                dto.setNov(getCellIntValue(row, 10));
                dto.setDec(getCellIntValue(row, 11));
                dto.setJan(getCellIntValue(row, 12));
                dto.setFeb(getCellIntValue(row, 13));
                dto.setMar(getCellIntValue(row, 14));
                
                // Parse remarks - column 15
                dto.setRemarks(getCellValue(row, 15));
                
                // Parse ID (column 16) and assetFkId (column 17) - both hidden in exported Excel
                String idStr = getCellValue(row, 16);
                if (idStr != null && !idStr.trim().isEmpty()) {
                    try {
                        dto.setId(UUID.fromString(idStr));
                    } catch (IllegalArgumentException e) {
                        logger.warn("[IMPORT] Invalid UUID format for ID: {}", idStr);
                    }
                }
                
                String assetFkIdStr = getCellValue(row, 17);
                if (assetFkIdStr != null && !assetFkIdStr.trim().isEmpty()) {
                    try {
                        dto.setAssetFkId(UUID.fromString(assetFkIdStr));
                    } catch (IllegalArgumentException e) {
                        logger.warn("[IMPORT] Invalid UUID format for assetFkId: {}", assetFkIdStr);
                    }
                }
                
                // Parse assetCategory from column 18 (hidden)
                String assetCategory = getCellValue(row, 18);
                if (assetCategory != null && !assetCategory.trim().isEmpty()) {
                    dto.setAssetCategory(assetCategory);
                }
                
                records.add(dto);
            }
        }
        
        return records;
    }

    private boolean isPriorityRecordModifiedForImport(CPPAssetPriorityResponseDto dto, boolean isPower) {
        if (dto.getId() == null) {
            return true;
        }

        try {
            if (isPower) {
                var optionalEntity = repository.findById(dto.getId());
                return optionalEntity.isEmpty() || isPriorityRecordModified(dto, optionalEntity.get());
            }

            var optionalEntity = steamRepository.findById(dto.getId());
            return optionalEntity.isEmpty() || isPriorityRecordModified(dto, optionalEntity.get());
        } catch (Exception e) {
            logger.error("[isPriorityRecordModifiedForImport] Error checking record ID {}: {}", dto.getId(), e.getMessage());
            return true;
        }
    }

    private String validateAssetPriorityData(CPPAssetPriorityResponseDto dto, boolean isPower) {
        if (dto.getId() == null) {
            return "Record ID is missing";
        }

        if (dto.getAssetName() == null || dto.getAssetName().trim().isEmpty()) {
            return "Asset name is required";
        }

        if (dto.getRemarks() == null || dto.getRemarks().trim().isEmpty()) {
            return "Remarks field is mandatory and cannot be empty";
        }
        
        // Validate that at least one monthly priority is set
        if (dto.getApr() == null && dto.getMay() == null && dto.getJun() == null && 
            dto.getJul() == null && dto.getAug() == null && dto.getSep() == null &&
            dto.getOct() == null && dto.getNov() == null && dto.getDec() == null &&
            dto.getJan() == null && dto.getFeb() == null && dto.getMar() == null) {
            return "At least one monthly priority value is required";
        }

        // Validate that no monthly priority value is negative
        java.util.Map<String, Integer> monthValues = new java.util.LinkedHashMap<>();
        monthValues.put("Apr", dto.getApr());
        monthValues.put("May", dto.getMay());
        monthValues.put("Jun", dto.getJun());
        monthValues.put("Jul", dto.getJul());
        monthValues.put("Aug", dto.getAug());
        monthValues.put("Sep", dto.getSep());
        monthValues.put("Oct", dto.getOct());
        monthValues.put("Nov", dto.getNov());
        monthValues.put("Dec", dto.getDec());
        monthValues.put("Jan", dto.getJan());
        monthValues.put("Feb", dto.getFeb());
        monthValues.put("Mar", dto.getMar());
        for (java.util.Map.Entry<String, Integer> entry : monthValues.entrySet()) {
            if (entry.getValue() != null && entry.getValue() < 0) {
                return "Priority value for " + entry.getKey() + " cannot be less than 0";
            }
        }

        try {
            if (isPower) {
                var optionalEntity = repository.findById(dto.getId());
                if (optionalEntity.isEmpty()) {
                    return "Record with this ID does not exist in database";
                }

                String dbRemarks = optionalEntity.get().getRemarks() != null ? optionalEntity.get().getRemarks().trim() : "";
                String importedRemarks = dto.getRemarks() != null ? dto.getRemarks().trim() : "";
                if (dbRemarks.equals(importedRemarks)) {
                    return "Remarks must be updated to explain the changes. Current remarks are identical to the database value.";
                }
            } else {
                var optionalEntity = steamRepository.findById(dto.getId());
                if (optionalEntity.isEmpty()) {
                    return "Record with this ID does not exist in database";
                }

                String dbRemarks = optionalEntity.get().getRemarks() != null ? optionalEntity.get().getRemarks().trim() : "";
                String importedRemarks = dto.getRemarks() != null ? dto.getRemarks().trim() : "";
                if (dbRemarks.equals(importedRemarks)) {
                    return "Remarks must be updated to explain the changes. Current remarks are identical to the database value.";
                }
            }
        } catch (Exception e) {
            logger.error("[Validation] Error checking remarks for ID {}: {}", dto.getId(), e.getMessage());
        }
        
        return null;
    }

    private byte[] generateExcelWithErrors(List<CPPAssetPriorityResponseDto> failedRecords, List<String> failureReasons, String sheetName, String aopYear) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle remarksStyle = createRemarksStyle(workbook);
            CellStyle errorStyle = createErrorCellStyle(workbook);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Asset Name", "Asset Type", "Plant Name", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan", "Feb", "Mar", "Remarks", "id", "assetFkId", "assetCategory", "Status", "Comment"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            int rowNum = 1;
            for (int i = 0; i < failedRecords.size(); i++) {
                CPPAssetPriorityResponseDto dto = failedRecords.get(i);
                String failureReason = failureReasons.get(i);
                Row row = sheet.createRow(rowNum++);
                
                createCell(row, 0, dto.getAssetName(), dataStyle);
                createCell(row, 1, dto.getAssetType(), dataStyle);
                createCell(row, 2, dto.getPlantName(), dataStyle);
                setNumericCell(row, 3, dto.getApr() != null ? dto.getApr().doubleValue() : null, dataStyle);
                setNumericCell(row, 4, dto.getMay() != null ? dto.getMay().doubleValue() : null, dataStyle);
                setNumericCell(row, 5, dto.getJun() != null ? dto.getJun().doubleValue() : null, dataStyle);
                setNumericCell(row, 6, dto.getJul() != null ? dto.getJul().doubleValue() : null, dataStyle);
                setNumericCell(row, 7, dto.getAug() != null ? dto.getAug().doubleValue() : null, dataStyle);
                setNumericCell(row, 8, dto.getSep() != null ? dto.getSep().doubleValue() : null, dataStyle);
                setNumericCell(row, 9, dto.getOct() != null ? dto.getOct().doubleValue() : null, dataStyle);
                setNumericCell(row, 10, dto.getNov() != null ? dto.getNov().doubleValue() : null, dataStyle);
                setNumericCell(row, 11, dto.getDec() != null ? dto.getDec().doubleValue() : null, dataStyle);
                setNumericCell(row, 12, dto.getJan() != null ? dto.getJan().doubleValue() : null, dataStyle);
                setNumericCell(row, 13, dto.getFeb() != null ? dto.getFeb().doubleValue() : null, dataStyle);
                setNumericCell(row, 14, dto.getMar() != null ? dto.getMar().doubleValue() : null, dataStyle);
                createCell(row, 15, dto.getRemarks(), remarksStyle);
                createCell(row, 16, dto.getId() != null ? dto.getId().toString() : "", dataStyle);
                createCell(row, 17, dto.getAssetFkId() != null ? dto.getAssetFkId().toString() : "", dataStyle);
                createCell(row, 18, dto.getAssetCategory(), dataStyle);
                createCell(row, 19, "Failed", errorStyle);
                createCell(row, 20, failureReason, errorStyle);
            }

            sheet.setColumnHidden(16, true);
            sheet.setColumnHidden(17, true);
            sheet.setColumnHidden(18, true);

            for (int i = 0; i < headers.length; i++) {
                if (i == 15 || i == 20) {
                    sheet.setColumnWidth(i, 8000);
                    continue;
                }
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private CellStyle createErrorCellStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
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

    private Integer getCellIntValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;
        
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        return null;
    }
}
