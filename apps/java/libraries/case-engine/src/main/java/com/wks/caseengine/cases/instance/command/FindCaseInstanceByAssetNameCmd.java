package com.wks.caseengine.cases.instance.command;

import java.util.List;

import com.wks.caseengine.cases.instance.CaseInstance;
import com.wks.caseengine.cases.instance.CaseInstanceFilter;
import com.wks.caseengine.command.Command;
import com.wks.caseengine.command.CommandContext;
import com.wks.caseengine.pagination.PageResult;

import lombok.AllArgsConstructor;

/**
 * @author victor.franca
 *
 */
@AllArgsConstructor
public class FindCaseInstanceByAssetNameCmd implements Command<PageResult<CaseInstance>> {

	private CaseInstanceFilter caseFilter;
	private String assetName;
	private List<String> eventIds;
	@Override
	public PageResult<CaseInstance> execute(CommandContext commandContext) {
		return commandContext.getCaseInstanceRepository().findByAssetName(caseFilter, assetName, eventIds);
	}

}
