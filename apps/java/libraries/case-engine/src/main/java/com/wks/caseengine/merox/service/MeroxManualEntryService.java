package com.wks.caseengine.merox.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface MeroxManualEntryService {

    AOPMessageVM calculateManualProduction(UUID plantId, String aopYear, String periodFrom, String periodTo);

    AOPMessageVM getManualProduction(UUID plantId, String aopYear, String periodFrom, String periodTo);

    AOPMessageVM saveManualProduction(UUID plantId, String aopYear, String periodFrom, String periodTo,
            List<Map<String, Object>> data);
}
