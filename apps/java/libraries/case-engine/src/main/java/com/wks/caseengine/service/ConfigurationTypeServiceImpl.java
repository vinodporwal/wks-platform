package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import com.wks.caseengine.message.vm.AOPMessageVM;

import com.wks.caseengine.repository.ConfigurationTypeRepository;

import com.wks.caseengine.entity.ConfigurationType;
import com.wks.caseengine.dto.ConfigurationTypeDTO;


@Service
public class ConfigurationTypeServiceImpl  implements ConfigurationTypeService{


	@Autowired
	private ConfigurationTypeRepository configurationTypeRepository;

    @Override
    public AOPMessageVM getConfigurationTypeData() {

         List<ConfigurationType> list =  configurationTypeRepository.findAllByOrderByDisplaySequenceAsc();
         List<ConfigurationTypeDTO> dtList = new ArrayList<>();
         for(ConfigurationType obj:list){
            ConfigurationTypeDTO dto = new ConfigurationTypeDTO();
            dto.setId(obj.getId().toString());
            dto.setName(obj.getName());
            dto.setDisplayName(obj.getDisplayName());
            dto.setDisplaySequence(obj.getDisplaySequence());
            dtList.add(dto);

         }

         AOPMessageVM aopMessageVM = new AOPMessageVM();
         Map<String, Object> finalResult = new HashMap<>();
			finalResult.put("configurationTypeList", list);

			// Set in response
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data fetched successfully");
			aopMessageVM.setData(finalResult);
			return aopMessageVM;

    }

    @Override
    public AOPMessageVM getConfigurationTypeById(String id) {
        try {
            UUID uuid = UUID.fromString(id);
            Optional<ConfigurationType> opt = configurationTypeRepository.findById(uuid);
            if (opt.isEmpty()) {
                AOPMessageVM vm = new AOPMessageVM();
                vm.setCode(404);
                vm.setMessage("ConfigurationType not found with id: " + id);
                vm.setData(null);
                return vm;
            }
            ConfigurationType obj = opt.get();
            ConfigurationTypeDTO dto = mapToDTO(obj);

            AOPMessageVM vm = new AOPMessageVM();
            vm.setCode(200);
            vm.setMessage("Data fetched successfully");
            vm.setData(dto);
            return vm;
        } catch (IllegalArgumentException e) {
            AOPMessageVM vm = new AOPMessageVM();
            vm.setCode(400);
            vm.setMessage("Invalid UUID format: " + id);
            vm.setData(null);
            return vm;
        }
    }

    @Override
    public AOPMessageVM createConfigurationType(ConfigurationTypeDTO dto) {
        try {
            ConfigurationType entity = new ConfigurationType();
            entity.setName(dto.getName());
            entity.setDisplayName(dto.getDisplayName());
            entity.setDisplaySequence(dto.getDisplaySequence());

            ConfigurationType saved = configurationTypeRepository.save(entity);
            ConfigurationTypeDTO savedDto = mapToDTO(saved);

            AOPMessageVM vm = new AOPMessageVM();
            vm.setCode(201);
            vm.setMessage("ConfigurationType created successfully");
            vm.setData(savedDto);
            return vm;
        } catch (Exception e) {
            AOPMessageVM vm = new AOPMessageVM();
            vm.setCode(500);
            vm.setMessage("Failed to create ConfigurationType: " + e.getMessage());
            vm.setData(null);
            return vm;
        }
    }

    @Override
    public AOPMessageVM updateConfigurationType(String id, ConfigurationTypeDTO dto) {
        try {
            UUID uuid = UUID.fromString(id);
            Optional<ConfigurationType> opt = configurationTypeRepository.findById(uuid);
            if (opt.isEmpty()) {
                AOPMessageVM vm = new AOPMessageVM();
                vm.setCode(404);
                vm.setMessage("ConfigurationType not found with id: " + id);
                vm.setData(null);
                return vm;
            }

            ConfigurationType entity = opt.get();
            if (dto.getName() != null) entity.setName(dto.getName());
            if (dto.getDisplayName() != null) entity.setDisplayName(dto.getDisplayName());
            if (dto.getDisplaySequence() != null) entity.setDisplaySequence(dto.getDisplaySequence());

            ConfigurationType updated = configurationTypeRepository.save(entity);
            ConfigurationTypeDTO updatedDto = mapToDTO(updated);

            AOPMessageVM vm = new AOPMessageVM();
            vm.setCode(200);
            vm.setMessage("ConfigurationType updated successfully");
            vm.setData(updatedDto);
            return vm;
        } catch (IllegalArgumentException e) {
            AOPMessageVM vm = new AOPMessageVM();
            vm.setCode(400);
            vm.setMessage("Invalid UUID format: " + id);
            vm.setData(null);
            return vm;
        } catch (Exception e) {
            AOPMessageVM vm = new AOPMessageVM();
            vm.setCode(500);
            vm.setMessage("Failed to update ConfigurationType: " + e.getMessage());
            vm.setData(null);
            return vm;
        }
    }

    @Override
    public AOPMessageVM deleteConfigurationType(String id) {
        try {
            UUID uuid = UUID.fromString(id);
            if (!configurationTypeRepository.existsById(uuid)) {
                AOPMessageVM vm = new AOPMessageVM();
                vm.setCode(404);
                vm.setMessage("ConfigurationType not found with id: " + id);
                vm.setData(null);
                return vm;
            }

            configurationTypeRepository.deleteById(uuid);

            AOPMessageVM vm = new AOPMessageVM();
            vm.setCode(200);
            vm.setMessage("ConfigurationType deleted successfully");
            vm.setData(null);
            return vm;
        } catch (IllegalArgumentException e) {
            AOPMessageVM vm = new AOPMessageVM();
            vm.setCode(400);
            vm.setMessage("Invalid UUID format: " + id);
            vm.setData(null);
            return vm;
        } catch (Exception e) {
            AOPMessageVM vm = new AOPMessageVM();
            vm.setCode(500);
            vm.setMessage("Failed to delete ConfigurationType: " + e.getMessage());
            vm.setData(null);
            return vm;
        }
    }

    private ConfigurationTypeDTO mapToDTO(ConfigurationType obj) {
        ConfigurationTypeDTO dto = new ConfigurationTypeDTO();
        dto.setId(obj.getId().toString());
        dto.setName(obj.getName());
        dto.setDisplayName(obj.getDisplayName());
        dto.setDisplaySequence(obj.getDisplaySequence());
        return dto;
    }
}