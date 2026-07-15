package com.wks.caseengine.cpp.serviceimpl;

import com.wks.caseengine.cpp.dto.heatrate.CppAuxBoilerHeatRateDto;
import com.wks.caseengine.cpp.dto.heatrate.CppCcppHeatRateDto;
import com.wks.caseengine.cpp.dto.heatrate.CppGtHeatRateDto;
import com.wks.caseengine.cpp.dto.heatrate.CppHrsgHeatRateDto;
import com.wks.caseengine.cpp.dto.heatrate.CppSteamGenerationAssetDto;
import com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateLookupDTO;
import com.wks.caseengine.cpp.dto.heatrate.HeatRateDTO;
import com.wks.caseengine.cpp.dto.heatrate.HeatRateProjection;
import com.wks.caseengine.cpp.dto.heatrate.PowerGenerationAssetDto;
import com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateProjection;
import com.wks.caseengine.cpp.dto.heatrate.STGHeatRateProjection;
import com.wks.caseengine.cpp.dto.heatrate.SelectedHeatRateType;
import com.wks.caseengine.cpp.dto.heatrate.STGHeatRateDTO;
import com.wks.caseengine.cpp.dto.heatrate.STGExtractionLookupDTO;
import com.wks.caseengine.cpp.entity.CppAuxBoilerHeatRate;
import com.wks.caseengine.cpp.entity.CppCCPPHeatRate;
import com.wks.caseengine.cpp.entity.CppGtHeatRate;
import com.wks.caseengine.cpp.entity.CppHrsgHeatRate;
import com.wks.caseengine.cpp.entity.CppSteamGenerationAsset;
import com.wks.caseengine.cpp.entity.CppStgHeatRate;
import com.wks.caseengine.cpp.entity.HRSGHeatRateLookup;
import com.wks.caseengine.cpp.entity.PowerGenerationAsset;
import com.wks.caseengine.cpp.entity.STGExtractionLookup;
import com.wks.caseengine.cpp.repository.CppAuxBoilerHeatRateRepository;
import com.wks.caseengine.cpp.repository.CppCCPPHeatRateRepository;
import com.wks.caseengine.cpp.repository.CppGtHeatRateRepository;
import com.wks.caseengine.cpp.repository.CppHrsgHeatRateRepository;
import com.wks.caseengine.cpp.repository.CppSteamGenerationAssetRepository;
import com.wks.caseengine.cpp.repository.CppStgHeatRateRepository;
import com.wks.caseengine.cpp.repository.JMDHRSGHeatRateLookupRepository;
import com.wks.caseengine.cpp.repository.JMDHeatRateRepository;
import com.wks.caseengine.cpp.repository.JMDSTGExtractionLookupRepository;
import com.wks.caseengine.cpp.repository.PowerGenerationAssetRepository;
import com.wks.caseengine.cpp.repository.SteamGenerationAssetRepository;
import com.wks.caseengine.cpp.service.JMDHeatRateService;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
public class JMDHeatRateServiceImpl implements JMDHeatRateService {

    private static final Logger logger = LoggerFactory.getLogger(JMDHeatRateServiceImpl.class);

    @Autowired
    private JMDHeatRateRepository heatRateRepository;

    @Autowired
    private JMDSTGExtractionLookupRepository stgExtractionLookupRepository;

    @Autowired
    private JMDHRSGHeatRateLookupRepository hrsgHeatRateLookupRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private  PowerGenerationAssetRepository assetRepository;
    
    @Autowired
    private  CppSteamGenerationAssetRepository cppSteamGenerationAssetRepository;
    
    @Autowired
    private CppGtHeatRateRepository cppGtHeatRateRepository;
    
    @Autowired
    private CppHrsgHeatRateRepository cppHrsgHeatRateRepository;
    
    @Autowired
    private CppStgHeatRateRepository cppStgHeatRateRepository;
    
    @Autowired
    private CppAuxBoilerHeatRateRepository cppAuxBoilerHeatRateRepository;
    
    @Autowired
    private CppCCPPHeatRateRepository cppCCPPHeatRateRepository;
    
    
    
    // ============================================================
    // DROPDOWN METHODS (multi-plant)
    // ============================================================
    
    @Override
    public AOPMessageVM getGTAssetDropdown(List<UUID> plantIds,String assetType) {
        logger.info("[JMDHeatRate] getGTAssetDropdown - plantIds: {}, assetType: {}", plantIds, assetType);
        AOPMessageVM vm = new AOPMessageVM();
        
        try {
            // String assetType = "GT";
            List<PowerGenerationAsset> entities = assetRepository.findByPlantIdsAndAssetType(plantIds, assetType);

            List<PowerGenerationAssetDto> result = entities.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            logger.info("[JMDHeatRate] getGTAssetDropdown - found {} assets", result.size());
            
           
            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(result);
            
        } catch (Exception e) {
            logger.error("[JMDHeatRate] getGTAssetDropdown error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to fetch GT asset dropdown: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }    

    @Override
    public AOPMessageVM getHRSGAssetDropdown(List<UUID> plantIds,String assetType) {
        logger.info("[JMDHeatRate] getHRSGAssetDropdown - plantIds: {}, assetType: {}", plantIds, assetType);
        AOPMessageVM vm = new AOPMessageVM();
        
        try {
            // String assetType = "GT";
            List<CppSteamGenerationAsset> entities = cppSteamGenerationAssetRepository.findByPlantIdsAndAssetType(plantIds, assetType);

            List<CppSteamGenerationAssetDto> result = entities.stream()
                    .map(this::convertToCppSteamGenerationAssetDto)
                    .collect(Collectors.toList());

            logger.info("[JMDHeatRate] getGTAssetDropdown - found {} assets", result.size());
            
           
            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(result);
            
        } catch (Exception e) {
            logger.error("[JMDHeatRate] getGTAssetDropdown error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to fetch GT asset dropdown: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }    

    
    @Override
    public AOPMessageVM getGTHeatRateData(UUID assetId, String year, String startDate, String endDate, List<UUID> plantIds) {
        logger.info("[JMDHeatRate] getGTHeatRateData - assetId: {}, year: {}, startDate: {}, endDate: {}, plantIds: {}", 
                assetId, year, startDate, endDate, plantIds);
        
        AOPMessageVM vm = new AOPMessageVM();
        
        try {
            String prevYear = null;
            if (year != null && year.contains("-")) {
                String[] parts = year.split("-");
                int startYear = Integer.parseInt(parts[0]);
                int endYear = Integer.parseInt(parts[1]);
                prevYear = (startYear - 1) + "-" + (endYear - 1);
            }

            List<CppGtHeatRate> entities = cppGtHeatRateRepository.findByAssetFkIdAndFinancialYearNative(assetId, year);
            if (entities == null) {
                entities = new java.util.ArrayList<>();
            }
            
            List<CppGtHeatRate> prevEntities = new java.util.ArrayList<>();
            if (prevYear != null) {
                prevEntities = cppGtHeatRateRepository.findByAssetFkIdAndFinancialYearNative(assetId, prevYear);
                if (prevEntities == null) prevEntities = new java.util.ArrayList<>();
            }
          
            java.util.Map<Double, Double> prevYearHeatRateMap = new java.util.HashMap<>();
            for (CppGtHeatRate prevEntity : prevEntities) {
                if (prevEntity != null && prevEntity.getGtLoad() != null) {
                    prevYearHeatRateMap.put(prevEntity.getGtLoad(), prevEntity.getFinalHeatRate());
                }
            }

            java.util.Map<Double, Double> proposedHeatRateMap = new java.util.HashMap<>();
            
            String plantIdsStr = "";
            if (plantIds != null && !plantIds.isEmpty()) {
                plantIdsStr = plantIds.stream()
                        .map(UUID::toString)
                        .collect(java.util.stream.Collectors.joining(","));
            }
            
            // ISOLATED TRY-CATCH GUARD: Prevents database Stored Procedure errors from crashing the API execution
            try {
                List<Object[]> spResultList = cppGtHeatRateRepository.executeCalculateCommonGTHeatRateSP(
                        startDate, endDate, assetId, plantIdsStr
                );

                if (spResultList != null) {
                    for (Object[] row : spResultList) {
                        if (row != null && row.length > 3) {
                            Double loadVal = row[1] != null ? Double.valueOf(row[1].toString()) : null;
                            Double proposedHeatRate = row[3] != null ? Double.valueOf(row[3].toString()) : null;
                            
                            if (loadVal != null && proposedHeatRate != null) {
                                proposedHeatRateMap.put(loadVal, proposedHeatRate);
                            }
                        }
                    }
                }
            } catch (Exception dbEx) {
                // Safely catches custom database exceptions like "Asset not found for selected plants."
                logger.warn("[JMDHeatRate] GT Stored procedure execution failed or bypassed for AssetId: [{}] and Plant IDs: [{}]. Reason: {}. Proceeding with default values.",
                        assetId, plantIdsStr, dbEx.getMessage());
            }
            
            List<CppGtHeatRateDto> resultList = new java.util.ArrayList<>();
            for (CppGtHeatRate entity : entities) {
                if (entity == null) continue;
                
                CppGtHeatRateDto dto = new CppGtHeatRateDto();

                dto.setId(entity.getId() != null ? entity.getId().toString() : null);
                dto.setAssetFkId(entity.getAssetFkId() != null ? entity.getAssetFkId().toString() : null);
                dto.setEquipType(entity.getAssetName() != null ? entity.getAssetName() : null);
                dto.setCppUtility(entity.getUtilityId() != null ? entity.getUtilityId() : null);
                dto.setFinancialYear(entity.getFinancialYear() != null ? entity.getFinancialYear() : null);
                dto.setGtLoad(entity.getGtLoad() != null ? entity.getGtLoad() : null);
                dto.setFreeSteamFactor(entity.getFreeSteamFactor() != null ? entity.getFreeSteamFactor() : null);
                dto.setRemarks(entity.getRemarks() != null ? entity.getRemarks() : "");
                
                // Native Date mapping
                dto.setCreatedDate(entity.getCreatedDate());
                dto.setUpdatedDate(entity.getUpdatedDate());
                
                dto.setFinalHeatRate(entity.getFinalHeatRate() != null ? entity.getFinalHeatRate() : null);
                dto.setOemHeatRate(entity.getOemHeatRate() != null ? entity.getOemHeatRate() : null);
                dto.setSelectedHeatRate(entity.getSelectedHeatRate() != null ? entity.getSelectedHeatRate() : "");
                
                if (entity.getGtLoad() != null && prevYearHeatRateMap.containsKey(entity.getGtLoad())) {
                    dto.setPrevYearFinalHeatRate(prevYearHeatRateMap.get(entity.getGtLoad()));
                } else {
                    dto.setPrevYearFinalHeatRate(0.0); 
                }

                if (entity.getGtLoad() != null && proposedHeatRateMap.containsKey(entity.getGtLoad())) {
                    dto.setProposedYearFinalHeatRate(proposedHeatRateMap.get(entity.getGtLoad()));
                } else {
                    dto.setProposedYearFinalHeatRate(0.0);
                }

                resultList.add(dto);
            }

            logger.info("[JMDHeatRate] getGTHeatRateData - found {} heat rate records", resultList.size());
            
            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(resultList);
            
        } catch (Exception e) {
            logger.error("[JMDHeatRate] getGTHeatRateData error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to fetch GT heat rate data: " + e.getMessage());
            vm.setData(null);
        }
        
        return vm;
    }
    
    
    @Override
    public AOPMessageVM getHRSGHeatRateData(UUID assetId, String year, String startDate, String endDate, List<UUID> plantIds) {
        logger.info("[JMDHeatRate] getHRSGHeatRateData - assetId: {}, year: {}, startDate: {}, endDate: {}, plantIds: {}", 
                assetId, year, startDate, endDate, plantIds);
        
        AOPMessageVM vm = new AOPMessageVM();
        
        try {
            String prevYear = null;
            if (year != null && year.contains("-")) {
                String[] parts = year.split("-");
                int startYear = Integer.parseInt(parts[0]);
                int endYear = Integer.parseInt(parts[1]);
                prevYear = (startYear - 1) + "-" + (endYear - 1);
            }

            List<CppHrsgHeatRate> entities = cppHrsgHeatRateRepository.findByAssetFkIdAndFinancialYearNative(assetId, year);
            if (entities == null) {
                entities = new java.util.ArrayList<>();
            }
            
            List<CppHrsgHeatRate> prevEntities = new java.util.ArrayList<>();
            if (prevYear != null) {
                prevEntities = cppHrsgHeatRateRepository.findByAssetFkIdAndFinancialYearNative(assetId, prevYear);
                if (prevEntities == null) prevEntities = new java.util.ArrayList<>();
            }
          
            java.util.Map<Double, Double> prevYearHeatRateMap = new java.util.HashMap<>();
            for (CppHrsgHeatRate prevEntity : prevEntities) {
                if (prevEntity != null && prevEntity.getHrsgLoad() != null) {
                    prevYearHeatRateMap.put(prevEntity.getHrsgLoad(), prevEntity.getFinalHeatRate());
                }
            }

            java.util.Map<Double, Double> proposedHeatRateMap = new java.util.HashMap<>();
            
            String plantIdsStr = "";
            if (plantIds != null && !plantIds.isEmpty()) {
                plantIdsStr = plantIds.stream()
                        .map(UUID::toString)
                        .collect(java.util.stream.Collectors.joining(","));
            }
            
            // ISOLATED TRY-CATCH GUARD: Prevents custom Stored Procedure business rule violations from breaking execution flow
            try {
                List<Object[]> spResultList = cppHrsgHeatRateRepository.executeCalculateCommonHRSGHeatRateSP(
                        startDate, endDate, assetId, plantIdsStr
                );

                if (spResultList != null) {
                    for (Object[] row : spResultList) {
                        if (row != null && row.length > 3) {
                            Double loadVal = row[1] != null ? Double.valueOf(row[1].toString()) : null;
                            Double proposedHeatRate = row[3] != null ? Double.valueOf(row[3].toString()) : null;
                            
                            if (loadVal != null && proposedHeatRate != null) {
                                proposedHeatRateMap.put(loadVal, proposedHeatRate);
                            }
                        }
                    }
                }
            } catch (Exception dbEx) {
                // Catches 'InvalidDataAccessResourceUsageException' or any custom DB error like 'Asset not found for selected plants.'
                logger.warn("[JMDHeatRate] HRSG Stored procedure execution bypassed/failed for AssetId: [{}] and Plant IDs: [{}]. Exception Message: {}. Proceeding with default values.",
                        assetId, plantIdsStr, dbEx.getMessage());
            }
            
            List<CppHrsgHeatRateDto> resultList = new java.util.ArrayList<>();
            for (CppHrsgHeatRate entity : entities) {
                if (entity == null) continue;
                
                CppHrsgHeatRateDto dto = new CppHrsgHeatRateDto();

                dto.setId(entity.getId() != null ? entity.getId().toString() : null);
                dto.setAssetFkId(entity.getAssetFkId() != null ? entity.getAssetFkId().toString() : null);
                dto.setEquipType(entity.getAssetName() != null ? entity.getAssetName() : null);
                dto.setCppUtility(entity.getUtilityId() != null ? entity.getUtilityId() : null);
                dto.setFinancialYear(entity.getFinancialYear() != null ? entity.getFinancialYear() : null);
                dto.setHrsgLoad(entity.getHrsgLoad() != null ? entity.getHrsgLoad() : null);
                dto.setRemarks(entity.getRemarks() != null ? entity.getRemarks() : "");
                
                dto.setCreatedDate(entity.getCreatedDate());
                dto.setUpdatedDate(entity.getUpdatedDate());
                
                dto.setFinalHeatRate(entity.getFinalHeatRate() != null ? entity.getFinalHeatRate() : null);
                dto.setOemHeatRate(entity.getOemHeatRate() != null ? entity.getOemHeatRate() : null);
                dto.setSelectedHeatRate(entity.getSelectedHeatRate() != null ? entity.getSelectedHeatRate() : "");
                
                if (entity.getHrsgLoad() != null && prevYearHeatRateMap.containsKey(entity.getHrsgLoad())) {
                    dto.setPrevYearFinalHeatRate(prevYearHeatRateMap.get(entity.getHrsgLoad()));
                } else {
                    dto.setPrevYearFinalHeatRate(0.0); 
                }

                if (entity.getHrsgLoad() != null && proposedHeatRateMap.containsKey(entity.getHrsgLoad())) {
                    dto.setProposedYearFinalHeatRate(proposedHeatRateMap.get(entity.getHrsgLoad()));
                } else {
                    dto.setProposedYearFinalHeatRate(0.0);
                }

                resultList.add(dto);
            }

            logger.info("[JMDHeatRate] getHRSGHeatRateData - found {} heat rate records", resultList.size());
            
            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(resultList);
            
        } catch (Exception e) {
            logger.error("[JMDHeatRate] getHRSGHeatRateData error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to fetch HRSG heat rate data: " + e.getMessage());
            vm.setData(null);
        }
        
        return vm;
    }

    @Override
    public AOPMessageVM getAuxboilerHeatRateData(UUID assetId, String year, String startDate, String endDate, List<UUID> plantIds) {
        logger.info("[JMDHeatRate] getAuxBoilerHeatRateData - assetId: {}, year: {}, startDate: {}, endDate: {}, plantIds: {}", 
                assetId, year, startDate, endDate, plantIds);
        
        AOPMessageVM vm = new AOPMessageVM();
        
        try {
            String prevYear = null;
            if (year != null && year.contains("-")) {
                String[] parts = year.split("-");
                int startYear = Integer.parseInt(parts[0]);
                int endYear = Integer.parseInt(parts[1]);
                prevYear = (startYear - 1) + "-" + (endYear - 1);
            }

            List<CppAuxBoilerHeatRate> entities = cppAuxBoilerHeatRateRepository.findByAssetFkIdAndFinancialYearNative(assetId, year);
            if (entities == null) {
                entities = new java.util.ArrayList<>();
            }
            
            List<CppAuxBoilerHeatRate> prevEntities = new java.util.ArrayList<>();
            if (prevYear != null) {
                prevEntities = cppAuxBoilerHeatRateRepository.findByAssetFkIdAndFinancialYearNative(assetId, prevYear);
                if (prevEntities == null) prevEntities = new java.util.ArrayList<>();
            }
          
            java.util.Map<Double, Double> prevYearHeatRateMap = new java.util.HashMap<>();
            for (CppAuxBoilerHeatRate prevEntity : prevEntities) {
                if (prevEntity != null && prevEntity.getAuxBoilerLoad() != null) {
                    prevYearHeatRateMap.put(prevEntity.getAuxBoilerLoad(), prevEntity.getFinalHeatRate());
                }
            }

            java.util.Map<Double, Double> proposedHeatRateMap = new java.util.HashMap<>();
            
            String plantIdsStr = "";
            if (plantIds != null && !plantIds.isEmpty()) {
                plantIdsStr = plantIds.stream()
                        .map(UUID::toString)
                        .collect(java.util.stream.Collectors.joining(","));
            }
            
            // ISOLATED TRY-CATCH GUARD: Prevents custom Stored Procedure business rule violations from breaking execution flow
            try {
                List<Object[]> spResultList = cppAuxBoilerHeatRateRepository.executeCalculateCommonAUXBoilerHeatRateSP(
                        startDate, endDate, assetId, plantIdsStr
                );

                if (spResultList != null) {
                    for (Object[] row : spResultList) {
                        if (row != null && row.length > 3) {
                            Double loadVal = row[1] != null ? Double.valueOf(row[1].toString()) : null;
                            Double proposedHeatRate = row[3] != null ? Double.valueOf(row[3].toString()) : null;
                            
                            if (loadVal != null && proposedHeatRate != null) {
                                proposedHeatRateMap.put(loadVal, proposedHeatRate);
                            }
                        }
                    }
                }
            } catch (Exception dbEx) {
                // Catches 'InvalidDataAccessResourceUsageException' or any custom DB error like 'Asset not found for selected plants.'
                logger.warn("[JMDHeatRate] HRSG Stored procedure execution bypassed/failed for AssetId: [{}] and Plant IDs: [{}]. Exception Message: {}. Proceeding with default values.",
                        assetId, plantIdsStr, dbEx.getMessage());
            }
            
            List<CppAuxBoilerHeatRateDto> resultList = new java.util.ArrayList<>();
            for (CppAuxBoilerHeatRate entity : entities) {
                if (entity == null) continue;
                
                CppAuxBoilerHeatRateDto dto = new CppAuxBoilerHeatRateDto();

                dto.setId(entity.getId() != null ? entity.getId().toString() : null);
                dto.setAssetFkId(entity.getAssetFkId() != null ? entity.getAssetFkId().toString() : null);
                dto.setEquipType(entity.getAssetName() != null ? entity.getAssetName() : null);
                dto.setCppUtility(entity.getUtilityId() != null ? entity.getUtilityId() : null);
                dto.setFinancialYear(entity.getFinancialYear() != null ? entity.getFinancialYear() : null);
                dto.setAuxBoilerLoad(entity.getAuxBoilerLoad() != null ? entity.getAuxBoilerLoad() : null);
                dto.setRemarks(entity.getRemarks() != null ? entity.getRemarks() : "");    
                dto.setCreatedDate(entity.getCreatedDate());
                dto.setUpdatedDate(entity.getUpdatedDate()); 
                dto.setFinalHeatRate(entity.getFinalHeatRate() != null ? entity.getFinalHeatRate() : null);
                dto.setOemHeatRate(entity.getOemHeatRate() != null ? entity.getOemHeatRate() : null);
                dto.setSelectedHeatRate(entity.getSelectedHeatRate() != null ? entity.getSelectedHeatRate() : "");
                
                if (entity.getAuxBoilerLoad() != null && prevYearHeatRateMap.containsKey(entity.getAuxBoilerLoad())) {
                    dto.setPrevYearFinalHeatRate(prevYearHeatRateMap.get(entity.getAuxBoilerLoad()));
                } else {
                    dto.setPrevYearFinalHeatRate(0.0); 
                }

                if (entity.getAuxBoilerLoad() != null && proposedHeatRateMap.containsKey(entity.getAuxBoilerLoad())) {
                    dto.setProposedYearFinalHeatRate(proposedHeatRateMap.get(entity.getAuxBoilerLoad()));
                } else {
                    dto.setProposedYearFinalHeatRate(0.0);
                }

                resultList.add(dto);
            }

            logger.info("[JMDHeatRate] getAuxBoilerHeatRateData - found {} heat rate records", resultList.size());
            
            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(resultList);
            
        } catch (Exception e) {
            logger.error("[JMDHeatRate] getAuxBoilerHeatRateData error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to fetch HRSG heat rate data: " + e.getMessage());
            vm.setData(null);
        }
        
        return vm;
    }
    @Override
    public AOPMessageVM getCcppHeatRateData(UUID assetId, String year, String startDate, String endDate, List<UUID> plantIds) {
        logger.info("[JMDHeatRate] getAuxBoilerHeatRateData - assetId: {}, year: {}, startDate: {}, endDate: {}, plantIds: {}", 
                assetId, year, startDate, endDate, plantIds);
        
        AOPMessageVM vm = new AOPMessageVM();
        
        try {
            String prevYear = null;
            if (year != null && year.contains("-")) {
                String[] parts = year.split("-");
                int startYear = Integer.parseInt(parts[0]);
                int endYear = Integer.parseInt(parts[1]);
                prevYear = (startYear - 1) + "-" + (endYear - 1);
            }

            List<CppCCPPHeatRate> entities = cppCCPPHeatRateRepository.findByAssetFkIdAndFinancialYearNative(assetId, year);
            if (entities == null) {
                entities = new java.util.ArrayList<>();
            }
            
            List<CppCCPPHeatRate> prevEntities = new java.util.ArrayList<>();
            if (prevYear != null) {
                prevEntities = cppCCPPHeatRateRepository.findByAssetFkIdAndFinancialYearNative(assetId, prevYear);
                if (prevEntities == null) prevEntities = new java.util.ArrayList<>();
            }
          
            java.util.Map<Double, Double> prevYearHeatRateMap = new java.util.HashMap<>();
            for (CppCCPPHeatRate prevEntity : prevEntities) {
                if (prevEntity != null && prevEntity.getCcppLoad() != null) {
                    prevYearHeatRateMap.put(prevEntity.getCcppLoad(), prevEntity.getFinalHeatRate());
                }
            }

            java.util.Map<Double, Double> proposedHeatRateMap = new java.util.HashMap<>();
            
            String plantIdsStr = "";
            if (plantIds != null && !plantIds.isEmpty()) {
                plantIdsStr = plantIds.stream()
                        .map(UUID::toString)
                        .collect(java.util.stream.Collectors.joining(","));
            }
            
            // ISOLATED TRY-CATCH GUARD: Prevents custom Stored Procedure business rule violations from breaking execution flow
            try {
                List<Object[]> spResultList = cppCCPPHeatRateRepository.executeCalculateCommonCCPPHeatRateSP(
                        startDate, endDate, assetId, plantIdsStr
                );

                if (spResultList != null) {
                    for (Object[] row : spResultList) {
                        if (row != null && row.length > 3) {
                            Double loadVal = row[1] != null ? Double.valueOf(row[1].toString()) : null;
                            Double proposedHeatRate = row[3] != null ? Double.valueOf(row[3].toString()) : null;
                            
                            if (loadVal != null && proposedHeatRate != null) {
                                proposedHeatRateMap.put(loadVal, proposedHeatRate);
                            }
                        }
                    }
                }
            } catch (Exception dbEx) {
                // Catches 'InvalidDataAccessResourceUsageException' or any custom DB error like 'Asset not found for selected plants.'
                logger.warn("[JMDHeatRate] HRSG Stored procedure execution bypassed/failed for AssetId: [{}] and Plant IDs: [{}]. Exception Message: {}. Proceeding with default values.",
                        assetId, plantIdsStr, dbEx.getMessage());
            }
            
            List<CppCcppHeatRateDto> resultList = new java.util.ArrayList<>();
            for (CppCCPPHeatRate entity : entities) {
                if (entity == null) continue;
                
                CppCcppHeatRateDto dto = new CppCcppHeatRateDto();

                dto.setId(entity.getId() != null ? entity.getId().toString() : null);
                dto.setAssetFkId(entity.getAssetFkId() != null ? entity.getAssetFkId().toString() : null);
                dto.setEquipType(entity.getAssetName() != null ? entity.getAssetName() : null);
                dto.setCppUtility(entity.getUtilityId() != null ? entity.getUtilityId() : null);
                dto.setFinancialYear(entity.getFinancialYear() != null ? entity.getFinancialYear() : null);
                dto.setCcppLoad(entity.getCcppLoad() != null ? entity.getCcppLoad() : null);
                dto.setRemarks(entity.getRemarks() != null ? entity.getRemarks() : "");    
                dto.setCreatedDate(entity.getCreatedDate());
                dto.setUpdatedDate(entity.getUpdatedDate()); 
                dto.setFinalHeatRate(entity.getFinalHeatRate() != null ? entity.getFinalHeatRate() : null);
                dto.setOemHeatRate(entity.getOemHeatRate() != null ? entity.getOemHeatRate() : null);
                dto.setSelectedHeatRate(entity.getSelectedHeatRate() != null ? entity.getSelectedHeatRate() : "");
                
                if (entity.getCcppLoad() != null && prevYearHeatRateMap.containsKey(entity.getCcppLoad())) {
                    dto.setPrevYearFinalHeatRate(prevYearHeatRateMap.get(entity.getCcppLoad()));
                } else {
                    dto.setPrevYearFinalHeatRate(0.0); 
                }

                if (entity.getCcppLoad() != null && proposedHeatRateMap.containsKey(entity.getCcppLoad())) {
                    dto.setProposedYearFinalHeatRate(proposedHeatRateMap.get(entity.getCcppLoad()));
                } else {
                    dto.setProposedYearFinalHeatRate(0.0);
                }

                resultList.add(dto);
            }

            logger.info("[JMDHeatRate] getAuxBoilerHeatRateData - found {} heat rate records", resultList.size());
            
            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(resultList);
            
        } catch (Exception e) {
            logger.error("[JMDHeatRate] getAuxBoilerHeatRateData error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to fetch HRSG heat rate data: " + e.getMessage());
            vm.setData(null);
        }
        
        return vm;
    }

    @Override
    public AOPMessageVM importGTHeatRateData(String year, UUID assetId, String startDate, String endDate, List<UUID> plantIds, MultipartFile file) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            
            List<CppGtHeatRateDto> data = readGTHeatRateData(file.getInputStream(), year);
            
            if (data != null && !data.isEmpty()) {
                CppGtHeatRateDto firstRow = data.get(0);
                if (assetId == null && firstRow.getAssetFkId() != null) {
                    assetId = UUID.fromString(firstRow.getAssetFkId());
                }
            }

            aopMessageVM = saveGTHeatRateData(data, year);
            
            List<CppGtHeatRateDto> failedList = (List<CppGtHeatRateDto>) aopMessageVM.getData();

            if (failedList != null && !failedList.isEmpty()) {
                byte[] fileByteArray = exportGTHeatRateExcelData(
                    assetId,     
                    year,        
                    startDate,   
                    endDate,    
                    plantIds,    
                    true,        
                    failedList   
                );
                
                if (fileByteArray != null && fileByteArray.length > 0) {
                    String base64File = Base64.getEncoder().encodeToString(fileByteArray);
                    aopMessageVM.setData(base64File);
                    aopMessageVM.setCode(400);
                    aopMessageVM.setMessage("Partial data has been saved. Please check the attached error details.");
                }
            } else {
                aopMessageVM.setCode(200);
                aopMessageVM.setMessage("All data has been saved successfully.");
                aopMessageVM.setData(null);
            }

            return aopMessageVM;
            
        } catch (Exception e) {
            logger.error("[JMDHeatRate] Error importing GT heat rate data: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to process file import: " + e.getMessage());
            aopMessageVM.setData(null);
        }
        return aopMessageVM;
    }
    
    @Override
    public AOPMessageVM importSTGHeatRateData(String year, UUID assetId, String startDate, String endDate, List<UUID> plantIds, MultipartFile file) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            
            List<STGHeatRateDTO> data = readSTGHeatRateData(file.getInputStream(), year);
            
            if (data != null && !data.isEmpty()) {
            	STGHeatRateDTO firstRow = data.get(0);
                if (assetId == null && firstRow.getId() != null) {
                    assetId = UUID.fromString(firstRow.getId());
                }
            }

            aopMessageVM = updateSTGHeatRate(data, year);
            
            List<STGHeatRateDTO> failedList = (List<STGHeatRateDTO>) aopMessageVM.getData();

            if (failedList != null && !failedList.isEmpty()) {
                byte[] fileByteArray = exportSTGHeatRateExcelData(
                    assetId,     
                    year,        
                    startDate,   
                    endDate,    
                    plantIds,    
                    true,        
                    failedList   
                );
                
                if (fileByteArray != null && fileByteArray.length > 0) {
                    String base64File = Base64.getEncoder().encodeToString(fileByteArray);
                    aopMessageVM.setData(base64File);
                    aopMessageVM.setCode(400);
                    aopMessageVM.setMessage("Partial data has been saved. Please check the attached error details.");
                }
            } else {
                aopMessageVM.setCode(200);
                aopMessageVM.setMessage("All data has been saved successfully.");
                aopMessageVM.setData(null);
            }

            return aopMessageVM;
            
        } catch (Exception e) {
            logger.error("[JMDHeatRate] Error importing GT heat rate data: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to process file import: " + e.getMessage());
            aopMessageVM.setData(null);
        }
        return aopMessageVM;
    }
    
    @Override
    public AOPMessageVM importHRSGHeatRateData(String year, UUID assetId, String startDate, String endDate, List<UUID> plantIds, MultipartFile file) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            
            List<CppHrsgHeatRateDto> data = readHRSGHeatRateData(file.getInputStream(), year);
            
            if (data != null && !data.isEmpty()) {
            	CppHrsgHeatRateDto firstRow = data.get(0);
                if (assetId == null && firstRow.getAssetFkId() != null) {
                    assetId = UUID.fromString(firstRow.getAssetFkId());
                }
            }

            aopMessageVM = saveHRSGHeatRateData(data, year);
            
            List<CppHrsgHeatRateDto> failedList = (List<CppHrsgHeatRateDto>) aopMessageVM.getData();

            if (failedList != null && !failedList.isEmpty()) {
                byte[] fileByteArray = exportHRSGHeatRateExcelData(
                    assetId,     
                    year,        
                    startDate,   
                    endDate,    
                    plantIds,    
                    true,        
                    failedList   
                );
                
                if (fileByteArray != null && fileByteArray.length > 0) {
                    String base64File = Base64.getEncoder().encodeToString(fileByteArray);
                    aopMessageVM.setData(base64File);
                    aopMessageVM.setCode(400);
                    aopMessageVM.setMessage("Partial data has been saved. Please check the attached error details.");
                }
            } else {
                aopMessageVM.setCode(200);
                aopMessageVM.setMessage("All data has been saved successfully.");
                aopMessageVM.setData(null);
            }

            return aopMessageVM;
            
        } catch (Exception e) {
            logger.error("[JMDHeatRate] Error importing GT heat rate data: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to process file import: " + e.getMessage());
            aopMessageVM.setData(null);
        }
        return aopMessageVM;
    }

    @Override
    public AOPMessageVM importAuxboilerHeatRateData(String year, UUID assetId, String startDate, String endDate, List<UUID> plantIds, MultipartFile file) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            
            List<CppAuxBoilerHeatRateDto> data = readAuxboilerHeatRateData(file.getInputStream(), year);
            
            if (data != null && !data.isEmpty()) {
            	CppAuxBoilerHeatRateDto firstRow = data.get(0);
                if (assetId == null && firstRow.getAssetFkId() != null) {
                    assetId = UUID.fromString(firstRow.getAssetFkId());
                }
            }

            aopMessageVM = updateAuxboilerHeatRate(data, year);
            
            List<CppAuxBoilerHeatRateDto> failedList = (List<CppAuxBoilerHeatRateDto>) aopMessageVM.getData();

            if (failedList != null && !failedList.isEmpty()) {
                byte[] fileByteArray = exportAuxboilerHeatRateExcelData(
                    assetId,     
                    year,        
                    startDate,   
                    endDate,    
                    plantIds,    
                    true,        
                    failedList   
                );
                
                if (fileByteArray != null && fileByteArray.length > 0) {
                    String base64File = Base64.getEncoder().encodeToString(fileByteArray);
                    aopMessageVM.setData(base64File);
                    aopMessageVM.setCode(400);
                    aopMessageVM.setMessage("Partial data has been saved. Please check the attached error details.");
                }
            } else {
                aopMessageVM.setCode(200);
                aopMessageVM.setMessage("All data has been saved successfully.");
                aopMessageVM.setData(null);
            }

            return aopMessageVM;
            
        } catch (Exception e) {
            logger.error("[JMDHeatRate] Error importing Auxboiler heat rate data: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to process file import: " + e.getMessage());
            aopMessageVM.setData(null);
        }
        return aopMessageVM;
    }

    public List<CppGtHeatRateDto> readGTHeatRateData(InputStream inputStream, String year) {
	    List<CppGtHeatRateDto> cppGtHeatRateDtos = new ArrayList<>();

	    try (Workbook workbook = new XSSFWorkbook(inputStream)) {
	        Sheet sheet = workbook.getSheetAt(0);

	        Iterator<Row> rowIterator = sheet.iterator();

	        if (rowIterator.hasNext())
	            rowIterator.next();  

	        while (rowIterator.hasNext()) {
	            Row row = rowIterator.next();
	            
	            CppGtHeatRateDto dto = new CppGtHeatRateDto();
	            try {
	                dto.setEquipType(getStringCellValue(row.getCell(0), dto));
	                dto.setCppUtility(getStringCellValue(row.getCell(1), dto));
	                dto.setGtLoad(getNumericCellValue(row.getCell(2), dto));
	                dto.setOemHeatRate(getNumericCellValue(row.getCell(3), dto));
	                dto.setPrevYearFinalHeatRate(getNumericCellValue(row.getCell(4), dto));
	                dto.setProposedYearFinalHeatRate(getNumericCellValue(row.getCell(5), dto));
	                dto.setFinalHeatRate(getNumericCellValue(row.getCell(6), dto));
	                dto.setFreeSteamFactor(getNumericCellValue(row.getCell(7), dto));
	                dto.setRemarks(getStringCellValue(row.getCell(8), dto));
	                dto.setFinancialYear(year);
	                dto.setSelectedHeatRate(getStringCellValue(row.getCell(9), dto));
	                dto.setId(getStringCellValue(row.getCell(10), dto));
	              } 
	              catch (Exception e) {
	                e.printStackTrace();
	                dto.setErrDescription(e.getMessage());
	                dto.setSaveStatus("Failed");
	            }
	            cppGtHeatRateDtos.add(dto);
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return cppGtHeatRateDtos;
	}

    public List<CppHrsgHeatRateDto> readHRSGHeatRateData(InputStream inputStream, String year) {
	    List<CppHrsgHeatRateDto> cppHrsgHeatRateDtos = new ArrayList<>();

	    try (Workbook workbook = new XSSFWorkbook(inputStream)) {
	        Sheet sheet = workbook.getSheetAt(0);

	        Iterator<Row> rowIterator = sheet.iterator();

	        if (rowIterator.hasNext())
	            rowIterator.next();  

	        while (rowIterator.hasNext()) {
	            Row row = rowIterator.next();
	            
	            CppHrsgHeatRateDto dto = new CppHrsgHeatRateDto();
	            try {
	                dto.setEquipType(getStringCellValue(row.getCell(0), dto));
	                dto.setCppUtility(getStringCellValue(row.getCell(1), dto));
	                dto.setHrsgLoad(getNumericCellValue(row.getCell(2), dto));
	                dto.setOemHeatRate(getNumericCellValue(row.getCell(3), dto));
	                dto.setPrevYearFinalHeatRate(getNumericCellValue(row.getCell(4), dto));
	                dto.setProposedYearFinalHeatRate(getNumericCellValue(row.getCell(5), dto));
	                dto.setFinalHeatRate(getNumericCellValue(row.getCell(6), dto));
	                dto.setRemarks(getStringCellValue(row.getCell(7), dto));
	                dto.setFinancialYear(year);
	                dto.setSelectedHeatRate(getStringCellValue(row.getCell(8), dto));
	                dto.setId(getStringCellValue(row.getCell(9), dto));
	              } 
	              catch (Exception e) {
	                e.printStackTrace();
	                dto.setErrDescription(e.getMessage());
	                dto.setSaveStatus("Failed");
	            }
	            cppHrsgHeatRateDtos.add(dto);
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return cppHrsgHeatRateDtos;
	}

    public List<CppAuxBoilerHeatRateDto> readAuxboilerHeatRateData(InputStream inputStream, String year) {
	    List<CppAuxBoilerHeatRateDto> cppAuxBoilerHeatRateDtos = new ArrayList<>();

	    try (Workbook workbook = new XSSFWorkbook(inputStream)) {
	        Sheet sheet = workbook.getSheetAt(0);

	        Iterator<Row> rowIterator = sheet.iterator();

	        if (rowIterator.hasNext())
	            rowIterator.next();  

	        while (rowIterator.hasNext()) {
	            Row row = rowIterator.next();
	            
	            CppAuxBoilerHeatRateDto dto = new CppAuxBoilerHeatRateDto();
	            try {
	                dto.setEquipType(getStringCellValue(row.getCell(0), dto));
	                dto.setCppUtility(getStringCellValue(row.getCell(1), dto));
	                dto.setAuxBoilerLoad(getNumericCellValue(row.getCell(2), dto));
	                dto.setOemHeatRate(getNumericCellValue(row.getCell(3), dto));
	                dto.setPrevYearFinalHeatRate(getNumericCellValue(row.getCell(4), dto));
	                dto.setProposedYearFinalHeatRate(getNumericCellValue(row.getCell(5), dto));
	                dto.setFinalHeatRate(getNumericCellValue(row.getCell(6), dto));
	                dto.setRemarks(getStringCellValue(row.getCell(7), dto));
	                dto.setFinancialYear(year);
	                dto.setSelectedHeatRate(getStringCellValue(row.getCell(8), dto));
	                dto.setId(getStringCellValue(row.getCell(9), dto));
	              } 
	              catch (Exception e) {
	                e.printStackTrace();
	                dto.setErrDescription(e.getMessage());
	                dto.setSaveStatus("Failed");
	            }
	            cppAuxBoilerHeatRateDtos.add(dto);
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return cppAuxBoilerHeatRateDtos;
	}

    public List<STGHeatRateDTO> readSTGHeatRateData(InputStream inputStream, String year) {
	    List<STGHeatRateDTO> stgHeatRateDTOs = new ArrayList<>();

	    try (Workbook workbook = new XSSFWorkbook(inputStream)) {
	        Sheet sheet = workbook.getSheetAt(0);

	        Iterator<Row> rowIterator = sheet.iterator();

	        if (rowIterator.hasNext())
	            rowIterator.next();  

	        while (rowIterator.hasNext()) {
	            Row row = rowIterator.next();
	            
	            STGHeatRateDTO dto = new STGHeatRateDTO();
	            try {
	                dto.setEquipType(getStringCellValue(row.getCell(0), dto));
	                dto.setCppUtility(getStringCellValue(row.getCell(1), dto));
	                dto.setStgLoad(getNumericCellValue(row.getCell(2), dto));
	                dto.setOemHeatRate(getNumericCellValue(row.getCell(3), dto));
	                dto.setPreviousYearHeatRate(getNumericCellValue(row.getCell(4), dto));
	                dto.setProposedHeatRate(getNumericCellValue(row.getCell(5), dto));
	                dto.setFinalHeatRate(getNumericCellValue(row.getCell(6), dto));
	                dto.setRemarks(getStringCellValue(row.getCell(7), dto));
	                dto.setFinancialYear(year);
	                dto.setSelectedHeatRate(getStringCellValue(row.getCell(8), dto));
	                dto.setId(getStringCellValue(row.getCell(9), dto));
	              } 
	              catch (Exception e) {
	                e.printStackTrace();
	                dto.setErrDescription(e.getMessage());
	                dto.setSaveStatus("Failed");
	            }
	            stgHeatRateDTOs.add(dto);
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return stgHeatRateDTOs;
	}

    private static Double getNumericCellValue(Cell cell, CppGtHeatRateDto dto) {
	    if (cell == null || cell.getCellType() == CellType.BLANK) {
	        return null;
	    }

	    if (cell.getCellType() == CellType.NUMERIC) {
	        return cell.getNumericCellValue();
	    } 
	    
	    if (cell.getCellType() == CellType.STRING) {
	        String val = cell.getStringCellValue().trim();
	        if (val.isEmpty()) {
	            return null; // Return null for blank strings
	        }
	        try {
	            return Double.parseDouble(val);
	        } catch (NumberFormatException e) {
	            dto.setSaveStatus("Failed");
	            dto.setErrDescription("Please enter numeric values");
	        }
	    }
	    return null;
	}

    
    private static String getStringCellValue(Cell cell, CppHrsgHeatRateDto dto) {
	    try {
	        if (cell == null || cell.getCellType() == CellType.BLANK) {
	            return null;
	        }
	        
	        cell.setCellType(CellType.STRING);
	        String val = cell.getStringCellValue().trim();
	        
	        // Return null if the string is empty after trimming
	        return val.isEmpty() ? null : val;
	        
	    } catch (Exception e) {
	        dto.setSaveStatus("Failed");
	        dto.setErrDescription("Please enter correct values");
	        e.printStackTrace();
	    }
	    return null;
	}

    private static Double getNumericCellValue(Cell cell, CppHrsgHeatRateDto dto) {
	    if (cell == null || cell.getCellType() == CellType.BLANK) {
	        return null;
	    }

	    if (cell.getCellType() == CellType.NUMERIC) {
	        return cell.getNumericCellValue();
	    } 
	    
	    if (cell.getCellType() == CellType.STRING) {
	        String val = cell.getStringCellValue().trim();
	        if (val.isEmpty()) {
	            return null; // Return null for blank strings
	        }
	        try {
	            return Double.parseDouble(val);
	        } catch (NumberFormatException e) {
	            dto.setSaveStatus("Failed");
	            dto.setErrDescription("Please enter numeric values");
	        }
	    }
	    return null;
	}

    private static String getStringCellValue(Cell cell, CppAuxBoilerHeatRateDto dto) {
	    try {
	        if (cell == null || cell.getCellType() == CellType.BLANK) {
	            return null;
	        }
	        
	        cell.setCellType(CellType.STRING);
	        String val = cell.getStringCellValue().trim();
	        
	        // Return null if the string is empty after trimming
	        return val.isEmpty() ? null : val;
	        
	    } catch (Exception e) {
	        dto.setSaveStatus("Failed");
	        dto.setErrDescription("Please enter correct values");
	        e.printStackTrace();
	    }
	    return null;
	}

    private static Double getNumericCellValue(Cell cell, CppAuxBoilerHeatRateDto dto) {
	    if (cell == null || cell.getCellType() == CellType.BLANK) {
	        return null;
	    }

	    if (cell.getCellType() == CellType.NUMERIC) {
	        return cell.getNumericCellValue();
	    } 
	    
	    if (cell.getCellType() == CellType.STRING) {
	        String val = cell.getStringCellValue().trim();
	        if (val.isEmpty()) {
	            return null; // Return null for blank strings
	        }
	        try {
	            return Double.parseDouble(val);
	        } catch (NumberFormatException e) {
	            dto.setSaveStatus("Failed");
	            dto.setErrDescription("Please enter numeric values");
	        }
	    }
	    return null;
	}

    private static String getStringCellValue(Cell cell, CppCcppHeatRateDto dto) {
	    try {
	        if (cell == null || cell.getCellType() == CellType.BLANK) {
	            return null;
	        }
	        
	        cell.setCellType(CellType.STRING);
	        String val = cell.getStringCellValue().trim();
	        
	        return val.isEmpty() ? null : val;
	        
	    } catch (Exception e) {
	        dto.setSaveStatus("Failed");
	        dto.setErrDescription("Please enter correct values");
	        e.printStackTrace();
	    }
	    return null;
	}

    private static Double getNumericCellValue(Cell cell, CppCcppHeatRateDto dto) {
	    if (cell == null || cell.getCellType() == CellType.BLANK) {
	        return null;
	    }

	    if (cell.getCellType() == CellType.NUMERIC) {
	        return cell.getNumericCellValue();
	    } 
	    
	    if (cell.getCellType() == CellType.STRING) {
	        String val = cell.getStringCellValue().trim();
	        if (val.isEmpty()) {
	            return null;
	        }
	        try {
	            return Double.parseDouble(val);
	        } catch (NumberFormatException e) {
	            dto.setSaveStatus("Failed");
	            dto.setErrDescription("Please enter numeric values");
	        }
	    }
	    return null;
	}

    private static String getStringCellValue(Cell cell, CppGtHeatRateDto dto) {
	    try {
	        if (cell == null || cell.getCellType() == CellType.BLANK) {
	            return null;
	        }
	        
	        cell.setCellType(CellType.STRING);
	        String val = cell.getStringCellValue().trim();
	        
	        // Return null if the string is empty after trimming
	        return val.isEmpty() ? null : val;
	        
	    } catch (Exception e) {
	        dto.setSaveStatus("Failed");
	        dto.setErrDescription("Please enter correct values");
	        e.printStackTrace();
	    }
	    return null;
	}

    private static Double getNumericCellValue(Cell cell, STGHeatRateDTO dto) {
	    if (cell == null || cell.getCellType() == CellType.BLANK) {
	        return null;
	    }

	    if (cell.getCellType() == CellType.NUMERIC) {
	        return cell.getNumericCellValue();
	    } 
	    
	    if (cell.getCellType() == CellType.STRING) {
	        String val = cell.getStringCellValue().trim();
	        if (val.isEmpty()) {
	            return null; // Return null for blank strings
	        }
	        try {
	            return Double.parseDouble(val);
	        } catch (NumberFormatException e) {
	            dto.setSaveStatus("Failed");
	            dto.setErrDescription("Please enter numeric values");
	        }
	    }
	    return null;
	}
    
    private static String getStringCellValue(Cell cell, STGHeatRateDTO dto) {
	    try {
	        if (cell == null || cell.getCellType() == CellType.BLANK) {
	            return null;
	        }
	        
	        cell.setCellType(CellType.STRING);
	        String val = cell.getStringCellValue().trim();
	        
	        // Return null if the string is empty after trimming
	        return val.isEmpty() ? null : val;
	        
	    } catch (Exception e) {
	        dto.setSaveStatus("Failed");
	        dto.setErrDescription("Please enter correct values");
	        e.printStackTrace();
	    }
	    return null;
	}

    @Override
    public AOPMessageVM saveGTHeatRateData(List<CppGtHeatRateDto> dtoList, String year) {
        logger.info("[JMDHeatRate] saveGTHeatRateData - processing {} records for year: {}", 
                dtoList != null ? dtoList.size() : 0, year);
        
        AOPMessageVM vm = new AOPMessageVM();
        
        try {
            if (dtoList == null || dtoList.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("Request body cannot be empty");
                return vm;
            }

            List<CppGtHeatRateDto> failedList = new java.util.ArrayList<>();
            List<CppGtHeatRate> entitiesToSave = new java.util.ArrayList<>();

            for (CppGtHeatRateDto dto : dtoList) {
                if (dto == null) continue;

               
                if (dto.getSaveStatus() != null && dto.getSaveStatus().equalsIgnoreCase("Failed")) {
                    failedList.add(dto);
                    continue;
                }
                
                
                CppGtHeatRate entity = null;
                if (dto.getId() != null && !dto.getId().trim().isEmpty()) {
                    try {
                        entity = cppGtHeatRateRepository.findById(UUID.fromString(dto.getId())).orElse(null);
                    } catch (IllegalArgumentException e) {
                        logger.warn("[JMDHeatRate] Invalid UUID format provided: {}", dto.getId());
                    }
                }
                
                if (entity == null) {
                    logger.warn("[JMDHeatRate] Record with ID {} not found. Skipping update.", dto.getId());
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription("Record ID not found in database");
                    failedList.add(dto);
                    continue; 
                }

                
                boolean isValueChanged = false;

                if (isDoubleChanged(entity.getGtLoad(), dto.getGtLoad())) isValueChanged = true;
                if (isDoubleChanged(entity.getOemHeatRate(), dto.getOemHeatRate())) isValueChanged = true;
                if (isDoubleChanged(entity.getFinalHeatRate(), dto.getFinalHeatRate())) isValueChanged = true;
                if (isDoubleChanged(entity.getFreeSteamFactor(), dto.getFreeSteamFactor())) isValueChanged = true;
                
                String existingSelectedHR = entity.getSelectedHeatRate() != null ? entity.getSelectedHeatRate().trim() : "";
                String incomingSelectedHR = dto.getSelectedHeatRate() != null ? dto.getSelectedHeatRate().trim() : "";
                if (!existingSelectedHR.equalsIgnoreCase(incomingSelectedHR)) {
                    isValueChanged = true;
                }

                
                String incomingRemarks = dto.getRemarks() != null ? dto.getRemarks().trim() : "";

                if (isValueChanged) {
                    if (incomingRemarks.isEmpty()) {
                        
                        dto.setSaveStatus("Failed");
                        dto.setErrDescription("Remarks are mandatory when data values are updated");
                        failedList.add(dto);
                        continue;
                    }
                    
                    String existingRemarks = entity.getRemarks() != null ? entity.getRemarks().trim() : "";
                    if (incomingRemarks.equalsIgnoreCase(existingRemarks)) {
                        
                        dto.setSaveStatus("Failed");
                        dto.setErrDescription("Remarks must be updated because data values changed");
                        failedList.add(dto);
                        continue;
                    }
                }

                
                entity.setGtLoad(dto.getGtLoad());
                entity.setOemHeatRate(dto.getOemHeatRate());
                entity.setFinalHeatRate(dto.getFinalHeatRate());
                entity.setFreeSteamFactor(dto.getFreeSteamFactor());
                entity.setSelectedHeatRate(dto.getSelectedHeatRate());
                entity.setRemarks(dto.getRemarks()); 
                
                entitiesToSave.add(entity);
            }
         
            
            if (!entitiesToSave.isEmpty()) {
                List<CppGtHeatRate> savedEntities = cppGtHeatRateRepository.saveAll(entitiesToSave);
                logger.info("[JMDHeatRate] saveGTHeatRateData - successfully updated {} records", savedEntities.size());
                
                if (!failedList.isEmpty()) {
                    vm.setCode(400);
                    vm.setMessage("Partial data saved with validation exceptions");
                } else {
                    vm.setCode(200);
                    vm.setMessage("Data updated successfully");
                }
                vm.setData(failedList);
            } else {
                vm.setCode(400);
                vm.setMessage("No valid records met conditions to update");
                vm.setData(failedList);
            }
            
        } catch (Exception e) {
            logger.error("[JMDHeatRate] saveGTHeatRateData error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to update GT heat rate data: " + e.getMessage());
            vm.setData(null);
        }
        
        return vm;
    }

    @Override
    public AOPMessageVM saveHRSGHeatRateData(List<CppHrsgHeatRateDto> dtoList, String year) {
        logger.info("[JMDHeatRate] saveGTHeatRateData - processing {} records for year: {}", 
                dtoList != null ? dtoList.size() : 0, year);
        
        AOPMessageVM vm = new AOPMessageVM();
        
        try {
            if (dtoList == null || dtoList.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("Request body cannot be empty");
                return vm;
            }

            List<CppHrsgHeatRateDto> failedList = new java.util.ArrayList<>();
            List<CppHrsgHeatRate> entitiesToSave = new java.util.ArrayList<>();

            for (CppHrsgHeatRateDto dto : dtoList) {
                if (dto == null) continue;

               
                if (dto.getSaveStatus() != null && dto.getSaveStatus().equalsIgnoreCase("Failed")) {
                    failedList.add(dto);
                    continue;
                }
                
                
                CppHrsgHeatRate entity = null;
                if (dto.getId() != null && !dto.getId().trim().isEmpty()) {
                    try {
                        entity = cppHrsgHeatRateRepository.findById(UUID.fromString(dto.getId())).orElse(null);
                    } catch (IllegalArgumentException e) {
                        logger.warn("[JMDHeatRate] Invalid UUID format provided: {}", dto.getId());
                    }
                }
                
                if (entity == null) {
                    logger.warn("[JMDHeatRate] Record with ID {} not found. Skipping update.", dto.getId());
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription("Record ID not found in database");
                    failedList.add(dto);
                    continue; 
                }

                
                boolean isValueChanged = false;

                if (isDoubleChanged(entity.getHrsgLoad(), dto.getHrsgLoad())) isValueChanged = true;
                if (isDoubleChanged(entity.getOemHeatRate(), dto.getOemHeatRate())) isValueChanged = true;
                if (isDoubleChanged(entity.getFinalHeatRate(), dto.getFinalHeatRate())) isValueChanged = true;
               
                
                String existingSelectedHR = entity.getSelectedHeatRate() != null ? entity.getSelectedHeatRate().trim() : "";
                String incomingSelectedHR = dto.getSelectedHeatRate() != null ? dto.getSelectedHeatRate().trim() : "";
                if (!existingSelectedHR.equalsIgnoreCase(incomingSelectedHR)) {
                    isValueChanged = true;
                }

                
                String incomingRemarks = dto.getRemarks() != null ? dto.getRemarks().trim() : "";

                if (isValueChanged) {
                    if (incomingRemarks.isEmpty()) {
                        
                        dto.setSaveStatus("Failed");
                        dto.setErrDescription("Remarks are mandatory when data values are updated");
                        failedList.add(dto);
                        continue;
                    }
                    
                    String existingRemarks = entity.getRemarks() != null ? entity.getRemarks().trim() : "";
                    if (incomingRemarks.equalsIgnoreCase(existingRemarks)) {
                        
                        dto.setSaveStatus("Failed");
                        dto.setErrDescription("Remarks must be updated because data values changed");
                        failedList.add(dto);
                        continue;
                    }
                }

                
                entity.setHrsgLoad(dto.getHrsgLoad());
                entity.setOemHeatRate(dto.getOemHeatRate());
                entity.setFinalHeatRate(dto.getFinalHeatRate());
               
                entity.setSelectedHeatRate(dto.getSelectedHeatRate());
                entity.setRemarks(dto.getRemarks()); 
                
                entitiesToSave.add(entity);
            }
         
            
            if (!entitiesToSave.isEmpty()) {
                List<CppHrsgHeatRate> savedEntities = cppHrsgHeatRateRepository.saveAll(entitiesToSave);
                logger.info("[JMDHeatRate] saveGTHeatRateData - successfully updated {} records", savedEntities.size());
                
                if (!failedList.isEmpty()) {
                    vm.setCode(400);
                    vm.setMessage("Partial data saved with validation exceptions");
                } else {
                    vm.setCode(200);
                    vm.setMessage("Data updated successfully");
                }
                vm.setData(failedList);
            } else {
                vm.setCode(400);
                vm.setMessage("No valid records met conditions to update");
                vm.setData(failedList);
            }
            
        } catch (Exception e) {
            logger.error("[JMDHeatRate] saveGTHeatRateData error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to update GT heat rate data: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }

    @Override
    public AOPMessageVM updateAuxboilerHeatRate(List<CppAuxBoilerHeatRateDto> dtoList, String year) {
        logger.info("[JMDHeatRate] saveAuxboilerHeatRateData - processing {} records for year: {}", 
                dtoList != null ? dtoList.size() : 0, year);
        
        AOPMessageVM vm = new AOPMessageVM();
        
        try {
            if (dtoList == null || dtoList.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("Request body cannot be empty");
                return vm;
            }

            List<CppAuxBoilerHeatRateDto> failedList = new java.util.ArrayList<>();
            List<CppAuxBoilerHeatRate> entitiesToSave = new java.util.ArrayList<>();

            for (CppAuxBoilerHeatRateDto dto : dtoList) {
                if (dto == null) continue;
                if (dto.getSaveStatus() != null && dto.getSaveStatus().equalsIgnoreCase("Failed")) {
                    failedList.add(dto);
                    continue;
                }
                CppAuxBoilerHeatRate entity = null;
                if (dto.getId() != null && !dto.getId().trim().isEmpty()) {
                    try {
                        entity = cppAuxBoilerHeatRateRepository.findById(UUID.fromString(dto.getId())).orElse(null);
                    } catch (IllegalArgumentException e) {
                        logger.warn("[JMDHeatRate] Invalid UUID format provided: {}", dto.getId());
                    }
                }
                
                if (entity == null) {
                    logger.warn("[JMDHeatRate] Record with ID {} not found. Skipping update.", dto.getId());
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription("Record ID not found in database");
                    failedList.add(dto);
                    continue; 
                }

                
                boolean isValueChanged = false;

                if (isDoubleChanged(entity.getAuxBoilerLoad(), dto.getAuxBoilerLoad())) isValueChanged = true;
                if (isDoubleChanged(entity.getOemHeatRate(), dto.getOemHeatRate())) isValueChanged = true;
                if (isDoubleChanged(entity.getFinalHeatRate(), dto.getFinalHeatRate())) isValueChanged = true;
               
                
                String existingSelectedHR = entity.getSelectedHeatRate() != null ? entity.getSelectedHeatRate().trim() : "";
                String incomingSelectedHR = dto.getSelectedHeatRate() != null ? dto.getSelectedHeatRate().trim() : "";
                if (!existingSelectedHR.equalsIgnoreCase(incomingSelectedHR)) {
                    isValueChanged = true;
                }

                
                String incomingRemarks = dto.getRemarks() != null ? dto.getRemarks().trim() : "";

                if (isValueChanged) {
                    if (incomingRemarks.isEmpty()) {
                        
                        dto.setSaveStatus("Failed");
                        dto.setErrDescription("Remarks are mandatory when data values are updated");
                        failedList.add(dto);
                        continue;
                    }
                    
                    String existingRemarks = entity.getRemarks() != null ? entity.getRemarks().trim() : "";
                    if (incomingRemarks.equalsIgnoreCase(existingRemarks)) {
                        
                        dto.setSaveStatus("Failed");
                        dto.setErrDescription("Remarks must be updated because data values changed");
                        failedList.add(dto);
                        continue;
                    }
                }

                
                entity.setAuxBoilerLoad(dto.getAuxBoilerLoad());
                entity.setOemHeatRate(dto.getOemHeatRate());
                entity.setFinalHeatRate(dto.getFinalHeatRate());
               
                entity.setSelectedHeatRate(dto.getSelectedHeatRate());
                entity.setRemarks(dto.getRemarks()); 
                
                entitiesToSave.add(entity);
            }
         
            
            if (!entitiesToSave.isEmpty()) {
                List<CppAuxBoilerHeatRate> savedEntities = cppAuxBoilerHeatRateRepository.saveAll(entitiesToSave);
                logger.info("[JMDHeatRate] saveAuxBoilerHeatRateData - successfully updated {} records", savedEntities.size());
                
                if (!failedList.isEmpty()) {
                    vm.setCode(400);
                    vm.setMessage("Partial data saved with validation exceptions");
                } else {
                    vm.setCode(200);
                    vm.setMessage("Data updated successfully");
                }
                vm.setData(failedList);
            } else {
                vm.setCode(400);
                vm.setMessage("No valid records met conditions to update");
                vm.setData(failedList);
            }
            
        } catch (Exception e) {
            logger.error("[JMDHeatRate] saveGTHeatRateData error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to update AuxBoiler heat rate data: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }
    
    private boolean isDoubleChanged(Double dbValue, Double incomingValue) {
        double dbPrimitive = (dbValue == null) ? 0.0 : dbValue;
        double incomingPrimitive = (incomingValue == null) ? 0.0 : incomingValue;
        
        return Double.compare(dbPrimitive, incomingPrimitive) != 0;
    }
    
    @Override
    public byte[] exportGTHeatRateExcelData(UUID assetId, String year, String startDate, String endDate, 
                                            List<UUID> plantIds, boolean isAfterSave, List<CppGtHeatRateDto> dtoList) {
        try {
            if (!isAfterSave) {
                AOPMessageVM aopMessageVM = getGTHeatRateData(assetId, year, startDate, endDate, plantIds);
                if (aopMessageVM != null && aopMessageVM.getData() != null) {
                    dtoList = (List<CppGtHeatRateDto>) aopMessageVM.getData();
                }
            }
            if (dtoList == null) {
                dtoList = new java.util.ArrayList<>();
            }
            try (Workbook workbook = new XSSFWorkbook(); 
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                 
                Sheet sheet = workbook.createSheet("Heat Rate");
                 CellStyle headerStyle = createHeaderStyle(workbook);
                CellStyle dataStyle = createDataStyle(workbook);
                CellStyle remarksStyle = createRemarksStyle(workbook);
                List<String> headerList = new java.util.ArrayList<>(java.util.Arrays.asList(
                    "Equipment Type", "CPP Utility", "GT Load", "OEM HR", 
                    "PREVIOUS YEAR BUDGET HR", "PROPOSED HR (Based On Actual Data)", 
                    "Final HR", "Free Steam Factor", "Remark", "Selected Heat Rate", "Id"
                ));

                if (isAfterSave) {
                    headerList.add("Status");
                    headerList.add("Error Description");
                }

                int rowNum = 0;
                Row headerRow = sheet.createRow(rowNum++);
                for (int i = 0; i < headerList.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headerList.get(i));
                    cell.setCellStyle(headerStyle);
                }
                sheet.setColumnHidden(9, true);
                sheet.setColumnHidden(10, true);
                for (CppGtHeatRateDto dto : dtoList) {
                    Row row = sheet.createRow(rowNum++);
                    int colNum = 0;

                    row.createCell(colNum++).setCellValue(dto.getEquipType() != null ? dto.getEquipType() : "");
                    row.createCell(colNum++).setCellValue(dto.getCppUtility() != null ? dto.getCppUtility() : "");
                    row.createCell(colNum++).setCellValue(dto.getGtLoad() != null ? dto.getGtLoad() : 0.0);
                    row.createCell(colNum++).setCellValue(dto.getOemHeatRate() != null ? dto.getOemHeatRate() : 0.0);
                    row.createCell(colNum++).setCellValue(dto.getPrevYearFinalHeatRate() != null ? dto.getPrevYearFinalHeatRate() : 0.0);
                    row.createCell(colNum++).setCellValue(dto.getProposedYearFinalHeatRate() != null ? dto.getProposedYearFinalHeatRate() : 0.0);
                    row.createCell(colNum++).setCellValue(dto.getFinalHeatRate() != null ? dto.getFinalHeatRate() : 0.0);
                    row.createCell(colNum++).setCellValue(dto.getFreeSteamFactor() != null ? dto.getFreeSteamFactor() : 0.0);
                    
                    Cell remarkCell = row.createCell(colNum++);
                    remarkCell.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
                    remarkCell.setCellStyle(remarksStyle);

                    row.createCell(colNum++).setCellValue(dto.getSelectedHeatRate() != null ? dto.getSelectedHeatRate() : "");
                    row.createCell(colNum++).setCellValue(dto.getId() != null ? dto.getId() : "");

                    if (isAfterSave) {
                        row.createCell(colNum++).setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
                        row.createCell(colNum++).setCellValue(dto.getErrDescription() != null ? dto.getErrDescription() : "");
                    }

                    for (int c = 0; c < colNum; c++) {
                        if (c != 8) { 
                            row.getCell(c).setCellStyle(dataStyle);
                        }
                    }
                }

                
                for (int i = 0; i < headerList.size(); i++) {
                    if (i == 8) {
                        sheet.setColumnWidth(i, 8000); 
                        continue;
                    }
                    sheet.autoSizeColumn(i);
                    applyHeaderMinWidth(sheet, i, headerList.get(i));
                }

                workbook.write(outputStream);
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            logger.error("[JMDHeatRate] exportGTHeatRateExcelData error: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public byte[] exportHRSGHeatRateExcelData(UUID assetId, String year, String startDate, String endDate, 
                                            List<UUID> plantIds, boolean isAfterSave, List<CppHrsgHeatRateDto> dtoList) {
        try {
            if (!isAfterSave) {
                AOPMessageVM aopMessageVM = getHRSGHeatRateData(assetId, year, startDate, endDate, plantIds);
                if (aopMessageVM != null && aopMessageVM.getData() != null) {
                    dtoList = (List<CppHrsgHeatRateDto>) aopMessageVM.getData();
                }
            }
            if (dtoList == null) {
                dtoList = new java.util.ArrayList<>();
            }
            try (Workbook workbook = new XSSFWorkbook(); 
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                 
                Sheet sheet = workbook.createSheet("Heat Rate");
                 CellStyle headerStyle = createHeaderStyle(workbook);
                CellStyle dataStyle = createDataStyle(workbook);
                CellStyle remarksStyle = createRemarksStyle(workbook);
                List<String> headerList = new java.util.ArrayList<>(java.util.Arrays.asList(
                    "Equipment Type", "CPP Utility", "HRSG Load", "OEM HR", 
                    "PREVIOUS YEAR BUDGET HR", "PROPOSED HR (Based On Actual Data)", 
                    "Final HR", "Remark", "Selected Heat Rate", "Id"
                ));

                if (isAfterSave) {
                    headerList.add("Status");
                    headerList.add("Error Description");
                }

                int rowNum = 0;
                Row headerRow = sheet.createRow(rowNum++);
                for (int i = 0; i < headerList.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headerList.get(i));
                    cell.setCellStyle(headerStyle);
                }
                sheet.setColumnHidden(8, true);
                sheet.setColumnHidden(9, true);
                for (CppHrsgHeatRateDto dto : dtoList) {
                    Row row = sheet.createRow(rowNum++);
                    int colNum = 0;

                    row.createCell(colNum++).setCellValue(dto.getEquipType() != null ? dto.getEquipType() : "");
                    row.createCell(colNum++).setCellValue(dto.getCppUtility() != null ? dto.getCppUtility() : "");
                    row.createCell(colNum++).setCellValue(dto.getHrsgLoad() != null ? dto.getHrsgLoad() : 0.0);
                    row.createCell(colNum++).setCellValue(dto.getOemHeatRate() != null ? dto.getOemHeatRate() : 0.0);
                    row.createCell(colNum++).setCellValue(dto.getPrevYearFinalHeatRate() != null ? dto.getPrevYearFinalHeatRate() : 0.0);
                    row.createCell(colNum++).setCellValue(dto.getProposedYearFinalHeatRate() != null ? dto.getProposedYearFinalHeatRate() : 0.0);
                    row.createCell(colNum++).setCellValue(dto.getFinalHeatRate() != null ? dto.getFinalHeatRate() : 0.0);
                    
                    Cell remarkCell = row.createCell(colNum++);
                    remarkCell.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
                    remarkCell.setCellStyle(remarksStyle);

                    row.createCell(colNum++).setCellValue(dto.getSelectedHeatRate() != null ? dto.getSelectedHeatRate() : "");
                    row.createCell(colNum++).setCellValue(dto.getId() != null ? dto.getId() : "");

                    if (isAfterSave) {
                        row.createCell(colNum++).setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
                        row.createCell(colNum++).setCellValue(dto.getErrDescription() != null ? dto.getErrDescription() : "");
                    }

                    for (int c = 0; c < colNum; c++) {
                        if (c != 8) { 
                            row.getCell(c).setCellStyle(dataStyle);
                        }
                    }
                }

                
                for (int i = 0; i < headerList.size(); i++) {
                    if (i == 8) {
                        sheet.setColumnWidth(i, 8000); 
                        continue;
                    }
                    sheet.autoSizeColumn(i);
                    applyHeaderMinWidth(sheet, i, headerList.get(i));
                }

                workbook.write(outputStream);
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            logger.error("[JMDHeatRate] exportGTHeatRateExcelData error: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public byte[] exportAuxboilerHeatRateExcelData(UUID assetId, String year, String startDate, String endDate, 
                                            List<UUID> plantIds, boolean isAfterSave, List<CppAuxBoilerHeatRateDto> dtoList) {
        try {
            if (!isAfterSave) {
                AOPMessageVM aopMessageVM = getAuxboilerHeatRateData(assetId, year, startDate, endDate, plantIds);
                if (aopMessageVM != null && aopMessageVM.getData() != null) {
                    dtoList = (List<CppAuxBoilerHeatRateDto>) aopMessageVM.getData();
                }
            }
            if (dtoList == null) {
                dtoList = new java.util.ArrayList<>();
            }
            try (Workbook workbook = new XSSFWorkbook(); 
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                 
                Sheet sheet = workbook.createSheet("Heat Rate");
                 CellStyle headerStyle = createHeaderStyle(workbook);
                CellStyle dataStyle = createDataStyle(workbook);
                CellStyle remarksStyle = createRemarksStyle(workbook);
                List<String> headerList = new java.util.ArrayList<>(java.util.Arrays.asList(
                    "Equipment Type", "CPP Utility", "Aux Boiler Load", "OEM HR", 
                    "PREVIOUS YEAR BUDGET HR", "PROPOSED HR (Based On Actual Data)", 
                    "Final HR", "Remark", "Selected Heat Rate", "Id"
                ));

                if (isAfterSave) {
                    headerList.add("Status");
                    headerList.add("Error Description");
                }

                int rowNum = 0;
                Row headerRow = sheet.createRow(rowNum++);
                for (int i = 0; i < headerList.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headerList.get(i));
                    cell.setCellStyle(headerStyle);
                }
                sheet.setColumnHidden(8, true);
                sheet.setColumnHidden(9, true);
                for (CppAuxBoilerHeatRateDto dto : dtoList) {
                    Row row = sheet.createRow(rowNum++);
                    int colNum = 0;

                    row.createCell(colNum++).setCellValue(dto.getEquipType() != null ? dto.getEquipType() : "");
                    row.createCell(colNum++).setCellValue(dto.getCppUtility() != null ? dto.getCppUtility() : "");
                    row.createCell(colNum++).setCellValue(dto.getAuxBoilerLoad() != null ? dto.getAuxBoilerLoad() : 0.0);
                    row.createCell(colNum++).setCellValue(dto.getOemHeatRate() != null ? dto.getOemHeatRate() : 0.0);
                    row.createCell(colNum++).setCellValue(dto.getPrevYearFinalHeatRate() != null ? dto.getPrevYearFinalHeatRate() : 0.0);
                    row.createCell(colNum++).setCellValue(dto.getProposedYearFinalHeatRate() != null ? dto.getProposedYearFinalHeatRate() : 0.0);
                    row.createCell(colNum++).setCellValue(dto.getFinalHeatRate() != null ? dto.getFinalHeatRate() : 0.0);
                    
                    Cell remarkCell = row.createCell(colNum++);
                    remarkCell.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
                    remarkCell.setCellStyle(remarksStyle);

                    row.createCell(colNum++).setCellValue(dto.getSelectedHeatRate() != null ? dto.getSelectedHeatRate() : "");
                    row.createCell(colNum++).setCellValue(dto.getId() != null ? dto.getId() : "");

                    if (isAfterSave) {
                        row.createCell(colNum++).setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
                        row.createCell(colNum++).setCellValue(dto.getErrDescription() != null ? dto.getErrDescription() : "");
                    }

                    for (int c = 0; c < colNum; c++) {
                        if (c != 8) { 
                            row.getCell(c).setCellStyle(dataStyle);
                        }
                    }
                }

                
                for (int i = 0; i < headerList.size(); i++) {
                    if (i == 8) {
                        sheet.setColumnWidth(i, 8000); 
                        continue;
                    }
                    sheet.autoSizeColumn(i);
                    applyHeaderMinWidth(sheet, i, headerList.get(i));
                }

                workbook.write(outputStream);
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            logger.error("[JMDHeatRate] export AuxboilerHeatRateExcelData error: {}", e.getMessage(), e);
        }
        return null;
    }

    public List<CppCcppHeatRateDto> readCcppHeatRateData(InputStream inputStream, String year) {
        List<CppCcppHeatRateDto> cppCcppHeatRateDtos = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            Iterator<Row> rowIterator = sheet.iterator();

            if (rowIterator.hasNext())
                rowIterator.next();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                CppCcppHeatRateDto dto = new CppCcppHeatRateDto();
                try {
                    dto.setEquipType(getStringCellValue(row.getCell(0), dto));
                    dto.setCppUtility(getStringCellValue(row.getCell(1), dto));
                    dto.setCcppLoad(getNumericCellValue(row.getCell(2), dto));
                    dto.setOemHeatRate(getNumericCellValue(row.getCell(3), dto));
                    dto.setPrevYearFinalHeatRate(getNumericCellValue(row.getCell(4), dto));
                    dto.setProposedYearFinalHeatRate(getNumericCellValue(row.getCell(5), dto));
                    dto.setFinalHeatRate(getNumericCellValue(row.getCell(6), dto));
                    dto.setRemarks(getStringCellValue(row.getCell(7), dto));
                    dto.setFinancialYear(year);
                    dto.setSelectedHeatRate(getStringCellValue(row.getCell(8), dto));
                    dto.setId(getStringCellValue(row.getCell(9), dto));
                }
                catch (Exception e) {
                    e.printStackTrace();
                    dto.setErrDescription(e.getMessage());
                    dto.setSaveStatus("Failed");
                }
                cppCcppHeatRateDtos.add(dto);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return cppCcppHeatRateDtos;
    }

    @Override
    public AOPMessageVM updateCcppHeatRate(List<CppCcppHeatRateDto> dtoList, String year) {
        logger.info("[JMDHeatRate] updateCcppHeatRate - processing {} records for year: {}",
                dtoList != null ? dtoList.size() : 0, year);

        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (dtoList == null || dtoList.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("Request body cannot be empty");
                return vm;
            }

            List<CppCcppHeatRateDto> failedList = new java.util.ArrayList<>();
            List<CppCCPPHeatRate> entitiesToSave = new java.util.ArrayList<>();

            for (CppCcppHeatRateDto dto : dtoList) {
                if (dto == null) continue;
                if (dto.getSaveStatus() != null && dto.getSaveStatus().equalsIgnoreCase("Failed")) {
                    failedList.add(dto);
                    continue;
                }
                CppCCPPHeatRate entity = null;
                if (dto.getId() != null && !dto.getId().trim().isEmpty()) {
                    try {
                        entity = cppCCPPHeatRateRepository.findById(UUID.fromString(dto.getId())).orElse(null);
                    } catch (IllegalArgumentException e) {
                        logger.warn("[JMDHeatRate] Invalid UUID format provided: {}", dto.getId());
                    }
                }

                if (entity == null) {
                    logger.warn("[JMDHeatRate] Record with ID {} not found. Skipping update.", dto.getId());
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription("Record ID not found in database");
                    failedList.add(dto);
                    continue;
                }

                boolean isValueChanged = false;

                if (isDoubleChanged(entity.getCcppLoad(), dto.getCcppLoad())) isValueChanged = true;
                if (isDoubleChanged(entity.getOemHeatRate(), dto.getOemHeatRate())) isValueChanged = true;
                if (isDoubleChanged(entity.getFinalHeatRate(), dto.getFinalHeatRate())) isValueChanged = true;

                String existingSelectedHR = entity.getSelectedHeatRate() != null ? entity.getSelectedHeatRate().trim() : "";
                String incomingSelectedHR = dto.getSelectedHeatRate() != null ? dto.getSelectedHeatRate().trim() : "";
                if (!existingSelectedHR.equalsIgnoreCase(incomingSelectedHR)) {
                    isValueChanged = true;
                }

                String incomingRemarks = dto.getRemarks() != null ? dto.getRemarks().trim() : "";

                if (isValueChanged) {
                    if (incomingRemarks.isEmpty()) {
                        dto.setSaveStatus("Failed");
                        dto.setErrDescription("Remarks are mandatory when data values are updated");
                        failedList.add(dto);
                        continue;
                    }

                    String existingRemarks = entity.getRemarks() != null ? entity.getRemarks().trim() : "";
                    if (incomingRemarks.equalsIgnoreCase(existingRemarks)) {
                        dto.setSaveStatus("Failed");
                        dto.setErrDescription("Remarks must be updated because data values changed");
                        failedList.add(dto);
                        continue;
                    }
                }

                entity.setCcppLoad(dto.getCcppLoad());
                entity.setOemHeatRate(dto.getOemHeatRate());
                entity.setFinalHeatRate(dto.getFinalHeatRate());
                entity.setSelectedHeatRate(dto.getSelectedHeatRate());
                entity.setRemarks(dto.getRemarks());

                entitiesToSave.add(entity);
            }

            if (!entitiesToSave.isEmpty()) {
                List<CppCCPPHeatRate> savedEntities = cppCCPPHeatRateRepository.saveAll(entitiesToSave);
                logger.info("[JMDHeatRate] updateCcppHeatRate - successfully updated {} records", savedEntities.size());

                if (!failedList.isEmpty()) {
                    vm.setCode(400);
                    vm.setMessage("Partial data saved with validation exceptions");
                } else {
                    vm.setCode(200);
                    vm.setMessage("Data updated successfully");
                }
                vm.setData(failedList);
            } else {
                vm.setCode(400);
                vm.setMessage("No valid records met conditions to update");
                vm.setData(failedList);
            }

        } catch (Exception e) {
            logger.error("[JMDHeatRate] updateCcppHeatRate error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to update CCPP heat rate data: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }

    @Override
    public byte[] exportCcppHeatRateExcelData(UUID assetId, String year, String startDate, String endDate,
                                            List<UUID> plantIds, boolean isAfterSave, List<CppCcppHeatRateDto> dtoList) {
        try {
            if (!isAfterSave) {
                AOPMessageVM aopMessageVM = getCcppHeatRateData(assetId, year, startDate, endDate, plantIds);
                if (aopMessageVM != null && aopMessageVM.getData() != null) {
                    dtoList = (List<CppCcppHeatRateDto>) aopMessageVM.getData();
                }
            }
            if (dtoList == null) {
                dtoList = new java.util.ArrayList<>();
            }
            try (Workbook workbook = new XSSFWorkbook();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                Sheet sheet = workbook.createSheet("Heat Rate");
                CellStyle headerStyle = createHeaderStyle(workbook);
                CellStyle dataStyle = createDataStyle(workbook);
                CellStyle remarksStyle = createRemarksStyle(workbook);
                List<String> headerList = new java.util.ArrayList<>(java.util.Arrays.asList(
                    "Equipment Type", "CPP Utility", "CCPP Load", "OEM HR",
                    "PREVIOUS YEAR BUDGET HR", "PROPOSED HR (Based On Actual Data)",
                    "Final HR", "Remark", "Selected Heat Rate", "Id"
                ));

                if (isAfterSave) {
                    headerList.add("Status");
                    headerList.add("Error Description");
                }

                int rowNum = 0;
                Row headerRow = sheet.createRow(rowNum++);
                for (int i = 0; i < headerList.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headerList.get(i));
                    cell.setCellStyle(headerStyle);
                }
                sheet.setColumnHidden(8, true);
                sheet.setColumnHidden(9, true);
                for (CppCcppHeatRateDto dto : dtoList) {
                    Row row = sheet.createRow(rowNum++);
                    int colNum = 0;

                    row.createCell(colNum++).setCellValue(dto.getEquipType() != null ? dto.getEquipType() : "");
                    row.createCell(colNum++).setCellValue(dto.getCppUtility() != null ? dto.getCppUtility() : "");
                    row.createCell(colNum++).setCellValue(dto.getCcppLoad() != null ? dto.getCcppLoad() : 0.0);
                    row.createCell(colNum++).setCellValue(dto.getOemHeatRate() != null ? dto.getOemHeatRate() : 0.0);
                    row.createCell(colNum++).setCellValue(dto.getPrevYearFinalHeatRate() != null ? dto.getPrevYearFinalHeatRate() : 0.0);
                    row.createCell(colNum++).setCellValue(dto.getProposedYearFinalHeatRate() != null ? dto.getProposedYearFinalHeatRate() : 0.0);
                    row.createCell(colNum++).setCellValue(dto.getFinalHeatRate() != null ? dto.getFinalHeatRate() : 0.0);

                    Cell remarkCell = row.createCell(colNum++);
                    remarkCell.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
                    remarkCell.setCellStyle(remarksStyle);

                    row.createCell(colNum++).setCellValue(dto.getSelectedHeatRate() != null ? dto.getSelectedHeatRate() : "");
                    row.createCell(colNum++).setCellValue(dto.getId() != null ? dto.getId() : "");

                    if (isAfterSave) {
                        row.createCell(colNum++).setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
                        row.createCell(colNum++).setCellValue(dto.getErrDescription() != null ? dto.getErrDescription() : "");
                    }

                    for (int c = 0; c < colNum; c++) {
                        if (c != 7) {
                            row.getCell(c).setCellStyle(dataStyle);
                        }
                    }
                }

                for (int i = 0; i < headerList.size(); i++) {
                    if (i == 7) {
                        sheet.setColumnWidth(i, 8000);
                        continue;
                    }
                    sheet.autoSizeColumn(i);
                    applyHeaderMinWidth(sheet, i, headerList.get(i));
                }

                workbook.write(outputStream);
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            logger.error("[JMDHeatRate] exportCcppHeatRateExcelData error: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public AOPMessageVM importCcppHeatRateData(String year, UUID assetId, String startDate, String endDate, List<UUID> plantIds, MultipartFile file) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {

            List<CppCcppHeatRateDto> data = readCcppHeatRateData(file.getInputStream(), year);

            if (data != null && !data.isEmpty()) {
                CppCcppHeatRateDto firstRow = data.get(0);
                if (assetId == null && firstRow.getAssetFkId() != null) {
                    assetId = UUID.fromString(firstRow.getAssetFkId());
                }
            }

            aopMessageVM = updateCcppHeatRate(data, year);

            List<CppCcppHeatRateDto> failedList = (List<CppCcppHeatRateDto>) aopMessageVM.getData();

            if (failedList != null && !failedList.isEmpty()) {
                byte[] fileByteArray = exportCcppHeatRateExcelData(
                    assetId,
                    year,
                    startDate,
                    endDate,
                    plantIds,
                    true,
                    failedList
                );

                if (fileByteArray != null && fileByteArray.length > 0) {
                    String base64File = Base64.getEncoder().encodeToString(fileByteArray);
                    aopMessageVM.setData(base64File);
                    aopMessageVM.setCode(400);
                    aopMessageVM.setMessage("Partial data has been saved. Please check the attached error details.");
                }
            } else {
                aopMessageVM.setCode(200);
                aopMessageVM.setMessage("All data has been saved successfully.");
                aopMessageVM.setData(null);
            }

            return aopMessageVM;

        } catch (Exception e) {
            logger.error("[JMDHeatRate] Error importing CCPP heat rate data: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to process file import: " + e.getMessage());
            aopMessageVM.setData(null);
        }
        return aopMessageVM;
    }

    @Override
    public byte[] exportSTGHeatRateExcelData(UUID assetId, String year, String startDate, String endDate, 
                                            List<UUID> plantIds, boolean isAfterSave, List<STGHeatRateDTO> dtoList) {
        try {
            if (!isAfterSave) {
                AOPMessageVM aopMessageVM = getSTGHeatRate(assetId.toString(), year, startDate, endDate, plantIds);
                if (aopMessageVM != null && aopMessageVM.getData() != null) {
                    dtoList = (List<STGHeatRateDTO>) aopMessageVM.getData();
                }
            }
            if (dtoList == null) {
                dtoList = new java.util.ArrayList<>();
            }
            try (Workbook workbook = new XSSFWorkbook(); 
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                 
                Sheet sheet = workbook.createSheet("Heat Rate");
                 CellStyle headerStyle = createHeaderStyle(workbook);
                CellStyle dataStyle = createDataStyle(workbook);
                CellStyle remarksStyle = createRemarksStyle(workbook);
                List<String> headerList = new java.util.ArrayList<>(java.util.Arrays.asList(
                    "Equipment Type", "CPP Utility", "STG Load (MW)", "OEM HR", 
                    "PREVIOUS YEAR BUDGET HR", "PROPOSED HR (Based On Actual Data)", 
                    "Final HR", "Remark", "Selected Heat Rate", "Id"
                ));

                if (isAfterSave) {
                    headerList.add("Status");
                    headerList.add("Error Description");
                }

                int rowNum = 0;
                Row headerRow = sheet.createRow(rowNum++);
                for (int i = 0; i < headerList.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headerList.get(i));
                    cell.setCellStyle(headerStyle);
                }
                sheet.setColumnHidden(8, true);
                sheet.setColumnHidden(9, true);
                for (STGHeatRateDTO dto : dtoList) {
                    Row row = sheet.createRow(rowNum++);
                    int colNum = 0;

                    row.createCell(colNum++).setCellValue(dto.getEquipType() != null ? dto.getEquipType() : "");
                    row.createCell(colNum++).setCellValue(dto.getCppUtility() != null ? dto.getCppUtility() : "");
                    row.createCell(colNum++).setCellValue(dto.getStgLoad() != null ? dto.getStgLoad() : 0.0);
                    row.createCell(colNum++).setCellValue(dto.getOemHeatRate() != null ? dto.getOemHeatRate() : 0.0);
                    row.createCell(colNum++).setCellValue(dto.getPreviousYearHeatRate() != null ? dto.getPreviousYearHeatRate() : 0.0);
                    row.createCell(colNum++).setCellValue(dto.getProposedHeatRate() != null ? dto.getProposedHeatRate() : 0.0);
                    row.createCell(colNum++).setCellValue(dto.getFinalHeatRate() != null ? dto.getFinalHeatRate() : 0.0);
                    
                    Cell remarkCell = row.createCell(colNum++);
                    remarkCell.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
                    remarkCell.setCellStyle(remarksStyle);

                    row.createCell(colNum++).setCellValue(dto.getSelectedHeatRate() != null ? dto.getSelectedHeatRate() : "");
                    row.createCell(colNum++).setCellValue(dto.getId() != null ? dto.getId() : "");

                    if (isAfterSave) {
                        row.createCell(colNum++).setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
                        row.createCell(colNum++).setCellValue(dto.getErrDescription() != null ? dto.getErrDescription() : "");
                    }

                    for (int c = 0; c < colNum; c++) {
                        if (c != 8) { 
                            row.getCell(c).setCellStyle(dataStyle);
                        }
                    }
                }

                
                for (int i = 0; i < headerList.size(); i++) {
                    if (i == 8) {
                        sheet.setColumnWidth(i, 8000); 
                        continue;
                    }
                    sheet.autoSizeColumn(i);
                    applyHeaderMinWidth(sheet, i, headerList.get(i));
                }

                workbook.write(outputStream);
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            logger.error("[JMDHeatRate] exportGTHeatRateExcelData error: {}", e.getMessage(), e);
        }
        return null;
    }

    private PowerGenerationAssetDto convertToDto(PowerGenerationAsset entity) {
        PowerGenerationAssetDto dto = new PowerGenerationAssetDto();

        dto.setAssetId(entity.getAssetId() != null ? entity.getAssetId() : null);
        dto.setAssetName(entity.getAssetName() != null ? entity.getAssetName() : null);
        dto.setCppPlantFkId(entity.getCppPlantFkId() != null ? entity.getCppPlantFkId() : null);
        dto.setPlantCode(entity.getPlantCode() != null ? entity.getPlantCode() : null);
        dto.setAssetType(entity.getAssetType() != null ? entity.getAssetType() : null);
        dto.setRemarks(entity.getRemarks() != null ? entity.getRemarks() : null);
        dto.setDisplayName(entity.getDisplayName() != null ? entity.getDisplayName() : null);
        dto.setUtilityGenerationFkId(entity.getUtilityGenerationFkId() != null ? entity.getUtilityGenerationFkId() : null);
        dto.setUtilityDistributedFkId(entity.getUtilityDistributedFkId() != null ? entity.getUtilityDistributedFkId() : null);
        dto.setCompatibleFuel(entity.getCompatibleFuel() != null ? entity.getCompatibleFuel() : null);

        return dto;
    }
    
    private CppSteamGenerationAssetDto convertToCppSteamGenerationAssetDto(CppSteamGenerationAsset entity) {
        if (entity == null) {
            return null;
        }

        CppSteamGenerationAssetDto dto = new CppSteamGenerationAssetDto();

        dto.setAssetId(entity.getAssetId() != null ? entity.getAssetId().toString() : null);
        dto.setCppPlantFkId(entity.getCppPlantFkId() != null ? entity.getCppPlantFkId().toString() : null);
        dto.setUtilityGenerationFkId(entity.getUtilityGenerationFkId() != null ? entity.getUtilityGenerationFkId().toString() : null);
        dto.setUtilityDistributedFkId(entity.getUtilityDistributedFkId() != null ? entity.getUtilityDistributedFkId().toString() : null);
        dto.setLinkedPowerAssetFkId(entity.getLinkedPowerAssetFkId() != null ? entity.getLinkedPowerAssetFkId().toString() : null);
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());
        dto.setAssetName(entity.getAssetName());
        dto.setAssetType(entity.getAssetType());
        dto.setPlantCode(entity.getPlantCode());
        dto.setRemarks(entity.getRemarks());
        dto.setDisplayName(entity.getDisplayName());
        dto.setSteamType(entity.getSteamType());
        dto.setCompatibleFuel(entity.getCompatibleFuel());
        dto.setIsVisible(entity.getIsVisible());
        dto.setIsEditable(entity.getIsEditable());

        return dto;
    }
    
    @Override
    public AOPMessageVM getHRSGAssetDropdown(List<UUID> plantIds) {
        logger.info("[JMDHeatRate] getHRSGAssetDropdown - plantIds: {}", plantIds);
        AOPMessageVM vm = new AOPMessageVM();
        try {
            String assetType = "HRSG";
            List<Object[]> result = new ArrayList<>();
            for (UUID plantId : plantIds) {
                result.addAll(heatRateRepository.findHRSGAssetNamesByCppIdAndAssetType(plantId, assetType).stream()
                        .map(projection -> new Object[]{projection.getAssetId(), projection.getAssetName()})
                        .toList());
            }
            logger.info("[JMDHeatRate] getHRSGAssetDropdown - found {} assets", result.size());
            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(result);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] getHRSGAssetDropdown error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to fetch HRSG asset dropdown: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }

    // ============================================================
    // GT HEAT RATE
    // ============================================================

    @Override
    public AOPMessageVM getGTHeatRate(String assetId, String aopYear, String startDate, String endDate) {
        logger.info("[JMDHeatRate] getGTHeatRate - assetId: {}, aopYear: {}, startDate: {}, endDate: {}", assetId, aopYear, startDate, endDate);
        AOPMessageVM vm = new AOPMessageVM();
        try {
            List<HeatRateDTO> result;
            if (startDate != null && !startDate.trim().isEmpty() && endDate != null && !endDate.trim().isEmpty()) {
                result = getGTHeatRateWithProposed(assetId, aopYear, startDate, endDate);
            } else {
                result = getGTHeatRateByAssetId(assetId, aopYear);
            }
            logger.info("[JMDHeatRate] getGTHeatRate - returning {} records", result.size());
            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(result);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] getGTHeatRate error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to fetch GT heat rate: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }

    private List<HeatRateDTO> getGTHeatRateByAssetId(String assetId, String financialYear) {
        String previousFinancialYear = calculatePreviousFinancialYear(financialYear);
        UUID assetUUID = UUID.fromString(assetId);
        List<HeatRateProjection> projections = heatRateRepository.findGtHeatRateByAssetId(assetUUID, financialYear, previousFinancialYear);
        return projections.stream()
                .map(projection -> {
                    HeatRateDTO dto = new HeatRateDTO();
                    dto.setId(projection.getId());
                    dto.setEquipType(projection.getEquipType());
                    dto.setCppUtility(projection.getCPPUtility());
                    dto.setGtLoad(projection.getGTLoad());
                    dto.setHeatRate(projection.getHeatRate());
                    dto.setFreeSteamFactor(projection.getFreeSteamFactor());
                    dto.setRemarks(projection.getRemarks());
                    dto.setPreviousYearHeatRate(projection.getPreviousYearHeatRate());
                    dto.setFinalHeatRate(projection.getFinalHeatRate());
                    dto.setOemHeatRate(projection.getOemHeatRate());
                    dto.setSelectedHeatRate(projection.getSelectedHeatRate());
                    return dto;
                })
                .toList();
    }

    private List<HeatRateDTO> getGTHeatRateWithProposed(String assetId, String financialYear, String startDate, String endDate) {
        List<HeatRateDTO> heatRateDTOs = getGTHeatRateByAssetId(assetId, financialYear);
        if (startDate != null && !startDate.trim().isEmpty() && endDate != null && !endDate.trim().isEmpty()) {
            UUID assetUUID = UUID.fromString(assetId);
            Map<Double, Double> proposedHeatRateMap = calculateProposedGTHeatRates(assetUUID, startDate, endDate);
            for (HeatRateDTO dto : heatRateDTOs) {
                Double proposedHeatRate = proposedHeatRateMap.get(dto.getGtLoad());
                if (proposedHeatRate != null) {
                    dto.setProposedHeatRate(proposedHeatRate);
                }
            }
        }
        return heatRateDTOs;
    }

    private Map<Double, Double> calculateProposedGTHeatRates(UUID assetId, String startDate, String endDate) {
        String assetName = null;
        try {
            assetName = jdbcTemplate.queryForObject(
                    "SELECT displayName FROM PowerGenerationAssets WITH(NOLOCK) WHERE AssetId = ?",
                    String.class, assetId);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] Error retrieving asset name for assetId {}: {}", assetId, e.getMessage());
        }
        if (assetName == null || assetName.trim().isEmpty()) {
            return new HashMap<>();
        }
        String sql = "EXEC CPP_CalculateGTHeatRate_ByDateRange @StartDate = ?, @EndDate = ?, @AssetName = ?";
        Map<Double, Double> proposedHeatRateMap = new HashMap<>();
        try {
            jdbcTemplate.query(sql,
                    (rs) -> {
                        Double gtLoad = rs.getDouble("GTLoad");
                        Double heatRate = rs.getDouble("HeatRate");
                        proposedHeatRateMap.put(gtLoad, heatRate);
                    },
                    startDate, endDate, assetName);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] Error calling GT stored procedure: {}", e.getMessage(), e);
        }
        return proposedHeatRateMap;
    }

    @Override
    @Transactional
    public AOPMessageVM updateGTHeatRate(List<HeatRateDTO> heatRateDTOs, String aopYear) {
        logger.info("[JMDHeatRate] updateGTHeatRate - {} records, aopYear: {}", heatRateDTOs != null ? heatRateDTOs.size() : 0, aopYear);
        AOPMessageVM vm = new AOPMessageVM();
        try {
            List<Object[]> updates = new ArrayList<>();
            for (HeatRateDTO dto : heatRateDTOs) {
                String selectedHeatRate = dto.getSelectedHeatRate();
                if (selectedHeatRate != null && !selectedHeatRate.trim().isEmpty()) {
                    if (!SelectedHeatRateType.isValid(selectedHeatRate)) {
                        throw new IllegalArgumentException(
                                String.format("Invalid selectedHeatRate value: '%s'. Must be one of: OEM, PREVIOUS_YEAR, PROPOSED, OTHER", selectedHeatRate));
                    }
                } else {
                    selectedHeatRate = SelectedHeatRateType.PROPOSED.getValue();
                    dto.setSelectedHeatRate(selectedHeatRate);
                }
                updates.add(new Object[]{
                        dto.getGtLoad(),
                        dto.getFreeSteamFactor(),
                        dto.getFinalHeatRate(),
                        dto.getOemHeatRate(),
                        selectedHeatRate,
                        dto.getRemarks(),
                        dto.getId()
                });
            }
            if (!updates.isEmpty()) {
                String sql = "update CPP_GTHeatRate set GTLoad = ?, FreeSteamFactor = ?, FinalHeatRate = ?, OEMHeatRate = ?, SelectedHeatRate = ?, Remarks = ?, UpdatedDate = GETDATE() WHERE Id = ?";
                jdbcTemplate.batchUpdate(sql, updates);
            }
            vm.setCode(200);
            vm.setMessage("GT heat rate updated successfully. " + updates.size() + " records updated.");
            vm.setData(null);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] updateGTHeatRate error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to update GT heat rate: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }

    // ============================================================
    // HRSG HEAT RATE
    // ============================================================

    @Override
    public AOPMessageVM getHRSGHeatRate(String assetId, String aopYear, String startDate, String endDate) {
        logger.info("[JMDHeatRate] getHRSGHeatRate - assetId: {}, aopYear: {}, startDate: {}, endDate: {}", assetId, aopYear, startDate, endDate);
        AOPMessageVM vm = new AOPMessageVM();
        try {
            List<com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateDTO> result;
            if (startDate != null && !startDate.trim().isEmpty() && endDate != null && !endDate.trim().isEmpty()) {
                result = getHRSGHeatRateWithProposed(assetId, aopYear, startDate, endDate);
            } else {
                result = getHRSGHeatRateByAssetId(assetId, aopYear);
            }
            logger.info("[JMDHeatRate] getHRSGHeatRate - returning {} records", result.size());
            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(result);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] getHRSGHeatRate error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to fetch HRSG heat rate: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }

    private List<com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateDTO> getHRSGHeatRateByAssetId(String assetId, String financialYear) {
        String previousFinancialYear = calculatePreviousFinancialYear(financialYear);
        UUID assetUUID = UUID.fromString(assetId);
        List<HRSGHeatRateProjection> projections = heatRateRepository.findHrsgHeatRateByAssetId(assetUUID, financialYear, previousFinancialYear);
        return projections.stream()
                .map(projection -> {
                    com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateDTO dto = new com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateDTO();
                    dto.setId(projection.getId());
                    dto.setEquipType(projection.getEquipType());
                    dto.setCppUtility(projection.getCPPUtility());
                    dto.setHrsgLoad(projection.getHRSGLoad());
                    dto.setHeatRate(projection.getHeatRate());
                    dto.setRemarks(projection.getRemarks());
                    dto.setPreviousYearHeatRate(projection.getPreviousYearHeatRate());
                    dto.setFinalHeatRate(projection.getFinalHeatRate());
                    dto.setOemHeatRate(projection.getOEMHeatRate());
                    dto.setSelectedHeatRate(projection.getSelectedHeatRate());
                    return dto;
                })
                .toList();
    }

    private List<com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateDTO> getHRSGHeatRateWithProposed(String assetId, String financialYear, String startDate, String endDate) {
        List<com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateDTO> dtos = getHRSGHeatRateByAssetId(assetId, financialYear);
        if (startDate != null && !startDate.trim().isEmpty() && endDate != null && !endDate.trim().isEmpty()) {
            UUID assetUUID = UUID.fromString(assetId);
            Map<Double, Double> proposedHeatRateMap = calculateProposedHRSGHeatRates(assetUUID, startDate, endDate);
            for (com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateDTO dto : dtos) {
                Double proposedHeatRate = proposedHeatRateMap.get(dto.getHrsgLoad());
                if (proposedHeatRate != null) {
                    dto.setProposedHeatRate(proposedHeatRate);
                }
            }
        }
        return dtos;
    }

    private Map<Double, Double> calculateProposedHRSGHeatRates(UUID assetId, String startDate, String endDate) {
        String assetName = null;
        try {
            assetName = jdbcTemplate.queryForObject(
                    "SELECT TOP 1 AssetName FROM CPP_HRSGHeatRate WITH(NOLOCK) WHERE Asset_FK_Id = ?",
                    String.class, assetId);
        } catch (Exception e) {
            try {
                assetName = jdbcTemplate.queryForObject(
                        "SELECT AssetName FROM SteamGenerationAssets WITH(NOLOCK) WHERE AssetId = ?",
                        String.class, assetId);
            } catch (Exception e2) {
                try {
                    assetName = jdbcTemplate.queryForObject(
                            "SELECT displayName FROM SteamGenerationAssets WITH(NOLOCK) WHERE AssetId = ?",
                            String.class, assetId);
                } catch (Exception e3) {
                    logger.error("[JMDHeatRate] Error retrieving HRSG asset name for assetId {}: {}", assetId, e3.getMessage());
                }
            }
        }
        if (assetName == null || assetName.trim().isEmpty()) {
            return new HashMap<>();
        }
        String spAssetName = assetName.replace("HRSG", "HRSG-");
        String sql = "EXEC CPP_CalculateHRSGHeatRate_ByDateRange @StartDate = ?, @EndDate = ?, @AssetName = ?";
        Map<Double, Double> proposedHeatRateMap = new HashMap<>();
        try {
            jdbcTemplate.query(sql,
                    (rs) -> {
                        Double hrsgLoad = rs.getDouble("HRSGLoad");
                        Double heatRate = rs.getDouble("HeatRate");
                        proposedHeatRateMap.put(hrsgLoad, heatRate);
                    },
                    startDate, endDate, spAssetName);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] Error calling HRSG stored procedure: {}", e.getMessage(), e);
        }
        return proposedHeatRateMap;
    }

    @Override
    @Transactional
    public AOPMessageVM updateHRSGHeatRate(List<com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateDTO> hrsgHeatRateDTOs, String aopYear) {
        logger.info("[JMDHeatRate] updateHRSGHeatRate - {} records, aopYear: {}", hrsgHeatRateDTOs != null ? hrsgHeatRateDTOs.size() : 0, aopYear);
        AOPMessageVM vm = new AOPMessageVM();
        try {
            List<Object[]> updates = new ArrayList<>();
            for (com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateDTO dto : hrsgHeatRateDTOs) {
                String selectedHeatRate = dto.getSelectedHeatRate();
                if (selectedHeatRate != null && !selectedHeatRate.trim().isEmpty()) {
                    if (!SelectedHeatRateType.isValid(selectedHeatRate)) {
                        throw new IllegalArgumentException(
                                String.format("Invalid selectedHeatRate value: '%s'. Must be one of: OEM, PREVIOUS_YEAR, PROPOSED, OTHER", selectedHeatRate));
                    }
                } else {
                    selectedHeatRate = SelectedHeatRateType.PROPOSED.getValue();
                    dto.setSelectedHeatRate(selectedHeatRate);
                }
                updates.add(new Object[]{
                        dto.getHrsgLoad(),
                        dto.getFinalHeatRate(),
                        dto.getOemHeatRate(),
                        selectedHeatRate,
                        dto.getRemarks(),
                        dto.getId()
                });
            }
            if (!updates.isEmpty()) {
                String sql = "UPDATE CPP_HRSGHeatRate SET HRSGLoad = ?, FinalHeatRate = ?, OEMHeatRate = ?, SelectedHeatRate = ?, Remarks = ?, UpdatedDate = GETDATE() WHERE Id = ?";
                jdbcTemplate.batchUpdate(sql, updates);
            }
            vm.setCode(200);
            vm.setMessage("HRSG heat rate updated successfully. " + updates.size() + " records updated.");
            vm.setData(null);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] updateHRSGHeatRate error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to update HRSG heat rate: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }

    // ============================================================
    // STG HEAT RATE
    // ============================================================

    @Override
    public AOPMessageVM getSTGHeatRate(String assetId, String aopYear, String startDate, String endDate, List<UUID> plantIds) {
        logger.info("[JMDHeatRate] getSTGHeatRate - assetId: {}, aopYear: {}, startDate: {}, endDate: {}, plantIds: {}", assetId, aopYear, startDate, endDate, plantIds);
        AOPMessageVM vm = new AOPMessageVM();
        try {
            List<STGHeatRateDTO> result;
            if (startDate != null && !startDate.trim().isEmpty() && endDate != null && !endDate.trim().isEmpty()) {
                result = getSTGHeatRateWithProposed(assetId, aopYear, startDate, endDate, plantIds);
            } else {
                result = getSTGHeatRateByAssetId(assetId, aopYear);
            }
            logger.info("[JMDHeatRate] getSTGHeatRate - returning {} records", result.size());
            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(result);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] getSTGHeatRate error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to fetch STG heat rate: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }

    private List<STGHeatRateDTO> getSTGHeatRateByAssetId(String assetId, String financialYear) {
        String previousFinancialYear = calculatePreviousFinancialYear(financialYear);
        UUID assetUUID = UUID.fromString(assetId);
        String assetName = null;
        try {
            assetName = jdbcTemplate.queryForObject(
                    "SELECT AssetName FROM PowerGenerationAssets WITH(NOLOCK) WHERE AssetId = ?",
                    String.class, assetUUID);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] Error retrieving asset name for assetId {}: {}", assetUUID, e.getMessage());
        }
        if (assetName == null || assetName.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<STGHeatRateProjection> projections = heatRateRepository.findStgHeatRateByAssetName(assetName, financialYear, previousFinancialYear);
        return projections.stream()
                .map(projection -> {
                    STGHeatRateDTO dto = new STGHeatRateDTO();
                    dto.setId(projection.getId().toString());
                    dto.setEquipType(projection.getEquipType());
                    dto.setCppUtility(projection.getCPPUtility());
                    dto.setStgLoad(projection.getSTGLoad());
                    dto.setHeatRate(projection.getHeatRate());
                    dto.setRemarks(projection.getRemarks());
                    dto.setPreviousYearHeatRate(projection.getPreviousYearHeatRate());
                    dto.setFinalHeatRate(projection.getFinalHeatRate());
                    dto.setOemHeatRate(projection.getOEMHeatRate());
                    dto.setSelectedHeatRate(projection.getSelectedHeatRate());
                    return dto;
                })
                .toList();
    }

    private List<STGHeatRateDTO> getSTGHeatRateWithProposed(String assetId, String financialYear, String startDate, String endDate, List<UUID> plantIds) {
        List<STGHeatRateDTO> dtos = getSTGHeatRateByAssetId(assetId, financialYear);
        if (startDate != null && !startDate.trim().isEmpty() && endDate != null && !endDate.trim().isEmpty()) {
            UUID assetUUID = UUID.fromString(assetId);
            Map<Double, Double> proposedHeatRateMap = calculateProposedSTGHeatRates(assetUUID, startDate, endDate, plantIds);
            for (STGHeatRateDTO dto : dtos) {
                Double proposedHeatRate = proposedHeatRateMap.get(dto.getStgLoad());
                if (proposedHeatRate != null) {
                    dto.setProposedHeatRate(proposedHeatRate);
                }
            }
        }
        return dtos;
    }

    private Map<Double, Double> calculateProposedSTGHeatRates(UUID assetId, String startDate, String endDate, List<UUID> plantIds) {
        String assetName = null;
        try {
            assetName = jdbcTemplate.queryForObject(
                    "SELECT AssetName FROM PowerGenerationAssets WITH(NOLOCK) WHERE AssetId = ?",
                    String.class, assetId);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] Error retrieving asset name for assetId {}: {}", assetId, e.getMessage());
        }
        if (assetName == null || assetName.trim().isEmpty()) {
            return new HashMap<>();
        }
        String plantIdsStr = "";
        if (plantIds != null && !plantIds.isEmpty()) {
            plantIdsStr = plantIds.stream()
                    .map(UUID::toString)
                    .collect(java.util.stream.Collectors.joining(","));
        }
        String sql = "EXEC CPP_CalculateCommonSTGHeatRate_ByDateRange @StartDate = ?, @EndDate = ?, @AssetName = ?, @PlantIds = ?";
        Map<Double, Double> proposedHeatRateMap = new HashMap<>();
        try {
            jdbcTemplate.query(sql,
                    (rs) -> {
                        Double stgLoad = rs.getDouble("STGLoad");
                        Double heatRate = rs.getDouble("HeatRate");
                        if (!rs.wasNull()) {
                            proposedHeatRateMap.put(stgLoad, heatRate);
                        }
                    },
                    startDate, endDate, assetName, plantIdsStr);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] Error calling STG stored procedure: {}", e.getMessage(), e);
        }
        return proposedHeatRateMap;
    }

    @Override
    @Transactional
    public AOPMessageVM updateSTGHeatRate(List<STGHeatRateDTO> stgHeatRateDTOs, String aopYear) {
        logger.info("[JMDHeatRate] updateSTGHeatRate - {} records, aopYear: {}", 
                stgHeatRateDTOs != null ? stgHeatRateDTOs.size() : 0, aopYear);
        
        AOPMessageVM vm = new AOPMessageVM();
        
        try {
            if (stgHeatRateDTOs == null || stgHeatRateDTOs.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("Request body cannot be empty");
                return vm;
            }

            List<STGHeatRateDTO> failedList = new java.util.ArrayList<>();
            List<CppStgHeatRate> entitiesToSave = new java.util.ArrayList<>();

            for (STGHeatRateDTO dto : stgHeatRateDTOs) {
                if (dto == null) continue;

                if (dto.getSaveStatus() != null && dto.getSaveStatus().equalsIgnoreCase("Failed")) {
                    failedList.add(dto);
                    continue;
                }

                String selectedHeatRate = dto.getSelectedHeatRate();
                if (selectedHeatRate != null && !selectedHeatRate.trim().isEmpty()) {
                    if (!SelectedHeatRateType.isValid(selectedHeatRate)) {
                        dto.setSaveStatus("Failed");
                        dto.setErrDescription(String.format("Invalid selectedHeatRate value: '%s'. Must be one of: OEM, PREVIOUS_YEAR, PROPOSED, OTHER", selectedHeatRate));
                        failedList.add(dto);
                        continue;
                    }
                } else {
                    selectedHeatRate = SelectedHeatRateType.PROPOSED.getValue();
                    dto.setSelectedHeatRate(selectedHeatRate);
                }

                CppStgHeatRate entity = null;
                if (dto.getId() != null && !dto.getId().toString().trim().isEmpty()) {
                    try {
                        entity = cppStgHeatRateRepository.findById(UUID.fromString(dto.getId())).orElse(null);
                    } catch (IllegalArgumentException e) {
                        logger.warn("[JMDHeatRate] Invalid UUID format provided: {}", dto.getId());
                    }
                }

                if (entity == null) {
                    logger.warn("[JMDHeatRate] Record with ID {} not found. Skipping update.", dto.getId());
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription("Record ID not found in database");
                    failedList.add(dto);
                    continue;
                }

                boolean isValueChanged = false;
                if (isDoubleChanged(entity.getStgLoad(), dto.getStgLoad())) isValueChanged = true;
                if (isDoubleChanged(entity.getOemHeatRate(), dto.getOemHeatRate())) isValueChanged = true;
                if (isDoubleChanged(entity.getFinalHeatRate(), dto.getFinalHeatRate())) isValueChanged = true;

                String dbSelected = entity.getSelectedHeatRate() != null ? entity.getSelectedHeatRate().trim() : "";
                String incomingSelectedHR = dto.getSelectedHeatRate() != null ? dto.getSelectedHeatRate().trim() : "";
                if (!dbSelected.equalsIgnoreCase(incomingSelectedHR)) {
                    isValueChanged = true;
                }

                String incomingRemarks = dto.getRemarks() != null ? dto.getRemarks().trim() : "";
                if (isValueChanged) {
                    if (incomingRemarks.isEmpty()) {
                        dto.setSaveStatus("Failed");
                        dto.setErrDescription("Remarks are mandatory when data values are updated");
                        failedList.add(dto);
                        continue;
                    }
                    
                    String dbRemarks = entity.getRemarks() != null ? entity.getRemarks().trim() : "";
                    if (incomingRemarks.equalsIgnoreCase(dbRemarks)) {
                        dto.setSaveStatus("Failed");
                        dto.setErrDescription("Remarks must be updated because data values changed");
                        failedList.add(dto);
                        continue;
                    }
                }

                entity.setStgLoad(dto.getStgLoad());
                entity.setFinalHeatRate(dto.getFinalHeatRate());
                entity.setOemHeatRate(dto.getOemHeatRate());
                entity.setSelectedHeatRate(selectedHeatRate);
                entity.setRemarks(dto.getRemarks());
                entity.setUpdatedDate(new java.util.Date()); 

                entitiesToSave.add(entity);
            }

            if (!entitiesToSave.isEmpty()) {
                List<CppStgHeatRate> savedEntities = cppStgHeatRateRepository.saveAll(entitiesToSave);
                logger.info("[JMDHeatRate] updateSTGHeatRate - successfully updated {} records", savedEntities.size());
                
                if (!failedList.isEmpty()) {
                    vm.setCode(400);
                    vm.setMessage("Partial data saved with validation exceptions");
                } else {
                    vm.setCode(200);
                    vm.setMessage("STG heat rate updated successfully. " + savedEntities.size() + " records updated.");
                }
                vm.setData(failedList);
            } else {
                vm.setCode(400);
                vm.setMessage("No valid records met conditions to update");
                vm.setData(failedList);
            }

        } catch (Exception e) {
            logger.error("[JMDHeatRate] updateSTGHeatRate error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to update STG heat rate: " + e.getMessage());
            vm.setData(null);
        }
        
        return vm;
    }
    
    // ============================================================
    // STG EXTRACTION LOOKUP
    // ============================================================

    @Override
    public AOPMessageVM getSTGExtractionLookup() {
        logger.info("[JMDHeatRate] getSTGExtractionLookup");
        AOPMessageVM vm = new AOPMessageVM();
        try {
            List<STGExtractionLookupDTO> result = stgExtractionLookupRepository.findAllByOrderByLoadMWAsc().stream()
                    .map(this::mapToSTGExtractionDTO)
                    .toList();
            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(result);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] getSTGExtractionLookup error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to fetch STG extraction lookup: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }

    @Override
    @Transactional
    public AOPMessageVM updateSTGExtraction(List<STGExtractionLookupDTO> stgExtractionLookupDTOs, String aopYear) {
        logger.info("[JMDHeatRate] updateSTGExtraction - {} records", stgExtractionLookupDTOs != null ? stgExtractionLookupDTOs.size() : 0);
        AOPMessageVM vm = new AOPMessageVM();
        try {
            List<Object[]> updates = new ArrayList<>();
            for (STGExtractionLookupDTO dto : stgExtractionLookupDTOs) {
                updates.add(new Object[]{
                        dto.getLoadMW(), dto.getSvhInletTPH(), dto.getSmBleedFlowTPH(),
                        dto.getSlExtFlowTPH(), dto.getCondensingLoadM3Hr(), dto.getHeatRateKcalKWH(),
                        dto.getRemarks(), dto.getId()
                });
            }
            if (!updates.isEmpty()) {
                String sql = "update STGExtractionLookup set LoadMW = ?, SVHInletTPH = ?, SMBleedFlowTPH = ?, SLExtFlowTPH = ?, CondensingLoadM3Hr = ?, HeatRateKcalKWH = ?, Remarks = ? where Id = ?";
                jdbcTemplate.batchUpdate(sql, updates);
            }
            vm.setCode(200);
            vm.setMessage("STG extraction lookup updated successfully. " + updates.size() + " records updated.");
            vm.setData(null);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] updateSTGExtraction error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to update STG extraction lookup: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }

    private STGExtractionLookupDTO mapToSTGExtractionDTO(STGExtractionLookup entity) {
        return STGExtractionLookupDTO.builder()
                .id(entity.getId())
                .loadMW(entity.getLoadMW())
                .svhInletTPH(entity.getSvhInletTPH())
                .smBleedFlowTPH(entity.getSmBleedFlowTPH())
                .slExtFlowTPH(entity.getSlExtFlowTPH())
                .condensingLoadM3Hr(entity.getCondensingLoadM3Hr())
                .heatRateKcalKWH(entity.getHeatRateKcalKWH())
                .remarks(entity.getRemarks())
                .build();
    }

    // ============================================================
    // HRSG HEAT RATE LOOKUP
    // ============================================================

    @Override
    public AOPMessageVM getHRSGHeatRateLookup() {
        logger.info("[JMDHeatRate] getHRSGHeatRateLookup");
        AOPMessageVM vm = new AOPMessageVM();
        try {
            List<HRSGHeatRateLookupDTO> result = hrsgHeatRateLookupRepository.findAllByOrderByHrsgLoadAsc().stream()
                    .map(this::mapToHRSGHeatRateDTO)
                    .toList();
            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(result);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] getHRSGHeatRateLookup error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to fetch HRSG heat rate lookup: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }

    @Override
    public AOPMessageVM getHRSGHeatRateByEquipmentName(String equipmentName) {
        logger.info("[JMDHeatRate] getHRSGHeatRateByEquipmentName - equipmentName: {}", equipmentName);
        AOPMessageVM vm = new AOPMessageVM();
        try {
            List<HRSGHeatRateLookupDTO> result = hrsgHeatRateLookupRepository.findByEquipmentNameOrderByHrsgLoadAsc(equipmentName).stream()
                    .map(this::mapToHRSGHeatRateDTO)
                    .toList();
            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(result);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] getHRSGHeatRateByEquipmentName error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to fetch HRSG heat rate by equipment name: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }

    @Override
    public AOPMessageVM getHRSGHeatRateByCppUtility(String cppUtility) {
        logger.info("[JMDHeatRate] getHRSGHeatRateByCppUtility - cppUtility: {}", cppUtility);
        AOPMessageVM vm = new AOPMessageVM();
        try {
            List<HRSGHeatRateLookupDTO> result = hrsgHeatRateLookupRepository.findByCppUtilityOrderByHrsgLoadAsc(cppUtility).stream()
                    .map(this::mapToHRSGHeatRateDTO)
                    .toList();
            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(result);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] getHRSGHeatRateByCppUtility error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to fetch HRSG heat rate by cpp utility: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }

    @Override
    @Transactional
    public AOPMessageVM updateHRSGHeatRateLookup(List<HRSGHeatRateLookupDTO> hrsgHeatRateLookupDTOs, String aopYear) {
        logger.info("[JMDHeatRate] updateHRSGHeatRateLookup - {} records", hrsgHeatRateLookupDTOs != null ? hrsgHeatRateLookupDTOs.size() : 0);
        AOPMessageVM vm = new AOPMessageVM();
        try {
            List<Object[]> updates = new ArrayList<>();
            for (HRSGHeatRateLookupDTO dto : hrsgHeatRateLookupDTOs) {
                updates.add(new Object[]{dto.getHrsgLoad(), dto.getHeatRate(), dto.getRemarks(), dto.getId()});
            }
            if (!updates.isEmpty()) {
                String sql = "update HRSGHeatRateLookup set HRSGLoad = ?, HeatRate = ?, Remarks = ? where Id = ?";
                jdbcTemplate.batchUpdate(sql, updates);
            }
            vm.setCode(200);
            vm.setMessage("HRSG heat rate lookup updated successfully. " + updates.size() + " records updated.");
            vm.setData(null);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] updateHRSGHeatRateLookup error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to update HRSG heat rate lookup: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }

    private HRSGHeatRateLookupDTO mapToHRSGHeatRateDTO(HRSGHeatRateLookup entity) {
        return HRSGHeatRateLookupDTO.builder()
                .id(entity.getId())
                .equipmentName(entity.getEquipmentName())
                .cppUtility(entity.getCppUtility())
                .hrsgLoad(entity.getHrsgLoad())
                .heatRate(entity.getHeatRate())
                .remarks(entity.getRemarks())
                .build();
    }

    // ============================================================
    // EXPORT METHODS
    // ============================================================

    @Override
    public byte[] exportGTHeatRate(String assetId, String aopYear, String startDate, String endDate) {
        logger.info("[JMDHeatRate] exportGTHeatRate - assetId: {}, aopYear: {}", assetId, aopYear);
        try {
            List<HeatRateDTO> data;
            if (startDate != null && !startDate.trim().isEmpty() && endDate != null && !endDate.trim().isEmpty()) {
                data = getGTHeatRateWithProposed(assetId, aopYear, startDate, endDate);
            } else {
                data = getGTHeatRateByAssetId(assetId, aopYear);
            }
            return generateGTHeatRateExcel(data);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] exportGTHeatRate error: {}", e.getMessage(), e);
            return null;
        }
    }

    private byte[] generateGTHeatRateExcel(List<HeatRateDTO> data) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Heat Rate");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle remarksStyle = createRemarksStyle(workbook);

        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Equipment Type", "CPP Utility", "GT Load", "OEM HR", "PREVIOUS YEAR BUDGET HR", "PROPOSED HR (Based On Actual Data)", "Final HR", "Free Steam Factor", "Remark", "Selected Heat Rate", "Id"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        sheet.setColumnHidden(9, true);
        sheet.setColumnHidden(10, true);

        for (HeatRateDTO dto : data) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;
            Cell cell = row.createCell(colNum++);
            cell.setCellValue(dto.getEquipType() != null ? dto.getEquipType() : "");
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getCppUtility() != null ? dto.getCppUtility() : "");
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getGtLoad() != null ? dto.getGtLoad() : 0.0);
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getOemHeatRate() != null ? dto.getOemHeatRate() : 0.0);
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getPreviousYearHeatRate() != null ? dto.getPreviousYearHeatRate() : 0.0);
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getProposedHeatRate() != null ? dto.getProposedHeatRate() : 0.0);
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getFinalHeatRate() != null ? dto.getFinalHeatRate() : 0.0);
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getFreeSteamFactor() != null ? dto.getFreeSteamFactor() : 0.0);
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
            cell.setCellStyle(remarksStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getSelectedHeatRate() != null ? dto.getSelectedHeatRate() : "");
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getId() != null ? dto.getId().toString() : "");
            cell.setCellStyle(dataStyle);
        }
        for (int i = 0; i < headers.length; i++) {
            if (i == 8) {
                sheet.setColumnWidth(i, 8000);
                continue;
            }
            sheet.autoSizeColumn(i);
            applyHeaderMinWidth(sheet, i, headers[i]);
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    @Override
    public byte[] exportSTGHeatRate(String assetId, String aopYear, String startDate, String endDate, List<UUID> plantIds) {
        logger.info("[JMDHeatRate] exportSTGHeatRate - assetId: {}, aopYear: {}, plantIds: {}", assetId, aopYear, plantIds);
        try {
            List<STGHeatRateDTO> data;
            if (startDate != null && !startDate.trim().isEmpty() && endDate != null && !endDate.trim().isEmpty()) {
                data = getSTGHeatRateWithProposed(assetId, aopYear, startDate, endDate, plantIds);
            } else {
                data = getSTGHeatRateByAssetId(assetId, aopYear);
            }
            return generateSTGHeatRateExcel(data);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] exportSTGHeatRate error: {}", e.getMessage(), e);
            return null;
        }
    }

    private byte[] generateSTGHeatRateExcel(List<STGHeatRateDTO> data) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("STG Heat Rate");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle remarksStyle = createRemarksStyle(workbook);

        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Equipment Type", "CPP Utility", "STG Load", "OEM HR", "PREVIOUS YEAR BUDGET HR", "PROPOSED HR (Based On Actual Data)", "Final HR", "Remark", "Selected Heat Rate", "Id"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        sheet.setColumnHidden(8, true);
        sheet.setColumnHidden(9, true);

        for (STGHeatRateDTO dto : data) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;
            Cell cell = row.createCell(colNum++);
            cell.setCellValue(dto.getEquipType() != null ? dto.getEquipType() : "");
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getCppUtility() != null ? dto.getCppUtility() : "");
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getStgLoad() != null ? dto.getStgLoad() : 0.0);
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getOemHeatRate() != null ? dto.getOemHeatRate() : 0.0);
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getPreviousYearHeatRate() != null ? dto.getPreviousYearHeatRate() : 0.0);
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getProposedHeatRate() != null ? dto.getProposedHeatRate() : 0.0);
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getFinalHeatRate() != null ? dto.getFinalHeatRate() : 0.0);
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
            cell.setCellStyle(remarksStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getSelectedHeatRate() != null ? dto.getSelectedHeatRate() : "");
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getId() != null ? dto.getId().toString() : "");
            cell.setCellStyle(dataStyle);
        }
        for (int i = 0; i < headers.length; i++) {
            if (i == 7) {
                sheet.setColumnWidth(i, 8000);
                continue;
            }
            sheet.autoSizeColumn(i);
            applyHeaderMinWidth(sheet, i, headers[i]);
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    @Override
    public byte[] exportHRSGHeatRate(String assetId, String aopYear, String startDate, String endDate) {
        logger.info("[JMDHeatRate] exportHRSGHeatRate - assetId: {}, aopYear: {}", assetId, aopYear);
        try {
            List<com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateDTO> data;
            if (startDate != null && !startDate.trim().isEmpty() && endDate != null && !endDate.trim().isEmpty()) {
                data = getHRSGHeatRateWithProposed(assetId, aopYear, startDate, endDate);
            } else {
                data = getHRSGHeatRateByAssetId(assetId, aopYear);
            }
            return generateHRSGHeatRateExcel(data);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] exportHRSGHeatRate error: {}", e.getMessage(), e);
            return null;
        }
    }

    private byte[] generateHRSGHeatRateExcel(List<com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateDTO> data) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("HRSG Heat Rate");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle remarksStyle = createRemarksStyle(workbook);

        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Equipment Type", "CPP Utility", "HRSG Load", "OEM HR", "PREVIOUS YEAR BUDGET HR", "PROPOSED HR (Based On Actual Data)", "Final HR", "Remark", "Selected Heat Rate", "Id"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        sheet.setColumnHidden(8, true);
        sheet.setColumnHidden(9, true);

        for (com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateDTO dto : data) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;
            Cell cell = row.createCell(colNum++);
            cell.setCellValue(dto.getEquipType() != null ? dto.getEquipType() : "");
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getCppUtility() != null ? dto.getCppUtility() : "");
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getHrsgLoad() != null ? dto.getHrsgLoad() : 0.0);
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getOemHeatRate() != null ? dto.getOemHeatRate() : 0.0);
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getPreviousYearHeatRate() != null ? dto.getPreviousYearHeatRate() : 0.0);
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getProposedHeatRate() != null ? dto.getProposedHeatRate() : 0.0);
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getFinalHeatRate() != null ? dto.getFinalHeatRate() : 0.0);
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
            cell.setCellStyle(remarksStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getSelectedHeatRate() != null ? dto.getSelectedHeatRate() : "");
            cell.setCellStyle(dataStyle);
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getId() != null ? dto.getId().toString() : "");
            cell.setCellStyle(dataStyle);
        }
        for (int i = 0; i < headers.length; i++) {
            if (i == 7) {
                sheet.setColumnWidth(i, 8000);
                continue;
            }
            sheet.autoSizeColumn(i);
            applyHeaderMinWidth(sheet, i, headers[i]);
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    @Override
    public byte[] exportHRSGHeatRateLookup() {
        logger.info("[JMDHeatRate] exportHRSGHeatRateLookup");
        try {
            List<HRSGHeatRateLookupDTO> data = hrsgHeatRateLookupRepository.findAllByOrderByHrsgLoadAsc().stream()
                    .map(this::mapToHRSGHeatRateDTO)
                    .toList();
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("HRSG Heat Rate Lookup");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle remarksStyle = createRemarksStyle(workbook);

            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"Equipment Type", "CPP Utility", "HRSG Load", "Heat Rate", "Remarks", "Id"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            sheet.setColumnHidden(5, true);

            for (HRSGHeatRateLookupDTO dto : data) {
                Row row = sheet.createRow(rowNum++);
                int colNum = 0;
                Cell cell = row.createCell(colNum++);
                cell.setCellValue(dto.getEquipmentName() != null ? dto.getEquipmentName() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(colNum++);
                cell.setCellValue(dto.getCppUtility() != null ? dto.getCppUtility() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(colNum++);
                cell.setCellValue(dto.getHrsgLoad() != null ? dto.getHrsgLoad().doubleValue() : 0.0);
                cell.setCellStyle(dataStyle);
                cell = row.createCell(colNum++);
                cell.setCellValue(dto.getHeatRate() != null ? dto.getHeatRate().doubleValue() : 0.0);
                cell.setCellStyle(dataStyle);
                cell = row.createCell(colNum++);
                cell.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
                cell.setCellStyle(remarksStyle);
                cell = row.createCell(colNum++);
                cell.setCellValue(dto.getId() != null ? dto.getId().toString() : "");
                cell.setCellStyle(dataStyle);
            }
            for (int i = 0; i < headers.length; i++) {
                if (i == 4) {
                    sheet.setColumnWidth(i, 8000);
                    continue;
                }
                sheet.autoSizeColumn(i);
                applyHeaderMinWidth(sheet, i, headers[i]);
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            logger.error("[JMDHeatRate] exportHRSGHeatRateLookup error: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public byte[] exportSTGExtractionLookup() {
        logger.info("[JMDHeatRate] exportSTGExtractionLookup");
        try {
            List<STGExtractionLookupDTO> data = stgExtractionLookupRepository.findAllByOrderByLoadMWAsc().stream()
                    .map(this::mapToSTGExtractionDTO)
                    .toList();
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("STG Extraction Lookup");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle remarksStyle = createRemarksStyle(workbook);

            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"Load (MW)", "SVH Inlet (TPH)", "SM Bleed Flow (TPH)", "SL Ext Flow (TPH)",
                    "Condensing Load (M3/Hr)", "Heat Rate (Kcal/KWH)", "Remarks", "Id"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            sheet.setColumnHidden(7, true);

            for (STGExtractionLookupDTO dto : data) {
                Row row = sheet.createRow(rowNum++);
                int colNum = 0;
                Cell cell = row.createCell(colNum++);
                cell.setCellValue(dto.getLoadMW() != null ? dto.getLoadMW().doubleValue() : 0.0);
                cell.setCellStyle(dataStyle);
                cell = row.createCell(colNum++);
                cell.setCellValue(dto.getSvhInletTPH() != null ? dto.getSvhInletTPH().doubleValue() : 0.0);
                cell.setCellStyle(dataStyle);
                cell = row.createCell(colNum++);
                cell.setCellValue(dto.getSmBleedFlowTPH() != null ? dto.getSmBleedFlowTPH().doubleValue() : 0.0);
                cell.setCellStyle(dataStyle);
                cell = row.createCell(colNum++);
                cell.setCellValue(dto.getSlExtFlowTPH() != null ? dto.getSlExtFlowTPH().doubleValue() : 0.0);
                cell.setCellStyle(dataStyle);
                cell = row.createCell(colNum++);
                cell.setCellValue(dto.getCondensingLoadM3Hr() != null ? dto.getCondensingLoadM3Hr().doubleValue() : 0.0);
                cell.setCellStyle(dataStyle);
                cell = row.createCell(colNum++);
                cell.setCellValue(dto.getHeatRateKcalKWH() != null ? dto.getHeatRateKcalKWH().doubleValue() : 0.0);
                cell.setCellStyle(dataStyle);
                cell = row.createCell(colNum++);
                cell.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
                cell.setCellStyle(remarksStyle);
                cell = row.createCell(colNum++);
                cell.setCellValue(dto.getId() != null ? dto.getId().toString() : "");
                cell.setCellStyle(dataStyle);
            }
            for (int i = 0; i < headers.length; i++) {
                if (i == 6) {
                    sheet.setColumnWidth(i, 8000);
                    continue;
                }
                sheet.autoSizeColumn(i);
                applyHeaderMinWidth(sheet, i, headers[i]);
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            logger.error("[JMDHeatRate] exportSTGExtractionLookup error: {}", e.getMessage(), e);
            return null;
        }
    }

    // ============================================================
    // IMPORT METHODS
    // ============================================================

    @Override
    @Transactional
    public AOPMessageVM importGTHeatRate(MultipartFile file) {
        logger.info("[JMDHeatRate] importGTHeatRate - file: {}", file.getOriginalFilename());
        AOPMessageVM vm = new AOPMessageVM();
        try {
            List<HeatRateDTO> dtos = new ArrayList<>();
            try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
                Sheet sheet = workbook.getSheetAt(0);
                int totalRows = sheet.getLastRowNum();
                for (int i = 1; i <= totalRows; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    HeatRateDTO dto = new HeatRateDTO();
                    String idStr = getCellValueAsString(row, 10);
                    if (idStr != null && !idStr.isEmpty()) {
                        dto.setId(UUID.fromString(idStr));
                    }
                    dto.setEquipType(getCellValueAsString(row, 0));
                    dto.setCppUtility(getCellValueAsString(row, 1));
                    dto.setGtLoad(getCellValueAsDouble(row, 2));
                    dto.setOemHeatRate(getCellValueAsDouble(row, 3));
                    dto.setPreviousYearHeatRate(getCellValueAsDouble(row, 4));
                    dto.setFinalHeatRate(getCellValueAsDouble(row, 6));
                    dto.setFreeSteamFactor(getCellValueAsDouble(row, 7));
                    dto.setRemarks(getCellValueAsString(row, 8));
                    dto.setSelectedHeatRate(getCellValueAsString(row, 9));
                    dtos.add(dto);
                }
            }
            if (!dtos.isEmpty()) {
                updateGTHeatRate(dtos, null);
            }
            vm.setCode(200);
            vm.setMessage("GT heat rate imported successfully. " + dtos.size() + " records processed.");
            vm.setData(null);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] importGTHeatRate error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to import GT heat rate: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }

    @Override
    @Transactional
    public AOPMessageVM importSTGHeatRate(MultipartFile file) {
        logger.info("[JMDHeatRate] importSTGHeatRate - file: {}", file.getOriginalFilename());
        AOPMessageVM vm = new AOPMessageVM();
        try {
            List<STGHeatRateDTO> dtos = new ArrayList<>();
            try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
                Sheet sheet = workbook.getSheetAt(0);
                int totalRows = sheet.getLastRowNum();
                for (int i = 1; i <= totalRows; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    STGHeatRateDTO dto = new STGHeatRateDTO();
                    String idStr = getCellValueAsString(row, 9);
                    if (idStr != null && !idStr.isEmpty()) {
                        dto.setId(idStr);
                    }
                    dto.setEquipType(getCellValueAsString(row, 0));
                    dto.setCppUtility(getCellValueAsString(row, 1));
                    dto.setStgLoad(getCellValueAsDouble(row, 2));
                    dto.setOemHeatRate(getCellValueAsDouble(row, 3));
                    dto.setPreviousYearHeatRate(getCellValueAsDouble(row, 4));
                    dto.setFinalHeatRate(getCellValueAsDouble(row, 6));
                    dto.setRemarks(getCellValueAsString(row, 7));
                    dto.setSelectedHeatRate(getCellValueAsString(row, 8));
                    dtos.add(dto);
                }
            }
            if (!dtos.isEmpty()) {
                updateSTGHeatRate(dtos, null);
            }
            vm.setCode(200);
            vm.setMessage("STG heat rate imported successfully. " + dtos.size() + " records processed.");
            vm.setData(null);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] importSTGHeatRate error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to import STG heat rate: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }

    @Override
    @Transactional
    public AOPMessageVM importHRSGHeatRate(MultipartFile file) {
        logger.info("[JMDHeatRate] importHRSGHeatRate - file: {}", file.getOriginalFilename());
        AOPMessageVM vm = new AOPMessageVM();
        try {
            List<com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateDTO> dtos = new ArrayList<>();
            try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
                Sheet sheet = workbook.getSheetAt(0);
                int totalRows = sheet.getLastRowNum();
                for (int i = 1; i <= totalRows; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateDTO dto = new com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateDTO();
                    String idStr = getCellValueAsString(row, 9);
                    if (idStr != null && !idStr.isEmpty()) {
                        dto.setId(UUID.fromString(idStr));
                    }
                    dto.setEquipType(getCellValueAsString(row, 0));
                    dto.setCppUtility(getCellValueAsString(row, 1));
                    dto.setHrsgLoad(getCellValueAsDouble(row, 2));
                    dto.setOemHeatRate(getCellValueAsDouble(row, 3));
                    dto.setPreviousYearHeatRate(getCellValueAsDouble(row, 4));
                    dto.setFinalHeatRate(getCellValueAsDouble(row, 6));
                    dto.setRemarks(getCellValueAsString(row, 7));
                    dto.setSelectedHeatRate(getCellValueAsString(row, 8));
                    dtos.add(dto);
                }
            }
            if (!dtos.isEmpty()) {
                updateHRSGHeatRate(dtos, null);
            }
            vm.setCode(200);
            vm.setMessage("HRSG heat rate imported successfully. " + dtos.size() + " records processed.");
            vm.setData(null);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] importHRSGHeatRate error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to import HRSG heat rate: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }

    @Override
    @Transactional
    public AOPMessageVM importHRSGHeatRateLookup(MultipartFile file) {
        logger.info("[JMDHeatRate] importHRSGHeatRateLookup - file: {}", file.getOriginalFilename());
        AOPMessageVM vm = new AOPMessageVM();
        try {
            List<HRSGHeatRateLookupDTO> dtos = new ArrayList<>();
            try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
                Sheet sheet = workbook.getSheetAt(0);
                int totalRows = sheet.getLastRowNum();
                for (int i = 1; i <= totalRows; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    HRSGHeatRateLookupDTO dto = new HRSGHeatRateLookupDTO();
                    String idStr = getCellValueAsString(row, 5);
                    if (idStr != null && !idStr.isEmpty()) {
                        dto.setId(UUID.fromString(idStr));
                    }
                    dto.setEquipmentName(getCellValueAsString(row, 0));
                    dto.setCppUtility(getCellValueAsString(row, 1));
                    dto.setHrsgLoad(getCellValueAsBigDecimal(row, 2));
                    dto.setHeatRate(getCellValueAsBigDecimal(row, 3));
                    dto.setRemarks(getCellValueAsString(row, 4));
                    dtos.add(dto);
                }
            }
            if (!dtos.isEmpty()) {
                updateHRSGHeatRateLookup(dtos, null);
            }
            vm.setCode(200);
            vm.setMessage("HRSG heat rate lookup imported successfully. " + dtos.size() + " records processed.");
            vm.setData(null);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] importHRSGHeatRateLookup error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to import HRSG heat rate lookup: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }

    @Override
    @Transactional
    public AOPMessageVM importSTGExtractionLookup(MultipartFile file) {
        logger.info("[JMDHeatRate] importSTGExtractionLookup - file: {}", file.getOriginalFilename());
        AOPMessageVM vm = new AOPMessageVM();
        try {
            List<STGExtractionLookupDTO> dtos = new ArrayList<>();
            try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
                Sheet sheet = workbook.getSheetAt(0);
                int totalRows = sheet.getLastRowNum();
                for (int i = 1; i <= totalRows; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    STGExtractionLookupDTO dto = new STGExtractionLookupDTO();
                    String idStr = getCellValueAsString(row, 7);
                    if (idStr != null && !idStr.isEmpty()) {
                        dto.setId(UUID.fromString(idStr));
                    }
                    dto.setLoadMW(getCellValueAsBigDecimal(row, 0));
                    dto.setSvhInletTPH(getCellValueAsBigDecimal(row, 1));
                    dto.setSmBleedFlowTPH(getCellValueAsBigDecimal(row, 2));
                    dto.setSlExtFlowTPH(getCellValueAsBigDecimal(row, 3));
                    dto.setCondensingLoadM3Hr(getCellValueAsBigDecimal(row, 4));
                    dto.setHeatRateKcalKWH(getCellValueAsBigDecimal(row, 5));
                    dto.setRemarks(getCellValueAsString(row, 6));
                    dtos.add(dto);
                }
            }
            if (!dtos.isEmpty()) {
                updateSTGExtraction(dtos, null);
            }
            vm.setCode(200);
            vm.setMessage("STG extraction lookup imported successfully. " + dtos.size() + " records processed.");
            vm.setData(null);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] importSTGExtractionLookup error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to import STG extraction lookup: " + e.getMessage());
            vm.setData(null);
        }
        return vm;
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private String calculatePreviousFinancialYear(String financialYear) {
        if (financialYear == null || !financialYear.contains("-")) {
            throw new IllegalArgumentException("Invalid financial year format. Expected format: YYYY-YY");
        }
        String[] parts = financialYear.split("-");
        int startYear = Integer.parseInt(parts[0]);
        int endYear = Integer.parseInt(parts[1]);
        int prevStartYear = startYear - 1;
        int prevEndYear = endYear - 1;
        return prevStartYear + "-" + String.format("%02d", prevEndYear);
    }

    private String getCellValueAsString(Row row, int cellIndex) {
        if (row.getCell(cellIndex) == null) {
            return null;
        }
        try {
            DataFormatter formatter = new DataFormatter();
            String value = formatter.formatCellValue(row.getCell(cellIndex));
            return value != null && !value.trim().isEmpty() ? value.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal getCellValueAsBigDecimal(Row row, int cellIndex) {
        if (row.getCell(cellIndex) == null) {
            return null;
        }
        try {
            switch (row.getCell(cellIndex).getCellType()) {
                case NUMERIC:
                    return BigDecimal.valueOf(row.getCell(cellIndex).getNumericCellValue());
                case STRING:
                    String strValue = row.getCell(cellIndex).getStringCellValue().trim();
                    if (strValue.isEmpty()) {
                        return null;
                    }
                    return new BigDecimal(strValue);
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private Double getCellValueAsDouble(Row row, int cellIndex) {
        if (row.getCell(cellIndex) == null) {
            return null;
        }
        try {
            switch (row.getCell(cellIndex).getCellType()) {
                case NUMERIC:
                    return row.getCell(cellIndex).getNumericCellValue();
                case STRING:
                    String strValue = row.getCell(cellIndex).getStringCellValue().trim();
                    if (strValue.isEmpty()) {
                        return null;
                    }
                    return Double.parseDouble(strValue);
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
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
        style.setWrapText(true);
        return style;
    }

    private void applyHeaderMinWidth(Sheet sheet, int col, String headerText) {
        if (headerText == null || headerText.isBlank()) {
            return;
        }
        int headerWidth = Math.min(255 * 256, (headerText.length() + 2) * 256);
        if (sheet.getColumnWidth(col) < headerWidth) {
            sheet.setColumnWidth(col, headerWidth);
        }
    }
}
