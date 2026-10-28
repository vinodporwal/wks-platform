package com.wks.caseengine.cpp.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface JMDOutputUtilityRateService {

    /**
     * Fetch utility rate snapshots aggregated across multiple CPP plants
     * (JMD multiplant approach) for a given AOP/financial year.
     */
    AOPMessageVM getUtilityRateData(List<UUID> plantIds, String aopYear);

    /**
     * Export the same utility rate data to an Excel workbook.
     */
    byte[] exportUtilityRateExcel(List<UUID> plantIds, String aopYear) throws IOException;
}
