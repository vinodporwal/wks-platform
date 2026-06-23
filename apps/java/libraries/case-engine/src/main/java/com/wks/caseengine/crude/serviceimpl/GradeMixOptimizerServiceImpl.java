package com.wks.caseengine.crude.serviceimpl;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.wks.caseengine.dto.GradeMixOptimizerConstantDTO;
import com.wks.caseengine.entity.AopCalculation;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.ScreenMapping;
import com.wks.caseengine.exception.RestInvalidArgumentException;
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
}
