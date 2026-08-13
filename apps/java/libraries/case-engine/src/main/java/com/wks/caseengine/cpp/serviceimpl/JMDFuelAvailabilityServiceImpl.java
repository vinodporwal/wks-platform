package com.wks.caseengine.cpp.serviceimpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
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

import com.wks.caseengine.cpp.dto.FuelAvailabilityTransactionDTO;
import com.wks.caseengine.cpp.dto.FuelMasterWithCategoryDTO;
import com.wks.caseengine.cpp.dto.FuelWithCategoryProjection;
import com.wks.caseengine.cpp.entity.CPPFuelAvailabilityTransaction;
import com.wks.caseengine.cpp.repository.JMDFuelAvailabilityRepository;
import com.wks.caseengine.cpp.service.JMDFuelAvailabilityService;
import com.wks.caseengine.cpp.utility.ExcelCells;
import com.wks.caseengine.cpp.utility.ExcelColumns;
import com.wks.caseengine.cpp.utility.ExcelRows;
import com.wks.caseengine.cpp.utility.ExcelStyles;
import com.wks.caseengine.cpp.utility.FiscalYearMonths;
import com.wks.caseengine.message.vm.AOPMessageVM;

@Service
public class JMDFuelAvailabilityServiceImpl implements JMDFuelAvailabilityService {

    private static final Logger logger = LoggerFactory.getLogger(JMDFuelAvailabilityServiceImpl.class);

    @Autowired
    private JMDFuelAvailabilityRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Override
    @Transactional
    public AOPMessageVM getFuelAvailability(List<UUID> plantIds, String financialYear, String type) {
        logger.info("[JMDFuelAvailability] GET - plantIds: {}, financialYear: {}, type: {}", plantIds, financialYear, type);
        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (plantIds == null || plantIds.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("plantIds cannot be null or empty");
                vm.setData(new ArrayList<>());
                return vm;
            }

            if (financialYear == null || financialYear.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("financialYear cannot be null or empty");
                vm.setData(new ArrayList<>());
                return vm;
            }

            String plantIdsCsv = plantIds.stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(","));

            StoredProcedureQuery sp = entityManager
                    .createStoredProcedureQuery("dbo.CPP_GetFuelAvailabilityTransaction")
                    .registerStoredProcedureParameter("CPPPlantFKIds", String.class, ParameterMode.IN)
                    .registerStoredProcedureParameter("FinancialYear", String.class, ParameterMode.IN)
                    .registerStoredProcedureParameter("Type", String.class, ParameterMode.IN);

            sp.setParameter("CPPPlantFKIds", plantIdsCsv);
            sp.setParameter("FinancialYear", financialYear);
            sp.setParameter("Type", type);

            logger.info("Executing stored procedure dbo.CPP_GetFuelAvailabilityTransaction for plantIds: {}, financialYear: {}, type: {}",
                    plantIdsCsv, financialYear, type);
            sp.execute();

            @SuppressWarnings("unchecked")
            List<Object[]> rawResults = sp.getResultList();
            logger.info("Raw result count: {}", rawResults.size());

            List<FuelAvailabilityTransactionDTO> result = new ArrayList<>();
            for (Object[] row : rawResults) {
                result.add(mapRowToDto(row));
            }

            logger.info("[JMDFuelAvailability] GET - found {} records", result.size());

            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(result);

        } catch (Exception e) {
            logger.error("[JMDFuelAvailability] GET error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
        }

        return vm;
    }

    @Override
    @Transactional
    public List<FuelMasterWithCategoryDTO> getFuels(String type) {
        logger.info("[JMDFuelAvailability] GET FUELS - type: {}", type);
        return repository.getFuelsWithCategory(type).stream()
                .map(this::toFuelDto)
                .toList();
    }

    private FuelMasterWithCategoryDTO toFuelDto(FuelWithCategoryProjection p) {
        FuelMasterWithCategoryDTO dto = new FuelMasterWithCategoryDTO();
        if (p.getId() != null) {
            dto.setId(UUID.fromString(p.getId()));
        }
        dto.setFuelCode(p.getFuelCode());
        dto.setFuelName(p.getFuelName());
        dto.setFuelDisplayName(p.getFuelDisplayName());
        dto.setType(p.getType());
        dto.setUom(p.getUom());
        if (p.getCategoryFkId() != null) {
            dto.setCategoryFkId(UUID.fromString(p.getCategoryFkId()));
        }
        dto.setCategoryName(p.getCategoryName());
        dto.setCategoryDisplayName(p.getCategoryDisplayName());
        return dto;
    }

    @Override
    @Transactional
    public AOPMessageVM saveFuelAvailability(List<UUID> plantIds, String financialYear,
                                             List<FuelAvailabilityTransactionDTO> dtoList) {
        logger.info("[JMDFuelAvailability] SAVE - plantIds: {}, financialYear: {}, records: {}",
                plantIds, financialYear, dtoList.size());
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

            for (FuelAvailabilityTransactionDTO dto : dtoList) {
                try {
                    if (dto.getCppPlantFkId() == null || dto.getFuelId() == null
                            || dto.getType() == null || dto.getFinancialYear() == null) {
                        errorCount++;
                        errorMessages.add("Record skipped: cppPlantFkId/fuelId/type/financialYear is null");
                        continue;
                    }

                    if (dto.getId() == null) {
                        // CREATE via JPA
                        CPPFuelAvailabilityTransaction entity = new CPPFuelAvailabilityTransaction();
                        entity.setCppPlantFkId(dto.getCppPlantFkId());
                        entity.setFuelFkId(dto.getFuelId());
                        entity.setType(dto.getType());
                        entity.setUom(dto.getUom());
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
                        entity.setFinancialYear(dto.getFinancialYear());
                        entity.setRemarks(dto.getRemarks());
                        java.time.LocalDateTime now = java.time.LocalDateTime.now();
                        entity.setCreatedDate(now);
                        entity.setUpdatedDate(now);
                        repository.save(entity);
                        successCount++;
                    } else {
                        // UPDATE
                        repository.updateFuelAvailability(
                                dto.getId(),
                                // dto.getCppPlantFkId(),
                                dto.getFuelId(),
                                // dto.getType(),
                                dto.getUom(),
                                dto.getApr(), dto.getMay(), dto.getJun(), dto.getJul(),
                                dto.getAug(), dto.getSep(), dto.getOct(), dto.getNov(),
                                dto.getDec(), dto.getJan(), dto.getFeb(), dto.getMar(),
                                // dto.getFinancialYear(),
                                dto.getRemarks());
                        successCount++;
                    }

                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = "Error processing record: " + e.getMessage();
                    errorMessages.add(errorMsg);
                    logger.error(errorMsg, e);
                }
            }

            logger.info("[JMDFuelAvailability] SAVE - success: {}, errors: {}", successCount, errorCount);

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
            logger.error("[JMDFuelAvailability] SAVE error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(null);
        }

        return vm;
    }

    @Override
    public byte[] exportFuelAvailability(List<UUID> plantIds, String financialYear, String type) throws IOException {
        logger.info("[JMDFuelAvailability] EXPORT - plantIds: {}, financialYear: {}, type: {}", plantIds, financialYear, type);

        try {
            AOPMessageVM result = getFuelAvailability(plantIds, financialYear, type);

            List<FuelAvailabilityTransactionDTO> dtoList = new ArrayList<>();
            if (result.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<FuelAvailabilityTransactionDTO> data = (List<FuelAvailabilityTransactionDTO>) result.getData();
                dtoList = data;
            }

            if (dtoList == null || dtoList.isEmpty()) {
                logger.warn("[JMDFuelAvailability] EXPORT - no data found");
                dtoList = new ArrayList<>();
            }

            return generateExcel(dtoList, financialYear, null);

        } catch (IOException e) {
            logger.error("[JMDFuelAvailability] EXPORT IOException: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("[JMDFuelAvailability] EXPORT error: {}", e.getMessage(), e);
            throw new IOException("Failed to export Fuel Availability: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public AOPMessageVM importFuelAvailability(List<UUID> plantIds, String financialYear, MultipartFile file) throws IOException {
        logger.info("[JMDFuelAvailability] IMPORT - plantIds: {}, financialYear: {}, file: {}",
                plantIds, financialYear, file.getOriginalFilename());

        AOPMessageVM vm = new AOPMessageVM();

        try (InputStream inputStream = file.getInputStream()) {
            List<FuelAvailabilityTransactionDTO> dtoList = readExcel(inputStream, financialYear);

            if (dtoList == null || dtoList.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("No valid records found in the Excel file");
                return vm;
            }

            List<FuelAvailabilityTransactionDTO> failedRecords = new ArrayList<>();
            List<FuelAvailabilityTransactionDTO> validRecords = new ArrayList<>();

            // Fetch existing data for remark validation
            AOPMessageVM existingData = getFuelAvailability(plantIds, financialYear, null);
            java.util.Map<UUID, String> existingRemarks = new java.util.HashMap<>();
            if (existingData.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<FuelAvailabilityTransactionDTO> existingList = (List<FuelAvailabilityTransactionDTO>) existingData.getData();
                for (FuelAvailabilityTransactionDTO existing : existingList) {
                    if (existing.getId() != null) {
                        existingRemarks.put(existing.getId(),
                                existing.getRemarks() != null ? existing.getRemarks().trim() : "");
                    }
                }
            }

            List<String> errorMessages = new ArrayList<>();
            int skippedCount = 0;

            for (FuelAvailabilityTransactionDTO dto : dtoList) {
                // Import only supports UPDATE - records without id cannot be processed
                if (dto.getId() == null) {
                    failedRecords.add(dto);
                    errorMessages.add("Record skipped: id is null (import only supports update)");
                    continue;
                }

                // Skip unchanged records (dataHash matches)
                if (!isRecordModified(dto, existingRemarks)) {
                    skippedCount++;
                    logger.debug("[JMDFuelAvailability] Skipping unchanged record: {}", dto.getId());
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

            logger.info("[JMDFuelAvailability] {} unchanged (skipped), {} modified to process",
                    skippedCount, dtoList.size() - skippedCount);

            AOPMessageVM saveResult = saveFuelAvailability(plantIds, financialYear, validRecords);

            if (!failedRecords.isEmpty()) {
                byte[] errorExcel = generateExcel(failedRecords, financialYear, errorMessages);
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
            logger.error("[JMDFuelAvailability] IMPORT error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
        }

        return vm;
    }

    @Override
    @Transactional
    public AOPMessageVM deleteFuelAvailability(UUID id) {
        logger.info("[JMDFuelAvailability] DELETE - id: {}", id);
        AOPMessageVM vm = new AOPMessageVM();
        try {
            if (id == null) {
                vm.setCode(400);
                vm.setMessage("id cannot be null");
                return vm;
            }
            if (!repository.existsById(id)) {
                vm.setCode(404);
                vm.setMessage("Record not found for id: " + id);
                return vm;
            }
            repository.deleteById(id);
            vm.setCode(200);
            vm.setMessage("Record deleted successfully");
        } catch (Exception e) {
            logger.error("[JMDFuelAvailability] DELETE error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
        }
        return vm;
    }

    // ── Helper Methods ──────────────────────────────────────────────────────

    private FuelAvailabilityTransactionDTO mapRowToDto(Object[] row) {
        FuelAvailabilityTransactionDTO dto = new FuelAvailabilityTransactionDTO();
        // Order must match the SELECT clause of dbo.CPP_GetFuelAvailabilityTransaction
        // (fat.Id must be the FIRST column returned by the SP)
        dto.setId(row[0] != null ? UUID.fromString(row[0].toString()) : null);
        dto.setCppPlantFkId(row[1] != null ? UUID.fromString(row[1].toString()) : null);
        dto.setCppPlantName(row[2] != null ? row[2].toString() : null);
        dto.setFuelId(row[3] != null ? UUID.fromString(row[3].toString()) : null);
        dto.setFuelName(row[4] != null ? row[4].toString() : null);
        dto.setFuelDisplayName(row[5] != null ? row[5].toString() : null);
        dto.setCategoryId(row[6] != null ? UUID.fromString(row[6].toString()) : null);
        dto.setCategoryName(row[7] != null ? row[7].toString() : null);
        dto.setCategoryDisplayName(row[8] != null ? row[8].toString() : null);
        dto.setType(row[9] != null ? row[9].toString() : null);
        dto.setUom(row[10] != null ? row[10].toString() : null);
        dto.setFinancialYear(row[11] != null ? row[11].toString() : null);
        dto.setApr(toDoubleObj(row[12]));
        dto.setMay(toDoubleObj(row[13]));
        dto.setJun(toDoubleObj(row[14]));
        dto.setJul(toDoubleObj(row[15]));
        dto.setAug(toDoubleObj(row[16]));
        dto.setSep(toDoubleObj(row[17]));
        dto.setOct(toDoubleObj(row[18]));
        dto.setNov(toDoubleObj(row[19]));
        dto.setDec(toDoubleObj(row[20]));
        dto.setJan(toDoubleObj(row[21]));
        dto.setFeb(toDoubleObj(row[22]));
        dto.setMar(toDoubleObj(row[23]));
        dto.setRemarks(row[24] != null ? row[24].toString() : null);
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

    private byte[] generateExcel(List<FuelAvailabilityTransactionDTO> dtoList, String financialYear,
                                 List<String> errorMessages) throws IOException {
        boolean isErrorExcel = errorMessages != null && !errorMessages.isEmpty();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(isErrorExcel ? "Fuel Availability Errors" : "Fuel Availability");

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(workbook);
        CellStyle dataStyle = ExcelStyles.createDataStyle(workbook);
        CellStyle remarksStyle = ExcelStyles.createRemarksStyle(workbook);
        CellStyle errorStyle = isErrorExcel ? ExcelStyles.createErrorStyle(workbook) : null;

        String[] months = FiscalYearMonths.getMonthHeaders(financialYear);

        int rowNum = 0;
        int col = 0;

        Row headerRow = sheet.createRow(rowNum++);
        String[] baseHeaders = {"CPP Plant", "Category", "Fuel", "Type", "UOM"};
        for (String header : baseHeaders) {
            ExcelCells.setString(headerRow.createCell(col++), header, headerStyle);
        }

        int monthStartCol = col;
        for (String month : months) {
            ExcelCells.setString(headerRow.createCell(col++), month, headerStyle);
        }

        int remarksCol = col;
        ExcelCells.setString(headerRow.createCell(col++), "Remarks", headerStyle);

        int idCol = col;
        ExcelCells.setString(headerRow.createCell(col++), "id", headerStyle);

        int cppPlantFkIdCol = col;
        ExcelCells.setString(headerRow.createCell(col++), "cppPlantFkId", headerStyle);

        int fuelIdCol = col;
        ExcelCells.setString(headerRow.createCell(col++), "fuelId", headerStyle);

        int dataHashCol = col;
        ExcelCells.setString(headerRow.createCell(col++), "dataHash", headerStyle);

        int commentCol = -1;
        if (isErrorExcel) {
            ExcelCells.setString(headerRow.createCell(col++), "Status", headerStyle);

            commentCol = col;
            ExcelCells.setString(headerRow.createCell(col++), "Error Message", headerStyle);
        }

        int totalColumns = col;

        for (int i = 0; i < dtoList.size(); i++) {
            FuelAvailabilityTransactionDTO dto = dtoList.get(i);
            Row row = sheet.createRow(rowNum++);
            col = 0;

            ExcelCells.setString(row.createCell(col++), dto.getCppPlantName(), dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getCategoryDisplayName(), dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getFuelDisplayName(), dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getType(), dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getUom(), dataStyle);

            ExcelCells.setDouble(row.createCell(monthStartCol + 0), dto.getApr(), dataStyle);
            ExcelCells.setDouble(row.createCell(monthStartCol + 1), dto.getMay(), dataStyle);
            ExcelCells.setDouble(row.createCell(monthStartCol + 2), dto.getJun(), dataStyle);
            ExcelCells.setDouble(row.createCell(monthStartCol + 3), dto.getJul(), dataStyle);
            ExcelCells.setDouble(row.createCell(monthStartCol + 4), dto.getAug(), dataStyle);
            ExcelCells.setDouble(row.createCell(monthStartCol + 5), dto.getSep(), dataStyle);
            ExcelCells.setDouble(row.createCell(monthStartCol + 6), dto.getOct(), dataStyle);
            ExcelCells.setDouble(row.createCell(monthStartCol + 7), dto.getNov(), dataStyle);
            ExcelCells.setDouble(row.createCell(monthStartCol + 8), dto.getDec(), dataStyle);
            ExcelCells.setDouble(row.createCell(monthStartCol + 9), dto.getJan(), dataStyle);
            ExcelCells.setDouble(row.createCell(monthStartCol + 10), dto.getFeb(), dataStyle);
            ExcelCells.setDouble(row.createCell(monthStartCol + 11), dto.getMar(), dataStyle);
            col = monthStartCol + 12;

            ExcelCells.setString(row.createCell(col++), dto.getRemarks(), remarksStyle);
            ExcelCells.setString(row.createCell(col++), dto.getId() != null ? dto.getId().toString() : null, dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getCppPlantFkId() != null ? dto.getCppPlantFkId().toString() : null, dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getFuelId() != null ? dto.getFuelId().toString() : null, dataStyle);
            ExcelCells.setString(row.createCell(col++), generateHash(dto), dataStyle);

            if (isErrorExcel) {
                ExcelCells.setString(row.createCell(col++), "Failed", errorStyle);
                ExcelCells.setString(row.createCell(col++), errorMessages.get(i), errorStyle);
            }
        }

        ExcelColumns.hideColumns(sheet, idCol, cppPlantFkIdCol, fuelIdCol, dataHashCol);
        ExcelColumns.autoSize(sheet, totalColumns, remarksCol);
        if (commentCol >= 0) {
            sheet.setColumnWidth(commentCol, ExcelColumns.DEFAULT_REMARKS_WIDTH);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    private List<FuelAvailabilityTransactionDTO> readExcel(InputStream inputStream, String financialYear) throws IOException {
        List<FuelAvailabilityTransactionDTO> dtoList = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);

        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            workbook.close();
            return dtoList;
        }

        int numCols = headerRow.getLastCellNum();

        int idColIdx = -1;
        int cppPlantFkIdColIdx = -1;
        int fuelIdColIdx = -1;
        int remarksColIdx = -1;
        int dataHashColIdx = -1;
        // Base headers: 0=CPP Plant, 1=Category, 2=Fuel, 3=Type, 4=UOM
        int monthStartIdx = 5;

        for (int i = 0; i < numCols; i++) {
            String header = ExcelCells.toString(headerRow.getCell(i));
            if (header == null) continue;
            String headerLower = header.toLowerCase();
            if (headerLower.equals("id")) {
                idColIdx = i;
            } else if (headerLower.equals("cppplantfkid")) {
                cppPlantFkIdColIdx = i;
            } else if (headerLower.equals("fuelid")) {
                fuelIdColIdx = i;
            } else if (headerLower.equals("remarks")) {
                remarksColIdx = i;
            } else if (headerLower.equals("datahash")) {
                dataHashColIdx = i;
            }
        }

        for (Row row : ExcelRows.getDataRows(sheet, 1)) {
            if (row.getLastCellNum() < monthStartIdx) continue;

            FuelAvailabilityTransactionDTO dto = new FuelAvailabilityTransactionDTO();
            dto.setFinancialYear(financialYear);

            // Parse text columns
            dto.setCppPlantName(ExcelCells.toStringValue(row.getCell(0)));
            dto.setCategoryDisplayName(ExcelCells.toStringValue(row.getCell(1)));
            dto.setFuelDisplayName(ExcelCells.toStringValue(row.getCell(2)));
            dto.setType(ExcelCells.toStringValue(row.getCell(3)));
            dto.setUom(ExcelCells.toStringValue(row.getCell(4)));

            if (idColIdx >= 0) {
                dto.setId(ExcelCells.toUUID(row.getCell(idColIdx)));
            }

            if (cppPlantFkIdColIdx >= 0) {
                dto.setCppPlantFkId(ExcelCells.toUUID(row.getCell(cppPlantFkIdColIdx)));
            }

            if (fuelIdColIdx >= 0) {
                dto.setFuelId(ExcelCells.toUUID(row.getCell(fuelIdColIdx)));
            }

            if (remarksColIdx >= 0) {
                dto.setRemarks(ExcelCells.toStringValue(row.getCell(remarksColIdx)));
            }

            if (dataHashColIdx >= 0) {
                dto.setDataHash(ExcelCells.toStringValue(row.getCell(dataHashColIdx)));
            }

            if (numCols > monthStartIdx + 0) dto.setApr(ExcelCells.toDouble(row.getCell(monthStartIdx + 0)));
            if (numCols > monthStartIdx + 1) dto.setMay(ExcelCells.toDouble(row.getCell(monthStartIdx + 1)));
            if (numCols > monthStartIdx + 2) dto.setJun(ExcelCells.toDouble(row.getCell(monthStartIdx + 2)));
            if (numCols > monthStartIdx + 3) dto.setJul(ExcelCells.toDouble(row.getCell(monthStartIdx + 3)));
            if (numCols > monthStartIdx + 4) dto.setAug(ExcelCells.toDouble(row.getCell(monthStartIdx + 4)));
            if (numCols > monthStartIdx + 5) dto.setSep(ExcelCells.toDouble(row.getCell(monthStartIdx + 5)));
            if (numCols > monthStartIdx + 6) dto.setOct(ExcelCells.toDouble(row.getCell(monthStartIdx + 6)));
            if (numCols > monthStartIdx + 7) dto.setNov(ExcelCells.toDouble(row.getCell(monthStartIdx + 7)));
            if (numCols > monthStartIdx + 8) dto.setDec(ExcelCells.toDouble(row.getCell(monthStartIdx + 8)));
            if (numCols > monthStartIdx + 9) dto.setJan(ExcelCells.toDouble(row.getCell(monthStartIdx + 9)));
            if (numCols > monthStartIdx + 10) dto.setFeb(ExcelCells.toDouble(row.getCell(monthStartIdx + 10)));
            if (numCols > monthStartIdx + 11) dto.setMar(ExcelCells.toDouble(row.getCell(monthStartIdx + 11)));

            dtoList.add(dto);
        }

        workbook.close();
        return dtoList;
    }

    private String generateHash(FuelAvailabilityTransactionDTO dto) {
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

    private boolean isRecordModified(FuelAvailabilityTransactionDTO dto, java.util.Map<UUID, String> existingRemarks) {
        if (dto.getId() == null) {
            return true;
        }

        String importedHash = dto.getDataHash();
        if (importedHash == null || importedHash.isEmpty()) {
            return true;
        }

        String currentHash = generateHash(dto);
        boolean modified = !importedHash.equals(currentHash);

        if (!modified) {
            logger.debug("[JMDFuelAvailability] Record {} unchanged - hash match", dto.getId());
        }

        return modified;
    }
}
