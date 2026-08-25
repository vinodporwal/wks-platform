package com.wks.caseengine.rest.server;

import java.util.List;
import java.util.UUID;

import com.wks.caseengine.service.SpyroInputService;
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

import com.wks.caseengine.dto.OptimizingVariablesDropdownDTO;
import com.wks.caseengine.dto.SpyroInputDTO;
import com.wks.caseengine.dto.SpyroInputMinMaxDTO;

import com.wks.caseengine.message.vm.AOPMessageVM;

@RestController
@RequestMapping("task")
public class SpyroInputController {
	
	@Autowired
	private SpyroInputService spyroInputService;
	
	@GetMapping(value="/spyro-input")
	public AOPMessageVM getSpyroInputData(@RequestParam String year,@RequestParam String plantId,@RequestParam String Mode,@RequestParam String type){
		return	spyroInputService.getSpyroInputData(year, plantId,Mode, type);
	}

	@PostMapping(value="/spyro-input")
	public AOPMessageVM updateSpyroInputData(@RequestBody List<SpyroInputDTO> spyroInputDTOList,@RequestParam String year,@RequestParam String plantId){
		return spyroInputService.updateSpyroInputData(spyroInputDTOList,plantId,year);
	}

	@GetMapping(value = "/spyro-input-export-excel")
	public ResponseEntity<byte[]> exportConfigurationConstantsReport(
	         @RequestParam String year,@RequestParam String plantId,@RequestParam String mode
	        ) {
	    try {
			
	        byte[] excelBytes = spyroInputService.createExcel(year, plantId, mode, false, null);

	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.parseMediaType(
	                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
	        headers.setContentDisposition(ContentDisposition.builder("attachment")
	                .filename("SpyroInput.xlsx")
	                .build());
	        headers.setContentLength(excelBytes.length);

	        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
	    } catch (Exception e) {
	        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}

	@PostMapping(value = "/spyro-input-import-excel", consumes = "multipart/form-data")
	public AOPMessageVM importExcel(
	         @RequestParam("plantId") String plantId,
            @RequestParam("year") String year,
			@RequestParam("mode") String mode,
			@RequestParam("file") MultipartFile file
	        ) {
			return	spyroInputService.importExcel(year, plantId, mode, file); 
	}

	/**
	 * V2 export: "Reactor Parameters" and "Recovery Parameters" tables use a single
	 * "Value" column (April value) instead of 12 month columns. All other tables
	 * are identical to the standard export.
	 */
	@GetMapping(value = "/spyro-input-export-excel-value")
	public ResponseEntity<byte[]> exportSpyroInputV2(
			@RequestParam String year, @RequestParam String plantId, @RequestParam String mode) {
		try {
			byte[] excelBytes = spyroInputService.createExcelV2(year, plantId, mode, false, null);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
			headers.setContentDisposition(ContentDisposition.builder("attachment")
					.filename("SpyroInputV2.xlsx")
					.build());
			headers.setContentLength(excelBytes.length);

			return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * V2 import: reads the V2 Excel format where "Reactor Parameters" and
	 * "Recovery Parameters" rows carry a single "Value" column mapped to April.
	 */
	@PostMapping(value = "/spyro-input-import-excel-value", consumes = "multipart/form-data")
	public AOPMessageVM importExcelV2(
			@RequestParam("plantId") String plantId,
			@RequestParam("year") String year,
			@RequestParam("mode") String mode,
			@RequestParam("file") MultipartFile file) {
		return spyroInputService.importExcelV2(year, plantId, mode, file);
	}

	@GetMapping(value="/modes")
	public AOPMessageVM getModes(@RequestParam String year,@RequestParam String plantId,@RequestParam String type){
		return	spyroInputService.getModes(year, plantId, type);
	}
		
	@GetMapping(value="/furnace-dropdown")
	public AOPMessageVM getFurnaceDropdown(@RequestParam String plantId) {
		return spyroInputService.getFurnaceDropdown(plantId);
	}

	@GetMapping(value = "/spyro-input/calculate")
	public AOPMessageVM calculateSpyroInputData(@RequestParam String year, @RequestParam String plantId,
			@RequestParam String Mode, @RequestParam String type) {
		return spyroInputService.calculateSpyroInputData(year, plantId, Mode, type);
	}

	@GetMapping(value = "/optimizing-variables-dropdown")
	public AOPMessageVM getOptimizingVariablesDropdown(@RequestParam String plantId, @RequestParam String aopYear) {
		return spyroInputService.getOptimizingVariablesDropdown(plantId, aopYear);
	}

	@PostMapping(value = "/optimizing-variables-dropdown")
	public AOPMessageVM updateOptimizingVariablesDropdown(
			@RequestBody List<OptimizingVariablesDropdownDTO> dtoList,
			@RequestParam String plantId,
			@RequestParam String aopYear) {
		List<OptimizingVariablesDropdownDTO> failedRecords = spyroInputService.updateOptimizingVariablesDropdown(dtoList, plantId, aopYear);

		if (failedRecords.isEmpty()) {
			return new AOPMessageVM(200, "Data updated successfully", null);
		} else {
			return new AOPMessageVM(422, "Partial data updated", failedRecords);
		}
	}

	@GetMapping(value = "/feed-type-flow-mappings")
	public AOPMessageVM getFeedTypeFlowMappings(@RequestParam String plantId, @RequestParam String aopYear) {
		return spyroInputService.getFeedTypeFlowMappings(plantId, aopYear);
	}

	@GetMapping(value = "/spyro-input-min-max")
	public AOPMessageVM getSpyroInputMinMax(@RequestParam String plantId, @RequestParam String siteId, @RequestParam String verticalId, @RequestParam String aopYear, @RequestParam String mode) {
		return spyroInputService.getSpyroInputMinMax(plantId, siteId, verticalId, aopYear, mode);
	}

	@GetMapping(value = "/spyro-input-min-max-export")
	public ResponseEntity<byte[]> exportSpyroInputMinMax(
			@RequestParam String plantId,
			@RequestParam String siteId,
			@RequestParam String verticalId,
			@RequestParam String aopYear,
			@RequestParam String mode) {
		try {
			byte[] excelBytes = spyroInputService.createSpyroInputMinMaxExcel(plantId, siteId, verticalId, aopYear, mode, false, null);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
			headers.setContentDisposition(ContentDisposition.builder("attachment")
					.filename("SpyroInputMinMax.xlsx")
					.build());
			headers.setContentLength(excelBytes.length);

			return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/spyro-input-min-max-import", consumes = "multipart/form-data")
	public AOPMessageVM importSpyroInputMinMax(
			@RequestParam String plantId,
			@RequestParam String siteId,
			@RequestParam String verticalId,
			@RequestParam String aopYear,
			@RequestParam String mode,
			@RequestParam("file") MultipartFile file) {
		return spyroInputService.importSpyroInputMinMaxExcel(plantId, siteId, verticalId, aopYear, mode, file);
	}

	@GetMapping(value="/naptha-summary")
	public AOPMessageVM getNapthaSummaryDataSet(@RequestParam String plantId,@RequestParam String year, @RequestParam String reportType) {
		return spyroInputService.getNapthaSummaryDataSet(plantId,year,reportType);
	}

	@GetMapping(value = "/naptha-summary-export")
	public ResponseEntity<byte[]> exportNapthaSummary(
			@RequestParam String plantId,
			@RequestParam String year,
			@RequestParam String reportType) {
		try {
			byte[] excelBytes = spyroInputService.createNapthaSummaryExcel(plantId, year, reportType);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
			headers.setContentDisposition(ContentDisposition.builder("attachment")
					.filename("NapthaSummary.xlsx")
					.build());
			headers.setContentLength(excelBytes.length);

			return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}

