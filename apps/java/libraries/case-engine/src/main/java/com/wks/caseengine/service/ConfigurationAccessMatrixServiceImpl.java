package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wks.caseengine.dto.ConfigurationAccessMatrixDTO;
import com.wks.caseengine.entity.ConfigurationAccessMatrix;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.ConfigurationAccessMatrixRepository;

@Service
public class ConfigurationAccessMatrixServiceImpl implements ConfigurationAccessMatrixService {

	@Autowired
	private ConfigurationAccessMatrixRepository configurationAccessMatrixRepository;

	@Override
	public AOPMessageVM getConfigurationAccessMatrix(String plantId, String siteId, String verticalId, String type) {
		try {
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			UUID plant = UUID.fromString(plantId);
			UUID site = UUID.fromString(siteId);
			UUID vertical = UUID.fromString(verticalId);

			String configurationTabsStr = configurationAccessMatrixRepository
			        .findConfigurationTabsByVerticalSitePlant(vertical, site, plant, type)
			        .orElse("[]");
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data fetched successfully");
			aopMessageVM.setData(configurationTabsStr);
			return aopMessageVM;

		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Override
	public AOPMessageVM getAllConfigurationAccessMatrix() {
		List<ConfigurationAccessMatrix> list = configurationAccessMatrixRepository.findAll();
		List<ConfigurationAccessMatrixDTO> dtoList = new ArrayList<>();
		for (ConfigurationAccessMatrix obj : list) {
			dtoList.add(mapToDTO(obj));
		}
		AOPMessageVM vm = new AOPMessageVM();
		vm.setCode(200);
		vm.setMessage("Data fetched successfully");
		vm.setData(dtoList);
		return vm;
	}

	@Override
	public AOPMessageVM getConfigurationAccessMatrixById(String id) {
		try {
			UUID uuid = UUID.fromString(id);
			Optional<ConfigurationAccessMatrix> opt = configurationAccessMatrixRepository.findById(uuid);
			if (opt.isEmpty()) {
				AOPMessageVM vm = new AOPMessageVM();
				vm.setCode(404);
				vm.setMessage("ConfigurationAccessMatrix not found with id: " + id);
				vm.setData(null);
				return vm;
			}
			AOPMessageVM vm = new AOPMessageVM();
			vm.setCode(200);
			vm.setMessage("Data fetched successfully");
			vm.setData(mapToDTO(opt.get()));
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
	public AOPMessageVM createConfigurationAccessMatrix(ConfigurationAccessMatrixDTO dto) {
		try {
			ConfigurationAccessMatrix entity = new ConfigurationAccessMatrix();
			entity.setVerticalId(dto.getVerticalId() != null ? UUID.fromString(dto.getVerticalId()) : null);
			entity.setSiteId(dto.getSiteId() != null ? UUID.fromString(dto.getSiteId()) : null);
			entity.setPlantId(dto.getPlantId() != null ? UUID.fromString(dto.getPlantId()) : null);
			entity.setConfigurationTabs(dto.getConfigurationTabs());
			entity.setType(dto.getType());

			ConfigurationAccessMatrix saved = configurationAccessMatrixRepository.save(entity);
			AOPMessageVM vm = new AOPMessageVM();
			vm.setCode(201);
			vm.setMessage("ConfigurationAccessMatrix created successfully");
			vm.setData(mapToDTO(saved));
			return vm;
		} catch (Exception e) {
			AOPMessageVM vm = new AOPMessageVM();
			vm.setCode(500);
			vm.setMessage("Failed to create: " + e.getMessage());
			vm.setData(null);
			return vm;
		}
	}

	@Override
	public AOPMessageVM updateConfigurationAccessMatrix(String id, ConfigurationAccessMatrixDTO dto) {
		try {
			UUID uuid = UUID.fromString(id);
			Optional<ConfigurationAccessMatrix> opt = configurationAccessMatrixRepository.findById(uuid);
			if (opt.isEmpty()) {
				AOPMessageVM vm = new AOPMessageVM();
				vm.setCode(404);
				vm.setMessage("ConfigurationAccessMatrix not found with id: " + id);
				vm.setData(null);
				return vm;
			}

			ConfigurationAccessMatrix entity = opt.get();
			if (dto.getVerticalId() != null) entity.setVerticalId(UUID.fromString(dto.getVerticalId()));
			if (dto.getSiteId() != null) entity.setSiteId(UUID.fromString(dto.getSiteId()));
			if (dto.getPlantId() != null) entity.setPlantId(UUID.fromString(dto.getPlantId()));
			if (dto.getConfigurationTabs() != null) entity.setConfigurationTabs(dto.getConfigurationTabs());
			if (dto.getType() != null) entity.setType(dto.getType());

			ConfigurationAccessMatrix updated = configurationAccessMatrixRepository.save(entity);
			AOPMessageVM vm = new AOPMessageVM();
			vm.setCode(200);
			vm.setMessage("ConfigurationAccessMatrix updated successfully");
			vm.setData(mapToDTO(updated));
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
			vm.setMessage("Failed to update: " + e.getMessage());
			vm.setData(null);
			return vm;
		}
	}

	@Override
	public AOPMessageVM deleteConfigurationAccessMatrix(String id) {
		try {
			UUID uuid = UUID.fromString(id);
			if (!configurationAccessMatrixRepository.existsById(uuid)) {
				AOPMessageVM vm = new AOPMessageVM();
				vm.setCode(404);
				vm.setMessage("ConfigurationAccessMatrix not found with id: " + id);
				vm.setData(null);
				return vm;
			}
			configurationAccessMatrixRepository.deleteById(uuid);
			AOPMessageVM vm = new AOPMessageVM();
			vm.setCode(200);
			vm.setMessage("ConfigurationAccessMatrix deleted successfully");
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
			vm.setMessage("Failed to delete: " + e.getMessage());
			vm.setData(null);
			return vm;
		}
	}

	private ConfigurationAccessMatrixDTO mapToDTO(ConfigurationAccessMatrix obj) {
		ConfigurationAccessMatrixDTO dto = new ConfigurationAccessMatrixDTO();
		dto.setId(obj.getId() != null ? obj.getId().toString() : null);
		dto.setVerticalId(obj.getVerticalId() != null ? obj.getVerticalId().toString() : null);
		dto.setSiteId(obj.getSiteId() != null ? obj.getSiteId().toString() : null);
		dto.setPlantId(obj.getPlantId() != null ? obj.getPlantId().toString() : null);
		dto.setConfigurationTabs(obj.getConfigurationTabs());
		dto.setType(obj.getType());
		return dto;
	}
}
