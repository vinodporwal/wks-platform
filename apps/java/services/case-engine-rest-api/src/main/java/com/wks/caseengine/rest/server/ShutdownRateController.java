package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
	public List<ShutdownRateDropdownDTO> getShutdownRateDropdown(@RequestParam String plantId) {
		return shutdownRateService.getShutdownRateDropdown(plantId);
	}

	@GetMapping(value = "/export-shutdown-rate-manual-entry")
	public ResponseEntity<byte[]> exportShutdownRate(
			@RequestParam("plantId") String plantId,
			@RequestParam("year") String year) {
		try {
			byte[] excelBytes = shutdownRateService.exportShutdownRate(plantId, year);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(
					MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
			headers.setContentDisposition(
					ContentDisposition.builder("attachment").filename("Shutdown_Rate.xlsx").build());
			headers.setContentLength(excelBytes.length);

			return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/import-shutdown-rate-manual-entry", consumes = "multipart/form-data")
	public AOPMessageVM importShutdownRate(
			@RequestParam("plantId") String plantId,
			@RequestParam("year") String year,
			@RequestParam(required = false) String version,
			@RequestParam("file") MultipartFile file,
			@RequestParam(required = false) Boolean calculation) {
		return shutdownRateService.importShutdownRate(plantId, year, version, file, calculation);
	}
}
