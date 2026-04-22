package com.wks.caseengine.cases.instance.command;

import java.util.List;

import com.wks.caseengine.cases.definition.service.CaseDefinitionService;
import com.wks.caseengine.cases.instance.CaseInstance;
import com.wks.caseengine.command.Command;
import com.wks.caseengine.command.CommandContext;

import lombok.AllArgsConstructor;
import lombok.Setter;

/**
 * @author victor.franca
 *
 */
@AllArgsConstructor
@Setter
public class UpdateEventIdsCmd implements Command<Void> {

	private List<String> businessKeys;
	private List<String> eventIds;
	private List<CaseInstance.EventUrlItem> eventTrendUrls;
	private List<CaseInstance.EventUrlItem> eventReportUrls;
	private CaseDefinitionService caseDefinitionService;

	@Override
	public Void execute(CommandContext commandContext) {
		commandContext.getCaseInstanceRepository().updateEventIds(businessKeys, eventIds, eventTrendUrls, eventReportUrls, caseDefinitionService);
		return null;
	}

}
