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
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.wks.caseengine.cases.definition.CaseDefinition;
import com.wks.caseengine.cases.instance.CaseComment;
import com.wks.caseengine.rest.db2.entity.Case;
import com.wks.caseengine.rest.db2.entity.CaseCauseCategory;
import com.wks.caseengine.rest.db2.entity.CaseCauseDescription;
import com.wks.caseengine.rest.db2.entity.CaseStatus;
import com.wks.caseengine.rest.db2.entity.FaultCategory;
import com.wks.caseengine.rest.model.FaultEvents;
import com.wks.caseengine.rest.model.FunctionalLocation;
import com.wks.caseengine.rest.model.Recommendations;
import com.wks.caseengine.rest.model.Users;

public interface CaseDefinitionService {

	List<CaseDefinition> find(final Optional<Boolean> deployed);

	CaseDefinition get(final String caseDefId);

	CaseDefinition create(final CaseDefinition caseDefinition);

	CaseDefinition update(final String caseDefId, CaseDefinition caseDefinition);

	void delete(final String caseDefinitionId);

	List<FaultCategory> findCaseCatagories();
	
	List<CaseStatus> getAllCaseStatus();
	
	List<CaseCauseCategory> getAllCategories();
	
	List<CaseCauseDescription> getDescriptionsByCategory(Long categoryId);

//	CaseDetails saveCaseDetails(CasePayload  casePayload);
//
	List<FaultEvents> getAllEvents(List<Long> eventIds);

	Case saveCase(Case caseData);

	String CaseNoGenerator();
//
	List<Case> getCaseDetails(String assetName, String hierarchyName);
//
	List<Case> getCaseDetails(LocalDate from, LocalDate to, String status);

	List<Users> getUserList();

	//List<com.wks.caseengine.rest.db2.entity.Users> getUsersList();

	List<com.wks.caseengine.rest.model.UserDTO> getUsersList();
		
	List<com.wks.caseengine.rest.model.UserDTO> getUsersByEmailIds(String emailIds);
	
	List<com.wks.caseengine.rest.model.UserDTO> searchUsers(String search, int limit, int skip);

	List<FunctionalLocation> getFunctionalLocations(String AssetName);

	Case addRecommendation(Recommendations recommendations);

	void sendEmail(String emailId, String subject, String body);

//	List<com.wks.caseengine.rest.db2.entity.Users> getAllUsersFromAD();

	List<com.wks.caseengine.rest.db2.entity.Users> getGEUsers() throws Exception;

	List<Case> updateRecommendationStatus() throws JsonMappingException, JsonProcessingException, Exception;

	Case saveAnalysis(Case caseData);

	List<Case> getCasesByCaseDefinitionId(String caseDefinitionId, String assetName, String hierarchyName);

	List<Case> filterCasesByCaseDefinitionId(String caseDefinitionId, String assetName, String hierarchyName, String search, String caseStatus);

	Case savePICase(Case caseData);
	
	Case saveValueRealization(Case caseData);
	
	Case submitFinalRecommendation(Case caseData);

	void dispatchCommentNotification(String caseNumber, CaseComment comment);

	Case updateCase(Case caseData);

	List<Case> updateCaseStatus() throws Exception;

	

	byte[] exportCasesToExcel(String caseDefinitionId,String assetName,String hierarchyName);


}
