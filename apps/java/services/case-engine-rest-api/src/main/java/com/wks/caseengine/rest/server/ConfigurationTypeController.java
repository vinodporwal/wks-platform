package com.wks.caseengine.rest.server;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.dto.ConfigurationTypeDTO;
import com.wks.caseengine.service.ConfigurationTypeService;


@RestController
@RequestMapping("task")
public class ConfigurationTypeController {

	@Autowired
	private ConfigurationTypeService configurationTypeService;

	@GetMapping(value="/configuration-type-data")
	public AOPMessageVM getConfigurationTypeData() {
		return configurationTypeService.getConfigurationTypeData();
	}

	@GetMapping(value="/configuration-type-data/{id}")
	public ResponseEntity<AOPMessageVM> getConfigurationTypeById(@PathVariable String id) {
		AOPMessageVM response = configurationTypeService.getConfigurationTypeById(id);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	@PostMapping(value="/configuration-type-data")
	public ResponseEntity<AOPMessageVM> createConfigurationType(@RequestBody ConfigurationTypeDTO dto) {
		AOPMessageVM response = configurationTypeService.createConfigurationType(dto);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	@PutMapping(value="/configuration-type-data/{id}")
	public ResponseEntity<AOPMessageVM> updateConfigurationType(@PathVariable String id, @RequestBody ConfigurationTypeDTO dto) {
		AOPMessageVM response = configurationTypeService.updateConfigurationType(id, dto);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	@DeleteMapping(value="/configuration-type-data/{id}")
	public ResponseEntity<AOPMessageVM> deleteConfigurationType(@PathVariable String id) {
		AOPMessageVM response = configurationTypeService.deleteConfigurationType(id);
		return ResponseEntity.status(response.getCode()).body(response);
	}
}