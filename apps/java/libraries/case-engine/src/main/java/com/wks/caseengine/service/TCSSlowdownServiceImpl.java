package com.wks.caseengine.service;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wks.caseengine.dto.TCSSlowdownDTO;
import com.wks.caseengine.entity.TCSSlowdown;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.TCSSlowdownRepository;

@Service
public class TCSSlowdownServiceImpl implements TCSSlowdownService {

    @Autowired
    private TCSSlowdownRepository tcsSlowdownRepository;


    
    @Override
    public AOPMessageVM getAll() {
        AOPMessageVM vm = new AOPMessageVM();

        try {
            List<TCSSlowdownDTO> dtoList = tcsSlowdownRepository.findAll()
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
    public AOPMessageVM saveOrUpdate(List<TCSSlowdownDTO> dtoList) {

        if (dtoList == null || dtoList.isEmpty()) {
            throw new RestInvalidArgumentException("Payload cannot be empty", null);
        }

        AOPMessageVM vm = new AOPMessageVM();

        try {
            List<TCSSlowdown> savedList = new ArrayList<>();

            for (TCSSlowdownDTO dto : dtoList) {

               
                if (dto.getParticulates() == null || dto.getParticulates().isBlank()) {
                    throw new RestInvalidArgumentException("Particulates is required", null);
                }

                if (dto.getTentativeDurationInDays() == null || dto.getTentativeDurationInDays() <= 0) {
                    throw new RestInvalidArgumentException("Tentative Duration (Days) must be greater than 0", null);
                }

                if (dto.getThroughputDuringSlowdown() == null || dto.getThroughputDuringSlowdown().isBlank()) {
                    throw new RestInvalidArgumentException("Throughput during Slowdown is required", null);
                }

                if (dto.getTentativeMonth() == null || dto.getTentativeMonth().isBlank()) {
                    throw new RestInvalidArgumentException("Tentative Month is required", null);
                }

                if (dto.getPurposeOfSlowdown() == null || dto.getPurposeOfSlowdown().isBlank()) {
                    throw new RestInvalidArgumentException("Purpose of Slowdown is required", null);
                }


                
                tcsSlowdownRepository.findByParticulatesAndTentativeMonth(
                        dto.getParticulates(),
                        dto.getTentativeMonth()
                ).ifPresent(existing -> {
                    throw new RestInvalidArgumentException(
                            "Duplicate record found → Same Particulates + Tentative Month not allowed.",
                            null
                    );
                });


                
                TCSSlowdown entity = new TCSSlowdown();

                
                if (dto.getId() != null && !dto.getId().isBlank()) {
                    try {
                        entity.setId(UUID.fromString(dto.getId()));
                    } catch (IllegalArgumentException ex) {
                        throw new RestInvalidArgumentException("Invalid UUID format", ex);
                    }
                }

                entity.setParticulates(dto.getParticulates());
                entity.setTentativeDurationInDays(dto.getTentativeDurationInDays());
                entity.setThroughputDuringSlowdown(dto.getThroughputDuringSlowdown());
                entity.setTentativeMonth(dto.getTentativeMonth());
                entity.setPurposeOfSlowdown(dto.getPurposeOfSlowdown());

                tcsSlowdownRepository.save(entity);
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



    private TCSSlowdownDTO toDTO(TCSSlowdown entity) {
        return TCSSlowdownDTO.builder()
                .id(entity.getId() != null ? entity.getId().toString() : null)
                .particulates(entity.getParticulates())
                .tentativeDurationInDays(entity.getTentativeDurationInDays())
                .throughputDuringSlowdown(entity.getThroughputDuringSlowdown())
                .tentativeMonth(entity.getTentativeMonth())
                .purposeOfSlowdown(entity.getPurposeOfSlowdown())
                .build();
    }
}
