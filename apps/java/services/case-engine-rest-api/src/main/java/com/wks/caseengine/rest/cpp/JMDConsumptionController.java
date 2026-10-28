package com.wks.caseengine.rest.cpp;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wks.caseengine.cpp.dto.CalculatedProcessDemandDTO;
import com.wks.caseengine.cpp.dto.PlantRequirementDTO;
import com.wks.caseengine.cpp.dto.ProcessDemandUpdateRequest;
import com.wks.caseengine.cpp.dto.ProcessDemandUpdateResponse;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.cpp.service.JMDConsumptionService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/task")
@Tag(name = "JMD Consumption", description = "JMD Plant Requirement and Consumption Management")
public class JMDConsumptionController {

	private static final Logger logger = LoggerFactory.getLogger(JMDConsumptionController.class);

	@Autowired
	private JMDConsumptionService jmdConsumptionService;

	@GetMapping(value = "/jmd-plant-requirement")
	public ResponseEntity<List<PlantRequirementDTO>> getCppConsumptions(
			@RequestParam(required = false) UUID plantId,
			@RequestParam(required = false) List<UUID> plantIds,
			@RequestParam String financialYear) {
		logger.info("[GET] Fetching plant requirement for plantId: {}, plantIds: {}, financialYear: {}", plantId, plantIds, financialYear);
		try {
			List<PlantRequirementDTO> result;
			if (plantId != null) {
				result = jmdConsumptionService.getCppConsumptions(plantId, financialYear);
			} else if (plantIds != null && !plantIds.isEmpty()) {
				result = jmdConsumptionService.getCppConsumptionsForMultiplePlants(plantIds, financialYear);
			} else {
				throw new IllegalArgumentException("Either plantId or plantIds must be provided");
			}
			logger.info("[GET] Successfully fetched plant requirement");
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			logger.error("[GET] Error fetching plant requirement: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	@GetMapping(value = "/jmd-process-demand")
	public ResponseEntity<List<CalculatedProcessDemandDTO>> getProcessDemand(
			@RequestParam(required = false) UUID plantId,
			@RequestParam(required = false) List<UUID> plantIds,
			@RequestParam String financialYear) {
		logger.info("[GET] Fetching process demand for plantId: {}, plantIds: {}, financialYear: {}", plantId, plantIds, financialYear);
		try {
			List<CalculatedProcessDemandDTO> result;
			if (plantId != null) {
				result = jmdConsumptionService.getProcessDemandByPlant(plantId, financialYear);
			} else if (plantIds != null && !plantIds.isEmpty()) {
				result = jmdConsumptionService.getProcessDemandByMultiplePlants(plantIds, financialYear);
			} else {
				result = jmdConsumptionService.getProcessDemand(financialYear);
			}
			logger.info("[GET] Successfully fetched process demand");
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			logger.error("[GET] Error fetching process demand: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	@PostMapping(value = "/jmd-process-demand/{financialYear}")
	public ResponseEntity<ProcessDemandUpdateResponse> updateProcessDemand(
			@PathVariable String financialYear,
			@RequestBody List<ProcessDemandUpdateRequest> requests) {
		logger.info("[POST] Updating process demand for financialYear: {}", financialYear);
		try {
			ProcessDemandUpdateResponse response = jmdConsumptionService.updateProcessDemand(financialYear, requests);
			logger.info("[POST] Process demand updated successfully");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			logger.error("[POST] Error updating process demand: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	@GetMapping(value = "/jmd-consumption/export")
	public ResponseEntity<byte[]> exportConsumption(
			@RequestParam(required = false) UUID plantId,
			@RequestParam(required = false) List<UUID> plantIds,
			@RequestParam String financialYear) {
		logger.info("[GET] Exporting consumption for plantId: {}, plantIds: {}, financialYear: {}", plantId, plantIds, financialYear);
		try {
			byte[] excelFile;
			if (plantId != null) {
				excelFile = jmdConsumptionService.exportConsumption(plantId, financialYear, false, null);
			} else if (plantIds != null && !plantIds.isEmpty()) {
				excelFile = jmdConsumptionService.exportConsumptionForMultiplePlants(plantIds, financialYear, false, null);
			} else {
				throw new IllegalArgumentException("Either plantId or plantIds must be provided");
			}
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
			headers.setContentDispositionFormData("attachment", "JMDConsumption_" + financialYear + ".xlsx");
			logger.info("[GET] Consumption exported successfully");
			return ResponseEntity.ok().headers(headers).body(excelFile);
		} catch (Exception e) {
			logger.error("[GET] Error exporting consumption: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	@PostMapping(value = "/jmd-consumption/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<AOPMessageVM> importConsumption(
			@RequestParam(required = false) UUID plantId,
			@RequestParam(required = false) List<UUID> plantIds,
			@RequestParam String financialYear,
			@RequestParam("file") MultipartFile file) {
		logger.info("[POST] Importing consumption for plantId: {}, plantIds: {}, financialYear: {}", plantId, plantIds, financialYear);
		if (file == null || file.isEmpty()) {
			AOPMessageVM errorResponse = new AOPMessageVM();
			errorResponse.setCode(400);
			errorResponse.setMessage("File is required");
			errorResponse.setData(null);
			return ResponseEntity.badRequest().body(errorResponse);
		}
		try {
			AOPMessageVM result;
			if (plantId != null) {
				result = jmdConsumptionService.importExcel(plantId, financialYear, file);
			} else if (plantIds != null && !plantIds.isEmpty()) {
				result = jmdConsumptionService.importExcelForMultiplePlants(plantIds, financialYear, file);
			} else {
				throw new IllegalArgumentException("Either plantId or plantIds must be provided");
			}
			logger.info("[POST] Consumption imported successfully");
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			logger.error("[POST] Error importing consumption: {}", e.getMessage(), e);
			AOPMessageVM errorResponse = new AOPMessageVM();
			errorResponse.setCode(500);
			errorResponse.setMessage("Failed to import consumption: " + e.getMessage());
			errorResponse.setData(null);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}
	}
}
