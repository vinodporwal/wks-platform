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

    @Override
    public AOPMessageVM importPowerAssetPriority(List<UUID> plantIds, String aopYear, MultipartFile file) {
        logger.info("[IMPORT Service - Power] Importing power asset priority from Excel file: {}", file.getOriginalFilename());
        AOPMessageVM response = new AOPMessageVM();
        
        try {
            // Read Excel file and parse asset priority data
            List<CPPAssetPriorityResponseDto> excelData = readAssetPriorityExcel(file.getInputStream(), aopYear);
            logger.info("[IMPORT Service - Power] Read {} records from Excel", excelData.size());
            
            List<CPPAssetPriorityResponseDto> validRecords = new ArrayList<>();
            List<CPPAssetPriorityResponseDto> failedRecords = new ArrayList<>();
            Map<String, String> errorMessages = new HashMap<>();
            
            // Validate all records (no category filtering - this is power import endpoint)
            for (CPPAssetPriorityResponseDto dto : excelData) {
                String validationError = validateAssetPriorityData(dto);
                if (validationError != null) {
                    failedRecords.add(dto);
                    errorMessages.put(dto.getAssetName(), validationError);
                    logger.warn("[IMPORT Service - Power] Invalid record - {}: {}", dto.getAssetName(), validationError);
                } else {
                    validRecords.add(dto);
                }
            }
            
            // Try to save valid records
            if (!validRecords.isEmpty()) {
                try {
                    AssetPriorityRequestDTO payload = new AssetPriorityRequestDTO();
                    payload.setPowerResponse(validRecords);
                    AOPMessageVM saveResult = saveAssetPriorities(plantIds, aopYear, payload);
                    
                    if (saveResult.getCode() != 200) {
                        failedRecords.addAll(validRecords);
                    }
                    logger.info("[IMPORT Service - Power] Successfully processed {} records", validRecords.size());
                } catch (Exception e) {
                    logger.error("[IMPORT Service - Power] Error saving records: {}", e.getMessage(), e);
                    failedRecords.addAll(validRecords);
                }
            }
            
            // Prepare response
            if (failedRecords.isEmpty()) {
                response.setCode(200);
                response.setMessage("All power asset priorities imported successfully");
            } else {
                byte[] failedRecordsFile = generateExcelWithErrors(failedRecords, "Power Asset Priority", aopYear);
                String base64File = java.util.Base64.getEncoder().encodeToString(failedRecordsFile);
                response.setCode(400);
                response.setMessage("Partial import: " + (excelData.size() - failedRecords.size()) + " saved, " + failedRecords.size() + " failed. Download file for details.");
                response.setData(base64File);
                logger.info("[IMPORT Service - Power] Exported {} failed records to Excel", failedRecords.size());
            }
            
            logger.info("[IMPORT Service - Power] Import completed - Valid: {}, Failed: {}", validRecords.size(), failedRecords.size());
        } catch (Exception e) {
            logger.error("[IMPORT Service - Power] Error during import: {}", e.getMessage(), e);
            response.setCode(500);
            response.setMessage("Failed to import power asset priority: " + e.getMessage());
        }
        
        return response;
    }

    @Override
    public AOPMessageVM importSteamAssetPriority(List<UUID> plantIds, String aopYear, MultipartFile file) {
        logger.info("[IMPORT Service - Steam] Importing steam asset priority from Excel file: {}", file.getOriginalFilename());
        AOPMessageVM response = new AOPMessageVM();
        
        try {
            // Read Excel file and parse asset priority data
            List<CPPAssetPriorityResponseDto> excelData = readAssetPriorityExcel(file.getInputStream(), aopYear);
            logger.info("[IMPORT Service - Steam] Read {} records from Excel", excelData.size());
            
            List<CPPAssetPriorityResponseDto> validRecords = new ArrayList<>();
            List<CPPAssetPriorityResponseDto> failedRecords = new ArrayList<>();
            Map<String, String> errorMessages = new HashMap<>();
            
            // Validate all records (no category filtering - this is steam import endpoint)
            for (CPPAssetPriorityResponseDto dto : excelData) {
                String validationError = validateAssetPriorityData(dto);
                if (validationError != null) {
                    failedRecords.add(dto);
                    errorMessages.put(dto.getAssetName(), validationError);
                    logger.warn("[IMPORT Service - Steam] Invalid record - {}: {}", dto.getAssetName(), validationError);
                } else {
                    validRecords.add(dto);
                }
            }
            
            // Try to save valid records
            if (!validRecords.isEmpty()) {
                try {
                    AssetPriorityRequestDTO payload = new AssetPriorityRequestDTO();
                    payload.setSteamResponse(validRecords);
                    AOPMessageVM saveResult = saveAssetPriorities(plantIds, aopYear, payload);
                    
                    if (saveResult.getCode() != 200) {
                        failedRecords.addAll(validRecords);
                    }
                    logger.info("[IMPORT Service - Steam] Successfully processed {} records", validRecords.size());
                } catch (Exception e) {
                    logger.error("[IMPORT Service - Steam] Error saving records: {}", e.getMessage(), e);
                    failedRecords.addAll(validRecords);
                }
            }
            
            // Prepare response
            if (failedRecords.isEmpty()) {
                response.setCode(200);
                response.setMessage("All steam asset priorities imported successfully");
            } else {
                byte[] failedRecordsFile = generateExcelWithErrors(failedRecords, "Steam Asset Priority", aopYear);
                String base64File = java.util.Base64.getEncoder().encodeToString(failedRecordsFile);
                response.setCode(400);
                response.setMessage("Partial import: " + (excelData.size() - failedRecords.size()) + " saved, " + failedRecords.size() + " failed. Download file for details.");
                response.setData(base64File);
                logger.info("[IMPORT Service - Steam] Exported {} failed records to Excel", failedRecords.size());
            }
            
            logger.info("[IMPORT Service - Steam] Import completed - Valid: {}, Failed: {}", validRecords.size(), failedRecords.size());
        } catch (Exception e) {
            logger.error("[IMPORT Service - Steam] Error during import: {}", e.getMessage(), e);
            response.setCode(500);
            response.setMessage("Failed to import steam asset priority: " + e.getMessage());
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
                
                CPPAssetPriorityResponseDto dto = new CPPAssetPriorityResponseDto();
                
                // Parse basic fields from Excel
                dto.setAssetName(getCellValue(row, 0));
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

    private String validateAssetPriorityData(CPPAssetPriorityResponseDto dto) {
        if (dto.getAssetName() == null || dto.getAssetName().trim().isEmpty()) {
            return "Asset name is required";
        }
        
        // Validate that at least one monthly priority is set
        if (dto.getApr() == null && dto.getMay() == null && dto.getJun() == null && 
            dto.getJul() == null && dto.getAug() == null && dto.getSep() == null &&
            dto.getOct() == null && dto.getNov() == null && dto.getDec() == null &&
            dto.getJan() == null && dto.getFeb() == null && dto.getMar() == null) {
            return "At least one monthly priority value is required";
        }
        
        return null;
    }

    private byte[] generateExcelWithErrors(List<CPPAssetPriorityResponseDto> failedRecords, String sheetName, String aopYear) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet(sheetName);
            
            // Create header row with Status and Error Message columns
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Asset Name", "Asset Category", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan", "Feb", "Mar", "Remarks", "Status", "Error Message"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderStyle(workbook));
            }
            
            // Add failed records
            int rowNum = 1;
            for (CPPAssetPriorityResponseDto dto : failedRecords) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(dto.getAssetName() != null ? dto.getAssetName() : "");
                row.createCell(1).setCellValue(dto.getAssetCategory() != null ? dto.getAssetCategory() : "");
                row.createCell(2).setCellValue(dto.getApr() != null ? dto.getApr() : 0);
                row.createCell(3).setCellValue(dto.getMay() != null ? dto.getMay() : 0);
                row.createCell(4).setCellValue(dto.getJun() != null ? dto.getJun() : 0);
                row.createCell(5).setCellValue(dto.getJul() != null ? dto.getJul() : 0);
                row.createCell(6).setCellValue(dto.getAug() != null ? dto.getAug() : 0);
                row.createCell(7).setCellValue(dto.getSep() != null ? dto.getSep() : 0);
                row.createCell(8).setCellValue(dto.getOct() != null ? dto.getOct() : 0);
                row.createCell(9).setCellValue(dto.getNov() != null ? dto.getNov() : 0);
                row.createCell(10).setCellValue(dto.getDec() != null ? dto.getDec() : 0);
                row.createCell(11).setCellValue(dto.getJan() != null ? dto.getJan() : 0);
                row.createCell(12).setCellValue(dto.getFeb() != null ? dto.getFeb() : 0);
                row.createCell(13).setCellValue(dto.getMar() != null ? dto.getMar() : 0);
                row.createCell(14).setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
                row.createCell(15).setCellValue("FAILED");
            }
            
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
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
