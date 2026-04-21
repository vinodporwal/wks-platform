package com.wks.caseengine.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;

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

	@Override
	public AOPMessageVM getProductionOptimizer(String plantId, String aopYear, String lineFkId, String type) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<Object[]> results = getProductionOptimizerData(plantId, aopYear, lineFkId, type);
			List<String> columnNames = getProductionOptimizerColumns(plantId, aopYear, lineFkId, type);

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

