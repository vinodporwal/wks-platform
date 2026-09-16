package com.wks.caseengine.cpp.serviceimpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.cpp.repository.ImportPowerCapacityProjection;
import com.wks.caseengine.cpp.repository.ImportPowerCapacityRepository;
import com.wks.caseengine.cpp.repository.ImportPowerHoursRepository;
import com.wks.caseengine.cpp.repository.ImportPowerSourceRepository;
import com.wks.caseengine.cpp.service.ImportPowerCapacityService;
import com.wks.caseengine.dto.AddImportPowerCapacitySourceRequestDTO;
import com.wks.caseengine.dto.ImportPowerCapacityDto;
import com.wks.caseengine.dto.UpdateImportPowerCapacitySourceRequestDTO;
import com.wks.caseengine.entity.CPPImportPowerCapacity;
import com.wks.caseengine.entity.CPPImportPowerOperationalHours;
import com.wks.caseengine.entity.CPPImportPowerSourceMapping;
import com.wks.caseengine.entity.NormParameters;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.NormParametersRepository;
import com.wks.caseengine.repository.PlantsRepository;

@Service
public class ImportPowerCapacityServiceImpl implements ImportPowerCapacityService {

    private static final Logger logger = LoggerFactory.getLogger(ImportPowerCapacityServiceImpl.class);

    @Autowired
    private ImportPowerCapacityRepository repository;

    @Autowired
    private ImportPowerSourceRepository importPowerSourceRepository;

    @Autowired
    private ImportPowerHoursRepository importPowerHoursRepository;

    @Autowired
    private NormParametersRepository normParametersRepository;

    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    @Qualifier("db1JdbcTemplate")
    private JdbcTemplate db1JdbcTemplate;

    /**
     * Get import power capacity for a financial year
     */
    @Override
    public List<ImportPowerCapacityDto> getImportPowerCapacity(UUID cppPlantId, String financialYear) {
        
        if (financialYear == null || financialYear.isBlank()) {
            throw new RestInvalidArgumentException("FinancialYear is required", null);
        }

        if (!financialYear.matches("^\\d{4}-\\d{2}$")) {
            throw new RestInvalidArgumentException(
                    "Invalid financialYear format. Expected format: YYYY-YY (e.g., 2026-27)", null);
        }

        List<ImportPowerCapacityProjection> projections = repository.getImportPowerCapacity(cppPlantId, financialYear);
        List<ImportPowerCapacityDto> result = new ArrayList<>();

        for (ImportPowerCapacityProjection proj : projections) {
            ImportPowerCapacityDto dto = new ImportPowerCapacityDto();
            dto.setSourceId(proj.getSourceId());
            dto.setSourceName(proj.getSourceName());
            dto.setMaterialCode(proj.getMaterialCode());
            dto.setSapCode(proj.getSAPMaterialCode());
            dto.setUtilityName(proj.getUtilityName());
            dto.setPlantName(proj.getPlantName());
            dto.setApril(proj.getApr());
            dto.setMay(proj.getMay());
            dto.setJune(proj.getJun());
            dto.setJuly(proj.getJul());
            dto.setAugust(proj.getAug());
            dto.setSeptember(proj.getSep());
            dto.setOctober(proj.getOct());
            dto.setNovember(proj.getNov());
            dto.setDecember(proj.getDec());
            dto.setJanuary(proj.getJan());
            dto.setFebruary(proj.getFeb());
            dto.setMarch(proj.getMar());
            dto.setUom(proj.getUOM() != null ? proj.getUOM() : "MW");
            dto.setRemarks(proj.getRemarks() != null ? proj.getRemarks() : "");
            dto.setEditable(true);

            result.add(dto);
        }

        return result;
    }

    /**
     * Upsert import power capacity
     */
    @Override
    @Transactional
    public void upsertImportPowerCapacity(List<ImportPowerCapacityDto> dtoList, String financialYear) {
        
        if (financialYear == null || financialYear.isBlank()) {
            throw new RestInvalidArgumentException("FinancialYear is required", null);
        }

        if (!financialYear.matches("^\\d{4}-\\d{2}$")) {
            throw new RestInvalidArgumentException(
                    "Invalid financialYear format. Expected format: YYYY-YY (e.g., 2026-27)", null);
        }

        for (ImportPowerCapacityDto dto : dtoList) {
            if (dto.getSourceId() == null) {
                throw new RestInvalidArgumentException("sourceId is required in ImportPowerCapacityDto", null);
            }

            try {
                Optional<CPPImportPowerCapacity> existingOpt = 
                    repository.findByImportPowerSourceFkIdAndFinancialYear(dto.getSourceId(), financialYear);

                CPPImportPowerCapacity record;
                if (existingOpt.isPresent()) {
                    record = existingOpt.get();
                    record.setUpdatedDate(LocalDateTime.now());
                } else {
                    record = new CPPImportPowerCapacity();
                    record.setImportPowerSourceFkId(dto.getSourceId());
                    record.setFinancialYear(financialYear);
                    record.setCreatedDate(LocalDateTime.now());
                }

                record.setApr(dto.getApril() != null ? dto.getApril() : 0.0);
                record.setMay(dto.getMay() != null ? dto.getMay() : 0.0);
                record.setJun(dto.getJune() != null ? dto.getJune() : 0.0);
                record.setJul(dto.getJuly() != null ? dto.getJuly() : 0.0);
                record.setAug(dto.getAugust() != null ? dto.getAugust() : 0.0);
                record.setSep(dto.getSeptember() != null ? dto.getSeptember() : 0.0);
                record.setOct(dto.getOctober() != null ? dto.getOctober() : 0.0);
                record.setNov(dto.getNovember() != null ? dto.getNovember() : 0.0);
                record.setDec(dto.getDecember() != null ? dto.getDecember() : 0.0);
                record.setJan(dto.getJanuary() != null ? dto.getJanuary() : 0.0);
                record.setFeb(dto.getFebruary() != null ? dto.getFebruary() : 0.0);
                record.setMar(dto.getMarch() != null ? dto.getMarch() : 0.0);
                record.setUom(dto.getUom() != null ? dto.getUom() : "MW");
                record.setRemarks(dto.getRemarks() != null ? dto.getRemarks() : "");

                repository.save(record);

            } catch (Exception e) {
                throw new RestInvalidArgumentException("Failed to upsert capacity for source: " + dto.getSourceName(), e);
            }
        }
    }

    // ========================================
    // ADD IMPORT POWER CAPACITY SOURCE
    // ========================================

    @Override
    @Transactional
    public AOPMessageVM addImportPowerCapacitySource(AddImportPowerCapacitySourceRequestDTO request) {
        logger.info("[ADD Capacity Source] Adding import power capacity source for CPP plant: {}", request.getCppPlant());

        AOPMessageVM response = new AOPMessageVM();

        try {
            // --- Validation ---
            if (request.getCppPlant() == null) {
                response.setCode(400);
                response.setMessage("cppPlant is required");
                return response;
            }
            if (request.getProcurementPlant() == null) {
                response.setCode(400);
                response.setMessage("procurementPlant is required");
                return response;
            }
            if (request.getName() == null || request.getName().isBlank()) {
                response.setCode(400);
                response.setMessage("name is required");
                return response;
            }
            if (request.getAopYear() == null || request.getAopYear().isBlank()) {
                response.setCode(400);
                response.setMessage("aopYear is required");
                return response;
            }

            // --- Step 1: Fetch CPP plant to validate it exists ---
            Optional<Plants> cppPlantOpt = plantsRepository.findById(request.getCppPlant());
            if (cppPlantOpt.isEmpty()) {
                response.setCode(404);
                response.setMessage("CPP plant not found: " + request.getCppPlant());
                return response;
            }

            // --- Step 2: Fetch procurement plant and verify it belongs to the CPP plant ---
            // (procurement plants are Plants rows whose SourceName column = CPP plant UUID)
            Optional<Plants> procPlantOpt = plantsRepository.findById(request.getProcurementPlant());
            if (procPlantOpt.isEmpty()) {
                response.setCode(404);
                response.setMessage("Procurement plant not found: " + request.getProcurementPlant());
                return response;
            }
            if (procPlantOpt.get().getSourceName() == null
                    || !procPlantOpt.get().getSourceName().equalsIgnoreCase(request.getCppPlant().toString())) {
                response.setCode(400);
                response.setMessage("Procurement plant does not belong to the given CPP plant");
                return response;
            }
            logger.info("[ADD Capacity Source] cppPlant={}, procurementPlant={} validated",
                    request.getCppPlant(), request.getProcurementPlant());

            // --- Step 3: Check for duplicate source mapping (same sourceName + procurementPlant, active) ---
            List<CPPImportPowerSourceMapping> existingMappings = importPowerSourceRepository
                    .findBySourceNameAndPlantId(request.getName(), request.getProcurementPlant());
            CPPImportPowerSourceMapping existingMapping = existingMappings.isEmpty() ? null : existingMappings.get(0);
            if (existingMapping != null && Boolean.TRUE.equals(existingMapping.getIsActive())) {
                response.setCode(409);
                response.setMessage("A source with this name already exists for this plant: " + request.getName());
                return response;
            }

            // --- Step 4: Check for existing NormParameter (same name + procurementPlant + normTypeFKId=2) ---
            // If found, reuse it; otherwise create a new one.
            NormParameters savedNorm;
            Optional<NormParameters> existingNormOpt =
                    normParametersRepository.findFirstByNameAndPlantFkIdAndNormTypeFkId(
                            request.getName(), request.getProcurementPlant(), 2);

            if (existingNormOpt.isPresent()) {
                // Reuse existing NormParameter
                savedNorm = existingNormOpt.get();
                logger.info("[ADD Capacity Source] Reusing existing NormParameters Id: {}", savedNorm.getId());

                // If it was soft-deleted, reactivate it
                if (Boolean.FALSE.equals(savedNorm.getIsVisible())) {
                    savedNorm.setIsVisible(true);
                    savedNorm = normParametersRepository.save(savedNorm);
                }
            } else {
                // Create new NormParameters entry
                NormParameters norm = new NormParameters();
                norm.setName(request.getName());
                norm.setDisplayName(request.getDisplayName() != null && !request.getDisplayName().isBlank()
                        ? request.getDisplayName() : request.getName());
                norm.setUom(request.getUom());
                norm.setSapMaterialCode(request.getSapCode());
                norm.setType("Imported Power");
                // Plant_FK_Id = procurement plant (mirrors existing NMD NormParameters data pattern)
                norm.setPlantFkId(request.getProcurementPlant());
                // Fixed constants mirroring existing POWER import records in NormParameters
                norm.setNormParameterTypeFkId(UUID.fromString("E9C9FCFB-C5C6-49D6-8017-6D1E4C46868E"));
                norm.setNormTypeFKId(2);
                norm.setIsVisible(true);
                norm.setIsEditable(true);

                savedNorm = normParametersRepository.save(norm);
                logger.info("[ADD Capacity Source] NormParameters created with Id: {}", savedNorm.getId());
            }

            // --- Step 5: Create or reactivate CPPImportPowerSourceMapping entry ---
            CPPImportPowerSourceMapping sourceMapping;
            if (existingMapping != null && !Boolean.TRUE.equals(existingMapping.getIsActive())) {
                // Reactivate previously soft-deleted mapping
                existingMapping.setIsActive(true);
                existingMapping.setMaterialCode(request.getMaterialCode());
                existingMapping.setNormParameterFkId(savedNorm.getId());
                existingMapping.setUpdatedDate(LocalDateTime.now());
                sourceMapping = importPowerSourceRepository.save(existingMapping);
                logger.info("[ADD Capacity Source] Reactivated CPPImportPowerSourceMapping Id: {}", sourceMapping.getId());
            } else {
                sourceMapping = new CPPImportPowerSourceMapping();
                sourceMapping.setSourceName(request.getName());
                sourceMapping.setMaterialCode(request.getMaterialCode());
                sourceMapping.setNormParameterFkId(savedNorm.getId());
                // Plant_FK_Id = procurement plant, CPPPlant_FK_Id = CPP plant
                sourceMapping.setPlantFkId(request.getProcurementPlant());
                sourceMapping.setCppPlantFkId(request.getCppPlant());
                sourceMapping.setIsActive(true);
                sourceMapping.setCreatedDate(LocalDateTime.now());
                sourceMapping.setUpdatedDate(LocalDateTime.now());
                sourceMapping = importPowerSourceRepository.save(sourceMapping);
                logger.info("[ADD Capacity Source] CPPImportPowerSourceMapping created with Id: {}", sourceMapping.getId());
            }

            // --- Step 6: Create CPPImportPowerCapacity row (zero months) for the aopYear,
            //            so the new source appears in the Capacity grid ---
            Optional<CPPImportPowerCapacity> existingCapacity =
                    repository.findByImportPowerSourceFkIdAndFinancialYear(
                            sourceMapping.getId(), request.getAopYear());
            if (existingCapacity.isEmpty()) {
                CPPImportPowerCapacity capacity = new CPPImportPowerCapacity();
                capacity.setImportPowerSourceFkId(sourceMapping.getId());
                capacity.setFinancialYear(request.getAopYear());
                capacity.setApr(0.0); capacity.setMay(0.0); capacity.setJun(0.0);
                capacity.setJul(0.0); capacity.setAug(0.0); capacity.setSep(0.0);
                capacity.setOct(0.0); capacity.setNov(0.0); capacity.setDec(0.0);
                capacity.setJan(0.0); capacity.setFeb(0.0); capacity.setMar(0.0);
                capacity.setUom(request.getUom() != null ? request.getUom() : "MW");
                capacity.setRemarks("");
                capacity.setCreatedDate(LocalDateTime.now());
                capacity.setUpdatedDate(LocalDateTime.now());
                repository.save(capacity);
                logger.info("[ADD Capacity Source] CPPImportPowerCapacity created for sourceId={}, year={}",
                        sourceMapping.getId(), request.getAopYear());
            }

            // --- Step 7: Create CPPImportPowerOperationalHours row (zero months) for the aopYear,
            //            so the new source appears in the Operational Hours grid ---
            Optional<CPPImportPowerOperationalHours> existingHours =
                    importPowerHoursRepository.findByImportPowerSourceFkIdAndFinancialYear(
                            sourceMapping.getId(), request.getAopYear());
            if (existingHours.isEmpty()) {
                CPPImportPowerOperationalHours opHours = new CPPImportPowerOperationalHours();
                opHours.setImportPowerSourceFkId(sourceMapping.getId());
                opHours.setFinancialYear(request.getAopYear());
                opHours.setApr(0.0); opHours.setMay(0.0); opHours.setJun(0.0);
                opHours.setJul(0.0); opHours.setAug(0.0); opHours.setSep(0.0);
                opHours.setOct(0.0); opHours.setNov(0.0); opHours.setDec(0.0);
                opHours.setJan(0.0); opHours.setFeb(0.0); opHours.setMar(0.0);
                opHours.setRemarks(null);
                opHours.setCreatedDate(LocalDateTime.now());
                opHours.setUpdatedDate(LocalDateTime.now());
                importPowerHoursRepository.save(opHours);
                logger.info("[ADD Capacity Source] CPPImportPowerOperationalHours created for sourceId={}, year={}",
                        sourceMapping.getId(), request.getAopYear());
            }

            // --- Step 8: Create CPP_AssetNorms_Mapping rows so the source shows
            //            Utility Generated/Distributed in the operational-hours grid ---
            createAssetNormMappings(sourceMapping.getId(), request.getCppPlant(),
                    request.getProcurementPlant(), request.getMaterialCode());

            // --- Step 9: Create NormsHeader + child records (CPPNorms, NormsMonthDetail,
            //            CPPMonthWisePrice) so the source appears in the CPPNorms screen ---
            createNormsHeaderAndChildRecords(request.getProcurementPlant(), request.getCppPlant(),
                    savedNorm.getId(), request.getName(), request.getSapCode(),
                    request.getAopYear());

            Map<String, Object> data = new HashMap<>();
            data.put("normParameterId", savedNorm.getId());
            data.put("sourceMappingId", sourceMapping.getId());

            response.setCode(200);
            response.setMessage("Import power capacity source added successfully");
            response.setData(data);

        } catch (Exception e) {
            logger.error("[ADD Capacity Source] Error adding import power capacity source: {}", e.getMessage(), e);
            response.setCode(500);
            response.setMessage("Failed to add import power capacity source: " + e.getMessage());
            response.setData(null);
        }

        return response;
    }

    // ========================================
    // UPDATE IMPORT POWER CAPACITY SOURCE
    // ========================================

    @Override
    @Transactional
    public AOPMessageVM updateImportPowerCapacitySource(UUID sourceId, UpdateImportPowerCapacitySourceRequestDTO request) {
        logger.info("[UPDATE Capacity Source] sourceId={}", sourceId);

        AOPMessageVM response = new AOPMessageVM();

        try {
            if (sourceId == null) {
                response.setCode(400);
                response.setMessage("sourceId is required");
                return response;
            }

            Optional<CPPImportPowerSourceMapping> sourceOpt = importPowerSourceRepository.findById(sourceId);
            if (sourceOpt.isEmpty()) {
                response.setCode(404);
                response.setMessage("Import power source not found: " + sourceId);
                return response;
            }

            CPPImportPowerSourceMapping sourceMapping = sourceOpt.get();

            // Update CPPImportPowerSourceMapping fields
            if (request.getName() != null && !request.getName().isBlank()) {
                sourceMapping.setSourceName(request.getName());
            }
            if (request.getMaterialCode() != null) {
                sourceMapping.setMaterialCode(request.getMaterialCode());
            }
            sourceMapping.setUpdatedDate(LocalDateTime.now());

            CPPImportPowerSourceMapping savedSourceMapping = importPowerSourceRepository.save(sourceMapping);
            logger.info("[UPDATE Capacity Source] CPPImportPowerSourceMapping {} updated successfully", savedSourceMapping.getId());

            // Update linked NormParameters entry
            if (sourceMapping.getNormParameterFkId() != null) {
                Optional<NormParameters> normOpt = normParametersRepository.findById(sourceMapping.getNormParameterFkId());
                if (normOpt.isPresent()) {
                    NormParameters norm = normOpt.get();
                    if (request.getName() != null && !request.getName().isBlank()) {
                        norm.setName(request.getName());
                    }
                    if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
                        norm.setDisplayName(request.getDisplayName());
                    }
                    if (request.getUom() != null) {
                        norm.setUom(request.getUom());
                    }
                    if (request.getSapCode() != null) {
                        norm.setSapMaterialCode(request.getSapCode());
                    }
                    NormParameters savedNorm = normParametersRepository.save(norm);
                    logger.info("[UPDATE Capacity Source] NormParameters {} updated successfully", savedNorm.getId());
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("sourceMappingId", savedSourceMapping.getId());

            response.setCode(200);
            response.setMessage("Import power capacity source updated successfully");
            response.setData(data);

        } catch (Exception e) {
            logger.error("[UPDATE Capacity Source] Error: {}", e.getMessage(), e);
            response.setCode(500);
            response.setMessage("Failed to update import power capacity source: " + e.getMessage());
        }

        return response;
    }

    // ========================================
    // DELETE (SOFT) IMPORT POWER CAPACITY SOURCE
    // ========================================

    @Override
    @Transactional
    public AOPMessageVM deleteImportPowerCapacitySource(UUID sourceId, String financialYear) {
        logger.info("[DELETE Capacity Source] Soft-deleting sourceId={}, financialYear={}", sourceId, financialYear);

        AOPMessageVM response = new AOPMessageVM();

        try {
            if (sourceId == null) {
                response.setCode(400);
                response.setMessage("sourceId is required");
                return response;
            }

            Optional<CPPImportPowerSourceMapping> sourceOpt = importPowerSourceRepository.findById(sourceId);
            if (sourceOpt.isEmpty()) {
                response.setCode(404);
                response.setMessage("Import power source not found: " + sourceId);
                return response;
            }

            CPPImportPowerSourceMapping sourceMapping = sourceOpt.get();

            // Soft-delete: set isActive = false on CPPImportPowerSourceMapping
            sourceMapping.setIsActive(false);
            sourceMapping.setUpdatedDate(LocalDateTime.now());
            importPowerSourceRepository.save(sourceMapping);
            logger.info("[DELETE Capacity Source] CPPImportPowerSourceMapping {} marked as inactive", sourceId);

            // Delete the operational-hours rows for this source (matches JMD delete pattern)
            importPowerHoursRepository.deleteByImportPowerSourceFkId(sourceId);
            logger.info("[DELETE Capacity Source] CPPImportPowerOperationalHours rows deleted for sourceId={}", sourceId);

            // Also soft-delete the linked NormParameters entry (isVisible = false)
            if (sourceMapping.getNormParameterFkId() != null) {
                Optional<NormParameters> normOpt = normParametersRepository.findById(sourceMapping.getNormParameterFkId());
                if (normOpt.isPresent()) {
                    NormParameters norm = normOpt.get();
                    norm.setIsVisible(false);
                    normParametersRepository.save(norm);
                    logger.info("[DELETE Capacity Source] NormParameters {} marked as not visible", norm.getId());
                }
            }

            // Remove the CPP_AssetNorms_Mapping rows for this source
            importPowerSourceRepository.deleteAssetNormsMappingByAssetId(sourceId);
            logger.info("[DELETE Capacity Source] CPP_AssetNorms_Mapping rows deleted for sourceId={}", sourceId);

            // Delete NormsHeader child records (CPPNorms, NormsMonthDetail, CPPMonthWisePrice)
            // linked to this source's norm parameter, scoped to the given financial year
            // (mirrors SR Mapping delete behavior — preserves other years)
            if (sourceMapping.getNormParameterFkId() != null) {
                deleteNormsHeaderChildRecords(sourceMapping.getNormParameterFkId(), financialYear);
            }

            response.setCode(200);
            response.setMessage("Import power capacity source deleted successfully (soft delete)");

        } catch (Exception e) {
            logger.error("[DELETE Capacity Source] Error: {}", e.getMessage(), e);
            response.setCode(500);
            response.setMessage("Failed to delete import power capacity source: " + e.getMessage());
        }

        return response;
    }

    // ========================================
    // GET PROCUREMENT PLANTS FOR CPP PLANT
    // ========================================

    /**
     * Returns all procurement/source plants linked to the given CPP plant.
     * In the Plants table, procurement plants store the CPP plant UUID in their
     * SourceName column (same pattern as JMD: TRY_CONVERT(UNIQUEIDENTIFIER, SourceName) = cppPlant).
     */
    @Override
    public AOPMessageVM getImportCapacityProcurementPlants(UUID cppPlantId) {
        logger.info("[GET Procurement Plants] cppPlantId={}", cppPlantId);

        AOPMessageVM response = new AOPMessageVM();

        try {
            if (cppPlantId == null) {
                response.setCode(400);
                response.setMessage("cppPlantId is required");
                return response;
            }

            List<Plants> plants = plantsRepository
                    .findBySourceNameInAndIsActiveTrue(List.of(cppPlantId.toString()));

            List<Map<String, Object>> result = new ArrayList<>();
            for (Plants plant : plants) {
                Map<String, Object> item = new HashMap<>();
                item.put("procurementPlantId", plant.getId());
                item.put("name", plant.getDisplayName() != null ? plant.getDisplayName() : plant.getName());
                result.add(item);
            }

            logger.info("[GET Procurement Plants] Found {} procurement plant(s) for cppPlantId={}",
                    result.size(), cppPlantId);

            response.setCode(200);
            response.setMessage("Procurement plants fetched successfully");
            response.setData(result);

        } catch (Exception e) {
            logger.error("[GET Procurement Plants] Error: {}", e.getMessage(), e);
            response.setCode(500);
            response.setMessage("Failed to fetch procurement plants: " + e.getMessage());
        }

        return response;
    }

    // ========================================
    // PRIVATE HELPER: CPP_AssetNorms_Mapping
    // ========================================

    /**
     * Creates the 2 CPP_AssetNorms_Mapping rows for a new import-power source so that
     * Utility Generated / Utility Distributed columns populate in the
     * operational-hours grid (getNormParametersByAssetIds join).
     *
     * Pattern (mirrors existing data):
     *  - Generated  → "POWERGEN" norm  (NormType_FK_Id = 1) at the CPP plant
     *  - Distributed → norm named after the source's MaterialCode
     *                  (NormType_FK_Id = 2) at the procurement plant,
     *                  falling back to the shared "POWER" norm
     *
     * Idempotent: deletes any existing mappings for the sourceId first, so re-adding
     * a previously deleted source produces a clean set of rows.
     */
    private void createAssetNormMappings(UUID sourceId, UUID cppPlantId, UUID procurementPlantId, String materialCode) {
        importPowerSourceRepository.deleteAssetNormsMappingByAssetId(sourceId);

        // Distributed norm: prefer a norm named after the material code (e.g. MEL → POWER_MEL),
        // else the shared "POWER" norm (e.g. MSCB → POWER), else create one.
        UUID distributedNormId = null;
        if (materialCode != null && !materialCode.isBlank()) {
            distributedNormId = findNorm(materialCode.trim(), 2, procurementPlantId);
        }
        if (distributedNormId == null) {
            distributedNormId = findNorm("POWER", 2, procurementPlantId);
        }
        if (distributedNormId == null) {
            distributedNormId = createNorm(
                    materialCode != null && !materialCode.isBlank() ? materialCode.trim() : "POWER",
                    2, procurementPlantId);
        }

        UUID generatedNormId = findNorm("POWERGEN", 1, cppPlantId);
        if (generatedNormId == null) {
            generatedNormId = createNorm("POWERGEN", 1, cppPlantId);
        }

        if (distributedNormId == null || generatedNormId == null) {
            logger.warn("[ADD Capacity Source] Skipped CPP_AssetNorms_Mapping for sourceId={} "
                    + "(distributedNorm={}, generatedNorm={}) — no existing norm to copy attributes from",
                    sourceId, distributedNormId, generatedNormId);
            return;
        }

        importPowerSourceRepository.insertAssetNormsMapping(generatedNormId, sourceId);
        importPowerSourceRepository.insertAssetNormsMapping(distributedNormId, sourceId);
        logger.info("[ADD Capacity Source] CPP_AssetNorms_Mapping rows created for sourceId={} "
                + "(generated={}, distributed={})", sourceId, generatedNormId, distributedNormId);
    }

    /** Look up a NormParameters id by name + plant + normType; null if not found. */
    private UUID findNorm(String name, int normType, UUID plantFkId) {
        return normParametersRepository
                .findFirstByNameAndPlantFkIdAndNormTypeFkId(name, plantFkId, normType)
                .map(NormParameters::getId)
                .orElse(null);
    }

    /**
     * Create a NormParameters row, copying SAPMaterialCode and NormParameterType_FK_Id
     * from an existing norm of the same normType at that plant (follows existing data
     * instead of hardcoding). Returns null if no template norm exists to copy from.
     */
    private UUID createNorm(String name, int normType, UUID plantFkId) {
        NormParameters template = normParametersRepository
                .findByPlantFkId(plantFkId).stream()
                .filter(n -> Integer.valueOf(normType).equals(n.getNormTypeFKId()))
                .findFirst()
                .orElse(null);
        if (template == null) {
            return null; // nothing to copy from — leave unmapped rather than guess
        }

        NormParameters norm = new NormParameters();
        norm.setName(name);
        norm.setDisplayName(name);
        norm.setUom("MW");
        norm.setSapMaterialCode(template.getSapMaterialCode());
        norm.setPlantFkId(plantFkId);
        norm.setNormParameterTypeFkId(template.getNormParameterTypeFkId());
        norm.setNormTypeFKId(normType);
        norm.setIsVisible(true);
        norm.setIsEditable(true);
        return normParametersRepository.save(norm).getId();
    }

    // ========================================
    // NORMS HEADER + CHILD RECORDS (NormsMonthDetail, CPPNorms, CPPMonthWisePrice)
    // Mirrors CPPSRMappingServiceImpl pattern so import-power sources appear in
    // the CPPNorms screen (SP: CPP_GetCPPNorms joins NormsHeader → GeneratingPlants).
    // ========================================

    /** Receiver plant name for import power (NMD - Utility/Power Dist). */
    private static final String IMPORT_POWER_RECEIVER_PLANT_NAME = "NMD - Utility/Power Dist";
    /** Receiver utility name for import power (Power_dis norm, NormType=1). */
    private static final String IMPORT_POWER_UTILITY_NAME = "Power_dis";
    /** Issuing UOM for import power in NormsHeader (capacity × available hours). */
    private static final String IMPORT_POWER_ISSUING_UOM = "KWH";

    /**
     * Creates a NormsHeader row for the import-power source (if absent) and then
     * inserts child records (CPPNorms, NormsMonthDetail, CPPMonthWisePrice) for the
     * given financial year if they don't already exist.
     *
     * NormsHeader column mapping for import power:
     *
     *   Receiver side (Power_dis, NormType=1, at NMD - Utility/Power Dist plant):
     *     Plant_FK_Id                    ← receiver plant (NMD - Utility/Power Dist)
     *     UtilityName                     ← "Power_dis"
     *     UtilityId                       ← Power_dis norm's SAPMaterialCode
     *     UtilityUOM                      ← Power_dis norm's UOM
     *     Utility_NormParameter_FK_Id     ← Power_dis norm's Id
     *
     *   Sender side (source's own norm, NormType=2, at procurement plant):
     *     MaterialName                    ← source name
     *     IssuingPlantName                 ← procurement plant display name
     *     IssuingPlant_FK_Id               ← procurement plant id
     *     NormParameter_FK_Id              ← source's own normparameter id
     *     IssuingUOM                      ← "KWH"
     *
     *   Other:
     *     AccountName                      ← "Utilities" (required by CPP_GetCPPNorms filter)
     *     DisplayOrder                    ← 1
     *     MaterialId                       ← source's SAP material code
     *     Remarks                         ← ""
     *     plantCode                       ← procurement plant code
     *     IsActive                        ← 1
     *     CPP_SR_Mapping_Master_Fk_Id     ← NULL
     */
    private void createNormsHeaderAndChildRecords(UUID procurementPlantId, UUID cppPlantId,
                                                   UUID sourceNormParameterId, String sourceName,
                                                   String sapCode, String aopYear) {
        if (procurementPlantId == null || sourceNormParameterId == null
                || aopYear == null || aopYear.isBlank()) {
            logger.warn("[ADD Capacity Source] Skipped NormsHeader creation – missing required inputs");
            return;
        }

        try {
            // ── Step A: Resolve receiver plant (NMD - Utility/Power Dist) ──
            // The receiver plant stores the CPP plant UUID in its SourceName column
            // (same pattern as procurement plants), so we reuse findBySourceNameInAndIsActiveTrue
            // and filter by name in Java.
            List<Plants> plantsForCpp = plantsRepository
                    .findBySourceNameInAndIsActiveTrue(List.of(cppPlantId.toString()));
            Optional<Plants> receiverPlantOpt = plantsForCpp.stream()
                    .filter(p -> IMPORT_POWER_RECEIVER_PLANT_NAME.equalsIgnoreCase(p.getName()))
                    .findFirst();
            if (receiverPlantOpt.isEmpty()) {
                logger.warn("[ADD Capacity Source] Receiver plant '{}' not found for cppPlant={} — skipping NormsHeader",
                        IMPORT_POWER_RECEIVER_PLANT_NAME, cppPlantId);
                return;
            }
            UUID receiverPlantId = receiverPlantOpt.get().getId();
            logger.info("[ADD Capacity Source] Receiver plant resolved: {} ({})", receiverPlantId,
                    IMPORT_POWER_RECEIVER_PLANT_NAME);

            // ── Step B: Look up Power_dis norm (NormType=1, at receiver plant) ──
            Optional<NormParameters> powerDisNormOpt = normParametersRepository
                    .findFirstByNameAndPlantFkIdAndNormTypeFkId(
                            IMPORT_POWER_UTILITY_NAME, receiverPlantId, 1);
            if (powerDisNormOpt.isEmpty()) {
                logger.warn("[ADD Capacity Source] '{}' norm not found at receiver plant {} — skipping NormsHeader",
                        IMPORT_POWER_UTILITY_NAME, receiverPlantId);
                return;
            }
            NormParameters powerDisNorm = powerDisNormOpt.get();
            logger.info("[ADD Capacity Source] Power_dis norm resolved: {} (SAP={}, UOM={})",
                    powerDisNorm.getId(), powerDisNorm.getSapMaterialCode(), powerDisNorm.getUom());

            // ── Step C: Fetch procurement plant details ──
            Plants procPlant = plantsRepository.findById(procurementPlantId).orElse(null);
            String procPlantName = procPlant != null && procPlant.getDisplayName() != null
                    ? procPlant.getDisplayName()
                    : (procPlant != null ? procPlant.getName() : null);
            String procPlantCode = procPlant != null ? procPlant.getPlantCode() : null;
            String effectiveSapCode = sapCode != null ? sapCode : "";

            // ── Step D: Check if NormsHeader already exists for this receiver plant + source norm ──
            String lookupSql = "SELECT TOP 1 Id FROM NormsHeader WITH(NOLOCK) " +
                    "WHERE Plant_FK_Id = ? AND NormParameter_FK_Id = ?";
            List<String> existing = db1JdbcTemplate.queryForList(lookupSql, String.class,
                    receiverPlantId.toString(), sourceNormParameterId.toString());

            UUID normsHeaderId;
            if (!existing.isEmpty()) {
                normsHeaderId = UUID.fromString(existing.get(0));
                logger.info("[ADD Capacity Source] Reusing existing NormsHeader Id={}", normsHeaderId);
            } else {
                // ── Step E: Insert new NormsHeader ──
                normsHeaderId = UUID.randomUUID();
                String insertSql = "INSERT INTO NormsHeader " +
                        "(Id, Plant_FK_Id, UtilityName, UtilityId, UtilityUOM, AccountName, " +
                        " MaterialName, IssuingPlantName, IssuingPlant_FK_Id, NormParameter_FK_Id, " +
                        " Utility_NormParameter_FK_Id, " +
                        " IsActive, IssuingUOM, DisplayOrder, MaterialId, Remarks, plantCode) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, 1, ?, ?, ?)";
                db1JdbcTemplate.update(insertSql,
                        normsHeaderId.toString(),
                        receiverPlantId.toString(),               // Plant_FK_Id (receiver plant)
                        IMPORT_POWER_UTILITY_NAME,                // UtilityName ("Power_dis")
                        powerDisNorm.getSapMaterialCode(),        // UtilityId (Power_dis SAP code)
                        powerDisNorm.getUom(),                    // UtilityUOM (Power_dis UOM)
                        "Utilities",                              // AccountName
                        sourceName,                               // MaterialName
                        procPlantName,                            // IssuingPlantName
                        procurementPlantId.toString(),            // IssuingPlant_FK_Id
                        sourceNormParameterId.toString(),          // NormParameter_FK_Id (source's own norm)
                        powerDisNorm.getId().toString(),          // Utility_NormParameter_FK_Id (Power_dis norm)
                        IMPORT_POWER_ISSUING_UOM,                 // IssuingUOM ("KWH")
                        effectiveSapCode,                         // MaterialId
                        "",                                       // Remarks
                        procPlantCode                             // plantCode
                );
                logger.info("[ADD Capacity Source] NormsHeader created Id={} for source={}", normsHeaderId, sourceName);
            }

            // ── Step F: Insert child records for the financial year (idempotent) ──
            insertChildRecordsIfAbsent(normsHeaderId, aopYear);

        } catch (Exception e) {
            logger.error("[ADD Capacity Source] Error creating NormsHeader/child records: {}", e.getMessage(), e);
        }
    }

    /**
     * Checks whether child records (CPPNorms, NormsMonthDetail, CPPMonthWisePrice) already exist
     * for the given NormsHeader and financial year.
     * <p>
     * Mirrors CPPSRMappingServiceImpl: checks CPPNorms only — if present, all 3 child tables
     * are assumed to exist and nothing is inserted. If absent, all 3 are inserted.
     *
     * @param normsHeaderId UUID of the NormsHeader row
     * @param financialYear financial year string, e.g. "2026-27"
     */
    private void insertChildRecordsIfAbsent(UUID normsHeaderId, String financialYear) {
        if (normsHeaderId == null || financialYear == null || financialYear.isBlank()) {
            logger.warn("[ADD Capacity Source] insertChildRecordsIfAbsent skipped – normsHeaderId={}, financialYear={}",
                    normsHeaderId, financialYear);
            return;
        }
        try {
            // Check CPPNorms for existing records for this NormsHeader + financialYear
            List<String> existing = db1JdbcTemplate.queryForList(
                    "SELECT TOP 1 Id FROM CPPNorms WITH(NOLOCK) WHERE NormsHeader_FK_Id = ? AND FinancialYear = ?",
                    String.class, normsHeaderId.toString(), financialYear);

            if (!existing.isEmpty()) {
                logger.info("[ADD Capacity Source] Child records already present for NormsHeader={}, financialYear={} — skipping insert",
                        normsHeaderId, financialYear);
                return;
            }

            // No records for this year — insert all 3 child tables
            insertNormsMonthDetails(normsHeaderId, financialYear);
            insertCppNorms(normsHeaderId, financialYear);
            insertCppMonthWisePrice(normsHeaderId, financialYear);
            logger.info("[ADD Capacity Source] Child records inserted for NormsHeader={}, financialYear={}",
                    normsHeaderId, financialYear);

        } catch (Exception e) {
            logger.error("[ADD Capacity Source] insertChildRecordsIfAbsent error for normsHeaderId={}: {}",
                    normsHeaderId, e.getMessage(), e);
        }
    }

    /** Inserts 12 NormsMonthDetail rows (one per month) with zero defaults. */
    private void insertNormsMonthDetails(UUID normsHeaderId, String financialYear) {
        try {
            int startYear = Integer.parseInt(financialYear.split("-")[0].trim());
            int endYear = startYear + 1;

            String fymSql = "SELECT Id FROM FinancialYearMonth " +
                    "WHERE (Year = ? AND Month >= 4) OR (Year = ? AND Month <= 3) " +
                    "ORDER BY Year, Month";
            List<String> fymIds = db1JdbcTemplate.queryForList(fymSql, String.class, startYear, endYear);
            if (fymIds.isEmpty()) {
                logger.warn("[ADD Capacity Source] No FinancialYearMonth records for year={}", financialYear);
                return;
            }

            String insertSql = "INSERT INTO NormsMonthDetail " +
                    "(Id, NormsHeader_FK_Id, FinancialYearMonth_FK_Id, ScenarioType, " +
                    " Norms, Quantity, Amount, Price, DisplayOrder, GenerationUOM, QTY, Remarks) " +
                    "VALUES (?, ?, ?, NULL, 0, 0, 0, 0, 1, NULL, 0, NULL)";

            for (String fymId : fymIds) {
                db1JdbcTemplate.update(insertSql, UUID.randomUUID().toString(),
                        normsHeaderId.toString(), fymId);
            }
        } catch (Exception e) {
            logger.error("[ADD Capacity Source] Error inserting NormsMonthDetail: {}", e.getMessage(), e);
        }
    }

    /** Inserts a default CPPNorms row (NormType_FK_Id=6 'Fixed', all months=0). */
    private void insertCppNorms(UUID normsHeaderId, String financialYear) {
        try {
            String insertSql = "INSERT INTO CPPNorms " +
                    "(Id, NormsHeader_FK_Id, FinancialYear, AOPYear, NormType_FK_Id, " +
                    " Apr_Norms, May_Norms, Jun_Norms, Jul_Norms, Aug_Norms, Sep_Norms, " +
                    " Oct_Norms, Nov_Norms, Dec_Norms, Jan_Norms, Feb_Norms, Mar_Norms, " +
                    " Remarks, CreatedBy, CreatedDate, ModifiedBy, ModifiedDate, ApplyActualNormToAll) " +
                    "VALUES (?, ?, ?, ?, 6, " +
                    " 0, 0, 0, 0, 0, 0, " +
                    " 0, 0, 0, 0, 0, 0, " +
                    " 'Add new record', 'SYSTEM', GETDATE(), 'SYSTEM', GETDATE(), 1)";
            db1JdbcTemplate.update(insertSql, UUID.randomUUID().toString(),
                    normsHeaderId.toString(), financialYear, financialYear);
        } catch (Exception e) {
            logger.error("[ADD Capacity Source] Error inserting CPPNorms: {}", e.getMessage(), e);
        }
    }

    /** Inserts a default CPPMonthWisePrice row (all prices=0). */
    private void insertCppMonthWisePrice(UUID normsHeaderId, String financialYear) {
        try {
            String insertSql = "INSERT INTO CPPMonthWisePrice " +
                    "(Id, NormsHeader_FK_Id, FinancialYear, AOPYear, " +
                    " Apr_Price, May_Price, Jun_Price, Jul_Price, Aug_Price, Sep_Price, " +
                    " Oct_Price, Nov_Price, Dec_Price, Jan_Price, Feb_Price, Mar_Price, " +
                    " Remarks, PriceSource, CreatedDate, UpdatedDate, ModifiedBy, ValueType) " +
                    "VALUES (?, ?, ?, ?, " +
                    " 0, 0, 0, 0, 0, 0, " +
                    " 0, 0, 0, 0, 0, 0, " +
                    " 'Added new norms', 'Calculation', GETDATE(), GETDATE(), 'SYSTEM', 'Calculation')";
            db1JdbcTemplate.update(insertSql, UUID.randomUUID().toString(),
                    normsHeaderId.toString(), financialYear, financialYear);
        } catch (Exception e) {
            logger.error("[ADD Capacity Source] Error inserting CPPMonthWisePrice: {}", e.getMessage(), e);
        }
    }

    /**
     * Deletes child records (CPPNorms, NormsMonthDetail, CPPMonthWisePrice) for the
     * NormsHeader linked to the given source norm, scoped to the given financial year.
     * The NormsHeader row itself is kept (mirrors SR Mapping delete behavior).
     *
     * Lookup: NormsHeader WHERE NormParameter_FK_Id = sourceNormParameterId
     * (the source's own norm, NormType=2, at procurement plant)
     *
     * If financialYear is null/blank, no child records are deleted (preserves all years).
     *
     * @param sourceNormParameterId the source's own NormParameters Id (NormParameter_FK_Id on NormsHeader)
     * @param financialYear         financial year string, e.g. "2026-27" (scopes deletion)
     */
    private void deleteNormsHeaderChildRecords(UUID sourceNormParameterId, String financialYear) {
        if (sourceNormParameterId == null) {
            return;
        }
        if (financialYear == null || financialYear.isBlank()) {
            logger.info("[DELETE Capacity Source] financialYear not provided — skipping NormsHeader child-record deletion for sourceNorm={}",
                    sourceNormParameterId);
            return;
        }
        try {
            // Parse financial year "2026-27" → startYear=2026, endYear=2027
            int startYear;
            int endYear;
            try {
                startYear = Integer.parseInt(financialYear.split("-")[0].trim());
                endYear = startYear + 1;
            } catch (Exception e) {
                logger.error("[DELETE Capacity Source] Could not parse financialYear='{}' — skipping child-record deletion", financialYear);
                return;
            }

            // Find NormsHeader(s) linked to this source's norm parameter
            String lookupSql = "SELECT Id FROM NormsHeader WITH(NOLOCK) WHERE NormParameter_FK_Id = ?";
            List<String> normsHeaderIds = db1JdbcTemplate.queryForList(lookupSql, String.class,
                    sourceNormParameterId.toString());

            if (normsHeaderIds.isEmpty()) {
                logger.info("[DELETE Capacity Source] No NormsHeader found for sourceNorm={}", sourceNormParameterId);
                return;
            }

            String inClause = normsHeaderIds.stream().map(h -> "?").collect(Collectors.joining(","));
            Object[] headerIdArgs = normsHeaderIds.toArray();

            // ── 1. Delete NormsMonthDetail scoped by financialYear via FinancialYearMonth ──
            Object[] monthDetailArgs = new Object[normsHeaderIds.size() + 2];
            System.arraycopy(headerIdArgs, 0, monthDetailArgs, 0, normsHeaderIds.size());
            monthDetailArgs[normsHeaderIds.size()] = startYear;
            monthDetailArgs[normsHeaderIds.size() + 1] = endYear;

            int deletedMonthDetail = db1JdbcTemplate.update(
                    "DELETE nmd FROM NormsMonthDetail nmd " +
                    "INNER JOIN FinancialYearMonth fym WITH(NOLOCK) ON nmd.FinancialYearMonth_FK_Id = fym.Id " +
                    "WHERE nmd.NormsHeader_FK_Id IN (" + inClause + ") " +
                    "AND ((fym.Year = ? AND fym.Month >= 4) OR (fym.Year = ? AND fym.Month <= 3))",
                    monthDetailArgs);
            logger.info("[DELETE Capacity Source] Deleted {} NormsMonthDetail row(s) for financialYear={}",
                    deletedMonthDetail, financialYear);

            // ── 2. Delete CPPNorms scoped by financialYear ──
            Object[] normsArgs = new Object[normsHeaderIds.size() + 1];
            System.arraycopy(headerIdArgs, 0, normsArgs, 0, normsHeaderIds.size());
            normsArgs[normsHeaderIds.size()] = financialYear;

            int deletedNorms = db1JdbcTemplate.update(
                    "DELETE FROM CPPNorms WHERE NormsHeader_FK_Id IN (" + inClause + ") AND FinancialYear = ?",
                    normsArgs);
            logger.info("[DELETE Capacity Source] Deleted {} CPPNorms row(s) for financialYear={}",
                    deletedNorms, financialYear);

            // ── 3. Delete CPPMonthWisePrice scoped by financialYear ──
            Object[] priceArgs = new Object[normsHeaderIds.size() + 1];
            System.arraycopy(headerIdArgs, 0, priceArgs, 0, normsHeaderIds.size());
            priceArgs[normsHeaderIds.size()] = financialYear;

            int deletedMonthWisePrice = db1JdbcTemplate.update(
                    "DELETE FROM CPPMonthWisePrice WHERE NormsHeader_FK_Id IN (" + inClause + ") AND FinancialYear = ?",
                    priceArgs);
            logger.info("[DELETE Capacity Source] Deleted {} CPPMonthWisePrice row(s) for financialYear={}",
                    deletedMonthWisePrice, financialYear);

        } catch (Exception e) {
            logger.error("[DELETE Capacity Source] Error deleting NormsHeader child records: {}", e.getMessage(), e);
        }
    }
}
