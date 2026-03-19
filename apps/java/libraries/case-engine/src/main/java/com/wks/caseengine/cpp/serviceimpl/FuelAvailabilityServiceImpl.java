package com.wks.caseengine.cpp.serviceimpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.cpp.service.FuelAvailabilityService;
import com.wks.caseengine.dto.FuelAvailabilityDto;
import com.wks.caseengine.entity.CPPFuelAvailability;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.repository.FuelAvailabilityRepository;

@Service
public class FuelAvailabilityServiceImpl implements FuelAvailabilityService {
    
    private static final Logger logger = LoggerFactory.getLogger(FuelAvailabilityServiceImpl.class);
    
    @Autowired
    private FuelAvailabilityRepository fuelAvailabilityRepository;
    
    @Override
    public List<FuelAvailabilityDto> getFuelAvailability(UUID cppId, String financialYear, String fuelType) {
        logger.debug("getFuelAvailability called with CPPId: {}, FinancialYear: {}, FuelType: {}", 
                cppId, financialYear, fuelType);
        
        if (cppId == null || financialYear == null || financialYear.isEmpty()) {
            logger.error("Invalid parameters - CPPId: {}, FinancialYear: {}", cppId, financialYear);
            throw new RestInvalidArgumentException("CPPId and FinancialYear are required", null);
        }
        
        List<CPPFuelAvailability> entities;
        
        if (fuelType != null && !fuelType.isEmpty()) {
            logger.debug("Fetching fuel availability by CPPId, FinancialYear and FuelType");
            entities = fuelAvailabilityRepository.findByCppIdAndFinancialYearAndFuelType(cppId, financialYear, fuelType);
        } else {
            logger.debug("Fetching all fuel availability by CPPId and FinancialYear");
            entities = fuelAvailabilityRepository.findByCppIdAndFinancialYear(cppId, financialYear);
        }
        
        logger.info("Retrieved {} fuel availability records for CPPId: {}, FinancialYear: {}", 
                entities.size(), cppId, financialYear);
        
        return entities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public FuelAvailabilityDto saveFuelAvailability(FuelAvailabilityDto dto) {
        logger.debug("saveFuelAvailability called for FuelName: {}, CPPId: {}, FinancialYear: {}", 
                dto != null ? dto.getFuelName() : null, 
                dto != null ? dto.getCppId() : null, 
                dto != null ? dto.getFinancialYear() : null);
        
        if (dto == null) {
            logger.error("FuelAvailabilityDto is null");
            throw new RestInvalidArgumentException("FuelAvailabilityDto cannot be null", null);
        }
        
        if (dto.getCppId() == null || dto.getFuelName() == null || dto.getFinancialYear() == null) {
            logger.error("Missing required fields - CPPId: {}, FuelName: {}, FinancialYear: {}", 
                    dto.getCppId(), dto.getFuelName(), dto.getFinancialYear());
            throw new RestInvalidArgumentException("CPPId, FuelName, and FinancialYear are required", null);
        }
        
        CPPFuelAvailability entity;
        boolean isUpdate = false;
        
        if (dto.getId() != null) {
            logger.debug("Updating existing record with Id: {}", dto.getId());
            entity = fuelAvailabilityRepository.findById(dto.getId())
                    .orElseThrow(() -> {
                        logger.error("Fuel availability record not found with Id: {}", dto.getId());
                        return new RestInvalidArgumentException("Fuel availability record not found", null);
                    });
            entity.setUpdatedDate(LocalDateTime.now());
            isUpdate = true;
        } else {
            CPPFuelAvailability existing = fuelAvailabilityRepository.findByCppIdAndFinancialYearAndFuelName(
                    dto.getCppId(), dto.getFinancialYear(), dto.getFuelName());
            
            if (existing != null) {
                logger.debug("Found existing record for FuelName: {}, updating it", dto.getFuelName());
                entity = existing;
                entity.setUpdatedDate(LocalDateTime.now());
                isUpdate = true;
            } else {
                logger.debug("Creating new fuel availability record for FuelName: {}", dto.getFuelName());
                entity = new CPPFuelAvailability();
                entity.setId(UUID.randomUUID());
                entity.setCreatedDate(LocalDateTime.now());
                entity.setUpdatedDate(LocalDateTime.now());
            }
        }
        
        BeanUtils.copyProperties(dto, entity, "id", "createdDate", "updatedDate");
        
        CPPFuelAvailability savedEntity = fuelAvailabilityRepository.save(entity);
        logger.info("{} fuel availability record - Id: {}, FuelName: {}, CPPId: {}, FinancialYear: {}", 
                isUpdate ? "Updated" : "Created", 
                savedEntity.getId(), savedEntity.getFuelName(), 
                savedEntity.getCppId(), savedEntity.getFinancialYear());
        
        return convertToDto(savedEntity);
    }
    
    @Override
    @Transactional
    public List<FuelAvailabilityDto> saveFuelAvailabilityBulk(List<FuelAvailabilityDto> dtos) {
        logger.info("saveFuelAvailabilityBulk called with {} records", dtos != null ? dtos.size() : 0);
        
        if (dtos == null || dtos.isEmpty()) {
            logger.error("FuelAvailabilityDto list is null or empty");
            throw new RestInvalidArgumentException("FuelAvailabilityDto list cannot be null or empty", null);
        }
        
        List<FuelAvailabilityDto> savedDtos = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        
        for (int i = 0; i < dtos.size(); i++) {
            FuelAvailabilityDto dto = dtos.get(i);
            try {
                logger.debug("Processing record {}/{}: FuelName: {}", i + 1, dtos.size(), dto.getFuelName());
                savedDtos.add(saveFuelAvailability(dto));
                successCount++;
            } catch (Exception e) {
                failureCount++;
                logger.error("Failed to save fuel availability record {}/{}: FuelName: {}", 
                        i + 1, dtos.size(), dto.getFuelName(), e);
                throw e;
            }
        }
        
        logger.info("Bulk save completed - Success: {}, Failure: {}, Total: {}", 
                successCount, failureCount, dtos.size());
        
        return savedDtos;
    }
    
    @Override
    @Transactional
    public void deleteFuelAvailability(UUID id) {
        logger.debug("deleteFuelAvailability called with Id: {}", id);
        
        if (id == null) {
            logger.error("Id is null");
            throw new RestInvalidArgumentException("Id cannot be null", null);
        }
        
        if (!fuelAvailabilityRepository.existsById(id)) {
            logger.error("Fuel availability record not found with Id: {}", id);
            throw new RestInvalidArgumentException("Fuel availability record not found", null);
        }
        
        fuelAvailabilityRepository.deleteById(id);
        logger.info("Successfully deleted fuel availability record with Id: {}", id);
    }
    
    private FuelAvailabilityDto convertToDto(CPPFuelAvailability entity) {
        FuelAvailabilityDto dto = new FuelAvailabilityDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
