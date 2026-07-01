package com.wks.caseengine.coker.service;

import java.util.UUID;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface CokerConfigurationService {
    public AOPMessageVM getConfigurationData(String year, UUID plantFKId, String type, String version);

    public AOPMessageVM getHistoricalPiggingStatus(String plantId, String aopYear);
}
