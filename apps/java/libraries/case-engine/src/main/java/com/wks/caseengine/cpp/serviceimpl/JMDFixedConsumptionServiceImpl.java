package com.wks.caseengine.cpp.serviceimpl;

import com.wks.caseengine.cpp.dto.FixedConsumptionCreateRequestDto;
import com.wks.caseengine.cpp.dto.JMDFixedConsumptionDto;
import com.wks.caseengine.cpp.dto.FixedConsumptionProjection;
import com.wks.caseengine.cpp.entity.CPPFixedConsumption;
import com.wks.caseengine.cpp.repository.JMDFixedConsumptionRepository;
import com.wks.caseengine.cpp.service.JMDFixedConsumptionService;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Comparator;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
public class JMDFixedConsumptionServiceImpl implements JMDFixedConsumptionService {

    private static final Logger logger = LoggerFactory.getLogger(JMDFixedConsumptionServiceImpl.class);

    @Autowired
    private JMDFixedConsumptionRepository repository;

    @Override
    public AOPMessageVM getFixedConsumptionForPlants(List<UUID> plantIds, String financialYear) {

        logger.info("[GET Service] Fetching fixed consumption for plantIds: {}, financialYear: {}", plantIds,
                financialYear);
        AOPMessageVM aopMessageVM = new AOPMessageVM();

        try {
            if (plantIds == null || plantIds.isEmpty()) {
                logger.warn("[GET Service] No plant IDs provided");
                aopMessageVM.setCode(400);
                aopMessageVM.setMessage("Plant IDs are required");
                aopMessageVM.setData(null);
                return aopMessageVM;
            }

            String plantIdsCsv = plantIds.stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(","));
            logger.debug("[GET Service] Executing SP with plantIds: {}", plantIdsCsv);

            List<FixedConsumptionProjection> projections = repository.getFixedConsumptionForPlants(plantIdsCsv,
                    financialYear);

            List<JMDFixedConsumptionDto> allResults = projections.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());

            logger.info("[GET Service] SP returned {} total records from {} plants", allResults.size(),
                    plantIds.size());

            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(allResults);
            logger.info("[GET Service] Successfully fetched fixed consumption data");
        } catch (Exception e) {
            logger.error("[GET Service] Error fetching fixed consumption: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to fetch data: " + e.getMessage());
            aopMessageVM.setData(null);
        }

        return aopMessageVM;
    }

    private JMDFixedConsumptionDto mapToDto(FixedConsumptionProjection p) {
        JMDFixedConsumptionDto dto = new JMDFixedConsumptionDto();

        dto.setId(p.getId()); // SP-returned row Id
        dto.setPlant(p.getPlantName());
        dto.setPlantId(p.getPlantCode());
        dto.setCostCenter(p.getCostCenterName());
        dto.setCostCenterId(p.getCostCenterCode());
        dto.setCppUtility(p.getUtilityName());
        dto.setCppUtilityId(p.getUtilitySAP());
        dto.setCppPlant(p.getUtilityPlantName());
        dto.setCppPlantId(p.getUtilityPlantCode());
        dto.setUom(p.getUom());
        dto.setNormParameterId(p.getNormParameterId());
        dto.setCostCenter_FK_Id(p.getCostCenter_FK_Id());
        dto.setNormParameter_FK_Id(p.getNormParameter_FK_Id());
        dto.setRemarkId(p.getRemarkId());
        dto.setRemarks(p.getRemarks());

        dto.setApril(p.getApr());
        dto.setMay(p.getMay());
        dto.setJune(p.getJun());
        dto.setJuly(p.getJul());
        dto.setAug(p.getAug());
        dto.setSep(p.getSep());
        dto.setOct(p.getOct());
        dto.setNov(p.getNov());
        dto.setDec(p.getDec());
        dto.setJan(p.getJan());
        dto.setFeb(p.getFeb());
        dto.setMar(p.getMar());

        dto.setGrandTotal(
                Optional.ofNullable(p.getApr()).orElse(0.0) +
                        Optional.ofNullable(p.getMay()).orElse(0.0) +
                        Optional.ofNullable(p.getJun()).orElse(0.0) +
                        Optional.ofNullable(p.getJul()).orElse(0.0) +
                        Optional.ofNullable(p.getAug()).orElse(0.0) +
                        Optional.ofNullable(p.getSep()).orElse(0.0) +
                        Optional.ofNullable(p.getOct()).orElse(0.0) +
                        Optional.ofNullable(p.getNov()).orElse(0.0) +
                        Optional.ofNullable(p.getDec()).orElse(0.0) +
                        Optional.ofNullable(p.getJan()).orElse(0.0) +
                        Optional.ofNullable(p.getFeb()).orElse(0.0) +
                        Optional.ofNullable(p.getMar()).orElse(0.0));

        return dto;
    }

    @Override
    public AOPMessageVM saveFixedConsumption(List<UUID> plantIds,
            String financialYear,
            List<JMDFixedConsumptionDto> payload) {
        logger.info("[POST Service] Saving fixed consumption, records: {}",
                payload != null ? payload.size() : 0);
        AOPMessageVM aopMessageVM = new AOPMessageVM();

        try {
            if (payload == null || payload.isEmpty()) {
                aopMessageVM.setCode(400);
                aopMessageVM.setMessage("Payload is empty");
                aopMessageVM.setData(null);
                return aopMessageVM;
            }

            int updated = 0;
            int skipped = 0;

            for (JMDFixedConsumptionDto dto : payload) {
                if (dto.getId() == null) {
                    logger.warn("[POST Service] Skipping record with null id");
                    skipped++;
                    continue;
                }

                int rows = repository.updateMonthValues(
                        dto.getId(),
                        dto.getApril(),
                        dto.getMay(),
                        dto.getJune(),
                        dto.getJuly(),
                        dto.getAug(),
                        dto.getSep(),
                        dto.getOct(),
                        dto.getNov(),
                        dto.getDec(),
                        dto.getJan(),
                        dto.getFeb(),
                        dto.getMar(),
                        dto.getRemarks());

                if (rows > 0) {
                    updated++;
                } else {
                    logger.warn("[POST Service] No row found for id: {}", dto.getId());
                    skipped++;
                }
            }

            logger.info("[POST Service] Updated: {}, Skipped: {}", updated, skipped);
            aopMessageVM.setCode(200);
            aopMessageVM
                    .setMessage("Fixed consumption saved successfully. Updated: " + updated + ", Skipped: " + skipped);
            aopMessageVM.setData(null);
        } catch (Exception e) {
            logger.error("[POST Service] Error saving fixed consumption: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to save fixed consumption: " + e.getMessage());
            aopMessageVM.setData(null);
        }

        return aopMessageVM;
    }

    @Override
    public byte[] exportFixedConsumption(List<UUID> plantIds, String financialYear) {
        logger.info("[Export Service] Exporting fixed consumption for plantIds: {}, financialYear: {}", plantIds,
                financialYear);

        try {
            AOPMessageVM response = getFixedConsumptionForPlants(plantIds, financialYear);
            @SuppressWarnings("unchecked")
            List<JMDFixedConsumptionDto> fixedConsumptions = (List<JMDFixedConsumptionDto>) response.getData();
            if (fixedConsumptions == null) {
                fixedConsumptions = new ArrayList<>();
            }
            // Preserve the same order as the SP: PlantName → CostCenterName → NormParameter Name
            fixedConsumptions.sort(
                    Comparator.comparing((JMDFixedConsumptionDto d) -> d.getPlant() != null ? d.getPlant() : "")
                            .thenComparing(d -> d.getCostCenter() != null ? d.getCostCenter() : "")
                            .thenComparing(d -> d.getCppUtility() != null ? d.getCppUtility() : ""));

            return generateFixedConsumptionExcel(fixedConsumptions, "Fixed Consumption", financialYear);
        } catch (Exception e) {
            logger.error("[Export Service] Error exporting fixed consumption: {}", e.getMessage(), e);
            return null;
        }
    }

    private byte[] generateFixedConsumptionExcel(List<JMDFixedConsumptionDto> dtoList, String sheetName,
            String financialYear) throws Exception {
        logger.info("[Excel Generation] Creating {} with {} records", sheetName, dtoList != null ? dtoList.size() : 0);
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);
        int currentRow = 0;

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle remarksStyle = createRemarksStyle(workbook);

        String startYearSuffix = financialYear.substring(2, 4);
        String endYearSuffix = financialYear.substring(5, 7);

        // Header row
        List<String> headers = new ArrayList<>();
        headers.add("Plant");
        headers.add("Plant Id");
        headers.add("Cost Center");
        headers.add("Cost Center Id");
        headers.add("CPP Utilities");
        headers.add("CPP Utility Ids");
        headers.add("CPP Plant");
        headers.add("CPP Plant Id");
        headers.add("UOM");
        headers.add("Apr-" + startYearSuffix);
        headers.add("May-" + startYearSuffix);
        headers.add("Jun-" + startYearSuffix);
        headers.add("Jul-" + startYearSuffix);
        headers.add("Aug-" + startYearSuffix);
        headers.add("Sep-" + startYearSuffix);
        headers.add("Oct-" + startYearSuffix);
        headers.add("Nov-" + startYearSuffix);
        headers.add("Dec-" + startYearSuffix);
        headers.add("Jan-" + endYearSuffix);
        headers.add("Feb-" + endYearSuffix);
        headers.add("Mar-" + endYearSuffix);
        headers.add("Grand Total");
        headers.add("Remarks");
        // Hidden columns (col 23 = id, col 24 = row hash for change detection)
        headers.add("id");
        headers.add("_hash");

        Row headerRow = sheet.createRow(currentRow++);
        for (int col = 0; col < headers.size(); col++) {
            Cell cell = headerRow.createCell(col);
            cell.setCellValue(headers.get(col));
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        for (JMDFixedConsumptionDto dto : dtoList) {
            Row row = sheet.createRow(currentRow++);
            int col = 0;

            Cell cell = row.createCell(col++);
            cell.setCellValue(dto.getPlant() != null ? dto.getPlant() : "");
            cell.setCellStyle(dataStyle);

            cell = row.createCell(col++);
            cell.setCellValue(dto.getPlantId() != null ? dto.getPlantId() : "");
            cell.setCellStyle(dataStyle);

            cell = row.createCell(col++);
            cell.setCellValue(dto.getCostCenter() != null ? dto.getCostCenter() : "");
            cell.setCellStyle(dataStyle);

            cell = row.createCell(col++);
            cell.setCellValue(dto.getCostCenterId() != null ? dto.getCostCenterId() : "");
            cell.setCellStyle(dataStyle);

            cell = row.createCell(col++);
            cell.setCellValue(dto.getCppUtility() != null ? dto.getCppUtility() : "");
            cell.setCellStyle(dataStyle);

            cell = row.createCell(col++);
            cell.setCellValue(dto.getCppUtilityId() != null ? dto.getCppUtilityId() : "");
            cell.setCellStyle(dataStyle);

            cell = row.createCell(col++);
            cell.setCellValue(dto.getCppPlant() != null ? dto.getCppPlant() : "");
            cell.setCellStyle(dataStyle);

            cell = row.createCell(col++);
            cell.setCellValue(dto.getCppPlantId() != null ? dto.getCppPlantId() : "");
            cell.setCellStyle(dataStyle);

            cell = row.createCell(col++);
            cell.setCellValue(dto.getUom() != null ? dto.getUom() : "");
            cell.setCellStyle(dataStyle);

            setDoubleCellValue(row.createCell(col++), dto.getApril(), dataStyle);
            setDoubleCellValue(row.createCell(col++), dto.getMay(), dataStyle);
            setDoubleCellValue(row.createCell(col++), dto.getJune(), dataStyle);
            setDoubleCellValue(row.createCell(col++), dto.getJuly(), dataStyle);
            setDoubleCellValue(row.createCell(col++), dto.getAug(), dataStyle);
            setDoubleCellValue(row.createCell(col++), dto.getSep(), dataStyle);
            setDoubleCellValue(row.createCell(col++), dto.getOct(), dataStyle);
            setDoubleCellValue(row.createCell(col++), dto.getNov(), dataStyle);
            setDoubleCellValue(row.createCell(col++), dto.getDec(), dataStyle);
            setDoubleCellValue(row.createCell(col++), dto.getJan(), dataStyle);
            setDoubleCellValue(row.createCell(col++), dto.getFeb(), dataStyle);
            setDoubleCellValue(row.createCell(col++), dto.getMar(), dataStyle);
            setDoubleCellValue(row.createCell(col++), dto.getGrandTotal(), dataStyle);

            cell = row.createCell(col++);
            cell.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
            cell.setCellStyle(remarksStyle);

            // col 23 – id (hidden)
            cell = row.createCell(col++);
            cell.setCellValue(dto.getId() != null ? dto.getId().toString() : "");
            cell.setCellStyle(dataStyle);

            // col 24 – row hash (hidden, used for change detection at import time)
            cell = row.createCell(col++);
            cell.setCellValue(computeRowHash(dto));
            cell.setCellStyle(dataStyle);
        }

        // Hide id column (23) and hash column (24)
        sheet.setColumnHidden(23, true);
        sheet.setColumnHidden(24, true);

        int totalColumns = headers.size();
        for (int col = 0; col < totalColumns; col++) {
            if (col == 22) { // Remarks column
                sheet.setColumnWidth(col, 8000);
                continue;
            }
            sheet.autoSizeColumn(col);
            String headerText = headers.get(col);
            int headerWidth = Math.min(255 * 256, (headerText.length() + 2) * 256);
            if (sheet.getColumnWidth(col) < headerWidth) {
                sheet.setColumnWidth(col, headerWidth);
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    private void setDoubleCellValue(Cell cell, Double value, CellStyle style) {
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

    @Override
    public AOPMessageVM importFixedConsumption(List<UUID> plantIds, String financialYear, MultipartFile file) {
        logger.info("[Import Service] Importing fixed consumption for plantIds: {}, financialYear: {}, fileName: {}",
                plantIds, financialYear, file.getOriginalFilename());
        AOPMessageVM aopMessageVM = new AOPMessageVM();

        try {
            List<JMDFixedConsumptionDto> excelData = readFixedConsumptionExcel(file.getInputStream(), financialYear);
            logger.info("[Import Service] Read {} records from Excel", excelData.size());

            List<JMDFixedConsumptionDto> validRecords = new ArrayList<>();
            List<JMDFixedConsumptionDto> failedRecords = new ArrayList<>();
            List<String> failureReasons = new ArrayList<>();
            int skippedCount = 0;

            for (JMDFixedConsumptionDto dto : excelData) {

                // ── 1. Basic validation (id present, exists in DB) ──────────────
                String validationError = validateFixedConsumptionRow(dto);
                if (validationError != null) {
                    failedRecords.add(dto);
                    failureReasons.add(validationError);
                    logger.warn("[Import Service] Invalid record (id={}): {}", dto.getId(), validationError);
                    continue;
                }

                // ── 2. Hash-based change detection ──────────────────────────────
                // Recompute the hash from the uploaded row and compare it with
                // the hash that was embedded at export time (hidden col 24).
                // If they match → user did NOT change anything → skip silently.
                String uploadedHash = computeRowHash(dto);
                String embeddedHash = dto.getRowHash();
                boolean rowChanged = embeddedHash == null || !embeddedHash.equals(uploadedHash);

                if (!rowChanged) {
                    skippedCount++;
                    logger.debug("[Import Service] Skipping unchanged record id={}", dto.getId());
                    continue;
                }

                // ── 3. Row was modified – remarks must also be updated ───────────
                // Fetch current remarks from DB and compare.
                String remarkError = validateRemarksUpdated(dto);
                if (remarkError != null) {
                    failedRecords.add(dto);
                    failureReasons.add(remarkError);
                    logger.warn("[Import Service] Remarks not updated for id={}: {}", dto.getId(), remarkError);
                    continue;
                }

                validRecords.add(dto);
            }

            logger.info("[Import Service] {} unchanged (skipped), {} to update, {} failed validation",
                    skippedCount, validRecords.size(), failedRecords.size());

            // ── 4. Persist only changed & valid records ──────────────────────────
            int updated = 0;
            for (JMDFixedConsumptionDto dto : validRecords) {
                try {
                    int rows = repository.updateMonthValues(
                            dto.getId(),
                            dto.getApril(),
                            dto.getMay(),
                            dto.getJune(),
                            dto.getJuly(),
                            dto.getAug(),
                            dto.getSep(),
                            dto.getOct(),
                            dto.getNov(),
                            dto.getDec(),
                            dto.getJan(),
                            dto.getFeb(),
                            dto.getMar(),
                            dto.getRemarks());
                    if (rows > 0) {
                        updated++;
                    } else {
                        failedRecords.add(dto);
                        failureReasons.add("Record with this ID does not exist in database");
                        logger.warn("[Import Service] No row updated for id={}", dto.getId());
                    }
                } catch (Exception e) {
                    failedRecords.add(dto);
                    failureReasons.add("Save failed: " + e.getMessage());
                    logger.error("[Import Service] Error saving record id={}: {}", dto.getId(), e.getMessage(), e);
                }
            }

            // ── 5. Build response ────────────────────────────────────────────────
            if (failedRecords.isEmpty()) {
                aopMessageVM.setCode(200);
                if (updated == 0 && skippedCount > 0) {
                    aopMessageVM.setMessage("No changes detected. All " + skippedCount + " records are unchanged.");
                } else {
                    aopMessageVM.setMessage("Fixed consumption imported successfully. Updated: " + updated
                            + ", Unchanged: " + skippedCount + ".");
                }
                aopMessageVM.setData(null);
            } else {
                byte[] errorFile = generateFixedConsumptionErrorExcel(failedRecords, failureReasons, financialYear);
                String base64File = java.util.Base64.getEncoder().encodeToString(errorFile);
                aopMessageVM.setCode(400);
                aopMessageVM.setMessage("Partial import: " + updated + " updated, " + skippedCount
                        + " unchanged, " + failedRecords.size() + " failed. Download error file for details.");
                aopMessageVM.setData(base64File);
                logger.info("[Import Service] Exported {} failed records to error Excel", failedRecords.size());
            }

            logger.info("[Import Service] Completed - Updated: {}, Unchanged: {}, Failed: {}",
                    updated, skippedCount, failedRecords.size());

        } catch (Exception e) {
            logger.error("[Import Service] Error importing fixed consumption: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to import fixed consumption: " + e.getMessage());
            aopMessageVM.setData(null);
        }

        return aopMessageVM;
    }

    /**
     * Reads the exported Fixed Consumption Excel file and maps each data row to a
     * DTO.
     * Column order must match the export format:
     * Plant(0), PlantId(1), CostCenter(2), CostCenterId(3), CPPUtility(4),
     * CPPUtilityId(5),
     * CPPPlant(6), CPPPlantId(7), UOM(8), Apr(9)..Mar(20), GrandTotal(21),
     * Remarks(22), id(23)
     */
    private List<JMDFixedConsumptionDto> readFixedConsumptionExcel(InputStream inputStream, String financialYear)
            throws Exception {
        List<JMDFixedConsumptionDto> records = new ArrayList<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                // Skip fully empty rows
                boolean isRowEmpty = true;
                for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
                    Cell cell = row.getCell(cellIndex);
                    if (cell != null && !cell.toString().trim().isEmpty()) {
                        isRowEmpty = false;
                        break;
                    }
                }
                if (isRowEmpty)
                    continue;

                JMDFixedConsumptionDto dto = new JMDFixedConsumptionDto();
                int col = 0;

                // Static / read-only columns (not updated, kept for context in error report)
                dto.setPlant(getCellStringValue(row, col++)); // col 0 Plant
                dto.setPlantId(getCellStringValue(row, col++)); // col 1 PlantId
                dto.setCostCenter(getCellStringValue(row, col++)); // col 2 CostCenter
                dto.setCostCenterId(getCellStringValue(row, col++)); // col 3 CostCenterId
                dto.setCppUtility(getCellStringValue(row, col++)); // col 4 CPPUtility
                dto.setCppUtilityId(getCellStringValue(row, col++)); // col 5 CPPUtilityId
                dto.setCppPlant(getCellStringValue(row, col++)); // col 6 CPPPlant
                dto.setCppPlantId(getCellStringValue(row, col++)); // col 7 CPPPlantId
                dto.setUom(getCellStringValue(row, col++)); // col 8 UOM

                // Month columns (updatable)
                dto.setApril(getCellDoubleVal(row, col++)); // col 9 Apr
                dto.setMay(getCellDoubleVal(row, col++)); // col 10 May
                dto.setJune(getCellDoubleVal(row, col++)); // col 11 Jun
                dto.setJuly(getCellDoubleVal(row, col++)); // col 12 Jul
                dto.setAug(getCellDoubleVal(row, col++)); // col 13 Aug
                dto.setSep(getCellDoubleVal(row, col++)); // col 14 Sep
                dto.setOct(getCellDoubleVal(row, col++)); // col 15 Oct
                dto.setNov(getCellDoubleVal(row, col++)); // col 16 Nov
                dto.setDec(getCellDoubleVal(row, col++)); // col 17 Dec
                dto.setJan(getCellDoubleVal(row, col++)); // col 18 Jan
                dto.setFeb(getCellDoubleVal(row, col++)); // col 19 Feb
                dto.setMar(getCellDoubleVal(row, col++)); // col 20 Mar

                col++; // col 21 Grand Total – skip (calculated, not stored)

                // Remarks (updatable)
                dto.setRemarks(getCellStringValue(row, col++)); // col 22 Remarks

                // col 23 – Hidden id column (required for update)
                String idStr = getCellStringValue(row, col++);
                if (idStr != null && !idStr.trim().isEmpty()) {
                    try {
                        dto.setId(UUID.fromString(idStr.trim()));
                    } catch (IllegalArgumentException e) {
                        logger.warn("[IMPORT] Invalid UUID for id at row {}: {}", i + 1, idStr);
                    }
                }

                // col 24 – Hidden row hash (written at export time for change detection)
                String hash = getCellStringValue(row, col++);
                dto.setRowHash(hash);

                records.add(dto);
            }
        }
        return records;
    }

    /**
     * Basic row validation (phase 1): checks that the record ID is present and
     * exists in the database. Remarks validation happens in phase 2 only for
     * rows that were actually modified (detected via hash comparison).
     */
    private String validateFixedConsumptionRow(JMDFixedConsumptionDto dto) {
        if (dto.getId() == null) {
            return "Record ID is missing – the hidden 'id' column must not be modified.";
        }
        try {
            Optional<com.wks.caseengine.cpp.entity.CPPFixedConsumption> optEntity = repository.findById(dto.getId());
            if (optEntity.isEmpty()) {
                return "Record with this ID does not exist in the database.";
            }
        } catch (Exception e) {
            logger.error("[IMPORT Validation] Error checking record id={}: {}", dto.getId(), e.getMessage());
        }
        return null;
    }

    /**
     * Phase-2 remarks validation – called only for rows where the hash
     * comparison shows that month values were changed.
     * Remarks must be non-empty AND different from the current DB value.
     */
    private String validateRemarksUpdated(JMDFixedConsumptionDto dto) {
        if (dto.getRemarks() == null || dto.getRemarks().trim().isEmpty()) {
            return "Remarks are required when changing values. Please add a remark explaining the change.";
        }
        try {
            Optional<com.wks.caseengine.cpp.entity.CPPFixedConsumption> optEntity = repository.findById(dto.getId());
            if (optEntity.isPresent()) {
                String dbRemarks = optEntity.get().getRemarks() != null ? optEntity.get().getRemarks().trim() : "";
                String importRemarks = dto.getRemarks().trim();
                if (dbRemarks.equals(importRemarks)) {
                    return "Remarks must be updated when changing values.";
                }
            }
        } catch (Exception e) {
            logger.error("[IMPORT Remarks Validation] Error for id={}: {}", dto.getId(), e.getMessage());
        }
        return null;
    }

    /**
     * Computes an MD5 hash of all 12 month values concatenated with remarks.
     * This hash is written to a hidden column at export time and re-read at
     * import time to detect which rows the user actually modified.
     */
    private String computeRowHash(JMDFixedConsumptionDto dto) {
        String raw = String.join("|",
                fmt(dto.getApril()),
                fmt(dto.getMay()),
                fmt(dto.getJune()),
                fmt(dto.getJuly()),
                fmt(dto.getAug()),
                fmt(dto.getSep()),
                fmt(dto.getOct()),
                fmt(dto.getNov()),
                fmt(dto.getDec()),
                fmt(dto.getJan()),
                fmt(dto.getFeb()),
                fmt(dto.getMar()),
                dto.getRemarks() != null ? dto.getRemarks().trim() : "");
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            logger.warn("[computeRowHash] MD5 unavailable, using raw string as fallback");
            return raw;
        }
    }

    private String fmt(Double val) {
        return val != null ? val.toString() : "null";
    }

    /**
     * Generates an error Excel file for records that failed during import.
     * Contains the same columns as the export plus Status and Comment columns.
     */
    private byte[] generateFixedConsumptionErrorExcel(List<JMDFixedConsumptionDto> failedRecords,
            List<String> failureReasons, String financialYear) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Failed Records");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle errorStyle = createErrorCellStyle(workbook);

            String startYearSuffix = financialYear.substring(2, 4);
            String endYearSuffix = financialYear.substring(5, 7);

            // Header row – mirrors export column order + Status + Comment
            Row headerRow = sheet.createRow(0);
            int col = 0;
            String[] headers = {
                    "Plant", "Plant Id", "Cost Center", "Cost Center Id",
                    "CPP Utilities", "CPP Utility Ids", "CPP Plant", "CPP Plant Id", "UOM",
                    "Apr-" + startYearSuffix, "May-" + startYearSuffix, "Jun-" + startYearSuffix,
                    "Jul-" + startYearSuffix, "Aug-" + startYearSuffix, "Sep-" + startYearSuffix,
                    "Oct-" + startYearSuffix, "Nov-" + startYearSuffix, "Dec-" + startYearSuffix,
                    "Jan-" + endYearSuffix, "Feb-" + endYearSuffix, "Mar-" + endYearSuffix,
                    "Grand Total", "Remarks", "id", "Status", "Comment"
            };
            for (String h : headers) {
                Cell cell = headerRow.createCell(col++);
                cell.setCellValue(h);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (int i = 0; i < failedRecords.size(); i++) {
                JMDFixedConsumptionDto dto = failedRecords.get(i);
                String reason = failureReasons.get(i);
                Row row = sheet.createRow(rowNum++);
                col = 0;

                createStrCell(row, col++, dto.getPlant(), dataStyle);
                createStrCell(row, col++, dto.getPlantId(), dataStyle);
                createStrCell(row, col++, dto.getCostCenter(), dataStyle);
                createStrCell(row, col++, dto.getCostCenterId(), dataStyle);
                createStrCell(row, col++, dto.getCppUtility(), dataStyle);
                createStrCell(row, col++, dto.getCppUtilityId(), dataStyle);
                createStrCell(row, col++, dto.getCppPlant(), dataStyle);
                createStrCell(row, col++, dto.getCppPlantId(), dataStyle);
                createStrCell(row, col++, dto.getUom(), dataStyle);
                setDoubleCell(row, col++, dto.getApril(), dataStyle);
                setDoubleCell(row, col++, dto.getMay(), dataStyle);
                setDoubleCell(row, col++, dto.getJune(), dataStyle);
                setDoubleCell(row, col++, dto.getJuly(), dataStyle);
                setDoubleCell(row, col++, dto.getAug(), dataStyle);
                setDoubleCell(row, col++, dto.getSep(), dataStyle);
                setDoubleCell(row, col++, dto.getOct(), dataStyle);
                setDoubleCell(row, col++, dto.getNov(), dataStyle);
                setDoubleCell(row, col++, dto.getDec(), dataStyle);
                setDoubleCell(row, col++, dto.getJan(), dataStyle);
                setDoubleCell(row, col++, dto.getFeb(), dataStyle);
                setDoubleCell(row, col++, dto.getMar(), dataStyle);
                setDoubleCell(row, col++, dto.getGrandTotal(), dataStyle);
                createStrCell(row, col++, dto.getRemarks(), dataStyle);
                createStrCell(row, col++, dto.getId() != null ? dto.getId().toString() : "", dataStyle);
                createStrCell(row, col++, "Failed", errorStyle);
                createStrCell(row, col++, reason, errorStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    // ── Helper cell-write utilities ──────────────────────────────────────────

    private void createStrCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void setDoubleCell(Row row, int col, Double value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value);
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }

    private String getCellStringValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null)
            return null;
        switch (cell.getCellType()) {
            case STRING:
                String s = cell.getStringCellValue();
                return s != null && !s.trim().isEmpty() ? s : null;
            case NUMERIC:
                // Some string-ish columns may be formatted as numbers in Excel
                double d = cell.getNumericCellValue();
                long l = (long) d;
                return (d == l) ? String.valueOf(l) : String.valueOf(d);
            case BLANK:
                return null;
            default:
                String str = cell.toString();
                return str != null && !str.trim().isEmpty() ? str.trim() : null;
        }
    }

    private Double getCellDoubleVal(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null)
            return null;
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
                logger.warn("[IMPORT] Cannot parse double from cell at col index {}", cellIndex);
            }
        }
        return null;
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

    @Override
    public AOPMessageVM createFixedConsumption(FixedConsumptionCreateRequestDto request) {
        logger.info("[CREATE] Creating fixed consumption: parentPlantId={}, recieverPlantId={}, costCenterId={}, senderPlantId={}, cppUtilityId={}, aopYear={}",
                request.getParentPlantId(), request.getRecieverPlantId(), request.getCostCenterId(), request.getSenderPlantId(), request.getCppUtilityId(), request.getAopYear());
        AOPMessageVM aopMessageVM = new AOPMessageVM();

        try {
            CPPFixedConsumption entity = new CPPFixedConsumption();
            entity.setId(UUID.randomUUID());
            entity.setPlantFkId(request.getRecieverPlantId());
            entity.setCppCostCenterFkId(request.getCostCenterId());
            entity.setNormParameterFkId(request.getCppUtilityId());
            entity.setRemarks(request.getRemarks());
            entity.setAopYear(request.getAopYear());
            entity.setApr(0.0);
            entity.setMay(0.0);
            entity.setJun(0.0);
            entity.setJul(0.0);
            entity.setAug(0.0);
            entity.setSep(0.0);
            entity.setOct(0.0);
            entity.setNov(0.0);
            entity.setDec(0.0);
            entity.setJan(0.0);
            entity.setFeb(0.0);
            entity.setMar(0.0);
            entity.setCreatedDate(LocalDateTime.now());
            entity.setUpdatedDate(LocalDateTime.now());

            repository.save(entity);

            logger.info("[CREATE] Fixed consumption created with id={}", entity.getId());
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Fixed consumption row created successfully");
            aopMessageVM.setData(entity.getId());
            return aopMessageVM;
        } catch (Exception e) {
            logger.error("[CREATE] Error creating fixed consumption: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to create fixed consumption: " + e.getMessage());
            aopMessageVM.setData(null);
            return aopMessageVM;
        }
    }

    @Override
    public AOPMessageVM updateFixedConsumption(FixedConsumptionCreateRequestDto request) {
        logger.info("[UPDATE] Updating fixed consumption: id={}", request.getId());
        AOPMessageVM aopMessageVM = new AOPMessageVM();

        try {
            Optional<CPPFixedConsumption> optionalEntity = repository.findById(request.getId());
            if (optionalEntity.isEmpty()) {
                logger.warn("[UPDATE] Fixed consumption not found for id={}", request.getId());
                aopMessageVM.setCode(404);
                aopMessageVM.setMessage("Fixed consumption row not found");
                aopMessageVM.setData(null);
                return aopMessageVM;
            }

            CPPFixedConsumption entity = optionalEntity.get();
            entity.setPlantFkId(request.getRecieverPlantId());
            entity.setCppCostCenterFkId(request.getCostCenterId());
            entity.setNormParameterFkId(request.getCppUtilityId());
            entity.setRemarks(request.getRemarks());
            entity.setUpdatedDate(LocalDateTime.now());

            repository.save(entity);

            logger.info("[UPDATE] Fixed consumption updated for id={}", request.getId());
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Fixed consumption row updated successfully");
            aopMessageVM.setData(request.getId());
            return aopMessageVM;
        } catch (Exception e) {
            logger.error("[UPDATE] Error updating fixed consumption: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to update fixed consumption: " + e.getMessage());
            aopMessageVM.setData(null);
            return aopMessageVM;
        }
    }

    @Override
    public AOPMessageVM deleteFixedConsumption(UUID id) {
        logger.info("[DELETE] Deleting fixed consumption with id={}", id);
        AOPMessageVM aopMessageVM = new AOPMessageVM();

        try {
            Optional<CPPFixedConsumption> optionalEntity = repository.findById(id);
            if (optionalEntity.isEmpty()) {
                logger.warn("[DELETE] Fixed consumption not found for id={}", id);
                aopMessageVM.setCode(404);
                aopMessageVM.setMessage("Fixed consumption row not found");
                aopMessageVM.setData(null);
                return aopMessageVM;
            }

            repository.deleteById(id);

            logger.info("[DELETE] Fixed consumption deleted for id={}", id);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Fixed consumption row deleted successfully");
            aopMessageVM.setData(id);
            return aopMessageVM;
        } catch (Exception e) {
            logger.error("[DELETE] Error deleting fixed consumption: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to delete fixed consumption: " + e.getMessage());
            aopMessageVM.setData(null);
            return aopMessageVM;
        }
    }
}
