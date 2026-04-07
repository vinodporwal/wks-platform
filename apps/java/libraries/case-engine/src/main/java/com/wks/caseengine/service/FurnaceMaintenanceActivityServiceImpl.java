package com.wks.caseengine.service;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.dto.FurnaceMaintenanceActivityDTO;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class FurnaceMaintenanceActivityServiceImpl implements FurnaceMaintenanceActivityService {

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
	

	@Override
	@Transactional
	public AOPMessageVM getFurnaceMaintenanceActivities(String plantId, String aopYear) {
		AOPMessageVM response = new AOPMessageVM();
		
		try {
			// Get data and columns dynamically
			List<Map<String, Object>> dataList = getFurnaceMaintenanceData(plantId, aopYear);
			List<Map<String, Object>> columnMetadata = getFurnaceMaintenanceColumnMetadata(plantId, aopYear);
			
			Map<String, Object> finalData = new HashMap<>();
			finalData.put("data", dataList);
			finalData.put("columns", columnMetadata);
			
			response.setCode(200);
			response.setMessage("Data fetched successfully");
			response.setData(finalData);
			
		} catch (Exception e) {
			response.setCode(200);
			response.setMessage("Failed to retrieve furnace maintenance activities: " + e.getMessage());
			e.printStackTrace();
		}
		
		return response;
	}
	
	private List<Map<String, Object>> getFurnaceMaintenanceData(String plantId, String aopYear) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> dataList = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId)).orElseThrow();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
			Sites site = siteRepository.findById(plant.getSiteFkId()).orElseThrow();
			String procedureName=vertical.getName()+"_"+site.getName()+"_GetFurnaceMaitenanceActivityForScrn";
			String sql = "{call " + procedureName + "(?, ?)}";
			
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
	
	private List<Map<String, Object>> getFurnaceMaintenanceColumnMetadata(String plantId, String aopYear) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> columnMetadata = new ArrayList<>();
			Plants plant = plantsRepository.findById(UUID.fromString(plantId)).orElseThrow();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
			Sites site = siteRepository.findById(plant.getSiteFkId()).orElseThrow();
			String procedureName=vertical.getName()+"_"+site.getName()+"_GetFurnaceMaitenanceActivityForScrn";
			String sql = "{call " + procedureName + "(?, ?)}";
			
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
			case "Id":
				return "Id";
			case "Name":
				return "Name";
			case "DisplayName":
				return "DisplayName";
			case "IBR_SD":
				return "IBR Start Date";
			case "IBR_ED":
				return "IBR End Date";
			case "TA_SD":
				return "TA_SD";
			case "TA_ED":
				return "TA_ED";
			case "ShutDown_SD":
				return "Shutdown Start Date";
			case "ShutDown_ED":
				return "Shutdown End Date";
			case "ActualRunLength":
				return "ActualRunLength";
			case "Reduction":
				return "% Reduction";
			case "Post_CR_Days":
				return "Post_CR_Days";
			case "Pre_CR_Days":
				return "Pre_CR_Days";
			case "IsCR":
				return "Is Coil Replacement";
			case "Plant_FK_Id":
				return "Plant_FK_Id";
			case "AOPYear":
				return "AOPYear";
			case "Remarks":
				return "Remarks";
			case "DisplaySeq":
				return "DisplaySeq";
			case "isEditable":
				return "isEditable";
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
