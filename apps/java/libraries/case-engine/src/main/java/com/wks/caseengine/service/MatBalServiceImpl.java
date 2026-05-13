package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
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

import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class MatBalServiceImpl implements MatBalService {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private PlantsRepository plantsRepository;

	@Autowired
	private SiteRepository siteRepository;

	@Autowired
	private VerticalsRepository verticalRepository;

	@Autowired
	private ConfigurationService configurationService;

	@Override
	@Transactional(readOnly = true)
	public AOPMessageVM getMatBal(String plantId, String year) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<Object[]> results = getMatBalData(plantId, year);
			List<String> columnNames = getMatBalColumns(plantId, year);

			List<Map<String, Object>> resultList = new ArrayList<>();
			for (Object[] row : results) {
				Map<String, Object> rowMap = new LinkedHashMap<>();
				for (int i = 0; i < columnNames.size(); i++) {
					rowMap.put(columnNames.get(i), i < row.length ? row[i] : null);
				}
				resultList.add(rowMap);
			}

			Map<String, Object> data = new HashMap<>();
			data.put("data", resultList);
			data.put("columns", getMatBalColumnMetadata(plantId, year));

			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("SP Executed successfully");
			aopMessageVM.setData(data);
			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch MATBAL data", ex);
		}
	}

	@Transactional(readOnly = true)
	public List<Object[]> getMatBalData(String plantId, String year) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_GetMATBAL";
			String sql = "EXEC " + storedProcedure + " @plantId = :plantId, @AopYear = :AopYear";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("plantId", plantId);
			query.setParameter("AopYear", year);
			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch MATBAL data", ex);
		}
	}

	@Transactional(readOnly = true)
	public List<String> getMatBalColumns(String plantId, String year) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<String> columnNames = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_GetMATBAL";
			String sql = "EXEC " + storedProcedure + " @plantId = ?, @AopYear = ?";

			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, year);
				try (ResultSet rs = ps.executeQuery()) {
					ResultSetMetaData rsMetaData = rs.getMetaData();
					for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {
						columnNames.add(rsMetaData.getColumnLabel(i));
					}
				}
			}
			return columnNames;
		});
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> getMatBalColumnMetadata(String plantId, String year) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> columnMetadata = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_GetMATBAL";
			String sql = "EXEC " + storedProcedure + " @plantId = ?, @AopYear = ?";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, year);
				try (ResultSet rs = ps.executeQuery()) {
					ResultSetMetaData rsMetaData = rs.getMetaData();
					for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {
						Map<String, Object> columnInfo = new HashMap<>();
						String columnName = rsMetaData.getColumnLabel(i);
						String columnType = rsMetaData.getColumnTypeName(i);

						columnInfo.put("field", columnName);
						columnInfo.put("title", formatTitle(columnName));
						columnInfo.put("editable", false);
						columnInfo.put("type", getFrontendType(columnType));
						columnMetadata.add(columnInfo);
					}
				}
			}
			return columnMetadata;
		});
	}

	private String formatTitle(String columnName) {
		return columnName.replace("_", " ");
	}

	private String getFrontendType(String sqlTypeName) {
		switch (sqlTypeName.toUpperCase()) {
		case "VARCHAR":
		case "NVARCHAR":
		case "CHAR":
		case "TEXT":
			return "string";
		case "INT":
		case "BIGINT":
		case "SMALLINT":
		case "TINYINT":
			return "number";
		case "DECIMAL":
		case "NUMERIC":
		case "FLOAT":
		case "REAL":
			return "number";
		case "DATE":
		case "DATETIME":
		case "DATETIME2":
		case "TIMESTAMP":
			return "date";
		case "BIT":
			return "boolean";
		default:
			return "string";
		}
	}

	@Override
	public byte[] exportMatBal(String year, String plantId, boolean isAfterSave, List<Map<String, Object>> dtoList) {
		try {
			if (!isAfterSave) {
				AOPMessageVM aopMessageVM = getMatBal(plantId, year);
				Map<String, Object> innerMap = (Map<String, Object>) aopMessageVM.getData();
				if (innerMap != null) {
					dtoList = (List<Map<String, Object>>) innerMap.get("data");
				}
			}

			Workbook workbook = new XSSFWorkbook();
			Sheet sheet = workbook.createSheet("Material Balance");

			int currentRow = 0;
			if (dtoList != null && !dtoList.isEmpty()) {
				Map<String, Object> firstRow = dtoList.get(0);
				Set<String> allDataKeys = firstRow.keySet();

				// Dynamic month headers based on financial year (YYYY-YY, e.g. 2026-27)
				List<String> monthDisplayHeaders = generateMonthHeaders(year);
				String[] monthAbbrevs = { "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan", "Feb",
						"Mar" };

				// Build ordered visible columns: display header  data key
				LinkedHashMap<String, String> visibleColumns = new LinkedHashMap<>();

				String particularsKey = findKey(allDataKeys, "Particulars");
				if (particularsKey != null) visibleColumns.put("Particulars", particularsKey);

				String uomKey = findKey(allDataKeys, "UOM");
				if (uomKey != null) visibleColumns.put("UOM", uomKey);

				for (int i = 0; i < monthAbbrevs.length; i++) {
					String monthKey = findKey(allDataKeys, monthAbbrevs[i]);
					if (monthKey != null) {
						visibleColumns.put(monthDisplayHeaders.get(i), monthKey);
					}
				}

				String remarkKey = findKey(allDataKeys, "Remark");
				if (remarkKey != null) visibleColumns.put("Remark", remarkKey);

				if (isAfterSave) {
					visibleColumns.put("saveStatus", "saveStatus");
					visibleColumns.put("errDescription", "errDescription");
				}

				// Remaining columns are hidden but still written (needed for re-import)
				Set<String> mappedDataKeys = new HashSet<>(visibleColumns.values());
				List<String> hiddenDataKeys = new ArrayList<>();
				for (String key : allDataKeys) {
					if (!mappedDataKeys.contains(key)) {
						hiddenDataKeys.add(key);
					}
				}

				List<String> allDisplayHeaders = new ArrayList<>(visibleColumns.keySet());
				List<String> allDataKeyOrder = new ArrayList<>(visibleColumns.values());
				int hiddenStartCol = allDisplayHeaders.size();

				for (String hiddenKey : hiddenDataKeys) {
					allDisplayHeaders.add(hiddenKey);
					allDataKeyOrder.add(hiddenKey);
				}

				Row headerRow = sheet.createRow(currentRow++);
				for (int col = 0; col < allDisplayHeaders.size(); col++) {
					Cell cell = headerRow.createCell(col);
					cell.setCellValue(allDisplayHeaders.get(col));
					cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
				}

				for (Map<String, Object> rowMap : dtoList) {
					Row row = sheet.createRow(currentRow++);
					for (int col = 0; col < allDataKeyOrder.size(); col++) {
						Cell cell = row.createCell(col);
						Object value = rowMap.get(allDataKeyOrder.get(col));
						if (value instanceof Number) {
							cell.setCellValue(((Number) value).doubleValue());
						} else if (value instanceof Boolean) {
							cell.setCellValue((Boolean) value);
						} else if (value != null) {
							cell.setCellValue(value.toString());
						} else {
							cell.setCellValue("");
						}
					}
				}

				for (int col = hiddenStartCol; col < allDisplayHeaders.size(); col++) {
					sheet.setColumnHidden(col, true);
				}
			}

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			workbook.write(outputStream);
			workbook.close();
			return outputStream.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private List<String> generateMonthHeaders(String year) {
		// year format: YYYY-YY (e.g. 2026-27)
		String[] parts = year.split("-");
		String startYY = parts[0].substring(2); // e.g. "26"
		String endYY = parts[1];               // e.g. "27"
		List<String> months = new ArrayList<>();
		months.add("Apr-" + startYY);
		months.add("May-" + startYY);
		months.add("Jun-" + startYY);
		months.add("Jul-" + startYY);
		months.add("Aug-" + startYY);
		months.add("Sep-" + startYY);
		months.add("Oct-" + startYY);
		months.add("Nov-" + startYY);
		months.add("Dec-" + startYY);
		months.add("Jan-" + endYY);
		months.add("Feb-" + endYY);
		months.add("Mar-" + endYY);
		return months;
	}

	private String findKey(Set<String> keys, String target) {
		for (String key : keys) {
			if (key.equalsIgnoreCase(target)) {
				return key;
			}
		}
		// For "Remark", also match "Remarks"
		if (target.equalsIgnoreCase("Remark")) {
			for (String key : keys) {
				if (key.toLowerCase().startsWith("remark")) {
					return key;
				}
			}
		}
		return null;
	}

	@Override
	public AOPMessageVM importMatBal(String year, UUID plantId, MultipartFile file) {
		try {
			List<ConfigurationDTO> data = readMatBal(file.getInputStream(), plantId, year);
			List<ConfigurationDTO> result = configurationService.saveConfigurationData(year, plantId.toString(), null, data, false,false);
			
			List<ConfigurationDTO> failedList = new ArrayList<>();
			for(ConfigurationDTO dto : result) {
				if(dto.getSaveStatus() != null && dto.getSaveStatus().equalsIgnoreCase("Failed")) {
					failedList.add(dto);
				}
			}

			AOPMessageVM aopMessageVM = new AOPMessageVM();
			if (failedList.size() > 0) {
				// Convert failedList to List<Map<String, Object>> for exportMatBal
				List<Map<String, Object>> failedMapList = new ArrayList<>();
				for(ConfigurationDTO dto : result) { // Include all to show status
					Map<String, Object> map = new HashMap<>();
					map.put("normParameterFKId", dto.getNormParameterFKId());
					map.put("Jan", dto.getJan());
					map.put("Feb", dto.getFeb());
					map.put("Mar", dto.getMar());
					map.put("Apr", dto.getApr());
					map.put("May", dto.getMay());
					map.put("Jun", dto.getJun());
					map.put("Jul", dto.getJul());
					map.put("Aug", dto.getAug());
					map.put("Sep", dto.getSep());
					map.put("Oct", dto.getOct());
					map.put("Nov", dto.getNov());
					map.put("Dec", dto.getDec());
					map.put("Remarks", dto.getRemarks());
					map.put("saveStatus", dto.getSaveStatus());
					map.put("errDescription", dto.getErrDescription());
					failedMapList.add(map);
				}
				
				byte[] fileByteArray = exportMatBal(year, plantId.toString(), true, failedMapList);
				String base64File = Base64.getEncoder().encodeToString(fileByteArray);
				aopMessageVM.setData(base64File);
				aopMessageVM.setCode(400);
				aopMessageVM.setMessage("Partial data has been saved");
			} else {
				aopMessageVM.setCode(200);
				aopMessageVM.setMessage("All data has been saved");
			}
			return aopMessageVM;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to import MatBal data", e);
		}
	}

	public List<ConfigurationDTO> readMatBal(InputStream inputStream, UUID plantId, String year) {
		List<ConfigurationDTO> dataList = new ArrayList<>();
		try (Workbook workbook = new XSSFWorkbook(inputStream)) {
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();

			if (rowIterator.hasNext()) {
				Row headerRow = rowIterator.next();
				Map<String, Integer> headerMap = new HashMap<>();
				for (Cell cell : headerRow) {
					headerMap.put(cell.getStringCellValue(), cell.getColumnIndex());
				}

				while (rowIterator.hasNext()) {
					Row row = rowIterator.next();
					ConfigurationDTO dto = new ConfigurationDTO();
					dto.setAuditYear(year);
					dto.setNormParameterFKId(getStringCellValue(row.getCell(headerMap.getOrDefault("normParameterFKId", -1))));
					if (dto.getNormParameterFKId() == null) {
						dto.setNormParameterFKId(getStringCellValue(row.getCell(headerMap.getOrDefault("NormParameterType_FK_Id", -1))));
					}
					
					dto.setJan(getNumericCellValue(row.getCell(headerMap.getOrDefault("Jan", -1))));
					dto.setFeb(getNumericCellValue(row.getCell(headerMap.getOrDefault("Feb", -1))));
					dto.setMar(getNumericCellValue(row.getCell(headerMap.getOrDefault("Mar", -1))));
					dto.setApr(getNumericCellValue(row.getCell(headerMap.getOrDefault("Apr", -1))));
					dto.setMay(getNumericCellValue(row.getCell(headerMap.getOrDefault("May", -1))));
					dto.setJun(getNumericCellValue(row.getCell(headerMap.getOrDefault("Jun", -1))));
					dto.setJul(getNumericCellValue(row.getCell(headerMap.getOrDefault("Jul", -1))));
					dto.setAug(getNumericCellValue(row.getCell(headerMap.getOrDefault("Aug", -1))));
					dto.setSep(getNumericCellValue(row.getCell(headerMap.getOrDefault("Sep", -1))));
					dto.setOct(getNumericCellValue(row.getCell(headerMap.getOrDefault("Oct", -1))));
					dto.setNov(getNumericCellValue(row.getCell(headerMap.getOrDefault("Nov", -1))));
					dto.setDec(getNumericCellValue(row.getCell(headerMap.getOrDefault("Dec", -1))));
					dto.setRemarks(getStringCellValue(row.getCell(headerMap.getOrDefault("Remarks", -1))));
					dto.setId(getStringCellValue(row.getCell(headerMap.getOrDefault("idFromApi", -1))));
					
					dataList.add(dto);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dataList;
	}

	private String getStringCellValue(Cell cell) {
		if (cell == null || cell.getCellType() == CellType.BLANK) {
			return null;
		}
		if (cell.getCellType() == CellType.STRING) {
			return cell.getStringCellValue().trim();
		}
		cell.setCellType(CellType.STRING);
		return cell.getStringCellValue().trim();
	}

	private Double getNumericCellValue(Cell cell) {
		if (cell == null || cell.getCellType() == CellType.BLANK) {
			return null;
		}
		if (cell.getCellType() == CellType.NUMERIC) {
			return cell.getNumericCellValue();
		}
		if (cell.getCellType() == CellType.STRING) {
			String val = cell.getStringCellValue().trim();
			if (val.isEmpty()) return null;
			try {
				return Double.parseDouble(val);
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	@Override
	public AOPMessageVM calculateMaterialBalance(String plantId, String year) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			
			// Create dynamic stored procedure name: {VerticalName}_{SiteName}_LoadSpyroOutput
			String procedureName = vertical.getName() + "_" + site.getName() + "_LoadMATBAL";
			
			// Call the stored procedure dynamically
			String sql = "EXEC " + procedureName + " @plantId = :plantId, @AopYear = :AopYear";
			
			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("plantId", plantId);
			query.setParameter("AopYear", year);
			
			List<Object[]> results = query.getResultList();

			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data calculated successfully");
			aopMessageVM.setData(0);
			return aopMessageVM;
			
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to calculate spyro output data", ex);
		}
	}

}

