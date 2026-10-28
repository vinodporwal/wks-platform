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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.EnergyPerformanceDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.EnergyPerformanceTranscationService;

@RestController
@RequestMapping("task")
public class EnergyPerformanceTranscationController {

	@Autowired
	private EnergyPerformanceTranscationService energyPerformanceTranscationService;

	@GetMapping(value = "/energy-performance")
	public AOPMessageVM getEnergyPerformanceTransaction(@RequestParam String siteId, @RequestParam String year) {
		return energyPerformanceTranscationService.getEnergyPerformanceTransaction(siteId, year);
	}

	@PostMapping(value = "/energy-performance")
	public AOPMessageVM saveEnergyPerformanceTransaction(@RequestParam String year, @RequestParam String siteId,
			@RequestBody List<EnergyPerformanceDTO> energyPerformanceDTOs) {
		return energyPerformanceTranscationService.saveEnergyPerformanceTransaction(year, siteId,
				energyPerformanceDTOs);
	}

	@GetMapping(value = "/energy-performance-export")
	public ResponseEntity<byte[]> exportEnergyPerformance(
			@RequestParam("siteId") String siteId,
			@RequestParam("year") String year,
			@RequestParam(value = "excelName", required = false, defaultValue = "energy_performance") String excelName) {
		try {
			byte[] excelBytes = energyPerformanceTranscationService.exportEnergyPerformance(siteId, year, false, null);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
			headers.setContentDisposition(ContentDisposition.builder("attachment")
					.filename(excelName + ".xlsx")
					.build());
			headers.setContentLength(excelBytes.length);

			return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/energy-performance-import", consumes = "multipart/form-data")
	public AOPMessageVM importEnergyPerformance(
			@RequestParam("siteId") String siteId,
			@RequestParam("year") String year,
			@RequestParam("file") MultipartFile file) {
		return energyPerformanceTranscationService.importEnergyPerformance(year, siteId, file);
	}
}
