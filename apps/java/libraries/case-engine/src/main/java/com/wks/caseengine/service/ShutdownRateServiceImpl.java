package com.wks.caseengine.service;

import java.sql.CallableStatement;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.message.vm.AOPMessageVM;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class ShutdownRateServiceImpl implements ShutdownRateService {

	@PersistenceContext
	private EntityManager entityManager;
	
	@Autowired
	private DataSource dataSource;

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
			
			String sql = "{call [dbo].[PE_C2_GetShutdownRate](?, ?)}";
			
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
			
			String sql = "{call [dbo].[PE_C2_GetShutdownRate](?, ?)}";
			
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
}
