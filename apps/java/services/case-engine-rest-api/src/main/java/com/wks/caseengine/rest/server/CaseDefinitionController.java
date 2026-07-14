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

import java.time.LocalDate;
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
import com.wks.caseengine.pagination.PageResult;
import com.wks.caseengine.rest.db2.entity.Case;
import com.wks.caseengine.rest.db2.entity.CaseCauseCategory;
import com.wks.caseengine.rest.db2.entity.CaseCauseDescription;
import com.wks.caseengine.rest.db2.entity.CaseStatus;
import com.wks.caseengine.rest.db2.entity.FaultCategory;
import com.wks.caseengine.rest.exception.RestInvalidArgumentException;
import com.wks.caseengine.rest.exception.RestResourceNotFoundException;
import com.wks.caseengine.rest.model.FaultEvents;
import com.wks.caseengine.rest.model.FunctionalLocation;
import com.wks.caseengine.rest.model.Recommendations;

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
    
//    @PostMapping("/case-details")
//    public void createCaseDetails(@RequestBody CasePayload casePayload) {
//        CaseDetails savedCaseDetails = caseDefinitionService.saveCaseDetails(casePayload);
////        return caseDefinitionService.saveCaseDetails(caseDetails);
//    }

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
       try {
		String eventIdsString = eventIds.stream()
                .map(String::valueOf) // Convert Long to String
                .collect(Collectors.joining(","));
        System.out.println("eventIds: "+eventIdsString);
        return ResponseEntity.ok(caseDefinitionService.getAllEvents(eventIds));
       } catch(Exception e) {
    	   throw new RestResourceNotFoundException(e.getMessage());
       }
    }
	
	@GetMapping(value = "/case-no")
	public ResponseEntity<String> getCaseNumber() {
        return ResponseEntity.ok(caseDefinitionService.CaseNoGenerator());
    }

	@PostMapping(value = "/link-events")
	public ResponseEntity<?> linkEventsToCase(@RequestBody java.util.Map<String, Object> request) {
		try {
			String businessKey = (String) request.get("businessKey");
			@SuppressWarnings("unchecked")
			List<Number> eventIdNums = (List<Number>) request.get("eventIds");
			List<Long> eventIds = eventIdNums.stream().map(Number::longValue).collect(Collectors.toList());

			caseDefinitionService.linkEventsToCase(businessKey, eventIds);
			return ResponseEntity.ok(java.util.Map.of("status", "updated", "businessKey", businessKey));
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Failed to link events: " + e.getMessage());
		}
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

	//Controller API For Search and Filtering on case list page
	@GetMapping("/cases/{caseDefinitionId}/filter")
	public ResponseEntity<List<Case>> filterCasesByCaseDefinition(
			@PathVariable("caseDefinitionId") String caseDefinitionId,
			@RequestParam String assetName,
			@RequestParam String hierarchyName,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) String caseStatus,
			@RequestParam(defaultValue = "10") int limit,
			@RequestParam(defaultValue = "0") int offset) {
		List<Case> cases = caseDefinitionService
				.filterCasesByCaseDefinitionId(caseDefinitionId, assetName, hierarchyName, search, caseStatus, limit, offset);
		return ResponseEntity.ok(cases);
	}

	@GetMapping("/cases/{caseDefinitionId}/count")
	public ResponseEntity<Long> countCasesByCaseDefinition(
			@PathVariable("caseDefinitionId") String caseDefinitionId,
			@RequestParam String assetName,
			@RequestParam String hierarchyName,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) String caseStatus) {
		long count = caseDefinitionService.countCasesByCaseDefinitionId(caseDefinitionId, assetName, hierarchyName, search, caseStatus);
		return ResponseEntity.ok(count);
	}
	
	@GetMapping("/cases-to-link")
	public ResponseEntity<Object> getCasesToLink(
			@RequestParam String assetName,
			@RequestParam(required = false) String hierarchyName,
			@RequestParam(required = false) String eventIds,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String caseDefinitionId,
			@RequestParam(required = false, name = "before") String before,
			@RequestParam(required = false, name = "after") String after,
			@RequestParam(required = false, name = "sort") String sort,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false, name = "limit") String limit) {
		int pageSize = (limit != null && !limit.isEmpty()) ? Integer.parseInt(limit) : size;
		List<Case> cases = caseDefinitionService.getCaseDetails(assetName, hierarchyName != null ? hierarchyName : "", page, pageSize);
		long total = com.wks.caseengine.cases.definition.service.CaseDefinitionServiceImpl.totalHolder.get();
		com.wks.caseengine.cases.definition.service.CaseDefinitionServiceImpl.totalHolder.remove();
		boolean hasNext = (long)(page + 1) * pageSize < total;
		boolean hasPrevious = page > 0;
		PageResult<Case> result = new PageResult<>(cases, hasNext, hasPrevious,
				hasNext ? page + 1 : null,
				hasPrevious ? page - 1 : null,
				org.springframework.data.domain.Sort.Direction.DESC, pageSize);
		return ResponseEntity.ok(result.toJson());
	}

	@GetMapping("/cases/filter")
	public ResponseEntity<List<Case>> getCasesByFilter(@RequestParam(required = false) LocalDate from,@RequestParam(required = false) LocalDate to,@RequestParam(required = false) String status) {
		List<Case> cases = caseDefinitionService.getCaseDetails(from, to, status);
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
	public ResponseEntity<List<?>> getRecommondationUsers() {
		try {
			return ResponseEntity.ok(caseDefinitionService.getUsersList());
		} catch (CaseDefinitionNotFoundException e) {
			throw new RestResourceNotFoundException(e.getMessage());
		}
	}

    @GetMapping(value = "/groups")
    public ResponseEntity<List<?>> getRecommondationGroups() {
        try {
            return ResponseEntity.ok(caseDefinitionService.getGroupsList());
        } catch (CaseDefinitionNotFoundException e) {
            throw new RestResourceNotFoundException(e.getMessage());
        }
    }

	@GetMapping(value = "/funcational-locations")
	public ResponseEntity<List<FunctionalLocation>> getRecommondationUsers(@RequestParam String assetName) {
		try {
			System.out.println("EventId ...: "+assetName);
			return ResponseEntity.ok(caseDefinitionService.getFunctionalLocations(assetName));
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
	
	@PostMapping("/send")
    public ResponseEntity<String> sendEmail(@RequestParam String emailId) {
        try {
            String subject = "Test Email";
            String body = "This is a test email from Spring Boot.";
            caseDefinitionService.sendEmail(emailId, subject, body);
            return ResponseEntity.ok("Email sent successfully to " + emailId);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error while sending email: " + e.getMessage());
        }
    }
	@GetMapping(value = "/users/ge-apm")
	public ResponseEntity<List<?>> getUsersFromAD() throws Exception {
		try {
			return ResponseEntity.ok(caseDefinitionService.getGEUsers());
		} catch (CaseDefinitionNotFoundException e) {
			throw new RestResourceNotFoundException(e.getMessage());
		}
	}
	@GetMapping(value = "/ge-apm/recommendation/status")
	public ResponseEntity<List<Case>> updateRecommendationStatus() throws Exception {
		try {
			return ResponseEntity.ok(caseDefinitionService.updateRecommendationStatus());
		} catch (CaseDefinitionNotFoundException e) {
			throw new RestResourceNotFoundException(e.getMessage());
		}
	}
	@PostMapping("/analysis")
    public ResponseEntity<Case> saveAnalysis(@RequestBody Case caseData) {
        Case savedCase = caseDefinitionService.saveAnalysis(caseData);
        return ResponseEntity.ok(savedCase);
    }
}
