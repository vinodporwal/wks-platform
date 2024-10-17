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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
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
import com.wks.caseengine.rest.entity.CaseAndRecommendationsMapping;
//import com.wks.caseengine.rest.entity.Case;
//import com.wks.caseengine.rest.entity.CaseAndOwnerMapping;
import com.wks.caseengine.rest.entity.CaseCauseCategory;
import com.wks.caseengine.rest.entity.CaseCauseDescription;
import com.wks.caseengine.rest.entity.CaseDetails;
import com.wks.caseengine.rest.entity.CaseIdSequences;
import com.wks.caseengine.rest.entity.CaseStatus;
import com.wks.caseengine.rest.entity.CasesAndEventsMapping;
import com.wks.caseengine.rest.entity.EventCategory;
import com.wks.caseengine.rest.entity.EventEnrichment;
import com.wks.caseengine.rest.entity.Events;
import com.wks.caseengine.rest.entity.FaultCategory;
import com.wks.caseengine.rest.entity.FaultHistory;
import com.wks.caseengine.rest.entity.FunctionalLocation;
import com.wks.caseengine.rest.entity.OwnerDetails;
import com.wks.caseengine.rest.model.Attribute;
import com.wks.caseengine.rest.model.CaseContainer;
import com.wks.caseengine.rest.model.CasePayload;
import com.wks.caseengine.rest.model.FaultDetail;
import com.wks.caseengine.rest.model.FaultEvents;
import com.wks.caseengine.rest.model.Recommendations;
import com.wks.caseengine.rest.model.Users;
//import com.wks.caseengine.rest.repository.CaseAndOwnerMappingRepository;
import com.wks.caseengine.rest.repository.CaseCauseCategoryRepository;
import com.wks.caseengine.rest.repository.CaseCauseDescriptionRepository;
import com.wks.caseengine.rest.repository.CaseDetailsRepository;
import com.wks.caseengine.rest.repository.CaseIdSequenceRepository;
import com.wks.caseengine.rest.repository.CaseRecommendationMappingRepository;
import com.wks.caseengine.rest.repository.CaseRepository;
//import com.wks.caseengine.rest.repository.CaseRepository;
import com.wks.caseengine.rest.repository.CaseStatusRepository;
import com.wks.caseengine.rest.repository.CasesAndEventsMappingRepository;
import com.wks.caseengine.rest.repository.EquipmentsRepository;
import com.wks.caseengine.rest.repository.EventCategoryRepository;
import com.wks.caseengine.rest.repository.EventEnrichmentRepository;
import com.wks.caseengine.rest.repository.EventsRepository;
import com.wks.caseengine.rest.repository.FaultCategoryRepository;
import com.wks.caseengine.rest.repository.FaultHistoryRepository;
import com.wks.caseengine.rest.repository.FunctionalLocationRepository;

import io.netty.util.internal.ThreadLocalRandom;
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
    
    @Autowired
    private CaseIdSequenceRepository caseIdSequenceRepository;
    
    @Autowired
    private CasesAndEventsMappingRepository casesAndEventsMappingRepository;
    
    @Autowired
    private CaseRecommendationMappingRepository caseRecommendationMappingRepository;
//    
    @Autowired
    private EquipmentsRepository equipmentsRepository;
    
    @Autowired
    private FunctionalLocationRepository functionalLocationRepository;

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
		List<EventEnrichment> eventEnrichmentList = eventEnrichmentRepository.getAllEventEnrichmentsByIds(eventIds);
		List<FaultHistory> faultHistorys = faultHistoryRepository.getAllFaultHistoryFromEventIds(eventIds);
		String equipmentName = "";
		for(FaultHistory faultHistory: faultHistorys) {
			equipmentName = equipmentsRepository.findEquipmentName(faultHistory.getEquipmentPkId().toString());
			break;
		}
		List<FaultEvents> faultEvents = new ArrayList<FaultEvents>();
		
		for(EventEnrichment eventEnrichment: eventEnrichmentList) {
			UUID eventId = eventEnrichment.getEventPkId();
			UUID eventCategoryId = eventEnrichment.getEventCategoryPkId();
			
			Events event = eventsRepository.findByEventId(eventId);
			EventCategory eventCategory = eventCategoryRepository.getCategoryById(eventCategoryId);
			FaultEvents faultEvent = new FaultEvents();
			faultEvent.setEvent(event);
			faultEvent.setEventEnrichment(eventEnrichment);
			faultEvent.setEventCategory(eventCategory);
			faultEvent.setAssetName(equipmentName);
			faultEvents.add(faultEvent);
		}
		
		
		
		return faultEvents;
	}

	@Override
	public Case saveCase(Case caseData) {
		OwnerDetails owner = caseData.getOwner();
		String assetName = "%"+caseData.getAssetName();
		String hierarchyNodePKID = "";
		hierarchyNodePKID = caseRepository.gethierarchyNodePKID(assetName);
		if(assetName!=null) {
			
			
			System.out.println("hierarchyNodePKID: "+hierarchyNodePKID);
		}
		caseData.setHierarchyNodePKID(hierarchyNodePKID);
		Case caseDetails = new Case();
		String caseNo = caseData.getCaseNo();
		if(caseNo==null || caseNo.length()==0) {
			caseNo = CaseNoGenerator();
			caseData.setCaseNo(caseNo);
			caseDetails  = caseRepository.save(caseData);
			int i = 0;
			System.out.println("Saving New Case Details....");
			List<Long> eventIds = new ArrayList<Long>();
			for(String eventId: caseData.getEventIds()) {
				eventIds.add(Long.parseLong(eventId));
			}
			List<EventEnrichment> eventEnrichmentList = eventEnrichmentRepository.getAllEventEnrichmentsByIds(eventIds);
			HashMap<Long, String> map = new HashMap<Long, String>();
			for(EventEnrichment eventEnrichment: eventEnrichmentList) {
				map.put(eventEnrichment.getEventEnrichmentPkId().longValue(), eventEnrichment.getEventPkId().toString());
			}
			for(String eventId: caseData.getEventIds()) {
				CasesAndEventsMapping mapping = new CasesAndEventsMapping();
				mapping.setCaseNo(caseDetails.getCaseNo());
				casesAndEventsMappingRepository.save(mapping);
				System.out.println("EventId of: "+i+" is: "+ eventId +" for case No: "+ caseDetails.getCaseNo());
			}
			List<Attribute> attributes = caseDetails.getAttributes();
			for (Attribute attribute : attributes) {
			    String attributeName = attribute.getName();
			    String attributeValue = attribute.getValue();
			    
			    System.out.println("Attribute Name: " + attributeName);
			    System.out.println("Attribute Value: " + attributeValue);
			    saveRecommendations(attributeValue, caseNo);
			}
			
		} else {
			System.out.println("Saving Exsting Case Details....");
			caseDetails  = caseRepository.save(caseData);
		}
		return caseDetails;
	}
	
	private void saveRecommendations(String attributeValue, String caseNo) {
		attributeValue = attributeValue.replace("\\\"", "\"");

		System.out.println("Attribute Value: " + attributeValue);

		try {
		    ObjectMapper objectMapper = new ObjectMapper();
		    JsonNode rootNode = objectMapper.readTree(attributeValue);

		    // Navigate to the "dataGrid1" array
		    JsonNode recommendationNode = rootNode.path("dataGrid1");
		    if (recommendationNode.isArray()) {
		    	int counter = 0;
		        for (JsonNode dataGridEntry : recommendationNode) {
		            System.out.println("recommendationHeadline: " + dataGridEntry.path("recommendationHeadline").asText());
		            System.out.println("recommendationDescription1: " + dataGridEntry.path("recommendationDescription1").asText());
		            System.out.println("recommendationAssignedTo1: " + dataGridEntry.path("recommendationAssignedTo1").asText());
		            System.out.println("recommendationStatus: " + dataGridEntry.path("recommendationStatus").asText());
		            System.out.println("equipmentFunctionLocation: " + dataGridEntry.path("equipmentFunctionLocation").asText());
		            
		            System.out.println("recommendationTargetCompletionDate1: " + dataGridEntry.path("recommendationTargetCompletionDate1").asText());
		            System.out.println("recommendationReviewer: " + dataGridEntry.path("recommendationReviewer").asText());
		            System.out.println("recommendationNo1: " + dataGridEntry.path("recommendationNo1").asText());
		            System.out.println("RecommendationSubmit: " + dataGridEntry.path("RecommendationSubmit").asText());
		            System.out.println("recommendationAssignedTo2: " + dataGridEntry.path("recommendationAssignedTo2").asText());
		            
		            Recommendations recommendation = new Recommendations();
		            recommendation.setEquipmentFunctionLocation(dataGridEntry.path("equipmentFunctionLocation").asText());
		            recommendation.setRecommendationAssignedTo1(dataGridEntry.path("recommendationAssignedTo1").asText());
		            recommendation.setRecommendationAssignedTo2(dataGridEntry.path("recommendationAssignedTo2").asText());
		            recommendation.setRecommendationDescription1(dataGridEntry.path("recommendationDescription1").asText());
		            recommendation.setRecommendationHeadline(dataGridEntry.path("recommendationHeadline").asText());
		            recommendation.setRecommendationNo1(dataGridEntry.path("recommendationNo1").asText());
		            recommendation.setRecommendationReviewer(dataGridEntry.path("recommendationReviewer").asText());
		            recommendation.setRecommendationStatus(dataGridEntry.path("recommendationStatus").asText());
		            recommendation.setRecommendationSubmit(dataGridEntry.path("RecommendationSubmit").asText());
		            recommendation.setRecommendationTargetCompletionDate1(dataGridEntry.path("recommendationTargetCompletionDate1").asText());
		            
		            String recommendationString = saveRecommendationMapping(dataGridEntry, caseNo);
		            System.out.println(recommendationString);
		        }
		    }
		} catch(Exception e) {
		    e.printStackTrace();
		}
	}
	
	private String saveRecommendationMapping(JsonNode dataGridEntry, String caseNo) {
		String recId = saveRecommendationGEAPMApi(dataGridEntry, caseNo);
		CaseAndRecommendationsMapping caseRecommendationMapping = new CaseAndRecommendationsMapping();
		caseRecommendationMapping.setCaseNo(caseNo);
		caseRecommendationMapping.setRecId(recId);
		caseRecommendationMapping.setRecommendationJson(dataGridEntry.toPrettyString().toString());
		caseRecommendationMappingRepository.save(caseRecommendationMapping);
		
		return recId;
	}
	
	private String saveRecommendationGEAPMApi(JsonNode dataGridEntry, String caseNo) {
//		System.out.println("Calling Recommendation GEAPM API...");
//		System.out.println("Calling Recommendation GEAPM API...");
//		System.out.println(dataGridEntry.toPrettyString().toString());
//		
//		try {
//			URL url = new URL("https://your-api-url.com/endpoint");
//			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            conn.setRequestMethod("POST");
//            conn.setRequestProperty("Content-Type", "application/json; utf-8");
//            conn.setRequestProperty("Accept", "application/json");
//            conn.setDoOutput(true);
//            
//            String jsonInputString = "{"
//                    + "\"recommendationHeadline\":\"Headline\","
//                    + "\"recommendationDescription1\":\"Description\","
//                    + "\"recommendationAssignedTo1\":\"\","
//                    + "\"recommendationStatus\":\"\","
//                    + "\"equipmentFunctionLocation\":48,"
//                    + "\"recommendationTargetCompletionDate1\":\"2024-10-15T00:00:00+05:30\","
//                    + "\"recommendationReviewer\":\"Bhaumik.Darji@ril.com\","
//                    + "\"recommendationNo1\":\"\","
//                    + "\"RecommendationSubmit\":false,"
//                    + "\"recommendationAssignedTo2\":\"Balasubramanian.R.Iyer@ril.com\","
//                    + "\"RecommendationConfirm\":\"\""
//                    + "}";
//
//            // Send the request
//            try (OutputStream os = conn.getOutputStream()) {
//                byte[] input = jsonInputString.getBytes("utf-8");
//                os.write(input, 0, input.length);
//            }
//            
//         // Read the response
//            int responseCode = conn.getResponseCode();
//            System.out.println("Response Code: " + responseCode);
//
//            try (Scanner scanner = new Scanner(conn.getInputStream())) {
//                String responseBody = scanner.useDelimiter("\\A").next();
//                System.out.println("Response Body: " + responseBody);
//            }
//
//            conn.disconnect();
//		} catch(Exception  e) {
//			e.printStackTrace();
//		}
		 String prefix = "REC-";
	        
        // Generate a random number between 1 and 999999
        int randomNumber = ThreadLocalRandom.current().nextInt(1, 1000000);
        
        // Format the random number as a 6-digit string with leading zeros
        String formattedId = String.format("%06d", randomNumber);
        
        // Return the generated ID with the prefix
        return prefix + formattedId;
	}
	
	@Override
	public String CaseNoGenerator() {
		CaseIdSequences caseId = caseIdSequenceRepository.findLastElement();
		Long id = Long.parseLong(caseId.getCaseNo()) + 1;
		caseId.setCaseNo(id+"");
		caseIdSequenceRepository.save(caseId);
		return caseId.getCaseNo();
	}

	@Override
	public List<Case> getCaseDetails(String displayName, String hierarchyName) {
		List<String> assetsPKIds = caseDetailsRepository.findNodesByHierarchyNameAndDisplayName(displayName, hierarchyName);
//		for(String assetPKID: assetsPKIds) {
//			System.out.println(assetPKID);
//		}
		List<Case> cases = caseRepository.findAllByAssetsPKID(assetsPKIds);
		return cases;
	}
	
	@Override
	public List<Users> getUserList() {
		List<Users> users = new ArrayList<Users>();
		HashMap<String, Character> usersMap = new HashMap<String,Character>();
		usersMap.put("Balasaheb.Chadile@ril.com", 'A');
		usersMap.put("Balasubramanian,Krishnamoorthy@ril.com", 'A');
		usersMap.put("Balasubramanian.R.Iyer@ril.com", 'A');
		usersMap.put("Bhaumik.Darji@ril.com", 'A');
		usersMap.put("Bhautik.Kansara", 'A');
		 for (Map.Entry<String, Character> entry : usersMap.entrySet()) {
	            String email = entry.getKey();
	            char status = entry.getValue();
	            
	            // Create User object
	            Users user = new Users(email, status);
	            
	            // Print or use the user object as needed
	            System.out.println(user);
	            users.add(user);
	        }
		return users; 
	}
	
	@Override
	public List<FunctionalLocation> getFunctionalLocations(List<Long> eventIds) {
		List<FunctionalLocation> locations = new ArrayList<FunctionalLocation>();
		System.out.println(eventIds);
		List<FaultHistory> faultHistorys = faultHistoryRepository.getAllFaultHistoryFromEventIds(eventIds);
		String equipmentName = "";
		for(FaultHistory faultHistory: faultHistorys) {
			equipmentName = equipmentsRepository.findEquipmentName(faultHistory.getEquipmentPkId().toString());
			break;
		}
		locations = functionalLocationRepository.findByUasDisplayName(equipmentName);
		if(locations.size()==0) {
			locations = functionalLocationRepository.findAll();
		}
		return locations;
	}
}
