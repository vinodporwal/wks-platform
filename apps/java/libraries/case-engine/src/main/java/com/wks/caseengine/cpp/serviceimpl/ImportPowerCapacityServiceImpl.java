package com.wks.caseengine.cpp.serviceimpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    public AOPMessageVM deleteImportPowerCapacitySource(UUID sourceId) {
        logger.info("[DELETE Capacity Source] Soft-deleting sourceId={}", sourceId);

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
}
