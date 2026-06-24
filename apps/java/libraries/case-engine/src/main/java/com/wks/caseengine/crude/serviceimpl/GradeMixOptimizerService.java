package com.wks.caseengine.crude.serviceimpl;

import java.util.UUID;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface GradeMixOptimizerService {
    
    AOPMessageVM getGradeMixOptimizerConstants(UUID plantId, String aopYear);

    AOPMessageVM calculateBudgetOperationHours(UUID plantId, String aopYear);

    AOPMessageVM getCalculatedProposedBusinessDemand(UUID plantId, String aopYear, String lineId);
}
