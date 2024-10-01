/*
 * WKS Platform - Open-Source Project
 * 
 * This file is part of the WKS Platform, an open-source project developed by WKS Power.
 * 
 * WKS Platform is licensed under the MIT License.
 * 
 * © 2021 WKS Power. All rights reserved.
 * 
 * For licensing information, see the LICENSE file in the root directory of the project.
 */
package com.wks.caseengine.cases.definition.service;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wks.caseengine.cases.definition.CaseDefinition;
import com.wks.caseengine.cases.definition.CaseDefinitionFilter;
import com.wks.caseengine.cases.definition.command.CreateCaseDefinitionCmd;
import com.wks.caseengine.cases.definition.command.DeleteCaseDefinitionCmd;
import com.wks.caseengine.cases.definition.command.FindCaseDefinitionCmd;
import com.wks.caseengine.cases.definition.command.GetCaseDefinitionCmd;
import com.wks.caseengine.cases.definition.command.UpdateCaseDefinitionCmd;
import com.wks.caseengine.command.CommandExecutor;
import com.wks.caseengine.rest.entity.Case;
//import com.wks.caseengine.rest.entity.Case;
//import com.wks.caseengine.rest.entity.CaseAndOwnerMapping;
import com.wks.caseengine.rest.entity.CaseCauseCategory;
import com.wks.caseengine.rest.entity.CaseCauseDescription;
import com.wks.caseengine.rest.entity.CaseDetails;
import com.wks.caseengine.rest.entity.CaseStatus;
import com.wks.caseengine.rest.entity.EventCategory;
import com.wks.caseengine.rest.entity.EventEnrichment;
import com.wks.caseengine.rest.entity.Events;
import com.wks.caseengine.rest.entity.FaultCategory;
import com.wks.caseengine.rest.entity.OwnerDetails;
import com.wks.caseengine.rest.model.Attribute;
import com.wks.caseengine.rest.model.CaseContainer;
import com.wks.caseengine.rest.model.CasePayload;
import com.wks.caseengine.rest.model.FaultDetail;
import com.wks.caseengine.rest.model.FaultEvents;
//import com.wks.caseengine.rest.repository.CaseAndOwnerMappingRepository;
import com.wks.caseengine.rest.repository.CaseCauseCategoryRepository;
import com.wks.caseengine.rest.repository.CaseCauseDescriptionRepository;
import com.wks.caseengine.rest.repository.CaseDetailsRepository;
import com.wks.caseengine.rest.repository.CaseRepository;
//import com.wks.caseengine.rest.repository.CaseRepository;
import com.wks.caseengine.rest.repository.CaseStatusRepository;
import com.wks.caseengine.rest.repository.EventCategoryRepository;
import com.wks.caseengine.rest.repository.EventEnrichmentRepository;
import com.wks.caseengine.rest.repository.EventsRepository;
import com.wks.caseengine.rest.repository.FaultCategoryRepository;
import com.wks.caseengine.rest.repository.FaultHistoryRepository;
//import com.wks.caseengine.rest.repository.OwnerDetailsRepository;

@Component
public class CaseDefinitionServiceImpl implements CaseDefinitionService {

	@Autowired
	private CommandExecutor commandExecutor;
	
	@Autowired
	private FaultCategoryRepository faultCategoryRepository; 
	
	@Autowired
	private CaseStatusRepository caseStatusRepository; 
	
	@Autowired
    private CaseCauseCategoryRepository categoryRepository;

    @Autowired
    private CaseCauseDescriptionRepository descriptionRepository;
    
    @Autowired
    private CaseDetailsRepository caseDetailsRepository;
    
    @Autowired
    private FaultHistoryRepository faultHistoryRepository; 
    
    @Autowired
    private CaseRepository caseRepository;
    
    @Autowired
    private EventEnrichmentRepository eventEnrichmentRepository;
    
    @Autowired
    private EventsRepository eventsRepository;
    
    @Autowired
    private EventCategoryRepository eventCategoryRepository;
    
//    @Autowired
//    private CaseAndOwnerMappingRepository caseAndOwnerMappingRepository;
//    
//    @Autowired
//    private OwnerDetailsRepository ownerDetailsRepository;

	@Override
	public List<CaseDefinition> find(final Optional<Boolean> deployed) {
		return commandExecutor.execute(
				new FindCaseDefinitionCmd(Optional.of(CaseDefinitionFilter.builder().deployed(deployed).build())));
	}

	@Override
	public CaseDefinition get(final String caseDefId) {
		return commandExecutor.execute(new GetCaseDefinitionCmd(caseDefId));
	}

	@Override
	public CaseDefinition create(final CaseDefinition caseDefinition) {
		if (caseDefinition.getId() == null || caseDefinition.getId().isEmpty()) {
			// TODO error handling
			throw new IllegalArgumentException("No Case Definition ID provided");
		}

		return commandExecutor.execute(new CreateCaseDefinitionCmd(caseDefinition));
	}

	@Override
	public CaseDefinition update(final String caseDefId, final CaseDefinition caseDefinition) {
		return commandExecutor.execute(new UpdateCaseDefinitionCmd(caseDefId, caseDefinition));
	}

	@Override
	public void delete(final String caseDefinitionId) {
		commandExecutor.execute(new DeleteCaseDefinitionCmd(caseDefinitionId));
	}
	
	@Override
	public List<FaultCategory> findCaseCatagories() {
		List<FaultCategory> faultCategoryList = faultCategoryRepository.findAll();
		return faultCategoryList;
	}
	
	@Override
	public List<CaseStatus> getAllCaseStatus() {
		List<CaseStatus> caseStatusList = caseStatusRepository.findAll();
		return caseStatusList;
	}
	
	public List<CaseCauseCategory> getAllCategories() {
		System.out.println("Calling... all categories");
        List<CaseCauseCategory> caseCauseCategory = categoryRepository.findAll();
        for(CaseCauseCategory casueCategory: caseCauseCategory) {
        	List<CaseCauseDescription> caseCauseDescptionList = new ArrayList<CaseCauseDescription>();
        	caseCauseDescptionList = descriptionRepository.findAllDescriptionByCategoryId(casueCategory.getId());
        	casueCategory.setDescriptions(caseCauseDescptionList);
        }
        return caseCauseCategory;
    }

    public List<CaseCauseDescription> getDescriptionsByCategory(Long categoryId) {
        Optional<CaseCauseCategory> category = categoryRepository.findById(categoryId);
        return category.map(descriptionRepository::findByCategory).orElse(Collections.emptyList());
    }

	@Override
	public CaseDetails saveCaseDetails(CasePayload casePayload) {
        CaseDetails caseDetails = new CaseDetails();
        
        // Map owner details if needed
        // Example: caseDetails.setCreatedBy(casePayload.getOwner().getName());

        // Parse attributes
        for (Attribute attribute : casePayload.getAttributes()) {
            if ("container".equals(attribute.getName()) && "Json".equals(attribute.getType())) {
                String jsonValue = attribute.getValue();
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    CaseContainer tempCaseDetails = objectMapper.readValue(jsonValue, CaseContainer.class);
                    
                    // Set fields to caseDetails from tempCaseDetails
                    caseDetails.setCaseNbr(tempCaseDetails.getCaseNo());
                    caseDetails.setTitle(tempCaseDetails.getCaseTitle());
                    caseDetails.setDescription(tempCaseDetails.getCaseDescription());
                    caseDetails.setCreatedAt(Date.from(tempCaseDetails.getCreatedOn().atZone(ZoneId.systemDefault()).toInstant()));
                    caseDetails.setDueAt(Date.from(tempCaseDetails.getDueDate().atZone(ZoneId.systemDefault()).toInstant()));
                    caseDetails.setClosedAt(Date.from(tempCaseDetails.getEndDate().atZone(ZoneId.systemDefault()).toInstant()));
                    caseDetails.setStatus("New"); // Example default status
                    caseDetails.setCaseCategory(tempCaseDetails.getValueRealizationCategory());
                    caseDetails.setJustification(tempCaseDetails.getValueRealizationConclusion());
                    caseDetails.setImpactExpectedSavings(tempCaseDetails.getTotalValueCaptured());
                    caseDetails.setImpactImplementationCost(tempCaseDetails.getProductionLoss());
                    caseDetails.setImpactProduction(tempCaseDetails.getManHoursCost());
                    caseDetails.setImpactEfforts(tempCaseDetails.getSpareCost());
//                    caseDetails.setTrackingSystem(tempCaseDetails.getTrackingSystem()); // If exists in your JSON
                    caseDetails.setAssignedTo(tempCaseDetails.getCaseAssignTo()); // If exists in your JSON
                    
                    // Add fault details if needed
                    if (tempCaseDetails.getDataGrid() != null) {
                        List<FaultDetail> faultDetailsList = new ArrayList<>();
                        for (FaultDetail fault : tempCaseDetails.getDataGrid()) {
                            faultDetailsList.add(fault);
                        }
                        // Save or process fault details as necessary
                    }
                } catch (Exception e) {
                    // Handle JSON parsing exception
                    e.printStackTrace();
                }
            }
        }
        System.out.println(caseDetails);
        System.out.println("Saving case details");
        System.out.println("Saving case details");
        System.out.println("Saving case details");
        System.out.println("Saving case details");
        
        return null;
    }

	@Override
	public List<FaultEvents> getAllEvents(List<Long> eventIds) {
		System.out.println("Calling repository method..");
		List<EventEnrichment> eventEnrichmentList = eventEnrichmentRepository.getAllEventEnrichmentsByIds(eventIds);
		List<FaultEvents> faultEvents = new ArrayList<FaultEvents>();
		for(EventEnrichment eventEnrichment: eventEnrichmentList) {
			UUID eventId = eventEnrichment.getEventPkId();
			UUID eventCategoryId = eventEnrichment.getEventCategoryPkId();
			
			System.out.println("Event Ids: "+ eventId);
			System.out.println("Event Categories Ids: "+ eventCategoryId);
			Events event = eventsRepository.findByEventId(eventId);
			EventCategory eventCategory = eventCategoryRepository.getCategoryById(eventCategoryId);
			FaultEvents faultEvent = new FaultEvents();
			faultEvent.setEvent(event);
			faultEvent.setEventEnrichment(eventEnrichment);
			faultEvent.setEventCategory(eventCategory);
			faultEvents.add(faultEvent);
		}
		return faultEvents;
	}

	@Override
	public Case saveCase(Case caseData) {
		OwnerDetails owner = caseData.getOwner();
		System.out.println(owner.getEmail());
		System.out.println(owner.getName());
		System.out.println(owner.getPhone());
		System.out.println(owner.getId());
		Case caseDetails  = caseRepository.save(caseData);
		return caseDetails;
	}

}
