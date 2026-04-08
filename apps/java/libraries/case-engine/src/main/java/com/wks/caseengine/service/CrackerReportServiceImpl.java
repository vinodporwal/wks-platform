package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.CatChemNormDTO;
import com.wks.caseengine.dto.ConfigurationDTO;
import com.wks.caseengine.dto.LIMSSpyroInputDTO;
import com.wks.caseengine.entity.AopCalculation;
import com.wks.caseengine.entity.NormParameters;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.ScreenMapping;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.NormParametersRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;
import com.wks.caseengine.service.ConfigurationService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class CrackerReportServiceImpl implements CrackerReportService {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private PlantsRepository plantsRepository;

	@Autowired
	private SiteRepository siteRepository;

	@Autowired
	private VerticalsRepository verticalRepository;
	
	@Autowired
	private NormParametersRepository normParametersRepository;

	@Autowired
	private ConfigurationService configurationService;

	@Override
	public AOPMessageVM getSpyroInputReport(String plantId, String AopYear, String Mode) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<Object[]> results = getSpyroInputReportData(plantId, AopYear, Mode);

			List<String> columnNames = getSpyroInputReportColumns(plantId, AopYear, Mode);

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
			data.put("columns", getSpyroInputReportColumnMetadata(plantId, AopYear, Mode));

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

	@Transactional(transactionManager = "db2TransactionManager", readOnly = false)
	public List<Object[]> getSpyroInputReportData(String plantId, String AopYear, String Mode) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			String storedProcedure = vertical.getName() + "_" + site.getName() + "_GetSpyroInputReport";

			String sql = "EXEC " + storedProcedure
					+ " @plantId = :plantId, @AopYear = :AopYear, @Mode = :Mode, @siteId = :siteId, @verticalId = :verticalId";

			Query query = entityManager.createNativeQuery(sql);

			query.setParameter("plantId", plantId);
			query.setParameter("AopYear", AopYear);
			query.setParameter("Mode", Mode);
			query.setParameter("siteId", site.getId().toString());
			query.setParameter("verticalId", vertical.getId().toString());

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}
	@Transactional(transactionManager = "db2TransactionManager", readOnly = false)
	public List<String> getSpyroInputReportColumns(String plantId, String AopYear, String Mode) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<String> columnNames = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_GetSpyroInputReport";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @AopYear = ?, @Mode = ?, @siteId = ?, @verticalId = ?";

			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, AopYear);
				ps.setString(3, Mode);
				ps.setString(4, site.getId().toString());
				ps.setString(5, vertical.getId().toString());

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
	
	@Transactional(transactionManager = "db2TransactionManager", readOnly = false)
	public List<Map<String, Object>> getSpyroInputReportColumnMetadata(String plantId, String AopYear, String Mode) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> columnMetadata = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_GetSpyroInputReport";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @AopYear = ?, @Mode = ?, @siteId = ?, @verticalId = ?";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, AopYear);
				ps.setString(3, Mode);
				ps.setString(4, site.getId().toString());
				ps.setString(5, vertical.getId().toString());

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

	// Helper method to format column titles
	private String formatTitle(String columnName) {
		return columnName.replace("_", " ");
	}

	// Helper method to map SQL data types to frontend types
	private String getFrontendType(String sqlTypeName) {
		switch (sqlTypeName.toUpperCase()) {
			case "VARCHAR":
			case "NVARCHAR":
			case "CHAR":
				return "string";
			case "INT":
			case "TINYINT":
			case "BIGINT":
			case "SMALLINT":
			case "DECIMAL":
			case "FLOAT":
			case "DOUBLE":
			case "NUMERIC":
				return "number";
			case "DATE":
			case "DATETIME":
			case "DATETIME2":
				return "date";
			default:
				return "string";
		}
	}

	@Override
	public AOPMessageVM getSpyroOutputReport(String plantId, String AopYear, String Mode) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<Object[]> results = getSpyroOutputReportData(plantId, AopYear, Mode);
			List<String> columnNames = getSpyroOutputReportColumns(plantId, AopYear, Mode);

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
			data.put("columns", getSpyroOutputReportColumnMetadata(plantId, AopYear, Mode));

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

	public List<Object[]> getSpyroOutputReportData(String plantId, String AopYear, String Mode) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			String storedProcedure = vertical.getName() + "_" + site.getName() + "_GetSpyroOutputReport";

			String sql = "EXEC " + storedProcedure
					+ " @plantId = :plantId, @AopYear = :AopYear, @Mode = :Mode, @siteId = :siteId, @verticalId = :verticalId";

			Query query = entityManager.createNativeQuery(sql);

			query.setParameter("plantId", plantId);
			query.setParameter("AopYear", AopYear);
			query.setParameter("Mode", Mode);
			query.setParameter("siteId", site.getId().toString());
			query.setParameter("verticalId", vertical.getId().toString());

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	public List<String> getSpyroOutputReportColumns(String plantId, String AopYear, String Mode) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<String> columnNames = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_GetSpyroOutputReport";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @AopYear = ?, @Mode = ?, @siteId = ?, @verticalId = ?";

			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, AopYear);
				ps.setString(3, Mode);
				ps.setString(4, site.getId().toString());
				ps.setString(5, vertical.getId().toString());

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

	public List<Map<String, Object>> getSpyroOutputReportColumnMetadata(String plantId, String AopYear, String Mode) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> columnMetadata = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_GetSpyroOutputReport";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @AopYear = ?, @Mode = ?, @siteId = ?, @verticalId = ?";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, AopYear);
				ps.setString(3, Mode);
				ps.setString(4, site.getId().toString());
				ps.setString(5, vertical.getId().toString());

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

	@Override
	public AOPMessageVM getFinalNormsReport(String plantId, String AopYear, String reportType) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<Object[]> results = getFinalNormsReportData(plantId, AopYear,reportType);
			List<String> columnNames = getFinalNormsReportColumns(plantId, AopYear,reportType);

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
			data.put("columns", getFinalNormsReportColumnMetadata(plantId, AopYear));

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

	public List<Object[]> getFinalNormsReportData(String plantId, String aopYear, String reportType) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			String storedProcedure = vertical.getName() + "_" + site.getName() + "_GetFinalNormsReport";

			String sql = "EXEC " + storedProcedure
					+ " @plantId = :plantId, @aopYear = :aopYear, @reportType = :reportType";

			Query query = entityManager.createNativeQuery(sql);

			query.setParameter("plantId", plantId);
			query.setParameter("aopYear", aopYear);
			query.setParameter("reportType", reportType);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	public List<String> getFinalNormsReportColumns(String plantId, String aopYear,String reportType) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<String> columnNames = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_GetFinalNormsReport";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @AopYear = ? , @reportType = ?";

			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);
				ps.setString(3, reportType);

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

	public List<Map<String, Object>> getFinalNormsReportColumnMetadata(String plantId, String aopYear) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> columnMetadata = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_GetFinalNormsReport";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @AopYear = ?";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);

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
	
	

	@Override
	@Transactional(transactionManager = "db2TransactionManager", readOnly = false)
	public AOPMessageVM getFinalNormsProductionReport(String plantId, String AopYear,String reportType) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<Object[]> results = getFinalNormsProductionReportData(plantId, AopYear, reportType);
			List<String> columnNames = getFinalNormsProductionReportColumns(plantId, AopYear,reportType);

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
			data.put("columns", getFinalNormsProductionReportColumnMetadata(plantId, AopYear));

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

	@Transactional(transactionManager = "db2TransactionManager", readOnly = false)
	public List<Object[]> getFinalNormsProductionReportData(String plantId, String aopYear,String reportType) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			String storedProcedure = vertical.getName() + "_" + site.getName() + "_GetFinalNormsProductionReport";

			String sql = "EXEC " + storedProcedure
					+ " @plantId = :plantId, @aopYear = :aopYear, @reportType= :reportType";

			Query query = entityManager.createNativeQuery(sql);

			query.setParameter("plantId", plantId);
			query.setParameter("aopYear", aopYear);
			query.setParameter("reportType", reportType);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Transactional(transactionManager = "db2TransactionManager", readOnly = false)
	public List<String> getFinalNormsProductionReportColumns(String plantId, String aopYear, String reportType) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<String> columnNames = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_GetFinalNormsProductionReport";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @AopYear = ?, @reportType=?" ;

			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);
				ps.setString(3, reportType);
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

	@Transactional(transactionManager = "db2TransactionManager", readOnly = false)
	public List<Map<String, Object>> getFinalNormsProductionReportColumnMetadata(String plantId, String aopYear) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> columnMetadata = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_GetFinalNormsProductionReport";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @AopYear = ?";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);

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

	@Override
	public AOPMessageVM getConfigurationIntermediateValues(String plantId, String AopYear) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<Object[]> results = getConfigurationIntermediateValuesData(plantId, AopYear);
			List<String> columnNames = getConfigurationIntermediateValuesColumns(plantId, AopYear);

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
			data.put("columns", getConfigurationIntermediateValuesMetadata(plantId, AopYear));

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

	public List<Object[]> getConfigurationIntermediateValuesData(String plantId, String aopYear) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			String storedProcedure = vertical.getName() + "_GetConfigurationIntermediateValuesDataSet";

			String sql = "EXEC " + storedProcedure
					+ " @plantId = :plantId, @aopYear = :aopYear";

			Query query = entityManager.createNativeQuery(sql);

			query.setParameter("plantId", plantId);
			query.setParameter("aopYear", aopYear);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	public List<String> getConfigurationIntermediateValuesColumns(String plantId, String aopYear) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<String> columnNames = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_GetConfigurationIntermediateValuesDataSet";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @AopYear = ?";

			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);

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

	public List<Map<String, Object>> getConfigurationIntermediateValuesMetadata(String plantId, String aopYear) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> columnMetadata = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_GetConfigurationIntermediateValuesDataSet";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @AopYear = ?";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);

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

	@Override
	public AOPMessageVM getFindingModel(String plantId, String AopYear) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<Object[]> results = getFindingModelData(plantId, AopYear);
			List<String> columnNames = getFindingModelColumns(plantId, AopYear);

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
			data.put("columns", getFindingModelMetadata(plantId, AopYear));

			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("SP Executed successfully");
			aopMessageVM.setData(data);
			return aopMessageVM;

		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to fetch data", ex);
		}

	}

	public List<Object[]> getFindingModelData(String plantId, String aopYear) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_GetCrackerFindingModelLast5Years";

			String sql = "EXEC " + storedProcedure
					+ " @aopYear = :aopYear";

			Query query = entityManager.createNativeQuery(sql);

			query.setParameter("aopYear", aopYear);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	public List<String> getFindingModelColumns(String plantId, String aopYear) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<String> columnNames = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_GetCrackerFindingModelLast5Years";
			String sql = "EXEC " + storedProcedure
					+ " @AopYear = ?";

			try (PreparedStatement ps = connection.prepareStatement(sql)) {

				ps.setString(1, aopYear);

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

	public List<Map<String, Object>> getFindingModelMetadata(String plantId, String aopYear) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> columnMetadata = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_GetCrackerFindingModelLast5Years";
			String sql = "EXEC " + storedProcedure
					+ " @AopYear = ?";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {

				ps.setString(1, aopYear);

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

	@Override
	public AOPMessageVM getMIISData(String plantId, String AopYear) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<Object[]> results = getMIISDataLast5Years(plantId, AopYear);
			List<String> columnNames = getMIISColumns(plantId, AopYear);

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
			data.put("columns", getMIISMetadata(plantId, AopYear));

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

	public List<Object[]> getMIISDataLast5Years(String plantId, String aopYear) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_GetMIISDataLast5Years";

			String sql = "EXEC " + storedProcedure
					+ " @aopYear = :aopYear";

			Query query = entityManager.createNativeQuery(sql);

			query.setParameter("aopYear", aopYear);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	public List<String> getMIISColumns(String plantId, String aopYear) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<String> columnNames = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_GetMIISDataLast5Years";
			String sql = "EXEC " + storedProcedure
					+ " @AopYear = ?";

			try (PreparedStatement ps = connection.prepareStatement(sql)) {

				ps.setString(1, aopYear);

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

	public List<Map<String, Object>> getMIISMetadata(String plantId, String aopYear) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> columnMetadata = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_GetMIISDataLast5Years";
			String sql = "EXEC " + storedProcedure
					+ " @AopYear = ?";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {

				ps.setString(1, aopYear);

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

	@Override
	public AOPMessageVM getCatChemRawDatasetReport(String plantId, String year, String periodTo, String periodFrom) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<Object[]> results = getCatChemRawDatasetReportData(plantId, year, periodTo, periodFrom);

			List<String> columnNames = getCatChemRawDatasetReportColumns(plantId, year, periodTo, periodFrom);

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
			data.put("columns", getCatChemRawDatasetReportColumnMetadata(plantId, year, periodTo, periodFrom));

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

	public List<Object[]> getCatChemRawDatasetReportData(String plantId, String aopYear, String PeriodTo,
			String PeriodFrom) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			String storedProcedure = vertical.getName() + "_" + site.getName() + "_STGCatChemRawDataset";

			String sql = "EXEC " + storedProcedure
					+ " @plantId = :plantId, @aopYear = :aopYear, @PeriodTo = :PeriodTo, @PeriodFrom = :PeriodFrom ";

			Query query = entityManager.createNativeQuery(sql);

			query.setParameter("plantId", plantId);
			query.setParameter("aopYear", aopYear);
			query.setParameter("PeriodTo", PeriodTo);
			query.setParameter("PeriodFrom", PeriodFrom);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	public List<String> getCatChemRawDatasetReportColumns(String plantId, String aopYear, String PeriodTo,
			String PeriodFrom) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<String> columnNames = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_STGCatChemRawDataset";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @aopYear = ?, @PeriodTo = ?, @PeriodFrom = ? ";

			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);
				ps.setString(3, PeriodTo);
				ps.setString(4, PeriodFrom);

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

	public List<Map<String, Object>> getCatChemRawDatasetReportColumnMetadata(String plantId, String aopYear,
			String PeriodTo, String PeriodFrom) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> columnMetadata = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_STGCatChemRawDataset";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @aopYear = ?, @PeriodTo = ?, @PeriodFrom = ? ";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);
				ps.setString(3, PeriodTo);
				ps.setString(4, PeriodFrom);

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

	@Override
	public AOPMessageVM getCatChemMonthlyAveragesReport(String plantId, String aopYear, String periodTo,
			String periodFrom) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			// Fetch Data
			List<Object[]> results = getCatChemMonthlyAveragesReportData(plantId, aopYear, periodTo, periodFrom);

			// Fetch Column Names
			List<String> columnNames = getCatChemMonthlyAveragesReportColumns(plantId, aopYear, periodTo, periodFrom);

			// Map results to list of maps
			List<Map<String, Object>> resultList = new ArrayList<>();
			for (Object[] row : results) {
				Map<String, Object> rowMap = new LinkedHashMap<>();
				for (int i = 0; i < columnNames.size(); i++) {
					rowMap.put(columnNames.get(i), row[i]);
				}
				resultList.add(rowMap);
			}

			// Prepare response data
			Map<String, Object> data = new HashMap<>();
			data.put("data", resultList);
			data.put("columns", getCatChemMonthlyAveragesReportColumnMetadata(plantId, aopYear, periodTo, periodFrom));

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
	public AOPMessageVM getCatChemNorms(String plantId, String aopYear, String type) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_GetCatChemNorms";
			String sql = "EXEC " + storedProcedure + " @plantId = :plantId, @aopYear = :aopYear, @type = :type";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("plantId", UUID.fromString(plantId));
			query.setParameter("aopYear", aopYear);
			query.setParameter("type", type);

			@SuppressWarnings("unchecked")
			List<Object[]> results = query.getResultList();

			List<CatChemNormDTO> resultList = new ArrayList<>();
			for (Object[] row : results) {
				CatChemNormDTO dto = new CatChemNormDTO();
				dto.setNormParameterFKId(row.length > 0 && row[0] != null ? row[0].toString() : null);
				dto.setJan(row.length > 1 && row[1] != null ? Double.parseDouble(row[1].toString()) : null);
				dto.setFeb(row.length > 2 && row[2] != null ? Double.parseDouble(row[2].toString()) : null);
				dto.setMar(row.length > 3 && row[3] != null ? Double.parseDouble(row[3].toString()) : null);
				dto.setApr(row.length > 4 && row[4] != null ? Double.parseDouble(row[4].toString()) : null);
				dto.setMay(row.length > 5 && row[5] != null ? Double.parseDouble(row[5].toString()) : null);
				dto.setJun(row.length > 6 && row[6] != null ? Double.parseDouble(row[6].toString()) : null);
				dto.setJul(row.length > 7 && row[7] != null ? Double.parseDouble(row[7].toString()) : null);
				dto.setAug(row.length > 8 && row[8] != null ? Double.parseDouble(row[8].toString()) : null);
				dto.setSep(row.length > 9 && row[9] != null ? Double.parseDouble(row[9].toString()) : null);
				dto.setOct(row.length > 10 && row[10] != null ? Double.parseDouble(row[10].toString()) : null);
				dto.setNov(row.length > 11 && row[11] != null ? Double.parseDouble(row[11].toString()) : null);
				dto.setDec(row.length > 12 && row[12] != null ? Double.parseDouble(row[12].toString()) : null);
				dto.setRemarks(row.length > 13 && row[13] != null ? row[13].toString() : null);
				dto.setAuditYear(row.length > 14 && row[14] != null ? row[14].toString() : null);
				dto.setUom(row.length > 15 && row[15] != null ? row[15].toString() : null);
				dto.setNormTypeName(row.length > 16 && row[16] != null ? row[16].toString() : null);
				if (row.length > 17 && row[17] != null) {
					dto.setIsEditable(row[17] instanceof Boolean ? (Boolean) row[17] : ((Number) row[17]).intValue() != 0);
				} else {
					dto.setIsEditable(null);
				}
				dto.setDisplayName(row.length > 18 && row[18] != null ? row[18].toString() : null);
				dto.setType(row.length > 19 && row[19] != null ? row[19].toString() : null);
				resultList.add(dto);
			}

			Map<String, Object> dataMap = new HashMap<>();
			dataMap.put("catChemNormList", resultList);
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("SP Executed successfully");
			aopMessageVM.setData(dataMap);
			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}
	
	public byte[] exportCatChemNorms(String year, String plantId,String type, boolean isAfterSave, List<CatChemNormDTO> catChemNormDTOs) {
	    try {
	        if (!isAfterSave) {
	            AOPMessageVM aopMessageVM = getCatChemNorms(plantId, year, type);
	            Map<String, Object> innerMap = (Map<String, Object>) aopMessageVM.getData();
	            if (innerMap != null) {
	            	// Data map from getCatChemNorms uses key \"catChemNormList\"
	            	catChemNormDTOs = (List<CatChemNormDTO>) innerMap.get("catChemNormList");
	            }
	        }
	        if (catChemNormDTOs == null) {
	        	catChemNormDTOs = new ArrayList<>();
	        }

	        Workbook workbook = new XSSFWorkbook();
	        Sheet sheet = workbook.createSheet("Sheet1");
	        int currentRow = 0;

	        List<String> innerHeaders = new ArrayList<>();
	        innerHeaders.add("Particulars");
	        innerHeaders.add("UOM");
	        innerHeaders.add("Value");
	        innerHeaders.add("Remark");
	        innerHeaders.add("NormParameterId");
	        
	        Row headerRow = sheet.createRow(currentRow++);
	        for (int col = 0; col < innerHeaders.size(); col++) {
	            Cell cell = headerRow.createCell(col);
	            cell.setCellValue(innerHeaders.get(col));
	            cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
	        }

	        for (CatChemNormDTO dto : catChemNormDTOs) {
	            Row row = sheet.createRow(currentRow++);
	            
	            setCellValue(row, 0, normParametersRepository.findById(UUID.fromString(dto.getNormParameterFKId())).get().getDisplayName());
	            setCellValue(row, 1, dto.getUom());
	            setCellValue(row, 2, dto.getApr());
	            setCellValue(row, 3, dto.getRemarks());
	            setCellValue(row, 4, dto.getNormParameterFKId());
	        }
	            sheet.setColumnHidden(4, true);
	        

	        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	        workbook.write(outputStream);
	        workbook.close();
	        return outputStream.toByteArray();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return null;
	}

	@Override
	public AOPMessageVM importCatChemNormsExcel(String year, String plantId, String type, MultipartFile file) {
	    try {
	        List<ConfigurationDTO> configurationDTOs = new ArrayList<>();

	        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
	            Sheet sheet = workbook.getSheetAt(0);
	            if (sheet == null) {
	                throw new IllegalArgumentException("Sheet1 not found in uploaded file");
	            }

	            // Start after header row
	            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
	                Row row = sheet.getRow(i);
	                if (row == null) {
	                    continue;
	                }

	                String normParameterId = getStringCellValue(row.getCell(4));
	                if (normParameterId == null || normParameterId.trim().isEmpty()) {
	                    continue;
	                }

	                Double value = getNumericCellValue(row.getCell(2));
	                String remarks = getStringCellValue(row.getCell(3));
	                String uom = getStringCellValue(row.getCell(1));

	                ConfigurationDTO dto = new ConfigurationDTO();
	                dto.setNormParameterFKId(normParameterId);
	                dto.setApr(value);
	                dto.setRemarks(remarks);
	                dto.setAuditYear(year);
	                dto.setUOM(uom);
	                dto.setType(type);
	                configurationDTOs.add(dto);
	            }
	        }

	        List<ConfigurationDTO> failedRecords = configurationService.saveConfigurationData(year, plantId, null,
	                configurationDTOs, null);

	        AOPMessageVM aopMessageVM = new AOPMessageVM();
	        if (failedRecords != null && !failedRecords.isEmpty()) {
	            byte[] fileByteArray = createCatChemNormsExcelResponse(year, plantId, configurationDTOs);
	            String base64File = Base64.getEncoder().encodeToString(fileByteArray);
	            aopMessageVM.setData(base64File);
	            aopMessageVM.setCode(400);
	            aopMessageVM.setMessage("Partial data has been saved");
	        } else {
	            aopMessageVM.setCode(200);
	            aopMessageVM.setMessage("All data has been saved");
	        }
	        return aopMessageVM;
	    } catch (IllegalArgumentException e) {
	        throw new RestInvalidArgumentException("Invalid input", e);
	    } catch (Exception e) {
	        throw new RuntimeException("Failed to import Cat/Chem norms from Excel", e);
	    }
	}

	private byte[] createCatChemNormsExcelResponse(String year, String plantId, List<ConfigurationDTO> list) {
	    try {
	        Workbook workbook = new XSSFWorkbook();
	        Sheet sheet = workbook.createSheet("Sheet1");
	        int currentRow = 0;

	        List<String> innerHeaders = new ArrayList<>();
	        innerHeaders.add("Particulars");
	        innerHeaders.add("UOM");
	        innerHeaders.add("Value");
	        innerHeaders.add("Remark");
	        innerHeaders.add("NormParameterId");

	        Row headerRow = sheet.createRow(currentRow++);
	        for (int col = 0; col < innerHeaders.size(); col++) {
	            Cell cell = headerRow.createCell(col);
	            cell.setCellValue(innerHeaders.get(col));
	            cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
	        }

	        for (ConfigurationDTO dto : list) {
	            if (dto.getSaveStatus() != null && dto.getSaveStatus().equalsIgnoreCase("Failed")) {
	                Row row = sheet.createRow(currentRow++);
	                setCellValue(row, 0, normParametersRepository.findById(UUID.fromString(dto.getNormParameterFKId())).get().getDisplayName());
	                setCellValue(row, 1, dto.getUOM());
	                setCellValue(row, 2, dto.getApr());
	                setCellValue(row, 3, dto.getRemarks());
	                setCellValue(row, 4, dto.getNormParameterFKId());
	            }
	        }

	        sheet.setColumnHidden(4, true);

	        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	        workbook.write(outputStream);
	        workbook.close();
	        return outputStream.toByteArray();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return null;
	}

	private String getStringCellValue(Cell cell) {
	    if (cell == null) {
	        return null;
	    }
	    switch (cell.getCellType()) {
	        case STRING:
	            return cell.getStringCellValue();
	        case NUMERIC:
	            return Double.toString(cell.getNumericCellValue());
	        case BOOLEAN:
	            return Boolean.toString(cell.getBooleanCellValue());
	        default:
	            return null;
	    }
	}

	private Double getNumericCellValue(Cell cell) {
	    if (cell == null) {
	        return null;
	    }
	    switch (cell.getCellType()) {
	        case NUMERIC:
	            return cell.getNumericCellValue();
	        case STRING:
	            try {
	                String s = cell.getStringCellValue();
	                return (s == null || s.trim().isEmpty()) ? null : Double.parseDouble(s.trim());
	            } catch (NumberFormatException e) {
	                return null;
	            }
	        default:
	            return null;
	    }
	}
	
	private void setCellValue(Row row, int col, Object value) {
	    Cell cell = row.createCell(col);
	    if (value == null) {
	        cell.setCellValue("");
	    } else if (value instanceof Number) {
	        cell.setCellValue(((Number) value).doubleValue());
	    } else if (value instanceof Boolean) {
	        cell.setCellValue((Boolean) value);
	    } else {
	        cell.setCellValue(value.toString());
	    }
	}

	// -------------------- Fetch Data --------------------
	public List<Object[]> getCatChemMonthlyAveragesReportData(String plantId, String aopYear, String PeriodTo,
			String PeriodFrom) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_STGCatChemMonthlyAverages";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = :plantId, @aopYear = :aopYear, @PeriodTo = :PeriodTo, @PeriodFrom = :PeriodFrom ";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("plantId", plantId);
			query.setParameter("aopYear", aopYear);
			query.setParameter("PeriodTo", PeriodTo);
			query.setParameter("PeriodFrom", PeriodFrom);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	// -------------------- Fetch Column Names --------------------
	public List<String> getCatChemMonthlyAveragesReportColumns(String plantId, String aopYear, String PeriodTo,
			String PeriodFrom) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<String> columnNames = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_STGCatChemMonthlyAverages";
			String sql = "EXEC " + storedProcedure + " ?, ?, ?, ?";

			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);
				ps.setString(3, PeriodTo);
				ps.setString(4, PeriodFrom);

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

	// -------------------- Fetch Column Metadata --------------------
	public List<Map<String, Object>> getCatChemMonthlyAveragesReportColumnMetadata(String plantId, String aopYear,
			String PeriodTo, String PeriodFrom) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> columnMetadata = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_STGCatChemMonthlyAverages";
			String sql = "EXEC " + storedProcedure + " ?, ?, ?, ?";

			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);
				ps.setString(3, PeriodTo);
				ps.setString(4, PeriodFrom);

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

	@Override
	public AOPMessageVM getUtilitiesRawDataReport(String plantId, String year, String periodTo, String periodFrom) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<Object[]> results = getUtilitiesRawReportData(plantId, year, periodTo, periodFrom);

			List<String> columnNames = getUtilitiesRawDataReportColumns(plantId, year, periodTo, periodFrom);

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
			data.put("columns", getUtilitiesRawReportColumnMetadata(plantId, year, periodTo, periodFrom));

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

	public List<Object[]> getUtilitiesRawReportData(String plantId, String aopYear, String PeriodTo,
			String PeriodFrom) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			String storedProcedure = vertical.getName() + "_" + site.getName() + "_MISUtilitiesRawDataSet";

			String sql = "EXEC " + storedProcedure
					+ " @plantId = :plantId, @aopYear = :aopYear, @PeriodTo = :PeriodTo, @PeriodFrom = :PeriodFrom ";

			Query query = entityManager.createNativeQuery(sql);

			query.setParameter("plantId", plantId);
			query.setParameter("aopYear", aopYear);
			query.setParameter("PeriodTo", PeriodTo);
			query.setParameter("PeriodFrom", PeriodFrom);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	public List<String> getUtilitiesRawDataReportColumns(String plantId, String aopYear, String PeriodTo,
			String PeriodFrom) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<String> columnNames = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_MISUtilitiesRawDataSet";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @aopYear = ?, @PeriodTo = ?, @PeriodFrom = ? ";

			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);
				ps.setString(3, PeriodTo);
				ps.setString(4, PeriodFrom);

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

	public List<Map<String, Object>> getUtilitiesRawReportColumnMetadata(String plantId, String aopYear,
			String PeriodTo, String PeriodFrom) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> columnMetadata = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_MISUtilitiesRawDataSet";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @aopYear = ?, @PeriodTo = ?, @PeriodFrom = ? ";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);
				ps.setString(3, PeriodTo);
				ps.setString(4, PeriodFrom);

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

	@Override
	public AOPMessageVM getSTGCatCamRawDatasetReport(String plantId, String year, String periodTo, String periodFrom) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<Object[]> results = getSTGCatCamRawDatasetReportData(plantId, year, periodTo, periodFrom);

			List<String> columnNames = getSTGCatCamRawDatasetReportColumns(plantId, year, periodTo, periodFrom);

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
			data.put("columns", getSTGCatCamRawDatasetReportColumnMetadata(plantId, year, periodTo, periodFrom));

			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("SP Executed successfully");
			aopMessageVM.setData(data);
			return aopMessageVM;

		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to fetch data", ex);
		}

	}

	public List<Object[]> getSTGCatCamRawDatasetReportData(String plantId, String aopYear, String PeriodTo,
			String PeriodFrom) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			String storedProcedure = vertical.getName() + "_" + site.getName() + "_STGCatChemRawDataset";

			String sql = "EXEC " + storedProcedure
					+ " @plantId = :plantId, @aopYear = :aopYear, @PeriodTo = :PeriodTo, @PeriodFrom = :PeriodFrom";

			Query query = entityManager.createNativeQuery(sql);

			query.setParameter("plantId", plantId);
			query.setParameter("aopYear", aopYear);
			query.setParameter("PeriodTo", PeriodTo);
			query.setParameter("PeriodFrom", PeriodFrom);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	public List<String> getSTGCatCamRawDatasetReportColumns(String plantId, String aopYear, String PeriodTo,
			String PeriodFrom) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<String> columnNames = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_STGCatChemRawDataset";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @aopYear = ?, @PeriodTo = ?, @PeriodFrom = ? ";

			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);
				ps.setString(3, PeriodTo);
				ps.setString(4, PeriodFrom);

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

	public List<Map<String, Object>> getSTGCatCamRawDatasetReportColumnMetadata(String plantId, String aopYear,
			String PeriodTo, String PeriodFrom) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> columnMetadata = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_STGCatChemRawDataset";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @aopYear = ?, @PeriodTo = ?, @PeriodFrom = ? ";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);
				ps.setString(3, PeriodTo);
				ps.setString(4, PeriodFrom);

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

	@Override
	public AOPMessageVM getMISUtiltiesMonthlyAveragesReport(String plantId, String year, String periodTo,
			String periodFrom) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<Object[]> results = getMISUtiltiesMonthlyAveragesReportData(plantId, year, periodTo, periodFrom);

			List<String> columnNames = getMISUtiltiesMonthlyAveragesReportColumns(plantId, year, periodTo, periodFrom);

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
			data.put("columns", getMISUtiltiesMonthlyAveragesReportColumnMetadata(plantId, year, periodTo, periodFrom));

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

	public List<Object[]> getMISUtiltiesMonthlyAveragesReportData(String plantId, String aopYear, String PeriodTo,
			String PeriodFrom) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			String storedProcedure = vertical.getName() + "_" + site.getName() + "_MISUtilitiesMonthlyAverages";

			String sql = "EXEC " + storedProcedure
					+ " @plantId = :plantId, @aopYear = :aopYear, @PeriodTo = :PeriodTo, @PeriodFrom = :PeriodFrom ";

			Query query = entityManager.createNativeQuery(sql);

			query.setParameter("plantId", plantId);
			query.setParameter("aopYear", aopYear);
			query.setParameter("PeriodTo", PeriodTo);
			query.setParameter("PeriodFrom", PeriodFrom);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	public List<String> getMISUtiltiesMonthlyAveragesReportColumns(String plantId, String aopYear, String PeriodTo,
			String PeriodFrom) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<String> columnNames = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_MISUtilitiesMonthlyAverages";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @aopYear = ?, @PeriodTo = ?, @PeriodFrom = ? ";

			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);
				ps.setString(3, PeriodTo);
				ps.setString(4, PeriodFrom);

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

	public List<Map<String, Object>> getMISUtiltiesMonthlyAveragesReportColumnMetadata(String plantId, String aopYear,
			String PeriodTo, String PeriodFrom) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> columnMetadata = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_MISUtilitiesMonthlyAverages";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @aopYear = ?, @PeriodTo = ?, @PeriodFrom = ? ";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);
				ps.setString(3, PeriodTo);
				ps.setString(4, PeriodFrom);

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

	@Override
	public AOPMessageVM getRawDataForSteamValuesReport(String plantId, String year, String periodTo, String periodFrom,
			String mode) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<Object[]> results = getRawDataForSteamValuesReportData(plantId, year, periodTo, periodFrom, mode);

			List<String> columnNames = getRawDataForSteamValuesReportColumns(plantId, year, periodTo, periodFrom, mode);

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
			data.put("columns", getRawDataForSteamValuesColumnMetadata(plantId, year, periodTo, periodFrom, mode));

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

	public List<Object[]> getRawDataForSteamValuesReportData(String plantId, String aopYear, String PeriodTo,
			String PeriodFrom, String mode) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			String storedProcedure = vertical.getName() + "_" + site.getName() + "_RawDataForSteamValues";

			String sql = "EXEC " + storedProcedure
					+ " @plantId = :plantId, @aopYear = :aopYear, @PeriodTo = :PeriodTo, @PeriodFrom = :PeriodFrom, @mode = :mode ";

			Query query = entityManager.createNativeQuery(sql);

			query.setParameter("plantId", plantId);
			query.setParameter("aopYear", aopYear);
			query.setParameter("PeriodTo", PeriodTo);
			query.setParameter("PeriodFrom", PeriodFrom);
			query.setParameter("mode", mode);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	public List<String> getRawDataForSteamValuesReportColumns(String plantId, String aopYear, String PeriodTo,
			String PeriodFrom, String mode) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<String> columnNames = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_RawDataForSteamValues";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @aopYear = ?, @PeriodTo = ?, @PeriodFrom = ?, @mode = ? ";

			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);
				ps.setString(3, PeriodTo);
				ps.setString(4, PeriodFrom);
				ps.setString(5, mode);

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

	public List<Map<String, Object>> getRawDataForSteamValuesColumnMetadata(String plantId, String aopYear,
			String PeriodTo, String PeriodFrom, String mode) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> columnMetadata = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_RawDataForSteamValues";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @aopYear = ?, @PeriodTo = ?, @PeriodFrom = ?, @mode = ? ";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);
				ps.setString(3, PeriodTo);
				ps.setString(4, PeriodFrom);
				ps.setString(5, mode);

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

	@Override
	public AOPMessageVM getFindingSteamValuesReport(String mode, String plantId, String year) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<Map<String, Object>> mapList = new ArrayList<>();
			List<Object[]> objList = null;
			Plants plant = plantsRepository.findById(UUID.fromString(plantId)).get();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
			String viewName = "vw" + vertical.getName() + "FindingSteamVaues";
			objList = getFindingSteamValues(mode, viewName);
			for (Object[] obj : objList) {
				Map<String, Object> map = new HashMap<>();
				map.put("id", obj[0]);
				map.put("modeofOperation", obj[1]);
				map.put("materialdescription", obj[2]);
				map.put("totalQuantity", obj[3]);
				mapList.add(map);
			}
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data fetched successfully");
			aopMessageVM.setData(mapList);
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
		// TODO Auto-generated method stub
		return aopMessageVM;
	}

	public List<Object[]> getFindingSteamValues(String ModeofOperation, String viewName) {
		try {
			String sql = "SELECT TOP (10000) [Id], "
					+ "[ModeofOperation], [materialdescription], [totalQuantity] "
					+ "FROM " + viewName + " "
					+ "WHERE ModeofOperation = :ModeofOperation ";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("ModeofOperation", ModeofOperation);
			query.setParameter("ModeofOperation", ModeofOperation);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	
	@Override
	@Transactional(transactionManager = "db2TransactionManager", readOnly = false)
	public AOPMessageVM getFurnaceReport(String plantId, String year, String reportType){
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<Object[]> results = getFurnaceReportData(plantId, year, reportType);

			List<String> columnNames = getFurnaceReportColumns(plantId, year, reportType);

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
			data.put("columns", getFurnaceReportColumnMetadata(plantId, year, reportType));

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
	@Transactional(transactionManager = "db2TransactionManager", readOnly = false)
	public List<Object[]> getFurnaceReportData(String plantId, String aopYear, String reportType) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			String storedProcedure = vertical.getName() + "_" + site.getName() + "_FurnaceDataSet";

			String sql = "EXEC " + storedProcedure
					+ " @plantId = :plantId, @aopYear = :aopYear, @reportType = :reportType";

			Query query = entityManager.createNativeQuery(sql);

			query.setParameter("plantId", plantId);
			query.setParameter("aopYear", aopYear);
			query.setParameter("reportType", reportType);
			

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Transactional(transactionManager = "db2TransactionManager", readOnly = false)
	public List<String> getFurnaceReportColumns(String plantId, String aopYear, String reportType) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<String> columnNames = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_FurnaceDataSet";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @aopYear = ?, @reportType = ?";

			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);
				ps.setString(3, reportType);
				

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

	@Transactional(transactionManager = "db2TransactionManager", readOnly = false)
	public List<Map<String, Object>> getFurnaceReportColumnMetadata(String plantId, String aopYear, String reportType) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> columnMetadata = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_FurnaceDataSet";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @aopYear = ?, @reportType = ?";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);
				ps.setString(3, reportType);
				

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
	
	@Override
	public AOPMessageVM getRunLengthDataSet(String plantId, String year, String reportType){
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<Object[]> results = getRunLengthDataSetData(plantId, year, reportType);

			List<String> columnNames = getRunLengthDataSetColumns(plantId, year, reportType);

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
			data.put("columns", getRunLengthDataSetColumnMetadata(plantId, year, reportType));

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

	public List<Object[]> getRunLengthDataSetData(String plantId, String aopYear, String reportType) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			String storedProcedure = vertical.getName() + "_" + site.getName() + "_RunLengthDataSet";

			String sql = "EXEC " + storedProcedure
					+ " @plantId = :plantId, @aopYear = :aopYear, @reportType = :reportType";

			Query query = entityManager.createNativeQuery(sql);

			query.setParameter("plantId", plantId);
			query.setParameter("aopYear", aopYear);
			query.setParameter("reportType", reportType);
			

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	public List<String> getRunLengthDataSetColumns(String plantId, String aopYear, String reportType) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<String> columnNames = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_RunLengthDataSet";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @aopYear = ?, @reportType = ?";

			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);
				ps.setString(3, reportType);
				

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

	public List<Map<String, Object>> getRunLengthDataSetColumnMetadata(String plantId, String aopYear, String reportType) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> columnMetadata = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_RunLengthDataSet";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @aopYear = ?, @reportType = ?";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, aopYear);
				ps.setString(3, reportType);
				

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
	@Override
	public AOPMessageVM calculateMonthWiseRawData(String year, String plantId) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId)).get();
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
			String storedProcedure = vertical.getName() + "_" + site.getName() + "_LoadAllReportsData";
			Integer result=  executeDynamicUpdateProcedure(storedProcedure, plantId, year);
			aopMessageVM.setCode(200);
	        aopMessageVM.setMessage("SP Executed successfully");
	        aopMessageVM.setData(result);
	        return aopMessageVM;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return aopMessageVM;
	}
	public int executeDynamicUpdateProcedure(String procedureName, String plantId,
			String year) {
		try {
			
			String callSql = "{call " + procedureName + "(?, ?)}";

	        try (Connection connection = dataSource.getConnection();
	             CallableStatement stmt = connection.prepareCall(callSql)) {

	            // Set parameters in the correct order
	            stmt.setString(1, plantId); // @finYear
	          	stmt.setString(2, year); // @siteId

	            // Execute the stored procedure
	            int rowsAffected = stmt.executeUpdate();

	            // Optional: commit if auto-commit is off
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
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Override
	@Transactional(transactionManager = "db2TransactionManager", readOnly = false)
	public AOPMessageVM getMonthWiseRawDataByMethod(String plantId,String year,String mode,String method) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<Object[]> results = getMonthWiseData(plantId, year, mode,method);
			List<String> columnNames = getMonthWiseDataColumns(plantId, year, mode,method);

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
			data.put("columns", getMonthWiseDataColumnMetadata(plantId, year, mode,method));

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

	@Transactional(transactionManager = "db2TransactionManager", readOnly = false)
	public List<Object[]> getMonthWiseData(String plantId,String year,String mode,String method) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			String storedProcedure = vertical.getName() + "_" + site.getName() + "_getMonthwiseRawdataByMethod";

			String sql = "EXEC " + storedProcedure
					+ " @plantId = :plantId, @year = :year, @mode = :mode, @method = :method";

			Query query = entityManager.createNativeQuery(sql);

			query.setParameter("plantId", plantId);
			query.setParameter("year", year);
			query.setParameter("mode", mode);
			query.setParameter("method", method);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Transactional(transactionManager = "db2TransactionManager", readOnly = false)
	public List<String> getMonthWiseDataColumns(String plantId,String year,String mode,String method) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<String> columnNames = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_getMonthwiseRawdataByMethod";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @year = ?, @mode = ?, @method = ?";

			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, year);
				ps.setString(3, mode);
				ps.setString(4, method);
				
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

	@Transactional(transactionManager = "db2TransactionManager", readOnly = false)
	public List<Map<String, Object>> getMonthWiseDataColumnMetadata(String plantId,String year,String mode,String method) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> columnMetadata = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String storedProcedure = vertical.getName() + "_" + site.getName() + "_getMonthwiseRawdataByMethod";
			String sql = "EXEC " + storedProcedure
					+ " @plantId = ?, @year = ?, @mode = ?, @method = ?";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, plantId);
				ps.setString(2, year);
				ps.setString(3, mode);
				ps.setString(4, method);
				

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


}
