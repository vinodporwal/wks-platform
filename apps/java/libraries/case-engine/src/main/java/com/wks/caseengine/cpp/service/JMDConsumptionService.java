package com.wks.caseengine.cpp.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.cpp.dto.CalculatedProcessDemandDTO;
import com.wks.caseengine.cpp.dto.PlantRequirementDTO;
import com.wks.caseengine.cpp.dto.ProcessDemandUpdateRequest;
import com.wks.caseengine.cpp.dto.ProcessDemandUpdateResponse;

/**
 * Service interface for JMD Consumption operations.
 * Supports both single plant and multiple plants operations.
 */
public interface JMDConsumptionService {

	/**
	 * Get CPP consumptions for a single plant.
	 * 
	 * @param plantId UUID of the plant
	 * @param financialYear Financial year (e.g., "2026-27")
	 * @return List of PlantRequirementDTO
	 */
	public List<PlantRequirementDTO> getCppConsumptions(UUID plantId, String financialYear);

	/**
	 * Get CPP consumptions for multiple plants.
	 * 
	 * @param plantIds List of plant UUIDs
	 * @param financialYear Financial year (e.g., "2026-27")
	 * @return List of PlantRequirementDTO
	 */
	public List<PlantRequirementDTO> getCppConsumptionsForMultiplePlants(List<UUID> plantIds, String financialYear);

	/**
	 * Get process demand for all plants.
	 * 
	 * @param financialYear Financial year (e.g., "2026-27")
	 * @return List of CalculatedProcessDemandDTO
	 */
	public List<CalculatedProcessDemandDTO> getProcessDemand(String financialYear);

	/**
	 * Get process demand for a single plant.
	 * 
	 * @param plantId UUID of the plant
	 * @param financialYear Financial year (e.g., "2026-27")
	 * @return List of CalculatedProcessDemandDTO
	 */
	public List<CalculatedProcessDemandDTO> getProcessDemandByPlant(UUID plantId, String financialYear);

	/**
	 * Get process demand for multiple plants.
	 * 
	 * @param plantIds List of plant UUIDs
	 * @param financialYear Financial year (e.g., "2026-27")
	 * @return List of CalculatedProcessDemandDTO
	 */
	public List<CalculatedProcessDemandDTO> getProcessDemandByMultiplePlants(List<UUID> plantIds, String financialYear);

	/**
	 * Update process demand.
	 * 
	 * @param financialYear Financial year (e.g., "2026-27")
	 * @param requests List of ProcessDemandUpdateRequest
	 * @return ProcessDemandUpdateResponse
	 */
	public ProcessDemandUpdateResponse updateProcessDemand(String financialYear, List<ProcessDemandUpdateRequest> requests);

	/**
	 * Export consumption data for a single plant to Excel.
	 * 
	 * @param plantId UUID of the plant
	 * @param financialYear Financial year (e.g., "2026-27")
	 * @param isAfterSave Whether this is after save operation
	 * @param dtoList List of CalculatedProcessDemandDTO (optional)
	 * @return Excel file as byte array
	 */
	public byte[] exportConsumption(UUID plantId, String financialYear, boolean isAfterSave, List<CalculatedProcessDemandDTO> dtoList);

	/**
	 * Export consumption data for multiple plants to Excel.
	 * 
	 * @param plantIds List of plant UUIDs
	 * @param financialYear Financial year (e.g., "2026-27")
	 * @param isAfterSave Whether this is after save operation
	 * @param dtoList List of CalculatedProcessDemandDTO (optional)
	 * @return Excel file as byte array
	 */
	public byte[] exportConsumptionForMultiplePlants(List<UUID> plantIds, String financialYear, boolean isAfterSave, List<CalculatedProcessDemandDTO> dtoList);

	/**
	 * Import consumption data from Excel for a single plant.
	 * 
	 * @param plantId UUID of the plant
	 * @param financialYear Financial year (e.g., "2026-27")
	 * @param file MultipartFile containing Excel data
	 * @return AOPMessageVM with import status
	 */
	public AOPMessageVM importExcel(UUID plantId, String financialYear, MultipartFile file);

	/**
	 * Import consumption data from Excel for multiple plants.
	 * 
	 * @param plantIds List of plant UUIDs
	 * @param financialYear Financial year (e.g., "2026-27")
	 * @param file MultipartFile containing Excel data
	 * @return AOPMessageVM with import status
	 */
	public AOPMessageVM importExcelForMultiplePlants(List<UUID> plantIds, String financialYear, MultipartFile file);
}
