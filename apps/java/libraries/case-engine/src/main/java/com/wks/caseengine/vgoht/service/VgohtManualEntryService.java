package com.wks.caseengine.vgoht.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface VgohtManualEntryService {

    AOPMessageVM getManualProduction(UUID plantId, String aopYear, String periodFrom, String periodTo, String tabName);

    AOPMessageVM saveManualProduction(UUID plantId, String aopYear, String periodFrom, String periodTo,
            List<Map<String, Object>> data, String tabName);
}
