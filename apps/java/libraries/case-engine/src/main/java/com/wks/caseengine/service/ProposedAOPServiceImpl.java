package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.repository.AopCalculationRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.ScreenMappingRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.dto.ProposedAOPDTO;
import com.wks.caseengine.entity.AopCalculation;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.ScreenMapping;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;


@Service
public class ProposedAOPServiceImpl implements ProposedAOPService {
   
    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private VerticalsRepository verticalRepository;

    @Autowired
    private AopCalculationRepository aopCalculationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ScreenMappingRepository screenMappingRepository;

    public AOPMessageVM getProposedAOP(UUID plantId, String aopYear, UUID gradeId) {

        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();
        
        String procedureName = verticalName + "_" + siteName + "_GetProposedAOP";
        List<ProposedAOPDTO> proposedAOP = fetchProposedAOPFromProcedure(plantId, aopYear, gradeId, procedureName);

        Map<String, Object> map = new HashMap<>();

			List<AopCalculation> aopCalculation = aopCalculationRepository
					.findByPlantIdAndAopYearAndCalculationScreen(plantId, aopYear, "configuration");
			map.put("proposedAOP", proposedAOP);
			map.put("aopCalculation", aopCalculation);
        return AOPMessageVM.builder()
            .code(200)
            .message("Proposed AOPs fetched successfully")
            .data(map)
            .build();
    }
    
    public List<ProposedAOPDTO> fetchProposedAOPFromProcedure(UUID plantId, String aopYear, UUID gradeId, String procedureName) {

        String sql = "EXEC " + procedureName + " @plantId = ?, @aopYear = ?, @gradeId = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
            ProposedAOPDTO.builder()
                .id(rs.getString("Id" ) != null ? UUID.fromString(rs.getString("Id")) : null)
                .normParameterId(rs.getString("NormparameterId") != null ? UUID.fromString(rs.getString("NormparameterId")) : null)
                .normParameterTypeId(rs.getString("NormParameterTypeId") != null ? UUID.fromString(rs.getString("NormParameterTypeId")) : null)
                .normParameterTypeDisplayName(rs.getString("NormParameterTypeDisplayName"))
                .productName(rs.getString("ProductName"))
                .uom(rs.getString("UOM"))
                .lastFY(rs.getDouble("LastFY"))
                .sysGrn(rs.getDouble("SysGrn"))
                .proposed(rs.getDouble("Proposed"))
                .remarks(rs.getString("Remarks"))
                .plantId(rs.getString("PlantId") != null ? UUID.fromString(rs.getString("PlantId")) : null)
                .aopYear(rs.getString("AopYear"))
                .gradeId(rs.getString("GradeId") != null ? UUID.fromString(rs.getString("GradeId")) : null)
                .build(),
            plantId.toString(), aopYear, gradeId.toString()
        );
    }


    @Override
    @Transactional
    public AOPMessageVM saveProposedAOP(List<ProposedAOPDTO> dtoList) {
        try {
          
            List<ProposedAOPDTO> failedList = new ArrayList<>();

        
            for (ProposedAOPDTO dto : dtoList) {
       
                if(dto.getNormParameterId() == null || dto.getGradeId() == null || dto.getAopYear() == null) { 
                    throw new RuntimeException("NormParameterId, GradeId and AopYear are required");
                }
               
                    String updateSql = "UPDATE MCUNormsValueGrade " +
                        "SET April = ?, May = ?, June = ?, July = ?, August = ?, September = ?, " +
                        "October = ?, November = ?, December = ?, January = ?, February = ?, March = ?, Remarks = ? " +
                        "WHERE Material_FK_Id = ? and Grade_FK_Id = ? and FinancialYear = ?";
                    jdbcTemplate.update(updateSql,
                        dto.getProposed(), dto.getProposed(), dto.getProposed(), dto.getProposed(),
                        dto.getProposed(), dto.getProposed(), dto.getProposed(), dto.getProposed(),
                        dto.getProposed(), dto.getProposed(), dto.getProposed(), dto.getProposed(),
                        dto.getRemarks(),
                        dto.getNormParameterId(), dto.getGradeId(), dto.getAopYear());
                
            }
            AOPMessageVM vm = new AOPMessageVM();
            vm.setCode(200);
            vm.setMessage("Proposed AOP saved successfully");
            vm.setData(failedList);
            return vm;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save proposed AOP", e);
        }
    }

    @Override
    public AOPMessageVM calculateProposedAOP(UUID plantId, String aopYear) {
        
        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();

        String procedureName = verticalName + "_" + siteName + "_CalculateProposedAOP";

        Integer result = executeProposedAOPCalculationSP(String.valueOf(plantId), aopYear, procedureName);
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		aopMessageVM.setCode(200);
		aopMessageVM.setMessage("Calculate SP Executed successfully");
		aopMessageVM.setData(result);
		
		aopCalculationRepository.deleteByPlantIdAndAopYearAndCalculationScreen(plantId, aopYear,
				"configuration");
                
		List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("configuration");
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

    
	public Integer executeProposedAOPCalculationSP( String plantId, String aopYear, String procedureName) {
		try {

			String callSql = "{call " + "[" + procedureName + "]" + "(?, ?)}";


			return jdbcTemplate.update(callSql, plantId, aopYear);

		} catch (Exception e) {
			throw new RuntimeException("Failed to execute stored procedure", e);
		}
	}
}
