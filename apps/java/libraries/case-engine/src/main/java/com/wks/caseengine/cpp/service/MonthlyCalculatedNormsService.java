package com.wks.caseengine.cpp.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface MonthlyCalculatedNormsService {

    AOPMessageVM getMonthlyCalculatedNorms(List<UUID> plantIds, String financialYear,
                                           String fromDate, String toDate);

    byte[] exportMonthlyCalculatedNorms(List<UUID> plantIds, String financialYear,
                                        String fromDate, String toDate) throws IOException;
}
