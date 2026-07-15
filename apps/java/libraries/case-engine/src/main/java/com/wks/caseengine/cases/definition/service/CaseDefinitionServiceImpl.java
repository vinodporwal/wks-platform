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

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.deser.std.StringArrayDeserializer;
import com.wks.caseengine.rest.db2.entity.*;
import com.wks.caseengine.rest.db2.repository.*;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
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
import com.wks.caseengine.cases.instance.CaseInstance.EventUrlItem;
import com.wks.caseengine.cases.instance.email.CaseEmailServiceImpl;
import com.wks.caseengine.command.CommandExecutor;
import com.wks.caseengine.rest.db1.repository.EventEnrichmentRepository;
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

    @Autowired
    private GroupsRepository groupsRepository;

    // @Autowired
    // private EventEnrichmentLinksService eventEnrichmentLinksService;

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
    @Value("${ge.case_status.api}")
    private String geCaseStatusAPI;
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
        List<FaultHistoryModel> faultHistories = fetchRecords.getFaultHistories(eventIds);
        List<FaultEvents> faultEventsList = new ArrayList<>();
        for (FaultHistoryModel faultHistory : faultHistories) {
            FaultEvents faultEvent = new FaultEvents();

            faultEvent.setStartTime(faultHistory.getStartTime());
            faultEvent.setEndTime(faultHistory.getEndTime());

            // Set Fault History Data
            String eventEnrichmentPkIdStr = faultHistory.getEventEnrichmentPkId();
            faultEvent.setEventEnrichment(new EventEnrichmentModel());
            if(!Objects.isNull(eventEnrichmentPkIdStr) && !eventEnrichmentPkIdStr.isEmpty()) {
                EventEnrichmentModel eventEnrichment = fetchRecords.getEventEnrichment(eventEnrichmentPkIdStr);
                faultEvent.setEventEnrichment(eventEnrichment);
                List<EventsModel> events = fetchRecords.findEventsByEventId(eventEnrichment.getEventPkId());
                faultEvent.setEvents(!events.isEmpty() ? events.get(0) : new EventsModel());
            }
            List<EquipmentModel> equipments = fetchRecords.getEquipmentName(faultHistory.getEquipmentPkId());
            if (!Objects.isNull(equipments) && !equipments.isEmpty()) {
                EquipmentModel equipment = equipments.get(0);
                faultEvent.setAssetName(equipment.getName());
                faultEvent.setAssetDisplayName(equipment.getDisplayName());
                faultEvent.setAssetId(equipment.getAssetId());
            }
            List<EventCategoryModel> eventCategories = fetchRecords.getCategoryByCategoryId(faultHistory.getEventCategoryPkId());
            faultEvent.setEventCategory(!eventCategories.isEmpty() ? eventCategories.get(0) : new EventCategoryModel());
            faultEventsList.add(faultEvent);
        }
        return faultEventsList;
    }

    @Override
    public void linkEventsToCase(String businessKey, List<Long> eventIds) {
        // 1. Fetch event details
        List<FaultEvents> faultEvents = getAllEvents(eventIds);

        // 2. Find the case in SQL by businessKey
        Case caseData = caseRepository.getByBusinessKey(businessKey);
        if (caseData == null) {
            caseData = caseRepository.getByCaseNo(businessKey);
        }
        if (caseData == null) {
            throw new RuntimeException("Case not found for businessKey: " + businessKey);
        }

        // 3. Parse the attributes JSON and find dataGrid2
        List<Attribute> attributes = caseData.getAttributes();
        if (attributes == null || attributes.isEmpty()) {
            throw new RuntimeException("Case has no attributes for businessKey: " + businessKey);
        }

        Attribute attribute = attributes.get(0);
        try {
            ObjectMapper mapper = new ObjectMapper();
            String rawValue = attribute.getValue();
            com.fasterxml.jackson.databind.node.ObjectNode rootNode =
                (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(rawValue);

            // Get existing dataGrid2 or create new array
            com.fasterxml.jackson.databind.node.ArrayNode dataGrid2;
            if (rootNode.has("dataGrid2") && rootNode.get("dataGrid2").isArray()) {
                dataGrid2 = (com.fasterxml.jackson.databind.node.ArrayNode) rootNode.get("dataGrid2");
            } else {
                dataGrid2 = mapper.createArrayNode();
            }

            // 4. Append new event entries
            for (FaultEvents event : faultEvents) {
                com.fasterxml.jackson.databind.node.ObjectNode entry = mapper.createObjectNode();
                entry.put("subAsset", event.getAssetDisplayName() != null ? event.getAssetDisplayName() : "");
                entry.put("events", event.getEvents() != null ? event.getEvents().getEventName() : "");
                entry.put("eventCategory", event.getEventCategory() != null ? event.getEventCategory().getName() : "");
                String startTime = event.getStartTime() != null ? event.getStartTime() : "";
                entry.put("TextFaultStartTimeDate", startTime);
                entry.put("TextFaultEndTimeDate", event.getEndTime() != null ? event.getEndTime() : "");
                entry.put("btnEventLink", false);
                entry.put("btnEventTrend", false);
                if (event.getEvents() != null) {
                    entry.put("eventPkId", event.getEvents().getEventPkId());
                }
                dataGrid2.add(entry);
            }

            rootNode.set("dataGrid2", dataGrid2);
            attribute.setValue(mapper.writeValueAsString(rootNode));
            attributes.set(0, attribute);
            caseData.setAttributes(attributes);

            // Also update eventIds on the case
            List<String> existingEventIds = caseData.getEventIds() != null ? new java.util.ArrayList<>(caseData.getEventIds()) : new java.util.ArrayList<>();
            for (Long eid : eventIds) {
                String eidStr = String.valueOf(eid);
                if (!existingEventIds.contains(eidStr)) {
                    existingEventIds.add(eidStr);
                }
            }
            caseData.setEventIds(existingEventIds);

            // 5. Save
            caseRepository.save(caseData);
            System.out.println("linkEventsToCase: Updated case " + businessKey + " with " + faultEvents.size() + " new events");
        } catch (Exception e) {
            throw new RuntimeException("Failed to update dataGrid2 for case " + businessKey + ": " + e.getMessage(), e);
        }
    }

//	public List<FaultEvents> getAllEvents(List<Long> eventIds) {
//		System.out.println(eventIds);
//		System.out.println(eventIds.get(0));
//		List<FaultHistoryModel> faultHistorys = fetchRecords.getFaultHistories(eventIds); 
//		String equipmentDisplayName = "";
//		String equipmentName = "";
//		List<Long> eventEnrichmentsPKIds = new ArrayList<>();
//		for(FaultHistoryModel faultHistory: faultHistorys) {
//			List<EquipmentModel> equipemnets = fetchRecords.getEquipmentName(faultHistory.getEquipmentPkId());
//			equipmentDisplayName = equipemnets.get(0).getDisplayName();
//			equipmentName = equipemnets.get(0).getName();
//			break;
//		}
//		for(FaultHistoryModel faultHistory: faultHistorys) {
//			String eventEnrichmentPkIdStr = faultHistory.getEventEnrichmentPkId();
//			if(!eventEnrichmentPkIdStr.isEmpty()) {
//				long eventEnrichmentPkId = Long.parseLong(eventEnrichmentPkIdStr);
//				eventEnrichmentsPKIds.add(eventEnrichmentPkId);
//			}
//		}
//		List<FaultEvents> faultEvents = new ArrayList<FaultEvents>();
//		
//		List<EventEnrichmentModel> eventEnrichments = fetchRecords.getEventEnrichments(eventEnrichmentsPKIds);
//		for(EventEnrichmentModel eventEnrichment: eventEnrichments) {
//			String eventId = eventEnrichment.getEventPkId();
//			String eventCategoryId = eventEnrichment.getEventCategoryPkId();
//			
//			List<EventsModel> events = fetchRecords.findEventsByEventId(eventId);
//			EventsModel event = new EventsModel();
//			if(events.size()>=1) {
//				event = events.get(0);
//			}
//			System.out.println("Events List");
//			System.out.println(eventIds.get(0));
//			System.out.println(eventId);
//			for(EventsModel event1: events) {
//				System.out.println(event1.getEventName());
//			}
//			List<EventCategoryModel> eventCategorys = fetchRecords.getCategoryByCategoryId(eventCategoryId);
//			EventCategoryModel eventCategory = new EventCategoryModel();
//			if(eventCategorys.size()>=1) {
//				eventCategory = eventCategorys.get(0);
//			}
//			FaultEvents faultEvent = new FaultEvents();
//			faultEvent.setEvents(event);
//			faultEvent.setEventEnrichment(eventEnrichment);
//			faultEvent.setEventCategory(eventCategory);
//			faultEvent.setAssetName(equipmentName);
//			faultEvent.setAssetDisplayName(equipmentDisplayName);
//			faultEvents.add(faultEvent);
//		}
//		return faultEvents;
//	}

    @Override
    public Case saveCase(Case caseData) {
        System.out.println("In saveCase");

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
        if (caseData.getAssignedTo() != null) {
            List<com.wks.caseengine.rest.db2.entity.Users> users = new ArrayList<>();
            for(com.wks.caseengine.rest.db2.entity.Users user : caseData.getAssignedTo()) {
                users.add(usersRepository.findByEmailId(user.getEmailId()));
            }

            caseData.setAssignedTo(users);
        }
        Case caseDetails = new Case();
        String caseNo = "";
        //  String caseNo = caseData.getCaseNo();
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

            System.out.println("caseNo: " + caseNo);

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
        // System.out.println("eventTrendUrl(): "+caseData.getEventTrendUrl());
        // System.out.println("eventReportUrl(): "+caseData.getEventReportUrl());
        System.out.println("eventTrendUrls(): "+caseData.getEventTrendUrls());
        System.out.println("eventTrendUrls(): "+caseData.getEventTrendUrls());
        System.out.println("eventReportUrls(): "+caseData.getEventReportUrls());

        if(caseNo==null || caseNo.length()==0) {
            System.out.println("saving new case details....");
            // caseNo = CaseNoGenerator();
            if(caseData.getBusinessKey()!=null && !caseData.getBusinessKey().isEmpty()) {
                caseNo = caseData.getBusinessKey();
            } else {
                caseNo = CaseNoGenerator();
        }
    
            caseData.setCaseNo(caseNo);
            caseData.setBusinessKey(caseNo);
            //	caseData.setCaseUrl(caseData.getCaseUrl()+"&caseNo="+caseNo);
            caseData.setCaseUrl(caseData.getCaseUrl()+"&caseNo="+ caseNo);
			// Inject the generated caseNo back into the container attribute JSON before saving
			try {
				ObjectMapper injectMapper = new ObjectMapper();
				String rawAttrValue = attribute.getValue().replace("\\\"", "\"");
				com.fasterxml.jackson.databind.node.ObjectNode rootNode =
					(com.fasterxml.jackson.databind.node.ObjectNode) injectMapper.readTree(rawAttrValue);
				rootNode.put("caseNo", caseNo);
				attribute.setValue(injectMapper.writeValueAsString(rootNode));
			} catch (Exception e) {
				e.printStackTrace();
			}

            System.out.println("Saving New Case Details....");
            caseData.setCreationDate(currentDate);
            attributes.set(0, attribute);
            caseData.setAttributes(attributes);
            caseDetails  = caseRepository.save(caseData);
          //  eventEnrichmentLinksService.createEventEnrichmentLinks("", caseDetails.getCaseUrl(), caseData.getEventIds().toString());


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
            if(savedCase == null) {
                savedCase =caseRepository.getByBusinessKey(caseNo);
            }
            System.out.println("savedCase: " + savedCase);
            caseData.setCreationDate(savedCase.getCreationDate()==null?currentDate:savedCase.getCreationDate());
            caseDetails  = caseRepository.save(caseData);
        }

        //sending Emails part
        System.out.println("**************************************sending Emails part*************************************************");
        System.out.println("************************************ Is Draft"+ caseData.getIsDraft());
        System.out.println("eventTrendUrls: " + caseData.getEventTrendUrls());
       
       
        // if(!caseData.getIsDraft().equals("y")) { // commented as per requiremnet from XOM team on 12th July 2026
            attributeValue = attributeValue.replace("\\\"", "\"");
            System.out.println("Attribute Value: " + attributeValue);

            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(attributeValue);
                String caseAssignedToValue = rootNode.path("caseAssignedTo").asText();

                if (caseAssignedToValue != null && !caseAssignedToValue.isBlank()
                        && (caseData.getAssignedTo() == null || caseData.getAssignedTo().isEmpty())) {

                    List<com.wks.caseengine.rest.db2.entity.Users> resolvedList = new ArrayList<>();

                    com.wks.caseengine.rest.db2.entity.Users resolvedUser = usersRepository
                            .findByEmailId(caseAssignedToValue);

                    if (resolvedUser != null) {
                        resolvedList.add(resolvedUser);
                        caseData.setAssignedToLabel(resolvedUser.getUserId());
                    } else {
                        com.wks.caseengine.rest.db2.entity.Groups resolvedGroup = groupsRepository
                                .findByGroupId(caseAssignedToValue);

                        if (resolvedGroup != null && resolvedGroup.getUsers() != null
                                && !resolvedGroup.getUsers().isEmpty()) {
                            resolvedList.addAll(resolvedGroup.getUsers());
                            caseData.setAssignedToLabel(resolvedGroup.getGroupId());
                        }
                    }

                    if (!resolvedList.isEmpty()) {
                        caseData.setAssignedTo(resolvedList);
                    }
                }
                String[] assignedTo = caseData.getAssignedTo().stream().map(ele -> ele.getEmailId())
                        .toArray(String[]::new);
                String caseNumber = caseData.getCaseNo();
                String caseTitle = rootNode.path("caseTitle").asText();
                System.out.println(rootNode.path("caseAssignedTo").asText());
                Long caseStatusNo = rootNode.path("caseStatus").asLong();
                Optional<CaseStatus> caseStatus = getAllCaseStatus().stream()
                        .filter(status -> status.getId().equals(caseStatusNo))
                        .findFirst();
                String caseStatusValue = caseStatus.get().getName();
                JsonNode analysisTeam = rootNode.path("analysisTeam");

                // Extract main asset
                String mainAsset = rootNode.path("mainAsset").asText();

                // Prepare a list for sub-assets (if multiple in dataGrid2)
                List<Map<String, String>> subAssetList = new ArrayList<>();
                for (JsonNode item : rootNode.path("dataGrid2")) {
                    Map<String, String> row = new HashMap<>();
                    row.put("subAsset", item.path("subAsset").asText());
                    row.put("events", item.path("events").asText());
                    row.put("eventCategory", item.path("eventCategory").asText());
                    row.put("faultStart", item.path("TextFaultStartTimeDate").asText());
                    row.put("faultEnd", item.path("TextFaultEndTimeDate").asText());
                    row.put("eventPkId", item.path("eventPkId").asText());
              
                    subAssetList.add(row);
                    System.out.println("eventPkIds: from attributes : "  + item.path("eventPkId").asText());
                }

             //   String[] reviewers = new String[analysisTeam.size()];
             // adding 1 to the size of the reviewers array to add the case owner in the reviewers array 
             String[] reviewers = new String[analysisTeam.size() + 1];
                if (analysisTeam.isArray()) {
                    int counter = 0;
                    for (JsonNode dataGridEntry : analysisTeam) {
                        reviewers[counter] = dataGridEntry.asText();
                        counter++;
                    }

                }

                // adding the case owner in reviewers so that the owner receives an email when a new case is created
                reviewers[reviewers.length -1] = caseData.getOwner().getEmail();
                System.out.println("reviewers: " + reviewers);


             Map<String, String>  eventTrendUrlsMap = new HashMap<>();
                if(caseData.getEventTrendUrls() != null) {  
                    eventTrendUrlsMap = caseData.getEventTrendUrls()
                    .stream()
                    .collect(Collectors.toMap(
                        Case.EventUrlItem::getUrlId,
                        Case.EventUrlItem::getUrl
                    ));

                }

                Map<String, String>  eventReportUrlsMap = new HashMap<>();
                if(caseData.getEventReportUrls() != null) {  
                    eventReportUrlsMap = caseData.getEventReportUrls()
                    .stream()
                    .collect(Collectors.toMap(
                        Case.EventUrlItem::getUrlId,
                        Case.EventUrlItem::getUrl
                    ));
                }

                if(!caseStatusValue.equals("Under Analysis")) {
                    System.out.println("Calling mail send method...");
//			    	String from = "amol.borse@honeywell.com";
                    Map<String, Object> data = new HashMap<>();
                    if (caseStatusValue.equals("Closed"))
                        data.put("caseTitle", "This is to inform you, the case has been closed");
                    else
                        data.put("caseTitle", "This is to inform you, the new case has been assigned to you");
                    data.put("headerText", caseStatusValue.equals("Closed") ? "Case has been closed"
                            : "New case has been assigned to you");
                    data.put("headerColor", caseStatusValue.equals("Closed") ? "#D32F2F" : "#2F8B8B");
                    
                    data.put("caseNumber", caseNumber);
                    data.put("status", caseStatusValue);
                    data.put("caseName", caseTitle);
                    data.put("caseUrl", caseDetails.getCaseUrl());
                    data.put("environment", "");
                    data.put("mainAsset", mainAsset);
                    data.put("subAssets", subAssetList);
                    // data.put("eventTrendUrls", caseData.getEventTrendUrls());
                    // data.put("eventReportUrls", caseData.getEventReportUrls());
                    data.put("eventTrendUrlsMap", eventTrendUrlsMap);
                    data.put("eventReportUrlsMap", eventReportUrlsMap);
                    data.put("assignedToLabel", caseData.getAssignedToLabel());
                    data.put("assignedBy", caseData.getOwner().getName());
                    caseTitle = "CASE MANAGEMENT :"+ caseTitle;

                    caseEmailService.send(from, assignedTo , caseTitle, reviewers, null, null, "email-template", data);
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
        // }
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

    private String[] saveRecommendationMapping(JsonNode dataGridEntry, String caseNo, String assignedUserId, String reviewerUserId) throws Exception {
        String[] recommendationStatusAndId = saveRecommendationGEAPMApi(dataGridEntry, caseNo, assignedUserId, reviewerUserId);
        CaseAndRecommendationsMapping caseRecommendationMapping = new CaseAndRecommendationsMapping();
        caseRecommendationMapping.setCaseNo(caseNo);
        caseRecommendationMapping.setRecId(recommendationStatusAndId[0]);
        caseRecommendationMapping.setRecommendationJson(dataGridEntry.toPrettyString().toString());
        caseRecommendationMappingRepository.save(caseRecommendationMapping);

        return recommendationStatusAndId;
    }

    private String[] saveRecommendationGEAPMApi(JsonNode dataGridEntry, String caseNo, String assignedUserId,
                                                String reviewerUserId) throws Exception {
        System.out.println("Calling Recommendation GEAPM API...");
        System.out.println(dataGridEntry.toPrettyString().toString());
        String geAPMAcsessToken = geLogin();
        System.out.println("GE APM Acsess Token: " + geAPMAcsessToken);
//		Boolean isFunctionalLocationAvailableInGEAPM = checkFunctionalLocationAvailableInGEAPM(geAPMAcsessToken, dataGridEntry.path("equipmentFunctionLocation").asText());
//		
//		Boolean isUserAvailableInGEAPM = checkUserAvailableInGEAPM(geAPMAcsessToken, dataGridEntry.path("recommendationAssignedTo2").asText());
        String recommendationId = "";
        String status = "Pending Approval";
        String[] recommendationStatusAndId = new String[2];
//		if(isFunctionalLocationAvailableInGEAPM && isUserAvailableInGEAPM) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("MeridiumToken", geAPMAcsessToken);
        String targetDateString = dataGridEntry.path("recommendationTargetCompletionDate1").asText();
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        Date date = inputFormat.parse(targetDateString);
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String targetDate = outputFormat.format(date);

        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("Auther_Domain_Id", dataGridEntry.path("recommendationAssignedTo2").asText());
        requestBody.put("Pending_Approval_Domain_Id", "MIADMIN");
        requestBody.put("Approved_Domain_Id", dataGridEntry.path("recommendationReviewer").asText());
        requestBody.put("RECOMMENDATION_Des", dataGridEntry.path("recommendationDescription1").asText());
        requestBody.put("MI_REC_BASIS", dataGridEntry.path("recommendationHeadline").asText());
        requestBody.put("MI_REC_LOC_ID_CHR", dataGridEntry.path("equipmentFunctionLocation").asText());
//			requestBody.put("MI_REC_LOC_ID_CHR", "JSR-CFP-Z357-Z357FV231A");
        requestBody.put("MI_REC_LONG_DESCR_TX", dataGridEntry.path("recommendationDescription1").asText());
        requestBody.put("MI_REC_TARGE_COMPL_DATE_DT", targetDate);
        requestBody.put("MI_REC_PRIORITY_C", "2");
//			requestBody.put("CC_REC_CREAT_SAP_REQUE_L", dataGridEntry.path("RecommendationConfirmSAP3").asText().toUpperCase());
        requestBody.put("CC_REC_CREAT_SAP_REQUE_L", "N");
        requestBody.put("CaseID", caseNo);
//		requestBody.put("Auther_Domain_Id", "Devang.Bhatt@ril.com");
//		requestBody.put("Pending_Approval_Domain_Id", "MIADMIN");
//		requestBody.put("Approved_Domain_Id", "Vipul.Rupareliya@ril.com");
//		requestBody.put("RECOMMENDATION_Des", "EED Headline");
//		requestBody.put("MI_REC_BASIS", "Recommendation Basis EED");
//		requestBody.put("MI_REC_LOC_ID_CHR", "JSR-CFP-Z357-Z357FV231A");
//		requestBody.put("MI_REC_LONG_DESCR_TX", "EED Long Description");
//		requestBody.put("MI_REC_TARGE_COMPL_DATE_DT", "2024-02-28 10:00:00");
//		requestBody.put("MI_REC_PRIORITY_C", "2");
//		requestBody.put("CC_REC_CREAT_SAP_REQUE_L", "N");
//		requestBody.put("CaseID", "123456");
        System.out.println("GE APM Create Case body: " + requestBody.toString());

        try {
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(geCreateCaseAPI, requestEntity, Map.class);
            System.out.println("Response Code: " + response.getStatusCode());
            System.out.println("Response Body: " + response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && responseBody.get("Data") instanceof Map) {
                    Map<String, Object> responseData = (Map<String, Object>) responseBody.get("Data");
                    recommendationId = responseData.get("MI_REC_ID") != null ? (String) responseData.get("MI_REC_ID") : "";
                    System.out.println("Recommendation Id: " + recommendationId);
                    recommendationStatusAndId[0] = recommendationId;
                    recommendationStatusAndId[1] = status;
                }
            }
        }catch(Exception e) {
            System.out.println("GE APM Post Recommendation API failed " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        sendMailToAssignedPerson(assignedUserId);
        sendMailToReviewerPerson(reviewerUserId);

//		String prefix = "REC-";
        // Generate a random number between 1 and 999999
//		int randomNumber = ThreadLocalRandom.current().nextInt(1, 1000000);

        // Format the random number as a 6-digit string with leading zeros
//		String formattedId = String.format("%06d", randomNumber);

        // Return the generated ID with the prefix
//		String id = prefix + formattedId;
//		String status = "Assigned";
//		} else {
//			recommendationStatusAndId[0] = null;
//			recommendationStatusAndId[1] = status;
//		}
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
        String query = "SELECT c.* FROM [CaseManagement].[dbo].[Cases] c " +
                "WHERE c.asset_name LIKE CONCAT('%', :assetName, '%') " +
                "AND (:hierarchyName IS NULL OR c.hierarchy_name = :hierarchyName) " +
                "ORDER BY c.case_no DESC";
        Query nativeQuery = entityManager.createNativeQuery(query, Case.class);
        nativeQuery.setParameter("assetName", displayName);
        nativeQuery.setParameter("hierarchyName", (hierarchyName == null || hierarchyName.isEmpty()) ? null : hierarchyName);
        List<Case> cases = nativeQuery.getResultList();
        cases.forEach(c -> c.setAssignedTo(new java.util.ArrayList<>()));
        return cases;
    }

    @Override
    public List<Case> getCaseDetails(String displayName, String hierarchyName, int page, int size) {
        String baseQuery = "SELECT c.* FROM [CaseManagement].[dbo].[Cases] c " +
                "WHERE c.asset_name LIKE CONCAT('%', :assetName, '%') " +
                "AND (:hierarchyName IS NULL OR c.hierarchy_name = :hierarchyName) " +
                "ORDER BY c.case_no DESC";
        String countQuery = "SELECT COUNT(*) FROM [CaseManagement].[dbo].[Cases] c " +
                "WHERE c.asset_name LIKE CONCAT('%', :assetName, '%') " +
                "AND (:hierarchyName IS NULL OR c.hierarchy_name = :hierarchyName)";

        String hn = (hierarchyName == null || hierarchyName.isEmpty()) ? null : hierarchyName;

        Query countNativeQuery = entityManager.createNativeQuery(countQuery);
        countNativeQuery.setParameter("assetName", displayName);
        countNativeQuery.setParameter("hierarchyName", hn);
        long total = ((Number) countNativeQuery.getSingleResult()).longValue();

        Query nativeQuery = entityManager.createNativeQuery(baseQuery, Case.class);
        nativeQuery.setParameter("assetName", displayName);
        nativeQuery.setParameter("hierarchyName", hn);
        nativeQuery.setFirstResult(page * size);
        nativeQuery.setMaxResults(size);
        List<Case> cases = nativeQuery.getResultList();
        cases.forEach(c -> c.setAssignedTo(new java.util.ArrayList<>()));

        CaseDefinitionServiceImpl.totalHolder.set(total);
        return cases;
    }

    // thread-local to pass total count back to controller without changing return type
    public static final ThreadLocal<Long> totalHolder = new ThreadLocal<>();

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

//	    System.out.println("Attribute Value: " + attributeValue);

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
    public List<Groups> getGroupsList()  {
        return groupsRepository.findAll();

    }

    @Override
    public List<com.wks.caseengine.rest.db2.entity.Users> getGEUsers() throws Exception {
        List<com.wks.caseengine.rest.db2.entity.Users> geUsers = new ArrayList<>();
        String geAPMAcsessToken = geLogin();
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("MeridiumToken", geAPMAcsessToken);
        Map<String, Object> inputsingleParams = new HashMap<>();
        inputsingleParams.put("Domain", "");
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("QueryPath", "Public\\Meridium\\Client\\APIs\\UserValidation_EED_APM_API");
        requestBody.put("Page", 0);
        requestBody.put("PageSize", 10000);
        requestBody.put("InputsingleParams", inputsingleParams);

        try {
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(geUsersAPI, requestEntity, Map.class);
            System.out.println("Response Code: " + response.getStatusCode());
// 	        System.out.println("Response Body: " + response.getBody());


            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.get("output") instanceof Map) {
                Map<String, Object> responseOutput = (Map<String, Object>) responseBody.get("output");
                if (responseOutput.get("data") instanceof Map) {
                    Map<String, Object> usersData = (Map<String, Object>) responseOutput.get("data");
                    if (usersData.get("rows") instanceof List) {
                        List<Map<String, Object>> usersList = (List<Map<String, Object>>) usersData.get("rows");
                        for (Map<String, Object> userMap : usersList) {
                            if ("A".equals(userMap.get("Status"))) { // Corrected String comparison
                                com.wks.caseengine.rest.db2.entity.Users user = new com.wks.caseengine.rest.db2.entity.Users();

                                user.setUserId(userMap.get("User ID") != null ? userMap.get("User ID").toString() : null);
                                user.setEmailId(userMap.get("User ID") != null ? userMap.get("User ID").toString() : null);
                                geUsers.add(user);
                            }
                        }
                    }
                }
            }
        } catch (RestClientException e) {
            System.err.println("GE APM API request failed: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error in getGEUsers(): " + e.getMessage());
            e.printStackTrace();
        }

        return geUsers;
    }

    private String geLogin() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> requestBody = Map.of(
                "DatasourceId", geAuthenticationDatasource,
                "Id", geAuthenticationId,
                "Password", geAuthenticationPassword
        );
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(geAuthenticationAPI, requestEntity, Map.class);
            System.out.println("Response Body: " + response.getBody());
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().getOrDefault("sessionId", "");
            } else {
                System.err.println("GE APM Authentication API failed: Non-successful response - " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            System.err.println("GE APM Authentication API request failed: " + e.getMessage());
            throw new Exception("GE APM Authentication API request failed: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error during authentication: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("GE APM Authentication API request failed: " + e.getMessage());
        }
        return "";
    }

    @Override
//	@Scheduled(cron = "0 */5 * * * ?")
    public List<Case> updateRecommendationStatus() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate oneMonthBefore = today.minusDays(10); //.minusMonths(1);
        String geAPMAcsessToken = geLogin();
        List<Case> cases = getCaseDetails(oneMonthBefore, today, "Open");
        System.out.println("Cases size: " + cases.size());
        ObjectMapper objectMapper = new ObjectMapper();
        for (Case caseDetails : cases) {
            boolean updated = false; // Track if updates are made
            for (Attribute attribute : caseDetails.getAttributes()) {
                String attributeValue = attribute.getValue();
                System.out.println("Case No: " + caseDetails.getCaseNo() + " :: Attribute: " + attributeValue);
                JsonNode rootNode = objectMapper.readTree(attributeValue);
                JsonNode recommendationNode = rootNode.path("dataGrid1");
                if (recommendationNode.isArray()) {
                    for (JsonNode node : recommendationNode) {
                        if (node.has("recommendationNo1") && node.isObject()) {
                            String recommendationNo = node.get("recommendationNo1").asText();
                            String recommendationStatus = getGEAPMRecommendationStatusAndUpdateRecommendationStatus(
                                    geAPMAcsessToken, recommendationNo);
                            if (recommendationStatus != null && !recommendationStatus.isEmpty()) {
                                ((ObjectNode) node).put("recommendationStatus", recommendationStatus);
                                updated = true; // Mark that an update occurred
                            }
                        }
                    }
                    if (updated) { // Only update attribute if changes were made
                        attribute.setValue(objectMapper.writeValueAsString(rootNode));
                    }
                }
            }
            if (updated) { // Save only if changes were made
                caseRepository.save(caseDetails);
            }
        }
        return cases;
    }
    @Override
    public Case saveAnalysis(Case caseData) {
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
        if (caseData.getAssignedTo() != null) {
            List<com.wks.caseengine.rest.db2.entity.Users> users = new ArrayList<>();
            for(com.wks.caseengine.rest.db2.entity.Users user : caseData.getAssignedTo()) {
                users.add(usersRepository.findByEmailId(user.getEmailId()));
            }

            caseData.setAssignedTo(users);
        }
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (statusId != null) {
            Optional<CaseStatus> caseStatus = caseStatusRepository.findById(statusId);
            if (caseStatus.isPresent()) {
                caseData.setStatus(caseStatus.get());
            }
        }
        System.out.println("Saving Exsting Case Details....");
        caseData.setCaseNo(caseNo);
        if (caseData.getCaseUrl() != null && !caseData.getCaseUrl().contains("&caseNo")) {
            caseData.setCaseUrl(caseData.getCaseUrl() + "&caseNo=" + caseNo);
        }
     System.out.println("Saving Exsting Case Details....");
        caseData.setCaseNo(caseNo);
        if (caseData.getCaseUrl() != null && !caseData.getCaseUrl().contains("&caseNo")) {
            caseData.setCaseUrl(caseData.getCaseUrl() + "&caseNo=" + caseNo);
        }
        Case savedCase = caseRepository.getByCaseNo(caseNo);
        caseData.setCreationDate(savedCase.getCreationDate());

        // ---- Notify Analysis Team on edit/status change ----
        try {
            String rawAttrValue = attributeValue.replace("\\\"", "\"");
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(rawAttrValue);
            String mainAsset = rootNode.path("mainAsset").asText();
            JsonNode analysisTeamNode = rootNode.path("analysisTeam");
            List<String> analysisTeamEmails = new ArrayList<>();
            List<String> reviewers = new ArrayList<>();
            if (analysisTeamNode.isArray()) {
                for (JsonNode member : analysisTeamNode) {
                    analysisTeamEmails.add(member.asText());
                }
            }

            if (!analysisTeamEmails.isEmpty()) {
                String caseTitle = rootNode.path("caseTitle").asText();
                String caseStatusValue = (caseData.getStatus() != null) ? caseData.getStatus().getName() : "";

                Map<String, Object> data = new HashMap<>();
                data.put("caseTitle", "This is to inform that the case is "+caseStatusValue);
                data.put("headerText", "Case has been updated");
                data.put("headerColor", caseStatusValue.equalsIgnoreCase("Closed") ? "#D32F2F" : "#2F8B8B");
                data.put("caseNumber", caseNo);
                data.put("status", caseStatusValue);
                data.put("caseName", caseTitle);
                data.put("caseUrl", caseData.getCaseUrl());
                data.put("mainAsset", mainAsset);
                data.put("assignedBy", caseData.getOwner() != null ? caseData.getOwner().getName() : "");
                
                String ownerEmail = caseData.getOwner() != null ? caseData.getOwner().getEmail() : "";
                 reviewers.add(ownerEmail);
                String assignedToLabel = "";
if (caseData.getAssignedTo() != null && !caseData.getAssignedTo().isEmpty()) {
    assignedToLabel = caseData.getAssignedTo().get(0).getUserId();
    reviewers.add(caseData.getAssignedTo().get(0).getEmailId());

} else if (caseData.getAssignedToLabel() != null && !caseData.getAssignedToLabel().isBlank()) {
    assignedToLabel = caseData.getAssignedToLabel();
} else {
    String caseAssignedToValue = rootNode.path("caseAssignedTo").asText();
    if (caseAssignedToValue != null && !caseAssignedToValue.isBlank()) {
        com.wks.caseengine.rest.db2.entity.Users resolvedUser = usersRepository.findByEmailId(caseAssignedToValue);
        if (resolvedUser != null) {
            assignedToLabel = resolvedUser.getUserId();
            reviewers.add(resolvedUser.getEmailId());
        } else {
            com.wks.caseengine.rest.db2.entity.Groups resolvedGroup = groupsRepository.findByGroupId(caseAssignedToValue);
            if (resolvedGroup != null) {
                assignedToLabel = resolvedGroup.getGroupId();
            }
        }
    }
}
data.put("assignedToLabel", assignedToLabel);

                caseEmailService.send(
                    from,
                    analysisTeamEmails.toArray(new String[0]),
                    "CASE MANAGEMENT : " + caseTitle,
                    reviewers.toArray(new String[0]),
                    null,
                    null,
                    "email-template",
                    data
                );
                System.out.println("Analysis Team notification sent to: " + analysisTeamEmails);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // ---- END Analysis Team notification ----

        caseDetails = caseRepository.save(caseData);
        return caseDetails;
    }
    private String getGEAPMRecommendationStatusAndUpdateRecommendationStatus(String geAPMAcsessToken, String recommendationNo) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("MeridiumToken", geAPMAcsessToken);

        Map<String, Object> requestBody = Map.of(
                "QueryPath", "Public\\Meridium\\Client\\APIs\\Recommendation_Status_EED",
                "Page", 0,
                "PageSize", 1000,
                "InputsingleParams", Map.of("ID", recommendationNo)
        );

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(geCaseStatusAPI, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> responseOutput = (Map<String, Object>) responseBody.getOrDefault("output", Map.of());
                Map<String, Object> usersData = (Map<String, Object>) responseOutput.getOrDefault("data", Map.of());
                List<Map<String, Object>> rows = (List<Map<String, Object>>) usersData.getOrDefault("rows", List.of());
                System.out.println("Recommendation Status API:- recommendationNo: " + recommendationNo);

                if (!rows.isEmpty()) {
                    System.out.println("Recommendation Status API:- recommendationStatus: " + (String) rows.get(0).getOrDefault("State Caption", ""));

                    return (String) rows.get(0).getOrDefault("State Caption", "");
                }
            }
        }catch(Exception e) {
            System.err.println("GE APM API call failed: " + e.getMessage());
        }
        return null; // Return null if API call fails
    }
    public Boolean checkFunctionalLocationAvailableInGEAPM(String geAPMAcsessToken, String functionalLocation) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("MeridiumToken", geAPMAcsessToken);
        Map<String, Object> requestBody = Map.of(
                "QueryPath", "Public\\Meridium\\Client\\APIs\\EED_APM_API",
                "Page", 0,
                "PageSize", 100,
                "InputsingleParams", Map.of("FL", functionalLocation)
        );
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(geUsersAPI, requestEntity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object rowCount = response.getBody().get("rowCount");
                return rowCount != null && Integer.parseInt(rowCount.toString()) == 1;
            }
        } catch (RestClientException e) {
//    public void scheduleTask() {
            System.err.println("GE APM API request failed: " + e.getMessage());
            throw new Exception("GE APM Check Available FL API request failed:"+ e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Invalid rowCount format in response: " + e.getMessage());
        }
//    }
        return false;
    }

    @Override
	public List<Case> filterCasesByCaseDefinitionId(String caseDefinitionId, String assetName, String hierarchyName, String search, String caseStatus) {
		return filterCasesByCaseDefinitionId(caseDefinitionId, assetName, hierarchyName, search, caseStatus, 10, 0);
	}

	public List<Case> filterCasesByCaseDefinitionId(String caseDefinitionId, String assetName, String hierarchyName, String search, String caseStatus, int limit, int offset) {
		StringBuilder query = new StringBuilder(
			"SELECT c.* FROM [CaseManagement].[dbo].[Cases] c " +
			"WHERE c.caseDefinitionId = :caseDefinitionId " +
			// "AND TRY_CAST(c.hierarchy_node_pk_id AS UNIQUEIDENTIFIER) IN (" +
			// 	"SELECT hn.HierarchyNode_PK_ID " +
			// 	"FROM [" + db1Name + "].[dbo].[HierarchyNodes] hn " +
			// 	"JOIN [" + db1Name + "].[dbo].[HierarchyTrees] ht " +
			// 	"ON hn.HierarchyTree_PK_ID = ht.HierarchyTree_PK_ID " +
			// 	"WHERE hn.IsDeleted = 0 " +
				"AND c.asset_name = :assetName " +
				"AND c.hierarchy_name = :hierarchyName" 
			// ")"
		);

		boolean hasSearch = search != null && !search.isBlank();
		boolean hasCaseStatus = caseStatus != null && !caseStatus.isBlank();

		if (hasSearch) {
			query.append(" AND (c.case_no LIKE :search OR c.path LIKE :search OR c.asset_name LIKE :search OR c.attributes LIKE :search)");
		}
		if (hasCaseStatus) {
			query.append(" AND c.status_id = :caseStatus");
		}

		query.append(" ORDER BY c.case_no DESC");
		query.append(" OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY");

		Query nativeQuery = entityManager.createNativeQuery(query.toString(), Case.class);
		nativeQuery.setParameter("caseDefinitionId", caseDefinitionId);
		nativeQuery.setParameter("assetName", assetName);
		nativeQuery.setParameter("hierarchyName", hierarchyName);
		nativeQuery.setParameter("offset", offset);
		nativeQuery.setParameter("limit", limit);
		if (hasSearch) {
			nativeQuery.setParameter("search", "%" + search + "%");
		}
		if (hasCaseStatus) {
			nativeQuery.setParameter("caseStatus", Long.parseLong(caseStatus));
		}

		return nativeQuery.getResultList();
	}

	@Override
	public long countCasesByCaseDefinitionId(String caseDefinitionId, String assetName, String hierarchyName, String search, String caseStatus) {
		StringBuilder query = new StringBuilder(
			"SELECT COUNT(*) FROM [CaseManagement].[dbo].[Cases] c " +
			"WHERE c.caseDefinitionId = :caseDefinitionId " +
			// "AND TRY_CAST(c.hierarchy_node_pk_id AS UNIQUEIDENTIFIER) IN (" +
			// 	"SELECT hn.HierarchyNode_PK_ID " +
			// 	"FROM [" + db1Name + "].[dbo].[HierarchyNodes] hn " +
			// 	"JOIN [" + db1Name + "].[dbo].[HierarchyTrees] ht " +
			// 	"ON hn.HierarchyTree_PK_ID = ht.HierarchyTree_PK_ID " +
			// 	"WHERE hn.IsDeleted = 0 " +
				"AND c.asset_name = :assetName " +
				"AND c.hierarchy_name = :hierarchyName"
			// ")"
		);

		boolean hasSearch = search != null && !search.isBlank();
		boolean hasCaseStatus = caseStatus != null && !caseStatus.isBlank();

		if (hasSearch) {
			query.append(" AND (c.case_no LIKE :search OR c.path LIKE :search OR c.asset_name LIKE :search OR c.attributes LIKE :search)");
		}
		if (hasCaseStatus) {
			query.append(" AND c.status_id = :caseStatus");
		}

		Query nativeQuery = entityManager.createNativeQuery(query.toString());
		nativeQuery.setParameter("caseDefinitionId", caseDefinitionId);
		nativeQuery.setParameter("assetName", assetName);
		nativeQuery.setParameter("hierarchyName", hierarchyName);
		if (hasSearch) {
			nativeQuery.setParameter("search", "%" + search + "%");
		}
		if (hasCaseStatus) {
			nativeQuery.setParameter("caseStatus", Long.parseLong(caseStatus));
		}

		Object result = nativeQuery.getSingleResult();
		return ((Number) result).longValue();
	}

    public Boolean checkUserAvailableInGEAPM(String geAPMAcsessToken, String userId) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("MeridiumToken", geAPMAcsessToken);
        Map<String, Object> requestBody = Map.of(
                "QueryPath",  "Public\\Meridium\\Client\\APIs\\UserValidation_EED_APM_API",
                "Page", 0,
                "PageSize", 100,
                "InputsingleParams", Map.of("Domain", userId)
        );
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(geUsersAPI, requestEntity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object rowCount = response.getBody().get("rowCount");
                return rowCount != null && Integer.parseInt(rowCount.toString()) == 1;
            }
        } catch (RestClientException e) {
            System.err.println("GE APM API request failed: " + e.getMessage());
            throw new Exception("GE APM Check Available User API request failed:"+ e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Invalid rowCount format in response: " + e.getMessage());
        }
        return false;
    }
    private com.wks.caseengine.rest.db2.entity.Users createUserFromMap(Map<String, Object> userMap) {
        com.wks.caseengine.rest.db2.entity.Users user = new com.wks.caseengine.rest.db2.entity.Users();
        user.setUserId(getString(userMap, "User ID"));
        user.setEmailId(getString(userMap, "User ID")); // Should this be "Email ID"?
        return user;
    }
    private String getString(Map<String, Object> map, String key) {
        return map.getOrDefault(key, "").toString();
    }
}
