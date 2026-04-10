package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.dto.ShutdownRateDTO;
import com.wks.caseengine.dto.ShutdownRateDropdownDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.ShutdownRateService;

@RestController
@RequestMapping("task")
public class ShutdownRateController {
	
	@Autowired
	private ShutdownRateService shutdownRateService;
	
	@GetMapping(value="/shutdown-rate-manual-entry")
	public AOPMessageVM getShutdownRate(
			@RequestParam(value = "plantId", required = true) String plantId,
			@RequestParam(value = "year", required = false) String year) {
		return shutdownRateService.getShutdownRate(plantId, year);
	}
	
	@GetMapping(value="/shutdown-rate-dropdown")
	public List<ShutdownRateDropdownDTO> getShutdownRateDropdown(@RequestParam  String plantId) {
		return shutdownRateService.getShutdownRateDropdown(plantId);
	}
}
