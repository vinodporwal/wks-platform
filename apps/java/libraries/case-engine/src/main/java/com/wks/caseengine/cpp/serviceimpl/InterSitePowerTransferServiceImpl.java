package com.wks.caseengine.cpp.serviceimpl;

import com.wks.caseengine.cpp.dto.InterSitePowerTransferDTO;
import com.wks.caseengine.cpp.dto.InterSitePowerTransferProjection;
import com.wks.caseengine.cpp.entity.CPPInterSitePowerTransfer;
import com.wks.caseengine.cpp.repository.InterSitePowerTransferRepository;
import com.wks.caseengine.cpp.service.InterSitePowerTransferService;
import com.wks.caseengine.cpp.utility.ExcelCells;
import com.wks.caseengine.cpp.utility.ExcelColumns;
import com.wks.caseengine.cpp.utility.ExcelRows;
import com.wks.caseengine.cpp.utility.ExcelStyles;
import com.wks.caseengine.cpp.utility.FiscalYearMonths;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
public class InterSitePowerTransferServiceImpl implements InterSitePowerTransferService {

    private static final Logger logger = LoggerFactory.getLogger(InterSitePowerTransferServiceImpl.class);

    @Autowired
    private InterSitePowerTransferRepository repository;

    // ──────────────────────────────────────────────────────────────────────
    //  GET
    // ──────────────────────────────────────────────────────────────────────
    @Override
    public AOPMessageVM getInterSitePowerTransfer(List<UUID> plantIds, String financialYear) {
        logger.info("[GET] Fetching inter site power transfer for plantIds: {}, financialYear: {}",
                plantIds, financialYear);
        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (plantIds == null || plantIds.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("Plant IDs are required");
                vm.setData(null);
                return vm;
            }

            String plantIdsCsv = plantIds.stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(","));

            List<InterSitePowerTransferProjection> projections =
                    repository.getInterSitePowerTransfer(plantIdsCsv, financialYear);

            List<InterSitePowerTransferDTO> dtoList = projections.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());

            logger.info("[GET] Returning {} records", dtoList.size());

            java.util.Map<String, Object> dataMap = new java.util.HashMap<>();
            dataMap.put("interSitePowerTransfers", dtoList);

            vm.setCode(200);
            vm.setMessage("Data fetched successfully");
            vm.setData(dataMap);
        } catch (Exception e) {
            logger.error("[GET] Error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to fetch data: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }

    private InterSitePowerTransferDTO mapToDto(InterSitePowerTransferProjection p) {
        InterSitePowerTransferDTO dto = new InterSitePowerTransferDTO();
        dto.setId(p.getId());
        dto.setFromPlantId(p.getFromPlantId());
        dto.setFromPlantName(p.getFromPlantName());
        dto.setToPlantId(p.getToPlantId());
        dto.setToPlantName(p.getToPlantName());
        dto.setUom(p.getUom());
        dto.setFinancialYear(p.getFinancialYear());
        dto.setRemarks(p.getRemarks());

        dto.setApr(p.getApr());
        dto.setMay(p.getMay());
        dto.setJun(p.getJun());
        dto.setJul(p.getJul());
        dto.setAug(p.getAug());
        dto.setSep(p.getSep());
        dto.setOct(p.getOct());
        dto.setNov(p.getNov());
        dto.setDec(p.getDec());
        dto.setJan(p.getJan());
        dto.setFeb(p.getFeb());
        dto.setMar(p.getMar());

        return dto;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  POST (save/update — insert new, update existing)
    // ──────────────────────────────────────────────────────────────────────
    @Override
    public AOPMessageVM saveInterSitePowerTransfer(
            List<UUID> plantIds,
            String financialYear,
            List<InterSitePowerTransferDTO> payload) {

        logger.info("[POST] Saving inter site power transfer, records: {}",
                payload != null ? payload.size() : 0);
        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (payload == null || payload.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("Payload is empty");
                vm.setData(null);
                return vm;
            }

            int inserted = 0;
            int updated = 0;
            int skipped = 0;

            for (InterSitePowerTransferDTO dto : payload) {
                // Validate required fields
                if (dto.getFromPlantId() == null || dto.getToPlantId() == null) {
                    logger.warn("[POST] Skipping record with missing fromPlantId or toPlantId");
                    skipped++;
                    continue;
                }
                if (dto.getFromPlantId().equals(dto.getToPlantId())) {
                    logger.warn("[POST] Skipping record: FromPlant and ToPlant are the same");
                    skipped++;
                    continue;
                }

                if (dto.getId() == null) {
                    // INSERT new record
                    UUID newId = UUID.randomUUID();
                    int rows = repository.insertRecord(
                            newId,
                            dto.getFromPlantId(),
                            dto.getToPlantId(),
                            dto.getUom(),
                            financialYear,
                            dto.getApr(), dto.getMay(), dto.getJun(), dto.getJul(),
                            dto.getAug(), dto.getSep(), dto.getOct(), dto.getNov(),
                            dto.getDec(), dto.getJan(), dto.getFeb(), dto.getMar(),
                            dto.getRemarks());
                    if (rows > 0) {
                        inserted++;
                    } else {
                        skipped++;
                    }
                } else {
                    // UPDATE existing record (only month columns + remarks)
                    int rows = repository.updateMonthValues(
                            dto.getId(),
                            dto.getApr(), dto.getMay(), dto.getJun(), dto.getJul(),
                            dto.getAug(), dto.getSep(), dto.getOct(), dto.getNov(),
                            dto.getDec(), dto.getJan(), dto.getFeb(), dto.getMar(),
                            dto.getRemarks());
                    if (rows > 0) {
                        updated++;
                    } else {
                        logger.warn("[POST] No row found for id: {}", dto.getId());
                        skipped++;
                    }
                }
            }

            logger.info("[POST] Inserted: {}, Updated: {}, Skipped: {}", inserted, updated, skipped);
            vm.setCode(200);
            vm.setMessage("Inter site power transfer saved successfully. Inserted: " + inserted
                    + ", Updated: " + updated + ", Skipped: " + skipped);
            vm.setData(null);
        } catch (Exception e) {
            logger.error("[POST] Error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to save: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  DELETE
    // ──────────────────────────────────────────────────────────────────────
    @Override
    public AOPMessageVM deleteInterSitePowerTransfer(UUID id) {
        logger.info("[DELETE] Deleting inter site power transfer id: {}", id);
        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (id == null) {
                vm.setCode(400);
                vm.setMessage("ID is required");
                vm.setData(null);
                return vm;
            }

            repository.deleteById(id);
            vm.setCode(200);
            vm.setMessage("Record deleted successfully");
            vm.setData(null);
        } catch (Exception e) {
            logger.error("[DELETE] Error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to delete: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  EXPORT
    // ──────────────────────────────────────────────────────────────────────
    @Override
    public byte[] exportInterSitePowerTransfer(List<UUID> plantIds, String financialYear) {
        logger.info("[Export] plantIds: {}, financialYear: {}", plantIds, financialYear);
        try {
            AOPMessageVM response = getInterSitePowerTransfer(plantIds, financialYear);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) response.getData();
            @SuppressWarnings("unchecked")
            List<InterSitePowerTransferDTO> dtoList =
                    (List<InterSitePowerTransferDTO>) dataMap.get("interSitePowerTransfers");
            if (dtoList == null) {
                dtoList = new ArrayList<>();
            }

            // Sort by fromPlantName → toPlantName
            dtoList.sort(Comparator
                    .comparing((InterSitePowerTransferDTO d) -> d.getFromPlantName() != null ? d.getFromPlantName() : "")
                    .thenComparing(d -> d.getToPlantName() != null ? d.getToPlantName() : ""));

            return buildExcel(dtoList, financialYear);
        } catch (Exception e) {
            logger.error("[Export] Error: {}", e.getMessage(), e);
            return null;
        }
    }

    private byte[] buildExcel(List<InterSitePowerTransferDTO> dtoList, String financialYear) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Inter Site Power Transfer");

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(workbook);
        CellStyle lockedStyle = ExcelStyles.createLockedStyle(workbook);
        CellStyle unlockedStyle = ExcelStyles.createUnlockedStyle(workbook);
        CellStyle remarksStyle = ExcelStyles.createEditableRemarksStyle(workbook);

        String[] monthHeaders = FiscalYearMonths.getMonthHeaders(financialYear);

        // Header row
        List<String> headers = new ArrayList<>();
        headers.add("From Plant");
        headers.add("To Plant");
        headers.add("UOM");
        for (String mh : monthHeaders) {
            headers.add(mh);
        }
        headers.add("Remarks");
        // Hidden columns
        headers.add("id");
        headers.add("_hash");

        Row headerRow = sheet.createRow(0);
        for (int c = 0; c < headers.size(); c++) {
            ExcelCells.setString(headerRow.createCell(c), headers.get(c), headerStyle);
        }

        // Data rows
        int rowNum = 1;
        for (InterSitePowerTransferDTO dto : dtoList) {
            Row row = sheet.createRow(rowNum++);
            int col = 0;

            // Locked (read-only) text columns
            ExcelCells.setString(row.createCell(col++), dto.getFromPlantName(), lockedStyle);
            ExcelCells.setString(row.createCell(col++), dto.getToPlantName(), lockedStyle);
            ExcelCells.setString(row.createCell(col++), dto.getUom(), lockedStyle);

            // Unlocked (editable) monthly columns
            ExcelCells.setDouble(row.createCell(col++), dto.getApr(), unlockedStyle);
            ExcelCells.setDouble(row.createCell(col++), dto.getMay(), unlockedStyle);
            ExcelCells.setDouble(row.createCell(col++), dto.getJun(), unlockedStyle);
            ExcelCells.setDouble(row.createCell(col++), dto.getJul(), unlockedStyle);
            ExcelCells.setDouble(row.createCell(col++), dto.getAug(), unlockedStyle);
            ExcelCells.setDouble(row.createCell(col++), dto.getSep(), unlockedStyle);
            ExcelCells.setDouble(row.createCell(col++), dto.getOct(), unlockedStyle);
            ExcelCells.setDouble(row.createCell(col++), dto.getNov(), unlockedStyle);
            ExcelCells.setDouble(row.createCell(col++), dto.getDec(), unlockedStyle);
            ExcelCells.setDouble(row.createCell(col++), dto.getJan(), unlockedStyle);
            ExcelCells.setDouble(row.createCell(col++), dto.getFeb(), unlockedStyle);
            ExcelCells.setDouble(row.createCell(col++), dto.getMar(), unlockedStyle);

            // Remarks is editable (unlocked + wrapped)
            ExcelCells.setString(row.createCell(col++), dto.getRemarks(), remarksStyle);

            // Hidden: id and row hash are locked
            ExcelCells.setString(row.createCell(col++),
                    dto.getId() != null ? dto.getId().toString() : "", lockedStyle);
            ExcelCells.setString(row.createCell(col++), computeRowHash(dto), lockedStyle);
        }

        // Hide id and hash columns
        int idColIndex = headers.size() - 2;
        int hashColIndex = headers.size() - 1;
        ExcelColumns.hideColumns(sheet, idColIndex, hashColIndex);

        // Auto-size + remarks width
        int remarksColIndex = headers.size() - 3;
        ExcelColumns.autoSize(sheet, headers.size(), remarksColIndex);

        // Protect the sheet so locked/unlocked cell styles take effect.
        // Only Apr–Mar and Remarks columns are unlocked (editable); all other
        // columns are locked with a grey background.
        sheet.protectSheet("");
        org.apache.poi.xssf.usermodel.XSSFSheet xssfSheet =
                (org.apache.poi.xssf.usermodel.XSSFSheet) sheet;
        xssfSheet.lockFormatColumns(false);
        xssfSheet.lockFormatRows(false);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        return baos.toByteArray();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  IMPORT
    // ──────────────────────────────────────────────────────────────────────
    @Override
    public AOPMessageVM importInterSitePowerTransfer(
            List<UUID> plantIds, String financialYear, MultipartFile file) {
        logger.info("[Import] plantIds: {}, financialYear: {}, fileName: {}",
                plantIds, financialYear, file != null ? file.getOriginalFilename() : "null");
        AOPMessageVM vm = new AOPMessageVM();

        try {
            List<InterSitePowerTransferDTO> excelData = readExcel(file.getInputStream());
            logger.info("[Import] Read {} records from Excel", excelData.size());

            List<InterSitePowerTransferDTO> validRecords = new ArrayList<>();
            List<InterSitePowerTransferDTO> failedRecords = new ArrayList<>();
            List<String> failureReasons = new ArrayList<>();
            int skippedCount = 0;

            for (InterSitePowerTransferDTO dto : excelData) {
                // 1. Validate id present + exists
                String validationError = validateRow(dto);
                if (validationError != null) {
                    failedRecords.add(dto);
                    failureReasons.add(validationError);
                    logger.warn("[Import] Invalid record (id={}): {}", dto.getId(), validationError);
                    continue;
                }

                // 2. Hash-based change detection
                String uploadedHash = computeRowHash(dto);
                String embeddedHash = dto.getRowHash();
                boolean rowChanged = embeddedHash == null || !embeddedHash.equals(uploadedHash);
                if (!rowChanged) {
                    skippedCount++;
                    logger.debug("[Import] Skipping unchanged record id={}", dto.getId());
                    continue;
                }

                // 3. Remarks must be updated when values change
                String remarkError = validateRemarksUpdated(dto);
                if (remarkError != null) {
                    failedRecords.add(dto);
                    failureReasons.add(remarkError);
                    logger.warn("[Import] Remarks not updated for id={}: {}", dto.getId(), remarkError);
                    continue;
                }

                validRecords.add(dto);
            }

            logger.info("[Import] {} unchanged (skipped), {} to update, {} failed",
                    skippedCount, validRecords.size(), failedRecords.size());

            // 4. Persist
            int updated = 0;
            for (InterSitePowerTransferDTO dto : validRecords) {
                try {
                    int rows = repository.updateMonthValues(
                            dto.getId(),
                            dto.getApr(), dto.getMay(), dto.getJun(), dto.getJul(),
                            dto.getAug(), dto.getSep(), dto.getOct(), dto.getNov(),
                            dto.getDec(), dto.getJan(), dto.getFeb(), dto.getMar(),
                            dto.getRemarks());
                    if (rows > 0) {
                        updated++;
                    } else {
                        failedRecords.add(dto);
                        failureReasons.add("Record with this ID does not exist in database");
                    }
                } catch (Exception e) {
                    failedRecords.add(dto);
                    failureReasons.add("Save failed: " + e.getMessage());
                    logger.error("[Import] Error saving id={}: {}", dto.getId(), e.getMessage(), e);
                }
            }

            // 5. Build response
            if (failedRecords.isEmpty()) {
                vm.setCode(200);
                if (updated == 0 && skippedCount > 0) {
                    vm.setMessage("No changes detected. All " + skippedCount + " records are unchanged.");
                } else {
                    vm.setMessage("Imported successfully. Updated: " + updated
                            + ", Unchanged: " + skippedCount + ".");
                }
                vm.setData(null);
            } else {
                byte[] errorFile = buildErrorExcel(failedRecords, failureReasons, financialYear);
                String base64File = java.util.Base64.getEncoder().encodeToString(errorFile);
                vm.setCode(400);
                vm.setMessage("Partial import: " + updated + " updated, " + skippedCount
                        + " unchanged, " + failedRecords.size() + " failed. Download error file for details.");
                vm.setData(base64File);
                logger.info("[Import] Exported {} failed records to error Excel", failedRecords.size());
            }

            logger.info("[Import] Completed - Updated: {}, Unchanged: {}, Failed: {}",
                    updated, skippedCount, failedRecords.size());

        } catch (Exception e) {
            logger.error("[Import] Error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to import: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }

    /**
     * Reads the exported Excel and maps each data row to a DTO.
     * Column order must match {@link #buildExcel}.
     */
    private List<InterSitePowerTransferDTO> readExcel(InputStream inputStream) throws Exception {
        List<InterSitePowerTransferDTO> records = new ArrayList<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : ExcelRows.getDataRows(sheet, 1)) {
                InterSitePowerTransferDTO dto = new InterSitePowerTransferDTO();
                int col = 0;

                // Static columns (0-2)
                dto.setFromPlantName(ExcelCells.toStringValue(row.getCell(col++)));
                dto.setToPlantName(ExcelCells.toStringValue(row.getCell(col++)));
                dto.setUom(ExcelCells.toStringValue(row.getCell(col++)));

                // 12 months (3-14)
                dto.setApr(ExcelCells.toDouble(row.getCell(col++)));
                dto.setMay(ExcelCells.toDouble(row.getCell(col++)));
                dto.setJun(ExcelCells.toDouble(row.getCell(col++)));
                dto.setJul(ExcelCells.toDouble(row.getCell(col++)));
                dto.setAug(ExcelCells.toDouble(row.getCell(col++)));
                dto.setSep(ExcelCells.toDouble(row.getCell(col++)));
                dto.setOct(ExcelCells.toDouble(row.getCell(col++)));
                dto.setNov(ExcelCells.toDouble(row.getCell(col++)));
                dto.setDec(ExcelCells.toDouble(row.getCell(col++)));
                dto.setJan(ExcelCells.toDouble(row.getCell(col++)));
                dto.setFeb(ExcelCells.toDouble(row.getCell(col++)));
                dto.setMar(ExcelCells.toDouble(row.getCell(col++)));

                // Remarks (15)
                dto.setRemarks(ExcelCells.toStringValue(row.getCell(col++)));

                // Hidden: id (16)
                String idStr = ExcelCells.toStringValue(row.getCell(col++));
                if (idStr != null && !idStr.trim().isEmpty()) {
                    try {
                        dto.setId(UUID.fromString(idStr.trim()));
                    } catch (IllegalArgumentException e) {
                        logger.warn("[Import] Invalid UUID: {}", idStr);
                    }
                }

                // Hidden: row hash (17)
                dto.setRowHash(ExcelCells.toStringValue(row.getCell(col++)));

                records.add(dto);
            }
        }
        return records;
    }

    private String validateRow(InterSitePowerTransferDTO dto) {
        if (dto.getId() == null) {
            return "Record ID is missing – the hidden 'id' column must not be modified.";
        }
        try {
            Optional<CPPInterSitePowerTransfer> optEntity = repository.findById(dto.getId());
            if (optEntity.isEmpty()) {
                return "Record with this ID does not exist in the database.";
            }
        } catch (Exception e) {
            logger.error("[Import Validation] Error checking id={}: {}", dto.getId(), e.getMessage());
        }
        return null;
    }

    private String validateRemarksUpdated(InterSitePowerTransferDTO dto) {
        if (dto.getRemarks() == null || dto.getRemarks().trim().isEmpty()) {
            return "Remarks are required when changing values. Please add a remark explaining the change.";
        }
        try {
            Optional<CPPInterSitePowerTransfer> optEntity = repository.findById(dto.getId());
            if (optEntity.isPresent()) {
                String dbRemarks = optEntity.get().getRemarks() != null ? optEntity.get().getRemarks().trim() : "";
                String importRemarks = dto.getRemarks().trim();
                if (dbRemarks.equals(importRemarks)) {
                    return "Remarks must be updated when changing values.";
                }
            }
        } catch (Exception e) {
            logger.error("[Import Remarks Validation] Error for id={}: {}", dto.getId(), e.getMessage());
        }
        return null;
    }

    private String computeRowHash(InterSitePowerTransferDTO dto) {
        String raw = String.join("|",
                fmt(dto.getApr()), fmt(dto.getMay()), fmt(dto.getJun()), fmt(dto.getJul()),
                fmt(dto.getAug()), fmt(dto.getSep()), fmt(dto.getOct()), fmt(dto.getNov()),
                fmt(dto.getDec()), fmt(dto.getJan()), fmt(dto.getFeb()), fmt(dto.getMar()),
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

    private byte[] buildErrorExcel(List<InterSitePowerTransferDTO> failedRecords,
                                   List<String> failureReasons, String financialYear) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Failed Records");

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(workbook);
        CellStyle dataStyle = ExcelStyles.createDataStyle(workbook);
        CellStyle remarksStyle = ExcelStyles.createRemarksStyle(workbook);
        CellStyle errorStyle = ExcelStyles.createErrorStyle(workbook);

        String[] monthHeaders = FiscalYearMonths.getMonthHeaders(financialYear);

        List<String> headers = new ArrayList<>();
        headers.add("From Plant");
        headers.add("To Plant");
        headers.add("UOM");
        for (String mh : monthHeaders) {
            headers.add(mh);
        }
        headers.add("Remarks");
        headers.add("Status");
        headers.add("Comment");

        Row headerRow = sheet.createRow(0);
        for (int c = 0; c < headers.size(); c++) {
            ExcelCells.setString(headerRow.createCell(c), headers.get(c), headerStyle);
        }

        for (int i = 0; i < failedRecords.size(); i++) {
            InterSitePowerTransferDTO dto = failedRecords.get(i);
            Row row = sheet.createRow(i + 1);
            int col = 0;

            ExcelCells.setString(row.createCell(col++), dto.getFromPlantName(), dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getToPlantName(), dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getUom(), dataStyle);

            ExcelCells.setDouble(row.createCell(col++), dto.getApr(), dataStyle);
            ExcelCells.setDouble(row.createCell(col++), dto.getMay(), dataStyle);
            ExcelCells.setDouble(row.createCell(col++), dto.getJun(), dataStyle);
            ExcelCells.setDouble(row.createCell(col++), dto.getJul(), dataStyle);
            ExcelCells.setDouble(row.createCell(col++), dto.getAug(), dataStyle);
            ExcelCells.setDouble(row.createCell(col++), dto.getSep(), dataStyle);
            ExcelCells.setDouble(row.createCell(col++), dto.getOct(), dataStyle);
            ExcelCells.setDouble(row.createCell(col++), dto.getNov(), dataStyle);
            ExcelCells.setDouble(row.createCell(col++), dto.getDec(), dataStyle);
            ExcelCells.setDouble(row.createCell(col++), dto.getJan(), dataStyle);
            ExcelCells.setDouble(row.createCell(col++), dto.getFeb(), dataStyle);
            ExcelCells.setDouble(row.createCell(col++), dto.getMar(), dataStyle);

            ExcelCells.setString(row.createCell(col++), dto.getRemarks(), remarksStyle);
            ExcelCells.setString(row.createCell(col++), "Failed", errorStyle);
            ExcelCells.setString(row.createCell(col++), failureReasons.get(i), errorStyle);
        }

        ExcelColumns.autoSize(sheet, headers.size(), -1);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        return baos.toByteArray();
    }
}
