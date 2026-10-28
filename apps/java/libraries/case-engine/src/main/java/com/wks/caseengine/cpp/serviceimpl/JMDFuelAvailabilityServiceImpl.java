package com.wks.caseengine.cpp.serviceimpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
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
import com.wks.caseengine.repository.FinancialYearMonthRepository;

@Service
public class JMDFuelAvailabilityServiceImpl implements JMDFuelAvailabilityService {

    private static final Logger logger = LoggerFactory.getLogger(JMDFuelAvailabilityServiceImpl.class);

    private static final int[] FINANCIAL_MONTHS = {4, 5, 6, 7, 8, 9, 10, 11, 12, 1, 2, 3};

    @Autowired
    private JMDFuelAvailabilityRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    @Qualifier("db1JdbcTemplate")
    private JdbcTemplate db1JdbcTemplate;

    @Autowired
    private FinancialYearMonthRepository financialYearMonthRepository;

    @Autowired
    @Qualifier("db1TransactionManager")
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    private TransactionTemplate getTransactionTemplate() {
        if (transactionTemplate == null && transactionManager != null) {
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            this.transactionTemplate = tt;
        }
        return transactionTemplate;
    }

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
        dto.setFuelDescription(p.getFuelDescription());
        if (p.getCategoryFkId() != null) {
            dto.setCategoryFkId(UUID.fromString(p.getCategoryFkId()));
        }
        dto.setCategoryName(p.getCategoryName());
        dto.setCategoryDisplayName(p.getCategoryDisplayName());
        return dto;
    }

    @Override
    public AOPMessageVM saveFuelAvailability(List<UUID> plantIds, String financialYear,
                                             List<FuelAvailabilityTransactionDTO> dtoList) {
        return saveInternal(dtoList, false, financialYear, plantIds);
    }

    @Override
    public AOPMessageVM saveFuelAvailabilityCost(List<UUID> plantIds, String financialYear,
                                                 List<FuelAvailabilityTransactionDTO> dtoList) {
        return saveInternal(dtoList, true, financialYear, plantIds);
    }

    public record FinancialYearRange(int startYear, int endYear) {}

    public FinancialYearRange parseAndValidateFinancialYear(String financialYear) {
        if (financialYear == null || financialYear.isBlank()) {
            throw new IllegalArgumentException("Financial year cannot be null or blank");
        }
        String trimmed = financialYear.trim();
        Pattern pattern = Pattern.compile("^(\\d{4})-(\\d{2}|\\d{4})$");
        Matcher matcher = pattern.matcher(trimmed);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid financial year format: '" + financialYear +
                    "'. Expected format 'YYYY-YY' (e.g. '2026-27') or 'YYYY-YYYY' (e.g. '2026-2027').");
        }

        int startYear = Integer.parseInt(matcher.group(1));
        String secondPart = matcher.group(2);
        int expectedEndYear = startYear + 1;

        if (startYear < 1900 || startYear > 2100) {
            throw new IllegalArgumentException("Financial year out of supported range (1900-2100): " + startYear);
        }

        if (secondPart.length() == 2) {
            int endTwoDigits = Integer.parseInt(secondPart);
            int expectedTwoDigits = expectedEndYear % 100;
            if (endTwoDigits != expectedTwoDigits) {
                throw new IllegalArgumentException("Invalid financial year sequence: '" + financialYear +
                        "'. End year digits (" + secondPart + ") must be consecutive to start year (" + startYear + "). Expected: " +
                        String.format("%02d", expectedTwoDigits));
            }
        } else {
            int endFourDigits = Integer.parseInt(secondPart);
            if (endFourDigits != expectedEndYear) {
                throw new IllegalArgumentException("Invalid financial year sequence: '" + financialYear +
                        "'. End year (" + endFourDigits + ") must be consecutive to start year (" + startYear + "). Expected: " + expectedEndYear);
            }
        }

        return new FinancialYearRange(startYear, expectedEndYear);
    }

    private Map<Integer, UUID> resolveFinancialYearMonths(String financialYear) {
        FinancialYearRange range = parseAndValidateFinancialYear(financialYear);
        List<Object[]> fyMonths = financialYearMonthRepository.findFinancialYearMonths(range.startYear(), range.endYear());
        Map<Integer, UUID> monthIdMap = new LinkedHashMap<>();
        for (Object[] row : fyMonths) {
            if (row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            Integer month = Integer.parseInt(row[0].toString());
            UUID monthId = UUID.fromString(row[1].toString());
            monthIdMap.put(month, monthId);
        }
        return monthIdMap;
    }

    private UUID resolveProcurementPlantId(UUID cppPlantFkId, Map<UUID, UUID> procurementPlantCache) {
        if (cppPlantFkId == null) {
            return null;
        }
        if (procurementPlantCache.containsKey(cppPlantFkId)) {
            return procurementPlantCache.get(cppPlantFkId);
        }

        // 1. Query CPPPlantFuelSourceMapping
        String mappingSql = "SELECT CAST(FuelAvailablePlantFKId AS varchar(36)) FROM dbo.CPPPlantFuelSourceMapping WHERE CPPPlantFKId = ?";
        List<String> mappingResults = db1JdbcTemplate.queryForList(mappingSql, String.class, cppPlantFkId.toString());

        String sourcePlantId = (!mappingResults.isEmpty() && mappingResults.get(0) != null)
                ? mappingResults.get(0)
                : cppPlantFkId.toString();

        // 2. Query Plants where SourceName matches sourcePlantId and BusinessCategoryName = 'Import'
        String plantSql = "SELECT CAST(Id AS varchar(36)) FROM dbo.Plants " +
                          "WHERE (SourceName = ? OR TRY_CONVERT(UNIQUEIDENTIFIER, SourceName) = TRY_CONVERT(UNIQUEIDENTIFIER, ?)) " +
                          "  AND BusinessCategoryName = 'Import' " +
                          "ORDER BY DisplayOrder ASC, DisplayName ASC";
        List<String> plantResults = db1JdbcTemplate.queryForList(plantSql, String.class, sourcePlantId, sourcePlantId);

        // If not found with mapped plant and sourcePlantId != cppPlantFkId, try cppPlantFkId directly
        if (plantResults.isEmpty() && !sourcePlantId.equals(cppPlantFkId.toString())) {
            plantResults = db1JdbcTemplate.queryForList(plantSql, String.class, cppPlantFkId.toString(), cppPlantFkId.toString());
        }

        if (plantResults.isEmpty()) {
            logger.warn("resolveProcurementPlantId: No Import plant found in dbo.Plants for CPPPlantFKId: {}", cppPlantFkId);
            return null;
        }

        UUID procurementPlantId = UUID.fromString(plantResults.get(0));
        logger.debug("resolveProcurementPlantId: cppPlantFkId={} resolved to procurementPlantId={}", cppPlantFkId, procurementPlantId);
        procurementPlantCache.put(cppPlantFkId, procurementPlantId);
        return procurementPlantId;
    }

    private String resolveFuelCode(FuelAvailabilityTransactionDTO dto, Map<UUID, String> fuelCodeCache) {
        if (dto.getFuelCode() != null && !dto.getFuelCode().trim().isEmpty()) {
            return dto.getFuelCode().trim();
        }
        if (dto.getFuelId() != null) {
            if (fuelCodeCache.containsKey(dto.getFuelId())) {
                return fuelCodeCache.get(dto.getFuelId());
            }
            String sql = "SELECT FuelCode FROM dbo.CPP_FuelMaster WHERE Id = ?";
            List<String> codes = db1JdbcTemplate.queryForList(sql, String.class, dto.getFuelId().toString());
            if (!codes.isEmpty() && codes.get(0) != null && !codes.get(0).trim().isEmpty()) {
                String code = codes.get(0).trim();
                fuelCodeCache.put(dto.getFuelId(), code);
                return code;
            }
        }
        return null;
    }

    private record NormParamLookupResult(UUID normParamId, String message) {}

    private NormParamLookupResult findFuelNormParameter(String fuelCode, UUID procurementPlantId) {
        String findSql = "SELECT CAST(Id AS varchar(36)) AS Id FROM dbo.NormParameters " +
                         "WHERE SAPMaterialCode = ? " +
                         "  AND Plant_FK_Id = ? " +
                         "  AND NormType_FK_Id = 2 " +
                         "  AND [Type] = 'CPPFuel'";
        List<String> normParamIds = db1JdbcTemplate.queryForList(findSql, String.class, fuelCode, procurementPlantId.toString());

        if (normParamIds.isEmpty()) {
            return new NormParamLookupResult(null, "No NormParameters record found for fuelCode '" + fuelCode +
                    "', procurementPlantId " + procurementPlantId + " (NormType_FK_Id=2, Type='CPPFuel')");
        }
        if (normParamIds.size() > 1) {
            return new NormParamLookupResult(null, "Ambiguous NormParameters records found (" + normParamIds.size() +
                    ") for fuelCode '" + fuelCode + "', procurementPlantId " + procurementPlantId + " (NormType_FK_Id=2, Type='CPPFuel')");
        }
        return new NormParamLookupResult(UUID.fromString(normParamIds.get(0)), null);
    }

    private List<UUID> findMatchingNormsHeaderIds(UUID normParameterId, UUID procurementPlantId) {
        String headerSql = "SELECT CAST(Id AS varchar(36)) AS Id FROM dbo.NormsHeader " +
                           "WHERE NormParameter_FK_Id = ? " +
                           "  AND (IssuingPlant_FK_Id = ? OR IssuingPlant_FK_Id IS NULL)";
        List<String> headerIdStrs = db1JdbcTemplate.queryForList(headerSql, String.class,
                normParameterId.toString(), procurementPlantId.toString());
        List<UUID> headerIds = new ArrayList<>();
        for (String idStr : headerIdStrs) {
            headerIds.add(UUID.fromString(idStr));
        }
        return headerIds;
    }

    private Double getCostForMonth(FuelAvailabilityTransactionDTO dto, int month) {
        return switch (month) {
            case 4 -> dto.getCostApr();
            case 5 -> dto.getCostMay();
            case 6 -> dto.getCostJun();
            case 7 -> dto.getCostJul();
            case 8 -> dto.getCostAug();
            case 9 -> dto.getCostSep();
            case 10 -> dto.getCostOct();
            case 11 -> dto.getCostNov();
            case 12 -> dto.getCostDec();
            case 1 -> dto.getCostJan();
            case 2 -> dto.getCostFeb();
            case 3 -> dto.getCostMar();
            default -> null;
        };
    }

    private String getCppMonthWisePriceColumn(int month) {
        return switch (month) {
            case 4 -> "Apr_Price";
            case 5 -> "May_Price";
            case 6 -> "Jun_Price";
            case 7 -> "Jul_Price";
            case 8 -> "Aug_Price";
            case 9 -> "Sep_Price";
            case 10 -> "Oct_Price";
            case 11 -> "Nov_Price";
            case 12 -> "Dec_Price";
            case 1 -> "Jan_Price";
            case 2 -> "Feb_Price";
            case 3 -> "Mar_Price";
            default -> null;
        };
    }

    private int syncNormsMonthDetailPrices(UUID normsHeaderId, FuelAvailabilityTransactionDTO dto,
                                           Map<Integer, UUID> monthIdMap) {
        String updateSql = "UPDATE dbo.NormsMonthDetail SET Price = ? " +
                           "WHERE NormsHeader_FK_Id = ? AND FinancialYearMonth_FK_Id = ?";
        int updatedTotal = 0;
        for (int month : FINANCIAL_MONTHS) {
            Double costVal = getCostForMonth(dto, month);
            if (costVal != null) {
                UUID fymId = monthIdMap.get(month);
                if (fymId != null) {
                    updatedTotal += db1JdbcTemplate.update(updateSql, BigDecimal.valueOf(costVal),
                            normsHeaderId.toString(), fymId.toString());
                }
            }
        }
        return updatedTotal;
    }

    private int syncCppMonthWisePrice(UUID normsHeaderId, String financialYear, FuelAvailabilityTransactionDTO dto,
                                      List<String> warnings) {
        String checkSql = "SELECT CAST(Id AS varchar(36)) FROM dbo.CPPMonthWisePrice " +
                          "WHERE NormsHeader_FK_Id = ? AND (FinancialYear = ? OR (FinancialYear IS NULL AND AOPYear = ?))";
        List<String> existing = db1JdbcTemplate.queryForList(checkSql, String.class,
                normsHeaderId.toString(), financialYear, financialYear);

        if (existing.isEmpty()) {
            logger.info("syncCppMonthWisePrice: No existing CPPMonthWisePrice record for NormsHeader={}, financialYear={} — skipping update",
                    normsHeaderId, financialYear);
            return 0;
        }

        if (existing.size() > 1) {
            String warning = "Ambiguous CPPMonthWisePrice records (" + existing.size() +
                    ") found for NormsHeader " + normsHeaderId + ", financialYear " + financialYear + " — skipped CPPMonthWisePrice update";
            logger.warn(warning);
            warnings.add(warning);
            return 0;
        }

        List<String> setClauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        for (int month : FINANCIAL_MONTHS) {
            Double costVal = getCostForMonth(dto, month);
            if (costVal != null) {
                setClauses.add(getCppMonthWisePriceColumn(month) + " = ?");
                params.add(BigDecimal.valueOf(costVal));
            }
        }

        if (setClauses.isEmpty()) {
            return 0;
        }

        setClauses.add("UpdatedDate = GETDATE()");
        String updateSql = "UPDATE dbo.CPPMonthWisePrice SET " + String.join(", ", setClauses) +
                           " WHERE NormsHeader_FK_Id = ? AND (FinancialYear = ? OR (FinancialYear IS NULL AND AOPYear = ?))";
        params.add(normsHeaderId.toString());
        params.add(financialYear);
        params.add(financialYear);
        return db1JdbcTemplate.update(updateSql, params.toArray());
    }

    private record SyncResult(boolean success, String message) {}

    private SyncResult synchronizeFuelAvailabilityCost(
            FuelAvailabilityTransactionDTO dto,
            String financialYear,
            Map<Integer, UUID> monthIdMap,
            Map<UUID, UUID> procurementPlantCache,
            Map<UUID, String> fuelCodeCache) {

        // 1. Resolve procurement/import plant
        UUID procurementPlantId = resolveProcurementPlantId(dto.getCppPlantFkId(), procurementPlantCache);
        if (procurementPlantId == null) {
            String msg = "Transaction saved, but synchronization skipped: Could not resolve procurement/import plant for CPPPlantFKId " + dto.getCppPlantFkId();
            logger.warn(msg);
            return new SyncResult(true, msg);
        }

        // 2. Resolve fuel code
        String fuelCode = resolveFuelCode(dto, fuelCodeCache);
        if (fuelCode == null || fuelCode.isBlank()) {
            String msg = "Transaction saved, but synchronization skipped: FuelCode is missing for fuel ID " + dto.getFuelId();
            logger.warn(msg);
            return new SyncResult(true, msg);
        }

        // 3. Find NormParameters record
        NormParamLookupResult normParamResult = findFuelNormParameter(fuelCode, procurementPlantId);
        if (normParamResult.normParamId() == null) {
            String msg = "Transaction saved, but synchronization skipped: " + normParamResult.message();
            logger.warn(msg);
            return new SyncResult(true, msg);
        }
        UUID normParamId = normParamResult.normParamId();

        // 4. Find all matching NormsHeader records
        List<UUID> normsHeaderIds = findMatchingNormsHeaderIds(normParamId, procurementPlantId);
        if (normsHeaderIds.isEmpty()) {
            String msg = "Transaction saved, but synchronization skipped: No NormsHeader found for NormParameter ID " + normParamId + " (fuelCode '" + fuelCode + "')";
            logger.info(msg);
            return new SyncResult(true, msg);
        }

        // 5. Update NormsMonthDetail and CPPMonthWisePrice for all matching headers
        List<String> warnings = new ArrayList<>();
        int totalNmdUpdated = 0;
        int totalPriceUpdated = 0;

        for (UUID headerId : normsHeaderIds) {
            totalNmdUpdated += syncNormsMonthDetailPrices(headerId, dto, monthIdMap);
            totalPriceUpdated += syncCppMonthWisePrice(headerId, financialYear, dto, warnings);
        }

        logger.info("synchronizeFuelAvailabilityCost: Synced fuelCode='{}', normParamId={}, headers={}, nmdRows={}, priceRows={}",
                fuelCode, normParamId, normsHeaderIds.size(), totalNmdUpdated, totalPriceUpdated);

        if (totalNmdUpdated == 0 && totalPriceUpdated == 0 && hasAnyCost(dto)) {
            String notice = "Transaction saved, but synchronization skipped: No existing NormsMonthDetail or CPPMonthWisePrice records found for NormsHeaders " + normsHeaderIds;
            logger.info(notice);
            warnings.add(notice);
        }

        if (!warnings.isEmpty()) {
            return new SyncResult(true, String.join("; ", warnings));
        }

        return new SyncResult(true, null);
    }

    private boolean hasAnyCost(FuelAvailabilityTransactionDTO dto) {
        for (int month : FINANCIAL_MONTHS) {
            if (getCostForMonth(dto, month) != null) {
                return true;
            }
        }
        return false;
    }

    private <T> T executeInTransaction(java.util.function.Function<org.springframework.transaction.TransactionStatus, T> action) {
        TransactionTemplate tt = getTransactionTemplate();
        if (tt != null) {
            return tt.execute(action::apply);
        } else {
            return action.apply(null);
        }
    }

    private AOPMessageVM saveInternal(List<FuelAvailabilityTransactionDTO> dtoList, boolean includeCost,
                                      String financialYearParam, List<UUID> plantIds) {
        logger.info("[JMDFuelAvailability] SAVE - includeCost: {}, financialYearParam: {}, records: {}",
                includeCost, financialYearParam, dtoList != null ? dtoList.size() : 0);
        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (dtoList == null || dtoList.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("Request body cannot be empty");
                return vm;
            }

            // Financial year validation & month resolution
            String authoritativeFy = null;
            if (financialYearParam != null && !financialYearParam.isBlank()) {
                authoritativeFy = financialYearParam.trim();
            } else if (dtoList != null) {
                // Fallback to the first nonblank DTO financial year when request param is blank
                for (FuelAvailabilityTransactionDTO dto : dtoList) {
                    if (dto.getFinancialYear() != null && !dto.getFinancialYear().isBlank()) {
                        authoritativeFy = dto.getFinancialYear().trim();
                        break;
                    }
                }
            }

            Map<Integer, UUID> monthIdMap = Collections.emptyMap();
            if (includeCost) {
                if (authoritativeFy == null || authoritativeFy.isBlank()) {
                    vm.setCode(400);
                    vm.setMessage("financialYear is required for cost synchronization (neither query parameter nor payload financialYear was supplied)");
                    return vm;
                }

                try {
                    parseAndValidateFinancialYear(authoritativeFy);
                    monthIdMap = resolveFinancialYearMonths(authoritativeFy);

                    // Validate that all 12 required financial-year months (April=4 through March=3) are available
                    List<Integer> missingMonths = new ArrayList<>();
                    for (int month : FINANCIAL_MONTHS) {
                        if (!monthIdMap.containsKey(month) || monthIdMap.get(month) == null) {
                            missingMonths.add(month);
                        }
                    }
                    if (!missingMonths.isEmpty() || monthIdMap.size() != 12) {
                        vm.setCode(400);
                        vm.setMessage("Incomplete financial-year month mapping for financial year '" + authoritativeFy +
                                "': expected 12 months (April-March), but found " + monthIdMap.size() +
                                (missingMonths.isEmpty() ? "" : ". Missing month(s): " + missingMonths));
                        return vm;
                    }
                } catch (IllegalArgumentException e) {
                    vm.setCode(400);
                    vm.setMessage("Invalid financialYear: " + e.getMessage());
                    return vm;
                }
            }

            int successCount = 0;
            int errorCount = 0;
            List<String> errorMessages = new ArrayList<>();

            Map<UUID, UUID> procurementPlantCache = new HashMap<>();
            Map<UUID, String> fuelCodeCache = new HashMap<>();

            for (FuelAvailabilityTransactionDTO dto : dtoList) {
                try {
                    // Fall back to single plantId if dto.cppPlantFkId is null
                    if (dto.getCppPlantFkId() == null && plantIds != null && plantIds.size() == 1) {
                        dto.setCppPlantFkId(plantIds.get(0));
                    }

                    if (dto.getCppPlantFkId() == null || dto.getFuelId() == null) {
                        errorCount++;
                        errorMessages.add("Record skipped: cppPlantFkId/fuelId is null");
                        continue;
                    }

                    if (dto.getType() == null) {
                        dto.setType("FUEL_AVAILABILITY");
                    }

                    // Validate financial year consistency
                    if (authoritativeFy != null) {
                        if (dto.getFinancialYear() != null && !dto.getFinancialYear().isBlank()) {
                            if (!dto.getFinancialYear().trim().equalsIgnoreCase(authoritativeFy)) {
                                errorCount++;
                                errorMessages.add("Record skipped: financialYear mismatch: payload has '" +
                                        dto.getFinancialYear() + "', expected '" + authoritativeFy + "'");
                                continue;
                            }
                        }
                        dto.setFinancialYear(authoritativeFy);
                    } else if (dto.getFinancialYear() == null || dto.getFinancialYear().isBlank()) {
                        errorCount++;
                        errorMessages.add("Record skipped: financialYear is null or cannot be determined");
                        continue;
                    }

                    final String currentFy = includeCost ? authoritativeFy : dto.getFinancialYear().trim();
                    final Map<Integer, UUID> fymMap = monthIdMap;

                    // Execute each record in its own transaction boundary for consistency & partial-success support
                    SyncResult recordResult = executeInTransaction(status -> {
                        if (dto.getId() == null) {
                            // CREATE via JPA
                            CPPFuelAvailabilityTransaction entity = new CPPFuelAvailabilityTransaction();
                            entity.setCppPlantFkId(dto.getCppPlantFkId());
                            entity.setFuelFkId(dto.getFuelId());
                            entity.setType(dto.getType());
                            entity.setUom(dto.getUom());
                            entity.setCalorificValue(dto.getCalorificValue());
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
                            if (includeCost) {
                                entity.setCostApr(dto.getCostApr());
                                entity.setCostMay(dto.getCostMay());
                                entity.setCostJun(dto.getCostJun());
                                entity.setCostJul(dto.getCostJul());
                                entity.setCostAug(dto.getCostAug());
                                entity.setCostSep(dto.getCostSep());
                                entity.setCostOct(dto.getCostOct());
                                entity.setCostNov(dto.getCostNov());
                                entity.setCostDec(dto.getCostDec());
                                entity.setCostJan(dto.getCostJan());
                                entity.setCostFeb(dto.getCostFeb());
                                entity.setCostMar(dto.getCostMar());
                            }
                            entity.setFinancialYear(currentFy);
                            entity.setRemarks(dto.getRemarks());
                            LocalDateTime now = LocalDateTime.now();
                            entity.setCreatedDate(now);
                            entity.setUpdatedDate(now);
                            repository.save(entity);
                            dto.setId(entity.getId());
                        } else {
                            // UPDATE
                            if (includeCost) {
                                int affected = repository.updateFuelAvailabilityCost(
                                        dto.getId(),
                                        dto.getCostApr(), dto.getCostMay(), dto.getCostJun(), dto.getCostJul(),
                                        dto.getCostAug(), dto.getCostSep(), dto.getCostOct(), dto.getCostNov(),
                                        dto.getCostDec(), dto.getCostJan(), dto.getCostFeb(), dto.getCostMar(),
                                        dto.getRemarks());
                                if (affected == 0) {
                                    if (status != null) status.setRollbackOnly();
                                    return new SyncResult(false, "Fuel availability record not found with id: " + dto.getId());
                                }
                            } else {
                                int affected = repository.updateFuelAvailability(
                                        dto.getId(),
                                        dto.getFuelId(),
                                        dto.getUom(),
                                        dto.getCalorificValue(),
                                        dto.getApr(), dto.getMay(), dto.getJun(), dto.getJul(),
                                        dto.getAug(), dto.getSep(), dto.getOct(), dto.getNov(),
                                        dto.getDec(), dto.getJan(), dto.getFeb(), dto.getMar(),
                                        dto.getRemarks());
                                if (affected == 0) {
                                    if (status != null) status.setRollbackOnly();
                                    return new SyncResult(false, "Fuel availability record not found with id: " + dto.getId());
                                }
                            }
                        }

                        // If includeCost, synchronize target tables
                        if (includeCost) {
                            return synchronizeFuelAvailabilityCost(dto, currentFy, fymMap, procurementPlantCache, fuelCodeCache);
                        }

                        return new SyncResult(true, null);
                    });

                    if (recordResult != null && !recordResult.success()) {
                        errorCount++;
                        errorMessages.add(recordResult.message());
                    } else {
                        successCount++;
                        if (recordResult != null && recordResult.message() != null) {
                            // Informational note / skip notification
                            errorMessages.add(recordResult.message());
                        }
                    }

                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = "Error processing record: Database operation failed";
                    errorMessages.add(errorMsg);
                    logger.error("Error processing record for fuel ID: " + dto.getFuelId(), e);
                }
            }

            logger.info("[JMDFuelAvailability] SAVE - success: {}, errors: {}", successCount, errorCount);

            if (errorCount > 0) {
                vm.setCode(207);
                vm.setMessage(String.format("Processed %d records. Success: %d, Errors: %d",
                        dtoList.size(), successCount, errorCount));
                vm.setData(errorMessages);
            } else if (!errorMessages.isEmpty()) {
                // All transactions succeeded, but some had sync skips/notices
                vm.setCode(207);
                vm.setMessage(String.format("Processed all %d records with synchronization notices", successCount));
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
    @Transactional
    public AOPMessageVM getFuelAvailabilityCost(UUID siteId, List<UUID> plantIds, String financialYear, String type) {
        logger.info("[JMDFuelAvailabilityCost] GET - siteId: {}, plantIds: {}, financialYear: {}, type: {}",
                siteId, plantIds, financialYear, type);
        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (siteId == null && (plantIds == null || plantIds.isEmpty())) {
                vm.setCode(400);
                vm.setMessage("Either siteId or plantIds is required");
                vm.setData(new ArrayList<>());
                return vm;
            }

            if (financialYear == null || financialYear.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("financialYear cannot be null or empty");
                vm.setData(new ArrayList<>());
                return vm;
            }

            String plantIdsCsv = plantIds != null
                    ? plantIds.stream().map(UUID::toString).collect(Collectors.joining(","))
                    : null;

            StoredProcedureQuery sp = entityManager
                    .createStoredProcedureQuery("dbo.CPP_GetFuelAvailabilityWithCost")
                    .registerStoredProcedureParameter("SiteId", UUID.class, ParameterMode.IN)
                    .registerStoredProcedureParameter("CPPPlantFKIds", String.class, ParameterMode.IN)
                    .registerStoredProcedureParameter("FinancialYear", String.class, ParameterMode.IN)
                    .registerStoredProcedureParameter("Type", String.class, ParameterMode.IN);

            sp.setParameter("SiteId", siteId);
            sp.setParameter("CPPPlantFKIds", plantIdsCsv);
            sp.setParameter("FinancialYear", financialYear);
            sp.setParameter("Type", type);

            logger.info("Executing stored procedure dbo.CPP_GetFuelAvailabilityWithCost for siteId: {}, plantIds: {}, financialYear: {}, type: {}",
                    siteId, plantIdsCsv, financialYear, type);
            sp.execute();

            @SuppressWarnings("unchecked")
            List<Object[]> rawResults = sp.getResultList();

            List<FuelAvailabilityTransactionDTO> result = new ArrayList<>();
            for (Object[] row : rawResults) {
                result.add(mapRowToDto(row));
            }

            logger.info("[JMDFuelAvailabilityCost] GET - found {} records", result.size());

            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(result);

        } catch (Exception e) {
            logger.error("[JMDFuelAvailabilityCost] GET error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
        }

        return vm;
    }

    @Override
    @Transactional
    public AOPMessageVM getMappedFuelAvailability(List<UUID> plantIds, String financialYear, String type) {
        logger.info("[JMDFuelAvailability] GET MAPPED - plantIds: {}, financialYear: {}, type: {}",
                plantIds, financialYear, type);
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

            // Resolves availability through dbo.CPPPlantFuelSourceMapping:
            // each asset-owning plant (CPPPlantFKId, e.g. C2-CPP,
            // DTA-PCG-CPP) inherits the fuel availability declared under
            // its mapped plant (FuelAvailablePlantFKId, e.g. DTA-CPP).
            // Column order must match mapRowToDto (same order as
            // dbo.CPP_GetFuelAvailabilityWithCost).
            String sql =
                    "SELECT fat.Id, " +
                    "       m.CPPPlantFKId AS CPPPlantFKId, " +
                    "       cpp.DisplayName AS CPPPlantName, " +
                    "       fat.Fuel_FK_Id AS FuelId, " +
                    "       fm.FuelName, fm.FuelDisplayName, fm.FuelCode, " +
                    "       fm.Category_FK_Id AS CategoryId, " +
                    "       cat.FuelName AS CategoryName, " +
                    "       cat.FuelDisplayName AS CategoryDisplayName, " +
                    "       fat.[Type], fat.UOM, fat.FinancialYear, " +
                    "       fat.Apr, fat.May, fat.Jun, fat.Jul, fat.Aug, fat.Sep, " +
                    "       fat.Oct, fat.Nov, fat.[Dec], fat.Jan, fat.Feb, fat.Mar, " +
                    "       fat.Remarks, " +
                    "       fat.Cost_Apr, fat.Cost_May, fat.Cost_Jun, fat.Cost_Jul, " +
                    "       fat.Cost_Aug, fat.Cost_Sep, fat.Cost_Oct, fat.Cost_Nov, " +
                    "       fat.Cost_Dec, fat.Cost_Jan, fat.Cost_Feb, fat.Cost_Mar, " +
                    "       fat.CalorificValue " +
                    "FROM dbo.CPPFuelAvailabilityTransaction fat WITH(NOLOCK) " +
                    "JOIN dbo.CPPPlantFuelSourceMapping m WITH(NOLOCK) " +
                    "       ON m.FuelAvailablePlantFKId = fat.CPPPlantFKId " +
                    "JOIN dbo.Plants cpp WITH(NOLOCK) " +
                    "       ON cpp.Id = m.CPPPlantFKId " +
                    "LEFT JOIN dbo.CPP_FuelMaster fm WITH(NOLOCK) " +
                    "       ON fm.Id = fat.Fuel_FK_Id " +
                    "LEFT JOIN dbo.CPP_FuelMaster cat WITH(NOLOCK) " +
                    "       ON cat.Id = fm.Category_FK_Id " +
                    "WHERE fat.FinancialYear = :financialYear " +
                    "  AND (:type IS NULL OR fat.[Type] = :type) " +
                    "  AND m.CPPPlantFKId IN (SELECT TRY_CAST(value AS UNIQUEIDENTIFIER) " +
                    "                       FROM STRING_SPLIT(:plantIds, ',')) " +
                    "ORDER BY cpp.DisplayName, fm.FuelDisplayName";

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("plantIds", plantIdsCsv);
            query.setParameter("financialYear", financialYear);
            query.setParameter("type", type);

            @SuppressWarnings("unchecked")
            List<Object[]> rawResults = query.getResultList();

            List<FuelAvailabilityTransactionDTO> result = new ArrayList<>();
            for (Object[] row : rawResults) {
                result.add(mapRowToDto(row));
            }

            logger.info("[JMDFuelAvailability] GET MAPPED - found {} records", result.size());

            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(result);

        } catch (Exception e) {
            logger.error("[JMDFuelAvailability] GET MAPPED error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
        }

        return vm;
    }

    @Override
    public byte[] exportFuelAvailability(UUID siteId, List<UUID> plantIds, String financialYear, String type, boolean minimal) throws IOException {
        return exportInternal(siteId, plantIds, financialYear, type, false, minimal);
    }

    @Override
    public byte[] exportFuelAvailabilityCost(UUID siteId, List<UUID> plantIds, String financialYear, String type) throws IOException {
        return exportInternal(siteId, plantIds, financialYear, type, true, true);
    }

    private byte[] exportInternal(UUID siteId, List<UUID> plantIds, String financialYear, String type, boolean includeCost, boolean minimal) throws IOException {
        logger.info("[JMDFuelAvailability] EXPORT - siteId: {}, plantIds: {}, financialYear: {}, type: {}, includeCost: {}, minimal: {}",
                siteId, plantIds, financialYear, type, includeCost, minimal);

        try {
            // Cost-aware SP carries fat.UOM + transaction CalorificValue —
            // use it for cost files and for the JMD "minimal" layout.
            AOPMessageVM result = (includeCost || minimal)
                    ? getFuelAvailabilityCost(siteId, plantIds, financialYear, type)
                    : getFuelAvailability(plantIds, financialYear, type);

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

            return generateExcel(dtoList, financialYear, null, includeCost, minimal);

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
    public AOPMessageVM importFuelAvailability(List<UUID> plantIds, String financialYear, MultipartFile file,
                                               boolean minimal, String type) throws IOException {
        return importInternal(plantIds, financialYear, file, false, minimal, type);
    }

    @Override
    @Transactional
    public AOPMessageVM importFuelAvailabilityCost(List<UUID> plantIds, String financialYear, MultipartFile file,
                                                   String type) throws IOException {
        return importInternal(plantIds, financialYear, file, true, true, type);
    }

    private AOPMessageVM importInternal(List<UUID> plantIds, String financialYear, MultipartFile file,
                                        boolean includeCost, boolean minimal, String type) throws IOException {
        logger.info("[JMDFuelAvailability] IMPORT - plantIds: {}, financialYear: {}, file: {}, includeCost: {}, minimal: {}",
                plantIds, financialYear, file.getOriginalFilename(), includeCost, minimal);

        AOPMessageVM vm = new AOPMessageVM();

        try (InputStream inputStream = file.getInputStream()) {
            List<FuelAvailabilityTransactionDTO> dtoList = readExcel(inputStream, financialYear, includeCost, minimal, type);

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
                if (!isRecordModified(dto, includeCost)) {
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

            AOPMessageVM saveResult = saveInternal(validRecords, includeCost, financialYear, plantIds);

            if (!failedRecords.isEmpty()) {
                byte[] errorExcel = generateExcel(failedRecords, financialYear, errorMessages, includeCost, minimal);
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

    // Exchange rate shown above the JMD Fuel Cost grid — persisted per
    // site + AOP year in dbo.CPPFuelExchangeRate via native queries
    // (no entity/repo/dto).
    @Override
    @Transactional
    public AOPMessageVM getFuelExchangeRate(UUID siteId, String financialYear) {
        logger.info("[JMDFuelAvailabilityCost] GET EXCHANGE RATE - siteId: {}, financialYear: {}",
                siteId, financialYear);
        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (siteId == null) {
                vm.setCode(400);
                vm.setMessage("siteId cannot be null");
                return vm;
            }

            if (financialYear == null || financialYear.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("financialYear cannot be null or empty");
                return vm;
            }

            String sql = "SELECT TOP 1 Id, SiteFKId, AOPYear, [Value], Remarks, CreatedDate, UpdatedDate " +
                    "FROM dbo.CPPFuelExchangeRate WITH(NOLOCK) " +
                    "WHERE SiteFKId = :siteId AND AOPYear = :financialYear";

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("siteId", siteId);
            query.setParameter("financialYear", financialYear);

            @SuppressWarnings("unchecked")
            List<Object[]> rawResults = query.getResultList();

            java.util.Map<String, Object> data = null;
            if (!rawResults.isEmpty()) {
                Object[] row = rawResults.get(0);
                data = new java.util.LinkedHashMap<>();
                data.put("id", row[0] != null ? row[0].toString() : null);
                data.put("siteId", row[1] != null ? row[1].toString() : null);
                data.put("aopYear", row[2] != null ? row[2].toString() : null);
                data.put("value", row[3] != null ? ((Number) row[3]).doubleValue() : null);
                data.put("remarks", row[4] != null ? row[4].toString() : null);
                data.put("createdDate", row[5] != null ? row[5].toString() : null);
                data.put("updatedDate", row[6] != null ? row[6].toString() : null);
            }

            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(data);

        } catch (Exception e) {
            logger.error("[JMDFuelAvailabilityCost] GET EXCHANGE RATE error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
        }

        return vm;
    }

    @Override
    @Transactional
    public AOPMessageVM saveFuelExchangeRate(UUID siteId, String financialYear, Double value, String remarks) {
        logger.info("[JMDFuelAvailabilityCost] SAVE EXCHANGE RATE - siteId: {}, financialYear: {}, value: {}",
                siteId, financialYear, value);
        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (siteId == null) {
                vm.setCode(400);
                vm.setMessage("siteId cannot be null");
                return vm;
            }

            if (financialYear == null || financialYear.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("financialYear cannot be null or empty");
                return vm;
            }

            Query update = entityManager.createNativeQuery(
                    "UPDATE dbo.CPPFuelExchangeRate " +
                    "SET [Value] = :value, Remarks = :remarks, UpdatedDate = SYSUTCDATETIME() " +
                    "WHERE SiteFKId = :siteId AND AOPYear = :financialYear");
            update.setParameter("value", value);
            update.setParameter("remarks", remarks);
            update.setParameter("siteId", siteId);
            update.setParameter("financialYear", financialYear);
            int updated = update.executeUpdate();

            if (updated == 0) {
                Query insert = entityManager.createNativeQuery(
                        "INSERT INTO dbo.CPPFuelExchangeRate " +
                        "(Id, SiteFKId, AOPYear, [Value], Remarks, CreatedDate, UpdatedDate) " +
                        "VALUES (NEWID(), :siteId, :financialYear, :value, :remarks, SYSUTCDATETIME(), SYSUTCDATETIME())");
                insert.setParameter("siteId", siteId);
                insert.setParameter("financialYear", financialYear);
                insert.setParameter("value", value);
                insert.setParameter("remarks", remarks);
                insert.executeUpdate();
            }

            vm.setCode(200);
            vm.setMessage(updated == 0 ? "Exchange rate saved successfully" : "Exchange rate updated successfully");

        } catch (Exception e) {
            logger.error("[JMDFuelAvailabilityCost] SAVE EXCHANGE RATE error: {}", e.getMessage(), e);
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
        dto.setFuelCode(row[6] != null ? row[6].toString() : null);
        dto.setCategoryId(row[7] != null ? UUID.fromString(row[7].toString()) : null);
        dto.setCategoryName(row[8] != null ? row[8].toString() : null);
        dto.setCategoryDisplayName(row[9] != null ? row[9].toString() : null);
        dto.setType(row[10] != null ? row[10].toString() : null);
        dto.setUom(row[11] != null ? row[11].toString() : null);
        dto.setFinancialYear(row[12] != null ? row[12].toString() : null);
        dto.setApr(toDoubleObj(row[13]));
        dto.setMay(toDoubleObj(row[14]));
        dto.setJun(toDoubleObj(row[15]));
        dto.setJul(toDoubleObj(row[16]));
        dto.setAug(toDoubleObj(row[17]));
        dto.setSep(toDoubleObj(row[18]));
        dto.setOct(toDoubleObj(row[19]));
        dto.setNov(toDoubleObj(row[20]));
        dto.setDec(toDoubleObj(row[21]));
        dto.setJan(toDoubleObj(row[22]));
        dto.setFeb(toDoubleObj(row[23]));
        dto.setMar(toDoubleObj(row[24]));
        dto.setRemarks(row[25] != null ? row[25].toString() : null);
        // Cost columns are present only after the SP is updated to return
        // Cost_Apr..Cost_Mar appended at the end of the SELECT (indexes 26-37).
        if (row.length > 37) {
            dto.setCostApr(toDoubleObj(row[26]));
            dto.setCostMay(toDoubleObj(row[27]));
            dto.setCostJun(toDoubleObj(row[28]));
            dto.setCostJul(toDoubleObj(row[29]));
            dto.setCostAug(toDoubleObj(row[30]));
            dto.setCostSep(toDoubleObj(row[31]));
            dto.setCostOct(toDoubleObj(row[32]));
            dto.setCostNov(toDoubleObj(row[33]));
            dto.setCostDec(toDoubleObj(row[34]));
            dto.setCostJan(toDoubleObj(row[35]));
            dto.setCostFeb(toDoubleObj(row[36]));
            dto.setCostMar(toDoubleObj(row[37]));
        }
        // CalorificValue is present only when the SP returns it appended last.
        if (row.length > 38) {
            dto.setCalorificValue(toDoubleObj(row[38]));
        }
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
                                 List<String> errorMessages, boolean includeCost, boolean minimal) throws IOException {
        boolean isErrorExcel = errorMessages != null && !errorMessages.isEmpty();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(isErrorExcel
                ? (includeCost ? "Fuel Cost Errors" : "Fuel Availability Errors")
                : (includeCost ? "Fuel Cost (In INR)" : "Fuel Availability (In MMBTU)"));

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(workbook);
        CellStyle lockedStyle = ExcelStyles.createLockedStyle(workbook);
        CellStyle unlockedStyle = ExcelStyles.createUnlockedStyle(workbook);
        CellStyle remarksStyle = ExcelStyles.createEditableRemarksStyle(workbook);
        CellStyle errorStyle = isErrorExcel ? ExcelStyles.createErrorStyle(workbook) : null;

        String[] months = FiscalYearMonths.getMonthHeaders(financialYear);

        int rowNum = 0;
        int col = 0;

        Row headerRow = sheet.createRow(rowNum++);
        String[] baseHeaders = minimal
                ? (includeCost
                        ? new String[]{"CPP Plant", "Fuel"}
                        : new String[]{"CPP Plant", "Fuel", "Calorific Value"})
                : new String[]{"CPP Plant", "Category", "Fuel", "Fuel Code", "Type", "UOM"};
        for (String header : baseHeaders) {
            ExcelCells.setString(headerRow.createCell(col++), header, headerStyle);
        }

        int monthStartCol = col;
        for (String month : months) {
            ExcelCells.setString(headerRow.createCell(col++), includeCost ? month + " Cost" : month, headerStyle);
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

            if (minimal) {
                ExcelCells.setString(row.createCell(col++), dto.getCppPlantName(), lockedStyle);
                ExcelCells.setString(row.createCell(col++), dto.getFuelDisplayName(), lockedStyle);
                if (!includeCost) {
                    ExcelCells.setDouble(row.createCell(col++), dto.getCalorificValue(), unlockedStyle);
                }
            } else {
                ExcelCells.setString(row.createCell(col++), dto.getCppPlantName(), lockedStyle);
                ExcelCells.setString(row.createCell(col++), dto.getCategoryDisplayName(), lockedStyle);
                ExcelCells.setString(row.createCell(col++), dto.getFuelDisplayName(), lockedStyle);
                ExcelCells.setString(row.createCell(col++), dto.getFuelCode(), lockedStyle);
                ExcelCells.setString(row.createCell(col++), dto.getType(), lockedStyle);
                ExcelCells.setString(row.createCell(col++), dto.getUom(), lockedStyle);
            }

            if (includeCost) {
                ExcelCells.setDouble(row.createCell(monthStartCol + 0), dto.getCostApr(), unlockedStyle);
                ExcelCells.setDouble(row.createCell(monthStartCol + 1), dto.getCostMay(), unlockedStyle);
                ExcelCells.setDouble(row.createCell(monthStartCol + 2), dto.getCostJun(), unlockedStyle);
                ExcelCells.setDouble(row.createCell(monthStartCol + 3), dto.getCostJul(), unlockedStyle);
                ExcelCells.setDouble(row.createCell(monthStartCol + 4), dto.getCostAug(), unlockedStyle);
                ExcelCells.setDouble(row.createCell(monthStartCol + 5), dto.getCostSep(), unlockedStyle);
                ExcelCells.setDouble(row.createCell(monthStartCol + 6), dto.getCostOct(), unlockedStyle);
                ExcelCells.setDouble(row.createCell(monthStartCol + 7), dto.getCostNov(), unlockedStyle);
                ExcelCells.setDouble(row.createCell(monthStartCol + 8), dto.getCostDec(), unlockedStyle);
                ExcelCells.setDouble(row.createCell(monthStartCol + 9), dto.getCostJan(), unlockedStyle);
                ExcelCells.setDouble(row.createCell(monthStartCol + 10), dto.getCostFeb(), unlockedStyle);
                ExcelCells.setDouble(row.createCell(monthStartCol + 11), dto.getCostMar(), unlockedStyle);
            } else {
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
            }
            col = monthStartCol + 12;

            ExcelCells.setString(row.createCell(col++), dto.getRemarks(), remarksStyle);
            ExcelCells.setString(row.createCell(col++), dto.getId() != null ? dto.getId().toString() : null, lockedStyle);
            ExcelCells.setString(row.createCell(col++), dto.getCppPlantFkId() != null ? dto.getCppPlantFkId().toString() : null, lockedStyle);
            ExcelCells.setString(row.createCell(col++), dto.getFuelId() != null ? dto.getFuelId().toString() : null, lockedStyle);
            ExcelCells.setString(row.createCell(col++), generateHash(dto, includeCost), lockedStyle);

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

    private List<FuelAvailabilityTransactionDTO> readExcel(InputStream inputStream, String financialYear,
                                                           boolean includeCost, boolean minimal, String type) throws IOException {
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
        int fuelCodeColIdx = -1;
        int remarksColIdx = -1;
        int dataHashColIdx = -1;
        // Full:         0=CPP Plant, 1=Category, 2=Fuel, 3=Fuel Code, 4=Type, 5=UOM
        // Minimal:      0=CPP Plant, 1=Fuel, 2=Calorific Value
        // Minimal cost: 0=CPP Plant, 1=Fuel (no UOM / Calorific Value)
        int monthStartIdx = minimal ? (includeCost ? 2 : 3) : 6;

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
            } else if (headerLower.equals("fuelcode")) {
                fuelCodeColIdx = i;
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
            if (minimal) {
                dto.setCppPlantName(ExcelCells.toStringValue(row.getCell(0)));
                dto.setFuelDisplayName(ExcelCells.toStringValue(row.getCell(1)));
                if (!includeCost) {
                    dto.setCalorificValue(ExcelCells.toDouble(row.getCell(2)));
                }
            } else {
                dto.setCppPlantName(ExcelCells.toStringValue(row.getCell(0)));
                dto.setCategoryDisplayName(ExcelCells.toStringValue(row.getCell(1)));
                dto.setFuelDisplayName(ExcelCells.toStringValue(row.getCell(2)));
                dto.setFuelCode(ExcelCells.toStringValue(row.getCell(3)));
                dto.setType(ExcelCells.toStringValue(row.getCell(4)));
                dto.setUom(ExcelCells.toStringValue(row.getCell(5)));
            }

            // Minimal files have no Type column — take it from the request param
            if (dto.getType() == null || dto.getType().trim().isEmpty()) {
                dto.setType(type);
            }

            if (idColIdx >= 0) {
                dto.setId(ExcelCells.toUUID(row.getCell(idColIdx)));
            }

            if (cppPlantFkIdColIdx >= 0) {
                dto.setCppPlantFkId(ExcelCells.toUUID(row.getCell(cppPlantFkIdColIdx)));
            }

            if (fuelIdColIdx >= 0) {
                dto.setFuelId(ExcelCells.toUUID(row.getCell(fuelIdColIdx)));
            }

            if (fuelCodeColIdx >= 0) {
                dto.setFuelCode(ExcelCells.toStringValue(row.getCell(fuelCodeColIdx)));
            }

            if (remarksColIdx >= 0) {
                dto.setRemarks(ExcelCells.toStringValue(row.getCell(remarksColIdx)));
            }

            if (dataHashColIdx >= 0) {
                dto.setDataHash(ExcelCells.toStringValue(row.getCell(dataHashColIdx)));
            }

            if (includeCost) {
                if (numCols > monthStartIdx + 0) dto.setCostApr(ExcelCells.toDouble(row.getCell(monthStartIdx + 0)));
                if (numCols > monthStartIdx + 1) dto.setCostMay(ExcelCells.toDouble(row.getCell(monthStartIdx + 1)));
                if (numCols > monthStartIdx + 2) dto.setCostJun(ExcelCells.toDouble(row.getCell(monthStartIdx + 2)));
                if (numCols > monthStartIdx + 3) dto.setCostJul(ExcelCells.toDouble(row.getCell(monthStartIdx + 3)));
                if (numCols > monthStartIdx + 4) dto.setCostAug(ExcelCells.toDouble(row.getCell(monthStartIdx + 4)));
                if (numCols > monthStartIdx + 5) dto.setCostSep(ExcelCells.toDouble(row.getCell(monthStartIdx + 5)));
                if (numCols > monthStartIdx + 6) dto.setCostOct(ExcelCells.toDouble(row.getCell(monthStartIdx + 6)));
                if (numCols > monthStartIdx + 7) dto.setCostNov(ExcelCells.toDouble(row.getCell(monthStartIdx + 7)));
                if (numCols > monthStartIdx + 8) dto.setCostDec(ExcelCells.toDouble(row.getCell(monthStartIdx + 8)));
                if (numCols > monthStartIdx + 9) dto.setCostJan(ExcelCells.toDouble(row.getCell(monthStartIdx + 9)));
                if (numCols > monthStartIdx + 10) dto.setCostFeb(ExcelCells.toDouble(row.getCell(monthStartIdx + 10)));
                if (numCols > monthStartIdx + 11) dto.setCostMar(ExcelCells.toDouble(row.getCell(monthStartIdx + 11)));
            } else {
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
            }

            dtoList.add(dto);
        }

        workbook.close();
        return dtoList;
    }

    private String generateHash(FuelAvailabilityTransactionDTO dto, boolean includeCost) {
        try {
            StringBuilder dataToHash = new StringBuilder();

            if (includeCost) {
                dataToHash.append(dto.getCostApr() != null ? String.valueOf(dto.getCostApr()) : "null").append("|");
                dataToHash.append(dto.getCostMay() != null ? String.valueOf(dto.getCostMay()) : "null").append("|");
                dataToHash.append(dto.getCostJun() != null ? String.valueOf(dto.getCostJun()) : "null").append("|");
                dataToHash.append(dto.getCostJul() != null ? String.valueOf(dto.getCostJul()) : "null").append("|");
                dataToHash.append(dto.getCostAug() != null ? String.valueOf(dto.getCostAug()) : "null").append("|");
                dataToHash.append(dto.getCostSep() != null ? String.valueOf(dto.getCostSep()) : "null").append("|");
                dataToHash.append(dto.getCostOct() != null ? String.valueOf(dto.getCostOct()) : "null").append("|");
                dataToHash.append(dto.getCostNov() != null ? String.valueOf(dto.getCostNov()) : "null").append("|");
                dataToHash.append(dto.getCostDec() != null ? String.valueOf(dto.getCostDec()) : "null").append("|");
                dataToHash.append(dto.getCostJan() != null ? String.valueOf(dto.getCostJan()) : "null").append("|");
                dataToHash.append(dto.getCostFeb() != null ? String.valueOf(dto.getCostFeb()) : "null").append("|");
                dataToHash.append(dto.getCostMar() != null ? String.valueOf(dto.getCostMar()) : "null").append("|");
            } else {
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
            }
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

    private boolean isRecordModified(FuelAvailabilityTransactionDTO dto, boolean includeCost) {
        if (dto.getId() == null) {
            return true;
        }

        String importedHash = dto.getDataHash();
        if (importedHash == null || importedHash.isEmpty()) {
            return true;
        }

        String currentHash = generateHash(dto, includeCost);
        boolean modified = !importedHash.equals(currentHash);

        if (!modified) {
            logger.debug("[JMDFuelAvailability] Record {} unchanged - hash match", dto.getId());
        }

        return modified;
    }
}
