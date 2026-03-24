package com.wks.caseengine.service;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface ShutdownDetailsService {
    AOPMessageVM getShutdownDetails(String plantId, String year, String type);
}

