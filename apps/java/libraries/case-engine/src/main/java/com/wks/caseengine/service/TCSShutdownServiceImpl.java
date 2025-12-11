package com.wks.caseengine.service;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wks.caseengine.dto.TCSShutdownDTO;
import com.wks.caseengine.entity.TCSShutdown;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.TCSShutdownRepository;

@Service
public class TCSShutdownServiceImpl implements TCSShutdownService {

    @Autowired
    private TCSShutdownRepository tcsShutdownRepository;


    
    @Override
    public AOPMessageVM getAll() {
        AOPMessageVM vm = new AOPMessageVM();

        try {
            List<TCSShutdownDTO> dtoList = tcsShutdownRepository.findAll()
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
    public AOPMessageVM saveOrUpdate(List<TCSShutdownDTO> dtoList) {

        if (dtoList == null || dtoList.isEmpty()) {
            throw new RestInvalidArgumentException("Payload cannot be empty", null);
        }

        AOPMessageVM vm = new AOPMessageVM();

        try {
            List<TCSShutdown> savedList = new ArrayList<>();

            for (TCSShutdownDTO dto : dtoList) {

                
                if (dto.getParticulates() == null || dto.getParticulates().isBlank()) {
                    throw new RestInvalidArgumentException("Particulates is required", null);
                }

                if (dto.getTentativeMonth() == null || dto.getTentativeMonth().isBlank()) {
                    throw new RestInvalidArgumentException("Tentative Month is required", null);
                }

                if (dto.getSdTotalDurationInDays() == null || dto.getSdTotalDurationInDays() <= 0) {
                    throw new RestInvalidArgumentException("SD Total Duration (Days) must be greater than 0", null);
                }

                if (dto.getPurposeOfShutdown() == null || dto.getPurposeOfShutdown().isBlank()) {
                    throw new RestInvalidArgumentException("Purpose of Shutdown is required", null);
                }


                
                tcsShutdownRepository.findByParticulatesAndTentativeMonth(dto.getParticulates(), dto.getTentativeMonth())
                        .ifPresent(existing -> {
                            throw new RestInvalidArgumentException(
                                    "Duplicate record found → Same Particulates + Tentative Month not allowed.",
                                    null
                            );
                        });


                
                TCSShutdown entity = new TCSShutdown();

                
                if (dto.getId() != null && !dto.getId().isBlank()) {
                    try {
                        entity.setId(UUID.fromString(dto.getId()));
                    } catch (IllegalArgumentException ex) {
                        throw new RestInvalidArgumentException("Invalid UUID format", ex);
                    }
                }

                entity.setParticulates(dto.getParticulates());
                entity.setSdTotalDurationInDays(dto.getSdTotalDurationInDays());
                entity.setTentativeMonth(dto.getTentativeMonth());
                entity.setPurposeOfShutdown(dto.getPurposeOfShutdown());

                tcsShutdownRepository.save(entity);
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


    
    private TCSShutdownDTO toDTO(TCSShutdown entity) {
        return TCSShutdownDTO.builder()
                .id(entity.getId() != null ? entity.getId().toString() : null)
                .particulates(entity.getParticulates())
                .sdTotalDurationInDays(entity.getSdTotalDurationInDays())
                .tentativeMonth(entity.getTentativeMonth())
                .purposeOfShutdown(entity.getPurposeOfShutdown())
                .build();
    }
}
