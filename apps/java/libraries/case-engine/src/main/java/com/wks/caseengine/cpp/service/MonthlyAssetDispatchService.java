package com.wks.caseengine.cpp.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface MonthlyAssetDispatchService {

    AOPMessageVM getMonthlyAssetDispatch(List<UUID> plantIds, String aopYear);

    byte[] exportMonthlyAssetDispatch(List<UUID> plantIds, String aopYear) throws IOException;
}

