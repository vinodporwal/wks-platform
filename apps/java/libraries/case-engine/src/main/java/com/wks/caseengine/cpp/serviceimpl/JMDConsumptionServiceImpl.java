package com.wks.caseengine.cpp.serviceimpl;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
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
 * Wraps ConsumptionService to support both single plant and multiple plants operations.
 */
@Service
public class JMDConsumptionServiceImpl implements JMDConsumptionService {

	private static final Logger logger = LoggerFactory.getLogger(JMDConsumptionServiceImpl.class);

	@Autowired
	private ConsumptionService consumptionService;

	@Override
	public List<PlantRequirementDTO> getCppConsumptions(UUID plantId, String financialYear) {
		logger.info("[JMDConsumption] Fetching CPP consumptions for plantId: {}, financialYear: {}", plantId, financialYear);
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
		logger.info("[JMDConsumption] Fetching CPP consumptions for plantIds: {}, financialYear: {}", plantIds, financialYear);
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
		logger.info("[JMDConsumption] Fetching process demand by plant for plantId: {}, financialYear: {}", plantId, financialYear);
		try {
			List<CalculatedProcessDemandDTO> result = consumptionService.getProcessDemandByPlant(plantId, financialYear);
			logger.info("[JMDConsumption] Successfully fetched process demand by plant");
			return result;
		} catch (Exception e) {
			logger.error("[JMDConsumption] Error fetching process demand by plant: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to fetch process demand by plant: " + e.getMessage(), e);
		}
	}

	@Override
	public List<CalculatedProcessDemandDTO> getProcessDemandByMultiplePlants(List<UUID> plantIds, String financialYear) {
		logger.info("[JMDConsumption] Fetching process demand by plants for plantIds: {}, financialYear: {}", plantIds, financialYear);
		try {
			List<CalculatedProcessDemandDTO> allResults = new ArrayList<>();
			for (UUID plantId : plantIds) {
				List<CalculatedProcessDemandDTO> result = consumptionService.getProcessDemandByPlant(plantId, financialYear);
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
	public ProcessDemandUpdateResponse updateProcessDemand(String financialYear, List<ProcessDemandUpdateRequest> requests) {
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
	public byte[] exportConsumption(UUID plantId, String financialYear, boolean isAfterSave, List<CalculatedProcessDemandDTO> dtoList) {
		logger.info("[JMDConsumption] Exporting consumption for plantId: {}, financialYear: {}", plantId, financialYear);
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
	public byte[] exportConsumptionForMultiplePlants(List<UUID> plantIds, String financialYear, boolean isAfterSave, List<CalculatedProcessDemandDTO> dtoList) {
		logger.info("[JMDConsumption] Exporting consumption for plantIds: {}, financialYear: {}", plantIds, financialYear);
		try {
			// Aggregate data for all plants
			List<CalculatedProcessDemandDTO> aggregatedData = new ArrayList<>();
			
			if (dtoList == null || dtoList.isEmpty()) {
				// Fetch data for each plant and aggregate
				for (UUID plantId : plantIds) {
					logger.debug("[JMDConsumption] Fetching consumption data for plantId: {}", plantId);
					List<CalculatedProcessDemandDTO> plantData = consumptionService.getProcessDemandByPlant(plantId, financialYear);
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
	private byte[] generateExcelForMultiplePlants(List<CalculatedProcessDemandDTO> dtoList, String financialYear, boolean isAfterSave) throws Exception {
		logger.debug("[JMDConsumption] Generating Excel for {} records", dtoList.size());
		
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Consumption");
		int currentRow = 0;

		CellStyle headerStyle = createHeaderStyle(workbook);
		CellStyle dataStyle = createDataStyle(workbook);
		CellStyle remarksStyle = createRemarksStyle(workbook);
		String startYearSuffix = financialYear.substring(2, 4);
		String endYearSuffix = financialYear.substring(5, 7);

		// Header row
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

		Row headerRow = sheet.createRow(currentRow++);
		for (int col = 0; col < headers.size(); col++) {
			Cell cell = headerRow.createCell(col);
			cell.setCellValue(headers.get(col));
			cell.setCellStyle(headerStyle);
		}
		
		// Hide Plant Code column (index 19)
		sheet.setColumnHidden(19, true);

		// Data rows
		for (CalculatedProcessDemandDTO dto : dtoList) {
			Row row = sheet.createRow(currentRow++);
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
			cell.setCellStyle(dataStyle); // Hidden column
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
	private void setDoubleCellValue(Cell cell, Double value, CellStyle style) {
		if (value != null) {
			cell.setCellValue(value);
		}
		cell.setCellStyle(style);
	}
	

	@Override
	public AOPMessageVM importExcel(UUID plantId, String financialYear, MultipartFile file) {
		logger.info("[JMDConsumption] Importing consumption for plantId: {}, financialYear: {}", plantId, financialYear);
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
		logger.info("[JMDConsumption] Importing consumption for plantIds: {}, financialYear: {}", plantIds, financialYear);
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
