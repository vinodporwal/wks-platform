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
package com.wks.caseengine.cases.instance.service;

import com.wks.caseengine.command.CommandContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wks.caseengine.cases.instance.CaseComment;
import com.wks.caseengine.cases.instance.CaseDocument;
import com.wks.caseengine.cases.instance.CaseInstance;
import com.wks.caseengine.cases.instance.CaseInstanceFilter;
import com.wks.caseengine.cases.instance.command.CreateCaseInstanceCommentCmd;
import com.wks.caseengine.cases.instance.command.CreateCaseInstanceDocumentCmd;
import com.wks.caseengine.cases.instance.command.DeleteCaseInstanceCmd;
import com.wks.caseengine.cases.instance.command.DeleteCaseInstanceCommentCmd;
import com.wks.caseengine.cases.instance.command.FindCaseInstanceCmd;
import com.wks.caseengine.cases.instance.command.GetCaseInstanceCmd;
import com.wks.caseengine.cases.instance.command.PatchCaseInstanceCmd;
import com.wks.caseengine.cases.instance.command.SaveCaseInstanceWithValuesCmd;
import com.wks.caseengine.cases.instance.command.StartCaseInstanceWithValuesCmd;
import com.wks.caseengine.cases.instance.command.UpdateCaseInstanceCommentCmd;
import com.wks.caseengine.command.CommandExecutor;
import com.wks.caseengine.pagination.PageResult;
import com.wks.caseengine.cases.instance.command.findOverdueCaseInstanceCmd;
import com.wks.caseengine.cases.instance.email.CaseEmailServiceImpl;
import com.wks.caseengine.cases.definition.service.CaseDefinitionService;
import com.wks.caseengine.cases.instance.command.FindCaseInstanceByAssetNameCmd;
import com.wks.caseengine.cases.instance.command.UpdateEventIdsCmd;

import com.wks.caseengine.rest.db2.repository.UsersRepository;
import java.util.ArrayList;

@Component
public class CaseInstanceServiceImpl implements CaseInstanceService {

	@Autowired
	private CommandExecutor commandExecutor;

    @Autowired
    private CommandContext commandContext;

	@Autowired
	private CaseEmailServiceImpl emailservice;

	@Autowired
	private CaseDefinitionService caseDefinitionService;

	@Autowired
	private UsersRepository usersRepository;

	@Value("${spring.data.mongodb.database.tenant}")
	private String dbTenant;

	@Override
	public PageResult<CaseInstance> find(CaseInstanceFilter filters) {
		return commandExecutor.execute(new FindCaseInstanceCmd(filters));
	}

	@Override
	public PageResult<CaseInstance> findByAssetName(CaseInstanceFilter filters, String assetName, List<String> eventIds) {
		return commandExecutor.execute(new FindCaseInstanceByAssetNameCmd(filters, assetName, eventIds));
	}

	@Override
	public CaseInstance get(final String businessKey) {
		return commandExecutor.execute(new GetCaseInstanceCmd(businessKey));
	}

	@Override
	public void updateEventIds(final List<String> businessKeys, final List<String> eventIds, List<CaseInstance.EventUrlItem> eventTrendUrls, List<CaseInstance.EventUrlItem> eventReportUrls) {
		commandExecutor.execute(new UpdateEventIdsCmd(businessKeys, eventIds, eventTrendUrls, eventReportUrls, caseDefinitionService));
	}

	@Override
	public CaseInstance startWithValues   (final CaseInstance caseInstance) {

		// return commandExecutor.execute(new StartCaseInstanceWithValuesCmd(caseInstance));
		CaseInstance changedInstance =  commandExecutor.execute(new StartCaseInstanceWithValuesCmd(caseInstance));


	    if(changedInstance.getBusinessKey().equals(caseInstance.getBusinessKey())) {

			System.out.println("****** case: update existingcase instance");

		    try {
		        commandContext.getCaseInstanceRepository().update(changedInstance.getBusinessKey(), changedInstance);
		    } catch(Exception e) {
		        System.out.println("error while updating caseInstance");
		          throw new RuntimeException(e);
		    }
		}
		else {
			  commandContext.getCaseInstanceRepository().save(changedInstance);
		}
		return changedInstance;
	}

	@Override
	public void saveWithValues(final CaseInstance caseInstance) {
		commandExecutor.execute(new SaveCaseInstanceWithValuesCmd(caseInstance));
	}

	@Override
	public CaseInstance patch(final String businessKey, final CaseInstance mergePatch) {
		return commandExecutor.execute(new PatchCaseInstanceCmd(businessKey, mergePatch));
	}

	@Override
	public void delete(final String businessKey) {
		commandExecutor.execute(new DeleteCaseInstanceCmd(businessKey));
	}

	@Override
	public void saveDocument(final String businessKey, final CaseDocument document) {
		commandExecutor.execute(new CreateCaseInstanceDocumentCmd(businessKey, document));
	}

	@Override
	public void saveComment(final String businessKey, final CaseComment comment) {
		commandExecutor.execute(new CreateCaseInstanceCommentCmd(businessKey, comment));
	}


	@Override
	public void updateComment(final String businessKey, final String commentId, final String body) {
		commandExecutor.execute(new UpdateCaseInstanceCommentCmd(businessKey, commentId, body));
	}

	@Override
	public void deleteComment(final String businessKey, final String commentId) {
		commandExecutor.execute(new DeleteCaseInstanceCommentCmd(businessKey, commentId));
	}

    @Override
	@Scheduled(cron = "0 40 19 * * *", zone = "Asia/Kolkata")
	public void findCasesWithDueDateGreaterThanNow() {
		
	commandContext.getSecurityContextTenantHolder().setTenantId(dbTenant);
	
	List<CaseInstance> cases = commandExecutor.execute(new findOverdueCaseInstanceCmd());

	for(CaseInstance caseInstance : cases) {
		
		caseInstance.getAttributes().forEach(attr -> {
				
				if (!"container".equals(attr.getName())) return;

				try {
                      ObjectMapper mapper = new ObjectMapper();
					  JsonNode node = mapper.readTree(attr.getValue());

					   JsonNode caseAssignedToNode = node.get("caseAssignedTo");
					    List<String> caseAssignedTo = null;

						   if(caseAssignedToNode == null || caseAssignedToNode.isNull()) { 
							//caseAssignedTo = new String[0];
							return; // skip if caseAssignedTo is null
						   }
						   else if(caseAssignedToNode.isArray()) { 
							//caseAssignedTo = mapper.convertValue(caseAssignedToNode, String[].class);
							caseAssignedTo = mapper.convertValue(caseAssignedToNode, List.class);
						   }

						   else if(caseAssignedToNode.isTextual()) {
							//caseAssignedTo = new String[] { caseAssignedToNode.asText() };
							caseAssignedTo = List.of(caseAssignedToNode.asText());
						   }
							
						   String caseName = node.get("caseTitle").asText();
						   //check for null
						  String caseAssignedToLabel = null;
						   if (caseAssignedTo != null && !caseAssignedTo.isEmpty()) {
						   	   var resolvedUser = usersRepository.findByEmailId(caseAssignedTo.get(0));
						   	   caseAssignedToLabel = (resolvedUser != null) ? resolvedUser.getUserId() : caseAssignedTo.get(0);
						   }
						   String caseAssignedBy = (caseInstance.getOwner() != null) ? caseInstance.getOwner().getName() : null;

						   List<String> ccList = new ArrayList<>();
						   JsonNode analysisTeamNode = node.path("analysisTeam");
						   if (analysisTeamNode.isArray()) {
						   	   for (JsonNode member : analysisTeamNode) {
						   	   	   ccList.add(member.asText());
						   	   }
						   }
						   if (caseInstance.getOwner() != null && caseInstance.getOwner().getEmail() != null) {
						   	   ccList.add(caseInstance.getOwner().getEmail());
						   }
						   String  caseNo = caseInstance.getBusinessKey();
						   String caseStatus = "Overdue";
						   Map<String, Object> data = new HashMap<>();
						   data.put("caseName", caseName);
						   data.put("status", caseStatus);
						   data.put("caseNumber" , caseNo);
						   data.put("assignedTo", caseAssignedToLabel);
						   data.put("assignedToLabel", caseAssignedToLabel);
						   data.put("assignedBy", caseAssignedBy);

					
					
                        emailservice.send(caseAssignedTo.toArray(new String[0]), caseName, ccList.toArray(new String[0]), null, null,"reminder-notification", data);					  
					
				}catch(Exception e) {
					e.printStackTrace();
					
				}
		});
	   
	}

}



}