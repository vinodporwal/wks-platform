package com.wks.caseengine.rest.server;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

	@GetMapping(value = "/configuration-other-cost-export")
	public ResponseEntity<byte[]> exportConfigurationOtherCost(
			@RequestParam String year,
			@RequestParam UUID plantFKId) {
		try {
			byte[] excelBytes = configurationService.createConfigurationOtherCostExcel(year, plantFKId, false, null);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
			headers.setContentDisposition(ContentDisposition.builder("attachment")
					.filename("configuration_other_cost.xlsx")
					.build());
			headers.setContentLength(excelBytes.length);
			return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/configuration-other-cost-import", consumes = "multipart/form-data")
	public AOPMessageVM importConfigurationOtherCostExcel(
			@RequestParam String year,
			@RequestParam UUID plantFKId,
			@RequestParam("file") MultipartFile file) {
		return configurationService.importConfigurationOtherCostExcel(year, plantFKId, file);
	}
}
