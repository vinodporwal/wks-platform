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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wks.caseengine.cases.definition.CaseDefinition;
import com.wks.caseengine.cases.definition.CaseDefinitionFilter;
import com.wks.caseengine.cases.definition.command.CreateCaseDefinitionCmd;
import com.wks.caseengine.cases.definition.command.DeleteCaseDefinitionCmd;
import com.wks.caseengine.cases.definition.command.FindCaseDefinitionCmd;
import com.wks.caseengine.cases.definition.command.GetCaseDefinitionCmd;
import com.wks.caseengine.cases.definition.command.UpdateCaseDefinitionCmd;
import com.wks.caseengine.cases.instance.email.CaseEmailServiceImpl;
import com.wks.caseengine.command.CommandExecutor;
import com.wks.caseengine.rest.db1.repository.EventEnrichmentRepository;
import com.wks.caseengine.rest.db2.entity.Case;
import com.wks.caseengine.rest.db2.entity.CaseAndRecommendationsMapping;
import com.wks.caseengine.rest.db2.entity.CaseCauseCategory;
import com.wks.caseengine.rest.db2.entity.CaseCauseDescription;
import com.wks.caseengine.rest.db2.entity.CaseIdSequences;
import com.wks.caseengine.rest.db2.entity.CaseStatus;
import com.wks.caseengine.rest.db2.entity.CasesAndEventsMapping;
import com.wks.caseengine.rest.db2.entity.FaultCategory;
import com.wks.caseengine.rest.db2.entity.OwnerDetails;
import com.wks.caseengine.rest.db2.repository.CaseCauseCategoryRepository;
import com.wks.caseengine.rest.db2.repository.CaseCauseDescriptionRepository;
import com.wks.caseengine.rest.db2.repository.CaseIdSequenceRepository;
import com.wks.caseengine.rest.db2.repository.CaseRecommendationMappingRepository;
import com.wks.caseengine.rest.db2.repository.CaseRepository;
import com.wks.caseengine.rest.db2.repository.CaseStatusRepository;
import com.wks.caseengine.rest.db2.repository.CasesAndEventsMappingRepository;
import com.wks.caseengine.rest.db2.repository.FaultCategoryRepository;
import com.wks.caseengine.rest.db2.repository.UsersRepository;
import com.wks.caseengine.rest.model.Attribute;
import com.wks.caseengine.rest.model.EquipmentModel;
import com.wks.caseengine.rest.model.EventCategoryModel;
import com.wks.caseengine.rest.model.EventEnrichmentModel;
import com.wks.caseengine.rest.model.EventsModel;
import com.wks.caseengine.rest.model.FaultEvents;
import com.wks.caseengine.rest.model.FaultHistoryModel;
import com.wks.caseengine.rest.model.FunctionalLocation;
import com.wks.caseengine.rest.model.HierarchyNodesModel;
import com.wks.caseengine.rest.model.Recommendations;
import com.wks.caseengine.rest.model.Users;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Component
public class CaseDefinitionServiceImpl implements CaseDefinitionService {
	
	private final JavaMailSender mailSender;

    @Autowired
    public CaseDefinitionServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

	@Autowired
	private CommandExecutor commandExecutor;
	
	@Autowired
	private FaultCategoryRepository faultCategoryRepository; 
//	
	@Autowired
	private CaseStatusRepository caseStatusRepository; 
	
	@Autowired
    private CaseCauseCategoryRepository categoryRepository;

    @Autowired
    private CaseCauseDescriptionRepository descriptionRepository;
//    
//    @Autowired
//    private CaseDetailsRepository caseDetailsRepository;
//    
//    @Autowired
//    private FaultHistoryRepository faultHistoryRepository; 
    
    @Autowired
    private CaseRepository caseRepository;
//    
    @Autowired
    private EventEnrichmentRepository eventEnrichmentRepository;
    
    @Autowired
    private FetchRecordsServiceImpl fetchRecords;
//    
//    @Autowired
//    private EventsRepository eventsRepository;
//    
//    @Autowired
//    private EventCategoryRepository eventCategoryRepository;
//    
    @Autowired
    private CaseIdSequenceRepository caseIdSequenceRepository;
    
    @Autowired
    private CasesAndEventsMappingRepository casesAndEventsMappingRepository;
    
    @Autowired
    private CaseRecommendationMappingRepository caseRecommendationMappingRepository;
    
    @Autowired
    private CaseEmailServiceImpl caseEmailService;

    
//    @Autowired
//    private FunctionalLocationRepository functionalLocationRepository;

    
    @Autowired
    private UsersRepository usersRepository;

	@Value("${spring.mail.fromEmail}")
	private String from;
    @PersistenceContext(unitName = "db2")
    private EntityManager entityManager;

    
 	@Value("${spring.datasource.db1.name}")
    private String db1Name;
 	@Value("${ge.authentication.datasource}")
    private String geAuthenticationDatasource;
 	@Value("${ge.authentication.id}")
    private String geAuthenticationId;
 	@Value("${ge.authentication.password}")
    private String geAuthenticationPassword;
 	@Value("${ge.authentication.api}")
    private String geAuthenticationAPI;
 	@Value("${ge.users.api}")
    private String geUsersAPI;
 	@Value("${ge.create_case.api}")
    private String geCreateCaseAPI;
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
        return caseCauseCategory;
    }

    public List<CaseCauseDescription> getDescriptionsByCategory(Long categoryId) {
        List<CaseCauseDescription> caseCauseDescriptions = descriptionRepository.findAllDescriptionByCategoryId(categoryId);
        return caseCauseDescriptions;
    }

//	@Override
//	public CaseDetails saveCaseDetails(CasePayload casePayload) {
//        CaseDetails caseDetails = new CaseDetails();
//        
//        // Map owner details if needed
//        // Example: caseDetails.setCreatedBy(casePayload.getOwner().getName());
//
//        // Parse attributes
//        for (Attribute attribute : casePayload.getAttributes()) {
//            if ("container".equals(attribute.getName()) && "Json".equals(attribute.getType())) {
//                String jsonValue = attribute.getValue();
//                try {
//                    ObjectMapper objectMapper = new ObjectMapper();
//                    CaseContainer tempCaseDetails = objectMapper.readValue(jsonValue, CaseContainer.class);
//                    
//                    // Set fields to caseDetails from tempCaseDetails
//                    caseDetails.setCaseNbr(tempCaseDetails.getCaseNo());
//                    caseDetails.setTitle(tempCaseDetails.getCaseTitle());
//                    caseDetails.setDescription(tempCaseDetails.getCaseDescription());
//                    caseDetails.setCreatedAt(Date.from(tempCaseDetails.getCreatedOn().atZone(ZoneId.systemDefault()).toInstant()));
//                    caseDetails.setDueAt(Date.from(tempCaseDetails.getDueDate().atZone(ZoneId.systemDefault()).toInstant()));
//                    caseDetails.setClosedAt(Date.from(tempCaseDetails.getEndDate().atZone(ZoneId.systemDefault()).toInstant()));
//                    caseDetails.setStatus("New"); // Example default status
//                    caseDetails.setCaseCategory(tempCaseDetails.getValueRealizationCategory());
//                    caseDetails.setJustification(tempCaseDetails.getValueRealizationConclusion());
//                    caseDetails.setImpactExpectedSavings(tempCaseDetails.getTotalValueCaptured());
//                    caseDetails.setImpactImplementationCost(tempCaseDetails.getProductionLoss());
//                    caseDetails.setImpactProduction(tempCaseDetails.getManHoursCost());
//                    caseDetails.setImpactEfforts(tempCaseDetails.getSpareCost());
////                    caseDetails.setTrackingSystem(tempCaseDetails.getTrackingSystem()); // If exists in your JSON
//                    caseDetails.setAssignedTo(tempCaseDetails.getCaseAssignTo()); // If exists in your JSON
//                    
//                    // Add fault details if needed
//                    if (tempCaseDetails.getDataGrid() != null) {
//                        List<FaultDetail> faultDetailsList = new ArrayList<>();
//                        for (FaultDetail fault : tempCaseDetails.getDataGrid()) {
//                            faultDetailsList.add(fault);
//                        }
//                        // Save or process fault details as necessary
//                    }
//                } catch (Exception e) {
//                    // Handle JSON parsing exception
//                    e.printStackTrace();
//                }
//            }
//        }
//        System.out.println(caseDetails);
//        System.out.println("Saving case details");
//        System.out.println("Saving case details");
//        System.out.println("Saving case details");
//        System.out.println("Saving case details");
//        
//        return null;
//    }
//
	@Override
	public List<FaultEvents> getAllEvents(List<Long> eventIds) {
		System.out.println(eventIds);
		System.out.println(eventIds.get(0));
		List<FaultHistoryModel> faultHistorys = fetchRecords.getFaultHistories(eventIds); 
		String equipmentDisplayName = "";
		String equipmentName = "";
		List<Long> eventEnrichmentsPKIds = new ArrayList<>();
		for(FaultHistoryModel faultHistory: faultHistorys) {
			List<EquipmentModel> equipemnets = fetchRecords.getEquipmentName(faultHistory.getEquipmentPkId());
			equipmentDisplayName = equipemnets.get(0).getDisplayName();
			equipmentName = equipemnets.get(0).getName();
			break;
		}
		for(FaultHistoryModel faultHistory: faultHistorys) {
			String eventEnrichmentPkIdStr = faultHistory.getEventEnrichmentPkId();
			if(!eventEnrichmentPkIdStr.isEmpty()) {
				long eventEnrichmentPkId = Long.parseLong(eventEnrichmentPkIdStr);
				eventEnrichmentsPKIds.add(eventEnrichmentPkId);
			}
		}
		List<FaultEvents> faultEvents = new ArrayList<FaultEvents>();
		
		List<EventEnrichmentModel> eventEnrichments = fetchRecords.getEventEnrichments(eventEnrichmentsPKIds);
		for(EventEnrichmentModel eventEnrichment: eventEnrichments) {
			String eventId = eventEnrichment.getEventPkId();
			String eventCategoryId = eventEnrichment.getEventCategoryPkId();
			
			List<EventsModel> events = fetchRecords.findEventsByEventId(eventId);
			EventsModel event = new EventsModel();
			if(events.size()>=1) {
				event = events.get(0);
			}
			System.out.println("Events List");
			System.out.println(eventIds.get(0));
			System.out.println(eventId);
			for(EventsModel event1: events) {
				System.out.println(event1.getEventName());
			}
			List<EventCategoryModel> eventCategorys = fetchRecords.getCategoryByCategoryId(eventCategoryId);
			EventCategoryModel eventCategory = new EventCategoryModel();
			if(eventCategorys.size()>=1) {
				eventCategory = eventCategorys.get(0);
			}
			FaultEvents faultEvent = new FaultEvents();
			faultEvent.setEvents(event);
			faultEvent.setEventEnrichment(eventEnrichment);
			faultEvent.setEventCategory(eventCategory);
			faultEvent.setAssetName(equipmentName);
			faultEvent.setAssetDisplayName(equipmentDisplayName);
			faultEvents.add(faultEvent);
		}
		return faultEvents;
	}
	
	

	@Override
	public Case saveCase(Case caseData) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		String currentDate = now.format(formatter);
		OwnerDetails owner = caseData.getOwner();
		String assetName = "%"+caseData.getAssetName();
		String hierarchyNodePKID = "";
		List<HierarchyNodesModel> hierarychyNodes = fetchRecords.gethierarchyNodePKID(assetName);
		if(hierarychyNodes.size()>=1) {
			hierarchyNodePKID = hierarychyNodes.get(0).getHierarchyNodePkId();
		}
		if(assetName!=null) {
			
			
			System.out.println("hierarchyNodePKID: "+hierarchyNodePKID);
		}
		caseData.setHierarchyNodePKID(hierarchyNodePKID);
		Case caseDetails = new Case();
		String caseNo = "";
		Long statusId = null;
		List<Attribute> attributes = caseData.getAttributes();
		Attribute attribute = attributes.get(0);
		String attributeValue = attribute.getValue();
		try {
		    ObjectMapper objectMapper = new ObjectMapper();
		    JsonNode rootNode = objectMapper.readTree(attributeValue);
		    caseNo = rootNode.path("caseNo").asText();
		    if (rootNode.has("caseStatus")) {
			    statusId = rootNode.path("caseStatus").asLong();
		    }
		    
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		if(statusId!=null) {
			Optional<CaseStatus> caseStatus =  caseStatusRepository.findById(statusId);
			if(caseStatus.isPresent()) {
				caseData.setStatus(caseStatus.get());
			}
			
		}
		
		System.out.println("Printing Payload...");
		System.out.println("Getting data..");
		System.out.println("Is Draft :"+caseData.getIsDraft());
		System.out.println("**************************************IN SAVE CASE*************************************************");
		if(caseNo==null || caseNo.length()==0) {
			caseNo = CaseNoGenerator();
			caseData.setCaseNo(caseNo);
			caseData.setCaseUrl(caseData.getCaseUrl()+"&caseNo="+caseNo);
			System.out.println("Saving New Case Details....");
			caseData.setCreationDate(currentDate);
			caseDetails  = caseRepository.save(caseData);
			
			List<Long> eventIds = new ArrayList<Long>();
			for(String eventId: caseData.getEventIds()) {
				eventIds.add(Long.parseLong(eventId));
			}
			System.out.println(eventIds);
			HashMap<String, String> map = new HashMap<String, String>();
			for(String eventId: caseData.getEventIds()) {
				CasesAndEventsMapping mapping = new CasesAndEventsMapping();
				mapping.setCaseNo(caseDetails.getCaseNo());
				casesAndEventsMappingRepository.save(mapping);
				System.out.println("EventId of is: "+ eventId +" for case No: "+ caseDetails.getCaseNo());
			}
			
		} else {
			System.out.println("Saving Exsting Case Details....");
			caseData.setCaseNo(caseNo);
			if(caseData.getCaseUrl()!=null && !caseData.getCaseUrl().contains("&caseNo")) {
				caseData.setCaseUrl(caseData.getCaseUrl()+"&caseNo="+caseNo);
			}
			Case savedCase =caseRepository.getByCaseNo(caseNo);
			caseData.setCreationDate(savedCase.getCreationDate());
			caseDetails  = caseRepository.save(caseData);
		}
		
		//sending Emails part
		System.out.println("**************************************sending Emails part*************************************************");
		System.out.println("************************************ Is Draft"+ caseData.getIsDraft());
		if(!caseData.getIsDraft().equals("y")) {
			attributeValue = attributeValue.replace("\\\"", "\"");
			System.out.println("Attribute Value: " + attributeValue);
	
			try {
			    ObjectMapper objectMapper = new ObjectMapper();
			    JsonNode rootNode = objectMapper.readTree(attributeValue);
			    String assignedTo = rootNode.path("caseAssignedTo").asText();
			    String caseNumber = caseData.getCaseNo();
			    String caseTitle = rootNode.path("caseTitle").asText();
			    System.out.println(rootNode.path("caseAssignedTo").asText());
			    Long caseStatusNo = rootNode.path("caseStatus").asLong();
			    Optional<CaseStatus> caseStatus = getAllCaseStatus().stream()
			    	    .filter(status -> status.getId().equals(caseStatusNo))
			    	    .findFirst();
			    String caseStatusValue = caseStatus.get().getName();
			    JsonNode analysisTeam = rootNode.path("analysisTeam");
			    String[] reviewers = new String[analysisTeam.size()];
			    if (analysisTeam.isArray()) {
			    	int counter = 0;
			        for (JsonNode dataGridEntry : analysisTeam) {
			        	reviewers[counter] = dataGridEntry.asText();
			        	counter++;
			        }
			        
				}
			    if(!caseStatusValue.equals("Under Analysis")) {
			    	System.out.println("Calling mail send method...");
//			    	String from = "amol.borse@honeywell.com";
			    	Map<String, Object> data = new HashMap<>();
			    	data.put("caseTitle", "This is to inform you, the new case has been assined to you");
					data.put("caseNumber", caseNumber);
					data.put("status", caseStatusValue);
					data.put("caseName", caseTitle);
					data.put("caseUrl", caseDetails.getCaseUrl());
					data.put("environment", "");
			    	caseTitle = "CASE MANAGEMENT :"+ caseTitle;
			    	caseEmailService.send(from, assignedTo, caseTitle, reviewers, null, null, "email-template", data);
			    	//(assignedTo, caseNumber, caseTitle, caseStatusValue, reviewers);
			    }
			    
			    int i = 0;
			    String attributeName = attribute.getName();
			    
			    System.out.println("Attribute Name: " + attributeName);
			    System.out.println("Attribute Value: " + attributeValue);
				System.out.println("After Updating Attributes...");
				System.out.println(attributes.get(0).getValue());
				caseData.setAttributes(attributes);
				caseDetails = caseRepository.save(caseData);
				return caseDetails;
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		return caseDetails;
	}
	
	private String saveRecommendations(String attributeValue, String caseNo) {
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
		            
		            String[] recommendationStatusAndId = saveRecommendationMapping(dataGridEntry, caseNo, recommendation.getRecommendationAssignedTo2(), recommendation.getRecommendationReviewer());
		            System.out.println("GEPM Recommendation ID: "+recommendationStatusAndId[0]);
		            System.out.println("GEPM Recommendation Status: "+recommendationStatusAndId[1]);
		            ((ObjectNode) dataGridEntry).put("recommendationNo1", recommendationStatusAndId[0]);
		            ((ObjectNode) dataGridEntry).put("recommendationStatus", recommendationStatusAndId[1]);
		            
		            System.out.println("Updated recommendationAssignedTo2: " + dataGridEntry.path("recommendationAssignedTo2").asText());
		        }
		    }
		    String updatedAttributeValue = objectMapper.writeValueAsString(rootNode);
            System.out.println("Updated Attribute Value: " + updatedAttributeValue);
            return updatedAttributeValue;
		} catch(Exception e) {
		    e.printStackTrace();
		}
		return null;
	}
	
	private String[] saveRecommendationMapping(JsonNode dataGridEntry, String caseNo, String assignedUserId, String reviewerUserId) {
		String[] recommendationStatusAndId = saveRecommendationGEAPMApi(dataGridEntry, caseNo, assignedUserId, reviewerUserId);
		CaseAndRecommendationsMapping caseRecommendationMapping = new CaseAndRecommendationsMapping();
		caseRecommendationMapping.setCaseNo(caseNo);
		caseRecommendationMapping.setRecId(recommendationStatusAndId[0]);
		caseRecommendationMapping.setRecommendationJson(dataGridEntry.toPrettyString().toString());
		caseRecommendationMappingRepository.save(caseRecommendationMapping);
		
		return recommendationStatusAndId;
	}
	
	private String[] saveRecommendationGEAPMApi(JsonNode dataGridEntry, String caseNo, String assignedUserId, String reviewerUserId) {
		System.out.println("Calling Recommendation GEAPM API...");
		System.out.println("Calling Recommendation GEAPM API...");
		System.out.println(dataGridEntry.toPrettyString().toString());
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
		 String geAPMAcsessToken = geLogin();
		 System.out.println("GE APM Acsess Token: " + geAPMAcsessToken);
		 RestTemplate restTemplate = new RestTemplate();
		 HttpHeaders headers = new HttpHeaders();
		 headers.setContentType(MediaType.APPLICATION_JSON);
		 headers.add("Meridium Token", geAPMAcsessToken);
         Map<String, Object> requestBody = new HashMap<>();
         requestBody.put("Auther_Domain_Id", dataGridEntry.get("recommendationAssignedTo2"));
         requestBody.put("Pending_Approval_Domain_Id", "MIADMIN");
         requestBody.put("Approved_Domain_Id", dataGridEntry.get("recommendationReviewer"));
         requestBody.put("RECOMMENDATION_Des", dataGridEntry.get("recommendationDescription1"));
         requestBody.put("MI_REC_BASIS", dataGridEntry.get("recommendationHeadline"));
         requestBody.put("MI_REC_LOC_ID_CHR", dataGridEntry.get("equipmentFunctionLocation"));
         requestBody.put("MI_REC_LONG_DESCR_TX", dataGridEntry.get("recommendationDescription1"));
         requestBody.put("MI_REC_TARGE_COMPL_DATE_DT", dataGridEntry.get("recommendationTargetCompletionDate1")) ;
         requestBody.put("MI_REC_PRIORITY_C", "2");
         requestBody.put("CC_REC_CREAT_SAP_REQUE_L", dataGridEntry.get("RecommendationConfirmSAP3"));
         requestBody.put("CaseID", caseNo);
		 System.out.println("GE APM Create Case body: " + requestBody.toString());
         HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
		 ResponseEntity<Map> response = restTemplate.postForEntity(geCreateCaseAPI, requestEntity, Map.class);
		 System.out.println("Response Code: " + response.getStatusCode());
		 System.out.println("Response Body: " + response.getBody());
		 String prefix = "REC-";
		 sendMailToAssignedPerson(assignedUserId);
		 sendMailToReviewerPerson(reviewerUserId);
	        
        // Generate a random number between 1 and 999999
        int randomNumber = ThreadLocalRandom.current().nextInt(1, 1000000);
        
        // Format the random number as a 6-digit string with leading zeros
        String formattedId = String.format("%06d", randomNumber);
        
        // Return the generated ID with the prefix
        String id = prefix + formattedId;
        String status = "Assigned";
        String[] recommendationStatusAndId = new String[2];
        recommendationStatusAndId[0] = id;
        recommendationStatusAndId[1] = status;
        return recommendationStatusAndId;
	}
	
	public void sendMailToAssignedPerson(String assignedUserId) {
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setTo("shrikantp2143@gmail.com");
//        message.setSubject("New Case has been assinged to you");
//        message.setText("This is to inform you, the new case has been assined to you.");
//        message.setFrom("shrikant.mnt@gmail.com");
//
//        try {
//            mailSender.send(message);
//            System.out.println("Email sent successfully!");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
	}
	
	public void sendMailToReviewerPerson(String reviewerUserId) {
//		SimpleMailMessage message = new SimpleMailMessage();
//        message.setTo("shrikantp2143@gmail.com");
//        message.setSubject("New Case has been assinged to you for review");
//        message.setText("This is to inform you, the new case has been assined to you for review");
//        message.setFrom("shrikant.mnt@gmail.com");
//
//        try {
//            mailSender.send(message);
//            System.out.println("Email sent successfully!");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
	}
	
	private void sendMailToAssignedPerson(String assignedTo, String caseNo, String caseTitle, String status, String[] reviewers) {
		try {
			System.out.println("In mail send method...");
			SimpleMailMessage message = new SimpleMailMessage();
	        message.setTo(assignedTo);
	        message.setCc(reviewers);
	        message.setSubject("New Case: "+ caseTitle);
	        message.setText("This is to inform you, the new case Case Number: " +caseNo + ", \n Case Title: "+caseTitle+", has been assined to you\n Case Status: "+status);
	        message.setFrom(from);
	        mailSender.send(message);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		// List<String> assetsPKIds = fetchRecords.findNodesByHierarchyNameAndDisplayName(displayName, hierarchyName);
		// String result = assetsPKIds.stream()
		// 	    .map(id -> "\'" + id + "\'") // Add quotes to each item
		// 	    .collect(Collectors.joining(", ")); // Join with a comma and space
		// List<Case> cases = caseRepository.findAllByAssetsPKID(assetsPKIds);
       String query = "SELECT c.* FROM [CaseManagement].[dbo].[Cases] c " +
                       "WHERE TRY_CAST(c.hierarchy_node_pk_id AS UNIQUEIDENTIFIER) IN (" +
                       "SELECT hn.HierarchyNode_PK_ID " +
                       "FROM [" + db1Name + "].[dbo].[HierarchyNodes] hn " +
                       "JOIN [" + db1Name + "].[dbo].[HierarchyTrees] ht " +
                       "ON hn.HierarchyTree_PK_ID = ht.HierarchyTree_PK_ID " +
                       "WHERE hn.IsDeleted = 0 " +
                       "AND hn.Path LIKE CONCAT('%', :assetName, '%') " +
                       "AND ht.HierarchyType = :hierarchyName" +
                       ") ORDER BY c.case_no DESC";
        Query nativeQuery = entityManager.createNativeQuery(query, Case.class);
        nativeQuery.setParameter("assetName", displayName);
        nativeQuery.setParameter("hierarchyName", hierarchyName);
        return nativeQuery.getResultList();
		// return cases;
	}
	
	@Override
	public List<Case> getCaseDetails(LocalDate from, LocalDate to, String status) {
//		String searchQueryStr = "select * FROM Cases";
		String searchQueryStr = "SELECT c.* FROM cases c LEFT JOIN case_status cs ON c.status_id = cs.id";
		ArrayList<String> conditions = new ArrayList<>();
		if(from != null) {
			conditions.add("CAST(c.creation_date AS DATE) >= '"+from+"'");
		}
		
		if(to != null) {
			conditions.add("CAST(c.creation_date AS DATE) <= '"+to+"'");
		}
		
		if(status!=null && !status.isBlank()) {
			if(status.equalsIgnoreCase("open")) {
				conditions.add("c.status_id in (1,2)");
			}else if(status.equalsIgnoreCase("close")) {
				conditions.add("c.status_id in (3)");
			}
		}
		
		if(conditions.size()>0) {
			searchQueryStr = searchQueryStr+ " where "+ String.join(" AND ", conditions);
		}

		System.out.println(searchQueryStr);
		
		Query searchQuery = entityManager.createNativeQuery(searchQueryStr, Case.class);
		List<Case> searchResults  = searchQuery.getResultList();
		return searchResults;
	}

	@Override
	public List<Users> getUserList() {
		List<Users> users = new ArrayList<Users>();
		HashMap<String, Character> usersMap = new HashMap<String,Character>();
//		usersMap.put("Balasaheb.Chadile@ril.com", 'A');
//		usersMap.put("Balasubramanian,Krishnamoorthy@ril.com", 'A');
//		usersMap.put("Balasubramanian.R.Iyer@ril.com", 'A');
//		usersMap.put("Bhaumik.Darji@ril.com", 'A');
//		usersMap.put("Bhautik.Kansara", 'A');
//		usersMap.put("Shrikantp2143@gmail.com", 'A');
//		usersMap.put("Amol.Borse@honeywell.com", 'A');
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
	public List<FunctionalLocation> getFunctionalLocations(String assetName) {
		List<FunctionalLocation> locations = new ArrayList<FunctionalLocation>();
		System.out.println("IN Functiona location record fetching block");
		if(assetName!=null && assetName.length()!=0) {
			List<FunctionalLocation> flList = fetchRecords.getParentFunctionalLocation(assetName); 
//			if(flList.size()>0 && flList.get(0).getParentFLName()!=null && flList.get(0).getParentFLName().length()!=0) {
//				return fetchRecords.getFunctionaLocationsByFLName(flList.get(0).getParentFLName()); 
	        if (flList != null && !flList.isEmpty()) {
	            FunctionalLocation firstFL = flList.get(0);
	            if (firstFL.getParentFLName() != null && !firstFL.getParentFLName().isEmpty()) {
	                return fetchRecords.getFunctionaLocationsByFLName(firstFL.getParentFLName());
	            }
			}
		}
		return fetchRecords.getAllFunctionalLocations(); 
	}
	
	
	@Override
	public Case addRecommendation(Recommendations recommendation) {
		String caseNo = recommendation.getCaseNo();
		Case caseDetails = caseRepository.getByCaseNo(caseNo);
		for(Attribute attribute: caseDetails.getAttributes()) {
			String attributeValue = attribute.getValue();
			String updatedAttributeValue = saveRecommendations(attributeValue, caseNo, recommendation);
			updatedAttributeValue = removeUnwantedRecommendations(updatedAttributeValue);
			attribute.setValue(updatedAttributeValue);
		}
		System.out.println("After processing everything...");
		System.out.println("..."+ caseDetails.getAttributes().get(0).getValue());
		caseDetails = caseRepository.save(caseDetails);
		return caseDetails;
	}
	
	private String removeUnwantedRecommendations(String attribute) {
		attribute = attribute.replace("\\\"", "\"");

	    System.out.println("Attribute Value: " + attribute);
	    try {
	    	ObjectMapper objectMapper = new ObjectMapper();
		    JsonNode rootNode = objectMapper.readTree(attribute);

		    // Navigate to the "dataGrid1" array
		    JsonNode recommendationNode = rootNode.path("dataGrid1");
		    if (recommendationNode.isArray()) {
		    	ArrayNode arrayNode = (ArrayNode) recommendationNode;
		    	
		    	for (int i = arrayNode.size() - 1; i >= 0; i--) {
                    JsonNode dataGridEntry = arrayNode.get(i);
                    String recNumber = dataGridEntry.path("recommendationNo1").asText();
                    
                    // Remove the entry if recommendationNo1 is empty or null
                    if (recNumber == null || recNumber.isEmpty()) {
                        arrayNode.remove(i);
                    }
                }
		    	String updatedAttributeValue = objectMapper.writeValueAsString(rootNode);
		    	System.out.println("After Saving Recommendation"+ updatedAttributeValue);
		    	return updatedAttributeValue;
		    }
	    } catch(Exception e) {
	    	e.printStackTrace();
	    }
	    return null;
	}
	
	private String saveRecommendations(String attributeValue, String caseNo, Recommendations newRecommendation) {
	    attributeValue = attributeValue.replace("\\\"", "\"");

	    System.out.println("Attribute Value: " + attributeValue);

	    try {
	        ObjectMapper objectMapper = new ObjectMapper();
	        JsonNode rootNode = objectMapper.readTree(attributeValue);

	        // Navigate to the "dataGrid1" array
	        JsonNode recommendationNode = rootNode.path("dataGrid1");
	        if (recommendationNode.isArray()) {
	            ArrayNode dataGridArray = (ArrayNode) recommendationNode; // Cast to ArrayNode for appending new elements

	            // Convert the new recommendation object to a JSON node
	            ObjectNode newRecommendationNode = objectMapper.createObjectNode();
	            newRecommendationNode.put("recommendationHeadline", newRecommendation.getRecommendationHeadline());
	            newRecommendationNode.put("recommendationDescription1", newRecommendation.getRecommendationDescription1());
	            newRecommendationNode.put("recommendationAssignedTo1", newRecommendation.getRecommendationAssignedTo1());
	            newRecommendationNode.put("recommendationAssignedTo2", newRecommendation.getRecommendationAssignedTo2());
	            newRecommendationNode.put("recommendationStatus", newRecommendation.getRecommendationStatus());
	            newRecommendationNode.put("equipmentFunctionLocation", newRecommendation.getEquipmentFunctionLocation());
	            newRecommendationNode.put("recommendationTargetCompletionDate1", newRecommendation.getRecommendationTargetCompletionDate1());
	            newRecommendationNode.put("recommendationReviewer", newRecommendation.getRecommendationReviewer());
	            newRecommendationNode.put("recommendationNo1", newRecommendation.getRecommendationNo1());
	            newRecommendationNode.put("RecommendationSubmit", newRecommendation.getRecommendationSubmit());
	            newRecommendationNode.put("RecommendationConfirmSAP3", newRecommendation.getRecommendationConfirmSAP3());
	            newRecommendationNode.put("createdBy", newRecommendation.getCreatedBy());

	            // Append the new recommendation node to the dataGrid1 array
	            
	            String[] recommendationStatusAndId = saveRecommendationMapping(newRecommendationNode, caseNo, newRecommendation.getRecommendationAssignedTo2(), newRecommendation.getRecommendationReviewer());
	            
	            newRecommendationNode.put("recommendationNo1", recommendationStatusAndId[0]);
	            newRecommendationNode.put("recommendationStatus", recommendationStatusAndId[1]);
	            dataGridArray.add(newRecommendationNode);
	            // Convert the updated root node back to a string
	            String updatedAttributeValue = objectMapper.writeValueAsString(rootNode);
	            System.out.println("Updated Attribute Value: " + updatedAttributeValue);
	            return updatedAttributeValue;
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return null;
	}

	@Override
	public void sendEmail(String emailId, String subject, String body) {
		SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(emailId);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom("your-email@gmail.com");

        mailSender.send(message);
	}

	@Override
	public List<com.wks.caseengine.rest.db2.entity.Users> getUsersList() {
		return usersRepository.findAll(); 

	}
	
	@Override
	public List<com.wks.caseengine.rest.db2.entity.Users> getGEUsers() {
	    List<com.wks.caseengine.rest.db2.entity.Users> geUsers = new ArrayList<>();
	    String geAPMAcsessToken = geLogin();
	    RestTemplate restTemplate = new RestTemplate();
	    HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
	    headers.add("MeridiumToken", geAPMAcsessToken);
	    Map<String, Object> inputsingleParams = new HashMap<>();
	    inputsingleParams.put("userValidation", "");
	    Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("QueryPath", "Public\\Meridium\\Client\\APIs\\UserValidation_EED_APM_API");
	    requestBody.put("Page", 0);
	    requestBody.put("PageSize", 1000);
	    requestBody.put("InputsingleParams", inputsingleParams);
	    try {
	        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
	        ResponseEntity<Map> response = restTemplate.postForEntity(geUsersAPI, requestEntity, Map.class);
	        System.out.println("Response Code: " + response.getStatusCode());
	        System.out.println("Response Body: " + response.getBody());
	        Map<String, Object> responseBody = response.getBody();
	        if (responseBody != null && responseBody.get("output") instanceof Map) {
	            Map<String, Object> responseOutput = (Map<String, Object>) responseBody.get("output");
	            if (responseOutput.get("data") instanceof List) {
	                List<Map<String, Object>> usersList = (List<Map<String, Object>>) responseOutput.get("data");
	                for (Map<String, Object> userMap : usersList) {
	                    com.wks.caseengine.rest.db2.entity.Users user = new com.wks.caseengine.rest.db2.entity.Users();
	                    if(userMap.get("status") != null && userMap.get("status").toString() == "A") {
		                    user.setUserId(userMap.get("userId") != null ? userMap.get("userId").toString() : null);
		                    user.setEmailId(userMap.get("userId") != null ? userMap.get("userId").toString() : null);
		                    geUsers.add(user);
	                    } 
	                }
	            }
	        }
	    } catch (Exception e) {
	        System.out.println("GE APM Authentication API failed: " + e.getLocalizedMessage());
	        e.printStackTrace();
	    }
	    return geUsers;
	}
	private String geLogin() {
		 String geAPMAcsessToken="";
		 RestTemplate restTemplate = new RestTemplate();
		 HttpHeaders headers = new HttpHeaders();
		 headers.setContentType(MediaType.APPLICATION_JSON);
         Map<String, Object> requestBody = new HashMap<>();
         requestBody.put("DatasourceId", geAuthenticationDatasource);
         requestBody.put("Id", geAuthenticationId);
         requestBody.put("Password", geAuthenticationPassword);
         try {
        	 HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
    		 ResponseEntity<Map> response = restTemplate.postForEntity(geAuthenticationAPI, requestEntity, Map.class);
    		 
    		 System.out.println("Response Code: " + response.getStatusCode());
    		 System.out.println("Response Body: " + response.getBody());
    		 Map<String, Object> responseBody = response.getBody();
    		 if (responseBody != null) {
    			 geAPMAcsessToken = responseBody.get("sessionId") != null ? (String) responseBody.get("sessionId") : "";
    		     System.out.println("GE APM Access Token: " + geAPMAcsessToken);
    		 }
         }catch(Exception e) {
        	 System.out.println("GE APM Authentication API failed " + e.getLocalizedMessage());
        	 e.printStackTrace();
         }
		 return geAPMAcsessToken;
	}
//	@Scheduled(cron = "0 0/1 * * * ?")// You can adjust this cron expression to run at a specific time (e.g., every day at noon)
//    public void scheduleTask() {
//        System.out.println("Scheduler triggered");
//        List<Case> cases = caseRepository.findAll();
//        System.out.println("Cases fetahed: "+ cases.size());
//        System.out.println("")
//    }
	
	
}
