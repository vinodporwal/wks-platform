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
package com.wks.caseengine.cases.instance.command;

import com.wks.caseengine.cases.instance.CaseInstance;
import com.wks.caseengine.command.Command;
import com.wks.caseengine.command.CommandContext;

import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author victor.franca
 *
 */
@AllArgsConstructor
@Setter
@Slf4j
public class SaveCaseInstanceWithValuesCmd implements Command<Void> {

	private CaseInstance caseInstance;

	@Override
	public Void execute(CommandContext commandContext) {
		log.info("MongoDB CaseInstance persistence started: businessKey={}, caseDefinitionId={}",
				caseInstance.getBusinessKey(), caseInstance.getCaseDefinitionId());
		try {
			commandContext.getCaseInstanceRepository().save(caseInstance);
			log.info("MongoDB CaseInstance persistence completed successfully: businessKey={}, caseDefinitionId={}",
					caseInstance.getBusinessKey(), caseInstance.getCaseDefinitionId());
		} catch (RuntimeException e) {
			log.error("Case creation failed during MongoDB CaseInstanceRepository.save: businessKey={}, caseDefinitionId={}",
					caseInstance.getBusinessKey(), caseInstance.getCaseDefinitionId(), e);
			throw e;
		}
		return null;
	}

}
