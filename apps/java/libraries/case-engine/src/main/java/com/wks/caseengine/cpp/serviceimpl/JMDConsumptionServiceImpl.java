package com.wks.caseengine.cpp.serviceimpl;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.service.ConsumptionService;
import com.wks.caseengine.cpp.service.JMDConsumptionService;
import com.wks.caseengine.cpp.dto.CalculatedProcessDemandDTO;
import com.wks.caseengine.cpp.dto.PlantRequirementDTO;
import com.wks.caseengine.cpp.dto.ProcessDemandUpdateRequest;
import com.wks.caseengine.cpp.dto.ProcessDemandUpdateResponse;
import com.wks.caseengine.message.vm.AOPMessageVM;

/**
 * Implementation of JMDConsumptionService.
 * Wraps ConsumptionService to support both single plant and multiple plants
 * operations.
 */
@Service
public class JMDConsumptionServiceImpl implements JMDConsumptionService {

	private static final Logger logger = LoggerFactory.getLogger(JMDConsumptionServiceImpl.class);

	@Autowired
	private ConsumptionService consumptionService;

	@Override
	public List<PlantRequirementDTO> getCppConsumptions(UUID plantId, String financialYear) {
		logger.info("[JMDConsumption] Fetching CPP consumptions for plantId: {}, financialYear: {}", plantId,
				financialYear);
		try {
			List<PlantRequirementDTO> result = consumptionService.getCppConsumptions(plantId, financialYear);
			logger.info("[JMDConsumption] Successfully fetched CPP consumptions");
			return result;
		} catch (Exception e) {
			logger.error("[JMDConsumption] Error fetching CPP consumptions: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to fetch CPP consumptions: " + e.getMessage(), e);
		}
	}

	@Override
	public List<PlantRequirementDTO> getCppConsumptionsForMultiplePlants(List<UUID> plantIds, String financialYear) {
		logger.info("[JMDConsumption] Fetching CPP consumptions for plantIds: {}, financialYear: {}", plantIds,
				financialYear);
		try {
			List<PlantRequirementDTO> allResults = new ArrayList<>();
			for (UUID plantId : plantIds) {
				List<PlantRequirementDTO> result = consumptionService.getCppConsumptions(plantId, financialYear);
				allResults.addAll(result);
			}
			logger.info("[JMDConsumption] Successfully fetched CPP consumptions for {} plants", plantIds.size());
			return allResults;
		} catch (Exception e) {
			logger.error("[JMDConsumption] Error fetching CPP consumptions for multiple plants: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to fetch CPP consumptions: " + e.getMessage(), e);
		}
	}

	@Override
	public List<CalculatedProcessDemandDTO> getProcessDemand(String financialYear) {
		logger.info("[JMDConsumption] Fetching process demand for financialYear: {}", financialYear);
		try {
			List<CalculatedProcessDemandDTO> result = consumptionService.getProcessDemand(financialYear);
			logger.info("[JMDConsumption] Successfully fetched process demand");
			return result;
		} catch (Exception e) {
			logger.error("[JMDConsumption] Error fetching process demand: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to fetch process demand: " + e.getMessage(), e);
		}
	}

	@Override
	public List<CalculatedProcessDemandDTO> getProcessDemandByPlant(UUID plantId, String financialYear) {
		logger.info("[JMDConsumption] Fetching process demand by plant for plantId: {}, financialYear: {}", plantId,
				financialYear);
		try {
			List<CalculatedProcessDemandDTO> result = consumptionService.getProcessDemandByPlant(plantId,
					financialYear);
			logger.info("[JMDConsumption] Successfully fetched process demand by plant");
			return result;
		} catch (Exception e) {
			logger.error("[JMDConsumption] Error fetching process demand by plant: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to fetch process demand by plant: " + e.getMessage(), e);
		}
	}

	@Override
	public List<CalculatedProcessDemandDTO> getProcessDemandByMultiplePlants(List<UUID> plantIds,
			String financialYear) {
		logger.info("[JMDConsumption] Fetching process demand by plants for plantIds: {}, financialYear: {}", plantIds,
				financialYear);
		try {
			List<CalculatedProcessDemandDTO> allResults = new ArrayList<>();
			for (UUID plantId : plantIds) {
				List<CalculatedProcessDemandDTO> result = consumptionService.getProcessDemandByPlant(plantId,
						financialYear);
				allResults.addAll(result);
			}
			logger.info("[JMDConsumption] Successfully fetched process demand for {} plants", plantIds.size());
			return allResults;
		} catch (Exception e) {
			logger.error("[JMDConsumption] Error fetching process demand by multiple plants: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to fetch process demand: " + e.getMessage(), e);
		}
	}

	@Override
	public ProcessDemandUpdateResponse updateProcessDemand(String financialYear,
			List<ProcessDemandUpdateRequest> requests) {
		logger.info("[JMDConsumption] Updating process demand for financialYear: {}", financialYear);
		try {
			ProcessDemandUpdateResponse response = consumptionService.updateProcessDemand(financialYear, requests);
			logger.info("[JMDConsumption] Process demand updated successfully");
			return response;
		} catch (Exception e) {
			logger.error("[JMDConsumption] Error updating process demand: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to update process demand: " + e.getMessage(), e);
		}
	}

	@Override
	public byte[] exportConsumption(UUID plantId, String financialYear, boolean isAfterSave,
			List<CalculatedProcessDemandDTO> dtoList) {
		logger.info("[JMDConsumption] Exporting consumption for plantId: {}, financialYear: {}", plantId,
				financialYear);
		try {
			byte[] result = consumptionService.exportConsumption(plantId, financialYear, isAfterSave, dtoList);
			logger.info("[JMDConsumption] Consumption exported successfully");
			return result;
		} catch (Exception e) {
			logger.error("[JMDConsumption] Error exporting consumption: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to export consumption: " + e.getMessage(), e);
		}
	}

	@Override
	public byte[] exportPlantRequirement(UUID plantId, String financialYear) {
		logger.info("[JMDConsumption] Exporting plant requirement for plantId: {}, financialYear: {}", plantId,
				financialYear);
		try {
			List<CalculatedProcessDemandDTO> processDemand = getProcessDemandByPlant(plantId, financialYear);
			return generatePlantRequirementExcel(processDemand, financialYear, false);
		} catch (Exception e) {
			logger.error("[JMDConsumption] Error exporting plant requirement: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to export plant requirement: " + e.getMessage(), e);
		}
	}

	@Override
	public byte[] exportPlantRequirementForMultiplePlants(List<UUID> plantIds, String financialYear) {
		logger.info("[JMDConsumption] Exporting plant requirement for plantIds: {}, financialYear: {}", plantIds,
				financialYear);
		try {
			List<CalculatedProcessDemandDTO> processDemand = getProcessDemandByMultiplePlants(plantIds, financialYear);
			return generatePlantRequirementExcel(processDemand, financialYear, false);
		} catch (Exception e) {
			logger.error("[JMDConsumption] Error exporting plant requirement: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to export plant requirement: " + e.getMessage(), e);
		}
	}

	private byte[] generatePlantRequirementExcel(
			List<CalculatedProcessDemandDTO> processDemand, String financialYear, boolean includeErrors)
			throws Exception {
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("Consumption");
			CellStyle headerStyle = createHeaderStyle(workbook);
			CellStyle dataStyle = createDataStyle(workbook);
			CellStyle remarksStyle = createRemarksStyle(workbook);
			CellStyle errorStyle = createDataStyle(workbook);
			Font errorFont = workbook.createFont();
			errorFont.setColor(IndexedColors.RED.getIndex());
			errorStyle.setFont(errorFont);
			String startYearSuffix = financialYear.substring(2, 4);
			String endYearSuffix = financialYear.substring(5, 7);

			List<String> headers = new ArrayList<>();
			headers.add("Parent Plant");
			headers.add("Process Plant");
			headers.add("CPP Utilities");
			headers.add("CPP Utility Ids");
			headers.add("CPP Plant");
			headers.add("UOM");
			headers.add("Apr-" + startYearSuffix);
			headers.add("May-" + startYearSuffix);
			headers.add("Jun-" + startYearSuffix);
			headers.add("Jul-" + startYearSuffix);
			headers.add("Aug-" + startYearSuffix);
			headers.add("Sep-" + startYearSuffix);
			headers.add("Oct-" + startYearSuffix);
			headers.add("Nov-" + startYearSuffix);
			headers.add("Dec-" + startYearSuffix);
			headers.add("Jan-" + endYearSuffix);
			headers.add("Feb-" + endYearSuffix);
			headers.add("Mar-" + endYearSuffix);
			headers.add("Remarks");
			headers.add("Plant Code");
			if (includeErrors) {
				headers.add("Status");
				headers.add("Error Description");
			}

			Row headerRow = sheet.createRow(0);
			for (int col = 0; col < headers.size(); col++) {
				Cell cell = headerRow.createCell(col);
				cell.setCellValue(headers.get(col));
				cell.setCellStyle(headerStyle);
			}
			sheet.setColumnHidden(19, true);

			int rowIndex = 1;
			for (CalculatedProcessDemandDTO dto : processDemand) {
				Row row = sheet.createRow(rowIndex++);
				int col = 0;
				Cell cell = row.createCell(col++);
				cell.setCellValue(dto.getParentPlantName() != null ? dto.getParentPlantName() : "");
				cell.setCellStyle(dataStyle);
				cell = row.createCell(col++);
				cell.setCellValue(dto.getProcessPlant() != null ? dto.getProcessPlant() : "");
				cell.setCellStyle(dataStyle);
				cell = row.createCell(col++);
				cell.setCellValue(dto.getCppUtility() != null ? dto.getCppUtility() : "");
				cell.setCellStyle(dataStyle);
				cell = row.createCell(col++);
				cell.setCellValue(dto.getCppUtilityId() != null ? dto.getCppUtilityId() : "");
				cell.setCellStyle(dataStyle);
				cell = row.createCell(col++);
				cell.setCellValue(dto.getCppPlant() != null ? dto.getCppPlant() : "");
				cell.setCellStyle(dataStyle);
				cell = row.createCell(col++);
				cell.setCellValue(dto.getUom() != null ? dto.getUom() : "");
				cell.setCellStyle(dataStyle);

				setDoubleCellValue(row.createCell(col++), dto.getApr(), dataStyle);
				setDoubleCellValue(row.createCell(col++), dto.getMay(), dataStyle);
				setDoubleCellValue(row.createCell(col++), dto.getJun(), dataStyle);
				setDoubleCellValue(row.createCell(col++), dto.getJul(), dataStyle);
				setDoubleCellValue(row.createCell(col++), dto.getAug(), dataStyle);
				setDoubleCellValue(row.createCell(col++), dto.getSep(), dataStyle);
				setDoubleCellValue(row.createCell(col++), dto.getOct(), dataStyle);
				setDoubleCellValue(row.createCell(col++), dto.getNov(), dataStyle);
				setDoubleCellValue(row.createCell(col++), dto.getDec(), dataStyle);
				setDoubleCellValue(row.createCell(col++), dto.getJan(), dataStyle);
				setDoubleCellValue(row.createCell(col++), dto.getFeb(), dataStyle);
				setDoubleCellValue(row.createCell(col++), dto.getMar(), dataStyle);

				cell = row.createCell(col++);
				cell.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
				cell.setCellStyle(remarksStyle);
				cell = row.createCell(col++);
				cell.setCellValue(dto.getProcessPlantId() != null ? dto.getProcessPlantId() : "");
				cell.setCellStyle(dataStyle);
				if (includeErrors) {
					cell = row.createCell(col++);
					cell.setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
					cell.setCellStyle(dataStyle);
					cell = row.createCell(col);
					cell.setCellValue(dto.getErrDescription() != null ? dto.getErrDescription() : "");
					cell.setCellStyle(errorStyle);
				}
			}

			for (int col = 0; col < headers.size(); col++) {
				if (col == 18) {
					sheet.setColumnWidth(col, 8000);
					continue;
				}
				sheet.autoSizeColumn(col);
				int headerWidth = Math.min(255 * 256, (headers.get(col).length() + 2) * 256);
				if (sheet.getColumnWidth(col) < headerWidth) {
					sheet.setColumnWidth(col, headerWidth);
				}
			}

			workbook.write(outputStream);
			return outputStream.toByteArray();
		}
	}

	@Override
	public byte[] exportConsumptionForMultiplePlants(List<UUID> plantIds, String financialYear, boolean isAfterSave,
			List<CalculatedProcessDemandDTO> dtoList) {
		logger.info("[JMDConsumption] Exporting consumption for plantIds: {}, financialYear: {}", plantIds,
				financialYear);
		try {
			// Aggregate data for all plants
			List<CalculatedProcessDemandDTO> aggregatedData = new ArrayList<>();

			if (dtoList == null || dtoList.isEmpty()) {
				// Fetch data for each plant and aggregate
				for (UUID plantId : plantIds) {
					logger.debug("[JMDConsumption] Fetching consumption data for plantId: {}", plantId);
					List<CalculatedProcessDemandDTO> plantData = consumptionService.getProcessDemandByPlant(plantId,
							financialYear);
					if (plantData != null && !plantData.isEmpty()) {
						aggregatedData.addAll(plantData);
						logger.debug("[JMDConsumption] Fetched {} records for plantId: {}", plantData.size(), plantId);
					}
				}
			} else {
				// Use provided data
				aggregatedData = dtoList;
			}

			// Export with aggregated data from all plants
			logger.info("[JMDConsumption] Exporting {} records for {} plants", aggregatedData.size(), plantIds.size());
			byte[] result = generateExcelForMultiplePlants(aggregatedData, financialYear, isAfterSave);
			logger.info("[JMDConsumption] Consumption exported successfully for multiple plants");
			return result;
		} catch (Exception e) {
			logger.error("[JMDConsumption] Error exporting consumption for multiple plants: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to export consumption: " + e.getMessage(), e);
		}
	}

	/**
	 * Generate Excel file with aggregated data from multiple plants
	 */
	private byte[] generateExcelForMultiplePlants(List<CalculatedProcessDemandDTO> dtoList, String financialYear,
			boolean isAfterSave) throws Exception {
		logger.debug("[JMDConsumption] Generating Excel for {} records", dtoList.size());

		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Consumption");
		int currentRow = 0;

		CellStyle headerStyle = createHeaderStyle(workbook);
		CellStyle dataStyle = createDataStyle(workbook);
		CellStyle remarksStyle = createRemarksStyle(workbook);
		CellStyle errorDescriptionStyle = createDataStyle(workbook);
		Font errorFont = workbook.createFont();
		errorFont.setColor(IndexedColors.RED.getIndex());
		errorDescriptionStyle.setFont(errorFont);
		String startYearSuffix = financialYear.substring(2, 4);
		String endYearSuffix = financialYear.substring(5, 7);

		// Header row
		List<String> headers = new ArrayList<>();
		headers.add("Parent Plant");
		// Sender (CPP) columns
		headers.add("Sender Utility");
		headers.add("Sender Utility Code");
		headers.add("Sender UOM");
		headers.add("Sender Plant");
		headers.add("Sender Plant Code");
		headers.add("Sender Cost Center");
		headers.add("Sender Cost Center Code");
		// Receiver (Process Plant) columns
		headers.add("Receiver Utility");
		headers.add("Receiver Utility Code");
		headers.add("Receiver UOM");
		headers.add("Receiver Plant");
		headers.add("Plant Code");
		headers.add("Receiver Cost Center");
		headers.add("Receiver Cost Center Code");
		headers.add("Apr-" + startYearSuffix);
		headers.add("May-" + startYearSuffix);
		headers.add("Jun-" + startYearSuffix);
		headers.add("Jul-" + startYearSuffix);
		headers.add("Aug-" + startYearSuffix);
		headers.add("Sep-" + startYearSuffix);
		headers.add("Oct-" + startYearSuffix);
		headers.add("Nov-" + startYearSuffix);
		headers.add("Dec-" + startYearSuffix);
		headers.add("Jan-" + endYearSuffix);
		headers.add("Feb-" + endYearSuffix);
		headers.add("Mar-" + endYearSuffix);
		headers.add("Total");
		headers.add("Remarks");
		if (isAfterSave) {
			headers.add("Status");
			headers.add("Error Description");
		}

		Row headerRow = sheet.createRow(currentRow++);
		for (int col = 0; col < headers.size(); col++) {
			Cell cell = headerRow.createCell(col);
			cell.setCellValue(headers.get(col));
			cell.setCellStyle(headerStyle);
		}

		// Data rows
		for (CalculatedProcessDemandDTO dto : dtoList) {
			Row row = sheet.createRow(currentRow++);
			int col = 0;

			Cell cell = row.createCell(col++);
			cell.setCellValue(dto.getParentPlantName() != null ? dto.getParentPlantName() : "");
			cell.setCellStyle(dataStyle);

			// Sender (CPP) columns
			cell = row.createCell(col++);
			cell.setCellValue(dto.getCppUtility() != null ? dto.getCppUtility() : "");
			cell.setCellStyle(dataStyle);
			cell = row.createCell(col++);
			cell.setCellValue(dto.getCppUtilityId() != null ? dto.getCppUtilityId() : "");
			cell.setCellStyle(dataStyle);
			cell = row.createCell(col++);
			cell.setCellValue(dto.getUom() != null ? dto.getUom() : "");
			cell.setCellStyle(dataStyle);
			cell = row.createCell(col++);
			cell.setCellValue(dto.getCppPlant() != null ? dto.getCppPlant() : "");
			cell.setCellStyle(dataStyle);
			cell = row.createCell(col++);
			cell.setCellValue(dto.getCppPlantId() != null ? dto.getCppPlantId() : "");
			cell.setCellStyle(dataStyle);
			cell = row.createCell(col++);
			cell.setCellValue(dto.getCppPlantCostCenter() != null ? dto.getCppPlantCostCenter() : "");
			cell.setCellStyle(dataStyle);
			cell = row.createCell(col++);
			cell.setCellValue(dto.getCppPlantCostCenterCode() != null ? dto.getCppPlantCostCenterCode() : "");
			cell.setCellStyle(dataStyle);

			// Receiver (Process Plant) columns
			cell = row.createCell(col++);
			cell.setCellValue(dto.getProcessPlantUtility() != null ? dto.getProcessPlantUtility() : "");
			cell.setCellStyle(dataStyle);
			cell = row.createCell(col++);
			cell.setCellValue(dto.getProcessPlantUtilityId() != null ? dto.getProcessPlantUtilityId() : "");
			cell.setCellStyle(dataStyle);
			cell = row.createCell(col++);
			cell.setCellValue(dto.getProcessPlantUom() != null ? dto.getProcessPlantUom() : "");
			cell.setCellStyle(dataStyle);
			cell = row.createCell(col++);
			cell.setCellValue(dto.getProcessPlant() != null ? dto.getProcessPlant() : "");
			cell.setCellStyle(dataStyle);
			cell = row.createCell(col++);
			cell.setCellValue(dto.getProcessPlantId() != null ? dto.getProcessPlantId() : "");
			cell.setCellStyle(dataStyle);
			cell = row.createCell(col++);
			cell.setCellValue(dto.getProcessPlantCostCenter() != null ? dto.getProcessPlantCostCenter() : "");
			cell.setCellStyle(dataStyle);
			cell = row.createCell(col++);
			cell.setCellValue(dto.getProcessPlantCostCenterCode() != null ? dto.getProcessPlantCostCenterCode() : "");
			cell.setCellStyle(dataStyle);

			double total = 0.0;
			total += setDoubleCellValue(row.createCell(col++), dto.getApr(), dataStyle);
			total += setDoubleCellValue(row.createCell(col++), dto.getMay(), dataStyle);
			total += setDoubleCellValue(row.createCell(col++), dto.getJun(), dataStyle);
			total += setDoubleCellValue(row.createCell(col++), dto.getJul(), dataStyle);
			total += setDoubleCellValue(row.createCell(col++), dto.getAug(), dataStyle);
			total += setDoubleCellValue(row.createCell(col++), dto.getSep(), dataStyle);
			total += setDoubleCellValue(row.createCell(col++), dto.getOct(), dataStyle);
			total += setDoubleCellValue(row.createCell(col++), dto.getNov(), dataStyle);
			total += setDoubleCellValue(row.createCell(col++), dto.getDec(), dataStyle);
			total += setDoubleCellValue(row.createCell(col++), dto.getJan(), dataStyle);
			total += setDoubleCellValue(row.createCell(col++), dto.getFeb(), dataStyle);
			total += setDoubleCellValue(row.createCell(col++), dto.getMar(), dataStyle);

			cell = row.createCell(col++);
			cell.setCellValue(total);
			cell.setCellStyle(dataStyle);

			cell = row.createCell(col++);
			cell.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
			cell.setCellStyle(remarksStyle);
			if (isAfterSave) {
				cell = row.createCell(col++);
				cell.setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
				cell.setCellStyle(dataStyle);
				cell = row.createCell(col++);
				cell.setCellValue(dto.getErrDescription() != null ? dto.getErrDescription() : "");
				cell.setCellStyle(errorDescriptionStyle);
			}
		}

		// Auto-size columns
		for (int i = 0; i < headers.size(); i++) {
			sheet.autoSizeColumn(i);
		}

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		workbook.write(outputStream);
		workbook.close();

		logger.debug("[JMDConsumption] Excel generated successfully with {} rows", dtoList.size());
		return outputStream.toByteArray();
	}

	/**
	 * Create header cell style
	 */
	private CellStyle createHeaderStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		Font font = workbook.createFont();
		font.setBold(true);
		style.setFont(font);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		return style;
	}

	/**
	 * Create data cell style
	 */
	private CellStyle createDataStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		return style;
	}

	/**
	 * Create remarks cell style
	 */
	private CellStyle createRemarksStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		style.setWrapText(true);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		return style;
	}

	/**
	 * Set double value in cell with proper formatting
	 */
	private double setDoubleCellValue(Cell cell, Double value, CellStyle style) {
		if (value != null) {
			cell.setCellValue(value);
		}
		cell.setCellStyle(style);
		return value != null ? value : 0.0;
	}

	@Override
	public AOPMessageVM importPlantRequirement(UUID plantId, String financialYear, MultipartFile file) {
		try {
			List<CalculatedProcessDemandDTO> importedRows;
			try (InputStream inputStream = file.getInputStream()) {
				importedRows = readPlantRequirementExcel(inputStream, financialYear);
			}
			if (importedRows.isEmpty()) {
				return createImportResponse(400, "The Excel file contains no data rows", null);
			}

			Map<String, CalculatedProcessDemandDTO> existingByKey = new HashMap<>();
			for (CalculatedProcessDemandDTO existing : getProcessDemandByPlant(plantId, financialYear)) {
				existingByKey.put(consumptionKey(existing.getProcessPlantId(), existing.getCppUtilityId()), existing);
			}

			List<CalculatedProcessDemandDTO> failedRows = new ArrayList<>();
			List<CalculatedProcessDemandDTO> validRows = new ArrayList<>();
			List<ProcessDemandUpdateRequest> requests = new ArrayList<>();
			int skippedCount = 0;
			for (CalculatedProcessDemandDTO dto : importedRows) {
				if (dto.getSaveStatus() != null) {
					failedRows.add(dto);
					continue;
				}
				if (isBlank(dto.getProcessPlantId())) {
					dto.setSaveStatus("Failed");
					dto.setErrDescription("Plant Code is missing");
					failedRows.add(dto);
					continue;
				}
				if (isBlank(dto.getCppUtilityId())) {
					dto.setSaveStatus("Failed");
					dto.setErrDescription("CPP Utility Ids is missing");
					failedRows.add(dto);
					continue;
				}

				CalculatedProcessDemandDTO existing = existingByKey.get(
						consumptionKey(dto.getProcessPlantId(), dto.getCppUtilityId()));
				if (existing == null) {
					dto.setSaveStatus("Failed");
					dto.setErrDescription(
							"No process demand record matches Plant Code and CPP Utility Ids for the selected plant");
					failedRows.add(dto);
					continue;
				}
				if (!hasConsumptionValuesChanged(dto, existing)) {
					skippedCount++;
					continue;
				}
				if (isBlank(dto.getRemarks())) {
					dto.setSaveStatus("Failed");
					dto.setErrDescription("Remarks field is mandatory and cannot be empty");
					failedRows.add(dto);
					continue;
				}
				if (existing.getRemarks() != null
						&& existing.getRemarks().trim().equals(dto.getRemarks().trim())) {
					dto.setSaveStatus("Failed");
					dto.setErrDescription(
							"Remarks must be updated to explain the changes. Current remarks are identical to the database value.");
					failedRows.add(dto);
					continue;
				}

				validRows.add(dto);
				requests.add(ProcessDemandUpdateRequest.builder()
						.processPlantId(dto.getProcessPlantId())
						.cppUtilityId(dto.getCppUtilityId())
						.apr(dto.getApr())
						.may(dto.getMay())
						.jun(dto.getJun())
						.jul(dto.getJul())
						.aug(dto.getAug())
						.sep(dto.getSep())
						.oct(dto.getOct())
						.nov(dto.getNov())
						.dec(dto.getDec())
						.jan(dto.getJan())
						.feb(dto.getFeb())
						.mar(dto.getMar())
						.remarks(dto.getRemarks())
						.build());
			}

			if (!requests.isEmpty()) {
				ProcessDemandUpdateResponse updateResponse = consumptionService.updateProcessDemand(financialYear,
						requests);
				List<String> updateErrors = updateResponse.getErrors() == null
						? new ArrayList<>()
						: updateResponse.getErrors();
				boolean hasUnmatchedError = false;
				for (String error : updateErrors) {
					CalculatedProcessDemandDTO failedRow = findRowForUpdateError(validRows, error);
					if (failedRow != null && failedRow.getSaveStatus() == null) {
						failedRow.setSaveStatus("Failed");
						failedRow.setErrDescription(error);
						failedRows.add(failedRow);
					} else {
						hasUnmatchedError = true;
						logger.error(
								"[JMDConsumption] Could not associate plant requirement update error with an imported row: {}",
								error);
					}
				}

				if (hasUnmatchedError || updateResponse.getFailureCount() > updateErrors.size()) {
					for (CalculatedProcessDemandDTO dto : validRows) {
						if (dto.getSaveStatus() == null) {
							dto.setSaveStatus("Failed");
							dto.setErrDescription("The process demand update failed; see server logs for details");
							failedRows.add(dto);
						}
					}
				}
			}

			if (!failedRows.isEmpty()) {
				byte[] failedWorkbook = generatePlantRequirementExcel(failedRows, financialYear, true);
				return createImportResponse(400, "Some JMD plant requirement rows failed to import",
						Base64.getEncoder().encodeToString(failedWorkbook));
			}
			return createImportResponse(200, "All plant requirement rows were imported successfully. "
					+ skippedCount + " unchanged rows skipped, " + validRows.size() + " rows updated.", null);
		} catch (IllegalArgumentException e) {
			logger.warn("[JMDConsumption] Invalid plant requirement import: {}", e.getMessage());
			return createImportResponse(400, e.getMessage(), null);
		} catch (Exception e) {
			logger.error("[JMDConsumption] Error importing plant requirement: {}", e.getMessage(), e);
			return createImportResponse(500, "Failed to import plant requirement: " + e.getMessage(), null);
		}
	}

	private List<CalculatedProcessDemandDTO> readPlantRequirementExcel(
			InputStream inputStream, String financialYear) throws Exception {
		List<CalculatedProcessDemandDTO> data = new ArrayList<>();
		try (Workbook workbook = new XSSFWorkbook(inputStream)) {
			Sheet sheet = workbook.getSheetAt(0);
			Row headerRow = sheet.getRow(0);
			if (headerRow == null) {
				throw new IllegalArgumentException("The Excel file is missing its header row");
			}

			Map<String, Integer> headerIndexes = new HashMap<>();
			for (Cell cell : headerRow) {
				String header = getExcelCellString(cell);
				if (!isBlank(header)) {
					headerIndexes.put(normalizeHeader(header), cell.getColumnIndex());
				}
			}

			int parentPlantColumn = findHeader(headerIndexes, "Parent Plant");
			int processPlantColumn = findHeader(headerIndexes, "Process Plant");
			int cppUtilitiesColumn = findHeader(headerIndexes, "CPP Utilities");
			int cppPlantColumn = findHeader(headerIndexes, "CPP Plant");
			int uomColumn = findHeader(headerIndexes, "UOM");
			int plantIdColumn = requireHeader(headerIndexes, "Plant Code");
			int utilityIdColumn = requireHeader(headerIndexes, "CPP Utility Ids");
			int aprColumn = requireHeader(headerIndexes, "Apr-" + financialYear.substring(2, 4));
			int mayColumn = requireHeader(headerIndexes, "May-" + financialYear.substring(2, 4));
			int junColumn = requireHeader(headerIndexes, "Jun-" + financialYear.substring(2, 4));
			int julColumn = requireHeader(headerIndexes, "Jul-" + financialYear.substring(2, 4));
			int augColumn = requireHeader(headerIndexes, "Aug-" + financialYear.substring(2, 4));
			int sepColumn = requireHeader(headerIndexes, "Sep-" + financialYear.substring(2, 4));
			int octColumn = requireHeader(headerIndexes, "Oct-" + financialYear.substring(2, 4));
			int novColumn = requireHeader(headerIndexes, "Nov-" + financialYear.substring(2, 4));
			int decColumn = requireHeader(headerIndexes, "Dec-" + financialYear.substring(2, 4));
			int janColumn = requireHeader(headerIndexes, "Jan-" + financialYear.substring(5, 7));
			int febColumn = requireHeader(headerIndexes, "Feb-" + financialYear.substring(5, 7));
			int marColumn = requireHeader(headerIndexes, "Mar-" + financialYear.substring(5, 7));
			int remarksColumn = requireHeader(headerIndexes, "Remarks");

			for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
				Row row = sheet.getRow(rowIndex);
				if (row == null || isBlankRow(row)) {
					continue;
				}

				CalculatedProcessDemandDTO dto = new CalculatedProcessDemandDTO();
				try {
					dto.setProcessPlantId(getExcelCellString(row.getCell(plantIdColumn)));
					dto.setCppUtilityId(getExcelCellString(row.getCell(utilityIdColumn)));
					dto.setParentPlantName(getExcelCellString(getCell(row, parentPlantColumn)));
					dto.setProcessPlant(getExcelCellString(getCell(row, processPlantColumn)));
					dto.setCppUtility(getExcelCellString(getCell(row, cppUtilitiesColumn)));
					dto.setCppPlant(getExcelCellString(getCell(row, cppPlantColumn)));
					dto.setUom(getExcelCellString(getCell(row, uomColumn)));
					dto.setApr(getExcelCellDouble(row.getCell(aprColumn)));
					dto.setMay(getExcelCellDouble(row.getCell(mayColumn)));
					dto.setJun(getExcelCellDouble(row.getCell(junColumn)));
					dto.setJul(getExcelCellDouble(row.getCell(julColumn)));
					dto.setAug(getExcelCellDouble(row.getCell(augColumn)));
					dto.setSep(getExcelCellDouble(row.getCell(sepColumn)));
					dto.setOct(getExcelCellDouble(row.getCell(octColumn)));
					dto.setNov(getExcelCellDouble(row.getCell(novColumn)));
					dto.setDec(getExcelCellDouble(row.getCell(decColumn)));
					dto.setJan(getExcelCellDouble(row.getCell(janColumn)));
					dto.setFeb(getExcelCellDouble(row.getCell(febColumn)));
					dto.setMar(getExcelCellDouble(row.getCell(marColumn)));
					dto.setRemarks(getExcelCellString(row.getCell(remarksColumn)));
				} catch (RuntimeException e) {
					dto.setSaveStatus("Failed");
					dto.setErrDescription("Invalid Excel row " + (rowIndex + 1) + ": " + e.getMessage());
				}
				data.add(dto);
			}
		}
		return data;
	}

	@Override
	public AOPMessageVM importJmdExcel(UUID plantId, String financialYear, MultipartFile file) {
		List<UUID> plantIds = new ArrayList<>();
		plantIds.add(plantId);
		return importJmdExcel(plantIds, financialYear, file);
	}

	@Override
	public AOPMessageVM importJmdExcelForMultiplePlants(List<UUID> plantIds, String financialYear, MultipartFile file) {
		return importJmdExcel(plantIds, financialYear, file);
	}

	private AOPMessageVM importJmdExcel(List<UUID> plantIds, String financialYear, MultipartFile file) {
		try {
			List<CalculatedProcessDemandDTO> importedRows;
			try (InputStream inputStream = file.getInputStream()) {
				importedRows = readJmdConsumption(inputStream, financialYear);
			}
			if (importedRows.isEmpty()) {
				return createImportResponse(400, "The Excel file contains no data rows", null);
			}

			Map<String, CalculatedProcessDemandDTO> existingByKey = new HashMap<>();
			for (UUID plantId : plantIds) {
				List<CalculatedProcessDemandDTO> existingRows = consumptionService.getProcessDemandByPlant(plantId,
						financialYear);
				for (CalculatedProcessDemandDTO existing : existingRows) {
					existingByKey.put(consumptionKey(existing.getProcessPlantId(), existing.getCppUtilityId()),
							existing);
				}
			}

			List<CalculatedProcessDemandDTO> failedRows = new ArrayList<>();
			List<CalculatedProcessDemandDTO> validRows = new ArrayList<>();
			List<ProcessDemandUpdateRequest> requests = new ArrayList<>();
			int skippedCount = 0;
			for (CalculatedProcessDemandDTO dto : importedRows) {
				if (dto.getSaveStatus() != null) {
					failedRows.add(dto);
				} else if (isBlank(dto.getProcessPlantId())) {
					dto.setSaveStatus("Failed");
					dto.setErrDescription("Receiver Plant Id is missing");
					failedRows.add(dto);
				} else if (isBlank(dto.getCppUtilityId())) {
					dto.setSaveStatus("Failed");
					dto.setErrDescription("Sender Utility Id is missing");
					failedRows.add(dto);
				} else {
					CalculatedProcessDemandDTO existing = existingByKey.get(
							consumptionKey(dto.getProcessPlantId(), dto.getCppUtilityId()));
					if (existing != null && !hasConsumptionValuesChanged(dto, existing)) {
						skippedCount++;
						continue;
					}
					if (isBlank(dto.getRemarks())) {
						dto.setSaveStatus("Failed");
						dto.setErrDescription("Remarks field is mandatory and cannot be empty");
						failedRows.add(dto);
						continue;
					}
					if (existing != null && existing.getRemarks() != null
							&& existing.getRemarks().trim().equals(dto.getRemarks().trim())) {
						dto.setSaveStatus("Failed");
						dto.setErrDescription(
								"Remarks must be updated to explain the changes. Current remarks are identical to the database value.");
						failedRows.add(dto);
						continue;
					}
					validRows.add(dto);
					requests.add(ProcessDemandUpdateRequest.builder()
							.processPlantId(dto.getProcessPlantId())
							.cppUtilityId(dto.getCppUtilityId())
							.apr(dto.getApr())
							.may(dto.getMay())
							.jun(dto.getJun())
							.jul(dto.getJul())
							.aug(dto.getAug())
							.sep(dto.getSep())
							.oct(dto.getOct())
							.nov(dto.getNov())
							.dec(dto.getDec())
							.jan(dto.getJan())
							.feb(dto.getFeb())
							.mar(dto.getMar())
							.remarks(dto.getRemarks())
							.build());
				}
			}

			logger.info(
					"[JMDConsumption] Import rows: {}, unchanged rows skipped: {}, rows to update: {}, rows failed validation: {}",
					importedRows.size(), skippedCount, validRows.size(), failedRows.size());

			if (!requests.isEmpty()) {
				ProcessDemandUpdateResponse updateResponse = consumptionService.updateProcessDemand(financialYear,
						requests);
				List<String> updateErrors = updateResponse.getErrors() == null
						? new ArrayList<>()
						: updateResponse.getErrors();
				boolean hasUnmatchedError = false;
				for (String error : updateErrors) {
					CalculatedProcessDemandDTO failedRow = findRowForUpdateError(validRows, error);
					if (failedRow != null && failedRow.getSaveStatus() == null) {
						failedRow.setSaveStatus("Failed");
						failedRow.setErrDescription(error);
						failedRows.add(failedRow);
					} else {
						hasUnmatchedError = true;
						logger.error("[JMDConsumption] Could not associate update error with an imported row: {}",
								error);
					}
				}

				if (hasUnmatchedError || updateResponse.getFailureCount() > updateErrors.size()) {
					for (CalculatedProcessDemandDTO dto : validRows) {
						if (dto.getSaveStatus() == null) {
							dto.setSaveStatus("Failed");
							dto.setErrDescription("The process demand update failed; see server logs for details");
							failedRows.add(dto);
						}
					}
				}
			}

			if (!failedRows.isEmpty()) {
				byte[] failedWorkbook = exportConsumptionForMultiplePlants(plantIds, financialYear, true, failedRows);
				return createImportResponse(400, "Some JMD consumption rows failed to import",
						Base64.getEncoder().encodeToString(failedWorkbook));
			}
			return createImportResponse(200, "All JMD consumption rows were imported successfully. "
					+ skippedCount + " unchanged rows skipped, " + validRows.size() + " rows updated.", null);
		} catch (Exception e) {
			logger.error("[JMDConsumption] Error importing JMD consumption: {}", e.getMessage(), e);
			return createImportResponse(500, "Failed to import JMD consumption: " + e.getMessage(), null);
		}
	}

	private String consumptionKey(String processPlantId, String cppUtilityId) {
		return processPlantId + ":" + cppUtilityId;
	}

	private boolean hasConsumptionValuesChanged(
			CalculatedProcessDemandDTO imported, CalculatedProcessDemandDTO existing) {
		return !sameMonthValue(imported.getApr(), existing.getApr())
				|| !sameMonthValue(imported.getMay(), existing.getMay())
				|| !sameMonthValue(imported.getJun(), existing.getJun())
				|| !sameMonthValue(imported.getJul(), existing.getJul())
				|| !sameMonthValue(imported.getAug(), existing.getAug())
				|| !sameMonthValue(imported.getSep(), existing.getSep())
				|| !sameMonthValue(imported.getOct(), existing.getOct())
				|| !sameMonthValue(imported.getNov(), existing.getNov())
				|| !sameMonthValue(imported.getDec(), existing.getDec())
				|| !sameMonthValue(imported.getJan(), existing.getJan())
				|| !sameMonthValue(imported.getFeb(), existing.getFeb())
				|| !sameMonthValue(imported.getMar(), existing.getMar());
	}

	private boolean sameMonthValue(Double imported, Double existing) {
		double importedValue = imported == null ? 0.0 : imported;
		double existingValue = existing == null ? 0.0 : existing;
		return Double.compare(importedValue, existingValue) == 0;
	}

	private List<CalculatedProcessDemandDTO> readJmdConsumption(InputStream inputStream, String financialYear)
			throws Exception {
		List<CalculatedProcessDemandDTO> data = new ArrayList<>();
		try (Workbook workbook = new XSSFWorkbook(inputStream)) {
			Sheet sheet = workbook.getSheetAt(0);
			Row headerRow = sheet.getRow(0);
			if (headerRow == null) {
				throw new IllegalArgumentException("The Excel file is missing its header row");
			}

			Map<String, Integer> headerIndexes = new HashMap<>();
			for (Cell cell : headerRow) {
				String header = getExcelCellString(cell);
				if (!isBlank(header)) {
					headerIndexes.put(normalizeHeader(header), cell.getColumnIndex());
				}
			}

			int plantIdColumn = requireHeader(headerIndexes, "Plant Code", "Receiver Plant Id", "Process Plant Id");
			int utilityIdColumn = requireHeader(headerIndexes, "Sender Utility Code", "Sender Utility Id",
					"CPP Utility Ids");
			int parentPlantColumn = findHeader(headerIndexes, "Parent Plant");
			// Sender (CPP) columns
			int senderUtilityColumn = findHeader(headerIndexes, "Sender Utility", "Sender Plant Utility",
					"CPP Utilities");
			int senderUomColumn = findHeader(headerIndexes, "Sender UOM", "Sender Utility UOM", "UOM");
			int senderPlantColumn = findHeader(headerIndexes, "Sender Plant", "CPP Plant");
			int senderPlantIdColumn = findHeader(headerIndexes, "Sender Plant Code", "Sender Plant Id",
					"CPP Plant Id");
			int senderCostCenterColumn = findHeader(headerIndexes, "Sender Cost Center", "CPP Plant Cost Center");
			int senderCostCenterCodeColumn = findHeader(headerIndexes, "Sender Cost Center Code",
					"CPP Plant Cost Center Code");
			// Receiver (Process Plant) columns
			int receiverUtilityColumn = findHeader(headerIndexes, "Receiver Utility", "Receiver Plant Utility",
					"Process Plant Utility");
			int receiverUtilityCodeColumn = findHeader(headerIndexes, "Receiver Utility Code", "Receiver Utility Id",
					"Process Plant Utility Id");
			int receiverUomColumn = findHeader(headerIndexes, "Receiver UOM", "Receiver Utility UOM",
					"Process Plant UOM");
			int receiverPlantColumn = findHeader(headerIndexes, "Receiver Plant", "Process Plant");
			int receiverCostCenterColumn = findHeader(headerIndexes, "Receiver Cost Center",
					"Process Plant Cost Center");
			int receiverCostCenterCodeColumn = findHeader(headerIndexes, "Receiver Cost Center Code",
					"Process Plant Cost Center Code");
			String startYear = financialYear.substring(2, 4);
			String endYear = financialYear.substring(5, 7);
			int aprColumn = requireHeader(headerIndexes, "Apr-" + startYear, "Apr");
			int mayColumn = requireHeader(headerIndexes, "May-" + startYear, "May");
			int junColumn = requireHeader(headerIndexes, "Jun-" + startYear, "Jun");
			int julColumn = requireHeader(headerIndexes, "Jul-" + startYear, "Jul");
			int augColumn = requireHeader(headerIndexes, "Aug-" + startYear, "Aug");
			int sepColumn = requireHeader(headerIndexes, "Sep-" + startYear, "Sep");
			int octColumn = requireHeader(headerIndexes, "Oct-" + startYear, "Oct");
			int novColumn = requireHeader(headerIndexes, "Nov-" + startYear, "Nov");
			int decColumn = requireHeader(headerIndexes, "Dec-" + startYear, "Dec");
			int janColumn = requireHeader(headerIndexes, "Jan-" + endYear, "Jan");
			int febColumn = requireHeader(headerIndexes, "Feb-" + endYear, "Feb");
			int marColumn = requireHeader(headerIndexes, "Mar-" + endYear, "Mar");
			int remarksColumn = findHeader(headerIndexes, "Remarks");

			for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
				Row row = sheet.getRow(rowIndex);
				if (row == null || isBlankRow(row)) {
					continue;
				}

				CalculatedProcessDemandDTO dto = new CalculatedProcessDemandDTO();
				try {
					dto.setProcessPlantId(getExcelCellString(row.getCell(plantIdColumn)));
					dto.setParentPlantName(getExcelCellString(getCell(row, parentPlantColumn)));
					// Sender (CPP) fields
					dto.setCppUtility(getExcelCellString(getCell(row, senderUtilityColumn)));
					dto.setUom(getExcelCellString(getCell(row, senderUomColumn)));
					dto.setCppPlant(getExcelCellString(getCell(row, senderPlantColumn)));
					dto.setCppPlantId(getExcelCellString(getCell(row, senderPlantIdColumn)));
					dto.setCppPlantCostCenter(getExcelCellString(getCell(row, senderCostCenterColumn)));
					dto.setCppPlantCostCenterCode(getExcelCellString(getCell(row, senderCostCenterCodeColumn)));
					dto.setCppUtilityId(getExcelCellString(row.getCell(utilityIdColumn)));
					// Receiver (Process Plant) fields
					dto.setProcessPlantUtility(getExcelCellString(getCell(row, receiverUtilityColumn)));
					dto.setProcessPlantUtilityId(getExcelCellString(getCell(row, receiverUtilityCodeColumn)));
					dto.setProcessPlantUom(getExcelCellString(getCell(row, receiverUomColumn)));
					dto.setProcessPlant(getExcelCellString(getCell(row, receiverPlantColumn)));
					dto.setProcessPlantCostCenter(getExcelCellString(getCell(row, receiverCostCenterColumn)));
					dto.setProcessPlantCostCenterCode(getExcelCellString(getCell(row, receiverCostCenterCodeColumn)));
					dto.setApr(getExcelCellDouble(row.getCell(aprColumn)));
					dto.setMay(getExcelCellDouble(row.getCell(mayColumn)));
					dto.setJun(getExcelCellDouble(row.getCell(junColumn)));
					dto.setJul(getExcelCellDouble(row.getCell(julColumn)));
					dto.setAug(getExcelCellDouble(row.getCell(augColumn)));
					dto.setSep(getExcelCellDouble(row.getCell(sepColumn)));
					dto.setOct(getExcelCellDouble(row.getCell(octColumn)));
					dto.setNov(getExcelCellDouble(row.getCell(novColumn)));
					dto.setDec(getExcelCellDouble(row.getCell(decColumn)));
					dto.setJan(getExcelCellDouble(row.getCell(janColumn)));
					dto.setFeb(getExcelCellDouble(row.getCell(febColumn)));
					dto.setMar(getExcelCellDouble(row.getCell(marColumn)));
					dto.setRemarks(remarksColumn < 0 ? null : getExcelCellString(row.getCell(remarksColumn)));
				} catch (RuntimeException e) {
					dto.setSaveStatus("Failed");
					dto.setErrDescription("Invalid Excel row " + (rowIndex + 1) + ": " + e.getMessage());
				}
				data.add(dto);
			}
		}
		return data;
	}

	private CalculatedProcessDemandDTO findRowForUpdateError(List<CalculatedProcessDemandDTO> rows, String error) {
		if (error == null) {
			return null;
		}
		for (CalculatedProcessDemandDTO row : rows) {
			if (row.getSaveStatus() == null
					&& error.contains("processPlantId=" + row.getProcessPlantId())
					&& error.contains("cppUtilityId=" + row.getCppUtilityId())) {
				return row;
			}
		}
		return null;
	}

	private int requireHeader(Map<String, Integer> headerIndexes, String... names) {
		int column = findHeader(headerIndexes, names);
		if (column < 0) {
			throw new IllegalArgumentException("Excel header is missing: " + names[0]);
		}
		return column;
	}

	private int findHeader(Map<String, Integer> headerIndexes, String... names) {
		for (String name : names) {
			Integer column = headerIndexes.get(normalizeHeader(name));
			if (column != null) {
				return column;
			}
		}
		return -1;
	}

	private Cell getCell(Row row, int columnIndex) {
		return columnIndex < 0 ? null : row.getCell(columnIndex);
	}

	private String normalizeHeader(String header) {
		return header.trim().toLowerCase(Locale.ROOT);
	}

	private String getExcelCellString(Cell cell) {
		if (cell == null) {
			return null;
		}
		CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
		if (type == CellType.STRING) {
			String value = cell.getStringCellValue();
			return isBlank(value) ? null : value.trim();
		}
		if (type == CellType.NUMERIC) {
			return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
		}
		return null;
	}

	private Double getExcelCellDouble(Cell cell) {
		String value = getExcelCellString(cell);
		if (value == null) {
			return null;
		}
		try {
			return Double.valueOf(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Expected a numeric month value but found '" + value + "'");
		}
	}

	private boolean isBlankRow(Row row) {
		for (Cell cell : row) {
			if (!isBlank(getExcelCellString(cell))) {
				return false;
			}
		}
		return true;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private AOPMessageVM createImportResponse(int code, String message, Object data) {
		AOPMessageVM response = new AOPMessageVM();
		response.setCode(code);
		response.setMessage(message);
		response.setData(data);
		return response;
	}

	@Override
	public AOPMessageVM importExcel(UUID plantId, String financialYear, MultipartFile file) {
		logger.info("[JMDConsumption] Importing consumption for plantId: {}, financialYear: {}", plantId,
				financialYear);
		try {
			AOPMessageVM result = consumptionService.importExcel(plantId, financialYear, file);
			logger.info("[JMDConsumption] Consumption imported successfully");
			return result;
		} catch (Exception e) {
			logger.error("[JMDConsumption] Error importing consumption: {}", e.getMessage(), e);
			AOPMessageVM errorResponse = new AOPMessageVM();
			errorResponse.setCode(500);
			errorResponse.setMessage("Failed to import consumption: " + e.getMessage());
			errorResponse.setData(null);
			return errorResponse;
		}
	}

	@Override
	public AOPMessageVM importExcelForMultiplePlants(List<UUID> plantIds, String financialYear, MultipartFile file) {
		logger.info("[JMDConsumption] Importing consumption for plantIds: {}, financialYear: {}", plantIds,
				financialYear);
		try {
			// Import for first plant (file contains data for all plants)
			AOPMessageVM result = consumptionService.importExcel(plantIds.get(0), financialYear, file);
			logger.info("[JMDConsumption] Consumption imported successfully for multiple plants");
			return result;
		} catch (Exception e) {
			logger.error("[JMDConsumption] Error importing consumption for multiple plants: {}", e.getMessage(), e);
			AOPMessageVM errorResponse = new AOPMessageVM();
			errorResponse.setCode(500);
			errorResponse.setMessage("Failed to import consumption: " + e.getMessage());
			errorResponse.setData(null);
			return errorResponse;
		}
	}
}
