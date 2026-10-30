package com.wks.caseengine.cpp.serviceimpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.dto.norm.CPPActualRateEDRPODTO;
import com.wks.caseengine.cpp.dto.norm.CPPActualRateEDRPORequestDTO;
import com.wks.caseengine.cpp.dto.norm.CPPEdRpoMappingDTO;
import com.wks.caseengine.cpp.dto.norm.CPPEdRpoMappingRequestDTO;
import com.wks.caseengine.cpp.dto.norm.JMDCPPNormPricesRequestDTO;
import com.wks.caseengine.cpp.dto.norm.JMDCPPNormPricesResponseDTO;
import com.wks.caseengine.cpp.service.JMDCPPNormPricesService;
import com.wks.caseengine.cpp.utility.ExcelStyles;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.FinancialYearMonthRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JMDCPPNormPricesServiceImpl implements JMDCPPNormPricesService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FinancialYearMonthRepository fyRepo;

    // ── GET ───────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM getCPPNormPrices(List<UUID> plantIds, String aopYear) {
        log.info("[JMDCPPNormPrices] GET - plantIds: {}, aopYear: {}", plantIds, aopYear);
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
                    .createStoredProcedureQuery("dbo.CPP_JMD_GetCPPNormPrices")
                    .registerStoredProcedureParameter("CPPPlantIds", String.class, ParameterMode.IN)
                    .registerStoredProcedureParameter("FinancialYear", String.class, ParameterMode.IN);

            sp.setParameter("CPPPlantIds", plantIdsCsv);
            sp.setParameter("FinancialYear", aopYear);

            log.info("Executing stored procedure dbo.CPP_JMD_GetCPPNormPrices for plantIds: {}, aopYear: {}", plantIdsCsv, aopYear);
            sp.execute();

            @SuppressWarnings("unchecked")
            List<Object[]> rawResults = sp.getResultList();
            log.info("[JMDCPPNormPrices] GET - SP returned {} records", rawResults.size());

            List<JMDCPPNormPricesResponseDTO> dtoList = new ArrayList<>();
            for (Object[] row : rawResults) {
                dtoList.add(mapRowToDto(row));
            }

            // Generate dataHash for each record (used for change detection on import)
            for (JMDCPPNormPricesResponseDTO dto : dtoList) {
                dto.setDataHash(generateHash(dto));
            }

            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(dtoList);

        } catch (Exception e) {
            log.error("[JMDCPPNormPrices] GET error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
        }

        return vm;
    }

    // ── SAVE ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM saveOrUpdateCPPNormPrices(List<UUID> plantIds, String aopYear, List<JMDCPPNormPricesRequestDTO> dtoList) {
        log.info("[JMDCPPNormPrices] SAVE - plantIds: {}, aopYear: {}, records: {}",
                plantIds, aopYear, dtoList != null ? dtoList.size() : 0);
        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (dtoList == null || dtoList.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("Request body cannot be empty");
                return vm;
            }

            int startYear = Integer.parseInt(aopYear.substring(0, 4));
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

            Map<UUID, JMDCPPNormPricesRequestDTO> uniqueRequests = new LinkedHashMap<>();
            List<String> errorMessages = new ArrayList<>();

            for (JMDCPPNormPricesRequestDTO dto : dtoList) {
                if (dto == null || dto.getNormsHeaderFkId() == null) {
                    errorMessages.add("NormsHeaderFkId is missing");
                    continue;
                }
                if (isNonEditableRow(dto)) {
                    log.info("[JMDCPPNormPrices] Skipping non-editable calculated row from update: account={}, normsHeaderFkId={}",
                            dto.getAccountName(), dto.getNormsHeaderFkId());
                    continue;
                }
                uniqueRequests.put(dto.getNormsHeaderFkId(), dto);
            }

            if (uniqueRequests.isEmpty()) {
                if (errorMessages.isEmpty()) {
                    vm.setCode(200);
                    vm.setMessage("No editable records to update. Calculated rows were skipped.");
                    vm.setData(null);
                } else {
                    vm.setCode(400);
                    vm.setMessage("No valid records to update");
                    vm.setData(errorMessages);
                }
                return vm;
            }

            String updateNormsSql = """
                UPDATE CPPMonthWisePrice
                SET Apr_Price = ?, May_Price = ?, Jun_Price = ?, Jul_Price = ?, Aug_Price = ?, Sep_Price = ?,
                    Oct_Price = ?, Nov_Price = ?, Dec_Price = ?, Jan_Price = ?, Feb_Price = ?, Mar_Price = ?,
                    Remarks = ?, PriceSource = ?, ValueType = ?, UpdatedDate = GETDATE()
                WHERE NormsHeader_FK_Id = ? AND FinancialYear = ?
                """;

            List<JMDCPPNormPricesRequestDTO> uniqueRequestList = new ArrayList<>(uniqueRequests.values());
            List<Object[]> cppNormUpdates = new ArrayList<>();
            List<Object[]> monthUpdates = new ArrayList<>();

            for (JMDCPPNormPricesRequestDTO dto : uniqueRequestList) {
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
                        dto.getRemarks(), dto.getPriceSource(), dto.getValueType(),
                        dto.getNormsHeaderFkId(), aopYear
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
                    JMDCPPNormPricesRequestDTO dto = uniqueRequestList.get(i);
                    insertParams.add(new Object[] {
                            UUID.randomUUID(), dto.getNormsHeaderFkId(), aopYear, dto.getAopYear(),
                            normalizePrice(dto.getAprPrice()), normalizePrice(dto.getMayPrice()), normalizePrice(dto.getJunPrice()),
                            normalizePrice(dto.getJulPrice()), normalizePrice(dto.getAugPrice()), normalizePrice(dto.getSepPrice()),
                            normalizePrice(dto.getOctPrice()), normalizePrice(dto.getNovPrice()), normalizePrice(dto.getDecPrice()),
                            normalizePrice(dto.getJanPrice()), normalizePrice(dto.getFebPrice()), normalizePrice(dto.getMarPrice()),
                            dto.getRemarks(), dto.getPriceSource(), "SYSTEM", dto.getValueType()
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
            log.error("[JMDCPPNormPrices] SAVE error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(null);
        }

        return vm;
    }

    // ── EXPORT ────────────────────────────────────────────────────────────────

    @Override
    public byte[] exportCPPNormPrices(List<UUID> plantIds, String aopYear) throws IOException {
        log.info("[JMDCPPNormPrices] EXPORT - plantIds: {}, aopYear: {}", plantIds, aopYear);

        try {
            AOPMessageVM result = getCPPNormPrices(plantIds, aopYear);

            List<JMDCPPNormPricesResponseDTO> dtoList = new ArrayList<>();
            if (result.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<JMDCPPNormPricesResponseDTO> data = (List<JMDCPPNormPricesResponseDTO>) result.getData();
                dtoList = data;
            }

            if (dtoList == null || dtoList.isEmpty()) {
                log.warn("[JMDCPPNormPrices] EXPORT - no data found");
                dtoList = new ArrayList<>();
            }

            return generateExcel(dtoList, aopYear, null);

        } catch (IOException e) {
            log.error("[JMDCPPNormPrices] EXPORT IOException: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("[JMDCPPNormPrices] EXPORT error: {}", e.getMessage(), e);
            throw new IOException("Failed to export CPP Norm Prices: " + e.getMessage(), e);
        }
    }

    // ── IMPORT ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM importExcel(List<UUID> plantIds, String aopYear, MultipartFile file) throws IOException {
        log.info("[JMDCPPNormPrices] IMPORT - plantIds: {}, aopYear: {}, file: {}",
                plantIds, aopYear, file.getOriginalFilename());

        AOPMessageVM vm = new AOPMessageVM();

        try (InputStream inputStream = file.getInputStream()) {
            List<JMDCPPNormPricesResponseDTO> data = readExcel(inputStream);
            log.info("[JMDCPPNormPrices] IMPORT - read {} records from Excel", data.size());

            // Fetch existing data for remark validation and change detection
            AOPMessageVM existingData = getCPPNormPrices(plantIds, aopYear);
            Map<UUID, String> existingRemarks = new HashMap<>();
            Map<UUID, String> existingHashes = new HashMap<>();
            java.util.Set<UUID> nonEditableHeaderIds = new java.util.HashSet<>();
            if (existingData.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<JMDCPPNormPricesResponseDTO> existingList = (List<JMDCPPNormPricesResponseDTO>) existingData.getData();
                for (JMDCPPNormPricesResponseDTO existing : existingList) {
                    if (existing.getNormsHeaderFkId() != null) {
                        existingRemarks.put(existing.getNormsHeaderFkId(),
                                existing.getRemarks() != null ? existing.getRemarks().trim() : "");
                        existingHashes.put(existing.getNormsHeaderFkId(), existing.getDataHash());
                        if (isNonEditableRow(existing)) {
                            nonEditableHeaderIds.add(existing.getNormsHeaderFkId());
                        }
                    }
                }
            }

            List<JMDCPPNormPricesRequestDTO> validRecords = new ArrayList<>();
            List<JMDCPPNormPricesResponseDTO> failedRecords = new ArrayList<>();
            int skippedCount = 0;

            for (JMDCPPNormPricesResponseDTO dto : data) {
                // Skip non-editable / calculated rows from update (whether edited or not)
                if (isNonEditableRow(dto)
                        || (dto.getNormsHeaderFkId() != null && nonEditableHeaderIds.contains(dto.getNormsHeaderFkId()))) {
                    skippedCount++;
                    log.info("[JMDCPPNormPrices] Skipping non-editable calculated row from import (edited or not): account={}, normsHeaderFkId={}",
                            dto.getAccountName(), dto.getNormsHeaderFkId());
                    continue;
                }

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

                // Skip unchanged records (dataHash-based change detection)
                if (!isRecordModified(dto, existingHashes)) {
                    skippedCount++;
                    log.debug("[JMDCPPNormPrices] Skipping unchanged record: {}", dto.getNormsHeaderFkId());
                    continue;
                }

                // Validate remarks is mandatory
                if (dto.getRemarks() == null || dto.getRemarks().trim().isEmpty()) {
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription("Remarks field is mandatory and cannot be empty");
                    failedRecords.add(dto);
                    continue;
                }

                // Validate remarks must be different from existing DB value
                String dbRemarks = existingRemarks.getOrDefault(dto.getNormsHeaderFkId(), "");
                String importedRemarks = dto.getRemarks().trim();
                if (dbRemarks.equals(importedRemarks)) {
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription("Remarks must be updated to explain the changes. Current remarks are identical to the database value.");
                    failedRecords.add(dto);
                    continue;
                }

                validRecords.add(convertToRequestDto(dto));
            }

            log.info("[JMDCPPNormPrices] {} unchanged (skipped), {} modified to process",
                    skippedCount, data.size() - skippedCount);

            if (!validRecords.isEmpty()) {
                AOPMessageVM saveResponse = saveOrUpdateCPPNormPrices(plantIds, aopYear, validRecords);

                if (saveResponse.getCode() == 207) {
                    List<String> saveErrorMessages = new ArrayList<>();
                    if (saveResponse.getData() instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> errs = (List<String>) saveResponse.getData();
                        saveErrorMessages = errs;
                    }
                    log.warn("[JMDCPPNormPrices] IMPORT - save returned partial errors: {}", saveErrorMessages);
                } else if (saveResponse.getCode() != 200) {
                    vm.setCode(saveResponse.getCode());
                    vm.setMessage(saveResponse.getMessage());
                    vm.setData(saveResponse.getData());
                    return vm;
                }
            }

            if (!failedRecords.isEmpty()) {
                byte[] errorExcel = generateExcel(failedRecords, aopYear, buildErrorMessages(failedRecords));
                String base64File = Base64.getEncoder().encodeToString(errorExcel);
                vm.setCode(400);
                vm.setData(base64File);
                vm.setMessage("Partial data saved. " + validRecords.size() + " saved, " + failedRecords.size()
                        + " failed, " + skippedCount + " unchanged. Please check the downloaded error file.");
            } else {
                vm.setCode(200);
                if (validRecords.isEmpty() && skippedCount > 0) {
                    vm.setMessage("No changes detected. All " + skippedCount + " records unchanged.");
                } else {
                    vm.setMessage("All data saved. " + validRecords.size() + " updated, " + skippedCount + " unchanged.");
                }
                vm.setData(null);
            }

        } catch (Exception e) {
            log.error("[JMDCPPNormPrices] IMPORT error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
        }

        return vm;
    }

    // ── Mapping ───────────────────────────────────────────────────────────────
    // Maps a raw Object[] row from SP dbo.CPP_JMD_GetCPPNormPrices to the DTO.
    // Column order must match the SP's SELECT clause:
    //   0:id  1:cppMonthWisePriceId  2:cppPlantId  3:cppPlantName  4:normsHeaderFkId
    //   5:generatingPlantName  6:utilityName  7:utilityId  8:uom  9:accountName
    //  10:materialName  11:materialId  12:issuingPlantName  13:issuingUom  14:aopYear
    //  15-26:aprPrice..marPrice
    //  27:remarks  28:priceSource  29:modifiedBy  30:createdDate (skipped)  31:updatedDate (skipped)  32:valueType

    private JMDCPPNormPricesResponseDTO mapRowToDto(Object[] row) {
        JMDCPPNormPricesResponseDTO dto = new JMDCPPNormPricesResponseDTO();

        int idx = 0;
        dto.setId(row[idx++] != null ? ((Number) row[idx - 1]).longValue() : null);
        dto.setCppMonthWisePriceId(toUUIDObj(row[idx++]));
        dto.setCppPlantId(toUUIDObj(row[idx++]));
        dto.setCppPlantName(toStringObj(row[idx++]));
        dto.setNormsHeaderFkId(toUUIDObj(row[idx++]));
        dto.setGeneratingPlantName(toStringObj(row[idx++]));
        dto.setUtilityName(toStringObj(row[idx++]));
        dto.setUtilityId(toStringObj(row[idx++]));
        dto.setUom(toStringObj(row[idx++]));
        dto.setAccountName(toStringObj(row[idx++]));
        dto.setMaterialName(toStringObj(row[idx++]));
        dto.setMaterialId(toStringObj(row[idx++]));
        dto.setIssuingPlantName(toStringObj(row[idx++]));
        dto.setIssuingUom(toStringObj(row[idx++]));
        dto.setAopYear(toStringObj(row[idx++]));

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

        dto.setRemarks(toStringObj(row[idx++]));
        dto.setPriceSource(toStringObj(row[idx++]));
        dto.setModifiedBy(toStringObj(row[idx++]));
        idx++; // skip createdDate (not needed)
        idx++; // skip updatedDate (not needed)
        dto.setValueType(toStringObj(row[idx++]));

        return dto;
    }

    private String toStringObj(Object value) {
        return value == null ? null : value.toString();
    }

    private UUID toUUIDObj(Object value) {
        if (value == null) return null;
        try {
            return UUID.fromString(value.toString());
        } catch (Exception e) {
            return null;
        }
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

    private BigDecimal normalizePrice(BigDecimal price) {
        return price == null ? BigDecimal.ZERO : price;
    }

    private void addMonthUpdate(
            List<Object[]> updates,
            UUID normsHeaderFkId,
            UUID monthId,
            BigDecimal value,
            String valueType) {

        if (normsHeaderFkId == null || monthId == null) {
            return;
        }

        BigDecimal price = BigDecimal.ZERO;
        BigDecimal amount = BigDecimal.ZERO;

        if ("Price".equalsIgnoreCase(valueType)) {
            price = value != null ? value : BigDecimal.ZERO;
        } else if ("Amount".equalsIgnoreCase(valueType)) {
            amount = value != null ? value : BigDecimal.ZERO;
        }

        updates.add(new Object[] { price, amount, normsHeaderFkId, monthId });
    }

    // ── Non-editable Account Check ──────────────────────────────────────────

    private boolean isNonEditableAccount(String accountName, String materialName, String utilityName) {
        return checkNonEditableMatch(accountName)
                || checkNonEditableMatch(materialName)
                || checkNonEditableMatch(utilityName);
    }

    private boolean checkNonEditableMatch(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.trim();
        if (trimmed.equalsIgnoreCase("Raw Material")
                || trimmed.equalsIgnoreCase("Raw Materials")
                || trimmed.equalsIgnoreCase("Electricity Duty (Variable) on Power Generated")
                || trimmed.equalsIgnoreCase("Renewable Power Purchases Obligation")
                || trimmed.equalsIgnoreCase("Renewable Power Purchase Obligation")
                || trimmed.equalsIgnoreCase("ED")
                || trimmed.equalsIgnoreCase("RPO")) {
            return true;
        }
        String lower = trimmed.toLowerCase();
        return lower.contains("raw material")
                || lower.contains("electricity duty")
                || lower.contains("renewable power")
                || lower.contains("purchases obligation")
                || lower.contains("purchase obligation")
                || lower.equals("ed")
                || lower.equals("rpo")
                || lower.startsWith("ed ")
                || lower.startsWith("rpo ");
    }

    private boolean isNonEditableRow(JMDCPPNormPricesResponseDTO dto) {
        return dto != null && isNonEditableAccount(dto.getAccountName(), dto.getMaterialName(), dto.getUtilityName());
    }

    private boolean isNonEditableRow(JMDCPPNormPricesRequestDTO dto) {
        return dto != null && isNonEditableAccount(dto.getAccountName(), dto.getMaterialName(), dto.getUtilityName());
    }

    // ── Hash & Change Detection ───────────────────────────────────────────────

    private String generateHash(JMDCPPNormPricesResponseDTO dto) {
        try {
            StringBuilder dataToHash = new StringBuilder();

            dataToHash.append(normalizeBigDecimalForHash(dto.getAprPrice())).append("|");
            dataToHash.append(normalizeBigDecimalForHash(dto.getMayPrice())).append("|");
            dataToHash.append(normalizeBigDecimalForHash(dto.getJunPrice())).append("|");
            dataToHash.append(normalizeBigDecimalForHash(dto.getJulPrice())).append("|");
            dataToHash.append(normalizeBigDecimalForHash(dto.getAugPrice())).append("|");
            dataToHash.append(normalizeBigDecimalForHash(dto.getSepPrice())).append("|");
            dataToHash.append(normalizeBigDecimalForHash(dto.getOctPrice())).append("|");
            dataToHash.append(normalizeBigDecimalForHash(dto.getNovPrice())).append("|");
            dataToHash.append(normalizeBigDecimalForHash(dto.getDecPrice())).append("|");
            dataToHash.append(normalizeBigDecimalForHash(dto.getJanPrice())).append("|");
            dataToHash.append(normalizeBigDecimalForHash(dto.getFebPrice())).append("|");
            dataToHash.append(normalizeBigDecimalForHash(dto.getMarPrice())).append("|");
            dataToHash.append(dto.getValueType() != null ? dto.getValueType() : "null").append("|");
            dataToHash.append(dto.getPriceSource() != null ? dto.getPriceSource() : "null").append("|");
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
            log.error("[JMDCPPNormPrices] Error generating hash: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * Normalizes a BigDecimal to a canonical string for hashing so that values
     * like "100.0000" (from DB) and "100.0" (after Excel round-trip) produce
     * the same hash. Without this, unchanged rows fail change detection because
     * Excel converts BigDecimal → double → back to BigDecimal with different scale.
     */
    private String normalizeBigDecimalForHash(BigDecimal value) {
        if (value == null) {
            return "null";
        }
        BigDecimal normalized = value.stripTrailingZeros();
        // stripTrailingZeros on "0.00" yields "0E+2" — normalize to "0"
        if (normalized.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }
        return normalized.toPlainString();
    }

    private boolean isRecordModified(JMDCPPNormPricesResponseDTO dto, Map<UUID, String> existingHashes) {
        if (dto.getNormsHeaderFkId() == null) {
            return true;
        }

        String currentHash = generateHash(dto);
        String importedHash = dto.getDataHash();

        if (importedHash != null && !importedHash.isEmpty()) {
            // dataHash available: compare Excel dataHash vs hash of current Excel values
            boolean modified = !importedHash.equals(currentHash);
            if (!modified) {
                log.debug("[JMDCPPNormPrices] Record {} unchanged - dataHash match", dto.getNormsHeaderFkId());
            }
            return modified;
        }

        // dataHash not available: fall back to comparing current Excel values vs DB values
        String dbHash = existingHashes != null ? existingHashes.get(dto.getNormsHeaderFkId()) : null;
        if (dbHash != null) {
            boolean modified = !dbHash.equals(currentHash);
            if (!modified) {
                log.debug("[JMDCPPNormPrices] Record {} unchanged - DB hash match", dto.getNormsHeaderFkId());
            }
            return modified;
        }

        // No dataHash and no DB hash available: treat as modified (backward compatible)
        return true;
    }

    private JMDCPPNormPricesRequestDTO convertToRequestDto(JMDCPPNormPricesResponseDTO dto) {
        JMDCPPNormPricesRequestDTO request = new JMDCPPNormPricesRequestDTO();
        request.setCppMonthWisePriceId(dto.getCppMonthWisePriceId());
        request.setNormsHeaderFkId(dto.getNormsHeaderFkId());
        request.setCppPlantId(dto.getCppPlantId());
        request.setCppPlantName(dto.getCppPlantName());
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
        return request;
    }

    private List<String> buildErrorMessages(List<JMDCPPNormPricesResponseDTO> failedRecords) {
        List<String> messages = new ArrayList<>();
        for (JMDCPPNormPricesResponseDTO dto : failedRecords) {
            messages.add(dto.getErrDescription() != null ? dto.getErrDescription() : "Unknown error");
        }
        return messages;
    }

    // ── Excel Generation ──────────────────────────────────────────────────────

    private byte[] generateExcel(List<JMDCPPNormPricesResponseDTO> dtoList, String aopYear, List<String> errorMessages) throws IOException {
        boolean isErrorExcel = errorMessages != null && !errorMessages.isEmpty();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(isErrorExcel ? "CPP Norm Prices Errors" : "CPP Norm Prices");

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

        String[] baseHeaders = {"CPP Plant", "Generating Plant", "Utility", "Utility ID", "UOM", "Account",
                "Material", "SAP Code", "Issuing Plant", "Issuing UOM", "AOP Year", "Value Type"};
        for (String header : baseHeaders) {
            headerRow.createCell(col).setCellValue(header);
            headerRow.getCell(col++).setCellStyle(headerStyle);
        }

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

        int dataHashCol = col;
        headerRow.createCell(col).setCellValue("dataHash");
        headerRow.getCell(col++).setCellStyle(headerStyle);

        int commentCol = -1;
        if (isErrorExcel) {
            headerRow.createCell(col).setCellValue("Status");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            commentCol = col;
            headerRow.createCell(col).setCellValue("Error Description");
            headerRow.getCell(col++).setCellStyle(headerStyle);
        }

        int totalColumns = col;

        for (int i = 0; i < dtoList.size(); i++) {
            JMDCPPNormPricesResponseDTO dto = dtoList.get(i);
            Row row = sheet.createRow(rowNum++);
            col = 0;

            setStringCellValue(row.createCell(col++), dto.getCppPlantName(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getGeneratingPlantName(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getUtilityName(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getUtilityId(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getUom(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getAccountName(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getMaterialName(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getMaterialId(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getIssuingPlantName(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getIssuingUom(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getAopYear(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getValueType(), lockedStyle);

            boolean nonEditable = isNonEditableRow(dto);
            CellStyle monthStyle = nonEditable ? lockedStyle : unlockedStyle;
            CellStyle rowRemarksStyle = nonEditable ? lockedStyle : remarksStyle;

            setBigDecimalCellValue(row.createCell(monthStartCol + 0), dto.getAprPrice(), monthStyle);
            setBigDecimalCellValue(row.createCell(monthStartCol + 1), dto.getMayPrice(), monthStyle);
            setBigDecimalCellValue(row.createCell(monthStartCol + 2), dto.getJunPrice(), monthStyle);
            setBigDecimalCellValue(row.createCell(monthStartCol + 3), dto.getJulPrice(), monthStyle);
            setBigDecimalCellValue(row.createCell(monthStartCol + 4), dto.getAugPrice(), monthStyle);
            setBigDecimalCellValue(row.createCell(monthStartCol + 5), dto.getSepPrice(), monthStyle);
            setBigDecimalCellValue(row.createCell(monthStartCol + 6), dto.getOctPrice(), monthStyle);
            setBigDecimalCellValue(row.createCell(monthStartCol + 7), dto.getNovPrice(), monthStyle);
            setBigDecimalCellValue(row.createCell(monthStartCol + 8), dto.getDecPrice(), monthStyle);
            setBigDecimalCellValue(row.createCell(monthStartCol + 9), dto.getJanPrice(), monthStyle);
            setBigDecimalCellValue(row.createCell(monthStartCol + 10), dto.getFebPrice(), monthStyle);
            setBigDecimalCellValue(row.createCell(monthStartCol + 11), dto.getMarPrice(), monthStyle);
            col = monthStartCol + 12;

            setStringCellValue(row.createCell(col++), dto.getPriceSource(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getRemarks(), rowRemarksStyle);
            setStringCellValue(row.createCell(col++), dto.getId() != null ? dto.getId().toString() : null, lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getCppMonthWisePriceId() != null ? dto.getCppMonthWisePriceId().toString() : null, lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getNormsHeaderFkId() != null ? dto.getNormsHeaderFkId().toString() : null, lockedStyle);
            setStringCellValue(row.createCell(col++), generateHash(dto), lockedStyle);

            if (isErrorExcel) {
                setStringCellValue(row.createCell(col++), dto.getSaveStatus(), errorStyle);
                setStringCellValue(row.createCell(col++), dto.getErrDescription(), errorStyle);
            }
        }

        sheet.setColumnHidden(idCol, true);
        sheet.setColumnHidden(cppMonthWisePriceIdCol, true);
        sheet.setColumnHidden(normsHeaderFkIdCol, true);
        sheet.setColumnHidden(dataHashCol, true);

        for (int i = 0; i < totalColumns; i++) {
            if (i == remarksCol) {
                sheet.setColumnWidth(i, 8000);
                continue;
            }
            sheet.autoSizeColumn(i);
        }
        if (commentCol >= 0) {
            sheet.setColumnWidth(commentCol, 8000);
        }

        // Protect the sheet so locked/unlocked cell styles take effect.
        // Only the 12 month columns (Apr–Mar) and Remarks are unlocked (editable);
        // all other columns are locked with a grey background.
        ExcelStyles.applySheetProtectionWithAutoFilter(sheet, totalColumns);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    // ── Excel Reading ─────────────────────────────────────────────────────────

    private List<JMDCPPNormPricesResponseDTO> readExcel(InputStream inputStream) {
        List<JMDCPPNormPricesResponseDTO> dataList = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (!rowIterator.hasNext()) {
                return dataList;
            }

            Row headerRow = rowIterator.next();
            int numCols = headerRow.getLastCellNum();

            // Base columns order: 0=CPP Plant, 1=Generating Plant, 2=Utility, 3=Utility ID, 4=UOM, 5=Account,
            // 6=Material, 7=SAP Code, 8=Issuing Plant, 9=Issuing UOM, 10=AOP Year, 11=Value Type
            // → monthStartIdx = 12
            int monthStartIdx = 12;

            int idColIdx = -1;
            int remarksColIdx = -1;
            int normsHeaderFkIdColIdx = -1;
            int dataHashColIdx = -1;
            int priceSourceColIdx = -1;

            for (int i = 0; i < numCols; i++) {
                Cell cell = headerRow.getCell(i);
                if (cell == null) continue;
                String header = cell.getStringCellValue().trim().toLowerCase();
                if (header.equals("id")) {
                    idColIdx = i;
                } else if (header.equals("remarks")) {
                    remarksColIdx = i;
                } else if (header.equals("normsheaderfkid")) {
                    normsHeaderFkIdColIdx = i;
                } else if (header.equals("datahash")) {
                    dataHashColIdx = i;
                } else if (header.equals("price source")) {
                    priceSourceColIdx = i;
                }
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (row.getLastCellNum() < monthStartIdx) continue;

                JMDCPPNormPricesResponseDTO dto = new JMDCPPNormPricesResponseDTO();

                try {
                    // Parse base text columns positionally
                    dto.setCppPlantName(getStringCellValue(row.getCell(0)));
                    dto.setGeneratingPlantName(getStringCellValue(row.getCell(1)));
                    dto.setUtilityName(getStringCellValue(row.getCell(2)));
                    dto.setUtilityId(getStringCellValue(row.getCell(3)));
                    dto.setUom(getStringCellValue(row.getCell(4)));
                    dto.setAccountName(getStringCellValue(row.getCell(5)));
                    dto.setMaterialName(getStringCellValue(row.getCell(6)));
                    dto.setMaterialId(getStringCellValue(row.getCell(7)));
                    dto.setIssuingPlantName(getStringCellValue(row.getCell(8)));
                    dto.setIssuingUom(getStringCellValue(row.getCell(9)));
                    dto.setAopYear(getStringCellValue(row.getCell(10)));
                    dto.setValueType(getStringCellValue(row.getCell(11)));

                    // Parse monthly columns positionally
                    if (numCols > monthStartIdx + 0) dto.setAprPrice(getBigDecimalCellValue(row.getCell(monthStartIdx + 0)));
                    if (numCols > monthStartIdx + 1) dto.setMayPrice(getBigDecimalCellValue(row.getCell(monthStartIdx + 1)));
                    if (numCols > monthStartIdx + 2) dto.setJunPrice(getBigDecimalCellValue(row.getCell(monthStartIdx + 2)));
                    if (numCols > monthStartIdx + 3) dto.setJulPrice(getBigDecimalCellValue(row.getCell(monthStartIdx + 3)));
                    if (numCols > monthStartIdx + 4) dto.setAugPrice(getBigDecimalCellValue(row.getCell(monthStartIdx + 4)));
                    if (numCols > monthStartIdx + 5) dto.setSepPrice(getBigDecimalCellValue(row.getCell(monthStartIdx + 5)));
                    if (numCols > monthStartIdx + 6) dto.setOctPrice(getBigDecimalCellValue(row.getCell(monthStartIdx + 6)));
                    if (numCols > monthStartIdx + 7) dto.setNovPrice(getBigDecimalCellValue(row.getCell(monthStartIdx + 7)));
                    if (numCols > monthStartIdx + 8) dto.setDecPrice(getBigDecimalCellValue(row.getCell(monthStartIdx + 8)));
                    if (numCols > monthStartIdx + 9) dto.setJanPrice(getBigDecimalCellValue(row.getCell(monthStartIdx + 9)));
                    if (numCols > monthStartIdx + 10) dto.setFebPrice(getBigDecimalCellValue(row.getCell(monthStartIdx + 10)));
                    if (numCols > monthStartIdx + 11) dto.setMarPrice(getBigDecimalCellValue(row.getCell(monthStartIdx + 11)));

                    // Parse header-detected columns
                    if (priceSourceColIdx >= 0) {
                        dto.setPriceSource(getStringCellValue(row.getCell(priceSourceColIdx)));
                    }
                    if (remarksColIdx >= 0) {
                        dto.setRemarks(getStringCellValue(row.getCell(remarksColIdx)));
                    }
                    if (idColIdx >= 0) {
                        String idStr = getStringCellValue(row.getCell(idColIdx));
                        if (idStr != null && !idStr.isEmpty()) {
                            try { dto.setId(Long.parseLong(idStr)); } catch (Exception ignored) {}
                        }
                    }
                    if (normsHeaderFkIdColIdx >= 0) {
                        String normsHeaderFkIdStr = getStringCellValue(row.getCell(normsHeaderFkIdColIdx));
                        if (normsHeaderFkIdStr != null && !normsHeaderFkIdStr.isEmpty()) {
                            try { dto.setNormsHeaderFkId(UUID.fromString(normsHeaderFkIdStr)); } catch (Exception ignored) {}
                        }
                    }
                    if (dataHashColIdx >= 0) {
                        dto.setDataHash(getStringCellValue(row.getCell(dataHashColIdx)));
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
            log.error("[JMDCPPNormPrices] Error reading Excel file", e);
        }

        return dataList;
    }

    // ── Cell Helpers ──────────────────────────────────────────────────────────

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

    /*********************************************************************************/
    // ED RPO Actual Rate implementations

    // ── GET EDRPO ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM getActualRateEDRPO(String aopYear) {
        log.info("[CPPActualRateEDRPO] GET - aopYear: {}", aopYear);
        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (aopYear == null || aopYear.trim().isEmpty()) {
                vm.setCode(400);
                vm.setMessage("aopYear cannot be null or empty");
                vm.setData(new ArrayList<>());
                return vm;
            }

            StoredProcedureQuery sp = entityManager
                    .createStoredProcedureQuery("dbo.CPP_GetActualRateEDRPO")
                    .registerStoredProcedureParameter("AOPYear", String.class, ParameterMode.IN);

            sp.setParameter("AOPYear", aopYear);

            log.info("Executing stored procedure dbo.CPP_GetActualRateEDRPO for aopYear: {}", aopYear);
            sp.execute();

            @SuppressWarnings("unchecked")
            List<Object[]> rawResults = sp.getResultList();
            log.info("[CPPActualRateEDRPO] GET - SP returned {} records", rawResults.size());

            List<CPPActualRateEDRPODTO> dtoList = new ArrayList<>();
            for (Object[] row : rawResults) {
                dtoList.add(mapRowToEDRPODto(row));
            }

            // Generate dataHash for change detection
            for (CPPActualRateEDRPODTO dto : dtoList) {
                dto.setDataHash(generateEDRPOHash(dto));
            }

            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(dtoList);

        } catch (Exception e) {
            log.error("[CPPActualRateEDRPO] GET error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
        }

        return vm;
    }

    // ── UPDATE EDRPO ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM updateActualRateEDRPO(String aopYear, List<CPPActualRateEDRPORequestDTO> dtoList) {
        log.info("[CPPActualRateEDRPO] UPDATE - aopYear: {}, records: {}",
                aopYear, dtoList != null ? dtoList.size() : 0);
        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (dtoList == null || dtoList.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("Request body cannot be empty");
                return vm;
            }

            if (aopYear == null || aopYear.trim().isEmpty()) {
                vm.setCode(400);
                vm.setMessage("aopYear cannot be null or empty");
                return vm;
            }

            String updateSql = """
                UPDATE dbo.CPPActualRateEDRPO
                SET Apr = ?, May = ?, Jun = ?, Jul = ?, Aug = ?, Sep = ?,
                    Oct = ?, Nov = ?, [Dec] = ?, Jan = ?, Feb = ?, Mar = ?,
                    Remarks = ?, ModifiedBy = ?, UpdatedDate = SYSDATETIME()
                WHERE Id = ? AND AOPYear = ?
                """;

            List<Object[]> updateParams = new ArrayList<>();
            List<String> errorMessages = new ArrayList<>();

            for (CPPActualRateEDRPORequestDTO dto : dtoList) {
                if (dto == null || dto.getId() == null) {
                    errorMessages.add("Record id is missing; skipping");
                    continue;
                }
                updateParams.add(new Object[]{
                        normalizePrice(dto.getApr()),
                        normalizePrice(dto.getMay()),
                        normalizePrice(dto.getJun()),
                        normalizePrice(dto.getJul()),
                        normalizePrice(dto.getAug()),
                        normalizePrice(dto.getSep()),
                        normalizePrice(dto.getOct()),
                        normalizePrice(dto.getNov()),
                        normalizePrice(dto.getDec()),
                        normalizePrice(dto.getJan()),
                        normalizePrice(dto.getFeb()),
                        normalizePrice(dto.getMar()),
                        dto.getRemarks(),
                        (dto.getModifiedBy() != null && !dto.getModifiedBy().trim().isEmpty())
                                ? dto.getModifiedBy().trim()
                                : "SYSTEM",
                        dto.getId(),
                        aopYear
                });
            }

            if (updateParams.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("No valid records to update");
                vm.setData(errorMessages);
                return vm;
            }

            jdbcTemplate.batchUpdate(updateSql, updateParams);

            if (!errorMessages.isEmpty()) {
                vm.setCode(207);
                vm.setMessage("Partial data processed with validation errors");
                vm.setData(errorMessages);
            } else {
                vm.setCode(200);
                vm.setMessage(String.format("Successfully updated %d record(s)", updateParams.size()));
                vm.setData(null);
            }

        } catch (Exception e) {
            log.error("[CPPActualRateEDRPO] UPDATE error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(null);
        }

        return vm;
    }

    // ── EXPORT EDRPO ──────────────────────────────────────────────────────────

    @Override
    public byte[] exportActualRateEDRPO(String aopYear) throws IOException {
        log.info("[CPPActualRateEDRPO] EXPORT - aopYear: {}", aopYear);

        try {
            AOPMessageVM result = getActualRateEDRPO(aopYear);

            List<CPPActualRateEDRPODTO> dtoList = new ArrayList<>();
            if (result.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<CPPActualRateEDRPODTO> data = (List<CPPActualRateEDRPODTO>) result.getData();
                dtoList = data;
            }

            if (dtoList.isEmpty()) {
                log.warn("[CPPActualRateEDRPO] EXPORT - no data found for aopYear: {}", aopYear);
            }

            return generateEDRPOExcel(dtoList, aopYear, null);

        } catch (IOException e) {
            log.error("[CPPActualRateEDRPO] EXPORT IOException: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("[CPPActualRateEDRPO] EXPORT error: {}", e.getMessage(), e);
            throw new IOException("Failed to export CPP Actual Rate EDRPO: " + e.getMessage(), e);
        }
    }

    // ── IMPORT EDRPO ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM importActualRateEDRPO(String aopYear, MultipartFile file) throws IOException {
        log.info("[CPPActualRateEDRPO] IMPORT - aopYear: {}, file: {}", aopYear, file.getOriginalFilename());

        AOPMessageVM vm = new AOPMessageVM();

        try (InputStream inputStream = file.getInputStream()) {
            List<CPPActualRateEDRPODTO> data = readEDRPOExcel(inputStream);
            log.info("[CPPActualRateEDRPO] IMPORT - read {} records from Excel", data.size());

            // Fetch existing data for change detection
            AOPMessageVM existingData = getActualRateEDRPO(aopYear);
            Map<UUID, String> existingHashes = new HashMap<>();
            Map<UUID, String> existingRemarks = new HashMap<>();
            if (existingData.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<CPPActualRateEDRPODTO> existingList = (List<CPPActualRateEDRPODTO>) existingData.getData();
                for (CPPActualRateEDRPODTO existing : existingList) {
                    if (existing.getId() != null) {
                        existingHashes.put(existing.getId(), existing.getDataHash());
                        existingRemarks.put(existing.getId(),
                                existing.getRemarks() != null ? existing.getRemarks().trim() : "");
                    }
                }
            }

            List<CPPActualRateEDRPORequestDTO> validRecords = new ArrayList<>();
            List<CPPActualRateEDRPODTO> failedRecords = new ArrayList<>();
            int skippedCount = 0;

            for (CPPActualRateEDRPODTO dto : data) {
                if (dto.getSaveStatus() != null && dto.getSaveStatus().equalsIgnoreCase("Failed")) {
                    failedRecords.add(dto);
                    continue;
                }

                if (dto.getId() == null) {
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription("Id is missing");
                    failedRecords.add(dto);
                    continue;
                }

                // Skip unchanged records (hash-based change detection)
                String currentHash = generateEDRPOHash(dto);
                String importedHash = dto.getDataHash();
                String dbHash = existingHashes.get(dto.getId());

                boolean isModified;
                if (importedHash != null && !importedHash.isEmpty()) {
                    isModified = !importedHash.equals(currentHash);
                } else if (dbHash != null) {
                    isModified = !dbHash.equals(currentHash);
                } else {
                    isModified = true;
                }

                if (!isModified) {
                    skippedCount++;
                    log.debug("[CPPActualRateEDRPO] Skipping unchanged record: {}", dto.getId());
                    continue;
                }

                // Validate remarks is mandatory and must differ from DB value
                if (dto.getRemarks() == null || dto.getRemarks().trim().isEmpty()) {
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription("Remarks field is mandatory and cannot be empty");
                    failedRecords.add(dto);
                    continue;
                }

                String dbRemarks = existingRemarks.getOrDefault(dto.getId(), "");
                if (dbRemarks.equals(dto.getRemarks().trim())) {
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription("Remarks must be updated to explain the changes. Current remarks are identical to the database value.");
                    failedRecords.add(dto);
                    continue;
                }

                validRecords.add(convertEDRPOToRequestDto(dto));
            }

            log.info("[CPPActualRateEDRPO] {} unchanged (skipped), {} modified to process",
                    skippedCount, data.size() - skippedCount);

            if (!validRecords.isEmpty()) {
                AOPMessageVM saveResponse = updateActualRateEDRPO(aopYear, validRecords);
                if (saveResponse.getCode() != 200 && saveResponse.getCode() != 207) {
                    vm.setCode(saveResponse.getCode());
                    vm.setMessage(saveResponse.getMessage());
                    vm.setData(saveResponse.getData());
                    return vm;
                }
            }

            if (!failedRecords.isEmpty()) {
                byte[] errorExcel = generateEDRPOExcel(failedRecords, aopYear, buildEDRPOErrorMessages(failedRecords));
                String base64File = Base64.getEncoder().encodeToString(errorExcel);
                vm.setCode(400);
                vm.setData(base64File);
                vm.setMessage("Partial data saved. " + validRecords.size() + " saved, " + failedRecords.size()
                        + " failed, " + skippedCount + " unchanged. Please check the downloaded error file.");
            } else {
                vm.setCode(200);
                if (validRecords.isEmpty() && skippedCount > 0) {
                    vm.setMessage("No changes detected. All " + skippedCount + " records unchanged.");
                } else {
                    vm.setMessage("All data saved. " + validRecords.size() + " updated, " + skippedCount + " unchanged.");
                }
                vm.setData(null);
            }

        } catch (Exception e) {
            log.error("[CPPActualRateEDRPO] IMPORT error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
        }

        return vm;
    }

    // ── EDRPO Mapping ─────────────────────────────────────────────────────────
    // SP dbo.CPP_GetActualRateEDRPO SELECT column order:
    //   0:id  1:cppPlantFkId  2:cppPlantName  3:accountName  4:displayName  5:rate  6:uom
    //   7:apr  8:may  9:jun  10:jul  11:aug  12:sep
    //  13:oct  14:nov  15:dec  16:jan  17:feb  18:mar
    //  19:aopYear  20:remarks  21:createdDate  22:updatedDate  23:modifiedBy

    private CPPActualRateEDRPODTO mapRowToEDRPODto(Object[] row) {
        CPPActualRateEDRPODTO dto = new CPPActualRateEDRPODTO();
        int idx = 0;
        dto.setId(toUUIDObj(row[idx++]));
        dto.setCppPlantFkId(toUUIDObj(row[idx++]));
        dto.setCppPlantName(toStringObj(row[idx++]));
        dto.setAccountName(toStringObj(row[idx++]));
        dto.setDisplayName(toStringObj(row[idx++]));
        dto.setRate(toStringObj(row[idx++]));
        dto.setUom(toStringObj(row[idx++]));
        dto.setApr(getBigDecimalOrZero(row[idx++]));
        dto.setMay(getBigDecimalOrZero(row[idx++]));
        dto.setJun(getBigDecimalOrZero(row[idx++]));
        dto.setJul(getBigDecimalOrZero(row[idx++]));
        dto.setAug(getBigDecimalOrZero(row[idx++]));
        dto.setSep(getBigDecimalOrZero(row[idx++]));
        dto.setOct(getBigDecimalOrZero(row[idx++]));
        dto.setNov(getBigDecimalOrZero(row[idx++]));
        dto.setDec(getBigDecimalOrZero(row[idx++]));
        dto.setJan(getBigDecimalOrZero(row[idx++]));
        dto.setFeb(getBigDecimalOrZero(row[idx++]));
        dto.setMar(getBigDecimalOrZero(row[idx++]));
        dto.setAopYear(toStringObj(row[idx++]));
        dto.setRemarks(toStringObj(row[idx++]));
        dto.setCreatedDate(toStringObj(row[idx++]));
        dto.setUpdatedDate(toStringObj(row[idx++]));
        dto.setModifiedBy(toStringObj(row[idx++]));
        return dto;
    }

    private CPPActualRateEDRPORequestDTO convertEDRPOToRequestDto(CPPActualRateEDRPODTO dto) {
        CPPActualRateEDRPORequestDTO request = new CPPActualRateEDRPORequestDTO();
        request.setId(dto.getId());
        request.setCppPlantFkId(dto.getCppPlantFkId());
        request.setCppPlantName(dto.getCppPlantName());
        request.setAccountName(dto.getAccountName());
        request.setDisplayName(dto.getDisplayName());
        request.setRate(dto.getRate());
        request.setUom(dto.getUom());
        request.setApr(dto.getApr());
        request.setMay(dto.getMay());
        request.setJun(dto.getJun());
        request.setJul(dto.getJul());
        request.setAug(dto.getAug());
        request.setSep(dto.getSep());
        request.setOct(dto.getOct());
        request.setNov(dto.getNov());
        request.setDec(dto.getDec());
        request.setJan(dto.getJan());
        request.setFeb(dto.getFeb());
        request.setMar(dto.getMar());
        request.setAopYear(dto.getAopYear());
        request.setRemarks(dto.getRemarks());
        request.setModifiedBy(dto.getModifiedBy());
        return request;
    }

    // ── EDRPO Hash ────────────────────────────────────────────────────────────

    private String generateEDRPOHash(CPPActualRateEDRPODTO dto) {
        try {
            StringBuilder dataToHash = new StringBuilder();
            dataToHash.append(normalizeBigDecimalForHash(dto.getApr())).append("|");
            dataToHash.append(normalizeBigDecimalForHash(dto.getMay())).append("|");
            dataToHash.append(normalizeBigDecimalForHash(dto.getJun())).append("|");
            dataToHash.append(normalizeBigDecimalForHash(dto.getJul())).append("|");
            dataToHash.append(normalizeBigDecimalForHash(dto.getAug())).append("|");
            dataToHash.append(normalizeBigDecimalForHash(dto.getSep())).append("|");
            dataToHash.append(normalizeBigDecimalForHash(dto.getOct())).append("|");
            dataToHash.append(normalizeBigDecimalForHash(dto.getNov())).append("|");
            dataToHash.append(normalizeBigDecimalForHash(dto.getDec())).append("|");
            dataToHash.append(normalizeBigDecimalForHash(dto.getJan())).append("|");
            dataToHash.append(normalizeBigDecimalForHash(dto.getFeb())).append("|");
            dataToHash.append(normalizeBigDecimalForHash(dto.getMar())).append("|");
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
            log.error("[CPPActualRateEDRPO] Error generating hash: {}", e.getMessage(), e);
            return "";
        }
    }

    private List<String> buildEDRPOErrorMessages(List<CPPActualRateEDRPODTO> failedRecords) {
        List<String> messages = new ArrayList<>();
        for (CPPActualRateEDRPODTO dto : failedRecords) {
            messages.add(dto.getErrDescription() != null ? dto.getErrDescription() : "Unknown error");
        }
        return messages;
    }

    // ── EDRPO Excel Generation ────────────────────────────────────────────────

    private byte[] generateEDRPOExcel(List<CPPActualRateEDRPODTO> dtoList, String aopYear,
                                       List<String> errorMessages) throws IOException {
        boolean isErrorExcel = errorMessages != null && !errorMessages.isEmpty();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(isErrorExcel ? "Actual Rate EDRPO Errors" : "Actual Rate EDRPO");

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(workbook);
        CellStyle lockedStyle = ExcelStyles.createLockedStyle(workbook);
        CellStyle unlockedStyle = ExcelStyles.createUnlockedStyle(workbook);
        CellStyle remarksStyle = ExcelStyles.createEditableRemarksStyle(workbook);
        CellStyle errorStyle = isErrorExcel ? ExcelStyles.createErrorStyle(workbook) : null;

        String startYearSuffix = aopYear.length() >= 4 ? aopYear.substring(2, 4) : "";
        String endYearSuffix = aopYear.length() >= 7 ? aopYear.substring(5, 7) : "";
        String[] months = {
            "Apr-" + startYearSuffix, "May-" + startYearSuffix, "Jun-" + startYearSuffix,
            "Jul-" + startYearSuffix, "Aug-" + startYearSuffix, "Sep-" + startYearSuffix,
            "Oct-" + startYearSuffix, "Nov-" + startYearSuffix, "Dec-" + startYearSuffix,
            "Jan-" + endYearSuffix, "Feb-" + endYearSuffix, "Mar-" + endYearSuffix
        };

        int rowNum = 0;
        int col = 0;
        Row headerRow = sheet.createRow(rowNum++);

        // Locked base columns
        String[] baseHeaders = {"Plant Name", "Account Name", "Display Name", "Rate", "UOM", "AOP Year"};
        for (String header : baseHeaders) {
            headerRow.createCell(col).setCellValue(header);
            headerRow.getCell(col++).setCellStyle(headerStyle);
        }

        // Editable month columns
        int monthStartCol = col;
        for (String month : months) {
            headerRow.createCell(col).setCellValue(month);
            headerRow.getCell(col++).setCellStyle(headerStyle);
        }

        // Remarks (editable)
        int remarksCol = col;
        headerRow.createCell(col).setCellValue("Remarks");
        headerRow.getCell(col++).setCellStyle(headerStyle);

        // Hidden identifier columns
        int idCol = col;
        headerRow.createCell(col).setCellValue("id");
        headerRow.getCell(col++).setCellStyle(headerStyle);

        int cppPlantFkIdCol = col;
        headerRow.createCell(col).setCellValue("cppPlantFkId");
        headerRow.getCell(col++).setCellStyle(headerStyle);

        int dataHashCol = col;
        headerRow.createCell(col).setCellValue("dataHash");
        headerRow.getCell(col++).setCellStyle(headerStyle);

        int commentCol = -1;
        if (isErrorExcel) {
            headerRow.createCell(col).setCellValue("Status");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            commentCol = col;
            headerRow.createCell(col).setCellValue("Error Description");
            headerRow.getCell(col++).setCellStyle(headerStyle);
        }

        int totalColumns = col;

        for (CPPActualRateEDRPODTO dto : dtoList) {
            Row row = sheet.createRow(rowNum++);
            col = 0;

            setStringCellValue(row.createCell(col++), dto.getCppPlantName(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getAccountName(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getDisplayName(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getRate(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getUom(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getAopYear(), lockedStyle);

            setBigDecimalCellValue(row.createCell(monthStartCol + 0), dto.getApr(), unlockedStyle);
            setBigDecimalCellValue(row.createCell(monthStartCol + 1), dto.getMay(), unlockedStyle);
            setBigDecimalCellValue(row.createCell(monthStartCol + 2), dto.getJun(), unlockedStyle);
            setBigDecimalCellValue(row.createCell(monthStartCol + 3), dto.getJul(), unlockedStyle);
            setBigDecimalCellValue(row.createCell(monthStartCol + 4), dto.getAug(), unlockedStyle);
            setBigDecimalCellValue(row.createCell(monthStartCol + 5), dto.getSep(), unlockedStyle);
            setBigDecimalCellValue(row.createCell(monthStartCol + 6), dto.getOct(), unlockedStyle);
            setBigDecimalCellValue(row.createCell(monthStartCol + 7), dto.getNov(), unlockedStyle);
            setBigDecimalCellValue(row.createCell(monthStartCol + 8), dto.getDec(), unlockedStyle);
            setBigDecimalCellValue(row.createCell(monthStartCol + 9), dto.getJan(), unlockedStyle);
            setBigDecimalCellValue(row.createCell(monthStartCol + 10), dto.getFeb(), unlockedStyle);
            setBigDecimalCellValue(row.createCell(monthStartCol + 11), dto.getMar(), unlockedStyle);
            col = monthStartCol + 12;

            setStringCellValue(row.createCell(col++), dto.getRemarks(), remarksStyle);
            setStringCellValue(row.createCell(col++), dto.getId() != null ? dto.getId().toString() : null, lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getCppPlantFkId() != null ? dto.getCppPlantFkId().toString() : null, lockedStyle);
            setStringCellValue(row.createCell(col++), generateEDRPOHash(dto), lockedStyle);

            if (isErrorExcel) {
                setStringCellValue(row.createCell(col++), dto.getSaveStatus(), errorStyle);
                setStringCellValue(row.createCell(col++), dto.getErrDescription(), errorStyle);
            }
        }

        sheet.setColumnHidden(idCol, true);
        sheet.setColumnHidden(cppPlantFkIdCol, true);
        sheet.setColumnHidden(dataHashCol, true);

        for (int i = 0; i < totalColumns; i++) {
            if (i == remarksCol) {
                sheet.setColumnWidth(i, 8000);
                continue;
            }
            sheet.autoSizeColumn(i);
        }
        if (commentCol >= 0) {
            sheet.setColumnWidth(commentCol, 8000);
        }

        ExcelStyles.applySheetProtectionWithAutoFilter(sheet, totalColumns);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    // ── EDRPO Excel Reading ───────────────────────────────────────────────────

    private List<CPPActualRateEDRPODTO> readEDRPOExcel(InputStream inputStream) {
        List<CPPActualRateEDRPODTO> dataList = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (!rowIterator.hasNext()) {
                return dataList;
            }

            Row headerRow = rowIterator.next();
            int numCols = headerRow.getLastCellNum();

            // Detect if first column is Plant Name
            int monthStartIdx = 5;
            boolean hasPlantCol = false;
            Cell firstCell = headerRow.getCell(0);
            if (firstCell != null) {
                String firstHeader = firstCell.getStringCellValue();
                if (firstHeader != null && firstHeader.trim().toLowerCase().contains("plant")) {
                    hasPlantCol = true;
                    monthStartIdx = 6;
                }
            }

            int remarksColIdx = -1;
            int idColIdx = -1;
            int cppPlantFkIdColIdx = -1;
            int dataHashColIdx = -1;

            for (int i = 0; i < numCols; i++) {
                Cell cell = headerRow.getCell(i);
                if (cell == null) continue;
                String header = cell.getStringCellValue().trim().toLowerCase();
                switch (header) {
                    case "remarks"      -> remarksColIdx = i;
                    case "id"           -> idColIdx = i;
                    case "cppplantfkid" -> cppPlantFkIdColIdx = i;
                    case "datahash"     -> dataHashColIdx = i;
                    default -> { /* ignore */ }
                }
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (row.getLastCellNum() < monthStartIdx) continue;

                CPPActualRateEDRPODTO dto = new CPPActualRateEDRPODTO();
                try {
                    int baseIdx = 0;
                    if (hasPlantCol) {
                        dto.setCppPlantName(getStringCellValue(row.getCell(baseIdx++)));
                    }
                    dto.setAccountName(getStringCellValue(row.getCell(baseIdx++)));
                    dto.setDisplayName(getStringCellValue(row.getCell(baseIdx++)));
                    dto.setRate(getStringCellValue(row.getCell(baseIdx++)));
                    dto.setUom(getStringCellValue(row.getCell(baseIdx++)));
                    dto.setAopYear(getStringCellValue(row.getCell(baseIdx++)));

                    if (numCols > monthStartIdx + 0)  dto.setApr(getBigDecimalCellValue(row.getCell(monthStartIdx)));
                    if (numCols > monthStartIdx + 1)  dto.setMay(getBigDecimalCellValue(row.getCell(monthStartIdx + 1)));
                    if (numCols > monthStartIdx + 2)  dto.setJun(getBigDecimalCellValue(row.getCell(monthStartIdx + 2)));
                    if (numCols > monthStartIdx + 3)  dto.setJul(getBigDecimalCellValue(row.getCell(monthStartIdx + 3)));
                    if (numCols > monthStartIdx + 4)  dto.setAug(getBigDecimalCellValue(row.getCell(monthStartIdx + 4)));
                    if (numCols > monthStartIdx + 5)  dto.setSep(getBigDecimalCellValue(row.getCell(monthStartIdx + 5)));
                    if (numCols > monthStartIdx + 6)  dto.setOct(getBigDecimalCellValue(row.getCell(monthStartIdx + 6)));
                    if (numCols > monthStartIdx + 7)  dto.setNov(getBigDecimalCellValue(row.getCell(monthStartIdx + 7)));
                    if (numCols > monthStartIdx + 8)  dto.setDec(getBigDecimalCellValue(row.getCell(monthStartIdx + 8)));
                    if (numCols > monthStartIdx + 9)  dto.setJan(getBigDecimalCellValue(row.getCell(monthStartIdx + 9)));
                    if (numCols > monthStartIdx + 10) dto.setFeb(getBigDecimalCellValue(row.getCell(monthStartIdx + 10)));
                    if (numCols > monthStartIdx + 11) dto.setMar(getBigDecimalCellValue(row.getCell(monthStartIdx + 11)));

                    if (remarksColIdx >= 0)      dto.setRemarks(getStringCellValue(row.getCell(remarksColIdx)));
                    if (dataHashColIdx >= 0)     dto.setDataHash(getStringCellValue(row.getCell(dataHashColIdx)));
                    if (idColIdx >= 0) {
                        String idStr = getStringCellValue(row.getCell(idColIdx));
                        if (idStr != null && !idStr.isEmpty()) {
                            try { dto.setId(UUID.fromString(idStr)); } catch (Exception ignored) {}
                        }
                    }
                    if (cppPlantFkIdColIdx >= 0) {
                        String fkIdStr = getStringCellValue(row.getCell(cppPlantFkIdColIdx));
                        if (fkIdStr != null && !fkIdStr.isEmpty()) {
                            try { dto.setCppPlantFkId(UUID.fromString(fkIdStr)); } catch (Exception ignored) {}
                        }
                    }

                    if (dto.getId() == null) {
                        dto.setSaveStatus("Failed");
                        dto.setErrDescription("Id is missing");
                    }

                } catch (Exception e) {
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription(e.getMessage());
                }

                dataList.add(dto);
            }
        } catch (Exception e) {
            log.error("[CPPActualRateEDRPO] Error reading Excel file", e);
        }

        return dataList;
    }

    /*********************************************************************************/
    // ED RPO Mapping implementations

    // ── GET EDRPO MAPPING ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM getEdRpoMapping(List<UUID> plantIds, String aopYear) {
        log.info("[CPPEdRpoMapping] GET - plantIds: {}, aopYear: {}", plantIds, aopYear);
        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (plantIds == null || plantIds.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("plantIds cannot be null or empty");
                vm.setData(new ArrayList<>());
                return vm;
            }

            if (aopYear == null || aopYear.trim().isEmpty()) {
                vm.setCode(400);
                vm.setMessage("aopYear cannot be null or empty");
                vm.setData(new ArrayList<>());
                return vm;
            }

            String plantIdsStr = plantIds.stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(","));

            StoredProcedureQuery sp = entityManager
                    .createStoredProcedureQuery("dbo.CPP_GetOrCreateEdRpoMapping")
                    .registerStoredProcedureParameter("CPPPlantIds", String.class, ParameterMode.IN)
                    .registerStoredProcedureParameter("AOPYear", String.class, ParameterMode.IN);

            sp.setParameter("CPPPlantIds", plantIdsStr);
            sp.setParameter("AOPYear", aopYear);

            log.info("Executing stored procedure dbo.CPP_GetOrCreateEdRpoMapping for plantIds: {}, aopYear: {}", plantIdsStr, aopYear);
            sp.execute();

            @SuppressWarnings("unchecked")
            List<Object[]> rawResults = sp.getResultList();
            log.info("[CPPEdRpoMapping] GET - SP returned {} records", rawResults.size());

            List<CPPEdRpoMappingDTO> dtoList = new ArrayList<>();
            for (Object[] row : rawResults) {
                dtoList.add(mapRowToEdRpoMappingDto(row));
            }

            // Generate dataHash for change detection
            for (CPPEdRpoMappingDTO dto : dtoList) {
                dto.setDataHash(generateEdRpoMappingHash(dto));
            }

            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(dtoList);

        } catch (Exception e) {
            log.error("[CPPEdRpoMapping] GET error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
        }

        return vm;
    }

    // ── UPDATE EDRPO MAPPING ──────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM updateEdRpoMapping(List<UUID> plantIds, String aopYear, List<CPPEdRpoMappingRequestDTO> dtoList) {
        log.info("[CPPEdRpoMapping] UPDATE - plantIds: {}, aopYear: {}, records: {}",
                plantIds, aopYear, dtoList != null ? dtoList.size() : 0);
        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (dtoList == null || dtoList.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("Request body cannot be empty");
                return vm;
            }

            if (aopYear == null || aopYear.trim().isEmpty()) {
                vm.setCode(400);
                vm.setMessage("aopYear cannot be null or empty");
                return vm;
            }

            String updateSql = """
                UPDATE dbo.CPPEdRpoMapping
                SET Exempt = ?, Remark = ?
                WHERE Id = ? AND AOPYear = ?
                """;

            List<Object[]> updateParams = new ArrayList<>();
            List<String> errorMessages = new ArrayList<>();

            for (CPPEdRpoMappingRequestDTO dto : dtoList) {
                if (dto == null || dto.getId() == null) {
                    errorMessages.add("Record id is missing; skipping");
                    continue;
                }
                updateParams.add(new Object[]{
                        Boolean.TRUE.equals(dto.getExempt()) ? 1 : 0,
                        dto.getRemark(),
                        dto.getId(),
                        aopYear
                });
            }

            if (updateParams.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("No valid records to update");
                vm.setData(errorMessages);
                return vm;
            }

            jdbcTemplate.batchUpdate(updateSql, updateParams);

            if (!errorMessages.isEmpty()) {
                vm.setCode(207);
                vm.setMessage("Partial data processed with validation errors");
                vm.setData(errorMessages);
            } else {
                vm.setCode(200);
                vm.setMessage(String.format("Successfully updated %d record(s)", updateParams.size()));
                vm.setData(null);
            }

        } catch (Exception e) {
            log.error("[CPPEdRpoMapping] UPDATE error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(null);
        }

        return vm;
    }

    // ── EXPORT EDRPO MAPPING ──────────────────────────────────────────────────

    @Override
    public byte[] exportEdRpoMapping(List<UUID> plantIds, String aopYear) throws IOException {
        log.info("[CPPEdRpoMapping] EXPORT - plantIds: {}, aopYear: {}", plantIds, aopYear);

        try {
            AOPMessageVM result = getEdRpoMapping(plantIds, aopYear);

            List<CPPEdRpoMappingDTO> dtoList = new ArrayList<>();
            if (result.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<CPPEdRpoMappingDTO> data = (List<CPPEdRpoMappingDTO>) result.getData();
                dtoList = data;
            }

            if (dtoList.isEmpty()) {
                log.warn("[CPPEdRpoMapping] EXPORT - no data found for plantIds: {}, aopYear: {}", plantIds, aopYear);
            }

            return generateEdRpoMappingExcel(dtoList, aopYear, null);

        } catch (IOException e) {
            log.error("[CPPEdRpoMapping] EXPORT IOException: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("[CPPEdRpoMapping] EXPORT error: {}", e.getMessage(), e);
            throw new IOException("Failed to export CPP ED RPO Mapping: " + e.getMessage(), e);
        }
    }

    // ── IMPORT EDRPO MAPPING ──────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM importEdRpoMapping(List<UUID> plantIds, String aopYear, MultipartFile file) throws IOException {
        log.info("[CPPEdRpoMapping] IMPORT - plantIds: {}, aopYear: {}, file: {}",
                plantIds, aopYear, file.getOriginalFilename());

        AOPMessageVM vm = new AOPMessageVM();

        try (InputStream inputStream = file.getInputStream()) {
            List<CPPEdRpoMappingDTO> data = readEdRpoMappingExcel(inputStream);
            log.info("[CPPEdRpoMapping] IMPORT - read {} records from Excel", data.size());

            // Fetch existing data for change detection
            AOPMessageVM existingData = getEdRpoMapping(plantIds, aopYear);
            Map<UUID, String> existingHashes = new HashMap<>();
            if (existingData.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<CPPEdRpoMappingDTO> existingList = (List<CPPEdRpoMappingDTO>) existingData.getData();
                for (CPPEdRpoMappingDTO existing : existingList) {
                    if (existing.getId() != null) {
                        existingHashes.put(existing.getId(), existing.getDataHash());
                    }
                }
            }

            List<CPPEdRpoMappingRequestDTO> validRecords = new ArrayList<>();
            List<CPPEdRpoMappingDTO> failedRecords = new ArrayList<>();
            int skippedCount = 0;

            for (CPPEdRpoMappingDTO dto : data) {
                if (dto.getSaveStatus() != null && dto.getSaveStatus().equalsIgnoreCase("Failed")) {
                    failedRecords.add(dto);
                    continue;
                }

                if (dto.getId() == null) {
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription("Id is missing");
                    failedRecords.add(dto);
                    continue;
                }

                // Skip unchanged records (hash-based change detection)
                String currentHash = generateEdRpoMappingHash(dto);
                String importedHash = dto.getDataHash();
                String dbHash = existingHashes.get(dto.getId());

                boolean isModified;
                if (importedHash != null && !importedHash.isEmpty()) {
                    isModified = !importedHash.equals(currentHash);
                } else if (dbHash != null) {
                    isModified = !dbHash.equals(currentHash);
                } else {
                    isModified = true;
                }

                if (!isModified) {
                    skippedCount++;
                    log.debug("[CPPEdRpoMapping] Skipping unchanged record: {}", dto.getId());
                    continue;
                }

                validRecords.add(convertEdRpoMappingToRequestDto(dto));
            }

            log.info("[CPPEdRpoMapping] {} unchanged (skipped), {} modified to process",
                    skippedCount, data.size() - skippedCount);

            if (!validRecords.isEmpty()) {
                AOPMessageVM saveResponse = updateEdRpoMapping(plantIds, aopYear, validRecords);
                if (saveResponse.getCode() != 200 && saveResponse.getCode() != 207) {
                    vm.setCode(saveResponse.getCode());
                    vm.setMessage(saveResponse.getMessage());
                    vm.setData(saveResponse.getData());
                    return vm;
                }
            }

            if (!failedRecords.isEmpty()) {
                byte[] errorExcel = generateEdRpoMappingExcel(failedRecords, aopYear, buildEdRpoMappingErrorMessages(failedRecords));
                String base64File = Base64.getEncoder().encodeToString(errorExcel);
                vm.setCode(400);
                vm.setData(base64File);
                vm.setMessage("Partial data saved. " + validRecords.size() + " saved, " + failedRecords.size()
                        + " failed, " + skippedCount + " unchanged. Please check the downloaded error file.");
            } else {
                vm.setCode(200);
                if (validRecords.isEmpty() && skippedCount > 0) {
                    vm.setMessage("No changes detected. All " + skippedCount + " records unchanged.");
                } else {
                    vm.setMessage("All data saved. " + validRecords.size() + " updated, " + skippedCount + " unchanged.");
                }
                vm.setData(null);
            }

        } catch (Exception e) {
            log.error("[CPPEdRpoMapping] IMPORT error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
        }

        return vm;
    }

    // ── Helper methods for ED RPO Mapping ─────────────────────────────────────

    private CPPEdRpoMappingDTO mapRowToEdRpoMappingDto(Object[] row) {
        CPPEdRpoMappingDTO dto = new CPPEdRpoMappingDTO();
        int idx = 0;
        dto.setId(toUUIDObj(row[idx++]));
        dto.setCppPlantFkId(toUUIDObj(row[idx++]));
        dto.setCppPlantName(toStringObj(row[idx++]));
        dto.setCppPlantCode(toStringObj(row[idx++]));
        dto.setNormheaderfkid(toUUIDObj(row[idx++]));
        dto.setGeneratingPlantFkId(toUUIDObj(row[idx++]));
        dto.setGeneratingPlantName(toStringObj(row[idx++]));
        dto.setGeneratingPlantCode(toStringObj(row[idx++]));
        dto.setIssuingPlantFkId(toUUIDObj(row[idx++]));
        dto.setIssuingPlantName(toStringObj(row[idx++]));
        dto.setIssuingPlantCode(toStringObj(row[idx++]));
        dto.setUtilityNormParameterFkId(toUUIDObj(row[idx++]));
        dto.setUtilityName(toStringObj(row[idx++]));
        dto.setUtilitySAPMaterialCode(toStringObj(row[idx++]));
        dto.setMaterialNormParameterFkId(toUUIDObj(row[idx++]));
        dto.setMaterialName(toStringObj(row[idx++]));
        dto.setMaterialSAPMaterialCode(toStringObj(row[idx++]));
        dto.setAopYear(toStringObj(row[idx++]));
        dto.setExempt(toBooleanObj(row[idx++]));
        dto.setRemark(toStringObj(row[idx++]));
        return dto;
    }

    private Boolean toBooleanObj(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Boolean) return (Boolean) obj;
        if (obj instanceof Number) return ((Number) obj).intValue() != 0;
        String s = obj.toString().trim();
        return "true".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s);
    }

    private CPPEdRpoMappingRequestDTO convertEdRpoMappingToRequestDto(CPPEdRpoMappingDTO dto) {
        CPPEdRpoMappingRequestDTO request = new CPPEdRpoMappingRequestDTO();
        request.setId(dto.getId());
        request.setCppPlantFkId(dto.getCppPlantFkId());
        request.setCppPlantName(dto.getCppPlantName());
        request.setCppPlantCode(dto.getCppPlantCode());
        request.setNormheaderfkid(dto.getNormheaderfkid());
        request.setGeneratingPlantFkId(dto.getGeneratingPlantFkId());
        request.setGeneratingPlantName(dto.getGeneratingPlantName());
        request.setGeneratingPlantCode(dto.getGeneratingPlantCode());
        request.setIssuingPlantFkId(dto.getIssuingPlantFkId());
        request.setIssuingPlantName(dto.getIssuingPlantName());
        request.setIssuingPlantCode(dto.getIssuingPlantCode());
        request.setUtilityNormParameterFkId(dto.getUtilityNormParameterFkId());
        request.setUtilityName(dto.getUtilityName());
        request.setUtilitySAPMaterialCode(dto.getUtilitySAPMaterialCode());
        request.setMaterialNormParameterFkId(dto.getMaterialNormParameterFkId());
        request.setMaterialName(dto.getMaterialName());
        request.setMaterialSAPMaterialCode(dto.getMaterialSAPMaterialCode());
        request.setAopYear(dto.getAopYear());
        request.setExempt(dto.getExempt());
        request.setRemark(dto.getRemark());
        request.setModifiedBy(null);
        return request;
    }

    private String generateEdRpoMappingHash(CPPEdRpoMappingDTO dto) {
        try {
            StringBuilder dataToHash = new StringBuilder();
            dataToHash.append(dto.getExempt() != null && dto.getExempt() ? "true" : "false").append("|");
            dataToHash.append(dto.getRemark() != null ? dto.getRemark().trim() : "null");

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
            log.error("[CPPEdRpoMapping] Error generating hash: {}", e.getMessage(), e);
            return "";
        }
    }

    private List<String> buildEdRpoMappingErrorMessages(List<CPPEdRpoMappingDTO> failedRecords) {
        List<String> messages = new ArrayList<>();
        for (CPPEdRpoMappingDTO dto : failedRecords) {
            messages.add(dto.getErrDescription() != null ? dto.getErrDescription() : "Unknown error");
        }
        return messages;
    }

    private byte[] generateEdRpoMappingExcel(List<CPPEdRpoMappingDTO> dtoList, String aopYear,
                                             List<String> errorMessages) throws IOException {
        boolean isErrorExcel = errorMessages != null && !errorMessages.isEmpty();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(isErrorExcel ? "ED RPO Mapping Errors" : "ED RPO Mapping");

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(workbook);
        CellStyle lockedStyle = ExcelStyles.createLockedStyle(workbook);
        CellStyle unlockedStyle = ExcelStyles.createUnlockedStyle(workbook);
        CellStyle remarksStyle = ExcelStyles.createEditableRemarksStyle(workbook);
        CellStyle errorStyle = isErrorExcel ? ExcelStyles.createErrorStyle(workbook) : null;

        int rowNum = 0;
        int col = 0;
        Row headerRow = sheet.createRow(rowNum++);

        String[] headers = {
            "CPP Plant Name", "CPP Plant Code",
            "Generating Plant Name", "Generating Plant Code",
            "Issuing Plant Name", "Issuing Plant Code",
            "Utility Name", "Utility SAP Material Code",
            "Material Name", "Material SAP Material Code",
            "AOP Year", "Exempt", "Remark"
        };

        for (String header : headers) {
            headerRow.createCell(col).setCellValue(header);
            headerRow.getCell(col++).setCellStyle(headerStyle);
        }

        // Hidden identifier columns
        int idCol = col;
        headerRow.createCell(col).setCellValue("id");
        headerRow.getCell(col++).setCellStyle(headerStyle);

        int cppPlantFkIdCol = col;
        headerRow.createCell(col).setCellValue("cppPlantFkId");
        headerRow.getCell(col++).setCellStyle(headerStyle);

        int normheaderfkidCol = col;
        headerRow.createCell(col).setCellValue("normheaderfkid");
        headerRow.getCell(col++).setCellStyle(headerStyle);

        int dataHashCol = col;
        headerRow.createCell(col).setCellValue("dataHash");
        headerRow.getCell(col++).setCellStyle(headerStyle);

        int commentCol = -1;
        if (isErrorExcel) {
            headerRow.createCell(col).setCellValue("Status");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            commentCol = col;
            headerRow.createCell(col).setCellValue("Error Description");
            headerRow.getCell(col++).setCellStyle(headerStyle);
        }

        int totalColumns = col;

        for (CPPEdRpoMappingDTO dto : dtoList) {
            Row row = sheet.createRow(rowNum++);
            col = 0;

            setStringCellValue(row.createCell(col++), dto.getCppPlantName(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getCppPlantCode(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getGeneratingPlantName(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getGeneratingPlantCode(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getIssuingPlantName(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getIssuingPlantCode(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getUtilityName(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getUtilitySAPMaterialCode(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getMaterialName(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getMaterialSAPMaterialCode(), lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getAopYear(), lockedStyle);

            // Exempt (editable)
            setStringCellValue(row.createCell(col++), Boolean.TRUE.equals(dto.getExempt()) ? "TRUE" : "FALSE", unlockedStyle);

            // Remark (editable)
            setStringCellValue(row.createCell(col++), dto.getRemark(), remarksStyle);

            // Hidden columns
            setStringCellValue(row.createCell(col++), dto.getId() != null ? dto.getId().toString() : null, lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getCppPlantFkId() != null ? dto.getCppPlantFkId().toString() : null, lockedStyle);
            setStringCellValue(row.createCell(col++), dto.getNormheaderfkid() != null ? dto.getNormheaderfkid().toString() : null, lockedStyle);
            setStringCellValue(row.createCell(col++), generateEdRpoMappingHash(dto), lockedStyle);

            if (isErrorExcel) {
                setStringCellValue(row.createCell(col++), dto.getSaveStatus(), errorStyle);
                setStringCellValue(row.createCell(col++), dto.getErrDescription(), errorStyle);
            }
        }

        sheet.setColumnHidden(idCol, true);
        sheet.setColumnHidden(cppPlantFkIdCol, true);
        sheet.setColumnHidden(normheaderfkidCol, true);
        sheet.setColumnHidden(dataHashCol, true);

        for (int i = 0; i < totalColumns; i++) {
            sheet.autoSizeColumn(i);
        }
        if (commentCol >= 0) {
            sheet.setColumnWidth(commentCol, 8000);
        }

        ExcelStyles.applySheetProtectionWithAutoFilter(sheet, totalColumns);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    private List<CPPEdRpoMappingDTO> readEdRpoMappingExcel(InputStream inputStream) {
        List<CPPEdRpoMappingDTO> dataList = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (!rowIterator.hasNext()) {
                return dataList;
            }

            Row headerRow = rowIterator.next();
            int numCols = headerRow.getLastCellNum();

            int exemptColIdx = -1;
            int remarkColIdx = -1;
            int idColIdx = -1;
            int cppPlantFkIdColIdx = -1;
            int normheaderfkidColIdx = -1;
            int dataHashColIdx = -1;

            for (int i = 0; i < numCols; i++) {
                Cell cell = headerRow.getCell(i);
                if (cell == null) continue;
                String header = cell.getStringCellValue().trim().toLowerCase();
                switch (header) {
                    case "exempt"         -> exemptColIdx = i;
                    case "remark"         -> remarkColIdx = i;
                    case "remarks"        -> remarkColIdx = i;
                    case "id"             -> idColIdx = i;
                    case "cppplantfkid"   -> cppPlantFkIdColIdx = i;
                    case "normheaderfkid" -> normheaderfkidColIdx = i;
                    case "datahash"       -> dataHashColIdx = i;
                    default -> { /* ignore */ }
                }
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (row.getLastCellNum() <= 0) continue;

                CPPEdRpoMappingDTO dto = new CPPEdRpoMappingDTO();
                try {
                    if (exemptColIdx >= 0) {
                        String exVal = getStringCellValue(row.getCell(exemptColIdx));
                        dto.setExempt("true".equalsIgnoreCase(exVal) || "1".equals(exVal) || "yes".equalsIgnoreCase(exVal));
                    }
                    if (remarkColIdx >= 0) {
                        dto.setRemark(getStringCellValue(row.getCell(remarkColIdx)));
                    }
                    if (dataHashColIdx >= 0) {
                        dto.setDataHash(getStringCellValue(row.getCell(dataHashColIdx)));
                    }
                    if (idColIdx >= 0) {
                        String idStr = getStringCellValue(row.getCell(idColIdx));
                        if (idStr != null && !idStr.isEmpty()) {
                            try { dto.setId(UUID.fromString(idStr)); } catch (Exception ignored) {}
                        }
                    }
                    if (cppPlantFkIdColIdx >= 0) {
                        String fkIdStr = getStringCellValue(row.getCell(cppPlantFkIdColIdx));
                        if (fkIdStr != null && !fkIdStr.isEmpty()) {
                            try { dto.setCppPlantFkId(UUID.fromString(fkIdStr)); } catch (Exception ignored) {}
                        }
                    }
                    if (normheaderfkidColIdx >= 0) {
                        String nhFkIdStr = getStringCellValue(row.getCell(normheaderfkidColIdx));
                        if (nhFkIdStr != null && !nhFkIdStr.isEmpty()) {
                            try { dto.setNormheaderfkid(UUID.fromString(nhFkIdStr)); } catch (Exception ignored) {}
                        }
                    }

                    if (dto.getId() == null) {
                        dto.setSaveStatus("Failed");
                        dto.setErrDescription("Id is missing");
                    }

                } catch (Exception e) {
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription(e.getMessage());
                }

                dataList.add(dto);
            }
        } catch (Exception e) {
            log.error("[CPPEdRpoMapping] Error reading Excel file", e);
        }

        return dataList;
    }
}
