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
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.Query;
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
        dto.setFuelDescription(p.getFuelDescription());
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
        return saveInternal(dtoList, false);
    }

    @Override
    @Transactional
    public AOPMessageVM saveFuelAvailabilityCost(List<UUID> plantIds, String financialYear,
                                                 List<FuelAvailabilityTransactionDTO> dtoList) {
        return saveInternal(dtoList, true);
    }

    private AOPMessageVM saveInternal(List<FuelAvailabilityTransactionDTO> dtoList, boolean includeCost) {
        logger.info("[JMDFuelAvailability] SAVE - includeCost: {}, records: {}",
                includeCost, dtoList.size());
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
                        entity.setFinancialYear(dto.getFinancialYear());
                        entity.setRemarks(dto.getRemarks());
                        java.time.LocalDateTime now = java.time.LocalDateTime.now();
                        entity.setCreatedDate(now);
                        entity.setUpdatedDate(now);
                        repository.save(entity);
                        successCount++;
                    } else {
                        // UPDATE
                        if (includeCost) {
                            // Cost path only updates cost columns + remarks — quantity
                            // fields are owned by the quantity grid/API and left untouched.
                            repository.updateFuelAvailabilityCost(
                                    dto.getId(),
                                    dto.getCostApr(), dto.getCostMay(), dto.getCostJun(), dto.getCostJul(),
                                    dto.getCostAug(), dto.getCostSep(), dto.getCostOct(), dto.getCostNov(),
                                    dto.getCostDec(), dto.getCostJan(), dto.getCostFeb(), dto.getCostMar(),
                                    dto.getRemarks());
                        } else {
                            repository.updateFuelAvailability(
                                    dto.getId(),
                                    // dto.getCppPlantFkId(),
                                    dto.getFuelId(),
                                    // dto.getType(),
                                    dto.getUom(),
                                    dto.getCalorificValue(),
                                    dto.getApr(), dto.getMay(), dto.getJun(), dto.getJul(),
                                    dto.getAug(), dto.getSep(), dto.getOct(), dto.getNov(),
                                    dto.getDec(), dto.getJan(), dto.getFeb(), dto.getMar(),
                                    // dto.getFinancialYear(),
                                    dto.getRemarks());
                        }
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

            AOPMessageVM saveResult = saveInternal(validRecords, includeCost);

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
        Sheet sheet = workbook.createSheet(isErrorExcel ? "Fuel Availability Errors" : "Fuel Availability");

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
                ? new String[]{"CPP Plant", "Fuel", "UOM", "Calorific Value"}
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
                ExcelCells.setString(row.createCell(col++), dto.getUom(), lockedStyle);
                ExcelCells.setDouble(row.createCell(col++), dto.getCalorificValue(), unlockedStyle);
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
        // Full:   0=CPP Plant, 1=Category, 2=Fuel, 3=Fuel Code, 4=Type, 5=UOM
        // Minimal: 0=CPP Plant, 1=Fuel, 2=UOM, 3=Calorific Value
        int monthStartIdx = minimal ? 4 : 6;

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
                dto.setUom(ExcelCells.toStringValue(row.getCell(2)));
                dto.setCalorificValue(ExcelCells.toDouble(row.getCell(3)));
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
