package com.wks.caseengine.cpp.serviceimpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.dto.norm.CPPNormsRequestDTO;
import com.wks.caseengine.cpp.dto.norm.CPPNormsResponseDTO;
import com.wks.caseengine.cpp.entity.CPPNorms;
import com.wks.caseengine.cpp.repository.CPPNormsRepository;
import com.wks.caseengine.cpp.service.JMDCPPNormsService;
import com.wks.caseengine.message.vm.AOPMessageVM;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JMDCPPNormsServiceImpl implements JMDCPPNormsService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private CPPNormsServiceImpl cppNormsServiceImpl;

    @Autowired
    private CPPNormsRepository cppNormsRepository;

    @Override
    public AOPMessageVM getCPPNormsForPlants(List<UUID> plantIds, String financialYear, String fromDate, String toDate) {
        log.info("=== Starting getCPPNormsForPlants (JMD) ===");
        log.info("PlantIds: {}, FinancialYear: {}, FromDate: {}, ToDate: {}", plantIds, financialYear, fromDate, toDate);

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
                vm.setMessage("FinancialYear cannot be null or empty");
                vm.setData(new ArrayList<>());
                return vm;
            }

            List<CPPNormsResponseDTO> allResults = new ArrayList<>();

            for (UUID plantId : plantIds) {
                log.info("Fetching CPP norms for plantId: {}", plantId);

                try {
                    StoredProcedureQuery sp = entityManager
                            .createStoredProcedureQuery("dbo.CPP_JMD_GetCPPNorms")
                            .registerStoredProcedureParameter(1, String.class, ParameterMode.IN)
                            .registerStoredProcedureParameter(2, String.class, ParameterMode.IN);

                    sp.setParameter(1, plantId.toString());
                    sp.setParameter(2, financialYear);

                    log.info("Executing stored procedure dbo.CPP_JMD_GetCPPNorms for plantId: {}", plantId);
                    sp.execute();

                    @SuppressWarnings("unchecked")
                    List<Object[]> rawResults = sp.getResultList();
                    log.info("PlantId: {}, raw result count: {}", plantId, rawResults.size());

                    for (Object[] row : rawResults) {
                        CPPNormsResponseDTO dto = mapRowToDto(row);
                        allResults.add(dto);
                    }
                } catch (Exception e) {
                    log.error("Error fetching CPP norms for plantId: {}", plantId, e);
                }
            }

            log.info("Total aggregated CPP norms records from {} plants: {}", plantIds.size(), allResults.size());

            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(allResults);

        } catch (Exception e) {
            log.error("=== ERROR in getCPPNormsForPlants ===", e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
        }

        return vm;
    }

    @Override
    @Transactional
    public AOPMessageVM saveOrUpdateCPPNorms(List<CPPNormsRequestDTO> dtoList, String financialYear, String modifiedBy) {
        log.info("=== Starting saveOrUpdateCPPNorms ===");
        log.info("Total records to process: {}", dtoList.size());
        log.info("Financial Year: {}", financialYear);

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

            for (CPPNormsRequestDTO dto : dtoList) {
                try {
                    log.info("Processing record for NormsHeaderFkId: {}", dto.getNormsHeaderFkId());

                    StoredProcedureQuery sp = entityManager
                            .createStoredProcedureQuery("dbo.CPP_UpdateCPPNorms")
                            .registerStoredProcedureParameter("Id", UUID.class, ParameterMode.IN)
                            .registerStoredProcedureParameter("NormsHeaderFkId", UUID.class, ParameterMode.IN)
                            .registerStoredProcedureParameter("FinancialYear", String.class, ParameterMode.IN)
                            .registerStoredProcedureParameter("AOPYear", String.class, ParameterMode.IN)
                            .registerStoredProcedureParameter("NormTypeFkId", Integer.class, ParameterMode.IN)
                            .registerStoredProcedureParameter("Apr_Norms", BigDecimal.class, ParameterMode.IN)
                            .registerStoredProcedureParameter("May_Norms", BigDecimal.class, ParameterMode.IN)
                            .registerStoredProcedureParameter("Jun_Norms", BigDecimal.class, ParameterMode.IN)
                            .registerStoredProcedureParameter("Jul_Norms", BigDecimal.class, ParameterMode.IN)
                            .registerStoredProcedureParameter("Aug_Norms", BigDecimal.class, ParameterMode.IN)
                            .registerStoredProcedureParameter("Sep_Norms", BigDecimal.class, ParameterMode.IN)
                            .registerStoredProcedureParameter("Oct_Norms", BigDecimal.class, ParameterMode.IN)
                            .registerStoredProcedureParameter("Nov_Norms", BigDecimal.class, ParameterMode.IN)
                            .registerStoredProcedureParameter("Dec_Norms", BigDecimal.class, ParameterMode.IN)
                            .registerStoredProcedureParameter("Jan_Norms", BigDecimal.class, ParameterMode.IN)
                            .registerStoredProcedureParameter("Feb_Norms", BigDecimal.class, ParameterMode.IN)
                            .registerStoredProcedureParameter("Mar_Norms", BigDecimal.class, ParameterMode.IN)
                            .registerStoredProcedureParameter("Remarks", String.class, ParameterMode.IN)
                            .registerStoredProcedureParameter("ApplyActualNormToAll", Boolean.class, ParameterMode.IN)
                            .registerStoredProcedureParameter("ModifiedBy", String.class, ParameterMode.IN);

                    sp.setParameter("Id", dto.getCppNormsId());
                    sp.setParameter("NormsHeaderFkId", dto.getNormsHeaderFkId());
                    sp.setParameter("FinancialYear", financialYear);
                    sp.setParameter("AOPYear", dto.getAopYear());
                    sp.setParameter("NormTypeFkId", dto.getNormTypeFkId());
                    sp.setParameter("Apr_Norms", dto.getAprNorms());
                    sp.setParameter("May_Norms", dto.getMayNorms());
                    sp.setParameter("Jun_Norms", dto.getJunNorms());
                    sp.setParameter("Jul_Norms", dto.getJulNorms());
                    sp.setParameter("Aug_Norms", dto.getAugNorms());
                    sp.setParameter("Sep_Norms", dto.getSepNorms());
                    sp.setParameter("Oct_Norms", dto.getOctNorms());
                    sp.setParameter("Nov_Norms", dto.getNovNorms());
                    sp.setParameter("Dec_Norms", dto.getDecNorms());
                    sp.setParameter("Jan_Norms", dto.getJanNorms());
                    sp.setParameter("Feb_Norms", dto.getFebNorms());
                    sp.setParameter("Mar_Norms", dto.getMarNorms());
                    sp.setParameter("Remarks", dto.getRemarks());
                    sp.setParameter("ApplyActualNormToAll", dto.getApplyActualNormToAll() != null ? dto.getApplyActualNormToAll() : false);
                    sp.setParameter("ModifiedBy", modifiedBy);

                    sp.execute();
                    successCount++;
                    
                    log.info("Successfully processed record for NormsHeaderFkId: {}", dto.getNormsHeaderFkId());

                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = "Error processing NormsHeaderFkId " + dto.getNormsHeaderFkId() + ": " + e.getMessage();
                    errorMessages.add(errorMsg);
                    log.error(errorMsg, e);
                }
            }

            log.info("Processing complete. Success: {}, Errors: {}", successCount, errorCount);

            if (errorCount > 0) {
                vm.setCode(207); // Multi-Status
                vm.setMessage(String.format("Processed %d records. Success: %d, Errors: %d", 
                    dtoList.size(), successCount, errorCount));
                vm.setData(errorMessages);
            } else {
                vm.setCode(200);
                vm.setMessage(String.format("Successfully processed all %d records", successCount));
                vm.setData(null);
            }

        } catch (Exception e) {
            log.error("=== ERROR in saveOrUpdateCPPNorms ===", e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(null);
        }

        return vm;
    }

  

    @Override
    public byte[] exportCPPNorms(List<UUID> plantIds, String financialYear, String startDate, String endDate) throws IOException {
        log.info("=== Starting exportCPPNorms (JMD) ===");
        log.info("PlantIds: {}, FinancialYear: {}, StartDate: {}, EndDate: {}", plantIds, financialYear, startDate, endDate);

        try {
            AOPMessageVM result = getCPPNormsForPlants(plantIds, financialYear, startDate, endDate);

            List<CPPNormsResponseDTO> dtoList = new ArrayList<>();
            if (result.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<CPPNormsResponseDTO> data = (List<CPPNormsResponseDTO>) result.getData();
                dtoList = data;
            }

            if (dtoList == null || dtoList.isEmpty()) {
                log.warn("No data found for export");
                dtoList = new ArrayList<>();
            }

            log.info("Exporting {} CPP norms records", dtoList.size());

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("CPP Norms");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle numericStyle = createNumericStyle(workbook);

            int rowNum = 0;
            int col = 0;

            Row headerRow = sheet.createRow(rowNum++);

            headerRow.createCell(col).setCellValue("CPP Plant");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("Generating Plant");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("Utility");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("Utility ID");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("UOM");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("Account");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("Material");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("SAP Code");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("Issuing Plant");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("Issuing UOM");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("AOP Year");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("Norm Type");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            String startYearSuffix = financialYear.substring(2, 4);
            String endYearSuffix = financialYear.substring(5, 7);
            String[] months = {"Apr-" + startYearSuffix, "May-" + startYearSuffix, "Jun-" + startYearSuffix, "Jul-" + startYearSuffix,
                    "Aug-" + startYearSuffix, "Sep-" + startYearSuffix, "Oct-" + startYearSuffix, "Nov-" + startYearSuffix,
                    "Dec-" + startYearSuffix, "Jan-" + endYearSuffix, "Feb-" + endYearSuffix, "Mar-" + endYearSuffix};

            int monthStartCol = col;
            for (String month : months) {
                headerRow.createCell(col).setCellValue(month);
                headerRow.getCell(col++).setCellStyle(headerStyle);
            }

            int remarksCol = col;
            headerRow.createCell(col).setCellValue("Remarks");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int applyActualNormToAllCol = col;
            headerRow.createCell(col).setCellValue("applyActualNormToAll");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int idCol = col;
            headerRow.createCell(col).setCellValue("id");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int cppNormsIdCol = col;
            headerRow.createCell(col).setCellValue("cppNormsId");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int normsHeaderFkIdCol = col;
            headerRow.createCell(col).setCellValue("normsHeaderFkId");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int normTypeFkIdCol = col;
            headerRow.createCell(col).setCellValue("normTypeFkId");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int dataHashCol = col;
            headerRow.createCell(col).setCellValue("dataHash");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int totalColumns = col;

            for (CPPNormsResponseDTO dto : dtoList) {
                Row row = sheet.createRow(rowNum++);
                col = 0;

                setStringCellValue(row.createCell(col++), dto.getCppPlantName(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getGeneratingPlantName(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getUtilityName(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getUtilityId(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getUom(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getAccountName(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getMaterialName(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getMaterialId(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getIssuingPlantName(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getIssuingUom(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getAopYear(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getNormTypeName(), dataStyle);

                setBigDecimalCellValue(row.createCell(monthStartCol + 0), dto.getAprNorms(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 1), dto.getMayNorms(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 2), dto.getJunNorms(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 3), dto.getJulNorms(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 4), dto.getAugNorms(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 5), dto.getSepNorms(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 6), dto.getOctNorms(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 7), dto.getNovNorms(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 8), dto.getDecNorms(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 9), dto.getJanNorms(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 10), dto.getFebNorms(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 11), dto.getMarNorms(), numericStyle);
                col = monthStartCol + 12;

                setStringCellValue(row.createCell(col++), dto.getRemarks(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getApplyActualNormToAll() != null ? dto.getApplyActualNormToAll().toString() : "false", dataStyle);
                setStringCellValue(row.createCell(col++), dto.getId() != null ? dto.getId().toString() : null, dataStyle);
                setStringCellValue(row.createCell(col++), dto.getCppNormsId() != null ? dto.getCppNormsId().toString() : null, dataStyle);
                setStringCellValue(row.createCell(col++), dto.getNormsHeaderFkId() != null ? dto.getNormsHeaderFkId().toString() : null, dataStyle);
                setStringCellValue(row.createCell(col++), dto.getNormTypeFkId() != null ? dto.getNormTypeFkId().toString() : null, dataStyle);

                String dataHash = generateNormsHash(dto);
                setStringCellValue(row.createCell(col++), dataHash, dataStyle);
            }

            sheet.setColumnHidden(applyActualNormToAllCol, true);
            sheet.setColumnHidden(idCol, true);
            sheet.setColumnHidden(cppNormsIdCol, true);
            sheet.setColumnHidden(normsHeaderFkIdCol, true);
            sheet.setColumnHidden(normTypeFkIdCol, true);
            sheet.setColumnHidden(dataHashCol, true);

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

            byte[] excelBytes = outputStream.toByteArray();
            log.info("Excel file generated successfully, size: {} bytes", excelBytes.length);
            return excelBytes;

        } catch (IOException e) {
            log.error("IOException while exporting CPP norms", e);
            throw e;
        } catch (Exception e) {
            log.error("Error exporting CPP norms", e);
            throw new IOException("Failed to export CPP norms: " + e.getMessage(), e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createNumericStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.0000"));
        return style;
    }

    private void setStringCellValue(Cell cell, String value, CellStyle style) {
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
    }

    private void setBigDecimalCellValue(Cell cell, BigDecimal value, CellStyle style) {
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }

    @Override
    @Transactional
    public AOPMessageVM importCPPNorms(List<UUID> plantIds, String financialYear, MultipartFile file, String modifiedBy) throws IOException {
        log.info("=== Starting importCPPNorms (JMD) ===");
        log.info("PlantIds: {}, FinancialYear: {}, FileName: {}, FileSize: {}",
                plantIds, financialYear, file.getOriginalFilename(), file.getSize());

        AOPMessageVM response = new AOPMessageVM();
        try {
            List<CPPNormsResponseDTO> excelData = readCPPNormsFromExcel(file.getInputStream());
            log.info("[Import CPP Norms] Read {} records from Excel", excelData.size());

            List<CPPNormsRequestDTO> validRecords = new ArrayList<>();
            List<CPPNormsResponseDTO> failedRecords = new ArrayList<>();
            int skippedCount = 0;

            for (CPPNormsResponseDTO dto : excelData) {
                // FIRST: Check if record was actually modified by user
                if (!isRecordModified(dto, financialYear)) {
                    skippedCount++;
                    log.debug("[Import CPP Norms] Skipping unchanged record: normsHeaderFkId={}", dto.getNormsHeaderFkId());
                    continue;
                }

                // SECOND: Validate modified records
                String validationError = validateNormsData(dto, financialYear);
                if (validationError != null) {
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription(validationError);
                    failedRecords.add(dto);
                    log.warn("[Import CPP Norms] Invalid record - normsHeaderFkId={}: {}", dto.getNormsHeaderFkId(), validationError);
                } else {
                    CPPNormsRequestDTO request = new CPPNormsRequestDTO();
                    request.setCppNormsId(dto.getCppNormsId());
                    request.setNormsHeaderFkId(dto.getNormsHeaderFkId());
                    request.setAopYear(dto.getAopYear());
                    request.setNormTypeFkId(dto.getNormTypeFkId());
                    request.setAprNorms(dto.getAprNorms());
                    request.setMayNorms(dto.getMayNorms());
                    request.setJunNorms(dto.getJunNorms());
                    request.setJulNorms(dto.getJulNorms());
                    request.setAugNorms(dto.getAugNorms());
                    request.setSepNorms(dto.getSepNorms());
                    request.setOctNorms(dto.getOctNorms());
                    request.setNovNorms(dto.getNovNorms());
                    request.setDecNorms(dto.getDecNorms());
                    request.setJanNorms(dto.getJanNorms());
                    request.setFebNorms(dto.getFebNorms());
                    request.setMarNorms(dto.getMarNorms());
                    request.setRemarks(dto.getRemarks());
                    request.setApplyActualNormToAll(dto.getApplyActualNormToAll());
                    validRecords.add(request);
                }
            }

            log.info("[Import CPP Norms] {} records unchanged (skipped), {} modified records to process", skippedCount, excelData.size() - skippedCount);

            // Try to save valid records and track any that fail during save
            if (!validRecords.isEmpty()) {
                try {
                    AOPMessageVM saveResult = saveOrUpdateCPPNorms(validRecords, financialYear, modifiedBy);

                    if (saveResult.getCode() == 207) {
                        List<String> saveErrors = new ArrayList<>();
                        if (saveResult.getData() instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> errs = (List<String>) saveResult.getData();
                            saveErrors = errs;
                        }
                        for (String msg : saveErrors) {
                            UUID normsHeaderFkId = extractNormsHeaderFkId(msg);
                            if (normsHeaderFkId != null) {
                                for (CPPNormsResponseDTO dto : excelData) {
                                    if (dto.getNormsHeaderFkId() != null && dto.getNormsHeaderFkId().equals(normsHeaderFkId)) {
                                        dto.setSaveStatus("Failed");
                                        dto.setErrDescription(msg);
                                        if (!failedRecords.contains(dto)) {
                                            failedRecords.add(dto);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                        log.warn("[Import CPP Norms] {} records failed during save", saveErrors.size());
                    }

                    log.info("[Import CPP Norms] Successfully processed {} records", validRecords.size());
                } catch (Exception e) {
                    log.error("[Import CPP Norms] Error saving records: {}", e.getMessage(), e);
                    for (CPPNormsRequestDTO failedReq : validRecords) {
                        for (CPPNormsResponseDTO dto : excelData) {
                            if (dto.getNormsHeaderFkId() != null && dto.getNormsHeaderFkId().equals(failedReq.getNormsHeaderFkId())) {
                                dto.setSaveStatus("Failed");
                                dto.setErrDescription("Save failed: " + e.getMessage());
                                if (!failedRecords.contains(dto)) {
                                    failedRecords.add(dto);
                                }
                                break;
                            }
                        }
                    }
                }
            }

            // Prepare response
            if (failedRecords.isEmpty()) {
                response.setCode(200);
                if (validRecords.isEmpty() && skippedCount > 0) {
                    response.setMessage("No changes detected in imported records. All " + skippedCount + " records unchanged.");
                } else {
                    response.setMessage("All CPP norms imported successfully. " + skippedCount + " records unchanged, " + validRecords.size() + " records updated.");
                }
            } else {
                byte[] failedRecordsFile = generateErrorExcel(failedRecords, financialYear);
                String base64File = Base64.getEncoder().encodeToString(failedRecordsFile);
                response.setCode(400);
                response.setMessage("Partial import: " + validRecords.size() + " saved, " + failedRecords.size() + " failed, " + skippedCount + " unchanged. Download file for details.");
                response.setData(base64File);
                log.info("[Import CPP Norms] Exported {} failed records to Excel", failedRecords.size());
            }

            log.info("[Import CPP Norms] Import completed - Unchanged: {}, Saved: {}, Failed: {}", skippedCount, validRecords.size(), failedRecords.size());
        } catch (IOException e) {
            log.error("IOException while importing CPP norms (JMD)", e);
            throw e;
        } catch (Exception e) {
            log.error("Error importing CPP norms (JMD)", e);
            throw new IOException("Failed to import CPP norms: " + e.getMessage(), e);
        }
        return response;
    }

    private List<CPPNormsResponseDTO> readCPPNormsFromExcel(InputStream inputStream) {
        List<CPPNormsResponseDTO> dataList = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (rowIterator.hasNext()) {
                rowIterator.next();
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                boolean isRowEmpty = true;
                for (int i = 0; i < row.getLastCellNum(); i++) {
                    Cell cell = row.getCell(i);
                    if (cell != null && cell.toString() != null && !cell.toString().trim().isEmpty()) {
                        isRowEmpty = false;
                        break;
                    }
                }
                if (isRowEmpty) {
                    continue;
                }

                CPPNormsResponseDTO dto = new CPPNormsResponseDTO();

                try {
                    int col = 0;
                    dto.setCppPlantName(getStringCellValue(row.getCell(col++)));
                    dto.setGeneratingPlantName(getStringCellValue(row.getCell(col++)));
                    dto.setUtilityName(getStringCellValue(row.getCell(col++)));
                    dto.setUtilityId(getStringCellValue(row.getCell(col++)));
                    dto.setUom(getStringCellValue(row.getCell(col++)));
                    dto.setAccountName(getStringCellValue(row.getCell(col++)));
                    dto.setMaterialName(getStringCellValue(row.getCell(col++)));
                    dto.setMaterialId(getStringCellValue(row.getCell(col++)));
                    dto.setIssuingPlantName(getStringCellValue(row.getCell(col++)));
                    dto.setIssuingUom(getStringCellValue(row.getCell(col++)));
                    dto.setAopYear(getStringCellValue(row.getCell(col++)));
                    dto.setNormTypeName(getStringCellValue(row.getCell(col++)));

                    dto.setAprNorms(getBigDecimalCellValue(row.getCell(col++)));
                    dto.setMayNorms(getBigDecimalCellValue(row.getCell(col++)));
                    dto.setJunNorms(getBigDecimalCellValue(row.getCell(col++)));
                    dto.setJulNorms(getBigDecimalCellValue(row.getCell(col++)));
                    dto.setAugNorms(getBigDecimalCellValue(row.getCell(col++)));
                    dto.setSepNorms(getBigDecimalCellValue(row.getCell(col++)));
                    dto.setOctNorms(getBigDecimalCellValue(row.getCell(col++)));
                    dto.setNovNorms(getBigDecimalCellValue(row.getCell(col++)));
                    dto.setDecNorms(getBigDecimalCellValue(row.getCell(col++)));
                    dto.setJanNorms(getBigDecimalCellValue(row.getCell(col++)));
                    dto.setFebNorms(getBigDecimalCellValue(row.getCell(col++)));
                    dto.setMarNorms(getBigDecimalCellValue(row.getCell(col++)));

                    dto.setRemarks(getStringCellValue(row.getCell(col++)));

                    String applyActualNormToAllStr = getStringCellValue(row.getCell(col++));
                    if (applyActualNormToAllStr != null && !applyActualNormToAllStr.isEmpty()) {
                        dto.setApplyActualNormToAll(Boolean.parseBoolean(applyActualNormToAllStr));
                    }

                    String idStr = getStringCellValue(row.getCell(col++));
                    if (idStr != null && !idStr.isEmpty()) {
                        try {
                            dto.setId(Long.parseLong(idStr));
                        } catch (NumberFormatException e) {
                            log.warn("Could not parse id '{}' as Long", idStr);
                        }
                    }

                    String cppNormsIdStr = getStringCellValue(row.getCell(col++));
                    if (cppNormsIdStr != null && !cppNormsIdStr.isEmpty()) {
                        dto.setCppNormsId(UUID.fromString(cppNormsIdStr));
                    }

                    String normsHeaderFkIdStr = getStringCellValue(row.getCell(col++));
                    if (normsHeaderFkIdStr != null && !normsHeaderFkIdStr.isEmpty()) {
                        dto.setNormsHeaderFkId(UUID.fromString(normsHeaderFkIdStr));
                    }

                    String normTypeFkIdStr = getStringCellValue(row.getCell(col++));
                    if (normTypeFkIdStr != null && !normTypeFkIdStr.isEmpty()) {
                        dto.setNormTypeFkId(Integer.parseInt(normTypeFkIdStr));
                    }

                    // Read dataHash column (stored during export for audit/change detection)
                    String storedHash = getStringCellValue(row.getCell(col++));
                    // Hash is used by isRecordModified() which compares DB values with imported values

                    if (dto.getNormsHeaderFkId() == null) {
                        dto.setSaveStatus("Failed");
                        dto.setErrDescription("NormsHeaderFkId is missing");
                    }

                } catch (Exception e) {
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription(e.getMessage());
                }

                dataList.add(dto);
            }
        } catch (Exception e) {
            log.error("Error reading CPP norms file", e);
        }

        return dataList;
    }

    /**
     * Generate SHA-256 hash from norms data (Apr-Mar) and remarks.
     * Used to detect if data has changed during import.
     */
    private String normalizeBigDecimal(BigDecimal value) {
        if (value == null) {
            return "null";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String generateNormsHash(CPPNormsResponseDTO dto) {
        try {
            StringBuilder dataToHash = new StringBuilder();

            dataToHash.append(normalizeBigDecimal(dto.getAprNorms())).append("|");
            dataToHash.append(normalizeBigDecimal(dto.getMayNorms())).append("|");
            dataToHash.append(normalizeBigDecimal(dto.getJunNorms())).append("|");
            dataToHash.append(normalizeBigDecimal(dto.getJulNorms())).append("|");
            dataToHash.append(normalizeBigDecimal(dto.getAugNorms())).append("|");
            dataToHash.append(normalizeBigDecimal(dto.getSepNorms())).append("|");
            dataToHash.append(normalizeBigDecimal(dto.getOctNorms())).append("|");
            dataToHash.append(normalizeBigDecimal(dto.getNovNorms())).append("|");
            dataToHash.append(normalizeBigDecimal(dto.getDecNorms())).append("|");
            dataToHash.append(normalizeBigDecimal(dto.getJanNorms())).append("|");
            dataToHash.append(normalizeBigDecimal(dto.getFebNorms())).append("|");
            dataToHash.append(normalizeBigDecimal(dto.getMarNorms())).append("|");

            dataToHash.append(dto.getRemarks() != null ? dto.getRemarks().trim() : "null");

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
            log.error("[Hash Generation] Error generating hash: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * Check if the imported record has been modified compared to DB.
     * Returns true if norms data or remarks have changed.
     */
    private boolean isRecordModified(CPPNormsResponseDTO dto, String financialYear) {
        try {
            if (dto.getNormsHeaderFkId() == null) {
                return true;
            }

            Optional<CPPNorms> dbEntityOpt = cppNormsRepository.findByNormsHeaderFkIdAndFinancialYear(dto.getNormsHeaderFkId(), financialYear);
            if (dbEntityOpt.isEmpty()) {
                return true;
            }

            CPPNorms dbEntity = dbEntityOpt.get();

            CPPNormsResponseDTO dbDto = new CPPNormsResponseDTO();
            dbDto.setAprNorms(dbEntity.getAprNorms());
            dbDto.setMayNorms(dbEntity.getMayNorms());
            dbDto.setJunNorms(dbEntity.getJunNorms());
            dbDto.setJulNorms(dbEntity.getJulNorms());
            dbDto.setAugNorms(dbEntity.getAugNorms());
            dbDto.setSepNorms(dbEntity.getSepNorms());
            dbDto.setOctNorms(dbEntity.getOctNorms());
            dbDto.setNovNorms(dbEntity.getNovNorms());
            dbDto.setDecNorms(dbEntity.getDecNorms());
            dbDto.setJanNorms(dbEntity.getJanNorms());
            dbDto.setFebNorms(dbEntity.getFebNorms());
            dbDto.setMarNorms(dbEntity.getMarNorms());
            dbDto.setRemarks(dbEntity.getRemarks());

            String dbHash = generateNormsHash(dbDto);
            String importedHash = generateNormsHash(dto);

            boolean modified = !dbHash.equals(importedHash);
            if (!modified) {
                log.debug("[isRecordModified] Record unchanged - hash match, normsHeaderFkId={}", dto.getNormsHeaderFkId());
            } else {
                log.debug("[isRecordModified] Record modified - hash mismatch, normsHeaderFkId={}", dto.getNormsHeaderFkId());
            }
            return modified;
        } catch (Exception e) {
            log.error("[isRecordModified] Error checking if record modified for normsHeaderFkId={}: {}", dto.getNormsHeaderFkId(), e.getMessage());
            return true;
        }
    }

    /**
     * Validate imported norms data.
     * Checks that NormsHeaderFkId is present, remarks are not empty,
     * and remarks have been updated from DB value (mandatory for audit).
     */
    private String validateNormsData(CPPNormsResponseDTO dto, String financialYear) {
        if (dto.getNormsHeaderFkId() == null) {
            return "NormsHeaderFkId is missing";
        }

        if (dto.getRemarks() == null || dto.getRemarks().trim().isEmpty()) {
            return "Remarks field is mandatory and cannot be empty";
        }

        try {
            Optional<CPPNorms> dbEntityOpt = cppNormsRepository.findByNormsHeaderFkIdAndFinancialYear(dto.getNormsHeaderFkId(), financialYear);
            if (dbEntityOpt.isPresent()) {
                String dbRemarks = dbEntityOpt.get().getRemarks() != null ? dbEntityOpt.get().getRemarks().trim() : "";
                String importedRemarks = dto.getRemarks() != null ? dto.getRemarks().trim() : "";

                if (dbRemarks.equals(importedRemarks)) {
                    return "Remarks must be updated to explain the changes. Current remarks are identical to the database value.";
                }
            }
        } catch (Exception e) {
            log.error("[Validation] Error checking remarks for normsHeaderFkId={}: {}", dto.getNormsHeaderFkId(), e.getMessage());
        }

        return null;
    }

    private UUID extractNormsHeaderFkId(String errorMessage) {
        try {
            if (errorMessage == null) {
                return null;
            }
            String token = "NormsHeaderFkId ";
            int start = errorMessage.indexOf(token);
            if (start < 0) {
                return null;
            }
            String after = errorMessage.substring(start + token.length());
            String idStr = after.split("[: ]", 2)[0].trim();
            if (idStr.isEmpty()) {
                return null;
            }
            return UUID.fromString(idStr);
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] generateErrorExcel(List<CPPNormsResponseDTO> dtoList, String financialYear) throws IOException {
        log.info("Exporting {} failed records with error information", dtoList.size());

        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("CPP Norms");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle numericStyle = createNumericStyle(workbook);

            int rowNum = 0;
            int col = 0;

            Row headerRow = sheet.createRow(rowNum++);

            headerRow.createCell(col).setCellValue("CPP Plant");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("Generating Plant");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("Utility");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("Utility ID");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("UOM");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("Account");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("Material");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("SAP Code");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("Issuing Plant");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("Issuing UOM");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("AOP Year");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("Norm Type");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            String startYearSuffix = financialYear.substring(2, 4);
            String endYearSuffix = financialYear.substring(5, 7);
            String[] months = {"Apr-" + startYearSuffix, "May-" + startYearSuffix, "Jun-" + startYearSuffix, "Jul-" + startYearSuffix,
                    "Aug-" + startYearSuffix, "Sep-" + startYearSuffix, "Oct-" + startYearSuffix, "Nov-" + startYearSuffix,
                    "Dec-" + startYearSuffix, "Jan-" + endYearSuffix, "Feb-" + endYearSuffix, "Mar-" + endYearSuffix};

            int monthStartCol = col;
            for (String month : months) {
                headerRow.createCell(col).setCellValue(month);
                headerRow.getCell(col++).setCellStyle(headerStyle);
            }

            int remarksCol = col;
            headerRow.createCell(col).setCellValue("Remarks");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int applyActualNormToAllCol = col;
            headerRow.createCell(col).setCellValue("applyActualNormToAll");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int idCol = col;
            headerRow.createCell(col).setCellValue("id");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int cppNormsIdCol = col;
            headerRow.createCell(col).setCellValue("cppNormsId");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int normsHeaderFkIdCol = col;
            headerRow.createCell(col).setCellValue("normsHeaderFkId");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int normTypeFkIdCol = col;
            headerRow.createCell(col).setCellValue("normTypeFkId");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int dataHashCol = col;
            headerRow.createCell(col).setCellValue("dataHash");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            headerRow.createCell(col).setCellValue("Status");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("Error Description");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int totalColumns = col;

            for (CPPNormsResponseDTO dto : dtoList) {
                Row row = sheet.createRow(rowNum++);
                col = 0;

                setStringCellValue(row.createCell(col++), dto.getCppPlantName(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getGeneratingPlantName(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getUtilityName(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getUtilityId(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getUom(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getAccountName(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getMaterialName(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getMaterialId(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getIssuingPlantName(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getIssuingUom(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getAopYear(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getNormTypeName(), dataStyle);

                setBigDecimalCellValue(row.createCell(monthStartCol + 0), dto.getAprNorms(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 1), dto.getMayNorms(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 2), dto.getJunNorms(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 3), dto.getJulNorms(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 4), dto.getAugNorms(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 5), dto.getSepNorms(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 6), dto.getOctNorms(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 7), dto.getNovNorms(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 8), dto.getDecNorms(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 9), dto.getJanNorms(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 10), dto.getFebNorms(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 11), dto.getMarNorms(), numericStyle);
                col = monthStartCol + 12;

                setStringCellValue(row.createCell(col++), dto.getRemarks(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getApplyActualNormToAll() != null ? dto.getApplyActualNormToAll().toString() : "false", dataStyle);
                setStringCellValue(row.createCell(col++), dto.getId() != null ? dto.getId().toString() : null, dataStyle);
                setStringCellValue(row.createCell(col++), dto.getCppNormsId() != null ? dto.getCppNormsId().toString() : null, dataStyle);
                setStringCellValue(row.createCell(col++), dto.getNormsHeaderFkId() != null ? dto.getNormsHeaderFkId().toString() : null, dataStyle);
                setStringCellValue(row.createCell(col++), dto.getNormTypeFkId() != null ? dto.getNormTypeFkId().toString() : null, dataStyle);

                String dataHash = generateNormsHash(dto);
                setStringCellValue(row.createCell(col++), dataHash, dataStyle);

                setStringCellValue(row.createCell(col++), dto.getSaveStatus(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getErrDescription(), dataStyle);
            }

            sheet.setColumnHidden(applyActualNormToAllCol, true);
            sheet.setColumnHidden(idCol, true);
            sheet.setColumnHidden(cppNormsIdCol, true);
            sheet.setColumnHidden(normsHeaderFkIdCol, true);
            sheet.setColumnHidden(normTypeFkIdCol, true);
            sheet.setColumnHidden(dataHashCol, true);

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
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Error exporting CPP norms with errors", e);
            throw new IOException("Failed to export error file: " + e.getMessage(), e);
        }
    }

    private String getStringCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        try {
            switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d)) {
                    return String.valueOf((long) d);
                }
                return String.valueOf(d);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal getBigDecimalCellValue(Cell cell) {
        String str = getStringCellValue(cell);
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(str.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private CPPNormsResponseDTO mapRowToDto(Object[] row) {
        CPPNormsResponseDTO dto = new CPPNormsResponseDTO();

        int idx = 0;
        dto.setId(row[idx++] != null ? ((Number) row[idx - 1]).longValue() : null);
        dto.setCppPlantId(row[idx++] != null ? UUID.fromString(row[idx - 1].toString()) : null);
        dto.setCppPlantName(row[idx++] != null ? row[idx - 1].toString() : null);
        dto.setCppNormsId(row[idx++] != null ? UUID.fromString(row[idx - 1].toString()) : null);
        dto.setNormsHeaderFkId(row[idx++] != null ? UUID.fromString(row[idx - 1].toString()) : null);
        dto.setGeneratingPlantName(row[idx++] != null ? row[idx - 1].toString() : null);
        dto.setUtilityName(row[idx++] != null ? row[idx - 1].toString() : null);
        dto.setUtilityId(row[idx++] != null ? row[idx - 1].toString() : null);
        dto.setUom(row[idx++] != null ? row[idx - 1].toString() : null);
        dto.setAccountName(row[idx++] != null ? row[idx - 1].toString() : null);
        dto.setMaterialName(row[idx++] != null ? row[idx - 1].toString() : null);
        dto.setMaterialId(row[idx++] != null ? row[idx - 1].toString() : null);
        dto.setIssuingPlantName(row[idx++] != null ? row[idx - 1].toString() : null);
        dto.setIssuingUom(row[idx++] != null ? row[idx - 1].toString() : null);
        dto.setAopYear(row[idx++] != null ? row[idx - 1].toString() : null);
        dto.setNormTypeFkId(row[idx++] != null ? ((Number) row[idx - 1]).intValue() : null);
        dto.setNormTypeName(row[idx++] != null ? row[idx - 1].toString() : null);

        dto.setAprNorms(row[idx++] != null ? new BigDecimal(row[idx - 1].toString()) : null);
        dto.setMayNorms(row[idx++] != null ? new BigDecimal(row[idx - 1].toString()) : null);
        dto.setJunNorms(row[idx++] != null ? new BigDecimal(row[idx - 1].toString()) : null);
        dto.setJulNorms(row[idx++] != null ? new BigDecimal(row[idx - 1].toString()) : null);
        dto.setAugNorms(row[idx++] != null ? new BigDecimal(row[idx - 1].toString()) : null);
        dto.setSepNorms(row[idx++] != null ? new BigDecimal(row[idx - 1].toString()) : null);
        dto.setOctNorms(row[idx++] != null ? new BigDecimal(row[idx - 1].toString()) : null);
        dto.setNovNorms(row[idx++] != null ? new BigDecimal(row[idx - 1].toString()) : null);
        dto.setDecNorms(row[idx++] != null ? new BigDecimal(row[idx - 1].toString()) : null);
        dto.setJanNorms(row[idx++] != null ? new BigDecimal(row[idx - 1].toString()) : null);
        dto.setFebNorms(row[idx++] != null ? new BigDecimal(row[idx - 1].toString()) : null);
        dto.setMarNorms(row[idx++] != null ? new BigDecimal(row[idx - 1].toString()) : null);

        dto.setRemarks(row[idx++] != null ? row[idx - 1].toString() : null);
        dto.setModifiedBy(row[idx++] != null ? row[idx - 1].toString() : null);
        dto.setModifiedDate(row[idx++] != null ? row[idx - 1].toString() : null);
        dto.setActualNorm(row[idx++] != null ? new BigDecimal(row[idx - 1].toString()) : BigDecimal.ZERO);
        dto.setApplyActualNormToAll(row[idx++] != null ? (Boolean) row[idx - 1] : false);

        return dto;
    }
}
