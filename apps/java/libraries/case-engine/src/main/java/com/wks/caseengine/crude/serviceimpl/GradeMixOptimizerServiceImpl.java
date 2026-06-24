package com.wks.caseengine.crude.serviceimpl;

import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.ResultSetExtractor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.wks.caseengine.dto.GradeMixOptimizerConstantDTO;
import com.wks.caseengine.entity.AopCalculation;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.ScreenMapping;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.AopCalculationRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.ScreenMappingRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;


@Service
public class GradeMixOptimizerServiceImpl implements GradeMixOptimizerService {
   
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private VerticalsRepository verticalRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private AopCalculationRepository aopCalculationRepository;

    @Autowired
    private ScreenMappingRepository screenMappingRepository;




    public AOPMessageVM getGradeMixOptimizerConstants(UUID plantId, String aopYear) {

        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();
        
        String procedureName = verticalName + "_" + siteName + "_GetGradeMixOptimizerConstant";
        List<GradeMixOptimizerConstantDTO> gradeMixOptimizerConstants = fetchGradeMixOptimizerConstantsFromProcedure(plantId, aopYear, procedureName);
        return AOPMessageVM.builder()
            .code(200)
            .message("GradeMixOptimizer constants fetched successfully")
            .data(gradeMixOptimizerConstants)
            .build();
    }
    
    public List<GradeMixOptimizerConstantDTO> fetchGradeMixOptimizerConstantsFromProcedure(UUID plantId, String aopYear, String procedureName) {

        String sql = "EXEC " + procedureName + " @plantId = ?, @aopYear = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
            GradeMixOptimizerConstantDTO.builder()
                .id(UUID.fromString(rs.getString("Id")))
                .normParameterFkId(UUID.fromString(rs.getString("NormParameter_FK_Id")))
                .jan(rs.getDouble("Jan"))
                .feb(rs.getDouble("Feb"))
                .mar(rs.getDouble("Mar"))
                .apr(rs.getDouble("Apr"))
                .may(rs.getDouble("May"))
                .jun(rs.getDouble("Jun"))
                .jul(rs.getDouble("Jul"))
                .aug(rs.getDouble("Aug"))
                .sep(rs.getDouble("Sep"))
                .oct(rs.getDouble("Oct"))
                .nov(rs.getDouble("Nov"))
                .dec(rs.getDouble("Dec"))
                .remarks(rs.getString("Remarks"))
                .auditYear(rs.getString("AuditYear"))
                .uom(rs.getString("UOM"))
                .normTypeName(rs.getString("NormTypeName"))
                .isEditable(rs.getBoolean("IsEditable"))
                .displayName(rs.getString("DisplayName"))
                .build(),
            plantId.toString(), aopYear
        );
    }

    @Override
    public AOPMessageVM calculateBudgetOperationHours(UUID plantId, String aopYear) {
        
        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();

        String procedureName = verticalName + "_" + siteName + "_CalculateGradewiseMonthwiseBudgetOperatingHours";

        Integer result = executeBudgetOperationHoursCalculationSP(String.valueOf(plantId), aopYear, procedureName);
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		aopMessageVM.setCode(200);
		aopMessageVM.setMessage("Calculate SP Executed successfully");
		aopMessageVM.setData(result);
		
		aopCalculationRepository.deleteByPlantIdAndAopYearAndCalculationScreen(plantId, aopYear,
				"budget-operating-hours");
                
		List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("budget-operating-hours");
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

    
	public Integer executeBudgetOperationHoursCalculationSP( String plantId, String aopYear, String procedureName) {
		try {

			String callSql = "{call " + "[" + procedureName + "]" + "(?, ?)}";


			return jdbcTemplate.update(callSql, plantId, aopYear);

		} catch (Exception e) {
			throw new RuntimeException("Failed to execute stored procedure", e);
		}
	}

    @Override
    public AOPMessageVM getCalculatedProposedBusinessDemand(UUID plantId, String aopYear, String lineId) {

        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();

        String procedureName = verticalName + "_" + siteName + "_GetCalculatedProposedBusinessDemand";

        try {
            Map<String, Object> databaseResults = fetchFromStoredProcedure(plantId.toString(), aopYear, lineId, procedureName);

            List<Map<String, Object>> rows = (List<Map<String, Object>>) databaseResults.get("data");
            List<Map<String, Object>> metadata = (List<Map<String, Object>>) databaseResults.get("metadata");

            Map<String, Object> finalData = new HashMap<>();
            finalData.put("data", rows);
            finalData.put("columns", metadata);

            AOPMessageVM aopMessageVM = new AOPMessageVM();
            aopMessageVM.setData(finalData);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            return aopMessageVM;

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Error fetching calculated proposed business demand", ex);
        }
    }

    private Map<String, Object> fetchFromStoredProcedure(String plantId, String aopYear, String lineId, String procedureName) {
        String sql = "EXEC " + procedureName + " @plantId = ?, @aopYear = ?, @lineId = ?";
        return jdbcTemplate.query(sql, (ResultSetExtractor<Map<String, Object>>) rs -> {
            List<Map<String, Object>> dataList = new ArrayList<>();
            List<Map<String, Object>> metadataList = new ArrayList<>();
            Set<String> numericFields = new HashSet<>();

            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = rsmd.getColumnLabel(i);
                int sqlType = rsmd.getColumnType(i);

                Map<String, Object> meta = new HashMap<>();
                meta.put("field", columnName);
                meta.put("title", columnName);
                meta.put("type", getFrontendType(rsmd.getColumnTypeName(i)));
                metadataList.add(meta);
                if (isNumericType(sqlType)) {
                    numericFields.add(columnName);
                }
            }
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String colName = rsmd.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(colName, value == null ? (numericFields.contains(colName) ? 0 : "") : value);
                }
                dataList.add(row);
            }

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("data", dataList);
            resultMap.put("metadata", metadataList);
            return resultMap;
        }, plantId, aopYear, lineId);
    }

    private boolean isNumericType(int sqlType) {
        return sqlType == Types.INTEGER || sqlType == Types.DOUBLE ||
               sqlType == Types.DECIMAL || sqlType == Types.FLOAT ||
               sqlType == Types.NUMERIC || sqlType == Types.REAL;
    }

    private String getFrontendType(String sqlTypeName) {
        if (sqlTypeName == null) {
            return "string";
        }
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
            case "UNIQUEIDENTIFIER":
                return "string";
            default:
                return "string";
        }
    }
}
