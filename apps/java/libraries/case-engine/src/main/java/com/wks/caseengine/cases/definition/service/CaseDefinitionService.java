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

import java.util.List;
import java.util.Optional;

import com.wks.caseengine.cases.definition.CaseDefinition;
import com.wks.caseengine.rest.entity.CaseCauseCategory;
import com.wks.caseengine.rest.entity.CaseCauseDescription;
import com.wks.caseengine.rest.entity.CaseDetails;
import com.wks.caseengine.rest.entity.CaseStatus;
import com.wks.caseengine.rest.entity.FaultCategory;
import com.wks.caseengine.rest.entity.FaultHistory;
import com.wks.caseengine.rest.model.CasePayload;

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

	CaseDetails saveCaseDetails(CasePayload  casePayload);

	List<FaultHistory> getAllEvents(List<Long> eventIds);

//	Case saveCase(Case caseData);

}
