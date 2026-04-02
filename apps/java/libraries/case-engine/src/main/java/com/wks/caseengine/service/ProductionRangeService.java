package com.wks.caseengine.service;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface ProductionRangeService {

    AOPMessageVM getProductionRange(String plantId, String aopYear);
    AOPMessageVM getProductionRangeLimit(String plantId, String aopYear);
}

