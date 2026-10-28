package com.wks.caseengine.cpp.serviceimpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.dto.SpinningMarginDTO;
import com.wks.caseengine.cpp.repository.JMDSpinningMarginRepository;
import com.wks.caseengine.cpp.service.JMDSpinningMarginService;
import com.wks.caseengine.message.vm.AOPMessageVM;

@Service
public class JMDSpinningMarginServiceImpl implements JMDSpinningMarginService {

    private static final Logger logger = LoggerFactory.getLogger(JMDSpinningMarginServiceImpl.class);

    @Autowired
    private JMDSpinningMarginRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Override
    @Transactional
    public AOPMessageVM getSpinningMargin(List<UUID> plantIds, String aopYear) {
        logger.info("[JMDSpinningMargin] GET - plantIds: {}, aopYear: {}", plantIds, aopYear);
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
                    .createStoredProcedureQuery("dbo.CPP_GetSpinningMargin")
                    .registerStoredProcedureParameter("PlantIds", String.class, ParameterMode.IN)
                    .registerStoredProcedureParameter("AOPYear", String.class, ParameterMode.IN);

            sp.setParameter("PlantIds", plantIdsCsv);
            sp.setParameter("AOPYear", aopYear);

            logger.info("Executing stored procedure dbo.CPP_GetSpinningMargin for plantIds: {}, aopYear: {}", plantIdsCsv, aopYear);
            sp.execute();

            @SuppressWarnings("unchecked")
            List<Object[]> rawResults = sp.getResultList();
            logger.info("Raw result count: {}", rawResults.size());

            List<SpinningMarginDTO> result = new ArrayList<>();
            for (Object[] row : rawResults) {
                result.add(mapRowToDto(row));
            }

            logger.info("[JMDSpinningMargin] GET - found {} records", result.size());

            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(result);

        } catch (Exception e) {
            logger.error("[JMDSpinningMargin] GET error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
        }

        return vm;
    }

    @Override
    @Transactional
    public AOPMessageVM saveSpinningMargin(List<UUID> plantIds, String aopYear, List<SpinningMarginDTO> dtoList) {
        logger.info("[JMDSpinningMargin] SAVE - plantIds: {}, aopYear: {}, records: {}", plantIds, aopYear, dtoList.size());
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

            for (SpinningMarginDTO dto : dtoList) {
                try {
                    if (dto.getId() == null) {
                        errorCount++;
                        errorMessages.add("Record skipped: id is null");
                        continue;
                    }

                    repository.updateSpinningMargin(
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
                            dto.getRemarks());
                    successCount++;

                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = "Error processing id " + dto.getId() + ": " + e.getMessage();
                    errorMessages.add(errorMsg);
                    logger.error(errorMsg, e);
                }
            }

            logger.info("[JMDSpinningMargin] SAVE - success: {}, errors: {}", successCount, errorCount);

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
            logger.error("[JMDSpinningMargin] SAVE error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(null);
        }

        return vm;
    }

    @Override
    public byte[] exportSpinningMargin(List<UUID> plantIds, String aopYear) throws IOException {
        logger.info("[JMDSpinningMargin] EXPORT - plantIds: {}, aopYear: {}", plantIds, aopYear);

        try {
            AOPMessageVM result = getSpinningMargin(plantIds, aopYear);

            List<SpinningMarginDTO> dtoList = new ArrayList<>();
            if (result.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<SpinningMarginDTO> data = (List<SpinningMarginDTO>) result.getData();
                dtoList = data;
            }

            if (dtoList == null || dtoList.isEmpty()) {
                logger.warn("[JMDSpinningMargin] EXPORT - no data found");
                dtoList = new ArrayList<>();
            }

            return generateExcel(dtoList, aopYear, null);

        } catch (IOException e) {
            logger.error("[JMDSpinningMargin] EXPORT IOException: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("[JMDSpinningMargin] EXPORT error: {}", e.getMessage(), e);
            throw new IOException("Failed to export Spinning Margin: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public AOPMessageVM importSpinningMargin(List<UUID> plantIds, String aopYear, MultipartFile file) throws IOException {
        logger.info("[JMDSpinningMargin] IMPORT - plantIds: {}, aopYear: {}, file: {}",
                plantIds, aopYear, file.getOriginalFilename());

        AOPMessageVM vm = new AOPMessageVM();

        try (InputStream inputStream = file.getInputStream()) {
            List<SpinningMarginDTO> dtoList = readExcel(inputStream, aopYear);

            if (dtoList == null || dtoList.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("No valid records found in the Excel file");
                return vm;
            }

            List<SpinningMarginDTO> failedRecords = new ArrayList<>();
            List<SpinningMarginDTO> validRecords = new ArrayList<>();

            // Fetch existing data for remark validation
            AOPMessageVM existingData = getSpinningMargin(plantIds, aopYear);
            java.util.Map<UUID, String> existingRemarks = new java.util.HashMap<>();
            if (existingData.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<SpinningMarginDTO> existingList = (List<SpinningMarginDTO>) existingData.getData();
                for (SpinningMarginDTO existing : existingList) {
                    if (existing.getId() != null) {
                        existingRemarks.put(existing.getId(),
                                existing.getRemarks() != null ? existing.getRemarks().trim() : "");
                    }
                }
            }

            List<String> errorMessages = new ArrayList<>();

            int skippedCount = 0;

            for (SpinningMarginDTO dto : dtoList) {
                if (dto.getId() == null) {
                    failedRecords.add(dto);
                    errorMessages.add("Record skipped: id is null");
                    continue;
                }

                // Skip unchanged records (dataHash matches)
                if (!isRecordModified(dto, existingRemarks)) {
                    skippedCount++;
                    logger.debug("[JMDSpinningMargin] Skipping unchanged record: {}", dto.getId());
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

            logger.info("[JMDSpinningMargin] {} unchanged (skipped), {} modified to process", skippedCount, dtoList.size() - skippedCount);

            AOPMessageVM saveResult = saveSpinningMargin(plantIds, aopYear, validRecords);

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
            logger.error("[JMDSpinningMargin] IMPORT error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
        }

        return vm;
    }

    // ── Helper Methods ──────────────────────────────────────────────────────

    private SpinningMarginDTO mapRowToDto(Object[] row) {
        SpinningMarginDTO dto = new SpinningMarginDTO();
        dto.setId(row[0] != null ? UUID.fromString(row[0].toString()) : null);
        dto.setCppPlantName(row[1] != null ? row[1].toString() : null);
        dto.setUtilityName(row[2] != null ? row[2].toString() : null);
        dto.setUtilityCode(row[3] != null ? row[3].toString() : null);
        dto.setUom(row[4] != null ? row[4].toString() : null);
        dto.setApr(toDoubleObj(row[5]));
        dto.setMay(toDoubleObj(row[6]));
        dto.setJun(toDoubleObj(row[7]));
        dto.setJul(toDoubleObj(row[8]));
        dto.setAug(toDoubleObj(row[9]));
        dto.setSep(toDoubleObj(row[10]));
        dto.setOct(toDoubleObj(row[11]));
        dto.setNov(toDoubleObj(row[12]));
        dto.setDec(toDoubleObj(row[13]));
        dto.setJan(toDoubleObj(row[14]));
        dto.setFeb(toDoubleObj(row[15]));
        dto.setMar(toDoubleObj(row[16]));
        dto.setAopYear(row[17] != null ? row[17].toString() : null);
        dto.setRemarks(row[18] != null ? row[18].toString() : null);
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
            logger.warn("[JMDSpinningMargin] Failed to parse cell value: {}", e.getMessage());
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
            logger.warn("[JMDSpinningMargin] Failed to parse UUID: {}", e.getMessage());
            return null;
        }
    }

    private byte[] generateExcel(List<SpinningMarginDTO> dtoList, String aopYear, List<String> errorMessages) throws IOException {
        boolean isErrorExcel = errorMessages != null && !errorMessages.isEmpty();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(isErrorExcel ? "Spinning Margin Errors" : "Spinning Margin");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle remarksStyle = createRemarksStyle(workbook);
        CellStyle errorStyle = isErrorExcel ? createRemarksStyle(workbook) : null;

        String startYearSuffix = aopYear.substring(2, 4);
        String endYearSuffix = aopYear.substring(5, 7);
        String[] months = {"Apr-" + startYearSuffix, "May-" + startYearSuffix, "Jun-" + startYearSuffix,
                "Jul-" + startYearSuffix, "Aug-" + startYearSuffix, "Sep-" + startYearSuffix,
                "Oct-" + startYearSuffix, "Nov-" + startYearSuffix, "Dec-" + startYearSuffix,
                "Jan-" + endYearSuffix, "Feb-" + endYearSuffix, "Mar-" + endYearSuffix};

        int rowNum = 0;
        int col = 0;

        Row headerRow = sheet.createRow(rowNum++);
        String[] baseHeaders = {"CPP Plant", "Utility Name", "Utility Code", "UOM"};
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
            SpinningMarginDTO dto = dtoList.get(i);
            Row row = sheet.createRow(rowNum++);
            col = 0;

            setStringCell(row.createCell(col++), dto.getCppPlantName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getUtilityName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getUtilityCode(), dataStyle);
            setStringCell(row.createCell(col++), dto.getUom(), dataStyle);

            setNumericCell(row.createCell(monthStartCol + 0), dto.getApr(), dataStyle);
            setNumericCell(row.createCell(monthStartCol + 1), dto.getMay(), dataStyle);
            setNumericCell(row.createCell(monthStartCol + 2), dto.getJun(), dataStyle);
            setNumericCell(row.createCell(monthStartCol + 3), dto.getJul(), dataStyle);
            setNumericCell(row.createCell(monthStartCol + 4), dto.getAug(), dataStyle);
            setNumericCell(row.createCell(monthStartCol + 5), dto.getSep(), dataStyle);
            setNumericCell(row.createCell(monthStartCol + 6), dto.getOct(), dataStyle);
            setNumericCell(row.createCell(monthStartCol + 7), dto.getNov(), dataStyle);
            setNumericCell(row.createCell(monthStartCol + 8), dto.getDec(), dataStyle);
            setNumericCell(row.createCell(monthStartCol + 9), dto.getJan(), dataStyle);
            setNumericCell(row.createCell(monthStartCol + 10), dto.getFeb(), dataStyle);
            setNumericCell(row.createCell(monthStartCol + 11), dto.getMar(), dataStyle);
            col = monthStartCol + 12;

            setStringCell(row.createCell(col++), dto.getRemarks(), remarksStyle);
            setStringCell(row.createCell(col++), dto.getId() != null ? dto.getId().toString() : null, dataStyle);
            setStringCell(row.createCell(col++), generateSpinningMarginHash(dto), dataStyle);

            if (isErrorExcel) {
                setStringCell(row.createCell(col++), "Failed", errorStyle);
                setStringCell(row.createCell(col++), errorMessages.get(i), errorStyle);
            }
        }

        sheet.setColumnHidden(idCol, true);
        sheet.setColumnHidden(dataHashCol, true);

        for (int i = 0; i < totalColumns; i++) {
            if (i == remarksCol || i == commentCol) {
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

    private List<SpinningMarginDTO> readExcel(InputStream inputStream, String aopYear) throws IOException {
        List<SpinningMarginDTO> dtoList = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.iterator();

        if (!rowIterator.hasNext()) {
            workbook.close();
            return dtoList;
        }

        Row headerRow = rowIterator.next();
        int numCols = headerRow.getLastCellNum();

        int idColIdx = -1;
        int remarksColIdx = -1;
        int dataHashColIdx = -1;
        int monthStartIdx = 4;

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

            SpinningMarginDTO dto = new SpinningMarginDTO();
            dto.setAopYear(aopYear);

            // Parse text columns (0=CPP Plant, 1=Utility Name, 2=Utility Code, 3=UOM)
            dto.setCppPlantName(toStringVal(row.getCell(0)));
            dto.setUtilityName(toStringVal(row.getCell(1)));
            dto.setUtilityCode(toStringVal(row.getCell(2)));
            dto.setUom(toStringVal(row.getCell(3)));

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

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
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

    private CellStyle createRemarksStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setWrapText(true);
        return style;
    }

    private void setStringCell(Cell cell, String value, CellStyle style) {
        cell.setCellStyle(style);
        if (value != null) {
            cell.setCellValue(value);
        }
    }

    private void setNumericCell(Cell cell, Double value, CellStyle style) {
        cell.setCellStyle(style);
        if (value != null) {
            cell.setCellValue(value);
        }
    }

    private String generateSpinningMarginHash(SpinningMarginDTO dto) {
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

    private boolean isRecordModified(SpinningMarginDTO dto, java.util.Map<UUID, String> existingRemarks) {
        if (dto.getId() == null) {
            return true;
        }

        String importedHash = dto.getDataHash();
        if (importedHash == null || importedHash.isEmpty()) {
            return true;
        }

        String currentHash = generateSpinningMarginHash(dto);
        boolean modified = !importedHash.equals(currentHash);

        if (!modified) {
            logger.debug("[JMDSpinningMargin] Record {} unchanged - hash match", dto.getId());
        }

        return modified;
    }

}
