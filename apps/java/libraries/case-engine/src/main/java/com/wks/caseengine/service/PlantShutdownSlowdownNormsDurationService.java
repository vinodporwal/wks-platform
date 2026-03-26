package com.wks.caseengine.service;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface PlantShutdownSlowdownNormsDurationService {
    AOPMessageVM getPlantShutdownSlowdownNormsDuration(String plantId, String year);
}

