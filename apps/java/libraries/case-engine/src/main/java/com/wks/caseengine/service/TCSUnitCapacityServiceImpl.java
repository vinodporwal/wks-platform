package com.wks.caseengine.service;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wks.caseengine.dto.TCSUnitCapacityDTO;
import com.wks.caseengine.entity.TCSUnitCapacity;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.TCSUnitCapacityRepository;

@Service
public class TCSUnitCapacityServiceImpl implements TCSUnitCapacityService {

    @Autowired
    private TCSUnitCapacityRepository tcsUnitCapacityRepository;


   
    @Override
    public AOPMessageVM getAll() {
        AOPMessageVM vm = new AOPMessageVM();

        try {
            List<TCSUnitCapacityDTO> dtoList = tcsUnitCapacityRepository.findAll()
                    .stream()
                    .map(this::toDTO)
                    .toList();

            vm.setCode(200);
            vm.setMessage("Data fetched successfully");
            vm.setData(dtoList);

            return vm;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch data", e);
        }
    }


    
    @Override
    public AOPMessageVM saveOrUpdate(List<TCSUnitCapacityDTO> dtoList) {

        if (dtoList == null || dtoList.isEmpty()) {
            throw new RestInvalidArgumentException("Payload cannot be empty", null);
        }

        AOPMessageVM vm = new AOPMessageVM();

        try {
            List<TCSUnitCapacity> savedList = new ArrayList<>();

            for (TCSUnitCapacityDTO dto : dtoList) {

                
                if (dto.getUom() == null || dto.getUom().isBlank()) {
                    throw new RestInvalidArgumentException("UOM is required", null);
                }

                if (dto.getParticulates() == null || dto.getParticulates().isBlank()) {
                    throw new RestInvalidArgumentException("Particulates is required", null);
                }

                
                tcsUnitCapacityRepository.findByParticulatesAndUom(dto.getParticulates(), dto.getUom())
                        .ifPresent(existing -> {
                            throw new RestInvalidArgumentException(
                                "Record already exists with same Particulates + UOM. Duplicate not allowed.",
                                null
                            );
                        });

                
                TCSUnitCapacity entity = new TCSUnitCapacity();

                
                if (dto.getId() != null && !dto.getId().isBlank()) {
                    try {
                        entity.setId(UUID.fromString(dto.getId()));
                    } catch (IllegalArgumentException ex) {
                        throw new RestInvalidArgumentException("Invalid UUID format", ex);
                    }
                }

                entity.setParticulates(dto.getParticulates());
                entity.setUom(dto.getUom());
                entity.setKbpsd(dto.getKbpsd());
                entity.setRemark(dto.getRemark());

                tcsUnitCapacityRepository.save(entity);
                savedList.add(entity);
            }

            vm.setCode(200);
            vm.setMessage("Data saved successfully");
            vm.setData(savedList.stream().map(this::toDTO).toList());
            return vm;

        } catch (RestInvalidArgumentException e) {
            throw e;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to save data", ex);
        }
    }


   
    private TCSUnitCapacityDTO toDTO(TCSUnitCapacity entity) {
        return TCSUnitCapacityDTO.builder()
            .id(entity.getId() != null ? entity.getId().toString() : null)
            .particulates(entity.getParticulates())
            .uom(entity.getUom())
            .kbpsd(entity.getKbpsd())
            .remark(entity.getRemark())
            .build();
    }
}
