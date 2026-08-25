package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.Session;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.repository.query.Param;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.BusinessDemandDataDTO;
import com.wks.caseengine.dto.NormAttributeTransactionsDTO;
import com.wks.caseengine.dto.ShutdownHistoryConfigDTO;
import com.wks.caseengine.dto.SlowdownHistoryConfigDTO;
import com.wks.caseengine.entity.AopCalculation;
import com.wks.caseengine.entity.NormAttributeTransactions;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.ScreenMapping;
import com.wks.caseengine.entity.ShutdownHistoryConfig;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.SlowdownConsumption;
import com.wks.caseengine.entity.SlowdownHistoryConfig;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.AopCalculationRepository;
import com.wks.caseengine.repository.NormAttributeTransactionsRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.ScreenMappingRepository;
import com.wks.caseengine.repository.ShutdownHistoryConfigRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.SlowdownHistoryConfigRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class ShutdownHistoryServiceImpl implements ShutdownHistoryService{
	
	@PersistenceContext
	private EntityManager entityManager;
	
	@Autowired
	private PlantsRepository plantsRepository;
	
	@Autowired
	private SiteRepository siteRepository;
	
	@Autowired
	private ScreenMappingRepository screenMappingRepository;
	
	@Autowired
	private AopCalculationRepository aopCalculationRepository;
	
	@Autowired
	private ShutdownHistoryConfigRepository shutdownHistoryConfigRepository;

	@Autowired
	private VerticalsRepository verticalRepository;

	@Autowired
	private SlowdownHistoryConfigRepository slowdownHistoryConfigRepository;
	
	@Autowired
	private NormAttributeTransactionsRepository normAttributeTransactionsRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ConfigurationService configurationService;

	@Override
	public AOPMessageVM getShutdownHistory(String plantId, String year) {
		List<ShutdownHistoryConfigDTO> shutdownHistoryConfigDTOs=new ArrayList<ShutdownHistoryConfigDTO>();
		try {
			List<ShutdownHistoryConfig> shutdownHistoryConfigList=shutdownHistoryConfigRepository.findByAopYear(year,UUID.fromString(plantId));
			for(ShutdownHistoryConfig shutdownHistoryConfig:shutdownHistoryConfigList) {
				ShutdownHistoryConfigDTO shutdownHistoryConfigDTO= new ShutdownHistoryConfigDTO();
				shutdownHistoryConfigDTO.setId(shutdownHistoryConfig.getId());
				shutdownHistoryConfigDTO.setMonth(shutdownHistoryConfig.getMonth());
				shutdownHistoryConfigDTO.setRemark(shutdownHistoryConfig.getRemark());
				shutdownHistoryConfigDTO.setAopYear(shutdownHistoryConfig.getAopYear());
				shutdownHistoryConfigDTO.setYear(shutdownHistoryConfig.getYear());
				shutdownHistoryConfigDTO.setPlantId(shutdownHistoryConfig.getPlantFKId().toString());
				shutdownHistoryConfigDTO.setTypeOfSD(shutdownHistoryConfig.getTypeOfSD());
				shutdownHistoryConfigDTOs.add(shutdownHistoryConfigDTO);
			}
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to fetch data", ex);
		}
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		aopMessageVM.setCode(200);
		aopMessageVM.setData(shutdownHistoryConfigDTOs);
		aopMessageVM.setMessage("Data Fetched successfully");
		return aopMessageVM;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public List<ShutdownHistoryConfigDTO> saveShutdownHistory(String year, String plantFKId,
			List<ShutdownHistoryConfigDTO> shutdownHistoryConfigDTOs) {
		try {
			List<ShutdownHistoryConfigDTO> failedRecords = new ArrayList<ShutdownHistoryConfigDTO>();
			UUID plantId = UUID.fromString(plantFKId);
			String verticalName = plantsRepository.findVerticalNameByPlantId(plantId);
			Plants plant = plantsRepository.findById(plantId).orElseThrow();
			Sites site = siteRepository.findById(plant.getSiteFkId()).orElseThrow();

			Set<Integer> validTypeOfSDValues = fetchValidTypeOfSDValues(plantFKId, year);
			Set<String> validYears = buildValidYearSet(year);

		for (ShutdownHistoryConfigDTO shutdownHistoryConfigDTO : shutdownHistoryConfigDTOs) {

			 validateShutdownHistoryMandatoryFields(shutdownHistoryConfigDTO);
			 validateShutdownHistoryYear(shutdownHistoryConfigDTO, validYears);
			 validateShutdownHistoryTypeOfSD(shutdownHistoryConfigDTO, validTypeOfSDValues);

				 if("Failed".equals(shutdownHistoryConfigDTO.getSaveStatus())) {
					failedRecords.add(shutdownHistoryConfigDTO);
					continue;
				 }
				

				ShutdownHistoryConfig shutdownHistoryConfig = null;
				if (shutdownHistoryConfigDTO.getId() != null) {
					Optional<ShutdownHistoryConfig> shutdownHistoryConfigOpt = shutdownHistoryConfigRepository.findById(shutdownHistoryConfigDTO.getId());
					if (shutdownHistoryConfigOpt.isPresent()) {
						shutdownHistoryConfig = shutdownHistoryConfigOpt.get();

						 validateShutdownHistoryRemark(shutdownHistoryConfig, shutdownHistoryConfigDTO);

						 if("Failed".equals(shutdownHistoryConfigDTO.getSaveStatus())) {
							failedRecords.add(shutdownHistoryConfigDTO);
							continue;
						 }
					

						shutdownHistoryConfig.setModifiedOn(new Date());
					}
				} else {
					shutdownHistoryConfig = new ShutdownHistoryConfig();
					shutdownHistoryConfig.setCreatedOn(new Date());
				}
				shutdownHistoryConfig.setAopYear(shutdownHistoryConfigDTO.getAopYear());
				shutdownHistoryConfig.setModifiedBy(Utility.getUserName());
				shutdownHistoryConfig.setMonth(shutdownHistoryConfigDTO.getMonth());
				shutdownHistoryConfig.setRemark(shutdownHistoryConfigDTO.getRemark());
				shutdownHistoryConfig.setYear(shutdownHistoryConfigDTO.getYear());
				shutdownHistoryConfig.setPlantFKId(plantId);
				shutdownHistoryConfig.setTypeOfSD(shutdownHistoryConfigDTO.getTypeOfSD());
				shutdownHistoryConfigRepository.save(shutdownHistoryConfig);
			}
			
			List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("shutdown-history-config");
			for (ScreenMapping screenMapping : screenMappingList) {
				AopCalculation aopCalculation = new AopCalculation();
				aopCalculation.setAopYear(year);
				aopCalculation.setIsChanged(true);
				aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
				aopCalculation.setPlantId(UUID.fromString(plantFKId));
				aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
				aopCalculationRepository.save(aopCalculation);
			}

			return failedRecords;
		} catch (Exception ex) {
			ex.printStackTrace();
			
			throw new RuntimeException("Failed to save data", ex);
		}
	}

	@Override
	public AOPMessageVM deleteShutdownHistory(UUID id) {
		Optional<ShutdownHistoryConfig> shutdownHistoryConfigOpt=shutdownHistoryConfigRepository.findById(id);
		if(shutdownHistoryConfigOpt.isPresent()) {
			ShutdownHistoryConfig shutdownHistoryConfig= shutdownHistoryConfigOpt.get();
			shutdownHistoryConfigRepository.delete(shutdownHistoryConfig);
		}
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		aopMessageVM.setCode(200);
		aopMessageVM.setData(id);
		aopMessageVM.setMessage("Data deleted successfully");
		return aopMessageVM;
	}
	
	@Override
	public AOPMessageVM getTypeOfSD(String plantId, String year) {
		try {
			String verticalName = plantsRepository.findVerticalNameByPlantId(UUID.fromString(plantId));
			String view="vwScrn"+verticalName+"TypeOfSD";
			List<Object[]> obj=getTypeOfSDData(view);
			List<Map<String,Object>> maps=new ArrayList<>();
			for (Object[] row : obj) {
				
				Map<String,Object> map=new HashMap<>();
				map.put("name", row[0] != null ? row[0].toString() : null);
				map.put("value", row[1] != null ? Integer.parseInt(row[1].toString()) : null);
				maps.add(map);
				
			}
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			aopMessageVM.setCode(200);
			aopMessageVM.setData(maps);
			aopMessageVM.setMessage("Data fetched successfully");
			
			// TODO Auto-generated method stub
			return aopMessageVM;
		}catch (Exception ex) {
			ex.printStackTrace();
			
			throw new RuntimeException("Failed to save data", ex);
		}
		
	}

	@Override
	public AOPMessageVM getLineDetails(String plantId, String year) {
		try {
			String verticalName = plantsRepository.findVerticalNameByPlantId(UUID.fromString(plantId));
			String view="vwScrn"+verticalName+"GetLineDetails";
			List<Object[]> obj=getLineDetailsData(view,plantId);
			List<Map<String,Object>> maps=new ArrayList<>();
			for (Object[] row : obj) {
				
				Map<String,Object> map=new HashMap<>();
				map.put("id", row[0] != null ? row[0].toString() : null);
				map.put("name", row[1] != null ? row[1].toString() : null);
				map.put("displayName", row[2] != null ? row[2].toString() : null);
				map.put("plantId", row[3] != null ? (row[3].toString()) : null);
				maps.add(map);
			}
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			aopMessageVM.setCode(200);
			aopMessageVM.setData(maps);
			aopMessageVM.setMessage("Data fetched successfully");
			
			// TODO Auto-generated method stub
			return aopMessageVM;
		}catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to save data", ex);
		}
		
	}

	public List<Object[]> getTypeOfSDData(String viewName) {
		try {
			String sql = "SELECT * from "+ viewName;

			Query query = entityManager.createNativeQuery(sql);
			
			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}
	
	public List<Object[]> getLineDetailsData(String viewName,String plantId) {
		try {
			String sql = "SELECT * from "+ viewName+" where PlantId= :plantId";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("plantId", plantId);
			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public List<SlowdownHistoryConfigDTO> saveSlowdownHistory(String year, String plantFKId,
			List<SlowdownHistoryConfigDTO> dtos) {

		List<SlowdownHistoryConfigDTO> failedRecords = new ArrayList<>();
		try {
			UUID plantId = UUID.fromString(plantFKId);

			for (SlowdownHistoryConfigDTO dto : dtos) {

				if ("Failed".equals(dto.getSaveStatus())) {
					failedRecords.add(dto);
					continue;
				}

				validateSlowdownMandatoryFields(dto);

				if ("Failed".equals(dto.getSaveStatus())) {
					failedRecords.add(dto);
					continue;
				}

			//	validateSlowdownDates(dto, year);

				if ("Failed".equals(dto.getSaveStatus())) {
					failedRecords.add(dto);
					continue;
				}

				SlowdownHistoryConfig entity = null;

				if (dto.getId() != null) {
					Optional<SlowdownHistoryConfig> opt = slowdownHistoryConfigRepository.findById(dto.getId());
					if (opt.isPresent()) {
						entity = opt.get();

						validateSlowdownRemark(entity, dto);

						if ("Failed".equals(dto.getSaveStatus())) {
							failedRecords.add(dto);
							continue;
						}
					} else {
						entity = new SlowdownHistoryConfig();
					}
				} else {
					entity = new SlowdownHistoryConfig();
				}

				entity.setDescription(dto.getDescription());
				entity.setMaintStartDateTime(dto.getMaintStartDateTime());
				entity.setMaintEndDateTime(dto.getMaintEndDateTime());
				entity.setDurationInMins(dto.getDurationInMins());
				entity.setMaintForMonth(dto.getMaintForMonth());
				entity.setAuditYear(year);
				entity.setRate(dto.getRate());
				entity.setRemarks(dto.getRemarks());
				entity.setUpdatedOn(new Date());
				entity.setUpdatedBy(Utility.getUserName());
				entity.setPlantFkId(plantId);

				slowdownHistoryConfigRepository.save(entity);
			}

			return failedRecords;

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Save failed", e);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public AOPMessageVM getSlowdownHistory(String plantId, String year) {

		AOPMessageVM aopMessageVM = new AOPMessageVM();
		List<Map<String, Object>> slowdownList = new ArrayList<>();

		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));

			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String procedureName = vertical.getName() + "_GetSlowdownHistoryConfig";

			List<Object[]> results = getData(plantId, year, procedureName);

			for (Object[] row : results) {
				Map<String, Object> map = new HashMap<>();
				map.put("id", row[0] != null ? row[0].toString() : null);
				map.put("description", row[1] != null ? row[1].toString() : null);
				map.put("maintStartDateTime", row[2]);
				map.put("maintEndDateTime", row[3]);
				map.put("durationInMins", row[4]);
				map.put("maintForMonth", row[5]);
				map.put("auditYear", row[6]);
				map.put("rate", row[7]);
				map.put("remarks", row[8]);
				map.put("updatedOn", row[9]);
				map.put("updatedBy", row[10]);
				map.put("plantFkId", row[11] != null ? row[11].toString() : null);

				slowdownList.add(map);
			}

			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data fetched successfully");
			aopMessageVM.setData(slowdownList);
			return aopMessageVM;

		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@SuppressWarnings("unchecked")
	public List<Object[]> getData(String plantId, String year, String procedureName) {

		String sql = "EXEC " + procedureName + " @plantId = :plantId, @aopyear = :aopyear";

		return (List<Object[]>) entityManager.createNativeQuery(sql)
				.setParameter("plantId", plantId)
				.setParameter("aopyear", year)
				.getResultList();
	}
	
	@SuppressWarnings("unchecked")
	public List<Object[]> getDataPTA(String plantId, String year, String procedureName) {

		String sql = "EXEC " + procedureName + " @PlantId = :plantId, @AOPYear = :aopyear";

		return (List<Object[]>) entityManager.createNativeQuery(sql)
				.setParameter("plantId", plantId)
				.setParameter("aopyear", year)
				.getResultList();
	}

	
	@Override
	public AOPMessageVM deleteSlowdownHistory(UUID id) {
		Optional<SlowdownHistoryConfig> slowdownHistoryConfigOpt = slowdownHistoryConfigRepository.findById(id);
		if (slowdownHistoryConfigOpt.isPresent()) {
			SlowdownHistoryConfig slowdownHistoryConfig = slowdownHistoryConfigOpt.get();
			slowdownHistoryConfigRepository.delete(slowdownHistoryConfig);
		}
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		aopMessageVM.setCode(200);
		aopMessageVM.setData(id);
		aopMessageVM.setMessage("Data deleted successfully");
		return aopMessageVM;
	}

	@Override
	public AOPMessageVM getShutdownHistoryPTA(String plantId, String year) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<Object[]> results = getDataPTA(plantId, year, getShutdownHistoryPTAProcedureName(plantId));
			List<String> columnNames = getShutdownHistoryPTADataColumns(plantId, year);

			List<Map<String, Object>> resultList = new ArrayList<>();
			for (Object[] row : results) {
				Map<String, Object> rowMap = new LinkedHashMap<>();
				for (int i = 0; i < columnNames.size() && i < row.length; i++) {
					rowMap.put(columnNames.get(i), row[i]);
				}
				resultList.add(rowMap);
			}

			Map<String, Object> data = new HashMap<>();
			data.put("data", resultList);
			data.put("columns", getShutdownHistoryPTAColumnMetadata(plantId, year));

			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data fetched successfully");
			aopMessageVM.setData(data);
			return aopMessageVM;

		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	private String getShutdownHistoryPTAProcedureName(String plantId) {
		Plants plant = plantsRepository.findById(UUID.fromString(plantId))
				.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
		Sites site = siteRepository.findById(plant.getSiteFkId())
				.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
		Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
				.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
		return vertical.getName() + "_" + site.getName() + "_GetShutdownConfiguration";
	}

	public List<String> getShutdownHistoryPTADataColumns(String plantId, String year) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<String> columnNames = new ArrayList<>();
			String procedureName = getShutdownHistoryPTAProcedureName(plantId);
			String sql = "EXEC " + procedureName + " @PlantId = ?, @AOPYear = ?";
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

	public List<Map<String, Object>> getShutdownHistoryPTAColumnMetadata(String plantId, String year) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			List<Map<String, Object>> columnMetadata = new ArrayList<>();
			String procedureName = getShutdownHistoryPTAProcedureName(plantId);
			String sql = "EXEC " + procedureName + " @PlantId = ?, @AOPYear = ?";
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
		return columnName == null ? "" : columnName.replace("_", " ");
	}

	private String getFrontendType(String sqlTypeName) {
		if (sqlTypeName == null) return "string";
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
	public AOPMessageVM saveHistoryPTA(String plantId, String year,
			List<NormAttributeTransactionsDTO> normAttributeTransactionsDTOList) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		
		List<NormAttributeTransactions> normAttributeTransactionsList = new ArrayList<NormAttributeTransactions>();
		try {
			for(NormAttributeTransactionsDTO normAttributeTransactionsDTO:normAttributeTransactionsDTOList) {
				String rawDesc = normAttributeTransactionsDTO.getDescription();
				String attributeValue = normAttributeTransactionsDTO.getAttributeValue();
				if (attributeValue == null) {
					attributeValue = "";
				}
				Plants plant = plantsRepository.findById(UUID.fromString(plantId)).orElseThrow();
				Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
				Sites site = siteRepository.findById(plant.getSiteFkId()).get();
				
				String viewName = vertical.getName() + site.getName() + "vwScrnShutdown";
				List<Object[]> results = getDescriptionIdBySite(site.getId(),rawDesc, viewName);
				UUID uuid = Optional.ofNullable(results)
					    .filter(res -> !res.isEmpty() && res.get(0).length > 0)
					    .map(res -> res.get(0)[0])
					    .map(val -> (val instanceof UUID) ? (UUID) val : UUID.fromString(val.toString()))
					    .orElse(null);				
				List<NormAttributeTransactions> normAttributeTransactions =normAttributeTransactionsRepository.findByAuditYearAndIds(year,normAttributeTransactionsDTO.getNormParameterFKId(),uuid);
				if(normAttributeTransactions!=null && normAttributeTransactions.size()>0) {
					for(NormAttributeTransactions normAttributeTransaction:normAttributeTransactions) {
						normAttributeTransaction.setAttributeValue(attributeValue);
						normAttributeTransaction.setModifiedOn(new Date());
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsList.add(normAttributeTransaction);
					}
				}else {
					for(int i=1;i<13;i++) {
						NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
						normAttributeTransaction.setAopMonth(i);
						normAttributeTransaction.setAttributeValue(attributeValue);
						normAttributeTransaction.setAuditYear(year);
						normAttributeTransaction.setCreatedOn(new Date());
						normAttributeTransaction.setNormParameterFKId(normAttributeTransactionsDTO.getNormParameterFKId());
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransaction.setShutdownTypeId(uuid);
						normAttributeTransactionsList.add(normAttributeTransaction);
					}
				}

				List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("slowdown-norms");
				for (ScreenMapping screenMapping : screenMappingList) {
					AopCalculation aopCalculation = new AopCalculation();
					aopCalculation.setAopYear(year);
					aopCalculation.setIsChanged(true);
					aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
					aopCalculation.setPlantId(UUID.fromString(plantId));
					aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
					aopCalculationRepository.save(aopCalculation);
				}
				List<ScreenMapping> screenMappingList1 = screenMappingRepository.findByDependentScreen("shutdown-plan");
				for (ScreenMapping screenMapping : screenMappingList1) {
					AopCalculation aopCalculation = new AopCalculation();
					aopCalculation.setAopYear(year);
					aopCalculation.setIsChanged(true);
					aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
					aopCalculation.setPlantId(UUID.fromString(plantId));
					aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
					aopCalculationRepository.save(aopCalculation);
				}

			}
		}catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to save/update data", ex);
		}
		normAttributeTransactionsRepository.saveAll(normAttributeTransactionsList);
		aopMessageVM.setCode(200);
		aopMessageVM.setData(normAttributeTransactionsList);
		aopMessageVM.setMessage("Data updated successfully");
		return aopMessageVM;
	}
	public List<Object[]> getDescriptionIdBySite(UUID siteId,String name, String viewName) {
		try {
			String sql = "SELECT * from " + viewName + " where Site_FK_Id = :siteId and DisplayName = :name order by DisplayOrder";
			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("siteId", siteId);
			query.setParameter("name", name);
			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Override
	public byte[] createShutdownHistoryPTAExcel(String plantId, String year) {
		try {
			List<String> columnNames = getShutdownHistoryPTADataColumns(plantId, year);
			List<Object[]> results = getDataPTA(plantId, year, getShutdownHistoryPTAProcedureName(plantId));

			try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				List<Integer> exportIndices = new ArrayList<>();
				List<String> exportColumnNames = new ArrayList<>();
				for (int i = 0; i < columnNames.size(); i++) {
					String name = columnNames.get(i);
					if (name != null && "IsEditable".equalsIgnoreCase(name.trim())) {
						continue;
					}
					exportIndices.add(i);
					exportColumnNames.add(name);
				}

				Sheet sheet = workbook.createSheet("Shutdown History PTA");
				CellStyle headerStyle = Utility.createBoldBorderedStyle(workbook);
				Row headerRow = sheet.createRow(0);
				for (int j = 0; j < exportColumnNames.size(); j++) {
					Cell cell = headerRow.createCell(j);
					cell.setCellValue(exportColumnNames.get(j));
					cell.setCellStyle(headerStyle);
				}
				int rowIdx = 1;
				for (Object[] row : results) {
					Row excelRow = sheet.createRow(rowIdx++);
					for (int j = 0; j < exportIndices.size(); j++) {
						int srcIdx = exportIndices.get(j);
						Cell cell = excelRow.createCell(j);
						Object val = (row != null && srcIdx < row.length) ? row[srcIdx] : null;
						setShutdownHistoryPTACellValue(cell, val);
					}
				}
				for (int j = 0; j < exportColumnNames.size(); j++) {
					sheet.autoSizeColumn(j);
					if (isShutdownHistoryPTAExportHiddenColumn(exportColumnNames.get(j))) {
						sheet.setColumnHidden(j, true);
					}
				}
				workbook.write(baos);
				return baos.toByteArray();
			}
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to export shutdown history PTA", ex);
		}
	}

	/** Columns still written (for import) but marked hidden in Excel. */
	private boolean isShutdownHistoryPTAExportHiddenColumn(String columnName) {
		if (columnName == null) {
			return false;
		}
		String n = columnName.trim();
		return "NormParameter_FK_Id".equalsIgnoreCase(n);
	}

	@Override
	@Transactional
	public AOPMessageVM importShutdownHistoryPTAExcel(String plantId, String year, MultipartFile file) {
		AOPMessageVM vm = new AOPMessageVM();
		try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
			Sheet sheet = workbook.getSheetAt(0);
			if (sheet == null) {
				vm.setCode(400);
				vm.setMessage("Workbook has no sheets");
				return vm;
			}
			Row headerRow = sheet.getRow(0);
			if (headerRow == null) {
				vm.setCode(400);
				vm.setMessage("Missing header row");
				return vm;
			}
			DataFormatter fmt = new DataFormatter();
			List<String> headers = new ArrayList<>();
			int lastCell = headerRow.getLastCellNum();
			for (int c = 0; c < lastCell; c++) {
				Cell cell = headerRow.getCell(c);
				headers.add(cell == null ? "" : fmt.formatCellValue(cell).trim());
			}
			while (!headers.isEmpty() && headers.get(headers.size() - 1).isEmpty()) {
				headers.remove(headers.size() - 1);
			}
			if (headers.isEmpty()) {
				vm.setCode(400);
				vm.setMessage("No column headers found");
				return vm;
			}

			List<Map<String, Object>> validPayload = new ArrayList<>();
			List<String[]> failedRawRows = new ArrayList<>();
			List<String> failedErrors = new ArrayList<>();

			int lastRow = sheet.getLastRowNum();
			for (int r = 1; r <= lastRow; r++) {
				Row row = sheet.getRow(r);
				if (row == null) {
					continue;
				}
				if (isShutdownHistoryPTARowEmpty(row, headers.size(), fmt)) {
					continue;
				}

				String[] rawValues = new String[headers.size()];
				Map<String, Object> item = new LinkedHashMap<>();
				String normIdStr = null;
				String err = null;

				for (int c = 0; c < headers.size(); c++) {
					String header = headers.get(c);
					Cell cell = row.getCell(c);
					String cellStr = cell == null ? "" : fmt.formatCellValue(cell).trim();
					rawValues[c] = cellStr;
					if (header.isEmpty()) {
						continue;
					}
					Object val = cellStr.isEmpty() ? null : cellStr;
					if (isShutdownHistoryPTANormParameterHeader(header)) {
						normIdStr = cellStr;
						item.put("normParameterFKId", val);
					} else {
						item.put(header, val);
					}
				}

				if (normIdStr == null || normIdStr.isEmpty()) {
					err = "Missing NormParameter_FK_Id / normParameterFKId";
				} else {
					try {
						UUID.fromString(normIdStr);
					} catch (IllegalArgumentException e) {
						err = "Invalid normParameterFKId UUID";
					}
				}

				if (err != null) {
					failedRawRows.add(rawValues);
					failedErrors.add(err);
				} else {
					validPayload.add(item);
				}
			}

			if (validPayload.isEmpty() && failedRawRows.isEmpty()) {
				vm.setCode(400);
				vm.setMessage("No data rows found in file");
				vm.setData(null);
				return vm;
			}

			if (!failedRawRows.isEmpty()) {
				byte[] errBytes = buildShutdownHistoryPTAErrorExcel(headers, failedRawRows, failedErrors);
				vm.setCode(400);
				vm.setMessage("Validation failed for " + failedRawRows.size() + " row(s); no data was saved");
				vm.setData(Base64.getEncoder().encodeToString(errBytes));
				return vm;
			}

			List<NormAttributeTransactionsDTO> dtoList = convertPtaPayloadToNormAttributeDtos(validPayload);
			try {
				return saveHistoryPTA(plantId, year, dtoList);
			} catch (DataIntegrityViolationException e) {
				Throwable root = e.getRootCause() != null ? e.getRootCause() : e;
				String detail = root.getMessage() != null ? root.getMessage() : e.getMessage();
				String errMsg = detail != null ? detail : "data constraint violation";
				List<String[]> saveFailedRows = new ArrayList<>();
				for (Map<String, Object> item : validPayload) {
					saveFailedRows.add(shutdownHistoryPTAPayloadRowToRawValues(item, headers));
				}
				List<String> saveErrors = new ArrayList<>(Collections.nCopies(saveFailedRows.size(), errMsg));
				byte[] errBytes = buildShutdownHistoryPTAErrorExcel(headers, saveFailedRows, saveErrors);
				vm.setCode(400);
				vm.setMessage("Save failed while importing; no data was saved. Error file attached in data (Base64).");
				vm.setData(Base64.getEncoder().encodeToString(errBytes));
				return vm;
			}

		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to import shutdown history PTA", ex);
		}
	}

	private void setShutdownHistoryPTACellValue(Cell cell, Object val) {
		if (val == null) {
			cell.setBlank();
			return;
		}
		if (val instanceof Number) {
			cell.setCellValue(((Number) val).doubleValue());
		} else if (val instanceof Date) {
			cell.setCellValue((Date) val);
		} else if (val instanceof Boolean) {
			cell.setCellValue((Boolean) val);
		} else {
			cell.setCellValue(val.toString());
		}
	}

	
	private String[] shutdownHistoryPTAPayloadRowToRawValues(Map<String, Object> item, List<String> headers) {
		String[] vals = new String[headers.size()];
		for (int c = 0; c < headers.size(); c++) {
			String h = headers.get(c);
			if (h == null || h.isEmpty()) {
				vals[c] = "";
				continue;
			}
			if (isShutdownHistoryPTANormParameterHeader(h)) {
				Object v = item.get("normParameterFKId");
				vals[c] = v != null ? v.toString() : "";
			} else {
				Object v = item.get(h);
				vals[c] = v != null ? v.toString() : "";
			}
		}
		return vals;
	}

	private boolean isShutdownHistoryPTANormParameterHeader(String header) {
		if (header == null) {
			return false;
		}
		String h = header.trim();
		return "normParameterFKId".equalsIgnoreCase(h) || "NormParameter_FK_Id".equalsIgnoreCase(h)
				|| "NormParameterFKId".equalsIgnoreCase(h);
	}

	private boolean isShutdownHistoryPTARowEmpty(Row row, int colCount, DataFormatter fmt) {
		for (int c = 0; c < colCount; c++) {
			Cell cell = row.getCell(c);
			if (cell != null && !fmt.formatCellValue(cell).trim().isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private byte[] buildShutdownHistoryPTAErrorExcel(List<String> headers, List<String[]> failedRawRows,
			List<String> failedErrors) {
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("Errors");
			CellStyle headerStyle = Utility.createBoldBorderedStyle(workbook);
			Row headerRow = sheet.createRow(0);
			for (int i = 0; i < headers.size(); i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(headers.get(i));
				cell.setCellStyle(headerStyle);
			}
			Cell errHeader = headerRow.createCell(headers.size());
			errHeader.setCellValue("errDescription");
			errHeader.setCellStyle(headerStyle);
			for (int r = 0; r < failedRawRows.size(); r++) {
				Row excelRow = sheet.createRow(r + 1);
				String[] vals = failedRawRows.get(r);
				for (int c = 0; c < headers.size(); c++) {
					excelRow.createCell(c).setCellValue(vals[c] != null ? vals[c] : "");
				}
				excelRow.createCell(headers.size()).setCellValue(failedErrors.get(r));
			}
			for (int i = 0; i <= headers.size(); i++) {
				sheet.autoSizeColumn(i);
			}
			workbook.write(baos);
			return baos.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException("Failed to build error workbook", e);
		}
	}

	/**
	 * Same mapping as {@link com.wks.caseengine.rest.server.ShutdownHistoryController#saveHistoryPTA}.
	 */
	private List<NormAttributeTransactionsDTO> convertPtaPayloadToNormAttributeDtos(List<Map<String, Object>> payload) {
		List<NormAttributeTransactionsDTO> dtoList = new ArrayList<>();
		for (Map<String, Object> item : payload) {
			UUID normParameterId = UUID.fromString(item.get("normParameterFKId").toString());
			for (Map.Entry<String, Object> entry : item.entrySet()) {
				String key = entry.getKey();
				if (!"normParameterFKId".equals(key)) {
					Object value = entry.getValue();
					NormAttributeTransactionsDTO dto = new NormAttributeTransactionsDTO();
					dto.setNormParameterFKId(normParameterId);
					dto.setDescription(key);
					// DB column AttributeValue is NOT NULL ? never leave null on import
					String attr = value != null ? value.toString() : "";
					dto.setAttributeValue(attr);
					dtoList.add(dto);
				}
			}
		}
		return dtoList;
	}

	@Override
	@Transactional(readOnly = true)
	public AOPMessageVM getShutdownHistoryConfig(String plantId, String year) {

		AOPMessageVM aopMessageVM = new AOPMessageVM();
		List<Map<String, Object>> shutdownHistoryConfigList = new ArrayList<>();

		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));

			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String procedureName = vertical.getName() + "_" + site.getName() + "_GetShutdownHistoryConfig";

			List<Object[]> results = getData(plantId, year, procedureName);

			for (Object[] row : results) {
				Map<String, Object> map = new HashMap<>();
				map.put("Id", row[0] != null ? row[0].toString() : null);
				map.put("ShutdownType", row[1] != null ? row[1].toString() : null);
				map.put("FromDate", row[2]);
				map.put("ToDate", row[3]);
				map.put("Remarks", row[4]);
				map.put("AopYear", row[5]);
				map.put("Plant_FK_Id", row[6] != null ? row[6].toString() : null);
				map.put("ModifiedOn", row[7]);
				map.put("ModifiedBy", row[8]);
				map.put("IsEditable", row[9]);
				map.put("IsVisible", row[10]);

				shutdownHistoryConfigList.add(map);
			}

			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data fetched successfully");
			aopMessageVM.setData(shutdownHistoryConfigList);
			return aopMessageVM;

		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Override
	public AOPMessageVM saveShutdownHistoryConfig(List<Map<String, Object>> shutdownHistoryConfigList, List<String[]> saveFailedRawRows, List<String> saveFailedErrors) {
		String modifiedBy = Utility.getUserName();
		AOPMessageVM aopMessageVM = new AOPMessageVM();

		if(saveFailedRawRows == null || saveFailedErrors == null) { 

			saveFailedRawRows = new ArrayList<>();
			saveFailedErrors = new ArrayList<>();
		}
	try {

	for (Map<String, Object> shutdownHistoryConfig : shutdownHistoryConfigList) {
		String Id = shutdownHistoryConfig.get("Id") != null ? shutdownHistoryConfig.get("Id").toString() : null;

	String dateRangeValidationError = validateShutdownHistoryDateRange(shutdownHistoryConfig);
	if(dateRangeValidationError != null) {
		saveFailedRawRows.add(shutdownConfigPayloadToRaw(shutdownHistoryConfig));
		saveFailedErrors.add(dateRangeValidationError);
		continue;
	}

		if(Id == null || Id.isEmpty()) { 

			jdbcTemplate.update("INSERT INTO ShutdownHistoryConfig (Id, ShutdownType, FromDate, ToDate, Remarks, AopYear, Plant_FK_Id, IsEditable, IsVisible) VALUES (NewId(), ?, ?, ?, ?, ?, ?, 1, 1)", shutdownHistoryConfig.get("ShutdownType"), shutdownHistoryConfig.get("FromDate"), shutdownHistoryConfig.get("ToDate"), shutdownHistoryConfig.get("Remarks"), shutdownHistoryConfig.get("AopYear"), shutdownHistoryConfig.get("Plant_FK_Id"));

			continue;
		}

		String remarkValidationError = validateRemark(Id, shutdownHistoryConfig);
		if (remarkValidationError != null) {
			saveFailedRawRows.add(shutdownConfigPayloadToRaw(shutdownHistoryConfig));
			saveFailedErrors.add(remarkValidationError);
			continue;
		}

			jdbcTemplate.update("UPDATE ShutdownHistoryConfig SET ShutdownType = ?, FromDate = ?, ToDate = ?, Remarks = ?, ModifiedBy = ?, ModifiedOn = ? WHERE Id = ?", shutdownHistoryConfig.get("ShutdownType"), shutdownHistoryConfig.get("FromDate"), shutdownHistoryConfig.get("ToDate"), shutdownHistoryConfig.get("Remarks"), modifiedBy, new Date(), Id);
	}

		if (!saveFailedErrors.isEmpty()) {
			aopMessageVM.setCode(400);
			aopMessageVM.setMessage(String.join("; ", saveFailedErrors));
			return aopMessageVM;
		}

		aopMessageVM.setCode(200);
		aopMessageVM.setMessage("Data saved successfully");
		return aopMessageVM;
	} catch (Exception ex) {
		ex.printStackTrace();
		throw new RuntimeException("Failed to save data", ex);
	}
}

	@Override
	@Transactional
	public AOPMessageVM deleteShutdownHistoryConfig(String id) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			jdbcTemplate.update("DELETE FROM ShutdownHistoryConfig WHERE Id = ?", id);
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data deleted successfully");
			return aopMessageVM;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to delete data", ex);
		}
	}

	// --- Shutdown History Config – Export Excel -----------------------------------

	@Override
	public byte[] createShutdownHistoryConfigExcel(String plantId, String year) {
		try {
			AOPMessageVM result = getShutdownHistoryConfig(plantId, year);
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> dataList = (List<Map<String, Object>>) result.getData();

			try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				CellStyle headerStyle = Utility.createBoldBorderedStyle(workbook);
				CellStyle borderStyle = Utility.createBorderedStyle(workbook);

				// Visible columns (cols 0-3) then hidden identity columns (cols 4-6)
				List<String> allHeaders = Arrays.asList(
						"Shutdown Type", "SD - From", "SD - To", "Remark",
						"Id", "AopYear", "Plant_FK_Id");
				int hiddenFromCol = 4;

				Sheet sheet = workbook.createSheet("Shutdown History Config");
				Row headerRow = sheet.createRow(0);
				for (int i = 0; i < allHeaders.size(); i++) {
					Cell cell = headerRow.createCell(i);
					cell.setCellValue(allHeaders.get(i));
					cell.setCellStyle(headerStyle);
				}

				SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
				int rowIdx = 1;
				for (Map<String, Object> item : dataList) {
					Row row = sheet.createRow(rowIdx++);

					// Col 0 – Shutdown Type
					Cell c0 = row.createCell(0);
					c0.setCellValue(item.get("ShutdownType") != null ? item.get("ShutdownType").toString() : "");
					c0.setCellStyle(borderStyle);

					// Col 1 – SD - From
					Cell c1 = row.createCell(1);
					c1.setCellValue(formatShutdownConfigDate(item.get("FromDate"), sdf));
					c1.setCellStyle(borderStyle);

					// Col 2 – SD - To
					Cell c2 = row.createCell(2);
					c2.setCellValue(formatShutdownConfigDate(item.get("ToDate"), sdf));
					c2.setCellStyle(borderStyle);

					// Col 3 – Remark
					Cell c3 = row.createCell(3);
					c3.setCellValue(item.get("Remarks") != null ? item.get("Remarks").toString() : "");
					c3.setCellStyle(borderStyle);

					// Col 4 – Id (hidden, used during import for updates)
					Cell c4 = row.createCell(4);
					c4.setCellValue(item.get("Id") != null ? item.get("Id").toString() : "");
					c4.setCellStyle(borderStyle);

					// Col 5 – AopYear (hidden)
					Cell c5 = row.createCell(5);
					c5.setCellValue(item.get("AopYear") != null ? item.get("AopYear").toString() : "");
					c5.setCellStyle(borderStyle);

					// Col 6 – Plant_FK_Id (hidden)
					Cell c6 = row.createCell(6);
					c6.setCellValue(item.get("Plant_FK_Id") != null ? item.get("Plant_FK_Id").toString() : "");
					c6.setCellStyle(borderStyle);
				}

				for (int i = 0; i < hiddenFromCol; i++) {
					sheet.autoSizeColumn(i);
				}
				for (int i = hiddenFromCol; i < allHeaders.size(); i++) {
					sheet.autoSizeColumn(i);
					sheet.setColumnHidden(i, true);
				}

				workbook.write(baos);
				return baos.toByteArray();
			}
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to export shutdown history config", ex);
		}
	}

	private String formatShutdownConfigDate(Object dateVal, SimpleDateFormat sdf) {
		if (dateVal == null) return "";
		if (dateVal instanceof java.util.Date) return sdf.format((java.util.Date) dateVal);
		return dateVal.toString();
	}

	// --- Shutdown History Config – Import Excel -----------------------------------

	@Override
	@Transactional
	public AOPMessageVM importShutdownHistoryConfigExcel(MultipartFile file, String plantId, String year) {
		if (file.isEmpty() || (file.getOriginalFilename() != null && !file.getOriginalFilename().endsWith(".xlsx"))) {
			throw new IllegalArgumentException("Invalid or empty Excel file.");
		}
		try {
			List<String> headers = Arrays.asList(
					"Shutdown Type", "SD - From", "SD - To", "Remark", "Id", "AopYear", "Plant_FK_Id");

			List<Map<String, Object>> validPayload = new ArrayList<>();
			List<String[]> failedRawRows = new ArrayList<>();
			List<String> failedErrors = new ArrayList<>();

			try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
				Sheet sheet = workbook.getSheetAt(0);
				if (sheet == null) {
					throw new IllegalArgumentException("Workbook has no sheets");
				}
				DataFormatter fmt = new DataFormatter();
				int lastRow = sheet.getLastRowNum();

				for (int r = 1; r <= lastRow; r++) {
					Row row = sheet.getRow(r);
					if (row == null) continue;

					String shutdownType = getShutdownConfigCellStr(row, 0, fmt);
					String sdFrom      = getShutdownConfigCellStr(row, 1, fmt);
					String sdTo        = getShutdownConfigCellStr(row, 2, fmt);
					String remark      = getShutdownConfigCellStr(row, 3, fmt);
					String id          = getShutdownConfigCellStr(row, 4, fmt);
					String aopYear     = getShutdownConfigCellStr(row, 5, fmt);
					String plantFkId   = getShutdownConfigCellStr(row, 6, fmt);

					String[] rawValues = { shutdownType, sdFrom, sdTo, remark, id, aopYear, plantFkId };

					boolean allEmpty = true;
					for (String v : rawValues) {
						if (v != null && !v.isEmpty()) { allEmpty = false; break; }
					}
					if (allEmpty) continue;

					// Fall back to request parameters when hidden columns are absent
					if (aopYear.isEmpty())   aopYear   = year;
					if (plantFkId.isEmpty()) plantFkId = plantId;

					String err = null;
					if (shutdownType.isEmpty()) {
						err = "Shutdown Type is required";
					}

					if (err != null) {
						failedRawRows.add(rawValues);
						failedErrors.add(err);
					} else {
					Map<String, Object> item = new LinkedHashMap<>();
					item.put("Id",           id.isEmpty() ? null : id);
					item.put("ShutdownType", shutdownType);
					item.put("FromDate",     sdFrom.isEmpty() ? null : parseShutdownConfigDate(sdFrom));
					item.put("ToDate",       sdTo.isEmpty()   ? null : parseShutdownConfigDate(sdTo));
					item.put("Remarks",      remark);
					item.put("AopYear",      aopYear);
					item.put("Plant_FK_Id",  plantFkId);
					validPayload.add(item);
					}
				}
			}

			if (validPayload.isEmpty() && failedRawRows.isEmpty()) {
				AOPMessageVM vm = new AOPMessageVM();
				vm.setCode(400);
				vm.setMessage("No data rows found in file");
				return vm;
			}

			if (!failedRawRows.isEmpty()) {
				byte[] errBytes = buildShutdownHistoryConfigErrorExcel(headers, failedRawRows, failedErrors);
				AOPMessageVM vm = new AOPMessageVM();
				vm.setCode(400);
				vm.setMessage("Validation failed for " + failedRawRows.size() + " row(s); no data was saved");
				vm.setData(Base64.getEncoder().encodeToString(errBytes));
				return vm;
			}

			// Save each record; collect per-row failures
			List<String[]> saveFailedRawRows = new ArrayList<>();
			List<String> saveFailedErrors = new ArrayList<>();

			for (Map<String, Object> item : validPayload) {
				try {
					saveShutdownHistoryConfig(Collections.singletonList(item), saveFailedRawRows, saveFailedErrors);
				} catch (Exception e) {
					throw new RuntimeException("Failed to import shutdown history config data", e);
				}
			}

			AOPMessageVM aopMessageVM = new AOPMessageVM();
			if (!saveFailedRawRows.isEmpty()) {
				byte[] errBytes = buildShutdownHistoryConfigErrorExcel(headers, saveFailedRawRows, saveFailedErrors);
				aopMessageVM.setCode(400);
				aopMessageVM.setMessage("Partial data has been saved. " + saveFailedRawRows.size() + " row(s) failed.");
				aopMessageVM.setData(Base64.getEncoder().encodeToString(errBytes));
			} else {
				aopMessageVM.setCode(200);
				aopMessageVM.setMessage("All data has been saved");
			}
			return aopMessageVM;

		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid argument", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to import shutdown history config", ex);
		}
	}

	private String getShutdownConfigCellStr(Row row, int col, DataFormatter fmt) {
		Cell cell = row.getCell(col);
		return cell == null ? "" : fmt.formatCellValue(cell).trim();
	}

	private java.sql.Date parseShutdownConfigDate(String dateStr) {
		if (dateStr == null || dateStr.isEmpty()) return null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
			sdf.setLenient(false);
			return new java.sql.Date(sdf.parse(dateStr).getTime());
		} catch (Exception e) {
			return null;
		}
	}

	private String[] shutdownConfigPayloadToRaw(Map<String, Object> item) {
		return new String[] {
			item.get("ShutdownType") != null ? item.get("ShutdownType").toString() : "",
			item.get("FromDate")     != null ? item.get("FromDate").toString()     : "",
			item.get("ToDate")       != null ? item.get("ToDate").toString()       : "",
			item.get("Remarks")      != null ? item.get("Remarks").toString()      : "",
			item.get("Id")           != null ? item.get("Id").toString()           : "",
			item.get("AopYear")      != null ? item.get("AopYear").toString()      : "",
			item.get("Plant_FK_Id")  != null ? item.get("Plant_FK_Id").toString()  : ""
		};
	}

	// --- saveShutdownHistory – Validation helpers --------------------------------

	private void validateShutdownHistoryMandatoryFields(ShutdownHistoryConfigDTO dto) {
		List<String> missing = new ArrayList<>();
		if (dto.getMonth() == null) {
			missing.add("month");
		}
		if (dto.getYear() == null || dto.getYear().trim().isEmpty()) {
			missing.add("year");
		}
		if (dto.getTypeOfSD() == null) {
			missing.add("typeOfSD");
		}
		if (!missing.isEmpty()) {
			dto.setSaveStatus("Failed");
			dto.setErrDescription("Mandatory field(s) missing: " + String.join(", ", missing));
		}
	}


	private void validateShutdownHistoryRemark(ShutdownHistoryConfig existing,
			ShutdownHistoryConfigDTO incoming) {
		boolean monthChanged    = !Objects.equals(existing.getMonth(),    incoming.getMonth());
		boolean yearChanged     = !Objects.equals(existing.getYear(),     incoming.getYear());
		boolean typeOfSDChanged = !Objects.equals(existing.getTypeOfSD(), incoming.getTypeOfSD());
		boolean anyBusinessFieldChanged = monthChanged || yearChanged || typeOfSDChanged;

		String existingRemark = existing.getRemark() != null ? existing.getRemark() : "";
		String incomingRemark = incoming.getRemark()  != null ? incoming.getRemark()  : "";
		boolean remarkChanged = !existingRemark.equals(incomingRemark);

		if (anyBusinessFieldChanged && !remarkChanged) {
			incoming.setSaveStatus("Failed");
			incoming.setErrDescription("Please update remark");
		}
	}


	@SuppressWarnings("unchecked")
	private Set<Integer> fetchValidTypeOfSDValues(String plantFKId, String year) {
		Set<Integer> validValues = new HashSet<>();
		try {
			AOPMessageVM result = getTypeOfSD(plantFKId, year);
			if (result != null && result.getData() != null) {
				List<Map<String, Object>> types = (List<Map<String, Object>>) result.getData();
				for (Map<String, Object> type : types) {
					Object value = type.get("value");
					if (value instanceof Integer) {
						validValues.add((Integer) value);
					} else if (value != null) {
						try {
							validValues.add(Integer.parseInt(value.toString()));
						} catch (NumberFormatException ignored) {}
					}
				}
			}
		} catch (Exception ignored) {}
		return validValues;
	}


	private void validateShutdownHistoryTypeOfSD(ShutdownHistoryConfigDTO dto, Set<Integer> validTypeOfSDValues) {
		if (dto.getTypeOfSD() == null || validTypeOfSDValues.isEmpty()) {
			return;
		}
		if (!validTypeOfSDValues.contains(dto.getTypeOfSD())) {
			dto.setSaveStatus("Failed");
			dto.setErrDescription("Invalid Type of SD value");
		}
	}

	/**
	 * Builds the set of valid year strings (e.g. "2026", "2025" … "2021") from
	 * the AOP year parameter that is in "YYYY-YY" format (e.g. "2026-27"). The
	 * start year and the previous 5 years are included (6 values total).
	 */
	private Set<String> buildValidYearSet(String aopYear) {
		int currentStartYear;
		try {
			currentStartYear = Integer.parseInt(aopYear.split("-")[0].trim());
		} catch (Exception e) {
			currentStartYear = LocalDate.now().getYear();
		}
		Set<String> validYears = new HashSet<>();
		for (int i = 0; i <= 5; i++) {
			validYears.add(String.valueOf(currentStartYear - i));
		}
		return validYears;
	}


	private void validateShutdownHistoryYear(ShutdownHistoryConfigDTO dto, Set<String> validYears) {
		if (dto.getYear() == null || dto.getYear().trim().isEmpty()) {
			return;
		}
		if (!validYears.contains(dto.getYear().trim())) {
			dto.setSaveStatus("Failed");
			dto.setErrDescription("Invalid Year value: " + dto.getYear() + ". Valid values are: " + validYears);
		}
	}

	
	private ShutdownHistoryConfig buildFailedShutdownHistoryRecord(ShutdownHistoryConfigDTO dto, UUID plantId) {
		ShutdownHistoryConfig record = new ShutdownHistoryConfig();
		record.setId(dto.getId());
		record.setMonth(dto.getMonth());
		record.setYear(dto.getYear());
		record.setAopYear(dto.getAopYear());
		record.setRemark(dto.getRemark());
		record.setPlantFKId(plantId);
		record.setTypeOfSD(dto.getTypeOfSD());
		return record;
	}

	
	private String validateRemark(String id, Map<String, Object> incoming) {
		try {
			List<Map<String, Object>> rows = jdbcTemplate.queryForList(
					"SELECT ShutdownType, FromDate, ToDate, Remarks FROM ShutdownHistoryConfig WHERE Id = ?", id);

			if (rows.isEmpty()) {
				return null;
			}

			Map<String, Object> existing = rows.get(0);

			String existingShutdownType = existing.get("ShutdownType") != null ? existing.get("ShutdownType").toString() : "";
			String existingRemarks      = existing.get("Remarks")      != null ? existing.get("Remarks").toString()      : "";

			LocalDate existingFromDate = parseToLocalDate(existing.get("FromDate"));
			LocalDate existingToDate   = parseToLocalDate(existing.get("ToDate"));

			String incomingShutdownType = incoming.get("ShutdownType") != null ? incoming.get("ShutdownType").toString() : "";
			String incomingRemarks      = incoming.get("Remarks")      != null ? incoming.get("Remarks").toString()      : "";

			LocalDate incomingFromDate = parseToLocalDate(incoming.get("FromDate"));
			LocalDate incomingToDate   = parseToLocalDate(incoming.get("ToDate"));

			boolean shutdownTypeChanged    = !existingShutdownType.equals(incomingShutdownType);
			boolean fromDateChanged        = !Objects.equals(existingFromDate, incomingFromDate);
			boolean toDateChanged          = !Objects.equals(existingToDate, incomingToDate);
			boolean anyBusinessFieldChanged = shutdownTypeChanged || fromDateChanged || toDateChanged;
			boolean remarkChanged           = !existingRemarks.equals(incomingRemarks);

			if (anyBusinessFieldChanged && !remarkChanged) {
				return "Remarks must be updated when ShutdownType, FromDate, or ToDate is changed";
			}

			return null;

		} catch (Exception ex) {
			return null;
		}
	}

	private String validateShutdownHistoryDateRange(Map<String, Object> shutdownHistoryConfig) {
		String aopYear   = shutdownHistoryConfig.get("AopYear")      != null ? shutdownHistoryConfig.get("AopYear").toString()      : null;
		String plantFkId = shutdownHistoryConfig.get("Plant_FK_Id")  != null ? shutdownHistoryConfig.get("Plant_FK_Id").toString()  : null;

		if (aopYear == null || aopYear.isEmpty() || plantFkId == null || plantFkId.isEmpty()) {
			return "AopYear and Plant_FK_Id are required";
		}

		AOPMessageVM configResult = configurationService.getConfigurationExecution(aopYear, plantFkId);
		if (configResult == null || configResult.getData() == null) {
			return "Configuration not found";
		}

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> configData = (List<Map<String, Object>>) configResult.getData();

		LocalDate startDate = null;
		LocalDate endDate   = null;
		DateTimeFormatter configFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		for (Map<String, Object> entry : configData) {
			Object nameObj  = entry.get("Name");
			Object valueObj = entry.get("AttributeValue");
			if (nameObj == null || valueObj == null) continue;
			String name  = nameObj.toString();
			String value = valueObj.toString().trim();
			if (value.isEmpty()) continue;
			try {
				if ("StartDate".equalsIgnoreCase(name)) {
					startDate = LocalDate.parse(value, configFmt);
				} else if ("EndDate".equalsIgnoreCase(name)) {
					endDate = LocalDate.parse(value, configFmt);
				}
			} catch (Exception ignored) {}
		}

		if (startDate == null || endDate == null) {
			return "Start date or end date is not found in configuration";
		}

		LocalDate fromDate = parseToLocalDate(shutdownHistoryConfig.get("FromDate"));
		LocalDate toDate   = parseToLocalDate(shutdownHistoryConfig.get("ToDate"));

		if (fromDate != null && (fromDate.isBefore(startDate) || fromDate.isAfter(endDate))) {
			return "FromDate (" + fromDate + ") is outside the allowed date range [" + startDate + " to " + endDate + "]";
		}

		if (toDate != null && (toDate.isBefore(startDate) || toDate.isAfter(endDate))) {
			return "ToDate (" + toDate + ") is outside the allowed date range [" + startDate + " to " + endDate + "]";
		}

		if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
			return "FromDate (" + fromDate + ") must not be after ToDate (" + toDate + ").";
		}

		return null;
	}

	
	private LocalDate parseToLocalDate(Object dateObj) {
		if (dateObj == null) return null;
		if (dateObj instanceof java.sql.Date) {
			return ((java.sql.Date) dateObj).toLocalDate();
		}
		if (dateObj instanceof java.util.Date) {
			return ((java.util.Date) dateObj).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		}
		String str = dateObj.toString().trim();
		if (str.isEmpty()) return null;
		String[] patterns = {"yyyy-MM-dd", "dd-MM-yyyy", "MM/dd/yyyy", "dd/MM/yyyy"};
		for (String pattern : patterns) {
			try {
				return LocalDate.parse(str, DateTimeFormatter.ofPattern(pattern));
			} catch (Exception ignored) {}
		}
		return null;
	}

	// --- Shutdown History – Export Excel -----------------------------------------

	private static final String[] SD_MONTH_NAMES = {
		"January", "February", "March", "April", "May", "June",
		"July", "August", "September", "October", "November", "December"
	};

	private String sdMonthName(Integer month) {
		if (month == null || month < 1 || month > 12) return "";
		return SD_MONTH_NAMES[month - 1];
	}

	private Integer sdMonthNumber(String name) {
		if (name == null || name.isEmpty()) return null;
		for (int i = 0; i < SD_MONTH_NAMES.length; i++) {
			if (SD_MONTH_NAMES[i].equalsIgnoreCase(name.trim())) return i + 1;
		}
		try {
			return (int) Math.round(Double.parseDouble(name.trim()));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private String sdTypeOfSDLabel(Integer days) {
		if (days == null) return "";
		return days + " Days";
	}

	private Integer sdTypeOfSDValue(String label) {
		if (label == null || label.isEmpty()) return null;
		String digits = label.replaceAll("[^0-9]", "").trim();
		if (digits.isEmpty()) return null;
		try {
			return Integer.parseInt(digits);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public byte[] createShutdownHistoryExcel(String plantId, String year, boolean isAfterSave,
			List<ShutdownHistoryConfigDTO> dtoList) {
		try {
			if (!isAfterSave) {
				AOPMessageVM result = getShutdownHistory(plantId, year);
				@SuppressWarnings("unchecked")
				List<ShutdownHistoryConfigDTO> fetched = (List<ShutdownHistoryConfigDTO>) result.getData();
				dtoList = fetched;
			}

			// Visible cols 0-3: Month, Year, Type of SD (Days), Remark
			// Hidden  col  4  : Id
			// isAfterSave adds cols 5-6: Status, Error Description
			List<String> headerNames = new ArrayList<>(Arrays.asList(
					"Month", "Year", "Type of SD (Days)", "Remark", "Id"));
			if (isAfterSave) {
				headerNames.add("Status");
				headerNames.add("Error Description");
			}

			try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				CellStyle headerStyle = Utility.createBoldBorderedStyle(workbook);
				CellStyle borderStyle = Utility.createBorderedStyle(workbook);

				Sheet sheet = workbook.createSheet("Shutdown History");
				Row headerRow = sheet.createRow(0);
				for (int col = 0; col < headerNames.size(); col++) {
					Cell cell = headerRow.createCell(col);
					cell.setCellValue(headerNames.get(col));
					cell.setCellStyle(headerStyle);
				}

				int rowIdx = 1;
				for (ShutdownHistoryConfigDTO dto : dtoList) {
					Row row = sheet.createRow(rowIdx++);

					// Col 0 – Month (full name, e.g. "January")
					Cell c0 = row.createCell(0);
					c0.setCellValue(sdMonthName(dto.getMonth()));
					c0.setCellStyle(borderStyle);

					// Col 1 – Year
					Cell c1 = row.createCell(1);
					c1.setCellValue(dto.getYear() != null ? dto.getYear() : "");
					c1.setCellStyle(borderStyle);

					// Col 2 – Type of SD (Days) (e.g. "5 Days")
					Cell c2 = row.createCell(2);
					c2.setCellValue(sdTypeOfSDLabel(dto.getTypeOfSD()));
					c2.setCellStyle(borderStyle);

					// Col 3 – Remark
					Cell c3 = row.createCell(3);
					c3.setCellValue(dto.getRemark() != null ? dto.getRemark() : "");
					c3.setCellStyle(borderStyle);

					// Col 4 – Id (hidden, used during import for updates)
					Cell c4 = row.createCell(4);
					c4.setCellValue(dto.getId() != null ? dto.getId().toString() : "");
					c4.setCellStyle(borderStyle);

					if (isAfterSave) {
						Cell c5 = row.createCell(5);
						c5.setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
						c5.setCellStyle(borderStyle);

						Cell c6 = row.createCell(6);
						c6.setCellValue(dto.getErrDescription() != null ? dto.getErrDescription() : "");
						c6.setCellStyle(borderStyle);
					}
				}

				int totalCols = isAfterSave ? 7 : 5;
				for (int col = 0; col < totalCols; col++) {
					sheet.autoSizeColumn(col);
				}
				sheet.setColumnHidden(4, true);

				workbook.write(baos);
				return baos.toByteArray();
			}
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to export shutdown history", ex);
		}
	}

	// --- Shutdown History – Import Excel -----------------------------------------

	@Override
	public AOPMessageVM importShutdownHistoryExcel(String year, String plantId, MultipartFile file) {
		if (file.isEmpty() || (file.getOriginalFilename() != null && !file.getOriginalFilename().endsWith(".xlsx"))) {
			throw new IllegalArgumentException("Invalid or empty Excel file.");
		}

			List<ShutdownHistoryConfigDTO> data = readShutdownHistoryExcel(file, plantId, year);
		
				
			List<ShutdownHistoryConfigDTO> failedRecords = saveShutdownHistory(year, plantId, data);
	
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			if (!failedRecords.isEmpty()) {
				byte[] fileByteArray = createShutdownHistoryExcel(plantId, year, true, failedRecords);
				String base64File = Base64.getEncoder().encodeToString(fileByteArray);
				aopMessageVM.setData(base64File);
				aopMessageVM.setCode(400);
				aopMessageVM.setMessage("Partial data has been saved");
			} else {
				aopMessageVM.setCode(200);
				aopMessageVM.setMessage("All data has been saved");
			}
			return aopMessageVM;

		
	}

	private List<ShutdownHistoryConfigDTO> readShutdownHistoryExcel(MultipartFile file, String plantId, String year) {
		List<ShutdownHistoryConfigDTO> resultList = new ArrayList<>();
		DataFormatter fmt = new DataFormatter();

		try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
			Sheet sheet = workbook.getSheetAt(0);
			if (sheet == null) {
				return resultList;
			}
			int lastRow = sheet.getLastRowNum();
			for (int r = 1; r <= lastRow; r++) {
				Row row = sheet.getRow(r);
				if (row == null) continue;

				String monthStr    = getShutdownHistoryCellStr(row, 0, fmt);
				String yearStr     = getShutdownHistoryCellStr(row, 1, fmt);
				String typeOfSDStr = getShutdownHistoryCellStr(row, 2, fmt);
				String remark      = getShutdownHistoryCellStr(row, 3, fmt);
				String idStr       = getShutdownHistoryCellStr(row, 4, fmt);

				// Skip completely empty rows
				if (monthStr.isEmpty() && yearStr.isEmpty() && typeOfSDStr.isEmpty() && remark.isEmpty()) {
					continue;
				}

				ShutdownHistoryConfigDTO dto = new ShutdownHistoryConfigDTO();
				String err = null;

				// Col 0 – Month (full name ? integer)
				if (!monthStr.isEmpty()) {
					Integer monthNum = sdMonthNumber(monthStr);
					if (monthNum == null) {
						err = "Invalid Month value: " + monthStr;
					} else {
						dto.setMonth(monthNum);
					}
				}

				// Col 1 – Year
				dto.setYear(yearStr.isEmpty() ? null : yearStr);

				// Col 2 – Type of SD (Days) ("X Days" ? integer)
				if (!typeOfSDStr.isEmpty()) {
					Integer days = sdTypeOfSDValue(typeOfSDStr);
					if (days == null) {
						if (err == null) err = "Invalid Type of SD value: " + typeOfSDStr;
					} else {
						dto.setTypeOfSD(days);
					}
				}

				// Col 3 – Remark
				dto.setRemark(remark.isEmpty() ? null : remark);

				// Col 4 – Id (hidden; present = update, absent = insert)
				if (!idStr.isEmpty()) {
					try {
						dto.setId(UUID.fromString(idStr));
					} catch (IllegalArgumentException e) {
						// treat as new record if UUID is malformed
					}
				}

				// aopYear and plantId come from request parameters
				dto.setAopYear(year);
				dto.setPlantId(plantId);

				if (err != null) {
					dto.setSaveStatus("Failed");
					dto.setErrDescription(err);
				}

				resultList.add(dto);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to read Shutdown History Excel", e);
		}
		return resultList;
	}

	private String getShutdownHistoryCellStr(Row row, int col, DataFormatter fmt) {
		Cell cell = row.getCell(col);
		return cell == null ? "" : fmt.formatCellValue(cell).trim();
	}

	private byte[] buildShutdownHistoryConfigErrorExcel(List<String> headers, List<String[]> failedRows,
		List<String> errors) {
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("Errors");
			CellStyle headerStyle = Utility.createBoldBorderedStyle(workbook);
			CellStyle borderStyle = Utility.createBorderedStyle(workbook);

			// Mirror the export layout: visible cols 0-3, hidden identity cols 4-6
			int hiddenFromCol = 4;

			Row headerRow = sheet.createRow(0);
			for (int i = 0; i < headers.size(); i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(headers.get(i));
				cell.setCellStyle(headerStyle);
			}
			Cell errHeaderCell = headerRow.createCell(headers.size());
			errHeaderCell.setCellValue("errDescription");
			errHeaderCell.setCellStyle(headerStyle);

			for (int r = 0; r < failedRows.size(); r++) {
				Row excelRow = sheet.createRow(r + 1);
				String[] vals = failedRows.get(r);
				for (int c = 0; c < headers.size(); c++) {
					Cell cell = excelRow.createCell(c);
					cell.setCellValue(vals[c] != null ? vals[c] : "");
					cell.setCellStyle(borderStyle);
				}
				Cell errCell = excelRow.createCell(headers.size());
				errCell.setCellValue(errors.get(r));
				errCell.setCellStyle(borderStyle);
			}

			// Auto-size visible columns
			for (int i = 0; i < hiddenFromCol; i++) {
				sheet.autoSizeColumn(i);
			}
			// Auto-size and hide the metadata columns (Id, AopYear, Plant_FK_Id)
			for (int i = hiddenFromCol; i < headers.size(); i++) {
				sheet.autoSizeColumn(i);
				sheet.setColumnHidden(i, true);
			}
			// errDescription column – always visible
			sheet.autoSizeColumn(headers.size());

			workbook.write(baos);
			return baos.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException("Failed to build error workbook", e);
		}
	}

	// --- saveSlowdownHistory – Validation helpers --------------------------------

	private void validateSlowdownMandatoryFields(SlowdownHistoryConfigDTO dto) {
		List<String> missingFields = new ArrayList<>();

		if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
			missingFields.add("Description");
		}
		if (dto.getMaintStartDateTime() == null) {
			missingFields.add("MaintStartDateTime");
		}
		if (dto.getMaintEndDateTime() == null) {
			missingFields.add("MaintEndDateTime");
		}
		if (dto.getDurationInMins() == null) {
			missingFields.add("DurationInMins");
		}
	
		if (dto.getRate() == null) {
			missingFields.add("Rate");
		}

		if (!missingFields.isEmpty()) {
			dto.setSaveStatus("Failed");
			dto.setErrDescription("Mandatory field(s) missing: " + String.join(", ", missingFields));
		}
	}

	/**
	 * Validates that MaintStartDateTime is not after MaintEndDateTime, and that
	 * both fall within the April–March fiscal range implied by the AOP year
	 * parameter (e.g. "2026-27" ? 2026-04-01 to 2027-03-31).
	 */
	private void validateSlowdownDates(SlowdownHistoryConfigDTO dto, String aopYear) {
		Date start = dto.getMaintStartDateTime();
		Date end   = dto.getMaintEndDateTime();

		if (start != null && end != null && start.after(end)) {
			dto.setSaveStatus("Failed");
			dto.setErrDescription("MaintStartDateTime cannot be greater than MaintEndDateTime");
			return;
		}

		LocalDate[] range = buildSlowdownYearDateRange(aopYear);
		if (range == null) {
			return;
		}
		LocalDate rangeStart = range[0];
		LocalDate rangeEnd   = range[1];

		if (start != null) {
			LocalDate startLocal = sldToLocalDate(start);
			if (startLocal.isBefore(rangeStart) || startLocal.isAfter(rangeEnd)) {
				dto.setSaveStatus("Failed");
				dto.setErrDescription("MaintStartDateTime (" + startLocal + ") is outside the allowed range ["
						+ rangeStart + " to " + rangeEnd + "] for year " + aopYear);
				return;
			}
		}

		if (end != null) {
			LocalDate endLocal = sldToLocalDate(end);
			if (endLocal.isBefore(rangeStart) || endLocal.isAfter(rangeEnd)) {
				dto.setSaveStatus("Failed");
				dto.setErrDescription("MaintEndDateTime (" + endLocal + ") is outside the allowed range ["
						+ rangeStart + " to " + rangeEnd + "] for year " + aopYear);
			}
		}
	}

	/**
	 * Builds the fiscal year date range from an AOP year string in "YYYY-YY"
	 * format (e.g. "2026-27"). Returns [April 1 of start year, March 31 of end
	 * year], or {@code null} if the format cannot be parsed.
	 */
	private LocalDate[] buildSlowdownYearDateRange(String aopYear) {
		try {
			int startYear = Integer.parseInt(aopYear.split("-")[0].trim());
			int endYear   = startYear + 1;
			return new LocalDate[]{
					LocalDate.of(startYear, 4, 1),
					LocalDate.of(endYear, 3, 31)
			};
		} catch (Exception e) {
			return null;
		}
	}

	private void validateSlowdownRemark(SlowdownHistoryConfig existing, SlowdownHistoryConfigDTO incoming) {
		boolean descriptionChanged        = !Objects.equals(existing.getDescription(),   incoming.getDescription());
		boolean maintStartDateTimeChanged = !Objects.equals(
				sldToLocalDate(existing.getMaintStartDateTime()), sldToLocalDate(incoming.getMaintStartDateTime()));
		boolean maintEndDateTimeChanged   = !Objects.equals(
				sldToLocalDate(existing.getMaintEndDateTime()),   sldToLocalDate(incoming.getMaintEndDateTime()));
		boolean durationChanged           = !Objects.equals(existing.getDurationInMins(), incoming.getDurationInMins());
		boolean maintForMonthChanged      = !Objects.equals(existing.getMaintForMonth(),  incoming.getMaintForMonth());
		boolean rateChanged               = !Objects.equals(existing.getRate(),           incoming.getRate());

		boolean anyBusinessFieldChanged = descriptionChanged || maintStartDateTimeChanged || maintEndDateTimeChanged
				|| durationChanged || maintForMonthChanged || rateChanged;

		String existingRemarks = existing.getRemarks() != null ? existing.getRemarks() : "";
		String incomingRemarks = incoming.getRemarks()  != null ? incoming.getRemarks()  : "";
		boolean remarkChanged  = !existingRemarks.equals(incomingRemarks);

		if (anyBusinessFieldChanged && !remarkChanged) {
			incoming.setSaveStatus("Failed");
			incoming.setErrDescription("Please update remark");
		}
	}

	private LocalDate sldToLocalDate(Date date) {
		if (date == null) return null;
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

	// --- Slowdown History – Export / Import Excel ---------------------------------

	private static final List<String> SLD_EXCEL_HEADERS = Arrays.asList(
			"Slowdown Desc", "SD - From", "SD - To", "Duration (Hrs)", "Rate", "Remarks", "Id");

	private static final int SLD_HIDDEN_COL = 6;

	/**
	 * Formats durationInMins (integer minutes) as "HH:mm", e.g. 1440 ? "24:00".
	 */
	private String formatSlowdownDuration(Integer durationInMins) {
		if (durationInMins == null) return "";
		int h = durationInMins / 60;
		int m = durationInMins % 60;
		return String.format("%02d:%02d", h, m);
	}

	/**
	 * Parses a user-supplied duration string to integer minutes.
	 * Accepted formats: "05:30", "5.3" (H.MM – single-digit decimal is right-padded,
	 * so 5.3 = 5h 30m), "0:45", "0.45", "24", "24:00".
	 */
	private Integer parseSlowdownDurationToMins(String input) {
		if (input == null || input.trim().isEmpty()) return null;
		input = input.trim();
		if (input.contains(":")) {
			String[] parts = input.split(":", 2);
			int h = Integer.parseInt(parts[0].trim());
			int m = parts[1].trim().isEmpty() ? 0 : Integer.parseInt(parts[1].trim());
			return h * 60 + m;
		}
		if (input.contains(".")) {
			String[] parts = input.split("\\.", 2);
			int h = Integer.parseInt(parts[0].trim());
			String minStr = parts[1].trim();
			if (minStr.length() == 1) minStr = minStr + "0"; // "5.3" ? 30 min
			int m = Integer.parseInt(minStr);
			return h * 60 + m;
		}
		// Plain integer ? treat as whole hours
		return (int) Math.round(Double.parseDouble(input) * 60);
	}

	/**
	 * Reads a datetime value from an Excel cell, gracefully handling:
	 * <ul>
	 *   <li>Numeric Excel date/datetime cells (both date-only and date+time serial)</li>
	 *   <li>Text cells with "dd-MM-yyyy hh:mm a" (12-hr AM/PM as shown in screenshot)</li>
	 *   <li>Text cells with "dd-MM-yyyy HH:mm" (24-hr without AM/PM)</li>
	 *   <li>The same two patterns with "/" separators</li>
	 * </ul>
	 */
	private Date parseSlowdownDateTime(Row row, int col, DataFormatter fmt) {
		Cell cell = row.getCell(col);
		if (cell == null) return null;

		if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
			return cell.getDateCellValue();
		}

		String str = fmt.formatCellValue(cell).trim();
		if (str.isEmpty()) return null;

		for (String pattern : new String[]{
				"dd-MM-yyyy hh:mm a", "dd-MM-yyyy HH:mm",
				"dd/MM/yyyy hh:mm a", "dd/MM/yyyy HH:mm",
				"dd-MM-yyyy", "dd/MM/yyyy"}) {
			try {
				SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ENGLISH);
				sdf.setLenient(false);
				return sdf.parse(str);
			} catch (Exception ignored) {}
		}
		return null;
	}

	/**
	 * Converts the raw Map list returned by {@link #getSlowdownHistory} into a
	 * typed {@link SlowdownHistoryConfigDTO} list for use by the export method.
	 */
	private List<SlowdownHistoryConfigDTO> convertSlowdownMapsToDto(List<Map<String, Object>> mapList) {
		List<SlowdownHistoryConfigDTO> result = new ArrayList<>();
		if (mapList == null) return result;
		for (Map<String, Object> map : mapList) {
			SlowdownHistoryConfigDTO dto = new SlowdownHistoryConfigDTO();
			Object idObj = map.get("id");
			if (idObj != null && !idObj.toString().isEmpty()) {
				try { dto.setId(UUID.fromString(idObj.toString())); } catch (Exception ignored) {}
			}
			dto.setDescription(map.get("description") != null ? map.get("description").toString() : null);
			Object startObj = map.get("maintStartDateTime");
			if (startObj instanceof Date) dto.setMaintStartDateTime((Date) startObj);
			Object endObj = map.get("maintEndDateTime");
			if (endObj instanceof Date) dto.setMaintEndDateTime((Date) endObj);
			Object durationObj = map.get("durationInMins");
			if (durationObj != null) {
				try { dto.setDurationInMins(Integer.parseInt(durationObj.toString())); } catch (Exception ignored) {}
			}
			Object maintMonthObj = map.get("maintForMonth");
			if (maintMonthObj != null) {
				try { dto.setMaintForMonth(Integer.parseInt(maintMonthObj.toString())); } catch (Exception ignored) {}
			}
			dto.setAuditYear(map.get("auditYear") != null ? map.get("auditYear").toString() : null);
			Object rateObj = map.get("rate");
			if (rateObj != null) {
				try { dto.setRate(Double.parseDouble(rateObj.toString())); } catch (Exception ignored) {}
			}
			dto.setRemarks(map.get("remarks") != null ? map.get("remarks").toString() : null);
			Object plantObj = map.get("plantFkId");
			if (plantObj != null && !plantObj.toString().isEmpty()) {
				try { dto.setPlantFkId(UUID.fromString(plantObj.toString())); } catch (Exception ignored) {}
			}
			result.add(dto);
		}
		return result;
	}

	@Override
	public byte[] createSlowdownHistoryExcel(String plantId, String year, boolean isAfterSave,
			List<SlowdownHistoryConfigDTO> dtoList) {
		try {
			if (!isAfterSave) {
				AOPMessageVM result = getSlowdownHistory(plantId, year);
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> dataList = (List<Map<String, Object>>) result.getData();
				dtoList = convertSlowdownMapsToDto(dataList);
			}

			List<String> headerNames = new ArrayList<>(SLD_EXCEL_HEADERS);
			if (isAfterSave) {
				headerNames.add("Save Status");
				headerNames.add("Error Description");
			}

			try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				CellStyle headerStyle = Utility.createBoldBorderedStyle(workbook);
				CellStyle borderStyle = Utility.createBorderedStyle(workbook);

				Sheet sheet = workbook.createSheet("Slowdown History");
				Row headerRow = sheet.createRow(0);
				for (int i = 0; i < headerNames.size(); i++) {
					Cell cell = headerRow.createCell(i);
					cell.setCellValue(headerNames.get(i));
					cell.setCellStyle(headerStyle);
				}

				SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.ENGLISH);
				int rowIdx = 1;
				for (SlowdownHistoryConfigDTO dto : dtoList) {
					Row row = sheet.createRow(rowIdx++);

					// Col 0 – Slowdown Desc
					Cell c0 = row.createCell(0);
					c0.setCellValue(dto.getDescription() != null ? dto.getDescription() : "");
					c0.setCellStyle(borderStyle);

					// Col 1 – SD - From (maintStartDateTime)
					Cell c1 = row.createCell(1);
					c1.setCellValue(dto.getMaintStartDateTime() != null ? sdf.format(dto.getMaintStartDateTime()) : "");
					c1.setCellStyle(borderStyle);

					// Col 2 – SD - To (maintEndDateTime)
					Cell c2 = row.createCell(2);
					c2.setCellValue(dto.getMaintEndDateTime() != null ? sdf.format(dto.getMaintEndDateTime()) : "");
					c2.setCellStyle(borderStyle);

					// Col 3 – Duration (Hrs): durationInMins ? "HH:mm"
					Cell c3 = row.createCell(3);
					c3.setCellValue(formatSlowdownDuration(dto.getDurationInMins()));
					c3.setCellStyle(borderStyle);

					// Col 4 – Rate
					Cell c4 = row.createCell(4);
					c4.setCellValue(dto.getRate() != null ? dto.getRate().toString() : "");
					c4.setCellStyle(borderStyle);

					// Col 5 – Remarks
					Cell c5 = row.createCell(5);
					c5.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
					c5.setCellStyle(borderStyle);

					// Col 6 – Id (hidden; used by import to identify existing records)
					Cell c6 = row.createCell(6);
					c6.setCellValue(dto.getId() != null ? dto.getId().toString() : "");
					c6.setCellStyle(borderStyle);

					if (isAfterSave) {
						Cell c7 = row.createCell(7);
						c7.setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
						c7.setCellStyle(borderStyle);

						Cell c8 = row.createCell(8);
						c8.setCellValue(dto.getErrDescription() != null ? dto.getErrDescription() : "");
						c8.setCellStyle(borderStyle);
					}
				}

				for (int i = 0; i < headerNames.size(); i++) {
					sheet.autoSizeColumn(i);
				}
				sheet.setColumnHidden(SLD_HIDDEN_COL, true);

				workbook.write(baos);
				return baos.toByteArray();
			}
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to export slowdown history", ex);
		}
	}

	// --- Slowdown History – Import Excel -----------------------------------------

	@Override
	public AOPMessageVM importSlowdownHistoryExcel(String year, String plantId, MultipartFile file) {
		if (file.isEmpty() || (file.getOriginalFilename() != null && !file.getOriginalFilename().endsWith(".xlsx"))) {
			throw new IllegalArgumentException("Invalid or empty Excel file.");
		}

		List<SlowdownHistoryConfigDTO> allDtos = readSlowdownHistoryExcel(file, plantId, year);

		if (allDtos.isEmpty()) {
			AOPMessageVM vm = new AOPMessageVM();
			vm.setCode(400);
			vm.setMessage("No data rows found in file");
			return vm;
		}

		List<SlowdownHistoryConfigDTO> failedRecords = saveSlowdownHistory(year, plantId, allDtos);
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		if (!failedRecords.isEmpty()) {
			byte[] fileByteArray = createSlowdownHistoryExcel(plantId, year, true, failedRecords);
			aopMessageVM.setCode(400);
			aopMessageVM.setMessage("Partial data has been saved. " + failedRecords.size() + " row(s) failed.");
			aopMessageVM.setData(Base64.getEncoder().encodeToString(fileByteArray));
		} else {
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("All data has been saved");
		}
		return aopMessageVM;
	}

	private List<SlowdownHistoryConfigDTO> readSlowdownHistoryExcel(MultipartFile file, String plantId, String year) {
		List<SlowdownHistoryConfigDTO> result = new ArrayList<>();
		DataFormatter fmt = new DataFormatter();

		try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
			Sheet sheet = workbook.getSheetAt(0);
			if (sheet == null) return result;

			int lastRow = sheet.getLastRowNum();
			for (int r = 1; r <= lastRow; r++) {
				Row row = sheet.getRow(r);
				if (row == null) continue;

				String descStr     = getSldCellStr(row, 0, fmt);
				String fromStr     = getSldCellStr(row, 1, fmt);
				String toStr       = getSldCellStr(row, 2, fmt);
				String durationStr = getSldCellStr(row, 3, fmt);
				String rateStr     = getSldCellStr(row, 4, fmt);
				String remarkStr   = getSldCellStr(row, 5, fmt);
				String idStr       = getSldCellStr(row, 6, fmt);

				// Skip completely empty rows (the hidden Id column is not considered)
				if (descStr.isEmpty() && fromStr.isEmpty() && toStr.isEmpty()
						&& durationStr.isEmpty() && rateStr.isEmpty() && remarkStr.isEmpty()) {
					continue;
				}

				SlowdownHistoryConfigDTO dto = new SlowdownHistoryConfigDTO();
				String err = null;

				// Col 0 – Slowdown Desc
				dto.setDescription(descStr.isEmpty() ? null : descStr);

				// Col 1 – SD - From: handles numeric Excel datetime and text (12-hr or 24-hr)
				Date startDate = parseSlowdownDateTime(row, 1, fmt);
				if (startDate != null) {
					dto.setMaintStartDateTime(startDate);
				} else if (!fromStr.isEmpty()) {
					err = "Invalid SD - From value: " + fromStr;
				}

				// Col 2 – SD - To
				Date endDate = parseSlowdownDateTime(row, 2, fmt);
				if (endDate != null) {
					dto.setMaintEndDateTime(endDate);
				} else if (!toStr.isEmpty()) {
					if (err == null) err = "Invalid SD - To value: " + toStr;
				}

				// Col 3 – Duration (Hrs): "05:30", "5.3", "0:45", "0.45", "24", "24:00"
				if (!durationStr.isEmpty()) {
					try {
						dto.setDurationInMins(parseSlowdownDurationToMins(durationStr));
					} catch (Exception e) {
						if (err == null)
							err = "Invalid Duration value: " + durationStr + " (expected HH:mm or H.MM, e.g. 05:30 or 5.3)";
					}
				}

				// Col 4 – Rate
				if (!rateStr.isEmpty()) {
					try {
						dto.setRate(Double.parseDouble(rateStr));
					} catch (NumberFormatException e) {
						if (err == null) err = "Invalid Rate value: " + rateStr;
					}
				}

				// Col 5 – Remarks
				dto.setRemarks(remarkStr.isEmpty() ? null : remarkStr);

				// Col 6 – Id (hidden; present = update, absent = insert)
				if (!idStr.isEmpty()) {
					try {
						dto.setId(UUID.fromString(idStr));
					} catch (IllegalArgumentException e) {
						// malformed UUID – treat as new record
					}
				}

				dto.setAuditYear(year);

				if (err != null) {
					dto.setSaveStatus("Failed");
					dto.setErrDescription(err);
				}

				result.add(dto);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to read Slowdown History Excel", e);
		}
		return result;
	}

	private String getSldCellStr(Row row, int col, DataFormatter fmt) {
		Cell cell = row.getCell(col);
		return cell == null ? "" : fmt.formatCellValue(cell).trim();
	}
}
