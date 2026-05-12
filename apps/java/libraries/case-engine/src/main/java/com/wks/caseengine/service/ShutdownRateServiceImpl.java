package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.ConfigurationDTO;
import com.wks.caseengine.dto.ShutdownRateDropdownDTO;
import com.wks.caseengine.entity.NormAttributeTransactions;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.NormAttributeTransactionsRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class ShutdownRateServiceImpl implements ShutdownRateService {

	@PersistenceContext
	private EntityManager entityManager;
	
	@Autowired
	private DataSource dataSource;
	
	@Autowired
	private PlantsRepository plantsRepository;
	
	@Autowired
	private VerticalsRepository verticalRepository;
	
	@Autowired
	private SiteRepository siteRepository;

	@Autowired
	private ConfigurationService configurationService;

	@Autowired
	private NormAttributeTransactionsRepository normAttributeTransactionsRepository;

	@Override
	@Transactional
	public AOPMessageVM getShutdownRate(String plantId, String aopYear) {
		AOPMessageVM response = new AOPMessageVM();
		
		try {
			// Get data and columns dynamically
			List<Map<String, Object>> dataList = getShutdownRateData(plantId, aopYear);
			List<Map<String, Object>> columnMetadata = getShutdownRateColumnMetadata(plantId, aopYear);
			
			Map<String, Object> finalData = new HashMap<>();
			finalData.put("data", dataList);
			finalData.put("columns", columnMetadata);
			
			response.setCode(200);
			response.setMessage("Data fetched successfully");
			response.setData(finalData);
			
		} catch (Exception e) {
			response.setCode(200);
			response.setMessage("Failed to retrieve shutdown rate data: " + e.getMessage());
			e.printStackTrace();
		}
		
		return response;
	}
	
	private List<Map<String, Object>> getShutdownRateData(String plantId, String aopYear) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> dataList = new ArrayList<>();
			String verticalName = plantsRepository.findVerticalNameByPlantId(UUID.fromString(plantId));
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
	                .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			String spName = verticalName + "_" + site.getName() + "_GetShutdownRate";
	        
	        String sql = "{call [dbo].[" + spName + "](?, ?)}";
			
			try (CallableStatement callableStatement = connection.prepareCall(sql)) {
				callableStatement.setString(1, plantId);
				callableStatement.setString(2, aopYear);
				
				boolean hasResultSet = callableStatement.execute();
				
				if (hasResultSet) {
					try (ResultSet resultSet = callableStatement.getResultSet()) {
						ResultSetMetaData metaData = resultSet.getMetaData();
						int columnCount = metaData.getColumnCount();
						
						while (resultSet.next()) {
							Map<String, Object> row = new LinkedHashMap<>();
							for (int i = 1; i <= columnCount; i++) {
								Object value = resultSet.getObject(i);
								row.put(metaData.getColumnLabel(i), value != null ? value : "");
							}
							dataList.add(row);
						}
					}
				}
			}
			
			return dataList;
		});
	}
	
	private List<Map<String, Object>> getShutdownRateColumnMetadata(String plantId, String aopYear) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> columnMetadata = new ArrayList<>();
			String verticalName = plantsRepository.findVerticalNameByPlantId(UUID.fromString(plantId));
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
	                .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			
			String spName = verticalName + "_" + site.getName() + "_GetShutdownRate";
	        
	        String sql = "{call [dbo].[" + spName + "](?, ?)}";
			
			try (CallableStatement callableStatement = connection.prepareCall(sql)) {
				callableStatement.setString(1, plantId);
				callableStatement.setString(2, aopYear);
				
				boolean hasResultSet = callableStatement.execute();
				
				if (hasResultSet) {
					try (ResultSet resultSet = callableStatement.getResultSet()) {
						ResultSetMetaData metaData = resultSet.getMetaData();
						
						for (int i = 1; i <= metaData.getColumnCount(); i++) {
							Map<String, Object> columnInfo = new HashMap<>();
							String columnName = metaData.getColumnLabel(i);
							String columnType = metaData.getColumnTypeName(i);
							
							columnInfo.put("field", columnName);
							columnInfo.put("title", formatTitle(columnName));
							columnInfo.put("editable", false);
							columnInfo.put("isVisible", "true");
							columnInfo.put("type", getFrontendType(columnType));
							columnMetadata.add(columnInfo);
						}
					}
				}
			}
			
			return columnMetadata;
		});
	}
	
	private String formatTitle(String columnName) {
		// Format column names to be more readable
		switch (columnName) {
			case "NormParameter_FK_Id":
				return "Norm Parameter FK Id";
			case "Major Shutdown":
				return "Major Shutdown";
			case "One Day Shutdown":
				return "One Day Shutdown";
			case "Remarks":
				return "Remarks";
			case "AuditYear":
				return "Audit Year";
			case "UOM":
				return "UOM";
			case "NormTypeName":
				return "Norm Type Name";
			case "isEditable":
				return "Is Editable";
			case "DisplayName":
				return "Display Name";
			case "Type":
				return "Type";
			default:
				return columnName.replace("_", " ");
		}
	}
	
	private String getFrontendType(String sqlTypeName) {
		if (sqlTypeName == null) {
			return "string";
		}
		
		switch (sqlTypeName.toUpperCase()) {
			case "VARCHAR":
			case "NVARCHAR":
			case "CHAR":
			case "UNIQUEIDENTIFIER":
				return "string";
			
			case "INT":
			case "TINYINT":
			case "BIGINT":
			case "SMALLINT":
			case "DECIMAL":
			case "FLOAT":
			case "DOUBLE":
			case "NUMERIC":
			case "REAL":
				return "number";
			
			case "DATE":
			case "DATETIME":
			case "DATETIME2":
			case "SMALLDATETIME":
			case "TIME":
				return "date";
			
			case "BIT":
				return "boolean";
			
			default:
				return "string";
		}
	}
	
	@Override
	public List<ShutdownRateDropdownDTO> getShutdownRateDropdown(String plantId) {
		List<ShutdownRateDropdownDTO> dropdownList = new ArrayList<>();
		
		try {
			String verticalName = plantsRepository.findVerticalNameByPlantId(UUID.fromString(plantId));
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
	                .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			String viewName="vwScrn"+verticalName+"ShutdownRateDropdown";
			String sql = "SELECT Name, DisplayName, DisplayOrder FROM [dbo].[" + viewName + "] ORDER BY DisplayOrder";
			List<Object[]> obj = entityManager.createNativeQuery(sql).getResultList();
			
			for (Object[] row : obj) {
				ShutdownRateDropdownDTO dropdown = new ShutdownRateDropdownDTO();
				
				dropdown.setName(row[0] != null ? row[0].toString() : null);
				dropdown.setDisplayName(row[1] != null ? row[1].toString() : null);
				dropdown.setDisplayOrder(row[2] != null ? Integer.parseInt(row[2].toString()) : null);
				
				dropdownList.add(dropdown);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return dropdownList;
	}

	@Override
	public byte[] exportShutdownRate(String plantId, String aopYear) {
		try {
			AOPMessageVM response = getShutdownRate(plantId, aopYear);
			@SuppressWarnings("unchecked")
			Map<String, Object> dataMap = (Map<String, Object>) response.getData();
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataMap.get("data");

			Workbook workbook = new XSSFWorkbook();
			Sheet sheet = workbook.createSheet("Shutdown Rate");

			// Header style: bold font only, no background color
			CellStyle headerStyle = workbook.createCellStyle();
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerStyle.setFont(headerFont);
			headerStyle.setBorderBottom(BorderStyle.THIN);
			headerStyle.setBorderTop(BorderStyle.THIN);
			headerStyle.setBorderLeft(BorderStyle.THIN);
			headerStyle.setBorderRight(BorderStyle.THIN);

			// Data cell style
			CellStyle dataStyle = workbook.createCellStyle();
			dataStyle.setBorderBottom(BorderStyle.THIN);
			dataStyle.setBorderTop(BorderStyle.THIN);
			dataStyle.setBorderLeft(BorderStyle.THIN);
			dataStyle.setBorderRight(BorderStyle.THIN);

			// Header row: Particular, UOM, Major Shutdown, One Day Shutdown, Remarks, NormParameter_FK_Id (hidden)
			String[] headers = { "Particular", "UOM", "Major Shutdown", "One Day Shutdown", "Remarks",
					"NormParameter_FK_Id" };
			Row headerRow = sheet.createRow(0);
			for (int i = 0; i < headers.length; i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(headerStyle);
			}

			// Data rows
			int rowIdx = 1;
			for (Map<String, Object> dataRow : dataList) {
				Row row = sheet.createRow(rowIdx++);
				setExcelCellValue(row.createCell(0), dataRow.get("DisplayName"), dataStyle);
				setExcelCellValue(row.createCell(1), dataRow.get("UOM"), dataStyle);
				setExcelCellValue(row.createCell(2), dataRow.get("MajorShutdown"), dataStyle);
				setExcelCellValue(row.createCell(3), dataRow.get("OneDayShutdown"), dataStyle);
				setExcelCellValue(row.createCell(4), dataRow.get("remarks"), dataStyle);
				setExcelCellValue(row.createCell(5), dataRow.get("NormParameter_FK_Id"), dataStyle);
			}

			// Strictly hide NormParameter_FK_Id: mark as hidden AND set width to 0
			// so it cannot be revealed by dragging column borders
			sheet.setColumnHidden(5, true);
			sheet.setColumnWidth(5, 0);

			// Auto-size visible columns
			for (int i = 0; i < 5; i++) {
				sheet.autoSizeColumn(i);
			}

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			workbook.write(outputStream);
			workbook.close();
			return outputStream.toByteArray();

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to export Shutdown Rate Excel", e);
		}
	}

	@Override
	@Transactional
	public AOPMessageVM importShutdownRate(String plantId, String aopYear, String version, MultipartFile file,
			Boolean calculation) {
		if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
			throw new IllegalArgumentException("Invalid or empty Excel file.");
		}

		try {
			List<ConfigurationDTO> configListToSave = new ArrayList<>();
			List<ConfigurationDTO> remarkFailedRows = new ArrayList<>();

			try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
				Sheet sheet = workbook.getSheetAt(0);
				Iterator<Row> rowIterator = sheet.iterator();

				if (rowIterator.hasNext()) {
					rowIterator.next(); // Skip header row
				}

				while (rowIterator.hasNext()) {
					Row row = rowIterator.next();

					// Skip completely empty rows
					if (isRowEmpty(row)) {
						continue;
					}

					ConfigurationDTO dto = new ConfigurationDTO();
					try {
						// Col 0: Particular (DisplayName) — display only, stored for error reporting
						dto.setProductName(readStringCell(row.getCell(0), dto));
						// Col 1: UOM
						dto.setUOM(readStringCell(row.getCell(1), dto));
						// Col 2: Major Shutdown ? AopMonth = 1 (January slot)
						Double majorShutdown = readNumericCell(row.getCell(2), dto);
						dto.setApr(majorShutdown);
						// Col 3: One Day Shutdown ? AopMonth = 2 (February slot)
						Double oneDayShutdown = readNumericCell(row.getCell(3), dto);
						dto.setMay(oneDayShutdown);
						// Col 4: Remarks
						String newRemark = readStringCell(row.getCell(4), dto);
						dto.setRemarks(newRemark);
						// Col 5: NormParameter_FK_Id (hidden)
						String normParamFKId = readStringCell(row.getCell(5), dto);
						dto.setNormParameterFKId(normParamFKId);
						dto.setAuditYear(aopYear);

						// Remarks validation: if incoming remark matches the existing remark, skip and flag
						if (dto.getSaveStatus() == null && normParamFKId != null && !normParamFKId.isBlank()) {
							String existingMajorShutdownValue = fetchExistingRecord(normParamFKId, aopYear, 4).getAttributeValue(); // major shutdown value
							String existingOneDayShutdownValue = fetchExistingRecord(normParamFKId, aopYear, 5).getAttributeValue(); // one day shutdown value
							String incomingRemark =  newRemark;
							String existingRemark = fetchExistingRecord(normParamFKId, aopYear, 4).getRemarks();

							boolean hasMajorShutdownValueChanged = hasValueChanged(existingMajorShutdownValue, dto.getApr());
							boolean hasOneDayShutdownValueChanged = hasValueChanged(existingOneDayShutdownValue, dto.getMay());
             
							boolean hasRemarkChanged = !incomingRemark.equalsIgnoreCase(existingRemark);

							if((hasMajorShutdownValueChanged || hasOneDayShutdownValueChanged) && !hasRemarkChanged) { 

								dto.setSaveStatus("Failed");
								dto.setErrDescription("Value has changed; please provide a updated remark.");
								remarkFailedRows.add(dto);
								continue;
							}

						}

					} catch (Exception e) {
						e.printStackTrace();
						dto.setErrDescription(e.getMessage());
						dto.setSaveStatus("Failed");
					}
					configListToSave.add(dto);
				}
			}

			List<ConfigurationDTO> saveFailedRecords = configurationService.saveConfigurationData(aopYear, plantId,
					version, configListToSave, calculation,false);

			// Combine remark-validation failures with save failures
			List<ConfigurationDTO> allFailed = new ArrayList<>();
			allFailed.addAll(remarkFailedRows);
			if (saveFailedRecords != null) {
				allFailed.addAll(saveFailedRecords);
			}

			AOPMessageVM aopMessageVM = new AOPMessageVM();
			if (!allFailed.isEmpty()) {
				byte[] failedExcel = buildFailedExcel(allFailed);
				String base64File = Base64.getEncoder().encodeToString(failedExcel);
				aopMessageVM.setCode(400);
				aopMessageVM.setMessage("Partial data has been saved");
				aopMessageVM.setData(base64File);
			} else {
				aopMessageVM.setCode(200);
				aopMessageVM.setMessage("All data has been saved");
			}
			return aopMessageVM;

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to import Shutdown Rate Excel", e);
		}
	}

	public boolean hasValueChanged(String majorShutdownValue, double apr) {  

// 		String majorShutdownValue = "12.1234567891";
// Double apr = 12.1234567891001;

BigDecimal value1 = new BigDecimal(majorShutdownValue).setScale(6, RoundingMode.HALF_UP);
BigDecimal value2 = BigDecimal.valueOf(apr).setScale(6, RoundingMode.HALF_UP);

return !(value1.compareTo(value2) == 0);

	}

	/**
	 * Fetches the existing remark for a NormParameter from NormAttributeTransactions
	 * using AopMonth=1 (the MajorShutdown slot) as the reference record.
	 */
	private NormAttributeTransactions fetchExistingRecord(String normParamFKId, String aopYear, Integer month) {
		try {
			UUID normParamId = UUID.fromString(normParamFKId);
			Optional<NormAttributeTransactions> existing =
					normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(normParamId, month, aopYear);
			if (existing.isPresent() && existing.get().getRemarks() != null) {
				return existing.get();
			}
			 return null;
		} catch (Exception e) {
			// If lookup fails (e.g. invalid UUID), proceed without blocking save
		}
	return null;
	}

	/**
	 * Builds an error report Excel for failed/skipped rows using the same
	 * template structure as the export (Particular, UOM, Major Shutdown,
	 * One Day Shutdown, Remarks, NormParameter_FK_Id hidden).
	 */
	private byte[] buildFailedExcel(List<ConfigurationDTO> failedRows) {
		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Shutdown Rate");

			CellStyle headerStyle = workbook.createCellStyle();
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerStyle.setFont(headerFont);
			headerStyle.setBorderBottom(BorderStyle.THIN);
			headerStyle.setBorderTop(BorderStyle.THIN);
			headerStyle.setBorderLeft(BorderStyle.THIN);
			headerStyle.setBorderRight(BorderStyle.THIN);

			CellStyle dataStyle = workbook.createCellStyle();
			dataStyle.setBorderBottom(BorderStyle.THIN);
			dataStyle.setBorderTop(BorderStyle.THIN);
			dataStyle.setBorderLeft(BorderStyle.THIN);
			dataStyle.setBorderRight(BorderStyle.THIN);

		String[] headers = { "Particular", "UOM", "Major Shutdown", "One Day Shutdown", "Remarks",
				"NormParameter_FK_Id", "Status", "Error Description" };
			Row headerRow = sheet.createRow(0);
			for (int i = 0; i < headers.length; i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(headerStyle);
			}
			int rowIdx = 1;
			for (ConfigurationDTO dto : failedRows) {
				Row row = sheet.createRow(rowIdx++);
				setExcelCellValue(row.createCell(0), dto.getProductName(), dataStyle);
				setExcelCellValue(row.createCell(1), dto.getUOM(), dataStyle);
				setExcelCellValue(row.createCell(2), dto.getApr(), dataStyle);
				setExcelCellValue(row.createCell(3), dto.getMay(), dataStyle);
				setExcelCellValue(row.createCell(4), dto.getRemarks(), dataStyle);
				setExcelCellValue(row.createCell(5), dto.getNormParameterFKId(), dataStyle);
				setExcelCellValue(row.createCell(6), dto.getSaveStatus(), dataStyle);
				setExcelCellValue(row.createCell(7), dto.getErrDescription(), dataStyle);
			}

			sheet.setColumnHidden(5, true);
			sheet.setColumnWidth(5, 0);
			for (int i = 0; i < headers.length; i++) {
				if (i != 5) {
					sheet.autoSizeColumn(i);
				}
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			workbook.write(out);
			return out.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to build error report Excel", e);
		}
	}

	/**
	 * Returns true if a row has no physical cells or all cells are blank/empty.
	 */
	private boolean isRowEmpty(Row row) {
		if (row == null || row.getPhysicalNumberOfCells() == 0) {
			return true;
		}
		for (int i = row.getFirstCellNum(); i <= row.getLastCellNum(); i++) {
			Cell cell = row.getCell(i);
			if (cell != null && cell.getCellType() != CellType.BLANK) {
				String val = cell.toString().trim();
				if (!val.isEmpty()) {
					return false;
				}
			}
		}
		return true;
	}

	private void setExcelCellValue(Cell cell, Object value, CellStyle style) {
		cell.setCellStyle(style);
		if (value == null) {
			cell.setCellValue("");
		} else if (value instanceof Number) {
			cell.setCellValue(((Number) value).doubleValue());
		} else if (value instanceof Boolean) {
			cell.setCellValue((Boolean) value);
		} else {
			String strVal = value.toString();
			try {
				cell.setCellValue(Double.parseDouble(strVal));
			} catch (NumberFormatException e) {
				cell.setCellValue(strVal);
			}
		}
	}

	private static String readStringCell(Cell cell, ConfigurationDTO dto) {
		try {
			if (cell == null) return null;
			org.apache.poi.ss.usermodel.DataFormatter formatter = new org.apache.poi.ss.usermodel.DataFormatter();
			return formatter.formatCellValue(cell).trim();
		} catch (Exception e) {
			dto.setSaveStatus("Failed");
			dto.setErrDescription("Please enter correct values");
			e.printStackTrace();
		}
		return null;
	}

	private static Double readNumericCell(Cell cell, ConfigurationDTO dto) {
		if (cell == null || cell.toString().equalsIgnoreCase("")) return null;
		if (cell.getCellType() == CellType.NUMERIC) {
			return cell.getNumericCellValue();
		} else if (cell.getCellType() == CellType.STRING) {
			try {
				return Double.parseDouble(cell.getStringCellValue().trim());
			} catch (NumberFormatException e) {
				dto.setSaveStatus("Failed");
				dto.setErrDescription("Please enter numeric values");
			}
		}
		return null;
	}
}
