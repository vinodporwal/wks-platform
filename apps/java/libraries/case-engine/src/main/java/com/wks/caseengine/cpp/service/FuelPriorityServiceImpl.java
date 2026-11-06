package com.wks.caseengine.cpp.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.dto.AssetFuelPriorityDto;
import com.wks.caseengine.cpp.dto.AssetFuelPriorityProjection;
import com.wks.caseengine.cpp.dto.CompatibleFuelAssetDto;
import com.wks.caseengine.cpp.dto.CompatibleFuelAssetProjection;
import com.wks.caseengine.cpp.dto.FuelMasterWithCategoryDTO;
import com.wks.caseengine.cpp.dto.FuelWithCategoryProjection;
import com.wks.caseengine.cpp.dto.PlantFuelAvailabilityMonthlyDto;
import com.wks.caseengine.cpp.dto.PlantFuelAvailabilityMonthlyProjection;
import com.wks.caseengine.cpp.dto.PlantWiseFuelPriorityDto;
import com.wks.caseengine.cpp.dto.PlantWiseFuelPriorityProjection;
import com.wks.caseengine.cpp.entity.CPPAssetFuelPriority;
import com.wks.caseengine.cpp.entity.CPPPlantFuelAvailabilityMonthly;
import com.wks.caseengine.cpp.repository.CPPAssetFuelPriorityRepository;
import com.wks.caseengine.cpp.repository.CPPPlantFuelAvailabilityMonthlyRepository;
import com.wks.caseengine.cpp.repository.FuelPriorityRepository;
import com.wks.caseengine.cpp.utility.ExcelCells;
import com.wks.caseengine.cpp.utility.ExcelColumns;
import com.wks.caseengine.cpp.utility.ExcelRows;
import com.wks.caseengine.cpp.utility.ExcelStyles;
import com.wks.caseengine.cpp.utility.FiscalYearMonths;
import com.wks.caseengine.message.vm.AOPMessageVM;

@Service
public class FuelPriorityServiceImpl implements FuelPriorityService {

    private static final Logger logger = LoggerFactory.getLogger(FuelPriorityServiceImpl.class);

    @Autowired
    private FuelPriorityRepository repository;

    @Autowired
    private CPPAssetFuelPriorityRepository assetFuelPriorityRepository;

    @Autowired
    private CPPPlantFuelAvailabilityMonthlyRepository monthlyRepository;

    @Autowired
    @Qualifier("db1JdbcTemplate")
    private JdbcTemplate db1JdbcTemplate;

    private record FuelMasterDetails(
        UUID id,
        String fuelName,
        String fuelDisplayName,
        String type,
        UUID categoryFkId,
        String fuelCode,
        String uom,
        String description,
        BigDecimal calorificValue
    ) {}

    private record NormParameterKey(
        UUID procurementPlantId,
        String fuelCode,
        UUID normParameterTypeId,
        int normTypeId
    ) {}

    public record ResolvedNormParameter(
        UUID id,
        String name,
        String uom,
        String sapMaterialCode,
        UUID plantFkId
    ) {}

    private record NormsHeaderKey(
        UUID assetFkId,
        UUID normParameterFkId,
        boolean isFreeSteam
    ) {}

    private record NormsHeaderBaseRow(
        String plantFkId,
        String utilityName,
        String utilityId,
        String utilityUOM,
        Integer displayOrder,
        String plantCode,
        String utilityNormParameterFkId,
        String utilityActivity
    ) {}

    @Override
    public List<FuelMasterWithCategoryDTO> getFuelMaster() {
        return repository.getFuelMaster().stream().map(this::toFuelMasterDto).toList();
    }

    @Override
    public List<PlantWiseFuelPriorityDto> getPlantWiseFuelPriority(String plantIds, String financialYear) {
        return repository.getPlantWiseFuelPriority(plantIds, financialYear)
            .stream().map(this::toPlantWiseDto).toList();
    }

    private FuelMasterWithCategoryDTO toFuelMasterDto(FuelWithCategoryProjection p) {
        FuelMasterWithCategoryDTO dto = new FuelMasterWithCategoryDTO();
        if (p.getId() != null) {
            dto.setId(UUID.fromString(p.getId()));
        }
        dto.setFuelCode(p.getFuelCode());
        dto.setFuelName(p.getFuelName());
        dto.setFuelDisplayName(p.getFuelDisplayName());
        dto.setType(p.getType());
        dto.setUom(p.getUom());
        dto.setFuelDescription(p.getFuelDescription());
        if (p.getCategoryFkId() != null) {
            dto.setCategoryFkId(UUID.fromString(p.getCategoryFkId()));
        }
        dto.setCategoryName(p.getCategoryName());
        dto.setCategoryDisplayName(p.getCategoryDisplayName());
        return dto;
    }

    @Override
    public AOPMessageVM updatePlantFuelAvailability(List<PlantWiseFuelPriorityDto> payload) {
        AOPMessageVM response = new AOPMessageVM();
        int updated = 0;
        int skipped = 0;

        if (payload == null || payload.isEmpty()) {
            response.setCode(400);
            response.setMessage("Payload is empty");
            return response;
        }

        for (PlantWiseFuelPriorityDto dto : payload) {
            if (dto.getId() == null) {
                skipped++;
                continue;
            }
            if (dto.getFuelFkId() == null) {
                skipped++;
                continue;
            }

            int rows = repository.updatePlantFuelAvailability(
                    dto.getId().toString(),
                    dto.getFuelFkId().toString(),
                    dto.getPriority(),
                    dto.getQuantity(),
                    dto.getRemarks());

            if (rows > 0) {
                updated++;
            } else {
                skipped++;
            }
        }

        response.setCode(200);
        response.setMessage("Plant fuel availability updated. Updated: " + updated + ", Skipped: " + skipped);
        return response;
    }

    private PlantWiseFuelPriorityDto toPlantWiseDto(PlantWiseFuelPriorityProjection p) {
        logger.info("Projection Data: Id={}, PlantName={}, FuelName={}, FuelDisplayName={}, FuelFkId={}, Priority={}, Quantity={}, Remarks={}, AopYear={}",
                p.getId(), p.getPlantName(), p.getFuelName(), p.getFuelDisplayName(), p.getFuelFkId(), p.getPriority(), p.getQuantity(), p.getRemarks(), p.getAopYear());
        PlantWiseFuelPriorityDto dto = new PlantWiseFuelPriorityDto();
        if (p.getId() != null) {
            dto.setId(UUID.fromString(p.getId()));
        }
        dto.setPlantName(p.getPlantName());
        dto.setFuelName(p.getFuelName());
        dto.setFuelDisplayName(p.getFuelDisplayName());
        if (p.getFuelFkId() != null) {
            dto.setFuelFkId(UUID.fromString(p.getFuelFkId()));
        }
        dto.setPriority(p.getPriority());
        dto.setQuantity(p.getQuantity());
        dto.setRemarks(p.getRemarks());
        dto.setAopYear(p.getAopYear());
        return dto;
    }

    @Override
    public List<PlantFuelAvailabilityMonthlyDto> getPlantFuelAvailabilityMonthly(String plantIds, String financialYear) {
        return monthlyRepository.getPlantFuelAvailabilityMonthly(plantIds, financialYear)
                .stream().map(this::toMonthlyDto).toList();
    }

    private PlantFuelAvailabilityMonthlyDto toMonthlyDto(PlantFuelAvailabilityMonthlyProjection p) {
        PlantFuelAvailabilityMonthlyDto dto = new PlantFuelAvailabilityMonthlyDto();
        if (p.getId() != null) dto.setId(UUID.fromString(p.getId()));
        dto.setPlantName(p.getPlantName());
        if (p.getPlantFkId() != null) dto.setPlantFkId(UUID.fromString(p.getPlantFkId()));
        if (p.getAprFuelFkId() != null) dto.setAprFuelFkId(UUID.fromString(p.getAprFuelFkId()));
        dto.setAprFuelName(p.getAprFuelName());
        dto.setAprPriority(p.getAprPriority());
        dto.setAprQuantity(p.getAprQuantity());
        if (p.getMayFuelFkId() != null) dto.setMayFuelFkId(UUID.fromString(p.getMayFuelFkId()));
        dto.setMayFuelName(p.getMayFuelName());
        dto.setMayPriority(p.getMayPriority());
        dto.setMayQuantity(p.getMayQuantity());
        if (p.getJunFuelFkId() != null) dto.setJunFuelFkId(UUID.fromString(p.getJunFuelFkId()));
        dto.setJunFuelName(p.getJunFuelName());
        dto.setJunPriority(p.getJunPriority());
        dto.setJunQuantity(p.getJunQuantity());
        if (p.getJulFuelFkId() != null) dto.setJulFuelFkId(UUID.fromString(p.getJulFuelFkId()));
        dto.setJulFuelName(p.getJulFuelName());
        dto.setJulPriority(p.getJulPriority());
        dto.setJulQuantity(p.getJulQuantity());
        if (p.getAugFuelFkId() != null) dto.setAugFuelFkId(UUID.fromString(p.getAugFuelFkId()));
        dto.setAugFuelName(p.getAugFuelName());
        dto.setAugPriority(p.getAugPriority());
        dto.setAugQuantity(p.getAugQuantity());
        if (p.getSepFuelFkId() != null) dto.setSepFuelFkId(UUID.fromString(p.getSepFuelFkId()));
        dto.setSepFuelName(p.getSepFuelName());
        dto.setSepPriority(p.getSepPriority());
        dto.setSepQuantity(p.getSepQuantity());
        if (p.getOctFuelFkId() != null) dto.setOctFuelFkId(UUID.fromString(p.getOctFuelFkId()));
        dto.setOctFuelName(p.getOctFuelName());
        dto.setOctPriority(p.getOctPriority());
        dto.setOctQuantity(p.getOctQuantity());
        if (p.getNovFuelFkId() != null) dto.setNovFuelFkId(UUID.fromString(p.getNovFuelFkId()));
        dto.setNovFuelName(p.getNovFuelName());
        dto.setNovPriority(p.getNovPriority());
        dto.setNovQuantity(p.getNovQuantity());
        if (p.getDecFuelFkId() != null) dto.setDecFuelFkId(UUID.fromString(p.getDecFuelFkId()));
        dto.setDecFuelName(p.getDecFuelName());
        dto.setDecPriority(p.getDecPriority());
        dto.setDecQuantity(p.getDecQuantity());
        if (p.getJanFuelFkId() != null) dto.setJanFuelFkId(UUID.fromString(p.getJanFuelFkId()));
        dto.setJanFuelName(p.getJanFuelName());
        dto.setJanPriority(p.getJanPriority());
        dto.setJanQuantity(p.getJanQuantity());
        if (p.getFebFuelFkId() != null) dto.setFebFuelFkId(UUID.fromString(p.getFebFuelFkId()));
        dto.setFebFuelName(p.getFebFuelName());
        dto.setFebPriority(p.getFebPriority());
        dto.setFebQuantity(p.getFebQuantity());
        if (p.getMarFuelFkId() != null) dto.setMarFuelFkId(UUID.fromString(p.getMarFuelFkId()));
        dto.setMarFuelName(p.getMarFuelName());
        dto.setMarPriority(p.getMarPriority());
        dto.setMarQuantity(p.getMarQuantity());
        dto.setRemarks(p.getRemarks());
        dto.setAopYear(p.getAopYear());
        return dto;
    }

    @Override
    public AOPMessageVM updatePlantFuelAvailabilityMonthly(List<PlantFuelAvailabilityMonthlyDto> payload) {
        AOPMessageVM response = new AOPMessageVM();
        int saved = 0;
        int skipped = 0;

        if (payload == null || payload.isEmpty()) {
            response.setCode(400);
            response.setMessage("Payload is empty");
            return response;
        }

        for (PlantFuelAvailabilityMonthlyDto dto : payload) {
            if (dto.getPlantFkId() == null) {
                skipped++;
                continue;
            }

            CPPPlantFuelAvailabilityMonthly entity;
            if (dto.getId() != null) {
                entity = monthlyRepository.findById(dto.getId()).orElse(null);
                if (entity == null) {
                    skipped++;
                    continue;
                }
            } else {
                entity = new CPPPlantFuelAvailabilityMonthly();
                entity.setCreatedDate(LocalDateTime.now());
            }

            entity.setPlantFkId(dto.getPlantFkId());
            entity.setAopYear(dto.getAopYear());
            entity.setAprFuelFkId(dto.getAprFuelFkId());
            entity.setAprPriority(dto.getAprPriority());
            entity.setAprQuantity(dto.getAprQuantity());
            entity.setMayFuelFkId(dto.getMayFuelFkId());
            entity.setMayPriority(dto.getMayPriority());
            entity.setMayQuantity(dto.getMayQuantity());
            entity.setJunFuelFkId(dto.getJunFuelFkId());
            entity.setJunPriority(dto.getJunPriority());
            entity.setJunQuantity(dto.getJunQuantity());
            entity.setJulFuelFkId(dto.getJulFuelFkId());
            entity.setJulPriority(dto.getJulPriority());
            entity.setJulQuantity(dto.getJulQuantity());
            entity.setAugFuelFkId(dto.getAugFuelFkId());
            entity.setAugPriority(dto.getAugPriority());
            entity.setAugQuantity(dto.getAugQuantity());
            entity.setSepFuelFkId(dto.getSepFuelFkId());
            entity.setSepPriority(dto.getSepPriority());
            entity.setSepQuantity(dto.getSepQuantity());
            entity.setOctFuelFkId(dto.getOctFuelFkId());
            entity.setOctPriority(dto.getOctPriority());
            entity.setOctQuantity(dto.getOctQuantity());
            entity.setNovFuelFkId(dto.getNovFuelFkId());
            entity.setNovPriority(dto.getNovPriority());
            entity.setNovQuantity(dto.getNovQuantity());
            entity.setDecFuelFkId(dto.getDecFuelFkId());
            entity.setDecPriority(dto.getDecPriority());
            entity.setDecQuantity(dto.getDecQuantity());
            entity.setJanFuelFkId(dto.getJanFuelFkId());
            entity.setJanPriority(dto.getJanPriority());
            entity.setJanQuantity(dto.getJanQuantity());
            entity.setFebFuelFkId(dto.getFebFuelFkId());
            entity.setFebPriority(dto.getFebPriority());
            entity.setFebQuantity(dto.getFebQuantity());
            entity.setMarFuelFkId(dto.getMarFuelFkId());
            entity.setMarPriority(dto.getMarPriority());
            entity.setMarQuantity(dto.getMarQuantity());
            entity.setRemarks(dto.getRemarks());
            entity.setUpdatedDate(LocalDateTime.now());

            monthlyRepository.save(entity);
            saved++;
        }

        response.setCode(200);
        response.setMessage("Plant fuel availability monthly saved. Saved: " + saved + ", Skipped: " + skipped);
        return response;
    }

    @Override
    public AOPMessageVM deletePlantFuelAvailabilityMonthly(UUID id) {
        AOPMessageVM response = new AOPMessageVM();
        try {
            if (!monthlyRepository.existsById(id)) {
                response.setCode(404);
                response.setMessage("Record not found with id: " + id);
                return response;
            }
            monthlyRepository.deleteById(id);
            response.setCode(200);
            response.setMessage("Record deleted successfully");
        } catch (Exception e) {
            response.setCode(500);
            response.setMessage("Error deleting record: " + e.getMessage());
        }
        return response;
    }

    @Override
    public List<AssetFuelPriorityDto> getAssetFuelPriority(String plantIds, String financialYear) {
        return assetFuelPriorityRepository.getAssetFuelPriority(plantIds, financialYear)
                .stream().map(p -> {
                    AssetFuelPriorityDto dto = toAssetFuelPriorityDto(p);
                    dto.setFinancialYear(financialYear);
                    return dto;
                }).toList();
    }

    private AssetFuelPriorityDto toAssetFuelPriorityDto(AssetFuelPriorityProjection p) {
        AssetFuelPriorityDto dto = new AssetFuelPriorityDto();

        if (p.getId() != null)           dto.setId(UUID.fromString(p.getId()));
        if (p.getAssetId() != null)      dto.setAssetId(UUID.fromString(p.getAssetId()));
        dto.setAssetName(p.getAssetName());
        dto.setAssetType(p.getAssetType());
        if (p.getCppPlantFkId() != null) dto.setCppPlantFkId(UUID.fromString(p.getCppPlantFkId()));
        dto.setPlantName(p.getPlantName());
        // April
        if (p.getAprPrimary() != null)   dto.setAprPrimary(UUID.fromString(p.getAprPrimary()));
        if (p.getAprSecondary() != null) dto.setAprSecondary(UUID.fromString(p.getAprSecondary()));
        if (p.getAprTertiary() != null)  dto.setAprTertiary(UUID.fromString(p.getAprTertiary()));

        // May
        if (p.getMayPrimary() != null)   dto.setMayPrimary(UUID.fromString(p.getMayPrimary()));
        if (p.getMaySecondary() != null) dto.setMaySecondary(UUID.fromString(p.getMaySecondary()));
        if (p.getMayTertiary() != null)  dto.setMayTertiary(UUID.fromString(p.getMayTertiary()));

        // June
        if (p.getJunPrimary() != null)   dto.setJunPrimary(UUID.fromString(p.getJunPrimary()));
        if (p.getJunSecondary() != null) dto.setJunSecondary(UUID.fromString(p.getJunSecondary()));
        if (p.getJunTertiary() != null)  dto.setJunTertiary(UUID.fromString(p.getJunTertiary()));

        // July
        if (p.getJulPrimary() != null)   dto.setJulPrimary(UUID.fromString(p.getJulPrimary()));
        if (p.getJulSecondary() != null) dto.setJulSecondary(UUID.fromString(p.getJulSecondary()));
        if (p.getJulTertiary() != null)  dto.setJulTertiary(UUID.fromString(p.getJulTertiary()));

        // August
        if (p.getAugPrimary() != null)   dto.setAugPrimary(UUID.fromString(p.getAugPrimary()));
        if (p.getAugSecondary() != null) dto.setAugSecondary(UUID.fromString(p.getAugSecondary()));
        if (p.getAugTertiary() != null)  dto.setAugTertiary(UUID.fromString(p.getAugTertiary()));

        // September
        if (p.getSepPrimary() != null)   dto.setSepPrimary(UUID.fromString(p.getSepPrimary()));
        if (p.getSepSecondary() != null) dto.setSepSecondary(UUID.fromString(p.getSepSecondary()));
        if (p.getSepTertiary() != null)  dto.setSepTertiary(UUID.fromString(p.getSepTertiary()));

        // October
        if (p.getOctPrimary() != null)   dto.setOctPrimary(UUID.fromString(p.getOctPrimary()));
        if (p.getOctSecondary() != null) dto.setOctSecondary(UUID.fromString(p.getOctSecondary()));
        if (p.getOctTertiary() != null)  dto.setOctTertiary(UUID.fromString(p.getOctTertiary()));

        // November
        if (p.getNovPrimary() != null)   dto.setNovPrimary(UUID.fromString(p.getNovPrimary()));
        if (p.getNovSecondary() != null) dto.setNovSecondary(UUID.fromString(p.getNovSecondary()));
        if (p.getNovTertiary() != null)  dto.setNovTertiary(UUID.fromString(p.getNovTertiary()));

        // December
        if (p.getDecPrimary() != null)   dto.setDecPrimary(UUID.fromString(p.getDecPrimary()));
        if (p.getDecSecondary() != null) dto.setDecSecondary(UUID.fromString(p.getDecSecondary()));
        if (p.getDecTertiary() != null)  dto.setDecTertiary(UUID.fromString(p.getDecTertiary()));

        // January
        if (p.getJanPrimary() != null)   dto.setJanPrimary(UUID.fromString(p.getJanPrimary()));
        if (p.getJanSecondary() != null) dto.setJanSecondary(UUID.fromString(p.getJanSecondary()));
        if (p.getJanTertiary() != null)  dto.setJanTertiary(UUID.fromString(p.getJanTertiary()));

        // February
        if (p.getFebPrimary() != null)   dto.setFebPrimary(UUID.fromString(p.getFebPrimary()));
        if (p.getFebSecondary() != null) dto.setFebSecondary(UUID.fromString(p.getFebSecondary()));
        if (p.getFebTertiary() != null)  dto.setFebTertiary(UUID.fromString(p.getFebTertiary()));

        // March
        if (p.getMarPrimary() != null)   dto.setMarPrimary(UUID.fromString(p.getMarPrimary()));
        if (p.getMarSecondary() != null) dto.setMarSecondary(UUID.fromString(p.getMarSecondary()));
        if (p.getMarTertiary() != null)  dto.setMarTertiary(UUID.fromString(p.getMarTertiary()));

        dto.setRemarks(p.getRemarks());
        dto.setCreatedDate(p.getCreatedDate());
        dto.setModifiedDate(p.getModifiedDate());

        return dto;
    }

    @Override
    public Map<UUID, Map<UUID, UUID>> processFuelPriorityNormParameters(List<AssetFuelPriorityDto> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("Payload is empty or null");
        }

        logger.info("processFuelPriorityNormParameters: starting for {} payload rows", payload.size());

        // Request-level local caches
        UUID rawMaterialTypeId = resolveRawMaterialTypeId();
        Map<UUID, UUID> procurementPlantCache = new HashMap<>();
        Map<UUID, FuelMasterDetails> fuelMasterCache = new HashMap<>();
        Map<NormParameterKey, ResolvedNormParameter> normParameterCache = new HashMap<>();
        Map<UUID, String> plantNameCache = new HashMap<>();
        Map<NormsHeaderKey, UUID> normsHeaderCache = new HashMap<>();

        Map<UUID, Map<UUID, UUID>> assetToFuelNormParams = new LinkedHashMap<>();

        for (AssetFuelPriorityDto dto : payload) {
            if (dto.getAssetId() == null) {
                throw new IllegalArgumentException("Asset ID (assetId) is required for priority row ID: " + dto.getId());
            }
            UUID assetId = dto.getAssetId();

            Set<UUID> uniqueFuelIds = extractUniqueFuelIds(dto);
            if (uniqueFuelIds.isEmpty()) {
                assetToFuelNormParams.put(assetId, Collections.emptyMap());
                continue;
            }

            if (dto.getCppPlantFkId() == null) {
                throw new IllegalArgumentException("Plant ID (cppPlantFkId) is required for asset ID: " + assetId);
            }

            UUID procurementPlantId = resolveProcurementPlantId(dto.getCppPlantFkId(), procurementPlantCache);

            Map<UUID, UUID> fuelToNormParamMap = new LinkedHashMap<>();
            for (UUID fuelId : uniqueFuelIds) {
                FuelMasterDetails fuelDetails = getFuelDetails(fuelId, fuelMasterCache);
                ResolvedNormParameter normParam = findOrCreateNormParameter(fuelDetails, procurementPlantId, rawMaterialTypeId, 2, normParameterCache);
                findOrCreateNormsHeader(assetId, normParam, plantNameCache, normsHeaderCache, false);
                ensureLinkedHrsgFreeSteamHeaders(dto, fuelId, normParam,
                        plantNameCache, normsHeaderCache);
                fuelToNormParamMap.put(fuelId, normParam.id());
            }

            assetToFuelNormParams.put(assetId, fuelToNormParamMap);
        }

        logger.info("processFuelPriorityNormParameters: processed {} assets with unique NormParameters resolved", assetToFuelNormParams.size());
        return assetToFuelNormParams;
    }

    /**
     * Creates the free-steam companion NormsHeader (IsFreeSteamFuel=1) for each linked HRSG
     * that has a generation-side header, using the same NormParameter as the GT asset.
     * Child records (NormsMonthDetail, CPPNorms, CPPMonthWisePrice) are also ensured.
     */
    private void ensureLinkedHrsgFreeSteamHeaders(
            AssetFuelPriorityDto dto,
            UUID fuelId,
            ResolvedNormParameter normParam,
            Map<UUID, String> plantNameCache,
            Map<NormsHeaderKey, UUID> normsHeaderCache) {
        UUID gtAssetId = dto.getAssetId();
        UUID cppPlantId = dto.getCppPlantFkId();

        // Find linked HRSG assets that already have a generation-side NormsHeader (SHP/HP utility)
        String linkedHrsgSql = "SELECT CAST(s.AssetId AS varchar(36)) " +
                "FROM dbo.CPPSteamGenerationAsset s " +
                "WHERE s.LinkedPowerAsset_FK_ID = ? AND s.CPPPLANT_FK_Id = ? " +
                "AND EXISTS (SELECT 1 FROM dbo.NormsHeader gh " +
                " WHERE gh.AssetFKId = s.AssetId AND gh.AccountName <> 'Raw Material' " +
                " AND (gh.UtilityName LIKE '%SHP%' OR gh.UtilityName LIKE '%HP%'))";
        List<String> hrsgIds = db1JdbcTemplate.queryForList(linkedHrsgSql, String.class,
                gtAssetId.toString(), cppPlantId.toString());
        if (hrsgIds.isEmpty()) {
            return;
        }

        // Use the same NormParameter as the GT asset — no separate "(Free steam)" lookup needed
        for (String hrsgId : hrsgIds) {
            UUID hrsgAssetId = UUID.fromString(hrsgId);
            // Create/find NormsHeader for the HRSG with IsFreeSteamFuel = 1
            UUID headerId = findOrCreateNormsHeader(hrsgAssetId, normParam, plantNameCache, normsHeaderCache, true);
            String financialYear = dto.getFinancialYear();
            if (financialYear != null && !financialYear.isBlank()) {
                ensureNormMonthDetails(headerId, financialYear);
                ensureCppNorms(headerId, financialYear);
                String sourcePlantSql = "SELECT CAST(FuelAvailablePlantFKId AS varchar(36)) " +
                        "FROM dbo.CPPPlantFuelSourceMapping WHERE CPPPlantFKId = ?";
                List<String> sourcePlantIds = db1JdbcTemplate.queryForList(sourcePlantSql, String.class, cppPlantId.toString());
                if (sourcePlantIds.size() != 1 || sourcePlantIds.get(0) == null) {
                    throw new IllegalStateException("Expected one fuel source mapping for CPP plant " + cppPlantId +
                            ", found " + sourcePlantIds.size());
                }
                MonthlyFuelPrices prices = getMonthlyFuelPrices(UUID.fromString(sourcePlantIds.get(0)), fuelId, financialYear);
                ensureCppMonthWisePrice(headerId, financialYear, prices);
            }
        }
    }

    public Set<UUID> extractUniqueFuelIds(AssetFuelPriorityDto dto) {
        if (dto == null) {
            return Collections.emptySet();
        }
        Set<UUID> fuelIds = new LinkedHashSet<>();
        addIfNotNull(fuelIds, dto.getAprPrimary());
        addIfNotNull(fuelIds, dto.getAprSecondary());
        addIfNotNull(fuelIds, dto.getAprTertiary());

        addIfNotNull(fuelIds, dto.getMayPrimary());
        addIfNotNull(fuelIds, dto.getMaySecondary());
        addIfNotNull(fuelIds, dto.getMayTertiary());

        addIfNotNull(fuelIds, dto.getJunPrimary());
        addIfNotNull(fuelIds, dto.getJunSecondary());
        addIfNotNull(fuelIds, dto.getJunTertiary());

        addIfNotNull(fuelIds, dto.getJulPrimary());
        addIfNotNull(fuelIds, dto.getJulSecondary());
        addIfNotNull(fuelIds, dto.getJulTertiary());

        addIfNotNull(fuelIds, dto.getAugPrimary());
        addIfNotNull(fuelIds, dto.getAugSecondary());
        addIfNotNull(fuelIds, dto.getAugTertiary());

        addIfNotNull(fuelIds, dto.getSepPrimary());
        addIfNotNull(fuelIds, dto.getSepSecondary());
        addIfNotNull(fuelIds, dto.getSepTertiary());

        addIfNotNull(fuelIds, dto.getOctPrimary());
        addIfNotNull(fuelIds, dto.getOctSecondary());
        addIfNotNull(fuelIds, dto.getOctTertiary());

        addIfNotNull(fuelIds, dto.getNovPrimary());
        addIfNotNull(fuelIds, dto.getNovSecondary());
        addIfNotNull(fuelIds, dto.getNovTertiary());

        addIfNotNull(fuelIds, dto.getDecPrimary());
        addIfNotNull(fuelIds, dto.getDecSecondary());
        addIfNotNull(fuelIds, dto.getDecTertiary());

        addIfNotNull(fuelIds, dto.getJanPrimary());
        addIfNotNull(fuelIds, dto.getJanSecondary());
        addIfNotNull(fuelIds, dto.getJanTertiary());

        addIfNotNull(fuelIds, dto.getFebPrimary());
        addIfNotNull(fuelIds, dto.getFebSecondary());
        addIfNotNull(fuelIds, dto.getFebTertiary());

        addIfNotNull(fuelIds, dto.getMarPrimary());
        addIfNotNull(fuelIds, dto.getMarSecondary());
        addIfNotNull(fuelIds, dto.getMarTertiary());

        return fuelIds;
    }

    private void addIfNotNull(Set<UUID> set, UUID id) {
        if (id != null) {
            set.add(id);
        }
    }

    private UUID resolveRawMaterialTypeId() {
        String sql = "SELECT CAST(Id AS varchar(36)) FROM dbo.NormParameterType WHERE Name = 'RawMaterial'";
        List<String> results = db1JdbcTemplate.queryForList(sql, String.class);
        if (results.isEmpty()) {
            throw new IllegalStateException("NormParameterType with Name 'RawMaterial' not found");
        }
        if (results.size() > 1) {
            throw new IllegalStateException("Ambiguous NormParameterType: multiple records found with Name 'RawMaterial'");
        }
        UUID typeId = UUID.fromString(results.get(0));
        logger.debug("resolveRawMaterialTypeId: resolved RawMaterial type ID={}", typeId);
        return typeId;
    }

    private UUID resolveProcurementPlantId(UUID cppPlantFkId, Map<UUID, UUID> procurementPlantCache) {
        if (cppPlantFkId == null) {
            throw new IllegalArgumentException("Invalid or missing cppPlantFkId");
        }
        if (procurementPlantCache.containsKey(cppPlantFkId)) {
            return procurementPlantCache.get(cppPlantFkId);
        }

        // Query CPPPlantFuelSourceMapping
        String mappingSql = "SELECT CAST(FuelAvailablePlantFKId AS varchar(36)) FROM dbo.CPPPlantFuelSourceMapping WHERE CPPPlantFKId = ?";
        List<String> mappingResults = db1JdbcTemplate.queryForList(mappingSql, String.class, cppPlantFkId.toString());
        if (mappingResults.isEmpty() || mappingResults.get(0) == null) {
            throw new IllegalStateException("Missing CPPPlantFuelSourceMapping record for CPPPlantFKId: " + cppPlantFkId);
        }
        if (mappingResults.size() > 1) {
            throw new IllegalStateException("Multiple CPPPlantFuelSourceMapping records found for CPPPlantFKId: " + cppPlantFkId);
        }
        String fuelAvailablePlantFkId = mappingResults.get(0);

        // Query Plants where SourceName matches FuelAvailablePlantFKId and BusinessCategoryName = 'Import'
        String plantSql = "SELECT CAST(Id AS varchar(36)) FROM dbo.Plants " +
                          "WHERE (SourceName = ? OR TRY_CONVERT(UNIQUEIDENTIFIER, SourceName) = TRY_CONVERT(UNIQUEIDENTIFIER, ?)) " +
                          "  AND BusinessCategoryName = 'Import'";
        List<String> plantResults = db1JdbcTemplate.queryForList(plantSql, String.class, fuelAvailablePlantFkId, fuelAvailablePlantFkId);
        if (plantResults.isEmpty()) {
            throw new IllegalStateException("No Import plant found in dbo.Plants for SourceName (FuelAvailablePlantFKId): " + fuelAvailablePlantFkId);
        }
        if (plantResults.size() > 1) {
            throw new IllegalStateException("Ambiguous Import plants: multiple plants found in dbo.Plants for SourceName: " + fuelAvailablePlantFkId);
        }

        UUID procurementPlantId = UUID.fromString(plantResults.get(0));
        logger.debug("resolveProcurementPlantId: cppPlantFkId={} resolved to procurementPlantId={}", cppPlantFkId, procurementPlantId);
        procurementPlantCache.put(cppPlantFkId, procurementPlantId);
        return procurementPlantId;
    }

    private FuelMasterDetails getFuelDetails(UUID fuelId, Map<UUID, FuelMasterDetails> fuelMasterCache) {
        if (fuelId == null) {
            throw new IllegalArgumentException("Fuel ID cannot be null");
        }
        if (fuelMasterCache.containsKey(fuelId)) {
            return fuelMasterCache.get(fuelId);
        }

        String fuelSql = "SELECT CAST(Id AS varchar(36)) AS Id, FuelName, FuelDisplayName, [Type], " +
                         "CAST(Category_FK_Id AS varchar(36)) AS Category_FK_Id, FuelCode, UOM, [Description], CalorificValue " +
                         "FROM dbo.CPP_FuelMaster WHERE Id = ?";
        List<FuelMasterDetails> list = db1JdbcTemplate.query(fuelSql, (rs, rowNum) -> new FuelMasterDetails(
            UUID.fromString(rs.getString("Id")),
            rs.getString("FuelName"),
            rs.getString("FuelDisplayName"),
            rs.getString("Type"),
            rs.getString("Category_FK_Id") != null ? UUID.fromString(rs.getString("Category_FK_Id")) : null,
            rs.getString("FuelCode"),
            rs.getString("UOM"),
            rs.getString("Description"),
            rs.getBigDecimal("CalorificValue")
        ), fuelId.toString());

        if (list.isEmpty()) {
            throw new IllegalStateException("Fuel record not found in CPP_FuelMaster for ID: " + fuelId);
        }

        FuelMasterDetails details = list.get(0);
        if (details.fuelCode() == null || details.fuelCode().trim().isEmpty()) {
            throw new IllegalStateException("FuelCode is null or blank in CPP_FuelMaster for fuel ID: " + fuelId);
        }

        fuelMasterCache.put(fuelId, details);
        return details;
    }

    private ResolvedNormParameter findOrCreateNormParameter(FuelMasterDetails fuelDetails, UUID procurementPlantId, UUID rawMaterialTypeId, int normTypeId, Map<NormParameterKey, ResolvedNormParameter> normParameterCache) {
        NormParameterKey key = new NormParameterKey(procurementPlantId, fuelDetails.fuelCode(), rawMaterialTypeId, normTypeId);
        if (normParameterCache.containsKey(key)) {
            logger.debug("findOrCreateNormParameter: cache hit for fuelCode='{}', plantId={}", fuelDetails.fuelCode(), procurementPlantId);
            return normParameterCache.get(key);
        }

        // Find existing record using all four conditions
        String findSql = "SELECT CAST(Id AS varchar(36)) AS Id, Name, UOM, SAPMaterialCode, CAST(Plant_FK_Id AS varchar(36)) AS Plant_FK_Id FROM dbo.NormParameters " +
                         "WHERE SAPMaterialCode = ? " +
                         "  AND Plant_FK_Id = ? " +
                         "  AND NormParameterType_FK_Id = ? " +
                         "  AND NormType_FK_Id = ? " +
                         "  AND (Name IS NULL OR Name NOT LIKE '% (Free steam)')";
        List<ResolvedNormParameter> existing = db1JdbcTemplate.query(findSql, (rs, rowNum) -> new ResolvedNormParameter(
                UUID.fromString(rs.getString("Id")),
                rs.getString("Name"),
                rs.getString("UOM"),
                rs.getString("SAPMaterialCode"),
                UUID.fromString(rs.getString("Plant_FK_Id"))
        ), fuelDetails.fuelCode(), procurementPlantId.toString(), rawMaterialTypeId.toString(), normTypeId);

        if (!existing.isEmpty()) {
            if (existing.size() > 1) {
                throw new IllegalStateException("Multiple NormParameters records match SAPMaterialCode: " + fuelDetails.fuelCode() +
                        ", Plant_FK_Id: " + procurementPlantId + ", NormParameterType_FK_Id: " + rawMaterialTypeId + ", NormType_FK_Id: " + normTypeId);
            }
            ResolvedNormParameter existingParam = existing.get(0);
            logger.info("findOrCreateNormParameter: reusing existing NormParameters ID={} for fuelCode='{}', plantId={}",
                    existingParam.id(), fuelDetails.fuelCode(), procurementPlantId);
            normParameterCache.put(key, existingParam);
            return existingParam;
        }

        // Insert new NormParameters record
        UUID newId = UUID.randomUUID();
        String insertSql = "INSERT INTO dbo.NormParameters " +
                "(Id, Name, DisplayName, UOM, [Type], NormParameterType_FK_Id, Plant_FK_Id, " +
                " NormType_FK_Id, IsHistorical, DisplayOrder, IsEditable, IsVisible, SAPMaterialCode) " +
                "VALUES (?, ?, ?, 'MMBTU', 'CPPFuel', ?, ?, ?, 1, 1, 1, 1, ?)";

        try {
            int inserted = db1JdbcTemplate.update(insertSql,
                    newId.toString(),
                    fuelDetails.fuelName(),
                    fuelDetails.fuelDisplayName(),
                    rawMaterialTypeId.toString(),
                    procurementPlantId.toString(),
                    normTypeId,
                    fuelDetails.fuelCode());

            if (inserted <= 0) {
                throw new IllegalStateException("Failed to insert NormParameters record for fuelCode: " + fuelDetails.fuelCode());
            }

            logger.info("findOrCreateNormParameter: inserted new NormParameters ID={} for fuelCode='{}', plantId={}",
                    newId, fuelDetails.fuelCode(), procurementPlantId);
            ResolvedNormParameter newParam = new ResolvedNormParameter(
                    newId,
                    fuelDetails.fuelName(),
                    "MMBTU",
                    fuelDetails.fuelCode(),
                    procurementPlantId
            );
            normParameterCache.put(key, newParam);
            return newParam;
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Concurrent transaction may have inserted matching row; re-query to reuse
            logger.warn("findOrCreateNormParameter: DataIntegrityViolationException on insert for fuelCode='{}', plantId={}. Re-checking existing...",
                    fuelDetails.fuelCode(), procurementPlantId);
            List<ResolvedNormParameter> recheck = db1JdbcTemplate.query(findSql, (rs, rowNum) -> new ResolvedNormParameter(
                    UUID.fromString(rs.getString("Id")),
                    rs.getString("Name"),
                    rs.getString("UOM"),
                    rs.getString("SAPMaterialCode"),
                    UUID.fromString(rs.getString("Plant_FK_Id"))
            ), fuelDetails.fuelCode(), procurementPlantId.toString(), rawMaterialTypeId.toString(), normTypeId);
            if (!recheck.isEmpty()) {
                ResolvedNormParameter existingParam = recheck.get(0);
                logger.info("findOrCreateNormParameter: recovered from concurrent insert, reusing existing ID={}", existingParam.id());
                normParameterCache.put(key, existingParam);
                return existingParam;
            }
            throw ex;
        }
    }

    private UUID findOrCreateNormsHeader(
            UUID assetId,
            ResolvedNormParameter normParam,
            Map<UUID, String> plantNameCache,
            Map<NormsHeaderKey, UUID> normsHeaderCache,
            boolean isFreeSteam) {

        if (assetId == null) {
            throw new IllegalArgumentException("Asset ID cannot be null when creating NormsHeader");
        }
        if (normParam == null || normParam.id() == null) {
            throw new IllegalArgumentException("NormParameter cannot be null when creating NormsHeader");
        }

        NormsHeaderKey key = new NormsHeaderKey(assetId, normParam.id(), isFreeSteam);
        if (normsHeaderCache.containsKey(key)) {
            logger.debug("findOrCreateNormsHeader: cache hit for assetId={}, normParameterId={}, isFreeSteam={}", assetId, normParam.id(), isFreeSteam);
            return normsHeaderCache.get(key);
        }

        // Duplicate check: AssetFKId = ? AND NormParameter_FK_Id = ? AND ISNULL(IsFreeSteamFuel, 0) = ?
        int isFreeSteamInt = isFreeSteam ? 1 : 0;
        String checkSql = "SELECT CAST(Id AS varchar(36)) FROM dbo.NormsHeader " +
                          "WHERE AssetFKId = ? AND NormParameter_FK_Id = ? AND ISNULL(IsFreeSteamFuel, 0) = ?";
        List<String> existing = db1JdbcTemplate.queryForList(checkSql, String.class,
                assetId.toString(), normParam.id().toString(), isFreeSteamInt);

        if (!existing.isEmpty()) {
            UUID existingId = UUID.fromString(existing.get(0));
            logger.info("findOrCreateNormsHeader: reusing existing NormsHeader ID={} for assetId={}, normParameterId={}, isFreeSteam={}",
                    existingId, assetId, normParam.id(), isFreeSteam);
            normsHeaderCache.put(key, existingId);
            return existingId;
        }

        // Select base row: TOP 1 from dbo.NormsHeader for the same AssetFKId
        String baseSql = "SELECT TOP 1 CAST(Plant_FK_Id AS varchar(36)) AS Plant_FK_Id, " +
                         "UtilityName, UtilityId, UtilityUOM, DisplayOrder, plantCode, " +
                         "CAST(Utility_NormParameter_FK_Id AS varchar(36)) AS Utility_NormParameter_FK_Id, " +
                         "UtilityActivity " +
                         "FROM dbo.NormsHeader " +
                         "WHERE AssetFKId = ? " +
                         "ORDER BY DisplayOrder ASC, Id ASC";
        List<NormsHeaderBaseRow> baseRows = db1JdbcTemplate.query(baseSql, (rs, rowNum) -> new NormsHeaderBaseRow(
                rs.getString("Plant_FK_Id"),
                rs.getString("UtilityName"),
                rs.getString("UtilityId"),
                rs.getString("UtilityUOM"),
                rs.getObject("DisplayOrder") != null ? rs.getInt("DisplayOrder") : 1,
                rs.getString("plantCode"),
                rs.getString("Utility_NormParameter_FK_Id"),
                rs.getString("UtilityActivity")
        ), assetId.toString());

        if (baseRows.isEmpty()) {
            throw new IllegalStateException("No base NormsHeader row found for AssetFKId: " + assetId);
        }
        NormsHeaderBaseRow base = baseRows.get(0);

        // Resolve plant name from dbo.Plants for NormParameters.Plant_FK_Id
        String issuingPlantName = plantNameCache.computeIfAbsent(normParam.plantFkId(), pid -> {
            String plantSql = "SELECT Name FROM dbo.Plants WHERE Id = ?";
            List<String> names = db1JdbcTemplate.queryForList(plantSql, String.class, pid.toString());
            if (names.isEmpty() || names.get(0) == null) {
                throw new IllegalStateException("Plant not found in dbo.Plants for Id: " + pid);
            }
            return names.get(0);
        });

        // Insert NormsHeader — IsFreeSteamFuel = 1 for HRSG free-steam headers, 0 for GT
        UUID newId = UUID.randomUUID();
        String insertSql = "INSERT INTO dbo.NormsHeader " +
                "(Id, Plant_FK_Id, UtilityName, UtilityId, UtilityUOM, AccountName, " +
                " MaterialName, IssuingPlantName, IssuingPlant_FK_Id, NormParameter_FK_Id, " +
                " IsActive, IssuingUOM, DisplayOrder, MaterialId, Remarks, plantCode, " +
                " CPP_SR_Mapping_Master_Fk_Id, Utility_NormParameter_FK_Id, UtilityActivity, " +
                " MaterialActivity, AssetFKId, IsFreeSteamFuel) " +
                "VALUES (?, ?, ?, ?, ?, 'Raw Material', ?, ?, ?, ?, 1, ?, ?, ?, 'Fuel added', ?, NULL, ?, ?, NULL, ?, ?)";

        try {
            int inserted = db1JdbcTemplate.update(insertSql,
                    newId.toString(),
                    base.plantFkId(),
                    base.utilityName(),
                    base.utilityId(),
                    base.utilityUOM(),
                    normParam.name(),
                    issuingPlantName,
                    normParam.plantFkId().toString(),
                    normParam.id().toString(),
                    normParam.uom(),
                    base.displayOrder(),
                    normParam.sapMaterialCode(),
                    base.plantCode(),
                    base.utilityNormParameterFkId(),
                    base.utilityActivity(),
                    assetId.toString(),
                    isFreeSteamInt
            );

            if (inserted <= 0) {
                throw new IllegalStateException("Failed to insert NormsHeader record for AssetFKId: " + assetId +
                        ", NormParameter_FK_Id: " + normParam.id());
            }

            logger.info("findOrCreateNormsHeader: inserted new NormsHeader ID={} for assetId={}, normParameterId={}, isFreeSteam={}",
                    newId, assetId, normParam.id(), isFreeSteam);
            normsHeaderCache.put(key, newId);
            return newId;

        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            logger.warn("findOrCreateNormsHeader: DataIntegrityViolationException on insert for assetId={}, normParameterId={}, isFreeSteam={}. Re-checking existing...",
                    assetId, normParam.id(), isFreeSteam);
            List<String> recheck = db1JdbcTemplate.queryForList(checkSql, String.class,
                    assetId.toString(), normParam.id().toString(), isFreeSteamInt);
            if (!recheck.isEmpty()) {
                UUID existingId = UUID.fromString(recheck.get(0));
                logger.info("findOrCreateNormsHeader: recovered from concurrent insert, reusing existing ID={}", existingId);
                normsHeaderCache.put(key, existingId);
                return existingId;
            }
            throw ex;
        }
    }

    public record FinancialYearRange(int startYear, int endYear) {}

    public FinancialYearRange parseAndValidateFinancialYear(String financialYear) {
        if (financialYear == null || financialYear.isBlank()) {
            throw new IllegalArgumentException("Financial year cannot be null or blank");
        }
        String trimmed = financialYear.trim();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(\\d{4})-(\\d{2}|\\d{4})$");
        java.util.regex.Matcher matcher = pattern.matcher(trimmed);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid financial year format: '" + financialYear +
                    "'. Expected format 'YYYY-YY' (e.g. '2026-27') or 'YYYY-YYYY' (e.g. '2026-2027').");
        }

        int startYear = Integer.parseInt(matcher.group(1));
        String secondPart = matcher.group(2);
        int expectedEndYear = startYear + 1;

        if (startYear < 1900 || startYear > 2100) {
            throw new IllegalArgumentException("Financial year out of supported range (1900-2100): " + startYear);
        }

        if (secondPart.length() == 2) {
            int endTwoDigits = Integer.parseInt(secondPart);
            int expectedTwoDigits = expectedEndYear % 100;
            if (endTwoDigits != expectedTwoDigits) {
                throw new IllegalArgumentException("Invalid financial year sequence: '" + financialYear +
                        "'. End year digits (" + secondPart + ") must be consecutive to start year (" + startYear + "). Expected: " +
                        String.format("%02d", expectedTwoDigits));
            }
        } else {
            int endFourDigits = Integer.parseInt(secondPart);
            if (endFourDigits != expectedEndYear) {
                throw new IllegalArgumentException("Invalid financial year sequence: '" + financialYear +
                        "'. End year (" + endFourDigits + ") must be consecutive to start year (" + startYear + "). Expected: " + expectedEndYear);
            }
        }

        return new FinancialYearRange(startYear, expectedEndYear);
    }

    @Override
    public List<UUID> getNormsHeaderIds(UUID assetId, UUID normParameterId) {
        if (assetId == null || normParameterId == null) {
            return Collections.emptyList();
        }
        String checkSql = "SELECT CAST(Id AS varchar(36)) FROM dbo.NormsHeader " +
                "WHERE AssetFKId = ? AND NormParameter_FK_Id = ?";
        List<String> results = db1JdbcTemplate.queryForList(checkSql, String.class,
                assetId.toString(), normParameterId.toString());
        if (results.isEmpty()) {
            return Collections.emptyList();
        }
        if (results.size() > 1) {
            logger.warn("getNormsHeaderIds: multiple NormsHeader records ({}) found for assetId={}, normParameterId={}. Repairing all matching headers.",
                    results.size(), assetId, normParameterId);
        }
        List<UUID> ids = new java.util.ArrayList<>(results.size());
        for (String idStr : results) {
            ids.add(UUID.fromString(idStr));
        }
        return ids;
    }

    @Override
    public UUID getNormsHeaderId(UUID assetId, UUID normParameterId) {
        List<UUID> ids = getNormsHeaderIds(assetId, normParameterId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private String resolveFinancialYear(AssetFuelPriorityDto dto, CPPAssetFuelPriority entity) {
        if (dto != null && dto.getFinancialYear() != null && !dto.getFinancialYear().isBlank()) {
            return dto.getFinancialYear().trim();
        }
        if (entity != null && entity.getFinancialYear() != null && !entity.getFinancialYear().isBlank()) {
            return entity.getFinancialYear().trim();
        }
        return null;
    }

    @Override
    public int ensureNormMonthDetails(UUID normsHeaderId, String financialYear) {
        return ensureNormMonthDetails(normsHeaderId, financialYear, new HashMap<>());
    }

    public int ensureNormMonthDetails(UUID normsHeaderId, String financialYear, Map<String, List<String>> fymCache) {
        if (normsHeaderId == null) {
            logger.warn("ensureNormMonthDetails: skipped – normsHeaderId is null");
            return 0;
        }

        // 1. Validate financial year format (e.g. 2026-27 or 2026-2027)
        FinancialYearRange range = parseAndValidateFinancialYear(financialYear);
        int startYear = range.startYear();
        int endYear = range.endYear();

        // 2. Fetch the 12 FinancialYearMonth rows spanning April (Month 4) to March (Month 3)
        List<String> fymIds = fymCache != null ? fymCache.get(financialYear) : null;
        if (fymIds == null) {
            String fymSql = "SELECT CAST(Id AS varchar(36)) AS Id FROM dbo.FinancialYearMonth " +
                    "WHERE (Year = ? AND Month >= 4) OR (Year = ? AND Month <= 3) " +
                    "ORDER BY Year ASC, Month ASC";
            fymIds = db1JdbcTemplate.queryForList(fymSql, String.class, startYear, endYear);

            // Validate all 12 month definitions exist
            Set<String> distinctFymIds = new LinkedHashSet<>(fymIds);
            if (distinctFymIds.size() != 12) {
                throw new IllegalStateException("Expected exactly 12 distinct FinancialYearMonth records for financialYear='" +
                        financialYear + "', but found " + distinctFymIds.size() + " (" + fymIds + ")");
            }

            if (fymCache != null) {
                fymCache.put(financialYear, fymIds);
            }
        }

        // 3. Check which monthly records already exist for this NormsHeader
        String existingSql = "SELECT CAST(FinancialYearMonth_FK_Id AS varchar(36)) FROM dbo.NormsMonthDetail " +
                "WHERE NormsHeader_FK_Id = ?";
        List<String> existingFymList = db1JdbcTemplate.queryForList(existingSql, String.class, normsHeaderId.toString());
        Set<String> existingFymSet = new HashSet<>(existingFymList);

        String insertSql = "INSERT INTO dbo.NormsMonthDetail " +
                "(Id, NormsHeader_FK_Id, FinancialYearMonth_FK_Id, ScenarioType, " +
                " Norms, Quantity, Amount, Price, DisplayOrder, GenerationUOM, QTY, Remarks) " +
                "VALUES (?, ?, ?, NULL, 0, 0, 0, 0, 1, NULL, 0, NULL)";

        int inserted = 0;
        for (String fymId : fymIds) {
            if (existingFymSet.contains(fymId)) {
                logger.debug("ensureNormMonthDetails: month record already exists for NormsHeader={}, FinancialYearMonth={} — preserved",
                        normsHeaderId, fymId);
                continue;
            }

            UUID newDetailId = UUID.randomUUID();
            try {
                db1JdbcTemplate.update(insertSql,
                        newDetailId.toString(),
                        normsHeaderId.toString(),
                        fymId
                );
                inserted++;
                logger.info("ensureNormMonthDetails: inserted NormsMonthDetail Id={} for NormsHeader={}, FinancialYearMonth={}",
                        newDetailId, normsHeaderId, fymId);
            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                logger.warn("ensureNormMonthDetails: DataIntegrityViolationException on insert for NormsHeader={}, FinancialYearMonth={}. Verifying if record exists...",
                        normsHeaderId, fymId);
                String verifySql = "SELECT COUNT(1) FROM dbo.NormsMonthDetail WHERE NormsHeader_FK_Id = ? AND FinancialYearMonth_FK_Id = ?";
                Integer count = db1JdbcTemplate.queryForObject(verifySql, Integer.class, normsHeaderId.toString(), fymId);
                if (count != null && count > 0) {
                    logger.info("ensureNormMonthDetails: confirmed record already exists concurrently for NormsHeader={}, FinancialYearMonth={}",
                            normsHeaderId, fymId);
                } else {
                    logger.error("ensureNormMonthDetails: insert failed with DataIntegrityViolationException but record does NOT exist for NormsHeader={}, FinancialYearMonth={}",
                            normsHeaderId, fymId);
                    throw ex;
                }
            }
        }

        logger.info("ensureNormMonthDetails: inserted {} missing month records for NormsHeader={}, financialYear={}",
                inserted, normsHeaderId, financialYear);
        return inserted;
    }

    @Override
    public int processFuelPriorityNormMonthDetails(
            List<AssetFuelPriorityDto> payload,
            Map<UUID, CPPAssetFuelPriority> entityMap,
            Map<UUID, Map<UUID, UUID>> resolvedNormParams) {

        if (payload == null || payload.isEmpty() || resolvedNormParams == null || resolvedNormParams.isEmpty()) {
            logger.info("processFuelPriorityNormMonthDetails: payload or resolvedNormParams empty, skipping");
            return 0;
        }

        logger.info("processFuelPriorityNormMonthDetails: starting Step 3 for {} payload rows", payload.size());

        Map<String, List<String>> fymCache = new HashMap<>();
        Map<NormsHeaderKey, List<UUID>> headerLookupCache = new HashMap<>();
        Set<String> processedHeaderYears = new HashSet<>();
        int totalInserted = 0;

        for (AssetFuelPriorityDto dto : payload) {
            UUID assetId = dto.getAssetId();
            if (assetId == null) {
                continue;
            }

            Map<UUID, UUID> fuelToNormParamMap = resolvedNormParams.get(assetId);
            if (fuelToNormParamMap == null || fuelToNormParamMap.isEmpty()) {
                continue;
            }

            CPPAssetFuelPriority entity = entityMap != null && dto.getId() != null ? entityMap.get(dto.getId()) : null;
            String financialYear = resolveFinancialYear(dto, entity);
            if (financialYear == null || financialYear.isBlank()) {
                logger.warn("processFuelPriorityNormMonthDetails: skipping assetId={} because financialYear is missing", assetId);
                continue;
            }

            Set<UUID> distinctNormParamIds = new LinkedHashSet<>(fuelToNormParamMap.values());
            for (UUID normParamId : distinctNormParamIds) {
                if (normParamId == null) {
                    continue;
                }

                NormsHeaderKey key = new NormsHeaderKey(assetId, normParamId, false);
                List<UUID> normsHeaderIds = headerLookupCache.computeIfAbsent(key, k -> getNormsHeaderIds(assetId, normParamId));
                if (normsHeaderIds.isEmpty()) {
                    logger.warn("processFuelPriorityNormMonthDetails: no NormsHeader found for assetId={}, normParameterId={}",
                            assetId, normParamId);
                    continue;
                }

                for (UUID normsHeaderId : normsHeaderIds) {
                    String headerYearKey = normsHeaderId + ":" + financialYear;
                    if (!processedHeaderYears.add(headerYearKey)) {
                        continue;
                    }

                    int inserted = ensureNormMonthDetails(normsHeaderId, financialYear, fymCache);
                    totalInserted += inserted;
                }
            }
        }

        logger.info("processFuelPriorityNormMonthDetails: completed Step 3, inserted {} missing NormMonthDetail records", totalInserted);
        return totalInserted;
    }

    /**
     * Step 4: Ensures a default CPPNorms record exists for the given NormsHeader and financial year.
     * If already present, the existing record and its values are preserved (idempotent).
     * If absent, a new CPPNorms record is inserted with:
     * - NormType_FK_Id = 6 ('Fixed')
     * - All 12 month norms (Apr..Mar) = 0
     * - FinancialYear = financialYear
     * - AOPYear = financialYear
     * - Remarks = 'Add new record'
     * - CreatedBy / ModifiedBy = 'SYSTEM'
     * - CreatedDate / ModifiedDate = GETDATE()
     * - ApplyActualNormToAll = 1
     *
     * @param normsHeaderId the ID of the target NormsHeader row
     * @param financialYear the validated financial year string (e.g. "2026-27")
     * @return 1 if newly inserted, 0 if already existed or skipped
     */
    @Override
    public int ensureCppNorms(UUID normsHeaderId, String financialYear) {
        if (normsHeaderId == null) {
            logger.warn("ensureCppNorms: skipped – normsHeaderId is null");
            return 0;
        }

        // Validate financial year format
        parseAndValidateFinancialYear(financialYear);
        String fy = financialYear.trim();

        // Check if CPPNorms already exists for this NormsHeader and financial year
        String checkSql = "SELECT CAST(Id AS varchar(36)) FROM dbo.CPPNorms " +
                "WHERE NormsHeader_FK_Id = ? AND FinancialYear = ?";
        List<String> existing = db1JdbcTemplate.queryForList(checkSql, String.class,
                normsHeaderId.toString(), fy);

        if (!existing.isEmpty()) {
            logger.debug("ensureCppNorms: CPPNorms already present for NormsHeader={}, financialYear={} — preserved",
                    normsHeaderId, fy);
            return 0;
        }

        UUID newId = UUID.randomUUID();
        String insertSql = "INSERT INTO dbo.CPPNorms " +
                "(Id, NormsHeader_FK_Id, FinancialYear, AOPYear, NormType_FK_Id, " +
                " Apr_Norms, May_Norms, Jun_Norms, Jul_Norms, Aug_Norms, Sep_Norms, " +
                " Oct_Norms, Nov_Norms, Dec_Norms, Jan_Norms, Feb_Norms, Mar_Norms, " +
                " Remarks, CreatedBy, CreatedDate, ModifiedBy, ModifiedDate, ApplyActualNormToAll) " +
                "VALUES (?, ?, ?, ?, 6, " +
                " 0, 0, 0, 0, 0, 0, " +
                " 0, 0, 0, 0, 0, 0, " +
                " 'Add new record', 'SYSTEM', GETDATE(), 'SYSTEM', GETDATE(), 1)";

        try {
            int inserted = db1JdbcTemplate.update(insertSql,
                    newId.toString(),
                    normsHeaderId.toString(),
                    fy,
                    fy
            );
            logger.info("ensureCppNorms: inserted CPPNorms Id={} for NormsHeader={}, financialYear={}",
                    newId, normsHeaderId, fy);
            return inserted;
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            logger.warn("ensureCppNorms: DataIntegrityViolationException on insert for NormsHeader={}, financialYear={}. Verifying existing...",
                    normsHeaderId, fy);
            String verifySql = "SELECT COUNT(1) FROM dbo.CPPNorms WHERE NormsHeader_FK_Id = ? AND FinancialYear = ?";
            Integer count = db1JdbcTemplate.queryForObject(verifySql, Integer.class, normsHeaderId.toString(), fy);
            if (count != null && count > 0) {
                logger.info("ensureCppNorms: confirmed record already exists concurrently for NormsHeader={}, financialYear={}",
                        normsHeaderId, fy);
                return 0;
            }
            logger.error("ensureCppNorms: insert failed with DataIntegrityViolationException and record was NOT found for NormsHeader={}, financialYear={}",
                    normsHeaderId, fy);
            throw ex;
        }
    }

    @Override
    public int processFuelPriorityCppNorms(
            List<AssetFuelPriorityDto> payload,
            Map<UUID, CPPAssetFuelPriority> entityMap,
            Map<UUID, Map<UUID, UUID>> resolvedNormParams) {

        if (payload == null || payload.isEmpty() || resolvedNormParams == null || resolvedNormParams.isEmpty()) {
            logger.info("processFuelPriorityCppNorms: payload or resolvedNormParams empty, skipping");
            return 0;
        }

        logger.info("processFuelPriorityCppNorms: starting Step 4 for {} payload rows", payload.size());

        Map<NormsHeaderKey, List<UUID>> headerLookupCache = new HashMap<>();
        Set<String> processedHeaderYears = new HashSet<>();
        int totalInserted = 0;

        for (AssetFuelPriorityDto dto : payload) {
            UUID assetId = dto.getAssetId();
            if (assetId == null) {
                continue;
            }

            Map<UUID, UUID> fuelToNormParamMap = resolvedNormParams.get(assetId);
            if (fuelToNormParamMap == null || fuelToNormParamMap.isEmpty()) {
                continue;
            }

            CPPAssetFuelPriority entity = entityMap != null && dto.getId() != null ? entityMap.get(dto.getId()) : null;
            String financialYear = resolveFinancialYear(dto, entity);
            if (financialYear == null || financialYear.isBlank()) {
                logger.warn("processFuelPriorityCppNorms: skipping assetId={} because financialYear is missing", assetId);
                continue;
            }

            Set<UUID> distinctNormParamIds = new LinkedHashSet<>(fuelToNormParamMap.values());
            for (UUID normParamId : distinctNormParamIds) {
                if (normParamId == null) {
                    continue;
                }

                NormsHeaderKey key = new NormsHeaderKey(assetId, normParamId, false);
                List<UUID> normsHeaderIds = headerLookupCache.computeIfAbsent(key, k -> getNormsHeaderIds(assetId, normParamId));
                if (normsHeaderIds.isEmpty()) {
                    logger.warn("processFuelPriorityCppNorms: no NormsHeader found for assetId={}, normParameterId={}",
                            assetId, normParamId);
                    continue;
                }

                for (UUID normsHeaderId : normsHeaderIds) {
                    String headerYearKey = normsHeaderId + ":" + financialYear;
                    if (!processedHeaderYears.add(headerYearKey)) {
                        continue;
                    }

                    int inserted = ensureCppNorms(normsHeaderId, financialYear);
                    totalInserted += inserted;
                }
            }
        }

        logger.info("processFuelPriorityCppNorms: completed Step 4, inserted {} missing CPPNorms records", totalInserted);
        return totalInserted;
    }

    @Override
    public int ensureCppMonthWisePrice(UUID normsHeaderId, String financialYear) {
        return ensureCppMonthWisePrice(normsHeaderId, financialYear, MonthlyFuelPrices.zero());
    }

    @Override
    public int ensureCppMonthWisePrice(UUID normsHeaderId, String financialYear, MonthlyFuelPrices prices) {
        if (normsHeaderId == null) {
            throw new IllegalArgumentException("normsHeaderId must not be null");
        }
        if (financialYear == null || financialYear.isBlank()) {
            throw new IllegalArgumentException("financialYear must not be null or blank");
        }

        // Validate financial year format
        parseAndValidateFinancialYear(financialYear);
        String fy = financialYear.trim();
        MonthlyFuelPrices p = prices != null ? prices : MonthlyFuelPrices.zero();

        // Check if CPPMonthWisePrice already exists for this NormsHeader and financial year
        String checkSql = "SELECT CAST(Id AS varchar(36)) FROM dbo.CPPMonthWisePrice " +
                "WHERE NormsHeader_FK_Id = ? AND FinancialYear = ?";
        List<String> existing = db1JdbcTemplate.queryForList(checkSql, String.class,
                normsHeaderId.toString(), fy);

        if (!existing.isEmpty()) {
            String updateSql = "UPDATE dbo.CPPMonthWisePrice SET " +
                    "  Apr_Price = ?, May_Price = ?, Jun_Price = ?, Jul_Price = ?, Aug_Price = ?, Sep_Price = ?, " +
                    "  Oct_Price = ?, Nov_Price = ?, Dec_Price = ?, Jan_Price = ?, Feb_Price = ?, Mar_Price = ?, " +
                    "  PriceSource = 'Price', ValueType = 'Price', UpdatedDate = GETDATE(), Remarks = 'Added new norms for fuel', AOPYear = ? " +
                    "WHERE NormsHeader_FK_Id = ? AND FinancialYear = ?";
            int updated = db1JdbcTemplate.update(updateSql,
                    p.apr(), p.may(), p.jun(), p.jul(), p.aug(), p.sep(),
                    p.oct(), p.nov(), p.dec(), p.jan(), p.feb(), p.mar(),
                    fy,
                    normsHeaderId.toString(), fy);
            logger.info("ensureCppMonthWisePrice: synced existing CPPMonthWisePrice with CPPFuelAvailabilityTransaction for NormsHeader={}, financialYear={}, updatedRows={}",
                    normsHeaderId, fy, updated);
            return 0;
        }

        UUID newId = UUID.randomUUID();
        String insertSql = "INSERT INTO dbo.CPPMonthWisePrice " +
                "(Id, NormsHeader_FK_Id, FinancialYear, AOPYear, " +
                " Apr_Price, May_Price, Jun_Price, Jul_Price, Aug_Price, Sep_Price, " +
                " Oct_Price, Nov_Price, Dec_Price, Jan_Price, Feb_Price, Mar_Price, " +
                " Remarks, PriceSource, CreatedDate, UpdatedDate, ModifiedBy, ValueType) " +
                "VALUES (?, ?, ?, ?, " +
                " ?, ?, ?, ?, ?, ?, " +
                " ?, ?, ?, ?, ?, ?, " +
                " 'Added new norms for fuel', 'Price', GETDATE(), GETDATE(), 'SYSTEM', 'Price')";

        try {
            int inserted = db1JdbcTemplate.update(insertSql,
                    newId.toString(),
                    normsHeaderId.toString(),
                    fy,
                    fy,
                    p.apr(), p.may(), p.jun(), p.jul(), p.aug(), p.sep(),
                    p.oct(), p.nov(), p.dec(), p.jan(), p.feb(), p.mar()
            );
            logger.info("ensureCppMonthWisePrice: inserted CPPMonthWisePrice Id={} for NormsHeader={}, financialYear={}",
                    newId, normsHeaderId, fy);
            return inserted;
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            logger.warn("ensureCppMonthWisePrice: DataIntegrityViolationException on insert for NormsHeader={}, financialYear={}. Verifying existing...",
                    normsHeaderId, fy);
            String verifySql = "SELECT COUNT(1) FROM dbo.CPPMonthWisePrice WHERE NormsHeader_FK_Id = ? AND FinancialYear = ?";
            Integer count = db1JdbcTemplate.queryForObject(verifySql, Integer.class, normsHeaderId.toString(), fy);
            if (count != null && count > 0) {
                logger.info("ensureCppMonthWisePrice: confirmed record already exists concurrently for NormsHeader={}, financialYear={}. Syncing prices...",
                        normsHeaderId, fy);
                String updateSql = "UPDATE dbo.CPPMonthWisePrice SET " +
                        "  Apr_Price = ?, May_Price = ?, Jun_Price = ?, Jul_Price = ?, Aug_Price = ?, Sep_Price = ?, " +
                        "  Oct_Price = ?, Nov_Price = ?, Dec_Price = ?, Jan_Price = ?, Feb_Price = ?, Mar_Price = ?, " +
                        "  PriceSource = 'Price', ValueType = 'Price', UpdatedDate = GETDATE(), Remarks = 'Added new norms for fuel', AOPYear = ? " +
                        "WHERE NormsHeader_FK_Id = ? AND FinancialYear = ?";
                db1JdbcTemplate.update(updateSql,
                        p.apr(), p.may(), p.jun(), p.jul(), p.aug(), p.sep(),
                        p.oct(), p.nov(), p.dec(), p.jan(), p.feb(), p.mar(),
                        fy,
                        normsHeaderId.toString(), fy);
                return 0;
            }
            logger.error("ensureCppMonthWisePrice: insert failed with DataIntegrityViolationException and record was NOT found for NormsHeader={}, financialYear={}",
                    normsHeaderId, fy);
            throw ex;
        }
    }

    private MonthlyFuelPrices getMonthlyFuelPrices(UUID fuelAvailablePlantFkId, UUID fuelId, String financialYear) {
        if (fuelAvailablePlantFkId == null || fuelId == null || financialYear == null || financialYear.isBlank()) {
            return MonthlyFuelPrices.zero();
        }
        String sql = "SELECT " +
                "  COALESCE(Cost_Apr, 0) AS Cost_Apr, " +
                "  COALESCE(Cost_May, 0) AS Cost_May, " +
                "  COALESCE(Cost_Jun, 0) AS Cost_Jun, " +
                "  COALESCE(Cost_Jul, 0) AS Cost_Jul, " +
                "  COALESCE(Cost_Aug, 0) AS Cost_Aug, " +
                "  COALESCE(Cost_Sep, 0) AS Cost_Sep, " +
                "  COALESCE(Cost_Oct, 0) AS Cost_Oct, " +
                "  COALESCE(Cost_Nov, 0) AS Cost_Nov, " +
                "  COALESCE(Cost_Dec, 0) AS Cost_Dec, " +
                "  COALESCE(Cost_Jan, 0) AS Cost_Jan, " +
                "  COALESCE(Cost_Feb, 0) AS Cost_Feb, " +
                "  COALESCE(Cost_Mar, 0) AS Cost_Mar " +
                "FROM dbo.CPPFuelAvailabilityTransaction WITH(NOLOCK) " +
                "WHERE CPPPlantFKId = ? " +
                "  AND Fuel_FK_Id = ? " +
                "  AND [Type] = 'FUEL_AVAILABILITY' " +
                "  AND FinancialYear = ? " +
                "ORDER BY UpdatedDate DESC, CreatedDate DESC, Id DESC";

        List<MonthlyFuelPrices> list = db1JdbcTemplate.query(sql, (rs, rowNum) -> new MonthlyFuelPrices(
                rs.getDouble("Cost_Apr"),
                rs.getDouble("Cost_May"),
                rs.getDouble("Cost_Jun"),
                rs.getDouble("Cost_Jul"),
                rs.getDouble("Cost_Aug"),
                rs.getDouble("Cost_Sep"),
                rs.getDouble("Cost_Oct"),
                rs.getDouble("Cost_Nov"),
                rs.getDouble("Cost_Dec"),
                rs.getDouble("Cost_Jan"),
                rs.getDouble("Cost_Feb"),
                rs.getDouble("Cost_Mar")
        ), fuelAvailablePlantFkId.toString(), fuelId.toString(), financialYear.trim());

        if (list.isEmpty()) {
            logger.warn("getMonthlyFuelPrices: no CPPFuelAvailabilityTransaction found for plant={}, fuel={}, fy={}; defaulting prices to 0",
                    fuelAvailablePlantFkId, fuelId, financialYear);
            return MonthlyFuelPrices.zero();
        }
        if (list.size() > 1) {
            logger.warn("getMonthlyFuelPrices: multiple CPPFuelAvailabilityTransaction records ({}) found for plant={}, fuel={}, fy={}. Using most recent record.",
                    list.size(), fuelAvailablePlantFkId, fuelId, financialYear);
        }
        return list.get(0);
    }

    @Override
    public int processFuelPriorityCppMonthWisePrices(
            List<AssetFuelPriorityDto> payload,
            Map<UUID, CPPAssetFuelPriority> entityMap,
            Map<UUID, Map<UUID, UUID>> resolvedNormParams) {

        if (payload == null || payload.isEmpty() || resolvedNormParams == null || resolvedNormParams.isEmpty()) {
            logger.info("processFuelPriorityCppMonthWisePrices: payload or resolvedNormParams empty, skipping");
            return 0;
        }

        logger.info("processFuelPriorityCppMonthWisePrices: starting Step 5 for {} payload rows", payload.size());

        Map<NormsHeaderKey, List<UUID>> headerLookupCache = new HashMap<>();
        Map<UUID, UUID> fuelAvailPlantCache = new HashMap<>();
        Set<String> processedHeaderYears = new HashSet<>();
        int totalInserted = 0;

        for (AssetFuelPriorityDto dto : payload) {
            UUID assetId = dto.getAssetId();
            if (assetId == null) {
                continue;
            }

            Map<UUID, UUID> fuelToNormParamMap = resolvedNormParams.get(assetId);
            if (fuelToNormParamMap == null || fuelToNormParamMap.isEmpty()) {
                continue;
            }

            CPPAssetFuelPriority entity = entityMap != null && dto.getId() != null ? entityMap.get(dto.getId()) : null;
            String financialYear = resolveFinancialYear(dto, entity);
            if (financialYear == null || financialYear.isBlank()) {
                logger.warn("processFuelPriorityCppMonthWisePrices: skipping assetId={} because financialYear is missing", assetId);
                continue;
            }

            UUID cppPlantFkId = dto.getCppPlantFkId();
            UUID fuelAvailablePlantFkId = null;
            if (cppPlantFkId != null) {
                fuelAvailablePlantFkId = fuelAvailPlantCache.computeIfAbsent(cppPlantFkId, pid -> {
                    String mappingSql = "SELECT CAST(FuelAvailablePlantFKId AS varchar(36)) FROM dbo.CPPPlantFuelSourceMapping WHERE CPPPlantFKId = ?";
                    List<String> mappingResults = db1JdbcTemplate.queryForList(mappingSql, String.class, pid.toString());
                    if (mappingResults.isEmpty() || mappingResults.get(0) == null) {
                        throw new IllegalStateException("Missing CPPPlantFuelSourceMapping record for CPPPlantFKId: " + pid);
                    }
                    if (mappingResults.size() > 1) {
                        throw new IllegalStateException("Multiple CPPPlantFuelSourceMapping records found for CPPPlantFKId: " + pid);
                    }
                    return UUID.fromString(mappingResults.get(0));
                });
            }

            for (Map.Entry<UUID, UUID> entry : fuelToNormParamMap.entrySet()) {
                UUID fuelId = entry.getKey();
                UUID normParamId = entry.getValue();
                if (normParamId == null) {
                    continue;
                }

                MonthlyFuelPrices prices = (fuelAvailablePlantFkId != null && fuelId != null)
                        ? getMonthlyFuelPrices(fuelAvailablePlantFkId, fuelId, financialYear)
                        : MonthlyFuelPrices.zero();

                NormsHeaderKey key = new NormsHeaderKey(assetId, normParamId, false);
                List<UUID> normsHeaderIds = headerLookupCache.computeIfAbsent(key, k -> getNormsHeaderIds(assetId, normParamId));
                if (normsHeaderIds.isEmpty()) {
                    logger.warn("processFuelPriorityCppMonthWisePrices: no NormsHeader found for assetId={}, normParameterId={}",
                            assetId, normParamId);
                    continue;
                }

                for (UUID normsHeaderId : normsHeaderIds) {
                    String headerYearKey = normsHeaderId + ":" + financialYear;
                    if (!processedHeaderYears.add(headerYearKey)) {
                        continue;
                    }

                    int inserted = ensureCppMonthWisePrice(normsHeaderId, financialYear, prices);
                    totalInserted += inserted;
                }
            }
        }

        logger.info("processFuelPriorityCppMonthWisePrices: completed Step 5, inserted {} missing CPPMonthWisePrice records", totalInserted);
        return totalInserted;
    }

    public record FuelNormsHeaderCandidate(
            UUID normsHeaderId,
            UUID normParameterId,
            UUID fuelId,
            String fuelCode,
            String normParamName,
            boolean freeSteam
    ) {}

    public List<FuelNormsHeaderCandidate> getFuelNormsHeaderCandidates(UUID assetId) {
        if (assetId == null) {
            return Collections.emptyList();
        }
        String sql = "SELECT CAST(nh.Id AS varchar(36)) AS NormsHeaderId, " +
                "  CAST(nh.NormParameter_FK_Id AS varchar(36)) AS NormParameterId, " +
                "  CAST(fm.Id AS varchar(36)) AS FuelId, " +
                "  fm.FuelCode, " +
                "  np.Name AS NormParamName, " +
                "  CAST(ISNULL(nh.IsFreeSteamFuel, 0) AS bit) AS FreeSteam " +
                "FROM dbo.NormsHeader nh WITH(NOLOCK) " +
                "INNER JOIN dbo.NormParameters np WITH(NOLOCK) ON nh.NormParameter_FK_Id = np.Id " +
                "LEFT JOIN dbo.CPP_FuelMaster fm WITH(NOLOCK) ON fm.FuelCode = np.SAPMaterialCode " +
                "WHERE nh.AssetFKId = ? " +
                "  AND nh.AccountName = 'Raw Material' " +
                "  AND np.[Type] = 'CPPFuel'";

        return db1JdbcTemplate.query(sql, (rs, rowNum) -> new FuelNormsHeaderCandidate(
                UUID.fromString(rs.getString("NormsHeaderId")),
                rs.getString("NormParameterId") != null ? UUID.fromString(rs.getString("NormParameterId")) : null,
                rs.getString("FuelId") != null ? UUID.fromString(rs.getString("FuelId")) : null,
                rs.getString("FuelCode"),
                rs.getString("NormParamName"),
                rs.getBoolean("FreeSteam")
        ), assetId.toString());
    }

    public List<FuelNormsHeaderCandidate> getFuelNormsHeaderCandidates(UUID assetId, UUID rawMaterialTypeId) {
        return getFuelNormsHeaderCandidates(assetId);
    }

    @Override
    public int cleanUpUnusedFuelRecords(
            List<AssetFuelPriorityDto> payload,
            Map<UUID, CPPAssetFuelPriority> entityMap) {

        if (payload == null || payload.isEmpty()) {
            return 0;
        }

        int totalCleanedUp = 0;

        for (AssetFuelPriorityDto dto : payload) {
            UUID assetId = dto.getAssetId();
            if (assetId == null) {
                continue;
            }

            CPPAssetFuelPriority entity = entityMap != null && dto.getId() != null ? entityMap.get(dto.getId()) : null;
            String financialYear = resolveFinancialYear(dto, entity);
            if (financialYear == null || financialYear.isBlank()) {
                logger.warn("cleanUpUnusedFuelRecords: skipping assetId={} because financialYear is missing", assetId);
                continue;
            }

            Set<UUID> finalUsedFuelIds = extractUniqueFuelIds(dto);
            totalCleanedUp += cleanUpUnusedFuelRecords(assetId, financialYear, finalUsedFuelIds);
        }

        return totalCleanedUp;
    }

    @Override
    public int cleanUpUnusedFuelRecords(
            UUID assetId,
            String financialYear,
            Set<UUID> finalUsedFuelIds) {

        Set<UUID> usedFuelIds = finalUsedFuelIds != null ? finalUsedFuelIds : Collections.emptySet();
        int cleaned = cleanUpFuelHeaders(assetId, financialYear, usedFuelIds, false);
        if (assetId != null) {
            String linkedSql = "SELECT CAST(AssetId AS varchar(36)) FROM dbo.CPPSteamGenerationAsset " +
                    "WHERE LinkedPowerAsset_FK_ID = ?";
            List<String> linkedHrsgIds = db1JdbcTemplate.queryForList(linkedSql, String.class, assetId.toString());
            for (String hrsgId : linkedHrsgIds) {
                cleaned += cleanUpFuelHeaders(UUID.fromString(hrsgId), financialYear, usedFuelIds, true);
            }
        }
        return cleaned;
    }

    private int cleanUpFuelHeaders(UUID assetId, String financialYear, Set<UUID> finalUsedFuelIds, boolean freeSteamOnly) {

        if (assetId == null || financialYear == null || financialYear.isBlank()) {
            return 0;
        }

        FinancialYearRange range = parseAndValidateFinancialYear(financialYear);
        int startYear = range.startYear();
        int endYear = range.endYear();
        String fy = financialYear.trim();

        // 1. Fetch all fuel NormsHeaders for this asset where AccountName = 'Raw Material' and NormParameters.Type = 'CPPFuel'
        List<FuelNormsHeaderCandidate> candidates = getFuelNormsHeaderCandidates(assetId);
        if (candidates.isEmpty()) {
            logger.debug("cleanUpUnusedFuelRecords: no fuel NormsHeaders found for assetId={}", assetId);
            return 0;
        }

        Set<UUID> processedHeaders = new HashSet<>();
        int cleanedUpHeaders = 0;

        for (FuelNormsHeaderCandidate candidate : candidates) {
            if (candidate.freeSteam() != freeSteamOnly) {
                continue;
            }
            UUID normsHeaderId = candidate.normsHeaderId();
            if (normsHeaderId == null || !processedHeaders.add(normsHeaderId)) {
                continue;
            }

            UUID fuelId = candidate.fuelId();
            if (fuelId == null) {
                logger.warn("cleanUpUnusedFuelRecords: NormsHeader {} has unresolvable fuel ownership (SAPMaterialCode/FuelCode is unmapped). Skipping deletion for safety.",
                        normsHeaderId);
                continue;
            }

            // Check if fuel is used in ANY month (Apr-Mar) across Primary, Secondary, or Tertiary
            if (finalUsedFuelIds.contains(fuelId)) {
                logger.debug("cleanUpUnusedFuelRecords: fuel {} (FuelCode={}) is used in year {} for asset {} — retained",
                        fuelId, candidate.fuelCode(), fy, assetId);
                continue;
            }

            logger.info("cleanUpUnusedFuelRecords: candidate fuel {} (FuelCode={}, NormsHeader={}) is unused for asset {} in year {} — deleting obsolete records",
                    fuelId, candidate.fuelCode(), normsHeaderId, assetId, fy);

            // 1. Delete NormsMonthDetail for this financial year
            String deleteNmdSql = "DELETE nmd FROM dbo.NormsMonthDetail nmd " +
                    "INNER JOIN dbo.FinancialYearMonth fym WITH(NOLOCK) ON nmd.FinancialYearMonth_FK_Id = fym.Id " +
                    "WHERE nmd.NormsHeader_FK_Id = ? " +
                    "  AND ((fym.Year = ? AND fym.Month >= 4) OR (fym.Year = ? AND fym.Month <= 3))";
            int deletedNmd = db1JdbcTemplate.update(deleteNmdSql, normsHeaderId.toString(), startYear, endYear);
            logger.info("cleanUpUnusedFuelRecords: deleted {} NormsMonthDetail row(s) for NormsHeader={}, financialYear={}",
                    deletedNmd, normsHeaderId, fy);

            // 2. Delete CPPNorms for this financial year
            String deleteNormsSql = "DELETE FROM dbo.CPPNorms WHERE NormsHeader_FK_Id = ? AND FinancialYear = ?";
            int deletedNorms = db1JdbcTemplate.update(deleteNormsSql, normsHeaderId.toString(), fy);
            logger.info("cleanUpUnusedFuelRecords: deleted {} CPPNorms row(s) for NormsHeader={}, financialYear={}",
                    deletedNorms, normsHeaderId, fy);

            // 3. Delete CPPMonthWisePrice for this financial year
            String deletePriceSql = "DELETE FROM dbo.CPPMonthWisePrice WHERE NormsHeader_FK_Id = ? AND FinancialYear = ?";
            int deletedPrice = db1JdbcTemplate.update(deletePriceSql, normsHeaderId.toString(), fy);
            logger.info("cleanUpUnusedFuelRecords: deleted {} CPPMonthWisePrice row(s) for NormsHeader={}, financialYear={}",
                    deletedPrice, normsHeaderId, fy);

            // 4. Check if NormsHeader has any remaining child records in other financial years
            String checkNmdSql = "SELECT COUNT(1) FROM dbo.NormsMonthDetail WITH(NOLOCK) WHERE NormsHeader_FK_Id = ?";
            Integer remNmd = db1JdbcTemplate.queryForObject(checkNmdSql, Integer.class, normsHeaderId.toString());

            String checkNormsSql = "SELECT COUNT(1) FROM dbo.CPPNorms WITH(NOLOCK) WHERE NormsHeader_FK_Id = ?";
            Integer remNorms = db1JdbcTemplate.queryForObject(checkNormsSql, Integer.class, normsHeaderId.toString());

            String checkPriceSql = "SELECT COUNT(1) FROM dbo.CPPMonthWisePrice WITH(NOLOCK) WHERE NormsHeader_FK_Id = ?";
            Integer remPrice = db1JdbcTemplate.queryForObject(checkPriceSql, Integer.class, normsHeaderId.toString());

            boolean hasRemainingRecords = (remNmd != null && remNmd > 0)
                    || (remNorms != null && remNorms > 0)
                    || (remPrice != null && remPrice > 0);

            if (hasRemainingRecords) {
                logger.info("cleanUpUnusedFuelRecords: NormsHeader {} retained because it is still referenced by other records/years (remNmd={}, remNorms={}, remPrice={})",
                        normsHeaderId, remNmd, remNorms, remPrice);
            } else {
                String deleteHeaderSql = "DELETE FROM dbo.NormsHeader WHERE Id = ? AND AssetFKId = ? AND CPP_SR_Mapping_Master_Fk_Id IS NULL";
                int deletedHeader = db1JdbcTemplate.update(deleteHeaderSql, normsHeaderId.toString(), assetId.toString());
                logger.info("cleanUpUnusedFuelRecords: deleted unreferenced NormsHeader {} for assetId={}, rowsAffected={}",
                        normsHeaderId, assetId, deletedHeader);
            }

            cleanedUpHeaders++;
        }

        return cleanedUpHeaders;
    }

    @Override
    @Transactional
    public AOPMessageVM updateAssetFuelPriority(List<AssetFuelPriorityDto> payload) {
        AOPMessageVM response = new AOPMessageVM();

        if (payload == null || payload.isEmpty()) {
            response.setCode(400);
            response.setMessage("Payload is empty");
            return response;
        }

        try {
            logger.info("updateAssetFuelPriority: started with payload size {}", payload.size());

            // ── PASS 1: Validate entire payload and preload existing entities ────────
            Map<UUID, CPPAssetFuelPriority> entityMap = new LinkedHashMap<>();

            for (AssetFuelPriorityDto dto : payload) {
                if (dto.getId() == null) {
                    throw new IllegalArgumentException("Priority record ID (id) is required for all rows");
                }
                if (dto.getAssetId() == null) {
                    throw new IllegalArgumentException("Asset ID (assetId) is required for priority row ID: " + dto.getId());
                }
                if (dto.getCppPlantFkId() == null) {
                    throw new IllegalArgumentException("Plant ID (cppPlantFkId) is required for asset ID: " + dto.getAssetId());
                }

                CPPAssetFuelPriority entity = assetFuelPriorityRepository.findById(dto.getId()).orElse(null);
                if (entity == null) {
                    throw new IllegalArgumentException("CPPAssetFuelPriority record not found for ID: " + dto.getId());
                }
                entityMap.put(dto.getId(), entity);
            }

            logger.info("updateAssetFuelPriority: Pass 1 validation succeeded for {} entities", entityMap.size());

            // ── PASS 2: Process NormParameters and update priority entities ──────────
            Map<UUID, Map<UUID, UUID>> resolvedNormParams = processFuelPriorityNormParameters(payload);
            logger.info("updateAssetFuelPriority: resolved NormParameters for {} assets", resolvedNormParams.size());

            // ── STEP 3: Ensure 12 NormMonthDetail records for each relevant NormsHeader ───
            int monthDetailsInserted = processFuelPriorityNormMonthDetails(payload, entityMap, resolvedNormParams);
            logger.info("updateAssetFuelPriority: ensured NormMonthDetails for {} payload rows (inserted: {})",
                    payload.size(), monthDetailsInserted);

            // ── STEP 4: Ensure CPPNorms records for each relevant NormsHeader ──────────────
            int cppNormsInserted = processFuelPriorityCppNorms(payload, entityMap, resolvedNormParams);
            logger.info("updateAssetFuelPriority: ensured CPPNorms for {} payload rows (inserted: {})",
                    payload.size(), cppNormsInserted);

            // ── STEP 5: Ensure CPPMonthWisePrice records for each relevant NormsHeader ────
            int monthWisePriceInserted = processFuelPriorityCppMonthWisePrices(payload, entityMap, resolvedNormParams);
            logger.info("updateAssetFuelPriority: ensured CPPMonthWisePrice for {} payload rows (inserted: {})",
                    payload.size(), monthWisePriceInserted);

            // ── STEP 6: Clean up obsolete fuel records for each relevant asset and financial year ──
            int cleanedUpCount = cleanUpUnusedFuelRecords(payload, entityMap);
            logger.info("updateAssetFuelPriority: cleaned up {} obsolete fuel candidate(s)", cleanedUpCount);

            int updated = 0;
            for (AssetFuelPriorityDto dto : payload) {
                CPPAssetFuelPriority entity = entityMap.get(dto.getId());

                // April  — entity fields are String (nvarchar), DTO fields are UUID
                entity.setAprPrimary(dto.getAprPrimary()     != null ? dto.getAprPrimary().toString()     : null);
                entity.setAprSecondary(dto.getAprSecondary() != null ? dto.getAprSecondary().toString()   : null);
                entity.setAprTertiary(dto.getAprTertiary()   != null ? dto.getAprTertiary().toString()    : null);
                // May
                entity.setMayPrimary(dto.getMayPrimary()     != null ? dto.getMayPrimary().toString()     : null);
                entity.setMaySecondary(dto.getMaySecondary() != null ? dto.getMaySecondary().toString()   : null);
                entity.setMayTertiary(dto.getMayTertiary()   != null ? dto.getMayTertiary().toString()    : null);
                // June
                entity.setJunPrimary(dto.getJunPrimary()     != null ? dto.getJunPrimary().toString()     : null);
                entity.setJunSecondary(dto.getJunSecondary() != null ? dto.getJunSecondary().toString()   : null);
                entity.setJunTertiary(dto.getJunTertiary()   != null ? dto.getJunTertiary().toString()    : null);
                // July
                entity.setJulPrimary(dto.getJulPrimary()     != null ? dto.getJulPrimary().toString()     : null);
                entity.setJulSecondary(dto.getJulSecondary() != null ? dto.getJulSecondary().toString()   : null);
                entity.setJulTertiary(dto.getJulTertiary()   != null ? dto.getJulTertiary().toString()    : null);
                // August
                entity.setAugPrimary(dto.getAugPrimary()     != null ? dto.getAugPrimary().toString()     : null);
                entity.setAugSecondary(dto.getAugSecondary() != null ? dto.getAugSecondary().toString()   : null);
                entity.setAugTertiary(dto.getAugTertiary()   != null ? dto.getAugTertiary().toString()    : null);
                // September
                entity.setSepPrimary(dto.getSepPrimary()     != null ? dto.getSepPrimary().toString()     : null);
                entity.setSepSecondary(dto.getSepSecondary() != null ? dto.getSepSecondary().toString()   : null);
                entity.setSepTertiary(dto.getSepTertiary()   != null ? dto.getSepTertiary().toString()    : null);
                // October
                entity.setOctPrimary(dto.getOctPrimary()     != null ? dto.getOctPrimary().toString()     : null);
                entity.setOctSecondary(dto.getOctSecondary() != null ? dto.getOctSecondary().toString()   : null);
                entity.setOctTertiary(dto.getOctTertiary()   != null ? dto.getOctTertiary().toString()    : null);
                // November
                entity.setNovPrimary(dto.getNovPrimary()     != null ? dto.getNovPrimary().toString()     : null);
                entity.setNovSecondary(dto.getNovSecondary() != null ? dto.getNovSecondary().toString()   : null);
                entity.setNovTertiary(dto.getNovTertiary()   != null ? dto.getNovTertiary().toString()    : null);
                // December
                entity.setDecPrimary(dto.getDecPrimary()     != null ? dto.getDecPrimary().toString()     : null);
                entity.setDecSecondary(dto.getDecSecondary() != null ? dto.getDecSecondary().toString()   : null);
                entity.setDecTertiary(dto.getDecTertiary()   != null ? dto.getDecTertiary().toString()    : null);
                // January
                entity.setJanPrimary(dto.getJanPrimary()     != null ? dto.getJanPrimary().toString()     : null);
                entity.setJanSecondary(dto.getJanSecondary() != null ? dto.getJanSecondary().toString()   : null);
                entity.setJanTertiary(dto.getJanTertiary()   != null ? dto.getJanTertiary().toString()    : null);
                // February
                entity.setFebPrimary(dto.getFebPrimary()     != null ? dto.getFebPrimary().toString()     : null);
                entity.setFebSecondary(dto.getFebSecondary() != null ? dto.getFebSecondary().toString()   : null);
                entity.setFebTertiary(dto.getFebTertiary()   != null ? dto.getFebTertiary().toString()    : null);
                // March
                entity.setMarPrimary(dto.getMarPrimary()     != null ? dto.getMarPrimary().toString()     : null);
                entity.setMarSecondary(dto.getMarSecondary() != null ? dto.getMarSecondary().toString()   : null);
                entity.setMarTertiary(dto.getMarTertiary()   != null ? dto.getMarTertiary().toString()    : null);

                entity.setRemarks(dto.getRemarks());
                entity.setModifiedDate(LocalDateTime.now());

                assetFuelPriorityRepository.save(entity);
                updated++;
            }

            logger.info("updateAssetFuelPriority: completed successfully, updated {} entities", updated);
            response.setCode(200);
            response.setMessage("Asset fuel priority updated. Updated: " + updated);
            return response;

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.error("updateAssetFuelPriority validation error: {}", e.getMessage());
            try {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            } catch (Exception te) {
                logger.warn("Could not set rollback-only: {}", te.getMessage());
            }
            response.setCode(400);
            response.setMessage("Validation error: " + e.getMessage());
            return response;
        } catch (Exception e) {
            logger.error("updateAssetFuelPriority error: {}", e.getMessage(), e);
            try {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            } catch (Exception te) {
                logger.warn("Could not set rollback-only: {}", te.getMessage());
            }
            response.setCode(500);
            response.setMessage("Error: " + e.getMessage());
            return response;
        }
    }

    @Override
    public List<CompatibleFuelAssetDto> getCompatibleFuelAssets(String plantIds) {
        if (plantIds == null || plantIds.trim().isEmpty()) {
            return repository.getCompatibleFuelAssets()
                    .stream().map(this::toCompatibleFuelAssetDto).toList();
        }
        return repository.getCompatibleFuelAssetsByPlants(plantIds)
                .stream().map(this::toCompatibleFuelAssetDto).toList();
    }

    private CompatibleFuelAssetDto toCompatibleFuelAssetDto(CompatibleFuelAssetProjection p) {
        CompatibleFuelAssetDto dto = new CompatibleFuelAssetDto();
        if (p.getAssetId() != null) {
            dto.setAssetId(UUID.fromString(p.getAssetId()));
        }
        dto.setAssetName(p.getAssetName());
        dto.setAssetType(p.getAssetType());
        if (p.getCppPlantFkId() != null) {
            dto.setCppPlantFkId(UUID.fromString(p.getCppPlantFkId()));
        }
        dto.setPlantName(p.getPlantName());
        dto.setAssetCategory(p.getAssetCategory());
        dto.setCompatibleFuel(p.getCompatibleFuel());
        return dto;
    }

    @Override
    public AOPMessageVM updateCompatibleFuelAssets(List<CompatibleFuelAssetDto> payload) {
        AOPMessageVM response = new AOPMessageVM();
        int updated = 0;
        int skipped = 0;

        if (payload == null || payload.isEmpty()) {
            response.setCode(400);
            response.setMessage("Payload is empty");
            return response;
        }

        for (CompatibleFuelAssetDto dto : payload) {
            if (dto.getAssetId() == null) {
                skipped++;
                continue;
            }
            if (dto.getCompatibleFuel() == null || dto.getCompatibleFuel().trim().isEmpty()) {
                skipped++;
                continue;
            }

            int rows = 0;
            if ("Power".equalsIgnoreCase(dto.getAssetCategory())) {
                rows = repository.updatePowerAssetCompatibleFuel(
                        dto.getAssetId().toString(),
                        dto.getCompatibleFuel());
            } else if ("Steam".equalsIgnoreCase(dto.getAssetCategory())) {
                rows = repository.updateSteamAssetCompatibleFuel(
                        dto.getAssetId().toString(),
                        dto.getCompatibleFuel());
            } else {
                skipped++;
                continue;
            }

            if (rows > 0) {
                updated++;
            } else {
                skipped++;
            }
        }

        response.setCode(200);
        response.setMessage("Compatible fuel assets updated. Updated: " + updated + ", Skipped: " + skipped);
        return response;
    }

    /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ */
    // Asset Fuel Priority — Excel export / import
    //
    // Layout of the generated workbook:
    //   Sheet "Asset Fuel Priority" (visible, protected)
    //     row 0  : merged group headers — "CPP Plant" | "Asset Name" | Apr-25..Mar-26 | "Remarks"
    //     row 1  : sub headers — Primary/Secondary/Tertiary per month + hidden
    //              technical columns id / assetId / cppPlantFkId
    //     row 2+ : one row per asset; fuel cells show the fuel display name and
    //              have a dropdown fed from the hidden "FuelLists" sheet
    //   Sheet "FuelLists" (hidden)
    //     one column per CPP plant; rows hold that plant's available fuel names
    //     (same source as the UI's plantFuelMap — fuel availability mapped
    //     through CPPPlantFuelSourceMapping). One list per plant since the fuel
    //     set is identical across months.
    //
    // Columns: 0=CPP Plant, 1=Asset Name,
    //          fuel cells = 2 + monthIndex*3 + priorityIndex (2..37),
    //          Remarks = 38, hidden = 39 (id), 40 (assetId), 41 (cppPlantFkId)
    /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ */

    private static final String[] EXCEL_PRIORITY_SUFFIX = {"Primary", "Secondary", "Tertiary"};
    private static final int EXCEL_PLANT_COL = 0;
    private static final int EXCEL_ASSET_COL = 1;
    private static final int EXCEL_REMARKS_COL = 38;
    private static final int EXCEL_ID_COL = 39;
    private static final int EXCEL_ASSET_ID_COL = 40;
    private static final int EXCEL_PLANT_ID_COL = 41;
    private static final int EXCEL_TOTAL_COLS = 42;
    private static final int EXCEL_FUEL_COL_START = 2;
    private static final int EXCEL_FUEL_COL_END = 37;
    private static final int EXCEL_DATA_START_ROW = 2;
    private static final String EXCEL_LISTS_SHEET = "FuelLists";

    private record AssetPriorityImportRow(
            UUID id,
            UUID assetId,
            UUID cppPlantFkId,
            String plantName,
            String assetName,
            String remarks,
            String[] fuelNames,
            List<String> errors
    ) {}

    @Override
    public byte[] exportAssetFuelPriorityExcel(List<UUID> plantIds, String financialYear, String fileName) throws IOException {
        if (plantIds == null || plantIds.isEmpty()) {
            throw new IllegalArgumentException("plantIds cannot be null or empty");
        }
        if (financialYear == null || financialYear.isBlank()) {
            throw new IllegalArgumentException("financialYear cannot be null or blank");
        }

        String plantIdsCsv = plantIds.stream().map(UUID::toString).collect(Collectors.joining(","));

        List<AssetFuelPriorityDto> dtos = getAssetFuelPriority(plantIdsCsv, financialYear)
                .stream()
                .filter(d -> d.getAssetType() == null
                        || (!"PRDS".equalsIgnoreCase(d.getAssetType()) && !"STG".equalsIgnoreCase(d.getAssetType())))
                .toList();

        Map<UUID, String> fuelDisplayNames = loadFuelDisplayNames();
        Map<UUID, List<String>> plantFuelOptions = loadPlantFuelOptions(plantIdsCsv, financialYear);
        Map<UUID, List<String>> plantNameVariants = loadPlantNameVariants(plantIdsCsv);
        Map<UUID, String> plantDisplayNames = plantNameVariants.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0), (a, b) -> a, LinkedHashMap::new));

        return generateAssetPriorityExcel(dtos, fuelDisplayNames, plantFuelOptions, plantDisplayNames, plantNameVariants, financialYear);
    }

    private Map<UUID, String> loadFuelDisplayNames() {
        String sql = "SELECT CAST(Id AS varchar(36)), FuelDisplayName, FuelName FROM dbo.CPP_FuelMaster WITH(NOLOCK)";
        List<Object[]> rows = db1JdbcTemplate.query(sql, (rs, rowNum) -> new Object[]{
                rs.getString(1), rs.getString(2), rs.getString(3)
        });
        Map<UUID, String> map = new LinkedHashMap<>();
        for (Object[] r : rows) {
            if (r[0] == null) continue;
            UUID id = UUID.fromString(r[0].toString());
            String name = r[1] != null && !r[1].toString().isBlank() ? r[1].toString() : (r[2] != null ? r[2].toString() : null);
            if (name != null) map.put(id, name);
        }
        return map;
    }

    /** cppPlantFkId -> ordered unique fuel display names available to that plant (same fuels every month). */
    private Map<UUID, List<String>> loadPlantFuelOptions(String plantIdsCsv, String financialYear) {
        String sql =
                "SELECT DISTINCT m.CPPPlantFKId, fm.FuelDisplayName, fm.FuelName " +
                "FROM dbo.CPPFuelAvailabilityTransaction fat WITH(NOLOCK) " +
                "JOIN dbo.CPPPlantFuelSourceMapping m WITH(NOLOCK) " +
                "       ON m.FuelAvailablePlantFKId = fat.CPPPlantFKId " +
                "JOIN dbo.CPP_FuelMaster fm WITH(NOLOCK) ON fm.Id = fat.Fuel_FK_Id " +
                "WHERE fat.[Type] = 'FUEL_AVAILABILITY' " +
                "  AND fat.FinancialYear = ? " +
                "  AND m.CPPPlantFKId IN (SELECT TRY_CAST(value AS UNIQUEIDENTIFIER) FROM STRING_SPLIT(?, ','))";
        List<Object[]> rows = db1JdbcTemplate.query(sql, (rs, rowNum) -> new Object[]{
                rs.getString(1), rs.getString(2), rs.getString(3)
        }, financialYear, plantIdsCsv);
        Map<UUID, LinkedHashMap<String, Boolean>> ordered = new LinkedHashMap<>();
        for (Object[] r : rows) {
            if (r[0] == null) continue;
            UUID plantId = UUID.fromString(r[0].toString());
            String name = r[1] != null && !r[1].toString().isBlank() ? r[1].toString()
                    : (r[2] != null ? r[2].toString() : null);
            if (name == null || name.isBlank()) continue;
            ordered.computeIfAbsent(plantId, k -> new LinkedHashMap<>()).putIfAbsent(name.trim(), Boolean.TRUE);
        }
        Map<UUID, List<String>> result = new LinkedHashMap<>();
        ordered.forEach((k, v) -> result.put(k, new ArrayList<>(v.keySet())));
        return result;
    }

    private Map<UUID, String> loadPlantDisplayNames(String plantIdsCsv) {
        return loadPlantNameVariants(plantIdsCsv).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0), (a, b) -> a, LinkedHashMap::new));
    }

    /** plantId -> all known name variants (DisplayName preferred first, then Name). */
    private Map<UUID, List<String>> loadPlantNameVariants(String plantIdsCsv) {
        String sql = "SELECT CAST(Id AS varchar(36)), DisplayName, Name FROM dbo.Plants WITH(NOLOCK) " +
                "WHERE Id IN (SELECT TRY_CAST(value AS UNIQUEIDENTIFIER) FROM STRING_SPLIT(?, ','))";
        List<Object[]> rows = db1JdbcTemplate.query(sql, (rs, rowNum) -> new Object[]{
                rs.getString(1), rs.getString(2), rs.getString(3)
        }, plantIdsCsv);
        Map<UUID, LinkedHashMap<String, Boolean>> variants = new LinkedHashMap<>();
        for (Object[] r : rows) {
            if (r[0] == null) continue;
            UUID id = UUID.fromString(r[0].toString());
            LinkedHashMap<String, Boolean> names = variants.computeIfAbsent(id, k -> new LinkedHashMap<>());
            if (r[1] != null && !r[1].toString().isBlank()) names.putIfAbsent(r[1].toString().trim(), Boolean.TRUE);
            if (r[2] != null && !r[2].toString().isBlank()) names.putIfAbsent(r[2].toString().trim(), Boolean.TRUE);
            if (names.isEmpty()) names.put(r[0].toString(), Boolean.TRUE);
        }
        Map<UUID, List<String>> result = new LinkedHashMap<>();
        variants.forEach((k, v) -> result.put(k, new ArrayList<>(v.keySet())));
        return result;
    }

    /** plantId -> (lower-cased fuel name -> set of fuel ids). Used to resolve dropdown names back to UUIDs on import. */
    private Map<UUID, Map<String, Set<UUID>>> loadPlantFuelNameIndex(String plantIdsCsv, String financialYear) {
        String sql =
                "SELECT DISTINCT m.CPPPlantFKId, fat.Fuel_FK_Id, fm.FuelDisplayName, fm.FuelName " +
                "FROM dbo.CPPFuelAvailabilityTransaction fat WITH(NOLOCK) " +
                "JOIN dbo.CPPPlantFuelSourceMapping m WITH(NOLOCK) " +
                "       ON m.FuelAvailablePlantFKId = fat.CPPPlantFKId " +
                "JOIN dbo.CPP_FuelMaster fm WITH(NOLOCK) ON fm.Id = fat.Fuel_FK_Id " +
                "WHERE fat.[Type] = 'FUEL_AVAILABILITY' " +
                "  AND fat.FinancialYear = ? " +
                "  AND m.CPPPlantFKId IN (SELECT TRY_CAST(value AS UNIQUEIDENTIFIER) FROM STRING_SPLIT(?, ','))";
        List<Object[]> rows = db1JdbcTemplate.query(sql, (rs, rowNum) -> new Object[]{
                rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)
        }, financialYear, plantIdsCsv);
        Map<UUID, Map<String, Set<UUID>>> index = new HashMap<>();
        for (Object[] r : rows) {
            if (r[0] == null || r[1] == null) continue;
            UUID plantId = UUID.fromString(r[0].toString());
            UUID fuelId = UUID.fromString(r[1].toString());
            addFuelNameKeys(index.computeIfAbsent(plantId, k -> new HashMap<>()), r[2], r[3], fuelId);
        }
        return index;
    }

    /** Global fallback name -> fuel ids (display name + fuel name), used when a name is not in the plant's availability list. */
    private Map<String, Set<UUID>> loadGlobalFuelNameIndex() {
        String sql = "SELECT CAST(Id AS varchar(36)), FuelDisplayName, FuelName FROM dbo.CPP_FuelMaster WITH(NOLOCK)";
        List<Object[]> rows = db1JdbcTemplate.query(sql, (rs, rowNum) -> new Object[]{
                rs.getString(1), rs.getString(2), rs.getString(3)
        });
        Map<String, Set<UUID>> index = new HashMap<>();
        for (Object[] r : rows) {
            if (r[0] == null) continue;
            UUID fuelId = UUID.fromString(r[0].toString());
            addFuelNameKeys(index, r[1], r[2], fuelId);
        }
        return index;
    }

    private void addFuelNameKeys(Map<String, Set<UUID>> index, Object displayName, Object fuelName, UUID fuelId) {
        for (Object n : new Object[]{displayName, fuelName}) {
            if (n == null) continue;
            String key = n.toString().trim().toLowerCase();
            if (key.isEmpty()) continue;
            index.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(fuelId);
        }
    }

    private static String cap(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private UUID getPriorityFuelId(AssetFuelPriorityDto dto, int monthIdx, int priIdx) {
        if (dto == null) return null;
        try {
            Method getter = AssetFuelPriorityDto.class.getMethod(
                    "get" + cap(FiscalYearMonths.MONTH_KEYS[monthIdx]) + EXCEL_PRIORITY_SUFFIX[priIdx]);
            return (UUID) getter.invoke(dto);
        } catch (Exception e) {
            return null;
        }
    }

    private void setPriorityFuelId(AssetFuelPriorityDto dto, int monthIdx, int priIdx, UUID fuelId) {
        try {
            Method setter = AssetFuelPriorityDto.class.getMethod(
                    "set" + cap(FiscalYearMonths.MONTH_KEYS[monthIdx]) + EXCEL_PRIORITY_SUFFIX[priIdx], UUID.class);
            setter.invoke(dto, fuelId);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set fuel field for month " + monthIdx + " priority " + priIdx, e);
        }
    }

    private byte[] generateAssetPriorityExcel(
            List<AssetFuelPriorityDto> dtos,
            Map<UUID, String> fuelDisplayNames,
            Map<UUID, List<String>> plantFuelOptions,
            Map<UUID, String> plantDisplayNames,
            Map<UUID, List<String>> plantNameVariants,
            String financialYear) throws IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Asset Fuel Priority");

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(workbook);
        CellStyle lockedStyle = ExcelStyles.createLockedStyle(workbook);
        CellStyle unlockedStyle = ExcelStyles.createUnlockedStyle(workbook);
        CellStyle remarksStyle = ExcelStyles.createEditableRemarksStyle(workbook);
        CellStyle listHeaderStyle = ExcelStyles.createHeaderStyle(workbook);
        CellStyle listDataStyle = ExcelStyles.createDataStyle(workbook);

        String[] monthHeaders = FiscalYearMonths.getMonthHeaders(financialYear);

        // ---- Row 0: merged group headers ----
        Row groupHeader = sheet.createRow(0);
        ExcelCells.setString(groupHeader.createCell(EXCEL_PLANT_COL), "CPP Plant", headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 1, EXCEL_PLANT_COL, EXCEL_PLANT_COL));
        ExcelCells.setString(groupHeader.createCell(EXCEL_ASSET_COL), "Asset Name", headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 1, EXCEL_ASSET_COL, EXCEL_ASSET_COL));
        for (int m = 0; m < 12; m++) {
            int firstCol = EXCEL_FUEL_COL_START + m * 3;
            ExcelCells.setString(groupHeader.createCell(firstCol), monthHeaders[m], headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, firstCol, firstCol + 2));
        }
        ExcelCells.setString(groupHeader.createCell(EXCEL_REMARKS_COL), "Remarks", headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 1, EXCEL_REMARKS_COL, EXCEL_REMARKS_COL));

        // ---- Row 1: sub headers + hidden technical columns ----
        Row subHeader = sheet.createRow(1);
        for (int m = 0; m < 12; m++) {
            for (int p = 0; p < 3; p++) {
                ExcelCells.setString(subHeader.createCell(EXCEL_FUEL_COL_START + m * 3 + p),
                        EXCEL_PRIORITY_SUFFIX[p], headerStyle);
            }
        }
        ExcelCells.setString(subHeader.createCell(EXCEL_ID_COL), "id", headerStyle);
        ExcelCells.setString(subHeader.createCell(EXCEL_ASSET_ID_COL), "assetId", headerStyle);
        ExcelCells.setString(subHeader.createCell(EXCEL_PLANT_ID_COL), "cppPlantFkId", headerStyle);

        // ---- Hidden FuelLists sheet: one column per plant ----
        Sheet listsSheet = workbook.createSheet(EXCEL_LISTS_SHEET);
        Row listsHeader = listsSheet.createRow(0);
        ExcelCells.setString(listsHeader.createCell(0), "Plant", listHeaderStyle);
        Map<UUID, Integer> plantListCol = new LinkedHashMap<>();
        Map<Integer, List<String>> optionsByCol = new HashMap<>();
        Map<Integer, Set<String>> namesByCol = new HashMap<>();
        // lower-cased plant name (every known variant) -> FuelLists column,
        // used as a fallback when a row's cppPlantFkId doesn't match a
        // mapping key (mirrors the UI's plantFuelMap plantName fallback)
        Map<String, Integer> plantListColByName = new HashMap<>();
        int listCol = 1;
        for (Map.Entry<UUID, List<String>> e : plantFuelOptions.entrySet()) {
            plantListCol.put(e.getKey(), listCol);
            String plantName = plantDisplayNames.getOrDefault(e.getKey(), e.getKey().toString());
            ExcelCells.setString(listsHeader.createCell(listCol), plantName, listHeaderStyle);
            List<String> names = e.getValue();
            optionsByCol.put(listCol, names);
            Set<String> lowerNames = names.stream()
                    .filter(Objects::nonNull).map(n -> n.trim().toLowerCase())
                    .collect(Collectors.toSet());
            namesByCol.put(listCol, lowerNames);
            List<String> variants = plantNameVariants.getOrDefault(e.getKey(), List.of(plantName));
            for (String variant : variants) {
                plantListColByName.putIfAbsent(variant.trim().toLowerCase(), listCol);
            }
            plantListColByName.putIfAbsent(plantName.trim().toLowerCase(), listCol);
            for (int i = 0; i < names.size(); i++) {
                Row listRow = listsSheet.getRow(i + 1);
                if (listRow == null) listRow = listsSheet.createRow(i + 1);
                ExcelCells.setString(listRow.createCell(listCol), names.get(i), listDataStyle);
            }
            listCol++;
        }

        // ---- Data rows ----
        XSSFDataValidationHelper dvHelper = new XSSFDataValidationHelper((XSSFSheet) sheet);
        int rowIdx = EXCEL_DATA_START_ROW;
        for (AssetFuelPriorityDto dto : dtos) {
            Row row = sheet.createRow(rowIdx);
            ExcelCells.setString(row.createCell(EXCEL_PLANT_COL), dto.getPlantName(), lockedStyle);
            ExcelCells.setString(row.createCell(EXCEL_ASSET_COL), dto.getAssetName(), lockedStyle);

            Set<String> rowFuelNames = new HashSet<>();
            for (int m = 0; m < 12; m++) {
                for (int p = 0; p < 3; p++) {
                    UUID fuelId = getPriorityFuelId(dto, m, p);
                    String name = fuelId != null ? fuelDisplayNames.get(fuelId) : null;
                    if (name != null) rowFuelNames.add(name.trim().toLowerCase());
                    ExcelCells.setString(row.createCell(EXCEL_FUEL_COL_START + m * 3 + p), name, unlockedStyle);
                }
            }

            ExcelCells.setString(row.createCell(EXCEL_REMARKS_COL), dto.getRemarks(), remarksStyle);
            ExcelCells.setString(row.createCell(EXCEL_ID_COL), dto.getId() != null ? dto.getId().toString() : null, lockedStyle);
            ExcelCells.setString(row.createCell(EXCEL_ASSET_ID_COL), dto.getAssetId() != null ? dto.getAssetId().toString() : null, lockedStyle);
            ExcelCells.setString(row.createCell(EXCEL_PLANT_ID_COL), dto.getCppPlantFkId() != null ? dto.getCppPlantFkId().toString() : null, lockedStyle);

            // Dropdown for this row — the plant's fuel list (same for all months).
            // Resolve the FuelLists column by cppPlantFkId first, then by
            // plant name (same fallback order as the UI's plantFuelMap lookup).
            Integer lc = dto.getCppPlantFkId() != null ? plantListCol.get(dto.getCppPlantFkId()) : null;
            if (lc == null && dto.getPlantName() != null) {
                lc = plantListColByName.get(dto.getPlantName().trim().toLowerCase());
            }
            // Last resort: pick the plant column whose fuel list best matches
            // the fuels already saved on this row
            if (lc == null && !rowFuelNames.isEmpty()) {
                int bestCol = -1, bestMatches = 0;
                for (Map.Entry<Integer, Set<String>> e : namesByCol.entrySet()) {
                    int matches = 0;
                    for (String n : rowFuelNames) if (e.getValue().contains(n)) matches++;
                    if (matches > bestMatches) { bestMatches = matches; bestCol = e.getKey(); }
                }
                if (bestCol > 0) lc = bestCol;
            }
            List<String> options = lc != null ? optionsByCol.get(lc) : null;
            if (options != null && !options.isEmpty()) {
                String colLetter = CellReference.convertNumToColString(lc);
                String formula = "'" + EXCEL_LISTS_SHEET + "'!$" + colLetter + "$2:$" + colLetter + "$" + (options.size() + 1);
                DataValidationConstraint constraint = dvHelper.createFormulaListConstraint(formula);
                CellRangeAddressList range = new CellRangeAddressList(rowIdx, rowIdx, EXCEL_FUEL_COL_START, EXCEL_FUEL_COL_END);
                DataValidation validation = dvHelper.createValidation(constraint, range);
                validation.setShowErrorBox(true);
                validation.setEmptyCellAllowed(true);
                sheet.addValidationData(validation);
            }
            rowIdx++;
        }

        ExcelColumns.hideColumns(sheet, EXCEL_ID_COL, EXCEL_ASSET_ID_COL, EXCEL_PLANT_ID_COL);
        ExcelColumns.autoSize(sheet, EXCEL_TOTAL_COLS, EXCEL_REMARKS_COL);

        // Freeze CPP Plant + Asset Name columns and both header rows so they
        // stay visible while scrolling the 36 fuel columns
        sheet.createFreezePane(EXCEL_FUEL_COL_START, EXCEL_DATA_START_ROW);

        // AutoFilter gives sort + filter dropdowns. The range starts at the
        // sub-header row (row 1) because row 0's merged month-group headers
        // are incompatible with filtering.
        int lastDataRow = Math.max(EXCEL_DATA_START_ROW, rowIdx - 1);
        if (lastDataRow >= EXCEL_DATA_START_ROW) {
            sheet.setAutoFilter(new CellRangeAddress(
                    EXCEL_DATA_START_ROW - 1, lastDataRow, 0, EXCEL_TOTAL_COLS - 1));
        }

        // Protect the main sheet (AutoFilter still works on a protected sheet)
        sheet.protectSheet("");
        XSSFSheet xssfSheet = (XSSFSheet) sheet;
        xssfSheet.enableLocking();
        xssfSheet.lockSelectLockedCells(false);
        xssfSheet.lockSelectUnlockedCells(false);
        xssfSheet.lockFormatColumns(false);
        xssfSheet.lockFormatRows(false);

        workbook.setSheetHidden(1, true);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    @Override
    @Transactional
    public AOPMessageVM importAssetFuelPriority(List<UUID> plantIds, String financialYear, MultipartFile file) throws IOException {
        AOPMessageVM vm = new AOPMessageVM();

        if (plantIds == null || plantIds.isEmpty()) {
            vm.setCode(400);
            vm.setMessage("plantIds cannot be null or empty");
            return vm;
        }
        if (financialYear == null || financialYear.isBlank()) {
            vm.setCode(400);
            vm.setMessage("financialYear cannot be null or empty");
            return vm;
        }
        if (file == null || file.isEmpty()) {
            vm.setCode(400);
            vm.setMessage("File cannot be empty");
            return vm;
        }

        String plantIdsCsv = plantIds.stream().map(UUID::toString).collect(Collectors.joining(","));

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getRow(0) == null) {
                vm.setCode(400);
                vm.setMessage("Invalid Excel file: no header row found");
                return vm;
            }

            // ---- Locate columns ----
            int assetCol = -1, remarksCol = -1, idCol = -1, assetIdCol = -1, plantCol = -1, plantNameCol = -1;
            Row row0 = sheet.getRow(0);
            Row row1 = sheet.getRow(1);
            for (Row headerRow : new Row[]{row0, row1}) {
                if (headerRow == null) continue;
                for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                    String label = ExcelCells.toString(headerRow.getCell(c));
                    if (label == null) continue;
                    String l = label.toLowerCase();
                    if (l.equals("cpp plant")) plantNameCol = c;
                    else if (l.equals("asset name")) assetCol = c;
                    else if (l.equals("remarks")) remarksCol = c;
                    else if (l.equals("id")) idCol = c;
                    else if (l.equals("assetid")) assetIdCol = c;
                    else if (l.equals("cppplantfkid")) plantCol = c;
                }
            }
            if (assetCol < 0) {
                vm.setCode(400);
                vm.setMessage("Invalid Excel file: 'Asset Name' column not found");
                return vm;
            }

            // Fuel columns come from row 1 sub-headers, in column order:
            // every 3 columns = one month (Primary, Secondary, Tertiary), Apr -> Mar
            List<int[]> fuelCols = new ArrayList<>(); // {colIndex, monthIdx, priIdx}
            if (row1 != null) {
                int seq = 0;
                for (int c = 0; c < row1.getLastCellNum(); c++) {
                    String label = ExcelCells.toString(row1.getCell(c));
                    if (label == null) continue;
                    String l = label.toLowerCase();
                    int pri = l.equals("primary") ? 0 : l.equals("secondary") ? 1 : l.equals("tertiary") ? 2 : -1;
                    if (pri >= 0) {
                        fuelCols.add(new int[]{c, seq / 3, seq % 3});
                        seq++;
                    }
                }
            }
            if (fuelCols.size() != 36) {
                vm.setCode(400);
                vm.setMessage("Invalid Excel file: expected 36 fuel columns (12 months x Primary/Secondary/Tertiary), found " + fuelCols.size());
                return vm;
            }

            // ---- Lookup indexes ----
            Map<UUID, Map<String, Set<UUID>>> plantFuelIndex = loadPlantFuelNameIndex(plantIdsCsv, financialYear);
            Map<String, Set<UUID>> globalFuelIndex = loadGlobalFuelNameIndex();
            Map<UUID, AssetFuelPriorityDto> existingById = getAssetFuelPriority(plantIdsCsv, financialYear)
                    .stream()
                    .filter(d -> d.getId() != null)
                    .collect(Collectors.toMap(AssetFuelPriorityDto::getId, d -> d, (a, b) -> a));

            List<AssetFuelPriorityDto> validRecords = new ArrayList<>();
            List<AssetPriorityImportRow> failedRows = new ArrayList<>();
            List<String> failedMessages = new ArrayList<>();
            int skippedCount = 0;

            for (Row row : ExcelRows.getDataRows(sheet, EXCEL_DATA_START_ROW)) {
                UUID id = idCol >= 0 ? ExcelCells.toUUID(row.getCell(idCol)) : null;
                UUID assetId = assetIdCol >= 0 ? ExcelCells.toUUID(row.getCell(assetIdCol)) : null;
                UUID plantId = plantCol >= 0 ? ExcelCells.toUUID(row.getCell(plantCol)) : null;
                String remarks = remarksCol >= 0 ? ExcelCells.toString(row.getCell(remarksCol)) : null;
                String assetName = ExcelCells.toString(row.getCell(assetCol));
                String plantName = plantNameCol >= 0 ? ExcelCells.toString(row.getCell(plantNameCol)) : null;

                AssetPriorityImportRow parsed = new AssetPriorityImportRow(
                        id, assetId, plantId, plantName, assetName, remarks,
                        new String[36], new ArrayList<>());

                AssetFuelPriorityDto existing = id != null ? existingById.get(id) : null;
                if (id == null) {
                    parsed.errors().add("id is missing — import only supports updating existing records");
                } else if (existing == null) {
                    parsed.errors().add("No asset fuel priority record found for id: " + id);
                }
                if (assetId == null && existing != null) assetId = existing.getAssetId();
                if (plantId == null && existing != null) plantId = existing.getCppPlantFkId();
                if (assetId == null) parsed.errors().add("assetId is missing");
                if (plantId == null) parsed.errors().add("cppPlantFkId is missing");

                // Resolve the 36 fuel-name cells to UUIDs
                UUID[] resolved = new UUID[36];
                String[] names = parsed.fuelNames();
                for (int i = 0; i < fuelCols.size() && i < 36; i++) {
                    int[] fc = fuelCols.get(i);
                    String name = ExcelCells.toString(row.getCell(fc[0]));
                    names[i] = name;
                    if (name == null || name.isBlank()) continue;
                    String monthLabel = FiscalYearMonths.MONTH_KEYS[fc[1]];
                    String priLabel = EXCEL_PRIORITY_SUFFIX[fc[2]];
                    UUID fuelId = resolveFuelName(plantId, name, plantFuelIndex, globalFuelIndex);
                    if (fuelId == null) {
                        parsed.errors().add(cap(monthLabel) + " " + priLabel + ": '" + name + "' is not a valid fuel for this plant");
                    } else {
                        resolved[i] = fuelId;
                    }
                }

                // Duplicate check within each month (Primary/Secondary/Tertiary must differ)
                for (int m = 0; m < 12; m++) {
                    Set<UUID> seen = new HashSet<>();
                    boolean dup = false;
                    for (int p = 0; p < 3; p++) {
                        UUID fu = resolved[m * 3 + p];
                        if (fu != null && !seen.add(fu)) dup = true;
                    }
                    if (dup) {
                        parsed.errors().add(cap(FiscalYearMonths.MONTH_KEYS[m]) + ": duplicate fuel selected in Primary/Secondary/Tertiary");
                    }
                }

                // ---- Change detection vs existing record ----
                boolean fuelsChanged = false;
                if (existing != null) {
                    for (int i = 0; i < 36; i++) {
                        int m = i / 3, p = i % 3;
                        UUID oldId = getPriorityFuelId(existing, m, p);
                        if (!java.util.Objects.equals(oldId, resolved[i])) { fuelsChanged = true; break; }
                    }
                } else {
                    fuelsChanged = true;
                }
                String newRemarks = remarks != null ? remarks.trim() : "";
                String oldRemarks = existing != null && existing.getRemarks() != null ? existing.getRemarks().trim() : "";
                boolean remarksChanged = !newRemarks.equals(oldRemarks);

                if (!fuelsChanged && !remarksChanged && existing != null) {
                    skippedCount++;
                    continue;
                }

                if (!parsed.errors().isEmpty()) {
                    failedRows.add(parsed);
                    failedMessages.add(String.join("; ", parsed.errors()));
                    continue;
                }

                // Remarks rule (mirrors UI): required & must differ when fuel data changed
                if (fuelsChanged && (newRemarks.isEmpty() || newRemarks.equals(oldRemarks))) {
                    parsed.errors().add("Remarks is required and must be different from previous when fuel priorities are changed");
                    failedRows.add(parsed);
                    failedMessages.add(String.join("; ", parsed.errors()));
                    continue;
                }

                AssetFuelPriorityDto dto = new AssetFuelPriorityDto();
                dto.setId(id);
                dto.setAssetId(assetId);
                dto.setCppPlantFkId(plantId);
                dto.setFinancialYear(financialYear);
                dto.setRemarks(remarks);
                for (int i = 0; i < 36; i++) {
                    setPriorityFuelId(dto, i / 3, i % 3, resolved[i]);
                }
                validRecords.add(dto);
            }

            logger.info("[AssetFuelPriority IMPORT] parsed rows - valid: {}, failed: {}, unchanged skipped: {}",
                    validRecords.size(), failedRows.size(), skippedCount);

            AOPMessageVM saveResult = null;
            if (!validRecords.isEmpty()) {
                saveResult = updateAssetFuelPriority(validRecords);
                if (saveResult.getCode() != 200) {
                    vm.setCode(saveResult.getCode());
                    vm.setMessage("Save failed: " + saveResult.getMessage());
                    return vm;
                }
            }

            if (!failedRows.isEmpty()) {
                byte[] errorExcel = generateAssetPriorityErrorExcel(failedRows, failedMessages, financialYear);
                vm.setCode(400);
                vm.setData(Base64.getEncoder().encodeToString(errorExcel));
                vm.setMessage("Partial data saved. " + validRecords.size() + " saved, " + failedRows.size()
                        + " failed, " + skippedCount + " unchanged. Please check the downloaded error file.");
            } else if (validRecords.isEmpty()) {
                vm.setCode(200);
                vm.setMessage("No changes detected. All " + skippedCount + " records unchanged.");
            } else {
                vm.setCode(200);
                vm.setMessage((saveResult != null ? saveResult.getMessage() : "Success") + " " + skippedCount + " unchanged.");
            }

            return vm;
        } catch (Exception e) {
            logger.error("[AssetFuelPriority IMPORT] error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            return vm;
        }
    }

    private UUID resolveFuelName(UUID plantId, String name,
                                 Map<UUID, Map<String, Set<UUID>>> plantFuelIndex,
                                 Map<String, Set<UUID>> globalFuelIndex) {
        String key = name.trim().toLowerCase();
        Map<String, Set<UUID>> plantMap = plantId != null ? plantFuelIndex.get(plantId) : null;
        if (plantMap != null) {
            Set<UUID> ids = plantMap.get(key);
            if (ids != null && ids.size() == 1) return ids.iterator().next();
            if (ids != null && ids.size() > 1) return null; // ambiguous within plant
        }
        Set<UUID> global = globalFuelIndex.get(key);
        if (global != null && global.size() == 1) return global.iterator().next();
        return null;
    }

    private byte[] generateAssetPriorityErrorExcel(List<AssetPriorityImportRow> failedRows,
                                                   List<String> errorMessages,
                                                   String financialYear) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Asset Fuel Priority Errors");

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(workbook);
        CellStyle lockedStyle = ExcelStyles.createLockedStyle(workbook);
        CellStyle remarksStyle = ExcelStyles.createEditableRemarksStyle(workbook);
        CellStyle errorStyle = ExcelStyles.createErrorStyle(workbook);

        String[] monthHeaders = FiscalYearMonths.getMonthHeaders(financialYear);

        Row groupHeader = sheet.createRow(0);
        ExcelCells.setString(groupHeader.createCell(EXCEL_PLANT_COL), "CPP Plant", headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 1, EXCEL_PLANT_COL, EXCEL_PLANT_COL));
        ExcelCells.setString(groupHeader.createCell(EXCEL_ASSET_COL), "Asset Name", headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 1, EXCEL_ASSET_COL, EXCEL_ASSET_COL));
        for (int m = 0; m < 12; m++) {
            int firstCol = EXCEL_FUEL_COL_START + m * 3;
            ExcelCells.setString(groupHeader.createCell(firstCol), monthHeaders[m], headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, firstCol, firstCol + 2));
        }
        ExcelCells.setString(groupHeader.createCell(EXCEL_REMARKS_COL), "Remarks", headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 1, EXCEL_REMARKS_COL, EXCEL_REMARKS_COL));

        Row subHeader = sheet.createRow(1);
        for (int m = 0; m < 12; m++) {
            for (int p = 0; p < 3; p++) {
                ExcelCells.setString(subHeader.createCell(EXCEL_FUEL_COL_START + m * 3 + p),
                        EXCEL_PRIORITY_SUFFIX[p], headerStyle);
            }
        }
        ExcelCells.setString(subHeader.createCell(EXCEL_ID_COL), "id", headerStyle);
        ExcelCells.setString(subHeader.createCell(EXCEL_ASSET_ID_COL), "assetId", headerStyle);
        ExcelCells.setString(subHeader.createCell(EXCEL_PLANT_ID_COL), "cppPlantFkId", headerStyle);
        int statusCol = EXCEL_TOTAL_COLS;
        ExcelCells.setString(subHeader.createCell(statusCol), "Status", headerStyle);
        ExcelCells.setString(subHeader.createCell(statusCol + 1), "Error Message", headerStyle);

        int rowIdx = EXCEL_DATA_START_ROW;
        for (int i = 0; i < failedRows.size(); i++) {
            AssetPriorityImportRow pr = failedRows.get(i);
            Row row = sheet.createRow(rowIdx++);
            ExcelCells.setString(row.createCell(EXCEL_PLANT_COL), pr.plantName(), errorStyle);
            ExcelCells.setString(row.createCell(EXCEL_ASSET_COL), pr.assetName(), errorStyle);
            for (int c = 0; c < 36; c++) {
                ExcelCells.setString(row.createCell(EXCEL_FUEL_COL_START + c), pr.fuelNames()[c], errorStyle);
            }
            ExcelCells.setString(row.createCell(EXCEL_REMARKS_COL), pr.remarks(), remarksStyle);
            ExcelCells.setString(row.createCell(EXCEL_ID_COL), pr.id() != null ? pr.id().toString() : null, lockedStyle);
            ExcelCells.setString(row.createCell(EXCEL_ASSET_ID_COL), pr.assetId() != null ? pr.assetId().toString() : null, lockedStyle);
            ExcelCells.setString(row.createCell(EXCEL_PLANT_ID_COL), pr.cppPlantFkId() != null ? pr.cppPlantFkId().toString() : null, lockedStyle);
            ExcelCells.setString(row.createCell(statusCol), "Failed", errorStyle);
            ExcelCells.setString(row.createCell(statusCol + 1), errorMessages.get(i), errorStyle);
        }

        ExcelColumns.hideColumns(sheet, EXCEL_ID_COL, EXCEL_ASSET_ID_COL, EXCEL_PLANT_ID_COL);
        ExcelColumns.autoSize(sheet, statusCol + 2, EXCEL_REMARKS_COL);
        sheet.setColumnWidth(statusCol + 1, ExcelColumns.DEFAULT_REMARKS_WIDTH);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

}
