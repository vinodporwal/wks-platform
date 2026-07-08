package com.wks.caseengine.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.wks.caseengine.dto.ProdSchedulingConfigDTO;
import com.wks.caseengine.entity.AopCalculation;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.ScreenMapping;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.AopCalculationRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.ScreenMappingRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.entity.Sites;

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

@Service
public class SchedulingTaskServiceImpl implements SchedulingTaskService {

  

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private VerticalsRepository verticalRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private AopCalculationRepository aopCalculationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ScreenMappingRepository screenMappingRepository;



    @Override
    public AOPMessageVM getProdScheduling(final String plantId, final String aopYear) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();

        try {
            Plants plant = plantsRepository.findById(UUID.fromString(plantId))
            .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
    Sites site = siteRepository.findById(plant.getSiteFkId()).get();
    Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();

String procedureName = vertical.getName() + "_" + site.getName() + "_GetProdScheduling";



            Map<String, Object> databaseResults = fetchProdSchedulingFroSP(plantId, aopYear, procedureName);
            
            List<Map<String, Object>> rows = (List<Map<String, Object>>) databaseResults.get("data");
            List<Map<String, Object>> metadata = (List<Map<String, Object>>) databaseResults.get("columns");

            List<AopCalculation> aopCalculations = aopCalculationRepository
            .findByPlantIdAndAopYearAndCalculationScreen(UUID.fromString(plantId), aopYear, "prod-scheduling");


            Map<String, Object> finalData = new HashMap<>();
                finalData.put("data", rows);
                finalData.put("columns", metadata);
                finalData.put("aopCalculation", aopCalculations != null ? aopCalculations : new ArrayList<>());

            aopMessageVM.setData(finalData);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Error fetching production scheduling data", ex);
        }

        return aopMessageVM;
    }

    private Map<String, Object> fetchProdSchedulingFroSP(String plantId, String aopYear, String procedureName) { 

        Map<String, Object> results = entityManager.unwrap(Session.class)
                    .doReturningWork(new ReturningWork<Map<String, Object>>() {
                        @Override
                        public Map<String, Object> execute(Connection connection) throws SQLException {
                            Map<String, Object> resultMap = new HashMap<>();
                            List<Map<String, Object>> dataList = new ArrayList<>();
                            List<Map<String, Object>> metadataList = new ArrayList<>();

                            String sql = "EXEC " + procedureName + " @plantId = ?, @aopYear = ?";
                            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                                ps.setString(1, plantId);
                                ps.setString(2, aopYear);

                                try (ResultSet rs = ps.executeQuery()) {
                                    ResultSetMetaData rsmd = rs.getMetaData();
                                    int columnCount = rsmd.getColumnCount();

                                    for (int i = 1; i <= columnCount; i++) {
                                        Map<String, Object> col = new LinkedHashMap<>();
                                        col.put("field", rsmd.getColumnLabel(i));
                                        col.put("title", rsmd.getColumnLabel(i));
                                        col.put("type", getFrontendType(rsmd.getColumnTypeName(i)));
                                        col.put("isVisible", "true");
                                        metadataList.add(col);
                                    }

                                    while (rs.next()) {
                                        Map<String, Object> row = new LinkedHashMap<>();
                                        for (int i = 1; i <= columnCount; i++) {
                                            String colName = rsmd.getColumnLabel(i);
                                            int sqlType = rsmd.getColumnType(i);
                                            Object value = rs.getObject(i);
                                            row.put(colName, (value == null) ? (isNumericType(sqlType) ? 0 : "") : value);
                                        }
                                        dataList.add(row);
                                    }
                                }
                            }

                            resultMap.put("data", dataList);
                            resultMap.put("columns", metadataList);
                            return resultMap;
                        }
                    });
                    return results;
    }

    private boolean isNumericType(int sqlType) {
        return sqlType == Types.INTEGER || sqlType == Types.BIGINT || sqlType == Types.SMALLINT
                || sqlType == Types.TINYINT || sqlType == Types.FLOAT || sqlType == Types.DOUBLE
                || sqlType == Types.DECIMAL || sqlType == Types.NUMERIC || sqlType == Types.REAL;
    }

    private String getFrontendType(String sqlTypeName) {
        if (sqlTypeName == null) return "string";
        switch (sqlTypeName.toLowerCase()) {
            case "int": case "bigint": case "smallint": case "tinyint":
            case "float": case "real": case "decimal": case "numeric": case "money": case "smallmoney":
                return "number";
            case "date": case "datetime": case "datetime2": case "smalldatetime": case "datetimeoffset":
                return "date";
            case "bit":
                return "boolean";
            default:
                return "string";
        }
    }

@Override
   public AOPMessageVM getProdSchedulingConfigData(String plantId, String aopYear) { 

        Plants plant = plantsRepository.findById(UUID.fromString(plantId))
        .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
Sites site = siteRepository.findById(plant.getSiteFkId()).get();
Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();

String procedureName = vertical.getName() + "_" + site.getName() + "_GetProdSchedulingConfig";

        List<ProdSchedulingConfigDTO> data = getProdSchedulingConfigDataFromSP(plantId, aopYear, procedureName);

        AOPMessageVM aopMessageVM = new AOPMessageVM();
        aopMessageVM.setData(data);
        aopMessageVM.setCode(200);
        aopMessageVM.setMessage("Data fetched successfully");
        return aopMessageVM;




    }

  
    public List<ProdSchedulingConfigDTO> getProdSchedulingConfigDataFromSP(String plantId, String aopYear, String procedureName) {
        try {
            String sql = "EXEC " + procedureName + " @plantId = ?, @aopYear = ?";

            List<ProdSchedulingConfigDTO> data = jdbcTemplate.query(sql, (rs, rowNum) ->
                ProdSchedulingConfigDTO.builder()
                .id(rs.getString("Id") != null ? UUID.fromString(rs.getString("Id")) : null)
                    .batchPerDay(rs.getInt("BatchPerDay"))
                    .productionPerBatch(rs.getDouble("ProductionPerBatch"))
                    .sdWashAfterBatch(rs.getInt("SDWashAfterBatch"))
                    .sdFlushAfterBatch(rs.getInt("SDFlushAfterBatch"))
                    .sdWashHr(rs.getInt("SDWashHr"))
                    .sdFlushHr(rs.getInt("SDFlushHr"))
                    .quarterlySDHr(rs.getInt("QuarterlySDHr"))
                    .aopYear(rs.getString("AOPYear"))
                    .plantId(UUID.fromString(rs.getString("Plant_FK_Id")))
                    .build(), plantId, aopYear);

                return data;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public AOPMessageVM saveProdSchedulingConfigData(String plantId, String aopYear, List<ProdSchedulingConfigDTO> prodSchedulingConfigDTOs) {
        try {
          boolean isDataUpdated = false;
            for (ProdSchedulingConfigDTO dto : prodSchedulingConfigDTOs) {
                if (dto.getId() == null) {
                    // insert logic 
                    String insertSql = "INSERT INTO Chem_Prod_Scheduling_Config (Id, BatchPerDay, SDWashAfterBatch, SDFlushAfterBatch, SDWashHr, SDFlushHr, QuarterlySDHr, AOPYear, Plant_FK_Id, ProductionPerBatch) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    jdbcTemplate.update(insertSql,
                    UUID.randomUUID().toString(),
                    dto.getBatchPerDay(),
                    dto.getSdWashAfterBatch(),
                    dto.getSdFlushAfterBatch(),
                    dto.getSdWashHr(),
                    dto.getSdFlushHr(),
                    dto.getQuarterlySDHr(), dto.getAopYear(), dto.getPlantId().toString(), dto.getProductionPerBatch());

                    isDataUpdated = true;
                    continue;
                }

                String sql = "UPDATE Chem_Prod_Scheduling_Config " +
                         "SET BatchPerDay = ?, SDWashAfterBatch = ?, SDFlushAfterBatch = ?, SDWashHr = ?, SDFlushHr = ?, QuarterlySDHr = ? " +
                         "WHERE Id = ?";

                jdbcTemplate.update(sql,
                    dto.getBatchPerDay(),
                    dto.getSdWashAfterBatch(),
                    dto.getSdFlushAfterBatch(),
                    dto.getSdWashHr(),
                    dto.getSdFlushHr(),
                    dto.getQuarterlySDHr(),
                    dto.getId().toString());

                    isDataUpdated = true;
            }

            if (isDataUpdated) { 

                List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("prod-scheduling-config");
                for (ScreenMapping screenMapping : screenMappingList) {
                    AopCalculation aopCalculation = new AopCalculation();
                    aopCalculation.setAopYear(aopYear);
                    aopCalculation.setIsChanged(true);
                    aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
                    aopCalculation.setPlantId(UUID.fromString(plantId));
                    aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
                    aopCalculationRepository.save(aopCalculation);
                }
            }

            AOPMessageVM response = new AOPMessageVM();
            response.setCode(200);
            response.setData(null);
            response.setMessage("Data saved successfully");
            return response;

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to save plant report data", ex);
        }
    }

    @Override
    public AOPMessageVM calculateProdScheduling(UUID plantId, String aopYear) {
        
        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();

        String procedureName = verticalName + "_" + siteName + "_LoadProdScheduling";

        Integer result = executeCalculateSP(String.valueOf(plantId), aopYear, procedureName);
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		aopMessageVM.setCode(200);
		aopMessageVM.setMessage("Calculate SP Executed successfully");
		aopMessageVM.setData(result);
		
		aopCalculationRepository.deleteByPlantIdAndAopYearAndCalculationScreen(plantId, aopYear,
				"prod-scheduling");
                
		List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("prod-scheduling");
		for (ScreenMapping screenMapping : screenMappingList) {
			AopCalculation aopCalculation = new AopCalculation();
			aopCalculation.setAopYear(aopYear);
			aopCalculation.setIsChanged(true);
			aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
			aopCalculation.setPlantId(plantId);
			aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
			aopCalculationRepository.save(aopCalculation);
		}
		return aopMessageVM;
    }

    
	public Integer executeCalculateSP( String plantId, String aopYear, String procedureName) {
		try {

			String callSql = "{call " + "[" + procedureName + "]" + "(?, ?)}";


			return jdbcTemplate.update(callSql, plantId, aopYear);

		} catch (Exception e) {
			throw new RuntimeException("Failed to execute stored procedure", e);
		}
	}
}
