package com.wks.caseengine.vgoht.service;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface SteadyStateService {
    
    public AOPMessageVM calculateExpressionConsumptionNorms(String year, String plantId);
}
