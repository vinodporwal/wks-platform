package com.wks.caseengine.rest.server;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.dto.ConfigurationDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.ConfigurationService;

@RestController
@RequestMapping("task")
public class ConfigurationOtherCostController {

	@Autowired
	private ConfigurationService configurationService;

	@GetMapping(value="/configuration-other-cost")
	public AOPMessageVM getConfigurationOtherCost(@RequestParam String year, @RequestParam UUID plantFKId) {
		return configurationService.getConfigurationOtherCost(year, plantFKId);
	}

	@PostMapping(value="/configuration-other-cost")
	public List<ConfigurationDTO> saveConfigurationOtherCost(@RequestParam String year, @RequestParam String plantFKId, @RequestBody List<ConfigurationDTO> configurationDTOList) {
		configurationService.saveConfigurationOtherCost(year, plantFKId, configurationDTOList);
		return configurationDTOList;
	}
}
