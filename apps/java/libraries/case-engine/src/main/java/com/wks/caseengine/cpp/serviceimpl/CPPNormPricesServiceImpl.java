package com.wks.caseengine.cpp.serviceimpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.poi.ss.usermodel.BorderStyle;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.dto.norm.CPPNormPricesRequestDTO;
import com.wks.caseengine.cpp.dto.norm.CPPNormPricesResponseDTO;
import com.wks.caseengine.cpp.service.CPPNormPricesService;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.FinancialYearMonthRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CPPNormPricesServiceImpl implements CPPNormPricesService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FinancialYearMonthRepository fyRepo;

    @Override
    public AOPMessageVM getCPPNormPrices(UUID cppPlantId, String financialYear) {
        log.info("=== Starting getCPPNormPrices ===");
        log.info("CPPPlantId: {}, FinancialYear: {}", cppPlantId, financialYear);

        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (cppPlantId == null) {
                vm.setCode(400);
                vm.setMessage("CPPPlantId cannot be null");
                vm.setData(new ArrayList<>());
                return vm;
            }

            if (financialYear == null || financialYear.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("FinancialYear cannot be null or empty");
                vm.setData(new ArrayList<>());
                return vm;
            }

            StoredProcedureQuery sp = entityManager
                    .createStoredProcedureQuery("dbo.CPP_GetCPPNormPrices")
                    .registerStoredProcedureParameter(1, String.class, ParameterMode.IN)
                    .registerStoredProcedureParameter(2, String.class, ParameterMode.IN);

            sp.setParameter(1, cppPlantId.toString());
            sp.setParameter(2, financialYear);

            log.info("Executing stored procedure dbo.CPP_GetCPPNormPrices ...");
            sp.execute();

            @SuppressWarnings("unchecked")
            List<Object[]> rawResults = sp.getResultList();
            log.info("Raw result count: {}", rawResults.size());

            List<CPPNormPricesResponseDTO> dtoList = new ArrayList<>();

            for (Object[] row : rawResults) {
                CPPNormPricesResponseDTO dto = new CPPNormPricesResponseDTO();

                int idx = 0;
                dto.setId(row[idx++] != null ? ((Number) row[idx - 1]).longValue() : null);
                dto.setCppMonthWisePriceId(row[idx++] != null ? UUID.fromString(row[idx - 1].toString()) : null);
                dto.setNormsHeaderFkId(row[idx++] != null ? UUID.fromString(row[idx - 1].toString()) : null);
                dto.setGeneratingPlantName(getString(row[idx++]));
                dto.setUtilityName(getString(row[idx++]));
                dto.setUtilityId(getString(row[idx++]));
                dto.setUom(getString(row[idx++]));
                dto.setAccountName(getString(row[idx++]));
                dto.setMaterialName(getString(row[idx++]));
                dto.setMaterialId(getString(row[idx++]));
                dto.setIssuingPlantName(getString(row[idx++]));
                dto.setIssuingUom(getString(row[idx++]));
                dto.setAopYear(getString(row[idx++]));
                dto.setAprPrice(getBigDecimalOrZero(row[idx++]));
                dto.setMayPrice(getBigDecimalOrZero(row[idx++]));
                dto.setJunPrice(getBigDecimalOrZero(row[idx++]));
                dto.setJulPrice(getBigDecimalOrZero(row[idx++]));
                dto.setAugPrice(getBigDecimalOrZero(row[idx++]));
                dto.setSepPrice(getBigDecimalOrZero(row[idx++]));
                dto.setOctPrice(getBigDecimalOrZero(row[idx++]));
                dto.setNovPrice(getBigDecimalOrZero(row[idx++]));
                dto.setDecPrice(getBigDecimalOrZero(row[idx++]));
                dto.setJanPrice(getBigDecimalOrZero(row[idx++]));
                dto.setFebPrice(getBigDecimalOrZero(row[idx++]));
                dto.setMarPrice(getBigDecimalOrZero(row[idx++]));

                dto.setRemarks(getString(row[idx++]));
                dto.setPriceSource(getString(row[idx++]));
                dto.setModifiedBy(getString(row[idx++]));
                dto.setCreatedDate(getString(row[idx++]));
                dto.setUpdatedDate(getString(row[idx++]));
                dto.setValueType(getString(row[idx++]));

                dtoList.add(dto);
            }

            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(dtoList);

        } catch (Exception e) {
            log.error("=== ERROR in getCPPNormPrices ===", e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
        }

        return vm;
    }

    @Override
    public byte[] exportCPPNormPrices(UUID cppPlantId, String financialYear) throws IOException {
        log.info("=== Starting exportCPPNormPrices ===");
        log.info("CPPPlantId: {}, FinancialYear: {}", cppPlantId, financialYear);

        try {
            AOPMessageVM result = getCPPNormPrices(cppPlantId, financialYear);
            List<CPPNormPricesResponseDTO> dtoList = new ArrayList<>();
            if (result.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<CPPNormPricesResponseDTO> data = (List<CPPNormPricesResponseDTO>) result.getData();
                dtoList = data;
            }

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("CPP Norm Prices");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle numericStyle = createNumericStyle(workbook);

            int rowNum = 0;
            int col = 0;

            Row headerRow = sheet.createRow(rowNum++);

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
            
            headerRow.createCell(col).setCellValue("Value Type");
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

            headerRow.createCell(col).setCellValue("Price Source");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int remarksCol = col;
            headerRow.createCell(col).setCellValue("Remarks");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int idCol = col;
            headerRow.createCell(col).setCellValue("id");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int cppMonthWisePriceIdCol = col;
            headerRow.createCell(col).setCellValue("cppMonthWisePriceId");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int normsHeaderFkIdCol = col;
            headerRow.createCell(col).setCellValue("normsHeaderFkId");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int createdDateCol = col;
            headerRow.createCell(col).setCellValue("createdDate");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int updatedDateCol = col;
            headerRow.createCell(col).setCellValue("updatedDate");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int totalColumns = col;

            for (CPPNormPricesResponseDTO dto : dtoList) {
                Row row = sheet.createRow(rowNum++);
                col = 0;

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
                setStringCellValue(row.createCell(col++), dto.getValueType(), dataStyle);

                setBigDecimalCellValue(row.createCell(monthStartCol + 0), dto.getAprPrice(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 1), dto.getMayPrice(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 2), dto.getJunPrice(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 3), dto.getJulPrice(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 4), dto.getAugPrice(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 5), dto.getSepPrice(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 6), dto.getOctPrice(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 7), dto.getNovPrice(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 8), dto.getDecPrice(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 9), dto.getJanPrice(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 10), dto.getFebPrice(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 11), dto.getMarPrice(), numericStyle);
                col = monthStartCol + 12;

                setStringCellValue(row.createCell(col++), dto.getPriceSource(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getRemarks(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getId() != null ? dto.getId().toString() : null, dataStyle);
                setStringCellValue(row.createCell(col++), dto.getCppMonthWisePriceId() != null ? dto.getCppMonthWisePriceId().toString() : null, dataStyle);
                setStringCellValue(row.createCell(col++), dto.getNormsHeaderFkId() != null ? dto.getNormsHeaderFkId().toString() : null, dataStyle);
                setStringCellValue(row.createCell(col++), dto.getCreatedDate(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getUpdatedDate(), dataStyle);
            }

            sheet.setColumnHidden(idCol, true);
            sheet.setColumnHidden(cppMonthWisePriceIdCol, true);
            sheet.setColumnHidden(normsHeaderFkIdCol, true);
            sheet.setColumnHidden(createdDateCol, true);
            sheet.setColumnHidden(updatedDateCol, true);

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

        } catch (IOException e) {
            log.error("IOException while exporting CPP norm prices", e);
            throw e;
        } catch (Exception e) {
            log.error("Error exporting CPP norm prices", e);
            throw new IOException("Failed to export CPP norm prices: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public AOPMessageVM importExcel(UUID cppPlantId, String financialYear, MultipartFile file, String modifiedBy) throws IOException {
        log.info("=== Starting importExcel (CPP Norm Prices) ===");
        log.info("CPPPlantId: {}, FinancialYear: {}, FileName: {}, FileSize: {}",
                cppPlantId, financialYear, file.getOriginalFilename(), file.getSize());

        try {
            List<CPPNormPricesResponseDTO> data = readCPPNormPrices(file.getInputStream());
            log.info("Read {} records from Excel file", data.size());

            Map<UUID, CPPNormPricesResponseDTO> existingByHeader = loadExistingPrices(cppPlantId, financialYear);
            List<CPPNormPricesResponseDTO> validRecords = new ArrayList<>();
            List<CPPNormPricesResponseDTO> failedRecords = new ArrayList<>();
            int skippedCount = 0;

            for (CPPNormPricesResponseDTO dto : data) {
                if (dto.getSaveStatus() != null && dto.getSaveStatus().equalsIgnoreCase("Failed")) {
                    failedRecords.add(dto);
                    continue;
                }

                if (dto.getNormsHeaderFkId() == null) {
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription("NormsHeaderFkId is missing");
                    failedRecords.add(dto);
                    continue;
                }

                CPPNormPricesResponseDTO existing = existingByHeader.get(dto.getNormsHeaderFkId());
                if (!isPriceModified(dto, existing)) {
                    skippedCount++;
                    continue;
                }

                String validationError = validatePriceData(dto);
                if (validationError != null) {
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription(validationError);
                    failedRecords.add(dto);
                    continue;
                }

                validRecords.add(dto);
            }

            if (!validRecords.isEmpty()) {
                List<CPPNormPricesRequestDTO> updateRequests = convertToUpdateRequests(validRecords);
                AOPMessageVM saveResponse = saveOrUpdateCPPNormPrices(updateRequests, financialYear, modifiedBy);

                if (saveResponse.getCode() == 207) {
                    List<String> errorMessages = new ArrayList<>();
                    if (saveResponse.getData() instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> errs = (List<String>) saveResponse.getData();
                        errorMessages = errs;
                    }

                    List<CPPNormPricesResponseDTO> saveFailed = applyErrorsToDtos(validRecords, errorMessages);
                    for (CPPNormPricesResponseDTO dto : saveFailed) {
                        if (!failedRecords.contains(dto)) {
                            failedRecords.add(dto);
                        }
                    }
                } else if (saveResponse.getCode() != 200) {
                    AOPMessageVM errorVM = new AOPMessageVM();
                    errorVM.setCode(saveResponse.getCode());
                    errorVM.setMessage(saveResponse.getMessage());
                    errorVM.setData(saveResponse.getData());
                    return errorVM;
                }
            }

            if (!failedRecords.isEmpty()) {
                byte[] fileByteArray = exportCPPNormPricesWithErrors(cppPlantId, financialYear, failedRecords);
                String base64File = Base64.getEncoder().encodeToString(fileByteArray);

                AOPMessageVM aopMessageVM = new AOPMessageVM();
                aopMessageVM.setCode(400);
                aopMessageVM.setMessage("Partial data has been saved. " + validRecords.size()
                        + " saved, " + failedRecords.size() + " failed, " + skippedCount + " unchanged.");
                aopMessageVM.setData(base64File);
                return aopMessageVM;
            }

            AOPMessageVM ok = new AOPMessageVM();
            ok.setCode(200);
            if (validRecords.isEmpty() && skippedCount > 0) {
                ok.setMessage("No changes detected in imported records. All " + skippedCount + " records unchanged.");
            } else {
                ok.setMessage("All data has been saved. " + validRecords.size() + " updated, " + skippedCount + " unchanged.");
            }
            return ok;

        } catch (IOException e) {
            log.error("IOException while importing CPP norm prices", e);
            throw e;
        } catch (Exception e) {
            log.error("Error importing CPP norm prices", e);
            throw new IOException("Failed to import CPP norm prices: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public AOPMessageVM saveOrUpdateCPPNormPrices(List<CPPNormPricesRequestDTO> dtoList, String financialYear, String modifiedBy) {
        log.info("=== Starting saveOrUpdateCPPNormPrices ===");
        log.info("Total records to process: {}", dtoList != null ? dtoList.size() : 0);
        log.info("Financial Year: {}", financialYear);

        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (dtoList == null || dtoList.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("Request body cannot be empty");
                return vm;
            }

            int startYear = Integer.parseInt(financialYear.substring(0, 4));
            int endYear = startYear + 1;

            List<Object[]> fyMonths = fyRepo.findFinancialYearMonths(startYear, endYear);
            Map<Integer, UUID> monthIdMap = new LinkedHashMap<>();
            for (Object[] row : fyMonths) {
                if (row.length < 2 || row[0] == null || row[1] == null) {
                    continue;
                }
                Integer month = Integer.parseInt(row[0].toString());
                UUID monthId = UUID.fromString(row[1].toString());
                monthIdMap.put(month, monthId);
            }

            Map<UUID, CPPNormPricesRequestDTO> uniqueRequests = new LinkedHashMap<>();
            List<String> errorMessages = new ArrayList<>();

            for (CPPNormPricesRequestDTO dto : dtoList) {
                if (dto == null || dto.getNormsHeaderFkId() == null) {
                    errorMessages.add("NormsHeaderFkId is missing");
                    continue;
                }
                uniqueRequests.put(dto.getNormsHeaderFkId(), dto);
            }

            if (uniqueRequests.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("No valid records to update");
                vm.setData(errorMessages);
                return vm;
            }

            String updateNormsSql = """
                UPDATE CPPMonthWisePrice
                SET Apr_Price = ?, May_Price = ?, Jun_Price = ?, Jul_Price = ?, Aug_Price = ?, Sep_Price = ?,
                    Oct_Price = ?, Nov_Price = ?, Dec_Price = ?, Jan_Price = ?, Feb_Price = ?, Mar_Price = ?,
                    Remarks = ?, PriceSource = ?, ValueType = ?, ModifiedBy = ?, UpdatedDate = GETDATE()
                WHERE NormsHeader_FK_Id = ? AND FinancialYear = ?
                """;

            List<CPPNormPricesRequestDTO> uniqueRequestList = new ArrayList<>(uniqueRequests.values());
            List<Object[]> cppNormUpdates = new ArrayList<>();
            List<Object[]> monthUpdates = new ArrayList<>();

            for (CPPNormPricesRequestDTO dto : uniqueRequestList) {
                BigDecimal apr = normalizePrice(dto.getAprPrice());
                BigDecimal may = normalizePrice(dto.getMayPrice());
                BigDecimal jun = normalizePrice(dto.getJunPrice());
                BigDecimal jul = normalizePrice(dto.getJulPrice());
                BigDecimal aug = normalizePrice(dto.getAugPrice());
                BigDecimal sep = normalizePrice(dto.getSepPrice());
                BigDecimal oct = normalizePrice(dto.getOctPrice());
                BigDecimal nov = normalizePrice(dto.getNovPrice());
                BigDecimal dec = normalizePrice(dto.getDecPrice());
                BigDecimal jan = normalizePrice(dto.getJanPrice());
                BigDecimal feb = normalizePrice(dto.getFebPrice());
                BigDecimal mar = normalizePrice(dto.getMarPrice());

                cppNormUpdates.add(new Object[] {
                        apr, may, jun, jul, aug, sep, oct, nov, dec, jan, feb, mar,
                        dto.getRemarks(), dto.getPriceSource(), dto.getValueType(), modifiedBy, dto.getNormsHeaderFkId(), financialYear
                });

                addMonthUpdate(monthUpdates, dto.getNormsHeaderFkId(), monthIdMap.get(4), apr, dto.getValueType());
                addMonthUpdate(monthUpdates, dto.getNormsHeaderFkId(), monthIdMap.get(5), may, dto.getValueType());
                addMonthUpdate(monthUpdates, dto.getNormsHeaderFkId(), monthIdMap.get(6), jun, dto.getValueType());
                addMonthUpdate(monthUpdates, dto.getNormsHeaderFkId(), monthIdMap.get(7), jul, dto.getValueType());
                addMonthUpdate(monthUpdates, dto.getNormsHeaderFkId(), monthIdMap.get(8), aug, dto.getValueType());
                addMonthUpdate(monthUpdates, dto.getNormsHeaderFkId(), monthIdMap.get(9), sep, dto.getValueType());
                addMonthUpdate(monthUpdates, dto.getNormsHeaderFkId(), monthIdMap.get(10), oct, dto.getValueType());
                addMonthUpdate(monthUpdates, dto.getNormsHeaderFkId(), monthIdMap.get(11), nov, dto.getValueType());
                addMonthUpdate(monthUpdates, dto.getNormsHeaderFkId(), monthIdMap.get(12), dec, dto.getValueType());
                addMonthUpdate(monthUpdates, dto.getNormsHeaderFkId(), monthIdMap.get(1), jan, dto.getValueType());
                addMonthUpdate(monthUpdates, dto.getNormsHeaderFkId(), monthIdMap.get(2), feb, dto.getValueType());
                addMonthUpdate(monthUpdates, dto.getNormsHeaderFkId(), monthIdMap.get(3), mar, dto.getValueType());
            }

            int[] updateCounts = jdbcTemplate.batchUpdate(updateNormsSql, cppNormUpdates);
            List<Object[]> insertParams = new ArrayList<>();
            for (int i = 0; i < updateCounts.length; i++) {
                if (updateCounts[i] == 0) {
                    CPPNormPricesRequestDTO dto = uniqueRequestList.get(i);
                    insertParams.add(new Object[] {
                            UUID.randomUUID(), dto.getNormsHeaderFkId(), financialYear, dto.getAopYear(),
                            normalizePrice(dto.getAprPrice()), normalizePrice(dto.getMayPrice()), normalizePrice(dto.getJunPrice()),
                            normalizePrice(dto.getJulPrice()), normalizePrice(dto.getAugPrice()), normalizePrice(dto.getSepPrice()),
                            normalizePrice(dto.getOctPrice()), normalizePrice(dto.getNovPrice()), normalizePrice(dto.getDecPrice()),
                            normalizePrice(dto.getJanPrice()), normalizePrice(dto.getFebPrice()), normalizePrice(dto.getMarPrice()),
                            dto.getRemarks(), dto.getPriceSource(), modifiedBy, dto.getValueType()
                    });
                }
            }

            if (!insertParams.isEmpty()) {
                String insertSql = """
                    INSERT INTO CPPMonthWisePrice (
                        Id, NormsHeader_FK_Id, FinancialYear, AOPYear,
                        Apr_Price, May_Price, Jun_Price, Jul_Price, Aug_Price, Sep_Price,
                        Oct_Price, Nov_Price, Dec_Price, Jan_Price, Feb_Price, Mar_Price,
                        Remarks, PriceSource, ModifiedBy, CreatedDate, UpdatedDate, ValueType
                    ) VALUES (
                        ?, ?, ?, ?,
                        ?, ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?, ?,
                        ?, ?, ?, GETDATE(), GETDATE(), ?
                    )
                    """;
                jdbcTemplate.batchUpdate(insertSql, insertParams);
            }

            String updateMonthSql = """
                UPDATE NormsMonthDetail
                SET Price = ?, Amount = ?
                WHERE NormsHeader_FK_Id = ? AND FinancialYearMonth_FK_Id = ?
                """;
            jdbcTemplate.batchUpdate(updateMonthSql, monthUpdates);

            if (!errorMessages.isEmpty()) {
                vm.setCode(207);
                vm.setMessage("Partial data processed with validation errors");
                vm.setData(errorMessages);
            } else {
                vm.setCode(200);
                vm.setMessage(String.format("Successfully processed %d record(s)", uniqueRequests.size()));
                vm.setData(null);
            }

        } catch (Exception e) {
            log.error("=== ERROR in saveOrUpdateCPPNormPrices ===", e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(null);
        }

        return vm;
    }

    private void addMonthUpdate(
        List<Object[]> updates,
        UUID normsHeaderFkId,
        UUID monthId,
        BigDecimal value,
        String valueType
        ) {

            if (normsHeaderFkId == null || monthId == null) {
                return;
            }

            BigDecimal price = BigDecimal.ZERO;
            BigDecimal amount = BigDecimal.ZERO;

            if ("Price".equalsIgnoreCase(valueType)) {

                price = value != null ? value : BigDecimal.ZERO;
                amount = BigDecimal.ZERO;

            } else if ("Amount".equalsIgnoreCase(valueType)) {

                price = BigDecimal.ZERO;
                amount = value != null ? value : BigDecimal.ZERO;

            } else {

                price = BigDecimal.ZERO;
                amount = BigDecimal.ZERO;
            }

            updates.add(new Object[] {
                    price,
                    amount,
                    normsHeaderFkId,
                    monthId
            });
        }

    private Map<UUID, CPPNormPricesResponseDTO> loadExistingPrices(UUID cppPlantId, String financialYear) {
        Map<UUID, CPPNormPricesResponseDTO> existing = new LinkedHashMap<>();
        try {
            AOPMessageVM response = getCPPNormPrices(cppPlantId, financialYear);
            if (response != null && response.getCode() == 200 && response.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<CPPNormPricesResponseDTO> data = (List<CPPNormPricesResponseDTO>) response.getData();
                for (CPPNormPricesResponseDTO dto : data) {
                    if (dto.getNormsHeaderFkId() != null) {
                        existing.put(dto.getNormsHeaderFkId(), dto);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Unable to load existing CPP norm prices for validation", e);
        }
        return existing;
    }

    private boolean isPriceModified(CPPNormPricesResponseDTO incoming, CPPNormPricesResponseDTO existing) {
        if (existing == null) {
            return true;
        }

        return !(pricesEqual(incoming.getAprPrice(), existing.getAprPrice())
                && pricesEqual(incoming.getMayPrice(), existing.getMayPrice())
                && pricesEqual(incoming.getJunPrice(), existing.getJunPrice())
                && pricesEqual(incoming.getJulPrice(), existing.getJulPrice())
                && pricesEqual(incoming.getAugPrice(), existing.getAugPrice())
                && pricesEqual(incoming.getSepPrice(), existing.getSepPrice())
                && pricesEqual(incoming.getOctPrice(), existing.getOctPrice())
                && pricesEqual(incoming.getNovPrice(), existing.getNovPrice())
                && pricesEqual(incoming.getDecPrice(), existing.getDecPrice())
                && pricesEqual(incoming.getJanPrice(), existing.getJanPrice())
                && pricesEqual(incoming.getFebPrice(), existing.getFebPrice())
                && pricesEqual(incoming.getMarPrice(), existing.getMarPrice())
                && remarksEqual(incoming.getRemarks(), existing.getRemarks()));
    }

    private boolean pricesEqual(BigDecimal left, BigDecimal right) {
        return normalizePrice(left).compareTo(normalizePrice(right)) == 0;
    }

    private boolean remarksEqual(String left, String right) {
        String leftValue = left == null ? "" : left.trim();
        String rightValue = right == null ? "" : right.trim();
        return leftValue.equals(rightValue);
    }

    private String validatePriceData(CPPNormPricesResponseDTO dto) {
        if (isNegative(dto.getAprPrice())) {
            return "Apr price cannot be negative";
        }
        if (isNegative(dto.getMayPrice())) {
            return "May price cannot be negative";
        }
        if (isNegative(dto.getJunPrice())) {
            return "Jun price cannot be negative";
        }
        if (isNegative(dto.getJulPrice())) {
            return "Jul price cannot be negative";
        }
        if (isNegative(dto.getAugPrice())) {
            return "Aug price cannot be negative";
        }
        if (isNegative(dto.getSepPrice())) {
            return "Sep price cannot be negative";
        }
        if (isNegative(dto.getOctPrice())) {
            return "Oct price cannot be negative";
        }
        if (isNegative(dto.getNovPrice())) {
            return "Nov price cannot be negative";
        }
        if (isNegative(dto.getDecPrice())) {
            return "Dec price cannot be negative";
        }
        if (isNegative(dto.getJanPrice())) {
            return "Jan price cannot be negative";
        }
        if (isNegative(dto.getFebPrice())) {
            return "Feb price cannot be negative";
        }
        if (isNegative(dto.getMarPrice())) {
            return "Mar price cannot be negative";
        }
        return null;
    }

    private boolean isNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        return price == null ? BigDecimal.ZERO : price;
    }

    private String getString(Object value) {
        return value == null ? null : value.toString();
    }

    private BigDecimal getBigDecimalOrZero(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        try {
            String str = value.toString();
            return str.isEmpty() ? BigDecimal.ZERO : new BigDecimal(str);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private byte[] exportCPPNormPricesWithErrors(UUID cppPlantId, String financialYear, List<CPPNormPricesResponseDTO> dtoList) throws IOException {
        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("CPP Norm Prices");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle numericStyle = createNumericStyle(workbook);

            int rowNum = 0;
            int col = 0;

            Row headerRow = sheet.createRow(rowNum++);

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
            
            headerRow.createCell(col).setCellValue("Value Type");
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

            headerRow.createCell(col).setCellValue("Price Source");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int remarksCol = col;
            headerRow.createCell(col).setCellValue("Remarks");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int idCol = col;
            headerRow.createCell(col).setCellValue("id");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int cppMonthWisePriceIdCol = col;
            headerRow.createCell(col).setCellValue("cppMonthWisePriceId");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int normsHeaderFkIdCol = col;
            headerRow.createCell(col).setCellValue("normsHeaderFkId");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int createdDateCol = col;
            headerRow.createCell(col).setCellValue("createdDate");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int updatedDateCol = col;
            headerRow.createCell(col).setCellValue("updatedDate");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            headerRow.createCell(col).setCellValue("Status");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            headerRow.createCell(col).setCellValue("Error Description");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int totalColumns = col;

            for (CPPNormPricesResponseDTO dto : dtoList) {
                Row row = sheet.createRow(rowNum++);
                col = 0;

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
                setStringCellValue(row.createCell(col++), dto.getValueType(), dataStyle);

                setBigDecimalCellValue(row.createCell(monthStartCol + 0), dto.getAprPrice(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 1), dto.getMayPrice(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 2), dto.getJunPrice(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 3), dto.getJulPrice(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 4), dto.getAugPrice(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 5), dto.getSepPrice(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 6), dto.getOctPrice(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 7), dto.getNovPrice(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 8), dto.getDecPrice(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 9), dto.getJanPrice(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 10), dto.getFebPrice(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 11), dto.getMarPrice(), numericStyle);
                col = monthStartCol + 12;

                setStringCellValue(row.createCell(col++), dto.getPriceSource(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getRemarks(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getId() != null ? dto.getId().toString() : null, dataStyle);
                setStringCellValue(row.createCell(col++), dto.getCppMonthWisePriceId() != null ? dto.getCppMonthWisePriceId().toString() : null, dataStyle);
                setStringCellValue(row.createCell(col++), dto.getNormsHeaderFkId() != null ? dto.getNormsHeaderFkId().toString() : null, dataStyle);
                setStringCellValue(row.createCell(col++), dto.getCreatedDate(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getUpdatedDate(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getSaveStatus(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getErrDescription(), dataStyle);
            }

            sheet.setColumnHidden(idCol, true);
            sheet.setColumnHidden(cppMonthWisePriceIdCol, true);
            sheet.setColumnHidden(normsHeaderFkIdCol, true);
            sheet.setColumnHidden(createdDateCol, true);
            sheet.setColumnHidden(updatedDateCol, true);

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
            log.error("Error exporting CPP norm prices with errors", e);
            throw new IOException("Failed to export error file: " + e.getMessage(), e);
        }
    }

    private List<CPPNormPricesResponseDTO> readCPPNormPrices(InputStream inputStream) {
        List<CPPNormPricesResponseDTO> dataList = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (rowIterator.hasNext()) {
                rowIterator.next();
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                CPPNormPricesResponseDTO dto = new CPPNormPricesResponseDTO();

                try {
                    int col = 0;
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
                    dto.setValueType(getStringCellValue(row.getCell(col++)));

                    dto.setAprPrice(getBigDecimalCellValue(row.getCell(col++)));
                    dto.setMayPrice(getBigDecimalCellValue(row.getCell(col++)));
                    dto.setJunPrice(getBigDecimalCellValue(row.getCell(col++)));
                    dto.setJulPrice(getBigDecimalCellValue(row.getCell(col++)));
                    dto.setAugPrice(getBigDecimalCellValue(row.getCell(col++)));
                    dto.setSepPrice(getBigDecimalCellValue(row.getCell(col++)));
                    dto.setOctPrice(getBigDecimalCellValue(row.getCell(col++)));
                    dto.setNovPrice(getBigDecimalCellValue(row.getCell(col++)));
                    dto.setDecPrice(getBigDecimalCellValue(row.getCell(col++)));
                    dto.setJanPrice(getBigDecimalCellValue(row.getCell(col++)));
                    dto.setFebPrice(getBigDecimalCellValue(row.getCell(col++)));
                    dto.setMarPrice(getBigDecimalCellValue(row.getCell(col++)));

                    dto.setPriceSource(getStringCellValue(row.getCell(col++)));
                    dto.setRemarks(getStringCellValue(row.getCell(col++)));

                    String idStr = getStringCellValue(row.getCell(col++));
                    if (idStr != null && !idStr.isEmpty()) {
                        dto.setId(Long.parseLong(idStr));
                    }

                    String cppMonthWisePriceIdStr = getStringCellValue(row.getCell(col++));
                    if (cppMonthWisePriceIdStr != null && !cppMonthWisePriceIdStr.isEmpty()) {
                        dto.setCppMonthWisePriceId(UUID.fromString(cppMonthWisePriceIdStr));
                    }

                    String normsHeaderFkIdStr = getStringCellValue(row.getCell(col++));
                    if (normsHeaderFkIdStr != null && !normsHeaderFkIdStr.isEmpty()) {
                        dto.setNormsHeaderFkId(UUID.fromString(normsHeaderFkIdStr));
                    }

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
            log.error("Error reading CPP norm prices file", e);
        }

        return dataList;
    }

    private List<CPPNormPricesRequestDTO> convertToUpdateRequests(List<CPPNormPricesResponseDTO> dtoList) {
        List<CPPNormPricesRequestDTO> requests = new ArrayList<>();
        for (CPPNormPricesResponseDTO dto : dtoList) {
            if (dto.getSaveStatus() != null && dto.getSaveStatus().equalsIgnoreCase("Failed")) {
                continue;
            }

            CPPNormPricesRequestDTO request = new CPPNormPricesRequestDTO();
            request.setCppMonthWisePriceId(dto.getCppMonthWisePriceId());
            request.setNormsHeaderFkId(dto.getNormsHeaderFkId());
            request.setAopYear(dto.getAopYear());
            request.setAprPrice(dto.getAprPrice());
            request.setMayPrice(dto.getMayPrice());
            request.setJunPrice(dto.getJunPrice());
            request.setJulPrice(dto.getJulPrice());
            request.setAugPrice(dto.getAugPrice());
            request.setSepPrice(dto.getSepPrice());
            request.setOctPrice(dto.getOctPrice());
            request.setNovPrice(dto.getNovPrice());
            request.setDecPrice(dto.getDecPrice());
            request.setJanPrice(dto.getJanPrice());
            request.setFebPrice(dto.getFebPrice());
            request.setMarPrice(dto.getMarPrice());
            request.setRemarks(dto.getRemarks());
            request.setPriceSource(dto.getPriceSource());
            request.setValueType(dto.getValueType());
            requests.add(request);
        }
        return requests;
    }

    private List<CPPNormPricesResponseDTO> applyErrorsToDtos(List<CPPNormPricesResponseDTO> dtoList, List<String> errorMessages) {
        List<CPPNormPricesResponseDTO> failed = new ArrayList<>();

        for (CPPNormPricesResponseDTO dto : dtoList) {
            if (dto.getSaveStatus() != null && dto.getSaveStatus().equalsIgnoreCase("Failed")) {
                failed.add(dto);
            }
        }

        for (String msg : errorMessages) {
            UUID normsHeaderFkId = extractNormsHeaderFkId(msg);
            if (normsHeaderFkId == null) {
                continue;
            }
            for (CPPNormPricesResponseDTO dto : dtoList) {
                if (dto.getNormsHeaderFkId() != null && dto.getNormsHeaderFkId().equals(normsHeaderFkId)) {
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription(msg);
                    if (!failed.contains(dto)) {
                        failed.add(dto);
                    }
                    break;
                }
            }
        }

        return failed;
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

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createNumericStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setDataFormat(workbook.createDataFormat().getFormat("0.##########"));
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
            case BLANK:
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
}
