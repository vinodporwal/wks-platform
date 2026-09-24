package com.wks.caseengine.cpp.serviceimpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.cpp.dto.CPPEfficiencyDTO;
import com.wks.caseengine.cpp.dto.CPPFuelRatioDTO;
import com.wks.caseengine.cpp.entity.CPPEfficiency;
import com.wks.caseengine.cpp.entity.CPPFuelRatio;
import com.wks.caseengine.cpp.entity.CppSteamGenerationAsset;
import com.wks.caseengine.cpp.entity.PowerGenerationAsset;
import com.wks.caseengine.cpp.repository.CPPEfficiencyRepository;
import com.wks.caseengine.cpp.repository.CPPFuelRatioRepository;
import com.wks.caseengine.cpp.repository.CppSteamGenerationAssetRepository;
import com.wks.caseengine.cpp.repository.PowerGenerationAssetRepository;
import com.wks.caseengine.cpp.service.JMDEfficiencyAndFuelRatioService;
import com.wks.caseengine.message.vm.AOPMessageVM;

@Service
public class JMDEfficiencyAndFuelRatioServiceImpl implements JMDEfficiencyAndFuelRatioService {

    private static final Logger logger = LoggerFactory.getLogger(JMDEfficiencyAndFuelRatioServiceImpl.class);

    @Autowired
    private CPPEfficiencyRepository efficiencyRepository;

    @Autowired
    private CPPFuelRatioRepository fuelRatioRepository;

    @Autowired
    private PowerGenerationAssetRepository powerGenerationAssetRepository;

    @Autowired
    private CppSteamGenerationAssetRepository cppSteamGenerationAssetRepository;

    // ── Efficiency ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM getEfficiency(List<UUID> plantIds, String aopYear) {
        logger.info("[Efficiency] GET - plantIds: {}, aopYear: {}", plantIds, aopYear);
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

            List<CPPEfficiency> entities =
                    efficiencyRepository.findByCppPlantFkIdInAndAopYearOrderByAssetName(plantIds, aopYear);

            // No rows configured for this year yet — seed one row per asset from
            // PowerGenerationAssets (Type='Power') + CPPSteamGenerationAsset
            // (Type='Steam') with default Value = 0.
            if (entities.isEmpty()) {
                entities = seedEfficiencyFromAssetMasters(plantIds, aopYear);
            }

            List<CPPEfficiencyDTO> result = new ArrayList<>();
            for (CPPEfficiency entity : entities) {
                // Only active assets are shown on screen. IsActive is managed
                // directly in the DB (no UI toggle).
                if (Boolean.FALSE.equals(entity.getIsActive())) continue;
                CPPEfficiencyDTO dto = new CPPEfficiencyDTO();
                dto.setId(entity.getId());
                dto.setCppPlantFkId(entity.getCppPlantFkId());
                dto.setAssetFkId(entity.getAssetFkId());
                dto.setAssetName(entity.getAssetName());
                dto.setType(entity.getType());
                dto.setIsActive(entity.getIsActive());
                dto.setUom(entity.getUom());
                dto.setValue(entity.getValue());
                dto.setRemarks(entity.getRemarks());
                dto.setAopYear(entity.getAopYear());
                result.add(dto);
            }

            logger.info("[Efficiency] GET - found {} records", result.size());

            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(result);

        } catch (Exception e) {
            logger.error("[Efficiency] GET error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
        }

        return vm;
    }

    @Override
    @Transactional
    public AOPMessageVM saveEfficiency(List<UUID> plantIds, String aopYear,
                                       List<CPPEfficiencyDTO> dtoList) {
        logger.info("[Efficiency] SAVE - plantIds: {}, aopYear: {}, records: {}",
                plantIds, aopYear, dtoList != null ? dtoList.size() : 0);
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

            for (CPPEfficiencyDTO dto : dtoList) {
                try {
                    if (dto.getCppPlantFkId() == null) {
                        errorCount++;
                        errorMessages.add("Record skipped: cppPlantFkId is null");
                        continue;
                    }

                    if (dto.getId() == null) {
                        // CREATE via JPA
                        CPPEfficiency entity = new CPPEfficiency();
                        entity.setCppPlantFkId(dto.getCppPlantFkId());
                        entity.setAssetFkId(dto.getAssetFkId());
                        entity.setAssetName(dto.getAssetName());
                        entity.setType(dto.getType());
                        entity.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
                        entity.setUom(dto.getUom());
                        entity.setValue(dto.getValue());
                        entity.setRemarks(dto.getRemarks());
                        entity.setAopYear(dto.getAopYear() != null ? dto.getAopYear() : aopYear);
                        LocalDateTime now = LocalDateTime.now();
                        entity.setCreatedDate(now);
                        entity.setUpdatedDate(now);
                        efficiencyRepository.save(entity);
                        successCount++;
                    } else {
                        // UPDATE
                        efficiencyRepository.updateEfficiency(
                                dto.getId(),
                                dto.getAssetFkId(),
                                dto.getAssetName(),
                                dto.getUom(),
                                dto.getValue(),
                                dto.getRemarks());
                        successCount++;
                    }

                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = "Error processing record: " + e.getMessage();
                    errorMessages.add(errorMsg);
                    logger.error(errorMsg, e);
                }
            }

            logger.info("[Efficiency] SAVE - success: {}, errors: {}", successCount, errorCount);

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
            logger.error("[Efficiency] SAVE error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(null);
        }

        return vm;
    }

    // ── Fuel Ratio ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM getFuelRatio(List<UUID> plantIds, String aopYear) {
        logger.info("[FuelRatio] GET - plantIds: {}, aopYear: {}", plantIds, aopYear);
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

            List<CPPFuelRatio> entities =
                    fuelRatioRepository.findByCppPlantFkIdInAndAopYearOrderByFuelName(plantIds, aopYear);

            List<CPPFuelRatioDTO> result = new ArrayList<>();
            for (CPPFuelRatio entity : entities) {
                CPPFuelRatioDTO dto = new CPPFuelRatioDTO();
                dto.setId(entity.getId());
                dto.setCppPlantFkId(entity.getCppPlantFkId());
                dto.setFuelFkId(entity.getFuelFkId());
                dto.setFuelName(entity.getFuelName());
                dto.setGcv(entity.getGcv());
                dto.setPercentageByWt(entity.getPercentageByWt());
                dto.setRemarks(entity.getRemarks());
                dto.setAopYear(entity.getAopYear());
                result.add(dto);
            }

            logger.info("[FuelRatio] GET - found {} records", result.size());

            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(result);

        } catch (Exception e) {
            logger.error("[FuelRatio] GET error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
        }

        return vm;
    }

    @Override
    @Transactional
    public AOPMessageVM saveFuelRatio(List<UUID> plantIds, String aopYear,
                                      List<CPPFuelRatioDTO> dtoList) {
        logger.info("[FuelRatio] SAVE - plantIds: {}, aopYear: {}, records: {}",
                plantIds, aopYear, dtoList != null ? dtoList.size() : 0);
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

            for (CPPFuelRatioDTO dto : dtoList) {
                try {
                    if (dto.getCppPlantFkId() == null) {
                        errorCount++;
                        errorMessages.add("Record skipped: cppPlantFkId is null");
                        continue;
                    }

                    if (dto.getId() == null) {
                        // CREATE via JPA
                        CPPFuelRatio entity = new CPPFuelRatio();
                        entity.setCppPlantFkId(dto.getCppPlantFkId());
                        entity.setFuelFkId(dto.getFuelFkId());
                        entity.setFuelName(dto.getFuelName());
                        entity.setGcv(dto.getGcv());
                        entity.setPercentageByWt(dto.getPercentageByWt());
                        entity.setRemarks(dto.getRemarks());
                        entity.setAopYear(dto.getAopYear() != null ? dto.getAopYear() : aopYear);
                        LocalDateTime now = LocalDateTime.now();
                        entity.setCreatedDate(now);
                        entity.setUpdatedDate(now);
                        fuelRatioRepository.save(entity);
                        successCount++;
                    } else {
                        // UPDATE
                        fuelRatioRepository.updateFuelRatio(
                                dto.getId(),
                                dto.getFuelFkId(),
                                dto.getFuelName(),
                                dto.getGcv(),
                                dto.getPercentageByWt(),
                                dto.getRemarks());
                        successCount++;
                    }

                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = "Error processing record: " + e.getMessage();
                    errorMessages.add(errorMsg);
                    logger.error(errorMsg, e);
                }
            }

            logger.info("[FuelRatio] SAVE - success: {}, errors: {}", successCount, errorCount);

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
            logger.error("[FuelRatio] SAVE error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(null);
        }

        return vm;
    }

    /**
     * Seeds one CPP_Efficiency row per asset for the given plants + AOP year,
     * pulled from the two asset master tables:
     *   PowerGenerationAssets      -> Type = 'Power'
     *   CPPSteamGenerationAsset    -> Type = 'Steam'
     * Rows are inserted with default Value = 0. Returns the freshly seeded rows
     * (empty list if the plants have no assets configured).
     */
    private List<CPPEfficiency> seedEfficiencyFromAssetMasters(List<UUID> plantIds, String aopYear) {
        List<CPPEfficiency> seeds = new ArrayList<>();

        for (PowerGenerationAsset asset : powerGenerationAssetRepository.findByCppPlantFkIdIn(plantIds)) {
            seeds.add(newEfficiencySeed(asset.getCppPlantFkId(), asset.getAssetId(),
                    asset.getAssetName(), "Power", aopYear));
        }

        for (CppSteamGenerationAsset asset : cppSteamGenerationAssetRepository.findByCppPlantFkIdIn(plantIds)) {
            seeds.add(newEfficiencySeed(asset.getCppPlantFkId(), asset.getAssetId(),
                    asset.getAssetName(), "Steam", aopYear));
        }

        if (seeds.isEmpty()) {
            logger.warn("[Efficiency] GET - no assets found in master tables for plantIds: {}", plantIds);
            return seeds;
        }

        efficiencyRepository.saveAll(seeds);
        logger.info("[Efficiency] GET - seeded {} asset rows for {}", seeds.size(), aopYear);

        return efficiencyRepository.findByCppPlantFkIdInAndAopYearOrderByAssetName(plantIds, aopYear);
    }

    private CPPEfficiency newEfficiencySeed(UUID cppPlantFkId, UUID assetFkId,
                                            String assetName, String type, String aopYear) {
        CPPEfficiency seed = new CPPEfficiency();
        seed.setCppPlantFkId(cppPlantFkId);
        seed.setAssetFkId(assetFkId);
        seed.setAssetName(assetName);
        seed.setType(type);
        seed.setIsActive(true);
        seed.setUom("%");
        seed.setValue(0.0);
        seed.setAopYear(aopYear);
        LocalDateTime now = LocalDateTime.now();
        seed.setCreatedDate(now);
        seed.setUpdatedDate(now);
        return seed;
    }
}
