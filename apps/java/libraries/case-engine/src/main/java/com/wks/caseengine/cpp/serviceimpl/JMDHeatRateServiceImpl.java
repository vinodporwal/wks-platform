package com.wks.caseengine.cpp.serviceimpl;

import com.wks.caseengine.cpp.dto.heatrate.CppGtHeatRateDto;
import com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateLookupDTO;
import com.wks.caseengine.cpp.dto.heatrate.HeatRateDTO;
import com.wks.caseengine.cpp.dto.heatrate.HeatRateProjection;
import com.wks.caseengine.cpp.dto.heatrate.PowerGenerationAssetDto;
import com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateProjection;
import com.wks.caseengine.cpp.dto.heatrate.STGHeatRateProjection;
import com.wks.caseengine.cpp.dto.heatrate.SelectedHeatRateType;
import com.wks.caseengine.cpp.dto.heatrate.STGHeatRateDTO;
import com.wks.caseengine.cpp.dto.heatrate.STGExtractionLookupDTO;
import com.wks.caseengine.cpp.entity.CppGtHeatRate;
import com.wks.caseengine.cpp.entity.HRSGHeatRateLookup;
import com.wks.caseengine.cpp.entity.PowerGenerationAsset;
import com.wks.caseengine.cpp.entity.STGExtractionLookup;
import com.wks.caseengine.cpp.repository.CppGtHeatRateRepository;
import com.wks.caseengine.cpp.repository.JMDHRSGHeatRateLookupRepository;
import com.wks.caseengine.cpp.repository.JMDHeatRateRepository;
import com.wks.caseengine.cpp.repository.JMDSTGExtractionLookupRepository;
import com.wks.caseengine.cpp.repository.PowerGenerationAssetRepository;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
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
    private CppGtHeatRateRepository cppGtHeatRateRepository;

    // ============================================================
    // DROPDOWN METHODS (multi-plant)
    // ============================================================

    @Override
    public AOPMessageVM getGTAssetDropdown(List<UUID> plantIds) {
        logger.info("[JMDHeatRate] getGTAssetDropdown - plantIds: {}", plantIds);
        AOPMessageVM vm = new AOPMessageVM();
        
        try {
            String assetType = "GT";
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
    public AOPMessageVM getGTHeatRateData(UUID assetId, String year, String startDate, String endDate, String assetName, List<UUID> plantIds) {
        logger.info("[JMDHeatRate] getGTHeatRateData - assetId: {}, year: {}, startDate: {}, endDate: {}, assessmentName: {}, plantIds: {}", 
                assetId, year, startDate, endDate, assetName, plantIds);
        
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
            
            List<CppGtHeatRate> prevEntities = new java.util.ArrayList<>();
            if (prevYear != null) {
                prevEntities = cppGtHeatRateRepository.findByAssetFkIdAndFinancialYearNative(assetId, prevYear);
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

            List<Object[]> spResultList = cppGtHeatRateRepository.executeCalculateCommonGTHeatRateSP(
                    startDate, endDate, assetName, plantIdsStr
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
            
            List<CppGtHeatRateDto> resultList = new java.util.ArrayList<>();
            for (CppGtHeatRate entity : entities) {
                if (entity != null) {
                    CppGtHeatRateDto dto = new CppGtHeatRateDto();

                    dto.setId(entity.getId() != null ? entity.getId() : null);
                    dto.setAssetFkId(entity.getAssetFkId() != null ? entity.getAssetFkId() : null);
                    dto.setEquipType(entity.getAssetName() != null ? entity.getAssetName() : null);
                    dto.setCppUtility(entity.getUtilityId() != null ? entity.getUtilityId() : null);
                    dto.setFinancialYear(entity.getFinancialYear() != null ? entity.getFinancialYear() : null);
                    dto.setGtLoad(entity.getGtLoad() != null ? entity.getGtLoad() : null);
                    dto.setFreeSteamFactor(entity.getFreeSteamFactor() != null ? entity.getFreeSteamFactor() : null);
                    dto.setRemarks(entity.getRemarks() != null ? entity.getRemarks() : "");
                    dto.setCreatedDate(entity.getCreatedDate() != null ? entity.getCreatedDate() : null);
                    dto.setUpdatedDate(entity.getUpdatedDate() != null ? entity.getUpdatedDate() : null);
                    dto.setFinalHeatRate(entity.getFinalHeatRate() != null ? entity.getFinalHeatRate() : null);
                    dto.setOemHeatRate(entity.getOemHeatRate() != null ? entity.getOemHeatRate() : null);
                    dto.setSelectedHeatRate(entity.getSelectedHeatRate() != null ? entity.getSelectedHeatRate() : "");
                   
                    if (entity.getGtLoad() != null && prevYearHeatRateMap.containsKey(entity.getGtLoad())) {
                        dto.setPrevYearFinalHeatRate(prevYearHeatRateMap.get(entity.getGtLoad()));
                    } else {
                        dto.setPrevYearFinalHeatRate(null); 
                    }

                    if (entity.getGtLoad() != null && proposedHeatRateMap.containsKey(entity.getGtLoad())) {
                        dto.setProposedYearFinalHeatRate(proposedHeatRateMap.get(entity.getGtLoad()));
                    } else {
                        dto.setProposedYearFinalHeatRate(null);
                    }

                    resultList.add(dto);
                }
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

            List<CppGtHeatRate> entitiesToSave = new java.util.ArrayList<>();

            for (CppGtHeatRateDto dto : dtoList) {
                if (dto != null) {
                    CppGtHeatRate entity = new CppGtHeatRate();

                    entity.setId(dto.getId());
                    entity.setAssetFkId(dto.getAssetFkId());
                    entity.setAssetName(dto.getEquipType());
                    entity.setUtilityId(dto.getCppUtility());
                    entity.setGtLoad(dto.getGtLoad());
                    entity.setFreeSteamFactor(dto.getFreeSteamFactor());
                    entity.setRemarks(dto.getRemarks());
                    entity.setFinalHeatRate(dto.getFinalHeatRate());
                    entity.setOemHeatRate(dto.getOemHeatRate());
                    entity.setSelectedHeatRate(dto.getSelectedHeatRate());

                    entitiesToSave.add(entity);
                }
            }

            
            List<CppGtHeatRate> savedEntities = cppGtHeatRateRepository.saveAll(entitiesToSave);
            
            logger.info("[JMDHeatRate] saveGTHeatRateData - successfully saved {} records", savedEntities.size());
            
            vm.setCode(200);
            vm.setMessage("Data saved successfully");
            vm.setData(null); 
            
        } catch (Exception e) {
            logger.error("[JMDHeatRate] saveGTHeatRateData error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Failed to save GT heat rate data: " + e.getMessage());
            vm.setData(null);
        }
        
        return vm;
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
    public AOPMessageVM getSTGHeatRate(String aopYear, String startDate, String endDate) {
        logger.info("[JMDHeatRate] getSTGHeatRate - aopYear: {}, startDate: {}, endDate: {}", aopYear, startDate, endDate);
        AOPMessageVM vm = new AOPMessageVM();
        try {
            List<STGHeatRateDTO> result;
            if (startDate != null && !startDate.trim().isEmpty() && endDate != null && !endDate.trim().isEmpty()) {
                result = getSTGHeatRateWithProposed(aopYear, startDate, endDate);
            } else {
                result = getSTGHeatRateByFinancialYear(aopYear);
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

    private List<STGHeatRateDTO> getSTGHeatRateByFinancialYear(String financialYear) {
        String previousFinancialYear = calculatePreviousFinancialYear(financialYear);
        List<STGHeatRateProjection> projections = heatRateRepository.findStgHeatRateByFinancialYear(financialYear, previousFinancialYear);
        return projections.stream()
                .map(projection -> {
                    STGHeatRateDTO dto = new STGHeatRateDTO();
                    dto.setId(projection.getId());
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

    private List<STGHeatRateDTO> getSTGHeatRateWithProposed(String financialYear, String startDate, String endDate) {
        List<STGHeatRateDTO> dtos = getSTGHeatRateByFinancialYear(financialYear);
        if (startDate != null && !startDate.trim().isEmpty() && endDate != null && !endDate.trim().isEmpty()) {
            Map<Double, Double> proposedHeatRateMap = calculateProposedSTGHeatRates(startDate, endDate);
            for (STGHeatRateDTO dto : dtos) {
                Double proposedHeatRate = proposedHeatRateMap.get(dto.getStgLoad());
                if (proposedHeatRate != null) {
                    dto.setProposedHeatRate(proposedHeatRate);
                }
            }
        }
        return dtos;
    }

    private Map<Double, Double> calculateProposedSTGHeatRates(String startDate, String endDate) {
        String sql = "EXEC CPP_CalculateSTGHeatRate_ByDateRange @StartDate = ?, @EndDate = ?";
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
                    startDate, endDate);
        } catch (Exception e) {
            logger.error("[JMDHeatRate] Error calling STG stored procedure: {}", e.getMessage(), e);
        }
        return proposedHeatRateMap;
    }

    @Override
    @Transactional
    public AOPMessageVM updateSTGHeatRate(List<STGHeatRateDTO> stgHeatRateDTOs, String aopYear) {
        logger.info("[JMDHeatRate] updateSTGHeatRate - {} records, aopYear: {}", stgHeatRateDTOs != null ? stgHeatRateDTOs.size() : 0, aopYear);
        AOPMessageVM vm = new AOPMessageVM();
        try {
            List<Object[]> updates = new ArrayList<>();
            for (STGHeatRateDTO dto : stgHeatRateDTOs) {
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
                        dto.getStgLoad(),
                        dto.getFinalHeatRate(),
                        dto.getOemHeatRate(),
                        selectedHeatRate,
                        dto.getRemarks(),
                        dto.getId()
                });
            }
            if (!updates.isEmpty()) {
                String sql = "UPDATE CPP_STGHeatRate SET STGLoad = ?, FinalHeatRate = ?, OEMHeatRate = ?, SelectedHeatRate = ?, Remarks = ?, UpdatedDate = GETDATE() WHERE Id = ?";
                jdbcTemplate.batchUpdate(sql, updates);
            }
            vm.setCode(200);
            vm.setMessage("STG heat rate updated successfully. " + updates.size() + " records updated.");
            vm.setData(null);
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
    public byte[] exportSTGHeatRate(String aopYear, String startDate, String endDate) {
        logger.info("[JMDHeatRate] exportSTGHeatRate - aopYear: {}", aopYear);
        try {
            List<STGHeatRateDTO> data;
            if (startDate != null && !startDate.trim().isEmpty() && endDate != null && !endDate.trim().isEmpty()) {
                data = getSTGHeatRateWithProposed(aopYear, startDate, endDate);
            } else {
                data = getSTGHeatRateByFinancialYear(aopYear);
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
                        dto.setId(UUID.fromString(idStr));
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
