package com.wks.caseengine.cpp.serviceimpl;

import com.wks.caseengine.dto.CPPAssetOperationalHoursResponseDto;
import com.wks.caseengine.dto.JMDOperationalHoursRequestDTO;
import com.wks.caseengine.cpp.dto.CPPAssetOperationalHoursProjection;
import com.wks.caseengine.cpp.entity.CPPAssetOperationalHours;
import com.wks.caseengine.cpp.entity.CPPSteamAssetsOperationalHours;
import com.wks.caseengine.cpp.repository.CPPAssetOperationalHoursRepository;
import com.wks.caseengine.cpp.service.JMDAssetsService;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
        logger.debug("[POST Service - Power] Successfully updated entity with ID: {}", saved.getId());
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
        logger.debug("[POST Service - Steam] Successfully updated entity with ID: {}", saved.getId());
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
            
            logger.info("[Export Steam] Generating Excel for {} steam assets", steamData != null ? steamData.size() : 0);
            
            return generateExcel(steamData, "Steam Operational Hours", financialYear);
            
        } catch (Exception e) {
            logger.error("[Export Steam] Error exporting steam operational hours: {}", e.getMessage(), e);
            return null;
        }
    }

    private byte[] generateExcel(List<CPPAssetOperationalHoursResponseDto> dataList, String sheetName, String financialYear) throws Exception {
        logger.debug("[Excel Generation] Creating workbook for sheet: {}", sheetName);
        
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
        
        // Month headers (each month shows only Operational Hrs - no Shut Down Hrs for JMD)
        String[] months = {"Apr-" + startYearSuffix, "May-" + startYearSuffix, "Jun-" + startYearSuffix, "Jul-" + startYearSuffix,
                "Aug-" + startYearSuffix, "Sep-" + startYearSuffix, "Oct-" + startYearSuffix, "Nov-" + startYearSuffix,
                "Dec-" + startYearSuffix, "Jan-" + endYearSuffix, "Feb-" + endYearSuffix, "Mar-" + endYearSuffix};
        
        int monthStartCol = col;
        for (String month : months) {
            createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, month + " (Hrs)", headerStyle);
            col++;
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
        
        // Data rows
        for (CPPAssetOperationalHoursResponseDto dto : dataList) {
            Row row = sheet.createRow(currentRow++);
            col = 0;

            createCell(row, col++, dto.getAssetName(), dataStyle);
            createCell(row, col++, dto.getAssetType(), dataStyle);
            createCell(row, col++, dto.getPlantName(), dataStyle);
            createCell(row, col++, dto.getUtilityDistributed(), dataStyle);
            createCell(row, col++, dto.getDistributedSapCode(), dataStyle);
            createCell(row, col++, dto.getUtilityGenerated(), dataStyle);
            createCell(row, col++, dto.getGeneratedUtilityCode(), dataStyle);
            
            // Monthly operational hours
            setNumericCell(row, col++, dto.getApr(), dataStyle);
            setNumericCell(row, col++, dto.getMay(), dataStyle);
            setNumericCell(row, col++, dto.getJun(), dataStyle);
            setNumericCell(row, col++, dto.getJul(), dataStyle);
            setNumericCell(row, col++, dto.getAug(), dataStyle);
            setNumericCell(row, col++, dto.getSep(), dataStyle);
            setNumericCell(row, col++, dto.getOct(), dataStyle);
            setNumericCell(row, col++, dto.getNov(), dataStyle);
            setNumericCell(row, col++, dto.getDec(), dataStyle);
            setNumericCell(row, col++, dto.getJan(), dataStyle);
            setNumericCell(row, col++, dto.getFeb(), dataStyle);
            setNumericCell(row, col++, dto.getMar(), dataStyle);
            
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
        
        logger.info("[Excel Generation] Successfully generated Excel with {} rows", dataList.size());
        return outputStream.toByteArray();
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
}
