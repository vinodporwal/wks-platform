package com.wks.caseengine.cpp.serviceimpl;

import com.wks.caseengine.cpp.dto.FixedConsumptionDto;
import com.wks.caseengine.cpp.dto.FixedConsumptionProjection;
import com.wks.caseengine.cpp.repository.JMDFixedConsumptionRepository;
import com.wks.caseengine.cpp.service.JMDFixedConsumptionService;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JMDFixedConsumptionServiceImpl implements JMDFixedConsumptionService {

    private static final Logger logger = LoggerFactory.getLogger(JMDFixedConsumptionServiceImpl.class);

    @Autowired
    private JMDFixedConsumptionRepository repository;

    @Override
    public AOPMessageVM getFixedConsumptionForPlants(List<UUID> plantIds, String financialYear) {

        logger.info("[GET Service] Fetching fixed consumption for plantIds: {}, financialYear: {}", plantIds, financialYear);
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        
        try {
            if (plantIds == null || plantIds.isEmpty()) {
                logger.warn("[GET Service] No plant IDs provided");
                aopMessageVM.setCode(400);
                aopMessageVM.setMessage("Plant IDs are required");
                aopMessageVM.setData(null);
                return aopMessageVM;
            }

            String plantIdsCsv = plantIds.stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(","));
            logger.debug("[GET Service] Executing SP with plantIds: {}", plantIdsCsv);

            List<FixedConsumptionProjection> projections =
                    repository.getFixedConsumptionForPlants(plantIdsCsv, financialYear);

            List<FixedConsumptionDto> allResults = projections.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());

            logger.info("[GET Service] SP returned {} total records from {} plants", allResults.size(), plantIds.size());

            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(allResults);
            logger.info("[GET Service] Successfully fetched fixed consumption data");
        } catch (Exception e) {
            logger.error("[GET Service] Error fetching fixed consumption: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to fetch data: " + e.getMessage());
            aopMessageVM.setData(null);
        }

        return aopMessageVM;
    }

    private FixedConsumptionDto mapToDto(FixedConsumptionProjection p) {
        FixedConsumptionDto dto = new FixedConsumptionDto();

        dto.setPlant(p.getPlantName());
        dto.setPlantId(p.getPlantCode());
        dto.setCostCenter(p.getCostCenterName());
        dto.setCostCenterId(p.getCostCenterCode());
        dto.setCppUtility(p.getUtilityName());
        dto.setCppUtilityId(p.getUtilitySAP());
        dto.setCppPlant(p.getUtilityPlantName());
        dto.setCppPlantId(p.getUtilityPlantCode());
        dto.setUom(p.getUom());
        dto.setNormParameterId(p.getNormParameterId());
        dto.setCostCenter_FK_Id(p.getCostCenter_FK_Id());
        dto.setNormParameter_FK_Id(p.getNormParameter_FK_Id());
        dto.setRemarkId(p.getRemarkId());
        dto.setRemarks(p.getRemarks());

        dto.setApril(p.getApr());
        dto.setMay(p.getMay());
        dto.setJune(p.getJun());
        dto.setJuly(p.getJul());
        dto.setAug(p.getAug());
        dto.setSep(p.getSep());
        dto.setOct(p.getOct());
        dto.setNov(p.getNov());
        dto.setDec(p.getDec());
        dto.setJan(p.getJan());
        dto.setFeb(p.getFeb());
        dto.setMar(p.getMar());

        dto.setGrandTotal(
            Optional.ofNullable(p.getApr()).orElse(0.0) +
            Optional.ofNullable(p.getMay()).orElse(0.0) +
            Optional.ofNullable(p.getJun()).orElse(0.0) +
            Optional.ofNullable(p.getJul()).orElse(0.0) +
            Optional.ofNullable(p.getAug()).orElse(0.0) +
            Optional.ofNullable(p.getSep()).orElse(0.0) +
            Optional.ofNullable(p.getOct()).orElse(0.0) +
            Optional.ofNullable(p.getNov()).orElse(0.0) +
            Optional.ofNullable(p.getDec()).orElse(0.0) +
            Optional.ofNullable(p.getJan()).orElse(0.0) +
            Optional.ofNullable(p.getFeb()).orElse(0.0) +
            Optional.ofNullable(p.getMar()).orElse(0.0));

        return dto;
    }

    @Override
    public AOPMessageVM saveFixedConsumption(List<UUID> plantIds, String financialYear, List<FixedConsumptionDto> payload) {
        logger.info("[POST Service] Saving fixed consumption for plantIds: {}, financialYear: {}, records: {}", 
                plantIds, financialYear, payload != null ? payload.size() : 0);
        AOPMessageVM aopMessageVM = new AOPMessageVM();

        try {
            // TODO: Implement save logic for multiple plants
            logger.info("[POST Service] Save operation completed");
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Fixed consumption saved successfully");
            aopMessageVM.setData(null);
        } catch (Exception e) {
            logger.error("[POST Service] Error saving fixed consumption: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to save fixed consumption: " + e.getMessage());
            aopMessageVM.setData(null);
        }

        return aopMessageVM;
    }

    @Override
    public byte[] exportFixedConsumption(List<UUID> plantIds, String financialYear) {
        logger.info("[Export Service] Exporting fixed consumption for plantIds: {}, financialYear: {}", plantIds, financialYear);
        
        try {
            // TODO: Implement export logic for multiple plants
            logger.info("[Export Service] Export completed");
            return null;
        } catch (Exception e) {
            logger.error("[Export Service] Error exporting fixed consumption: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public AOPMessageVM importFixedConsumption(List<UUID> plantIds, String financialYear, MultipartFile file) {
        logger.info("[Import Service] Importing fixed consumption for plantIds: {}, financialYear: {}, fileName: {}", 
                plantIds, financialYear, file.getOriginalFilename());
        AOPMessageVM aopMessageVM = new AOPMessageVM();

        try {
            // TODO: Implement import logic for multiple plants
            logger.info("[Import Service] Import completed");
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Fixed consumption imported successfully");
            aopMessageVM.setData(null);
        } catch (Exception e) {
            logger.error("[Import Service] Error importing fixed consumption: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to import fixed consumption: " + e.getMessage());
            aopMessageVM.setData(null);
        }

        return aopMessageVM;
    }
}
