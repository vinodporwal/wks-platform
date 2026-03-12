package com.wks.caseengine.service;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface NormConfigurationService {

    AOPMessageVM getNormConfiguration(String plantId, String aopYear, String type);

    AOPMessageVM calculateNormConfiguration(String plantId, String aopYear);
}

