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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
}
