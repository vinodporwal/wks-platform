package com.wks.caseengine.crude.service;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface SteadyStateService {
    
    public AOPMessageVM calculateExpressionConsumptionNorms(String year, String plantId);
}
