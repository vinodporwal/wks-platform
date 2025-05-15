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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wks.caseengine.cases.instance.email.CaseEmailServiceImpl;
import com.wks.caseengine.rest.db2.entity.Case;
import com.wks.caseengine.rest.db2.entity.CaseAndRecommendationsMapping;
import com.wks.caseengine.rest.db2.entity.CaseIdSequences;
import com.wks.caseengine.rest.db2.entity.CaseStatus;
import com.wks.caseengine.rest.db2.repository.CaseIdSequenceRepository;
import com.wks.caseengine.rest.db2.repository.CaseRecommendationMappingRepository;
import com.wks.caseengine.rest.db2.repository.CaseRepository;
import com.wks.caseengine.rest.db2.repository.CaseStatusRepository;
import com.wks.caseengine.rest.db2.repository.CasesAndEventsMappingRepository;
import com.wks.caseengine.rest.db2.repository.UsersRepository;
import com.wks.caseengine.rest.model.Attribute;
import com.wks.caseengine.rest.model.Recommendations;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Component
public class CMSCaseDefinitionServiceImpl implements CMSCaseDefinitionService {
	@Autowired
	private CaseStatusRepository caseStatusRepository;

	@Autowired
	private CaseRepository caseRepository;

	@Autowired
	private CaseIdSequenceRepository caseIdSequenceRepository;

	@Autowired
	private CasesAndEventsMappingRepository casesAndEventsMappingRepository;

	@Autowired
	private CaseRecommendationMappingRepository caseRecommendationMappingRepository;

	@Autowired
	private CaseEmailServiceImpl caseEmailService;

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
	@Value("${ge.case_status.api}")
	private String geCaseStatusAPI;

	@Override
	public String CaseNoGenerator() {
		CaseIdSequences caseId = caseIdSequenceRepository.findLastElement();
		Long id = Long.parseLong(caseId.getCaseNo()) + 1;
		caseId.setCaseNo(id + "");
		caseIdSequenceRepository.save(caseId);
		return caseId.getCaseNo();
	}

	@Override
	public List<CaseStatus> getAllCaseStatus() {
		List<CaseStatus> caseStatusList = caseStatusRepository.findAll();
		return caseStatusList;
	}

	@Override
	public List<Case> getCMSCases(String caseDefinitionId) {
		String query = "SELECT c.* FROM [CaseManagement].[dbo].[Cases] c "
				+ "WHERE c.caseDefinitionId = :caseDefinitionId ORDER BY c.case_no DESC";

		Query nativeQuery = entityManager.createNativeQuery(query, Case.class);
		nativeQuery.setParameter("caseDefinitionId", caseDefinitionId);

		List<Case> cases = nativeQuery.getResultList();
		return cases;
	}

	@Override
	public Case saveCMSAnalysis(Case caseData) {
		if (caseData.getAssignedTo() != null) {
			caseData.setAssignedTo(usersRepository.findByEmailId(caseData.getAssignedTo().getEmailId()));
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
		if (caseData.getCaseUrl() != null && !caseData.getCaseUrl().contains("?caseNo")) {
			caseData.setCaseUrl(caseData.getCaseUrl() + "?caseNo=" + caseNo);
		}
		Case savedCase = caseRepository.getByCaseNo(caseNo);
		caseData.setCreationDate(savedCase.getCreationDate());
		caseDetails = caseRepository.save(caseData);
		return caseDetails;
	}

	@Override
	public Case saveCMSCase(Case caseData) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		String currentDate = now.format(formatter);
		Case caseDetails = new Case();
		String caseNo = "";
		Long statusId = null;

		if (caseData.getAssignedTo() != null) {
			caseData.setAssignedTo(usersRepository.findByEmailId(caseData.getAssignedTo().getEmailId()));
		}

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

		if (caseNo == null || caseNo.length() == 0) {
			caseNo = CaseNoGenerator();
			caseData.setCaseNo(caseNo);
			caseData.setCaseUrl(caseData.getCaseUrl() + "?caseNo=" + caseNo);
			caseData.setCreationDate(currentDate);
			caseDetails = caseRepository.save(caseData);

		} else {
			caseData.setCaseNo(caseNo);
			if (caseData.getCaseUrl() != null && !caseData.getCaseUrl().contains("?caseNo")) {
				caseData.setCaseUrl(caseData.getCaseUrl() + "?caseNo=" + caseNo);
			}
			Case savedCase = caseRepository.getByCaseNo(caseNo);
			caseData.setCreationDate(savedCase.getCreationDate());
			caseDetails = caseRepository.save(caseData);
		}

		if (!caseData.getIsDraft().equals("y")) {
			attributeValue = attributeValue.replace("\\\"", "\"");

			try {

				ObjectMapper objectMapper = new ObjectMapper();
				JsonNode rootNode = objectMapper.readTree(attributeValue);
				String assignedTo = rootNode.path("caseCreatedBy").asText();
				String caseNumber = caseData.getCaseNo();
				String caseTitle = rootNode.path("caseTitle").asText();
				System.out.println(rootNode.path("caseCreatedBy").asText());
				Long caseStatusNo = rootNode.path("caseStatus").asLong();
				Optional<CaseStatus> caseStatus = getAllCaseStatus().stream()
						.filter(status -> status.getId().equals(caseStatusNo)).findFirst();
				String caseStatusValue = caseStatus.get().getName();
				JsonNode analysisTeam = rootNode.path("caseAssignedTo");
				String[] reviewers = new String[analysisTeam.size()];
				if (analysisTeam.isArray()) {
					int counter = 0;
					for (JsonNode dataGridEntry : analysisTeam) {
						reviewers[counter] = dataGridEntry.asText();
						counter++;
					}

				}
				if (!caseStatusValue.equals("Under Analysis")) {
					System.out.println("Calling mail send method...");
//			    	String from = "amol.borse@honeywell.com";
					Map<String, Object> data = new HashMap<>();
					data.put("caseTitle", "This is to inform you, the new case has been assined to you");
					data.put("caseNumber", caseNumber);
					data.put("status", caseStatusValue);
					data.put("caseName", caseTitle);
					data.put("caseUrl", caseDetails.getCaseUrl());
					data.put("environment", "");
					caseTitle = "CASE MANAGEMENT :" + caseTitle;
					caseEmailService.send(from, assignedTo, caseTitle, reviewers, null, null, "email-template", data);
					// (assignedTo, caseNumber, caseTitle, caseStatusValue, reviewers);
				}

				caseData.setAttributes(attributes);
				caseDetails = caseRepository.save(caseData);
				return caseDetails;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return caseDetails;
	}

	@Override
	public Case saveCMSCaseRecommendation(Recommendations recommendation) {
		String caseNo = recommendation.getCaseNo();
		Case caseDetails = caseRepository.getByCaseNo(caseNo);
		for (Attribute attribute : caseDetails.getAttributes()) {
			String attributeValue = attribute.getValue();
			String updatedAttributeValue = saveCMSCaseRecommendations(attributeValue, caseNo, recommendation);
			sendMailToAssignedPerson(caseDetails);
			updatedAttributeValue = removeUnwantedCMSRecommendations(updatedAttributeValue);
			attribute.setValue(updatedAttributeValue);
		}
		System.out.println("After processing everything...");
		System.out.println("..." + caseDetails.getAttributes().get(0).getValue());
		caseDetails = caseRepository.save(caseDetails);
		return caseDetails;
	}

	private String saveCMSCaseRecommendations(String attributeValue, String caseNo, Recommendations newRecommendation) {
		attributeValue = attributeValue.replace("\\\"", "\"");

		System.out.println("Attribute Value: " + attributeValue);

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode rootNode = objectMapper.readTree(attributeValue);

			// Navigate to the "dataGrid1" array
			JsonNode recommendationNode = rootNode.path("dataGrid1");
			if (recommendationNode.isArray()) {
				ArrayNode dataGridArray = (ArrayNode) recommendationNode; // Cast to ArrayNode for appending new
																			// elements

				// Convert the new recommendation object to a JSON node
				ObjectNode newRecommendationNode = objectMapper.createObjectNode();
				newRecommendationNode.put("recommendationCategory", newRecommendation.getRecommendationCategory());
				newRecommendationNode.put("recommendationDescription",
						newRecommendation.getRecommendationDescription1());
				newRecommendationNode.put("recommendationAssignedTo", newRecommendation.getRecommendationAssignedTo2());
				newRecommendationNode.put("recommendationStatus", newRecommendation.getRecommendationStatus());
				newRecommendationNode.put("recommendationTargetDate",
						newRecommendation.getRecommendationTargetCompletionDate1());
				newRecommendationNode.put("recommendationNo", newRecommendation.getRecommendationNo1());
				newRecommendationNode.put("recommendationSubmit", newRecommendation.getRecommendationSubmit());
				newRecommendationNode.put("createdBy", newRecommendation.getCreatedBy());

				// Append the new recommendation node to the dataGrid1 array

				String[] recommendationStatusAndId = saveCMSCaseRecommendationMapping(newRecommendationNode, caseNo,
						newRecommendation.getRecommendationAssignedTo2(),
						newRecommendation.getRecommendationReviewer());

				newRecommendationNode.put("recommendationNo", recommendationStatusAndId[0]);
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

	private String[] saveCMSCaseRecommendationMapping(JsonNode dataGridEntry, String caseNo, String assignedUserId,
			String reviewerUserId) throws Exception {
		String[] recommendationStatusAndId = saveCMSCaseRecommendationAPI(dataGridEntry, caseNo, assignedUserId,
				reviewerUserId);
		CaseAndRecommendationsMapping caseRecommendationMapping = new CaseAndRecommendationsMapping();
		caseRecommendationMapping.setCaseNo(caseNo);
		caseRecommendationMapping.setRecId(recommendationStatusAndId[0]);
		caseRecommendationMapping.setRecommendationJson(dataGridEntry.toPrettyString().toString());
		caseRecommendationMappingRepository.save(caseRecommendationMapping);

		return recommendationStatusAndId;
	}

	private String[] saveCMSCaseRecommendationAPI(JsonNode dataGridEntry, String caseNo, String assignedUserId,
			String reviewerUserId) throws Exception {
//		sendMailToAssignedPerson(assignedUserId);
//		sendMailToReviewerPerson(reviewerUserId);

		String prefix = "REC-";
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

	@Override
	public Case saveCMSCaseSiteRecommendation(Recommendations recommendation) {
		String caseNo = recommendation.getCaseNo();
		Case caseDetails = caseRepository.getByCaseNo(caseNo);
		for (Attribute attribute : caseDetails.getAttributes()) {
			String attributeValue = attribute.getValue();
			String updatedAttributeValue = savePICaseSiteRecommendations(attributeValue, caseNo, recommendation);
			updatedAttributeValue = removeUnwantedCMSRecommendations(updatedAttributeValue);
			attribute.setValue(updatedAttributeValue);
		}
		System.out.println("After processing everything...");
		System.out.println("..." + caseDetails.getAttributes().get(0).getValue());
		caseDetails = caseRepository.save(caseDetails);
		return caseDetails;
	}

	private String savePICaseSiteRecommendations(String attributeValue, String caseNo,
			Recommendations newRecommendation) {
		attributeValue = attributeValue.replace("\\\"", "\"");

		System.out.println("Attribute Value: " + attributeValue);

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode rootNode = objectMapper.readTree(attributeValue);

			// Navigate to the "dataGrid1" array
			JsonNode recommendationNode = rootNode.path("siteRecommendations");
			if (recommendationNode.isArray()) {
				ArrayNode dataGridArray = (ArrayNode) recommendationNode; // Cast to ArrayNode for appending new
																			// elements

				// Convert the new recommendation object to a JSON node
				ObjectNode newRecommendationNode = objectMapper.createObjectNode();
				newRecommendationNode.put("siteRecommendationHeadline", newRecommendation.getRecommendationHeadline());
				newRecommendationNode.put("siteRecommendationDescription",
						newRecommendation.getRecommendationDescription1());
				newRecommendationNode.put("siteRecommendationAssignedTo",
						newRecommendation.getRecommendationAssignedTo2());
				newRecommendationNode.put("siteRecommendationStatus", newRecommendation.getRecommendationStatus());
				newRecommendationNode.put("siteRecommendationTargetCompletionDate",
						newRecommendation.getRecommendationTargetCompletionDate1());
				newRecommendationNode.put("siteRecommendationNo", newRecommendation.getRecommendationNo1());
				newRecommendationNode.put("RecommendationSubmit", newRecommendation.getRecommendationSubmit());
				newRecommendationNode.put("siteRecommendationSubmit", newRecommendation.isRecommendationSubmit3());
				newRecommendationNode.put("createdBy", newRecommendation.getCreatedBy());

				// Append the new recommendation node to the dataGrid1 array

				String[] recommendationStatusAndId = saveCMSCaseRecommendationMapping(newRecommendationNode, caseNo,
						newRecommendation.getRecommendationAssignedTo2(),
						newRecommendation.getRecommendationReviewer());

				newRecommendationNode.put("siteRecommendationNo", recommendationStatusAndId[0]);
				newRecommendationNode.put("siteRecommendationStatus", recommendationStatusAndId[1]);
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

	private String removeUnwantedCMSRecommendations(String attribute) {
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
					String recNumber = dataGridEntry.path("recommendationNo").asText();

					// Remove the entry if recommendationNo1 is empty or null
					if (recNumber == null || recNumber.isEmpty()) {
						arrayNode.remove(i);
					}
				}
				String updatedAttributeValue = objectMapper.writeValueAsString(rootNode);
				System.out.println("After Saving Recommendation" + updatedAttributeValue);
				return updatedAttributeValue;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Case cmsCaseAssignment(Case caseData) {
		Case caseDetails = updateCMSCase(caseData);
		List<Attribute> attributes = caseDetails.getAttributes();
		Attribute attribute = attributes.get(0);
		String attributeValue = attribute.getValue();

		attributeValue = attributeValue.replace("\\\"", "\"");

		try {

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode rootNode = objectMapper.readTree(attributeValue);
			String assignedTo = rootNode.path("caseCreatedBy").asText();
			String caseNumber = caseData.getCaseNo();
			String caseTitle = rootNode.path("caseTitle").asText();
			System.out.println(rootNode.path("caseAssignedTo").asText());
			Long caseStatusNo = rootNode.path("caseStatus").asLong();
			Optional<CaseStatus> caseStatus = getAllCaseStatus().stream()
					.filter(status -> status.getId().equals(caseStatusNo)).findFirst();
			String caseStatusValue = caseStatus.get().getName();
			JsonNode analysisTeam = rootNode.path("caseAssignedTo");
			String[] reviewers = new String[analysisTeam.size()];
			if (analysisTeam.isArray()) {
				int counter = 0;
				for (JsonNode dataGridEntry : analysisTeam) {
					reviewers[counter] = dataGridEntry.asText();
					counter++;
				}

			}

			System.out.println("Calling mail send method...");
			String[] ccUsers = new String[0];
			Map<String, Object> data = new HashMap<>();
			data.put("caseTitle", "This is to inform you, the new case has been assined to you");
			data.put("caseNumber", caseNumber);
			data.put("status", caseStatusValue);
			data.put("caseName", caseTitle);
			data.put("caseUrl", caseDetails.getCaseUrl());
			data.put("environment", "");
			caseTitle = "CASE MANAGEMENT :" + caseTitle;
			for(String reviewer: reviewers){
				caseEmailService.send(from, reviewer, caseTitle, ccUsers, null, null, "email-template", data);
			}

			caseData.setAttributes(attributes);
			caseDetails = caseRepository.save(caseData);
			return caseDetails;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return caseDetails;
	}

	@Override
	public Case cmsActionSubmit(Case caseData) {
		Case caseDetails = updateCMSCase(caseData);
		List<Attribute> attributes = caseDetails.getAttributes();
		Attribute attribute = attributes.get(0);
		String attributeValue = attribute.getValue();

		attributeValue = attributeValue.replace("\\\"", "\"");

		try {

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode rootNode = objectMapper.readTree(attributeValue);
			String assignedTo = rootNode.path("caseCreatedBy").asText();
			String caseNumber = caseData.getCaseNo();
			String caseTitle = rootNode.path("caseTitle").asText();
			System.out.println(rootNode.path("caseAssignedTo").asText());
			Long caseStatusNo = rootNode.path("caseStatus").asLong();
			Optional<CaseStatus> caseStatus = getAllCaseStatus().stream()
					.filter(status -> status.getId().equals(caseStatusNo)).findFirst();
			String caseStatusValue = caseStatus.get().getName();
			JsonNode analysisTeam = rootNode.path("caseAssignedTo");
			String[] reviewers = new String[analysisTeam.size()];
			if (analysisTeam.isArray()) {
				int counter = 0;
				for (JsonNode dataGridEntry : analysisTeam) {
					reviewers[counter] = dataGridEntry.asText();
					counter++;
				}

			}

			System.out.println("Calling mail send method...");
			String[] ccUsers = new String[0];
			Map<String, Object> data = new HashMap<>();
			data.put("caseTitle", "This is to inform you, the new case has been assined to you");
			data.put("caseNumber", caseNumber);
			data.put("status", caseStatusValue);
			data.put("caseName", caseTitle);
			data.put("caseUrl", caseDetails.getCaseUrl());
			data.put("environment", "");
			caseTitle = "CASE MANAGEMENT :" + caseTitle;
				
			caseEmailService.send(from, assignedTo, caseTitle, ccUsers, null, null, "email-template", data);
			

			caseData.setAttributes(attributes);
			caseDetails = caseRepository.save(caseData);
			return caseDetails;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return caseDetails;
	}

	@Override
	public Case cmsCaseClosure(Case caseData) {
		Case caseDetails = updateCMSCase(caseData);
		List<Attribute> attributes = caseDetails.getAttributes();
		Attribute attribute = attributes.get(0);
		String attributeValue = attribute.getValue();

		attributeValue = attributeValue.replace("\\\"", "\"");

		try {

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode rootNode = objectMapper.readTree(attributeValue);
			String assignedTo = rootNode.path("caseCreatedBy").asText();
			String caseNumber = caseData.getCaseNo();
			String caseTitle = rootNode.path("caseTitle").asText();
			System.out.println(rootNode.path("caseAssignedTo").asText());
			Long caseStatusNo = rootNode.path("caseStatus").asLong();
			Optional<CaseStatus> caseStatus = getAllCaseStatus().stream()
					.filter(status -> status.getId().equals(caseStatusNo)).findFirst();
			String caseStatusValue = caseStatus.get().getName();
			JsonNode analysisTeam = rootNode.path("caseAssignedTo");
			String[] reviewers = new String[analysisTeam.size()];
			if (analysisTeam.isArray()) {
				int counter = 0;
				for (JsonNode dataGridEntry : analysisTeam) {
					reviewers[counter] = dataGridEntry.asText();
					counter++;
				}

			}

			System.out.println("Calling mail send method...");
			Map<String, Object> data = new HashMap<>();
			data.put("caseTitle", "This is to inform you, the new case has been assined to you");
			data.put("caseNumber", caseNumber);
			data.put("status", caseStatusValue);
			data.put("caseName", caseTitle);
			data.put("caseUrl", caseDetails.getCaseUrl());
			data.put("environment", "");
			caseTitle = "CASE MANAGEMENT :" + caseTitle;
				
			caseEmailService.send(from, assignedTo, caseTitle, reviewers, null, null, "email-template", data);
			

			caseData.setAttributes(attributes);
			caseDetails = caseRepository.save(caseData);
			return caseDetails;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return caseDetails;
	}

	private Case updateCMSCase(Case caseData) {
		Case caseDetails = new Case();
		String caseNo = "";
		Long statusId = null;

		if (caseData.getAssignedTo() != null) {
			caseData.setAssignedTo(usersRepository.findByEmailId(caseData.getAssignedTo().getEmailId()));
		}

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

		caseData.setCaseNo(caseNo);
		if (caseData.getCaseUrl() != null && !caseData.getCaseUrl().contains("?caseNo")) {
			caseData.setCaseUrl(caseData.getCaseUrl() + "?caseNo=" + caseNo);
		}
		Case savedCase = caseRepository.getByCaseNo(caseNo);
		caseData.setCreationDate(savedCase.getCreationDate());
		caseDetails = caseRepository.save(caseData);
		
		return caseDetails;
	}
	
	
	private void sendMailToAssignedPerson(Case caseDetails) {
		List<Attribute> attributes = caseDetails.getAttributes();
		Attribute attribute = attributes.get(0);
		String attributeValue = attribute.getValue();

		attributeValue = attributeValue.replace("\\\"", "\"");

		try {

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode rootNode = objectMapper.readTree(attributeValue);
			String assignedTo = rootNode.path("caseCreatedBy").asText();
			String caseNumber = caseDetails.getCaseNo();
			String caseTitle = rootNode.path("caseTitle").asText();
			Long caseStatusNo = rootNode.path("caseStatus").asLong();
			Optional<CaseStatus> caseStatus = getAllCaseStatus().stream()
					.filter(status -> status.getId().equals(caseStatusNo)).findFirst();
			String caseStatusValue = caseStatus.get().getName();
			
			String[] reviewers = new String[0];
			Map<String, Object> data = new HashMap<>();
			data.put("caseTitle", "This is to inform you, the new case has been assined to you");
			data.put("caseNumber", caseNumber);
			data.put("status", caseStatusValue);
			data.put("caseName", caseTitle);
			data.put("caseUrl", caseDetails.getCaseUrl());
			data.put("environment", "");
			caseTitle = "CASE MANAGEMENT :" + caseTitle;
				
			caseEmailService.send(from, assignedTo, caseTitle, reviewers, null, null, "email-template", data);
		} catch (Exception e) {
			e.printStackTrace();
		}
			


	}
}
