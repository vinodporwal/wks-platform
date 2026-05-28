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
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
}
