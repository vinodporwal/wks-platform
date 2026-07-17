package com.wks.caseengine.crude.serviceimpl;

import com.wks.caseengine.crude.service.SteadyStateService;
import com.wks.caseengine.entity.AopCalculation;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.ScreenMapping;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import com.wks.caseengine.repository.AopCalculationRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.ScreenMappingRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class SteadyStateServiceImpl implements SteadyStateService {

    @PersistenceContext
	private EntityManager entityManager;

	@Autowired
	PlantsRepository plantsRepository;

	@Autowired
	SiteRepository siteRepository;

	@Autowired
	VerticalsRepository verticalRepository;

    @Autowired
	private ScreenMappingRepository screenMappingRepository;

	@Autowired
	private AopCalculationRepository aopCalculationRepository;
    

    @Override
	@Transactional
	public AOPMessageVM calculateExpressionConsumptionNorms(String year, String plantId) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		Plants plant = plantsRepository.findById(UUID.fromString(plantId)).get();
		Sites site = siteRepository.findById(plant.getSiteFkId()).get();
		Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();

		
    //CRUDE_DTA_CDU1_SteadyStateCalculation
		String storedProcedure = vertical.getName() + "_" + site.getName() + "_" + plant.getName() +"_SteadyStateCalculation";  
		

		System.out.println("storedProcedure" + storedProcedure);
		String errorMessage = executeDynamicUpdateProcedure(storedProcedure, plantId, site.getId().toString(),
				vertical.getId().toString(), year);


                if(errorMessage != null) {
                    aopMessageVM.setCode(422);
                    aopMessageVM.setMessage(errorMessage);
                    return aopMessageVM;
                }


		aopCalculationRepository.deleteByPlantIdAndAopYearAndCalculationScreen(UUID.fromString(plantId), year,
				"normal-op-norms");
		List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("normal-op-norms");
		for (ScreenMapping screenMapping : screenMappingList) {
			if (!screenMapping.getCalculationScreen().equalsIgnoreCase(screenMapping.getDependentScreen())) {
				AopCalculation aopCalculation = new AopCalculation();
				aopCalculation.setAopYear(year);
				aopCalculation.setIsChanged(true);
				aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
				aopCalculation.setPlantId(UUID.fromString(plantId));
				aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
				aopCalculationRepository.save(aopCalculation);
			}
		}
		List<ScreenMapping> calculateScreenMappingList = screenMappingRepository
				.findByDependentScreen("normal-op-norms-calculate");
		for (ScreenMapping screenMapping : calculateScreenMappingList) {
			if (!screenMapping.getCalculationScreen().equalsIgnoreCase(screenMapping.getDependentScreen())) {
				AopCalculation aopCalculation = new AopCalculation();
				aopCalculation.setAopYear(year);
				aopCalculation.setIsChanged(true);
				aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
				aopCalculation.setPlantId(UUID.fromString(plantId));
				aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
				aopCalculationRepository.save(aopCalculation);
			}
		}

 
		aopMessageVM.setCode(200);
		aopMessageVM.setMessage("SP Executed successfully");
	//	aopMessageVM.setData(result);
		return aopMessageVM;
	}

    public String executeDynamicUpdateProcedure(String procedureName, String plantId, String siteId, String verticalId,
        String finYear) {
      
            try {
                // Safeguard procedure name by wrapping it in square brackets if it isn't already
                String sanitizedProcedureName = procedureName;
                if (!sanitizedProcedureName.startsWith("[") && !sanitizedProcedureName.endsWith("]")) {
                    sanitizedProcedureName = "[" + sanitizedProcedureName + "]";
                }

                StoredProcedureQuery query = entityManager
                        .createStoredProcedureQuery(sanitizedProcedureName);
        
                // Input parameters
                query.registerStoredProcedureParameter("plantId", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("siteid", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("verticalId", String.class, ParameterMode.IN);
                query.registerStoredProcedureParameter("finYear", String.class, ParameterMode.IN);

                // OUTPUT parameter
                query.registerStoredProcedureParameter("ErrorMessage", String.class, ParameterMode.OUT);
        
                query.setParameter("plantId", plantId.toString());
                query.setParameter("siteid", siteId.toString());
                query.setParameter("verticalId", verticalId.toString());
                query.setParameter("finYear", finYear);
        
                query.execute();
        
                try {
                    query.getResultList(); // flush any pending result sets
                } catch (Exception ignored) {}
        
                String errorMessage = (String) query.getOutputParameterValue("ErrorMessage");
        
                System.out.println("errorMessage string: " + errorMessage);
        
                return errorMessage;
        
            } catch (IllegalArgumentException e) {
                throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to execute procedure", ex);
            }
}

}
