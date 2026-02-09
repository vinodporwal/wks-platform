package com.wks.caseengine.cases.instance.command;

import java.util.List;

import com.wks.caseengine.cases.instance.CaseInstance;
import com.wks.caseengine.command.Command;
import com.wks.caseengine.command.CommandContext;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class findOverdueCaseInstanceCmd implements Command<List<CaseInstance>> {

    @Override
    public List<CaseInstance> execute(CommandContext commandContext) {
        return  commandContext.getCaseInstanceRepository().findCasesWithDueDateGreaterThanNow();

         
    }

  
}
