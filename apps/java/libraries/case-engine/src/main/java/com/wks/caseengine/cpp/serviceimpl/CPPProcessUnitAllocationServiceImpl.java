package com.wks.caseengine.cpp.serviceimpl;

import com.wks.caseengine.cpp.dto.CPPProcessUnitAllocationDTO;
import com.wks.caseengine.cpp.dto.CPPProcessUnitAllocationProjection;
import com.wks.caseengine.cpp.entity.CPPProcessUnitAllocation;
import com.wks.caseengine.cpp.repository.CPPProcessUnitAllocationRepository;
import com.wks.caseengine.cpp.service.CPPProcessUnitAllocationService;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
public class CPPProcessUnitAllocationServiceImpl implements CPPProcessUnitAllocationService {

    private static final Logger logger = LoggerFactory.getLogger(CPPProcessUnitAllocationServiceImpl.class);

    @Autowired
    private CPPProcessUnitAllocationRepository repository;

    // ── GET ────────────────────────────────────────────────────────────────────

    @Override
    public AOPMessageVM getProcessUnitAllocations(List<UUID> plantIds, String aopYear) {
        logger.info("[GET Service] Fetching process unit allocations - plantIds: {}, aopYear: {}", plantIds, aopYear);
        AOPMessageVM response = new AOPMessageVM();

        try {
            // Convert UUID list to comma-separated string as expected by the stored procedure
            String plantIdsParam = plantIds.stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(","));
            List<CPPProcessUnitAllocationProjection> projections = repository.findByPlantIdsAndAopYear(plantIdsParam, aopYear);
            logger.info("[GET Service] Query returned {} records", projections.size());

            List<CPPProcessUnitAllocationDTO> results = projections.stream()
                    .map(this::mapProjectionToDto)
                    .collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("processUnitAllocations", results);

            response.setCode(200);
            response.setMessage("Data fetched successfully");
            response.setData(data);

        } catch (Exception e) {
            logger.error("[GET Service] Error fetching process unit allocations: {}", e.getMessage(), e);
            response.setCode(500);
            response.setMessage("Failed to fetch data: " + e.getMessage());
            response.setData(null);
        }

        return response;
    }

    // ── POST (upsert) ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM saveProcessUnitAllocations(
            List<UUID> plantIds, String aopYear, List<CPPProcessUnitAllocationDTO> payload) {

        logger.info("[POST Service] Saving process unit allocations - plantIds: {}, aopYear: {}, records: {}",
                plantIds, aopYear, payload != null ? payload.size() : 0);

        AOPMessageVM response = new AOPMessageVM();

        int inserted = 0;
        int updated  = 0;
        int skipped  = 0;
        List<Map<String, String>> skippedRecords = new ArrayList<>();

        try {
            List<CPPProcessUnitAllocationDTO> allocations = payload;
            if (allocations == null || allocations.isEmpty()) {
                response.setCode(200);
                response.setMessage("No records to process.");
                return response;
            }

            for (CPPProcessUnitAllocationDTO dto : allocations) {
                boolean isNew = dto.getId() == null;

                if (isNew) {
                    // ── INSERT ────────────────────────────────────────────────
                    // Duplicate guard: same CPPPlant + ImportPower + ProcessPlant + AOPYear
                    if (dto.getCppPlantId() != null && dto.getSourceId() != null
                            && dto.getProcessPlantName() != null && dto.getAopYear() != null) {
                        int exists = repository.existsAllocation(
                                dto.getCppPlantId(),
                                dto.getSourceId(),
                                dto.getProcessPlantName(),
                                dto.getAopYear()
                        );
                        if (exists > 0) {
                            skipped++;
                            logger.warn("[POST Service] Duplicate allocation skipped for CPPPlant: {}, ImportPower: {}, ProcessPlant: {}",
                                    dto.getCppPlantId(), dto.getSourceId(), dto.getProcessPlantName());
                            Map<String, String> rec = new HashMap<>();
                            rec.put("processUnit", dto.getProcessUnit() != null ? dto.getProcessUnit() : "Unknown");
                            rec.put("reason", "Duplicate: allocation already exists for this CPPPlant + ImportPower + ProcessPlant + AOPYear");
                            skippedRecords.add(rec);
                            continue;
                        }
                    }

                    CPPProcessUnitAllocation entity = mapToEntity(dto);
                    entity.setId(null);                        // let DB generate UUID
                    entity.setCreatedDate(LocalDateTime.now());
                    entity.setUpdatedDate(LocalDateTime.now());
                    repository.save(entity);
                    inserted++;
                    logger.debug("[POST Service] Inserted new allocation for ProcessPlant: {}", dto.getProcessPlantName());

                } else {
                    // ── UPDATE ────────────────────────────────────────────────
                    UUID recordId = dto.getId();

                    Optional<CPPProcessUnitAllocation> existing = repository.findById(recordId);
                    if (existing.isEmpty()) {
                        skipped++;
                        logger.warn("[POST Service] Record not found for update, id: {}", recordId);
                        Map<String, String> rec = new HashMap<>();
                        rec.put("id", recordId.toString());
                        rec.put("reason", "Record not found in database");
                        skippedRecords.add(rec);
                        continue;
                    }

                    CPPProcessUnitAllocation entity = existing.get();
                    applyUpdate(entity, dto);
                    entity.setUpdatedDate(LocalDateTime.now());
                    repository.save(entity);
                    updated++;
                    logger.debug("[POST Service] Updated allocation id: {}", recordId);

                }
            }

            logger.info("[POST Service] Completed - Inserted: {}, Updated: {}, Skipped: {}", inserted, updated, skipped);

            Map<String, Object> data = new HashMap<>();
            data.put("inserted", inserted);
            data.put("updated", updated);
            data.put("skipped", skipped);
            data.put("totalProcessed", inserted + updated + skipped);
            if (!skippedRecords.isEmpty()) {
                data.put("skippedRecords", skippedRecords);
            }

            response.setCode(skippedRecords.isEmpty() ? 200 : 207);
            response.setMessage(String.format(
                    "Process unit allocations saved. Inserted: %d, Updated: %d, Skipped: %d",
                    inserted, updated, skipped));
            response.setData(data);

        } catch (Exception e) {
            logger.error("[POST Service] Error saving process unit allocations: {}", e.getMessage(), e);
            response.setCode(500);
            response.setMessage("Failed to save process unit allocations: " + e.getMessage());
            response.setData(null);
        }

        return response;
    }

    // ── DELETE ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM deleteProcessUnitAllocation(UUID id) {
        logger.info("[DELETE Service] Deleting process unit allocation id: {}", id);
        AOPMessageVM response = new AOPMessageVM();

        try {
            if (!repository.existsById(id)) {
                response.setCode(404);
                response.setMessage("Allocation not found with id: " + id);
                return response;
            }

            repository.deleteAllocationById(id);
            logger.info("[DELETE Service] Deleted allocation id: {}", id);

            response.setCode(200);
            response.setMessage("Allocation deleted successfully.");

        } catch (Exception e) {
            logger.error("[DELETE Service] Error deleting allocation id {}: {}", id, e.getMessage(), e);
            response.setCode(500);
            response.setMessage("Failed to delete allocation: " + e.getMessage());
        }

        return response;
    }

    // ── Mapping helpers ────────────────────────────────────────────────────────

    /** Maps a JOIN projection (from GET query) to DTO — includes display labels. */
    private CPPProcessUnitAllocationDTO mapProjectionToDto(CPPProcessUnitAllocationProjection p) {
        CPPProcessUnitAllocationDTO dto = new CPPProcessUnitAllocationDTO();

        dto.setId(p.getId());
        dto.setCppPlantId(p.getCppPlantFkId());
        dto.setSourceId(p.getImportPowerFkId());
        dto.setNormParameterFkId(p.getNormParameterFkId());
        dto.setProcessPlantName(p.getProcessPlantName());
        dto.setProcessPlantCode(p.getProcessPlantCode());

        // Display labels resolved via SP JOINs
        dto.setProcurementPlant(p.getProcurementPlant());
        dto.setPlantName(p.getPlantName());
        dto.setUtility(p.getUtility());
        dto.setMaterial(p.getMaterial());
        dto.setMaterialDisplayName(p.getMaterialDisplayName());
        dto.setUom(p.getUom());
        dto.setProcessUnit(p.getProcessUnit());

        dto.setAopYear(p.getAopYear());
        dto.setRemarks(p.getRemarks());
        dto.setCreatedDate(p.getCreatedDate());
        dto.setUpdatedDate(p.getUpdatedDate());

        // Source monthly values from CPPImportPower
        dto.setSourceApr(p.getSourceApr()); dto.setSourceMay(p.getSourceMay());
        dto.setSourceJun(p.getSourceJun()); dto.setSourceJul(p.getSourceJul());
        dto.setSourceAug(p.getSourceAug()); dto.setSourceSep(p.getSourceSep());
        dto.setSourceOct(p.getSourceOct()); dto.setSourceNov(p.getSourceNov());
        dto.setSourceDec(p.getSourceDec()); dto.setSourceJan(p.getSourceJan());
        dto.setSourceFeb(p.getSourceFeb()); dto.setSourceMar(p.getSourceMar());

        dto.setApr(p.getApr());   dto.setMay(p.getMay());   dto.setJun(p.getJun());
        dto.setJul(p.getJul());   dto.setAug(p.getAug());   dto.setSep(p.getSep());
        dto.setOct(p.getOct());   dto.setNov(p.getNov());   dto.setDec(p.getDec());
        dto.setJan(p.getJan());   dto.setFeb(p.getFeb());   dto.setMar(p.getMar());

        dto.setBalanceApr(p.getBalanceApr()); dto.setBalanceMay(p.getBalanceMay());
        dto.setBalanceJun(p.getBalanceJun()); dto.setBalanceJul(p.getBalanceJul());
        dto.setBalanceAug(p.getBalanceAug()); dto.setBalanceSep(p.getBalanceSep());
        dto.setBalanceOct(p.getBalanceOct()); dto.setBalanceNov(p.getBalanceNov());
        dto.setBalanceDec(p.getBalanceDec()); dto.setBalanceJan(p.getBalanceJan());
        dto.setBalanceFeb(p.getBalanceFeb()); dto.setBalanceMar(p.getBalanceMar());

        return dto;
    }

    private CPPProcessUnitAllocation mapToEntity(CPPProcessUnitAllocationDTO dto) {
        CPPProcessUnitAllocation e = new CPPProcessUnitAllocation();
        applyUpdate(e, dto);
        return e;
    }

    /**
     * Applies updatable fields from DTO onto an entity.
     * FK fields (cppPlantFkId, importPowerFkId, normParameterFkId, aopYear)
     * are set on INSERT but should NOT be changed on UPDATE.
     * processPlantName, processPlantCode, remarks, month values and balance values are always overwritten.
     */
    private void applyUpdate(CPPProcessUnitAllocation e, CPPProcessUnitAllocationDTO dto) {
        // FKs — only set if not yet persisted (insert path); ignored on update path
        if (e.getId() == null) {
            e.setCppPlantFkId(dto.getCppPlantId());
            e.setImportPowerFkId(dto.getSourceId());
            e.setNormParameterFkId(dto.getNormParameterFkId());
            e.setAopYear(dto.getAopYear());
        }

        // processPlantName / processPlantCode are updatable on both INSERT and UPDATE
        // (user can change process unit during edit)
        e.setProcessPlantName(dto.getProcessPlantName());
        e.setProcessPlantCode(dto.getProcessPlantCode());

        e.setRemarks(dto.getRemarks());

        e.setApr(bd(dto.getApr())); e.setMay(bd(dto.getMay())); e.setJun(bd(dto.getJun()));
        e.setJul(bd(dto.getJul())); e.setAug(bd(dto.getAug())); e.setSep(bd(dto.getSep()));
        e.setOct(bd(dto.getOct())); e.setNov(bd(dto.getNov())); e.setDec(bd(dto.getDec()));
        e.setJan(bd(dto.getJan())); e.setFeb(bd(dto.getFeb())); e.setMar(bd(dto.getMar()));

        e.setBalanceApr(bd(dto.getBalanceApr())); e.setBalanceMay(bd(dto.getBalanceMay()));
        e.setBalanceJun(bd(dto.getBalanceJun())); e.setBalanceJul(bd(dto.getBalanceJul()));
        e.setBalanceAug(bd(dto.getBalanceAug())); e.setBalanceSep(bd(dto.getBalanceSep()));
        e.setBalanceOct(bd(dto.getBalanceOct())); e.setBalanceNov(bd(dto.getBalanceNov()));
        e.setBalanceDec(bd(dto.getBalanceDec())); e.setBalanceJan(bd(dto.getBalanceJan()));
        e.setBalanceFeb(bd(dto.getBalanceFeb())); e.setBalanceMar(bd(dto.getBalanceMar()));
    }

    /** Null-safe BigDecimal helper — defaults to 0 when null. */
    private BigDecimal bd(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    // ========================================
    // EXPORT PROCESS UNIT ALLOCATIONS
    // ========================================

    @Override
    public byte[] exportProcessUnitAllocations(List<UUID> plantIds, String aopYear) {
        logger.info("[Export PUA] Exporting for plantIds: {}, aopYear: {}", plantIds, aopYear);

        try {
            AOPMessageVM response = getProcessUnitAllocations(plantIds, aopYear);

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.getData();

            @SuppressWarnings("unchecked")
            List<CPPProcessUnitAllocationDTO> allocations =
                    (List<CPPProcessUnitAllocationDTO>) data.get("processUnitAllocations");

            if (allocations != null && !allocations.isEmpty()) {
                allocations.sort(Comparator
                        .comparing(CPPProcessUnitAllocationDTO::getPlantName,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(CPPProcessUnitAllocationDTO::getProcessUnit,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
                logger.debug("[Export PUA] Sorted {} records", allocations.size());
            }

            logger.info("[Export PUA] Generating Excel for {} records", allocations != null ? allocations.size() : 0);
            return generateAllocationExcel(allocations, "Process Unit Allocations", aopYear);

        } catch (Exception e) {
            logger.error("[Export PUA] Error exporting: {}", e.getMessage(), e);
            return null;
        }
    }

    // ========================================
    // IMPORT PROCESS UNIT ALLOCATIONS
    // ========================================

    @Override
    @Transactional
    public AOPMessageVM importProcessUnitAllocations(List<UUID> plantIds, String aopYear, MultipartFile file) {
        logger.info("[Import PUA] Importing for plantIds: {}, aopYear: {}, file: {}",
                plantIds, aopYear, file.getOriginalFilename());

        AOPMessageVM response = new AOPMessageVM();

        try {
            List<CPPProcessUnitAllocationDTO> excelData = readAllocationExcel(file.getInputStream(), aopYear);
            logger.info("[Import PUA] Read {} records from Excel", excelData.size());

            List<CPPProcessUnitAllocationDTO> validRecords = new ArrayList<>();
            List<CPPProcessUnitAllocationDTO> failedRecords = new ArrayList<>();
            List<String> failureReasons = new ArrayList<>();
            int skippedCount = 0;

            for (CPPProcessUnitAllocationDTO dto : excelData) {
                if (!isAllocationRecordModifiedForImport(dto)) {
                    skippedCount++;
                    logger.debug("[Import PUA] Skipping unchanged record: {}", dto.getProcessUnit());
                    continue;
                }

                String validationError = validateAllocationData(dto);
                if (validationError != null) {
                    failedRecords.add(dto);
                    failureReasons.add(validationError);
                    logger.warn("[Import PUA] Invalid record - {}: {}", dto.getProcessUnit(), validationError);
                } else {
                    validRecords.add(dto);
                }
            }

            logger.info("[Import PUA] {} unchanged, {} modified to process", skippedCount, excelData.size() - skippedCount);

            // Cross-record validation: sum of allocations per sourceId+month must not exceed source total (Power Qty)
            // Include records that will be in the DB after import: unchanged (skipped) + valid (modified).
            // Exclude per-record failures — they won't be saved, so their allocations shouldn't count.
            Set<CPPProcessUnitAllocationDTO> perRecordFailures = Collections.newSetFromMap(new IdentityHashMap<>());
            perRecordFailures.addAll(failedRecords);

            Map<UUID, BigDecimal[]> crossAllocatedBySource = new HashMap<>();
            Map<UUID, BigDecimal[]> crossSourceTotals = new HashMap<>();
            for (CPPProcessUnitAllocationDTO dto : excelData) {
                if (dto.getSourceId() == null || perRecordFailures.contains(dto)) continue;
                BigDecimal[] alloc = crossAllocatedBySource.computeIfAbsent(dto.getSourceId(), k -> new BigDecimal[12]);
                BigDecimal[] src = crossSourceTotals.computeIfAbsent(dto.getSourceId(), k -> new BigDecimal[12]);
                BigDecimal[] a = {dto.getApr(), dto.getMay(), dto.getJun(), dto.getJul(), dto.getAug(), dto.getSep(),
                                   dto.getOct(), dto.getNov(), dto.getDec(), dto.getJan(), dto.getFeb(), dto.getMar()};
                BigDecimal[] s = {dto.getSourceApr(), dto.getSourceMay(), dto.getSourceJun(), dto.getSourceJul(),
                                   dto.getSourceAug(), dto.getSourceSep(), dto.getSourceOct(), dto.getSourceNov(),
                                   dto.getSourceDec(), dto.getSourceJan(), dto.getSourceFeb(), dto.getSourceMar()};
                for (int m = 0; m < 12; m++) {
                    if (s[m] != null) src[m] = s[m];
                    if (a[m] != null) alloc[m] = (alloc[m] != null ? alloc[m] : BigDecimal.ZERO).add(a[m]);
                }
            }
            String[] crossMonthNames = {"Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan", "Feb", "Mar"};
            Set<UUID> crossFailureIds = new java.util.HashSet<>();
            for (CPPProcessUnitAllocationDTO dto : validRecords) {
                if (dto.getSourceId() == null) continue;
                BigDecimal[] alloc = crossAllocatedBySource.get(dto.getSourceId());
                BigDecimal[] src = crossSourceTotals.get(dto.getSourceId());
                if (alloc == null || src == null) continue;
                for (int m = 0; m < 12; m++) {
                    if (alloc[m] != null && src[m] != null && alloc[m].compareTo(src[m]) > 0) {
                        crossFailureIds.add(dto.getId());
                        break;
                    }
                }
            }
            Iterator<CPPProcessUnitAllocationDTO> validIter = validRecords.iterator();
            while (validIter.hasNext()) {
                CPPProcessUnitAllocationDTO dto = validIter.next();
                if (crossFailureIds.contains(dto.getId())) {
                    validIter.remove();
                    failedRecords.add(dto);
                    BigDecimal[] alloc = crossAllocatedBySource.get(dto.getSourceId());
                    BigDecimal[] src = crossSourceTotals.get(dto.getSourceId());
                    StringBuilder exceededMonths = new StringBuilder();
                    for (int m = 0; m < 12; m++) {
                        if (alloc[m] != null && src[m] != null && alloc[m].compareTo(src[m]) > 0) {
                            exceededMonths.append(crossMonthNames[m]).append(" (alloc: ").append(alloc[m])
                                    .append(" > source: ").append(src[m]).append("); ");
                        }
                    }
                    failureReasons.add("Cross-record validation failed: total allocation exceeds source total for months: " + exceededMonths);
                    logger.warn("[Import PUA] Cross-record validation failed for record {}: {}", dto.getProcessUnit(), exceededMonths);
                }
            }
            if (!crossFailureIds.isEmpty()) {
                logger.info("[Import PUA] {} records failed cross-record validation", crossFailureIds.size());
            }

            if (!validRecords.isEmpty()) {
                try {
                    AOPMessageVM saveResult = saveProcessUnitAllocations(plantIds, aopYear, validRecords);

                    if (saveResult.getCode() == 207) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> saveData = (Map<String, Object>) saveResult.getData();
                        if (saveData != null && saveData.containsKey("skippedRecords")) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, String>> skippedRecords =
                                    (List<Map<String, String>>) saveData.get("skippedRecords");
                            logger.warn("[Import PUA] {} records skipped during save", skippedRecords.size());
                        }
                    }

                    logger.info("[Import PUA] Successfully processed {} records", validRecords.size());
                } catch (Exception e) {
                    logger.error("[Import PUA] Error saving records: {}", e.getMessage(), e);
                    for (CPPProcessUnitAllocationDTO failedDto : validRecords) {
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
                    response.setMessage("All process unit allocations saved successfully. "
                            + skippedCount + " records unchanged, " + validRecords.size() + " records updated.");
                }
            } else {
                byte[] failedRecordsFile = generateAllocationErrorExcel(failedRecords, failureReasons,
                        "Process Unit Allocations", aopYear);
                String base64File = java.util.Base64.getEncoder().encodeToString(failedRecordsFile);
                response.setCode(400);
                response.setMessage("Partial import: " + validRecords.size() + " saved, " + failedRecords.size()
                        + " failed, " + skippedCount + " unchanged. Download file for details.");
                response.setData(base64File);
                logger.info("[Import PUA] Exported {} failed records to Excel", failedRecords.size());
            }

            logger.info("[Import PUA] Completed - Unchanged: {}, Saved: {}, Failed: {}",
                    skippedCount, validRecords.size(), failedRecords.size());
        } catch (Exception e) {
            logger.error("[Import PUA] Error during import: {}", e.getMessage(), e);
            response.setCode(500);
            response.setMessage("Failed to import process unit allocations: " + e.getMessage());
        }

        return response;
    }

    // ========================================
    // RECORD MODIFICATION CHECK FOR IMPORT
    // ========================================

    private boolean isAllocationRecordModifiedForImport(CPPProcessUnitAllocationDTO dto) {
        if (dto.getId() == null) {
            return true;
        }

        try {
            Optional<CPPProcessUnitAllocation> existing = repository.findById(dto.getId());
            if (existing.isEmpty()) {
                return true;
            }
            return isAllocationRecordModified(dto, existing.get());
        } catch (Exception e) {
            logger.error("[isAllocationRecordModifiedForImport] Error checking record ID {}: {}", dto.getId(), e.getMessage());
            return true;
        }
    }

    private boolean isAllocationRecordModified(CPPProcessUnitAllocationDTO dto, CPPProcessUnitAllocation entity) {
        boolean modified = false;

        if (!bdEquals(dto.getApr(), entity.getApr())) modified = true;
        if (!bdEquals(dto.getMay(), entity.getMay())) modified = true;
        if (!bdEquals(dto.getJun(), entity.getJun())) modified = true;
        if (!bdEquals(dto.getJul(), entity.getJul())) modified = true;
        if (!bdEquals(dto.getAug(), entity.getAug())) modified = true;
        if (!bdEquals(dto.getSep(), entity.getSep())) modified = true;
        if (!bdEquals(dto.getOct(), entity.getOct())) modified = true;
        if (!bdEquals(dto.getNov(), entity.getNov())) modified = true;
        if (!bdEquals(dto.getDec(), entity.getDec())) modified = true;
        if (!bdEquals(dto.getJan(), entity.getJan())) modified = true;
        if (!bdEquals(dto.getFeb(), entity.getFeb())) modified = true;
        if (!bdEquals(dto.getMar(), entity.getMar())) modified = true;

        String dtoRemarks = dto.getRemarks() != null ? dto.getRemarks().trim() : "";
        String entityRemarks = entity.getRemarks() != null ? entity.getRemarks().trim() : "";
        if (!dtoRemarks.equals(entityRemarks)) modified = true;

        if (!modified) {
            logger.debug("[isAllocationRecordModified] Record {} unchanged", dto.getId());
        } else {
            logger.debug("[isAllocationRecordModified] Record {} modified", dto.getId());
        }
        return modified;
    }

    private boolean bdEquals(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.stripTrailingZeros().compareTo(b.stripTrailingZeros()) == 0;
    }

    // ========================================
    // VALIDATION HELPER
    // ========================================

    private String validateAllocationData(CPPProcessUnitAllocationDTO dto) {
        if (dto.getId() == null) {
            return "Record ID is missing";
        }

        if (dto.getRemarks() == null || dto.getRemarks().trim().isEmpty()) {
            return "Remarks field is mandatory and cannot be empty";
        }

        // Validate that at least one monthly value is set
        if (dto.getApr() == null && dto.getMay() == null && dto.getJun() == null &&
            dto.getJul() == null && dto.getAug() == null && dto.getSep() == null &&
            dto.getOct() == null && dto.getNov() == null && dto.getDec() == null &&
            dto.getJan() == null && dto.getFeb() == null && dto.getMar() == null) {
            return "At least one monthly allocation value is required";
        }

        // Validate that monthly values are not negative
        if (isNegative(dto.getApr()) || isNegative(dto.getMay()) || isNegative(dto.getJun()) ||
            isNegative(dto.getJul()) || isNegative(dto.getAug()) || isNegative(dto.getSep()) ||
            isNegative(dto.getOct()) || isNegative(dto.getNov()) || isNegative(dto.getDec()) ||
            isNegative(dto.getJan()) || isNegative(dto.getFeb()) || isNegative(dto.getMar())) {
            return "Monthly allocation values cannot be negative";
        }

        // Validate that Illustrative Qty (allocation) does not exceed Power Qty (source) per month
        String[] monthNames = {"Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan", "Feb", "Mar"};
        BigDecimal[] allocValues = {dto.getApr(), dto.getMay(), dto.getJun(), dto.getJul(), dto.getAug(), dto.getSep(),
                                     dto.getOct(), dto.getNov(), dto.getDec(), dto.getJan(), dto.getFeb(), dto.getMar()};
        BigDecimal[] sourceValues = {dto.getSourceApr(), dto.getSourceMay(), dto.getSourceJun(), dto.getSourceJul(),
                                      dto.getSourceAug(), dto.getSourceSep(), dto.getSourceOct(), dto.getSourceNov(),
                                      dto.getSourceDec(), dto.getSourceJan(), dto.getSourceFeb(), dto.getSourceMar()};
        for (int m = 0; m < 12; m++) {
            if (allocValues[m] != null && sourceValues[m] != null
                    && allocValues[m].compareTo(sourceValues[m]) > 0) {
                return monthNames[m] + " Illustrative Qty (" + allocValues[m]
                        + ") exceeds Power Qty (" + sourceValues[m] + ")";
            }
        }

        try {
            Optional<CPPProcessUnitAllocation> existing = repository.findById(dto.getId());
            if (existing.isEmpty()) {
                return "Record with this ID does not exist in database";
            }

            String dbRemarks = existing.get().getRemarks() != null ? existing.get().getRemarks().trim() : "";
            String importedRemarks = dto.getRemarks() != null ? dto.getRemarks().trim() : "";
            if (dbRemarks.equals(importedRemarks)) {
                return "Remarks must be updated to explain the changes. Current remarks are identical to the database value.";
            }
        } catch (Exception e) {
            logger.error("[Validation] Error checking remarks for ID {}: {}", dto.getId(), e.getMessage());
        }

        return null;
    }

    private boolean isNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }

    // ========================================
    // EXCEL GENERATION HELPER
    // ========================================

    private byte[] generateAllocationExcel(List<CPPProcessUnitAllocationDTO> dataList, String sheetName, String aopYear) throws Exception {
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
        createCell(headerRow, col++, "Process Unit", headerStyle);

        String[] months = {"Apr-" + startYearSuffix, "May-" + startYearSuffix, "Jun-" + startYearSuffix, "Jul-" + startYearSuffix,
                "Aug-" + startYearSuffix, "Sep-" + startYearSuffix, "Oct-" + startYearSuffix, "Nov-" + startYearSuffix,
                "Dec-" + startYearSuffix, "Jan-" + endYearSuffix, "Feb-" + endYearSuffix, "Mar-" + endYearSuffix};

        for (String month : months) {
            createCell(headerRow, col++, month + " Power Qty", headerStyle);
            createCell(headerRow, col++, month + " Illustrative Qty", headerStyle);
            createCell(headerRow, col++, month + " Balance", headerStyle);
        }

        int remarksCol = col;
        createCell(headerRow, col++, "Remarks", headerStyle);
        createCell(headerRow, col++, "id", headerStyle);
        int idCol = col - 1;
        createCell(headerRow, col++, "cppPlantId", headerStyle);
        int cppPlantIdCol = col - 1;
        createCell(headerRow, col++, "sourceId", headerStyle);
        int sourceIdCol = col - 1;
        createCell(headerRow, col++, "normParameterFkId", headerStyle);
        int normParameterFkIdCol = col - 1;
        createCell(headerRow, col++, "processPlantCode", headerStyle);
        int processPlantCodeCol = col - 1;

        int totalColumns = col;

        // Data rows
        int rowCount = 0;
        if (dataList != null) {
            for (CPPProcessUnitAllocationDTO dto : dataList) {
                rowCount++;
                Row row = sheet.createRow(currentRow++);
                col = 0;
                logger.debug("[Excel Generation] Writing row {} for processUnit: {}", rowCount, dto.getProcessUnit());

                createCell(row, col++, dto.getProcurementPlant(), dataStyle);
                createCell(row, col++, dto.getPlantName(), dataStyle);
                createCell(row, col++, dto.getUtility(), dataStyle);
                createCell(row, col++, dto.getMaterial(), dataStyle);
                createCell(row, col++, dto.getUom(), dataStyle);
                createCell(row, col++, dto.getProcessUnit(), dataStyle);

                // Apr: Power Qty, Illustrative Qty, Balance
                setNumericCell(row, col++, dto.getSourceApr() != null ? dto.getSourceApr().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getApr() != null ? dto.getApr().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getBalanceApr() != null ? dto.getBalanceApr().doubleValue() : null, dataStyle);
                // May
                setNumericCell(row, col++, dto.getSourceMay() != null ? dto.getSourceMay().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getMay() != null ? dto.getMay().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getBalanceMay() != null ? dto.getBalanceMay().doubleValue() : null, dataStyle);
                // Jun
                setNumericCell(row, col++, dto.getSourceJun() != null ? dto.getSourceJun().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getJun() != null ? dto.getJun().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getBalanceJun() != null ? dto.getBalanceJun().doubleValue() : null, dataStyle);
                // Jul
                setNumericCell(row, col++, dto.getSourceJul() != null ? dto.getSourceJul().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getJul() != null ? dto.getJul().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getBalanceJul() != null ? dto.getBalanceJul().doubleValue() : null, dataStyle);
                // Aug
                setNumericCell(row, col++, dto.getSourceAug() != null ? dto.getSourceAug().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getAug() != null ? dto.getAug().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getBalanceAug() != null ? dto.getBalanceAug().doubleValue() : null, dataStyle);
                // Sep
                setNumericCell(row, col++, dto.getSourceSep() != null ? dto.getSourceSep().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getSep() != null ? dto.getSep().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getBalanceSep() != null ? dto.getBalanceSep().doubleValue() : null, dataStyle);
                // Oct
                setNumericCell(row, col++, dto.getSourceOct() != null ? dto.getSourceOct().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getOct() != null ? dto.getOct().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getBalanceOct() != null ? dto.getBalanceOct().doubleValue() : null, dataStyle);
                // Nov
                setNumericCell(row, col++, dto.getSourceNov() != null ? dto.getSourceNov().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getNov() != null ? dto.getNov().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getBalanceNov() != null ? dto.getBalanceNov().doubleValue() : null, dataStyle);
                // Dec
                setNumericCell(row, col++, dto.getSourceDec() != null ? dto.getSourceDec().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getDec() != null ? dto.getDec().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getBalanceDec() != null ? dto.getBalanceDec().doubleValue() : null, dataStyle);
                // Jan
                setNumericCell(row, col++, dto.getSourceJan() != null ? dto.getSourceJan().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getJan() != null ? dto.getJan().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getBalanceJan() != null ? dto.getBalanceJan().doubleValue() : null, dataStyle);
                // Feb
                setNumericCell(row, col++, dto.getSourceFeb() != null ? dto.getSourceFeb().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getFeb() != null ? dto.getFeb().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getBalanceFeb() != null ? dto.getBalanceFeb().doubleValue() : null, dataStyle);
                // Mar
                setNumericCell(row, col++, dto.getSourceMar() != null ? dto.getSourceMar().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getMar() != null ? dto.getMar().doubleValue() : null, dataStyle);
                setNumericCell(row, col++, dto.getBalanceMar() != null ? dto.getBalanceMar().doubleValue() : null, dataStyle);

                createCell(row, col++, dto.getRemarks(), remarksStyle);
                createCell(row, col++, dto.getId() != null ? dto.getId().toString() : "", dataStyle);
                createCell(row, col++, dto.getCppPlantId() != null ? dto.getCppPlantId().toString() : "", dataStyle);
                createCell(row, col++, dto.getSourceId() != null ? dto.getSourceId().toString() : "", dataStyle);
                createCell(row, col++, dto.getNormParameterFkId() != null ? dto.getNormParameterFkId().toString() : "", dataStyle);
                createCell(row, col++, dto.getProcessPlantCode() != null ? dto.getProcessPlantCode() : "", dataStyle);
            }
        }

        // Hide ID columns
        sheet.setColumnHidden(idCol, true);
        sheet.setColumnHidden(cppPlantIdCol, true);
        sheet.setColumnHidden(sourceIdCol, true);
        sheet.setColumnHidden(normParameterFkIdCol, true);
        sheet.setColumnHidden(processPlantCodeCol, true);

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

    private List<CPPProcessUnitAllocationDTO> readAllocationExcel(InputStream inputStream, String aopYear) throws Exception {
        List<CPPProcessUnitAllocationDTO> records = new ArrayList<>();

        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            // Export column layout (must stay in sync with generateAllocationExcel):
            // col 0  = Procurement Plant
            // col 1  = Plant Name
            // col 2  = Utility
            // col 3  = Material
            // col 4  = UOM
            // col 5  = Process Unit
            // For each month (12 months × 3 columns = 36, cols 6-41):
            //   col N   = {Month} Power Qty      (source{Month})
            //   col N+1 = {Month} Illustrative Qty ({month})
            //   col N+2 = {Month} Balance        (balance{Month})
            // col 42 = Remarks
            // col 43 = id (hidden)
            // col 44 = cppPlantId (hidden)
            // col 45 = sourceId (hidden)
            // col 46 = normParameterFkId (hidden)
            // col 47 = processPlantCode (hidden)

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

                CPPProcessUnitAllocationDTO dto = new CPPProcessUnitAllocationDTO();

                dto.setProcurementPlant(getCellValue(row, 0));
                dto.setPlantName(getCellValue(row, 1));
                dto.setUtility(getCellValue(row, 2));
                dto.setMaterial(getCellValue(row, 3));
                dto.setUom(getCellValue(row, 4));
                dto.setProcessUnit(getCellValue(row, 5));

                // Apr: Power Qty (6), Illustrative Qty (7), Balance (8)
                dto.setSourceApr(getCellBigDecimalValue(row, 6));
                dto.setApr(getCellBigDecimalValue(row, 7));
                dto.setBalanceApr(getCellBigDecimalValue(row, 8));
                // May: Power Qty (9), Illustrative Qty (10), Balance (11)
                dto.setSourceMay(getCellBigDecimalValue(row, 9));
                dto.setMay(getCellBigDecimalValue(row, 10));
                dto.setBalanceMay(getCellBigDecimalValue(row, 11));
                // Jun: Power Qty (12), Illustrative Qty (13), Balance (14)
                dto.setSourceJun(getCellBigDecimalValue(row, 12));
                dto.setJun(getCellBigDecimalValue(row, 13));
                dto.setBalanceJun(getCellBigDecimalValue(row, 14));
                // Jul: Power Qty (15), Illustrative Qty (16), Balance (17)
                dto.setSourceJul(getCellBigDecimalValue(row, 15));
                dto.setJul(getCellBigDecimalValue(row, 16));
                dto.setBalanceJul(getCellBigDecimalValue(row, 17));
                // Aug: Power Qty (18), Illustrative Qty (19), Balance (20)
                dto.setSourceAug(getCellBigDecimalValue(row, 18));
                dto.setAug(getCellBigDecimalValue(row, 19));
                dto.setBalanceAug(getCellBigDecimalValue(row, 20));
                // Sep: Power Qty (21), Illustrative Qty (22), Balance (23)
                dto.setSourceSep(getCellBigDecimalValue(row, 21));
                dto.setSep(getCellBigDecimalValue(row, 22));
                dto.setBalanceSep(getCellBigDecimalValue(row, 23));
                // Oct: Power Qty (24), Illustrative Qty (25), Balance (26)
                dto.setSourceOct(getCellBigDecimalValue(row, 24));
                dto.setOct(getCellBigDecimalValue(row, 25));
                dto.setBalanceOct(getCellBigDecimalValue(row, 26));
                // Nov: Power Qty (27), Illustrative Qty (28), Balance (29)
                dto.setSourceNov(getCellBigDecimalValue(row, 27));
                dto.setNov(getCellBigDecimalValue(row, 28));
                dto.setBalanceNov(getCellBigDecimalValue(row, 29));
                // Dec: Power Qty (30), Illustrative Qty (31), Balance (32)
                dto.setSourceDec(getCellBigDecimalValue(row, 30));
                dto.setDec(getCellBigDecimalValue(row, 31));
                dto.setBalanceDec(getCellBigDecimalValue(row, 32));
                // Jan: Power Qty (33), Illustrative Qty (34), Balance (35)
                dto.setSourceJan(getCellBigDecimalValue(row, 33));
                dto.setJan(getCellBigDecimalValue(row, 34));
                dto.setBalanceJan(getCellBigDecimalValue(row, 35));
                // Feb: Power Qty (36), Illustrative Qty (37), Balance (38)
                dto.setSourceFeb(getCellBigDecimalValue(row, 36));
                dto.setFeb(getCellBigDecimalValue(row, 37));
                dto.setBalanceFeb(getCellBigDecimalValue(row, 38));
                // Mar: Power Qty (39), Illustrative Qty (40), Balance (41)
                dto.setSourceMar(getCellBigDecimalValue(row, 39));
                dto.setMar(getCellBigDecimalValue(row, 40));
                dto.setBalanceMar(getCellBigDecimalValue(row, 41));

                dto.setRemarks(getCellValue(row, 42));

                String idStr = getCellValue(row, 43);
                if (idStr != null && !idStr.trim().isEmpty()) {
                    try {
                        dto.setId(UUID.fromString(idStr));
                    } catch (IllegalArgumentException e) {
                        logger.warn("[IMPORT] Invalid UUID format for ID: {}", idStr);
                    }
                }

                String cppPlantIdStr = getCellValue(row, 44);
                if (cppPlantIdStr != null && !cppPlantIdStr.trim().isEmpty()) {
                    try {
                        dto.setCppPlantId(UUID.fromString(cppPlantIdStr));
                    } catch (IllegalArgumentException e) {
                        logger.warn("[IMPORT] Invalid UUID format for cppPlantId: {}", cppPlantIdStr);
                    }
                }

                String sourceIdStr = getCellValue(row, 45);
                if (sourceIdStr != null && !sourceIdStr.trim().isEmpty()) {
                    try {
                        dto.setSourceId(UUID.fromString(sourceIdStr));
                    } catch (IllegalArgumentException e) {
                        logger.warn("[IMPORT] Invalid UUID format for sourceId: {}", sourceIdStr);
                    }
                }

                String normParameterFkIdStr = getCellValue(row, 46);
                if (normParameterFkIdStr != null && !normParameterFkIdStr.trim().isEmpty()) {
                    try {
                        dto.setNormParameterFkId(UUID.fromString(normParameterFkIdStr));
                    } catch (IllegalArgumentException e) {
                        logger.warn("[IMPORT] Invalid UUID format for normParameterFkId: {}", normParameterFkIdStr);
                    }
                }

                String processPlantCode = getCellValue(row, 47);
                if (processPlantCode != null && !processPlantCode.trim().isEmpty()) {
                    dto.setProcessPlantCode(processPlantCode);
                }

                dto.setAopYear(aopYear);

                records.add(dto);
            }
        }

        return records;
    }

    // ========================================
    // ERROR EXCEL GENERATION
    // ========================================

    private byte[] generateAllocationErrorExcel(List<CPPProcessUnitAllocationDTO> failedRecords,
            List<String> failureReasons, String sheetName, String aopYear) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle remarksStyle = createRemarksStyle(workbook);
            CellStyle errorStyle = createErrorCellStyle(workbook);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Procurement Plant", "Plant Name", "Utility", "Material", "UOM", "Process Unit",
                    "Apr Power Qty", "Apr Illustrative Qty", "Apr Balance",
                    "May Power Qty", "May Illustrative Qty", "May Balance",
                    "Jun Power Qty", "Jun Illustrative Qty", "Jun Balance",
                    "Jul Power Qty", "Jul Illustrative Qty", "Jul Balance",
                    "Aug Power Qty", "Aug Illustrative Qty", "Aug Balance",
                    "Sep Power Qty", "Sep Illustrative Qty", "Sep Balance",
                    "Oct Power Qty", "Oct Illustrative Qty", "Oct Balance",
                    "Nov Power Qty", "Nov Illustrative Qty", "Nov Balance",
                    "Dec Power Qty", "Dec Illustrative Qty", "Dec Balance",
                    "Jan Power Qty", "Jan Illustrative Qty", "Jan Balance",
                    "Feb Power Qty", "Feb Illustrative Qty", "Feb Balance",
                    "Mar Power Qty", "Mar Illustrative Qty", "Mar Balance",
                    "Remarks", "id", "cppPlantId", "sourceId", "normParameterFkId", "processPlantCode", "Status", "Comment"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (int i = 0; i < failedRecords.size(); i++) {
                CPPProcessUnitAllocationDTO dto = failedRecords.get(i);
                String failureReason = failureReasons.get(i);
                Row row = sheet.createRow(rowNum++);

                createCell(row, 0, dto.getProcurementPlant(), dataStyle);
                createCell(row, 1, dto.getPlantName(), dataStyle);
                createCell(row, 2, dto.getUtility(), dataStyle);
                createCell(row, 3, dto.getMaterial(), dataStyle);
                createCell(row, 4, dto.getUom(), dataStyle);
                createCell(row, 5, dto.getProcessUnit(), dataStyle);

                // Apr: Power Qty, Illustrative Qty, Balance
                setNumericCell(row, 6, dto.getSourceApr() != null ? dto.getSourceApr().doubleValue() : null, dataStyle);
                setNumericCell(row, 7, dto.getApr() != null ? dto.getApr().doubleValue() : null, dataStyle);
                setNumericCell(row, 8, dto.getBalanceApr() != null ? dto.getBalanceApr().doubleValue() : null, dataStyle);
                // May
                setNumericCell(row, 9, dto.getSourceMay() != null ? dto.getSourceMay().doubleValue() : null, dataStyle);
                setNumericCell(row, 10, dto.getMay() != null ? dto.getMay().doubleValue() : null, dataStyle);
                setNumericCell(row, 11, dto.getBalanceMay() != null ? dto.getBalanceMay().doubleValue() : null, dataStyle);
                // Jun
                setNumericCell(row, 12, dto.getSourceJun() != null ? dto.getSourceJun().doubleValue() : null, dataStyle);
                setNumericCell(row, 13, dto.getJun() != null ? dto.getJun().doubleValue() : null, dataStyle);
                setNumericCell(row, 14, dto.getBalanceJun() != null ? dto.getBalanceJun().doubleValue() : null, dataStyle);
                // Jul
                setNumericCell(row, 15, dto.getSourceJul() != null ? dto.getSourceJul().doubleValue() : null, dataStyle);
                setNumericCell(row, 16, dto.getJul() != null ? dto.getJul().doubleValue() : null, dataStyle);
                setNumericCell(row, 17, dto.getBalanceJul() != null ? dto.getBalanceJul().doubleValue() : null, dataStyle);
                // Aug
                setNumericCell(row, 18, dto.getSourceAug() != null ? dto.getSourceAug().doubleValue() : null, dataStyle);
                setNumericCell(row, 19, dto.getAug() != null ? dto.getAug().doubleValue() : null, dataStyle);
                setNumericCell(row, 20, dto.getBalanceAug() != null ? dto.getBalanceAug().doubleValue() : null, dataStyle);
                // Sep
                setNumericCell(row, 21, dto.getSourceSep() != null ? dto.getSourceSep().doubleValue() : null, dataStyle);
                setNumericCell(row, 22, dto.getSep() != null ? dto.getSep().doubleValue() : null, dataStyle);
                setNumericCell(row, 23, dto.getBalanceSep() != null ? dto.getBalanceSep().doubleValue() : null, dataStyle);
                // Oct
                setNumericCell(row, 24, dto.getSourceOct() != null ? dto.getSourceOct().doubleValue() : null, dataStyle);
                setNumericCell(row, 25, dto.getOct() != null ? dto.getOct().doubleValue() : null, dataStyle);
                setNumericCell(row, 26, dto.getBalanceOct() != null ? dto.getBalanceOct().doubleValue() : null, dataStyle);
                // Nov
                setNumericCell(row, 27, dto.getSourceNov() != null ? dto.getSourceNov().doubleValue() : null, dataStyle);
                setNumericCell(row, 28, dto.getNov() != null ? dto.getNov().doubleValue() : null, dataStyle);
                setNumericCell(row, 29, dto.getBalanceNov() != null ? dto.getBalanceNov().doubleValue() : null, dataStyle);
                // Dec
                setNumericCell(row, 30, dto.getSourceDec() != null ? dto.getSourceDec().doubleValue() : null, dataStyle);
                setNumericCell(row, 31, dto.getDec() != null ? dto.getDec().doubleValue() : null, dataStyle);
                setNumericCell(row, 32, dto.getBalanceDec() != null ? dto.getBalanceDec().doubleValue() : null, dataStyle);
                // Jan
                setNumericCell(row, 33, dto.getSourceJan() != null ? dto.getSourceJan().doubleValue() : null, dataStyle);
                setNumericCell(row, 34, dto.getJan() != null ? dto.getJan().doubleValue() : null, dataStyle);
                setNumericCell(row, 35, dto.getBalanceJan() != null ? dto.getBalanceJan().doubleValue() : null, dataStyle);
                // Feb
                setNumericCell(row, 36, dto.getSourceFeb() != null ? dto.getSourceFeb().doubleValue() : null, dataStyle);
                setNumericCell(row, 37, dto.getFeb() != null ? dto.getFeb().doubleValue() : null, dataStyle);
                setNumericCell(row, 38, dto.getBalanceFeb() != null ? dto.getBalanceFeb().doubleValue() : null, dataStyle);
                // Mar
                setNumericCell(row, 39, dto.getSourceMar() != null ? dto.getSourceMar().doubleValue() : null, dataStyle);
                setNumericCell(row, 40, dto.getMar() != null ? dto.getMar().doubleValue() : null, dataStyle);
                setNumericCell(row, 41, dto.getBalanceMar() != null ? dto.getBalanceMar().doubleValue() : null, dataStyle);

                createCell(row, 42, dto.getRemarks(), remarksStyle);
                createCell(row, 43, dto.getId() != null ? dto.getId().toString() : "", dataStyle);
                createCell(row, 44, dto.getCppPlantId() != null ? dto.getCppPlantId().toString() : "", dataStyle);
                createCell(row, 45, dto.getSourceId() != null ? dto.getSourceId().toString() : "", dataStyle);
                createCell(row, 46, dto.getNormParameterFkId() != null ? dto.getNormParameterFkId().toString() : "", dataStyle);
                createCell(row, 47, dto.getProcessPlantCode() != null ? dto.getProcessPlantCode() : "", dataStyle);
                createCell(row, 48, "Failed", errorStyle);
                createCell(row, 49, failureReason, errorStyle);
            }

            sheet.setColumnHidden(43, true);
            sheet.setColumnHidden(44, true);
            sheet.setColumnHidden(45, true);
            sheet.setColumnHidden(46, true);
            sheet.setColumnHidden(47, true);

            for (int i = 0; i < headers.length; i++) {
                if (i == 42 || i == 49) {
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
    // EXCEL STYLE & CELL HELPERS
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
        return createDataStyle(workbook);
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
}
