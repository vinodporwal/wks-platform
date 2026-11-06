package com.wks.caseengine.cpp.serviceimpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.dto.SpinningMarginReportDTO;
import com.wks.caseengine.cpp.repository.CPPMonthlySpinningMarginMaintainedRepository;
import com.wks.caseengine.cpp.service.JMDSpinningMarginReportService;
import com.wks.caseengine.cpp.utility.CPPUserContext;
import com.wks.caseengine.cpp.utility.ExcelStyles;
import com.wks.caseengine.cpp.utility.FiscalYearMonths;
import com.wks.caseengine.message.vm.AOPMessageVM;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JMDSpinningMarginReportServiceImpl implements JMDSpinningMarginReportService {

    private static final String COMBINED_PLANT_NAME = "Combined/All Plant";

    private static final String[] MONTH_NAMES = {
        "Apr", "May", "Jun", "Jul", "Aug", "Sep",
        "Oct", "Nov", "Dec", "Jan", "Feb", "Mar"
    };

    @Autowired
    private CPPMonthlySpinningMarginMaintainedRepository repository;

    // =========================================================
    // GET SPINNING MARGIN REPORT
    // =========================================================

    @Override
    @Transactional
    public AOPMessageVM getSpinningMarginReport(
            List<UUID> plantIds,
            String aopYear) {

        log.info(
                "[SpinningMarginReport] GET - plantIds: {}, aopYear: {}",
                plantIds, aopYear);

        AOPMessageVM vm = new AOPMessageVM();

        if (aopYear == null || aopYear.trim().isEmpty()) {
            vm.setCode(400);
            vm.setMessage("aopYear cannot be null or empty");
            vm.setData(new ArrayList<>());
            return vm;
        }

        try {

            String selectedAopYear = normaliseFinancialYear(aopYear);

            if (!selectedAopYear.matches("\\d{4}-\\d{2}")) {
                throw new IllegalArgumentException(
                        "Invalid AOP year. Expected format YYYY-YY, e.g. 2026-27");
            }

            String previousAopYear = getPreviousAopYear(selectedAopYear);

            // Carry forward only when the selected AOP year has no records.
            int insertedCount = repository.carryForwardIfYearMissing(
                    selectedAopYear,
                    previousAopYear);

            log.info(
                    "[SpinningMarginReport] Carry-forward completed. " +
                    "AOPYear: {}, PreviousAOPYear: {}, InsertedRecords: {}",
                    selectedAopYear, previousAopYear, insertedCount);

            // Fetch all rows or selected plants, including common rows.
            List<Object[]> rawRows;

            if (plantIds == null || plantIds.isEmpty()) {
                rawRows = repository.findByAopYear(selectedAopYear);
            } else {
                rawRows = repository.findByPlantIdsAndAopYear(
                        plantIds,
                        selectedAopYear);
            }

            log.info(
                    "[SpinningMarginReport] GET - AOPYear: {}, found {} records",
                    selectedAopYear, rawRows.size());

            List<SpinningMarginReportDTO> dtoList = new ArrayList<>();

            for (Object[] row : rawRows) {
                SpinningMarginReportDTO dto = mapRowToDto(row);
                dto.setDataHash(generateSpinningMarginReportHash(dto));
                dtoList.add(dto);
            }

            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(dtoList);

        } catch (IllegalArgumentException e) {

            log.error(
                    "[SpinningMarginReport] Invalid AOP year: {}",
                    e.getMessage());

            vm.setCode(400);
            vm.setMessage(e.getMessage());
            vm.setData(new ArrayList<>());

        } catch (Exception e) {

            log.error(
                    "[SpinningMarginReport] GET error: {}",
                    e.getMessage(), e);

            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
        }

        return vm;
    }

    // =========================================================
    // EXPORT SPINNING MARGIN REPORT
    // =========================================================

    @Override
    public byte[] exportSpinningMarginReport(
            List<UUID> plantIds,
            String aopYear) throws IOException {
        return exportSpinningMarginReport(plantIds, aopYear, true);
    }

    @Override
    public byte[] exportSpinningMarginReport(
            List<UUID> plantIds,
            String aopYear,
            boolean showMaintained) throws IOException {

        log.info(
                "[SpinningMarginReport] EXPORT - plantIds: {}, aopYear: {}, showMaintained: {}",
                plantIds, aopYear, showMaintained);

        try {

            AOPMessageVM result = getSpinningMarginReport(plantIds, aopYear);

            if (result.getCode() != 200) {
                throw new IOException(
                        "Failed to fetch Spinning Margin report: "
                        + result.getMessage());
            }

            List<SpinningMarginReportDTO> dtoList = new ArrayList<>();

            if (result.getData() instanceof List<?>) {

                @SuppressWarnings("unchecked")
                List<SpinningMarginReportDTO> data =
                        (List<SpinningMarginReportDTO>) result.getData();

                dtoList = data;
            }

            return generateExcel(dtoList, aopYear, null, showMaintained);

        } catch (IOException e) {

            log.error(
                    "[SpinningMarginReport] EXPORT IOException: {}",
                    e.getMessage(), e);

            throw e;

        } catch (Exception e) {

            log.error(
                    "[SpinningMarginReport] EXPORT error: {}",
                    e.getMessage(), e);

            throw new IOException(
                    "Failed to export Spinning Margin report: "
                    + e.getMessage(), e);
        }
    }

    // =========================================================
    // SAVE SPINNING MARGIN REPORT
    // =========================================================

    @Override
    @Transactional
    public AOPMessageVM saveSpinningMarginReport(
            List<UUID> plantIds,
            String aopYear,
            List<SpinningMarginReportDTO> dtoList) {

        log.info("[SpinningMarginReport] SAVE - plantIds: {}, aopYear: {}, records: {}",
                plantIds, aopYear, dtoList != null ? dtoList.size() : 0);
        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (dtoList == null || dtoList.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("Request body cannot be empty");
                return vm;
            }

            // Resolve user from JWT token - using preferred_username
            CPPUserContext user = CPPUserContext.current();
            String currentUsername = user.getPreferredUsername();
            if (currentUsername == null || currentUsername.trim().isEmpty()) {
                currentUsername = "SYSTEM";
            }
            log.info("[SpinningMargin] Resolved preferred_username for ModifiedBy: {}", currentUsername);

            int successCount = 0;
            int errorCount = 0;
            List<String> errorMessages = new ArrayList<>();

            for (SpinningMarginReportDTO dto : dtoList) {
                try {
                    if (dto.getId() == null) {
                        errorCount++;
                        errorMessages.add("Record skipped: id is null");
                        continue;
                    }

                    dto.setModifiedBy(currentUsername);

                    repository.updateSpinningMarginTarget(
                            dto.getId(),
                            dto.getAprTarget(),
                            dto.getMayTarget(),
                            dto.getJunTarget(),
                            dto.getJulTarget(),
                            dto.getAugTarget(),
                            dto.getSepTarget(),
                            dto.getOctTarget(),
                            dto.getNovTarget(),
                            dto.getDecTarget(),
                            dto.getJanTarget(),
                            dto.getFebTarget(),
                            dto.getMarTarget(),
                            dto.getRemarks(),
                            currentUsername);
                    successCount++;

                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = "Error processing id " + dto.getId() + ": " + e.getMessage();
                    errorMessages.add(errorMsg);
                    log.error(errorMsg, e);
                }
            }

            log.info("[SpinningMarginReport] SAVE - success: {}, errors: {}", successCount, errorCount);

            if (errorCount > 0) {
                vm.setCode(207);
                vm.setMessage(String.format("Processed %d records. Success: %d, Errors: %d",
                        dtoList.size(), successCount, errorCount));
                vm.setData(errorMessages);
            } else {
                vm.setCode(200);
                vm.setMessage(String.format("Successfully processed all %d records", successCount));
                vm.setData(null);
            }

        } catch (Exception e) {
            log.error("[SpinningMarginReport] SAVE error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(null);
        }

        return vm;
    }

    // =========================================================
    // IMPORT SPINNING MARGIN REPORT
    // =========================================================

    @Override
    @Transactional
    public AOPMessageVM importSpinningMarginReport(
            List<UUID> plantIds,
            String aopYear,
            MultipartFile file) throws IOException {

        log.info("[SpinningMarginReport] IMPORT - plantIds: {}, aopYear: {}, file: {}",
                plantIds, aopYear, file != null ? file.getOriginalFilename() : "null");

        AOPMessageVM vm = new AOPMessageVM();

        try (InputStream inputStream = file.getInputStream()) {
            List<SpinningMarginReportDTO> dtoList = readExcel(inputStream, aopYear);

            if (dtoList == null || dtoList.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("No valid records found in the Excel file");
                return vm;
            }

            List<SpinningMarginReportDTO> failedRecords = new ArrayList<>();
            List<SpinningMarginReportDTO> validRecords = new ArrayList<>();

            // Fetch existing data for remark validation and change detection
            AOPMessageVM existingData = getSpinningMarginReport(plantIds, aopYear);
            Map<UUID, String> existingRemarks = new HashMap<>();
            Map<UUID, String> existingHashes = new HashMap<>();
            Map<String, UUID> existingByPlantAndType = new HashMap<>();

            if (existingData.getData() instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<SpinningMarginReportDTO> existingList = (List<SpinningMarginReportDTO>) existingData.getData();
                for (SpinningMarginReportDTO existing : existingList) {
                    if (existing.getId() != null) {
                        existingRemarks.put(existing.getId(),
                                existing.getRemarks() != null ? existing.getRemarks().trim() : "");
                        existingHashes.put(existing.getId(), generateSpinningMarginReportHash(existing));

                        String plantKey = (existing.getPlantName() != null ? existing.getPlantName().trim().toLowerCase() : "");
                        String typeKey = (existing.getMarginType() != null ? existing.getMarginType().trim().toLowerCase() : "");
                        String utilCodeKey = (existing.getUtilityCode() != null ? existing.getUtilityCode().trim().toLowerCase() : "");
                        String utilNameKey = (existing.getUtilityName() != null ? existing.getUtilityName().trim().toLowerCase() : "");
                        if (!utilCodeKey.isEmpty()) {
                            existingByPlantAndType.put(plantKey + "|" + typeKey + "|" + utilCodeKey, existing.getId());
                        }
                        if (!utilNameKey.isEmpty()) {
                            existingByPlantAndType.put(plantKey + "|" + typeKey + "|" + utilNameKey, existing.getId());
                        }
                        existingByPlantAndType.putIfAbsent(plantKey + "|" + typeKey, existing.getId());
                    }
                }
            }

            List<String> errorMessages = new ArrayList<>();
            int skippedCount = 0;

            for (SpinningMarginReportDTO dto : dtoList) {
                if (dto.getId() == null) {
                    String plantKey = (dto.getPlantName() != null ? dto.getPlantName().trim().toLowerCase() : "");
                    String typeKey = (dto.getMarginType() != null ? dto.getMarginType().trim().toLowerCase() : "");
                    String utilCodeKey = (dto.getUtilityCode() != null ? dto.getUtilityCode().trim().toLowerCase() : "");
                    String utilNameKey = (dto.getUtilityName() != null ? dto.getUtilityName().trim().toLowerCase() : "");
                    UUID resolvedId = null;
                    if (!utilCodeKey.isEmpty()) {
                        resolvedId = existingByPlantAndType.get(plantKey + "|" + typeKey + "|" + utilCodeKey);
                    }
                    if (resolvedId == null && !utilNameKey.isEmpty()) {
                        resolvedId = existingByPlantAndType.get(plantKey + "|" + typeKey + "|" + utilNameKey);
                    }
                    if (resolvedId == null) {
                        resolvedId = existingByPlantAndType.get(plantKey + "|" + typeKey);
                    }
                    if (resolvedId != null) {
                        dto.setId(resolvedId);
                    }
                }

                if (dto.getId() == null) {
                    failedRecords.add(dto);
                    errorMessages.add("Record skipped: id is null");
                    continue;
                }

                // Skip unchanged records (dataHash matches, or DB hash matches when dataHash missing)
                if (!isRecordModified(dto, existingHashes)) {
                    skippedCount++;
                    log.debug("[SpinningMarginReport] Skipping unchanged record: {}", dto.getId());
                    continue;
                }

                // Validate remarks is mandatory
                if (dto.getRemarks() == null || dto.getRemarks().trim().isEmpty()) {
                    failedRecords.add(dto);
                    errorMessages.add("Remarks field is mandatory and cannot be empty");
                    continue;
                }

                // Validate remarks must be different from existing DB value
                String dbRemarks = existingRemarks.getOrDefault(dto.getId(), "");
                String importedRemarks = dto.getRemarks().trim();
                if (dbRemarks.equals(importedRemarks)) {
                    failedRecords.add(dto);
                    errorMessages.add("Remarks must be updated to explain the changes. Current remarks are identical to the database value.");
                    continue;
                }

                validRecords.add(dto);
            }

            log.info("[SpinningMarginReport] {} unchanged (skipped), {} modified to process",
                    skippedCount, dtoList.size() - skippedCount);

            AOPMessageVM saveResult = saveSpinningMarginReport(plantIds, aopYear, validRecords);

            if (!failedRecords.isEmpty()) {
                byte[] errorExcel = generateExcel(failedRecords, aopYear, errorMessages, false);
                String base64File = Base64.getEncoder().encodeToString(errorExcel);
                vm.setCode(400);
                vm.setData(base64File);
                vm.setMessage("Partial data saved. " + validRecords.size() + " saved, " + failedRecords.size()
                        + " failed, " + skippedCount + " unchanged. Please check the downloaded error file.");
            } else {
                vm.setCode(saveResult.getCode());
                if (validRecords.isEmpty() && skippedCount > 0) {
                    vm.setMessage("No changes detected. All " + skippedCount + " records unchanged.");
                } else {
                    vm.setMessage(saveResult.getMessage() + " " + skippedCount + " unchanged.");
                }
                vm.setData(saveResult.getData());
            }

        } catch (Exception e) {
            log.error("[SpinningMarginReport] IMPORT error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
        }

        return vm;
    }

    // =========================================================
    // MAP DATABASE ROW TO DTO
    // =========================================================

    /**
     * Repository column order:
     *
     * 0  Id
     * 1  AOPYear
     * 2  MarginType
     * 3  Plant_FK_Id
     * 4  PlantName
     * 5  UtilityName
     * 6  UtilityCode
     * 7  NormParameterFkId
     * 8  Apr
     * 9  May
     * 10 Jun
     * 11 Jul
     * 12 Aug
     * 13 Sep
     * 14 Oct
     * 15 Nov
     * 16 Dec
     * 17 Jan
     * 18 Feb
     * 19 Mar
     * 20 AprTarget
     * 21 MayTarget
     * 22 JunTarget
     * 23 JulTarget
     * 24 AugTarget
     * 25 SepTarget
     * 26 OctTarget
     * 27 NovTarget
     * 28 DecTarget
     * 29 JanTarget
     * 30 FebTarget
     * 31 MarTarget
     * 32 Remarks
     * 33 ModifiedBy
     */
    private SpinningMarginReportDTO mapRowToDto(Object[] row) {

        if (row == null || row.length < 34) {
            throw new IllegalArgumentException(
                    "Invalid Spinning Margin query result. Expected at least 34 columns.");
        }

        SpinningMarginReportDTO dto = new SpinningMarginReportDTO();

        dto.setId(toUUID(row[0]));
        dto.setAopYear(toStringValue(row[1]));
        dto.setMarginType(toStringValue(row[2]));
        dto.setPlantFkId(toUUID(row[3]));

        dto.setPlantName(
                row[3] == null
                        ? COMBINED_PLANT_NAME
                        : toStringValue(row[4]));

        dto.setUtilityName(toStringValue(row[5]));
        dto.setUtilityCode(toStringValue(row[6]));
        dto.setNormParameterFkId(toUUID(row[7]));

        // Maintained values.
        dto.setApr(toDoubleObj(row[8]));
        dto.setMay(toDoubleObj(row[9]));
        dto.setJun(toDoubleObj(row[10]));
        dto.setJul(toDoubleObj(row[11]));
        dto.setAug(toDoubleObj(row[12]));
        dto.setSep(toDoubleObj(row[13]));
        dto.setOct(toDoubleObj(row[14]));
        dto.setNov(toDoubleObj(row[15]));
        dto.setDec(toDoubleObj(row[16]));
        dto.setJan(toDoubleObj(row[17]));
        dto.setFeb(toDoubleObj(row[18]));
        dto.setMar(toDoubleObj(row[19]));

        // Target values.
        dto.setAprTarget(toDoubleObj(row[20]));
        dto.setMayTarget(toDoubleObj(row[21]));
        dto.setJunTarget(toDoubleObj(row[22]));
        dto.setJulTarget(toDoubleObj(row[23]));
        dto.setAugTarget(toDoubleObj(row[24]));
        dto.setSepTarget(toDoubleObj(row[25]));
        dto.setOctTarget(toDoubleObj(row[26]));
        dto.setNovTarget(toDoubleObj(row[27]));
        dto.setDecTarget(toDoubleObj(row[28]));
        dto.setJanTarget(toDoubleObj(row[29]));
        dto.setFebTarget(toDoubleObj(row[30]));
        dto.setMarTarget(toDoubleObj(row[31]));

        // Remarks and audit information.
        dto.setRemarks(toStringValue(row[32]));
        dto.setModifiedBy(toStringValue(row[33]));

        return dto;
    }

    private UUID toUUID(Object value) {

        if (value == null) {
            return null;
        }

        return UUID.fromString(value.toString());
    }

    private String toStringValue(Object value) {

        return value != null ? value.toString() : null;
    }

    private Double toDoubleObj(Object value) {

        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        try {
            return Double.parseDouble(value.toString().trim());
        } catch (NumberFormatException e) {
            log.warn("Unable to convert value to Double: {}", value);
            return null;
        }
    }

    // =========================================================
    // NORMALIZE FINANCIAL YEAR
    // =========================================================

    private String normaliseFinancialYear(String fy) {

        if (fy == null) {
            return "";
        }

        String value = fy.trim().replace("/", "-");

        // 2026-27
        if (value.matches("\\d{4}-\\d{2}")) {
            return value;
        }

        // 2026-2027
        if (value.matches("\\d{4}-\\d{4}")) {
            return value.substring(0, 4) + "-"
                    + value.substring(7, 9);
        }

        // 2026
        if (value.matches("\\d{4}")) {

            int startYear = Integer.parseInt(value);

            return startYear + "-"
                    + String.format("%02d", (startYear + 1) % 100);
        }

        // 26-27
        if (value.matches("\\d{2}-\\d{2}")) {
            return "20" + value;
        }

        return value;
    }

    // =========================================================
    // GET PREVIOUS AOP YEAR
    // =========================================================

    private String getPreviousAopYear(String aopYear) {

        if (aopYear == null || !aopYear.matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException(
                    "Invalid AOP year. Expected format YYYY-YY, e.g. 2026-27");
        }

        int startYear = Integer.parseInt(aopYear.substring(0, 4));

        String expectedEndYear =
                String.format("%02d", (startYear + 1) % 100);

        if (!aopYear.substring(5).equals(expectedEndYear)) {
            throw new IllegalArgumentException(
                    "Invalid AOP year: " + aopYear);
        }

        int previousStartYear = startYear - 1;

        return String.format(
                "%04d-%02d",
                previousStartYear,
                startYear % 100);
    }

    // =========================================================
    // GENERATE EXCEL
    // =========================================================

    private byte[] generateExcel(
            List<SpinningMarginReportDTO> dtoList,
            String aopYear,
            List<String> errorMessages,
            boolean showMaintained) throws IOException {

        boolean isErrorExcel = errorMessages != null && !errorMessages.isEmpty();

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet(isErrorExcel ? "Spinning Margin Report Errors" : "Spinning Margin Report");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle subHeaderStyle = createSubHeaderStyle(workbook);
            CellStyle textStyle = createDataStyle(workbook);
            CellStyle numericStyle = createNumericStyle(workbook);
            CellStyle remarksStyle = createRemarksStyle(workbook);
            CellStyle errorStyle = isErrorExcel ? ExcelStyles.createErrorStyle(workbook) : null;

            String normalizedYear = normaliseFinancialYear(aopYear);

            String[] monthHeaders =
                    FiscalYearMonths.getMonthHeaders(normalizedYear);

            if (monthHeaders == null || monthHeaders.length != 12) {
                throw new IllegalArgumentException(
                        "Unable to generate 12 month headers for AOP year: "
                        + normalizedYear);
            }

            // -------------------------------------------------
            // HEADER ROWS
            // -------------------------------------------------

            Row topHeaderRow = sheet.createRow(0);
            int col = 0;

            // Static columns spanning rows 0 and 1
            createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Plant Name", headerStyle);
            sheet.setColumnWidth(col, 28 * 256);
            col++;

            createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Type", headerStyle);
            sheet.setColumnWidth(col, 16 * 256);
            col++;

            createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Utility Name", headerStyle);
            sheet.setColumnWidth(col, 24 * 256);
            col++;

            createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Utility Code", headerStyle);
            sheet.setColumnWidth(col, 18 * 256);
            col++;

            // Month headers (each month spans 2 columns: Maintained | Target)
            int monthStartCol = col;
            List<Integer> maintainedCols = new ArrayList<>();
            for (String month : monthHeaders) {
                maintainedCols.add(col);
                if (showMaintained) {
                    createMergedHeaderCell(sheet, topHeaderRow, 0, 0, col, col + 1, month, headerStyle);
                } else {
                    Cell mCell = topHeaderRow.createCell(col);
                    mCell.setCellValue(month);
                    mCell.setCellStyle(headerStyle);

                    Cell tCell = topHeaderRow.createCell(col + 1);
                    tCell.setCellValue(month);
                    tCell.setCellStyle(headerStyle);
                }
                sheet.setColumnWidth(col, 15 * 256);
                sheet.setColumnWidth(col + 1, 15 * 256);
                col += 2;
            }

            int remarksCol = col;
            createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Remarks", headerStyle);
            sheet.setColumnWidth(remarksCol, 30 * 256);
            col++;

            int modifiedByCol = col;
            createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Modified By", headerStyle);
            sheet.setColumnWidth(modifiedByCol, 22 * 256);
            col++;

            int idCol = col;
            createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "id", headerStyle);
            col++;

            int dataHashCol = col;
            createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "dataHash", headerStyle);
            col++;

            int statusCol = -1;
            int errorMsgCol = -1;
            if (isErrorExcel) {
                statusCol = col;
                createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Status", headerStyle);
                sheet.setColumnWidth(statusCol, 15 * 256);
                col++;

                errorMsgCol = col;
                createMergedHeaderCell(sheet, topHeaderRow, 0, 1, col, col, "Error Message", headerStyle);
                sheet.setColumnWidth(errorMsgCol, 40 * 256);
                col++;
            }

            // Sub-header row (Row 1): use getRow to avoid destroying cells created on row 1 by createMergedHeaderCell
            Row subHeaderRow = sheet.getRow(1) != null ? sheet.getRow(1) : sheet.createRow(1);
            col = monthStartCol;

            for (int i = 0; i < 12; i++) {
                Cell maintainedCell = subHeaderRow.createCell(col++);
                maintainedCell.setCellValue("Maintained");
                maintainedCell.setCellStyle(subHeaderStyle);

                Cell targetCell = subHeaderRow.createCell(col++);
                targetCell.setCellValue("Target");
                targetCell.setCellStyle(subHeaderStyle);
            }

            // Set consistent row heights for header rows
            topHeaderRow.setHeightInPoints(25);
            subHeaderRow.setHeightInPoints(22);

            // -------------------------------------------------
            // DATA ROWS
            // -------------------------------------------------

            int rowNum = 2;

            for (int r = 0; r < dtoList.size(); r++) {
                SpinningMarginReportDTO dto = dtoList.get(r);

                Row row = sheet.createRow(rowNum++);
                row.setHeightInPoints(20);

                int dataCol = 0;

                createTextCell(
                        row,
                        dataCol++,
                        dto.getPlantName(),
                        textStyle);

                createTextCell(
                        row,
                        dataCol++,
                        dto.getMarginType(),
                        textStyle);

                createTextCell(
                        row,
                        dataCol++,
                        dto.getUtilityName(),
                        textStyle);

                createTextCell(
                        row,
                        dataCol++,
                        dto.getUtilityCode(),
                        textStyle);

                // Maintained and target values in adjacent columns.
                Double[] maintainedValues = {
                    dto.getApr(), dto.getMay(), dto.getJun(),
                    dto.getJul(), dto.getAug(), dto.getSep(),
                    dto.getOct(), dto.getNov(), dto.getDec(),
                    dto.getJan(), dto.getFeb(), dto.getMar()
                };

                Double[] targetValues = {
                    dto.getAprTarget(), dto.getMayTarget(), dto.getJunTarget(),
                    dto.getJulTarget(), dto.getAugTarget(), dto.getSepTarget(),
                    dto.getOctTarget(), dto.getNovTarget(), dto.getDecTarget(),
                    dto.getJanTarget(), dto.getFebTarget(), dto.getMarTarget()
                };

                for (int i = 0; i < 12; i++) {

                    createNumericCell(
                            row,
                            dataCol++,
                            maintainedValues[i],
                            numericStyle);

                    createNumericCell(
                            row,
                            dataCol++,
                            targetValues[i],
                            numericStyle);
                }

                createTextCell(
                        row,
                        dataCol++,
                        dto.getRemarks(),
                        remarksStyle);

                createTextCell(
                        row,
                        dataCol++,
                        dto.getModifiedBy(),
                        textStyle);

                createTextCell(
                        row,
                        dataCol++,
                        dto.getId() != null ? dto.getId().toString() : "",
                        textStyle);

                String hash = dto.getDataHash() != null && !dto.getDataHash().isEmpty()
                        ? dto.getDataHash()
                        : generateSpinningMarginReportHash(dto);
                createTextCell(
                        row,
                        dataCol++,
                        hash,
                        textStyle);

                if (isErrorExcel) {
                    createTextCell(row, dataCol++, "Failed", errorStyle != null ? errorStyle : textStyle);
                    String msg = (errorMessages != null && r < errorMessages.size()) ? errorMessages.get(r) : "";
                    createTextCell(row, dataCol++, msg, errorStyle != null ? errorStyle : remarksStyle);
                }
            }

            // -------------------------------------------------
            // SHEET FORMATTING
            // -------------------------------------------------

            // Freeze top 2 header rows and first 4 static columns (Plant Name, Type, Utility Name, Utility Code)
            sheet.createFreezePane(4, 2);

            // AutoFilter on the sub-header row (row 1)
            sheet.setAutoFilter(
                    new CellRangeAddress(
                            1,
                            Math.max(1, rowNum - 1),
                            0,
                            isErrorExcel ? errorMsgCol : remarksCol));

            // Hide maintained columns if showMaintained is false (e.g. for input)
            if (!showMaintained) {
                for (int mCol : maintainedCols) {
                    sheet.setColumnHidden(mCol, true);
                }
            }

            // Hide Modified By, id and dataHash columns
            sheet.setColumnHidden(modifiedByCol, true);
            sheet.setColumnHidden(idCol, true);
            sheet.setColumnHidden(dataHashCol, true);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            workbook.write(outputStream);

            return outputStream.toByteArray();
        }
    }

    // =========================================================
    // READ EXCEL
    // =========================================================

    private List<SpinningMarginReportDTO> readExcel(InputStream inputStream, String aopYear) throws IOException {
        List<SpinningMarginReportDTO> dtoList = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return dtoList;
            }

            int lastRowNum = sheet.getLastRowNum();
            if (lastRowNum < 2) {
                return dtoList;
            }

            Row row0 = sheet.getRow(0);
            Row row1 = sheet.getRow(1);

            int lastCellNum = Math.max(
                    row0 != null ? row0.getLastCellNum() : 0,
                    row1 != null ? row1.getLastCellNum() : 0
            );

            int plantColIdx = 0;
            int typeColIdx = 1;
            int utilityNameColIdx = -1;
            int utilityCodeColIdx = -1;
            int remarksColIdx = -1;
            int modifiedByColIdx = -1;
            int idColIdx = -1;
            int dataHashColIdx = -1;

            List<Integer> targetColIndices = new ArrayList<>();
            List<Integer> maintainedColIndices = new ArrayList<>();

            for (int c = 0; c < lastCellNum; c++) {
                String h0 = row0 != null ? toStringVal(row0.getCell(c)) : "";
                String h1 = row1 != null ? toStringVal(row1.getCell(c)) : "";
                String h0Trimmed = h0 != null ? h0.trim().toLowerCase() : "";
                String h1Trimmed = h1 != null ? h1.trim().toLowerCase() : "";

                if (h0Trimmed.equals("datahash") || h1Trimmed.equals("datahash")) {
                    dataHashColIdx = c;
                } else if (h0Trimmed.equals("id") || h1Trimmed.equals("id")) {
                    idColIdx = c;
                } else if (h0Trimmed.contains("remark") || h1Trimmed.contains("remark")) {
                    remarksColIdx = c;
                } else if (h0Trimmed.contains("modified by") || h1Trimmed.contains("modified by")) {
                    modifiedByColIdx = c;
                } else if (h0Trimmed.contains("utility name") || h1Trimmed.contains("utility name")) {
                    utilityNameColIdx = c;
                } else if (h0Trimmed.contains("utility code") || h1Trimmed.contains("utility code")
                        || h0Trimmed.contains("sap code") || h1Trimmed.contains("sap code")
                        || h0Trimmed.contains("sapcode") || h1Trimmed.contains("sapcode")) {
                    utilityCodeColIdx = c;
                } else if (h0Trimmed.contains("plant") || h1Trimmed.contains("plant")) {
                    plantColIdx = c;
                } else if (h0Trimmed.contains("type") || h1Trimmed.contains("type")) {
                    typeColIdx = c;
                }

                if (h1Trimmed.equalsIgnoreCase("target")) {
                    targetColIndices.add(c);
                } else if (h1Trimmed.equalsIgnoreCase("maintained")) {
                    maintainedColIndices.add(c);
                }
            }

            if (targetColIndices.size() != 12) {
                targetColIndices.clear();
                for (int i = 0; i < 12; i++) {
                    targetColIndices.add(5 + (i * 2));
                }
            }

            if (maintainedColIndices.size() != 12) {
                maintainedColIndices.clear();
                for (int i = 0; i < 12; i++) {
                    maintainedColIndices.add(4 + (i * 2));
                }
            }

            if (utilityNameColIdx == -1) {
                utilityNameColIdx = 2;
            }
            if (utilityCodeColIdx == -1) {
                utilityCodeColIdx = 3;
            }
            if (remarksColIdx == -1) {
                remarksColIdx = 28;
            }
            if (idColIdx == -1 && lastCellNum > 30) {
                idColIdx = 30;
            }
            if (dataHashColIdx == -1 && lastCellNum > 31) {
                dataHashColIdx = 31;
            }

            for (int r = 2; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }

                boolean empty = true;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        empty = false;
                        break;
                    }
                }
                if (empty) {
                    continue;
                }

                SpinningMarginReportDTO dto = new SpinningMarginReportDTO();
                dto.setAopYear(aopYear);

                dto.setPlantName(toStringVal(row.getCell(plantColIdx)));
                dto.setMarginType(toStringVal(row.getCell(typeColIdx)));
                if (utilityNameColIdx >= 0) {
                    dto.setUtilityName(toStringVal(row.getCell(utilityNameColIdx)));
                }
                if (utilityCodeColIdx >= 0) {
                    dto.setUtilityCode(toStringVal(row.getCell(utilityCodeColIdx)));
                }

                if (idColIdx >= 0) {
                    dto.setId(toUUID(row.getCell(idColIdx)));
                }

                if (dataHashColIdx >= 0) {
                    dto.setDataHash(toStringVal(row.getCell(dataHashColIdx)));
                }

                if (remarksColIdx >= 0) {
                    dto.setRemarks(toStringVal(row.getCell(remarksColIdx)));
                }

                if (modifiedByColIdx >= 0) {
                    dto.setModifiedBy(toStringVal(row.getCell(modifiedByColIdx)));
                }

                dto.setApr(toDouble(row.getCell(maintainedColIndices.get(0))));
                dto.setMay(toDouble(row.getCell(maintainedColIndices.get(1))));
                dto.setJun(toDouble(row.getCell(maintainedColIndices.get(2))));
                dto.setJul(toDouble(row.getCell(maintainedColIndices.get(3))));
                dto.setAug(toDouble(row.getCell(maintainedColIndices.get(4))));
                dto.setSep(toDouble(row.getCell(maintainedColIndices.get(5))));
                dto.setOct(toDouble(row.getCell(maintainedColIndices.get(6))));
                dto.setNov(toDouble(row.getCell(maintainedColIndices.get(7))));
                dto.setDec(toDouble(row.getCell(maintainedColIndices.get(8))));
                dto.setJan(toDouble(row.getCell(maintainedColIndices.get(9))));
                dto.setFeb(toDouble(row.getCell(maintainedColIndices.get(10))));
                dto.setMar(toDouble(row.getCell(maintainedColIndices.get(11))));

                dto.setAprTarget(toDouble(row.getCell(targetColIndices.get(0))));
                dto.setMayTarget(toDouble(row.getCell(targetColIndices.get(1))));
                dto.setJunTarget(toDouble(row.getCell(targetColIndices.get(2))));
                dto.setJulTarget(toDouble(row.getCell(targetColIndices.get(3))));
                dto.setAugTarget(toDouble(row.getCell(targetColIndices.get(4))));
                dto.setSepTarget(toDouble(row.getCell(targetColIndices.get(5))));
                dto.setOctTarget(toDouble(row.getCell(targetColIndices.get(6))));
                dto.setNovTarget(toDouble(row.getCell(targetColIndices.get(7))));
                dto.setDecTarget(toDouble(row.getCell(targetColIndices.get(8))));
                dto.setJanTarget(toDouble(row.getCell(targetColIndices.get(9))));
                dto.setFebTarget(toDouble(row.getCell(targetColIndices.get(10))));
                dto.setMarTarget(toDouble(row.getCell(targetColIndices.get(11))));

                dtoList.add(dto);
            }
        }
        return dtoList;
    }

    // =========================================================
    // HASH GENERATION & CHANGE DETECTION
    // =========================================================

    private String generateSpinningMarginReportHash(SpinningMarginReportDTO dto) {
        try {
            StringBuilder dataToHash = new StringBuilder();

            dataToHash.append(dto.getAprTarget() != null ? String.valueOf(dto.getAprTarget()) : "null").append("|");
            dataToHash.append(dto.getMayTarget() != null ? String.valueOf(dto.getMayTarget()) : "null").append("|");
            dataToHash.append(dto.getJunTarget() != null ? String.valueOf(dto.getJunTarget()) : "null").append("|");
            dataToHash.append(dto.getJulTarget() != null ? String.valueOf(dto.getJulTarget()) : "null").append("|");
            dataToHash.append(dto.getAugTarget() != null ? String.valueOf(dto.getAugTarget()) : "null").append("|");
            dataToHash.append(dto.getSepTarget() != null ? String.valueOf(dto.getSepTarget()) : "null").append("|");
            dataToHash.append(dto.getOctTarget() != null ? String.valueOf(dto.getOctTarget()) : "null").append("|");
            dataToHash.append(dto.getNovTarget() != null ? String.valueOf(dto.getNovTarget()) : "null").append("|");
            dataToHash.append(dto.getDecTarget() != null ? String.valueOf(dto.getDecTarget()) : "null").append("|");
            dataToHash.append(dto.getJanTarget() != null ? String.valueOf(dto.getJanTarget()) : "null").append("|");
            dataToHash.append(dto.getFebTarget() != null ? String.valueOf(dto.getFebTarget()) : "null").append("|");
            dataToHash.append(dto.getMarTarget() != null ? String.valueOf(dto.getMarTarget()) : "null").append("|");
            dataToHash.append(dto.getRemarks() != null ? dto.getRemarks().trim() : "null");

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(dataToHash.toString().getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception e) {
            log.error("[SpinningMarginReport] Error generating hash: {}", e.getMessage(), e);
            return "";
        }
    }

    private boolean isRecordModified(SpinningMarginReportDTO dto, Map<UUID, String> existingHashes) {
        if (dto.getId() == null) {
            return true;
        }

        String currentHash = generateSpinningMarginReportHash(dto);
        String importedHash = dto.getDataHash();

        if (importedHash != null && !importedHash.isEmpty()) {
            boolean modified = !importedHash.equals(currentHash);
            if (!modified) {
                log.debug("[SpinningMarginReport] Record {} unchanged - dataHash match", dto.getId());
            }
            return modified;
        }

        String dbHash = existingHashes != null ? existingHashes.get(dto.getId()) : null;
        if (dbHash != null) {
            boolean modified = !dbHash.equals(currentHash);
            if (!modified) {
                log.debug("[SpinningMarginReport] Record {} unchanged - DB hash match", dto.getId());
            }
            return modified;
        }

        return true;
    }

    private Double toDouble(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String val = cell.getStringCellValue().trim();
                return val.isEmpty() ? null : Double.parseDouble(val);
            }
        } catch (Exception e) {
            log.warn("[SpinningMarginReport] Failed to parse cell value: {}", e.getMessage());
        }
        return null;
    }

    private String toStringVal(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf(cell.getNumericCellValue());
        }
        return cell.toString().trim();
    }

    private UUID toUUID(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        try {
            String val = cell.getCellType() == CellType.STRING ? cell.getStringCellValue().trim() : String.valueOf(cell).trim();
            return val.isEmpty() ? null : UUID.fromString(val);
        } catch (Exception e) {
            log.warn("[SpinningMarginReport] Failed to parse UUID: {}", e.getMessage());
            return null;
        }
    }

    // =========================================================
    // EXCEL CELL & STYLE HELPERS
    // =========================================================

    private void createMergedHeaderCell(
            Sheet sheet,
            Row row,
            int rowStart,
            int rowEnd,
            int colStart,
            int colEnd,
            String value,
            CellStyle style) {

        if (rowStart != rowEnd || colStart != colEnd) {
            sheet.addMergedRegion(new CellRangeAddress(rowStart, rowEnd, colStart, colEnd));
        }

        Cell cell = row.createCell(colStart);
        cell.setCellValue(value != null ? value : "");
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

    private void createTextCell(
            Row row,
            int column,
            String value,
            CellStyle style) {

        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    /**
     * Write a real numeric Excel value.
     * Format removes unnecessary trailing decimal zeros.
     * Example: 240.0 displays as 240.
     */
    private void createNumericCell(
            Row row,
            int column,
            Double value,
            CellStyle style) {

        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = ExcelStyles.createHeaderStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createSubHeaderStyle(Workbook workbook) {
        CellStyle style = ExcelStyles.createHeaderStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = ExcelStyles.createDataStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createRemarksStyle(Workbook workbook) {
        CellStyle style = ExcelStyles.createRemarksStyle(workbook);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createNumericStyle(Workbook workbook) {
        CellStyle style = ExcelStyles.createNumericStyle(workbook, "0.##########");
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
}