package com.wks.caseengine.service;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wks.caseengine.dto.AssetImportMappingDTO;
import com.wks.caseengine.entity.AssetImportMapping;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.AssetImportMappingRepository;

@Service
public class AssetImportMappingServiceImpl implements AssetImportMappingService {

    @Autowired
    private AssetImportMappingRepository repository;

 
    @Override
    public AOPMessageVM getAll() {

        AOPMessageVM vm = new AOPMessageVM();

        try {

            List<AssetImportMappingDTO> dtoList = repository.findAll()
                    .stream()
                    .map(this::toDTO)
                    .toList();

            vm.setCode(200);
            vm.setMessage("Data fetched successfully");
            vm.setData(dtoList);

            return vm;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch AssetImportMapping data", e);
        }
    }

    
    @Override
    public AOPMessageVM saveOrUpdate(List<AssetImportMappingDTO> dtoList) {

        if (dtoList == null || dtoList.isEmpty()) {
            throw new RestInvalidArgumentException("Payload cannot be empty", null);
        }

        AOPMessageVM vm = new AOPMessageVM();

        try {
            List<AssetImportMapping> savedList = new ArrayList<>();

            for (AssetImportMappingDTO dto : dtoList) {

                
                if (dto.getAssetId() == null || dto.getAssetId().isBlank()) {
                    throw new RestInvalidArgumentException("AssetId is required", null);
                }

                if (dto.getFinancialMonthId() == null || dto.getFinancialMonthId().isBlank()) {
                    throw new RestInvalidArgumentException("FinancialMonthId is required", null);
                }

                if (dto.getValue() == null) {
                    throw new RestInvalidArgumentException("Value is required", null);
                }

                if (dto.getUom() == null || dto.getUom().isBlank()) {
                    throw new RestInvalidArgumentException("UOM is required", null);
                }

             
               
                if (dto.getValue() < 0) {
                    throw new RestInvalidArgumentException("Value cannot be negative", null);
                }

                
                if (dto.getValue() > 99999999999999.99) {
                    throw new RestInvalidArgumentException("Value exceeds allowed limit", null);
                }

                
                AssetImportMapping entity = new AssetImportMapping();

                
                if (dto.getId() != null && !dto.getId().isBlank()) {
                    try {
                        UUID dtoId = UUID.fromString(dto.getId());
                        entity = repository.findById(dtoId)
                                .orElseThrow(() -> new RestInvalidArgumentException("Record not found for given Id", null));
                    } catch (IllegalArgumentException ex) {
                        throw new RestInvalidArgumentException("Invalid UUID format in Id", ex);
                    }
                }
                else {
                    
                    repository.findByAssetIdAndFinancialMonthId(
                            UUID.fromString(dto.getAssetId()),
                            UUID.fromString(dto.getFinancialMonthId())
                    ).ifPresent(existing -> {
                        throw new RestInvalidArgumentException(
                            "Record already exists for same AssetId + FinancialMonthId. Duplicate not allowed.",
                            null
                        );
                    });
                }

              
                try {
                    entity.setAssetId(UUID.fromString(dto.getAssetId()));
                } catch (IllegalArgumentException e) {
                    throw new RestInvalidArgumentException("Invalid UUID format in AssetId", e);
                }

                try {
                    entity.setFinancialMonthId(UUID.fromString(dto.getFinancialMonthId()));
                } catch (IllegalArgumentException e) {
                    throw new RestInvalidArgumentException("Invalid UUID format in FinancialMonthId", e);
                }

                entity.setValue(dto.getValue());
                entity.setUom(dto.getUom());

                
                repository.save(entity);
                savedList.add(entity);
            }

            vm.setCode(200);
            vm.setMessage("Data saved successfully");
            vm.setData(savedList.stream().map(this::toDTO).toList());

            return vm;

        } catch (RestInvalidArgumentException e) {
            throw e;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to save AssetImportMapping data", ex);
        }
    }

   
    private AssetImportMappingDTO toDTO(AssetImportMapping entity) {
        return AssetImportMappingDTO.builder()
                .id(entity.getId() != null ? entity.getId().toString() : null)
                .assetId(entity.getAssetId() != null ? entity.getAssetId().toString() : null)
                .financialMonthId(entity.getFinancialMonthId() != null ? entity.getFinancialMonthId().toString() : null)
                .value(entity.getValue())
                .uom(entity.getUom())
                .build();
    }
}
