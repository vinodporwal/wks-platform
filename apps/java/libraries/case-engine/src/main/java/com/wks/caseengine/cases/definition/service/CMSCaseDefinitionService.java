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

import com.wks.caseengine.rest.db2.entity.Case;
import com.wks.caseengine.rest.db2.entity.CaseStatus;
import com.wks.caseengine.rest.model.Recommendations;

public interface CMSCaseDefinitionService {
	String CaseNoGenerator();

	List<CaseStatus> getAllCaseStatus();

	Case saveCMSCase(Case caseData);
	
	List<Case> getCMSCases(String caseDefinitionId);

	Case saveCMSCaseRecommendation(Recommendations recommendations);

	Case saveCMSCaseSiteRecommendation(Recommendations recommendations);

	Case saveCMSAnalysis(Case caseData);

	Case cmsCaseAssignment(Case caseData);

	Case cmsActionSubmit(Case caseData);

	Case cmsCaseClosure(Case caseData);
}
