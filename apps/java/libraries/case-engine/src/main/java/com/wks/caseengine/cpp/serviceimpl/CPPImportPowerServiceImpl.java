package com.wks.caseengine.cpp.serviceimpl;

import com.wks.caseengine.cpp.dto.CPPImportPowerProjection;
import com.wks.caseengine.cpp.entity.CPPImportPower;
import com.wks.caseengine.cpp.repository.CPPImportPowerRepository;
import com.wks.caseengine.cpp.service.CPPImportPowerService;
import com.wks.caseengine.dto.CPPImportPowerResponseDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class CPPImportPowerServiceImpl implements CPPImportPowerService {

    private static final Logger logger = LoggerFactory.getLogger(CPPImportPowerServiceImpl.class);

    @Autowired
    private CPPImportPowerRepository repository;

    @Override
    public AOPMessageVM getImportedPowerPlans(List<UUID> plantIds, String aopYear) {
        logger.info("[GET Service] Fetching imported power plans for plantIds: {}, aopYear: {}", plantIds, aopYear);
        AOPMessageVM aopMessageVM = new AOPMessageVM();

        try {
            List<CPPImportPowerProjection> projections = repository.findImportedPowerPlans(plantIds, aopYear);
            logger.info("[GET Service] Query returned {} records", projections.size());

            List<CPPImportPowerResponseDTO> results = projections.stream()
                    .map(this::mapToDto)
                    .toList();

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
        dto.setUtility(projection.getUtility());
        dto.setMaterial(projection.getMaterial());
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

        return dto;
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
                        missingRecord.put("utility", dto.getUtility() != null ? dto.getUtility() : "Unknown");
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
                        missingRecord.put("utility", dto.getUtility() != null ? dto.getUtility() : "Unknown");
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

    private boolean isRecordModified(CPPImportPowerResponseDTO dto, CPPImportPower entity) {
        try {
            if (!Objects.equals(dto.getApr(), entity.getApr())) return true;
            if (!Objects.equals(dto.getMay(), entity.getMay())) return true;
            if (!Objects.equals(dto.getJun(), entity.getJun())) return true;
            if (!Objects.equals(dto.getJul(), entity.getJul())) return true;
            if (!Objects.equals(dto.getAug(), entity.getAug())) return true;
            if (!Objects.equals(dto.getSep(), entity.getSep())) return true;
            if (!Objects.equals(dto.getOct(), entity.getOct())) return true;
            if (!Objects.equals(dto.getNov(), entity.getNov())) return true;
            if (!Objects.equals(dto.getDec(), entity.getDec())) return true;
            if (!Objects.equals(dto.getJan(), entity.getJan())) return true;
            if (!Objects.equals(dto.getFeb(), entity.getFeb())) return true;
            if (!Objects.equals(dto.getMar(), entity.getMar())) return true;
            if (!Objects.equals(dto.getRemarks(), entity.getRemarks())) return true;
            return false;
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
}
