package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
public class ProductionOptimizerServiceImpl implements ProductionOptimizerService {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private PlantsRepository plantsRepository;

	@Autowired
	private VerticalsRepository verticalRepository;

	@Autowired
	private SiteRepository siteRepository;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private ShutdownHistoryService shutdownHistoryService;

	@Override
	public AOPMessageVM getProductionOptimizer(String plantId, String aopYear, String lineFkId, String type) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<Object[]> results = getProductionOptimizerData(plantId, aopYear, lineFkId, type);
			for(Object[] row : results) { 
				for(Object obj : row) { 
					System.out.println("obj: " + obj.toString());
				}
			}
			List<String> columnNames = getProductionOptimizerColumns(plantId, aopYear, lineFkId, type);
System.out.println("columnNames: " + columnNames);
			List<Map<String, Object>> resultList = new ArrayList<>();
			for (Object[] row : results) {
				Map<String, Object> rowMap = new LinkedHashMap<>();
				for (int i = 0; i < columnNames.size(); i++) {
					rowMap.put(columnNames.get(i), row[i]);
				}
				resultList.add(rowMap);
			}

			Map<String, Object> data = new HashMap<>();
			data.put("data", resultList);
			data.put("columns", getProductionOptimizerColumnMetadata(plantId, aopYear, lineFkId, type));

			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("SP Executed successfully");
			aopMessageVM.setData(data);
			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Override
	public AOPMessageVM getCombinedProductionOptimizer(String plantId, String aopYear, String type) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<Object[]> results = getCombinedProductionOptimizerData(plantId, aopYear, type);
			List<String> columnNames = getCombinedProductionOptimizerColumns(plantId, aopYear, type);

			List<Map<String, Object>> resultList = new ArrayList<>();
			for (Object[] row : results) {
				Map<String, Object> rowMap = new LinkedHashMap<>();
				for (int i = 0; i < columnNames.size(); i++) {
					rowMap.put(columnNames.get(i), row[i]);
				}
				resultList.add(rowMap);
			}

			Map<String, Object> data = new HashMap<>();
			data.put("data", resultList);
			data.put("columns", getCombinedProductionOptimizerColumnMetadata(plantId, aopYear, type));

			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("SP Executed successfully");
			aopMessageVM.setData(data);
			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Override
	public AOPMessageVM calculateProductionOptimizer(String plantId, String aopYear) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			String baseProcedure = resolveStoredProcedure(plantId);
			String calculateProcedure = baseProcedure.replace("_ProductionOptimizer", "_CalculateProductionOptimizer");
			Integer result = executeDynamicUpdateProcedure(calculateProcedure, plantId, aopYear);
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("SP Executed successfully");
			aopMessageVM.setData(result);
			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to execute calculate production optimizer", ex);
		}
	}

	@Override
	public AOPMessageVM getCombinedProductionOptimizerDropdown(String plantId) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			String viewName = "vwScrn" + vertical.getName() + "DropdownForCombinedProdctionOptimizer";

			String sql = "SELECT name, displayName, displayOrder FROM " + viewName + " ORDER BY displayOrder";
			List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
			List<Map<String, Object>> resultList = new ArrayList<>();
			for (Object[] row : rows) {
				Map<String, Object> map = new HashMap<>();
				map.put("name", row[0] != null ? row[0].toString() : null);
				map.put("displayName", row[1] != null ? row[1].toString() : null);
				map.put("displayOrder", row[2]);
				resultList.add(map);
			}

			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data fetched successfully");
			aopMessageVM.setData(resultList);
			return aopMessageVM;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch dropdown data", ex);
		}
	}

	@Override
	public AOPMessageVM getProductionOptimizerDropdown(String plantId) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			String viewName = "vwScrn" + vertical.getName() + "DropdownForProdctionOptimizer";

			String sql = "SELECT name, displayName, displayOrder FROM " + viewName + " ORDER BY displayOrder";
			List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
			List<Map<String, Object>> resultList = new ArrayList<>();
			for (Object[] row : rows) {
				Map<String, Object> map = new HashMap<>();
				map.put("name", row[0] != null ? row[0].toString() : null);
				map.put("displayName", row[1] != null ? row[1].toString() : null);
				map.put("displayOrder", row[2]);
				resultList.add(map);
			}

			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data fetched successfully");
			aopMessageVM.setData(resultList);
			return aopMessageVM;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch dropdown data", ex);
		}
	}

	public int executeDynamicUpdateProcedure(String procedureName, String plantId, String aopYear) {
		try {
			String callSql = "{call " + procedureName + "(?, ?)}";
			try (Connection connection = dataSource.getConnection();
				 CallableStatement stmt = connection.prepareCall(callSql)) {
				stmt.setString(1, plantId);
				stmt.setString(2, aopYear);
				int rowsAffected = stmt.executeUpdate();
				if (!connection.getAutoCommit()) {
					connection.commit();
				}
				return rowsAffected;
			} catch (SQLException e) {
				e.printStackTrace();
				return 0;
			}
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to execute procedure", ex);
		}
	}

	private String resolveStoredProcedure(String plantId) {
		Plants plant = plantsRepository.findById(UUID.fromString(plantId))
				.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
		Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
				.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
		Sites site = siteRepository.findById(plant.getSiteFkId())
				.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
		return vertical.getName() + "_" + site.getName() + "_ProductionOptimizer";
	}

	private List<Object[]> getProductionOptimizerData(String plantId, String aopYear, String lineFkId, String type) {
		try {
			String storedProcedure = resolveStoredProcedure(plantId);
			String sql = "EXEC " + storedProcedure
					+ " @plantId = :plantId, @aopYear = :aopYear, @lineFkId = :lineFkId, @type = :type";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("plantId", plantId);
			query.setParameter("aopYear", aopYear);
			query.setParameter("lineFkId", lineFkId);
			query.setParameter("type", type);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	private List<String> getProductionOptimizerColumns(String plantId, String aopYear, String lineFkId, String type) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<String> columnNames = new ArrayList<>();
			String storedProcedure = resolveStoredProcedure(plantId);
			String sql = "EXEC " + storedProcedure + " @plantId = ?, @aopYear = ?, @lineFkId = ?, @type = ?";

			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);
				ps.setString(3, lineFkId);
				ps.setString(4, type);
				try (ResultSet rs = ps.executeQuery()) {
					ResultSetMetaData metaData = rs.getMetaData();
					int columnCount = metaData.getColumnCount();
					for (int i = 1; i <= columnCount; i++) {
						columnNames.add(metaData.getColumnName(i));
					}
				}
			}
			return columnNames;
		});
	}

	private List<Map<String, Object>> getProductionOptimizerColumnMetadata(String plantId, String aopYear, String lineFkId,
			String type) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> columnMetadata = new ArrayList<>();
			String storedProcedure = resolveStoredProcedure(plantId);
			String sql = "EXEC " + storedProcedure + " @plantId = ?, @aopYear = ?, @lineFkId = ?, @type = ?";

			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);
				ps.setString(3, lineFkId);
				ps.setString(4, type);
				try (ResultSet rs = ps.executeQuery()) {
					ResultSetMetaData metaData = rs.getMetaData();
					int columnCount = metaData.getColumnCount();
					for (int i = 1; i <= columnCount; i++) {
						Map<String, Object> column = new HashMap<>();
						String columnType = metaData.getColumnTypeName(i);
						column.put("field", metaData.getColumnName(i));
						column.put("title", metaData.getColumnName(i));
						column.put("type", getFrontendType(columnType));
						column.put("editable", false);
						columnMetadata.add(column);
					}
				}
			}
			return columnMetadata;
		});
	}

	private List<Object[]> getCombinedProductionOptimizerData(String plantId, String aopYear, String type) {
		try {
			String storedProcedure = resolveStoredProcedure(plantId).replace("_ProductionOptimizer",
					"_CombinedProductionOptimizer");
			String sql = "EXEC " + storedProcedure + " @plantId = :plantId, @aopYear = :aopYear, @type = :type";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("plantId", plantId);
			query.setParameter("aopYear", aopYear);
			query.setParameter("type", type);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	private List<String> getCombinedProductionOptimizerColumns(String plantId, String aopYear, String type) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<String> columnNames = new ArrayList<>();
			String storedProcedure = resolveStoredProcedure(plantId).replace("_ProductionOptimizer",
					"_CombinedProductionOptimizer");
			String sql = "EXEC " + storedProcedure + " @plantId = ?, @aopYear = ?, @type = ?";

			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);
				ps.setString(3, type);
				try (ResultSet rs = ps.executeQuery()) {
					ResultSetMetaData metaData = rs.getMetaData();
					int columnCount = metaData.getColumnCount();
					for (int i = 1; i <= columnCount; i++) {
						columnNames.add(metaData.getColumnName(i));
					}
				}
			}
			return columnNames;
		});
	}

	private List<Map<String, Object>> getCombinedProductionOptimizerColumnMetadata(String plantId, String aopYear,
			String type) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> columnMetadata = new ArrayList<>();
			String storedProcedure = resolveStoredProcedure(plantId).replace("_ProductionOptimizer",
					"_CombinedProductionOptimizer");
			String sql = "EXEC " + storedProcedure + " @plantId = ?, @aopYear = ?, @type = ?";

			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);
				ps.setString(3, type);
				try (ResultSet rs = ps.executeQuery()) {
					ResultSetMetaData metaData = rs.getMetaData();
					int columnCount = metaData.getColumnCount();
					for (int i = 1; i <= columnCount; i++) {
						Map<String, Object> column = new HashMap<>();
						String columnType = metaData.getColumnTypeName(i);
						column.put("field", metaData.getColumnName(i));
						column.put("title", metaData.getColumnName(i));
						column.put("type", getFrontendType(columnType));
						column.put("editable", false);
						columnMetadata.add(column);
					}
				}
			}
			return columnMetadata;
		});
	}

	
	@Override
	@SuppressWarnings("unchecked")
	public byte[] exportProductionOptimizer(String plantId, String aopYear, String type) {
		try {
			AOPMessageVM lineVm = shutdownHistoryService.getLineDetails(plantId, aopYear);
			List<Map<String, Object>> lines = new ArrayList<>();
			if (lineVm != null && lineVm.getData() instanceof List) {
				for (Object o : (List<?>) lineVm.getData()) {
					if (o instanceof Map) {
						lines.add((Map<String, Object>) o);
					}
				}
			}

			int[] bounds = parseProdFiscalYearBounds(aopYear);
			List<String> monthHeaders = buildProdFiscalMonthHeaders(bounds[0], bounds[1]);

			Workbook workbook = new XSSFWorkbook();
			Set<String> usedSheetNames = new HashSet<>();

			if (lines.isEmpty()) {
				Sheet sheet = workbook.createSheet("ProductionOptimizer");
				writeProductionOptimizerSheet(workbook, sheet, monthHeaders, new ArrayList<>());
			} else {
				for (Map<String, Object> line : lines) {
					Object idObj = line.get("id");
					if (idObj == null || idObj.toString().isBlank()) {
						continue;
					}
					String lineId = idObj.toString();
					String display = line.get("displayName") != null ? line.get("displayName").toString()
							: (line.get("name") != null ? line.get("name").toString() : "Line");
					String sheetName = uniqueProdSheetName(Utility.sanitizeSheetName(display), usedSheetNames);
					usedSheetNames.add(sheetName);

					List<Map<String, Object>> rowData = getProductionOptimizerRows(plantId, aopYear, lineId, type);
					Sheet sheet = workbook.createSheet(sheetName);
					writeProductionOptimizerSheet(workbook, sheet, monthHeaders, rowData);
				}
				if (usedSheetNames.isEmpty()) {
					Sheet sheet = workbook.createSheet("ProductionOptimizer");
					writeProductionOptimizerSheet(workbook, sheet, monthHeaders, new ArrayList<>());
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

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> getProductionOptimizerRows(String plantId, String aopYear, String lineId,
			String type) {
		try {
			AOPMessageVM vm = getProductionOptimizer(plantId, aopYear, lineId, type);
			if (vm != null && vm.getData() instanceof Map) {
				Map<String, Object> dataMap = (Map<String, Object>) vm.getData();
				Object dataObj = dataMap.get("data");
				if (dataObj instanceof List) {
					List<Map<String, Object>> rows = new ArrayList<>();
					for (Object o : (List<?>) dataObj) {
						if (o instanceof Map) {
							rows.add((Map<String, Object>) o);
						}
					}
					return rows;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}

	private void writeProductionOptimizerSheet(Workbook workbook, Sheet sheet, List<String> monthHeaders,
			List<Map<String, Object>> rows) {
		CellStyle headerStyle = Utility.createBoldBorderedStyle(workbook);
		CellStyle totalStyle = Utility.createBoldBorderedStyle(workbook);

		// Fixed SP month keys in fiscal order
		String[] spMonthKeys = { "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan", "Feb", "Mar" };

		int currentRow = 0;
		Row headerRow = sheet.createRow(currentRow++);

		Cell gradeNameHeader = headerRow.createCell(0);
		gradeNameHeader.setCellValue("GradeName");
		gradeNameHeader.setCellStyle(headerStyle);

		for (int col = 0; col < monthHeaders.size(); col++) {
			Cell cell = headerRow.createCell(col + 1);
			cell.setCellValue(monthHeaders.get(col));
			cell.setCellStyle(headerStyle);
		}

		Cell totalHeader = headerRow.createCell(1 + monthHeaders.size());
		totalHeader.setCellValue("Total");
		totalHeader.setCellStyle(headerStyle);

		double[] columnTotals = new double[spMonthKeys.length];

		for (Map<String, Object> rowData : rows) {
			Row row = sheet.createRow(currentRow++);

			Object gradeNameVal = rowData.get("GradeName");
			row.createCell(0).setCellValue(gradeNameVal != null ? gradeNameVal.toString() : "");

			double rowTotal = 0;
			for (int i = 0; i < spMonthKeys.length; i++) {
				Object val = rowData.get(spMonthKeys[i]);
				double numVal = 0;
				if (val instanceof Number) {
					numVal = ((Number) val).doubleValue();
				}
				row.createCell(i + 1).setCellValue(numVal);
				columnTotals[i] += numVal;
				rowTotal += numVal;
			}
			row.createCell(spMonthKeys.length + 1).setCellValue(rowTotal);
		}

		Row totalRow = sheet.createRow(currentRow);
		Cell totalLabelCell = totalRow.createCell(0);
		totalLabelCell.setCellValue("Total");
		totalLabelCell.setCellStyle(totalStyle);

		double grandTotal = 0;
		for (int i = 0; i < spMonthKeys.length; i++) {
			Cell cell = totalRow.createCell(i + 1);
			cell.setCellValue(columnTotals[i]);
			cell.setCellStyle(totalStyle);
			grandTotal += columnTotals[i];
		}
		Cell grandTotalCell = totalRow.createCell(spMonthKeys.length + 1);
		grandTotalCell.setCellValue(grandTotal);
		grandTotalCell.setCellStyle(totalStyle);

		for (int i = 0; i <= spMonthKeys.length + 1; i++) {
			sheet.autoSizeColumn(i);
		}
	}

	private int[] parseProdFiscalYearBounds(String aopYear) {
		if (aopYear == null || aopYear.isBlank()) {
			int y = java.time.Year.now(java.time.ZoneId.systemDefault()).getValue();
			return new int[] { y, y + 1 };
		}
		String trimmed = aopYear.trim();
		int dash = trimmed.indexOf('-');
		if (dash < 0) {
			try {
				int y = Integer.parseInt(trimmed);
				return new int[] { y, y + 1 };
			} catch (NumberFormatException e) {
				int y = java.time.Year.now(java.time.ZoneId.systemDefault()).getValue();
				return new int[] { y, y + 1 };
			}
		}
		String first = trimmed.substring(0, dash).trim();
		String second = trimmed.substring(dash + 1).trim();
		try {
			int startYear = Integer.parseInt(first);
			int endYear;
			if (second.length() <= 2) {
				int century = (startYear / 100) * 100;
				endYear = century + Integer.parseInt(second);
				if (endYear <= startYear) {
					endYear += 100;
				}
			} else {
				endYear = Integer.parseInt(second);
			}
			return new int[] { startYear, endYear };
		} catch (NumberFormatException e) {
			int y = java.time.Year.now(java.time.ZoneId.systemDefault()).getValue();
			return new int[] { y, y + 1 };
		}
	}

	private static String prodTwoDigitYear(int fullYear) {
		return String.format("%02d", fullYear % 100);
	}

	private List<String> buildProdFiscalMonthHeaders(int startYear, int endYear) {
		String yy1 = prodTwoDigitYear(startYear);
		String yy2 = prodTwoDigitYear(endYear);
		List<String> headers = new ArrayList<>(12);
		for (String m : new String[] { "Apr-", "May-", "Jun-", "Jul-", "Aug-", "Sep-", "Oct-", "Nov-", "Dec-" }) {
			headers.add(m + yy1);
		}
		for (String m : new String[] { "Jan-", "Feb-", "Mar-" }) {
			headers.add(m + yy2);
		}
		return headers;
	}

	private String uniqueProdSheetName(String sanitizedBase, Set<String> used) {
		String name = sanitizedBase;
		int counter = 1;
		while (used.contains(name)) {
			String suffix = "_" + (++counter);
			int maxBase = Math.max(1, 31 - suffix.length());
			String base = sanitizedBase.length() > maxBase ? sanitizedBase.substring(0, maxBase) : sanitizedBase;
			name = base + suffix;
			if (name.length() > 31) {
				name = name.substring(0, 31);
			}
		}
		return name;
	}

	private String getFrontendType(String sqlTypeName) {
		if (sqlTypeName == null) {
			return "string";
		}
		
		String typeUpper = sqlTypeName.toUpperCase();

		if (typeUpper.contains("CHAR") || typeUpper.contains("TEXT") || typeUpper.contains("CLOB")) {
			return "string";
		}
		if (typeUpper.contains("INT") || typeUpper.contains("TINYINT") || typeUpper.contains("BIGINT")
				|| typeUpper.contains("SMALLINT") || typeUpper.contains("DECIMAL") || typeUpper.contains("NUMERIC")
				|| typeUpper.contains("FLOAT") || typeUpper.contains("REAL") || typeUpper.contains("DOUBLE")
				|| typeUpper.contains("MONEY")) {
			return "number";
		}
		if (typeUpper.contains("DATE") || typeUpper.contains("DATETIME") || typeUpper.contains("TIME")) {
			return "date";
		}
		return "string";
	}
}

