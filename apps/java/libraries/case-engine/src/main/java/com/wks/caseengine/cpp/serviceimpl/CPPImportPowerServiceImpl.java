package com.wks.caseengine.cpp.serviceimpl;

import com.wks.caseengine.cpp.dto.CPPImportPowerProjection;
import com.wks.caseengine.cpp.dto.CPPImportPowerResponseDTO;
import com.wks.caseengine.cpp.dto.ImportProcurementPlantProjection;
import com.wks.caseengine.cpp.entity.CPPImportPower;
import com.wks.caseengine.cpp.entity.CPPPowerSourceOperationHours;
import com.wks.caseengine.cpp.repository.CPPImportPowerRepository;
import com.wks.caseengine.cpp.repository.CPPPowerSourceOperationHoursRepository;
import com.wks.caseengine.cpp.service.CPPImportPowerService;
import com.wks.caseengine.dto.AddImportPowerSourceRequestDTO;
import com.wks.caseengine.dto.ImportPowerProcurementPlantDTO;
import com.wks.caseengine.dto.ImportPowerSourceDTO;
import com.wks.caseengine.dto.UpdateImportPowerSourceRequestDTO;
import com.wks.caseengine.entity.NormParameters;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.NormParametersRepository;
import com.wks.caseengine.repository.PlantsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CPPImportPowerServiceImpl implements CPPImportPowerService {

    private static final Logger logger = LoggerFactory.getLogger(CPPImportPowerServiceImpl.class);

    @Autowired
    private CPPImportPowerRepository repository;

    @Autowired
    private CPPPowerSourceOperationHoursRepository powerSourceOperationHoursRepository;

    @Autowired
    private NormParametersRepository normParametersRepository;

    @Autowired
    private PlantsRepository plantsRepository;

    /** Utility norm name (NormType=1) under the CPP plant. */
    private static final String IMPORT_POWER_UTILITY_NAME = "Power_dis";

    /**
     * Resolves the 'Power_dis' utility norm (NormType=1) attached directly to
     * the CPP plant (NormParameters.Plant_FK_Id = cppPlantId).
     * Returns null if the norm cannot be found.
     */
    private UUID resolveUtilityNormParameterId(UUID cppPlantId) {
        try {
            Optional<NormParameters> powerDisNormOpt = normParametersRepository
                    .findFirstByNameAndPlantFkIdAndNormTypeFkId(
                            IMPORT_POWER_UTILITY_NAME, cppPlantId, 1);
            if (powerDisNormOpt.isEmpty()) {
                logger.warn("[Utility Norm] '{}' norm not found at cppPlant={}",
                        IMPORT_POWER_UTILITY_NAME, cppPlantId);
                return null;
            }
            return powerDisNormOpt.get().getId();
        } catch (Exception e) {
            logger.error("[Utility Norm] Error resolving utility norm for cppPlant={}: {}",
                    cppPlantId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    @Transactional
    public AOPMessageVM getImportedPowerPlans(List<UUID> plantIds, String aopYear) {
        logger.info("[GET Service] Fetching imported power plans for plantIds: {}, aopYear: {}", plantIds, aopYear);
        AOPMessageVM aopMessageVM = new AOPMessageVM();

        try {
            List<CPPImportPowerProjection> projections = repository.findImportedPowerPlans(plantIds, aopYear);
            logger.info("[GET Service] Query returned {} records", projections.size());

            // ── TIER 2: CARRY-FORWARD – per plant, clone from previous FY where the requested FY is missing ──
            List<UUID> missingPlantIds = findPlantsWithoutData(plantIds, projections);
            if (!missingPlantIds.isEmpty()) {
                logger.info("[GET Service] No records found for {} for plantIds: {}. Attempting carry-forward from previous financial year.",
                        aopYear, missingPlantIds);
                String previousYear = derivePreviousFinancialYear(aopYear);

                if (previousYear != null) {
                    logger.info("[GET Service] Looking for carry-forward source in financialYear: {}", previousYear);
                    carryForwardImportedPowerPlans(missingPlantIds, previousYear, aopYear);

                    // Re-query after carry-forward attempt
                    projections = repository.findImportedPowerPlans(plantIds, aopYear);
                    logger.info("[GET Service] After carry-forward, re-query returned {} records for {}", projections.size(), aopYear);
                } else {
                    logger.warn("[GET Service] Could not derive previous financial year from '{}'. Skipping carry-forward.", aopYear);
                }
            }

            // ── TIER 3: ZERO-SEED – plants still missing after carry-forward → seed from configured sources ──
            missingPlantIds = findPlantsWithoutData(plantIds, projections);
            if (!missingPlantIds.isEmpty()) {
                logger.info("[GET Service] No carry-forward data found for plantIds: {}. Seeding zero import power plans from configured sources for {}.",
                        missingPlantIds, aopYear);
                seedImportedPowerPlans(missingPlantIds, aopYear);

                // Final re-query to return the seeded data with real IDs
                projections = repository.findImportedPowerPlans(plantIds, aopYear);
                logger.info("[GET Service] After zero-seed, re-query returned {} records for {}", projections.size(), aopYear);
            }

            // Ensure every target-year source has a matching op-hours row — repairs
            // sources whose op-hours are missing (e.g. orphans from older data)
            ensureOpHoursForSources(plantIds, aopYear);

            List<CPPImportPowerResponseDTO> results = projections.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("importedPowerPlans", results);

            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(data);
            logger.info("[GET Service] Successfully fetched {} imported power plans", results.size());
        } catch (Exception e) {
            logger.error("[GET Service] Error fetching imported power plans: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to fetch data: " + e.getMessage());
            aopMessageVM.setData(null);
        }

        return aopMessageVM;
    }

    private CPPImportPowerResponseDTO mapToDto(CPPImportPowerProjection projection) {
        CPPImportPowerResponseDTO dto = new CPPImportPowerResponseDTO();

        dto.setId(projection.getId());
        dto.setProcurementPlant(projection.getProcurementPlant());
        dto.setPlantName(projection.getPlantName());
        dto.setUtilityCode(projection.getUtilityCode());
        dto.setUtilityName(projection.getUtilityName());
        dto.setMaterialCode(projection.getMaterialCode());
        dto.setMaterialName(projection.getMaterialName());
        dto.setUtility(projection.getUtility());
        dto.setMaterial(projection.getMaterial());
        dto.setMaterialDisplayName(projection.getMaterialDisplayName());
        dto.setUom(projection.getUom());

        dto.setApr(projection.getApr());
        dto.setMay(projection.getMay());
        dto.setJun(projection.getJun());
        dto.setJul(projection.getJul());
        dto.setAug(projection.getAug());
        dto.setSep(projection.getSep());
        dto.setOct(projection.getOct());
        dto.setNov(projection.getNov());
        dto.setDec(projection.getDec());
        dto.setJan(projection.getJan());
        dto.setFeb(projection.getFeb());
        dto.setMar(projection.getMar());

        dto.setAopYear(projection.getAopYear());
        dto.setSiteFkId(projection.getSiteFkId());
        dto.setVerticalFkId(projection.getVerticalFkId());
        dto.setRemarks(projection.getRemarks());
        dto.setCreatedDate(projection.getCreatedDate());
        dto.setUpdatedDate(projection.getUpdatedDate());
        dto.setImportPlantFkId(projection.getImportPlantFkId());
        dto.setCppPlantFkId(projection.getCppPlantFkId());
        dto.setNormParameterFkId(projection.getNormParameterFkId());
        dto.setUtilityNormParameterFkId(projection.getUtilityNormParameterFkId());

        return dto;
    }

    /**
     * Returns the subset of plantIds that have no imported power plan rows in
     * the given projection result — these plants are candidates for
     * carry-forward / zero-seed.
     */
    private List<UUID> findPlantsWithoutData(List<UUID> plantIds, List<CPPImportPowerProjection> projections) {
        Set<UUID> plantsWithData = projections.stream()
                .map(CPPImportPowerProjection::getCppPlantFkId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return plantIds.stream()
                .filter(plantId -> !plantsWithData.contains(plantId))
                .collect(Collectors.toList());
    }

    /**
     * Derives the previous financial year string.
     * Expected format: "YYYY-YY" (e.g. "2026-27" → "2025-26", "2025-26" → "2024-25").
     * Returns null if the format is unrecognised.
     */
    private String derivePreviousFinancialYear(String financialYear) {
        try {
            String[] parts = financialYear.split("-");
            if (parts.length != 2) return null;
            int startYear = Integer.parseInt(parts[0]);
            int prevStart = startYear - 1;
            int prevEnd = prevStart + 1;
            String prevEndSuffix = String.format("%02d", prevEnd % 100);
            return prevStart + "-" + prevEndSuffix;
        } catch (Exception e) {
            logger.warn("[GET Service] Could not parse financial year '{}': {}", financialYear, e.getMessage());
            return null;
        }
    }

    /**
     * Clones all previous-FY CPPImportPower records (and their linked
     * CPPPowerSourceOperationHours rows) into the requested FY.
     * Records whose NormParameter source has been soft-deleted (isVisible = 0)
     * are skipped so hidden sources are not resurrected.
     */
    private void carryForwardImportedPowerPlans(List<UUID> plantIds, String previousYear, String aopYear) {
        List<CPPImportPower> prevRecords = repository.findByCppPlantFkIdInAndAopYear(plantIds, previousYear);
        if (prevRecords.isEmpty()) {
            logger.info("[GET Service] No previous-year records found in {} for plantIds: {}", previousYear, plantIds);
            return;
        }

        // Only carry forward sources that are still visible
        Set<UUID> visibleNormIds = normParametersRepository.findAllById(
                        prevRecords.stream()
                                .map(CPPImportPower::getNormParameterFkId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()))
                .stream()
                .filter(norm -> !Boolean.FALSE.equals(norm.getIsVisible()))
                .map(NormParameters::getId)
                .collect(Collectors.toSet());

        // Records that already exist for the target year — a row with the same
        // (cppPlantFkId, normParameterFkId, aopYear) is a duplicate, so skip it.
        Map<String, CPPImportPower> existingByKey = repository
                .findByCppPlantFkIdInAndAopYear(plantIds, aopYear)
                .stream()
                .collect(Collectors.toMap(
                        r -> r.getCppPlantFkId() + "|" + r.getNormParameterFkId(),
                        r -> r,
                        (a, b) -> a));

        for (UUID plantId : plantIds) {
            List<CPPImportPower> plantPrevRecords = prevRecords.stream()
                    .filter(r -> plantId.equals(r.getCppPlantFkId()))
                    .filter(r -> r.getNormParameterFkId() == null || visibleNormIds.contains(r.getNormParameterFkId()))
                    .collect(Collectors.toList());
            if (plantPrevRecords.isEmpty()) continue;

            logger.info("[GET Service] Carrying forward {} import power records for plantId: {} from {}",
                    plantPrevRecords.size(), plantId, previousYear);

            // Previous-FY operational hours keyed by their PowerSource_FK_Id (CPPImportPower.Id)
            Map<UUID, List<CPPPowerSourceOperationHours>> prevOpHoursBySource =
                    powerSourceOperationHoursRepository.findByPlantFkIdAndAopYear(plantId, previousYear)
                            .stream()
                            .filter(o -> o.getAssetFkId() != null)
                            .collect(Collectors.groupingBy(
                                    CPPPowerSourceOperationHours::getAssetFkId));

            // Op-hours already present for the target year — never add a second row per source+year
            Set<UUID> existingOpHourTargets =
                    powerSourceOperationHoursRepository.findByPlantFkIdAndAopYear(plantId, aopYear)
                            .stream()
                            .map(CPPPowerSourceOperationHours::getAssetFkId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());

            List<CPPImportPower> clones = new ArrayList<>();
            List<UUID> clonePrevSourceIds = new ArrayList<>();
            List<CPPImportPower> backfillSources = new ArrayList<>();
            List<UUID> backfillPrevSourceIds = new ArrayList<>();

            for (CPPImportPower src : plantPrevRecords) {
                String key = src.getCppPlantFkId() + "|" + src.getNormParameterFkId();
                CPPImportPower existing = existingByKey.get(key);
                if (existing != null) {
                    // Same source + year already exists — don't duplicate the plan row,
                    // only backfill op-hours if the target year is missing them
                    if (existingOpHourTargets.add(existing.getId())) {
                        backfillSources.add(existing);
                        backfillPrevSourceIds.add(src.getId());
                    }
                    continue;
                }

                CPPImportPower clone = new CPPImportPower();
               
                clone.setImportPlantFkId(src.getImportPlantFkId());
                clone.setNormParameterFkId(src.getNormParameterFkId());
                clone.setUtilityNormParameterFkId(src.getUtilityNormParameterFkId());
                clone.setCppPlantFkId(src.getCppPlantFkId());
                clone.setApr(src.getApr());
                clone.setMay(src.getMay());
                clone.setJun(src.getJun());
                clone.setJul(src.getJul());
                clone.setAug(src.getAug());
                clone.setSep(src.getSep());
                clone.setOct(src.getOct());
                clone.setNov(src.getNov());
                clone.setDec(src.getDec());
                clone.setJan(src.getJan());
                clone.setFeb(src.getFeb());
                clone.setMar(src.getMar());
                clone.setAopYear(aopYear);
                clone.setRemarks(src.getRemarks());
                clone.setSiteFkId(src.getSiteFkId());
                clone.setVerticalFkId(src.getVerticalFkId());
                clone.setCreatedDate(LocalDateTime.now());
                clone.setUpdatedDate(LocalDateTime.now());
                clones.add(clone);
                clonePrevSourceIds.add(src.getId());
                existingByKey.put(key, clone);
            }

            // Persist the sources first so the generated ids are known before
            // building the linked op-hours rows (PowerSource_FK_Id)
            List<CPPImportPower> savedClones = clones.isEmpty() ? new ArrayList<>() : repository.saveAll(clones);

            List<CPPPowerSourceOperationHours> opHourWrites = new ArrayList<>();
            for (int i = 0; i < savedClones.size(); i++) {
                UUID prevSourceId = clonePrevSourceIds.get(i);
                List<CPPPowerSourceOperationHours> srcOpHours = prevOpHoursBySource.get(prevSourceId);
                if (srcOpHours != null && !srcOpHours.isEmpty()) {
                    // (PowerSource_FK_Id, AOPYear) must stay unique — clone only one row per source
                    if (srcOpHours.size() > 1) {
                        logger.warn("[GET Service] {} duplicate op-hours found for source {} in {} — cloning only one",
                                srcOpHours.size(), prevSourceId, previousYear);
                    }
                    opHourWrites.add(cloneImportPowerOpHours(srcOpHours.get(0), savedClones.get(i).getId(), aopYear));
                } else {
                    // addImportPowerSource always creates a matching op-hours row — keep the invariant
                    opHourWrites.add(buildImportPowerOpHoursSeed(savedClones.get(i)));
                }
            }
            for (int i = 0; i < backfillSources.size(); i++) {
                UUID prevSourceId = backfillPrevSourceIds.get(i);
                List<CPPPowerSourceOperationHours> srcOpHours = prevOpHoursBySource.get(prevSourceId);
                if (srcOpHours != null && !srcOpHours.isEmpty()) {
                    if (srcOpHours.size() > 1) {
                        logger.warn("[GET Service] {} duplicate op-hours found for source {} in {} — cloning only one",
                                srcOpHours.size(), prevSourceId, previousYear);
                    }
                    opHourWrites.add(cloneImportPowerOpHours(srcOpHours.get(0), backfillSources.get(i).getId(), aopYear));
                } else {
                    opHourWrites.add(buildImportPowerOpHoursSeed(backfillSources.get(i)));
                }
            }

            if (!opHourWrites.isEmpty()) {
                powerSourceOperationHoursRepository.saveAll(opHourWrites);
            }
            logger.info("[GET Service] Saved {} carry-forward import power records (+ {} op-hours) for plantId: {}",
                    clones.size(), opHourWrites.size(), plantId);
        }
    }

    /**
     * Seeds zero-value CPPImportPower records from the configured procurement
     * plants and their visible NormParameter sources (same shape that
     * addImportPowerSource creates), plus the linked zero op-hours row.
     */
    private void seedImportedPowerPlans(List<UUID> plantIds, String aopYear) {
        // Records that already exist for the target year — a row with the same
        // (cppPlantFkId, normParameterFkId, aopYear) is a duplicate, so skip it.
        Map<String, CPPImportPower> existingByKey = repository
                .findByCppPlantFkIdInAndAopYear(plantIds, aopYear)
                .stream()
                .collect(Collectors.toMap(
                        r -> r.getCppPlantFkId() + "|" + r.getNormParameterFkId(),
                        r -> r,
                        (a, b) -> a));

        for (UUID plantId : plantIds) {
            List<ImportProcurementPlantProjection> sourceRows = repository.getProcurementPlantsWithSources(plantId)
                    .stream()
                    .filter(r -> r.getNormParameterId() != null)
                    .collect(Collectors.toList());
            if (sourceRows.isEmpty()) {
                logger.warn("[GET Service] No configured import sources found for plantId: {}. Skipping zero-seed.", plantId);
                continue;
            }

            // Op-hours already present for the target year — never add a second row per source+year
            Set<UUID> existingOpHourTargets =
                    powerSourceOperationHoursRepository.findByPlantFkIdAndAopYear(plantId, aopYear)
                            .stream()
                            .map(CPPPowerSourceOperationHours::getAssetFkId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());

            Optional<Plants> cppPlantOpt = plantsRepository.findById(plantId);
            UUID siteFkId = cppPlantOpt.map(Plants::getSiteFkId).orElse(null);
            UUID verticalFkId = cppPlantOpt.map(Plants::getVerticalFKId).orElse(null);
            UUID utilityNormId = resolveUtilityNormParameterId(plantId);

            List<CPPImportPower> seeds = new ArrayList<>();
            List<CPPPowerSourceOperationHours> opHourSeeds = new ArrayList<>();
            for (ImportProcurementPlantProjection row : sourceRows) {
                String key = plantId + "|" + row.getNormParameterId();
                CPPImportPower existing = existingByKey.get(key);
                if (existing != null) {
                    // Same source + year already exists — only backfill op-hours if missing
                    if (!existingOpHourTargets.contains(existing.getId())) {
                        opHourSeeds.add(buildImportPowerOpHoursSeed(existing));
                        existingOpHourTargets.add(existing.getId());
                    }
                    continue;
                }

                CPPImportPower seed = new CPPImportPower();
                // NOTE: no manual id — see carryForwardImportedPowerPlans; a pre-set id
                // makes save() merge() and replace it with a different generated value
                seed.setImportPlantFkId(row.getProcurementPlantId());
                seed.setNormParameterFkId(row.getNormParameterId());
                seed.setUtilityNormParameterFkId(utilityNormId);
                seed.setCppPlantFkId(plantId);
                seed.setSiteFkId(siteFkId);
                seed.setVerticalFkId(verticalFkId);
                seed.setAopYear(aopYear);
                seed.setRemarks(null);
                seed.setCreatedDate(LocalDateTime.now());
                seed.setUpdatedDate(LocalDateTime.now());
                BigDecimal zero = BigDecimal.ZERO;
                seed.setApr(zero);  seed.setMay(zero);  seed.setJun(zero);
                seed.setJul(zero);  seed.setAug(zero);  seed.setSep(zero);
                seed.setOct(zero);  seed.setNov(zero);  seed.setDec(zero);
                seed.setJan(zero);  seed.setFeb(zero);  seed.setMar(zero);
                seeds.add(seed);
                existingByKey.put(key, seed);
            }

            // Persist first so generated ids are known, then build op-hours rows
            // pointing at the real persisted ids
            List<CPPImportPower> savedSeeds = seeds.isEmpty() ? new ArrayList<>() : repository.saveAll(seeds);
            for (CPPImportPower savedSeed : savedSeeds) {
                opHourSeeds.add(buildImportPowerOpHoursSeed(savedSeed));
            }
            if (!opHourSeeds.isEmpty()) {
                powerSourceOperationHoursRepository.saveAll(opHourSeeds);
            }
            logger.info("[GET Service] Inserted {} zero-seeded import power records (+ {} op-hours) for plantId: {} in {}",
                    seeds.size(), opHourSeeds.size(), plantId, aopYear);
        }
    }

    /**
     * Ensures every target-year CPPImportPower source has a linked
     * CPPPowerSourceOperationHours row for the same year. For uncovered sources
     * the row is cloned from the previous year's op-hours (matched by
     * cppPlantFkId + normParameterFkId); if none exists, a zero row is seeded.
     */
    private void ensureOpHoursForSources(List<UUID> plantIds, String aopYear) {
        List<CPPImportPower> targetSources = repository.findByCppPlantFkIdInAndAopYear(plantIds, aopYear);
        if (targetSources.isEmpty()) return;

        Set<UUID> covered = powerSourceOperationHoursRepository
                .findByPlantFkIdInAndAopYear(plantIds, aopYear)
                .stream()
                .map(CPPPowerSourceOperationHours::getAssetFkId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<CPPImportPower> uncovered = targetSources.stream()
                .filter(s -> !covered.contains(s.getId()))
                .collect(Collectors.toList());
        if (uncovered.isEmpty()) return;

        logger.info("[GET Service] {} source(s) missing op-hours for {} — backfilling", uncovered.size(), aopYear);

        String previousYear = derivePreviousFinancialYear(aopYear);
        Map<String, CPPImportPower> prevSourceByKey = new HashMap<>();
        Map<UUID, List<CPPPowerSourceOperationHours>> prevOpHoursBySource = new HashMap<>();
        if (previousYear != null) {
            prevSourceByKey = repository
                    .findByCppPlantFkIdInAndAopYear(plantIds, previousYear)
                    .stream()
                    .collect(Collectors.toMap(
                            s -> s.getCppPlantFkId() + "|" + s.getNormParameterFkId(),
                            s -> s, (a, b) -> a));
            prevOpHoursBySource = powerSourceOperationHoursRepository
                    .findByPlantFkIdInAndAopYear(plantIds, previousYear)
                    .stream()
                    .filter(o -> o.getAssetFkId() != null)
                    .collect(Collectors.groupingBy(CPPPowerSourceOperationHours::getAssetFkId));
        }

        List<CPPPowerSourceOperationHours> backfill = new ArrayList<>();
        for (CPPImportPower source : uncovered) {
            String key = source.getCppPlantFkId() + "|" + source.getNormParameterFkId();
            CPPImportPower prevSource = prevSourceByKey.get(key);
            List<CPPPowerSourceOperationHours> prevOpHours =
                    prevSource == null ? null : prevOpHoursBySource.get(prevSource.getId());
            if (prevOpHours != null && !prevOpHours.isEmpty()) {
                backfill.add(cloneImportPowerOpHours(prevOpHours.get(0), source.getId(), aopYear));
            } else {
                backfill.add(buildImportPowerOpHoursSeed(source));
            }
        }

        powerSourceOperationHoursRepository.saveAll(backfill);
        logger.info("[GET Service] Backfilled {} op-hours rows for {}", backfill.size(), aopYear);
    }

    /** Clones a previous-FY op-hours row, re-pointing it at the new CPPImportPower id. */
    private CPPPowerSourceOperationHours cloneImportPowerOpHours(
            CPPPowerSourceOperationHours src, UUID newPowerSourceId, String aopYear) {
        CPPPowerSourceOperationHours clone =
                new CPPPowerSourceOperationHours();
        clone.setId(UUID.randomUUID());
        clone.setAssetFkId(newPowerSourceId);
        clone.setApr(src.getApr());
        clone.setMay(src.getMay());
        clone.setJun(src.getJun());
        clone.setJul(src.getJul());
        clone.setAug(src.getAug());
        clone.setSep(src.getSep());
        clone.setOct(src.getOct());
        clone.setNov(src.getNov());
        clone.setDec(src.getDec());
        clone.setJan(src.getJan());
        clone.setFeb(src.getFeb());
        clone.setMar(src.getMar());
        clone.setAopYear(aopYear);
        clone.setRemarks(src.getRemarks());
        clone.setSiteFkId(src.getSiteFkId());
        clone.setVerticalFkId(src.getVerticalFkId());
        clone.setPlantFkId(src.getPlantFkId());
        clone.setCreatedDate(LocalDateTime.now());
        clone.setModifiedDate(LocalDateTime.now());
        return clone;
    }

    /** Builds a zero-value op-hours row linked to the given CPPImportPower record. */
    private CPPPowerSourceOperationHours buildImportPowerOpHoursSeed(CPPImportPower importPower) {
        CPPPowerSourceOperationHours opHours =
                new CPPPowerSourceOperationHours();
        opHours.setId(UUID.randomUUID());
        opHours.setAssetFkId(importPower.getId());
        opHours.setApr(0.0);  opHours.setMay(0.0);  opHours.setJun(0.0);
        opHours.setJul(0.0);  opHours.setAug(0.0);  opHours.setSep(0.0);
        opHours.setOct(0.0);  opHours.setNov(0.0);  opHours.setDec(0.0);
        opHours.setJan(0.0);  opHours.setFeb(0.0);  opHours.setMar(0.0);
        opHours.setAopYear(importPower.getAopYear());
        opHours.setRemarks(null);
        opHours.setSiteFkId(importPower.getSiteFkId());
        opHours.setVerticalFkId(importPower.getVerticalFkId());
        opHours.setPlantFkId(importPower.getCppPlantFkId());
        opHours.setCreatedDate(LocalDateTime.now());
        opHours.setModifiedDate(LocalDateTime.now());
        return opHours;
    }

    @Override
    @Transactional
    public AOPMessageVM saveImportedPowerPlans(List<UUID> plantIds, String aopYear, List<CPPImportPowerResponseDTO> payload) {
        logger.info("[POST Service] Saving imported power plans for plantIds: {}, aopYear: {}", plantIds, aopYear);
        AOPMessageVM aopMessageVM = new AOPMessageVM();

        try {
            int saved = 0;
            int unchanged = 0;
            int skipped = 0;
            List<Map<String, String>> missingIdRecords = new ArrayList<>();

            if (payload != null) {
                logger.info("[POST Service] Processing {} imported power plan records", payload.size());
                for (CPPImportPowerResponseDTO dto : payload) {
                    if (dto.getId() == null) {
                        skipped++;
                        logger.warn("[POST Service] Skipping record without ID - ProcurementPlant: {}",
                                dto.getProcurementPlant() != null ? dto.getProcurementPlant() : "Unknown");

                        Map<String, String> missingRecord = new HashMap<>();
                        missingRecord.put("procurementPlant", dto.getProcurementPlant() != null ? dto.getProcurementPlant() : "Unknown");
                        missingRecord.put("materialCode", dto.getMaterialCode() != null ? dto.getMaterialCode() : "Unknown");
                        missingRecord.put("reason", "ID is null - cannot update non-existent record");
                        missingIdRecords.add(missingRecord);
                        continue;
                    }

                    // Check if record was actually modified
                    var optionalEntity = repository.findById(dto.getId());
                    if (optionalEntity.isPresent() && !isRecordModified(dto, optionalEntity.get())) {
                        unchanged++;
                        logger.debug("[POST Service] Skipping unchanged record: {}", dto.getProcurementPlant());
                        continue;
                    }

                    boolean result = saveImportedPowerPlan(dto);
                    if (result) {
                        saved++;
                    } else {
                        skipped++;
                        Map<String, String> missingRecord = new HashMap<>();
                        missingRecord.put("id", dto.getId().toString());
                        missingRecord.put("procurementPlant", dto.getProcurementPlant() != null ? dto.getProcurementPlant() : "Unknown");
                        missingRecord.put("materialCode", dto.getMaterialCode() != null ? dto.getMaterialCode() : "Unknown");
                        missingRecord.put("reason", "Record with this ID does not exist in database");
                        missingIdRecords.add(missingRecord);
                    }
                }
                logger.info("[POST Service] Imported power plans - Saved: {}, Unchanged: {}, Skipped: {}", saved, unchanged, skipped);
            } else {
                logger.info("[POST Service] No imported power plans to process");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("saved", saved);
            data.put("unchanged", unchanged);
            data.put("skipped", skipped);
            data.put("totalProcessed", saved + unchanged + skipped);

            if (!missingIdRecords.isEmpty()) {
                data.put("skippedRecords", missingIdRecords);
            }

            if (missingIdRecords.isEmpty()) {
                aopMessageVM.setCode(200);
                if (saved == 0 && unchanged > 0) {
                    aopMessageVM.setMessage("No changes detected. All " + unchanged + " records unchanged.");
                } else {
                    aopMessageVM.setMessage("Imported power plans saved successfully. " + unchanged + " unchanged, " + saved + " updated.");
                }
            } else {
                aopMessageVM.setCode(207);
                aopMessageVM.setMessage(String.format("Partial success: %d saved, %d unchanged, %d skipped (missing or invalid IDs)",
                        saved, unchanged, skipped));
            }

            aopMessageVM.setData(data);
            logger.info("[POST Service] Save operation completed - Saved: {}, Unchanged: {}, Skipped: {}",
                    saved, unchanged, skipped);

        } catch (Exception e) {
            logger.error("[POST Service] Error saving imported power plans: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to save imported power plans: " + e.getMessage());
            aopMessageVM.setData(null);
        }

        return aopMessageVM;
    }

    private String generatePowerPlanHash(CPPImportPowerResponseDTO dto) {
        try {
            StringBuilder dataToHash = new StringBuilder();
            dataToHash.append(dto.getApr() != null ? dto.getApr().stripTrailingZeros().toPlainString() : "null").append("|");
            dataToHash.append(dto.getMay() != null ? dto.getMay().stripTrailingZeros().toPlainString() : "null").append("|");
            dataToHash.append(dto.getJun() != null ? dto.getJun().stripTrailingZeros().toPlainString() : "null").append("|");
            dataToHash.append(dto.getJul() != null ? dto.getJul().stripTrailingZeros().toPlainString() : "null").append("|");
            dataToHash.append(dto.getAug() != null ? dto.getAug().stripTrailingZeros().toPlainString() : "null").append("|");
            dataToHash.append(dto.getSep() != null ? dto.getSep().stripTrailingZeros().toPlainString() : "null").append("|");
            dataToHash.append(dto.getOct() != null ? dto.getOct().stripTrailingZeros().toPlainString() : "null").append("|");
            dataToHash.append(dto.getNov() != null ? dto.getNov().stripTrailingZeros().toPlainString() : "null").append("|");
            dataToHash.append(dto.getDec() != null ? dto.getDec().stripTrailingZeros().toPlainString() : "null").append("|");
            dataToHash.append(dto.getJan() != null ? dto.getJan().stripTrailingZeros().toPlainString() : "null").append("|");
            dataToHash.append(dto.getFeb() != null ? dto.getFeb().stripTrailingZeros().toPlainString() : "null").append("|");
            dataToHash.append(dto.getMar() != null ? dto.getMar().stripTrailingZeros().toPlainString() : "null").append("|");
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

    private String generatePowerPlanHash(CPPImportPower entity) {
        try {
            StringBuilder dataToHash = new StringBuilder();
            dataToHash.append(entity.getApr() != null ? entity.getApr().stripTrailingZeros().toPlainString() : "null").append("|");
            dataToHash.append(entity.getMay() != null ? entity.getMay().stripTrailingZeros().toPlainString() : "null").append("|");
            dataToHash.append(entity.getJun() != null ? entity.getJun().stripTrailingZeros().toPlainString() : "null").append("|");
            dataToHash.append(entity.getJul() != null ? entity.getJul().stripTrailingZeros().toPlainString() : "null").append("|");
            dataToHash.append(entity.getAug() != null ? entity.getAug().stripTrailingZeros().toPlainString() : "null").append("|");
            dataToHash.append(entity.getSep() != null ? entity.getSep().stripTrailingZeros().toPlainString() : "null").append("|");
            dataToHash.append(entity.getOct() != null ? entity.getOct().stripTrailingZeros().toPlainString() : "null").append("|");
            dataToHash.append(entity.getNov() != null ? entity.getNov().stripTrailingZeros().toPlainString() : "null").append("|");
            dataToHash.append(entity.getDec() != null ? entity.getDec().stripTrailingZeros().toPlainString() : "null").append("|");
            dataToHash.append(entity.getJan() != null ? entity.getJan().stripTrailingZeros().toPlainString() : "null").append("|");
            dataToHash.append(entity.getFeb() != null ? entity.getFeb().stripTrailingZeros().toPlainString() : "null").append("|");
            dataToHash.append(entity.getMar() != null ? entity.getMar().stripTrailingZeros().toPlainString() : "null").append("|");
            dataToHash.append(entity.getRemarks() != null ? entity.getRemarks() : "null");

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

    private boolean isRecordModified(CPPImportPowerResponseDTO dto, CPPImportPower entity) {
        try {
            String dbHash = generatePowerPlanHash(entity);
            String importedHash = generatePowerPlanHash(dto);

            boolean modified = !dbHash.equals(importedHash);
            if (!modified) {
                logger.debug("[isRecordModified] Record {} unchanged - hash match", dto.getId());
            } else {
                logger.debug("[isRecordModified] Record {} modified - hash mismatch", dto.getId());
            }
            return modified;
        } catch (Exception e) {
            logger.error("[isRecordModified] Error comparing record ID {}: {}", dto.getId(), e.getMessage());
            return true;
        }
    }

    private boolean saveImportedPowerPlan(CPPImportPowerResponseDTO dto) {
        logger.debug("[POST Service] Updating existing record with ID: {}", dto.getId());

        var optionalEntity = repository.findById(dto.getId());
        if (optionalEntity.isEmpty()) {
            logger.error("[POST Service] Record with ID {} not found in database", dto.getId());
            return false;
        }

        CPPImportPower entity = optionalEntity.get();
        entity.setUpdatedDate(LocalDateTime.now());

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

        entity.setRemarks(dto.getRemarks());

        CPPImportPower saved = repository.save(entity);
        logger.debug("[POST Service] Successfully updated entity with ID: {}", saved.getId());
        return true;
    }

    // ========================================
    // EXPORT IMPORTED POWER PLANS
    // ========================================

    @Override
    public byte[] exportImportedPowerPlans(List<UUID> plantIds, String aopYear) {
        logger.info("[Export Power Plans] Exporting for plantIds: {}, aopYear: {}", plantIds, aopYear);

        try {
            AOPMessageVM response = getImportedPowerPlans(plantIds, aopYear);

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.getData();

            @SuppressWarnings("unchecked")
            List<CPPImportPowerResponseDTO> powerPlans =
                (List<CPPImportPowerResponseDTO>) data.get("importedPowerPlans");

            // Sort by plantName, then procurementPlant
            if (powerPlans != null && !powerPlans.isEmpty()) {
                powerPlans.sort(Comparator
                    .comparing(CPPImportPowerResponseDTO::getPlantName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(CPPImportPowerResponseDTO::getProcurementPlant,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                logger.debug("[Export Power Plans] Sorted {} records", powerPlans.size());
            }

            logger.info("[Export Power Plans] Generating Excel for {} records", powerPlans != null ? powerPlans.size() : 0);
            return generatePowerPlanExcel(powerPlans, "Imported Power Plans", aopYear);

        } catch (Exception e) {
            logger.error("[Export Power Plans] Error exporting: {}", e.getMessage(), e);
            return null;
        }
    }

    // ========================================
    // IMPORT IMPORTED POWER PLANS
    // ========================================

    @Override
    public AOPMessageVM importImportedPowerPlans(List<UUID> plantIds, String aopYear, MultipartFile file) {
        logger.info("[Import Power Plans] Importing for plantIds: {}, aopYear: {}, file: {}",
                plantIds, aopYear, file.getOriginalFilename());

        AOPMessageVM response = new AOPMessageVM();

        try {
            List<CPPImportPowerResponseDTO> excelData = readPowerPlanExcel(file.getInputStream(), aopYear);
            logger.info("[Import Power Plans] Read {} records from Excel", excelData.size());

            List<CPPImportPowerResponseDTO> validRecords = new ArrayList<>();
            List<CPPImportPowerResponseDTO> failedRecords = new ArrayList<>();
            List<String> failureReasons = new ArrayList<>();
            int skippedCount = 0;

            for (CPPImportPowerResponseDTO dto : excelData) {
                if (!isPowerPlanRecordModifiedForImport(dto)) {
                    skippedCount++;
                    logger.debug("[Import Power Plans] Skipping unchanged record: {}", dto.getProcurementPlant());
                    continue;
                }

                String validationError = validatePowerPlanData(dto);
                if (validationError != null) {
                    failedRecords.add(dto);
                    failureReasons.add(validationError);
                    logger.warn("[Import Power Plans] Invalid record - {}: {}", dto.getProcurementPlant(), validationError);
                } else {
                    validRecords.add(dto);
                }
            }

            logger.info("[Import Power Plans] {} unchanged, {} modified to process", skippedCount, excelData.size() - skippedCount);

            if (!validRecords.isEmpty()) {
                try {
                    AOPMessageVM saveResult = saveImportedPowerPlans(plantIds, aopYear, validRecords);

                    if (saveResult.getCode() == 207) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> saveData = (Map<String, Object>) saveResult.getData();
                        if (saveData != null && saveData.containsKey("skippedRecords")) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, String>> skippedRecords =
                                    (List<Map<String, String>>) saveData.get("skippedRecords");
                            logger.warn("[Import Power Plans] {} records skipped during save", skippedRecords.size());
                        }
                    }

                    logger.info("[Import Power Plans] Successfully processed {} records", validRecords.size());
                } catch (Exception e) {
                    logger.error("[Import Power Plans] Error saving records: {}", e.getMessage(), e);
                    for (CPPImportPowerResponseDTO failedDto : validRecords) {
                        failedRecords.add(failedDto);
                        failureReasons.add("Save failed: " + e.getMessage());
                    }
                }
            }

            if (failedRecords.isEmpty()) {
                response.setCode(200);
                if (validRecords.isEmpty() && skippedCount > 0) {
                    response.setMessage("No changes detected. All " + skippedCount + " records unchanged.");
                } else {
                    response.setMessage("All imported power plans saved successfully. "
                            + skippedCount + " records unchanged, " + validRecords.size() + " records updated.");
                }
            } else {
                byte[] failedRecordsFile = generatePowerPlanErrorExcel(failedRecords, failureReasons,
                        "Imported Power Plans", aopYear);
                String base64File = java.util.Base64.getEncoder().encodeToString(failedRecordsFile);
                response.setCode(400);
                response.setMessage("Partial import: " + validRecords.size() + " saved, " + failedRecords.size()
                        + " failed, " + skippedCount + " unchanged. Download file for details.");
                response.setData(base64File);
                logger.info("[Import Power Plans] Exported {} failed records to Excel", failedRecords.size());
            }

            logger.info("[Import Power Plans] Completed - Unchanged: {}, Saved: {}, Failed: {}",
                    skippedCount, validRecords.size(), failedRecords.size());
        } catch (Exception e) {
            logger.error("[Import Power Plans] Error during import: {}", e.getMessage(), e);
            response.setCode(500);
            response.setMessage("Failed to import power plans: " + e.getMessage());
        }

        return response;
    }

    // ========================================
    // EXCEL GENERATION HELPER
    // ========================================

    private byte[] generatePowerPlanExcel(List<CPPImportPowerResponseDTO> dataList, String sheetName, String aopYear) throws Exception {
        logger.info("[Excel Generation] Creating workbook: {} with {} records", sheetName, dataList != null ? dataList.size() : 0);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle remarksStyle = createRemarksStyle(workbook);

        String startYearSuffix = aopYear.substring(2, 4);
        String endYearSuffix = aopYear.substring(5, 7);

        int currentRow = 0;
        int col = 0;

        // Header row
        Row headerRow = sheet.createRow(currentRow++);
        col = 0;

        createCell(headerRow, col++, "Procurement Plant", headerStyle);
        createCell(headerRow, col++, "Plant Name", headerStyle);
        createCell(headerRow, col++, "Utility", headerStyle);
        createCell(headerRow, col++, "Material", headerStyle);
        createCell(headerRow, col++, "UOM", headerStyle);

        String[] months = {"Apr-" + startYearSuffix, "May-" + startYearSuffix, "Jun-" + startYearSuffix, "Jul-" + startYearSuffix,
                "Aug-" + startYearSuffix, "Sep-" + startYearSuffix, "Oct-" + startYearSuffix, "Nov-" + startYearSuffix,
                "Dec-" + startYearSuffix, "Jan-" + endYearSuffix, "Feb-" + endYearSuffix, "Mar-" + endYearSuffix};

        for (String month : months) {
            createCell(headerRow, col++, month, headerStyle);
        }

        int remarksCol = col;
        createCell(headerRow, col++, "Remarks", headerStyle);
        createCell(headerRow, col++, "id", headerStyle);
        int idCol = col - 1;
        createCell(headerRow, col++, "importPlantFkId", headerStyle);
        int importPlantFkIdCol = col - 1;
        createCell(headerRow, col++, "cppPlantFkId", headerStyle);
        int cppPlantFkIdCol = col - 1;
        createCell(headerRow, col++, "normParameterFkId", headerStyle);
        int normParameterFkIdCol = col - 1;

        int totalColumns = col;

        // Data rows
        int rowCount = 0;
        for (CPPImportPowerResponseDTO dto : dataList) {
            rowCount++;
            Row row = sheet.createRow(currentRow++);
            col = 0;
            logger.debug("[Excel Generation] Writing row {} for plant: {}", rowCount, dto.getProcurementPlant());

            createCell(row, col++, dto.getProcurementPlant(), dataStyle);
            createCell(row, col++, dto.getPlantName(), dataStyle);
            createCell(row, col++, formatCodeName(dto.getUtilityCode(), dto.getUtilityName()), dataStyle);
            createCell(row, col++, formatCodeName(dto.getMaterialCode(), dto.getMaterialName()), dataStyle);
            createCell(row, col++, dto.getUom(), dataStyle);

            setNumericCell(row, col++, dto.getApr() != null ? dto.getApr().doubleValue() : null, dataStyle);
            setNumericCell(row, col++, dto.getMay() != null ? dto.getMay().doubleValue() : null, dataStyle);
            setNumericCell(row, col++, dto.getJun() != null ? dto.getJun().doubleValue() : null, dataStyle);
            setNumericCell(row, col++, dto.getJul() != null ? dto.getJul().doubleValue() : null, dataStyle);
            setNumericCell(row, col++, dto.getAug() != null ? dto.getAug().doubleValue() : null, dataStyle);
            setNumericCell(row, col++, dto.getSep() != null ? dto.getSep().doubleValue() : null, dataStyle);
            setNumericCell(row, col++, dto.getOct() != null ? dto.getOct().doubleValue() : null, dataStyle);
            setNumericCell(row, col++, dto.getNov() != null ? dto.getNov().doubleValue() : null, dataStyle);
            setNumericCell(row, col++, dto.getDec() != null ? dto.getDec().doubleValue() : null, dataStyle);
            setNumericCell(row, col++, dto.getJan() != null ? dto.getJan().doubleValue() : null, dataStyle);
            setNumericCell(row, col++, dto.getFeb() != null ? dto.getFeb().doubleValue() : null, dataStyle);
            setNumericCell(row, col++, dto.getMar() != null ? dto.getMar().doubleValue() : null, dataStyle);

            createCell(row, col++, dto.getRemarks(), remarksStyle);
            createCell(row, col++, dto.getId() != null ? dto.getId().toString() : "", dataStyle);
            createCell(row, col++, dto.getImportPlantFkId() != null ? dto.getImportPlantFkId().toString() : "", dataStyle);
            createCell(row, col++, dto.getCppPlantFkId() != null ? dto.getCppPlantFkId().toString() : "", dataStyle);
            createCell(row, col++, dto.getNormParameterFkId() != null ? dto.getNormParameterFkId().toString() : "", dataStyle);
        }

        // Hide ID columns
        sheet.setColumnHidden(idCol, true);
        sheet.setColumnHidden(importPlantFkIdCol, true);
        sheet.setColumnHidden(cppPlantFkIdCol, true);
        sheet.setColumnHidden(normParameterFkIdCol, true);

        // Auto-size columns
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

        logger.info("[Excel Generation] Successfully generated Excel with {} data rows", rowCount);
        return outputStream.toByteArray();
    }

    // ========================================
    // EXCEL READ HELPER
    // ========================================

    private List<CPPImportPowerResponseDTO> readPowerPlanExcel(InputStream inputStream, String aopYear) throws Exception {
        List<CPPImportPowerResponseDTO> records = new ArrayList<>();

        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            // Export column layout (must stay in sync with generatePowerPlanExcel):
            // col 0  = Procurement Plant
            // col 1  = Plant Name
            // col 2  = Utility
            // col 3  = Material
            // col 4  = UOM
            // col 5  = Apr
            // col 6  = May
            // col 7  = Jun
            // col 8  = Jul
            // col 9  = Aug
            // col 10 = Sep
            // col 11 = Oct
            // col 12 = Nov
            // col 13 = Dec
            // col 14 = Jan
            // col 15 = Feb
            // col 16 = Mar
            // col 17 = Remarks
            // col 18 = id (hidden)
            // col 19 = importPlantFkId (hidden)
            // col 20 = cppPlantFkId (hidden)
            // col 21 = normParameterFkId (hidden)

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                boolean isRowEmpty = true;
                for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
                    Cell cell = row.getCell(cellIndex);
                    if (cell != null && !cell.toString().trim().isEmpty()) {
                        isRowEmpty = false;
                        break;
                    }
                }
                if (isRowEmpty) {
                    logger.debug("[IMPORT] Skipping empty row: {}", row.getRowNum());
                    continue;
                }

                CPPImportPowerResponseDTO dto = new CPPImportPowerResponseDTO();

                dto.setProcurementPlant(getCellValue(row, 0));
                dto.setPlantName(getCellValue(row, 1));
                dto.setUtilityName(getCellValue(row, 2));
                dto.setMaterialName(getCellValue(row, 3));
                dto.setUom(getCellValue(row, 4));

                dto.setApr(getCellBigDecimalValue(row, 5));
                dto.setMay(getCellBigDecimalValue(row, 6));
                dto.setJun(getCellBigDecimalValue(row, 7));
                dto.setJul(getCellBigDecimalValue(row, 8));
                dto.setAug(getCellBigDecimalValue(row, 9));
                dto.setSep(getCellBigDecimalValue(row, 10));
                dto.setOct(getCellBigDecimalValue(row, 11));
                dto.setNov(getCellBigDecimalValue(row, 12));
                dto.setDec(getCellBigDecimalValue(row, 13));
                dto.setJan(getCellBigDecimalValue(row, 14));
                dto.setFeb(getCellBigDecimalValue(row, 15));
                dto.setMar(getCellBigDecimalValue(row, 16));

                dto.setRemarks(getCellValue(row, 17));

                String idStr = getCellValue(row, 18);
                if (idStr != null && !idStr.trim().isEmpty()) {
                    try {
                        dto.setId(UUID.fromString(idStr));
                    } catch (IllegalArgumentException e) {
                        logger.warn("[IMPORT] Invalid UUID format for ID: {}", idStr);
                    }
                }

                String importPlantFkIdStr = getCellValue(row, 19);
                if (importPlantFkIdStr != null && !importPlantFkIdStr.trim().isEmpty()) {
                    try {
                        dto.setImportPlantFkId(UUID.fromString(importPlantFkIdStr));
                    } catch (IllegalArgumentException e) {
                        logger.warn("[IMPORT] Invalid UUID format for importPlantFkId: {}", importPlantFkIdStr);
                    }
                }

                String cppPlantFkIdStr = getCellValue(row, 20);
                if (cppPlantFkIdStr != null && !cppPlantFkIdStr.trim().isEmpty()) {
                    try {
                        dto.setCppPlantFkId(UUID.fromString(cppPlantFkIdStr));
                    } catch (IllegalArgumentException e) {
                        logger.warn("[IMPORT] Invalid UUID format for cppPlantFkId: {}", cppPlantFkIdStr);
                    }
                }

                String normParameterFkIdStr = getCellValue(row, 21);
                if (normParameterFkIdStr != null && !normParameterFkIdStr.trim().isEmpty()) {
                    try {
                        dto.setNormParameterFkId(UUID.fromString(normParameterFkIdStr));
                    } catch (IllegalArgumentException e) {
                        logger.warn("[IMPORT] Invalid UUID format for normParameterFkId: {}", normParameterFkIdStr);
                    }
                }

                records.add(dto);
            }
        }

        return records;
    }

    // ========================================
    // RECORD MODIFICATION CHECK
    // ========================================

    private boolean isPowerPlanRecordModifiedForImport(CPPImportPowerResponseDTO dto) {
        if (dto.getId() == null) {
            return true;
        }

        try {
            var optionalEntity = repository.findById(dto.getId());
            if (optionalEntity.isEmpty()) {
                return true;
            }
            return isRecordModified(dto, optionalEntity.get());
        } catch (Exception e) {
            logger.error("[isPowerPlanRecordModifiedForImport] Error checking record ID {}: {}", dto.getId(), e.getMessage());
            return true;
        }
    }

    // ========================================
    // VALIDATION HELPER
    // ========================================

    private String validatePowerPlanData(CPPImportPowerResponseDTO dto) {
        if (dto.getId() == null) {
            return "Record ID is missing";
        }

        if (dto.getProcurementPlant() == null || dto.getProcurementPlant().trim().isEmpty()) {
            return "Procurement plant is required";
        }

        if (dto.getRemarks() == null || dto.getRemarks().trim().isEmpty()) {
            return "Remarks field is mandatory and cannot be empty";
        }

        // Validate that at least one monthly value is set
        if (dto.getApr() == null && dto.getMay() == null && dto.getJun() == null &&
            dto.getJul() == null && dto.getAug() == null && dto.getSep() == null &&
            dto.getOct() == null && dto.getNov() == null && dto.getDec() == null &&
            dto.getJan() == null && dto.getFeb() == null && dto.getMar() == null) {
            return "At least one monthly value is required";
        }

        try {
            var optionalEntity = repository.findById(dto.getId());
            if (optionalEntity.isEmpty()) {
                return "Record with this ID does not exist in database";
            }

            String dbRemarks = optionalEntity.get().getRemarks() != null ? optionalEntity.get().getRemarks().trim() : "";
            String importedRemarks = dto.getRemarks() != null ? dto.getRemarks().trim() : "";
            if (dbRemarks.equals(importedRemarks)) {
                return "Remarks must be updated to explain the changes. Current remarks are identical to the database value.";
            }
        } catch (Exception e) {
            logger.error("[Validation] Error checking remarks for ID {}: {}", dto.getId(), e.getMessage());
        }

        return null;
    }

    // ========================================
    // ERROR EXCEL GENERATION
    // ========================================

    private byte[] generatePowerPlanErrorExcel(List<CPPImportPowerResponseDTO> failedRecords,
            List<String> failureReasons, String sheetName, String aopYear) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle remarksStyle = createRemarksStyle(workbook);
            CellStyle errorStyle = createErrorCellStyle(workbook);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Procurement Plant", "Plant Name", "Utility", "Material", "UOM",
                    "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan", "Feb", "Mar",
                    "Remarks", "id", "importPlantFkId", "cppPlantFkId", "normParameterFkId", "Status", "Comment"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (int i = 0; i < failedRecords.size(); i++) {
                CPPImportPowerResponseDTO dto = failedRecords.get(i);
                String failureReason = failureReasons.get(i);
                Row row = sheet.createRow(rowNum++);

                createCell(row, 0, dto.getProcurementPlant(), dataStyle);
                createCell(row, 1, dto.getPlantName(), dataStyle);
                createCell(row, 2, formatCodeName(dto.getUtilityCode(), dto.getUtilityName()), dataStyle);
                createCell(row, 3, formatCodeName(dto.getMaterialCode(), dto.getMaterialName()), dataStyle);
                createCell(row, 4, dto.getUom(), dataStyle);

                setNumericCell(row, 5, dto.getApr() != null ? dto.getApr().doubleValue() : null, dataStyle);
                setNumericCell(row, 6, dto.getMay() != null ? dto.getMay().doubleValue() : null, dataStyle);
                setNumericCell(row, 7, dto.getJun() != null ? dto.getJun().doubleValue() : null, dataStyle);
                setNumericCell(row, 8, dto.getJul() != null ? dto.getJul().doubleValue() : null, dataStyle);
                setNumericCell(row, 9, dto.getAug() != null ? dto.getAug().doubleValue() : null, dataStyle);
                setNumericCell(row, 10, dto.getSep() != null ? dto.getSep().doubleValue() : null, dataStyle);
                setNumericCell(row, 11, dto.getOct() != null ? dto.getOct().doubleValue() : null, dataStyle);
                setNumericCell(row, 12, dto.getNov() != null ? dto.getNov().doubleValue() : null, dataStyle);
                setNumericCell(row, 13, dto.getDec() != null ? dto.getDec().doubleValue() : null, dataStyle);
                setNumericCell(row, 14, dto.getJan() != null ? dto.getJan().doubleValue() : null, dataStyle);
                setNumericCell(row, 15, dto.getFeb() != null ? dto.getFeb().doubleValue() : null, dataStyle);
                setNumericCell(row, 16, dto.getMar() != null ? dto.getMar().doubleValue() : null, dataStyle);

                createCell(row, 17, dto.getRemarks(), remarksStyle);
                createCell(row, 18, dto.getId() != null ? dto.getId().toString() : "", dataStyle);
                createCell(row, 19, dto.getImportPlantFkId() != null ? dto.getImportPlantFkId().toString() : "", dataStyle);
                createCell(row, 20, dto.getCppPlantFkId() != null ? dto.getCppPlantFkId().toString() : "", dataStyle);
                createCell(row, 21, dto.getNormParameterFkId() != null ? dto.getNormParameterFkId().toString() : "", dataStyle);
                createCell(row, 22, "Failed", errorStyle);
                createCell(row, 23, failureReason, errorStyle);
            }

            sheet.setColumnHidden(18, true);
            sheet.setColumnHidden(19, true);
            sheet.setColumnHidden(20, true);
            sheet.setColumnHidden(21, true);

            for (int i = 0; i < headers.length; i++) {
                if (i == 17 || i == 23) {
                    sheet.setColumnWidth(i, 8000);
                    continue;
                }
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    // ========================================
    // EXCEL STYLE HELPERS
    // ========================================

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void setNumericCell(Row row, int col, Double value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value);
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
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
        return style;
    }

    private CellStyle createErrorCellStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        Font font = workbook.createFont();
        font.setColor(IndexedColors.RED.getIndex());
        font.setBold(true);
        style.setFont(font);
        style.setWrapText(true);
        return style;
    }

    /** Formats a code + name pair as "code - name" for the exported Excel template. */
    private String formatCodeName(String code, String name) {
        boolean hasCode = code != null && !code.isBlank();
        boolean hasName = name != null && !name.isBlank();
        if (hasCode && hasName && !code.equals(name)) {
            return code + " - " + name;
        }
        return hasName ? name : (hasCode ? code : "");
    }

    private String getCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            default:
                return null;
        }
    }

    private BigDecimal getCellBigDecimalValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;

        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        if (cell.getCellType() == CellType.STRING) {
            try {
                String val = cell.getStringCellValue().trim();
                if (!val.isEmpty()) {
                    return new BigDecimal(val);
                }
            } catch (NumberFormatException e) {
                logger.warn("[IMPORT] Cannot parse BigDecimal from cell at index {}", cellIndex);
            }
        }
        return null;
    }

    // ========================================
    // ADD IMPORT POWER SOURCE
    // ========================================

    @Override
    @Transactional
    public AOPMessageVM addImportPowerSource(AddImportPowerSourceRequestDTO request) {
        logger.info("[ADD Source] Adding import power source for CPP plant: {}, procurement plant: {}",
                request.getCppPlant(), request.getProcurementPlant());

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

            // --- Step 1: Fetch CPP plant to get Site_FK_Id and Vertical_FK_Id ---
            Optional<Plants> cppPlantOpt = plantsRepository.findById(request.getCppPlant());
            if (cppPlantOpt.isEmpty()) {
                response.setCode(404);
                response.setMessage("CPP plant not found: " + request.getCppPlant());
                return response;
            }
            Plants cppPlant = cppPlantOpt.get();
            UUID siteFkId     = cppPlant.getSiteFkId();
            UUID verticalFkId = cppPlant.getVerticalFKId();
            logger.info("[ADD Source] Resolved site={}, vertical={} from CPP plant {}",
                    siteFkId, verticalFkId, request.getCppPlant());

            // --- Step 2: Create NormParameters entry ---
            NormParameters norm = new NormParameters();
            norm.setName(request.getName());
            norm.setDisplayName(request.getDisplayName() != null ? request.getDisplayName() : request.getName());
            norm.setUom(request.getUom());
            norm.setSapMaterialCode(request.getSapCode());
            norm.setType("Imported Power");
            // Plant_FK_Id = procurement plant (mirrors existing NormParameters data pattern)
            norm.setPlantFkId(request.getProcurementPlant());
            // Fixed constants mirroring existing POWER import records in NormParameters
            norm.setNormParameterTypeFkId(UUID.fromString("E9C9FCFB-C5C6-49D6-8017-6D1E4C46868E"));
            norm.setNormTypeFKId(2);
            norm.setIsVisible(true);
            norm.setIsEditable(true);

            NormParameters savedNorm = normParametersRepository.save(norm);
            logger.info("[ADD Source] NormParameters created with Id: {}", savedNorm.getId());

            // --- Step 3: Create CPPImportPower entry ---
            CPPImportPower importPower = new CPPImportPower();
            importPower.setNormParameterFkId(savedNorm.getId());
            importPower.setUtilityNormParameterFkId(resolveUtilityNormParameterId(request.getCppPlant()));
            importPower.setImportPlantFkId(request.getProcurementPlant());
            importPower.setCppPlantFkId(request.getCppPlant());
            importPower.setSiteFkId(siteFkId);
            importPower.setVerticalFkId(verticalFkId);
            importPower.setAopYear(request.getAopYear());
            importPower.setCreatedDate(LocalDateTime.now());
            importPower.setUpdatedDate(LocalDateTime.now());
            // All monthly values default to 0
            BigDecimal zero = BigDecimal.ZERO;
            importPower.setApr(zero); importPower.setMay(zero); importPower.setJun(zero);
            importPower.setJul(zero); importPower.setAug(zero); importPower.setSep(zero);
            importPower.setOct(zero); importPower.setNov(zero); importPower.setDec(zero);
            importPower.setJan(zero); importPower.setFeb(zero); importPower.setMar(zero);

            CPPImportPower savedImportPower = repository.save(importPower);
            logger.info("[ADD Source] CPPImportPower created with Id: {}", savedImportPower.getId());

            // --- Step 4: Create CPPPowerSourceOperationHours entry with zero values ---
            CPPPowerSourceOperationHours opHours = new CPPPowerSourceOperationHours();
            opHours.setId(UUID.randomUUID());
            opHours.setAssetFkId(savedImportPower.getId());
            opHours.setApr(0.0);  opHours.setMay(0.0);  opHours.setJun(0.0);
            opHours.setJul(0.0);  opHours.setAug(0.0);  opHours.setSep(0.0);
            opHours.setOct(0.0);  opHours.setNov(0.0);  opHours.setDec(0.0);
            opHours.setJan(0.0);  opHours.setFeb(0.0);  opHours.setMar(0.0);
            opHours.setAopYear(request.getAopYear());
            opHours.setRemarks(null);
            opHours.setSiteFkId(siteFkId);
            opHours.setVerticalFkId(verticalFkId);
            opHours.setPlantFkId(request.getCppPlant());
            opHours.setCreatedDate(LocalDateTime.now());
            opHours.setModifiedDate(LocalDateTime.now());
            powerSourceOperationHoursRepository.save(opHours);
            logger.info("[ADD Source] CPPPowerSourceOperationHours created with Id: {} for PowerSource_FK_Id: {}", opHours.getId(), savedImportPower.getId());

            Map<String, Object> data = new HashMap<>();
            data.put("normParameterId", savedNorm.getId());
            data.put("importPowerId", savedImportPower.getId());

            response.setCode(200);
            response.setMessage("Import power source added successfully");
            response.setData(data);

        } catch (Exception e) {
            logger.error("[ADD Source] Error adding import power source: {}", e.getMessage(), e);
            response.setCode(500);
            response.setMessage("Failed to add import power source: " + e.getMessage());
            response.setData(null);
        }

        return response;
    }

    // ========================================
    // UPDATE IMPORT POWER SOURCE
    // ========================================

    @Override
    @Transactional
    public AOPMessageVM updateImportPowerSource(UUID normParameterId, UpdateImportPowerSourceRequestDTO request) {
        logger.info("[UPDATE Source] normParameterId={}, procurementPlant={}", normParameterId, request.getProcurementPlant());

        AOPMessageVM response = new AOPMessageVM();

        try {
            if (normParameterId == null) {
                response.setCode(400);
                response.setMessage("normParameterId is required");
                return response;
            }
            if (request.getProcurementPlant() == null) {
                response.setCode(400);
                response.setMessage("procurementPlant is required");
                return response;
            }

            Optional<NormParameters> normOpt = normParametersRepository.findById(normParameterId);
            if (normOpt.isEmpty()) {
                response.setCode(404);
                response.setMessage("NormParameter not found: " + normParameterId);
                return response;
            }

            NormParameters norm = normOpt.get();

            // Guard: ensure the record belongs to the given procurement plant
            if (!request.getProcurementPlant().equals(norm.getPlantFkId())) {
                response.setCode(403);
                response.setMessage("NormParameter does not belong to the supplied procurementPlant");
                return response;
            }

            // Apply only editable fields — Plant_FK_Id is intentionally NOT changed
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

            NormParameters saved = normParametersRepository.save(norm);
            logger.info("[UPDATE Source] NormParameters {} updated successfully", saved.getId());

            Map<String, Object> data = new HashMap<>();
            data.put("normParameterId", saved.getId());
            data.put("name", saved.getName());
            data.put("displayName", saved.getDisplayName());
            data.put("uom", saved.getUom());
            data.put("sapCode", saved.getSapMaterialCode());

            response.setCode(200);
            response.setMessage("Import power source updated successfully");
            response.setData(data);

        } catch (Exception e) {
            logger.error("[UPDATE Source] Error: {}", e.getMessage(), e);
            response.setCode(500);
            response.setMessage("Failed to update import power source: " + e.getMessage());
        }

        return response;
    }

    // ========================================
    // DELETE (SOFT) IMPORT POWER SOURCE
    // ========================================

    @Override
    @Transactional
    public AOPMessageVM deleteImportPowerSource(UUID normParameterId, UUID procurementPlant) {
        logger.info("[DELETE Source] Soft-deleting normParameterId={}, procurementPlant={}",
                normParameterId, procurementPlant);

        AOPMessageVM response = new AOPMessageVM();

        try {
            if (normParameterId == null) {
                response.setCode(400);
                response.setMessage("normParameterId is required");
                return response;
            }
            if (procurementPlant == null) {
                response.setCode(400);
                response.setMessage("procurementPlant is required");
                return response;
            }

            Optional<NormParameters> normOpt = normParametersRepository.findById(normParameterId);
            if (normOpt.isEmpty()) {
                response.setCode(404);
                response.setMessage("NormParameter not found: " + normParameterId);
                return response;
            }

            NormParameters norm = normOpt.get();

            // Guard: ensure the record belongs to the given procurement plant
            if (!procurementPlant.equals(norm.getPlantFkId())) {
                response.setCode(403);
                response.setMessage("NormParameter does not belong to the supplied procurementPlant");
                return response;
            }

            // Soft-delete: set isVisible = false (isVisible = 0 in DB)
            norm.setIsVisible(false);
            normParametersRepository.save(norm);
            logger.info("[DELETE Source] NormParameters {} marked as not visible", normParameterId);

            // Also delete corresponding CPPPowerSourceOperationHours entries
            CPPImportPower importPower = repository.findByNormParameterFkId(normParameterId);
            if (importPower != null) {
                powerSourceOperationHoursRepository.deleteByAssetFkId(importPower.getId());
                logger.info("[DELETE Source] CPPPowerSourceOperationHours deleted for PowerSource_FK_Id: {}", importPower.getId());
            }

            response.setCode(200);
            response.setMessage("Import power source deleted successfully (soft delete)");

        } catch (Exception e) {
            logger.error("[DELETE Source] Error: {}", e.getMessage(), e);
            response.setCode(500);
            response.setMessage("Failed to delete import power source: " + e.getMessage());
        }

        return response;
    }

    // ========================================
    // GET IMPORT PROCUREMENT PLANTS WITH SOURCES
    // ========================================

    @Override
    public AOPMessageVM getImportProcurementPlants(UUID cppPlantId) {
        logger.info("[GET Procurement Plants] cppPlantId={}", cppPlantId);

        AOPMessageVM response = new AOPMessageVM();

        try {
            if (cppPlantId == null) {
                response.setCode(400);
                response.setMessage("cppPlantId is required");
                return response;
            }

            // Fetch flat rows from DB: one row per (procurementPlant, normParameter)
            List<ImportProcurementPlantProjection> rows =
                    repository.getProcurementPlantsWithSources(cppPlantId);

            // Group flat rows into nested DTOs: one entry per procurement plant
            Map<UUID, ImportPowerProcurementPlantDTO> plantMap = new LinkedHashMap<>();

            for (ImportProcurementPlantProjection row : rows) {
                UUID plantId = row.getProcurementPlantId();

                // Create the plant entry if not yet seen
                ImportPowerProcurementPlantDTO plantDTO = plantMap.computeIfAbsent(plantId, id -> {
                    ImportPowerProcurementPlantDTO dto = new ImportPowerProcurementPlantDTO();
                    dto.setProcurementPlantId(plantId);
                    dto.setName(row.getPlantName());
                    dto.setCppPlantId(row.getCppPlantId());
                    return dto;
                });

                // Append source if a NormParameter row is present (LEFT JOIN may give nulls)
                if (row.getNormParameterId() != null) {
                    ImportPowerSourceDTO source = new ImportPowerSourceDTO();
                    source.setNormParameterId(row.getNormParameterId());
                    source.setName(row.getNormName());
                    source.setDisplayName(row.getNormDisplayName());
                    source.setSapCode(row.getSapCode());
                    source.setUom(row.getUom());
                    plantDTO.getSources().add(source);
                }
            }

            List<ImportPowerProcurementPlantDTO> result = new ArrayList<>(plantMap.values());
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
}
