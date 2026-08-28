package com.wks.caseengine.cpp.serviceimpl;

import com.wks.caseengine.cpp.dto.CPPStandbyLoadResponseDto;
import com.wks.caseengine.cpp.repository.CPPStandbyLoadRepository;
import com.wks.caseengine.cpp.service.StandbyLoadService;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.wks.caseengine.cpp.utility.ExcelCells;
import com.wks.caseengine.cpp.utility.ExcelColumns;
import com.wks.caseengine.cpp.utility.ExcelStyles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StandbyLoadServiceImpl implements StandbyLoadService {

    private static final Logger logger = LoggerFactory.getLogger(StandbyLoadServiceImpl.class);

    @Autowired
    private CPPStandbyLoadRepository repository;

    @Autowired
    private EntityManager entityManager;

    // ── GET ───────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM getStandbyLoadData(List<UUID> plantIds, String aopYear) {
        logger.info("[StandbyLoad] GET - plantIds: {}, aopYear: {}", plantIds, aopYear);
        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (plantIds == null || plantIds.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("plantIds cannot be null or empty");
                vm.setData(new ArrayList<>());
                return vm;
            }

            if (aopYear == null || aopYear.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("aopYear cannot be null or empty");
                vm.setData(new ArrayList<>());
                return vm;
            }

            String plantIdsCsv = plantIds.stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(","));

            StoredProcedureQuery sp = entityManager
                    .createStoredProcedureQuery("dbo.CPP_GetStandbyLoad")
                    .registerStoredProcedureParameter("PlantIds", String.class, ParameterMode.IN)
                    .registerStoredProcedureParameter("AOPYear", String.class, ParameterMode.IN);

            sp.setParameter("PlantIds", plantIdsCsv);
            sp.setParameter("AOPYear", aopYear);

            logger.info("Executing stored procedure dbo.CPP_GetStandbyLoad for plantIds: {}, aopYear: {}", plantIdsCsv, aopYear);
            sp.execute();

            @SuppressWarnings("unchecked")
            List<Object[]> rawResults = sp.getResultList();
            logger.info("[StandbyLoad] GET - SP returned {} records", rawResults.size());

            List<CPPStandbyLoadResponseDto> results = new ArrayList<>();
            for (Object[] row : rawResults) {
                results.add(mapRowToDto(row));
            }

            // Generate dataHash for each record (used for change detection on import)
            for (CPPStandbyLoadResponseDto dto : results) {
                dto.setDataHash(generateStandbyLoadHash(dto));
            }

            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(results);

        } catch (Exception e) {
            logger.error("[StandbyLoad] GET error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
        }

        return vm;
    }

    // ── SAVE ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM saveStandbyLoadData(
            List<UUID> plantIds,
            String aopYear,
            List<CPPStandbyLoadResponseDto> dtoList) {

        logger.info("[StandbyLoad] SAVE - plantIds: {}, aopYear: {}, records: {}",
                plantIds, aopYear, dtoList != null ? dtoList.size() : 0);
        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (dtoList == null || dtoList.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("Request body cannot be empty");
                return vm;
            }

            int successCount = 0;
            int errorCount = 0;
            List<String> errorMessages = new ArrayList<>();

            for (CPPStandbyLoadResponseDto dto : dtoList) {
                try {
                    if (dto.getId() == null) {
                        errorCount++;
                        errorMessages.add("Record skipped: id is null");
                        continue;
                    }

                    repository.updateStandbyLoad(
                            dto.getId(),
                            dto.getApr(),
                            dto.getMay(),
                            dto.getJun(),
                            dto.getJul(),
                            dto.getAug(),
                            dto.getSep(),
                            dto.getOct(),
                            dto.getNov(),
                            dto.getDec(),
                            dto.getJan(),
                            dto.getFeb(),
                            dto.getMar(),
                            dto.getRemarks(),
                            dto.getUom());
                    successCount++;

                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = "Error processing id " + dto.getId() + ": " + e.getMessage();
                    errorMessages.add(errorMsg);
                    logger.error(errorMsg, e);
                }
            }

            logger.info("[StandbyLoad] SAVE - success: {}, errors: {}", successCount, errorCount);

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
            logger.error("[StandbyLoad] SAVE error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(null);
        }

        return vm;
    }

    // ── EXPORT ────────────────────────────────────────────────────────────────

    @Override
    public byte[] exportStandbyLoadExcel(List<UUID> plantIds, String aopYear) throws IOException {
        logger.info("[StandbyLoad] EXPORT - plantIds: {}, aopYear: {}", plantIds, aopYear);

        try {
            AOPMessageVM result = getStandbyLoadData(plantIds, aopYear);

            List<CPPStandbyLoadResponseDto> dtoList = new ArrayList<>();
            if (result.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<CPPStandbyLoadResponseDto> data = (List<CPPStandbyLoadResponseDto>) result.getData();
                dtoList = data;
            }

            if (dtoList == null || dtoList.isEmpty()) {
                logger.warn("[StandbyLoad] EXPORT - no data found");
                dtoList = new ArrayList<>();
            }

            return generateExcel(dtoList, aopYear, null);

        } catch (IOException e) {
            logger.error("[StandbyLoad] EXPORT IOException: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("[StandbyLoad] EXPORT error: {}", e.getMessage(), e);
            throw new IOException("Failed to export Standby Load: " + e.getMessage(), e);
        }
    }

    // ── IMPORT ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM importStandbyLoadExcel(List<UUID> plantIds, String aopYear, MultipartFile file) throws IOException {
        logger.info("[StandbyLoad] IMPORT - plantIds: {}, aopYear: {}, file: {}",
                plantIds, aopYear, file.getOriginalFilename());

        AOPMessageVM vm = new AOPMessageVM();

        try (InputStream inputStream = file.getInputStream()) {
            List<CPPStandbyLoadResponseDto> dtoList = readExcel(inputStream, aopYear);

            if (dtoList == null || dtoList.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("No valid records found in the Excel file");
                return vm;
            }

            List<CPPStandbyLoadResponseDto> failedRecords = new ArrayList<>();
            List<CPPStandbyLoadResponseDto> validRecords = new ArrayList<>();

            // Fetch existing data for remark validation and change detection
            AOPMessageVM existingData = getStandbyLoadData(plantIds, aopYear);
            Map<UUID, String> existingRemarks = new HashMap<>();
            Map<UUID, String> existingHashes = new HashMap<>();
            if (existingData.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<CPPStandbyLoadResponseDto> existingList = (List<CPPStandbyLoadResponseDto>) existingData.getData();
                for (CPPStandbyLoadResponseDto existing : existingList) {
                    if (existing.getId() != null) {
                        existingRemarks.put(existing.getId(),
                                existing.getRemarks() != null ? existing.getRemarks().trim() : "");
                        existingHashes.put(existing.getId(), generateStandbyLoadHash(existing));
                    }
                }
            }

            List<String> errorMessages = new ArrayList<>();
            int skippedCount = 0;

            for (CPPStandbyLoadResponseDto dto : dtoList) {
                if (dto.getId() == null) {
                    failedRecords.add(dto);
                    errorMessages.add("Record skipped: id is null");
                    continue;
                }

                // Skip unchanged records (dataHash matches, or DB hash matches when dataHash missing)
                if (!isRecordModified(dto, existingHashes)) {
                    skippedCount++;
                    logger.debug("[StandbyLoad] Skipping unchanged record: {}", dto.getId());
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

            logger.info("[StandbyLoad] {} unchanged (skipped), {} modified to process",
                    skippedCount, dtoList.size() - skippedCount);

            AOPMessageVM saveResult = saveStandbyLoadData(plantIds, aopYear, validRecords);

            if (!failedRecords.isEmpty()) {
                byte[] errorExcel = generateExcel(failedRecords, aopYear, errorMessages);
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
            logger.error("[StandbyLoad] IMPORT error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
        }

        return vm;
    }

    // ── Mapping ───────────────────────────────────────────────────────────────
    // Maps a raw Object[] row from SP dbo.CPP_GetStandbyLoad to the DTO.
    // Column order must match the SP's SELECT clause:
    //   0:Id  1:AssetFkId  2:AssetName  3:PlantFkId
    //   4:Apr  5:May  6:Jun  7:Jul  8:Aug  9:Sep  10:Oct  11:Nov  12:Dec  13:Jan  14:Feb  15:Mar
    //   16:AOPYear  17:Remarks  18:UOM  19:CreatedDate  20:ModifiedDate
    //   21:GeneratingPlant  22:UtilityDistributed  23:DistributedSapCode
    //   24:UtilityGenerated  25:GeneratedUtilityCode
    //   26:CppPlantName  27:PlantCode  28:Type
    //   29:SiteFkId  30:VerticalFkId

    private CPPStandbyLoadResponseDto mapRowToDto(Object[] row) {
        CPPStandbyLoadResponseDto dto = new CPPStandbyLoadResponseDto();
        dto.setId(toUUIDObj(row[0]));
        dto.setAssetFkId(toUUIDObj(row[1]));
        dto.setAssetName(toStringObj(row[2]));
        dto.setPlantFkId(toUUIDObj(row[3]));

        dto.setApr(toDoubleObj(row[4]));
        dto.setMay(toDoubleObj(row[5]));
        dto.setJun(toDoubleObj(row[6]));
        dto.setJul(toDoubleObj(row[7]));
        dto.setAug(toDoubleObj(row[8]));
        dto.setSep(toDoubleObj(row[9]));
        dto.setOct(toDoubleObj(row[10]));
        dto.setNov(toDoubleObj(row[11]));
        dto.setDec(toDoubleObj(row[12]));
        dto.setJan(toDoubleObj(row[13]));
        dto.setFeb(toDoubleObj(row[14]));
        dto.setMar(toDoubleObj(row[15]));

        dto.setAopYear(toStringObj(row[16]));
        dto.setRemarks(toStringObj(row[17]));
        dto.setUom(toStringObj(row[18]));
        dto.setCreatedDate(toStringObj(row[19]));
        dto.setModifiedDate(toStringObj(row[20]));

        dto.setGeneratingPlant(toStringObj(row[21]));
        dto.setUtilityDistributed(toStringObj(row[22]));
        dto.setDistributedSapCode(toStringObj(row[23]));
        dto.setUtilityGenerated(toStringObj(row[24]));
        dto.setGeneratedUtilityCode(toStringObj(row[25]));
        dto.setCppPlantName(toStringObj(row[26]));
        dto.setPlantCode(toStringObj(row[27]));
        dto.setType(toStringObj(row[28]));

        dto.setSiteFkId(toUUIDObj(row[29]));
        dto.setVerticalFkId(toUUIDObj(row[30]));
        return dto;
    }

    private Double toDoubleObj(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String toStringObj(Object value) {
        if (value == null) return null;
        return value.toString();
    }

    private UUID toUUIDObj(Object value) {
        if (value == null) return null;
        try {
            return UUID.fromString(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    // ── Excel Generation ──────────────────────────────────────────────────────

    private byte[] generateExcel(List<CPPStandbyLoadResponseDto> dtoList, String aopYear, List<String> errorMessages) throws IOException {
        boolean isErrorExcel = errorMessages != null && !errorMessages.isEmpty();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(isErrorExcel ? "Standby Load Errors" : "Standby Load");

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(workbook);
        CellStyle lockedStyle = ExcelStyles.createLockedStyle(workbook);
        CellStyle unlockedStyle = ExcelStyles.createUnlockedStyle(workbook);
        CellStyle remarksStyle = ExcelStyles.createEditableRemarksStyle(workbook);
        CellStyle errorStyle = isErrorExcel ? ExcelStyles.createErrorStyle(workbook) : null;

        String startYearSuffix = aopYear.length() >= 4 ? aopYear.substring(2, 4) : "";
        String endYearSuffix = aopYear.length() >= 7 ? aopYear.substring(5, 7) : "";
        String[] months = {"Apr-" + startYearSuffix, "May-" + startYearSuffix, "Jun-" + startYearSuffix,
                "Jul-" + startYearSuffix, "Aug-" + startYearSuffix, "Sep-" + startYearSuffix,
                "Oct-" + startYearSuffix, "Nov-" + startYearSuffix, "Dec-" + startYearSuffix,
                "Jan-" + endYearSuffix, "Feb-" + endYearSuffix, "Mar-" + endYearSuffix};

        int rowNum = 0;
        int col = 0;

        Row headerRow = sheet.createRow(rowNum++);
        // Base headers: Generating Plant(assetName), Utility Distributed, Distributed SAP Code,
        // Utility Generated, Generated SAP Code, Distribution Plant, UOM, Type
        String[] baseHeaders = {"Generating Plant", "Utility Distributed", "Distributed SAP Code",
                "Utility Generated", "Generated SAP Code",
                "Distribution Plant", "UOM", "Type"};
        for (String header : baseHeaders) {
            headerRow.createCell(col).setCellValue(header);
            headerRow.getCell(col++).setCellStyle(headerStyle);
        }

        int monthStartCol = col;
        for (String month : months) {
            headerRow.createCell(col).setCellValue(month);
            headerRow.getCell(col++).setCellStyle(headerStyle);
        }

        int remarksCol = col;
        headerRow.createCell(col).setCellValue("Remarks");
        headerRow.getCell(col++).setCellStyle(headerStyle);

        int idCol = col;
        headerRow.createCell(col).setCellValue("id");
        headerRow.getCell(col++).setCellStyle(headerStyle);

        int dataHashCol = col;
        headerRow.createCell(col).setCellValue("dataHash");
        headerRow.getCell(col++).setCellStyle(headerStyle);

        int commentCol = -1;
        if (isErrorExcel) {
            headerRow.createCell(col).setCellValue("Status");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            commentCol = col;
            headerRow.createCell(col).setCellValue("Error Message");
            headerRow.getCell(col++).setCellStyle(headerStyle);
        }

        int totalColumns = col;

        for (int i = 0; i < dtoList.size(); i++) {
            CPPStandbyLoadResponseDto dto = dtoList.get(i);
            Row row = sheet.createRow(rowNum++);
            col = 0;

            // Locked (read-only) text columns
            ExcelCells.setString(row.createCell(col++), dto.getAssetName(), lockedStyle);
            ExcelCells.setString(row.createCell(col++), dto.getUtilityDistributed(), lockedStyle);
            ExcelCells.setString(row.createCell(col++), dto.getDistributedSapCode(), lockedStyle);
            ExcelCells.setString(row.createCell(col++), dto.getUtilityGenerated(), lockedStyle);
            ExcelCells.setString(row.createCell(col++), dto.getGeneratedUtilityCode(), lockedStyle);
            ExcelCells.setString(row.createCell(col++), dto.getCppPlantName(), lockedStyle);
            ExcelCells.setString(row.createCell(col++), dto.getUom(), lockedStyle);
            ExcelCells.setString(row.createCell(col++), dto.getType(), lockedStyle);

            // Unlocked (editable) monthly columns
            ExcelCells.setDouble(row.createCell(monthStartCol + 0), dto.getApr(), unlockedStyle);
            ExcelCells.setDouble(row.createCell(monthStartCol + 1), dto.getMay(), unlockedStyle);
            ExcelCells.setDouble(row.createCell(monthStartCol + 2), dto.getJun(), unlockedStyle);
            ExcelCells.setDouble(row.createCell(monthStartCol + 3), dto.getJul(), unlockedStyle);
            ExcelCells.setDouble(row.createCell(monthStartCol + 4), dto.getAug(), unlockedStyle);
            ExcelCells.setDouble(row.createCell(monthStartCol + 5), dto.getSep(), unlockedStyle);
            ExcelCells.setDouble(row.createCell(monthStartCol + 6), dto.getOct(), unlockedStyle);
            ExcelCells.setDouble(row.createCell(monthStartCol + 7), dto.getNov(), unlockedStyle);
            ExcelCells.setDouble(row.createCell(monthStartCol + 8), dto.getDec(), unlockedStyle);
            ExcelCells.setDouble(row.createCell(monthStartCol + 9), dto.getJan(), unlockedStyle);
            ExcelCells.setDouble(row.createCell(monthStartCol + 10), dto.getFeb(), unlockedStyle);
            ExcelCells.setDouble(row.createCell(monthStartCol + 11), dto.getMar(), unlockedStyle);
            col = monthStartCol + 12;

            // Remarks is editable (unlocked + wrapped)
            ExcelCells.setString(row.createCell(col++), dto.getRemarks(), remarksStyle);
            // Hidden id/dataHash columns are locked
            ExcelCells.setString(row.createCell(col++), dto.getId() != null ? dto.getId().toString() : null, lockedStyle);
            ExcelCells.setString(row.createCell(col++), generateStandbyLoadHash(dto), lockedStyle);

            if (isErrorExcel) {
                ExcelCells.setString(row.createCell(col++), "Failed", errorStyle);
                ExcelCells.setString(row.createCell(col++), errorMessages.get(i), errorStyle);
            }
        }

        // Hide: id, dataHash, F(Distribution Plant), H(Type)
        ExcelColumns.hideColumns(sheet, idCol, dataHashCol, 5, 7);
        ExcelColumns.autoSize(sheet, totalColumns, remarksCol);
        if (commentCol >= 0) {
            sheet.setColumnWidth(commentCol, ExcelColumns.DEFAULT_REMARKS_WIDTH);
        }

        // Protect the sheet so locked/unlocked cell styles take effect.
        // Only Apr–Mar and Remarks columns are unlocked (editable); all other
        // columns are locked with a grey background.
        sheet.protectSheet("");
        XSSFSheet xssfSheet = (XSSFSheet) sheet;
        xssfSheet.lockFormatColumns(false);  // allow column width changes + unhide
        xssfSheet.lockFormatRows(false);     // allow row height changes

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    // ── Excel Reading ─────────────────────────────────────────────────────────

    private List<CPPStandbyLoadResponseDto> readExcel(InputStream inputStream, String aopYear) throws IOException {
        List<CPPStandbyLoadResponseDto> dtoList = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.iterator();

        if (!rowIterator.hasNext()) {
            workbook.close();
            return dtoList;
        }

        Row headerRow = rowIterator.next();
        int numCols = headerRow.getLastCellNum();

        // Base columns: 0=Generating Plant, 1=Utility Distributed, 2=Distributed SAP Code,
        // 3=Utility Generated, 4=Generated SAP Code,
        // 5=Distribution Plant, 6=UOM, 7=Type → monthStartIdx = 8
        int monthStartIdx = 8;

        int idColIdx = -1;
        int remarksColIdx = -1;
        int dataHashColIdx = -1;

        for (int i = 0; i < numCols; i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null) continue;
            String header = cell.getStringCellValue().trim().toLowerCase();
            if (header.equals("id")) {
                idColIdx = i;
            } else if (header.equals("remarks")) {
                remarksColIdx = i;
            } else if (header.equals("datahash")) {
                dataHashColIdx = i;
            }
        }

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            if (row.getLastCellNum() < monthStartIdx) continue;

            CPPStandbyLoadResponseDto dto = new CPPStandbyLoadResponseDto();
            dto.setAopYear(aopYear);

            // Parse text columns
            dto.setAssetName(toStringVal(row.getCell(0)));
            dto.setUtilityDistributed(toStringVal(row.getCell(1)));
            dto.setDistributedSapCode(toStringVal(row.getCell(2)));
            dto.setUtilityGenerated(toStringVal(row.getCell(3)));
            dto.setGeneratedUtilityCode(toStringVal(row.getCell(4)));
            dto.setCppPlantName(toStringVal(row.getCell(5)));
            dto.setUom(toStringVal(row.getCell(6)));
            dto.setType(toStringVal(row.getCell(7)));

            if (idColIdx >= 0) {
                dto.setId(toUUID(row.getCell(idColIdx)));
            }

            if (remarksColIdx >= 0) {
                dto.setRemarks(toStringVal(row.getCell(remarksColIdx)));
            }

            if (dataHashColIdx >= 0) {
                dto.setDataHash(toStringVal(row.getCell(dataHashColIdx)));
            }

            if (numCols > monthStartIdx + 0) dto.setApr(toDouble(row.getCell(monthStartIdx + 0)));
            if (numCols > monthStartIdx + 1) dto.setMay(toDouble(row.getCell(monthStartIdx + 1)));
            if (numCols > monthStartIdx + 2) dto.setJun(toDouble(row.getCell(monthStartIdx + 2)));
            if (numCols > monthStartIdx + 3) dto.setJul(toDouble(row.getCell(monthStartIdx + 3)));
            if (numCols > monthStartIdx + 4) dto.setAug(toDouble(row.getCell(monthStartIdx + 4)));
            if (numCols > monthStartIdx + 5) dto.setSep(toDouble(row.getCell(monthStartIdx + 5)));
            if (numCols > monthStartIdx + 6) dto.setOct(toDouble(row.getCell(monthStartIdx + 6)));
            if (numCols > monthStartIdx + 7) dto.setNov(toDouble(row.getCell(monthStartIdx + 7)));
            if (numCols > monthStartIdx + 8) dto.setDec(toDouble(row.getCell(monthStartIdx + 8)));
            if (numCols > monthStartIdx + 9) dto.setJan(toDouble(row.getCell(monthStartIdx + 9)));
            if (numCols > monthStartIdx + 10) dto.setFeb(toDouble(row.getCell(monthStartIdx + 10)));
            if (numCols > monthStartIdx + 11) dto.setMar(toDouble(row.getCell(monthStartIdx + 11)));

            dtoList.add(dto);
        }

        workbook.close();
        return dtoList;
    }

    // ── Hash & Change Detection ───────────────────────────────────────────────

    private String generateStandbyLoadHash(CPPStandbyLoadResponseDto dto) {
        try {
            StringBuilder dataToHash = new StringBuilder();

            dataToHash.append(dto.getApr() != null ? String.valueOf(dto.getApr()) : "null").append("|");
            dataToHash.append(dto.getMay() != null ? String.valueOf(dto.getMay()) : "null").append("|");
            dataToHash.append(dto.getJun() != null ? String.valueOf(dto.getJun()) : "null").append("|");
            dataToHash.append(dto.getJul() != null ? String.valueOf(dto.getJul()) : "null").append("|");
            dataToHash.append(dto.getAug() != null ? String.valueOf(dto.getAug()) : "null").append("|");
            dataToHash.append(dto.getSep() != null ? String.valueOf(dto.getSep()) : "null").append("|");
            dataToHash.append(dto.getOct() != null ? String.valueOf(dto.getOct()) : "null").append("|");
            dataToHash.append(dto.getNov() != null ? String.valueOf(dto.getNov()) : "null").append("|");
            dataToHash.append(dto.getDec() != null ? String.valueOf(dto.getDec()) : "null").append("|");
            dataToHash.append(dto.getJan() != null ? String.valueOf(dto.getJan()) : "null").append("|");
            dataToHash.append(dto.getFeb() != null ? String.valueOf(dto.getFeb()) : "null").append("|");
            dataToHash.append(dto.getMar() != null ? String.valueOf(dto.getMar()) : "null").append("|");
            dataToHash.append(dto.getRemarks() != null ? dto.getRemarks() : "null");

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(dataToHash.toString().getBytes("UTF-8"));

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

    private boolean isRecordModified(CPPStandbyLoadResponseDto dto, Map<UUID, String> existingHashes) {
        if (dto.getId() == null) {
            return true;
        }

        String currentHash = generateStandbyLoadHash(dto);
        String importedHash = dto.getDataHash();

        if (importedHash != null && !importedHash.isEmpty()) {
            // dataHash available: compare Excel dataHash vs hash of current Excel values
            boolean modified = !importedHash.equals(currentHash);
            if (!modified) {
                logger.debug("[StandbyLoad] Record {} unchanged - dataHash match", dto.getId());
            }
            return modified;
        }

        // dataHash not available: fall back to comparing current Excel values vs DB values
        String dbHash = existingHashes != null ? existingHashes.get(dto.getId()) : null;
        if (dbHash != null) {
            boolean modified = !dbHash.equals(currentHash);
            if (!modified) {
                logger.debug("[StandbyLoad] Record {} unchanged - DB hash match", dto.getId());
            }
            return modified;
        }

        // No dataHash and no DB hash available: treat as modified (backward compatible)
        return true;
    }

    // ── Cell Helpers ──────────────────────────────────────────────────────────

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
            logger.warn("[StandbyLoad] Failed to parse cell value: {}", e.getMessage());
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
        return String.valueOf(cell);
    }

    private UUID toUUID(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        try {
            String val = cell.getCellType() == CellType.STRING ? cell.getStringCellValue().trim() : String.valueOf(cell);
            return val.isEmpty() ? null : UUID.fromString(val);
        } catch (Exception e) {
            logger.warn("[StandbyLoad] Failed to parse UUID: {}", e.getMessage());
            return null;
        }
    }
}
