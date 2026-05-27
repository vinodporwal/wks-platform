package com.wks.caseengine.cpp.serviceimpl;

import com.wks.caseengine.dto.CPPAssetOperationalHoursResponseDto;
import com.wks.caseengine.dto.JMDOperationalHoursRequestDTO;
import com.wks.caseengine.cpp.dto.CPPAssetOperationalHoursProjection;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
            
            // Monthly hours with formulas
            setMonthCellValuesWithFormulas(row, col, dto.getApr(), totalHoursByMonth.get("apr"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, dto.getMay(), totalHoursByMonth.get("may"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, dto.getJun(), totalHoursByMonth.get("jun"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, dto.getJul(), totalHoursByMonth.get("jul"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, dto.getAug(), totalHoursByMonth.get("aug"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, dto.getSep(), totalHoursByMonth.get("sep"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, dto.getOct(), totalHoursByMonth.get("oct"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, dto.getNov(), totalHoursByMonth.get("nov"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, dto.getDec(), totalHoursByMonth.get("dec"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, dto.getJan(), totalHoursByMonth.get("jan"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, dto.getFeb(), totalHoursByMonth.get("feb"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            setMonthCellValuesWithFormulas(row, col, dto.getMar(), totalHoursByMonth.get("mar"), excelRowNum, unlockedStyle, lockedStyle);
            col += 2;
            
            createCell(row, col++, dto.getRemarks(), editableRemarksStyle);
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
    
    private Map<String, Double> calculateTotalAvailableHours(String financialYear) {
        Map<String, Double> totalHours = new LinkedHashMap<>();
        
        String[] parts = financialYear.split("-");
        int startYear = Integer.parseInt(parts[0]);
        int endYear = Integer.parseInt(parts[1]);
        
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
    
    private void setMonthCellValuesWithFormulas(Row row, int startCol, Double operationalHours, Double totalHours, int rowNum, CellStyle unlockedStyle, CellStyle lockedStyle) {
        // Shutdown Hrs cell - editable, with validation
        Cell shutdownCell = row.createCell(startCol);
        if (operationalHours != null && totalHours != null) {
            shutdownCell.setCellValue(totalHours - operationalHours);
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
            List<String> invalidRecords = new ArrayList<>();
            
            for (CPPAssetOperationalHoursResponseDto dto : excelData) {
                String validationError = validateShutdownHoursData(dto, totalHoursByMonth);
                if (validationError != null) {
                    invalidRecords.add(dto.getAssetName() + ": " + validationError);
                    logger.warn("[Import Power Operational Hours] Invalid record: {}", validationError);
                } else {
                    // Calculate operational hours from shutdown hours
                    calculateOperationalHoursFromShutdown(dto, totalHoursByMonth);
                    validRecords.add(dto);
                }
            }
            
            // Save valid records
            if (!validRecords.isEmpty()) {
                JMDOperationalHoursRequestDTO payload = new JMDOperationalHoursRequestDTO();
                payload.setPowerResponse(validRecords);
                saveOperationalHours(plantIds, financialYear, payload);
                logger.info("[Import Power Operational Hours] Successfully saved {} records", validRecords.size());
            }
            
            // Prepare response
            if (invalidRecords.isEmpty()) {
                response.setCode(200);
                response.setMessage("All power operational hours imported successfully");
            } else {
                // Export failed records to Excel file
                byte[] failedRecordsFile = exportPowerOperationalHours(plantIds, financialYear);
                String base64File = java.util.Base64.getEncoder().encodeToString(failedRecordsFile);
                response.setCode(400);
                response.setMessage("Partial import: " + validRecords.size() + " saved, " + invalidRecords.size() + " failed. Download file for details.");
                response.setData(base64File);
                logger.info("[Import Power Operational Hours] Exported {} failed records to Excel", invalidRecords.size());
            }
            
            logger.info("[Import Power Operational Hours] Import completed - Valid: {}, Invalid: {}", validRecords.size(), invalidRecords.size());
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
            List<String> invalidRecords = new ArrayList<>();
            
            for (CPPAssetOperationalHoursResponseDto dto : excelData) {
                String validationError = validateShutdownHoursData(dto, totalHoursByMonth);
                if (validationError != null) {
                    invalidRecords.add(dto.getAssetName() + ": " + validationError);
                    logger.warn("[Import Steam Operational Hours] Invalid record: {}", validationError);
                } else {
                    // Calculate operational hours from shutdown hours
                    calculateOperationalHoursFromShutdown(dto, totalHoursByMonth);
                    validRecords.add(dto);
                }
            }
            
            // Save valid records
            if (!validRecords.isEmpty()) {
                JMDOperationalHoursRequestDTO payload = new JMDOperationalHoursRequestDTO();
                payload.setSteamResponse(validRecords);
                saveOperationalHours(plantIds, financialYear, payload);
                logger.info("[Import Steam Operational Hours] Successfully saved {} records", validRecords.size());
            }
            
            // Prepare response
            if (invalidRecords.isEmpty()) {
                response.setCode(200);
                response.setMessage("All steam operational hours imported successfully");
            } else {
                // Export failed records to Excel file
                byte[] failedRecordsFile = exportSteamOperationalHours(plantIds, financialYear);
                String base64File = java.util.Base64.getEncoder().encodeToString(failedRecordsFile);
                response.setCode(400);
                response.setMessage("Partial import: " + validRecords.size() + " saved, " + invalidRecords.size() + " failed. Download file for details.");
                response.setData(base64File);
                logger.info("[Import Steam Operational Hours] Exported {} failed records to Excel", invalidRecords.size());
            }
            
            logger.info("[Import Steam Operational Hours] Import completed - Valid: {}, Invalid: {}", validRecords.size(), invalidRecords.size());
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
        
        // Validate each month's shutdown hours
        if (dto.getApr() != null && dto.getApr() > totalHoursByMonth.get("apr")) {
            return "April shutdown hours (" + dto.getApr() + ") exceeds total available (" + totalHoursByMonth.get("apr") + ")";
        }
        if (dto.getMay() != null && dto.getMay() > totalHoursByMonth.get("may")) {
            return "May shutdown hours (" + dto.getMay() + ") exceeds total available (" + totalHoursByMonth.get("may") + ")";
        }
        if (dto.getJun() != null && dto.getJun() > totalHoursByMonth.get("jun")) {
            return "June shutdown hours (" + dto.getJun() + ") exceeds total available (" + totalHoursByMonth.get("jun") + ")";
        }
        if (dto.getJul() != null && dto.getJul() > totalHoursByMonth.get("jul")) {
            return "July shutdown hours (" + dto.getJul() + ") exceeds total available (" + totalHoursByMonth.get("jul") + ")";
        }
        if (dto.getAug() != null && dto.getAug() > totalHoursByMonth.get("aug")) {
            return "August shutdown hours (" + dto.getAug() + ") exceeds total available (" + totalHoursByMonth.get("aug") + ")";
        }
        if (dto.getSep() != null && dto.getSep() > totalHoursByMonth.get("sep")) {
            return "September shutdown hours (" + dto.getSep() + ") exceeds total available (" + totalHoursByMonth.get("sep") + ")";
        }
        if (dto.getOct() != null && dto.getOct() > totalHoursByMonth.get("oct")) {
            return "October shutdown hours (" + dto.getOct() + ") exceeds total available (" + totalHoursByMonth.get("oct") + ")";
        }
        if (dto.getNov() != null && dto.getNov() > totalHoursByMonth.get("nov")) {
            return "November shutdown hours (" + dto.getNov() + ") exceeds total available (" + totalHoursByMonth.get("nov") + ")";
        }
        if (dto.getDec() != null && dto.getDec() > totalHoursByMonth.get("dec")) {
            return "December shutdown hours (" + dto.getDec() + ") exceeds total available (" + totalHoursByMonth.get("dec") + ")";
        }
        if (dto.getJan() != null && dto.getJan() > totalHoursByMonth.get("jan")) {
            return "January shutdown hours (" + dto.getJan() + ") exceeds total available (" + totalHoursByMonth.get("jan") + ")";
        }
        if (dto.getFeb() != null && dto.getFeb() > totalHoursByMonth.get("feb")) {
            return "February shutdown hours (" + dto.getFeb() + ") exceeds total available (" + totalHoursByMonth.get("feb") + ")";
        }
        if (dto.getMar() != null && dto.getMar() > totalHoursByMonth.get("mar")) {
            return "March shutdown hours (" + dto.getMar() + ") exceeds total available (" + totalHoursByMonth.get("mar") + ")";
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
}
