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
package com.wks.caseengine.rest.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.cases.definition.CaseDefinition;
import com.wks.caseengine.cases.definition.CaseDefinitionNotFoundException;
import com.wks.caseengine.cases.definition.service.CaseDefinitionService;
import com.wks.caseengine.rest.entity.Case;
import com.wks.caseengine.rest.entity.CaseCauseCategory;
import com.wks.caseengine.rest.entity.CaseCauseDescription;
import com.wks.caseengine.rest.entity.CaseDetails;
import com.wks.caseengine.rest.entity.CaseStatus;
import com.wks.caseengine.rest.entity.FaultCategory;
import com.wks.caseengine.rest.entity.FaultHistory;
import com.wks.caseengine.rest.entity.FunctionalLocation;
import com.wks.caseengine.rest.exception.RestInvalidArgumentException;
import com.wks.caseengine.rest.exception.RestResourceNotFoundException;
import com.wks.caseengine.rest.model.CasePayload;
import com.wks.caseengine.rest.model.FaultEvents;
import com.wks.caseengine.rest.model.Recommendations;
import com.wks.caseengine.rest.model.Users;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("case-definition")
@Tag(name = "Case Definition", description = "A Case Definition is the 'template' for the creation of new Cases Instances. If defines which attributes, stages and processes definitions will be used by Cases Instances created from it")
public class CaseDefinitionController {

	@Autowired
	private CaseDefinitionService caseDefinitionService;

	@GetMapping
	public ResponseEntity<List<CaseDefinition>> find(@RequestParam(required = false) Boolean deployed) {
		System.out.println("Called.. Cakkcvdshgcvshkdgcvkshcvkhsagvckhgvdks");
		return ResponseEntity.ok(caseDefinitionService.find(Optional.ofNullable(deployed)));
	}
	
	@GetMapping(value = "/fault-category")
	public ResponseEntity<List<FaultCategory>> getFaultCategory() {
		return ResponseEntity.ok(caseDefinitionService.findCaseCatagories());
	}
	
	@GetMapping(value = "/case-status")
	public ResponseEntity<List<CaseStatus>> getCaseStatus() {
		return ResponseEntity.ok(caseDefinitionService.getAllCaseStatus());
	}
	
	@GetMapping("/categories")
    public List<CaseCauseCategory> getCategories() {
        return caseDefinitionService.getAllCategories();
    }

    @GetMapping("/descriptions")
    public ResponseEntity<List<CaseCauseDescription>> getDescriptions(@RequestParam Long categoryId) {
        return ResponseEntity.ok(caseDefinitionService.getDescriptionsByCategory(categoryId));
    }
    
    @PostMapping("/case-details")
    public void createCaseDetails(@RequestBody CasePayload casePayload) {
        CaseDetails savedCaseDetails = caseDefinitionService.saveCaseDetails(casePayload);
//        return caseDefinitionService.saveCaseDetails(caseDetails);
    }

	@GetMapping(value = "/{caseDefId}")
	public ResponseEntity<CaseDefinition> get(@PathVariable final String caseDefId) {
		try {
			return ResponseEntity.ok(caseDefinitionService.get(caseDefId));
		} catch (CaseDefinitionNotFoundException e) {
			throw new RestResourceNotFoundException(e.getMessage());
		}
	}
	
	@GetMapping(value = "/fault-history/eventIds")
	public ResponseEntity<List<FaultEvents>> getFaultHistoryByEventIds(@RequestParam List<Long> eventIds) {
        List<FaultHistory> faultHistories = new ArrayList<FaultHistory>();
        String eventIdsString = eventIds.stream()
                .map(String::valueOf) // Convert Long to String
                .collect(Collectors.joining(","));
        System.out.println("eventIds: "+eventIdsString);
        return ResponseEntity.ok(caseDefinitionService.getAllEvents(eventIds));
    }
	
	@GetMapping(value = "/case-no")
	public ResponseEntity<String> getCaseNumber() {
        return ResponseEntity.ok(caseDefinitionService.CaseNoGenerator());
    }

	@PostMapping
	public ResponseEntity<CaseDefinition> save(@RequestBody final CaseDefinition caseDefinition) {
		try {
			return ResponseEntity.ok(caseDefinitionService.create(caseDefinition));
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("caseDefinitionId", e);
		}
	}
	
	@PostMapping("/save-case")
    public ResponseEntity<Case> createCase(@RequestBody Case caseData) {
        Case savedCase = caseDefinitionService.saveCase(caseData);
        return ResponseEntity.ok(savedCase);
    }
	
	@PostMapping("/save-recommendation")
    public ResponseEntity<Case> addRecommendation(@RequestBody Recommendations recommendations) {
        Case savedCase = caseDefinitionService.addRecommendation(recommendations);
        return ResponseEntity.ok(savedCase);
    }
	
	@GetMapping("/cases")
	public ResponseEntity<List<Case>> getCases(@RequestParam String assetName, @RequestParam String hierarchyName) {
		System.out.println("AssetName: "+assetName);
		System.out.println("HierarchyName: "+hierarchyName);
		List<Case> cases = caseDefinitionService.getCaseDetails(assetName, hierarchyName);
		return ResponseEntity.ok(cases);
	}

	@PutMapping(value = "/{caseDefId}")
	public ResponseEntity<CaseDefinition> update(@PathVariable final String caseDefId,
			@RequestBody final CaseDefinition caseDefinition) {
		try {
			return ResponseEntity.ok(caseDefinitionService.update(caseDefId, caseDefinition));
		} catch (CaseDefinitionNotFoundException e) {
			throw new RestResourceNotFoundException(e.getMessage());
		}
	}
	
	@GetMapping(value = "/users")
	public ResponseEntity<List<Users>> getRecommondationUsers() {
		try {
			return ResponseEntity.ok(caseDefinitionService.getUserList());
		} catch (CaseDefinitionNotFoundException e) {
			throw new RestResourceNotFoundException(e.getMessage());
		}
	}

	@GetMapping(value = "/funcational-locations")
	public ResponseEntity<List<FunctionalLocation>> getRecommondationUsers(@RequestParam List<Long> eventIds) {
		try {
			System.out.println("EventId ...: "+eventIds);
			return ResponseEntity.ok(caseDefinitionService.getFunctionalLocations(eventIds));
		} catch (CaseDefinitionNotFoundException e) {
			throw new RestResourceNotFoundException(e.getMessage());
		}
	}
	
	@DeleteMapping(value = "/{caseDefId}")
	public ResponseEntity<Void> delete(@PathVariable final String caseDefId) {
		try {
			caseDefinitionService.delete(caseDefId);
		} catch (CaseDefinitionNotFoundException e) {
			throw new RestResourceNotFoundException(e.getMessage());
		}
		return ResponseEntity.noContent().build();
	}
}
